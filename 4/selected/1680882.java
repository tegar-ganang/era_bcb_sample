package com.intel.bluetooth;

import java.io.IOException;

/**
 * 
 */
class EmulatorRFCOMMService extends EmulatorServiceConnection {

    private int channel;

    EmulatorRFCOMMService(EmulatorLocalDevice localDevice, long handle, int channel) {
        super(localDevice, handle);
        this.channel = channel;
    }

    void open(BluetoothConnectionNotifierParams params) throws IOException {
        this.params = params;
        localDevice.getDeviceManagerService().rfOpenService(localDevice.getAddress(), this.channel);
    }

    /**
	 * 
	 * @return connectionHandle on server
	 * @throws IOException
	 */
    long accept() throws IOException {
        return localDevice.getDeviceManagerService().rfAccept(localDevice.getAddress(), this.channel, this.params.authenticate, this.params.encrypt);
    }

    int getChannel() {
        return channel;
    }

    void close(ServiceRecordImpl serviceRecord) throws IOException {
        localDevice.getDeviceManagerService().removeServiceRecord(localDevice.getAddress(), serviceRecord.getHandle());
        localDevice.getDeviceManagerService().rfCloseService(localDevice.getAddress(), channel);
    }
}
