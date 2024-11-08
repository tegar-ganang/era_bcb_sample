package ppine.io.readers.tasks;

import ppine.io.parsers.rootparser.RootInteractionsParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import ppine.io.exceptions.InteractionsFileFormatException;
import ppine.io.parsers.InteractionParserStruct;
import ppine.logicmodel.controllers.DataHandle;
import ppine.logicmodel.structs.PPINetwork;
import ppine.main.PluginDataHandle;
import ppine.utils.IDCreator;

public class LoadSpeciesInteractionsTask extends PPINELoadTask {

    private Double treshold;

    private PPINetwork network;

    private int lineNumber = 0;

    LoadSpeciesInteractionsTask(String filepath, PPINetwork network, Double treshold) {
        super(filepath);
        this.network = network;
        this.treshold = treshold;
    }

    @Override
    public void run() {
        myThread = Thread.currentThread();
        taskMonitor.setStatus("Interactions ares loading for: " + network.getID());
        taskMonitor.setPercentCompleted(-1);
        try {
            openStreams();
            taskMonitor.setPercentCompleted(0);
            reading();
            taskMonitor.setPercentCompleted(100);
            doneActionPerformed();
        } catch (InteractionsFileFormatException ex) {
            sendErrorEvent(ex, "Line number: " + String.valueOf(lineNumber), "LoadSpeciesInteractionsTask.run()");
        } catch (FileNotFoundException ex) {
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
        return "Interactions are with treshold: " + String.valueOf(treshold);
    }

    private void reading() throws IOException, InteractionsFileFormatException {
        DataHandle dh = PluginDataHandle.getDataHandle();
        int count = 0;
        float percent = 0;
        float last_percent = 0;
        long current;
        while (br.ready()) {
            lineNumber++;
            InteractionParserStruct interaction = null;
            String line = br.readLine();
            interaction = RootInteractionsParser.readInteraction(line);
            String SourceID = interaction.getFrom();
            String TargetID = interaction.getTo();
            Double probability = interaction.getSim();
            if (treshold == null || probability > treshold) {
                if (network.containsProtein(TargetID) && network.containsProtein(SourceID)) {
                    String EdgeID = IDCreator.createInteractionID(SourceID, TargetID, probability);
                    dh.createInteraction(EdgeID, SourceID, TargetID, probability, network);
                    count++;
                }
            }
            current = fis.getChannel().position();
            percent = current * 100 / (float) max;
            if (percent > last_percent + 1) {
                last_percent = percent;
                taskMonitor.setPercentCompleted(Math.round(percent));
                taskMonitor.setStatus("Interactions are loading for: " + network.getID() + "  " + count);
            }
        }
    }
}
