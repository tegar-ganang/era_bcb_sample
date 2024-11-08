package com.peterhi.client;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TrayItem;
import com.peterhi.beans.ChannelBean;
import com.peterhi.client.managers.PeerManager;
import com.peterhi.client.managers.StoreManager;
import com.peterhi.client.ui.AboutDialog;
import com.peterhi.client.ui.AbstractWindow;
import com.peterhi.client.ui.ChannelDialog;
import com.peterhi.client.ui.ClassMemberPane;
import com.peterhi.client.ui.LoginDialog;
import com.peterhi.client.ui.TestVoiceDialog;
import com.peterhi.client.ui.ViewSide;
import com.peterhi.client.ui.ViewSpecification;
import com.peterhi.client.ui.constants.Images;
import com.peterhi.client.ui.constants.Strings;
import com.peterhi.client.views.ActionView;
import com.peterhi.client.views.ClassMemberView;
import com.peterhi.client.views.ConsoleView;
import com.peterhi.client.views.ExplorerView;
import com.peterhi.client.views.PropertyView;
import com.peterhi.client.views.WhiteboardView;
import com.peterhi.net.messages.LogoutMessage;

/**
 * Default application window implementation.
 * 
 * @author YUN TAO HAI
 * 
 */
final class Window extends AbstractWindow {

    private MenuItem mFileBean;

    private Menu mFile;

    private MenuItem miLogin;

    private MenuItem miLogout;

    private MenuItem miChannels;

    private MenuItem miLeaveChannel;

    private MenuItem miExit;

    private MenuItem mToolBean;

    private Menu mTool;

    private MenuItem miTestVoice;

    private MenuItem mHelpBean;

    private Menu mHelp;

    private MenuItem miHelp;

    private MenuItem miAbout;

    Window() {
        super();
        shell.setImage(Images.peterhi16);
    }

    @Override
    protected void initViews() {
        ViewSpecification spec;
        spec = new ViewSpecification();
        spec.setViewClass(PropertyView.class);
        spec.setViewSide(ViewSide.BottomRight);
        spec.setText(Strings.window_view_properties_text);
        spec.setImage(Images.properties16);
        masterPane.add(spec);
        spec = new ViewSpecification();
        spec.setViewClass(WhiteboardView.class);
        spec.setViewSide(ViewSide.Center);
        spec.setText(Strings.window_view_whiteboard_text);
        spec.setImage(Images.whiteboard16);
        masterPane.add(spec);
        spec = new ViewSpecification();
        spec.setViewClass(ExplorerView.class);
        spec.setViewSide(ViewSide.UpperRight);
        spec.setText(Strings.window_view_explorer_text);
        spec.setImage(Images.explorer16);
        masterPane.add(spec);
        spec = new ViewSpecification();
        spec.setViewClass(ActionView.class);
        spec.setViewSide(ViewSide.UpperRight);
        spec.setText(Strings.window_view_tools_text);
        spec.setImage(Images.tools16);
        masterPane.add(spec);
        spec = new ViewSpecification();
        spec.setViewClass(ConsoleView.class);
        spec.setViewSide(ViewSide.BottomLeft);
        spec.setText(Strings.window_view_console_text);
        spec.setImage(Images.console16);
        masterPane.add(spec);
        spec = new ViewSpecification();
        spec.setViewClass(ClassMemberView.class);
        spec.setViewSide(ViewSide.LowerRight);
        spec.setText(Strings.window_view_classmembers_text);
        spec.setImage(Images.members16);
        masterPane.add(spec);
    }

    private void netSetup() {
    }

    @Override
    protected void initTray() {
        TrayItem ti;
        ti = new TrayItem(tray, SWT.NONE);
        ti.setImage(Images.peterhi22);
    }

