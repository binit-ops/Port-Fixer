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
    private TextView logText, statusText, deviceLabel;
    private Button scanBtn, fixAllBtn, buildBtn;
    private CheckBox[] checkBoxes;
    private List detectedIssues, fixes;
    private VendorDB vendorDB;
    private FixPackServer fixPackServer;
    private String lastReport;
    private ProgressDialog pd;
    private ThemeManager tm;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        tm = new ThemeManager(this);
        ScrollView sv = new ScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(20, 40, 20, 40);
        l.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, tm.getBackgroundGradient()));
        
        LinearLayout hc = gc(20, 25); hc.setOrientation(LinearLayout.VERTICAL);
        LinearLayout tr = new LinearLayout(this); tr.setOrientation(LinearLayout.HORIZONTAL); tr.setGravity(Gravity.CENTER_VERTICAL);
        TextView iv = new TextView(this); iv.setText("P"); iv.setTextSize(40); iv.setTextColor(Color.parseColor("#FF6B6B")); iv.setTypeface(null, android.graphics.Typeface.BOLD); tr.addView(iv);
        LinearLayout tc = new LinearLayout(this); tc.setOrientation(LinearLayout.VERTICAL); tc.setPadding(15, 0, 0, 0);
        TextView t1 = new TextView(this); t1.setText("Port Doctor"); t1.setTextSize(28); t1.setTextColor(Color.parseColor(tm.getTextPrimary())); t1.setTypeface(null, android.graphics.Typeface.BOLD); tc.addView(t1);
        TextView t2 = new TextView(this); t2.setText("ROM Diagnostic Suite"); t2.setTextColor(Color.parseColor(tm.getTextSecondary())); t2.setTextSize(13); tc.addView(t2);
        tr.addView(tc); hc.addView(tr);
        
        LinearLayout dc = gc(8, 12); dc.setPadding(15, 10, 15, 10);
        deviceLabel = new TextView(this); deviceLabel.setText("Device: " + gp("ro.product.model", "?")); deviceLabel.setTextColor(Color.parseColor("#FFE66D")); deviceLabel.setTextSize(12); dc.addView(deviceLabel);
        Button cb = sb("Switch", "#7C4DFF", 0); cb.setOnClickListener(v -> showDeviceSelector()); dc.addView(cb);
        Button tb = sb(tm.isDark() ? "Light" : "Dark", "#FFA502", 0); tb.setOnClickListener(v -> { tm.toggle(this); recreate(); }); dc.addView(tb);
        hc.addView(dc); l.addView(hc);
        
        LinearLayout sc = gc(15, 15); sc.setPadding(20, 12, 20, 12);
        statusText = new TextView(this); statusText.setText("Ready"); statusText.setTextColor(Color.parseColor(tm.getTextPrimary())); statusText.setTextSize(14); sc.addView(statusText); l.addView(sc);
        
        scanBtn = gb("Scan Device", "#FF6B6B", "#4ECDC4"); scanBtn.setOnClickListener(v -> runScan()); l.addView(scanBtn);
        
        TextView ih = new TextView(this); ih.setText("\nISSUES"); ih.setTextColor(Color.parseColor(tm.getTextSecondary())); ih.setTextSize(11); ih.setTypeface(null, android.graphics.Typeface.BOLD); ih.setPadding(10, 20, 0, 10); l.addView(ih);
        issueContainer = new LinearLayout(this); issueContainer.setOrientation(LinearLayout.VERTICAL); l.addView(issueContainer);
        
        LinearLayout ar = new LinearLayout(this); ar.setOrientation(LinearLayout.HORIZONTAL); ar.setPadding(0, 15, 0, 0);
        fixAllBtn = sb("Generate", "#2ED573", 0); fixAllBtn.setEnabled(false); fixAllBtn.setAlpha(0.5f); fixAllBtn.setOnClickListener(v -> generateFixes()); ar.addView(fixAllBtn);
        View g1 = new View(this); g1.setLayoutParams(new LinearLayout.LayoutParams(10, 1)); ar.addView(g1);
        buildBtn = sb("Build", "#1E90FF", 0); buildBtn.setEnabled(false); buildBtn.setAlpha(0.5f); buildBtn.setOnClickListener(v -> buildModule()); ar.addView(buildBtn); l.addView(ar);
        
        TextView rh = new TextView(this); rh.setText("\nTOOLS"); rh.setTextColor(Color.parseColor(tm.getTextSecondary())); rh.setTextSize(11); rh.setTypeface(null, android.graphics.Typeface.BOLD); rh.setPadding(10, 20, 0, 10); l.addView(rh);
        LinearLayout rr = new LinearLayout(this); rr.setOrientation(LinearLayout.HORIZONTAL);
        Button rp = sb("Report", "#7C4DFF", 0); rp.setOnClickListener(v -> genReport()); rr.addView(rp);
        View g2 = new View(this); g2.setLayoutParams(new LinearLayout.LayoutParams(8, 1)); rr.addView(g2);
        Button sh = sb("Share", "#00CED1", 0); sh.setOnClickListener(v -> shareReport()); rr.addView(sh); l.addView(rr);
        
        LinearLayout rr2 = new LinearLayout(this); rr2.setOrientation(LinearLayout.HORIZONTAL); rr2.setPadding(0, 8, 0, 0);
        Button bk = sb("Backup", "#FF6B6B", 0); bk.setOnClickListener(v -> createBackup()); rr2.addView(bk);
        View g3 = new View(this); g3.setLayoutParams(new LinearLayout.LayoutParams(8, 1)); rr2.addView(g3);
        Button hy = sb("History", "#FFE66D", 0); hy.setOnClickListener(v -> showHistory()); rr2.addView(hy); l.addView(rr2);
        
        TextView ch = new TextView(this); ch.setText("\nCOMMUNITY"); ch.setTextColor(Color.parseColor(tm.getTextSecondary())); ch.setTextSize(11); ch.setTypeface(null, android.graphics.Typeface.BOLD); ch.setPadding(10, 20, 0, 10); l.addView(ch);
        Button sy = gb("Sync Database", "#7C4DFF", "#1E90FF"); sy.setOnClickListener(v -> syncDB()); l.addView(sy);
        LinearLayout cr = new LinearLayout(this); cr.setOrientation(LinearLayout.HORIZONTAL); cr.setPadding(0, 8, 0, 0);
        Button br = sb("Packs", "#FFA502", 0); br.setOnClickListener(v -> browsePacks()); cr.addView(br);
        View g4 = new View(this); g4.setLayoutParams(new LinearLayout.LayoutParams(8, 1)); cr.addView(g4);
        Button vc = sb("Vendor", "#FF4757", 0); vc.setOnClickListener(v -> checkVendor()); cr.addView(vc); l.addView(cr);
        
        LinearLayout lc = gc(12, 15); lc.setPadding(15, 12, 15, 12);
        logText = new TextView(this); logText.setText("Log..."); logText.setTextColor(Color.parseColor(tm.getLogText())); logText.setTextSize(11); lc.addView(logText); l.addView(lc);
        
        sv.addView(l); setContentView(sv);
        SharedPreferences pr = getSharedPreferences("pd", MODE_PRIVATE);
        String sd = pr.getString("device", "");
        if (!sd.isEmpty()) deviceLabel.setText("Device: " + sd);
    }

    private LinearLayout gc(int r, int mt) {
        LinearLayout c = new LinearLayout(this);
        GradientDrawable g = new GradientDrawable(); g.setCornerRadius(r * 2); g.setColor(Color.parseColor(tm.getCardBg())); g.setStroke(1, Color.parseColor(tm.getCardBorder())); c.setBackground(g); c.setPadding(20, 15, 20, 15);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, mt, 0, 0); c.setLayoutParams(p);
        return c;
    }

    private Button gb(String t, String c1, String c2) {
        Button b = new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setAllCaps(false); b.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.parseColor(c1), Color.parseColor(c2)}); g.setCornerRadius(50); b.setBackground(g); b.setPadding(30, 18, 30, 18);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 12, 0, 0); b.setLayoutParams(p);
        return b;
    }

    private Button sb(String t, String c, int mt) {
        Button b = new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setTextSize(11); b.setAllCaps(false); b.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable g = new GradientDrawable(); g.setCornerRadius(25); g.setColor(Color.parseColor(c)); g.setStroke(1, Color.parseColor("#44FFFFFF")); b.setBackground(g); b.setPadding(18, 10, 18, 10);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.setMargins(0, mt, 0, 0); b.setLayoutParams(p);
        return b;
    }

    private void runScan() {
        scanBtn.setEnabled(false); scanBtn.setText("Scanning..."); issueContainer.removeAllViews();
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                SharedPreferences pr = getSharedPreferences("pd", MODE_PRIVATE);
                scanner = new PortScanner(pr.getString("device", gp("ro.product.model", "?")), pr.getString("donor", ""));
                return scanner.scanAll();
            }
            protected void onPostExecute(Object r) { detectedIssues = (List) r; disp(); scanBtn.setEnabled(true); scanBtn.setText("Scan Device"); }
        }.execute();
    }

    private void disp() {
        if (detectedIssues.isEmpty()) { statusText.setText("No issues!"); logText.setText(scanner.getScanLog()); return; }
        statusText.setText("Found " + detectedIssues.size() + " issues");
        checkBoxes = new CheckBox[detectedIssues.size()];
        for (int i = 0; i < detectedIssues.size(); i++) {
            PortScanner.Issue is = (PortScanner.Issue) detectedIssues.get(i);
            LinearLayout ic = gc(10, 6); ic.setOrientation(LinearLayout.VERTICAL); ic.setPadding(15, 10, 15, 10);
            CheckBox cb = new CheckBox(this); cb.setText("[" + is.severity + "] " + is.name); cb.setTextColor(Color.parseColor(tm.getTextPrimary())); cb.setChecked(true); checkBoxes[i] = cb;
            TextView d = new TextView(this); d.setText(is.description); d.setTextColor(Color.parseColor(tm.getTextSecondary())); d.setTextSize(11);
            ic.addView(cb); ic.addView(d); issueContainer.addView(ic);
        }
        fixAllBtn.setEnabled(true); fixAllBtn.setAlpha(1.0f); logText.setText(scanner.getScanLog());
    }

    private void generateFixes() {
        List s = new ArrayList();
        for (int i = 0; i < checkBoxes.length; i++) if (checkBoxes[i].isChecked()) s.add(detectedIssues.get(i));
        scanner.getDetectedIssues().clear(); scanner.getDetectedIssues().addAll(s);
        fixEngine = new FixEngine(scanner); fixes = fixEngine.generateFixes();
        statusText.setText("Generated " + fixes.size() + " fixes"); buildBtn.setEnabled(true); buildBtn.setAlpha(1.0f);
    }

    private void buildModule() {
        if (fixes == null || fixes.isEmpty()) { Toast.makeText(this, "Generate fixes first!", Toast.LENGTH_SHORT).show(); return; }
        pd = ProgressDialog.show(this, "", "Building...", true);
        final File od = getExternalFilesDir(null);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) {
                try { return new Object[]{true, ModuleBuilder.buildModule(fixes, od).getAbsolutePath()}; }
                catch (Exception e) { return new Object[]{false, e.getMessage()}; }
            }
            protected void onPostExecute(Object r) {
                pd.dismiss(); Object[] a = (Object[]) r;
                if ((Boolean) a[0]) {
                    statusText.setText("Module saved!");
                    HistoryManager.saveEntry(MainActivity.this, new HistoryManager.HistoryEntry(new Date().toString(), gp("ro.product.model","?"), getSharedPreferences("pd",MODE_PRIVATE).getString("donor",""), fixes.size(), ""));
                    new AlertDialog.Builder(MainActivity.this).setTitle("Module Built").setMessage("Saved:\n" + a[1] + "\n\nFlash in Magisk.").setPositiveButton("OK", null).show();
                } else {
                    new AlertDialog.Builder(MainActivity.this).setTitle("Failed").setMessage("Error: " + a[1]).setPositiveButton("OK", null).show();
                }
            }
        }.execute();
    }

    private void genReport() {
        pd = ProgressDialog.show(this, "", "Report...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { return ReportGenerator.generateReport(scanner, fixes); }
            protected void onPostExecute(Object r) { pd.dismiss(); lastReport = (String) r; showPreview(); }
        }.execute();
    }

    private void showPreview() {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle("Report");
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(lastReport.length() > 2000 ? lastReport.substring(0, 2000) + "..." : lastReport);
        tv.setPadding(20, 20, 20, 20); tv.setTextSize(11);
        sv.addView(tv);
        d.setView(sv);
        d.setPositiveButton("Close", null);
        d.setNegativeButton("Share", (x, y) -> shareReport());
        d.show();
    }

    private void shareReport() {
        Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT, lastReport); startActivity(Intent.createChooser(i, "Share"));
    }

    private void createBackup() {
        pd = ProgressDialog.show(this, "", "Backup...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { try { return BackupManager.backupVendorFiles(new File(getExternalFilesDir(null), "backups")); } catch (Exception e) { return e.getMessage(); } }
            protected void onPostExecute(Object r) { pd.dismiss(); Toast.makeText(MainActivity.this, r instanceof List ? ((List)r).size() + " files backed up!" : "Failed: " + r, Toast.LENGTH_SHORT).show(); }
        }.execute();
    }

    private void showHistory() {
        List<HistoryManager.HistoryEntry> h = HistoryManager.getHistory(this);
        if (h.isEmpty()) { Toast.makeText(this, "No history", Toast.LENGTH_SHORT).show(); return; }
        StringBuilder sb = new StringBuilder();
        for (HistoryManager.HistoryEntry e : h) sb.append(e.toString()).append("\n\n");
        new AlertDialog.Builder(this).setTitle("History").setMessage(sb.toString()).setPositiveButton("Clear", (x,y)->{HistoryManager.clearHistory(this);Toast.makeText(this,"Cleared",Toast.LENGTH_SHORT).show();}).setNegativeButton("Close",null).show();
    }

    private void syncDB() {
        pd = ProgressDialog.show(this, "", "Syncing...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { if (vendorDB == null) vendorDB = new VendorDB(); return CloudSync.fetchCommunityDB(vendorDB); }
            protected void onPostExecute(Object r) { pd.dismiss(); Toast.makeText(MainActivity.this, ((CloudSync.SyncResult)r).message, Toast.LENGTH_SHORT).show(); }
        }.execute();
    }

    private void browsePacks() {
        pd = ProgressDialog.show(this, "", "Fetching...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { fixPackServer = new FixPackServer(); return fixPackServer.fetchAvailablePacks(); }
            protected void onPostExecute(Object r) { pd.dismiss(); Toast.makeText(MainActivity.this, ((List)r).size() + " packs", Toast.LENGTH_SHORT).show(); }
        }.execute();
    }

    private void checkVendor() {
        pd = ProgressDialog.show(this, "", "Scanning...", true);
        new AsyncTask() {
            protected Object doInBackground(Object[] p) { if (vendorDB == null) vendorDB = new VendorDB(); vendorDB.scanVendor(); return vendorDB.getVendorReport(); }
            protected void onPostExecute(Object r) { pd.dismiss(); new AlertDialog.Builder(MainActivity.this).setTitle("Vendor").setMessage((String)r).setPositiveButton("Close",null).show(); }
        }.execute();
    }

    private void showDeviceSelector() {
        AlertDialog.Builder b = new AlertDialog.Builder(this); b.setTitle("Set Device");
        LinearLayout f = new LinearLayout(this); f.setOrientation(LinearLayout.VERTICAL); f.setPadding(30, 20, 30, 20);
        EditText di = new EditText(this); di.setHint("Your device"); SharedPreferences pr = getSharedPreferences("pd", MODE_PRIVATE); di.setText(pr.getString("device", gp("ro.product.model", ""))); f.addView(di);
        EditText dn = new EditText(this); dn.setHint("Donor device"); dn.setText(pr.getString("donor", "")); f.addView(dn);
        b.setView(f); b.setPositiveButton("Save", (d, w) -> { pr.edit().putString("device", di.getText().toString().trim()).putString("donor", dn.getText().toString().trim()).commit(); deviceLabel.setText("Device: " + di.getText().toString().trim()); }); b.setNegativeButton("Cancel", null); b.show();
    }

    private String gp(String k, String d) {
        try { Process p = Runtime.getRuntime().exec("getprop " + k); BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())); String v = br.readLine(); br.close(); return v != null ? v.trim() : d; }
        catch (Exception e) { return d; }
    }
}
