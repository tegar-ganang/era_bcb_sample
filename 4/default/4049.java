import java.util.Vector;
import java.io.*;

public class FTPFile {

    FTPFile parent = null;

    boolean isFile = true;

    String name;

    Vector children = null;

    Vector longlist = null;

    FTP ftp;

    /** Creates new FTPFile */
    public FTPFile(FTPFile parent, String name, FTP ftp) {
        this.parent = parent;
        this.name = name;
        this.ftp = ftp;
        if (!parent.child_isFile(name)) isFile = false;
    }

    public FTPFile(FTP ftp) {
        isFile = false;
        this.name = "/";
        this.ftp = ftp;
        children = ftp.ls();
        longlist = ftp.longls();
    }

    public boolean child_isFile(String nameOfChild) {
        System.out.println("child_isFile(): " + nameOfChild);
        if (!children.contains(nameOfChild)) System.out.println("Weird! Im not the Father...");
        if (longlist == null) longlist = ftp.longls();
        for (int i = 0; i < longlist.size(); i++) if (((String) longlist.get(i)).endsWith(nameOfChild)) {
            if (((String) longlist.get(i)).charAt(0) == 'd') return false; else return true;
        }
        System.out.println("VERY Weird! Im not the Father of " + nameOfChild);
        return false;
    }

    public Vector list() {
        if (isFile) return null;
        if (children == null) if (ftp.cwd(parent.getPath() + "/" + name) == 0) {
            children = ftp.ls();
        }
        return children;
    }

    public FTP getFtp() {
        return ftp;
    }

    public String toString() {
        return name;
    }

    public String getPath() {
        String s = new String();
        FTPFile par = this;
        if (parent == null) return "/";
        s = name;
        do {
            par = par.parent;
            s = par.getName() + "/" + s;
        } while (par.getName() != "/");
        return s.substring(1);
    }

    public String getName() {
        return name;
    }

    public boolean isFile() {
        return isFile;
    }

    public int receiveTo(String localPath) {
        File f;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte[] buffer = new byte[2048];
        int lastread = 0;
        f = new File(localPath);
        if (f.exists()) {
            System.out.println("FILE exists, will not overwrite");
            return -1;
        } else try {
            if (!f.createNewFile()) {
                System.out.println("CANT create FILE " + localPath);
                return -2;
            }
        } catch (IOException e) {
        }
        ;
        try {
            out = new BufferedOutputStream(new FileOutputStream(f));
        } catch (FileNotFoundException e) {
            System.out.println("CANT FIND FILE");
        }
        System.out.println("receiveTO: " + this.getPath());
        in = ftp.retr(this.getPath());
        if ((in == null) || (out == null)) {
            System.out.println("Ooops! Cant Set-Up File-Retreiving!");
            return -1;
        }
        try {
            for (int i = 0; (lastread = in.read(buffer, 0, 2048)) != -1; ) {
                out.write(buffer, 0, lastread);
                i += lastread;
                System.out.println(i);
            }
            ;
            if (ftp.getResult() != 226) return -1;
            in.close();
            out.close();
        } catch (IOException e) {
        }
        return 0;
    }

    public int receiveTo_Progress(String localPath, FileReceiveProgressBar progress) {
        File f;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte[] buffer = new byte[2048];
        int lastread = 0;
        f = new File(localPath);
        if (f.exists()) {
            System.out.println("FILE exists, will not overwrite");
            return -1;
        } else try {
            if (!f.createNewFile()) {
                System.out.println("CANT create FILE " + localPath);
                return -2;
            }
        } catch (IOException e) {
        }
        ;
        try {
            out = new BufferedOutputStream(new FileOutputStream(f));
        } catch (FileNotFoundException e) {
            System.out.println("CANT FIND FILE");
        }
        System.out.println("receiveTO: " + this.getPath());
        in = ftp.retr(this.getPath());
        int size = ftp.getLastFileSize();
        if ((in == null) || (out == null)) {
            System.out.println("Ooops! Cant Set-Up File-Retreiving!");
            return -1;
        }
        progress.setMaximum(size);
        progress.setNote("Receiving " + this.getName());
        try {
            for (int i = 0; (lastread = in.read(buffer, 0, 2048)) != -1; ) {
                out.write(buffer, 0, lastread);
                i += lastread;
                if (progress.isCanceled()) {
                    System.out.println("Canceled");
                }
                progress.setProgress(i);
            }
            ;
            if (ftp.getResult() != 226) return -1;
            progress.close();
            in.close();
            out.close();
        } catch (IOException e) {
        }
        return 0;
    }

    public int putFrom(String localFilePath) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(localFilePath));
        } catch (FileNotFoundException e) {
            System.out.println("Cant open " + localFilePath);
            return -2;
        }
        if (ftp.put(in, this.getPath()) != 0) {
            System.out.println("putFrom: ERROR from ftp.put");
            return -1;
        }
        try {
            in.close();
        } catch (IOException e) {
        }
        return 0;
    }
}
