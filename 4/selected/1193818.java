package org.translator.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.translator.java.code.SourceFile;
import org.translator.java.eclipseproject.ProjectBuilder;
import org.translator.java.eclipseproject.ProjectFile;
import org.translator.java.manifest.ManifestBuilder;
import org.translator.java.manifest.ManifestFile;

/**
 *
 * @author jswank
 */
public class AppInventorProject {

    private final ArrayList<SourceFile> files = new ArrayList<SourceFile>();

    private ManifestFile manifest = null;

    private ProjectFile project = null;

    private final ArrayList<String> assets = new ArrayList<String>();

    private final HashMap<String, AppInventorScreen> screens = new HashMap<String, AppInventorScreen>();

    private String projectName = null;

    private File assetsDir;

    public AppInventorProject(File inputFile) throws IOException {
        load(inputFile);
    }

    private void load(File inputFile) throws IOException {
        InputStream inputStream = null;
        if (inputFile.getName().toLowerCase().endsWith(".zip")) {
            inputStream = new ZipInputStream(new FileInputStream(inputFile));
            load((ZipInputStream) inputStream);
        } else {
        }
        inputStream.close();
    }

    private void load(ZipInputStream inputStream) throws IOException {
        clear();
        assetsDir = File.createTempFile("assets", "tmp");
        assetsDir.deleteOnExit();
        if (!(assetsDir.delete())) {
            throw new IOException("Could not delete file: " + assetsDir.getAbsolutePath());
        }
        if (!(assetsDir.mkdir())) {
            throw new IOException("Could not create dir: " + assetsDir.getAbsolutePath());
        }
        ZipEntry ze = null;
        while ((ze = inputStream.getNextEntry()) != null) {
            String name = ze.getName();
            if (name.startsWith("assets")) {
                File outputFile = new File(assetsDir.getAbsoluteFile().toString() + File.separator + name);
                if (outputFile.equals(new File(assetsDir.getAbsolutePath().toString() + File.separator + "assets"))) continue;
                assets.add(name);
                saveAsset(name, inputStream);
            } else if (name.endsWith(".blk") || name.endsWith(".scm") || name.endsWith(".yail")) loadSourceFile(name, inputStream);
        }
        generateSource();
    }

    public void clear() {
        assets.clear();
        screens.clear();
        if (assetsDir != null && assetsDir.exists()) {
            assetsDir.delete();
        }
    }

    public void writeOutput(ZipOutputStream outputStream) {
    }

    public void writeOutput(String directory) throws IOException {
        File f = new File(directory);
        int i = 0;
        if (f.isDirectory()) {
            for (AppInventorScreen screen : screens.values()) {
                File screenFile = new File(getScreenFilePath(f.getAbsolutePath(), screen));
                screenFile.getParentFile().mkdirs();
                screenFile.createNewFile();
                FileWriter out = new FileWriter(screenFile);
                String initial = files.get(i).toString();
                Map<String, String> types = screen.getTypes();
                String[] lines = initial.split("\n");
                for (String key : types.keySet()) {
                    if (!key.trim().equals(screen.getName().trim())) {
                        String value = types.get(key);
                        boolean varFound = false;
                        boolean importFound = false;
                        for (String line : lines) {
                            if (line.matches("^\\s*(public|private)\\s+" + value + "\\s+" + key + "\\s*=.*;$")) varFound = true;
                            if (line.matches("^\\s*(public|private)\\s+" + value + "\\s+" + key + "\\s*;$")) varFound = true;
                            if (line.matches("^\\s*import\\s+.*" + value + "\\s*;$")) importFound = true;
                        }
                        if (!varFound) initial = initial.replaceFirst("(?s)(?<=\\{\n)", "\tprivate " + value + " " + key + ";\n");
                        if (!importFound) initial = initial.replaceFirst("(?=import)", "import com.google.devtools.simple.runtime.components.android." + value + ";\n");
                    }
                }
                out.write(initial);
                out.close();
                i++;
            }
            File manifestFile = new File(getManifestFilePath(f.getAbsolutePath(), manifest));
            manifestFile.getParentFile().mkdirs();
            manifestFile.createNewFile();
            FileWriter out = new FileWriter(manifestFile);
            out.write(manifest.toString());
            out.close();
            File projectFile = new File(getProjectFilePath(f.getAbsolutePath(), project));
            projectFile.getParentFile().mkdirs();
            projectFile.createNewFile();
            out = new FileWriter(projectFile);
            out.write(project.toString());
            out.close();
            String[] copyResourceFilenames = { "proguard.cfg", "project.properties", "libSimpleAndroidRuntime.jar", "\\.classpath", "res/drawable/icon.png", "\\.settings/org.eclipse.jdt.core.prefs" };
            for (String copyResourceFilename : copyResourceFilenames) {
                InputStream is = getClass().getResourceAsStream("/resources/" + copyResourceFilename.replace("\\.", ""));
                File outputFile = new File(f.getAbsoluteFile() + File.separator + copyResourceFilename.replace("\\.", "."));
                outputFile.getParentFile().mkdirs();
                OutputStream os = new FileOutputStream(outputFile);
                byte[] buf = new byte[1024];
                int readBytes;
                if (is == null) System.out.println("/resources/" + copyResourceFilename.replace("\\.", ""));
                if (os == null) System.out.println(f.getAbsolutePath() + File.separator + copyResourceFilename.replace("\\.", "."));
                while ((readBytes = is.read(buf)) > 0) {
                    os.write(buf, 0, readBytes);
                }
            }
            for (String assetName : assets) {
                InputStream is = new FileInputStream(new File(assetsDir.getAbsolutePath() + File.separator + assetName));
                File outputFile = new File(f.getAbsoluteFile() + File.separator + assetName);
                outputFile.getParentFile().mkdirs();
                OutputStream os = new FileOutputStream(outputFile);
                byte[] buf = new byte[1024];
                int readBytes;
                while ((readBytes = is.read(buf)) > 0) {
                    os.write(buf, 0, readBytes);
                }
            }
            File assetsOutput = new File(getAssetsFilePath(f.getAbsolutePath()));
            new File(assetsDir.getAbsoluteFile() + File.separator + "assets").renameTo(assetsOutput);
        }
    }

