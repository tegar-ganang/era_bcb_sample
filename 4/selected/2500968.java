package org.jpedal.examples.simpleviewer.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.Box.Filler;
import javax.swing.border.EtchedBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jpedal.Display;
import org.jpedal.PdfDecoder;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.TextObject;
import org.jpedal.examples.simpleviewer.Commands;
import org.jpedal.examples.simpleviewer.Values;
import org.jpedal.examples.simpleviewer.gui.generic.GUIButton;
import org.jpedal.examples.simpleviewer.gui.generic.GUICombo;
import org.jpedal.examples.simpleviewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.examples.simpleviewer.gui.swing.CommandListener;
import org.jpedal.examples.simpleviewer.gui.swing.FrameCloser;
import org.jpedal.examples.simpleviewer.gui.swing.PageViewChanger;
import org.jpedal.examples.simpleviewer.gui.swing.SwingButton;
import org.jpedal.examples.simpleviewer.gui.swing.SwingCombo;
import org.jpedal.examples.simpleviewer.gui.swing.SwingMenuItem;
import org.jpedal.examples.simpleviewer.gui.swing.SwingOutline;
import org.jpedal.examples.simpleviewer.utils.Printer;
import org.jpedal.examples.simpleviewer.utils.PropertiesFile;
import org.jpedal.exception.PdfException;
import org.jpedal.gui.GUIFactory;
import org.jpedal.io.StatusBar;
import org.jpedal.objects.PdfFileInformation;
import org.jpedal.objects.PdfPageData;
import org.jpedal.utils.BrowserLauncher;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;
import org.jpedal.utils.SwingWorker;
import org.jpedal.utils.repositories.Vector_Int;
import org.w3c.dom.Node;

/**
 * <br>Description: Swing GUI functions in Viewer
 *
 *
 */
public class SwingGUI extends GUI implements GUIFactory {

    boolean finishedDecoding = false;

    final int startSize = 30, expandedSize = 190;

    String pageTitle, bookmarksTitle;

    boolean hasListener = false;

    private boolean isSetup = false;

    int lastTabSelected = -1;

    public boolean messageShown = false;

    ButtonGroup layoutGroup = new ButtonGroup();

    /**listener on buttons, menus, combboxes to execute options (one instance on all objects)*/
    private CommandListener currentCommandListener;

    /**holds OPEN, INFO,etc*/
    private JToolBar topButtons = new JToolBar();

    /**holds rotation, quality, scaling and status*/
    private JToolBar comboBar = new JToolBar();

    /**holds back/forward buttons at bottom of page*/
    private JToolBar bottomNavButtons = new JToolBar();

    /**holds all menu entries (File, View, Help)*/
    private JMenuBar currentMenu = new JMenuBar();

    /**tell user on first form change it can be saved*/
    private boolean firstTimeFormMessage = true;

    /** visual display of current cursor co-ords on page*/
    private JLabel coords = new JLabel();

    /**root element to hold display*/
    private JFrame frame = new JFrame();

    /**displayed on left to hold thumbnails, bookmarks*/
    private JTabbedPane navOptionsPanel = new JTabbedPane();

    /**split display between PDF and thumbnails/bookmarks*/
    private JSplitPane displayPane;

    /**Scrollpane for pdf panel*/
    private JScrollPane scrollPane = new JScrollPane();

    private final Font headFont = new Font("SansSerif", Font.BOLD, 14);

    private final Font textFont = new Font("Serif", Font.PLAIN, 12);

    /**Interactive display object - needs to be added to PdfDecoder*/
    private StatusBar statusBar = new StatusBar(Color.orange);

    public JTextField pageCounter2 = new JTextField(4);

    private JLabel pageCounter3;

    private JLabel optimizationLabel;

    /**user dir in which program can write*/
    private String user_dir = System.getProperty("user.dir");

    /**stop user forcing open tab before any pages loaded*/
    private boolean tabsNotInitialised = true;

    private JToolBar navToolBar = new JToolBar();

    private JToolBar pagesToolBar = new JToolBar();

    public SwingGUI(PdfDecoder decode_pdf, Values commonValues, GUIThumbnailPanel thumbnails, PropertiesFile properties) {
        this.decode_pdf = decode_pdf;
        this.commonValues = commonValues;
        this.thumbnails = thumbnails;
        this.properties = properties;
        if (commonValues.isContentExtractor()) {
            titleMessage = "IDRsolutions Extraction Solution " + PdfDecoder.version + " ";
            showOutlines = false;
        }
    }

    public JComponent getDisplayPane() {
        return displayPane;
    }

    /**
     * adjusty x co-ordinate shown in display for user to include
     * any page centering
     */
    public int AdjustForAlignment(int cx) {
        if (decode_pdf.getPageAlignment() == Display.DISPLAY_CENTERED) {
            int width = decode_pdf.getBounds().width;
            int pdfWidth = decode_pdf.getPDFWidth();
            if (decode_pdf.getDisplayView() != Display.SINGLE_PAGE) pdfWidth = (int) decode_pdf.getMaximumSize().getWidth();
            if (width > pdfWidth) cx = cx - ((width - pdfWidth) / (2));
        }
        return cx;
    }

    public String getBookmark(String bookmark) {
        return tree.getPage(bookmark);
    }

    public void reinitialiseTabs() {
        displayPane.setDividerLocation(startSize);
        lastTabSelected = -1;
        if (commonValues.isContentExtractor()) {
            navOptionsPanel.removeAll();
            displayPane.setDividerLocation(0);
        } else if (!commonValues.isPDF()) {
            navOptionsPanel.setVisible(false);
        } else {
            navOptionsPanel.setVisible(true);
            if (!decode_pdf.hasOutline()) {
                int outlineTab = -1;
                if (PdfDecoder.isRunningOnMac) {
                    String tabName = "";
                    for (int jj = 0; jj < navOptionsPanel.getTabCount(); jj++) {
                        if (navOptionsPanel.getTitleAt(jj).equals(bookmarksTitle)) outlineTab = jj;
                    }
                } else {
                    String tabName = "";
                    for (int jj = 0; jj < navOptionsPanel.getTabCount(); jj++) {
                        if (navOptionsPanel.getIconAt(jj).toString().equals(bookmarksTitle)) outlineTab = jj;
                    }
                }
                if (outlineTab != -1) navOptionsPanel.remove(outlineTab);
            } else {
                int outlineTab = -1;
                if (PdfDecoder.isRunningOnMac) {
                    String tabName = "";
                    for (int jj = 0; jj < navOptionsPanel.getTabCount(); jj++) {
                        if (navOptionsPanel.getTitleAt(jj).equals(bookmarksTitle)) outlineTab = jj;
                    }
                    if (outlineTab == -1) navOptionsPanel.addTab(bookmarksTitle, (SwingOutline) tree);
                } else {
                    String tabName = "";
                    for (int jj = 0; jj < navOptionsPanel.getTabCount(); jj++) {
                        if (navOptionsPanel.getIconAt(jj).toString().equals(bookmarksTitle)) outlineTab = jj;
                    }
                    if (outlineTab == -1) {
                        VTextIcon textIcon2 = new VTextIcon(navOptionsPanel, bookmarksTitle, VTextIcon.ROTATE_LEFT);
                        navOptionsPanel.addTab(null, textIcon2, (SwingOutline) tree);
                    }
                }
            }
            setBookmarks(false);
        }
    }

    public void stopThumbnails() {
        if (thumbnails.isShownOnscreen()) {
            thumbnails.terminateDrawing();
            thumbnails.removeAllListeners();
        }
    }

    public void reinitThumbnails() {
        isSetup = false;
    }

    /**reset so appears closed*/
    public void resetNavBar() {
        displayPane.setDividerLocation(startSize);
        tabsNotInitialised = true;
    }

    public void alignLayoutMenuOption(int mode) {
        int i = 1;
        Enumeration menuOptions = layoutGroup.getElements();
        while (menuOptions.hasMoreElements() && i != mode) {
            menuOptions.nextElement();
            i++;
        }
        ((JMenuItem) menuOptions.nextElement()).setSelected(true);
    }

