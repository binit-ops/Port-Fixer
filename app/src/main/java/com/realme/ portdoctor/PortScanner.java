package com.realme.portdoctor;

import java.io.*;
import java.util.*;

public class PortScanner {

    public static class Issue {
        public String name;
        public String description;
        public String severity;
        public String fixType;
        public boolean detected;

        public Issue(String name, String desc, String severity, String fixType) {
            this.name = name;
            this.description = desc;
            this.severity = severity;
            this.fixType = fixType;
            this.detected = false;
        }
    }

    private List<Issue> issues;
    private StringBuilder scanLog;
    private String currentDevice;
    private String donorDevice;

    public PortScanner(String currentDevice, String donorDevice) {
        this.currentDevice = currentDevice;
        this.donorDevice = donorDevice;
        issues = new ArrayList<>();
        scanLog = new StringBuilder();
        initIssues();
    }

    private void initIssues() {
        issues.add(new Issue("Brightness Range Mismatch", 
            "Panel supports higher brightness than ROM currently allows", "HIGH", "brightness"));
        issues.add(new Issue("Speaker/Earpiece Volume Low", 
            "Audio mixer paths may not be optimized for this device", "MEDIUM", "audio"));
        issues.add(new Issue("Proximity Sensor Issue", 
            "Screen may not turn off during calls", "HIGH", "sensor"));
        issues.add(new Issue("Auto-Rotation Issue", 
            "Accelerometer/Gyroscope may not be calibrated correctly", "HIGH", "sensor"));
        issues.add(new Issue("Camera Functionality Reduced", 
            "Camera HAL or processing pipeline may be from donor", "HIGH", "camera"));
        issues.add(new Issue("Flashlight Control Missing", 
            "Torch LED path may be incorrect for this device", "LOW", "camera"));
        issues.add(new Issue("Refresh Rate Limited", 
            "Display driver may not expose all supported modes", "MEDIUM", "display"));
        issues.add(new Issue("High Brightness Mode Unavailable", 
            "HBM trigger path may be missing or incorrect", "MEDIUM", "display"));
        issues.add(new Issue("Charging Speed Below Capability", 
            "Charger driver values may be from donor device", "MEDIUM", "thermal"));
        issues.add(new Issue("Vibration Intensity Low", 
            "Haptic driver may not be calibrated for this device", "LOW", "other"));
    }

    public List<Issue> scanAll() {
        scanLog.setLength(0);
        scanLog.append("=== Port Doctor Scan ===\n");
        scanLog.append("Device: ").append(currentDevice).append("\n");
        if (donorDevice != null && !donorDevice.isEmpty()) {
            scanLog.append("Donor ROM source: ").append(donorDevice).append("\n");
        }
        scanLog.append("Time: ").append(new Date()).append("\n\n");

        checkBrightness();
        checkAudio();
        checkSensors();
        checkCamera();
        checkDisplay();
        checkCharging();
        checkOther();

        // Summary
        int detected = getDetectedIssues().size();
        scanLog.append("\n=== Scan Complete: ").append(detected)
            .append(" issue(s) found ===\n");
        if (detected == 0) {
            scanLog.append("Your ROM appears well-configured for this device.\n");
        }
        return getDetectedIssues();
    }

    private void checkBrightness() {
        scanLog.append("[Brightness]\n");
        try {
            File blDir = new File("/sys/class/backlight/");
            File[] panels = blDir.listFiles();
            
            if (panels != null && panels.length > 0) {
                File panel = panels[0];
                String maxPath = panel.getAbsolutePath() + "/max_brightness";
                String brightPath = panel.getAbsolutePath() + "/brightness";
                
                BufferedReader br = new BufferedReader(new FileReader(maxPath));
                int maxBright = Integer.parseInt(br.readLine().trim());
                br.close();
                
                br = new BufferedReader(new FileReader(brightPath));
                int currentBright = Integer.parseInt(br.readLine().trim());
                br.close();

                scanLog.append("  Panel detected: ").append(panel.getName()).append("\n");
                scanLog.append("  Hardware maximum: ").append(maxBright).append("\n");
                scanLog.append("  Current value: ").append(currentBright).append("\n");

                if (maxBright > 255 && currentBright <= 255) {
                    markDetected("Brightness Range Mismatch");
                    scanLog.append("  -> ISSUE: Panel supports ").append(maxBright)
                        .append(" but ROM may be capping at lower range\n");
                } else {
                    scanLog.append("  -> OK: Brightness range appears normal\n");
                }
            } else {
                scanLog.append("  Standard backlight path not found.\n");
                scanLog.append("  This is normal for some kernels. Brightness may still work.\n");
            }
        } catch (Exception e) {
            scanLog.append("  Could not check: ").append(e.getMessage()).append("\n");
        }
        scanLog.append("\n");
    }

