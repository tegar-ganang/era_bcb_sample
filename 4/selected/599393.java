package com.octane.network.clj;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Date;
import com.octane.start.OctaneLauncherMain;
import com.octane.util.FileUtils;
import com.octane.util.StringUtils;

/**
 * Simple Server Client Thread Handler 
 * @author Berlin
 * @version $Revision: 1.0 $
 */
public class OctaneClientHandlerThread implements Runnable {

    private Socket client;

    private DataInputStream dataInputStream;

    private OutputStream outputStream;

    private String clientInputData;

    private RequestState requestState;

    private String installDir = "";

    private String htdocsDir = "";

    /**
	 * Constructor for OctaneClientHandlerThread.
	 * @param client Socket
	 */
    public OctaneClientHandlerThread(final Socket client) {
        this.init(client);
    }

    /**
	 * Constructor for OctaneClientHandlerThread.
	 */
    public OctaneClientHandlerThread() {
        ;
    }

    /**
	 * Implementation Routine init.
	 * @param client Socket
	 */
    public final void init(final Socket client) {
        this.setInstallDir();
        this.setHtdocsDir();
        this.setClientSocket(client);
        try {
            System.out.println("communicating with server=" + client);
            this.dataInputStream = new DataInputStream(client.getInputStream());
            this.outputStream = client.getOutputStream();
        } catch (IOException e) {
            try {
                client.close();
            } catch (IOException e2) {
                ;
            }
            System.err.println("Exception while opening socket streams: " + e);
            return;
        }
    }

    /**
     * Implementation Routine setClientSocket.
     * @param client Socket
     */
    public void setClientSocket(final Socket client) {
        this.client = client;
    }

    /**
     * @return the requestState
     */
    public final RequestState getRequestState() {
        return requestState;
    }

    /**
     * Implementation Routine isActionOpenFile.
     * @return boolean
     */
    public final boolean isActionOpenFile() {
        if (this.requestState != null) {
            return this.requestState.isActionOpenFile();
        }
        return false;
    }

    /**
     * Implementation Routine isActionBrowseDir.
     * @return boolean
     */
    public final boolean isActionBrowseDir() {
        if (this.requestState != null) {
            return this.requestState.isActionBrowseDir();
        }
        return false;
    }

    /**
     * @return the installDir
     */
    public final String getInstallDir() {
        return installDir;
    }

    /**
     * @param installDir the installDir to set
     */
    public final void setInstallDir(String installDir) {
        this.installDir = installDir;
    }

    /**
     * Implementation Routine setInstallDir.
     */
    public final void setInstallDir() {
        this.setInstallDir(OctaneLauncherMain.getInstallDir());
    }

    /**
     * @return the htdocsDir
     */
    public final String getHtdocsDir() {
        return htdocsDir;
    }

    /**
     */
    public final void setHtdocsDir() {
        this.htdocsDir = this.getInstallDir() + File.separatorChar + ServerConsts.DEFAULT_HTDOCS;
    }

    /**
     * Implementation Routine readClientInput.
     * @param self OctaneClientHandlerThread
     * @throws IOException
     */
    public void readClientInput(final OctaneClientHandlerThread self) throws IOException {
        final StringBuffer bufferData = new StringBuffer(128);
        try {
            final BufferedReader bufReader = new BufferedReader(new InputStreamReader(this.dataInputStream));
            String line;
            int lineCount = 0;
            while (true) {
                line = bufReader.readLine();
                if (line == null) {
                    break;
                }
                System.out.println("[server/" + line.length() + ":" + lineCount + "] " + line);
                if (line.length() == 0) {
                    break;
                }
                bufferData.append(line);
                lineCount++;
            }
            if (bufReader != null) {
                bufReader.close();
            }
        } finally {
            this.clientInputData = bufferData.toString();
        }
    }

