package be.lassi.ui.main;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import be.lassi.base.NameListener;
import be.lassi.base.NamedObject;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Level;
import be.lassi.domain.LevelListener;
import be.lassi.domain.Show;
import be.lassi.ui.base.BasicFrame;
import be.lassi.ui.widgets.LevelIndicatorHorizontal;
import be.lassi.ui.widgets.LevelLabel;

public class SyncFrame extends BasicFrame {

    public SyncFrame(final ShowContext context) {
        super(context, "Synchronizer");
        init();
    }

    private void addLine(final JPanel panel, final GridBagConstraints c, final int index) {
        Channel channel = getShow().getChannels().get(index);
        Level inputLevel = getShow().getInputs().get(index);
        Level channelLevel = channel.getLevel();
        c.gridy = index;
        c.weightx = 1;
        c.gridx = 0;
        panel.add(createLabelChannelName(channel), c);
        c.weightx = 0;
        c.gridx = 1;
        panel.add(createLabelChannelId(getShow(), index), c);
        c.gridx = 2;
        panel.add(new LevelIndicatorHorizontal(inputLevel), c);
        c.gridx = 3;
        panel.add(new LevelLabel(inputLevel), c);
        c.gridx = 4;
        panel.add(new LevelIndicatorHorizontal(channelLevel), c);
        c.gridx = 5;
        panel.add(new LevelLabel(channelLevel), c);
    }

    private JComponent createLabelChannelId(final Show show, final int id) {
        final JTextField label = new JTextField("" + (id + 1), 2);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setEditable(false);
        label.setBorder(null);
        label.setBackground(null);
        label.setHighlighter(null);
        Channel channel = getShow().getChannels().get(id);
        final Level inputLevel = getShow().getInputs().get(id);
        final Level channelLevel = channel.getLevel();
        inputLevel.add(new LevelListener() {

            public void levelChanged() {
                if (inputLevel.getIntValue() == channelLevel.getIntValue()) {
                    label.setBackground(Color.white);
                } else {
                    label.setBackground(null);
                }
            }
        });
        channelLevel.add(new LevelListener() {

            public void levelChanged() {
                if (inputLevel.getIntValue() == channelLevel.getIntValue()) {
                    label.setBackground(Color.white);
                } else {
                    label.setBackground(null);
                }
            }
        });
        return label;
    }

    private JComponent createLabelChannelName(final Channel channel) {
        final JTextField label = new JTextField(channel.getName());
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setEditable(false);
        label.setBorder(null);
        label.setBackground(null);
        label.setHighlighter(null);
        channel.addNameListener(new NameListener() {

            public void nameChanged(final NamedObject object) {
                label.setText(object.getName());
            }
        });
        return label;
    }

    protected JComponent createPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 3, 1, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < getShow().getNumberOfChannels(); i++) {
            addLine(panel, c, i);
        }
        return panel;
    }

    public static void main(final String[] args) {
        new SyncFrame(new ShowContext()).setVisible(true);
    }
}
