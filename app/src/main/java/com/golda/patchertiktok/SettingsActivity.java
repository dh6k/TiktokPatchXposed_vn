package com.golda.patchertiktok;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Lean settings screen. Defaults keep the previous hardcoded Vietnam behavior.
 */
public final class SettingsActivity extends Activity {

    private SharedPreferences prefs;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(ModuleConfig.PREFS, Context.MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(header("TiktokPatchXposed"));
        statusView = note(buildStatus());
        root.addView(statusView);
        root.addView(note(
                "THIS IS THE SETTINGS APP (not an overlay in TikTok).\n"
                        + "Required order:\n"
                        + "1) Open TikTok once — log must show \"config broadcast receiver registered\".\n"
                        + "2) Leave TikTok running (background is OK).\n"
                        + "3) Open this app, change toggles (status: broadcast=sent).\n"
                        + "4) Force-stop TikTok, reopen it.\n"
                        + "5) Log should show config source=runtime-prefs (or live broadcast apply)."));
        Button resend = new Button(this);
        resend.setText("Resend config broadcast now");
        resend.setOnClickListener(v -> {
            prefs.edit().commit();
            boolean ok = ModuleConfig.broadcastConfig(this, prefs);
            Toast.makeText(this, ok ? "Broadcast sent to TikTok packages" : "Broadcast failed",
                    Toast.LENGTH_SHORT).show();
            persist();
        });
        root.addView(resend);

        root.addView(section("Vietnam profile"));
        bindSwitch(root, ModuleConfig.KEY_VIETNAM_REGION, "Vietnam SIM/region spoof");
        bindSwitch(root, ModuleConfig.KEY_VIETNAMESE_LANGUAGE, "Vietnamese language / content");
        bindSwitch(root, ModuleConfig.KEY_FEED_REGION_OVERRIDE, "Feed request region override (VN)");

        root.addView(section("Feed filters"));
        bindSwitch(root, ModuleConfig.KEY_HIDE_FEED_ADS, "Hide ads");
        bindSwitch(root, ModuleConfig.KEY_HIDE_LIVE, "Hide LIVE + LIVE button");
        bindSwitch(root, ModuleConfig.KEY_HIDE_SUGGESTED, "Hide “People you may know”");
        bindSwitch(root, ModuleConfig.KEY_HIDE_PHOTO_POSTS, "Hide photo posts");
        bindSwitch(root, ModuleConfig.KEY_HIDE_AI_POSTS, "Hide AI-generated posts");
        bindSwitch(root, ModuleConfig.KEY_HIDE_LONG_POSTS, "Hide long videos");
        bindLongField(root, ModuleConfig.KEY_LONG_POST_SECONDS, "Long video threshold (sec)", "60");
        bindSwitch(root, ModuleConfig.KEY_FILTER_METRICS, "Filter by views/likes");
        bindLongField(root, ModuleConfig.KEY_VIEWS_MIN, "Views min", "0");
        bindLongField(root, ModuleConfig.KEY_VIEWS_MAX, "Views max (blank = no limit)", "");
        bindLongField(root, ModuleConfig.KEY_LIKES_MIN, "Likes min", "0");
        bindLongField(root, ModuleConfig.KEY_LIKES_MAX, "Likes max (blank = no limit)", "");
        bindTextField(root, ModuleConfig.KEY_KEYWORD_BLACKLIST,
                "Keyword blacklist (comma / newline)",
                "spam,giveaway");

        root.addView(section("Player / download"));
        bindSwitch(root, ModuleConfig.KEY_DOWNLOAD_NO_WATERMARK, "Download without watermark");
        bindSwitch(root, ModuleConfig.KEY_FORCE_SEEKBAR, "Always show seekbar");
        bindSwitch(root, ModuleConfig.KEY_HIDE_SPLASH_ADS, "Hide splash / TopView ads");
        bindSwitch(root, ModuleConfig.KEY_GOOGLE_LOGIN_FIX, "Google login fix");
        bindSwitch(root, ModuleConfig.KEY_PLAYBACK_SPEED_ENABLED, "Auto playback speed");
        bindSpinner(root, ModuleConfig.KEY_PLAYBACK_SPEED,
                new String[]{"1.0", "1.25", "1.5", "1.75", "2.0"}, "1.25");

        root.addView(section("Page purification"));
        bindSwitch(root, ModuleConfig.KEY_HIDE_AUTHOR_AVATAR, "Hide author avatar");
        bindSwitch(root, ModuleConfig.KEY_HIDE_AUTHOR_INFO, "Hide author name / info");
        bindSwitch(root, ModuleConfig.KEY_HIDE_VIDEO_DESC, "Hide video description");
        bindSwitch(root, ModuleConfig.KEY_HIDE_MUSIC_TITLE, "Hide music title");
        bindSwitch(root, ModuleConfig.KEY_HIDE_ACTION_BUTTONS, "Hide like/comment/share row");
        bindSwitch(root, ModuleConfig.KEY_HIDE_TOP_NAV, "Hide top tabs (Following / For You)");
        bindSwitch(root, ModuleConfig.KEY_HIDE_SEARCH, "Hide search entry");
        bindSwitch(root, ModuleConfig.KEY_HIDE_BOTTOM_NAV, "Hide bottom navigation");

        root.addView(section("Maintenance"));
        Button reset = new Button(this);
        reset.setText("Reset to defaults");
        reset.setOnClickListener(v -> {
            prefs.edit().clear().commit();
            ModuleConfig.writeMirror(this, prefs);
            Toast.makeText(this, "Defaults restored. Restart TikTok.", Toast.LENGTH_LONG).show();
            recreate();
        });
        root.addView(reset);

        root.addView(note("Xposed module may cause instability or account limits. Use at your own risk."));

        setContentView(scroll);
    }

