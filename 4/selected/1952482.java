package be.lassi.ui.main;

import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Level;
import be.lassi.domain.LevelListener;
import be.lassi.domain.Show;
import be.lassi.ui.base.BasicFrame;
import be.lassi.ui.widgets.LevelDeltaIndicatorVertical;
import be.lassi.ui.widgets.LevelLabel;
import be.lassi.util.NLS;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class SynchronizerFrame extends BasicFrame {

    public SynchronizerFrame(final ShowContext context) {
        super(context, NLS.get("synchronizer.window.title"));
        init();
    }

    private JComponent createLabelChannelId(final Show show, final int id) {
        final JLabel label = new JLabel("" + (id + 1));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        Channel channel = getShow().getChannels().get(id);
        final Level inputLevel = getShow().getChannelInputs().get(id);
        final Level channelLevel = channel.getLevel();
        LevelListener listener = new LevelListener() {

            public void levelChanged() {
                if (inputLevel.getIntValue() == channelLevel.getIntValue()) {
                    label.setBackground(Color.white);
                } else {
                    label.setBackground(null);
                }
            }
        };
        inputLevel.add(listener);
        channelLevel.add(listener);
        return label;
    }

    private void addIndicator(final PanelBuilder builder, final CellConstraints cc, final int x, final int y, final int index) {
        Channel channel = getShow().getChannels().get(index);
        Level inputLevel = getShow().getChannelInputs().get(index);
        Level channelLevel = channel.getLevel();
        builder.add(new LevelDeltaIndicatorVertical(inputLevel, channelLevel), cc.xy(x, y));
        builder.add(new LevelLabel(inputLevel), cc.xy(x, y + 2));
        builder.add(new LevelLabel(channelLevel), cc.xy(x, y + 4));
        builder.add(createLabelChannelId(getShow(), index), cc.xy(x, y + 6));
    }

    @Override
    protected JComponent createPanel() {
        FormLayout layout = new FormLayout("pref, 1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref", "pref, 1dlu, pref, 1dlu, pref, 1dlu, pref,   1dlu, pref, 1dlu, pref, 1dlu, pref, 1dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        int numberOfChannels = getShow().getNumberOfChannels();
        int half = numberOfChannels / 2;
        for (int i = 0; i < half; i++) {
            int x = i * 2 + 1;
            int y = 1;
            addIndicator(builder, cc, x, y, i);
        }
        for (int i = 0; i < half; i++) {
            int channelId = half + i;
            if (channelId < numberOfChannels) {
                int x = i * 2 + 1;
                int y = 9;
                addIndicator(builder, cc, x, y, channelId);
            }
        }
        return builder.getPanel();
    }
}
