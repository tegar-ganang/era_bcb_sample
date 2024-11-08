package org.makagiga.fs.feeds;

import static org.makagiga.commons.UI._;
import java.awt.Dimension;
import java.awt.Window;
import java.net.URL;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import org.makagiga.commons.MActionInfo;
import org.makagiga.commons.MClipboard;
import org.makagiga.commons.TK;
import org.makagiga.commons.UI;
import org.makagiga.commons.swing.MDialog;
import org.makagiga.commons.swing.MGroupLayout;
import org.makagiga.commons.swing.MPanel;
import org.makagiga.commons.swing.MSmallButton;
import org.makagiga.commons.swing.MText;
import org.makagiga.commons.swing.MTextField;
import org.makagiga.commons.swing.event.MDocumentAdapter;
import org.makagiga.commons.validator.NotEmptyValidator;
import org.makagiga.feeds.FeedComponent;
import org.makagiga.feeds.FeedUtils;
import org.makagiga.feeds.archive.Archive;
import org.makagiga.fs.FSHelper;

/**
 * @since 2.0
 */
public final class AddFeedDialog extends MDialog {

    private boolean previewDone;

    private FeedComponent preview;

    private MainPanel mainPanel;

    private MTextField fileName;

    private String newFileName;

    private String newURL;

    public AddFeedDialog(final Window parent, final String linkOrNull, final FeedsFS fs) {
        super(parent, fs.getNewFileActionText(), "ui/feed", STANDARD_DIALOG);
        updateButtons(false);
        mainPanel = new MainPanel(false, linkOrNull);
        mainPanel.feedURLTextField.getDocument().addDocumentListener(new MDocumentAdapter<MTextField>() {

            @Override
            protected void onChange(final DocumentEvent e) {
                AddFeedDialog.this.updateButtons(false);
            }
        });
        getValidatorSupport().add(new NotEmptyValidator(mainPanel.feedURLTextField));
        MPanel previewPanel = MPanel.createBorderPanel(5);
        previewPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(_("Preview")), UI.createEmptyBorder(5)));
        preview = new FeedComponent(FeedComponent.Type.COMBO_BOX_AND_VIEWER);
        preview.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                if ((preview == null) || preview.isActive()) return;
                if (preview.isCancelled() || preview.isError()) {
                    MText.setText(fileName, _("New Feed"));
                    newURL = null;
                    updateButtons(false);
                    updateComponents(true);
                } else {
                    MText.setText(fileName, preview.getTitle());
                    FeedComponent.ChannelInfo info = preview.getChannelInfo();
                    URL source = info.getFeed().getSource();
                    newURL = Objects.toString(source, null);
                    updateButtons(true);
                    updateComponents(true);
                }
            }
        });
        preview.setArchiveFlags(Archive.NO_ARCHIVE);
        previewPanel.addCenter(preview);
        fileName = new MTextField(_("New Feed"));
        fileName.setAutoCompletion("rename");
        getValidatorSupport().add(new FSHelper.NameValidator(fileName));
        Dimension maxMainPanelSize = new Dimension(MGroupLayout.DEFAULT_SIZE, MGroupLayout.PREFERRED_SIZE);
        getMainPanel().setGroupLayout(true).beginRows().addComponent(mainPanel, null, null, maxMainPanelSize).addComponent(previewPanel).beginColumns().addComponent(fileName, _("Name:")).end().end();
        setSize(UI.WindowSize.MEDIUM);
        installValidatorMessage();
    }

    @Override
    public boolean exec() {
        return exec(mainPanel.feedURLTextField);
    }

    @Override
    protected boolean onAccept() {
        mainPanel.feedURLTextField.saveAutoCompletion();
        fileName.saveAutoCompletion();
        if (previewDone) {
            newFileName = fileName.getText();
            if (newURL == null) newURL = mainPanel.feedURLTextField.getText();
            return true;
        }
        updateComponents(false);
        preview.download(mainPanel.feedURLTextField.getText());
        return false;
    }

    @Override
    protected void onClose() {
        if (preview != null) {
            preview.cancel();
            preview = null;
        }
        mainPanel = null;
        fileName = null;
    }

    private void updateButtons(final boolean previewDone) {
        this.previewDone = previewDone;
        if (previewDone) changeButton(getOKButton(), _("Create"), "ui/ok"); else changeButton(getOKButton(), _("Preview"), "ui/next");
    }

    private void updateComponents(final boolean enabled) {
        if (getOKButton() != null) getOKButton().setEnabled(enabled);
    }

    String getNewFileName() {
        return newFileName;
    }

    String getNewURL() {
        return newURL;
    }

    public static final class MainPanel extends FeedsFSPlugin.AbstractPanel {

        private final MSmallButton pasteButton;

        MTextField feedURLTextField;

        String originalFeedURL;

        public MainPanel(final boolean showHeader, final String link) {
            super(showHeader ? _("Feed") : null, true);
            add(FeedUtils.createAddressTip());
            addContentGap();
            feedURLTextField = new MTextField();
            if (TK.isEmpty(link)) {
                URL url = MClipboard.getURL();
                if (url != null) feedURLTextField.setText(url.toString());
            } else {
                feedURLTextField.setText(link);
            }
            feedURLTextField.setAutoCompletion("feedurl");
            pasteButton = new MSmallButton(MActionInfo.PASTE) {

                @Override
                protected void onClick() {
                    feedURLTextField.clear();
                    feedURLTextField.paste();
                    feedURLTextField.setText(feedURLTextField.getText().trim());
                    feedURLTextField.makeDefault();
                }
            };
            MPanel feedURLTextFieldPanel = MPanel.createHLabelPanel(feedURLTextField, _("Address:"));
            feedURLTextFieldPanel.addEast(pasteButton);
            add(feedURLTextFieldPanel);
        }

        @Override
        public void setLocked(final boolean value) {
            super.setLocked(value);
            feedURLTextField.setEditable(!value);
            pasteButton.setEnabled(!value);
        }

        @Override
        protected boolean shouldRefresh() {
            return !feedURLTextField.getText().equals(originalFeedURL);
        }
    }
}
