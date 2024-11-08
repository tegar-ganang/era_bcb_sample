import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentDownloader;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import java.net.MalformedURLException;
import java.net.URL;

public class FooPlugin implements Plugin {

    public void initialize(PluginInterface pluginInterface) throws PluginException {
        LoggerChannel loggerChannel = pluginInterface.getLogger().getChannel("foo");
        DownloadManager downloadManager = pluginInterface.getDownloadManager();
        TorrentManager torrentManager = pluginInterface.getTorrentManager();
        String url = "http://www.demonoid.com/files/download/HTTP/1738055/14132955";
        pluginInterface.addListener(new FooPluginListener(pluginInterface));
        try {
            TorrentDownloader torrentDownloader = torrentManager.getURLDownloader(new URL(url));
            Torrent torrent = torrentDownloader.download();
            Download download = downloadManager.addDownload(torrent);
            loggerChannel.logAlert(LoggerChannel.LT_INFORMATION, download.getName() + " \n added succesfully.");
        } catch (MalformedURLException e) {
            loggerChannel.logAlert("URL of the torrent was not right.", e);
        } catch (TorrentException e) {
            loggerChannel.logAlert("Torrent was not right.", e);
        } catch (DownloadException e) {
            loggerChannel.logAlert("Torrent could not be added.", e);
        }
    }
}
