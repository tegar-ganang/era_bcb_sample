package integration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import ru.cos.sim.communication.FrameProperties;
import ru.cos.sim.engine.TrafficModelDefinition;
import ru.cos.sim.engine.TrafficSimulationEngine;
import ru.cos.sim.mdf.MDFReader;
import ru.cos.sim.road.objects.RoadObject;
import ru.cos.sim.vehicle.Vehicle;

public class OriginAndDestTest {

    @Test
    public void test() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("originAndDestination.xml");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(is, byteArrayOutputStream);
        TrafficModelDefinition def = MDFReader.read(byteArrayOutputStream.toByteArray());
        TrafficSimulationEngine se = new TrafficSimulationEngine();
        se.init(def);
        int linkId = 2;
        int segmentId = 0;
        Map<Integer, Set<Integer>> linkSegments = new HashMap<Integer, Set<Integer>>();
        Set<Integer> segments = new HashSet<Integer>();
        segments.add(segmentId);
        linkSegments.put(linkId, segments);
        FrameProperties frameProperties = new FrameProperties(linkSegments, new HashSet<Integer>());
        se.setFrameProperties(frameProperties);
        for (float time = 0; time < 60 * 10; time += 0.1f) {
            se.step(0.1f);
            for (RoadObject vehicle : se.getDynamicObjects()) {
                System.out.println(time + ": X=" + vehicle.getPosition() + "\tV=" + vehicle.getSpeed());
            }
        }
    }
}
