package com.realme.portdoctor;

import java.io.*;
import java.net.*;

public class CloudSync {

    private static final String CLOUD_DB_URL = 
        "https://raw.githubusercontent.com/YOUR_USERNAME/PortDoctor/main/community_db.json";

    public static class SyncResult {
        public boolean success;
        public int newEntries;
        public String message;

        public SyncResult(boolean s, int n, String m) {
            success = s;
            newEntries = n;
            message = m;
        }
    }

    public static SyncResult fetchCommunityDB(VendorDB localDB) {
        try {
            URL url = new URL(CLOUD_DB_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            
            int beforeCount = localDB.getDatabase().size();
            localDB.importDatabase(response.toString());
            int newEntries = localDB.getDatabase().size() - beforeCount;
            
            return new SyncResult(true, newEntries, 
                "Synced " + newEntries + " entries from community database");
        } catch (Exception e) {
            return new SyncResult(false, 0, 
                "Sync failed. Check your internet connection.");
        }
    }
}
