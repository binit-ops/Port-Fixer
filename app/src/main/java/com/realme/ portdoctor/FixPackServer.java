package com.realme.portdoctor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FixPackServer {

    private static final String FIXPACK_REPO = 
        "https://raw.githubusercontent.com/YOUR_USERNAME/PortDoctor/main/fixpacks/";

    public static class FixPack {
        public String name;
        public String donorDevice;
        public String targetDevice;
        public String description;
        public String downloadUrl;
        public int downloads;
        public String author;

        public FixPack(String name, String donor, String target, 
                       String desc, String url, int dls, String author) {
            this.name = name;
            this.donorDevice = donor;
            this.targetDevice = target;
            this.description = desc;
            this.downloadUrl = url;
            this.downloads = dls;
            this.author = author;
        }
    }

    private List<FixPack> availablePacks;

    public FixPackServer() {
        availablePacks = new ArrayList<>();
    }

    public List<FixPack> fetchAvailablePacks() {
        try {
            URL url = new URL(FIXPACK_REPO + "index.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            
            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                json.append(line);
            }
            br.close();
            
            parsePackList(json.toString());
        } catch (Exception e) {
            // Return empty list if server unavailable
        }
        return availablePacks;
    }

    private void parsePackList(String json) {
        String[] packs = json.split("\\{");
        for (String packStr : packs) {
            if (packStr.contains("\"name\"")) {
                String name = extractValue(packStr, "name");
                String donor = extractValue(packStr, "donor");
                String target = extractValue(packStr, "target");
                String desc = extractValue(packStr, "description");
                String url = extractValue(packStr, "url");
                String author = extractValue(packStr, "author");
                int downloads = 0;
                try {
                    downloads = Integer.parseInt(extractValue(packStr, "downloads", "0"));
                } catch (Exception ignored) {}
                
                if (name != null && url != null) {
                    availablePacks.add(new FixPack(name, donor, target, 
                        desc, url, downloads, author));
                }
            }
        }
    }

    private String extractValue(String json, String key) {
        return extractValue(json, key, null);
    }

    private String extractValue(String json, String key, String defaultVal) {
        String searchKey = "\"" + key + "\": \"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            searchKey = "\"" + key + "\": ";
            start = json.indexOf(searchKey);
            if (start == -1) return defaultVal;
            start += searchKey.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (end == -1) return defaultVal;
            return json.substring(start, end).trim();
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return defaultVal;
        return json.substring(start, end);
    }

    public static File downloadFixPack(String urlStr, String savePath) {
        try {
            URL downloadUrl = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) downloadUrl.openConnection();
            conn.setRequestMethod("GET");
            
            FileOutputStream fos = new FileOutputStream(savePath);
            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();
            
            return new File(savePath);
        } catch (Exception e) {
            return null;
        }
    }

    public List<FixPack> getAllPacks() {
        return availablePacks;
    }
}
