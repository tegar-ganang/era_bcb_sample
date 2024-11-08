package javax.microedition.sensor;

import com.sun.javame.sensor.TestingSensor;
import com.sun.midp.i3test.TestCase;

public class TestSensorManager extends TestCase {

    private static final SensorInfo DUMMY_SENSOR_INFO = new SensorInfo() {

        public ChannelInfo[] getChannelInfos() {
            return null;
        }

        public int getConnectionType() {
            return 0;
        }

        public String getContextType() {
            return null;
        }

        public String getDescription() {
            return null;
        }

        public int getMaxBufferSize() {
            return 0;
        }

        public String getModel() {
            return null;
        }

        public Object getProperty(String name) {
            return null;
        }

        public String[] getPropertyNames() {
            return null;
        }

        public String getQuantity() {
            return null;
        }

        public String getUrl() {
            return null;
        }

        public boolean isAvailabilityPushSupported() {
            return false;
        }

        public boolean isAvailable() {
            return false;
        }

        public boolean isConditionPushSupported() {
            return false;
        }
    };

    private static final SensorListener DUMMY_SENSOR_LISTENER = new SensorListener() {

        public void sensorAvailable(SensorInfo info) {
        }

        public void sensorUnavailable(SensorInfo info) {
        }
    };

    private void testAddSensorListenerThrowsNullPointerException() {
        assertTrue(true);
        try {
            SensorManager.addSensorListener(null, (SensorInfo) null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignore) {
        }
        try {
            SensorManager.addSensorListener(null, DUMMY_SENSOR_INFO);
            fail("NullPointerException expected");
        } catch (NullPointerException ignore) {
        }
        try {
            SensorManager.addSensorListener(DUMMY_SENSOR_LISTENER, (SensorInfo) null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignore) {
        }
        try {
            SensorManager.addSensorListener(null, (String) null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignore) {
        }
        try {
            SensorManager.addSensorListener(DUMMY_SENSOR_LISTENER, (String) null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignore) {
        }
        try {
            SensorManager.addSensorListener(null, "");
            fail("NullPointerException expected");
        } catch (NullPointerException ignore) {
        }
    }

    private void testAddSensorListenerThrowsIllegalArgumentException() {
        assertTrue(true);
        try {
            SensorManager.addSensorListener(DUMMY_SENSOR_LISTENER, DUMMY_SENSOR_INFO);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void testRemoveSensorListenerThrowsNull() {
        assertTrue(true);
        try {
            SensorManager.removeSensorListener(null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignore) {
        }
    }

    private void testRemoveNonExistingSensorListener() {
        assertTrue(true);
        SensorManager.removeSensorListener(DUMMY_SENSOR_LISTENER);
        SensorManager.removeSensorListener(DUMMY_SENSOR_LISTENER);
    }

    class MockSensorListener implements SensorListener {

        private int available;

        private int unavailable;

        private boolean isavailable;

        public void sensorAvailable(SensorInfo info) {
            incAvailable();
        }

        public void sensorUnavailable(SensorInfo info) {
            incUnAvailable();
        }

        private synchronized void incAvailable() {
            available++;
            isavailable = true;
        }

        private synchronized void incUnAvailable() {
            unavailable++;
            isavailable = false;
        }

        private int expectedAvailable;

        private int expectedUnavailable;

        private boolean expectedIsavailable;

        synchronized void validate() {
            assertEquals("Available calls count should equal", expectedAvailable, available);
            assertEquals("Unavailable calls count should equal", expectedUnavailable, unavailable);
            assertTrue("Availability should equal", expectedIsavailable == isavailable);
        }

        public synchronized void expectCallback(boolean available) {
            if (available) {
                expectedAvailable++;
            } else {
                expectedUnavailable++;
            }
            expectedIsavailable = available;
        }
    }

    class MalliciousMockSensorListener extends MockSensorListener {

        /** To avoid throwing from the addSensorListener method. */
        private volatile boolean firstTime = true;

        public void sensorAvailable(SensorInfo info) {
            super.sensorAvailable(info);
            if (!firstTime) {
                throw new RuntimeException("Mallicious throw");
            }
        }

        public void sensorUnavailable(SensorInfo info) {
            super.sensorUnavailable(info);
            if (!firstTime) {
                throw new RuntimeException("Mallicious throw");
            }
            firstTime = false;
        }
    }

    private void testMultipleAddSensorListenerCalls(boolean available, MockSensorListener listener) {
        SensorInfo[] infos = SensorManager.findSensors("sensor:sensor_tester");
        assertTrue(infos.length > 0);
        TestingSensor sensor = TestingSensor.tryGetInstance();
        listener.validate();
        sensor.setAvailable(available);
        assertTrue(available == infos[0].isAvailable());
        listener.validate();
        try {
            SensorManager.addSensorListener(listener, infos[0]);
            listener.expectCallback(available);
            listener.validate();
            SensorManager.addSensorListener(listener, infos[0]);
            listener.validate();
        } finally {
            SensorManager.removeSensorListener(listener);
        }
    }

    private void testSwapAvailabilityState(long timeout, boolean async, boolean nativeAsync, MockSensorListener listener) throws InterruptedException {
        SensorInfo[] infos = SensorManager.findSensors("sensor:sensor_tester");
        assertTrue(infos.length > 0);
        TestingSensor sensor = TestingSensor.tryGetInstance();
        sensor.setNotificationEnabled(async);
        sensor.setNativeAvailability(nativeAsync);
        boolean state = false;
        listener.validate();
        try {
            SensorManager.addSensorListener(listener, infos[0]);
            listener.expectCallback(state);
            for (int i = 0; i < 10; i++) {
                listener.validate();
                state = !state;
                sensor.setAvailable(state);
                assertTrue("Info should be updated immediately", state == infos[0].isAvailable());
                listener.expectCallback(state);
                Thread.sleep(timeout);
            }
        } finally {
            SensorManager.removeSensorListener(listener);
        }
        for (int i = 0; i < 10; i++) {
            sensor.setAvailable(!infos[0].isAvailable());
        }
        Thread.sleep(timeout);
        listener.validate();
    }

    public void runTests() throws Exception {
        declare("testAddSensorListenerThrowsNullPointerException");
        testAddSensorListenerThrowsNullPointerException();
        declare("testAddSensorListenerThrowsIllegalArgumentException");
        testAddSensorListenerThrowsIllegalArgumentException();
        declare("testRemoveSensorListenerThrowsNull");
        testRemoveSensorListenerThrowsNull();
        declare("testRemoveNonExistingSensorListener");
        testRemoveNonExistingSensorListener();
        declare("testMultipleAddSensorListenerCalls: available=true, mallicious");
        testMultipleAddSensorListenerCalls(true, new MalliciousMockSensorListener());
        declare("testMultipleAddSensorListenerCalls: available=false, mallicious");
        testMultipleAddSensorListenerCalls(false, new MalliciousMockSensorListener());
        declare("testSwapAvailabilityState: sync, mallicious");
        testSwapAvailabilityState(2000, false, false, new MalliciousMockSensorListener());
        declare("testSwapAvailabilityState: async, nativeAsync=false, mallicious");
        testSwapAvailabilityState(100, true, false, new MalliciousMockSensorListener());
        declare("testSwapAvailabilityState: async, nativeAsync=true, mallicious");
        testSwapAvailabilityState(100, true, true, new MalliciousMockSensorListener());
        declare("testMultipleAddSensorListenerCalls: available=true");
        testMultipleAddSensorListenerCalls(true, new MockSensorListener());
        declare("testMultipleAddSensorListenerCalls: available=false");
        testMultipleAddSensorListenerCalls(false, new MockSensorListener());
        declare("testSwapAvailabilityState: sync");
        testSwapAvailabilityState(2000, false, false, new MockSensorListener());
        declare("testSwapAvailabilityState: async, nativeAsync=false");
        testSwapAvailabilityState(100, true, false, new MockSensorListener());
        declare("testSwapAvailabilityState: async, nativeAsync=true");
        testSwapAvailabilityState(100, true, true, new MockSensorListener());
    }
}
