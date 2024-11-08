package uk.ekiwi.messaging.mq;

import uk.ekiwi.messaging.mq.ui.FrmDifferences;
import com.ibm.mq.*;
import java.io.*;
import java.util.ArrayList;
import oracle.xml.differ.*;
import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import javax.swing.JInternalFrame;
import javax.swing.JTextPane;
import uk.ekiwi.messaging.*;
import uk.ekiwi.messaging.mq.ui.DataConvert;

public class MqUtil {

    private MqQueueManager qm = null;

    private MQQueueManager _queueManager = null;

    private MQQueue _browseQ = null;

    /**
    *
    * @param hostName String Name/IP address of machine where qm resides
    * @param qMgr String Queue manager
    * @param channel String Server connection channel defined in QM
    * @param port int QM port to connect
    * @throws MQException
    */
    public MqUtil(MqQueueManager qm) throws MQException {
        this.qm = qm;
        _queueManager = getMQQueueManager(qm);
    }

    private static MQQueueManager getMQQueueManager(MqQueueManager qm) throws MQException {
        if (!qm.getHostName().toLowerCase().equals("localhost")) {
            MQEnvironment.hostname = qm.getHostName();
        }
        MQEnvironment.channel = qm.getChannel();
        MQEnvironment.port = qm.getPort();
        return new MQQueueManager(qm.getQManager());
    }

