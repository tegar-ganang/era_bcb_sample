package com.szxys.mhub.base.btprotocol;

import java.io.IOException;
import java.io.InputStream;
import com.szxys.mhub.R;
import com.szxys.mhub.base.btdevice.BthTransportAttribute;
import android.bluetooth.BluetoothSocket;
import android.media.MediaPlayer;

class CommPolicyOfCommonMode implements CommPolicy {

    public CommPolicyOfCommonMode(BthTransportAttribute aBthTransAttribute, IProtocolInformHandler aInformHandler, BluetoothSocket aBtSocket) throws Exception {
        if (aBthTransAttribute == null) {
            BthLog.e(TAG, "at least one parameter is invalid");
            throw new Exception("at least one parameter is invalid");
        }
        if (aInformHandler == null || aBtSocket == null || aBthTransAttribute.fNumOfChannels < 1 || aBthTransAttribute.fNumOfChannels > 256) {
            BthLog.e(TAG, "at least one parameter is invalid");
            throw new Exception("at least one parameter is invalid");
        }
        fAttributes = new CommAttributes();
        fAttributes.fInformHandler = aInformHandler;
        fAttributes.fBtSocket = aBtSocket;
        fAttributes.fInputStream = aBtSocket.getInputStream();
        fAttributes.fOutputStream = aBtSocket.getOutputStream();
        fAttributes.fTransportAttribute = aBthTransAttribute;
        fAttributes.initChannels();
        setRunning(false);
        fIsActiveDisconnection = false;
    }

    @Override
    public synchronized boolean run() {
        if (isRunning() == true) return true;
        initialize();
        setRunning(true);
        return true;
    }

    @Override
    public synchronized boolean postData(byte[] aSendData, int aChannel) {
        if (isRunning() == false) return false;
        if (aSendData == null) return true;
        if (aSendData.length == 0) return false;
        if (fAttributes.getChannel(aChannel) == null) {
            BthLog.e(TAG, "channel out of range");
            return false;
        }
        return realSend(fBthDataOutputStream, aSendData, aChannel, CommPacket.PACKET_DATA);
    }

    @Override
    public synchronized void shutDown() {
        if (isRunning() == false) return;
        processActiveDisconnection();
        unInitialize();
    }

