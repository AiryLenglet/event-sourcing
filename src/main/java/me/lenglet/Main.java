package me.lenglet;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import me.lenglet.service.CreateDueDiligenceService;
import me.lenglet.service.PatchDueDiligenceService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    public enum DataSourceHolder {
        INSTANCE;

        private DataSource dataSource;

        public void set(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public DataSource get() {
            return this.dataSource;
        }
    }

    public static void main(String[] args) {

        final var dataSource = new HikariDataSource();
        dataSource.setUsername("sa");
        dataSource.setPassword("Password22");
        dataSource.setJdbcUrl("jdbc:sqlserver://localhost:1433;encrypt=false;disableStatementPooling=true;statementPoolingCacheSize=2");
        dataSource.setAutoCommit(false);
        dataSource.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        dataSource.setMaximumPoolSize(1);
        dataSource.setPoolName("hikari-pool");
        DataSourceHolder.INSTANCE.set(dataSource);

        final var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        final var dueDiligenceRepository =
                new MsSqlServerDueDiligenceRepository(objectMapper, dataSource, new ConnectionFactory());

        Javalin.create(config -> {
                    config.useVirtualThreads = true;
                    config.jsonMapper(new JavalinJackson(objectMapper, true));
                })
                .get("/due-diligence/{id}", ctx -> {
                    ctx.json(objectMapper.valueToTree(dueDiligenceRepository.findById(Long.parseLong(ctx.pathParam("id")))));
                })
                .patch("/due-diligence/{id}", ctx -> {
                    ScopedValue.runWhere(MsSqlServerDueDiligenceRepository.CURRENT_VERSION, new AtomicLong(0), () -> {
                        new PatchDueDiligenceService(dueDiligenceRepository).execute(Long.parseLong(ctx.pathParam("id")));
                        ctx.status(200);
                    });
                })
                .post("/due-diligence", ctx -> {
                    new CreateDueDiligenceService(dueDiligenceRepository).execute(new CreateDueDiligenceService.Request("43545"));
                    ctx.status(200);
                })
                .exception(NoResultException.class, (e, ctx) -> {
                    ctx.status(404).json("""
                            {
                                "errorCode": "00002",
                                "errorMessage": "not found"
                            }
                            """);
                })
                .exception(ConflictException.class, (e, ctx) -> ctx.status(429).json("""
                            {
                                "errorCode": "00003",
                                "errorMessage": "conflict updating data"
                            }
                        """))
                .exception(Exception.class, (e, ctx) -> {
                    LoggerFactory.getLogger("main").error("Unhandled exception occurred", e);
                    ctx.status(500).json("""
                            {
                                "errorCode": "00001",
                                "errorMessage": "internal error"
                            }
                            """);
                })
                .start(8080);
    }
}
