package gnu.launcher.awt;

import java.awt.*;
import java.io.*;

public class AwtUtil {

    /**
	* Centers a window on the container (null for the desktop).
	**/
    public static void center(Window window, Container container) {
        Dimension size = window.getSize();
        Rectangle containerBounds = container == null ? new Rectangle(0, 0, Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height) : container.getBounds();
        window.setBounds(containerBounds.x + (containerBounds.width - size.width) / 2, containerBounds.y + (containerBounds.height - size.height) / 2, size.width, size.height);
    }

    public static Image createImage(String filename) throws IOException {
        InputStream input = new FileInputStream(filename);
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        int blockSize = 1024;
        byte[] block = new byte[blockSize];
        int bytes;
        while ((bytes = input.read(block, 0, blockSize)) > -1) bytestream.write(block, 0, bytes);
        return Toolkit.getDefaultToolkit().createImage(bytestream.toByteArray());
    }
}
