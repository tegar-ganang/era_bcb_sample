package org.apache.lucene.index;

import java.util.Random;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.TestIndexWriterReader.HeavyAtomicInt;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MockRAMDirectory;
import org.apache.lucene.util.LuceneTestCase;

public class TestNRTReaderWithThreads extends LuceneTestCase {

    Random random = new Random();

    HeavyAtomicInt seq = new HeavyAtomicInt(1);

    public void testIndexing() throws Exception {
        Directory mainDir = new MockRAMDirectory();
        IndexWriter writer = new IndexWriter(mainDir, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
        writer.setUseCompoundFile(false);
        IndexReader reader = writer.getReader();
        reader.close();
        writer.setMergeFactor(2);
        writer.setMaxBufferedDocs(10);
        RunThread[] indexThreads = new RunThread[4];
        for (int x = 0; x < indexThreads.length; x++) {
            indexThreads[x] = new RunThread(x % 2, writer);
            indexThreads[x].setName("Thread " + x);
            indexThreads[x].start();
        }
        long startTime = System.currentTimeMillis();
        long duration = 5 * 1000;
        while ((System.currentTimeMillis() - startTime) < duration) {
            Thread.sleep(100);
        }
        int delCount = 0;
        int addCount = 0;
        for (int x = 0; x < indexThreads.length; x++) {
            indexThreads[x].run = false;
            assertTrue(indexThreads[x].ex == null);
            addCount += indexThreads[x].addCount;
            delCount += indexThreads[x].delCount;
        }
        for (int x = 0; x < indexThreads.length; x++) {
            indexThreads[x].join();
        }
        writer.close();
        mainDir.close();
    }

    public class RunThread extends Thread {

        IndexWriter writer;

        boolean run = true;

        Throwable ex;

        int delCount = 0;

        int addCount = 0;

        int type;

        public RunThread(int type, IndexWriter writer) {
            this.type = type;
            this.writer = writer;
        }

        @Override
        public void run() {
            try {
                while (run) {
                    if (type == 0) {
                        int i = seq.addAndGet(1);
                        Document doc = TestIndexWriterReader.createDocument(i, "index1", 10);
                        writer.addDocument(doc);
                        addCount++;
                    } else if (type == 1) {
                        IndexReader reader = writer.getReader();
                        int id = random.nextInt(seq.intValue());
                        Term term = new Term("id", Integer.toString(id));
                        int count = TestIndexWriterReader.count(term, reader);
                        writer.deleteDocuments(term);
                        reader.close();
                        delCount += count;
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
                this.ex = ex;
                run = false;
            }
        }
    }
}
