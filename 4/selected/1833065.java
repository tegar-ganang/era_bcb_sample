package com.peterhi.client.ui;

import java.net.PasswordAuthentication;
import java.util.List;
import javax.sound.sampled.Clip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.ToolTip;
import com.peterhi.beans.ChannelBean;
import com.peterhi.beans.PeerBean;
import com.peterhi.beans.Role;
import com.peterhi.beans.TalkState;
import com.peterhi.client.App;
import com.peterhi.client.managers.PeerManager;
import com.peterhi.client.managers.StoreAdapter;
import com.peterhi.client.managers.StoreEvent;
import com.peterhi.client.managers.StoreManager;
import com.peterhi.client.ui.constants.Images;
import com.peterhi.client.ui.constants.SoundEffects;
import com.peterhi.client.ui.constants.Strings;
import com.peterhi.client.voice.RecorderListener;
import java.util.ArrayList;

/**
 * A panel showing all class members.
 * 
 * @author YUN TAO
 * 
 */
public class ClassMemberPane extends Composite implements RecorderListener {

    private static final int HINT_NONE = 0;

    private static final int HINT_LOGIN = 1;

    private static final int HINT_CHANNEL = 2;

    private ToolTip infoTip;

    private ToolTip warnTip;

    private ToolTip errTip;

    private ToolBar meBar;

    private ToolItem meLabel;

    private ToolItem meTalk;

    private ToolItem meFavActions;

    private Table peerTable;

    private TableColumn selCol;

    private TableColumn imageCol;

    private TableColumn nameCol;

    private TableColumn volCol;

    private Image addOnImage;

    private Menu mPresence;

    private MenuItem miReady;

    private MenuItem miBeRightBack;

    private Menu mFavActions;

    private MenuItem miDisableMic;

    private MenuItem miEnableMic;

    private MenuItem miDisableText;

    private MenuItem miEnableText;

    private List<PeerDecorator> decorators = new ArrayList<PeerDecorator>();

