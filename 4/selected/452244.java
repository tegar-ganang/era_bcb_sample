package org.jcvi.vics.shared.fasta;

import junit.framework.TestCase;
import org.jcvi.vics.model.genomics.BaseSequenceEntity;
import org.jcvi.vics.model.genomics.ORF;
import org.jcvi.vics.shared.TestUtils;
import org.jcvi.vics.shared.utils.InFileChannelHandler;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FASTAFileNodeHelperTest extends TestCase {

    public FASTAFileNodeHelperTest() {
        super();
    }

    public void testSearchOneSequence() {
        FASTAFileNodeHelper fastaHelper = new FASTAFileNodeHelper();
        RandomAccessFile fastaFile = null;
        try {
            File testFile = TestUtils.getTestFile("shared" + File.separator + "testfiles" + File.separator + "fasta" + File.separator + "test.nucleotide.fasta");
            String testAcc = "JCVI_ORF_1096130161890";
            fastaFile = new RandomAccessFile(testFile, "r");
            InFileChannelHandler fastaFileChannelHandler = new InFileChannelHandler(fastaFile.getChannel());
            FASTAFileNodeHelper.FASTASequenceCache fastaSequenceCache = new FASTAFileNodeHelper.FASTASequenceCache();
            BaseSequenceEntity seqEntity = fastaHelper.readSequence(fastaFileChannelHandler, testAcc, fastaSequenceCache);
            assertTrue(seqEntity != null && seqEntity instanceof ORF);
            assertTrue(seqEntity.getCameraAcc().equals(testAcc));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            if (fastaFile != null) {
                try {
                    fastaFile.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public void testSearchMultipleSequences() {
        FASTAFileNodeHelper fastaHelper = new FASTAFileNodeHelper();
        File testIndexFile = null;
        String testFileName = TestUtils.getTestFile("shared" + File.separator + "testfiles" + File.separator + "fasta" + File.separator + "test.nucleotide.fasta").getAbsolutePath();
        String testIndexFileName = TestUtils.getTestFile("shared" + File.separator + "testfiles" + File.separator + "fasta" + File.separator + "test.nucleotide.fasta.ndx").getAbsolutePath();
        try {
            Set<String> testAccessions = null;
            Set<BaseSequenceEntity> seqEntities;
            seqEntities = fastaHelper.readSequences(testFileName, testIndexFileName, testAccessions);
            assertTrue(seqEntities != null);
            assertTrue(seqEntities.size() == 163);
            testIndexFile = new File(testIndexFileName);
            if (testIndexFile.exists()) {
                testIndexFile.delete();
            }
            testAccessions = new HashSet<String>();
            Collections.addAll(testAccessions, "JCVI_ORF_1096130161884", "JCVI_ORF_1096130162208", "JCVI_ORF_1096130162200");
            seqEntities = fastaHelper.readSequences(testFileName, testIndexFileName, testAccessions);
            assertTrue(seqEntities != null);
            assertTrue(seqEntities.size() == 3);
            fastaHelper.createFASTAIndexFile(testFileName, testIndexFileName);
            seqEntities = fastaHelper.readSequences(testFileName, testIndexFileName, testAccessions);
            assertTrue(seqEntities != null);
            assertTrue(seqEntities.size() == 3);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            testIndexFile = new File(testIndexFileName);
            if (testIndexFile.exists()) {
                testIndexFile.delete();
            }
        }
    }

    public void testIndexFASTA() {
        FASTAFileNodeHelper fastaHelper = new FASTAFileNodeHelper();
        String testFileName = TestUtils.getTestFile("shared" + File.separator + "testfiles" + File.separator + "fasta" + File.separator + "test.nucleotide.fasta").getAbsolutePath();
        String testIndexFileName = TestUtils.getTestFile("shared" + File.separator + "testfiles" + File.separator + "fasta" + File.separator + "test.nucleotide.fasta.ndx").getAbsolutePath();
        try {
            fastaHelper.createFASTAIndexFile(testFileName, testIndexFileName);
            String testAcc = "JCVI_ORF_1096130161922";
            FASTAFileNodeHelper.FASTASequenceCache fastaSequenceCache = new FASTAFileNodeHelper.FASTASequenceCache();
            long testPos = fastaHelper.searchSequencePos(testIndexFileName, testAcc, fastaSequenceCache);
            assertTrue(testPos == 10559 || testPos == 10463);
            assertEquals(fastaSequenceCache.getSequenceEntityPos(testAcc), testPos);
            testPos = fastaHelper.searchSequencePos(testIndexFileName, "NOT THERE", fastaSequenceCache);
            assertEquals(testPos, -1);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
        }
    }

    public void testIndexLargeFASTA() {
        FASTAFileNodeHelper fastaHelper = new FASTAFileNodeHelper();
        String testFileName = TestUtils.getTestFile("shared" + File.separator + "testfiles" + File.separator + "fasta" + File.separator + "largetest.nucleotide.fasta").getAbsolutePath();
        String testIndexFileName = TestUtils.getTestFile("shared" + File.separator + "testfiles" + File.separator + "fasta" + File.separator + "largetest.nucleotide.fasta.ndx").getAbsolutePath();
        try {
            fastaHelper.createFASTAIndexFile(testFileName, testIndexFileName);
            FASTAFileNodeHelper.FASTASequenceCache fastaSequenceCache = new FASTAFileNodeHelper.FASTASequenceCache();
            String testAcc = "62000019";
            long testPos = fastaHelper.searchSequencePos(testIndexFileName, testAcc, fastaSequenceCache);
            assertTrue(testPos == 1681L || testPos == 1645L);
            assertTrue(fastaSequenceCache.getSequenceEntityPos(testAcc) == testPos);
            testAcc = "62086754";
            testPos = fastaHelper.searchSequencePos(testIndexFileName, testAcc, fastaSequenceCache);
            assertTrue(testPos == 9733961 || testPos == 0x91e187L);
            assertEquals(fastaSequenceCache.getSequenceEntityPos(testAcc), testPos);
            testAcc = "62106906";
            testPos = fastaHelper.searchSequencePos(testIndexFileName, testAcc, fastaSequenceCache);
            assertTrue(testPos == 12190899 || testPos == 0xb6c181L);
            assertEquals(fastaSequenceCache.getSequenceEntityPos(testAcc), testPos);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
        }
    }
}
