package com.kitbox.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 共享 Gson 实例。
 */
public final class Gsons {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private Gsons() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static Gson compact() {
        return COMPACT;
    }
}
