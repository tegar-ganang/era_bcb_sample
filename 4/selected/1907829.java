package ch.laoe.plugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AClip;
import ch.laoe.clip.ALayer;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GChannelViewer;
import ch.laoe.ui.GControlTextX;
import ch.laoe.ui.GDialog;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.GToolkit;
import ch.laoe.ui.Laoe;
import ch.oli4.ui.UiCartesianLayout;
import ch.oli4.ui.control.UiControlText;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			GPChannelStack
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	plugin to manage the tracks of one layer.  

History:
Date:			Description:									Autor:
22.10.00		erster Entwurf									oli4
13.09.00		add copy/paste channel							oli4
23.09.00		add dialog on new channel						oli4

***********************************************************/
public class GPChannelStack extends GPluginFrame {

    public GPChannelStack(GPluginHandler ph) {
        super(ph);
        initGui();
        frame.setResizable(true);
    }

    public String getName() {
        return "channelStack";
    }

    public void start() {
        super.start();
    }

    public void reload() {
        super.reload();
        if (frame.isVisible() && (getFocussedClip() != null)) {
            onClipChange();
        }
    }

    private JScrollPane scrollPane;

    private JPanel pScroll;

    private JButton newButton, upButton, downButton, copyButton, pasteButton, duplicateButton, deleteButton, mergeButton, mergeAllButton;

    private EventDispatcher eventDispatcher;

    private class EventDispatcher implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            try {
                LProgressViewer.getInstance().entrySubProgress(getName());
                if (e.getSource() == newButton) {
                    Debug.println(1, "plugin " + getName() + " [new] clicked");
                    onNewButton();
                } else if (e.getSource() == upButton) {
                    Debug.println(1, "plugin " + getName() + " [up] clicked");
                    onUpButton();
                } else if (e.getSource() == downButton) {
                    Debug.println(1, "plugin " + getName() + " [down] clicked");
                    onDownButton();
                } else if (e.getSource() == copyButton) {
                    Debug.println(1, "plugin " + getName() + " [copy] clicked");
                    onCopyButton();
                } else if (e.getSource() == pasteButton) {
                    Debug.println(1, "plugin " + getName() + " [paste] clicked");
                    onPasteButton();
                } else if (e.getSource() == duplicateButton) {
                    Debug.println(1, "plugin " + getName() + " [duplicate] clicked");
                    onDuplicateButton();
                } else if (e.getSource() == deleteButton) {
                    Debug.println(1, "plugin " + getName() + " [delete] clicked");
                    onDeleteButton();
                } else if (e.getSource() == mergeButton) {
                    Debug.println(1, "plugin " + getName() + " [merge] clicked");
                    onMergeButton();
                } else if (e.getSource() == mergeAllButton) {
                    Debug.println(1, "plugin " + getName() + " [merge all] clicked");
                    onMergeAllButton();
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            LProgressViewer.getInstance().exitSubProgress();
        }
    }

