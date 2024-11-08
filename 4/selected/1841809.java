package jreader.gui;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import jreader.Channel;
import jreader.JReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Tworzy listę subskrypcji.
 * 
 * @author Karol
 *
 */
public class SubsList {

    /**
	 * Lista subskrypcji.
	 */
    public static Table subsList;

    public static Image def = new Image(Subscriptions.subComposite.getDisplay(), "data" + File.separator + "icons" + File.separator + "unread.png");

    static Font fontBold;

    public SubsList(final Composite comp) {
        subsList = new Table(comp, SWT.SINGLE);
        Font initialFont = subsList.getFont();
        FontData[] fontData = initialFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setHeight(10);
            fontData[i].setStyle(SWT.BOLD);
        }
        fontBold = new Font(comp.getDisplay(), fontData);
        refresh();
        Menu popupMenu = new Menu(subsList);
        MenuItem markRead = new MenuItem(popupMenu, SWT.NONE);
        markRead.setText("Mark as read");
        MenuItem synchronize = new MenuItem(popupMenu, SWT.NONE);
        synchronize.setText("Synchronize");
        MenuItem changeTag = new MenuItem(popupMenu, SWT.NONE);
        changeTag.setText("Edit tags");
        MenuItem delete = new MenuItem(popupMenu, SWT.NONE);
        delete.setText("Delete");
        subsList.setMenu(popupMenu);
        subsList.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                int indeks = subsList.getSelectionIndex();
                ItemsTable.refresh();
                if (Preview.folderPreview.getItemCount() != 0) {
                    JReader.selectChannel(indeks, Preview.folderPreview.getSelectionIndex());
                    Preview.previewItemList.get(Preview.folderPreview.getSelectionIndex()).refresh();
                } else {
                    JReader.addNewPreviewTab();
                    JReader.selectChannel(indeks, 0);
                    GUI.openTab(JReader.getChannel(indeks).getTitle()).refresh();
                }
            }
        });
        markRead.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                int indeks = subsList.getSelectionIndex();
                if (indeks == -1) return;
                JReader.markChannelAsRead(JReader.getChannel(indeks));
                SubsList.refresh();
                ItemsTable.refresh();
                Filters.refresh();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        synchronize.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                int indeks = subsList.getSelectionIndex();
                if (indeks == -1) return;
                try {
                    JReader.updateChannel(JReader.getChannel(indeks));
                    JReader.getChannel(indeks).setFail(false);
                    GUI.statusLine.setText("Channel has been updated.");
                } catch (SAXParseException spe) {
                    GUI.statusLine.setText("Failed to update channel.");
                    errorDialog("Failed to update channel.\n" + "Source is not a valid XML.\n" + "Error in line " + spe.getLineNumber() + ". " + "Details:\n" + spe.getLocalizedMessage());
                    JReader.getChannel(indeks).setFail(true);
                } catch (SAXException saxe) {
                    GUI.statusLine.setText("Failed to update channel.");
                    errorDialog("Failed to update channel.\n" + "XML parser error has occured.");
                    JReader.getChannel(indeks).setFail(true);
                } catch (SocketException se) {
                    GUI.statusLine.setText("Failed to update channel.");
                    errorDialog("Failed to update channel.\n" + se.getLocalizedMessage());
                    JReader.getChannel(indeks).setFail(true);
                } catch (IOException ioe) {
                    GUI.statusLine.setText("Failed to update channel.");
                    errorDialog("Failed to update channel.\n" + "Unable to connect to the site.");
                    JReader.getChannel(indeks).setFail(true);
                }
                SubsList.refresh();
                ItemsTable.refresh();
                Filters.refresh();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        changeTag.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                final int indeks = subsList.getSelectionIndex();
                if (indeks == -1) return;
                final Shell changeShell = new Shell(comp.getDisplay(), SWT.DIALOG_TRIM);
                changeShell.setText("Edit tags: " + subsList.getItem(indeks).getText());
                RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
                rowLayout.pack = true;
                rowLayout.justify = true;
                rowLayout.marginWidth = 40;
                rowLayout.center = true;
                rowLayout.spacing = 10;
                changeShell.setLayout(rowLayout);
                changeShell.setLocation(300, 300);
                new Label(changeShell, SWT.NONE).setText("Enter tags: ");
                final Text tags = new Text(changeShell, SWT.BORDER);
                tags.setText(JReader.getChannel(indeks).getTagsAsString());
                Button okBut = new Button(changeShell, SWT.PUSH);
                okBut.setText("OK");
                okBut.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event event) {
                        JReader.editTags(JReader.getChannel(indeks), tags.getText());
                        TagList.refresh();
                        changeShell.close();
                    }
                });
                tags.addListener(SWT.DefaultSelection, new Listener() {

                    public void handleEvent(Event e) {
                        JReader.editTags(JReader.getChannel(indeks), tags.getText());
                        TagList.refresh();
                        changeShell.close();
                    }
                });
                changeShell.pack();
                changeShell.open();
                TagList.refresh();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        delete.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                int indeks = subsList.getSelectionIndex();
                if (indeks == -1) return;
                JReader.removeChannel(indeks);
                SubsList.refresh();
                ItemsTable.refresh();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        subsList.addFocusListener(Focus.setFocus((Subscriptions.folderSubs)));
    }

    /**
	 * Odświeża listę subskrypcji.
	 */
    public static void refresh() {
        GUI.display.asyncExec(new Runnable() {

            public void run() {
                subsList.removeAll();
                for (Channel ch : JReader.getVisibleChannels()) {
                    TableItem subs = new TableItem(SubsList.subsList, SWT.NONE);
                    if (ch.getIconPath() == null) if (ch.getUnreadItemsCount() == 0) subs.setImage(ItemsTable.read); else subs.setImage(def); else {
                        try {
                            ImageData imData = new ImageData(ch.getIconPath());
                            imData = imData.scaledTo(16, 16);
                            subs.setImage(new Image(Subscriptions.subComposite.getDisplay(), imData));
                        } catch (SWTException swte) {
                            if (ch.getUnreadItemsCount() == 0) subs.setImage(ItemsTable.read); else subs.setImage(def);
                        }
                    }
                    subs.setText(ch.getTitle() + " (" + ch.getUnreadItemsCount() + "/" + ch.getItems().size() + ")");
                    if (ch.getUnreadItemsCount() != 0) subs.setFont(fontBold);
                    if (ch.isFail()) subs.setForeground(new Color(Display.getCurrent(), 255, 0, 0)); else subs.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
                }
            }
        });
    }

    private void errorDialog(String err) {
        MessageBox messageBox = new MessageBox(GUI.shell, SWT.ICON_ERROR | SWT.OK);
        messageBox.setText("Error");
        messageBox.setMessage(err);
        messageBox.open();
    }
}
