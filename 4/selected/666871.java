package org.ninjasoft.macpackager;

import java.io.*;
import java.util.*;

/**
 * Ant task that will create a Mac OS X application bundle.
 * This task expects a nested fileset of *.jar files
 * 
 * Requires: jar files (in nested fileset)
 *           Version - Application version
 *           InfoString - Name, version, copyright information
 *           Icon file - *.icns file containing application icon(s)
 *           VM Options - Optional things to pass to the VM (name in app menu, shiny metal look, etc)
 *           mainclass - main class
 *  
 * Derives:  BundleIdentifier: from mainclass + version
 *           
 * @author Brian Enigma
 */
public class MacPackager {

    private String appName = null;

    private String version = null;

    private String infoString = null;

    private String icon = null;

    private String vmOptions = null;

    private String mainClass = null;

    private InfoBuilder builder = new InfoBuilder();

    private String appLocation;

    private Vector jarFiles = new Vector();

    private static final String appExt = "";

    public void buildApplication() throws PackageException {
        validate();
        builder.setBundleName(this.appName);
        builder.setVersion(this.version);
        builder.setInfoString(this.infoString);
        builder.setVmOptions(this.vmOptions);
        builder.setMainClass(this.mainClass);
        for (Iterator i = this.jarFiles.iterator(); i.hasNext(); ) builder.addJarFile(i.next().toString());
        String target = "target/" + appName + appExt;
        if (new File(target).exists()) {
            System.out.println("Removing existing application " + target);
            deleteRecursive(target);
        }
        if (new File(target).exists()) throw new PackageException("Unable to delete " + target);
        mkdirs();
        copyFiles();
        createFiles();
        tagFolder();
        System.out.println("Application bundle ready");
    }

    private void deleteRecursive(String s) {
        if ((s == null) || (s.length() == 0) || s.equals(".") || s.equals("..")) return;
        File f = new File(s);
        if (f.isDirectory()) {
            String files[] = f.list();
            for (int i = 0; i < files.length; i++) deleteRecursive(s + "/" + files[i]);
            f.delete();
        } else {
            f.delete();
        }
    }

    /**
     * Create the target directories
     * @throws BuildException
     */
    private void mkdirs() throws PackageException {
        appLocation = "target/" + appName + appExt + "/Contents/MacOS";
        File d = new File(appLocation);
        if (!d.mkdirs()) throw new PackageException("Unable to create folder " + d.getPath());
        d = new File("target/" + appName + appExt + "/Contents/Resources/Java");
        if (!d.mkdirs()) throw new PackageException("Unable to create folder " + d.getPath());
    }

    private void copyFiles() throws PackageException {
        File s = new File("/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub");
        File d = new File("target/" + appName + appExt + "/Contents/MacOS/JavaApplicationStub");
        copy(s, d);
        String[] command = new String[] { "/bin/chmod", "0755", d.getPath() };
        try {
            Process p = Runtime.getRuntime().exec(command);
            int rc = p.waitFor();
            if (rc != 0) throw new PackageException("chmod of JavaApplicationStub returned error code " + rc);
        } catch (IOException ioe) {
            throw new PackageException("chmod of JavaApplicationStub threw an IOException");
        } catch (InterruptedException ie) {
            throw new PackageException("chmod of JavaApplicationStub threw an InterruptedException");
        }
        s = new File(icon);
        d = new File("target/" + appName + appExt + "/Contents/Resources/icon.icns");
        copy(s, d);
        for (Iterator i = this.jarFiles.iterator(); i.hasNext(); ) {
            s = new File(i.next().toString());
            d = new File("target/" + appName + appExt + "/Contents/Resources/Java/" + s.getName());
            copy(s, d);
        }
    }

    private void createFiles() throws PackageException {
        try {
            File f = new File("target/" + appName + appExt + "/Contents/PkgInfo");
            FileOutputStream out = new FileOutputStream(f);
            out.write("APPL????".getBytes());
            out.close();
        } catch (IOException e) {
            throw new PackageException("Problem creating PkgInfo file: " + e.toString());
        }
        try {
            File f = new File("target/" + appName + appExt + "/Contents/Info.plist");
            FileOutputStream out = new FileOutputStream(f);
            out.write(builder.getXml().getBytes());
            out.close();
        } catch (IOException e) {
            throw new PackageException("Problem creating Info.plist XML file: " + e.toString());
        }
    }

    /**
     * Copy a source file to a destination file
     * @param source
     * @param destination
     * @throws BuildException
     */
    private void copy(File source, File destination) throws PackageException {
        try {
            FileInputStream in = new FileInputStream(source);
            FileOutputStream out = new FileOutputStream(destination);
            byte[] buff = new byte[1024];
            int len;
            while ((len = in.read(buff)) > 0) out.write(buff, 0, len);
            in.close();
            out.close();
        } catch (IOException e) {
            throw new PackageException("Unable to copy " + source.getPath() + " to " + destination.getPath() + " :: " + e.toString());
        }
    }

    /**
     * Tag a folder as a bundle
     * @throws BuildException
     */
    private void tagFolder() throws PackageException {
        String[] command = new String[] { "/Developer/Tools/SetFile", "-a", "B", "target/" + appName + appExt };
        try {
            Process p = Runtime.getRuntime().exec(command);
            int rc = p.waitFor();
            if (rc != 0) throw new PackageException("chmod of JavaApplicationStub returned error code " + rc);
        } catch (IOException ioe) {
            throw new PackageException("chmod of JavaApplicationStub threw an IOException");
        } catch (InterruptedException ie) {
            throw new PackageException("chmod of JavaApplicationStub threw an InterruptedException");
        }
    }

    /**
     * Check that all of the required attributes have been set, source files exist, and
     * required executables exist.
     * 
     * @throws BuildException
     */
    public void validate() throws PackageException {
        if (appName == null) throw new PackageException("appName attribute required");
        if (version == null) throw new PackageException("version attribute required");
        if (infoString == null) throw new PackageException("infoString attribute required");
        if (icon == null) throw new PackageException("icon attribute required");
        if (vmOptions == null) vmOptions = "";
        if (mainClass == null) throw new PackageException("mainClass attribute required");
        if (jarFiles.size() == 0) throw new PackageException("There should be at least one jar file.  Use a nested FileSet.");
        File check = new File(icon);
        if (!check.exists()) throw new PackageException("Unable to locate icon file " + icon);
        check = new File("/bin/chmod");
        if (!check.exists()) throw new PackageException("Unable to locate /bin/chmod");
        check = new File("/Developer/Tools/SetFile");
        if (!check.exists()) throw new PackageException("/Developer/Tools/SetFile command does not exist.  Please install the OS X Developer Tools.");
    }

    public void addJar(String jar) {
        this.jarFiles.add(jar);
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setInfoString(String infoString) {
        this.infoString = infoString;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setVmOptions(String vmOptions) {
        this.vmOptions = vmOptions;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
