package me.lenglet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.ExceptionHandler;
import org.jetbrains.annotations.NotNull;

public class Main {

    public static void main(String[] args) {

        final var dataSource = new HikariDataSource();
        dataSource.setUsername("sa");
        dataSource.setPassword("Password22");
        dataSource.setJdbcUrl("jdbc:sqlserver://localhost:1433;encrypt=false");
        dataSource.setAutoCommit(false);
        dataSource.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        dataSource.setMaximumPoolSize(1);
        dataSource.setPoolName("hikari-pool");

        final var dueDiligenceRepository =
                new MsSqlServerDueDiligenceRepository(dataSource);

        Javalin.create(config -> config.useVirtualThreads = true)
                .get("/due-diligence/{id}", ctx -> {
                    ctx.json(new ObjectMapper().valueToTree(dueDiligenceRepository.findById(Long.parseLong(ctx.pathParam("id")))));
                })
                .exception(NoResultException.class, (e, ctx) -> {
                    ctx.status(404).json("""
                            {
                                "errorCode": "00002",
                                "errorMessage": "not found"
                            }
                            """);
                })
                .exception(Exception.class, (e, ctx) -> {
                    ctx.status(404).json("""
                            {
                                "errorCode": "00001",
                                "errorMessage": "internal error"
                            }
                            """);
                })
                .start(8080);
    }
}
