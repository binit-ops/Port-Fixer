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
      
