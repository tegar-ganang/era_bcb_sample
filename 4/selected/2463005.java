package net.emotivecloud.vrmm.vtm.executor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jcraft.jsch.*;
import es.bsc.brein.jsdl.JSDL;

/**
 * Executes tasks using SSH.
 * @author goirix
 */
public class SSH {

    private static Log log = LogFactory.getLog(SSH.class);

    private String user;

    private String identity;

    private String host;

    private String executorPath;

    private String executorPathLocal;

    /**
	 * Creates a SSH executor in a given host.
	 * @param host Host where the tasks will be executed.
	 */
    public SSH(String host) {
        this("user", "/root/.ssh/id_rsa", host);
    }

    /**
	 * Creates a SSH executor.
	 * @param user User that will execute the tasks.
	 * @param identity Identity of the user containing the certificate.
	 * @param host Host where the tasks will be executed.
	 */
    public SSH(String user, String identity, String host) {
        this.user = user;
        this.identity = identity;
        this.host = host;
        this.executorPath = "/usr/bin/executor";
        this.executorPathLocal = "/usr/bin/executor";
        URL url = SSH.class.getResource("SSH.class");
        if (url != null) {
            String path = url.toString();
            if (path.startsWith("jar:")) {
                path = path.replaceFirst("jar:", "");
                if (path.startsWith("file:")) path = path.replaceFirst("file:", "");
                String jarPath = path.substring(0, path.indexOf("!"));
                executorPathLocal = unJar(jarPath, "bin/executor");
                log.debug("SSH Executor: " + executorPathLocal);
            } else if (path.startsWith("file:")) {
                path = path.replaceFirst("file:", "");
                path = path.substring(0, path.lastIndexOf("VtM/") + 4);
                executorPathLocal = path + "bin/executor";
                log.info("SSH Executor: " + executorPathLocal);
            } else {
                log.error("Unknown path: " + url);
            }
        } else {
            log.error("Searching SSH executor.");
        }
    }

    public void setExecutor(String path) {
        this.executorPath = path;
    }

    /**
	 * Gets the identity of the executor.
	 * @return The user identity.
	 */
    public String getIdentity() {
        return identity;
    }

    public static boolean isPortOpen(String address, int port) {
        boolean open = false;
        try {
            Socket testSock = new Socket(address, port);
            testSock.close();
            open = true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            log.error("Port of " + address + " is closed.");
        }
        return open;
    }

    /**
	 * Deploy the required stuff inside the VM.
	 */
    public void deploy() {
        this.executorPath = this.deploy(executorPathLocal, "executor");
    }

    public boolean isDeployed() {
        boolean ret = false;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(this.identity);
            Session session = jsch.getSession(this.user, this.host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            Vector aux = sftp.ls(this.executorPath);
            if (aux.size() > 0) ret = true;
            session.disconnect();
        } catch (Exception e) {
            log.error("Checking if the file exists: " + e.getMessage());
        }
        return ret;
    }

    public boolean existsFile(String fileName) {
        boolean ret = false;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(this.identity);
            Session session = jsch.getSession(this.user, this.host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            Vector aux = sftp.ls(fileName);
            if (aux.size() > 0) ret = true;
            session.disconnect();
        } catch (Exception e) {
            log.error("Checking if the file exists: " + e.getMessage());
        }
        return ret;
    }

