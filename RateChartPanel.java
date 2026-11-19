/*
 * Decompiled with CFR 0.152.
 */
package com.apex.currencyconverter;

import com.apex.currencyconverter.RatePoint;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.JPanel;

public final class RateChartPanel
extends JPanel {
    private static final Color BLUE = new Color(35, 111, 209);
    private static final Color TEXT = new Color(38, 52, 70);
    private static final Color MUTED = new Color(112, 128, 147);
    private static final Color GRID = new Color(231, 237, 245);
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private String baseCurrency = "";
    private String targetCurrency = "";
    private String message = "Select a currency pair to load its 30-day rate graph.";
    private List<RatePoint> points = List.of();

    public RateChartPanel() {
        this.setBackground(Color.WHITE);
        this.setOpaque(true);
    }

    public void showLoading(String base, String target) {
        this.baseCurrency = base;
        this.targetCurrency = target;
        this.points = List.of();
        this.message = "Loading daily exchange rates\u2026";
        this.repaint();
    }

    public void showRates(String base, String target, List<RatePoint> rates) {
        this.baseCurrency = base;
        this.targetCurrency = target;
        this.points = List.copyOf(rates);
        this.message = this.points.isEmpty() ? "No historical exchange rates are available." : "";
        this.repaint();
    }

    public void showError(String errorMessage) {
        this.points = List.of();
        this.message = errorMessage;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (this.points.isEmpty()) {
            this.paintMessage(g);
        } else {
            this.paintChart(g);
        }
        g.dispose();
    }

    private void paintMessage(Graphics2D g) {
        g.setFont(new Font("Segoe UI", 0, 14));
        g.setColor(MUTED);
        FontMetrics metrics = g.getFontMetrics();
        int x = Math.max(12, (this.getWidth() - metrics.stringWidth(this.message)) / 2);
        int y = Math.max(metrics.getAscent() + 8, this.getHeight() / 2);
        g.drawString(this.message, x, y);
    }

    private void paintChart(Graphics2D g) {
        int left = 78;
        int right = 22;
        int top = 38;
        int bottom = 54;
        int chartWidth = Math.max(1, this.getWidth() - left - right);
        int chartHeight = Math.max(1, this.getHeight() - top - bottom);
        double minimum = this.points.stream().mapToDouble(RatePoint::rate).min().orElse(0.0);
        double maximum = this.points.stream().mapToDouble(RatePoint::rate).max().orElse(1.0);
        double spread = maximum - minimum;
        double padding = spread == 0.0 ? Math.max(Math.abs(maximum) * 0.02, 0.01) : spread * 0.12;
        double lower = minimum - padding;
        double upper = maximum + padding;
        g.setFont(new Font("Segoe UI", 1, 14));
        g.setColor(TEXT);
        g.drawString("1 " + this.baseCurrency + "  =  " + this.targetCurrency, left, 22);
        g.setFont(new Font("Segoe UI", 0, 11));
        for (int tick = 0; tick <= 4; ++tick) {
            double value = upper - (upper - lower) * (double)tick / 4.0;
            int y = top + chartHeight * tick / 4;
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1.0f));
            g.drawLine(left, y, left + chartWidth, y);
            g.setColor(MUTED);
            g.drawString(String.format(Locale.US, "%.5g", value), 7, y + 4);
        }
        for (int index : new int[]{0, this.points.size() / 2, this.points.size() - 1}) {
            RatePoint point = this.points.get(index);
            int x = this.xFor(index, chartWidth, left);
            String label = point.date().format(DATE_LABEL);
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(MUTED);
            g.drawString(label, x - metrics.stringWidth(label) / 2, top + chartHeight + 23);
        }
        Path2D.Double line = new Path2D.Double();
        for (int index = 0; index < this.points.size(); ++index) {
            RatePoint point = this.points.get(index);
            double x = this.xFor(index, chartWidth, left);
            double y = (double)top + (upper - point.rate()) / (upper - lower) * (double)chartHeight;
            if (index == 0) {
                ((Path2D)line).moveTo(x, y);
                continue;
            }
            ((Path2D)line).lineTo(x, y);
        }
        g.setColor(BLUE);
        g.setStroke(new BasicStroke(2.5f, 1, 1));
        g.draw(line);
        RatePoint latest = this.points.get(this.points.size() - 1);
        double latestX = this.xFor(this.points.size() - 1, chartWidth, left);
        double latestY = (double)top + (upper - latest.rate()) / (upper - lower) * (double)chartHeight;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(latestX - 5.0, latestY - 5.0, 10.0, 10.0));
        g.setColor(BLUE);
        g.setStroke(new BasicStroke(2.0f));
        g.draw(new Ellipse2D.Double(latestX - 5.0, latestY - 5.0, 10.0, 10.0));
        g.setFont(new Font("Segoe UI", 1, 12));
        FontMetrics rateMetrics = g.getFontMetrics();
        String latestLabel = String.format(Locale.US, "Latest: %.6g", latest.rate());
        g.setColor(BLUE);
        g.drawString(latestLabel, this.getWidth() - right - rateMetrics.stringWidth(latestLabel), 22);
    }

    private int xFor(int index, int chartWidth, int left) {
        if (this.points.size() <= 1) {
            return left + chartWidth / 2;
        }
        return left + (int)Math.round((double)index / (double)(this.points.size() - 1) * (double)chartWidth);
    }
}

