package net.sourceforge.jfreeplayer.httpserver.service.freeplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.jfreeplayer.httpserver.request.HttpRequest;
import net.sourceforge.jfreeplayer.httpserver.response.HttpResponse;
import net.sourceforge.jfreeplayer.httpserver.service.freeplayer.action.Action;
import net.sourceforge.jfreeplayer.httpserver.service.freeplayer.action.ActionManager;

public class FreePlayerWorker implements Runnable {

    protected static final String LOG_NAME = FreePlayerWorker.class.getName();

    protected static final Logger logger = Logger.getLogger(LOG_NAME);

    HttpRequest message;

    BufferedReader reader;

    OutputStream writer;

    public FreePlayerWorker(HttpRequest message, BufferedReader reader, OutputStream writer) {
        this.message = message;
        this.reader = reader;
        this.writer = writer;
    }

    public void run() {
        final String METHOD = "run";
        logger.entering(LOG_NAME, METHOD);
        logger.info("worker started");
        Action action = ActionManager.getAction(message);
        if (action == null) {
            logger.warning("No action found for request " + message);
            try {
                writer.close();
                reader.close();
            } catch (Exception e) {
            }
            return;
        }
        action.setMessage(message);
        HttpResponse response = action.generateResponse();
        try {
            response.generateTo(writer);
            logger.fine("closing connection");
            writer.flush();
            writer.close();
            reader.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing streams ", e);
        }
        logger.exiting(LOG_NAME, METHOD);
    }
}
