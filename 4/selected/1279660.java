package de.cinek.rssview;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.cinek.rssview.event.ArticleSelectionEvent;
import de.cinek.rssview.event.ArticleSelectionListener;
import de.cinek.rssview.event.ChannelModelEvent;
import de.cinek.rssview.event.ChannelModelListener;
import de.cinek.rssview.event.ChannelSelectionEvent;
import de.cinek.rssview.event.ChannelSelectionListener;

/**
 * TODO: Cleanup
 * 
 * @author $author$
 * @version $Id: RssChannelView.java,v 1.32 2004/10/27 23:19:08 saintedlama Exp $
 */
public class RssChannelView extends JPanel implements HyperlinkListener, ChannelModelListener, ViewComponent {

    private static Log log = LogFactory.getLog(RssChannelView.class);

    private JTextPane content;

    private RssDropDownMenu dropdownmenu;

    private RssView parent;

    private RssStatusBar statusBar = null;

    private Element bodyTag;

    private HTMLDocument htmlDoc;

    private ChannelModel model;

    private Channel currentChannel;

    private JTabbedPane tabbedPane;

    private ChannelContentPanel channelPanel;

    private ChannelSelectionListener channelSelectionListener;

    private ChannelViewArticleSelectionListener articleSelectionListener;

    /**
	 * Creates a new RssChannelView object.
	 * 
	 * @param parent DOCUMENT ME!
	 * @param statusBar DOCUMENT ME!
	 * @param model DOCUMENT ME!
	 */
    public RssChannelView(RssView parent, RssStatusBar statusBar, ChannelModel model) {
        super(new BorderLayout());
        this.parent = parent;
        this.statusBar = statusBar;
        this.model = model;
        this.channelSelectionListener = new ChannelViewSelectionListener();
        this.articleSelectionListener = new ChannelViewArticleSelectionListener();
        initComponents();
        initListeners();
    }

