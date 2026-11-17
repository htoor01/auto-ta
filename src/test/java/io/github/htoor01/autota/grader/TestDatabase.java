package io.github.htoor01.autota.grader;

import java.nio.file.Path;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

/** A fresh, migrated SQLite database in a temp folder, without starting Spring. */
final class TestDatabase {

    private TestDatabase() {
    }

    static JdbcClient create(Path dir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dir.resolve("test.db") + "?foreign_keys=true");
        Flyway.configure().dataSource(dataSource).load().migrate();
        return JdbcClient.create(dataSource);
    }
}
