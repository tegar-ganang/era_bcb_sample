package com.sun.javame.sensor;

import com.sun.midp.i3test.TestCase;
import javax.microedition.sensor.*;
import javax.microedition.io.*;
import java.io.*;

public class TestAsyncSensor extends TestCase implements DataListener {

    private boolean isDataRead;

    private Data[] arrData;

    public synchronized void dataReceived(SensorConnection sensor, Data[] data, boolean isDataLost) {
        isDataRead = true;
        arrData = data;
        notify();
    }

    private synchronized void testAsyncRead(Object[] dataBuff, long interval, boolean isTimestampIncluded, boolean isUncertaintyIncluded, boolean isValidityIncluded, String uri) throws IOException {
        SensorConnection conn = (SensorConnection) Connector.open(uri);
        assertTrue(conn.getState() == SensorConnection.STATE_OPENED);
        TestChannelDevice channelDevice = (TestChannelDevice) (((Sensor) conn).getChannelDevice(0));
        assertTrue(channelDevice != null);
        channelDevice.setTestData(dataBuff);
        isDataRead = false;
        arrData = null;
        conn.setDataListener(this, dataBuff.length, interval, isTimestampIncluded, isUncertaintyIncluded, isValidityIncluded);
        try {
            wait(interval + 1000);
        } catch (InterruptedException ex) {
        }
        assertTrue(isDataRead);
        assertTrue(arrData != null);
        assertTrue(arrData.length > 0);
        double[] doubleResults = null;
        int[] intResults = null;
        Object[] objResults = null;
        int resultsLength = 0;
        int dataType = conn.getSensorInfo().getChannelInfos()[0].getDataType();
        switch(dataType) {
            case ChannelInfo.TYPE_INT:
                intResults = arrData[0].getIntValues();
                assertTrue(intResults != null);
                resultsLength = intResults.length;
                break;
            case ChannelInfo.TYPE_DOUBLE:
                doubleResults = arrData[0].getDoubleValues();
                assertTrue(doubleResults != null);
                resultsLength = doubleResults.length;
                break;
            case ChannelInfo.TYPE_OBJECT:
                objResults = arrData[0].getObjectValues();
                assertTrue(objResults != null);
                resultsLength = objResults.length;
                break;
        }
        assertTrue(resultsLength == dataBuff.length);
        for (int i = 0; i < resultsLength; i++) {
            switch(dataType) {
                case ChannelInfo.TYPE_INT:
                    assertTrue(intResults[i] == ((Integer) dataBuff[i]).intValue());
                    break;
                case ChannelInfo.TYPE_DOUBLE:
                    assertTrue(doubleResults[i] == ((Double) dataBuff[i]).doubleValue());
                    break;
                case ChannelInfo.TYPE_OBJECT:
                    break;
            }
            if (isTimestampIncluded) {
                try {
                    arrData[0].getTimestamp(i);
                    assertTrue(true);
                } catch (Throwable ex) {
                    fail("Unexpected exception " + ex);
                }
            } else {
                try {
                    arrData[0].getTimestamp(i);
                    fail("No IllegalStateException is throwed");
                } catch (IllegalStateException ex) {
                    assertTrue(true);
                } catch (Throwable ex) {
                    fail("Unexpected exception " + ex);
                }
            }
            if (isUncertaintyIncluded) {
                try {
                    arrData[0].getUncertainty(i);
                    assertTrue(true);
                } catch (Throwable ex) {
                    fail("Unexpected exception " + ex);
                }
            } else {
                try {
                    arrData[0].getUncertainty(i);
                    fail("No IllegalStateException is throwed");
                } catch (IllegalStateException ex) {
                    assertTrue(true);
                } catch (Throwable ex) {
                    fail("Unexpected exception " + ex);
                }
            }
            if (isValidityIncluded) {
                try {
                    arrData[0].isValid(i);
                    assertTrue(true);
                } catch (Throwable ex) {
                    fail("Unexpected exception " + ex);
                }
            } else {
                try {
                    arrData[0].isValid(i);
                    fail("No IllegalStateException is throwed");
                } catch (IllegalStateException ex) {
                    assertTrue(true);
                } catch (Throwable ex) {
                    fail("Unexpected exception " + ex);
                }
            }
        }
        conn.close();
    }

    public void runTests() {
        try {
            SensorTable table = new SensorTable();
            for (int i = 0; i < table.size(); i++) {
                declare("Test of asynchronous reading data");
                testAsyncRead(table.getData(i), 2000, false, false, false, table.getUrl(i));
                testAsyncRead(table.getData(i), 2000, true, true, true, table.getUrl(i));
            }
            ;
        } catch (Throwable t) {
            fail("" + t);
        }
    }
}
