package de.genodeftest.k8055_old.gui.perspectives;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import de.genodeftest.k8055_old.IOChannels;
import de.genodeftest.k8055_old.IOEvent;
import de.genodeftest.k8055_old.IOListener;
import de.genodeftest.k8055_old.K8055Channel;
import de.genodeftest.k8055_old.IOEvent.AnalogAllEvent;
import de.genodeftest.k8055_old.JWrapperK8055.AnalogInput;
import de.genodeftest.k8055_old.gui.AbstractSimplePerspective;
import de.genodeftest.k8055_old.gui.JK8055GUI;
import de.genodeftest.swing.JCopyableLabel;

public class AnalogInput_Simple extends AbstractSimplePerspective {

    private static final long serialVersionUID = 7593609244648594672L;

    private final JProgressBar view1, view2;

    private final JCopyableLabel valueLabel1, valueLabel2;

    private final IOListener ioListener;

    private long lastRepaint = 0L;

    public AnalogInput_Simple() {
        super("Analog Input - 2 Eing�nge");
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        view1 = new JProgressBar(SwingConstants.HORIZONTAL, AnalogInput.MIN_VALUE, AnalogInput.MAX_VALUE);
        view2 = new JProgressBar(SwingConstants.HORIZONTAL, AnalogInput.MIN_VALUE, AnalogInput.MAX_VALUE);
        valueLabel1 = new JCopyableLabel(AnalogInput.toRepresentingString(AnalogInput.MIN_VALUE), 15);
        valueLabel2 = new JCopyableLabel(AnalogInput.toRepresentingString(AnalogInput.MIN_VALUE), 15);
        this.add(Box.createHorizontalStrut(3));
        this.add(new JCopyableLabel("A In 1: "));
        this.add(view1);
        this.add(Box.createHorizontalStrut(3));
        this.add(valueLabel1);
        this.add(Box.createHorizontalStrut(8));
        {
            JSeparator j = new JSeparator(SwingConstants.VERTICAL);
            j.setMaximumSize(new Dimension(4, Integer.MAX_VALUE));
            j.setMinimumSize(new Dimension(4, Integer.MIN_VALUE));
            this.add(j);
        }
        this.add(Box.createHorizontalStrut(8));
        this.add(new JCopyableLabel("A In 2: "));
        this.add(view2);
        this.add(Box.createHorizontalStrut(3));
        this.add(valueLabel2);
        this.add(Box.createHorizontalGlue());
        ioListener = new IOListener() {

            @Override
            public IOChannels getDataType() {
                return IOChannels.ANALOG;
            }

            @Override
            public Component getTargetComponent() {
                return AnalogInput_Simple.this;
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
    }

    @Override
    protected void processEvent(AWTEvent evt) {
        if (evt.getID() == IOEvent.ID_ANALOG_ALL) {
            if (System.currentTimeMillis() - lastRepaint < 50) return;
            AnalogAllEvent e = (AnalogAllEvent) evt;
            if (view1.getValue() != e.values[0]) {
                view1.setValue(e.values[0]);
                valueLabel1.setText(AnalogInput.toRepresentingString(e.values[0]));
            }
            if (view2.getValue() != e.values[1]) {
                view2.setValue(e.values[1]);
                valueLabel2.setText(AnalogInput.toRepresentingString(e.values[1]));
            }
            lastRepaint = System.currentTimeMillis();
        } else super.processEvent(evt);
    }

    @Override
    public void setPerspectiveEnabled(boolean flag) {
        if (flag) JK8055GUI.getDataAdapter().addDataListener(ioListener); else JK8055GUI.getDataAdapter().removeDataListener(ioListener);
        for (Component c : this.getComponents()) {
            c.setEnabled(flag);
        }
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
        return "Analog Input with 2 Channels / 8Bit";
    }
}
