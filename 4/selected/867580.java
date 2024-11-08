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

package org.makagiga.commons.swing;

import static org.makagiga.commons.UI._;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.makagiga.commons.ClipboardException;
import org.makagiga.commons.FS;
import org.makagiga.commons.MAction;
import org.makagiga.commons.MActionInfo;
import org.makagiga.commons.MApplication;
import org.makagiga.commons.MClipboard;
import org.makagiga.commons.MDate;
import org.makagiga.commons.MFormat;
import org.makagiga.commons.MIcon;
import org.makagiga.commons.MLogger;
import org.makagiga.commons.OS;
import org.makagiga.commons.PassiveException;
import org.makagiga.commons.Property;
import org.makagiga.commons.TK;
import org.makagiga.commons.UI;
import org.makagiga.commons.io.LogFile;
import org.makagiga.commons.mv.MRenderer;
import org.makagiga.commons.preview.PreviewPanel;
import org.makagiga.commons.swing.event.MMouseAdapter;
		
/**
 * Misc. message and input dialogs.
 *
 * @since 2.0, 4.0 (org.makagiga.commons.swing package)
 */
public class MMessage<V extends JComponent> extends MDialog {

	// private

	private WeakReference<V> viewRef;

	// public

	public MMessage(final Window owner, final String label, final String title, final Icon icon, final V view, final int flags) {
		super(owner, title, icon, flags);
		init(label, view);
	}

	public MMessage(final Window owner, final String label, final String title, final String iconName, final V view, final int flags) {
		super(owner, title, iconName, flags);
		init(label, view);
	}

	/**
	 * Displays a confirmation box.
	 * @param owner the owner window
	 * @param text A message text
	 * @return @c true if @b OK pressed; otherwise @c false
	 */
	public static boolean confirm(final Window owner, final String text) {
		return customConfirm(owner, MIcon.stock("ui/question"), null, null, text);
	}

	public static boolean confirmDelete(final Window owner, final String text) {
		return confirmDelete(owner, text, null);
	}
	
	public static boolean confirmDelete(final Window owner, final String text, final Object info) {
		return customConfirm(
			owner,
			MIcon.stock("ui/delete"),
			MActionInfo.DELETE,
			null,
			UI.makeHTML(text + makeInfo(info))
		);
	}

	/**
	 * @since 3.0
	 */
	public static boolean confirmFileOverwrite(final Window parent, final String oldFileDisplayName, final File oldFile, final File newFile) {
		Objects.requireNonNull(oldFile);
		
		FileInfoPanel oldFileInfo = new FileInfoPanel(oldFile, _("Existing File"));
		FileInfoPanel newFileInfo = (newFile != null) ? (new FileInfoPanel(newFile, _("New File"))) : null;
		
		MDialog dialog = new MDialog(
			parent,
			_("File already exists. Overwrite?"),
			MIcon.stock("ui/question"),
			MDialog.STANDARD_DIALOG
		);
		dialog.changeButton(dialog.getOKButton(), _("Overwrite"), "ui/warning");
		
		MPanel p = dialog.getMainPanel();
		p.setVBoxLayout();
		
		if (oldFileDisplayName != null)
			oldFileInfo.addInfo(_("Name:"), oldFileDisplayName);
		p.add(oldFileInfo);
		
		if (newFileInfo != null) {
			p.addContentGap();
			p.add(newFileInfo);
		}
		
		oldFileInfo.alignLabels();
		oldFileInfo.startPreview();
		if (newFileInfo != null) {
			newFileInfo.alignLabels();
			newFileInfo.startPreview();
		}
		
		dialog.pack();
		boolean result = dialog.exec();
		
		oldFileInfo.stopPreview();
		if (newFileInfo != null)
			newFileInfo.stopPreview();
		
		return result;
	}

	/**
	 * @since 4.0
	 */
	public static boolean confirmFileOverwrite(final Window parent, final File file) {
		return customConfirm(
			parent, null,
			MActionInfo.OVERWRITE,
			null,
			UI.makeHTML(_("File already exists. Overwrite?") + makeInfo(file))
		);
	}

