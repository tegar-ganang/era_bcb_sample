package ti.plato.ui.views.send.views;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import oscript.OscriptInterpreter;
import oscript.data.BasicScope;
import oscript.data.ONoSuchMemberException;
import oscript.data.ONullReferenceException;
import oscript.exceptions.PackagedScriptObjectException;
import oscript.parser.ParseException;
import ti.exceptions.ProgrammingErrorException;
import ti.mcore.Environment;
import ti.mcore.RoutingContext;
import ti.plato.components.logger.contentprovider.IContentProvider;
import ti.plato.components.logger.process.ContentManager;
import ti.plato.ui.images.util.ImagesUtil;
import ti.plato.ui.u.PluginUtil;
import ti.plato.ui.u.UICopyUtil;
import ti.plato.ui.views.properties.PropertiesAccess;
import ti.plato.ui.views.send.SendPlugin;
import ti.plato.ui.views.send.TypeController;
import ti.plato.ui.views.send.WorkspaceSaveContainer;
import ti.plato.ui.views.send.constants.Constants;

/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */
public class SendView extends ViewPart {

    private Composite sendViewComp;

    private TreeViewer treeViewer = null;

    private Action actionSend;

    private Action actionSwitch;

    private Action actionClear;

    private Action actionImport;

    private Action actionExport;

    private Action actionDelete;

    private String currentType = "";

    private String preType = "";

    private TreeObject preNode = null;

    private FormData formDataTree = new FormData();

    private Canvas buttonSeparator = null;

    private MouseEvent eTrack = null;

    private Table historyTable = null;

    private Widget textMemory = null;

    private Image rowIconImage = null;

    private static Color leafValBgColor = null;

    private static Color leafInfoBgColor = null;

    private static Color innerValBgColor = null;

    private static Color innerInfoBgColor = null;

    private TableItem selectedTableItem = null;

    private int selectedTableItemIndex = -1;

    private Hashtable<String, ti.event.Event> text2evt = new Hashtable<String, ti.event.Event>();

    RoutingContext rctx = ti.mcore.Routing.getRouting().getRoutingContext();

    private final String ZIP_EXT = ".os";

    private String lastFileDialogPath = null;

    private Shell parentShell = null;

    private String newline = TypeController.NEW_LINE;

    public static final String ID = "ti.plato.ui.views.send.views.SendView";

    public static SendView getDefault() {
        SendView def = null;
        int wwCount = PlatformUI.getWorkbench().getWorkbenchWindowCount();
        for (int ww = 0; ww < wwCount; ++ww) {
            IWorkbenchWindow wWindow = PlatformUI.getWorkbench().getWorkbenchWindows()[ww];
            int pCount = wWindow.getPages().length;
            for (int i = 0; i < pCount; ++i) {
                IWorkbenchPage page = wWindow.getPages()[i];
                IViewReference viewReferences[] = page.getViewReferences();
                int vCount = viewReferences.length;
                for (int j = 0; j < vCount; ++j) {
                    if (viewReferences[j].getId().equals(ID)) {
                        IViewPart vp = viewReferences[j].getView(false);
                        if (vp instanceof SendView) {
                            def = (SendView) vp;
                            break;
                        }
                    }
                }
                if (def != null) break;
            }
            if (def != null) break;
        }
        return def;
    }

    /**
	 * The constructor.
	 */
    public SendView() {
    }

    private static Display getDisplay() {
        return Display.getDefault();
    }

