package com.realme.portdoctor;

import java.io.*;
import java.util.*;

public class BackupManager {

    public static class Backup {
        public String path;
        public String backupPath;
        public long size;
        public String date;
    }

    public static List<Backup> backupVendorFiles(File backupDir) throws Exception {
        List<Backup> backups = new ArrayList<>();
        backupDir.mkdirs();
        String date = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String[] criticalPaths = {
            "/vendor/etc/mixer_paths.xml",
            "/vendor/etc/mixer_paths_mtp.xml",
            "/vendor/lib64/hw/camera.qcom.so",
            "/vendor/lib64/sensors.ssc.so",
            "/vendor/lib64/hw/display.qcom.so"
        };

        for (String path : criticalPaths) {
            File source = new File(path);
            if (source.exists()) {
                String name = source.getName();
                File dest = new File(backupDir, date + "_" + name);
                copyFile(source, dest);
                
                Backup b = new Backup();
                b.path = path;
                b.backupPath = dest.getAbsolutePath();
                b.size = dest.length();
                b.date = date;
                backups.add(b);
            }
        }
        return backups;
    }

    public static boolean restoreFile(String backupPath, String originalPath) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("cp " + backupPath + " " + originalPath + "\n");
            os.writeBytes("chmod 644 " + originalPath + "\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dst);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = fis.read(buffer)) > 0) fos.write(buffer, 0, len);
        fis.close();
        fos.close();
    }
}
