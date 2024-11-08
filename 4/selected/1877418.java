package innerbus.logtrap.biz;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import com.innerbus.basis.util.Inner_StringUtil;
import innerbus.logtrap.common.*;
import innerbus.logtrap.resource.*;
import innerbus.logtrap.event.*;
import innerbus.logtrap.util.*;

public class Receiver implements Runnable, SchedulerListener, FileOutputHandler {

    private SyslogScheduler scheduler;

    private InetSocketAddress inetSocketAddress;

    private boolean isOnThread = true;

    private ThreadPoolExecutor threadPoolExecutor;

    private ByteBufferPool bufferPool;

    private FileChannel fileChannel;

    private SimpleDateFormat sf;

    private UnitTask unitTask;

    private ProjectHandler handler;

    private DatagramChannel udpChannel = null;

    public Receiver(ProjectHandler handler) throws IOException {
        this.handler = handler;
        if (handler.iSYSScheduleRule == ProjectHandler.TIME) {
            scheduler = new SyslogScheduler(handler, 0, 0);
            scheduler.addSchedulerListener(this);
            scheduler.startHour();
        } else if (handler.iSYSScheduleRule == ProjectHandler.DAY) {
            scheduler = new SyslogScheduler(handler, 0, 0, 0);
            scheduler.addSchedulerListener(this);
            scheduler.startDaily();
        }
        sf = new SimpleDateFormat(handler.sDateFormat);
        String ip = handler.sSYSIP;
        int port = handler.iSYSPort;
        inetSocketAddress = new InetSocketAddress(port);
        threadPoolExecutor = ThreadPoolExecutor.getInstance();
        threadPoolExecutor.init(handler);
        bufferPool = new ByteBufferPool();
        bufferPool.init(handler);
        activate();
        _LOG("Receiver has been initialized.");
    }

    Thread thread;

    MISThread misThread;

    public void activate() {
        misThread = new MISThread();
        misThread.start();
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
    }

    int count = 0;

    public void run() {
        try {
            udpChannel = DatagramChannel.open();
            udpChannel.socket().bind(inetSocketAddress);
            System.out.println("udpChannel.isBlocking() : " + udpChannel.isBlocking());
            ArrayList arIP = new ArrayList();
            List arIPTmp;
            String oldIP = "";
            while (isOnThread && !thread.isInterrupted()) {
                final ByteBuffer[] buffers = bufferPool.getBuffer();
                for (int i = 0, size = buffers.length; i < size; i++) {
                    SocketAddress sa = udpChannel.receive(buffers[i]);
                    String ip = sa.toString();
                    buffers[i].put((byte) '\n');
                    arIP.add(ip);
                    if (!oldIP.equals(ip)) getChannel(ip);
                }
                arIPTmp = (List) arIP.clone();
                arIP.clear();
                threadPoolExecutor.execute(new UnitTask(bufferPool, buffers, Receiver.this, arIPTmp));
            }
        } catch (Exception e) {
            e.printStackTrace();
            _LOG("[Receiver][run] udpChannel Close.");
        }
    }

    int iMPS;

    public int getMPS() {
        return iMPS;
    }

