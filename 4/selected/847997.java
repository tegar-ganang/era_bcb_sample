package driver.composite.cases;

import static org.junit.Assert.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import ru.cos.sim.communication.FrameProperties;
import ru.cos.sim.driver.composite.CompositeDriver;
import ru.cos.sim.driver.composite.Perception;
import ru.cos.sim.driver.composite.cases.SafetyCase;
import ru.cos.sim.driver.composite.framework.HandRange;
import ru.cos.sim.driver.composite.framework.Priority;
import ru.cos.sim.driver.composite.framework.RectangleCCRange;
import ru.cos.sim.engine.TrafficModelDefinition;
import ru.cos.sim.engine.TrafficSimulationEngine;
import ru.cos.sim.mdf.MDFReader;
import ru.cos.sim.road.objects.BlockRoadObject;
import ru.cos.sim.utils.Hand;
import ru.cos.sim.vehicle.RegularVehicle;

/**
 * 
 * @author zroslaw
 */
public class SafetyCaseTest {

    /**
	 * @throws java.lang.Exception
	 */
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void checkForSafetyTest() {
        SafetyCase safety = new SafetyCase(null);
        Perception back;
        Perception front;
        front = new Perception(0, new BlockRoadObject());
        back = new Perception(0, new BlockRoadObject());
        assertTrue(!safety.checkForSafety(back, front));
        front = new Perception(-0.001f, new BlockRoadObject());
        back = new Perception(0.001f, new BlockRoadObject());
        assertTrue(!safety.checkForSafety(back, front));
        front = new Perception(1, new BlockRoadObject());
        back = new Perception(-1, new BlockRoadObject());
        assertTrue(safety.checkForSafety(back, front));
        front = new Perception(100, new BlockRoadObject());
        back = new Perception(-100, new BlockRoadObject());
        assertTrue(safety.checkForSafety(back, front));
        front = new Perception(1, new BlockRoadObject());
        back = new Perception(-1, new BlockRoadObject());
        assertTrue(safety.checkForSafety(back, null));
        front = new Perception(1, new BlockRoadObject());
        back = new Perception(-1, new BlockRoadObject());
        assertTrue(safety.checkForSafety(null, front));
        front = new Perception(1, new BlockRoadObject());
        back = new Perception(-1, new BlockRoadObject());
        assertTrue(safety.checkForSafety(null, null));
    }

    @Test
    public void behaveTest() {
        InputStream is = this.getClass().getResourceAsStream("safetyCaseTest.mdf");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(is, out);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read mdf", e);
        }
        TrafficSimulationEngine engine = new TrafficSimulationEngine();
        TrafficModelDefinition def = MDFReader.read(out.toByteArray());
        engine.init(def);
        Map<Integer, Set<Integer>> linkSegments = new HashMap<Integer, Set<Integer>>();
        Set<Integer> segments = new HashSet<Integer>();
        segments.add(0);
        linkSegments.put(0, segments);
        FrameProperties frameProperties = new FrameProperties(linkSegments, new HashSet<Integer>());
        engine.setFrameProperties(frameProperties);
        RegularVehicle vehicle = (RegularVehicle) engine.getDynamicObjects().iterator().next();
        CompositeDriver driver = (CompositeDriver) vehicle.getDriver();
        driver.drive(0.1f);
        SafetyCase safety = new SafetyCase(driver);
        RectangleCCRange ccRange = (RectangleCCRange) safety.behave(0.1f);
        HandRange turnRange = ccRange.getTurnRange();
        HandRange probeRange = new HandRange();
        probeRange.remove(Hand.Left);
        assertTrue(turnRange.equals(probeRange));
        assertTrue(ccRange.getPriority() == Priority.SafetyCase);
    }
}
