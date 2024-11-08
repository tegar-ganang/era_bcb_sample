package jOptical.Bitmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * @author dennis
 */
public class BitmapFileTest extends TestCase {

    /**
	 * Test method for
	 * {@link jOptical.Bitmap.BitmapFile#writeBitmap(java.lang.String, jOptical.Bitmap.BitmapObject)}.
	 * @throws IOException 
	 */
    public final void testWriteBitmap() throws IOException {
        (new File("images/test/")).mkdir();
        BitmapObject bmo = BitmapFile.readBitmap("images/in_write.bmp");
        BitmapFile.writeBitmap("images/test/test_write.bmp", bmo);
        assertEquals(true, compareFiles("images/test/test_write.bmp", "images/in_write.bmp"));
    }

    /**
	 * Test method for
	 * {@link jOptical.Bitmap.BitmapFile#readBitmap(java.lang.String)}.
	 *
	 * @throws FileNotFoundException
	 */
    public final void testReadBitmap() throws FileNotFoundException {
        BitmapObject bmo = BitmapFile.readBitmap("images/in_objects.bmp");
        assertNotNull(bmo);
        assertEquals(20, bmo.getWidth());
        assertEquals(20, bmo.getHeight());
        assertEquals(4, bmo.getColourDepth());
        assertEquals(0, bmo.bitMap[0][0].getGrayscaleValue());
        assertEquals(0, bmo.bitMap[0][1].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][2].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][3].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][4].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][5].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][6].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][7].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][8].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][9].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][10].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][11].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][12].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][13].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][14].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][15].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][16].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][17].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][18].getGrayscaleValue());
        assertEquals(255, bmo.bitMap[0][19].getGrayscaleValue());
    }

    public static void testColourBitmap() throws FileNotFoundException {
        BitmapObject bmo = BitmapFile.readBitmap("images/spectrum.bmp");
        assertEquals(24, bmo.getColourDepth());
        BitmapFile.writeBitmap("images/test/spectrum.bmp", bmo);
        assertEquals(true, compareFiles("images/test/spectrum.bmp", "images/spectrum.bmp"));
    }

    public static final boolean compareFiles(String file1, String file2) {
        try {
            File f1 = new File(file1);
            File f2 = new File(file2);
            long l = 0;
            if (!f1.exists() || !f2.exists()) return (false);
            FileReader br1 = new FileReader(f1);
            ;
            FileReader br2 = new FileReader(f2);
            ;
            int r1, r2;
            assertEquals(f1.length(), f2.length());
            while ((r1 = br1.read()) != -1 && (r2 = br2.read()) != -1) {
                if (r1 != r2) {
                    System.out.println(file1);
                    System.out.println(l);
                    return (false);
                }
                l++;
            }
            br1.close();
            br2.close();
            return (true);
        } catch (IOException e) {
            return (false);
        }
    }
}