    private void checkAudio() {
        scanLog.append("[Audio]\n");
        try {
            File mixerDir = new File("/vendor/etc/");
            File[] mixerFiles = mixerDir.listFiles((dir, name) -> 
                name.contains("mixer_path") && name.endsWith(".xml"));
            
            if (mixerFiles != null && mixerFiles.length > 0) {
                scanLog.append("  Mixer files found: ").append(mixerFiles.length).append("\n");
                
                boolean lowVolume = false;
                for (File f : mixerFiles) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) content.append(line);
                        br.close();
                        
                        if (content.toString().contains("RX1 Digital Volume") && 
                            content.toString().contains("value=\"84\"")) {
                            lowVolume = true;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
                
                if (lowVolume) {
                    markDetected("Speaker/Earpiece Volume Low");
                    scanLog.append("  -> ISSUE: Volume values appear to be at defaults\n");
                } else {
                    scanLog.append("  -> OK: Volume levels appear configured\n");
                }
            } else {
                scanLog.append("  No mixer files found at standard location\n");
            }
        } catch (Exception e) {
            scanLog.append("  Could not check: ").append(e.getMessage()).append("\n");
        }
        scanLog.append("\n");
    }

    private void checkSensors() {
        scanLog.append("[Sensors]\n");
        boolean proxOk = false;
        boolean accelOk = false;

        // Check proximity via multiple universal paths
        String[] proxPaths = {
            "/sys/class/sensors/proximity_sensor/raw_data",
            "/sys/devices/virtual/sensors/proximity_sensor/prox_raw",
            "/sys/bus/platform/drivers/proximity/proximity_sensor",
            "/proc/proximity"
        };
        for (String p : proxPaths) {
            if (new File(p).exists()) { proxOk = true; break; }
        }

        // Check accelerometer via multiple universal paths
        String[] accelPaths = {
            "/sys/class/sensors/accelerometer_sensor/raw_data",
            "/sys/devices/virtual/sensors/accelerometer_sensor/accel_data",
            "/sys/bus/iio/devices/iio:device0/in_accel_x_raw"
        };
        for (String p : accelPaths) {
            if (new File(p).exists()) { accelOk = true; break; }
        }

        // Check sensor service via getprop (universal)
        try {
            Process proc = Runtime.getRuntime().exec("getprop init.svc.sensors");
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String status = br.readLine();
            br.close();
            scanLog.append("  Sensor service: ").append(status != null ? status.trim() : "unknown").append("\n");
        } catch (Exception ignored) {}

        if (!proxOk) {
            markDetected("Proximity Sensor Issue");
            scanLog.append("  -> ISSUE: Proximity sensor path not detected\n");
        } else {
            scanLog.append("  Proximity sensor: Detected\n");
        }

        if (!accelOk) {
            markDetected("Auto-Rotation Issue");
            scanLog.append("  -> ISSUE: Accelerometer path not detected\n");
        } else {
            scanLog.append("  Accelerometer: Detected\n");
        }
        scanLog.append("\n");
    }

    private void checkCamera() {
        scanLog.append("[Camera]\n");
        try {
            // Check camera HAL libraries
            int libCount = 0;
            String[] libDirs = {"/vendor/lib64/", "/vendor/lib/"};
            for (String dir : libDirs) {
                File d = new File(dir);
                File[] libs = d.listFiles((f, name) -> 
                    name.contains("camera") && name.endsWith(".so"));
                if (libs != null) libCount += libs.length;
            }
            
            if (libCount > 0) {
                scanLog.append("  Camera libraries: ").append(libCount).append(" found\n");
            } else {
                markDetected("Camera Functionality Reduced");
                scanLog.append("  -> ISSUE: No camera libraries detected\n");
            }

            // Check flashlight via universal paths
            String[] flashPaths = {
                "/sys/class/leds/flashlight/brightness",
                "/sys/class/leds/torch-light/brightness",
                "/sys/class/leds/led:torch_0/brightness",
                "/sys/class/leds/led:flash_0/brightness",
                "/sys/class/leds/led:switch/brightness"
            };
            boolean flashFound = false;
            for (String p : flashPaths) {
                if (new File(p).exists()) { flashFound = true; break; }
            }
            
            if (flashFound) {
                scanLog.append("  Flashlight control: Detected\n");
            } else {
                markDetected("Flashlight Control Missing");
                scanLog.append("  -> ISSUE: Flashlight path not found\n");
            }
        } catch (Exception e) {
            scanLog.append("  Could not check: ").append(e.getMessage()).append("\n");
        }
        scanLog.append("\n");
    }

