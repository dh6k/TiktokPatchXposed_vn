package com.golda.patchertiktok;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Module settings shared between the settings app and the TikTok hook process.
 * Defaults preserve the previous hardcoded Vietnam-patch behavior.
 */
public final class ModuleConfig {
    public static final String MODULE_PACKAGE = "com.golda.patchertiktok";
    public static final String PREFS = "module_settings";
    public static final String AUTHORITY = "com.golda.patchertiktok.settings";
    public static final String METHOD_GET_CONFIG = "getConfig";
    public static final String METHOD_PING = "ping";
    public static final String MIRROR_FILE = "module_settings.properties";
    public static final String PUBLIC_MIRROR_NAME = "TiktokPatchXposed_config.properties";
    public static final String ACTION_CONFIG = "com.golda.patchertiktok.ACTION_CONFIG";
    public static final String EXTRA_CONFIG = "config";
    /** Prefs stored inside TikTok's data dir after a config broadcast. */
    public static final String RUNTIME_PREFS = "patcher_runtime_config";
    private static final String[] TARGET_PACKAGES = {
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill"
    };

    /** How the hook process last resolved config (for logs / diagnostics). */
    public static volatile String lastLoadSource = "unset";

    public static Uri settingsUri() {
        return Uri.parse("content://" + AUTHORITY);
    }

    public static final String KEY_VIETNAM_REGION = "vietnam_region";
    public static final String KEY_VIETNAMESE_LANGUAGE = "vietnamese_language";
    public static final String KEY_FEED_REGION_OVERRIDE = "feed_region_override";
    public static final String KEY_DOWNLOAD_NO_WATERMARK = "download_no_watermark";
    public static final String KEY_HIDE_FEED_ADS = "hide_feed_ads";
    public static final String KEY_HIDE_LIVE = "hide_live";
    public static final String KEY_HIDE_SUGGESTED = "hide_suggested";
    public static final String KEY_HIDE_SPLASH_ADS = "hide_splash_ads";
    public static final String KEY_FORCE_SEEKBAR = "force_seekbar";
    public static final String KEY_GOOGLE_LOGIN_FIX = "google_login_fix";
    public static final String KEY_HIDE_PHOTO_POSTS = "hide_photo_posts";
    public static final String KEY_HIDE_AI_POSTS = "hide_ai_posts";
    public static final String KEY_HIDE_LONG_POSTS = "hide_long_posts";
    public static final String KEY_LONG_POST_SECONDS = "long_post_seconds";
    public static final String KEY_FILTER_METRICS = "filter_metrics";
    public static final String KEY_VIEWS_MIN = "views_min";
    public static final String KEY_VIEWS_MAX = "views_max";
    public static final String KEY_LIKES_MIN = "likes_min";
    public static final String KEY_LIKES_MAX = "likes_max";
    public static final String KEY_KEYWORD_BLACKLIST = "keyword_blacklist";
    public static final String KEY_PLAYBACK_SPEED_ENABLED = "playback_speed_enabled";
    public static final String KEY_PLAYBACK_SPEED = "playback_speed";
    public static final String KEY_HIDE_AUTHOR_AVATAR = "hide_author_avatar";
    public static final String KEY_HIDE_AUTHOR_INFO = "hide_author_info";
    public static final String KEY_HIDE_VIDEO_DESC = "hide_video_desc";
    public static final String KEY_HIDE_MUSIC_TITLE = "hide_music_title";
    public static final String KEY_HIDE_ACTION_BUTTONS = "hide_action_buttons";
    public static final String KEY_HIDE_TOP_NAV = "hide_top_nav";
    public static final String KEY_HIDE_SEARCH = "hide_search";
    public static final String KEY_HIDE_BOTTOM_NAV = "hide_bottom_nav";

    public final boolean vietnamRegion;
    public final boolean vietnameseLanguage;
    public final boolean feedRegionOverride;
    public final boolean downloadNoWatermark;
    public final boolean hideFeedAds;
    public final boolean hideLive;
    public final boolean hideSuggested;
    public final boolean hideSplashAds;
    public final boolean forceSeekbar;
    public final boolean googleLoginFix;
    public final boolean hidePhotoPosts;
    public final boolean hideAiPosts;
    public final boolean hideLongPosts;
    public final int longPostSeconds;
    public final boolean filterMetrics;
    public final long viewsMin;
    public final long viewsMax;
    public final long likesMin;
    public final long likesMax;
    public final Set<String> keywordBlacklist;
    public final boolean playbackSpeedEnabled;
    public final float playbackSpeed;
    public final boolean hideAuthorAvatar;
    public final boolean hideAuthorInfo;
    public final boolean hideVideoDesc;
    public final boolean hideMusicTitle;
    public final boolean hideActionButtons;
    public final boolean hideTopNav;
    public final boolean hideSearch;
    public final boolean hideBottomNav;

