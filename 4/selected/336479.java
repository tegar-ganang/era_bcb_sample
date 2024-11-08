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

public class MainWindowSolpro1_alt {

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

    private ArrayList<Trackv1> filterTrackList = new ArrayList<Trackv1>();

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

    private File MusicPodDir = null;

    private File ListPodDir = null;

    private Combo combo = null;

    private Label label3 = null;

    private boolean isBList = false;

    /**
	 * This method initializes combo	
	 *
	 */
    private void createCombo() {
        combo = new Combo(composite2, SWT.READ_ONLY);
        combo.add("playlist1");
        combo.add("playlist2");
        combo.add("playlist3");
        combo.add("playlist4");
        combo.add("playlist5");
        combo.add("playlist6");
        combo.add("playlist7");
        combo.add("playlist8");
        combo.add("playlist9");
        combo.add("playlist10");
        combo.add("playlist11");
        combo.add("playlist12");
        combo.add("playlist13");
        combo.add("playlist14");
        combo.add("playlist15");
        combo.select(0);
    }

    /**
	 * This method initializes sShell
	 */
    public static void main(String[] args) {
        Display display = new Display();
        MainWindowSolpro1_alt a = new MainWindowSolpro1_alt();
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
        sShell.setText(Constantsv1.Title);
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
        button1.setText("New List");
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
        buttonSaveList.setText("Save List");
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
        buttonLoadList.setText("Load List");
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
        TabItem tabItem2 = new TabItem(tabFolder, SWT.NONE);
        tabItem2.setText("Playlists for 15 Days");
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
                dragTableItemEvent(tableFullList, event, 1, 0, Constantsv1.maximumPalylistItemsBig);
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
        MessageBox msgBox = new MessageBox(sShell, SWT.OK | SWT.CANCEL);
        msgBox.setMessage("The player device was not found! You can not use the programm if there is no player device! Press OK to search for the device again or CANCEL to close the programm!");
        try {
            do {
                String result = dialog.open();
                if (result != null) {
                    File D = new File(result);
                    File MD = new File(result + Constantsv1.MusikFolder);
                    File PD = new File(result + Constantsv1.PlaylistFolder);
                    if (D.isDirectory() && PD.isDirectory() && PD.isDirectory()) {
                        isPOD = true;
                        MusicPodDir = MD;
                        ListPodDir = PD;
                        loadMainList();
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

    private void loadMainList() throws Exception {
        if (MusicPodDir != null) {
            FilenameFilter filter = new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return (name.toLowerCase().endsWith(Constantsv1.musicFileEnd) && (!name.startsWith(".")));
                }
            };
            File[] files = MusicPodDir.listFiles(filter);
            Trackv1 a = null;
            for (int i = 0; i < files.length; i++) {
                a = new Trackv1(files[i].getName().substring(0, files[i].getName().length() - 4), tableFullList, 0, Constantsv1.MainList);
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
        label1 = new Label(composite2, SWT.NONE);
        label1.setText("Title:");
        bigListTitle = new Text(composite2, SWT.BORDER);
        bigListTitle.setLayoutData(gridData9);
        label3 = new Label(composite2, SWT.NONE);
        label3.setText("Save to:");
        createCombo();
        createColumns(tableBigPlaylist);
        createDNDListener(tableBigPlaylist, Constantsv1.PalylistBig, Constantsv1.maximumPalylistItemsBig);
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
        createDNDListener(tableDay01, Constantsv1.PalylistDay01, Constantsv1.maximumPalylistItemsPerDay);
        tableDay02 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay02.setHeaderVisible(true);
        tableDay02.setLayoutData(gridData11);
        tableDay02.setLinesVisible(true);
        createColumns(tableDay02, "Day 2");
        createDNDListener(tableDay02, Constantsv1.PalylistDay02, Constantsv1.maximumPalylistItemsPerDay);
        tableDay03 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay03.setHeaderVisible(true);
        tableDay03.setLayoutData(gridData11);
        tableDay03.setLinesVisible(true);
        createColumns(tableDay03, "Day 3");
        createDNDListener(tableDay03, Constantsv1.PalylistDay03, Constantsv1.maximumPalylistItemsPerDay);
        tableDay04 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay04.setHeaderVisible(true);
        tableDay04.setLayoutData(gridData11);
        tableDay04.setLinesVisible(true);
        createColumns(tableDay04, "Day 4");
        createDNDListener(tableDay04, Constantsv1.PalylistDay04, Constantsv1.maximumPalylistItemsPerDay);
        tableDay05 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay05.setHeaderVisible(true);
        tableDay05.setLayoutData(gridData11);
        tableDay05.setLinesVisible(true);
        createColumns(tableDay05, "Day 5");
        createDNDListener(tableDay05, Constantsv1.PalylistDay05, Constantsv1.maximumPalylistItemsPerDay);
        tableDay06 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay06.setHeaderVisible(true);
        tableDay06.setLayoutData(gridData11);
        tableDay06.setLinesVisible(true);
        createColumns(tableDay06, "Day 6");
        createDNDListener(tableDay06, Constantsv1.PalylistDay06, Constantsv1.maximumPalylistItemsPerDay);
        tableDay07 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay07.setHeaderVisible(true);
        tableDay07.setLayoutData(gridData11);
        tableDay07.setLinesVisible(true);
        createColumns(tableDay07, "Day 7");
        createDNDListener(tableDay07, Constantsv1.PalylistDay07, Constantsv1.maximumPalylistItemsPerDay);
        tableDay08 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay08.setHeaderVisible(true);
        tableDay08.setLayoutData(gridData11);
        tableDay08.setLinesVisible(true);
        createColumns(tableDay08, "Day 8");
        createDNDListener(tableDay08, Constantsv1.PalylistDay08, Constantsv1.maximumPalylistItemsPerDay);
        tableDay09 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay09.setHeaderVisible(true);
        tableDay09.setLayoutData(gridData11);
        tableDay09.setLinesVisible(true);
        createColumns(tableDay09, "Day 9");
        createDNDListener(tableDay09, Constantsv1.PalylistDay09, Constantsv1.maximumPalylistItemsPerDay);
        tableDay10 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay10.setHeaderVisible(true);
        tableDay10.setLayoutData(gridData11);
        tableDay10.setLinesVisible(true);
        createColumns(tableDay10, "Day 10");
        createDNDListener(tableDay10, Constantsv1.PalylistDay10, Constantsv1.maximumPalylistItemsPerDay);
        tableDay11 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay11.setHeaderVisible(true);
        tableDay11.setLayoutData(gridData11);
        tableDay11.setLinesVisible(true);
        createColumns(tableDay11, "Day 11");
        createDNDListener(tableDay11, Constantsv1.PalylistDay11, Constantsv1.maximumPalylistItemsPerDay);
        tableDay12 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay12.setHeaderVisible(true);
        tableDay12.setLayoutData(gridData11);
        tableDay12.setLinesVisible(true);
        createColumns(tableDay12, "Day 12");
        createDNDListener(tableDay12, Constantsv1.PalylistDay12, Constantsv1.maximumPalylistItemsPerDay);
        tableDay13 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay13.setHeaderVisible(true);
        tableDay13.setLayoutData(gridData11);
        tableDay13.setLinesVisible(true);
        createColumns(tableDay13, "Day 13");
        createDNDListener(tableDay13, Constantsv1.PalylistDay13, Constantsv1.maximumPalylistItemsPerDay);
        tableDay14 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay14.setHeaderVisible(true);
        tableDay14.setLayoutData(gridData11);
        tableDay14.setLinesVisible(true);
        createColumns(tableDay14, "Day 14");
        createDNDListener(tableDay14, Constantsv1.PalylistDay14, Constantsv1.maximumPalylistItemsPerDay);
        tableDay15 = new Table(composite4, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        tableDay15.setHeaderVisible(true);
        tableDay15.setLayoutData(gridData11);
        tableDay15.setLinesVisible(true);
        createColumns(tableDay15, "Day 15");
        createDNDListener(tableDay15, Constantsv1.PalylistDay15, Constantsv1.maximumPalylistItemsPerDay);
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
                        if (string == null || strings == null || strings.length != Constantsv1.DNDStrLength || !strings[0].equals(Trackv1.TrackName) || table.getItemCount() > (listID == Integer.valueOf(strings[4]) ? maxItems : maxItems - 1)) {
                            event.detail = DND.DROP_NONE;
                            return;
                        }
                        Point p = event.display.map(null, table, event.x, event.y);
                        TableItem dropItem = table.getItem(p);
                        int index = (dropItem == null || table.getItemCount() - 1 == table.indexOf(dropItem)) ? table.getItemCount() : table.indexOf(dropItem);
                        aaa = index;
                        Trackv1 track = new Trackv1(strings[2], table, index, listID);
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
            System.out.println("drop exception");
        }
    }

    private void dragTableItemEvent(Table table, DragSourceEvent event, int dragStatus, final int listID, final int maxItems) {
        try {
            switch(dragStatus) {
                case 1:
                    if (table.getSelection()[0].getData() != null) {
                        event.data = ((Trackv1) table.getSelection()[0].getData()).toString();
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
        while (it.hasPrevious()) ((Trackv1) it.previous()).setNewTableItemb(tableFullList, 0);
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
                filterTrackList.add((Trackv1) tableFullList.getItem(i).getData());
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
        writer.writeStartElement(Constantsv1.XMLRootElement);
        SimpleDateFormat date = new SimpleDateFormat();
        date.setTimeZone(TimeZone.getDefault());
        String filename = "d";
        writer.writeAttribute(Constantsv1.XMLRootElementName, filename);
        fileItemInput(tableDay01, writer, Constantsv1.PalylistDay01);
        fileItemInput(tableDay02, writer, Constantsv1.PalylistDay02);
        fileItemInput(tableDay03, writer, Constantsv1.PalylistDay03);
        fileItemInput(tableDay04, writer, Constantsv1.PalylistDay04);
        fileItemInput(tableDay05, writer, Constantsv1.PalylistDay05);
        fileItemInput(tableDay06, writer, Constantsv1.PalylistDay06);
        fileItemInput(tableDay07, writer, Constantsv1.PalylistDay07);
        fileItemInput(tableDay08, writer, Constantsv1.PalylistDay08);
        fileItemInput(tableDay09, writer, Constantsv1.PalylistDay09);
        fileItemInput(tableDay10, writer, Constantsv1.PalylistDay10);
        fileItemInput(tableDay11, writer, Constantsv1.PalylistDay11);
        fileItemInput(tableDay12, writer, Constantsv1.PalylistDay12);
        fileItemInput(tableDay13, writer, Constantsv1.PalylistDay13);
        fileItemInput(tableDay14, writer, Constantsv1.PalylistDay14);
        fileItemInput(tableDay15, writer, Constantsv1.PalylistDay15);
        fileItemInput(tableBigPlaylist, writer, Constantsv1.PalylistBig);
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
                Iterator playlisten = playliste.getChildren(Constantsv1.XMLPlaylisteElementNameValue).iterator();
                while (playlisten.hasNext()) {
                    Element liste = (Element) playlisten.next();
                    switch(Integer.valueOf(liste.getAttribute(Constantsv1.XMLTrackItemElementName).getValue())) {
                        case Constantsv1.PalylistDay01:
                            fillTable(tableDay01, Constantsv1.PalylistDay01, liste);
                            break;
                        case Constantsv1.PalylistDay02:
                            fillTable(tableDay02, Constantsv1.PalylistDay01, liste);
                            break;
                        case Constantsv1.PalylistDay03:
                            fillTable(tableDay03, Constantsv1.PalylistDay03, liste);
                            break;
                        case Constantsv1.PalylistDay04:
                            fillTable(tableDay04, Constantsv1.PalylistDay04, liste);
                            break;
                        case Constantsv1.PalylistDay05:
                            fillTable(tableDay05, Constantsv1.PalylistDay05, liste);
                            break;
                        case Constantsv1.PalylistDay06:
                            fillTable(tableDay06, Constantsv1.PalylistDay06, liste);
                            break;
                        case Constantsv1.PalylistDay07:
                            fillTable(tableDay07, Constantsv1.PalylistDay07, liste);
                            break;
                        case Constantsv1.PalylistDay08:
                            fillTable(tableDay08, Constantsv1.PalylistDay08, liste);
                            break;
                        case Constantsv1.PalylistDay09:
                            fillTable(tableDay09, Constantsv1.PalylistDay09, liste);
                            break;
                        case Constantsv1.PalylistDay10:
                            fillTable(tableDay10, Constantsv1.PalylistDay10, liste);
                            break;
                        case Constantsv1.PalylistDay11:
                            fillTable(tableDay11, Constantsv1.PalylistDay11, liste);
                            break;
                        case Constantsv1.PalylistDay12:
                            fillTable(tableDay12, Constantsv1.PalylistDay12, liste);
                            break;
                        case Constantsv1.PalylistDay13:
                            fillTable(tableDay13, Constantsv1.PalylistDay13, liste);
                            break;
                        case Constantsv1.PalylistDay14:
                            fillTable(tableDay14, Constantsv1.PalylistDay14, liste);
                            break;
                        case Constantsv1.PalylistDay15:
                            fillTable(tableDay15, Constantsv1.PalylistDay15, liste);
                            break;
                        case Constantsv1.PalylistBig:
                            fillTable(tableBigPlaylist, Constantsv1.PalylistBig, liste);
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
        if (tableIndex == Constantsv1.PalylistBig) {
            bigListTitle.setText(liste.getAttributeValue(Constantsv1.XMLPlaylisteElementTitle));
        }
        Iterator XMLItems = liste.getChildren(Constantsv1.XMLTrackItemElementNameValue).iterator();
        while (XMLItems.hasNext()) {
            Element XMLItem = (Element) XMLItems.next();
            Trackv1 a = new Trackv1(XMLItem.getAttributeValue(Constantsv1.XMLTrackItemElementName), table, Integer.valueOf(XMLItem.getAttributeValue(Constantsv1.XMLTrackItemElementIDName)), tableIndex);
        }
        numTable(table);
    }

    private void fileItemInput(Table table, XMLStreamWriter writer, int Playlistenname) throws Exception {
        writer.writeStartElement(Constantsv1.XMLPlaylisteElementNameValue);
        writer.writeAttribute(Constantsv1.XMLPlaylisteElementName, String.valueOf(Playlistenname));
        if (Playlistenname == Constantsv1.PalylistBig) writer.writeAttribute(Constantsv1.XMLPlaylisteElementTitle, bigListTitle.getText());
        for (int i = 0; i < table.getItemCount(); i++) {
            if (!table.getItem(i).isDisposed()) {
                String itmeName = ((Trackv1) table.getItem(i).getData()).getTitle();
                writer.writeStartElement(Constantsv1.XMLTrackItemElementNameValue);
                writer.writeAttribute(Constantsv1.XMLTrackItemElementName, itmeName);
                writer.writeAttribute(Constantsv1.XMLTrackItemElementIDName, String.valueOf(i));
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
        String[] filterExt = { ("*." + Constantsv1.FileEnd) };
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
        String[] filterExt = { ("*." + Constantsv1.FileEnd) };
        dialog.setFilterExtensions(filterExt);
        String result = dialog.open();
        if (result != null) {
            if (!result.toLowerCase().endsWith("." + Constantsv1.FileEnd)) {
                result = result + "." + Constantsv1.FileEnd;
            } else if (result.toLowerCase().endsWith(".")) result = result + Constantsv1.FileEnd;
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
        sShell.setText(Constantsv1.Title + " " + strTitle + " *");
        return isChange;
    }

    public void setChange(boolean isChange) {
        if (isChange) sShell.setText(Constantsv1.Title + " " + strTitle + " *"); else sShell.setText(Constantsv1.Title + " " + strTitle + " ");
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
        String[] filterExt = { ("*." + Constantsv1.PDFFileEnd) };
        dialog.setFilterExtensions(filterExt);
        dialog.setFileName(bigListTitle.getText());
        String result = dialog.open();
        if (result != null) {
            if (!result.toLowerCase().endsWith("." + Constantsv1.PDFFileEnd)) {
                result = result + "." + Constantsv1.PDFFileEnd;
            } else if (result.toLowerCase().endsWith(".")) result = result + Constantsv1.PDFFileEnd;
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
            if (tabFolder.getSelectionIndex() == 0 && (tableBigPlaylist.getItemCount() != 0)) {
                Paragraph H1 = new Paragraph(Constantsv1.TitleA, FontFactory.getFont(FontFactory.HELVETICA, 16));
                document.add(H1);
                Paragraph H2 = new Paragraph(Constantsv1.ProgrammFor + bigListTitle.getText(), FontFactory.getFont(FontFactory.HELVETICA, 16));
                document.add(H2);
                Paragraph H5 = new Paragraph(Constantsv1.TitleB + combo.getText(), FontFactory.getFont(FontFactory.HELVETICA, 16));
                document.add(H5);
                pdfItemInput(tableBigPlaylist, document, Constantsv1.PalylistBig);
            }
            if (tabFolder.getSelectionIndex() == 1 && (tableDay01.getItemCount() != 0 || tableDay02.getItemCount() != 0 || tableDay03.getItemCount() != 0 || tableDay04.getItemCount() != 0 || tableDay05.getItemCount() != 0 || tableDay06.getItemCount() != 0 || tableDay07.getItemCount() != 0 || tableDay08.getItemCount() != 0 || tableDay09.getItemCount() != 0 || tableDay10.getItemCount() != 0 || tableDay11.getItemCount() != 0 || tableDay12.getItemCount() != 0 || tableDay13.getItemCount() != 0 || tableDay14.getItemCount() != 0 || tableDay15.getItemCount() != 0)) {
                Paragraph H1 = new Paragraph(Constantsv1.TitleA, FontFactory.getFont(FontFactory.HELVETICA, 16));
                document.add(H1);
                Paragraph H2 = new Paragraph(Constantsv1.ProgrammFor + strTitle, FontFactory.getFont(FontFactory.HELVETICA, 16));
                document.add(H2);
                pdfItemInput(tableDay01, document, Constantsv1.PalylistDay01);
                pdfItemInput(tableDay02, document, Constantsv1.PalylistDay02);
                pdfItemInput(tableDay03, document, Constantsv1.PalylistDay03);
                pdfItemInput(tableDay04, document, Constantsv1.PalylistDay04);
                pdfItemInput(tableDay05, document, Constantsv1.PalylistDay05);
                pdfItemInput(tableDay06, document, Constantsv1.PalylistDay06);
                pdfItemInput(tableDay07, document, Constantsv1.PalylistDay07);
                pdfItemInput(tableDay08, document, Constantsv1.PalylistDay08);
                pdfItemInput(tableDay09, document, Constantsv1.PalylistDay09);
                pdfItemInput(tableDay10, document, Constantsv1.PalylistDay10);
                pdfItemInput(tableDay11, document, Constantsv1.PalylistDay11);
                pdfItemInput(tableDay12, document, Constantsv1.PalylistDay12);
                pdfItemInput(tableDay13, document, Constantsv1.PalylistDay13);
                pdfItemInput(tableDay14, document, Constantsv1.PalylistDay14);
                pdfItemInput(tableDay15, document, Constantsv1.PalylistDay15);
            }
            document.close();
        } catch (Exception e) {
        }
    }

    private void pdfItemInput(Table table, com.lowagie.text.Document document, int Playlistenname) throws Exception {
        if ((table.getItemCount() != 0)) {
            Paragraph pdfHead = new Paragraph("    " + Constantsv1.TablesTitle[Playlistenname], FontFactory.getFont(FontFactory.HELVETICA, 15));
            pdfHead.setSpacingBefore(20);
            if (tabFolder.getSelectionIndex() == 1) document.add(pdfHead);
            com.lowagie.text.List list = new com.lowagie.text.List(true, 17);
            list.setNumbered(true);
            list.setIndentationLeft(50);
            list.setPostSymbol(")");
            for (int i = 0; i < table.getItemCount(); i++) {
                if (!table.getItem(i).isDisposed()) {
                    String itmeName = ((Trackv1) table.getItem(i).getData()).getTitle();
                    list.add(new com.lowagie.text.ListItem(itmeName));
                }
            }
            document.add(list);
            if (Playlistenname == Constantsv1.PalylistBig) document.newPage();
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
            if (tabFolder.getSelectionIndex() == 0) {
                if (tableBigPlaylist.getItemCount() == 0) {
                    msgBox.setMessage("The paylist is empty!");
                    msgBox.open();
                    return;
                }
                try {
                    FileWriter outFile = new FileWriter(ListPodDir + "/" + combo.getText() + Constantsv1.playlistFileEnd);
                    PrintWriter out = new PrintWriter(outFile);
                    for (int i = 0; i < tableBigPlaylist.getItemCount(); i++) {
                        out.print(Constantsv1.podPlaylistFolder + tableBigPlaylist.getItem(i).getText(1) + Constantsv1.SoundfileEnd + "\n");
                    }
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (tabFolder.getSelectionIndex() == 1) if (tableDay01.getItemCount() != 0 || tableDay02.getItemCount() != 0 || tableDay03.getItemCount() != 0 || tableDay04.getItemCount() != 0 || tableDay05.getItemCount() != 0 || tableDay06.getItemCount() != 0 || tableDay07.getItemCount() != 0 || tableDay08.getItemCount() != 0 || tableDay09.getItemCount() != 0 || tableDay10.getItemCount() != 0 || tableDay11.getItemCount() != 0 || tableDay12.getItemCount() != 0 || tableDay13.getItemCount() != 0 || tableDay14.getItemCount() != 0 || tableDay15.getItemCount() != 0) {
                makePodList("playlist1", tableDay01);
                makePodList("playlist2", tableDay02);
                makePodList("playlist3", tableDay03);
                makePodList("playlist4", tableDay04);
                makePodList("playlist5", tableDay05);
                makePodList("playlist6", tableDay06);
                makePodList("playlist7", tableDay07);
                makePodList("playlist8", tableDay08);
                makePodList("playlist9", tableDay09);
                makePodList("playlist10", tableDay10);
                makePodList("playlist11", tableDay11);
                makePodList("playlist12", tableDay12);
                makePodList("playlist13", tableDay13);
                makePodList("playlist14", tableDay14);
                makePodList("playlist15", tableDay15);
            } else {
                msgBox.setMessage("The paylists are emty!");
                msgBox.open();
                return;
            }
        } catch (Exception e) {
        }
    }

    private void makePodList(String listName, Table table) throws Exception {
        File iniFile = new File(ListPodDir + "/" + listName + Constantsv1.playlistFileEnd);
        if (iniFile.isFile()) {
            iniFile.delete();
        }
        if (table.getItemCount() == 0) return;
        if (table.getItemCount() != 0) {
            PrintWriter out = new PrintWriter(iniFile);
            for (int i = 0; i < table.getItemCount(); i++) {
                out.print(Constantsv1.podPlaylistFolder + table.getItem(i).getText(1) + Constantsv1.SoundfileEnd + "\n");
            }
            out.close();
        }
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
