package net.sf.jga.swing.spreadsheet;

import static net.sf.jga.fn.property.PropertyFunctors.setProperty;
import static net.sf.jga.fn.property.PropertyFunctors.invokeMethod;
import static net.sf.jga.fn.property.PropertyFunctors.invokeNoArgMethod;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import net.sf.jga.fn.BinaryFunctor;
import net.sf.jga.fn.UnaryFunctor;
import net.sf.jga.fn.adaptor.ApplyBinary;
import net.sf.jga.fn.adaptor.ConstantBinary;
import net.sf.jga.fn.adaptor.Project1st;
import net.sf.jga.fn.adaptor.Project2nd;
import net.sf.jga.fn.property.ArrayBinary;
import net.sf.jga.fn.property.ArrayUnary;
import net.sf.jga.fn.property.SetProperty;
import net.sf.jga.parser.JFXGParser;
import net.sf.jga.swing.GenericAction;

/**
 * An application wrapper for Spreadsheet, providing a main method to
 * allow for standalone use.  
 * <p>
 * Copyright &copy; 2004-2005  David A. Hall
 * @author <a href="mailto:davidahall@users.sf.net">David A. Hall</a>
 */
public class Application {

    private JFrame _frame;

    private Spreadsheet _sheet;

    private JFXGParser _parser;

    private Controller _controller;

    private JFileChooser _chooser;

    public Application() {
        _sheet = makeDefaultSheet();
        _parser = new JFXGParser();
        _parser.bindThis(this);
        _controller = new Controller(_sheet);
        _sheet.setUpdateHandler(new SetProperty<Controller, Boolean>(Controller.class, "SheetDirty", Boolean.TYPE).bind(_controller, Boolean.TRUE));
        createUI(_sheet, _controller);
    }

    public Spreadsheet makeDefaultSheet() {
        Spreadsheet sheet = new Spreadsheet(16, 16);
        sheet.setPreferredScrollableViewportSize(new Dimension(400, 250));
        sheet.setEditableByDefault(true);
        sheet.setRowSelectionInterval(0, 0);
        sheet.setColumnSelectionInterval(0, 0);
        return sheet;
    }

