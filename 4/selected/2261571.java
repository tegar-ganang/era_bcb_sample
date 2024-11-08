package org.indi.client;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.indi.reactor.OutputQueue;
import org.indi.reactor.Reactor;
import org.xml.sax.SAXException;

public class ServerHandler extends OutputQueue implements Runnable {

    private final PipedInputStream fromServer;

    private final PipedOutputStream toThread;

    private final SAXParser parser;

    private Queue<Object> threadToClientQueue;

    public ServerHandler(Reactor r, SelectableChannel ch) throws IOException, ParserConfigurationException, SAXException {
        super(r, ch);
        this.fromServer = new PipedInputStream();
        this.toThread = new PipedOutputStream(this.fromServer);
        this.toThread.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <doc>".getBytes());
        this.parser = SAXParserFactory.newInstance().newSAXParser();
    }

    public void setQueue(Queue<Object> toClientQueue) {
        this.threadToClientQueue = toClientQueue;
        (new Thread(this)).start();
    }

    public void run() {
        SaxHandler h = new SaxHandler(this.threadToClientQueue);
        try {
            this.parser.parse(this.fromServer, h);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRead() throws IOException {
        ByteBuffer input = ByteBuffer.allocate(10000);
        try {
            ((ReadableByteChannel) this.channel).read(input);
        } catch (IOException e) {
            register(this.registeredOperations - (SelectionKey.OP_READ & this.registeredOperations));
            return;
        }
        input.flip();
        if (input.position() == input.limit()) {
            onClientDisconnected();
            return;
        }
        if (input.position() == input.limit()) {
            register(this.registeredOperations - (SelectionKey.OP_READ & this.registeredOperations));
        }
        this.toThread.write(input.array(), input.position(), input.limit());
        input.clear();
    }

    private void onClientDisconnected() {
        this.reactor.unregister(this);
    }

    public void shutDown() {
        try {
            this.toThread.write("</doc>".getBytes());
            toThread.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