    private void initGui() {
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JPanel pTop = new JPanel();
        pScroll = new JPanel();
        JPanel pBottom = new JPanel();
        p.add(pTop, BorderLayout.NORTH);
        scrollPane = new JScrollPane(pScroll, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        p.add(scrollPane, BorderLayout.CENTER);
        newButton = new JButton(loadIcon("new"));
        newButton.setToolTipText(GLanguage.translate("newChannel"));
        newButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(newButton);
        upButton = new JButton(loadIcon("up"));
        upButton.setToolTipText(GLanguage.translate("moveChannelUp"));
        upButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(upButton);
        downButton = new JButton(loadIcon("down"));
        downButton.setToolTipText(GLanguage.translate("moveChannelDown"));
        downButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(downButton);
        copyButton = new JButton(loadIcon("copy"));
        copyButton.setToolTipText(GLanguage.translate("copyChannel"));
        copyButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(copyButton);
        pasteButton = new JButton(loadIcon("paste"));
        pasteButton.setToolTipText(GLanguage.translate("pasteChannel"));
        pasteButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(pasteButton);
        duplicateButton = new JButton(loadIcon("duplicate"));
        duplicateButton.setToolTipText(GLanguage.translate("duplicateChannel"));
        duplicateButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(duplicateButton);
        deleteButton = new JButton(loadIcon("delete"));
        deleteButton.setToolTipText(GLanguage.translate("deleteChannel"));
        deleteButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(deleteButton);
        mergeButton = new JButton(loadIcon("mergeUpChannel"));
        mergeButton.setToolTipText(GLanguage.translate("mergeUpChannel"));
        mergeButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(mergeButton);
        mergeAllButton = new JButton(loadIcon("mergeAllChannels"));
        mergeAllButton.setToolTipText(GLanguage.translate("mergeAllChannels"));
        mergeAllButton.setPreferredSize(new Dimension(26, 26));
        pBottom.add(mergeAllButton);
        p.add(pBottom, BorderLayout.SOUTH);
        frame.getContentPane().add(p);
        frame.setSize(new Dimension(360, 260));
        eventDispatcher = new EventDispatcher();
        newButton.addActionListener(eventDispatcher);
        upButton.addActionListener(eventDispatcher);
        downButton.addActionListener(eventDispatcher);
        copyButton.addActionListener(eventDispatcher);
        pasteButton.addActionListener(eventDispatcher);
        duplicateButton.addActionListener(eventDispatcher);
        deleteButton.addActionListener(eventDispatcher);
        mergeButton.addActionListener(eventDispatcher);
        mergeAllButton.addActionListener(eventDispatcher);
    }

    private void updateThisAndFocussedClip() {
        frame.validate();
        frame.repaint();
        repaintFocussedClipEditor();
    }

    /**
      *	new channel dialog
      */
    private UiControlText samples;

    private JPanel createNewChannelDialogContent(int s) {
        JPanel p = new JPanel();
        UiCartesianLayout l = new UiCartesianLayout(p, 10, 1);
        l.setPreferredCellSize(new Dimension(25, 35));
        p.setLayout(l);
        l.add(new JLabel(GLanguage.translate("samples")), 0, 0, 4, 1);
        samples = new GControlTextX(Laoe.getInstance(), 10, true, true);
        samples.setDataRange(1, 1e9);
        samples.setData(s);
        l.add(samples, 4, 0, 6, 1);
        return p;
    }

    private void onNewButton() {
        ALayer l = getFocussedClip().getSelectedLayer();
        if (GDialog.showCustomOkCancelDialog(frame, createNewChannelDialogContent(l.getMaxSampleLength()), GLanguage.translate("newChannelNoHtml"))) {
            l.insert(new AChannel((int) samples.getData()), l.getSelectedIndex());
            onClipChange();
            autoScaleFocussedClip();
            reloadFocussedClipEditor();
            updateHistory(GLanguage.translate("newChannel"));
        }
    }

    private void onDownButton() {
        ALayer l = getFocussedClip().getSelectedLayer();
        l.moveUp(l.getSelectedIndex());
        onClipChange();
        autoScaleFocussedClip();
        reloadFocussedClipEditor();
        updateHistory(GLanguage.translate("moveChannelDown"));
    }

    private void onUpButton() {
        ALayer l = getFocussedClip().getSelectedLayer();
        l.moveDown(l.getSelectedIndex());
        onClipChange();
        autoScaleFocussedClip();
        reloadFocussedClipEditor();
        updateHistory(GLanguage.translate("moveChannelUp"));
    }

    private static AChannel clipBoardChannel;

    private void onCopyButton() {
        ALayer l = getFocussedClip().getSelectedLayer();
        clipBoardChannel = new AChannel(l.getSelectedChannel());
    }

    private void onPasteButton() {
        if (clipBoardChannel != null) {
            ALayer l = getFocussedClip().getSelectedLayer();
            l.insert(new AChannel(clipBoardChannel), l.getSelectedIndex());
            onClipChange();
            autoScaleFocussedClip();
            reloadFocussedClipEditor();
            updateHistory(GLanguage.translate("copyChannel"));
        }
    }

    private void onDuplicateButton() {
        ALayer l = getFocussedClip().getSelectedLayer();
        l.insert(new AChannel(l.getSelectedChannel()), l.getSelectedIndex());
        onClipChange();
        autoScaleFocussedClip();
        reloadFocussedClipEditor();
        updateHistory(GLanguage.translate("duplicateChannel"));
    }

    private void onDeleteButton() {
        ALayer l = getFocussedClip().getSelectedLayer();
        l.remove(l.getSelectedIndex());
        onClipChange();
        autoScaleFocussedClip();
        reloadFocussedClipEditor();
        updateHistory(GLanguage.translate("deleteChannel"));
    }

    private void onMergeButton() {
        ALayer l = getFocussedClip().getSelectedLayer();
        l.mergeDownChannel(l.getSelectedIndex());
        onClipChange();
        autoScaleFocussedClip();
        reloadFocussedClipEditor();
        updateHistory(GLanguage.translate("mergeUpChannel"));
    }

    private void onMergeAllButton() {
        ALayer l = getFocussedClip().getSelectedLayer();
        l.mergeAllChannels();
        onClipChange();
        autoScaleFocussedClip();
        reloadFocussedClipEditor();
        updateHistory(GLanguage.translate("mergeAllChannels"));
    }

    /**
	*	loads all data from the selected clip
	*/
    private void onClipChange() {
        AClip c = getFocussedClip();
        pScroll.removeAll();
        pScroll.setPreferredSize(new Dimension(300, 60 * getFocussedClip().getSelectedLayer().getNumberOfChannels()));
        for (int i = 0; i < c.getSelectedLayer().getNumberOfChannels(); i++) {
            pScroll.add(new ChannelPanel(this, c, i));
        }
        updateThisAndFocussedClip();
    }

    /**
	*	A panel representing a channel in the channel-stack
	*/
    private class ChannelPanel extends JPanel implements ActionListener, MouseListener {

        private JCheckBox audible;

        private GChannelViewer channelView;

        private JTextField channelName;

        private boolean selected;

        private GPChannelStack channelStack;

        private ALayer layer;

        private AChannel channel;

        private int channelIndex;

        public ChannelPanel(GPChannelStack chs, AClip c, int channelIndex) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            channelStack = chs;
            layer = c.getSelectedLayer();
            this.channelIndex = channelIndex;
            channel = layer.getChannel(channelIndex);
            audible = new JCheckBox();
            audible.setSelectedIcon(GToolkit.loadIcon(this, "audibleSelectedCheckBox"));
            audible.setIcon(GToolkit.loadIcon(this, "audibleUnselectedCheckBox"));
            audible.setToolTipText(GLanguage.translate("audible"));
            audible.setSelected(channel.isAudible());
            audible.addActionListener(this);
            add(audible);
            channelView = new GChannelViewer(getFocussedClip(), c.getSelectedIndex(), channelIndex);
            channelView.setPreferredSize(new Dimension(100, 30));
            channelView.addMouseListener(this);
            add(channelView);
            channelName = new JTextField(30);
            channelName.setToolTipText(GLanguage.translate("channelName"));
            channelName.setText(channel.getName());
            channelName.addActionListener(this);
            add(channelName);
            setBorder(BorderFactory.createEtchedBorder());
            setSelected(layer.getSelected() == channel);
            addMouseListener(this);
            setPreferredSize(new Dimension(340, 40));
        }

        public void setSelected(boolean b) {
            selected = b;
            Color c = new Color(0x486c9e);
            if (selected) {
                setBackground(c);
                audible.setBackground(c);
                channelName.setBackground(c);
            }
            repaint();
        }

        public void actionPerformed(ActionEvent e) {
            try {
                if (e.getSource() == channelName) {
                    Debug.println(1, "plugin " + channelStack.getName() + " name [enter] clicked");
                    channel.setName(channelName.getText());
                } else if (e.getSource() == audible) {
                    Debug.println(1, "plugin " + channelStack.getName() + " [audible] clicked");
                    channel.setAudible(audible.isSelected());
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            Debug.println(1, "plugin " + this.getName() + " [select channel] mouse-click");
            layer.setSelectedIndex(channelIndex);
            channelStack.onClipChange();
            channelStack.reloadFocussedClipEditor();
        }
    }
}
