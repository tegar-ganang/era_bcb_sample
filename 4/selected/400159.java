package solpro;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.TimeZone;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Display;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Group;

public class MainWindowSolpro2 {

    public Shell sShell = null;

    private Display display = null;

    private Composite compositeButtons = null;

    private Button buttonExit = null;

    private Button buttonSaveList = null;

    private Button buttonLoadList = null;

    private Button buttonPDFExport = null;

    private Composite compositePlaylist = null;

    private TabFolder tabFolder = null;

    private Composite composite = null;

    private Composite composite2 = null;

    private TabFolder tabFolder1 = null;

    private Composite composite3 = null;

    private Table tableFullList = null;

    private ArrayList<Track> filterTrackList = new ArrayList<Track>();

    private Table tableBigPlaylist = null;

    private Composite composite4 = null;

    private Table tableDay01 = null;

    private Table tableDay02 = null;

    private Table tableDay03 = null;

    private Table tableDay04 = null;

    private Table tableDay05 = null;

    private Table tableDay06 = null;

    private Table tableDay07 = null;

    private Table tableDay08 = null;

    private Table tableDay09 = null;

    private Table tableDay10 = null;

    private Table tableDay11 = null;

    private Table tableDay12 = null;

    private Table tableDay13 = null;

    private Table tableDay14 = null;

    private Table tableDay15 = null;

    private String strTitle = "(untitled)";

    private boolean isFilter1 = false;

    private int bigListSortDirection = 0;

    final TextTransfer textTransfer = TextTransfer.getInstance();

    Transfer[] types = new Transfer[] { textTransfer };

    private Text filter2 = null;

    private Text filter1 = null;

    private Button button = null;

    private Label label = null;

    private Text text2 = null;

    protected boolean isChange = false;

    private Text bigListTitle = null;

    private Label label1 = null;

    private Button button1 = null;

    private Button button2 = null;

    private Label label2 = null;

    private boolean isPOD = false;

    private File MusicPodDirA = null;

    private File ListPodDir = null;

    private Combo combo = null;

    private Label label3 = null;

    private boolean isBList = false;

    private String driveA = null;

    private String driveB = null;

    private Composite compositeEdit = null;

    private Label label4 = null;

    private File MusicPodDirB = null;

    /**
	 * This method initializes combo	
	 *
	 */
    private void createCombo() {
        combo = new Combo(compositeEdit, SWT.READ_ONLY);
        combo.add(Constants.PROGRAM + Constants.PalylistDay01);
        combo.add(Constants.PROGRAM + Constants.PalylistDay02);
        combo.add(Constants.PROGRAM + Constants.PalylistDay03);
        combo.add(Constants.PROGRAM + Constants.PalylistDay04);
        combo.add(Constants.PROGRAM + Constants.PalylistDay05);
        combo.add(Constants.PROGRAM + Constants.PalylistDay06);
        combo.add(Constants.PROGRAM + Constants.PalylistDay07);
        combo.add(Constants.PROGRAM + Constants.PalylistDay08);
        combo.add(Constants.PROGRAM + Constants.PalylistDay09);
        combo.add(Constants.PROGRAM + Constants.PalylistDay10);
        combo.add(Constants.PROGRAM + Constants.PalylistDay11);
        combo.add(Constants.PROGRAM + Constants.PalylistDay12);
        combo.add(Constants.PROGRAM + Constants.PalylistDay13);
        combo.add(Constants.PROGRAM + Constants.PalylistDay14);
        combo.add(Constants.PROGRAM + Constants.PalylistDay15);
        combo.select(0);
    }

