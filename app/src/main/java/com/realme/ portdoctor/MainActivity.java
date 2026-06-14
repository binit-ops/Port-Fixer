package com.realme.portdoctor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import java.io.BufferedReader;
import java.io.File;
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
    private List<PortScanner.Issue> detectedIssues;
    private List<FixEngine.Fix> fixes;
    private VendorDB vendorDB;
    private FixPackServer fixPackServer;
    private String lastReport;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.parseColor("#1A1A2E"));

        LinearLayout deviceBar = new LinearLayout(this);
        deviceBar.setOrientation(LinearLayout.HORIZONTAL);
        deviceBar.setPadding(0, 0, 0, 15);

        deviceLabel = new TextView(this);
        String currentDevice = getBuildProp("ro.product.model", "Unknown");
        String currentROM = getBuildProp("ro.build.display.id", "Unknown ROM");
        deviceLabel.setText("Device: " + currentDevice + "\nROM: " + currentROM);
        deviceLabel.setTextColor(Color.parseColor("#AAAAAA"));
        deviceLabel.setTextSize(11);
        deviceBar.addView(deviceLabel);

        Button changeDeviceBtn = new Button(this);
        changeDeviceBtn.setText("Set Device");
        changeDeviceBtn.setTextColor(Color.WHITE);
        changeDeviceBtn.setBackgroundColor(Color.parseColor("#424242"));
        changeDeviceBtn.setTextSize(10);
        changeDeviceBtn.setMinWidth(0);
        changeDeviceBtn.setMinimumWidth(120);
        changeDeviceBtn.setOnClickListener(v -> showDeviceSelector());
        deviceBar.addView(changeDeviceBtn);

        layout.addView(deviceBar);

        TextView title = new TextView(this);
        title.setText("Port Doctor");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#FF6F00"));
        title.setPadding(0, 0, 0, 5);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ROM Port Bug Detector & Fixer");
        subtitle.setTextColor(Color.parseColor("#AAAAAA"));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, 0, 0, 20);
        layout.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Ready to scan.");
        statusText.setTextColor(Color.parseColor("#4CAF50"));
        statusText.setPadding(0, 0, 0, 15);
        layout.addView(statusText);

        scanBtn = new Button(this);
        scanBtn.setText("Scan Device for Issues");
        scanBtn.setTextColor(Color.WHITE);
        scanBtn.setBackgroundColor(Color.parseColor("#E65100"));
        scanBtn.setPadding(20, 15, 20, 15);
        scanBtn.setOnClickListener(v -> runScan());
        layout.addView(scanBtn);

        TextView issuesLabel = new TextView(this);
        issuesLabel.setText("\nDetected Issues:");
        issuesLabel.setTextColor(Color.WHITE);
        issuesLabel.setTextSize(16);
        issuesLabel.setPadding(0, 20, 0, 10);
        layout.addView(issuesLabel);

        issueContainer = new LinearLayout(this);
        issueContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(issueContainer);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 20, 0, 0);

        fixAllBtn = new Button(this);
        fixAllBtn.setText("Generate Fixes");
        fixAllBtn.setTextColor(Color.WHITE);
        fixAllBtn.setBackgroundColor(Color.parseColor("#2E7D32"));
        fixAllBtn.setEnabled(false);
        fixAllBtn.setOnClickListener(v -> generateFixes());
        btnRow.addView(fixAllBtn);

        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(10, 1));
        btnRow.addView(spacer1);

        buildBtn = new Button(this);
        buildBtn.setText("Build Module");
        buildBtn.setTextColor(Color.WHITE);
        buildBtn.setBackgroundColor(Color.parseColor("#1565C0"));
        buildBtn.setEnabled(false);
        buildBtn.setOnClickListener(v -> buildModule());
        btnRow.addView(buildBtn);

        layout.addView(btnRow);

        TextView reportLabel = new TextView(this);
        reportLabel.setText("\nReports & Sharing");
        reportLabel.setTextColor(Color.parseColor("#FF6F00"));
        reportLabel.setTextSize(16);
        reportLabel.setPadding(0, 25, 0, 10);
        layout.addView(reportLabel);

        LinearLayout reportRow = new LinearLayout(this);
        reportRow.setOrientation(LinearLayout.HORIZONTAL);

        Button reportBtn = new Button(this);
        reportBtn.setText("Full Report");
        reportBtn.setTextColor(Color.WHITE);
        reportBtn.setBackgroundColor(Color.parseColor("#6A1B9A"));
        reportBtn.setOnClickListener(v -> generateFullReport());
        reportRow.addView(reportBtn);

        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        reportRow.addView(spacer2);

        Button shareBtn = new Button(this);
        shareBtn.setText("Share Report");
        shareBtn.setTextColor(Color.WHITE);
        shareBtn.setBackgroundColor(Color.parseColor("#00838F"));
        shareBtn.setOnClickListener(v -> shareReport());
        reportRow.addView(shareBtn);

        layout.addView(reportRow);

        TextView communityLabel = new TextView(this);
        communityLabel.setText("\nCommunity Database");
        communityLabel.setTextColor(Color.parseColor("#FF6F00"));
        communityLabel.setTextSize(16);
        communityLabel.setPadding(0, 25, 0, 10);
        layout.addView(communityLabel);

        Button syncBtn = new Button(this);
        syncBtn.setText("Sync Community Database");
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
        browsePacksBtn.setText("Browse Fix Packs");
        browsePacksBtn.setTextColor(Color.WHITE);
        browsePacksBtn.setBackgroundColor(Color.parseColor("#37474F"));
        browsePacksBtn.setOnClickListener(v -> browseFixPacks());
        fixpackRow.addView(browsePacksBtn);

        View spacer4 = new View(this);
        spacer4.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        fixpackRow.addView(spacer4);

        Button vendorCheckBtn = new Button(this);
        vendorCheckBtn.setText("Check Vendor");
        vendorCheckBtn.setTextColor(Color.WHITE);
        vendorCheckBtn.setBackgroundColor(Color.parseColor("#BF360C"));
        vendorCheckBtn.setOnClickListener(v -> checkVendorFiles());
        fixpackRow.addView(vendorCheckBtn);

        layout.addView(fixpackRow);

        logText = new TextView(this);
        logText.setText("\nScan log will appear here...");
        logText.setTextColor(Color.parseColor("#888888"));
        logText.setTextSize(11);
        logText.setPadding(0, 20, 0, 40);
        layout.addView(logText);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        setContentView(scrollView);

        SharedPreferences prefs = getSharedPreferences("portdoctor", MODE_PRIVATE);
        String savedDevice = prefs.getString("device", "");
        String savedDonor = prefs.getString("donor", "");
        if (!savedDevice.isEmpty()) {
            deviceLabel.setText("Device: " + savedDevice + 
                (savedDonor.isEmpty() ? "" : "\nDonor: " + savedDonor));
        }
    }

    private void runScan() {
        scanBtn.setEnabled(false);
        scanBtn.setText("Scanning...");
        statusText.setText("Scanning hardware...");
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
                scanBtn.setText("Re-Scan");
            }
        }.execute();
    }

    private void displayResults() {
        if (detectedIssues.isEmpty()) {
            statusText.setText("No issues detected!");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            logText.setText(scanner.getScanLog());
            return;
        }

        statusText.setText("Found " + detectedIssues.size() + " issue(s)");
        statusText.setTextColor(Color.parseColor("#FF9800"));

        checkBoxes = new CheckBox[detectedIssues.size()];

        for (int i = 0; i < detectedIssues.size(); i++) {
            PortScanner.Issue issue = detectedIssues.get(i);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(10, 8, 10, 8);

            CheckBox cb = new CheckBox(this);
            cb.setText("[" + issue.severity + "] " + issue.name);
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

        statusText.setText("Generated " + fixes.size() + " fix(es)");
        statusText.setTextColor(Color.parseColor("#4CAF50"));
        buildBtn.setEnabled(true);

        Toast.makeText(this, fixes.size() + " fixes ready!", Toast.LENGTH_LONG).show();
    }

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
                    statusText.setText("Module saved to /sdcard/");
                    statusText.setTextColor(Color.parseColor("#4CAF50"));
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Module Built")
                        .setMessage("Saved to /sdcard/PortDoctor_FixPack.zip\n\nFlash in Magisk and reboot.")
                        .setPositiveButton("OK", null)
                        .show();
                } else {
                    statusText.setText("Build failed!");
                    statusText.setTextColor(Color.parseColor("#F44336"));
                    Toast.makeText(MainActivity.this, "Build failed!", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void generateFullReport() {
        if (scanner == null) {
            Toast.makeText(this, "Run scan first!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Generating", 
            "Creating report...", true);

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
                    statusText.setText("Report saved!");
                    statusText.setTextColor(Color.parseColor("#4CAF50"));
                } catch (Exception e) {
                    statusText.setText("Could not save report");
                }
                showReportPreview();
            }
        }.execute();
    }

    private void showReportPreview() {
        if (lastReport == null) return;
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Scan Report");
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        String preview = lastReport;
        if (preview.length() > 3000) {
            preview = preview.substring(0, 3000) + "\n\n... (see file for full report)";
        }
        tv.setText(preview);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(11);
        sv.addView(tv);
        dialog.setView(sv);
        dialog.setPositiveButton("Close", null);
        dialog.setNegativeButton("Share", (d, w) -> shareReport());
        dialog.show();
    }

    private void shareReport() {
        if (lastReport == null) {
            Toast.makeText(this, "Generate report first!", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Port Doctor Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, lastReport);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void syncCommunityDB() {
        progressDialog = ProgressDialog.show(this, "Syncing", 
            "Downloading database...", true);
        
        new AsyncTask<Void, Void, CloudSync.SyncResult>() {
            @Override
            protected CloudSync.SyncResult doInBackground(Void... params) {
                if (vendorDB == null) vendorDB = new VendorDB();
                return CloudSync.fetchCommunityDB(vendorDB);
            }

            @Override
            protected void onPostExecute(CloudSync.SyncResult result) {
                progressDialog.dismiss();
                String msg = result.success ? "OK: " + result.message : "Error: " + result.message;
                statusText.setText(msg);
                statusText.setTextColor(result.success ? 
                    Color.parseColor("#4CAF50") : Color.parseColor("#FF9800"));
                if (vendorDB != null) vendorDB.saveLocalCache();
                Toast.makeText(MainActivity.this, result.message, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    private void browseFixPacks() {
        progressDialog = ProgressDialog.show(this, "Loading", 
            "Fetching fix packs...", true);
        
        new AsyncTask<Void, Void, List<FixPackServer.FixPack>>() {
            @Override
            protected List<FixPackServer.FixPack> doInBackground(Void... params) {
                fixPackServer = new FixPackServer();
                return fixPackServer.fetchAvailablePacks();
            }

            @Override
            protected void onPostExecute(List<FixPackServer.FixPack> packs) {
                progressDialog.dismiss();
                if (packs.isEmpty()) {
                    Toast.makeText(MainActivity.this, 
                        "No packs available yet.", Toast.LENGTH_LONG).show();
                    return;
                }
                
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Fix Packs (" + packs.size() + ")");
                LinearLayout list = new LinearLayout(MainActivity.this);
                list.setOrientation(LinearLayout.VERTICAL);
                list.setPadding(20, 10, 20, 10);
                
                for (FixPackServer.FixPack pack : packs) {
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText(pack.name + " (" + pack.downloads + " downloads)");
                    tv.setTextColor(Color.BLACK);
                    tv.setPadding(5, 10, 5, 10);
                    tv.setOnClickListener(v -> downloadPack(pack));
                    list.addView(tv);
                }
                
                ScrollView sv = new ScrollView(MainActivity.this);
                sv.addView(list);
                builder.setView(sv);
                builder.setPositiveButton("Close", null);
                builder.show();
            }
        }.execute();
    }

    private void downloadPack(FixPackServer.FixPack pack) {
        progressDialog = ProgressDialog.show(this, "Downloading", pack.name, true);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                File f = FixPackServer.downloadFixPack(pack.downloadUrl, 
                    "/sdcard/" + pack.name.replace(" ", "_") + ".zip");
                return f != null && f.exists();
            }
            @Override
            protected void onPostExecute(Boolean ok) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, 
                    ok ? "Downloaded to /sdcard/" : "Download failed!", 
     