    private void checkDisplay() {
        scanLog.append("[Display]\n");
        
        // Check refresh rate via universal paths
        String[] refreshPaths = {
            "/sys/class/graphics/fb0/refresh_rate",
            "/sys/class/graphics/fb0/fps",
            "/sys/devices/virtual/graphics/fb0/refresh_rate"
        };
        boolean refreshFound = false;
        for (String p : refreshPaths) {
            File f = new File(p);
            if (f.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    scanLog.append("  Refresh rate: ").append(br.readLine().trim()).append(" Hz\n");
                    br.close();
                    refreshFound = true;
                    break;
                } catch (Exception ignored) {}
            }
        }
        if (!refreshFound) {
            markDetected("Refresh Rate Limited");
            scanLog.append("  -> ISSUE: Refresh rate info not exposed by kernel\n");
        }

        // Check HBM via universal paths
        String[] hbmPaths = {
            "/sys/class/backlight/panel0-backlight/hbm_mode",
            "/sys/class/backlight/backlight/hbm_mode",
            "/sys/class/backlight/panel0-backlight/hbm"
        };
        boolean hbmFound = false;
        for (String p : hbmPaths) {
            if (new File(p).exists()) { hbmFound = true; break; }
        }
        if (hbmFound) {
            scanLog.append("  HBM: Supported\n");
        } else {
            markDetected("High Brightness Mode Unavailable");
            scanLog.append("  -> ISSUE: HBM path not found\n");
        }
        scanLog.append("\n");
    }

    private void checkCharging() {
        scanLog.append("[Charging]\n");
        String[] chargePaths = {
            "/sys/class/power_supply/battery/current_max",
            "/sys/class/power_supply/battery/constant_charge_current_max",
            "/sys/class/power_supply/battery/input_current_max"
        };
        boolean found = false;
        for (String p : chargePaths) {
            File f = new File(p);
            if (f.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    int val = Integer.parseInt(br.readLine().trim());
                    br.close();
                    int ma = val / 1000;
                    scanLog.append("  Max current: ").append(ma).append(" mA\n");
                    if (ma > 0 && ma < 2000) {
                        markDetected("Charging Speed Below Capability");
                        scanLog.append("  -> ISSUE: Charging may be capped\n");
                    } else {
                        scanLog.append("  -> OK: Charging current appears normal\n");
                    }
                    found = true;
                    break;
                } catch (Exception ignored) {}
            }
        }
        if (!found) {
            scanLog.append("  Charging info not available via sysfs\n");
        }
        scanLog.append("\n");
    }

    private void checkOther() {
        scanLog.append("[Other]\n");
        
        // Vibration - universal paths
        String[] vibePaths = {
            "/sys/class/leds/vibrator/activate",
            "/sys/class/leds/vibrator/duration",
            "/sys/devices/virtual/timed_output/vibrator/enable",
            "/sys/class/timed_output/vibrator/enable"
        };
        boolean vibeFound = false;
        for (String p : vibePaths) {
            if (new File(p).exists()) { vibeFound = true; break; }
        }
        if (vibeFound) {
            scanLog.append("  Vibration control: Detected\n");
        } else {
            markDetected("Vibration Intensity Low");
            scanLog.append("  -> ISSUE: Vibration path not found\n");
        }

        // System info via getprop (universal, no permission issues)
        try {
            Process p = Runtime.getRuntime().exec("getprop ro.build.version.release");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String ver = br.readLine(); br.close();
            scanLog.append("  Android version: ").append(ver != null ? ver.trim() : "?").append("\n");
            
            p = Runtime.getRuntime().exec("getprop ro.sf.lcd_density");
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String density = br.readLine(); br.close();
            scanLog.append("  Display density: ").append(density != null ? density.trim() : "?").append(" DPI\n");
        } catch (Exception ignored) {}
        scanLog.append("\n");
    }

    private void markDetected(String issueName) {
        for (Issue issue : issues) {
            if (issue.name.equals(issueName)) {
                issue.detected = true;
            }
        }
    }

    public List<Issue> getDetectedIssues() {
        List<Issue> detected = new ArrayList<>();
        for (Issue issue : issues) {
            if (issue.detected) {
                detected.add(issue);
            }
        }
        return detected;
    }

    public String getScanLog() {
        return scanLog.toString();
    }

    public List<Issue> getAllIssues() {
        return issues;
    }
}
