// Copyright 2006 Konrad Twardowski
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.makagiga.fs.feeds;

import static java.awt.event.KeyEvent.*;

import static org.makagiga.commons.UI._;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

import org.makagiga.Tabs;
import org.makagiga.chart.ChartModel;
import org.makagiga.chart.ChartPainter;
import org.makagiga.commons.ColorProperty;
import org.makagiga.commons.Config;
import org.makagiga.commons.FS;
import org.makagiga.commons.Flags;
import org.makagiga.commons.MAction;
import org.makagiga.commons.MActionInfo;
import org.makagiga.commons.MApplication;
import org.makagiga.commons.MArrayList;
import org.makagiga.commons.MCalendar;
import org.makagiga.commons.MColor;
import org.makagiga.commons.MDataAction;
import org.makagiga.commons.MDate;
import org.makagiga.commons.MFormat;
import org.makagiga.commons.MIcon;
import org.makagiga.commons.MLogger;
import org.makagiga.commons.TK;
import org.makagiga.commons.UI;
import org.makagiga.commons.html.HTMLBuilder;
import org.makagiga.commons.painters.GlassPainter;
import org.makagiga.commons.script.ScriptYourself;
import org.makagiga.commons.swing.ActionGroup;
import org.makagiga.commons.swing.MButton;
import org.makagiga.commons.swing.MDialog;
import org.makagiga.commons.swing.MFileChooser;
import org.makagiga.commons.swing.MLabel;
import org.makagiga.commons.swing.MLinkAction;
import org.makagiga.commons.swing.MMenu;
import org.makagiga.commons.swing.MMessage;
import org.makagiga.commons.swing.MMessageLabel;
import org.makagiga.commons.swing.MNotification;
import org.makagiga.commons.swing.MPanel;
import org.makagiga.commons.swing.MScrollPane;
import org.makagiga.commons.swing.MSplitPane;
import org.makagiga.commons.swing.MTextLabel;
import org.makagiga.commons.swing.MToolBar;
import org.makagiga.editors.Editor;
import org.makagiga.editors.EditorConfig;
import org.makagiga.editors.EditorNavigation;
import org.makagiga.editors.EditorStats;
import org.makagiga.editors.TextUtils;
import org.makagiga.feeds.AbstractItem;
import org.makagiga.feeds.Enclosure;
import org.makagiga.feeds.FeedUtils;
import org.makagiga.feeds.archive.Archive;
import org.makagiga.feeds.archive.ArchiveChannel;
import org.makagiga.feeds.archive.ArchiveException;
import org.makagiga.feeds.archive.ArchiveItem;
import org.makagiga.fs.MetaInfo;
import org.makagiga.plugins.PluginSettings;
import org.makagiga.tabs.WebBrowserTab;
import org.makagiga.tree.Tree;
import org.makagiga.web.Favicon;
import org.makagiga.web.browser.SwingWebBrowser;
import org.makagiga.web.browser.WebBrowser;

/**
 * @since 2.0
 */
