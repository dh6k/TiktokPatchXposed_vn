package com.golda.patchertiktok;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FeedFilterPolicyTest {
    @Test
    public void matchesKeywordsCaseInsensitively() {
        Set<String> keywords = new HashSet<>(Arrays.asList("spam", "giveaway"));
        assertTrue(FeedFilterPolicy.matchesKeyword("Win a SPAM prize", keywords));
        assertFalse(FeedFilterPolicy.matchesKeyword("normal dance", keywords));
        assertFalse(FeedFilterPolicy.matchesKeyword("x", Collections.emptySet()));
    }

    @Test
    public void detectsLongVideoFromMilliseconds() {
        assertTrue(FeedFilterPolicy.isLongVideo(61_000, 60));
        assertFalse(FeedFilterPolicy.isLongVideo(59_000, 60));
        assertFalse(FeedFilterPolicy.isLongVideo(null, 60));
    }

    @Test
    public void metricsRangeUsesParsedCounts() {
        assertTrue(FeedFilterPolicy.shouldRemoveByMetrics(
                10L, 10L, true, 1000L, Long.MAX_VALUE, 0L, Long.MAX_VALUE));
        assertFalse(FeedFilterPolicy.shouldRemoveByMetrics(
                5000L, 20L, true, 1000L, Long.MAX_VALUE, 0L, Long.MAX_VALUE));
        assertFalse(FeedFilterPolicy.shouldRemoveByMetrics(
                1L, 1L, false, 1000L, Long.MAX_VALUE, 0L, Long.MAX_VALUE));
        assertTrue(FeedFilterPolicy.shouldRemoveByMetrics(
                null, 1L, true, 1000L, Long.MAX_VALUE, 0L, Long.MAX_VALUE));
    }

    @Test
    public void combinesFlags() {
        assertTrue(FeedFilterPolicy.shouldRemove(
                true, false, false, false, false, false, false,
                true, false, false, false, false, false, false, false));
        assertTrue(FeedFilterPolicy.shouldRemove(
                false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, true));
        assertFalse(FeedFilterPolicy.shouldRemove(
                false, false, false, false, false, false, false,
                true, true, true, true, true, true, true, false));
    }
}
