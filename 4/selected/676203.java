package com.google.code.jouka.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility for FileChUtil.
 *
 * @author Str.Isym
 *
 */
public class FileUtil {

    private static final String LINE_SEP = File.separator;

    /**
     * <pre>
     * ファイルを削除する。
     *
     * ※注意：
     * 　対象ファイルを、
     * 　　a) FileChUtil.USE_MAPMODE_READ_ONLY
     * 　　b) FileChUtil.USE_MAPMODE_READ_WRITE
     * 　で読み込んでいた場合は、ファイル読み込み用のFileChannelでMappedByteBufferが使われる。
     * 　
     * 　Windows環境ではガーベージコレクションが行われるまではMappedByteBufferが閉じられないので
     * 　MappedByteBuffer経由でファイルを読み書きする場合は、システム稼働中は削除する必要のない
     * 　ファイルを対象とするか、どうしても削除したい場合はバッファにNULLをセットした後に
     * 　System.gc()を先に実行するFileUtil.deleteAfterGC()を使うこと。
     *
     * 　参考情報：http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
     *
     * </pre>
     *
     * @author Str.Isym
     * @param file
     * @throws IOException
     */
    public static boolean delete(File file) throws IOException {
        boolean rtn = false;
        try {
            rtn = file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (file.exists()) {
                throw new IOException(LINE_SEP + "    A file '" + file.getPath() + "' can not deleted." + LINE_SEP + "    If you use :" + LINE_SEP + "       'a) FileChUtil.USE_MAPMODE_READ_ONLY'" + LINE_SEP + "       'b) FileChUtil.USE_MAPMODE_READ_WRITE'" + LINE_SEP + "    for read/write, then you have to use :" + LINE_SEP + "        1) set NULL to buffer" + LINE_SEP + "        2) use 'FileChUtil.deleteAfterGC()' for file deleting." + LINE_SEP + "    OR use :" + LINE_SEP + "        FileChUtil.USE_DIRECT_ALLOCATED_BUFFER (USE_ALLOCATED_BUFFER)" + LINE_SEP + "    for read/write." + LINE_SEP + "    and check target channel closed." + LINE_SEP + LINE_SEP + "    If your development/deployment platform is Win32," + LINE_SEP + "    see also bug information:" + LINE_SEP + "    http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154" + LINE_SEP + "-----------------------------------------------------------------------");
            }
        }
        return rtn;
    }

    public static boolean delete(String fileName) throws IOException {
        return delete(new File(fileName));
    }

    /**
     * <pre>
     * System.gc()実行後に、ファイルを削除する。
     * 非常時以外は、本メソッドの利用は非推奨。
     *
     * 対象ファイルを、
     *     FileChUtil.readFileToString(fileChannel, FileChUtil.USE_MAPPED_BYTE_BUFFER)
     * で読み込んでいた場合は、ファイル読み込み用のFileChannelでMappedByteBufferが使われる。
     *
     * Windows環境ではガーベージコレクションが行われるまではMappedByteBufferが閉じられないので
     * MappedByteBuffer経由でファイルを読み書きする場合は極力、システム稼働中は削除する必要のない
     * ファイルを対象とするべき。
     * どうしても削除したい場合のみ、このメソッドを使う。
     *
     * 参考情報：http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
     *
     * </pre>
     *
     * @param file
     * @param waitTime
     * @throws IOException
     */
    public static boolean deleteAfterGC(File file) throws IOException {
        System.gc();
        boolean rtn = false;
        try {
            rtn = delete(file);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (file.exists()) {
                throw new IOException(LINE_SEP + "    FATAL ERROR :" + LINE_SEP + "    A file '" + file.getPath() + "' can not deleted after 'System.gc()'." + LINE_SEP);
            }
        }
        return rtn;
    }

    /**
     * <pre>
     * 対象Fileが存在するかどうかを確認する。
     * 対象がファイル・ディレクトリかは問わない。
     * 対象が存在しない場合に、FileNotFoundExceptionをthrowするかどうかを指定できる。
     * （java.io.File#exists()は、ファイルが存在しない場合に例外を投げてくれない）
     *
     * </pre>
     * @param file 対象ファイル
     * @param enableException 結果がfalseになる場合に、例外を発生させたいならtrueに設定。
     * @return fileが存在するならtrue、存在せずenableException=falseならfalseを返し、enableException=trueならFileNotFoundExceptionをthrowする。
     * @throws FileNotFoundException
     */
    public static boolean exists(File file, boolean enableException) throws FileNotFoundException {
        if (file == null) {
            if (enableException) {
                throw new NullPointerException("file is null.");
            }
            return false;
        }
        if (!file.exists()) {
            if (enableException) {
                throw new FileNotFoundException("File \"" + file + "\" does not exist.");
            }
            return false;
        }
        return true;
    }

