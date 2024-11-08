package org.xsocket.stream.management;

import java.io.IOException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.JmxServer;
import org.xsocket.TestUtil;
import org.xsocket.stream.BlockingConnection;
import org.xsocket.stream.IBlockingConnection;
import org.xsocket.stream.IDataHandler;
import org.xsocket.stream.INonBlockingConnection;
import org.xsocket.stream.MultithreadedServer;

/**
*
* @author grro@xsocket.org
*/
public final class ManagementTest {

    @Test
    public void testSimple() throws Exception {
        int port = 9922;
        JmxServer jmxServer = new JmxServer();
        JMXServiceURL url = jmxServer.start("testmanagement", port);
        MultithreadedServer server = new MultithreadedServer(new ServerHandler());
        new Thread(server).start();
        MultithreadedServerMBeanProxyFactory.createAndRegister(server, "test");
        JMXConnector connector = JMXConnectorFactory.connect(url);
        server.setReceiveBufferPreallocationSize(555);
        TestUtil.sleep(100);
        ObjectName serverObjectName = new ObjectName("test" + ":type=MultithreadedServer,name=" + server.getLocalAddress().getHostName() + "." + server.getLocalPort());
        Assert.assertNotNull(connector.getMBeanServerConnection().getObjectInstance(serverObjectName));
        Assert.assertTrue(555 == (Integer) connector.getMBeanServerConnection().getAttribute(serverObjectName, "ReceiveBufferPreallocationSize"));
        server.setReceiveBufferPreallocationSize(6666);
        TestUtil.sleep(100);
        Assert.assertTrue(6666 == (Integer) connector.getMBeanServerConnection().getAttribute(serverObjectName, "ReceiveBufferPreallocationSize"));
        server.setDispatcherPoolSize(1);
        server.setReceiveBufferPreallocationSize(90000);
        IBlockingConnection connection = new BlockingConnection(server.getLocalAddress(), server.getLocalPort());
        connection.write("rtrt");
        connection.flush();
        connection.close();
        TestUtil.sleep(100);
        connector.close();
        jmxServer.stop();
    }

    private static class ServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException {
            connection.write(connection.readAvailable());
            return true;
        }
    }
}
