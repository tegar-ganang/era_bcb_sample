package be.lassi.ui.main;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import be.lassi.base.NameListener;
import be.lassi.base.NamedObject;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Level;
import be.lassi.domain.Show;
import be.lassi.domain.Tracker;
import be.lassi.domain.TrackerEngine;
import be.lassi.domain.TrackerSelectionListener;
import be.lassi.ui.base.BasicFrame;
import be.lassi.ui.widgets.LevelIndicatorHorizontal;
import be.lassi.ui.widgets.LevelLabel;
import be.lassi.util.NLS;

public class TrackerFrame extends BasicFrame {

    public TrackerFrame(final ShowContext context) {
        super(context, NLS.get("tracker.window.title"));
        init();
    }

    private void addMouseListener(final JComponent component, final int id) {
        component.addMouseListener(new MouseAdapter() {

            public void mouseClicked(final MouseEvent e) {
                if (e.isControlDown()) {
                    getTrackerEngine().selectExtra(id);
                } else if (e.isShiftDown()) {
                    getTrackerEngine().selectRange(id);
                } else {
                    getTrackerEngine().select(id);
                }
            }

            public void mousePressed(final MouseEvent e) {
                getTrackerEngine().startTracking();
            }

            public void mouseReleased(final MouseEvent e) {
                getTrackerEngine().setPosition(0);
            }
        });
    }

    private void addMouseMotionListener(final JComponent component) {
        component.addMouseMotionListener(new MouseMotionAdapter() {

            public void mouseDragged(final MouseEvent e) {
                int x = e.getX();
                if (x > 0 && x < component.getWidth()) {
                    x = 0;
                } else {
                    if (x > component.getWidth()) {
                        x -= component.getWidth();
                    }
                }
                getTrackerEngine().setPosition(x);
            }
        });
    }

    private void addTracker(final JPanel panel, final GridBagConstraints c, final int index) {
        Channel channel = getShow().getChannels().get(index);
        Tracker tracker = getShow().getTrackerEngine().get(index);
        c.gridy = index;
        c.weightx = 1;
        c.gridx = 0;
        panel.add(createLabelChannelName(channel), c);
        c.weightx = 0;
        c.gridx = 1;
        panel.add(createLabelChannelId(getShow(), index), c);
        c.gridx = 2;
        panel.add(createLevelIndicator(tracker.getLevel(), index), c);
        c.gridx = 3;
        panel.add(new LevelLabel(tracker.getLevel()), c);
    }

    private JComponent createLabelChannelId(final Show show, final int id) {
        final JTextField label = new JTextField("" + (id + 1), 2);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setEditable(false);
        label.setBorder(null);
        label.setBackground(null);
        label.setHighlighter(null);
        final Tracker tracker = show.getTrackerEngine().get(id);
        tracker.add(new TrackerSelectionListener() {

            public void selectionChanged() {
                if (tracker.isSelected()) {
                    label.setBackground(Color.white);
                } else {
                    label.setBackground(null);
                }
            }
        });
        addMouseListener(label, id);
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

    private JComponent createLevelIndicator(final Level level, final int index) {
        JComponent indicator = new LevelIndicatorHorizontal(level);
        addMouseListener(indicator, index);
        addMouseMotionListener(indicator);
        return indicator;
    }

    protected JComponent createPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 3, 1, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < getShow().getNumberOfChannels(); i++) {
            addTracker(panel, c, i);
        }
        return panel;
    }

    private TrackerEngine getTrackerEngine() {
        return getShow().getTrackerEngine();
    }

    public static void main(final String[] args) {
        new TrackerFrame(new ShowContext()).setVisible(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isObsolete() {
        return true;
    }
}
