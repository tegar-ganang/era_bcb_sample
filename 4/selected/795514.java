package blauwahl.server.controller;

import blauwahl.server.controller.exception.CDatabaseException;
import blauwahl.server.controller.logging.CLogging;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.StreamConnection;
import java.io.ByteArrayOutputStream;
import blauwahl.server.model.*;

/**
 * CHandler manages the connection in 3 steps. The client starts an inquiry.
 * The incoming stream is separated character by character.
 * In second step the server is going to send the serialized question.
 * The third step contains the incoming answer stream from client to server.
 * This stream will also be separated.
 * This stream is deserialized and continued for processing.
 *
 * @author Marcus
 * @version 1.0
 * @since 1.0
 */
public class CHandler {

    private MAPacket nMAPacket = null;

    private MQuestion question;

    private int qID = 0;

    private int pID = 0;

    private long date = 0;

    /**
	 * Create a new CHandler
	 *
	 * @param con Represent the connection stream
	 * @param pID Poll ID
	 * @param qID Question ID
	 */
    public CHandler(StreamConnection con, int pID, int qID) {
        CLogging.simpleLog("handleClient");
        this.pID = pID;
        this.qID = qID;
        this.date = new java.util.Date().getTime();
        try {
            this.question = CDatabaseAdapter.getQuestion(this.qID);
        } catch (Exception ex) {
            CLogging.simpleLog("Exception: " + ex);
            CLogging.saveStackTrace(ex);
        }
        this.question.setDate(this.date);
        byte[] qBytes;
        String resultStr = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;
        OutputStream out1 = null;
        int read = -1;
        try {
            in = con.openInputStream();
        } catch (Exception ex) {
            CLogging.simpleLog("openInputStream failed: " + ex);
        }
        try {
            out = new ByteArrayOutputStream();
            while (((read = in.read()) != -1) && (read != 27)) {
                out.write(read);
            }
            resultStr = new String(out.toByteArray(), "UTF-8");
            CLogging.simpleLog("Request Client: \"" + resultStr + "\"");
        } catch (Exception ex) {
            CLogging.simpleLog("Evaluate incoming stream failed: " + ex);
        } finally {
            out = null;
        }
        if (resultStr.equals("GetQ")) {
            CLogging.simpleLog("Preparing QPacket");
            try {
                out1 = con.openOutputStream();
                qBytes = CBluetoothAdapter.serialize(this.getMQPacket());
                out1.write(qBytes);
                CLogging.simpleLog("MQPacket Stream: " + out1.toString());
                CLogging.simpleLog("MQPacket " + qBytes.toString() + " gesendet");
            } catch (Exception ex) {
                CLogging.simpleLog("Sending serialized MQPacket failed: " + ex);
            } finally {
                out1 = null;
            }
        } else if (resultStr.equals("GiveA")) {
            CLogging.simpleLog("Give A");
            try {
                out = new ByteArrayOutputStream();
                while (((read = in.read()) != -1) && (read != 27)) {
                    out.write(read);
                }
                resultStr = new String(out.toByteArray(), "UTF-8");
                CLogging.simpleLog("Content of clien stream: " + resultStr);
            } catch (Exception ex) {
                CLogging.simpleLog("Evaluate incoming stream failed: " + ex);
            } finally {
                out = null;
            }
            this.nMAPacket = CBluetoothAdapter.deserialize(resultStr);
            CLogging.simpleLog("Answer Packet: " + this.nMAPacket);
        } else {
            CLogging.simpleLog("Client sent a defective stream: " + resultStr);
        }
    }

    /**
	 * Get the answer packet of client
	 *
	 * @return MAPacket which includes information sent by client
	 */
    public MAPacket getMAPacket() {
        CLogging.simpleLog("getMAPacket: " + this.nMAPacket);
        return this.nMAPacket;
    }

    /**
	 * Create a new MQPacket for sending by CHandler.
	 * This MQPacket-object contains the poll iD, date and the question-object
	 *
	 * @return MQPacket-object
	 */
    public MQPacket getMQPacket() {
        try {
            MQPacket nMQPacket = new MQPacket();
            CLogging.simpleLog("HandlerFactory: New MQPacket");
            nMQPacket.setPID(this.pID);
            nMQPacket.setDate(this.date);
            CLogging.simpleLog("Content of question: " + this.question);
            nMQPacket.setQuestion(this.question);
            return nMQPacket;
        } catch (Exception ex) {
            CLogging.simpleLog("Create new MQPacket failed!");
            return null;
        }
    }
}