	/**
	 * @since 4.0
	 */
	public static MPanel createErrorPanel(final Throwable throwable, final int flags) {
		MPanel p = MPanel.createBorderPanel();

		MLabel error = new MLabel(_("Error"), getRandomErrorIconName());
		error.makeLargeMessage();
		p.addCenter(error);

		p.addSouth(new MButton(_("Details...")) {
			@Override
			protected void onClick() {
				MMessage.errorDetails(this.getWindowAncestor(), throwable);
			}
		} );

		return p;
	}

	public static MList<Object> createListView(final Icon icon, final Object[] list) {
		return new ListView(icon, list);
	}

	/**
	 * @since 3.0
	 */
	public static boolean customConfirm(
		final Window parent,
		final Icon icon,
		final MActionInfo okInfo,
		final MActionInfo cancelInfo,
		final String text
	) {
		return customConfirm(parent, icon, okInfo, cancelInfo, text, null);
	}

	/**
	 * @since 3.0
	 */
	public static boolean customConfirm(
		final Window parent,
		final Icon icon,
		final MActionInfo okInfo,
		final MActionInfo cancelInfo,
		final String text,
		final Object[] list
	) {
		return customConfirm(
			parent,
			icon,
			(okInfo == null) ? null : okInfo.getIcon(),
			(okInfo == null) ? null : okInfo.getText(),
			(cancelInfo == null) ? null : cancelInfo.getIcon(),
			(cancelInfo == null) ? null : cancelInfo.getText(),
			text,
			list
		);
	}

	/**
	 * Displays an error message box.
	 * @param owner the owner window
	 * @param text A message text
	 */
	public static void error(final Window owner, final String text) {
		MPanel view = createView(text);
		MMessage<MPanel> base = new MMessage<>(
			owner,
			null,
			_("Error"),
			getRandomErrorIconName(),
			view,
			MDialog.SIMPLE_DIALOG
		);
		base.setResizable(false);
		base.exec();
	}
	
	public static void error(final Window parent, final Throwable throwable) {
		error(parent, throwable, null);
	}
	
	public static void error(final Window parent, final Throwable throwable, final Object info) {
		if (throwable != null)
			MLogger.exception(throwable);

		// passive message

		if (throwable instanceof PassiveException) {
			MStatusBar.error(getThrowablePassiveMessage(throwable, info));

			return;
		}

		// popup message

		String text = (throwable == null) ? _("Unknown Error") : TK.buildErrorMessage(throwable);

		if (info != null) {
			String infoString = info.toString();
			text += "\n\n" + infoString;

			//!!!document, remove double <html>?
			if (infoString.startsWith("<html>") && !text.startsWith("<html>"))
				text = UI.makeHTML(text);
		}
		
		MPanel view = createView(text);
		MMessage<MPanel> base = new MMessage<MPanel>(
			parent,
			null,
			_("Error"),
			getRandomErrorIconName(),
			view,
			MDialog.SIMPLE_DIALOG | MDialog.USER_BUTTON
		) {
			@Override
			protected void onUserClick() {
				errorDetails(this, throwable);
			}
		};
		if (throwable == null)
			base.getUserButton().setVisible(false);
		else
			base.changeButton(base.getUserButton(), _("Details"));
		base.setResizable(false);
		base.exec(base.getOKButton());
	}
	
	public static void errorDetails(final Window parent, final Throwable throwable) {
		errorDetails(parent, throwable, true);
	}

