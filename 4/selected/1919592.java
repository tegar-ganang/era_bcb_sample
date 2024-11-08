package com.ericsson.xsmp.service.alert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.smslib.ICallNotification;
import org.smslib.IInboundMessageNotification;
import org.smslib.InboundBinaryMessage;
import org.smslib.InboundMessage;
import org.smslib.OutboundBinaryMessage;
import org.smslib.OutboundMessage;
import org.smslib.Service;
import org.smslib.StatusReportMessage;
import org.smslib.UnknownMessage;
import org.smslib.Message.MessageEncodings;
import org.smslib.Message.MessageTypes;
import org.smslib.modem.SerialModemGateway;
import org.spark.util.FileUtil;
import org.spark.util.ThreadUtil;
import org.springframework.beans.factory.InitializingBean;
import com.ericsson.xsmp.service.cdr.CDRService;
import com.ericsson.xsmp.service.cdr.Record;
import com.ericsson.xsmp.util.DateUtil;
import edu.emory.mathcs.backport.java.util.concurrent.Semaphore;

public class PduSmsAlertHandler implements InitializingBean, AlertHandler {

    private static Log LOG = LogFactory.getLog(PduSmsAlertHandler.class);

    Service pduService;

    String port;

    long idleTime = 3000;

    int baud = 9600;

    String manufacturer;

    String model;

    String simPin = null;

    boolean dropAfterRead = true;

    boolean checkUnReadOnly = false;

    CDRService cdrService;

    AtomicInteger idSeed = new AtomicInteger();

    AtomicInteger refNoSeed = new AtomicInteger();

    String storgePath;

    Semaphore lock = new Semaphore(1);

    Map<String, int[]> outboundStatuses = new HashMap<String, int[]>();

    Map<String, String[][]> outboundMessages = new HashMap<String, String[][]>();

    public String getStorgePath() {
        return storgePath;
    }

    public void setStorgePath(String storgePath) {
        this.storgePath = storgePath;
    }

    public long getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(long idleTime) {
        this.idleTime = idleTime;
    }

    public CDRService getCdrService() {
        return cdrService;
    }

    public void setCdrService(CDRService cdrService) {
        this.cdrService = cdrService;
    }

    public boolean isCheckUnReadOnly() {
        return checkUnReadOnly;
    }

    public void setCheckUnReadOnly(boolean checkUnReadOnly) {
        this.checkUnReadOnly = checkUnReadOnly;
    }

    public boolean isDropAfterRead() {
        return dropAfterRead;
    }

    public void setDropAfterRead(boolean dropAfterRead) {
        this.dropAfterRead = dropAfterRead;
    }

    public int getBaud() {
        return baud;
    }

