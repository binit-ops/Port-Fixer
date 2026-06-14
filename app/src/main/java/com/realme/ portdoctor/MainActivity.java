package com.realme.portdoctor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
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
    private ThemeManager theme;
    private Vibrator vibrator;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = new ThemeManager(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        buildUI();
    }

    private void buildUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 40, 20, 40);
        layout.setBackground(theme.createBackground());

        // Header
        LinearLayout headerCard = glassCard(20, 25);
        headerCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView iconView = new TextView(this);
        iconView.setText("P");
        iconView.setTextSize(40);
        iconView.setTextColor(Color.parseColor("#FF6B6B"));
        iconView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleRow.addView(iconView);

        LinearLayout titleTextCol = new LinearLayout(this);
        titleTextCol.setOrientation(LinearLayout.VERTICAL);
        titleTextCol.setPadding(15, 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("Port Doctor v2.0");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor(theme.getTextPrimary()));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTextCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ROM Port Diagnostic Suite");
        subtitle.setTextColor(Color.parseColor(theme.getTextSecondary()));
        subtitle.setTextSize(13);
        titleTextCol.addView(subtitle);

        titleRow.addView(titleTextCol);
        headerCard.addView(titleRow);

        LinearLayout deviceChip = glassCard(8, 12);
        deviceChip.setPadding(15, 10, 15, 10);

        deviceLabel = new TextView(this);
        String cd = getprop("ro.product.model", "Unknown");
        deviceLabel.setText("Device: " + cd);
        deviceLabel.setTextColor(Color.parseColor("#FFE66D"));
        deviceLabel.setTextSize(12);
        deviceChip.addView(deviceLabel);

        Button changeDeviceBtn = smallBtn("Switch", "#7C4DFF", 0);
        changeDeviceBtn.setOnClickListener(v -> showDeviceSelector());
        deviceChip.addView(changeDeviceBtn);

        Button themeBtn = smallBtn(theme.isDark() ? "Light" : "Dark", "#FFA502", 0);
        themeBtn.setOnClickListener(v -> {
            theme.toggle(this);
            vibrator.vibrate(20);
            recreate();
        });
        deviceChip.addView(themeBtn);

        headerCard.addView(deviceChip);
        layout.addView(headerCard);

        // Status
        LinearLayout statusCard = glassCard(15, 15);
        statusCard.setPadding(20, 12, 20, 12);
        statusText = new TextView(this);
        statusText.setText("Ready to diagnose your ROM");
        statusText.setTextColor(Color.parseColor(theme.getTextPrimary()));
        statusText.setTextSize(14);
        statusCard.addView(statusText);
        layout.addView(statusCard);

        // Scan
        scanBtn = gradBtn("Scan Device for Issues", "#FF6B6B", "#4ECDC4");
        scanBtn.setOnClickListener(v -> { vibrator.vibrate(15); runScan(); });
        layout.addView(scanBtn);

        // Issues
        TextView issuesHeader = new TextView(this);
        issuesHeader.setText("\nDETECTED ISSUES");
        issuesHeader.setTextColor(Color.parseColor(theme.getTextSecondary()));
        issuesHeader.setTextSize(11);
        issuesHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        issuesHeader.setPadding(10, 20, 0, 10);
        layout.addView(issuesHeader);

        issueContainer = new LinearLayout(this);
        issueContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(issueContainer);

        // Actions
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 15, 0, 0);

        fixAllBtn = smallBtn("Generate Fixes", "#2ED573", 0);
        fixAllBtn.setEnabled(false);
        fixAllBtn.setAlpha(0.5f);
        fixAllBtn.setOnClickListener(v -> { vibrator.vibrate(15); generateFixes(); });
        actionRow.addView(fixAllBtn);

        View gap1 = new View(this);
        gap1.setLayoutParams(new LinearLayout.LayoutParams(10, 1));
        actionRow.addView(gap1);

        buildBtn = smallBtn("Build Module", "#1E90FF", 0);
        buildBtn.setEnabled(false);
        buildBtn.setAlpha(0.5f);
        buildBtn.setOnClickListener(v -> { vibrator.vibrate(15); buildModule(); });
        actionRow.addView(buildBtn);
        layout.addView(actionRow);

        // Reports
        TextView reportsHeader = new TextView(this);
        reportsHeader.setText("\nREPORTS & TOOLS");
        reportsHeader.setTextColor(Color.parseColor(theme.getTextSecondary()));
        reportsHeader.setTextSize(11);
        reportsHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        reportsHeader.setPadding(10, 20, 0, 10);
        layout.addView(reportsHeader);

        LinearLayout toolRow1 = new LinearLayout(this);
        toolRow1.setOrientation(LinearLayout.HORIZONTAL);
        Button reportBtn = smallBtn("Report", "#7C4DFF", 0);
        reportBtn.setOnClickListener(v -> generateFullReport());
        toolRow1.addView(reportBtn);
        View g2 = new View(this);
        g2.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        toolRow1.addView(g2);
        Button shareBtn = smallBtn("Share", "#00CED1", 0);
        shareBtn.setOnClickListener(v -> shareReport());
        toolRow1.addView(shareBtn);
        layout.addView(toolRow1);

        LinearLayout toolRow2 = new LinearLayout(this);
        toolRow2.setOrientation(LinearLayout.HORIZONTAL);
        toolRow2.setPadding(0, 8, 0, 0);
        Button backupBtn = smallBtn("Backup", "#FF6B6B", 0);
        backupBtn.setOnClickListener(v -> createBackup());
        toolRow2.addView(backupBtn);
        View g3 = new View(this);
        g3.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        toolRow2.addView(g3);
        Button historyBtn = smallBtn("History", "#FFE66D", 0);
        historyBtn.setOnClickListener(v -> showHistory());
        toolRow2.addView(historyBtn);
        layout.addView(toolRow2);

        // Community
        TextView communityHeader = new TextView(this);
        communityHeader.setText("\nCOMMUNITY TOOLS");
        communityHeader.setTextColor(Color.parseColor(theme.getTextSecondary()));
        communityHeader.setTextSize(11);
        communityHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        communityHeader.setPadding(10, 20, 0, 10);
        layout.addView(communityHeader);

        Button syncBtn = gradBtn("Sync Community Database", "#7C4DFF", "#1E90FF");
        syncBtn.setOnClickListener(v -> syncCommunityDB());
        layout.addView(syncBtn);

        LinearLayout communityRow = new LinearLayout(this);
        communityRow.setOrientation(LinearLayout.HORIZONTAL);
        communityRow.setPadding(0, 8, 0, 0);
        Button browseBtn = smallBtn("Fix Packs", "#FFA502", 0);
        browseBtn.setOnClickListener(v -> browseFixPacks());
        communityRow.addView(browseBtn);
        View g4 = new View(this);
        g4.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        communityRow.addView(g4);
        Button vendorBtn = smallBtn("Vendor", "#FF4757", 0);
        vendorBtn.setOnClickListener(v -> checkVendorFiles());
        communityRow.addView(vendorBtn);
        layout.addView(communityRow);

        // Log
        LinearLayout logCard = glassCard(12, 15);
        logCard.setPadding(15, 12, 15, 12);
        logText = new TextView(this);
        logText.setText("Scan log will appear here...");
        logText.setTextColor(Color.parseColor(theme.getLogText()));
        logText.setTextSize(11);
        logCard.addView(logText);
        layout.addView(logCard);

        scrollView.addView(layout);
        setContentView(scrollView);

        SharedPreferences prefs = getSharedPreferences("portdoctor", MODE_PRIVATE);
        String sd = prefs.getString("device", "");
        if (!sd.isEmpty()) deviceLabel.setText("Device: " + sd);
    }

    // ==================== UI HELPERS (Themed) ====================
    private LinearLayout glassCard(int radius, int marginTop) {
        LinearLayout card = new LinearLayout(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(radius * 2);
        gd.setColor(Color.parseColor(theme.getCardBg()));
        gd.setStroke(1, Color.parseColor(theme.getCardBorder()));
        card.setBackground(gd);
        card.setPadding(20, 15, 20, 15);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, marginTop, 0, 0);
        card.setLayoutParams(params);
        AnimationHelper.scaleIn(card, 600);
        return card;
    }

    private Button gradBtn(String text, String c1, String c2) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor(c1), Color.parseColor(c2)}
        );
        gd.setCornerRadius(50);
        btn.setBackground(gd);
        btn.setPadding(30, 18, 30, 18);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 12, 0, 0);
        btn.setLayoutParams(params);
        return btn;
    }

    private Button smallBtn(String text, String color, int mt) {
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, mt, 0, 0);
        btn.setLayoutParams(params);
        return btn;
    }

    // ==================== CORE ====================
    private void runScan() {
        scanBtn.setEnabled(false);
        scanBtn.setText("Scanning...");
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
                scanBtn.setText("Scan Device for Issues");
                AnimationHelper.pulse(scanBtn);
            }
        }.execute();
    }

    private void displayResults() {
        if (detectedIssues.isEmpty()) {
            statusText.setText("No issues detected! Your ROM is healthy.");
            logText.setText(scanner.getScanLog());
            return;
        }
        statusText.setText("Found " + detectedIssues.size() + " issue(s)");
        checkBoxes = new CheckBox[detectedIssues.size()];
        for (int i = 0; i < detectedIssues.size(); i++) {
            PortScanner.Issue issue = (PortScanner.Issue) detectedIssues.get(i);
            LinearLayout ic = glassCard(10, 6);
            ic.setOrientation(LinearLayout.VERTICAL);
            ic.setPadding(15, 10, 15, 10);
            CheckBox cb = new CheckBox(this);
            cb.setText("[" + issue.severity + "] " + issue.name);
            cb.setTextColor(Color.parseColor(theme.getTextPrimary()));
            cb.setChecked(true);
            checkBoxes[i] = cb;
            TextView desc = new TextView(this);
            desc.setText(issue.description);
            desc.setTextColor(Color.parseColor(theme.getTextSecondary()));
            desc.setTextSize(11);
            ic.addView(cb);
            ic.addView(desc);
            issueContainer.addView(ic);
            AnimationHelper.slideUp(ic, 400, i * 50);
        }
        fixAllBtn.setEnabled(true);
        fixAllBtn.setAlpha(1.0f);
        AnimationHelper.pulse(fixAllBtn);
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
        statusText.setText("Generated " + fixes.size() + " fix(es)");
        buildBtn.setEnabled(true);
        buildBtn.setAlpha(1.0f);
        AnimationHelper.pulse(buildBtn);
    }

    private void buildModule() {
        if (fixes == null || fixes.isEmpty()) {
            AnimationHelper.shake(buildBtn);
            Toast.makeText(this, "Generate fixes first!", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = ProgressDialog.show(this, "", "Building module...", true);
        final File outDir = getExternalFilesDir(null);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                try {
                    File zip = ModuleBuilder.buildModule(fixes, outDir);
                    return new Object[]{true, zip.getAbsolutePath()};
                } catch (Exception e) {
                    return new Object[]{false, e.getMessage()};
                }
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                Object[] result = (Object[]) r;
                if ((Boolean) result[0]) {
                    statusText.setText("Module saved!");
                    String path = (String) result[1];
                    // Save history
                    StringBuilder fixNames = new StringBuilder();
                    for (Object f : fixes) fixNames.append(((FixEngine.Fix)f).name).append(", ");
                    HistoryManager.saveEntry(MainActivity.this, new HistoryManager.HistoryEntry(
                        new Date().toString(), getprop("ro.product.model", "?"), 
                        getSharedPreferences("portdoctor",MODE_PRIVATE).getString("donor",""), 
                        fixes.size(), fixNames.toString()));
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Module Built")
                        .setMessage("Saved to:\n" + path + "\n\nFlash in Magisk.")
                        .setPositiveButton("OK", null).show();
                } else {
                    statusText.setText("Build failed!");
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Build Failed")
                        .setMessage("Error: " + result[1])
                        .setPositiveButton("OK", null).show();
                }
            }
        }.execute();
    }

    // ==================== NEW FEATURES ====================
    private void createBackup() {
        progressDialog = ProgressDialog.show(this, "", "Creating backup...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                try {
                    return BackupManager.backupVendorFiles(new File(getExternalFilesDir(null), "backups"));
                } catch (Exception e) { return e.getMessage(); }
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                if (r instanceof List) {
                    List backups = (List) r;
                    Toast.makeText(MainActivity.this, backups.size() + " files backed up!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Backup failed: " + r, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void showHistory() {
        List<HistoryManager.HistoryEntry> history = HistoryManager.getHistory(this);
        if (history.isEmpty()) {
            Toast.makeText(this, "No history yet", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle("Fix History");
        StringBuilder sb = new StringBuilder();
        for (HistoryManager.HistoryEntry e : history) {
            sb.append(e.toString()).append("\n\n");
        }
        d.setMessage(sb.toString());
        d.setPositiveButton("Clear", (dia, w) -> { HistoryManager.clearHistory(this); Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show(); });
        d.setNegativeButton("Close", null);
        d.show();
    }

    // ==================== EXISTING METHODS ====================
    private void generateFullReport() {
        progressDialog = ProgressDialog.show(this, "", "Creating report...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { return ReportGenerator.generateReport(scanner, fixes); }
            protected void onPostExecute(Object r) { progressDialog.dismiss(); lastReport = (String) r; showReportPreview(); }
        }.execute();
    }

    private void showReportPreview() {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle("Scan Report");
        TextView tv = new TextView(this);
        tv.setText(lastReport.length() > 2500 ? lastReport.substring(0, 2500) + "..." : lastReport);
        tv.setPaddi
