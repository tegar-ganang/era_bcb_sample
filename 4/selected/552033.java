package ch.laoe.plugin;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.ALayer;
import ch.laoe.operation.AOSegmentGenerator;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GControlTextX;
import ch.laoe.ui.GControlTextY;
import ch.laoe.ui.GCookie;
import ch.laoe.ui.GEditableFreehand;
import ch.laoe.ui.GEditableSegments;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.LWorker;
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


Class:			GPFreeGenerator
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	
plugin to generate free editable curves. freehand, segments of
0th order,  1st order polynom and cubic spline interpolation.
these curves can be made fully graphical, with the use of the
mouse. once the curve edition terminated, the curve can be
written to the actual layer and channel.

mouse action table of segment mode:
---------------------------------------------------------
				press-drag-release	click			
---------------------------------------------------------
-				move point				add point		
shift			-							remove point		
ctrl			move "locked"			add point		
---------------------------------------------------------

mouse action table of freehand mode:
---------------------------------------------------------
				press-drag-release	click			
---------------------------------------------------------
-				draw						-		
shift			remove					-		
ctrl			draw "locked"			-		
---------------------------------------------------------

History:
Date:			Description:									Autor:
12.03.01		first draft										oli4
25.07.01		add 3rd order interpolation				oli4
28.07.01		cubic spline interpolation added			oli4
14.09.01		numeric point view added					oli4
01.12.01		add envelope operation						oli4
15.12.01		GUI rearranged, add smooth freehand		oli4
16.12.01		add "locked" mode when presing ctrl		oli4
16.06.02		use channelstack as channel-chooser		oli4
17.06.02		add copy/paste									oli4

***********************************************************/
public class GPFreeGenerator extends GPluginFrame {

    public GPFreeGenerator(GPluginHandler ph) {
        super(ph);
        initGui();
        updateCookie();
    }

    public String getName() {
        return "freeGenerator";
    }

    public void start() {
        super.start();
        updateCookie();
        reloadGui();
    }

    public void reload() {
        super.reload();
        updateCookie();
        reloadGui();
    }

    protected boolean isFocussingMouseEvents() {
        return true;
    }

    private GEditableSegments segments;

    private GEditableFreehand freehand;

    private void updateCookie() {
        try {
            ALayer l = getSelectedLayer();
            for (int i = 0; i < l.getNumberOfChannels(); i++) {
                if (l.getChannel(i).getCookies().getCookie(getName()) == null) {
                    AChannel ch = l.getChannel(i);
                    ch.getCookies().setCookie(new GCookieFreeGenerator(ch), getName());
                }
            }
            segments = ((GCookieFreeGenerator) getSelectedLayer().getSelectedChannel().getCookies().getCookie(getName())).segments;
            freehand = ((GCookieFreeGenerator) getSelectedLayer().getSelectedChannel().getCookies().getCookie(getName())).freehand;
        } catch (Exception e) {
        }
    }

    private class GCookieFreeGenerator extends GCookie {

        public GCookieFreeGenerator(AChannel ch) {
            segments = new GEditableSegments();
            segments.setLockMode(true);
            segments.setCoverAllXRangeEnabled(false);
            segments.setChannel(ch);
            freehand = new GEditableFreehand();
            freehand.setChannel(ch);
        }

        public GEditableSegments segments;

        public GEditableFreehand freehand;
    }

    /**
	 *	mouse events
	 */
    public void mousePressed(MouseEvent e) {
        try {
            switch(tab.getSelectedIndex()) {
                case 0:
                    segments = ((GCookieFreeGenerator) getSelectedLayer().getChannel(e.getPoint()).getCookies().getCookie(getName())).segments;
                    segments.mousePressed(e);
                    break;
                case 1:
                    freehand = ((GCookieFreeGenerator) getSelectedLayer().getChannel(e.getPoint()).getCookies().getCookie(getName())).freehand;
                    freehand.mousePressed(e);
                    break;
            }
            reloadGui();
            repaintFocussedClipEditor();
        } catch (Exception exc) {
        }
    }

    public void mouseMoved(MouseEvent e) {
        try {
            switch(tab.getSelectedIndex()) {
                case 0:
                    segments = ((GCookieFreeGenerator) getSelectedLayer().getChannel(e.getPoint()).getCookies().getCookie(getName())).segments;
                    segments.mouseMoved(e);
                    break;
                case 1:
                    freehand = ((GCookieFreeGenerator) getSelectedLayer().getChannel(e.getPoint()).getCookies().getCookie(getName())).freehand;
                    freehand.mouseMoved(e);
                    break;
            }
            reloadGui();
            repaintFocussedClipEditor();
        } catch (Exception exc) {
        }
    }

    public void mouseDragged(MouseEvent e) {
        switch(tab.getSelectedIndex()) {
            case 0:
                segments.mouseDragged(e);
                break;
            case 1:
                freehand.mouseDragged(e);
                break;
        }
        reloadGui();
        repaintFocussedClipEditor();
    }

