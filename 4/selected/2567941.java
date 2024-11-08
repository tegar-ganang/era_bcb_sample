package org.apache.lucene.store;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

public class TestLockFactory extends LuceneTestCase {

    public void testCustomLockFactory() throws IOException {
        Directory dir = new RAMDirectory();
        MockLockFactory lf = new MockLockFactory();
        dir.setLockFactory(lf);
        assertTrue("lock prefix was not set by the RAMDirectory", lf.lockPrefixSet);
        IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        for (int i = 0; i < 100; i++) {
            addDoc(writer);
        }
        assertEquals("# of unique locks created (after instantiating IndexWriter)", 1, lf.locksCreated.size());
        assertTrue("# calls to makeLock is 0 (after instantiating IndexWriter)", lf.makeLockCount >= 1);
        for (Iterator e = lf.locksCreated.keySet().iterator(); e.hasNext(); ) {
            String lockName = (String) e.next();
            MockLockFactory.MockLock lock = (MockLockFactory.MockLock) lf.locksCreated.get(lockName);
            assertTrue("# calls to Lock.obtain is 0 (after instantiating IndexWriter)", lock.lockAttempts > 0);
        }
        writer.close();
    }

    public void testRAMDirectoryNoLocking() throws IOException {
        Directory dir = new RAMDirectory();
        dir.setLockFactory(NoLockFactory.getNoLockFactory());
        assertTrue("RAMDirectory.setLockFactory did not take", NoLockFactory.class.isInstance(dir.getLockFactory()));
        IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        IndexWriter writer2 = null;
        try {
            writer2 = new IndexWriter(dir, new WhitespaceAnalyzer(), false, IndexWriter.MaxFieldLength.LIMITED);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Should not have hit an IOException with no locking");
        }
        writer.close();
        if (writer2 != null) {
            writer2.close();
        }
    }

    public void testDefaultRAMDirectory() throws IOException {
        Directory dir = new RAMDirectory();
        assertTrue("RAMDirectory did not use correct LockFactory: got " + dir.getLockFactory(), SingleInstanceLockFactory.class.isInstance(dir.getLockFactory()));
        IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        IndexWriter writer2 = null;
        try {
            writer2 = new IndexWriter(dir, new WhitespaceAnalyzer(), false, IndexWriter.MaxFieldLength.LIMITED);
            fail("Should have hit an IOException with two IndexWriters on default SingleInstanceLockFactory");
        } catch (IOException e) {
        }
        writer.close();
        if (writer2 != null) {
            writer2.close();
        }
    }

    public void testSimpleFSLockFactory() throws IOException {
        new SimpleFSLockFactory("test");
    }

    public void testStressLocks() throws Exception {
        _testStressLocks(null, _TestUtil.getTempDir("index.TestLockFactory6"));
    }

    public void testStressLocksNativeFSLockFactory() throws Exception {
        File dir = _TestUtil.getTempDir("index.TestLockFactory7");
        _testStressLocks(new NativeFSLockFactory(dir), dir);
    }

