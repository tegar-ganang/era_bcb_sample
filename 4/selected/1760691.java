package jdbm.btree;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.recman.TestCaseWithTestFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConcurrentTest {

    public static class Dummy implements Serializable {

        private static final long serialVersionUID = -5567451291089724793L;

        private long key;

        @SuppressWarnings("unused")
        private byte space[] = new byte[1024];

        public Dummy() {
        }

        public Dummy(long key) {
            this.key = key;
        }

        @Override
        public int hashCode() {
            return (int) key;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Dummy)) return false;
            Dummy other = (Dummy) obj;
            if (key != other.key) return false;
            return true;
        }
    }

    private ConcurrentHashMap<Long, Dummy> map = new ConcurrentHashMap<Long, Dummy>();

    private BlockingQueue<Long> added = new LinkedBlockingQueue<Long>();

    private int readers = 4;

    private int writers = 2;

    private int removers = 1;

    private RecordManager recman;

    private BTree btree;

    private int times = 10000;

    @Before
    public void setUp() throws Exception {
        TestCaseWithTestFile.deleteTestFile();
        recman = RecordManagerFactory.createRecordManager(TestCaseWithTestFile.testFileName);
        System.err.println(recman.getClass());
    }

    @After
    public void tearDown() throws Exception {
        recman.close();
        TestCaseWithTestFile.deleteTestFile();
    }

    @Test
    public void testConcurrentRecMan() throws Exception {
        testConcurrent();
    }

    private void testConcurrent() throws Exception {
        Runnable insert = new Runnable() {

            public void run() {
                insert();
            }
        };
        Runnable read = new Runnable() {

            public void run() {
                read();
            }
        };
        Runnable remove = new Runnable() {

            public void run() {
                remove();
            }
        };
        Thread t[] = new Thread[readers + writers + removers];
        int c = 0;
        for (int i = 0; i < writers; i++) {
            t[c++] = new Thread(insert);
        }
        for (int i = 0; i < readers; i++) {
            t[c++] = new Thread(read);
        }
        for (int i = 0; i < removers; i++) {
            t[c++] = new Thread(remove);
        }
        for (int i = 0; i < t.length; i++) {
            t[i].start();
        }
        System.err.println("wait for complete");
        for (int i = 0; i < t.length; i++) {
            t[i].join();
        }
    }

    @Test
    public void testConcurrentBTree() throws Exception {
        btree = BTree.createInstance(recman, Collections.reverseOrder());
        testConcurrent();
    }

    private void delete(Long key) throws IOException {
        if (btree != null) btree.remove(key); else recman.delete(key);
    }

    private Object fetch(Long id) throws IOException {
        if (btree != null) return btree.find(id); else return recman.fetch(id);
    }

    private long insert(Dummy d) throws IOException {
        if (btree != null) {
            btree.insert(d.key, d, true);
            return d.key;
        } else {
            return recman.insert(d);
        }
    }

    private void update(long id, Dummy d) throws IOException {
        if (btree != null) {
            btree.insert(id, d, true);
        } else {
            recman.update(id, d);
        }
    }

    private void commit() throws IOException {
        recman.commit();
    }

    private void remove() {
        int count = 0;
        Random r = new Random();
        int times2 = times;
        while (count < times2) {
            try {
                Long id = added.take();
                map.remove(id);
                if (r.nextInt(3) == 0) {
                    delete(id);
                    count++;
                } else {
                    added.put(id);
                }
                if (count % 1000 == 0) commit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        System.err.println("delete done");
    }

    private void read() {
        int c = 0;
        while (c < times) {
            for (Entry<Long, Dummy> e : map.entrySet()) {
                Long id = e.getKey();
                Object fetch;
                try {
                    fetch = fetch(id);
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
                if (map.contains(id)) assertEquals(e.getValue(), fetch);
                c++;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                break;
            }
        }
        System.err.println("read done");
    }

    private void insert() {
        try {
            insert0();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void insert0() throws IOException {
        Random r = new Random();
        for (int i = 0; i < times; i++) {
            Dummy d = new Dummy(i);
            long id;
            id = insert(d);
            map.put(id, d);
            if (r.nextInt(3) == 0) {
                update(id, d);
            } else {
                added.add(id);
            }
        }
        recman.commit();
        System.err.println("insert done");
    }
}
