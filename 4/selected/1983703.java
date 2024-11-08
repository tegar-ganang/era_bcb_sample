package ioio.lib.impl;

import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.impl.FlowControlledPacketSender.Packet;
import ioio.lib.impl.FlowControlledPacketSender.Sender;
import ioio.lib.impl.IncomingState.DataModuleListener;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.util.Log;

public class TwiMasterImpl extends AbstractResource implements TwiMaster, DataModuleListener, Sender {

    class TwiResult implements Result {

        boolean ready_ = false;

        boolean success_;

        final byte[] data_;

        public TwiResult(byte[] data) {
            data_ = data;
        }

        @Override
        public synchronized boolean waitReady() throws ConnectionLostException, InterruptedException {
            while (!ready_ && state_ != State.DISCONNECTED) {
                wait();
            }
            checkState();
            return success_;
        }
    }

    class OutgoingPacket implements Packet {

        int writeSize_;

        byte[] writeData_;

        boolean tenBitAddr_;

        int addr_;

        int readSize_;

        @Override
        public int getSize() {
            return writeSize_ + 4;
        }
    }

    private final Queue<TwiResult> pendingRequests_ = new ConcurrentLinkedQueue<TwiMasterImpl.TwiResult>();

    private final FlowControlledPacketSender outgoing_ = new FlowControlledPacketSender(this);

    private final int twiNum_;

    TwiMasterImpl(IOIOImpl ioio, int twiNum) throws ConnectionLostException {
        super(ioio);
        twiNum_ = twiNum;
    }

    @Override
    public synchronized void disconnected() {
        super.disconnected();
        outgoing_.kill();
        for (TwiResult tr : pendingRequests_) {
            synchronized (tr) {
                tr.notify();
            }
        }
    }

    @Override
    public boolean writeRead(int address, boolean tenBitAddr, byte[] writeData, int writeSize, byte[] readData, int readSize) throws ConnectionLostException, InterruptedException {
        Result result = writeReadAsync(address, tenBitAddr, writeData, writeSize, readData, readSize);
        return result.waitReady();
    }

    @Override
    public Result writeReadAsync(int address, boolean tenBitAddr, byte[] writeData, int writeSize, byte[] readData, int readSize) throws ConnectionLostException {
        checkState();
        TwiResult result = new TwiResult(readData);
        OutgoingPacket p = new OutgoingPacket();
        p.writeSize_ = writeSize;
        p.writeData_ = writeData;
        p.tenBitAddr_ = tenBitAddr;
        p.readSize_ = readSize;
        p.addr_ = address;
        synchronized (this) {
            pendingRequests_.add(result);
            try {
                outgoing_.write(p);
            } catch (IOException e) {
                Log.e("SpiMasterImpl", "Exception caught", e);
            }
        }
        return result;
    }

    @Override
    public void dataReceived(byte[] data, int size) {
        TwiResult result = pendingRequests_.remove();
        synchronized (result) {
            result.ready_ = true;
            result.success_ = (size != 0xFF);
            if (result.success_) {
                System.arraycopy(data, 0, result.data_, 0, size);
            }
            result.notify();
        }
    }

    @Override
    public void reportAdditionalBuffer(int bytesRemaining) {
        outgoing_.readyToSend(bytesRemaining);
    }

    @Override
    public synchronized void close() {
        super.close();
        outgoing_.close();
        ioio_.closeTwi(twiNum_);
    }

    @Override
    public void send(Packet packet) {
        OutgoingPacket p = (OutgoingPacket) packet;
        try {
            ioio_.protocol_.i2cWriteRead(twiNum_, p.tenBitAddr_, p.addr_, p.writeSize_, p.readSize_, p.writeData_);
        } catch (IOException e) {
            Log.e("TwiImpl", "Caught exception", e);
        }
    }
}