    public final void createUI(final Spreadsheet sheet, Controller controller) {
        final JPopupMenu popupMenu = new JPopupMenu("Popup Menu");
        popupMenu.add(new JMenuItem(controller.getCellRenameCmd()));
        popupMenu.add(new JMenuItem(controller.getCellFormatCmd()));
        popupMenu.add(new JMenuItem(controller.getCellTypeCmd()));
        sheet.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    Spreadsheet sht = (Spreadsheet) e.getComponent();
                    Point p = e.getPoint();
                    int row = sht.rowAtPoint(p);
                    int col = sht.columnAtPoint(p);
                    sht.setRowSelectionInterval(row, row);
                    sht.setColumnSelectionInterval(col, col);
                    if (e.isPopupTrigger()) {
                        popupMenu.show(e.getComponent(), p.x, p.y);
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        JLabel statusLabel = new JLabel("cell(0,0)");
        sheet.setStatusHandler(setProperty(JLabel.class, "Text", String.class).bind1st(statusLabel));
        _frame = new JFrame("Application");
        _frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        _frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                closeWorksheet();
                System.exit(0);
            }
        });
        controller.setLoadFunctor(buildLoadFunctor());
        controller.setSaveFunctor(buildSaveFunctor());
        controller.setErrorFunctor(buildErrorFunctor());
        controller.setPromptFunctor(buildPromptFunctor());
        controller.setConfirmFunctor(buildConfirmFunctor());
        Container rootPane = _frame.getContentPane();
        rootPane.setLayout(new BorderLayout(5, 5));
        JScrollPane pane = new JScrollPane(sheet);
        rootPane.add(pane, BorderLayout.CENTER);
        rootPane.add(statusLabel, BorderLayout.SOUTH);
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(controller.getFileNewCmd());
        fileMenu.add(controller.getFileOpenCmd());
        fileMenu.add(controller.getFileSaveCmd());
        fileMenu.add(controller.getFileSaveAsCmd());
        fileMenu.add(getFileExitCmd());
        JMenu defaultMenu = new JMenu("Default");
        defaultMenu.add(controller.getDefaultEditableCmd());
        defaultMenu.add(controller.getDefaultTypeCmd());
        defaultMenu.add(controller.getDefaultValueCmd());
        JMenu sheetMenu = new JMenu("Worksheet");
        sheetMenu.add(controller.getSheetColumnsCmd());
        sheetMenu.add(controller.getSheetRowsCmd());
        sheetMenu.add(defaultMenu);
        sheetMenu.add(controller.getImportClassCmd());
        JMenu cellMenu = new JMenu("Cell");
        cellMenu.add(controller.getCellRenameCmd());
        JMenuBar menu = new JMenuBar();
        menu.add(fileMenu);
        menu.add(sheetMenu);
        menu.add(cellMenu);
        _frame.setJMenuBar(menu);
        _frame.pack();
        _frame.setVisible(true);
    }

    /**
     * Returns an Action that closes the spreadsheet.
     */
    public Action getFileExitCmd() {
        UnaryFunctor<ActionEvent, ?> fn = new Project2nd<ActionEvent, Object>().generate2nd(invokeNoArgMethod(Application.class, "closeWorksheet").bind(this));
        return new GenericAction(fn, "Exit");
    }

    public int loadFile(Spreadsheet sheet) {
        if (getChooser().showOpenDialog(_frame) != JFileChooser.APPROVE_OPTION) return Controller.CANCEL_OPTION;
        File file = getChosenFile();
        if (file == null) return Controller.CANCEL_OPTION;
        try {
            FileInputStream fis = new FileInputStream(file);
            sheet.readSpreadsheet(fis);
            _controller.setSheetSource(file.toURI().toURL());
            return Controller.YES_OPTION;
        } catch (IOException x) {
            _controller.notify(x.getMessage(), Controller.getExceptionName(x));
            return Controller.CANCEL_OPTION;
        }
    }

    public int saveFile(Spreadsheet sheet, boolean promptForName) {
        URL hint = _controller.getSheetSource();
        if (promptForName || hint == null) if (getChooser().showSaveDialog(_frame) != JFileChooser.APPROVE_OPTION) return Controller.CANCEL_OPTION;
        File file = getChosenFile();
        if (file != null) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                _controller.setSheetSource(file.toURI().toURL());
                _sheet.writeSpreadsheet(fos);
                fos.close();
                _controller.setSheetDirty(false);
                return Controller.YES_OPTION;
            } catch (IOException x) {
                Throwable t = Controller.getRootCause(x);
                _controller.notify(x.getMessage(), Controller.getExceptionName(t));
            }
        }
        return Controller.CANCEL_OPTION;
    }

    public void closeWorksheet() {
        int ans = Controller.YES_OPTION;
        if (_controller.isSheetDirty()) {
            ans = _controller.promptAndSave();
        }
        if (ans != Controller.CANCEL_OPTION) {
            _frame.dispose();
        }
    }

    private JFileChooser getChooser() {
        if (_chooser != null) return _chooser;
        File pwd = new File(".");
        _chooser = new JFileChooser(pwd);
        return _chooser;
    }

    private File getChosenFile() {
        File dir = _chooser.getCurrentDirectory();
        if (dir == null) return null;
        return _chooser.getSelectedFile();
    }

    protected Controller getController() {
        return _controller;
    }

    @SuppressWarnings("unchecked")
    public BinaryFunctor<String, String, String> buildPromptFunctor() {
        BinaryFunctor<JOptionPane, Object[], String> showInput = invokeMethod(JOptionPane.class, "showInputDialog", Component.class, Object.class, Object.class);
        ApplyBinary<String, String> threeArgs = new ApplyBinary<String, String>(new ConstantBinary<String, String, Component>(_frame), new Project1st<String, String>(), new Project2nd<String, String>());
        return showInput.bind1st(null).compose(threeArgs);
    }

    @SuppressWarnings("unchecked")
    public BinaryFunctor<String, String, ?> buildErrorFunctor() {
        BinaryFunctor<JOptionPane, Object[], Integer> showError = invokeMethod(JOptionPane.class, "showMessageDialog", Component.class, Object.class, String.class, Integer.TYPE);
        ApplyBinary<String, String> fourArgs = new ApplyBinary<String, String>(new ConstantBinary<String, String, Component>(_frame), new Project1st<String, String>(), new Project2nd<String, String>(), new ConstantBinary<String, String, Integer>(JOptionPane.ERROR_MESSAGE));
        return showError.bind1st(null).compose(fourArgs);
    }

    @SuppressWarnings("unchecked")
    public BinaryFunctor<String, String, Integer> buildConfirmFunctor() {
        BinaryFunctor<JOptionPane, Object[], Integer> showConfirm = invokeMethod(JOptionPane.class, "showConfirmDialog", Component.class, Object.class, String.class, Integer.TYPE);
        ApplyBinary<String, String> fourArgs = new ApplyBinary<String, String>(new ConstantBinary<String, String, Component>(_frame), new Project1st<String, String>(), new Project2nd<String, String>(), new ConstantBinary<String, String, Integer>(JOptionPane.YES_NO_CANCEL_OPTION));
        return showConfirm.bind1st(null).compose(fourArgs);
    }

    public BinaryFunctor<Spreadsheet, Boolean, Integer> buildSaveFunctor() {
        BinaryFunctor<Application, Object[], Integer> getSave = invokeMethod(Application.class, "saveFile", Spreadsheet.class, Boolean.TYPE);
        return getSave.bind1st(this).compose(new ArrayBinary<Spreadsheet, Boolean>());
    }

    public UnaryFunctor<Spreadsheet, Integer> buildLoadFunctor() {
        BinaryFunctor<Application, Object[], Integer> getLoad = invokeMethod(Application.class, "loadFile", Spreadsheet.class);
        return getLoad.bind1st(this).compose(new ArrayUnary<Spreadsheet>());
    }

    public static void main(String[] args) {
        printStartupHeader();
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception x) {
            System.err.println("Error loading L&F:" + x);
        }
        new Application();
    }

    private static void printStartupHeader() {
        System.out.println("");
        System.out.println("/**");
        System.out.println(" * A Java Hacker's Worksheet");
        System.out.println(" * Copyright (c) 2004-2007  David A. Hall");
        System.out.println(" */");
        System.out.println("");
    }
}
