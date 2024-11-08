package jcomicdownloader;

import java.util.logging.Level;
import java.util.logging.Logger;
import jcomicdownloader.table.*;
import jcomicdownloader.tools.*;
import jcomicdownloader.module.*;
import jcomicdownloader.enums.*;
import jcomicdownloader.frame.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.io.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.JFileChooser.*;

/**
 * @author surveyorK
 * @version 1.14
 * user 主介面，同時監聽window、mouse、button和textField。
 * */
public class ComicDownGUI extends JFrame implements ActionListener, DocumentListener, MouseListener, WindowFocusListener {

    public static JFrame mainFrame;

    private BorderLayout layout;

    private JPanel buttonPanel, textPanel;

    private JButton button[];

    private JTextArea messageArea;

    private JTextField urlField;

    private JLabel urlLabel, logoLabel;

    JTabbedPane tabbedPane;

    public static TrayIcon trayIcon;

    private JPopupMenu trayPopup;

    private JMenuItem trayShowItem;

    private JMenuItem trayStartItem;

    private JMenuItem trayStopItem;

    private JMenuItem trayExitItem;

    private JPopupMenu urlFieldPopup;

    private JMenuItem pasteSystemClipboardItem;

    private JPopupMenu downloadTablePopup;

    private int downloadTablePopupRow;

    private JMenuItem tableSearchDownloadComic;

    private JMenuItem tableSearchBookmarkComic;

    private JMenuItem tableSearchRecordComic;

    private JMenuItem tableOpenDownloadURL;

    private JMenuItem tableOpenDownloadFile;

    private JMenuItem tableOpenDownloadDirectoryItem;

    private JMenuItem tableAddBookmarkFromDownloadItem;

    private JMenuItem tableRechoiceVolumeItem;

    private JMenuItem tableDeleteMissionItem;

    private JMenuItem tableDeleteAllUnselectedMissionItem;

    private JMenuItem tableDeleteAllDoneMissionItem;

    private JMenuItem tableMoveToRoofItem;

    private JMenuItem tableMoveToFloorItem;

    private JPopupMenu bookmarkTablePopup;

    private int bookmarkTablePopupRow;

    private JMenuItem tableOpenBookmarkURL;

    private JMenuItem tableOpenBookmarkFile;

    private JMenuItem tableOpenBookmarkDirectoryItem;

    private JMenuItem tableAddMissionFromBookmarkItem;

    private JMenuItem tableDeleteBookmarkItem;

    private JPopupMenu recordTablePopup;

    private int recordTablePopupRow;

    private JMenuItem tableOpenRecordURL;

    private JMenuItem tableOpenRecordFile;

    private JMenuItem tableOpenRecordDirectoryItem;

    private JMenuItem tableAddBookmarkFromRecordItem;

    private JMenuItem tableAddMissionFromRecordItem;

    private JMenuItem tableDeleteRecordItem;

    public static LogFrame logFrame;

    public JTable downTable;

    public JTable bookmarkTable;

    public JTable recordTable;

    public static JLabel stateBar;

    public static DownloadTableModel downTableModel;

    public static BookmarkTableModel bookmarkTableModel;

    public static RecordTableModel recordTableModel;

    public static String[] downTableUrlStrings;

    public static String[] nowSelectedCheckStrings;

    public static int[][] downTableRealChoiceOrder;

    public static String defaultSkinClassName;

    private String[] args;

    private static String resourceFolder;

    private StringBuffer messageString;

    private Run mainRun;

    private int nowDownloadMissionRow;

    public static String versionString = "JComicDownloader  v2.14";

    public ComicDownGUI() {
        super(versionString);
        minimizeEvent();
        initTrayIcon();
        mainFrame = this;
        if (Common.isUnix()) {
            defaultSkinClassName = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        } else {
            defaultSkinClassName = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
        }
        resourceFolder = "resource" + Common.getSlash();
        downTableUrlStrings = new String[1000];
        downTableRealChoiceOrder = new int[2000][];
        Thread logFrameThread = new Thread(new Runnable() {

            public void run() {
                logFrame = new LogFrame();
                if (SetUp.getOpenDebugMessageWindow()) {
                    logFrame.setVisible(true);
                    Debug.commandDebugMode = false;
                } else {
                    logFrame.setVisible(false);
                    Debug.commandDebugMode = true;
                }
            }
        });
        logFrameThread.start();
        messageString = new StringBuffer("");
        setUpUIComponent();
        setUpeListener();
        setVisible(true);
        checkSkin();
    }

    private void checkSkin() {
        if (SetUp.getSkinClassName().matches("com.jtattoo.plaf.*") && !new File(Common.getNowAbsolutePath() + "JTattoo.jar").exists()) {
            new CommonGUI().downloadJTattoo();
        }
    }

    private void setUpUIComponent() {
        setSize(640, 480);
        setLocationRelativeTo(this);
        setIconImage(new CommonGUI().getImage("main_icon.png"));
        addWindowFocusListener(this);
        Container contentPane = getContentPane();
        setButton(contentPane);
        setText(contentPane);
        setSkin(SetUp.getSkinClassName());
        stateBar = new JLabel("請貼上網址");
        stateBar.setHorizontalAlignment(SwingConstants.LEFT);
        stateBar.setBorder(BorderFactory.createEtchedBorder());
        stateBar.setToolTipText("可顯示程式執行流程與目前下載進度");
        contentPane.add(stateBar, BorderLayout.SOUTH);
    }

    public static String getDefaultSkinClassName() {
        return defaultSkinClassName;
    }

    public static void setDefaultSkinClassName(String className) {
        defaultSkinClassName = className;
    }

    /**
     * 改成defaultSkinClassName名稱的版面
     * */
    private void setSkin() {
        setSkin(SetUp.getSkinClassName());
    }

    private void setSkin(String skinClassName) {
        Common.debugPrintln("設置" + skinClassName + "介面");
        try {
            CommonGUI.setLookAndFeelByClassName(skinClassName);
        } catch (Exception ex) {
            Common.errorReport("無法使用" + skinClassName + "介面 !!");
            CommonGUI.setLookAndFeelByClassName(defaultSkinClassName);
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void setButton(Container contentPane) {
        button = new JButton[7];
        String buttonPic;
        String buttonText;
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, button.length));
        button[ButtonEnum.ADD] = getButton("加入", "add.png");
        button[ButtonEnum.DOWNLOAD] = getButton("下載", "download.png");
        button[ButtonEnum.STOP] = getButton("停止", "stop.png");
        button[ButtonEnum.CLEAR] = getButton("清除", "clear.png");
        button[ButtonEnum.OPTION] = getButton("選項", "option.png");
        button[ButtonEnum.INFORMATION] = getButton("資訊", "information.png");
        button[ButtonEnum.EXIT] = getButton("離開", "exit.png");
        button[ButtonEnum.ADD].setToolTipText("解析網址列的網址，解析後可選擇欲下載集數並加入任務");
        button[ButtonEnum.DOWNLOAD].setToolTipText("若網址列有網址，則解析後加入任務並開始下載；若網址列沒有網址，則開始下載目前的任務清單");
        button[ButtonEnum.STOP].setToolTipText("停止下載，中斷進行中的任務");
        button[ButtonEnum.CLEAR].setToolTipText("清除目前的任務清單（若一次無法清空且按多次）");
        button[ButtonEnum.OPTION].setToolTipText("功能設定與調整（粗體字為預設功能）");
        button[ButtonEnum.INFORMATION].setToolTipText("相關提示與訊息");
        button[ButtonEnum.EXIT].setToolTipText("關閉本程式");
        for (int count = 0; count < button.length; count++) {
            button[count].setHorizontalTextPosition(SwingConstants.CENTER);
            button[count].setVerticalTextPosition(SwingConstants.BOTTOM);
            buttonPanel.add(button[count]);
            button[count].addActionListener(this);
        }
        contentPane.add(buttonPanel, BorderLayout.NORTH);
    }

