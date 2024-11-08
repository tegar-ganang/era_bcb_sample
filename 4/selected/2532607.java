package com.tms.webservices.applications;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.KeyStroke;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

/**
 * A class that builds a <code>JInternalFrame</code> that enables a
 * user to send a <code>request</code> to the <code>XTVD webservice
 * </code> and view/save the response.
 *
 * <p><b>Note:</b> This class uses a <code>JTextArea</code> to display
 * the <code>XTVD XML document</code>.  A <code>JTextArea</code> uses 
 * a fairly complex <code>Document</code> internally to store the text,
 * and hence consumes a lot of memory.  It is not recommended that you
 * use this class to display large documents.</p>
 *
 * @author Rakesh Vidyadharan 2<sup><small>nd</small></sup> February, 2004
 *
 * <p>Copyright 2004, Tribune Media Services</p>
 *
 * $Id: XTVDFrame.java,v 1.1.1.1 2005/07/19 04:28:17 shawndr Exp $
 */
public class XTVDFrame extends JInternalFrame {

    /**
   * The <code>newline</code> character to use for the {@link
   * #outputArea}.
   */
    private static final String NEWLINE = "\n";

    /**
   * An instance of the {@link XTVDClient} class that is used to
   * interact with the <code>XTVD webservice</code>.
   */
    private XTVDClient xtvdClient = null;

    /**
   * A text-field that is used to enter the username to use to
   * authenticate the request with the webservice.
   */
    private JTextField userNameField = null;

    /**
   * A text-field that is used to enter the username to use to
   * authenticate the request with the webservice.
   */
    private JPasswordField passwordField = null;

    /**
   * A text-field that is used to enter the time from which data is
   * to be requested.
   */
    private JTextField startTimeField = null;

    /**
   * A text-field that is used to enter the time till which data is
   * to be requested.
   */
    private JTextField endTimeField = null;

    /**
   * A text-field that is used to enter the <code>XTVD webservice
   * URI</code>.
   */
    private JTextField webserviceURIField = null;

    /**
   * A button that is used to send the download request.
   */
    private JButton sendButton = null;

    /**
   * A button that is used to launch a dialog box prompting the user
   * for a file to save the XTVD contents.
   */
    private JButton saveXTVDButton = null;

    /**
   * A button that is used to save the values in the parameters pane
   * to the underlying properties XML file.
   */
    private JButton savePropertiesButton = null;

    /**
   * A text-area that is used to display the XTVD XML data that is
   * returned by the XTVD webservice.
   */
    private JTextArea outputArea = null;

    /**
   * A text-area that is used to display log messages (errors) from the
   * XTVD webservice.
   */
    private JTextArea logArea = null;

    /**
   * An instance of <code>JFileChooser</code> that is used to allow
   * users the ability to save the <code>XTVD document</code>.
   */
    private JFileChooser fileChooser = null;

    /**
   * A <code>JLabel</code> that is used to indicate the status of the
   * download activity in the first tab of the tabbed pane.
   */
    private JLabel messageLabel = null;

    /**
   * A <code>flag</code> that is used to indicate whether the download
   * from the <code>XTVD webservice</code> was successful or not.
   */
    private volatile boolean successfulDownload = false;

    /**
   * The only constructor method supported.  Initialise {@link
   * #xtvdClient} with the specified instance, and create a new
   * <code>JInternalFrame</code> instance that is <code>resizable,
   * closable, maximisable,</code> and <code>iconifiable</code>.
   * Creates all the necessary components, and adds them to the
   * internal frame.
   *
   * @see #buildInterface()
   * @param String title - The title for the frame
   * @param XTVDClient xtvdClient - The {@link XTVDClient} instance to
   *   associate with this frame.
   */
    public XTVDFrame(String title, XTVDClient xtvdClient) {
        super(title, true, true, true, true);
        this.xtvdClient = xtvdClient;
        fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save XTVD Document");
        fileChooser.setMultiSelectionEnabled(false);
        buildInterface();
        pack();
    }

