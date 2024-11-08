package vavix.util.grep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * �h����
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050215 nsano initial version <br>
 */
public class Grep {

    /** */
    private static Charset charset = Charset.forName(System.getProperty("file.encoding"));

    /** */
    private static CharsetDecoder decoder = charset.newDecoder();

    /** Pattern used to parse lines */
    private static final Pattern linePattern = Pattern.compile(".*\r?\n");

    /** �h���p�^�[�� */
    private Pattern pattern;

    /** �h���Ώ� */
    private File file;

    /** �h����쐬 */
    public Grep(File file, Pattern pattern) {
        this.file = file;
        this.pattern = pattern;
    }

    /**
     * grep �ň�����������s��\���N���X�ł��B
     * <p>
     * �h��������
     * </p>
     */
    public static class ResultSet {

        ResultSet(File file, int lineNumber, String line) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.line = line;
        }

        /** grep �ň�����������t�@�C�� */
        File file;

        /** grep �ň�����������s�ԍ� */
        int lineNumber;

        /** grep �ň�����������s */
        String line;
    }

    /** �h�����̂𗭂߂� */
    private List<ResultSet> results = new ArrayList<ResultSet>();

    /** �ЂƂ��h���܂� */
    private void grep(CharBuffer cb) {
        Matcher lm = linePattern.matcher(cb);
        Matcher pm = null;
        int lines = 0;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();
            if (pm == null) {
                pm = pattern.matcher(cs);
            } else {
                pm.reset(cs);
            }
            if (pm.find()) {
                results.add(new ResultSet(file, lines, cs.toString()));
            }
            if (lm.end() == cb.limit()) {
                break;
            }
        }
    }

    /** �h���܂� */
    public List<ResultSet> exec() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        int size = (int) fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, size);
        CharBuffer cb = decoder.decode(bb);
        grep(cb);
        return results;
    }

    /**
     * @param args 0: top directory, 1: grep pattern, 2: file pattern
     */
    public static void main(final String[] args) throws Exception {
        new RegexFileDigger(new FileDigger.FileDredger() {

            public void dredge(File file) throws IOException {
                for (Grep.ResultSet rs : new Grep(file, Pattern.compile(args[1])).exec()) {
                    System.out.print(rs.file + ":" + rs.lineNumber + ":" + rs.line);
                }
            }
        }, Pattern.compile(args[2])).dig(new File(args[0]));
    }
}