    public void mouseClicked(MouseEvent e) {
        switch(tab.getSelectedIndex()) {
            case 0:
                segments.mouseClicked(e);
                break;
            case 1:
                freehand.mouseClicked(e);
                break;
        }
        reloadGui();
        repaintFocussedClipEditor();
    }

    public void mouseEntered(MouseEvent e) {
        switch(tab.getSelectedIndex()) {
            case 0:
                segments.mouseEntered(e);
                break;
            case 1:
                freehand.mouseEntered(e);
                break;
        }
        reloadGui();
        repaintFocussedClipEditor();
    }

    /**
	 *	graphics
	 */
    public void paintOntoClip(Graphics2D g2d, Rectangle rect) {
        try {
            ALayer l = getSelectedLayer();
            for (int i = 0; i < l.getNumberOfChannels(); i++) {
                switch(tab.getSelectedIndex()) {
                    case 0:
                        ((GCookieFreeGenerator) l.getChannel(i).getCookies().getCookie(getName())).segments.paintOntoClip(g2d, rect);
                        break;
                    case 1:
                        ((GCookieFreeGenerator) l.getChannel(i).getCookies().getCookie(getName())).freehand.paintOntoClip(g2d, rect);
                        break;
                }
            }
        } catch (Exception exc) {
        }
    }

    /**
	 *	GUI
	 */
    private JButton clear, smooth, apply, copy, paste;

    private String interpolationItem[] = { GLanguage.translate("no"), GLanguage.translate("0Order"), GLanguage.translate("1Order"), GLanguage.translate("spline") };

    private JComboBox interpolation;

    private String operationItem[] = { GLanguage.translate("replace"), GLanguage.translate("envelope") };

    private JComboBox operation;

    private JCheckBox selectionIndependent;

    private JTabbedPane tab;

    private EventDispatcher eventDispatcher;

    private void initGui() {
        JPanel p = new JPanel();
        UiCartesianLayout l = new UiCartesianLayout(p, 10, 8);
        l.setPreferredCellSize(new Dimension(25, 35));
        p.setLayout(l);
        tab = new JTabbedPane();
        JPanel p1 = new JPanel();
        UiCartesianLayout l1 = new UiCartesianLayout(p1, 10, 3);
        l1.setPreferredCellSize(new Dimension(25, 35));
        p1.setLayout(l1);
        l1.add(new JLabel(GLanguage.translate("interpolation")), 0, 0, 5, 1);
        interpolation = new JComboBox(interpolationItem);
        interpolation.setSelectedIndex(2);
        l1.add(interpolation, 5, 0, 5, 1);
        tab.add(p1, GLanguage.translate("points"));
        JPanel p2 = new JPanel();
        UiCartesianLayout l2 = new UiCartesianLayout(p2, 10, 3);
        l2.setPreferredCellSize(new Dimension(25, 35));
        p2.setLayout(l2);
        smooth = new JButton(GLanguage.translate("smooth"));
        l2.add(smooth, 3, 1, 4, 1);
        tab.add(p2, GLanguage.translate("freeHand"));
        l.add(tab, 0, 0, 10, 4);
        l.add(new JLabel(GLanguage.translate("operation")), 0, 5, 4, 1);
        operation = new JComboBox(operationItem);
        operation.setSelectedIndex(0);
        l.add(operation, 4, 5, 6, 1);
        selectionIndependent = new JCheckBox(GLanguage.translate("selectionIndependent"));
        selectionIndependent.setSelected(true);
        l.add(selectionIndependent, 0, 4, 8, 1);
        clear = new JButton(GLanguage.translate("clear"));
        l.add(clear, 1, 6, 4, 1);
        apply = new JButton(GLanguage.translate("apply"));
        l.add(apply, 5, 6, 4, 1);
        copy = new JButton(GLanguage.translate("copy"));
        l.add(copy, 1, 7, 4, 1);
        paste = new JButton(GLanguage.translate("paste"));
        l.add(paste, 5, 7, 4, 1);
        frame.getContentPane().add(p);
        pack();
        eventDispatcher = new EventDispatcher();
        clear.addActionListener(eventDispatcher);
        smooth.addActionListener(eventDispatcher);
        apply.addActionListener(new LWorker(eventDispatcher));
        copy.addActionListener(eventDispatcher);
        paste.addActionListener(eventDispatcher);
        tab.addChangeListener(eventDispatcher);
        interpolation.addActionListener(eventDispatcher);
        onInterpolationChange();
    }

    private void reloadGui() {
        {
            switch(segments.getSegmentMode()) {
                case GEditableSegments.SINGLE_POINTS:
                    interpolation.setSelectedIndex(0);
                    break;
                case GEditableSegments.ORDER_0:
                    interpolation.setSelectedIndex(1);
                    break;
                case GEditableSegments.ORDER_1:
                    interpolation.setSelectedIndex(2);
                    break;
                case GEditableSegments.SPLINE:
                    interpolation.setSelectedIndex(3);
                    break;
            }
        }
    }