    /**used when clicking on thumbnails to move onto new page*/
    private class PageChanger implements ActionListener {

        int page;

        public PageChanger(int i) {
            i++;
            page = i;
        }

        public void actionPerformed(ActionEvent e) {
            if ((!commonValues.isProcessing()) && (commonValues.getCurrentPage() != page)) {
                commonValues.setCurrentPage(page);
                statusBar.resetStatus("");
                decodePage(false);
            }
        }
    }

    /**
     * show fonts displayed
     */
    private JScrollPane getFontInfoBox() {
        JPanel details = new JPanel();
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(400, 300));
        scrollPane.getViewport().add(details);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        details.setOpaque(true);
        details.setBackground(Color.white);
        details.setEnabled(false);
        details.setLayout(new BoxLayout(details, BoxLayout.PAGE_AXIS));
        String xmlText = decode_pdf.getFontsInFile();
        if (xmlText.length() > 0) {
            JTextArea xml = new JTextArea();
            xml.setLineWrap(false);
            xml.setText(xmlText);
            details.add(xml);
            xml.setCaretPosition(0);
            xml.setOpaque(false);
            details.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        return scrollPane;
    }

    public void getInfoBox() {
        final JPanel details = new JPanel();
        details.setPreferredSize(new Dimension(400, 260));
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel header1 = new JLabel(Messages.getMessage("PdfViewerInfo.title"));
        header1.setOpaque(false);
        header1.setFont(headFont);
        header1.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(header1);
        details.add(Box.createRigidArea(new Dimension(0, 15)));
        String xmlText = Messages.getMessage("PdfViewerInfo1") + Messages.getMessage("PdfViewerInfo2");
        if (xmlText.length() > 0) {
            JTextArea xml = new JTextArea();
            xml.setOpaque(false);
            xml.setText(xmlText + "\n\nVersion: " + PdfDecoder.version);
            xml.setLineWrap(true);
            xml.setWrapStyleWord(true);
            xml.setEditable(false);
            details.add(xml);
            xml.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        ImageIcon logo = new ImageIcon(getClass().getResource("/org/jpedal/examples/simpleviewer/res/logo.gif"));
        details.add(Box.createRigidArea(new Dimension(0, 25)));
        JLabel idr = new JLabel(logo);
        idr.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(idr);
        final JLabel url = new JLabel("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Text") + Messages.getMessage("PdfViewer.WebAddress"));
        url.setForeground(Color.blue);
        url.setHorizontalAlignment(JLabel.CENTER);
        url.setAlignmentX(Component.CENTER_ALIGNMENT);
        url.addMouseListener(new MouseListener() {

            public void mouseEntered(MouseEvent e) {
                details.setCursor(new Cursor(Cursor.HAND_CURSOR));
                url.setText("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Link") + Messages.getMessage("PdfViewerJpedalLibrary.Text") + Messages.getMessage("PdfViewer.WebAddress") + "</a></center>");
            }

            public void mouseExited(MouseEvent e) {
                details.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                url.setText("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Text") + Messages.getMessage("PdfViewer.WebAddress"));
            }

            public void mouseClicked(MouseEvent e) {
                try {
                    BrowserLauncher.openURL(Messages.getMessage("PdfViewer.VisitWebsite"));
                } catch (IOException e1) {
                    showMessageDialog(Messages.getMessage("PdfViewer.ErrorWebsite"));
                }
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }
        });
        details.add(url);
        details.add(Box.createRigidArea(new Dimension(0, 5)));
        details.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        showMessageDialog(details, Messages.getMessage("PdfViewerInfo3"), JOptionPane.PLAIN_MESSAGE);
    }

    public void resetRotationBox() {
        PdfPageData currentPageData = decode_pdf.getPdfPageData();
        if (decode_pdf.getDisplayView() == Display.SINGLE_PAGE) rotation = currentPageData.getRotation(commonValues.getCurrentPage());
        if (getSelectedComboIndex(Commands.ROTATION) != (rotation / 90)) {
            setSelectedComboIndex(Commands.ROTATION, (rotation / 90));
        } else if (!commonValues.isProcessing()) {
            decode_pdf.repaint();
        }
    }

    /**
     * show document properties
     */
    private JScrollPane getPropertiesBox(String file, String path, String user_dir, long size, int pageCount, int currentPage) {
        PdfFileInformation currentFileInformation = decode_pdf.getFileInformationData();
        if (currentFileInformation != null) {
            JPanel details = new JPanel();
            details.setOpaque(true);
            details.setBackground(Color.white);
            details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setPreferredSize(new Dimension(400, 300));
            scrollPane.getViewport().add(details);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            JLabel header1 = new JLabel(Messages.getMessage("PdfViewerGeneral"));
            header1.setFont(headFont);
            header1.setOpaque(false);
            details.add(header1);
            JLabel g1 = new JLabel(Messages.getMessage("PdfViewerFileName") + file);
            g1.setFont(textFont);
            g1.setOpaque(false);
            details.add(g1);
            JLabel g2 = new JLabel(Messages.getMessage("PdfViewerFilePath") + path);
            g2.setFont(textFont);
            g2.setOpaque(false);
            details.add(g2);
            JLabel g3 = new JLabel(Messages.getMessage("PdfViewerCurrentWorkingDir") + " " + user_dir);
            g3.setFont(textFont);
            g3.setOpaque(false);
            details.add(g3);
            JLabel g4 = new JLabel(Messages.getMessage("PdfViewerFileSize") + size + " K");
            g4.setFont(textFont);
            g4.setOpaque(false);
            details.add(g4);
            JLabel g5 = new JLabel(Messages.getMessage("PdfViewerPageCount") + pageCount);
            g5.setOpaque(false);
            g5.setFont(textFont);
            details.add(g5);
            JLabel g6 = new JLabel("PDF " + decode_pdf.getPDFVersion());
            g6.setOpaque(false);
            g6.setFont(textFont);
            details.add(g6);
            details.add(Box.createVerticalStrut(10));
            JLabel header2 = new JLabel(Messages.getMessage("PdfViewerProperties"));
            header2.setFont(headFont);
            header2.setOpaque(false);
            details.add(header2);
            String[] values = currentFileInformation.getFieldValues();
            String[] fields = currentFileInformation.getFieldNames();
            int count = fields.length;
            JLabel[] displayValues = new JLabel[count];
            for (int i = 0; i < count; i++) {
                if (values[i].length() > 0) {
                    displayValues[i] = new JLabel(fields[i] + " = " + values[i]);
                    displayValues[i].setFont(textFont);
                    displayValues[i].setOpaque(false);
                    details.add(displayValues[i]);
                }
            }
            details.add(Box.createVerticalStrut(10));
            PdfPageData currentPageSize = decode_pdf.getPdfPageData();
            if (currentPageSize != null) {
                JLabel header3 = new JLabel(Messages.getMessage("PdfViewerCoords.text"));
                header3.setFont(headFont);
                details.add(header3);
                JLabel g7 = new JLabel(Messages.getMessage("PdfViewermediaBox.text") + currentPageSize.getMediaValue(currentPage));
                g7.setFont(textFont);
                details.add(g7);
                JLabel g8 = new JLabel(Messages.getMessage("PdfViewercropBox.text") + currentPageSize.getCropValue(currentPage));
                g8.setFont(textFont);
                details.add(g8);
                JLabel g9 = new JLabel(Messages.getMessage("PdfViewerLabel.Rotation") + currentPageSize.getRotation(currentPage));
                g3.setFont(textFont);
                details.add(g9);
            }
            details.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return scrollPane;
        } else {
            return new JScrollPane();
        }
    }

    /**
     * page info option
     */
    private JScrollPane getXMLInfoBox(String xmlText) {
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.PAGE_AXIS));
        details.setOpaque(true);
        details.setBackground(Color.white);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(400, 300));
        scrollPane.getViewport().add(details);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JTextArea xml = new JTextArea();
        xml.setRows(5);
        xml.setColumns(15);
        xml.setLineWrap(true);
        xml.setText(xmlText);
        details.add(new JScrollPane(xml));
        xml.setCaretPosition(0);
        xml.setOpaque(true);
        xml.setBackground(Color.white);
        return scrollPane;
    }

    public void showDocumentProperties(String selectedFile, String inputDir, long size, int pageCount, int currentPage) {
        JTabbedPane tabbedPane = new JTabbedPane();
        if (selectedFile == null) {
            showMessageDialog(Messages.getMessage("PdfVieweremptyFile.message"), Messages.getMessage("PdfViewerTooltip.pageSize"), JOptionPane.PLAIN_MESSAGE);
        } else {
            String filename = selectedFile;
            int ptr = filename.lastIndexOf("\\");
            if (ptr == -1) ptr = filename.lastIndexOf("/");
            String file = filename.substring(ptr + 1, filename.length());
            String path = filename.substring(0, ptr + 1);
            tabbedPane.add(getPropertiesBox(file, path, user_dir, size, pageCount, currentPage));
            tabbedPane.setTitleAt(0, Messages.getMessage("PdfViewerTab.Properties"));
            tabbedPane.add(getFontInfoBox());
            tabbedPane.setTitleAt(1, Messages.getMessage("PdfViewerTab.Fonts"));
            PdfFileInformation currentFileInformation = decode_pdf.getFileInformationData();
            String xmlText = currentFileInformation.getFileXMLMetaData();
            if (xmlText.length() > 0) {
                tabbedPane.add(getXMLInfoBox(xmlText));
                tabbedPane.setTitleAt(2, "XML");
            }
            showMessageDialog(tabbedPane, Messages.getMessage("PdfViewerTab.DocumentProperties"), JOptionPane.PLAIN_MESSAGE);
        }
    }

    public void init(String[] scalingValues, final Object currentCommands, Object currentPrinter) {
        currentCommandListener = new CommandListener((Commands) currentCommands);
        setViewerTitle(Messages.getMessage("PdfViewerEnhanced.titlebar") + "  " + PdfDecoder.version);
        decode_pdf.setInset(inset, inset);
        decode_pdf.setBackground(new Color(190, 190, 190));
        scalingBox = new SwingCombo(scalingValues);
        scalingBox.setBackground(Color.white);
        scalingBox.setEditable(true);
        scalingBox.setSelectedIndex(defaultSelection);
        rotationBox = new SwingCombo(rotationValues);
        rotationBox.setBackground(Color.white);
        rotationBox.setSelectedIndex(0);
        scrollPane.getViewport().add(decode_pdf);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(80);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(80);
        bottomNavButtons.setBorder(BorderFactory.createEmptyBorder());
        bottomNavButtons.setLayout(new FlowLayout(FlowLayout.LEADING));
        bottomNavButtons.setFloatable(false);
        bottomNavButtons.setFont(new Font("SansSerif", Font.PLAIN, 8));
        navOptionsPanel.setTabPlacement(JTabbedPane.LEFT);
        navOptionsPanel.setOpaque(true);
        navOptionsPanel.setMinimumSize(new Dimension(0, 100));
        pageTitle = Messages.getMessage("PdfViewerJPanel.thumbnails");
        bookmarksTitle = Messages.getMessage("PdfViewerJPanel.bookmarks");
        displayPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navOptionsPanel, scrollPane);
        displayPane.setOneTouchExpandable(true);
        if (!commonValues.isContentExtractor()) {
            if (PdfDecoder.isRunningOnMac) {
                if (thumbnails.isShownOnscreen()) navOptionsPanel.addTab(bookmarksTitle, (SwingOutline) tree);
                navOptionsPanel.addTab(pageTitle, (Component) thumbnails);
            } else {
                VTextIcon textIcon2 = new VTextIcon(navOptionsPanel, bookmarksTitle, VTextIcon.ROTATE_LEFT);
                navOptionsPanel.addTab(null, textIcon2, (SwingOutline) tree);
                if (thumbnails.isShownOnscreen()) {
                    VTextIcon textIcon1 = new VTextIcon(navOptionsPanel, pageTitle, VTextIcon.ROTATE_LEFT);
                    navOptionsPanel.addTab(null, textIcon1, (Component) thumbnails);
                }
            }
            displayPane.setDividerLocation(startSize);
        } else displayPane.setDividerLocation(0);
        frame.getContentPane().add(displayPane, BorderLayout.CENTER);
        if (!hasListener) {
            hasListener = true;
            navOptionsPanel.addMouseListener(new MouseListener() {

                public void focusLost(FocusEvent focusEvent) {
                }

                public void mouseClicked(MouseEvent mouseEvent) {
                    handleTabbedPanes();
                }

                public void mousePressed(MouseEvent mouseEvent) {
                }

                public void mouseReleased(MouseEvent mouseEvent) {
                }

                public void mouseEntered(MouseEvent mouseEvent) {
                }

                public void mouseExited(MouseEvent mouseEvent) {
                }
            });
        }
        if (!commonValues.isContentExtractor()) {
            first = new SwingButton();
            fback = new SwingButton();
            back = new SwingButton();
            forward = new SwingButton();
            fforward = new SwingButton();
            end = new SwingButton();
        }
        snapshotButton = new SwingButton();
        singleButton = new SwingButton();
        continuousButton = new SwingButton();
        continuousFacingButton = new SwingButton();
        facingButton = new SwingButton();
        pageCounter2.setEditable(true);
        pageCounter2.setToolTipText(Messages.getMessage("PdfViewerTooltip.goto"));
        pageCounter2.setBorder(BorderFactory.createLineBorder(Color.black));
        pageCounter2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                String value = pageCounter2.getText().trim();
                ((Commands) currentCommands).gotoPage(value);
            }
        });
        pageCounter3 = new JLabel(Messages.getMessage("PdfViewerOfLabel.text") + " ");
        pageCounter3.setOpaque(false);
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        frame.getContentPane().add(top, BorderLayout.NORTH);
        navToolBar.setLayout(new BoxLayout(navToolBar, BoxLayout.LINE_AXIS));
        navToolBar.setFloatable(false);
        pagesToolBar.setFloatable(false);
        comboBar.setBorder(BorderFactory.createEmptyBorder());
        comboBar.setLayout(new BorderLayout());
        comboBar.setFloatable(false);
        comboBar.setPreferredSize(new Dimension(5, 24));
        frame.getContentPane().add(comboBar, BorderLayout.SOUTH);
        top.add(currentMenu, BorderLayout.NORTH);
        topButtons.setBorder(BorderFactory.createEmptyBorder());
        topButtons.setLayout(new FlowLayout(FlowLayout.LEADING));
        topButtons.setFloatable(false);
        topButtons.setFont(new Font("SansSerif", Font.PLAIN, 8));
        top.add(topButtons, BorderLayout.CENTER);
        top.add(bottomNavButtons, BorderLayout.SOUTH);
        createNavbar();
        if (commonValues.getModeOfOperation() != Values.RUNNING_APPLET) {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            int width = d.width / 2, height = d.height / 2;
            if (width < minimumScreenWidth) width = minimumScreenWidth;
            frame.setSize(width, height);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new FrameCloser((Commands) currentCommands, this, decode_pdf, (Printer) currentPrinter, thumbnails, commonValues));
            frame.show();
        }
    }

    private void handleTabbedPanes() {
        if (tabsNotInitialised) return;
        int currentSize = displayPane.getDividerLocation();
        int tabSelected = navOptionsPanel.getSelectedIndex();
        if (tabSelected == -1) return;
        if (currentSize == startSize) {
            String tabName = "";
            if (PdfDecoder.isRunningOnMac) {
                tabName = navOptionsPanel.getTitleAt(tabSelected);
            } else tabName = navOptionsPanel.getIconAt(tabSelected).toString();
            setupThumbnailPanel();
            setBookmarks(true);
            displayPane.setDividerLocation(expandedSize);
        } else if (tabSelected == lastTabSelected) displayPane.setDividerLocation(startSize);
        lastTabSelected = tabSelected;
    }

    public void addCursor() {
        JToolBar cursor = new JToolBar();
        cursor.setBorder(BorderFactory.createEmptyBorder());
        cursor.setLayout(new FlowLayout(FlowLayout.LEADING));
        cursor.setFloatable(false);
        cursor.setFont(new Font("SansSerif", Font.ITALIC, 10));
        cursor.add(new JLabel(Messages.getMessage("PdfViewerToolbarCursorLoc.text")));
        cursor.add(initCoordBox());
        topButtons.add(cursor);
    }

    /**setup keyboard shortcuts*/
    private void setKeyAccelerators(int ID, JMenuItem menuItem) {
        switch(ID) {
            case Commands.SAVE:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK));
                break;
            case Commands.PRINT:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.Event.CTRL_MASK));
                break;
            case Commands.EXIT:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.Event.CTRL_MASK));
                break;
            case Commands.DOCINFO:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.Event.CTRL_MASK));
                break;
            case Commands.OPENFILE:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Event.CTRL_MASK));
                break;
            case Commands.OPENURL:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.Event.CTRL_MASK));
                break;
            case Commands.PREVIOUSDOCUMENT:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK));
                break;
            case Commands.NEXTDOCUMENT:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK));
                break;
            case Commands.FIRSTPAGE:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_HOME, 0));
                break;
            case Commands.BACKPAGE:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, 0));
                break;
            case Commands.FORWARDPAGE:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0));
                break;
            case Commands.LASTPAGE:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_END, 0));
                break;
            case Commands.GOTO:
                menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
                break;
        }
    }

    public void addButton(int line, String toolTip, String path, final int ID) {
        GUIButton newButton = new SwingButton();
        switch(ID) {
            case Commands.FIRSTPAGE:
                newButton = first;
                break;
            case Commands.FBACKPAGE:
                newButton = fback;
                break;
            case Commands.BACKPAGE:
                newButton = back;
                break;
            case Commands.FORWARDPAGE:
                newButton = forward;
                break;
            case Commands.FFORWARDPAGE:
                newButton = fforward;
                break;
            case Commands.LASTPAGE:
                newButton = end;
                break;
            case Commands.SNAPSHOT:
                newButton = snapshotButton;
                break;
            case Commands.SINGLE:
                newButton = singleButton;
                break;
            case Commands.CONTINUOUS:
                newButton = continuousButton;
                break;
            case Commands.CONTINUOUS_FACING:
                newButton = continuousFacingButton;
                break;
            case Commands.FACING:
                newButton = facingButton;
                break;
        }
        newButton.init(path, ID, toolTip);
        ((AbstractButton) newButton).addActionListener(currentCommandListener);
        if (line == BUTTONBAR) {
            topButtons.add((AbstractButton) newButton);
            topButtons.add(Box.createHorizontalGlue());
        } else if (line == NAVBAR) {
            navToolBar.add((AbstractButton) newButton);
        } else if (line == PAGES) {
            pagesToolBar.add((AbstractButton) newButton, BorderLayout.CENTER);
        }
    }

    public void addMenuItem(JMenu parentMenu, String text, String toolTip, final int ID) {
        SwingMenuItem menuItem = new SwingMenuItem(text);
        if (toolTip.length() > 0) menuItem.setToolTipText(toolTip);
        menuItem.setID(ID);
        setKeyAccelerators(ID, menuItem);
        menuItem.addActionListener(currentCommandListener);
        parentMenu.add(menuItem);
    }

    public void addCombo(String title, String tooltip, int ID) {
        GUICombo combo = null;
        switch(ID) {
            case Commands.SCALING:
                combo = scalingBox;
                break;
            case Commands.ROTATION:
                combo = rotationBox;
                break;
        }
        combo.setID(ID);
        optimizationLabel = new JLabel(title);
        if (tooltip.length() > 0) combo.setToolTipText(tooltip);
        bottomNavButtons.add(optimizationLabel);
        bottomNavButtons.add((SwingCombo) combo);
        ((SwingCombo) combo).addActionListener(currentCommandListener);
    }

    public void setViewerTitle(final String title) {
        if (title != null) {
            frame.setTitle(title);
        } else {
            String finalMessage = "";
            if (titleMessage == null) finalMessage = (Messages.getMessage("PdfViewerEnhanced.titlebar") + PdfDecoder.version + " " + commonValues.getSelectedFile()); else finalMessage = titleMessage + commonValues.getSelectedFile();
            if (commonValues.isFormsChanged()) finalMessage = "* " + finalMessage;
            frame.setTitle(finalMessage);
        }
    }

    public void resetComboBoxes(boolean value) {
        scalingBox.setEnabled(value);
        rotationBox.setEnabled(value);
    }

    public final JScrollPane createPane(JTextPane text_pane, String content, boolean useXML) throws BadLocationException {
        text_pane.setEditable(true);
        text_pane.setFont(new Font("Lucida", Font.PLAIN, 14));
        text_pane.setToolTipText(Messages.getMessage("PdfViewerTooltip.text"));
        Document doc = text_pane.getDocument();
        text_pane.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(), Messages.getMessage("PdfViewerTitle.text")));
        text_pane.setForeground(Color.black);
        SimpleAttributeSet token_attribute = new SimpleAttributeSet();
        SimpleAttributeSet text_attribute = new SimpleAttributeSet();
        SimpleAttributeSet plain_attribute = new SimpleAttributeSet();
        StyleConstants.setForeground(token_attribute, Color.blue);
        StyleConstants.setForeground(text_attribute, Color.black);
        StyleConstants.setForeground(plain_attribute, Color.black);
        int pointer = 0;
        if ((useXML) && (content != null)) {
            StringTokenizer data_As_tokens = new StringTokenizer(content, "<>", true);
            while (data_As_tokens.hasMoreTokens()) {
                String next_item = data_As_tokens.nextToken();
                if ((next_item.equals("<")) && ((data_As_tokens.hasMoreTokens()))) {
                    String current_token = next_item + data_As_tokens.nextToken() + data_As_tokens.nextToken();
                    doc.insertString(pointer, current_token, token_attribute);
                    pointer = pointer + current_token.length();
                } else {
                    doc.insertString(pointer, next_item, text_attribute);
                    pointer = pointer + next_item.length();
                }
            }
        } else doc.insertString(pointer, content, plain_attribute);
        JScrollPane text_scroll = new JScrollPane();
        text_scroll.getViewport().add(text_pane);
        text_scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        text_scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return text_scroll;
    }

    public int getSelectedComboIndex(int ID) {
        switch(ID) {
            case Commands.SCALING:
                return scalingBox.getSelectedIndex();
            case Commands.ROTATION:
                return rotationBox.getSelectedIndex();
            default:
                return -1;
        }
    }

    public void setSelectedComboIndex(int ID, int index) {
        switch(ID) {
            case Commands.SCALING:
                scalingBox.setSelectedIndex(index);
                break;
            case Commands.ROTATION:
                rotationBox.setSelectedIndex(index);
                break;
        }
    }

    public void setSelectedComboItem(int ID, String index) {
        switch(ID) {
            case Commands.SCALING:
                scalingBox.setSelectedItem(index);
                break;
            case Commands.ROTATION:
                rotationBox.setSelectedItem(index);
                break;
        }
    }

    public Object getSelectedComboItem(int ID) {
        switch(ID) {
            case Commands.SCALING:
                return scalingBox.getSelectedItem();
            case Commands.ROTATION:
                return rotationBox.getSelectedItem();
            default:
                return null;
        }
    }

    public void zoom(boolean Rotated) {
        float width, height;
        width = scrollPane.getViewport().getWidth() - inset - inset;
        height = scrollPane.getViewport().getHeight() - inset - inset;
        if (decode_pdf != null) {
            int index = getSelectedComboIndex(Commands.SCALING);
            if (index == -1) {
                String numberValue = (String) getSelectedComboItem(Commands.SCALING);
                float zoom = -1;
                if ((numberValue != null) && (numberValue.length() > 0)) {
                    try {
                        zoom = Float.parseFloat(numberValue);
                    } catch (Exception e) {
                        zoom = -1;
                        int length = numberValue.length();
                        int ii = 0;
                        while (ii < length) {
                            char c = numberValue.charAt(ii);
                            if (((c >= '0') && (c <= '9')) | (c == '.')) ii++; else break;
                        }
                        if (ii > 0) numberValue = numberValue.substring(0, ii);
                        if (zoom == -1) {
                            try {
                                zoom = Float.parseFloat(numberValue);
                            } catch (Exception e1) {
                                zoom = -1;
                            }
                        }
                    }
                    if (zoom > 1000) {
                        zoom = 1000;
                    }
                }
                if (zoom == -1) {
                    index = defaultSelection;
                    setSelectedComboIndex(Commands.SCALING, index);
                } else {
                    scaling = convertToCorrectedScaling(zoom / 100);
                    setSelectedComboItem(Commands.SCALING, zoom + "");
                }
            }
            if (index != -1) {
                if (index < 3) {
                    PdfPageData pageData = decode_pdf.getPdfPageData();
                    int cw, ch, raw_rotation = pageData.getRotation(commonValues.getCurrentPage());
                    if (rotation == 90 || rotation == 270) {
                        cw = pageData.getCropBoxHeight(commonValues.getCurrentPage());
                        ch = pageData.getCropBoxWidth(commonValues.getCurrentPage());
                    } else {
                        cw = pageData.getCropBoxWidth(commonValues.getCurrentPage());
                        ch = pageData.getCropBoxHeight(commonValues.getCurrentPage());
                    }
                    if (displayPane != null) {
                        width = width - displayPane.getDividerSize();
                    }
                    float x_factor = 0, y_factor = 0;
                    x_factor = width / cw;
                    y_factor = height / ch;
                    if (index == 0) {
                        if (x_factor < y_factor) scaling = x_factor; else scaling = y_factor;
                    } else if (index == 1) scaling = y_factor; else if (index == 2) scaling = x_factor;
                } else {
                    scaling = convertToCorrectedScaling(scalingFloatValues[index]);
                }
            }
            PdfPageData pagedata = decode_pdf.getPdfPageData();
            if ((pagedata.getCropBoxHeight(commonValues.getCurrentPage()) * scaling < 100) && (pagedata.getCropBoxWidth(commonValues.getCurrentPage()) * scaling < 100) && commonValues.isPDF()) {
                scaling = 1;
                setSelectedComboItem(Commands.SCALING, "100");
            }
            decode_pdf.setPageParameters(scaling, commonValues.getCurrentPage(), rotation);
            decode_pdf.invalidate();
            decode_pdf.updateUI();
            scrollPane.invalidate();
            scrollPane.updateUI();
            if (Rotated) {
            }
            if (commonValues.isPDF()) scrollToPage(commonValues.getCurrentPage());
        }
    }

    private float convertToCorrectedScaling(float rawScaling) {
        if (PdfDecoder.isRunningOnWindows) {
            return rawScaling * 1.53f;
        } else {
            return rawScaling;
        }
    }

    public void rotate() {
        rotation = Integer.parseInt((String) getSelectedComboItem(Commands.ROTATION));
        zoom(true);
        decode_pdf.updateUI();
    }

    public void scrollToPage(int page) {
        commonValues.setCurrentPage(page);
        if (decode_pdf.getDisplayView() != Display.SINGLE_PAGE && commonValues.getCurrentPage() > 0) {
            int yCord = decode_pdf.getYCordForPage(commonValues.getCurrentPage(), scaling);
            Rectangle r = scrollPane.getVisibleRect();
            decode_pdf.scrollRectToVisible(new Rectangle(0, (int) (yCord), (int) r.width - 1, (int) r.height - 1));
            decode_pdf.scrollRectToVisible(new Rectangle(0, (int) (yCord), (int) r.width - 1, (int) r.height - 1));
        }
    }

    public void decodePage(final boolean resizePanel) {
        if (thumbnails.isShownOnscreen()) thumbnails.terminateDrawing();
        if (thumbnails.isShownOnscreen()) setupThumbnailPanel();
        if (decode_pdf.getDisplayView() == Display.SINGLE_PAGE) {
            pageCounter2.setForeground(Color.black);
            pageCounter2.setText(" " + commonValues.getCurrentPage());
            pageCounter3.setText(Messages.getMessage("PdfViewerOfLabel.text") + " " + commonValues.getPageCount());
        }
        tabsNotInitialised = false;
        boolean isContentExtractor = commonValues.isContentExtractor();
        decode_pdf.unsetScaling();
        if (isContentExtractor) decode_pdf.setExtractionMode(PdfDecoder.TEXT); else decode_pdf.setExtractionMode(PdfDecoder.TEXT + PdfDecoder.TEXTCOLOR);
        decode_pdf.setFoundTextArea(null);
        decode_pdf.setFoundTextAreas(null);
        decode_pdf.setHighlightedAreas(null);
        setRectangle(null);
        if (decode_pdf.getDisplayView() == Display.FACING) {
            zoom(false);
            scrollToPage(commonValues.getCurrentPage());
            decode_pdf.decodeOtherPages(commonValues.getPageCount());
            return;
        } else if (decode_pdf.getDisplayView() == Display.CONTINUOUS || decode_pdf.getDisplayView() == Display.CONTINUOUS_FACING) {
            zoom(false);
            scrollToPage(commonValues.getCurrentPage());
            return;
        }
        resetComboBoxes(false);
        if (!commonValues.isContentExtractor()) commonValues.setProcessing(true);
        SwingWorker worker = new SwingWorker() {

            public Object construct() {
                try {
                    statusBar.updateStatus("Decoding Page", 0);
                    if (Thread.interrupted()) throw new InterruptedException();
                    try {
                        decode_pdf.decodePage(commonValues.getCurrentPage());
                        if (!decode_pdf.hasAllImages()) {
                            String status = (Messages.getMessage("PdfViewer.ImageDisplayError") + Messages.getMessage("PdfViewer.ImageDisplayError1") + Messages.getMessage("PdfViewer.ImageDisplayError2") + Messages.getMessage("PdfViewer.ImageDisplayError3") + Messages.getMessage("PdfViewer.ImageDisplayError4") + Messages.getMessage("PdfViewer.ImageDisplayError5") + Messages.getMessage("PdfViewer.ImageDisplayError6") + Messages.getMessage("PdfViewer.ImageDisplayError7"));
                            showMessageDialog(status);
                        }
                        PdfPageData page_data = decode_pdf.getPdfPageData();
                        mediaW = page_data.getMediaBoxWidth(commonValues.getCurrentPage());
                        mediaH = page_data.getMediaBoxHeight(commonValues.getCurrentPage());
                        mediaX = page_data.getMediaBoxX(commonValues.getCurrentPage());
                        mediaY = page_data.getMediaBoxY(commonValues.getCurrentPage());
                        cropX = page_data.getCropBoxX(commonValues.getCurrentPage());
                        cropY = page_data.getCropBoxY(commonValues.getCurrentPage());
                        cropW = page_data.getCropBoxWidth(commonValues.getCurrentPage());
                        cropH = page_data.getCropBoxHeight(commonValues.getCurrentPage());
                        resetRotationBox();
                        commonValues.setPageAnnotations(decode_pdf.getPdfAnnotsData(null));
                        if (!decode_pdf.showAnnotations) {
                            createUniqueAnnotationIcons();
                        }
                        statusBar.updateStatus("Displaying Page", 0);
                    } catch (Exception e) {
                        System.err.println(Messages.getMessage("PdfViewerError.Exception") + " " + e + " " + Messages.getMessage("PdfViewerError.DecodePage"));
                        e.printStackTrace();
                        commonValues.setProcessing(false);
                    }
                    String status = decode_pdf.getPageDecodeReport();
                    if ((status.indexOf("java.lang.OutOfMemoryError") != -1) && PdfDecoder.showErrorMessages) {
                        status = (Messages.getMessage("PdfViewer.OutOfMemoryDisplayError") + Messages.getMessage("PdfViewer.OutOfMemoryDisplayError1") + Messages.getMessage("PdfViewer.OutOfMemoryDisplayError2") + Messages.getMessage("PdfViewer.OutOfMemoryDisplayError3") + Messages.getMessage("PdfViewer.OutOfMemoryDisplayError4") + Messages.getMessage("PdfViewer.OutOfMemoryDisplayError5"));
                        showMessageDialog(status);
                    }
                    if ((decode_pdf.getPageDecodeReport().indexOf("JBIG") != -1)) {
                        status = "This File Contains a JBIG image. \n\nSupport for JBIG images is currently in development";
                        showMessageDialog(status);
                    }
                    if (thumbnails != null && decode_pdf.getDisplayView() == Display.SINGLE_PAGE) thumbnails.addDisplayedPageAsThumbnail(commonValues.getCurrentPage(), null);
                    commonValues.setProcessing(false);
                    setViewerTitle(null);
                    if (thumbnails.isShownOnscreen() && decode_pdf.getDisplayView() == Display.SINGLE_PAGE) thumbnails.generateOtherVisibleThumbnails(commonValues.getCurrentPage());
                } catch (Exception e) {
                    setViewerTitle(null);
                }
                selectBookmark();
                statusBar.setProgress(100);
                resetComboBoxes(true);
                addFormsListeners();
                String message = null;
                if (decode_pdf.hasJavascript() && decode_pdf.isXFAForm()) {
                    message = (Messages.getMessage("PdfViewerMessage.JavaScriptAndXFA") + Messages.getMessage("PdfViewerMessage.JavaScriptAndXFA1") + Messages.getMessage("PdfViewerMessage.JavaScriptAndXFA2"));
                    messageShown = true;
                }
                if (!messageShown && decode_pdf.isXFAForm()) {
                    message = (Messages.getMessage("PdfViewerMessage.XFA") + Messages.getMessage("PdfViewerMessage.XFA1") + Messages.getMessage("PdfViewerMessage.XFA2"));
                    messageShown = true;
                }
                if (!messageShown && decode_pdf.hasJavascript()) {
                    message = (Messages.getMessage("PdfViewerMessage.JavaScript") + Messages.getMessage("PdfViewerMessage.JavaScript1"));
                    messageShown = true;
                }
                if (message != null) showMessageDialog(message);
                decode_pdf.setPDFBorder(BorderFactory.createLineBorder(Color.black, 1));
                decode_pdf.disableBorderForPrinting();
                zoom(false);
                decode_pdf.updateUI();
                finishedDecoding = true;
                return null;
            }
        };
        worker.start();
    }

    /**this method adds listeners to GUI widgets to track changes*/
    private void addFormsListeners() {
        commonValues.setFormsChanged(false);
        boolean showMessage = false;
        String formsFlag = System.getProperty("listenForms");
        if (formsFlag != null) showMessage = true;
        org.jpedal.objects.acroforms.AcroRenderer formRenderer = decode_pdf.getCurrentFormRenderer();
        if (formRenderer == null) return;
        java.util.List formsOnPage = null;
        try {
            formsOnPage = formRenderer.getComponentNameList(commonValues.getCurrentPage());
        } catch (PdfException e) {
            LogWriter.writeLog("Exception " + e + " reading component list");
        }
        if (formsOnPage == null) {
            if (showMessage) showMessageDialog(Messages.getMessage("PdfViewer.NoFields"));
            return;
        }
        int formCount = formsOnPage.size();
        JPanel formPanel = new JPanel();
        if (showMessage) {
            formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
            JLabel formHeader = new JLabel("This page contains " + formCount + " form objects");
            formHeader.setFont(headFont);
            formPanel.add(formHeader);
            formPanel.add(Box.createRigidArea(new Dimension(10, 10)));
            JTextPane instructions = new JTextPane();
            instructions.setPreferredSize(new Dimension(450, 180));
            instructions.setEditable(false);
            instructions.setText("This provides a simple example of Forms handling. We have" + " added a listener to each form so clicking on it shows the form name.\n\n" + "Code is in addExampleListeners() in org.examples.simpleviewer.SimpleViewer\n\n" + "This could be easily be extended to interface with a database directly " + "or collect results on an action and write back using itext.\n\n" + "Forms have been converted into Swing components and are directly accessible" + " (as is the original data).\n\n" + "If you don't like the standard SwingSet you can replace with your own set.");
            instructions.setFont(textFont);
            formPanel.add(instructions);
            formPanel.add(Box.createRigidArea(new Dimension(10, 10)));
        }
        for (int i = 0; i < formCount; i++) {
            String formName = (String) formsOnPage.get(i);
            Component[] comp = formRenderer.getComponentsByName(formName);
            Integer pageKey = new Integer(i);
            if (comp != null && pagesDecoded.get(pageKey) == null) {
                pagesDecoded.put(pageKey, "x");
                int count = comp.length;
                for (int index = 0; index < count; index++) {
                    if ((showMessage) && (index == 0)) {
                        JLabel type = new JLabel();
                        JLabel label = new JLabel("Form name=" + formName);
                        String labelS = "type=" + comp[index].getClass();
                        if (count > 1) {
                            labelS = "Group of " + count + " Objects, type=" + comp[index].getClass();
                            type.setForeground(Color.red);
                        }
                        type.setText(labelS);
                        label.setFont(headFont);
                        type.setFont(textFont);
                        formPanel.add(label);
                        formPanel.add(type);
                        formPanel.add(new JLabel(" "));
                    }
                    FormActionListener changeList = new FormActionListener(formName + index, frame, showMessage);
                    if (comp[index] instanceof JComboBox) {
                        ((JComboBox) comp[index]).addActionListener(changeList);
                    } else if (comp[index] instanceof JCheckBox) {
                        ((JCheckBox) comp[index]).addActionListener(changeList);
                    } else if (comp[index] instanceof JRadioButton) {
                        ((JRadioButton) comp[index]).addActionListener(changeList);
                    } else if (comp[index] instanceof JTextField) {
                        ((JTextField) comp[index]).addActionListener(changeList);
                    }
                }
            }
        }
        if (showMessage) {
            final JDialog displayFrame = new JDialog(frame, true);
            if (commonValues.getModeOfOperation() != Values.RUNNING_APPLET) {
                displayFrame.setLocationRelativeTo(null);
                displayFrame.setLocation(frame.getLocationOnScreen().x + 10, frame.getLocationOnScreen().y + 10);
            }
            JScrollPane scroll = new JScrollPane();
            scroll.getViewport().add(formPanel);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            displayFrame.setSize(500, 500);
            displayFrame.setTitle("List of forms on this page");
            displayFrame.getContentPane().setLayout(new BorderLayout());
            displayFrame.getContentPane().add(scroll, BorderLayout.CENTER);
            JPanel buttonBar = new JPanel();
            buttonBar.setLayout(new BorderLayout());
            displayFrame.getContentPane().add(buttonBar, BorderLayout.SOUTH);
            JButton no = new JButton(Messages.getMessage("PdfViewerButton.Close"));
            no.setFont(new Font("SansSerif", Font.PLAIN, 12));
            buttonBar.add(no, BorderLayout.EAST);
            no.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    displayFrame.dispose();
                }
            });
            displayFrame.show();
        }
    }

    /**
     *  put the outline data into a display panel which we can pop up
     * for the user - outlines, thumbnails
     */
    private void createOutlinePanels() {
        setupThumbnailPanel();
        setBookmarks(false);
    }

    private void setupThumbnailPanel() {
        decode_pdf.setThumbnailPanel(thumbnails);
        if (isSetup) return;
        isSetup = true;
        if (!commonValues.isContentExtractor() && thumbnails.isShownOnscreen()) {
            int pages = decode_pdf.getPageCount();
            thumbnails.setupThumbnails(pages, textFont, Messages.getMessage("PdfViewerPageLabel.text"), decode_pdf.getPdfPageData());
            Object[] buttons = thumbnails.getButtons();
            for (int i = 0; i < pages; i++) ((JButton) buttons[i]).addActionListener(new PageChanger(i));
            thumbnails.addComponentListener();
        }
    }

    public void setBookmarks(boolean alwaysGenerate) {
        int currentSize = displayPane.getDividerLocation();
        if ((currentSize == startSize) && !alwaysGenerate) return;
        boolean hasNavBars;
        {
            hasNavBars = true;
            org.w3c.dom.Document doc = decode_pdf.getOutlineAsXML();
            Node rootNode = null;
            if (doc != null) rootNode = doc.getFirstChild();
            if (rootNode != null) {
                tree.reset(rootNode);
                ((JTree) tree.getTree()).addTreeSelectionListener(new TreeSelectionListener() {

                    /** Required by TreeSelectionListener interface. */
                    public void valueChanged(TreeSelectionEvent e) {
                        if (tree.isIgnoreAlteredBookmark()) return;
                        DefaultMutableTreeNode node = tree.getLastSelectedPathComponent();
                        if (node == null) return;
                        Object nodeInfo = node.getUserObject();
                        String title = (String) node.getUserObject();
                        String page = tree.getPage(title);
                        if ((page != null) && (page.length() > 0)) {
                            int pageToDisplay = Integer.parseInt(page);
                            if ((!commonValues.isProcessing()) && (commonValues.getCurrentPage() != pageToDisplay)) {
                                commonValues.setCurrentPage(pageToDisplay);
                                setScalingToDefault();
                                decode_pdf.setPageParameters(getScaling(), commonValues.getCurrentPage());
                                decodePage(false);
                            }
                            Point p = tree.getPoint(title);
                            if (p != null) decode_pdf.ensurePointIsVisible(p);
                        } else {
                            showMessageDialog(Messages.getMessage("PdfViewerError.NoBookmarkLink") + title);
                            System.out.println("No dest page set for " + title);
                        }
                    }
                });
            } else {
                hasNavBars = false;
                tree.reset(null);
            }
        }
    }

    private void selectBookmark() {
        if (decode_pdf.hasOutline() && (tree != null)) tree.selectBookmark();
    }

    public void initStatus() {
        decode_pdf.setStatusBarObject(statusBar);
        resetStatus();
    }

    public void resetStatus() {
        statusBar.setColorForSubroutines(Color.blue);
        bottomNavButtons.add(statusBar.getStatusObject());
    }

    public void initThumbnails(int itemSelectedCount, Vector_Int pageUsed) {
        navOptionsPanel.removeAll();
        if (thumbnails.isShownOnscreen()) thumbnails.setupThumbnails(itemSelectedCount - 1, pageUsed.get(), commonValues.getPageCount());
        if (PdfDecoder.isRunningOnMac) {
            navOptionsPanel.add((Component) thumbnails, "Extracted items");
        } else {
            VTextIcon textIcon2 = new VTextIcon(navOptionsPanel, "Extracted items", VTextIcon.ROTATE_LEFT);
            navOptionsPanel.addTab(null, textIcon2, (Component) thumbnails);
        }
        displayPane.setDividerLocation(150);
    }

    class FormActionListener implements ActionListener {

        private Container c;

        private String formName;

        boolean showMessage;

        public FormActionListener(String formName, Container c, boolean showMessage) {
            this.c = c;
            this.formName = formName;
            this.showMessage = showMessage;
        }

        public void actionPerformed(ActionEvent arg0) {
            Object comp = arg0.getSource();
            Object value = null;
            if (comp instanceof JComboBox) value = ((JComboBox) comp).getSelectedItem(); else if (comp instanceof JCheckBox) value = "" + ((JCheckBox) comp).isSelected(); else if (comp instanceof JRadioButton) value = "" + ((JRadioButton) comp).isSelected(); else if (comp instanceof JTextField) value = "" + ((JTextField) comp).getText();
            {
                boolean showSaveFormsMessage = properties.getValue("showsaveformsmessage").equals("true");
                if (showSaveFormsMessage && firstTimeFormMessage && commonValues.isFormsChanged() == false) {
                    firstTimeFormMessage = false;
                    JPanel panel = new JPanel();
                    panel.setLayout(new GridBagLayout());
                    final GridBagConstraints p = new GridBagConstraints();
                    p.anchor = GridBagConstraints.WEST;
                    p.gridx = 0;
                    p.gridy = 0;
                    String str = (Messages.getMessage("PdfViewerFormsWarning.ChangedFormsValue"));
                    if (!commonValues.isItextOnClasspath()) str = (Messages.getMessage("PdfViewerFormsWarning.ChangedFormsValueNoItext"));
                    JCheckBox cb = new JCheckBox();
                    cb.setText(Messages.getMessage("PdfViewerFormsWarning.CheckBox"));
                    Font font = cb.getFont();
                    JTextArea ta = new JTextArea(str);
                    ta.setOpaque(false);
                    ta.setFont(font);
                    p.ipady = 20;
                    panel.add(ta, p);
                    p.ipady = 0;
                    p.gridy = 1;
                    panel.add(cb, p);
                    JOptionPane.showMessageDialog(c, panel);
                    if (cb.isSelected()) properties.setValue("showsaveformsmessage", "false");
                }
            }
            commonValues.setFormsChanged(true);
            setViewerTitle(null);
            if (showMessage) JOptionPane.showMessageDialog(c, "FormName >>" + formName + "<<. Value changed to " + value);
        }
    }

    public void setCoordText(String string) {
        coords.setText(string);
    }

    private JLabel initCoordBox() {
        coords.setBackground(Color.white);
        coords.setOpaque(true);
        coords.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        coords.setText("  X: " + " Y: " + " " + " ");
        return coords;
    }

    public void toggleSnapshotButton() {
        if (!commonValues.isExtractImageOnSelection()) snapshotButton.setIcon(new ImageIcon(getClass().getResource("/org/jpedal/examples/simpleviewer/res/snapshotX.gif"))); else snapshotButton.setIcon(new ImageIcon(getClass().getResource("/org/jpedal/examples/simpleviewer/res/snapshot.gif")));
    }

    public void setPageNumber() {
        pageCounter2.setForeground(Color.black);
        pageCounter2.setText(" " + commonValues.getCurrentPage());
        pageCounter3.setText(Messages.getMessage("PdfViewerOfLabel.text") + " " + commonValues.getPageCount());
    }

    private void createNavbar() {
        comboBar.add(Box.createHorizontalGlue());
        JLabel pageCounter1 = new JLabel(Messages.getMessage("PdfViewerPageLabel.text"));
        pageCounter1.setOpaque(false);
        navToolBar.add(Box.createHorizontalGlue());
        addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.RewindToStart"), "/org/jpedal/examples/simpleviewer/res/start.gif", Commands.FIRSTPAGE);
        addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.Rewind10"), "/org/jpedal/examples/simpleviewer/res/fback.gif", Commands.FBACKPAGE);
        addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.Rewind1"), "/org/jpedal/examples/simpleviewer/res/back.gif", Commands.BACKPAGE);
        navToolBar.add(pageCounter1);
        pageCounter2.setMaximumSize(new Dimension(5, 50));
        navToolBar.add(pageCounter2);
        navToolBar.add(pageCounter3);
        addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.Forward1"), "/org/jpedal/examples/simpleviewer/res/forward.gif", Commands.FORWARDPAGE);
        addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.Forward10"), "/org/jpedal/examples/simpleviewer/res/fforward.gif", Commands.FFORWARDPAGE);
        addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.ForwardLast"), "/org/jpedal/examples/simpleviewer/res/end.gif", Commands.LASTPAGE);
        navToolBar.add(Box.createHorizontalGlue());
        comboBar.add(navToolBar, BorderLayout.CENTER);
        comboBar.add(pagesToolBar, BorderLayout.EAST);
        Dimension size = pagesToolBar.getPreferredSize();
        final JProgressBar pb = new JProgressBar();
        pb.setPreferredSize(size);
        Timer memoryMonitor = new Timer(500, new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                int free = (int) (Runtime.getRuntime().freeMemory() / (1024 * 1024));
                int total = (int) (Runtime.getRuntime().totalMemory() / (1024 * 1024));
                if (finishedDecoding) {
                    System.gc();
                    finishedDecoding = false;
                }
                pb.setMaximum(total);
                pb.setValue(total - free);
                pb.setStringPainted(true);
                pb.setString((total - free) + "M of " + total + "M");
            }
        });
        memoryMonitor.start();
        Filler filler = new Box.Filler(size, size, size);
        comboBar.add(pb, BorderLayout.WEST);
    }

    public void setPage(int page) {
        commonValues.setCurrentPage(page);
        pageCounter2.setText(page + "");
        setThumbnails();
    }

    public void setRotation() {
        PdfPageData currentPageData = decode_pdf.getPdfPageData();
        rotation = currentPageData.getRotation(commonValues.getCurrentPage());
        System.out.println("rotation ==" + rotation + "<");
        rotation = (rotation + (getSelectedComboIndex(Commands.ROTATION) * 90));
        System.out.println("rotation ==" + rotation + "<");
        if (rotation > 360) rotation = rotation - 360;
        System.out.println("rotation ==" + rotation + "<");
        if (getSelectedComboIndex(Commands.ROTATION) != (rotation / 90)) {
            setSelectedComboIndex(Commands.ROTATION, (rotation / 90));
        } else if (!commonValues.isProcessing()) {
            decode_pdf.repaint();
        }
    }

    public void addToMainMenu(JMenu fileMenuList) {
        currentMenu.add(fileMenuList);
    }

    public JFrame getFrame() {
        return frame;
    }

    public JToolBar getTopButtonBar() {
        return topButtons;
    }

    public void showMessageDialog(Object message1) {
        JOptionPane.showMessageDialog(frame, message1);
    }

    public void showMessageDialog(Object message, String title, int type) {
        JOptionPane.showMessageDialog(frame, message, title, type);
    }

    public String showInputDialog(Object message, String title, int type) {
        return JOptionPane.showInputDialog(frame, message, title, type);
    }

    public String showInputDialog(String message) {
        return JOptionPane.showInputDialog(frame, message);
    }

    public int showOptionDialog(Object displayValue, String message, int option, int type, Object icon, Object[] options, Object initial) {
        return JOptionPane.showOptionDialog(frame, displayValue, message, option, type, (Icon) icon, options, initial);
    }

    public int showConfirmDialog(String message, String message2, int option) {
        return JOptionPane.showConfirmDialog(frame, message, message2, option);
    }

    public int showOverwriteDialog(String file, boolean yesToAllPresent) {
        int n = -1;
        if (yesToAllPresent) {
            final Object[] buttonRowObjects = new Object[] { Messages.getMessage("PdfViewerConfirmButton.Yes"), Messages.getMessage("PdfViewerConfirmButton.YesToAll"), Messages.getMessage("PdfViewerConfirmButton.No"), Messages.getMessage("PdfViewerConfirmButton.Cancel") };
            n = JOptionPane.showOptionDialog(frame, file + "\n" + Messages.getMessage("PdfViewerMessage.FileAlreadyExists") + "\n" + Messages.getMessage("PdfViewerMessage.ConfirmResave"), Messages.getMessage("PdfViewerMessage.Overwrite"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonRowObjects, buttonRowObjects[0]);
        } else {
            n = JOptionPane.showOptionDialog(frame, file + "\n" + Messages.getMessage("PdfViewerMessage.FileAlreadyExists") + "\n" + Messages.getMessage("PdfViewerMessage.ConfirmResave"), Messages.getMessage("PdfViewerMessage.Overwrite"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        }
        return n;
    }

    public void showMessageDialog(JTextArea info) {
        JOptionPane.showMessageDialog(frame, info);
    }

    public void showItextPopup() {
        JEditorPane p = new JEditorPane("text/html", "Itext is not on the classpath.<BR>" + "JPedal includes code to take advantage of itext and<BR>" + "provide additional functionality with options<BR>" + "to spilt pdf files, and resave forms data<BR>" + "\nItext website - <a href=http://www.lowagie.com/iText/>http://www.lowagie.com/iText/</a>");
        p.setEditable(false);
        p.setOpaque(false);
        p.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    try {
                        BrowserLauncher.openURL("http://www.lowagie.com/iText/");
                    } catch (IOException e1) {
                        showMessageDialog(Messages.getMessage("PdfViewer.ErrorWebsite"));
                    }
                }
            }
        });
        showMessageDialog(p);
    }

    public void showFirstTimePopup() {
        try {
            final JPanel a = new JPanel();
            a.setLayout(new BorderLayout());
            JLabel lab = new JLabel(new ImageIcon(getClass().getResource("/org/jpedal/objects/acroforms/ceo.jpg")));
            a.add(lab, BorderLayout.NORTH);
            final JLabel message = new JLabel("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Text") + Messages.getMessage("PdfViewer.WebAddress"));
            message.setHorizontalAlignment(JLabel.CENTER);
            message.setForeground(Color.blue);
            message.setFont(new Font("Lucida", Font.PLAIN, 16));
            message.addMouseListener(new MouseListener() {

                public void mouseEntered(MouseEvent e) {
                    a.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    message.setText("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Link") + Messages.getMessage("PdfViewerJpedalLibrary.Text") + Messages.getMessage("PdfViewer.WebAddress") + "</a></center>");
                }

                public void mouseExited(MouseEvent e) {
                    a.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    message.setText("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Text") + Messages.getMessage("PdfViewer.WebAddress"));
                }

                public void mouseClicked(MouseEvent e) {
                    try {
                        BrowserLauncher.openURL(Messages.getMessage("PdfViewer.VisitWebsite"));
                    } catch (IOException e1) {
                        showMessageDialog(Messages.getMessage("PdfViewer.ErrorWebsite"));
                    }
                }

                public void mousePressed(MouseEvent e) {
                }

                public void mouseReleased(MouseEvent e) {
                }
            });
            a.add(message, BorderLayout.CENTER);
            a.setPreferredSize(new Dimension(300, 240));
            Object[] options = { Messages.getMessage("PdfViewerButton.RunSoftware") };
            int n = JOptionPane.showOptionDialog(frame, a, Messages.getMessage("PdfViewerTitle.RunningFirstTime"), JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        } catch (Exception e) {
            System.err.println(Messages.getMessage("PdfViewerFirstRunDialog.Error"));
        } catch (Error e) {
            System.err.println(Messages.getMessage("PdfViewerFirstRunDialog.Error"));
        }
    }

    public void showConfirmDialog(Object label, String message, int option, int plain_message) {
        JOptionPane.showConfirmDialog(frame, label, message, option, plain_message);
    }

    public void updateStatusMessage(String message) {
        statusBar.updateStatus(message, 0);
    }

    public void resetStatusMessage(String message) {
        statusBar.resetStatus(message);
    }

    public void setStatusProgress(int size) {
        statusBar.setProgress(size);
    }

    public boolean isPDFOutlineVisible() {
        return navOptionsPanel.isVisible();
    }

    public void setPDFOutlineVisible(boolean visible) {
        navOptionsPanel.setVisible(visible);
    }

    public void setSplitDividerLocation(int size) {
        displayPane.setDividerLocation(size);
    }

    public void setQualityBoxVisible(boolean visible) {
    }

    private void setThumbnails() {
        SwingWorker worker = new SwingWorker() {

            public Object construct() {
                if (thumbnails.isShownOnscreen()) {
                    setupThumbnailPanel();
                    if (decode_pdf.getDisplayView() == Display.SINGLE_PAGE) thumbnails.generateOtherVisibleThumbnails(commonValues.getCurrentPage());
                }
                return null;
            }
        };
        worker.start();
    }
}
