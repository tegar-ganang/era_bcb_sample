package gawky.service.mt940;

import gawky.message.parser.PatternParser;
import gawky.message.part.Part;
import java.io.File;
import java.io.FileInputStream;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mt940Reader {

    private static Charset charset = Charset.forName("UTF-8");

    private static CharsetDecoder decoder = charset.newDecoder();

    private static Pattern linePattern = Pattern.compile("(?s)((.*?)\r?\n)((:[0-9]{0,2}.?:)|(-)|(\\s))");

    static PatternParser parser = new PatternParser();

    String currenttag = null;

    private void handler(String line, Part part) throws Exception {
        currenttag = line.substring(0, 4);
        part.parse(parser, line);
        handler.process(line, part);
    }

    private void processLine(String line) throws Exception {
        if (line.startsWith(":20:")) handler(line, new Satz20()); else if (line.startsWith(":21:")) handler(line, new Satz21()); else if (line.startsWith(":25:")) handler(line, new Satz25()); else if (line.startsWith(":28:")) handler(line, new Satz28()); else if (line.startsWith(":28C:")) handler(line, new Satz28()); else if (line.startsWith(":60")) handler(line, new Satz60()); else if (line.startsWith(":62")) handler(line, new Satz62()); else if (line.startsWith(":64")) handler(line, new Satz64()); else if (line.startsWith(":65")) handler(line, new Satz65()); else if (line.startsWith(":61:")) handler(line, new Satz61()); else if (line.startsWith(":86")) handler(line, new Satz86()); else if (line.equals("-")) System.out.println("--------------------------------------------------------<br>"); else if (currenttag != null && !line.startsWith(":")) {
            line = currenttag + line;
            processLine(line);
        }
    }

    public final void matchLines(CharBuffer cb) throws Exception {
        Matcher lm = linePattern.matcher(cb);
        int pos = 0;
        while (lm.find(pos)) {
            String cs = lm.group(2);
            processLine(cs);
            pos = lm.start(3);
            if (lm.end() == cb.limit()) break;
        }
    }

    MTListener handler;

    public void registerHandler(MTListener handler) {
        this.handler = handler;
    }

    public void read(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        int sz = (int) fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
        CharBuffer cb = decoder.decode(bb);
        matchLines(cb);
        fc.close();
    }

    public static void main(String[] args) throws Exception {
        Mt940Reader reader = new Mt940Reader();
        reader.registerHandler(new Handler());
        File f = new File("C:/mt940.txt");
        reader.read(f);
    }
}
