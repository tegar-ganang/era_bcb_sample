package gov.sns.apps.viewers.scalarpvviewer;

import java.net.*;
import java.io.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import gov.sns.ca.*;
import gov.sns.application.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.apputils.*;
import gov.sns.tools.scan.SecondEdition.UpdatingEventController;
import gov.sns.apps.viewers.scalarpvviewer.utils.*;

/**
 *  ScalarpvviewerDocument is a custom XalDocument for Scalarpvviewer
 *  application. The document manages the data that is displayed in the window.
 *
 *@author    shishlo
 */
public class ScalarpvviewerDocument extends XalDocument {

    static {
        ChannelFactory.defaultFactory().init();
    }

    private JTextField messageTextLocal = new JTextField();

    private UpdatingEventController updatingController = new UpdatingEventController();

    private ScalarPVs spvs = null;

    private Action setViewValuesAction = null;

    private Action setViewChartsAction = null;

    private Action setPredefConfigAction = null;

    private ScalarPVsValuePanel viewValuesPanel = null;

    private ScalarPVsChartPanel viewChartsPanel = null;

    private JPanel preferencesPanel = new JPanel();

    private JButton setFont_PrefPanel_Button = new JButton("Set Font Size");

    private JSpinner fontSize_PrefPanel_Spinner = new JSpinner(new SpinnerNumberModel(7, 7, 26, 1));

    private Font globalFont = new Font("Monospaced", Font.BOLD, 10);

    private PredefinedConfController predefinedConfController = null;

    private JPanel configPanel = null;

    private int ACTIVE_PANEL = 0;

    private int VIEW_VALUES_PANEL = 0;

    private int VIEW_CHARTS_PANEL = 1;

    private int PREFERENCES_PANEL = 2;

    private int PREDEF_CONF_PANEL = 3;

    private static DateAndTimeText dateAndTime = new DateAndTimeText();

    private String dataRootName = "Scalar_PV_VIEWER";

    /**
	 *  Create a new empty ScalarpvviewerDocument
	 */
    public ScalarpvviewerDocument() {
        updatingController.setStop(true);
        spvs = new ScalarPVs(updatingController);
        ACTIVE_PANEL = VIEW_VALUES_PANEL;
        viewValuesPanel = new ScalarPVsValuePanel(spvs, updatingController);
        viewChartsPanel = new ScalarPVsChartPanel(spvs, updatingController);
        makePreferencesPanel();
        makePredefinedConfigurationsPanel();
    }

    /**
	 *  Create a new document loaded from the URL file
	 *
	 *@param  url  The URL of the file to load into the new document.
	 */
    public ScalarpvviewerDocument(URL url) {
        this();
        if (url == null) {
            return;
        }
        setSource(url);
        readScalarpvviewerDocument(url);
        if (!url.getProtocol().equals("jar")) {
            setHasChanges(true);
        }
    }

    /**
	 *  Make a main window by instantiating the ScalarpvviewerWindow window.
	 */
    @Override
    public void makeMainWindow() {
        mainWindow = new ScalarpvviewerWindow(this);
        getScalarpvviewerWindow().setJComponent(viewValuesPanel.getPanel());
        messageTextLocal = getScalarpvviewerWindow().getMessageTextField();
        fontSize_PrefPanel_Spinner.setValue(new Integer(globalFont.getSize()));
        setFontForAll(globalFont);
        predefinedConfController.setMessageTextField(getScalarpvviewerWindow().getMessageTextField());
        JToolBar toolbar = getScalarpvviewerWindow().getToolBar();
        JTextField timeTxt_temp = dateAndTime.getNewTimeTextField();
        timeTxt_temp.setHorizontalAlignment(JTextField.CENTER);
        toolbar.add(timeTxt_temp);
        mainWindow.setSize(new Dimension(700, 600));
    }

    /**
	 *  Dispose of ScalarpvviewerDocument resources. This method overrides an empty
	 *  superclass method.
	 */
    @Override
    protected void freeCustomResources() {
        cleanUp();
    }

