package com.taobao.top.analysis.jobmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import com.taobao.top.analysis.AnalysisConstants;
import com.taobao.top.analysis.TopAnalysisConfig;
import com.taobao.top.analysis.util.AnalyzerFilenameFilter;
import com.taobao.top.analysis.util.FTPUtil;
import com.taobao.top.analysis.worker.PullFileJobWorker;

/**
 * 默认的任务管理实现
 * 
 * @author fangweng
 * 
 */
public class DefaultJobManager implements IJobManager {

    private final Log logger = LogFactory.getLog(DefaultJobManager.class);

    /**
	 * 全局配置
	 */
    protected TopAnalysisConfig topAnalyzerConfig;

    /**
	 * 文件切割线程池
	 */
    protected ExecutorService splitFileJobExecuter;

    public void setTopAnalyzerConfig(TopAnalysisConfig topAnalyzerConfig) {
        this.topAnalyzerConfig = topAnalyzerConfig;
    }

    public DefaultJobManager() {
    }

    @Override
    public void init() {
        if (splitFileJobExecuter != null) {
            splitFileJobExecuter.shutdown();
            splitFileJobExecuter = null;
        }
        splitFileJobExecuter = Executors.newFixedThreadPool(topAnalyzerConfig.getSplitWorkerNum());
    }

    @Override
    public boolean deleteJobData(String[] resource) {
        boolean result = true;
        if (resource == null || (resource != null && resource.length == 0)) return result;
        try {
            if (topAnalyzerConfig.getJobFileFrom().equals(AnalysisConstants.JOBFILEFROM_FTP)) {
                FTPClient ftp = FTPUtil.getFtpClient(topAnalyzerConfig);
                for (String r : resource) ftp.deleteFile(r);
            }
        } catch (Exception ex) {
            logger.error(ex, ex);
            result = false;
        }
        return result;
    }

    @Override
    public List<String> getJobs() {
        List<String> jobs = new ArrayList<String>();
        if (topAnalyzerConfig.getJobFileFrom().equals(AnalysisConstants.JOBFILEFROM_FTP)) {
            try {
                PullFileJobWorker pullFileJobWorker = new PullFileJobWorker();
                pullFileJobWorker.setUsername("pubftp");
                pullFileJobWorker.setPassword("look");
                pullFileJobWorker.setTargetDir(topAnalyzerConfig.getInput());
                pullFileJobWorker.setFilelist(null);
                pullFileJobWorker.setPullflag(false);
                pullFileJobWorker.setTopAnalyzerConfig(topAnalyzerConfig);
                Thread worker = new Thread(pullFileJobWorker);
                worker.start();
                worker.join();
                Map<String, String> jobfiles = pullFileJobWorker.getJobfiles();
                if (jobfiles.size() > 0) {
                    jobs.addAll(jobfiles.keySet());
                }
            } catch (Exception ex) {
                logger.error(ex, ex);
            }
        } else if (topAnalyzerConfig.getJobFileFrom().equals(AnalysisConstants.JOBFILEFROM_MACHINE)) {
            String[] arrJobs = topAnalyzerConfig.getJobs().split(",");
            if (topAnalyzerConfig.getResourcePattern() != null) {
                for (String j : arrJobs) jobs.add(topAnalyzerConfig.getResourcePattern().replace("$job$", j));
            } else for (String j : arrJobs) jobs.add(j);
        }
        return jobs;
    }

