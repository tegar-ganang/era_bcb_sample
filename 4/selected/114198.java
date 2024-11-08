package org.dbe.composer.wfengine.bpeladmin.war;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dbe.composer.wfengine.bpel.server.engine.SdlEngineFactory;
import org.dbe.composer.wfengine.util.SdlCloser;

/**
 * Responsible for dumping the contents of a process's log file and streaming it
 * to a client.
 */
public class SdlProcessLogDumpServlet extends HttpServlet {

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        doPost(aRequest, aResponse);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        long pid;
        try {
            pid = Long.parseLong(aRequest.getParameter("pid"));
        } catch (Exception e) {
            aResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, SdlMessages.getString("SdlProcessLogDumpServlet.1"));
            return;
        }
        streamLog(aResponse, pid);
    }

    /**
     * Gets the log from the server and streams it to the response.
     * @param aResponse
     * @param aPid
     * @throws IOException
     */
    private void streamLog(HttpServletResponse aResponse, long aPid) throws IOException {
        try {
            Reader reader = SdlEngineFactory.getSdlLogger().getFullLog(aPid);
            PrintWriter out = setupStream(aPid, aResponse);
            consume(reader, out);
            out.flush();
            out.close();
        } catch (Exception ex) {
            aResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /**
     * Consumes the inputstream, writing its contents to the outputstream.
     * @param aReader
     * @param aWriter
     * @throws IOException
     */
    private void consume(Reader aReader, PrintWriter aWriter) throws IOException {
        char[] buff = new char[1024 * 4];
        int read;
        try {
            while ((read = aReader.read(buff)) != -1) {
                aWriter.write(buff, 0, read);
                if (aWriter.checkError()) {
                    break;
                }
            }
        } finally {
            SdlCloser.close(aReader);
        }
    }

    /**
     * Sets up the headers needed for the outputstream to do the streaming and
     * returns the outputstream.
     * @param aPid
     * @param aResponse
     */
    private PrintWriter setupStream(long aPid, HttpServletResponse aResponse) throws IOException {
        aResponse.setContentType("application/octet-stream");
        aResponse.setHeader("Content-disposition", "attachment;filename=process_" + aPid + ".log");
        return aResponse.getWriter();
    }
}
