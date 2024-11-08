package com.google.code.b0rx0r.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.google.code.b0rx0r.B0rx0r;
import com.google.code.b0rx0r.program.SampleDataContainer;

public class OutputRoutingMatrix extends JPanel {

    private SampleDataContainer current;

    private ActionListener listener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox cb = (JCheckBox) e.getSource();
            String name = cb.getName();
            StringTokenizer st = new StringTokenizer(name, "-", false);
            int output = Integer.parseInt(st.nextToken());
            int channel = Integer.parseInt(st.nextToken());
            if (cb.isSelected()) current.getOutputMap().addMapping(channel, output); else current.getOutputMap().removeMapping(channel, output);
        }
    };

    public SampleDataContainer getCurrent() {
        return current;
    }

    public void setCurrent(SampleDataContainer current) {
        this.current = current;
        refreshLayout();
    }

    private void refreshLayout() {
        for (Component c : getComponents()) {
            if (c instanceof JCheckBox) {
                ((JCheckBox) c).removeActionListener(listener);
            }
        }
        removeAll();
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        for (int x = 0; x < B0rx0r.NUMBER_OF_STEREO_OUTS * 2; x++) {
            gbc.gridx = x;
            add(new JLabel("" + (x + 1)), gbc);
        }
        for (int x = 0; x < B0rx0r.NUMBER_OF_STEREO_OUTS * 2; x++) {
            gbc.gridx = x;
            for (int y = 0; y < getChannelCount(); y++) {
                gbc.gridy = y + 1;
                JCheckBox cb = new JCheckBox();
                cb.setSelected(current.getOutputMap().getOutputs(y).contains(x));
                cb.setName(x + "-" + y);
                add(cb, gbc);
                cb.addActionListener(listener);
            }
        }
        invalidate();
        revalidate();
        repaint();
    }

    private int getChannelCount() {
        if (current != null) return current.getFloatSampleBuffer().getChannelCount(); else return 2;
    }
}