    private void setText(Container contentPane) {
        urlField = new JTextField("請複製欲下載的漫畫頁面網址，此輸入欄會自動捕捉");
        urlField.setFont(SetUp.getDefaultFont(3));
        urlField.addMouseListener(this);
        urlField.setToolTipText("請輸入漫畫作品的主頁面或單集頁面網址");
        setUrlFieldJPopupMenu();
        Document doc = urlField.getDocument();
        doc.addDocumentListener(this);
        JPanel urlPanel = new CommonGUI().getCenterPanel(urlField);
        textPanel = new JPanel(new BorderLayout());
        textPanel.add(urlPanel, BorderLayout.NORTH);
        setTabbedPane(textPanel);
        contentPane.add(textPanel, BorderLayout.CENTER);
    }

    private void setDownloadTable(JPanel textPanel) {
        downTable = getDownloadTable();
        downTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane downScrollPane = new JScrollPane(downTable);
        setDownloadTableJPopupMenu();
        textPanel.add(downScrollPane, BorderLayout.CENTER);
    }

    private void setBookmarkTable(JPanel textPanel) {
        bookmarkTable = getBookmarkTable();
        bookmarkTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        bookmarkTable.setFillsViewportHeight(true);
        bookmarkTable.setAutoCreateRowSorter(true);
        JScrollPane bookmarkScrollPane = new JScrollPane(bookmarkTable);
        setBookmarkTableJPopupMenu();
        textPanel.add(bookmarkScrollPane, BorderLayout.CENTER);
    }

    private void setRecordTable(JPanel textPanel) {
        recordTable = getRecordTable();
        recordTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        recordTable.setFillsViewportHeight(true);
        recordTable.setAutoCreateRowSorter(true);
        JScrollPane recordScrollPane = new JScrollPane(recordTable);
        setRecordTableJPopupMenu();
        textPanel.add(recordScrollPane, BorderLayout.CENTER);
    }

    private void setTabbedPane(JPanel textPanel) {
        tabbedPane = new JTabbedPane();
        JPanel downTablePanel = new JPanel(new GridLayout(1, 1));
        setDownloadTable(downTablePanel);
        tabbedPane.addTab(" 下載任務  ", new CommonGUI().getImageIcon("tab_download.png"), downTablePanel, "所有欲下載的任務都會出現在此處，可依序下載");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
        JPanel bookmarkTablePanel = new JPanel(new GridLayout(1, 1));
        setBookmarkTable(bookmarkTablePanel);
        tabbedPane.addTab(" 我的書籤  ", new CommonGUI().getImageIcon("tab_bookmark.png"), bookmarkTablePanel, "希望持續追蹤的漫畫可加入到此處");
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
        JPanel recordTablePanel = new JPanel(new GridLayout(1, 1));
        setRecordTable(recordTablePanel);
        tabbedPane.addTab(" 任務記錄  ", new CommonGUI().getImageIcon("tab_record.png"), recordTablePanel, "所有曾經加入到下載任務的漫畫都會記錄在這邊，可由『選項』來選擇持續記錄或關閉後清空");
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
        textPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    protected JComponent makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }

    public static Vector<String> getDownloadColumns() {
        Vector<String> columnName = new Vector<String>();
        columnName.add("編號");
        columnName.add("是否下載");
        columnName.add("漫畫名稱");
        columnName.add("總共集數");
        columnName.add("勾選集數");
        columnName.add("目前狀態");
        columnName.add("網址解析");
        return columnName;
    }

    public static Vector<String> getBookmarkColumns() {
        Vector<String> columnName = new Vector<String>();
        columnName.add("編號");
        columnName.add("漫畫名稱");
        columnName.add("漫畫網址");
        columnName.add("加入日期");
        columnName.add("評論註解");
        return columnName;
    }

    public static Vector<String> getRecordColumns() {
        Vector<String> columnName = new Vector<String>();
        columnName.add("編號");
        columnName.add("漫畫名稱");
        columnName.add("漫畫網址");
        columnName.add("加入日期");
        return columnName;
    }

    private void setUrlFieldJPopupMenu() {
        pasteSystemClipboardItem = new JMenuItem("貼上網址");
        pasteSystemClipboardItem.addActionListener(this);
        urlFieldPopup = new JPopupMenu();
        urlFieldPopup.add(pasteSystemClipboardItem);
        urlField.add(urlFieldPopup);
    }

    private void setDownloadTableJPopupMenu() {
        tableSearchDownloadComic = new JMenuItem("搜尋這本漫畫");
        tableSearchDownloadComic.addActionListener(this);
        tableOpenDownloadURL = new JMenuItem("開啟網頁");
        tableOpenDownloadURL.addActionListener(this);
        tableOpenDownloadFile = new JMenuItem("開啟檔案");
        tableOpenDownloadFile.addActionListener(this);
        tableOpenDownloadDirectoryItem = new JMenuItem("開啟資料夾");
        tableOpenDownloadDirectoryItem.addActionListener(this);
        tableAddBookmarkFromDownloadItem = new JMenuItem("加入到書籤");
        tableAddBookmarkFromDownloadItem.addActionListener(this);
        tableRechoiceVolumeItem = new JMenuItem("重新選擇集數");
        tableRechoiceVolumeItem.addActionListener(this);
        tableDeleteMissionItem = new JMenuItem("刪除此任務");
        tableDeleteMissionItem.addActionListener(this);
        tableDeleteAllUnselectedMissionItem = new JMenuItem("刪除所有未勾選任務");
        tableDeleteAllUnselectedMissionItem.addActionListener(this);
        tableDeleteAllDoneMissionItem = new JMenuItem("刪除所有已完成任務");
        tableDeleteAllDoneMissionItem.addActionListener(this);
        tableMoveToRoofItem = new JMenuItem("將此列任務置頂");
        tableMoveToRoofItem.addActionListener(this);
        tableMoveToFloorItem = new JMenuItem("將此列任務置底");
        tableMoveToFloorItem.addActionListener(this);
        downloadTablePopup = new JPopupMenu();
        downloadTablePopup.add(tableAddBookmarkFromDownloadItem);
        downloadTablePopup.add(tableOpenDownloadDirectoryItem);
        downloadTablePopup.add(tableOpenDownloadFile);
        downloadTablePopup.add(tableOpenDownloadURL);
        downloadTablePopup.add(tableSearchDownloadComic);
        downloadTablePopup.add(tableRechoiceVolumeItem);
        downloadTablePopup.add(tableDeleteMissionItem);
        downloadTablePopup.add(tableDeleteAllUnselectedMissionItem);
        downloadTablePopup.add(tableDeleteAllDoneMissionItem);
        downloadTablePopup.add(tableMoveToRoofItem);
        downloadTablePopup.add(tableMoveToFloorItem);
        downTable.add(downloadTablePopup);
    }

