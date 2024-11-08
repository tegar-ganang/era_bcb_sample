package ca.uhn.hl7v2.testpanel.controller;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.beans.PropertyVetoException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.uhn.hl7v2.conf.ProfileException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.GenericParser;
import ca.uhn.hl7v2.testpanel.model.ActivityIncomingMessage;
import ca.uhn.hl7v2.testpanel.model.ActivityMessage;
import ca.uhn.hl7v2.testpanel.model.MessagesList;
import ca.uhn.hl7v2.testpanel.model.conf.ProfileFileList;
import ca.uhn.hl7v2.testpanel.model.conf.ProfileGroup;
import ca.uhn.hl7v2.testpanel.model.conf.TableFileList;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnection;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnectionList;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnection;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnectionList;
import ca.uhn.hl7v2.testpanel.model.msg.AbstractMessage;
import ca.uhn.hl7v2.testpanel.model.msg.Comment;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageBase;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageCollection;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageEr7;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageXml;
import ca.uhn.hl7v2.testpanel.ui.AddMessageDialog;
import ca.uhn.hl7v2.testpanel.ui.FileChooserSaveAccessory;
import ca.uhn.hl7v2.testpanel.ui.NothingSelectedPanel;
import ca.uhn.hl7v2.testpanel.ui.TestPanelWindow;
import ca.uhn.hl7v2.testpanel.ui.conn.CreateOutboundConnectionDialog;
import ca.uhn.hl7v2.testpanel.ui.conn.InboundConnectionPanel;
import ca.uhn.hl7v2.testpanel.ui.conn.OutboundConnectionPanel;
import ca.uhn.hl7v2.testpanel.ui.editor.Hl7V2MessageEditorPanel;
import ca.uhn.hl7v2.testpanel.util.AllFileFilter;
import ca.uhn.hl7v2.testpanel.util.ExtensionFilter;
import ca.uhn.hl7v2.testpanel.util.FileUtils;
import ca.uhn.hl7v2.testpanel.util.IOkCancelCallback;
import ca.uhn.hl7v2.testpanel.util.LineEndingsEnum;
import ca.uhn.hl7v2.testpanel.util.PortUtil;
import ca.uhn.hl7v2.testpanel.xsd.Hl7V2EncodingTypeEnum;
import ca.uhn.hl7v2.validation.impl.DefaultValidation;

public class Controller {

    static final String DIALOG_TITLE = "TestPanel";

    private static final Logger ourLog = LoggerFactory.getLogger(Controller.class);

    private String myAppVersionString;

    private JFileChooser myConformanceProfileFileChooser;

    private InboundConnectionList myInboundConnectionList;

    private Object myLeftSelectedItem;

    private boolean myMessageEditorInFollowMode = true;

    private MessagesList myMessagesList;

    private Object myNothingSelectedMarker = new Object();

    private JFileChooser myOpenMessagesFileChooser;

    private OutboundConnectionList myOutboundConnectionList;

    private ProfileFileList myProfileFileList;

    private ConformanceEditorController myProfilesAndTablesController;

    private JFileChooser mySaveMessagesFileChooser;

    private FileChooserSaveAccessory mySaveMessagesFileChooserAccessory;

    private TableFileList myTableFileList;

    private TestPanelWindow myView;

