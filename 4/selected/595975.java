package eu.kostia.filetypedetect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Magic0 {

    private Position currentPosition;

    private List<Pattern> patterns;

    private FileChannel channel;

    private Queue<Pattern> tests;

    @SuppressWarnings("unchecked")
    public Magic0() throws IOException {
        ObjectInputStream objIn = new ObjectInputStream(getClass().getResourceAsStream("/eu/kostia/filetypedetect/magic.ser"));
        try {
            patterns = (ArrayList<Pattern>) objIn.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    public Result detect(File file) throws IOException {
        currentPosition = new Position();
        FileInputStream fstream = new FileInputStream(file);
        Result result = null;
        try {
            channel = fstream.getChannel();
            if (channel.size() == 0L) {
                result = new Result();
                result.setDescription("empty");
                result.setMime("application/x-empty");
                return result;
            }
            for (Pattern p : patterns) {
                result = process(result, p);
                if (result != null && !result.getDescription().isEmpty()) {
                    result.setDescription(result.getDescription().trim());
                    break;
                }
            }
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
        return result;
    }

    private Result process(Result result, Pattern p) throws IOException {
        if (p.getMessage() != null && p.getMessage().startsWith("OpenDocument")) {
            System.out.println(p);
        }
        if (p.getTest().startsWith("vnd.sun.xml.")) {
            System.out.println(p);
        }
        Object value = performTest(p.getOffset(), p.getType(), p.getTest());
        if (value != null) {
            if (result == null) {
                result = new Result();
                tests = new LinkedList<Pattern>();
            }
            if (result != null && result.getMime() == null && p.getMimetype() != null) {
                result.setMime(p.getMimetype());
            }
            String message = p.getMessage();
            if (message != null) {
                if (message.startsWith("\\b")) {
                    if (result.getDescription().length() > 0) {
                        result.setDescription(result.getDescription().substring(0, result.getDescription().length() - 1));
                    }
                    message = message.substring("\\b".length());
                }
                result.setDescription(result.getDescription() + format(message, value) + " ");
            }
            tests.addAll(p.getChildren());
        }
        if (tests != null) {
            Pattern p0 = tests.poll();
            if (p0 != null) {
                process(result, p0);
            }
        }
        return result;
    }

    private Object performTest(String offsetStr, Type type, String test) throws IOException {
        int offset = new MagicUtil(channel, currentPosition).toInt(offsetStr);
        if (offset < 0) {
            return null;
        }
        if (type.isString()) {
            return new StringTest(offset, type, test, currentPosition, channel).performTest();
        } else if (type.isSearch()) {
            return new SearchTest(offset, type, test, currentPosition, channel).performTest();
        } else if (type.isRegex()) {
            return new RegexTest(offset, type, test, currentPosition, channel).performTest();
        } else {
            return new ByteTest(offset, type, test, currentPosition, channel).performTest();
        }
    }

    private String format(String format, Object... args) {
        return Printf.format(format, args);
    }
}
