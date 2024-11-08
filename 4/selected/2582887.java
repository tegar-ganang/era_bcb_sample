package octlight.image;

/**
 * @author $Author: creator $
 * @version $Revision: 1.2 $
 */
public class MemoryImage implements Image {

    private int width;

    private int height;

    private ChannelLayout layout;

    private byte[] pixels;

    public MemoryImage(int width, int height, ChannelLayout layout, byte[] pixels) {
        this.width = width;
        this.height = height;
        this.layout = layout;
        this.pixels = pixels;
    }

    public int getBitsPerChannel() {
        return 8;
    }

    public ChannelLayout getChannelLayout() {
        return layout;
    }

    public byte[] getData() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