    public Controller() {
        myTableFileList = new TableFileList();
        myProfileFileList = new ProfileFileList();
        myMessagesList = new MessagesList(this);
        try {
            File workfilesDir = Prefs.getTempWorkfilesDirectory();
            if (workfilesDir.exists() && workfilesDir.listFiles().length > 0) {
                ourLog.info("Restoring work files from directory: {}", workfilesDir.getAbsolutePath());
                myMessagesList.restoreFromWorkDirectory(workfilesDir);
            }
        } catch (IOException e1) {
            ourLog.error("Failed to restore from work direrctory", e1);
        }
        String savedOutboundList = Prefs.getOutboundConnectionList();
        if (StringUtils.isNotBlank(savedOutboundList)) {
            try {
                myOutboundConnectionList = OutboundConnectionList.fromXml(savedOutboundList);
            } catch (Exception e) {
                ourLog.error("Failed to load outbound connections from storage, going to create default value", e);
                createDefaultOutboundConnectionList();
            }
        }
        if (myOutboundConnectionList == null || myOutboundConnectionList.getConnections().isEmpty()) {
            ourLog.info("No saved outbound connection list found");
            createDefaultOutboundConnectionList();
        }
        String savedInboundList = Prefs.getInboundConnectionList();
        if (StringUtils.isNotBlank(savedInboundList)) {
            try {
                myInboundConnectionList = InboundConnectionList.fromXml(this, savedInboundList);
            } catch (Exception e) {
                ourLog.error("Failed to load inbound connections from storage, going to create default value", e);
                createDefaultInboundConnectionList();
            }
        }
        if (myInboundConnectionList == null || myInboundConnectionList.getConnections().isEmpty()) {
            ourLog.info("No saved inbound connection list found");
            createDefaultInboundConnectionList();
        }
    }

    public void addInboundConnection() {
        InboundConnection con = myInboundConnectionList.createDefaultConnection(provideRandomPort());
        setLeftSelectedItem(con);
        myInboundConnectionList.addConnection(con);
    }

