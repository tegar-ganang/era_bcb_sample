package es.upv.dsic.issi.moment.maudedaemon.maude.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import es.upv.dsic.issi.moment.maudedaemon.maude.IMaudeJob;
import es.upv.dsic.issi.moment.maudedaemon.maude.IMaudeProcess;
import es.upv.dsic.issi.moment.maudedaemon.maude.IMaudeProcessBatch;
import es.upv.dsic.issi.moment.maudedaemon.parser.BaseTermsJoinerTreeParser;
import es.upv.dsic.issi.moment.maudedaemon.parser.FullMaudeCommandsLexer;
import es.upv.dsic.issi.moment.maudedaemon.parser.FullMaudeCommandsParser;
import es.upv.dsic.issi.moment.maudedaemon.parser.ParseException;

/**
 * @author Abel G�mez. agomez@dsic.upv.es
 * 
 */
public class MaudeProcessBatch extends MaudeProcessBase implements IMaudeProcess, IMaudeProcessBatch {

    private ThreadSendJob thSend;

    private ThreadGetOutput thGet;

    private BlockingQueue<IMaudeJob> jobsList = new LinkedBlockingQueue<IMaudeJob>();

    private MaudeJob activeJob;

    private final Object sendingJob = new Object();

    private final Object finishedJob = new Object();

    private Boolean receiving = false;

    private static final Logger log = Logger.getLogger(MaudeProcessBatch.class.getName());

    private boolean errorClean = true;

    private IMaudeJob errorSource;

    /**
	 * Constructor for MaudeProcessBath
	 */
    public MaudeProcessBatch() {
        super();
        setShowBanner(false);
        thSend = new ThreadSendJob();
        thGet = new ThreadGetOutput();
        thSend.start();
        thGet.start();
    }

