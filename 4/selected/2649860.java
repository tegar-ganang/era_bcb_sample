package com.google.code.cubeirc.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Level;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wb.swt.SWTResourceManager;
import org.pircbotx.Channel;
import org.pircbotx.User;
import com.google.code.cubeirc.base.BaseTab;
import com.google.code.cubeirc.common.ApplicationInfo;
import com.google.code.cubeirc.common.Utils;
import com.google.code.cubeirc.connection.Connection;
import com.google.code.cubeirc.connection.MessagesFormat;
import com.google.code.cubeirc.connection.data.ChannelMessageResponse;
import com.google.code.cubeirc.connection.data.ChannelOperationResponse;
import com.google.code.cubeirc.connection.data.ChannelResponse;
import com.google.code.cubeirc.connection.data.ChannelTopicResponse;
import com.google.code.cubeirc.connection.data.GenericChannelResponse;
import com.google.code.cubeirc.connection.data.GenericUserResponse;
import com.google.code.cubeirc.connection.data.PrivateMessageResponse;
import com.google.code.cubeirc.dialogs.ChannelModeForm;
import com.google.code.cubeirc.dialogs.ColorsChooserAdapter;
import com.google.code.cubeirc.editor.EditorManager;
import com.google.code.cubeirc.queue.MessageQueue;
import com.google.code.cubeirc.queue.MessageQueueEnum;
import com.google.code.cubeirc.queue.MessageQueueEvent;
import com.google.code.cubeirc.scripting.ScriptManager;
import com.google.code.cubeirc.ui.adapters.ChannelPopmenuListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;

public class ChannelForm extends BaseTab {

    @Getter
    @Setter
    private Channel channel;

    private StyledText stTopic;

    @Getter
    @Setter
    private Tree tUser;

    private Sash separator;

    private StyledText stOutput;

    private StyledText stInput;

    @Getter
    @Setter
    private EditorManager emOutput;

    @Getter
    @Setter
    private EditorManager emInput;

    @Getter
    @Setter
    private EditorManager emTopic;

    @Setter
    @Getter
    private Text tSearch;

    private Button btnSearch;

    private Label lblTotaUsers;

    public ChannelForm(Composite composite, int style, String name, Channel channel) {
        super(composite, style, name);
        setChannel(channel);
        jbInit();
    }

