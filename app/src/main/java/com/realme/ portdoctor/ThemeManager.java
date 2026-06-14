package com.realme.portdoctor;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public class ThemeManager {

    public static final int THEME_DARK = 0;
    public static final int THEME_LIGHT = 1;

    private int currentTheme;

    public static class Dark {
        public static final String BG_START = "#0F0C29";
        public static final String BG_MID = "#302B63";
        public static final String BG_END = "#24243E";
        public static final String CARD_BG = "#22FFFFFF";
        public static final String CARD_BORDER = "#33FFFFFF";
        public static final String TEXT_PRIMARY = "#FFFFFF";
        public static final String TEXT_SECONDARY = "#B0B0D0";
        public static final String LOG_TEXT = "#8888AA";
        public static final String ISSUE_BG = "#22FFFFFF";
    }

    public static class Light {
        public static final String BG_START = "#F5F7FA";
        public static final String BG_MID = "#E4E8F0";
        public static final String BG_END = "#D0D5E0";
        public static final String CARD_BG = "#FFFFFF";
        public static final String CARD_BORDER = "#E0E0E0";
        public static final String TEXT_PRIMARY = "#1A1A2E";
        public static final String TEXT_SECONDARY = "#555577";
        public static final String LOG_TEXT = "#666688";
        public static final String ISSUE_BG = "#F0F0F5";
    }

    public ThemeManager(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("portdoctor", Activity.MODE_PRIVATE);
        currentTheme = prefs.getInt("theme", THEME_DARK);
    }

    public int getTheme() { return currentTheme; }

    public void toggle(Activity activity) {
        currentTheme = (currentTheme == THEME_DARK) ? THEME_LIGHT : THEME_DARK;
        activity.getSharedPreferences("portdoctor", Activity.MODE_PRIVATE)
            .edit().putInt("theme", currentTheme).commit();
    }

    public boolean isDark() { return currentTheme == THEME_DARK; }

    public int[] getBackgroundGradient() {
        if (isDark()) {
            return new int[]{Color.parseColor(Dark.BG_START), 
                Color.parseColor(Dark.BG_MID), Color.parseColor(Dark.BG_END)};
        } else {
            return new int[]{Color.parseColor(Light.BG_START), 
                Color.parseColor(Light.BG_MID), Color.parseColor(Light.BG_END)};
        }
    }

    public String getCardBg() { return isDark() ? Dark.CARD_BG : Light.CARD_BG; }
    public String getCardBorder() { return isDark() ? Dark.CARD_BORDER : Light.CARD_BORDER; }
    public String getTextPrimary() { return isDark() ? Dark.TEXT_PRIMARY : Light.TEXT_PRIMARY; }
    public String getTextSecondary() { return isDark() ? Dark.TEXT_SECONDARY : Light.TEXT_SECONDARY; }
    public String getLogText() { return isDark() ? Dark.LOG_TEXT : Light.LOG_TEXT; }
    public String getIssueBg() { return isDark() ? Dark.ISSUE_BG : Light.ISSUE_BG; }

    public GradientDrawable createBackground() {
        return new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, getBackgroundGradient());
    }
}