    public static int copy(MqQueueManager srcQM, MqQueueManager destQM, String srcQueue, String destQueue, int count) throws MessagingException, IOException {
        int i = 0;
        MqSession srcSession = new MqSession(srcQM);
        MqSession destSession = new MqSession(destQM);
        QueueBrowser browser = srcSession.createBrowser(srcQueue, "");
        QueueSender sender = destSession.createSender(destQueue);
        try {
            for (i = 0; i < count; i++) {
                MqTextMessage msg = ((MqQueueBrowser) browser).browseNext();
                ((MqQueueSender) sender).send(msg);
            }
        } catch (MqMessagingException e) {
            if (e.getErrorCode() != 2033) {
                throw e;
            }
        } finally {
            try {
                srcSession.close();
                destSession.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            return i;
        }
    }

    public static int move(MqQueueManager srcQM, MqQueueManager destQM, String srcQueue, String destQueue, int count) throws MessagingException, IOException {
        int i = 0;
        MqSession srcSession = new MqSession(srcQM);
        MqSession destSession = new MqSession(destQM);
        MqQueueReceiver receiver = null;
        MqQueueSender sender = null;
        try {
            receiver = new MqQueueReceiver(srcSession, srcQueue);
            sender = new MqQueueSender(destSession, destQueue);
            for (i = 0; i < count; i++) {
                TextMessage msg = receiver.readMessage();
                sender.send(msg);
            }
        } catch (MQException e) {
            if (e.reasonCode != 2033) {
                throw e;
            }
        } finally {
            try {
                srcSession.close();
                destSession.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            return i;
        }
    }

    /**
  * Writes string message to specified queue. This closes and disconnects queue when finished.
  * @param qName String Output queue within connected QM
  * @param msg String TextMessage to output
  * @throws MQException
  * @throws IOException
  */
    public int saveMessages(String qName, String filename, int startPos, int maxCount, boolean bInclMQMD, boolean bInclHeaders) throws MQException, IOException, OptionalDataException, StreamCorruptedException, InvalidClassException, ClassNotFoundException, IOException {
        boolean isMore = true;
        int count = 0;
        int strlen = 0;
        String str = null;
        File file = null;
        FileOutputStream file_output = null;
        DataOutputStream data_out = null;
        file = new File(filename);
        file_output = new FileOutputStream(file);
        data_out = new DataOutputStream(file_output);
        byte[] result = null;
        int openOptions = MQC.MQOO_BROWSE;
        MQQueue queue = _queueManager.accessQueue(qName, openOptions, null, null, null);
        System.out.println("MQUtil connected");
        MQGetMessageOptions gmo = new MQGetMessageOptions();
        gmo.options = MQC.MQGMO_WAIT | MQC.MQGMO_BROWSE_NEXT;
        MQMessage msg = new MQMessage();
        try {
            for (int i = 0; i < startPos - 1; i++) {
                msg.clearMessage();
                msg.correlationId = MQC.MQCI_NONE;
                msg.messageId = MQC.MQMI_NONE;
                queue.get(msg, gmo);
            }
            while (isMore && (maxCount == -1 || count < maxCount)) {
                msg.clearMessage();
                msg.correlationId = MQC.MQCI_NONE;
                msg.messageId = MQC.MQMI_NONE;
                queue.get(msg, gmo);
                strlen = msg.getDataLength();
                if (bInclMQMD) {
                    result = new byte[strlen + 364];
                    byte[] mqmd = MQMD.getMQMDHeader(msg);
                    System.arraycopy(mqmd, 0, result, 0, mqmd.length);
                    msg.readFully(result, 364, strlen);
                } else {
                    result = new byte[strlen];
                    msg.readFully(result, 0, strlen);
                }
                if (!bInclHeaders) {
                    msg.setDataOffset(0);
                    String rfhStrucID = msg.readString(4);
                    if (rfhStrucID.equals("RFH ")) {
                        int rfhVersion = msg.readInt();
                        int rfhStrucLength = msg.readInt();
                        int rfhEncoding = msg.readInt();
                        int rfhCodedCharSetId = msg.readInt();
                        String rfhFormat = msg.readString(8);
                        int rfhFlags = msg.readInt();
                        int rfhNameValueCCSID = msg.readInt();
                        int offset = msg.getDataOffset();
                        offset = msg.skipBytes(rfhStrucLength - offset);
                        byte[] tmpBuffer = new byte[result.length - rfhStrucLength];
                        System.arraycopy(result, 0, tmpBuffer, 0, 364);
                        System.arraycopy(result, 364 + rfhStrucLength, tmpBuffer, 364, tmpBuffer.length - 364);
                        result = tmpBuffer;
                    }
                }
                data_out.write(result, 0, result.length);
                data_out.writeBytes("#@#@#");
                count++;
            }
        } catch (MQException e) {
            if (e.reasonCode != 2033) throw e; else isMore = false;
        } finally {
            data_out.flush();
            data_out.close();
            file_output.close();
            queue.close();
            _queueManager.disconnect();
        }
        return count;
    }

    public int loadQueue(String queueName, String filename, int startMsgPos, int maxCount, boolean bInclMQMD, boolean bInclHeaders) throws MQException, Exception {
        final int BUFFER_SIZE = 104000000;
        final String MSG_DELIMITER = "#@#@#";
        File file = null;
        byte[] buffer = null;
        byte[] tmpBuffer = null;
        String tmpStr = null;
        int startPos = 0, endPos = 0;
        int currentMsgPos = 0;
        MQMessage msg = new MQMessage();
        int msgCount = 0;
        FileInputStream file_input = null;
        int length = 0;
        boolean isMore = true;
        try {
            int openOptions = MQC.MQOO_OUTPUT;
            MQQueue queue = _queueManager.accessQueue(queueName, openOptions, null, null, null);
            file = new File(filename);
            if (!file.exists()) {
                throw new Exception("Invalid filename entered");
            }
            file_input = new FileInputStream(file);
            long fileLength = file.length();
            if (fileLength > Integer.MAX_VALUE) {
                throw new Exception("File is too large");
            }
            if (fileLength < BUFFER_SIZE) buffer = new byte[(int) fileLength]; else buffer = new byte[BUFFER_SIZE];
            int offset = 0;
            int bytesRead = file_input.read(buffer, 0, buffer.length);
            offset += bytesRead;
            while (isMore && startPos < fileLength) {
                endPos = DataConvert.findBytesPosInByteArray(buffer, startPos, MSG_DELIMITER.getBytes());
                if (endPos < 1) {
                    if (offset < fileLength) {
                        tmpBuffer = buffer;
                        buffer = new byte[BUFFER_SIZE];
                        System.arraycopy(tmpBuffer, startPos, buffer, 0, buffer.length - startPos);
                        bytesRead = file_input.read(buffer, buffer.length - startPos, buffer.length - (buffer.length - startPos));
                        length = buffer.length - startPos + bytesRead;
                        offset += bytesRead;
                        startPos = 0;
                        endPos = DataConvert.findBytesPosInByteArray(buffer, startPos, MSG_DELIMITER.getBytes());
                        if (endPos < 1) {
                            endPos = buffer.length;
                            isMore = false;
                        }
                    } else {
                        endPos = buffer.length;
                        isMore = false;
                    }
                }
                tmpBuffer = new byte[endPos - startPos];
                System.arraycopy(buffer, startPos, tmpBuffer, 0, tmpBuffer.length);
                MqTextMessage mqmsg = new MqTextMessage(tmpBuffer);
                queue.put(mqmsg.getMQMessage());
                msgCount++;
                tmpBuffer = null;
                System.gc();
                startPos = endPos + MSG_DELIMITER.length();
            }
        } finally {
            if (file_input != null) file_input.close();
        }
        return msgCount;
    }

    public String DisplayRFH2(MQMessage gotMessage) throws IOException {
        String rfhStrucID = gotMessage.readString(4);
        int rfhVersion = gotMessage.readInt();
        int rfhStrucLength = gotMessage.readInt();
        int rfhEncoding = gotMessage.readInt();
        int rfhCodedCharSetId = gotMessage.readInt();
        String rfhFormat = gotMessage.readString(8);
        int rfhFlags = gotMessage.readInt();
        int rfhNameValueCCSID = gotMessage.readInt();
        gotMessage.characterSet = rfhNameValueCCSID;
        int hdrOffset = gotMessage.getDataOffset();
        {
            hdrOffset = gotMessage.skipBytes(rfhStrucLength - hdrOffset);
        }
        gotMessage.encoding = rfhEncoding;
        gotMessage.characterSet = rfhCodedCharSetId;
        return rfhFormat;
    }

    public void close() throws Exception {
        if (_browseQ != null) {
            if (_browseQ.isOpen()) {
                _browseQ.close();
            }
        }
        if (_queueManager.isConnected()) _queueManager.disconnect();
        _queueManager = null;
    }

    protected void finalize() throws Throwable {
        close();
    }

    public static int compareXmlQueues(MqQueueManager srcQM, MqQueueManager destQM, String srcQueue, String destQueue, JInternalFrame frm) throws Exception {
        int result = 0;
        int i = 0;
        boolean isDifferent = false;
        ArrayList<JTextPane> srcDifferences = new ArrayList<JTextPane>();
        ArrayList<JTextPane> destDifferences = new ArrayList<JTextPane>();
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        MqSession srcSession = new MqSession(srcQM);
        MqSession destSession = new MqSession(destQM);
        MqQueueBrowser srcQBrowser = srcSession.createBrowser(srcQueue, "");
        MqQueueBrowser destQBrowser = destSession.createBrowser(destQueue, "");
        MqTextMessage srcMsg = srcQBrowser.browseFirst();
        MqTextMessage destMsg = destQBrowser.browseFirst();
        for (i = 0; srcMsg != null && destMsg != null; i++) {
            String srcString = srcMsg.getText().trim();
            String destString = destMsg.getText().trim();
            ByteArrayInputStream srcContent = new ByteArrayInputStream(srcString.getBytes());
            ByteArrayInputStream destContent = new ByteArrayInputStream(destString.getBytes());
            DOMParser parser = new DOMParser();
            parser.setDebugMode(true);
            parser.showWarnings(true);
            parser.parse(srcContent);
            XMLDocument xmlSrcDocument = parser.getDocument();
            parser.parse(destContent);
            XMLDocument xmlDestDocument = parser.getDocument();
            oracle.xml.differ.XMLDiff xmlDiff = new XMLDiff();
            xmlDiff.setDocuments(xmlSrcDocument, xmlDestDocument);
            if (xmlDiff.diff()) {
                isDifferent = true;
                indexes.add(new Integer(i));
                srcDifferences.add(xmlDiff.getDiffPane1());
                destDifferences.add(xmlDiff.getDiffPane2());
            }
            srcMsg = srcQBrowser.browseNext();
            destMsg = destQBrowser.browseNext();
        }
        if (isDifferent) {
            FrmDifferences f = new FrmDifferences(indexes, srcDifferences, destDifferences, i);
            frm.getParent().add(f);
            f.setVisible(true);
        }
        if (isDifferent) return -1;
        return i;
    }

    public static int compareQueues(MqQueueManager srcQM, MqQueueManager destQM, String srcQueue, String destQueue, JInternalFrame frm) throws Exception {
        int result = 0;
        int i = 0;
        boolean isDifferent = false;
        ArrayList<JTextPane> srcDifferences = new ArrayList<JTextPane>();
        ArrayList<JTextPane> destDifferences = new ArrayList<JTextPane>();
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        MqSession srcSession = new MqSession(srcQM);
        MqSession destSession = new MqSession(destQM);
        MqQueueBrowser srcQBrowser = srcSession.createBrowser(srcQueue, "");
        MqQueueBrowser destQBrowser = destSession.createBrowser(destQueue, "");
        MqTextMessage srcMsg = srcQBrowser.browseFirst();
        MqTextMessage destMsg = destQBrowser.browseFirst();
        for (i = 0; srcMsg != null && destMsg != null; i++) {
            String srcString = srcMsg.getText().trim();
            String destString = destMsg.getText().trim();
            if (!srcString.equals(destString)) {
                isDifferent = true;
                indexes.add(new Integer(i));
                JTextPane src = new JTextPane();
                src.setText(srcString);
                srcDifferences.add(src);
                JTextPane dest = new JTextPane();
                dest.setText(destString);
                destDifferences.add(dest);
            }
            srcMsg = srcQBrowser.browseNext();
            destMsg = destQBrowser.browseNext();
        }
        if (isDifferent) {
            FrmDifferences f = new FrmDifferences(indexes, srcDifferences, destDifferences, i);
            frm.getParent().add(f);
            f.setVisible(true);
        }
        if (isDifferent) return -1;
        return i;
    }
}
