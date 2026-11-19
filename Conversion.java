/*
 * Decompiled with CFR 0.152.
 */
package com.apex.currencyconverter;

import java.time.LocalDateTime;

public record Conversion(long id, String fromCurrency, String toCurrency, double amount, double convertedAmount, double exchangeRate, LocalDateTime conversionDate) {
}

