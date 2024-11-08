package de.herberlin.server.httpd;

import java.io.*;
import java.text.*;
import java.util.*;
import de.herberlin.server.common.*;
import de.herberlin.server.common.TempFileHandler;
import de.herberlin.server.httpd.ssi.BSssi;
import de.herberlin.server.httpd.ssi.SSICommand;
import de.herberlin.wwwutil.SimpleResponse;
import de.herberlin.wwwutil.httperror.HttpError;
import de.herberlin.wwwutil.httperror.InternalServerError_500;

/**
 * @author hans joachim herbertz
 * created 28.01.2004
 */
public class SsiHandler extends CgiRequestHandler {

    private BSssi ssi = null;

    /**
     * @param data
     */
    public SsiHandler(HttpData data) {
        super(data);
    }

    /**
     * Constructor for subprocesses preserves the environment.
     * @param data
     * @param ssi
     */
    public SsiHandler(HttpData data, BSssi ssi) {
        super(data);
        this.ssi = ssi;
    }

    /** handles Server side includes*/
    private void handleSSI(HttpData data) throws HttpError, IOException {
        File tempFile = TempFileHandler.getTempFile();
        FileOutputStream out = new FileOutputStream(tempFile);
        BufferedReader in = new BufferedReader(new FileReader(data.realPath));
        String[] env = getEnvironmentVariables(data);
        if (ssi == null) {
            ssi = new BSssi();
        }
        ssi.addEnvironment(env);
        if (data.resp == null) {
            SimpleResponse resp = new SimpleResponse();
            resp.setHeader("Content-Type", "text/html");
            moreHeaders(resp);
            resp.setHeader("Connection", "close");
            data.resp = resp;
            resp.write(data.out);
        }
        String t;
        int start;
        Enumeration en;
        boolean anIfCondition = true;
        while ((t = in.readLine()) != null) {
            if ((start = t.indexOf("<!--#")) > -1) {
                if (anIfCondition) out.write(t.substring(0, start).getBytes());
                try {
                    en = ssi.parse(t.substring(start)).elements();
                    SSICommand command;
                    while (en.hasMoreElements()) {
                        command = (SSICommand) en.nextElement();
                        logger.fine("Command=" + command);
                        switch(command.getCommand()) {
                            case BSssi.CMD_IF_TRUE:
                                anIfCondition = true;
                                break;
                            case BSssi.CMD_IF_FALSE:
                                anIfCondition = false;
                                break;
                            case BSssi.CMD_CGI:
                                out.flush();
                                if (command.getFileType() != null && command.getFileType().startsWith("shtm")) {
                                    HttpData d = newHttpData(data);
                                    d.out = out;
                                    d.realPath = HttpThread.getMappedFilename(command.getMessage(), data.req.getUrl());
                                    new SsiHandler(d, ssi).perform();
                                } else {
                                    String application = getExtension(command.getFileType());
                                    if (application == null) {
                                        writePaused(new FileInputStream(HttpThread.getMappedFilename(command.getMessage(), data.req.getUrl())), out, pause);
                                    } else {
                                        String parameter = "";
                                        if (command.getMessage().indexOf("php") >= 0) {
                                            parameter = "-f ";
                                        }
                                        Process p = Runtime.getRuntime().exec(application + " " + parameter + HttpThread.getMappedFilename(command.getMessage(), data.req.getUrl()));
                                        BufferedReader pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                        String aLine;
                                        while ((aLine = pIn.readLine()) != null) out.write((aLine + "\n").getBytes());
                                        pIn.close();
                                    }
                                }
                                break;
                            case BSssi.CMD_EXEC:
                                Process p = Runtime.getRuntime().exec(command.getMessage());
                                BufferedReader pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                String aLine;
                                while ((aLine = pIn.readLine()) != null) out.write((aLine + "\n").getBytes());
                                BufferedReader pErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                                while ((aLine = pErr.readLine()) != null) out.write((aLine + "\n").getBytes());
                                pIn.close();
                                pErr.close();
                                p.destroy();
                                break;
                            case BSssi.CMD_INCLUDE:
                                File incFile = HttpThread.getMappedFilename(command.getMessage());
                                if (incFile.exists() && incFile.canRead()) {
                                    writePaused(new FileInputStream(incFile), out, pause);
                                }
                                break;
                            case BSssi.CMD_FILESIZE:
                                long sizeBytes = HttpThread.getMappedFilename(command.getMessage(), data.req.getUrl()).length();
                                double smartSize;
                                String unit = "bytes";
                                if (command.getFileType().trim().equals("abbrev")) {
                                    if (sizeBytes > 1000000) {
                                        smartSize = sizeBytes / 1024000.0;
                                        unit = "M";
                                    } else if (sizeBytes > 1000) {
                                        smartSize = sizeBytes / 1024.0;
                                        unit = "K";
                                    } else {
                                        smartSize = sizeBytes;
                                        unit = "bytes";
                                    }
                                    NumberFormat numberFormat = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.ENGLISH));
                                    out.write((numberFormat.format(smartSize) + "" + unit).getBytes());
                                } else {
                                    NumberFormat numberFormat = new DecimalFormat("#,###,##0", new DecimalFormatSymbols(Locale.ENGLISH));
                                    out.write((numberFormat.format(sizeBytes) + " " + unit).getBytes());
                                }
                                break;
                            case BSssi.CMD_FLASTMOD:
                                out.write(ssi.format(new Date(HttpThread.getMappedFilename(command.getMessage(), data.req.getUrl()).lastModified()), TimeZone.getTimeZone("GMT")).getBytes());
                                break;
                            case BSssi.CMD_NOECHO:
                                break;
                            case BSssi.CMD_ECHO:
                            default:
                                out.write(command.getMessage().getBytes());
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    out.write((ssi.getErrorMessage() + " " + e.getMessage()).getBytes());
                }
                if (anIfCondition) out.write("\n".getBytes());
            } else {
                if (anIfCondition) out.write((t + "\n").getBytes());
            }
            out.flush();
        }
        in.close();
        out.close();
        data.fileData.setContentType("text/html");
        data.fileData.setFile(tempFile);
        writePaused(new FileInputStream(tempFile), data.out, pause);
        logger.fine("HandleSSI done for " + data.resp);
    }

    public void perform() throws HttpError {
        try {
            handleSSI(data);
        } catch (IOException e) {
            logger.throwing(getClass().getName(), "perform", e);
            throw new InternalServerError_500(e);
        }
    }

    /**
     * Returns a new instance of HttpData;
     * This is not a clone, 
     * all fields are just references.
     * @param data
     * @return
     */
    private HttpData newHttpData(HttpData data) {
        HttpData d = new HttpData();
        d.fileData = data.fileData;
        d.inetAddress = data.inetAddress;
        d.mimeType = data.mimeType;
        d.out = data.out;
        d.port = data.port;
        d.req = data.req;
        d.resp = data.resp;
        d.realPath = data.realPath;
        return d;
    }
}
