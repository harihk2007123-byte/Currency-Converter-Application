/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonParser
 */
package com.apex.currencyconverter;

import com.apex.currencyconverter.APIResponse;
import com.apex.currencyconverter.Currency;
import com.apex.currencyconverter.RatePoint;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CurrencyService {
    private static final String API_URL = "https://open.er-api.com/v6/latest/";
    private static final String HISTORY_API_URL = "https://api.frankfurter.dev/v1/";
    private static final int CHART_DAYS = 30;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public CurrencyService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    public APIResponse getRates(String baseCurrency) throws IOException, InterruptedException {
        APIResponse rates;
        String code = baseCurrency == null ? "" : baseCurrency.toUpperCase(Locale.ROOT);
        boolean supported = Currency.supportedCurrencies().stream().anyMatch(currency -> currency.code().equals(code));
        if (!supported) {
            throw new IllegalArgumentException("Unsupported source currency: " + baseCurrency);
        }
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL + code)).timeout(Duration.ofSeconds(20L)).header("Accept", "application/json").GET().build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Exchange-rate service returned HTTP " + response.statusCode() + ".");
        }
        try {
            rates = (APIResponse)this.gson.fromJson(response.body(), APIResponse.class);
        }
        catch (RuntimeException exception) {
            throw new IOException("Exchange-rate service returned invalid JSON.", exception);
        }
        if (rates == null || !"success".equalsIgnoreCase(rates.getResult()) || !code.equalsIgnoreCase(rates.getBaseCode()) || rates.getRates() == null || rates.getLastUpdatedUtc() == null || rates.getLastUpdatedUtc().isBlank()) {
            throw new IOException("Exchange-rate service returned an incomplete or unsuccessful response.");
        }
        return rates;
    }

    public List<RatePoint> getHistoricalRates(String baseCurrency, String targetCurrency) throws IOException, InterruptedException {
        String target;
        String base = CurrencyService.normalizeSupportedCurrency(baseCurrency);
        if (base.equals(target = CurrencyService.normalizeSupportedCurrency(targetCurrency))) {
            throw new IllegalArgumentException("Choose two different currencies to view a rate chart.");
        }
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(29L);
        URI uri = URI.create(HISTORY_API_URL + String.valueOf(startDate) + ".." + String.valueOf(endDate) + "?base=" + base + "&symbols=" + target);
        HttpRequest request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(20L)).header("Accept", "application/json").GET().build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Historical rate service returned HTTP " + response.statusCode() + ".");
        }
        return CurrencyService.parseHistoricalRates(response.body(), base, target);
    }

    static List<RatePoint> parseHistoricalRates(String body, String base, String target) throws IOException {
        JsonObject root;
        try {
            root = JsonParser.parseString((String)body).getAsJsonObject();
        }
        catch (JsonParseException | IllegalStateException exception) {
            throw new IOException("Historical rate service returned invalid JSON.", exception);
        }
        if (!(root.has("base") && root.get("base").isJsonPrimitive() && base.equalsIgnoreCase(root.get("base").getAsString()) && root.has("rates") && root.get("rates").isJsonObject())) {
            throw new IOException("Historical rate service returned an incomplete response.");
        }
        ArrayList<RatePoint> points = new ArrayList<RatePoint>();
        for (Map.Entry entry : root.getAsJsonObject("rates").entrySet()) {
            double rate;
            LocalDate date;
            try {
                date = LocalDate.parse((CharSequence)entry.getKey());
            }
            catch (DateTimeParseException exception) {
                throw new IOException("Historical rate service returned an invalid date.", exception);
            }
            JsonElement targetRate = ((JsonElement)entry.getValue()).isJsonObject() ? ((JsonElement)entry.getValue()).getAsJsonObject().get(target) : null;
            if (targetRate == null || !targetRate.isJsonPrimitive() || !targetRate.getAsJsonPrimitive().isNumber() || !Double.isFinite(rate = targetRate.getAsDouble()) || !(rate > 0.0)) continue;
            points.add(new RatePoint(date, rate));
        }
        points.sort(Comparator.comparing(RatePoint::date));
        if (points.isEmpty()) {
            throw new IOException("No historical rates are available for " + base + " to " + target + ".");
        }
        return List.copyOf(points);
    }

    private static String normalizeSupportedCurrency(String currencyCode) {
        String code = currencyCode == null ? "" : currencyCode.toUpperCase(Locale.ROOT);
        boolean supported = Currency.supportedCurrencies().stream().anyMatch(currency -> currency.code().equals(code));
        if (!supported) {
            throw new IllegalArgumentException("Unsupported currency: " + currencyCode);
        }
        return code;
    }
}

