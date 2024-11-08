package be.lassi.ui.main;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import be.lassi.base.BooleanHolder;
import be.lassi.base.BooleanListener;
import be.lassi.context.ShowContext;
import be.lassi.domain.PreHeating;
import be.lassi.ui.base.BasicFrame;
import be.lassi.ui.widgets.LevelSpinner;
import be.lassi.ui.widgets.SmallButton;
import be.lassi.util.NLS;

public class PreHeatingFrame extends BasicFrame {

    public PreHeatingFrame(final ShowContext context) {
        super(context, NLS.get("preheating.window.title"));
        init();
    }

    private void addMouseListener(final JComponent component, final int channelIndex) {
        component.addMouseListener(new MouseAdapter() {

            public void mouseClicked(final MouseEvent e) {
                toggle(channelIndex);
            }
        });
    }

    private JComponent createButtonAllOff() {
        JButton button = new SmallButton(NLS.get("preheating.action.allOff"));
        button.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                getContext().getPreHeating().setAllChannelsEnabled(false);
            }
        });
        return button;
    }

    private JComponent createButtonAllOn() {
        JButton button = new SmallButton(NLS.get("preheating.action.allOn"));
        button.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                (new Thread() {

                    public void run() {
                        getContext().getPreHeating().setAllChannelsEnabled(true);
                    }
                }).start();
            }
        });
        return button;
    }

    private JComponent createLabelChannelId(final int channelIndex) {
        final JTextField label = new JTextField("" + (channelIndex + 1), 2);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setEditable(false);
        label.setBorder(null);
        label.setBackground(null);
        label.setHighlighter(null);
        final BooleanHolder holder = getContext().getPreHeating().getChannelEnabled(channelIndex);
        holder.add(new BooleanListener() {

            public void changed() {
                if (holder.getValue()) {
                    label.setBackground(Color.white);
                } else {
                    label.setBackground(null);
                }
            }
        });
        addMouseListener(label, channelIndex);
        return label;
    }

    protected JComponent createPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        for (int i = 0; i < getShow().getNumberOfChannels(); i++) {
            panel.add(createLabelChannelId(i));
        }
        panel.add(createButtonAllOn());
        panel.add(createButtonAllOff());
        panel.add(new LevelSpinner(getPreHeating().getLevel()));
        return panel;
    }

    private PreHeating getPreHeating() {
        return getContext().getPreHeating();
    }

    /**
     * Toggle enabled/disabled state for channel with given index.
     */
    private void toggle(final int channelIndex) {
        BooleanHolder holder = getPreHeating().getChannelEnabled(channelIndex);
        holder.setValue(!holder.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isObsolete() {
        return true;
    }
}
