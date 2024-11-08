package kernel;

import java.io.*;
import java.net.*;

public class FM {

    public static final void main() {
        Dev.sda[0] = System.getProperty("user.home").trim() + "/.JTeal/";
        if (Init.args.length > 0) {
            int i = Init.in_array(Init.args, "--path");
            if (i != -1) {
                String p = Init.args[i + 1].trim();
                if (p.endsWith("\"")) {
                    p = p.substring(0, p.length() - 1);
                }
                if (!p.endsWith("/")) {
                    p += "/";
                }
                Dev.sda[0] = p;
            }
        }
        if (FM.exist(Dev.sda[0] + "etc/rc.conf")) {
            String[] l = FM.list(Dev.sda[0] + "dev");
            for (int i = 0; i < l.length; i++) {
                System.out.println("Mounting " + l[i].trim() + " to " + (l[i] = FM.read(Dev.sda[0] + "dev/" + l[i].trim()).trim()));
            }
            Dev.sda = l;
        }
    }

    public static void partition() {
        Console.log("Creating filesystem");
        FM.mkdir(Dev.sda[0]);
        Console.log("Mounting folder to sda0");
        FM.mkdir(Dev.sda[0] + "etc");
        Console.log("Settings folder created");
        FM.mkdir(Dev.sda[0] + "var");
        FM.mkdir(Dev.sda[0] + "var/log");
        FM.create(Dev.sda[0] + "var/log/sys.log");
        Console.log("Log files created");
        FM.mkdir(Dev.sda[0] + "root");
        Console.log("Root folder created");
        FM.mkdir(Dev.sda[0] + "home");
        Console.log("Home folder created");
        FM.mkdir(Dev.sda[0] + "etc/rc.d");
        FM.jarget("rc0", Dev.sda[0] + "etc/rc.d/");
        FM.jarget("rc.conf", Dev.sda[0] + "etc/");
        Settings.set("user", System.getProperty("user.name"));
        Console.log("Boot Scripts created");
        FM.mkdir(Dev.sda[0] + "home/" + Settings.get("user"));
        Console.log("User folder " + Settings.get("user") + " created");
        FM.mkdir(Dev.sda[0] + "bin");
        Console.log("Bin folder created");
        FM.mkdir(Dev.sda[0] + "sbin");
        FM.jarget("env.rhino.js", Dev.sda[0] + "sbin/");
        FM.jarget("boot.html", Dev.sda[0]);
        FM.jarget("boot.js", Dev.sda[0] + "/sbin/");
        FM.jarget("thread.js", Dev.sda[0] + "/sbin/");
        FM.jarget("jquery.js", Dev.sda[0] + "/sbin/");
        Console.log("Sbin folder created");
        FM.mkdir(Dev.sda[0] + "dev");
        Console.log("Devices Folder created\nPopulating");
        FM.write(Dev.sda[0] + "dev/sda0", Dev.sda[0]);
    }

    /**
	 * @param path path to file
	 * @param text text to store 
	 */
    public static boolean write(String path, String text) {
        BufferedWriter out;
        boolean r = false;
        try {
            out = new BufferedWriter(new FileWriter(path));
            out.write(text);
            out.close();
            r = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    /**
	 * @param path path to folder
	 */
    public static String[] list(String path) {
        return (new File(path)).list();
    }

    /**
	 * @param path path to file
	 */
    public static boolean exist(String path) {
        File file = new File(path);
        return file.exists();
    }

    /**
	 * @param path path to file
	 */
    public static String read(String path) {
        File file = new File(path);
        String out = "";
        String tmp = "";
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(bis));
            try {
                while ((tmp = br.readLine()) != null) {
                    out += tmp + "\n";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static String get(String uri, String path) {
        FM.create(path);
        URL u;
        InputStream is = null;
        BufferedReader br;
        String s;
        try {
            u = new URL(uri);
            is = u.openStream();
            br = new BufferedReader(new InputStreamReader(new BufferedInputStream(is)));
            while ((s = br.readLine()) != null) {
                FM.append(path, s);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
        return FM.read(path);
    }

    public static boolean create(String path) {
        return FM.write(path, "");
    }

    public static boolean append(String path, String text) {
        return FM.write(path, FM.read(path) + text);
    }

    public static boolean delete(String path) {
        File f = new File(path);
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    FM.delete(files[i].getPath());
                    files[i].delete();
                } else {
                    files[i].delete();
                }
            }
            return f.delete();
        } else if (f.isFile()) {
            return f.delete();
        }
        return false;
    }

    public static boolean mkdir(String path) {
        return new File(path).mkdir();
    }

    public static void jarget(String getfile, String dest) {
        try {
            InputStream in = new BufferedInputStream(java.lang.System.class.getClass().getResourceAsStream(("/resource/" + getfile)));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(dest, getfile)));
            byte[] buffer = new byte[2048];
            for (; ; ) {
                int nBytes = in.read(buffer);
                if (nBytes <= 0) break;
                out.write(buffer, 0, nBytes);
            }
            out.flush();
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