    public boolean execMaude() throws IOException {
        char c;
        String txt = "";
        super.execMaude();
        if (!running) return false;
        do {
            while (buffStdout.ready()) {
                c = (char) buffStdout.read();
                txt += c;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!txt.endsWith("Maude> "));
        return true;
    }

    public synchronized void addJob(IMaudeJob job) {
        jobsList.add(job);
    }

    public List<IMaudeJob> createJobs(String input) throws ParseException {
        try {
            return createJobs(new ByteArrayInputStream(input.getBytes()));
        } catch (ParseException e) {
            throw new ParseException("No valid parse for " + input, e);
        }
    }

    public List<IMaudeJob> createJobs(InputStream input) throws ParseException {
        List<IMaudeJob> jobs = new ArrayList<IMaudeJob>();
        if (isFullMaude()) try {
            int inputChars = input.available();
            FullMaudeCommandsParser p = new FullMaudeCommandsParser(new FullMaudeCommandsLexer(input));
            p.program();
            BaseTermsJoinerTreeParser bt = new BaseTermsJoinerTreeParser();
            List<String> commands = bt.program(p.getAST());
            int charCounter = 0;
            for (String s : commands) {
                jobs.add(new MaudeJob(s));
                charCounter += s.length();
            }
        } catch (Exception e) {
            throw new ParseException(e);
        } else {
            try {
                InputStreamReader isr = new InputStreamReader(input);
                StringWriter output = new StringWriter();
                char[] buffer = new char[1024];
                int n;
                while (-1 != (n = isr.read(buffer))) output.write(buffer, 0, n);
                jobs.add(new MaudeJob(output.toString()));
            } catch (Exception e) {
                throw new ParseException(e);
            }
        }
        return jobs;
    }

    public List<IMaudeJob> createAndRunJobs(InputStream input) throws ParseException {
        List<IMaudeJob> jobs = createJobs(input);
        for (IMaudeJob job : jobs) addJob(job);
        return jobs;
    }

    public List<IMaudeJob> createAndRunJobs(String input) throws ParseException {
        return createAndRunJobs(new ByteArrayInputStream(input.getBytes()));
    }

    private class ThreadSendJob extends Thread {

        ThreadSendJob() {
            super("Maude (send job)");
        }

        /**
		 * Run method Actions to do while the thread is active
		 */
        public void run() {
            while (true) {
                try {
                    synchronized (finishedJob) {
                        while (activeJob != null && !activeJob.isFinished()) try {
                            finishedJob.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                        while (activeJob == null || activeJob.isFinished()) try {
                            activeJob = (MaudeJob) jobsList.take();
                        } catch (InterruptedException e) {
                            return;
                        }
                        log.fine("Sending job to Maude");
                        synchronized (sendingJob) {
                            sendingJob.notifyAll();
                        }
                        buffStdin.write(activeJob.getInput());
                        buffStdin.flush();
                        sendToLog(activeJob.getInput());
                    }
                } catch (IOException e1) {
                } finally {
                }
            }
        }
    }

    private class ThreadGetOutput extends Thread {

        public ThreadGetOutput() {
            super("Maude (get output)");
        }

        /**
		 * Run method Actions to do while the thread is active
		 */
        public synchronized void run() {
            char c;
            try {
                while (true) {
                    synchronized (sendingJob) {
                        while (activeJob == null || activeJob.isFinished()) try {
                            sendingJob.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    String patternStr = "(.*)" + activeJob.getRedExprPattern();
                    Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);
                    Matcher matcher = null;
                    StringBuffer sbOut = new StringBuffer();
                    StringBuffer sbErr = new StringBuffer();
                    do {
                        while (isRunning() && buffStdout.ready()) {
                            c = (char) buffStdout.read();
                            sbOut.append(c);
                        }
                        while (isRunning() && buffStderr.ready()) {
                            c = (char) buffStderr.read();
                            sbErr.append(c);
                        }
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                        }
                        if (sbOut.length() >= 200) {
                            matcher = pattern.matcher(sbOut.substring(sbOut.length() - 200, sbOut.length()));
                        } else matcher = pattern.matcher(sbOut);
                    } while (!((isCoreMaude() && matcher.matches()) || (isFullMaude() && sbOut.toString().endsWith("\nMaude> "))) && !isInterrupted());
                    if (!interrupted()) {
                        sendToLog(sbErr.toString());
                        sendToLog(sbOut.toString());
                        activeJob.completed(sbOut.toString(), sbErr.toString());
                        jobCompleted(activeJob);
                    } else {
                        log.info("Job interrupted by user");
                        activeJob.completed(null, "Job Interrupted by user");
                        errorClean = false;
                        return;
                    }
                    synchronized (finishedJob) {
                        log.fine("Marking job finished");
                        finishedJob.notifyAll();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.info("Unable to process result.");
                activeJob.completed(null, "Unable to process result. Out of memory.");
                errorClean = false;
                return;
            }
        }
    }

    public void waitUntilFinish() {
        if (activeJob == null) return;
        activeJob.waitUntilFinish();
        while (jobsList.isEmpty() == false) {
            log.fine("Wating till job is finished");
            activeJob.waitUntilFinish();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

    public boolean isErrorClean() {
        boolean currentValue = errorClean;
        errorClean = true;
        return currentValue;
    }

    public void loopInit() {
        jobsList.clear();
        thSend.interrupt();
        thGet.interrupt();
        thSend = new ThreadSendJob();
        thGet = new ThreadGetOutput();
        thSend.start();
        thGet.start();
        addJob(new MaudeJob("loop init ."));
        errorClean = true;
    }

    public IMaudeJob getLastError() {
        return errorSource;
    }

    protected void finalize() throws Throwable {
        killMaude();
    }

    @Override
    public void killMaude() {
        thGet.interrupt();
        thSend.interrupt();
        super.killMaude();
    }

    @Override
    public void quitMaude() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.quitMaude();
    }

    private void jobCompleted(IMaudeJob job) {
        if (errorClean && job.isFailed()) {
            errorClean = false;
            errorSource = job;
        }
    }
}
