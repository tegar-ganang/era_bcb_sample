package com.lemu.leco.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import jm.music.data.Part;
import ren.gui.components.DragSourceJPanel;
import ren.util.PO;

public class PartBlock extends DragSourceJPanel {

    private Part part;

    private ScoreBlock scoreBlock;

    private JComboBox channelSelect;

    private boolean guiDebug = System.getProperty("com.lemu.leco.GuiDebug", "true").equals("true");

    public PartBlock(ScoreBlock sb, Part p) {
        super();
        this.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.getInsets().top = 0;
        this.getInsets().bottom = 0;
        this.scoreBlock = sb;
        this.part = p;
        if (guiDebug) {
            this.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 1));
        }
        String labelString = "null part";
        if (p != null) {
            labelString = p.getTitle();
        } else {
            part = new Part(labelString);
        }
        JLabel label = new JLabel(labelString);
        label.setPreferredSize(new Dimension(80, 20));
        if (guiDebug) {
            label.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        }
        this.add(label);
        constructChannelSelect();
        this.add(channelSelect);
    }

    private void constructChannelSelect() {
        channelSelect = new JComboBox();
        for (int i = 0; i < 16; i++) {
            channelSelect.addItem("ch " + (i + 1));
        }
        channelSelect.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                part.setChannel(channelSelect.getSelectedIndex() + 1);
            }
        });
        channelSelect.setPreferredSize(new Dimension(100, 24));
        if (part.getChannel() > 16 || part.getChannel() < 1) {
            PO.p("channel out of bounds warning " + part.getChannel());
        }
        channelSelect.setSelectedIndex(part.getChannel() - 1);
    }

    public void setPart(Part part) {
        this.part = part;
    }

    public Part getPart() {
        return part;
    }

    public void setScoreBlock(ScoreBlock scoreBlock) {
        this.scoreBlock = scoreBlock;
    }

    public ScoreBlock getScoreBlock() {
        return scoreBlock;
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = (int) (scoreBlock.getPreferredSize().height * 1.0 / 17.0);
        d.width = scoreBlock.getPreferredSize().width;
        return d;
    }
}
