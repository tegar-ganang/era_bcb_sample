package org.simpleframework.http;

import java.util.List;
import org.simpleframework.http.store.BufferStorage;
import org.simpleframework.http.store.Storage;
import org.simpleframework.util.net.Part;
import junit.framework.TestCase;

public class ReactorTest extends TestCase implements Reactor {

    private static final String SOURCE = "POST /index.html HTTP/1.0\r\n" + "Content-Type: multipart/form-data; boundary=AaB03x\r\n" + "Accept: image/gif;q=1.0,\r\n image/jpeg;q=0.8,\r\n" + "   \t\t   image/png;\t\r\n\t" + "   q=1.0,*;q=0.1\r\n" + "Accept-Language: fr;q=0.1, en-us;q=0.4, en-gb; q=0.8, en;q=0.7\r\n" + "Host:   some.host.com    \r\n" + "Cookie: $Version=1; UID=1234-5678; $Path=/; $Domain=.host.com\r\n" + "Cookie: $Version=1; NAME=\"Niall Gallagher\"; $path=\"/\"\r\n" + "\r\n" + "--AaB03x\r\n" + "Content-Disposition: file; name=\"pics\"; filename=\"file1.txt\"; modification-date=\"Wed, 12 Feb 1997 16:29:51 -0500\"\r\n" + "Content-Type: text/plain\r\n\r\n" + "example contents of file1.txt\r\n" + "--AaB03x\r\n" + "Content-Type: multipart/mixed; boundary=BbC04y\r\n\r\n" + "--BbC04y\r\n" + "Content-Disposition: file; name=\"pics\"; filename=\"file2.txt\"\r\n" + "Content-Type: text/plain\r\n\r\n" + "example contents of file3.txt ...\r\n" + "--BbC04y\r\n" + "Content-Disposition: file; name=\"pics\"; filename=\"file3.txt\"\r\n" + "Content-Type: text/plain\r\n\r\n" + "example contents of file4.txt ...\r\n" + "--BbC04y\r\n" + "Content-Disposition: file; name=\"pics\"; filename=\"file4.txt\"\r\n" + "Content-Type: text/plain\r\n\r\n" + "example contents of file4.txt ...\r\n" + "--BbC04y--\r\n" + "--AaB03x--\r\n";

    public static class TestChannel implements Channel {

        private Cursor cursor;

        public TestChannel(Cursor cursor, int dribble) {
            this.cursor = new DribbleCursor(cursor, dribble);
        }

        public Storage getStorage() {
            return null;
        }

        public Cursor getCursor() {
            return cursor;
        }

        public Sender getSender() {
            return null;
        }

        public Link getLink() {
            return null;
        }
    }

    public void testHandler() throws Exception {
        testHandler(1024);
        for (int i = 10; i < 2048; i++) {
            testHandler(i);
        }
    }

    public void testHandler(int dribble) throws Exception {
        Cursor cursor = new StringCursor(SOURCE);
        Channel channel = new TestChannel(cursor, dribble);
        Storage storage = new BufferStorage();
        StartEvent event = new StartEvent(channel, storage);
        handle(event);
        assertEquals(cursor.ready(), 0);
    }

    public void handle(StartEvent event) throws Exception {
        Collector collector = event.getCollector();
        collector.collect(this);
    }

    public void handle(RequestEvent event) throws Exception {
        Collector collector = event.getCollector();
        collector.collect(this);
    }

    public void handle(DelayEvent event) throws Exception {
        Collector collector = event.getCollector();
        collector.collect(this);
    }

    public void handle(TransactionEvent event) throws Exception {
        Entity body = event.getEntity();
        Channel channel = event.getChannel();
        Cursor cursor = channel.getCursor();
        RequestHeader header = body.getHeader();
        List<Part> list = body.getParts();
        assertEquals(header.getTarget(), "/index.html");
        assertEquals(header.getMethod(), "POST");
        assertEquals(header.getMajor(), 1);
        assertEquals(header.getMinor(), 0);
        assertEquals(header.getContentType().getPrimary(), "multipart");
        assertEquals(header.getContentType().getSecondary(), "form-data");
        assertEquals(header.getValue("Host"), "some.host.com");
        assertEquals(header.getValues("Accept").size(), 4);
        assertEquals(header.getValues("Accept").get(0), "image/gif");
        assertEquals(header.getValues("Accept").get(1), "image/png");
        assertEquals(header.getValues("Accept").get(2), "image/jpeg");
        assertEquals(header.getValues("Accept").get(3), "*");
        assertEquals(list.size(), 4);
        assertEquals(list.get(0).getContentType().getPrimary(), "text");
        assertEquals(list.get(0).getContentType().getSecondary(), "plain");
        assertEquals(list.get(0).getHeader("Content-Disposition"), "file; name=\"pics\"; filename=\"file1.txt\"; modification-date=\"Wed, 12 Feb 1997 16:29:51 -0500\"");
        assertEquals(list.get(0).getContentDisposition().getName(), "pics");
        assertEquals(list.get(0).getContentDisposition().getFileName(), "file1.txt");
        assertEquals(list.get(0).getContentDisposition().isFile(), true);
        assertEquals(list.get(1).getContentType().getPrimary(), "text");
        assertEquals(list.get(1).getContentType().getSecondary(), "plain");
        assertEquals(list.get(1).getHeader("Content-Disposition"), "file; name=\"pics\"; filename=\"file2.txt\"");
        assertEquals(list.get(1).getContentType().getPrimary(), "text");
        assertEquals(list.get(1).getContentDisposition().getName(), "pics");
        assertEquals(list.get(1).getContentDisposition().getFileName(), "file2.txt");
        assertEquals(list.get(1).getContentDisposition().isFile(), true);
        assertEquals(list.get(2).getContentType().getSecondary(), "plain");
        assertEquals(list.get(2).getHeader("Content-Disposition"), "file; name=\"pics\"; filename=\"file3.txt\"");
        assertEquals(list.get(2).getContentDisposition().getName(), "pics");
        assertEquals(list.get(2).getContentDisposition().getFileName(), "file3.txt");
        assertEquals(list.get(2).getContentDisposition().isFile(), true);
        assertEquals(list.get(3).getContentType().getPrimary(), "text");
        assertEquals(list.get(3).getContentType().getSecondary(), "plain");
        assertEquals(list.get(3).getHeader("Content-Disposition"), "file; name=\"pics\"; filename=\"file4.txt\"");
        assertEquals(list.get(3).getContentDisposition().getName(), "pics");
        assertEquals(list.get(3).getContentDisposition().getFileName(), "file4.txt");
        assertEquals(list.get(3).getContentDisposition().isFile(), true);
        assertEquals(cursor.ready(), 0);
    }
}
