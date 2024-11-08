package com.google.code.jouka.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class FileUtilTest {

    private static String _testDataDir = "testdata";

    private static String _testDataRootPath = null;

    private static String _testDataDir_POSTALCODE = null;

    private static String _SJIS_CRLF = "post_offices_SJIS_CRLF.csv";

    private static String _JIS_CR = "post_offices_JIS_CR.csv";

    private static String _EUCJP_LF = "post_offices_EUCJP_LF.csv";

    static {
        try {
            _testDataRootPath = new File(_testDataDir).getCanonicalPath() + File.separator;
            _testDataDir_POSTALCODE = _testDataRootPath + "POSTALCODE.ja" + File.separator;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getBufferedFileReader_FILE_STRING() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _SJIS_CRLF;
        String charset = "SJIS";
        BufferedReader reader = FileUtil.getBufferedFileReader(new File(filePath), charset);
        assertEquals(true, reader instanceof BufferedReader);
        reader.close();
    }

    @Test
    public void getBufferedFileReader_STRING_STRING() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _SJIS_CRLF;
        String charset = "SJIS";
        BufferedReader reader = FileUtil.getBufferedFileReader(filePath, charset);
        assertEquals(true, reader instanceof BufferedReader);
        reader.close();
    }

    @Test
    public void readLine_SJIS_CRLF() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _SJIS_CRLF;
        String charset = "SJIS";
        readLine_Core(filePath, charset);
    }

    @Test
    public void readLine_JIS_CR() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _JIS_CR;
        String charset = "JIS";
        readLine_Core(filePath, charset);
    }

    @Test
    public void readLine_EUCJP_LF() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _EUCJP_LF;
        String charset = "EUC_JP";
        readLine_Core(filePath, charset);
    }

    /**
     * <PRE>
     * ＜テストシナリオ＞
     * １つのテストデータに対して、２つのBufferedReaderを使って読み出し＆整合性チェック。
     * １つめは、File->FileInputStream->InputStreamReader->BufferedReaderで取得したもの。
     * ２つめは、FileUtil#getBufferedFileReader()で取得したもの。
     * それぞれ同じデータを読み出して、マッチしているかをテストする。
     *
     * </PRE>
     *
     * @param filePath
     * @param charset
     * @throws IOException
     */
    private void readLine_Core(String filePath, String charset) throws IOException {
        BufferedReader rdSrc = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), charset));
        BufferedReader rdTest = FileUtil.getBufferedFileReader(filePath, charset);
        while (true) {
            if (rdSrc.ready() && rdTest.ready()) {
                String lineSrc = new String(rdSrc.readLine());
                String lineTest = new String(FileUtil.readLine(rdTest));
                assertEquals(lineSrc, lineTest);
            } else {
                break;
            }
        }
        rdTest.close();
        rdTest = null;
        rdSrc.close();
        rdSrc = null;
    }

    @Test
    public void writeLine_SJIS_CRLF() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _SJIS_CRLF;
        String charsetOrg = "SJIS";
        String charsetTmpTest = "SJIS";
        String crlf = "\r\n";
        writeLine_Core(filePath, charsetOrg, charsetTmpTest, crlf);
    }

    @Test
    public void writeLine_JIS_CR() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _JIS_CR;
        String charsetOrg = "JIS";
        String charsetTmpTest = "JIS";
        String crlf = "\r";
        writeLine_Core(filePath, charsetOrg, charsetTmpTest, crlf);
    }

    @Test
    public void writeLine_EUCJP_LF() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _EUCJP_LF;
        String charsetOrg = "EUC_JP";
        String charsetTmpTest = "EUC_JP";
        String crlf = "\n";
        writeLine_Core(filePath, charsetOrg, charsetTmpTest, crlf);
    }

    /**
     * <PRE>
     * ＜テストシナリオ＞
     * 1) テストデータを、FileUtil#writeLine()でテンポラリファイルとしてコピー。
     * 　　その際、テストデータの入力用charsetと、テンポラリファイルの出力用charsetは
     * 　　同じものを使う。
     * 2) テストデータをBufferedReader#readLineで読み込み、
     * 　　テンポラリファイルをFileUtil#readLineで読み込んで、合致しているかテスト。
     * 　　その際、テストデータの入力用charsetと、テンポラリファイルの出力用charsetは
     * 　　別のものを使う。
     * 3) テスト後は、テンポラリファイルは削除する。
     *
     *
     * </PRE>
     *
     * @param filePath
     * @param charsetOrg
     * @param charsetTmpTest
     * @param crlf
     * @throws IOException
     */
    private void writeLine_Core(String filePath, String charsetOrg, String charsetTmpTest, String crlf) throws IOException {
        String tmpTestFile = System.getProperty("java.io.tmpdir") + File.separator + "FileUtilTest_TempFile_writeLine_Core_" + StrUtil.getRandomHexString(16, true) + ".txt";
        BufferedReader rdSrc = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), charsetOrg));
        BufferedWriter wrTest = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(tmpTestFile)), charsetOrg));
        while (rdSrc.ready()) {
            String lineSrc = rdSrc.readLine();
            FileUtil.writeLine(wrTest, lineSrc + crlf);
        }
        rdSrc.close();
        rdSrc = null;
        wrTest.flush();
        wrTest.close();
        wrTest = null;
        rdSrc = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), charsetTmpTest));
        File fTmpFile = new File(tmpTestFile);
        BufferedReader rdTest = FileUtil.getBufferedFileReader(fTmpFile, charsetOrg);
        while (true) {
            if (rdSrc.ready() && rdTest.ready()) {
                String lineSrc = new String(rdSrc.readLine());
                String lineTest = new String(FileUtil.readLine(rdTest));
                assertEquals(lineSrc, lineTest);
            } else {
                break;
            }
        }
        rdTest.close();
        rdTest = null;
        rdSrc.close();
        rdSrc = null;
        fTmpFile.delete();
        if (fTmpFile.exists()) {
            fail("temp file can not deleted : " + tmpTestFile);
        }
    }

    @Test
    public void readLines_SJIS_CRLF() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _SJIS_CRLF;
        String charsetTestData = "SJIS";
        String charsetCheck = "SJIS";
        readLines_Core(filePath, charsetTestData, charsetCheck);
    }

    @Test
    public void readLines_JIS_CR() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _JIS_CR;
        String charsetTestData = "JIS";
        String charsetCheck = "JIS";
        readLines_Core(filePath, charsetTestData, charsetCheck);
    }

    @Test
    public void readLines_EUCJP_LF() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _EUCJP_LF;
        String charsetTestData = "EUC_JP";
        String charsetCheck = "EUC_JP";
        readLines_Core(filePath, charsetTestData, charsetCheck);
    }

    private void readLines_Core(String filePath, String charsetTestData, String charsetCheck) throws IOException {
        java.util.List<String> checkLines = new ArrayList<String>();
        FileUtil.readLines(filePath, charsetCheck, checkLines);
        BufferedReader rdSrc = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), charsetTestData));
        int lineIndex = 0;
        while (rdSrc.ready()) {
            String lineSrc = new String(rdSrc.readLine());
            String lineChk = checkLines.get(lineIndex++);
            assertEquals(lineSrc, lineChk);
        }
        rdSrc.close();
        rdSrc = null;
    }

    @Test
    public void writeLines_SJIS_CRLF() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _SJIS_CRLF;
        String charsetOrg = "SJIS";
        String charsetTmpTest = "SJIS";
        String crlf = "\r\n";
        writeLines_Core(filePath, charsetOrg, charsetTmpTest, crlf);
    }

    @Test
    public void writeLines_JIS_CR() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _JIS_CR;
        String charsetOrg = "JIS";
        String charsetTmpTest = "JIS";
        String crlf = "\r";
        writeLines_Core(filePath, charsetOrg, charsetTmpTest, crlf);
    }

    @Test
    public void writeLines_EUCJP_LF() throws IOException {
        String filePath = _testDataDir_POSTALCODE + _EUCJP_LF;
        String charsetOrg = "EUC_JP";
        String charsetTmpTest = "EUC_JP";
        String crlf = "\n";
        writeLines_Core(filePath, charsetOrg, charsetTmpTest, crlf);
    }

    /**
     * <PRE>
     * ＜テストシナリオ＞
     * 1) テストデータを、FileUtil#writeLine()でテンポラリファイルとしてコピー。
     * 　　その際、テストデータの入力用charsetと、テンポラリファイルの出力用charsetは
     * 　　同じものを使う。
     * 2) テストデータをBufferedReader#readLineで読み込み、
     * 　　テンポラリファイルをFileUtil#readLineで読み込んで、合致しているかテスト。
     * 　　その際、テストデータの入力用charsetと、テンポラリファイルの出力用charsetは
     * 　　別のものを使う。
     * 3) テスト後は、テンポラリファイルは削除する。
     *
     * </PRE>
     *
     * @param filePath
     * @param charsetOrg
     * @param charsetTmpTest
     * @param crlf
     * @throws IOException
     */
    private void writeLines_Core(String filePath, String charsetOrg, String charsetTmpTest, String crlf) throws IOException {
        BufferedReader rdSrc = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), charsetOrg));
        List<String> readLines = new ArrayList<String>();
        while (rdSrc.ready()) {
            String lineSrc = rdSrc.readLine();
            readLines.add(lineSrc);
        }
        rdSrc.close();
        rdSrc = null;
        String tmpTestFile = System.getProperty("java.io.tmpdir") + File.separator + "FileUtilTest_TempFile_writeLines_Core_" + StrUtil.getRandomHexString(16, true) + ".txt";
        FileUtil.writeLines(new File(tmpTestFile), readLines, crlf, charsetOrg, null);
        java.util.List<String> checkLines = new ArrayList();
        FileUtil.readLines(filePath, charsetTmpTest, checkLines);
        File fTmpFile = new File(tmpTestFile);
        BufferedReader rdTest = new BufferedReader(new InputStreamReader(new FileInputStream(fTmpFile), charsetOrg));
        int lineIndex = 0;
        while (rdTest.ready()) {
            String lineSrc = new String(rdTest.readLine());
            String lineChk = checkLines.get(lineIndex++);
            assertEquals(lineSrc, lineChk);
        }
        rdTest.close();
        rdTest = null;
        fTmpFile.delete();
        if (fTmpFile.exists()) {
            fail("temp file can not deleted : " + tmpTestFile);
        }
    }
}
