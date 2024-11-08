package be.lassi.domain;

import static be.lassi.util.Util.newArrayList;
import java.util.Iterator;
import java.util.List;
import be.lassi.util.Util;

public class Attribute implements Iterable<FixtureChannel> {

    public static final String INTENSITY = "Intensity";

    public static final String PAN = "Pan";

    public static final String PAN_FINE = "Pan-fine";

    public static final String TILT = "Tilt";

    public static final String TILT_FINE = "Tilt-fine";

    public static final String RGB = "RGB";

    public static final String CMY = "CMY";

    public static final String RED = "Red";

    public static final String GREEN = "Green";

    public static final String BLUE = "Blue";

    public static final String CYAN = "Cyan";

    public static final String MAGENTA = "Magenta";

    public static final String YELLOW = "Yellow";

    private final AttributeDefinition definition;

    private final List<FixtureChannel> channels = newArrayList();

    public Attribute(final AttributeDefinition definition) {
        this.definition = definition;
        initChannels();
    }

    public AttributeDefinition getDefinition() {
        return definition;
    }

    public FixtureChannel getChannel(final int index) {
        return channels.get(index);
    }

    public int getChannelCount() {
        return channels.size();
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + definition.getName() + ")";
    }

    private void initChannels() {
        String string = definition.getChannels();
        String[] substrings = string.split(",");
        for (int i = 0; i < substrings.length; i++) {
            int offset = Util.toInt(substrings[i]);
            String channelName = channelName(i);
            channels.add(new FixtureChannel(channelName, offset));
        }
    }

    public int getMaxChannelNumber() {
        int max = 0;
        for (FixtureChannel channel : channels) {
            max = Math.max(max, channel.getNumber());
        }
        return max;
    }

    private String channelName(final int index) {
        String channelName = "" + (index + 1);
        if (PAN.equals(definition.getName())) {
            if (index == 0) {
                channelName = PAN;
            } else if (index == 1) {
                channelName = PAN_FINE;
            }
        } else if (TILT.equals(definition.getName())) {
            if (index == 0) {
                channelName = TILT;
            } else if (index == 1) {
                channelName = TILT_FINE;
            }
        } else if (RGB.equals(definition.getName())) {
            if (index == 0) {
                channelName = RED;
            } else if (index == 1) {
                channelName = GREEN;
            } else if (index == 2) {
                channelName = BLUE;
            }
        } else if (CMY.equals(definition.getName())) {
            if (index == 0) {
                channelName = CYAN;
            } else if (index == 1) {
                channelName = MAGENTA;
            } else if (index == 2) {
                channelName = YELLOW;
            }
        } else if (index == 0) {
            channelName = definition.getName();
        }
        return channelName;
    }

    public Iterator<FixtureChannel> iterator() {
        return channels.iterator();
    }
}