    private void jbInit() {
        setLayout(new FormLayout());
        this.stTopic = new StyledText(this, SWT.BORDER);
        FormData fd_stTopic = new FormData();
        fd_stTopic.top = new FormAttachment(0);
        fd_stTopic.left = new FormAttachment(0);
        fd_stTopic.bottom = new FormAttachment(0, 27);
        fd_stTopic.right = new FormAttachment(100);
        this.stTopic.setLayoutData(fd_stTopic);
        this.tUser = new Tree(this, SWT.BORDER);
        this.tUser.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                if (getTUser().getSelectionCount() == 1) {
                    User user = (User) getTUser().getSelection()[0].getData();
                    MessageQueue.addQueue(MessageQueueEnum.MSG_PRIVATE_OUT, new PrivateMessageResponse(Connection.getUserInfo(), user, ""));
                }
            }
        });
        this.tUser.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
        FormData fd_tUser = new FormData();
        fd_tUser.right = new FormAttachment(100);
        this.tUser.setLayoutData(fd_tUser);
        this.separator = new Sash(this, SWT.VERTICAL);
        fd_tUser.left = new FormAttachment(separator);
        FormData fd_separator = new FormData();
        fd_separator.bottom = new FormAttachment(100, 0);
        fd_separator.left = new FormAttachment(100, -120);
        fd_separator.right = new FormAttachment(100, -117);
        fd_separator.top = new FormAttachment(this.stTopic, 0);
        this.separator.setLayoutData(fd_separator);
        this.stOutput = new StyledText(this, SWT.BORDER);
        FormData fd_stOutput = new FormData();
        fd_stOutput.top = new FormAttachment(this.stTopic, 0);
        fd_stOutput.right = new FormAttachment(this.separator, 0);
        fd_stOutput.left = new FormAttachment(0);
        this.stOutput.setLayoutData(fd_stOutput);
        this.stInput = new StyledText(this, SWT.BORDER);
        this.stInput.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                if (arg0.keyCode == 13) {
                    String cmd = stInput.getText();
                    if (cmd.startsWith("/")) {
                        ScriptManager.ParseCmd(stInput.getText());
                        stInput.setText("");
                    } else {
                        MessageQueue.addQueue(MessageQueueEnum.MSG_CHANNEL_OUT, new ChannelMessageResponse(getChannel(), Connection.getUserInfo(), stInput.getText().replace("\r\n", "")));
                        getEmInput().Clear();
                    }
                }
            }
        });
        fd_stOutput.bottom = new FormAttachment(this.stInput, 0);
        FormData fd_stInput = new FormData();
        fd_stInput.right = new FormAttachment(this.separator);
        fd_stInput.bottom = new FormAttachment(100);
        fd_stInput.left = new FormAttachment(0);
        this.stInput.setLayoutData(fd_stInput);
        setEmInput(new EditorManager(ApplicationInfo.EDT_CHANNEL_INPUT_NAME, stInput));
        setEmOutput(new EditorManager(ApplicationInfo.EDT_CHANNEL_OUTPUT_NAME, stOutput));
        setEmTopic(new EditorManager(ApplicationInfo.EDT_CHANNEL_TOPIC_NAME, stTopic));
        getEmTopic().setEditable(true);
        getEmInput().setEditable(true);
        getEmTopic().addPopupItem("Channel modes", "/com/google/code/cubeirc/resources/img_colors.png", new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ChannelModeForm cmf = new ChannelModeForm(getShell(), SWT.NONE, getChannel());
                cmf.open();
                super.widgetSelected(e);
            }
        });
        setTopic(new ChannelTopicResponse(getChannel(), getChannel().getTopic(), getChannel().getTopicSetter(), getChannel().getTopicTimestamp()));
        this.tSearch = new Text(this, SWT.BORDER);
        FormData fd_tSearch = new FormData();
        fd_tSearch.top = new FormAttachment(stTopic);
        fd_tSearch.left = new FormAttachment(separator);
        this.tSearch.setLayoutData(fd_tSearch);
        this.btnSearch = new Button(this, SWT.FLAT);
        fd_tSearch.bottom = new FormAttachment(btnSearch, 0, SWT.BOTTOM);
        fd_tSearch.right = new FormAttachment(btnSearch);
        this.btnSearch.setImage(SWTResourceManager.getImage(ChannelForm.class, "/com/google/code/cubeirc/resources/img_search.png"));
        FormData fd_btnSearch = new FormData();
        fd_btnSearch.top = new FormAttachment(this.stTopic, 0);
        fd_btnSearch.right = new FormAttachment(this.stTopic, 0, SWT.RIGHT);
        this.btnSearch.setLayoutData(fd_btnSearch);
        getTSearch().setBackground(getEmOutput().getEditor().getBackground());
        getTSearch().setForeground(getEmOutput().getEditor().getForeground());
        updateWho();
        this.lblTotaUsers = new Label(this, SWT.NONE);
        fd_tUser.bottom = new FormAttachment(lblTotaUsers, -4);
        lblTotaUsers.setAlignment(SWT.CENTER);
        FormData fd_lblTotaUsers = new FormData();
        fd_lblTotaUsers.left = new FormAttachment(separator);
        fd_lblTotaUsers.right = new FormAttachment(100);
        fd_lblTotaUsers.bottom = new FormAttachment(separator, 0, SWT.BOTTOM);
        this.lblTotaUsers.setLayoutData(fd_lblTotaUsers);
        this.lblTotaUsers.setText("Tota users:");
        Button btnRefresh = new Button(this, SWT.NONE);
        btnRefresh.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Connection.UpdateUsers(getChannel());
            }
        });
        btnRefresh.setImage(SWTResourceManager.getImage(ChannelForm.class, "/com/google/code/cubeirc/resources/img_refresh.png"));
        fd_tUser.top = new FormAttachment(btnRefresh, 3);
        FormData fd_btnRefresh = new FormData();
        fd_btnRefresh.left = new FormAttachment(separator);
        fd_btnRefresh.right = new FormAttachment(100);
        fd_btnRefresh.top = new FormAttachment(tSearch, 0);
        btnRefresh.setLayoutData(fd_btnRefresh);
        btnRefresh.setText("Refresh");
        this.stTopic.addKeyListener(new ColorsChooserAdapter(getEmTopic()));
        this.stInput.addKeyListener(new ColorsChooserAdapter(getEmInput()));
        this.tUser.addListener(SWT.MenuDetect, new ChannelPopmenuListener(getShell(), getTUser(), getChannel()));
    }

    @Override
    protected void checkSubclass() {
    }

    @Override
    public void actionPerformed(MessageQueueEvent e) {
        if (e.getMsgtype() == MessageQueueEnum.CHANNEL_JOIN || e.getMsgtype() == MessageQueueEnum.CHANNEL_PART) {
            ChannelResponse cr = (ChannelResponse) e.getData();
            if (getChannel().equals(cr.getChannel())) {
                String action = "";
                if (e.getMsgtype() == MessageQueueEnum.CHANNEL_JOIN) action = "join"; else action = "part";
                getEmOutput().addText(ApplicationInfo.CLS_COLOR_ACTION, String.format(MessagesFormat.MSG_CHANNEL_ACTION, cr.getUser().getNick(), cr.getUser().getLogin(), cr.getUser().getHostmask(), action, cr.getChannel().getName()));
                updateUser(action, cr.getUser());
            }
        }
        if (e.getMsgtype() == MessageQueueEnum.MSG_CHANNEL_IN || e.getMsgtype() == MessageQueueEnum.MSG_CHANNEL_OUT) {
            ChannelMessageResponse mug = (ChannelMessageResponse) e.getData();
            if (getChannel().getName().toLowerCase().equals(mug.getChannel().getName().toLowerCase())) {
                getEmOutput().addText(ApplicationInfo.CLS_COLOR_USERMSG, String.format(MessagesFormat.MSG_USER, mug.getSender().getNick(), mug.getMessage()));
            }
        }
        if (e.getMsgtype() == MessageQueueEnum.CHANNEL_NOTICE) {
            GenericChannelResponse ccr = (GenericChannelResponse) e.getData();
            if (getChannel().equals(ccr.getChannel())) getEmOutput().addText(ApplicationInfo.CLS_COLOR_NOTICE, String.format(MessagesFormat.MSG_NOTICE, ccr.getUser().getNick(), ccr.getMessage()));
        }
        if (e.getMsgtype() == MessageQueueEnum.CHANNEL_TOPIC) {
            ChannelTopicResponse ctp = (ChannelTopicResponse) e.getData();
            if (getChannel().equals(ctp.getChannel())) {
                setTopic(ctp);
            }
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_MODE) {
            GenericChannelResponse gcr = (GenericChannelResponse) e.getData();
            if (gcr.getChannel() != null) {
                if (getChannel().equals(gcr.getChannel())) {
                    getEmOutput().addText(ApplicationInfo.CLS_COLOR_MODE, String.format(MessagesFormat.MSG_MODE, gcr.getUser(), gcr.getMessage()));
                }
            }
        }
        if (e.getMsgtype() == MessageQueueEnum.USER_QUIT) {
            GenericUserResponse gur = (GenericUserResponse) e.getData();
            if (gur.getSender().getChannels().contains(getChannel())) getEmOutput().addText(ApplicationInfo.CLS_COLOR_ACTION, String.format(MessagesFormat.MSG_CHANNEL_ACTION, gur.getSender().getNick(), gur.getSender().getLogin(), gur.getSender().getHostmask(), "quit", getChannel().getName()));
        }
        if (e.getMsgtype() == MessageQueueEnum.CHANNEL_USERLIST) {
            Channel ch = (Channel) e.getData();
            if (getChannel().equals(ch)) updateWho();
        }
        if (e.getMsgtype() == MessageQueueEnum.CHANNEL_OPERATION) {
            ChannelOperationResponse cor = (ChannelOperationResponse) e.getData();
            getEmOutput().addText(ApplicationInfo.CLS_COLOR_ACTION, String.format(MessagesFormat.MSG_CHANNEL_OPERATION, cor.getSender().getNick(), cor.getType().toString().toLowerCase(), cor.getTarget().getNick()));
            updateWho();
        }
        super.actionPerformed(e);
    }

    private void setTopic(final ChannelTopicResponse ctr) {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                getEmTopic().Clear();
                getEmTopic().addText(ctr.getMessage());
                stTopic.setToolTipText(String.format("Topic set by %s @ %s", ctr.getSetby(), Utils.parseDate(ctr.getDate())));
            }
        });
    }

    private void addUsersToTable(final List<User> users, final String category, final String prefix) {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                Comparator<User> nickOrder = new Comparator<User>() {

                    @Override
                    public int compare(User u1, User u2) {
                        ;
                        return u1.getNick().compareTo(u2.getNick());
                    }
                };
                Collections.sort(users, nickOrder);
                for (User u : users) {
                    addUserToTree(category, prefix, u);
                }
            }
        });
    }

    private void addUserToTree(final String category, final String prefix, final User user) {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                TreeItem root = getTreeItem(category, getTUser().getItems());
                if (root == null) {
                    root = new TreeItem(getTUser(), SWT.NORMAL);
                    root.setText(category);
                    root.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_user_category.png"));
                }
                TreeItem ti = new TreeItem(root, SWT.NORMAL);
                ti.setText(String.format("%s%s", prefix, user.getNick()));
                ti.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_user.png"));
                ti.setData(user);
                root.setExpanded(true);
                getTUser().redraw();
            }
        });
    }

    private TreeItem getTreeItem(String name, TreeItem[] items) {
        TreeItem res = null;
        for (int i = 0; i < items.length; i++) {
            if (items[i].getText().equals(name)) {
                res = items[i];
                continue;
            }
            if (items[i].getItems() != null) {
                res = getTreeItem(name, items[i].getItems());
            }
        }
        return res;
    }

    private TreeItem getUserTree(User user, TreeItem[] items) {
        TreeItem res = null;
        for (int i = 0; i < items.length; i++) {
            if (items[i].getData() instanceof User) {
                if ((User) items[i].getData() == user) {
                    res = items[i];
                    continue;
                }
            }
            if (items[i].getItems() != null) {
                res = getUserTree(user, items[i].getItems());
            }
        }
        return res;
    }

    @Override
    public boolean Close() {
        addDebug(Level.INFO, "Quitting from channel %s", getChannel().getName());
        MessageQueue.addQueue(MessageQueueEnum.CHANNEL_USR_QUIT, getChannel());
        return super.Close();
    }

    private void updateWho() {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                getTUser().removeAll();
                getChannel().getUsers();
                int count = getChannel().getUsers().size();
                lblTotaUsers.setText(String.format("Total users: %s", count));
                addUsersToTable(new LinkedList<User>(getChannel().getOwners()), "Owners", "@");
                addUsersToTable(new LinkedList<User>(getChannel().getOps()), "Ops", "@");
                addUsersToTable(new LinkedList<User>(getChannel().getVoices()), "Voice", "+");
                addUsersToTable(new LinkedList<User>(getChannel().getNormalUsers()), "Normal", "");
            }
        });
    }

    private void updateUser(String action, User user) {
        if (action == "join") {
            LinkedList<User> ls = new LinkedList<User>();
            ls.add(user);
            addUsersToTable(ls, "Normal", "");
        } else {
            TreeItem it = getUserTree(user, getTUser().getItems());
            if (it != null) {
                it.dispose();
            }
        }
    }
}
