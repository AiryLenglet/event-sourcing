package me.lenglet;

import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import jakarta.validation.Validation;

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

        final var dueDuligenceRepository =
                new MsSqlServerDueDiligenceRepository(dataSource);

        final var app = Javalin.create()
                .start(8080);
        app.get("/due-diligence/{id}", ctx -> {
            dueDuligenceRepository.findById(Long.parseLong(ctx.pathParam("id")));
        });
    }
}
