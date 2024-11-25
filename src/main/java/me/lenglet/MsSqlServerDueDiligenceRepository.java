package me.lenglet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gravity9.jsonpatch.JsonPatch;
import com.gravity9.jsonpatch.diff.JsonDiff;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MsSqlServerDueDiligenceRepository implements DueDiligenceRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataSource dataSource;

    public MsSqlServerDueDiligenceRepository(
            DataSource dataSource
    ) {
        this.dataSource = dataSource;
    }

    @Override
    public DueDiligence findById(long id) {
        try {
            return this.findByIdInternal(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DueDiligence findByIdInternal(long id) throws Exception {
        final var connection = this.dataSource.getConnection();
        final var preparedStatement = connection.prepareStatement("""
                  WITH stream (version, type, data) AS (
                      SELECT version
                           , type
                           , data
                      FROM event_stream
                      WHERE id = (SELECT TOP 1 id
                                  FROM event_stream
                                  WHERE stream_id = ?
                                  ORDER BY version DESC)
                      UNION ALL
                      SELECT ancestor.version
                           , ancestor.type
                           , ancestor.data
                      FROM event_stream AS ancestor
                      JOIN stream s ON s.version = ancestor.version + 1
                      AND ancestor.type = 'snapshot')
                  SELECT *
                  FROM stream
                  ORDER BY version ASC
                """);
        try (connection; preparedStatement) {
            preparedStatement.setLong(1, id);

            try (final var resultSet = preparedStatement.executeQuery()) {
                long version = 1;
                String snapshot = null;
                final List<JsonNode> jsonNodePatches = new ArrayList<>();

                var rowCount = 0;
                while (resultSet.next()) {
                    rowCount++;
                    version = resultSet.getLong("version");
                    final var type = resultSet.getString("type");
                    final var data = resultSet.getString("data");
                    if ("snapshot".equals(type)) {
                        snapshot = data;
                    } else {
                        jsonNodePatches.add(this.objectMapper.readTree(data));
                    }
                }
                if (rowCount == 0) {
                    throw new NoResultException("Unable to find stream " + id);
                }
                connection.commit();

                if (snapshot == null) {
                    throw new RuntimeException("Malformed stream, unable to find snapshot");
                }
                var jsonNodeSnapshot = this.objectMapper.readTree(snapshot);
                for (final var jsonNodePatch : jsonNodePatches) {
                    jsonNodeSnapshot = JsonPatch.fromJson(jsonNodePatch).apply(jsonNodeSnapshot);
                }

                final var data = this.objectMapper.treeToValue(jsonNodeSnapshot, DueDiligence.Data.class);
                return new DueDiligence(id, version, data);
            }
        } catch (Exception e) {
            if (!connection.isClosed()) {
                connection.rollback();
            }
            throw e;
        }
    }

    @Override
    public void update(DueDiligence dueDiligence) {
        try {
            this.updateInternal(dueDiligence);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updateInternal(DueDiligence dueDiligence) throws Exception {
        /* adding transaction id ? */
        final var version = (long) get("version", dueDiligence);
        final var data = get("data", dueDiligence);

        final var diff = JsonDiff.asJsonPatch((JsonNode) get("originState", dueDiligence), this.objectMapper.valueToTree(data));
        final var jsonPatchString = this.objectMapper.writeValueAsString(diff);

        final var connection = this.dataSource.getConnection();
        final var preparedStatement = connection.prepareStatement("""
                INSERT INTO event_stream (version, timestamp, user_id, type, data)
                VALUES (?, ?, ?, ?, ?)
                """);
        try (connection; preparedStatement) {

            preparedStatement.setLong(1, dueDiligence.getId());
            preparedStatement.setLong(2, version + 1);
            preparedStatement.setTimestamp(3, Timestamp.from(Instant.now()));
            preparedStatement.setString(4, "573658");
            preparedStatement.setString(5, "patch");
            preparedStatement.setString(6, jsonPatchString);
            try {
                preparedStatement.execute();
                connection.commit();
            } catch (SQLException e) {
                if (e.getErrorCode() == -803) {
                    throw new RuntimeException("concurrent update");
                }
                throw e;
            }
        } catch (Exception e) {
            connection.rollback();
            throw e;
        }
    }

    private static Object get(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        final var field = DueDiligence.class.getDeclaredField(fieldName);
        field.trySetAccessible();
        return field.get(object);
    }
}
