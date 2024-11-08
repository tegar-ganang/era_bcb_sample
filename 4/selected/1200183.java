package org.mhpbox.infra;

import java.awt.Component;
import java.awt.Dimension;

public abstract class TVControls {

    public static final long TIMEOUT = 1500;

    protected ChannelSelector chSelector;

    public TVControls() {
    }

    public ChannelSelector getChannelSelector() {
        return this.chSelector;
    }

    public void assembly(VideoStack vdStack) {
        Dimension dimScreen = vdStack.getRootComponent().getSize();
        ChannelLabel chLabel = this.chSelector.getChannelLabel();
        Component chLabelComp = chLabel.getComponent();
        vdStack.getTVControlContainer().add(chLabelComp);
        Dimension chLabelDim = chLabelComp.getMinimumSize();
        int x = dimScreen.width - chLabelDim.width - 25;
        int y = dimScreen.height - chLabelDim.height - 10;
        chLabelComp.setBounds(x, y, chLabelDim.width, chLabelDim.height);
        chLabelComp.setVisible(false);
    }
}
