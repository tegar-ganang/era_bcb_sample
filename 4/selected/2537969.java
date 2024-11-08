// Copyright 2005 Konrad Twardowski
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

package org.makagiga.fs;

import static org.makagiga.commons.UI._;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.tree.MutableTreeNode;

import org.makagiga.Vars;
import org.makagiga.commons.Config;
import org.makagiga.commons.FS;
import org.makagiga.commons.Flags;
import org.makagiga.commons.Kiosk;
import org.makagiga.commons.Lockable;
import org.makagiga.commons.MDate;
import org.makagiga.commons.MFormat;
import org.makagiga.commons.MIcon;
import org.makagiga.commons.MLogger;
import org.makagiga.commons.MProperties;
import org.makagiga.commons.Net;
import org.makagiga.commons.TK;
import org.makagiga.commons.TriBoolean;
import org.makagiga.commons.UI;
import org.makagiga.commons.annotation.Important;
import org.makagiga.commons.annotation.InvokedFromConstructor;
import org.makagiga.commons.category.CategoryList;
import org.makagiga.commons.category.CategoryManager;
import org.makagiga.commons.html.HTMLBuilder;
import org.makagiga.commons.mv.MRenderer;
import org.makagiga.commons.mv.MV;
import org.makagiga.commons.preview.DefaultPreview;
import org.makagiga.commons.preview.Preview;
import org.makagiga.commons.request.RequestInfo;
import org.makagiga.commons.request.RequestSource;
import org.makagiga.commons.security.MPermission;
import org.makagiga.commons.swing.MTreeItem;
import org.makagiga.editors.EditorCrypto;
import org.makagiga.editors.EditorPlugin;
import org.makagiga.plugins.PluginInfo;
import org.makagiga.search.SortMethod;
import org.makagiga.tags.Tags;
import org.makagiga.tags.TagsUtils;
import org.makagiga.tree.Tree;
import org.makagiga.tree.tracker.Tracker;
import org.makagiga.tree.tracker.TrackerEvent;

/**
 * A tree item.
 * 
 * Meta info contains additional information, such as file color or comment.
 *
 * @since 2.0
 */
