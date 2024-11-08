package com.todo.core;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import com.todo.objects.Filter;
import com.todo.objects.ToDoDate;
import com.todo.objects.ToDoItem;
import com.todo.prompters.FilterPrompter;
import com.todo.prompters.ItemPrompter;
import com.todo.utils.Constants;
import com.todo.utils.Utils;

public class ToDo {

    private ItemPrompter itemPrompter;

    private FilterPrompter filterPrompter;

    private boolean ascending;

    private boolean fileModified;

    private boolean filterActivated;

    private volatile boolean stopClock;

    private volatile boolean canQuit;

    private Preferences preferences;

    private int pref_x_pos;

    private int pref_y_pos;

    private int pref_height;

    private int pref_width;

    private int[] pref_columnOrder;

    private boolean pref_altRowColors;

    private boolean pref_showGrid;

    private boolean pref_keepBlanksAtBottom;

    private boolean pref_maximized;

    private File currentFile;

    private Filter filter;

    private ArrayList list_filteredItems;

    private ArrayList list_selectedIds;

    private TreeSet stringSet;

    private Stack undoStack;

    private Stack redoStack;

    private SimpleDateFormat sdf;

    private ToDoDate currentTime;

    private String currentFilename;

    private int lastColumnSorted;

    private int topIndex;

    private Display display;

    private Shell shell;

    private ToolBar toolbar_main;

    private ToolBar toolbar_quickSearch;

    private Menu menuBar;

    private Table table;

    private Text text_quickSearch;

    private Label label_quickSearch;

    private Label label_dateTime;

    private Label label_itemCount;

    private Composite composite_quickSearch;

    /**
	 * Starts the program.
	 * ============================================================================
	 */
    public static void main(String[] args) {
        new ToDo();
    }

    /**
	 * Default constructor which creates and runs a new ToDo instance.
	 * ============================================================================
	 */
    public ToDo() {
        ItemPrompter it = new ItemPrompter(null, null, -1, -1, -1);
        loadPreferences();
        list_selectedIds = new ArrayList();
        list_filteredItems = new ArrayList();
        filter = new Filter();
        undoStack = new Stack();
        redoStack = new Stack();
        stringSet = new TreeSet(new Comparator() {

            public int compare(Object o1, Object o2) {
                String s1 = (String) o1;
                String s2 = (String) o2;
                int result = s1.trim().toUpperCase().compareTo(s2.trim().toUpperCase());
                if (result > 0) {
                    return 1;
                } else if (result == 0) {
                    int result2 = s1.trim().compareTo(s2.trim());
                    if (result2 == 0) return 0;
                    if (result2 > 0) return 1;
                    return -1;
                } else {
                    return -1;
                }
            }
        });
        filterActivated = false;
        shellSetup();
        shellDisplay();
    }

    /**
	 * Makes final preparations and then displays the shell on-screen.
	 * ============================================================================ 
	 */
    private void shellDisplay() {
        shell.setImage(Utils.getImage("logo.gif"));
        shell.open();
        table.forceFocus();
        toolbar_main.setEnabled(false);
        toolbar_main.setEnabled(true);
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }

