package ch.laoe.plugin;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import ch.laoe.audio.Audio;
import ch.laoe.audio.AudioException;
import ch.laoe.audio.AudioListener;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AChannelPlotter;
import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.AClip;
import ch.laoe.clip.ALayer;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GClipPanel;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.GToolkit;
import ch.oli4.persistence.Persistence;
import ch.oli4.ui.UiCartesianLayout;
import ch.oli4.ui.control.UiControlEvent;
import ch.oli4.ui.control.UiControlListener;
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


Class:			GPPlayLoopRec
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	plugin to play/loop/record the clip.  

History:
Date:			Description:									Autor:
31.08.00		erster Entwurf									oli4
28.03.01		limit loop-pointers on reload				oli4
01.06.01		loop-pointer settable						oli4
01.06.01		add button-signaling							oli4
26.12.01		play/loop pointers always painted		oli4
20.03.02		loop checkbox introduced					oli4

***********************************************************/
public class GPPlayLoopRec extends GPluginFrame {

    public GPPlayLoopRec(GPluginHandler ph) {
        super(ph);
        initCursors();
        initGui();
    }

    public void onBackup(Persistence p) {
        super.onBackup(p);
        p.setInt("plugin." + getName() + ".playBlockSize", Audio.getPlayBlockSize());
        p.setInt("plugin." + getName() + ".captureBlockSize", Audio.getCaptureBlockSize());
        p.setInt("plugin." + getName() + ".playPointerAdjustment", Audio.getPlayPointerAdjustment());
        p.setInt("plugin." + getName() + ".capturePointerAdjustment", Audio.getCapturePointerAdjustment());
    }

    public String getName() {
        return "playLoopRec";
    }

    public JMenuItem createMenuItem() {
        return super.createMenuItem(KeyEvent.VK_P);
    }

    public void reload() {
        super.reload();
        try {
            playBlockSize.setData(Audio.getPlayBlockSize());
            captureBlockSize.setData(Audio.getCaptureBlockSize());
            playPointerAdjustment.setData(Audio.getPlayPointerAdjustment());
            capturePointerAdjustment.setData(Audio.getCapturePointerAdjustment());
            Audio a = getFocussedClip().getAudio();
            loop.setSelected(a.isLooping());
            loop.setEnabled(!a.isAutoGrowing());
            autoGrow.setSelected(a.isAutoGrowing());
            sampleRate.setData(getFocussedClip().getSampleRate());
            getFocussedClip().getAudio().limitLoopPointers();
            getFocussedClip().getAudio().setAudioListener(eventDispatcher);
            updateButtons();
        } catch (NullPointerException e) {
        }
    }

    protected boolean isFocussingMouseEvents() {
        return true;
    }

    private int selectedPointer;

    private static final int NO_POINTER = 0;

    private static final int LOOP_START_POINTER = 1;

    private static final int LOOP_END_POINTER = 2;

    private Cursor defaultCursor, placeCursor, scratchCursor;

    private void initCursors() {
        defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        placeCursor = new Cursor(Cursor.HAND_CURSOR);
        scratchCursor = createCustomCursor("scratchPlayCursor");
    }

