package groom.handlers;

import groom.Acceptor;
import groom.utils.Configuration;
import groom.FileTypeHandler;
import groom.GroomException;
import groom.HttpQuery;
import groom.utils.LoggerInterface;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 *
 * @author Mathieu Allory
 */
public class CgiHandler implements FileTypeHandler {

    protected LoggerInterface _logger;

    protected boolean _authorized;

    protected String _content;

    protected File _resourceAsFile;

    protected HttpQuery _query;

    protected long _expirationTime;

    public CgiHandler() throws Exception {
        try {
            _authorized = false;
            if (Configuration.getInstance().getProperty("Handler." + this.getClass().getSimpleName() + ".Authorize").equals("true")) _authorized = true;
        } catch (Exception ex) {
        }
        try {
            _expirationTime = Long.parseLong(Configuration.getInstance().getProperty("Handler." + this.getClass().getSimpleName() + ".Watchdog"));
        } catch (Exception ex) {
            _expirationTime = 10000;
        }
    }

    public void setResource(HttpQuery iQuery, LoggerInterface iLogger) {
        _query = iQuery;
        _logger = iLogger;
        _content = "";
    }

    public boolean mustFinalizeHeaders() {
        return false;
    }

    public long prefetch() throws GroomException {
        _logger.log(this, 10, "Entered handler to get resource " + _query.Resource);
        _resourceAsFile = new File(_query.Resource);
        if (!_authorized) throw new GroomException(GroomException.NOT_AUTHORIZED);
        if (!_resourceAsFile.exists() || !_resourceAsFile.isFile() || !_resourceAsFile.canRead()) {
            throw new GroomException(GroomException.FILE_NOT_FOUND);
        }
        return -1;
    }

    public void writeContent(OutputStream oStream) throws GroomException {
        class Watchdog extends TimerTask {

            private Process _ProcessToWatch;

            Watchdog(Process iProcessToWatch) {
                _ProcessToWatch = iProcessToWatch;
            }

            public void run() {
                try {
                    _ProcessToWatch.exitValue();
                } catch (IllegalThreadStateException ex) {
                    _logger.logErr(this, 5, "CGI " + _query.Resource + " took too much time, aborting");
                    _ProcessToWatch.destroy();
                }
            }
        }
        Process aProcess = runBinary(_query.GetParameters, _query.PostParameters);
        if (_expirationTime > 0) (new Timer()).schedule(new Watchdog(aProcess), _expirationTime);
        try {
            if (_query.PostParameters != null && _query.PostParameters.length != 0) {
                OutputStream aDOS = aProcess.getOutputStream();
                aDOS.write(_query.PostParameters);
                aDOS.write("\r\n".getBytes());
                aDOS.flush();
                aDOS.close();
            }
            DataInputStream aDIS = new DataInputStream(aProcess.getInputStream());
            while (true) oStream.write(aDIS.readUnsignedByte());
        } catch (EOFException ex) {
        } catch (IOException ex) {
            throw new GroomException(GroomException.INTERNAL_ERROR);
        }
        aProcess.destroy();
        aProcess = null;
    }

    protected Process runBinary(String iGetParameters, byte[] iPostParameters) throws GroomException {
        Process aProcess = null;
        try {
            String[] aEnvVar = new String[1];
            aEnvVar = (String[]) getEnvironmentVariables(iGetParameters, iPostParameters).toArray(aEnvVar);
            _logger.log(this, 6, "Running CGI in " + _query.Resource);
            aProcess = Runtime.getRuntime().exec(_query.Resource, aEnvVar, new File(extractPath(_query.Resource)));
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new GroomException(GroomException.IO_ERROR);
        }
        return aProcess;
    }

    protected Vector getEnvironmentVariables(String iGetParameters, byte[] iPostParameters) {
        Vector aEnvVarVector = new Vector();
        aEnvVarVector.add("SERVER_SOFTWARE=" + Acceptor.gServerSignature + "");
        aEnvVarVector.add("SERVER_NAME=" + _query.ServerAddress);
        aEnvVarVector.add("GATEWAY_INTERFACE=CGI/1.1");
        aEnvVarVector.add("REMOTE_HOST=");
        aEnvVarVector.add("REMOTE_ADDR=" + _query.ClientAddress);
        aEnvVarVector.add("REDIRECT_STATUS=200");
        aEnvVarVector.add("PATH_TRANSLATED=" + _query.Resource);
        aEnvVarVector.add("PATH_INFO=" + _query.Path);
        aEnvVarVector.add("SCRIPT_FILENAME=" + _query.Filename);
        aEnvVarVector.add("SCRIPT_NAME=" + _query.Filename);
        aEnvVarVector.add("SERVER_PROTOCOL=HTTP/1.1");
        Iterator aIterHeaders = (Iterator) _query.HeadersMap.keySet().iterator();
        while (aIterHeaders.hasNext()) {
            String aHeader = (String) aIterHeaders.next();
            aEnvVarVector.add("HTTP_" + aHeader.toUpperCase().replace("-", "_") + "=" + _query.HeadersMap.get(aHeader));
        }
        if (iPostParameters != null && iPostParameters.length != 0) {
            aEnvVarVector.add("REQUEST_METHOD=POST");
            aEnvVarVector.add("CONTENT_LENGTH=" + _query.PostParameters.length);
            if (_query.HeadersMap.containsKey("Content-Type")) aEnvVarVector.add("CONTENT_TYPE=" + _query.HeadersMap.get("Content-Type"));
        } else {
            aEnvVarVector.add("REQUEST_METHOD=GET");
            if (iGetParameters != null && !iGetParameters.equals("")) aEnvVarVector.add("QUERY_STRING=" + iGetParameters);
        }
        return aEnvVarVector;
    }

    public static String extractPath(String iResource) {
        String aResource = iResource;
        if (iResource.contains("?")) {
            aResource = aResource.substring(0, aResource.indexOf("?"));
        }
        int aIndexLastSlash = aResource.lastIndexOf("/");
        if (aIndexLastSlash == -1) aResource = "/"; else aResource = aResource.substring(0, aIndexLastSlash + 1);
        return aResource;
    }

    public String getOverrideMimeType() {
        return "";
    }
}
