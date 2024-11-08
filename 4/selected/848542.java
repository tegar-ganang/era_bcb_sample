package se.mdh.mrtc.saveccm.repository.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class FileFtpUtility {

    public FileFtpUtility() {
    }

    public static void removeDirectory(String directory) {
        File dir = new File(directory);
        String[] info = dir.list();
        try {
            for (int i = 0; i < info.length; i++) {
                File n = new File(directory + dir.separator + info[i]);
                if (!n.isFile()) removeDirectory(n.getAbsolutePath());
                if (!n.delete()) System.err.println("Couldn't remove " + n.getPath());
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        if (!dir.delete()) System.err.println("Couldn't remove " + dir.getPath());
    }

    public static void copyFiles(String strPath, String dstPath) throws IOException {
        File src = new File(strPath);
        File dest = new File(dstPath);
        if (src.isDirectory()) {
            dest.mkdirs();
            String list[] = src.list();
            for (int i = 0; i < list.length; i++) {
                String dest1 = dest.getAbsolutePath() + File.separatorChar + list[i];
                String src1 = src.getAbsolutePath() + File.separatorChar + list[i];
                copyFiles(src1, dest1);
            }
        } else {
            FileInputStream fin = new FileInputStream(src);
            FileOutputStream fout = new FileOutputStream(dest);
            int c;
            while ((c = fin.read()) >= 0) fout.write(c);
            fin.close();
            fout.close();
        }
    }

    public IFile getWorkspaceFile(File file) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath location = Path.fromOSString(file.getAbsolutePath());
        IFile[] files = workspace.getRoot().findFilesForLocation(location);
        if (files == null || files.length == 0) {
            return null;
        }
        return files[0];
    }
}