    private void setBookmarkTableJPopupMenu() {
        tableSearchBookmarkComic = new JMenuItem("搜尋這本漫畫");
        tableSearchBookmarkComic.addActionListener(this);
        tableOpenBookmarkURL = new JMenuItem("開啟網頁");
        tableOpenBookmarkURL.addActionListener(this);
        tableOpenBookmarkDirectoryItem = new JMenuItem("開啟資料夾");
        tableOpenBookmarkDirectoryItem.addActionListener(this);
        tableOpenBookmarkFile = new JMenuItem("開啟檔案");
        tableOpenBookmarkFile.addActionListener(this);
        tableAddMissionFromBookmarkItem = new JMenuItem("加入到下載任務");
        tableAddMissionFromBookmarkItem.addActionListener(this);
        tableDeleteBookmarkItem = new JMenuItem("刪除此書籤");
        tableDeleteBookmarkItem.addActionListener(this);
        bookmarkTablePopup = new JPopupMenu();
        bookmarkTablePopup.add(tableAddMissionFromBookmarkItem);
        bookmarkTablePopup.add(tableOpenBookmarkDirectoryItem);
        bookmarkTablePopup.add(tableOpenBookmarkFile);
        bookmarkTablePopup.add(tableOpenBookmarkURL);
        bookmarkTablePopup.add(tableSearchBookmarkComic);
        bookmarkTablePopup.add(tableDeleteBookmarkItem);
        bookmarkTable.add(bookmarkTablePopup);
    }

    private void setRecordTableJPopupMenu() {
        tableSearchRecordComic = new JMenuItem("搜尋這本漫畫");
        tableSearchRecordComic.addActionListener(this);
        tableOpenRecordURL = new JMenuItem("開啟網頁");
        tableOpenRecordURL.addActionListener(this);
        tableOpenRecordFile = new JMenuItem("開啟檔案");
        tableOpenRecordFile.addActionListener(this);
        tableOpenRecordDirectoryItem = new JMenuItem("開啟資料夾");
        tableOpenRecordDirectoryItem.addActionListener(this);
        tableAddMissionFromRecordItem = new JMenuItem("加入到下載任務");
        tableAddMissionFromRecordItem.addActionListener(this);
        tableAddBookmarkFromRecordItem = new JMenuItem("加入到書籤");
        tableAddBookmarkFromRecordItem.addActionListener(this);
        tableDeleteRecordItem = new JMenuItem("刪除此記錄");
        tableDeleteRecordItem.addActionListener(this);
        recordTablePopup = new JPopupMenu();
        recordTablePopup.add(tableAddMissionFromRecordItem);
        recordTablePopup.add(tableAddBookmarkFromRecordItem);
        recordTablePopup.add(tableOpenRecordDirectoryItem);
        recordTablePopup.add(tableOpenRecordFile);
        recordTablePopup.add(tableOpenRecordURL);
        recordTablePopup.add(tableSearchRecordComic);
        recordTablePopup.add(tableDeleteRecordItem);
        recordTable.add(recordTablePopup);
    }

