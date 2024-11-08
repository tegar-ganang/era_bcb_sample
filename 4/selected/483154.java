package com.loribel.commons.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import junit.framework.TestCase;
import com.loribel.commons.abstraction.ENCODING;
import com.loribel.commons.util.FTools;
import com.loribel.commons.util.GB_FileTools;
import com.loribel.commons.util.STools;

/**
 * Classe de test pour GB_StringTools.
 *
 * @author Gregory Borelli
 */
public class GB_FileToolsTest extends TestCase {

    public GB_FileToolsTest(String a_name) {
        super(a_name);
    }

    /**
     * Returns a temp file.
     *   - path : tempPath.
     *   - Delete it if exist.
     *   - DeleteOnExist.
     *
     * @param a_index int -
     * @param a_filename String -
     *
     * @return File
     */
    protected File getTempFile(int a_index, String a_filename) {
        File l_path = GB_FileTools.getTempDir();
        File retour = new File(l_path, a_filename);
        retour.delete();
        retour.deleteOnExit();
        assertTrue(a_index + "- Temp file " + a_filename + " already exist", !retour.canRead());
        return retour;
    }

    /**
     * Tests method <tt>copy</tt>.
     */
    public void test_copy() throws IOException {
        File l_file = getTempFile(1, "testCopy.txt");
        File l_file2 = getTempFile(2, "testCopy2.txt");
        String l_content = "test test\n\ntest";
        GB_FileTools.writeFile(l_file, l_content, false);
        assertTrue(3 + " - file OK", l_file.canRead());
        GB_FileTools.copy(l_file, l_file2);
        assertTrue(4 + " - file2 OK", l_file2.canRead());
        String l_content2 = GB_FileTools.readFile(l_file2);
        assertEquals(5 + " - Content verification", l_content, l_content2);
        String l_content3 = "";
        GB_FileTools.writeFile(l_file2, l_content3, false);
        l_content2 = GB_FileTools.readFile(l_file2);
        assertEquals(6 + " - file2 clear", l_content3, l_content2);
        GB_FileTools.copy(l_file, l_file2);
        l_content2 = GB_FileTools.readFile(l_file2);
        assertEquals(7 + " - Check copy if fileDest exist", l_content, l_content2);
    }

    /**
     * Tests method <tt>equals</tt>.
     */
    public void test_equals() {
        String l_file1;
        String l_file2;
        l_file1 = "C:/toto";
        l_file2 = "C:/toto";
        test_equals(1, l_file1, l_file2, true);
    }

    private void test_equals(int a_index, String a_file1, String a_file2, boolean a_equal) {
        boolean l_result;
        l_result = GB_FileTools.equalsPath(a_file1, a_file2);
        assertEquals(a_index + .1 + " - " + a_file1 + " - " + a_file2, a_equal, l_result);
        a_file1 = STools.replace(a_file1, "/", "\\");
        l_result = GB_FileTools.equalsPath(a_file1, a_file2);
        assertEquals(a_index + .2 + " - " + a_file1 + " - " + a_file2, a_equal, l_result);
        a_file2 = STools.replace(a_file2, "/", "\\");
        l_result = GB_FileTools.equalsPath(a_file1, a_file2);
        assertEquals(a_index + .3 + " - " + a_file1 + " - " + a_file2, a_equal, l_result);
    }

    /**
     * Tests method <tt>extensionToLowerCase</tt>.
     */
    public void test_extensionToLowerCase() {
        test_extensionToLowerCase(1, "toto.xml", "toto.xml");
        test_extensionToLowerCase(2, "toto.XML", "toto.xml");
        test_extensionToLowerCase(3, "toto.Xml", "toto.xml");
        test_extensionToLowerCase(4, "TOTO.XML", "TOTO.xml");
        test_extensionToLowerCase(5, "C:/tata.toto.XML", "C:/tata.toto.xml");
    }

    public void test_extensionToLowerCase(int a_index, String a_filename, String a_expected) {
        String l_newName = FTools.toLowerCaseExtension(a_filename);
        assertEquals(a_index + .1 + " - " + a_filename, a_expected, l_newName);
        l_newName = FTools.toLowerCaseExtension(l_newName);
        assertEquals(a_index + .2 + " - " + a_filename, a_expected, l_newName);
    }

    /**
     * Tests method <tt>getExtension</tt>.
     */
    public void test_getExtension() {
        test_getExtension(1, "toto.xml", "xml");
        test_getExtension(2, "toto.xml.xml", "xml");
        test_getExtension(3, "toto.x.m..l..xml", "xml");
        test_getExtension(4, "toto", "");
        test_getExtension(5, "C:/toto/toto.gif", "gif");
        test_getExtension(6, "C:/toto/toto.GIF", "GIF");
    }