    private String lastPersistNote = "";

    private void persist() {
        boolean broadcast = ModuleConfig.broadcastConfig(this, prefs);
        boolean publicFile = ModuleConfig.writePublicMirror(this, prefs);
        ModuleConfig.writeMirror(this, prefs);
        lastPersistNote = "broadcast=" + (broadcast ? "sent" : "failed")
                + " public-file=" + (publicFile ? "written" : "failed");
        if (statusView != null) {
            statusView.setText(buildStatus());
        }
    }

    private String buildStatus() {
        int keyCount;
        try {
            keyCount = prefs.getAll().size();
        } catch (RuntimeException e) {
            keyCount = -1;
        }
        String bridge;
        try {
            android.os.Bundle result = getContentResolver().call(
                    ModuleConfig.settingsUri(), ModuleConfig.METHOD_PING, null, null);
            bridge = (result != null && result.getBoolean("ok", false))
                    ? "provider ping OK"
                    : "provider ping failed";
        } catch (RuntimeException e) {
            bridge = "provider ping error: " + e.getClass().getSimpleName();
        }
        File mirror = new File(getFilesDir(), ModuleConfig.MIRROR_FILE);
        return "Saved keys: " + keyCount
                + " | " + bridge
                + " | mirror: " + (mirror.isFile() ? "yes" + " (" + mirror.length() + "B)" : "no")
                + (lastPersistNote.isEmpty() ? "" : " | " + lastPersistNote)
                + "\n1) Open TikTok once (register receiver). 2) Change toggles here. "
                + "3) Force-stop TikTok, reopen. Log should show config source=runtime-prefs or public-file.";
    }

    private TextView header(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(22f);
        view.setPadding(0, 0, 0, 12);
        return view;
    }

