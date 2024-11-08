package ppine.io.readers.tasks;

import ppine.io.exceptions.FamiliesTreeFormatException;
import ppine.io.parsers.DataParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import ppine.main.PluginDataHandle;

public class LoadTreesTask extends PPINELoadTask {

    private long current;

    public LoadTreesTask(String treespath) {
        super(treespath);
    }

    public void run() {
        myThread = Thread.currentThread();
        taskMonitor.setStatus("Loading proteins data...");
        taskMonitor.setPercentCompleted(-1);
        try {
            openStreams();
            taskMonitor.setPercentCompleted(0);
            reading();
            PluginDataHandle.getLoadedDataHandle().setProteinsLoaded(true);
            taskMonitor.setPercentCompleted(100);
            doneActionPerformed();
        } catch (FamiliesTreeFormatException ex) {
            sendErrorEvent(ex, "FamiliesTreeFormatException", "LoadTreesTask.run()");
        } catch (FileNotFoundException ex) {
            sendErrorEvent(ex, "FileNotFoundException", "LoadTreesTask.run()");
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
        return "Loading proteins data...";
    }

    private void reading() throws IOException, FamiliesTreeFormatException {
        float last_percent = 0;
        float percent = 0;
        while (br.ready()) {
            String line;
            line = br.readLine();
            if (line != null && !line.equals("")) {
                String[] families = line.split(";");
                for (String family : families) {
                    DataParser.getInstance().readFamiliesTreeString(family);
                }
                current = fis.getChannel().position();
                percent = current * 100 / (float) max;
                if (percent > last_percent + 1) {
                    last_percent = percent;
                    taskMonitor.setPercentCompleted(Math.round(percent));
                }
            }
        }
    }
}
