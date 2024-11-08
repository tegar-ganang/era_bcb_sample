package jreader.gui;

import java.io.File;
import jreader.Item;
import jreader.JReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Tworzy tabelę z listą itemów danego kanału, taga lub filtru.
 * 
 * @author Karol
 *
 */
public class ItemsTable {

    /**
	 * Tabela itemów.
	 */
    public static Table itemsTable;

    /**
	 * Nazwy kolumn tabeli.
	 */
    String[] titles = { "Title", "Date" };

    private static Image unread;

    static Image read;

    /**
	 * Umieszcza tabelę w kompozycie podanym jako parametr.
	 * 
	 * @param comp Kompozyt służący jako <i>parent</i>, w którym ma być umieszczona tabela.  
	 */
    public ItemsTable(final Composite comp) {
        unread = new Image(comp.getDisplay(), "data" + File.separator + "icons" + File.separator + "unread.png");
        read = new Image(comp.getDisplay(), "data" + File.separator + "icons" + File.separator + "read.png");
        itemsTable = new Table(comp, SWT.SINGLE | SWT.FULL_SELECTION);
        itemsTable.setLinesVisible(true);
        itemsTable.setHeaderVisible(true);
        final TableColumn column1 = new TableColumn(itemsTable, SWT.NONE);
        column1.setText(titles[0]);
        final TableColumn column2 = new TableColumn(itemsTable, SWT.NONE);
        column2.setText(titles[1]);
        Menu popupMenu = new Menu(itemsTable);
        MenuItem openNewTab = new MenuItem(popupMenu, SWT.NONE);
        openNewTab.setText("Open item in a new tab");
        MenuItem deleteItem = new MenuItem(popupMenu, SWT.NONE);
        deleteItem.setText("Delete item");
        itemsTable.setMenu(popupMenu);
        refresh();
        comp.addControlListener(new ControlAdapter() {

            public void controlResized(ControlEvent e) {
                Rectangle area = comp.getClientArea();
                Point preferredSize = itemsTable.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                int width = area.width - 2 * itemsTable.getBorderWidth();
                if (preferredSize.y > area.height + itemsTable.getHeaderHeight()) {
                    Point vBarSize = itemsTable.getVerticalBar().getSize();
                    width -= vBarSize.x;
                }
                Point oldSize = itemsTable.getSize();
                if (oldSize.x > area.width) {
                    column1.setWidth((width / 3) * 2);
                    column2.setWidth(width - column1.getWidth());
                    itemsTable.setSize(area.width, area.height);
                } else {
                    itemsTable.setSize(area.width, area.height);
                    column1.setWidth((width / 3) * 2);
                    column2.setWidth(width - column1.getWidth());
                }
            }
        });
        itemsTable.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                TableItem[] item = itemsTable.getSelection();
                Font initialFont = item[0].getFont();
                FontData[] fontData = initialFont.getFontData();
                if (fontData[0].getStyle() != SWT.NORMAL) {
                    for (int i = 0; i < fontData.length; i++) {
                        fontData[i].setStyle(SWT.NORMAL);
                    }
                    Font newFont = new Font(comp.getDisplay(), fontData);
                    item[0].setFont(newFont);
                    Item it = JReader.getItems().get(itemsTable.getSelectionIndex());
                    if (JReader.getChannel(it.getChannelId()).getIconPath() == null) item[0].setImage(read);
                }
                if (Preview.folderPreview.getItemCount() != 0) {
                    JReader.selectItem(JReader.getItems().get(itemsTable.getSelectionIndex()), Preview.folderPreview.getSelectionIndex());
                    Preview.previewItemList.get(Preview.folderPreview.getSelectionIndex()).refresh();
                } else {
                    JReader.addNewPreviewTab();
                    JReader.selectItem(JReader.getItems().get(itemsTable.getSelectionIndex()), 0);
                    GUI.openTab(item[0].getText()).refresh();
                }
                SubsList.refresh();
                Filters.refresh();
            }
        });
        itemsTable.addListener(SWT.EraseItem, new Listener() {

            public void handleEvent(Event event) {
                if ((event.detail & SWT.SELECTED) != 0) {
                    GC gc = event.gc;
                    Rectangle area = itemsTable.getClientArea();
                    int columnCount = itemsTable.getColumnCount();
                    if (event.index == columnCount - 1 || columnCount == 0) {
                        int width = area.x + area.width - event.x;
                        if (width > 0) {
                            Region region = new Region();
                            gc.getClipping(region);
                            region.add(event.x, event.y, width, event.height);
                            gc.setClipping(region);
                            region.dispose();
                        }
                    }
                    gc.setAdvanced(true);
                    if (gc.getAdvanced()) gc.setAlpha(127);
                    Rectangle rect = event.getBounds();
                    Color foreground = gc.getForeground();
                    Color background = gc.getBackground();
                    gc.setForeground(comp.getDisplay().getSystemColor(SWT.COLOR_BLUE));
                    gc.setBackground(comp.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
                    gc.fillGradientRectangle(0, rect.y, itemsTable.getClientArea().width, rect.height, false);
                    gc.setForeground(foreground);
                    gc.setBackground(background);
                    event.detail &= ~SWT.SELECTED;
                }
            }
        });
        MouseListener openListener = new MouseListener() {

            public void mouseDoubleClick(MouseEvent me) {
                openTab();
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
            }
        };
        itemsTable.addMouseListener(openListener);
        openNewTab.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                openTab();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        deleteItem.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                JReader.removeItem(JReader.getItems().get(itemsTable.getSelectionIndex()));
                Filters.refresh();
                SubsList.refresh();
                refresh();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        itemsTable.addFocusListener(Focus.setFocus((Items.folderItem)));
    }

    /**
	 * Odświeża tabelę itemsTable.
	 */
    public static void refresh() {
        GUI.display.asyncExec(new Runnable() {

            public void run() {
                ItemsTable.itemsTable.removeAll();
                Font initialFont = itemsTable.getFont();
                FontData[] fontData = initialFont.getFontData();
                for (int i = 0; i < fontData.length; i++) {
                    fontData[i].setStyle(SWT.BOLD);
                }
                Font fontBold = new Font(Items.tableComposite.getDisplay(), fontData);
                int index = 0;
                for (Item it : JReader.getItems()) {
                    TableItem item = new TableItem(ItemsTable.itemsTable, SWT.NONE);
                    if (JReader.getChannel(it.getChannelId()).getIconPath() == null) if (it.isRead()) item.setImage(read); else item.setImage(unread); else {
                        try {
                            ImageData imData = new ImageData(JReader.getChannel(it.getChannelId()).getIconPath());
                            imData = imData.scaledTo(16, 16);
                            item.setImage(new Image(Items.tableComposite.getDisplay(), imData));
                        } catch (SWTException swte) {
                            if (it.isRead()) item.setImage(read); else item.setImage(unread);
                        }
                    }
                    item.setText(0, (it.getTitle() != null ? it.getTitle() : "brak tytułu"));
                    item.setText(1, GUI.shortDateFormat.format(it.getDate()));
                    if (!it.isRead()) {
                        item.setFont(fontBold);
                    }
                    if (!System.getProperty("os.name").equalsIgnoreCase("Linux")) {
                        if (index % 2 == 0) {
                            item.setBackground(GUI.gray);
                        } else item.setBackground(GUI.white);
                    }
                    index++;
                }
            }
        });
    }

    private void openTab() {
        TableItem[] item = itemsTable.getSelection();
        if (item != null) {
            JReader.addNewPreviewTab();
            PreviewItem previewItem = GUI.openTab(item[0].getText());
            JReader.selectItem(JReader.getItems().get(itemsTable.getSelectionIndex()), Preview.folderPreview.getSelectionIndex());
            previewItem.refresh();
        }
    }
}
