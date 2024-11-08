package org.makagiga.commons;

import static org.makagiga.commons.UI._;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import org.makagiga.commons.preview.PreviewPanel;
import org.makagiga.form.Default;
import org.makagiga.form.Factory;
import org.makagiga.form.Field;
import org.makagiga.form.Form;
import org.makagiga.form.Info;

/**
 * Misc. message and input dialogs.
 *
 * @since 2.0
 */
public class MMessage extends MDialog {

    public MMessage(final Window parent, final String label, final String title, final Icon icon, final JComponent view, final int flags) {
        super(parent, title, icon, flags);
        init(label, icon, view);
    }

    public MMessage(final Window parent, final String label, final String title, final String iconName, final JComponent view, final int flags) {
        super(parent, title, iconName, flags);
        init(label, MIcon.stock(iconName), view);
    }

    /**
	 * Displays a confirmation box.
	 * @param parent A parent window
	 * @param text A message text
	 * @return @c true if @b OK pressed; otherwise @c false
	 */
    public static boolean confirm(final Window parent, final String text) {
        return customConfirm(parent, MIcon.stock("ui/question"), null, null, text);
    }

    public static boolean confirmDelete(final Window parent, final String text) {
        return confirmDelete(parent, text, null);
    }

    public static boolean confirmDelete(final Window parent, final String text, final Object info) {
        return customConfirm(parent, MIcon.stock("ui/delete"), MActionInfo.DELETE, null, UI.makeHTML(text + makeInfo(info)));
    }

    /**
	 * @since 3.0
	 */
    public static boolean confirmFileOverwrite(final Window parent, final String oldFileDisplayName, final File oldFile, final File newFile) {
        TK.checkNull(oldFile, "oldFile");
        String oldFileString = (oldFileDisplayName != null) ? _("Existing File ({0})", oldFileDisplayName) : _("Existing File");
        FileInfoPanel oldFileInfo = new FileInfoPanel(oldFile, oldFileString);
        FileInfoPanel newFileInfo = (newFile != null) ? (new FileInfoPanel(newFile, _("New File"))) : null;
        MDialog dialog = new MDialog(parent, _("Confirm"), MIcon.stock("ui/question"), MDialog.STANDARD_DIALOG);
        dialog.changeButton(dialog.getOKButton(), _("Overwrite"));
        dialog.addNorth(new MLabel(_("File already exists. Overwrite?")));
        MPanel p = MPanel.createVBoxPanel();
        p.addGap();
        p.add(oldFileInfo);
        p.addGap();
        if (newFileInfo != null) p.add(newFileInfo);
        dialog.addCenter(p);
        oldFileInfo.infoPanel.alignLabels();
        oldFileInfo.startPreview();
        if (newFileInfo != null) {
            newFileInfo.infoPanel.alignLabels();
            newFileInfo.startPreview();
        }
        dialog.pack();
        boolean result = dialog.exec();
        oldFileInfo.stopPreview();
        if (newFileInfo != null) newFileInfo.stopPreview();
        return result;
    }

    public static boolean confirmFileOverwrite(final Window parent, final String path) {
        return customConfirm(parent, null, MActionInfo.OVERWRITE, null, UI.makeHTML(_("File already exists. Overwrite?") + makeInfo(path)));
    }

    public static MList<Object> createListView(final Icon icon, final Object[] list) {
        final Property<Watermark> watermark = Property.create();
        Image image = (icon instanceof ImageIcon) ? ImageIcon.class.cast(icon).getImage() : null;
        if (image != null) watermark.set(new Watermark(image, Watermark.MEDIUM, 5));
        MList<Object> listView = new MList<Object>() {

            @Override
            public void paintComponent(final Graphics g) {
                super.paintComponent(g);
                if (!watermark.isNull()) watermark.get().paint(this, g);
            }
        };
        listView.setCellRenderer(MRenderer.getDefaultListCellRenderer());
        listView.setEnabled(false);
        listView.setSingleSelectionMode();
        listView.setToolTipText(_("Selected items"));
        for (Object i : list) {
            if (i != null) {
                if (i instanceof Icon) {
                    listView.addItem(i);
                } else {
                    String s = i.toString();
                    if (TK.isEmpty(s)) listView.addItem(" "); else listView.addItem(TK.centerSqueeze(s, 128));
                }
            }
        }
        listView.clearSelection();
        return listView;
    }

