package ppine.io.readers.tasks;

import ppine.io.exceptions.ExperimentsFileFormatException;
import java.io.IOException;
import ppine.io.parsers.ExperimentParserStruct;
import ppine.io.parsers.rootparser.RootExperimentsParser;
import ppine.logicmodel.controllers.DataHandle;
import ppine.logicmodel.structs.PPINetworkExp;
import ppine.main.PluginDataHandle;
import ppine.utils.IDCreator;

public class LoadAllExperimentsTask extends PPINELoadTask {

    private long current;

    private int created = 0;

    private int all = 0;

    LoadAllExperimentsTask(String exppath) {
        super(exppath);
    }

    public void run() {
        myThread = Thread.currentThread();
        taskMonitor.setStatus("Experiments are loading...");
        taskMonitor.setPercentCompleted(-1);
        try {
            openStreams();
            taskMonitor.setPercentCompleted(0);
            reading();
            taskMonitor.setPercentCompleted(100);
            doneActionPerformed();
        } catch (ExperimentsFileFormatException ex) {
            sendErrorEvent(ex);
        } catch (IOException ex) {
            sendErrorEvent(ex);
        } finally {
            try {
                closeStreams();
            } catch (IOException ex) {
                sendErrorEvent(ex);
            }
        }
    }

    public String getTitle() {
        return "Experiments are loading...";
    }

    private void reading() throws IOException, ExperimentsFileFormatException {
        DataHandle dh = PluginDataHandle.getDataHandle();
        float percent = 0;
        float last_percent = 0;
        while (br.ready()) {
            all++;
            String line = br.readLine();
            ExperimentParserStruct interaction = RootExperimentsParser.readExperiment(line);
            String speciesName = interaction.getSpeciesName();
            String expNetworkName = IDCreator.createExpNetworkID(speciesName);
            PPINetworkExp netOrNull = dh.tryGetExpPPINetowrk(expNetworkName);
            if (netOrNull == null) {
                netOrNull = dh.createExpPPINetwork(speciesName, expNetworkName);
            }
            String edgeID = IDCreator.createExpInteractionID(interaction);
            dh.createInteractionExp(netOrNull, edgeID, interaction);
            created++;
            current = fis.getChannel().position();
            percent = current * 100 / (float) max;
            if (percent > last_percent + 1) {
                last_percent = percent;
                taskMonitor.setPercentCompleted(Math.round(percent));
            }
        }
    }
}
