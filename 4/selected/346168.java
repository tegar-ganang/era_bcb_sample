package org.ugr.bluerose.test;

import java.util.Vector;
import org.ugr.bluerose.ByteStreamReader;
import org.ugr.bluerose.Initializer;
import org.ugr.bluerose.MethodResult;
import org.ugr.bluerose.ObjectServant;
import org.ugr.bluerose.devices.TcpCompatibleDevice;
import org.ugr.bluerose.events.Event;
import org.ugr.bluerose.messages.MessageHeader;

public class TestServer extends ObjectServant {

    int add(int x, int y) {
        return x + y;
    }

    void print(String str) {
        System.out.println(str);
    }

    void event_test(Event evt) {
        System.out.println("Event received: " + evt.topic + "  ---> (" + evt.getMemberValue("x").getFloat() + ", " + evt.getMemberValue("y").getFloat() + ")");
    }

    public static void main(String[] args) {
        TcpCompatibleDevice device = new TcpCompatibleDevice();
        TestServer servant = new TestServer();
        try {
            Initializer.initialize(new java.io.File(Test.class.getResource("/bluerose_config.xml").getPath()));
            Initializer.initializeServant(servant, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
        device.waitForConnections();
        Initializer.destroy();
    }

    /**
	 * DO NOT MODIFY THIS
	 */
    public TestServer() {
        identity.id_name = "TestServant";
        identity.category = "Test";
    }

    @Override
    public MethodResult runMethod(String method, String userID, Vector<Byte> args) {
        ByteStreamReader reader = new ByteStreamReader(args);
        MethodResult result = new MethodResult();
        if (method.equals("add")) {
            synchronized (mutex) {
                writer.writeInteger(add(reader.readInteger(), reader.readInteger()));
                result.status = MessageHeader.SUCCESS_STATUS;
                result.result = writer.toVector();
                writer.reset();
            }
            return result;
        } else if (method.equals("print")) {
            String res = reader.readUTF8String();
            print(res);
            result.status = MessageHeader.SUCCESS_STATUS;
            return result;
        } else if (method.equals("event_test")) {
            Event evt = new Event();
            evt.unmarshall(reader);
            event_test(evt);
            result.status = MessageHeader.SUCCESS_STATUS;
            return result;
        }
        result.status = MessageHeader.OPERATION_NOT_EXIST_STATUS;
        return result;
    }
}
