package com.microfly.core;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.*;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import com.microfly.exception.NpsException;
import com.microfly.exception.ErrorHelper;
import com.microfly.util.Utils;

/**
 * FTP��ҵ���У�FIFO
 *  ÿ����ҵ������ϴ�4�Σ�ʧ�ܽ������״Ӵ���ɾ��
 *  a new publishing system
 *  Copyright (c) 2007
 *
 * @author jialin
 * @version 1.0
 */
public class FtpQueue {

    private FtpHost host = null;

    private FTPClient client = null;

    private Queue new_ftp_tasks = null;

    private Queue fail1_ftp_tasks = null;

    private Queue fail2_ftp_tasks = null;

    private Queue fail3_ftp_tasks = null;

    private boolean nativeLoopRunning = true;

    private FtpThread dispatchThread = null;

    public FtpQueue(FtpHost host) {
        this.host = host;
        new_ftp_tasks = new ConcurrentLinkedQueue();
        fail1_ftp_tasks = new ConcurrentLinkedQueue();
        fail2_ftp_tasks = new ConcurrentLinkedQueue();
        fail3_ftp_tasks = new ConcurrentLinkedQueue();
    }

    public FtpHost GetHost() {
        return host;
    }

    public synchronized void AddTask(File local, String remote) throws NpsException {
        FtpTaskDef task = new FtpTaskDef(local, remote);
        new_ftp_tasks.add(task);
        Store2File(task);
        if (dispatchThread == null || !dispatchThread.isAlive()) {
            dispatchThread = new FtpThread(this);
            dispatchThread.start();
        }
        notify();
    }

    public synchronized void AddTask(String remote) throws NpsException {
        FtpTaskDef task = new FtpTaskDef(remote);
        new_ftp_tasks.add(task);
        Store2File(task);
        if (dispatchThread == null || !dispatchThread.isAlive()) {
            dispatchThread = new FtpThread(this);
            dispatchThread.start();
        }
        notify();
    }

    private String GetStoreFileName(FtpTaskDef task) {
        return task.id + host.GetHostname() + ".ftp";
    }

