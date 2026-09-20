package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class FeedRegionRewriterTest {
    private static final String[][] OVERRIDES = {
            {"region", "VN"},
            {"mcc_mnc", "45204"},
            {"app_language", "vi"},
            {"locale", "vi-VN"}
    };

    @Test
    public void detectsFeedPaths() {
        assertTrue(FeedRegionRewriter.containsFeedPath("https://api.tiktokv.com/aweme/v2/feed/?aid=1"));
        assertTrue(FeedRegionRewriter.containsFeedPath("https://x/aweme/v1/feed/"));
        assertFalse(FeedRegionRewriter.containsFeedPath("https://x/aweme/v2/comment/"));
    }

    @Test
    public void rewritesExistingQueryParams() {
        String url = "https://api.tiktokv.com/aweme/v2/feed/?aid=123&region=US&mcc_mnc=310260&device_platform=android";
        String out = FeedRegionRewriter.applyOverrides(url, OVERRIDES);
        assertTrue(out.contains("region=VN"));
        assertTrue(out.contains("mcc_mnc=45204"));
        assertTrue(out.contains("app_language=vi"));
        assertTrue(out.contains("device_platform=android"));
        assertFalse(out.contains("region=US"));
    }

    @Test
    public void appendsQueryWhenMissing() {
        String url = "https://api.tiktokv.com/aweme/v2/feed/";
        String out = FeedRegionRewriter.applyOverrides(url, OVERRIDES);
        assertTrue(out.startsWith("https://api.tiktokv.com/aweme/v2/feed/?"));
        assertTrue(out.contains("region=VN"));
        assertTrue(out.contains("locale=vi-VN"));
    }

    @Test
    public void preservesFragment() {
        String url = "https://api.tiktokv.com/aweme/v2/feed/?region=US#frag";
        String out = FeedRegionRewriter.applyOverrides(url, OVERRIDES);
        assertTrue(out.endsWith("#frag"));
        assertTrue(out.contains("region=VN"));
    }

    @Test
    public void parseAndEncodeRoundTrip() {
        Map<String, List<String>> params = FeedRegionRewriter.parseQuery("a=1&b=hello%20world");
        assertEquals("1", params.get("a").get(0));
        assertEquals("hello world", params.get("b").get(0));
        String encoded = FeedRegionRewriter.encodeQuery(params);
        assertTrue(encoded.contains("a=1"));
        assertTrue(encoded.contains("b=hello+world") || encoded.contains("b=hello%20world"));
    }

    @Test
    public void recognizesFeedQueryMaps() {
        Map<String, List<String>> params = FeedRegionRewriter.parseQuery("aid=1&device_platform=android&region=US");
        assertTrue(FeedRegionRewriter.looksLikeFeedQuery(params));
        assertFalse(FeedRegionRewriter.looksLikeFeedQuery(FeedRegionRewriter.parseQuery("foo=bar")));
    }
}
