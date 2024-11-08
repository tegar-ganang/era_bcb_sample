package gov.sns.apps.quadshaker;

import java.net.*;
import java.io.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import javax.swing.tree.DefaultTreeModel;
import gov.sns.ca.*;
import gov.sns.tools.plot.*;
import gov.sns.application.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.apputils.*;
import gov.sns.tools.swing.*;
import gov.sns.xal.smf.application.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.xal.smf.impl.qualify.*;
import gov.sns.tools.scan.SecondEdition.UpdatingEventController;
import gov.sns.apps.quadshaker.utils.*;

/**
 *  QuadshakerDocument is a custom XalDocument for Quadshaker application. The
 *  document manages the data that is displayed in the window.
 *
 *@author     shishlo
 *@created    December 12, 2006
 */
public class QuadshakerDocument extends AcceleratorDocument {

    static {
        ChannelFactory.defaultFactory().init();
    }

    private JTextField messageTextLocal = new JTextField();

    UpdatingEventController updatingController = new UpdatingEventController();

    private JTabbedPane mainTabbedPanel = new JTabbedPane();

    private JPanel firstPanel = null;

    private JPanel secondPanel = null;

    private JPanel thirdPanel = null;

    private JPanel preferencesPanel = new JPanel();

    private JButton setFont_PrefPanel_Button = new JButton("Set Font Size");

    private JSpinner fontSize_PrefPanel_Spinner = new JSpinner(new SpinnerNumberModel(7, 7, 26, 1));

    private JLabel timeDealyUC_Label = new JLabel("time delay for graphics update [sec]", JLabel.LEFT);

