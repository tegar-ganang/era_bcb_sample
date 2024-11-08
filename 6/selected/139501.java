package org.n3rd.carajox.exporters;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.n3rd.carajo.core.FormattingExporter;
import org.n3rd.carajo.core.Library;
import org.n3rd.carajo.core.PluginException;
import org.w3c.dom.Document;

/**
 * 
 *
 * @version @RELEASE@
 */
public class FtpExporter extends FormattingExporter {

    protected PrintWriter writer;

    protected PipedInputStream inStream;

    private PipedOutputStream outStream;

    protected FTPClient ftp;

    protected String user;

    protected String pass;

    protected String host;

    protected String dir;

    protected String file;

    @Override
    public void export(final Library lib) throws PluginException {
        try {
            new Thread(new Runnable() {

                public void run() {
                    formatter.format(lib, writer);
                    writer.flush();
                    writer.close();
                }
            }).start();
            ftp.connect(host);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                ftp.disconnect();
                throw new PluginException("Unable to connect to FTP");
            }
            ftp.login(user, pass);
            ftp.pasv();
            ftp.changeWorkingDirectory(dir);
            ftp.storeFile(file, inStream);
            ftp.logout();
        } catch (SocketException e) {
            throw new PluginException(e);
        } catch (IOException e) {
            throw new PluginException(e);
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void configure(Document config) {
    }

    @Override
    public void init() throws PluginException {
        try {
            inStream = new PipedInputStream();
            outStream = new PipedOutputStream(inStream);
            writer = new PrintWriter(outStream, true);
            ftp = new FTPClient();
        } catch (IOException e) {
            throw new PluginException(e);
        }
    }
}
