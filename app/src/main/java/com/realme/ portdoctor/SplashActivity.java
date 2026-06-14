package com.realme.portdoctor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public class SplashActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#0F0C29"), Color.parseColor("#302B63"), Color.parseColor("#24243E")}
        );
        layout.setBackground(bg);

        TextView icon = new TextView(this);
        icon.setText("P");
        icon.setTextSize(80);
        icon.setTextColor(Color.parseColor("#FF6B6B"));
        icon.setTypeface(null, android.graphics.Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        layout.addView(icon);

        TextView title = new TextView(this);
        title.setText("Port Doctor");
        title.setTextSize(32);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 10);
        layout.addView(title);

        TextView sub = new TextView(this);
        sub.setText("ROM Port Diagnostic Suite");
        sub.setTextSize(14);
        sub.setTextColor(Color.parseColor("#B0B0D0"));
        sub.setGravity(Gravity.CENTER);
        layout.addView(sub);

        TextView ver = new TextView(this);
        ver.setText("v2.0");
        ver.setTextSize(11);
        ver.setTextColor(Color.parseColor("#666688"));
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, 30, 0, 0);
        layout.addView(ver);

        setContentView(layout);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(1200);
        fadeIn.setStartOffset(300);

        ScaleAnimation scaleIn = new ScaleAnimation(0.5f, 1f, 0.5f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleIn.setDuration(1000);

        icon.startAnimation(scaleIn);
        title.startAnimation(fadeIn);
        sub.startAnimation(fadeIn);
        ver.startAnimation(fadeIn);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2000);
    }
}
