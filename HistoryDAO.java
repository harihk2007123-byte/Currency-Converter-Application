/*
 * Decompiled with CFR 0.152.
 */
package com.apex.currencyconverter;

import com.apex.currencyconverter.Conversion;
import com.apex.currencyconverter.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class HistoryDAO {
    private final DatabaseConnection database;

    public HistoryDAO(DatabaseConnection database) {
        this.database = database;
    }

    public void save(Conversion conversion) throws SQLException {
        String sql = "INSERT INTO conversion_history\n    (from_currency, to_currency, amount, converted_amount, exchange_rate, conversion_date)\nVALUES (?, ?, ?, ?, ?, ?)\n";
        try (Connection connection = this.database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);){
            statement.setString(1, conversion.fromCurrency());
            statement.setString(2, conversion.toCurrency());
            statement.setDouble(3, conversion.amount());
            statement.setDouble(4, conversion.convertedAmount());
            statement.setDouble(5, conversion.exchangeRate());
            statement.setString(6, conversion.conversionDate().toString());
            statement.executeUpdate();
        }
    }

    public List<Conversion> findRecent() throws SQLException {
        ArrayList<Conversion> records = new ArrayList<Conversion>();
        String sql = "SELECT id, from_currency, to_currency, amount, converted_amount, exchange_rate, conversion_date\nFROM conversion_history\nORDER BY id DESC\n";
        try (Connection connection = this.database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql);){
            while (result.next()) {
                records.add(new Conversion(result.getLong("id"), result.getString("from_currency"), result.getString("to_currency"), result.getDouble("amount"), result.getDouble("converted_amount"), result.getDouble("exchange_rate"), LocalDateTime.parse(result.getString("conversion_date"))));
            }
        }
        return records;
    }

    public void clear() throws SQLException {
        try (Connection connection = this.database.openConnection();
             Statement statement = connection.createStatement();){
            statement.executeUpdate("DELETE FROM conversion_history");
        }
    }
}