    private String getScreenFilePath(String prefix, AppInventorScreen screen) {
        StringBuilder builder = new StringBuilder(prefix);
        String s = File.separator;
        builder.append(s).append("src").append(s).append("org");
        builder.append(s).append(projectName.toLowerCase());
        builder.append(s).append(screen.getName()).append(".java");
        return builder.toString();
    }

    private String getAssetsFilePath(String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        String s = File.separator;
        builder.append(s).append("assets");
        return builder.toString();
    }

    private String getManifestFilePath(String prefix, ManifestFile m) {
        StringBuilder builder = new StringBuilder(prefix);
        String s = File.separator;
        builder.append(s).append(m.getFileName());
        return builder.toString();
    }

    private String getProjectFilePath(String prefix, ProjectFile p) {
        StringBuilder builder = new StringBuilder(prefix);
        String s = File.separator;
        builder.append(s).append(p.getFileName());
        return builder.toString();
    }

    private String getClasspathFilePath(String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        String s = File.separator;
        builder.append(s).append(".classpath");
        return builder.toString();
    }

    private String getLibSimplePath(String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        String s = File.separator;
        builder.append(s).append("libSimpleAndroid.jar");
        return builder.toString();
    }

    private String getProguardPath(String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        String s = File.separator;
        builder.append(s).append("proguard.cfg");
        return builder.toString();
    }

    private String getDefaultPropsPath(String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        String s = File.separator;
        builder.append(s).append("default.properties");
        return builder.toString();
    }

    private String getFolder(String path) {
        int lastSlash = path.lastIndexOf('/');
        return path.substring(path.lastIndexOf('/', lastSlash - 1) + 1, lastSlash);
    }

    private void loadSourceFile(String path, InputStream inputStream) throws IOException {
        projectName = getFolder(path);
        AppInventorScreen screen = screens.get(projectName);
        if (screen == null) screen = new AppInventorScreen(projectName);
        if (path.endsWith(".blk")) screen.loadBlocksFile(inputStream); else if (path.endsWith(".scm")) screen.loadComponentFile(inputStream); else if (path.endsWith(".yail")) screen.loadInterfaceFile(inputStream);
        screens.put(projectName, screen);
    }

    private void saveAsset(String name, InputStream is) throws IOException {
        File outputFile = new File(assetsDir.getAbsoluteFile().toString() + File.separator + name);
        outputFile.getParentFile().mkdirs();
        outputFile.createNewFile();
        OutputStream os = new FileOutputStream(outputFile);
        byte[] buf = new byte[1024];
        int readBytes;
        while ((readBytes = is.read(buf)) > 0) {
            os.write(buf, 0, readBytes);
        }
    }

    private void generateSource() {
        files.clear();
        for (AppInventorScreen screen : screens.values()) files.add(screen.generateJavaFile());
        manifest = ManifestBuilder.generateManifest(projectName, screens.values());
        project = ProjectBuilder.generateProject(projectName);
    }
}
