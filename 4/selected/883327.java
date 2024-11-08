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

package org.makagiga.fs;

import static org.makagiga.commons.UI._;

import java.awt.Color;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.makagiga.MainWindow;
import org.makagiga.Tabs;
import org.makagiga.Vars;
import org.makagiga.commons.BooleanProperty;
import org.makagiga.commons.Config;
import org.makagiga.commons.FS;
import org.makagiga.commons.Flags;
import org.makagiga.commons.MActionInfo;
import org.makagiga.commons.MArrayList;
import org.makagiga.commons.MIcon;
import org.makagiga.commons.MLogger;
import org.makagiga.commons.MStringBuilder;
import org.makagiga.commons.StringList;
import org.makagiga.commons.TK;
import org.makagiga.commons.UI;
import org.makagiga.commons.annotation.Obsolete;
import org.makagiga.commons.annotation.Uninstantiable;
import org.makagiga.commons.category.CategoryList;
import org.makagiga.commons.mv.MRenderer;
import org.makagiga.commons.mv.MV;
import org.makagiga.commons.preview.DefaultPreview;
import org.makagiga.commons.swing.Input;
import org.makagiga.commons.swing.MCheckBox;
import org.makagiga.commons.swing.MDialog;
import org.makagiga.commons.swing.MGroupLayout;
import org.makagiga.commons.swing.MLabel;
import org.makagiga.commons.swing.MList;
import org.makagiga.commons.swing.MMessage;
import org.makagiga.commons.swing.MRating;
import org.makagiga.commons.swing.MTextField;
import org.makagiga.commons.swing.MTextFieldPanel;
import org.makagiga.commons.swing.MainView;
import org.makagiga.commons.validator.TextComponentValidator;
import org.makagiga.editors.EditorPlugin;
import org.makagiga.tags.Tags;
import org.makagiga.tags.TagsUtils;
import org.makagiga.tree.Tree;
import org.makagiga.tree.tracker.Tracker;
import org.makagiga.tree.tracker.TrackerEvent;
import org.makagiga.tree.version.VersionControl;
import org.makagiga.tree.version.VersionException;
import org.makagiga.tree.version.VersionInfo;

public final class FSHelper {

	// public

	/**
	 * @since 4.0
	 */
	public enum Permission {
		
		// public
		
		DEFAULT(_("Use default system settings")),
		FORBIDDEN(_("View or write is forbidden")),
		READ(_("Can view content of this folder/subfolders")),
		READ_WRITE(_("Can view or MODIFY content of this folder/subfolders"));
				
		// private
				
		private String text;
		
		// public
		
		@Override
		public String toString() { return text; }
				
		// private		
		
		private Permission(final String text) {
			this.text = text;
		}
				
	};

	public static final int NO_UPDATE_COMPONENTS = 1;
	
	/**
	 * @since 3.0
	 */
	public static final int UPDATE_ID = 1 << 1;

	/**
	 * @since 3.0
	 */
	public static final int APPLY_PERMISSIONS = 1 << 2;
	
	// private
	
	private static final boolean DEFAULT_DEFAULT = true;
	private static final boolean DEFAULT_READ = false;
	private static final boolean DEFAULT_WRITE = false;

	// public

	/**
	 * @since 4.0
	 */
	public static void applyPermissions(final MetaInfo metaInfo, final boolean useParent) {
		MetaInfo parent = metaInfo.getParentFolder();
		if (metaInfo.isDynamicFolder()) {
			applyPermissionsFromParent(parent, metaInfo.getFile());
		}
		else if (metaInfo.isFile()) {
			applyPermissionsFromParent(parent, metaInfo.getFile());
			applyPermissionsFromParent(parent, new File(metaInfo.getFilePath() + ".properties"));
			try {
				List<VersionInfo> versions = VersionControl.getAllVersions(metaInfo);
				for (VersionInfo i : versions)
					applyPermissionsFromParent(parent, i.toFile());
			}
			catch (VersionException exception) {
				MLogger.exception(exception);
			}
		}
		else if (metaInfo.isFolder()) {
			if (useParent)
				applyPermissionsToFolder(metaInfo, readFolderPermission(parent));
			recursiveApplyPermissions(metaInfo);
		}
	}

