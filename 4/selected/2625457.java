package net.sf.wubiq.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import net.sf.wubiq.common.ParameterKeys;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;

/**
 * Handles the necessary steps for printing on the client.
 * @author Federico Alcantara
 *
 */
public enum PrintClientUtils {

    INSTANCE;

    private static final String TAG = "PrintClientUtils";

    /**
	 * Prints the given input to the device, performing all required conversion steps.
	 * @param context Android context.
	 * @param deviceName Complete device name.
	 * @param input Input data as a stream
	 */
    public void print(Context context, String deviceName, InputStream input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String[] deviceData = deviceName.split(ParameterKeys.ATTRIBUTE_SET_SEPARATOR);
        MobileDeviceInfo deviceInfo = MobileDevices.INSTANCE.getDevices().get(deviceData[2]);
        String deviceAddress = deviceData[1];
        try {
            byte[] b = new byte[16 * 1024];
            int read;
            while ((read = input.read(b)) != -1) {
                output.write(b, 0, read);
            }
            byte[] printData = output.toByteArray();
            for (MobileClientConversionStep step : deviceInfo.getClientSteps()) {
                if (step.equals(MobileClientConversionStep.OUTPUT_BYTES)) {
                    printBytes(deviceInfo, deviceAddress, printData);
                } else if (step.equals(MobileClientConversionStep.OUTPUT_SM_BYTES)) {
                    printStarMicronicsByteArray(deviceInfo, deviceAddress, printData);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Outputs to a star micronics portable printer.
	 * @param deviceInfo Device information
	 * @param deviceAddress Device address (mac address)
	 * @param printData Data to print
	 * @return true if everything is okey.
	 */
    private boolean printStarMicronicsByteArray(MobileDeviceInfo deviceInfo, String deviceAddress, byte[] printData) {
        StarIOPort port = null;
        try {
            port = StarIOPort.getPort("bt:" + deviceAddress, "mini", 10000);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            port.writePort(printData, 0, printData.length);
            try {
                Thread.sleep(3000);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (port != null) {
                try {
                    StarIOPort.releasePort(port);
                } catch (StarIOPortException e) {
                }
            }
        }
        return false;
    }

    /**
	 * Print bytes creating a basic bluetooth connection
	 * @param deviceInfo Device to be connected to.
	 * @param deviceAddress Address of the device
	 * @param printData Data to be printed.
	 * @return true if printing was okey.
	 */
    private boolean printBytes(MobileDeviceInfo deviceInfo, String deviceAddress, byte[] printData) {
        boolean returnValue = false;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if (device.getAddress().equals(deviceAddress)) {
                Thread connectThread = new ConnectThread(device, UUID.randomUUID(), printData);
                connectThread.start();
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    /**
	 * Private class for handling connection.
	 * @author Federico Alcantara
	 *
	 */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;

        private byte[] printData;

        public ConnectThread(BluetoothDevice device, UUID uuid, byte[] printData) {
            BluetoothSocket tmp = null;
            this.printData = printData;
            try {
                Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
                tmp = (BluetoothSocket) m.invoke(device, 1);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.d(TAG, closeException.getMessage());
                }
                return;
            }
            ConnectedThread connectedThread = new ConnectedThread(mmSocket, printData);
            connectedThread.start();
        }
    }

    private class ConnectedThread extends Thread {

        private final OutputStream mmOutStream;

        private byte[] printData;

        public ConnectedThread(BluetoothSocket socket, byte[] printData) {
            this.printData = printData;
            OutputStream tmpOut = null;
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            mmOutStream = tmpOut;
        }

        public void run() {
            try {
                mmOutStream.write(printData);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
