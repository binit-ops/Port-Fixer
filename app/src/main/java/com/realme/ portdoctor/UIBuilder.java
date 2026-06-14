package com.realme.portdoctor;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public class UIBuilder {

    public static final String bgStart = "#0F0C29";
    public static final String bgMid = "#302B63";
    public static final String bgEnd = "#24243E";
    public static final String glassBg = "#22FFFFFF";
    public static final String glassBorder = "#33FFFFFF";
    public static final String accent = "#FF6B6B";
    public static final String accent2 = "#4ECDC4";
    public static final String accent3 = "#FFE66D";
    public static final String textPrimary = "#FFFFFF";
    public static final String textSecondary = "#B0B0D0";
    public static final String green = "#2ED573";
    public static final String red = "#FF4757";
    public static final String orange = "#FFA502";
    public static final String purple = "#7C4DFF";
    public static final String blue = "#1E90FF";
    public static final String teal = "#00CED1";

    public static LinearLayout glassCard(int radius, int marginTop) {
        LinearLayout card = new LinearLayout(Activity.getAppContext());
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(radius * 2);
        gd.setColor(Color.parseColor(glassBg));
        gd.setStroke(1, Color.parseColor(glassBorder));
        card.setBackground(gd);
        card.setPadding(20, 15, 20, 15);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, marginTop, 0, 0);
        card.setLayoutParams(params);
        card.setElevation(10);
        return card;
    }

    public static Button gradientButton(String text, String color1, String color2) {
        Button btn = new Button(Activity.getAppContext());
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor(color1), Color.parseColor(color2)}
        );
        gd.setCornerRadius(50);
        btn.setBackground(gd);
        btn.setPadding(30, 18, 30, 18);
        btn.setElevation(8);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 12, 0, 0);
        btn.setLayoutParams(params);
        return btn;
    }

    public static Button smallButton(String text, String color, int marginTop) {
        Button btn = new Button(Activity.getAppContext());
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11);
        btn.setAllCaps(false);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(25);
        gd.setColor(Color.parseColor(color));
        gd.setStroke(1, Color.parseColor("#44FFFFFF"));
        btn.setBackground(gd);
        btn.setPadding(18, 10, 18, 10);
        btn.setElevation(4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, marginTop, 0, 0);
        btn.setLayoutParams(params);
        return btn;
    }
}
