/*
 * Decompiled with CFR 0.152.
 */
package com.apex.currencyconverter;

import java.util.List;

public record Currency(String code, String name, String symbol) {
    private static final List<Currency> SUPPORTED = List.of(new Currency("USD", "US Dollar", "$"), new Currency("EUR", "Euro", "\u20ac"), new Currency("INR", "Indian Rupee", "\u20b9"), new Currency("GBP", "British Pound", "\u00a3"), new Currency("JPY", "Japanese Yen", "\u00a5"), new Currency("AUD", "Australian Dollar", "A$"), new Currency("CAD", "Canadian Dollar", "C$"), new Currency("SGD", "Singapore Dollar", "S$"), new Currency("CNY", "Chinese Yuan", "\u00a5"), new Currency("AED", "UAE Dirham", "\u062f.\u0625"));

    public static List<Currency> supportedCurrencies() {
        return SUPPORTED;
    }

    @Override
    public String toString() {
        return this.code + "  \u2014  " + this.name;
    }
}

