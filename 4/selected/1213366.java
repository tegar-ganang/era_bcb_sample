package Network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.bluetooth.BluetoothStateException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/**
 * This class handles with a given Connection the communication.
 *
 * @author Steffen
 */
public class ConnectionHandler implements Runnable {

    /**
	 * NetworkDispatcher instance to be able to message
	 */
    private NetworkDispatcher networkDispatcher;

    private String qPlusA;

    /**
	 * serialized answer
	 */
    private String answer;

    /**
	 * connection that is used to communicate
	 */
    private StreamConnection connection;

    /**
	 * task determines the kind of communication
	 */
    private int task;

    /**
	 * Task to be server
	 */
    public static final int TASK_BESERVER = 1;

    /**
	 * Task to get question/answers
	 */
    public static final int TASK_GETQ = 2;

    /**
	 * Task to send given answer
	 */
    public static final int TASK_SENDA = 3;

    /**
	 * Our own EndofStream sign, because the End of stream (-1) of Java's streams mostly don't come
	 */
    private final char EndOfStream = 27;

    /**
	 * InputStream used to receive
	 */
    private InputStream in;

    /**
	 * OutputStream used to send
	 */
    private OutputStream out;

    /**
	 * our protocol level, that we have used to communicate
	 */
    private int usedLevel;

    /**
	 * Initializes all neccessary parameters to communicate.
	 *
	 * @param con StreamConnection that is used to communicate
	 * @param task Integer that represents the kind of communication
	 * @param qa either the question/answers or the given answer, corresponding to task
	 * @param nd NetworkDispatcher instance to message
	 */
    public ConnectionHandler(StreamConnection con, int task, String qa, NetworkDispatcher nd) {
        connection = con;
        this.task = task;
        if (task == TASK_BESERVER) {
            qPlusA = qa;
        } else if (task == TASK_SENDA) {
            answer = qa;
        } else if (task == TASK_GETQ) {
            qPlusA = null;
        } else {
        }
        networkDispatcher = nd;
    }

    /**
	 * General method to receive a string through the connection.
	 * Uses our special End of Stream sign.
	 * @return received string
	 * @throws IOException if something does not work with the connection.
	 */
    public String receive() throws IOException {
        if (in == null) {
            in = connection.openInputStream();
        }
        message("Receiving...");
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        int read;
        while ((read = in.read()) != -1 && read != EndOfStream) {
            out1.write(read);
        }
        String result = new String(out1.toByteArray(), "UTF-8");
        out1.close();
        message("\"" + result + "\" empfangen");
        System.out.println("\"" + result + "\" empfangen");
        return result;
    }

    /**
	 * General method to send a string through the connection.
	 * Extends the given String with our End of Stream sign.
	 * @param msg String to send
	 * @throws IOException if something does not work with the connection.
	 */
    public void send(String msg) throws IOException {
        if (out == null) {
            out = connection.openOutputStream();
            out.flush();
        }
        byte[] b = (msg + EndOfStream).getBytes("UTF-8");
        out.write(b);
        message(msg + " gesendet");
        System.out.println(msg + " gesendet");
    }

    /**
	 * Returns serialized qPlusA string.
	 * @return serialized qPlusA string
	 */
    public String getqPlusA() {
        return qPlusA;
    }

    /**
	 * Triggers the message method of NetworkDispatcher.
	 * @param msg message to show
	 */
    public void message(String msg) {
        networkDispatcher.message(msg);
    }

    /**
	 * Triggers the message method of NetworkDispatcher.
	 * @param msg
	 */
    public void message2(String msg) {
        networkDispatcher.message2(msg);
    }

    /**
	 * Returns the protocol level with was used to communicate.
	 * @return the protocol level with was used to communicate
	 */
    public int getUsedLevel() {
        return usedLevel;
    }

    /**
	 * Execute the task that was set in the constructor.
	 * <ul>
	 *		<li>
	 *			TASK_BESERVER
	 *			<ul>
	 *				<li>receive the task (GetQ or GiveA)</li>
	 *				<li>GetQ - send the serialized string</li>
	 *				<li>GiveA - receive the answer string and try to forward it</li>
	 *			</ul>
	 *		</li>
	 *		<li>TASK_SENDA - send the answer</li>
	 *		<li>TASK_GETQ - receive the question/answers</li>
	 * </ul>
	 */
    public void run() {
        if (task == TASK_BESERVER) {
            String forward_answer;
            if (connection == null) {
                return;
            }
            String task;
            try {
                task = receive();
            } catch (IOException e) {
                message(e + " jeschmissen beim empfangen");
                System.out.println(e + " jeschmissen beim empfangen");
                return;
            }
            message("Task: \"" + task + "\"");
            if (task.equals("GetQ")) {
                message2("Leite Frage weiter.");
                try {
                    if (in != null) {
                        in.close();
                    }
                    send(qPlusA);
                } catch (IOException e) {
                    message(e + " jeschmissen beim senden");
                    System.out.println(e + " jeschmissen beim senden");
                    return;
                }
            } else if (task.equals("GiveA")) {
                message2("Empfange weiterzuleitende Antwort.");
                try {
                    forward_answer = receive();
                    message("Answer to forward: " + forward_answer);
                } catch (IOException e) {
                    message(e + " jeschmissen beim empfangen");
                    System.out.println(e + " jeschmissen beim empfangen");
                    return;
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (IOException e) {
                }
                connection = null;
                in = null;
                out = null;
                message2("Leite Antwort weiter.");
                NetworkDiscoverer discoverer;
                try {
                    discoverer = new NetworkDiscoverer(networkDispatcher);
                } catch (BluetoothStateException e) {
                    return;
                }
                StreamConnection newcon = null;
                try {
                    message("Try to use last connection");
                    String lastURL = networkDispatcher.getUsedURL();
                    if (lastURL != null && !lastURL.equals("")) {
                        newcon = (StreamConnection) Connector.open(lastURL, Connector.READ_WRITE, true);
                    }
                } catch (IOException e) {
                    newcon = null;
                }
                NetworkServiceEntry se = null;
                if (newcon == null) {
                    message("Con = null, suche neu");
                    discoverer.findServices(NetworkDispatcher.getClientUUIDs());
                    se = discoverer.bestService();
                    if (se == null) {
                        message("No matching Service found, is there a poll running? " + "if yes, try again please");
                        System.out.println("No matching Service found, is there a poll running? " + "if yes, try again please");
                        return;
                    }
                    newcon = se.getConnection();
                }
                ConnectionHandler handler = new ConnectionHandler(newcon, TASK_SENDA, forward_answer, networkDispatcher);
                handler.run();
            } else {
            }
        } else if (task == TASK_SENDA) {
            try {
                send("GiveA");
                send(answer);
                message("Answer written");
                System.out.println("Answer written");
                message2("Antwort gesendet.");
            } catch (IOException e) {
                message(e + " jeschmissen beim senden");
                System.out.println(e + " jeschmissen beim senden");
                return;
            }
        } else if (task == TASK_GETQ) {
            try {
                send("GetQ");
                if (out != null) {
                    out.close();
                }
                message("Warte uff de Frage");
                System.out.println("Warte uff de Frage");
                qPlusA = receive();
            } catch (IOException e) {
                message(e + " jeschmissen beim senden");
                System.out.println(e + " jeschmissen beim senden");
            }
        }
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
        }
        connection = null;
        in = null;
        out = null;
    }
}