    public void addMessage() {
        AddMessageDialog dialog = new AddMessageDialog(this);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    /**
	 * Create a new message collection with a single instantiated message and
	 * add it to the list of available messages, then select it for editing.
	 */
    public void addMessage(String theVersion, String theType, String theTrigger, String theStructure, Hl7V2EncodingTypeEnum theEncoding) {
        DefaultModelClassFactory mcf = new DefaultModelClassFactory();
        try {
            Hl7V2MessageCollection col = new Hl7V2MessageCollection();
            col.setValidationContext(new DefaultValidation());
            Class<? extends Message> messageClass = mcf.getMessageClass(theStructure, theVersion, true);
            ca.uhn.hl7v2.model.AbstractMessage message = (ca.uhn.hl7v2.model.AbstractMessage) messageClass.newInstance();
            message.initQuickstart(theType, theTrigger, "T");
            GenericParser p = new GenericParser();
            Hl7V2MessageBase msg;
            if (theEncoding == Hl7V2EncodingTypeEnum.ER_7) {
                p.setPipeParserAsPrimary();
                col.setEncoding(Hl7V2EncodingTypeEnum.ER_7);
                msg = new Hl7V2MessageEr7();
                msg.setSourceMessage(p.encode(message));
            } else {
                p.setXMLParserAsPrimary();
                col.setEncoding(Hl7V2EncodingTypeEnum.XML);
                msg = new Hl7V2MessageXml();
                msg.setSourceMessage(p.encode(message));
            }
            col.addMessage(new Comment(""));
            msg.setIndexWithinCollection(1);
            col.addMessage(msg);
            setLeftSelectedItem(col);
            myMessagesList.addMessage(col);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    public void addOutboundConnection() {
        OutboundConnection con = myOutboundConnectionList.createDefaultConnection(provideRandomPort());
        setLeftSelectedItem(con);
        myOutboundConnectionList.addConnection(con);
    }

    public void addOutboundConnectionToSendTo(final IOkCancelCallback<OutboundConnection> theHandler) {
        OutboundConnection connection = myOutboundConnectionList.createDefaultConnection(provideRandomPort());
        final IOkCancelCallback<OutboundConnection> handler = new IOkCancelCallback<OutboundConnection>() {

            public void cancel(OutboundConnection theArg) {
                theHandler.cancel(theArg);
            }

            public void ok(OutboundConnection theArg) {
                myOutboundConnectionList.addConnection(theArg);
                theHandler.ok(theArg);
            }
        };
        CreateOutboundConnectionDialog dialog = new CreateOutboundConnectionDialog(connection, handler);
        dialog.setVisible(true);
    }

    public void chooseAndLoadConformanceProfileForMessage(Hl7V2MessageCollection theMessage, IOkCancelCallback<Void> theCallback) {
        if (myConformanceProfileFileChooser == null) {
            myConformanceProfileFileChooser = new JFileChooser(Prefs.getOpenPathConformanceProfile());
            myConformanceProfileFileChooser.setDialogTitle("Choose an HL7 Conformance Profile");
            ExtensionFilter type = new ExtensionFilter("XML Files", new String[] { ".xml" });
            myConformanceProfileFileChooser.addChoosableFileFilter(type);
        }
        int value = myConformanceProfileFileChooser.showOpenDialog(myView.getMyframe());
        if (value == JFileChooser.APPROVE_OPTION) {
            File file = myConformanceProfileFileChooser.getSelectedFile();
            Prefs.setOpenPathConformanceProfile(file.getPath());
            try {
                String profileString = FileUtils.readFile(file);
                theMessage.setRuntimeProfile(ProfileGroup.createFromRuntimeProfile(profileString));
                theCallback.ok(null);
            } catch (IOException e) {
                ourLog.error("Failed to load profile", e);
                theCallback.cancel(null);
            } catch (ProfileException e) {
                ourLog.error("Failed to load profile", e);
                theCallback.cancel(null);
            }
        } else {
            theCallback.cancel(null);
        }
    }

    public void close() {
        if (!saveAllMessagesAndReturnFalseIfCancelIsPressed()) {
            return;
        }
        myOutboundConnectionList.removeNonPersistantConnections();
        ourLog.info("Saving {} outbound connection descriptors", myOutboundConnectionList.getConnections().size());
        Prefs.setOutboundConnectionList(myOutboundConnectionList.exportConfigToXml());
        myInboundConnectionList.removeNonPersistantConnections();
        ourLog.info("Saving {} inbound connection descriptors", myInboundConnectionList.getConnections().size());
        Prefs.setInboundConnectionList(myInboundConnectionList.exportConfigToXml());
        File workfilesDir;
        try {
            workfilesDir = Prefs.getTempWorkfilesDirectory();
            myMessagesList.dumpToWorkDirectory(workfilesDir);
        } catch (IOException e) {
            ourLog.error("Failed to flush work directory!", e);
        }
        myView.destroy();
        ourLog.info("TestPanel is exiting with status 0");
        System.exit(0);
    }

    public void closeMessage(Hl7V2MessageCollection theMsg) {
        if (theMsg.isSaved() == false) {
            int save = showPromptToSaveMessageBeforeClosingIt(theMsg, true);
            switch(save) {
                case JOptionPane.YES_OPTION:
                    if (!saveMessages(theMsg)) {
                        return;
                    }
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                    return;
                default:
                    throw new Error("invalid option:" + save);
            }
        }
        myMessagesList.removeMessage(theMsg);
        if (myMessagesList.getMessages().size() > 0) {
            setLeftSelectedItem(myMessagesList.getMessages().get(0));
        } else {
            tryToSelectSomething();
        }
    }

    private void createDefaultInboundConnectionList() {
        myInboundConnectionList = new InboundConnectionList();
    }

    private void createDefaultOutboundConnectionList() {
        myOutboundConnectionList = new OutboundConnectionList();
    }

    /**
	 * @return Returns true if the file is saved
	 */
    private boolean doSave(Hl7V2MessageCollection theSelectedValue) {
        Validate.notNull(theSelectedValue);
        try {
            File saveFile = new File(theSelectedValue.getSaveFileName());
            FileOutputStream fos = new FileOutputStream(saveFile);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fos, theSelectedValue.getSaveCharset()));
            boolean saveStripComments = theSelectedValue.isSaveStripComments();
            LineEndingsEnum lineEndings = theSelectedValue.getSaveLineEndings();
            theSelectedValue.writeToFile(w, saveStripComments, lineEndings);
            w.close();
            fos.close();
            theSelectedValue.setSaveFileTimestamp(saveFile.lastModified());
            ourLog.info("Saved " + theSelectedValue.getMessages().size() + " messages to " + theSelectedValue.getSaveFileName());
            theSelectedValue.setSaved(true);
            return true;
        } catch (IOException e) {
            ourLog.error("Failed to save file", e);
            showDialogError("Failed to save file: " + e.getMessage());
            return false;
        }
    }

    public void editMessages(List<ActivityMessage> theList) {
        Validate.notEmpty(theList);
        Hl7V2MessageCollection messageCollection = new Hl7V2MessageCollection();
        int index = 0;
        for (ActivityMessage next : theList) {
            Hl7V2MessageBase nextModel;
            if (next.getEncoding() == Hl7V2EncodingTypeEnum.ER_7) {
                nextModel = new Hl7V2MessageEr7();
            } else {
                nextModel = new Hl7V2MessageXml();
            }
            nextModel.setEncoding(next.getEncoding());
            try {
                nextModel.setSourceMessage(next.getRawMessage());
            } catch (PropertyVetoException e) {
                ourLog.error("Failed to create message object", e);
                continue;
            }
            nextModel.setIndexWithinCollection(index++);
            StringBuilder b = new StringBuilder();
            if (index > 1) {
                b.append("\n");
            }
            if (next instanceof ActivityIncomingMessage) {
                b.append("Received ");
            } else {
                b.append("Sent ");
            }
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS").format(next.getTimestamp());
            b.append(timestamp);
            messageCollection.addComment(b.toString());
            messageCollection.addMessage(nextModel);
        }
        setLeftSelectedItem(messageCollection);
        myMessagesList.addMessage(messageCollection);
    }

    public String getAppVersionString() {
        if (myAppVersionString == null) {
            Properties prop = new Properties();
            try {
                prop.load(Controller.class.getClassLoader().getResourceAsStream("testpanelversion.properties"));
                myAppVersionString = prop.getProperty("app.version");
            } catch (IOException e) {
                ourLog.error("Couldn't load version property", e);
                myAppVersionString = "v.UNK";
            }
        }
        return myAppVersionString;
    }

    /**
	 * @return the inboundConnectionList
	 */
    public InboundConnectionList getInboundConnectionList() {
        return myInboundConnectionList;
    }

    public Object getLeftSelectedItem() {
        return myLeftSelectedItem;
    }

    public MessagesList getMessagesList() {
        return myMessagesList;
    }

    public OutboundConnectionList getOutboundConnectionList() {
        return myOutboundConnectionList;
    }

    public ProfileFileList getProfileFileList() {
        return myProfileFileList;
    }

    /**
	 * @return the tableFileList
	 */
    public TableFileList getTableFileList() {
        return myTableFileList;
    }

    private void handleUnexpectedError(Exception theE) {
        ourLog.error(theE.getMessage(), theE);
        showDialogError(theE.getMessage());
    }

    public boolean isMessageEditorInFollowMode() {
        return myMessageEditorInFollowMode;
    }

    private void openMessageFile(File file) {
        try {
            String profileString = FileUtils.readFile(file);
            Hl7V2MessageCollection col = new Hl7V2MessageCollection();
            col.setSourceMessage(profileString);
            col.setSaveFileName(file.getAbsolutePath());
            col.setSaved(true);
            if (col.getMessages().isEmpty()) {
                showDialogError("No messages were found in the file");
            } else {
                setLeftSelectedItem(col);
                myMessagesList.addMessage(col);
                updateRecentMessageFiles();
            }
        } catch (IOException e) {
            ourLog.error("Failed to load profile", e);
        }
    }

    public void openMessages() {
        if (myOpenMessagesFileChooser == null) {
            myOpenMessagesFileChooser = new JFileChooser(Prefs.getOpenPathMessages());
            myOpenMessagesFileChooser.setDialogTitle("Choose a file containing HL7 messages");
            FileFilter type = new ExtensionFilter("HL7 Files", new String[] { ".hl7" });
            myOpenMessagesFileChooser.addChoosableFileFilter(type);
            type = new ExtensionFilter("XML Files", new String[] { ".xml" });
            myOpenMessagesFileChooser.addChoosableFileFilter(type);
            type = new AllFileFilter();
            myOpenMessagesFileChooser.addChoosableFileFilter(type);
        }
        int value = myOpenMessagesFileChooser.showOpenDialog(myView.getMyframe());
        if (value == JFileChooser.APPROVE_OPTION) {
            File file = myOpenMessagesFileChooser.getSelectedFile();
            Prefs.setOpenPathMessages(file.getPath());
            openMessageFile(file);
        }
    }

    public void openOrSwitchToMessage(String theFileName) {
        for (Hl7V2MessageCollection next : myMessagesList.getMessages()) {
            if (theFileName.equals(next.getSaveFileName())) {
                setLeftSelectedItem(next);
                return;
            }
        }
        File file = new File(theFileName);
        if (file.exists() == false) {
            ourLog.error("Can't find file: {}", theFileName);
        }
        openMessageFile(file);
    }

    public void populateWithSampleMessageAndConnections() {
        Hl7V2MessageCollection col = new Hl7V2MessageCollection();
        col.setValidationContext(new DefaultValidation());
        String message = "MSH|^~\\&|NES|NINTENDO|TESTSYSTEM|TESTFACILITY|20010101000000||ADT^A04|Q123456789T123456789X123456|P|2.3\r" + "EVN|A04|20010101000000|||^KOOPA^BOWSER^^^^^^^CURRENT\r" + "PID|1||123456789|0123456789^AA^^JP|BROS^MARIO^^^^||19850101000000|M|||123 FAKE STREET^MARIO \\T\\ LUIGI BROS PLACE^TOADSTOOL KINGDOM^NES^A1B2C3^JP^HOME^^1234|1234|(555)555-0123^HOME^JP:1234567|||S|MSH|12345678|||||||0|||||N\r" + "NK1|1|PEACH^PRINCESS^^^^|SO|ANOTHER CASTLE^^TOADSTOOL KINGDOM^NES^^JP|(123)555-1234|(123)555-2345|NOK|||||||||||||\r" + "NK1|2|TOADSTOOL^PRINCESS^^^^|SO|YET ANOTHER CASTLE^^TOADSTOOL KINGDOM^NES^^JP|(123)555-3456|(123)555-4567|EMC|||||||||||||\r" + "PV1|1|O|ABCD^EFGH^|||^^|123456^DINO^YOSHI^^^^^^MSRM^CURRENT^^^NEIGHBOURHOOD DR NBR^|^DOG^DUCKHUNT^^^^^^^CURRENT||CRD|||||||123456^DINO^YOSHI^^^^^^MSRM^CURRENT^^^NEIGHBOURHOOD DR NBR^|AO|0123456789|1|||||||||||||||||||MSH||A|||20010101000000\r" + "IN1|1|PAR^PARENT||||LUIGI\r" + "IN1|2|FRI^FRIEND||||PRINCESS";
        col.setEncoding(Hl7V2EncodingTypeEnum.ER_7);
        col.setSourceMessage(message);
        myMessagesList.addMessage(col);
        int port = PortUtil.findFreePort();
        InboundConnection iCon = myInboundConnectionList.createDefaultConnection(port);
        iCon.setPersistent(true);
        myInboundConnectionList.addConnection(iCon);
        OutboundConnection oCon = myOutboundConnectionList.createDefaultConnection(port);
        oCon.setPersistent(true);
        myOutboundConnectionList.addConnection(oCon);
        setLeftSelectedItem(col);
    }

    /**
	 * Provide a random, currently unused port
	 */
    private int provideRandomPort() {
        ServerSocket server;
        try {
            server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void removeInboundConnection(InboundConnection theConnection) {
        myInboundConnectionList.removeConnecion(theConnection);
        if (myInboundConnectionList.getConnections().size() > 0) {
            setLeftSelectedItem(myInboundConnectionList.getConnections().get(0));
        } else {
            tryToSelectSomething();
        }
    }

    public void removeOutboundConnection(OutboundConnection theConnection) {
        myOutboundConnectionList.removeConnecion(theConnection);
        if (myOutboundConnectionList.getConnections().size() > 0) {
            setLeftSelectedItem(myOutboundConnectionList.getConnections().get(0));
        } else {
            tryToSelectSomething();
        }
    }

    public boolean saveAllMessagesAndReturnFalseIfCancelIsPressed() {
        for (Hl7V2MessageCollection next : myMessagesList.getMessages()) {
            if (next.isSaved() == false) {
                int save = showPromptToSaveMessageBeforeClosingIt(next, true);
                switch(save) {
                    case JOptionPane.YES_OPTION:
                        if (!saveMessages(next)) {
                            return false;
                        }
                        break;
                    case JOptionPane.NO_OPTION:
                        break;
                    case JOptionPane.CANCEL_OPTION:
                        return false;
                    default:
                        throw new Error("invalid option:" + save);
                }
            }
        }
        return true;
    }

    public boolean saveMessages(Hl7V2MessageCollection theSelectedValue) {
        Validate.notNull(theSelectedValue);
        if (theSelectedValue.getSaveFileName() == null) {
            return saveMessagesAs(theSelectedValue);
        } else {
            return doSave(theSelectedValue);
        }
    }

    /**
	 * Prompt for a filename and save the currently selected messages
	 * 
	 * @return Returns true if the file is saved
	 */
    public boolean saveMessagesAs(Hl7V2MessageCollection theSelectedValue) {
        Validate.notNull(theSelectedValue);
        if (mySaveMessagesFileChooser == null) {
            mySaveMessagesFileChooser = new JFileChooser(Prefs.getSavePathMessages());
            mySaveMessagesFileChooser.setDialogTitle("Choose a file to save the current message(s) to");
            mySaveMessagesFileChooserAccessory = new FileChooserSaveAccessory();
            mySaveMessagesFileChooser.setAccessory(mySaveMessagesFileChooserAccessory);
            FileFilter type = new ExtensionFilter("HL7 Files", new String[] { ".hl7" });
            mySaveMessagesFileChooser.addChoosableFileFilter(type);
            type = new ExtensionFilter("XML Files", new String[] { ".xml" });
            mySaveMessagesFileChooser.addChoosableFileFilter(type);
            type = new AllFileFilter();
            mySaveMessagesFileChooser.addChoosableFileFilter(type);
            mySaveMessagesFileChooser.setPreferredSize(new Dimension(700, 500));
        }
        int value = mySaveMessagesFileChooser.showSaveDialog(myView.getMyframe());
        if (value == JFileChooser.APPROVE_OPTION) {
            File file = mySaveMessagesFileChooser.getSelectedFile();
            Prefs.setSavePathMessages(file.getPath());
            if (!file.getName().contains(".")) {
                switch(theSelectedValue.getEncoding()) {
                    case ER_7:
                        file = new File(file.getAbsolutePath() + ".hl7");
                        break;
                    case XML:
                        file = new File(file.getAbsolutePath() + ".xml");
                        break;
                }
            }
            if (file.exists()) {
                String message = "The file \"" + file.getName() + "\" already exists. Do you wish to overwrite it?";
                int confirmed = showDialogYesNo(message);
                if (confirmed == JOptionPane.NO_OPTION) {
                    return false;
                }
                ourLog.info("Deleting file: {}", file.getAbsolutePath());
                file.delete();
            }
            theSelectedValue.setSaveCharset(mySaveMessagesFileChooserAccessory.getSelectedCharset());
            theSelectedValue.setSaveFileName(file.getAbsolutePath());
            theSelectedValue.setSaveStripComments(mySaveMessagesFileChooserAccessory.isSelectedSaveStripComments());
            theSelectedValue.setSaveLineEndings(mySaveMessagesFileChooserAccessory.getSelectedLineEndings());
            doSave(theSelectedValue);
            updateRecentMessageFiles();
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Send one or more messages out over an interface
	 */
    public void sendMessages(OutboundConnection theConnection, List<AbstractMessage<?>> theMessages) {
        theConnection.sendMessages(theMessages);
    }

    public void setLeftSelectedItem(Object theSelectedValue) {
        if (myLeftSelectedItem == theSelectedValue) {
            return;
        }
        myLeftSelectedItem = theSelectedValue;
        if (myLeftSelectedItem instanceof Hl7V2MessageCollection) {
            Hl7V2MessageEditorPanel hl7v2MessageEditorPanel = new Hl7V2MessageEditorPanel(this);
            hl7v2MessageEditorPanel.setMessage((Hl7V2MessageCollection) myLeftSelectedItem);
            myView.setMainPanel(hl7v2MessageEditorPanel);
        } else if (myLeftSelectedItem instanceof OutboundConnection) {
            OutboundConnectionPanel panel = new OutboundConnectionPanel();
            panel.setController(this);
            panel.setConnection((OutboundConnection) myLeftSelectedItem);
            myView.setMainPanel(panel);
        } else if (myLeftSelectedItem instanceof InboundConnection) {
            InboundConnectionPanel panel = new InboundConnectionPanel(this);
            panel.setConnection((InboundConnection) myLeftSelectedItem);
            myView.setMainPanel(panel);
        } else if (myLeftSelectedItem == myNothingSelectedMarker) {
            myView.setMainPanel(new NothingSelectedPanel(this));
        }
    }

    /**
	 * @param theMessageEditorInFollowMode
	 *            the messageEditorInFollowMode to set
	 */
    public void setMessageEditorInFollowMode(boolean theMessageEditorInFollowMode) {
        myMessageEditorInFollowMode = theMessageEditorInFollowMode;
    }

    public void showAboutDialog() {
        myView.showAboutDialog();
    }

    public void showDialogError(String message) {
        JOptionPane.showMessageDialog(provideViewFrameIfItExists(), message, DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
    }

    private Component provideViewFrameIfItExists() {
        return myView != null ? myView.getMyframe() : null;
    }

    public void showDialogWarning(String message) {
        JOptionPane.showMessageDialog(provideViewFrameIfItExists(), message, DIALOG_TITLE, JOptionPane.WARNING_MESSAGE);
    }

    public int showDialogYesNo(String message) {
        return JOptionPane.showConfirmDialog(provideViewFrameIfItExists(), message, DIALOG_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    public void showProfilesAndTablesEditor() {
        if (myProfilesAndTablesController == null) {
            myProfilesAndTablesController = new ConformanceEditorController(this);
        }
        myProfilesAndTablesController.show();
    }

    private int showPromptToSaveMessageBeforeClosingIt(Hl7V2MessageCollection theMsg, boolean theShowCancelButton) {
        Component parentComponent = myView.getMyframe();
        Object message = "<html>The following file is unsaved, do you want to save before closing?<br>" + theMsg.getBestDescription() + "</html>";
        String title = DIALOG_TITLE;
        int optionType = theShowCancelButton ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION;
        int messageType = JOptionPane.QUESTION_MESSAGE;
        return JOptionPane.showConfirmDialog(parentComponent, message, title, optionType, messageType);
    }

    public void start() {
        myView = new TestPanelWindow(this);
        myView.getFrame().setVisible(true);
        updateRecentMessageFiles();
        if (myMessagesList.getMessages().size() > 0) {
            setLeftSelectedItem(myMessagesList.getMessages().get(0));
        } else {
            setLeftSelectedItem(myNothingSelectedMarker);
        }
        new VersionChecker().start();
    }

    public void startAllInboundConnections() {
        ourLog.info("Starting all inbound connections");
        for (InboundConnection next : myInboundConnectionList.getConnections()) {
            next.start();
        }
    }

    public void startAllOutboundConnections() {
        ourLog.info("Starting all outbound connections");
        for (OutboundConnection next : myOutboundConnectionList.getConnections()) {
            next.start();
        }
    }

    public void startInboundConnection(InboundConnection theLeftSelectedItem) {
        theLeftSelectedItem.start();
    }

    public void startOutboundConnection(OutboundConnection theLeftSelectedItem) {
        theLeftSelectedItem.start();
    }

    public void stopAllInboundConnections() {
        ourLog.info("Stopping all inbound connections");
        for (InboundConnection next : myInboundConnectionList.getConnections()) {
            next.stop();
        }
    }

    public void stopAllOutboundConnections() {
        ourLog.info("Stopping all outbound connections");
        for (OutboundConnection next : myOutboundConnectionList.getConnections()) {
            next.stop();
        }
    }

    private void tryToSelectSomething() {
        if (myMessagesList.getMessages().size() > 0) {
            setLeftSelectedItem(myMessagesList.getMessages().get(0));
        } else if (myOutboundConnectionList.getConnections().size() > 0) {
            setLeftSelectedItem(myOutboundConnectionList.getConnections().get(0));
        } else if (myInboundConnectionList.getConnections().size() > 0) {
            setLeftSelectedItem(myInboundConnectionList.getConnections().get(0));
        } else {
            setLeftSelectedItem(myNothingSelectedMarker);
        }
    }

    private void updateRecentMessageFiles() {
        Prefs.addMessagesFileToRecents(myMessagesList.getMessageFiles());
        if (myView != null) {
            myView.setRecentMessageFiles(Prefs.getRecentMessageFiles());
        }
    }

    public boolean validateNewValue(String theTerserPath, String theNewValue) {
        String errorMsg = null;
        if (theTerserPath.endsWith("MSH-1")) {
            if (theNewValue.length() != 1) {
                errorMsg = "MSH-1 must be exactly 1 character";
            }
        }
        if (theTerserPath.endsWith("MSH-2")) {
            if (theNewValue.length() != 4) {
                errorMsg = "MSH-2 must be exactly 4 characters";
            }
        }
        if (errorMsg != null) {
            showDialogError(errorMsg);
            return false;
        }
        return true;
    }

    /**
	 * Thread which checks if we are running the latest version of the TestPanel
	 */
    private class VersionChecker extends Thread {

        @Override
        public void run() {
            String version = getAppVersionString();
            if (version.contains("$")) {
                version = "1.0";
            }
            boolean isWebstart = true;
            try {
                Class.forName("javax.jnlp.ServiceManager");
            } catch (Throwable t) {
                isWebstart = false;
            }
            try {
                String javaVersion = System.getProperty("java.version");
                String os = System.getProperty("os.name").replace(" ", "+");
                URL url = new URL("http://hl7api.sourceforge.net/cgi-bin/testpanelversion.cgi?version=" + version + "&java=" + javaVersion + "&os=" + os + "&webstart=" + isWebstart + "&end");
                InputStream is = (InputStream) url.getContent();
                Reader reader = new InputStreamReader(is, "US-ASCII");
                String content = FileUtils.readFromReaderIntoString(reader);
                if (content.contains("OK")) {
                    ourLog.info("HAPI TestPanel is up to date. Great!");
                } else if (content.contains("ERRORNOE ")) {
                    final String message = content.replace("ERRORNOE ", "");
                    ourLog.warn(message);
                    EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            showDialogWarning(message);
                        }
                    });
                } else {
                    ourLog.warn(content);
                }
            } catch (MalformedURLException e) {
                ourLog.warn("Couldn't parse version checker URL", e);
            } catch (IOException e) {
                ourLog.info("Failed to check if we are running the latest version");
            }
        }
    }
}
