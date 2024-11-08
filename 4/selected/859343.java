package ejaz.jfilewatcher.core;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;

/**
 * This is file watcher service! Add the files to watch by calling watch()
 * function. You can include wild card in file name - they should be complaint
 * with java's regular expression.
 * 
 * @author Ejaz
 */
public class JFileWatcherService extends Thread {

    private static Logger logger = Logger.getLogger(JFileWatcherService.class);

    private static final List<Runnable> tasks = new ArrayList<Runnable>();

    private static final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    private static final BlockingQueue<JFileWatcherEvent> queue = new LinkedBlockingQueue<JFileWatcherEvent>();

    public JFileWatcherService() {
        logger.info("# File Watcher Threads : " + (Runtime.getRuntime().availableProcessors() * 2));
    }

    /**
	 * Add new file watcher task Watches a folder/file
	 * 
	 * @param task
	 */
    protected void addTask(Runnable task) {
        synchronized (tasks) {
            tasks.add(task);
        }
    }

    /**
	 * Adds an event in the notification queue This will notify the listener
	 * 
	 * @param event
	 */
    protected void offer(JFileWatcherEvent event) {
        queue.offer(event);
    }

    /**
	 * Obtains a file lock. This maybe useful for those applications who do not
	 * want to run multiple file watchers at same time. You could be running two
	 * or more file watchers in active-passive mode on same/different machines.
	 * Whoever gets lock, becomes primary file watcher If primary goes down, one
	 * of the others will become master automatically.
	 * 
	 * @param file
	 *          Lock file name
	 * @param block
	 *          Return if lock not available
	 */
    public boolean lock(File file, boolean block) {
        boolean flag = false;
        RandomAccessFile raf = null;
        logger.info("Getting lock on " + file.getAbsolutePath());
        try {
            raf = new RandomAccessFile(file, "rw");
            if (block) {
                raf.getChannel().lock();
                flag = true;
            } else {
                if (null != raf.getChannel().tryLock()) {
                    flag = true;
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
        if (flag) {
            logger.info("Got lock!");
        }
        return flag;
    }

    /**
	 * Specify the folder/file patterns you need to be watched This can include
	 * wild card characters
	 * 
	 * @param file
	 *          Watch this file
	 * @param patterns
	 *          File name patterns
	 * @param frequency
	 *          Polling frequency
	 */
    public void watch(File folder, String[] patterns) {
        JFileWatcherContext ctx = new JFileWatcherContext();
        ctx.setFile(folder);
        ctx.setPatterns(patterns);
        ctx.setFileWatcherService(this);
        addTask(new JFolderWatcher(ctx));
    }

    /**
	 * Returns next available file. Blocks if not available
	 * 
	 * @return JFileWatcherEvent instance
	 * @throws InterruptedException
	 */
    public JFileWatcherEvent next() throws InterruptedException {
        return queue.take();
    }

    /**
	 * Main loop ...
	 * 
	 */
    public void run() {
        final List<Runnable> tasksCopy = new ArrayList<Runnable>();
        long sleepPeriod = 500;
        if (System.getProperty("sleep.period") != null) {
            sleepPeriod = Long.parseLong(System.getProperty("sleep.period"));
        }
        while (true) {
            tasksCopy.clear();
            synchronized (tasks) {
                tasksCopy.addAll(tasks);
            }
            logger.info("# tasks : " + tasksCopy.size());
            final CountDownLatch cd = new CountDownLatch(tasksCopy.size());
            for (final Runnable task : tasksCopy) {
                pool.submit(new Runnable() {

                    public void run() {
                        try {
                            task.run();
                        } catch (Exception e) {
                            logger.fatal("ERR", e);
                        } finally {
                            cd.countDown();
                        }
                    }
                });
            }
            try {
                cd.await();
            } catch (Exception e) {
            }
            try {
                Thread.sleep(sleepPeriod);
            } catch (Exception e) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        File folder = null;
        List<String> patterns = new ArrayList<String>();
        JFileWatcherService fservice = new JFileWatcherService();
        if (!fservice.lock(new File("/tmp/file-watch.lck"), false)) {
            System.err.println("Failed to get lock");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-watch")) {
                if (patterns.size() > 0) {
                    logger.info("Watch Folder : " + folder);
                    logger.info("Watch Patterns : " + patterns);
                    fservice.watch(folder, patterns.toArray(new String[patterns.size()]));
                }
                folder = new File(args[++i]);
                patterns.clear();
            } else {
                patterns.add(args[i]);
            }
        }
        if (patterns.size() > 0) {
            logger.info("Watch Folder : " + folder);
            logger.info("Watch Patterns : " + patterns);
            fservice.watch(folder, patterns.toArray(new String[patterns.size()]));
        }
        fservice.start();
        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm a");
        while (true) {
            JFileWatcherEvent e = fservice.next();
            System.out.println("[" + fmt.format(e.getFile().lastModified()) + "] " + e.getType() + " : " + e.getFile().getAbsolutePath());
        }
    }
}
