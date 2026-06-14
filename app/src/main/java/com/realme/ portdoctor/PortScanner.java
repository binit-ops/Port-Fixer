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
            "Panel supports higher brightness than ROM allows", "HIGH", "brightness"));
        issues.add(new Issue("Auto-Brightness Broken", 
            "Ambient sensor values don't match brightness curve", "MEDIUM", "brightness"));
        issues.add(new Issue("Speaker Gain Low", 
            "Audio mixer paths not optimized for your device", "MEDIUM", "audio"));
        issues.add(new Issue("Earpiece Volume Low", 
            "RX digital volume capped in mixer paths", "MEDIUM", "audio"));
        issues.add(new Issue("Proximity Sensor Broken", 
            "Screen doesn't turn off during calls", "HIGH", "sensor"));
        issues.add(new Issue("Auto-Rotation Broken", 
            "Accelerometer/gyro values not calibrated", "HIGH", "sensor"));
        issues.add(new Issue("Fingerprint Slow", 
            "Fingerprint HAL or libs from donor not optimized", "MEDIUM", "sensor"));
        issues.add(new Issue("Camera Quality Reduced", 
            "Camera HAL using wrong libs or processing pipeline", "HIGH", "camera"));
        issues.add(new Issue("Flashlight Dim/Flickering", 
            "LED current control mismatch", "LOW", "camera"));
        issues.add(new Issue("Refresh Rate Stuck", 
            "Display driver not reporting correct modes", "MEDIUM", "display"));
        issues.add(new Issue("HBM Not Working", 
            "High Brightness Mode trigger path wrong", "MEDIUM", "display"));
        issues.add(new Issue("DC Dimming Missing", 
            "Flicker-free dimming not enabled in kernel", "LOW", "display"));
        issues.add(new Issue("Charging Speed Capped", 
            "Charger driver values from donor don't match hardware", "MEDIUM", "thermal"));
        issues.add(new Issue("Thermal Throttle Aggressive", 
            "Thermal engine config from donor too conservative", "MEDIUM", "thermal"));
        issues.add(new Issue("Battery Drain High", 
            "Wakelocks from mismatched drivers", "MEDIUM", "thermal"));
        issues.add(new Issue("Vibration Too Weak", 
            "Haptic driver voltage not calibrated", "LOW", "other"));
        issues.add(new Issue("Status Bar Padding Wrong", 
            "Overlay values from donor device don't match", "LOW", "other"));
    }

    public List<Issue> scanAll() {
        scanLog.setLength(0);
        scanLog.append("=== Port Doctor Scan ===\n");
        scanLog.append("Device: ").append(currentDevice).append("\n");
        if (donorDevice != null && !donorDevice.isEmpty()) {
            scanLog.append("Donor: ").append(donorDevice).append("\n");
        }
        scanLog.append("Time: ").append(new Date()).append("\n\n");

        checkBrightness();
        checkAudio();
        checkSensors();
        checkCamera();
        checkDisplay();
        checkThermal();
        checkOther();

        scanLog.append("\n=== Scan Complete ===\n");
        return getDetectedIssues();
    }

    private void checkBrightness() {
        try {
            File blDir = new File("/sys/class/backlight/");
            File[] panels = blDir.listFiles();
            if (panels != null && panels.length > 0) {
                String maxPath = panels[0].getAbsolutePath() + "/max_brightness";
                String brightPath = panels[0].getAbsolutePath() + "/brightness";
                
                BufferedReader br = new BufferedReader(new FileReader(maxPath));
                int maxBright = Integer.parseInt(br.readLine().trim());
                br.close();
                
                br = new BufferedReader(new FileReader(brightPath));
                int currentBright = Integer.parseInt(br.readLine().trim());
                br.close();

                scanLog.append("[Brightness] Max: ").append(maxBright)
                    .append(", Current: ").append(currentBright).append("\n");

                if (maxBright > 255 && currentBright <= 255) {
                    markDetected("Brightness Range Mismatch");
                    scanLog.append("  WARNING: Panel supports ").append(maxBright)
                        .append(" but ROM caps at 255\n");
                }

                File hbmPath = new File(panels[0].getAbsolutePath() + "/hbm_mode");
                if (!hbmPath.exists()) {
                    markDetected("HBM Not Working");
                } else {
                    scanLog.append("  HBM mode supported\n");
                }
            } else {
                markDetected("Brightness Range Mismatch");
                scanLog.append("  ERROR: No backlight panel found!\n");
            }
        } catch (Exception e) {
            scanLog.append("[Brightness] Error: ").append(e.getMessage()).append("\n");
        }
    }

    private void checkAudio() {
        try {
            File mixerDir = new File("/vendor/etc/");
            File[] mixerFiles = mixerDir.listFiles((dir, name) -> 
                name.contains("mixer_path") && name.endsWith(".xml"));
            
            if (mixerFiles != null && mixerFiles.length > 0) {
                scanLog.append("[Audio] Found ").append(mixerFiles.length)
                    .append(" mixer path files\n");
                
                for (File f : mixerFiles) {
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) content.append(line);
                    br.close();
                    
                    if (content.toString().contains("RX1 Digital Volume") && 
                        content.toString().contains("value=\"84\"")) {
                        markDetected("Earpiece Volume Low");
                        scanLog.append("  WARNING: Earpiece volume capped at 84 (default)\n");
                        break;
                    }
                }
            } else {
                markDetected("Speaker Gain Low");
                scanLog.append("  ERROR: No mixer paths found\n");
            }
        } catch (Exception e) {
            scanLog.append("[Audio] Error: ").append(e.getMessage()).append("\n");
        }
    }

    private void checkSensors() {
        try {
            File proxFile = new File("/sys/class/sensors/proximity_sensor/raw_data");
            if (!proxFile.exists()) {
                proxFile = new File("/sys/devices/virtual/sensors/proximity_sensor/prox_raw");
            }
            if (!proxFile.exists()) {
                markDetected("Proximity Sensor Broken");
                scanLog.append("[Sensors] WARNING: Proximity sensor not found\n");
            } else {
                scanLog.append("[Sensors] Proximity sensor detected\n");
            }

            File accelFile = new File("/sys/class/sensors/accelerometer_sensor/raw_data");
            if (!accelFile.exists()) {
                markDetected("Auto-Rotation Broken");
                scanLog.append("[Sensors] WARNING: Accelerometer not found\n");
            } else {
                scanLog.append("[Sensors] Accelerometer detected\n");
            }
        } catch (Exception e) {
            scanLog.append("[Sensors] Error: ").append(e.getMessage()).append("\n");
        }
    }

    private void checkCamera() {
        try {
            File cameraDir = new File("/vendor/lib64/");
            File[] cameraLibs = cameraDir.listFiles((dir, name) -> 
                name.contains("camera") && name.endsWith(".so"));
            
            if (cameraLibs == null || cameraLibs.length < 5) {
                markDetected("Camera Quality Reduced");
                scanLog.append("[Camera] WARNING: Camera libraries may be incomplete\n");
            } else {
                scanLog.append("[Camera] ").append(cameraLibs.length)
                    .append(" camera libraries found\n");
            }

            File flashFile = new File("/sys/class/leds/flashlight/brightness");
            if (!flashFile.exists()) {
                flashFile = new File("/sys/class/leds/torch-light/brightness");
            }
            if (!flashFile.exists()) {
                markDetected("Flashlight Dim/Flickering");
                scanLog.append("[Camera] WARNING: Flashlight control not found\n");
            }
        } catch (Exception e) {
            scanLog.append("[Camera] Error: ").append(e.getMessage()).append("\n");
        }
    }

    private void checkDisplay() {
        try {
            File refreshFile = new File("/sys/class/graphics/fb0/refresh_rate");
            if (refreshFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(refreshFile));
                String rate = br.readLine().trim();
                br.close();
                scanLog.append("[Display] Current refresh rate: ").append(rate).append("Hz\n");
            } else {
                markDetected("Refresh Rate Stuck");
                scanLog.append("[Display] WARNING: Refresh rate control not found\n");
            }

            File dcDimming = new File("/sys/class/backlight/panel0-backlight/dc_mode");
            if (!dcDimming.exists()) {
                dcDimming = new File("/sys/class/backlight/backlight/dc_mode");
            }
            if (!dcDimming.exists()) {
                markDetected("DC Dimming Missing");
                scanLog.append("[Display] WARNING: DC dimming not available\n");
            } else {
                scanLog.append("[Display] DC dimming supported\n");
            }
        } catch (Exception e) {
            scanLog.append("[Display] Error: ").append(e.getMessage()).append("\n");
        }
    }

    private void checkThermal() {
        try {
            File thermalDir = new File("/sys/class/thermal/");
            File[] zones = thermalDir.listFiles((dir, name) -> 
                name.startsWith("thermal_zone"));
            
            if (zones != null) {
                scanLog.append("[Thermal] Found ").append(zones.length).append(" zones\n");
            }

            File chargingFile = new File("/sys/class/power_supply/battery/current_max");
            if (chargingFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(chargingFile));
                int currentMax = Integer.parseInt(br.readLine().trim());
                br.close();
                if (currentMax < 2000000) {
                    markDetected("Charging Speed Capped");
                    scanLog.append("[Thermal] WARNING: Max current: ").append(currentMax / 1000)
                        .append("mA (may be capped)\n");
                }
            }
        } catch (Exception e) {
            scanLog.append("[Thermal] Error: ").append(e.getMessage()).append("\n");
        }
    }

    private void checkOther() {
        try {
            File vibratorFile = new File("/sys/class/leds/vibrator/activate");
            if (!vibratorFile.exists()) {
                markDetected("Vibration Too Weak");
                scanLog.append("[Other] WARNING: Vibration control not found\n");
            }

            File buildProp = new File("/system/build.prop");
            if (buildProp.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(buildProp));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("ro.sf.lcd_density")) {
                        scanLog.append("[Other] LCD Density: ").append(line).append("\n");
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            scanLog.append("[Other] Error: ").append(e.getMessage()).append("\n");
        }
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
