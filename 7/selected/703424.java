package eu.mpower.framework.sensor.fsa.adapter.eibnet;

import basys.eib.EIBFrame;
import basys.eib.EIBGrpaddress;
import basys.eib.EIBPhaddress;
import basys.eib.exceptions.EIBAddressFormatException;
import eu.mpower.framework.sensor.fsa.core.Adapter;
import eu.mpower.framework.sensor.fsa.core.Sensor;
import java.io.Serializable;
import tuwien.auto.eibxlator.*;
import tuwien.auto.eicl.util.EICLException;

/**
 * This class is in charge of communicate with the KNX sensors. This class implements the EIBnet protocol
 * and the KNX sensors use it to recive sensorial data and to act with the actuators.
 *
 * @author Emilio Lorente Ramon
 * @version 0.1
 */
public class AdapterEIBNET extends Adapter implements Serializable {

    private String connectionIPAddress;

    private EIBnetConnection tunnel;

    protected transient PointPDUXlator pointPDUXlator;

    /**
     * This method returns the IP of the phisycal EIBnet Gateway.
     * @return Returns a IP address like "200.12.23.12".
     */
    public String getConnectionIPAddress() {
        return connectionIPAddress;
    }

    /**
     * This method change the IP of the phisycal EIBnet Gateway.
     * @param IPAddress The IP address of the EIBNET Gateway like "200.12.23.12".
     */
    public void setConnectionIPAddress(String IPAddress) {
        this.connectionIPAddress = IPAddress;
    }

    /**
     * This method start the conection with the physical channel for this adapter.
     */
    public void connect() {
        if (!isConnected() && connectionIPAddress != null && sensor != null) {
            tunnel = EIBnetConnectionFactory.getConnection(connectionIPAddress, this);
            tunnel.setConnected(false);
            tunnel.connect();
            setConnected(tunnel.isConnected());
            sensor.onStartConnection(this);
        }
    }

    /**
     * This method stop the conection with the physical channel for this adapter.
     */
    public void disconnect() {
        EIBnetConnectionFactory.closeConnection(tunnel, this);
        setConnected(false);
    }

    /**
     * This method start the adapter and try to connect to the phisycal channel.
     *
     * @param sensor The sensor associated to this adapter.
     */
    public void start(Sensor sensor) {
        this.sensor = sensor;
        connect();
    }

    /**
     * This method stop the adapter and try to disconnect from the channel.
     *
     */
    public void stop() {
        disconnect();
    }

    /**
     * This method read from the adapter the values of the especific sensor attached to this adapter.
     * This method creates and sends a EIBframe to make a query to the phisical sensor.
     *
     * @exception EIBAddressFormatException
     */
    public void read() {
        if (isConnected() == true) {
            if (pointPDUXlator != null) {
                try {
                    int[] data = { 0 };
                    EIBFrame eibframe = new EIBFrame(false, 0, new EIBPhaddress(0, 0, 0), new EIBGrpaddress(sensor.getIDDevice()), 6, 0, 128, data);
                    eibframe.setAPCI(0);
                    eibframe.setPriority(3);
                    tunnel.sendEIBFrame(eibframe);
                } catch (EIBAddressFormatException ex) {
                    System.err.println("EIBAddress of the sensor(IDDevice) is not correct.");
                }
            }
        }
    }

    /**
     * This method send a EIBframe to change the value of a phisycal sensor. It creates the EIBframe
     * in a specific format to make possible the actuation over the sensor.
     *
     * @param Message The information that sends the adapter to the channel.
     * @exception EIBAddressFormatException
     * @exception EICLException
     */
    public void write(String Message) {
        if (isConnected() == true) {
            if (pointPDUXlator != null) {
                try {
                    pointPDUXlator.setServiceType(PointPDUXlator.A_GROUPVALUE_WRITE);
                    pointPDUXlator.setASDUfromString(Message);
                    int[] data = CalimeroFrame2BasysFrame(pointPDUXlator);
                    EIBFrame eibframe = new EIBFrame(false, 0, new EIBPhaddress(0, 0, 0), new EIBGrpaddress(sensor.getIDDevice()), 6, 0, 128, data);
                    eibframe.setAPCI(128);
                    eibframe.setPriority(3);
                    tunnel.sendEIBFrame(eibframe);
                } catch (EIBAddressFormatException ex) {
                    System.err.println("EIBAddress of the sensor(IDDevice) is not correct.");
                } catch (EICLException ex) {
                    System.err.println("EIBFrame not correclty formed. Take a look to the pointPDUXlator " + "from the sensor attached to this adapter.");
                }
            }
        }
    }

    /**
     * This method converts the calimero frame into a basys frame, because the calimero frame doesn't works with
     * the EIBconnection but deals with the data in a better way. To send frames to the gateway is used the basys frame
     * and to deal with the data is used the calimero frame.
     *
     * @param calimeroFrame The calimero frame to convert.
     *
     * @return Returns the basys frame in a int array.
     *
     */
    public int[] CalimeroFrame2BasysFrame(PointPDUXlator calimeroFrame) {
        byte[] cali = calimeroFrame.getAPDUByteArray();
        int[] basys = new int[cali.length - 1];
        basys[0] = cali[1] + 128;
        int j = 2;
        for (int i = 1; i < basys.length; i++) {
            basys[i] = cali[i + 1];
            j++;
        }
        return basys;
    }

    /**
     * This method converts the basys frame into a calimero frame, because the calimero frame doesn't works with
     * the EIBconnection but deals with the data in a better way. To send frames to the gateway is used the basys frame
     * and to deal with the data is used the calimero frame.
     *
     * @param basysFrame The basys frame to convert.
     *
     * @return Returns the calimero frame in a byte array.
     *
     */
    public byte[] BasysFrame2CalimeroFrame(EIBFrame basysFrame) {
        int[] aux = basysFrame.getApdata();
        byte[] aux2 = new byte[aux.length + 1];
        aux2[0] = 0;
        aux2[1] = (byte) (aux[0] - 128);
        int j = 2;
        for (int i = 1; i < aux.length; i++) {
            aux2[j] = (byte) aux[i];
            j++;
        }
        return aux2;
    }

    /**
     * This method is invoked by the connection to send the EIBFrame to the adapter. When the adapter receives
     * a frame it sends to the attached sensor.
     *
     * @param eibFrame The frame received from the EIBConnection.
     *
     */
    public void packetReceived(EIBFrame eibFrame) {
        if (sensor != null && pointPDUXlator != null) {
            try {
                pointPDUXlator.setAPDUByteArray(BasysFrame2CalimeroFrame(eibFrame));
                sensor.onMessage(pointPDUXlator.getASDUasString().getBytes(), pointPDUXlator.getASDUasString().length(), "Data from konnex", this);
            } catch (EICLException ex) {
                System.err.println("EIBFrame received isn't correclty formed. Take a look to the pointPDUXlator " + "from the sensor attached to this adapter.");
            }
        }
    }

    /**
     * This method set the type of the message that the adapter has to send or receive.
     * For example a simple sensor of a door/window that only sends open or close, has a type of boolean
     * and a value of a open/close.
     *
     * @param point The point value with is going to change the pointPDUXlator of the adapter.
     */
    public void setPointPDUXlator(PointPDUXlator point) {
        pointPDUXlator = point;
    }
}
