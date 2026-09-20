package com.golda.patchertiktok;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "Xposed-TikTokPatcher";
    private static final String PKG_TIKTOK_1 = "com.ss.android.ugc.trill";
    private static final String PKG_TIKTOK_2 = "com.zhiliaoapp.musically";
    private static final String FEED_SNAPSHOT_KEY =
            "com.golda.patchertiktok.cleaned_feed_snapshot";
    private static final String RENDERED_AD_SKIPPED_KEY =
            "com.golda.patchertiktok.rendered_ad_skipped";
    private static final String SEEKBAR_ELIGIBILITY_KEY =
            "com.golda.patchertiktok.seekbar_eligible";
    private static final String RECOMMENDATION_FEED_PATH = "/aweme/v2/feed/";
    private static final String LEGACY_RECOMMENDATION_FEED_PATH = "/aweme/v1/feed/";
    private static final String[] SUGGESTION_SIGNAL_METHOD_NAMES = {
            "getRelationTextKey", "getRecType", "getFriendTypeStr",
            "getLabelInfo", "getTabText", "getText", "getKey"
    };
    private static final String[] SUGGESTION_SIGNAL_FIELD_NAMES = {
            "relationTextKey", "recType", "friendTypeStr",
            "labelInfo", "tabText", "text", "key"
    };
    private static final String[][] RECOMMENDATION_FEED_OVERRIDES = {
            {"region", "VN"},
            {"carrier_region", "VN"},
            {"sys_region", "VN"},
            {"current_region", "VN"},
            {"residence", "VN"},
            {"op_region", "VN"},
            {"store_region", "VN"},
            {"mcc_mnc", "45204"},
            {"carrier_region_v2", "452"},
            {"language", "vi"},
            {"app_language", "vi"},
            {"locale", "vi-VN"}
    };
    private static final String COUNTRY_ISO = "VN";
    private static final String COUNTRY_ISO_LOWER = "vn";
    private static final String MCC = "452";
    private static final String MNC = "04";
    private static final String OPERATOR = MCC + MNC;
    private static final String OPERATOR_NAME = "Viettel";
    private static final String CONTENT_LANGUAGE = "vi";
    private static final Locale APP_LOCALE = new Locale("vi", "VN");
    private static final String APP_LOCALE_TAG = "vi-VN";

    private static final String AWEME_CLASS =
            "com.ss.android.ugc.aweme.feed.model.Aweme";
    // 47.0.3: X.06nl / X.06nm HomeSeekBarControl; older: X.06XH / X.1BSr
    private static final String[] SEEKBAR_CONTROLLER_CANDIDATES = {
            "X.06nl",
            "X.06nm",
            "X.06XH",
            "X.1BSr"
    };
    // 47.0.3: X.06l9 has setSeekBarShowType
    private static final String[] SEEKBAR_VIEW_CANDIDATES = {
            "X.06l9",
            "X.06W4",
            "X.17kt"
    };
    private static final String[] SEEKBAR_SHOULD_SHOW_METHOD_CANDIDATES = {
            "LJII",
            "LIZLLL"
    };

    private static volatile ModuleConfig config = ModuleConfig.defaults();
    private static final AtomicBoolean installed = new AtomicBoolean(false);

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!PKG_TIKTOK_1.equals(lpparam.packageName) && !PKG_TIKTOK_2.equals(lpparam.packageName)) return;
        XposedBridge.log(TAG + ": loaded for " + lpparam.packageName);

        final boolean isMainProcess = lpparam.packageName.equals(lpparam.processName);
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            XposedBridge.hookMethod(attach, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!installed.compareAndSet(false, true)) return;
                    Context context = (Context) param.args[0];
                    config = ModuleConfig.load(context);
                    XposedBridge.log(TAG + ": config source=" + ModuleConfig.lastLoadSource
                            + " " + config);
                    registerConfigReceiver(context);
                    installConfiguredHooks(lpparam, isMainProcess);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [attach hook] " + t);
            if (installed.compareAndSet(false, true)) {
                config = ModuleConfig.defaults();
                installConfiguredHooks(lpparam, isMainProcess);
            }
        }
    }

    private void registerConfigReceiver(Context context) {
        try {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context receiverContext, Intent intent) {
                    if (intent == null) return;
                    Bundle bundle = intent.getBundleExtra(ModuleConfig.EXTRA_CONFIG);
                    if (bundle == null) return;
                    ModuleConfig.saveRuntime(receiverContext, bundle);
                    XposedBridge.log(TAG + ": config broadcast saved to TikTok prefs; "
                            + "force-stop TikTok to apply " + ModuleConfig.fromBundle(bundle));
                }
            };
            IntentFilter filter = new IntentFilter(ModuleConfig.ACTION_CONFIG);
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
            XposedBridge.log(TAG + ": config broadcast receiver registered");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [config receiver] " + t);
        }
    }

    private void installConfiguredHooks(
            final XC_LoadPackage.LoadPackageParam lpparam,
            boolean isMainProcess
    ) {
        if (config.vietnamRegion || config.vietnameseLanguage) {
            installVietnamRegionSpoof();
        }
        if (config.vietnameseLanguage) {
            installVietnameseRecommendationLanguage(lpparam.classLoader);
        }
        if (config.feedRegionOverride) {
            installRecommendationFeedRegionOverride(lpparam.classLoader);
        }
        if (!isMainProcess) {
            return;
        }

        if (config.downloadNoWatermark) {
            installDownloadPatches(lpparam);
        }
        if (config.forceSeekbar) {
            installSeekbarPatch(lpparam.classLoader);
        }
        if (config.anyFeedFilterEnabled()) {
            installSafeFeedFilter(lpparam);
        }
        if (config.hideFeedAds) {
            installRenderedAdSkip(lpparam.classLoader);
        }
        if (config.hideSplashAds) {
            installStartupAdBlocker(lpparam.classLoader);
        }
        if (config.hideLive) {
            installTopLiveButtonPatch(lpparam.classLoader);
        }
        if (config.googleLoginFix) {
            installGoogleLoginFix(lpparam);
        }
        if (config.anyPurificationEnabled()) {
            installPurification(lpparam.classLoader);
        }
        if (PlaybackSpeedPolicy.shouldApply(config.playbackSpeedEnabled, config.playbackSpeed)) {
            installPlaybackSpeed(lpparam.classLoader);
        }
    }

    private void installDownloadPatches(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookReturnConst("com.ss.android.ugc.aweme.feed.model.ACLCommonShare", lpparam.classLoader, "getCode", 0);
            hookReturnConst("com.ss.android.ugc.aweme.feed.model.ACLCommonShare", lpparam.classLoader, "getShowType", 2);
            hookReturnConst("com.ss.android.ugc.aweme.feed.model.ACLCommonShare", lpparam.classLoader, "getTranscode", 1);
            XposedBridge.log(TAG + ": download patches installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [video patches] " + t);
        }
    }

    private void installPurification(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    LayoutInflater.class,
                    "inflate",
                    int.class,
                    ViewGroup.class,
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object result = param.getResult();
                            if (!(result instanceof View)) return;
                            int resId = (Integer) param.args[0];
                            String name = resourceName(((View) result).getResources(), resId);
                            if (name != null && shouldPurifyResource(name)) {
                                hideViewTree((View) result);
                            }
                        }
                    });
            XposedBridge.log(TAG + ": purification hooks installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [purification] " + t);
        }
    }

    private static String resourceName(android.content.res.Resources resources, int resId) {
        try {
            return resources.getResourceEntryName(resId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean shouldPurifyResource(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (config.hideAuthorAvatar && (lower.contains("avatar") || lower.contains("head_image"))) return true;
        if (config.hideAuthorInfo && (lower.contains("author_name") || lower.contains("nickname")
                || lower.contains("user_name"))) return true;
        if (config.hideVideoDesc && (lower.contains("desc") || lower.contains("caption"))) return true;
        if (config.hideMusicTitle && lower.contains("music")) return true;
        if (config.hideActionButtons && (lower.contains("like") || lower.contains("comment")
                || lower.contains("share") || lower.contains("collect") || lower.contains("favorite"))) return true;
        if (config.hideTopNav && (lower.contains("following") || lower.contains("for_you")
                || lower.contains("top_tab"))) return true;
        if (config.hideSearch && lower.contains("search")) return true;
        if (config.hideBottomNav && (lower.contains("tab_") || lower.contains("bottom_nav")
                || lower.contains("main_tab"))) return true;
        return false;
    }

    private void hideViewTree(View view) {
        hideView(view);
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            hideViewTree(group.getChildAt(i));
        }
    }

    private void installPlaybackSpeed(ClassLoader classLoader) {
        try {
            final float speed = PlaybackSpeedPolicy.clamp(config.playbackSpeed);
            int hooks = 0;
            String[] candidateClasses = {
                    "com.ss.android.ugc.aweme.feed.controller.PlayerController",
                    "com.ss.android.ugc.aweme.feed.controller.I18nPlayerController",
                    "X.037l"
            };
            for (String raw : candidateClasses) {
                String name = raw.startsWith("X.")
                        ? "X/" + raw.substring(2)
                        : raw;
                Class<?> cls = XposedHelpers.findClassIfExists(name, classLoader);
                if (cls == null) {
                    cls = XposedHelpers.findClassIfExists(raw, classLoader);
                }
                if (cls == null) continue;
                for (Method method : cls.getDeclaredMethods()) {
                    Class<?>[] params = method.getParameterTypes();
                    String methodName = method.getName().toLowerCase(Locale.ROOT);
                    boolean speedNamed = methodName.contains("speed")
                            || methodName.contains("rate")
                            || "setspeed".equals(methodName)
                            || "setrate".equals(methodName);
                    if (!speedNamed) continue;
                    if (params.length == 1
                            && (params[0] == float.class || params[0] == Float.class)) {
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (param.args.length > 0 && param.args[0] instanceof Number) {
                                    param.args[0] = speed;
                                }
                            }
                        });
                        hooks++;
                    } else if (params.length == 2
                            && params[0] == String.class
                            && (params[1] == float.class || params[1] == Float.class)) {
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (param.args.length > 1 && param.args[1] instanceof Number) {
                                    param.args[1] = speed;
                                }
                            }
                        });
                        hooks++;
                    }
                }
            }
            XposedBridge.log(TAG + ": playback speed hooks=" + hooks + " speed=" + speed);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [playback speed] " + t);
        }
    }

    private void hookReturnConst(String cls, ClassLoader cl, String method, Object value) {
        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, XC_MethodReplacement.returnConstant(value));
        } catch (Throwable t) {
            XposedBridge.log(TAG + " hookReturnConst failed for " + cls + "#" + method + ": " + t);
        }
    }

    private void installSeekbarPatch(ClassLoader classLoader) {
        Class<?> awemeClass = XposedHelpers.findClassIfExists(AWEME_CLASS, classLoader);
        int shouldShowHooks = 0;
        int shortVideoHooks = 0;
        int showTypeHooks = 0;
        int legacyHooks = 0;

        for (String className : SEEKBAR_CONTROLLER_CANDIDATES) {
            try {
                Class<?> controller = XposedHelpers.findClassIfExists(className, classLoader);
                if (controller == null) continue;

                for (Method method : controller.getDeclaredMethods()) {
                    Class<?>[] parameters = method.getParameterTypes();
                    if (awemeClass != null
                            && isNamed(method, SEEKBAR_SHOULD_SHOW_METHOD_CANDIDATES)
                            && method.getReturnType() == boolean.class
                            && parameters.length == 1
                            && parameters[0] == awemeClass) {
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (Boolean.TRUE.equals(param.getResult())) return;
                                if (param.args.length > 0
                                        && shouldForceSeekbarForAweme(param.args[0])) {
                                    param.setResult(true);
                                }
                            }
                        });
                        shouldShowHooks++;
                    }

                    if ("LJIIL".equals(method.getName())
                            && method.getReturnType() == int.class
                            && parameters.length == 1
                            && parameters[0] == boolean.class) {
                        XposedBridge.hookMethod(
                                method,
                                XC_MethodReplacement.returnConstant(0)
                        );
                        shortVideoHooks++;
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + " [seekbar controller " + className + "] " + t);
            }
        }

        List<Class<?>> seekbarViewClasses = new ArrayList<>();
        for (String className : SEEKBAR_VIEW_CANDIDATES) {
            addClassIfMissing(
                    seekbarViewClasses,
                    XposedHelpers.findClassIfExists(className, classLoader)
            );
        }
        discoverSeekbarViewClasses(classLoader, seekbarViewClasses);

        List<Method> hookedShowTypeMethods = new ArrayList<>();
        for (Class<?> seekbarViewClass : seekbarViewClasses) {
            for (Method method : seekbarViewClass.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!"setSeekBarShowType".equals(method.getName())
                        || method.getReturnType() != void.class
                        || parameters.length != 1
                        || parameters[0] != int.class
                        || hookedShowTypeMethods.contains(method)) {
                    continue;
                }

                try {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length == 0 || !(param.args[0] instanceof Number)) {
                                return;
                            }
                            int requestedType = ((Number) param.args[0]).intValue();
                            param.args[0] = SeekbarPolicy.normalizeShowType(requestedType);
                        }
                    });
                    hookedShowTypeMethods.add(method);
                    showTypeHooks++;
                } catch (Throwable t) {
                    XposedBridge.log(TAG + " [seekbar show type "
                            + seekbarViewClass.getName() + "] " + t);
                }
            }
        }

        try {
            Class<?> legacyManager = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.player.sdk.api.SeekBarManager",
                    classLoader
            );
            if (legacyManager != null) {
                XposedBridge.hookAllMethods(
                        legacyManager,
                        "shouldShowSeekBar",
                        XC_MethodReplacement.returnConstant(true)
                );
                legacyHooks++;
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [legacy seekbar] " + t);
        }

        XposedBridge.log(TAG + ": seekbar patch installed; shouldShow=" + shouldShowHooks
                + " showType=" + showTypeHooks
                + " shortVideo=" + shortVideoHooks
                + " legacy=" + legacyHooks);
    }

    private boolean isNamed(Method method, String[] candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(method.getName())) return true;
        }
        return false;
    }

    private void addClassIfMissing(List<Class<?>> classes, Class<?> candidate) {
        if (candidate != null && !classes.contains(candidate)) {
            classes.add(candidate);
        }
    }

    private void discoverSeekbarViewClasses(
            ClassLoader classLoader,
            List<Class<?>> seekbarViewClasses
    ) {
        try {
            Class<?> mainPageSeekAssem = XposedHelpers.findClassIfExists(
                    "com.bytedance.tiktok.homepage.mainpagefragment.assem.MainPageSeekAssem",
                    classLoader
            );
            if (mainPageSeekAssem == null) return;

            for (Field field : mainPageSeekAssem.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                for (Method method : fieldType.getDeclaredMethods()) {
                    Class<?>[] parameters = method.getParameterTypes();
                    if ("setSeekBarShowType".equals(method.getName())
                            && method.getReturnType() == void.class
                            && parameters.length == 1
                            && parameters[0] == int.class) {
                        addClassIfMissing(seekbarViewClasses, fieldType);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [seekbar view discovery] " + t);
        }
    }

    private boolean shouldForceSeekbarForAweme(Object aweme) {
        if (aweme == null) return false;

        try {
            Object cached = XposedHelpers.getAdditionalInstanceField(
                    aweme,
                    SEEKBAR_ELIGIBILITY_KEY
            );
            if (cached instanceof Boolean) return (Boolean) cached;

            boolean hasVideo = XposedHelpers.callMethod(aweme, "getVideo") != null;
            boolean isAd = Boolean.TRUE.equals(XposedHelpers.callMethod(aweme, "isAd"))
                    || XposedHelpers.callMethod(aweme, "getAwemeRawAd") != null;
            int awemeType = ((Number) XposedHelpers.callMethod(
                    aweme,
                    "getAwemeType"
            )).intValue();
            boolean isLive = awemeType == 101;
            boolean isPhoto = awemeType == 150
                    || XposedHelpers.callMethod(aweme, "getPhotoModeImageInfo") != null
                    || XposedHelpers.callMethod(aweme, "getPhotoModeTextInfo") != null;
            boolean eligible = SeekbarPolicy.shouldForceShow(
                    false,
                    hasVideo,
                    isAd,
                    isLive,
                    isPhoto
            );
            XposedHelpers.setAdditionalInstanceField(
                    aweme,
                    SEEKBAR_ELIGIBILITY_KEY,
                    eligible
            );
            return eligible;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void installSafeFeedFilter(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            installFeedItemListHooks(lpparam.classLoader);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [feed filter] " + t);
        }
    }

    private void installStartupAdBlocker(ClassLoader classLoader) {
        final String serviceClassName =
                "com.bytedance.ies.ugc.aweme.commercialize.splash.core.SplashAdServiceImpl";
        final String[] enabledMethodCandidates = {
                "LJIILIIL", "LJIILL", "LJFF", "LJJIJIL"
        };
        final String[] preloadTaskClasses = {
                "com.bytedance.ies.ugc.aweme.commercialize.splash.SplashAdManagerPreloadTask",
                "com.bytedance.ies.ugc.aweme.commercialize.splash.topview.TopViewPreloadTask",
                "com.bytedance.ies.ugc.aweme.commercialize.splash.topview.TopViewPreloadJsonTask",
                "com.bytedance.ies.ugc.aweme.commercialize.splash.topview.RealTimeSplashTask"
        };

        try {
            Class<?> serviceClass = XposedHelpers.findClassIfExists(
                    serviceClassName,
                    classLoader
            );
            int serviceHooks = 0;
            if (serviceClass != null) {
                for (String methodName : enabledMethodCandidates) {
                    for (Method method : serviceClass.getDeclaredMethods()) {
                        if (!methodName.equals(method.getName())
                                || method.getParameterTypes().length != 0) {
                            continue;
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) {
                            XposedBridge.hookMethod(
                                    method,
                                    XC_MethodReplacement.returnConstant(false)
                            );
                            serviceHooks++;
                        } else if (returnType == void.class) {
                            XposedBridge.hookMethod(
                                    method,
                                    XC_MethodReplacement.returnConstant(null)
                            );
                            serviceHooks++;
                        }
                    }
                }

                XposedBridge.hookAllMethods(
                        serviceClass,
                        "LJJIIJZLJL",
                        XC_MethodReplacement.returnConstant(null)
                );
            }

            int taskHooks = 0;
            for (String className : preloadTaskClasses) {
                Class<?> taskClass = XposedHelpers.findClassIfExists(className, classLoader);
                if (taskClass == null) continue;

                for (Method method : taskClass.getDeclaredMethods()) {
                    if (!"run".equals(method.getName())
                            || method.getReturnType() != void.class) {
                        continue;
                    }
                    XposedBridge.hookMethod(
                            method,
                            XC_MethodReplacement.returnConstant(null)
                    );
                    taskHooks++;
                }
            }

            XposedBridge.log(TAG + ": startup splash/TopView ad blocker installed; service="
                    + serviceHooks + " tasks=" + taskHooks);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [startup ad blocker] " + t);
        }
    }

    private void installTopLiveButtonPatch(ClassLoader classLoader) {
        final String className =
                "com.bytedance.tiktok.homepage.mainfragment.toolbar.LiveIconGenerator";
        try {
            Class<?> liveIconGenerator = XposedHelpers.findClassIfExists(className, classLoader);
            if (liveIconGenerator == null) {
                XposedBridge.log(TAG + ": LiveIconGenerator not found");
                return;
            }

            int factoryHooks = 0;
            int visibilityHooks = 0;
            for (Method method : liveIconGenerator.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if ("enabled".equals(method.getName())
                        && method.getReturnType() == boolean.class
                        && params.length == 0) {
                    XposedBridge.hookMethod(
                            method,
                            XC_MethodReplacement.returnConstant(false)
                    );
                    continue;
                }

                if (View.class.isAssignableFrom(method.getReturnType())
                        && params.length == 1
                        && "android.content.Context".equals(params[0].getName())) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            hideView(param.getResult());
                            hideLiveIconFields(param.thisObject);
                        }
                    });
                    factoryHooks++;
                    continue;
                }

                if (method.getReturnType() == void.class
                        && params.length == 1
                        && params[0] == boolean.class) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.args[0] = false;
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            hideLiveIconFields(param.thisObject);
                        }
                    });
                    visibilityHooks++;
                }
            }

            XC_MethodHook keepHidden = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    hideLiveIconFields(param.thisObject);
                }
            };
            XposedBridge.hookAllMethods(liveIconGenerator, "onCreate", keepHidden);
            XposedBridge.hookAllMethods(liveIconGenerator, "onResume", keepHidden);
            XposedBridge.hookAllMethods(
                    liveIconGenerator,
                    "onLiveIconEntranceEnable",
                    keepHidden
            );
            XposedBridge.log(TAG + ": top-left LIVE button patch installed; factories="
                    + factoryHooks + " visibility=" + visibilityHooks);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [top LIVE button] " + t);
        }
    }

    private void hideLiveIconFields(Object generator) {
        if (generator == null) return;

        for (Class<?> cls = generator.getClass(); cls != null; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) {
                if (!ImageView.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    hideView(field.get(generator));
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void hideView(Object value) {
        if (!(value instanceof View)) return;

        View view = (View) value;
        view.setVisibility(View.GONE);
        view.setClickable(false);
        view.setLongClickable(false);
    }

    private void installFeedItemListHooks(ClassLoader classLoader) {
        Class<?> cls = XposedHelpers.findClassIfExists(
                "com.ss.android.ugc.aweme.feed.model.FeedItemList",
                classLoader
        );
        if (cls == null) {
            XposedBridge.log(TAG + ": FeedItemList not found");
            return;
        }

        XposedBridge.hookAllMethods(cls, "setItems", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length > 0 && param.args[0] instanceof List<?>) {
                    List<?> filtered = filterFeedItems((List<?>) param.args[0]);
                    param.args[0] = filtered;
                    rememberCleanFeed(param.thisObject, filtered);
                }
            }
        });
        XposedBridge.hookAllMethods(cls, "getItems", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!(param.getResult() instanceof List<?>)) return;

                List<?> items = (List<?>) param.getResult();
                Object cached = XposedHelpers.getAdditionalInstanceField(
                        param.thisObject,
                        FEED_SNAPSHOT_KEY
                );
                if (cached instanceof FeedSnapshot
                        && ((FeedSnapshot) cached).matches(items)) {
                    return;
                }

                List<?> filtered = filterFeedItems(items);
                if (filtered != items) {
                    try {
                        XposedHelpers.setObjectField(param.thisObject, "items", filtered);
                    } catch (Throwable ignored) {
                    }
                    param.setResult(filtered);
                }
                rememberCleanFeed(param.thisObject, filtered);
            }
        });
        XposedBridge.hookAllMethods(cls, "setPreloadAds", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length > 0) param.args[0] = Collections.emptyList();
            }
        });
        XposedBridge.hookAllMethods(cls, "getPreloadAds", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return Collections.emptyList();
            }
        });
        XposedBridge.hookAllMethods(cls, "setHasAd", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length > 0) param.args[0] = false;
            }
        });
        XposedBridge.hookAllMethods(
                cls,
                "isHasAd",
                XC_MethodReplacement.returnConstant(false)
        );
        XposedBridge.log(TAG + ": lightweight FeedItemList ad/live hooks installed");
    }

    private void rememberCleanFeed(Object owner, List<?> items) {
        XposedHelpers.setAdditionalInstanceField(
                owner,
                FEED_SNAPSHOT_KEY,
                new FeedSnapshot(items)
        );
    }

    private static final class FeedSnapshot {
        private final List<?> items;
        private final int size;

        private FeedSnapshot(List<?> items) {
            this.items = items;
            this.size = items.size();
        }

        private boolean matches(List<?> current) {
            return items == current && size == current.size();
        }
    }

    private List<?> filterFeedItems(List<?> items) {
        if (items == null || items.isEmpty()) return items;

        ArrayList<Object> filtered = null;
        for (int index = 0; index < items.size(); index++) {
            Object item = items.get(index);
            Object aweme = unwrapAweme(item);
            boolean remove = FeedFilterPolicy.shouldRemove(
                    config.hideFeedAds,
                    config.hideLive,
                    config.hideSuggested,
                    config.hidePhotoPosts,
                    config.hideAiPosts,
                    config.hideLongPosts,
                    config.filterMetrics,
                    isAdItem(item) || isAdItem(aweme),
                    isLiveItem(item) || isLiveItem(aweme),
                    isSuggestedAcquaintance(aweme),
                    isPhotoPost(aweme),
                    isAiPost(aweme),
                    isLongPost(aweme),
                    isMetricOutOfRange(aweme),
                    matchesKeywordBlacklist(aweme)
            );
            if (remove) {
                if (filtered == null) {
                    filtered = new ArrayList<>(Math.max(0, items.size() - 1));
                    for (int previous = 0; previous < index; previous++) {
                        filtered.add(items.get(previous));
                    }
                }
            } else if (filtered != null) {
                filtered.add(item);
            }
        }

        if (filtered != null) {
            return filtered;
        }
        return items;
    }

    private boolean isPhotoPost(Object aweme) {
        if (aweme == null) return false;
        Object awemeType = callNoArg(aweme, "getAwemeType");
        if (awemeType instanceof Number && ((Number) awemeType).intValue() == 150) return true;
        return callNoArg(aweme, "getPhotoModeImageInfo") != null
                || callNoArg(aweme, "getPhotoModeTextInfo") != null;
    }

    private boolean isAiPost(Object aweme) {
        if (aweme == null) return false;
        return callBooleanNoArg(aweme, "isAigc")
                || callBooleanNoArg(aweme, "isAIGC")
                || callBooleanNoArg(aweme, "isAiGenerated")
                || callBooleanNoArg(aweme, "isAIGCContent")
                || callNoArg(aweme, "getAigcInfo") != null
                || callNoArg(aweme, "getAigcInfoModel") != null
                || hasPositiveOrObjectField(aweme, "aigcInfo")
                || hasPositiveOrObjectField(aweme, "aigcInfoModel")
                || hasPositiveOrObjectField(aweme, "moderationAigcInfo");
    }

    private boolean isLongPost(Object aweme) {
        if (aweme == null || !config.hideLongPosts) return false;
        Integer durationMs = getDurationMs(aweme);
        return FeedFilterPolicy.isLongVideo(durationMs, config.longPostSeconds);
    }

    private Integer getDurationMs(Object aweme) {
        Object video = callNoArg(aweme, "getVideo");
        if (video == null) return null;
        Object length = findFieldValue(video, "videoLength");
        if (length instanceof Number) return ((Number) length).intValue();
        length = findFieldValue(video, "duration");
        if (length instanceof Number) return ((Number) length).intValue();
        length = callNoArg(video, "getDuration");
        if (length instanceof Number) return ((Number) length).intValue();
        return null;
    }

    private boolean isMetricOutOfRange(Object aweme) {
        if (aweme == null || !config.filterMetrics) return false;
        Object stats = callNoArg(aweme, "getStatistics");
        if (stats == null) stats = findFieldValue(aweme, "statistics");
        Long playCount = readLong(stats, aweme, "playCount", "getPlayCount");
        Long diggCount = readLong(stats, aweme, "diggCount", "getDiggCount");
        return FeedFilterPolicy.shouldRemoveByMetrics(
                playCount,
                diggCount,
                true,
                config.viewsMin,
                config.viewsMax,
                config.likesMin,
                config.likesMax
        );
    }

    private Long readLong(Object primary, Object fallback, String fieldName, String getter) {
        Object value = findFieldValue(primary, fieldName);
        if (value == null) value = callNoArg(primary, getter);
        if (value == null) value = findFieldValue(fallback, fieldName);
        if (value == null) value = callNoArg(fallback, getter);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private boolean matchesKeywordBlacklist(Object aweme) {
        if (aweme == null || config.keywordBlacklist.isEmpty()) return false;
        Set<String> keywords = config.keywordBlacklist;
        Object desc = callNoArg(aweme, "getDesc");
        if (desc == null) desc = findFieldValue(aweme, "desc");
        if (FeedFilterPolicy.matchesKeyword(desc == null ? null : String.valueOf(desc), keywords)) {
            return true;
        }
        Object hashtags = callNoArg(aweme, "getHashtags");
        if (hashtags instanceof List<?>) {
            for (Object tag : (List<?>) hashtags) {
                Object text = callNoArg(tag, "getHashtagName");
                if (text == null) text = callNoArg(tag, "getChallengeName");
                if (text == null) text = findFieldValue(tag, "hashtagName");
                if (text == null) text = findFieldValue(tag, "challengeName");
                if (FeedFilterPolicy.matchesKeyword(text == null ? null : String.valueOf(text), keywords)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSuggestedAcquaintance(Object aweme) {
        if (aweme == null) return false;

        Object relationInfo = callNoArg(aweme, "getRelationRecommendInfo");
        if (relationInfo == null) {
            relationInfo = findFieldValue(aweme, "relationRecommendInfo");
        }
        boolean familiar = callBooleanNoArg(aweme, "isFamiliar")
                || getBooleanField(aweme, "isFamiliar");
        if (relationInfo != null
                && FeedSuggestionClassifier.shouldRemove(
                        familiar,
                        true,
                        hasSuggestedAcquaintanceSignal(relationInfo)
                )) {
            return true;
        }

        Object relationLabel = callNoArg(aweme, "getRelationLabel");
        if (relationLabel == null) relationLabel = findFieldValue(aweme, "relationLabel");
        if (hasSuggestedAcquaintanceSignal(relationLabel)) return true;

        Object feedRelationLabel = callNoArg(aweme, "getFeedRelationLabel");
        if (feedRelationLabel == null) {
            feedRelationLabel = findFieldValue(aweme, "feedRelationLabel");
        }
        return hasSuggestedAcquaintanceSignal(feedRelationLabel);
    }

    private boolean hasSuggestedAcquaintanceSignal(Object model) {
        if (model == null) return false;

        for (String methodName : SUGGESTION_SIGNAL_METHOD_NAMES) {
            if (FeedSuggestionClassifier.hasAcquaintanceMarker(
                    callNoArg(model, methodName)
            )) {
                return true;
            }
        }

        for (String fieldName : SUGGESTION_SIGNAL_FIELD_NAMES) {
            if (FeedSuggestionClassifier.hasAcquaintanceMarker(
                    findFieldValue(model, fieldName)
            )) {
                return true;
            }
        }
        return false;
    }

    private Object unwrapAweme(Object item) {
        if (item == null) return null;
        String[] fieldNames = {"aweme", "mAweme", "item"};
        for (String fieldName : fieldNames) {
            Object value = getObjectField(item, fieldName);
            if (value != null) return value;
        }
        return item;
    }

    private boolean isAdItem(Object item) {
        if (item == null) return false;
        if (getBooleanField(item, "isAd")) return true;
        if (callBooleanNoArg(item, "isAd")) return true;
        if (callBooleanNoArg(item, "isAdAweme")) return true;
        if (callBooleanNoArg(item, "isSoftAd")) return true;
        if (callNoArg(item, "getAwemeRawAd") != null) return true;
        if (isPseudoAd(item)) return true;
        return hasPositiveOrObjectField(item, "awemeRawAd")
                || hasPositiveOrObjectField(item, "rawAd");
    }

    private boolean isPseudoAd(Object item) {
        Object commerce = callNoArg(item, "getCommerceVideoAuthInfo");
        return commerce != null
                && callBooleanNoArg(commerce, "isPseudoAd")
                && callNoArg(commerce, "getPseudoAdData") != null;
    }

    private boolean isLiveItem(Object item) {
        if (item == null) return false;
        Object awemeType = callNoArg(item, "getAwemeType");
        if (awemeType instanceof Number && ((Number) awemeType).intValue() == 101) return true;
        if (getBooleanField(item, "isLive")) return true;
        if (callBooleanNoArg(item, "isLive")) return true;
        if (callBooleanNoArg(item, "isLiveReplay")) return true;
        if (matchesStringField(item, "contentType", "live")) return true;
        if (matchesStringField(item, "content_type", "live")) return true;
        return matchesStringField(item, "schema", "aweme://live");
    }

    private boolean getBooleanField(Object target, String fieldName) {
        try {
            return XposedHelpers.getBooleanField(target, fieldName);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Object getObjectField(Object target, String fieldName) {
        try {
            return XposedHelpers.getObjectField(target, fieldName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object findFieldValue(Object target, String fieldName) {
        if (target == null) return null;
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Field field = cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Throwable ignored) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }

    private boolean hasPositiveOrObjectField(Object target, String fieldName) {
        Object value = getObjectField(target, fieldName);
        if (value == null) value = findFieldValue(target, fieldName);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).longValue() > 0;
        return value != null;
    }

    private boolean matchesStringField(Object target, String fieldName, String expected) {
        Object value = getObjectField(target, fieldName);
        if (value == null) value = findFieldValue(target, fieldName);
        return value instanceof String && expected.equalsIgnoreCase((String) value);
    }

    private boolean callBooleanNoArg(Object target, String methodName) {
        Object result = callNoArg(target, methodName);
        return result instanceof Boolean && (Boolean) result;
    }

    private Object callNoArg(Object target, String methodName) {
        for (Class<?> cls = target.getClass(); cls != null; cls = cls.getSuperclass()) {
            try {
                Method method = cls.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private void installVietnamRegionSpoof() {
        hookTelephony("getSimCountryIso", COUNTRY_ISO);
        hookTelephony("getNetworkCountryIso", COUNTRY_ISO);
        hookTelephony("getSimOperator", OPERATOR);
        hookTelephony("getNetworkOperator", OPERATOR);
        hookTelephony("getSimOperatorName", OPERATOR_NAME);
        hookTelephony("getNetworkOperatorName", OPERATOR_NAME);
        hookSubscriptionInfo();
        hookSystemProperties();
        hookLocale();
        XposedBridge.log(TAG + ": Vietnam SIM spoof with Vietnamese language installed");
    }

    private void installRenderedAdSkip(ClassLoader classLoader) {
        final String panelClassName =
                "com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel";
        try {
            Class<?> panelClass = XposedHelpers.findClassIfExists(panelClassName, classLoader);
            if (panelClass == null) {
                XposedBridge.log(TAG + ": BaseListFragmentPanel not found");
                return;
            }

            final Method currentAwemeMethod = findCurrentAwemeMethod(panelClass);
            if (currentAwemeMethod == null) {
                XposedBridge.log(TAG + ": current Aweme getter not found");
                return;
            }
            currentAwemeMethod.setAccessible(true);

            int hooks = 0;
            for (Method method : panelClass.getDeclaredMethods()) {
                if (("onRenderFirstFrame".equals(method.getName())
                        || "LJJIJIL".equals(method.getName()))
                        && method.getReturnType() == void.class) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object aweme = currentAwemeMethod.invoke(param.thisObject);
                                if (isAdItem(aweme)) {
                                    skipRenderedAd(param.thisObject, aweme);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + " [rendered ad check] " + t);
                            }
                        }
                    });
                    hooks++;
                }
            }
            XposedBridge.log(TAG + ": rendered ad skip installed; renderHooks=" + hooks
                    + " currentAweme=" + currentAwemeMethod.getName());
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [rendered ad skip] " + t);
        }
    }

    private Method findCurrentAwemeMethod(Class<?> panelClass) {
        String awemeClassName = "com.ss.android.ugc.aweme.feed.model.Aweme";
        String[] preferredNames = {
                "getCurrentAweme", "LJII", "LJIIIIZZ", "QP", "vQ", "LLLZIL"
        };
        for (String preferredName : preferredNames) {
            for (Method method : panelClass.getDeclaredMethods()) {
                if (preferredName.equals(method.getName())
                        && method.getParameterTypes().length == 0
                        && awemeClassName.equals(method.getReturnType().getName())) {
                    return method;
                }
            }
        }

        Method fallback = null;
        for (Method method : panelClass.getDeclaredMethods()) {
            if (method.getParameterTypes().length != 0
                    || !awemeClassName.equals(method.getReturnType().getName())) {
                continue;
            }
            if (fallback == null
                    && Modifier.isPublic(method.getModifiers())
                    && Modifier.isFinal(method.getModifiers())) {
                fallback = method;
            }
        }
        return fallback;
    }

    private void skipRenderedAd(Object panel, Object aweme) {
        if (aweme == null || Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(
                aweme,
                RENDERED_AD_SKIPPED_KEY
        ))) {
            return;
        }

        Activity activity = null;
        Object value = findFieldValue(panel, "activity");
        if (value instanceof Activity) activity = (Activity) value;
        if (activity == null) {
            value = callNoArg(panel, "getActivity");
            if (value instanceof Activity) activity = (Activity) value;
        }
        if (activity == null) {
            Object fragment = callNoArg(panel, "getFragment");
            value = fragment == null ? null : callNoArg(fragment, "getActivity");
            if (value instanceof Activity) activity = (Activity) value;
        }
        if (activity == null || activity.isFinishing()) return;

        XposedHelpers.setAdditionalInstanceField(aweme, RENDERED_AD_SKIPPED_KEY, true);
        View decor = activity.getWindow().getDecorView();
        decor.postDelayed(() -> {
            try {
                dispatchSwipeToNext(decor);
            } catch (Throwable t) {
                XposedBridge.log(TAG + " [rendered ad swipe] " + t);
            }
        }, 80L);
    }

    private void dispatchSwipeToNext(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) return;

        float x = width / 2f;
        float startY = height * 0.80f;
        float endY = height * 0.20f;
        long downTime = SystemClock.uptimeMillis();
        dispatchTouch(view, MotionEvent.ACTION_DOWN, x, startY, downTime, downTime);
        for (int step = 1; step <= 10; step++) {
            float y = startY + (endY - startY) * step / 10f;
            dispatchTouch(
                    view,
                    MotionEvent.ACTION_MOVE,
                    x,
                    y,
                    downTime,
                    downTime + step * 6L
            );
        }
        dispatchTouch(
                view,
                MotionEvent.ACTION_UP,
                x,
                endY,
                downTime,
                downTime + 66L
        );
    }

    private void dispatchTouch(
            View view,
            int action,
            float x,
            float y,
            long downTime,
            long eventTime
    ) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            view.dispatchTouchEvent(event);
        } finally {
            event.recycle();
        }
    }

    private void hookTelephony(String methodName, final String result) {
        try {
            int hooked = 0;
            for (Method method : TelephonyManager.class.getDeclaredMethods()) {
                if (methodName.equals(method.getName()) && method.getReturnType() == String.class) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(result));
                    hooked++;
                }
            }
            XposedBridge.log(TAG + ": hooked TelephonyManager#" + methodName + " overloads=" + hooked);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [telephony:" + methodName + "] " + t);
        }
    }

    private void hookSubscriptionInfo() {
        try {
            Class<?> cls = XposedHelpers.findClassIfExists("android.telephony.SubscriptionInfo", null);
            if (cls == null) {
                XposedBridge.log(TAG + ": SubscriptionInfo not found");
                return;
            }

            hookMatchingMethods(cls, "getCountryIso", String.class, COUNTRY_ISO_LOWER);
            hookMatchingMethods(cls, "getMccString", String.class, MCC);
            hookMatchingMethods(cls, "getMncString", String.class, MNC);
            hookMatchingMethods(cls, "getMcc", int.class, 452);
            hookMatchingMethods(cls, "getMnc", int.class, 4);
            hookMatchingMethods(cls, "getCarrierName", CharSequence.class, OPERATOR_NAME);
            hookMatchingMethods(cls, "getDisplayName", CharSequence.class, OPERATOR_NAME);
            XposedBridge.log(TAG + ": SubscriptionInfo spoof installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [subscription info] " + t);
        }
    }

    private void hookMatchingMethods(Class<?> cls, String methodName, Class<?> returnType, Object result) {
        try {
            int hooked = 0;
            for (Method method : cls.getDeclaredMethods()) {
                if (methodName.equals(method.getName()) && returnType.isAssignableFrom(method.getReturnType())) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(result));
                    hooked++;
                }
            }
            XposedBridge.log(TAG + ": hooked " + cls.getSimpleName() + "#" + methodName + " overloads=" + hooked);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [hookMatchingMethods " + cls.getName() + "#" + methodName + "] " + t);
        }
    }

    private void hookSystemProperties() {
        try {
            Class<?> cls = XposedHelpers.findClassIfExists("android.os.SystemProperties", null);
            if (cls == null) {
                XposedBridge.log(TAG + ": SystemProperties not found");
                return;
            }

            XposedHelpers.findAndHookMethod(cls, "get", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String value = getSpoofedSystemProperty((String) param.args[0]);
                    if (value != null) param.setResult(value);
                }
            });

            XposedHelpers.findAndHookMethod(cls, "get", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String value = getSpoofedSystemProperty((String) param.args[0]);
                    if (value != null) param.setResult(value);
                }
            });

            XposedBridge.log(TAG + ": SystemProperties spoof installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [system properties] " + t);
        }
    }

    private String getSpoofedSystemProperty(String key) {
        if (key == null) return null;
        switch (key) {
            case "gsm.operator.iso-country":
            case "gsm.sim.operator.iso-country":
                return COUNTRY_ISO_LOWER;
            case "gsm.operator.numeric":
            case "gsm.sim.operator.numeric":
                return OPERATOR;
            case "gsm.operator.alpha":
            case "gsm.sim.operator.alpha":
                return OPERATOR_NAME;
            case "persist.sys.country":
            case "ro.product.locale.region":
                return "VN";
            case "persist.sys.locale":
            case "ro.product.locale":
                return APP_LOCALE_TAG;
            default:
                return null;
        }
    }

    private void hookLocale() {
        try {
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", XC_MethodReplacement.returnConstant(APP_LOCALE));
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", Locale.Category.class, XC_MethodReplacement.returnConstant(APP_LOCALE));
            XposedBridge.log(TAG + ": Locale spoof installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [locale] " + t);
        }
    }

    private void installVietnameseRecommendationLanguage(ClassLoader classLoader) {
        try {
            Class<?> service = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.contentlanguage.ContentLanguageServiceImpl",
                    classLoader
            );
            if (service != null) {
                XposedHelpers.findAndHookMethod(
                        service,
                        "getContentLanguage",
                        XC_MethodReplacement.returnConstant(CONTENT_LANGUAGE)
                );
                XposedHelpers.findAndHookMethod(service, "getLanguage", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        return new ArrayList<>(Collections.singletonList(CONTENT_LANGUAGE));
                    }
                });
            }

            Class<?> guideService = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.contentlanguage.api.ContentLanguageGuideServiceImpl",
                    classLoader
            );
            if (guideService != null) {
                XposedHelpers.findAndHookMethod(
                        guideService,
                        "getContentLanguage",
                        XC_MethodReplacement.returnConstant(CONTENT_LANGUAGE)
                );
            }

            if (service == null && guideService == null) {
                XposedBridge.log(TAG + ": content-language services not found");
                return;
            }
            XposedBridge.log(TAG + ": Vietnamese recommendation language installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [recommendation language] " + t);
        }
    }

    private void installRecommendationFeedRegionOverride(ClassLoader classLoader) {
        final String className = "com.ss.android.ugc.aweme.net.partner.ApiAlisgTTNetHandler";
        try {
            Class<?> handlerClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (handlerClass == null) {
                XposedBridge.log(TAG + ": recommendation feed request handler not found");
                return;
            }

            int hooks = 0;
            for (Method method : handlerClass.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (Modifier.isStatic(method.getModifiers())
                        || method.getReturnType() != void.class
                        || params.length != 2
                        || params[0].isPrimitive()) {
                    continue;
                }

                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.args.length == 0) return;
                        rewriteRecommendationFeedRegion(param.args[0]);
                    }
                });
                hooks++;
            }
            XposedBridge.log(TAG + ": recommendation feed RU request override installed; hooks="
                    + hooks);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [recommendation feed request override] " + t);
        }
    }

    private void rewriteRecommendationFeedRegion(Object requestContext) {
        if (requestContext == null) return;
        try {
            Object request = findRequestObject(requestContext);
            if (request == null) return;

            Object urlValue = callNoArg(request, "getUrl");
            if (!(urlValue instanceof String)
                    || !isRecommendationFeedUrl((String) urlValue)) {
                return;
            }

            Map<Object, Object> query = findQueryMapFromRequestContext(requestContext, request);
            if (query == null) {
                XposedBridge.log(TAG + ": recommendation feed query map not found");
                return;
            }

            for (String[] override : RECOMMENDATION_FEED_OVERRIDES) {
                replaceQueryValue(query, override[0], override[1]);
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [recommendation feed rewrite] " + t);
        }
    }

    private Object findRequestObject(Object requestContext) {
        Object typed = findFieldValueByTypeName(
                requestContext,
                "com.bytedance.retrofit2.client.Request"
        );
        if (typed != null) return typed;
        for (String fieldName : new String[]{"LIZ", "LIZJ", "request"}) {
            Object value = findFieldValue(requestContext, fieldName);
            if (value != null && callNoArg(value, "getUrl") != null) {
                return value;
            }
        }
        return null;
    }

    private Map<Object, Object> findQueryMapFromRequestContext(Object requestContext, Object request) {
        for (String fieldName : new String[]{"LIZ", "LIZJ", "LJI", "LJFF"}) {
            Object candidate = findFieldValue(requestContext, fieldName);
            Map<Object, Object> map = findQueryLikeMapDeep(candidate, 2);
            if (map != null) return map;
        }
        Map<Object, Object> fromContext = findQueryLikeMapDeep(requestContext, 2);
        if (fromContext != null) return fromContext;
        return findQueryLikeMapDeep(request, 2);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> findQueryLikeMapDeep(Object target, int depth) {
        if (target == null || depth < 0) return null;
        if (target instanceof Map<?, ?>) {
            Map<Object, Object> map = (Map<Object, Object>) target;
            if (looksLikeUrlQuery(map)) return map;
        }

        Map<Object, Object> direct = findMapField(target);
        if (direct != null && looksLikeUrlQuery(direct)) return direct;

        for (Class<?> cls = target.getClass(); cls != null; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (fieldType.isPrimitive() || fieldType == String.class) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value == null) continue;
                    if (value instanceof Map<?, ?>) {
                        Map<Object, Object> map = (Map<Object, Object>) value;
                        if (looksLikeUrlQuery(map)) return map;
                    } else if (depth > 0) {
                        Map<Object, Object> nested = findQueryLikeMapDeep(value, depth - 1);
                        if (nested != null) return nested;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private boolean isRecommendationFeedUrl(String url) {
        return url.contains(RECOMMENDATION_FEED_PATH)
                || url.contains(LEGACY_RECOMMENDATION_FEED_PATH);
    }

    private Object findFieldValueByTypeName(Object target, String typeName) {
        if (target == null) return null;
        for (Class<?> cls = target.getClass(); cls != null; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) {
                if (!typeName.equals(field.getType().getName())) continue;
                try {
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> findMapField(Object target) {
        if (target == null) return null;
        for (Class<?> cls = target.getClass(); cls != null; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof Map<?, ?>) return (Map<Object, Object>) value;
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private boolean looksLikeUrlQuery(Map<Object, Object> candidate) {
        if (candidate == null) return false;
        return candidate.containsKey("aid")
                || candidate.containsKey("device_platform")
                || candidate.containsKey("region")
                || candidate.containsKey("app_language");
    }

    private void replaceQueryValue(Map<Object, Object> query, String key, String value) {
        Object actualKey = key;
        for (Map.Entry<Object, Object> entry : query.entrySet()) {
            if (entry.getKey() instanceof String
                    && key.equalsIgnoreCase((String) entry.getKey())) {
                actualKey = entry.getKey();
                break;
            }
        }

        query.put(actualKey, new ArrayList<>(Collections.singletonList(value)));
    }


    private void installGoogleLoginFix(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            forceBooleanNoArgMethods("com.bytedance.lobby.google.GoogleAuth", lpparam.classLoader, false);
            forceBooleanNoArgMethods("com.bytedance.lobby.google.GoogleOneTapAuth", lpparam.classLoader, false);
            hookBooleanMethodByName(
                    "com.ss.android.ugc.aweme.account.login.googleonetap.GoogleOneTapService",
                    lpparam.classLoader,
                    "LIZJ",
                    false
            );
            XposedBridge.log(TAG + ": Google login fix installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [google login fix] " + t);
        }
    }

    private void hookBooleanMethodByName(String className, ClassLoader cl, String methodName, boolean returnValue) {
        try {
            Class<?> cls = XposedHelpers.findClassIfExists(className, cl);
            if (cls == null) {
                XposedBridge.log(TAG + ": class not found " + className);
                return;
            }
            for (Method m : cls.getDeclaredMethods()) {
                if (!methodName.equals(m.getName()) || m.getReturnType() != boolean.class) {
                    continue;
                }
                XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(returnValue));
                XposedBridge.log(TAG + ": hooked " + className + "#" + m.getName() + "() -> " + returnValue);
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [hookBooleanMethodByName " + className + "#" + methodName + "] " + t);
        }
    }

    private void forceBooleanNoArgMethods(String className, ClassLoader cl, boolean returnValue) {
        try {
            Class<?> cls = XposedHelpers.findClassIfExists(className, cl);
            if (cls == null) {
                XposedBridge.log(TAG + ": class not found " + className);
                return;
            }
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getParameterTypes().length == 0
                        && m.getReturnType() == boolean.class
                        && Modifier.isFinal(m.getModifiers())
                        && Modifier.isPublic(m.getModifiers())) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(returnValue));
                    XposedBridge.log(TAG + ": hooked " + className + "#" + m.getName() + "() -> " + returnValue);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " [forceBooleanNoArgMethods " + className + "] " + t);
        }
    }
}
