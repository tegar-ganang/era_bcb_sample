package jamsa.rcp.downloader.models;

import jamsa.rcp.downloader.Messages;
import jamsa.rcp.downloader.http.HttpClientUtils;
import jamsa.rcp.downloader.preference.PreferenceManager;
import jamsa.rcp.downloader.utils.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 下载线程
 * 
 * @author 朱杰
 * 
 */
public class DownloadThread extends Thread {

    private static final Logger logger = new Logger(DownloadThread.class);

    private PreferenceManager pm;

    private RandomAccessFile file;

    private Task task;

    /**
	 * 当前下载块
	 */
    private TaskSplitter splitter;

    private List threads;

    /**
	 * 构造器
	 * 
	 * @param task
	 *            任务对象
	 * @param file
	 *            下载数据将写入的文件
	 * @param splitter
	 *            本纯种下载的块信息
	 */
    public DownloadThread(Task task, RandomAccessFile file, TaskSplitter splitter) {
        this.file = file;
        this.task = task;
        this.splitter = splitter;
        pm = PreferenceManager.getInstance();
    }

    /**
	 * 获取线程完成量，这个量并不只是本次启动后完成的量
	 * 
	 * @return
	 */
    public long getFinishedSize() {
        return this.splitter.getFinished();
    }

    /**
	 * 获取远程文件输入流
	 * 
	 * @return
	 */
    public InputStream getInputStream() {
        Properties prop = new Properties();
        prop.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
        if (splitter.getEndPos() != 0) {
            prop.put("RANGE", "bytes=" + (splitter.getStartPos() + splitter.getFinished()) + "-" + splitter.getEndPos());
        } else {
            prop.put("RANGE", "bytes=" + splitter.getStartPos() + "-");
        }
        return HttpClientUtils.getInputStream(task.getFileUrl(), pm.getRetryTimes(), pm.getRetryDelay() * 1000, pm.getTimeout() * 1000, prop, "GET", task, splitter.getName());
    }

    public void run() {
        splitter.setRun(true);
        InputStream input = getInputStream();
        if (input == null) {
            task.writeMessage(splitter.getName(), Messages.DownloadThread_ERR_Can_Not_Get_Input_Stream);
            splitter.setRun(false);
            return;
        }
        try {
            task.writeMessage(splitter.getName(), Messages.DownloadThread_MSG_Reading);
            int size = 0;
            byte[] buf = new byte[2048];
            while ((size = input.read(buf, 0, buf.length)) > 0 && splitter.isRun() && !this.isInterrupted() && (((splitter.getFinished() + splitter.getStartPos()) < splitter.getEndPos()) || splitter.getEndPos() == 0)) {
                int pos = Integer.parseInt((splitter.getStartPos() + splitter.getFinished()) + "");
                synchronized (file) {
                    file.seek(pos);
                    file.write(buf, 0, size);
                }
                splitter.setFinished(splitter.getFinished() + size);
                Arrays.fill(buf, (byte) 0);
                sleep(10);
            }
            if (splitter.isFinish()) {
                logger.info(splitter.getName() + "线程任务完成!");
                task.writeMessage(splitter.getName(), Messages.DownloadThread_MSG_Thread_Task_Finished);
            } else {
                logger.info(splitter.getName() + "线程停止!");
                task.writeMessage(splitter.getName(), Messages.DownloadThread_MSG_Thread_Stopped);
            }
        } catch (IOException e) {
            task.writeMessage(splitter.getName(), Messages.DownloadThread_ERR_Stream_Exception + e.getLocalizedMessage());
            return;
        } catch (InterruptedException e) {
            task.writeMessage(splitter.getName(), Messages.DownloadThread_ERR_Thread_Interrupted);
            return;
        } finally {
            splitter.setRun(false);
            try {
                if (input != null) input.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
