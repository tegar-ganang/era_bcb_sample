package net.sf.mzmine.modules.peaklistmethods.io.xmlimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.projectmethods.projectload.PeakListOpenHandler;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_0.PeakListOpenHandler_2_0;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class XMLImportTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskStatus status = TaskStatus.WAITING;

    private PeakListOpenHandler peakListOpenHander;

    private PeakList buildingPeakList;

    private File fileName;

    /**
	 * 
	 * @param parameters
	 */
    public XMLImportTask(ParameterSet parameters) {
        fileName = parameters.getParameter(XMLImportParameters.filename).getValue();
    }

    /**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
    public void cancel() {
        super.cancel();
        if (peakListOpenHander != null) peakListOpenHander.cancel();
    }

    /**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
    public double getFinishedPercentage() {
        if (peakListOpenHander == null) return 0;
        return peakListOpenHander.getProgress();
    }

    /**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
    public String getTaskDescription() {
        return "Loading peak list from " + fileName;
    }

    /**
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        setStatus(TaskStatus.PROCESSING);
        logger.info("Started parsing file " + fileName);
        try {
            if ((!fileName.exists()) || (!fileName.canRead())) {
                throw new Exception("Parsing Cancelled, file does not exist or is not readable");
            }
            FileInputStream fis = new FileInputStream(fileName);
            InputStream finalStream = fis;
            byte b[] = new byte[32];
            fis.read(b);
            String firstLine = new String(b);
            if (!firstLine.contains("<?xml")) {
                FileChannel fc = fis.getChannel();
                fc.position(0);
                ZipInputStream zis = new ZipInputStream(fis);
                zis.getNextEntry();
                finalStream = zis;
            } else {
                FileChannel fc = fis.getChannel();
                fc.position(0);
            }
            Hashtable<String, RawDataFile> dataFilesIDMap = new Hashtable<String, RawDataFile>();
            for (RawDataFile file : MZmineCore.getCurrentProject().getDataFiles()) {
                dataFilesIDMap.put(file.getName(), file);
            }
            peakListOpenHander = new PeakListOpenHandler_2_0(dataFilesIDMap);
            buildingPeakList = peakListOpenHander.readPeakList(finalStream);
        } catch (Throwable e) {
            if (status == TaskStatus.PROCESSING) setStatus(TaskStatus.ERROR);
            errorMessage = e.toString();
            e.printStackTrace();
            return;
        }
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(buildingPeakList);
        logger.info("Finished parsing " + fileName);
        setStatus(TaskStatus.FINISHED);
    }

    public Object[] getCreatedObjects() {
        return new Object[] { buildingPeakList };
    }
}
