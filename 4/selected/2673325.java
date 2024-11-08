package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.moonshot.chatty.ChattyInternalError;
import org.moonshot.chatty.ChattyParseException;
import org.moonshot.chatty.parse.ChattyParser;
import org.moonshot.chatty.parse.ChattyWriter;

public class ChattyTest {

    private static ChattyParser read;

    private static ChattyWriter write;

    /**
     * @param args
     */
    public static void main(String[] args) {
        read = new ChattyParser();
        write = new ChattyWriter(System.out);
        for (String aPath : args) {
            final File scanDir = new File(aPath);
            if (scanDir.isDirectory()) {
                for (File aFile : scanDir.listFiles()) demoFile(aFile);
            } else demoFile(scanDir);
        }
    }

    public static void demoFile(final File aFile) {
        if (!aFile.isFile()) return;
        System.out.println();
        System.out.println("-- file " + aFile.getPath());
        System.out.flush();
        final StringBuilder document = new StringBuilder();
        try {
            final BufferedReader instream = new BufferedReader(new FileReader(aFile));
            final char[] buffer = new char[1024];
            int nRead = -1;
            while (-1 != (nRead = instream.read(buffer))) document.append(buffer, 0, nRead);
            write.print(read.parse(document));
        } catch (ChattyParseException e) {
            System.out.println(showErrorLine(document, e));
            System.out.println(e.getProblem() + (e.getLexeme() != null ? " (at " + e.getLexeme().toString() + ")" : ""));
            e.printStackTrace(System.out);
        } catch (ChattyInternalError e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.flush();
        System.err.flush();
    }

    public static String showErrorLine(CharSequence aDocument, ChattyParseException e) {
        int B = e.getStart() - 1;
        int L = e.getEnd() - 1;
        int ls, le;
        for (ls = B; ls >= 0 && aDocument.charAt(ls) != '\n'; ls--) ;
        for (le = L; le < aDocument.length() && aDocument.charAt(le) != '\n'; le++) ;
        if (ls < 0) ls = 0;
        StringBuilder theOut = new StringBuilder(le - ls + 2);
        theOut.append(aDocument.subSequence(ls, le));
        theOut.append('\n');
        for (int i = ls + 1; i < le; i++) theOut.append(aDocument.charAt(i) == '\t' ? '\t' : (i < B || i >= L ? ' ' : '^'));
        return theOut.toString();
    }
}
