package me.lenglet;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class MsSqlServerDueDiligenceRepository implements DueDiligenceRepository {

    public static final ScopedValue<AtomicLong> CURRENT_VERSION = ScopedValue.newInstance();

    private final ObjectMapper objectMapper;
    private final DataSource dataSource;
    private final ConnectionFactory connectionFactory;

    public MsSqlServerDueDiligenceRepository(
            ObjectMapper objectMapper,
            DataSource dataSource,
            ConnectionFactory connectionFactory
    ) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public DueDiligence findById(long id) {
        try {
            final var connection = this.connectionFactory.getConnection();
            final var statement = connection.prepareStatement("""
                    SELECT type, data
                    FROM event_stream
                    WHERE stream_id = ?
                    ORDER BY version ASC
                    """);
            statement.setLong(1, id);
            final var rs = statement.executeQuery();
            final var events = new ArrayList<DueDiligenceEvent>();
            while (rs.next()) {
                switch (rs.getString("type")) {
                    case "init" ->
                            events.add(this.objectMapper.readValue(rs.getString("data"), DueDiligenceEvent.Created.class));
                    case "patch" ->
                            events.add(this.objectMapper.readValue(rs.getString("data"), DueDiligenceEvent.Patched.class));
                }
            }
            CURRENT_VERSION.get().set(events.size());
            return DueDiligence.from(id, events);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(DueDiligence dueDiligence) {
        final var currentVersion = CURRENT_VERSION.get();
        try {
            final var connection = this.connectionFactory.getConnection();
            final var statement = connection.prepareStatement("""
                             INSERT INTO event_stream (stream_id, version, timestamp, user_id, type, data, previous)
                             VALUES (?, ?, ?, ?, ?, ?, ?)
                    """);
            statement.setLong(1, dueDiligence.getId());
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.setString(4, "user343432");
            statement.setString(5, "patch");

            dueDiligence.getEvents().stream()
                    .skip(currentVersion.get())
                    .forEach(event -> {
                        try {
                            statement.setLong(7, currentVersion.get());
                            statement.setLong(2, currentVersion.incrementAndGet());
                            statement.setString(6, this.objectMapper.writeValueAsString(event));
                            statement.addBatch();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void persist(DueDiligence dueDiligence) {
        final var events = dueDiligence.getEvents();
        try {
            final var connection = this.dataSource.getConnection();
            final var statement = connection.prepareStatement("""
                             INSERT INTO event_stream (version, timestamp, user_id, type, data)
                             OUTPUT INSERTED.id
                             VALUES (?, ?, ?, ?, ?)
                    """);
            statement.setLong(1, 1L);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.setString(3, "user343432");
            statement.setString(4, "init");
            statement.setString(5, this.objectMapper.writeValueAsString(events.iterator().next()));

            final var rs = statement.executeQuery();
            rs.next();
            final var id = rs.getLong(1);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
