package com.vincentwang.ecommerce.catalog.model;

import java.util.Locale;

/** An ISO 4217 currency code such as USD, stored upper case so prices and orders compare equal. */
public record CurrencyCode(String value) {

    public CurrencyCode {
        value = value.trim().toUpperCase(Locale.ROOT);
    }

    public static CurrencyCode of(String raw) {
        return new CurrencyCode(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
