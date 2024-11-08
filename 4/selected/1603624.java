package courselog;

import ewe.filechooser.FileChooser;
import ewe.fx.Color;
import ewe.fx.Dimension;
import ewe.fx.Font;
import ewe.io.File;
import ewe.sys.Device;
import ewe.sys.mThread;
import ewe.ui.CellPanel;
import ewe.ui.CheckBoxGroup;
import ewe.ui.ControlConstants;
import ewe.ui.ControlEvent;
import ewe.ui.EditControl;
import ewe.ui.Event;
import ewe.ui.Form;
import ewe.ui.Menu;
import ewe.ui.MenuEvent;
import ewe.ui.MenuItem;
import ewe.ui.MessageBox;
import ewe.ui.VerticalScrollPanel;
import ewe.ui.mCheckBox;
import ewe.ui.mInput;
import ewe.ui.mLabel;
import ewe.ui.mTextPad;
import ewe.util.Vector;
import courselog.gpsutils.ExporterToGPX;
import courselog.gpsutils.ExporterToKML;
import courselog.gpsutils.TrackRecord;

/**
 * Form to convert CourseLog log files into standard xml files.
 * Supported formats are :
 * GPX : General xml format for GPS files.
 * KML : xml format supported by google earth navigator.
 * @author rigal
 */
public class ConvertForm extends CourseLogAppForm {

    /** Calling application. */
    MainApplication myApp;

    /**
     * Input file selection button object
     */
    protected ActionKeySensitiveButton bInputFile = new ActionKeySensitiveButton(null, "ewe/opensmall.bmp", Color.White);

    /**
     * Input file test area.
     */
    protected mInput mInputFile = new mInput();

    /**
     * Output file selection button.
     */
    protected ActionKeySensitiveButton bOutputFile = new ActionKeySensitiveButton(null, "ewe/opensmall.bmp", Color.White);

    /**
     * Output file text area.
     */
    protected mInput mOutputFile = new mInput();

    /**
     * Convert button (with changing label according to the activity).
     */
    protected ActionKeySensitiveButton bConvert = new ActionKeySensitiveButton("   Convert   ");

    /**
     * Done button to exit the convertion form.
     */
    protected PullDownForKey bGoto;

    /**
     * Radio button for GPX selection.
     */
    protected mCheckBox cGPX = new mCheckBox("GPX format");

    /**
     * Radio button for KML selection.
     */
    protected mCheckBox cKML = new mCheckBox("KML format");

    /**
     * Radio button for CSV selection.
     */
    protected mCheckBox cCSV = new mCheckBox("CSV log format");

    /**
     * Radio buttons group for format selection.
     */
    protected CheckBoxGroup cbgFormat = new CheckBoxGroup();

    /**
     * Radio button for Time coloring selection.
     */
    protected mCheckBox cTime = new mCheckBox("Time");

    /**
     * Radio button for Altitude coloring selection.
     */
    protected mCheckBox cAltitude = new mCheckBox("Altitude");

    /**
     * Radio button for Speed coloring selection.
     */
    protected mCheckBox cSpeed = new mCheckBox("Speed");

    /**
     * Panel containing the coloring selection.
     */
    protected CellPanel cPanelColor = new CellPanel();

    /**
     * Radio button group for coloring selection.
     */
    protected CheckBoxGroup cbgColorData = new CheckBoxGroup();

    /**
     * Selection of a single track re-arrangement of thhe input file.
     * @see gpsutils.TrackRecord#makeOneTrack()
     */
    protected mCheckBox cOneTrack = new mCheckBox("Convert log as a single track");

    /**
     * The name of the input file.
     */
    public String sInputFileName;

    /**
     * The name of the output file.
     */
    public String sOutputFileName;

