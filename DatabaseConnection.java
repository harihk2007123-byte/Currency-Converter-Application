/*
 * Decompiled with CFR 0.152.
 */
package com.apex.currencyconverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    private final String jdbcUrl;

    public DatabaseConnection() throws SQLException {
        this(DatabaseConnection.resolveApplicationDataDirectory());
    }

    DatabaseConnection(Path appData) throws SQLException {
        try {
            Files.createDirectories(appData, new FileAttribute[0]);
        }
        catch (IOException exception) {
            throw new SQLException("Unable to create the application data directory: " + String.valueOf(appData), exception);
        }
        this.jdbcUrl = "jdbc:sqlite:" + String.valueOf(appData.resolve("currency.db").toAbsolutePath());
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException exception) {
            throw new SQLException("SQLite JDBC driver is missing. Rebuild the packaged application with dependencies.", exception);
        }
        try (Connection connection = this.openConnection();
             Statement statement = connection.createStatement();){
            statement.execute("CREATE TABLE IF NOT EXISTS conversion_history (\n    id INTEGER PRIMARY KEY AUTOINCREMENT,\n    from_currency TEXT NOT NULL,\n    to_currency TEXT NOT NULL,\n    amount REAL NOT NULL,\n    converted_amount REAL NOT NULL,\n    exchange_rate REAL NOT NULL,\n    conversion_date TEXT NOT NULL\n)\n");
        }
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl);
    }

    public Path getDatabasePath() {
        return Path.of(this.jdbcUrl.substring("jdbc:sqlite:".length()), new String[0]);
    }

    private static Path resolveApplicationDataDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "CurrencyConverter");
        }
        return Path.of(System.getProperty("user.home"), ".currency-converter");
    }
}

