package de.genodeftest.k8055_old.gui.perspectives;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.border.BevelBorder;
import de.genodeftest.k8055_old.IOChannels;
import de.genodeftest.k8055_old.IOListener;
import de.genodeftest.k8055_old.K8055Channel;
import de.genodeftest.k8055_old.IOEvent.CounterAllEvent;
import de.genodeftest.k8055_old.JWrapperK8055.Counter;
import de.genodeftest.k8055_old.gui.AbstractSimplePerspective;
import de.genodeftest.k8055_old.gui.JK8055GUI;
import de.genodeftest.swing.JCopyableLabel;

public class CounterInput_Simple extends AbstractSimplePerspective {

    private static final long serialVersionUID = 2186511433720286636L;

    private final JCopyableLabel value1, value2;

    private final IOListener ioListener;

    private long counterValue1 = 0L, counterValue2 = 0L;

    public CounterInput_Simple() {
        super("Counter - 2 Eing�nge");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Box top = new Box(BoxLayout.X_AXIS), bottom = new Box(BoxLayout.X_AXIS);
        value1 = new JCopyableLabel("CounterInput 1: 0", 15);
        value2 = new JCopyableLabel("CounterInput 2: 0", 15);
        top.add(Box.createHorizontalStrut(3));
        top.add(value1);
        top.add(Box.createHorizontalStrut(3));
        top.add(getCounterResetButton(1));
        top.add(Box.createHorizontalStrut(24));
        top.add(new JCopyableLabel("Entprellzeit :"));
        addDebounceTimeSlider(1, top);
        top.add(Box.createHorizontalGlue());
        bottom.add(Box.createHorizontalStrut(3));
        bottom.add(value2);
        bottom.add(Box.createHorizontalStrut(3));
        bottom.add(getCounterResetButton(2));
        bottom.add(Box.createHorizontalStrut(24));
        bottom.add(new JCopyableLabel("Entprellzeit :"));
        addDebounceTimeSlider(2, bottom);
        bottom.add(Box.createHorizontalGlue());
        this.add(top);
        this.add(bottom);
        ioListener = new IOListener() {

            @Override
            public boolean listenToAllChannels() {
                return true;
            }

            @Override
            public Component getTargetComponent() {
                return CounterInput_Simple.this;
            }

            @Override
            public IOChannels getDataType() {
                return IOChannels.COUNTER;
            }

            @Override
            public K8055Channel getChannel() {
                return null;
            }
        };
    }

    private JButton getCounterResetButton(final int channel) {
        JButton button = new JButton("reset");
        button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        button.setBorderPainted(true);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JK8055GUI.getDataAdapter().resetCounter(channel);
            }
        });
        return button;
    }

    private void addDebounceTimeSlider(final int channelNo, Box box) {
        final JCopyableLabel debounceTimeValue = new JCopyableLabel("", 25);
        final JSlider debounceTimeSlider = new JSlider(Counter.DEBOUNCETIME_MINVALUE, Counter.DEBOUNCETIME_MAXVALUE, 20);
        debounceTimeSlider.setMinorTickSpacing(1000);
        debounceTimeSlider.setPaintTicks(true);
        debounceTimeSlider.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent e) {
                short debounceTime = (short) debounceTimeSlider.getValue();
                JK8055GUI.getDataAdapter().setValue_CounterDebounce(channelNo, debounceTime);
                debounceTimeValue.setText(Integer.toString(debounceTime) + "ms (" + Integer.toString((((debounceTime / 100) + 5) / 10)) + "s)");
            }
        });
        box.add(debounceTimeSlider);
        box.add(debounceTimeValue);
        debounceTimeSlider.setValue(0);
    }

    @Override
    public void setPerspectiveEnabled(boolean flag) {
        if (flag) JK8055GUI.getDataAdapter().addDataListener(ioListener); else JK8055GUI.getDataAdapter().removeDataListener(ioListener);
        for (Component c : this.getComponents()) {
            c.setEnabled(flag);
        }
    }

    @Override
    protected void processEvent(AWTEvent e) {
        if (e instanceof CounterAllEvent) {
            System.out.println("IOEvent_CounterAll recieved in CounterInput_Simple : " + Arrays.toString(((CounterAllEvent) e).values));
            if (counterValue1 != ((CounterAllEvent) e).values[0]) {
                value1.setText("CounterInput 1: " + Long.toString(((CounterAllEvent) e).values[0]));
                counterValue1 = ((CounterAllEvent) e).values[0];
            }
            if (counterValue2 != ((CounterAllEvent) e).values[1]) {
                value2.setText("CounterInput 2: " + Long.toString(((CounterAllEvent) e).values[1]));
                counterValue2 = ((CounterAllEvent) e).values[1];
            }
        } else super.processEvent(e);
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
        return "Counter Input with 2 Channels";
    }
}
