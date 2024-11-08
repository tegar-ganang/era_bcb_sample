package org.wakhok.utils;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;
import org.wakhok.chart.DynamicMemoryChart;
import org.wakhok.grammar.Grammar;
import org.wakhok.index.DocProperty;
import org.wakhok.index.FileProperty;
import org.wakhok.index.ImageProperty;
import org.wakhok.index.Mp3Property;
import org.wakhok.space.Index;
import org.wakhok.space.PrivateRequest;
import org.wakhok.space.Request;
import org.wakhok.space.SpaceAccessor;
import org.wakhok.space.SpaceSearch;
import org.wakhok.thread.propertyThread;

/**
 *
 * @author  bishal acharya :bishalacharya@gmail.com
 */
public class FileSearch extends javax.swing.JFrame implements Runnable, ActionListener {

    public static String channelName;

    JavaSpace space;

    SpaceSearch objSearch;

    JFileChooser fc;

    Thread searchThread;

    DefaultListModel model, model1, monitorModel;

    public static DefaultListModel browserModel, threadParameterModel, virtualMemoryListModel;

    Object data[][], dataThread[][];

    long startTime, endTime, spaceFindTime, writeTime, startTimeUpload, spaceFindTimeUpload, writeTimeUpload;

    private long speedControl = 2000;

    ArrayList friendsList, indexArrayList;

    String[] namesList;

    public static DefaultTableModel objModel, threadPropjTableModel;

    Thread frensThread;

    boolean notifyFlag = false;

    String[] namePairs = new String[2];

    static Monitor mon;

    private String newline = System.getProperty("line.separator");

    private String userName, Props;

    byte[] binaryData = null;

    propertyThread objPropThread;

    MemoryFields objMemFields;

    Grammar objGrammar;

    LoggerImpl objLogger;

    DynamicMemoryChart objDynamic;

    String downloadPath = System.getProperty("user.dir").concat("/downloads/");

    String fileName, privateFileName;

    Object[] options = { "Save file", "Run File", "Cancel" };