    /**
	 * Create a new instance of {@link ClassmembersPane}.
	 * 
	 * @param parent
	 *            The parent.
	 * @param style
	 *            The style.
	 */
    public ClassMemberPane(Composite parent, int style) {
        super(parent, style);
        getShell().addControlListener(new ControlListener() {

            public void controlMoved(ControlEvent e) {
                setInfoTipVisible(false);
            }

            public void controlResized(ControlEvent e) {
                setInfoTipVisible(false);
            }
        });
        setLayout(new GridLayout());
        meBar = new ToolBar(this, SWT.NONE);
        meBar.addPaintListener(new PaintListener() {

            public void paintControl(PaintEvent e) {
                meBar_paintControl(e);
            }
        });
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = false;
        data.widthHint = -1;
        data.heightHint = 40;
        meBar.setLayoutData(data);
        meLabel = new ToolItem(meBar, SWT.DROP_DOWN);
        meLabel.setImage(Images.absent32);
        meLabel.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                meLabel_widgetSelected(e);
            }
        });
        meTalk = new ToolItem(meBar, SWT.CHECK);
        meTalk.setEnabled(false);
        meTalk.setImage(Images.talk32);
        meTalk.setToolTipText(Strings.classmembers_talk_text);
        meTalk.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                meTalk_widgetSelected(e);
            }
        });
        meFavActions = new ToolItem(meBar, SWT.DROP_DOWN);
        meFavActions.setImage(Images.favaction32);
        meFavActions.setToolTipText(Strings.classmembers_favactions_text);
        meFavActions.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                meFavActions_widgetSelected(e);
            }
        });
        peerTable = new Table(this, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.widthHint = -1;
        data.heightHint = -1;
        peerTable.setLayoutData(data);
        peerTable.setHeaderVisible(true);
        imageCol = new TableColumn(peerTable, SWT.NONE);
        imageCol.setWidth(40);
        imageCol.setAlignment(SWT.LEFT);
        selCol = new TableColumn(peerTable, SWT.NONE);
        selCol.setWidth(0);
        selCol.setAlignment(SWT.LEFT);
        nameCol = new TableColumn(peerTable, SWT.NONE);
        nameCol.setWidth(60);
        nameCol.setAlignment(SWT.LEFT);
        peerTable.setSortColumn(nameCol);
        peerTable.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem ti = peerTable.getSelection()[0];
                PeerDecorator dec = (PeerDecorator) ti.getData();
                PeerBean pbean = dec.getBean();
                ChannelBean cbean = getStore().getChannel();
                updateMenu(cbean.getRole(), pbean.getRole());
            }
        });
        volCol = new TableColumn(peerTable, SWT.NONE);
        volCol.setWidth(70);
        volCol.setAlignment(SWT.LEFT);
        volCol.setResizable(false);
        infoTip = new ToolTip(getShell(), SWT.BALLOON | SWT.ICON_INFORMATION);
        warnTip = new ToolTip(getShell(), SWT.BALLOON | SWT.ICON_WARNING);
        errTip = new ToolTip(getShell(), SWT.BALLOON | SWT.ICON_ERROR);
        mPresence = new Menu(getShell(), SWT.POP_UP);
        miReady = new MenuItem(mPresence, SWT.PUSH);
        miReady.setEnabled(false);
        miReady.setText(Strings.classmembers_available_text);
        miReady.setImage(Images.available16);
        miReady.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                miReady_widgetSelected(e);
            }
        });
        miBeRightBack = new MenuItem(mPresence, SWT.PUSH);
        miBeRightBack.setEnabled(false);
        miBeRightBack.setText(Strings.classmembers_berightback_text);
        miBeRightBack.setImage(Images.berightback16);
        miBeRightBack.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                miBeRightBack_widgetSelected(e);
            }
        });
        mFavActions = new Menu(getShell(), SWT.POP_UP);
        miDisableMic = new MenuItem(mFavActions, SWT.PUSH);
        miDisableMic.setEnabled(false);
        miDisableMic.setText(Strings.classmembers_disablemic_text);
        miDisableMic.setImage(Images.disablemic16);
        miDisableMic.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                miDisableMic_widgetSelected(e);
            }
        });
        miEnableMic = new MenuItem(mFavActions, SWT.PUSH);
        miEnableMic.setEnabled(false);
        miEnableMic.setText(Strings.classmembers_enablemic_text);
        miEnableMic.setImage(Images.enablemic16);
        miEnableMic.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                miEnableMic_widgetSelected(e);
            }
        });
        miDisableText = new MenuItem(mFavActions, SWT.PUSH);
        miDisableText.setEnabled(false);
        miDisableText.setText(Strings.classmembers_disabletext_text);
        miDisableText.setImage(Images.disabletext16);
        miDisableText.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                miSquelchText_widgetSelected(e);
            }
        });
        miEnableText = new MenuItem(mFavActions, SWT.PUSH);
        miEnableText.setEnabled(false);
        miEnableText.setText(Strings.classmembers_enabletext_text);
        miEnableText.setImage(Images.enabletext16);
        miEnableText.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                miUnsquelchText_widgetSelected(e);
            }
        });
        netSetup();
        storeSetup();
    }

    private void miSquelchText_widgetSelected(SelectionEvent e) {
        if (peerTable.getSelectionCount() <= 0) return;
        TableItem ti = (TableItem) peerTable.getSelection()[0];
        PeerDecorator dec = (PeerDecorator) ti.getData();
        dec.redraw();
        dec.getBean().setTalkState(TalkState.Off);
    }

    private void miUnsquelchText_widgetSelected(SelectionEvent e) {
        if (peerTable.getSelectionCount() <= 0) return;
        TableItem ti = (TableItem) peerTable.getSelection()[0];
        PeerDecorator dec = (PeerDecorator) ti.getData();
        dec.redraw();
        dec.getBean().setTalkState(TalkState.Off);
    }

    private void miEnableMic_widgetSelected(SelectionEvent e) {
        if (peerTable.getSelectionCount() <= 0) return;
        TableItem ti = (TableItem) peerTable.getSelection()[0];
        PeerDecorator dec = (PeerDecorator) ti.getData();
        dec.redraw();
        dec.getBean().setTalkState(TalkState.Off);
    }

    private void miDisableMic_widgetSelected(SelectionEvent e) {
        if (peerTable.getSelectionCount() <= 0) return;
        TableItem ti = (TableItem) peerTable.getSelection()[0];
        PeerDecorator dec = (PeerDecorator) ti.getData();
        dec.redraw();
        dec.getBean().setTalkState(TalkState.Off);
    }

    private void updateMenu(Role your, Role his) {
        miDisableMic.setEnabled(Role.isPrevilidged(your, his));
        miEnableMic.setEnabled(Role.isPrevilidged(your, his));
        miDisableText.setEnabled(Role.isPrevilidged(your, his));
        miEnableText.setEnabled(Role.isPrevilidged(your, his));
    }

    private void storeSetup() {
        getStore().addStoreListener(new StoreAdapter() {

            @Override
            public void onVoiceTest(StoreEvent e) {
                ClassMemberPane.this.onVoiceTestFinished(e);
            }

            @Override
            public void onTalking(StoreEvent e) {
                ClassMemberPane.this.onTalking(e);
            }

            @Override
            public void onVoiceEnabled(StoreEvent e) {
                ClassMemberPane.this.onVoiceEnabled(e);
            }
        });
    }

    private void onVoiceEnabled(StoreEvent e) {
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                if (getStore().isVoiceEnabled()) {
                    meTalk.setEnabled(true);
                } else {
                    meTalk.setSelection(false);
                    meTalk.setEnabled(false);
                }
            }
        });
    }

    private void onTalking(StoreEvent e) {
        TalkState ts = getStore().getTalkState();
        if (ts == TalkState.On) {
            getStore().getMic().addRecorderListener(this);
            getStore().getSpeaker().start();
            getStore().getMic().start();
        } else if (ts == TalkState.Off) {
            getStore().getMic().removeRecorderListener(this);
            getStore().getSpeaker().stop();
            getStore().getMic().stop();
        }
    }

    private void checkTalk() {
        boolean b = getStore().isVoicePassed() && getStore().getChannel() != null && getStore().isVoiceEnabled();
        meTalk.setEnabled(b);
    }

    private void onVoiceTestFinished(StoreEvent e) {
        if (getStore().isVoicePassed()) {
            setAddOnImage(null);
            infoTip.setText(Strings.classmembers_talk_test_pass_title);
            infoTip.setMessage(Strings.classmembers_talk_test_pass_text);
            setInfoTipVisible(true);
        } else {
            setAddOnImage(Images.disablemic16);
            errTip.setText(Strings.classmembers_talk_test_fail_title);
            errTip.setMessage(Strings.classmembers_talk_test_fail_text);
            setErrorTipVisible(true);
        }
        checkTalk();
    }

    /**
	 * Adds a peer to the pane.
	 * @param bean The {@link PeerBean} to add.
	 */
    public synchronized void add(final PeerBean bean) {
        for (PeerDecorator decorator : decorators) if (decorator.getBean().equals(bean)) return;
        PeerDecorator decorator = new PeerDecorator(bean, new TableItem(peerTable, SWT.NONE));
        decorators.add(decorator);
        getPeers().add(bean);
        imageCol.setWidth(imageCol.getWidth());
    }

    /**
	 * Gets a {@link PeerDecorator} with the specified
	 * client ID. The {@link PeerDecorator} usually
	 * contains the corresponding {@link PeerBean}.
	 * @param id The client ID.
	 * @return The {@link PeerDecorator}.
	 */
    public synchronized PeerDecorator getByID(int id) {
        for (PeerDecorator decorator : decorators) if (decorator.getBean().getID() == id) return decorator;
        return null;
    }

    /**
	 * Gets the index of a client in the list, by specifying the client ID.
	 * @param id The client ID.
	 * @return The index of the client in the list.
	 */
    public int indexOf(int id) {
        for (int i = 0; i < decorators.size(); i++) if (decorators.get(i).getBean().getID() == id) return i;
        return -1;
    }

    /**
	 * Sets whether to show/hide the error tooltip.
	 * @param b <c>true</c> to show the error tooltip,
	 * <c>false</c> to hide it.
	 */
    public void setErrorTipVisible(boolean b) {
        if (b) {
            if (errTip.isVisible()) errTip.setVisible(false);
            errTip.setAutoHide(true);
            int x = meLabel.getBounds().x + meLabel.getBounds().width / 2;
            int y = meLabel.getBounds().y + meLabel.getBounds().height / 2;
            errTip.setLocation(meBar.toDisplay(x, y));
            errTip.setVisible(true);
        } else errTip.setVisible(false);
    }

    /**
	 * Sets whether to show/hide the info tooltip.
	 * @param b <c>true</c> to show the info tooltip,
	 * <c>false</c> to hide it.
	 */
    public void setInfoTipVisible(boolean b) {
        if (infoTip == null) return;
        if (b) {
            if (infoTip.isVisible()) infoTip.setVisible(false);
            infoTip.setAutoHide(true);
            int x = meLabel.getBounds().x + meLabel.getBounds().width / 2;
            int y = meLabel.getBounds().y + meLabel.getBounds().height / 2;
            infoTip.setLocation(meBar.toDisplay(x, y));
            infoTip.setVisible(true);
        } else infoTip.setVisible(false);
    }

    /**
	 * Sets whether to show/hide to warn tooltip.
	 * @param b <c>true</c> to show the warn tooltip,
	 * <c>false</c> to hide it.
	 */
    public void setWarnTipVisible(boolean b) {
        if (b) {
            if (warnTip.isVisible()) warnTip.setVisible(false);
            warnTip.setAutoHide(false);
            int x = meLabel.getBounds().x + meLabel.getBounds().width / 2;
            int y = meLabel.getBounds().y + meLabel.getBounds().height / 2;
            warnTip.setLocation(meBar.toDisplay(x, y));
            warnTip.setVisible(true);
        } else warnTip.setVisible(false);
    }

    private void addPeer(PeerBean[] beans) {
        if (beans == null) return;
        for (PeerBean bean : beans) {
            add(bean);
            notifyEnter(bean);
        }
    }

    private void foundPeer(PeerBean[] beans) {
        if (beans == null) return;
        for (PeerBean bean : beans) add(bean);
    }

    private PeerManager getPeers() {
        return App.getApp().getManager(PeerManager.class);
    }

    private synchronized StoreManager getStore() {
        return App.getApp().getManager(StoreManager.class);
    }

    private void meBar_paintControl(PaintEvent e) {
        if (addOnImage != null) {
            Rectangle rt = meLabel.getBounds();
            e.gc.drawImage(addOnImage, rt.x + 5, rt.y + 5);
        }
    }

    private void meFavActions_widgetSelected(SelectionEvent e) {
        if (infoTip.isVisible()) setInfoTipVisible(false);
        mFavActions.setVisible(true);
    }

    private void meLabel_widgetSelected(SelectionEvent e) {
        if (infoTip.isVisible()) setInfoTipVisible(false);
        mPresence.setVisible(true);
    }

    private void meTalk_widgetSelected(SelectionEvent e) {
        updateMeBar();
    }

    public void setAddOnImage(Image image) {
        addOnImage = image;
        meBar.redraw();
    }

    private void updateMeBar() {
        if (meTalk.getSelection()) {
            setAddOnImage(Images.talk16);
            playClip(SoundEffects.talk);
        } else {
            setAddOnImage(null);
            playClip(SoundEffects.talkdone);
        }
        getStore().setTalkState(meTalk.getSelection() ? TalkState.On : TalkState.Off);
    }

    private void miBeRightBack_widgetSelected(SelectionEvent e) {
        meTalk.setSelection(false);
        meTalk.setEnabled(false);
        if (getStore().getTalkState() == TalkState.On) updateMeBar();
        addOnImage = miBeRightBack.getImage();
        meBar.redraw();
    }

    private void miReady_widgetSelected(SelectionEvent e) {
        if (getStore().isVoiceEnabled() && getStore().isVoicePassed()) meTalk.setEnabled(true);
        if (!meTalk.getSelection()) addOnImage = null;
        meBar.redraw();
    }

    private void netSetup() {
    }

    private void notifyEnter(final PeerBean bean) {
        playClip(SoundEffects.enter);
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                showEnterTip(bean);
            }
        });
    }

    private void notifyLeave(final PeerDecorator bean) {
        playClip(SoundEffects.leave);
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                showLeaveTip(bean);
            }
        });
    }

    private void playClip(Clip c) {
        if (c != null) {
            c.stop();
            c.setFramePosition(0);
            c.start();
        }
    }

    private void remove(final PeerBean ref) {
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                removeRow(ref);
            }
        });
    }

    private void removeAll() {
        for (PeerDecorator bean : decorators) bean.dispose();
        decorators.clear();
        getPeers().removeAll();
        imageCol.setWidth(imageCol.getWidth());
    }

    private void removeRow(PeerBean ref) {
        PeerDecorator bean = getByID(ref.getID());
        if (bean == null) return;
        bean.dispose();
        getPeers().remove(ref.getID());
        if (!imageCol.isDisposed()) imageCol.setWidth(imageCol.getWidth());
        decorators.remove(bean);
    }

    private void showEnterTip(PeerBean bean) {
        if (infoTip.isVisible()) {
            setInfoTipVisible(false);
            infoTip.setVisible(false);
        }
        if (!mPresence.isVisible() && !mFavActions.isVisible()) {
            infoTip.setText(Strings.classmembers_new_title);
            infoTip.setMessage(String.format(Strings.classmembers_new_text, bean.getName()));
            setInfoTipVisible(true);
        }
    }

    private void showLeaveTip(PeerDecorator decor) {
        if (infoTip.isVisible()) {
            setInfoTipVisible(false);
            infoTip.setVisible(false);
        }
        if (decor == null) return;
        if (decor.getBean() == null) return;
        if (!mPresence.isVisible() && !mFavActions.isVisible()) {
            infoTip.setText(Strings.classmembers_leave_title);
            infoTip.setMessage(String.format(Strings.classmembers_leave_text, decor.getBean().getName()));
            setInfoTipVisible(true);
        }
    }

    private void updateLocalUser() {
        int hint = HINT_NONE;
        Integer id = getStore().getID();
        PasswordAuthentication auth = getStore().getAuth();
        ChannelBean channel = getStore().getChannel();
        Role role = null;
        if (auth == null) hint = HINT_NONE; else if (channel == null) hint = HINT_LOGIN; else hint = HINT_CHANNEL;
        Image image = null;
        if (channel != null) role = channel.getRole();
        image = Util.getRoleImage(role);
        meLabel.setImage(image);
        String toolTipText;
        miReady.setEnabled(hint == HINT_CHANNEL);
        miBeRightBack.setEnabled(hint == HINT_CHANNEL);
        switch(hint) {
            case HINT_LOGIN:
                toolTipText = String.format(Strings.classmembers_tip_text_login, id, auth.getUserName());
                break;
            case HINT_CHANNEL:
                toolTipText = String.format(Strings.classmembers_tip_text_in_channel, id, auth.getUserName(), role);
                break;
            default:
                toolTipText = Strings.classmembers_tip_text_dc;
                break;
        }
        meLabel.setToolTipText(toolTipText);
        meBar.pack();
    }

    public void onData(byte[] buf, int len) {
        if (len <= 0) return;
    }
}