    /**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
    public void createPartControl(Composite parent) {
        sendViewComp = parent;
        sendViewComp.setLayout(new FormLayout());
        parentShell = parent.getShell();
        treeViewer = new TreeViewer(sendViewComp, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        treeViewer.setUseHashlookup(true);
        final Tree tree = treeViewer.getTree();
        formDataTree.top = new FormAttachment(0, 0);
        formDataTree.bottom = new FormAttachment(tree, 200, SWT.TOP);
        formDataTree.left = new FormAttachment(0, 0);
        formDataTree.right = new FormAttachment(100, 0);
        tree.setLayoutData(formDataTree);
        final TreeEditor editor = new TreeEditor(tree);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        editor.minimumWidth = 50;
        tree.addListener(SWT.MouseHover, new Listener() {

            public void handleEvent(Event evt) {
                Point pt = new Point(evt.x, evt.y);
                if (tree.getTopItem() == null) return;
                final TreeItem item = findItem(pt);
                String toolTipText = null;
                if (item == null) {
                    tree.setToolTipText(toolTipText);
                    return;
                }
                final GC gc = new GC(tree);
                for (int i = 0; i < tree.getColumnCount(); i++) {
                    Rectangle rect = item.getBounds(i);
                    if (i == 0) continue;
                    final TreeObject data = (TreeObject) item.getData();
                    if (rect.contains(pt)) {
                        String text = "";
                        switch(i) {
                            case 1:
                                if (data instanceof ComboNode) {
                                    text = ((ComboNode) data).getCurrentSelection();
                                } else {
                                    text = data.getValue();
                                }
                                break;
                            case 2:
                                text = data.getInfo();
                                break;
                        }
                        Point stringPt = gc.textExtent(text);
                        if (stringPt.x > rect.width) toolTipText = text; else toolTipText = "";
                    }
                }
                tree.setToolTipText(toolTipText);
            }
        });
        tree.addListener(SWT.MouseDown, new Listener() {

            public void handleEvent(Event event) {
                Point pt = new Point(event.x, event.y);
                if (tree.getTopItem() == null) return;
                final TreeItem item = findItem(pt);
                if (item == null) return;
                for (int i = 0; i < tree.getColumnCount(); i++) {
                    if (i == 0) continue;
                    final int column = i;
                    Rectangle rect = item.getBounds(i);
                    if (rect.contains(pt)) {
                        final Control editorControl;
                        final Object data = item.getData();
                        if (data instanceof ComboNode && ((ComboNode) data).getIndex() == i) {
                            ComboNode comboNode = (ComboNode) data;
                            final Combo combo = new Combo(tree, SWT.DROP_DOWN | SWT.READ_ONLY);
                            combo.setVisibleItemCount(20);
                            final String[] choices = comboNode.getChoices();
                            combo.setItems(choices);
                            combo.select(0);
                            if (choices == TypeController.getRootChoices()) {
                                combo.setVisibleItemCount(5);
                            }
                            combo.addSelectionListener(new SelectionListener() {

                                public void widgetSelected(SelectionEvent e) {
                                    ((ComboNode) data).setCurrentSelection(combo.getItem(combo.getSelectionIndex()));
                                    if (choices == TypeController.getRootChoices()) {
                                        currentType = combo.getItem(combo.getSelectionIndex());
                                        WorkspaceSaveContainer.typeSelected = currentType;
                                        TypeController.getTypeController(currentType).setView(getDefault());
                                    }
                                    TreeObject node = (TreeObject) data;
                                    if (node.getParent() != null && node.getParent().getClass() == TreeParentCombo.class) {
                                        treeCommit(item, 1, combo.getItem(combo.getSelectionIndex()), false);
                                        combo.dispose();
                                        return;
                                    } else {
                                        combo.dispose();
                                        updateTree((TreeObject) data);
                                    }
                                }

                                public void widgetDefaultSelected(SelectionEvent e) {
                                    widgetSelected(e);
                                }
                            });
                            editorControl = combo;
                        } else if ((data instanceof TreeObject) && ((TreeObject) data).isEditable(i)) {
                            editorControl = new Text(tree, SWT.NONE);
                        } else {
                            editorControl = new Text(tree, SWT.READ_ONLY);
                        }
                        Listener textListener = new Listener() {

                            public void handleEvent(final Event e) {
                                String text = (editorControl instanceof Combo) ? ((Combo) editorControl).getText() : ((Text) editorControl).getText();
                                switch(e.type) {
                                    case SWT.FocusOut:
                                        treeCommit(item, column, text, false);
                                        editorControl.dispose();
                                        break;
                                    case SWT.Traverse:
                                        switch(e.detail) {
                                            case SWT.TRAVERSE_RETURN:
                                                treeCommit(item, column, text, true);
                                            case SWT.TRAVERSE_ESCAPE:
                                                editorControl.dispose();
                                                e.doit = false;
                                        }
                                        break;
                                }
                            }
                        };
                        editorControl.addListener(SWT.FocusOut, textListener);
                        editorControl.addListener(SWT.Traverse, textListener);
                        editor.setEditor(editorControl, item, i);
                        if (editorControl instanceof Combo) {
                            ((Combo) editorControl).setText(item.getText(i));
                        } else {
                            String fieldText = "";
                            TreeObject to = (TreeObject) data;
                            switch(i) {
                                case 1:
                                    fieldText = to.getValue();
                                    break;
                                case 2:
                                    fieldText = to.getInfo();
                                    break;
                                default:
                                    break;
                            }
                            ((Text) editorControl).setText(fieldText);
                            ((Text) editorControl).selectAll();
                        }
                        editorControl.setFocus();
                        textMemory = editorControl;
                        return;
                    }
                }
            }
        });
        int columnCount = Constants.COLUMN_NAMES.length;
        for (int i = 0; i < columnCount; i++) {
            Image image = null;
            TreeColumn column = new TreeColumn(tree, SWT.LEFT);
            column.setText(Constants.COLUMN_NAMES[i]);
            switch(i) {
                case 0:
                    image = ImagesUtil.getImageDescriptor("e_" + "element").createImage();
                    column.setImage(image);
                    break;
                case 1:
                    image = ImagesUtil.getImageDescriptor("e_" + "data").createImage();
                    column.setImage(image);
                    break;
                case 2:
                    image = ImagesUtil.getImageDescriptor("e_" + "info").createImage();
                    column.setImage(image);
                    break;
            }
            column.addControlListener(new ControlListener() {

                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    int columnCount = treeViewer.getTree().getColumnCount();
                    int[] result = new int[columnCount];
                    for (int idx = 0; idx < columnCount; idx++) {
                        TreeColumn myColumn = treeViewer.getTree().getColumns()[idx];
                        result[idx] = myColumn.getWidth();
                    }
                    WorkspaceSaveContainer.setColumnWidthsTop(result);
                }
            });
        }
        int[] columnWidths = WorkspaceSaveContainer.getColumnWidthsTop();
        if (columnWidths != null) {
            if (columnWidths.length < columnCount) columnCount = columnWidths.length;
            for (int idx = 0; idx < columnCount; idx++) {
                TreeColumn myColumn2 = treeViewer.getTree().getColumns()[idx];
                myColumn2.setWidth(columnWidths[idx]);
            }
        } else {
            for (int idx = 0; idx < columnCount; idx++) {
                TreeColumn myColumn2 = treeViewer.getTree().getColumns()[idx];
                myColumn2.setWidth(200);
            }
        }
        treeViewer.setColumnProperties(Constants.COLUMN_NAMES);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        int columnIndex = 0;
        TextCellEditor textEditor;
        CellEditor[] editors = new CellEditor[Constants.COLUMN_NAMES.length];
        editors[columnIndex++] = null;
        textEditor = new TextCellEditor(treeViewer.getTree());
        ((Text) textEditor.getControl()).addVerifyListener(new VerifyListener() {

            public void verifyText(VerifyEvent e) {
            }
        });
        if (columnIndex < Constants.COLUMN_NAMES.length) editors[columnIndex++] = textEditor;
        treeViewer.setCellEditors(editors);
        addSeparator();
        addHistoryTable();
        makeActions();
        hookContextMenu();
        contributeToActionBars();
        treeViewer.setContentProvider(new SendViewContentProvider());
        treeViewer.setLabelProvider(new SendViewLabelProvider());
        treeViewer.setCellModifier(new SendViewCellModifier(treeViewer));
        int extensionTypesCount = TypeController.getTypes().length;
        if (extensionTypesCount > 0) {
            boolean found = false;
            if (WorkspaceSaveContainer.typeSelected.length() > 0) {
                for (int x = 0; x < extensionTypesCount; x++) {
                    String typeName = TypeController.getTypeControllerName(x);
                    if (typeName.equals(WorkspaceSaveContainer.typeSelected)) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) currentType = WorkspaceSaveContainer.typeSelected; else currentType = TypeController.getTypeControllerName(0);
            TypeController typeController = TypeController.getTypeController(currentType);
            TypeController.getTypeController(currentType).setView(this);
            treeViewer.getTree().setRedraw(false);
            typeController.updateTree();
            treeViewer.setInput(typeController);
            treeViewer.getTree().setRedraw(true);
        } else {
            throw new ProgrammingErrorException("No plugins using types extension point");
        }
        treeViewer.expandAll();
        displayHistory(WorkspaceSaveContainer.displayHistory);
        update();
    }

    public void addHistoryTable() {
        historyTable = new Table(sendViewComp, SWT.SINGLE | SWT.FULL_SELECTION);
        historyTable.setHeaderVisible(true);
        historyTable.setLinesVisible(true);
        int columnCount = Constants.rowHistoryTableArray.length;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            final TableColumn column = new TableColumn(historyTable, SWT.NONE);
            column.setText(Constants.rowHistoryTableArray[columnIndex]);
            column.setImage(ImagesUtil.getImageDescriptor("e_" + Constants.rowHistoryTableIconArray[columnIndex]).createImage());
            column.setWidth(Constants.columnHistoryBottomInitialSize);
            column.addSelectionListener(new SelectionListener() {

                public void widgetSelected(SelectionEvent e) {
                    sortHistoryTable(column.getText());
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });
        }
        int[] columnWidths = WorkspaceSaveContainer.getColumnWidthsBottom();
        if (columnWidths != null) {
            if (columnWidths.length < columnCount) columnCount = columnWidths.length;
            for (int idx = 0; idx < columnCount; idx++) {
                TableColumn myColumn2 = historyTable.getColumns()[idx];
                myColumn2.setWidth(columnWidths[idx]);
            }
        }
        for (int idx = 0; idx < columnCount; idx++) {
            TableColumn myColumn = historyTable.getColumns()[idx];
            myColumn.addControlListener(new ControlListener() {

                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    int columnCount = historyTable.getColumnCount();
                    int[] result = new int[columnCount];
                    for (int idx = 0; idx < columnCount; idx++) {
                        TableColumn myColumn = historyTable.getColumns()[idx];
                        result[idx] = myColumn.getWidth();
                    }
                    WorkspaceSaveContainer.setColumnWidthsBottom(result);
                }
            });
        }
        FormData formDataTree = new FormData();
        formDataTree.top = new FormAttachment(buttonSeparator, 5, SWT.TOP);
        formDataTree.bottom = new FormAttachment(100, 0);
        formDataTree.left = new FormAttachment(0, 0);
        formDataTree.right = new FormAttachment(100, 0);
        historyTable.setLayoutData(formDataTree);
        historyTable.addMouseListener(mouseListener);
        historyTable.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                if (e.keyCode == 262144) {
                    if (historyTable.getSelectionIndex() == -1) return;
                    copy();
                }
                if (e.keyCode == SWT.DEL) {
                    delete();
                }
            }

            public void keyReleased(KeyEvent e) {
            }
        });
    }

    private void sortHistoryTable(String columnText) {
        int columnIndex = getColumnIndex(columnText);
        Vector<String> v = new Vector<String>();
        Hashtable<String, List> table = new Hashtable<String, List>();
        Hashtable<String, List> imageTable = new Hashtable<String, List>();
        TableItem[] items = historyTable.getItems();
        for (int i = 0; i < items.length; i++) {
            String text = items[i].getText(columnIndex);
            v.add(text);
            List<String[]> list = table.get(text);
            if (list == null) {
                list = new LinkedList<String[]>();
                table.put(text, list);
            }
            List<Image> imageList = imageTable.get(text);
            if (imageList == null) {
                imageList = new LinkedList<Image>();
                imageTable.put(text, imageList);
            }
            String[] columnTexts = new String[Constants.rowHistoryTableArray.length];
            for (int j = 0; j < Constants.rowHistoryTableArray.length; j++) {
                columnTexts[j] = items[i].getText(j);
            }
            list.add(columnTexts);
            imageList.add(items[i].getImage(0));
        }
        Collections.sort(v, new SendViewComparator());
        String[] newOrder = v.toArray(new String[v.size()]);
        historyTable.removeAll();
        historyTable.setRedraw(false);
        Hashtable<String, String> tempTable = new Hashtable<String, String>();
        for (int i = 0; i < newOrder.length; i++) {
            String txt = newOrder[i];
            if (tempTable.get(txt) == null) {
                tempTable.put(txt, txt);
                List list = table.get(txt);
                List imageList = imageTable.get(txt);
                for (int j = 0; j < list.size(); j++) {
                    String[] columnTexts = (String[]) list.get(j);
                    TableItem item = new TableItem(historyTable, SWT.READ_ONLY | SWT.LEFT);
                    item.setText(columnTexts);
                    item.setImage(0, (Image) imageList.get(j));
                }
            }
        }
        historyTable.setRedraw(true);
    }

    private int getColumnIndex(String columnText) {
        for (int i = 0; i < Constants.rowHistoryTableArray.length; i++) {
            if (Constants.rowHistoryTableArray[i].equals(columnText)) return i;
        }
        return 0;
    }

    private void delete() {
        if (selectedTableItem != null) {
            String text = getColumnText(selectedTableItem);
            ti.event.Event evt = text2evt.get(text);
            if (evt != null) {
                TypeController temp = TypeController.getTypeController(selectedTableItem.getText(0));
                String script = temp.getScript(evt);
                if (selectedTableItemIndex >= 0 && selectedTableItemIndex < historyTable.getItemCount()) {
                    historyTable.remove(selectedTableItemIndex);
                    removeFromHistory(script);
                    text2evt.remove(text);
                    selectedTableItem = null;
                }
                if (historyTable.getItemCount() > 0 && historyTable.getItemCount() == selectedTableItemIndex) {
                    TableItem item = historyTable.getItem(selectedTableItemIndex - 1);
                    historyTable.setSelection(new TableItem[] { item });
                    String itemText = getColumnText(item);
                    ti.event.Event tempEvt = text2evt.get(itemText);
                    updateTreeFromHistory(tempEvt, item.getText(0));
                    selectedTableItem = item;
                    selectedTableItemIndex--;
                }
                if (historyTable.getItemCount() > selectedTableItemIndex) {
                    TableItem item = historyTable.getItem(selectedTableItemIndex);
                    historyTable.setSelection(new TableItem[] { item });
                    String itemText = getColumnText(item);
                    ti.event.Event tempEvt = text2evt.get(itemText);
                    updateTreeFromHistory(tempEvt, item.getText(0));
                    selectedTableItem = item;
                }
                update();
            }
        }
    }

    private String getColumnText(TableItem item) {
        String text = "";
        for (int i = 0; i < historyTable.getColumnCount(); i++) {
            text += item.getText(i);
        }
        return text;
    }

    private void removeFromHistory(String script) {
        WorkspaceSaveContainer.historyList.remove(script);
        if (WorkspaceSaveContainer.historyList.size() == 0) displayHistory(false);
    }

    private void copy() {
        TypeController typeController = TypeController.getTypeController(currentType);
        String script = typeController.getScript();
        UICopyUtil.setClipboardText(sendViewComp.getDisplay(), script);
    }

    public void addSeparator() {
        buttonSeparator = new Canvas(sendViewComp, 0);
        buttonSeparator.setCursor(new Cursor(sendViewComp.getDisplay(), SWT.CURSOR_SIZENS));
        FormData formDataSeparator = new FormData();
        formDataSeparator.top = new FormAttachment(treeViewer.getTree(), 0, SWT.BOTTOM);
        formDataSeparator.bottom = new FormAttachment(treeViewer.getTree(), Constants.separatorHeight, SWT.BOTTOM);
        formDataSeparator.right = new FormAttachment(100, 0);
        formDataSeparator.left = new FormAttachment(0, 0);
        buttonSeparator.setLayoutData(formDataSeparator);
        buttonSeparator.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
                buttonSeparator.setCapture(true);
                eTrack = e;
            }

            public void mouseUp(MouseEvent e) {
                buttonSeparator.setCapture(false);
                eTrack = null;
            }
        });
        buttonSeparator.addMouseMoveListener(new MouseMoveListener() {

            public void mouseMove(MouseEvent e) {
                if (eTrack != null) {
                    int hDiff = e.y - eTrack.y;
                    if (treeViewer.getTree().getSize().y + hDiff < Constants.treeMinHeight) return;
                    if (historyTable.getSize().y - hDiff < Constants.treeMinHeight) return;
                    formDataTree.bottom.offset += hDiff;
                    sendViewComp.layout();
                    WorkspaceSaveContainer.topPaneHeight = treeViewer.getTree().getSize().y;
                }
            }
        });
        treeViewer.getTree().addListener(SWT.Resize, resizeListener);
        buttonSeparator.addPaintListener(new PaintListener() {

            public void paintControl(PaintEvent e) {
                Rectangle clientArea = sendViewComp.getClientArea();
                e.gc.setForeground(sendViewComp.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                e.gc.drawLine(0, Constants.separatorHeight - 1, clientArea.width, Constants.separatorHeight - 1);
            }
        });
        buttonSeparator.addPaintListener(new PaintListener() {

            public void paintControl(PaintEvent e) {
                Rectangle clientArea = sendViewComp.getClientArea();
                e.gc.setForeground(sendViewComp.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                e.gc.drawLine(0, Constants.separatorHeight - 1, clientArea.width, Constants.separatorHeight - 1);
            }
        });
    }

    private Listener resizeListener = new Listener() {

        public void handleEvent(Event e) {
            if (!WorkspaceSaveContainer.displayHistory) return;
            if (e.type == SWT.Resize) {
                while (historyTable.getSize().y < Constants.tablesMinHeight) {
                    int hdiff = Constants.tablesMinHeight - historyTable.getSize().y;
                    if (treeViewer.getTree().getSize().y - hdiff < Constants.tablesMinHeight) return;
                    formDataTree.bottom.offset -= hdiff;
                    sendViewComp.layout();
                    WorkspaceSaveContainer.topPaneHeight = treeViewer.getTree().getSize().y;
                }
            }
        }
    };

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                SendView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
        treeViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, treeViewer);
        Menu menu2 = menuMgr.createContextMenu(historyTable);
        historyTable.setMenu(menu2);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(actionSend);
        manager.add(new Separator());
        manager.add(actionSwitch);
        manager.add(new Separator());
        manager.add(actionExport);
        manager.add(actionImport);
        manager.add(new Separator());
        manager.add(actionClear);
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(actionSend);
        manager.add(new Separator());
        manager.add(actionSwitch);
        manager.add(new Separator());
        manager.add(actionExport);
        manager.add(actionImport);
        manager.add(new Separator());
        manager.add(actionDelete);
        manager.add(actionClear);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(new Separator());
        manager.add(PropertiesAccess.getActionProperties());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(actionSend);
        manager.add(new Separator());
        manager.add(actionSwitch);
        manager.add(new Separator());
        manager.add(actionExport);
        manager.add(actionImport);
        manager.add(new Separator());
        manager.add(actionClear);
    }

    private void makeActions() {
        actionSend = new Action() {

            public void run() {
                send();
            }
        };
        actionSend.setText(Constants.actionSendName);
        actionSend.setToolTipText(Constants.actionSendName);
        actionSend.setImageDescriptor(ImagesUtil.getImageDescriptor("e_send"));
        actionSend.setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_send"));
        actionSwitch = new Action() {

            public void run() {
                switchHistory();
            }
        };
        actionSwitch.setText(Constants.actionSwitchName);
        actionSwitch.setToolTipText(Constants.actionSwitchName);
        actionSwitch.setImageDescriptor(ImagesUtil.getImageDescriptor("e_switch"));
        actionSwitch.setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_switch"));
        actionClear = new Action() {

            public void run() {
                clearHistory();
            }
        };
        actionClear.setText(Constants.actionClearName);
        actionClear.setToolTipText(Constants.actionClearName);
        actionClear.setImageDescriptor(ImagesUtil.getImageDescriptor("e_clear"));
        actionImport = new Action() {

            public void run() {
                importEvents();
            }
        };
        actionImport.setText(Constants.actionImportName);
        actionImport.setToolTipText(Constants.actionImportToolTip);
        actionImport.setImageDescriptor(SendPlugin.getImageDescriptor("icons/e_import.gif"));
        actionExport = new Action() {

            public void run() {
                exportEvents();
            }
        };
        actionExport.setText(Constants.actionExportName);
        actionExport.setToolTipText(Constants.actionExportToolTip);
        actionExport.setImageDescriptor(SendPlugin.getImageDescriptor("icons/e_export.gif"));
        actionExport.setDisabledImageDescriptor(SendPlugin.getImageDescriptor("icons/d_export.gif"));
        actionDelete = new Action() {

            public void run() {
                delete();
            }
        };
        actionDelete.setText(Constants.actionDeleteName);
        actionDelete.setToolTipText(Constants.actionDeleteName);
        actionDelete.setImageDescriptor(SendPlugin.getImageDescriptor("icons/e_delete.gif"));
        actionDelete.setDisabledImageDescriptor(SendPlugin.getImageDescriptor("icons/d_delete.gif"));
        update();
    }

    private void importEvents() {
        String fileName = importFromFile();
        StringBuffer buffer = new StringBuffer();
        if (fileName.length() > 0) {
            fileName = "\"/" + fileName.replace('\\', '/') + "\"";
            buffer.append("pkg.system.declareJavaPackage(\"ti.plato.ui.views.send\");");
            buffer.append("pkg.system.declareJavaPackage(\"ti.plato.ui.views.send.views\");");
            buffer.append("{");
            buffer.append("  public const var rctx = new '{");
            buffer.append("    public function sendEvent(evt)");
            buffer.append("      {");
            buffer.append("         ti.plato.ui.views.send.views.SendView.getDefault().add2history(evt);");
            buffer.append("       }");
            buffer.append("  }();");
            buffer.append(" import " + fileName + ";");
            buffer.append("}" + newline);
            final String str = buffer.toString();
            Environment.getEnvironment().run(new Runnable() {

                public void run() {
                    BasicScope scope = new BasicScope(OscriptInterpreter.getGlobalScope());
                    BufferedReader br = new BufferedReader(new StringReader(str));
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            try {
                                OscriptInterpreter.eval(line, scope);
                            } catch (ParseException e) {
                                Environment.getEnvironment().error("Error Parsing: " + str);
                                Environment.getEnvironment().unhandledException(e);
                            } catch (PackagedScriptObjectException e) {
                                if (e.val.getClass() == ONullReferenceException.class) {
                                    Environment.getEnvironment().showInfoMessage("Not connected to the target!");
                                }
                                if (e.val.getClass() == ONoSuchMemberException.class) {
                                    Environment.getEnvironment().showInfoMessage("Type Descriptor Table database is not loaded!");
                                }
                            }
                        }
                    } catch (IOException e) {
                        Environment.getEnvironment().unhandledException(e);
                    }
                }
            }, "Import events from " + fileName);
        }
    }

    private void exportEvents() {
        String fileName = export2file();
        if (fileName.length() > 0) {
            StringBuffer buffer = new StringBuffer();
            TableItem[] items = historyTable.getItems();
            for (int i = 0; i < items.length; i++) {
                TableItem item = items[i];
                String text = getColumnText(item);
                ti.event.Event evt = text2evt.get(text);
                TypeController type = TypeController.getTypeController(item.getText(0));
                String script = type.getScript(evt);
                buffer.append(script);
                buffer.append("rctx.sendEvent(evt);" + newline + newline);
                if (i + 1 < items.length) buffer.append("//" + (i + 1) + "******************************************************************************" + newline);
            }
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
                out.print(buffer.toString());
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                Environment.getEnvironment().unhandledException(e);
            }
        }
    }

    private String importFromFile() {
        FileDialog dialog = new FileDialog(PluginUtil.getShell(), SWT.OPEN | SWT.SYSTEM_MODAL);
        dialog.setText("Open");
        dialog.setFilterPath(".");
        dialog.setFilterExtensions(new String[] { "*" + ZIP_EXT });
        dialog.setFilterNames(new String[] { "ObjectScript Files (*" + ZIP_EXT + ")" });
        if (lastFileDialogPath == null) dialog.setFilterPath(System.getProperty("user.home") + "\\Desktop"); else dialog.setFilterPath(lastFileDialogPath);
        String name = dialog.open();
        if (name == null || name.compareTo("") == 0) {
            return "";
        }
        lastFileDialogPath = dialog.getFilterPath();
        return name;
    }

    private String export2file() {
        FileDialog dialog = new FileDialog(PluginUtil.getShell(), SWT.SAVE | SWT.SYSTEM_MODAL);
        dialog.setText("Save");
        dialog.setFilterExtensions(new String[] { "*" + ZIP_EXT });
        dialog.setFilterNames(new String[] { "ObjectScript Files (*" + ZIP_EXT + ")" });
        if (lastFileDialogPath == null) dialog.setFilterPath(System.getProperty("user.home") + "\\Desktop"); else dialog.setFilterPath(lastFileDialogPath);
        dialog.setFileName("Untitled");
        final String name = dialog.open();
        if (name == null || name.compareTo("") == 0) {
            return "";
        }
        File file = new File(name);
        if (file.exists()) {
            MessageBox mbox = new MessageBox(parentShell, SWT.OK | SWT.CANCEL | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
            mbox.setText("Warning");
            mbox.setMessage("This file already exists. Do you want to overwrite it?");
            if (mbox.open() == SWT.CANCEL) return "";
        }
        lastFileDialogPath = dialog.getFilterPath();
        return name;
    }

    private void update() {
        actionSwitch.setEnabled(WorkspaceSaveContainer.historyList.size() != 0);
        actionSwitch.setChecked(WorkspaceSaveContainer.displayHistory);
        actionExport.setEnabled(historyTable.getItemCount() > 0);
        actionDelete.setEnabled(historyTable.getSelection().length > 0);
    }

    private void clearHistory() {
        WorkspaceSaveContainer.historyList.clear();
        text2evt.clear();
        historyTable.removeAll();
        if (rowIconImage != null && (!rowIconImage.isDisposed())) rowIconImage.dispose();
        displayHistory(false);
    }

    private void send() {
        if (textMemory != null && !textMemory.isDisposed()) {
            textMemory.notifyListeners(SWT.FocusOut, null);
        }
        TypeController type = TypeController.getTypeController(currentType);
        if (!type.ready2send()) {
            Environment.getEnvironment().showInfoMessage(type.getFailed2SendMessage());
            return;
        }
        String script = type.getScript();
        if (script.length() == 0) return;
        debug("script : " + script);
        ti.event.Event event = null;
        try {
            event = type.getEvent(script);
        } catch (IOException e) {
            ti.mcore.Environment.getEnvironment().unhandledException(e);
        } catch (ParseException e) {
            Environment.getEnvironment().unhandledException(e);
        }
        if (event != null) {
            rctx.sendEvent(event);
            if (!WorkspaceSaveContainer.historyList.contains(script)) WorkspaceSaveContainer.historyList.add(script);
            update();
            add2HistoryTable(event);
            if (WorkspaceSaveContainer.historyList.size() == 1) displayHistory(true);
        }
    }

    private void add2HistoryTable(ti.event.Event evt) {
        Image[] icon = new Image[1];
        String[] texts = getTextForHistoryTable(evt, icon);
        if (texts == null) return;
        String appendString = appendColumnText(texts);
        if (text2evt.get(appendString) != null) return;
        TableItem item = new TableItem(historyTable, SWT.READ_ONLY | SWT.LEFT);
        item.setText(texts);
        rowIconImage = icon[0];
        item.setImage(0, rowIconImage);
        text2evt.put(appendString, evt);
    }

    public void add2history(final ti.event.Event evt) {
        if (evt == null) return;
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                Image[] icon = new Image[1];
                String[] texts = getTextForHistoryTable(evt, icon);
                if (texts == null) return;
                String appendString = appendColumnText(texts);
                if (text2evt.get(appendString) != null) return;
                TableItem item = new TableItem(historyTable, SWT.READ_ONLY | SWT.LEFT);
                item.setText(texts);
                rowIconImage = icon[0];
                item.setImage(0, rowIconImage);
                text2evt.put(appendString, evt);
                TypeController temp = TypeController.getTypeController(texts[0]);
                String script = temp.getScript(evt);
                WorkspaceSaveContainer.historyList.add(script);
                if (WorkspaceSaveContainer.historyList.size() == 1) {
                    update();
                    displayHistory(true);
                }
            }
        });
    }

    private String appendColumnText(String[] texts) {
        String text = "";
        for (int i = 0; i < texts.length; i++) {
            text += texts[i];
        }
        return text;
    }

    public void displayHistory(boolean status) {
        if (treeViewer.getTree().isDisposed()) return;
        WorkspaceSaveContainer.displayHistory = status;
        if (status) {
            formDataTree.top = new FormAttachment(0, 0);
            formDataTree.bottom = new FormAttachment(treeViewer.getTree(), WorkspaceSaveContainer.topPaneHeight, SWT.TOP);
            formDataTree.left = new FormAttachment(0, 0);
            formDataTree.right = new FormAttachment(100, 0);
            treeViewer.getTree().setLayoutData(formDataTree);
            historyTable.setVisible(true);
            buttonSeparator.setVisible(true);
            sendViewComp.layout();
            while (historyTable.getSize().y < Constants.tablesMinHeight) {
                int hdiff = Constants.tablesMinHeight - historyTable.getSize().y;
                if (treeViewer.getTree().getSize().y - hdiff < Constants.tablesMinHeight) {
                    if (WorkspaceSaveContainer.historyList.size() > 0) {
                        populateHistoryTable();
                    }
                    return;
                }
                formDataTree.bottom.offset -= hdiff;
                sendViewComp.layout();
                WorkspaceSaveContainer.topPaneHeight = treeViewer.getTree().getSize().y;
            }
            if (WorkspaceSaveContainer.historyList.size() > 0) {
                populateHistoryTable();
            }
        } else {
            historyTable.setVisible(false);
            buttonSeparator.setVisible(false);
            formDataTree.top = new FormAttachment(0, 0);
            formDataTree.bottom = new FormAttachment(100, 0);
            formDataTree.left = new FormAttachment(0, 0);
            formDataTree.right = new FormAttachment(100, 0);
            treeViewer.getTree().setLayoutData(formDataTree);
        }
        sendViewComp.layout();
        update();
    }

    private void populateHistoryTable() {
        Vector<String> badScripts = new Vector<String>();
        for (int i = 0; i < WorkspaceSaveContainer.historyList.size(); i++) {
            String script = WorkspaceSaveContainer.historyList.get(i);
            try {
                ti.event.Event evt = null;
                TypeController[] types = TypeController.getTypes();
                for (int j = 0; j < types.length; j++) {
                    TypeController typeController = types[j];
                    evt = typeController.getEvent(script);
                    if (evt != null) {
                        add2HistoryTable(evt);
                        break;
                    }
                }
            } catch (IOException e) {
                Environment.getEnvironment().unhandledException(e);
            } catch (ParseException e) {
                badScripts.add(script);
            }
        }
        for (int i = 0; i < badScripts.size(); i++) {
            String badScript = badScripts.get(i);
            WorkspaceSaveContainer.historyList.remove(badScript);
        }
    }

    private void switchHistory() {
        displayHistory(!WorkspaceSaveContainer.displayHistory);
    }

    /**
	 * Passing the focus request to the viewer's control.
	 */
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    private TreeItem findItem(Point pt) {
        TreeItem ret = null;
        Tree tree = treeViewer.getTree();
        TreeItem[] items = tree.getItems();
        for (int x = 0; x < items.length; x++) {
            TreeItem item = items[x];
            ret = findItem(pt, item);
            if (ret != null) break;
        }
        return ret;
    }

