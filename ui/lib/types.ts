export type HealthSnapshot = {
  status: string;
  appName: string;
  timestamp: string;
  totalTablets: number;
  availableLogs: number;
  mode: string;
};

export type TabletSummary = {
  tabletId: number;
  status: string;
  logFileExists: boolean;
  latestOffset: number;
  recordCount: number;
  fileSizeBytes: number;
  lastModifiedAt: string | null;
};

export type LogRecord = {
  offset: number;
  timestamp: number;
  stream?: string;
  key: string;
  value: string;
};

export type TabletDetail = {
  tabletId: number;
  status: string;
  logFilePath: string;
  logFileExists: boolean;
  latestOffset: number;
  nextOffset: number;
  recordCount: number;
  fileSizeBytes: number;
  lastModifiedAt: string | null;
  recentRecords: LogRecord[];
};

export type ClusterOverview = {
  status: string;
  topologyMode: string;
  leaderElection: string;
  replication: string;
  totalTablets: number;
  note: string;
};

export type AppendResult = {
  stream: string;
  tabletId: number;
  offset: number;
};

export type ReadResult = {
  stream: string;
  tabletId: number;
  offset: number;
  limit: number;
  nextOffset: number;
  logRecords: LogRecord[];
};

export type ActivityEvent = {
  tabletId: number;
  offset: number;
  timestamp: number;
  key: string;
  value: string;
};
