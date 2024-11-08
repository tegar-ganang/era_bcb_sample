package de.genodeftest.k8055_old.gui.perspectives;

import java.awt.AWTEvent;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import de.genodeftest.k8055_old.IOChannels;
import de.genodeftest.k8055_old.IOEvent;
import de.genodeftest.k8055_old.IOListener;
import de.genodeftest.k8055_old.JWrapperK8055;
import de.genodeftest.k8055_old.K8055Channel;
import de.genodeftest.k8055_old.IOEvent.DigitalAllEvent;
import de.genodeftest.k8055_old.JWrapperK8055.DigitalInput;
import de.genodeftest.k8055_old.gui.AbstractSimplePerspective;
import de.genodeftest.k8055_old.gui.JK8055GUI;
import de.genodeftest.swing.JCopyableLabel;

public class DigitalInput_Simple extends AbstractSimplePerspective {

    private static final long serialVersionUID = -289421580284930224L;

    private SingleChannel[] digitalInputViewLabels = new SingleChannel[5];

    private final IOListener ioListener;

    public DigitalInput_Simple() {
        super("Digital Input - 5 Eing�nge, davon 2 Counter");
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        for (int i = 0; i < 5; i++) {
            digitalInputViewLabels[i] = new SingleChannel(JWrapperK8055.DigitalInput.getChannelForIndex((short) (i + 1)), false);
        }
        ioListener = new IOListener() {

            @Override
            public IOChannels getDataType() {
                return IOChannels.DIGITAL;
            }

            @Override
            public Component getTargetComponent() {
                return DigitalInput_Simple.this;
            }

            @Override
            public boolean listenToAllChannels() {
                return true;
            }

            @Override
            public K8055Channel getChannel() {
                return null;
            }
        };
        for (int i = 0; i < 5; i++) {
            this.add(Box.createHorizontalStrut(3));
            this.add(digitalInputViewLabels[i]);
        }
        this.add(Box.createHorizontalGlue());
    }

    private class SingleChannel extends JCopyableLabel {

        private static final long serialVersionUID = 8482687553460242251L;

        private boolean currentValue;

        private SingleChannel(final DigitalInput inputChannel, final boolean isON) {
            super(12);
            refreshJPanel(isON, inputChannel.channelNo);
        }

        private void refreshJPanel(boolean value, int inputChannel) {
            currentValue = value;
            if (value) {
                this.setText("D In " + inputChannel + " : 1");
            } else {
                this.setText("D In " + inputChannel + " : _");
            }
        }

        private boolean getCurrentValue() {
            return currentValue;
        }
    }

    @Override
    public void setPerspectiveEnabled(boolean flag) {
        if (flag) JK8055GUI.getDataAdapter().addDataListener(ioListener); else JK8055GUI.getDataAdapter().removeDataListener(ioListener);
        for (Component c : this.getComponents()) {
            c.setEnabled(flag);
        }
    }

    @Override
    protected void processEvent(AWTEvent evt) {
        if (evt.getID() == IOEvent.ID_DIGITAL_ALL) {
            for (int i = 0; i < 5; i++) {
                if (digitalInputViewLabels[i].getCurrentValue() != ((DigitalAllEvent) evt).values[i]) digitalInputViewLabels[i].refreshJPanel(((DigitalAllEvent) evt).values[i], i + 1);
            }
        } else super.processEvent(evt);
    }

    @Override
    protected void finalize() {
        try {
            JK8055GUI.getDataAdapter().removeDataListener(ioListener);
        } catch (Exception e) {
        }
    }

    @Override
    public String getTitle() {
        return "Digital Input with 5 Channels";
    }
}
