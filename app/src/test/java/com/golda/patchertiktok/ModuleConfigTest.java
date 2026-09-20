package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class ModuleConfigTest {
    @Test
    public void defaultsMatchLegacyHardcodedBehavior() {
        ModuleConfig config = ModuleConfig.defaults();
        assertTrue(config.vietnamRegion);
        assertTrue(config.hideFeedAds);
        assertTrue(config.hideLive);
        assertTrue(config.forceSeekbar);
        assertTrue(config.downloadNoWatermark);
        assertFalse(config.hidePhotoPosts);
        assertFalse(config.playbackSpeedEnabled);
        assertTrue(config.keywordBlacklist.isEmpty());
    }

    @Test
    public void parsesKeywords() {
        assertEquals(
                new HashSet<>(Arrays.asList("spam", "giveaway")),
                ModuleConfig.parseKeywords("Spam, giveaway\n")
        );
        assertTrue(ModuleConfig.parseKeywords(null).isEmpty());
    }

    @Test
    public void sanitizesSpeed() {
        assertEquals(1.0f, ModuleConfig.sanitizeSpeed(Float.NaN), 0.0001f);
        assertEquals(0.25f, ModuleConfig.sanitizeSpeed(0.01f), 0.0001f);
        assertEquals(2.0f, ModuleConfig.sanitizeSpeed(2.0f), 0.0001f);
    }

    @Test
    public void feedFilterFlagAggregation() {
        ModuleConfig defaults = ModuleConfig.defaults();
        assertTrue(defaults.anyFeedFilterEnabled());
        assertFalse(defaults.anyPurificationEnabled());
    }
}
