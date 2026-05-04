package com.logstore.core.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void appendAndReadWorksWithoutSpring() {
        try (LogStore store = openStore()) {
            AppendResult result = store.append("orders", "ORD-1", bytes("created"));

            List<LogRecord> records = store.read("orders", 0, 100);

            assertThat(result.offset()).isZero();
            assertThat(records).hasSize(1);
            assertThat(records.get(0).offset()).isZero();
            assertThat(records.get(0).key()).isEqualTo("ORD-1");
            assertThat(records.get(0).value()).isEqualTo(bytes("created"));
        }
    }

    @Test
    void readsFromRequestedOffset() {
        try (LogStore store = openStore()) {
            store.append("orders", "ORD-1", bytes("one"));
            store.append("orders", "ORD-2", bytes("two"));
            store.append("orders", "ORD-3", bytes("three"));

            List<LogRecord> records = store.read("orders", 1, 10);

            assertThat(records).extracting(LogRecord::offset).containsExactly(1L, 2L);
            assertThat(records).extracting(LogRecord::key).containsExactly("ORD-2", "ORD-3");
        }
    }

    @Test
    void readsOnlyRequestedStreamWhenStreamsShareTablet() {
        try (LogStore store = openStore()) {
            store.append("orders", "ORD-1", bytes("one"));
            store.append("payments", "PAY-1", bytes("paid"));
            store.append("orders", "ORD-2", bytes("two"));

            List<LogRecord> records = store.read("orders", 0, 10);

            assertThat(records).extracting(LogRecord::stream).containsExactly("orders", "orders");
            assertThat(records).extracting(LogRecord::key).containsExactly("ORD-1", "ORD-2");
        }
    }

    @Test
    void replayWalksRecordsFromOffsetInBatches() {
        try (LogStore store = openStore()) {
            store.append("orders", "ORD-1", bytes("one"));
            store.append("orders", "ORD-2", bytes("two"));
            store.append("orders", "ORD-3", bytes("three"));
            List<LogRecord> replayed = new ArrayList<>();

            store.replay("orders", 1, 2, replayed::add);

            assertThat(replayed).extracting(LogRecord::offset).containsExactly(1L, 2L);
            assertThat(replayed).extracting(LogRecord::key).containsExactly("ORD-2", "ORD-3");
        }
    }

    @Test
    void replayOnlyReturnsRequestedStream() {
        try (LogStore store = openStore()) {
            store.append("orders", "ORD-1", bytes("one"));
            store.append("payments", "PAY-1", bytes("paid"));
            store.append("orders", "ORD-2", bytes("two"));
            List<LogRecord> replayed = new ArrayList<>();

            store.replay("orders", 0, 1, replayed::add);

            assertThat(replayed).extracting(LogRecord::stream).containsExactly("orders", "orders");
            assertThat(replayed).extracting(LogRecord::key).containsExactly("ORD-1", "ORD-2");
        }
    }

    @Test
    void restartRecoveryResumesAtNextValidOffset() {
        try (LogStore store = openStore()) {
            store.append("orders", "ORD-1", bytes("one"));
            store.append("orders", "ORD-2", bytes("two"));
        }

        try (LogStore reopened = openStore()) {
            AppendResult result = reopened.append("orders", "ORD-3", bytes("three"));

            assertThat(result.offset()).isEqualTo(2L);
            assertThat(reopened.read("orders", 0, 10)).extracting(LogRecord::offset).containsExactly(0L, 1L, 2L);
        }
    }

    @Test
    void restartRecoveryTruncatesPartialLastRecord() throws Exception {
        try (LogStore store = openStore()) {
            store.append("orders", "ORD-1", bytes("one"));
            store.append("orders", "ORD-2", bytes("two"));
        }

        Path logFile = tempDir.resolve("tablet-0.log");
        long originalSize = Files.size(logFile);
        Files.write(logFile, new byte[]{1, 2, 3, 4, 5}, StandardOpenOption.APPEND);

        try (LogStore reopened = openStore()) {
            assertThat(Files.size(logFile)).isEqualTo(originalSize);
            AppendResult result = reopened.append("orders", "ORD-3", bytes("three"));

            assertThat(result.offset()).isEqualTo(2L);
            assertThat(reopened.read("orders", 0, 10)).extracting(LogRecord::key).containsExactly("ORD-1", "ORD-2", "ORD-3");
        }
    }

    @Test
    void restartRecoveryRejectsOversizedCorruptFrameWithoutAllocating() throws Exception {
        Path logFile = tempDir.resolve("tablet-0.log");
        ByteBuffer corruptPrefix = ByteBuffer.allocate(Integer.BYTES + Short.BYTES + Integer.BYTES);
        corruptPrefix.putInt(0x4c535431);
        corruptPrefix.putShort((short) 1);
        corruptPrefix.putInt(Integer.MAX_VALUE);
        Files.write(logFile, corruptPrefix.array(), StandardOpenOption.CREATE_NEW);

        try (LogStore store = openStore()) {
            AppendResult result = store.append("orders", "ORD-1", bytes("one"));

            assertThat(result.offset()).isZero();
            assertThat(store.read("orders", 0, 10)).extracting(LogRecord::key).containsExactly("ORD-1");
        }
    }

    @Test
    void readTabletReturnsRecentRecordsAcrossStreams() {
        try (LogStore store = openStore()) {
            store.append("orders", "ORD-1", bytes("one"));
            store.append("payments", "PAY-1", bytes("paid"));

            List<LogRecord> records = store.readTabletForAdmin(0, 0, 10);

            assertThat(records).extracting(LogRecord::stream).containsExactly("orders", "payments");
            assertThat(records).extracting(LogRecord::key).containsExactly("ORD-1", "PAY-1");
        }
    }

    @Test
    void invalidConfigIsRejected() {
        assertThatThrownBy(() -> LogStoreConfig.builder().dataDir(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> LogStoreConfig.builder().durability(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> LogStoreConfig.builder().partitions(0).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidApiInputsAreRejected() {
        try (LogStore store = openStore()) {
            assertThatThrownBy(() -> store.append(null, "key", bytes("value")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> store.append("orders", " ", bytes("value")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> store.append("orders", "key", null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> store.read("orders", -1, 10))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> store.tabletForStreamId(" "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> store.replay("orders", 0, 0, record -> { }))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> store.replay("orders", 0, 10, null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> store.readTabletForAdmin(99, 0, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void plannedDurabilityModesCurrentlyBehaveLikeFsyncEveryWrite() {
        for (Durability durability : List.of(Durability.BATCHED_FSYNC, Durability.ASYNC_FLUSH)) {
            try (LogStore store = LogStore.open(LogStoreConfig.builder()
                    .dataDir(tempDir.resolve(durability.name()))
                    .partitions(1)
                    .durability(durability)
                    .build())) {
                store.append("orders", "ORD-1", bytes("one"));

                assertThat(store.read("orders", 0, 10)).extracting(LogRecord::key).containsExactly("ORD-1");
            }
        }
    }

    @Test
    void operationsAfterCloseAreRejected() {
        LogStore store = openStore();
        store.close();

        assertThatThrownBy(() -> store.append("orders", "ORD-1", bytes("one")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store.read("orders", 0, 10))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(store::partitionCount)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void oversizedAppendPayloadIsRejected() {
        try (LogStore store = openStore()) {
            byte[] value = new byte[17 * 1024 * 1024];

            assertThatThrownBy(() -> store.append("orders", "ORD-1", value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void restartRecoveryTruncatesCrcMismatchTail() throws Exception {
        try (LogStore store = openStore()) {
            store.append("orders", "ORD-1", bytes("one"));
        }
        Path logFile = tempDir.resolve("tablet-0.log");
        byte[] contents = Files.readAllBytes(logFile);
        contents[contents.length - 1] = (byte) (contents[contents.length - 1] + 1);
        Files.write(logFile, contents, StandardOpenOption.TRUNCATE_EXISTING);

        try (LogStore reopened = openStore()) {
            assertThat(reopened.read("orders", 0, 10)).isEmpty();
            AppendResult result = reopened.append("orders", "ORD-1", bytes("one"));

            assertThat(result.offset()).isZero();
            assertThat(reopened.read("orders", 0, 10)).extracting(LogRecord::key).containsExactly("ORD-1");
        }
    }

    @Test
    void startupMigratesLegacyTextRecordsToFramedRecords() throws Exception {
        Path logFile = tempDir.resolve("tablet-0.log");
        Files.writeString(logFile,
                "0|1734150400123|ORD-1|one\n1|1734150400456|ORD-2|two\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);

        try (LogStore store = openStore()) {
            assertThat(store.read("default", 0, 10)).extracting(LogRecord::key).containsExactly("ORD-1", "ORD-2");
            assertThat(store.append("default", "ORD-3", bytes("three")).offset()).isEqualTo(2L);
        }

        assertThat(Files.readString(logFile, StandardCharsets.ISO_8859_1)).doesNotContain("1734150400123|ORD-1");
    }

    @Test
    void concurrentAppendsToSameStreamProduceUniqueIncreasingOffsets() throws Exception {
        int records = 200;
        try (LogStore store = openStore()) {
            var executor = Executors.newFixedThreadPool(8);
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < records; i++) {
                int recordId = i;
                tasks.add(() -> store.append("orders", "ORD-" + recordId, bytes("value-" + recordId)).offset());
            }

            List<Future<Long>> futures = executor.invokeAll(tasks);
            executor.shutdown();

            Set<Long> offsets = new HashSet<>();
            for (Future<Long> future : futures) {
                offsets.add(future.get());
            }

            assertThat(offsets).hasSize(records);
            assertThat(offsets).containsAll(java.util.stream.LongStream.range(0, records).boxed().toList());
            assertThat(store.read("orders", 0, records + 1)).extracting(LogRecord::offset)
                    .containsExactlyElementsOf(java.util.stream.LongStream.range(0, records).boxed().toList());
        }
    }

    private LogStore openStore() {
        return LogStore.open(LogStoreConfig.builder()
                .dataDir(tempDir)
                .partitions(1)
                .durability(Durability.FSYNC_EVERY_WRITE)
                .build());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