    /**
     * <pre>
     * 対象Fileが存在するかどうかを確認する。
     * 対象がファイル・ディレクトリかは問わない。
     * 対象が存在しなければ、FileNotFoundExceptionをthrowする。
     * （java.io.File#exists()はファイルが存在しない場合に例外を投げてくれない）
     *
     * </pre>
     * @param file 対象ファイル
     * @return fileが存在するならtrue、存在しなければFileNotFoundExceptionをthrowする。
     * @throws FileNotFoundException
     */
    public static boolean exists(File file) throws FileNotFoundException {
        return exists(file, true);
    }

    /**
     * <PRE>
     * 与えられたFileオブジェクトから、BufferedReaderを取得して返す。
     * 返されたBufferedReaderは、使用後に必ずcloseすること。
     *
     * Example:
     *     File file = new File("C:\\usr\\testdata\\test.txt");
     *     BufferedReader reader = FileUtil.getBufferedFileReader(file, "SJIS");
     *     String line = null;
     *     while((line = FileUtil.readLine(reader)) != null){
     *         System.out.println(line);
     *     }
     *     reader.close();
     *
     * </PRE>
     *
     * @param file 対象ファイル
     * @param charset 対象ファイルを読み出す際に指定するcharset
     * @return 対象ファイルから取得したBufferedReader
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static BufferedReader getBufferedFileReader(File file, String charset) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
    }

    /**
     * <PRE>
     * 与えられたファイルパス文字列から、BufferedReaderを取得して返す。
     * 返されたBufferedReaderは、使用後に必ずcloseすること。
     *
     * Example:
     *     String filePath = "C:\\usr\\testdata\\test.txt";
     *     BufferedReader reader = FileUtil.getBufferedFileReader(filePath, "SJIS");
     *     String line = null;
     *     while((line = FileUtil.readLine(reader)) != null){
     *         System.out.println(line);
     *     }
     *     reader.close();
     *
     * </PRE>
     *
     * @param filePath 対象ファイルパス
     * @param charset 対象ファイルを読み出す際に指定するcharset
     * @return 対象ファイルパスから取得したBufferedReader
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static BufferedReader getBufferedFileReader(String filePath, String charset) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        return getBufferedFileReader(new File(filePath), charset);
    }

    /**
     * <PRE>
     * 与えられたFileオブジェクトから、BufferedWriterを取得して返す。
     * 返されたBufferedWriterは、使用後に必ずcloseすること。
     *
     * </PRE>
     *
     * @param file 対象ファイル
     * @param charset 対象ファイルに書き出す際に指定するcharset
     * @return 対象ファイルから取得したBufferedWriter
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static BufferedWriter getBufferedFileWriter(File file, String charset) throws UnsupportedEncodingException, IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
    }

    /**
     * <PRE>
     * 与えられたファイルパスから、BufferedWriterを取得して返す。
     * 返されたBufferedWriterは、使用後に必ずcloseすること。
     *
     * </PRE>
     *
     * @param filePath 対象ファイルパス
     * @param charset 対象ファイルに書き出す際に指定するcharset
     * @return 対象ファイルから取得したBufferedWriter
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static BufferedWriter getBufferedFileWriter(String filePath, String charset) throws UnsupportedEncodingException, IOException {
        return getBufferedFileWriter(new File(filePath), charset);
    }

    /**
     * <pre>
     * 指定されたパスの親ディレクトリ名を返す。
     *
     * Example:
     *  - Windows
     *   FileUtil.getParentPathOf("C:/windows/temp") = C:\windows
     *   FileUtil.getParentPathOf("C:/windows")      = C:
     *   FileUtil.getParentPathOf("C:/")             = null
     *   FileUtil.getParentPathOf("/var")            = C:
     *   FileUtil.getParentPathOf("/var/tmp")        = C:\var
     *   FileUtil.getParentPathOf("/var/tmp/")       = C:\var
     *   FileUtil.getParentPathOf("/")               = null
     *
     * </pre>
     * @param path
     *
     * @return 指定されたパスの親ディレクトリ名文字列
     */
    public static String getParentPathOf(String path) {
        String rtn = new File(path).getAbsoluteFile().getParent();
        return rtn;
    }

