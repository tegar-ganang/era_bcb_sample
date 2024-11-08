package com.od.jtimeseries.net.httpd.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
* Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 20/12/11
* Time: 19:32
*/
public class InputStreamResponse extends NoCacheResponse {

    private InputStream data;

    public InputStreamResponse(String status, String mimeType, InputStream data) {
        super(status, mimeType);
        this.data = data;
    }

    public void writeResponseBody(OutputStream out, PrintWriter pw) {
        if (data != null) {
            try {
                byte[] buff = new byte[2048];
                while (true) {
                    int read = data.read(buff, 0, 2048);
                    if (read <= 0) break;
                    out.write(buff, 0, read);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    data.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