    /**
     * Creates a new instance of CourseLogOptionsForm.
     * Initialises the form controls and the current values.
     */
    public ConvertForm(MainApplication callingApp) {
        myApp = callingApp;
        Dimension dScr = Device.getScreenSize();
        if (dScr.width > 240) dScr.width = 240;
        if (dScr.height > 320) dScr.height = 320;
        setPreferredSize(dScr.width, dScr.height);
        setTitle("CourseLog - Convert log files ");
        Form.globalIcon = myApp.myAppGlobalIcon;
        MenuItem miCalibrate = new MenuItem("Calibrate$C", "res/iconcalibsmall.png", Color.White);
        miCalibrate.action = "C";
        MenuItem miNavigate = new MenuItem("Navigate$N", "res/iconnavigate.png", Color.White);
        miNavigate.action = "N";
        MenuItem miDistance = new MenuItem("Distance$D", "res/courselogsmall.png", Color.White);
        miDistance.action = "D";
        MenuItem miOptions = new MenuItem("Options$O", "ewe/optionssmall.bmp", Color.White);
        miOptions.action = "P";
        MenuItem miExit = new MenuItem("Exit$x", "ewe/exitsmall.bmp", Color.White);
        miExit.action = "X";
        MenuItem miSeparator = new MenuItem("-");
        MenuItem[] tmiFileActions = new MenuItem[] { miOptions, miNavigate, miDistance, miCalibrate, miExit };
        Menu menuFileActions = new Menu(tmiFileActions, "GO TO");
        bGoto = new PullDownForKey("GO TO", menuFileActions);
        ActionKeysDispatcher akd = new ActionKeysDispatcher();
        bGoto.setActionKeyListener(akd);
        bConvert.setMyActioKeyDispatcher(akd);
        bInputFile.setMyActioKeyDispatcher(akd);
        bOutputFile.setMyActioKeyDispatcher(akd);
        akd.setLeftAndRight(bGoto, bConvert);
        mTextPad mTxtInfo = new mTextPad("Convert Log files into XML files.\n" + "GPX : General GPS XML format to use with GPSbabel\n" + "CSV : CSV format produced as log file\n" + "KML : Track format to use with GoogleEarth.\n" + "      Select color data.");
        mTxtInfo.modify(EditControl.DisplayOnly, 0);
        mLabel lInputFilet = new mLabel("Log :");
        Font normalFont = lInputFilet.getFont();
        int nHeight = normalFont.getSize();
        Font largeFont = normalFont.changeNameAndSize(null, nHeight * 2);
        normalFont = normalFont.changeNameAndSize(null, nHeight + 2);
        Font boldFont = normalFont.changeStyle(Font.BOLD);
        Dimension dInputArea = mInputFile.getSize(null);
        dInputArea.width = (dScr.width * 12) / 16;
        dInputArea.height = (nHeight * 12) / 8;
        mInputFile.setPreferredSize(dInputArea.width, dInputArea.height);
        mOutputFile.setPreferredSize(dInputArea.width, dInputArea.height);
        bInputFile.setFont(boldFont);
        mLabel lOutputFilet = new mLabel("XML :");
        bOutputFile.setFont(boldFont);
        cGPX.setGroup(cbgFormat);
        cCSV.setGroup(cbgFormat);
        cKML.setGroup(cbgFormat);
        cAltitude.setGroup(cbgColorData);
        cTime.setGroup(cbgColorData);
        cSpeed.setGroup(cbgColorData);
        VerticalScrollPanel vsPanelInfo = new VerticalScrollPanel(mTxtInfo);
        vsPanelInfo.setPreferredSize(dScr.width, dScr.height / 6);
        CellPanel cPanelFileNames = new CellPanel();
        cPanelFileNames.setBorder(BDR_OUTLINE | BF_RECT, 1);
        cPanelFileNames.setText("File names");
        CellPanel cPanelFormats = new CellPanel();
        cPanelFormats.setBorder(BDR_OUTLINE | BF_RECT, 1);
        cPanelFormats.setText("Formats");
        cPanelFormats.setToolTip("Select formats :");
        cPanelColor.setBorder(BDR_OUTLINE | BF_RECT, 1);
        cPanelColor.setText("Color data");
        cPanelColor.setToolTip("Select Color data");
        CellPanel cPanelOneTrack = new CellPanel();
        cPanelOneTrack.setBorder(BDR_OUTLINE | BF_RECT, 1);
        cPanelOneTrack.setText("Single track");
        CellPanel cPanelButtons = new CellPanel();
        cPanelFileNames.addNext(lInputFilet, Form.DONTSTRETCH, Form.VCENTER);
        cPanelFileNames.addNext(mInputFile, Form.FILL, Form.VCENTER);
        cPanelFileNames.addLast(bInputFile, Form.DONTSTRETCH, Form.VCENTER);
        cPanelFileNames.addNext(lOutputFilet, Form.DONTSTRETCH, Form.VCENTER);
        cPanelFileNames.addNext(mOutputFile, Form.FILL, Form.VCENTER);
        cPanelFileNames.addLast(bOutputFile, Form.DONTSTRETCH, Form.VCENTER);
        cPanelFormats.addLast(cGPX, Form.FILL, Form.CENTER);
        cPanelFormats.addLast(cCSV, Form.FILL, Form.CENTER);
        cPanelFormats.addLast(cKML, Form.FILL, Form.CENTER);
        cbgFormat.selectIndex(0);
        cPanelColor.addLast(cAltitude, Form.FILL, Form.LEFT);
        cPanelColor.addLast(cTime, Form.FILL, Form.LEFT);
        cPanelColor.addLast(cSpeed, Form.FILL, Form.LEFT);
        cbgColorData.selectIndex(0);
        cPanelColor.modify(ControlConstants.Disabled, 0);
        cPanelOneTrack.addLast(cOneTrack, Form.FILL, Form.CENTER);
        addLast(vsPanelInfo, Form.STRETCH, Form.CENTER);
        addLast(cPanelFileNames, Form.STRETCH, Form.CENTER);
        addNext(cPanelFormats, Form.STRETCH, Form.CENTER);
        addLast(cPanelColor, Form.STRETCH, Form.CENTER);
        addLast(cPanelOneTrack, Form.STRETCH, Form.CENTER);
        addLast(cPanelButtons, Form.FILL, Form.BOTTOM);
        courseLogAppFormSetup(bGoto, bConvert, null);
    }

