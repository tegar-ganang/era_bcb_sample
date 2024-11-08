package omschaub.episodetracker.epitrack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.swt.SWT;
import omschaub.episodetracker.epitrack.EpiMaker;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.custom.TableTreeEditor;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginView;

public class View extends PluginView {

    private Display display;

    private Composite composite;

    EpiMaker epiMaker;

    boolean tableCounter = false;

    String name;

    String lines[] = new String[10];

    Table listEps;

    TableTree listEpsTree;

    PluginInterface pluginInterface;

    public View(PluginInterface pluginInterface) {
        this.pluginInterface = pluginInterface;
    }

    /**
	 * The Plugin name, as it'll be seen within the Plugins Menu in azureus
	 */
    public String getPluginViewName() {
        return "EpiTrack";
    }

    /**
	 * The plugin Title, used for its Tab name in the main window
	 */
    public String getFullTitle() {
        return "EpiTrack";
    }

    /**
	 * Here stands any GUI initialisation
	 */
    public void initialize(Composite parent) {
        this.display = parent.getDisplay();
        this.composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        composite.setLayout(layout);
        listEpsTree = new TableTree(composite, SWT.FULL_SELECTION | SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 3;
        listEpsTree.setLayoutData(gridData);
        listEps = listEpsTree.getTable();
        listEps.setHeaderVisible(true);
        TableColumn epiNameColumn = new TableColumn(listEps, SWT.NULL);
        epiNameColumn.setText("Series Name");
        epiNameColumn.setWidth(300);
        TableColumn epiNumberColumn = new TableColumn(listEps, SWT.NULL);
        epiNumberColumn.setText("Episode Number");
        epiNumberColumn.setWidth(300);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 3;
        listEps.setLayoutData(gridData);
        final TableTreeEditor editor = new TableTreeEditor(listEpsTree);
        listEps.addMouseListener(new MouseAdapter() {

            public void mouseDoubleClick(MouseEvent event) {
                if (listEps == null || listEps.isDisposed()) return;
                TableItem[] items = listEps.getSelection();
                if (items.length == 1) {
                    name = items[0].getText();
                    epiMaker = new EpiMaker();
                    lines = epiMaker.commentOpen(pluginInterface, name);
                    inLineEdit(editor);
                }
            }
        });
        listEpsTree.addTreeListener(new TreeListener() {

            public void treeCollapsed(TreeEvent arg0) {
                System.out.println("Collapsed");
            }

            public void treeExpanded(TreeEvent arg0) {
                System.out.println("Expanded");
            }
        });
        Menu popupmenu = new Menu(composite);
        MenuItem deleteItem = new MenuItem(popupmenu, SWT.PUSH);
        deleteItem.setText("Delete from List");
        deleteItem.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                try {
                    if (listEps == null || listEps.isDisposed()) return;
                    TableItem[] items = listEps.getSelection();
                    if (items.length == 1) {
                        TableItem item = items[0];
                        String nameToDie = item.getText(0);
                        String episode = item.getText(1);
                        item.dispose();
                        deleteFromList(nameToDie, episode);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        listEps.setMenu(popupmenu);
        Button refresh = new Button(composite, SWT.PUSH);
        refresh.setText("Refresh List");
        refresh.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                readEpifile();
            }
        });
        Button addNew = new Button(composite, SWT.PUSH);
        addNew.setText("Add New");
        addNew.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                lines[1] = "";
                lines[2] = "";
                openForEdit(lines, true);
            }
        });
        Label info = new Label(composite, SWT.BORDER);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        info.setLayoutData(gridData);
        info.setText("  Double Click Item to Edit Item  /  Right Click to bring up Menu to Delete Item  ");
        readEpifile();
    }

    /**
	 * This method will be called after initialization, in order to grab this
	 * view composite.
	 */
    public Composite getComposite() {
        return this.composite;
    }

    public void readEpifile() {
        try {
            sorter();
            listEps.removeAll();
            String inputLine1, inputLine2;
            String pluginDir = pluginInterface.getPluginDirectoryName();
            String eplist_file = pluginDir + System.getProperty("file.separator") + "EpisodeList.txt";
            File episodeList = new File(eplist_file);
            if (!episodeList.isFile()) {
                episodeList.createNewFile();
            }
            final BufferedReader in = new BufferedReader(new FileReader(episodeList));
            int lineCounter = 1;
            int temp1 = 0;
            int temp2 = 0;
            String[][] epiTemp = new String[1000][2];
            while ((inputLine1 = in.readLine()) != null) {
                if ((inputLine2 = in.readLine()) != null) {
                    epiTemp[temp1][0] = inputLine1;
                    epiTemp[temp1][1] = inputLine2;
                    ++temp1;
                }
            }
            in.close();
            for (int i = 0; i < (temp1); i++) {
                addTableElementDouble(listEpsTree, epiTemp[i][0], epiTemp[i][1]);
            }
        } catch (Exception e) {
        }
    }

    private void addTableElementDouble(final TableTree tableToAdd, final String epiName, final String epiNumber) {
        tableCounter = false;
        if (display == null || display.isDisposed()) return;
        if (tableToAdd == null || tableToAdd.isDisposed()) return;
        int tableTotal = tableToAdd.getItemCount();
        if (tableTotal % 2 == 0) {
            tableCounter = false;
        } else {
            tableCounter = true;
        }
        TableTreeItem[] items = tableToAdd.getItems();
        for (int g = 0; g < items.length; g++) {
            if (items[g].toString().endsWith(epiName)) {
                return;
            }
        }
        TableTreeItem item = new TableTreeItem(tableToAdd, SWT.NULL);
        TableTreeItem subItemURL = new TableTreeItem(item, SWT.NULL);
        TableTreeItem subItemComments = new TableTreeItem(item, SWT.NULL);
        if (tableCounter == true) {
            Color gray_color = new Color(display, 240, 240, 240);
            item.setBackground(gray_color);
            gray_color.dispose();
        }
        item.setText(0, epiName);
        item.setText(1, epiNumber);
        subItemURL.setText("URL: ");
        subItemComments.setText("Comments: ");
        tableCounter = false;
    }

    public String[] openForEdit(final String lines[], boolean editable) {
        final Shell shell = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        shell.setLayout(layout);
        shell.setText("Edit Field");
        Label seriesLabel = new Label(shell, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        seriesLabel.setLayoutData(gridData);
        seriesLabel.setText("Series Name");
        final Text line1 = new Text(shell, SWT.BORDER);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        line1.setLayoutData(gridData);
        line1.setEditable(editable);
        if (editable == false) {
            shell.setText(lines[1]);
            line1.setVisible(false);
        }
        line1.setText(lines[1]);
        Label episodeLabel = new Label(shell, SWT.NONE);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        episodeLabel.setLayoutData(gridData);
        episodeLabel.setText("Episode Number");
        final Text line2 = new Text(shell, SWT.BORDER);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        line2.setLayoutData(gridData);
        line2.setEditable(true);
        Button commit = new Button(shell, SWT.PUSH);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 1;
        commit.setLayoutData(gridData);
        commit.setText("Commit Changes");
        commit.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                lines[1] = line1.getText();
                lines[2] = line2.getText();
                if (lines[1] == null) {
                    shell.close();
                    shell.dispose();
                    return;
                }
                if (lines[2] == null) {
                    lines[2] = "Nothing Entered ";
                }
                epiMaker = new EpiMaker();
                epiMaker.commentWriter(pluginInterface, lines);
                shell.close();
                shell.dispose();
                readEpifile();
            }
        });
        Button cancel = new Button(shell, SWT.PUSH);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 1;
        cancel.setLayoutData(gridData);
        cancel.setText("Cancel");
        cancel.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                shell.close();
                shell.dispose();
            }
        });
        Label spacer = new Label(shell, SWT.PUSH);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 1;
        spacer.setLayoutData(gridData);
        spacer.setText("                                               ");
        line2.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                int escPressed = 0;
                switch(e.character) {
                    case SWT.CR:
                        escPressed = 1;
                        break;
                }
                if (escPressed == 1) {
                    lines[1] = line1.getText();
                    lines[2] = line2.getText();
                    if (lines[1] == null) {
                        shell.close();
                        shell.dispose();
                        return;
                    }
                    if (lines[2] == null) {
                        lines[2] = "Nothing Entered ";
                    }
                    epiMaker = new EpiMaker();
                    epiMaker.commentWriter(pluginInterface, lines);
                    shell.close();
                    shell.dispose();
                    readEpifile();
                }
            }

            public void keyReleased(KeyEvent e) {
            }
        });
        shell.pack();
        Monitor primary = display.getPrimaryMonitor();
        Rectangle bounds = display.getBounds();
        Rectangle rect = shell.getBounds();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;
        shell.setLocation(x, y);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        return lines;
    }

    public void inLineEdit(final TableTreeEditor editor) {
        Control oldEditor = editor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
        TableTreeItem[] selection = listEpsTree.getSelection();
        if (selection.length == 0) return;
        final TableTreeItem item = selection[0];
        final Text text = new Text(listEpsTree.getTable(), SWT.NONE);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        editor.setEditor(text, item, 1);
        text.setText(item.getText(1));
        text.setFocus();
        text.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                int escPressed = 0;
                switch(e.character) {
                    case SWT.CR:
                        escPressed = 1;
                        break;
                }
                if (escPressed == 1) {
                    lines[1] = item.getText(0);
                    lines[2] = text.getText();
                    epiMaker = new EpiMaker();
                    epiMaker.commentWriter(pluginInterface, lines);
                    Control oldEditor = editor.getEditor();
                    if (oldEditor != null) {
                        oldEditor.dispose();
                    }
                    if (!item.isDisposed()) item.setText(1, lines[2]);
                }
            }

            public void keyReleased(KeyEvent e) {
            }
        });
        text.addFocusListener(new FocusAdapter() {

            public void focusLost(FocusEvent e) {
                lines[1] = item.getText(0);
                lines[2] = text.getText();
                epiMaker = new EpiMaker();
                epiMaker.commentWriter(pluginInterface, lines);
                Control oldEditor = editor.getEditor();
                if (oldEditor != null) {
                    oldEditor.dispose();
                }
                if (!item.isDisposed()) item.setText(1, lines[2]);
            }
        });
    }

    public void deleteFromList(String nameToDie, String episode) {
        epiMaker = new EpiMaker();
        epiMaker.commentDelete(pluginInterface, nameToDie);
        readEpifile();
    }

    public void sorter() {
        String inputLine1, inputLine2;
        String epiNames[] = new String[1000];
        String epiEpisodes[] = new String[1000];
        int lineCounter = 0;
        try {
            String pluginDir = pluginInterface.getPluginDirectoryName();
            String eplist_file = pluginDir + System.getProperty("file.separator") + "EpisodeList.txt";
            File episodeList = new File(eplist_file);
            if (!episodeList.isFile()) {
                episodeList.createNewFile();
            }
            final BufferedReader in = new BufferedReader(new FileReader(episodeList));
            while ((inputLine1 = in.readLine()) != null) {
                if ((inputLine2 = in.readLine()) != null) {
                    epiNames[lineCounter] = inputLine1;
                    epiEpisodes[lineCounter] = inputLine2;
                    lineCounter++;
                }
            }
            in.close();
            int epiLength = epiNames.length;
            for (int i = 0; i < (lineCounter); i++) {
                for (int j = 0; j < (lineCounter - 1); j++) {
                    if (epiNames[j].compareToIgnoreCase(epiNames[j + 1]) > 0) {
                        String temp = epiNames[j];
                        epiNames[j] = epiNames[j + 1];
                        epiNames[j + 1] = temp;
                        String temp2 = epiEpisodes[j];
                        epiEpisodes[j] = epiEpisodes[j + 1];
                        epiEpisodes[j + 1] = temp2;
                    }
                }
            }
            File episodeList2 = new File(eplist_file);
            BufferedWriter bufWriter = new BufferedWriter(new FileWriter(episodeList2));
            for (int i = 0; i <= lineCounter; i++) {
                if (epiNames[i] == null) {
                    break;
                }
                bufWriter.write(epiNames[i] + "\n");
                bufWriter.write(epiEpisodes[i] + "\n");
            }
            bufWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete() {
        super.delete();
        composite.dispose();
    }
}
