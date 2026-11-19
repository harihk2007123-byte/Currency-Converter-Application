/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 */
package com.apex.currencyconverter;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public final class APIResponse {
    private String result;
    @SerializedName(value="base_code")
    private String baseCode;
    @SerializedName(value="time_last_update_utc")
    private String lastUpdatedUtc;
    @SerializedName(value="rates", alternate={"conversion_rates"})
    private Map<String, Double> rates;

    public String getResult() {
        return this.result;
    }

    public String getBaseCode() {
        return this.baseCode;
    }

    public String getLastUpdatedUtc() {
        return this.lastUpdatedUtc;
    }

    public Map<String, Double> getRates() {
        return this.rates;
    }
}

