import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class OpenURLInSystemBrowser {
	public static void openURL(URI uri) throws IOException, URISyntaxException {
		if(Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			desktop.browse(uri);
		} else {
			throw new UnsupportedOperationException("Desktop is not supported on this platform.");
		}
	}
}