    private class EventDispatcher implements ActionListener, ChangeListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == clear) {
                Debug.println(1, "plugin " + getName() + " [clear] clicked");
                onClear();
                repaintFocussedClipEditor();
            } else if (e.getSource() == smooth) {
                Debug.println(1, "plugin " + getName() + " [smooth] clicked");
                onSmooth();
                repaintFocussedClipEditor();
            } else if (e.getSource() == apply) {
                LProgressViewer.getInstance().entrySubProgress(getName());
                LProgressViewer.getInstance().entrySubProgress(0.7);
                Debug.println(1, "plugin " + getName() + " [apply] clicked");
                onApply();
                updateHistory(GLanguage.translate(getName()));
                LProgressViewer.getInstance().exitSubProgress();
                LProgressViewer.getInstance().exitSubProgress();
                reloadFocussedClipEditor();
                autoCloseNow();
            } else if (e.getSource() == copy) {
                Debug.println(1, "plugin " + getName() + " [copy] clicked");
                onCopy();
                repaintFocussedClipEditor();
            } else if (e.getSource() == paste) {
                Debug.println(1, "plugin " + getName() + " [paste] clicked");
                onPaste();
                repaintFocussedClipEditor();
            } else if (e.getSource() == interpolation) {
                Debug.println(1, "plugin " + getName() + " [interpolation] clicked");
                onInterpolationChange();
                repaintFocussedClipEditor();
            }
        }

        public void stateChanged(ChangeEvent e) {
            Debug.println(1, "plugin " + getName() + " [tab] clicked");
            onInterpolationChange();
        }
    }

    private void onClear() {
        updateCookie();
        switch(tab.getSelectedIndex()) {
            case 0:
                segments.clear();
                break;
            case 1:
                freehand.clear();
                break;
        }
    }

    private void onSmooth() {
        updateCookie();
        freehand.smooth();
    }

    private void onApply() {
        ALayer l = getSelectedLayer();
        for (int i = 0; i < l.getNumberOfChannels(); i++) {
            GEditableSegments s = ((GCookieFreeGenerator) l.getChannel(i).getCookies().getCookie(getName())).segments;
            GEditableFreehand f = ((GCookieFreeGenerator) l.getChannel(i).getCookies().getCookie(getName())).freehand;
            int o = 0;
            switch(operation.getSelectedIndex()) {
                case 0:
                    o = AOSegmentGenerator.REPLACE_OPERATION;
                    break;
                case 1:
                    o = AOSegmentGenerator.ENVELOPE_OPERATION;
                    break;
            }
            switch(tab.getSelectedIndex()) {
                case 0:
                    if (selectionIndependent.isSelected()) {
                        s.convertToSamples(o);
                    } else {
                        s.convertToSelectedSamples(o);
                    }
                    break;
                case 1:
                    if (selectionIndependent.isSelected()) {
                        f.convertToSamples(o);
                    } else {
                        f.convertToSelectedSamples(o);
                    }
                    break;
            }
        }
    }

    private void onInterpolationChange() {
        ALayer l = getSelectedLayer();
        try {
            for (int i = 0; i < l.getNumberOfChannels(); i++) {
                GEditableSegments s = ((GCookieFreeGenerator) l.getChannel(i).getCookies().getCookie(getName())).segments;
                switch(tab.getSelectedIndex()) {
                    case 0:
                        switch(interpolation.getSelectedIndex()) {
                            case 0:
                                s.setSegmentMode(GEditableSegments.SINGLE_POINTS);
                                break;
                            case 1:
                                s.setSegmentMode(GEditableSegments.ORDER_0);
                                break;
                            case 2:
                                s.setSegmentMode(GEditableSegments.ORDER_1);
                                break;
                            case 3:
                                s.setSegmentMode(GEditableSegments.SPLINE);
                                break;
                        }
                        break;
                    case 1:
                        break;
                }
            }
        } catch (Exception e) {
        }
    }

    private GEditableSegments segmentsClipBoard;

    private GEditableFreehand freehandClipBoard;

    private void onCopy() {
        updateCookie();
        segmentsClipBoard = new GEditableSegments(segments);
        freehandClipBoard = new GEditableFreehand(freehand);
        repaintFocussedClipEditor();
    }

    private void onPaste() {
        AChannel ch = getFocussedClip().getSelectedLayer().getSelectedChannel();
        segments = new GEditableSegments(segmentsClipBoard);
        segments.setChannel(ch);
        ((GCookieFreeGenerator) getSelectedLayer().getSelectedChannel().getCookies().getCookie(getName())).segments = segments;
        freehand = new GEditableFreehand(freehandClipBoard);
        freehand.setChannel(ch);
        ((GCookieFreeGenerator) getSelectedLayer().getSelectedChannel().getCookies().getCookie(getName())).freehand = freehand;
        repaintFocussedClipEditor();
    }
}