    /**
	 * @since 3.0
	 */
    public static boolean customConfirm(final Window parent, final Icon icon, final MActionInfo okInfo, final MActionInfo cancelInfo, final String text) {
        return customConfirm(parent, icon, okInfo, cancelInfo, text, null);
    }

    /**
	 * @since 3.0
	 */
    public static boolean customConfirm(final Window parent, final Icon icon, final MActionInfo okInfo, final MActionInfo cancelInfo, final String text, final Object[] list) {
        return customConfirm(parent, icon, (okInfo == null) ? null : okInfo.getIcon(), (okInfo == null) ? null : okInfo.getText(), (cancelInfo == null) ? null : cancelInfo.getIcon(), (cancelInfo == null) ? null : cancelInfo.getText(), text, list);
    }

    public static char[] enterPassword(final Window parent, final String prompt) {
        PasswordForm form = new PasswordForm();
        form.header = prompt;
        Factory.Content content = Factory.newDialog(parent, form, _("Enter Password"), MIcon.stock("ui/password"));
        content.setLabel("password", _("Password:"));
        return content.getDialog().exec() ? form.password : null;
    }

    /**
	 * Displays an error message box.
	 * @param parent A parent window
	 * @param text A message text
	 */
    public static void error(final Window parent, final String text) {
        MPanel view = createView(text);
        MMessage base = new MMessage(parent, null, _("Error"), "ui/error", view, MDialog.SIMPLE_DIALOG);
        base.setResizable(false);
        base.exec();
    }

    public static void error(final Window parent, final Throwable throwable) {
        error(parent, throwable, null);
    }

    public static void error(final Window parent, final Throwable throwable, final Object info) {
        String text;
        if (throwable == null) {
            text = _("Unknown Error");
        } else {
            text = throwable.getMessage();
            if (text == null) {
                Throwable cause = throwable.getCause();
                text = (cause == null) ? _("Unknown Error") : cause.getMessage();
            }
        }
        if (info != null) text += "\n\n" + info;
        MPanel view = createView(text);
        MMessage base = new MMessage(parent, null, _("Error"), "ui/error", view, MDialog.SIMPLE_DIALOG | MDialog.USER_BUTTON) {

            @Override
            protected void onUserClick() {
                errorDetails(parent, throwable);
            }
        };
        if (throwable == null) base.getUserButton().setVisible(false); else base.changeButton(base.getUserButton(), _("Details"));
        base.setResizable(false);
        base.exec(base.getOKButton());
        if (throwable != null) MLogger.exception(throwable);
    }

    public static void errorDetails(final Window parent, final Throwable throwable) {
        if (throwable == null) return;
        final MTextArea stackText = new MTextArea();
        stackText.setWordWrap(true);
        MDialog dialog = new MDialog(parent, _("Details"), MDialog.SIMPLE_DIALOG | MDialog.URL_BUTTON | MDialog.USER_BUTTON) {

            @Override
            protected void onUserClick() {
                MText.copyAll(stackText);
            }
        };
        dialog.changeButton(dialog.getUserButton(), MActionInfo.COPY);
        MURLButton urlButton = dialog.getURLButton();
        if (MApplication.getBugs() == null) {
            urlButton.setVisible(false);
        } else {
            urlButton.setText(_("Report Bug..."));
            urlButton.setURL(MApplication.getBugs());
        }
        LogFile logFile = null;
        String log = null;
        if (MLogger.developer.get() && !FS.isRestricted()) {
            logFile = MApplication.getLogFile();
            logFile.flush();
            try {
                log = FS.read(logFile.getFile(), "UTF8");
            } catch (IOException exception) {
            }
        }
        StackTraceElement[] stack = throwable.getStackTrace();
        if (!TK.isEmpty(stack)) {
            stackText.setText((FS.isRestricted() ? "" : OS.getSummary()) + formatThrowable(throwable) + formatThrowable(throwable.getCause()) + ((log == null) ? "" : ("\n\n" + logFile.getFile() + ":\n\n" + log)));
            stackText.scrollToTop();
            stackText.setEditable(false);
            dialog.addCenter(stackText);
        }
        dialog.setSize(640, 480);
        dialog.exec(dialog.getOKButton());
    }

    /**
	 * Displays an information message box.
	 * @param parent A parent window
	 * @param text A message text
	 */
    public static void info(final Window parent, final String text) {
        infoWithIcon(parent, "ui/info", text);
    }

