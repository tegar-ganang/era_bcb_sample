package com.monad.homerun.admin.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.ObjectOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import com.monad.homerun.config.ConfigContext;
import com.monad.homerun.config.ConfigService;
import com.monad.homerun.config.impl.XmlConfigService;
import com.monad.homerun.core.GlobalProps;
import com.monad.homerun.core.LogTag;
import com.monad.homerun.core.PkgTag;

/**
 * BootStrap is the access point for managing the HomeRun server.
 * It listens on a designated administrative port for commands falling
 * into 5 categories. (1) server control commands: (A) start the
 * server, (B) stop the server, (C) get status or version of the server, or 
 * (D) exit server and bootstrap entirely. (2) file manipulation commands,
 * which apply to specific sets of files (logs, config files, etc):
 * (A) get a file, (B) put a file, (C) list files or directories,
 * (D) delete a file, (E) archive a set of files, and (F) restore a file
 * to its previous version
 * (3) exec commands, which load and execute a class on the server.
 * (4) user commands, which are used to authenticate users of the system.
 * (5) package commands, used for specific operations on packages:
 * (A) get package description, (B) get document enumerating all installed
 * packages, (C) get package localizers
 * 
 * The rationale for BootStrap is twofold. First, since most configuration
 * changes require a server restart, there needs to be hook into the
 * JVM to perform this task. Second, there should be a very simple server
 * always available in cases where the HR server has been compromised, e.g.
 * misconfigured, crashed, disk full, etc. Since Bootstrap can access both
 * the server logs and configuration files, diagnosis and repair of the server
 * can be effected using only Bootstrap. Clients using the BootStrap are:
 * (1) 'Console' application for starting & stopping the server,
 * (2) 'LaunchPad' application for launching agent apps 
 * (3) 'Setup', for working with the config files, and
 * (4) 'LogViewer' for accessing server log files,
 *  but other GUI or non-GUI clients could be easily be written.
 * 
 * Important Note: Bootstrap currently lacks any security (authentication).
 */
class BootStrap extends Thread {

    private int port = 0;

    private String hrBaseDir = null;

    private int swingIndex = -1;

    private static final String logSuffix = ".log";

    private static final String archSuffix = ".zip";

    public BootStrap(int port) {
        this.port = port;
        hrBaseDir = GlobalProps.getHomeDir();
    }

