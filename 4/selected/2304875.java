package org.thole.phiirc.irc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;

public class IRCDataWriteHandler implements Runnable {

    private IRCConnection connection;

    private IRCMsgHandler mh;

    private Buffer buffer = new UnboundedFifoBuffer();

    protected IRCDataWriteHandler(final IRCConnection connection) {
        this.setConnection(connection);
        this.setMh(new IRCMsgHandler(this.getConnection()));
    }

    @Override
    public void run() {
        writeThread();
    }

    public void write(final String msg) {
        this.getBuffer().add(msg);
    }

    private void writeThread() {
        try {
            final BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(this.getConnection().getSocket().getOutputStream(), "UTF-8"));
            String msg;
            while (this.getConnection().getSocket().isConnected()) {
                if (!this.getBuffer().isEmpty()) {
                    msg = (String) this.getBuffer().remove();
                    System.out.println("DEBUG: " + msg);
                    wr.write(msg + "\r\n");
                    wr.flush();
                }
                if (this.getBuffer().size() <= 2 || this.getBuffer().get().toString().startsWith("WHO ")) Thread.sleep(50); else if (this.getBuffer().size() <= 5) Thread.sleep(1000); else if (this.getBuffer().size() > 5) Thread.sleep(1500);
            }
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public IRCMsgHandler getMh() {
        return mh;
    }

    private void setMh(final IRCMsgHandler mh) {
        this.mh = mh;
    }

    private IRCConnection getConnection() {
        return connection;
    }

    private void setConnection(final IRCConnection connection) {
        this.connection = connection;
    }

    private Buffer getBuffer() {
        return this.buffer;
    }
}
