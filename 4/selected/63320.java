package shu.cms.measure.meterapi.argyll;

import java.io.*;
import shu.util.log.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 * wrapper�[�c��:
 * spotread <=exec=> SpotreadWrapper    <=exec=>    caller
 *                                   <-ctr signal<-
 * �ǲά[�c��:
 * spotread <=exec=> caller
 *
 * �[�WSpotreadWrapper���ηN�b��, ���F�קK�b�ǲά[�c�U, �p�Gcaller�L�wĵ�����_��,
 * �|�y������spotread��java.exe��cpu�ϥβv����. ���F�ѨM�o�˪����D, �[�J�@Wrapper���J�䤤,
 * Wrapper�ݭn�w�ɱ���@�ӱ���T��, �n�O����T���L�[�S���ǰe�i��, �N�|�P�wcaller�w�g����.
 * ����Wrapper�|������spotread������java.exe, �M��A�۱�.
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public class SpotreadWrapper {

    /**
   *
   * <p>Title: Colour Management System</p>
   *
   * <p>Description: a Colour Management System by Java</p>
   * �۰ʵo�e Control Signal��Thread
   *
   * <p>Copyright: Copyright (c) 2008</p>
   *
   * <p>Company: skygroup</p>
   *
   * @author skyforce
   * @version 1.0
   */
    public static class SignalTransmitter extends Thread {

        private OutputStream os;

        public SignalTransmitter(OutputStream os) {
            this.os = os;
        }

        public void close() {
            closeFlag = true;
        }

        private boolean closeFlag = false;

        public void run() {
            try {
                while (true) {
                    if (closeFlag) {
                        return;
                    }
                    Thread.sleep(CtrlSigMaxWaitTime);
                    if (closeFlag) {
                        return;
                    }
                    os.write(CTRL_SIGNAL);
                    if (closeFlag) {
                        return;
                    }
                    os.flush();
                    Thread.yield();
                }
            } catch (InterruptedException ex) {
                Logger.log.error("", ex);
            } catch (IOException ex) {
                Logger.log.error("", ex);
            }
        }
    }

    private static final String ArgyllExec = Argyll.DIR + "\\spotread -yl";

    private static final String FakeExec = "java -jar " + Argyll.DIR + "\\SpotreadSimulator.jar  -yl";

    public static final char CTRL_SIGNAL = '%';

    private boolean fake = false;

    public SpotreadWrapper(boolean fake) {
        this.fake = fake;
        init();
    }

    private static final long CtrlSigMaxWaitTime = 3000;

    public static void main(String[] args) {
        boolean fake = false;
        if (args.length == 1 && args[0].equals("fake")) {
            fake = true;
        }
        SpotreadWrapper wrapper = new SpotreadWrapper(fake);
    }

    private long ctrlSigRecvTime;

    protected void connectAndListener(final Process p) {
        ctrlSigRecvTime = System.currentTimeMillis();
        final InputStream ais = p.getInputStream();
        final OutputStream aos = p.getOutputStream();
        final InputStream aes = p.getErrorStream();
        final InputStream bis = System.in;
        final OutputStream bos = System.out;
        final OutputStream bes = System.err;
        Thread listener = new Thread() {

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(CtrlSigMaxWaitTime);
                        long diff = System.currentTimeMillis() - ctrlSigRecvTime;
                        if (diff > CtrlSigMaxWaitTime) {
                            p.destroy();
                            System.exit(1);
                        }
                        Thread.yield();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        listener.start();
        Thread ist = new Thread() {

            public void run() {
                while (true) {
                    try {
                        int read = bis.read();
                        if (read == -1) {
                            return;
                        }
                        if (read == CTRL_SIGNAL) {
                            ctrlSigRecvTime = System.currentTimeMillis();
                            continue;
                        }
                        aos.write(read);
                        aos.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        ist.start();
        Thread ost = new Thread() {

            public void run() {
                while (true) {
                    try {
                        int read = ais.read();
                        if (read == -1) {
                            return;
                        }
                        bos.write(read);
                        bos.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        ost.start();
        Thread est = new Thread() {

            public void run() {
                while (true) {
                    try {
                        int read = aes.read();
                        if (read == -1) {
                            return;
                        }
                        bes.write(read);
                        bes.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        est.start();
    }

    protected void init() {
        Runtime rt = Runtime.getRuntime();
        Process p = null;
        String exec = fake ? FakeExec : ArgyllExec;
        try {
            p = rt.exec(exec);
            connectAndListener(p);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
