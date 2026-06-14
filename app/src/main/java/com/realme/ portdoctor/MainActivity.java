package com.realme.portdoctor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private PortScanner scanner;
    private FixEngine fixEngine;
    private LinearLayout issueContainer;
    private TextView logText;
    private TextView statusText;
    private TextView deviceLabel;
    private Button scanBtn;
    private Button fixAllBtn;
    private Button buildBtn;
    private CheckBox[] checkBoxes;
    private List detectedIssues;
    private List fixes;
    private VendorDB vendorDB;
    private FixPackServer fixPackServer;
    private String lastReport;
    private ProgressDialog progressDialog;

    // Glassmorphism Colors
    private final String bgStart = "#0F0C29";
    private final String bgMid = "#302B63";
    private final String bgEnd = "#24243E";
    private final String glassBg = "#22FFFFFF";
    private final String glassBorder = "#33FFFFFF";
    private final String accent = "#FF6B6B";
    private final String accent2 = "#4ECDC4";
    private final String accent3 = "#FFE66D";
    private final String textPrimary = "#FFFFFF";
    private final String textSecondary = "#B0B0D0";
    private final String green = "#2ED573";
    private final String red = "#FF4757";
    private final String orange = "#FFA502";
    private final String purple = "#7C4DFF";
    private final String blue = "#1E90FF";
    private final String teal = "#00CED1";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Main scrollable layout
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 40, 20, 40);

        // Gradient background
        GradientDrawable bgDrawable = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor(bgStart), Color.parseColor(bgMid), Color.parseColor(bgEnd)}
        );
        layout.setBackground(bgDrawable);

        // ==================== HEADER SECTION ====================
        LinearLayout headerCard = createGlassCard(20, 25);
        headerCard.setOrientation(LinearLayout.VERTICAL);

        // App icon and title row
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(View.Gravity.CENTER_VERTICAL);

        TextView iconView = new TextView(this);
        iconView.setText("🩺");
        iconView.setTextSize(40);
        titleRow.addView(iconView);

        LinearLayout titleTextCol = new LinearLayout(this);
        titleTextCol.setOrientation(LinearLayout.VERTICAL);
        titleTextCol.setPadding(15, 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("Port Doctor");
        title.setTextSize(30);
        title.setTextColor(Color.parseColor(textPrimary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTextCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ROM Port Diagnostic Suite");
        subtitle.setTextColor(Color.parseColor(textSecondary));
        subtitle.setTextSize(13);
        titleTextCol.addView(subtitle);

        titleRow.addView(titleTextCol);
        headerCard.addView(titleRow);

        // Device info chip
        LinearLayout deviceChip = createGlassCard(8, 12);
        deviceChip.setPadding(15, 10, 15, 10);

        deviceLabel = new TextView(this);
        String cd = getprop("ro.product.model", "Unknown Device");
        deviceLabel.setText("📱  " + cd);
        deviceLabel.setTextColor(Color.parseColor(accent3));
        deviceLabel.setTextSize(12);
        deviceChip.addView(deviceLabel);

        Button changeDeviceBtn = createSmallButton("Switch Device", purple, 8);
        changeDeviceBtn.setOnClickListener(v -> showDeviceSelector());
        deviceChip.addView(changeDeviceBtn);

        headerCard.addView(deviceChip);
        layout.addView(headerCard);

        // ==================== STATUS CARD ====================
        LinearLayout statusCard = createGlassCard(15, 15);
        statusCard.setPadding(20, 12, 20, 12);

        statusText = new TextView(this);
        statusText.setText("🟢  Ready to diagnose your ROM");
        statusText.setTextColor(Color.parseColor(textPrimary));
        statusText.setTextSize(14);
        statusCard.addView(statusText);
        layout.addView(statusCard);

        // ==================== SCAN BUTTON ====================
        scanBtn = createGradientButton("🔍  Scan Device for Issues", accent, accent2);
        scanBtn.setOnClickListener(v -> runScan());
        layout.addView(scanBtn);

        // ==================== ISSUES SECTION ====================
        TextView issuesHeader = new TextView(this);
        issuesHeader.setText("\n📋  DETECTED ISSUES");
        issuesHeader.setTextColor(Color.parseColor(textSecondary));
        issuesHeader.setTextSize(11);
        issuesHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        issuesHeader.setPadding(10, 20, 0, 10);
        layout.addView(issuesHeader);

        issueContainer = new LinearLayout(this);
        issueContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(issueContainer);

        // ==================== ACTION BUTTONS ====================
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 15, 0, 0);

        fixAllBtn = createSmallButton("⚡  Generate Fixes", green, 4);
        fixAllBtn.setEnabled(false);
        fixAllBtn.setAlpha(0.5f);
        fixAllBtn.setOnClickListener(v -> generateFixes());
        actionRow.addView(fixAllBtn);

        View gap1 = new View(this);
        gap1.setLayoutParams(new LinearLayout.LayoutParams(10, 1));
        actionRow.addView(gap1);

        buildBtn = createSmallButton("📦  Build Module", blue, 4);
        buildBtn.setEnabled(false);
        buildBtn.setAlpha(0.5f);
        buildBtn.setOnClickListener(v -> buildModule());
        actionRow.addView(buildBtn);

        layout.addView(actionRow);

        // ==================== REPORTS SECTION ====================
        TextView reportsHeader = new TextView(this);
        reportsHeader.setText("\n📊  REPORTS & SHARING");
        reportsHeader.setTextColor(Color.parseColor(textSecondary));
        reportsHeader.setTextSize(11);
        reportsHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        reportsHeader.setPadding(10, 20, 0, 10);
        layout.addView(reportsHeader);

        LinearLayout reportRow = new LinearLayout(this);
        reportRow.setOrientation(LinearLayout.HORIZONTAL);

        Button reportBtn = createSmallButton("📄  Full Report", purple, 4);
        reportBtn.setOnClickListener(v -> generateFullReport());
        reportRow.addView(reportBtn);

        View gap2 = new View(this);
        gap2.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        reportRow.addView(gap2);

        Button shareBtn = createSmallButton("📤  Share", teal, 4);
        shareBtn.setOnClickListener(v -> shareReport());
        reportRow.addView(shareBtn);

        layout.addView(reportRow);

        // ==================== COMMUNITY SECTION ====================
        TextView communityHeader = new TextView(this);
        communityHeader.setText("\n🌐  COMMUNITY TOOLS");
        communityHeader.setTextColor(Color.parseColor(textSecondary));
        communityHeader.setTextSize(11);
        communityHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        communityHeader.setPadding(10, 20, 0, 10);
        layout.addView(communityHeader);

        Button syncBtn = createGradientButton("🔄  Sync Community Database", purple, blue);
        syncBtn.setOnClickListener(v -> syncCommunityDB());
        layout.addView(syncBtn);

        LinearLayout communityRow = new LinearLayout(this);
        communityRow.setOrientation(LinearLayout.HORIZONTAL);
        communityRow.setPadding(0, 8, 0, 0);

        Button browseBtn = createSmallButton("📦  Fix Packs", orange, 4);
        browseBtn.setOnClickListener(v -> browseFixPacks());
        communityRow.addView(browseBtn);

        View gap3 = new View(this);
        gap3.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        communityRow.addView(gap3);

        Button vendorBtn = createSmallButton("🔍  Vendor", red, 4);
        vendorBtn.setOnClickListener(v -> checkVendorFiles());
        communityRow.addView(vendorBtn);

        layout.addView(communityRow);

        // ==================== LOG SECTION ====================
        LinearLayout logCard = createGlassCard(12, 15);
        logCard.setPadding(15, 12, 15, 12);

        logText = new TextView(this);
        logText.setText("📄  Scan log will appear here after scanning...");
        logText.setTextColor(Color.parseColor("#8888AA"));
        logText.setTextSize(11);
        logText.setLineSpacing(3, 1);
        logCard.addView(logText);
        layout.addView(logCard);

        scrollView.addView(layout);
        setContentView(scrollView);

        // Load saved preferences
        SharedPreferences prefs = getSharedPreferences("portdoctor", MODE_PRIVATE);
        String sd = prefs.getString("device", "");
        if (!sd.isEmpty()) {
            deviceLabel.setText("📱  " + sd);
        }
    }

    // ==================== GLASS CARD HELPER ====================
    private LinearLayout createGlassCard(int radius, int marginTop) {
        LinearLayout card = new LinearLayout(this);
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

    // ==================== GRADIENT BUTTON HELPER ====================
    private Button createGradientButton(String text, String color1, String color2) {
        Button btn = new Button(this);
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

    // ==================== SMALL BUTTON HELPER ====================
    private Button createSmallButton(String text, String color, int marginTop) {
        Button btn = new Button(this);
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

    // ==================== CORE METHODS ====================
    private void runScan() {
        scanBtn.setEnabled(false);
        scanBtn.setText("⏳  Scanning hardware...");
        statusText.setText("🔵  Analyzing ROM components...");
        issueContainer.removeAllViews();

        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                SharedPreferences pr = getSharedPreferences("portdoctor", MODE_PRIVATE);
                String d = pr.getString("device", getprop("ro.product.model", "Unknown"));
                String dn = pr.getString("donor", "");
                scanner = new PortScanner(d, dn);
                return scanner.scanAll();
            }
            protected void onPostExecute(Object r) {
                detectedIssues = (List) r;
                displayResults();
                scanBtn.setEnabled(true);
                scanBtn.setText("🔍  Scan Device for Issues");
            }
        }.execute();
    }

    private void displayResults() {
        if (detectedIssues.isEmpty()) {
            statusText.setText("🟢  No issues detected! Your ROM is healthy.");
            logText.setText(scanner.getScanLog());
            return;
        }

        statusText.setText("🟡  Found " + detectedIssues.size() + " issue(s)");
        checkBoxes = new CheckBox[detectedIssues.size()];

        for (int i = 0; i < detectedIssues.size(); i++) {
            PortScanner.Issue issue = (PortScanner.Issue) detectedIssues.get(i);

            LinearLayout issueCard = createGlassCard(10, 6);
            issueCard.setOrientation(LinearLayout.VERTICAL);
            issueCard.setPadding(15, 10, 15, 10);

            CheckBox cb = new CheckBox(this);
            String sevIcon = issue.severity.equals("HIGH") ? "🔴" : 
                             issue.severity.equals("MEDIUM") ? "🟡" : "🟢";
            cb.setText(sevIcon + "  " + issue.name);
            cb.setTextColor(Color.parseColor(textPrimary));
            cb.setChecked(true);
            checkBoxes[i] = cb;

            TextView desc = new TextView(this);
            desc.setText(issue.description);
            desc.setTextColor(Color.parseColor(textSecondary));
            desc.setTextSize(11);
            desc.setPadding(35, 2, 0, 0);

            issueCard.addView(cb);
            issueCard.addView(desc);
            issueContainer.addView(issueCard);
        }

        fixAllBtn.setEnabled(true);
        fixAllBtn.setAlpha(1.0f);
        logText.setText(scanner.getScanLog());
    }

    private void generateFixes() {
        List sel = new ArrayList();
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isChecked()) sel.add(detectedIssues.get(i));
        }
        scanner.getDetectedIssues().clear();
        scanner.getDetectedIssues().addAll(sel);
        fixEngine = new FixEngine(scanner);
        fixes = fixEngine.generateFixes();
        statusText.setText("🟢  Generated " + fixes.size() + " fix(es) - Ready to build!");
        buildBtn.setEnabled(true);
        buildBtn.setAlpha(1.0f);
    }

    private void buildModule() {
        progressDialog = ProgressDialog.show(this, "", "Building Magisk module...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                try {
                    ModuleBuilder.buildModule(fixes, "/sdcard/PortDoctor_FixPack.zip");
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                if ((Boolean) r) {
                    statusText.setText("🟢  Module saved to /sdcard/PortDoctor_FixPack.zip");
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("✨  Module Built!")
                        .setMessage("Flash in Magisk to apply all fixes.\nReboot after flashing.")
                        .setPositiveButton("OK", null)
                        .show();
                } else {
                    statusText.setText("🔴  Build failed!");
                }
            }
        }.execute();
    }

    private void generateFullReport() {
        progressDialog = ProgressDialog.show(this, "", "Creating report...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                return ReportGenerator.generateReport(scanner, fixes);
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                lastReport = (String) r;
                showReportPreview();
            }
        }.execute();
    }

    private void showReportPreview() {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle("📊  Scan Report");
        TextView tv = new TextView(this);
        String preview = lastReport.length() > 2500 ? 
            lastReport.substring(0, 2500) + "\n\n... (full report in /sdcard/)" : lastReport;
        tv.setText(preview);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(11);
        tv.setTextColor(Color.BLACK);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        d.setView(sv);
        d.setPositiveButton("Close", null);
        d.setNegativeButton("Share", (dia, w) -> shareReport());
        d.show();
    }

    private void shareReport() {
        Intent si = new Intent(Intent.ACTION_SEND);
        si.setType("text/plain");
        si.putExtra(Intent.EXTRA_SUBJECT, "Port Doctor Report");
        si.putExtra(Intent.EXTRA_TEXT, lastReport);
        startActivity(Intent.createChooser(si, "Share Report"));
    }

    private void syncCommunityDB() {
        progressDialog = ProgressDialog.show(this, "", "Syncing community database...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                if (vendorDB == null) vendorDB = new VendorDB();
                return CloudSync.fetchCommunityDB(vendorDB);
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                CloudSync.SyncResult sr = (CloudSync.SyncResult) r;
                statusText.setText(sr.success ? "🟢  " + sr.message : "🔴  " + sr.message);
                Toast.makeText(MainActivity.this, sr.message, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void browseFixPacks() {
        progressDialog = ProgressDialog.show(this, "", "Fetching fix packs...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                fixPackServer = new FixPackServer();
                return fixPackServer.fetchAvailablePacks();
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                List packs = (List) r;
                if (packs.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No packs available yet", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActiv
