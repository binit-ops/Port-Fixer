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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 40, 20, 40);

        GradientDrawable bgDrawable = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#0F0C29"), Color.parseColor("#302B63"), Color.parseColor("#24243E")}
        );
        layout.setBackground(bgDrawable);

        // Header Card
        LinearLayout headerCard = glassCard(20, 25);
        headerCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(View.Gravity.CENTER_VERTICAL);

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
        title.setText("Port Doctor");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTextCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ROM Port Diagnostic Suite");
        subtitle.setTextColor(Color.parseColor("#B0B0D0"));
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
        headerCard.addView(deviceChip);
        layout.addView(headerCard);

        // Status Card
        LinearLayout statusCard = glassCard(15, 15);
        statusCard.setPadding(20, 12, 20, 12);
        statusText = new TextView(this);
        statusText.setText("Ready to diagnose your ROM");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(14);
        statusCard.addView(statusText);
        layout.addView(statusCard);

        // Scan Button
        scanBtn = gradBtn("Scan Device for Issues", "#FF6B6B", "#4ECDC4");
        scanBtn.setOnClickListener(v -> runScan());
        layout.addView(scanBtn);

        // Issues Section
        TextView issuesHeader = new TextView(this);
        issuesHeader.setText("\nDETECTED ISSUES");
        issuesHeader.setTextColor(Color.parseColor("#B0B0D0"));
        issuesHeader.setTextSize(11);
        issuesHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        issuesHeader.setPadding(10, 20, 0, 10);
        layout.addView(issuesHeader);

        issueContainer = new LinearLayout(this);
        issueContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(issueContainer);

        // Action Buttons
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 15, 0, 0);

        fixAllBtn = smallBtn("Generate Fixes", "#2ED573", 0);
        fixAllBtn.setEnabled(false);
        fixAllBtn.setAlpha(0.5f);
        fixAllBtn.setOnClickListener(v -> generateFixes());
        actionRow.addView(fixAllBtn);

        View gap1 = new View(this);
        gap1.setLayoutParams(new LinearLayout.LayoutParams(10, 1));
        actionRow.addView(gap1);

        buildBtn = smallBtn("Build Module", "#1E90FF", 0);
        buildBtn.setEnabled(false);
        buildBtn.setAlpha(0.5f);
        buildBtn.setOnClickListener(v -> buildModule());
        actionRow.addView(buildBtn);
        layout.addView(actionRow);

        // Reports Section
        TextView reportsHeader = new TextView(this);
        reportsHeader.setText("\nREPORTS & SHARING");
        reportsHeader.setTextColor(Color.parseColor("#B0B0D0"));
        reportsHeader.setTextSize(11);
        reportsHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        reportsHeader.setPadding(10, 20, 0, 10);
        layout.addView(reportsHeader);

        LinearLayout reportRow = new LinearLayout(this);
        reportRow.setOrientation(LinearLayout.HORIZONTAL);

        Button reportBtn = smallBtn("Full Report", "#7C4DFF", 0);
        reportBtn.setOnClickListener(v -> generateFullReport());
        reportRow.addView(reportBtn);

        View gap2 = new View(this);
        gap2.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        reportRow.addView(gap2);

        Button shareBtn = smallBtn("Share", "#00CED1", 0);
        shareBtn.setOnClickListener(v -> shareReport());
        reportRow.addView(shareBtn);
        layout.addView(reportRow);

        // Community Section
        TextView communityHeader = new TextView(this);
        communityHeader.setText("\nCOMMUNITY TOOLS");
        communityHeader.setTextColor(Color.parseColor("#B0B0D0"));
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

        View gap3 = new View(this);
        gap3.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        communityRow.addView(gap3);

        Button vendorBtn = smallBtn("Vendor", "#FF4757", 0);
        vendorBtn.setOnClickListener(v -> checkVendorFiles());
        communityRow.addView(vendorBtn);
        layout.addView(communityRow);

        // Log Section
        LinearLayout logCard = glassCard(12, 15);
        logCard.setPadding(15, 12, 15, 12);
        logText = new TextView(this);
        logText.setText("Scan log will appear here...");
        logText.setTextColor(Color.parseColor("#8888AA"));
        logText.setTextSize(11);
        logCard.addView(logText);
        layout.addView(logCard);

        scrollView.addView(layout);
        setContentView(scrollView);

        SharedPreferences prefs = getSharedPreferences("portdoctor", MODE_PRIVATE);
        String sd = prefs.getString("device", "");
        if (!sd.isEmpty()) deviceLabel.setText("Device: " + sd);
    }

    // UI Helpers
    private LinearLayout glassCard(int radius, int marginTop) {
        LinearLayout card = new LinearLayout(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(radius * 2);
        gd.setColor(Color.parseColor("#22FFFFFF"));
        gd.setStroke(1, Color.parseColor("#33FFFFFF"));
        card.setBackground(gd);
        card.setPadding(20, 15, 20, 15);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, marginTop, 0, 0);
        card.setLayoutParams(params);
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

    // Core methods
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
            }
        }.execute();
    }

    private void displayResults() {
        if (detectedIssues.isEmpty()) {
            statusText.setText("No issues detected!");
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
            cb.setTextColor(Color.WHITE);
            cb.setChecked(true);
            checkBoxes[i] = cb;
            TextView desc = new TextView(this);
            desc.setText(issue.description);
            desc.setTextColor(Color.parseColor("#B0B0D0"));
            desc.setTextSize(11);
            ic.addView(cb);
            ic.addView(desc);
            issueContainer.addView(ic);
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
        statusText.setText("Generated " + fixes.size() + " fix(es)");
        buildBtn.setEnabled(true);
        buildBtn.setAlpha(1.0f);
    }

    private void buildModule() {
        progressDialog = ProgressDialog.show(this, "", "Building...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                try { ModuleBuilder.buildModule(fixes, "/sdcard/PortDoctor_FixPack.zip"); return true; }
                catch (Exception e) { return false; }
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                if ((Boolean) r) {
                    statusText.setText("Module saved!");
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Module Built")
                        .setMessage("Flash in Magisk to apply fixes.")
                        .setPositiveButton("OK", null).show();
                } else { statusText.setText("Build failed!"); }
            }
        }.execute();
    }

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
        tv.setPadding(20, 20, 20, 20); tv.setTextSize(11);
        ScrollView sv = new ScrollView(this); sv.addView(tv);
        d.setView(sv); d.setPositiveButton("Close", null);
        d.setNegativeButton("Share", (dia, w) -> shareReport()); d.show();
    }

    private void shareReport() {
        Intent si = new Intent(Intent.ACTION_SEND); si.setType("text/plain");
        si.putExtra(Intent.EXTRA_TEXT, lastReport); startActivity(Intent.createChooser(si, "Share"));
    }

    private void syncCommunityDB() {
        progressDialog = ProgressDialog.show(this, "", "Syncing...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { if (vendorDB == null) vendorDB = new VendorDB(); return CloudSync.fetchCommunityDB(vendorDB); }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss(); CloudSync.SyncResult sr = (CloudSync.SyncResult) r;
                Toast.makeText(MainActivity.this, sr.message, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void browseFixPacks() {
        progressDialog = ProgressDialog.show(this, "", "Fetching...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { fixPackServer = new FixPackServer(); return fixPackServer.fetchAvailablePacks(); }
            protected void onPostExecute(Object r) { progressDialog.dismiss(); Toast.makeText(MainActivity.this, ((List)r).size() + " packs", Toast.LENGTH_SHORT).show(); }
        }.execute();
    }

    private void checkVendorFiles() {
        progressDialog = ProgressDialog.show(this, "", "Scanning...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { if (vendorDB == null) vendorDB = new VendorDB(); vendorDB.scanVendor(); return vendorDB.getVendorReport(); }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this);
                d.setTitle("Vendor Analysis"); TextView tv = new TextView(MainActivity.this);
                tv.setText((String) r); tv.setPadding(20, 20, 20, 20); d.setView(tv);
                d.setPositiveButton("Close", null); d.show();
            }
        }.execute();
    }

    private void showDeviceSelector() {
        AlertDialog.Builder b = new AlertDialog.Builder(this); b.setTitle("Set Device Info");
        LinearLayout f = new LinearLayout(this); f.setOrientation(LinearLayout.VERTICAL); f.setPadding(30, 20, 30, 20);
        EditText di = new EditText(this); di.setHint("Your device");
        SharedPreferences pr = getSharedPreferences("portdoctor", MODE_PRIVATE);
        di.setText(pr.getString("device", getprop("ro.product.model", ""))); f.addView(di);
        EditText dn = new EditText(this); dn.setHint("Donor device");
        dn.setText(pr.getString("donor", "")); f.addView(dn);
        b.setView(f);
        b.setPositiveButton("Save", (d, w) -> {
            pr.edit().putString("device", di.getText().toString().trim()).putString("donor", dn.getText().toString().trim()).commit();
            deviceLabel.setText("Device: " + di.getText().toString().trim());
        });
        b.setNegativeButton("Cancel", null); b.show();
    }

    private String getprop(String key, String def) {
        try {
            Process p = Runtime.getRuntime().exec("getprop " + key);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String v = br.readLine(); br.close(); return v != null ? v.trim() : def;
        } catch (Exception e) { return def; }
    }
}