	/**
	 * @since 4.2
	 */
	public static void errorDetails(final Window parent, final Throwable throwable, final boolean showSystemSummary) {
		if (throwable == null)
			return;
		
		final MEditorPane stackText = new MEditorPane();
		stackText.setEditable(false);
		stackText.setFont(UI.createMonospacedFont(UI.getDefaultFontSize() + 1));
		
// TODO: 2.0: save to file as .txt ([ui/save] Save As...)
		MDialog dialog = new MDialog(parent, _("Error Details"), "ui/error", MDialog.SIMPLE_DIALOG | MDialog.LINK_BUTTON | MDialog.USER_BUTTON) {//!!!leak
			@Override
			protected void onUserClick() {
				MText.copyAll(stackText);
			}
		};
		dialog.changeButton(dialog.getUserButton(), MActionInfo.COPY);
		MLinkButton linkButton = dialog.getLinkButton();
		if (MApplication.getBugs() == null) {
			linkButton.setVisible(false);
		}
		else {
			linkButton.setText(_("Report Bug..."));
			linkButton.setURI(MApplication.getBugs());
		}

		LogFile logFile = null;
		String log = null;
		if (MLogger.isDeveloper() && !FS.isRestricted()) {
			logFile = MApplication.getLogFile();//!!!priv
			logFile.flush();
			try {
				log = FS.read(logFile.getFile(), "UTF8");
			}
			catch (IOException exception) { } // quiet
		}

		StringBuilder stackBuf = new StringBuilder();
		Throwable i = throwable;
		do {
			stackBuf.append(formatThrowable(i));
		} while ((i = i.getCause()) != null);
		
		if (stackBuf.length() > 0) {
			String s =
				(
					(FS.isRestricted() || !showSystemSummary)
					? ""
					: OS.getSummary(true)
				) +
				stackBuf +
				((log == null) ? "" : ("\n\n" + logFile.getFile() + ":\n\n" + log));
			stackText.setText(showSystemSummary ? s : s.trim()/* no empty lines on top */);
			stackText.setCaretPosition(0);
			dialog.addCenter(stackText);
		}
		dialog.setSize(UI.WindowSize.MEDIUM);
		dialog.exec(dialog.getOKButton());
	}
	
	/**
	 * @since 4.4
	 */
	public static MActionInfo getAction(final Window owner, final String title, final Icon icon, final List<MActionInfo> actionList) {
		final Property<MActionInfo> result = new Property<>();
	
		MButton defaultButton = null;
		MPanel view = MPanel.createGridPanel(actionList.size(), 1, 10, 10);
		
		for (final MActionInfo i : actionList) {
			MButton b = new MButton(i) {
				@Override
				protected void onClick() {
					MMessage<?> message = UI.getAncestorOfClass(MMessage.class, this);
					result.set(i);
					message.accept();
				}
			};
			b.setHorizontalAlignment(UI.LEADING);
			view.add(b);
			
			if (defaultButton == null)
				defaultButton = b;
		}
		
		MMessage<MPanel> message = new MMessage<>(
			owner,
			null, // label
			title,
			icon,
			view,
			CANCEL_BUTTON | MODAL
		);
		message.packFixed();
		
		if (message.exec(defaultButton))
			return result.get();

		return null;
	}

	/**
	 * Returns a localized "Are you sure?" text.
	 *
	 * @since 3.8
	 */
	public static String getSimpleConfirmMessage() {
		return _("Are you sure?");
	}

	/**
	 * @since 3.8.6
	 */
	public V getView() { return viewRef.get(); }

	/**
	 * Displays an information message box.
	 * @param owner the owner window
	 * @param text A message text
	 */
	public static void info(final Window owner, final String text) {
		infoWithIcon(owner, "ui/info", text);
	}

	/**
	 * Displays an information message box.
	 * @param owner the owner window
	 * @param iconName An icon name
	 * @param text A message text
	 */
	public static void infoWithIcon(final Window owner, final String iconName, final String text) {
		MPanel view = createView(text);
		MMessage<MPanel> base = new MMessage<>(
			owner,
			null,
			_("Information"),
			iconName,
			view,
			MDialog.SIMPLE_DIALOG
		);
		base.setResizable(false);
		base.exec();
	}

	/**
	 * @since 3.0
	 */
	public static boolean simpleConfirm(final Window parent) {
		return simpleConfirm(parent, MActionInfo.OK, null);
	}

