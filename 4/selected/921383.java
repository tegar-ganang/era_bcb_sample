package com.tikal.delivery.patchtool.taskdefs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;
import com.sun.javaws.jardiff.JarDiffPatcher;
import com.tikal.delivery.patchtool.util.PatchConfigXML;
import com.tikal.delivery.patchtool.util.PatchRandom;
import com.tikal.delivery.patchtool.util.PatchReportXML;

/**
 * @author itaio
 * 
 * To change this generated comment go to Window>Preferences>Java>Code
 * Generation>Code and Comments
 */
public class Patch extends Task {

    protected Map map = new HashMap();

    protected PatchReportXML report = null;

    protected PatchConfigXML conf = null;

    protected File outputDir = null, tempDir = null;

    protected File configFile = null;

    protected File baseDir = null;

    protected String os;

    protected String name;

    File out = null;

    String[] excludesDirs, includesDirs;

    protected File workingDir;

    protected static final String PATCH_COMPARE_FILE = "PatchCompare.txt";

    public static final String EAR_EXT = ".ear";

    public static final String SAR_EXT = ".sar";

    public static final String WAR_EXT = ".war";

    public static final String JAR_EXT = ".jar";

    public static final String DIFF_EXT = ".diff";

    public static final String DEL_EXT = ".DEL";

    public static final String PROP_EXT = ".properties";

    public static final String PATCH_OUTDIR = "patch";

    public static final String PATCH_SUFFIX = "zip";

    public static final String BACKUP_OUTDIR = "backup";

    public static final String APPLY_OUTDIR = "apply";

    public static final String OS_TYPE_KEY = "os=";

    public static final String[] DEFAULT_EXCLUDES = new String[] { PatchReportXML.PATCH_REPORT_FILE };

    protected void loadConfiguration() {
        if (configFile == null || !configFile.exists()) conf = new PatchConfigXML(); else conf = new PatchConfigXML(configFile.getAbsolutePath());
    }

    protected void createReport() {
        createReport("");
    }

    protected void createReport(String type) {
        report = new PatchReportXML(workingDir.getAbsolutePath() + File.separator + type + PatchReportXML.PATCH_REPORT_FILE);
    }

    protected void saveReport() {
        this.report.save();
    }

    public Patch() {
        String tempPath = System.getProperty("java.io.tmpdir");
        tempDir = new File(tempPath, "patch" + System.currentTimeMillis());
        tempDir.mkdir();
    }

