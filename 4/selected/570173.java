package playground.gregor.snapshots.postprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.events.Events;
import org.matsim.events.EventsReaderTXTv1;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.world.World;
import playground.gregor.snapshots.postprocessor.processors.DestinationDependentColorizer;
import playground.gregor.snapshots.postprocessor.processors.EvacuationLinksTeleporter;
import playground.gregor.snapshots.postprocessor.processors.FloodlineGenerator;
import playground.gregor.snapshots.postprocessor.processors.PostProcessorI;
import playground.gregor.snapshots.postprocessor.processors.TimeDependentColorizer;
import playground.gregor.snapshots.postprocessor.processors.FloodlineGenerator.FloodEvent;

public class TransimsSnapshotFilePostProcessor {

    private static final Logger log = Logger.getLogger(TransimsSnapshotFilePostProcessor.class);

    private static final double MAX_HEIGHT = 2.0;

    private static final double MIN_WALKABLE = 0.2;

    private final TransimsSnapshotFileReader reader;

    private final TransimsSnapshotFileWriter writer;

    private final List<PostProcessorI> processors;

    private FloodlineGenerator floodlineGenerator = null;

    public TransimsSnapshotFilePostProcessor(final String eventsfile, final String tVehFile) {
        this.reader = new TransimsSnapshotFileReader(tVehFile);
        TimeDependentColorizer tdc = new TimeDependentColorizer();
        DestinationDependentColorizer ddc = new DestinationDependentColorizer();
        Events events = new Events();
        events.addHandler(tdc);
        events.addHandler(ddc);
        new EventsReaderTXTv1(events).readFile(eventsfile);
        String outfile = "../outputs/output/colorizedT.veh.txt";
        this.writer = new TransimsSnapshotFileWriter(outfile);
        this.floodlineGenerator = new FloodlineGenerator("../inputs/networks/padang_flooding.txt.gz");
        this.processors = new ArrayList<PostProcessorI>();
        addProcessor(tdc);
        addProcessor(ddc);
        addProcessor(new EvacuationLinksTeleporter());
    }

    public void run() {
        double old_time = 0.;
        this.writer.writeLine(this.reader.readLine());
        String[] line = this.reader.readLine();
        while (line != null) {
            double time = Double.parseDouble(line[1]);
            if (time > 5 * 3600) {
                break;
            }
            for (PostProcessorI processor : this.processors) {
                line = processor.processEvent(line);
            }
            this.writer.writeLine(line);
            int min_id = 800000;
            if (this.floodlineGenerator != null && time > old_time) {
                old_time = time;
                Collection<FloodEvent> events = this.floodlineGenerator.getFlooded(time);
                int id = min_id;
                for (FloodEvent e : events) {
                    line[0] = Integer.toString(id);
                    int color = 0;
                    if (e.getFlooding() < MIN_WALKABLE) {
                        color = (int) (Math.min(20, e.getFlooding() / MIN_WALKABLE) * 20);
                    } else {
                        color = (int) (Math.min(235, e.getFlooding() / MAX_HEIGHT) * 235) + 20;
                    }
                    line[7] = Integer.toString(color);
                    line[9] = Integer.toString(id++);
                    line[11] = Double.toString(e.getX());
                    line[12] = Double.toString(e.getY());
                    line[15] = "-1";
                    this.writer.writeLine(line);
                }
            }
            line = this.reader.readLine();
        }
        this.writer.finish();
    }

    public void addProcessor(final PostProcessorI processor) {
        this.processors.add(processor);
    }

    public static void main(final String[] args) {
        String tVehFile;
        if (args.length != 2) {
            throw new RuntimeException("wrong number of arguments! Pleas run TransimsSnaphotFilePostProcessor config.xml T.veh.gz");
        } else {
            Gbl.createConfig(new String[] { args[0], "config_v1.dtd" });
            tVehFile = args[1];
        }
        World world = Gbl.createWorld();
        log.info("loading network from " + Gbl.getConfig().network().getInputFile());
        NetworkLayer network = new NetworkLayer();
        new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
        world.setNetworkLayer(network);
        world.complete();
        log.info("done.");
        String eventsfile = Gbl.getConfig().events().getInputFile();
        TransimsSnapshotFilePostProcessor tpp = new TransimsSnapshotFilePostProcessor(eventsfile, tVehFile);
        tpp.run();
    }
}