    private TreeItem findItem(Point pt, TreeItem item) {
        TreeItem ret = null;
        Tree tree = treeViewer.getTree();
        for (int i = 0; i < tree.getColumnCount(); i++) {
            if (i == 0) continue;
            Rectangle rect = item.getBounds(i);
            if (rect.contains(pt)) {
                ret = item;
                break;
            }
        }
        if (ret == null && item.getItems().length > 0) {
            TreeItem[] subItems = item.getItems();
            for (int j = 0; j < subItems.length; j++) {
                ret = findItem(pt, subItems[j]);
                if (ret != null) {
                    break;
                }
            }
        }
        return ret;
    }

    private void treeCommit(TreeItem item, int column, String text, boolean displayError) {
        if (!item.isDisposed()) {
            item.setText(column, text);
            TreeObject obj = (TreeObject) item.getData();
            if (column == 1 && obj.isEditable(column)) {
                obj.setValue(text);
            }
        }
    }

    private TableItem findTableItem(Point pt) {
        TableItem ret = null;
        TableItem[] items = historyTable.getItems();
        for (int j = 0; j < items.length; j++) {
            TableItem item = items[j];
            for (int i = 0; i < historyTable.getColumnCount(); i++) {
                Rectangle rect = item.getBounds(i);
                if (rect.contains(pt)) {
                    ret = item;
                    selectedTableItemIndex = j;
                    break;
                }
            }
        }
        return ret;
    }

