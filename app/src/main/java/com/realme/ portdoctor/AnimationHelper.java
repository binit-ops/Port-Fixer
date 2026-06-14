package com.realme.portdoctor;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

public class AnimationHelper {

    public static void fadeIn(View view, int duration, int delay) {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(duration);
        anim.setStartOffset(delay);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    public static void fadeOut(View view, int duration) {
        AlphaAnimation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(duration);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    public static void scaleIn(View view, int duration) {
        ScaleAnimation anim = new ScaleAnimation(0.8f, 1f, 0.8f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(duration);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    public static void slideUp(View view, int duration, int delay) {
        TranslateAnimation anim = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0.3f,
            Animation.RELATIVE_TO_SELF, 0f);
        anim.setDuration(duration);
        anim.setStartOffset(delay);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    public static void slideDown(View view, int duration) {
        TranslateAnimation anim = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0.3f);
        anim.setDuration(duration);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    public static void pulse(View view) {
        ScaleAnimation anim = new ScaleAnimation(1f, 1.05f, 1f, 1.05f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(300);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        view.startAnimation(anim);
    }

    public static void shake(View view) {
        TranslateAnimation anim = new TranslateAnimation(-10, 10, 0, 0);
        anim.setDuration(100);
        anim.setRepeatCount(3);
        anim.setRepeatMode(Animation.REVERSE);
        view.startAnimation(anim);
    }
}
