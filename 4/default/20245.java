import java.io.*;
import java.util.Vector;

public class DirectoryManager {

    private String directory;

    public DirectoryManager(String newDir) {
        setDirectory(newDir);
    }

    public void setDirectory(String newDir) {
        File f = new File(newDir);
        if (f.exists() && f.isDirectory()) {
            directory = newDir;
            if (!directory.endsWith(File.separator)) directory = directory.concat(File.separator);
        } else {
            if (!PatchManager.mute) System.out.println("Invalid Directory : " + newDir);
            throw new Error("Invalid Directory : " + newDir);
        }
    }

    public File[] listMyFiles() {
        File directoryToScan = new File(directory);
        return directoryToScan.listFiles();
    }

    public void getAllMyFileNames(Vector<String> fileName, boolean exploreSubDir, boolean includeDirectoryName) {
        File[] filesSource = listMyFiles();
        if (filesSource != null) {
            for (int i = 0; i < filesSource.length; i++) {
                if (filesSource[i].isDirectory()) {
                    if (includeDirectoryName) fileName.add(filesSource[i].getPath());
                    if (exploreSubDir) {
                        DirectoryManager subDir = new DirectoryManager(filesSource[i].getPath());
                        if (!PatchManager.mute) System.out.println("exploring " + filesSource[i].getPath());
                        subDir.getAllMyFileNames(fileName, exploreSubDir, includeDirectoryName);
                    }
                } else fileName.add(filesSource[i].getPath());
            }
        }
    }

    public int calcNumFiles(boolean includeSubDir, boolean includeDirectory) {
        Vector<String> v = new Vector<String>();
        getAllMyFileNames(v, includeSubDir, includeDirectory);
        return v.size();
    }

    public boolean ownsThisFile(String fileName) {
        File f = new File(directory + fileName);
        if (f.exists()) return f.isFile();
        return false;
    }

    public boolean ownsThisDirectory(String dirName) {
        File f = new File(directory + dirName);
        if (f.exists()) return f.isDirectory();
        return false;
    }

    public String getDirectory() {
        return directory;
    }

    public boolean copyMe(String pathDest) {
        if (!pathDest.endsWith(File.separator)) pathDest = pathDest.concat(File.separator);
        File f = new File(pathDest);
        if (!f.exists() || !f.isDirectory()) {
            f.mkdir();
            if (!PatchManager.mute) System.out.println("directory created : " + f.getPath());
        }
        File[] myFiles = listMyFiles();
        if (myFiles != null) {
            for (int i = 0; i < myFiles.length; i++) {
                if (myFiles[i].isFile()) {
                    copyOneOfMyFile(myFiles[i], pathDest);
                } else if (myFiles[i].isDirectory()) {
                    DirectoryManager subDir = new DirectoryManager(myFiles[i].getPath());
                    subDir.copyMe(pathDest + myFiles[i].getName());
                }
            }
        }
        return true;
    }

    public boolean copyOneOfMyFile(File f, String dest) {
        if (!ownsThisFile(f.getName())) return false;
        if (!dest.endsWith(File.separator)) dest = dest.concat(File.separator);
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(dest + f.getName())));
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
            int len = 1024;
            byte[] read = new byte[len];
            while ((len = in.read(read)) > 0) out.write(read, 0, len);
            out.flush();
            out.close();
            in.close();
            if (!PatchManager.mute) System.out.println("file created : " + dest + f.getName());
        } catch (IOException e) {
            System.out.println("copy directory : " + e);
            return false;
        }
        return true;
    }

    public void deleteMe() {
        File[] myFiles = listMyFiles();
        if (myFiles != null) {
            for (int i = 0; i < myFiles.length; i++) {
                if (myFiles[i].isFile()) {
                    if (!PatchManager.mute) System.out.println("deleted : " + myFiles[i].getPath());
                    myFiles[i].delete();
                } else if (myFiles[i].isDirectory()) {
                    DirectoryManager subDir = new DirectoryManager(myFiles[i].getPath());
                    subDir.deleteMe();
                }
            }
        }
        File me = new File(directory);
        if (!PatchManager.mute) System.out.println("deleted : " + me.getPath());
        me.delete();
    }

    public boolean addMeFile(File f) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(directory + f.getName())));
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
            byte[] read = new byte[128];
            int len = 128;
            while ((len = in.read(read)) > 0) out.write(read, 0, len);
            out.flush();
            out.close();
            in.close();
            if (!PatchManager.mute) System.out.println("added : " + directory + f.getName());
        } catch (IOException e) {
            System.out.println("copy directory : " + e);
            return false;
        }
        return true;
    }
}
