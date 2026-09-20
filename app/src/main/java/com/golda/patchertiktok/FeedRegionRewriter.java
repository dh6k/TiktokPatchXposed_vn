package com.golda.patchertiktok;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure URL-query rewriter for recommendation-feed region overrides.
 * TikTok 47.0.3 keeps feed query params on Request.url/uri, not a Map.
 */
public final class FeedRegionRewriter {
    private FeedRegionRewriter() {
    }

    public static boolean containsFeedPath(String url) {
        if (url == null) return false;
        return url.contains("/aweme/v2/feed/")
                || url.contains("/aweme/v1/feed/")
                || url.contains("/self_define/aweme/v2/feed/");
    }

    public static String applyOverrides(String url, String[][] overrides) {
        if (url == null || overrides == null || overrides.length == 0) {
            return url;
        }
        String fragment = "";
        String work = url;
        int hash = work.indexOf('#');
        if (hash >= 0) {
            fragment = work.substring(hash);
            work = work.substring(0, hash);
        }
        int q = work.indexOf('?');
        String base = q >= 0 ? work.substring(0, q) : work;
        String query = q >= 0 ? work.substring(q + 1) : "";
        Map<String, List<String>> params = parseQuery(query);
        for (String[] override : overrides) {
            if (override == null || override.length < 2) continue;
            putIgnoreCase(params, override[0], override[1]);
        }
        return base + "?" + encodeQuery(params) + fragment;
    }

    public static Map<String, List<String>> parseQuery(String query) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String rawKey = eq >= 0 ? pair.substring(0, eq) : pair;
            String rawValue = eq >= 0 ? pair.substring(eq + 1) : "";
            String key = decode(rawKey);
            String value = decode(rawValue);
            List<String> list = params.get(key);
            if (list == null) {
                list = new ArrayList<>();
                params.put(key, list);
            }
            list.add(value);
        }
        return params;
    }

    public static void putIgnoreCase(Map<String, List<String>> params, String key, String value) {
        if (params == null || key == null) return;
        String actualKey = key;
        for (String existing : params.keySet()) {
            if (existing != null && existing.equalsIgnoreCase(key)) {
                actualKey = existing;
                break;
            }
        }
        List<String> values = new ArrayList<>();
        values.add(value);
        params.put(actualKey, values);
    }

    public static String encodeQuery(Map<String, List<String>> params) {
        StringBuilder sb = new StringBuilder();
        if (params == null) return "";
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                if (!first) sb.append('&');
                first = false;
                sb.append(encode(entry.getKey())).append('=');
                continue;
            }
            for (String value : values) {
                if (!first) sb.append('&');
                first = false;
                sb.append(encode(entry.getKey()))
                        .append('=')
                        .append(value == null ? "" : encode(value));
            }
        }
        return sb.toString();
    }

    public static boolean looksLikeFeedQuery(Map<?, ?> candidate) {
        if (candidate == null || candidate.isEmpty()) return false;
        String[] keys = {
                "aid", "device_platform", "region", "app_language", "mcc_mnc",
                "carrier_region", "carrier_region_v2", "iid", "device_id",
                "version_code", "app_version", "locale"
        };
        for (Map.Entry<?, ?> entry : candidate.entrySet()) {
            Object key = entry.getKey();
            if (!(key instanceof String)) continue;
            String lower = ((String) key).toLowerCase(Locale.ROOT);
            for (String probe : keys) {
                if (lower.equals(probe) || lower.endsWith("_" + probe)) {
                    return true;
                }
            }
        }
        return candidate.size() >= 8;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return value;
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
