package com.fourspaces.scratch.result;

import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fourspaces.scratch.mapping.ControllerMappingPath;

public class InputStreamResult extends Result {

    private final String contentType;

    protected InputStream inputStream = null;

    protected HttpServletRequest request = null;

    public InputStreamResult(InputStream is) {
        this.inputStream = is;
        this.contentType = "application/octet-stream";
    }

    public InputStreamResult(InputStream is, String contentType) {
        this.inputStream = is;
        this.contentType = contentType;
    }

    protected InputStreamResult(String contentType) {
        super();
        this.contentType = contentType;
    }

    protected InputStreamResult() {
        super();
        this.contentType = "application/octet-stream";
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    protected void process(HttpServletRequest request, HttpServletResponse response, ControllerMappingPath mappedPath, boolean contextRelativePaths) throws ResultException {
        this.request = request;
        response.setContentType(contentType);
        InputStream in = getInputStream();
        OutputStream out = null;
        try {
            if (in != null) {
                out = response.getOutputStream();
                if (in != null && out != null) {
                    int read = 0;
                    byte[] buffer = new byte[16 * 1024];
                    while (read > -1) {
                        read = in.read(buffer);
                        if (read > -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                    in.close();
                    out.close();
                }
            } else {
                throw new ResultException("InputStream was null");
            }
        } catch (Exception e) {
            log.error(e);
            throw new ResultException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
