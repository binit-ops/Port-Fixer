package com.realme.portdoctor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private List<PortScanner.Issue> detectedIssues;
    private List<FixEngine.Fix> fixes;
    private VendorDB vendorDB;
    private FixPackServer fixPackServer;
    private String lastReport;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build UI
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.parseColor("#1A1A2E"));

        // ==================== DEVICE INFO BAR ====================
        LinearLayout deviceBar = new LinearLayout(this);
        deviceBar.setOrientation(LinearLayout.HORIZONTAL);
        deviceBar.setPadding(0, 0, 0, 15);

        deviceLabel = new TextView(this);
        String currentDevice = getBuildProp("ro.product.model", "Unknown");
        String currentROM = getBuildProp("ro.build.display.id", "Unknown ROM");
        deviceLabel.setText("📱 " + currentDevice + "\n📦 " + currentROM);
        deviceLabel.setTextColor(Color.parseColor("#AAAAAA"));
        deviceLabel.setTextSize(11);
        deviceBar.addView(deviceLabel);

        Button changeDeviceBtn = new Button(this);
        changeDeviceBtn.setText("🔄 Set Device");
        changeDeviceBtn.setTextColor(Color.WHITE);
        changeDeviceBtn.setBackgroundColor(Color.parseColor("#424242"));
        changeDeviceBtn.setTextSize(10);
        changeDeviceBtn.setMinWidth(0);
        changeDeviceBtn.setMinimumWidth(120);
        changeDeviceBtn.setOnClickListener(v -> showDeviceSelector());
        deviceBar.addView(changeDeviceBtn);

        layout.addView(deviceBar);

        // ==================== HEADER ====================
        TextView title = new TextView(this);
        title.setText("🩺 Port Doctor");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#FF6F00"));
        title.setPadding(0, 0, 0, 5);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Universal ROM Port Bug Detector & Fixer");
        subtitle.setTextColor(Color.parseColor("#AAAAAA"));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, 0, 0, 20);
        layout.addView(subtitle);

        // ==================== STATUS ====================
        statusText = new TextView(this);
        statusText.setText("Ready to scan. Tap below to start.");
        statusText.setTextColor(Color.parseColor("#4CAF50"));
        statusText.setPadding(0, 0, 0, 15);
        layout.addView(statusText);

        // ==================== SCAN BUTTON ====================
        scanBtn = new Button(this);
        scanBtn.setText("🔍 Scan Device for Issues");
        scanBtn.setTextColor(Color.WHITE);
        scanBtn.setBackgroundColor(Color.parseColor("#E65100"));
        scanBtn.setPadding(20, 15, 20, 15);
        scanBtn.setOnClickListener(v -> runScan());
        layout.addView(scanBtn);

        // ==================== ISSUES CONTAINER ====================
        TextView issuesLabel = new TextView(this);
        issuesLabel.setText("\n📋 Detected Issues:");
        issuesLabel.setTextColor(Color.WHITE);
        issuesLabel.setTextSize(16);
        issuesLabel.setPadding(0, 20, 0, 10);
        layout.addView(issuesLabel);

        issueContainer = new LinearLayout(this);
        issueContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(issueContainer);

        // ==================== ACTION BUTTONS ====================
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 20, 0, 0);

        fixAllBtn = new Button(this);
        fixAllBtn.setText("⚡ Generate Fixes");
        fixAllBtn.setTextColor(Color.WHITE);
        fixAllBtn.setBackgroundColor(Color.parseColor("#2E7D32"));
        fixAllBtn.setEnabled(false);
        fixAllBtn.setOnClickListener(v -> generateFixes());
        btnRow.addView(fixAllBtn);

        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(10, 1));
        btnRow.addView(spacer1);

        buildBtn = new Button(this);
        buildBtn.setText("📦 Build Module");
        buildBtn.setTextColor(Color.WHITE);
        buildBtn.setBackgroundColor(Color.parseColor("#1565C0"));
        buildBtn.setEnabled(false);
        buildBtn.setOnClickListener(v -> buildModule());
        btnRow.addView(buildBtn);

        layout.addView(btnRow);

        // ==================== REPORT SECTION ====================
        TextView reportLabel = new TextView(this);
        reportLabel.setText("\n📊 Reports & Sharing");
        reportLabel.setTextColor(Color.parseColor("#FF6F00"));
        reportLabel.setTextSize(16);
        reportLabel.setPadding(0, 25, 0, 10);
        layout.addView(reportLabel);

        LinearLayout reportRow = new LinearLayout(this);
        reportRow.setOrientation(LinearLayout.HORIZONTAL);

        Button reportBtn = new Button(this);
        reportBtn.setText("📊 Full Report");
        reportBtn.setTextColor(Color.WHITE);
        reportBtn.setBackgroundColor(Color.parseColor("#6A1B9A"));
        reportBtn.setOnClickListener(v -> generateFullReport());
        reportRow.addView(reportBtn);

        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        reportRow.addView(spacer2);

        Button shareBtn = new Button(this);
        shareBtn.setText("📤 Share Report");
        shareBtn.setTextColor(Color.WHITE);
        shareBtn.setBackgroundColor(Color.parseColor("#00838F"));
        shareBtn.setOnClickListener(v -> shareReport());
        reportRow.addView(shareBtn);

        layout.addView(reportRow);

        // ==================== COMMUNITY SECTION ====================
        TextView communityLabel = new TextView(this);
        communityLabel.setText("\n🌐 Community Database");
        communityLabel.setTextColor(Color.parseColor("#FF6F00"));
        communityLabel.setTextSize(16);
        communityLabel.setPadding(0, 25, 0, 10);
        layout.addView(communityLabel);

        Button syncBtn = new Button(this);
        syncBtn.setText("🔄 Sync Community Database");
        syncBtn.setTextColor(Color.WHITE);
        syncBtn.setBackgroundColor(Color.parseColor("#00695C"));
        syncBtn.setOnClickListener(v -> syncCommunityDB());
        layout.addView(syncBtn);

        View space3 = new View(this);
        space3.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 8));
        layout.addView(space3);

        LinearLayout fixpackRow = new LinearLayout(this);
        fixpackRow.setOrientation(LinearLayout.HORIZONTAL);

        Button browsePacksBtn = new Button(this);
        browsePacksBtn.setText("📦 Browse Fix Packs");
        browsePacksBtn.setTextColor(Color.WHITE);
        browsePacksBtn.setBackgroundColor(Color.parseColor("#37474F"));
        browsePacksBtn.setOnClickListener(v -> browseFixPacks());
        fixpackRow.addView(browsePacksBtn);

        View spacer4 = new View(this);
        spacer4.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        fixpackRow.addView(spacer4);

        Button vendorCheckBtn = new Button(this);
        vendorCheckBtn.setText("🔍 Check Vendor Files");
        vendorCheckBtn.setTextColor(Color.WHITE);
        vendorCheckBtn.setBackgroundColor(Color.parseColor("#BF360C"));
        vendorCheckBtn.setOnClickListener(v -> checkVendorFiles());
        fixpackRow.addView(vendorCheckBtn);

        layout.addView(fixpackRow);

        // ==================== LOG ====================
        logText = new TextView(this);
        logText.setText("\n📄 Scan log will appear here...");
        logText.setTextColor(Color.parseColor("#888888"));
        logText.setTextSize(11);
        logText.setPadding(0, 20, 0, 40);
        layout.addView(logText);

        // ==================== SCROLL VIEW ====================
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        setContentView(scrollView);

        // Load saved preferences
        SharedPreferences prefs = getSharedPreferences("portdoctor", MODE_PRIVATE);
        String savedDevice = prefs.getString("device", "");
        String savedDonor = prefs.getString("donor", "");
        if (!savedDevice.isEmpty()) {
            deviceLabel.setText("📱 " + savedDevice + 
                (savedDonor.isEmpty() ? "" : "\n📦 Donor: " + savedDonor));
        }
    }

    // ==================== CORE: SCAN ====================
    private void runScan() {
        scanBtn.setEnabled(false);
        scanBtn.setText("Scanning...");
        statusText.setText("🔍 Scanning hardware...");
        statusText.setTextColor(Color.parseColor("#FF9800"));
        issueContainer.removeAllViews();

        new AsyncTask<Void, Void, List<PortScanner.Issue>>() {
            @Override
            protected List<PortScanner.Issue> doInBackground(Void... params) {
                SharedPreferences prefs = getSharedPreferences("portdoctor", MODE_PRIVATE);
                String device = prefs.getString("device", getBuildProp("ro.product.model", "Unknown"));
                String donor = prefs.getString("donor", "");
                scanner = new PortScanner(device, donor);
                return scanner.scanAll();
            }

            @Override
            protected void onPostExecute(List<PortScanner.Issue> issues) {
                detectedIssues = issues;
                displayResults();
                scanBtn.setEnabled(true);
                scanBtn.setText("🔄 Re-Scan");
            }
        }.execute();
    }

    private void displayResults() {
        if (detectedIssues.isEmpty()) {
            statusText.setText("✅ No issues detected! Your ROM is healthy.");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            logText.setText(scanner.getScanLog());
            return;
        }

        statusText.setText("⚠️ Found " + detectedIssues.size() + " issue(s)");
        statusText.setTextColor(Color.parseColor("#FF9800"));

        checkBoxes = new CheckBox[detectedIssues.size()];

        for (int i = 0; i < detectedIssues.size(); i++) {
            PortScanner.Issue issue = detectedIssues.get(i);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(10, 8, 10, 8);

            CheckBox cb = new CheckBox(this);
            String icon = issue.severity.equals("HIGH") ? "🔴" : 
                          issue.severity.equals("MEDIUM") ? "🟡" : "🟢";
            cb.setText(icon + " [" + issue.severity + "] " + issue.name);
            cb.setTextColor(Color.WHITE);
            cb.setChecked(true);
            checkBoxes[i] = cb;

            TextView desc = new TextView(this);
            desc.setText("   " + issue.description);
            desc.setTextColor(Color.parseColor("#AAAAAA"));
            desc.setTextSize(12);

            item.addView(cb);
            item.addView(desc);
            issueContainer.addView(item);
        }

        fixAllBtn.setEnabled(true);
        logText.setText(scanner.getScanLog());
    }

    // ==================== CORE: GENERATE FIXES ====================
    private void generateFixes() {
        List<PortScanner.Issue> selectedIssues = new ArrayList<>();
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isChecked()) {
                selectedIssues.add(detectedIssues.get(i));
            }
        }

        if (selectedIssues.isEmpty()) {
            Toast.makeText(this, "Select at least one issue!", Toast.LENGTH_SHORT).show();
            return;
        }

        scanner.getDetectedIssues().clear();
        scanner.getDetectedIssues().addAll(selectedIssues);

        fixEngine = new FixEngine(scanner);
        fixes = fixEngine.generateFixes();

        statusText.setText("✅ Generated " + fixes.size() + " fix(es)");
        statusText.setTextColor(Color.parseColor("#4CAF50"));
        buildBtn.setEnabled(true);

        Toast.makeText(this, fixes.size() + " fixes ready! Tap Build Module.", 
            Toast.LENGTH_LONG).show();
    }

    // ==================== CORE: BUILD MODULE ====================
    private void buildModule() {
        if (fixes == null || fixes.isEmpty()) {
            Toast.makeText(this, "Generate fixes first!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Building", 
            "Creating Magisk module...", true);

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    ModuleBuilder.buildModule(fixes, "/sdcard/PortDoctor_FixPack.zip");
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                progressDialog.dismiss();
                if (success) {
                    statusText.setText("✅ Module: /sdcard/PortDoctor_FixPack.zip");
                    statusText.setTextColor(Color.parseColor("#4CAF50"));
                    
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Module Built Successfully")
                        .setMessage("📦 Saved to: /sdcard/PortDoctor_FixPack.zip\n\n" +
                            "Flash this file in Magisk to apply all fixes.\n" +
                            "Reboot after flashing.\n\n" +
                            "Share this module with other users!")
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Share Module", (d, w) -> {
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setType("application/zip");
                            share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(
                                new File("/sdcard/PortDoctor_FixPack.zip")));
                            startActivity(Intent.createChooser(share, "Share Fix Pack"));
                        })
                        .show();
                } else {
                    statusText.setText("❌ Build failed!");
                    statusText.setTextColor(Color.parseColor("#F44336"));
                    Toast.makeText(MainActivity.this, 
                        "Build failed! Check storage permissions.", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    // ==================== REPORT: GENERATE ====================
    private void generateFullReport() {
        if (scanner == null) {
            Toast.makeText(this, "Run scan first!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Generating", 
            "Creating detailed report...", true);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return ReportGenerator.generateReport(scanner, fixes);
            }

            @Override
            protected void onPostExecute(String report) {
                progressDialog.dismiss();
                lastReport = report;
                
                try {
                    ReportGenerator.saveReport(report, "/sdcard/PortDoctor_Report.txt");
                    statusText.setText("✅ Report: /sdcard/PortDoctor_Report.txt");
                    statusText.setTextColor(Color.parseColor("#4CAF50"));
                } catch (Exception e) {
                    statusText.setText("⚠️ Could not save report");
                }

                showReportPreview();
            }
        }.execute();
    }

    private void showReportPreview() {
        if (lastReport == null) return;
        
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("📊 Scan Report");
        
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        String preview = lastReport.length() > 3000 ? 
            lastReport.substring(0, 3000) + "\n\n... (full report in file)" : lastReport;
        tv.setText(preview);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(11);
        tv.setTextColor(Color.BLACK);
        sv.addView(tv);
        
        dialog.setView(sv);
        dialog.setPositiveButton("Close", null);
        dialog.setNegativeButton("Share", (d, w) -> shareReport());
        dialog.show();
    }

    // ==================== REPORT: SHARE ====================
    private void shareReport() {
        if (lastReport == null) {
            Toast.makeText(this, "Generate report first!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Port Doctor Report - " + 
            getBuildProp("ro.product.model", "Device"));
        shareIntent.putExtra(Intent.EXTRA_TEXT, lastReport);
        startActivity(Intent.createChooser(shareIntent, "Share Report via"));
    }

    // ==================== COMMUNITY: SYNC DATABASE ====================
    private void syncCommunityDB() {
        progressDialog = ProgressDialog.show(this, "Syncing", 
            "Downloading community database...", true);
        
        new AsyncTask<Void, Void, CloudSync.SyncResult>() {
            @Override
            protected CloudSync.SyncResult doInBackground(Void... params) {
                if (vendorDB == null) vendorDB = new VendorDB();
                return CloudSync.fetchCommunityDB(vendorDB);
            }

            @Override
            protected void onPostExecute(CloudSync.SyncResult result) {
                progressDialog.dismiss();
                if (result.success) {
                    statusText.setText("✅ " + result.message);
                    statusText.setTextColor(Color.parseColor("#4CAF50"));
                    if (vendorDB != null) vendorDB.saveLocalCache();
                } else {
                    statusText.setText("⚠️ " + result.message);
                    statusText.setTextColor(Color.parseColor("#FF9800"));
                }
                Toast.makeText(MainActivity.this, result.message, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    // ==================== COMMUNITY: BROWSE FIX PACKS ====================
    private void browseFixPacks() {
        progressDialog = ProgressDialog.show(this, "Loading", 
            "Fetching available fix packs...", true);
        
        new AsyncTask<Void, Void, List<FixPackServer.FixPack>>() {
            @Override
            protected List<FixPackServer.FixPack> doInBackground(Void... params) {
                fixPackServer = new FixPackServer();
                retu