    public void setBaud(int baud) {
        this.baud = baud;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getSimPin() {
        return simPin;
    }

    public void setSimPin(String simPin) {
        this.simPin = simPin;
    }

    public void afterPropertiesSet() throws Exception {
        pduService = new Service();
        SerialModemGateway gateway = new SerialModemGateway("modem." + port, port, baud, manufacturer, model);
        gateway.setInbound(true);
        gateway.setOutbound(true);
        if (simPin != null) gateway.setSimPin(simPin);
        pduService.setCallNotification(new CallNotification());
        pduService.setInboundNotification(new InboundNotification());
        pduService.addGateway(gateway);
        pduService.startService();
        LOG.info("Manufacturer: " + gateway.getManufacturer());
        LOG.info("Model: " + gateway.getModel());
        LOG.info("Serial No: " + gateway.getSerialNo());
        LOG.info("SIM IMSI: " + gateway.getImsi());
        LOG.info("Signal Level: " + gateway.getSignalLevel());
        LOG.info("Battery Level: " + gateway.getBatteryLevel());
        FileUtil.mkdirs(storgePath);
        String temp = FileUtil.getFileText(storgePath + "/idseed");
        if (temp != null && temp.length() > 0) idSeed.set(Integer.parseInt(temp.trim()));
    }

    public void process(AlertTask alertTask) throws Exception {
        String mobile = alertTask.getParameter("receiver");
        String mode = alertTask.getParameter("mode");
        OutboundMessage message = null;
        if ("1".equalsIgnoreCase(mode)) {
            byte[] buffer = defalte(alertTask.getMessage());
            int pduSize = 132;
            int total = buffer.length / pduSize;
            if (total * pduSize < buffer.length) total++;
            int newMsgId = nextMsgId();
            for (int i = 0; i < total; i++) {
                int from = i * pduSize;
                int length = (i == total - 1) ? (buffer.length - from) : pduSize;
                ByteBuffer tempbuf = ByteBuffer.allocate(140);
                tempbuf.putInt(newMsgId);
                tempbuf.putShort((short) total);
                tempbuf.putShort((short) i);
                tempbuf.put(buffer, from, length);
                byte[] temparr = new byte[tempbuf.position()];
                tempbuf.position(0);
                tempbuf.get(temparr);
                tempbuf.clear();
                message = new OutboundBinaryMessage(mobile, temparr);
                int refNo = refNoSeed.incrementAndGet();
                message.setRefNo("" + (refNo % 256));
                message.setStatusReport(true);
                LOG.info("Ready to deliver MTB to " + mobile + ",MsgId is " + newMsgId + "-" + total + "-" + i);
                try {
                    lock.acquire();
                    pduService.sendMessage(message);
                } finally {
                    lock.release();
                }
                LOG.info("Deliver message MTB to " + mobile + " completed. Status:" + message.getMessageStatus());
                Record cdrRecord = new Record();
                cdrRecord.addField("type", "MTB");
                cdrRecord.addField("receiver", mobile);
                cdrRecord.addField("sndtime", DateUtil.dateToString(message.getDate(), "yyyyMMddhhmmss"));
                cdrRecord.addField("status", message.getMessageStatus() == null ? "" : message.getMessageStatus().toString());
                cdrRecord.addField("originid", message.getRefNo());
                cdrRecord.addField("message", "PDU message #" + i + " of " + total);
                cdrService.createCDR(cdrRecord);
                ThreadUtil.sleep(idleTime);
            }
            Record cdrRecord = new Record();
            cdrRecord.addField("type", "MT");
            cdrRecord.addField("receiver", mobile);
            cdrRecord.addField("sndtime", DateUtil.dateToString(message.getDate(), "yyyyMMddhhmmss"));
            cdrRecord.addField("status", message.getMessageStatus() == null ? "" : message.getMessageStatus().toString());
            cdrRecord.addField("originid", message.getRefNo());
            cdrRecord.addField("message", alertTask.getMessage().replace("\r\n", " ").replace("\n", " "));
            cdrService.createCDR(cdrRecord);
        } else {
            message = new OutboundMessage(mobile, alertTask.getMessage());
            message.setEncoding(MessageEncodings.ENCUCS2);
            message.setStatusReport(true);
            LOG.info("Ready to deliver to " + mobile + ",Message is " + alertTask.getMessage());
            try {
                lock.acquire();
                pduService.sendMessage(message);
            } finally {
                lock.release();
            }
            LOG.info("Deliver message to " + mobile + " completed. Status:" + message.getMessageStatus());
            Record cdrRecord = new Record();
            cdrRecord.addField("type", "MT");
            cdrRecord.addField("receiver", mobile);
            cdrRecord.addField("sndtime", DateUtil.dateToString(message.getDate(), "yyyyMMddhhmmss"));
            cdrRecord.addField("status", message.getMessageStatus() == null ? "" : message.getMessageStatus().toString());
            cdrRecord.addField("originid", message.getRefNo());
            cdrRecord.addField("message", alertTask.getMessage().replace("\r\n", " ").replace("\n", " "));
            cdrService.createCDR(cdrRecord);
            ThreadUtil.sleep(idleTime);
        }
    }

    synchronized int nextMsgId() {
        int newId = this.idSeed.incrementAndGet();
        FileUtil.writeFileText(storgePath + "/idseed", "" + newId);
        return newId;
    }

    byte[] defalte(String message) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(output, new Deflater(Deflater.BEST_COMPRESSION));
        deflater.write(message.getBytes("GBK"));
        deflater.finish();
        deflater.flush();
        return output.toByteArray();
    }

