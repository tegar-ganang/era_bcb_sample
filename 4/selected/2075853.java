package ursus.client.plugins.core;

import ursus.io.IHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import ursus.common.Plugin;
import ursus.client.*;

/**
 * Plugin that provides basic console input and output for the client.
 * @author Anthony
 */
public class ClientCorePlugin implements Plugin, Runnable {

    public static final String NAME = "Client Core Plugin";

    public static final String TYPE = "ursus:client:core";

    public static final String DESCRIPTION = "<B>Client Core Plugin</b>" + "<br>Core Plugin for Ursus Client</br>";

    public static final String[] REQUIRES = new String[] { "ursus:server:core" };

    public static final String TYPE_TEXT = "text";

    protected BufferedReader buffer;

    protected boolean alive;

    protected Thread theThread;

    public String getName() {
        return NAME;
    }

    public String getType() {
        return TYPE;
    }

    public String getDescription() {
        return DESCRIPTION;
    }

    public String[] requires() {
        return REQUIRES;
    }

    public void run() {
        try {
            IHandler handler = Client.getClient().getReactor().getHandler();
            while (!Thread.interrupted() && alive) {
                String write = buffer.readLine();
                handler.write(TYPE_TEXT, write);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                System.out.println("bye");
            } else e.printStackTrace();
        }
    }

    public void init() {
        IClient client = (IClient) Client.getClient();
        client.addListener(new ConsoleListener());
        client.addListener(new ConnectionListener());
        client.addListener(new DisconnectListener());
        buffer = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        alive = true;
        theThread = new Thread(this);
        theThread.start();
    }

    public void stop() {
        alive = false;
    }

    public void destroy() {
        try {
            theThread.interrupt();
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