    private MouseAdapter mouseListener = new MouseAdapter() {

        public void mouseDown(MouseEvent event) {
            Point pt = new Point(event.x, event.y);
            TableItem item = findTableItem(pt);
            selectedTableItem = item;
            if (item != null) {
                String text = "";
                for (int i = 0; i < historyTable.getColumnCount(); i++) {
                    text += item.getText(i);
                }
                ti.event.Event evt = text2evt.get(text);
                if (evt != null) {
                    try {
                        TypeController current = TypeController.getTypeController(currentType);
                        ti.event.Event currentEvent = current.getEvent(current.getScript());
                        String currentText = "";
                        String[] textCheck = null;
                        if (currentEvent != null) {
                            textCheck = getTextForHistoryTable(currentEvent, null);
                            if (text != null) currentText = appendColumnText(textCheck);
                        }
                        textCheck = getTextForHistoryTable(evt, null);
                        String newText = "";
                        if (textCheck != null) newText = appendColumnText(textCheck);
                        if (!newText.equals(currentText)) updateTreeFromHistory(evt, item.getText(0));
                    } catch (IOException e) {
                        Environment.getEnvironment().unhandledException(e);
                    } catch (ParseException e) {
                        Environment.getEnvironment().unhandledException(e);
                    }
                }
                update();
            }
        }

        public void mouseDoubleClick(MouseEvent event) {
            Point pt = new Point(event.x, event.y);
            TableItem item = findTableItem(pt);
            selectedTableItem = item;
            if (item != null) {
                mouseDown(event);
                send();
            }
        }
    };

