package me.lenglet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import static java.lang.ScopedValue.callWhere;

public class ConnectionFactory {

    static final Logger LOGGER = LoggerFactory.getLogger(ConnectionFactory.class);

    private static final ScopedValue<Connection> CONNECTION = ScopedValue.newInstance();

    public Connection getConnection() {
        return CONNECTION.get();
    }

    public static Object wrapInTransaction(Supplier<Object> u) {
        final var dataSource = me.lenglet.Main.DataSourceHolder.INSTANCE.get();
        try {
            LOGGER.info("Opening transaction");
            final var connection = dataSource.getConnection();
            try (connection) {
                final var result = callWhere(CONNECTION, connection, u::get);
                connection.commit();
                return result;
            } catch (Exception e) {
                LOGGER.error("Exception occurred, rolling back");
                if (!connection.isClosed()) {
                    connection.rollback();
                }
                if (e.getCause() instanceof SQLException sqlException) {
                    switch (sqlException.getErrorCode()) {
                        case 2601 -> throw new ConflictException(sqlException);
                    }
                }
                throw e instanceof RuntimeException r ?
                        r :
                        new RuntimeException(e);
            }
        } catch (java.sql.SQLException s) {
            throw new RuntimeException(s);
        }
    }
}
