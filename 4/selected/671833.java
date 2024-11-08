package stasis;

import java.io.*;

public class Parser {

    public interface ParserHandler {

        public String handleToken(String token) throws StasisException;
    }

    private Parser() {
    }

    public static String parse(String input, ParserHandler handler) throws StasisException {
        StringReader reader = new StringReader(input);
        StringWriter writer = new StringWriter();
        try {
            parse(reader, writer, handler);
        } catch (IOException e) {
            throw new StasisException("IO error while parsing", e);
        }
        return writer.toString();
    }

    public static void parse(Reader reader, Writer writer, ParserHandler handler) throws IOException, StasisException {
        StringBuffer sb = new StringBuffer();
        boolean token = false;
        for (int i = 0; ; i++) {
            int c = reader.read();
            if (c == -1) break;
            if (c == '{') {
                token = true;
                sb.setLength(0);
            } else if (token) {
                if (c == '}') {
                    token = false;
                    writer.write(handler.handleToken(sb.toString()));
                } else if (c == '{') {
                    throw new StasisException("Nested tokens not allowed (at offset " + i + ")");
                } else {
                    sb.append((char) c);
                }
            } else {
                writer.write((char) c);
            }
        }
        writer.flush();
        if (token) throw new StasisException("Last token was not closed");
    }
}
