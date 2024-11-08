package org.apache.lucene.index;

import org.apache.lucene.util.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;
import java.util.Random;
import java.io.File;
import java.io.IOException;

public class TestAtomicUpdate extends LuceneTestCase {

    private static final Analyzer ANALYZER = new SimpleAnalyzer();

    private Random RANDOM;

    public class MockIndexWriter extends IndexWriter {

        public MockIndexWriter(Directory dir, Analyzer a, boolean create, IndexWriter.MaxFieldLength mfl) throws IOException {
            super(dir, a, create, mfl);
        }

        @Override
        boolean testPoint(String name) {
            if (RANDOM.nextInt(4) == 2) Thread.yield();
            return true;
        }
    }

    private abstract static class TimedThread extends Thread {

        boolean failed;

        int count;

        private static int RUN_TIME_SEC = 3;

        private TimedThread[] allThreads;

        public abstract void doWork() throws Throwable;

        TimedThread(TimedThread[] threads) {
            this.allThreads = threads;
        }

        @Override
        public void run() {
            final long stopTime = System.currentTimeMillis() + 1000 * RUN_TIME_SEC;
            count = 0;
            try {
                while (System.currentTimeMillis() < stopTime && !anyErrors()) {
                    doWork();
                    count++;
                }
            } catch (Throwable e) {
                System.out.println(Thread.currentThread().getName() + ": exc");
                e.printStackTrace(System.out);
                failed = true;
            }
        }

        private boolean anyErrors() {
            for (int i = 0; i < allThreads.length; i++) if (allThreads[i] != null && allThreads[i].failed) return true;
            return false;
        }
    }

    private static class IndexerThread extends TimedThread {

        IndexWriter writer;

        public int count;

        public IndexerThread(IndexWriter writer, TimedThread[] threads) {
            super(threads);
            this.writer = writer;
        }

        @Override
        public void doWork() throws Exception {
            for (int i = 0; i < 100; i++) {
                Document d = new Document();
                d.add(new Field("id", Integer.toString(i), Field.Store.YES, Field.Index.NOT_ANALYZED));
                d.add(new Field("contents", English.intToEnglish(i + 10 * count), Field.Store.NO, Field.Index.ANALYZED));
                writer.updateDocument(new Term("id", Integer.toString(i)), d);
            }
        }
    }

    private static class SearcherThread extends TimedThread {

        private Directory directory;

        public SearcherThread(Directory directory, TimedThread[] threads) {
            super(threads);
            this.directory = directory;
        }

        @Override
        public void doWork() throws Throwable {
            IndexReader r = IndexReader.open(directory, true);
            assertEquals(100, r.numDocs());
            r.close();
        }
    }

    public void runTest(Directory directory) throws Exception {
        TimedThread[] threads = new TimedThread[4];
        IndexWriter writer = new MockIndexWriter(directory, ANALYZER, true, IndexWriter.MaxFieldLength.UNLIMITED);
        writer.setMaxBufferedDocs(7);
        writer.setMergeFactor(3);
        for (int i = 0; i < 100; i++) {
            Document d = new Document();
            d.add(new Field("id", Integer.toString(i), Field.Store.YES, Field.Index.NOT_ANALYZED));
            d.add(new Field("contents", English.intToEnglish(i), Field.Store.NO, Field.Index.ANALYZED));
            if ((i - 1) % 7 == 0) {
                writer.commit();
            }
            writer.addDocument(d);
        }
        writer.commit();
        IndexReader r = IndexReader.open(directory, true);
        assertEquals(100, r.numDocs());
        r.close();
        IndexerThread indexerThread = new IndexerThread(writer, threads);
        threads[0] = indexerThread;
        indexerThread.start();
        IndexerThread indexerThread2 = new IndexerThread(writer, threads);
        threads[1] = indexerThread2;
        indexerThread2.start();
        SearcherThread searcherThread1 = new SearcherThread(directory, threads);
        threads[2] = searcherThread1;
        searcherThread1.start();
        SearcherThread searcherThread2 = new SearcherThread(directory, threads);
        threads[3] = searcherThread2;
        searcherThread2.start();
        indexerThread.join();
        indexerThread2.join();
        searcherThread1.join();
        searcherThread2.join();
        writer.close();
        assertTrue("hit unexpected exception in indexer", !indexerThread.failed);
        assertTrue("hit unexpected exception in indexer2", !indexerThread2.failed);
        assertTrue("hit unexpected exception in search1", !searcherThread1.failed);
        assertTrue("hit unexpected exception in search2", !searcherThread2.failed);
    }

    public void testAtomicUpdates() throws Exception {
        RANDOM = newRandom();
        Directory directory;
        directory = new MockRAMDirectory();
        runTest(directory);
        directory.close();
        File dirPath = _TestUtil.getTempDir("lucene.test.atomic");
        directory = FSDirectory.open(dirPath);
        runTest(directory);
        directory.close();
        _TestUtil.rmDir(dirPath);
    }
}
