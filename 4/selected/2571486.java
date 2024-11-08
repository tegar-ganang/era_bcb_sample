package org.iceinn.iceparser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.iceinn.iceparser.data.IceData;
import org.iceinn.iceparser.handler.IceTagHandler;
import org.iceinn.iceparser.handler.SyserrHandler;
import org.iceinn.tools.Logger;

/**
 * 
 * @author Lionel FLAHAUT
 *
 */
public class Main {

    private static final int FILE = 0;

    private static final int STRING = 1;

    /** Creates a new instance of Main */
    public Main() {
    }

    /**
	 * @param args
	 *            the command line arguments
	 */
    public static void main(String[] args) throws IOException {
        SyserrHandler handler = new SyserrHandler("hahoyo");
        IceParser iceParser = getParser(handler);
        ByteBuffer buffer = getBuffer(STRING, null);
        int maxL = 1;
        Logger.setLogLevel(Logger.DEBUG_LEVEL);
        long mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();
        byte[] data = new byte[maxL];
        int totalLength = 0;
        while (buffer.position() < buffer.limit()) {
            int length = buffer.remaining() > maxL ? maxL : buffer.remaining();
            totalLength += length;
            buffer.get(data, 0, length);
            iceParser.parse(data, length, false, false);
        }
        long stop = System.currentTimeMillis();
        long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double second = (stop - start) / 1000.;
        System.err.println("data : " + (totalLength) + " bytes");
        System.err.println("took : " + (second) + " s");
        System.err.println("speed : " + (((data.length) / second) / 1000000) + " Mbytes/s");
        System.err.println("footprint : " + (mem1 - mem) + " ram bytes");
        buffer.rewind();
        data = new byte[buffer.limit()];
        buffer.get(data);
        handler.setRawData(data);
        handler.printDatas();
    }

    public static IceParser getParser(IceTagHandler handler) {
        IceParser reader = new IceParser();
        reader.registerHandler(IceParser.IMAGE, handler);
        reader.registerHandler(IceParser.A, handler);
        reader.registerHandler(IceParser.SCRIPT, handler);
        reader.registerHandler(IceParser.COMMENT, handler);
        reader.registerHandler(IceParser.DIV, handler);
        reader.registerHandler(IceParser.P, handler);
        reader.registerHandler(IceParser.FORM, handler);
        return reader;
    }

    public static ByteBuffer getBuffer(int type, String toData) throws IOException {
        ByteBuffer buffer = null;
        if (type == FILE) {
            FileInputStream input = new FileInputStream(toData);
            FileChannel channel = input.getChannel();
            int fileLength = (int) channel.size();
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        } else if (type == STRING) {
            String xml = "<div>\n<div><p><img src='img1' /> <a		\n	 href='a1'>kjg <a			href='a2'>\t</a></a></p></div></div>";
            buffer = ByteBuffer.wrap(xml.getBytes());
        }
        return buffer;
    }
}