	/**
	 * @since 4.0
	 */
	public static void applyPermissionsToFolder(final MetaInfo folder, final Permission permission) {
		if (folder.isExternal())
			return;
	
		applyPermissionsToPath(folder.getFile(), permission);
		applyPermissionsToPath(new File(folder.getFilePath(), ".properties"), permission);
		writePermission(folder, permission);
		folder.sync();
		File mount = folder.getMountDirectory();
		if (mount != null) {
			applyPermissionsToPath(mount, permission);
			applyPermissionsToPath(new File(mount, ".properties"), permission);
		}
	}

	/**
	 * @since 4.0
	 */
	@Obsolete
	public static void checkIfExists(final File file) throws FSException {
		if (file.exists())
			throw new FSException(_("File already exists"));
	}

	public static void checkName(final String name) throws FSException {
		if (TK.isEmpty(name))
			throw new FSException(_("Name cannot be empty"));
		
		if (name.length() > AbstractFS.MAX_NAME_LENGTH)
			throw new FSException(_("Name is too long"));
	}
	
	/**
	 * @since 2.0
	 */
	public static void checkReadOnly() throws FSException {
		if (Vars.treeReadOnly.get())
			throw new FSException(_("The \"Tree\" is in read only mode"));
	}

	/**
	 * Check if the URL-escaped {@code name} is not too long.
	 *
	 * @since 4.2
	 */
	public static void checkRealNameLength(final String name) throws FSException {
		String s = TK.escapeURL(name, TK.ESCAPE_ASTERISK);//!!!review TK.escapeURL and TK.ESCAPE_ASTERISK
		
		if (s.length() > 200)
			throw new FSException(_("Name is too long"));
	}

	/**
	 * @since 3.0
	 */
	public static boolean copy(final MetaInfo metaInfo, final MetaInfo toFolder, final int flags) throws FSException {
		checkReadOnly();
		
		if (metaInfo == null)
			return false;

		if (metaInfo.isVirtualFile())
			return false;

		if (toFolder == null)
			return false;

		if (!toFolder.isAnyFolder() && !toFolder.isFSRoot())
			return false;
		
		boolean applyPermissions = (flags & APPLY_PERMISSIONS) != 0;

		if (metaInfo.isDynamicFolder()) {
			MetaInfo dynamicFolder = copyDynamicFolder(metaInfo, toFolder, flags);
			if (applyPermissions)
				applyPermissions(dynamicFolder, true);
		}
		else if (metaInfo.isFile()) {
			MetaInfo file = copyFile(metaInfo, toFolder, flags);
			if (applyPermissions)
				applyPermissions(file, true);
		}
		else if (metaInfo.isFolder()) {
			MetaInfo folder;
			File mount = metaInfo.getMountDirectory();
			if (mount != null) {
				MountManager.unmount(metaInfo);
				folder = copyFolder(metaInfo, toFolder, flags);
				MountManager.mount(folder);
				folder.getFS().scan(folder, mount);
				folder.reload();
			}
			else {
				folder = copyFolder(metaInfo, toFolder, flags);
				recursiveCopy(metaInfo, folder, flags);
			}
			if (applyPermissions) {
				applyPermissionsFromProperties(folder);
				applyPermissions(folder, false);
			}
		}

		return true;
	}

	/**
	 * @since 2.0
	 */
	public static void deleteItems(final boolean move, final MetaInfo... selection) throws FSException {
		deleteItems(move, false, selection);
	}

	private static void deleteItems(final boolean move, final boolean secure, final MetaInfo... selection) throws FSException {
		checkReadOnly();
		
		MArrayList<MetaInfo> list = new MArrayList<>();
		try {
			for (MetaInfo i : selection)
				deleteItem(list, i, move, secure);
		}
		finally {
			for (AbstractFS fs : Tree.getInstance())
				fs.deleteNotify(list.toArray(MetaInfo.class));
		}
	}