    /** Creates new form FileSearch */
    public FileSearch() {
        initComponents();
        objLogger = new LoggerImpl();
        try {
            space = SpaceAccessor.getSpace();
            objLogger.logger.info("JavaSpace was successfully created");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not find space");
            objLogger.logger.error("Could not find space" + ex);
        }
        buttonGroup.add(fastRadio);
        buttonGroup.add(mediumRadio);
        buttonGroup.add(slowRadio);
        buttonGroup1.add(browserPauseRadioButton);
        buttonGroup1.add(browserRestartRadioButton);
        buttonGroup1.add(browserStopRadioButton);
        fastRadio.addActionListener(this);
        mediumRadio.addActionListener(this);
        slowRadio.addActionListener(this);
        buttonGroup.setSelected(mediumRadio.getModel(), true);
        browserPauseRadioButton.addActionListener(this);
        browserRestartRadioButton.addActionListener(this);
        browserStopRadioButton.addActionListener(this);
        objModel = new DefaultTableModel(data, new String[] { "FileName", "id", "Type", "UserName", "ChannelName", "Time Taken to Search" });
        threadPropjTableModel = new DefaultTableModel(dataThread, new String[] { "Thread Name", "Thread Priority", "Thread Group" });
        SearchTable.setModel(objModel);
        threadPropjTable.setModel(threadPropjTableModel);
        SearchTable.setAutoCreateRowSorter(true);
        model = new DefaultListModel();
        model1 = new DefaultListModel();
        monitorModel = new DefaultListModel();
        browserModel = new DefaultListModel();
        threadParameterModel = new DefaultListModel();
        virtualMemoryListModel = new DefaultListModel();
        fc = new JFileChooser();
        notifyList.setModel(model1);
        propertyList.setModel(model);
        processMemoryList.setModel(monitorModel);
        browserList.setModel(browserModel);
        threadParameterList.setModel(threadParameterModel);
        virtualMemoryList.setModel(virtualMemoryListModel);
        indexArrayList = new ArrayList();
        propertyList.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object lastIndex = model.getElementAt(propertyList.getModel().getSize() - 1);
                    int lastIndex1 = Integer.parseInt(lastIndex.toString());
                    int n = JOptionPane.showOptionDialog(FileSearch.getFrames()[0], "What would you like to do with " + "the file?", "Question", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                    if (n == JOptionPane.YES_OPTION) {
                        Request finalResult = objSearch.Search(channelName, lastIndex1);
                        Utils.putRawData(finalResult.data, downloadPath + finalResult.inputName);
                        JOptionPane.showMessageDialog(null, "Successfully downloaded file to :" + downloadPath);
                    } else if (n == JOptionPane.NO_OPTION) {
                        System.out.println("No");
                        Request finalResult = objSearch.Search(channelName, lastIndex1);
                        Utils.runJar(finalResult.data, downloadPath + finalResult.inputName);
                    } else if (n == JOptionPane.CANCEL_OPTION) {
                    } else {
                        System.out.println("tell me");
                    }
                }
            }
        });
        notifyList.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    model.clear();
                    model.addElement(notifyList.getSelectedValue());
                    namePairs[0] = indexArrayList.get(notifyList.getSelectedIndex()).toString();
                    namePairs[1] = notifyList.getSelectedValue().toString();
                    clearTable(objModel);
                    notifyFlag = true;
                }
            }
        });
        jMenuItem1.addActionListener(new MyActionListener());
        jMenuItem2.addActionListener(new MyActionListener());
        jMenuItem3.addActionListener(new MyActionListener());
        jMenuItem4.addActionListener(new MyActionListener());
        jMenuItem5.addActionListener(new MyActionListener());
        jMenuItem6.addActionListener(new MyActionListener());
        jMenuItem7.addActionListener(new MyActionListener());
        jMenuItem8.addActionListener(new MyActionListener());
        friendsList = new ArrayList();
        FileHandling objHandle = new FileHandling();
        friendsList = objHandle.read();
        namesList = friendsList.toString().split(" ");
        mon.stop();
        String[] monSplit = mon.toString().split("=");
        monitorModel.addElement(mon.getLabel());
        monitorModel.addElement("Active" + mon.getActive());
        monitorModel.addElement("Average" + mon.getAvg());
        monitorModel.addElement("Average Active" + mon.getAvgActive());
        monitorModel.addElement("Hits" + mon.getHits());
        monitorModel.addElement("DetailRow" + mon.getJAMonDetailRow());
        monitorModel.addElement("LastValue" + mon.getLastValue());
        monitorModel.addElement("Units" + mon.getUnits());
        objPropThread = new propertyThread();
        objMemFields = new MemoryFields();
        initializeComboBox();
        objDynamic = new DynamicMemoryChart();
        monitorTabbedPane.add("Performance", objDynamic.DynamicMemoryChart("memoryChart"));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        buttonGroup = new javax.swing.ButtonGroup();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        leftPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        mp3Button = new javax.swing.JButton();
        videoButton = new javax.swing.JButton();
        documentButton = new javax.swing.JButton();
        FileButton = new javax.swing.JButton();
        imageButton = new javax.swing.JButton();
        othersButton = new javax.swing.JButton();
        searchPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTabbedPane4 = new javax.swing.JTabbedPane();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        slowRadio = new javax.swing.JRadioButton();
        mediumRadio = new javax.swing.JRadioButton();
        fastRadio = new javax.swing.JRadioButton();
        jPanel13 = new javax.swing.JPanel();
        totalMemoryLabel = new javax.swing.JLabel();
        freeMemoryLabel = new javax.swing.JLabel();
        gcLabel = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        notifyList = new javax.swing.JList();
        rightPanel = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        SearchTable = new javax.swing.JTable();
        jTabbedPane3 = new javax.swing.JTabbedPane();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        propertyList = new javax.swing.JList();
        jPanel3 = new javax.swing.JPanel();
        jPanel24 = new javax.swing.JPanel();
        queryTextField = new javax.swing.JTextField();
        browserButton = new javax.swing.JButton();
        jPanel25 = new javax.swing.JPanel();
        jScrollPane13 = new javax.swing.JScrollPane();
        browserList = new javax.swing.JList();
        jPanel26 = new javax.swing.JPanel();
        browserPauseRadioButton = new javax.swing.JRadioButton();
        browserRestartRadioButton = new javax.swing.JRadioButton();
        browserStopRadioButton = new javax.swing.JRadioButton();
        objCountLabel = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        searchLogComboBox = new javax.swing.JComboBox();
        jPanel29 = new javax.swing.JPanel();
        jScrollPane15 = new javax.swing.JScrollPane();
        logEditorPane = new javax.swing.JEditorPane();
        jPanel16 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        threadParameterList = new javax.swing.JList();
        jPanel18 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane14 = new javax.swing.JScrollPane();
        threadPropjTable = new javax.swing.JTable();
        refreshLabel = new javax.swing.JLabel();
        jPanel20 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        virtualMemoryList = new javax.swing.JList();
        jLabel4 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        monitorTabbedPane = new javax.swing.JTabbedPane();
        jScrollPane7 = new javax.swing.JScrollPane();
        processMemoryList = new javax.swing.JList();
        jPanel2 = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        emailTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        authorTextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        fileTypeComboBox = new javax.swing.JComboBox();
        fileNameTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        browseButton = new javax.swing.JButton();
        uploadButton = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane8 = new javax.swing.JScrollPane();
        descriptionTextArea = new javax.swing.JTextArea();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane9 = new javax.swing.JScrollPane();
        advPropTextArea = new javax.swing.JTextArea();
        jPanel22 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        privateEmailTextField = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel18 = new javax.swing.JLabel();
        privatefileTextField = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        tagButton = new javax.swing.JButton();
        jScrollPane10 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jScrollPane11 = new javax.swing.JScrollPane();
        jTextArea4 = new javax.swing.JTextArea();
        jLabel19 = new javax.swing.JLabel();
        PasswordField = new javax.swing.JPasswordField();
        jLabel20 = new javax.swing.JLabel();
        jPanel27 = new javax.swing.JPanel();
        jTabbedPane5 = new javax.swing.JTabbedPane();
        jPanel23 = new javax.swing.JPanel();
        jScrollPane12 = new javax.swing.JScrollPane();
        monitorTextArea = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        jSplitPane1.setDividerLocation(220);
        jPanel4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        mp3Button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ei0021-48.gif")));
        mp3Button.setText("mp3");
        mp3Button.setToolTipText("mp3's");
        mp3Button.setIconTextGap(0);
        mp3Button.setPreferredSize(new java.awt.Dimension(80, 27));
        mp3Button.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mp3ButtonActionPerformed(evt);
            }
        });
        videoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/wi0054-48.gif")));
        videoButton.setText("video");
        videoButton.setIconTextGap(0);
        videoButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                videoButtonActionPerformed(evt);
            }
        });
        documentButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/wi0122-48.gif")));
        documentButton.setText("Documents");
        documentButton.setIconTextGap(0);
        documentButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                documentButtonActionPerformed(evt);
            }
        });
        FileButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/wi0063-48.gif")));
        FileButton.setText("Files");
        FileButton.setToolTipText("files");
        FileButton.setIconTextGap(0);
        FileButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileButtonActionPerformed(evt);
            }
        });
        imageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ei0021-48.gif")));
        imageButton.setText("Image");
        imageButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageButtonActionPerformed(evt);
            }
        });
        othersButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/wi0054-48.gif")));
        othersButton.setText("Others");
        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(imageButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(documentButton, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE).addComponent(mp3Button, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)).addGap(22, 22, 22).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(videoButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE).addComponent(FileButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE).addComponent(othersButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(mp3Button, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(videoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(documentButton, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(FileButton, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(imageButton).addComponent(othersButton))));
        searchPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel1.setFont(new java.awt.Font("MS UI Gothic", 1, 14));
        jLabel1.setText("Advanced Search");
        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 210, Short.MAX_VALUE));
        jPanel7Layout.setVerticalGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 223, Short.MAX_VALUE));
        jTabbedPane4.addTab("mp3", jPanel7);
        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 210, Short.MAX_VALUE));
        jPanel8Layout.setVerticalGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 223, Short.MAX_VALUE));
        jTabbedPane4.addTab("Video", jPanel8);
        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 210, Short.MAX_VALUE));
        jPanel9Layout.setVerticalGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 223, Short.MAX_VALUE));
        jTabbedPane4.addTab("Images", jPanel9);
        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 210, Short.MAX_VALUE));
        jPanel10Layout.setVerticalGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 223, Short.MAX_VALUE));
        jTabbedPane4.addTab("Files", jPanel10);
        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 210, Short.MAX_VALUE));
        jPanel11Layout.setVerticalGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 223, Short.MAX_VALUE));
        jTabbedPane4.addTab("others", jPanel11);
        javax.swing.GroupLayout searchPanelLayout = new javax.swing.GroupLayout(searchPanel);
        searchPanel.setLayout(searchPanelLayout);
        searchPanelLayout.setHorizontalGroup(searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(searchPanelLayout.createSequentialGroup().addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(84, Short.MAX_VALUE)).addComponent(jTabbedPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE));
        searchPanelLayout.setVerticalGroup(searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(searchPanelLayout.createSequentialGroup().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jTabbedPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE).addContainerGap()));
        jPanel12.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        slowRadio.setText("slow");
        mediumRadio.setText("medium");
        fastRadio.setText("fast");
        fastRadio.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fastRadioActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel12Layout.createSequentialGroup().addComponent(slowRadio, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(mediumRadio).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE).addComponent(fastRadio, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        jPanel12Layout.setVerticalGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel12Layout.createSequentialGroup().addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(fastRadio).addComponent(mediumRadio)).addComponent(slowRadio)).addContainerGap(13, Short.MAX_VALUE)));
        jPanel13.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        totalMemoryLabel.setText("Total memory");
        freeMemoryLabel.setText("Free Memory");
        gcLabel.setText("GC off");
        gcLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                gcLabelMouseClicked(evt);
            }
        });
        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel13Layout.createSequentialGroup().addGap(5, 5, 5).addComponent(totalMemoryLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(freeMemoryLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(gcLabel).addContainerGap(17, Short.MAX_VALUE)));
        jPanel13Layout.setVerticalGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(totalMemoryLabel).addComponent(freeMemoryLabel).addComponent(gcLabel)).addContainerGap()));
        jPanel15.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane3.setViewportView(notifyList);
        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE));
        jPanel15Layout.setVerticalGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE));
        javax.swing.GroupLayout leftPanelLayout = new javax.swing.GroupLayout(leftPanel);
        leftPanel.setLayout(leftPanelLayout);
        leftPanelLayout.setHorizontalGroup(leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(searchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        leftPanelLayout.setVerticalGroup(leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(leftPanelLayout.createSequentialGroup().addContainerGap().addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(searchPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        jSplitPane1.setLeftComponent(leftPanel);
        jTabbedPane2.setBackground(new java.awt.Color(213, 226, 230));
        jTabbedPane2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        SearchTable.setAutoCreateRowSorter(true);
        SearchTable.setBackground(new java.awt.Color(213, 226, 230));
        SearchTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null } }, new String[] { "FileName", "FileId", "Type", "UserName", "Time Taken To Search" }) {

            boolean[] canEdit = new boolean[] { false, false, false, false, false };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane1.setViewportView(SearchTable);
        jScrollPane2.setViewportView(propertyList);
        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 795, Short.MAX_VALUE));
        jPanel5Layout.setVerticalGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel5Layout.createSequentialGroup().addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(15, Short.MAX_VALUE)));
        jTabbedPane3.addTab("Advanced Properties", jPanel5);
        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE).addComponent(jTabbedPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE));
        jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup().addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jTabbedPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)));
        jTabbedPane2.addTab("Search Results", jPanel6);
        javax.swing.GroupLayout rightPanelLayout = new javax.swing.GroupLayout(rightPanel);
        rightPanel.setLayout(rightPanelLayout);
        rightPanelLayout.setHorizontalGroup(rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 807, Short.MAX_VALUE));
        rightPanelLayout.setVerticalGroup(rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 637, Short.MAX_VALUE));
        jSplitPane1.setRightComponent(rightPanel);
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1033, Short.MAX_VALUE));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 639, Short.MAX_VALUE));
        jTabbedPane1.addTab("Search", jPanel1);
        jPanel24.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        queryTextField.setText("JQL->");
        browserButton.setText("Search");
        browserButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browserButtonActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel24Layout.createSequentialGroup().addComponent(queryTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 955, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(browserButton)));
        jPanel24Layout.setVerticalGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel24Layout.createSequentialGroup().addGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(browserButton).addComponent(queryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(16, Short.MAX_VALUE)));
        jPanel25.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        browserList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                browserListMouseClicked(evt);
            }
        });
        jScrollPane13.setViewportView(browserList);
        jPanel26.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        browserPauseRadioButton.setText("Pause");
        browserRestartRadioButton.setText("Restart");
        browserStopRadioButton.setText("Stop");
        objCountLabel.setText("No of Objects affected");
        javax.swing.GroupLayout jPanel26Layout = new javax.swing.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel26Layout.createSequentialGroup().addContainerGap().addComponent(browserPauseRadioButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(browserRestartRadioButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(browserStopRadioButton).addGap(18, 18, 18).addComponent(objCountLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(652, Short.MAX_VALUE)));
        jPanel26Layout.setVerticalGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel26Layout.createSequentialGroup().addContainerGap(16, Short.MAX_VALUE).addGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(browserPauseRadioButton).addComponent(browserRestartRadioButton).addComponent(browserStopRadioButton).addComponent(objCountLabel)).addContainerGap()));
        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane13, javax.swing.GroupLayout.DEFAULT_SIZE, 1029, Short.MAX_VALUE).addComponent(jPanel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        jPanel25Layout.setVerticalGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel25Layout.createSequentialGroup().addComponent(jScrollPane13, javax.swing.GroupLayout.PREFERRED_SIZE, 503, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(jPanel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel25, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addComponent(jPanel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel25, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        jTabbedPane1.addTab("Browser", jPanel3);
        jPanel28.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jButton1.setText("view Log");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel28Layout = new javax.swing.GroupLayout(jPanel28);
        jPanel28.setLayout(jPanel28Layout);
        jPanel28Layout.setHorizontalGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel28Layout.createSequentialGroup().addComponent(searchLogComboBox, 0, 940, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jButton1)));
        jPanel28Layout.setVerticalGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel28Layout.createSequentialGroup().addGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jButton1).addComponent(searchLogComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel29.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        logEditorPane.setContentType("text/HTML");
        logEditorPane.setDragEnabled(true);
        jScrollPane15.setViewportView(logEditorPane);
        javax.swing.GroupLayout jPanel29Layout = new javax.swing.GroupLayout(jPanel29);
        jPanel29.setLayout(jPanel29Layout);
        jPanel29Layout.setHorizontalGroup(jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane15, javax.swing.GroupLayout.DEFAULT_SIZE, 1029, Short.MAX_VALUE));
        jPanel29Layout.setVerticalGroup(jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane15, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE));
        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel28, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel29, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        jPanel14Layout.setVerticalGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel14Layout.createSequentialGroup().addComponent(jPanel28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jPanel29, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jTabbedPane1.addTab("Logs", jPanel14);
        jPanel17.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel2.setFont(new java.awt.Font("Agency FB", 1, 14));
        jLabel2.setText("Thread Parameters");
        jScrollPane5.setViewportView(threadParameterList);
        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel17Layout.createSequentialGroup().addContainerGap().addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE).addComponent(jLabel2)).addContainerGap()));
        jPanel17Layout.setVerticalGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel17Layout.createSequentialGroup().addGap(4, 4, 4).addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(31, Short.MAX_VALUE)));
        jPanel18.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel3.setFont(new java.awt.Font("Agency FB", 1, 14));
        jLabel3.setText("Thread Details");
        threadPropjTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null }, { null, null, null }, { null, null, null }, { null, null, null }, { null, null, null }, { null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3" }));
        jScrollPane14.setViewportView(threadPropjTable);
        refreshLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Refresh24.gif")));
        refreshLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                refreshThreadTable(evt);
            }
        });
        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel18Layout.createSequentialGroup().addContainerGap().addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane14, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE).addGroup(jPanel18Layout.createSequentialGroup().addComponent(jLabel3).addGap(18, 18, 18).addComponent(refreshLabel))).addContainerGap()));
        jPanel18Layout.setVerticalGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel18Layout.createSequentialGroup().addGap(10, 10, 10).addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel3).addComponent(refreshLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane14, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE).addContainerGap()));
        jPanel20.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane4.setViewportView(virtualMemoryList);
        jLabel4.setFont(new java.awt.Font("Agency FB", 1, 14));
        jLabel4.setText("Virtual Memory Parameters");
        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel20Layout.createSequentialGroup().addContainerGap().addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE).addComponent(jLabel4)).addContainerGap()));
        jPanel20Layout.setVerticalGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel20Layout.createSequentialGroup().addGap(13, 13, 13).addComponent(jLabel4).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(18, Short.MAX_VALUE)));
        jPanel19.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel5.setFont(new java.awt.Font("Agency FB", 1, 14));
        jLabel5.setText("JavaSpace Monitor");
        jScrollPane7.setViewportView(processMemoryList);
        monitorTabbedPane.addTab("Monitor", jScrollPane7);
        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel19Layout.createSequentialGroup().addContainerGap().addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(monitorTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1005, Short.MAX_VALUE).addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        jPanel19Layout.setVerticalGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel19Layout.createSequentialGroup().addGap(10, 10, 10).addComponent(jLabel5).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(monitorTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE).addContainerGap()));
        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel16Layout.createSequentialGroup().addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))).addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        jPanel16Layout.setVerticalGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel16Layout.createSequentialGroup().addContainerGap().addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(jPanel16Layout.createSequentialGroup().addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addGap(36, 36, 36).addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        jTabbedPane1.addTab("Runtime Figures", jPanel16);
        jPanel21.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel6.setText("Your Name");
        jLabel7.setText("Email Address");
        jLabel8.setText("File Author");
        jLabel9.setText("Type of File");
        fileTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "music", "video", "Image", "Document", "file" }));
        jLabel10.setText("FileName");
        browseButton.setText("Browse");
        browseButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });
        uploadButton.setText("Upload");
        uploadButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uploadButtonActionPerformed(evt);
            }
        });
        jLabel11.setFont(new java.awt.Font("Agency FB", 1, 14));
        jLabel11.setText("Enter Details for the files to Upload");
        descriptionTextArea.setColumns(20);
        descriptionTextArea.setRows(5);
        descriptionTextArea.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane8.setViewportView(descriptionTextArea);
        jLabel12.setText("Tags");
        advPropTextArea.setColumns(20);
        advPropTextArea.setEditable(false);
        advPropTextArea.setRows(5);
        advPropTextArea.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane9.setViewportView(advPropTextArea);
        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel21Layout.createSequentialGroup().addGap(33, 33, 33).addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel11).addGroup(jPanel21Layout.createSequentialGroup().addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel6).addGroup(jPanel21Layout.createSequentialGroup().addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jLabel10).addComponent(jLabel9).addComponent(uploadButton).addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addGap(47, 47, 47).addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel21Layout.createSequentialGroup().addComponent(fileNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(browseButton)).addComponent(emailTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE).addComponent(authorTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE).addComponent(nameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE).addComponent(fileTypeComboBox, 0, 192, Short.MAX_VALUE)))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel12).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(jScrollPane9).addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)))).addContainerGap()));
        jPanel21Layout.setVerticalGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel21Layout.createSequentialGroup().addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel21Layout.createSequentialGroup().addContainerGap().addComponent(jLabel11).addGap(13, 13, 13).addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel6).addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel12)).addGap(9, 9, 9).addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jLabel7).addComponent(emailTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(authorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel8))).addGroup(jPanel21Layout.createSequentialGroup().addGap(38, 38, 38).addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGap(18, 18, 18).addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel21Layout.createSequentialGroup().addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(fileTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel9)).addGap(28, 28, 28).addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel10).addComponent(fileNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(browseButton)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(uploadButton)).addComponent(jScrollPane9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(63, Short.MAX_VALUE)));
        jPanel22.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel13.setFont(new java.awt.Font("Agency FB", 1, 14));
        jLabel13.setText("Tag a Friend");
        jLabel14.setText("Friends Name");
        jLabel15.setText("Friends Email");
        jLabel16.setText("Author of File");
        jLabel17.setText("Type Of File");
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jLabel18.setText("FileName");
        jButton3.setText("Browse");
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        tagButton.setText("Upload");
        tagButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagButtonActionPerformed(evt);
            }
        });
        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jTextArea3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane10.setViewportView(jTextArea3);
        jTextArea4.setColumns(20);
        jTextArea4.setRows(5);
        jTextArea4.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane11.setViewportView(jTextArea4);
        jLabel19.setText("Tags");
        PasswordField.setText("jP");
        jLabel20.setText("Password");
        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel22Layout.createSequentialGroup().addContainerGap().addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel13).addGroup(jPanel22Layout.createSequentialGroup().addGap(10, 10, 10).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel14).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(jLabel16, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jLabel15, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jLabel18, javax.swing.GroupLayout.Alignment.LEADING).addComponent(tagButton, javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel20, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jLabel17, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(jPanel22Layout.createSequentialGroup().addGap(3, 3, 3).addComponent(privatefileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(jPanel22Layout.createSequentialGroup().addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jComboBox2, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jTextField7, javax.swing.GroupLayout.Alignment.LEADING).addComponent(privateEmailTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE).addComponent(jTextField5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE).addComponent(PasswordField, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel19))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane11, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE).addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)).addContainerGap()));
        jPanel22Layout.setVerticalGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel22Layout.createSequentialGroup().addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(jPanel22Layout.createSequentialGroup().addContainerGap().addComponent(jLabel13).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel14).addComponent(jLabel19).addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel15).addComponent(privateEmailTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel16).addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel20).addComponent(PasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(jPanel22Layout.createSequentialGroup().addGap(36, 36, 36).addComponent(jScrollPane10, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel22Layout.createSequentialGroup().addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel17).addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(18, 18, 18).addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel18).addComponent(privatefileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jButton3)).addGap(26, 26, 26).addComponent(tagButton).addGap(69, 69, 69)).addGroup(jPanel22Layout.createSequentialGroup().addGap(18, 18, 18).addComponent(jScrollPane11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()))));
        jPanel27.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        monitorTextArea.setColumns(20);
        monitorTextArea.setRows(5);
        jScrollPane12.setViewportView(monitorTextArea);
        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane12, javax.swing.GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE));
        jPanel23Layout.setVerticalGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane12, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE));
        jTabbedPane5.addTab("monitor", jPanel23);
        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1005, Short.MAX_VALUE));
        jPanel27Layout.setVerticalGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE));
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jPanel27, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(jPanel2Layout.createSequentialGroup().addComponent(jPanel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))).addContainerGap()));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addGap(41, 41, 41).addComponent(jPanel27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        jTabbedPane1.addTab("Upload", jPanel2);
        jMenu1.setText("File");
        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText("Open");
        jMenu1.add(jMenuItem1);
        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setText("Exit");
        jMenu1.add(jMenuItem2);
        jMenuBar1.add(jMenu1);
        jMenu2.setText("Search");
        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem3.setText("Stop");
        jMenu2.add(jMenuItem3);
        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem4.setText("Pause");
        jMenu2.add(jMenuItem4);
        jMenuItem5.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem5.setText("Restart");
        jMenu2.add(jMenuItem5);
        jMenuBar1.add(jMenu2);
        jMenu3.setText("Upload");
        jMenuItem6.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem6.setText("Status");
        jMenu3.add(jMenuItem6);
        jMenuItem7.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem7.setText("Friends");
        jMenu3.add(jMenuItem7);
        jMenuBar1.add(jMenu3);
        jMenu4.setText("Help");
        jMenuItem8.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem8.setText("Help");
        jMenu4.add(jMenuItem8);
        jMenuBar1.add(jMenu4);
        setJMenuBar(jMenuBar1);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1038, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 666, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pack();
    }

    private void documentButtonActionPerformed(java.awt.event.ActionEvent evt) {
        clearTable(objModel);
        model.clear();
        model1.clear();
        notifyFlag = false;
        this.channelName = "DocumentChannel";
        Runtime.getRuntime().gc();
        searchThread = new Thread(this);
        searchThread.start();
    }

    public void clearTable(DefaultTableModel objModel1) {
        for (int j = 0; j < objModel1.getRowCount(); j++) {
            objModel1.removeRow(j);
        }
    }

    public void checkThread() {
        if (searchThread.isAlive()) {
            searchThread.stop();
        }
    }

    private void mp3ButtonActionPerformed(java.awt.event.ActionEvent evt) {
        clearTable(objModel);
        model.clear();
        model1.clear();
        notifyFlag = false;
        this.channelName = "musicChannel";
        Runtime.getRuntime().gc();
        searchThread = new Thread(this);
        searchThread.start();
        SearchTable.addMouseListener(new MyMouseListener());
    }

    private void videoButtonActionPerformed(java.awt.event.ActionEvent evt) {
        clearTable(objModel);
        model.clear();
        model1.clear();
        notifyFlag = false;
        this.channelName = "videochannel";
        this.channelName = "musicChannel";
        Runtime.getRuntime().gc();
    }

    private void fastRadioActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void uploadButtonActionPerformed(java.awt.event.ActionEvent evt) {
        startTimeUpload = Calendar.getInstance().getTimeInMillis();
        if (space == null) {
            try {
                space = SpaceAccessor.getSpace();
                monitorTextArea.append("Space was found successfully: Time taken to find :");
                spaceFindTimeUpload = Calendar.getInstance().getTimeInMillis();
                monitorTextArea.append(java.lang.Long.toString(spaceFindTimeUpload - startTimeUpload) + "milliseconds" + newline);
            } catch (Exception ex) {
                objLogger.logger.error("Space could not be read" + ex);
            }
        } else {
            monitorTextArea.append("Space was found successfully");
            spaceFindTimeUpload = Calendar.getInstance().getTimeInMillis();
        }
        upload();
    }

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int returnVal = fc.showOpenDialog(FileSearch.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            fileNameTextField.setText(file.getPath());
            fileName = file.getName();
        }
    }

    private void browserButtonActionPerformed(java.awt.event.ActionEvent evt) {
        browserModel.clear();
        if (objGrammar != null) {
            if (objGrammar.isAlive()) {
                objLogger.logger.debug("Thread" + objGrammar);
                objGrammar.stop();
            }
        }
        String query = queryTextField.getText();
        if (query.contains("JQL->")) {
            query = query.replace("JQL->", " ").trim();
            objGrammar = new Grammar(query);
            objGrammar.start();
        }
    }

    private void refreshThreadTable(java.awt.event.MouseEvent evt) {
        clearTable(threadPropjTableModel);
        threadParameterModel.clear();
        virtualMemoryListModel.clear();
        objPropThread = new propertyThread();
        objMemFields = new MemoryFields();
    }

    private void gcLabelMouseClicked(java.awt.event.MouseEvent evt) {
        Runtime.getRuntime().gc();
        gcLabel.setText("GC ON");
    }

    public void initializeComboBox() {
        String[] logFiles;
        ListFilesSubs filesList = new ListFilesSubs();
        String path = System.getProperty("user.dir").concat("/log");
        logFiles = filesList.doSimpleFileListing(path);
        StringListComboBoxModel comboModel = new StringListComboBoxModel(logFiles);
        searchLogComboBox.setModel(comboModel);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        URL pageURL = null;
        String logFileName = searchLogComboBox.getSelectedItem().toString();
        String filePath = "file:///" + System.getProperty("user.dir").concat("/log/").concat(logFileName);
        try {
            pageURL = new URL(filePath);
        } catch (Exception ex) {
            System.out.println("MalformedURL");
        }
        try {
            logEditorPane.setPage(pageURL);
        } catch (IOException ex) {
        }
    }

    private void FileButtonActionPerformed(java.awt.event.ActionEvent evt) {
        clearTable(objModel);
        model.clear();
        model1.clear();
        notifyFlag = false;
        this.channelName = "fileChannel";
        searchThread = new Thread(this);
        searchThread.start();
        SearchTable.addMouseListener(new MyMouseListener());
    }

    private void imageButtonActionPerformed(java.awt.event.ActionEvent evt) {
        clearTable(objModel);
        model.clear();
        model1.clear();
        notifyFlag = false;
        this.channelName = "ImageChannel";
        searchThread = new Thread(this);
        searchThread.start();
        SearchTable.addMouseListener(new MyMouseListener());
    }

    private void tagButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (PasswordField.getText() != null || privateEmailTextField.getText() != null || jTextField7.getText() != null) {
            byte[] binaryData1 = Utils.getRawData(privatefileTextField.getText());
            Integer num = getRequestNumber("privateChannel");
            num = num + 1;
            String email = privateEmailTextField.getText();
            String password = PasswordField.getText();
            PrivateRequest request = new PrivateRequest("privateChannel", num, email, privateFileName, binaryData1, password);
            try {
                space.write(request, null, Lease.FOREVER);
                monitorTextArea.setText("successfully written file with id=" + num + "to the private space");
            } catch (Exception ex) {
                System.out.println("could not write to the space in private channel");
            }
        } else {
            JOptionPane.showMessageDialog(null, "please enter all fields");
        }
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        int returnVal = fc.showOpenDialog(FileSearch.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            privatefileTextField.setText(file.getPath());
            privateFileName = file.getName();
        }
    }

    private void browserListMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            String[] splitVal = browserList.getSelectedValue().toString().split(":");
            Integer indexId = Integer.parseInt(splitVal[0].trim());
            int n = JOptionPane.showOptionDialog(FileSearch.getFrames()[0], "What would you like to do with " + "the file?", "Question", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
            if (n == JOptionPane.YES_OPTION) {
                SpaceSearch objSearch1 = new SpaceSearch();
                if (channelName.equals("privateChannel")) {
                    PrivateRequest finalResult = objSearch1.privateSearch(channelName, indexId);
                    Utils.putRawData(finalResult.data, downloadPath + finalResult.inputName);
                } else {
                    Request finalResult1 = objSearch1.Search(channelName, indexId);
                    Utils.putRawData(finalResult1.data, downloadPath + finalResult1.inputName);
                }
            } else if (n == JOptionPane.NO_OPTION) {
                System.out.println("No");
                Request finalResult = objSearch.Search(channelName, indexId);
                byte[] returnt = finalResult.data;
                Utils.putRawData(finalResult.data, downloadPath + finalResult.inputName);
            } else if (n == JOptionPane.CANCEL_OPTION) {
                System.out.println("Cancel");
            } else {
                System.out.println("tell me");
            }
        }
    }

    public void upload() {
        userName = nameTextField.getText();
        String email = emailTextField.getText();
        String author = authorTextField.getText();
        String fileType = fileTypeComboBox.getModel().getSelectedItem().toString();
        String description = descriptionTextArea.getText();
        if (fileTypeComboBox.getSelectedItem() == "music") {
            Mp3Property objMp3 = new Mp3Property(fileNameTextField.getText());
            advPropTextArea.setText(objMp3.getmp3props());
        }
        if (fileTypeComboBox.getSelectedItem() == "Document") {
            DocProperty objDoc = new DocProperty(fileNameTextField.getText());
            advPropTextArea.setText(objDoc.getDocprops());
        }
        if (fileTypeComboBox.getSelectedItem() == "Image") {
            ImageProperty objImg = new ImageProperty(fileNameTextField.getText());
            advPropTextArea.setText(objImg.getImageprops());
        }
        if (fileTypeComboBox.getSelectedItem() == "file") {
            FileProperty objImg = new FileProperty(fileNameTextField.getText());
            advPropTextArea.setText(objImg.getFileprops());
        }
        Props = advPropTextArea.getText();
        if (userName.equals("")) {
            monitorTextArea.setText("Please Enter the UserName." + newline);
            return;
        }
        if (fileName.equals("")) {
            monitorTextArea.append("Enter absolute file name of a file." + newline);
            return;
        }
        if (email.equals("")) {
            monitorTextArea.append("Enter absolute email." + newline);
            return;
        }
        binaryData = Utils.getRawData(fileNameTextField.getText());
        if (binaryData != null) {
            appendRequest(fileType.concat("Channel"), email, fileName, binaryData, userName, description);
        }
    }

    private void appendRequest(String channel, String email, String fileName, byte[] binaryData, String userName, String description) {
        monitorTextArea.append("Searching for Channel of type " + channel + "..." + newline);
        Integer num = getRequestNumber(channel);
        num = num + 1;
        Request request = new Request(channel, num, email, fileName, binaryData, userName, description, Props);
        monitorTextArea.append("Channel Found uploading your file to " + channel + "..." + newline);
        try {
            space.write(request, null, Lease.FOREVER);
            objLogger.logger.info("Successfuly written to space :" + request.toString());
        } catch (Exception ex) {
            monitorTextArea.append("Eroor occured while writing file to space..." + newline);
            objLogger.logger.error("Could not write to the space" + ex);
            return;
        }
        writeTimeUpload = Calendar.getInstance().getTimeInMillis();
        monitorTextArea.append("Successfully Sent request: " + fileName + ",Request no" + " to \"" + channel + "\"....in total time =");
        monitorTextArea.append(java.lang.Long.toString((writeTimeUpload - spaceFindTimeUpload) / 1000) + "seconds" + newline);
    }

    private Integer getRequestNumber(String channel) {
        try {
            Index template = new Index("head", channel);
            Index head = (Index) space.take(template, null, Long.MAX_VALUE);
            head.increment();
            space.write(head, null, Lease.FOREVER);
            return head.getId();
        } catch (Exception e) {
            objLogger.logger.error("Could not get Request number" + e);
            return null;
        }
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        mon = MonitorFactory.start("JavaSpaceMonitor");
        try {
            javax.swing.UIManager.setLookAndFeel("com.birosoft.liquid.LiquidLookAndFeel");
            com.birosoft.liquid.LiquidLookAndFeel.setLiquidDecorations(true);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new FileSearch().setVisible(true);
            }
        });
    }

    private javax.swing.JButton FileButton;

    private javax.swing.JPasswordField PasswordField;

    private javax.swing.JTable SearchTable;

    private javax.swing.JTextArea advPropTextArea;

    private javax.swing.JTextField authorTextField;

    private javax.swing.JButton browseButton;

    private javax.swing.JButton browserButton;

    private javax.swing.JList browserList;

    private javax.swing.JRadioButton browserPauseRadioButton;

    private javax.swing.JRadioButton browserRestartRadioButton;

    private javax.swing.JRadioButton browserStopRadioButton;

    private javax.swing.ButtonGroup buttonGroup;

    private javax.swing.ButtonGroup buttonGroup1;

    private javax.swing.JTextArea descriptionTextArea;

    private javax.swing.JButton documentButton;

    private javax.swing.JTextField emailTextField;

    private javax.swing.JRadioButton fastRadio;

    private javax.swing.JTextField fileNameTextField;

    private javax.swing.JComboBox fileTypeComboBox;

    private javax.swing.JLabel freeMemoryLabel;

    private javax.swing.JLabel gcLabel;

    private javax.swing.JButton imageButton;

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton3;

    private javax.swing.JComboBox jComboBox2;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel11;

    private javax.swing.JLabel jLabel12;

    private javax.swing.JLabel jLabel13;

    private javax.swing.JLabel jLabel14;

    private javax.swing.JLabel jLabel15;

    private javax.swing.JLabel jLabel16;

    private javax.swing.JLabel jLabel17;

    private javax.swing.JLabel jLabel18;

    private javax.swing.JLabel jLabel19;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel20;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JLabel jLabel9;

    private javax.swing.JMenu jMenu1;

    private javax.swing.JMenu jMenu2;

    private javax.swing.JMenu jMenu3;

    private javax.swing.JMenu jMenu4;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JMenuItem jMenuItem1;

    private javax.swing.JMenuItem jMenuItem2;

    private javax.swing.JMenuItem jMenuItem3;

    private javax.swing.JMenuItem jMenuItem4;

    private javax.swing.JMenuItem jMenuItem5;

    private javax.swing.JMenuItem jMenuItem6;

    private javax.swing.JMenuItem jMenuItem7;

    private javax.swing.JMenuItem jMenuItem8;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel10;

    private javax.swing.JPanel jPanel11;

    private javax.swing.JPanel jPanel12;

    private javax.swing.JPanel jPanel13;

    private javax.swing.JPanel jPanel14;

    private javax.swing.JPanel jPanel15;

    private javax.swing.JPanel jPanel16;

    private javax.swing.JPanel jPanel17;

    private javax.swing.JPanel jPanel18;

    private javax.swing.JPanel jPanel19;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel20;

    private javax.swing.JPanel jPanel21;

    private javax.swing.JPanel jPanel22;

    private javax.swing.JPanel jPanel23;

    private javax.swing.JPanel jPanel24;

    private javax.swing.JPanel jPanel25;

    private javax.swing.JPanel jPanel26;

    private javax.swing.JPanel jPanel27;

    private javax.swing.JPanel jPanel28;

    private javax.swing.JPanel jPanel29;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel6;

    private javax.swing.JPanel jPanel7;

    private javax.swing.JPanel jPanel8;

    private javax.swing.JPanel jPanel9;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane10;

    private javax.swing.JScrollPane jScrollPane11;

    private javax.swing.JScrollPane jScrollPane12;

    private javax.swing.JScrollPane jScrollPane13;

    private javax.swing.JScrollPane jScrollPane14;

    private javax.swing.JScrollPane jScrollPane15;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JScrollPane jScrollPane4;

    private javax.swing.JScrollPane jScrollPane5;

    private javax.swing.JScrollPane jScrollPane7;

    private javax.swing.JScrollPane jScrollPane8;

    private javax.swing.JScrollPane jScrollPane9;

    private javax.swing.JSplitPane jSplitPane1;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JTabbedPane jTabbedPane2;

    private javax.swing.JTabbedPane jTabbedPane3;

    private javax.swing.JTabbedPane jTabbedPane4;

    private javax.swing.JTabbedPane jTabbedPane5;

    private javax.swing.JTextArea jTextArea3;

    private javax.swing.JTextArea jTextArea4;

    private javax.swing.JTextField jTextField5;

    private javax.swing.JTextField jTextField7;

    private javax.swing.JPanel leftPanel;

    private javax.swing.JEditorPane logEditorPane;

    private javax.swing.JRadioButton mediumRadio;

    private javax.swing.JTabbedPane monitorTabbedPane;

    private javax.swing.JTextArea monitorTextArea;

    private javax.swing.JButton mp3Button;

    private javax.swing.JTextField nameTextField;

    private javax.swing.JList notifyList;

    public static javax.swing.JLabel objCountLabel;

    private javax.swing.JButton othersButton;

    private javax.swing.JTextField privateEmailTextField;

    private javax.swing.JTextField privatefileTextField;

    private javax.swing.JList processMemoryList;

    private javax.swing.JList propertyList;

    private javax.swing.JTextField queryTextField;

    private javax.swing.JLabel refreshLabel;

    private javax.swing.JPanel rightPanel;

    private javax.swing.JComboBox searchLogComboBox;

    private javax.swing.JPanel searchPanel;

    private javax.swing.JRadioButton slowRadio;

    private javax.swing.JButton tagButton;

    private javax.swing.JList threadParameterList;

    private javax.swing.JTable threadPropjTable;

    private javax.swing.JLabel totalMemoryLabel;

    private javax.swing.JButton uploadButton;

    private javax.swing.JButton videoButton;

    private javax.swing.JList virtualMemoryList;

    public void run() {
        objSearch = new SpaceSearch();
        Request outputSearch;
        int index = 0;
        while (true) {
            totalMemoryLabel.setText("Total :" + Long.toString(Runtime.getRuntime().totalMemory() / (1024 * 1024)) + "MB");
            freeMemoryLabel.setText("Free :" + Long.toString(Runtime.getRuntime().freeMemory() / (1024 * 1024)) + "MB");
            startTime = Calendar.getInstance().getTimeInMillis();
            try {
                searchThread.sleep(this.speedControl);
            } catch (Exception ex) {
                System.out.println(ex);
            }
            if (this.notifyFlag == true) {
                outputSearch = objSearch.Search(channelName, Integer.parseInt(namePairs[0]), namePairs[1]);
            } else {
                outputSearch = objSearch.Search(channelName);
            }
            endTime = Calendar.getInstance().getTimeInMillis();
            String FName = outputSearch.inputName.substring(0, outputSearch.inputName.length() - 3);
            String extension = outputSearch.inputName.substring(outputSearch.inputName.length() - 3, outputSearch.inputName.length());
            String[] datatest = { FName, outputSearch.id.toString(), extension, outputSearch.userName, outputSearch.channelName, Long.toString((endTime - startTime)) + " milliseconds" };
            objModel.addRow(datatest);
            for (int i = 0; i < namesList.length; i++) {
                if (outputSearch.email.equals(namesList[i]) && !model1.contains(outputSearch.email)) {
                    model1.addElement(outputSearch.email);
                    indexArrayList.add(index, outputSearch.id);
                    index++;
                    break;
                }
            }
        }
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "fast") {
            this.speedControl = 0;
            Runtime.getRuntime().gc();
            gcLabel.setText("GC on");
        }
        if (e.getActionCommand() == "medium") {
            this.speedControl = 2000;
        }
        if (e.getActionCommand() == "slow") {
            this.speedControl = 4000;
        }
        if (e.getActionCommand() == "Pause") {
            System.out.println("paused");
            if (objGrammar.isAlive()) {
                objGrammar.suspend();
            }
        }
        if (e.getActionCommand() == "Restart") {
            System.out.println("restarted");
            if (objGrammar.isAlive()) {
                objGrammar.resume();
            }
        }
        if (e.getActionCommand() == "Stop") {
            if (objGrammar.isAlive()) {
                objGrammar.stop();
            }
        }
    }

    class MyActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == "Exit") {
                objLogger.logger.info("User exits from the system");
                System.exit(0);
            }
            if (e.getActionCommand() == "Stop") {
                searchThread.stop();
                clearTable(objModel);
            }
            if (e.getActionCommand() == "Pause") {
                searchThread.suspend();
            }
            if (e.getActionCommand() == "Restart") {
                searchThread.resume();
            }
            if (e.getActionCommand() == "Friends") {
                String val = JOptionPane.showInputDialog(null, "Input email address of your friend");
                if (val.contains("@") && val.contains(".")) {
                    System.out.println(val);
                    FileHandling objFile = new FileHandling();
                    objFile.write(val);
                } else {
                    JOptionPane.showMessageDialog(null, "Incorrect email address : Operation failed");
                }
            }
        }
    }

    class StringListComboBoxModel extends AbstractListModel implements ComboBoxModel {

        private Object selectedItem;

        private String[] anStringList;

        public StringListComboBoxModel(String[] stringList) {
            anStringList = stringList;
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public void setSelectedItem(Object newValue) {
            selectedItem = newValue;
        }

        public int getSize() {
            return anStringList.length;
        }

        public Object getElementAt(int i) {
            return anStringList[i];
        }
    }

    class MyMouseListener implements MouseListener {

        public void mouseClicked(MouseEvent e) {
            int objectId = Integer.parseInt(SearchTable.getValueAt(SearchTable.getSelectedRow(), new Integer(1)).toString());
            String ChName = SearchTable.getValueAt(SearchTable.getSelectedRow(), new Integer(4)).toString();
            Request tempReq = new Request(ChName, objectId);
            Request result = null;
            try {
                result = (Request) space.read(tempReq, null, Long.MAX_VALUE);
                objLogger.logger.info("template was read from the space from channel" + ChName + " with objId" + objectId);
            } catch (Exception ex) {
                objLogger.logger.error("Space could not be read" + ex);
            }
            model.clear();
            String[] propSplit = result.property.split("@");
            for (int i = 0; i < propSplit.length; i++) {
                model.addElement(propSplit[i]);
            }
            model.addElement(result.id);
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }
}