    /**
	 * Deploy the required stuff inside the VM.
	 */
    public String deploy(String src, String dst) {
        String ret = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(this.identity);
            Session session = jsch.getSession(this.user, this.host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            sftp.mkdir(dst.substring(0, dst.lastIndexOf("/")));
            sftp.put(src, dst);
            sftp.chmod(320, dst);
            ret = sftp.pwd() + "/" + dst;
            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void checkConnection() throws Exception {
        JSch jsch = new JSch();
        jsch.addIdentity(this.identity);
        Session session = jsch.getSession(this.user, this.host, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        session.disconnect();
    }

    /**
	 * Executes a Job in a remote location. Requires "StrictHostKeyChecking no" in /etc/ssh/ssh_config.
	 * @param id Identifier of the job to execute.
	 * @param jsdl Description of te job.
	 * @return If the job has been well finished.
	 * @throws Exception
	 */
    public boolean submitJob(String id, JSDL jsdl) throws Exception {
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(this.identity);
            Session session = jsch.getSession(this.user, this.host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            File tempJSDL = File.createTempFile("task", "jsdl");
            FileUtils.writeStringToFile(tempJSDL, jsdl.toString());
            sftp.put(tempJSDL.getAbsolutePath(), "task-" + id + ".jsdl");
            SSHExecutor exec = new SSHExecutor(id);
            exec.connect();
            exec.start();
            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public String cancelJob2(String id) throws Exception {
        String pid = "Unknown";
        int exit = 0;
        JSch jsch = new JSch();
        jsch.addIdentity(this.identity);
        Session session = jsch.getSession(this.user, this.host, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(executorPath + " kill " + id);
        log.debug("Executing: " + executorPath + " kill " + id);
        channel.setInputStream(null);
        InputStream in = channel.getInputStream();
        channel.connect();
        String auxStatus = "";
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                auxStatus += new String(tmp, 0, i);
            }
            if (channel.isClosed()) {
                exit = channel.getExitStatus();
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }
        }
        pid = auxStatus;
        if (exit != 0) {
            pid = "Failed";
            log.error("SSH execution failed: " + exit);
        } else if (pid.startsWith("Failed")) {
            pid = "cancel";
            log.error("Command execution failed: " + pid);
        }
        channel.disconnect();
        session.disconnect();
        pid = pid.replaceAll("  ", " ");
        pid = pid.replaceAll("\n", "");
        return pid;
    }

    public String cancelJob(String idd) {
        int exit = 0;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(this.identity);
            Session session = jsch.getSession(this.user, this.host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            InputStream in = channel.getInputStream();
            channel.setCommand(executorPath + " kill " + idd);
            log.info("Executing: " + executorPath + " kill " + idd);
            channel.connect();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    exit = channel.getExitStatus();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            channel.disconnect();
            session.disconnect();
            System.out.println("Execution finished with exit status");
            return "ok";
        } catch (Exception e) {
            log.error(e);
        }
        return "bad";
    }

    /**
	 * Get the status of a job.
	 * @param id Identifier of the Job.
	 * @return The status of the job.
	 * @throws Exception
	 */
    public String getStatus(String id) throws Exception {
        String status = "Unknown";
        int exit = 0;
        JSch jsch = new JSch();
        jsch.addIdentity(this.identity);
        Session session = jsch.getSession(this.user, this.host, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(executorPath + " status " + id);
        log.debug("Executing: " + executorPath + " status " + id);
        channel.setInputStream(null);
        InputStream in = channel.getInputStream();
        channel.connect();
        String auxStatus = "";
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                auxStatus += new String(tmp, 0, i);
            }
            if (channel.isClosed()) {
                exit = channel.getExitStatus();
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }
        }
        status = auxStatus;
        if (exit != 0) {
            status = "Failed";
            log.error("SSH execution failed: " + exit);
        } else if (status.startsWith("Failed")) {
            log.error("Command execution failed: " + status);
        }
        channel.disconnect();
        session.disconnect();
        status = status.replaceAll("  ", " ");
        status = status.replaceAll("\n", "");
        if (status.contains("Done")) status = "Done"; else if (status.contains("Unsubmitted")) status = "Unsubmitted"; else if (status.contains("Failed")) {
            status = "Failed";
        }
        return status;
    }

    public String execute(JSDL jsdl) {
        String ret = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(this.identity);
            Session session = jsch.getSession(this.user, this.host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelExec exec = (ChannelExec) session.openChannel("exec");
            InputStream in = exec.getInputStream();
            exec.setCommand(jsdl.getCommand());
            log.info("Executing: " + jsdl.getCommand());
            exec.connect();
            ret = "";
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    ret += new String(tmp, 0, i);
                }
                if (exec.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            exec.disconnect();
            session.disconnect();
        } catch (Exception e) {
            log.error("Error executing job: " + e.getMessage());
        }
        return ret;
    }

    /**
	 * Extract a given entry from its JAR file.
	 * @param jarPath
	 * @param jarEntry
	 */
    private String unJar(String jarPath, String jarEntry) {
        String path;
        if (jarPath.lastIndexOf("lib/") >= 0) path = jarPath.substring(0, jarPath.lastIndexOf("lib/")); else path = jarPath.substring(0, jarPath.lastIndexOf("/"));
        String relPath = jarEntry.substring(0, jarEntry.lastIndexOf("/"));
        try {
            new File(path + "/" + relPath).mkdirs();
            JarFile jar = new JarFile(jarPath);
            ZipEntry ze = jar.getEntry(jarEntry);
            File bin = new File(path + "/" + jarEntry);
            IOUtils.copy(jar.getInputStream(ze), new FileOutputStream(bin));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path + "/" + jarEntry;
    }

    /**
	 * SSH executor.
	 * @author goirix
	 */
    class SSHExecutor extends Thread {

        private String id;

        private Session session;

        private Integer exit;

        public SSHExecutor(String id) {
            this.id = id;
            this.exit = null;
        }

        public void connect() throws Exception {
            JSch jsch = new JSch();
            jsch.addIdentity(identity);
            session = jsch.getSession(user, host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
        }

        public void run() {
            try {
                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                InputStream in = channel.getInputStream();
                channel.setCommand(executorPath + " run " + id + " task-" + id + ".jsdl");
                log.info("Executing: " + executorPath + " run " + id + " task-" + id + ".jsdl");
                channel.connect();
                byte[] tmp = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
                        System.out.print(new String(tmp, 0, i));
                    }
                    if (channel.isClosed()) {
                        this.exit = new Integer(channel.getExitStatus());
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ee) {
                    }
                }
                channel.disconnect();
                session.disconnect();
                System.out.println("Execution finished with exit status " + this.exit);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
}
