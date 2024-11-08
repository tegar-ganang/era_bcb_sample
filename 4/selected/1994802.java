package gumbo.net.test.awt;

import gumbo.app.awt.MainWindow;
import gumbo.core.util.AssertUtils;
import gumbo.json.JsonMessage;
import gumbo.json.JsonMessageReader;
import gumbo.json.JsonMessageWriter;
import gumbo.net.NetThreadPrinter;
import gumbo.net.ServerThread;
import gumbo.net.java.JavaMessageReader;
import gumbo.net.java.JavaMessageWriter;
import gumbo.net.msg.MessageIOPrinter;
import gumbo.net.msg.MessageIOReader;
import gumbo.net.msg.MessageUtils;
import gumbo.net.msg.MessageIOWriter;
import gumbo.net.test.awt.util.TestBrokerType;
import gumbo.net.test.awt.util.TestMessageType;
import gumbo.net.test.awt.util.TestNetUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * TODO: Out of date with latest net framework.
 * <p>
 * Test server without a GUI.
 * @author Jon Barrilleaux (jonb@jmbaai.com) of JMB and Associates Inc.
 */
public class TestNoGuiServerApp {

    /**
	 * @param serverPort Server port number.
	 * @param writer All status will be sent to this writer. Null if none.
	 * @param msgType Message transport type. Never null.
	 * @param sockType Socket transport type. Never null.
	 * @param isBlocking If true, does not return until thread is done.
	 */
    public TestNoGuiServerApp(int serverPort, PrintWriter writer, TestMessageType msgType, TestBrokerType sockType, boolean isBlocking) {
        switch(sockType) {
            case PERS:
                _type = MessageUtils.SocketType.PERSISTANT;
                break;
            case TRANS:
                _type = MessageUtils.SocketType.TRANSIENT;
                break;
            default:
                AssertUtils.throwIllegalArgument(this, "Bad socket type. type=" + sockType);
        }
        _messagePrinter = new MessageIOPrinter(APP_NAME, writer);
        switch(msgType) {
            case JAVA:
                _thread = newJavaThread(serverPort, _messagePrinter);
                break;
            case JSON:
                _thread = newJsonThread(serverPort, _messagePrinter);
                break;
        }
        _threadPrinter = new NetThreadPrinter(APP_NAME, writer);
        _thread.addNetThreadListener(_threadPrinter);
        if (isBlocking) {
            _thread.run();
        } else {
            _thread.start();
        }
    }

    private ServerThread<Object> newJavaThread(int serverPort, final MessageIOPrinter printer) {
        return new ServerThread<Object>(serverPort) {

            protected MessageIOReader<Object> newMessageReader(Socket socket) throws IOException {
                MessageIOReader<Object> reader = new JavaMessageReader(socket, _type);
                reader.addMessageIOListener(printer);
                return reader;
            }

            protected MessageIOWriter<Object> newMessageWriter(Socket socket) throws IOException {
                MessageIOWriter<Object> writer = new JavaMessageWriter(socket, _type);
                writer.addMessageIOListener(printer);
                return writer;
            }
        };
    }

    private ServerThread<JsonMessage> newJsonThread(int serverPort, final MessageIOPrinter printer) {
        return new ServerThread<JsonMessage>(serverPort) {

            protected MessageIOReader<JsonMessage> newMessageReader(Socket socket) throws IOException {
                MessageIOReader<JsonMessage> reader = new JsonMessageReader(socket, _type);
                reader.addMessageIOListener(printer);
                return reader;
            }

            protected MessageIOWriter<JsonMessage> newMessageWriter(Socket socket) throws IOException {
                MessageIOWriter<JsonMessage> writer = new JsonMessageWriter(socket, _type);
                writer.addMessageIOListener(printer);
                return writer;
            }
        };
    }

    private MessageUtils.SocketType _type;

    private ServerThread<?> _thread;

    private NetThreadPrinter _threadPrinter;

    private MessageIOPrinter _messagePrinter;

    public static final String APP_NAME = "TestNoGuiServer";

    public static void main(String args[]) {
        int serverPort = TestNetUtils.DEFAULT_SERVER_PORT;
        TestMessageType msgType = TestMessageType.JAVA;
        TestBrokerType sockType = TestBrokerType.PERS;
        try {
            try {
                int argi = 0;
                if (argi < args.length) {
                    String argFlag = args[argi];
                    TestMessageType type = TestMessageType.fromArgFlag(argFlag);
                    if (type != null) {
                        msgType = type;
                        argi++;
                    }
                }
                if (argi < args.length) {
                    String argFlag = args[argi];
                    TestBrokerType type = TestBrokerType.fromArgFlag(argFlag);
                    if (type != null) {
                        sockType = type;
                        argi++;
                    }
                }
                if (argi < args.length) serverPort = Integer.parseInt(args[argi]);
                argi++;
                if (args.length > argi) throw new IllegalArgumentException("Too many arguments.");
            } catch (Exception ex) {
                throw new IllegalArgumentException("usage: " + APP_NAME + " [-java|-json] [-pers|-trans] [server_port_number]\n" + ex);
            }
            PrintWriter printer = new PrintWriter(System.out, true);
            new TestNoGuiServerApp(serverPort, printer, msgType, sockType, true);
        } catch (Throwable th) {
            MainWindow.showProblemMessage(APP_NAME, null, null, th);
        }
        System.exit(0);
    }
}
