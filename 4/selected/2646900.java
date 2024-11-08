package de.schwarzrot.system.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import de.schwarzrot.app.errors.ApplicationException;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.system.SysInfo;

public class FileUtils {

    private static SysInfo sysInfo = null;

    public static boolean checkSudoAccess(String sudoPassword) {
        boolean rv = false;
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
        if (sysInfo.isLinux() && sudoPassword != null) {
            ArrayList<String> cmd = new ArrayList<String>();
            StringBuilder work = new StringBuilder("echo \"");
            cmd.add(sysInfo.getShell().getAbsolutePath());
            cmd.add("-c");
            work.append(sudoPassword);
            work.append("\" | sudo -S wc -l /etc/sudoers");
            cmd.add(work.toString());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process proc;
            try {
                proc = pb.start();
                BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line = br.readLine();
                proc.waitFor();
                br.close();
                if (line != null && line.length() > 0) {
                    String[] parts = line.split(" ");
                    int lines = Integer.parseInt(parts[0]);
                    rv = lines > 1;
                }
            } catch (Throwable t) {
                throw new ApplicationException("could not verify sudo access", t);
            }
        }
        return rv;
    }

    public static boolean copyFile(File dest, File source) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean rv = false;
        byte[] buf = new byte[1000000];
        int bytesRead = 0;
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(dest);
            while ((bytesRead = fis.read(buf)) > 0) fos.write(buf, 0, bytesRead);
            fis.close();
            fis = null;
            fos.close();
            fos = null;
            rv = true;
        } catch (Throwable t) {
            throw new ApplicationException("copy error (" + source.getAbsolutePath() + " => " + dest.getAbsolutePath(), t);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                }
                fis = null;
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
                fos = null;
            }
        }
        return rv;
    }

    public static boolean copyFile(File dest, File source, String sudoPassword) {
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
        if (sysInfo.isLinux() && sudoPassword != null) {
            ArrayList<String> cmd = new ArrayList<String>();
            StringBuilder work = new StringBuilder("echo \"");
            if (!dest.getParentFile().exists()) createDirectory(dest.getParentFile(), sudoPassword);
            cmd.add(sysInfo.getShell().getAbsolutePath());
            cmd.add("-c");
            work.append(sudoPassword);
            work.append("\" | sudo -S cp \"");
            work.append(source.getAbsolutePath());
            work.append("\" \"");
            work.append(dest.getAbsolutePath());
            work.append("\"");
            cmd.add(work.toString());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process proc;
            try {
                proc = pb.start();
                proc.waitFor();
            } catch (Throwable t) {
                throw new ApplicationException("error on privileged file copy (" + source.getAbsolutePath() + ")", t);
            }
        } else copyFile(dest, source);
        return dest.exists();
    }

    public static boolean copyRecursive(File dest, File source) {
        boolean rv = true;
        if (source.isFile()) return copyFile(dest, source);
        if (source.isDirectory() && source.canRead()) {
            if (!dest.exists()) dest.mkdirs();
            if (dest.exists() && dest.isDirectory()) {
                for (File cur : source.listFiles()) {
                    if (cur.isFile()) {
                        rv &= copyFile(new File(dest, cur.getName()), cur);
                    } else if (cur.isDirectory()) {
                        rv &= copyRecursive(new File(dest, cur.getName()), cur);
                    } else {
                        throw new ApplicationException("don't know how to handle: " + cur.getAbsolutePath());
                    }
                }
            }
        } else rv = false;
        return rv;
    }

    public static boolean createDirectory(File dir, String sudoPassword) {
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
        if (sysInfo.isLinux() && sudoPassword != null) {
            List<String> cmd = new ArrayList<String>();
            StringBuilder work = new StringBuilder("echo \"");
            cmd.add(sysInfo.getShell().getAbsolutePath());
            cmd.add("-c");
            work.append(sudoPassword);
            work.append("\" | sudo -S mkdir -p \"");
            work.append(dir.getAbsolutePath());
            work.append("\"");
            cmd.add(work.toString());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process proc;
            try {
                proc = pb.start();
                proc.waitFor();
            } catch (Throwable t) {
                throw new ApplicationException("could not create privileged directory (" + dir.getAbsolutePath() + ")", t);
            }
        } else dir.mkdirs();
        return dir.exists() && dir.isDirectory();
    }

    public static boolean createSoftLink(File dest, File source) throws Exception {
        boolean rv = false;
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
        if (!sysInfo.isLinux()) throw new ApplicationException("softlink only supported on linux systems!");
        if (!(dest.getParentFile().exists() && dest.getParentFile().isDirectory())) dest.getParentFile().mkdirs();
        if (!(dest.getParentFile().exists() && dest.getParentFile().isDirectory())) throw new ApplicationException("could not create link, cause parent is not a directory");
        List<String> cmd = new ArrayList<String>();
        StringBuilder sb = new StringBuilder("ln -s ");
        cmd.add(sysInfo.getShell().getAbsolutePath());
        cmd.add("-c");
        sb.append("'");
        sb.append(source.getCanonicalPath());
        sb.append("' '");
        sb.append(dest.getAbsolutePath());
        sb.append("'");
        cmd.add(sb.toString());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process proc;
        try {
            proc = pb.start();
            proc.waitFor();
            rv = dest.exists();
        } catch (Throwable t) {
            throw new ApplicationException("could not create softlink (" + dest.getAbsolutePath() + ")", t);
        }
        return rv;
    }

    public static String getExtension(File aFile) {
        String ext = null;
        String tmp = aFile.getName();
        int i = tmp.lastIndexOf('.');
        if (i > 0 && i < tmp.length() - 1) ext = tmp.substring(i + 1).toLowerCase();
        return ext;
    }

    public static boolean joinFiles(File dest, Collection<File> sources) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean rv = false;
        byte[] buf = new byte[1000000];
        int bytesRead = 0;
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        try {
            fos = new FileOutputStream(dest);
            for (File source : sources) {
                fis = new FileInputStream(source);
                while ((bytesRead = fis.read(buf)) > 0) fos.write(buf, 0, bytesRead);
                fis.close();
                fis = null;
            }
            fos.close();
            fos = null;
            rv = true;
        } catch (Throwable t) {
            throw new ApplicationException("error joining files to " + dest.getAbsolutePath(), t);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                }
                fis = null;
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
                fos = null;
            }
        }
        return rv;
    }

    public static boolean moveOrRename(File dest, File source) {
        boolean rv = false;
        if (source.renameTo(dest)) rv = true; else if (FileUtils.copyFile(dest, source)) {
            source.delete();
            rv = true;
        }
        return rv;
    }

    public static boolean removeDirectory(File dir) {
        boolean rv = false;
        if (dir != null && dir.exists() && dir.isDirectory()) {
            for (File cur : dir.listFiles()) {
                if (cur.isDirectory()) removeDirectory(cur);
                cur.delete();
            }
            dir.delete();
        }
        return rv;
    }

    public static boolean removeDirectory(File dir, String sudoPassword) {
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
        if (sysInfo.isLinux() && sudoPassword != null) {
            ArrayList<String> cmd = new ArrayList<String>();
            StringBuilder work = new StringBuilder("echo \"");
            cmd.add(sysInfo.getShell().getAbsolutePath());
            cmd.add("-c");
            work.append(sudoPassword);
            work.append("\" | sudo -S rm -rf \"");
            work.append(dir.getAbsolutePath());
            work.append("\"");
            cmd.add(work.toString());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process proc;
            try {
                proc = pb.start();
                proc.waitFor();
            } catch (Throwable t) {
                throw new ApplicationException("could not create privileged directory (" + dir.getAbsolutePath() + ")", t);
            }
        } else removeDirectory(dir);
        return !(dir.exists() && dir.isDirectory());
    }

    public static boolean removeDirectoryDoNotCare(File dir) {
        boolean rv = false;
        if (dir != null && dir.exists() && dir.isDirectory()) {
            for (File cur : dir.listFiles()) {
                if (cur.isDirectory()) removeDirectoryDoNotCare(cur);
                try {
                    cur.delete();
                } catch (Throwable t) {
                }
            }
            try {
                dir.delete();
            } catch (Throwable t) {
            }
        }
        return rv;
    }

    public static void setFileOwner(File file, String owner, String sudoPassword) {
        setFileOwner(file, owner, sudoPassword, false);
    }

    public static void setFileOwner(File file, String owner, String sudoPassword, boolean recursive) {
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
        if (sysInfo.isLinux() && sudoPassword != null) {
            ArrayList<String> cmd = new ArrayList<String>();
            StringBuilder work = new StringBuilder("echo \"");
            cmd.add(sysInfo.getShell().getAbsolutePath());
            cmd.add("-c");
            work.append(sudoPassword);
            work.append("\" | sudo -S chown ");
            if (recursive) work.append("-R ");
            work.append(owner);
            work.append(" \"");
            work.append(file.getAbsolutePath());
            work.append("\"");
            cmd.add(work.toString());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process proc;
            try {
                proc = pb.start();
                proc.waitFor();
            } catch (Throwable t) {
                throw new ApplicationException("could change permission of privileged file (" + file.getAbsolutePath() + ")", t);
            }
        } else throw new ApplicationException("invalid usage! Use File.setWritable() or File.setExecutable()");
    }

    public static void setFilePermission(File file, String permission, String sudoPassword) {
        setFilePermission(file, permission, sudoPassword, false);
    }

    public static void setFilePermission(File file, String permission, String sudoPassword, boolean recursive) {
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
        if (sysInfo.isLinux() && sudoPassword != null) {
            ArrayList<String> cmd = new ArrayList<String>();
            StringBuilder work = new StringBuilder("echo \"");
            cmd.add(sysInfo.getShell().getAbsolutePath());
            cmd.add("-c");
            work.append(sudoPassword);
            work.append("\" | sudo -S chmod ");
            if (recursive) work.append(" -R ");
            work.append(permission);
            work.append(" \"");
            work.append(file.getAbsolutePath());
            work.append("\"");
            cmd.add(work.toString());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process proc;
            try {
                proc = pb.start();
                proc.waitFor();
            } catch (Throwable t) {
                throw new ApplicationException("could change permission of privileged file (" + file.getAbsolutePath() + ")", t);
            }
            if (file.isDirectory()) {
                if (file.getParent() != null) setFilePermission(file.getParentFile(), permission, sudoPassword, false);
            }
        } else throw new ApplicationException("invalid usage! Use File.setWritable() or File.setExecutable()");
    }
}