public class FeedViewer extends WebBrowserTab
implements
	EditorConfig,
	EditorNavigation,
	EditorStats<ChartModel>
{

	// private

	private ArticleList articleList;
	private boolean loadCompleteStory;
	private CommentsAction commentsAction;
	private Favicon favicon;
	private int readItems;
	private int totalItems;
	private final MArrayList<Enclosure> enclosures = new MArrayList<>();
	private MAction feedsSettingsAction;
	private MButton enclosuresButton;
	private MButton exportButton;
	private MLabel hostLabel;
	private MMessageLabel messageLabel;
	private MSplitPane splitPane;
	private OpenAction openAction;
	private String feedURL;
	private String oldHost;

	// package

	FeedsFS fs;

	// public

	public FeedViewer() {
		super(new SwingWebBrowser());
		
		messageLabel = new MMessageLabel();
		messageLabel.setAnimationEnabled(false);
		messageLabel.setRoundType(GlassPainter.RoundType.NONE);
		addSouth(messageLabel);
		
		articleList = new ArticleList();

		splitPane = new MSplitPane(MSplitPane.VERTICAL_SPLIT);
		splitPane.setOneTouchExpandable(true);
		splitPane.setTopComponent(new MScrollPane(articleList, MScrollPane.NO_BORDER));
		splitPane.setBottomComponent(core);
		addCenter(splitPane);

		commentsAction = new CommentsAction();
		openAction = new OpenAction();
		feedsSettingsAction = new FeedsSettingsAction();

		// enclosures
		enclosuresButton = new MButton(_("Enclosures"), MActionInfo.DOWNLOAD.getIconName()) {
			@Override
			protected MMenu onPopupMenu() {
				return FeedViewer.this.createEnclosuresMenu();
			}
		};
		enclosuresButton.setEnabled(false);
		enclosuresButton.setPopupMenuEnabled(true);

		// export
		exportButton = new MButton(_("Export"), MActionInfo.SAVE.getIconName()) {
			@Override
			protected MMenu onPopupMenu() {
				return FeedViewer.this.createExportMenu();
			}
		};
		exportButton.setPopupMenuEnabled(true);

		// host info
		hostLabel = MLabel.createSmall(null, null);

		getWebBrowser().setProperty(WebBrowser.IMAGE_FACTORY_PROPERTY, new FeedUtils.BlockImageFactory());
	}
	
	public String getLink() {
		return openAction.getURI().toString();
	}

	void loadFromMetaInfo(final MetaInfo parent, final MetaInfo metaInfo) { // package
		WebBrowser browser = getWebBrowser();

		if (!(metaInfo instanceof FeedsFS.ArticleMetaInfo))
			return;

		// auto close notifications for this feed
		MNotification notification = MNotification.getInstance();
		for (MNotification.Message i : notification) {
			if (i.getAction() instanceof FeedThread.NotificationAction) {
				FeedThread.NotificationAction action = (FeedThread.NotificationAction)i.getAction();
				if (action.getParent() == parent) {
					notification.hideMessage(i);
				}
			}
		}

		Config feedConfig = parent.getConfig();

		boolean blockImages = feedConfig.read("x.blockImages", false);
		Object imageFactoryProperty = browser.getProperty(WebBrowser.IMAGE_FACTORY_PROPERTY);
		if (imageFactoryProperty instanceof FeedUtils.BlockImageFactory) {
			FeedUtils.BlockImageFactory bif = (FeedUtils.BlockImageFactory)imageFactoryProperty;
			bif.setPolicy(
				blockImages
				? FeedUtils.BlockImageFactory.Policy.BLOCK_ALL_IMAGES
				: FeedUtils.BlockImageFactory.Policy.BLOCK_TRACKING_IMAGES
			);
		}

		loadCompleteStory = false;/*
			MWebBrowserPanel.getUseInternalBrowser() &&
			parent.getProperty("feed.loadCompleteStory", false);!!!*/
		FeedsFS.ArticleMetaInfo article = (FeedsFS.ArticleMetaInfo)metaInfo;
		String id = article.getID();
		String url = feedConfig.read("x.url", null);
		
		Archive.ItemInfo info;
		try {
			info = Archive.getInstance().get(url, id, true);
		}
		catch (ArchiveException exception) {
			MLogger.exception(exception);
			messageLabel.setErrorMessage(exception.getMessage());
			messageLabel.setVisible(true);
			
			return;
		}
		
		ArchiveChannel channel = info.getChannel();
		feedURL = url;
		
		// get stats
		readItems = 0;
		totalItems = 0;
		List<ArchiveItem> allItems = channel.getItems();
		if (!TK.isEmpty(allItems)) {
			totalItems = allItems.size();
			for (AbstractItem i : allItems) {
				if (i.isRead())
					readItems++;
			}
		}

		ArchiveItem item = info.getItem();

		articleList.update(parent, allItems, item);

		// comments link
		if (item.isCommentsLinkPresent()) {
			commentsAction.setEnabled(true);
			commentsAction.setToolTipText(UI.getLinkToolTipText(item.getCommentsLink()));
			commentsAction.setURI(item.getCommentsLink());
		}
		else {
			commentsAction.setEnabled(false);
		}

		// link
		if (item.isLinkPresent() && !TK.isEmpty(item.getLink())) {
			try {
				URL hostURL = new URL(item.getLink());
				String newHost = hostURL.getHost();
				// do not fetch favicon again if host is the same
				if (TK.isChange(newHost, oldHost)) {
					if (favicon != null) {
						favicon.cancelRequest();
						favicon.setImageObserver(null);
						favicon = null;
					}

					oldHost = newHost;
					if (blockImages) {
						hostLabel.setIcon(null);
					}
					else {
						favicon = new Favicon(hostLabel, hostURL);
						hostLabel.setIcon(favicon);
					}
				}
				hostLabel.setText(newHost);
				hostLabel.setVisible(true);
			}
			catch (MalformedURLException exception) {
				MLogger.exception(exception);
				hostLabel.setVisible(false);
			}
			
			openAction.setEnabled(true);
			openAction.setToolTipText(UI.getLinkToolTipText(item.getLink()));
			openAction.setURI(item.getLink());

			// disable "Comments" if links are identical
			if (commentsAction.isEnabled() && item.getLink().equals(item.getCommentsLink()))
				commentsAction.setEnabled(false);
		}
		else {
			hostLabel.setVisible(false);

			openAction.setEnabled(false);
			openAction.setToolTipText(null);
		}
		
		String channelTitle = parent.toString();
		if (channel.isTitlePresent()) {
			String t = channel.getTitle();
			if (!t.equals(channelTitle))
				channelTitle += " (" + t + ")";
		}

		// hide unused Back and Stop buttons
		if (browser instanceof SwingWebBrowser) {
			ActionGroup actionGroup = browser.getActionGroup();
			Action a = actionGroup.getAction(WebBrowser.BACK_ACTION);
			if (a != null)
				a.putValue(MAction.VISIBLE_KEY, false); // always hidden

			a = actionGroup.getAction(WebBrowser.FORWARD_ACTION);
			if (a != null)
				a.putValue(MAction.VISIBLE_KEY, false); // always hidden
		}

/* TODO: 2.0: set base URL
		try {
			if (channel.isLinkPresent())
				HTMLDocument.class.cast(text.getDocument()).setBase(new URL(channel.getLink()));
		}
		catch (MalformedURLException exception) {
			MLogger.exception(exception);
		}
*/
		// setup base URL for relative links
		URI baseURI = null;
		try {
			String base = channel.getBase();
			baseURI = (base == null) ? null : (new URI(base));
		}
		catch (URISyntaxException exception) {
			MLogger.exception(exception);
		}

		// reset message
		messageLabel.setVisible(false);

		if (loadCompleteStory && item.isLinkPresent()) {
			try {
				browser.setProperty(WebBrowser.HONOR_DISPLAY_PROPERTIES_PROPERTY, false);
				browser.setDocumentLocation(new URI(item.getLink()));
			}
			catch (Exception exception) {
				MLogger.exception(exception);

				loadItem(channelTitle, item, baseURI);
			}
		}
		else {
			loadItem(channelTitle, item, baseURI);
		}
		
		// enclosures
		enclosures.clear();
		if (item.hasEnclosure()) {
			enclosures.addAll(item.getEnclosure());
			enclosuresButton.setEnabled(true);
		}
		else {
			enclosuresButton.setEnabled(false);
		}
	}

	@Override
	public void onClose() {
		super.onClose();

		// collapse folder
		MetaInfo viewerParent = getMetaInfo().getParentFolder();
		if (viewerParent != null)
			Tree.getInstance().setExpanded(viewerParent, false);

		articleList = null;
		commentsAction = null;
		favicon = null;
		feedsSettingsAction = null;
		enclosures.clear();
		enclosuresButton = null;
		exportButton = null;
		hostLabel = null;
		messageLabel = null;
		splitPane = null;
		openAction = null;
	}
 
	// EditorConfig

	@Override
	public void loadConfig(final Config local) {
		loadFont();

		Config global = Config.getDefault();
		articleList.getColumnManager().readConfig(global, local, this);

		int articleListHeight = Math.min(200, articleList.getRowHeight() * 6);
		splitPane.setDividerLocation(global.readInt(getGlobalEntry("SplitPane.dividerLocation"), articleListHeight, 0));
	}

	@Override
	public void saveConfig(final Config local) {
		Config global = Config.getDefault();
		articleList.getColumnManager().writeConfig(global, local, this);

		global.write(getGlobalEntry("SplitPane.dividerLocation"), splitPane.getDividerLocation());
	}

	// EditorNavigation
	
	/**
	 * @since 4.2
	 */
	@Override
	public String getNavigationActionName(final Flags flags) {
		if (flags.isSet(NAVIGATION_PREVIOUS))
			return _("Previous New");

		if (flags.isSet(NAVIGATION_NEXT))
			return _("Next New");

		return null;
	}

	/**
	 * @since 3.6
	 */
	@Override
	public int getNavigationCapabilities() {
		return
			NAVIGATION_PREVIOUS | NAVIGATION_NEXT |
			NAVIGATION_MENU | NAVIGATION_TOOL_BAR;
	}

	/**
	 * @since 3.6
	 */
	@Override
	public JComponent getNavigationComponent() { return null; }

	/**
	 * @since 4.0
	 */
	@Override
	public void navigateTo(final Flags flags) {
		FeedsFSPlugin plugin = (FeedsFSPlugin)fs.getInfo().getPlugin();
		MAction a = plugin.actions.getAction(flags.isSet(NAVIGATION_PREVIOUS) ? "previous-new" : "next-new");
		if (a != null)
			a.fire();
	}

	// EditorPrint

	@Override
	public MessageFormat getPrintFooter(final boolean enabled) {
		if (enabled)
			return new MessageFormat(TK.centerSqueeze(getLink(), 50));
		
		return null;
	}

	@Override
	public MessageFormat getPrintHeader(final boolean enabled) {
		if (enabled) {
			MessageFormat originalHeader = super.getPrintHeader(enabled);
			
			// complete page - page number and title, or none
			if (loadCompleteStory)
				return originalHeader;

			// article description - page number, or none
			return (originalHeader == null) ? null : new MessageFormat("{0}");
		}
		
		return null;
	}
	
	// EditorStats
	
	@Override
	public ChartPainter<ChartModel> getStatsChart() {
		ChartModel model = new ChartModel();
		model.setFormat("${text} - ${number} (${percent-int})");
		int unreadItems = totalItems - readItems;
		model.addItem(_("Read Articles"), readItems, MColor.deriveColor(MColor.WHITE, 0.9f), "ui/ok");
		model.addItem(_("Unread Articles"), unreadItems, MColor.deriveColor((fs == null) ? FeedsFS.DEFAULT_UNREAD_COLOR : fs.getUnreadColor(), 0.9f));
		
		ChartPainter<ChartModel> painter = new ChartPainter<>(model);
		painter.imageScale.no();
		//painter.outlineColor.set(Color.BLACK);
		painter.textAlpha.set(0.7f);
		painter.textBackground.set(Color.WHITE);
		painter.textForeground.set(Color.BLACK);
		painter.textDistance.set(-10);
		painter.textLineSize.set(2);
		painter.textPadding.set(5);

		return painter;
	}
	
	@Override
	public String getStatsTitle() {
		MetaInfo parent = getMetaInfo().getParentFolder();

		return Objects.toString(parent, null);
	}
	
	// EditorZoom
	
	@Override
	public void resetZoom() {
		doZoom(null, true);
	}
	
	@Override
	public void zoom(final ZoomType type) {
		doZoom(type, false);
	}

	// PluginMenu

	@Override
	public void updateMenu(final String type, final MMenu menu) {
		super.updateMenu(type, menu);
		if (type.equals(VIEW_MENU_EDITOR)) {
			menu.addSeparator();
			menu.add(openAction);
			menu.add(commentsAction);
		}
	}

	@Override
	public void updateToolBar(final String type, final MToolBar toolBar) {
		super.updateToolBar(type, toolBar);
		if (type.equals(EDITOR_TOOL_BAR)) {
			toolBar.addSeparator();
			toolBar.add(openAction, MToolBar.SHOW_TEXT);
			toolBar.add(commentsAction);
			toolBar.addSeparator();
			toolBar.addButton(enclosuresButton);
			toolBar.addButton(exportButton);
			ScriptYourself.install(toolBar, "feed-viewer");
			toolBar.add(feedsSettingsAction);
			toolBar.addSeparator();
			toolBar.add(hostLabel);
		}
	}

	// protected

	@Override
	protected void initializeFont() { }
	
	@Override
	protected boolean processKeyBinding(final KeyStroke ks, final KeyEvent e, final int condition, final boolean pressed) {
		if (
			(fs != null) &&
			(condition == WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) &&
			pressed &&
			(e.getModifiers() == 0)
		) {
			FeedsFSPlugin plugin = (FeedsFSPlugin)fs.getInfo().getPlugin();
			MAction a = null;
			
			switch (e.getKeyCode()) {
				case VK_J:
					a = plugin.actions.getAction("next-new");
					break;
				case VK_K:
					a = plugin.actions.getAction("previous-new");
					break;
			}
			
			if (a != null) {
				a.fire();

				return true;
			}
		}
		
		return super.processKeyBinding(ks, e, condition, pressed);
	}

	// private

	private MMenu createEnclosuresMenu() {
		MMenu menu = new MMenu();
		for (Enclosure i : enclosures)
			menu.add(new EnclosureAction(i));
		
		return menu;
	}

	private MMenu createExportMenu() {
		MMenu menu = new MMenu();
		menu.add(new SaveAsAction(this));
		
		if (feedURL != null) {
			menu.addSeparator();
			menu.add(new AddToAction("Google Reader", "http://www.google.com/reader/view/feed/{0}", feedURL));
			menu.add(new AddToAction("Netvibes", "http://www.netvibes.com/subscribe.php?url={0}", feedURL));
		}

		return menu;
	}

	private String createHTMLView(final String channelTitle, final AbstractItem item) {
		HTMLBuilder html = new HTMLBuilder();
		html.beginHTML();
		
		Color bg = UI.isNimbus() ? Color.WHITE : UI.getBackground(getWebBrowser().getComponent());
		
		html.beginStyle();
			html.bestLinkRules(bg);
		html.endStyle();
		
		html.beginDoc();
	
		html.beginTag("center"); // HTML 3.2 rulez ;)

		String headerStyle = "background-color: " + ColorProperty.toString(MColor.getDarker(bg));
		html.beginTag(
			"table",
			"border", "0px",
			"style", "margin-bottom: 5px; width: 80%"
		);

		// feed title

		StringBuilder feedTitleCode = new StringBuilder();

		if (!TK.isEmpty(channelTitle))
			feedTitleCode.append(html.escape(channelTitle));

		if (feedTitleCode.length() > 0) {
			html.beginTag("tr");
				html.doubleTag(
					"td", feedTitleCode.toString(),
					"colspan", 2,
					"style", headerStyle
				);
			html.endTag("tr");
		}

		String articleTitle = item.getTitle();
		String articleTitleCode;
		if (TK.isEmpty(articleTitle)) {
			articleTitleCode = ("<i>" + html.escape(_("No Title")) + "</i>");
		}
		else {
			articleTitleCode = ("<b>" + html.escape(articleTitle) + "</b>");
			if (item.isLinkPresent() && !TK.isEmpty(item.getLink()))
				articleTitleCode = "<a href=\"" + html.escape(item.getLink()) + "\">" + articleTitleCode + "</a>";
		}
		
		html.beginTag("tr");
// FIXME: 2.0: unescaped title
			String author = item.getAuthor();
			if (TK.isEmpty(author)) {
				// title
				html.doubleTag(
					"td", articleTitleCode,
					"colspan", 2,
					"style", headerStyle
				);
			}
			else {
				// title
				html.doubleTag(
					"td", articleTitleCode,
					"style", headerStyle
				);
				// author
				html.doubleTag(
					"td", html.escape(_("Author: {0}", author)),
					"align", "right",
					"style", headerStyle
				);
			}
		html.endTag("tr");

		// actual article
		html.beginTag("tr");
		html.beginTag(
			"td",
			"colspan", 2
		);
		
		String code = item.getText();
		if (
			(code != null) &&
			!code.equals(articleTitle) // don't repeat text
		) {
			html.appendLine(code);
		}
		
		html.endTag("td");
		html.endTag("tr");

		// link to article
		if (item.isLinkPresent() && !TK.isEmpty(item.getLink())) {
			html.beginTag("tr");
			html.beginTag(
				"td",
				"colspan", 2,
				"style", headerStyle
			);
			html.doubleTag(
				"a",
				html.escape(_("Complete Story")),
				"href", item.getLink()
			);
			html.endTag("td");
			html.endTag("tr");
		}

		html.endTag("table");
		
		html.endTag("center");
		
		html.endDoc();
		
		return html.toString();
	}
	
	private void doZoom(final ZoomType type, final boolean reset) {
		WebBrowser browser = getWebBrowser();
		Component component = browser.getComponent();
		if (component instanceof JTextComponent) {
			if (reset)
				TextUtils.resetZoom((JTextComponent)component, -1);
			else
				TextUtils.zoom((JTextComponent)component, type);
			saveFont(getGlobalEntry("font"), component.getFont());
			Config.getDefault().sync();
		}
		applyFont();
	}

	private void loadFont() {
		Font font = loadFont(getGlobalEntry("font"));
		Component component = getWebBrowser().getComponent();
		if (component instanceof JTextComponent)
			component.setFont(font);
	}

	private void loadItem(final String channelTitle, final AbstractItem item, final URI baseURI) {
		if (!messageLabel.isVisible()) {
			java.util.Date date = (item != null) ? item.getDate() : null;
			if ((date != null) && MDate.isValid(date)) {
				MCalendar cal = MCalendar.of(date);
				if ((cal.get(MCalendar.MONTH) == MCalendar.APRIL) && (cal.getDay() == 1)) {
					messageLabel.setInfoMessage("April Fools' Day? ;-)");
					messageLabel.setVisible(true);
				}
			}
		}
	
		WebBrowser browser = getWebBrowser();
		browser.setProperty(WebBrowser.HONOR_DISPLAY_PROPERTIES_PROPERTY, true);
		browser.setDocumentContent(createHTMLView(channelTitle, item), baseURI);
	}
	
	// package protected
	
	static void applyFont() {
		for (Editor<?> i : Tabs.getInstance()) {
			if (i instanceof FeedViewer)
				FeedViewer.class.cast(i).loadFont();
		}
	}
	
	static Font loadFont(final String key) {
		Config config = Config.getDefault();
		Font font = config.readFont(Config.getPlatformKey(key), null);
		if (font == null)
			font = UI.createDefaultFont();
		
		return font;
	}
	
	static void saveFont(final String key, final Font font) {
		Config config = Config.getDefault();
		config.write(Config.getPlatformKey(key), font);
	}

	// private classes

	private static final class AddToAction extends MAction {
	
		// private
		
		private final String feedURL;
		private final String readerURL;

		// public

		@Override
		public void onAction() {
			MApplication.openURI(readerURL, feedURL);
		}

		// private

		private AddToAction(final String name, final String readerURL, final String feedURL) {
			super(_("Add to {0}", name));
			this.readerURL = readerURL;
			this.feedURL = feedURL;
		}

	}

	private static final class CommentsAction extends MLinkAction {

		// private

		private CommentsAction() {
			setEnabled(false);
			setIconName("ui/conversation");
			setName(_("Comments"));
			setSecureOpen(true);
		}

	}

	private static final class EnclosureAction extends MLinkAction {

		// private

		private final Enclosure enclosure;

		// public

		@Override
		public void onAction() {
			MDialog dialog = new MDialog(
				getSourceWindow(),
				_("Enclosure"),
				MDialog.STANDARD_DIALOG | MDialog.USER_BUTTON
			) {
				@Override
				protected void onUserClick() {
					MFileChooser chooser = MFileChooser.createFileChooser(this, _("Save As"));
					chooser.setConfigKey("enclosure");
					try {
						// set default save file name
						String urlPath = enclosure.toURL().getPath();
						int fileSep = urlPath.lastIndexOf('/');
						if ((fileSep != -1) && (fileSep < urlPath.length() - 1))
							chooser.setSelectedFile(new File(urlPath.substring(fileSep + 1)));

						if (chooser.saveDialog())
							enclosure.download(this, chooser.getSelectedFile());
					}
					catch (IOException exception) {
						MMessage.error(getSourceWindow(), exception);
					}

					reject();
				}
			};
			dialog.changeButton(dialog.getOKButton(), MActionInfo.OPEN_URI);
			dialog.changeButton(dialog.getUserButton(), MActionInfo.DOWNLOAD);

			MPanel p = MPanel.createVBoxPanel();

			p.add(MPanel.createHLabelPanel(new MTextLabel(enclosure.getURL()), "URL:"));

			p.addGap();

			if (enclosure.getLength() > 0) {
				p.add(MPanel.createHLabelPanel(new MLabel(MFormat.toAutoSize(enclosure.getLength())), _("File Size:")));
				p.addGap();
			}

			p.add(MPanel.createHLabelPanel(new MLabel(enclosure.getType()), _("File Type:")));

			p.addGap();

			p.add(MLabel.createSmall(_("Do not download/open files from untrusted sources"), MIcon.small("ui/warning")));

			dialog.addCenter(p);
			dialog.packFixed();
			p.alignLabels();

			if (dialog.exec())
				super.onAction(); // open
		}

		// private

		private EnclosureAction(final Enclosure enclosure) {
			this.enclosure = enclosure;
			setName(enclosure.toString());
			setSecureOpen(true);
			if (enclosure.getURL() == null) {
				setEnabled(false);
			}
			else {
				setEnabled(true);
				setURI(enclosure.getURL());
			}
		}

	}

	private final class FeedsSettingsAction extends MAction {

		// public

		public FeedsSettingsAction() {
			super(MActionInfo.SETTINGS);
		}
		
		@Override
		public void onAction() {
			PluginSettings.showPluginOptionsDialog(this.getSourceWindow(), MDialog.APPLY_BUTTON, FeedViewer.this.fs.getInfo());
		}

	}

	private static final class OpenAction extends MLinkAction {

		// private

		private OpenAction() {
			setActionInfo(MActionInfo.OPEN_URI);
			setEnabled(false);
			setSecureOpen(true);
		}

	}

	private static final class SaveAsAction extends MDataAction.Weak<FeedViewer> {

		// public

		@Override
		public void onAction() {
			FeedViewer viewer = get();

			MFileChooser chooser = MFileChooser.createFileChooser(getSourceWindow(), MActionInfo.SAVE_AS.getDialogTitle());
			chooser.addFilter(_("Plain Text"), "txt");
			FileFilter htmlFilter = chooser.addFilter("HTML", "html");
			chooser.setAutoAddExtension(true);//!!!fix MFileChooser
			chooser.setConfigKey("feed");
			chooser.setFileFilter(htmlFilter);
			chooser.setSelectedFile(new File(FS.replaceUnsafeCharacters(viewer.getMetaInfo().toString())));

			if (!chooser.saveDialog())
				return;

			WebBrowser browser = viewer.getWebBrowser();
			String text =
				(chooser.getFileFilter() == htmlFilter)
				? browser.getHTMLText()
				: browser.getPlainText();
			try {
				FS.write(
					chooser.getSelectedFile(),
					TK.isEmpty(text) ? "\n" : text
				);
			}
			catch (IOException exception) {
				MMessage.error(getSourceWindow(), exception);
			}
		}

		// private

		private SaveAsAction(final FeedViewer feedViewer) {
			super(feedViewer, MActionInfo.SAVE_AS);
		}

	}

}
