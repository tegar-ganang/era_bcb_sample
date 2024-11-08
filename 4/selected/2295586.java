package neon.features.files;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import neon.util.trees.PathTree;
import java.nio.channels.*;

public class FileSystem {

    private HashMap<String, String> jars;

    private String temp;

    private PathTree<String, String> files;

    private HashMap<String, String> paths;

    public FileSystem() {
        this("temp");
    }

    public FileSystem(String temp) {
        files = new PathTree<String, String>();
        paths = new HashMap<String, String>();
        jars = new HashMap<String, String>();
        this.temp = temp;
        clearTemp();
    }

    /**
	 * Adds a directory or jar archive to this virtual file system
	 * 
	 * @param path	the path of the directory or jar file
	 */
    public void addPath(String path) {
        String root = path.substring(0, path.lastIndexOf(File.separator) + 1);
        if (new File(path).isDirectory()) {
            addDirectory(path, root);
            paths.put(path.replace(root, ""), path);
        } else {
            addArchive(path, root);
            jars.put(path.replace(root, ""), path);
        }
    }

    /**
	 * @param dir	the directory to search
	 * @return		all files in the given directory
	 */
    public Collection<String> listFiles(String... dir) {
        return files.list(dir);
    }

    private void addArchive(String path, String root) {
        JarFile jar = null;
        try {
            jar = new JarFile(new File(path));
        } catch (IOException e) {
            System.out.println("Error in addArchive(" + path + ").");
        }
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                String name = new String(entry.getName());
                int separatorCount = name.length() - name.replace("/", "").length();
                String[] pathArray = new String[separatorCount + 2];
                pathArray[0] = jar.getName().replace(root, "").replace(File.separator, "/");
                for (int i = 1; i < separatorCount + 1; i++) {
                    pathArray[i] = name.substring(0, name.indexOf("/"));
                    name = name.substring(name.indexOf("/") + 1);
                }
                pathArray[separatorCount + 1] = name;
                files.add(pathArray, entry.getName());
            }
        }
    }

    private void addDirectory(String path, String root) {
        File dir = new File(path);
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                addDirectory(file.getPath(), root);
            } else {
                String separator = File.separator;
                String relativePath = file.getPath().replace(root, "");
                int separatorCount = relativePath.length() - relativePath.replace(separator, "").length();
                String[] pathArray = new String[separatorCount + 1];
                for (int i = 0; i < separatorCount; i++) {
                    pathArray[i] = relativePath.substring(0, relativePath.indexOf(separator));
                    relativePath = relativePath.substring(relativePath.indexOf(separator) + 1);
                }
                pathArray[separatorCount] = relativePath;
                files.add(pathArray, file.getPath());
            }
        }
    }

    /**
	 * @param <T>
	 * @param name
	 * @param translator
	 * @return				the file with the given name
	 */
    public <T> T getFile(Translator<T> translator, String... path) {
        try {
            return translator.translate(getFile(path));
        } catch (IOException e) {
            return null;
        }
    }

    /**
	 * @param file
	 * @return	whether this file exists or not
	 */
    public boolean exists(String... file) {
        return (files.contains(file));
    }

    public InputStream getFile(String... path) throws IOException {
        if (new File("temp" + File.separator + files.get(path)).exists()) {
            return new FileInputStream("temp" + File.separator + files.get(path));
        } else if (jars.containsKey(path[0])) {
            JarFile jar = new JarFile(new File(jars.get(path[0])));
            return jar.getInputStream(jar.getEntry(files.get(path)));
        } else {
            return new FileInputStream(files.get(path));
        }
    }

    /**
	 * Saves a file with the given path, using a translator.
	 * 
	 * @param <T>			the type of data that has to be saved
	 * @param output		the data that has to be saved
	 * @param translator	a translator 
	 * @param path			the path of the file
	 */
    public <T> void saveFile(T output, Translator<T> translator, String... path) {
        try {
            if (paths.containsKey(path[0])) {
                path[0] = paths.get(path[0]);
            }
            String fullPath = path[0];
            for (int i = 1; i < path.length; i++) {
                fullPath = fullPath + File.separator + path[i];
            }
            File file = new File(fullPath);
            if (!file.getParentFile().exists()) {
                makeDir(file.getParent());
            }
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            translator.translate(output).writeTo(out);
            out.close();
        } catch (IOException e) {
            System.out.println("IOException in FileSystem.saveFile()");
        }
    }

    /**
	 * Saves a file with the given path in the temp directory, using a translator.
	 * 
	 * @param <T>			the type of data that has to be saved
	 * @param output		the data that has to be saved
	 * @param translator	a translator 
	 * @param path			the path of the file
	 */
    public <T> void saveTemp(T output, Translator<T> translator, String... path) {
        try {
            String tempPath = "temp" + File.separator + files.get(path);
            File file = new File(tempPath);
            if (!file.getParentFile().exists()) {
                makeDir(file.getParent());
            }
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            translator.translate(output).writeTo(out);
            out.close();
        } catch (IOException e) {
            System.out.println("IOException in FileSystem.saveFile()");
        }
    }

    private void makeDir(String path) throws IOException {
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            makeDir(file.getParent());
        }
        file.mkdir();
    }

    /**
	 * This method copies all files from the temp directory to the 
	 * designated directory.
	 * 
	 * @param path	the name of the directory to copy temp to
	 */
    public void storeTemp(String path) {
        if (new File(path).isDirectory()) {
            copyDir(new File(temp), new File(path));
        }
    }

    /**
	 * Copies all contents of the given directory to the temp directory.
	 * 
	 * @param path	path of the directory that has to be moved
	 */
    public void moveToTemp(String path) {
        if (new File(path).isDirectory()) {
            copyDir(new File(path), new File(temp));
        }
    }

    private void copyDir(File dir, File dest) {
        File[] files = dir.listFiles();
        for (File file : files) {
            File copy = new File(dest.getPath() + File.separator + file.getName());
            if (file.isDirectory()) {
                copy.mkdir();
                copyDir(file, copy);
            } else {
                try {
                    copyFile(file, copy);
                } catch (IOException e) {
                }
            }
        }
    }

    private void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    /**
	 * Deletes the given file.
	 * 
	 * @param file	the file to delete.
	 */
    public void delete(String file) {
        delete(new File(file));
    }

    /**
	 * This method clears all files in the temp directory.
	 */
    public void clearTemp() {
        File tempDir = new File(temp);
        if (tempDir.exists()) {
            for (File file : tempDir.listFiles()) {
                delete(file);
            }
        } else {
            tempDir.mkdir();
        }
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                delete(f);
            }
        }
        file.delete();
    }

    /**
	 * Packs a directory into a jar file of the same name.
	 * 
	 * @param path	the path to the directory that has to be packed
	 */
    public void pack(String... path) {
        try {
            String name;
            if (path.length == 1) {
                name = path[0];
            } else {
                name = files.get(path);
            }
            File mod = new File(name);
            File jar = new File(name + ".jar");
            byte buffer[] = new byte[1024];
            FileOutputStream stream = new FileOutputStream(jar);
            JarOutputStream out = new JarOutputStream(stream);
            for (String s : listFiles(path)) {
                File file = new File(files.get(path), s);
                if (file != null && file.exists() && !file.isDirectory()) {
                    String entry = file.getPath().replace(mod.getAbsolutePath() + File.separator, "");
                    System.out.println("Adding " + entry);
                    JarEntry jarAdd = new JarEntry(entry);
                    jarAdd.setTime(file.lastModified());
                    out.putNextEntry(jarAdd);
                    FileInputStream in = new FileInputStream(file);
                    while (true) {
                        int nRead = in.read(buffer, 0, buffer.length);
                        if (nRead <= 0) {
                            break;
                        }
                        out.write(buffer, 0, nRead);
                    }
                    in.close();
                }
            }
            out.close();
            stream.close();
            System.out.println("Adding completed OK");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in FileSystem.pack: " + e.getMessage());
        }
    }

    /**
	 * Unpacks a jar file to a directory of the same name.
	 * 
	 * @param path	the path to the jar file that has to be unpacked
	 */
    public void unpack(String... path) {
        try {
            JarFile jar;
            if (path.length == 1) {
                jar = new JarFile(new File(path[0]));
            } else {
                jar = new JarFile(new File(jars.get(path)));
            }
            File dir = new File(jar.getName().replace(".jar", ""));
            dir.mkdir();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                File file = new File(dir.getPath() + File.separator + name);
                if (!entry.isDirectory()) {
                    File parent = new File(file.getParent());
                    if (!parent.exists()) {
                        parent.mkdir();
                    }
                    InputStream in = jar.getInputStream(jar.getEntry(entry.getName()));
                    OutputStream out = new FileOutputStream(file);
                    int nextByte;
                    while ((nextByte = in.read()) != -1) {
                        out.write((byte) nextByte);
                    }
                    out.write('\n');
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Error in FileSystem.unpack: " + e.getMessage());
        }
    }
}