    public void executeAction(String actString) {
        if (actString == "X") tryExitForm(-1); else if (actString == "C") tryExitForm(MainApplication.ACTIVECALIBRATION); else if (actString == "N") tryExitForm(MainApplication.ACTIVENAVIGATE); else if (actString == "D") tryExitForm(MainApplication.ACTIVEDISTANCE); else if (actString == "P") tryExitForm(MainApplication.ACTIVEOPTIONS);
    }

    /** Triggers an exit from the application */
    public void tryExitForm(int nextState) {
        myApp.myNextActivePanel = nextState;
        exit(Form.IDOK);
    }

    /**
     * Launch files conversion.
     */
    public void convertFiles() {
        File fInputLog = new File(getInputFileName());
        if (!fInputLog.exists()) {
            MessageBox mbErr = new MessageBox("No input", "File " + getInputFileName() + " does not exist. Select an existing log file", MessageBox.MBOK);
            int resp = mbErr.execute();
            return;
        }
        File fOutputX = new File(getOutputFileName());
        if (fOutputX.exists()) {
            MessageBox mbErr = new MessageBox("Output file exists", "Output file " + getOutputFileName() + " already exists. It will be overwritten.\n" + " Cancel does not overwrite the file.", MessageBox.MBOKCANCEL);
            int resp = mbErr.execute();
            if (resp == MessageBox.IDCANCEL) {
                System.out.println("Cancelled");
                return;
            }
        }
        bGoto.modify(ControlConstants.Disabled, 0);
        bConvert.modify(ControlConstants.Disabled, 0);
        this.bConvert.setText("Reading...");
        TrackRecord tr = null;
        Vector vTr = null;
        this.bConvert.setText("Converting...");
        if (cOneTrack.getState()) {
            tr = TrackRecord.readLogFile(fInputLog, null, TrackRecord.OPTION_SINGLETRACK);
        } else {
            tr = TrackRecord.readLogFile(fInputLog, null, TrackRecord.OPTION_SEPARATETRACK);
            if (tr != null) {
                vTr = tr.makeSplitTracks();
            }
        }
        if (tr != null) {
            this.bConvert.setText("Writing...");
            switch(cbgFormat.getSelectedIndex()) {
                case 0:
                    ExporterToGPX expG;
                    if (vTr == null) {
                        expG = new ExporterToGPX(tr);
                    } else {
                        expG = new ExporterToGPX(vTr);
                    }
                    expG.exportToFile(fOutputX, 0);
                    break;
                case 1:
                    tr.writeLogFile(fOutputX);
                    break;
                case 2:
                    ExporterToKML expK;
                    if (vTr == null) {
                        expK = new ExporterToKML(tr);
                    } else {
                        expK = new ExporterToKML(vTr);
                    }
                    int typeExport = 0;
                    switch(cbgColorData.getSelectedIndex()) {
                        case 0:
                            typeExport = ExporterToKML.DF_ALTITUDE;
                            break;
                        case 1:
                            typeExport = ExporterToKML.DF_TIME;
                            break;
                        case 2:
                            typeExport = ExporterToKML.DF_SPEED;
                            break;
                    }
                    expK.exportToFile(fOutputX, typeExport);
                    break;
            }
        } else {
            (new MessageBox("Error", "Error reading input file\nOperation cancelled.", MessageBox.MBOK)).execute();
        }
        bConvert.modify(0, ControlConstants.Disabled);
        this.bConvert.setText("Convert");
        bGoto.modify(0, ControlConstants.Disabled);
    }