    /**
     * Implementation Routine isIndexPage.
     * @return boolean
     */
    public boolean isIndexPage() {
        if (this.requestState != null) {
            if (StringUtils.isEmpty(this.requestState.getRequestLocation())) {
                return true;
            }
            final String loc = this.requestState.getRequestLocation();
            if ("/".equals(loc)) {
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Implementation Routine writeDefaultIndexPage.
     */
    public void writeDefaultIndexPage() {
        final PrintStream ps = new PrintStream(this.outputStream);
        StringBuffer buf = new StringBuffer(80);
        buf.append("HTTP/1.1 200 OK").append(ServerConsts.ENDL);
        buf.append("Server: Octane-Test").append(ServerConsts.ENDL);
        buf.append("Connection: close").append(ServerConsts.ENDL);
        buf.append("Content-Type: text/html; charset=ISO-8859-1\r\n");
        buf.append(ServerConsts.ENDL);
        buf.append("<html>");
        buf.append("<body> [Octane] Page Not Found ");
        buf.append("" + new Date() + " / " + this.client);
        buf.append("</body>");
        buf.append("</html>");
        buf.append(ServerConsts.ENDL);
        ps.print(buf);
    }

    /**
     * Implementation Routine writeClientOutput.
     * @param self OctaneClientHandlerThread
     */
    public void writeClientOutput(final OctaneClientHandlerThread self) {
        System.out.println(this.requestState);
        System.out.println(this.getHtdocsDir());
        Throwable lastErrorReadHtml = null;
        try {
            readWriteHtmlDoc();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            lastErrorReadHtml = e;
        }
        if (lastErrorReadHtml != null) {
            this.writeDefaultIndexPage();
        }
    }

    /**
     * Implementation Routine writeClientOutput.
     * @param self OctaneClientHandlerThread
     */
    public final void writeClientOutputDefault(final OctaneClientHandlerThread self) {
        System.out.println(this.requestState);
        System.out.println(this.getHtdocsDir());
        Throwable lastErrorReadHtml = null;
        try {
            readWriteHtmlDoc();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            lastErrorReadHtml = e;
        }
        if (lastErrorReadHtml != null) {
            this.writeDefaultIndexPage();
        }
    }

    /**
     * Implementation Routine writeClientOutputIndex.
     * @param self OctaneClientHandlerThread
     */
    public void writeClientOutputIndex(final OctaneClientHandlerThread self) {
        System.out.println("INFO: OctaneClientHandler/writeClientOutputIndex : Reading index.html");
        System.out.println("INFO: OctaneClientHandler/writeClientOutputIndex : " + this.requestState);
        System.out.println("INFO: OctaneClientHandler/writeClientOutputIndex htdocs : " + this.getHtdocsDir());
        Throwable lastErrorReadHtml = null;
        try {
            readWriteHtmlDoc();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            lastErrorReadHtml = e;
        }
        if (lastErrorReadHtml != null) {
            this.writeDefaultIndexPage();
        }
    }

    /**
     * Implementation Routine readFileLocation.
     * @return File
     * @throws FileNotFoundException
     */
    public File readFileLocation() throws FileNotFoundException {
        final String pageRaw = this.getRequestState().getRequestLocation();
        if ("/".equals(pageRaw)) {
            final File file = new File(this.getHtdocsDir() + File.separatorChar + "index.html");
            return file;
        }
        String page = pageRaw;
        if ((pageRaw.length() > 1) && pageRaw.startsWith("/")) {
            page = pageRaw.substring(1);
        }
        final File file = new File(this.getHtdocsDir() + File.separatorChar + page);
        if (!file.exists()) {
            throw new FileNotFoundException("Invalid File");
        }
        System.out.println(file);
        return file;
    }

    /**
     * Implementation Routine writeHtmlDoc.
     * @param statusCode String
     * @param contentType String
     * @param htmlData String
     * @throws IOException
     */
    public void writeHtmlDoc(final String statusCode, final String contentType, final String htmlData) throws IOException {
        final PrintStream ps = new PrintStream(this.outputStream);
        StringBuffer buf = new StringBuffer(80);
        buf.append("HTTP/1.1 ").append(statusCode).append(" OK").append(ServerConsts.ENDL);
        buf.append("Server: Octane-Test").append(ServerConsts.ENDL);
        buf.append("Connection: close").append(ServerConsts.ENDL);
        buf.append("Content-Type: " + contentType + "; charset=ISO-8859-1").append(ServerConsts.ENDL);
        buf.append(ServerConsts.ENDL);
        buf.append(htmlData);
        buf.append(ServerConsts.ENDL);
        ps.print(buf);
    }

    /**
     * Implementation Routine readWriteHtmlDoc.
     * @throws IOException
     */
    public void readWriteHtmlDoc() throws IOException {
        final File htmlDocument = this.readFileLocation();
        System.out.println("DEBUG: readWriteHtmlDoc : " + htmlDocument);
        System.out.println("DEBUG: readWriteHtmlDoc file-exists? : " + htmlDocument.exists());
        final String fileData = FileUtils.readLinesRaw(htmlDocument);
        final String contentType = (htmlDocument.getName().endsWith("css")) ? "text/css" : "text/html";
        final PrintStream ps = new PrintStream(this.outputStream);
        StringBuffer buf = new StringBuffer(80);
        buf.append("HTTP/1.1 200 OK").append(ServerConsts.ENDL);
        buf.append("Server: Octane-Test").append(ServerConsts.ENDL);
        buf.append("Connection: close").append(ServerConsts.ENDL);
        buf.append("Content-Type: " + contentType + "; charset=ISO-8859-1").append(ServerConsts.ENDL);
        buf.append(ServerConsts.ENDL);
        buf.append(fileData);
        buf.append(ServerConsts.ENDL);
        ps.print(buf);
        ps.flush();
    }

    /**
     * Implementation Routine parseInputHeaders.
     * @param self OctaneClientHandlerThread
     */
    public void parseInputHeaders(final OctaneClientHandlerThread self) {
        this.requestState = InputParser.parse(clientInputData);
    }

    /**
     * Implementation Routine handleClient.
     * @param self OctaneClientHandlerThread
     */
    public void handleClient(final OctaneClientHandlerThread self) {
        try {
            this.readClientInput(self);
            this.parseInputHeaders(self);
            if (isIndexPage()) {
                this.writeClientOutputIndex(self);
            } else {
                this.writeClientOutput(self);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (this.outputStream != null) {
                    this.outputStream.flush();
                    this.outputStream.close();
                }
                if (this.dataInputStream != null) {
                    this.dataInputStream.close();
                }
                client.close();
            } catch (IOException e2) {
                ;
            }
            System.out.println("[server] closing connection, socket=" + this.client);
        }
    }

    /**
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        synchronized (this) {
            this.handleClient(this);
        }
    }
}