    public void setOnThread(boolean isOnThread) {
        this.isOnThread = isOnThread;
        if (isOnThread == false) {
            try {
                if (thread != null) {
                    thread.interrupt();
                    thread = null;
                }
                if (misThread != null) {
                    isStop = true;
                    misThread.interrupt();
                    misThread = null;
                }
                if (bufferPool != null) {
                    bufferPool.clear();
                }
                if (scheduler != null) {
                    scheduler.removeSchedulerListener(this);
                    scheduler.stop();
                    scheduler = null;
                }
                if (udpChannel != null) {
                    udpChannel.socket().close();
                    udpChannel.close();
                    udpChannel = null;
                }
                ProjectListXmlParser.getInstance().modifyMPS(handler.sName, 0);
                System.err.println(">>>>>>>>>Syslogd Stop<<<<<<<<<<");
                System.err.println("ProjectName : " + handler.sName);
                System.err.println(">>>>>>>>>Syslogd Stop<<<<<<<<<<");
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (udpChannel != null) udpChannel.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public boolean isOnThread() {
        return isOnThread;
    }

    String sPath;

    Map hmFileChannel = new HashMap();

    private void getChannel(String ip) throws IOException {
        if (!hmFileChannel.containsKey(ip)) {
            String ipAddr = ip.substring(0, ip.indexOf(":"));
            if (handler.sSYSSavePath.indexOf("\\") != -1) {
                handler.sSYSSavePath = handler.sSYSSavePath.replace('\\', '/');
            }
            if (handler.sSYSSavePath.endsWith("/")) sPath = handler.sSYSSavePath + handler.sName + ipAddr + "/" + sf.format(new Date()) + "_" + handler.sName + ".log"; else sPath = handler.sSYSSavePath + "/" + handler.sName + ipAddr + "/" + sf.format(new Date()) + "_" + handler.sName + ".log";
            File file = new File(sPath);
            if (!new File(file.getParent()).exists()) new File(file.getParent()).mkdirs();
            FileOutputStream fos = new FileOutputStream(file, true);
            FileChannel fc = fos.getChannel();
            hmFileChannel.put(ip, fc);
        }
    }

    public FileChannel getFileChannel(Object ip) {
        return (FileChannel) hmFileChannel.get(ip);
    }

    public Map getFileList() {
        return hmFileChannel;
    }

    public void fireScheduler(SchedulerEvent event) {
        try {
            synchronized (hmFileChannel) {
                _LOG(">>>>> File Change <<<<<");
                Iterator iter = hmFileChannel.keySet().iterator();
                FileChannel fc;
                while (iter.hasNext()) {
                    fc = (FileChannel) hmFileChannel.get(iter.next());
                    fc.close();
                }
                hmFileChannel.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    long totPacket;

    boolean isStop;

    SimpleDateFormat sfHH = new SimpleDateFormat("yyyyMMdd");

    SimpleDateFormat sfMM = new SimpleDateFormat("HH:mm");

    class MISThread extends Thread {

        long lStartTime = 0;

        long iEndTime = 0;

        float fTime = 0;

        String mis, misPath;

        String sData;

        RandomAccessFile rf;

        public MISThread() {
            String webPath = null;
            try {
                webPath = ProjectListXmlParser.getInstance().getRootPath();
            } catch (Exception ex) {
                webPath = System.getProperty("user.dir");
            }
            misPath = webPath + "/mis/" + handler.sName;
            if (!new File(misPath).exists()) new File(misPath).mkdirs();
            try {
                mis = misPath + "/" + sfHH.format(new Date()) + ".mis";
                rf = new RandomAccessFile(mis, "rw");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            lStartTime = System.currentTimeMillis();
            this.setPriority(Thread.MIN_PRIORITY);
        }

        public void run() {
            try {
                boolean isCheck = false;
                long offset = 0;
                while (!isStop && !this.isInterrupted()) {
                    offset = 0;
                    if (count != 0) {
                        try {
                            mis = misPath + "/" + sfHH.format(new Date()) + ".mis";
                            if (!new File(misPath).exists()) {
                                new File(misPath).mkdirs();
                            }
                            if (!new File(mis).exists()) {
                                if (rf != null) rf.close();
                                rf = new RandomAccessFile(mis, "rw");
                            }
                            isCheck = false;
                            iEndTime = (System.currentTimeMillis() - lStartTime);
                            fTime = (float) iEndTime / 1000;
                            String sMPS = ((int) (count / fTime)) + "";
                            switch(sMPS.length()) {
                                case 1:
                                    sMPS = "0000" + sMPS;
                                    break;
                                case 2:
                                    sMPS = "000" + sMPS;
                                    break;
                                case 3:
                                    sMPS = "00" + sMPS;
                                    break;
                                case 4:
                                    sMPS = "0" + sMPS;
                                    break;
                            }
                            rf.seek(offset);
                            while ((sData = rf.readLine()) != null) {
                                if (sData.startsWith(sfMM.format(new Date()))) {
                                    rf.seek(offset);
                                    rf.write((sfMM.format(new Date()) + " " + sMPS + "\n").getBytes());
                                    isCheck = true;
                                    break;
                                }
                                offset = rf.getFilePointer();
                            }
                            if (isCheck == false) {
                                rf.write((sfMM.format(new Date()) + " " + sMPS + "\n").getBytes());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        try {
                            mis = misPath + "/" + sfHH.format(new Date()) + ".mis";
                            if (!new File(misPath).exists()) {
                                new File(misPath).mkdirs();
                            }
                            if (!new File(mis).exists()) {
                                if (rf != null) rf.close();
                                rf = new RandomAccessFile(mis, "rw");
                            }
                            isCheck = false;
                            String sMPS = "00000";
                            rf.seek(offset);
                            while ((sData = rf.readLine()) != null) {
                                if (sData.startsWith(sfMM.format(new Date()))) {
                                    offset = rf.getFilePointer() - (sData.getBytes().length + 1);
                                    rf.seek(offset);
                                    rf.write((sfMM.format(new Date()) + " " + sMPS + "\n").getBytes());
                                    isCheck = true;
                                    break;
                                }
                            }
                            if (isCheck == false) {
                                rf.write((sfMM.format(new Date()) + " " + sMPS + "\n").getBytes());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    lStartTime = System.currentTimeMillis();
                    count = 0;
                    try {
                        Thread.sleep(10000);
                    } catch (Exception ex) {
                        System.out.println("[MISThread][run] : " + ex.getMessage());
                    }
                }
            } catch (Exception ie) {
            }
        }
    }

    private static void _LOG(String s) {
        System.out.println(s);
    }

    public static void main(String[] args) {
        try {
            String project = args[0];
            if (project.indexOf("*") != -1) project = Inner_StringUtil.replaceString(project, "*", " ");
            System.out.println(">>>>>>>>>>>>>>Receiver Name : " + project);
            System.setProperty("user.dir", args[1]);
            ProjectListXmlParser.getInstance().PATH = args[1] + "/LTProjectList.xml";
            ProjectHandler handler = ProjectListXmlParser.getInstance().find(project);
            new Receiver(handler);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
