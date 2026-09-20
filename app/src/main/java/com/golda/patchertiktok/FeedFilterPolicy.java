package com.golda.patchertiktok;

import java.util.Locale;
import java.util.Set;

/**
 * Pure feed-filter decisions. Unit-tested without Android/Xposed.
 */
public final class FeedFilterPolicy {
    private FeedFilterPolicy() {
    }

    public static boolean matchesKeyword(String text, Set<String> keywords) {
        if (keywords == null || keywords.isEmpty() || text == null) return false;
        String haystack = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty() && haystack.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLongVideo(Integer durationMs, Integer thresholdSeconds) {
        if (durationMs == null || thresholdSeconds == null || thresholdSeconds <= 0) return false;
        return durationMs > thresholdSeconds * 1000L;
    }

    public static boolean isOutOfRange(Long value, long min, long max) {
        if (value == null) {
            // Missing metric: only reject when user set a non-default range.
            return min > 0 || max < Long.MAX_VALUE;
        }
        return value < min || value > max;
    }

    public static boolean shouldRemoveByMetrics(
            Long playCount,
            Long diggCount,
            boolean filterMetrics,
            long viewsMin,
            long viewsMax,
            long likesMin,
            long likesMax) {
        if (!filterMetrics) return false;
        return isOutOfRange(playCount, viewsMin, viewsMax)
                || isOutOfRange(diggCount, likesMin, likesMax);
    }

    public static boolean shouldRemove(
            boolean removeAds,
            boolean removeLive,
            boolean removeSuggested,
            boolean removePhoto,
            boolean removeAi,
            boolean removeLong,
            boolean removeMetrics,
            boolean isAd,
            boolean isLive,
            boolean isSuggested,
            boolean isPhoto,
            boolean isAi,
            boolean isLong,
            boolean metricsOut,
            boolean keywordHit) {
        return (removeAds && isAd)
                || (removeLive && isLive)
                || (removeSuggested && isSuggested)
                || (removePhoto && isPhoto)
                || (removeAi && isAi)
                || (removeLong && isLong)
                || (removeMetrics && metricsOut)
                || keywordHit;
    }
}
