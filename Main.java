/*
 * Decompiled with CFR 0.152.
 */
package com.apex.currencyconverter;

import com.apex.currencyconverter.CurrencyConverterUI;
import com.apex.currencyconverter.CurrencyService;
import com.apex.currencyconverter.DatabaseConnection;
import com.apex.currencyconverter.HistoryDAO;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                DatabaseConnection database = new DatabaseConnection();
                new CurrencyConverterUI(new HistoryDAO(database), new CurrencyService()).setVisible(true);
            }
            catch (SQLException exception) {
                exception.printStackTrace(System.err);
                JOptionPane.showMessageDialog(null, "The application could not initialize its local database.\n" + exception.getMessage(), "Startup error", 0);
            }
            catch (Exception exception) {
                exception.printStackTrace(System.err);
                JOptionPane.showMessageDialog(null, "The application could not start.\n" + exception.getMessage(), "Startup error", 0);
            }
        });
    }
}