    /**
	 * This method initializes compositeEdit	
	 *
	 */
    private void createCompositeEdit() {
        GridData gridData16 = new GridData();
        gridData16.heightHint = 5;
        GridData gridData10 = new GridData();
        gridData10.horizontalAlignment = GridData.FILL;
        gridData10.grabExcessHorizontalSpace = false;
        gridData10.widthHint = -1;
        gridData10.horizontalSpan = 5;
        gridData10.verticalAlignment = GridData.CENTER;
        GridData gridData14 = new GridData();
        gridData14.widthHint = 70;
        GridData gridData13 = new GridData();
        gridData13.heightHint = -1;
        gridData13.horizontalAlignment = GridData.BEGINNING;
        gridData13.verticalAlignment = GridData.CENTER;
        gridData13.grabExcessHorizontalSpace = false;
        gridData13.widthHint = 70;
        GridLayout gridLayout5 = new GridLayout();
        gridLayout5.numColumns = 5;
        gridLayout5.marginWidth = 5;
        gridLayout5.marginHeight = 0;
        gridLayout5.verticalSpacing = 0;
        gridLayout5.horizontalSpacing = 5;
        compositeEdit = new Composite(composite4, SWT.NONE);
        compositeEdit.setLayout(gridLayout5);
        compositeEdit.setLayoutData(gridData10);
        label1 = new Label(compositeEdit, SWT.NONE);
        label1.setText("Title:");
        bigListTitle = new Text(compositeEdit, SWT.BORDER);
        bigListTitle.addModifyListener(new org.eclipse.swt.events.ModifyListener() {

            public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
                isChange = true;
                isChange();
            }
        });
        Label filler4 = new Label(compositeEdit, SWT.NONE);
        filler4.setLayoutData(gridData14);
        label3 = new Label(compositeEdit, SWT.NONE);
        label3.setText("Save to:");
        label3.setLayoutData(gridData13);
        createCombo();
        label4 = new Label(compositeEdit, SWT.NONE);
        label4.setText("");
        label4.setLayoutData(gridData16);
    }

    /**
	 * This method initializes sShell
	 */
    public static void main(String[] args) {
        Display display = new Display();
        MainWindowSolpro2 a = new MainWindowSolpro2();
        a.createSShell();
        a.sShell.open();
        a.loadPod();
        while (!a.sShell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        display.dispose();
    }

    public void createSShell() {
        GridLayout gridLayout1 = new GridLayout();
        gridLayout1.makeColumnsEqualWidth = true;
        gridLayout1.numColumns = 3;
        sShell = new Shell();
        sShell.setText(Constants.Title2);
        sShell.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/solpro/solisten_logo.ico")));
        setChange(false);
        createCompositePlaylist();
        createComposite3();
        sShell.setLayout(gridLayout1);
        createCompositeButtons();
        sShell.setSize(new Point(933, 625));
    }

    /**
	 * This method initializes compositeButtons
	 * 
	 */
    private void createCompositeButtons() {
        GridData gridData12 = new GridData();
        gridData12.grabExcessHorizontalSpace = true;
        GridData gridData2 = new GridData();
        gridData2.grabExcessVerticalSpace = false;
        gridData2.horizontalAlignment = GridData.FILL;
        gridData2.verticalAlignment = GridData.CENTER;
        gridData2.horizontalSpan = 3;
        gridData2.grabExcessHorizontalSpace = true;
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 9;
        gridLayout.horizontalSpacing = 20;
        compositeButtons = new Composite(sShell, SWT.NONE);
        compositeButtons.setLayout(gridLayout);
        compositeButtons.setLayoutData(gridData2);
        Label filler1 = new Label(compositeButtons, SWT.NONE);
        button1 = new Button(compositeButtons, SWT.NONE);
        button1.setText("New program");
        button1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                MessageBox msgBox = new MessageBox(sShell, SWT.YES | SWT.NO | SWT.CANCEL);
                msgBox.setMessage("The file was not saved! Do you want to save it?");
                if (isChange()) {
                    switch(msgBox.open()) {
                        case SWT.YES:
                            saveFile();
                            break;
                        case SWT.NO:
                            break;
                        case SWT.CANCEL:
                            return;
                    }
                }
                clearTables();
                setChange(false);
            }
        });
        buttonSaveList = new Button(compositeButtons, SWT.NONE);
        buttonSaveList.setText("Save program");
        buttonSaveList.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                try {
                    saveFile();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        buttonLoadList = new Button(compositeButtons, SWT.NONE);
        buttonLoadList.setText("Load program");
        buttonLoadList.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                try {
                    loadFile();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        buttonPDFExport = new Button(compositeButtons, SWT.NONE);
        buttonPDFExport.setText("Export to PDF");
        button2 = new Button(compositeButtons, SWT.NONE);
        button2.setText("Export to SOLISTEN-Player");
        button2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                saveToPod();
            }
        });
        label2 = new Label(compositeButtons, SWT.NONE);
        label2.setText("");
        label2.setLayoutData(gridData12);
        buttonPDFExport.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                savePDF();
            }
        });
        Label filler = new Label(compositeButtons, SWT.NONE);
        filler.setLayoutData(gridData);
        buttonExit = new Button(compositeButtons, SWT.NONE);
        buttonExit.setText("Exit");
        buttonExit.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                doExit();
            }
        });
    }

    /**
	 * This method initializes compositePlaylist
	 * 
	 */
    private void createCompositePlaylist() {
        GridData gridData1 = new GridData();
        gridData1.grabExcessVerticalSpace = true;
        gridData1.horizontalAlignment = GridData.FILL;
        gridData1.verticalAlignment = GridData.FILL;
        gridData1.horizontalSpan = 2;
        gridData1.grabExcessHorizontalSpace = true;
        compositePlaylist = new Composite(sShell, SWT.NONE);
        compositePlaylist.setLayout(new FillLayout());
        createTabFolder();
        compositePlaylist.setLayoutData(gridData1);
    }

    /**
	 * This method initializes tabFolder
	 * 
	 */
    private void createTabFolder() {
        tabFolder = new TabFolder(compositePlaylist, SWT.NONE);
        createComposite2();
        createComposite4();
        text2 = new Text(tabFolder, SWT.BORDER);
        TabItem tabItem3 = new TabItem(tabFolder, SWT.NONE);
        tabItem3.setText("One Playlist");
        tabItem3.setControl(composite2);
        tabItem3.dispose();
        TabItem tabItem2 = new TabItem(tabFolder, SWT.NONE);
        tabItem2.setText("Program for 15 Days");
        tabItem2.setControl(composite4);
    }

    /**
	 * This method initializes composite
	 * 
	 */
    private void createComposite() {
        GridData gridData8 = new GridData();
        gridData8.horizontalSpan = 2;
        GridData gridData7 = new GridData();
        gridData7.widthHint = 10;
        GridData gridData6 = new GridData();
        gridData6.horizontalAlignment = GridData.FILL;
        gridData6.grabExcessHorizontalSpace = true;
        gridData6.grabExcessVerticalSpace = true;
        gridData6.horizontalSpan = 7;
        gridData6.verticalAlignment = GridData.FILL;
        GridLayout gridLayout4 = new GridLayout();
        gridLayout4.numColumns = 7;
        composite = new Composite(tabFolder1, SWT.NONE);
        composite.setLayout(gridLayout4);
        tableFullList = new Table(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableFullList.setHeaderVisible(true);
        tableFullList.setLayoutData(gridData6);
        tableFullList.setLinesVisible(true);
        label = new Label(composite, SWT.NONE);
        label.setText("Selection:");
        filter1 = new Text(composite, SWT.BORDER);
        filter1.setTextLimit(1);
        filter1.setLayoutData(gridData7);
        filter1.addModifyListener(new org.eclipse.swt.events.ModifyListener() {

            public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
                filter1(filter1.getText(), filter2.getText());
                if (filter1.getText().length() == 1) {
                    filter2.setFocus();
                }
            }
        });
        filter2 = new Text(composite, SWT.BORDER);
        filter2.setLayoutData(gridData8);
        filter2.addKeyListener(new org.eclipse.swt.events.KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                if (filter2.getText().length() == 0 && e.keyCode == SWT.BS) {
                    filter1.setFocus();
                }
            }
        });
        filter2.addModifyListener(new org.eclipse.swt.events.ModifyListener() {

            public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
                filter1(filter1.getText(), filter2.getText());
            }
        });
        button = new Button(composite, SWT.NONE);
        button.setText("clear");
        button.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                filter1.setText("");
                filter2.setText("");
            }
        });
        tableFullList.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
            }
        });
        createColumns(tableFullList);
        tableFullList.getColumn(1).addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                sortFullList(true);
            }
        });
        numTable(tableFullList);
        DragSource ds = new DragSource(tableFullList, DND.DROP_MOVE | DND.DROP_COPY);
        ds.setTransfer(new Transfer[] { TextTransfer.getInstance() });
        ds.addDragListener(new DragSourceAdapter() {

            public void dragSetData(DragSourceEvent event) {
                dragTableItemEvent(tableFullList, event, 1, 0, Constants.maximumPalylistItemsBig);
            }

            public void dragFinished(org.eclipse.swt.dnd.DragSourceEvent event) {
                if (isBList) {
                    tableBigPlaylist.select(tableBigPlaylist.getItemCount() - 1);
                    isBList = false;
                }
                setChange(true);
            }
        });
    }

    public void loadPod() {
        DirectoryDialog dialog = new DirectoryDialog(sShell);
        try {
            for (File root : File.listRoots()) {
                String path = root.getPath();
                if (root.exists()) {
                    if (driveA == null) {
                        File fileDriveA = new File(path + Constants.fileDriveA);
                        File MD = new File(path + Constants.MusikFolder);
                        if (fileDriveA.isFile()) {
                            if (MD.isDirectory()) {
                                isPOD = true;
                                MusicPodDirA = MD;
                                ListPodDir = root;
                                driveA = root.getPath();
                                loadMainList(MusicPodDirA, Constants.DriveA);
                            } else {
                                isPOD = true;
                                MusicPodDirA = MD;
                                ListPodDir = root;
                                driveA = root.getPath();
                                MessageBox msgBox = new MessageBox(sShell, SWT.OK);
                                msgBox.setMessage("No music in internal memory!");
                                msgBox.open();
                            }
                        } else {
                        }
                    }
                    if (driveB == null) {
                        File fileDriveB = new File(path + Constants.fileDriveB);
                        File MD = new File(path + Constants.MusikFolder);
                        if (fileDriveB.isFile()) {
                            if (MD.isDirectory() && (MusicPodDirA == null || !MD.equals(MusicPodDirA))) {
                                driveB = root.getPath();
                                MusicPodDirB = MD;
                                loadMainList(MusicPodDirB, Constants.DriveB);
                            } else {
                                driveB = root.getPath();
                                MusicPodDirB = MD;
                                MessageBox msgBox = new MessageBox(sShell, SWT.OK);
                                msgBox.setMessage("No music on memory card!");
                                msgBox.open();
                            }
                        } else {
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (driveA == null) {
            MessageBox msgBox = new MessageBox(sShell, SWT.OK | SWT.CANCEL);
            msgBox.setMessage("The player device was not found! You can not use the programm if there is no player device! Press OK to search for the device again or CANCEL to close the programm!");
            try {
                do {
                    String result = dialog.open();
                    if (result != null) {
                        File D = new File(result);
                        File MD = new File(result + "/" + Constants.MusikFolder);
                        System.out.println(MD.getAbsolutePath());
                        if (D.isDirectory() && MD.isDirectory()) {
                            isPOD = true;
                            MusicPodDirA = MD;
                            ListPodDir = D;
                            loadMainList(MusicPodDirA, Constants.DriveA);
                        }
                    }
                    if (!isPOD && msgBox.open() == SWT.CANCEL) {
                        doExit();
                        return;
                    }
                } while (!isPOD);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadMainList(File musicPodDir, String drive) throws Exception {
        if (musicPodDir != null) {
            FilenameFilter filter = new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return (name.toLowerCase().endsWith(Constants.musicFileEnd) && (!name.startsWith(".")));
                }
            };
            File[] files = musicPodDir.listFiles(filter);
            Track a = null;
            for (int i = 0; i < files.length; i++) {
                a = new Track(files[i].getName().substring(0, files[i].getName().length() - 4), tableFullList, 0, Constants.MainList, drive);
            }
            sortFullList(false);
        }
    }

    /**
	 * This method initializes composite2
	 * 
	 */
    private void createComposite2() {
        GridData gridData9 = new GridData();
        gridData9.widthHint = 100;
        GridLayout gridLayout2 = new GridLayout();
        gridLayout2.numColumns = 4;
        GridData gridData4 = new GridData();
        gridData4.horizontalAlignment = GridData.FILL;
        gridData4.grabExcessVerticalSpace = true;
        gridData4.grabExcessHorizontalSpace = true;
        gridData4.horizontalSpan = 4;
        gridData4.verticalAlignment = GridData.FILL;
        GridData gridData5 = new GridData();
        gridData5.horizontalAlignment = GridData.FILL;
        gridData5.grabExcessHorizontalSpace = true;
        gridData5.grabExcessVerticalSpace = true;
        gridData5.verticalAlignment = GridData.FILL;
        composite2 = new Composite(tabFolder, SWT.NONE);
        composite2.setLayout(gridLayout2);
        tableBigPlaylist = new Table(composite2, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableBigPlaylist.setHeaderVisible(true);
        tableBigPlaylist.setLayoutData(gridData4);
        tableBigPlaylist.setLinesVisible(true);
        createColumns(tableBigPlaylist);
        createDNDListener(tableBigPlaylist, Constants.PalylistBig, Constants.maximumPalylistItemsBig);
    }

    /**
	 * This method initializes tabFolder1
	 * 
	 */
    private void createTabFolder1() {
        tabFolder1 = new TabFolder(composite3, SWT.NONE);
        createComposite();
        TabItem tabItem = new TabItem(tabFolder1, SWT.NONE);
        tabItem.setText("Available Music");
        tabItem.setControl(composite);
    }

    /**
	 * This method initializes composite3
	 * 
	 */
    private void createComposite3() {
        GridData gridData3 = new GridData();
        gridData3.grabExcessHorizontalSpace = true;
        gridData3.horizontalAlignment = GridData.FILL;
        gridData3.verticalAlignment = GridData.FILL;
        gridData3.grabExcessVerticalSpace = true;
        composite3 = new Composite(sShell, SWT.NONE);
        composite3.setLayout(new FillLayout());
        createTabFolder1();
        composite3.setLayoutData(gridData3);
    }

    private void numTable(Table table, int index) {
        for (int i = 0; i < table.getItemCount(); i++) {
            if (!table.getItem(i).isDisposed() || i != index) {
                table.getItem(i).setText(0, String.valueOf(i + 1));
            }
        }
        table.getColumns()[0].pack();
        table.getColumns()[1].pack();
    }

    private void numTable(Table table) {
        table.redraw();
        for (int i = 0; i < table.getItemCount(); i++) {
            if (!table.getItem(i).isDisposed()) {
                table.getItem(i).setText(0, String.valueOf(i + 1));
            }
        }
        table.getColumns()[0].pack();
        table.getColumns()[1].pack();
    }

    /**
	 * This method initializes composite4
	 * 
	 */
    private void createComposite4() {
        GridData gridData11 = new GridData();
        gridData11.grabExcessHorizontalSpace = true;
        gridData11.horizontalAlignment = GridData.FILL;
        gridData11.verticalAlignment = GridData.FILL;
        gridData11.grabExcessVerticalSpace = true;
        GridLayout gridLayout3 = new GridLayout();
        gridLayout3.numColumns = 5;
        composite4 = new Composite(tabFolder, SWT.NONE);
        composite4.setLayout(gridLayout3);
        tableDay01 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay01.setHeaderVisible(true);
        tableDay01.setLayoutData(gridData11);
        tableDay01.setLinesVisible(true);
        createColumns(tableDay01, "Day 1");
        createDNDListener(tableDay01, Constants.PalylistDay01, Constants.maximumPalylistItemsPerDay);
        tableDay02 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay02.setHeaderVisible(true);
        tableDay02.setLayoutData(gridData11);
        tableDay02.setLinesVisible(true);
        createColumns(tableDay02, "Day 2");
        createDNDListener(tableDay02, Constants.PalylistDay02, Constants.maximumPalylistItemsPerDay);
        tableDay03 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay03.setHeaderVisible(true);
        tableDay03.setLayoutData(gridData11);
        tableDay03.setLinesVisible(true);
        createColumns(tableDay03, "Day 3");
        createDNDListener(tableDay03, Constants.PalylistDay03, Constants.maximumPalylistItemsPerDay);
        tableDay04 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay04.setHeaderVisible(true);
        tableDay04.setLayoutData(gridData11);
        tableDay04.setLinesVisible(true);
        createColumns(tableDay04, "Day 4");
        createDNDListener(tableDay04, Constants.PalylistDay04, Constants.maximumPalylistItemsPerDay);
        tableDay05 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay05.setHeaderVisible(true);
        tableDay05.setLayoutData(gridData11);
        tableDay05.setLinesVisible(true);
        createColumns(tableDay05, "Day 5");
        createDNDListener(tableDay05, Constants.PalylistDay05, Constants.maximumPalylistItemsPerDay);
        tableDay06 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay06.setHeaderVisible(true);
        tableDay06.setLayoutData(gridData11);
        tableDay06.setLinesVisible(true);
        createColumns(tableDay06, "Day 6");
        createDNDListener(tableDay06, Constants.PalylistDay06, Constants.maximumPalylistItemsPerDay);
        tableDay07 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay07.setHeaderVisible(true);
        tableDay07.setLayoutData(gridData11);
        tableDay07.setLinesVisible(true);
        createColumns(tableDay07, "Day 7");
        createDNDListener(tableDay07, Constants.PalylistDay07, Constants.maximumPalylistItemsPerDay);
        tableDay08 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay08.setHeaderVisible(true);
        tableDay08.setLayoutData(gridData11);
        tableDay08.setLinesVisible(true);
        createColumns(tableDay08, "Day 8");
        createDNDListener(tableDay08, Constants.PalylistDay08, Constants.maximumPalylistItemsPerDay);
        tableDay09 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay09.setHeaderVisible(true);
        tableDay09.setLayoutData(gridData11);
        tableDay09.setLinesVisible(true);
        createColumns(tableDay09, "Day 9");
        createDNDListener(tableDay09, Constants.PalylistDay09, Constants.maximumPalylistItemsPerDay);
        tableDay10 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay10.setHeaderVisible(true);
        tableDay10.setLayoutData(gridData11);
        tableDay10.setLinesVisible(true);
        createColumns(tableDay10, "Day 10");
        createDNDListener(tableDay10, Constants.PalylistDay10, Constants.maximumPalylistItemsPerDay);
        tableDay11 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay11.setHeaderVisible(true);
        tableDay11.setLayoutData(gridData11);
        tableDay11.setLinesVisible(true);
        createColumns(tableDay11, "Day 11");
        createDNDListener(tableDay11, Constants.PalylistDay11, Constants.maximumPalylistItemsPerDay);
        tableDay12 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay12.setHeaderVisible(true);
        tableDay12.setLayoutData(gridData11);
        tableDay12.setLinesVisible(true);
        createColumns(tableDay12, "Day 12");
        createDNDListener(tableDay12, Constants.PalylistDay12, Constants.maximumPalylistItemsPerDay);
        tableDay13 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay13.setHeaderVisible(true);
        tableDay13.setLayoutData(gridData11);
        tableDay13.setLinesVisible(true);
        createColumns(tableDay13, "Day 13");
        createDNDListener(tableDay13, Constants.PalylistDay13, Constants.maximumPalylistItemsPerDay);
        tableDay14 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay14.setHeaderVisible(true);
        tableDay14.setLayoutData(gridData11);
        tableDay14.setLinesVisible(true);
        createColumns(tableDay14, "Day 14");
        createDNDListener(tableDay14, Constants.PalylistDay14, Constants.maximumPalylistItemsPerDay);
        tableDay15 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay15.setHeaderVisible(true);
        tableDay15.setLayoutData(gridData11);
        tableDay15.setLinesVisible(true);
        createColumns(tableDay15, "Day 15");
        createDNDListener(tableDay15, Constants.PalylistDay15, Constants.maximumPalylistItemsPerDay);
        createCompositeEdit();
    }

    private void dropTableItemEvent(Table table, DropTargetEvent event, int dropStatus, final int listID, final int maxItems) {
        try {
            int aaa = 0;
            event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
            switch(dropStatus) {
                case 1:
                    {
                        String string = (String) event.data;
                        String[] strings = string.split("\n");
                        if (string == null || strings == null || strings.length != Constants.DNDStrLength || !strings[0].equals(Track.TrackName) || table.getItemCount() > (listID == Integer.valueOf(strings[4]) ? maxItems : maxItems - 1)) {
                            event.detail = DND.DROP_NONE;
                            return;
                        }
                        Point p = event.display.map(null, table, event.x, event.y);
                        TableItem dropItem = table.getItem(p);
                        int index = (dropItem == null || table.getItemCount() - 1 == table.indexOf(dropItem)) ? table.getItemCount() : table.indexOf(dropItem);
                        aaa = index;
                        Track track = new Track(strings[2], table, index, listID, strings[5]);
                        if (table == tableBigPlaylist && table.getItemCount() <= index + 2) {
                            isBList = true;
                        }
                        track.setPath(strings[3]);
                    }
                    break;
                case 2:
                    for (int i = 0; i < event.dataTypes.length; i++) {
                        if (!textTransfer.isSupportedType(event.dataTypes[i]) || table.getItemCount() > (maxItems)) {
                            event.detail = DND.DROP_NONE;
                        }
                    }
                    break;
                default:
                    break;
            }
            numTable(table);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dragTableItemEvent(Table table, DragSourceEvent event, int dragStatus, final int listID, final int maxItems) {
        try {
            switch(dragStatus) {
                case 1:
                    if (table.getSelection()[0].getData() != null) {
                        event.data = ((Track) table.getSelection()[0].getData()).toString();
                    } else {
                        event.data = "null";
                    }
                    break;
                case 2:
                    if (event.detail == DND.DROP_MOVE || table.getItemCount() > maxItems) {
                        table.getSelection()[0].dispose();
                    }
                    tableBigPlaylist.setFocus();
                    if (isBList) {
                        isBList = false;
                    }
                    break;
                default:
                    break;
            }
            numTable(table);
        } catch (Exception e) {
        }
    }

    private void createColumns(Table table, String Name) {
        TableColumn tableColumn2 = new TableColumn(table, SWT.NONE);
        tableColumn2.setWidth(60);
        tableColumn2.pack();
        TableColumn tableColumn3 = new TableColumn(table, SWT.NONE);
        tableColumn3.setWidth(60);
        tableColumn3.setText(Name);
    }

    private void createColumns(Table table) {
        TableColumn tableColumn2 = new TableColumn(table, SWT.NONE);
        tableColumn2.setWidth(60);
        tableColumn2.setText("#");
        tableColumn2.pack();
        TableColumn tableColumn3 = new TableColumn(table, SWT.NONE);
        tableColumn3.setWidth(60);
        tableColumn3.setText("Name");
    }

    private void createDNDListener(final Table table, final int listID, final int maxItems) {
        DragSource ds1 = new DragSource(table, DND.DROP_MOVE | DND.DROP_COPY);
        ds1.setTransfer(new Transfer[] { TextTransfer.getInstance() });
        ds1.addDragListener(new DragSourceAdapter() {

            public void dragSetData(DragSourceEvent event) {
                dragTableItemEvent(table, event, 1, listID, maxItems);
            }

            public void dragFinished(DragSourceEvent event) {
                dragTableItemEvent(table, event, 2, listID, maxItems);
                setChange(true);
            }
        });
        DropTarget ddt = new DropTarget(table, DND.DROP_MOVE | DND.DROP_COPY);
        ddt.setTransfer(types);
        ddt.setTransfer(new Transfer[] { TextTransfer.getInstance() });
        ddt.addDropListener(new DropTargetAdapter() {

            public void drop(DropTargetEvent event) {
                dropTableItemEvent(table, event, 1, listID, maxItems);
            }

            public void dragEnter(DropTargetEvent event) {
                dropTableItemEvent(table, event, 2, listID, maxItems);
            }

            public void dragOver(DropTargetEvent event) {
                event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
            }
        });
        table.addKeyListener(new org.eclipse.swt.events.KeyAdapter() {

            public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
                if (e.character == SWT.DEL && table.getSelection().length != 0) {
                    int index = table.getSelectionIndex();
                    table.getSelection()[0].dispose();
                    table.select(index - 1 == -1 ? 0 : index - 1);
                }
            }
        });
    }

    private void filter1(String FilterString1, String FilterString2) {
        ListIterator it = filterTrackList.listIterator(filterTrackList.size());
        while (it.hasPrevious()) ((Track) it.previous()).setNewTableItemb(tableFullList, 0);
        filterTrackList.clear();
        sortFullList(false);
        tableFullList.redraw();
        int[] ii = {};
        int length = tableFullList.getItems().length;
        for (int i = 0; i < length; i++) {
            String f = tableFullList.getItem(i).getText(1);
            if ((f.length() > 0 && f.length() > FilterString2.length() && ((FilterString1.length() == 0 ? true : (f.substring(0, 1).equalsIgnoreCase(FilterString1))) && (FilterString2.length() == 0 ? true : (f.substring(1, FilterString2.length() + 1).equalsIgnoreCase(FilterString2)))))) {
            } else {
                int[] ii3 = new int[ii.length + 1];
                System.arraycopy(ii, 0, ii3, 0, ii.length);
                ii3[ii.length] = i;
                ii = ii3;
                filterTrackList.add((Track) tableFullList.getItem(i).getData());
            }
        }
        tableFullList.remove(ii);
        tableFullList.redraw();
    }

    private void sortFullList(boolean changeSort) {
        if (changeSort) {
            if (bigListSortDirection < 0) bigListSortDirection = 1; else bigListSortDirection = -1;
        }
        TableItem[] items = tableFullList.getItems();
        Collator collator = Collator.getInstance(Locale.getDefault());
        for (int i = 1; i < items.length; i++) {
            String value1 = items[i].getText(1);
            for (int j = 0; j < i; j++) {
                String value2 = items[j].getText(1);
                if ((collator.compare(value1, value2) > 0 && bigListSortDirection > 0) || (collator.compare(value1, value2) < 0 && bigListSortDirection <= 0)) {
                    Object a = items[i].getData();
                    String[] values = { items[i].getText(0), items[i].getText(1) };
                    items[i].dispose();
                    TableItem item = new TableItem(tableFullList, SWT.NONE, j);
                    item.setText(values);
                    item.setData(a);
                    items = tableFullList.getItems();
                    break;
                }
            }
        }
        numTable(tableFullList);
    }

    private void wirteToFile(File f) throws Exception, Exception {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        Writer a = new StringWriter();
        XMLStreamWriter writer = factory.createXMLStreamWriter(new FileOutputStream(f));
        writer.writeStartDocument();
        writer.writeStartElement(Constants.XMLRootElement);
        SimpleDateFormat date = new SimpleDateFormat();
        date.setTimeZone(TimeZone.getDefault());
        String filename = "d";
        writer.writeAttribute(Constants.XMLRootElementName, filename);
        fileItemInput(tableDay01, writer, Constants.PalylistDay01);
        fileItemInput(tableDay02, writer, Constants.PalylistDay02);
        fileItemInput(tableDay03, writer, Constants.PalylistDay03);
        fileItemInput(tableDay04, writer, Constants.PalylistDay04);
        fileItemInput(tableDay05, writer, Constants.PalylistDay05);
        fileItemInput(tableDay06, writer, Constants.PalylistDay06);
        fileItemInput(tableDay07, writer, Constants.PalylistDay07);
        fileItemInput(tableDay08, writer, Constants.PalylistDay08);
        fileItemInput(tableDay09, writer, Constants.PalylistDay09);
        fileItemInput(tableDay10, writer, Constants.PalylistDay10);
        fileItemInput(tableDay11, writer, Constants.PalylistDay11);
        fileItemInput(tableDay12, writer, Constants.PalylistDay12);
        fileItemInput(tableDay13, writer, Constants.PalylistDay13);
        fileItemInput(tableDay14, writer, Constants.PalylistDay14);
        fileItemInput(tableDay15, writer, Constants.PalylistDay15);
        fileItemInput(tableBigPlaylist, writer, Constants.PalylistBig);
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
    }

    private boolean readFromFile(File f) {
        Document doc;
        try {
            doc = new SAXBuilder().build(f);
            Element playliste = doc.getRootElement();
            if (playliste != null) {
                clearTables();
                Iterator playlisten = playliste.getChildren(Constants.XMLPlaylisteElementNameValue).iterator();
                while (playlisten.hasNext()) {
                    Element liste = (Element) playlisten.next();
                    switch(Integer.valueOf(liste.getAttribute(Constants.XMLTrackItemElementName).getValue())) {
                        case Constants.PalylistDay01:
                            fillTable(tableDay01, Constants.PalylistDay01, liste);
                            break;
                        case Constants.PalylistDay02:
                            fillTable(tableDay02, Constants.PalylistDay01, liste);
                            break;
                        case Constants.PalylistDay03:
                            fillTable(tableDay03, Constants.PalylistDay03, liste);
                            break;
                        case Constants.PalylistDay04:
                            fillTable(tableDay04, Constants.PalylistDay04, liste);
                            break;
                        case Constants.PalylistDay05:
                            fillTable(tableDay05, Constants.PalylistDay05, liste);
                            break;
                        case Constants.PalylistDay06:
                            fillTable(tableDay06, Constants.PalylistDay06, liste);
                            break;
                        case Constants.PalylistDay07:
                            fillTable(tableDay07, Constants.PalylistDay07, liste);
                            break;
                        case Constants.PalylistDay08:
                            fillTable(tableDay08, Constants.PalylistDay08, liste);
                            break;
                        case Constants.PalylistDay09:
                            fillTable(tableDay09, Constants.PalylistDay09, liste);
                            break;
                        case Constants.PalylistDay10:
                            fillTable(tableDay10, Constants.PalylistDay10, liste);
                            break;
                        case Constants.PalylistDay11:
                            fillTable(tableDay11, Constants.PalylistDay11, liste);
                            break;
                        case Constants.PalylistDay12:
                            fillTable(tableDay12, Constants.PalylistDay12, liste);
                            break;
                        case Constants.PalylistDay13:
                            fillTable(tableDay13, Constants.PalylistDay13, liste);
                            break;
                        case Constants.PalylistDay14:
                            fillTable(tableDay14, Constants.PalylistDay14, liste);
                            break;
                        case Constants.PalylistDay15:
                            fillTable(tableDay15, Constants.PalylistDay15, liste);
                            break;
                        case Constants.PalylistBig:
                            bigListTitle.setText(liste.getAttributeValue(Constantsv1.XMLPlaylisteElementTitle));
                            try {
                                combo.select(Integer.valueOf(liste.getAttributeValue(Constants.XMLPlaylisteElementExport)));
                            } catch (Exception e) {
                                combo.select(0);
                            }
                            break;
                        default:
                            break;
                    }
                }
                strTitle = f.getName();
                setChange(false);
                return true;
            } else {
                return false;
            }
        } catch (JDOMException e) {
            MessageBox msgBox = new MessageBox(sShell, SWT.OK);
            msgBox.setMessage("Can not read file! The file is invalid.");
            msgBox.open();
            return false;
        } catch (IOException e) {
            MessageBox msgBox = new MessageBox(sShell, SWT.OK);
            msgBox.setMessage("Can not read file! The file does not exist.");
            msgBox.open();
            return false;
        }
    }

    private void fillTable(Table table, int tableIndex, Element liste) {
        Iterator XMLItems = liste.getChildren(Constants.XMLTrackItemElementNameValue).iterator();
        while (XMLItems.hasNext()) {
            Element XMLItem = (Element) XMLItems.next();
            Track a = new Track(XMLItem.getAttributeValue(Constants.XMLTrackItemElementName), table, Integer.valueOf(XMLItem.getAttributeValue(Constants.XMLTrackItemElementIDName)), tableIndex, String.valueOf(XMLItem.getAttributeValue(Constants.XMLTrackItemElementDrive)));
        }
        numTable(table);
    }

    private void fileItemInput(Table table, XMLStreamWriter writer, int Playlistenname) throws Exception {
        writer.writeStartElement(Constants.XMLPlaylisteElementNameValue);
        writer.writeAttribute(Constants.XMLPlaylisteElementName, String.valueOf(Playlistenname));
        if (Playlistenname == Constants.PalylistBig) {
            writer.writeAttribute(Constants.XMLPlaylisteElementTitle, bigListTitle.getText());
            writer.writeAttribute(Constants.XMLPlaylisteElementExport, String.valueOf(combo.getSelectionIndex()));
        } else for (int i = 0; i < table.getItemCount(); i++) {
            if (!table.getItem(i).isDisposed()) {
                String itmeName = ((Track) table.getItem(i).getData()).getTitle();
                String itmeDrive = ((Track) table.getItem(i).getData()).getDrive();
                writer.writeStartElement(Constants.XMLTrackItemElementNameValue);
                writer.writeAttribute(Constants.XMLTrackItemElementName, itmeName);
                writer.writeAttribute(Constants.XMLTrackItemElementIDName, String.valueOf(i));
                writer.writeAttribute(Constants.XMLTrackItemElementDrive, itmeDrive);
                writer.writeCharacters(itmeName);
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void clearTables() {
        tableDay01.removeAll();
        tableDay02.removeAll();
        tableDay03.removeAll();
        tableDay04.removeAll();
        tableDay05.removeAll();
        tableDay06.removeAll();
        tableDay07.removeAll();
        tableDay08.removeAll();
        tableDay09.removeAll();
        tableDay10.removeAll();
        tableDay11.removeAll();
        tableDay12.removeAll();
        tableDay13.removeAll();
        tableDay14.removeAll();
        tableDay15.removeAll();
        tableBigPlaylist.removeAll();
        bigListTitle.setText("");
        combo.select(0);
    }

    private void loadFile() {
        MessageBox msgBox = new MessageBox(sShell, SWT.YES | SWT.NO);
        msgBox.setMessage("The file was not saved! Do you want to save it?");
        if (isChange() && msgBox.open() == SWT.YES) {
            saveFile();
        }
        FileDialog dialog = new FileDialog(sShell, SWT.OPEN);
        dialog.setText("Load file");
        String[] filterExt = { ("*." + Constants.FileEnd) };
        dialog.setFilterExtensions(filterExt);
        String result = dialog.open();
        if (result != null) {
            File f = new File(result);
            readFromFile(f);
        }
    }

    private void saveFile() {
        FileDialog dialog = new FileDialog(sShell, SWT.SAVE);
        dialog.setText("Save file");
        dialog.setFileName(bigListTitle.getText());
        String[] filterExt = { ("*." + Constants.FileEnd) };
        dialog.setFilterExtensions(filterExt);
        String result = dialog.open();
        if (result != null) {
            if (!result.toLowerCase().endsWith("." + Constants.FileEnd)) {
                result = result + "." + Constants.FileEnd;
            } else if (result.toLowerCase().endsWith(".")) result = result + Constants.FileEnd;
            File f = new File(result);
            MessageBox msgBox = new MessageBox(sShell, SWT.YES | SWT.NO);
            msgBox.setMessage("The file already exists! Do you want to overwrite it?");
            try {
                if (!f.isFile() || msgBox.open() == SWT.YES) {
                    wirteToFile(f);
                    strTitle = f.getName();
                    setChange(false);
                } else {
                    saveFile();
                }
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isChange() {
        sShell.setText(Constants.Title2 + " " + strTitle + " *");
        return isChange;
    }

    public void setChange(boolean isChange) {
        if (isChange) sShell.setText(Constants.Title2 + " " + strTitle + " *"); else sShell.setText(Constants.Title2 + " " + strTitle + " ");
        this.isChange = isChange;
    }

    private void savePDF() {
        MessageBox msgBox = new MessageBox(sShell, SWT.OK);
        if (bigListTitle.getCharCount() == 0) {
            msgBox.setMessage("The playlist needs a title!");
            msgBox.open();
            return;
        }
        FileDialog dialog = new FileDialog(sShell, SWT.SAVE);
        dialog.setText("Save PDF file");
        String[] filterExt = { ("*." + Constants.PDFFileEnd) };
        dialog.setFilterExtensions(filterExt);
        dialog.setFileName(bigListTitle.getText());
        String result = dialog.open();
        if (result != null) {
            if (!result.toLowerCase().endsWith("." + Constants.PDFFileEnd)) {
                result = result + "." + Constants.PDFFileEnd;
            } else if (result.toLowerCase().endsWith(".")) result = result + Constants.PDFFileEnd;
            File f = new File(result);
            msgBox = new MessageBox(sShell, SWT.YES | SWT.NO);
            msgBox.setMessage("The file already exists! Do you want to overwrite it?");
            try {
                if (!f.isFile() || msgBox.open() == SWT.YES) {
                    writeToPdf(f);
                } else {
                    savePDF();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeToPdf(File f) {
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(f));
            document.open();
            if ((tableDay01.getItemCount() != 0 || tableDay02.getItemCount() != 0 || tableDay03.getItemCount() != 0 || tableDay04.getItemCount() != 0 || tableDay05.getItemCount() != 0 || tableDay06.getItemCount() != 0 || tableDay07.getItemCount() != 0 || tableDay08.getItemCount() != 0 || tableDay09.getItemCount() != 0 || tableDay10.getItemCount() != 0 || tableDay11.getItemCount() != 0 || tableDay12.getItemCount() != 0 || tableDay13.getItemCount() != 0 || tableDay14.getItemCount() != 0 || tableDay15.getItemCount() != 0)) {
                Paragraph H1 = new Paragraph(Constants.TitleA, FontFactory.getFont(FontFactory.HELVETICA, 16));
                document.add(H1);
                Paragraph H2 = new Paragraph(Constants.ProgrammFor + bigListTitle.getText(), FontFactory.getFont(FontFactory.HELVETICA, 16));
                document.add(H2);
                pdfItemInput(tableDay01, document, Constants.PalylistDay01);
                pdfItemInput(tableDay02, document, Constants.PalylistDay02);
                pdfItemInput(tableDay03, document, Constants.PalylistDay03);
                pdfItemInput(tableDay04, document, Constants.PalylistDay04);
                pdfItemInput(tableDay05, document, Constants.PalylistDay05);
                pdfItemInput(tableDay06, document, Constants.PalylistDay06);
                pdfItemInput(tableDay07, document, Constants.PalylistDay07);
                pdfItemInput(tableDay08, document, Constants.PalylistDay08);
                pdfItemInput(tableDay09, document, Constants.PalylistDay09);
                pdfItemInput(tableDay10, document, Constants.PalylistDay10);
                pdfItemInput(tableDay11, document, Constants.PalylistDay11);
                pdfItemInput(tableDay12, document, Constants.PalylistDay12);
                pdfItemInput(tableDay13, document, Constants.PalylistDay13);
                pdfItemInput(tableDay14, document, Constants.PalylistDay14);
                pdfItemInput(tableDay15, document, Constants.PalylistDay15);
            }
            document.close();
        } catch (Exception e) {
        }
    }

    private void pdfItemInput(Table table, com.lowagie.text.Document document, int Playlistenname) throws Exception {
        if ((table.getItemCount() != 0)) {
            Paragraph pdfHead = new Paragraph("    " + Constants.TablesTitle[Playlistenname], FontFactory.getFont(FontFactory.HELVETICA, 15));
            pdfHead.setSpacingBefore(20);
            document.add(pdfHead);
            com.lowagie.text.List list = new com.lowagie.text.List(true, 17);
            list.setNumbered(true);
            list.setIndentationLeft(50);
            list.setPostSymbol(")");
            for (int i = 0; i < table.getItemCount(); i++) {
                if (!table.getItem(i).isDisposed()) {
                    String itmeName = ((Track) table.getItem(i).getData()).getTitle();
                    list.add(new com.lowagie.text.ListItem(itmeName));
                }
            }
            document.add(list);
        }
    }

    private void saveToPod() {
        try {
            MessageBox msgBox = new MessageBox(sShell, SWT.OK);
            msgBox.setMessage("The file already exists! Do you want to overwrite it?");
            if (!ListPodDir.isDirectory()) {
                msgBox.setMessage("There is a problem with the player device! Please check the player device and restart the programm!");
                msgBox.open();
                return;
            }
            File program = new File(ListPodDir.getAbsolutePath() + ((ListPodDir.getAbsolutePath().charAt(ListPodDir.getAbsolutePath().length() - 1)) == '/' ? "" : "/") + combo.getText());
            if ((tableDay01.getItemCount() == 0 && tableDay02.getItemCount() == 0 && tableDay03.getItemCount() == 0 && tableDay04.getItemCount() == 0 && tableDay05.getItemCount() == 0 && tableDay06.getItemCount() == 0 && tableDay07.getItemCount() == 0 && tableDay08.getItemCount() == 0 && tableDay09.getItemCount() == 0 && tableDay10.getItemCount() == 0 && tableDay11.getItemCount() == 0 && tableDay12.getItemCount() == 0 && tableDay13.getItemCount() == 0 && tableDay14.getItemCount() == 0 && tableDay15.getItemCount() == 0 && !program.isDirectory())) {
                MessageBox msgBox3 = new MessageBox(sShell, SWT.OK);
                msgBox3.setMessage("All days are empty!");
                msgBox3.open();
                return;
            }
            if (program.isDirectory()) {
                MessageBox msgBox2 = new MessageBox(sShell, SWT.OK | SWT.CANCEL);
                if ((tableDay01.getItemCount() == 0 && tableDay02.getItemCount() == 0 && tableDay03.getItemCount() == 0 && tableDay04.getItemCount() == 0 && tableDay05.getItemCount() == 0 && tableDay06.getItemCount() == 0 && tableDay07.getItemCount() == 0 && tableDay08.getItemCount() == 0 && tableDay09.getItemCount() == 0 && tableDay10.getItemCount() == 0 && tableDay11.getItemCount() == 0 && tableDay12.getItemCount() == 0 && tableDay13.getItemCount() == 0 && tableDay14.getItemCount() == 0 && tableDay15.getItemCount() == 0)) {
                    msgBox2.setMessage("The program on the player already exists! Delete?");
                    if (msgBox2.open() != SWT.OK) return;
                    nukeSubFolder(program);
                    return;
                } else {
                    msgBox2.setMessage("The program on the player already exists! Overwrite?");
                    if (msgBox2.open() != SWT.OK) return;
                }
                nukeSubFolder(program);
            }
            program.mkdirs();
            makePodList(Constants.DAY + Constants.PalylistDay01, program, tableDay01);
            makePodList(Constants.DAY + Constants.PalylistDay02, program, tableDay02);
            makePodList(Constants.DAY + Constants.PalylistDay03, program, tableDay03);
            makePodList(Constants.DAY + Constants.PalylistDay04, program, tableDay04);
            makePodList(Constants.DAY + Constants.PalylistDay05, program, tableDay05);
            makePodList(Constants.DAY + Constants.PalylistDay06, program, tableDay06);
            makePodList(Constants.DAY + Constants.PalylistDay07, program, tableDay07);
            makePodList(Constants.DAY + Constants.PalylistDay08, program, tableDay08);
            makePodList(Constants.DAY + Constants.PalylistDay09, program, tableDay09);
            makePodList(Constants.DAY + Constants.PalylistDay10, program, tableDay10);
            makePodList(Constants.DAY + Constants.PalylistDay11, program, tableDay11);
            makePodList(Constants.DAY + Constants.PalylistDay12, program, tableDay12);
            makePodList(Constants.DAY + Constants.PalylistDay13, program, tableDay13);
            makePodList(Constants.DAY + Constants.PalylistDay14, program, tableDay14);
            makePodList(Constants.DAY + Constants.PalylistDay15, program, tableDay15);
            msgBox.setMessage("Export to player complete!");
            msgBox.open();
        } catch (Exception e) {
            MessageBox msgBox = new MessageBox(sShell, SWT.OK);
            msgBox.setMessage("Error on export!");
            msgBox.open();
        }
    }

    private void makePodList(String listName, File program, Table table) {
        try {
            if (table.getItemCount() == 0) return;
            new File(program.getPath() + "/" + listName).mkdirs();
            File iniFile = new File(program.getPath() + "/" + listName + "/" + Constants.playlistFileName);
            if (iniFile.isFile()) {
                iniFile.delete();
            }
            if (table.getItemCount() != 0) {
                PrintWriter out;
                out = new PrintWriter(iniFile);
                for (int i = 0; i < table.getItemCount(); i++) {
                    Track track = (Track) (table.getItem(i).getData());
                    String drive = ((track.getDrive().equals(Constants.DriveB)) ? Constants.podPlaylistFolderB : Constants.podPlaylistFolderA);
                    out.print(drive + Constants.MusikFolderOnPlayer + table.getItem(i).getText(1) + Constants.SoundfileEnd + "\n");
                }
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void nukeSubFolder(File path) throws IOException {
        File[] files = path.listFiles();
        for (int nIndex = 0; nIndex < files.length; ++nIndex) {
            if (files[nIndex].isDirectory()) {
                nukeSubFolder(files[nIndex]);
            }
            files[nIndex].delete();
        }
        path.delete();
    }

    private boolean doExit() {
        MessageBox msgBox = new MessageBox(sShell, SWT.YES | SWT.NO);
        msgBox.setMessage("The file was not saved! Do you want to save it?");
        if (isChange() && msgBox.open() == SWT.YES) {
            saveFile();
        }
        sShell.close();
        sShell.dispose();
        return true;
    }
}