    private ModuleConfig(
            boolean vietnamRegion,
            boolean vietnameseLanguage,
            boolean feedRegionOverride,
            boolean downloadNoWatermark,
            boolean hideFeedAds,
            boolean hideLive,
            boolean hideSuggested,
            boolean hideSplashAds,
            boolean forceSeekbar,
            boolean googleLoginFix,
            boolean hidePhotoPosts,
            boolean hideAiPosts,
            boolean hideLongPosts,
            int longPostSeconds,
            boolean filterMetrics,
            long viewsMin,
            long viewsMax,
            long likesMin,
            long likesMax,
            Set<String> keywordBlacklist,
            boolean playbackSpeedEnabled,
            float playbackSpeed,
            boolean hideAuthorAvatar,
            boolean hideAuthorInfo,
            boolean hideVideoDesc,
            boolean hideMusicTitle,
            boolean hideActionButtons,
            boolean hideTopNav,
            boolean hideSearch,
            boolean hideBottomNav) {
        this.vietnamRegion = vietnamRegion;
        this.vietnameseLanguage = vietnameseLanguage;
        this.feedRegionOverride = feedRegionOverride;
        this.downloadNoWatermark = downloadNoWatermark;
        this.hideFeedAds = hideFeedAds;
        this.hideLive = hideLive;
        this.hideSuggested = hideSuggested;
        this.hideSplashAds = hideSplashAds;
        this.forceSeekbar = forceSeekbar;
        this.googleLoginFix = googleLoginFix;
        this.hidePhotoPosts = hidePhotoPosts;
        this.hideAiPosts = hideAiPosts;
        this.hideLongPosts = hideLongPosts;
        this.longPostSeconds = longPostSeconds;
        this.filterMetrics = filterMetrics;
        this.viewsMin = viewsMin;
        this.viewsMax = viewsMax;
        this.likesMin = likesMin;
        this.likesMax = likesMax;
        this.keywordBlacklist = keywordBlacklist;
        this.playbackSpeedEnabled = playbackSpeedEnabled;
        this.playbackSpeed = playbackSpeed;
        this.hideAuthorAvatar = hideAuthorAvatar;
        this.hideAuthorInfo = hideAuthorInfo;
        this.hideVideoDesc = hideVideoDesc;
        this.hideMusicTitle = hideMusicTitle;
        this.hideActionButtons = hideActionButtons;
        this.hideTopNav = hideTopNav;
        this.hideSearch = hideSearch;
        this.hideBottomNav = hideBottomNav;
    }

    public static ModuleConfig defaults() {
        return new ModuleConfig(
                true, true, true, true,
                true, true, true, true, true, true,
                false, false, false, 60,
                false, 0L, Long.MAX_VALUE, 0L, Long.MAX_VALUE,
                Collections.emptySet(),
                false, 1.0f,
                false, false, false, false, false,
                false, false, false
        );
    }

    public static ModuleConfig fromPreferences(SharedPreferences prefs) {
        return new ModuleConfig(
                prefs.getBoolean(KEY_VIETNAM_REGION, true),
                prefs.getBoolean(KEY_VIETNAMESE_LANGUAGE, true),
                prefs.getBoolean(KEY_FEED_REGION_OVERRIDE, true),
                prefs.getBoolean(KEY_DOWNLOAD_NO_WATERMARK, true),
                prefs.getBoolean(KEY_HIDE_FEED_ADS, true),
                prefs.getBoolean(KEY_HIDE_LIVE, true),
                prefs.getBoolean(KEY_HIDE_SUGGESTED, true),
                prefs.getBoolean(KEY_HIDE_SPLASH_ADS, true),
                prefs.getBoolean(KEY_FORCE_SEEKBAR, true),
                prefs.getBoolean(KEY_GOOGLE_LOGIN_FIX, true),
                prefs.getBoolean(KEY_HIDE_PHOTO_POSTS, false),
                prefs.getBoolean(KEY_HIDE_AI_POSTS, false),
                prefs.getBoolean(KEY_HIDE_LONG_POSTS, false),
                positiveInt(prefs.getString(KEY_LONG_POST_SECONDS, "60"), 60),
                prefs.getBoolean(KEY_FILTER_METRICS, false),
                nonNegativeLong(prefs.getString(KEY_VIEWS_MIN, "0"), 0L),
                positiveLong(prefs.getString(KEY_VIEWS_MAX, ""), Long.MAX_VALUE),
                nonNegativeLong(prefs.getString(KEY_LIKES_MIN, "0"), 0L),
                positiveLong(prefs.getString(KEY_LIKES_MAX, ""), Long.MAX_VALUE),
                parseKeywords(prefs.getString(KEY_KEYWORD_BLACKLIST, "")),
                prefs.getBoolean(KEY_PLAYBACK_SPEED_ENABLED, false),
                sanitizeSpeed(parseFloat(prefs.getString(KEY_PLAYBACK_SPEED, "1.25"), 1.25f)),
                prefs.getBoolean(KEY_HIDE_AUTHOR_AVATAR, false),
                prefs.getBoolean(KEY_HIDE_AUTHOR_INFO, false),
                prefs.getBoolean(KEY_HIDE_VIDEO_DESC, false),
                prefs.getBoolean(KEY_HIDE_MUSIC_TITLE, false),
                prefs.getBoolean(KEY_HIDE_ACTION_BUTTONS, false),
                prefs.getBoolean(KEY_HIDE_TOP_NAV, false),
                prefs.getBoolean(KEY_HIDE_SEARCH, false),
                prefs.getBoolean(KEY_HIDE_BOTTOM_NAV, false)
        );
    }

