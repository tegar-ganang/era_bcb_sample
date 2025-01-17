package org.damour.base.server;

import java.io.InputStream;
import java.net.URL;
import org.junit.Test;

public class ServletPounderTest {

    @Test
    public void poundServlet() {
        for (int i = 0; i < 10; i++) {
            try {
                Thread t = new Thread(new Runnable() {

                    public void run() {
                        try {
                            URL url = new URL("http://localhost:8888/servlet/GetFileService?id=9");
                            InputStream is = url.openConnection().getInputStream();
                            is.close();
                        } catch (Throwable t) {
                        }
                    }
                });
                t.start();
            } catch (Throwable t) {
            }
        }
    }
}