	/**
	 * @since 3.0
	 */
	public static boolean simpleConfirm(final Window parent, final MActionInfo okInfo) {
		return simpleConfirm(parent, okInfo, null);
	}

	/**
	 * @since 3.0
	 */
	public static boolean simpleConfirm(final Window parent, final MActionInfo okInfo, final Object[] list) {
		return customConfirm(parent, okInfo.getIcon(), okInfo, null, getSimpleConfirmMessage(), list);
	}

	public static void warning(final Window parent, final String text) {
		MMessage<MPanel> base = new MMessage<>(
			parent,
			null,
			_("Warning"),
			"ui/warning",
			createView(text),
			MDialog.SIMPLE_DIALOG
		);
		base.setResizable(false);
		base.exec();
	}

	// private

	private static MPanel createView(String text) {//!!!leak
		if (text == null)
			text = "";
		boolean html = text.startsWith("<html>");
		boolean multiline = (text.indexOf('\n') != -1);
		final String originalText = text;
		if (multiline && html)
			text = text.replace("\n", "<br>");

		final MLabel pane = new MLabel();
		if (multiline && html)
			pane.setText(text);
		else if (multiline && !html)
			pane.setMultilineText(text);
		else
			pane.setText(text);

		final MAction copyAction = new MAction(MActionInfo.COPY) {
			@Override
			public void onAction() {
				if (!originalText.isEmpty()) {
					try {
						MClipboard.setString(originalText);
					}
					catch (ClipboardException exception) {
						MMessage.error(null, exception);
					}
				}
			}
		};
		copyAction.connect(pane, JComponent.WHEN_IN_FOCUSED_WINDOW);

		pane.addMouseListener(new MMouseAdapter() {
			@Override
			public void popupTrigger(final MouseEvent e) {
				MMenu menu = new MMenu();
				menu.add(copyAction);
				menu.showPopup(e);
			}
		} );

		MPanel view = MPanel.createBorderPanel(MPanel.DEFAULT_CONTENT_MARGIN);
		view.setContentMargin();
		view.addCenter(pane);
		
		MTip tip = new MTip(MMessage.class.getName(), "message");
		tip.showNextTip(MTip.Visible.SELDOM);
		view.addSouth(tip);

		return view;
	}
	
	private static boolean customConfirm(
		final Window parent,
		final Icon icon,
		final Icon okIcon,
		final String okText,
		final Icon cancelIcon,
		final String cancelText,
		final String text,
		final Object[] list
	) {
		boolean hideIcon = false;
		boolean resizable = false;
		MPanel mainPanel = MPanel.createBorderPanel(5);
		mainPanel.addNorth(createView(text));
		if (!TK.isEmpty(list) && (list.length < 1000)) {
			mainPanel.addCenter(createListView(icon, list));
			hideIcon = true;
			resizable = true;
		}
		
		MMessage<MPanel> base = new MMessage<>(
			parent,
			null,
			_("Confirm"),
			hideIcon ? null : icon,
			mainPanel,
			MDialog.STANDARD_DIALOG
		);

		// setup "Cancel" button
		MButton button = base.getCancelButton();
		if (UI.buttonIcons.get() && (cancelIcon != null))
			button.setIcon(cancelIcon);
		if (cancelText != null)
			button.setText(cancelText);

		// setup "OK" button
		button = base.getOKButton();
		if (UI.buttonIcons.get() && (okIcon != null))
			button.setIcon(okIcon);
		if (okText != null)
			button.setText(okText);
		
		// update layout
		if ((cancelIcon != null) || (cancelText != null) || (okIcon != null) || (okText != null))
			base.pack();

		base.setResizable(resizable);

		return base.exec();
	}

	private static String formatThrowable(final Throwable throwable) {
		if (throwable == null)
			return "";
		
		StackTraceElement[] stack = throwable.getStackTrace();
		
		if (TK.isEmpty(stack))
			return "\n\n" + throwable;
		
		return "\n\n" + throwable + "\n\n" + TK.toString(stack, "\n");
	}

