package moe.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import moe.config.Configuration;
import moe.config.Constants;
import moe.config.PropertiesUtil;
import moe.dao.BaseDao;
import moe.dao.CollectionDao;
import moe.dao.PostDao;
import moe.entity.Collection;
import moe.entity.Post;

public class CollectionScanner extends Thread {

    private static final Logger log = Logger.getLogger(CollectionScanner.class.getName());

    private static final int QUEUE_LENGTH = 100;

    private static final long TIMEOUT = 10;

    private static final int WRITE_BATCH_SIZE = 30;

    private static final Pattern imoutoPattern = Pattern.compile("^(?:moe|yande\\.re) (\\d+)[ .].+$");

    private static final String PREVIEW_DIR = System.getProperty("user.dir") + File.separator + "preview" + File.separator;

    private final BlockingQueue<Collection> entityQueue = new ArrayBlockingQueue<Collection>(QUEUE_LENGTH);

    private final BlockingQueue<Integer> clearQueue = new LinkedBlockingQueue<Integer>();

    private final Thread[] threadArray = new Thread[2];

    private Thread scannerThread, writerThread;

    private CollectionDao collectionDao = new CollectionDao();

    private PostDao postDao = new PostDao(BaseDao.ConnType.DEFAULT);

    @Override
    public void run() {
        boolean isRescan = PropertiesUtil.hasProperty(Constants.COLLECTION_LIBRARY);
        if (!isRescan) {
            log.setLevel(Level.WARNING);
        }
        long intervalTime = Configuration.getLibraryRefreshInterval() * 60 * 1000;
        do {
            scan();
            try {
                Thread.sleep(intervalTime);
            } catch (InterruptedException e) {
                return;
            }
        } while (isRescan);
    }

    public void listen() {
        this.setName("Collection Scanner Listen Thread");
        this.setDaemon(true);
        this.start();
    }

    public void shutdown() {
        for (Thread t : threadArray) {
            if (t.isAlive() || !t.isInterrupted()) {
                t.interrupt();
            }
        }
        if (this.isAlive() || !this.isInterrupted()) {
            this.interrupt();
        }
    }

    public void scan() {
        log.info("Refreshing collection library...");
        initThreads();
        for (Thread thread : threadArray) {
            thread.start();
        }
        for (Thread thread : threadArray) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
        log.info("Refresh finish.");
    }

    private void initThreads() {
        threadArray[0] = new Thread() {

            @Override
            public void run() {
                try {
                    collectionDao.createCollectionTables();
                    doScan();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        threadArray[1] = new Thread() {

            @Override
            public void run() {
                int count = 0;
                Collection entity = null;
                while (scannerThread.isAlive() || !entityQueue.isEmpty()) {
                    entity = readQueue();
                    if (entity == null) {
                        continue;
                    }
                    if (collectionDao.addCollection(entity)) {
                        count++;
                    }
                }
                doClearInvalidCollection();
                updatePostTable();
                log.info("Found " + count + " new item");
            }
        };
        scannerThread = threadArray[0];
        writerThread = threadArray[1];
        scannerThread.setName("Collection Scanner Thread");
        scannerThread.setDaemon(true);
        writerThread.setName("Collection Writer Thread");
        writerThread.setDaemon(true);
    }

    private void updatePostTable() {
        Integer[] ids = collectionDao.getMissingPostsId();
        if (ids.length < 1) {
            return;
        }
        List<Long> idList = new ArrayList<Long>(WRITE_BATCH_SIZE);
        for (int i = 0, length = ids.length; i < length; i++) {
            idList.add((long) ids[i]);
            if (idList.size() == WRITE_BATCH_SIZE) {
                addPosts(idList.toArray(new Long[WRITE_BATCH_SIZE]));
                idList.clear();
            }
        }
        if (!idList.isEmpty()) {
            addPosts(idList.toArray(new Long[idList.size()]));
        }
    }

    private void addPosts(Long[] ids) {
        List<Post> posts = postDao.getPosts(ids);
        if (posts.size() < 1) {
            return;
        }
        collectionDao.addPosts(posts);
    }

    private void doScan() {
        int version = collectionDao.getMaxLibraryVersion() + 1;
        String[] dirPaths = getCollectionLibrary();
        File dir = null;
        for (String path : dirPaths) {
            dir = new File(path);
            if (!dir.exists() || dir.isFile()) {
                continue;
            }
            doScan(dir, version);
        }
    }

    private void doScan(File dir, int version) {
        boolean isNeedScan = collectionDao.addOrUpdateLibrary(dir, version);
        int lid = collectionDao.getLibraryId(dir.getPath());
        File[] subFiles = dir.listFiles();
        for (File subFile : subFiles) {
            if (subFile.isDirectory()) {
                doScan(subFile, version);
            } else if (isNeedScan) {
                Matcher matcher = imoutoPattern.matcher(subFile.getName());
                if (!matcher.matches()) {
                    continue;
                }
                String id = matcher.group(1);
                writeQueue(new Collection(Long.valueOf(id), lid, version, subFile));
            }
        }
        if (isNeedScan) {
            clearInvalidCollection(lid);
        }
    }

    private String[] getCollectionLibrary() {
        String pathString = Configuration.getCollectionLibraryPath();
        if (pathString == null || pathString.trim().equals("")) {
            return new String[] {};
        }
        return pathString.split(";");
    }

    private void clearInvalidCollection(int id) {
        clearQueue.add(id);
    }

    private void doClearInvalidCollection() {
        Integer id = null;
        int version = Integer.MAX_VALUE;
        if (PropertiesUtil.hasProperty(Constants.COLLECTION_LIBRARY)) {
            version = collectionDao.getMaxLibraryVersion();
        }
        List<String> names = collectionDao.clearInvalidLibrary(version);
        while (scannerThread.isAlive() || !clearQueue.isEmpty()) {
            id = readClearQueue();
            if (id == null) {
                continue;
            }
            names.addAll(collectionDao.clearResidualCollection(id));
        }
        clearInvalidPreview(names);
        log.info("Cleared " + names.size() + " invalid item");
    }

    private void clearInvalidPreview(List<String> names) {
        File file = null;
        for (String name : names) {
            file = new File(PREVIEW_DIR, "thumbnail." + name);
            file.delete();
        }
    }

    private void writeQueue(Collection entity) {
        while (true) {
            try {
                if (entityQueue.offer(entity, TIMEOUT, TimeUnit.SECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!writerThread.isAlive()) {
                throw new RuntimeException("writerThread was died.");
            }
        }
    }

    private Collection readQueue() {
        Collection entity = null;
        while (true) {
            try {
                entity = entityQueue.poll(TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (entity != null || (!scannerThread.isAlive() && entityQueue.isEmpty())) {
                return entity;
            }
        }
    }

    private Integer readClearQueue() {
        Integer result = null;
        while (true) {
            try {
                result = clearQueue.poll(TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (result != null || (!scannerThread.isAlive() && clearQueue.isEmpty())) {
                return result;
            }
        }
    }
}