    private JSpinner timeDealyUC_Spinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));

    private JLabel signCoeffX_Label = new JLabel("correction coeff sign X", JLabel.LEFT);

    private JLabel signCoeffY_Label = new JLabel("correction coeff sign Y", JLabel.LEFT);

    private Font globalFont = new Font("Monospaced", Font.BOLD, 10);

    private int ACTIVE_PANEL = 0;

    private int FIRST_PANEL = 0;

    private int SECOND_PANEL = 1;

    private int THIRD_PANEL = 2;

    private int PREFERENCES_PANEL = 3;

    private static DateAndTimeText dateAndTime = new DateAndTimeText();

    private ShakerController shakerController = null;

    private ShakeAnalysis shakeAnalysis = null;

    private OrbitCorrector orbitCorrector = null;

    private QuadsTable quadsTable = new QuadsTable();

    private BPMsTable bpmsTable = new BPMsTable();

    private String dataRootName = "QUAD_SHAKER";

    /**
	 *  Create a new empty QuadshakerDocument
	 */
    public QuadshakerDocument() {
        super();
        updatingController.setUpdateTime(1.0);
        shakerController = new ShakerController(updatingController);
        shakerController.setTableModels(quadsTable, bpmsTable);
        shakeAnalysis = new ShakeAnalysis();
        shakeAnalysis.setTableModels(quadsTable, bpmsTable);
        shakerController.getShakerRunController().setShakeAnalysis(shakeAnalysis);
        orbitCorrector = new OrbitCorrector();
        orbitCorrector.setTableModel(quadsTable);
        shakeAnalysis.setOrbitCorrector(orbitCorrector);
        firstPanel = shakerController.getPanel();
        secondPanel = shakeAnalysis.getPanel();
        thirdPanel = orbitCorrector.getPanel();
        mainTabbedPanel.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                JTabbedPane tbp = (JTabbedPane) e.getSource();
                setActivePanel(tbp.getSelectedIndex());
            }
        });
        makePreferencesPanel();
        mainTabbedPanel.add("Shaker", firstPanel);
        mainTabbedPanel.add("Analysis", secondPanel);
        mainTabbedPanel.add("Orbit Correction", thirdPanel);
        mainTabbedPanel.add("Preferences", preferencesPanel);
        mainTabbedPanel.setSelectedIndex(0);
        loadDefaultAccelerator();
        Accelerator acc = getAccelerator();
        AcceleratorSeq seq1 = acc.getSequence("CCL1");
        AcceleratorSeq seq2 = acc.getSequence("CCL2");
        AcceleratorSeq seq3 = acc.getSequence("CCL3");
        AcceleratorSeq seq4 = acc.getSequence("CCL4");
        ArrayList cclArr = new ArrayList();
        cclArr.add(seq1);
        cclArr.add(seq2);
        cclArr.add(seq3);
        cclArr.add(seq4);
        AcceleratorSeqCombo cclSeq = new AcceleratorSeqCombo("CCL", cclArr);
        java.util.List cclQuads = cclSeq.getAllNodesWithQualifier((new OrTypeQualifier()).or(Quadrupole.s_strType));
        Iterator itr = cclQuads.iterator();
        while (itr.hasNext()) {
            Quadrupole quad = (Quadrupole) itr.next();
            Quad_Element quadElm = new Quad_Element(quad.getId());
            quadElm.setActive(true);
            quadsTable.getListModel().addElement(quadElm);
            quadElm.getWrpChRBField().setChannelName(quad.getChannel("fieldRB").channelName());
            if (quad.getType().equals("QTH") || quad.getType().equals("QTV")) {
                quadElm.isItTrim(true);
                quadElm.getWrpChCurrent().setChannelName(quad.getChannel("trimI_Set").channelName());
            } else {
                quadElm.isItTrim(false);
                quadElm.getWrpChCurrent().setChannelName(quad.getChannel("I_Set").channelName());
            }
        }
        AcceleratorSeq seq5 = acc.getSequence("SCLMed");
        java.util.List cclBPMs = cclSeq.getAllNodesOfType("BPM");
        java.util.List sclBPMs = seq5.getAllNodesOfType("BPM");
        int n_add = Math.min(4, sclBPMs.size());
        for (int i = 0; i < n_add; i++) {
            cclBPMs.add(sclBPMs.get(i));
        }
        for (int i = 0, n = cclBPMs.size(); i < n; i++) {
            BPM bpm = (BPM) cclBPMs.get(i);
            BPM_Element bpmElm = new BPM_Element(bpm.getId());
            bpmElm.setActive(true);
            bpmElm.getWrpChannelX().setChannelName(bpm.getChannel("xAvg").channelName());
            bpmElm.getWrpChannelY().setChannelName(bpm.getChannel("yAvg").channelName());
            bpmsTable.getListModel().addElement(bpmElm);
        }
        AcceleratorSeq seq0 = acc.getSequence("DTL6");
        ArrayList dtl_cclArr = new ArrayList();
        dtl_cclArr.add(seq0);
        dtl_cclArr.add(seq1);
        dtl_cclArr.add(seq2);
        dtl_cclArr.add(seq3);
        dtl_cclArr.add(seq4);
        AcceleratorSeqCombo comboSeq1 = new AcceleratorSeqCombo("DTL-CCL", dtl_cclArr);
        orbitCorrector.setAccelSeq(comboSeq1);
        ArrayList dtl_sclArr = new ArrayList();
        dtl_sclArr.add(seq0);
        dtl_sclArr.add(seq1);
        dtl_sclArr.add(seq2);
        dtl_sclArr.add(seq3);
        dtl_sclArr.add(seq4);
        dtl_sclArr.add(seq5);
        AcceleratorSeqCombo comboSeq2 = new AcceleratorSeqCombo("DTL-SCL", dtl_sclArr);
        shakeAnalysis.setAccelSeq(comboSeq2);
    }

    /**
	 *  Create a new document loaded from the URL file
	 *
	 *@param  url  The URL of the file to load into the new document.
	 */
    public QuadshakerDocument(URL url) {
        this();
        if (url == null) {
            return;
        }
        setSource(url);
        readQuadshakerDocument(url);
        if (url.getProtocol().equals("jar")) {
            return;
        }
        setHasChanges(true);
    }

    /**
	 *  Make a main window by instantiating the QuadshakerWindow window.
	 */
    public void makeMainWindow() {
        mainWindow = new QuadshakerWindow(this);
        getQuadshakerWindow().setJComponent(mainTabbedPanel);
        messageTextLocal = getQuadshakerWindow().getMessageTextField();
        shakerController.getMessageText().setDocument(messageTextLocal.getDocument());
        shakeAnalysis.getMessageText().setDocument(messageTextLocal.getDocument());
        orbitCorrector.getMessageText().setDocument(messageTextLocal.getDocument());
        fontSize_PrefPanel_Spinner.setValue(new Integer(globalFont.getSize()));
        setFontForAll(globalFont);
        timeDealyUC_Spinner.setValue(new Double(updatingController.getUpdateTime()));
        updatingController.setUpdateTime(((Double) timeDealyUC_Spinner.getValue()).doubleValue());
        JTextField timeTxt_temp = dateAndTime.getNewTimeTextField();
        timeTxt_temp.setHorizontalAlignment(JTextField.CENTER);
        getQuadshakerWindow().addTimeStamp(timeTxt_temp);
        mainWindow.setSize(new Dimension(800, 600));
    }

    /**
	 *  Dispose of QuadshakerDocument resources. This method overrides an empty
	 *  superclass method.
	 */
    protected void freeCustomResources() {
        cleanUp();
    }

    /**
	 *  Reads the content of the document from the specified URL.
	 *
	 *@param  url  Description of the Parameter
	 */
    public void readQuadshakerDocument(URL url) {
        XmlDataAdaptor readAdp = null;
        readAdp = XmlDataAdaptor.adaptorForUrl(url, false);
        if (readAdp != null) {
            XmlDataAdaptor quadshakerData_Adaptor = (XmlDataAdaptor) readAdp.childAdaptor(dataRootName);
            if (quadshakerData_Adaptor != null) {
                cleanUp();
                setTitle(quadshakerData_Adaptor.stringValue("title"));
                XmlDataAdaptor params_font = (XmlDataAdaptor) quadshakerData_Adaptor.childAdaptor("font");
                int font_size = params_font.intValue("size");
                int style = params_font.intValue("style");
                String font_Family = params_font.stringValue("name");
                globalFont = new Font(font_Family, style, font_size);
                fontSize_PrefPanel_Spinner.setValue(new Integer(font_size));
                setFontForAll(globalFont);
                XmlDataAdaptor params_da = (XmlDataAdaptor) quadshakerData_Adaptor.childAdaptor("shared_parameters");
                updatingController.setUpdateTime(params_da.doubleValue("update_time"));
                shakerController.readData(quadshakerData_Adaptor);
                long pvLoggerId = shakerController.getShakerRunController().pvLoggerSnapshotId();
                if (pvLoggerId > 1) {
                    shakeAnalysis.setPVLoggerId(pvLoggerId);
                } else {
                    shakeAnalysis.setPVLoggerId(0);
                }
            }
        }
    }

    /**
	 *  Save the QuadshakerDocument document to the specified URL.
	 *
	 *@param  url  Description of the Parameter
	 */
    public void saveDocumentAs(URL url) {
        XmlDataAdaptor da = XmlDataAdaptor.newEmptyDocumentAdaptor();
        XmlDataAdaptor quadshakerData_Adaptor = (XmlDataAdaptor) da.createChild(dataRootName);
        quadshakerData_Adaptor.setValue("title", url.getFile());
        XmlDataAdaptor params_font = (XmlDataAdaptor) quadshakerData_Adaptor.createChild("font");
        params_font.setValue("name", globalFont.getFamily());
        params_font.setValue("style", globalFont.getStyle());
        params_font.setValue("size", globalFont.getSize());
        XmlDataAdaptor params_da = (XmlDataAdaptor) quadshakerData_Adaptor.createChild("shared_parameters");
        params_da.setValue("update_time", updatingController.getUpdateTime());
        shakerController.dumpData(quadshakerData_Adaptor);
        try {
            quadshakerData_Adaptor.writeTo(new File(url.getFile()));
            setHasChanges(true);
        } catch (IOException e) {
            System.out.println("IOException e=" + e);
        }
    }

    /**
	 *  Edit preferences for the document.
	 */
    void editPreferences() {
        mainTabbedPanel.setSelectedIndex(PREFERENCES_PANEL);
        setActivePanel(PREFERENCES_PANEL);
    }

    /**
	 *  Convenience method for getting the QuadshakerWindow window. It is the cast
	 *  to the proper subclass of XalWindow. This allows me to avoid casting the
	 *  window every time I reference it.
	 *
	 *@return    The main window cast to its dynamic runtime class
	 */
    private QuadshakerWindow getQuadshakerWindow() {
        return (QuadshakerWindow) mainWindow;
    }

    /**
	 *  Register actions for the menu items and toolbar.
	 *
	 *@param  commander  Description of the Parameter
	 */
    protected void customizeCommands(Commander commander) {
        Action dumpDataToASCIIAction = new AbstractAction("save-data-to-ascii") {

            public void actionPerformed(ActionEvent event) {
                System.out.println("debug dump data to ascii");
            }
        };
        commander.registerAction(dumpDataToASCIIAction);
    }

    /**
	 *  Description of the Method
	 */
    private void makePreferencesPanel() {
        fontSize_PrefPanel_Spinner.setAlignmentX(JSpinner.CENTER_ALIGNMENT);
        timeDealyUC_Spinner.setAlignmentX(JSpinner.CENTER_ALIGNMENT);
        JPanel tmp_0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        tmp_0.add(fontSize_PrefPanel_Spinner);
        tmp_0.add(setFont_PrefPanel_Button);
        JPanel tmp_1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        tmp_1.add(timeDealyUC_Spinner);
        tmp_1.add(timeDealyUC_Label);
        JPanel tmp_2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        tmp_2.add(orbitCorrector.getSignXText());
        tmp_2.add(signCoeffX_Label);
        JPanel tmp_3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        tmp_3.add(orbitCorrector.getSignYText());
        tmp_3.add(signCoeffY_Label);
        JPanel tmp_elms = new JPanel(new GridLayout(0, 1));
        tmp_elms.add(tmp_0);
        tmp_elms.add(tmp_1);
        tmp_elms.add(tmp_2);
        tmp_elms.add(tmp_3);
        preferencesPanel.setLayout(new BorderLayout());
        preferencesPanel.add(tmp_elms, BorderLayout.NORTH);
        setFont_PrefPanel_Button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int fnt_size = ((Integer) fontSize_PrefPanel_Spinner.getValue()).intValue();
                globalFont = new Font(globalFont.getFamily(), globalFont.getStyle(), fnt_size);
                setFontForAll(globalFont);
                int h = getQuadshakerWindow().getHeight();
                int w = getQuadshakerWindow().getWidth();
                getQuadshakerWindow().validate();
            }
        });
        timeDealyUC_Spinner.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evnt) {
                updatingController.setUpdateTime(((Double) timeDealyUC_Spinner.getValue()).doubleValue());
            }
        });
    }

    /**
	 *  Clean up the document content
	 */
    private void cleanUp() {
        cleanMessageTextField();
        shakeAnalysis.clearAllGraphContent();
    }

    /**
	 *  Description of the Method
	 */
    private void cleanMessageTextField() {
        messageTextLocal.setText(null);
        messageTextLocal.setForeground(Color.red);
    }

    /**
	 *  Sets the fontForAll attribute of the QuadshakerDocument object
	 *
	 *@param  fnt  The new fontForAll value
	 */
    private void setFontForAll(Font fnt) {
        messageTextLocal.setFont(fnt);
        fontSize_PrefPanel_Spinner.setValue(new Integer(fnt.getSize()));
        setFont_PrefPanel_Button.setFont(fnt);
        fontSize_PrefPanel_Spinner.setFont(fnt);
        ((JSpinner.DefaultEditor) fontSize_PrefPanel_Spinner.getEditor()).getTextField().setFont(fnt);
        globalFont = fnt;
        shakerController.setFontForAll(fnt);
        shakeAnalysis.setFontForAll(fnt);
        orbitCorrector.setFontForAll(fnt);
        timeDealyUC_Label.setFont(fnt);
        timeDealyUC_Spinner.setFont(fnt);
        ((JSpinner.DefaultEditor) timeDealyUC_Spinner.getEditor()).getTextField().setFont(fnt);
        signCoeffX_Label.setFont(fnt);
        signCoeffY_Label.setFont(fnt);
    }

    /**
	 *  Sets the activePanel attribute of the QuadshakerDocument object
	 *
	 *@param  newActPanelInd  The new activePanel value
	 */
    private void setActivePanel(int newActPanelInd) {
        int oldActPanelInd = ACTIVE_PANEL;
        if (oldActPanelInd == newActPanelInd) {
            return;
        }
        if (oldActPanelInd == FIRST_PANEL) {
        } else if (oldActPanelInd == SECOND_PANEL) {
        } else if (oldActPanelInd == THIRD_PANEL) {
        } else if (oldActPanelInd == PREFERENCES_PANEL) {
        }
        if (newActPanelInd == FIRST_PANEL) {
        } else if (newActPanelInd == SECOND_PANEL) {
        } else if (newActPanelInd == THIRD_PANEL) {
        } else if (newActPanelInd == PREFERENCES_PANEL) {
        }
        ACTIVE_PANEL = newActPanelInd;
        cleanMessageTextField();
    }
}

/**
 *  Description of the Class
 *
 *@author     shishlo
 *@created    July 8, 2004
 *@version
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
