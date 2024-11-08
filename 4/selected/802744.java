package Utilitaires;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import javax.swing.JFileChooser;

public class FileUtility {

    @SuppressWarnings("unchecked")
    protected static String getPrimaryPath(File ref, File access) {
        String path = "";
        ArrayList refp = new ArrayList();
        for (File cur = ref; cur != null; cur = cur.getParentFile()) {
            String name = cur.getName();
            if (name.length() == 0) name = cur.getAbsolutePath();
            refp.add(name);
        }
        ArrayList accp = new ArrayList();
        for (File cur = access; cur != null; cur = cur.getParentFile()) {
            String name = cur.getName();
            if (name.length() == 0) name = cur.getAbsolutePath();
            accp.add(name);
        }
        if (refp.size() == 0 || accp.size() == 0) return path;
        if (refp.get(refp.size() - 1).equals(accp.get(accp.size() - 1))) {
            boolean equal = true;
            while (equal && refp.size() > 1 && accp.size() > 1) {
                refp.remove(refp.size() - 1);
                accp.remove(accp.size() - 1);
                equal = (refp.get(refp.size() - 1).equals(accp.get(accp.size() - 1)));
            }
            if (refp.size() == 1) {
                if (!equal) {
                    refp.remove(refp.size() - 1);
                    path += "..";
                } else {
                    refp.remove(refp.size() - 1);
                    accp.remove(accp.size() - 1);
                    path += ".";
                }
            } else {
                if (equal && accp.size() == 1) {
                    refp.remove(refp.size() - 1);
                    accp.remove(accp.size() - 1);
                }
                while (refp.size() > 0) {
                    refp.remove(refp.size() - 1);
                    path += "..";
                    if (refp.size() > 0) path += File.separator;
                }
            }
            while (accp.size() > 0) {
                String name = (String) accp.remove(accp.size() - 1);
                path += File.separator;
                path += name;
            }
        } else {
            try {
                path = access.getCanonicalPath();
            } catch (IOException e) {
                path = access.getAbsolutePath();
                e.printStackTrace();
            }
        }
        return path;
    }

    /**
    * retourne le path differentiel
    * @param ref -File le fichier
    * @param acc -File 
    */
    public static String getDiffPath(File ref, File acc) {
        try {
            File cref = ref.getCanonicalFile();
            File cacc = acc.getCanonicalFile();
            return getPrimaryPath(cref, cacc);
        } catch (IOException e) {
            e.printStackTrace();
            return getPrimaryPath(ref, acc);
        }
    }

    /**
    * retourne le path differentiel
    * @param ref -String le fichier
    * @param acc -String 
    */
    public static String getDiffPath(String ref, String acc) {
        File fref = new File(ref);
        File facc = new File(acc);
        return getDiffPath(fref, facc);
    }

    public static void main(String[] args) {
        JFileChooser dlg = new JFileChooser();
        dlg.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        while (true) {
            if (dlg.showOpenDialog(null) == JFileChooser.CANCEL_OPTION) return;
            File first = dlg.getSelectedFile();
            if (first == null) return;
            if (dlg.showOpenDialog(null) == JFileChooser.CANCEL_OPTION) return;
            File last = dlg.getSelectedFile();
            if (last == null) return;
            System.out.println("First Path : " + first.getPath());
            System.out.println("Last  Path : " + last.getPath());
            System.out.println("First Name : " + first.getName());
            System.out.println("Last  Name : " + last.getName());
            System.out.println("First AbsolutePath : " + first.getAbsolutePath());
            System.out.println("Last  AbsolutePath : " + last.getAbsolutePath());
            System.out.println("Diff  PrimaryPath : " + getPrimaryPath(first, last));
            System.out.println("--------------------------------------------------\n");
        }
    }

    /**
    * supprime recurssivement un repertoire
    * @param path -File le repertoire a supprimer
    * @throws IOException
    */
    public static void recursifDelete(File path) throws IOException {
        if (!path.exists()) {
            throw new IOException("File not found '" + path.getAbsolutePath() + "'");
        }
        if (path.isDirectory()) {
            File[] children = path.listFiles();
            for (int i = 0; children != null && i < children.length; i++) recursifDelete(children[i]);
            if (!path.delete()) {
                throw new IOException("No delete path '" + path.getAbsolutePath() + "'");
            }
        } else if (!path.delete()) throw new IOException("No delete file '" + path.getAbsolutePath() + "'");
    }

    /**
    * copie de fichier
    * @param src -File la source
    * @param dest -File la destination
    * @throws IOException
    */
    public static void copyFile(File src, File dest) throws IOException {
        if (!src.exists()) throw new IOException("File not found '" + src.getAbsolutePath() + "'");
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
        byte[] read = new byte[128];
        int len = 128;
        while ((len = in.read(read)) > 0) out.write(read, 0, len);
        out.flush();
        out.close();
        in.close();
    }

    /**
    * cr�ation d'un fichier en particulier
    * @return File
    * @param path -String chemin du fichier
    * @param pattern -String extension
    * @param base -int 
    * @throws IOException
    */
    public static File createSpecifiedFile(String path, String pattern, int base) {
        File file = new File(path);
        if (!file.isDirectory()) return null;
        for (int cur = base; ; cur++) {
            Object[] args = { new Integer(cur) };
            String result = MessageFormat.format(pattern, args);
            file = new File(path, result);
            if (!file.exists()) return file;
        }
    }
}