    /**
	 * Displays an information message box.
	 * @param parent A parent window
	 * @param iconName An icon name
	 * @param text A message text
	 */
    public static void infoWithIcon(final Window parent, final String iconName, final String text) {
        MPanel view = createView(text);
        MMessage base = new MMessage(parent, null, _("Information"), iconName, view, MDialog.SIMPLE_DIALOG);
        base.setResizable(false);
        base.exec();
    }

    /**
	 * Displays an input box (prompt).
	 * @param parent A parent window
	 * @param initialValue An initial value
	 * @param label A label
	 * @param title A dialog title
	 * @param iconName An icon name
	 * @param autoCompletionID An auto completion ID (@c null - no text completion)
	 * @return Entered text or @c null if cancelled
	 */
    public static String input(final Window parent, final String initialValue, final String label, final String title, final String iconName, final String autoCompletionID) {
        MTextFieldPanel view = new MTextFieldPanel(initialValue);
        view.getMenuButton().setVisible(false);
        if (autoCompletionID != null) view.setAutoCompletion(autoCompletionID);
        final MMessage base = new MMessage(parent, label, title, iconName, view, MDialog.STANDARD_DIALOG);
        base.getOKButton().setEnabled(!view.isEmpty());
        base.packFixed(480);
        view.makeDefault();
        view.getTextField().getDocument().addDocumentListener(new MDocumentAdapter() {

            @Override
            protected void onChange(final DocumentEvent e) {
                base.getOKButton().setEnabled(!isDocumentEmpty(e));
            }
        });
        if (base.exec()) {
            if (autoCompletionID != null) view.saveAutoCompletion();
            return view.getText();
        }
        return null;
    }

    /**
	 * Displays an input box (prompt).
	 * @param parent A parent window
	 * @param initialValue An initial value
	 * @param label A label
	 * @param title A dialog title
	 * @return Entered text or @c null if cancelled
	 */
    public static String input(final Window parent, final String initialValue, final String label, final String title) {
        return input(parent, initialValue, label, title, null, "input");
    }

    /**
	 * @since 2.4
	 */
    public static MPasswordPanel.Info newPassword(final Window parent, final String prompt, final int flags) {
        final MPasswordPanel passwordPanel = new MPasswordPanel(flags, prompt);
        final MMessage base = new MMessage(parent, null, _("New Password"), "ui/password", passwordPanel, MDialog.STANDARD_DIALOG);
        final MPasswordField newPassword = passwordPanel.getNewPasswordField();
        newPassword.getDocument().addDocumentListener(new MDocumentAdapter() {

            @Override
            protected void onChange(final DocumentEvent e) {
                base.getOKButton().setEnabled(passwordPanel.validateNewPassword(newPassword));
            }
        });
        final MPasswordField confirmedNewPassword = passwordPanel.getConfirmedNewPasswordField();
        confirmedNewPassword.getDocument().addDocumentListener(new MDocumentAdapter() {

            @Override
            protected void onChange(final DocumentEvent e) {
                base.getOKButton().setEnabled(passwordPanel.validateNewPassword(confirmedNewPassword));
            }
        });
        base.packFixed();
        base.getOKButton().setEnabled(passwordPanel.validateNewPassword(newPassword));
        return base.exec() ? passwordPanel.getPasswordInfo() : null;
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
        return customConfirm(parent, okInfo.getIcon(), okInfo, null, _("Are you sure?"), list);
    }

    public static String text(final Window parent, final String initialValue, final String title) {
        MDialog dialog = new MDialog(parent, title, MDialog.NO_SPECIAL_EFFECTS | MDialog.STANDARD_DIALOG);
        MTextArea editor = new MTextArea(initialValue);
        dialog.addCenter(editor);
        dialog.setSize(640, 480);
        return dialog.exec() ? editor.getText() : null;
    }

    public static void warning(final Window parent, final String text) {
        MMessage base = new MMessage(parent, null, _("Warning"), "ui/warning", createView(text), MDialog.SIMPLE_DIALOG);
        base.setResizable(false);
        base.exec();
    }