    @Override
    public String[] pullJobData(String resources) {
        String[] result = null;
        if (topAnalyzerConfig.getJobFileFrom().equals(AnalysisConstants.JOBFILEFROM_FTP)) {
            File[] _files = null;
            try {
                Calendar calendar = Calendar.getInstance();
                File sourceDir = new File(new StringBuilder(topAnalyzerConfig.getInput()).append(File.separator).append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString());
                PullFileJobWorker pullFileJobWorker = new PullFileJobWorker();
                pullFileJobWorker.setUsername("pubftp");
                pullFileJobWorker.setPassword("look");
                pullFileJobWorker.setTargetDir(topAnalyzerConfig.getInput());
                pullFileJobWorker.setFilelist(resources);
                pullFileJobWorker.setTopAnalyzerConfig(topAnalyzerConfig);
                Thread worker = new Thread(pullFileJobWorker);
                worker.start();
                worker.join();
                String currentInputTarget = sourceDir.getAbsolutePath();
                if (currentInputTarget == null || "".equals(currentInputTarget)) {
                    logger.error("currentInputTarget is null!");
                    throw new java.lang.RuntimeException("currentInputTarget is null!");
                }
                File in = new File(currentInputTarget);
                if (!in.exists()) {
                    logger.error("input is not exist!");
                    throw new java.lang.RuntimeException("input is not exist!");
                }
                logger.info("JobManager start splitDataFile...");
                long start = System.currentTimeMillis();
                if (in.isDirectory()) {
                    _files = in.listFiles(new AnalyzerFilenameFilter(".log"));
                    if (_files.length > 0) {
                        final CountDownLatch countDown = new CountDownLatch(_files.length);
                        for (int i = 0; i < _files.length; i++) {
                            final File file = _files[i];
                            splitFileJobExecuter.execute(new Thread() {

                                public void run() {
                                    splitDataFile(file, topAnalyzerConfig.getMaxFileBlockSize());
                                    countDown.countDown();
                                }
                            });
                        }
                        countDown.await();
                        _files = in.listFiles(new AnalyzerFilenameFilter(".log"));
                    }
                } else {
                    if (in.isFile()) {
                        _files = new File[1];
                        _files[0] = in;
                    }
                }
                if (logger.isInfoEnabled() && _files != null) logger.info(new StringBuilder("JobManager split file end...").append(" file Number :").append(_files.length).append(", time consume: ").append((System.currentTimeMillis() - start) / 1000).toString());
                if (_files != null && _files.length > 0) {
                    result = new String[_files.length];
                    int i = 0;
                    for (File f : _files) {
                        result[i] = "file:" + f.getAbsolutePath();
                        i += 1;
                    }
                } else {
                    result = new String[0];
                }
            } catch (Exception ex) {
                logger.error(ex, ex);
            }
        } else if (topAnalyzerConfig.getJobFileFrom().equals(AnalysisConstants.JOBFILEFROM_MACHINE)) {
            if (resources != null && (resources.indexOf(",") > 0)) {
                result = resources.split(",");
            } else {
                result = new String[1];
                result[0] = resources;
            }
        }
        return result;
    }

    @Override
    public void destory() {
        if (splitFileJobExecuter != null) {
            if (!splitFileJobExecuter.isShutdown()) splitFileJobExecuter.shutdown();
        }
    }

    /**
	 * 切割文件
	 * 
	 * @param file
	 * @param blockSize
	 */
    private boolean splitDataFile(File file, int blockSize) {
        RandomAccessFile parentFile = null;
        long filenum = 0;
        boolean isSuccess = true;
        try {
            long index = (long) blockSize * 1024 * 1024;
            long fileSize = file.length();
            File[] subFiles = null;
            if (fileSize <= (index + 1024)) {
                return isSuccess;
            }
            if (fileSize % index == 0) filenum = fileSize / index; else filenum = fileSize / index + 1;
            String sourceFile = file.getAbsolutePath();
            parentFile = new RandomAccessFile(sourceFile, "r");
            String fileName = sourceFile.substring(0, sourceFile.lastIndexOf("."));
            subFiles = new File[(int) filenum];
            for (int i = 0; i < filenum; i++) {
                String _tempFileName = new StringBuilder().append(fileName).append("sub").append(i).append(".log").toString();
                new File(_tempFileName).createNewFile();
                subFiles[i] = new File(_tempFileName);
            }
            int beg = 0;
            for (int i = 0; i < filenum; i++) {
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(subFiles[i]);
                    FileChannel inChannel = parentFile.getChannel();
                    FileChannel outChannel = outputStream.getChannel();
                    long remain;
                    if (fileSize - beg > index) remain = index; else remain = fileSize - beg;
                    while (remain > 0) {
                        if (remain > 5 * 1024 * 1024) {
                            inChannel.transferTo(beg, 5 * 1024 * 1024, outChannel);
                            remain -= 5 * 1024 * 1024;
                            beg += 5 * 1024 * 1024;
                        } else {
                            inChannel.transferTo(beg, remain, outChannel);
                            beg += remain;
                            break;
                        }
                    }
                    if (i < filenum - 1) {
                        parentFile.seek(beg);
                        String tail = parentFile.readLine();
                        if (tail == null) {
                            for (int j = i + 1; j < filenum; j++) {
                                subFiles[j].delete();
                            }
                            break;
                        }
                        beg += tail.length() + 2;
                        outputStream.write(tail.getBytes());
                    }
                } finally {
                    try {
                        if (outputStream != null) outputStream.close();
                    } catch (Exception e) {
                        logger.error(e, e);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error(ex, ex);
            isSuccess = false;
        } finally {
            if (parentFile != null) try {
                parentFile.close();
            } catch (IOException e) {
                logger.error(e, e);
            }
            if (isSuccess && filenum > 0) file.renameTo(new File(new StringBuilder().append(file.getAbsolutePath()).append(".bk").toString()));
        }
        return isSuccess;
    }
}
