package dl.pushlog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

public class MultiStreamWatchTest extends TestCase {

    /** much easier with log4j to make sense of logging from of multithread test */
    private static Logger log = Logger.getLogger(MultiStreamWatchTest.class);

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MultiStreamWatchTest.class);
    }

    public void testStop() throws InterruptedException {
        log.info(" >> testStop");
        List<ChannelConfig> cfg = getWatchEntries();
        final MultiStreamWatch msw = new MultiStreamWatch(cfg);
        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        Thread t = new Thread(new Runnable() {

            public void run() {
                log.info("msw thread started");
                startSignal.countDown();
                msw.run(new MultiStreamWatch.Callback() {

                    public CallbackResponse processMatch(ChannelConfig config, String line) {
                        System.out.print("msw cb: " + config.getId() + ">" + line);
                        return CallbackResponse.Continue;
                    }
                });
                log.info("msw thread exiting");
                doneSignal.countDown();
            }
        });
        log.info("starting msw thread");
        t.start();
        log.info("awaiting msw thread start-up");
        startSignal.await();
        log.info("interrupting msw thread");
        t.interrupt();
        log.info("waiting for msw thread to stop");
        doneSignal.await();
        log.info("msw thread completed.");
    }

    public void testLoadConfig_InputStream() throws IOException {
        log.info(" >> testLoadConfig_InputStream");
        Class clazz = this.getClass();
        final String filename = clazz.getSimpleName() + ".pushlog";
        log.info("config file: " + filename);
        InputStream is = clazz.getResourceAsStream(filename);
        Collection<ChannelConfig> config = MultiStreamWatch.loadConfig(is);
        assertTrue(config.size() == 4);
        Map<String, ChannelConfig> name2config = new HashMap<String, ChannelConfig>();
        for (ChannelConfig entry : config) {
            name2config.put(entry.getId(), entry);
        }
        assertTrue(name2config.containsKey("oneMoreLine"));
        ChannelConfig entry = name2config.get("oneMoreLine");
        assertEquals(entry.getChannelOpener().getClass(), FileChannelOpener.class);
        assertEquals(".*", entry.getRegex());
        assertEquals(new File("oneMoreLine.filename"), entry.getFile());
        assertEquals("${0}", entry.getFormat());
        assertTrue(name2config.containsKey("mailSend"));
        entry = name2config.get("mailSend");
        assertEquals(".*(status=send).*", entry.getRegex());
        assertEquals(new File("/var/log/mail"), entry.getFile());
        assertEquals("email(${1})", entry.getFormat());
        assertTrue(name2config.containsKey("mailReceived"));
        entry = name2config.get("mailReceived");
        assertEquals(".*status=send.*", entry.getRegex());
        assertEquals(new File("/var/log/mail"), entry.getFile());
        assertEquals("${0}", entry.getFormat());
        assertTrue(name2config.containsKey("urlStream"));
        entry = name2config.get("urlStream");
        assertEquals(entry.getChannelOpener().getClass(), URLStreamOpener.class);
    }

    /** @return list of configuration objects for testing */
    private List<ChannelConfig> getWatchEntries() {
        ChannelConfig cc = new ChannelConfig("System.in") {

            public ReadableByteChannel openChannel() throws IOException {
                return Channels.newChannel(System.in);
            }
        };
        return Arrays.asList(new ChannelConfig[] { cc });
    }
}
