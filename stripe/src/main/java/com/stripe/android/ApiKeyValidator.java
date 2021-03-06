package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class ApiKeyValidator {
    private static final ApiKeyValidator DEFAULT = new ApiKeyValidator();

    @NonNull
    static ApiKeyValidator get() {
        return DEFAULT;
    }

    @NonNull
    String requireValid(@Nullable String apiKey) {
        if (apiKey == null || apiKey.trim().length() == 0) {
            throw new IllegalArgumentException("Invalid Publishable Key: " +
                    "You must use a valid Stripe API key to make a Stripe API request. " +
                    "For more info, see https://stripe.com/docs/keys");
        }

        if (apiKey.startsWith("sk_")) {
            throw new IllegalArgumentException("Invalid Publishable Key: " +
                    "You are using a secret key instead of a publishable one. " +
                    "For more info, see https://stripe.com/docs/keys");
        }

        return apiKey;
    }
}
