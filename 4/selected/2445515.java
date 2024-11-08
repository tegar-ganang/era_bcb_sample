package omschaub.stuffer.main;

import java.io.File;
import java.util.HashMap;
import omschaub.stuffer.containers.IP;
import omschaub.stuffer.containers.Table1Container;
import omschaub.stuffer.containers.Table2Container;
import omschaub.stuffer.utilities.ImageRepository;
import omschaub.stuffer.utilities.TableColumnWidthUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.plugins.ui.UIException;

/**
 * Main Tab1 Class.
 * @author marc
 *
 */
public class Tab1 {

    private Label timeNext;

    private Table table1, peer_remove;

    private int peers_count, purge_count;

    private int escPressed;

    private Button setting_table2;

    private Group peerRemoveGroup, group1;

    private String defaultPath;

    private TableColumnWidthUtility table1ColumnWidthUtility, table2ColumnWidthUtility;

    public void open(Composite composite) {
        peers_count = 1;
        purge_count = 1;
        table1ColumnWidthUtility = new TableColumnWidthUtility("table1ColumnWidths");
        table2ColumnWidthUtility = new TableColumnWidthUtility("table2ColumnWidths");
        Composite button_composite = new Composite(composite, SWT.NULL);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END);
        button_composite.setLayoutData(gridData);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        button_composite.setLayout(layout);
        Button rezero = new Button(button_composite, SWT.PUSH);
        rezero.setText("Re-Zero Counters");
        rezero.pack();
        rezero.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                Plugin.total_blocked = 0;
                Plugin.total_removed = 0;
                totalChange();
            }
        });
        Button restart = new Button(button_composite, SWT.PUSH);
        restart.setText("Clear All Lists");
        restart.pack();
        restart.addListener(SWT.Selection, new Listener() {

            public void handleEvent(final Event e) {
                peers_count = 1;
                purge_count = 1;
                Plugin.table1_set.clearSet();
                Plugin.table2_set.clearSet();
                if (table1 != null || !table1.isDisposed()) {
                    table1.setItemCount(1);
                    table1.clearAll();
                }
                if (peer_remove != null || !peer_remove.isDisposed()) {
                    peer_remove.setItemCount(1);
                    peer_remove.clearAll();
                }
            }
        });
        final SashForm sash = new SashForm(composite, SWT.VERTICAL);
        layout = new GridLayout();
        layout.numColumns = 2;
        sash.setLayout(layout);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        gridData.verticalSpan = 5;
        sash.setLayoutData(gridData);
        int[] sash_array = { Plugin.config_getter.getPluginIntParameter("stuffer_sash1", 500), Plugin.config_getter.getPluginIntParameter("stuffer_sash2", 393) };
        group1 = new Group(sash, SWT.BORDER);
        group1.setText("Client Blocking Information (Total Blocks: " + Plugin.total_blocked + ")");
        layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        group1.setLayout(layout);
        Composite tool_comp = new Composite(group1, SWT.NULL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        tool_comp.setLayoutData(gridData);
        layout = new GridLayout();
        layout.numColumns = 6;
        layout.marginHeight = 0;
        tool_comp.setLayout(layout);
        final Button pause = new Button(tool_comp, SWT.TOGGLE);
        pause.setImage(ImageRepository.getImage("pause"));
        pause.setToolTipText("Pause ALL filtering rules");
        pause.pack();
        pause.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                Plugin.areRulesPaused = pause.getSelection();
                if (Plugin.getDisplay() == null && Plugin.getDisplay().isDisposed()) return;
                Plugin.getDisplay().syncExec(new Runnable() {

                    public void run() {
                        table1.setEnabled(!Plugin.areRulesPaused);
                    }
                });
            }
        });
        Button clear_table1 = new Button(tool_comp, SWT.PUSH);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gridData.grabExcessHorizontalSpace = true;
        clear_table1.setLayoutData(gridData);
        clear_table1.setText("Clear List");
        clear_table1.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                if (table1 != null || !table1.isDisposed()) {
                    peers_count = 1;
                    Plugin.table1_set.clearSet();
                    if (table1 != null || !table1.isDisposed()) {
                        table1.setItemCount(1);
                        table1.clearAll();
                    }
                }
            }
        });
        draw_table1();
        peerRemoveGroup = new Group(sash, SWT.BORDER);
        peerRemoveGroup.setText("IPFilter Removal Information (Total Filters Purged: " + (Plugin.total_removed) + ")");
        layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        peerRemoveGroup.setLayout(layout);
        Composite tool_comp2 = new Composite(peerRemoveGroup, SWT.NULL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        tool_comp2.setLayoutData(gridData);
        layout = new GridLayout();
        layout.numColumns = 10;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        tool_comp2.setLayout(layout);
        final Composite settingComp = new Composite(tool_comp2, SWT.NULL);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        settingComp.setLayout(layout);
        setting_table2 = new Button(settingComp, SWT.TOGGLE);
        final Text timer = new Text(settingComp, SWT.BORDER);
        timer.setToolTipText("Duration between each automatic peer removal in minutes (range is between 1 and 10000)");
        timer.addVerifyListener(new VerifyListener() {

            public void verifyText(VerifyEvent event) {
                event.doit = event.text.length() == 0 || Character.isDigit(event.text.charAt(0));
            }
        });
        timer.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent arg0) {
                if (timer.getText().equalsIgnoreCase("") || Integer.parseInt(timer.getText()) == 0) {
                    timer.setText("1");
                }
                if (Integer.parseInt(timer.getText()) > 10000) {
                    timer.setText("10000");
                }
                Plugin.config_getter.setPluginParameter("stuffer_time_interval", Integer.parseInt(timer.getText()));
                Plugin.resetTimer();
            }
        });
        timer.setText(String.valueOf(Plugin.config_getter.getPluginIntParameter("stuffer_time_interval", 10)));
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.widthHint = 30;
        timer.setLayoutData(gridData);
        final Button restart_timer = new Button(tool_comp2, SWT.PUSH);
        setting_table2.setImage(ImageRepository.getImage("turn_off"));
        boolean no_auto_peer_removal = Plugin.config_getter.getPluginBooleanParameter("stuffer_noauto", false);
        setting_table2.setSelection(no_auto_peer_removal);
        if (no_auto_peer_removal) {
            setting_table2.setToolTipText("Click to turn on automatic peer removal");
            timer.setEnabled(false);
        } else {
            setting_table2.setToolTipText("Click to turn off automatic peer removal");
            timer.setEnabled(true);
        }
        setting_table2.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                if (setting_table2.getSelection()) {
                    Plugin.config_getter.setPluginParameter("stuffer_noauto", true);
                    setting_table2.setToolTipText("Click to Turn on automatic peer removal");
                    timer.setEnabled(false);
                    restart_timer.setEnabled(false);
                    timeNext.setVisible(false);
                } else {
                    Plugin.config_getter.setPluginParameter("stuffer_noauto", false);
                    setting_table2.setToolTipText("Click to Turn off automatic peer removal");
                    timer.setEnabled(true);
                    restart_timer.setEnabled(true);
                    timeNext.setVisible(true);
                }
            }
        });
        restart_timer.setText("Restart Timer");
        restart_timer.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                Plugin.resetTimer();
            }
        });
        final Button manualPurge = new Button(tool_comp2, SWT.PUSH);
        manualPurge.setImage(ImageRepository.getImage("trashcan"));
        manualPurge.setToolTipText("Manually Purge IPFilter List");
        manualPurge.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                Plugin.manualPurge();
            }
        });
        timeNext = new Label(tool_comp2, SWT.NULL);
        timeNext.setText("Next Auto Purge: " + Utils.getNextRunTime((1000 * 60 * Plugin.config_getter.getPluginIntParameter("stuffer_time_interval", 10)), Plugin.config_getter.getPluginBooleanParameter("stuffer_military_time", false)));
        if (setting_table2.getSelection()) {
            restart_timer.setEnabled(false);
            timeNext.setVisible(false);
        } else {
            timeNext.setVisible(true);
            restart_timer.setEnabled(true);
        }
        Button clear_table2 = new Button(tool_comp2, SWT.PUSH);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gridData.grabExcessHorizontalSpace = true;
        clear_table2.setLayoutData(gridData);
        clear_table2.setText("Clear List");
        clear_table2.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                if (peer_remove != null || !peer_remove.isDisposed()) {
                    purge_count = 1;
                    Plugin.table2_set.clearSet();
                    peer_remove.setItemCount(1);
                    peer_remove.clearAll();
                }
            }
        });
        draw_peer_remove_table();
        sash.setWeights(sash_array);
        group1.addListener(SWT.Resize, new Listener() {

            public void handleEvent(Event e) {
                if (group1 == null || group1.isDisposed()) return;
                if (sash == null || sash.isDisposed()) return;
                int[] sash_weight_array = sash.getWeights();
                Plugin.config_getter.setPluginParameter("stuffer_sash1", sash_weight_array[0]);
                Plugin.config_getter.setPluginParameter("stuffer_sash2", sash_weight_array[1]);
                if (table1 == null || table1.isDisposed()) return;
                table1.setTopIndex(table1.getItemCount() - 1);
            }
        });
    }

    /**
     * Adds a item to the table in tab1
     *
     * @param peerIP
     * @param peerID
     * @param peerClient
     * @param downloadName
     * @param rgb
     */
    public void addElement(final String peerIP, final String peerID, final String peerClient, final String downloadName, final String rgb) {
        IP ip = new IP(peerIP);
        Plugin.table1_set.addToSet(new Table1Container(Plugin.table1_set.getNum() + 1, Utils.getCurrentTime(), peerClient, peerID, downloadName, rgb, ip));
        Plugin.total_blocked++;
        peers_count++;
        if (Plugin.getDisplay() != null && !Plugin.getDisplay().isDisposed()) {
            Plugin.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    if (table1 != null && !table1.isDisposed()) {
                        try {
                            boolean scroll_seek = false;
                            if ((table1.getVerticalBar().getMaximum() - (table1.getVerticalBar().getSelection() + table1.getVerticalBar().getThumb())) == 0) {
                                scroll_seek = true;
                            }
                            if (table1.isDisposed()) return;
                            table1.setItemCount(Plugin.table1_set.getNum());
                            table1.clearAll();
                            totalChange();
                            if (scroll_seek) {
                                table1.setTopIndex(table1.getItemCount() - 1);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    public void addElementPeer(final String iprDescription, final String iprStartIP, final String type) {
        String subString;
        if (iprDescription.startsWith("Stuffer")) subString = iprDescription.substring(iprDescription.indexOf("Stuffer - ") + 10, iprDescription.indexOf("Killed")); else if (iprDescription.startsWith("stuffer")) subString = iprDescription.substring(iprDescription.indexOf("stuffer - ") + 10, iprDescription.indexOf("killed")); else subString = "Error Decoding Title";
        Plugin.table2_set.addToSet(new Table2Container(Plugin.table2_set.getNum() + 1, Utils.getCurrentTime(), type, subString, new IP(iprStartIP), iprDescription));
        purge_count++;
        if (Plugin.getDisplay() != null && !Plugin.getDisplay().isDisposed()) {
            Plugin.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    if (peer_remove != null && !peer_remove.isDisposed()) {
                        boolean scroll_seek = false;
                        if ((peer_remove.getVerticalBar().getMaximum() - (peer_remove.getVerticalBar().getSelection() + peer_remove.getVerticalBar().getThumb())) == 0) {
                            scroll_seek = true;
                        }
                        if (peer_remove.isDisposed()) return;
                        peer_remove.setItemCount(Plugin.table2_set.getNum());
                        peer_remove.clearAll();
                        if (scroll_seek) {
                            peer_remove.setTopIndex(peer_remove.getItemCount() - 1);
                        }
                    }
                }
            });
        }
    }

    public void removeAll(final List list) {
        if (Plugin.getDisplay() != null && !Plugin.getDisplay().isDisposed()) {
            Plugin.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    if (list != null && !list.isDisposed()) {
                        list.removeAll();
                    }
                }
            });
        }
    }

    public void totalChange() {
        if (Plugin.getPluginInterface().getUtilities().isOSX()) return;
        if (Plugin.getDisplay() != null && !Plugin.getDisplay().isDisposed()) {
            Plugin.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    try {
                        if (group1 != null && !group1.isDisposed()) {
                            group1.setText("Client Blocking Information (Total Blocks: " + (Plugin.total_blocked) + ")");
                            peerRemoveGroup.setText("IPFilter Removal Information (Total Filters Purged: " + (Plugin.total_removed) + ")");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void draw_table1() {
        if (Plugin.getDisplay() != null && !Plugin.getDisplay().isDisposed()) {
            Plugin.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    if (table1 != null && !table1.isDisposed()) {
                        table1.dispose();
                    }
                    table1 = new Table(group1, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
                    GridData gridData = new GridData(GridData.FILL_BOTH);
                    gridData.horizontalSpan = 1;
                    gridData.verticalSpan = 5;
                    table1.setLayoutData(gridData);
                    table1.setHeaderVisible(true);
                    final TableColumn number = new TableColumn(table1, SWT.CENTER);
                    number.setText("#");
                    number.setWidth(50);
                    final TableColumn date = new TableColumn(table1, SWT.LEFT);
                    date.setText("Time");
                    date.setWidth(130);
                    final TableColumn peer_IP = new TableColumn(table1, SWT.LEFT);
                    peer_IP.setText("IP");
                    peer_IP.setWidth(100);
                    final TableColumn peer_clientName = new TableColumn(table1, SWT.LEFT);
                    peer_clientName.setText("Client");
                    peer_clientName.setWidth(150);
                    final TableColumn peer_ID = new TableColumn(table1, SWT.LEFT);
                    peer_ID.setText("ID");
                    peer_ID.setWidth(100);
                    final TableColumn download = new TableColumn(table1, SWT.LEFT);
                    download.setText("Torrent");
                    download.setWidth(200);
                    try {
                        HashMap map = table1ColumnWidthUtility.getMap();
                        if (map != null) {
                            TableColumn[] columns = table1.getColumns();
                            for (int i = 0; i < columns.length; i++) {
                                String value = (String) map.get(columns[i].getText());
                                if (value != null) {
                                    int tempNum = Integer.parseInt((String) map.get(columns[i].getText()));
                                    if (tempNum > 0) columns[i].setWidth(tempNum);
                                } else {
                                    if (columns[i].getText().equalsIgnoreCase("Filter")) {
                                        columns[i].setWidth(200);
                                    } else {
                                        columns[i].pack();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T1_Numbers", true)) {
                        number.dispose();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T1_Time", true)) {
                        date.dispose();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T1_Client", true)) {
                        peer_clientName.dispose();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T1_Client_id", false)) {
                        peer_ID.dispose();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T1_Torrent", true)) {
                        download.dispose();
                    }
                    Plugin.getDisplay().syncExec(new Runnable() {

                        public void run() {
                            if (table1.isDisposed()) return;
                            table1.setItemCount(Plugin.table1_set.getNum());
                            table1.clearAll();
                        }
                    });
                    TableColumn[] table_columns = table1.getColumns();
                    for (int i = 0; i < table_columns.length; i++) {
                        if (table_columns[i].getText().equalsIgnoreCase("#")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table1_set.sortByIndex(true));
                        } else if (table_columns[i].getText().equalsIgnoreCase("IP")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table1_set.sortByIP(true));
                        } else if (table_columns[i].getText().equalsIgnoreCase("Time")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table1_set.sortTable1ByDate());
                        } else if (table_columns[i].getText().equalsIgnoreCase("Torrent")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table1_set.sortTable1ByTorrent());
                        } else if (table_columns[i].getText().equalsIgnoreCase("Client")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table1_set.sortTable1ByClient());
                        } else if (table_columns[i].getText().equalsIgnoreCase("ID")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table1_set.sortTable1ByID());
                        }
                        table_columns[i].addControlListener(getResizeListener(1));
                    }
                    table1.addListener(SWT.SetData, new Listener() {

                        public void handleEvent(Event e) {
                            try {
                                TableItem item = (TableItem) e.item;
                                int index = table1.indexOf(item);
                                if (index % 2 == 0) {
                                    item.setBackground(ColorUtilities.getBackgroundColor());
                                }
                                if (table1 == null || table1.isDisposed()) return;
                                TableColumn[] columns = table1.getColumns();
                                String[] stringItems;
                                try {
                                    stringItems = Plugin.table1_set.getTable1ContainerArray()[index].getTableItemsAsString();
                                } catch (ArrayIndexOutOfBoundsException e2) {
                                    return;
                                }
                                for (int i = 0; i < columns.length; i++) {
                                    String columnName = columns[i].getText();
                                    if (columnName.equalsIgnoreCase("#")) {
                                        item.setText(i, stringItems[0]);
                                    } else if (columnName.equalsIgnoreCase("Time")) {
                                        item.setText(i, stringItems[1]);
                                    } else if (columnName.equalsIgnoreCase("IP")) {
                                        item.setText(i, stringItems[2]);
                                    } else if (columnName.equalsIgnoreCase("Client")) {
                                        item.setText(i, stringItems[3]);
                                    } else if (columnName.equalsIgnoreCase("ID")) {
                                        item.setText(i, stringItems[4]);
                                    } else if (columnName.equalsIgnoreCase("Torrent")) {
                                        item.setText(i, stringItems[5]);
                                    }
                                }
                                Color temp_color = new Color(Plugin.getDisplay(), Utils.getRGB(stringItems[6]));
                                item.setForeground(temp_color);
                                temp_color.dispose();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    table1.addMouseListener(new MouseAdapter() {

                        public void mouseDown(MouseEvent e) {
                            if (e.button == 1) {
                                if (table1.getItem(new Point(e.x, e.y)) == null) {
                                    table1.deselectAll();
                                }
                            }
                        }
                    });
                    Menu popupmenu_table = new Menu(group1);
                    final MenuItem dump = new MenuItem(popupmenu_table, SWT.PUSH);
                    dump.setText("Save table contents to a file");
                    dump.setEnabled(false);
                    dump.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event e) {
                            try {
                                TableItem[] items = table1.getItems();
                                if (items.length == 0) {
                                    MessageBox messageBox = new MessageBox(peerRemoveGroup.getShell(), SWT.ICON_ERROR | SWT.OK);
                                    messageBox.setText("Table is Empty");
                                    messageBox.setMessage("The table is empty, therefore nothing can be written to a file.");
                                    messageBox.open();
                                    return;
                                }
                                FileDialog fileDialog = new FileDialog(group1.getShell(), SWT.SAVE);
                                fileDialog.setText("Please choose a file to save the information to");
                                String[] filterExtensions = { "*.txt", "*.log", "*.*" };
                                fileDialog.setFilterExtensions(filterExtensions);
                                if (defaultPath == null) {
                                    defaultPath = Plugin.getPluginInterface().getPluginDirectoryName();
                                }
                                fileDialog.setFilterPath(defaultPath);
                                String selectedFile = fileDialog.open();
                                if (selectedFile != null) {
                                    final File fileToSave = new File(selectedFile);
                                    defaultPath = fileToSave.getParent();
                                    if (fileToSave.exists()) {
                                        if (!fileToSave.canWrite()) {
                                            MessageBox messageBox = new MessageBox(group1.getShell(), SWT.ICON_ERROR | SWT.OK);
                                            messageBox.setText("Error writing to file");
                                            messageBox.setMessage("Your computer is reporting that the selected file cannot be written to, please retry this operation and select a different file");
                                            messageBox.open();
                                            return;
                                        }
                                        final Shell shell = new Shell(SWT.DIALOG_TRIM);
                                        shell.setLayout(new GridLayout(3, false));
                                        shell.setText("File Exists");
                                        Label message = new Label(shell, SWT.NULL);
                                        message.setText("Your selected file already exists. \n" + "Choose 'Overwrite' to overwrite it, deleting the original contents \n" + "Choose 'Append' to append the information to the existing file \n" + "Choose 'Cancel' to abort this action all together\n\n");
                                        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                                        gridData.horizontalSpan = 3;
                                        message.setLayoutData(gridData);
                                        Button overwrite = new Button(shell, SWT.PUSH);
                                        overwrite.setText("Overwrite");
                                        overwrite.addListener(SWT.Selection, new Listener() {

                                            public void handleEvent(Event e) {
                                                shell.close();
                                                shell.dispose();
                                                FileUtilities.writeToLog(Plugin.table1_set, fileToSave, false);
                                            }
                                        });
                                        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                        overwrite.setLayoutData(gridData);
                                        Button append = new Button(shell, SWT.PUSH);
                                        append.setText("Append");
                                        append.addListener(SWT.Selection, new Listener() {

                                            public void handleEvent(Event e) {
                                                shell.close();
                                                shell.dispose();
                                                FileUtilities.writeToLog(Plugin.table1_set, fileToSave, true);
                                            }
                                        });
                                        Button cancel = new Button(shell, SWT.PUSH);
                                        cancel.setText("Cancel");
                                        cancel.addListener(SWT.Selection, new Listener() {

                                            public void handleEvent(Event e) {
                                                shell.close();
                                                shell.dispose();
                                            }
                                        });
                                        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                        cancel.setLayoutData(gridData);
                                        overwrite.addKeyListener(new KeyListener() {

                                            public void keyPressed(KeyEvent e) {
                                                switch(e.character) {
                                                    case SWT.ESC:
                                                        escPressed = 1;
                                                        break;
                                                }
                                            }

                                            public void keyReleased(KeyEvent e) {
                                                if (escPressed == 1) {
                                                    escPressed = 0;
                                                    shell.close();
                                                    shell.dispose();
                                                }
                                            }
                                        });
                                        Utils.centerShellandOpen(shell);
                                    } else {
                                        fileToSave.createNewFile();
                                        FileUtilities.writeToLog(Plugin.table1_set, fileToSave, true);
                                    }
                                }
                            } catch (Exception f) {
                                f.printStackTrace();
                                MessageBox messageBox = new MessageBox(group1.getShell(), SWT.ICON_ERROR | SWT.OK);
                                messageBox.setText("Error writing to file");
                                messageBox.setMessage("Your computer is reporting that the selected file cannot be written to, please retry this operation and select a different file");
                                messageBox.open();
                            }
                        }
                    });
                    final MenuItem copyClip = new MenuItem(popupmenu_table, SWT.PUSH);
                    copyClip.setText("Copy selected line(s) to clipboard");
                    copyClip.setEnabled(false);
                    copyClip.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event e) {
                            try {
                                String item_text = new String();
                                TableItem[] item = table1.getSelection();
                                int[] indices = table1.getSelectionIndices();
                                if (item.length == 0) {
                                    return;
                                } else if (item.length > 0) {
                                    for (int i = 0; i < item.length; i++) {
                                        String[] itemString = Plugin.table1_set.getTable1ContainerArray()[indices[i]].getTableItemsAsString();
                                        for (int j = 0; j < 6; j++) {
                                            item_text = item_text + " | " + itemString[j];
                                        }
                                        item_text = item_text + "\n";
                                    }
                                }
                                Plugin.getPluginInterface().getUIManager().copyToClipBoard(item_text);
                            } catch (UIException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    MenuItem seperator = new MenuItem(popupmenu_table, SWT.SEPARATOR);
                    seperator.setText("null");
                    MenuItem setup = new MenuItem(popupmenu_table, SWT.PUSH);
                    setup.setText("Table Setup");
                    setup.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event e) {
                            try {
                                Tab1Customization tab1cust = new Tab1Customization();
                                tab1cust.clientBlockingCustomizationOpen();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    table1.setMenu(popupmenu_table);
                    popupmenu_table.addMenuListener(new MenuListener() {

                        public void menuHidden(MenuEvent arg0) {
                        }

                        public void menuShown(MenuEvent arg0) {
                            dump.setEnabled(false);
                            copyClip.setEnabled(false);
                            TableItem[] item = table1.getSelection();
                            if (item.length > 0) {
                                copyClip.setEnabled(true);
                            }
                            if (table1.getItemCount() > 0) {
                                dump.setEnabled(true);
                            }
                        }
                    });
                    group1.layout();
                }
            });
        }
    }

    public void draw_peer_remove_table() {
        if (Plugin.getDisplay() != null && !Plugin.getDisplay().isDisposed()) {
            Plugin.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    if (peer_remove != null && !peer_remove.isDisposed()) {
                        peer_remove.dispose();
                    }
                    peer_remove = new Table(peerRemoveGroup, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
                    GridData gridData = new GridData(GridData.FILL_BOTH);
                    gridData.horizontalSpan = 1;
                    gridData.verticalSpan = 5;
                    peer_remove.setLayoutData(gridData);
                    peer_remove.setHeaderVisible(true);
                    final TableColumn number = new TableColumn(peer_remove, SWT.CENTER);
                    number.setText("#");
                    number.setWidth(50);
                    final TableColumn date = new TableColumn(peer_remove, SWT.LEFT);
                    date.setText("Time");
                    date.setWidth(130);
                    final TableColumn type = new TableColumn(peer_remove, SWT.LEFT);
                    type.setText("Type");
                    type.setWidth(100);
                    final TableColumn peer_clientName = new TableColumn(peer_remove, SWT.LEFT);
                    peer_clientName.setText("Client");
                    peer_clientName.setWidth(150);
                    final TableColumn peer_IP = new TableColumn(peer_remove, SWT.LEFT);
                    peer_IP.setText("IP");
                    peer_IP.setWidth(100);
                    try {
                        HashMap map = table2ColumnWidthUtility.getMap();
                        if (map != null) {
                            TableColumn[] columns = peer_remove.getColumns();
                            for (int i = 0; i < columns.length; i++) {
                                String value = (String) map.get(columns[i].getText());
                                if (value != null) {
                                    int tempNum = Integer.parseInt((String) map.get(columns[i].getText()));
                                    if (tempNum > 0) columns[i].setWidth(tempNum);
                                } else {
                                    columns[i].setWidth(150);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T2_Numbers", true)) {
                        number.dispose();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T2_Time", true)) {
                        date.dispose();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T2_Manual", true)) {
                        type.dispose();
                    }
                    if (!Plugin.getPluginInterface().getPluginconfig().getPluginBooleanParameter("Stuffer_T2_Client_ip", true)) {
                        peer_IP.dispose();
                    }
                    Plugin.getDisplay().syncExec(new Runnable() {

                        public void run() {
                            if (peer_remove.isDisposed()) return;
                            peer_remove.setItemCount(Plugin.table2_set.getNum());
                            peer_remove.clearAll();
                        }
                    });
                    TableColumn[] table_columns = peer_remove.getColumns();
                    for (int i = 0; i < table_columns.length; i++) {
                        if (table_columns[i].getText().equalsIgnoreCase("#")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table2_set.sortByIndex(false));
                        } else if (table_columns[i].getText().equalsIgnoreCase("IP")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table2_set.sortByIP(false));
                        } else if (table_columns[i].getText().equalsIgnoreCase("Time")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table2_set.sortTable2ByDate());
                        } else if (table_columns[i].getText().equalsIgnoreCase("Type")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table2_set.sortTable2ByType());
                        } else if (table_columns[i].getText().equalsIgnoreCase("Client")) {
                            table_columns[i].addListener(SWT.Selection, Plugin.table2_set.sortTable2ByClient());
                        }
                        table_columns[i].addControlListener(getResizeListener(2));
                    }
                    peer_remove.addMouseListener(new MouseAdapter() {

                        public void mouseDown(MouseEvent e) {
                            if (e.button == 1) {
                                if (peer_remove.getItem(new Point(e.x, e.y)) == null) {
                                    peer_remove.deselectAll();
                                }
                            }
                        }
                    });
                    peer_remove.addListener(SWT.SetData, new Listener() {

                        public void handleEvent(Event e) {
                            TableItem item = (TableItem) e.item;
                            int index = peer_remove.indexOf(item);
                            if (index % 2 == 0) {
                                item.setBackground(ColorUtilities.getBackgroundColor());
                            }
                            TableColumn[] columns = peer_remove.getColumns();
                            String[] itemString;
                            try {
                                itemString = Plugin.table2_set.getTable2ContainerArray()[index].getTableItemsAsString();
                            } catch (ArrayIndexOutOfBoundsException e2) {
                                return;
                            }
                            for (int i = 0; i < columns.length; i++) {
                                if (columns[i].getText().equalsIgnoreCase("#")) {
                                    item.setText(i, itemString[0]);
                                } else if (columns[i].getText().equalsIgnoreCase("Time")) {
                                    item.setText(i, itemString[1]);
                                } else if (columns[i].getText().equalsIgnoreCase("Type")) {
                                    item.setText(i, itemString[2]);
                                } else if (columns[i].getText().equalsIgnoreCase("Client")) {
                                    item.setText(i, itemString[3]);
                                } else if (columns[i].getText().equalsIgnoreCase("IP")) {
                                    item.setText(i, itemString[4]);
                                }
                            }
                            Color temp_color = new Color(Plugin.getDisplay(), Utils.getRGBfromHex(itemString[5]));
                            item.setForeground(temp_color);
                            temp_color.dispose();
                        }
                    });
                    Menu popupmenu_table = new Menu(peerRemoveGroup);
                    final MenuItem dump = new MenuItem(popupmenu_table, SWT.PUSH);
                    dump.setText("Save table contents to a file");
                    dump.setEnabled(false);
                    dump.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event e) {
                            try {
                                TableItem[] items = table1.getItems();
                                if (items.length == 0) {
                                    MessageBox messageBox = new MessageBox(peerRemoveGroup.getShell(), SWT.ICON_ERROR | SWT.OK);
                                    messageBox.setText("Table is Empty");
                                    messageBox.setMessage("The table is empty, therefore nothing can be written to a file.");
                                    messageBox.open();
                                    return;
                                }
                                FileDialog fileDialog = new FileDialog(group1.getShell(), SWT.SAVE);
                                fileDialog.setText("Please choose a file to save the information to");
                                String[] filterExtensions = { "*.txt", "*.log", "*.*" };
                                fileDialog.setFilterExtensions(filterExtensions);
                                if (defaultPath == null) {
                                    defaultPath = Plugin.getPluginInterface().getPluginDirectoryName();
                                }
                                fileDialog.setFilterPath(defaultPath);
                                String selectedFile = fileDialog.open();
                                if (selectedFile != null) {
                                    final File fileToSave = new File(selectedFile);
                                    defaultPath = fileToSave.getParent();
                                    if (fileToSave.exists()) {
                                        if (!fileToSave.canWrite()) {
                                            MessageBox messageBox = new MessageBox(group1.getShell(), SWT.ICON_ERROR | SWT.OK);
                                            messageBox.setText("Error writing to file");
                                            messageBox.setMessage("Your computer is reporting that the selected file cannot be written to, please retry this operation and select a different file");
                                            messageBox.open();
                                            return;
                                        }
                                        final Shell shell = new Shell(SWT.DIALOG_TRIM);
                                        shell.setLayout(new GridLayout(3, false));
                                        shell.setText("File Exists");
                                        Label message = new Label(shell, SWT.NULL);
                                        message.setText("Your selected file already exists. \n" + "Choose 'Overwrite' to overwrite it, deleting the original contents \n" + "Choose 'Append' to append the information to the existing file \n" + "Choose 'Cancel' to abort this action all together\n\n");
                                        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                                        gridData.horizontalSpan = 3;
                                        message.setLayoutData(gridData);
                                        Button overwrite = new Button(shell, SWT.PUSH);
                                        overwrite.setText("Overwrite");
                                        overwrite.addListener(SWT.Selection, new Listener() {

                                            public void handleEvent(Event e) {
                                                shell.close();
                                                shell.dispose();
                                                FileUtilities.writeToLog(Plugin.table2_set, fileToSave, false);
                                            }
                                        });
                                        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                        overwrite.setLayoutData(gridData);
                                        Button append = new Button(shell, SWT.PUSH);
                                        append.setText("Append");
                                        append.addListener(SWT.Selection, new Listener() {

                                            public void handleEvent(Event e) {
                                                shell.close();
                                                shell.dispose();
                                                FileUtilities.writeToLog(Plugin.table2_set, fileToSave, true);
                                            }
                                        });
                                        Button cancel = new Button(shell, SWT.PUSH);
                                        cancel.setText("Cancel");
                                        cancel.addListener(SWT.Selection, new Listener() {

                                            public void handleEvent(Event e) {
                                                shell.close();
                                                shell.dispose();
                                            }
                                        });
                                        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
                                        cancel.setLayoutData(gridData);
                                        overwrite.addKeyListener(new KeyListener() {

                                            public void keyPressed(KeyEvent e) {
                                                switch(e.character) {
                                                    case SWT.ESC:
                                                        escPressed = 1;
                                                        break;
                                                }
                                            }

                                            public void keyReleased(KeyEvent e) {
                                                if (escPressed == 1) {
                                                    escPressed = 0;
                                                    shell.close();
                                                    shell.dispose();
                                                }
                                            }
                                        });
                                        Utils.centerShellandOpen(shell);
                                    } else {
                                        fileToSave.createNewFile();
                                        FileUtilities.writeToLog(Plugin.table2_set, fileToSave, true);
                                    }
                                }
                            } catch (Exception f) {
                                f.printStackTrace();
                                MessageBox messageBox = new MessageBox(group1.getShell(), SWT.ICON_ERROR | SWT.OK);
                                messageBox.setText("Error writing to file");
                                messageBox.setMessage("Your computer is reporting that the selected file cannot be written to, please retry this operation and select a different file");
                                messageBox.open();
                            }
                        }
                    });
                    final MenuItem copyClip = new MenuItem(popupmenu_table, SWT.PUSH);
                    copyClip.setText("Copy selected line(s) to clipboard");
                    copyClip.setEnabled(false);
                    copyClip.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event e) {
                            try {
                                String item_text = new String();
                                TableItem[] item = peer_remove.getSelection();
                                int[] indices = peer_remove.getSelectionIndices();
                                if (item.length == 0) {
                                    return;
                                } else if (item.length > 0) {
                                    for (int i = 0; i < item.length; i++) {
                                        String[] itemString = Plugin.table2_set.getTable2ContainerArray()[indices[i]].getTableItemsAsString();
                                        for (int j = 0; j < 6; j++) {
                                            item_text = item_text + " | " + itemString[j];
                                        }
                                        item_text = item_text + "\n";
                                    }
                                }
                                Plugin.getPluginInterface().getUIManager().copyToClipBoard(item_text);
                            } catch (UIException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    MenuItem seperator = new MenuItem(popupmenu_table, SWT.SEPARATOR);
                    seperator.setText("null");
                    MenuItem setup = new MenuItem(popupmenu_table, SWT.PUSH);
                    setup.setText("Table Setup");
                    setup.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event e) {
                            try {
                                Tab1Customization tab1cust = new Tab1Customization();
                                tab1cust.ipFilterRemovalInformationCustomizationOpen();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    peer_remove.setMenu(popupmenu_table);
                    popupmenu_table.addMenuListener(new MenuListener() {

                        public void menuHidden(MenuEvent arg0) {
                        }

                        public void menuShown(MenuEvent arg0) {
                            dump.setEnabled(false);
                            copyClip.setEnabled(false);
                            TableItem[] item = peer_remove.getSelection();
                            if (item.length > 0) {
                                copyClip.setEnabled(true);
                            }
                            if (peer_remove.getItemCount() > 0) {
                                dump.setEnabled(true);
                            }
                        }
                    });
                    peerRemoveGroup.layout();
                }
            });
        }
    }

    public ControlListener getResizeListener(final int table_num) {
        ControlListener cl = new ControlListener() {

            public void controlMoved(ControlEvent arg0) {
            }

            public void controlResized(ControlEvent arg0) {
                if (table_num == 1) {
                    if (table1 != null || !table1.isDisposed()) {
                        TableColumn[] columns_for_table1 = table1.getColumns();
                        String total = new String();
                        for (int i = 0; i < columns_for_table1.length; i++) {
                            String name = columns_for_table1[i].getText();
                            int width = columns_for_table1[i].getWidth();
                            total = total + name + ":" + String.valueOf(width);
                            if (i != columns_for_table1.length - 1) {
                                total = total + ";";
                            }
                        }
                        table1ColumnWidthUtility.setPluginConfigVariable(total);
                    }
                } else if (table_num == 2) {
                    if (peer_remove != null || !peer_remove.isDisposed()) {
                        TableColumn[] columns_for_peer_remove = peer_remove.getColumns();
                        String total = new String();
                        for (int i = 0; i < columns_for_peer_remove.length; i++) {
                            String name = columns_for_peer_remove[i].getText();
                            int width = columns_for_peer_remove[i].getWidth();
                            total = total + name + ":" + String.valueOf(width);
                            if (i != columns_for_peer_remove.length - 1) {
                                total = total + ";";
                            }
                        }
                        table2ColumnWidthUtility.setPluginConfigVariable(total);
                    }
                }
            }
        };
        return cl;
    }

    public void setTimeNextLabel(final String text) {
        if (Plugin.getDisplay() != null && !Plugin.getDisplay().isDisposed()) {
            Plugin.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    if (timeNext != null && !timeNext.isDisposed()) timeNext.setText(text);
                }
            });
        }
    }

    public Table getTable1() {
        return table1;
    }

    /**
     * Gets the peer_remove table.
     * @return peer_remove
     */
    public Table getPeerRemoveTable() {
        return peer_remove;
    }
}