    protected void copyToPatch(File src, File dest) {
        try {
            FileUtils.newFileUtils().copyFile(src, dest);
            String srcRelativePath = src.getAbsolutePath().replace(baseDir.getAbsolutePath(), "");
            String path = dest.getCanonicalPath().substring(outputDir.getAbsolutePath().length() + 1).replace('\\', '/');
            String action = conf.getFileAction(srcRelativePath);
            String comment = "";
            if ((dest.getName().toUpperCase().indexOf("EXE") > -1) || (dest.getName().toUpperCase().indexOf("BAT") > -1)) {
                comment = ("NEED TO CREATE LINUX PATCH" + "\n");
            } else if ((dest.getName().toUpperCase().indexOf(".SO") > -1) || (dest.getName().toUpperCase().indexOf("SH") > -1) || dest.getName().toUpperCase().indexOf(".") < 0) {
                comment = ("NEED TO CREATE WINDOWS PATCH" + "\n");
            }
            this.report.reportFile(path, action, comment);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * check if in the diff file there is just the Index.jd(meaning no diff)
	 * 
	 * @param f
	 * @return boolean
	 */
    public boolean realDiff(File f) {
        InputStream inFrom = null;
        JarInputStream jin = null;
        try {
            int count = 0;
            inFrom = new FileInputStream(f);
            jin = new JarInputStream(inFrom);
            ZipEntry entry = jin.getNextEntry();
            while (entry != null) {
                String fileName = entry.getName();
                count++;
                if (count > 1) return true;
                if (!fileName.equals("META-INF/INDEX.JD")) return true;
                entry = jin.getNextEntry();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException io) {
            throw new RuntimeException(io);
        } finally {
            try {
                inFrom.close();
                jin.close();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
        return false;
    }

    /**
	 * @return String[]
	 */
    public String[] getExcludesDirs() {
        return excludesDirs;
    }

    /**
	 * Sets the excludesDirs.
	 * 
	 * @param excludesDirs
	 *            The excludesDirs to set
	 */
    public void setExcludesDirs(String[] excludesDirs) {
        this.excludesDirs = excludesDirs;
    }

    /**
	 * @return String[]
	 */
    public String[] getIncludesDirs() {
        return includesDirs;
    }

    /**
	 * Sets the includesDirs.
	 * 
	 * @param includesDirs
	 *            The includesDirs to set
	 */
    public void setIncludesDirs(String[] includesDirs) {
        this.includesDirs = includesDirs;
    }

    /**
	 * byte compare of files.
	 * 
	 * @param srcFile
	 * @param targetFile
	 * @return boolean
	 */
    public boolean isDiff(File srcFile, File targetFile) {
        int buff_size = 1024;
        byte srcBuf[] = new byte[1024], targetBuf[] = new byte[1024];
        if (srcFile.length() != targetFile.length()) return true;
        try {
            BufferedInputStream targetStream = new BufferedInputStream(new FileInputStream(targetFile));
            BufferedInputStream srcStream = new BufferedInputStream(new FileInputStream(srcFile));
            while (srcStream.read(srcBuf, 0, buff_size) != -1 && targetStream.read(targetBuf, 0, buff_size) != -1) {
                for (int i = 0; i < buff_size; i++) {
                    if (srcBuf[i] != targetBuf[i]) return true;
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        return false;
    }

    private static boolean isLinux() {
        return (System.getProperties().getProperty("os.name").toLowerCase().indexOf("windows") < 0);
    }

    /**
	 * remove from the INDEX.JD the move entry of exists package create the
	 * patch with the new INDEX.JD and apply it. should be call in ZipException
	 * "duplicate entry:"
	 * 
	 * @param ze
	 * @param src
	 * @param backUpPath
	 * @param fileoutputstream1
	 * @throws IOException
	 */
    protected void removeEntryJD(ZipException ze, File src, String backUpPath, FileOutputStream fileoutputstream1) throws IOException {
        Enumeration enumer;
        String line;
        try {
            File newZip = new File(tempDir, src.getName() + System.currentTimeMillis() + PatchRandom.getRandom());
            JarOutputStream jout = new JarOutputStream(new FileOutputStream(newZip.getAbsoluteFile()));
            ZipFile oldZip = new ZipFile(src.getAbsolutePath());
            enumer = oldZip.entries();
            while (enumer.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) enumer.nextElement();
                InputStream is = oldZip.getInputStream(zipEntry);
                if (zipEntry.getName().equals("META-INF/INDEX.JD")) {
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    ZipEntry newZipEntry = new ZipEntry(zipEntry.getName());
                    jout.putNextEntry(newZipEntry);
                    while ((line = br.readLine()) != null) {
                        if (line.indexOf(ze.getMessage().substring("duplicate entry:".length())) > -1) continue;
                        if (line.startsWith("move")) {
                            List list1 = getSubpaths(line.substring("move".length()));
                            if (hasEntry(oldZip, (String) list1.get(1)) && !((String) list1.get(1)).equals("META-INF")) {
                                continue;
                            }
                        }
                        jout.write(line.getBytes());
                        jout.write(Character.LINE_SEPARATOR);
                    }
                    jout.closeEntry();
                    isr.close();
                    br.close();
                } else {
                    jout.putNextEntry(zipEntry);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) jout.write(buffer, 0, len);
                    jout.closeEntry();
                    is.close();
                }
            }
            jout.close();
            new JarDiffPatcher().applyPatch(null, backUpPath, newZip.getAbsolutePath(), fileoutputstream1);
        } catch (IOException e2) {
            throw e2;
        }
    }

    private List getSubpaths(String s) {
        int i = 0;
        int j = s.length();
        ArrayList arraylist = new ArrayList();
        while (i < j) {
            while (i < j && Character.isWhitespace(s.charAt(i))) i++;
            if (i < j) {
                int k = i;
                int l = k;
                String s1 = null;
                for (; i < j; i++) {
                    char c = s.charAt(i);
                    if (c == '\\' && i + 1 < j && s.charAt(i + 1) == ' ') {
                        if (s1 == null) s1 = s.substring(l, i); else s1 = s1 + s.substring(l, i);
                        l = ++i;
                        continue;
                    }
                    if (Character.isWhitespace(c)) break;
                }
                if (l != i) if (s1 == null) s1 = s.substring(l, i); else s1 = s1 + s.substring(l, i);
                arraylist.add(s1);
            }
        }
        return arraylist;
    }

    private boolean hasEntry(ZipFile oldZip, String name) {
        Enumeration en = oldZip.entries();
        while (en.hasMoreElements()) {
            if (((ZipEntry) en.nextElement()).getName().indexOf(name) != -1) return true;
        }
        return false;
    }

    /**
	 * @return Returns the output.
	 */
    public File getOutputDir() {
        return outputDir;
    }

    /**
	 * @param output
	 *            The output to set.
	 */
    protected void setOutputDir(File output) {
        this.outputDir = output;
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setConfigFile(File PatchConfigXMLFile) {
        this.configFile = PatchConfigXMLFile;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File basePath) {
        this.baseDir = basePath;
    }

    /**
	 * 
	 */
    protected void validate() throws BuildException {
        if (workingDir == null) throw new BuildException("missing working directory");
    }

    /**
	 * @return Returns the os.
	 */
    public String getOs() {
        return os;
    }

    /**
	 * @param os
	 *            The os to set.
	 */
    public void setOs(String os) {
        this.os = os;
    }

    public String getName() {
        return name;
    }

    public void setName(String patchName) {
        this.name = patchName;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    public String getDefaultPatchFile() {
        return workingDir.getPath() + File.separator + this.getName() + "." + PATCH_SUFFIX;
    }

    public String getDefaultOutputDir() {
        return workingDir.getPath() + File.separator + this.getName();
    }

    public String getDefaultPatchOutputDir() {
        return workingDir.getPath() + File.separator + PATCH_OUTDIR;
    }

    public String getDefaultBackupDir() {
        return workingDir.getPath() + File.separator + BACKUP_OUTDIR;
    }
}