    /**
     * <pre>
     * 実行ディレクトリのカレントパスを基準形式で返す。
     * パスの最後にはファイルセパレータが付加される。
     *
     * Example:
     *  - Current-path that program running is "C:\MyProject" :
     *   FileUtil.getCurrentPath = "C:\MyProject\"
     * </pre>
     *
     *
     * @return 実行ディレクトリのカレントパス文字列
     */
    public static String getCurrentPath() {
        String rtn = null;
        try {
            rtn = new File(".").getAbsoluteFile().getCanonicalPath();
            if (rtn != null && !rtn.endsWith(File.separator)) {
                rtn = rtn + File.separator;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
     * <pre>
     * システムのテンポラリディレクトリのパスを返す。
     * パスの最後にはファイルセパレータが付加される。
     *
     * Example:
     *   // WIN32:
     *   FileUtil.getSystemTempDir() = "D:\USER~1\LOCALS~1\Temp\"
     *
     * </pre>
     * @return System.getProperty("java.io.tmpdir")
     */
    public static String getSystemTempDir() {
        String rtn = System.getProperty("java.io.tmpdir");
        if (!rtn.endsWith(File.separator)) {
            rtn += File.separator;
        }
        return rtn;
    }

    /**
     * <PRE>
     * テキストファイルから取得したBufferedReaderから、１行分を読み込む。
     * {@link #getBufferedFileReader(File, String)} 又は {@link #getBufferedFileReader(String, String)}
     * を実行して取得したBufferedReaderを使うと便利である。
     *
     * 読み込み対象のテキストファイルの終端に達した場合、引数として与えられたBufferedReaderは
     * このメソッド内部でcloseされた後にnullがセットされる。
     * closeされているBufferedReaderから読み出そうとした場合、戻り値はnullとなる。
     *
     * Example:
     *     String filePath = "C:\\usr\\testdata\\test.txt";
     *     BufferedReader reader = FileUtil.getBufferedFileReader(filePath, "JISAutoDetect");
     *     String line = null;
     *     while((line = FileUtil.readLine(reader)) != null){
     *         System.out.println(line);
     *     }
     *     reader.close();
     *
     *
     * </PRE>
     *
     * @param reader ファイルから取得したBufferedReader
     * @return BufferedReaderから１行読み込んだ結果。BufferedReaderがnull又はcloseされていた場合はnullを返す。
     * @throws IOException
     */
    public static String readLine(BufferedReader reader) throws IOException {
        try {
            reader.ready();
        } catch (IOException e) {
            reader.close();
            reader = null;
            return null;
        }
        return reader.readLine();
    }

    /**
     * <PRE>
     * 対象テキストファイルから、テキストデータを指定したcharsetで1行づつ読み込み、
     * Listに格納して返す。
     *
     * Example:
     *     java.util.List<String> lines = new ArrayList<String>();
     *     FileUtil.readLines(new File("c:\\aaa.txt"), "JISAutoDetect", lines);
     *     for (String line : lines) {
     * 	       System.out.println(line);
     *     }
     *
     *
     * </PRE>
     *
     * @param file 対象となるファイル
     * @param charset テキストデータのエンコーディング(SJIS/EUC_JP/UTF8/JISAutoDetectなど)
     * @param lines 結果を格納するリスト
     * @return
     * @throws IOException
     */
    public static void readLines(File file, String charset, List<String> lines) throws IOException {
        BufferedReader reader = getBufferedFileReader(file, charset);
        String line = null;
        while ((line = readLine(reader)) != null) {
            lines.add(line);
        }
        reader.close();
        reader = null;
    }

    /**
     * <PRE>
     * 対象テキストファイルから、テキストデータを指定したcharsetで1行づつ読み込み、
     * Listに格納して返す。
     *
     * Example:
     *     String filePath = "C:\\usr\\testdata\\test.txt";
     *     java.util.List<String> lines = new ArrayList<String>();
     *     FileUtil.readLines(filePath, "JISAutoDetect", lines);
     *     for (String line : lines) {
     *         System.out.println(line);
     *     }
     *
     *
     * </PRE>
     *
     * @param filePath 対象となるファイルのパス文字列
     * @param charset テキスト・エンコーディング(SJIS/EUC_JP/UTF8/JISAutoDetectなど)
     * @param lines 結果を格納するリスト
     * @return
     * @throws IOException
     */
    public static void readLines(String filePath, String charset, List<String> list) throws IOException {
        readLines(new File(filePath), charset, list);
    }

    public static void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
    }

    /**
     * <PRE>
     * String配列を任意の文字エンコーディングでファイルに出力
     *
     * </PRE>
     *
     * @param file 出力先ファイル
     * @param lines 出力したいString配列
     * @param crlf 改行コード：nullなら出力しない
     * @param charset 文字エンコード
     * @param lineNumFormat 行番号を出力したいならString#format(param)のparamとして設定。nullなら未出力
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static void writeLines(File file, List<String> lines, String crlf, String charset, String lineNumFormat) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        BufferedWriter writer = getBufferedFileWriter(file, charset);
        int lineNum = 0;
        while (lineNum < lines.size()) {
            if (lineNumFormat != null) {
                writer.write(String.format(lineNumFormat, lineNum + 1));
            }
            writer.write(lines.get(lineNum++));
            if (crlf != null) {
                writer.write(crlf);
            }
        }
        writer.flush();
        writer.close();
    }

    public static void writeLines(String filePath, List<String> lines, String crlf, String charset, String lineNumFormat) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        writeLines(new File(filePath), lines, crlf, charset, lineNumFormat);
    }
}