    public static ModuleConfig fromBundle(Bundle bundle) {
        if (bundle == null) return defaults();
        return new ModuleConfig(
                bundle.getBoolean(KEY_VIETNAM_REGION, true),
                bundle.getBoolean(KEY_VIETNAMESE_LANGUAGE, true),
                bundle.getBoolean(KEY_FEED_REGION_OVERRIDE, true),
                bundle.getBoolean(KEY_DOWNLOAD_NO_WATERMARK, true),
                bundle.getBoolean(KEY_HIDE_FEED_ADS, true),
                bundle.getBoolean(KEY_HIDE_LIVE, true),
                bundle.getBoolean(KEY_HIDE_SUGGESTED, true),
                bundle.getBoolean(KEY_HIDE_SPLASH_ADS, true),
                bundle.getBoolean(KEY_FORCE_SEEKBAR, true),
                bundle.getBoolean(KEY_GOOGLE_LOGIN_FIX, true),
                bundle.getBoolean(KEY_HIDE_PHOTO_POSTS, false),
                bundle.getBoolean(KEY_HIDE_AI_POSTS, false),
                bundle.getBoolean(KEY_HIDE_LONG_POSTS, false),
                positiveInt(String.valueOf(bundle.getInt(KEY_LONG_POST_SECONDS, 60)), 60),
                bundle.getBoolean(KEY_FILTER_METRICS, false),
                bundle.getLong(KEY_VIEWS_MIN, 0L),
                bundle.getLong(KEY_VIEWS_MAX, Long.MAX_VALUE),
                bundle.getLong(KEY_LIKES_MIN, 0L),
                bundle.getLong(KEY_LIKES_MAX, Long.MAX_VALUE),
                parseKeywords(bundle.getString(KEY_KEYWORD_BLACKLIST, "")),
                bundle.getBoolean(KEY_PLAYBACK_SPEED_ENABLED, false),
                sanitizeSpeed((float) bundle.getDouble(KEY_PLAYBACK_SPEED, 1.25)),
                bundle.getBoolean(KEY_HIDE_AUTHOR_AVATAR, false),
                bundle.getBoolean(KEY_HIDE_AUTHOR_INFO, false),
                bundle.getBoolean(KEY_HIDE_VIDEO_DESC, false),
                bundle.getBoolean(KEY_HIDE_MUSIC_TITLE, false),
                bundle.getBoolean(KEY_HIDE_ACTION_BUTTONS, false),
                bundle.getBoolean(KEY_HIDE_TOP_NAV, false),
                bundle.getBoolean(KEY_HIDE_SEARCH, false),
                bundle.getBoolean(KEY_HIDE_BOTTOM_NAV, false)
        );
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_VIETNAM_REGION, vietnamRegion);
        bundle.putBoolean(KEY_VIETNAMESE_LANGUAGE, vietnameseLanguage);
        bundle.putBoolean(KEY_FEED_REGION_OVERRIDE, feedRegionOverride);
        bundle.putBoolean(KEY_DOWNLOAD_NO_WATERMARK, downloadNoWatermark);
        bundle.putBoolean(KEY_HIDE_FEED_ADS, hideFeedAds);
        bundle.putBoolean(KEY_HIDE_LIVE, hideLive);
        bundle.putBoolean(KEY_HIDE_SUGGESTED, hideSuggested);
        bundle.putBoolean(KEY_HIDE_SPLASH_ADS, hideSplashAds);
        bundle.putBoolean(KEY_FORCE_SEEKBAR, forceSeekbar);
        bundle.putBoolean(KEY_GOOGLE_LOGIN_FIX, googleLoginFix);
        bundle.putBoolean(KEY_HIDE_PHOTO_POSTS, hidePhotoPosts);
        bundle.putBoolean(KEY_HIDE_AI_POSTS, hideAiPosts);
        bundle.putBoolean(KEY_HIDE_LONG_POSTS, hideLongPosts);
        bundle.putInt(KEY_LONG_POST_SECONDS, longPostSeconds);
        bundle.putBoolean(KEY_FILTER_METRICS, filterMetrics);
        bundle.putLong(KEY_VIEWS_MIN, viewsMin);
        bundle.putLong(KEY_VIEWS_MAX, viewsMax);
        bundle.putLong(KEY_LIKES_MIN, likesMin);
        bundle.putLong(KEY_LIKES_MAX, likesMax);
        bundle.putString(KEY_KEYWORD_BLACKLIST, joinKeywords(keywordBlacklist));
        bundle.putBoolean(KEY_PLAYBACK_SPEED_ENABLED, playbackSpeedEnabled);
        bundle.putDouble(KEY_PLAYBACK_SPEED, playbackSpeed);
        bundle.putBoolean(KEY_HIDE_AUTHOR_AVATAR, hideAuthorAvatar);
        bundle.putBoolean(KEY_HIDE_AUTHOR_INFO, hideAuthorInfo);
        bundle.putBoolean(KEY_HIDE_VIDEO_DESC, hideVideoDesc);
        bundle.putBoolean(KEY_HIDE_MUSIC_TITLE, hideMusicTitle);
        bundle.putBoolean(KEY_HIDE_ACTION_BUTTONS, hideActionButtons);
        bundle.putBoolean(KEY_HIDE_TOP_NAV, hideTopNav);
        bundle.putBoolean(KEY_HIDE_SEARCH, hideSearch);
        bundle.putBoolean(KEY_HIDE_BOTTOM_NAV, hideBottomNav);
        return bundle;
    }

    public boolean anyFeedFilterEnabled() {
        return hideFeedAds || hideLive || hideSuggested || hidePhotoPosts
                || hideAiPosts || hideLongPosts || filterMetrics
                || !keywordBlacklist.isEmpty();
    }

    public boolean anyPurificationEnabled() {
        return hideAuthorAvatar || hideAuthorInfo || hideVideoDesc
                || hideMusicTitle || hideActionButtons || hideTopNav
                || hideSearch || hideBottomNav;
    }

    /**
     * Resolve settings from the TikTok/hook process.
     * Many Xposed/LSPosed builds cannot read module prefs or the module
     * ContentProvider from TikTok (package visibility / no remote prefs).
     * Order: runtime prefs in TikTok (set after broadcast) -> Xposed prefs
     * -> provider -> package-context -> public files -> defaults.
     */
    public static ModuleConfig load(Context hostContext) {
        StringBuilder trace = new StringBuilder();

        ModuleConfig fromRuntime = loadRuntime(hostContext);
        if (fromRuntime != null) {
            lastLoadSource = "runtime-prefs|" + trace;
            return fromRuntime;
        }
        trace.append("runtime=empty;");

        ModuleConfig fromXposed = loadViaXSharedPreferences(trace);
        if (fromXposed != null) {
            lastLoadSource = "xposed|" + trace;
            return fromXposed;
        }

        ModuleConfig fromProvider = loadViaProvider(hostContext, trace);
        if (fromProvider != null) {
            lastLoadSource = "provider|" + trace;
            return fromProvider;
        }

        ModuleConfig fromPackage = loadViaPackageContext(hostContext, trace);
        if (fromPackage != null) {
            lastLoadSource = "package-context|" + trace;
            return fromPackage;
        }

        ModuleConfig fromPublic = loadFromPublicFiles(trace);
        if (fromPublic != null) {
            lastLoadSource = "public-file|" + trace;
            return fromPublic;
        }

        lastLoadSource = "defaults|" + trace;
        return defaults();
    }

    public static void saveRuntime(Context context, Bundle bundle) {
        if (context == null || bundle == null) return;
        SharedPreferences.Editor editor = context
                .getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE)
                .edit();
        editor.putBoolean(KEY_VIETNAM_REGION, bundle.getBoolean(KEY_VIETNAM_REGION, true));
        editor.putBoolean(KEY_VIETNAMESE_LANGUAGE, bundle.getBoolean(KEY_VIETNAMESE_LANGUAGE, true));
        editor.putBoolean(KEY_FEED_REGION_OVERRIDE, bundle.getBoolean(KEY_FEED_REGION_OVERRIDE, true));
        editor.putBoolean(KEY_DOWNLOAD_NO_WATERMARK, bundle.getBoolean(KEY_DOWNLOAD_NO_WATERMARK, true));
        editor.putBoolean(KEY_HIDE_FEED_ADS, bundle.getBoolean(KEY_HIDE_FEED_ADS, true));
        editor.putBoolean(KEY_HIDE_LIVE, bundle.getBoolean(KEY_HIDE_LIVE, true));
        editor.putBoolean(KEY_HIDE_SUGGESTED, bundle.getBoolean(KEY_HIDE_SUGGESTED, true));
        editor.putBoolean(KEY_HIDE_SPLASH_ADS, bundle.getBoolean(KEY_HIDE_SPLASH_ADS, true));
        editor.putBoolean(KEY_FORCE_SEEKBAR, bundle.getBoolean(KEY_FORCE_SEEKBAR, true));
        editor.putBoolean(KEY_GOOGLE_LOGIN_FIX, bundle.getBoolean(KEY_GOOGLE_LOGIN_FIX, true));
        editor.putBoolean(KEY_HIDE_PHOTO_POSTS, bundle.getBoolean(KEY_HIDE_PHOTO_POSTS, false));
        editor.putBoolean(KEY_HIDE_AI_POSTS, bundle.getBoolean(KEY_HIDE_AI_POSTS, false));
        editor.putBoolean(KEY_HIDE_LONG_POSTS, bundle.getBoolean(KEY_HIDE_LONG_POSTS, false));
        editor.putString(KEY_LONG_POST_SECONDS, String.valueOf(bundle.getInt(KEY_LONG_POST_SECONDS, 60)));
        editor.putBoolean(KEY_FILTER_METRICS, bundle.getBoolean(KEY_FILTER_METRICS, false));
        editor.putString(KEY_VIEWS_MIN, String.valueOf(bundle.getLong(KEY_VIEWS_MIN, 0L)));
        long viewsMax = bundle.getLong(KEY_VIEWS_MAX, Long.MAX_VALUE);
        editor.putString(KEY_VIEWS_MAX, viewsMax == Long.MAX_VALUE ? "" : String.valueOf(viewsMax));
        editor.putString(KEY_LIKES_MIN, String.valueOf(bundle.getLong(KEY_LIKES_MIN, 0L)));
        long likesMax = bundle.getLong(KEY_LIKES_MAX, Long.MAX_VALUE);
        editor.putString(KEY_LIKES_MAX, likesMax == Long.MAX_VALUE ? "" : String.valueOf(likesMax));
        editor.putString(KEY_KEYWORD_BLACKLIST, bundle.getString(KEY_KEYWORD_BLACKLIST, ""));
        editor.putBoolean(KEY_PLAYBACK_SPEED_ENABLED, bundle.getBoolean(KEY_PLAYBACK_SPEED_ENABLED, false));
        editor.putString(KEY_PLAYBACK_SPEED, String.valueOf(bundle.getDouble(KEY_PLAYBACK_SPEED, 1.25)));
        editor.putBoolean(KEY_HIDE_AUTHOR_AVATAR, bundle.getBoolean(KEY_HIDE_AUTHOR_AVATAR, false));
        editor.putBoolean(KEY_HIDE_AUTHOR_INFO, bundle.getBoolean(KEY_HIDE_AUTHOR_INFO, false));
        editor.putBoolean(KEY_HIDE_VIDEO_DESC, bundle.getBoolean(KEY_HIDE_VIDEO_DESC, false));
        editor.putBoolean(KEY_HIDE_MUSIC_TITLE, bundle.getBoolean(KEY_HIDE_MUSIC_TITLE, false));
        editor.putBoolean(KEY_HIDE_ACTION_BUTTONS, bundle.getBoolean(KEY_HIDE_ACTION_BUTTONS, false));
        editor.putBoolean(KEY_HIDE_TOP_NAV, bundle.getBoolean(KEY_HIDE_TOP_NAV, false));
        editor.putBoolean(KEY_HIDE_SEARCH, bundle.getBoolean(KEY_HIDE_SEARCH, false));
        editor.putBoolean(KEY_HIDE_BOTTOM_NAV, bundle.getBoolean(KEY_HIDE_BOTTOM_NAV, false));
        editor.commit();
    }

    public static ModuleConfig loadRuntime(Context context) {
        if (context == null) return null;
        try {
            SharedPreferences prefs = context.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE);
            if (isEmptyPrefs(prefs)) return null;
            return fromPreferences(prefs);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Broadcast current settings into running TikTok processes. */
    public static boolean broadcastConfig(Context moduleContext, SharedPreferences prefs) {
        if (moduleContext == null || prefs == null) return false;
        Bundle bundle = fromPreferences(prefs).toBundle();
        bundle.putBoolean("ok", true);
        int sent = 0;
        for (String pkg : TARGET_PACKAGES) {
            try {
                Intent intent = new Intent(ACTION_CONFIG);
                intent.setPackage(pkg);
                intent.putExtra(EXTRA_CONFIG, bundle);
                moduleContext.sendBroadcast(intent);
                sent++;
            } catch (Throwable ignored) {
            }
        }
        return sent > 0;
    }

    public static boolean writePublicMirror(Context moduleContext, SharedPreferences prefs) {
        if (moduleContext == null || prefs == null) return false;
        Properties properties = propertiesFromPrefs(prefs);
        // MediaStore Downloads (Android 10+)
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                ContentResolver cr = moduleContext.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, PUBLIC_MIRROR_NAME);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream out = cr.openOutputStream(uri)) {
                        if (out != null) {
                            properties.store(out, "TiktokPatchXposed");
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        File[] candidates = publicMirrorFiles();
        for (File file : candidates) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                }
                try (FileOutputStream out = new FileOutputStream(file)) {
                    properties.store(out, "TiktokPatchXposed");
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static File[] publicMirrorFiles() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File root = Environment.getExternalStorageDirectory();
        return new File[]{
                new File(downloads, PUBLIC_MIRROR_NAME),
                new File(docs, PUBLIC_MIRROR_NAME),
                new File(root, PUBLIC_MIRROR_NAME),
                new File(root, "TiktokPatchXposed/" + PUBLIC_MIRROR_NAME)
        };
    }

    static ModuleConfig loadFromPublicFiles(StringBuilder trace) {
        File[] files = publicMirrorFiles();
        boolean sawFile = false;
        for (File file : files) {
            boolean exists = false;
            boolean readable = false;
            try {
                exists = file.isFile();
                readable = exists && file.canRead();
            } catch (Throwable ignored) {
            }
            if (trace != null) {
                trace.append("public[").append(file.getName())
                        .append("] exists=").append(exists)
                        .append(" readable=").append(readable).append(';');
            }
            if (!readable) continue;
            try {
                if (file.length() <= 0) continue;
                Properties properties = new Properties();
                try (FileInputStream in = new FileInputStream(file)) {
                    properties.load(in);
                }
                if (!properties.isEmpty()) {
                    sawFile = true;
                    return fromProperties(properties);
                }
            } catch (Throwable t) {
                if (trace != null) {
                    trace.append("public-read-err:").append(t.getClass().getSimpleName()).append(';');
                }
            }
        }
        return sawFile ? null : null;
    }

    private static Properties propertiesFromPrefs(SharedPreferences prefs) {
        Properties properties = new Properties();
        properties.setProperty(KEY_VIETNAM_REGION, String.valueOf(prefs.getBoolean(KEY_VIETNAM_REGION, true)));
        properties.setProperty(KEY_VIETNAMESE_LANGUAGE, String.valueOf(prefs.getBoolean(KEY_VIETNAMESE_LANGUAGE, true)));
        properties.setProperty(KEY_FEED_REGION_OVERRIDE, String.valueOf(prefs.getBoolean(KEY_FEED_REGION_OVERRIDE, true)));
        properties.setProperty(KEY_DOWNLOAD_NO_WATERMARK, String.valueOf(prefs.getBoolean(KEY_DOWNLOAD_NO_WATERMARK, true)));
        properties.setProperty(KEY_HIDE_FEED_ADS, String.valueOf(prefs.getBoolean(KEY_HIDE_FEED_ADS, true)));
        properties.setProperty(KEY_HIDE_LIVE, String.valueOf(prefs.getBoolean(KEY_HIDE_LIVE, true)));
        properties.setProperty(KEY_HIDE_SUGGESTED, String.valueOf(prefs.getBoolean(KEY_HIDE_SUGGESTED, true)));
        properties.setProperty(KEY_HIDE_SPLASH_ADS, String.valueOf(prefs.getBoolean(KEY_HIDE_SPLASH_ADS, true)));
        properties.setProperty(KEY_FORCE_SEEKBAR, String.valueOf(prefs.getBoolean(KEY_FORCE_SEEKBAR, true)));
        properties.setProperty(KEY_GOOGLE_LOGIN_FIX, String.valueOf(prefs.getBoolean(KEY_GOOGLE_LOGIN_FIX, true)));
        properties.setProperty(KEY_HIDE_PHOTO_POSTS, String.valueOf(prefs.getBoolean(KEY_HIDE_PHOTO_POSTS, false)));
        properties.setProperty(KEY_HIDE_AI_POSTS, String.valueOf(prefs.getBoolean(KEY_HIDE_AI_POSTS, false)));
        properties.setProperty(KEY_HIDE_LONG_POSTS, String.valueOf(prefs.getBoolean(KEY_HIDE_LONG_POSTS, false)));
        properties.setProperty(KEY_LONG_POST_SECONDS, prefs.getString(KEY_LONG_POST_SECONDS, "60"));
        properties.setProperty(KEY_FILTER_METRICS, String.valueOf(prefs.getBoolean(KEY_FILTER_METRICS, false)));
        properties.setProperty(KEY_VIEWS_MIN, prefs.getString(KEY_VIEWS_MIN, "0"));
        properties.setProperty(KEY_VIEWS_MAX, prefs.getString(KEY_VIEWS_MAX, ""));
        properties.setProperty(KEY_LIKES_MIN, prefs.getString(KEY_LIKES_MIN, "0"));
        properties.setProperty(KEY_LIKES_MAX, prefs.getString(KEY_LIKES_MAX, ""));
        properties.setProperty(KEY_KEYWORD_BLACKLIST, prefs.getString(KEY_KEYWORD_BLACKLIST, ""));
        properties.setProperty(KEY_PLAYBACK_SPEED_ENABLED, String.valueOf(prefs.getBoolean(KEY_PLAYBACK_SPEED_ENABLED, false)));
        properties.setProperty(KEY_PLAYBACK_SPEED, prefs.getString(KEY_PLAYBACK_SPEED, "1.25"));
        properties.setProperty(KEY_HIDE_AUTHOR_AVATAR, String.valueOf(prefs.getBoolean(KEY_HIDE_AUTHOR_AVATAR, false)));
        properties.setProperty(KEY_HIDE_AUTHOR_INFO, String.valueOf(prefs.getBoolean(KEY_HIDE_AUTHOR_INFO, false)));
        properties.setProperty(KEY_HIDE_VIDEO_DESC, String.valueOf(prefs.getBoolean(KEY_HIDE_VIDEO_DESC, false)));
        properties.setProperty(KEY_HIDE_MUSIC_TITLE, String.valueOf(prefs.getBoolean(KEY_HIDE_MUSIC_TITLE, false)));
        properties.setProperty(KEY_HIDE_ACTION_BUTTONS, String.valueOf(prefs.getBoolean(KEY_HIDE_ACTION_BUTTONS, false)));
        properties.setProperty(KEY_HIDE_TOP_NAV, String.valueOf(prefs.getBoolean(KEY_HIDE_TOP_NAV, false)));
        properties.setProperty(KEY_HIDE_SEARCH, String.valueOf(prefs.getBoolean(KEY_HIDE_SEARCH, false)));
        properties.setProperty(KEY_HIDE_BOTTOM_NAV, String.valueOf(prefs.getBoolean(KEY_HIDE_BOTTOM_NAV, false)));
        return properties;
    }

    /** Used by the settings app itself (same UID as provider + prefs). */
    public static ModuleConfig loadFromProvider(Context context) {
        StringBuilder trace = new StringBuilder();
        ModuleConfig config = loadViaProvider(context, trace);
        if (config != null) {
            lastLoadSource = "provider-self";
            return config;
        }
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!prefs.getAll().isEmpty()) {
                lastLoadSource = "local-prefs";
                return fromPreferences(prefs);
            }
        } catch (RuntimeException ignored) {
        }
        lastLoadSource = "defaults|" + trace;
        return defaults();
    }

    private static ModuleConfig loadViaXSharedPreferences(StringBuilder trace) {
        try {
            Class<?> clazz = Class.forName("de.robv.android.xposed.XSharedPreferences");
            Object prefs = clazz
                    .getConstructor(String.class, String.class)
                    .newInstance(MODULE_PACKAGE, PREFS);
            try {
                clazz.getMethod("makeWorldReadable").invoke(prefs);
            } catch (Throwable ignored) {
            }
            try {
                clazz.getMethod("reload").invoke(prefs);
            } catch (Throwable ignored) {
            }
            if (!(prefs instanceof SharedPreferences)) {
                if (trace != null) {
                    trace.append("xposed=not-sharedprefs:").append(prefs == null ? "null" : prefs.getClass().getName()).append(';');
                }
                return null;
            }
            SharedPreferences sp = (SharedPreferences) prefs;
            int size = -1;
            try {
                size = sp.getAll() == null ? -1 : sp.getAll().size();
            } catch (Throwable ignored) {
            }
            if (size <= 0) {
                if (trace != null) trace.append("xposed=empty(").append(size).append(");");
                return null;
            }
            if (trace != null) trace.append("xposed=ok(").append(size).append(");");
            return fromPreferences(sp);
        } catch (Throwable t) {
            if (trace != null) trace.append("xposed=err:").append(t.getClass().getSimpleName()).append(';');
            return null;
        }
    }

    private static ModuleConfig loadViaProvider(Context context, StringBuilder trace) {
        if (context == null) return null;
        try {
            Bundle result = context.getContentResolver().call(
                    settingsUri(), METHOD_GET_CONFIG, null, null);
            if (result == null) {
                if (trace != null) trace.append("provider=null;");
                return null;
            }
            if (!result.getBoolean("ok", false) && !result.containsKey(KEY_VIETNAM_REGION)) {
                if (trace != null) trace.append("provider=not-ok;");
                return null;
            }
            return fromBundle(result);
        } catch (RuntimeException t) {
            if (trace != null) trace.append("provider=err:").append(t.getClass().getSimpleName()).append(';');
            return null;
        }
    }

    private static ModuleConfig loadViaPackageContext(Context hostContext, StringBuilder trace) {
        if (hostContext == null) return null;
        try {
            Context moduleContext = hostContext.createPackageContext(
                    MODULE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences prefs = moduleContext.getSharedPreferences(
                    PREFS, Context.MODE_PRIVATE);
            if (!isEmptyPrefs(prefs)) {
                return fromPreferences(prefs);
            }
            Properties properties = readMirror(moduleContext);
            if (properties != null && !properties.isEmpty()) {
                return fromProperties(properties);
            }
            if (trace != null) trace.append("package-context=empty;");
        } catch (Throwable t) {
            if (trace != null) {
                trace.append("package-context=err:").append(t.getClass().getSimpleName()).append(';');
            }
        }
        return null;
    }

    private static boolean isEmptyPrefs(SharedPreferences prefs) {
        try {
            Map<String, ?> all = prefs.getAll();
            return all == null || all.isEmpty();
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void writeMirror(Context moduleContext, SharedPreferences prefs) {
        if (moduleContext == null || prefs == null) return;
        Properties properties = new Properties();
        properties.setProperty(KEY_VIETNAM_REGION, String.valueOf(prefs.getBoolean(KEY_VIETNAM_REGION, true)));
        properties.setProperty(KEY_VIETNAMESE_LANGUAGE, String.valueOf(prefs.getBoolean(KEY_VIETNAMESE_LANGUAGE, true)));
        properties.setProperty(KEY_FEED_REGION_OVERRIDE, String.valueOf(prefs.getBoolean(KEY_FEED_REGION_OVERRIDE, true)));
        properties.setProperty(KEY_DOWNLOAD_NO_WATERMARK, String.valueOf(prefs.getBoolean(KEY_DOWNLOAD_NO_WATERMARK, true)));
        properties.setProperty(KEY_HIDE_FEED_ADS, String.valueOf(prefs.getBoolean(KEY_HIDE_FEED_ADS, true)));
        properties.setProperty(KEY_HIDE_LIVE, String.valueOf(prefs.getBoolean(KEY_HIDE_LIVE, true)));
        properties.setProperty(KEY_HIDE_SUGGESTED, String.valueOf(prefs.getBoolean(KEY_HIDE_SUGGESTED, true)));
        properties.setProperty(KEY_HIDE_SPLASH_ADS, String.valueOf(prefs.getBoolean(KEY_HIDE_SPLASH_ADS, true)));
        properties.setProperty(KEY_FORCE_SEEKBAR, String.valueOf(prefs.getBoolean(KEY_FORCE_SEEKBAR, true)));
        properties.setProperty(KEY_GOOGLE_LOGIN_FIX, String.valueOf(prefs.getBoolean(KEY_GOOGLE_LOGIN_FIX, true)));
        properties.setProperty(KEY_HIDE_PHOTO_POSTS, String.valueOf(prefs.getBoolean(KEY_HIDE_PHOTO_POSTS, false)));
        properties.setProperty(KEY_HIDE_AI_POSTS, String.valueOf(prefs.getBoolean(KEY_HIDE_AI_POSTS, false)));
        properties.setProperty(KEY_HIDE_LONG_POSTS, String.valueOf(prefs.getBoolean(KEY_HIDE_LONG_POSTS, false)));
        properties.setProperty(KEY_LONG_POST_SECONDS, prefs.getString(KEY_LONG_POST_SECONDS, "60"));
        properties.setProperty(KEY_FILTER_METRICS, String.valueOf(prefs.getBoolean(KEY_FILTER_METRICS, false)));
        properties.setProperty(KEY_VIEWS_MIN, prefs.getString(KEY_VIEWS_MIN, "0"));
        properties.setProperty(KEY_VIEWS_MAX, prefs.getString(KEY_VIEWS_MAX, ""));
        properties.setProperty(KEY_LIKES_MIN, prefs.getString(KEY_LIKES_MIN, "0"));
        properties.setProperty(KEY_LIKES_MAX, prefs.getString(KEY_LIKES_MAX, ""));
        properties.setProperty(KEY_KEYWORD_BLACKLIST, prefs.getString(KEY_KEYWORD_BLACKLIST, ""));
        properties.setProperty(KEY_PLAYBACK_SPEED_ENABLED, String.valueOf(prefs.getBoolean(KEY_PLAYBACK_SPEED_ENABLED, false)));
        properties.setProperty(KEY_PLAYBACK_SPEED, prefs.getString(KEY_PLAYBACK_SPEED, "1.25"));
        properties.setProperty(KEY_HIDE_AUTHOR_AVATAR, String.valueOf(prefs.getBoolean(KEY_HIDE_AUTHOR_AVATAR, false)));
        properties.setProperty(KEY_HIDE_AUTHOR_INFO, String.valueOf(prefs.getBoolean(KEY_HIDE_AUTHOR_INFO, false)));
        properties.setProperty(KEY_HIDE_VIDEO_DESC, String.valueOf(prefs.getBoolean(KEY_HIDE_VIDEO_DESC, false)));
        properties.setProperty(KEY_HIDE_MUSIC_TITLE, String.valueOf(prefs.getBoolean(KEY_HIDE_MUSIC_TITLE, false)));
        properties.setProperty(KEY_HIDE_ACTION_BUTTONS, String.valueOf(prefs.getBoolean(KEY_HIDE_ACTION_BUTTONS, false)));
        properties.setProperty(KEY_HIDE_TOP_NAV, String.valueOf(prefs.getBoolean(KEY_HIDE_TOP_NAV, false)));
        properties.setProperty(KEY_HIDE_SEARCH, String.valueOf(prefs.getBoolean(KEY_HIDE_SEARCH, false)));
        properties.setProperty(KEY_HIDE_BOTTOM_NAV, String.valueOf(prefs.getBoolean(KEY_HIDE_BOTTOM_NAV, false)));
        File file = new File(moduleContext.getFilesDir(), MIRROR_FILE);
        try (FileOutputStream out = new FileOutputStream(file)) {
            properties.store(out, "TiktokPatchXposed module settings");
        } catch (IOException ignored) {
        }
    }

    private static Properties readMirror(Context moduleContext) {
        File file = new File(moduleContext.getFilesDir(), MIRROR_FILE);
        if (!file.isFile()) return null;
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            properties.load(in);
            return properties;
        } catch (IOException ignored) {
            return null;
        }
    }

    static ModuleConfig fromProperties(Properties properties) {
        return new ModuleConfig(
                Boolean.parseBoolean(properties.getProperty(KEY_VIETNAM_REGION, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_VIETNAMESE_LANGUAGE, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_FEED_REGION_OVERRIDE, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_DOWNLOAD_NO_WATERMARK, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_FEED_ADS, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_LIVE, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_SUGGESTED, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_SPLASH_ADS, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_FORCE_SEEKBAR, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_GOOGLE_LOGIN_FIX, "true")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_PHOTO_POSTS, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_AI_POSTS, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_LONG_POSTS, "false")),
                positiveInt(properties.getProperty(KEY_LONG_POST_SECONDS, "60"), 60),
                Boolean.parseBoolean(properties.getProperty(KEY_FILTER_METRICS, "false")),
                nonNegativeLong(properties.getProperty(KEY_VIEWS_MIN, "0"), 0L),
                positiveLong(properties.getProperty(KEY_VIEWS_MAX, ""), Long.MAX_VALUE),
                nonNegativeLong(properties.getProperty(KEY_LIKES_MIN, "0"), 0L),
                positiveLong(properties.getProperty(KEY_LIKES_MAX, ""), Long.MAX_VALUE),
                parseKeywords(properties.getProperty(KEY_KEYWORD_BLACKLIST, "")),
                Boolean.parseBoolean(properties.getProperty(KEY_PLAYBACK_SPEED_ENABLED, "false")),
                sanitizeSpeed(parseFloat(properties.getProperty(KEY_PLAYBACK_SPEED, "1.25"), 1.25f)),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_AUTHOR_AVATAR, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_AUTHOR_INFO, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_VIDEO_DESC, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_MUSIC_TITLE, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_ACTION_BUTTONS, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_TOP_NAV, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_SEARCH, "false")),
                Boolean.parseBoolean(properties.getProperty(KEY_HIDE_BOTTOM_NAV, "false"))
        );
    }

    static Set<String> parseKeywords(String raw) {
        if (raw == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String part : raw.split("[,\\n]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed.toLowerCase());
        }
        return Collections.unmodifiableSet(out);
    }

    static String joinKeywords(Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String keyword : keywords) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(keyword);
        }
        return sb.toString();
    }

    static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static float sanitizeSpeed(float speed) {
        if (Float.isNaN(speed) || Float.isInfinite(speed)) return 1.0f;
        if (speed < 0.25f) return 0.25f;
        if (speed > 3.0f) return 3.0f;
        return speed;
    }

    private static int positiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long nonNegativeLong(String value, long fallback) {
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed >= 0 ? parsed : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long positiveLong(String value, long fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    @Override
    public String toString() {
        return "ModuleConfig{vn=" + vietnamRegion
                + ", ads=" + hideFeedAds
                + ", live=" + hideLive
                + ", seekbar=" + forceSeekbar
                + ", speed=" + playbackSpeedEnabled + "/" + playbackSpeed
                + ", keywords=" + keywordBlacklist.size()
                + ", purify=" + anyPurificationEnabled()
                + "}";
    }
}