    /**
     * Select input file using an open file dialog.
     */
    public void selectInputFile() {
        FileChooser fc = new FileChooser(FileChooser.OPEN, getInputFileName());
        if (fc.execute() == fc.IDCANCEL) return;
        String newName = fc.getChosen();
        setInputFileName(newName);
    }

    /**
     * Select input file using an open file dialog.
     */
    public void selectOutputFile() {
        String oldName = getOutputFileName();
        if (oldName.equals("")) {
            String inputName = getInputFileName();
            if (inputName != null) {
                File inFile = new File(inputName);
                oldName = inFile.getParent();
            }
        }
        FileChooser fc = new FileChooser(FileChooser.SAVE, oldName);
        if (fc.execute() == fc.IDCANCEL) return;
        String newName = fc.getChosen();
        setOutputFileName(newName);
    }

    /**
     * Return the input file selected.
     * @return Iput file name in the input box.
     */
    public String getInputFileName() {
        String newName = mInputFile.getText();
        if (newName.equals("")) {
            return null;
        } else {
            return newName;
        }
    }

    /**
     * Set the input file name.
     * @param newName Name of the input fle.
     */
    public void setInputFileName(String newName) {
        if (newName == null) {
            mInputFile.setText("");
        } else {
            mInputFile.setText(newName);
        }
    }

    /**
     * Return the input file selected.
     * @return Iput file name in the input box.
     */
    public String getOutputFileName() {
        return mOutputFile.getText();
    }

    /**
     * Set the output file name.
     * @param newName Name of the input fle.
     */
    public void setOutputFileName(String newName) {
        if (newName == null) {
            mOutputFile.setText("");
        } else {
            mOutputFile.setText(newName);
        }
    }

    /**
     * When a control is activated, reacts to control press.
     * @param ev Control event.
     */
    public void onEvent(Event ev) {
        try {
            if (ev.target == bGoto) {
                if (ev.type == MenuEvent.SELECTED) {
                    MenuEvent mEv = (MenuEvent) ev;
                    final MenuItem selItem = (MenuItem) mEv.selectedItem;
                    mThread actThread = new mThread(new Runnable() {

                        public void run() {
                            executeAction(selItem.action);
                        }
                    });
                    actThread.start();
                }
            } else if ((ev.target == bConvert) && (ev.type == ControlEvent.PRESSED)) {
                convertFiles();
                return;
            } else if ((ev.target == bInputFile) && (ev.type == ControlEvent.PRESSED)) {
                selectInputFile();
                return;
            } else if ((ev.target == bOutputFile) && (ev.type == ControlEvent.PRESSED)) {
                selectOutputFile();
                return;
            } else if ((ev.target == cbgFormat) && (ev.type == ControlEvent.PRESSED)) {
                if (cbgFormat.getSelectedIndex() == 2) {
                    cPanelColor.modify(0, ControlConstants.Disabled);
                } else {
                    cPanelColor.modify(ControlConstants.Disabled, 0);
                }
                this.repaint();
                return;
            } else {
                super.onEvent(ev);
            }
        } catch (NullPointerException ex) {
            CourseLogAppForm.dbgL.addExceptionToLog(ex);
        } catch (IndexOutOfBoundsException ex) {
            CourseLogAppForm.dbgL.addExceptionToLog(ex);
        } catch (ClassCastException ex) {
            CourseLogAppForm.dbgL.addExceptionToLog(ex);
        }
    }
}