    /**
   * Build the components that displays the input fields that the 
   * user can use to specify the parameters necessary to make the SOAP 
   * request.
   */
    private void buildInterface() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Input Parameters", null, createInputAreas(), "Input parameters area.");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_I);
        tabbedPane.addTab("XTVD Document", null, createOutputArea(), "XTVD Response area.");
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_D);
        tabbedPane.addTab("Server Messages", null, createLogArea(), "XTVD log area.");
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_M);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    /**
   * Create the {@link #userNameField}, {@link #passwordField}, {@link
   * #startTimeField}, and {@link #endTimeField} input fields, add them
   * to the specified panel.
   *
   * @return JPanel - The panel that contains all the components for the
   *   input parameters.
   */
    private JComponent createInputAreas() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        JLabel userNameLabel = new JLabel("User Name", JLabel.TRAILING);
        userNameField = new JTextField(xtvdClient.userName);
        userNameLabel.setLabelFor(userNameField);
        JLabel passwordLabel = new JLabel("Password", JLabel.TRAILING);
        passwordField = new JPasswordField(xtvdClient.password);
        passwordLabel.setLabelFor(passwordField);
        JLabel startTimeLabel = new JLabel("Start Time", JLabel.TRAILING);
        startTimeField = new JTextField(xtvdClient.sdf.format(xtvdClient.start.getTime()));
        startTimeLabel.setLabelFor(startTimeField);
        JLabel endTimeLabel = new JLabel("End Time", JLabel.TRAILING);
        endTimeField = new JTextField(xtvdClient.sdf.format(xtvdClient.end.getTime()));
        endTimeLabel.setLabelFor(endTimeField);
        JLabel uriLabel = new JLabel("Webservice URI", JLabel.TRAILING);
        webserviceURIField = new JTextField(xtvdClient.webserviceURI);
        uriLabel.setLabelFor(webserviceURIField);
        JPanel inputPanel = new JPanel(new SpringLayout());
        inputPanel.add(userNameLabel);
        inputPanel.add(userNameField);
        inputPanel.add(passwordLabel);
        inputPanel.add(passwordField);
        inputPanel.add(startTimeLabel);
        inputPanel.add(startTimeField);
        inputPanel.add(endTimeLabel);
        inputPanel.add(endTimeField);
        inputPanel.add(uriLabel);
        inputPanel.add(webserviceURIField);
        SpringUtilities.makeCompactGrid(inputPanel, 5, 2, 6, 6, 6, 6);
        contentPanel.add(inputPanel, BorderLayout.NORTH);
        JPanel messagePanel = new JPanel(new GridLayout(1, 1));
        messagePanel.setBorder(BorderFactory.createLoweredBevelBorder());
        messageLabel = new JLabel("Click \"Send Request\" to download data.", javax.swing.SwingConstants.CENTER);
        messagePanel.add(messageLabel);
        contentPanel.add(messagePanel, BorderLayout.CENTER);
        createButtons(contentPanel);
        return contentPanel;
    }

    /**
   * Create the {@link #outputArea} text area that will display the 
   * output from the webservice.  Create a <code>JScrollPane</code>
   * to hold the text area.  Also add a panel that contains a 
   * <code>JTextField</code> where a user can enter a string to find
   * in the {@link #outputArea}.
   *
   * @see FindAction
   * @see FindNextAction
   * @return JComponent - The scrollpane that contains the text area
   *   to which the XTVD document will be written.
   */
    private JComponent createOutputArea() {
        JPanel panel = new JPanel(new BorderLayout());
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        JTextField searchField = new JTextField(25);
        FindAction action = new FindAction(searchField, outputArea);
        FindNextAction nextAction = new FindNextAction(searchField, outputArea);
        JButton find = new JButton(action);
        JButton findNext = new JButton(nextAction);
        searchField.setToolTipText("Input text to search for.");
        searchField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        searchField.getActionMap().put("pressed", nextAction);
        find.setText("Find");
        find.setToolTipText("Find first instance of specified text.");
        find.setMnemonic(KeyEvent.VK_F);
        find.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        find.getActionMap().put("pressed", action);
        findNext.setText("Find Next");
        findNext.setToolTipText("Find next instance of specified text from cursor location.");
        findNext.setMnemonic(KeyEvent.VK_N);
        findNext.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        findNext.getActionMap().put("pressed", nextAction);
        JPanel findPanel = new JPanel();
        findPanel.add(searchField);
        findPanel.add(find);
        findPanel.add(findNext);
        panel.add(findPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
   * Create the {@link #logArea} text area that will display the 
   * output from the webservice.  Create a <code>JScrollPane</code>
   * to hold the text area.
   *
   * @return JComponent - The scrollpane that contains the text area
   *   to which the XTVD document will be written.
   */
    private JComponent createLogArea() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        return scrollPane;
    }

    /**
   * Create a panel that holds the {@link #sendButton} and {@link
   * #saveXTVDButton} buttons, and add it to the panel specified.
   *
   * @see #createSendButton()
   * @see #createSaveXTVDButton()
   * @see #createSavePropertiesButton()
   * @param JPanel panel - The panel to which the buttons are to be
   *   added.
   */
    private void createButtons(JPanel panel) {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        createSendButton();
        buttonPanel.add(sendButton);
        createSaveXTVDButton();
        buttonPanel.add(saveXTVDButton);
        createSavePropertiesButton();
        buttonPanel.add(savePropertiesButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
   * Create the {@link #sendButton} button, and set up the 
   * <code>Action</code> for it.
   *
   * @see RequestAction
   */
    private void createSendButton() {
        RequestAction action = new RequestAction();
        sendButton = new JButton(action);
        sendButton.setText("Send Request");
        sendButton.setToolTipText("Send a new download request to the XTVD webservice.");
        sendButton.setMnemonic(KeyEvent.VK_R);
        sendButton.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        sendButton.getActionMap().put("pressed", action);
    }

    /**
   * Create the {@link #saveXTVDButton} button, and set up the
   * <code>Action</code> for it.
   *
   * @see XTVDFrame.SaveXTVDAction
   */
    private void createSaveXTVDButton() {
        SaveXTVDAction action = new SaveXTVDAction();
        saveXTVDButton = new JButton(action);
        saveXTVDButton.setText("Save XTVD");
        saveXTVDButton.setToolTipText("Save the downloaded XTVD document.");
        saveXTVDButton.setMnemonic(KeyEvent.VK_S);
        saveXTVDButton.setEnabled(false);
        saveXTVDButton.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        saveXTVDButton.getActionMap().put("pressed", action);
    }

    /**
   * Create the {@link #savePropertiesButton} button, and set up the
   * <code>Action</code> for it.
   *
   * @see XTVDFrame.SavePropertiesAction
   */
    private void createSavePropertiesButton() {
        SavePropertiesAction action = new SavePropertiesAction();
        savePropertiesButton = new JButton(action);
        savePropertiesButton.setText("Save Properties");
        savePropertiesButton.setToolTipText("Save the current request parameters as default.");
        savePropertiesButton.setMnemonic(KeyEvent.VK_P);
        savePropertiesButton.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        savePropertiesButton.getActionMap().put("pressed", action);
    }

    /**
   * A <code>thread</code> that is used to make the <code>SOAP
   * request</code> to the <code>XTVD webservice</code>, and get the
   * data from the service.
   */
    class RequestThread extends Thread {

        /**
     * The <code>Writer</code> to which the response from the 
     * <code>XTVD webservice</code> is to be written.
     */
        private Writer out;

        /**
     * Create a new instance of this <code>inner-class</code>, and
     * initialise the writer with the specified writer.
     *
     * @param Writer writer - The writer to use for capturing the
     *   response.
     */
        public RequestThread(Writer writer) {
            out = writer;
        }

        /**
     * Defines the actions to be performed by this thread when it
     * runs.  Invoke {@link 
     * com.tms.webservices.applications.xtvd.SOAPRequest#getData( 
     * Calendar, Calendar, Writer )}.
     */
        public void run() {
            try {
                xtvdClient.soapRequest.getData(xtvdClient.start, xtvdClient.end, out);
                out.close();
                successfulDownload = true;
            } catch (Throwable t) {
                logArea.append(DataDirectException.getStackTraceString(t));
                try {
                    out.close();
                } catch (IOException ioex) {
                }
                successfulDownload = false;
            }
        }
    }

    /**
   * A <code>Thread</code> that is used to read the <code>SOAP
   * response</code> from the <code>XTVD webservice</code>, and update
   * the {@link #outputArea} with the response.
   */
    class ResponseThread extends Thread {

        /**
     * A <code>Reader</code> that is used to read the response from
     * the webservice.
     */
        private BufferedReader reader;

        /**
     * Create a new instance of the <code>thread</code>, and initialise
     * the reader with the specified reader.
     */
        public ResponseThread(BufferedReader reader) {
            this.reader = reader;
        }

        /**
     * Defines the actions to be performed by this thread when it runs.
     * Read the lines of text from the {@link 
     * XTVDFrame.ResponseThread#reader}, and append the line to the 
     * {@link #outputArea}.
     */
        public void run() {
            displayDownloadingMessage();
            try {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    outputArea.append(line);
                    outputArea.append(XTVDFrame.NEWLINE);
                }
                reader.close();
            } catch (Throwable t) {
                logArea.append(DataDirectException.getStackTraceString(t));
                successfulDownload = false;
            }
            displayDownloadStatus();
        }

        /**
     * Update the {@link #messageLabel} with a <code>Downloading data.
     * </code> message.
     */
        private void displayDownloadingMessage() {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    messageLabel.setText("Downloading data.");
                    outputArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    outputArea.setText("");
                }
            });
        }

        /**
     * Update the {@link #messageLabel} with a message regarding the
     * success or failure of the download attempt.
     */
        private void displayDownloadStatus() {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    if (successfulDownload) {
                        messageLabel.setText("Finished! Click \"Save XTVD\" to save the downloaded document.");
                        saveXTVDButton.setEnabled(true);
                    } else {
                        messageLabel.setText("Failed! View the \"Server Messages\" area for error information.");
                    }
                    sendButton.setEnabled(true);
                    outputArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            });
        }
    }

    /**
   * The <code>Action</code> that is associated with the {@link 
   * #sendButton}.  Makes a <code>SOAP request</code> to the
   * <code>XTVD webservice</code>.
   *
   * @since ddclient version 1.3
   */
    class RequestAction extends AbstractAction {

        /**
     * Set the parameters for the {@link 
     * com.tms.webservices.applications.xtvd.SOAPRequest} class from
     * the input fields, and make the SOAP request.  Start new 
     * {@link LogReaderThread}, {@link XTVDFrame.RequestThread} and 
     * {@link XTVDFrame.ResponseThread} instances to get the data.
     */
        public void actionPerformed(ActionEvent event) {
            try {
                String userName = userNameField.getText();
                String password = new String(passwordField.getPassword());
                if (userName == null || userName.equals("") || password == null || password.equals("")) {
                    JOptionPane.showInternalMessageDialog(XTVDFrame.this, "Username or password not specified", "Missing Information", JOptionPane.ERROR_MESSAGE);
                } else {
                    sendButton.setEnabled(false);
                    saveXTVDButton.setEnabled(false);
                    xtvdClient.soapRequest.setUserName(userName);
                    xtvdClient.soapRequest.setPassword(password);
                    xtvdClient.soapRequest.setWebserviceURI(webserviceURIField.getText());
                    xtvdClient.start.setTime(xtvdClient.sdf.parse(startTimeField.getText()));
                    xtvdClient.end.setTime(xtvdClient.sdf.parse(endTimeField.getText()));
                    try {
                        PipedWriter out = new PipedWriter();
                        PipedReader in = new PipedReader(out);
                        xtvdClient.soapRequest.setLog(out);
                        new LogReaderThread(new BufferedReader(in), logArea).start();
                    } catch (IOException ioex) {
                        logArea.append(DataDirectException.getStackTraceString(ioex));
                    }
                    PipedOutputStream pipedOutputStream = new PipedOutputStream();
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(pipedOutputStream, "UTF-8"));
                    BufferedReader in = new BufferedReader(new InputStreamReader(new PipedInputStream(pipedOutputStream), "UTF-8"));
                    new RequestThread(out).start();
                    new ResponseThread(in).start();
                }
            } catch (Throwable t) {
                logArea.append(DataDirectException.getStackTraceString(t));
            }
        }
    }

    /**
   * The <code>Action</code> that is associated with the {@link
   * #savePropertiesButton}.  Update the properties file with the values
   * in the input fields.
   *
   * @see XTVDProperties#saveProperties( String, String, int, String )
   *
   * @since ddclient version 1.3
   */
    class SavePropertiesAction extends AbstractAction {

        /**
     * Save the values in the input fields to the associated properties
     * file.  Pop up a <code>PLAIN_MESSAGE dialog</code> that allows
     * the user the ability the over-ride the default <code>numberOfDays
     * </code> of data property value.
     */
        public void actionPerformed(ActionEvent event) {
            Object[] values = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" };
            int numberOfDays = xtvdClient.xtvdProperties.getNumberOfDays();
            String days = (String) JOptionPane.showInternalInputDialog(XTVDFrame.this, "Enter the default number\nof days of data to download.", "Number of Days", JOptionPane.PLAIN_MESSAGE, null, values, String.valueOf(numberOfDays));
            if (days != null && days.length() > 0) {
                numberOfDays = Integer.parseInt(days);
            }
            try {
                xtvdClient.xtvdProperties.saveProperties(userNameField.getText(), new String(passwordField.getPassword()), numberOfDays, webserviceURIField.getText());
            } catch (DataDirectException ddex) {
                logArea.append(DataDirectException.getStackTraceString(ddex));
            }
        }
    }

    /**
   * The <code>Action</code> that is associated with the {@link
   * #saveXTVDButton}.  Save the text in the {@link #outputArea}
   * to a file specified by the user.
   *
   * @since ddclient version 1.3
   */
    class SaveXTVDAction extends AbstractAction {

        /**
     * Display a <code>SaveDialog</code> using {@link #fileChooser}.
     * Write the contents of the {@link #outputArea} to the file
     * selected by the user.
     */
        public void actionPerformed(ActionEvent event) {
            sendButton.setEnabled(false);
            saveXTVDButton.setEnabled(false);
            int value = fileChooser.showSaveDialog(XTVDFrame.this);
            if (value == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileChooser.getSelectedFile()), "UTF-8"));
                    writer.write(outputArea.getText());
                    writer.flush();
                    writer.close();
                } catch (IOException ioex) {
                    logArea.append(DataDirectException.getStackTraceString(ioex));
                }
            }
            sendButton.setEnabled(true);
            saveXTVDButton.setEnabled(true);
        }
    }
}
