package com.logstore.core.storage;

import com.logstore.core.api.AppendResult;
import com.logstore.core.api.BackpressurePolicy;
import com.logstore.core.api.Durability;
import com.logstore.core.api.LogRecord;
import com.logstore.core.api.LogStoreConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public final class Tablet {
    private final int tabletId;
    private final Path dataDir;
    private final boolean rollingSegments;
    private final Durability durability;
    private final Clock clock;
    private final int batchSize;
    private final long flushIntervalMillis;
    private final int indexInterval;
    private final long maxSegmentBytes;
    private final BackpressurePolicy backpressurePolicy;
    private final ArrayBlockingQueue<WriteRequest> queue;
    private final Thread writerThread;
    private final List<Segment> segments = new ArrayList<>();
    private final NavigableMap<Long, IndexedPosition> sparseIndex = new TreeMap<>();
    private volatile boolean closed;
    private Segment activeSegment;
    private FileChannel writer;
    private long nextOffset;
    private long currentSizeBytes;
    private int writesSinceForce;
    private long lastForceMillis;

    Tablet(int tabletId, LogStoreConfig config, Clock clock) throws IOException {
        this.tabletId = tabletId;
        this.dataDir = config.dataDir();
        this.rollingSegments = config.maxSegmentBytes() != Long.MAX_VALUE;
        this.durability = config.durability();
        this.clock = clock;
        this.batchSize = config.batchSize();
        this.flushIntervalMillis = config.flushIntervalMillis();
        this.indexInterval = config.indexInterval();
        this.maxSegmentBytes = config.maxSegmentBytes();
        this.backpressurePolicy = config.backpressurePolicy();
        this.queue = new ArrayBlockingQueue<>(config.queueCapacity());
        Files.createDirectories(config.dataDir());
        recoverSegments();
        this.currentSizeBytes = Files.exists(activeSegment.path()) ? Files.size(activeSegment.path()) : 0L;
        rebuildSparseIndex();
        this.writer = FileChannel.open(activeSegment.path(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        this.lastForceMillis = clock.millis();
        this.writerThread = new Thread(this::writerLoop, "logstore-tablet-writer-" + tabletId);
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    public AppendResult append(String stream, String key, byte[] value) throws IOException {
        return enqueue(new WriteRequest(stream, key, value, -1L, clock.millis()));
    }

    public AppendResult appendAt(String stream, String key, byte[] value, long offset, long timestamp) throws IOException {
        return enqueue(new WriteRequest(stream, key, value, offset, timestamp));
    }

    public synchronized List<LogRecord> read(String stream, long offset, int limit) throws IOException {
        if (limit <= 0 || segments.isEmpty()) {
            return List.of();
        }

        List<LogRecord> records = new ArrayList<>();
        IndexedPosition start = sparseIndex.floorEntry(offset) == null ? null : sparseIndex.floorEntry(offset).getValue();
        for (Segment segment : segmentsFrom(start)) {
            if (!Files.exists(segment.path())) {
                continue;
            }
            try (FileChannel channel = FileChannel.open(segment.path(), StandardOpenOption.READ)) {
                if (start != null && start.segment().equals(segment)) {
                    channel.position(start.position());
                }
                while (records.size() < limit) {
                    RecordDecoder.DecodedRecord record = RecoveryManager.readNext(channel);
                    if (record == null) {
                        break;
                    }
                    if (record.offset() >= offset && stream.equals(record.stream())) {
                        records.add(new LogRecord(record.stream(), record.offset(), record.timestamp(), record.key(), record.value()));
                    }
                }
            }
            if (records.size() >= limit) {
                break;
            }
        }
        return records;
    }

    public synchronized List<LogRecord> readAll(long offset, int limit) throws IOException {
        if (limit <= 0 || segments.isEmpty()) {
            return List.of();
        }

        List<LogRecord> records = new ArrayList<>();
        IndexedPosition start = sparseIndex.floorEntry(offset) == null ? null : sparseIndex.floorEntry(offset).getValue();
        for (Segment segment : segmentsFrom(start)) {
            if (!Files.exists(segment.path())) {
                continue;
            }
            try (FileChannel channel = FileChannel.open(segment.path(), StandardOpenOption.READ)) {
                if (start != null && start.segment().equals(segment)) {
                    channel.position(start.position());
                }
                while (records.size() < limit) {
                    RecordDecoder.DecodedRecord record = RecoveryManager.readNext(channel);
                    if (record == null) {
                        break;
                    }
                    if (record.offset() >= offset) {
                        records.add(new LogRecord(record.stream(), record.offset(), record.timestamp(), record.key(), record.value()));
                    }
                }
            }
            if (records.size() >= limit) {
                break;
            }
        }
        return records;
    }

    int tabletId() {
        return tabletId;
    }

    synchronized long nextOffset() {
        return nextOffset;
    }

    synchronized long latestOffset() {
        return nextOffset == 0 ? -1L : nextOffset - 1L;
    }

    long sizeBytes() throws IOException {
        long total = 0L;
        for (Segment segment : segments) {
            total += Files.exists(segment.path()) ? Files.size(segment.path()) : 0L;
        }
        return total;
    }

    synchronized void close() throws IOException {
        closed = true;
        writerThread.interrupt();
        try {
            writerThread.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (writer != null && writer.isOpen()) {
            writer.force(true);
            writer.close();
        }
    }

    static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private AppendResult enqueue(WriteRequest request) throws IOException {
        if (closed) {
            throw new IOException("tablet is closed");
        }
        boolean accepted;
        try {
            if (backpressurePolicy == BackpressurePolicy.REJECT) {
                accepted = queue.offer(request);
            } else {
                queue.put(request);
                accepted = true;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for tablet queue capacity", ex);
        }
        if (!accepted) {
            throw new IOException("tablet append queue is full");
        }
        try {
            return request.result.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw ex;
        }
    }

    private void writerLoop() {
        while (!closed || !queue.isEmpty()) {
            try {
                WriteRequest request = queue.poll(Math.max(1L, flushIntervalMillis), TimeUnit.MILLISECONDS);
                if (request == null) {
                    maybeForceByInterval();
                    continue;
                }
                appendOnWriterThread(request);
            } catch (InterruptedException ex) {
                if (closed) {
                    return;
                }
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private synchronized void appendOnWriterThread(WriteRequest request) {
        long assignedOffset = request.offset >= 0 ? request.offset : nextOffset;
        if (assignedOffset != nextOffset) {
            request.result.completeExceptionally(new IOException("out-of-order append for tablet " + tabletId + ": expected offset " + nextOffset + " but received " + assignedOffset));
            return;
        }
        try {
            byte[] encoded = RecordEncoder.encode(assignedOffset, request.timestamp, request.stream, request.key, request.value);
            if (currentSizeBytes > 0 && currentSizeBytes + encoded.length > maxSegmentBytes) {
                rollSegment(assignedOffset);
            }
            long position = writer.position();
            writeFully(writer, ByteBuffer.wrap(encoded));
            currentSizeBytes += encoded.length;
            if (assignedOffset % indexInterval == 0) {
                sparseIndex.put(assignedOffset, new IndexedPosition(activeSegment, position));
            }
            nextOffset++;
            writesSinceForce++;
            forceIfNeeded();
            request.result.complete(new AppendResult(request.stream, tabletId, assignedOffset, request.timestamp));
        } catch (RuntimeException | IOException ex) {
            request.result.completeExceptionally(ex);
        }
    }

    private void forceIfNeeded() throws IOException {
        if (durability == Durability.FSYNC_EVERY_WRITE) {
            writer.force(true);
            writesSinceForce = 0;
            lastForceMillis = clock.millis();
            return;
        }
        if (durability == Durability.BATCHED_FSYNC && writesSinceForce >= batchSize) {
            writer.force(true);
            writesSinceForce = 0;
            lastForceMillis = clock.millis();
        }
    }

    private synchronized void maybeForceByInterval() {
        if (durability != Durability.BATCHED_FSYNC || writesSinceForce == 0) {
            return;
        }
        long now = clock.millis();
        if (flushIntervalMillis == 0 || now - lastForceMillis >= flushIntervalMillis) {
            try {
                writer.force(true);
                writesSinceForce = 0;
                lastForceMillis = now;
            } catch (IOException ignored) {
                // The next append/read recovery path will surface persistent writer failure.
            }
        }
    }

    private synchronized void rebuildSparseIndex() throws IOException {
        sparseIndex.clear();
        for (Segment segment : segments) {
            if (!Files.exists(segment.path())) {
                continue;
            }
            try (FileChannel channel = FileChannel.open(segment.path(), StandardOpenOption.READ)) {
                while (true) {
                    long position = channel.position();
                    RecordDecoder.DecodedRecord record = RecoveryManager.readNext(channel);
                    if (record == null) {
                        break;
                    }
                    if (record.offset() % indexInterval == 0 || record.offset() == segment.baseOffset()) {
                        sparseIndex.put(record.offset(), new IndexedPosition(segment, position));
                    }
                }
            }
        }
    }

    private void recoverSegments() throws IOException {
        if (!rollingSegments) {
            activeSegment = Segment.legacy(dataDir, tabletId);
            nextOffset = RecoveryManager.recoverNextOffset(activeSegment.path(), durability);
            segments.add(activeSegment);
            return;
        }
        Path tabletDir = dataDir.resolve("tablet-" + tabletId);
        Files.createDirectories(tabletDir);
        try (var paths = Files.list(tabletDir)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".log"))
                    .map(path -> new Segment(parseBaseOffset(path), path))
                    .sorted(Comparator.comparingLong(Segment::baseOffset))
                    .forEach(segments::add);
        }
        if (segments.isEmpty()) {
            activeSegment = Segment.rolling(dataDir, tabletId, 0L);
            Files.createDirectories(activeSegment.path().getParent());
            segments.add(activeSegment);
            nextOffset = 0L;
            return;
        }
        long expected = 0L;
        for (Segment segment : segments) {
            expected = RecoveryManager.recoverNextOffset(segment.path(), durability, expected);
        }
        nextOffset = expected;
        activeSegment = segments.get(segments.size() - 1);
    }

    private void rollSegment(long baseOffset) throws IOException {
        forceIfNeeded();
        writer.close();
        activeSegment = Segment.rolling(dataDir, tabletId, baseOffset);
        Files.createDirectories(activeSegment.path().getParent());
        segments.add(activeSegment);
        writer = FileChannel.open(activeSegment.path(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        currentSizeBytes = 0L;
    }

    private List<Segment> segmentsFrom(IndexedPosition start) {
        if (start == null) {
            return List.copyOf(segments);
        }
        int index = segments.indexOf(start.segment());
        if (index < 0) {
            return List.copyOf(segments);
        }
        return List.copyOf(segments.subList(index, segments.size()));
    }

    private static long parseBaseOffset(Path path) {
        String filename = path.getFileName().toString();
        return Long.parseLong(filename.substring(0, filename.length() - ".log".length()));
    }

    private record IndexedPosition(Segment segment, long position) {
    }

    private record WriteRequest(String stream, String key, byte[] value, long offset, long timestamp, CompletableFuture<AppendResult> result) {
        private WriteRequest(String stream, String key, byte[] value, long offset, long timestamp) {
            this(stream, key, value.clone(), offset, timestamp, new CompletableFuture<>());
        }
    }
}
