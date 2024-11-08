package org.xsocket.connection;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;

/**
*
* @author grro@xsocket.org
*/
public final class DeleteMeTest {

    @Test
    public void testSimple() throws Exception {
        HttpServlet servlet = new HttpServlet() {

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().write("test");
            }
        };
        WebContainer webContainer = new WebContainer(servlet);
        webContainer.start();
        NonBlockingConnectionPool pool = new NonBlockingConnectionPool();
        for (int i = 0; i < 2; i++) {
            INonBlockingConnection con = pool.getNonBlockingConnection("localhost", webContainer.getLocalPort());
            con.write("GET /de/themen/unterhaltung/bildergalerien/8114728,image=5.html HTTP/1.1\r\n" + "Host: magazine.web.de\r\n" + "User-agent: me\r\n\r\n");
            QAUtil.sleep(1000);
            String header = con.readStringByDelimiter("\r\n\r\n");
            con.readStringByLength(4);
            Assert.assertEquals(0, con.available());
            Assert.assertTrue(header.indexOf("200") != -1);
            con.close();
        }
        pool.close();
        webContainer.stop();
    }

    private static final class Handler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException {
            connection.write(connection.readBytesByLength(connection.available()));
            return true;
        }
    }
}
