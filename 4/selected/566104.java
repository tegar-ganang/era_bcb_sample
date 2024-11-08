package com.google.code.cubeirc.dialogs;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.FormLayout;
import org.pircbotx.Channel;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.wb.swt.SWTResourceManager;
import com.google.code.cubeirc.common.ApplicationInfo;
import com.google.code.cubeirc.connection.Connection;
import com.google.code.cubeirc.editor.EditorManager;
import com.google.code.cubeirc.editor.TemplateManager;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Spinner;

public class ChannelModeForm extends Dialog {

    protected Object result;

    protected Shell shell;

    @Getter
    @Setter
    private Boolean isOperator;

    @Setter
    @Getter
    private Channel channel;

    private Label lblTopic;

    @Getter
    @Setter
    private EditorManager edstTopic;

    private StyledText stTopic;

    private Button btnCancel;

    private Button btnOk;

    private Label lblChannelStatus;

    private Button btnInvitei;

    private Button btnPrivatep;

    private Button btnOnlySetOp;

    private Button btnCheckButton;

    private Button btnRequireKey;

    private Text edtKey;

    private Button btnUserLimitTo;

    private Spinner sLimit;

    @Getter
    @Setter
    private boolean IstopicChange;

    public ChannelModeForm(Shell parent, int style, Channel channel) {
        super(parent, style);
        setChannel(channel);
        setText(String.format("Mode for channel %s", getChannel().getName()));
    }

