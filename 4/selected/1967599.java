package com.duniptech.soa.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class Util {

    public static boolean debug = true;

    public static final String UPLOAD_SERVICE = "/axis2/services/Upload";

    public static final String COMPILE_SERVICE = "/axis2/services/Compile";

    public static final String SIMULATION_SERVICE = "/axis2/services/Simulation";

    public static final String RTSIMULATION_SERVICE = "/axis2/services/RTSimulation";

    public static final String JAVA_DIR = ".." + File.separator + "temp" + File.separator + "MicroSimSoaJavaSrc";

    public static final String CLASSES_DIR = ".." + File.separator + "temp" + File.separator + "MicroSimSoaJavaCls";

    public static final String LIB_DIR = ".." + File.separator + "temp" + File.separator + "MicroSimSoaJavaLib";

    public static final String LOG_DIR = ".." + File.separator + "temp" + File.separator + "MicroSimSoaJavaLog";

    public static final String CLASSPATH_SEPARATOR = ":";

    public static Object fromByteArray(byte[] content) {
        Object object = null;
        ByteArrayInputStream i = new ByteArrayInputStream(content);
        try {
            ObjectInputStream in = new ObjectInputStream(i);
            object = in.readObject();
            in.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return object;
    }

    public static byte[] toByteArray(Object obj) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(o);
            out.writeObject(obj);
            out.flush();
            out.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return o.toByteArray();
    }

    public static void save(String packageName, ArrayList<byte[]> fileContents, ArrayList<String> fileNames) throws Exception {
        String dirBase = Util.JAVA_DIR + File.separator + packageName;
        File packageDir = new File(dirBase);
        if (!packageDir.exists()) {
            boolean created = packageDir.mkdir();
            if (!created) {
                File currentPath = new File(".");
                throw new Exception("Directory " + packageName + " could not be created. Current directory: " + currentPath.getAbsolutePath());
            }
        }
        for (int i = 0; i < fileContents.size(); i++) {
            File file = new File(Util.JAVA_DIR + File.separator + fileNames.get(i));
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fileContents.get(i));
            fos.flush();
            fos.close();
        }
        for (int i = 0; i < fileNames.size(); i++) {
            File fileSrc = new File(Util.JAVA_DIR + File.separator + fileNames.get(i));
            File fileDst = new File(dirBase + File.separator + fileNames.get(i));
            BufferedReader reader = new BufferedReader(new FileReader(fileSrc));
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileDst));
            writer.append("package " + packageName + ";\n");
            String line = "";
            while ((line = reader.readLine()) != null) writer.append(line + "\n");
            writer.flush();
            writer.close();
            reader.close();
        }
    }

    public static String compile(String packageName, ArrayList<String> fileNames) throws Exception {
        ArrayList<String> filePaths = new ArrayList<String>();
        for (String fileName : fileNames) filePaths.add(Util.JAVA_DIR + File.separator + packageName + File.separator + fileName);
        MyCompiler compiler = new MyCompiler();
        if (!compiler.compile(filePaths)) throw new Exception(compiler.getOutput());
        return compiler.getOutput();
    }

    public static void setClassPath() throws Exception {
        File clsDir = new File(CLASSES_DIR);
        ArrayList<File> libFiles = new ArrayList<File>();
        libFiles.add(new File(LIB_DIR + File.separator + "devsjava.jar"));
        libFiles.add(new File(LIB_DIR + File.separator + "microSimJava.jar"));
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { clsDir.toURI().toURL() });
            for (File libFile : libFiles) method.invoke(sysloader, new Object[] { libFile.toURI().toURL() });
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Exception("Error, could not add URL to system classloader");
        }
    }

    public static void addClassPath(String classesDir) throws Exception {
        File clsDir = new File(classesDir);
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { clsDir.toURI().toURL() });
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Exception("Error, could not add URL to system classloader");
        }
    }
}
