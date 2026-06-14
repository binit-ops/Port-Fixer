package com.realme.portdoctor;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class ModuleBuilder {

    public static File buildModule(List<FixEngine.Fix> fixes, String outputPath) throws Exception {
        File moduleDir = new File("/sdcard/PortDoctor_Module");
        deleteRecursive(moduleDir);
        moduleDir.mkdirs();

        // module.prop
        FileWriter prop = new FileWriter(new File(moduleDir, "module.prop"));
        prop.write("id=port_doctor_fix\n");
        prop.write("name=Port Doctor Fix Pack\n");
        prop.write("version=1.0\n");
        prop.write("versionCode=1\n");
        prop.write("author=Port Doctor\n");
        prop.write("description=Generated fix pack with " + fixes.size() + " fixes\n");
        prop.close();

        // service.sh
        FileWriter service = new FileWriter(new File(moduleDir, "service.sh"));
        service.write("#!/system/bin/sh\n");
        service.write("# Port Doctor Generated Fix Pack\n");
        service.write("# Generated: " + new Date() + "\n\n");

        for (FixEngine.Fix fix : fixes) {
            service.write("#### " + fix.name + " ####\n");
            service.write(fix.scriptContent);
            service.write("\n");
        }

        service.write("\n# All fixes applied\n");
        service.close();
        new File(moduleDir, "service.sh").setExecutable(true);

        // post-fs-data.sh
        FileWriter postfs = new FileWriter(new File(moduleDir, "post-fs-data.sh"));
        postfs.write("#!/system/bin/sh\n");
        postfs.write("# Early boot fixes\n");
        postfs.write("max_path=$(ls /sys/class/backlight/*/max_brightness 2>/dev/null | head -1)\n");
        postfs.write("if [ -f \"$max_path\" ]; then\n");
        postfs.write("    echo 4095 > \"$max_path\"\n");
        postfs.write("fi\n");
        postfs.close();
        new File(moduleDir, "post-fs-data.sh").setExecutable(true);

        // README
        FileWriter readme = new FileWriter(new File(moduleDir, "README.txt"));
        readme.write("Port Doctor Fix Pack\n");
        readme.write("====================\n\n");
        readme.write("Applied Fixes:\n");
        for (int i = 0; i < fixes.size(); i++) {
            readme.write((i + 1) + ". " + fixes.get(i).name + "\n");
            readme.write("   " + fixes.get(i).description + "\n\n");
        }
        readme.write("Flash this module in Magisk and reboot.\n");
        readme.close();

        // Zip
        File outputFile = new File(outputPath);
        FileOutputStream fos = new FileOutputStream(outputFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        for (File file : moduleDir.listFiles()) {
            ZipEntry entry = new ZipEntry(file.getName());
            zos.putNextEntry(entry);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            fis.close();
            zos.closeEntry();
        }

        zos.close();
        fos.close();

        return outputFile;
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }
}
