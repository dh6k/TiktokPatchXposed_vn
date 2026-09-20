package com.golda.patchertiktok;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

public final class SettingsProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context context = getContext();
        if (context == null) {
            return Bundle.EMPTY;
        }
        if (!isAuthorizedCaller(context)) {
            Bundle denied = new Bundle();
            denied.putBoolean("ok", false);
            denied.putString("error", "unauthorized");
            return denied;
        }
        if (ModuleConfig.METHOD_PING.equals(method)) {
            Bundle ping = new Bundle();
            ping.putBoolean("ok", true);
            return ping;
        }
        if (ModuleConfig.METHOD_GET_CONFIG.equals(method) || method == null) {
            SharedPreferences prefs = context.getSharedPreferences(
                    ModuleConfig.PREFS, Context.MODE_PRIVATE);
            Bundle config = ModuleConfig.fromPreferences(prefs).toBundle();
            config.putBoolean("ok", true);
            return config;
        }
        Bundle unknown = new Bundle();
        unknown.putBoolean("ok", false);
        unknown.putString("error", "unknown_method");
        return unknown;
    }

    private static boolean isAuthorizedCaller(Context context) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid()) {
            return true;
        }
        String[] packages = context.getPackageManager().getPackagesForUid(callingUid);
        if (packages == null) {
            return false;
        }
        for (String packageName : packages) {
            if ("com.zhiliaoapp.musically".equals(packageName)
                    || "com.ss.android.ugc.trill".equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