    private void initialize() {
        fCommSlicable = new CommSlicable();
        BthDataWithACKOutputStream bthWithAck = new BthDataWithACKOutputStream(fAttributes.fOutputStream, 3000);
        fSendAckHandler = bthWithAck.getAckEventHandler();
        fBthDataOutputStreamWithoutAck = bthWithAck.getWithoutACKOutputStream();
        BthDataRepeatableOutputStream bthRepatable = new BthDataRepeatableOutputStream(bthWithAck, 3);
        BthDataSlicableOutputStream bthSlicer = new BthDataSlicableOutputStream(bthRepatable, 1024, fCommSlicable);
        fBthDataOutputStream = bthSlicer;
        fCommPacket = new CommPacket(new CommCheckSum());
        fChannelCurrentSent = null;
        fParser = new PacketParser();
        fParser.fTotalLength = 0;
        fCommRecvRunner = new CommReceiverRunnable(fAttributes.fInputStream);
        new Thread(fCommRecvRunner, TAG + " : receiver").start();
        if (fAttributes.fTransportAttribute.fHeartBeatFrequency == 0) {
            BthLog.e(TAG, "do not use heart beat policy");
            fHeartBeatPolicy = new ProcessorOfNopHeartBeat();
        } else {
            BthLog.e(TAG, "use heart beat policy");
            fHeartBeatPolicyHandler = new CommHeartBeatPolicyHandler();
            fHeartBeatPolicy = new ProcessorOfHeartBeat(10000, 1000, fHeartBeatPolicyHandler);
        }
        try {
            fHeartBeatPolicy.start();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private synchronized void unInitialize() {
        if (isRunning() == false) return;
        setRunning(false);
        stopHeartBeatPolicy();
        if (fBthDataOutputStream != null) {
            fBthDataOutputStream.releaseResource();
            fBthDataOutputStream = null;
            fBthDataOutputStreamWithoutAck = null;
        }
        if (fCommRecvRunner != null) {
            fCommRecvRunner.cancel();
            fCommRecvRunner = null;
        }
        if (fAttributes.fBtSocket != null) {
            try {
                fAttributes.fBtSocket.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        fAttributes.fInformHandler.inform(IProtocolInformHandler.E_DISCONNECTION);
    }

    private int getHeaderLength() {
        return fCommPacket.getHeadlerLength();
    }

    private synchronized byte[] pack(byte[] aRawData, byte aSlicerStatus) {
        fChannelCurrentSent.fSliceStatus = aSlicerStatus;
        if (aRawData == null) return fCommPacket.makePacketWithoutData(fChannelCurrentSent); else return fCommPacket.makePacketWithData(fChannelCurrentSent, aRawData);
    }

    /**
	 * reply message without get a reply ACK
	 * @param aAppId
	 * @param aChannel
	 * @param aSN
	 * @param aType
	 * @return
	 */
    private synchronized boolean realReply(byte aAppId, byte aChannel, byte aSN, byte aType) {
        if (isRunning() == false) return false;
        fHeartBeatPolicy.pause();
        byte[] data = fCommPacket.makeReplyPacket(aAppId, aChannel, aSN, aType);
        boolean result = fBthDataOutputStreamWithoutAck.send(data);
        fHeartBeatPolicy.resume();
        if (result == false) {
            BthLog.e(TAG, "reply ERROR !!!!");
        }
        return result;
    }

    /**
	 * send data
	 * @param aStream the output stream of sending data
	 * @param aRawData raw data
	 * @param aChannel channel index used, it from 0 to 255
	 * @param aPacketType data type, to see {@link CommPacket}
	 * @return true if sent out the data without any error, otherwise false
	 */
    private synchronized boolean realSend(IBthDataOutputStream aStream, byte[] aRawData, int aChannel, byte aPacketType) {
        if (isRunning() == false) return false;
        fHeartBeatPolicy.pause();
        fChannelCurrentSent = fAttributes.getChannel(aChannel);
        fChannelCurrentSent.fPacketType = aPacketType;
        boolean result = aStream.send(aRawData);
        fHeartBeatPolicy.resume();
        if (result == false) {
            BthLog.e(TAG, "SEND DATA ERROR !!!!");
            unInitialize();
        }
        return result;
    }

    private void stopHeartBeatPolicy() {
        BthLog.i(TAG, "enter stopHeartBeatPolicy ...");
        if (fHeartBeatPolicy.isStopped() == false) {
            try {
                fHeartBeatPolicy.stop();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        BthLog.i(TAG, "leave stopHeartBeatPolicy ...");
    }

    private synchronized boolean replyACK(Packet aPacket) {
        boolean replyOk = false;
        replyOk = realReply(fParser.fPacket.fAppId, fParser.fPacket.fChannel, fParser.fPacket.fSN, CommPacket.PACKET_ACK);
        if (replyOk == false) {
            BthLog.d(TAG, "reply data ACK failed");
        } else {
            BthLog.d(TAG, "reply data ACK OK");
        }
        return replyOk;
    }

    private synchronized void processActiveDisconnection() {
        BthLog.e(TAG, "process Active Disconnection");
        fIsActiveDisconnection = true;
        realSend(fBthDataOutputStream, null, 0, CommPacket.PACKET_DISCONNECTION);
        fIsActiveDisconnection = false;
    }

    private synchronized void processPassiveDisconnection(Packet aPacket) {
        BthLog.e(TAG, "process Passive Disconnection");
        realReply(aPacket.fAppId, aPacket.fChannel, aPacket.fSN, CommPacket.PACKET_DISCONNECTION);
        unInitialize();
    }

    private synchronized void processActiveHeartBeatPolicy() {
        fHeartBeatPolicy.pause();
        boolean result = realSend(fBthDataOutputStream, null, 0, CommPacket.PACKET_HEART_BEAT);
        if (result == false) {
            BthLog.i(TAG, "send heart beat policy error");
            return;
        }
        BthLog.i(TAG, "leave heart beat policy");
        fHeartBeatPolicy.resume();
    }

    private synchronized void processPassiveHeartBeatPolicy(Packet aPacket) {
        replyACK(aPacket);
    }

    private boolean isReplyed(CommChannel aChannel, Packet aPacket) {
        if ((aChannel.fAppId == aPacket.fAppId) && (aChannel.fChannelIndex == aPacket.fChannel) && (aChannel.fCurrentSN == aPacket.fSN)) {
            return true;
        }
        return false;
    }

    private int processPacketData(byte[] aReadData, int aDataLength) {
        int startPos = 0;
        int headerLength = getHeaderLength();
        while (aDataLength > 0) {
            if (fParser.fParsedHeader == false) {
                if (aDataLength < headerLength) {
                    break;
                }
                if (fCommPacket.isHeaderValidate(aReadData, startPos) == false) {
                    startPos += headerLength;
                    aDataLength -= headerLength;
                    BthLog.e(TAG, "parcket header invalid");
                    continue;
                }
                fParser.fParsedHeader = true;
                fParser.fPacket = null;
                fParser.fPacket = fCommPacket.setHeaderInfo(aReadData, startPos);
                fParser.fType = CommPacket.getPacketType(fParser.fPacket);
                fParser.fDataLength = (fParser.fPacket.fDatalenHigh << 8) + fParser.fPacket.fDataLenLow;
                if (fParser.fDataLength == 0) {
                    if (CommPacket.PACKET_ACK == fParser.fType) {
                        if (isReplyed(fChannelCurrentSent, fParser.fPacket)) {
                            BthLog.d(TAG, "get a ACK packet");
                            fSendAckHandler.onGetAck();
                        } else {
                            BthLog.i(TAG, "error: get a repetable ACK packet");
                        }
                    } else if (CommPacket.PACKET_DISCONNECTION == fParser.fType) {
                        if (fIsActiveDisconnection == true) {
                            fSendAckHandler.onGetAck();
                            BthLog.d(TAG, "acitve disconnection get replyed disconnection packet");
                        } else {
                            processPassiveDisconnection(fParser.fPacket);
                            BthLog.d(TAG, "get a disconnection packet");
                        }
                    } else if (CommPacket.PACKET_HEART_BEAT == fParser.fType) {
                        processPassiveHeartBeatPolicy(fParser.fPacket);
                        BthLog.d(TAG, "get a hear beat packet");
                    } else if (CommPacket.PACKET_DATA == fParser.fType) {
                        BthLog.e(TAG, "get a DATA packet type, but it's length IS ZERO");
                    } else {
                        BthLog.e(TAG, "unKnown packet type");
                    }
                    fParser.fParsedHeader = false;
                    startPos += headerLength;
                    aDataLength -= headerLength;
                    continue;
                }
                fParser.fTotalLength = fParser.fDataLength + headerLength + 1;
            }
            if (CommPacket.PACKET_DATA != fParser.fType) {
                BthLog.e(TAG, "get a NON-DATA packet type, but it's data length is NOT ZERO");
                fParser.fParsedHeader = false;
                startPos += fParser.fTotalLength;
                aDataLength -= fParser.fTotalLength;
                continue;
            }
            if (aDataLength >= fParser.fTotalLength) {
                if (fChannelLastRecvData == null) {
                    fChannelLastRecvData = new CommChannel();
                    fChannelLastRecvData.fCurrentSN = (byte) (fParser.fPacket.fSN - 0x01);
                }
                if (ByteUtil.isEqual(fChannelLastRecvData.fCurrentSN, fParser.fPacket.fSN) == true) {
                    BthLog.i(TAG, "receive a repeat data packet");
                } else {
                    fChannelLastRecvData.fCurrentSN = fParser.fPacket.fSN;
                    byte[] recvData = fCommPacket.getAppData(aReadData, startPos + headerLength, fParser.fDataLength);
                    if (recvData == null) {
                        BthLog.i(TAG, "received data's check sum is WRONG");
                    } else {
                        replyACK(fParser.fPacket);
                        fAttributes.fInformHandler.onDataReceived(recvData, fParser.fPacket.fChannel);
                        BthLog.d(TAG, "receive a data packet");
                    }
                }
                fParser.fParsedHeader = false;
                startPos += fParser.fTotalLength;
                aDataLength -= fParser.fTotalLength;
                continue;
            } else {
                break;
            }
        }
        if (aDataLength > 0) {
            System.arraycopy(aReadData, startPos, aReadData, 0, aDataLength);
        }
        return aDataLength;
    }

    private synchronized void processReceiveError(boolean aIsCancelled) {
        if (aIsCancelled == false) {
            fAttributes.fInformHandler.inform(IProtocolInformHandler.E_RECEIVE_ERROR);
            BthLog.e(TAG, "receiver thread exit with exception");
        } else {
            BthLog.e(TAG, "receiver thread exit normally");
        }
    }

    private synchronized boolean isRunning() {
        return fIsRunning;
    }

    private synchronized void setRunning(boolean aIsRunning) {
        fIsRunning = aIsRunning;
    }

    private static final String TAG = "bt.BthProCommCommonMode";

    private CommAttributes fAttributes;

    private boolean fIsRunning;

    private boolean fIsActiveDisconnection;

    private CommPacket fCommPacket;

    private CommChannel fChannelCurrentSent;

    private CommChannel fChannelLastRecvData;

    private IAckEventHandler fSendAckHandler;

    private IBthDataOutputStream fBthDataOutputStreamWithoutAck;

    private IBthDataOutputStream fBthDataOutputStream;

    private CommHeartBeatPolicyHandler fHeartBeatPolicyHandler;

    private CommSlicable fCommSlicable;

    private CommReceiverRunnable fCommRecvRunner;

    private IProcessorOfHeartBeat fHeartBeatPolicy;

    private PacketParser fParser;

    private class CommHeartBeatPolicyHandler implements IHeartBeatEventHandler {

        @Override
        public void inform(int aEventType) {
            if (aEventType == IHeartBeatEventHandler.ETYPE_CAN_SEND_PACKET) {
                CommPolicyOfCommonMode.this.processActiveHeartBeatPolicy();
            }
        }
    }

    /**
	 * a call back handler
	 */
    private class CommSlicable implements IBthDataSlicableProtocol {

        @Override
        public byte[] getPacket(byte[] aRawData, byte aSlicerStatus) {
            return CommPolicyOfCommonMode.this.pack(aRawData, aSlicerStatus);
        }
    }

    private class CommReceiverRunnable implements Runnable {

        CommReceiverRunnable(InputStream aBtStream) {
            fCancelled = false;
            fBtSocketInputStream = aBtStream;
        }

        @Override
        public void run() {
            int bufLength = 4096;
            byte[] readBuf = new byte[bufLength];
            int realRead = 0;
            int totalRead = 0;
            int headerLength = CommPolicyOfCommonMode.this.getHeaderLength();
            while (fCancelled == false) {
                try {
                    realRead = fBtSocketInputStream.read(readBuf, totalRead, bufLength - totalRead);
                    totalRead += realRead;
                    BthLog.d(CommPolicyOfCommonMode.TAG, "length before process : " + totalRead);
                    if (totalRead >= headerLength) {
                        totalRead = CommPolicyOfCommonMode.this.processPacketData(readBuf, totalRead);
                        BthLog.d(CommPolicyOfCommonMode.TAG, "length after process  : " + totalRead);
                    }
                } catch (Exception exception) {
                    if (fCancelled == false) {
                        BthLog.e(TAG, "receiver thread exception", exception);
                    }
                    CommPolicyOfCommonMode.this.processReceiveError(fCancelled);
                    break;
                }
            }
        }

        public void cancel() {
            fCancelled = true;
        }

        private boolean fCancelled;

        private InputStream fBtSocketInputStream;
    }
}