    /**
	 *  Reads the content of the document from the specified URL.
	 *
	 *@param  url  Description of the Parameter
	 */
    public void readScalarpvviewerDocument(URL url) {
        try {
            String xmlData = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            boolean cont = true;
            while (cont) {
                line = in.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '%') {
                    xmlData = xmlData + line + System.getProperty("line.separator");
                }
                if (line.length() > 1 && line.charAt(0) == '%' && line.charAt(1) == '=') {
                    cont = false;
                }
            }
            XmlDataAdaptor readAdp = null;
            readAdp = XmlDataAdaptor.adaptorForString(xmlData, false);
            if (readAdp != null) {
                XmlDataAdaptor scalarpvviewerData_Adaptor = readAdp.childAdaptor(dataRootName);
                if (scalarpvviewerData_Adaptor != null) {
                    cleanUp();
                    setTitle(scalarpvviewerData_Adaptor.stringValue("title"));
                    XmlDataAdaptor params_font = scalarpvviewerData_Adaptor.childAdaptor("font");
                    int font_size = params_font.intValue("size");
                    int style = params_font.intValue("style");
                    String font_Family = params_font.stringValue("name");
                    globalFont = new Font(font_Family, style, font_size);
                    fontSize_PrefPanel_Spinner.setValue(new Integer(font_size));
                    setFontForAll(globalFont);
                    XmlDataAdaptor params_pts = scalarpvviewerData_Adaptor.childAdaptor("Panels_titles");
                    viewValuesPanel.setTitle(params_pts.stringValue("values_panel_title"));
                    viewChartsPanel.setTitle(params_pts.stringValue("charts_panel_title"));
                    XmlDataAdaptor params_data = scalarpvviewerData_Adaptor.childAdaptor("PARAMETERS");
                    if (params_data != null) {
                        viewValuesPanel.setLastMemorizingTime(params_data.stringValue("lastMemorizingTime"));
                    } else {
                        viewValuesPanel.setLastMemorizingTime("No Info. See time of file modification.");
                    }
                    XmlDataAdaptor params_uc = scalarpvviewerData_Adaptor.childAdaptor("UpdateController");
                    double updateTime = params_uc.doubleValue("updateTime");
                    updatingController.setUpdateTime(updateTime);
                    double chartUpdateTime = params_uc.doubleValue("ChartUpdateTime");
                    viewChartsPanel.setTimeStep(chartUpdateTime);
                    viewValuesPanel.listenModeOn(params_uc.booleanValue("listenToEPICS"));
                    viewChartsPanel.recordOn(params_uc.booleanValue("recordChartFromEPICS"));
                    java.util.Iterator<XmlDataAdaptor> pvIt = scalarpvviewerData_Adaptor.childAdaptorIterator("ScalarPV");
                    while (pvIt.hasNext()) {
                        XmlDataAdaptor pvDA = pvIt.next();
                        String pvName = pvDA.stringValue("pvName");
                        double refVal = pvDA.doubleValue("referenceValue");
                        double val = 0.;
                        if (pvDA.hasAttribute("value")) {
                            val = pvDA.doubleValue("value");
                        }
                        spvs.addScalarPV(pvName, refVal);
                        ScalarPV spv = spvs.getScalarPV(spvs.getSize() - 1);
                        spv.setValue(val);
                        spv.showValueChart(pvDA.booleanValue("showValueChart"));
                        spv.showRefChart(pvDA.booleanValue("showRefChart"));
                        spv.showDifChart(pvDA.booleanValue("showDifChart"));
                        spv.showDif(pvDA.booleanValue("showDif"));
                        spv.showValue(pvDA.booleanValue("showValue"));
                        spv.showRef(pvDA.booleanValue("showRef"));
                    }
                }
            }
            spvs.readChart(in);
            in.close();
            updatingController.setStop(false);
            viewValuesPanel.updateGraph();
            viewChartsPanel.updateGraph();
        } catch (IOException exception) {
            messageTextLocal.setText(null);
            messageTextLocal.setText("Fatal error. Something wrong with input file. Stop.");
        }
    }

    /**
	 *  Save the ScalarpvviewerDocument document to the specified URL.
	 *
	 *@param  url  Description of the Parameter
	 */
    @Override
    public void saveDocumentAs(URL url) {
        XmlDataAdaptor da = XmlDataAdaptor.newEmptyDocumentAdaptor();
        XmlDataAdaptor scalarpvviewerData_Adaptor = da.createChild(dataRootName);
        scalarpvviewerData_Adaptor.setValue("title", url.getFile());
        XmlDataAdaptor params_font = scalarpvviewerData_Adaptor.createChild("font");
        params_font.setValue("name", globalFont.getFamily());
        params_font.setValue("style", globalFont.getStyle());
        params_font.setValue("size", globalFont.getSize());
        XmlDataAdaptor params_pts = scalarpvviewerData_Adaptor.createChild("Panels_titles");
        params_pts.setValue("values_panel_title", viewValuesPanel.getTitle());
        params_pts.setValue("charts_panel_title", viewChartsPanel.getTitle());
        XmlDataAdaptor params_data = scalarpvviewerData_Adaptor.createChild("PARAMETERS");
        params_data.setValue("lastMemorizingTime", viewValuesPanel.getLastMemorizingTime());
        XmlDataAdaptor params_uc = scalarpvviewerData_Adaptor.createChild("UpdateController");
        params_uc.setValue("updateTime", updatingController.getUpdateTime());
        params_uc.setValue("ChartUpdateTime", viewChartsPanel.getTimeStep());
        params_uc.setValue("listenToEPICS", viewValuesPanel.listenModeOn());
        params_uc.setValue("recordChartFromEPICS", viewChartsPanel.recordOn());
        int nPVs = spvs.getSize();
        for (int i = 0; i < nPVs; i++) {
            ScalarPV pv = spvs.getScalarPV(i);
            XmlDataAdaptor pvDA = scalarpvviewerData_Adaptor.createChild("ScalarPV");
            pvDA.setValue("pvName", pv.getMonitoredPV().getChannelName());
            pvDA.setValue("referenceValue", pv.getRefValue());
            pvDA.setValue("value", pv.getValue());
            pvDA.setValue("showValueChart", pv.showValueChart());
            pvDA.setValue("showRefChart", pv.showRefChart());
            pvDA.setValue("showDifChart", pv.showDifChart());
            pvDA.setValue("showDif", pv.showDif());
            pvDA.setValue("showValue", pv.showValue());
            pvDA.setValue("showRef", pv.showRef());
        }
        try {
            StringWriter strW = new StringWriter();
            scalarpvviewerData_Adaptor.writeTo(strW);
            BufferedWriter out = new BufferedWriter(new FileWriter(url.getFile()));
            out.write(strW.toString());
            spvs.writeChart(out);
            setHasChanges(true);
            out.close();
        } catch (IOException e) {
            System.out.println("IOException e=" + e);
        }
    }

    /**
	 *  Edit preferences for the document.
	 */
    void editPreferences() {
        setActivePanel(PREFERENCES_PANEL);
    }

    /**
	 *  Convenience method for getting the ScalarpvviewerWindow window. It is the
	 *  cast to the proper subclass of XalWindow. This allows me to avoid casting
	 *  the window every time I reference it.
	 *
	 *@return    The main window cast to its dynamic runtime class
	 */
    private ScalarpvviewerWindow getScalarpvviewerWindow() {
        return (ScalarpvviewerWindow) mainWindow;
    }

    /**
	 *  Register actions for the menu items and toolbar.
	 *
	 *@param  commander  Description of the Parameter
	 */
    @Override
    protected void customizeCommands(Commander commander) {
        setViewValuesAction = new AbstractAction("show-view-values-panel") {

            public void actionPerformed(ActionEvent event) {
                setActivePanel(VIEW_VALUES_PANEL);
            }
        };
        commander.registerAction(setViewValuesAction);
        setViewChartsAction = new AbstractAction("show-view-charts-panel") {

            public void actionPerformed(ActionEvent event) {
                setActivePanel(VIEW_CHARTS_PANEL);
            }
        };
        commander.registerAction(setViewChartsAction);
        setPredefConfigAction = new AbstractAction("set-predef-config") {

            public void actionPerformed(ActionEvent event) {
                setActivePanel(PREDEF_CONF_PANEL);
            }
        };
        commander.registerAction(setPredefConfigAction);
    }

    /**
	 *  Description of the Method
	 */
    private void makePreferencesPanel() {
        fontSize_PrefPanel_Spinner.setAlignmentX(JSpinner.CENTER_ALIGNMENT);
        preferencesPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        preferencesPanel.add(fontSize_PrefPanel_Spinner);
        preferencesPanel.add(setFont_PrefPanel_Button);
        setFont_PrefPanel_Button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int fnt_size = ((Integer) fontSize_PrefPanel_Spinner.getValue()).intValue();
                globalFont = new Font(globalFont.getFamily(), globalFont.getStyle(), fnt_size);
                setFontForAll(globalFont);
            }
        });
    }

    /**
	 *  Description of the Method
	 */
    private void makePredefinedConfigurationsPanel() {
        predefinedConfController = new PredefinedConfController(this, "config", "predefinedConfiguration.spv");
        configPanel = predefinedConfController.getJPanel();
        ActionListener selectConfListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                URL url = (URL) e.getSource();
                if (url == null) {
                    Toolkit.getDefaultToolkit().beep();
                    messageTextLocal.setText(null);
                    messageTextLocal.setText("Cannot find an input configuration file!");
                }
                cleanUp();
                readScalarpvviewerDocument(url);
                setHasChanges(false);
                setFontForAll(globalFont);
                setActivePanel(VIEW_VALUES_PANEL);
            }
        };
        predefinedConfController.setSelectorListener(selectConfListener);
    }

    /**
	 *  Clean up the document content
	 */
    private void cleanUp() {
        cleanMessageTextField();
        spvs.clearAll();
    }

    /**
	 *  Description of the Method
	 */
    private void cleanMessageTextField() {
        messageTextLocal.setText(null);
        messageTextLocal.setForeground(Color.red);
    }

    /**
	 *  Sets the fontForAll attribute of the ScalarpvviewerDocument object
	 *
	 *@param  fnt  The new fontForAll value
	 */
    private void setFontForAll(Font fnt) {
        messageTextLocal.setFont(fnt);
        fontSize_PrefPanel_Spinner.setValue(new Integer(fnt.getSize()));
        predefinedConfController.setFontsForAll(fnt);
        viewValuesPanel.setFont(fnt);
        viewChartsPanel.setFont(fnt);
        globalFont = fnt;
    }

    /**
	 *  Updates all data on graphs panels
	 */
    private void updateGraphPanels() {
    }

    /**
	 *  Sets the activePanel attribute of the ScalarpvviewerDocument object
	 *
	 *@param  newActPanelInd  The new activePanel value
	 */
    private void setActivePanel(int newActPanelInd) {
        int oldActPanelInd = ACTIVE_PANEL;
        if (oldActPanelInd == newActPanelInd) {
            return;
        }
        if (oldActPanelInd == VIEW_VALUES_PANEL) {
        } else if (oldActPanelInd == VIEW_CHARTS_PANEL) {
        } else if (oldActPanelInd == PREFERENCES_PANEL) {
        } else if (oldActPanelInd == PREDEF_CONF_PANEL) {
        }
        if (newActPanelInd == VIEW_VALUES_PANEL) {
            getScalarpvviewerWindow().setJComponent(viewValuesPanel.getPanel());
        } else if (newActPanelInd == VIEW_CHARTS_PANEL) {
            getScalarpvviewerWindow().setJComponent(viewChartsPanel.getPanel());
        } else if (newActPanelInd == PREFERENCES_PANEL) {
            getScalarpvviewerWindow().setJComponent(preferencesPanel);
        } else if (newActPanelInd == PREDEF_CONF_PANEL) {
            getScalarpvviewerWindow().setJComponent(configPanel);
        }
        ACTIVE_PANEL = newActPanelInd;
        cleanMessageTextField();
    }
}

