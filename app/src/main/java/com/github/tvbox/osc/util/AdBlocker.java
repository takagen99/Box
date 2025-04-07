package com.github.tvbox.osc.util;

import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class AdBlocker {
    private static final List<String> AD_HOSTS = new ArrayList<>();

    public static void clear() {
        AD_HOSTS.clear();
    }

    public static boolean isEmpty() {
        return AD_HOSTS.isEmpty();
    }

    public static void addAdHost(String host) {
        AD_HOSTS.add(host);
    }
    public static boolean hasHost(String host) {
        return AD_HOSTS.contains(host);
    }
    public static boolean isAd(String url) {
        url = url.toLowerCase();
        for (String adHost : AD_HOSTS) {
            if (url.contains(adHost)) {
                return true;
            }
        }
        return false;
    }

    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }

}
