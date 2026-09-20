package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackSpeedPolicyTest {
    @Test
    public void clampsSpeed() {
        assertEquals(0.25f, PlaybackSpeedPolicy.clamp(0.01f), 0.0001f);
        assertEquals(3.0f, PlaybackSpeedPolicy.clamp(9f), 0.0001f);
        assertEquals(1.5f, PlaybackSpeedPolicy.clamp(1.5f), 0.0001f);
    }

    @Test
    public void appliesOnlyWhenEnabledAndNotDefault() {
        assertTrue(PlaybackSpeedPolicy.shouldApply(true, 1.25f));
        assertFalse(PlaybackSpeedPolicy.shouldApply(false, 1.5f));
        assertFalse(PlaybackSpeedPolicy.shouldApply(true, 1.0f));
    }

    @Test
    public void parsesPreferenceStrings() {
        assertEquals(1.75f, PlaybackSpeedPolicy.fromPreferenceString("1.75"), 0.0001f);
        assertEquals(1.0f, PlaybackSpeedPolicy.fromPreferenceString("nope"), 0.0001f);
    }
}
