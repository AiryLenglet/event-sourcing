CREATE SEQUENCE event_stream_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1;
GO
CREATE TABLE event_stream (
    id BIGINT DEFAULT (NEXT VALUE FOR event_stream_seq) PRIMARY KEY CLUSTERED
  , stream_id BIGINT DEFAULT (NEXT VALUE FOR event_stream_seq) NOT NULL
  , timestamp DATETIME NOT NULL
  , version BIGINT NOT NULL
  , user_id VARCHAR(10) NOT NULL
  , type VARCHAR(20) NOT NULL
  , data NVARCHAR(max) NOT NULL --add JSON constraint
  , CONSTRAINT stream_id_version_unique UNIQUE(stream_id, version)
);
GO