    public Object open() {
        jbInit();
        this.shell.open();
        this.shell.layout();
        Display display = getParent().getDisplay();
        while (!this.shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return this.result;
    }

    /**
	 * Create contents of the dialog.
	 */
    private void jbInit() {
        this.shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.SYSTEM_MODAL);
        this.shell.setSize(389, 277);
        this.shell.setText(getText());
        this.shell.setLayout(new FormLayout());
        this.lblTopic = new Label(this.shell, SWT.NONE);
        FormData fd_lblTopic = new FormData();
        this.lblTopic.setLayoutData(fd_lblTopic);
        this.lblTopic.setText("Topic:");
        this.stTopic = new StyledText(this.shell, SWT.BORDER | SWT.H_SCROLL | SWT.WRAP);
        fd_lblTopic.bottom = new FormAttachment(this.stTopic, -6);
        fd_lblTopic.left = new FormAttachment(this.stTopic, 0, SWT.LEFT);
        FormData fd_stTopic = new FormData();
        fd_stTopic.top = new FormAttachment(0, 45);
        fd_stTopic.left = new FormAttachment(0, 10);
        fd_stTopic.right = new FormAttachment(100, -10);
        this.stTopic.setLayoutData(fd_stTopic);
        this.stTopic.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                setIstopicChange(true);
                super.keyPressed(e);
            }
        });
        this.btnCancel = new Button(this.shell, SWT.NONE);
        this.btnCancel.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });
        this.btnCancel.setImage(SWTResourceManager.getImage(ChannelModeForm.class, "/com/google/code/cubeirc/resources/img_remove.gif"));
        FormData fd_btnCancel = new FormData();
        fd_btnCancel.left = new FormAttachment(this.lblTopic, 0, SWT.LEFT);
        this.btnCancel.setLayoutData(fd_btnCancel);
        this.btnCancel.setText("Cancel");
        this.btnOk = new Button(this.shell, SWT.NONE);
        this.btnOk.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                SetModes();
            }
        });
        this.btnOk.setImage(SWTResourceManager.getImage(ChannelModeForm.class, "/com/google/code/cubeirc/resources/img_ok.png"));
        FormData fd_btnOk = new FormData();
        fd_btnOk.top = new FormAttachment(this.btnCancel, 0, SWT.TOP);
        fd_btnOk.right = new FormAttachment(this.stTopic, 0, SWT.RIGHT);
        fd_btnOk.left = new FormAttachment(100, -80);
        this.btnOk.setLayoutData(fd_btnOk);
        this.btnOk.setText("OK");
        setEdstTopic(new EditorManager(ApplicationInfo.EDT_CHANNEL_TOPIC_NAME, stTopic));
        this.lblChannelStatus = new Label(this.shell, SWT.NONE);
        this.lblChannelStatus.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
        this.lblChannelStatus.setAlignment(SWT.CENTER);
        FormData fd_lblChannelStatus = new FormData();
        fd_lblChannelStatus.right = new FormAttachment(this.stTopic, 0, SWT.RIGHT);
        fd_lblChannelStatus.top = new FormAttachment(0, 3);
        fd_lblChannelStatus.left = new FormAttachment(0, 10);
        this.lblChannelStatus.setLayoutData(fd_lblChannelStatus);
        this.lblChannelStatus.setText("");
        this.stTopic.addKeyListener(new ColorsChooserAdapter(getEdstTopic()));
        Display display = getParent().getDisplay();
        Monitor primary = display.getPrimaryMonitor();
        Rectangle bounds = primary.getBounds();
        Rectangle rect = shell.getBounds();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;
        shell.setLocation(x, y);
        this.btnInvitei = new Button(this.shell, SWT.CHECK);
        fd_btnCancel.right = new FormAttachment(this.btnInvitei, 0, SWT.RIGHT);
        FormData fd_btnInvitei = new FormData();
        fd_btnInvitei.left = new FormAttachment(this.lblTopic, 0, SWT.LEFT);
        this.btnInvitei.setLayoutData(fd_btnInvitei);
        this.btnInvitei.setText("Invite (+i)");
        this.btnPrivatep = new Button(this.shell, SWT.CHECK);
        FormData fd_btnPrivatep = new FormData();
        fd_btnPrivatep.top = new FormAttachment(this.btnInvitei, 6);
        fd_btnPrivatep.left = new FormAttachment(this.lblTopic, 0, SWT.LEFT);
        this.btnPrivatep.setLayoutData(fd_btnPrivatep);
        this.btnPrivatep.setText("Private (+p)");
        this.btnOnlySetOp = new Button(this.shell, SWT.CHECK);
        fd_btnCancel.top = new FormAttachment(this.btnOnlySetOp, 12);
        FormData fd_btnOnlySetOp = new FormData();
        fd_btnOnlySetOp.top = new FormAttachment(this.btnPrivatep, 6);
        fd_btnOnlySetOp.left = new FormAttachment(this.lblTopic, 0, SWT.LEFT);
        this.btnOnlySetOp.setLayoutData(fd_btnOnlySetOp);
        this.btnOnlySetOp.setText("Only operators set topic (+t)");
        this.btnCheckButton = new Button(this.shell, SWT.CHECK);
        FormData fd_btnCheckButton = new FormData();
        fd_btnCheckButton.top = new FormAttachment(this.btnPrivatep, 0, SWT.TOP);
        this.btnCheckButton.setLayoutData(fd_btnCheckButton);
        this.btnCheckButton.setText("Secret (+s)");
        this.btnRequireKey = new Button(this.shell, SWT.CHECK);
        fd_btnCheckButton.left = new FormAttachment(this.btnRequireKey, 0, SWT.LEFT);
        fd_btnInvitei.top = new FormAttachment(this.btnRequireKey, 0, SWT.TOP);
        FormData fd_btnRequireKey = new FormData();
        fd_btnRequireKey.top = new FormAttachment(this.stTopic, 8);
        this.btnRequireKey.setLayoutData(fd_btnRequireKey);
        this.btnRequireKey.setText("Require key");
        this.edtKey = new Text(this.shell, SWT.BORDER);
        fd_stTopic.bottom = new FormAttachment(this.edtKey, -6);
        fd_btnRequireKey.right = new FormAttachment(this.edtKey, -9);
        FormData fd_edtKey = new FormData();
        fd_edtKey.top = new FormAttachment(0, 138);
        fd_edtKey.right = new FormAttachment(this.stTopic, 0, SWT.RIGHT);
        this.edtKey.setLayoutData(fd_edtKey);
        this.btnUserLimitTo = new Button(this.shell, SWT.CHECK);
        FormData fd_btnUserLimitTo = new FormData();
        fd_btnUserLimitTo.top = new FormAttachment(this.btnCheckButton, 8);
        fd_btnUserLimitTo.left = new FormAttachment(this.btnCheckButton, 0, SWT.LEFT);
        this.btnUserLimitTo.setLayoutData(fd_btnUserLimitTo);
        this.btnUserLimitTo.setText("User limit to:");
        this.sLimit = new Spinner(this.shell, SWT.BORDER);
        FormData fd_sLimit = new FormData();
        fd_sLimit.top = new FormAttachment(this.btnOnlySetOp, -2, SWT.TOP);
        fd_sLimit.left = new FormAttachment(this.edtKey, 0, SWT.LEFT);
        this.sLimit.setLayoutData(fd_sLimit);
        setLabelStatus();
    }

    private void setLabelStatus() {
        if (getChannel().isOp(Connection.getUserInfo()) || getChannel().isOwner(Connection.getUserInfo())) {
            lblChannelStatus.setText("You are operator, you can edit settings!");
            lblChannelStatus.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
            setIsOperator(true);
        } else {
            lblChannelStatus.setText("You are NOT operator, you can't edit settings!");
            lblChannelStatus.setBackground(SWTResourceManager.getColor(SWT.COLOR_YELLOW));
            setIsOperator(false);
        }
    }

    private void GetModes() {
    }

    private void SetModes() {
        if (getIsOperator()) {
            if (btnRequireKey.getSelection()) Connection.getIrcclient().setChannelKey(getChannel(), edtKey.getText()); else Connection.getIrcclient().removeChannelKey(getChannel(), edtKey.getText());
            if (btnInvitei.getSelection()) Connection.getIrcclient().setInviteOnly(getChannel()); else Connection.getIrcclient().removeInviteOnly(getChannel());
            if (btnPrivatep.getSelection()) Connection.getIrcclient().setSecret(getChannel()); else Connection.getIrcclient().removeSecret(getChannel());
            if (btnOnlySetOp.getSelection()) Connection.getIrcclient().setTopicProtection(getChannel()); else Connection.getIrcclient().removeTopicProtection(getChannel());
            if (btnUserLimitTo.getSelection()) Connection.getIrcclient().setChannelLimit(getChannel(), sLimit.getSelection()); else Connection.getIrcclient().removeChannelLimit(getChannel());
            if (isIstopicChange()) {
                Connection.getIrcclient().setTopic(getChannel(), TemplateManager.replace(stTopic.getText()));
            }
        }
    }
}
