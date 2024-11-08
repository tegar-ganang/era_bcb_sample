package it.unisannio.xmxmlcompiler.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 * @author Gianluca Iannella
 *
 */
public class FileUtils {

    public static void copyFile(File in, File out) throws IOException {
        try {
            FileReader inf = new FileReader(in);
            OutputStreamWriter outf = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
            int c;
            while ((c = inf.read()) != -1) outf.write(c);
            inf.close();
            outf.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Dato un IResource passato come parametro, ne restituisce il path assoluto
	 * @param file
	 * @return
	 */
    public static String getAsolutePathFromIResource(IResource file) {
        return getFilefromIResource(file).getAbsolutePath();
    }

    /**
	 * Restituisce l'oggetto File relativo all'IResource fornito come parametro
	 * @param file
	 * @return
	 */
    public static File getFilefromIResource(IResource file) {
        return file.getLocation().toFile();
    }

    public static void copyDirectory(File srcPath, File dstPath, boolean overwrite, boolean ignoreHiddenFolder) throws IOException {
        if (srcPath.isDirectory()) {
            if (ignoreHiddenFolder) if (srcPath.getName().startsWith(".")) return;
            if (!dstPath.exists()) {
                dstPath.mkdir();
            }
            String files[] = srcPath.list();
            for (int i = 0; i < files.length; i++) {
                copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]), overwrite, ignoreHiddenFolder);
            }
        } else {
            if (!srcPath.exists()) {
                System.out.println("File or directory does not exist.");
            } else {
                if (overwrite) dstPath.delete();
                if (!dstPath.exists()) {
                    InputStream in = new FileInputStream(srcPath);
                    OutputStream out = new FileOutputStream(dstPath);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                }
            }
        }
        System.out.println("Directory copied.");
    }

    public static void copyDirectory(IResource srcPath, IResource dstPath, boolean overwrite, boolean ignoreHiddenFolder) throws CoreException {
        if (srcPath.getType() == IResource.FOLDER && dstPath.getType() == IResource.FOLDER) {
            IFolder sourceFolder = (IFolder) srcPath;
            IFolder destinationFolder = (IFolder) dstPath;
            if (!destinationFolder.exists()) destinationFolder.create(true, true, null);
            sourceFolder.copy(destinationFolder.getFullPath(), true, null);
            String destiString1 = destinationFolder.getLocation().toOSString();
            String destiString2 = destinationFolder.getProjectRelativePath().toOSString();
            String destiString3 = destinationFolder.getFullPath().toOSString();
            System.out.println(destiString1 + "\n" + destiString2 + "\n" + destiString3);
        } else {
            if (!srcPath.exists()) {
                System.out.println("File or directory does not exist.");
            } else {
            }
        }
        System.out.println("Directory copied.");
    }
}