    public void test_getExtension(int a_index, String a_filename, String a_extension) {
        String l_ext = FTools.getExtension(a_filename);
        assertEquals(a_index + .1 + " - " + a_filename, a_extension, l_ext);
        l_ext = FTools.getExtension(a_filename);
        assertEquals(a_index + .2 + " - " + a_filename + " (File)", a_extension, l_ext);
        l_ext = FTools.getExtension(a_filename);
        assertEquals(a_index + .3 + " - " + a_filename + " (File 2)", a_extension, l_ext);
    }

    /**
     * Tests method <tt>nameWithConvention</tt>.
     */
    public void test_nameWithConvention() {
        String l_file;
        String l_expected;
        l_file = "C:/toto";
        l_expected = "C:\\toto";
        assertEquals(1 + " - " + l_file, l_expected, GB_FileTools.nameWithConvention(l_file));
        l_file = "C:\\toto";
        l_expected = "C:\\toto";
        assertEquals(1 + " - " + l_file, l_expected, GB_FileTools.nameWithConvention(l_file));
        l_file = "C:/toto/";
        l_expected = "C:\\toto";
        assertEquals(1 + " - " + l_file, l_expected, GB_FileTools.nameWithConvention(l_file));
        l_file = "C:/toto\\";
        l_expected = "C:\\toto";
        assertEquals(1 + " - " + l_file, l_expected, GB_FileTools.nameWithConvention(l_file));
        l_file = "C:/toto\\titi/";
        l_expected = "C:\\toto\\titi";
        assertEquals(1 + " - " + l_file, l_expected, GB_FileTools.nameWithConvention(l_file));
    }

    /**
     * Tests method <tt>pathRelative</tt>.
     */
    public void test_pathAbsolute() {
        String l_pathRoot;
        String l_pathSrc;
        String l_path;
        String l_expected;
        l_pathRoot = "C:/toto";
        l_pathSrc = "C:/toto/titi";
        l_path = "/tata";
        l_expected = "C:/toto/tata";
        test_pathAbsolute(1, l_pathRoot, l_pathSrc, l_path, l_expected);
        l_pathRoot = "C:/toto";
        l_pathSrc = "C:/toto/titi";
        l_path = "tata";
        l_expected = "C:/toto/titi/tata";
        test_pathAbsolute(2, l_pathRoot, l_pathSrc, l_path, l_expected);
        l_pathRoot = "C:/toto";
        l_pathSrc = "C:/toto/titi";
        l_path = "tata";
        l_expected = "C:/toto/titi/tata";
        test_pathAbsolute(4, l_pathRoot, l_pathSrc, l_path, l_expected);
        l_pathRoot = "C:/toto";
        l_pathSrc = "C:/toto/titi";
        l_path = "./tata";
        l_expected = "C:/toto/titi/tata";
        test_pathAbsolute(5, l_pathRoot, l_pathSrc, l_path, l_expected);
        l_pathRoot = "C:/toto";
        l_pathSrc = "C:/toto/titi";
        l_path = "../../tata";
        l_expected = "C:/tata";
        test_pathAbsolute(6, l_pathRoot, l_pathSrc, l_path, l_expected);
    }

    private void test_pathAbsolute(int a_index, String a_pathRoot, String a_pathSrc, String a_path, String a_expected) {
        String l_result;
        a_expected = GB_FileTools.nameWithConvention(a_expected);
        l_result = GB_FileTools.pathAbsolute(a_pathRoot, a_pathSrc, a_path);
        assertEquals(a_index + .1 + " - " + a_pathRoot + " - " + a_pathSrc + "- " + a_path, a_expected, l_result);
        a_pathRoot = a_pathRoot + "/";
        l_result = GB_FileTools.pathAbsolute(a_pathRoot, a_pathSrc, a_path);
        assertEquals(a_index + .2 + " - " + a_pathRoot + " - " + a_pathSrc + "- " + a_path, a_expected, l_result);
        a_pathSrc = a_pathSrc + "/";
        l_result = GB_FileTools.pathAbsolute(a_pathRoot, a_pathSrc, a_path);
        assertEquals(a_index + .3 + " - " + a_pathRoot + " - " + a_pathSrc + "- " + a_path, a_expected, l_result);
        a_pathRoot = STools.replace(a_pathRoot, "/", "\\");
        a_path = STools.replace(a_path, "/", "\\");
        l_result = GB_FileTools.pathAbsolute(a_pathRoot, a_pathSrc, a_path);
        assertEquals(a_index + .4 + " - " + a_pathRoot + " - " + a_pathSrc + "- " + a_path, a_expected, l_result);
    }

