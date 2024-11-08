package com.intel.bluetooth;

import java.io.IOException;
import java.io.InterruptedIOException;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;

/**
 * @author vlads
 * 
 */
class BluetoothL2CAPConnectionNotifier extends BluetoothConnectionNotifierBase implements L2CAPConnectionNotifier {

    private int psm = -1;

    public BluetoothL2CAPConnectionNotifier(BluetoothStack bluetoothStack, BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU) throws IOException {
        super(bluetoothStack, params);
        this.handle = bluetoothStack.l2ServerOpen(params, receiveMTU, transmitMTU, serviceRecord);
        this.psm = serviceRecord.getChannel(BluetoothConsts.L2CAP_PROTOCOL_UUID);
        this.serviceRecord.attributeUpdated = false;
        this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);
        this.connectionCreated();
    }

    public L2CAPConnection acceptAndOpen() throws IOException {
        if (closed) {
            throw new IOException("Notifier is closed");
        }
        updateServiceRecord(true);
        try {
            long clientHandle = bluetoothStack.l2ServerAcceptAndOpenServerConnection(handle);
            int clientSecurityOpt = bluetoothStack.l2GetSecurityOpt(clientHandle, this.securityOpt);
            return new BluetoothL2CAPServerConnection(bluetoothStack, clientHandle, clientSecurityOpt);
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            if (closed) {
                throw new InterruptedIOException("Notifier has been closed; " + e.getMessage());
            }
            throw e;
        }
    }

    protected void stackServerClose(long handle) throws IOException {
        bluetoothStack.l2ServerClose(handle, serviceRecord);
    }

    protected void validateServiceRecord(ServiceRecord srvRecord) {
        if (this.psm != serviceRecord.getChannel(BluetoothConsts.L2CAP_PROTOCOL_UUID)) {
            throw new IllegalArgumentException("Must not change the PSM");
        }
        super.validateServiceRecord(srvRecord);
    }

    protected void updateStackServiceRecord(ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
        bluetoothStack.l2ServerUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
    }
}
