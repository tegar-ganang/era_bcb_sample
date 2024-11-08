package com.intel.bluetooth;

import java.io.IOException;
import java.io.InterruptedIOException;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

class BluetoothRFCommConnectionNotifier extends BluetoothConnectionNotifierBase implements StreamConnectionNotifier {

    private int rfcommChannel = -1;

    public BluetoothRFCommConnectionNotifier(BluetoothStack bluetoothStack, BluetoothConnectionNotifierParams params) throws IOException {
        super(bluetoothStack, params);
        this.handle = bluetoothStack.rfServerOpen(params, serviceRecord);
        this.rfcommChannel = serviceRecord.getChannel(BluetoothConsts.RFCOMM_PROTOCOL_UUID);
        this.serviceRecord.attributeUpdated = false;
        this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);
        this.connectionCreated();
    }

    protected void stackServerClose(long handle) throws IOException {
        bluetoothStack.rfServerClose(handle, serviceRecord);
    }

    public StreamConnection acceptAndOpen() throws IOException {
        if (closed) {
            throw new IOException("Notifier is closed");
        }
        updateServiceRecord(true);
        try {
            long clientHandle = bluetoothStack.rfServerAcceptAndOpenRfServerConnection(handle);
            int clientSecurityOpt = bluetoothStack.rfGetSecurityOpt(clientHandle, this.securityOpt);
            return new BluetoothRFCommServerConnection(bluetoothStack, clientHandle, clientSecurityOpt);
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            if (closed) {
                throw new InterruptedIOException("Notifier has been closed; " + e.getMessage());
            }
            throw e;
        }
    }

    protected void validateServiceRecord(ServiceRecord srvRecord) {
        if (this.rfcommChannel != serviceRecord.getChannel(BluetoothConsts.RFCOMM_PROTOCOL_UUID)) {
            throw new IllegalArgumentException("Must not change the RFCOMM server channel number");
        }
        super.validateServiceRecord(srvRecord);
    }

    protected void updateStackServiceRecord(ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
        bluetoothStack.rfServerUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
    }
}
