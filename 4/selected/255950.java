package au.gov.naa.digipres.xena.viewer;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.EtchedBorder;
import au.gov.naa.digipres.xena.core.NormalisedObjectViewFactory;
import au.gov.naa.digipres.xena.core.Xena;
import au.gov.naa.digipres.xena.kernel.FileExistsException;
import au.gov.naa.digipres.xena.kernel.IconFactory;
import au.gov.naa.digipres.xena.kernel.XenaException;
import au.gov.naa.digipres.xena.kernel.XenaInputSource;
import au.gov.naa.digipres.xena.kernel.view.ViewManager;
import au.gov.naa.digipres.xena.kernel.view.XenaView;

/**
 * Simple frame to display a Normalised Object View, a JPanel
 * which has been returned by NormalisedOjectViewFactory.
 * 
 * The frame's toolbar will contain a select box which will 
 * enable the user to change the type of view, e.g. Package, 
 * raw XML, tree XML etc.
 * 
 * created 29/11/2005
 * xena
 * Short desc of class: frame to display Normalised Object Views
 */
public class NormalisedObjectViewDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_WIDTH = 800;

    public static final int DEFAULT_HEIGHT = 600;

    public static final Cursor busyCursor = new Cursor(Cursor.WAIT_CURSOR);

    public static final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    private ViewManager viewManager;

    JComboBox viewTypeCombo;

    DefaultComboBoxModel viewTypeModel = null;

    private XenaView currentDisplayView;

    private File xenaFile;

    NormalisedObjectViewFactory novFactory;

    JPanel xenaViewPanel;

    /**
	 * Construct a new NormalisedObjectViewDialog with a Dialog as a parent
	 * 
	 * @param parent
	 * @param xenaView
	 * @param xena
	 * @param xenaFile
	 */
    public NormalisedObjectViewDialog(Dialog parent, XenaView xenaView, Xena xena, File xenaFile) {
        super(parent, false);
        init(xenaView, xena, xenaFile);
    }

    /**
	 * Construct a new NormalisedObjectViewDialog with a Frame as a parent
	 * 
	 * @param parent
	 * @param xenaView
	 * @param xena
	 * @param xenaFile
	 */
    public NormalisedObjectViewDialog(Frame parent, XenaView xenaView, Xena xena, File xenaFile) {
        super(parent, false);
        init(xenaView, xena, xenaFile);
    }

    /**
	 * Initialise the NormalisedObjectViewDialog
	 * 
	 * @param xenaView
	 * @param xena
	 * @param xenaFile
	 */
    private void init(XenaView xenaView, Xena xena, File xenaFile) {
        this.xenaFile = xenaFile;
        this.viewManager = xena.getPluginManager().getViewManager();
        novFactory = new NormalisedObjectViewFactory(viewManager);
        try {
            initFrame(viewManager.isShowExportButton());
            setupTypeComboBox(xenaView);
            displayXenaView(xenaView);
            currentDisplayView = xenaView;
        } catch (XenaException e) {
            handleException(e);
        }
    }

    /**
	 * One-time initialisation of frame GUI - menu, toolbar and
	 * event listeners.
	 * 
	 * @throws XenaException
	 */
    private void initFrame(boolean showExportButton) throws XenaException {
        setIconImage(IconFactory.getIconByName("images/xena-splash.png").getImage());
        JToolBar toolBar = new JToolBar();
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel toolBarPanel = new JPanel(new BorderLayout());
        toolBarPanel.setBorder(new EtchedBorder());
        viewTypeCombo = new JComboBox();
        viewTypeCombo.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    try {
                        displayXenaView(currentDisplayView, (XenaView) viewTypeModel.getSelectedItem());
                    } catch (XenaException e1) {
                        handleException(e1);
                    }
                }
            }
        });
        JButton exportButton = new JButton("Export");
        exportButton.setVisible(showExportButton);
        exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportXenaFile();
            }
        });
        toolBar.add(viewTypeCombo);
        toolBar.add(exportButton);
        toolBarPanel.add(toolBar, BorderLayout.NORTH);
        getContentPane().add(toolBarPanel, BorderLayout.NORTH);
        xenaViewPanel = new JPanel(new BorderLayout());
        getContentPane().add(xenaViewPanel, BorderLayout.CENTER);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                doCloseWindow();
            }
        });
    }

    /**
	 * Displays the given XenaView, by adding the view to the display panel.
	 * 
	 * @param concreteView
	 * @throws XenaException
	 */
    private void displayXenaView(XenaView concreteView) throws XenaException {
        xenaViewPanel.removeAll();
        xenaViewPanel.add(concreteView, BorderLayout.CENTER);
        validate();
        setTitle("XenaViewer - " + xenaFile.getName() + " (" + viewTypeModel.getSelectedItem().toString() + ")");
        System.gc();
    }

    /**
	 * Displays the given view using the new view type. A new XenaView,
	 * using the new view type, is retrieved from the 
	 * NormalisedObjectViewFactory.
	 * 
	 * @param concreteView
	 * @param viewType
	 * @throws XenaException
	 */
    private void displayXenaView(XenaView concreteView, XenaView viewType) throws XenaException {
        viewType = viewManager.lookup(viewType.getClass(), concreteView.getLevel(), concreteView.getTopTag());
        XenaView displayView = novFactory.getView(xenaFile, viewType);
        displayXenaView(displayView);
        currentDisplayView = displayView;
    }

    /**
	 * Retrieves the list of view types applicable to the given XenaView,
	 * and displays these options in the combox box.
	 * 
	 * @param xenaView
	 * @throws XenaException
	 */
    private void setupTypeComboBox(XenaView xenaView) throws XenaException {
        viewTypeModel = new DefaultComboBoxModel();
        List<XenaView> viewTypes = viewManager.lookup(xenaView.getTopTag(), 0);
        Iterator<XenaView> iter = viewTypes.iterator();
        while (iter.hasNext()) {
            viewTypeModel.addElement(iter.next());
        }
        viewTypeCombo.setModel(viewTypeModel);
    }

    /**
	 * Surrender this window's resources, and close
	 *
	 */
    private void doCloseWindow() {
        setVisible(false);
        dispose();
        currentDisplayView.doClose();
        currentDisplayView = null;
        System.gc();
    }

    /**
	 * Display error messages
	 * @param xex
	 */
    private void handleException(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Xena Viewer", JOptionPane.ERROR_MESSAGE);
    }

    /**
	 * 
	 *
	 */
    private void exportXenaFile() {
        JFileChooser chooser = new JFileChooser() {

            static final long serialVersionUID = 1L;

            @Override
            public void approveSelection() {
                if (getSelectedFile().exists() && getSelectedFile().isDirectory()) {
                    super.approveSelection();
                } else {
                    JOptionPane.showMessageDialog(this, "Please enter a valid output directory.", "Invalid Output Directory", JOptionPane.WARNING_MESSAGE, IconFactory.getIconByName("images/icons/warning_32.png"));
                }
            }
        };
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle("Choose Export Directory");
        int retVal = chooser.showSaveDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            Xena xena = viewManager.getPluginManager().getXena();
            try {
                this.setCursor(busyCursor);
                xena.export(new XenaInputSource(xenaFile), chooser.getSelectedFile());
                this.setCursor(defaultCursor);
                JOptionPane.showMessageDialog(this, "Xena file exported successfully.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (FileExistsException e) {
                this.setCursor(defaultCursor);
                retVal = JOptionPane.showConfirmDialog(this, "A file with the same name already exists in this directory. Do you want to overwrite it?", "File Already Exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (retVal == JOptionPane.OK_OPTION) {
                    try {
                        this.setCursor(busyCursor);
                        xena.export(new XenaInputSource(xenaFile), chooser.getCurrentDirectory(), true);
                        this.setCursor(defaultCursor);
                    } catch (Exception ex) {
                        handleException(ex);
                    }
                }
            } catch (Exception e) {
                handleException(e);
            } finally {
                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }
}
