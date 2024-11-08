package zkthemer;

import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.Cli;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

/**
 * 
 * @author JoseLuis Casas
 *
 */
public class CreateTheme {

    private final Theme theme;

    private final HueFilter op;

    private final File zklocation;

    private final StringBuffer fileList;

    private final String[] ignoreFiles;

    private boolean isIgnore = true;

    public CreateTheme(String themeName, Color color, File zkLocation, String ignoreFiles) {
        this.theme = new Theme(themeName);
        this.op = new HueFilter(color);
        this.zklocation = zkLocation;
        fileList = new StringBuffer();
        if (ignoreFiles.equals("n")) isIgnore = false;
        this.ignoreFiles = ignoreFiles.split(",");
    }

    private void processDir(File dirWithZkJars) throws Exception {
        boolean noJars = true;
        for (File jar : dirWithZkJars.listFiles()) {
            if (jar.getName().toLowerCase().endsWith(".jar")) {
                processJarFile(jar);
                noJars = false;
            }
        }
        if (noJars) {
            throw new IllegalArgumentException("No jars found in zklib " + dirWithZkJars);
        }
    }

    private boolean isIgnoreFile(String fileName) {
        if (!isIgnore) return false;
        for (String ignoreFile : this.ignoreFiles) {
            if (!(fileName.indexOf(ignoreFile) < 0)) return true;
        }
        return false;
    }

    private void processJarFile(File file) throws Exception {
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            String entryName = entry.getName().toLowerCase();
            if (entryName.endsWith("png") || entryName.endsWith("gif")) {
                if (isIgnoreFile(entryName)) continue;
                ProcessImage proc = new ProcessImage(entryName, jarFile.getInputStream(entry), theme, op);
                proc.run();
            } else if (entryName.endsWith(".css.dsp")) {
                if (isIgnoreFile(entryName)) continue;
                ProcessCssDsp proc = new ProcessCssDsp(entryName, jarFile.getInputStream(entry), theme, op);
                proc.run();
                fileList.append(entryName.substring(entryName.lastIndexOf("/") + 1) + ",");
            } else if (entryName.endsWith(".css")) {
                if (isIgnoreFile(entryName)) continue;
                ProcessCssDsp proc = new ProcessCssDsp(entryName, jarFile.getInputStream(entry), theme, op);
                proc.run();
                fileList.append(entryName.substring(entryName.lastIndexOf("/") + 1) + ",");
            } else if (entryName.endsWith(".wcs")) {
                File outputFile = theme.relocateFile(entryName);
                outputFile.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(outputFile);
                byte[] buf = new byte[1024];
                InputStream in = jarFile.getInputStream(entry);
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
        }
        jarFile.close();
    }

    private void pack() throws Exception {
        ArrayList<File> filesToJar = new ArrayList<File>();
        _packRecurse(filesToJar, theme.getJarRootFile());
        createJarArchive(new File(theme.getName() + ".jar"), filesToJar, theme.getJarRootFile());
    }

    private void _packRecurse(List<File> filesToJar, File cwd) throws Exception {
        if (cwd.isDirectory()) {
            for (File child : cwd.listFiles()) {
                _packRecurse(filesToJar, child);
            }
        } else if (cwd.isFile()) {
            filesToJar.add(cwd);
        }
    }

    private void createJarArchive(File archiveFile, List<File> filesToBeJared, File base) throws Exception {
        FileOutputStream stream = new FileOutputStream(archiveFile);
        JarOutputStream out = new JarOutputStream(stream);
        for (File tobeJared : filesToBeJared) {
            if (tobeJared == null || !tobeJared.exists() || tobeJared.isDirectory()) continue;
            String entryName = tobeJared.getAbsolutePath().substring(base.getAbsolutePath().length() + 1).replace("\\", "/");
            JarEntry jarEntry = new JarEntry(entryName);
            jarEntry.setTime(tobeJared.lastModified());
            out.putNextEntry(jarEntry);
            FileInputStream in = new FileInputStream(tobeJared);
            IOUtils.copy(in, out);
            IOUtils.closeQuietly(in);
            out.closeEntry();
        }
        out.close();
        stream.close();
        System.out.println("Generated file: " + archiveFile);
    }

    private void copyThemeProviderClass() throws Exception {
        InputStream is = getClass().getResourceAsStream("/zkthemer/ThemeProvider.class");
        if (is == null) throw new RuntimeException("Cannot find ThemeProvider.class");
        File outFile = new File(theme.getJarRootFile(), "zkthemer/ThemeProvider.class");
        outFile.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(outFile);
        IOUtils.copy(is, out);
        out.close();
        FileUtils.writeStringToFile(new File(theme.getJarRootFile(), "zkthemer.properties"), "theme=" + theme.getName() + "\r\nfileList=" + fileList.deleteCharAt(fileList.length() - 1).toString());
    }

    public void run() throws Exception {
        FileUtils.deleteDirectory(theme.getJarRootFile());
        processDir(zklocation);
        copyThemeProviderClass();
        pack();
        FileUtils.deleteDirectory(theme.getJarRootFile());
    }

    public static void main(String[] args) throws Exception {
        Cli<CliOptions> cli = CliFactory.createCli(CliOptions.class);
        try {
            CliOptions opts = cli.parseArguments(args);
            String colorString = opts.getBaseColor();
            if (colorString.startsWith("#")) colorString = colorString.substring(1);
            if (!colorString.matches("^[a-fA-F0-9]{6,6}$")) {
                throw new IllegalArgumentException("Color should be in 6 hex digit format, for example: A4FFC0");
            }
            String themeNameRegExp = "^[\\w\\-]+$";
            if (!opts.getThemeName().matches(themeNameRegExp)) {
                throw new IllegalArgumentException("Theme names must be alphanumeric with no blanks, conforming to regexp: " + themeNameRegExp);
            }
            if (!opts.getZklib().isDirectory()) throw new IllegalArgumentException("zklib argument should be a directory" + opts.getZklib());
            Color color = new Color(Integer.parseInt(colorString, 16));
            CreateTheme createTheme = new CreateTheme(opts.getThemeName(), color, opts.getZklib(), opts.getIgnoreFiles());
            createTheme.run();
        } catch (ArgumentValidationException e) {
            System.err.println(e.getMessage());
            System.err.println(cli.getHelpMessage());
            System.exit(-1);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(cli.getHelpMessage());
            System.exit(-1);
        }
    }
}
