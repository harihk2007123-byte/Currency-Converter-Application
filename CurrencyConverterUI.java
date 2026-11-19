/*
 * Decompiled with CFR 0.152.
 */
package com.apex.currencyconverter;

import com.apex.currencyconverter.APIResponse;
import com.apex.currencyconverter.Conversion;
import com.apex.currencyconverter.Currency;
import com.apex.currencyconverter.CurrencyService;
import com.apex.currencyconverter.HistoryDAO;
import com.apex.currencyconverter.RateChartPanel;
import com.apex.currencyconverter.RatePoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public final class CurrencyConverterUI
extends JFrame {
    private static final Color NAVY = new Color(17, 47, 86);
    private static final Color BLUE = new Color(35, 111, 209);
    private static final Color PALE_BLUE = new Color(239, 246, 255);
    private static final Color TEXT = new Color(38, 52, 70);
    private static final Color MUTED = new Color(112, 128, 147);
    private static final Color GREEN = new Color(20, 145, 94);
    private static final DateTimeFormatter TABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a", Locale.US);
    private final HistoryDAO historyDAO;
    private final CurrencyService currencyService;
    private final JTextField amountField = new JTextField();
    private final JComboBox<Currency> fromCurrency = new JComboBox();
    private final JComboBox<Currency> toCurrency = new JComboBox();
    private final JLabel resultAmount = new JLabel("\u2014");
    private final JLabel exchangeRate = new JLabel("Exchange rate will appear here");
    private final JLabel updatedAt = new JLabel("Updated time will appear here");
    
    
    
    private final JLabel statusLabel = new JLabel("Ready to convert");
    private final JLabel chartStatusLabel = new JLabel("Daily rates over the last 30 days");
    private final JButton convertButton = new PrimaryButton("CONVERT");
    private final RateChartPanel rateChartPanel = new RateChartPanel();
    private final JTable historyTable;
    private final HistoryTableModel historyModel = new HistoryTableModel();
    private SwingWorker<List<RatePoint>, Void> chartWorker;
    private int chartRequestId;
    private boolean changingCurrencyPair;

    public CurrencyConverterUI(HistoryDAO historyDAO, CurrencyService currencyService) throws SQLException {
        super("Currency Converter");
        this.historyDAO = historyDAO;
        this.currencyService = currencyService;
        this.historyTable = new JTable(this.historyModel);
        this.configureWindow();
        this.buildInterface();
        this.refreshHistory();
        this.fromCurrency.addActionListener(event -> {
            if (!this.changingCurrencyPair) {
                this.loadRateChart();
            }
        });
        this.toCurrency.addActionListener(event -> {
            if (!this.changingCurrencyPair) {
                this.loadRateChart();
            }
        });
        this.loadRateChart();
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent event) {
                CurrencyConverterUI.this.dispose();
            }
        });
    }

    private void configureWindow() {
        this.setDefaultCloseOperation(3);
        this.setMinimumSize(new Dimension(1000, 700));
        this.setSize(1500, 900);
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(new Color(246, 249, 253));
    }

    private void buildInterface() {
        this.setLayout(new BorderLayout());
        this.add((Component)this.createHeader(), "North");
        this.add((Component)this.createMainContent(), "Center");
        this.add((Component)this.createFooter(), "South");
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(25, 50, 25, 50));
        JLabel title = new JLabel("CURRENCY CONVERTER");
        title.setFont(new Font("Segoe UI", 1, 30));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Convert Any Currency Instantly");
        subtitle.setFont(new Font("Segoe UI", 0, 16));
        subtitle.setForeground(new Color(207, 223, 243));
        JPanel titles = new JPanel(new GridBagLayout());
        titles.setOpaque(false);
        GridBagConstraints c = CurrencyConverterUI.constraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = 17;
        c.insets = new Insets(0, 0, 5, 0);
        titles.add((Component)title, c);
        ++c.gridy;
        c.insets = InsetsZero.VALUE;
        titles.add((Component)subtitle, c);
        JLabel liveBadge = new JLabel("  LIVE EXCHANGE RATES  ");
        liveBadge.setOpaque(true);
        liveBadge.setBackground(new Color(37, 73, 118));
        liveBadge.setForeground(new Color(222, 237, 255));
        liveBadge.setFont(new Font("Segoe UI", 1, 12));
        liveBadge.setBorder(BorderFactory.createEmptyBorder(9, 8, 9, 8));
        header.add((Component)titles, "West");
        header.add((Component)liveBadge, "East");
        return header;
    }

    private JPanel createMainContent() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(28, 38, 25, 38));
        GridBagConstraints c = CurrencyConverterUI.constraints();
        c.fill = 1;
        c.weighty = 1.0;
        c.gridx = 0;
        c.weightx = 0.43;
        c.insets = new Insets(0, 0, 0, 14);
        content.add((Component)this.createConversionPanel(), c);
        c.gridx = 1;
        c.weightx = 0.57;
        c.insets = new Insets(0, 14, 0, 0);
        content.add((Component)this.createHistoryPanel(), c);
        return content;
    }

    private JPanel createConversionPanel() {
        JPanel panel = CurrencyConverterUI.cardPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = CurrencyConverterUI.constraints();
        c.fill = 2;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 10, 0);
        JLabel heading = new JLabel("Convert Currency");
        heading.setFont(new Font("Segoe UI", 1, 23));
        heading.setForeground(TEXT);
        c.gridy = 0;
        c.insets = new Insets(0, 0, 3, 0);
        panel.add((Component)heading, c);
        JLabel caption = new JLabel("Get the latest exchange rate in seconds");
        caption.setFont(new Font("Segoe UI", 0, 13));
        caption.setForeground(MUTED);
        ++c.gridy;
        c.insets = new Insets(0, 0, 24, 0);
        panel.add((Component)caption, c);
        this.addFieldLabel(panel, "AMOUNT", 2);
        this.amountField.setFont(new Font("Segoe UI", 0, 17));
        this.amountField.setForeground(TEXT);
        this.amountField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(218, 228, 240)), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        this.amountField.setToolTipText("Enter an amount greater than zero");
        c.gridy = 3;
        c.insets = new Insets(0, 0, 18, 0);
        panel.add((Component)this.amountField, c);
        this.addFieldLabel(panel, "FROM CURRENCY", 4);
        CurrencyConverterUI.populateCurrencies(this.fromCurrency);
        CurrencyConverterUI.styleComboBox(this.fromCurrency);
        c.gridy = 5;
        c.insets = new Insets(0, 0, 12, 0);
        panel.add(this.fromCurrency, c);
        this.addFieldLabel(panel, "TO CURRENCY", 6);
        CurrencyConverterUI.populateCurrencies(this.toCurrency);
        this.toCurrency.setSelectedIndex(1);
        CurrencyConverterUI.styleComboBox(this.toCurrency);
        c.gridy = 7;
        c.insets = new Insets(0, 0, 12, 0);
        panel.add(this.toCurrency, c);
        JButton swapButton = new JButton("\u21c5   SWAP CURRENCIES");
        CurrencyConverterUI.styleSecondaryButton(swapButton);
        swapButton.addActionListener(event -> this.swapCurrencies());
        c.gridy = 8;
        c.insets = new Insets(0, 0, 20, 0);
        panel.add((Component)swapButton, c);
        this.resultAmount.setFont(new Font("Segoe UI", 1, 32));
        this.resultAmount.setForeground(GREEN);
        this.resultAmount.setHorizontalAlignment(0);
        this.exchangeRate.setFont(new Font("Segoe UI", 0, 13));
        this.exchangeRate.setForeground(TEXT);
        this.exchangeRate.setHorizontalAlignment(0);
        this.updatedAt.setFont(new Font("Segoe UI", 0, 12));
        this.updatedAt.setForeground(MUTED);
        this.updatedAt.setHorizontalAlignment(0);
        JPanel resultPanel = new JPanel(new GridBagLayout());
        resultPanel.setBackground(PALE_BLUE);
        resultPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(218, 232, 249)), BorderFactory.createEmptyBorder(13, 14, 13, 14)));
        GridBagConstraints rc = CurrencyConverterUI.constraints();
        rc.gridx = 0;
        rc.gridy = 0;
        resultPanel.add((Component)CurrencyConverterUI.sectionLabel("CONVERTED AMOUNT"), rc);
        ++rc.gridy;
        rc.insets = new Insets(5, 0, 6, 0);
        resultPanel.add((Component)this.resultAmount, rc);
        ++rc.gridy;
        rc.insets = new Insets(0, 0, 5, 0);
        resultPanel.add((Component)this.exchangeRate, rc);
        ++rc.gridy;
        rc.insets = InsetsZero.VALUE;
        resultPanel.add((Component)this.updatedAt, rc);
        c.gridy = 9;
        c.insets = new Insets(0, 0, 12, 0);
        panel.add((Component)resultPanel, c);
        this.statusLabel.setFont(new Font("Segoe UI", 0, 12));
        this.statusLabel.setForeground(MUTED);
        c.gridy = 10;
        c.insets = new Insets(0, 0, 12, 0);
        panel.add((Component)this.statusLabel, c);
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints bc = CurrencyConverterUI.constraints();
        bc.fill = 2;
        bc.weightx = 1.0;
        this.convertButton.setFont(new Font("Segoe UI", 1, 14));
        this.convertButton.setForeground(Color.WHITE);
        this.convertButton.setBackground(BLUE);
        this.convertButton.setFocusPainted(false);
        this.convertButton.setBorder(BorderFactory.createEmptyBorder(14, 15, 14, 15));
        this.convertButton.addActionListener(event -> this.convert());
        buttons.add((Component)this.convertButton, bc);
        JButton clearButton = new JButton("CLEAR");
        CurrencyConverterUI.styleSecondaryButton(clearButton);
        bc.gridx = 1;
        bc.weightx = 0.38;
        bc.insets = new Insets(0, 9, 0, 0);
        clearButton.addActionListener(event -> this.clearInputs());
        buttons.add((Component)clearButton, bc);
        c.gridy = 11;
        c.insets = InsetsZero.VALUE;
        panel.add((Component)buttons, c);
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = CurrencyConverterUI.cardPanel();
        panel.setLayout(new BorderLayout(0, 16));
        JPanel heading = new JPanel(new BorderLayout(8, 5));
        heading.setOpaque(false);
        JPanel headingText = new JPanel(new GridBagLayout());
        headingText.setOpaque(false);
        GridBagConstraints c = CurrencyConverterUI.constraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = 17;
        JLabel title = new JLabel("Rates & History");
        title.setFont(new Font("Segoe UI", 1, 23));
        title.setForeground(TEXT);
        headingText.add((Component)title, c);
        ++c.gridy;
        JLabel subheading = new JLabel("Live market trend and your recent conversions");
        subheading.setFont(new Font("Segoe UI", 0, 13));
        subheading.setForeground(MUTED);
        headingText.add((Component)subheading, c);
        JButton clearHistoryButton = new JButton("CLEAR HISTORY");
        CurrencyConverterUI.styleSecondaryButton(clearHistoryButton);
        clearHistoryButton.addActionListener(event -> this.clearHistory());
        heading.add((Component)headingText, "West");
        heading.add((Component)clearHistoryButton, "East");
        panel.add((Component)heading, "North");
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", 1, 12));
        tabs.addTab("30-DAY RATE GRAPH", this.createRateChartTab());
        tabs.addTab("CONVERSION HISTORY", this.createHistoryTab());
        panel.add((Component)tabs, "Center");
        return panel;
    }

    private JPanel createRateChartTab() {
        JPanel chartTab = new JPanel(new BorderLayout(0, 10));
        chartTab.setBackground(Color.WHITE);
        chartTab.setBorder(BorderFactory.createEmptyBorder(18, 8, 8, 8));
        this.chartStatusLabel.setFont(new Font("Segoe UI", 0, 12));
        this.chartStatusLabel.setForeground(MUTED);
        chartTab.add((Component)this.chartStatusLabel, "North");
        this.rateChartPanel.setBorder(BorderFactory.createLineBorder(new Color(232, 237, 244)));
        chartTab.add((Component)this.rateChartPanel, "Center");
        JLabel sourceNote = new JLabel("Daily reference rates \u2022 latest available quote \u2022 weekends/holidays may be omitted");
        sourceNote.setFont(new Font("Segoe UI", 0, 11));
        sourceNote.setForeground(MUTED);
        chartTab.add((Component)sourceNote, "South");
        return chartTab;
    }

    private JPanel createHistoryTab() {
        JPanel historyTab = new JPanel(new BorderLayout());
        historyTab.setBackground(Color.WHITE);
        historyTab.setBorder(BorderFactory.createEmptyBorder(14, 8, 8, 8));
        this.historyTable.setRowHeight(42);
        this.historyTable.setFont(new Font("Segoe UI", 0, 13));
        this.historyTable.setForeground(TEXT);
        this.historyTable.setGridColor(new Color(232, 237, 244));
        this.historyTable.setShowVerticalLines(false);
        this.historyTable.setIntercellSpacing(new Dimension(0, 1));
        this.historyTable.setFillsViewportHeight(true);
        this.historyTable.getTableHeader().setFont(new Font("Segoe UI", 1, 12));
        this.historyTable.getTableHeader().setForeground(new Color(86, 103, 124));
        this.historyTable.getTableHeader().setBackground(new Color(245, 248, 252));
        this.historyTable.getTableHeader().setPreferredSize(new Dimension(0, 43));
        this.historyTable.setDefaultRenderer(Object.class, new AlternatingRowRenderer());
        JScrollPane scrollPane = new JScrollPane(this.historyTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 233, 241)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        historyTab.add((Component)scrollPane, "Center");
        return historyTab;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(239, 244, 250));
        footer.setBorder(BorderFactory.createEmptyBorder(12, 40, 12, 40));
        JLabel supported = new JLabel("SUPPORTED: USD  \u2022  EUR  \u2022  INR  \u2022  GBP  \u2022  JPY  \u2022  AUD  \u2022  CAD  \u2022  SGD  \u2022  CNY  \u2022  AED");
        supported.setFont(new Font("Segoe UI", 0, 11));
        supported.setForeground(new Color(94, 111, 132));
        JLabel credit = new JLabel("Developed using Java & Java Swing");
        credit.setFont(new Font("Segoe UI", 0, 11));
        credit.setForeground(new Color(94, 111, 132));
        footer.add((Component)supported, "West");
        footer.add((Component)credit, "East");
        return footer;
    }

    private void convert() {
        BigDecimal amount;
        String rawAmount = this.amountField.getText().trim();
        try {
            if (rawAmount.isEmpty()) {
                throw new NumberFormatException("Amount is empty.");
            }
            if (rawAmount.contains(",") && !rawAmount.matches("\\d{1,3}(,\\d{3})+(\\.\\d+)?")) {
                throw new NumberFormatException("Amount has invalid digit grouping.");
            }
            amount = new BigDecimal(rawAmount.replace(",", ""));
            if (amount.signum() <= 0) {
                this.showError("Enter an amount greater than zero.");
                return;
            }
            if (!Double.isFinite(amount.doubleValue()) || amount.precision() > 15 || amount.scale() > 8) {
                this.showError("Enter an amount with at most 15 significant digits and 8 decimal places.");
                return;
            }
        }
        catch (NumberFormatException exception) {
            this.showError("Enter a valid positive number.");
            return;
        }
        final Currency source = (Currency)this.fromCurrency.getSelectedItem();
        final Currency target = (Currency)this.toCurrency.getSelectedItem();
        if (source == null || target == null) {
            this.showError("Select both currencies before converting.");
            return;
        }
        this.convertButton.setEnabled(false);
        this.convertButton.setText("FETCHING RATES\u2026");
        this.statusLabel.setForeground(BLUE);
        this.statusLabel.setText("Connecting to live exchange rates\u2026");
        new SwingWorker<ConversionResult, Void>(this){
            final /* synthetic */ CurrencyConverterUI this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            protected ConversionResult doInBackground() throws Exception {
                APIResponse response = this.this$0.currencyService.getRates(source.code());
                Double rawRate = response.getRates().get(target.code());
                if (rawRate == null || !Double.isFinite(rawRate) || rawRate <= 0.0) {
                    throw new IllegalStateException("A valid rate for " + target.code() + " was not supplied by the API.");
                }
                BigDecimal rate = BigDecimal.valueOf(rawRate);
                BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                if (!Double.isFinite(converted.doubleValue())) {
                    throw new IllegalArgumentException("The converted amount is too large to store.");
                }
                Conversion record = new Conversion(0L, source.code(), target.code(), amount.doubleValue(), converted.doubleValue(), rawRate, LocalDateTime.now());
                this.this$0.historyDAO.save(record);
                return new ConversionResult(record, rate, converted, response.getLastUpdatedUtc());
            }

            @Override
            protected void done() {
                this.this$0.convertButton.setEnabled(true);
                this.this$0.convertButton.setText("CONVERT");
                try {
                    ConversionResult result = (ConversionResult)this.get();
                    this.this$0.resultAmount.setText(CurrencyConverterUI.formatCurrency(result.converted(), target));
                    this.this$0.exchangeRate.setText("1 " + source.code() + " = " + result.rate().stripTrailingZeros().toPlainString() + " " + target.code());
                    this.this$0.updatedAt.setText("Rates updated " + result.updatedAt());
                    this.this$0.statusLabel.setForeground(GREEN);
                    this.this$0.statusLabel.setText("Conversion saved to history");
                    this.this$0.refreshHistory();
                }
                catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    cause.printStackTrace(System.err);
                    this.this$0.statusLabel.setForeground(new Color(190, 55, 55));
                    this.this$0.statusLabel.setText("Conversion could not be completed");
                    this.this$0.showError(CurrencyConverterUI.explainConversionError(cause));
                }
            }
        }.execute();
    }

    private void swapCurrencies() {
        int sourceIndex = this.fromCurrency.getSelectedIndex();
        int targetIndex = this.toCurrency.getSelectedIndex();
        this.changingCurrencyPair = true;
        this.fromCurrency.setSelectedIndex(targetIndex);
        this.toCurrency.setSelectedIndex(sourceIndex);
        this.changingCurrencyPair = false;
        this.loadRateChart();
    }

    private void loadRateChart() {
        final Currency source = (Currency)this.fromCurrency.getSelectedItem();
        final Currency target = (Currency)this.toCurrency.getSelectedItem();
        if (source == null || target == null) {
            return;
        }
        final int requestId = ++this.chartRequestId;
        if (this.chartWorker != null) {
            this.chartWorker.cancel(true);
        }
        if (source.code().equals(target.code())) {
            this.rateChartPanel.showError("Choose two different currencies to view a rate trend.");
            this.chartStatusLabel.setText("Select a different target currency");
            return;
        }
        this.rateChartPanel.showLoading(source.code(), target.code());
        this.chartStatusLabel.setText("Loading the last 30 days for " + source.code() + " \u2192 " + target.code() + "\u2026");
        this.chartStatusLabel.setForeground(BLUE);
        this.chartWorker = new SwingWorker<List<RatePoint>, Void>(this){
            final /* synthetic */ CurrencyConverterUI this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            protected List<RatePoint> doInBackground() throws Exception {
                return this.this$0.currencyService.getHistoricalRates(source.code(), target.code());
            }

            @Override
            protected void done() {
                if (requestId != this.this$0.chartRequestId) {
                    return;
                }
                try {
                    List rates = (List)this.get();
                    RatePoint latest = (RatePoint)rates.get(rates.size() - 1);
                    this.this$0.rateChartPanel.showRates(source.code(), target.code(), rates);
                    this.this$0.chartStatusLabel.setForeground(MUTED);
                    this.this$0.chartStatusLabel.setText(String.format(Locale.US, "%d daily quotes \u2022 Latest: 1 %s = %.6g %s on %s", rates.size(), source.code(), latest.rate(), target.code(), latest.date()));
                }
                catch (CancellationException rates) {
                }
                catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    cause.printStackTrace(System.err);
                    this.this$0.rateChartPanel.showError("Historical reference rates are unavailable for this pair.");
                    this.this$0.chartStatusLabel.setForeground(new Color(190, 55, 55));
                    this.this$0.chartStatusLabel.setText("Could not load the 30-day rate graph: " + cause.getMessage() + " Conversion remains available.");
                }
            }
        };
        this.chartWorker.execute();
    }

    private void clearInputs() {
        this.amountField.setText("");
        this.resultAmount.setText("\u2014");
        this.exchangeRate.setText("Exchange rate will appear here");
        this.updatedAt.setText("Updated time will appear here");
        this.statusLabel.setForeground(MUTED);
        this.statusLabel.setText("Ready to convert");
        this.amountField.requestFocusInWindow();
    }

    private void clearHistory() {
        if (JOptionPane.showConfirmDialog(this, "Delete all saved conversion records?", "Clear history", 0, 2) != 0) {
            return;
        }
        try {
            this.historyDAO.clear();
            this.historyModel.setConversions(List.of());
        }
        catch (SQLException exception) {
            exception.printStackTrace(System.err);
            this.showError("Could not clear conversion history.\n" + exception.getMessage());
        }
    }

    private void refreshHistory() {
        try {
            this.historyModel.setConversions(this.historyDAO.findRecent());
        }
        catch (RuntimeException | SQLException exception) {
            exception.printStackTrace(System.err);
            this.statusLabel.setForeground(new Color(190, 55, 55));
            this.statusLabel.setText("Could not load conversion history");
            this.showError("Could not read conversion history.\n" + exception.getMessage());
        }
    }

    private static String explainConversionError(Throwable exception) {
        if (exception instanceof ConnectException || exception instanceof UnknownHostException || exception instanceof HttpTimeoutException) {
            return "Could not connect to the exchange-rate service. Check your internet connection and try again.";
        }
        if (exception instanceof SQLException) {
            return "The exchange rate was retrieved, but the conversion could not be saved to the database.\n" + exception.getMessage();
        }
        return "Conversion failed.\n" + exception.getMessage();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Currency Converter", 0);
    }

    private static String formatCurrency(BigDecimal amount, Currency currency) {
        return currency.symbol() + " " + String.format(Locale.US, "%,.2f", amount);
    }

    private void addFieldLabel(JPanel panel, String text, int row) {
        GridBagConstraints c = CurrencyConverterUI.constraints();
        c.gridy = row;
        c.fill = 2;
        c.insets = new Insets(0, 0, 7, 0);
        panel.add((Component)CurrencyConverterUI.sectionLabel(text), c);
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", 1, 11));
        label.setForeground(new Color(93, 111, 133));
        return label;
    }

    private static JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(227, 233, 241)), BorderFactory.createEmptyBorder(25, 25, 25, 25)));
        return panel;
    }

    private static void populateCurrencies(JComboBox<Currency> comboBox) {
        Currency.supportedCurrencies().forEach(comboBox::addItem);
    }

    private static void styleComboBox(JComboBox<Currency> comboBox) {
        comboBox.setFont(new Font("Segoe UI", 0, 14));
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT);
        comboBox.setPreferredSize(new Dimension(100, 46));
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(218, 228, 240)));
    }

    private static void styleSecondaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", 1, 11));
        button.setForeground(BLUE);
        button.setBackground(new Color(247, 250, 254));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(207, 221, 239)), BorderFactory.createEmptyBorder(10, 13, 10, 13)));
    }

    private static GridBagConstraints constraints() {
        return new GridBagConstraints();
    }

    private static final class PrimaryButton
    extends JButton {
        private PrimaryButton(String text) {
            super(text);
            this.setForeground(Color.WHITE);
            this.setBackground(BLUE);
            this.setFocusPainted(false);
            this.setBorderPainted(false);
            this.setContentAreaFilled(false);
            this.setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(this.isEnabled() ? BLUE : new Color(105, 158, 221));
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 8, 8);
            g.setFont(this.getFont());
            g.setColor(Color.WHITE);
            FontMetrics metrics = g.getFontMetrics();
            int x = (this.getWidth() - metrics.stringWidth(this.getText())) / 2;
            int y = (this.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(this.getText(), x, y);
            g.dispose();
        }
    }

    private static final class HistoryTableModel
    extends AbstractTableModel {
        private final String[] columns = new String[]{"FROM", "TO", "AMOUNT", "RESULT", "DATE & TIME"};
        private List<Conversion> conversions = List.of();

        private HistoryTableModel() {
        }

        public void setConversions(List<Conversion> conversions) {
            this.conversions = List.copyOf(conversions);
            this.fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return this.conversions.size();
        }

        @Override
        public int getColumnCount() {
            return this.columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return this.columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Conversion conversion = this.conversions.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> conversion.fromCurrency();
                case 1 -> conversion.toCurrency();
                case 2 -> String.format(Locale.US, "%,.2f", conversion.amount());
                case 3 -> String.format(Locale.US, "%,.2f", conversion.convertedAmount());
                case 4 -> conversion.conversionDate().atZone(ZoneId.systemDefault()).format(TABLE_DATE_FORMAT);
                default -> throw new IndexOutOfBoundsException("Invalid history column " + columnIndex);
            };
        }
    }

    private static final class InsetsZero {
        private static final Insets VALUE = new Insets(0, 0, 0, 0);

        private InsetsZero() {
        }
    }

    private static final class AlternatingRowRenderer
    extends DefaultTableCellRenderer {
        private AlternatingRowRenderer() {
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected) {
                component.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 252, 255));
            }
            this.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            return component;
        }
    }

    private record ConversionResult(Conversion record, BigDecimal rate, BigDecimal converted, String updatedAt) {
    }
}

