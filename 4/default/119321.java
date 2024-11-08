import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import javax.swing.JOptionPane;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Project {

    private Hashtable<String, ProjectInclude> includes;

    private boolean fromJar = false;

    private String srcFolder = null;

    private String lastSaved = null;

    public Project() {
        includes = new Hashtable<String, ProjectInclude>();
    }

    public Project(boolean fromJar) {
        this.fromJar = fromJar;
        includes = new Hashtable<String, ProjectInclude>();
    }

    public Project(String systemFile) {
        this();
        parseSystem(systemFile, null);
    }

    public Project(String systemFile, String srcFolder) {
        this();
        parseSystem(systemFile, srcFolder);
    }

    public boolean isFromJar() {
        return fromJar;
    }

    public Hashtable<String, ProjectInclude> getIncludes() {
        return includes;
    }

    public void parseSystem(String systemFile, String srcFolder) {
        this.srcFolder = srcFolder;
        try {
            new DependencyBuilder(systemFile, true, srcFolder).getDependencyTreeXML();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (JDOMException e1) {
            e1.printStackTrace();
        }
        File docFile = new File(System.getProperty("user.dir") + "\\DependencyTree.xml".replace('\\', File.separatorChar));
        ProjectInclude sysInclude = new ProjectInclude();
        sysInclude.name = systemFile.substring(systemFile.lastIndexOf(File.separator) + 1);
        sysInclude.path = systemFile.replace(srcFolder, "$SRC$");
        sysInclude.modified = new Date(new File(systemFile).lastModified());
        addFile(sysInclude);
        if (docFile.exists()) {
            try {
                FileInputStream fi = new FileInputStream(docFile);
                BufferedReader br = new BufferedReader(new InputStreamReader(fi));
                String line = "";
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.contains("<FBType Name=")) {
                        String temp = line.split("Type=\"")[1];
                        String name = temp.split("\"")[0];
                        temp = line.split("FileLocation=\"")[1];
                        String path = temp.split("\"")[0];
                        Date modified = new Date(new File(path).lastModified());
                        path = path.replace(srcFolder, "$SRC$");
                        addFile(new ProjectInclude(name, path, modified));
                        System.out.println(name);
                    }
                }
                br.close();
                fi.close();
            } catch (IOException e) {
                System.out.println("Unable to parse " + systemFile + ": " + e.getMessage());
            }
        }
    }

    private void addFile(ProjectInclude file) {
        if (!includes.containsKey(file.name)) includes.put(file.name, file); else if (!includes.get(file.name).path.equals(file.path)) {
            int num = 0;
            String newName = file.name + "_" + num;
            while (includes.containsKey(newName)) newName = file.name + "_" + ++num;
            includes.put(newName, file);
        }
    }

    public boolean exportFiles(String path, boolean jar) {
        boolean hasErrors = false;
        File outPath = new File(path);
        if (!outPath.exists()) outPath.mkdir();
        if (jar) {
            hasErrors = exportJar(path, srcFolder);
        } else {
            hasErrors = exportFiles(path, srcFolder);
        }
        return hasErrors;
    }

    private boolean exportFiles(String path, String root) {
        boolean hasErrors = false;
        StringBuilder text = new StringBuilder();
        FileInputStream fi;
        BufferedReader br;
        FileOutputStream fo;
        PrintStream ps;
        for (String reference : includes.keySet()) {
            ProjectInclude include = includes.get(reference).clone();
            String file = include.path;
            file.replace("$SRC", root);
            try {
                fi = new FileInputStream(file);
                br = new BufferedReader(new InputStreamReader(fi));
                fo = new FileOutputStream(path + File.separator + reference);
                ps = new PrintStream(fo);
                String line;
                while ((line = br.readLine()) != null) {
                    ps.println(line);
                }
                fo.close();
                fi.close();
            } catch (FileNotFoundException e) {
                System.out.println("Error copying file " + file + ": " + e.getMessage());
                hasErrors = true;
                continue;
            } catch (IOException e) {
                System.out.println("Error copying file " + file + ": " + e.getMessage());
                hasErrors = true;
                continue;
            }
            include.path = include.path.replace(root, "");
            text.append(include.toString() + "\n");
        }
        try {
            fo = new FileOutputStream(path + File.separator + "config.prj");
            ps = new PrintStream(fo);
            ps.print(text.toString());
            fo.close();
            lastSaved = path + File.separator + "config.prj";
        } catch (IOException e) {
            hasErrors = true;
        }
        return hasErrors;
    }

    private boolean exportJar(String path, String root) {
        boolean hasErrors = false;
        try {
            String outPath = path + File.separator + "project.jar";
            File jarFile = new File(outPath);
            if (jarFile.exists()) {
                int i = 1;
                File f = new File(path + File.separator + "project_" + i + ".jar");
                while (f.exists()) {
                    i++;
                    f = new File(path + File.separator + "project_" + i + ".jar");
                }
                JOptionPane.showMessageDialog(null, "Project archive exists. Saving as project_" + i + ".jar");
                outPath = f.getPath();
                jarFile = f;
            } else {
                jarFile.getParentFile().mkdirs();
            }
            FileOutputStream dest = new FileOutputStream(jarFile);
            JarOutputStream out = new JarOutputStream(new BufferedOutputStream(dest));
            FileInputStream fi = null;
            BufferedReader br = null;
            StringBuilder text = new StringBuilder();
            for (String reference : includes.keySet()) {
                ProjectInclude include = includes.get(reference).clone();
                String srcFile = include.path;
                String file = srcFile;
                if (file.contains("$SRC$")) {
                    file = file.substring(0, file.indexOf("$SRC$")) + root + file.substring(file.indexOf("$SRC$") + 5);
                }
                try {
                    fi = new FileInputStream(file);
                    br = new BufferedReader(new InputStreamReader(fi));
                    PrintStream ps = new PrintStream(out);
                    JarEntry entry = new JarEntry(reference);
                    out.putNextEntry(entry);
                    String line;
                    while ((line = br.readLine()) != null) {
                        ps.println(line);
                    }
                    br.close();
                    fi.close();
                } catch (FileNotFoundException e) {
                    System.out.println("Error copying file " + file + ": " + e.getMessage());
                    hasErrors = true;
                    continue;
                } catch (IOException e) {
                    System.out.println("Error copying file " + file + ": " + e.getMessage());
                    hasErrors = true;
                    continue;
                } finally {
                    br.close();
                    fi.close();
                }
                text.append(include.toString() + "\n");
            }
            try {
                JarEntry entry = new JarEntry("config.prj");
                out.putNextEntry(entry);
                PrintStream ps = new PrintStream(out);
                ps.print(text.toString());
                lastSaved = jarFile.getPath();
            } catch (IOException e) {
                hasErrors = true;
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hasErrors;
    }

    public static Project fromFile(String path) {
        String ext = path.substring(path.lastIndexOf("."));
        if (ext.equals(".jar")) {
            return fromJar(path);
        } else {
            File projFile = new File(path);
            if (projFile.exists()) {
                Project proj = new Project();
                FileInputStream fi;
                try {
                    fi = new FileInputStream(projFile);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fi));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        try {
                            proj.addFile(ProjectInclude.parse(line));
                        } catch (ParseException e) {
                            System.out.println("Error reading project file: " + line);
                        }
                    }
                    return proj;
                } catch (FileNotFoundException e) {
                    System.err.println("Error reading project file " + path + ": " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("Error reading project file " + path + ": " + e.getMessage());
                }
            }
        }
        return null;
    }

    private static Project fromJar(String path) {
        try {
            Project proj = new Project(true);
            FileInputStream fis = new FileInputStream(path);
            JarInputStream jis = new JarInputStream(new BufferedInputStream(fis));
            JarEntry entry;
            while ((entry = (JarEntry) jis.getNextEntry()) != null) {
                if (entry.getName().equals("config.prj")) {
                    System.out.println("Extracting: " + entry);
                    BufferedReader br = new BufferedReader(new InputStreamReader(jis));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        try {
                            proj.addFile(ProjectInclude.parse(line));
                        } catch (ParseException e) {
                            System.out.println("Error reading project file: " + line);
                        }
                    }
                    br.close();
                    jis.close();
                    fis.close();
                    return proj;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println("Jar does not contain config.prj file.");
        return null;
    }

    public static boolean importProject(String path, boolean overWrite, String srcFolder) {
        Project proj = fromFile(path);
        boolean hasErrors = false;
        if (srcFolder == null) srcFolder = System.getProperty("user.dir") + "\\src".replace('\\', File.separatorChar);
        if (proj.isFromJar()) {
            return importFromJar(proj, path, overWrite, srcFolder);
        } else {
            File projFile = new File(path);
            FileInputStream fi;
            BufferedReader br;
            FileOutputStream fo;
            PrintStream ps;
            for (String reference : proj.getIncludes().keySet()) {
                ProjectInclude include = proj.getIncludes().get(reference).clone();
                include.path = include.path.replace("$SRC$", srcFolder);
                int result = checkExisting(include);
                if (result == -1) {
                    return false;
                } else if (result == 0) {
                    continue;
                } else {
                    try {
                        fi = new FileInputStream(projFile.getParent().toString() + File.separator + reference);
                        br = new BufferedReader(new InputStreamReader(fi));
                        fo = new FileOutputStream(include.path);
                        ps = new PrintStream(fo);
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            if (!line.equals("null")) ps.println(line);
                        }
                        fo.close();
                        fi.close();
                        System.out.println(include.path);
                    } catch (Exception e) {
                        System.err.println("Read error: " + e.getMessage());
                        hasErrors = true;
                    }
                }
            }
        }
        return hasErrors;
    }

    /**
	 * Checks whether a file exists already in the source directory, and if it
	 * does, prompt the user whether to overwrite or not.
	 * 
	 * @param include
	 * @return An integer indicating the result. -1 = Abort operation. 0 = Don't
	 *         overwrite file. 1 = Proceed.
	 */
    private static int checkExisting(ProjectInclude include) {
        File file = new File(include.path);
        if (file.exists()) {
            Date fileModified = new Date(file.lastModified());
            int dateResult = fileModified.compareTo(include.modified);
            if (dateResult > 0) {
                int result = JOptionPane.showConfirmDialog(null, "The file " + include.path + " already exists in the source directory," + " and is newer than the one in the imported project. OK to " + "overwrite the newer file?\n\n" + "Source directory: " + fileModified.toString() + " (newer)\n" + "Project: " + include.modified.toString());
                if (result == JOptionPane.YES_OPTION) return 1; else if (result == JOptionPane.CANCEL_OPTION) return -1;
                return 0;
            } else if (dateResult < 0) {
                int result = JOptionPane.showConfirmDialog(null, "The file " + include.path + " already exists in the source directory," + " and is older than the one in the imported project. OK to " + "overwrite the older file?\n\n" + "Source directory: " + fileModified.toString() + "\n" + "Project: " + include.modified.toString() + " (newer)");
                if (result == JOptionPane.YES_OPTION) return 1; else if (result == JOptionPane.CANCEL_OPTION) return -1;
                return 0;
            }
        }
        return 1;
    }

    private static boolean importFromJar(Project proj, String path, boolean overWrite, String srcFolder) {
        boolean hasErrors = false;
        FileInputStream fis;
        try {
            FileOutputStream fo;
            PrintStream ps;
            fis = new FileInputStream(path);
            JarInputStream jis = new JarInputStream(new BufferedInputStream(fis));
            JarEntry entry;
            while ((entry = (JarEntry) jis.getNextEntry()) != null) {
                if (!entry.getName().equals("config.prj") && proj.getIncludes().containsKey(entry.getName())) {
                    ProjectInclude include = proj.getIncludes().get(entry.getName()).clone();
                    include.path = include.path.replace("$SRC$", srcFolder);
                    File refFile = new File(include.path);
                    int result = checkExisting(include);
                    if (result == -1) {
                        return false;
                    } else if (result == 0) {
                        continue;
                    } else {
                        try {
                            System.out.println("Extracting: " + entry + " to " + include.path);
                            File dirs = refFile.getParentFile();
                            if (!dirs.exists()) dirs.mkdirs();
                            BufferedReader br = new BufferedReader(new InputStreamReader(jis));
                            fo = new FileOutputStream(refFile);
                            ps = new PrintStream(fo);
                            String line = "";
                            while ((line = br.readLine()) != null) {
                                if (!line.equals("null")) ps.println(line);
                            }
                            ps.close();
                            fo.close();
                        } catch (Exception e) {
                            hasErrors = true;
                            System.err.println("Read error: " + e.getMessage());
                        }
                    }
                }
            }
            jis.close();
            fis.close();
            return hasErrors;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getLastSavedLocation() {
        return lastSaved;
    }

    public static void main(String[] args) {
        Project p = new Project(System.getProperty("user.dir") + "/src/ita/FLASHER_TESTR.sys".replace('/', File.separatorChar));
        p.exportFiles("proj".replace('/', File.separatorChar), true);
        Project.importProject("proj/project.jar".replace('/', File.separatorChar), true, null);
    }
}