    private String[] getTextForHistoryTable(ti.event.Event evt, Image[] imageIcon) {
        IContentProvider icontentProvider = ContentManager.getContentProvider(evt);
        String[] loggerTexts = new String[Constants.LOGGER_COLUMN_COUNT];
        if (icontentProvider == null) {
            ti.mcore.Environment.getEnvironment().fatalError("No Content Provider has been registered for " + evt.getClass());
            return null;
        }
        icontentProvider.getDispInfo(evt, loggerTexts, imageIcon, IContentProvider.ColumnMasks.ALL_COLUMNS, true);
        String[] texts = new String[historyTable.getColumnCount()];
        texts[0] = loggerTexts[0];
        texts[1] = loggerTexts[3];
        texts[2] = loggerTexts[4];
        texts[3] = loggerTexts[5];
        texts[4] = loggerTexts[6];
        return texts;
    }

    private void updateTreeFromHistory(ti.event.Event evt, String text) {
        currentType = text;
        TypeController currType = TypeController.getTypeController(currentType);
        currType.initTree(evt);
        treeViewer.getTree().setRedraw(false);
        treeViewer.setInput(currType);
        treeViewer.expandAll();
        treeViewer.setSelection(new StructuredSelection((TreeObjectCombo) treeViewer.getTree().getItem(0).getData()));
        treeViewer.getTree().setRedraw(true);
    }

