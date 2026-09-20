package com.golda.patchertiktok;

import java.util.Collections;

/**
 * Built-in feature flags. Settings UI removed — these values are compiled in.
 * Toki-inspired feed filters stay enabled; Tako/reward entrances are always hidden.
 */
public final class ModuleConfig {
    public static final String PREFS = "module_settings";

    // Feature fields reused by MainHook / FeedFilterPolicy paths.
    public final boolean vietnamRegion = true;
    public final boolean vietnameseLanguage = true;
    public final boolean feedRegionOverride = true;
    public final boolean downloadNoWatermark = true;
    public final boolean hideFeedAds = true;
    public final boolean hideLive = true;
    public final boolean hideSuggested = true;
    public final boolean hideSplashAds = true;
    public final boolean forceSeekbar = true;
    public final boolean googleLoginFix = true;
    public final boolean hidePhotoPosts = false;
    public final boolean hideAiPosts = false;
    public final boolean hideLongPosts = false;
    public final int longPostSeconds = 60;
    public final boolean filterMetrics = false;
    public final long viewsMin = 0L;
    public final long viewsMax = Long.MAX_VALUE;
    public final long likesMin = 0L;
    public final long likesMax = Long.MAX_VALUE;
    public final java.util.Set<String> keywordBlacklist = Collections.emptySet();
    public final boolean playbackSpeedEnabled = false;
    public final float playbackSpeed = 1.0f;
    public final boolean hideAuthorAvatar = false;
    public final boolean hideAuthorInfo = false;
    public final boolean hideVideoDesc = false;
    public final boolean hideMusicTitle = false;
    public final boolean hideActionButtons = false;
    public final boolean hideTopNav = false;
    public final boolean hideSearch = false;
    public final boolean hideBottomNav = false;
    /** Always-on entrance cleaners (no UI). */
    public final boolean hideTakoIcon = true;
    public final boolean hideRewardEntrance = true;

    private static final ModuleConfig INSTANCE = new ModuleConfig();

    private ModuleConfig() {
    }

    public static ModuleConfig defaults() {
        return INSTANCE;
    }

    public static float sanitizeSpeed(float speed) {
        if (Float.isNaN(speed) || Float.isInfinite(speed)) return 1.0f;
        if (speed < 0.25f) return 0.25f;
        if (speed > 3.0f) return 3.0f;
        return speed;
    }

    public static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value == null ? "" : value.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public boolean anyFeedFilterEnabled() {
        return hideFeedAds || hideLive || hideSuggested || hidePhotoPosts
                || hideAiPosts || hideLongPosts || filterMetrics
                || !keywordBlacklist.isEmpty();
    }

    public boolean anyPurificationEnabled() {
        return hideAuthorAvatar || hideAuthorInfo || hideVideoDesc
                || hideMusicTitle || hideActionButtons || hideTopNav
                || hideSearch || hideBottomNav || hideTakoIcon || hideRewardEntrance;
    }

    @Override
    public String toString() {
        return "ModuleConfig{hardcoded vn=true, ads=true, photo/ai/long=false, speed="
                + playbackSpeedEnabled + ", tako=" + hideTakoIcon + ", reward=" + hideRewardEntrance + "}";
    }
}
