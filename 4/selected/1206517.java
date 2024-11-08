package be.lassi.ui.fixtures;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import be.lassi.context.ShowContext;
import be.lassi.domain.Attribute;
import be.lassi.domain.FixtureChannel;
import be.lassi.domain.Level;
import be.lassi.lanbox.domain.Buffer;
import be.lassi.ui.log.ColorBox;
import be.lassi.util.Dmx;

public class AttributeRenderer implements TableCellRenderer {

    private final ColorBox colorBox = new ColorBox();

    private final JLabel label = new JLabel();

    private final ShowContext context;

    public AttributeRenderer(final ShowContext context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        label.setText("");
        Component renderer = label;
        if (value instanceof Attribute) {
            Attribute attribute = (Attribute) value;
            String name = attribute.getDefinition().getName();
            if (Attribute.INTENSITY.equals(name)) {
                Level level = getLevel(attribute, 0);
                label.setText("" + level.getIntValue() + "%");
            } else if (Attribute.RGB.equals(name)) {
                if (attribute.getChannelCount() == 3) {
                    int red = getDmxValue(attribute, 0);
                    int green = getDmxValue(attribute, 1);
                    int blue = getDmxValue(attribute, 2);
                    Color color = new Color(red, green, blue);
                    colorBox.setColor(color);
                    renderer = colorBox;
                }
            } else if (Attribute.CMY.equals(name)) {
                int red = 255 - getDmxValue(attribute, 0);
                int green = 255 - getDmxValue(attribute, 1);
                int blue = 255 - getDmxValue(attribute, 2);
                Color color = new Color(red, green, blue);
                colorBox.setColor(color);
                renderer = colorBox;
            } else {
                if (attribute.getChannelCount() == 1) {
                    if (attribute.getDefinition().getValueCount() > 0) {
                        int dmxValue = getDmxValue(attribute, 0);
                        String valueName = attribute.getDefinition().getValueOf(dmxValue);
                        label.setText(valueName);
                    }
                }
            }
        }
        return renderer;
    }

    private int getDmxValue(final Attribute attribute, final int index) {
        int dmxValue = 0;
        Level level = getLevel(attribute, index);
        if (level != null) {
            float value = level.getValue();
            dmxValue = Dmx.getDmxValue(value);
        }
        return dmxValue;
    }

    private Level getLevel(final Attribute attribute, final int index) {
        Level level = null;
        FixtureChannel channel = attribute.getChannel(index);
        int channelIndex = channel.getNumber() - 1;
        if (channelIndex >= 0) {
            Buffer buffer = context.getLanbox().getMixer();
            level = buffer.getLevels().get(channelIndex);
        }
        return level;
    }
}
