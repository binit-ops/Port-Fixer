package com.realme.portdoctor;

import android.app.Activity;
import android.content.SharedPreferences;
import java.util.*;

public class HistoryManager {

    private static final String PREF = "portdoctor_history";
    private static final int MAX_HISTORY = 20;

    public static class HistoryEntry {
        public String date;
        public String device;
        public String donor;
        public int issuesFixed;
        public String fixesList;

        public HistoryEntry(String date, String device, String donor, int issuesFixed, String fixesList) {
            this.date = date;
            this.device = device;
            this.donor = donor;
            this.issuesFixed = issuesFixed;
            this.fixesList = fixesList;
        }

        public String toString() {
            return date + " | " + device + " -> " + donor + " | " + issuesFixed + " fixes";
        }
    }

    public static void saveEntry(Activity activity, HistoryEntry entry) {
        SharedPreferences prefs = activity.getSharedPreferences(PREF, Activity.MODE_PRIVATE);
        String existing = prefs.getString("history", "");
        String newEntry = entry.date + "||" + entry.device + "||" + entry.donor + "||" + 
            entry.issuesFixed + "||" + entry.fixesList;
        if (existing.isEmpty()) {
            existing = newEntry;
        } else {
            String[] lines = existing.split("\n");
            StringBuilder sb = new StringBuilder(newEntry);
            int count = 1;
            for (String line : lines) {
                if (count >= MAX_HISTORY) break;
                sb.append("\n").append(line);
                count++;
            }
            existing = sb.toString();
        }
        prefs.edit().putString("history", existing).commit();
    }

    public static List<HistoryEntry> getHistory(Activity activity) {
        List<HistoryEntry> list = new ArrayList<>();
        SharedPreferences prefs = activity.getSharedPreferences(PREF, Activity.MODE_PRIVATE);
        String data = prefs.getString("history", "");
        if (!data.isEmpty()) {
            String[] lines = data.split("\n");
            for (String line : lines) {
                String[] parts = line.split("\\|\\|");
                if (parts.length >= 5) {
                    list.add(new HistoryEntry(parts[0], parts[1], parts[2], 
                        Integer.parseInt(parts[3]), parts[4]));
                }
            }
        }
        return list;
    }

    public static void clearHistory(Activity activity) {
        activity.getSharedPreferences(PREF, Activity.MODE_PRIVATE)
            .edit().putString("history", "").commit();
    }
}