    private void Store2File(FtpTaskDef task) {
        File ftp_file_dir = new File(Config.OUTPATH_FTPTASK);
        if (!ftp_file_dir.exists()) ftp_file_dir.mkdirs();
        File tmp_task_file = new File(ftp_file_dir, GetStoreFileName(task));
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(tmp_task_file));
            oos.writeObject(host);
            oos.writeObject(task);
            oos.flush();
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error_noexception(e);
        } finally {
            if (oos != null) try {
                oos.close();
            } catch (Exception e) {
            }
        }
    }

    private void DeleteFromStore(FtpTaskDef task) {
        File file_new = new File(Config.OUTPATH_FTPTASK + GetStoreFileName(task));
        if (!file_new.exists()) return;
        try {
            file_new.delete();
        } catch (Exception e) {
        }
    }

    public void LoadTask(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        FtpTaskDef task = (FtpTaskDef) stream.readObject();
        new_ftp_tasks.add(task);
        if (dispatchThread == null || !dispatchThread.isAlive()) {
            dispatchThread = new FtpThread(this);
            dispatchThread.start();
        }
        notify();
    }

    private boolean isShutdown() {
        if (nativeLoopRunning) return false;
        if (new_ftp_tasks.peek() != null) return false;
        if (fail1_ftp_tasks.peek() != null) return false;
        if (fail2_ftp_tasks.peek() != null) return false;
        if (fail3_ftp_tasks.peek() != null) return false;
        return true;
    }

    public synchronized Object[] GetNextTask() throws Exception {
        Object[] ret = new Object[2];
        FtpTaskDef task = (FtpTaskDef) new_ftp_tasks.poll();
        if (task != null) {
            ret[0] = new Integer(0);
            ret[1] = task;
            return ret;
        }
        task = (FtpTaskDef) fail3_ftp_tasks.poll();
        if (task != null) {
            ret[0] = new Integer(3);
            ret[1] = task;
            return ret;
        }
        task = (FtpTaskDef) fail2_ftp_tasks.poll();
        if (task != null) {
            ret[0] = new Integer(2);
            ret[1] = task;
            return ret;
        }
        task = (FtpTaskDef) fail1_ftp_tasks.poll();
        if (task != null) {
            ret[0] = new Integer(1);
            ret[1] = task;
            return ret;
        }
        while (task == null) {
            if (isShutdown()) {
                dispatchThread = null;
                throw new InterruptedException();
            }
            if (fail1_ftp_tasks.peek() != null || fail2_ftp_tasks.peek() != null || fail3_ftp_tasks.peek() != null) {
                wait(Config.SCHEDULE_INTERVAL * 60 * 1000);
            } else {
                FtpScheduler.GetScheduler().Load(host);
                wait();
            }
            task = (FtpTaskDef) new_ftp_tasks.poll();
            if (task != null) {
                ret[0] = new Integer(0);
                ret[1] = task;
                return ret;
            }
            task = (FtpTaskDef) fail3_ftp_tasks.poll();
            if (task != null) {
                ret[0] = new Integer(3);
                ret[1] = task;
                return ret;
            }
            task = (FtpTaskDef) fail2_ftp_tasks.poll();
            if (task != null) {
                ret[0] = new Integer(2);
                ret[1] = task;
                return ret;
            }
            task = (FtpTaskDef) fail1_ftp_tasks.poll();
            if (task != null) {
                ret[0] = new Integer(1);
                ret[1] = task;
                return ret;
            }
        }
        return null;
    }

    protected void run(FtpTaskDef task, Integer runlevel) {
        try {
            switch(task.mode) {
                case 0:
                    Upload(task);
                    break;
                case 1:
                    Delete(task);
            }
            DeleteFromStore(task);
        } catch (FTPConnectionClosedException e1) {
            Disconnect();
        } catch (Exception e) {
            switch(runlevel) {
                case 0:
                    fail1_ftp_tasks.add(task);
                    break;
                case 1:
                    fail2_ftp_tasks.add(task);
                    break;
                case 2:
                    fail3_ftp_tasks.add(task);
                    break;
            }
        }
        Disconnect();
    }

    private void Connect() throws NpsException {
        try {
            client = new FTPClient();
            client.connect(host.hostname, host.remoteport);
            int reply = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect();
                client = null;
                com.microfly.util.DefaultLog.error_noexception("FTP Server:" + host.hostname + "refused connection.");
                return;
            }
            client.login(host.uname, host.upasswd);
            client.enterLocalPassiveMode();
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
            client.changeWorkingDirectory(host.remotedir);
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        }
    }

    private void Disconnect() {
        if (client == null) return;
        try {
            client.logout();
            client.disconnect();
        } catch (Exception e) {
            com.microfly.util.DefaultLog.info(host.hostname + "�Ͽ�����ʧ�ܣ�\n" + e.getMessage());
        } finally {
            client = null;
        }
    }

    private void Upload(FtpTaskDef task) throws Exception {
        if (host == null) return;
        if (client == null) Connect();
        MkDirs(task.remote);
        client.changeWorkingDirectory(task.remote);
        InputStream fis = new FileInputStream(task.local);
        OutputStream os = client.storeFileStream(task.remote);
        byte buf[] = new byte[8192];
        int bytesRead = fis.read(buf);
        while (bytesRead != -1) {
            os.write(buf, 0, bytesRead);
            bytesRead = fis.read(buf);
        }
        try {
            fis.close();
        } catch (Exception e) {
        }
        try {
            os.close();
        } catch (Exception e) {
        }
        if (!client.completePendingCommand()) {
            com.microfly.util.DefaultLog.error_noexception(host.hostname + "�ϴ��ļ�ʧ�ܣ�" + task.remote);
            throw new NpsException(host.hostname + "�ϴ��ļ�ʧ�ܣ�" + task.remote, ErrorHelper.FTP_UPLOAD_FAILED);
        }
    }

    private void Delete(FtpTaskDef task) throws Exception {
        if (host == null) return;
        if (client == null) Connect();
        client.deleteFile(host.remotedir + "/" + task.remote);
    }

    private void MkDirs(String remote_file) throws IOException {
        if (remote_file.length() == 0) return;
        String remote = Utils.FixPath(remote_file);
        int pos_splash = remote.lastIndexOf("/");
        remote = remote.substring(0, pos_splash + 1);
        if (!client.changeWorkingDirectory(remote)) {
            if (!client.makeDirectory(remote)) {
                pos_splash = remote.substring(0, remote.length() - 1).lastIndexOf("/");
                String remote_parent = remote.substring(0, pos_splash + 1);
                MkDirs(remote_parent);
                client.makeDirectory(remote);
            }
        }
    }
}
