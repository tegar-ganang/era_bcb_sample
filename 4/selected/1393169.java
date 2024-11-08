package jamsa.rcp.downloader.models;

import jamsa.rcp.downloader.Messages;
import jamsa.rcp.downloader.utils.FileUtils;
import jamsa.rcp.downloader.views.IConsoleWriter;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * 下载任务对象
 * 
 * @author 朱杰
 * 
 */
public class Task extends Observable implements IConsoleWriter, Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -9206186502703680217L;

    /**
	 * 任务状态常量
	 */
    public static final int STATUS_STOP = 0;

    public static final int STATUS_RUNNING = 1;

    public static final int STATUS_FINISHED = 3;

    public static final int STATUS_ERROR = 4;

    private static final long BLOCK_MIN_SIZE = 50000;

    public static final String FILENAME_DOWNLOAD_SUFFIX = ".GET";

    public static final String FILENAME_SUFFIX = "_1";

    private String fileName;

    private String fileUrl;

    private String pageUrl;

    private String filePath;

    private Category category;

    private long fileSize;

    private String fileType = "";

    private long beginTime;

    private long finishTime;

    private int status;

    private long totalTime;

    private long finishedSize;

    private long speed;

    private String memo;

    private int blocks = 5;

    private List splitters = new ArrayList(10);

    private boolean deleted = false;

    private int start = 0;

    public static final int START_AUTO = 0;

    public static final int START_MANUAL = 1;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public long getFinishedSize() {
        return finishedSize;
    }

    public void setFinishedSize(long finishedSize) {
        this.finishedSize = finishedSize;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public List getSplitters() {
        return splitters;
    }

    public void setSplitters(List splitters) {
        this.splitters = splitters;
    }

    public int getBlocks() {
        return blocks;
    }

    public void setBlocks(int blocks) {
        this.blocks = blocks;
    }

    public void addSplitter(TaskSplitter splitter) {
        this.splitters.add(splitter);
    }

    public void removeSplitter(TaskSplitter splitter) {
        this.splitters.remove(splitter);
    }

    public int getStatus() {
        return status;
    }

    /**
	 * 设置任务状态，通知监听者
	 * 
	 * @param status
	 */
    public void setStatus(int status) {
        if (this.status != status) {
            this.status = status;
            if (this.status == STATUS_ERROR || this.status == this.STATUS_STOP) {
                this._stopAllSplitters();
            }
            this.setChanged();
            this.notifyObservers();
        }
    }

    /**
	 * 获取任务平均速度(k/s)
	 * 
	 * @return
	 */
    public long getAverageSpeed() {
        if (totalTime != 0) return this.finishedSize / totalTime;
        return 0;
    }

    /**
	 * 重置任务
	 * 
	 */
    public void reset() {
        this.resetSplitters();
        this.setBeginTime(0);
        this.setFileSize(0);
        this.setFinishedSize(0);
        this.setTotalTime(0);
        this.setStatus(Task.STATUS_STOP);
    }

    /**
	 * 重置线程信息
	 * 
	 */
    public void resetSplitters() {
        splitters.clear();
    }

    /**
	 * 终端消息
	 */
    private Map messages = Collections.synchronizedMap(new LinkedHashMap(6));

    public Map getMessages() {
        return messages;
    }

    /**
	 * 向线程终端输出日志
	 * 
	 * @param threadName
	 *            线程标识
	 * @param message
	 *            日志内容
	 */
    public void writeMessage(String threadName, String message) {
        if (messages.get(threadName) == null) {
            List msgs = new ArrayList(20);
            msgs.add(message);
            messages.put(threadName, msgs);
        } else {
            List msgs = (List) messages.get(threadName);
            if (msgs.size() >= 20) msgs.clear();
            msgs.add(message);
        }
        setChanged();
        notifyObservers(new String[] { Messages.ConsoleView_Thread + threadName, message });
    }

    /**
	 * 清除消息
	 * 
	 */
    public void clearMessage() {
        messages.clear();
        setChanged();
        notifyObservers();
    }

    /**
	 * 块检查，应该在每次下载之前调用
	 */
    public void checkBlocks() {
        if (this.splitters.isEmpty()) {
            this.split();
            return;
        }
        if (this.splitters.size() < this.blocks) {
            int diff = this.blocks - this.splitters.size();
            for (int i = 0; i < Math.abs(diff); i++) {
                this.addSplitter();
            }
        }
    }

    /**
	 * 减少任务块
	 * 
	 * @return
	 */
    public TaskSplitter removeSplitter() {
        for (Iterator it = splitters.iterator(); it.hasNext(); ) {
            TaskSplitter splitter = (TaskSplitter) it.next();
            if (!splitter.isFinish() && splitter.isRun()) {
                splitter.setRun(true);
                return splitter;
            }
        }
        return null;
    }

    /**
	 * 添加新的任务块
	 * 
	 * @return 新增加的任务块对象，或者null(表示未添加，由于文件未完成部分太小)
	 */
    public TaskSplitter addSplitter() {
        TaskSplitter ret = null;
        if (getSplitters().isEmpty()) this.split();
        if (getSplitters().isEmpty() || getSplitters().size() == 1) return null;
        for (Iterator it = getSplitters().iterator(); it.hasNext(); ) {
            TaskSplitter splitter = (TaskSplitter) it.next();
            long unfinished = (splitter.getEndPos() - splitter.getStartPos() - splitter.getFinished());
            long spliteBlock = unfinished / 2;
            if (spliteBlock > BLOCK_MIN_SIZE) {
                long newEndPos = splitter.getStartPos() + splitter.getFinished() + spliteBlock;
                ret = new TaskSplitter(newEndPos, splitter.getEndPos(), 0, getSplitters().size() + "");
                break;
            }
        }
        addSplitter(ret);
        this.blocks++;
        return ret;
    }

    /**
	 * 按blocks属性自动分割任务
	 * 
	 */
    private void split() {
        int block = this.blocks;
        long fileSize = this.fileSize;
        if (fileSize == 0 || block == 0) {
            TaskSplitter splitter = new TaskSplitter(0, 0, 0, getSplitters().size() + "");
            this.addSplitter(splitter);
            this.blocks = 1;
            return;
        }
        if (getSplitters().isEmpty()) {
            writeMessage("Task", Messages.Task_MSG_Split_Task);
            long blockSize = fileSize / block;
            if (blockSize < BLOCK_MIN_SIZE) {
                this.blocks = 0;
                for (int i = 0; i < ++block; i++) {
                    boolean finished = false;
                    long startPos = i * BLOCK_MIN_SIZE;
                    long endPos = (i + 1) * BLOCK_MIN_SIZE;
                    if (endPos >= fileSize) {
                        endPos = fileSize;
                        finished = true;
                    }
                    addSplitter(new TaskSplitter(startPos, endPos, 0, getSplitters().size() + ""));
                    this.blocks++;
                    if (finished) break;
                }
                return;
            }
            this.blocks = 0;
            for (int i = 0; i < (block - 1); i++) {
                addSplitter(new TaskSplitter(i * blockSize, (i + 1) * blockSize, 0, getSplitters().size() + ""));
                this.blocks++;
            }
            addSplitter(new TaskSplitter((block - 1) * blockSize, fileSize, 0, getSplitters().size() + ""));
            this.blocks++;
            return;
        }
    }

    /**
	 * 下载完成后，重命名文件
	 * 
	 * @param savedFile
	 */
    public void renameSavedFile() {
        String finalFileName = getFilePath() + File.separator + getFileName();
        while (FileUtils.existsFile(finalFileName)) {
            String name = getFileName();
            int length = name.length();
            int idx = name.lastIndexOf(".");
            name = name.substring(0, idx) + FILENAME_SUFFIX + name.substring(idx, length);
            setFileName(name);
            finalFileName = getFilePath() + File.separator + getFileName();
        }
        getTempSavedFile().renameTo(new File(finalFileName));
    }

    /**
	 * 获取保存的临时文件名
	 * 
	 * @return
	 */
    public File getTempSavedFile() {
        FileUtils.createDirectory(getFilePath());
        writeMessage("Task", Messages.Task_MSG_Check_Create_Directory + getFilePath());
        String fileName = getFilePath() + File.separator + getFileName();
        while (FileUtils.existsFile(fileName)) {
            String name = getFileName();
            int length = name.length();
            int idx = name.lastIndexOf(".");
            name = name.substring(0, idx) + FILENAME_SUFFIX + name.substring(idx, length);
            setFileName(name);
            fileName = getFilePath() + File.separator + getFileName();
        }
        fileName += FILENAME_DOWNLOAD_SUFFIX;
        return new File(fileName);
    }

    /**
	 * 获取处于运行状态的块数量
	 * 
	 * @return
	 */
    public int getRunBlocks() {
        int ret = 0;
        for (Iterator iter = this.splitters.iterator(); iter.hasNext(); ) {
            TaskSplitter splitter = (TaskSplitter) iter.next();
            if (splitter.isRun()) ret++;
        }
        return ret;
    }

    /**
	 * 获取一个未完成的块
	 * 
	 * @return
	 */
    public TaskSplitter getUnfinishedSplitter() {
        if (getRunBlocks() < blocks) {
            for (Iterator iter = splitters.iterator(); iter.hasNext(); ) {
                TaskSplitter s = (TaskSplitter) iter.next();
                if (!s.isFinish() && !s.isRun()) {
                    return s;
                }
            }
        }
        return null;
    }

    private void _stopAllSplitters() {
        for (Iterator it = splitters.iterator(); it.hasNext(); ) {
            TaskSplitter splitter = (TaskSplitter) it.next();
            splitter.setRun(false);
        }
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }
}