    private static MPanel createView(final String text) {
        final JOptionPane pane = new JOptionPane(text, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
        final MAction copyAction = new MAction(MActionInfo.COPY) {

            @Override
            public void onAction() {
                Object message = pane.getMessage();
                if (message != null) {
                    try {
                        MClipboard.setString(message.toString());
                    } catch (ClipboardException exception) {
                        MMessage.error(null, exception);
                    }
                }
            }
        };
        copyAction.connect(pane, JOptionPane.WHEN_IN_FOCUSED_WINDOW);
        pane.addMouseListener(new MMouseAdapter() {

            @Override
            public void popupTrigger(final MouseEvent e) {
                MMenu menu = new MMenu();
                menu.add(copyAction);
                menu.showPopup(e);
            }
        });
        new ContainerScanner(pane) {

            public void processComponent(final Container parent, final Component component) {
                if (component instanceof JButton) parent.remove(component);
            }
        };
        MPanel view = MPanel.createBorderPanel(5);
        view.addCenter(pane);
        MTip tip = new MTip(MMessage.class.getName(), "message");
        tip.showNextTip(MTip.Visible.SELDOM);
        view.addSouth(tip);
        return view;
    }

    private static boolean customConfirm(final Window parent, final Icon icon, final Icon okIcon, final String okText, final Icon cancelIcon, final String cancelText, final String text, final Object[] list) {
        boolean hideIcon = false;
        boolean resizable = false;
        MPanel mainPanel = MPanel.createBorderPanel(5);
        mainPanel.addNorth(createView(text));
        if (!TK.isEmpty(list) && (list.length < 1000)) {
            mainPanel.addCenter(createListView(icon, list));
            hideIcon = true;
            resizable = true;
        }
        MMessage base = new MMessage(parent, null, _("Confirm"), hideIcon ? null : icon, mainPanel, MDialog.STANDARD_DIALOG);
        MButton button = base.getCancelButton();
        if (UI.buttonIcons.get() && (cancelIcon != null)) button.setIcon(cancelIcon);
        if (cancelText != null) button.setText(cancelText);
        button = base.getOKButton();
        if (UI.buttonIcons.get() && (okIcon != null)) button.setIcon(okIcon);
        if (okText != null) button.setText(okText);
        if ((cancelIcon != null) || (cancelText != null) || (okIcon != null) || (okText != null)) base.pack();
        base.setResizable(resizable);
        return base.exec();
    }

    private static String formatThrowable(final Throwable throwable) {
        if (throwable == null) return "";
        StackTraceElement[] stack = throwable.getStackTrace();
        if (TK.isEmpty(stack)) return "\n\n" + throwable;
        return "\n\n" + throwable + "\n\n" + TK.join(stack, "\n");
    }

    private void init(final String aLabel, final Icon icon, final JComponent view) {
        MPanel mainPanel = MPanel.createVBoxPanel();
        MLabel label = null;
        if (aLabel != null) {
            label = new MLabel(aLabel);
            mainPanel.add(label);
            mainPanel.addGap();
        }
        if ((icon != null) && !UI.specialEffects.get()) addWest(new MLabel(icon));
        if (view != null) {
            mainPanel.add(view);
            if (label != null) {
                if (view instanceof MPanel.Wrapper<?>) label.setLabelFor(MPanel.Wrapper.class.cast(view).getView()); else label.setLabelFor(view);
            }
        }
        addCenter(mainPanel);
        pack();
    }

    private static String makeInfo(final Object info) {
        return (info == null) ? "" : "<br><br><b>" + TK.escapeXML(info.toString()) + "</b>";
    }

    private static final class FileInfoPanel extends MPanel {

        private File file;

        private MPanel infoPanel;

        private PreviewPanel preview;

        private FileInfoPanel(final File file, final String title) {
            super(MDialog.CONTENT_MARGIN, 0);
            this.file = file;
            preview = new PreviewPanel(PreviewPanel.SMALL_WIDTH);
            preview.setMaximumHeight(PreviewPanel.SMALL_WIDTH);
            setTitle(title);
            infoPanel = MPanel.createVBoxPanel();
            addInfo(_("Name:"), file.getPath());
            addInfo(_("Modified:"), new MDate(file.lastModified()).fancyFormat(MDate.LONG, true));
            addInfo(_("Size:"), MFormat.toAutoSize(file.length()) + " (" + file.length() + ")");
            infoPanel.addStretch();
            addEast(preview);
            addCenter(infoPanel);
        }

        private void addInfo(final String label, final String info) {
            MTextLabel infoComponent = new MTextLabel(info);
            MPanel.Wrapper<?> p = (MPanel.Wrapper<?>) MPanel.createHLabelPanel(infoComponent, label);
            p.getLabel().setStyle("font-weight: bold");
            infoPanel.add(p);
        }

        private void startPreview() {
            preview.update(file);
        }

        private void stopPreview() {
            preview.cancel(true);
        }
    }

    @Form(pack = 320)
    private static final class PasswordForm {

        @Info
        private String header;

        @Default
        @Field(required = true)
        private char[] password;
    }
}
