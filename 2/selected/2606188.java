package net.sf.janos.ui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import net.sf.janos.control.AVTransportListener;
import net.sf.janos.control.AVTransportService;
import net.sf.janos.control.ZonePlayer;
import net.sf.janos.model.MediaInfo;
import net.sf.janos.model.PositionInfo;
import net.sf.janos.model.TrackMetaData;
import net.sf.janos.model.xml.AVTransportEventHandler.AVTransportEventType;
import net.sf.janos.util.ui.ImageUtilities;
import net.sf.janos.util.ui.LabelHelper;
import net.sf.janos.util.ui.SingleWorkQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * A UI component for displaying the now-playing and queue.
 * @author David Wheeler
 *
 */
public class NowPlaying extends Composite implements AVTransportListener {

    private static final Log LOG = LogFactory.getLog(NowPlaying.class);

    /**
	 * The color of the labels
	 */
    private final Color LABEL_COLOR;

    /**
	 * The name of the artist of the currently playing track
	 */
    private final Label trackArtist;

    /**
	 * The name of the album of the currently playing track
	 */
    private final Label trackAlbum;

    /**
	 * The name of the currently playing track
	 */
    private final Label trackName;

    /**
	 * The label with the album artwork
	 */
    private final Label artwork;

    private final Group nowPlaying;

    private final TransportControl transportControl;

    /**
	 * The zone we represent/manage
	 */
    private final ZonePlayer zone;

    private final ArtworkWorker artworkWorker;

