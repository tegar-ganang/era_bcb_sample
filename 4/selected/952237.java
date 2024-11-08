package persdocmanager.gui.use;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.media.jai.JAI;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import net.coobird.thumbnailator.Thumbnailator;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import persdocmanager.Persdocmanager;
import persdocmanager.action.Actionhandler;
import persdocmanager.action.BasicAction;
import persdocmanager.action.EditPageExternalAction;
import persdocmanager.action.MailAction;
import persdocmanager.action.PrintPageAction;
import persdocmanager.action.ScanAction;
import persdocmanager.action.ShowDocumentLinksAction;
import persdocmanager.database.Category;
import persdocmanager.database.DBWriter;
import persdocmanager.database.Docindex;
import persdocmanager.database.Documentpages;
import persdocmanager.database.Documents;
import persdocmanager.database.Formfield;
import persdocmanager.database.Forms;
import persdocmanager.database.Indextypes;
import persdocmanager.errorhandling.PropertyNotFoundException;
import persdocmanager.gui.base.BasicDocumentFrame;
import persdocmanager.gui.base.BasicFormBasedPanel;
import persdocmanager.gui.base.BasicFormField;
import persdocmanager.gui.base.CorrectionPanel;
import persdocmanager.gui.base.DeviceBoxModel;
import persdocmanager.gui.base.DocumentRenderer;
import persdocmanager.gui.base.FormCheckBox;
import persdocmanager.gui.base.FormComboBox;
import persdocmanager.gui.base.FormDateChooser;
import persdocmanager.gui.base.FormFactory;
import persdocmanager.gui.base.FormField;
import persdocmanager.gui.base.FormTextArea;
import persdocmanager.gui.base.FormTextField;
import persdocmanager.gui.base.PageLabel;
import persdocmanager.gui.table.DocumentPageTableModel;
import persdocmanager.gui.table.DocumentPageTableRenderer;
import persdocmanager.gui.table.PageTableSelectionListener;
import persdocmanager.scan.FileSource;
import persdocmanager.scan.ScanSource;
import persdocmanager.scan.ScanSourceFactory;
import persdocmanager.util.Configuration;
import persdocmanager.util.ContentType;

/**
 * @author Gerald Skokalski
 * 
 */
