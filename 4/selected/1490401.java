package org.apache.nutch.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CommandRunner {

    private boolean _waitForExit = true;

    private String _command;

    private int _timeout = 10;

    private InputStream _stdin;

    private OutputStream _stdout;

    private OutputStream _stderr;

    private static final int BUF = 4096;

    private int _xit;

    private Throwable _thrownError;

    private CyclicBarrier _barrier;

    public int getExitValue() {
        return _xit;
    }

    public void setCommand(String s) {
        _command = s;
    }

    public String getCommand() {
        return _command;
    }

    public void setInputStream(InputStream is) {
        _stdin = is;
    }

    public void setStdOutputStream(OutputStream os) {
        _stdout = os;
    }

    public void setStdErrorStream(OutputStream os) {
        _stderr = os;
    }

    public void evaluate() throws IOException {
        this.exec();
    }

    /**
   *
   * @return process exit value (return code) or -1 if timed out.
   * @throws IOException
   */
    public int exec() throws IOException {
        Process proc = Runtime.getRuntime().exec(_command);
        _barrier = new CyclicBarrier(3 + ((_stdin != null) ? 1 : 0));
        PullerThread so = new PullerThread("STDOUT", proc.getInputStream(), _stdout);
        so.setDaemon(true);
        so.start();
        PullerThread se = new PullerThread("STDERR", proc.getErrorStream(), _stderr);
        se.setDaemon(true);
        se.start();
        PusherThread si = null;
        if (_stdin != null) {
            si = new PusherThread("STDIN", _stdin, proc.getOutputStream());
            si.setDaemon(true);
            si.start();
        }
        boolean _timedout = false;
        long end = System.currentTimeMillis() + _timeout * 1000;
        try {
            if (_timeout == 0) {
                _barrier.await();
            } else {
                _barrier.await(_timeout, TimeUnit.SECONDS);
            }
        } catch (TimeoutException ex) {
            _timedout = true;
        } catch (BrokenBarrierException bbe) {
        } catch (InterruptedException e) {
        }
        if (si != null) {
            si.interrupt();
        }
        so.interrupt();
        se.interrupt();
        _xit = -1;
        if (!_timedout) {
            if (_waitForExit) {
                do {
                    try {
                        Thread.sleep(1000);
                        _xit = proc.exitValue();
                    } catch (InterruptedException ie) {
                        if (Thread.interrupted()) {
                            break;
                        } else {
                            continue;
                        }
                    } catch (IllegalThreadStateException iltse) {
                        continue;
                    }
                    break;
                } while (!(_timedout = (System.currentTimeMillis() > end)));
            } else {
                try {
                    _xit = proc.exitValue();
                } catch (IllegalThreadStateException iltse) {
                    _timedout = true;
                }
            }
        }
        if (_waitForExit) {
            proc.destroy();
        }
        return _xit;
    }

    public Throwable getThrownError() {
        return _thrownError;
    }

    private class PumperThread extends Thread {

        private OutputStream _os;

        private InputStream _is;

        private boolean _closeInput;

        protected PumperThread(String name, InputStream is, OutputStream os, boolean closeInput) {
            super(name);
            _is = is;
            _os = os;
            _closeInput = closeInput;
        }

        public void run() {
            try {
                byte[] buf = new byte[BUF];
                int read = 0;
                while (!isInterrupted() && (read = _is.read(buf)) != -1) {
                    if (read == 0) continue;
                    _os.write(buf, 0, read);
                    _os.flush();
                }
            } catch (InterruptedIOException iioe) {
            } catch (Throwable t) {
                _thrownError = t;
            } finally {
                try {
                    if (_closeInput) {
                        _is.close();
                    } else {
                        _os.close();
                    }
                } catch (IOException ioe) {
                }
            }
            try {
                _barrier.await();
            } catch (InterruptedException ie) {
            } catch (BrokenBarrierException bbe) {
            }
        }
    }

    private class PusherThread extends PumperThread {

        PusherThread(String name, InputStream is, OutputStream os) {
            super(name, is, os, false);
        }
    }

    private class PullerThread extends PumperThread {

        PullerThread(String name, InputStream is, OutputStream os) {
            super(name, is, os, true);
        }
    }

    public int getTimeout() {
        return _timeout;
    }

    public void setTimeout(int timeout) {
        _timeout = timeout;
    }

    public boolean getWaitForExit() {
        return _waitForExit;
    }

    public void setWaitForExit(boolean waitForExit) {
        _waitForExit = waitForExit;
    }

    public static void main(String[] args) throws Exception {
        String commandPath = null;
        String filePath = null;
        int timeout = 10;
        String usage = "Usage: CommandRunner [-timeout timeoutSecs] commandPath filePath";
        if (args.length < 2) {
            System.err.println(usage);
            System.exit(-1);
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-timeout")) {
                timeout = Integer.parseInt(args[++i]);
                ;
            } else if (i != args.length - 2) {
                System.err.println(usage);
                System.exit(-1);
            } else {
                commandPath = args[i];
                filePath = args[++i];
            }
        }
        CommandRunner cr = new CommandRunner();
        cr.setCommand(commandPath);
        cr.setInputStream(new java.io.FileInputStream(filePath));
        cr.setStdErrorStream(System.err);
        cr.setStdOutputStream(System.out);
        cr.setTimeout(timeout);
        cr.evaluate();
        System.err.println("output value: " + cr.getExitValue());
    }
}
