package com.google.code.xbeejavaapi.api;

import com.google.code.xbeejavaapi.api.Constants.*;
import com.google.code.xbeejavaapi.api.exception.ATCommandReturnedErrorException;
import com.google.code.xbeejavaapi.api.exception.ChecksumFailedException;
import com.google.code.xbeejavaapi.api.exception.XBeeOperationFailedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;

/**
 *
 * @author David Miguel Antunes <davidmiguel [ at ] antunes.net>
 */
public class LocalXBee implements XBee {

    public interface DiscoveredNodeListener {

        public void nodeFound(DiscoveredNode node);
    }

    private static final Logger logger = Logger.getLogger(LocalXBee.class);

    protected static final APIOutputFormat workingAPIOutputFormat = APIOutputFormat.EXPLICIT_ADDRESSING_DATA_FRAMES;

    protected InputStream in;

    protected OutputStream out;

    protected Map<Integer, int[]> messages = new HashMap<Integer, int[]>();

    protected int frameIDGenerator = 1;

    protected FrameListener listener;

    protected Map<XBeeAddress, RemoteXBee> remoteXBees = new HashMap<XBeeAddress, RemoteXBee>();

    protected ArrayList<ReceivedIOSamplesListener> receivedIOSamplesListeners = new ArrayList<ReceivedIOSamplesListener>();

    protected ArrayList<DiscoveredNodeListener> discoveredNodeListeners = new ArrayList<DiscoveredNodeListener>();

    private List<XBeeInputStream> inputStreams = Collections.synchronizedList(new ArrayList<XBeeInputStream>());

    protected long waitForModuleResponseTimeout = 4000;

    protected Set<DiscoveredNode> foundNodes = new HashSet<DiscoveredNode>();

    protected Object lock = new Object();

    protected Set<XBeeAddress> nodesListeningTo = new HashSet<XBeeAddress>();

    public LocalXBee(InputStream in, OutputStream out) throws XBeeOperationFailedException {
        this.in = in;
        this.out = out;
        setAPIMode();
        listener = new FrameListener();
        listener.start();
        setAPIOutputFormat(workingAPIOutputFormat);
    }

    public LocalXBee() {
    }

    private void setAPIMode() throws XBeeOperationFailedException {
        try {
            write("+++");
            readLine();
            write("ATAP1\r");
            readLine();
            write("ATCN\r");
            readLine();
        } catch (IOException ex) {
            logger.error(ex + " at " + ex.getStackTrace()[0].toString());
            throw new XBeeOperationFailedException();
        }
    }