    /**
     * Tests method <tt>pathRelative</tt>.
     */
    public void test_pathRelative() {
        String l_pathRoot;
        String l_path;
        String l_pathRelative;
        l_pathRoot = "C:/toto";
        l_path = "C:/toto/titi";
        l_pathRelative = "titi";
        test_pathRelative(1, l_pathRoot, l_path, l_pathRelative);
        l_pathRoot = "C:/toto";
        l_path = "C:/toto";
        l_pathRelative = ".";
        test_pathRelative(2, l_pathRoot, l_path, l_pathRelative);
        l_pathRoot = "C:/toto";
        l_path = "C:/toto/";
        l_pathRelative = ".";
        test_pathRelative(3, l_pathRoot, l_path, l_pathRelative);
        l_pathRoot = "C:/toto";
        l_path = "C:/titi";
        l_pathRelative = "C:\\titi";
        test_pathRelative(4, l_pathRoot, l_path, l_pathRelative);
        l_pathRoot = "C:/toto";
        l_path = "C:/toto\\tata/titi";
        l_pathRelative = "tata\\titi";
        test_pathRelative(5, l_pathRoot, l_path, l_pathRelative);
    }

    private void test_pathRelative(int a_index, String a_pathRoot, String a_path, String a_pathRelative) {
        a_pathRelative = GB_FileTools.nameWithConvention(a_pathRelative);
        String l_result;
        l_result = GB_FileTools.pathRelative(a_pathRoot, a_path);
        assertEquals(a_index + .1 + " - " + a_pathRoot + " - " + a_path, a_pathRelative, l_result);
        a_pathRoot = a_pathRoot + "/";
        l_result = GB_FileTools.pathRelative(a_pathRoot, a_path);
        assertEquals(a_index + .2 + " - " + a_pathRoot + " - " + a_path, a_pathRelative, l_result);
        a_pathRoot = STools.replace(a_pathRoot, "/", "\\");
        a_path = STools.replace(a_path, "/", "\\");
        l_result = GB_FileTools.pathRelative(a_pathRoot, a_path);
        assertEquals(a_index + .3 + " - " + a_pathRoot + " - " + a_path, a_pathRelative, l_result);
    }

    /**
     * Tests method <tt>copy</tt>.
     */
    public void test_read_write() throws IOException {
        test_read_write(1, "");
        test_read_write(2, "test test test");
        test_read_write(3, "test with letters : ����");
        StringBuffer l_buffer = new StringBuffer();
        for (int i = 0; i < 1000; i++) {
            l_buffer.append("line " + i + "\n");
        }
        test_read_write(4, l_buffer.toString());
        l_buffer = new StringBuffer();
        for (int i = 0; i < 100000; i++) {
            l_buffer.append("line " + i + "\n");
        }
        test_read_write(5, l_buffer.toString());
    }

    public void test_read_write(int a_index, String a_content) throws IOException {
        test_read_write(a_index + ".NULL", a_content, null);
        test_read_write(a_index + ".DEF", a_content, ENCODING.DEFAULT);
        test_read_write(a_index + ".ISO", a_content, ENCODING.ISO_8859_1);
        test_read_write(a_index + ".UTF8", a_content, ENCODING.UTF8);
    }

    public void test_read_write(String a_index, String a_content, String a_encoding) throws IOException {
        File l_file = getTempFile(1, "test.txt");
        GB_FileTools.writeFile(l_file, a_content, false, a_encoding);
        assertTrue(a_index + ".2" + " - write OK", l_file.canRead());
        String l_newContent = GB_FileTools.readFile(l_file, a_encoding);
        assertEquals(a_index + ".3" + " - read OK", a_content, l_newContent);
        GB_FileTools.writeFile(l_file, a_content, true, a_encoding);
        l_newContent = GB_FileTools.readFile(l_file, a_encoding);
        assertEquals(a_index + ".4" + " - read OK (append)", a_content + a_content, l_newContent);
        l_file.delete();
    }

    public void test_read_writeDefault(String a_index, String a_content) throws IOException {
        File l_file = getTempFile(1, "test.txt");
        GB_FileTools.writeFile(l_file, a_content, false);
        assertTrue(a_index + ".2" + " - write OK", l_file.canRead());
        String l_newContent = GB_FileTools.readFile(l_file);
        assertEquals(a_index + ".3" + " - read OK", a_content, l_newContent);
        GB_FileTools.writeFile(l_file, a_content, true);
        l_newContent = GB_FileTools.readFile(l_file);
        assertEquals(a_index + ".4" + " - read OK (append)", a_content + a_content, l_newContent);
        l_file.delete();
    }

    public void test_read_writeUTF8() throws IOException {
        String l_name = "fileUTF8.txt";
        File l_file = FTools.getTempFile(l_name);
        InputStream l_inputStream = this.getClass().getResourceAsStream(l_name);
        if (l_inputStream == null) {
            throw new NullPointerException("Resource not found : " + l_name);
        }
        FTools.copyInputStreamToFile(l_inputStream, l_file);
        String l_txt = FTools.readFile(l_file, ENCODING.UTF8);
        File l_fileTemp = getTempFile(1, "testCopyUTF.txt");
        FTools.writeFile(l_fileTemp, l_txt, false, ENCODING.UTF8);
        String l_txt2 = FTools.readFile(l_fileTemp, ENCODING.UTF8);
        assertEquals(l_txt, l_txt2);
    }
}