public class DocumentFrame extends BasicDocumentFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = -4878503474058591792L;

    private DocumentPageTableModel mPagesmodel = new DocumentPageTableModel();

    private Documents mDocument = new Documents();

    private JSplitPane mDocumentSplitter;

    private JScrollPane jScrollPane1;

    private JScrollPane jScrollPane2;

    private JPopupMenu PageTablePopup;

    private DocumentRenderer mDocumentPanel;

    private JTable mDocumentPagesTable = new JTable();

    private Category mCategory;

    private Hashtable<Integer, BufferedImage> mFinished = null;

    private ArrayList<PageLabel> mPagesToDelete = new ArrayList<PageLabel>();

    static ResourceBundle mActionbundle = ResourceBundle.getBundle(BasicAction.class.getName(), Locale.getDefault());

    /**
	 * 
	 */
    public DocumentFrame() {
        super();
        prepareUI();
    }

    /**
	 * @param pTitle
	 */
    public DocumentFrame(String pTitle, Category pCategory) {
        super(pTitle);
        mCategory = pCategory;
        prepareUI();
    }

    /**
	 * @param pTitle
	 * @param pResizable
	 */
    public DocumentFrame(String pTitle, boolean pResizable) {
        super(pTitle, pResizable);
        prepareUI();
    }

    /**
	 * @param pTitle
	 * @param pResizable
	 * @param pClosable
	 */
    public DocumentFrame(String pTitle, boolean pResizable, boolean pClosable) {
        super(pTitle, pResizable, pClosable);
        prepareUI();
    }

    /**
	 * @param pTitle
	 * @param pResizable
	 * @param pClosable
	 * @param pMaximizable
	 */
    public DocumentFrame(String pTitle, boolean pResizable, boolean pClosable, boolean pMaximizable) {
        super(pTitle, pResizable, pClosable, pMaximizable);
        prepareUI();
    }

    /**
	 * @param pTitle
	 * @param pResizable
	 * @param pClosable
	 * @param pMaximizable
	 * @param pIconifiable
	 */
    public DocumentFrame(String pTitle, boolean pResizable, boolean pClosable, boolean pMaximizable, boolean pIconifiable) {
        super(pTitle, pResizable, pClosable, pMaximizable, pIconifiable);
        prepareUI();
    }

    private void prepareUI() {
        registerClosingEvent();
        ArrayList<ScanSource> l = new ArrayList<ScanSource>();
        l.addAll(Configuration.getScanSources());
        pack();
        getScanSourceListBox().setModel(new DeviceBoxModel(l));
        super.getDocumentSidePanel().setPreferredSize(new java.awt.Dimension(600, 295));
        mDocumentPagesTable.setModel(mPagesmodel);
        mDocumentPagesTable.setRowHeight(100);
        mDocumentPagesTable.getColumnModel().getColumn(0).setCellRenderer(new DocumentPageTableRenderer());
        mDocumentPagesTable.setVisible(true);
        mDocumentPagesTable.setRowMargin(10);
        mDocumentPagesTable.setTableHeader(null);
        mDocumentPagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mDocumentPagesTable.setSelectionForeground(Color.orange);
        mDocumentPagesTable.getSelectionModel().addListSelectionListener(new PageTableSelectionListener(mDocumentPagesTable, this));
        {
            mDocumentSplitter = new JSplitPane();
            super.getDocumentSidePanel().add(mDocumentSplitter, BorderLayout.CENTER);
            mDocumentSplitter.setPreferredSize(new java.awt.Dimension(600, 272));
            {
                jScrollPane1 = new JScrollPane();
                mDocumentSplitter.add(jScrollPane1, JSplitPane.LEFT);
                jScrollPane1.setViewportView(mDocumentPagesTable);
                setComponentPopupMenu(mDocumentPagesTable, getPageTablePopup());
            }
            {
                jScrollPane2 = new JScrollPane();
                mDocumentSplitter.add(jScrollPane2, JSplitPane.RIGHT);
                {
                    mDocumentPanel = new DocumentRenderer();
                    jScrollPane2.setViewportView(mDocumentPanel);
                }
            }
            mDocumentSplitter.setDividerLocation(200);
        }
        getScanAppend().setToolTipText(mActionbundle.getString("scan.append.statustext"));
        getScanAppend().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent pE) {
                ScanAction action = new ScanAction("scan.append");
                action.scan(pE, (ScanSource) getScanSourceListBox().getSelectedItem());
                ArrayList<File> pages = action.getScannedPages();
                File scandir = new File(Configuration.getTmpDir() + File.separator + "scanned");
                scandir.mkdir();
                for (File f : pages) {
                    Documentpages p = new Documentpages();
                    File dest = new File(Configuration.getTmpDir() + File.separator + "scanned" + File.separator + f.getName());
                    try {
                        FileUtils.copyFile(f.getAbsoluteFile(), dest);
                        p.setFilename(dest.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        p.setFilename(f.getAbsolutePath());
                    }
                    mPagesmodel.appendPage(p);
                }
                refreshPageNumbers();
            }
        });
        getScanBtn().setToolTipText(mActionbundle.getString("scan.append.statustext"));
        getScanBtn().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent pE) {
                ScanAction action = new ScanAction("scan.append");
                action.scan(pE, (ScanSource) getScanSourceListBox().getSelectedItem());
                ArrayList<File> pages = action.getScannedPages();
                for (File f : pages) {
                    Documentpages p = new Documentpages();
                    p.setFilename(f.getAbsolutePath());
                    mPagesmodel.appendPage(p);
                }
                refreshPageNumbers();
            }
        });
        getScanAfterBtn().setToolTipText(mActionbundle.getString("scan.after.statustext"));
        getScanAfterBtn().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent pE) {
                ScanAction action = new ScanAction("scan.after");
                action.scan(pE, (ScanSource) getScanSourceListBox().getSelectedItem());
                ArrayList<File> pages = action.getScannedPages();
                int selection = mDocumentPagesTable.getSelectedRow();
                for (File f : pages) {
                    Documentpages p = new Documentpages();
                    p.setFilename(f.getAbsolutePath());
                    if (selection <= 0) {
                        mPagesmodel.appendPage(p);
                    } else {
                        mPagesmodel.insertPage(selection + 1, p);
                    }
                }
                refreshPageNumbers();
            }
        });
        getScanReplaceBtn().setToolTipText(mActionbundle.getString("scan.replace.statustext"));
        getScanReplaceBtn().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent pE) {
                ScanAction action = new ScanAction("scan.replace");
                action.scan(pE, (ScanSource) getScanSourceListBox().getSelectedItem());
                ArrayList<File> pages = action.getScannedPages();
                int selection = mDocumentPagesTable.getSelectedRow();
                for (File f : pages) {
                    Documentpages p = new Documentpages();
                    p.setFilename(f.getAbsolutePath());
                    if (selection < 0) {
                        mPagesmodel.appendPage(p);
                    } else {
                        mPagesmodel.replacePage(selection, p);
                    }
                }
                refreshPageNumbers();
                mDocumentPanel.removeAll();
            }
        });
        getScanBeforeBtn().setToolTipText(mActionbundle.getString("scan.before.statustext"));
        getScanBeforeBtn().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent pE) {
                ScanAction action = new ScanAction("scan.before");
                action.scan(pE, (ScanSource) getScanSourceListBox().getSelectedItem());
                ArrayList<File> pages = action.getScannedPages();
                int selection = mDocumentPagesTable.getSelectedRow();
                for (File f : pages) {
                    Documentpages p = new Documentpages();
                    p.setFilename(f.getAbsolutePath());
                    if (selection < 0) {
                        selection = 0;
                    }
                    mPagesmodel.insertPage(selection, p);
                }
                refreshPageNumbers();
            }
        });
        getDeleteStackBtn().setToolTipText(mActionbundle.getString("scan.deletepage"));
        getDeleteStackBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                int selection = mDocumentPagesTable.getSelectedRow();
                mPagesToDelete.add(mPagesmodel.getPages().get(selection));
                mPagesmodel.removePage(selection);
                mDocumentPanel.removeAll();
                refreshPageNumbers();
            }
        });
        getMoveFirstBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                int selection = mDocumentPagesTable.getSelectedRow();
                if (selection != -1) {
                    mPagesmodel.moveFirst(selection);
                    refreshPageNumbers();
                    mDocumentPanel.removeAll();
                }
            }
        });
        getMoveLastBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                int selection = mDocumentPagesTable.getSelectedRow();
                if (selection != -1) {
                    mPagesmodel.moveLast(selection);
                    refreshPageNumbers();
                    mDocumentPanel.removeAll();
                }
            }
        });
        getMoveForwardBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                int selection = mDocumentPagesTable.getSelectedRow();
                if (selection != -1) {
                    mPagesmodel.moveForwards(selection);
                    refreshPageNumbers();
                    mDocumentPagesTable.getSelectionModel().setSelectionInterval(selection + 1, selection + 1);
                }
            }
        });
        getMoveBackwardsBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                int selection = mDocumentPagesTable.getSelectedRow();
                if (selection != -1) {
                    mPagesmodel.moveBackwards(selection);
                    refreshPageNumbers();
                    mDocumentPagesTable.getSelectionModel().setSelectionInterval(selection - 1, selection - 1);
                }
            }
        });
        getZoomInBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                File p;
                if (getDocumentPanel().getPage() != null) {
                    p = getDocumentPanel().getPage();
                    String ct = ContentType.getContentTypeForFilename(p.getName());
                    if (ct.startsWith("image")) {
                        ImageIcon ico = (ImageIcon) getDocumentPanel().getDisplay().getIcon();
                        BufferedImage img = (BufferedImage) ico.getImage();
                        img = Thumbnailator.createThumbnail(img, img.getWidth() + img.getWidth() / 2, img.getHeight() + img.getHeight() / 2);
                        getDocumentPanel().getDisplay().setIcon(new ImageIcon(img));
                        getDocumentPanel().updateUI();
                    }
                }
            }
        });
        getZoomOutBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                File p;
                if (getDocumentPanel().getPage() != null) {
                    p = getDocumentPanel().getPage();
                    String ct = ContentType.getContentTypeForFilename(p.getName());
                    if (ct.startsWith("image")) {
                        ImageIcon ico = (ImageIcon) getDocumentPanel().getDisplay().getIcon();
                        BufferedImage img = (BufferedImage) ico.getImage();
                        img = Thumbnailator.createThumbnail(img, img.getWidth() - img.getWidth() / 2, img.getHeight() - img.getHeight() / 2);
                        getDocumentPanel().getDisplay().setIcon(new ImageIcon(img));
                        getDocumentPanel().updateUI();
                    }
                }
            }
        });
        getRotateLeftBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                File p;
                if (getDocumentPanel().getPage() != null) {
                    p = getDocumentPanel().getPage();
                    String ct = ContentType.getContentTypeForFilename(p.getName());
                    if (ct.startsWith("image")) {
                        ImageIcon ico = (ImageIcon) getDocumentPanel().getDisplay().getIcon();
                        BufferedImage img = (BufferedImage) ico.getImage();
                        try {
                            img = Thumbnails.of(img).size(img.getWidth(), img.getHeight()).rotate(-90).asBufferedImage();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        getDocumentPanel().getDisplay().setIcon(new ImageIcon(img));
                        getDocumentPanel().updateUI();
                    }
                }
            }
        });
        getRotateRightBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                File p;
                if (getDocumentPanel().getPage() != null) {
                    p = getDocumentPanel().getPage();
                    String ct = ContentType.getContentTypeForFilename(p.getName());
                    if (ct.startsWith("image")) {
                        ImageIcon ico = (ImageIcon) getDocumentPanel().getDisplay().getIcon();
                        BufferedImage img = (BufferedImage) ico.getImage();
                        try {
                            img = Thumbnails.of(img).size(img.getWidth(), img.getHeight()).rotate(90).asBufferedImage();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        getDocumentPanel().getDisplay().setIcon(new ImageIcon(img));
                        getDocumentPanel().updateUI();
                    }
                }
            }
        });
        getApplyImageChangesBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                File p;
                if (getDocumentPanel().getPage() != null) {
                    p = getDocumentPanel().getPage();
                    String ct = ContentType.getContentTypeForFilename(p.getName());
                    getDocumentPanel().setPage(p);
                    if (ct.startsWith("image")) {
                        ImageIcon ico = (ImageIcon) getDocumentPanel().getDisplay().getIcon();
                        BufferedImage img = (BufferedImage) ico.getImage();
                        try {
                            Thumbnails.of(img).size(img.getWidth(), img.getHeight()).toFile(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        getUndoImageChangesBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent pE) {
                File p;
                if (getDocumentPanel().getPage() != null) {
                    p = getDocumentPanel().getPage();
                    getDocumentPanel().setPage(p);
                }
            }
        });
    }

    protected void registerClosingEvent() {
        this.addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameClosing(InternalFrameEvent pE) {
                super.internalFrameClosing(pE);
                UseMainFrame.removeHistoriesFromHistoryBtl();
                UseMainFrame.removeLinksFromLinkBtn();
                Actionhandler.disableAction(Actionhandler.CREATE_DOCUMENT_LINK);
                Actionhandler.disableAction(Actionhandler.SHOW_DOCUMENT_LINK_TREE);
            }

            @Override
            public void internalFrameActivated(InternalFrameEvent pE) {
                super.internalFrameActivated(pE);
                Actionhandler.enableAction(Actionhandler.CREATE_DOCUMENT_LINK);
                Actionhandler.enableAction(Actionhandler.SHOW_DOCUMENT_LINKS);
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent pE) {
                super.internalFrameDeactivated(pE);
                UseMainFrame.removeHistoriesFromHistoryBtl();
                UseMainFrame.removeLinksFromLinkBtn();
                Actionhandler.disableAction(Actionhandler.CREATE_DOCUMENT_LINK);
                Actionhandler.disableAction(Actionhandler.SHOW_DOCUMENT_LINKS);
                Actionhandler.disableAction(Actionhandler.SHOW_DOCUMENT_LINK_TREE);
            }
        });
    }

    protected void refreshPageNumbers() {
        for (int i = 0; i < mPagesmodel.getPages().size(); i++) {
            PageLabel l = mPagesmodel.getPages().get(i);
            l.getPage().setPagenumber(i + 1);
            l.setText(String.valueOf(l.getPage().getPagenumber()));
            l.repaint();
        }
        this.updateUI();
    }

    protected void constructGUI(ArrayList<Documentpages> pPages, ArrayList<BasicFormField> pFormFields) {
        System.out.println("entering constructGUI");
        final ArrayList<Documentpages> pages = pPages;
        final ArrayList<BasicFormField> fields = pFormFields;
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                FormFactory.getForm(getFormpanel(), fields, persdocmanager.gui.base.FormFactory.UPDATE_DOCUMENT);
                for (Component comp : getFormpanel().getComponents()) {
                    if (comp instanceof BasicFormBasedPanel) {
                        Component[] c = ((BasicFormBasedPanel) comp).getFormFieldspanel().getComponents();
                        for (Component element : c) {
                            if (element instanceof FormField) {
                                FormField bff = (FormField) element;
                                for (Docindex idx : mDocument.getDocindexes()) {
                                    if (idx.getFormfield().getFieldid() == bff.getFieldId()) {
                                        if (element instanceof JComboBox) {
                                            ((JComboBox) element).setSelectedItem(idx.getIndexvalue());
                                        }
                                        if (element instanceof JTextField) {
                                            ((JTextField) element).setText(idx.getIndexvalue());
                                        }
                                        if (element instanceof JScrollPane) {
                                            JScrollPane p = (JScrollPane) element;
                                            ((JTextArea) p.getViewport().getComponent(0)).setText(idx.getIndexvalue());
                                        }
                                        if (element instanceof FormDateChooser) {
                                            SimpleDateFormat sd = new SimpleDateFormat();
                                            sd.applyPattern("yyyy-MM-dd");
                                            try {
                                                if (idx.getIndexvalue() != null) {
                                                    ((FormDateChooser) element).setDate(sd.parse(idx.getIndexvalue()));
                                                }
                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HashSet<Documentpages> set = new HashSet<Documentpages>(pages);
                mDocument.setDocumentpagesesForDocid(set);
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        super.run();
                        loadPageIcons(pages);
                    }
                };
                System.err.println("starting loading pages");
                t.start();
                while (mFinished == null) {
                    updateUI();
                    repaint();
                    t.yield();
                }
                t = null;
                System.err.println("finished loading pages");
                for (Documentpages p : pages) {
                    if (p != null) {
                        BufferedImage i = mFinished.get(p.getPagenumber());
                        System.out.println(i);
                        PageLabel l = new PageLabel();
                        l.setIcon(new ImageIcon(i));
                        System.out.println(p.getFilename());
                        l.setPage(p);
                        mPagesmodel.insertPage(p.getPagenumber() - 1, p);
                        mPagesmodel.fireTableDataChanged();
                    }
                }
                refreshPageNumbers();
            }
        });
    }

    private void loadPageIcons(ArrayList<Documentpages> pPages) {
        final Hashtable finished = new Hashtable<Integer, BufferedImage>();
        for (final Documentpages page : pPages) {
            if (page.getFilename().startsWith(Configuration.getTmpDir())) {
                String filename = page.getFilename();
                String sub = filename.substring(filename.lastIndexOf(File.separator) + 1);
                page.setFilename(sub);
            }
            try {
                new persdocmanager.crypto.Crypto().decryptFile(Configuration.getArchiveDir() + File.separator + page.getFilename(), Configuration.getTmpDir() + File.separator + page.getFilename(), Configuration.getCryptokey());
                new persdocmanager.crypto.Crypto().decryptFile(Configuration.getArchiveDir() + File.separator + "_thumb_" + page.getFilename(), Configuration.getTmpDir() + File.separator + "_thumb_" + page.getFilename(), Configuration.getCryptokey());
                try {
                    BufferedImage thumb = Persdocmanager.getImageLoader().getBufferedImage(Configuration.getTmpDir() + File.separator + "_thumb_" + page.getFilename(), 60, 40);
                    finished.put(page.getPagenumber(), thumb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                page.setFilename(Configuration.getTmpDir() + File.separator + page.getFilename());
            } catch (PropertyNotFoundException e1) {
                final String msg = e1.getMessage();
                final Exception e = e1;
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        Persdocmanager.handleError(msg, e);
                    }
                });
                e1.printStackTrace();
            } catch (Exception e1) {
                final String msg = e1.getMessage();
                final Exception e = e1;
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        Persdocmanager.handleError(msg, e);
                    }
                });
                e1.printStackTrace();
            }
        }
        mFinished = finished;
    }

    public void setDocumentExternal(Documents pDocuments) {
        mDocument = pDocuments;
        ArrayList<Documentpages> pages = new ArrayList<Documentpages>();
        ArrayList<BasicFormField> fields = new ArrayList<BasicFormField>();
        pages.addAll(DBWriter.getPagesForDocument(mDocument));
        fields.addAll(DBWriter.getCategoryFormFields(mCategory));
        constructGUI(pages, fields);
    }

    /**
	 * @return the documentPanel
	 */
    public DocumentRenderer getDocumentPanel() {
        return mDocumentPanel;
    }

    /**
	 * completes the document object so it can be saved pages are added from the
	 * pagetablemodel and indexes are added from the form
	 * 
	 * @return the document
	 */
    public Documents getDocument() {
        DocumentPageTableModel tm = (DocumentPageTableModel) mDocumentPagesTable.getModel();
        Vector<Documentpages> v = new Vector<Documentpages>();
        Vector<PageLabel> pl = tm.getPages();
        for (int i = 0; i < pl.size(); i++) {
            PageLabel plabel = pl.get(i);
            Documentpages page = (Documentpages) plabel.getPage();
            v.add(page);
        }
        HashSet<Documentpages> ps = new HashSet<Documentpages>();
        ps.addAll(v);
        mDocument.setDocumentpagesesForDocid(ps);
        HashSet<Docindex> idx = new HashSet<Docindex>();
        idx.addAll(extractDocindexes());
        mDocument.setDocindexes(idx);
        mDocument.setCategory(mCategory);
        if (mDocument.getCreated() == null) {
            mDocument.setCreated(new Date());
        }
        return mDocument;
    }

    /**
	 * @param pDocument
	 *            the document to set
	 */
    public void setDocument(Documents pDocument) {
        mDocument = pDocument;
    }

    /**
	 * 
	 * get the formpanel ( the one with the index formular ) then check for the
	 * mandatory fields content if there is any unfilled mandatory field the
	 * method will ask for an entry into the field
	 */
    public void checkIfDocCanBeSaved() {
        System.out.println("entering checkIfDocCanBeSaved");
        JPanel formPanel = getFormpanel();
        BasicFormBasedPanel bFbP = null;
        Component jc = null;
        int children = formPanel.getComponentCount();
        for (int i = 0; i < children; i++) {
            jc = formPanel.getComponent(i);
            System.err.println(jc);
            if (jc instanceof BasicFormBasedPanel) {
                System.out.println("formpanel gefunden");
                bFbP = (BasicFormBasedPanel) jc;
                break;
            }
        }
        JPanel formFileds = bFbP.getFormFieldspanel();
        Component[] comp = formFileds.getComponents();
        for (Component c : comp) {
            if (c instanceof FormDateChooser) {
                FormDateChooser fdc = (FormDateChooser) c;
                fdc.setDateFormatString("yyyy-MM-dd");
                if (fdc.isMandatory() == true) {
                    if (fdc.getDate() == null) {
                        new CorrectionPanel(formPanel, this);
                    }
                }
            }
            if (c instanceof FormTextField) {
                FormTextField ftf = (FormTextField) c;
                if (ftf.isMandatory() == true) {
                    if (ftf.getText() == null || ftf.getText().equals(" ") || ftf.getText().equals("")) {
                        new CorrectionPanel(formPanel, this);
                    }
                }
            }
            if (c instanceof FormTextArea) {
                FormTextArea fta = (FormTextArea) c;
                System.err.println(fta.isMandatory());
                if (fta.isMandatory() == true) {
                    if (fta.getText() == null || fta.getText().equals(" ") || fta.getText().equals("")) {
                        new CorrectionPanel(formPanel, this);
                    }
                }
            }
            if (c instanceof FormCheckBox) {
                FormCheckBox fcb = (FormCheckBox) c;
                if (fcb.isMandatory() == true) {
                    System.err.println(fcb.isMandatory());
                    if (fcb.isSelected() == false) {
                        new CorrectionPanel(formPanel, this);
                    }
                }
            }
            if (c instanceof FormComboBox) {
                FormComboBox fcc = (FormComboBox) c;
                if (fcc.isMandatory() == true) {
                    if (fcc.getSelectedIndex() == 0 || fcc.getSelectedItem() == null) {
                        new CorrectionPanel(formPanel, this);
                    }
                }
            }
        }
    }

    /**
	 * This method gets the Indexvalues from the Form of the DocumentFrame and
	 * adds them to the document about to save
	 * 
	 * @return a vector holding the indexes
	 */
    public Vector<Docindex> extractDocindexes() {
        System.out.println("entering extractDocindexes");
        Vector<Docindex> indexes = new Vector<Docindex>();
        JPanel formPanel = getFormpanel();
        BasicFormBasedPanel bFbP = null;
        Component jc = null;
        int children = formPanel.getComponentCount();
        for (int i = 0; i < children; i++) {
            jc = formPanel.getComponent(i);
            System.err.println(jc);
            if (jc instanceof BasicFormBasedPanel) {
                System.out.println("formpanel gefunden");
                bFbP = (BasicFormBasedPanel) jc;
                break;
            }
        }
        JPanel formFileds = bFbP.getFormFieldspanel();
        Component[] comp = formFileds.getComponents();
        for (Component c : comp) {
            if (c instanceof FormField) {
                FormField fdc = (FormField) c;
                Docindex di = new Docindex();
                Boolean mandatory = new Boolean(fdc.isMandatory());
                di.setFormfield(new Formfield(fdc.getFieldId(), new Forms(mCategory), mandatory.toString()));
                di.setDocuments(mDocument);
                Indextypes it = new Indextypes();
                it.setIndextypeid(fdc.getindextypeId());
                di.setIndextypesByIndextype(it);
                di.setIndexvalue(fdc.getvalue());
                di.setCategory(mDocument.getCategory());
                indexes.add(di);
            }
        }
        return indexes;
    }

    public JPopupMenu getPageTablePopup() {
        if (PageTablePopup == null) {
            PageTablePopup = new JPopupMenu();
        }
        return PageTablePopup;
    }

    /**
	 * Auto-generated method for setting the popup menu for a component
	 */
    private void setComponentPopupMenu(final java.awt.Component parent, final javax.swing.JPopupMenu menu) {
        parent.addMouseListener(new java.awt.event.MouseAdapter() {

            private void prepareMenu(Object pObject) {
                getPageTablePopup().removeAll();
                Object o = pObject;
                PageLabel pl = (PageLabel) o;
                ArrayList<Documentpages> pages = new ArrayList<Documentpages>();
                pages.add((Documentpages) pl.getPage());
                MailAction ma = new MailAction(mActionbundle.getString("open.mail"), Persdocmanager.getImageLoader().getImage("email_small.png"), pages);
                String filename = pl.getPage().getFilename();
                EditPageExternalAction ea = new EditPageExternalAction(mActionbundle.getString("open.edit"), Persdocmanager.getImageLoader().getImage("indexdata_edit.png"), new File(filename));
                PrintPageAction ppa = new PrintPageAction(mActionbundle.getString("open.print"), Persdocmanager.getImageLoader().getImage("print_small.png"));
                ppa.setFileToPrint(new File(pl.getPage().getFilename()));
                getPageTablePopup().add(ma);
                getPageTablePopup().add(ea);
                getPageTablePopup().add(ppa);
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    Object o = mDocumentPagesTable.getValueAt(mDocumentPagesTable.rowAtPoint(e.getPoint()), mDocumentPagesTable.columnAtPoint(e.getPoint()));
                    prepareMenu(o);
                    menu.show(parent, e.getX(), e.getY());
                }
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    Object o = mDocumentPagesTable.getValueAt(mDocumentPagesTable.rowAtPoint(e.getPoint()), mDocumentPagesTable.columnAtPoint(e.getPoint()));
                    prepareMenu(o);
                    menu.show(parent, e.getX(), e.getY());
                }
            }
        });
    }

    /**
	 * @return the pagesToDelete
	 */
    public ArrayList<PageLabel> getPagesToDelete() {
        return mPagesToDelete;
    }
}