    String infalte(byte[] buffer) throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(buffer);
        InflaterInputStream inflater = new InflaterInputStream(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] temparr = new byte[1024];
        int count = 0;
        while ((count = inflater.read(temparr)) > 0) output.write(temparr, 0, count);
        buffer = output.toByteArray();
        return new String(buffer, "GBK");
    }

    synchronized void tryMergeBinaryMessage(final int msgId, final int total, final int index, final InboundMessage inboundMsg) throws Exception {
        File storgeRoot = new File(storgePath);
        String[] fileNames = storgeRoot.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith("inbound-" + msgId + "-");
            }
        });
        if (fileNames == null) return;
        if (total != fileNames.length) return;
        Arrays.sort(fileNames);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (String fileName : fileNames) {
            buffer.write(FileUtil.getFileBytes(storgePath + "/" + fileName));
            FileUtil.deleteFile(storgePath + "/" + fileName);
        }
        String message = infalte(buffer.toByteArray());
        Record cdrRecord = new Record();
        cdrRecord.addField("type", "MO");
        String receiver = inboundMsg.getOriginator();
        if (receiver != null && receiver.startsWith("+86")) receiver = receiver.substring(3);
        cdrRecord.addField("receiver", receiver);
        cdrRecord.addField("rcvtime", DateUtil.dateToString(inboundMsg.getDate(), "yyyyMMddhhmmss"));
        cdrRecord.addField("message", message.replace("\r\n", " ").replace("\n", " "));
        cdrService.createCDR(cdrRecord);
        LOG.info("Received <" + inboundMsg.getType() + "> msg from " + inboundMsg.getOriginator() + ", MsgId:" + inboundMsg.getId() + ", Msg:" + message);
    }

    class InboundNotification implements IInboundMessageNotification {

        public void process(String gtwId, MessageTypes msgType, InboundMessage inboundMsg) {
            if (inboundMsg instanceof StatusReportMessage) {
                StatusReportMessage rptMsg = (StatusReportMessage) inboundMsg;
                Record cdrRecord = new Record();
                cdrRecord.addField("type", "DR");
                String receiver = rptMsg.getRecipient();
                if (receiver != null && receiver.startsWith("+86")) receiver = receiver.substring(3);
                cdrRecord.addField("receiver", receiver);
                cdrRecord.addField("rcvtime", DateUtil.dateToString(rptMsg.getReceived(), "yyyyMMddhhmmss"));
                cdrRecord.addField("status", rptMsg.getStatus().toString());
                cdrRecord.addField("originid", rptMsg.getRefNo());
                cdrRecord.addField("message", inboundMsg.getText().replace("\r\n", " ").replace("\n", " "));
                cdrService.createCDR(cdrRecord);
                LOG.info("Received <" + inboundMsg.getType() + "> msg from " + inboundMsg.getOriginator() + ", MsgId:" + inboundMsg.getId() + ", Msg:" + inboundMsg.getText());
            } else if (inboundMsg instanceof UnknownMessage) {
                Record cdrRecord = new Record();
                cdrRecord.addField("type", inboundMsg.getType().toString());
                String receiver = inboundMsg.getOriginator();
                if (receiver != null && receiver.startsWith("+86")) receiver = receiver.substring(3);
                cdrRecord.addField("receiver", receiver);
                cdrRecord.addField("rcvtime", DateUtil.dateToString(inboundMsg.getDate(), "yyyyMMddhhmmss"));
                cdrRecord.addField("message", inboundMsg.getText().replace("\r\n", " ").replace("\n", " "));
                cdrService.createCDR(cdrRecord);
                LOG.info("Received <" + inboundMsg.getType() + "> msg from " + inboundMsg.getOriginator() + ", MsgId:" + inboundMsg.getId() + ", Msg:" + inboundMsg.getText());
            } else if (inboundMsg instanceof InboundBinaryMessage) {
                Record cdrRecord = new Record();
                InboundBinaryMessage pduMsg = (InboundBinaryMessage) inboundMsg;
                ByteBuffer buffer = ByteBuffer.wrap(pduMsg.getDataBytes());
                int msgId = buffer.getInt();
                int total = buffer.getShort();
                int index = buffer.getShort();
                byte[] temparr = new byte[buffer.remaining()];
                buffer.get(temparr);
                String fileName = storgePath + "/inbound-" + msgId + "-" + total + "-" + index;
                FileUtil.writeFileBytes(fileName, temparr);
                cdrRecord.addField("type", "MOB");
                String receiver = inboundMsg.getOriginator();
                if (receiver != null && receiver.startsWith("+86")) receiver = receiver.substring(3);
                cdrRecord.addField("receiver", receiver);
                cdrRecord.addField("rcvtime", DateUtil.dateToString(inboundMsg.getDate(), "yyyyMMddhhmmss"));
                cdrRecord.addField("message", "PDU message #" + index + " of " + total);
                cdrService.createCDR(cdrRecord);
                LOG.info("Received MOB <" + inboundMsg.getType() + "> msg from " + inboundMsg.getOriginator() + ", MsgId:" + inboundMsg.getId() + ", Msg:" + "PDU message #" + index + " of " + total);
                try {
                    tryMergeBinaryMessage(msgId, total, index, inboundMsg);
                } catch (Exception err) {
                    LOG.error("Call merge binary message failed!", err);
                }
            } else {
                Record cdrRecord = new Record();
                cdrRecord.addField("type", "MO");
                String receiver = inboundMsg.getOriginator();
                if (receiver != null && receiver.startsWith("+86")) receiver = receiver.substring(3);
                cdrRecord.addField("receiver", receiver);
                cdrRecord.addField("rcvtime", DateUtil.dateToString(inboundMsg.getDate(), "yyyyMMddhhmmss"));
                cdrRecord.addField("message", inboundMsg.getText().replace("\r\n", " ").replace("\n", " "));
                cdrService.createCDR(cdrRecord);
                LOG.info("Received <" + inboundMsg.getType() + "> msg from " + inboundMsg.getOriginator() + ", MsgId:" + inboundMsg.getId() + ", Msg:" + inboundMsg.getText());
            }
            if (dropAfterRead) {
                LOG.info("Ready to drop message " + inboundMsg.getId());
                try {
                    lock.acquire();
                    pduService.deleteMessage(inboundMsg);
                    LOG.info("Drop message " + inboundMsg.getId() + " successful.");
                } catch (Throwable err) {
                    LOG.error("Error occured when dropping message " + inboundMsg.getId(), err);
                } finally {
                    lock.release();
                }
            }
        }
    }

    class CallNotification implements ICallNotification {

        public void process(String gatewayId, String callerId) {
            LOG.info("New call detected from " + gatewayId + ", caller is " + callerId);
        }
    }
}
