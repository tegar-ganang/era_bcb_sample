package org.nilisoft.jftp4i.conn;

import org.nilisoft.jftp4i.framework.impl.JFTPResponseImpl;
import org.nilisoft.jftp4i.framework.impl.JFTPRequestImpl;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import org.nilisoft.jftp4i.event.DefaultClientEvent;
import org.nilisoft.jftp4i.event.IEvent;
import org.nilisoft.jftp4i.GenericServer;
import org.nilisoft.jftp4i.InitServer;

/**
 * <p> Implements the generic communication process. </p>
 * 
 * <p> This method is only used to return the proper communication pipe
 * to the framework. </p>
 *
 * <p> Created on August 23, 2004, 3:41 PM </p>
 *
 * @author  Jlopes
 */
public class GenericCommunication extends GenericServer implements ICommunication {

    /** 
   * <p> Creates a new instance of GenericCommunication </p>
   * 
   * @param in  The inputStream pipe
   * @param out The outputStream pipe
   * @param event The event that should be used
   */
    public GenericCommunication(InputStream in, OutputStream out, IEvent event, JFTPConnection conn) {
        super("org.nilisoft.jftp4i.conn.GenericCommunication");
        this.event = event;
        this.connection = conn;
        InputStreamReader inreader = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(inreader);
        PrintWriter writer = new PrintWriter(out, false);
        BufferedInputStream _in = new BufferedInputStream(in);
        BufferedOutputStream _out = new BufferedOutputStream(out);
        JFTPStream stream = new JFTPStream(reader, _in, writer, _out, this);
        try {
            this.handler = new JFTPStreamHandler(JFTPStreamHandler.ASCII_TYPE, stream);
        } catch (JFTPConnectionException ftpex) {
            log.severe("An error ocurred while traying to initialize " + "the server streams. Error description: " + ftpex.toString());
        }
    }

    /** 
   * <p> This constructor can only be used by subclasses of this class </p>
   * <p> This constructor is intended to be used only for the subclasses of this
   * class, like Request and Response classes, any other use should be prevented.
   * 
   * @param name  Name of the subclass instance
   */
    protected GenericCommunication(String name) {
        super(name);
        JFTPStream stream = new JFTPStream(null, null, null, null, this);
        try {
            this.handler = new JFTPStreamHandler(JFTPStreamHandler.ASCII_TYPE, stream);
        } catch (JFTPConnectionException ftpex) {
            log.severe("An error ocurred while traying to initialize " + "the server streams. Error description: " + ftpex.toString());
        }
    }

    /**
   * <p> Returns the main stream handler class that have encapsultated all 
   * communications that should be used between the FTP server and the client
   * requests. </p>
   * <p> The stream handler automatic define what type os transaction stream
   * will be used, implemented for the communication between the client and the 
   * server. </p>
   */
    public JFTPStreamHandler getStreamHandler() throws JFTPConnectionException {
        if (!this.handler.isInitilized()) throw new JFTPConnectionException("The streams are not initialized yet " + "or there where an error on the stream " + "initialization process. Please verify " + "your application log. Possible errors, " + "the client have disconnect the server " + "while the server was trying to bring " + "up the stream."); else return this.handler;
    }

    /**
   * <p> Defines the stream handler that should be used in the client/Server
   * communication pipe. </p>
   *
   * @param stream  The stream that should be used
   */
    protected void setStreamHandler(JFTPStreamHandler handler) {
        this.handler = handler;
    }

    /**
   * <P> Returns the specific comm pipe defined by the parameter passed to
   * this method. </p>
   * <p> The valid types for this pipe is:<br>
   * <li><i>ICommunication.TYPE_FTP_REQUEST;</i> <br>
   * <li><i>ICommunication.TYPE_FTP_RESPONSE;</i> </p>
   *
   * @param type  The comm pipe type to be returned.
   */
    public ICommunication getCommunication(int type) throws JFTPConnectionException {
        switch(type) {
            case TYPE_FTP_REQUEST:
                {
                    JFTPRequestImpl request = new JFTPRequestImpl(null);
                    request.setStreamHandler(this.getStreamHandler());
                    request.setConnection(this.getConnection());
                    request.event = this.event;
                    return request;
                }
            case TYPE_FTP_RESPONSE:
                {
                    JFTPResponseImpl response = new JFTPResponseImpl();
                    response.setStreamHandler(this.getStreamHandler());
                    response.setConnection(this.getConnection());
                    response.event = this.event;
                    return response;
                }
            default:
                throw new JFTPConnectionException("The type specified do not exist: " + type + ". The valid types are, " + "request or response.");
        }
    }

    /**
   * <p> Updates the timemillis when the user uses the open communication 
   * link. </p>
   */
    public void updateTimemillis() throws JFTPConnectionException {
        if (event instanceof DefaultClientEvent) ((DefaultClientEvent) event).lastTimeoutMillis = System.currentTimeMillis(); else {
            try {
                Class clazz = event.getClass();
                clazz.getField("lastTimeoutMillis").setLong(event, System.currentTimeMillis());
            } catch (Exception ex) {
                throw new JFTPConnectionException("Cannot update the last change made " + "by the client. The error is: " + ex.toString() + ". Maybe the attribute lastTimeoutMillis " + "was not implemented in the Event class. " + "To have a specific implementation of " + "the event interface, you must define " + "lastTimeoutMillis attribute inside " + "your class.");
            }
        }
    }

    /**
   * <p> Returns the client connection. </p>
   */
    protected JFTPConnection getConnection() {
        return this.connection;
    }

    /**
   * <p> Defines the client connection. </p>
   *
   * @param conn  The client connection
   */
    protected void setConnection(JFTPConnection conn) {
        this.connection = conn;
    }

    protected IEvent event;

    private JFTPStreamHandler handler;

    private JFTPConnection connection;
}
