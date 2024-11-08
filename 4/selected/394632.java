package de.buelowssiege.mail.berkeleystore.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a file in the berkeley format.
 * 
 * @author Maximilian Schwerin
 * @created November 12, 2002
 */
public class StandardMailFileParserImplementation implements BerkeleyMailFileParser {

    private BerkeleyEventHandler eventHandler;

    private Pattern contentStartPattern = Pattern.compile("\r?\n\r?\n");

    private Pattern messageStartPattern = Pattern.compile("(?m)^From .*\r?\n");

    public StandardMailFileParserImplementation() {
    }

    public void setEventHandler(BerkeleyEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void parse(File file) throws BerkeleyMailFileParserException, BerkeleyEventHandlerException {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            FileChannel fileChannel = fileInputStream.getChannel();
            int fileSize = (int) fileChannel.size();
            if (fileSize > 0) {
                MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
                CharSequence charBuffer = new BerkeleyCharSequence(mappedByteBuffer);
                Matcher messageStartMatcher = messageStartPattern.matcher(charBuffer);
                Matcher contentStartMatcher = contentStartPattern.matcher(charBuffer);
                messageStartMatcher.find();
                if (messageStartMatcher.start() != 0) {
                    throw new BerkeleyMailFileParserException("The parsed file is corrupt!");
                }
                eventHandler.messageStart(messageStartMatcher.start());
                eventHandler.headerStart(messageStartMatcher.end());
                contentStartMatcher.find(messageStartMatcher.end());
                eventHandler.contentStart(contentStartMatcher.end());
                while (messageStartMatcher.find()) {
                    eventHandler.messageEnd(messageStartMatcher.start());
                    eventHandler.messageStart(messageStartMatcher.start());
                    eventHandler.headerStart(messageStartMatcher.end());
                    contentStartMatcher.find(messageStartMatcher.end());
                    eventHandler.contentStart(contentStartMatcher.end());
                }
                eventHandler.messageEnd(fileSize);
            }
            fileChannel.close();
        } catch (IOException IOEx) {
            throw new BerkeleyMailFileParserException(IOEx);
        } catch (SecurityException SecEx) {
            throw new BerkeleyMailFileParserException(SecEx);
        }
    }

    public static void main(String[] argv) throws Exception {
        StandardMailFileParserImplementation bfp = new StandardMailFileParserImplementation();
        bfp.setEventHandler(new StandardEventHandlerImplementation());
        bfp.parse(new File(argv[0]));
    }
}
