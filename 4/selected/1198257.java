package wand.filterChannel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import javax.swing.*;
import wand.ChannelFrame;
import wand.genericChannel.*;

public class ChannelTransitions extends JPanel {

    public JSlider transparencySlider;

    private float alpha;

    private GenericChannel channelFocus, channelOutput;

    public ChannelTransitions() {
        channelFocus = ChannelFrame.channelGridPanel.getFocusChannel();
        channelOutput = ChannelFrame.channelGridPanel.getOutPutChannel();
        setBackground(Color.white);
        makeButtons();
        setLayout(new GridLayout(2, 1));
        add(new FaderMap());
        add(transparencySlider);
    }

    class FaderMap extends JPanel {

        public FaderMap() {
            repaint();
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            super.paintComponent(g2);
            g2.fillRect(0, 0, 20, 20);
        }
    }

    public void setChannelsForFadeTransition(GenericChannel focus, GenericChannel output) {
        channelFocus = focus;
        channelOutput = output;
    }

    private void makeButtons() {
        transparencySlider = new javax.swing.JSlider();
        transparencySlider.setMaximum(100);
        transparencySlider.setMinimum(0);
        transparencySlider.setValue(100);
        transparencySlider.setOrientation(javax.swing.JSlider.HORIZONTAL);
        transparencySlider.setToolTipText("Cross Fader");
        transparencySlider.setDoubleBuffered(true);
        transparencySlider.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                transparencySliderStateChanged(evt);
                if (transparencySlider.getValue() == transparencySlider.getMinimum() || transparencySlider.getValue() == transparencySlider.getMaximum()) transparencySlider.setBackground(Color.lightGray); else transparencySlider.setBackground(Color.yellow);
                restoreFocus();
            }
        });
    }

    private boolean sliderExtremeListening = false;

    private int sliderValue;

    private boolean minExtremeReady = true;

    private boolean maxExtremeReady = false;

    private void transparencySliderStateChanged(javax.swing.event.ChangeEvent evt) {
        sliderValue = transparencySlider.getValue();
        alpha = transparencySlider.getValue() * 1.0f / transparencySlider.getMaximum();
        if (maxExtremeReady) {
            channelOutput.getChannelDisplay().setAlpha(1 - alpha, false);
            channelOutput.getOutputDisplayPanel().setAlpha(1 - alpha, true);
            channelOutput.getOutputPreviewPanel().setAlpha(1 - alpha, true);
        }
        if (minExtremeReady) {
            channelOutput.getChannelDisplay().setAlpha(alpha, false);
            channelOutput.getOutputDisplayPanel().setAlpha(alpha, true);
            channelOutput.getOutputPreviewPanel().setAlpha(alpha, true);
        }
        if (sliderValue == 0 && minExtremeReady) {
            flipFocusAndOutputChannels();
            minExtremeReady = false;
            maxExtremeReady = true;
        }
        if (sliderValue == 100 && maxExtremeReady) {
            flipFocusAndOutputChannels();
            maxExtremeReady = false;
            minExtremeReady = true;
        }
    }

    private void flipFocusAndOutputChannels() {
        ChannelFrame.controlPanel.channelOutTriggerPanel.transparencySlider.setValue(100);
        ChannelFrame.channelGridPanel.punchFocusedChannel();
        ChannelFrame.controlPanel.channelOutTriggerPanel.transparencySlider.setValue(100);
    }

    private void restoreFocus() {
        ChannelFrame.enginePanel.requestFocus();
    }
}
