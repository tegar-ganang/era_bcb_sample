package org.gstreamer.interfaces;

import java.util.List;
import org.gstreamer.Element;
import org.gstreamer.lowlevel.GstAPI.GstCallback;
import com.sun.jna.Pointer;
import static org.gstreamer.lowlevel.GstColorBalanceAPI.GSTCOLORBALANCE_API;

public class ColorBalance extends GstInterface {

    /**
	 * Wraps the {@link Element} in a <tt>ColorBalance</tt> interface
	 * 
	 * @param element
	 *            the element to use as a <tt>ColorBalance</tt>
	 * @return a <tt>ColorBalance</tt> for the element
	 */
    public static final ColorBalance wrap(Element element) {
        return new ColorBalance(element);
    }

    /**
	 * Creates a new ColorBalance instance
	 * 
	 * @param element
	 *            the element that implements the ColorBalance interface
	 */
    private ColorBalance(Element element) {
        super(element, GSTCOLORBALANCE_API.gst_color_balance_get_type());
    }

    /**
	 * Retrieves a list of ColorBalanceChannels from the ColorBalance
	 * 
	 * @return a list of color balance channels available on this device
	 */
    public List<ColorBalanceChannel> getChannelList() {
        return objectList(GSTCOLORBALANCE_API.gst_color_balance_list_channels(this), new ListElementCreator<ColorBalanceChannel>() {

            public ColorBalanceChannel create(Pointer pointer) {
                return channelFor(pointer, true);
            }
        });
    }

    /**
	 * Retrieves a ColorBalanceChannel for the given Pointer
	 * 
	 * @param pointer
	 * @param needRef
	 * @return a ColorBalanceChannel instance
	 */
    private final ColorBalanceChannel channelFor(Pointer pointer, boolean needRef) {
        return new ColorBalanceChannel(this, pointer, needRef, true);
    }

    /**
	 * Signal emitted when color balance value changed
	 * 
	 * @see #connect(VALUE_CHANGED)
	 * @see #disconnect(VALUE_CHANGED)
	 */
    public static interface VALUE_CHANGED {

        /**
		 * Called when the color balance channel value changes
		 */
        public void colorBalanceValueChanged(ColorBalance colorBalance, ColorBalanceChannel channel, int value);
    }

    /**
	 * Add a listener for norm-changed messages.
	 * 
	 * @param listener
	 *            the listener to be called when the norm changes
	 */
    public void connect(final VALUE_CHANGED listener) {
        element.connect(VALUE_CHANGED.class, listener, new GstCallback() {

            @SuppressWarnings("unused")
            public boolean callback(Pointer colorBalance, ColorBalanceChannel channel, int value) {
                listener.colorBalanceValueChanged(ColorBalance.this, channel, value);
                return true;
            }
        });
    }

    /**
	 * Disconnect the listener for norm-changed messages.
	 * 
	 * @param listener
	 *            the listener that was registered to receive the message.
	 */
    public void disconnect(VALUE_CHANGED listener) {
        element.disconnect(VALUE_CHANGED.class, listener);
    }
}