    public void run() {
        boolean shutdown = false;
        Activator.svrdSvc.registerService(GlobalProps.BOOT_SVC_TAG);
        setSwingIndex();
        if (!GlobalProps.EMBEDDED) {
            System.out.println("HomeRun bootstrap on port: " + port);
        }
        try {
            ServerSocket srvSock = new ServerSocket(port);
            while (true) {
                Socket sock = srvSock.accept();
                InputStream in = sock.getInputStream();
                byte[] cmdBuf = new byte[2048];
                int read = 0;
                int idx = 0;
                while ((read = in.read()) != -1) {
                    byte b = (byte) read;
                    if (b == '\n') {
                        break;
                    }
                    cmdBuf[idx++] = b;
                }
                String line = new String(cmdBuf).substring(0, idx);
                if (GlobalProps.DEBUG) {
                    System.out.println("Bootstrap - got: '" + line + "'");
                }
                String[] cmdArgs = line.substring(line.indexOf(":") + 1).split(" ");
                if (line.startsWith("server:")) {
                    String cmd = line.substring(line.indexOf(":") + 1);
                    shutdown = procServerCmds(sock, cmd);
                } else if (line.startsWith("file:")) {
                    if (cmdArgs.length >= 2) {
                        if ("get".equals(cmdArgs[0])) {
                            procGetCmds(sock, cmdArgs);
                        } else if ("put".equals(cmdArgs[0])) {
                            procPutCmds(sock, cmdArgs);
                        } else if ("delete".equals(cmdArgs[0])) {
                            procDelCmds(sock, cmdArgs);
                        } else if ("restore".equals(cmdArgs[0])) {
                            procRestCmds(sock, cmdArgs);
                        } else if ("list".equals(cmdArgs[0])) {
                            procListCmds(sock, cmdArgs);
                        } else if ("category".equals(cmdArgs[0])) {
                            procCatCmds(sock, cmdArgs);
                        } else if ("archive".equals(cmdArgs[0])) {
                            procArchCmds(sock, cmdArgs);
                        } else if ("find".equals(cmdArgs[0])) {
                            procFindCmds(sock, cmdArgs);
                        }
                    }
                } else if (line.startsWith("exec:")) {
                    if (cmdArgs.length >= 2) {
                        procExecCmds(sock, cmdArgs);
                    }
                } else if (line.startsWith("package:")) {
                    procPkgCmds(sock, cmdArgs);
                } else if (line.startsWith("user:")) {
                    if (cmdArgs.length >= 1) {
                        procUserCmds(sock, cmdArgs);
                    }
                }
                in.close();
                sock.close();
                if (shutdown) {
                    Activator.svrdSvc.unregisterService(GlobalProps.BOOT_SVC_TAG);
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            if (GlobalProps.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    private boolean procServerCmds(Socket sock, String cmd) throws IOException {
        String reply = "OK";
        boolean shutdown = false;
        if (GlobalProps.DEBUG) {
            System.out.println("serverCmds cmd: '" + cmd + "'" + " len: " + cmd.length() + "v.len: " + "version".length());
        }
        if ("version".equals(cmd)) {
            reply = GlobalProps.VERSION;
        } else if ("swingindex".equals(cmd)) {
            reply = String.valueOf(swingIndex);
        } else if ("status".equals(cmd)) {
            reply = (Activator.adminMgr != null && Activator.adminMgr.started()) ? "active" : "inactive";
        } else if ("start".equals(cmd)) {
            startServer();
        } else if ("stop".equals(cmd)) {
            stopServer();
        } else if ("exit".equals(cmd)) {
            stopServer();
            shutdown = true;
        }
        if (GlobalProps.DEBUG) {
            System.out.println("writing reply:" + reply);
        }
        OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
        out.write(reply + '\n');
        out.flush();
        return shutdown;
    }

    private void procGetCmds(Socket sock, String[] cmdArgs) throws IOException {
        String resPath = hrBaseDir + File.separator;
        if ("conf".equals(cmdArgs[1]) && cmdArgs.length == 3) {
            String trans = cmdArgs[2].replace(':', File.separatorChar);
            resPath += "conf" + File.separator + trans + ".xml";
        } else if ("log".equals(cmdArgs[1]) && cmdArgs.length == 4) {
            resPath += "logs" + File.separator + cmdArgs[2] + File.separator + cmdArgs[3];
        } else if ("icon".equals(cmdArgs[1]) && cmdArgs.length == 3) {
            resPath += "icons" + File.separator + cmdArgs[2];
        } else if ("image".equals(cmdArgs[1]) && cmdArgs.length == 4) {
            resPath += "var" + File.separator + "images" + File.separator + cmdArgs[2] + File.separator + cmdArgs[3];
        } else if ("jar".equals(cmdArgs[1]) && cmdArgs.length == 4) {
            resPath += "var" + File.separator + "ui" + File.separator + cmdArgs[2] + File.separator + cmdArgs[3];
        } else if ("uicfg".equals(cmdArgs[1]) && cmdArgs.length == 4) {
            resPath += "var" + File.separator + "ui" + File.separator + cmdArgs[2] + File.separator + cmdArgs[3];
        }
        if (!resPath.endsWith(File.separator)) {
            File file = new File(resPath);
            if (file != null && file.exists()) {
                sendFile(file, sock.getOutputStream());
            }
        }
    }

    private void procPutCmds(Socket sock, String[] cmdArgs) throws IOException {
        if ("conf".equals(cmdArgs[1]) && cmdArgs.length == 3) {
            String trans = cmdArgs[2].replace(':', File.separatorChar);
            String tmpName = trans + ".tmp";
            if (Activator.cfgSvc.saveConfigStream(sock.getInputStream(), tmpName)) {
                String backPath = hrBaseDir + File.separator + "conf" + File.separator + trans + ".bak";
                File backFile = new File(backPath);
                if (backFile.exists()) {
                    backFile.delete();
                }
                String cfgPath = hrBaseDir + File.separator + "conf" + File.separator + trans + ".xml";
                File cfgFile = new File(cfgPath);
                if (cfgFile.exists()) {
                    cfgFile.renameTo(backFile);
                }
                String newPath = hrBaseDir + File.separator + "conf" + File.separator + tmpName + ".xml";
                File newCfgFile = new File(newPath);
                File oldCfgFile = new File(cfgPath);
                if (newCfgFile.exists()) {
                    newCfgFile.renameTo(oldCfgFile);
                }
            }
        } else if ("icon".equals(cmdArgs[1]) && cmdArgs.length == 4) {
            String iconPath = hrBaseDir + File.separator + "icons" + File.separator + cmdArgs[2] + File.separator + cmdArgs[3];
            receiveFile(sock.getInputStream(), new File(iconPath));
        } else if ("image".equals(cmdArgs[1]) && cmdArgs.length == 4) {
            String imageDir = hrBaseDir + File.separator + "var" + File.separator + "images" + File.separator + cmdArgs[2];
            File dir = new File(imageDir);
            if (!dir.exists()) {
                dir.mkdir();
            }
            String imagePath = imageDir + File.separator + cmdArgs[3];
            if (GlobalProps.DEBUG) {
                System.out.println("put image path: " + imagePath);
            }
            receiveFile(sock.getInputStream(), new File(imagePath));
        }
    }

    private void procDelCmds(Socket sock, String[] cmdArgs) throws IOException {
        if ("log".equals(cmdArgs[1]) && cmdArgs.length == 4) {
            String logPath = hrBaseDir + File.separator + "logs" + File.separator + cmdArgs[2] + File.separator + cmdArgs[3];
            File logFile = new File(logPath);
            logFile.delete();
        } else if ("icon".equals(cmdArgs[1]) && cmdArgs.length == 4) {
            String iconPath = hrBaseDir + File.separator + "icons" + File.separator + cmdArgs[2] + File.separator + cmdArgs[3];
            File iconFile = new File(iconPath);
            iconFile.delete();
        } else if ("trace".equals(cmdArgs[1]) || "snapshot".equals(cmdArgs[1])) {
            String path = hrBaseDir + File.separator + "temp" + File.separator + cmdArgs[1];
            File cleanDir = new File(path);
            if (cleanDir.exists()) {
                File[] toClean = cleanDir.listFiles();
                for (int i = 0; i < toClean.length; i++) {
                    toClean[i].delete();
                }
            }
        }
    }

    private void procRestCmds(Socket sock, String[] cmdArgs) throws IOException {
        if ("conf".equals(cmdArgs[1]) && cmdArgs.length == 3) {
            String backPath = hrBaseDir + File.separator + "conf" + File.separator + cmdArgs[2] + ".bak";
            File backFile = new File(backPath);
            if (backFile.exists()) {
                String cfgPath = hrBaseDir + File.separator + "conf" + File.separator + cmdArgs[2] + ".xml";
                File cfgFile = new File(cfgPath);
                if (cfgFile.exists()) {
                    cfgFile.delete();
                    backFile.renameTo(cfgFile);
                }
            }
        }
    }

    private void procListCmds(Socket sock, String[] cmdArgs) throws IOException {
        if ("log".equals(cmdArgs[1])) {
            ObjectOutputStream objOut = new ObjectOutputStream(sock.getOutputStream());
            File[] logs = getLogFiles();
            objOut.writeInt(logs.length);
            if (GlobalProps.DEBUG) {
                System.out.println("Sending num logs: " + logs.length);
            }
            for (int j = 0; j < logs.length; j++) {
                objOut.writeObject(new LogTag(logs[j]));
            }
            objOut.flush();
            objOut.close();
        } else if ("package".equals(cmdArgs[1]) && cmdArgs.length == 3) {
            List<PkgTag> tagList = new ArrayList<PkgTag>();
            for (PkgTag tag : Activator.repoSvc.getPackageTags(cmdArgs[2])) {
                if (!"null".equals(tag.pkgType)) {
                    tag.installed = Activator.isPackageInstalled(tag.pkgName, null);
                    tagList.add(tag);
                }
            }
            if (tagList.size() > 0) {
                ObjectOutputStream objOut = new ObjectOutputStream(sock.getOutputStream());
                objOut.writeInt(tagList.size());
                if (GlobalProps.DEBUG) {
                    System.out.println("Sending num tags: " + tagList.size());
                }
                for (PkgTag tag : tagList) {
                    objOut.writeObject(tag);
                }
                objOut.flush();
                objOut.close();
            }
        } else if ("conf".equals(cmdArgs[1]) && cmdArgs.length == 3) {
            String cfgPath = hrBaseDir + File.separator + "conf" + File.separator + cmdArgs[2];
            File cfgDir = new File(cfgPath);
            if (cfgDir.isDirectory()) {
                StringBuffer sb = new StringBuffer();
                File[] files = cfgDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    String name = files[i].getName();
                    if (name.endsWith(".xml")) {
                        sb.append(name.substring(0, name.indexOf(".xml")));
                        sb.append(" ");
                    }
                }
                OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
                out.write(sb.toString() + '\n');
                out.flush();
            }
        } else if ("jars".equals(cmdArgs[1]) && cmdArgs.length == 3) {
            String cfgPath = hrBaseDir + File.separator + "var" + File.separator + "ui" + File.separator + cmdArgs[2];
            File cfgDir = new File(cfgPath);
            if (cfgDir.isDirectory()) {
                StringBuffer sb = new StringBuffer();
                File[] files = cfgDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    String name = files[i].getName();
                    if (name.endsWith(".jar")) {
                        sb.append(name.substring(0, name.indexOf(".jar")));
                        sb.append(" ");
                    }
                }
                OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
                out.write(sb.toString() + '\n');
                out.flush();
            }
        }
    }

    private class VersionFilter implements FileFilter {

        private String vsn = null;

        public VersionFilter(String vsn) {
            this.vsn = vsn;
        }

        public boolean accept(File file) {
            String fileName = file.getName();
            int match = fileName.indexOf("-") + 1;
            if (match > 0) {
                return fileName.regionMatches(match, vsn, 0, vsn.length());
            }
            return false;
        }
    }

    private void procCatCmds(Socket sock, String[] cmdArgs) throws IOException {
        StringBuffer sb = new StringBuffer();
        if ("conf".equals(cmdArgs[1])) {
            String path = hrBaseDir + File.separator + cmdArgs[1];
            File theDir = new File(path);
            if (theDir.isDirectory()) {
                String[] dirNames = theDir.list();
                if (GlobalProps.DEBUG) {
                    System.out.println("procCat num: " + dirNames.length);
                }
                for (int i = 0; i < dirNames.length; i++) {
                    File entry = new File(path + File.separator + dirNames[i]);
                    if (entry.isDirectory()) {
                        sb.append(dirNames[i]).append(" ");
                    }
                }
            }
        } else if ("package".equals(cmdArgs[1])) {
            for (String cat : Activator.repoSvc.getCategories(true)) {
                sb.append(cat).append(" ");
            }
        }
        if (sb.toString().length() > 0) {
            OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
            out.write(sb.toString() + '\n');
            out.flush();
        }
    }

    private void procArchCmds(Socket sock, String[] cmdArgs) throws IOException {
        LogTag[] tags = new LogTag[(cmdArgs.length - 1) / 2];
        int idx = 0;
        for (int i = 1; i < cmdArgs.length; i += 2) {
            String logPath = hrBaseDir + File.separator + "logs" + File.separator + cmdArgs[i] + File.separator + cmdArgs[i + 1];
            File logFile = new File(logPath);
            tags[idx++] = new LogTag(logFile);
        }
        if (Activator.logSvc.archiveLogs(tags)) {
            Activator.logSvc.removeLogs(tags);
        }
    }

    private void procPkgCmds(Socket sock, String[] cmdArgs) throws IOException {
        if ("describe".equals(cmdArgs[0])) {
            String desc = null;
            if (cmdArgs.length == 2) {
                desc = PackageInstaller.getDescription(cmdArgs[1]);
            }
            if (desc == null) {
                desc = "<html><body>No description available</body></html>";
            }
            OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
            desc = desc.replaceAll("\n", "\t");
            out.write(desc + '\n');
            out.flush();
        } else if ("localizers".equals(cmdArgs[0])) {
            Properties props = new Properties();
            if (cmdArgs.length == 2) {
                props = PackageInstaller.getLocalizerBase(cmdArgs[1]);
            }
            ObjectOutputStream objOut = new ObjectOutputStream(sock.getOutputStream());
            objOut.writeObject(props);
            objOut.flush();
            objOut.close();
        } else if ("localize".equals(cmdArgs[0])) {
            ObjectInputStream objIn = new ObjectInputStream(sock.getInputStream());
            Properties props = null;
            try {
                props = (Properties) objIn.readObject();
            } catch (ClassNotFoundException cnfE) {
                if (GlobalProps.DEBUG) {
                    System.out.println("procPkgCmds:localize - class not found exception");
                }
            }
            PackageInstaller.localizeSite(cmdArgs[1], props);
        } else if ("install".equals(cmdArgs[0])) {
            String reply = null;
            if (cmdArgs.length == 2) {
                reply = PackageInstaller.installPackage(cmdArgs[1]);
            }
            if (reply == null) {
                reply = "OK";
            }
            setSwingIndex();
            OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
            out.write(reply + '\n');
            out.flush();
        } else if ("installed".equals(cmdArgs[0])) {
            OutputStream out = sock.getOutputStream();
            out.write(Activator.getInstalled().getBytes());
            out.close();
        }
    }

    private void procFindCmds(Socket sock, String[] cmdArgs) throws IOException {
        if ("log".equals(cmdArgs[1]) && cmdArgs.length == 6) {
            String logPath = hrBaseDir + File.separator + "logs" + File.separator + cmdArgs[2] + File.separator + cmdArgs[3];
            File file = new File(logPath);
            int matchIdx = -1;
            if (file.exists()) {
                int lineCount = -1;
                boolean ignoreCase = cmdArgs[5].startsWith("I");
                boolean wholeWord = cmdArgs[5].endsWith("W");
                BufferedReader in = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = in.readLine()) != null) {
                    ++lineCount;
                    String[] parts = line.split("\t");
                    if (parts.length == 4) {
                        String message = parts[3];
                        String text = cmdArgs[4];
                        if (ignoreCase) {
                            message = message.toLowerCase();
                            text = text.toLowerCase();
                        }
                        int pos = message.indexOf(text);
                        if (pos != -1) {
                            if (!wholeWord || wholeWordMatch(message, text, pos)) {
                                matchIdx = lineCount;
                                break;
                            }
                        }
                    }
                }
            }
            OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
            out.write(String.valueOf(matchIdx) + '\n');
            out.flush();
        }
    }

    private static boolean wholeWordMatch(String text, String word, int pos) {
        if (pos == 0) {
            if (text.endsWith(word) || text.indexOf(word + " ") != -1) {
                return true;
            }
        } else if (text.endsWith(word)) {
            if (text.startsWith(word) || text.indexOf(" " + word) != -1) {
                return true;
            }
        } else if (text.indexOf(" " + word + " ") != -1) {
            return true;
        }
        return false;
    }

    private void procExecCmds(Socket sock, String[] cmdArgs) throws IOException {
        String reply = null;
        if ("loader".equals(cmdArgs[0]) && cmdArgs.length == 3) {
            String path = hrBaseDir + File.separator + "var" + File.separator + "package" + File.separator + cmdArgs[1] + File.separator + cmdArgs[2] + ".xml";
            if (new File(path).exists()) {
                reply = new ObjectLoader().loadObjects(path, new Properties());
            }
        } else if ("exporter".equals(cmdArgs[0]) && cmdArgs.length == 4) {
            reply = Exporter.export(cmdArgs[1], GlobalProps.VERSION, cmdArgs[2], cmdArgs[3]);
        }
        if (reply == null) {
            reply = "OK";
        }
        OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
        out.write(reply + '\n');
        out.flush();
    }

    private void procUserCmds(Socket sock, String[] cmdArgs) throws IOException {
        String reply = "fail";
        if ("auth".equals(cmdArgs[0]) && cmdArgs.length == 3) {
            if (Activator.appSvc != null) {
                if (Activator.appSvc.authenticateUser(cmdArgs[1], cmdArgs[2])) {
                    reply = "OK";
                }
            }
        } else if ("list".equals(cmdArgs[0])) {
            if (Activator.appSvc != null) {
                String[] users = Activator.appSvc.getUserNames();
                if (users != null) {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < users.length; i++) {
                        sb.append(users[i]);
                        sb.append(" ");
                    }
                    reply = sb.toString();
                }
            }
        } else if ("prof".equals(cmdArgs[0]) && cmdArgs.length == 2) {
            if (Activator.appSvc != null) {
                InetAddress remAddr = sock.getInetAddress();
                reply = Activator.appSvc.userAuthRequired(cmdArgs[1], remAddr.isSiteLocalAddress());
            }
            if (reply == null) {
                reply = "BadRequest";
            }
        } else if ("describe".equals(cmdArgs[0]) && cmdArgs.length == 2) {
            reply = null;
            if (Activator.appSvc != null) {
                reply = Activator.appSvc.describeUser(cmdArgs[1]);
            }
            if (reply == null) {
                reply = "BadRequest";
            }
        } else if ("update".equals(cmdArgs[0]) && cmdArgs.length == 3) {
            reply = "failed";
            if (Activator.appSvc != null) {
                String[] roles = cmdArgs[2].split(",");
                if (Activator.appSvc.updateUser(cmdArgs[1], roles)) {
                    reply = "OK";
                }
            }
        } else if ("remove".equals(cmdArgs[0]) && cmdArgs.length == 2) {
            reply = "failed";
            if (Activator.appSvc != null) {
                if (Activator.appSvc.removeUser(cmdArgs[1])) {
                    reply = "OK";
                }
            }
        } else if ("chgpwd".equals(cmdArgs[0]) && cmdArgs.length == 3) {
            reply = "failed";
            if (Activator.appSvc != null) {
                if (Activator.appSvc.changeUserPassword(cmdArgs[1], cmdArgs[2])) {
                    reply = "OK";
                }
            }
        }
        OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
        out.write(reply + '\n');
        out.flush();
    }