    public void _testStressLocks(LockFactory lockFactory, File indexDir) throws Exception {
        FSDirectory fs1 = FSDirectory.open(indexDir, lockFactory);
        IndexWriter w = new IndexWriter(fs1, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        addDoc(w);
        w.close();
        WriterThread writer = new WriterThread(100, fs1);
        SearcherThread searcher = new SearcherThread(100, fs1);
        writer.start();
        searcher.start();
        while (writer.isAlive() || searcher.isAlive()) {
            Thread.sleep(1000);
        }
        assertTrue("IndexWriter hit unexpected exceptions", !writer.hitException);
        assertTrue("IndexSearcher hit unexpected exceptions", !searcher.hitException);
        _TestUtil.rmDir(indexDir);
    }

    public void testNativeFSLockFactory() throws IOException {
        NativeFSLockFactory f = new NativeFSLockFactory(System.getProperty("tempDir"));
        f.setLockPrefix("test");
        Lock l = f.makeLock("commit");
        Lock l2 = f.makeLock("commit");
        assertTrue("failed to obtain lock", l.obtain());
        assertTrue("succeeded in obtaining lock twice", !l2.obtain());
        l.release();
        assertTrue("failed to obtain 2nd lock after first one was freed", l2.obtain());
        l2.release();
        assertTrue("failed to obtain lock", l.obtain());
        assertTrue(l.isLocked());
        assertTrue(l2.isLocked());
        l.release();
        assertFalse(l.isLocked());
        assertFalse(l2.isLocked());
    }

    public void testNativeFSLockFactoryLockExists() throws IOException {
        File lockFile = new File(TEMP_DIR, "test.lock");
        lockFile.createNewFile();
        Lock l = new NativeFSLockFactory(TEMP_DIR).makeLock("test.lock");
        assertTrue("failed to obtain lock", l.obtain());
        l.release();
        assertFalse("failed to release lock", l.isLocked());
        if (lockFile.exists()) {
            lockFile.delete();
        }
    }

    public void testNativeFSLockReleaseByOtherLock() throws IOException {
        NativeFSLockFactory f = new NativeFSLockFactory(System.getProperty("tempDir"));
        f.setLockPrefix("test");
        Lock l = f.makeLock("commit");
        Lock l2 = f.makeLock("commit");
        assertTrue("failed to obtain lock", l.obtain());
        try {
            assertTrue(l2.isLocked());
            l2.release();
            fail("should not have reached here. LockReleaseFailedException should have been thrown");
        } catch (LockReleaseFailedException e) {
        } finally {
            l.release();
        }
    }

    public void testNativeFSLockFactoryPrefix() throws IOException {
        File fdir1 = _TestUtil.getTempDir("TestLockFactory.8");
        File fdir2 = _TestUtil.getTempDir("TestLockFactory.8.Lockdir");
        Directory dir1 = FSDirectory.open(fdir1, new NativeFSLockFactory(fdir1));
        Directory dir2 = FSDirectory.open(fdir1, new NativeFSLockFactory(fdir2));
        String prefix1 = dir1.getLockFactory().getLockPrefix();
        assertNull("Lock prefix for lockDir same as directory should be null", prefix1);
        String prefix2 = dir2.getLockFactory().getLockPrefix();
        assertNotNull("Lock prefix for lockDir outside of directory should be not null", prefix2);
        _TestUtil.rmDir(fdir1);
        _TestUtil.rmDir(fdir2);
    }

    public void testDefaultFSLockFactoryPrefix() throws IOException {
        File dirName = _TestUtil.getTempDir("TestLockFactory.10");
        Directory dir = FSDirectory.open(dirName);
        String prefix = dir.getLockFactory().getLockPrefix();
        assertTrue("Default lock prefix should be null", null == prefix);
        _TestUtil.rmDir(dirName);
    }

    private class WriterThread extends Thread {

        private Directory dir;

        private int numIteration;

        public boolean hitException = false;

        public WriterThread(int numIteration, Directory dir) {
            this.numIteration = numIteration;
            this.dir = dir;
        }

        @Override
        public void run() {
            WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
            IndexWriter writer = null;
            for (int i = 0; i < this.numIteration; i++) {
                try {
                    writer = new IndexWriter(dir, analyzer, false, IndexWriter.MaxFieldLength.LIMITED);
                } catch (IOException e) {
                    if (e.toString().indexOf(" timed out:") == -1) {
                        hitException = true;
                        System.out.println("Stress Test Index Writer: creation hit unexpected IOException: " + e.toString());
                        e.printStackTrace(System.out);
                    } else {
                    }
                } catch (Exception e) {
                    hitException = true;
                    System.out.println("Stress Test Index Writer: creation hit unexpected exception: " + e.toString());
                    e.printStackTrace(System.out);
                    break;
                }
                if (writer != null) {
                    try {
                        addDoc(writer);
                    } catch (IOException e) {
                        hitException = true;
                        System.out.println("Stress Test Index Writer: addDoc hit unexpected exception: " + e.toString());
                        e.printStackTrace(System.out);
                        break;
                    }
                    try {
                        writer.close();
                    } catch (IOException e) {
                        hitException = true;
                        System.out.println("Stress Test Index Writer: close hit unexpected exception: " + e.toString());
                        e.printStackTrace(System.out);
                        break;
                    }
                    writer = null;
                }
            }
        }
    }

    private class SearcherThread extends Thread {

        private Directory dir;

        private int numIteration;

        public boolean hitException = false;

        public SearcherThread(int numIteration, Directory dir) {
            this.numIteration = numIteration;
            this.dir = dir;
        }

        @Override
        public void run() {
            IndexSearcher searcher = null;
            Query query = new TermQuery(new Term("content", "aaa"));
            for (int i = 0; i < this.numIteration; i++) {
                try {
                    searcher = new IndexSearcher(dir, false);
                } catch (Exception e) {
                    hitException = true;
                    System.out.println("Stress Test Index Searcher: create hit unexpected exception: " + e.toString());
                    e.printStackTrace(System.out);
                    break;
                }
                if (searcher != null) {
                    ScoreDoc[] hits = null;
                    try {
                        hits = searcher.search(query, null, 1000).scoreDocs;
                    } catch (IOException e) {
                        hitException = true;
                        System.out.println("Stress Test Index Searcher: search hit unexpected exception: " + e.toString());
                        e.printStackTrace(System.out);
                        break;
                    }
                    try {
                        searcher.close();
                    } catch (IOException e) {
                        hitException = true;
                        System.out.println("Stress Test Index Searcher: close hit unexpected exception: " + e.toString());
                        e.printStackTrace(System.out);
                        break;
                    }
                    searcher = null;
                }
            }
        }
    }

    public class MockLockFactory extends LockFactory {

        public boolean lockPrefixSet;

        public Map locksCreated = Collections.synchronizedMap(new HashMap());

        public int makeLockCount = 0;

        @Override
        public void setLockPrefix(String lockPrefix) {
            super.setLockPrefix(lockPrefix);
            lockPrefixSet = true;
        }

        @Override
        public synchronized Lock makeLock(String lockName) {
            Lock lock = new MockLock();
            locksCreated.put(lockName, lock);
            makeLockCount++;
            return lock;
        }

        @Override
        public void clearLock(String specificLockName) {
        }

        public class MockLock extends Lock {

            public int lockAttempts;

            @Override
            public boolean obtain() {
                lockAttempts++;
                return true;
            }

            @Override
            public void release() {
            }

            @Override
            public boolean isLocked() {
                return false;
            }
        }
    }

    private void addDoc(IndexWriter writer) throws IOException {
        Document doc = new Document();
        doc.add(new Field("content", "aaa", Field.Store.NO, Field.Index.ANALYZED));
        writer.addDocument(doc);
    }
}