	public static void deleteUI(final MetaInfo... aMetaInfos) {
		if (TK.isEmpty(aMetaInfos))
			return;
		
		// put files before folders; this fixes selection problems
		final MetaInfo[] metaInfos = aMetaInfos.clone();
		Arrays.sort(metaInfos, new MetaInfoComparator() {
			@Override
			public int compare(final MetaInfo o1, final MetaInfo o2) {
				return this.compareValues(o1, o2);
			}
			@Override
			protected int compareValues(final MetaInfo o1, final MetaInfo o2) {
				return TK.compare(o1.getFilePath(), o2.getFilePath(), "") * -1;
			}
		} );

		final Tree tree = Tree.getInstance();

		// trash support
		final AbstractFS trash = tree.getFS("trash");
		final BooleanProperty trashEnabled = new BooleanProperty(true);

		MDialog confirmation = new MDialog(
			MainView.getWindow(),
			_("Confirm"),
			MDialog.STANDARD_DIALOG | MDialog.USER_BUTTON
		) {
			@Override
			protected void onUserClick() {
				if (trash != null) {
					try {
						moveToTrash(trash, metaInfos);
					}
					catch (FSException exception) {
						MMessage.error(null, exception);
					}
					reject();
				}
			}
		};
		confirmation.changeButton(confirmation.getOKButton(), MActionInfo.DELETE);

		MGroupLayout layout = confirmation.getMainPanel().setGroupLayout(true);
		
		StringList fileList = new StringList(metaInfos.length);
		for (MetaInfo i : metaInfos)
			fileList.add(i.getNicePath());
		MList<Object> filesToDelete = MMessage.createListView(MIcon.stock("ui/delete"), fileList.toArray());
		filesToDelete.setCellRenderer(MRenderer.getSharedInstance());

		// show additional info
		final MLabel info = new MLabel();
		for (MetaInfo i : metaInfos) {
			new Tree.Scanner(i) {
				public void processItem(final MetaInfo item) {
					// disable trash support if files are already in the trash
					if ((trash != null) && trashEnabled.get() && item.isNodeAncestor(trash.getRoot()))
						trashEnabled.no();

					if ((item.getMountDirectory() != null) && TK.isEmpty(info.getText()))
						info.setText(_("Note: Content of the mounted directories will not be deleted"));
				}
			};
		}

		layout
			.beginRows()
			.addScrollable(filesToDelete, _("Delete selected item(s)?"));

		if (!TK.isEmpty(info.getText()))
			layout.addComponent(info);

		MCheckBox secureDelete = new MCheckBox(_("Secure Delete (experimental)")) {
			@Override
			protected void onClick() {
				MDialog dialog = (MDialog)UI.windowFor(this);
				dialog.getUserButton().setEnabled(!this.isSelected());
			}
		};
		secureDelete.setToolTipText(UI.makeHTML(
			_("Overwrite file contents with random data before delete.") + "<br>" +
			_("Note: This feature is system-dependent<br>and may not work correctly on all platforms.")
		));
		layout.addComponent(secureDelete);

		layout.end();

		// trash support
		if ((trash == null) || !trashEnabled.get())
			confirmation.getUserButton().setVisible(false);
		else
			confirmation.changeButton(confirmation.getUserButton(), _("Move To Trash"), "fulltrashcan");

		confirmation.pack();

		if (!confirmation.exec(confirmation.getCancelButton()))
			return;

		try {
			deleteItems(false, secureDelete.isSelected(), metaInfos);
		}
		catch (FSException exception) {
			MMessage.error(null, exception);
		}
	}

	/**
	 * @since 4.0
	 */
	public static String getUniqueName(final File directory, final String baseName) {
		return getUniqueName(directory, baseName, null);
	}

	/**
	 * @since 4.0
	 */
	public static String getUniqueName(final File directory, final String baseName, final String extension) {//!!!sec
		int number = 1;
		SecurityManager sm = System.getSecurityManager();
		MStringBuilder testName = new MStringBuilder();
		while (true) {
			testName.reset();
			if (number > 1)
				testName.append(baseName).append(' ').append(number);
			else
				testName.append(baseName);
			if (!TK.isEmpty(extension))
				testName.append('.').append(extension);

			String unescapedFileName = testName.toString();
			final File file = new File(directory, TK.escapeURL(unescapedFileName));
			boolean exists;
			if (sm == null) {
				exists = file.exists();
			}
			else {
				exists = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
					@Override
					public Boolean run() {
						return file.exists();
					}
				} );
			}
			
			number++;
			
