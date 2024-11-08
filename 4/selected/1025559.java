package ca.gc.drdc_rddc.atlantic.event;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.log4j.Logger;
import ca.gc.drdc_rddc.atlantic.hla.HLAModel;

public class EventServer implements Runnable {

    /** Logger for this class */
    static final Logger logger = Logger.getLogger(EventServer.class);

    private EventWriterFactory writerFactory;

    private EventReaderFactory readerFactory;

    HLAModel model;

    EventWriter modelWriter;

    boolean finished = false;

    public static final int DEFAULT_XML_PORT = 4444;

    public static final int DEFAULT_OBJECT_PORT = 4445;

    private int port;

    public EventServer(HLAModel m, EventWriter modelWriter, int port, EventWriterFactory writerFactory, EventReaderFactory readerFactory) {
        model = m;
        this.modelWriter = modelWriter;
        this.port = port;
        this.writerFactory = writerFactory;
        this.readerFactory = readerFactory;
    }

    public void run() {
        ThreadGroup tg = new ThreadGroup("EventSocketHandlers");
        try {
            ServerSocket servSock = new ServerSocket(port);
            try {
                while (!finished) {
                    Socket s = servSock.accept();
                    EventWriter socketWriter = writerFactory.createEventWriter(s.getOutputStream());
                    Recorder recorder = new Recorder(socketWriter);
                    recorder.registerWith(model);
                    EventReader socketReader = readerFactory.createEventReader(s.getInputStream());
                    Player player = new Player(socketReader, modelWriter);
                    Thread thread = new Thread(tg, new EventSocketHandler(recorder, player));
                    thread.run();
                }
            } finally {
                tg.interrupt();
                servSock.close();
            }
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    public void shutdown() {
        finished = true;
    }
}
