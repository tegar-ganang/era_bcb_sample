package vxmlsurfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import util.ProgressLogger;

/**
 * This Class is a wrapper around the Processor and the Input Window
 * 
 * @author Joydeep Jana
 * 
 */
public class VXMLSurfer {

    private Processor processThread = null;

    private InputWindow inputWindow = null;

    /**
	 * Constructor Creates Processor and InputWindow
	 * @author Joydeep Jana
	 */
    public VXMLSurfer() {
        ProgressLogger.getInstance().debug("enter VXMLSurfer()");
        processThread = new Processor();
        inputWindow = new InputWindow(processThread);
        ProgressLogger.getInstance().debug("exit VXMLSurfer()");
    }

    /**
	 * this class returns an instance of InputWindow.
	 * @author Joydeep Jana
	 * @return InputWindow
	 */
    public InputWindow getInputWindow() {
        ProgressLogger.getInstance().debug("enter getInputWindow()");
        ProgressLogger.getInstance().debug("exit getInputWindow()");
        return inputWindow;
    }

    /**
	 * this class returns an instance of Processor
	 * @author Joydeep Jana
	 * @return Processor
	 */
    public Processor getProcessThread() {
        ProgressLogger.getInstance().debug("enter getProcessThread()");
        ProgressLogger.getInstance().debug("exit getProcessThread()");
        return processThread;
    }

    /**
	 * sets the processor thread
	 * @param Procesoor
	 * 
	 */
    public void setProcessThread(Processor proc) {
        this.processThread = proc;
    }

    /**
	 * Load the saved VXMLInterpreter settings
	 * @author First Author Joydeep Jana
	 * @param path The relative path + the file name to load the settings from
	 * @return void
	 */
    public void loadVXMLSettings(Properties property) {
        ProgressLogger.getInstance().debug("enter loadVXMLSettings(Properties)");
        processThread.setCurrVoice(Integer.parseInt(property.getProperty("voice")));
        processThread.setTTS_volume(Float.parseFloat(property.getProperty("volume")));
        processThread.setTTS_pitch(Float.parseFloat(property.getProperty("pitch")));
        processThread.setTTS_rate(Float.parseFloat(property.getProperty("rate")));
        processThread.setCurrEngine(Integer.parseInt(property.getProperty("engine")));
        InputWindow.setEchoMode((Integer.parseInt(property.getProperty("echoMode")) == 1 ? true : false));
        ProgressLogger.getInstance().debug("exit loadVXMLSettings(Properties)");
    }

    public void loadVXMLSettings(String path) {
        Properties prop = new Properties();
        try {
            prop.loadFromXML(new FileInputStream(path));
            loadVXMLSettings(prop);
        } catch (Exception e) {
            ProgressLogger.getInstance().error("Could not load VXML settings", e);
            e.printStackTrace();
        }
    }

    public void loadHearSaySettings(Properties property) {
        ProgressLogger.getInstance().debug("enter loadHearSaySettings(Properties)");
        processThread.setContextFlag(Integer.parseInt(property.getProperty("contextFlag")));
        processThread.setEarconMode(Integer.parseInt(property.getProperty("earconMode")));
        processThread.setVerbosityMode(Integer.parseInt(property.getProperty("verbosityMode")));
        processThread.setUnicodeMode(Integer.parseInt(property.getProperty("unicode")));
        processThread.setConceptFlag(Integer.parseInt(property.getProperty("conceptFlag")));
        ProgressLogger.getInstance().debug("exit loadHearSaySettings(Properties)");
    }

    public void loadHearSaySettings(String path) {
        Properties prop = new Properties();
        try {
            prop.loadFromXML(new FileInputStream(path));
            loadHearSaySettings(prop);
        } catch (Exception e) {
            ProgressLogger.getInstance().error("Could not load HearSay settings", e);
            e.printStackTrace();
        }
    }

    /**
	 * Save the VXMLInterpreter settings
	 * @param String path The relative path + the file name to save the settings to 
	 */
    public void saveVXMLSettings(String path) {
        ProgressLogger.getInstance().debug("enter saveVXMLSettings(String)");
        Properties prop = new Properties();
        prop.setProperty("voice", Integer.toString(processThread.getCurrVoice()));
        prop.setProperty("volume", Float.toString(processThread.getTTS_volume()));
        prop.setProperty("pitch", Float.toString(processThread.getTTS_pitch()));
        prop.setProperty("rate", Float.toString(processThread.getTTS_rate()));
        prop.setProperty("engine", Integer.toString(processThread.getCurrEngine()));
        prop.setProperty("echoMode", InputWindow.getEchoMode() ? Integer.toString(1) : Integer.toString(0));
        prop.setProperty("write_to_disk", Boolean.toString(processThread.isReadFromDisk()));
        try {
            prop.storeToXML(new FileOutputStream(path), "VXMLSurfer settings");
        } catch (Exception e) {
            ProgressLogger.getInstance().error("Could not save VXML settings", e);
            e.printStackTrace();
        }
        ProgressLogger.getInstance().debug("exit saveVXMLSettings(String)");
    }

    /**
	 * Save the HearSay settings used by VXMLSurfer
	 * @param String path The relative path + the file name to save the settings to 
	 */
    public void saveHearSaySettings(String path) {
        ProgressLogger.getInstance().debug("enter saveHearSaySettings(String)");
        Properties prop = new Properties();
        prop.setProperty("contextFlag", new Integer(((Double) Variables.getVarVal("contextFlag")).intValue()).toString());
        prop.setProperty("earconMode", new Integer(((Double) Variables.getVarVal("earconMode")).intValue()).toString());
        prop.setProperty("verbosityMode", new Integer(((Double) Variables.getVarVal("verbosityMode")).intValue()).toString());
        prop.setProperty("unicode", new Integer(((Double) Variables.getVarVal("unicode")).intValue()).toString());
        prop.setProperty("conceptFlag", new Integer(((Double) Variables.getVarVal("conceptFlag")).intValue()).toString());
        try {
            prop.storeToXML(new FileOutputStream(path), "HearSay settings used by VXMLSurfer");
        } catch (Exception e) {
            ProgressLogger.getInstance().error("Could not save VXML settings", e);
            e.printStackTrace();
        }
        ProgressLogger.getInstance().debug("exit saveHearSaySettings(String)");
    }
}
