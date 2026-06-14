package com.realme.portdoctor;

import java.io.*;
import java.util.*;

public class FixEngine {

    public static class Fix {
        public String name;
        public String scriptContent;
        public String description;

        public Fix(String name, String script, String desc) {
            this.name = name;
            this.scriptContent = script;
            this.description = desc;
        }
    }

    private PortScanner scanner;

    public FixEngine(PortScanner scanner) {
        this.scanner = scanner;
    }

    public List<Fix> generateFixes() {
        List<Fix> fixes = new ArrayList<>();
        List<PortScanner.Issue> issues = scanner.getDetectedIssues();

        for (PortScanner.Issue issue : issues) {
            Fix fix = generateFixForIssue(issue);
            if (fix != null) {
                fixes.add(fix);
            }
        }

        return fixes;
    }

    private Fix generateFixForIssue(PortScanner.Issue issue) {
        switch (issue.fixType) {
            case "brightness":
                return generateBrightnessFix();
            case "audio":
                return generateAudioFix();
            case "sensor":
                return generateSensorFix();
            case "camera":
                return generateCameraFix();
            case "display":
                return generateDisplayFix();
            case "thermal":
                return generateThermalFix();
            default:
                return null;
        }
    }

    private Fix generateBrightnessFix() {
        StringBuilder script = new StringBuilder();
        script.append("#!/system/bin/sh\n");
        script.append("# Brightness Unlock Fix - Port Doctor\n\n");
        script.append("backlight_path=$(ls /sys/class/backlight/*/brightness 2>/dev/null | head -1)\n");
        script.append("max_path=$(ls /sys/class/backlight/*/max_brightness 2>/dev/null | head -1)\n\n");
        script.append("if [ -f \"$max_path\" ]; then\n");
        script.append("    real_max=$(cat \"$max_path\")\n");
        script.append("    echo \"$real_max\" > \"$max_path\"\n");
        script.append("    state_file=\"/data/local/tmp/portdoctor_brightness\"\n");
        script.append("    echo \"0\" > \"$state_file\"\n");
        script.append("    while true; do\n");
        script.append("        current=$(cat \"$backlight_path\")\n");
        script.append("        last_scaled=$(cat \"$state_file\")\n");
        script.append("        if [ \"$current\" -le 2047 ] && [ \"$current\" -ne \"$last_scaled\" ]; then\n");
        script.append("            scaled=$(( current * real_max / 2047 ))\n");
        script.append("            echo \"$scaled\" > \"$backlight_path\"\n");
        script.append("            echo \"$scaled\" > \"$state_file\"\n");
        script.append("        fi\n");
        script.append("        sleep 0.1\n");
        script.append("    done &\n");
        script.append("fi\n");

        return new Fix("Brightness Unlock", script.toString(),
            "Unlocks full panel brightness range and fixes dim screen");
    }

    private Fix generateAudioFix() {
        StringBuilder script = new StringBuilder();
        script.append("#!/system/bin/sh\n");
        script.append("# Audio Gain Fix - Port Doctor\n\n");
        script.append("tinymix \"RX1 Digital Volume\" 90 2>/dev/null\n");
        script.append("tinymix \"RX2 Digital Volume\" 90 2>/dev/null\n");
        script.append("tinymix \"RX3 Digital Volume\" 90 2>/dev/null\n");
        script.append("tinymix \"Speaker Gain\" 5 2>/dev/null\n");
        script.append("tinymix \"Headphone Gain\" 3 2>/dev/null\n");

        return new Fix("Audio Booster", script.toString(),
            "Increases earpiece and speaker volume levels");
    }

    private Fix generateSensorFix() {
        StringBuilder script = new StringBuilder();
        script.append("#!/system/bin/sh\n");
        script.append("# Sensor Calibration Fix - Port Doctor\n\n");
        script.append("stop sensors 2>/dev/null\n");
        script.append("sleep 1\n");
        script.append("start sensors 2>/dev/null\n");
        script.append("if [ -f /sys/class/sensors/proximity_sensor/prox_cal ]; then\n");
        script.append("    echo 1 > /sys/class/sensors/proximity_sensor/prox_cal\n");
        script.append("fi\n");

        return new Fix("Sensor Fix", script.toString(),
            "Restarts sensors service and calibrates proximity sensor");
    }

    private Fix generateCameraFix() {
        StringBuilder script = new StringBuilder();
        script.append("#!/system/bin/sh\n");
        script.append("# Camera Library Fix - Port Doctor\n\n");
        script.append("chmod 644 /vendor/lib64/hw/camera.* 2>/dev/null\n");
        script.append("chmod 644 /vendor/lib/hw/camera.* 2>/dev/null\n");
        script.append("stop camera-provider-2-4 2>/dev/null\n");
        script.append("sleep 1\n");
        script.append("start camera-provider-2-4 2>/dev/null\n");

        return new Fix("Camera Fix", script.toString(),
            "Fixes camera HAL permissions and restarts camera service");
    }

    private Fix generateDisplayFix() {
        StringBuilder script = new StringBuilder();
        script.append("#!/system/bin/sh\n");
        script.append("# Display Fix - Port Doctor\n\n");
        script.append("if [ -f /sys/class/graphics/fb0/refresh_rate ]; then\n");
        script.append("    echo 120 > /sys/class/graphics/fb0/refresh_rate\n");
        script.append("fi\n");
        script.append("dc_mode=$(ls /sys/class/backlight/*/dc_mode 2>/dev/null | head -1)\n");
        script.append("if [ -f \"$dc_mode\" ]; then\n");
        script.append("    echo 1 > \"$dc_mode\"\n");
        script.append("fi\n");

        return new Fix("Display Fix", script.toString(),
            "Forces max refresh rate and enables DC dimming if available");
    }

    private Fix generateThermalFix() {
        StringBuilder script = new StringBuilder();
        script.append("#!/system/bin/sh\n");
        script.append("# Thermal & Charging Fix - Port Doctor\n\n");
        script.append("if [ -f /sys/class/power_supply/battery/current_max ]; then\n");
        script.append("    echo 3000000 > /sys/class/power_supply/battery/current_max\n");
        script.append("fi\n");

        return new Fix("Thermal & Charging Fix", script.toString(),
            "Increases charging current limit for faster charging");
    }
          }