    @Override
    protected void initMenus() {
        mFileBean = new MenuItem(menuBar, SWT.CASCADE);
        mFileBean.setText(Strings.window_mfile_text);
        mFileBean.setImage(Images.file22);
        mFile = new Menu(shell, SWT.DROP_DOWN);
        mFileBean.setMenu(mFile);
        miLogin = new MenuItem(mFile, SWT.PUSH);
        miLogin.setText(Strings.window_milogin_text);
        miLogin.setImage(Images.login16);
        miLogin.setAccelerator(SWT.CTRL + 'L');
        miLogin.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                LoginDialog dialog = App.getApp().getDialog(LoginDialog.class);
                dialog.setVisible(true);
            }
        });
        miLogout = new MenuItem(mFile, SWT.PUSH);
        miLogout.setEnabled(false);
        miLogout.setText(Strings.window_milogout_text);
        miLogout.setImage(Images.logout16);
        miLogout.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                LogoutMessage message = new LogoutMessage();
                try {
                    App.getApp().getClient().sendTcp(message);
                    App.getApp().getClient().close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        miChannels = new MenuItem(mFile, SWT.PUSH);
        miChannels.setEnabled(false);
        miChannels.setText(Strings.window_michannels_text);
        miChannels.setImage(Images.channels16);
        miChannels.setAccelerator(SWT.ALT + 'C');
        miChannels.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ChannelDialog dialog = App.getApp().getDialog(ChannelDialog.class);
                dialog.setVisible(true);
            }
        });
        miLeaveChannel = new MenuItem(mFile, SWT.PUSH);
        miLeaveChannel.setEnabled(false);
        miLeaveChannel.setText(Strings.window_mileavechannel_text);
        miLeaveChannel.setImage(Images.leavechannel16);
        miLeaveChannel.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                leaveCurrentChannel();
            }
        });
        miExit = new MenuItem(mFile, SWT.PUSH);
        miExit.setText(Strings.window_miexit_text);
        miExit.setImage(Images.exit16);
        miExit.setAccelerator(SWT.ALT | SWT.F4);
        miExit.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });
        mToolBean = new MenuItem(menuBar, SWT.CASCADE);
        mToolBean.setText(Strings.window_mitools_text);
        mToolBean.setImage(Images.tools22);
        mTool = new Menu(shell, SWT.DROP_DOWN);
        mToolBean.setMenu(mTool);
        miTestVoice = new MenuItem(mTool, SWT.PUSH);
        miTestVoice.setText(Strings.window_mitestvoice_text);
        miTestVoice.setImage(Images.testvoice16);
        miTestVoice.setAccelerator(SWT.ALT + 'v');
        miTestVoice.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                TestVoiceDialog.show(shell);
                if (getStore().getChannel() == null) {
                    return;
                }
                getClassMemberPane().setAddOnImage(Images.testvoice16);
            }
        });
        mHelpBean = new MenuItem(menuBar, SWT.CASCADE);
        mHelpBean.setText(Strings.window_mhelp_text);
        mHelpBean.setImage(Images.help22);
        mHelp = new Menu(shell, SWT.DROP_DOWN);
        mHelpBean.setMenu(mHelp);
        miHelp = new MenuItem(mHelp, SWT.PUSH);
        miHelp.setText(Strings.window_mihelp_text);
        miHelp.setImage(Images.help16);
        miAbout = new MenuItem(mHelp, SWT.PUSH);
        miAbout.setText(Strings.window_miabout_text);
        miAbout.setImage(Images.whiteboard16);
        miAbout.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                AboutDialog.show(shell);
            }
        });
        netSetup();
        update();
    }

    private void update() {
        boolean login = getStore().getID() != null;
        boolean inChannel = getStore().getChannel() != null;
        String name = "";
        if (login) {
            name = " - " + getStore().getAuth().getUserName();
        }
        String online;
        online = login ? Strings.window_online : Strings.window_offline;
        String channel = "";
        if (inChannel) {
            String channelName = getStore().getChannel().getName();
            channel = String.format(Strings.window_channel, channelName);
        }
        String fmt = Strings.window_title + " %s %s %s";
        String title = String.format(fmt, online, name, channel);
        shell.setText(title);
        miLogin.setEnabled(!login);
        miLogout.setEnabled(login);
        miChannels.setEnabled(login && !inChannel);
        miLeaveChannel.setEnabled(login && inChannel);
    }

    private void leaveCurrentChannel() {
        if (getStore().getChannel() == null) {
            throw new NullPointerException("null channel bean");
        }
        ChannelBean bean = getStore().getChannel();
    }

    private ClassMemberPane getClassMemberPane() {
        return getView(ClassMemberView.class).getClassMemberPane();
    }

    private StoreManager getStore() {
        return App.getApp().getManager(StoreManager.class);
    }

    private PeerManager getPeers() {
        return App.getApp().getManager(PeerManager.class);
    }

    public void updateAppState() {
        AppState state = App.getApp().getAppState();
        System.out.println("state change to: " + state);
        miLogin.setEnabled(state == AppState.Initial);
        miLogout.setEnabled(state == AppState.Login);
        miChannels.setEnabled(state == AppState.Login);
        miLeaveChannel.setEnabled(state == AppState.Channel);
        miTestVoice.setEnabled(state == AppState.Channel);
    }
}
