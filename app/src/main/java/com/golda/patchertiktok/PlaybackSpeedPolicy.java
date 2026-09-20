package com.golda.patchertiktok;

/**
 * Playback-speed helpers kept free of Xposed APIs for unit tests.
 */
public final class PlaybackSpeedPolicy {
    private PlaybackSpeedPolicy() {
    }

    public static float clamp(float speed) {
        return ModuleConfig.sanitizeSpeed(speed);
    }

    public static boolean shouldApply(boolean enabled, float speed) {
        return enabled && speed > 0f && Math.abs(speed - 1.0f) > 0.001f;
    }

    public static float fromPreferenceString(String raw) {
        return ModuleConfig.sanitizeSpeed(ModuleConfig.parseFloat(raw, 1.0f));
    }
}
