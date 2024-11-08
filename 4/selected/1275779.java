package com.taobao.common.smonitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**   
 * @author xiaoxie   
 * @create time��2010-4-6 ����06:06:24   
 * @description  
 */
public class SmonitorAdaptorServlet extends HttpServlet {

    private static final String GBK = "GBK";

    private static final String YYYY_MM_DD = "'.'yyyy-MM-dd";

    private static final String M_N = "%d %m%n";

    private static Logger log = Logger.getLogger(SmonitorAdaptorServlet.class);

    private static Logger mbeanLog = Logger.getLogger("logMbean");

    private static final String DIR_NAME = "logs";

    private static final String FILE_NAME = "mbean.log";

    private static final String USER_HOME = "user.home";

    private static final String WRITETHREAD_NAME = "CORE_MONITOR";

    private static final long serialVersionUID = 1L;

    /** д����ݵ��߳� */
    private static Thread writeThread = null;

    private static final ReentrantLock timerLock = new ReentrantLock();

    private static final Condition condition = timerLock.newCondition();

    /** ��־���ͣ�1�����ӳ� 2���̳߳� 3���ڴ� ��Ĭ�ϴ�ӡ���ӳغ��̳߳�*/
    private static String type = "1,2,3";

    /** ���Դ���˹ؼ��� �磺DB1DataSouce,DB2DataSouce */
    private static String dataSource;

    /** �̹߳��˹ؼ��� �磺ajp,http */
    private static String threadNameKeys;

    /** <p>
	 * д����ݵ�ʱ����.(ie.��ݲ���ʱ��)
	 * </p>
	 */
    private static long intervalTime = 20000L;

    public void init() throws ServletException {
        initParameter();
        final List<MonitorTask> monitorTasks = new ArrayList<MonitorTask>();
        initMonitorTasks(monitorTasks);
        initWriter();
        if (null != writeThread) {
            try {
                writeThread.interrupt();
            } catch (Exception e) {
                log.error("interrupt write thread error", e);
            }
        }
        writeThread = new Thread(new Runnable() {

            public void run() {
                while (true) {
                    timerLock.lock();
                    try {
                        condition.await(intervalTime, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("wait error", e);
                    } finally {
                        timerLock.unlock();
                    }
                    for (MonitorTask task : monitorTasks) {
                        try {
                            task.start();
                        } catch (java.lang.Throwable t) {
                            log.error("task error" + task.getClass(), t);
                        }
                    }
                }
            }
        });
        writeThread.setName(WRITETHREAD_NAME);
        writeThread.start();
    }

    /**
	 * @param monitorTasks
	 */
    private void initMonitorTasks(final List<MonitorTask> monitorTasks) {
        if (type.indexOf('1') != -1) {
            if (dataSource != null && dataSource.length() > 0) {
                monitorTasks.add(new ConpoolMonitorTask(mbeanLog, dataSource.split(",")));
            } else {
                monitorTasks.add(new ConpoolMonitorTask(mbeanLog, null));
            }
        }
        if (type.indexOf('2') != -1) {
            ThreadPoolMonitorTask t = new ThreadPoolMonitorTask(mbeanLog, threadNameKeys);
            monitorTasks.add(t);
        }
        if (type.indexOf('3') != -1) {
            monitorTasks.add(new MemoryMonitorTask(mbeanLog));
        }
    }

    private void initParameter() {
        String initType = getInitParameter("type");
        if (initType != null) {
            type = initType;
        }
        String initintervalTime = getInitParameter("intervalTime");
        if (initintervalTime != null) {
            intervalTime = Long.parseLong(initintervalTime);
        }
        dataSource = getInitParameter("dataSource");
        threadNameKeys = getInitParameter("threadNameKeys");
    }

    private void initWriter() {
        PatternLayout layout = new PatternLayout(M_N);
        String userHome = System.getProperty(USER_HOME);
        if (!userHome.endsWith(File.separator)) {
            userHome += File.separator;
        }
        String path = userHome + DIR_NAME + File.separator;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = path + FILE_NAME;
        FileAppender appender = null;
        try {
            appender = new DailyRollingFileAppender(layout, fileName, YYYY_MM_DD);
            appender.setAppend(true);
            appender.setEncoding(GBK);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (appender != null) {
            mbeanLog.removeAllAppenders();
            mbeanLog.addAppender(appender);
        }
        mbeanLog.setLevel(Level.INFO);
        mbeanLog.setAdditivity(false);
    }

    public void destroy() {
        if (null != writeThread) {
            try {
                writeThread.interrupt();
            } catch (Exception e) {
                log.error("interrupt write thread error", e);
            }
        }
    }
}