/**
 *  Description of the Class
 *
 *@author     shishlo
 *@version
 *@created    July 8, 2004
 */
class DateAndTimeText {

    private SimpleDateFormat dFormat = null;

    private JFormattedTextField dateTimeField = null;

    /**
	 *  Constructor for the DateAndTimeText object
	 */
    public DateAndTimeText() {
        dFormat = new SimpleDateFormat("'Time': MM.dd.yy HH:mm ");
        dateTimeField = new JFormattedTextField(dFormat);
        dateTimeField.setEditable(false);
        Runnable timer = new Runnable() {

            public void run() {
                while (true) {
                    dateTimeField.setValue(new Date());
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        Thread thr = new Thread(timer);
        thr.start();
    }

    /**
	 *  Returns the time attribute of the DateAndTimeText object
	 *
	 *@return    The time value
	 */
    protected String getTime() {
        return dateTimeField.getText();
    }

    /**
	 *  Returns the timeTextField attribute of the DateAndTimeText object
	 *
	 *@return    The timeTextField value
	 */
    protected JFormattedTextField getTimeTextField() {
        return dateTimeField;
    }

    /**
	 *  Returns the newTimeTextField attribute of the DateAndTimeText object
	 *
	 *@return    The newTimeTextField value
	 */
    protected JTextField getNewTimeTextField() {
        JTextField newText = new JTextField();
        newText.setDocument(dateTimeField.getDocument());
        newText.setEditable(false);
        return newText;
    }
}
