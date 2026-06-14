package com.realme.portdoctor;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

public class VendorDB {

    public static class VendorFile {
        public String path;
        public String expectedHash;
        public String description;
        public String deviceSource;
        public boolean verified;

        public VendorFile(String path, String hash, String desc, String device) {
            this.path = path;
            this.expectedHash = hash;
            this.description = desc;
            this.deviceSource = device;
            this.verified = false;
        }
    }

    private List<VendorFile> database;
    private List<VendorFile> suspicious;

    public VendorDB() {
        database = new ArrayList<>();
        suspicious = new ArrayList<>();
        loadLocalCache();
        if (database.isEmpty()) {
            autoSeedDatabase();
        }
    }

    private void loadLocalCache() {
        try {
            File cacheFile = new File("/sdcard/PortDoctor/vendor_db.json");
            if (cacheFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(cacheFile));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    json.append(line);
                }
                br.close();
                importDatabase(json.toString());
            }
        } catch (Exception ignored) {}
    }

    private void autoSeedDatabase() {
        String currentDevice = getBuildProp("ro.product.model", "Unknown Device");
        String manufacturer = getBuildProp("ro.product.manufacturer", "Unknown");
        
        String[] commonPaths = {
            "/vendor/etc/mixer_paths.xml",
            "/vendor/lib64/hw/camera.qcom.so",
            "/vendor/lib64/sensors.ssc.so",
            "/vendor/lib64/hw/display.qcom.so"
        };
        
        for (String path : commonPaths) {
            File file = new File(path);
            if (file.exists()) {
                database.add(new VendorFile(
                    path,
                    computeFileHash(file),
                    "Auto-detected vendor file for " + currentDevice,
                    currentDevice + " (" + manufacturer + ")"
                ));
            }
        }
    }

    private String computeFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
            fis.close();
            
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "ERROR_" + file.length() + "_" + file.lastModified();
        }
    }

    private String getBuildProp(String key, String defaultVal) {
        try {
            Process p = Runtime.getRuntime().exec("getprop " + key);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String val = br.readLine();
            br.close();
            return val != null ? val.trim() : defaultVal;
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public List<VendorFile> scanVendor() {
        suspicious.clear();
        
        for (VendorFile dbFile : database) {
            File actualFile = new File(dbFile.path);
            if (actualFile.exists()) {
                String actualHash = computeFileHash(actualFile);
                if (!actualHash.equals(dbFile.expectedHash)) {
                    dbFile.verified = false;
                    suspicious.add(dbFile);
                } else {
                    dbFile.verified = true;
                }
            } else {
                dbFile.verified = false;
                suspicious.add(dbFile);
            }
        }
        
        return suspicious;
    }

    public String getVendorReport() {
        StringBuilder report = new StringBuilder();
        report.append("\nVENDOR FILE ANALYSIS\n");
        report.append("--------------------\n\n");
        
        if (suspicious.isEmpty()) {
            report.append("All checked vendor files match known-good versions.\n");
        } else {
            report.append("Found " + suspicious.size() + " suspicious file(s):\n\n");
            
            for (VendorFile vf : suspicious) {
                report.append("  ! " + vf.path + "\n");
                report.append("     " + vf.description + "\n");
                report.append("     Expected: " + vf.deviceSource + "\n\n");
            }
        }
        
        return report.toString();
    }

    public List<VendorFile> getDatabase() {
        return database;
    }

    public void addEntry(String path, String hash, String desc, String device) {
        database.add(new VendorFile(path, hash, desc, device));
    }

    public String exportDatabase() {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"vendor_files\": [\n");
        
        for (int i = 0; i < database.size(); i++) {
            VendorFile vf = database.get(i);
            json.append("    {\n");
            json.append("      \"path\": \"" + vf.path + "\",\n");
            json.append("      \"hash\": \"" + vf.expectedHash + "\",\n");
            json.append("      \"description\": \"" + vf.description + "\",\n");
            json.append("      \"device\": \"" + vf.deviceSource + "\"\n");
            json.append("    }");
            if (i < database.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ]\n}");
        return json.toString();
    }

    public void importDatabase(String json) {
        String[] entries = json.split("\\{");
        for (String entry : entries) {
            if (entry.contains("\"path\"")) {
                String path = extractValue(entry, "path");
                String hash = extractValue(entry, "hash");
                String desc = extractValue(entry, "description");
                String device = extractValue(entry, "device");
                
                if (path != null && hash != null) {
                    database.add(new VendorFile(path, hash, 
                        desc != null ? desc : "Community submission",
                        device != null ? device : "Unknown"));
                }
            }
        }
    }

    private String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\": \"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    public void saveLocalCache() {
        try {
            File cacheDir = new File("/sdcard/PortDoctor/");
            cacheDir.mkdirs();
            FileWriter fw = new FileWriter(new File(cacheDir, "vendor_db.json"));
            fw.write(exportDatabase());
            fw.close();
        } catch (Exception ignored) {}
    }
}