    public void write() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().WR());
        ATCommandResponse.WR resp1 = listener.getResponse(frameID);
    }

    public void restoreDefaults() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().RE());
        ATCommandResponse.RE resp1 = listener.getResponse(frameID);
    }

    public void softwareReset() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().FR());
        ATCommandResponse.FR resp1 = listener.getResponse(frameID);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }
        setAPIMode();
    }

    public void applyChanges() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().AC());
        ATCommandResponse.AC resp1 = listener.getResponse(frameID);
    }

    public void setDestinationAddress(XBeeAddress address) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setDH(address.getHighBytes()));
        ATCommandResponse.DH resp1 = listener.getResponse(frameID);
        frameID = sendATCommand(new ATCommandPayloadFactory().setDL(address.getLowBytes()));
        ATCommandResponse.DL resp2 = listener.getResponse(frameID);
    }

    public XBeeAddress getDestinationAddress() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryDH());
        ATCommandResponse.DH resp1 = listener.getResponse(frameID);
        frameID = sendATCommand(new ATCommandPayloadFactory().queryDL());
        ATCommandResponse.DL resp2 = listener.getResponse(frameID);
        return new XBeeAddress(resp1.getValue(), resp2.getValue());
    }

    public long getDeviceTypeIdentifier() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryDD());
        ATCommandResponse.DD resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public XBeeAddress getSerialNumber() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().querySH());
        ATCommandResponse.SH resp1 = listener.getResponse(frameID);
        frameID = sendATCommand(new ATCommandPayloadFactory().querySL());
        ATCommandResponse.SL resp2 = listener.getResponse(frameID);
        return new XBeeAddress(resp1.getValue(), resp2.getValue());
    }

    public void setHoppingChannel(long hoppingChannel) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setHP((int) hoppingChannel));
        ATCommandResponse.HP resp1 = listener.getResponse(frameID);
    }

    public long getHoppingChannel() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryHP());
        ATCommandResponse.HP resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setSourceEndpoint(long sourceEndpoint) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setSE((int) sourceEndpoint));
        ATCommandResponse.SE resp1 = listener.getResponse(frameID);
    }

    public long getSourceEndpoint() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().querySE());
        ATCommandResponse.SE resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setDestinationEndpoint(long destinationEndpoint) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setDE((int) destinationEndpoint));
        ATCommandResponse.DE resp1 = listener.getResponse(frameID);
    }

    public long getDestinationEndpoint() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryDE());
        ATCommandResponse.DE resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setClusterIdentifier(long clusterIdentifier) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setCI((int) clusterIdentifier));
        ATCommandResponse.CI resp1 = listener.getResponse(frameID);
    }

    public long getClusterIdentifier() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryCI());
        ATCommandResponse.CI resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setMaximumRFPayloadBytes(long maximumRFPayloadBytes) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setNP((int) maximumRFPayloadBytes));
        ATCommandResponse.NP resp1 = listener.getResponse(frameID);
    }

    public long getMaximumRFPayloadBytes() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryNP());
        ATCommandResponse.NP resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setAPIMode(APIMode apiMode) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setAP(apiMode));
        ATCommandResponse.AP resp1 = listener.getResponse(frameID);
    }

    public APIMode getAPIMode() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryAP());
        ATCommandResponse.AP resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setAPIOutputFormat(APIOutputFormat apiOutputFormat) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setAO(apiOutputFormat));
        ATCommandResponse.AO resp1 = listener.getResponse(frameID);
    }

    public APIOutputFormat getAPIOutputFormat() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryAO());
        ATCommandResponse.AO resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setBaudRate(BaudRate baudRate) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setBD(baudRate));
        ATCommandResponse.BD resp1 = listener.getResponse(frameID);
    }

    public BaudRate getBaudRate() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryBD());
        ATCommandResponse.BD resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setPacketizationTimeout(long packetizationTimeout) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setRO(packetizationTimeout));
        ATCommandResponse.RO resp1 = listener.getResponse(frameID);
    }

    public long getPacketizationTimeout() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryRO());
        ATCommandResponse.RO resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setFlowControlThreshold(long flowControlThreshold) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setFT(flowControlThreshold));
        ATCommandResponse.FT resp1 = listener.getResponse(frameID);
    }

    public long getFlowControlThreshold() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryFT());
        ATCommandResponse.FT resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setParity(Parity parity) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setNB(parity));
        ATCommandResponse.NB resp1 = listener.getResponse(frameID);
    }

    public long getParity() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryNB());
        ATCommandResponse.NB resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setDIO7Configuration(DIO7Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD7(config));
        ATCommandResponse.D7 resp1 = listener.getResponse(frameID);
    }

    public DIO7Configuration getDIO7Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD7());
        ATCommandResponse.D7 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setDIO6Configuration(DIO6Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD6(config));
        ATCommandResponse.D6 resp1 = listener.getResponse(frameID);
    }

    public DIO6Configuration getDIO6Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD6());
        ATCommandResponse.D6 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setPWM0DIO10Configuration(PWM0_DIO10_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setP0(config));
        ATCommandResponse.P0 resp1 = listener.getResponse(frameID);
    }

    public PWM0_DIO10_Configuration getPWM0DIO10Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryP0());
        ATCommandResponse.P0 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setPWM1DIO11Configuration(PWM1_DIO11_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setP1(config));
        ATCommandResponse.P1 resp1 = listener.getResponse(frameID);
    }

    public PWM1_DIO11_Configuration getPWM1DIO11Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryP1());
        ATCommandResponse.P1 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setDIO12Configuration(DIO12_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setP2(config));
        ATCommandResponse.P2 resp1 = listener.getResponse(frameID);
    }

    public DIO12_Configuration getDIO12Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryP2());
        ATCommandResponse.P2 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setAD0DIO0Configuration(AD0_DIO0_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD0(config));
        ATCommandResponse.D0 resp1 = listener.getResponse(frameID);
    }

    public AD0_DIO0_Configuration getAD0DIO0Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD0());
        ATCommandResponse.D0 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setAD1DIO1Configuration(AD1_DIO1_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD1(config));
        ATCommandResponse.D1 resp1 = listener.getResponse(frameID);
    }

    public AD1_DIO1_Configuration getAD1DIO1Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD1());
        ATCommandResponse.D1 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setAD2DIO2Configuration(AD2_DIO2_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD2(config));
        ATCommandResponse.D2 resp1 = listener.getResponse(frameID);
    }

    public AD2_DIO2_Configuration getAD2DIO2Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD2());
        ATCommandResponse.D2 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setAD3DIO3Configuration(AD3_DIO3_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD3(config));
        ATCommandResponse.D3 resp1 = listener.getResponse(frameID);
    }

    public AD3_DIO3_Configuration getAD3DIO3Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD3());
        ATCommandResponse.D3 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setAD4DIO4Configuration(AD4_DIO4_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD4(config));
        ATCommandResponse.D4 resp1 = listener.getResponse(frameID);
    }

    public AD4_DIO4_Configuration getAD4DIO4Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD4());
        ATCommandResponse.D4 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setAD5DIO5Configuration(AD5_DIO5_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD5(config));
        ATCommandResponse.D5 resp1 = listener.getResponse(frameID);
    }

    public AD5_DIO5_Configuration getAD5DIO5Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD5());
        ATCommandResponse.D5 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setDIO8Configuration(DIO8_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD8(config));
        ATCommandResponse.D8 resp1 = listener.getResponse(frameID);
    }

    public DIO8_Configuration getDIO8Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD8());
        ATCommandResponse.D8 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setDIO9Configuration(DIO9_Configuration config) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setD9(config));
        ATCommandResponse.D9 resp1 = listener.getResponse(frameID);
    }

    public DIO9_Configuration getDIO9Configuration() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryD9());
        ATCommandResponse.D9 resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setRssiPwmTimer(long rssiPwmTimer) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setRP((int) rssiPwmTimer));
        ATCommandResponse.RP resp1 = listener.getResponse(frameID);
    }

    public long getRssiPwmTimer() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryRP());
        ATCommandResponse.RP resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setEnabledPullupResistors(Set<Pullup_Resistor> enabledPullupResistors) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setPR(enabledPullupResistors));
        ATCommandResponse.PR resp1 = listener.getResponse(frameID);
    }

    public Set<Pullup_Resistor> getEnabledPullupResistors() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryPR());
        ATCommandResponse.PR resp1 = listener.getResponse(frameID);
        return resp1.getEnabledBits();
    }

    public void setPWM0OutputLevel(long pwm0OutputLevel) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setM0((int) pwm0OutputLevel));
        ATCommandResponse.M0 resp1 = listener.getResponse(frameID);
    }

    public long getPWM0OutputLevel() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryM0());
        ATCommandResponse.M0 resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setPWM1OutputLevel(long pwm0OutputLevel) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setM1((int) pwm0OutputLevel));
        ATCommandResponse.M1 resp1 = listener.getResponse(frameID);
    }

    public long getPWM1OutputLevel() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryM1());
        ATCommandResponse.M1 resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setAssocLEDBlinkTime(long assocLEDBlinkTime) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setLT((int) assocLEDBlinkTime));
        ATCommandResponse.LT resp1 = listener.getResponse(frameID);
    }

    public long getAssocLEDBlinkTime() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryLT());
        ATCommandResponse.LT resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void pushCommissioningButton() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().CB());
        ATCommandResponse.CB resp1 = listener.getResponse(frameID);
    }

    public void setIOSampleRate(long ioSampleRate) throws XBeeOperationFailedException {
        int frameID;
        if (ioSampleRate > 0) {
            System.out.println("Adding " + getSerialNumber() + " to nodesListeningTo");
            nodesListeningTo.add(getSerialNumber());
        }
        frameID = sendATCommand(new ATCommandPayloadFactory().setIR((int) ioSampleRate));
        ATCommandResponse.IR resp1 = listener.getResponse(frameID);
        if (ioSampleRate == 0) {
            nodesListeningTo.remove(getSerialNumber());
        }
    }

    public long getIOSampleRate() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryIR());
        ATCommandResponse.IR resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setSampleFromSleepRate(long sampleFromSleepRate) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setIF((int) sampleFromSleepRate));
        ATCommandResponse.IF resp1 = listener.getResponse(frameID);
    }

    public long getSampleFromSleepRate() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryIF());
        ATCommandResponse.IF resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setMacRetries(long macRetries) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setRR((int) macRetries));
        ATCommandResponse.RR resp1 = listener.getResponse(frameID);
    }

    public long getMacRetries() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryRR());
        ATCommandResponse.RR resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setMultipleTransmissions(long multipleTransmissions) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setMT((int) multipleTransmissions));
        ATCommandResponse.MT resp1 = listener.getResponse(frameID);
    }

    public long getMultipleTransmissions() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryMT());
        ATCommandResponse.MT resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setEnabledIODigitalChangeDetectionPins(Set<Digital_IO_Pin> enabledDigitalChangeDetectionIOPins) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setIC(enabledDigitalChangeDetectionIOPins));
        ATCommandResponse.IC resp1 = listener.getResponse(frameID);
    }

    public Set<Digital_IO_Pin> getEnabledIODigitalChangeDetectionPins() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryIC());
        ATCommandResponse.IC resp1 = listener.getResponse(frameID);
        return resp1.getEnabledBits();
    }

    public IOState forceSample() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().IS());
        ATCommandResponse.IS resp1 = listener.getResponse(frameID);
        IOState state = new IOState(resp1.getDigitalIOState(), resp1.getAnalogIOState());
        for (ReceivedIOSamplesListener receivedIOSamplesListener : receivedIOSamplesListeners) {
            receivedIOSamplesListener.ioSamplesReceived(state);
        }
        return state;
    }

    public long getFirmwareVersion() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryVR());
        ATCommandResponse.VR resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public long getHardwareVersion() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryHV());
        ATCommandResponse.HV resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public long getNumberOfRFErrors() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryER());
        ATCommandResponse.ER resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public long getNumberOfGoodPackets() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryGD());
        ATCommandResponse.GD resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public long getNumberOfTransmissionErrors() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryTR());
        ATCommandResponse.TR resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public Association_Indication getAssociationIndication() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryAI());
        ATCommandResponse.AI resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setCommandModeTimeout(long commandModeTimeout) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setCT(commandModeTimeout));
        ATCommandResponse.CT resp1 = listener.getResponse(frameID);
    }

    public long getCommandModeTimeout() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryCT());
        ATCommandResponse.CT resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setGuardTimes(long guardTimes) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setGT(guardTimes));
        ATCommandResponse.GT resp1 = listener.getResponse(frameID);
    }

    public long getGuardTimes() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryGT());
        ATCommandResponse.GT resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setCommandCharacter(long commandCharacter) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setCC((int) commandCharacter));
        ATCommandResponse.CC resp1 = listener.getResponse(frameID);
    }

    public long getCommandCharacter() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryCC());
        ATCommandResponse.CC resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setPanId(long panId) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setID((int) panId));
        ATCommandResponse.ID resp1 = listener.getResponse(frameID);
    }

    public long getPanId() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryID());
        ATCommandResponse.ID resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setNodeDiscoverTimeout(long nodeDiscoverTimeout) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setNT((int) nodeDiscoverTimeout));
        ATCommandResponse.NT resp1 = listener.getResponse(frameID);
    }

    public long getNodeDiscoverTimeout() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryNT());
        ATCommandResponse.NT resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setNodeIdentifier(String nodeIdentifier) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setNI(nodeIdentifier));
        ATCommandResponse.NI resp1 = listener.getResponse(frameID);
    }

    public String getNodeIdentifier() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryNI());
        ATCommandResponse.NI resp1 = listener.getResponse(frameID);
        return resp1.getText();
    }

    public XBeeAddress discoverNode(String node) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().DN(node));
        ATCommandResponse.DN resp1 = listener.getResponse(frameID);
        return resp1.getAddress();
    }

    public void setNetworkDiscoverOptions(Set<Network_Discovery_Option> networkDiscoveryOptions) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setNO(networkDiscoveryOptions));
        ATCommandResponse.NO resp1 = listener.getResponse(frameID);
    }

    public Set<Network_Discovery_Option> getNetworkDiscoverOptions() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryNO());
        ATCommandResponse.NO resp1 = listener.getResponse(frameID);
        return resp1.getEnabledBits();
    }

    public Set<DiscoveredNode> searchNodes() throws XBeeOperationFailedException {
        long nt = (long) (getNodeDiscoverTimeout() * 100 * 1.2);
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().ND());
        long start = System.currentTimeMillis();
        ATCommandResponse.ND resp1;
        Set<DiscoveredNode> discoveredNodes = new HashSet<DiscoveredNode>();
        while ((resp1 = listener.getResponse(frameID, Math.max(nt - (System.currentTimeMillis() - start), 0))) != null) {
            DiscoveredNode discoveredNode = new DiscoveredNode(resp1.getAddress(), resp1.getNodeIdentifier(), resp1.getParentNetworkAddress(), resp1.getDeviceType(), resp1.getStatus(), resp1.getProfileId(), resp1.getManufacturerId(), openRemoteXBee(resp1.getAddress()));
            discoveredNodes.add(discoveredNode);
            if (!foundNodes.contains(discoveredNode)) {
                foundNodes.add(discoveredNode);
                for (DiscoveredNodeListener discoveredNodeListener : discoveredNodeListeners) {
                    discoveredNodeListener.nodeFound(discoveredNode);
                }
            }
        }
        return discoveredNodes;
    }

    public void setSleepMode(Sleep_Mode sleepMode) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setSM(sleepMode));
        ATCommandResponse.SM resp1 = listener.getResponse(frameID);
    }

    public Sleep_Mode getSleepMode() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().querySM());
        ATCommandResponse.SM resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setSleepOptions(Set<Sleep_Option> sleepOptions) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setSO(sleepOptions));
        ATCommandResponse.SO resp1 = listener.getResponse(frameID);
    }

    public Set<Sleep_Option> getSleepOptions() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().querySO());
        ATCommandResponse.SO resp1 = listener.getResponse(frameID);
        return resp1.getEnabledBits();
    }

    public void setWakeTime(long wakeTime) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setST(wakeTime));
        ATCommandResponse.ST resp1 = listener.getResponse(frameID);
    }

    public long getWakeTime() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryST());
        ATCommandResponse.ST resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setSleepPeriod(long sleepPeriod) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setSP(sleepPeriod));
        ATCommandResponse.SP resp1 = listener.getResponse(frameID);
    }

    public long getSleepPeriod() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().querySP());
        ATCommandResponse.SP resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public long getNumberOfMissedSyncs() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryMS());
        ATCommandResponse.MS resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void clearMissedSyncCount(long sleepPeriod) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setSQ(0l));
        ATCommandResponse.SQ resp1 = listener.getResponse(frameID);
    }

    public long getMissedSyncCount() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().querySQ());
        ATCommandResponse.SQ resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public Set<Sleep_Status_Value> getSleepStatus() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().querySS());
        ATCommandResponse.SS resp1 = listener.getResponse(frameID);
        return resp1.getEnabledBits();
    }

    public long getOperationalSleepPeriod() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryOS());
        ATCommandResponse.OS resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public long getOperationalWakePeriod() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryOW());
        ATCommandResponse.OW resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setWakeHost(long wakeHost) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setWH(wakeHost));
        ATCommandResponse.WH resp1 = listener.getResponse(frameID);
    }

    public long getWakeHost() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryWH());
        ATCommandResponse.WH resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setEE(encryptionEnabled ? Encryption_Enable.ENCRYPTION_ENABLED : Encryption_Enable.ENCRYPTION_DISABLED));
        ATCommandResponse.EE resp1 = listener.getResponse(frameID);
    }

    public long getEncryptionEnabled() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryEE());
        ATCommandResponse.EE resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setEncryptionKey(int[] encryptionKey) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setKY(encryptionKey));
        ATCommandResponse.KY resp1 = listener.getResponse(frameID);
    }

    public void setChannel(long channel) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setCH((int) channel));
        ATCommandResponse.CH resp1 = listener.getResponse(frameID);
    }

    public long getChannel() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryCH());
        ATCommandResponse.CH resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setNetworkIdentifier(long networkIdentifier) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setID((int) networkIdentifier));
        ATCommandResponse.ID resp1 = listener.getResponse(frameID);
    }

    public long getNetworkIdentifier() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryID());
        ATCommandResponse.ID resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setRoutingType(Routing_Type routingType) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setCE(routingType));
        ATCommandResponse.CE resp1 = listener.getResponse(frameID);
    }

    public Routing_Type getRoutingType() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryCE());
        ATCommandResponse.CE resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setPowerLevel(Power_Level powerLevel) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setPL(powerLevel));
        ATCommandResponse.PL resp1 = listener.getResponse(frameID);
    }

    public Power_Level getPowerLevel() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryPL());
        ATCommandResponse.PL resp1 = listener.getResponse(frameID);
        return resp1.getElement();
    }

    public void setCcaThreshold(long ccaThreshold) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setCA(ccaThreshold));
        ATCommandResponse.CA resp1 = listener.getResponse(frameID);
    }

    public long getCcaThreshold() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryCA());
        ATCommandResponse.CA resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public long getReceivedSignalStrength() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryDB());
        ATCommandResponse.DB resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setNetworkHops(long networkHops) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setNH(networkHops));
        ATCommandResponse.NH resp1 = listener.getResponse(frameID);
    }

    public long getNetworkHops() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryNH());
        ATCommandResponse.NH resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setNetworkDelaySlots(long networkDelaySlots) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setNN(networkDelaySlots));
        ATCommandResponse.NN resp1 = listener.getResponse(frameID);
    }

    public long getNetworkDelaySlots() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryNN());
        ATCommandResponse.NN resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setNetworkRouteRequests(long networkRouteRequests) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setNQ(networkRouteRequests));
        ATCommandResponse.NQ resp1 = listener.getResponse(frameID);
    }

    public long getNetworkRouteRequests() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryNQ());
        ATCommandResponse.NQ resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public void setMeshNetworkRetries(long meshNetworkRetries) throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().setMR(meshNetworkRetries));
        ATCommandResponse.MR resp1 = listener.getResponse(frameID);
    }

    public long getMeshNetworkRetries() throws XBeeOperationFailedException {
        int frameID;
        frameID = sendATCommand(new ATCommandPayloadFactory().queryMR());
        ATCommandResponse.MR resp1 = listener.getResponse(frameID);
        return resp1.getValue();
    }

    public long getWaitForModuleResponseTimeout() {
        return waitForModuleResponseTimeout;
    }

    public void setWaitForModuleResponseTimeout(long waitForModuleResponseTimeout) {
        this.waitForModuleResponseTimeout = waitForModuleResponseTimeout;
    }

    public RemoteXBee openRemoteXBee(XBeeAddress address) throws XBeeOperationFailedException {
        if (remoteXBees.get(address) == null) {
            RemoteXBee xbee = new RemoteXBee(address);
            remoteXBees.put(address, xbee);
        }
        return remoteXBees.get(address);
    }

    public int sendATCommand(ATCommandRequest command) throws XBeeOperationFailedException {
        int[] data = new int[4 + command.getParameters().length];
        int i = 0;
        int frameID = generateFrameID();
        data[i++] = APIFrameType.ATCommand.getValue();
        data[i++] = frameID;
        data[i++] = (int) command.getCommand().getCommandString().charAt(0);
        data[i++] = (int) command.getCommand().getCommandString().charAt(1);
        for (int j = 0; j < command.getParameters().length; j++) {
            data[i++] = command.getParameters()[j];
        }
        sendFrame(data);
        return frameID;
    }

    public XBeeInputStream openInputStream(Filter filter) {
        XBeeInputStream is = new XBeeInputStream(filter);
        inputStreams.add(is);
        return is;
    }

    public XBeeOutputStream openOutputStream(XBeeAddress destination, long sourceEndpoint, long destinationEndpoint, long clusterId, long profileId, long maxBroadcastHops, boolean attemptRouteDiscovery) {
        return new XBeeOutputStream(destination, sourceEndpoint, destinationEndpoint, clusterId, profileId, maxBroadcastHops, attemptRouteDiscovery);
    }

    protected int generateFrameID() {
        frameIDGenerator++;
        if (frameIDGenerator == 256) {
            frameIDGenerator = 1;
        }
        return frameIDGenerator;
    }

    protected void sendFrame(int[] data) throws XBeeOperationFailedException {
        synchronized (out) {
            try {
                write(0x7E);
                write(data.length >> 8);
                write(data.length);
                write(data);
                int sum = 0;
                for (int i = 0; i < data.length; i++) {
                    sum += data[i];
                }
                write(0xFF - (sum % 256));
            } catch (IOException ex) {
                logger.error(ex + " at " + ex.getStackTrace()[0].toString());
                throw new XBeeOperationFailedException();
            }
        }
    }

    /**
     * Disconnects this instance from hardware (normally a serial port).
     * No other method may be called after this one.
     */
    public void disconnect() {
        try {
            in.close();
            out.close();
        } catch (IOException ex) {
            logger.error(ex + " at " + ex.getStackTrace()[0].toString());
        }
    }

    protected void write(String s) throws IOException {
        logger.trace("Writting: " + "\"" + s + "\"".replace("\r", "\\r"));
        for (int i = 0; i < s.toCharArray().length; i++) {
            char c = s.toCharArray()[i];
            write(c);
        }
    }

    protected void write(int[] data) throws IOException {
        for (int j = 0; j < data.length; j++) {
            write(data[j]);
        }
    }

    protected void write(int i) throws IOException {
        logger.trace(">" + "0x" + Integer.toHexString(i).toUpperCase() + " '" + (char) i + "'");
        out.write(i);
        out.flush();
    }

    protected String readLine() throws IOException {
        String line = "";
        char c = (char) read();
        while (c != '\r') {
            line += c;
            c = (char) read();
        }
        logger.trace("Read line: \"" + line + "\"");
        return line;
    }

    protected int read() throws IOException {
        int b = in.read();
        logger.trace("<" + "0x" + Integer.toHexString(b).toUpperCase() + " '" + (char) b + "'");
        return b;
    }

    public void addDigitalChangeDetectionListener(ReceivedIOSamplesListener listener) {
        receivedIOSamplesListeners.add(listener);
    }

    public void addDiscoveredNodeListener(DiscoveredNodeListener listener) {
        discoveredNodeListeners.add(listener);
    }

    class FrameListener extends Thread {

        Map<Integer, Object> locks = new HashMap<Integer, Object>();

        Map<Integer, Object> frames = new HashMap<Integer, Object>();

        {
            for (int i = 0; i < 256; i++) {
                locks.put(i, new Object());
            }
        }

        public <T extends FrameWithID> T getResponse(int id) throws XBeeOperationFailedException {
            T response = (T) getResponse(id, waitForModuleResponseTimeout);
            if (response == null) {
                throw new XBeeOperationFailedException("Timeout while waiting for frame.");
            }
            return response;
        }

        public <T extends FrameWithID> T getResponse(int id, long timeout) throws XBeeOperationFailedException {
            frames.put(id, null);
            synchronized (locks.get(id)) {
                try {
                    locks.get(id).wait(timeout);
                } catch (InterruptedException ex) {
                    logger.error(ex + " at " + ex.getStackTrace()[0].toString());
                }
            }
            if (frames.get(id) instanceof XBeeOperationFailedException) {
                throw (XBeeOperationFailedException) frames.get(id);
            }
            return (T) frames.get(id);
        }

        @Override
        public void run() {
            try {
                logger.debug("Started frameListener");
                while (true) {
                    int b = read();
                    if (b != 0x7E) {
                        continue;
                    }
                    int[] data = receiveFrame();
                    if (APIFrameType.get(data[0]) == null) {
                        logger.error("Received an unknown frame type: " + "0x" + Integer.toHexString(data[0]).toUpperCase());
                        continue;
                    }
                    String debug = "Received API Frame (" + APIFrameType.get(data[0]).toString() + "): ";
                    debug += data[0];
                    for (int i = 1; i < data.length; i++) {
                        debug += (char) data[i];
                    }
                    logger.trace(debug);
                    Object frame = null;
                    try {
                        switch(APIFrameType.get(data[0])) {
                            case ReceivePacket:
                                parseReceivePacket(data);
                                break;
                            case ExplicitRxIndicator:
                                parseExplicitRxIndicator(data);
                                break;
                            case TransmitStatus:
                                frame = new TransmitStatusFactory().parse(data);
                                break;
                            case ATCommandResponse:
                                frame = new ATCommandResponseFactory().parse(data);
                                if (!((ATCommandResponse) frame).getCommandStatus().equals(ATCommandResponse.CommandStatus.OK)) {
                                    logger.error("ATCommand " + ((ATCommandResponse) frame).getCommand().getCommandString() + " returned an error");
                                }
                                break;
                            case RemoteCommandResponse:
                                int[] partialRemoteData = new int[data.length - 10];
                                for (int i = 0; i < 2; i++) {
                                    partialRemoteData[i] = data[i];
                                }
                                for (int i = 12; i < data.length; i++) {
                                    partialRemoteData[i - 10] = data[i];
                                }
                                frame = new ATCommandResponseFactory().parse(partialRemoteData);
                                if (!((ATCommandResponse) frame).getCommandStatus().equals(ATCommandResponse.CommandStatus.OK)) {
                                    logger.error("ATCommand " + ((ATCommandResponse) frame).getCommand().getCommandString() + " returned an error");
                                }
                                break;
                            case IODataSampleRxIndicator:
                                parseIODataSampleRxIndicator(data);
                                break;
                            default:
                                logger.warn("Handle for frame type " + data[0] + " not implemented.");
                                break;
                        }
                    } catch (ATCommandReturnedErrorException ex) {
                        logger.error(ex + " at " + ex.getStackTrace()[0].toString());
                        frame = ex;
                    }
                    if (frame != null && frame instanceof FrameWithID) {
                        FrameWithID frameWithID = (FrameWithID) frame;
                        frames.put(frameWithID.getId(), frameWithID);
                        synchronized (locks.get(frameWithID.getId())) {
                            locks.get(frameWithID.getId()).notifyAll();
                        }
                    } else if (frame instanceof ATCommandReturnedErrorException) {
                        ATCommandReturnedErrorException ex = (ATCommandReturnedErrorException) frame;
                        frames.put(ex.getFrameID(), ex);
                        synchronized (locks.get(ex.getFrameID())) {
                            locks.get(ex.getFrameID()).notifyAll();
                        }
                    }
                    logger.debug("API Frame: " + frame);
                }
            } catch (XBeeOperationFailedException ex) {
                logger.error(ex + " at " + ex.getStackTrace()[0].toString());
            } catch (ChecksumFailedException ex) {
                logger.error(ex + " at " + ex.getStackTrace()[0].toString());
            } catch (IOException ex) {
                logger.error(ex + " at " + ex.getStackTrace()[0].toString());
            }
        }

        private void parseIODataSampleRxIndicatorPayload(int idx, int[] data, XBeeAddress address) throws XBeeOperationFailedException {
            idx++;
            HashMap<Digital_IO_Pin, Boolean> digitalIOState = new HashMap<Digital_IO_Pin, Boolean>();
            HashMap<Analog_IO_Pin, Double> analogIOState = new HashMap<Analog_IO_Pin, Double>();
            long enabledDigitalIOValue = data[idx++] << 8;
            enabledDigitalIOValue = enabledDigitalIOValue | data[idx++];
            List<Digital_IO_Pin> enabledDigitalIOPins = readEnabledBits(Digital_IO_Pin.class, 13, enabledDigitalIOValue);
            long enabledAnalogIOValue = data[idx++];
            List<Analog_IO_Pin> enabledAnalogIOPins = readEnabledBits(Analog_IO_Pin.class, 6, enabledAnalogIOValue);
            if (!enabledDigitalIOPins.isEmpty()) {
                long digitalIOStateValue = data[idx++] << 8;
                digitalIOStateValue = digitalIOStateValue | data[idx++];
                List<Digital_IO_Pin> tempEnabledDigitalIOState = readEnabledBits(Digital_IO_Pin.class, 13, digitalIOStateValue);
                for (Digital_IO_Pin digital_IO_Pin : enabledDigitalIOPins) {
                    if (tempEnabledDigitalIOState.contains(digital_IO_Pin)) {
                        digitalIOState.put(digital_IO_Pin, Boolean.TRUE);
                    } else {
                        digitalIOState.put(digital_IO_Pin, Boolean.FALSE);
                    }
                }
            }
            for (Analog_IO_Pin analog_IO_Pin : enabledAnalogIOPins) {
                long value = data[idx++] << 8;
                value = value | data[idx++];
                analogIOState.put(analog_IO_Pin, (double) value);
            }
            IOState ioState = new IOState(digitalIOState, analogIOState);
            for (ReceivedIOSamplesListener receivedIOSamplesListener : openRemoteXBee(address).receivedIOSamplesListeners) {
                receivedIOSamplesListener.ioSamplesReceived(ioState);
            }
        }

        private <T> List<T> readEnabledBits(Class e, int nBits, long value) {
            ArrayList<T> enabledBits = new ArrayList<T>();
            int[] bits = new int[nBits];
            for (int i = 0; i < nBits; i++) {
                bits[i] = i;
            }
            Method m;
            try {
                m = e.getMethod("values", new Class[0]);
                ValueBasedEnum[] values = (ValueBasedEnum[]) m.invoke(null, new Object[0]);
                for (int idx = 0; idx < bits.length; idx++) {
                    int bit = bits[idx];
                    if ((value & (0x1 << bit)) != 0) {
                        for (int j = 0; j < values.length; j++) {
                            ValueBasedEnum valueBasedEnum = values[j];
                            if (valueBasedEnum.getValue() == bit) {
                                enabledBits.add((T) valueBasedEnum);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error(ex + " at " + ex.getStackTrace()[0].toString());
            }
            return enabledBits;
        }

        /**
         * Returns frame data (bytes 4-n, see LocalXBee®/LocalXBee-PRO® DigiMeshTM 2.4 RF Modules Manual)
         */
        private int[] receiveFrame() throws XBeeOperationFailedException, ChecksumFailedException, IOException {
            int length;
            int length1 = read();
            int length2 = read();
            length = (length1 << 8) | length2;
            int[] data = new int[length];
            for (int i = 0; i < length; i++) {
                data[i] = read();
            }
            int checksum = 0;
            for (int i = 0; i < data.length; i++) {
                checksum += data[i];
            }
            int receivedChecksum = read();
            checksum += receivedChecksum;
            if (checksum % 256 != 0xFF) {
                throw new ChecksumFailedException();
            }
            return data;
        }

        private void parseExplicitRxIndicator(int[] data) throws XBeeOperationFailedException {
            int idx = 1;
            long highBytes = data[idx++];
            for (int i = 0; i < 3; i++) {
                highBytes = highBytes << 8;
                highBytes = highBytes | data[idx++];
            }
            long lowBytes = data[idx++];
            for (int i = 0; i < 3; i++) {
                lowBytes = lowBytes << 8;
                lowBytes = lowBytes | data[idx++];
            }
            XBeeAddress address = new XBeeAddress(highBytes, lowBytes);
            idx++;
            idx++;
            int sourceEndpoint = data[idx++];
            int destinationEndpoint = data[idx++];
            int clusterId = data[idx++] << 8;
            clusterId = clusterId | data[idx++];
            int profileId = data[idx++] << 8;
            profileId = profileId | data[idx++];
            idx++;
            if (nodesListeningTo.contains(address) && workingAPIOutputFormat.equals(workingAPIOutputFormat.EXPLICIT_ADDRESSING_DATA_FRAMES)) {
                parseIODataSampleRxIndicatorPayload(idx, data, address);
            } else {
                int[] rfData = Arrays.copyOfRange(data, idx, data.length);
                for (XBeeInputStream xBeeInputStream : inputStreams) {
                    xBeeInputStream.newData(rfData, address, sourceEndpoint, destinationEndpoint, clusterId, profileId);
                }
            }
        }

        private void parseReceivePacket(int[] data) {
            int idx = 1;
            idx++;
            long highBytes = data[idx++];
            for (int i = 0; i < 3; i++) {
                highBytes = highBytes << 8;
                highBytes = highBytes | data[idx++];
            }
            long lowBytes = data[idx++];
            for (int i = 0; i < 3; i++) {
                lowBytes = lowBytes << 8;
                lowBytes = lowBytes | data[idx++];
            }
            XBeeAddress address = new XBeeAddress(highBytes, lowBytes);
            idx++;
            idx++;
            int sourceEndpoint = -1;
            int destinationEndpoint = -1;
            int clusterId = -1;
            int profileId = -1;
            int[] rfData = Arrays.copyOfRange(data, idx, data.length);
            for (XBeeInputStream xBeeInputStream : inputStreams) {
                xBeeInputStream.newData(rfData, address, sourceEndpoint, destinationEndpoint, clusterId, profileId);
            }
        }

        private void parseIODataSampleRxIndicator(int[] data) throws XBeeOperationFailedException {
            int idx = 1;
            long highBytes = data[idx++];
            for (int i = 0; i < 3; i++) {
                highBytes = highBytes << 8;
                highBytes = highBytes | data[idx++];
            }
            long lowBytes = data[idx++];
            for (int i = 0; i < 3; i++) {
                lowBytes = lowBytes << 8;
                lowBytes = lowBytes | data[idx++];
            }
            XBeeAddress address = new XBeeAddress(highBytes, lowBytes);
            idx++;
            idx++;
            idx++;
            parseIODataSampleRxIndicatorPayload(idx, data, address);
        }
    }

    public class RemoteXBee extends LocalXBee implements XBee {

        private XBeeAddress address;

        public RemoteXBee(XBeeAddress address) throws XBeeOperationFailedException {
            this.address = address;
            this.in = LocalXBee.this.in;
            this.out = LocalXBee.this.out;
            this.messages = LocalXBee.this.messages;
            this.frameIDGenerator = LocalXBee.this.frameIDGenerator;
            this.listener = LocalXBee.this.listener;
            this.lock = LocalXBee.this.lock;
            this.nodesListeningTo = LocalXBee.this.nodesListeningTo;
        }

        @Override
        public int sendATCommand(ATCommandRequest command) throws XBeeOperationFailedException {
            int[] data = new int[4 + 11 + command.getParameters().length];
            int i = 0;
            int frameID = generateFrameID();
            data[i++] = APIFrameType.RemoteCommandRequest.getValue();
            data[i++] = frameID;
            data[i++] = ((int) address.getHighBytes() & 0xFF000000) >> 24;
            data[i++] = ((int) address.getHighBytes() & 0xFF0000) >> 16;
            data[i++] = ((int) address.getHighBytes() & 0xFF00) >> 8;
            data[i++] = ((int) address.getHighBytes() & 0xFF);
            data[i++] = ((int) address.getLowBytes() & 0xFF000000) >> 24;
            data[i++] = ((int) address.getLowBytes() & 0xFF0000) >> 16;
            data[i++] = ((int) address.getLowBytes() & 0xFF00) >> 8;
            data[i++] = ((int) address.getLowBytes() & 0xFF);
            data[i++] = 0xFF;
            data[i++] = 0xFE;
            data[i++] = 0x02;
            data[i++] = (int) command.getCommand().getCommandString().charAt(0);
            data[i++] = (int) command.getCommand().getCommandString().charAt(1);
            for (int j = 0; j < command.getParameters().length; j++) {
                data[i++] = command.getParameters()[j];
            }
            sendFrame(data);
            return frameID;
        }

        @Override
        public IOState forceSample() throws XBeeOperationFailedException {
            IOState state = super.forceSample();
            for (ReceivedIOSamplesListener receivedIOSamplesListener : receivedIOSamplesListeners) {
                receivedIOSamplesListener.ioSamplesReceived(state);
            }
            return state;
        }

        public void softwareReset() throws XBeeOperationFailedException {
            int frameID;
            frameID = sendATCommand(new ATCommandPayloadFactory().FR());
            ATCommandResponse.FR resp1 = listener.getResponse(frameID);
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
            }
        }
    }

    public class XBeeOutputStream extends OutputStream {

        private final Logger logger = Logger.getLogger(XBeeOutputStream.class);

        private XBeeAddress destination;

        private long sourceEndpoint;

        private long destinationEndpoint;

        private long clusterId;

        private long profileId;

        private long maxBroadcastHops;

        private boolean attemptRouteDiscovery;

        private long timeout = 1000;

        private LinkedList<Integer> queue = new LinkedList<Integer>();

        private final Object lock = new Object();

        public XBeeOutputStream(XBeeAddress destination, long sourceEndpoint, long destinationEndpoint, long clusterId, long profileId, long maxBroadcastHops, boolean attemptRouteDiscovery) {
            this.destination = destination;
            this.sourceEndpoint = sourceEndpoint;
            this.destinationEndpoint = destinationEndpoint;
            this.clusterId = clusterId;
            this.profileId = profileId;
            this.maxBroadcastHops = maxBroadcastHops;
            this.attemptRouteDiscovery = attemptRouteDiscovery;
        }

        @Override
        public void write(int b) throws IOException {
            synchronized (lock) {
                try {
                    queue.addLast(b);
                    if (queue.size() >= (int) getMaximumRFPayloadBytes()) {
                        flush();
                    } else {
                        new Timer().schedule(new TimerTask() {

                            @Override
                            public void run() {
                                try {
                                    flush();
                                } catch (IOException ex) {
                                    logger.error(ex + " at " + ex.getStackTrace()[0].toString());
                                }
                            }
                        }, timeout);
                    }
                } catch (XBeeOperationFailedException ex) {
                    throw new IOException(ex);
                }
            }
        }

        @Override
        public void flush() throws IOException {
            synchronized (lock) {
                try {
                    while (!queue.isEmpty()) {
                        int size = (int) getMaximumRFPayloadBytes();
                        List<Integer> data = queue.subList(Math.max(0, queue.size() - size), queue.size());
                        try {
                            sendTransmitRequest(data.toArray(new Integer[data.size()]));
                        } catch (XBeeOperationFailedException ex) {
                            throw new IOException(ex);
                        } finally {
                            data.clear();
                        }
                    }
                } catch (XBeeOperationFailedException ex) {
                    throw new IOException(ex);
                }
            }
        }

        public void sendTransmitRequest(Integer[] payload) throws XBeeOperationFailedException {
            int[] data = new int[14 + payload.length];
            int i = 0;
            int frameID = generateFrameID();
            data[i++] = APIFrameType.TransmitRequest.getValue();
            data[i++] = frameID;
            data[i++] = (int) (destination.getHighBytes() >> 24 & 0xFF);
            data[i++] = (int) (destination.getHighBytes() >> 16 & 0xFF);
            data[i++] = (int) (destination.getHighBytes() >> 8 & 0xFF);
            data[i++] = (int) (destination.getHighBytes() & 0xFF);
            data[i++] = (int) (destination.getLowBytes() >> 24 & 0xFF);
            data[i++] = (int) (destination.getLowBytes() >> 16 & 0xFF);
            data[i++] = (int) (destination.getLowBytes() >> 8 & 0xFF);
            data[i++] = (int) (destination.getLowBytes() & 0xFF);
            data[i++] = (int) (0xFF);
            data[i++] = (int) (0xFE);
            data[i++] = (int) (maxBroadcastHops);
            data[i++] = (int) (attemptRouteDiscovery ? 0 : 1 >> 1);
            for (int j = 0; j < payload.length; j++) {
                data[i++] = payload[j];
            }
            sendFrame(data);
            TransmitStatus transmitStatus = listener.getResponse(frameID);
            if (!transmitStatus.getDeliveryStatus().equals(TransmitStatus.DeliveryStatus.Success)) {
                throw new XBeeOperationFailedException("Failed to transmit packet: " + transmitStatus.getDeliveryStatus().toString());
            }
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public String toString() {
            return "XBeeOutputStream{" + "destination=" + destination + "sourceEndpoint=" + sourceEndpoint + "destinationEndpoint=" + destinationEndpoint + "clusterId=" + clusterId + "profileId=" + profileId + "maxBroadcastHops=" + maxBroadcastHops + "attemptRouteDiscovery=" + attemptRouteDiscovery + "timeout=" + timeout + '}';
        }
    }

    public class XBeeInputStream extends InputStream {

        private Filter filter;

        private final LinkedList<Integer> data = new LinkedList<Integer>();

        public XBeeInputStream(Filter filter) {
            this.filter = filter;
        }

        @Override
        public int read() throws IOException {
            synchronized (data) {
                while (true) {
                    if (!data.isEmpty()) {
                        return data.pop();
                    }
                    try {
                        data.wait(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        public void newData(int[] data, XBeeAddress senderAddress, int sourceEndpoint, int destinationEndpoint, int clusterId, int profileId) {
            if (filter.matches(senderAddress, sourceEndpoint, destinationEndpoint, clusterId, profileId)) {
                synchronized (data) {
                    for (int i = 0; i < data.length; i++) {
                        int j = data[i];
                        this.data.addLast(j);
                    }
                    data.notifyAll();
                }
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            inputStreams.remove(this);
        }

        @Override
        public String toString() {
            return "XBeeInputStream{" + "filter=" + filter + '}';
        }
    }
}