    /**
	 * Initializes the shell display.
	 * ============================================================================ 
	 */
    private void shellSetup() {
        display = Display.getDefault();
        shell = new Shell(display);
        currentFilename = "Untitled";
        updateFileStatus(false);
        GridLayout gridLayout = new GridLayout(2, false);
        shell.setLayout(gridLayout);
        shell.addShellListener(new ShellAdapter() {

            public void shellClosed(ShellEvent e) {
                e.doit = close();
            }
        });
        setupMenuBar();
        Label separator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        separator.setLayoutData(gridData);
        setupPrompters();
        setupToolbar();
        setupDateTime();
        setupQuickSearch();
        setupTable();
        label_itemCount = new Label(shell, SWT.NONE);
        label_itemCount.setAlignment(SWT.LEFT);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        label_itemCount.setLayoutData(gridData);
        startClockThread();
        if (pref_maximized) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            shell.setSize(Constants.SHELL_WIDTH, Constants.SHELL_HEIGHT);
            shell.setLocation((int) Math.max(0, ((screenSize.getWidth() - Constants.SHELL_WIDTH) / 2)), (int) Math.max(0, ((screenSize.getHeight() - Constants.SHELL_HEIGHT) / 2)));
            shell.setMaximized(true);
        } else {
            shell.setSize(pref_width, pref_height);
            shell.setLocation(pref_x_pos, pref_y_pos);
        }
        updateItemCount();
    }

    /**
	 * Sets up the Item Prompter and the Filter Prompter.
	 * ============================================================================
	 */
    private void setupPrompters() {
        itemPrompter = new ItemPrompter(shell, "", 500, 300, Constants.BUTTON_CANCEL | Constants.BUTTON_OK);
        itemPrompter.initialize();
        filterPrompter = new FilterPrompter(shell, "", 495, 285, Constants.BUTTON_CANCEL | Constants.BUTTON_OK | Constants.BUTTON_CUSTOM);
        filterPrompter.initialize();
    }

    /**
	 * Creates and starts the clock thread.
	 * ============================================================================
	 */
    private void startClockThread() {
        stopClock = false;
        final Runnable runner = new Runnable() {

            long counter = 0;

            public void run() {
                currentTime.setToCurrentTime();
                label_dateTime.setText(sdf.format(currentTime) + ":" + currentTime.getMinutesStr() + " " + currentTime.getAMPMStr());
            }
        };
        canQuit = false;
        Thread t = new Thread("clock") {

            public void run() {
                try {
                    while (!stopClock && !display.isDisposed()) {
                        display.asyncExec(runner);
                        Thread.sleep(500);
                    }
                    canQuit = true;
                } catch (Exception e) {
                    System.out.println("Error in clock thread:");
                    e.printStackTrace();
                    Utils.showMessageBox(shell, "Error", "Error in clock thread:\n\n" + Utils.getStackTraceStr(e), SWT.ICON_ERROR | SWT.OK);
                }
            }
        };
        t.start();
    }

    /**
	 * Initializes the Table.
	 * ============================================================================
	 */
    private void setupTable() {
        table = new Table(shell, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        table.setLayoutData(gridData);
        table.addSelectionListener(new SelectionAdapter() {

            public void widgetDefaultSelected(SelectionEvent e) {
                ToDoItem editItem = itemPrompter.editItem(getCurrentItem(), getCategories());
                if (editItem != null) {
                    pushUndo(true);
                    editItem.pasteOverTable(table, table.getSelectionIndex());
                    updateFileStatus(true);
                    table.forceFocus();
                    searchTable();
                }
            }
        });
        SelectionListener columnListener = new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                Object index = ((TableColumn) e.getSource()).getData();
                sort(((Integer) index).intValue());
            }
        };
        for (int i = 0; i < Constants.COLUMN_NAMES.length; i++) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(Constants.COLUMN_NAMES[i]);
            column.setData(new Integer(i));
            column.setMoveable(true);
            switch(i) {
                case 0:
                    column.setWidth(16);
                    column.setResizable(false);
                    break;
                case 1:
                    column.setWidth(16);
                    column.setResizable(false);
                    break;
                case 2:
                    column.setWidth(90);
                    break;
                case 3:
                    column.setWidth(90);
                    break;
                case 4:
                    column.setWidth(90);
                    break;
                case 5:
                    column.setWidth(110);
                    break;
                case 6:
                    column.setWidth(215);
                    break;
                default:
            }
            column.addSelectionListener(columnListener);
        }
        table.setHeaderVisible(true);
        table.setColumnOrder(pref_columnOrder);
        table.setLinesVisible(pref_showGrid);
    }

    /**
	 * Initializes the Date/Time display.
	 * ============================================================================
	 */
    private void setupDateTime() {
        currentTime = new ToDoDate();
        sdf = new SimpleDateFormat("EEEEEE MMM dd, yyyy - h");
        label_dateTime = new Label(shell, SWT.RIGHT);
        label_dateTime.setFont(new Font(display, "Tahoma", 8, SWT.BOLD));
        label_dateTime.setText(sdf.format(currentTime) + ":" + currentTime.getMinutesStr() + " " + currentTime.getAMPMStr());
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 1;
        label_dateTime.setLayoutData(gridData);
    }

    /**
	 * Initializes the Quick Search.
	 * ============================================================================ 
	 */
    private void setupQuickSearch() {
        composite_quickSearch = new Composite(shell, SWT.NONE);
        label_quickSearch = new Label(composite_quickSearch, SWT.NONE);
        label_quickSearch.setText("Quick Search");
        label_quickSearch.setLocation(10, 5);
        label_quickSearch.pack();
        text_quickSearch = new Text(composite_quickSearch, SWT.SINGLE | SWT.BORDER);
        text_quickSearch.setSize(90, 21);
        text_quickSearch.setLocation(80, 1);
        text_quickSearch.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                searchTable();
            }
        });
        toolbar_quickSearch = new ToolBar(composite_quickSearch, SWT.FLAT);
        toolbar_quickSearch.setLocation(173, 1);
        toolbar_quickSearch.setSize(25, 22);
        final ToolItem item_clearQuickSearch = new ToolItem(toolbar_quickSearch, SWT.PUSH);
        item_clearQuickSearch.setImage(Utils.getImage("delete.gif"));
        item_clearQuickSearch.setToolTipText("Clear");
        item_clearQuickSearch.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                text_quickSearch.setText("");
                searchTable();
            }
        });
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gridData.horizontalSpan = 2;
        gridData.widthHint = 196;
        composite_quickSearch.setLayoutData(gridData);
    }

    /**
	 * Initializes the Toolbar.
	 * ============================================================================ 
	 */
    private void setupToolbar() {
        toolbar_main = new ToolBar(shell, SWT.FLAT);
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL);
        gridData.horizontalSpan = 1;
        toolbar_main.setLayoutData(gridData);
        ToolItem newListItem = new ToolItem(toolbar_main, SWT.PUSH);
        newListItem.setImage(Utils.getImage("new.gif"));
        newListItem.setToolTipText("New List");
        newListItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                newTable(true);
            }
        });
        ToolItem openListItem = new ToolItem(toolbar_main, SWT.PUSH);
        openListItem.setImage(Utils.getImage("open.gif"));
        openListItem.setToolTipText("Open List");
        openListItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                openPrompt();
            }
        });
        ToolItem saveListItem = new ToolItem(toolbar_main, SWT.PUSH);
        saveListItem.setImage(Utils.getImage("save.gif"));
        saveListItem.setToolTipText("Save List");
        saveListItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                save();
            }
        });
        ToolItem pdfListItem = new ToolItem(toolbar_main, SWT.PUSH);
        pdfListItem.setImage(Utils.getImage("pdf_icon.gif"));
        pdfListItem.setToolTipText("Export List as Adobe PDF");
        pdfListItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                ArrayList items = new ArrayList();
                for (int i = 0; i < table.getItemCount(); i++) {
                    items.add(getItem(i));
                }
                ReportGenerator.createReport(shell, items, table.getColumnOrder());
            }
        });
        new ToolItem(toolbar_main, SWT.SEPARATOR);
        ToolItem addItem = new ToolItem(toolbar_main, SWT.PUSH);
        addItem.setImage(Utils.getImage("add.gif"));
        addItem.setToolTipText("Add Item");
        addItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                ToDoItem newItem = itemPrompter.addItem(getCategories());
                if (newItem != null) {
                    pushUndo(true);
                    newItem.insertIntoTable(table, 0);
                    updateFileStatus(true);
                    colorRows();
                    table.forceFocus();
                    searchTable();
                    updateItemCount();
                }
            }
        });
        ToolItem editItem = new ToolItem(toolbar_main, SWT.PUSH);
        editItem.setImage(Utils.getImage("edit.gif"));
        editItem.setToolTipText("Edit Item");
        editItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (table.getSelectionCount() != 1) return;
                ToDoItem editItem = itemPrompter.editItem(getCurrentItem(), getCategories());
                if (editItem != null) {
                    pushUndo(true);
                    editItem.pasteOverTable(table, table.getSelectionIndex());
                    updateFileStatus(true);
                    table.forceFocus();
                    searchTable();
                }
            }
        });
        ToolItem completeItem = new ToolItem(toolbar_main, SWT.PUSH);
        completeItem.setImage(Utils.getImage("check.gif"));
        completeItem.setToolTipText("Complete");
        completeItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                completeItems();
            }
        });
        ToolItem renewItem = new ToolItem(toolbar_main, SWT.PUSH);
        renewItem.setImage(Utils.getImage("renew.gif"));
        renewItem.setToolTipText("Renew");
        renewItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                renewItems();
            }
        });
        new ToolItem(toolbar_main, SWT.SEPARATOR);
        ToolItem moveTopItem = new ToolItem(toolbar_main, SWT.PUSH);
        moveTopItem.setImage(Utils.getImage("top.gif"));
        moveTopItem.setToolTipText("Move to Top");
        moveTopItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                moveToTop();
            }
        });
        ToolItem moveUpItem = new ToolItem(toolbar_main, SWT.PUSH);
        moveUpItem.setImage(Utils.getImage("up.gif"));
        moveUpItem.setToolTipText("Move Up");
        moveUpItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                moveUp();
            }
        });
        ToolItem moveDownItem = new ToolItem(toolbar_main, SWT.PUSH);
        moveDownItem.setImage(Utils.getImage("down.gif"));
        moveDownItem.setToolTipText("Move Down");
        moveDownItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                moveDown();
            }
        });
        ToolItem moveBottomItem = new ToolItem(toolbar_main, SWT.PUSH);
        moveBottomItem.setImage(Utils.getImage("bottom.gif"));
        moveBottomItem.setToolTipText("Move to Bottom");
        moveBottomItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                moveToBottom();
            }
        });
        new ToolItem(toolbar_main, SWT.SEPARATOR);
        ToolItem filterItem = new ToolItem(toolbar_main, SWT.CHECK);
        filterItem.setImage(Utils.getImage("filterOn.gif"));
        filterItem.setToolTipText("Toggle Filter");
        filterItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                filterActivated = !filterActivated;
                searchTable();
            }
        });
        ToolItem filterSetupItem = new ToolItem(toolbar_main, SWT.PUSH);
        filterSetupItem.setImage(Utils.getImage("filterSetup.gif"));
        filterSetupItem.setToolTipText("Configure Filter");
        filterSetupItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                filter = filterPrompter.editFilter(filter);
                searchTable();
            }
        });
        new ToolItem(toolbar_main, SWT.SEPARATOR);
        ToolItem deleteItem = new ToolItem(toolbar_main, SWT.PUSH);
        deleteItem.setImage(Utils.getImage("delete.gif"));
        deleteItem.setToolTipText("Delete");
        deleteItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                deleteItems();
            }
        });
        ToolItem deleteCompletedItem = new ToolItem(toolbar_main, SWT.PUSH);
        deleteCompletedItem.setImage(Utils.getImage("deleteCompleted.gif"));
        deleteCompletedItem.setToolTipText("Delete All Completed Items");
        deleteCompletedItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                deleteCompletedItems();
            }
        });
    }

    /**
	 * Initializes the MenuBar.
	 * ============================================================================ 
	 */
    private void setupMenuBar() {
        menuBar = new Menu(shell, SWT.BAR);
        Menu menu1 = new Menu(shell, SWT.DROP_DOWN);
        MenuItem fileMenu = new MenuItem(menuBar, SWT.CASCADE);
        fileMenu.setText("File");
        fileMenu.setMenu(menu1);
        MenuItem newItem = new MenuItem(menu1, SWT.NULL);
        newItem.setText("New\tCtrl+N");
        newItem.setAccelerator(SWT.CTRL + 'N');
        newItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                newTable(true);
            }
        });
        MenuItem openItem = new MenuItem(menu1, SWT.NULL);
        openItem.setText("Open...\tCtrl+O");
        openItem.setAccelerator(SWT.CTRL + 'O');
        openItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                openPrompt();
            }
        });
        MenuItem saveItem = new MenuItem(menu1, SWT.NULL);
        saveItem.setText("Save\tCtrl+S");
        saveItem.setAccelerator(SWT.CTRL + 'S');
        saveItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                save();
            }
        });
        MenuItem saveAsItem = new MenuItem(menu1, SWT.NULL);
        saveAsItem.setText("Save as...");
        saveAsItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                saveAs();
            }
        });
        new MenuItem(menu1, SWT.SEPARATOR);
        MenuItem exitItem = new MenuItem(menu1, SWT.NULL);
        exitItem.setText("Exit\tCtrl+E");
        exitItem.setAccelerator(SWT.CTRL + 'E');
        exitItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                shell.close();
            }
        });
        Menu editMenu = new Menu(shell, SWT.DROP_DOWN);
        MenuItem editMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        editMenuItem.setText("Edit");
        editMenuItem.setMenu(editMenu);
        MenuItem undoItem = new MenuItem(editMenu, SWT.NULL);
        undoItem.setText("Undo\tCtrl+Z");
        undoItem.setAccelerator(SWT.CTRL + 'Z');
        undoItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (!undoStack.empty()) {
                    pushRedo();
                    popUndo();
                }
            }
        });
        MenuItem redoItem = new MenuItem(editMenu, SWT.NULL);
        redoItem.setText("Redo\tCtrl+Y");
        redoItem.setAccelerator(SWT.CTRL + 'Y');
        redoItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (!redoStack.empty()) {
                    pushUndo(false);
                    popRedo();
                }
            }
        });
        new MenuItem(editMenu, SWT.SEPARATOR);
        MenuItem deleteItem = new MenuItem(editMenu, SWT.NULL);
        deleteItem.setText("Delete\tDEL");
        deleteItem.setAccelerator(SWT.DEL);
        deleteItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                deleteItems();
            }
        });
        MenuItem selectAllItem = new MenuItem(editMenu, SWT.NULL);
        selectAllItem.setText("Select All\tCtrl+A");
        selectAllItem.setAccelerator(SWT.CTRL + 'A');
        selectAllItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                table.selectAll();
            }
        });
        Menu menu2 = new Menu(shell, SWT.DROP_DOWN);
        MenuItem optionsMenu = new MenuItem(menuBar, SWT.CASCADE);
        optionsMenu.setText("Options");
        optionsMenu.setMenu(menu2);
        MenuItem gridItem = new MenuItem(menu2, SWT.CHECK);
        gridItem.setText("Show Grid");
        gridItem.setSelection(pref_showGrid);
        gridItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                pref_showGrid = !pref_showGrid;
                table.setLinesVisible(pref_showGrid);
            }
        });
        MenuItem colorItem = new MenuItem(menu2, SWT.CHECK);
        colorItem.setText("Alternate Row Colors");
        colorItem.setSelection(pref_altRowColors);
        colorItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                pref_altRowColors = !pref_altRowColors;
                colorRows();
            }
        });
        MenuItem keepBlanks = new MenuItem(menu2, SWT.CHECK);
        keepBlanks.setText("Keep Blanks at the Bottom When Sorting");
        keepBlanks.setSelection(pref_keepBlanksAtBottom);
        keepBlanks.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                pref_keepBlanksAtBottom = !pref_keepBlanksAtBottom;
            }
        });
        Menu menu3 = new Menu(shell, SWT.DROP_DOWN);
        MenuItem aboutMenu = new MenuItem(menuBar, SWT.CASCADE);
        aboutMenu.setText("About");
        aboutMenu.setMenu(menu3);
        MenuItem aboutItem = new MenuItem(menu3, SWT.NULL);
        aboutItem.setText("About ToDo");
        aboutItem.setSelection(true);
        aboutItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                Utils.showMessageBox(shell, "ToDo", "ToDo v0.9\n\nDennis McKnight\ndennis.mcknight@gmail.com", SWT.ICON_INFORMATION | SWT.YES);
            }
        });
        shell.setMenuBar(menuBar);
    }

    /**
	 * Loads the user's preferences.
	 * ============================================================================ 
	 */
    private void loadPreferences() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        preferences = Preferences.userNodeForPackage(ToDo.class);
        pref_altRowColors = preferences.getBoolean(Constants.PREF_ALT_ROW_COLORS, true);
        pref_showGrid = preferences.getBoolean(Constants.PREF_SHOW_GRID, false);
        pref_keepBlanksAtBottom = preferences.getBoolean(Constants.PREF_KEEP_BLANKS_AT_BOTTOM, true);
        pref_maximized = preferences.getBoolean(Constants.PREF_MAXIMIZED, false);
        pref_height = preferences.getInt(Constants.PREF_HEIGHT, Constants.SHELL_HEIGHT);
        pref_width = preferences.getInt(Constants.PREF_WIDTH, Constants.SHELL_WIDTH);
        pref_x_pos = preferences.getInt(Constants.PREF_X_POS, Math.max(0, (screenSize.width - pref_width) / 2));
        pref_y_pos = preferences.getInt(Constants.PREF_Y_POS, Math.max(0, (screenSize.height - pref_height) / 2));
        setColumnOrder();
    }

    /**
	 * Saves the user's preferences.
	 * ============================================================================ 
	 */
    private void savePreferences() {
        preferences.putBoolean(Constants.PREF_ALT_ROW_COLORS, pref_altRowColors);
        preferences.putBoolean(Constants.PREF_SHOW_GRID, pref_showGrid);
        preferences.putBoolean(Constants.PREF_KEEP_BLANKS_AT_BOTTOM, pref_keepBlanksAtBottom);
        preferences.putBoolean(Constants.PREF_MAXIMIZED, shell.getMaximized());
        preferences.putInt(Constants.PREF_X_POS, shell.getBounds().x);
        preferences.putInt(Constants.PREF_Y_POS, shell.getBounds().y);
        preferences.putInt(Constants.PREF_HEIGHT, shell.getBounds().height);
        preferences.putInt(Constants.PREF_WIDTH, shell.getBounds().width);
        preferences.putByteArray(Constants.PREF_COLUMN_POS, getColumnOrder());
    }

    /**
	 * Updates the filename and appends a '*' if the file has been modified.
	 * ============================================================================ 
	 */
    private void updateFileStatus(boolean modified) {
        StringBuffer title = new StringBuffer(Constants.SHELL_NAME);
        title.append(" - ");
        title.append(currentFilename);
        if (modified) {
            title.append("*");
        }
        fileModified = modified;
        shell.setText(title.toString());
    }

    /**
	 * Returns the data for the currently selected item on the table.
	 * ============================================================================ 
	 */
    private ToDoItem getCurrentItem() {
        if (table.getSelectionCount() != 1) {
            return null;
        }
        return getItem(table.getSelectionIndex());
    }

    /**
	 * Returns the data for the item in the table specified at the zero-based index.
	 * ============================================================================ 
	 */
    private ToDoItem getItem(int index) {
        if (index < 0 || index > table.getItemCount() - 1) {
            return null;
        }
        return ((ToDoItem) table.getItem(index).getData());
    }

    /**
	 * Colors the backgrounds of the rows on the table.
	 * ============================================================================ 
	 */
    private void colorRows() {
        int size = table.getItemCount();
        for (int i = 0; i < size; i++) {
            if (i % 2 == 0) {
                table.getItem(i).setBackground((pref_altRowColors ? Constants.LIGHT_GRAY : Constants.WHITE));
            } else {
                table.getItem(i).setBackground(Constants.WHITE);
            }
        }
    }

    /**
	 * Updates the item count at the bottom of the GUI.
	 * ============================================================================ 
	 */
    private void updateItemCount() {
        int shown = table.getItemCount();
        int filtered = list_filteredItems.size();
        int total = shown + filtered;
        StringBuffer sb = new StringBuffer("");
        sb.append(total);
        sb.append(" Item");
        if (total > 1 || total == 0) {
            sb.append("s");
        }
        if (list_filteredItems.size() > 0) {
            sb.append(" (");
            sb.append(table.getItemCount());
            sb.append(" shown, ");
            sb.append(list_filteredItems.size());
            sb.append(" filtered)");
        }
        label_itemCount.setText(sb.toString());
    }

    /**
	 * Swaps the items specified by the zero-based pos1 and pos2 arguments.
	 * ============================================================================ 
	 */
    private void swapItems(int pos1, int pos2) {
        ToDoItem temp = (ToDoItem) table.getItem(pos1).getData();
        getItem(pos2).pasteOverTable(table, pos1);
        table.deselect(pos1);
        temp.pasteOverTable(table, pos2);
        table.select(pos2);
    }

    /**
	 * Moves all the selected items to the top of the table.
	 * ============================================================================ 
	 */
    private void moveToTop() {
        int[] selected = table.getSelectionIndices();
        ToDoItem temp;
        boolean redraw = false;
        for (int i = 0; i < selected.length; i++) {
            if (selected[i] == i) continue;
            if (!redraw) {
                table.setRedraw(false);
                storeSelectedIds(true);
                pushUndo(true);
                redraw = true;
            }
            temp = (ToDoItem) table.getItem(selected[i]).getData();
            table.remove(selected[i]);
            temp.insertIntoTable(table, i);
        }
        if (redraw) {
            table.setRedraw(true);
            updateFileStatus(true);
            colorRows();
            restoreSelectedIds(true);
        }
    }

    /**
	 * Moves each selected item up one position in the table (provided there
	 * is room to move up to). 
	 * ============================================================================ 
	 */
    private void moveUp() {
        if (table.getSelectionCount() <= 0) return;
        int[] indices = table.getSelectionIndices();
        int closed = -1;
        boolean undoPushed = false;
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] - 1 == closed) {
                closed = indices[i];
                continue;
            }
            if (!undoPushed) {
                pushUndo(true);
                undoPushed = true;
                updateFileStatus(true);
            }
            swapItems(indices[i], indices[i] - 1);
        }
    }

    /**
	 * Moves each selected item down one position in the table (provided there
	 * is room to move down to).
	 * ============================================================================ 
	 */
    private void moveDown() {
        if (table.getSelectionCount() <= 0) return;
        int[] indices = table.getSelectionIndices();
        int closed = table.getItemCount();
        boolean undoPushed = false;
        for (int i = indices.length - 1; i >= 0; i--) {
            if (indices[i] + 1 == closed) {
                closed = indices[i];
                continue;
            }
            if (!undoPushed) {
                pushUndo(true);
                undoPushed = true;
                updateFileStatus(true);
            }
            swapItems(indices[i], indices[i] + 1);
        }
    }

    /**
	 * Moves all the seleted items to the bottom of the table.
	 * ============================================================================ 
	 */
    private void moveToBottom() {
        int[] selected = table.getSelectionIndices();
        ToDoItem temp;
        boolean redraw = false;
        int count = 0;
        for (int i = selected.length - 1; i >= 0; i--) {
            if (selected[i] == table.getItemCount() - 1 - (count++)) continue;
            if (!redraw) {
                table.setRedraw(false);
                storeSelectedIds(true);
                pushUndo(true);
                redraw = true;
            }
            temp = (ToDoItem) table.getItem(selected[i]).getData();
            table.remove(selected[i]);
            temp.insertIntoTable(table, table.getItemCount() + 1 - count);
        }
        if (redraw) {
            table.setRedraw(true);
            updateFileStatus(true);
            colorRows();
            restoreSelectedIds(true);
        }
    }

    /**
	 * Completes all the selected un-completed items on the table.
	 * ============================================================================ 
	 */
    private boolean completeItems() {
        int count = table.getSelectionCount();
        if (count <= 0) return false;
        int[] indices = table.getSelectionIndices();
        boolean canComplete = false;
        for (int i = 0; i < count; i++) {
            if (!(getItem(indices[i]).isCompleted())) {
                canComplete = true;
                break;
            }
        }
        if (!canComplete) {
            return false;
        }
        storeSelectedIds(true);
        pushUndo(true);
        for (int i = 0; i < indices.length; i++) {
            ToDoItem item = getItem(indices[i]);
            if (item.isCompleted()) continue;
            item.setCompleted(true);
            item.setCompletedDate(new ToDoDate());
            table.getItem(indices[i]).setText(Constants.COLUMN_COMPLETED_DATE, item.getCompletedDateStr());
            table.getItem(indices[i]).setImage(Constants.COLUMN_COMPLETED, Constants.IMAGE_CHECK2);
        }
        restoreSelectedIds(true);
        updateFileStatus(true);
        searchTable();
        return true;
    }

    /**
	 * Renews all the selected completed items on the table.
	 * ============================================================================ 
	 */
    private boolean renewItems() {
        int count = table.getSelectionCount();
        if (count <= 0) return false;
        int[] indices = table.getSelectionIndices();
        boolean canRenew = false;
        for (int i = 0; i < count; i++) {
            if (getItem(indices[i]).isCompleted()) {
                canRenew = true;
                break;
            }
        }
        if (!canRenew) {
            return false;
        }
        storeSelectedIds(true);
        pushUndo(true);
        for (int i = 0; i < indices.length; i++) {
            ToDoItem item = ((ToDoItem) table.getItem(indices[i]).getData());
            if (!item.isCompleted()) continue;
            item.setCompleted(false);
            item.setCompletedDate(null);
            table.getItem(indices[i]).setText(Constants.COLUMN_COMPLETED_DATE, item.getCompletedDateStr());
            table.getItem(indices[i]).setImage(Constants.COLUMN_COMPLETED, Constants.IMAGE_BLANK);
        }
        restoreSelectedIds(true);
        updateFileStatus(true);
        searchTable();
        return true;
    }

    /**
	 * Pushes a current view of all item data onto the undo stack.
	 * ============================================================================ 
	 */
    private void pushUndo(boolean clearRedoStack) {
        ArrayList currentItems = new ArrayList();
        for (int i = 0; i < table.getItemCount(); i++) {
            currentItems.add(getItem(i).clone());
        }
        for (int i = 0; i < list_filteredItems.size(); i++) {
            currentItems.add(((ToDoItem) list_filteredItems.get(i)).clone());
        }
        if (undoStack.size() >= 25) undoStack.remove(0);
        undoStack.push(currentItems);
        if (clearRedoStack) redoStack.removeAllElements();
    }

    /**
	 * Clears all item data from memory then pops an old view of all data off
	 * the undo stack and places it back on the table.
	 * ============================================================================ 
	 */
    private void popUndo() {
        if (undoStack.empty()) return;
        ArrayList restoreItems = (ArrayList) undoStack.pop();
        table.removeAll();
        list_filteredItems.clear();
        Iterator it = restoreItems.iterator();
        table.setRedraw(false);
        while (it.hasNext()) {
            ((ToDoItem) it.next()).insertIntoTable(table, table.getItemCount());
        }
        table.setRedraw(true);
        updateFileStatus(true);
        searchTable();
        colorRows();
    }

    /**
	 * Pushes a current view of all item data onto the redo stack.
	 * ============================================================================ 
	 */
    private void pushRedo() {
        ArrayList currentItems = new ArrayList();
        for (int i = 0; i < table.getItemCount(); i++) {
            currentItems.add(getItem(i).clone());
        }
        for (int i = 0; i < list_filteredItems.size(); i++) {
            currentItems.add(((ToDoItem) list_filteredItems.get(i)).clone());
        }
        redoStack.push(currentItems);
    }

    /**
	 * Clears all item data from memory then pops an old view of all data off
	 * the redo stack and places it back on the table.
	 * ============================================================================ 
	 */
    private void popRedo() {
        if (redoStack.empty()) return;
        ArrayList restoreItems = (ArrayList) redoStack.pop();
        table.removeAll();
        list_filteredItems.clear();
        Iterator it = restoreItems.iterator();
        table.setRedraw(false);
        while (it.hasNext()) {
            ((ToDoItem) it.next()).insertIntoTable(table, table.getItemCount());
        }
        table.setRedraw(true);
        updateFileStatus(true);
        searchTable();
        colorRows();
    }

    /**
	 * Deletes all the selected items.
	 * ============================================================================ 
	 */
    private boolean deleteItems() {
        if (table.getSelectionCount() <= 0) {
            return false;
        }
        pushUndo(true);
        table.setRedraw(false);
        for (int i = 0; i < table.getItemCount(); i++) {
            if (table.isSelected(i)) {
                table.remove(i);
                i--;
            }
        }
        table.setRedraw(true);
        updateFileStatus(true);
        colorRows();
        updateItemCount();
        return true;
    }

    /**
	 * Deletes all completed items on the table.
	 * ============================================================================ 
	 */
    private boolean deleteCompletedItems() {
        int count = table.getItemCount();
        if (count <= 0) {
            return false;
        }
        boolean canDelete = false;
        for (int i = 0; i < count; i++) {
            if ((getItem(i)).isCompleted()) {
                canDelete = true;
                break;
            }
        }
        if (!canDelete) {
            return false;
        }
        pushUndo(true);
        table.setRedraw(false);
        for (int i = 0; i < table.getItemCount(); i++) {
            if ((getItem(i)).isCompleted()) {
                table.remove(i);
                i--;
            }
        }
        table.setRedraw(true);
        updateFileStatus(true);
        colorRows();
        updateItemCount();
        return true;
    }

    /**
	 * Filters items based on the filter settings into list_filteredItems and off
	 * the main table.
	 * ============================================================================ 
	 */
    private void filterItems(List allItems) {
        int priority = filter.getPriority();
        int completed = filter.getCompleted();
        String category = filter.getCategory();
        String description = filter.getDescription();
        ToDoDate postedAfter = filter.getPostedAfter();
        ToDoDate postedBefore = filter.getPostedBefore();
        ToDoDate dueAfter = filter.getDueAfter();
        ToDoDate dueBefore = filter.getDueBefore();
        ToDoDate completedAfter = filter.getCompletedAfter();
        ToDoDate completedBefore = filter.getCompletedBefore();
        if (postedBefore != null) {
            postedBefore = (ToDoDate) (filter.getPostedBefore()).clone();
            postedBefore.advanceDay();
            postedBefore.setToStartOfDay();
        }
        if (dueBefore != null) {
            dueBefore = (ToDoDate) (filter.getDueBefore()).clone();
            dueBefore.advanceDay();
            dueBefore.setToStartOfDay();
        }
        if (completedBefore != null) {
            completedBefore = (ToDoDate) (filter.getCompletedBefore()).clone();
            completedBefore.advanceDay();
            completedBefore.setToStartOfDay();
        }
        Iterator it = allItems.iterator();
        while (it.hasNext()) {
            ToDoItem item = (ToDoItem) it.next();
            boolean shouldFilter = filterItem(item, priority, completed, category, description, postedAfter, postedBefore, dueAfter, dueBefore, completedAfter, completedBefore);
            if (shouldFilter) {
                list_filteredItems.add(item);
                it.remove();
            }
        }
    }

    /**
	 * Determines whether or not the specified item should be filtered based
	 * on the supplied filter criteria.
	 * ============================================================================ 
	 */
    private boolean filterItem(ToDoItem item, int priority, int completed, String category, String description, ToDoDate postedAfter, ToDoDate postedBefore, ToDoDate dueAfter, ToDoDate dueBefore, ToDoDate completedAfter, ToDoDate completedBefore) {
        boolean passed = true;
        if (Utils.isNotEmpty(description)) {
            passed = false;
            StringTokenizer filterTokens = new StringTokenizer(description);
            String itemDescription = item.getDescription().toLowerCase();
            while (filterTokens.hasMoreTokens()) {
                if (itemDescription.indexOf(filterTokens.nextToken().trim().toLowerCase()) >= 0) {
                    passed = true;
                    break;
                }
            }
        }
        if (!passed) return true;
        if (Utils.isNotEmpty(category)) {
            passed = false;
            StringTokenizer filterTokens = new StringTokenizer(category);
            String itemCategory = item.getCategory().toLowerCase();
            while (filterTokens.hasMoreTokens()) {
                if (itemCategory.indexOf(filterTokens.nextToken().trim().toLowerCase()) >= 0) {
                    passed = true;
                    break;
                }
            }
        }
        if (!passed) return true;
        if (priority != 0) {
            int itemPriority = item.getPriority();
            passed = false;
            if (((priority & itemPriority) == Constants.PRIORITY_NORMAL) || ((priority & itemPriority) == Constants.PRIORITY_HIGH)) {
                passed = true;
            }
            if (!passed) return true;
        }
        if (completed != 0) {
            int itemCompleted = (item.isCompleted() ? Constants.COMPLETED_YES : Constants.COMPLETED_NO);
            passed = false;
            if (((completed & itemCompleted) == Constants.COMPLETED_NO) || ((completed & itemCompleted) == Constants.COMPLETED_YES)) {
                passed = true;
            }
            if (!passed) return true;
        }
        if (postedAfter != null) {
            passed = false;
            if (postedAfter.before(item.getPostedDate())) {
                passed = true;
            }
        }
        if (!passed) return true;
        if (postedBefore != null) {
            passed = false;
            if (postedBefore.after(item.getPostedDate())) {
                passed = true;
            }
        }
        if (!passed) return true;
        if (dueAfter != null) {
            passed = false;
            if (item.getDueDate() == null) {
                passed = false;
            } else if (dueAfter.before(item.getDueDate())) {
                passed = true;
            }
        }
        if (!passed) return true;
        if (dueBefore != null) {
            passed = false;
            if (item.getDueDate() == null) {
                passed = false;
            } else if (dueBefore.after(item.getDueDate())) {
                passed = true;
            }
        }
        if (!passed) return true;
        if (completedAfter != null) {
            passed = false;
            if (item.getCompletedDate() == null) {
                passed = false;
            } else if (completedAfter.before(item.getCompletedDate())) {
                passed = true;
            }
        }
        if (!passed) return true;
        if (completedBefore != null) {
            passed = false;
            if (item.getCompletedDate() == null) {
                passed = false;
            } else if (completedBefore.after(item.getCompletedDate())) {
                passed = true;
            }
        }
        if (!passed) return true;
        return false;
    }

    /**
	 * Prompts the user to save if the file has been modified. Then saves all
	 * the user's preferences, stops the clock thread, and exits the program.
	 * ============================================================================ 
	 */
    private boolean close() {
        if (fileModified) {
            int choice = Utils.showMessageBox(shell, "ToDo", "The file " + currentFilename + " has been modified.\nWould you like to save the changes?", SWT.ICON_WARNING | SWT.YES | SWT.NO | SWT.CANCEL);
            if (choice == SWT.YES) {
                save();
            } else if (choice == SWT.CANCEL) {
                return false;
            }
        }
        savePreferences();
        stopClock = true;
        try {
            while (!canQuit) {
                Thread.sleep(250);
            }
        } catch (Exception e) {
        }
        return true;
    }

    /**
	 * Returns the current column order in a byte array.
	 * ============================================================================ 
	 */
    private byte[] getColumnOrder() {
        int[] int_order = table.getColumnOrder();
        byte[] byte_order = new byte[int_order.length];
        for (int i = 0; i < int_order.length; i++) {
            byte_order[i] = (byte) int_order[i];
        }
        return byte_order;
    }

    /**
	 * Loads the user's column order preferences into the session's preferences.
	 * ============================================================================ 
	 */
    private void setColumnOrder() {
        byte[] byte_order = preferences.getByteArray(Constants.PREF_COLUMN_POS, new byte[] { 0, 1, 2, 3, 4, 5, 6 });
        pref_columnOrder = new int[byte_order.length];
        for (int i = 0; i < byte_order.length; i++) {
            pref_columnOrder[i] = byte_order[i];
        }
    }

    /**
	 * Sorts the table based on the columnNo clicked.
	 * ============================================================================ 
	 */
    private void sort(int columnNo) {
        if (columnNo != lastColumnSorted) {
            lastColumnSorted = columnNo;
            ascending = true;
        } else {
            ascending = !ascending;
        }
        storeSelectedIds(true);
        table.setSortColumn(table.getColumn(columnNo));
        table.setSortDirection(ascending ? SWT.UP : SWT.DOWN);
        Comparator c;
        switch(columnNo) {
            case 0:
                c = Constants.COMP_COMPLETED;
                break;
            case 1:
                c = Constants.COMP_PRIORITY;
                break;
            case 2:
                c = Constants.COMP_POSTED_DATE;
                break;
            case 3:
                c = Constants.COMP_DUE_DATE;
                break;
            case 4:
                c = Constants.COMP_COMPLETED_DATE;
                break;
            case 5:
                c = Constants.COMP_CATEGORY;
                break;
            case 6:
                c = Constants.COMP_DESCRIPTION;
                break;
            default:
                c = Constants.COMP_POSTED_DATE;
                break;
        }
        ToDoItem[] items = new ToDoItem[table.getItemCount()];
        for (int i = 0; i < table.getItemCount(); i++) {
            items[i] = getItem(i);
        }
        Arrays.sort(items, c);
        if (!ascending) {
            for (int left = 0, right = items.length - 1; left < right; left++, right--) {
                ToDoItem temp = items[left];
                items[left] = items[right];
                items[right] = temp;
            }
        }
        int upperPos = 0;
        int lowerPos = table.getItemCount() - 1;
        for (int i = 0; i < items.length; i++) {
            if (pref_keepBlanksAtBottom) {
                if ((columnNo == Constants.COLUMN_DUE_DATE && items[i].getDueDate() == null) || (columnNo == Constants.COLUMN_COMPLETED_DATE && items[i].getCompletedDate() == null) || (columnNo == Constants.COLUMN_CATGEGORY && Utils.isEmpty(items[i].getCategory())) || (columnNo == Constants.COLUMN_DESCRIPTION && Utils.isEmpty(items[i].getDescription()))) {
                    items[i].pasteOverTable(table, lowerPos--);
                    continue;
                }
            }
            items[i].pasteOverTable(table, upperPos++);
        }
        restoreSelectedIds(true);
    }

    /**
	 * Filters the table by the current filter settings and the quick search
	 * phrase. Removes elements from the table display that do not match (they
	 * are added to the ArrayList list_filteredItems).
	 * ============================================================================ 
	 */
    private void searchTable() {
        storeSelectedIds(true);
        table.setRedraw(false);
        ArrayList allItems = new ArrayList();
        for (int i = 0; i < table.getItemCount(); i++) {
            allItems.add(getItem(i));
        }
        Iterator it = list_filteredItems.iterator();
        while (it.hasNext()) {
            allItems.add(it.next());
        }
        list_filteredItems.clear();
        if (filterActivated) {
            filterItems(allItems);
        }
        String quickSearchPhrase = text_quickSearch.getText().toLowerCase().trim();
        ToDoItem item;
        if (Utils.isNotEmpty(quickSearchPhrase)) {
            it = allItems.iterator();
            while (it.hasNext()) {
                item = (ToDoItem) it.next();
                if (item.getCategory().toLowerCase().indexOf(quickSearchPhrase) < 0 && item.getDescription().toLowerCase().indexOf(quickSearchPhrase) < 0) {
                    list_filteredItems.add(item);
                    it.remove();
                }
            }
        }
        table.removeAll();
        it = allItems.iterator();
        while (it.hasNext()) {
            item = (ToDoItem) it.next();
            item.insertIntoTable(table, table.getItemCount());
            it.remove();
        }
        table.setRedraw(true);
        restoreSelectedIds(true);
        colorRows();
        updateItemCount();
    }

    /**
	 * Saves the current data to the current file.
	 * ============================================================================ 
	 */
    private void save() {
        if (currentFile == null) {
            saveAs();
            if (currentFile == null) {
                return;
            }
        }
        int tableCount = table.getItemCount();
        int filterCount = list_filteredItems.size();
        ToDoItem[] totalData = new ToDoItem[tableCount + filterCount];
        for (int i = 0; i < tableCount; i++) {
            totalData[i] = getItem(i);
        }
        for (int j = 0; j < list_filteredItems.size(); j++) {
            totalData[j + tableCount] = (ToDoItem) list_filteredItems.get(j);
        }
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(currentFile));
            oos.writeObject(totalData);
            oos.writeObject(filter);
            oos.close();
        } catch (Exception e) {
            System.out.println("Error saving file: " + currentFilename);
            e.printStackTrace();
            Utils.showMessageBox(shell, "Error", "Error saving file: " + currentFilename + "\n\n", SWT.ICON_ERROR | SWT.OK);
            return;
        }
        updateFileStatus(false);
    }

    /**
	 * Prompts the user for a file to save to, then saves.
	 * ============================================================================ 
	 */
    private void saveAs() {
        FileDialog saveDialog = new FileDialog(shell, SWT.SAVE);
        saveDialog.setFilterExtensions(new String[] { "*.tdl;", "*.*" });
        saveDialog.setFilterNames(new String[] { "ToDo Lists (*.tdl)", "All Files " });
        if (saveDialog.open() == null) return;
        String fileName = saveDialog.getFileName();
        if (!Utils.endsWith(fileName, ".tdl")) {
            fileName += ".tdl";
        }
        File file = new File(saveDialog.getFilterPath(), fileName);
        if (file.exists()) {
            int choice = Utils.showMessageBox(shell, "ToDo", "The file " + fileName + " already exists.\nWould you like to overwrite it?", SWT.ICON_WARNING | SWT.OK | SWT.NO);
            if (choice == SWT.NO) {
                return;
            }
        }
        currentFile = file;
        currentFilename = fileName;
        save();
    }

    /**
	 * Prompts the user for a file to open, then opens it.
	 * ============================================================================ 
	 */
    private void openPrompt() {
        if (fileModified) {
            int choice = Utils.showMessageBox(shell, "ToDo", "The file " + currentFilename + " has changed.\nWould you like to save the changes?", SWT.ICON_WARNING | SWT.YES | SWT.NO | SWT.CANCEL);
            if (choice == SWT.YES) {
                save();
            } else if (choice == SWT.CANCEL) {
                return;
            }
        }
        FileDialog openDialog = new FileDialog(shell, SWT.OPEN);
        openDialog.setFilterExtensions(new String[] { "*.tdl;", "*.*" });
        openDialog.setFilterNames(new String[] { "ToDo Lists (*.tdl)", "All Files " });
        if (openDialog.open() == null) return;
        String newFileName = openDialog.getFileName();
        File file = new File(openDialog.getFilterPath(), newFileName);
        if (!file.exists()) {
            Utils.showMessageBox(shell, "Error", "The file " + newFileName + " does not exist.", SWT.ICON_ERROR);
            return;
        }
        open(file);
    }

    /**
	 * Opens the specified file and loads its data into the table.
	 * ============================================================================ 
	 */
    private void open(File openFile) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(openFile));
            ToDoItem[] data = (ToDoItem[]) ois.readObject();
            filter = ((Filter) ois.readObject());
            ois.close();
            newTable(false);
            table.setRedraw(false);
            for (int i = 0; i < data.length; i++) {
                data[i].insertIntoTable(table, i);
            }
            table.setRedraw(true);
            currentFile = openFile;
            currentFilename = openFile.getName();
            updateFileStatus(false);
            colorRows();
            updateItemCount();
        } catch (Exception e) {
            newTable(false);
            System.out.println("Error opening file: " + openFile.getName());
            e.printStackTrace();
            Utils.showMessageBox(shell, "Error", "Error opening file: " + openFile.getName() + "\n\n" + Utils.getStackTraceStr(e), SWT.ICON_ERROR | SWT.OK);
        }
    }

    /**
	 * Creates a blank table for the user by clearing out the current data.
	 * ============================================================================ 
	 */
    private void newTable(boolean prompt) {
        if (prompt && fileModified) {
            int choice = Utils.showMessageBox(shell, "ToDo", "The file " + currentFilename + " has been modified.\nWould you like to save the changes?", SWT.ICON_WARNING | SWT.YES | SWT.NO | SWT.CANCEL);
            if (choice == SWT.YES) {
                save();
            } else if (choice == SWT.CANCEL) {
                return;
            }
        }
        table.removeAll();
        list_filteredItems.clear();
        undoStack.clear();
        redoStack.clear();
        text_quickSearch.setText("");
        currentFile = null;
        currentFilename = "Untitled";
        updateFileStatus(false);
        filter.reset();
        updateItemCount();
    }

    /**
	 * Stores all the ids of the items on the table that are currently selected.
	 * ============================================================================ 
	 */
    private void storeSelectedIds(boolean clearAfter) {
        topIndex = table.getTopIndex();
        list_selectedIds.clear();
        int[] selectedIndices = table.getSelectionIndices();
        for (int i = 0; i < selectedIndices.length; i++) {
            list_selectedIds.add(new Long((getItem(selectedIndices[i])).getId()));
        }
        if (clearAfter) table.deselectAll();
    }

    /**
	 * Selects all the items on the table that have their ids stored.
	 * ============================================================================ 
	 */
    private void restoreSelectedIds(boolean clearFirst) {
        if (clearFirst) table.deselectAll();
        for (int i = 0; i < table.getItemCount(); i++) {
            for (int j = 0; j < list_selectedIds.size(); j++) {
                if ((getItem(i)).getId() == ((Long) list_selectedIds.get(j)).longValue()) {
                    table.select(i);
                    list_selectedIds.remove(j);
                    break;
                }
            }
        }
        table.setTopIndex(topIndex);
    }

    /**
	 * Returns an array containing each Category(exactly once) listed on the table.
	 * ============================================================================ 
	 */
    private String[] getCategories() {
        String[] categories = { "" };
        String category = "";
        stringSet.clear();
        for (int i = 0; i < table.getItemCount(); i++) {
            category = table.getItem(i).getText(Constants.COLUMN_CATGEGORY);
            if (Utils.isNotEmpty(category)) {
                stringSet.add(category);
            }
        }
        for (int i = 0; i < list_filteredItems.size(); i++) {
            category = ((ToDoItem) list_filteredItems.get(i)).getCategory();
            if (Utils.isNotEmpty(category)) {
                stringSet.add(category);
            }
        }
        categories = (String[]) stringSet.toArray(new String[0]);
        return categories;
    }
}
