package com.markpiper.tvtray.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseListener;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Timer;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkListener;
import com.markpiper.tvtray.Channel;
import com.markpiper.tvtray.NowAndNextModel;
import com.markpiper.tvtray.Programme;

/**
 * @author mark
 */
public class NowAndNextWindow extends TVTrayWindow {

    private final int WIDTH = 210;

    private final int HEIGHT = 250;

    private final int LIFETIME = 5000;

    private Timer winTimer;

    private Vector listeners;

    private JButton btn_close;

    private NowAndNextModel model;

    private InfoWindow info;

    private Vector channelLabels;

    public NowAndNextWindow(NowAndNextModel model) {
        super();
        this.model = model;
        setSize(WIDTH, HEIGHT);
        setTitle("Now & Next..." + getCurrentTime());
        JPanel progsPanel = new JPanel();
        channelLabels = getProgrammes();
        progsPanel.setLayout(new GridLayout(channelLabels.size(), 1));
        progsPanel.setBackground(Color.WHITE);
        for (Iterator i = channelLabels.iterator(); i.hasNext(); ) {
            JEditorPane lab = (JEditorPane) i.next();
            progsPanel.add(lab);
        }
        JScrollPane scrPane = new JScrollPane(progsPanel);
        scrPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        getContentPane().add(scrPane);
    }

    public void addChannelListListener(MouseListener mListener, HyperlinkListener hListener) {
        for (Iterator i = channelLabels.iterator(); i.hasNext(); ) {
            JEditorPane pane = (JEditorPane) i.next();
            pane.addMouseListener(mListener);
            pane.addHyperlinkListener(hListener);
        }
    }

    private Vector getProgrammes() {
        channelLabels = new Vector();
        Vector channels = null;
        Programme nowProg = null;
        Programme nextProg = null;
        channels = model.getChannels();
        Calendar tm = Calendar.getInstance();
        for (Iterator i = channels.iterator(); i.hasNext(); ) {
            Channel ch = (Channel) i.next();
            if (ch.isActive()) {
                nowProg = ch.getByTime(tm);
                nextProg = ch.getNext(tm, 1);
                if (nowProg != null || !model.isHidingNotOnAir()) {
                    String channelName = (ch != null ? ch.getName() : "unknown");
                    String channelAlias = (ch != null ? ch.getAlias() : "unknown");
                    String nowDesc = (nowProg != null ? nowProg.getShortHTML() : "Not on Air");
                    String nextDesc = (nextProg != null ? nextProg.getShortHTML() : "Not on Air");
                    StringBuffer html = new StringBuffer("<html");
                    html.append("<font face=\"Trebuchet MS\" size=\"2\"><b>");
                    html.append(channelAlias);
                    html.append("</b><br>");
                    html.append(nowDesc);
                    html.append("<br>");
                    html.append(nextDesc);
                    html.append("</font></html>");
                    NowAndNextLabel progLab = new NowAndNextLabel(new String(html));
                    progLab.setName("channel:" + channelName);
                    progLab.setAnchor((nowProg != null ? nowProg.getStart() : "top"));
                    channelLabels.add(progLab);
                }
            }
        }
        return channelLabels;
    }

    private String getCurrentTime() {
        String currentHour = String.valueOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        if (currentHour.length() == 1) currentHour = "0" + currentHour;
        String currentMinute = String.valueOf(Calendar.getInstance().get(Calendar.MINUTE));
        if (currentMinute.length() == 1) currentMinute = "0" + currentMinute;
        String currentTime = currentHour + ":" + currentMinute;
        return currentTime;
    }
}
