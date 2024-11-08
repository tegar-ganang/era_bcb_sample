package jreader.gui;

import java.util.Date;
import jreader.JReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Klasa obsługująca tworzenie nowych zakładek.
 * 
 * @author Karol
 *
 */
public class PreviewItem {

    private Browser browser;

    private Label info;

    private Link titleLink;

    private Link source;

    private CTabItem previewItem;

    private Composite header;

    private Font fontBold;

    /**
	 * Tworzy nową zakładkę.
	 * 
	 * @param text       Tytuł zakładki.
	 * @param itemImage  Ikona zakładki.
	 */
    public PreviewItem(String text, Image itemImage) {
        if (System.getProperty("os.name").equalsIgnoreCase("Linux")) {
            fontBold = new Font(GUI.display, new FontData("Arila", 12, SWT.BOLD));
        } else {
            fontBold = new Font(GUI.display, new FontData("Arila", 8, SWT.BOLD));
        }
        previewItem = new CTabItem(Preview.folderPreview, SWT.CLOSE);
        previewItem.setText(text);
        previewItem.setImage(itemImage);
        Composite comp = new Composite(Preview.folderPreview, SWT.NONE);
        comp.setLayout(new GridLayout());
        header = new Composite(comp, SWT.NONE);
        header.setLayout(new FillLayout(SWT.VERTICAL));
        header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        titleLink = new Link(header, SWT.NONE);
        source = new Link(header, SWT.NONE);
        info = new Label(header, SWT.NONE);
        if (System.getProperty("os.name").equalsIgnoreCase("Linux")) {
            browser = new Browser(comp, SWT.MOZILLA | SWT.BORDER);
        } else {
            browser = new Browser(comp, SWT.NONE | SWT.BORDER);
        }
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        previewItem.setControl(comp);
        Preview.folderPreview.setSelection(previewItem);
        Menu popupMenu = new Menu(titleLink);
        MenuItem openBrowser = new MenuItem(popupMenu, SWT.NONE);
        openBrowser.setText("Open link in external browser");
        titleLink.setMenu(popupMenu);
        openBrowser.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                int tabIndex = Preview.folderPreview.getSelectionIndex();
                BrowserControl.displayURL(JReader.getPreview(tabIndex).getCurrent().getLink());
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        browser.addListener(SWT.KeyUp, new Listener() {

            public void handleEvent(Event event) {
                if (event.keyCode == 16777230) {
                    browser.refresh();
                }
            }
        });
        browser.addProgressListener(new ProgressListener() {

            public void changed(ProgressEvent event) {
                if (event.total == 0) return;
                int ratio = event.current * 100 / event.total;
                GUI.progressBar.setSelection(ratio);
                GUI.progressBar.setVisible(true);
            }

            public void completed(ProgressEvent event) {
                GUI.progressBar.setSelection(0);
                GUI.progressBar.setVisible(false);
            }
        });
        browser.addStatusTextListener(new StatusTextListener() {

            public void changed(StatusTextEvent event) {
                GUI.statusLine.setText(event.text);
            }
        });
        browser.addMouseListener(new MouseListener() {

            public void mouseDown(MouseEvent e) {
            }

            public void mouseDoubleClick(MouseEvent arg0) {
                Preview.folderPreview.setFocus();
            }

            public void mouseUp(MouseEvent arg0) {
            }
        });
        browser.addFocusListener(Focus.setFocus((Preview.folderPreview)));
        titleLink.addFocusListener(Focus.setFocus((Preview.folderPreview)));
    }

    /**
	 * Odświeża zawartość zakładki.
	 */
    public void refresh() {
        int tabIndex = Preview.folderPreview.getSelectionIndex();
        String titleText = JReader.getPreview(tabIndex).getCurrent().getTitle();
        Date date = JReader.getPreview(tabIndex).getCurrent().getDate();
        String authorText = JReader.getPreview(tabIndex).getCurrent().getAuthor();
        String fromText = JReader.getPreview(tabIndex).getCurrent().getChannelTitle();
        String sourceText = "View channel source (XML)";
        final String url = JReader.getPreview(tabIndex).getCurrent().getLink();
        if (!previewItem.isDisposed()) if (titleText != null) previewItem.setText((titleText.length() > 20) ? titleText.substring(0, 16).concat("...") : titleText); else previewItem.setText("brak tytułu");
        titleLink.setText("<a>" + titleText + "</a>");
        titleLink.setFont(fontBold);
        browser.setText(JReader.getPreview(tabIndex).getCurrent().getHTML());
        if (JReader.getPreview(tabIndex).getCurrent().isShowingItem()) {
            String infoText = "";
            if (date != null) infoText = GUI.shortDateFormat.format(date) + "     ";
            if (authorText != null) infoText += "Author: " + authorText + "\t";
            if (fromText != null) infoText += "From: " + fromText;
            info.setText(infoText);
            source.setText("");
        } else {
            info.setText("");
            source.setText("<a>" + sourceText + "</a>");
        }
        titleLink.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                browser.setUrl(url);
            }
        });
        source.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                int tabIndex = Preview.folderPreview.getSelectionIndex();
                if (JReader.getPreview(tabIndex).getCurrent().getSource() != null) browser.setUrl(JReader.getPreview(tabIndex).getCurrent().getSource()); else browser.setText("<b>Source not found.</b>");
            }
        });
        source.addMouseMoveListener(new MouseMoveListener() {

            public void mouseMove(MouseEvent e) {
                int tabIndex = Preview.folderPreview.getSelectionIndex();
                String text = JReader.getPreview(tabIndex).getCurrent().getSource();
                if (text != null) {
                    GUI.statusLine.setText(text);
                }
            }
        });
        titleLink.addMouseMoveListener(new MouseMoveListener() {

            public void mouseMove(MouseEvent e) {
                int tabIndex = Preview.folderPreview.getSelectionIndex();
                String text = JReader.getPreview(tabIndex).getCurrent().getLink();
                if (text != null) {
                    GUI.statusLine.setText(text);
                }
            }
        });
        titleLink.addListener(SWT.KeyUp, new Listener() {

            public void handleEvent(Event event) {
                if (event.character == 'm') {
                    browser.setUrl(url);
                }
            }
        });
    }
}
