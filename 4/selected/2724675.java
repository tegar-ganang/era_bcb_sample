package org.xsocket.stream;

import java.io.IOException;
import java.net.InetAddress;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.JmxServer;
import org.xsocket.QAUtil;
import org.xsocket.stream.IDataHandler;
import org.xsocket.stream.INonBlockingConnection;
import org.xsocket.stream.MultithreadedServer;
import org.xsocket.stream.MultithreadedServerMBeanProxyFactory;
import org.xsocket.stream.StreamUtils;

/**
*
* @author grro@xsocket.org
*/
public final class ManagementTest {

    @Test
    public void testSimple() throws Exception {
        int port = 9929;
        JmxServer jmxServer = new JmxServer();
        JMXServiceURL url = jmxServer.start("testmanagement", port);
        MultithreadedServer server = new MultithreadedServer(InetAddress.getLocalHost(), 9988, new ServerHandler());
        StreamUtils.start(server);
        MultithreadedServerMBeanProxyFactory.createAndRegister(server, "test");
        JMXConnector connector = JMXConnectorFactory.connect(url);
        IBlockingConnection connection = new BlockingConnection(server.getLocalAddress(), server.getLocalPort());
        connection.write("helo");
        server.setIdleTimeoutSec(60);
        QAUtil.sleep(100);
        ObjectName serverObjectName = new ObjectName("test" + ":type=MultithreadedServer,name=" + server.getLocalAddress().getHostName() + "." + server.getLocalPort());
        Assert.assertNotNull(connector.getMBeanServerConnection().getObjectInstance(serverObjectName));
        Assert.assertTrue(60 == (Integer) connector.getMBeanServerConnection().getAttribute(serverObjectName, "IdleTimeoutSec"));
        server.setIdleTimeoutSec(90);
        QAUtil.sleep(100);
        Assert.assertTrue(90 == (Integer) connector.getMBeanServerConnection().getAttribute(serverObjectName, "IdleTimeoutSec"));
        connector.close();
        connection.close();
        server.close();
        jmxServer.stop();
    }

    private static class ServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException {
            connection.write(connection.readAvailable());
            return true;
        }
    }
}
