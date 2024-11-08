package test.jhomenet.server.persistence;

import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import jhomenet.commons.hw.HomenetHardware;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.hw.sensor.*;
import jhomenet.commons.polling.PollingIntervals;
import jhomenet.server.persistence.HardwarePersistenceFacadeHibernate;
import jhomenet.server.hw.sensor.TempSensor;

/**
 * TODO: Class description.
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class HardwarePersistenceHibernateTest {

    /**
     * 
     */
    private ValueSensor mockTempSensor;

    /**
     * 
     */
    private static HardwarePersistenceFacadeHibernate persistenceLayer;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        persistenceLayer = HardwarePersistenceFacadeHibernate.instance();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.mockTempSensor = new TempSensor("0001", "Mock temperature sensor #1");
        this.mockTempSensor.setChannelDescription(new Integer(0), "Test channel #0");
        this.mockTempSensor.setPollingInterval(PollingIntervals.THIRTY_SECOND);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * 
     */
    @Test
    public void persistTemperatureSensorTest() {
        this.mockTempSensor = (ValueSensor) persistenceLayer.storeHardware(this.mockTempSensor);
        if (this.mockTempSensor != null) {
            System.out.println("Mock sensor not null after persisting!");
        } else {
            System.out.println("Null sensor after persisting.");
        }
    }

    /**
     * 
     */
    @Test
    public void retrieveTemperatureSensorTest() {
        Collection<RegisteredHardware> hwList = persistenceLayer.retrieveRegisteredHardware();
        if (hwList != null) {
            System.out.println("Hardware list IS NOT null!");
            System.out.println("List size: " + hwList.size());
            for (RegisteredHardware hw : hwList) {
                System.out.println("Hardware:");
                System.out.println("  Hardware addr: " + hw.getHardwareAddr());
                System.out.println("  App desc: " + hw.getAppHardwareDescription());
                System.out.println("  Setup desc: " + hw.getHardwareSetupDescription());
                if (hw instanceof HomenetHardware) {
                    System.out.println("  # I/O channels: " + ((HomenetHardware) hw).getNumChannels());
                    for (int i = 0; i < ((HomenetHardware) hw).getNumChannels(); i++) System.out.println("    CH-" + i + ": " + ((HomenetHardware) hw).getChannelDescription(i));
                }
                if (hw instanceof Sensor) System.out.println("  Polling interval: " + ((Sensor) hw).getPollingInterval().toString());
            }
        } else {
            System.out.println("Hardware list IS null.");
        }
    }
}
