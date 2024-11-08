package net.sourceforge.tess4j;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel.MapMode;
import org.junit.*;
import static org.junit.Assert.*;

public class TessDllAPITest {

    public TessDllAPITest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of TessDllBeginPage method, of class TessDllLibrary.
     */
    @Ignore
    @Test
    public void testTessDllBeginPage() {
        System.out.println("TessDllBeginPage");
        int xsize = 0;
        int ysize = 0;
        ByteBuffer buf = null;
        TessDllAPI instance = new TessDllAPIImpl();
        int expResult = 0;
        int result = instance.TessDllBeginPage(xsize, ysize, buf);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of TessDllBeginPageLang method, of class TessDllLibrary.
     */
    @Ignore
    @Test
    public void testTessDllBeginPageLang() {
        System.out.println("TessDllBeginPageLang");
        int xsize = 0;
        int ysize = 0;
        ByteBuffer buf = null;
        String lang = "";
        TessDllAPI instance = new TessDllAPIImpl();
        int expResult = 0;
        int result = instance.TessDllBeginPageLang(xsize, ysize, buf, lang);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of TessDllBeginPageUpright method, of class TessDllLibrary.
     */
    @Ignore
    @Test
    public void testTessDllBeginPageUpright() {
        System.out.println("TessDllBeginPageUpright");
        int xsize = 0;
        int ysize = 0;
        ByteBuffer buf = null;
        String lang = "";
        TessDllAPI instance = new TessDllAPIImpl();
        int expResult = 0;
        int result = instance.TessDllBeginPageUpright(xsize, ysize, buf, lang);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of TessDllBeginPageBPP method, of class TessDllLibrary.
     */
    @Ignore
    @Test
    public void testTessDllBeginPageBPP() {
        System.out.println("TessDllBeginPageBPP");
        int xsize = 0;
        int ysize = 0;
        ByteBuffer buf = null;
        byte bpp = 0;
        TessDllAPI instance = new TessDllAPIImpl();
        int expResult = 0;
        int result = instance.TessDllBeginPageBPP(xsize, ysize, buf, bpp);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of TessDllBeginPageLangBPP method, of class TessDllLibrary.
     */
    @Ignore
    @Test
    public void testTessDllBeginPageLangBPP() {
        System.out.println("TessDllBeginPageLangBPP");
        int xsize = 0;
        int ysize = 0;
        ByteBuffer buf = null;
        String lang = "";
        byte bpp = 0;
        TessDllAPI instance = new TessDllAPIImpl();
        int expResult = 0;
        int result = instance.TessDllBeginPageLangBPP(xsize, ysize, buf, lang, bpp);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of TessDllBeginPageUprightBPP method, of class TessDllLibrary.
     */
    @Ignore
    @Test
    public void testTessDllBeginPageUprightBPP() {
        System.out.println("TessDllBeginPageUprightBPP");
        int xsize = 0;
        int ysize = 0;
        ByteBuffer buf = null;
        String lang = "";
        byte bpp = 0;
        TessDllAPI instance = new TessDllAPIImpl();
        int expResult = 0;
        int result = instance.TessDllBeginPageUprightBPP(xsize, ysize, buf, lang, bpp);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of TessDllEndPage method, of class TessDllLibrary.
     */
    @Ignore
    @Test
    public void testTessDllEndPage() {
        System.out.println("TessDllEndPage");
        TessDllAPI instance = new TessDllAPIImpl();
        instance.TessDllEndPage();
        fail("The test case is a prototype.");
    }

    /**
     * Test of TessDllRecognize_a_Block method, of class TessDllLibrary.
     */
    @Test
    public void testTessDllRecognize_a_Block() throws Exception {
        System.out.println("TessDllRecognize_a_Block");
        TessDllAPI api = new TessDllAPIImpl().getInstance();
        String expResult = "The (quick) [brown] {fox} jumps!\nOver the $43,456.78 <lazy> #90 dog";
        String lang = "eng";
        File tiff = new File("eurotext.tif");
        BufferedImage image = ImageIO.read(tiff);
        MappedByteBuffer buf = new FileInputStream(tiff).getChannel().map(MapMode.READ_ONLY, 0, tiff.length());
        int resultRead = api.TessDllBeginPageUpright(image.getWidth(), image.getHeight(), buf, lang);
        ETEXT_DESC output = api.TessDllRecognize_a_Block(91, 91 + 832, 170, 170 + 614);
        EANYCODE_CHAR[] text = (EANYCODE_CHAR[]) output.text[0].toArray(output.count);
        List<Byte> unistr = new ArrayList<Byte>();
        int j = 0;
        for (int i = 0; i < output.count; i = j) {
            final EANYCODE_CHAR ch = text[i];
            for (int b = 0; b < ch.blanks; ++b) {
                unistr.add((byte) ' ');
            }
            for (j = i; j < output.count; j++) {
                final EANYCODE_CHAR unich = text[j];
                if (ch.left != unich.left || ch.right != unich.right || ch.top != unich.top || ch.bottom != unich.bottom) {
                    break;
                }
                unistr.add(unich.char_code);
            }
            if ((ch.formatting & 64) == 64) {
                unistr.add((byte) '\n');
            } else if ((ch.formatting & 128) == 128) {
                unistr.add((byte) '\n');
                unistr.add((byte) '\n');
            }
        }
        byte[] bb = Tesseract.wrapperListToByteArray(unistr);
        String result = new String(bb, "utf8");
        System.out.println(result);
        assertEquals(expResult, result.substring(0, expResult.length()));
    }

    /**
     * Test of TessDllRecognize_all_Words method, of class TessDllLibrary.
     */
    @Test
    public void testTessDllRecognize_all_Words() throws Exception {
        System.out.println("TessDllRecognize_all_Words");
        TessDllAPI api = new TessDllAPIImpl().getInstance();
        String expResult = "The (quick) [brown] {fox} jumps!\nOver the $43,456.78 <lazy> #90 dog";
        String lang = "eng";
        File tiff = new File("eurotext.tif");
        BufferedImage image = ImageIO.read(new FileInputStream(tiff));
        MappedByteBuffer buf = new FileInputStream(tiff).getChannel().map(MapMode.READ_ONLY, 0, tiff.length());
        int resultRead = api.TessDllBeginPageUpright(image.getWidth(), image.getHeight(), buf, lang);
        ETEXT_DESC output = api.TessDllRecognize_all_Words();
        EANYCODE_CHAR[] text = (EANYCODE_CHAR[]) output.text[0].toArray(output.count);
        StringBuilder sb = new StringBuilder();
        int j = 0;
        for (int i = 0; i < output.count; i = j) {
            final EANYCODE_CHAR ch = text[i];
            List<Byte> unistr = new ArrayList<Byte>();
            for (int b = 0; b < ch.blanks; ++b) {
                sb.append(" ");
            }
            for (j = i; j < output.count; j++) {
                final EANYCODE_CHAR unich = text[j];
                if (ch.left != unich.left || ch.right != unich.right || ch.top != unich.top || ch.bottom != unich.bottom) {
                    break;
                }
                unistr.add(unich.char_code);
            }
            byte[] bb = Tesseract.wrapperListToByteArray(unistr);
            String chr = new String(bb, "utf8");
            sb.append(chr);
            if ((ch.formatting & 64) == 64) {
                sb.append('\n');
            } else if ((ch.formatting & 128) == 128) {
                sb.append("\n\n");
            }
        }
        String result = sb.toString();
        System.out.println(result);
        assertEquals(expResult, result.substring(0, expResult.length()));
    }

    /**
     * Test of TessDllRelease method, of class TessDllLibrary.
     */
    @Ignore
    @Test
    public void testTessDllRelease() {
        System.out.println("TessDllRelease");
        TessDllAPI instance = new TessDllAPIImpl();
        instance.TessDllRelease();
        fail("The test case is a prototype.");
    }

    public class TessDllAPIImpl implements TessDllAPI {

        public TessDllAPI getInstance() {
            return INSTANCE;
        }

        public int TessDllBeginPage(int xsize, int ysize, ByteBuffer buf) {
            return 0;
        }

        public int TessDllBeginPageLang(int xsize, int ysize, ByteBuffer buf, String lang) {
            return 0;
        }

        public int TessDllBeginPageUpright(int xsize, int ysize, ByteBuffer buf, String lang) {
            return 0;
        }

        public int TessDllBeginPageBPP(int xsize, int ysize, ByteBuffer buf, byte bpp) {
            return 0;
        }

        public int TessDllBeginPageLangBPP(int xsize, int ysize, ByteBuffer buf, String lang, byte bpp) {
            return 0;
        }

        public int TessDllBeginPageUprightBPP(int xsize, int ysize, ByteBuffer buf, String lang, byte bpp) {
            return 0;
        }

        public void TessDllEndPage() {
        }

        public ETEXT_DESC TessDllRecognize_a_Block(int left, int right, int top, int bottom) {
            return null;
        }

        public ETEXT_DESC TessDllRecognize_all_Words() {
            return null;
        }

        public void TessDllRelease() {
        }

        public boolean SetVariable(String variable, String value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void SimpleInit(String datapath, String language, boolean numeric_mode) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int Init(String datapath, String outputbase, String configfile, boolean numeric_mode, int argc, String[] argv) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int InitWithLanguage(String datapath, String outputbase, String language, String configfile, boolean numeric_mode, int argc, String[] argv) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int InitLangMod(String datapath, String outputbase, String language, String configfile, boolean numeric_mode, int argc, String[] argv) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void SetInputName(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String TesseractRect(ByteBuffer imagedata, int bytes_per_pixel, int bytes_per_line, int left, int top, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String TesseractRectBoxes(ByteBuffer imagedata, int bytes_per_pixel, int bytes_per_line, int left, int top, int width, int height, int imageheight) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String TesseractRectUNLV(ByteBuffer imagedata, int bytes_per_pixel, int bytes_per_line, int left, int top, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void ClearAdaptiveClassifier() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void End() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void DumpPGM(String filename) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Image GetTesseractImage() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int OtsuStats(int histogram, int H_out, int omega0_out) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int IsValidWord(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