			if (!exists)
				return unescapedFileName;
		}
	}

	/**
	 * @since 4.0
	 */
	public static boolean isMakagigaFolder(final File directory) {
		return new File(directory, ".properties").exists();
	}

	public static boolean isSameFS(final List<MetaInfo> metaInfos) {
		if (metaInfos.size() == 1)
			return true;

		Set<AbstractFS> fs = new HashSet<>();
		for (MetaInfo i : metaInfos)
			fs.add(i.getFS());

		return (fs.size() == 1);
	}

	/**
	 * @since 4.0
	 */
	public static Permission readFolderPermission(final MetaInfo folder) {
		if (folder.isFSRoot())
			return Permission.DEFAULT;
			
		if (!folder.isFolder()) {
			MLogger.warning("fs", "Folder expected: %s", folder);
			//MLogger.trace();

			return Permission.DEFAULT;
		}
		
		Config config = folder.getConfig();

		if (config.read("permission.default", DEFAULT_DEFAULT))
			return Permission.DEFAULT;

		boolean read = config.read("permission.read", DEFAULT_READ);
		boolean write = config.read("permission.write", DEFAULT_WRITE);

		if (read && write)
			return Permission.READ_WRITE;

		if (read)
			return Permission.READ;
		
		return Permission.FORBIDDEN;
	}

	/**
	 * @since 2.0
	 */
	public static String removePropertiesSuffix(final String path) {
		int i = path.lastIndexOf(".properties");
		
		if (i != -1)
			return path.substring(0, i);
		
		return path;
	}

	public static void rename(final int aFlags, final MetaInfo metaInfo, final String aNewName) throws FSException {
		checkReadOnly();
		
		if (!metaInfo.isPermission(AbstractFS.ActionType.RENAME))
			throw new FSException(_("No permissions to rename \"{0}\"", metaInfo.getNicePath()));

		MetaInfo parent = metaInfo.getParentFolder();

		if (parent == null)
			return;

		String newName = TK.escapeURL(aNewName, TK.ESCAPE_ASTERISK);
		String oldName = TK.escapeURL(metaInfo.toString(), TK.ESCAPE_ASTERISK);
		String oldPath = metaInfo.getFilePath();

		checkName(newName);

		if (newName.equals(oldName))
			return;

		if (metaInfo.isDynamicFolder())
			newName = FS.makePath(parent.getTargetPath(), newName + ".properties");
		else if (metaInfo.isFile())
			newName = FS.makePath(parent.getTargetPath(), newName + "." + metaInfo.getFileExtension());
		else if (metaInfo.isFolder())
			newName = FS.makePath(parent.getTargetPath(), newName);
		
		if (new File(newName).exists())
			throw new FSException(_("Item with this name already exists"));

// FIXME: 3.0: do VersionControl.rename atomically
		// rename backups
		try {
			VersionControl.rename(metaInfo, newName);
		}
		catch (VersionException exception) {
			throw new FSException(_("Could not rename\n\"{0}\"\nto \"{1}\"", metaInfo.getNicePath(), newName), exception);
		}

		// rename file
		if (!metaInfo.getFile().renameTo(new File(newName)))
			throw new FSException(_("Could not rename\n\"{0}\"\nto \"{1}\"", metaInfo.getNicePath(), newName));
		
		// dynamic folder
		if (metaInfo.isDynamicFolder()) {
			// already renamed
			metaInfo.setFilePath(newName);
		}
		// file
		else if (metaInfo.isFile()) {
			File oldFile = new File(metaInfo.getFilePath() + ".properties");
			File newFile = new File(newName + ".properties");
			oldFile.renameTo(newFile);
			metaInfo.setFilePath(newName);
		}
		// folder
		else if (metaInfo.isFolder()) {
			final String OLD_PATH = metaInfo.getFilePath(); // #1
			final String NEW_PATH = newName;
			metaInfo.setFilePath(newName); // #2
			new Tree.Scanner(metaInfo, false) {
				public void processItem(final MetaInfo metaInfo) {
					String path = metaInfo.getFilePath();
					if (path.startsWith(OLD_PATH + File.separator)) {
						String newPath = NEW_PATH + path.substring(OLD_PATH.length());
						metaInfo.setFilePath(newPath);
					}
				}
			};
		}
		
		Tracker.add(metaInfo, TrackerEvent.ID.ITEM_RENAMED, oldPath);

		Flags flags = Flags.valueOf(aFlags);
		if (flags.isClear(NO_UPDATE_COMPONENTS))
			MainWindow.getInstance().updateComponents(metaInfo);
	}

	public static void renameUI(final int flags, final MetaInfo metaInfo, final String newName) {
		try {
			rename(flags, metaInfo, newName);
		}
		catch (FSException exception) {
			MMessage.error(null, exception);
		}
	}
	
	/**
	 * @since 2.0
	 */
	public static void renameUI(final Window owner, final MetaInfo metaInfo) {
		if (!metaInfo.isPermission(AbstractFS.ActionType.RENAME))
			return;

		MMessage<MTextFieldPanel> inputMessage = Input.createMessage(owner, metaInfo.toString(), _("New Name:"), _("Rename"), null, "rename");
		MTextField name = inputMessage.getView().getTextField();

		inputMessage.getValidatorSupport().add(0, new NameValidator(name));
		inputMessage.installValidatorMessage();
		
		if (inputMessage.exec(name))
			renameUI(0, metaInfo, name.getText());
	}

	/**
	 * @since 4.0
	 */
	public static void setProperty(final int key, final Object value, final MetaInfo... selection) {
		Flags capabilities;
		FSProperties fsp;
		Object convertedValue = value;

		for (MetaInfo i : selection) {
			switch (key) {
				case FSProperties.META_INFO_CATEGORY:
					if (value instanceof CategoryList)
						i.setCategories(CategoryList.class.cast(value).toString());
					else
						i.setCategories((String)value);
					break;
				case FSProperties.META_INFO_COLOR:
					i.setColor((Color)value);
					break;
				case FSProperties.META_INFO_COMMENT:
					i.setComment((String)value);
					break;
				case FSProperties.META_INFO_ICON:
					if (value instanceof MIcon) {
						i.setIcon((MIcon)value);
					}
					else if (value instanceof String) {
						i.setIconName((String)value);
					}
					else {
						if (i.isFile())
							i.setIcon(EditorPlugin.findIconForExtension(i.getFileExtension()));
						else
							i.setIcon(null);
					}
					Tracker.add(i, TrackerEvent.ID.ITEM_ICON_CHANGED);
					break;
				case FSProperties.META_INFO_RATING:
					// convert to float
					if (value instanceof MRating.Star)
						convertedValue = MRating.Star.class.cast(value).toFloat();
					else
						convertedValue = value;
					i.setRating((Float)convertedValue);
					break;
				case FSProperties.META_INFO_TAGS:
					if (value instanceof Tags) {
						Tags before = TagsUtils.removeDuplicates(i.getTags());
						Tags after = (Tags)value;
						i.setTags(after);
						
						// flush tag cache
						if (!after.equals(before))
							Tags.clearAll();
					}
					else {
						i.setTags((String)value);
					}
					break;
				default:
					throw new IllegalArgumentException("Unknown property key: " + key);
			}
			
			if (i.getFS() instanceof FSProperties) {
				fsp = (FSProperties)i.getFS();
				capabilities = fsp.getMetaInfoCapabilities(i);
			}
			else {
				fsp = null;
				capabilities = null;
			}
			if ((capabilities != null) && capabilities.isSet(key))
				fsp.setMetaInfoProperty(i, key, convertedValue);
			
			i.sync();
		}
		
		MainWindow.getInstance().updateComponents(selection);
	}

	// private

	@Uninstantiable
	private FSHelper() { }

	private static void applyPermissionsFromParent(final MetaInfo parent, final File file) {
		applyPermissionsToPath(file, readFolderPermission(parent));
	}

	private static void applyPermissionsFromProperties(final MetaInfo folder) {
		Permission permission = readFolderPermission(folder);
		File file = folder.getFile();
		applyPermissionsToPath(file, permission);
		applyPermissionsToPath(new File(file, ".properties"), permission);
	}

	private static void applyPermissionsToPath(final File file, final Permission permission) {
		MLogger.debug("vfs", "%s, permission = %s", file, ((permission == null) ? null : permission.name()));
		boolean readable = DEFAULT_READ;
		boolean writeable = DEFAULT_WRITE;
		boolean useDefaults = false;
		if (permission == Permission.DEFAULT) {
			File parent = file.getParentFile();
			
			if (parent == null)
				return;
			
			readable = parent.canRead();
			writeable = parent.canWrite();
			useDefaults = true;
		}
		else if (permission == Permission.FORBIDDEN) {
			readable = false;
			writeable = false;
		}
		else if (permission == Permission.READ) {
			readable = true;
			writeable = false;
		}
		else if (permission == Permission.READ_WRITE) {
			readable = true;
			writeable = true;
		}
		
		if (file.isDirectory()) {
			file.setExecutable(false, false);
			file.setExecutable(true, !readable);
		}
		file.setReadable(false, false);
		file.setReadable(true, !readable);
		file.setWritable(false, false);
		file.setWritable(true, !writeable || useDefaults);
	}

	private static MetaInfo copyDynamicFolder(final MetaInfo metaInfo, final MetaInfo toFolder, final int flags) throws FSException {
		// save before copy
		metaInfo.sync();

		String source = metaInfo.getFilePath();
		String sourceName = new File(source).getName();
		String sourceNameWithoutSuffix = removePropertiesSuffix(sourceName);
		String destination = FS.makePath(
			toFolder.getTargetPath(),
			TK.escapeURL(getUniqueName(toFolder.getTargetFile(), TK.unescapeURL(sourceNameWithoutSuffix), "properties"))
		);
		MLogger.debug("vfs", "COPY DYNAMIC FOLDER: source=\"%s\" destination=\"%s\"", source, destination);

		// copy file
		try {
			FS.copyFile(source, destination);
			updateID(destination, flags);
		}
		catch (Exception exception) {
			MLogger.exception(exception);

			copyException(metaInfo, toFolder);
		}
		// update tree
		MetaInfo dynamicFolder = MetaInfo.createDynamicFolder(toFolder.getFS(), destination);
		Tree.getInstance().addItem(toFolder, dynamicFolder);

		return dynamicFolder;
	}

	private static void copyException(final MetaInfo source, final MetaInfo destination) throws FSException {
		throw new FSException(_("Could not copy \"{0}\" to \"{1}\"", source.getNicePath(), destination.getNicePath()));
	}

	private static MetaInfo copyFile(final MetaInfo metaInfo, final MetaInfo toFolder, final int flags) throws FSException {
		// save before copy
		final Tabs tabs = Tabs.getInstance();
		int index = tabs.findEditor(metaInfo);
		if (index != -1) {
			// try to save editor
			if (!tabs.saveEditor(tabs.getTabAt(index), Tabs.NORMAL_SAVE))
				// cancel copy if saving failed (e.g. bad password)
				copyException(metaInfo, toFolder);
		}
		metaInfo.sync();

		String source = metaInfo.getFilePath();
		String sourceExtension = FS.getFileExtension(source);
		String sourceName = new File(source).getName();
		String sourceNameWithoutSuffix = FS.getBaseName(sourceName);
		String targetName = TK.escapeURL(getUniqueName(toFolder.getTargetFile(), TK.unescapeURL(sourceNameWithoutSuffix), sourceExtension));
		String destination = FS.makePath(toFolder.getTargetPath(), targetName);
		MLogger.debug("vfs", "COPY FILE: source=\"%s\" destination=\"%s\"", source, destination);

		// copy file
		try {
			FS.copyFile(source, destination);
		}
		catch (Exception exception) {
			MLogger.exception(exception);

			copyException(metaInfo, toFolder);
		}
		
		// copy properties
		try {
			File sourceProperties = new File(metaInfo.getFilePath() + ".properties");
			if (sourceProperties.exists()) {
				FS.copyFile(sourceProperties.getPath(), destination + ".properties");
				updateID(destination + ".properties", flags);
			}
		}
		catch (Exception exception) {
			MLogger.exception(exception);
		}
		
		// copy backups
		try {
			VersionControl.copy(metaInfo, toFolder.getTargetPath(), targetName);
		}
		catch (VersionException exception) {
			copyException(metaInfo, toFolder);
		}

		// update tree
		MetaInfo file = MetaInfo.createFile(toFolder.getFS(), destination, sourceExtension);
		Tree.getInstance().addItem(toFolder, file);

		return file;
	}

	private static MetaInfo copyFolder(final MetaInfo metaInfo, final MetaInfo toFolder, final int flags) throws FSException {
		// save before copy
		metaInfo.sync();

		File sourceFile = metaInfo.getFile();
		String sourceName = sourceFile.getName();
		String destinationName = getUniqueName(toFolder.getTargetFile(), TK.unescapeURL(sourceName));
		File destinationFile = new File(toFolder.getTargetPath(), TK.escapeURL(destinationName));

		MLogger.debug("vfs", "COPY FOLDER: source=\"%s\" destination=\"%s\"", sourceFile, destinationFile);
		if (!destinationFile.mkdirs())
			copyException(metaInfo, toFolder);

		// copy properties
		sourceFile = new File(sourceFile, ".properties");
		destinationFile = new File(destinationFile, ".properties");
		MLogger.debug("vfs", "COPY FOLDER PROPERTIES: source=\"%s\" destination=\"%s\"", sourceFile, destinationFile);
		try {
			FS.copyFile(sourceFile, destinationFile);
			if (!toFolder.isExternal())
				updateID(destinationFile.getPath(), flags);
		}
		catch (FileNotFoundException exception) { } // quiet
		catch (Exception exception) {
			MLogger.exception(exception);
		}

		// update tree
		MetaInfo folder = toFolder.getFS().createFolder(toFolder, destinationName, null, false);

		if (folder == null)
			copyException(metaInfo, toFolder);
		
		return folder;
	}

	private static void deleteException(final MetaInfo metaInfo) throws FSException {
		throw new FSException(_("Could not delete \"{0}\"", metaInfo.getNicePath()));
	}

	private static void deleteFile(final MetaInfo metaInfo, final boolean secure) throws FSException {
		// close editor w/o saving (if any)
		final Tabs tabs = Tabs.getInstance();
		tabs.closeEditorAt(tabs.findEditor(metaInfo), Tabs.REMOVE_TAB);

		File file = metaInfo.getFile();
		File properties = new File(file + ".properties");

		MLogger.debug("vfs", "DELETE FILE: " + file);

		deleteFile(properties, secure); // quiet

		// delete backups
		try {
			VersionControl.deleteAllVersions(metaInfo, false, secure);
		}
		catch (VersionException exception) {
			MLogger.exception(exception);
		}
		
		// delete preview cache
		metaInfo.invalidatePreview(false);
		DefaultPreview.getInstance().removePreview(file);

		if (!deleteFile(file, secure))
			deleteException(metaInfo);
	}

	private static boolean deleteFile(final File file, final boolean secure) {
		if (secure) {
			MLogger.debug("vfs", "Secure delete: %s", file);
			try {
				FS.secureDelete(file);

				return true;
			}
			catch (IOException exception) {
				MLogger.exception(exception);

				return file.delete(); // try normal delete
			}
		}
		else {
			MLogger.debug("vfs", "Normal delete: %s", file);

			return file.delete();
		}
	}

	private static void deleteFolder(final MetaInfo metaInfo, final boolean secure) throws FSException {
		File folder = metaInfo.getFile();
		File properties = new File(folder, ".properties");

		MLogger.debug("vfs", "DELETE FOLDER: " + folder);

		deleteFile(properties, secure); // quiet

		if (!folder.delete())
			deleteException(metaInfo);
	}

	private static void deleteItem(final List<MetaInfo> list, final MetaInfo metaInfo, final boolean move, final boolean secure) throws FSException {
		if (!metaInfo.isPermission(AbstractFS.ActionType.DELETE))
			throw new FSException(_("No permissions to delete \"{0}\".\nPlease make sure that the selected item is unlocked.", metaInfo.getNicePath()));

		final Tree tree = Tree.getInstance();

		if (metaInfo.isLink()) {
			MetaInfo link = metaInfo.getLink();
			MLogger.debug("vfs", "DELETE LINK: " + metaInfo);
			if (link != metaInfo) {
				MLogger.debug("vfs", "DELETE LINK TARGET: " + link);
				deleteItem(list, link, move, secure);
			}
			tree.deleteItem(metaInfo);
			list.add(link);
		}
		else if (metaInfo.isDynamicFolder()) {
			// delete *.properties
			if (!deleteFile(metaInfo.getFile(), secure))
				deleteException(metaInfo);

			tree.deleteItem(metaInfo);
			list.add(metaInfo);
		}
		else if (metaInfo.isVirtualFile()) {
			// no physical file
			tree.deleteItem(metaInfo);
			list.add(metaInfo);
		}
		else if (metaInfo.isFile()) {
			deleteFile(metaInfo, secure);
			tree.deleteItem(metaInfo);
			list.add(metaInfo);
		}
		else if (metaInfo.isFolder()) {
			if (metaInfo.getMountDirectory() != null) {
				tree.removeAllChildren(metaInfo);
				MountManager.unmount(metaInfo);
			}
			else {
				recursiveDelete(list, metaInfo, move, secure);
			}
			deleteFolder(metaInfo, secure);
			tree.deleteItem(metaInfo);
			list.add(metaInfo);
		}
		else {
			MLogger.warning("fs", "Cannot delete: %s", metaInfo);
		}

		// notify FS
		metaInfo.getFS().deleteNotify(metaInfo, move);

		// flush tag cache
		Tags.clearAll();
	}

	private static void moveToTrash(final AbstractFS trash, final MetaInfo... metaInfos) throws FSException {
		trash.importMetaInfos(trash.getRoot(), true,  Arrays.asList(metaInfos));
	}

	private static void recursiveApplyPermissions(final MetaInfo folder) {
		for (MetaInfo i : folder.list(MV.MODEL))
			applyPermissions(i, true);
	}

	private static void recursiveCopy(final MetaInfo metaInfo, final MetaInfo toFolder, final int flags) throws FSException {
		for (MetaInfo i : metaInfo.list(MV.MODEL))
			copy(i, toFolder, flags);
	}

	private static void recursiveDelete(final List<MetaInfo> list, final MetaInfo metaInfo, final boolean move, final boolean secure) throws FSException {
		for (MetaInfo i : metaInfo.list(MV.MODEL))
			deleteItem(list, i, move, secure);
	}
	
	private static void updateID(final String properties, final int flags) {
		if ((flags & UPDATE_ID) == 0)
			return;
		
		MLogger.debug("fs", "Updating properties ID: %s", properties);
		Config config = new Config(properties);
		if (!config.isError()) {
			config.write("id", TK.createRandomUUID());
			config.sync();
		}
	}

	private static void writePermission(final MetaInfo folder, final Permission permission) {
		if (folder.isFSRoot())
			return;

		if (!folder.isFolder()) {
			MLogger.warning("fs", "Folder expected: %s", folder);
			//MLogger.trace();
			
			return;
		}
		
		Config config = folder.getConfig();
		switch (permission) {
			case DEFAULT:
				config.write("permission.default", true);
				config.removeBoolean("permission.read");
				config.removeBoolean("permission.write");
				break;
			case FORBIDDEN:
				config.write("permission.default", false);
				config.write("permission.read", false);
				config.write("permission.write", false);
				break;
			case READ:
				config.write("permission.default", false);
				config.write("permission.read", true);
				config.write("permission.write", false);
				break;
			case READ_WRITE:
				config.write("permission.default", false);
				config.write("permission.read", true);
				config.write("permission.write", true);
				break;
		}
	}

	// public classes

	/**
	 * @since 3.4
	 */
	public static class NameValidator extends TextComponentValidator {

		// private

		private static boolean egg = true;

		// public

		public NameValidator() { }

		public NameValidator(final MTextField component) {
			super(component);
		}

		// protected

		protected boolean isValid() throws Exception {
			String text = getText();
			setMessageType(MessageType.ERROR);
			FSHelper.checkName(text);
			FSHelper.checkRealNameLength(text);

			// NOTE: for backward compatibility do not put this
			// into FSHelper.checkName
			if (text.trim().isEmpty())
				throw new FSException(_("Name cannot be empty"));

			if (egg && "JAVA".equalsIgnoreCase(text)) {
				egg = false;
				setMessageType(MessageType.INFO);

				throw new Exception("Damn good coffee!");
			}

			return true;
		}

	}

}