    private JTable getDownloadTable() {
        downTableModel = Common.inputDownTableFile();
        JTable table = new JTable(downTableModel) {

            protected String[] columnToolTips = { "此欄位滑鼠左鍵點兩下可刪除該列任務", "此欄位若沒有勾選就不會進行下載", "此欄位滑鼠左鍵點兩下可開啟該列任務的下載資料夾", "此欄位滑鼠左鍵點兩下可重新選取該列任務的下載集數", "此欄位滑鼠左鍵點兩下可重新選取該列任務的下載集數", "此欄位可顯示目前的下載進度，滑鼠左鍵點兩下以預設瀏覽程式開啟", null };

            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return columnToolTips[realIndex];
                    }
                };
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(400, 170));
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(new RowListener());
        table.addMouseListener(this);
        TableColumnModel cModel = table.getColumnModel();
        cModel.getColumn(DownTableEnum.ORDER).setPreferredWidth((int) (this.getWidth() * 0.07));
        cModel.getColumn(DownTableEnum.YES_OR_NO).setPreferredWidth((int) (this.getWidth() * 0.14));
        cModel.getColumn(DownTableEnum.TITLE).setPreferredWidth((int) (this.getWidth() * 0.6));
        cModel.getColumn(DownTableEnum.VOLUMES).setPreferredWidth((int) (this.getWidth() * 0.14));
        cModel.getColumn(DownTableEnum.CHECKS).setPreferredWidth((int) (this.getWidth() * 0.14));
        cModel.getColumn(DownTableEnum.STATE).setPreferredWidth((int) (this.getWidth() * 0.3));
        cModel.getColumn(DownTableEnum.URL).setPreferredWidth((int) (this.getWidth() * 0.002));
        return table;
    }

    private JTable getBookmarkTable() {
        bookmarkTableModel = Common.inputBookmarkTableFile();
        JTable table = new JTable(bookmarkTableModel) {

            protected String[] columnToolTips = { "此欄位滑鼠左鍵點兩下可刪除該列書籤", "此欄位滑鼠左鍵點兩下可開啟該列書籤的下載資料夾", "此欄位滑鼠左鍵點兩下可將該列書籤加入到下載任務清單中", "此欄位顯示該列漫畫加入書籤的系統時間，滑鼠左鍵點兩下以預設瀏覽程式開啟", "此欄位滑鼠左鍵點兩下可自由編輯該列任務的注解" };

            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return columnToolTips[realIndex];
                    }
                };
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(400, 170));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(new RowListener());
        table.addMouseListener(this);
        TableColumnModel cModel = table.getColumnModel();
        cModel.getColumn(BookmarkTableEnum.ORDER).setPreferredWidth((int) (this.getWidth() * 0.07));
        cModel.getColumn(BookmarkTableEnum.TITLE).setPreferredWidth((int) (this.getWidth() * 0.25));
        cModel.getColumn(BookmarkTableEnum.DATE).setPreferredWidth((int) (this.getWidth() * 0.25));
        cModel.getColumn(BookmarkTableEnum.URL).setPreferredWidth((int) (this.getWidth() * 0.38));
        cModel.getColumn(BookmarkTableEnum.COMMENT).setPreferredWidth((int) (this.getWidth() * 0.27));
        return table;
    }

    private JTable getRecordTable() {
        recordTableModel = Common.inputRecordTableFile();
        JTable table = new JTable(recordTableModel) {

            protected String[] columnToolTips = { "此欄位滑鼠左鍵點兩下可刪除該列記錄", "此欄位滑鼠左鍵點兩下可開啟該列記錄的下載資料夾", "此欄位滑鼠左鍵點兩下可將該列漫畫加入到下載任務清單中", "此欄位顯示該列漫畫在當初加入任務的系統時間，滑鼠左鍵點兩下以預設瀏覽程式開啟" };

            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return columnToolTips[realIndex];
                    }
                };
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(400, 170));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(new RowListener());
        table.addMouseListener(this);
        TableColumnModel cModel = table.getColumnModel();
        cModel.getColumn(RecordTableEnum.ORDER).setPreferredWidth((int) (this.getWidth() * 0.07));
        cModel.getColumn(RecordTableEnum.TITLE).setPreferredWidth((int) (this.getWidth() * 0.32));
        cModel.getColumn(RecordTableEnum.DATE).setPreferredWidth((int) (this.getWidth() * 0.25));
        cModel.getColumn(RecordTableEnum.URL).setPreferredWidth((int) (this.getWidth() * 0.43));
        return table;
    }

    private void showNotifyMessage(String message) {
    }

    private void setUpeListener() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Common.debugPrintln("JComicDownloader start ...");
        SetUp set = new SetUp();
        set.readSetFile();
        new ComicDownGUI();
    }

    public void windowGainedFocus(WindowEvent e) {
        SystemClipBoard clip = new SystemClipBoard();
        String clipString = clip.getClipString();
        if (!clipString.equals(Common.prevClipString)) {
            if (Common.isLegalURL(clipString)) {
                urlField.setText(clipString);
                Common.prevClipString = clipString;
                if (SetUp.getAutoAddMission()) {
                    String[] tempArgs = { clipString };
                    parseURL(tempArgs, false, false, 0);
                }
            }
        } else {
            urlField.setText("");
        }
    }

    public void windowLostFocus(WindowEvent e) {
    }

    private void minimizeEvent() {
        this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                if (Common.isWindows()) {
                    setState(Frame.ICONIFIED);
                } else {
                    setVisible(false);
                    minimizeToTray();
                }
            }

            public void windowIconified(WindowEvent e) {
                if (SystemTray.isSupported() && Common.isWindows()) {
                    setVisible(false);
                    minimizeToTray();
                } else {
                    setState(Frame.ICONIFIED);
                }
            }
        });
    }

    public void minimizeToTray() {
        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(this.trayIcon);
        } catch (AWTException ex) {
            System.err.println("無法加入系統工具列圖示");
            ex.printStackTrace();
        }
    }

    private void initTrayIcon() {
        Image image = new CommonGUI().getImage("main_icon.png");
        trayExitItem = new JMenuItem("離開此程式");
        trayExitItem.addActionListener(this);
        trayStartItem = new JMenuItem("開始任務");
        trayStartItem.addActionListener(this);
        trayStopItem = new JMenuItem("停止任務");
        trayStopItem.addActionListener(this);
        trayShowItem = new JMenuItem("開啟主介面");
        trayShowItem.addActionListener(this);
        trayPopup = new JPopupMenu();
        trayPopup.add(trayExitItem);
        trayPopup.add(trayStopItem);
        trayPopup.add(trayStartItem);
        trayPopup.add(trayShowItem);
        if (image != null) {
            trayIcon = new TrayIcon(image, "JComicDownloader", null);
            trayIcon.addMouseListener(this);
        } else {
            trayIcon = null;
        }
    }

    public void insertUpdate(DocumentEvent event) {
        Document doc = event.getDocument();
        try {
            args = doc.getText(0, doc.getLength()).split("\\s+");
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public void removeUpdate(DocumentEvent event) {
        Document doc = event.getDocument();
        try {
            args = doc.getText(0, doc.getLength()).split("\\s+");
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public void changedUpdate(DocumentEvent event) {
        Document doc = event.getDocument();
        try {
            args = doc.getText(0, doc.getLength()).split("\\s+");
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public void mousePressed(MouseEvent event) {
        if (event.getSource() == urlField) {
            urlField.setText("");
        }
        if (event.getSource() == trayIcon && event.getButton() == MouseEvent.BUTTON1) {
            setVisible(true);
            setState(Frame.NORMAL);
            SystemTray.getSystemTray().remove(trayIcon);
        }
        if (event.getSource() == urlField && event.getButton() == MouseEvent.BUTTON3) {
            showUrlFieldPopup(event);
            System.out.print(".");
        }
        if (event.getSource() == downTable && tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
            showDownloadPopup(event);
        } else if (event.getSource() == bookmarkTable && tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
            showBookmarkPopup(event);
        } else if (event.getSource() == recordTable && tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            showRecordPopup(event);
        }
    }

    public void mouseExited(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent event) {
    }

    public void mouseReleased(MouseEvent event) {
        if (event.getSource() == downTable && tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
            showDownloadPopup(event);
        } else if (event.getSource() == bookmarkTable && tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
            showBookmarkPopup(event);
        } else if (event.getSource() == recordTable && tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            showRecordPopup(event);
        }
        if (event.getSource() == urlField && event.getButton() == MouseEvent.BUTTON3) {
            showUrlFieldPopup(event);
        }
        if (event.getSource() == trayIcon && event.getButton() == MouseEvent.BUTTON3) {
            if (event.isPopupTrigger() && !trayPopup.isVisible()) {
                trayPopup.setLocation(event.getX() + 10, event.getY());
                trayPopup.setInvoker(trayPopup);
                trayPopup.setVisible(true);
            } else {
                trayPopup.setVisible(false);
            }
        }
    }

    public void mouseClicked(MouseEvent event) {
        if ((event.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && event.getClickCount() == 2 && tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
            int row = event.getY() / downTable.getRowHeight();
            int col = downTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            if (event.getSource() == downTable && row < Common.missionCount && row >= 0) {
                if (col == DownTableEnum.TITLE) {
                    openDownloadDirectory(row);
                } else if (col == DownTableEnum.STATE) {
                    openDownloadFile(row);
                }
                if (col == DownTableEnum.VOLUMES || col == DownTableEnum.CHECKS) {
                    if (!Flag.downloadingFlag) {
                        rechoiceVolume(row);
                    } else {
                        JOptionPane.showMessageDialog(this, "目前正下載中，無法重新選擇集數", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else if (col == DownTableEnum.ORDER) {
                    if (!Flag.downloadingFlag) {
                        deleteMission(row);
                    } else {
                        JOptionPane.showMessageDialog(this, "目前正下載中，無法刪除任務", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        }
        if ((event.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && event.getClickCount() == 2 && tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
            int row = event.getY() / bookmarkTable.getRowHeight();
            int col = bookmarkTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            if (event.getSource() == bookmarkTable && row < Common.bookmarkCount && row >= 0) {
                if (col == BookmarkTableEnum.TITLE) {
                    openDownloadDirectory(row);
                } else if (col == BookmarkTableEnum.DATE) {
                    openDownloadFile(row);
                }
                if (col == BookmarkTableEnum.URL) {
                    addMission(row);
                } else if (col == BookmarkTableEnum.ORDER) {
                    deleteBookmark(row);
                }
            }
        }
        if ((event.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && event.getClickCount() == 2 && tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            int row = event.getY() / recordTable.getRowHeight();
            int col = recordTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            if (event.getSource() == recordTable && row < Common.recordCount && row >= 0) {
                if (col == RecordTableEnum.TITLE) {
                    openDownloadDirectory(row);
                } else if (col == RecordTableEnum.DATE) {
                    openDownloadFile(row);
                }
                if (col == RecordTableEnum.URL) {
                    addMission(row);
                } else if (col == RecordTableEnum.ORDER) {
                    deleteRecord(row);
                }
            }
        }
    }

    private void addMission(int row) {
        String[] tempArgs = new String[1];
        if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
            row = bookmarkTable.convertRowIndexToModel(row);
            tempArgs[0] = bookmarkTableModel.getValueAt(row, BookmarkTableEnum.URL).toString();
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            row = recordTable.convertRowIndexToModel(row);
            tempArgs[0] = recordTableModel.getValueAt(row, RecordTableEnum.URL).toString();
        } else {
            Common.errorReport("不可能從書籤和記錄以外的地方加入任務！");
        }
        urlField.setText(tempArgs[0]);
        parseURL(tempArgs, false, false, 0);
    }

    private void rechoiceVolume(int row) {
        row = downTable.convertRowIndexToModel(row);
        System.out.println(downTableModel.getRealValueAt(row, DownTableEnum.CHECKS).toString());
        ComicDownGUI.nowSelectedCheckStrings = Common.getSeparateStrings(String.valueOf(downTableModel.getRealValueAt(row, DownTableEnum.CHECKS)));
        Common.debugPrintln("重新解析位址（為了重選集數）：" + downTableUrlStrings[row]);
        parseURL(new String[] { downTableUrlStrings[row] }, false, true, row);
    }

    private void stringsMoveOneForward(String[] strings, int beginIndex) {
        for (int i = beginIndex; strings[i + 1] != null; i++) {
            strings[i] = strings[i + 1];
        }
    }

    private void moveMissionToRoof(int row) {
        row = downTable.convertRowIndexToModel(row);
        Common.debugPrint("指定要置頂的列：" + row + "\t");
        Common.debugPrint("目前正在下載的列：" + nowDownloadMissionRow + "\t");
        if (row == nowDownloadMissionRow) {
            JOptionPane.showMessageDialog(this, "目前正下載中，無法移動任務的順序位置", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int roof;
        if (row < nowDownloadMissionRow || !Flag.downloadingFlag) {
            roof = 0;
        } else {
            roof = nowDownloadMissionRow + 1;
        }
        Common.debugPrintln("允許置換後的列：" + roof);
        String urlString = downTableUrlStrings[row];
        for (int i = row - 1; i >= roof; i--) {
            downTableUrlStrings[i + 1] = downTableUrlStrings[i];
            i = downTable.convertRowIndexToModel(i);
        }
        downTableUrlStrings[roof] = urlString;
        downTableModel.moveRow(row, row, roof);
    }

    private void moveMissionToFloor(int row) {
        row = downTable.convertRowIndexToModel(row);
        Common.debugPrint("指定要置底的列：" + row + "\t");
        Common.debugPrint("目前正在下載的列：" + nowDownloadMissionRow + "\t");
        if (row == nowDownloadMissionRow) {
            JOptionPane.showMessageDialog(this, "目前正下載中，無法移動任務的順序位置", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int missionAmount = downTableModel.getRowCount();
        int floor;
        if (row > nowDownloadMissionRow || !Flag.downloadingFlag) {
            floor = missionAmount - 1;
        } else {
            floor = nowDownloadMissionRow - 1;
        }
        Common.debugPrintln("允許置換後的列：" + floor);
        String urlString = downTableUrlStrings[row];
        for (int i = row + 1; i <= floor; i++) {
            i = downTable.convertRowIndexToModel(i);
            downTableUrlStrings[i - 1] = downTableUrlStrings[i];
        }
        downTableUrlStrings[floor] = urlString;
        downTableModel.moveRow(row, row, floor);
    }

    private void deleteMission(int row) {
        row = downTable.convertRowIndexToModel(row);
        String title = String.valueOf(downTableModel.getRealValueAt(row, DownTableEnum.TITLE));
        String message = "是否要在任務清單中刪除" + title + " ?";
        int choice = JOptionPane.showConfirmDialog(this, message, "提醒訊息", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            Common.missionCount--;
            downTableModel.removeRow(row);
            stringsMoveOneForward(downTableUrlStrings, row);
        }
    }

    private void deleteAllUnselectedMission() {
        String message = "是否要在任務清單中刪除所有未勾選的任務 ?";
        int choice = JOptionPane.showConfirmDialog(this, message, "詢問視窗", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            int nowRow = 0;
            while (nowRow < downTableModel.getRowCount()) {
                if (downTableModel.getValueAt(nowRow, DownTableEnum.YES_OR_NO).toString().equals("false")) {
                    downTableModel.removeRow(nowRow);
                    stringsMoveOneForward(downTableUrlStrings, nowRow);
                    Common.missionCount--;
                } else {
                    nowRow++;
                }
            }
            repaint();
        }
    }

    private void deleteAllDoneMission() {
        String message = "是否要在任務清單中刪除所有已經完成的任務 ?";
        int choice = JOptionPane.showConfirmDialog(this, message, "提醒訊息", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            int nowRow = 0;
            while (nowRow < downTableModel.getRowCount()) {
                if (downTableModel.getValueAt(nowRow, DownTableEnum.STATE).toString().equals("下載完畢")) {
                    downTableModel.removeRow(nowRow);
                    stringsMoveOneForward(downTableUrlStrings, nowRow);
                    Common.missionCount--;
                } else {
                    nowRow++;
                }
            }
            repaint();
        }
    }

    private void deleteBookmark(int row) {
        row = bookmarkTable.convertRowIndexToModel(row);
        String title = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.TITLE));
        String message = "是否要在書籤中刪除" + title + " ?";
        int choice = JOptionPane.showConfirmDialog(this, message, "提醒訊息", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            Common.bookmarkCount--;
            bookmarkTableModel.removeRow(row);
        }
    }

    private void deleteRecord(int row) {
        row = recordTable.convertRowIndexToModel(row);
        String title = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.TITLE));
        String message = "是否要在記錄中刪除" + title + " ?";
        int choice = JOptionPane.showConfirmDialog(this, message, "提醒訊息", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            Common.recordCount--;
            recordTableModel.removeRow(row);
        }
    }

    private void addBookmark(int row) {
        row = downTable.convertRowIndexToModel(row);
        String title = "";
        String url = "";
        if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
            title = String.valueOf(downTableModel.getRealValueAt(row, DownTableEnum.TITLE));
            url = downTableUrlStrings[row];
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            title = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.TITLE));
            url = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.URL));
        }
        Common.debugPrintln("加入到書籤：" + title + " " + url);
        bookmarkTableModel.addRow(CommonGUI.getBookmarkDataRow(++Common.bookmarkCount, title, url));
        Common.outputBookmarkTableFile(bookmarkTableModel);
    }

    private void searchDownloadComic(int row) {
        String title = "";
        String url = "";
        if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
            row = downTable.convertRowIndexToModel(row);
            title = String.valueOf(downTableModel.getRealValueAt(row, DownTableEnum.TITLE));
            url = downTableUrlStrings[row];
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
            row = bookmarkTable.convertRowIndexToModel(row);
            title = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.TITLE));
            url = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.URL));
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            row = recordTable.convertRowIndexToModel(row);
            title = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.TITLE));
            url = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.URL));
        }
        Common.debugPrintln("瀏覽器開啟自訂搜尋引擎，並以『" + title + "』作為關鍵字進行搜尋");
        new RunBrowser().runBroswer(getKeywordSearchURL(title));
    }

    private void openDownloadURL(int row) {
        String title = "";
        String url = "";
        if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
            row = downTable.convertRowIndexToModel(row);
            title = String.valueOf(downTableModel.getRealValueAt(row, DownTableEnum.TITLE));
            url = downTableUrlStrings[row];
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
            row = bookmarkTable.convertRowIndexToModel(row);
            title = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.TITLE));
            url = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.URL));
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            row = recordTable.convertRowIndexToModel(row);
            title = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.TITLE));
            url = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.URL));
        }
        Common.debugPrintln("以預設瀏覽器開啟" + title + "的原始網頁");
        new RunBrowser().runBroswer(url);
    }

    private void openDownloadFile(int row) {
        String title = "";
        String url = "";
        if (SetUp.getOpenPicFileProgram().matches("")) {
            String nowSkinName = UIManager.getLookAndFeel().getName();
            String colorString = "blue";
            if (nowSkinName.equals("HiFi") || nowSkinName.equals("Noire")) {
                colorString = "yellow";
            }
            JOptionPane.showMessageDialog(this, "<html>尚未設定開啟程式，請前往<font color=" + colorString + ">選項 -> 瀏覽</font>做設定</html>", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
            row = downTable.convertRowIndexToModel(row);
            title = String.valueOf(downTableModel.getRealValueAt(row, DownTableEnum.TITLE));
            url = downTableModel.getRealValueAt(row, DownTableEnum.URL).toString();
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
            row = bookmarkTable.convertRowIndexToModel(row);
            title = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.TITLE));
            url = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.URL));
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            row = recordTable.convertRowIndexToModel(row);
            title = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.TITLE));
            url = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.URL));
        }
        Common.debugPrintln("以外部程式開啟" + title + "的下載資料夾或壓縮檔");
        if (url.matches("(?s).*e-hentai(?s).*") || url.matches("(?s).*exhentai(?s).*")) {
            String cmd = SetUp.getOpenZipFileProgram();
            String path = "";
            if (new File(SetUp.getOriginalDownloadDirectory() + title + ".zip").exists()) {
                path = SetUp.getOriginalDownloadDirectory() + title + ".zip";
                Common.debugPrintln("開啟命令：" + cmd + " " + path);
                if (Common.isWindows()) {
                    Common.runUnansiCmd(cmd, path);
                } else {
                    try {
                        String[] cmds = new String[] { cmd, path };
                        Runtime.getRuntime().exec(cmds, null, new File(Common.getNowAbsolutePath()));
                    } catch (IOException ex) {
                        Logger.getLogger(ComicDownGUI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                path = SetUp.getOriginalDownloadDirectory() + title + Common.getSlash();
                if (!new File(path).exists()) {
                    String nowSkinName = UIManager.getLookAndFeel().getName();
                    String colorString = "blue";
                    if (nowSkinName.equals("HiFi") || nowSkinName.equals("Noire")) {
                        colorString = "yellow";
                    }
                    JOptionPane.showMessageDialog(this, "<html><font color=" + colorString + ">" + path + "</font>" + "不存在，無法開啟</html>", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (Common.isWindows()) {
                    Common.debugPrintln("開啟命令：" + cmd + " " + path);
                    Common.runUnansiCmd(cmd, path);
                } else {
                    String[] picList = new File(path).list();
                    String firstPicFileInFirstVolume = picList[0];
                    path += firstPicFileInFirstVolume;
                    Common.debugPrintln("開啟命令：" + cmd + " " + path);
                    try {
                        String[] cmds = new String[] { cmd, path };
                        Runtime.getRuntime().exec(cmds, null, new File(Common.getNowAbsolutePath()));
                    } catch (IOException ex) {
                        Logger.getLogger(ComicDownGUI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            if (Common.isWindows()) {
                Common.runUnansiCmd(SetUp.getOpenPicFileProgram(), SetUp.getOriginalDownloadDirectory() + title + Common.getSlash());
            } else {
                Common.runCmd(SetUp.getOpenPicFileProgram(), SetUp.getOriginalDownloadDirectory() + title);
            }
        }
    }

    private void openDownloadDirectory(int row) {
        String title = "";
        String url = "";
        if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
            row = downTable.convertRowIndexToModel(row);
            title = String.valueOf(downTableModel.getRealValueAt(row, DownTableEnum.TITLE));
            url = downTableModel.getRealValueAt(row, DownTableEnum.URL).toString();
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
            row = bookmarkTable.convertRowIndexToModel(row);
            title = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.TITLE));
            url = String.valueOf(bookmarkTableModel.getValueAt(row, BookmarkTableEnum.URL));
        } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
            row = recordTable.convertRowIndexToModel(row);
            title = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.TITLE));
            url = String.valueOf(recordTableModel.getValueAt(row, RecordTableEnum.URL));
        }
        Common.debugPrintln("開啟" + title + "的下載資料夾");
        if (url.matches("(?s).*e-hentai(?s).*") || url.matches("(?s).*exhentai(?s).*")) {
            if (Common.isWindows()) {
                if (new File(SetUp.getOriginalDownloadDirectory() + title + ".zip").exists()) {
                    Common.runUnansiCmd("explorer /select, ", SetUp.getOriginalDownloadDirectory() + title + ".zip");
                } else if (new File(SetUp.getOriginalDownloadDirectory() + title + Common.getSlash()).exists()) {
                    Common.runUnansiCmd("explorer /select, ", SetUp.getOriginalDownloadDirectory() + title);
                } else {
                    Common.runUnansiCmd("explorer ", SetUp.getOriginalDownloadDirectory());
                }
            } else if (Common.isMac()) {
                Common.runCmd("Finder ", SetUp.getOriginalDownloadDirectory());
            } else {
                Common.runCmd("nautilus ", SetUp.getOriginalDownloadDirectory());
            }
        } else {
            if (Common.isWindows()) {
                Common.runUnansiCmd("explorer ", SetUp.getOriginalDownloadDirectory() + title + Common.getSlash());
            } else if (Common.isMac()) {
                Common.runCmd("Finder ", SetUp.getOriginalDownloadDirectory() + title + Common.getSlash());
            } else {
                Common.runCmd("nautilus ", SetUp.getOriginalDownloadDirectory() + title + Common.getSlash());
            }
        }
    }

    private void showDownloadPopup(MouseEvent event) {
        downloadTablePopupRow = event.getY() / downTable.getRowHeight();
        if (downloadTablePopupRow < Common.missionCount && downloadTablePopupRow >= 0) {
            if (event.isPopupTrigger()) {
                downloadTablePopup.show(event.getComponent(), event.getX(), event.getY());
            }
        }
    }

    private void showBookmarkPopup(MouseEvent event) {
        bookmarkTablePopupRow = event.getY() / bookmarkTable.getRowHeight();
        if (bookmarkTablePopupRow < Common.bookmarkCount && bookmarkTablePopupRow >= 0) {
            if (event.isPopupTrigger()) {
                bookmarkTablePopup.show(event.getComponent(), event.getX(), event.getY());
            }
        }
    }

    private void showRecordPopup(MouseEvent event) {
        recordTablePopupRow = event.getY() / recordTable.getRowHeight();
        if (recordTablePopupRow < Common.recordCount && recordTablePopupRow >= 0) {
            if (event.isPopupTrigger()) {
                recordTablePopup.show(event.getComponent(), event.getX(), event.getY());
            }
        }
    }

    private void showUrlFieldPopup(MouseEvent event) {
        System.out.print("|");
        System.out.println(event.getX() + "," + event.getY());
        urlFieldPopup.show(event.getComponent(), event.getX() + 15, event.getY());
    }

    public void startDownloadList(final boolean downloadAfterChoice) {
        Thread downThread = new Thread(new Runnable() {

            public void run() {
                Common.debugPrintln("進入下載主函式中");
                if (downloadAfterChoice) {
                    Common.downloadLock = true;
                    Common.debugPrintln("進入選擇集數，等待中...");
                    synchronized (ComicDownGUI.mainFrame) {
                        while (Common.downloadLock) {
                            try {
                                ComicDownGUI.mainFrame.wait();
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                    Common.debugPrintln("選擇集數完畢，結束等待");
                }
                for (int i = 0; i < Common.missionCount && Run.isAlive; i++) {
                    if (downTableModel.getValueAt(i, DownTableEnum.YES_OR_NO).toString().equals("false") || downTableModel.getValueAt(i, DownTableEnum.STATE).toString().equals("下載完畢")) {
                        Common.processPrintln("跳過 " + downTableModel.getValueAt(i, DownTableEnum.TITLE).toString());
                        continue;
                    }
                    nowDownloadMissionRow = i;
                    String[] urlStrings = Common.getSeparateStrings(downTableModel.getValueAt(i, DownTableEnum.URL).toString());
                    String[] checkStrings = Common.getSeparateStrings(String.valueOf(downTableModel.getRealValueAt(i, DownTableEnum.CHECKS)));
                    String[] volumeStrings = Common.getSeparateStrings(String.valueOf(downTableModel.getRealValueAt(i, DownTableEnum.VOLUMES)));
                    int downloadCount = 0;
                    for (int j = 0; j < urlStrings.length && Run.isAlive; j++) {
                        if (checkStrings[j].equals("true")) {
                            Flag.allowDownloadFlag = true;
                            String nowState = "下載進度：" + downloadCount + " / " + Common.getTrueCountFromStrings(checkStrings);
                            downTableModel.setValueAt(nowState, i, DownTableEnum.STATE);
                            Run mainRun = new Run(urlStrings[j], volumeStrings[j], downTableModel.getValueAt(i, DownTableEnum.TITLE).toString(), RunModeEnum.DOWNLOAD_MODE);
                            mainRun.run();
                            downloadCount++;
                        }
                    }
                    if (Run.isAlive) {
                        if (Flag.downloadErrorFlag) {
                            downTableModel.setValueAt("下載錯誤", i, DownTableEnum.STATE);
                            Flag.downloadErrorFlag = false;
                        } else {
                            downTableModel.setValueAt("下載完畢", i, DownTableEnum.STATE);
                        }
                        String title = String.valueOf(downTableModel.getRealValueAt(i, DownTableEnum.TITLE));
                        if (SetUp.getShowDoneMessageAtSystemTray()) {
                            trayIcon.displayMessage("JComicDownloader Message", title + "下載完畢! ", TrayIcon.MessageType.INFO);
                            if (SetUp.getPlaySingleDoneAudio()) {
                                Common.playSingleDoneAudio();
                            }
                        }
                    } else {
                        downTableModel.setValueAt("下載中斷", i, DownTableEnum.STATE);
                        trayIcon.setToolTip("下載中斷");
                    }
                    Common.outputDownTableFile(downTableModel);
                }
                if (Run.isAlive) {
                    stateBar.setText(Common.missionCount + "個任務全部下載完畢! ");
                    if (SetUp.getPlayAllDoneAudio()) {
                        Common.playAllDoneAudio();
                    }
                    if (SetUp.getShowDoneMessageAtSystemTray()) {
                        trayIcon.displayMessage("JComicDownloader Message", Common.missionCount + "個任務全部下載完畢! ", TrayIcon.MessageType.INFO);
                    }
                    trayIcon.setToolTip("JComicDownloader");
                    Flag.allowDownloadFlag = false;
                    System.gc();
                }
            }
        });
        Common.downloadThread = downThread;
        downThread.start();
    }

    public void startDownloadURL(final String[] newArgs) {
        Thread downThread = new Thread(new Runnable() {

            public void run() {
                Flag.allowDownloadFlag = true;
                Common.processPrintln("開始單集下載");
                stateBar.setText("  開始單集下載");
                Run singleRun = new Run(newArgs, RunModeEnum.DOWNLOAD_MODE);
                singleRun.start();
                try {
                    singleRun.join();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                Flag.allowDownloadFlag = false;
            }
        });
        Common.downloadThread = downThread;
        downThread.start();
    }

    public void runChoiceFrame(boolean modifySelected, int modifyRow, String title, String urlString) {
        ChoiceFrame choiceFrame;
        if (modifySelected) {
            choiceFrame = new ChoiceFrame("重新選擇欲下載的集數 [" + title + "]", true, modifyRow, title, urlString);
        } else {
            choiceFrame = new ChoiceFrame(title, urlString);
        }
        String[] volumeStrings = choiceFrame.getVolumeStrings();
        String[] checkStrings = choiceFrame.getCheckStrings();
        args = null;
        urlField.setText("");
        repaint();
    }

    private void clearMission() {
        int downListCount = downTableModel.getRowCount();
        while (downTableModel.getRowCount() > 1) {
            downTableModel.removeRow(downTableModel.getRowCount() - 1);
            Common.missionCount--;
        }
        if (Common.missionCount > 0) {
            downTableModel.removeRow(0);
        }
        repaint();
        Common.missionCount = 0;
        Common.processPrint("全部下載任務清空");
        stateBar.setText("全部下載任務清空");
        trayIcon.setToolTip("JComicDownloader");
    }

    private void clearBookmark() {
        int bookmarkListCount = bookmarkTableModel.getRowCount();
        while (bookmarkTableModel.getRowCount() > 1) {
            bookmarkTableModel.removeRow(bookmarkTableModel.getRowCount() - 1);
            Common.bookmarkCount--;
        }
        if (Common.bookmarkCount > 0) {
            bookmarkTableModel.removeRow(0);
        }
        repaint();
        Common.bookmarkCount = 0;
        Common.processPrint("全部書籤清空");
        stateBar.setText("全部書籤清空");
        trayIcon.setToolTip("JComicDownloader");
    }

    private void clearRecord() {
        int recordListCount = recordTableModel.getRowCount();
        while (recordTableModel.getRowCount() > 1) {
            recordTableModel.removeRow(recordTableModel.getRowCount() - 1);
            Common.recordCount--;
        }
        if (Common.recordCount > 0) {
            recordTableModel.removeRow(0);
        }
        repaint();
        Common.recordCount = 0;
        Common.processPrint("全部記錄清空");
        stateBar.setText("全部記錄清空");
        trayIcon.setToolTip("JComicDownloader");
    }

    public void parseURL(final String[] newArgs, final boolean allowDownload, final boolean modifySelected, final int modifyRow) {
        Thread praseThread = new Thread(new Runnable() {

            public void run() {
                Common.urlIsUnknown = false;
                if (newArgs == null || newArgs[0].equals("")) {
                    if (!allowDownload) {
                        stateBar.setText("  沒有輸入網址 !!");
                    } else {
                        if (Common.missionCount > 0) {
                            Flag.parseUrlFlag = false;
                            startDownloadList(false);
                        } else {
                            stateBar.setText("  沒有下載任務也沒有輸入網址 !!");
                        }
                    }
                } else if (!Common.isLegalURL(newArgs[0])) {
                    stateBar.setText("  網址錯誤，請輸入正確的網址 !!");
                } else {
                    stateBar.setText("  解析網址中");
                    if (Common.withGUI()) {
                        trayIcon.setToolTip("解析網址中");
                    }
                    Flag.allowDownloadFlag = false;
                    Run.isAlive = true;
                    String[] tempArgs = Common.getCopiedStrings(newArgs);
                    mainRun = new Run(tempArgs, RunModeEnum.PARSE_MODE);
                    mainRun.start();
                    String title = "";
                    try {
                        mainRun.join();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    title = mainRun.getTitle();
                    Common.debugPrintln("選擇集數前解析得到的title：" + title);
                    if (Common.urlIsUnknown) {
                        stateBar.setText("  無法解析此網址 !!");
                    } else if (Common.isMainPage) {
                        runChoiceFrame(modifySelected, modifyRow, title, tempArgs[0]);
                        if (allowDownload) {
                            Flag.parseUrlFlag = false;
                            startDownloadList(true);
                        }
                    } else {
                        if (!allowDownload) {
                            stateBar.setText("  單集頁面無法加入下載佇列 !!");
                        } else {
                            stateBar.setText("  正在下載單一集數");
                            Flag.parseUrlFlag = false;
                            startDownloadURL(tempArgs);
                        }
                    }
                }
                Flag.parseUrlFlag = false;
            }
        });
        praseThread.start();
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == urlField) {
        }
        if (event.getSource() == tableSearchDownloadComic) {
            searchDownloadComic(downloadTablePopupRow);
        } else if (event.getSource() == tableSearchBookmarkComic) {
            searchDownloadComic(bookmarkTablePopupRow);
        } else if (event.getSource() == tableSearchRecordComic) {
            searchDownloadComic(recordTablePopupRow);
        }
        if (event.getSource() == tableOpenDownloadFile) {
            openDownloadFile(downloadTablePopupRow);
        } else if (event.getSource() == tableOpenBookmarkFile) {
            openDownloadFile(bookmarkTablePopupRow);
        } else if (event.getSource() == tableOpenRecordFile) {
            openDownloadFile(recordTablePopupRow);
        }
        if (event.getSource() == tableOpenDownloadURL) {
            openDownloadURL(downloadTablePopupRow);
        } else if (event.getSource() == tableOpenBookmarkURL) {
            openDownloadURL(bookmarkTablePopupRow);
        } else if (event.getSource() == tableOpenRecordURL) {
            openDownloadURL(recordTablePopupRow);
        } else if (event.getSource() == tableOpenDownloadDirectoryItem) {
            openDownloadDirectory(downloadTablePopupRow);
        } else if (event.getSource() == tableOpenBookmarkDirectoryItem) {
            openDownloadDirectory(bookmarkTablePopupRow);
        } else if (event.getSource() == tableOpenRecordDirectoryItem) {
            openDownloadDirectory(recordTablePopupRow);
        }
        if (event.getSource() == pasteSystemClipboardItem) {
            String clipString = new SystemClipBoard().getClipString();
            urlField.setText(clipString);
        }
        if (event.getSource() == tableAddBookmarkFromDownloadItem) {
            addBookmark(downloadTablePopupRow);
        } else if (event.getSource() == tableAddBookmarkFromRecordItem) {
            addBookmark(recordTablePopupRow);
        }
        if (event.getSource() == tableAddMissionFromBookmarkItem) {
            addMission(bookmarkTablePopupRow);
        } else if (event.getSource() == tableAddMissionFromRecordItem) {
            addMission(recordTablePopupRow);
        }
        if (event.getSource() == tableRechoiceVolumeItem) {
            if (!Flag.downloadingFlag) {
                rechoiceVolume(downloadTablePopupRow);
            } else {
                JOptionPane.showMessageDialog(this, "目前正下載中，無法重新選擇集數", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        if (event.getSource() == tableDeleteMissionItem) {
            if (!Flag.downloadingFlag) {
                deleteMission(downloadTablePopupRow);
            } else {
                JOptionPane.showMessageDialog(this, "目前正下載中，無法刪除任務", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        if (event.getSource() == tableDeleteAllUnselectedMissionItem) {
            if (!Flag.downloadingFlag) {
                deleteAllUnselectedMission();
            } else {
                JOptionPane.showMessageDialog(this, "目前正下載中，無法刪除任務", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        if (event.getSource() == tableDeleteAllDoneMissionItem) {
            if (!Flag.downloadingFlag) {
                deleteAllDoneMission();
            } else {
                JOptionPane.showMessageDialog(this, "目前正下載中，無法刪除任務", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        if (event.getSource() == tableMoveToRoofItem) {
            moveMissionToRoof(downloadTablePopupRow);
        }
        if (event.getSource() == tableMoveToFloorItem) {
            moveMissionToFloor(downloadTablePopupRow);
        }
        if (event.getSource() == tableDeleteBookmarkItem) {
            deleteBookmark(bookmarkTablePopupRow);
        }
        if (event.getSource() == tableDeleteRecordItem) {
            deleteRecord(recordTablePopupRow);
        }
        if (event.getSource() == trayShowItem) {
            setVisible(true);
            setState(Frame.NORMAL);
            SystemTray.getSystemTray().remove(trayIcon);
        }
        if (event.getSource() == button[ButtonEnum.ADD]) {
            logFrame.redirectSystemStreams();
            testDownload();
            String urlString = urlField.getText();
            parseURL(args, false, false, 0);
            args = null;
        }
        if (event.getSource() == button[ButtonEnum.DOWNLOAD] || event.getSource() == trayStartItem) {
            if (Flag.downloadingFlag || Flag.parseUrlFlag) {
                JOptionPane.showMessageDialog(this, "目前正下載中，不提供直接下載，請按「加入」來加入下載任務。", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            } else {
                logFrame.redirectSystemStreams();
                Run.isAlive = true;
                stateBar.setText("開始下載中...");
                tabbedPane.setSelectedIndex(TabbedPaneEnum.MISSION);
                Flag.parseUrlFlag = true;
                parseURL(args, true, false, 0);
                args = null;
            }
        }
        if (event.getSource() == button[ButtonEnum.STOP] || event.getSource() == trayStopItem) {
            Run.isAlive = false;
            Flag.allowDownloadFlag = Flag.downloadingFlag = Flag.parseUrlFlag = false;
            stateBar.setText("所有下載任務停止");
            trayIcon.setToolTip("JComicDownloader");
        }
        if (event.getSource() == button[ButtonEnum.OPTION]) {
            new Thread(new Runnable() {

                public void run() {
                    new OptionFrame();
                }
            }).start();
        }
        if (event.getSource() == button[ButtonEnum.INFORMATION]) {
            new Thread(new Runnable() {

                public void run() {
                    final InformationFrame frame = new InformationFrame();
                    new Thread(new Runnable() {

                        public void run() {
                            frame.setNewestVersion();
                        }
                    }).start();
                }
            }).start();
        }
        if (event.getSource() == button[ButtonEnum.CLEAR]) {
            int choice = JOptionPane.showConfirmDialog(this, "請問是否要將目前內容全部清空？", "提醒訊息", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.MISSION) {
                    clearMission();
                } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.BOOKMARK) {
                    clearBookmark();
                } else if (tabbedPane.getSelectedIndex() == TabbedPaneEnum.RECORD) {
                    clearRecord();
                }
            }
        }
        if (event.getSource() == button[ButtonEnum.EXIT] || event.getSource() == trayExitItem) {
            int choice = JOptionPane.showConfirmDialog(this, "請問是否要關閉JComicDownloader？", "提醒訊息", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                exit();
            }
        }
    }

    public static void exit() {
        SetUp.writeSetFile();
        Common.outputDownTableFile(downTableModel);
        Common.outputBookmarkTableFile(bookmarkTableModel);
        Common.outputRecordTableFile(recordTableModel);
        Run.isAlive = false;
        Common.debugPrintln("刪除所有暫存檔案");
        Common.deleteFolder(SetUp.getTempDirectory());
        Common.debugPrintln("Exit JComicDownloader ... ");
        System.exit(0);
    }

    private void whlie(boolean b) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class RowListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent event) {
        }
    }

    private String getKeywordSearchURL(String keyword) {
        String url = "http://www.google.com/cse?cx=002948535609514911011%3Als5mhwb6sqa&ie=UTF-8&q=" + keyword + "&sa=%E6%90%9C%E5%B0%8B&hl=zh-TW&siteurl=www.google.com%2Fcse%2Fhome%3Fcx%3D002948535609514911011%3Als5mhwb6sqa%26hl%3Dzh-TW#gsc.tab=0&gsc.q=" + keyword + "&gsc.page=1";
        return Common.getFixedChineseURL(url);
    }

    private void counter() {
        new Thread(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ComicDownGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
                String counterURL = "http://jcomicdownloader.googlecode.com/files/count.txt";
                Common.urlIsOK(counterURL);
            }
        }).start();
    }

    public void testDownload() {
        Thread downThread = new Thread(new Runnable() {

            public void run() {
                String picURL = "http://up1.emland.net/img.php?url=photo/27464/27465/46781292144946.jpg";
                String pageURL = "http://www.kangdm.com/comic/10256/";
                String testURL = "http://comic.veryim.com/manhua/zuishangys/";
                String cookie = null;
                System.out.println(cookie);
                Common.downloadFile(picURL, "", "test.jpg", false, cookie);
                System.out.println("OVER");
            }
        });
    }

    private JButton getButton(String string, String picName) {
        JButton button = new JButton(string, new CommonGUI().getImageIcon(picName));
        button.setFont(SetUp.getDefaultFont(5));
        return button;
    }
}