    public void mouseMoved(MouseEvent e) {
        AClip c = ((GClipPanel) e.getSource()).getClip();
        int x = e.getPoint().x;
        final int D_MAX = 5;
        int dStart = Math.abs(c.getAudio().getPlotter().getXLoopStartPointer() - x);
        int dEnd = Math.abs(c.getAudio().getPlotter().getXLoopEndPointer() - x);
        selectedPointer = NO_POINTER;
        if (dEnd < D_MAX) {
            selectedPointer = LOOP_END_POINTER;
            ((Component) e.getSource()).setCursor(placeCursor);
        } else if (dStart < D_MAX) {
            selectedPointer = LOOP_START_POINTER;
            ((Component) e.getSource()).setCursor(placeCursor);
        } else {
            ((Component) e.getSource()).setCursor(defaultCursor);
        }
        if (GToolkit.isShiftKey(e)) {
            ((Component) e.getSource()).setCursor(scratchCursor);
            ALayer l = getFocussedClip().getSelectedLayer();
            int i = l.getPlotter().getInsideChannelIndex(e.getPoint());
            if (i >= 0) {
                AChannel ch = l.getChannel(i);
                AChannelPlotter cp = ch.getPlotter();
                int xx = (int) cp.graphToSampleX(e.getPoint().x);
                pluginHandler.getFocussedClip().getAudio().scratch(xx);
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (selectedPointer != NO_POINTER) {
            AClip c = ((GClipPanel) e.getSource()).getClip();
            int x = (int) c.getSelectedLayer().getChannel(0).getPlotter().graphToSampleX(e.getPoint().x);
            switch(selectedPointer) {
                case LOOP_START_POINTER:
                    c.getAudio().setLoopStartPointer(x);
                    break;
                case LOOP_END_POINTER:
                    c.getAudio().setLoopEndPointer(x);
                    break;
            }
            repaintFocussedClipEditor();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        int m = e.getWheelRotation();
        final int step = (int) (getFocussedClip().getSampleRate() * 1);
        getFocussedClip().getAudio().scratch(getFocussedClip().getAudio().getPlayPointer() + (m > 0 ? step : (-step)));
    }

    private JButton stopButton;

    private JButton pauseButton;

    private JButton rewButton;

    private JButton playButton;

    private JButton forwButton;

    private JButton recButton;

    private JCheckBox loop, autoGrow;

    private ImageIcon pausePassive, pauseActive;

    private ImageIcon playPassive, playActive;

    private ImageIcon recPassive, recActive;

    private UiControlText sampleRate, playBlockSize, captureBlockSize, playPointerAdjustment, capturePointerAdjustment;

    private JComboBox loopPointerSettings;

    private EventDispatcher eventDispatcher;

    private class EventDispatcher implements ActionListener, UiControlListener, AudioListener {

        public void actionPerformed(ActionEvent e) {
            getFocussedClip().getAudio().setAudioListener(eventDispatcher);
            try {
                if (e.getSource() == stopButton) {
                    Debug.println(1, "plugin " + getName() + " [stop] clicked");
                    getFocussedClip().getAudio().stop();
                } else if (e.getSource() == pauseButton) {
                    Debug.println(1, "plugin " + getName() + " [pause] clicked");
                    getFocussedClip().getAudio().pause();
                } else if (e.getSource() == rewButton) {
                    Debug.println(1, "plugin " + getName() + " [rew] clicked");
                    getFocussedClip().getAudio().rewind();
                } else if (e.getSource() == playButton) {
                    try {
                        Debug.println(1, "plugin " + getName() + " [play] clicked");
                        getFocussedClip().getAudio().play();
                    } catch (AudioException ae) {
                        showErrorDialog("audioError", ae.getMessage());
                    }
                } else if (e.getSource() == forwButton) {
                    Debug.println(1, "plugin " + getName() + " [forwind] clicked");
                    getFocussedClip().getAudio().forwind();
                } else if (e.getSource() == recButton) {
                    try {
                        Debug.println(1, "plugin " + getName() + " [rec] clicked");
                        getFocussedClip().getAudio().rec();
                    } catch (AudioException ae) {
                        showErrorDialog("audioError", ae.getMessage());
                    }
                } else if (e.getSource() == loop) {
                    Debug.println(1, "plugin " + getName() + " [loop] clicked");
                    getFocussedClip().getAudio().setLooping(loop.isSelected());
                } else if (e.getSource() == autoGrow) {
                    Debug.println(1, "plugin " + getName() + " [autoGrow] clicked");
                    getFocussedClip().getAudio().setAutoGrowing(autoGrow.isSelected());
                    reload();
                } else if (e.getSource() == loopPointerSettings) {
                    Debug.println(1, "plugin " + getName() + " [loop pointer settings] clicked");
                    AClip c = getFocussedClip();
                    Audio a = c.getAudio();
                    AChannelPlotter p = c.getSelectedLayer().getChannel(0).getPlotter();
                    AChannelSelection s = c.getSelectedLayer().getChannel(0).getSelection();
                    switch(loopPointerSettings.getSelectedIndex()) {
                        case 0:
                            a.setLoopStartPointer(0);
                            a.setLoopEndPointer(c.getMaxSampleLength());
                            break;
                        case 1:
                            a.setLoopStartPointer((int) p.getXOffset());
                            a.setLoopEndPointer((int) (p.getXOffset() + p.getXLength()));
                            break;
                        case 2:
                            a.setLoopStartPointer((int) s.getOffset());
                            a.setLoopEndPointer((int) (s.getOffset() + s.getLength()));
                            break;
                        case 3:
                            a.setLoopStartPointer((int) GPMeasure.getLowerCursor());
                            a.setLoopEndPointer((int) GPMeasure.getHigherCursor());
                            break;
                    }
                    repaintFocussedClipEditor();
                }
            } catch (NullPointerException npe) {
            }
            updateButtons();
        }

        public void onDataChanging(UiControlEvent e) {
        }

        public void onDataChanged(UiControlEvent e) {
            if (e.getSource() == sampleRate) {
                Debug.println(1, "plugin " + getName() + " [samplerate] changed");
                getFocussedClip().setSampleRate((float) sampleRate.getData());
            } else if (e.getSource() == playBlockSize) {
                Audio.setPlayBlockSize((int) playBlockSize.getData());
            } else if (e.getSource() == captureBlockSize) {
                Audio.setCaptureBlockSize((int) captureBlockSize.getData());
            } else if (e.getSource() == playPointerAdjustment) {
                Audio.setPlayPointerAdjustment((int) playPointerAdjustment.getData());
            } else if (e.getSource() == capturePointerAdjustment) {
                Audio.setCapturePointerAdjustment((int) capturePointerAdjustment.getData());
            }
        }

        public void onValidate(UiControlEvent e) {
        }

        private boolean recording = false;

        public void onStateChange(int state) {
            updateButtons();
            switch(state) {
                case Audio.PLAY:
                    recording = false;
                    break;
                case Audio.PAUSE:
                    if (recording) {
                        reloadFocussedClipEditor();
                        updateHistory(GLanguage.translate(getName()));
                        Debug.println(1, "on state change: pause");
                    }
                    recording = false;
                    break;
                case Audio.REC:
                    recording = true;
                    break;
                default:
                    if (recording) {
                        autoScaleFocussedClip();
                        getFocussedClip().getAudio().setLoopEndPointer(getFocussedClip().getMaxSampleLength());
                        reloadFocussedClipEditor();
                        updateHistory(GLanguage.translate(getName()));
                        Debug.println(1, "on state change: others");
                    }
                    recording = false;
                    break;
            }
        }
    }

    private void updateButtons() {
        switch(getFocussedClip().getAudio().getState()) {
            case Audio.PLAY:
                pauseButton.setIcon(pausePassive);
                playButton.setIcon(playActive);
                recButton.setIcon(recPassive);
                break;
            case Audio.PAUSE:
                pauseButton.setIcon(pauseActive);
                playButton.setIcon(playPassive);
                recButton.setIcon(recPassive);
                break;
            case Audio.REC:
                pauseButton.setIcon(pausePassive);
                playButton.setIcon(playPassive);
                recButton.setIcon(recActive);
                break;
            default:
                pauseButton.setIcon(pausePassive);
                playButton.setIcon(playPassive);
                recButton.setIcon(recPassive);
                break;
        }
    }

    private void initGui() {
        JTabbedPane tab = new JTabbedPane();
        pauseActive = loadIcon("pauseActive");
        pausePassive = loadIcon("pausePassive");
        playActive = loadIcon("playActive");
        playPassive = loadIcon("playPassive");
        recActive = loadIcon("recActive");
        recPassive = loadIcon("recPassive");
        stopButton = new JButton(loadIcon("stop"));
        stopButton.setToolTipText(GLanguage.translate("stop"));
        stopButton.setPreferredSize(new Dimension(26, 26));
        pauseButton = new JButton(pausePassive);
        pauseButton.setToolTipText(GLanguage.translate("pause"));
        pauseButton.setPreferredSize(new Dimension(26, 26));
        rewButton = new JButton(loadIcon("rewind"));
        rewButton.setToolTipText(GLanguage.translate("rewind"));
        rewButton.setPreferredSize(new Dimension(26, 26));
        playButton = new JButton(playPassive);
        playButton.setToolTipText(GLanguage.translate("play"));
        playButton.setPreferredSize(new Dimension(26, 26));
        forwButton = new JButton(loadIcon("forwind"));
        forwButton.setToolTipText(GLanguage.translate("forwind"));
        forwButton.setPreferredSize(new Dimension(26, 26));
        recButton = new JButton(recPassive);
        recButton.setToolTipText(GLanguage.translate("record"));
        recButton.setPreferredSize(new Dimension(26, 26));
        loop = new JCheckBox(GLanguage.translate("loop"));
        autoGrow = new JCheckBox(GLanguage.translate("autoGrow"));
        JPanel pControl = new JPanel();
        UiCartesianLayout lControl = new UiCartesianLayout(pControl, 14, 2);
        lControl.setPreferredCellSize(new Dimension(35, 35));
        pControl.setLayout(lControl);
        pControl.add(stopButton, new Rectangle(0, 0, 1, 1));
        pControl.add(pauseButton, new Rectangle(1, 0, 1, 1));
        pControl.add(rewButton, new Rectangle(3, 0, 1, 1));
        pControl.add(playButton, new Rectangle(4, 0, 1, 1));
        pControl.add(forwButton, new Rectangle(5, 0, 1, 1));
        pControl.add(recButton, new Rectangle(7, 0, 1, 1));
        pControl.add(loop, new Rectangle(8, 0, 2, 1));
        pControl.add(autoGrow, new Rectangle(10, 0, 3, 1));
        pControl.add(new JLabel(GLanguage.translate("sampleRate")), new Rectangle(0, 1, 3, 1));
        sampleRate = new UiControlText(15, true, false);
        sampleRate.setDataRange(100, 48000);
        pControl.add(sampleRate, new Rectangle(3, 1, 4, 1));
        pControl.add(new JLabel(GLanguage.translate("loopPoints")), new Rectangle(7, 1, 3, 1));
        String loopPointerItems[] = { GLanguage.translate("wholeClip"), GLanguage.translate("zoomedRange"), GLanguage.translate("selection"), GLanguage.translate("measurePoints") };
        loopPointerSettings = new JComboBox(loopPointerItems);
        pControl.add(loopPointerSettings, new Rectangle(10, 1, 4, 1));
        tab.add(GLanguage.translate("control"), pControl);
        JPanel pConfig = new JPanel();
        UiCartesianLayout lConfig = new UiCartesianLayout(pConfig, 12, 2);
        lConfig.setPreferredCellSize(new Dimension(35, 35));
        pConfig.setLayout(lConfig);
        pConfig.add(new JLabel(GLanguage.translate("playBlockSize")), new Rectangle(0, 0, 3, 1));
        playBlockSize = new UiControlText(15, true, false);
        playBlockSize.setDataRange(100, 10000);
        pConfig.add(playBlockSize, new Rectangle(3, 0, 3, 1));
        pConfig.add(new JLabel(GLanguage.translate("captureBlockSize")), new Rectangle(0, 1, 3, 1));
        captureBlockSize = new UiControlText(15, true, false);
        captureBlockSize.setDataRange(100, 10000);
        pConfig.add(captureBlockSize, new Rectangle(3, 1, 3, 1));
        pConfig.add(new JLabel(GLanguage.translate("playPointerAdjustment")), new Rectangle(6, 0, 3, 1));
        playPointerAdjustment = new UiControlText(15, true, false);
        playPointerAdjustment.setDataRange(-100000, 100000);
        pConfig.add(playPointerAdjustment, new Rectangle(9, 0, 3, 1));
        pConfig.add(new JLabel(GLanguage.translate("capturePointerAdjustment")), new Rectangle(6, 1, 3, 1));
        capturePointerAdjustment = new UiControlText(15, true, false);
        capturePointerAdjustment.setDataRange(-100000, 100000);
        pConfig.add(capturePointerAdjustment, new Rectangle(9, 1, 3, 1));
        Audio.setPlayBlockSize((int) persistance.getInt("plugin." + getName() + ".playBlockSize", 3000));
        Audio.setCaptureBlockSize((int) persistance.getInt("plugin." + getName() + ".captureBlockSize", 3000));
        Audio.setPlayPointerAdjustment((int) persistance.getInt("plugin." + getName() + ".playPointerAdjustment", -30000));
        Audio.setCapturePointerAdjustment((int) persistance.getInt("plugin." + getName() + ".capturePointerAdjustment", -3000));
        tab.add(GLanguage.translate("configure"), pConfig);
        frame.getContentPane().add(tab);
        pack();
        eventDispatcher = new EventDispatcher();
        stopButton.addActionListener(eventDispatcher);
        pauseButton.addActionListener(eventDispatcher);
        rewButton.addActionListener(eventDispatcher);
        playButton.addActionListener(eventDispatcher);
        forwButton.addActionListener(eventDispatcher);
        recButton.addActionListener(eventDispatcher);
        loop.addActionListener(eventDispatcher);
        autoGrow.addActionListener(eventDispatcher);
        sampleRate.addControlListener(eventDispatcher);
        playBlockSize.addControlListener(eventDispatcher);
        captureBlockSize.addControlListener(eventDispatcher);
        playPointerAdjustment.addControlListener(eventDispatcher);
        capturePointerAdjustment.addControlListener(eventDispatcher);
        loopPointerSettings.addActionListener(eventDispatcher);
    }
}