	private static String getThrowablePassiveMessage(final Throwable throwable, final Object info) {
		if (throwable == null)
			return _("Unknown Error");

		String text = throwable.getLocalizedMessage();
		if (text == null) {
			Throwable cause = throwable.getCause();
			text = (cause == null) ? _("Unknown Error") : cause.getLocalizedMessage();
		}

		if (info != null)
			text += " - " + info;

		return text.replace('\n', ' ');
	}

	private void init(final String aLabel, final V view) {
		viewRef = new WeakReference<>(view);

		MPanel mainPanel = MPanel.createVBoxPanel();

		// init label
		MLabel label = null;
		if (aLabel != null) {
			label = new MLabel(aLabel);
			mainPanel.add(label);
			mainPanel.addGap();
		}

		// init view
		if (view != null) {
			mainPanel.add(view);
			if (label != null)
				label.setLabelFor(MWrapperPanel.getWrappedView(view));
		}

		addCenter(mainPanel);
		pack();
	}

	private static String makeInfo(final Object info) {
		return
			(info == null)
			? ""
			: "<br><br><b>" + TK.escapeXML(info.toString()) + "</b>";
	}

	// package

	@edu.umd.cs.findbugs.annotation.SuppressWarnings("SACM_STATIC_ARRAY_CREATED_IN_METHOD")
	static String getRandomErrorIconName() {
		String[] icons = {
			"ui/error",
			"labels/emotion/angry",
			"labels/emotion/cry",
			"labels/emotion/sad"
		};

		return icons[new Random().nextInt(icons.length)];
	}

	// private classes
	
	private static final class FileInfoPanel extends MPanel {
		
		// private
		
		private final File file;
		private final PreviewPanel preview;
		
		// private
		
		private FileInfoPanel(final File file, final String title) {
			super(UI.VERTICAL);
			setContentMargin();
			this.file = file;
			preview = new PreviewPanel(PreviewPanel.SMALL_WIDTH);
			preview.setMaximumHeight(PreviewPanel.SMALL_WIDTH);
			
			addSeparator(title);
			
			addInfo(_("Name:"), file.getPath());
			addInfo(_("Modified:"), new MDate(file.lastModified()).fancyFormat(MDate.FANCY_FORMAT_APPEND_TIME));
			addInfo(_("Size:"), MFormat.toAutoSize(file.length()) + " (" + file.length() + ")");
			addContentGap();
			add(preview);
		}
		
		private void addInfo(final String label, final String info) {
			MTextLabel infoComponent = new MTextLabel(info);
			addGap();
			add(MPanel.createHLabelPanel(infoComponent, label));
		}

		private void startPreview() {
			preview.update(file);
		}

		private void stopPreview() {
			preview.cancel(true);
		}
		
	}
	
	private static final class ListView extends MList<Object> {
		
		// private
		
		private Watermark watermark;
		
		// public
		
		public ListView(final Icon icon, final Object[] data) {
			Image image = (icon instanceof ImageIcon) ? ImageIcon.class.cast(icon).getImage() : null;
			if (image != null)
				watermark = new Watermark(UI.scaleImage(image, 64, 64, UI.Quality.HIGH), new Dimension(5, 5));

			setCellRenderer(MRenderer.getSharedInstance());
			setEnabled(false);
			setSingleSelectionMode();
			setToolTipText(_("Selected items"));
			for (Object i : data) {
				if (i != null) {
					if (i instanceof Icon) {
						addItem(i);
					}
					else {
						String s = i.toString();
						if (TK.isEmpty(s))
							addItem(" ");
						else
							addItem(TK.centerSqueeze(s, 128));
					}
				}
			}
			clearSelection();
		}
		
		@Override
		public void removeNotify() {
			super.removeNotify();
			watermark = TK.dispose(watermark);
		}
		
		// protected
		
		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			if (watermark != null)
				watermark.paint(this, g);
		}
		
	}

}