    protected void startServer() {
        if (!Activator.adminMgr.started()) {
            Activator.adminMgr.start();
        }
    }

    private void stopServer() {
        if (Activator.adminMgr.started()) {
            Activator.adminMgr.shutdown();
        }
    }

    private void sendFile(File file, OutputStream out) throws IOException {
        FileInputStream fileIn = new FileInputStream(file);
        byte[] buff = new byte[2048];
        int read = 0;
        int total = 0;
        while ((read = fileIn.read(buff)) != -1) {
            total += read;
            if (GlobalProps.DEBUG) {
                System.out.println("sendFile read: " + read + " total: " + total);
            }
            out.write(buff, 0, read);
        }
        if (GlobalProps.DEBUG) {
            System.out.println("sendFile read total: " + total);
        }
        fileIn.close();
        out.close();
    }

    private void receiveFile(InputStream in, File file) {
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[2048];
            int numRead = in.read(buffer);
            while (numRead != -1) {
                if (GlobalProps.DEBUG) {
                    System.out.println("RCV_FILE read: " + numRead);
                }
                out.write(buffer, 0, numRead);
                numRead = in.read(buffer);
            }
        } catch (IOException e) {
            if (GlobalProps.DEBUG) {
                System.out.println("Exception: " + e.toString());
                e.printStackTrace();
            }
            GlobalProps.sysLog("receiveFile - file: " + file.getName() + " was not saved");
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                GlobalProps.sysLog("Caught IO ex: " + e.toString());
            }
        }
    }

    private static File[] getLogFiles() {
        String logBaseDir = GlobalProps.getHomeDir() + File.separator + "logs";
        File logParent = new File(logBaseDir);
        if (!logParent.exists()) {
            return new File[0];
        }
        List<File> fileList = new ArrayList<File>();
        String[] subDirs = logParent.list();
        for (int i = 0; i < subDirs.length; i++) {
            File logDir = new File(logBaseDir + File.separator + subDirs[i]);
            File[] logFiles = logDir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return (name.endsWith(logSuffix) || name.endsWith(archSuffix));
                }
            });
            for (int j = 0; j < logFiles.length; j++) {
                fileList.add(logFiles[j]);
            }
        }
        return fileList.toArray(new File[0]);
    }

    private void setSwingIndex() {
        ConfigService configSvc = new XmlConfigService();
        String path = GlobalProps.getHomeDir() + File.separator + "var" + File.separator + "ui" + File.separator + "swing" + File.separator + "swingui.xml";
        try {
            ConfigContext uiCtx = configSvc.getContextAt(path, "clients/@Console");
            swingIndex = Integer.parseInt(uiCtx.getAttribute("version"));
        } catch (IOException ioE) {
            if (GlobalProps.DEBUG) {
                System.out.println("Ouch");
                ioE.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int port = -1;
        boolean start = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-port") && args.length > i + 1) {
                port = Integer.parseInt(args[i + 1]);
            } else if (args[i].equalsIgnoreCase("-start")) {
                start = true;
            } else if (args[i].equalsIgnoreCase("-help")) {
                System.out.println("Usage: BootStrap -port <portnum> -start" + '\n' + "-start starts server");
                System.exit(0);
            }
        }
        if (port == -1) {
            System.out.println("No port specified - exiting");
            System.exit(0);
        }
        BootStrap bootstrap = new BootStrap(port);
        if (start) {
            bootstrap.startServer();
        }
        bootstrap.start();
    }
}
