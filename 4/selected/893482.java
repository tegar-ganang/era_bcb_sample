package data;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class JarHandler {

    private File jar;

    public File tmpDir;

    public File verFile;

    public static int BUFFER_SIZE = 10240;

    public JarHandler(File file) {
        jar = file;
        tmpDir = new File((jar.getParentFile().getAbsolutePath() + File.separator + "tmp").replaceAll("\\\\", "/"));
        verFile = new File((jar.getParentFile().getAbsolutePath() + File.separator + "version").replaceAll("\\\\", "/"));
    }

    public File addFileToJar(String file, byte[] data) {
        File f = new File("");
        try {
            f = new File((tmpDir.getAbsolutePath() + File.separator + file).replaceAll("\\\\", "/"));
            String[] sarr = file.split("/");
            if (sarr.length > 1) {
                String dirStr = new String();
                for (int i = 0; i < sarr.length - 1; i++) dirStr += sarr[i];
                File dir = new File(tmpDir.getAbsolutePath() + File.separator + dirStr);
                dir.mkdirs();
            }
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            for (int i = 0; i < data.length; i++) {
                out.writeByte(data[i]);
            }
            out.close();
            return f;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not add " + f.getAbsolutePath() + " to JAR file!", "ERROR", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public Map<String, File> extractJarArchive(JLabel label) {
        tmpDir.mkdir();
        Map<String, File> files = new TreeMap<String, File>();
        File f = new File("");
        JarFile jarf;
        try {
            jarf = new JarFile(jar);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not open JAR file!", "ERROR", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        try {
            Enumeration<JarEntry> enum1 = jarf.entries();
            while (enum1.hasMoreElements()) {
                JarEntry file = (JarEntry) enum1.nextElement();
                f = new File((tmpDir.getAbsolutePath() + File.separator + file.getName()).replaceAll("\\\\", "/"));
                label.setText("Extracting from JAR: " + file.getName());
                if (file.getName().indexOf("META-INF") != -1) continue;
                String[] sarr = file.getName().replaceAll("\\\\", "/").split("/");
                if (sarr.length == 1 && file.getName().contains("/")) continue;
                if (sarr.length > 1) {
                    String dirStr = new String();
                    for (int i = 0; i < sarr.length - 1; i++) {
                        dirStr += sarr[i];
                        if (i < sarr.length - 2) dirStr += "\\";
                    }
                    File dir = new File(tmpDir.getAbsolutePath() + File.separator + dirStr);
                    dir.mkdirs();
                }
                files.put((file.getName().replaceAll("\\\\", "/")), f);
                InputStream is = jarf.getInputStream(file);
                FileOutputStream fos = new java.io.FileOutputStream(f);
                while (is.available() > 0) {
                    fos.write(is.read());
                }
                fos.close();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not extract " + f.getAbsolutePath() + " from JAR file!", "ERROR", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return files;
    }

    public boolean createJarArchive(File[] tobeJared, JLabel label) {
        try {
            byte buffer[] = new byte[BUFFER_SIZE];
            FileOutputStream stream = new FileOutputStream(jar);
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            manifest.getMainAttributes().putValue("Main-Class", "net.minecraft.client.Minecraft");
            JarOutputStream out = new JarOutputStream(stream, manifest);
            for (int i = 0; i < tobeJared.length; i++) {
                String internalPath = "";
                try {
                    if (tobeJared[i] == null || !tobeJared[i].exists() || tobeJared[i].isDirectory()) {
                        System.out.println("Skipping " + tobeJared[i].getName());
                        continue;
                    }
                    internalPath = tobeJared[i].getAbsolutePath().substring(tmpDir.getAbsolutePath().length() + 1).replaceAll("\\\\", "/");
                    label.setText("Saving to JAR: " + internalPath);
                    JarEntry jarAdd = new JarEntry(internalPath);
                    jarAdd.setTime(tobeJared[i].lastModified());
                    out.putNextEntry(jarAdd);
                    FileInputStream in = new FileInputStream(tobeJared[i]);
                    while (true) {
                        int nRead = in.read(buffer, 0, buffer.length);
                        if (nRead <= 0) break;
                        out.write(buffer, 0, nRead);
                    }
                    in.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Could not import " + internalPath + " into JAR!", "ERROR", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            out.close();
            stream.close();
            deleteDir(tmpDir);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not create JAR file!", "ERROR", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public static boolean deleteDir(File dir) {
        try {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not delete temporary file" + dir.getAbsolutePath() + "!", "ERROR", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