    private TextView section(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16f);
        view.setPadding(0, 24, 0, 8);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView note(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12f);
        view.setPadding(0, 8, 0, 8);
        return view;
    }

    private void bindSwitch(LinearLayout root, String key, String label) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setChecked(prefs.getBoolean(key, defaultsBoolean(key)));
        box.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(key, isChecked).commit();
            persist();
        });
        root.addView(box);
    }

    private void bindLongField(LinearLayout root, String key, String label, String fallback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView caption = new TextView(this);
        caption.setText(label);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(prefs.getString(key, fallback));
        input.addTextChangedListener(new SimpleWatcher(value -> {
            prefs.edit().putString(key, value).commit();
            persist();
        }));
        row.addView(caption);
        row.addView(input);
        root.addView(row);
    }

    private void bindTextField(LinearLayout root, String key, String label, String fallback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView caption = new TextView(this);
        caption.setText(label);
        EditText input = new EditText(this);
        input.setMinLines(2);
        input.setText(prefs.getString(key, fallback));
        input.addTextChangedListener(new SimpleWatcher(value -> {
            prefs.edit().putString(key, value).commit();
            persist();
        }));
        row.addView(caption);
        row.addView(input);
        root.addView(row);
    }

    private void bindSpinner(LinearLayout root, String key, String[] options, String fallback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView caption = new TextView(this);
        caption.setText("Playback speed value");
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, options);
        spinner.setAdapter(adapter);
        String current = prefs.getString(key, fallback);
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(current)) {
                index = i;
                break;
            }
        }
        spinner.setSelection(index);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.edit().putString(key, options[position]).commit();
                persist();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        row.addView(caption);
        row.addView(spinner);
        root.addView(row);
    }

    private static boolean defaultsBoolean(String key) {
        ModuleConfig defaults = ModuleConfig.defaults();
        switch (key) {
            case ModuleConfig.KEY_VIETNAM_REGION:
                return defaults.vietnamRegion;
            case ModuleConfig.KEY_VIETNAMESE_LANGUAGE:
                return defaults.vietnameseLanguage;
            case ModuleConfig.KEY_FEED_REGION_OVERRIDE:
                return defaults.feedRegionOverride;
            case ModuleConfig.KEY_DOWNLOAD_NO_WATERMARK:
                return defaults.downloadNoWatermark;
            case ModuleConfig.KEY_HIDE_FEED_ADS:
                return defaults.hideFeedAds;
            case ModuleConfig.KEY_HIDE_LIVE:
                return defaults.hideLive;
            case ModuleConfig.KEY_HIDE_SUGGESTED:
                return defaults.hideSuggested;
            case ModuleConfig.KEY_HIDE_SPLASH_ADS:
                return defaults.hideSplashAds;
            case ModuleConfig.KEY_FORCE_SEEKBAR:
                return defaults.forceSeekbar;
            case ModuleConfig.KEY_GOOGLE_LOGIN_FIX:
                return defaults.googleLoginFix;
            case ModuleConfig.KEY_HIDE_PHOTO_POSTS:
                return defaults.hidePhotoPosts;
            case ModuleConfig.KEY_HIDE_AI_POSTS:
                return defaults.hideAiPosts;
            case ModuleConfig.KEY_HIDE_LONG_POSTS:
                return defaults.hideLongPosts;
            case ModuleConfig.KEY_FILTER_METRICS:
                return defaults.filterMetrics;
            case ModuleConfig.KEY_PLAYBACK_SPEED_ENABLED:
                return defaults.playbackSpeedEnabled;
            case ModuleConfig.KEY_HIDE_AUTHOR_AVATAR:
                return defaults.hideAuthorAvatar;
            case ModuleConfig.KEY_HIDE_AUTHOR_INFO:
                return defaults.hideAuthorInfo;
            case ModuleConfig.KEY_HIDE_VIDEO_DESC:
                return defaults.hideVideoDesc;
            case ModuleConfig.KEY_HIDE_MUSIC_TITLE:
                return defaults.hideMusicTitle;
            case ModuleConfig.KEY_HIDE_ACTION_BUTTONS:
                return defaults.hideActionButtons;
            case ModuleConfig.KEY_HIDE_TOP_NAV:
                return defaults.hideTopNav;
            case ModuleConfig.KEY_HIDE_SEARCH:
                return defaults.hideSearch;
            case ModuleConfig.KEY_HIDE_BOTTOM_NAV:
                return defaults.hideBottomNav;
            default:
                return false;
        }
    }

    private interface TextCallback {
        void onText(String value);
    }

    private static final class SimpleWatcher implements TextWatcher {
        private final TextCallback callback;

        SimpleWatcher(TextCallback callback) {
            this.callback = callback;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            callback.onText(s == null ? "" : s.toString());
        }
    }
}
