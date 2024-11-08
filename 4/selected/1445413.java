package jamsa.rcp.downloader.models;

import jamsa.rcp.downloader.Messages;
import jamsa.rcp.downloader.http.HttpClientUtils;
import jamsa.rcp.downloader.http.RemoteFileInfo;
import jamsa.rcp.downloader.preference.PreferenceManager;
import jamsa.rcp.downloader.utils.Logger;
import jamsa.rcp.downloader.utils.StringUtils;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * 多线程下载中的任务线程,用于控制其它下载线程
 * 
 * @author 朱杰
 * 
 */
public class TaskThread2 extends Thread {

    private static Logger logger = new Logger(TaskThread2.class);

    private Task task;

    private TaskModel taskModel;

    private PreferenceManager preferenceManager;

    public TaskThread2(Task task) {
        this.task = task;
        this.taskModel = TaskModel.getInstance();
        preferenceManager = PreferenceManager.getInstance();
    }

    private void changeStatus(int status) {
        task.setStatus(status);
    }

    private List threads = new ArrayList(10);

    /**
	 * 验证远程文件与本地文件的一致性
	 * 
	 * @param remoteFileName
	 *            远端文件名
	 * @param remoteFileSize
	 *            远端文件大小
	 */
    private void checkFile(String remoteFileName, long remoteFileSize) {
        if (task.getFileSize() > 0 && task.getFileSize() != remoteFileSize) {
            task.writeMessage("Task", Messages.TaskThread2_MSG_FileSize_Change_Restart);
            logger.info("文件大小不一至和，重新下载！");
            task.reset();
            task.setStatus(Task.STATUS_RUNNING);
        }
        task.setFileSize(remoteFileSize);
        if (task.getFinishedSize() == 0 && !StringUtils.isEmpty(remoteFileName) && !task.getFileName().equals(remoteFileName)) {
            task.setFileName(remoteFileName);
        }
    }

    /**
	 * 启动
	 * 
	 */
    public void runUnfinishedSplitters() {
    }

    public void run() {
        task.writeMessage("Task", Messages.TaskThread2_MSG_Task_Started);
        task.getMessages().clear();
        changeStatus(Task.STATUS_RUNNING);
        if (task.getBeginTime() == 0) task.setBeginTime(System.currentTimeMillis());
        taskModel.updateTask(task);
        RemoteFileInfo remoteFile = HttpClientUtils.getRemoteFileInfo(task.getFileUrl(), preferenceManager.getRetryTimes(), preferenceManager.getRetryDelay() * 1000, preferenceManager.getTimeout() * 1000, new Properties(), "HEAD", task, "Task");
        if (remoteFile == null) {
            task.writeMessage("Task", Messages.TaskThread2_ERR_Can_Not_Get_Remote_File_Info);
            task.setStatus(Task.STATUS_ERROR);
            taskModel.updateTask(task);
            return;
        }
        checkFile(remoteFile.getFileName(), remoteFile.getFileSize());
        task.checkBlocks();
        taskModel.updateTask(task);
        File file = task.getTempSavedFile();
        RandomAccessFile savedFile = null;
        try {
            savedFile = new RandomAccessFile(file, "rw");
            task.writeMessage("Task", Messages.TaskThread2_MSG_Open_Temp_File + file);
            int ct = 0;
            for (Iterator iter = task.getSplitters().iterator(); iter.hasNext(); ) {
                ct++;
                if (ct > task.getBlocks()) break;
                TaskSplitter s = (TaskSplitter) iter.next();
                if (!s.isFinish()) {
                    DownloadThread t = new DownloadThread(task, savedFile, s);
                    threads.add(t);
                    t.start();
                    task.writeMessage("Task", Messages.TaskThread2_MSG_Start_Download_Thread + s.getName());
                    Thread.sleep(500);
                }
            }
            long lastTime = System.currentTimeMillis();
            long lastSize = task.getFinishedSize();
            int lastRunBlocks = task.getRunBlocks();
            while (task.getStatus() == Task.STATUS_RUNNING && !this.isInterrupted()) {
                long currentSize = 0;
                boolean finished = true;
                for (Iterator it = task.getSplitters().iterator(); it.hasNext(); ) {
                    TaskSplitter splitter = (TaskSplitter) it.next();
                    currentSize += splitter.getFinished();
                    if (!splitter.isFinish()) {
                        finished = false;
                    }
                }
                long current = System.currentTimeMillis();
                long timeDiff = current - lastTime;
                task.setTotalTime(task.getTotalTime() + timeDiff);
                lastTime = current;
                task.setFinishedSize(currentSize);
                long sizeDiff = currentSize - lastSize;
                lastSize = currentSize;
                if (timeDiff > 0) {
                    task.setSpeed(sizeDiff / timeDiff);
                    lastSize = task.getFinishedSize();
                }
                if (finished) task.setStatus(Task.STATUS_FINISHED);
                taskModel.updateTask(task);
                TaskSplitter s = task.getUnfinishedSplitter();
                if (s != null) {
                    DownloadThread t = new DownloadThread(task, savedFile, s);
                    threads.add(t);
                    t.start();
                    task.writeMessage("Task", Messages.TaskThread2_MSG_Start_Download_Thread + s.getName());
                }
                sleep(1000);
            }
            if (task.getStatus() == Task.STATUS_STOP || this.isInterrupted()) {
                stopAll();
                changeStatus(Task.STATUS_STOP);
                logger.info("下载停止");
                task.writeMessage("Task", Messages.TaskThread2_MSG_Task_Stop);
                return;
            }
            task.setFinishTime(System.currentTimeMillis());
            savedFile.close();
            task.renameSavedFile();
            changeStatus(Task.STATUS_FINISHED);
            logger.info("下载完成");
            task.writeMessage("Task", Messages.TaskThread2_MSG_Task_Finished);
        } catch (InterruptedException e) {
            e.printStackTrace();
            changeStatus(Task.STATUS_STOP);
        } catch (Exception e) {
            e.printStackTrace();
            changeStatus(Task.STATUS_ERROR);
        } finally {
            if (savedFile != null) {
                try {
                    savedFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            stopAll();
            task.clearMessage();
            taskModel.updateTask(task);
        }
    }

    private void stopAll() {
        for (Iterator it = task.getSplitters().iterator(); it.hasNext(); ) {
            TaskSplitter splitter = (TaskSplitter) it.next();
            splitter.setRun(false);
        }
        for (Iterator it = threads.iterator(); it.hasNext(); ) {
            DownloadThread thread = (DownloadThread) it.next();
            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        threads.clear();
    }
}