    /**
	 * Creates a new NowPlaying which is responsible for showing the title, artist, album and artwork for
	 * a song which is currently playing on a zone.
	 * 
	 * @param parent
	 * @param style
	 * @param controller
	 */
    public NowPlaying(Composite parent, int style, ZonePlayer zone) {
        super(parent, style);
        this.zone = zone;
        LABEL_COLOR = new Color(parent.getDisplay(), new RGB(0, 0, 128));
        setLayout(new GridLayout(1, false));
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        nowPlaying = new Group(this, SWT.NONE);
        nowPlaying.setText("Now Playing");
        nowPlaying.setForeground(LABEL_COLOR);
        nowPlaying.setLayout(new GridLayout(3, false));
        nowPlaying.setLayoutData(gridData);
        artwork = new Label(nowPlaying, 0);
        GridData gd = new GridData();
        gd.verticalSpan = 4;
        gd.widthHint = 128;
        gd.heightHint = 128;
        artwork.setLayoutData(gd);
        ArtworkDisplayer artworkDisplayer = new ArtworkDisplayer(artwork);
        GridData labelGridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        Label trackArtistLabel = new Label(nowPlaying, SWT.RIGHT);
        trackArtistLabel.setText("Artist:");
        trackArtistLabel.setForeground(LABEL_COLOR);
        trackArtist = new Label(nowPlaying, SWT.LEFT);
        trackArtist.setLayoutData(gridData);
        Label trackAlbumLabel = new Label(nowPlaying, SWT.RIGHT);
        trackAlbumLabel.setText("Album:");
        trackAlbumLabel.setForeground(LABEL_COLOR);
        trackAlbum = new Label(nowPlaying, SWT.LEFT);
        trackAlbum.setLayoutData(gridData);
        Label trackNameLabel = new Label(nowPlaying, SWT.RIGHT);
        trackNameLabel.setText("Name:");
        trackNameLabel.setForeground(LABEL_COLOR);
        trackName = new Label(nowPlaying, SWT.LEFT);
        trackName.setLayoutData(gridData);
        int width = trackArtistLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
        width = Math.max(width, trackAlbumLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        width = Math.max(width, trackNameLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        labelGridData.widthHint = width;
        trackNameLabel.setLayoutData(labelGridData);
        trackArtistLabel.setLayoutData(labelGridData);
        trackAlbumLabel.setLayoutData(labelGridData);
        transportControl = new TransportControl(nowPlaying, SWT.NONE, zone);
        GridData transportGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        transportGridData.horizontalSpan = 2;
        transportGridData.horizontalAlignment = GridData.FILL;
        transportGridData.grabExcessHorizontalSpace = true;
        transportControl.setLayoutData(transportGridData);
        artworkWorker = new ArtworkWorker(new SingleWorkQueue());
        artworkWorker.start();
        zone.getMediaRendererDevice().getAvTransportService().addAvTransportListener(this);
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void dispose() {
        zone.getMediaRendererDevice().getAvTransportService().removeAvTransportListener(this);
        artworkWorker.q.addWork(artworkWorker.NO_MORE_WORK);
        transportControl.dispose();
        super.dispose();
    }

    /**
	 * Reloads the now-playing.
	 */
    public void showNowPlaying() {
        new NowPlayingFetcher().start();
    }

    /**
	 * Asynchronously sets the now playing details to the given information. The
	 * image is loaded in a background thread before being displayed.
	 * 
	 * @param artist
	 * @param album
	 * @param name
	 * @param artworkUrl
	 */
    protected void setNowPlayingAsync(final String artist, final String album, final String name, final URL artworkUrl, final String groupTitle) {
        getDisplay().asyncExec(new NowPlayingSetter(album, artworkUrl, artist, name, groupTitle));
    }

    /**
	 * {@inheritDoc}
	 */
    public void valuesChanged(Set<AVTransportEventType> events, AVTransportService source) {
        new NowPlayingFetcher().start();
    }

    /**
	 * Sets the now playing labels
	 */
    private class ArtworkWorker extends Thread {

        final Object NO_MORE_WORK = new Object();

        SingleWorkQueue q;

        ArtworkWorker(SingleWorkQueue q) {
            this.q = q;
            this.setName("ArtworkWorker: " + zone.getDevicePropertiesService().getZoneAttributes().getName());
            this.setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Object x = q.getWork();
                    if (x == NO_MORE_WORK) {
                        break;
                    }
                    if (x == null) {
                        loadImage((Image) null, null);
                    } else {
                        loadImage((URL) x);
                    }
                }
            } catch (InterruptedException e) {
                LOG.debug("image loading interrupted.");
                Thread.currentThread().interrupt();
            }
        }

        private void loadImage(URL url) {
            ImageData imageData;
            Image artworkImage = null;
            InputStream artworkStream = null;
            try {
                artworkStream = new BufferedInputStream(url.openStream());
                imageData = new ImageLoader().load(artworkStream)[0];
                Image tmpImage = new Image(getDisplay(), imageData);
                artworkImage = ImageUtilities.scaleImageTo(tmpImage, 128, 128);
                tmpImage.dispose();
            } catch (Exception e) {
            } finally {
                if (artworkStream != null) {
                    try {
                        artworkStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            loadImage(artworkImage, url);
        }

        private void loadImage(Image image, URL url) {
            final Image i = image;
            final URL u = url;
            getDisplay().asyncExec(new Runnable() {

                public void run() {
                    URL lastURL = (URL) artwork.getData();
                    if (u == null || lastURL == null || !lastURL.sameFile(u)) {
                        Image oldImage = artwork.getImage();
                        artwork.setImage(i);
                        if (oldImage != null) {
                            oldImage.dispose();
                        }
                        artwork.setData(u);
                        layout();
                    }
                }
            });
        }
    }

    private final class NowPlayingSetter extends Thread {

        private final String album;

        private final URL url;

        private final String artist;

        private final String name;

        private final String groupTitle;

        private NowPlayingSetter(String album, URL url, String artist, String name, String groupTitle) {
            this.album = album;
            this.url = url;
            this.artist = artist;
            this.name = name;
            this.groupTitle = groupTitle;
            this.setName("NowPlaying:NowPlayingSetter: " + zone.getDevicePropertiesService().getZoneAttributes().getName());
        }

        @Override
        public void run() {
            trackArtist.setText(LabelHelper.escapeText(artist));
            trackAlbum.setText(LabelHelper.escapeText(album));
            trackName.setText(LabelHelper.escapeText(name));
            nowPlaying.setText(LabelHelper.escapeText(groupTitle));
            URL oldURL = (URL) artwork.getData();
            if (oldURL != null && url != null && url.sameFile(oldURL) == false) {
                if (artwork.getImage() != null) {
                    artwork.getImage().dispose();
                    artwork.setData(null);
                }
                artwork.setImage(null);
            }
            artworkWorker.q.addWork(url);
            layout();
        }
    }

    /**
	 * A Runnable that requests the currently playing track and applies the info to this QueueDisplay
	 * @author David Wheeler
	 *
	 */
    protected class NowPlayingFetcher extends Thread {

        public NowPlayingFetcher() {
            this.setName("NowPlaying:NowPlayingFetcher:" + zone.getDevicePropertiesService().getZoneAttributes().getName());
        }

        @Override
        public void run() {
            if (zone == null) {
                return;
            }
            try {
                MediaInfo mediaInfo = zone.getMediaRendererDevice().getAvTransportService().getMediaInfo();
                PositionInfo posInfo = zone.getMediaRendererDevice().getAvTransportService().getPositionInfo();
                String uri = mediaInfo.getCurrentURI();
                if (uri == null || posInfo == null) {
                    setNowPlayingAsync("", "", "", null, "No Music");
                } else if (uri.startsWith("x-rincon-queue:")) {
                    if (posInfo.getTrackMetaData() != null) {
                        setNowPlayingAsync(posInfo.getTrackMetaData().getCreator(), posInfo.getTrackMetaData().getAlbum(), posInfo.getTrackMetaData().getTitle(), posInfo.getTrackMetaData().getAlbumArtUrl(zone), "Now Playing");
                    } else {
                        setNowPlayingAsync("", "", "", null, "No Music");
                    }
                } else if (uri.startsWith("x-rincon:")) {
                } else if (uri.startsWith("x-file-cifs:")) {
                    setNowPlayingAsync(mediaInfo.getCurrentURIMetaData().getCreator(), mediaInfo.getCurrentURIMetaData().getAlbum(), mediaInfo.getCurrentURIMetaData().getTitle(), mediaInfo.getCurrentURIMetaData().getAlbumArtUrl(zone), "Now Playing: CIFS");
                } else if (uri.startsWith("x-rincon-mp3radio:")) {
                    setNowPlayingAsync("", mediaInfo.getCurrentURI().substring(mediaInfo.getCurrentURI().lastIndexOf("://") + 3), mediaInfo.getCurrentURIMetaData().getTitle(), null, "Internet Radio");
                } else if (uri.startsWith("x-rincon-stream:")) {
                    setNowPlayingAsync("Line In", "", mediaInfo.getCurrentURIMetaData().getTitle(), null, "Now Playing: Local Library");
                } else if (uri.startsWith("pndrradio:")) {
                    try {
                        TrackMetaData i = posInfo.getTrackMetaData();
                        setNowPlayingAsync(i.getCreator(), i.getAlbum(), i.getTitle(), new URL(i.getAlbumArtUri()), "Pandora Radio: " + mediaInfo.getCurrentURIMetaData().getTitle());
                    } catch (Exception e) {
                        setNowPlayingAsync("", "", "", null, "No Music");
                    }
                } else if (uri.startsWith("rdradio:station:")) {
                    try {
                        TrackMetaData i = posInfo.getTrackMetaData();
                        setNowPlayingAsync(i.getCreator(), i.getAlbum(), i.getTitle(), i.getAlbumArtUrl(zone), "Rhapsody Channel: " + mediaInfo.getCurrentURIMetaData().getTitle());
                    } catch (Exception e) {
                        setNowPlayingAsync("", "", "", null, "No Music");
                    }
                } else if (uri.startsWith("lastfm:")) {
                    try {
                        TrackMetaData i = posInfo.getTrackMetaData();
                        setNowPlayingAsync(i.getCreator(), i.getAlbum(), i.getTitle(), new URL(i.getAlbumArtUri()), "last.fm: " + mediaInfo.getCurrentURIMetaData().getTitle());
                    } catch (Exception e) {
                        setNowPlayingAsync("", "", "", null, "No Music");
                    }
                } else if (uri.startsWith("x-sonosapi-stream:")) {
                    try {
                        TrackMetaData i = posInfo.getTrackMetaData();
                        setNowPlayingAsync(i.getStreamContent(), "", "", i.getAlbumArtUrl(zone), "Radio Station: " + mediaInfo.getCurrentURIMetaData().getTitle());
                    } catch (Exception e) {
                        setNowPlayingAsync("", "", "", null, "No Music");
                    }
                } else {
                    if (LOG.isWarnEnabled() && mediaInfo != null) {
                        LOG.warn("Couldn't find type of " + uri);
                        setNowPlayingAsync("", "", "", null, "Unknown Type of Music");
                    }
                }
            } catch (Exception e) {
                LOG.error("Couldn't load queue", e);
            }
        }
    }
}