    public static Color getLeafValBgColor() {
        if (leafValBgColor == null) {
            int color = Constants.VALUE_LEAF_COLOR;
            int red = (color >> 16) & 0xff;
            int green = (color >> 8) & 0xff;
            int blue = color & 0xff;
            leafValBgColor = new Color(getDisplay(), red, green, blue);
        }
        return leafValBgColor;
    }

    public static Color getLeafInfoBgColor() {
        if (leafInfoBgColor == null) {
            int color = Constants.INFO_LEAF_COLOR;
            int red = (color >> 16) & 0xff;
            int green = (color >> 8) & 0xff;
            int blue = color & 0xff;
            leafInfoBgColor = new Color(getDisplay(), red, green, blue);
        }
        return leafInfoBgColor;
    }

    public static Color getInnerValBgColor() {
        if (innerValBgColor == null) {
            int color = Constants.VALUE_PARENT_COLOR;
            int red = (color >> 16) & 0xff;
            int green = (color >> 8) & 0xff;
            int blue = color & 0xff;
            innerValBgColor = new Color(getDisplay(), red, green, blue);
        }
        return innerValBgColor;
    }

    public static Color getInnerInfoBgColor() {
        if (innerInfoBgColor == null) {
            int color = Constants.INFO_PARENT_COLOR;
            int red = (color >> 16) & 0xff;
            int green = (color >> 8) & 0xff;
            int blue = color & 0xff;
            innerInfoBgColor = new Color(getDisplay(), red, green, blue);
        }
        return innerInfoBgColor;
    }

    public void updateTree(TreeObject node) {
        if (preType.equals(currentType) && (node == preNode)) return;
        debug("update Tree");
        preType = currentType;
        TypeController type = TypeController.getTypeController(currentType);
        treeViewer.getTree().setRedraw(false);
        type.updateTree();
        treeViewer.setInput(type);
        treeViewer.expandToLevel(2);
        treeViewer.setSelection(new StructuredSelection(node));
        treeViewer.getTree().setRedraw(true);
    }

    private void debug(String str) {
    }

    public synchronized void dispose() {
        if (innerInfoBgColor != null) innerInfoBgColor.dispose();
        if (innerValBgColor != null) innerValBgColor.dispose();
        if (leafInfoBgColor != null) leafInfoBgColor.dispose();
        if (leafValBgColor != null) leafValBgColor.dispose();
        leafValBgColor = null;
        leafInfoBgColor = null;
        innerInfoBgColor = null;
        innerValBgColor = null;
        rctx.dispose();
    }

    public TypeController getCurrentType() {
        return TypeController.getTypeController(currentType);
    }
}
