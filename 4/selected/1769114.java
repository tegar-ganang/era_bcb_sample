package neembuu.vfs.test.test;

import java.io.File;
import neembuu.common.RangeArray;
import neembuu.common.RangeArrayElement;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JOptionPane;
import neembuu.util.ContentPeek;
import org.junit.BeforeClass;
import org.junit.Test;
import static junit.framework.Assert.*;

/**
 *
 * @author Shashank Tulsyan
 */
public final class BoundaryConditions {

    private static FileChannel fc1 = null;

    private static FileChannel fc2 = null;

    private static final int KB = 1000;

    private static final int MB = 100000;

    @BeforeClass
    public static void initalize() throws Exception {
        File storageDirectory = new File("j:\\neembuu\\heap\\test120k.http.rmvb_neembuu_download_data");
        File[] files = storageDirectory.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            file.delete();
        }
        JOptionPane.showMessageDialog(null, "Start");
        try {
            fc1 = new FileInputStream("j:\\neembuu\\virtual\\monitored.nbvfs\\test120k.http.rmvb").getChannel();
        } catch (Exception e) {
            fc1 = null;
        }
        try {
            fc2 = new FileInputStream("j:\\neembuu\\realfiles\\test120k.rmvb").getChannel();
        } catch (Exception e) {
            fc2 = null;
        }
        assertNotNull(fc1);
        assertNotNull(fc2);
        ByteBuffer region1, region11, region2, region3, region4;
    }

    private static final int allowance = 5 * KB;

    private static final int requestSize = (int) (MB * 0.4);

    public static void printContentPeek(ByteBuffer bb_tocheck, ByteBuffer bb_actual) {
        System.out.println("bbactual+{" + ContentPeek.generatePeekString(bb_actual) + "}");
        System.out.println("bbtocheck+{" + ContentPeek.generatePeekString(bb_tocheck) + "}");
    }

    private String region(long start, int size) {
        return start + "-->" + (start - 1 + size);
    }

    public void connectionResumeCheck() throws Exception {
        System.out.println("connectionResumeCheck");
        ByteBuffer region11;
        fc1.position(0);
        fc1.read((region11 = ByteBuffer.allocateDirect(MB)));
        JOptionPane.showMessageDialog(null, "Try resume");
        fc1.position(MB);
        fc1.read((region11 = ByteBuffer.allocateDirect(MB)));
        ByteBuffer bb_actual = ByteBuffer.allocateDirect(MB);
        fc2.position(MB);
        fc2.read(bb_actual);
        checkBuffers(region11, bb_actual);
    }

    @Test
    public void linearRead() throws Exception {
        System.out.println("linearRead=" + region(0, requestSize));
        ByteBuffer bb_toCheck, bb_actual;
        fc1.position(0);
        fc1.read((bb_toCheck = ByteBuffer.allocateDirect(requestSize)));
        fc2.position(0);
        fc2.read((bb_actual = ByteBuffer.allocateDirect(requestSize)));
        printContentPeek(bb_toCheck, bb_actual);
        checkBuffers(bb_toCheck, bb_actual);
    }

    @Test
    public void case_start_1_sameend() throws Exception {
        System.out.println("case_start_1_sameend " + region(MB - allowance, requestSize));
        ByteBuffer bb_toCheck, bb_actual;
        fc1.position(MB - allowance);
        fc1.read((bb_toCheck = ByteBuffer.allocateDirect(requestSize)));
        fc2.position(MB - allowance);
        fc2.read((bb_actual = ByteBuffer.allocateDirect(requestSize)));
        printContentPeek(bb_toCheck, bb_actual);
        checkBuffers(bb_toCheck, bb_actual);
    }

    @Test
    public void case_start_2_sameend() throws Exception {
        System.out.println("case_start_2_sameend " + region(MB + allowance, requestSize));
        ByteBuffer bb_toCheck, bb_actual;
        fc1.position(MB + allowance);
        fc1.read((bb_toCheck = ByteBuffer.allocateDirect(requestSize)));
        fc2.position(MB + allowance);
        fc2.read((bb_actual = ByteBuffer.allocateDirect(requestSize)));
        printContentPeek(bb_toCheck, bb_actual);
        checkBuffers(bb_toCheck, bb_actual);
    }

    @Test
    public void case_start_2_end() throws Exception {
        System.out.println("case_start_2_end " + region(MB + allowance, MB * 2));
        ByteBuffer bb_toCheck, bb_actual;
        fc1.position(MB + allowance);
        fc1.read((bb_toCheck = ByteBuffer.allocateDirect(MB * 2)));
        fc2.position(MB + allowance);
        fc2.read((bb_actual = ByteBuffer.allocateDirect(MB * 2)));
        printContentPeek(bb_toCheck, bb_actual);
        checkBuffers(bb_toCheck, bb_actual);
    }

    @Test
    public void case_start_1_end() throws Exception {
        System.out.println("case_start_1_end " + region(MB - allowance, MB * 2 + 2 * allowance));
        ByteBuffer bb_toCheck, bb_actual;
        int start = MB - allowance;
        int size = MB * 2 + 2 * allowance;
        fc1.position(start);
        fc1.read((bb_toCheck = ByteBuffer.allocateDirect(size)));
        fc2.position(start);
        fc2.read((bb_actual = ByteBuffer.allocateDirect(size)));
        printContentPeek(bb_toCheck, bb_actual);
        checkBuffers(bb_toCheck, bb_actual);
    }

    @Test
    public void case_start_singleinter_end() throws Exception {
        System.out.println("case_start_singleinter_end " + region(MB - allowance, MB * 4 + 2 * allowance));
        ByteBuffer bb_toCheck, bb_actual;
        fc1.position(MB - allowance);
        fc1.read((bb_toCheck = ByteBuffer.allocateDirect(MB * 4 + 2 * allowance)));
        fc2.position(MB - allowance);
        fc2.read((bb_actual = ByteBuffer.allocateDirect(MB * 4 + 2 * allowance)));
        printContentPeek(bb_toCheck, bb_actual);
        checkBuffers(bb_toCheck, bb_actual);
    }

    @Test
    public void case_start_multipleinter_end_inside() throws Exception {
        System.out.println("case_start_multipleinter_end_inside " + region(MB - allowance, MB * 7 + 2 * allowance));
        ByteBuffer bb_toCheck, bb_actual;
        fc1.position(MB - allowance);
        fc1.read((bb_toCheck = ByteBuffer.allocateDirect(MB * 7 + 2 * allowance)));
        fc2.position(MB - allowance);
        fc2.read((bb_actual = ByteBuffer.allocateDirect(MB * 7 + 2 * allowance)));
        printContentPeek(bb_toCheck, bb_actual);
        checkBuffers(bb_toCheck, bb_actual);
    }

    @Test
    public void case_start_multipleinter_end_outside() throws Exception {
        System.out.println("case_start_multipleinter_end_outside " + region(MB - allowance, MB * 9 + 2 * allowance));
        ByteBuffer bb_toCheck, bb_actual;
        fc1.position(MB - allowance);
        fc1.read((bb_toCheck = ByteBuffer.allocateDirect(MB * 9 + 2 * allowance)));
        fc2.position(MB - allowance);
        fc2.read((bb_actual = ByteBuffer.allocateDirect(MB * 9 + 2 * allowance)));
        printContentPeek(bb_toCheck, bb_actual);
        checkBuffers(bb_toCheck, bb_actual);
    }

    public static void checkBuffers(ByteBuffer toCheck, ByteBuffer actual) {
        toCheck = (ByteBuffer) toCheck.position(0);
        actual = (ByteBuffer) actual.position(0);
        for (int i = 0; i < toCheck.capacity(); i++) {
            if (toCheck.get() != actual.get()) {
                System.out.println("matches till=" + (i - 1));
                assertTrue(false);
            }
        }
    }
}
