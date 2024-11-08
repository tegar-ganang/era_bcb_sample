package moe.dump;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import moe.config.Configuration;
import moe.config.Constants;
import moe.config.PropertiesUtil;
import moe.dao.BaseDao;
import moe.dao.PoolDao;
import moe.dao.PostDao;
import moe.dao.TagDao;
import moe.entity.Pool;
import moe.entity.Post;
import moe.entity.Tag;
import moe.util.Paging;

public class Dumper {

    private static final int capacity = 3, pageSize = 100;

    private final FlushablePrintConsole console = new FlushablePrintConsole();

    private CountDownLatch countDownLatch = new CountDownLatch(3);

    public void doDump() {
        createTables();
        dumpTag();
        dumpPost();
        dumpPool();
        updateTables();
    }

    private void updateTables() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PostDao dao = new PostDao(BaseDao.ConnType.DEFAULT);
        dao.alterDumpTables();
    }

    private void dumpPost() {
        final BlockingQueue<List<Post>> queue = new ArrayBlockingQueue<List<Post>>(capacity);
        final String consoleId = "dumpPost";
        final Thread[] threads = new Thread[2];
        threads[0] = new Thread("postDumpReaderThread") {

            @Override
            public void run() {
                Thread writerThread = threads[1];
                PostDao postDao = new PostDao(BaseDao.ConnType.DUMP);
                int next = 1, progress = 0, tmp = 0;
                Paging paging = new Paging(pageSize);
                Map<String, String[]> query = new HashMap<String, String[]>();
                query.put("order", new String[] { "desc id" });
                List<Post> list = null;
                do {
                    paging.setPageNumber(next);
                    try {
                        list = postDao.listPosts(paging);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        return;
                    }
                    tmp = Math.round(paging.getPageNumber() * 1f / paging.getPageTotal() * 100);
                    if (progress != tmp) {
                        progress = tmp;
                        console.flushablePrint(consoleId, "Post progress: " + progress + "%");
                    }
                    boolean offer = false;
                    do {
                        offer = false;
                        try {
                            offer = queue.offer(list, 5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                        }
                        if (!writerThread.isAlive()) {
                            return;
                        }
                    } while (!offer);
                    if (paging.hasNext()) {
                        next++;
                    }
                } while (paging.hasNext());
            }
        };
        threads[1] = new Thread("postDumpWriterThread") {

            @Override
            public void run() {
                Thread readerThread = threads[0];
                PostDao postDao = new PostDao(BaseDao.ConnType.DEFAULT);
                List<Post> list = null;
                while (!queue.isEmpty() || readerThread.isAlive()) {
                    list = null;
                    do {
                        try {
                            list = queue.poll(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                        }
                        if (list == null && !readerThread.isAlive()) {
                            break;
                        }
                    } while (list == null);
                    try {
                        postDao.insertPosts(list);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        countDownLatch.countDown();
                        return;
                    }
                }
                countDownLatch.countDown();
                console.remove(consoleId);
                console.println("Post table dump finish!");
            }
        };
        console.println("Post table dump start.");
        for (Thread thread : threads) {
            thread.start();
        }
    }

    private void dumpTag() {
        final BlockingQueue<List<Tag>> queue = new ArrayBlockingQueue<List<Tag>>(capacity);
        final String consoleId = "dumpTag";
        final Thread[] threads = new Thread[2];
        threads[0] = new Thread("tagDumpReaderThread") {

            @Override
            public void run() {
                Thread writerThread = threads[1];
                TagDao tagDao = new TagDao(BaseDao.ConnType.DUMP);
                int next = 1, progress = 0, tmp = 0;
                Paging paging = new Paging(pageSize);
                List<Tag> list = null;
                boolean offer = false;
                do {
                    paging.setPageNumber(next);
                    try {
                        list = tagDao.listTags(paging);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        return;
                    }
                    tmp = Math.round(paging.getPageNumber() * 1f / paging.getPageTotal() * 100);
                    if (progress != tmp) {
                        progress = tmp;
                        console.flushablePrint(consoleId, "Tags progress: " + progress + "%");
                    }
                    offer = false;
                    do {
                        try {
                            offer = queue.offer(list, 5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                        }
                        if (!writerThread.isAlive()) {
                            return;
                        }
                    } while (!offer);
                    if (paging.hasNext()) {
                        next++;
                    }
                } while (paging.hasNext());
            }
        };
        threads[1] = new Thread("tagDumpWriterThread") {

            @Override
            public void run() {
                Thread readerThread = threads[0];
                TagDao tagDao = new TagDao();
                List<Tag> list = null;
                while (!queue.isEmpty() || readerThread.isAlive()) {
                    list = null;
                    do {
                        try {
                            list = queue.poll(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                        }
                        if (list == null && !readerThread.isAlive()) {
                            break;
                        }
                    } while (list == null);
                    try {
                        tagDao.insertTags(list);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        countDownLatch.countDown();
                        return;
                    }
                }
                countDownLatch.countDown();
                console.remove(consoleId);
                console.println("Tags table dump finish!\t\t\t\t\t\t");
            }
        };
        console.println("Tags table dump start.");
        for (Thread thread : threads) {
            thread.start();
        }
    }

    private void dumpPool() {
        final BlockingQueue<List<Pool>> queue = new ArrayBlockingQueue<List<Pool>>(capacity);
        final String consoleId = "dumpPool";
        final Thread[] threads = new Thread[2];
        threads[0] = new Thread("poolDumpReaderThread") {

            @Override
            public void run() {
                Thread writerThread = threads[1];
                PoolDao poolDao = new PoolDao(BaseDao.ConnType.DUMP);
                int next = 1, progress = 0, tmp = 0;
                Paging paging = new Paging(pageSize);
                List<Pool> list = null;
                boolean offer = false;
                do {
                    paging.setPageNumber(next);
                    try {
                        list = poolDao.listFullPools(paging);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        return;
                    }
                    tmp = Math.round(paging.getPageNumber() * 1f / paging.getPageTotal() * 100);
                    if (progress != tmp) {
                        progress = tmp;
                        console.flushablePrint(consoleId, "Pool progress: " + progress + "%");
                    }
                    offer = false;
                    do {
                        try {
                            offer = queue.offer(list, 5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                        }
                        if (!writerThread.isAlive()) {
                            return;
                        }
                    } while (!offer);
                    if (paging.hasNext()) {
                        next++;
                    }
                } while (paging.hasNext());
            }
        };
        threads[1] = new Thread("poolDumpWriterThread") {

            @Override
            public void run() {
                Thread readerThread = threads[0];
                PoolDao poolDao = new PoolDao();
                List<Pool> list = null;
                while (!queue.isEmpty() || readerThread.isAlive()) {
                    list = null;
                    do {
                        try {
                            list = queue.poll(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                        }
                        if (list == null && !readerThread.isAlive()) {
                            break;
                        }
                    } while (list == null);
                    try {
                        for (Pool pool : list) {
                            poolDao.insertPool(pool);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        countDownLatch.countDown();
                        return;
                    }
                }
                countDownLatch.countDown();
                console.remove(consoleId);
                console.println("Pool table dump finish!\t\t\t\t");
            }
        };
        console.println("Pool table dump start.");
        for (Thread thread : threads) {
            thread.start();
        }
    }

    public void createTables() {
        PostDao dao = new PostDao(BaseDao.ConnType.DEFAULT);
        dao.createDumpTables();
    }

    public static class FlushablePrintConsole {

        private Map<String, String> map = new LinkedHashMap<String, String>();

        private PrintStream out = System.out;

        private int index = 0;

        public synchronized String flushablePrint(String id, String msg) {
            if (id == null) {
                id = createId();
            }
            Map<String, String> newMap = cloneMap(map);
            newMap.put(id, msg);
            doPrint(newMap);
            return id;
        }

        public synchronized void remove(String id) {
            clear();
            map.remove(id);
            output();
        }

        public synchronized void println(String msg) {
            clear();
            out.println(msg);
            output();
        }

        private void doPrint(Map<String, String> newMap) {
            flush(newMap);
            map = newMap;
        }

        private void flush() {
            flush(map);
        }

        private void flush(Map<String, String> target) {
            clear();
            output(target);
        }

        private void clear() {
            int length = 0;
            for (String str : map.values()) {
                length += str.length() + 2;
            }
            printBackspace(length);
        }

        private void output() {
            output(map);
        }

        private void output(Map<String, String> target) {
            StringBuilder builder = new StringBuilder();
            for (Iterator<String> i = target.values().iterator(); i.hasNext(); ) {
                builder.append(i.next());
                if (i.hasNext()) {
                    builder.append(", ");
                }
            }
            out.print(builder.toString());
        }

        private void pritnBackspace(String msg) {
            printBackspace(msg.length());
        }

        private void printBackspace(int length) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < length; i++) {
                builder.append('\b');
            }
            out.print(builder.toString());
        }

        private String createId() {
            return String.valueOf(index++);
        }

        private Map<String, String> cloneMap(Map<String, String> srcMap) {
            return (Map<String, String>) ((HashMap<String, String>) srcMap).clone();
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        updateUserDir();
        try {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream(Constants.LOGGING_CONF_FILE));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (PropertiesUtil.getProperty(Constants.DB_TYPE, "sqlite").equals("sqlite")) {
            throw new RuntimeException("Illegal config");
        }
        Configuration.getJdbcUrl();
        new Dumper().doDump();
    }

    private static void updateUserDir() {
        File userDir = new File(System.getProperty("user.dir"));
        String newUserDir = userDir.getParent();
        if (newUserDir != null) {
            System.setProperty("user.dir", newUserDir);
        }
    }
}
