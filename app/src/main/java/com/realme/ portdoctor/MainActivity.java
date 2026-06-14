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
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.parseColor("#1A1A2E"));
        LinearLayout deviceBar = new LinearLayout(this);
        deviceBar.setOrientation(LinearLayout.HORIZONTAL);
        deviceBar.setPadding(0, 0, 0, 15);
        deviceLabel = new TextView(this);
        String cd = getprop("ro.product.model", "Unknown");
        String cr = getprop("ro.build.display.id", "Unknown ROM");
        deviceLabel.setText("Device: " + cd + "\nROM: " + cr);
        deviceLabel.setTextColor(Color.parseColor("#AAAAAA"));
        deviceLabel.setTextSize(11);
        deviceBar.addView(deviceLabel);
        Button cbtn = new Button(this);
        cbtn.setText("Set Device");
        cbtn.setTextColor(Color.WHITE);
        cbtn.setBackgroundColor(Color.parseColor("#424242"));
        cbtn.setTextSize(10);
        cbtn.setOnClickListener(v -> showDeviceSelector());
        deviceBar.addView(cbtn);
        layout.addView(deviceBar);
        TextView title = new TextView(this);
        title.setText("Port Doctor");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#FF6F00"));
        layout.addView(title);
        TextView sub = new TextView(this);
        sub.setText("ROM Port Bug Detector & Fixer");
        sub.setTextColor(Color.parseColor("#AAAAAA"));
        sub.setTextSize(14);
        layout.addView(sub);
        statusText = new TextView(this);
        statusText.setText("Ready to scan.");
        statusText.setTextColor(Color.parseColor("#4CAF50"));
        layout.addView(statusText);
        scanBtn = new Button(this);
        scanBtn.setText("Scan Device for Issues");
        scanBtn.setTextColor(Color.WHITE);
        scanBtn.setBackgroundColor(Color.parseColor("#E65100"));
        scanBtn.setOnClickListener(v -> runScan());
        layout.addView(scanBtn);
        TextView il = new TextView(this);
        il.setText("\nDetected Issues:");
        il.setTextColor(Color.WHITE);
        il.setTextSize(16);
        layout.addView(il);
        issueContainer = new LinearLayout(this);
        issueContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(issueContainer);
        LinearLayout br = new LinearLayout(this);
        br.setOrientation(LinearLayout.HORIZONTAL);
        fixAllBtn = new Button(this);
        fixAllBtn.setText("Generate Fixes");
        fixAllBtn.setTextColor(Color.WHITE);
        fixAllBtn.setBackgroundColor(Color.parseColor("#2E7D32"));
        fixAllBtn.setEnabled(false);
        fixAllBtn.setOnClickListener(v -> generateFixes());
        br.addView(fixAllBtn);
        buildBtn = new Button(this);
        buildBtn.setText("Build Module");
        buildBtn.setTextColor(Color.WHITE);
        buildBtn.setBackgroundColor(Color.parseColor("#1565C0"));
        buildBtn.setEnabled(false);
        buildBtn.setOnClickListener(v -> buildModule());
        br.addView(buildBtn);
        layout.addView(br);
        TextView rl = new TextView(this);
        rl.setText("\nReports & Sharing");
        rl.setTextColor(Color.parseColor("#FF6F00"));
        rl.setTextSize(16);
        layout.addView(rl);
        LinearLayout rr = new LinearLayout(this);
        rr.setOrientation(LinearLayout.HORIZONTAL);
        Button rb = new Button(this);
        rb.setText("Full Report");
        rb.setTextColor(Color.WHITE);
        rb.setBackgroundColor(Color.parseColor("#6A1B9A"));
        rb.setOnClickListener(v -> generateFullReport());
        rr.addView(rb);
        Button sb = new Button(this);
        sb.setText("Share Report");
        sb.setTextColor(Color.WHITE);
        sb.setBackgroundColor(Color.parseColor("#00838F"));
        sb.setOnClickListener(v -> shareReport());
        rr.addView(sb);
        layout.addView(rr);
        TextView cl = new TextView(this);
        cl.setText("\nCommunity Database");
        cl.setTextColor(Color.parseColor("#FF6F00"));
        cl.setTextSize(16);
        layout.addView(cl);
        Button syb = new Button(this);
        syb.setText("Sync Community Database");
        syb.setTextColor(Color.WHITE);
        syb.setBackgroundColor(Color.parseColor("#00695C"));
        syb.setOnClickListener(v -> syncCommunityDB());
        layout.addView(syb);
        LinearLayout fr = new LinearLayout(this);
        fr.setOrientation(LinearLayout.HORIZONTAL);
        Button bp = new Button(this);
        bp.setText("Browse Fix Packs");
        bp.setTextColor(Color.WHITE);
        bp.setBackgroundColor(Color.parseColor("#37474F"));
        bp.setOnClickListener(v -> browseFixPacks());
        fr.addView(bp);
        Button vc = new Button(this);
        vc.setText("Check Vendor");
        vc.setTextColor(Color.WHITE);
        vc.setBackgroundColor(Color.parseColor("#BF360C"));
        vc.setOnClickListener(v -> checkVendorFiles());
        fr.addView(vc);
        layout.addView(fr);
        logText = new TextView(this);
        logText.setText("\nScan log will appear here...");
        logText.setTextColor(Color.parseColor("#888888"));
        logText.setTextSize(11);
        layout.addView(logText);
        ScrollView sv = new ScrollView(this);
        sv.addView(layout);
        setContentView(sv);
        SharedPreferences prefs = getSharedPreferences("portdoctor", MODE_PRIVATE);
        String sd = prefs.getString("device", "");
        if (!sd.isEmpty()) {
            deviceLabel.setText("Device: " + sd);
        }
    }

    private void runScan() {
        scanBtn.setEnabled(false);
        statusText.setText("Scanning...");
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
                scanBtn.setText("Re-Scan");
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
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            CheckBox cb = new CheckBox(this);
            cb.setText("[" + issue.severity + "] " + issue.name);
            cb.setTextColor(Color.WHITE);
            cb.setChecked(true);
            checkBoxes[i] = cb;
            TextView desc = new TextView(this);
            desc.setText(issue.description);
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
    }

    private void buildModule() {
        progressDialog = ProgressDialog.show(this, "Building", "Creating module...", true);
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
                Toast.makeText(MainActivity.this, (Boolean)r ? "Module saved!" : "Build failed!", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void generateFullReport() {
        progressDialog = ProgressDialog.show(this, "Generating", "Creating report...", true);
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
        d.setTitle("Report");
        TextView tv = new TextView(this);
        tv.setText(lastReport);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(11);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        d.setView(sv);
        d.setPositiveButton("Close", null);
        d.show();
    }

    private void shareReport() {
        Intent si = new Intent(Intent.ACTION_SEND);
        si.setType("text/plain");
        si.putExtra(Intent.EXTRA_TEXT, lastReport);
        startActivity(Intent.createChooser(si, "Share"));
    }

    private void syncCommunityDB() {
        progressDialog = ProgressDialog.show(this, "Syncing", "Downloading...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                if (vendorDB == null) vendorDB = new VendorDB();
                return CloudSync.fetchCommunityDB(vendorDB);
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                CloudSync.SyncResult sr = (CloudSync.SyncResult) r;
                Toast.makeText(MainActivity.this, sr.message, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void browseFixPacks() {
        progressDialog = ProgressDialog.show(this, "Loading", "Fetching...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                fixPackServer = new FixPackServer();
                return fixPackServer.fetchAvailablePacks();
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Packs: " + ((List)r).size(), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void checkVendorFiles() {
        progressDialog = ProgressDialog.show(this, "Checking", "Scanning...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                if (vendorDB == null) vendorDB = new VendorDB();
                vendorDB.scanVendor();
                return vendorDB.getVendorReport();
            }
            protected void onPostExecute(Object r) {
                progressDialog.dismiss();
                AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this);
                d.setTitle("Vendor Analysis");
                TextView tv = new TextView(MainActivity.this);
                tv.setText((String) r);
                tv.setPadding(20, 20, 20, 20);
                d.setView(tv);
                d.setPositiveButton("Close", null);
                d.show();
            }
        }.execute();
    }

    private void showDeviceSelector() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Set Device Info");
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        EditText di = new EditText(this);
        di.setHint("Your device");
        SharedPreferences pr = getSharedPreferences("portdoctor", MODE_PRIVATE);
        di.setText(pr.getString("device", getprop("ro.product.model", "")));
        f.addView(di);
        EditText dn = new EditText(this);
        dn.setHint("Donor device");
        dn.setText(pr.getString("donor", ""));
        f.addView(dn);
        b.setView(f);
        b.setPositiveButton("Save", (d, w) -> {
            pr.edit().putString("device", di.getText().toString().trim())
                      .putString("donor", dn.getText().toString().trim()).commit();
            deviceLabel.setText("Device: " + di.getText().toString().trim());
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private String getprop(String key, String def) {
        try {
            Process p = Runtime.getRuntime().exec("getprop " + key);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String v = br.readLine();
            br.close();
            return v != null ? v.trim() : def;
        } catch (Exception e) {
            return def;
        }
    }
          }