public class MetaInfo extends MTreeItem
implements
	Comparable<MetaInfo>,
	Lockable,
	MIcon.Name,
	RequestSource<DefaultPreview.Result>
{

	// public
	
	/**
	 * Meta info <code>.properties</code> file version.
	 */
	public static final int VERSION = 1;
	
	// getToolTipText flags
	
	// 1 << 0 - deprecated and removed

	/**
	 * No "This item is locked" info.
	 */
	public static final int NO_LOCK_INFO = 1;

	/**
	 * No "Tags" info.
	 */
	public static final int NO_TAGS_INFO = 1 << 2;

	/**
	 * No "Comment" info.
	 */
	public static final int NO_COMMENT_INFO = 1 << 3;
	
	public static final int NO_LOCATION_INFO = 1 << 4;
	public static final int NO_DATE_INFO = 1 << 5;
	public static final int NO_NAME_INFO = 1 << 6;
	
	// properties
	public static final String EXTRA_COLOR = "metaInfo.extra.color";
	public static final String EXTRA_TEXT = "metaInfo.extra.text";
	public static final String OVERRIDE_ICON = "metaInfo.override.icon";
	public static final String OVERRIDE_NAME = "metaInfo.override.name";
	public static final String PREVIEW_IMAGE = "metaInfo.previewImage";
	public static final String PREVIEW_INFO = "metaInfo.previewInfo";
	public static final String PREVIEW_PROPERTIES = "metaInfo.previewProperties";
	public static final String NO_IMAGE_PREVIEW = "metaInfo.noImagePreview";
	
	/**
	 * @since 4.0
	 */
	public static final String ENCRYPTED = "metaInfo.encrypted";

	/**
	 * The owner of this @c MetaInfo object
	 * (e.g. org.makagiga.desktop.Widget).
	 * The default value is @c null.
	 *
	 * @since 3.8
	 */
	public static final String OWNER = "metaInfo.owner";

	// private

	// item flags

	/**
	 * FS root folder.
	 */
	private static final int FS_ROOT = 1;

	/**
	 * Normal folder.
	 */
	private static final int FOLDER = 1 << 1;

	/**
	 * Normal file.
	 */
	private static final int FILE = 1 << 2;

	// private static final int NOT_SUPPORTED = 1 << 3;

	/**
	 * Dynamic folder.
	 */
	private static final int DYNAMIC_FOLDER = 1 << 4;

	/**
	 * Virtual file.
	 */
	private static final int VIRTUAL_FILE = 1 << 5;

	/**
	 * Dummy item.
	 */
	private static final int DUMMY = 1 << 6;

	private static final int BOOKMARK = 1 << 7;

	private static final int LOCKED = 1 << 8;

	private static final int WRITEABLE = 1 << 9;

	// private static final int ENCRYPTED = 1 << 10;

	private static final int OPEN = 1 << 11;
	
	private static final int EXTERNAL = 1 << 12;

	private final AbstractFS fs;
	private CategoryList categoryList;
	private Color color;
	private Config config;
	private float rating;
	private static Image lockedMiniIcon;
	private int attr;
	private long _createdTime = UNDEFINED_TIME;
	private long _openTime = UNDEFINED_TIME;
	private static final long UNDEFINED_TIME = Long.MIN_VALUE;
	private Map<String, Object> properties;
	private static MetaInfoAction.Icon rendererIcon;
	private MIcon icon;
	private MIcon lockedIconCache;
	private MIcon smallIcon;
	private static final MPermission GET_CONFIG_PERMISSION = new MetaInfo.Permission("getConfig");
	private String categories;
	private String comment;
	private String iconName;
	private String id = "";
	private String name;
	private String path;
	private String tags;
	
	// public
	
	/**
	 * @mg.note Don't forget to invoke {@link #reload()}.
	 */
	@Override
	public void add(final MutableTreeNode child) {
		Tracker.add((MetaInfo)child, TrackerEvent.ID.ITEM_ADDED);
		super.add(child);
	}

	public static void addInfo(final HTMLBuilder html, final String title, final Object info) {
		html.beginTag("tr");
			if (info == null) {
				html.doubleTag("td", html.escape(title), "colspan", 2);
			}
			else {
				html.doubleTag("td", html.escape(title), "align", "right");
				html.doubleTag("td", html.escape(info.toString()));
			}
		html.endTag("tr");
	}

	public static void addSeparator(final HTMLBuilder html) {
		html.appendLine("<tr><td colspan=\"2\"><hr></td></tr>");
	}

	public static void addWarning(final HTMLBuilder html, final String text) {
		html.appendLine(
			"<tr><td colspan=\"2\"><font color=\"#ff0000\">" +
			html.escape(text) +
			"</font></td></tr>"
		);
	}

	/**
	 * @since 4.0
	 */
	public boolean canBookmark() {
		if (!Kiosk.actionBookmark.get() || Vars.treeReadOnly.get() || isExternal())
			return false;

		return
			(isAnyFolder() || isFile()) &&
			isWriteable();
	}

	public boolean canModify() {
		return ((attr & LOCKED) == 0) && ((attr & WRITEABLE) != 0);
	}
	
	/**
	 * Clears internal cached data: category list.
	 *
	 * @since 4.2
	 */
	public void clearCache() {
		categoryList = null;
	}
	
	public void clearIcon() {
		icon = null;
		iconName = null;
		smallIcon = null;
	}

	public Object clearProperty(final String key) {
		if (properties == null)
			return null;
		
		Object result = properties.remove(key);
		
		if (properties.isEmpty())
			properties = null;
		
		return result;
	}
	
	@Override
	public int compareTo(final MetaInfo another) {
		return TK.compare(this.getFilePath(), another.getFilePath(), "");
	}

	public static MetaInfo createDummy(final String name, final MIcon icon) {
		MetaInfo dummy = createVirtualFile(null, name, icon);
		dummy.attr |= DUMMY;
		
		return dummy;
	}

	public static MetaInfo createDynamicFolder(final AbstractFS fs, final String path) {
		return createDynamicFolder(fs, path, null);
	}
	
	public static MetaInfo createDynamicFolder(final AbstractFS fs, final String path, final String id) {
		MetaInfo dynamicFolder = new MetaInfo(fs, path, DYNAMIC_FOLDER);
		if (path != null) {
			File f = dynamicFolder.getFile();
			if (f.exists())
				dynamicFolder.setWriteable(f);
		}

		if (id == null)
			dynamicFolder.initID();
		else
			dynamicFolder.id = id;
		
		return dynamicFolder;
	}

	/**
	 * Creates and returns a <b>file</b> item.
	 * @param path A full path to the file
	 * @param info A plugin info associated with this file type
	 */
	public static MetaInfo createFile(final AbstractFS fs, final String path, final PluginInfo info) {
		MetaInfo file = new MetaInfo(fs, path, FILE);
		if (info != null) {
			if (!file.isCustomIcon())
				file.setIcon(info.getIcon());
		}
		file.setAllowsChildren(false);
		file.setWriteable(file.getFile());
		
		file.initID();

		return file;
	}
	
	public static MetaInfo createFile(final AbstractFS fs, final String path, final String type) {
		return createFile(fs, path, EditorPlugin.findPluginForExtension(type));
	}
	
	static MetaInfo createFSRoot(final AbstractFS fs, final String path) { // package
		return new MetaInfo(fs, path, FS_ROOT);
	}
	
	public static MetaInfo createLink(final MetaInfo metaInfo) {
		if (metaInfo.isLink())
			return null;
		
		return new LinkMetaInfo(metaInfo);
	}

	/**
	 * Creates and returns a <b>folder</b> item.
	 * @param path A full path to the directory
	 */
	public static MetaInfo createFolder(final AbstractFS fs, final String path, final String mount) {
		MetaInfo folder = new FolderMetaInfo(fs, path, mount);
		folder.initID();

		return folder;
	}

	/**
	 * Internal method. Do not use.
	 */
	public static MetaInfo createMainRoot() {
		return new MetaInfo(null, FS.makeConfigPath("vfs"), 0);
	}
	
	public static MetaInfo createVirtualFile(final AbstractFS fs, final String name, final MIcon icon) {
		MetaInfo virtualFile = new VirtualFileMetaInfo(fs, name);
		if (icon != null)
			virtualFile.setIcon(icon);

		return virtualFile;
	}

	/**
	 * @since 4.2
	 */
	public static String formatDate(final long date) {
		if (date == MDate.INVALID_TIME)
			return "";

		return new MDate(date).fancyFormat(MDate.FANCY_FORMAT_APPEND_TIME);
	}

	public String getID() { return id; }

	public void setID(final String value) {
		checkPermission("setID");
	
		id = Objects.requireNonNull(value);
	}
	
	public float getRating() { return rating; }
	
	public void setRating(final float value) { rating = value; }

	/**
	 * @since 4.0
	 */
	public File getTargetFile() {
		File f = getMountDirectory();

		return (f != null) ? f : getFile();
	}

	public String getTargetPath() {
		File f = getMountDirectory();

		return (f != null) ? f.getPath() : getFilePath();
	}
	
	public void invalidatePreview(final boolean repaint) {
		if (!isFile())
			return;
		
		clearProperty(NO_IMAGE_PREVIEW);
		clearProperty(PREVIEW_IMAGE);
		clearProperty(PREVIEW_PROPERTIES);
		@SuppressWarnings("unchecked")
		RequestInfo<DefaultPreview.Result> requestInfo = (RequestInfo<DefaultPreview.Result>)clearProperty(PREVIEW_INFO);
		if (requestInfo != null) {
			requestInfo.setCancelled(true);
			if (repaint)
				Tree.getInstance().repaint(getTreePath());
		}
	}

	public boolean isAnyFile() {
		return isFile() || isVirtualFile();
	}

	public boolean isAnyFolder() {
		return isFolder() || isDynamicFolder();
	}

	public boolean isBookmark() {
		return (attr & BOOKMARK) != 0;
	}

	public void setBookmark(final boolean value) {
		if (value)
			attr |= BOOKMARK;
		else
			attr &= ~BOOKMARK;
	}

	/**
	 * @since 3.6
	 */
	public boolean isEncrypted() {
		if (!isFile())
			return false;
		
		TriBoolean b = getProperty(ENCRYPTED, TriBoolean.UNDEFINED);
		if (b.isUndefined()) {
			b = TriBoolean.of(EditorCrypto.isEncrypted(getFile()));
			setProperty(ENCRYPTED, b);
		}
	
		return b.isTrue();
	}

	/**
	 * @since 4.2
	 */
	public boolean isExternal() {
		if (isAnyFile()) {
			MetaInfo parent = getParentFolder();

			return (parent != null) && parent.isFolder() && ((parent.attr & EXTERNAL) != 0);
		}
	
		return isFolder() && ((attr & EXTERNAL) != 0);
	}

	/**
	 * @since 3.0
	 */
	public String getCategories() { return categories; }

	/**
	 * @since 3.0
	 */
	public void setCategories(final String value) {
		categories = value;
		categoryList = null;
	}

	/**
	 * Returns a category list or @c null.
	 *
	 * @since 3.2
	 */
	public CategoryList getCategoryList() {
		if (TK.isEmpty(categories))
			return null;

		if (categoryList == null)
			categoryList = new CategoryList(CategoryManager.getSharedInstance(), categories);

		return categoryList;
	}

	/**
	 * Returns color.
	 */
	public Color getColor() { return color; }

	/**
	 * Sets color to @p value.
	 */
	public void setColor(final Color value) { color = value; }

	/**
	 * Returns comment.
	 */
	public String getComment() { return comment; }

	/**
	 * Sets comment to @p value.
	 */
	public void setComment(final String value) { comment = value; }
	
	public Config getConfig() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(GET_CONFIG_PERMISSION);
	
		return config;
	}

	/**
	 * Returns file associated with this meta info.
	 */
	public File getFile() {
		return new File(path);
	}

	/**
	 * @since 4.0
	 */
	public String getFileExtension() {
		return isFile() ? FS.getFileExtension(path) : "";
	}

	/**
	 * Returns path (full file name).
	 */
	public String getFilePath() { return path; }

	/**
	 * Sets path to @p value.
	 */
	@InvokedFromConstructor
	public void setFilePath(final String value) {
		if (isFSRoot()) {
			checkPermission("setFilePath");
		
			path = value;
			name = value;
		}
		else if (isVirtualFile()) {
			// no permission checking
		
			path = value;
			boolean needEscape = false;
			if (value != null) {
				int len = value.length();
				if (len > 0) {
					for (int i = 0; i < len; i++) {
						char c = value.charAt(i);
						if (
							(c != ' ') &&
							((c < 'A') || ((c > 'Z') && (c < 'a')) || (c > 'z'))
						) {
							needEscape = true;
							
							break;
						}
					}
				}
				
			}
			name = needEscape ? TK.escapeURL(value) : value;
		}
		else {
			checkPermission("setFilePath");
		
			path = value;
			if (isDynamicFolder()) {
				name = getPathWithoutExtension(getFile().getName());
				
				// NOTE: do not add ".properties" to the dynamic folder path
				if (config != null)
					config.setPath(path);
			}
			else if (isFile()) {
				name = getPathWithoutExtension(getFile().getName());
				if (config != null)
					config.setPath(path + ".properties");
			}
			else if (isFolder()) {
				name = getFile().getName();
				if (config != null)
					config.setPath(FS.makePath(path, ".properties"));
			}
			else {
				name = path;
			}
		}
	}
	
	public AbstractFS getFS() { return fs; }

	/**
	 * Returns icon associated with this item.
	 */
	public MIcon getIcon() {
		// no icon?
		if (icon == null) {
			if (isVirtualFile())
				return null;
			
			// no plugin?
			if (iconName == null)
				return null;

			// load custom icon
			icon = getIcon(iconName);
			// no custom icon? - set safe icon
			if (icon == null) {
				iconName = "ui/misc";
				icon = MIcon.stock(iconName);
			}
		}
		if ((icon != null) && !canModify()) {
			if (lockedIconCache != null)
				return lockedIconCache;

			if (lockedMiniIcon == null) {
				lockedMiniIcon = UI.scaleImage(
					MIcon.getImage("ui/locked"),
					icon.getIconWidth() / 3,
					icon.getIconHeight() / 3,
					UI.Quality.HIGH
				);
			}

			lockedIconCache = new MIcon(icon.getImage());
			lockedIconCache.setOverlay(lockedMiniIcon);

			return lockedIconCache;
		}

		return icon;
	}

	/**
	 * Sets icon to @p value.
	 */
	public void setIcon(final MIcon value) {
		clearProperty(OVERRIDE_ICON);
		
		if (value == null) {
			if (fs != null)
				iconName = fs.getDefaultIconName(this);
			else
				iconName = null;
			if (iconName == null) {
				if (isDynamicFolder())
					iconName = "dynamicfolder";
				else if (isFolder())
					iconName = "ui/folder";
				else
					iconName = "ui/file";
			}
			icon = MIcon.stock(iconName);
		}
		else {
			iconName = null;
			icon = value;
		}
		lockedIconCache = null;
		smallIcon = null;
	}
	
	public MetaInfo getLink() { return null; }
	
	public boolean isLink() { return false; }

	/**
	 * @since 4.0
	 */
	public File getMountDirectory() { return null; }

	/**
	 * @since 4.0
	 */
	public void setMountDirectory(final File value) { }

	/**
	 * Returns a "nice" representation of this item.
	 * This function should be used only in <i>GUI-display</i> related functions.
	 */
	public String getNicePath() {
		if (fs == null)
			return toString();

		if (isFSRoot())
			return fs.getName();
		
		if (isVirtualFile()) {
			MetaInfo parentFolder = getParentFolder();
			
			if (parentFolder == null)
				return path;
			
			return parentFolder.getNicePath() + " > " + path;
		}

		String dir;
		if (isRoot()) {
			dir = path;
		}
		else if (path.startsWith(fs.getDirectory().getPath())) {
			dir = path.substring(fs.getDirectory().getPath().length() + 1);
		}
		else {
			// external directory (mounted folder)
			// or item in the Trash
			MetaInfo parentFolder = getParentFolder();
			if (parentFolder != null) {
				dir = "...";
				if (!parentFolder.isFSRoot())
					dir += (File.separator + parentFolder.name);
			}
			else {
				dir = "";
			}
			if (path != null)
				dir += (File.separator + getFile().getName());
		}
		
		return fs.getName() + " > " + TK.unescapeURL(getPathWithoutExtension(dir).replace(File.separator, " > "));
	}

	/**
	 * @since 3.4
	 */
	public String getOverrideName() {
		String overrideName = getProperty(OVERRIDE_NAME, null);
		String result = toString();
		if (overrideName != null)
			result = overrideName.replace("%name", result);

		return result;
	}

	public MetaInfo getParentFolder() {
		return (MetaInfo)getParentItem();
	}
	
	/**
	 * @since 2.4
	 */
	public PluginInfo getPluginInfo() {
		String type = getFileExtension();

		if (type.isEmpty())
			return null;

		return EditorPlugin.findPluginForExtension(type);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getProperty(final String key, final T defaultValue) {
		if (properties == null)
			return defaultValue;
		
		Object result = properties.get(key);

		return (result == null) ? defaultValue : (T)result;
	}

	public Object setProperty(final String key, final Object value) {
		if (properties == null)
			properties = new HashMap<>();
			
		return properties.put(key, value);
	}

	/**
	 * @since 3.8.1
	 */
	public MIcon getSmallIcon() {
		if (smallIcon != null)
			return smallIcon;

		if (isCustomIcon()) {
			smallIcon =
				Net.isLocalFile(iconName)
				? MIcon.fromFileURI(iconName, MIcon.getSmallSize())
				: MIcon.small(iconName);
		}
		else {
			String n = MIcon.getName(icon);
			if (n != null)
				smallIcon = MIcon.small(n);
		}

		if (MIcon.isEmpty(smallIcon)) {
			smallIcon = getIcon();
			if (smallIcon != null)
				smallIcon = smallIcon.scaleSmall();
		}

		return smallIcon;
	}

	/**
	 * @since 3.0
	 */
	public SortMethod getSortMethod() {
		return config.readEnum("sortMethod", SortMethod.NAME);
	}

	public String getTags() { return tags; }
	
	public void setTags(final String value) {
		tags = value;

		if (isFile() || isAnyFolder())
			Tags.addAll(value);
	}

	public void setTags(final Tags value) {
		String s = value.toString();
		setTags("".equals(s) ? null : s);
	}

	/**
	 * Returns tool tip text or @c null.
	 * @param aFlags A flags (e.g. @ref NO_LOCK_INFO)
	 */
	public String getToolTipText(final int aFlags) {
		Flags flags = Flags.valueOf(aFlags);

		HTMLBuilder builder = new HTMLBuilder();
		builder.beginHTML();
		builder.beginDoc();
		
		builder.beginTag(
			"table",
			"border", 0
		);

		// name
		if (flags.isClear(NO_NAME_INFO))
			addInfo(builder, TK.centerSqueeze(toString(), 128), null);

		// type
		String type = getFileExtension();
		// dynamic folder/folder
		if (type.isEmpty()) {
			if (isDynamicFolder())
				type = _("Virtual Folder");
		}
		// file
		else {
			PluginInfo info = EditorPlugin.findPluginForExtension(type);
			// format: Type Description (ext)
			type = ((info == null) ? _("Unknown") : (info.name.get()) + " (" + type + ")");
		}
		if (!type.isEmpty())
			addInfo(builder, _("Type:"), type);

		if (!TK.isEmpty(comment) && flags.isClear(NO_COMMENT_INFO)) {
			// get first line from a multiline comment
			String[] s = TK.splitPair(comment.trim(), '\n', TK.SPLIT_PAIR_NULL_ERROR);
			addInfo(
				builder,
				_("Comment:"),
				TK.centerSqueeze((s == null) ? comment : (s[0] + "..."), 128)
			);
		}

		if ((tags != null) && flags.isClear(NO_TAGS_INFO))
			addInfo(builder, _("Tags:"), TagsUtils.getLocalizedTags(tags));

		if (categories != null)
			addInfo(builder, _("Categories:"), categories);

		if (flags.isClear(NO_DATE_INFO)) {
			long createdTime = getCreatedTime();
			if (createdTime != 0)
				addDateInfo(builder, _("Created:"), createdTime);
			if (isFile())
				addDateInfo(builder, _("Modified:"), getFile().lastModified());
		}
		
		if (flags.isClear(NO_LOCATION_INFO))
			addInfo(builder, _("Location:"), TK.centerSqueeze(getNicePath(), 128));

		File mount = getMountDirectory();
		if (mount != null)
			addInfo(builder, _("Mounted External Directory:"), mount);

		if (!isWriteable())
			addWarning(builder, _("This item is not writeable (read only)"));
		else if (isLocked() && flags.isClear(NO_LOCK_INFO))
			addWarning(builder, _("This item is locked (read only)"));

		if (flags.isClear(NO_DATE_INFO)) {
			boolean isSeparator = false;
			long openTime = getLastOpenTime();
			if (isFile() && (openTime != 0)) {
				addSeparator(builder);
				isSeparator = true;
				addDateInfo(builder, _("Last Opened:"), openTime);
			}
			long age = getAge();
			if (age != -1) {
				if (!isSeparator)
					addSeparator(builder);
				addInfo(builder, _("Age:"), _("{0} day(s)", age));
			}
		}
		
		// file size
		if (isFile()) {
			addInfo(builder, _("Size:"), MFormat.toAutoSize(getFile().length()));
		}
		
		if (Vars.treeShowMetaInfo.get()) {
			MProperties metaProperties = getProperty(PREVIEW_PROPERTIES, null);
			if (metaProperties != null) {
				String imageType = metaProperties.getProperty(Preview.IMAGE_TYPE_PROPERTY, "");
				if (!TK.isEmpty(imageType)) {
					addSeparator(builder);
					addInfo(builder, _("Dimensions:"), String.format(
						"%s x %s",
						metaProperties.getProperty(Preview.IMAGE_WIDTH_PROPERTY, "?"),
						metaProperties.getProperty(Preview.IMAGE_HEIGHT_PROPERTY, "?")
					));
					addInfo(builder, _("Type:"), imageType);
					addInfo(builder, "bpp:", metaProperties.getProperty(Preview.IMAGE_BPP_PROPERTY, "?"));
				}
				String url = metaProperties.getProperty(Preview.URL_PROPERTY, null);
				if (url != null) {
					addSeparator(builder);
					addInfo(builder, "URL:", TK.centerSqueeze(url, 128));
				}
			}
		}
		
		if (getFS() instanceof FSProperties)
			FSProperties.class.cast(getFS()).updateInfo(this, builder);
		
		builder.endTag("table");
		builder.endDoc();
		
		// TEST: MLogger.debug("fs", builder.toString());
		
		// 75 - no HTML content
		return (builder.length() == 75) ? null : builder.toString();
	}

	public boolean isCustomIcon() {
		return iconName != null;
	}
	
	/**
	 * Returns @c true if this item is <b>dummy</b>.
	 * 
	 * @since 3.0
	 */
	public boolean isDummy() {
		return (attr & DUMMY) != 0;
	}

	public boolean isDynamicFolder() {
		return (attr & DYNAMIC_FOLDER) != 0;
	}

	/**
	 * Returns @c true if this item is a <b>data file</b>.
	 */
	public boolean isFile() {
		return (attr & FILE) != 0;
	}

	/**
	 * Returns @c true if this item is a <b>folder</b>.
	 */
	public boolean isFolder() {
		return (attr & FOLDER) != 0;
	}

	/**
	 * Returns @c true if this item is a <b>file system root</b>.
	 */
	public boolean isFSRoot() {
		return (attr & FS_ROOT) != 0;
	}

	@Override
	public boolean isLocked() {
		return (attr & LOCKED) != 0;
	}

	@Override
	public void setLocked(final boolean value) {
		if (value)
			attr |= LOCKED;
		else
			attr &= ~LOCKED;

		lockedIconCache = null;
	}

	/**
	 * @since 3.8.2
	 */
	public boolean isOpen() {
		if (!isAnyFile())
			return false;

		return (attr & OPEN) != 0;
	}

	/**
	 * @since 3.8.2
	 */
	public void setOpen(final boolean value) {
		if (value)
			attr |= OPEN;
		else
			attr &= ~OPEN;
	}

	/**
	 * @since 4.0
	 */
	public boolean isPermission(final AbstractFS.ActionType action) {
		if (fs == null)
			return false;

		return fs.isPermission(this, action);
	}

	/**
	 * @since 3.0
	 */
	public boolean isSortable() {
		return isFolder() || isFSRoot();
	}
	
	public boolean isVirtualFile() {
		return (attr & VIRTUAL_FILE) != 0;
	}

	public boolean isWriteable() {
		return (attr & WRITEABLE) != 0;
	}

	public void setWriteable(final boolean value) {
		if (value)
			attr |= WRITEABLE;
		else
			attr &= ~WRITEABLE;
	}
	
	public void setWriteable(final File file) {
		setWriteable(!Vars.treeReadOnly.get() && file.canWrite());
	}
	
	public List<MetaInfo> list(final MV modelView) {
		// NOTE: do not use "link"
		return toList(modelView);
	}
	
	/**
	 * @since 4.0
	 */
	public long getCreatedTime() {
		if (isVirtualFile())
			return 0;
	
		if ((config != null) && (_createdTime == UNDEFINED_TIME)) {
			_createdTime = config.readDateValue("created", 0);
			if (_createdTime == 0)
				_createdTime = getFile().lastModified();
		}
		
		return _createdTime;
	}

	/**
	 * Sets "creation" time to @p value.
	 */
	public void setCreatedTime(final java.util.Date value) {
		_createdTime = value.getTime();
	}

	public void setExtraInfo(final Object info) {
		if (info == null) {
			clearProperty(EXTRA_COLOR);
			clearProperty(EXTRA_TEXT);
		}
		else {
			setProperty(EXTRA_COLOR, Color.WHITE);
			setProperty(EXTRA_TEXT, info.toString()/* must be java.lang.String */);
		}
	}
	
	/**
	 * @since 4.0
	 */
	public long getLastOpenTime() {
		if ((config != null) && (_openTime == UNDEFINED_TIME))
			_openTime = config.readDateValue("lastOpen", 0);
		
		return _openTime;
	}
	
	/**
	 * Sets "last open" time to <i>now</i>.
	 */
	public void setLastOpenTime() {
		_openTime = MDate.currentTime();
	}

	@Override
	public void setupRenderer(final MRenderer<?> r) {
		if (rendererIcon == null)
			rendererIcon = new MetaInfoAction.Icon();
		rendererIcon.update(this);
		r.setIcon(rendererIcon);
		r.setText(toString());
	}

	/**
	 * Saves meta info to file.
	 * Quick info:
	 * - Directory (folder) saves its info in ".properties" file.
	 * - File saves its info in "Foo.properties" file, where "Foo" is its file name.
	 * - Root folder has no properties.
	 * 
	 * @since 2.4
	 */
	public void sync() {
		if (config == null)
			return;
		
		if (isExternal() || !isWriteable())
			return;
		
		config.write("bookmark", (attr & BOOKMARK) != 0);
		if (TK.isEmpty(categories))
			config.removeString("categories");
		else
			config.write("categories", categories);
		config.write("color", color);
		config.write("comment", comment);
		if (_createdTime != UNDEFINED_TIME)
			config.writeDate("created", _createdTime);
		config.write("icon", iconName);
		config.write("id", id);
		if (_openTime != UNDEFINED_TIME)
			config.writeDate("lastOpen", _openTime);
		config.write("locked", (attr & LOCKED) != 0);
		//config.write("order", order);
		config.write("rating", rating);
		if (TK.isEmpty(tags))
			config.removeString("tags");
		else
			config.write("tags", tags);
		config.write("version", VERSION);
		
		// folder properties
		if (isFolder()) {
			File mount = getMountDirectory();
			config.write("mount", (mount == null) ? null : mount.getPath());
		}
		config.sync();
	}

	/**
	 * Returns the icon name, or @c null if no icon.
	 */
	@Override
	public String getIconName() { return iconName; }

	/**
	 * Sets icon to @p name.
	 */
	@Override
	public void setIconName(final String value) {
		clearProperty(OVERRIDE_ICON);

		iconName = value;
		icon = getIcon(iconName);
		if (icon == null) {
			// set default icon
			setIcon(null);

			return;
		}
		lockedIconCache = null;
		smallIcon = null;
	}
	
	public void refresh(final boolean repaint) {
		if (isDummy())
			return;
	
		// NOTE: do not use "link"
		Tree tree = Tree.getInstance();
		tree.getModel().nodeChanged(this);
		if (repaint)
			tree.repaint(getTreePath());

		for (AbstractFS i : tree) {
			if (i != getFS())
				i.refreshNotify(this, repaint);
		}
	}

	public void reload() {
		Tree tree = Tree.getInstance();
		boolean expanded = tree.isExpanded(this);
		tree.getModel().reload(this);
		if (expanded)
			tree.setExpanded(this, true);
	}
	
	/**
	 * @since 4.0
	 */
	public Path toPath() {
		return Paths.get(path);
	}

	/**
	 * Returns unescaped name.
	 * File name without directory and extension, "%20" -> " ".
	 */
	@Important
	@Override
	public String toString() {
		// WARNING: do not modify
		if (isFSRoot())
			return fs.getName();
		
		return TK.unescapeURL(name);
	}

	/**
	 * @since 4.0
	 */
	public Path toTargetPath() {
		File f = getMountDirectory();

		return (f != null) ? f.toPath() : toPath();
	}

	// RequestSource

	@Override
	public void requestDone(final RequestInfo<DefaultPreview.Result> info, final DefaultPreview.Result result) {
		Icon iconProperty = (result == null) ? null : (Icon)result.getProperties().get(Preview.ICON_PROPERTY);
		Image image = (result == null) ? null : result.getImage();
		if (image == null) {
			clearProperty(PREVIEW_IMAGE);
			setProperty(NO_IMAGE_PREVIEW, true);
		}
		else {
			setProperty(PREVIEW_IMAGE, image);
			clearProperty(NO_IMAGE_PREVIEW);
		}
		
		if (result == null)
			clearProperty(PREVIEW_PROPERTIES);
		else
			setProperty(PREVIEW_PROPERTIES, result.getProperties());
		
		if ((iconProperty != null) && (iconName == null))
			setIcon(new MIcon(UI.toBufferedImage(iconProperty)).scaleUI());

		refresh(false);

		Tracker.add(this, TrackerEvent.ID.ITEM_PREVIEW_CHANGED);
	}

	// protected

	/**
	 * @since 3.4
	 */
	protected MetaInfo(final AbstractFS fs, final String path, final int attr) {
		this.fs = fs;
		this.attr = attr | WRITEABLE;
		setFilePath(path);
		loadFromFile();
	}

	// private

	private void addDateInfo(final HTMLBuilder html, final String title, final long date) {
		if (date != MDate.INVALID_TIME) {
			String s = formatDate(date);
			if (!s.isEmpty())
				addInfo(html, title, s);
		}
	}

	private static void checkPermission(final String name) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new MetaInfo.Permission(name));
	}

	private long getAge() {
		if (isVirtualFile())
			return -1;

		long createdTime = getCreatedTime();
		
		return (createdTime == 0) ? -1 : MDate.now().getDayCount(createdTime);
	}

	private static MIcon getIcon(final String path) {
		return (path == null) ? null : MIcon.fromFileURI(path, MIcon.getUISize());
	}

	private String getPathWithoutExtension(final String pathWithExtension) {
		if (isLink())
			return getLink().getPathWithoutExtension(pathWithExtension);
		
		if (isFolder())
			return pathWithExtension;

		return FS.getBaseName(pathWithExtension);
	}

	private void initID() {
		id = config.read("id", null);
		if (id == null) {
			if (isWriteable())
				id = TK.createRandomUUID();
			else
				id = "";
		}
	}

	private void loadFromFile() {
		if (isLink()) {
			getLink().loadFromFile();
			
			return;
		}

		String configPath;
		if (isDynamicFolder())
			configPath = path;
		else if (isVirtualFile())
			configPath = null; // no ".properties" file
		else if (isFile())
			configPath = (path + ".properties");
		else if (isFolder() || isFSRoot())
			configPath = FS.makePath(path, ".properties");
		else
			configPath = null;

		config =
			(configPath == null)
			? null
			: new Config(configPath, (isWriteable() || (getMountDirectory() != null)) ? 0 : Config.READ_ONLY);
		
		if (config == null)
			return;
		
		if (config.read("bookmark", false))
			attr |= BOOKMARK;

		categories = config.read("categories", null);
		color = config.readColor("color", null);
		comment = config.read("comment", null);

		if (config.read("locked", false)) // before "icon" read
			attr |= LOCKED;
		
		String defaultIconName = (fs != null) ? fs.getDefaultIconName(this) : null;
		boolean hasFSDefaultIcon;
		if (defaultIconName == null) {
			hasFSDefaultIcon = false;
			if (isDynamicFolder())
				defaultIconName = "dynamicfolder";
			else if (isFolder())
				defaultIconName = "ui/folder";
			else
				defaultIconName = null;
		}
		else {
			hasFSDefaultIcon = true;
		}
		iconName = config.read("icon", defaultIconName);
		
		// override legacy icon names
		if (
			hasFSDefaultIcon &&
			(
				(isDynamicFolder() && "dynamicfolder".equals(iconName)) ||
				(isFolder() && "ui/folder".equals(iconName))
			)
		) {
			iconName = defaultIconName;
		}

		//order = config.readInt("order", 0);
		rating = config.readFloat("rating", 0.0f);
		setTags(config.read("tags", null)); // use setter to update all tags cache

		// folder properties
		if (isFolder()) {
			String mount = config.read("mount", null);
			if (!TK.isEmpty(mount))
				setMountDirectory(new File(mount));
		}
	}
	
	// package
	
	MetaInfo(final AbstractFS fs) {
		this.fs = fs;
	}

	void setExternal() {
		attr |= EXTERNAL;
	}

	// public classes

	/**
	 * @since 4.2
	 */
	public static final class Permission extends MPermission {

		// private

		private Permission(final String name) {
			super(
				name,
				ThreatLevel.HIGH,
				"Document/Tree Item Information"
			);
		}

	}

	/**
	 * @since 3.4
	 */
	public static class VirtualFileMetaInfo extends MetaInfo {

		// public

		public VirtualFileMetaInfo(final AbstractFS fs, final String name) {
			super(fs, name, VIRTUAL_FILE);
			setAllowsChildren(false);
		}

	}

	// private classes

	private static final class FolderMetaInfo extends MetaInfo {

		// private

		private File mountDirectory;

		// public

		@Override
		public File getMountDirectory() { return mountDirectory; }

		@Override
		public void setMountDirectory(final File value) {
			MetaInfo.checkPermission("setMountDirectory");
		
			mountDirectory = value;
		}

		// private

		private FolderMetaInfo(final AbstractFS fs, final String path, final String mount) {
			super(fs, path, FOLDER);

			// adjust permissions
			File directory = getFile();
			setWriteable(directory);

			// use the "mount" parameter
			if ((mountDirectory == null) && (mount != null)) {
				mountDirectory = new File(mount);
				setMountDirectory(mountDirectory);
			}

			// mount external directory
			if (mountDirectory != null) {
				try {
					MountManager.validate(mountDirectory);
					MountManager.mount(this);
					setWriteable(mountDirectory.canWrite());
				}
				catch (FSException exception) {
					MLogger.error("vfs", exception.getMessage());

					// disable mount folder if already exist
					if (exception instanceof MountManager.ValidationException) {
						mountDirectory = null;
					}

					setProperty(OVERRIDE_ICON, "ui/error");
					setProperty(OVERRIDE_NAME, "%name [" + exception.getMessage() + "]");
				}
			}
		}

	}

}