    private void initListeners() {
        MouseListener listener = new PopupMenuMouseAdapter();
        content.addMouseListener(listener);
        channelPanel.addMouseListener(listener);
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        content = new JTextPane();
        RssHTMLEditorKit editorKit = new RssHTMLEditorKit();
        content.setEditorKit(editorKit);
        htmlDoc = (HTMLDocument) content.getDocument();
        Element defaultRoot = htmlDoc.getDefaultRootElement();
        bodyTag = htmlDoc.getElement(defaultRoot, StyleConstants.NameAttribute, HTML.Tag.BODY);
        URL stylesheet = getClass().getResource("styles/default.css");
        if (stylesheet != null) {
            htmlDoc.getStyleSheet().importStyleSheet(stylesheet);
        } else if (log.isDebugEnabled()) {
            log.debug("Stylesheet not found!");
        }
        content.setEditable(false);
        content.addHyperlinkListener(this);
        JScrollPane articleScrollPane = new JScrollPane(content);
        channelPanel = new ChannelContentPanel();
        tabbedPane.add(ResourceBundle.getBundle("rssview").getString("Overview"), channelPanel);
        tabbedPane.add(ResourceBundle.getBundle("rssview").getString("Articles"), articleScrollPane);
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent event) {
                int idx = ((JTabbedPane) event.getSource()).getSelectedIndex();
                RssSettings.getInstance().setSelectedTabIndex(idx);
            }
        });
        add(tabbedPane, BorderLayout.CENTER);
        dropdownmenu = new RssDropDownMenu(parent, null);
        tabbedPane.setSelectedIndex(RssSettings.getInstance().getSelectedTabIndex());
    }

    /**
	 * @param channel Channel to add
	 */
    public synchronized void updateContent(Channel channel) {
        if (channel != null) {
            channelPanel.setChannel(channel);
            this.tabbedPane.setTitleAt(0, channel.getName());
        }
    }

    protected void updateContent(Article article) {
        if (article != null) {
            if (article.isDescriptionSet()) {
                content.setDocument(htmlDoc);
                removeContent();
                insertArticle(article);
                setCaretPosition(0);
            } else {
                try {
                    content.setPage(article.getLink());
                } catch (IOException ioex) {
                    log.debug("Could not open article URL", ioex);
                }
            }
            tabbedPane.setTitleAt(1, article.getTitle());
            tabbedPane.setSelectedIndex(1);
        }
    }

    protected void setCaretPosition(int position) {
        content.setCaretPosition(position);
    }

    protected void removeContent() {
        try {
            htmlDoc.remove(0, htmlDoc.getLength());
        } catch (BadLocationException ex) {
            log.error("BadLocation in HTML Document", ex);
        }
    }

    protected void maybePopup(MouseEvent e) {
        if (parent.isInteractiveMode()) {
            dropdownmenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
	 * TODO: We get this callback, when something changed in the subscription.
	 * Whether it's an article added, removed or something else, in
	 * RssChannelModel everything will end up in a call to subscriptionChanged.
	 */
    public void channelChanged(ChannelModelEvent event) {
        if (content != null) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    updateContent(currentChannel);
                }
            });
        }
    }

    public void channelAdded(ChannelModelEvent event) {
    }

    public void channelRemoved(ChannelModelEvent event) {
    }

    /**
	 * @param article
	 */
    protected synchronized void insertArticle(Article article) {
        try {
            HtmlTransformer transformer = TransformerFactory.createHtmlTransformer(article);
            String html = transformer.convertToHtml(article);
            htmlDoc.insertAfterStart(bodyTag, html);
        } catch (BadLocationException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Tried to insert at a bad location in html document", ex);
            }
        } catch (IOException ioex) {
            if (log.isDebugEnabled()) {
                log.debug("Tried to insert article", ioex);
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param e DOCUMENT ME!
	 */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
            try {
                new RssBrowserStart(e.getURL().toString());
            } catch (Exception ex) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to start Browser", ex);
                }
            }
        } else if (HyperlinkEvent.EventType.ENTERED.equals(e.getEventType())) {
            statusBar.setText("Click to open " + e.getURL() + " in your browser!");
        } else if (HyperlinkEvent.EventType.EXITED.equals(e.getEventType())) {
            statusBar.setText("");
        }
    }

    /**
	 * Getter for property channelPanel.
	 * @return Value of property channelPanel.
	 *  
	 */
    public ChannelContentPanel getChannelPanel() {
        return channelPanel;
    }

    private class PopupMenuMouseAdapter extends MouseAdapter {

        /**
		 * Creates a new PopupMenuMouseAdapter object.
		 */
        public PopupMenuMouseAdapter() {
        }

        /**
		 * DOCUMENT ME!
		 * 
		 * @param e DOCUMENT ME!
		 */
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                maybePopup(e);
            }
        }

        /**
		 * DOCUMENT ME!
		 * 
		 * @param e DOCUMENT ME!
		 */
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                maybePopup(e);
            }
        }
    }

    /**
	 * @param html
	 */
    public void setContent(String html) {
        try {
            htmlDoc.insertAfterStart(bodyTag, html);
        } catch (BadLocationException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Tried to insert at a bad location in html document", ex);
            }
        } catch (IOException ioex) {
            if (log.isDebugEnabled()) {
                log.debug("Tried to insert article", ioex);
            }
        }
    }

    private void setChannelSelection(NodePath path) {
        if (path.getLastElement() instanceof Channel) {
            currentChannel = (Channel) path.getLastElement();
            final Channel channel = currentChannel;
            if (channel != null) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        updateContent(channel);
                    }
                });
            }
        } else if (path.getLastElement() instanceof RssGroupNode) {
            RssGroupNode node = (RssGroupNode) path.getLastElement();
            final RssAggregateChannel aggregateChannel = AggregateChannelBuilder.buildAggregateChannel(node);
            currentChannel = aggregateChannel;
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    updateContent(currentChannel);
                }
            });
        } else {
            currentChannel = null;
        }
    }

    /**
	 * @param article
	 */
    private void setArticleSelection(final Article article) {
        if (article == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateContent(article);
            }
        });
    }

    /**
	 * @see de.cinek.rssview.ViewComponent#deactivate()
	 */
    public void deactivate() {
        parent.removeChannelSelectionListener(this.channelSelectionListener);
        parent.removeArticleSelectionListener(this.articleSelectionListener);
        model.removeChannelModelListener(this);
    }

    /**
	 * @see de.cinek.rssview.ViewComponent#activate()
	 */
    public void activate() {
        parent.addChannelSelectionListener(this.channelSelectionListener);
        parent.addArticleSelectionListener(this.articleSelectionListener);
        model.addChannelModelListener(this);
        setChannelSelection(new NodePath(parent.getSelectionModel().getSelectionPath()));
        setArticleSelection(parent.getArticleSelectionModel().getSelection());
    }

    private class ChannelViewSelectionListener implements ChannelSelectionListener {

        public void channelSelectionChanged(ChannelSelectionEvent evt) {
            NodePath path = new NodePath(evt.getPath());
            setChannelSelection(path);
        }
    }

    private class ChannelViewArticleSelectionListener implements ArticleSelectionListener {

        public void articleSelectionChanged(ArticleSelectionEvent evt) {
            setArticleSelection(evt.getArticle());
        }
    }
}
