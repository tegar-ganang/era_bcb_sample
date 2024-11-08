package net.sourceforge.pyrus.bundle.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import net.sourceforge.pyrus.gfx.api.ImageGfx;
import net.sourceforge.pyrus.hal.Kernel;
import net.sourceforge.pyrus.mcollection.api.Playlist;
import net.sourceforge.pyrus.mcollection.api.Song;
import net.sourceforge.pyrus.mplayer.api.MusicPlayer;
import net.sourceforge.pyrus.mplayer.api.MusicPlayer.State;
import net.sourceforge.pyrus.screen.api.Menu;
import net.sourceforge.pyrus.screen.api.Theme;
import net.sourceforge.pyrus.widget.api.InputEvent;
import net.sourceforge.pyrus.widget.api.PIcon;
import net.sourceforge.pyrus.widget.api.PLabel;
import net.sourceforge.pyrus.widget.api.PView;
import net.sourceforge.pyrus.widget.api.PWidget;
import net.sourceforge.pyrus.widget.api.WidgetFactory;
import net.sourceforge.pyrus.widget.api.PIcon.ResizePolicy;

public class PlaylistScreen extends JPanel implements PView, PLabel.Listener, Playlist.Listener, MusicPlayer.Listener, PIcon.Listener {

    private static final Kernel kernel = Kernel.get();

    private static final WidgetFactory wfactory = kernel.getMObject(WidgetFactory.class);

    private static final Theme theme = kernel.getMObject(Theme.class);

    private static final Playlist playlist = kernel.getMObject(Playlist.class);

    private static final MusicPlayer player = kernel.getMObject(MusicPlayer.class);

    private static final ImageGfx imgGfx = kernel.getMObject(ImageGfx.class);

    private static final Menu menu = kernel.getMObject(Menu.class);

    private static final Font titleFont = theme.getFont(Font.BOLD, 20);

    private static final Font songFont = theme.getFont(Font.BOLD, 18);

    private static final Font infoFont = theme.getFont(Font.PLAIN, 12);

    private static final Image jukebox = theme.getImage("jukebox");

    private static final Image currentSong = theme.getImage("currentSong");

    private static final Image selSong = theme.getImage("info24");

    private static final Image otherSong = theme.getImage("otherSong");

    private PLabel title = wfactory.getLabel("Lista de reproducción", Color.WHITE, titleFont);

    private PIcon prev = wfactory.getIcon(theme.getImage("prev24"), ResizePolicy.NO_RESIZE_LEFT);

    private PIcon next = wfactory.getIcon(theme.getImage("next24"), ResizePolicy.NO_RESIZE_RIGHT);

    private PIcon cover = wfactory.getIcon(jukebox, ResizePolicy.ADAPT);

    private PIcon play = wfactory.getIcon(theme.getImage("play32"), ResizePolicy.NO_RESIZE);

    private PIcon pause = wfactory.getIcon(theme.getImage("pause32"), ResizePolicy.NO_RESIZE);

    private PIcon skipback = wfactory.getIcon(theme.getImage("skipback32"), ResizePolicy.NO_RESIZE);

    private PIcon skip = wfactory.getIcon(theme.getImage("skip32"), ResizePolicy.NO_RESIZE);

    private PIcon stop = wfactory.getIcon(theme.getImage("stop32"), ResizePolicy.NO_RESIZE);

    private PLabel infoSong = wfactory.getLabel("", Color.WHITE, infoFont);

    private PLabel infoAlbum = wfactory.getLabel("", Color.WHITE, infoFont);

    private PLabel infoArtist = wfactory.getLabel("", Color.WHITE, infoFont);

    private PLabel infoLength = wfactory.getLabel("", Color.WHITE, infoFont);

    private MigLayout layout = new MigLayout("fill,wrap 2", "[33%][67%]", "[32!][][grow][]");

    private JPanel labelsPanel = new JPanel(new MigLayout("fill,wrap 2"));

    private JPanel coverPanel = new JPanel(new MigLayout("fill"));

    private JPanel infoPanel = new JPanel(new MigLayout("fill,wrap 1"));

    private JPanel controlsPanel = new JPanel(new MigLayout("fill"));

    private final int pageSize = 8;

    private PLabel[] labels = new PLabel[pageSize];

    private PIcon[] labelIcon = new PIcon[pageSize];

    private List<Song> songs;

    private int page;

    private Action prevAction = new AbstractAction("Retroceder página") {

        public void actionPerformed(ActionEvent e) {
            gotoPage(page - 1);
        }
    };

    private Action nextAction = new AbstractAction("Avanzar página") {

        public void actionPerformed(ActionEvent e) {
            gotoPage(page + 1);
        }
    };

    private Action playAction = new AbstractAction("Empezar reproducción") {

        public void actionPerformed(ActionEvent e) {
            if (player.getState() == State.STOP) {
                playlist.playNext();
            }
        }
    };

    private Action pauseAction = new AbstractAction("Pausar reproducción") {

        public void actionPerformed(ActionEvent e) {
            player.pause();
        }
    };

    private Action skipBack = new AbstractAction("Canción anterior") {

        public void actionPerformed(ActionEvent e) {
            playlist.playPrevious();
        }
    };

    private Action skipAction = new AbstractAction("Canción siguiente") {

        public void actionPerformed(ActionEvent e) {
            playlist.playNext();
        }
    };

    private Action stopAction = new AbstractAction("Parar reproducción") {

        public void actionPerformed(ActionEvent e) {
            playlist.stopPlaying();
        }
    };

    private Action clearAction = new AbstractAction("Vaciar la lista") {

        public void actionPerformed(ActionEvent e) {
            playlist.stopPlaying();
            playlist.clear();
        }
    };

    public PlaylistScreen() {
        player.addListener(this);
        playlist.addListener(this);
        infoSong.setHorizontalAlignment(PLabel.LEFT);
        infoAlbum.setHorizontalAlignment(PLabel.LEFT);
        infoArtist.setHorizontalAlignment(PLabel.LEFT);
        infoLength.setHorizontalAlignment(PLabel.LEFT);
        setOpaque(false);
        setLayout(layout);
        add(this.prev.getComponent(), "w 48!,growy,spanx 2,split 3");
        add(this.title.getComponent(), "grow");
        add(this.next.getComponent(), "w 48!,growy,right");
        add(infoPanel, "grow");
        add(labelsPanel, "grow,spany 3");
        add(coverPanel, "grow");
        add(controlsPanel, "grow");
        labelsPanel.setOpaque(false);
        for (int i = 0; i < pageSize; i++) {
            labels[i] = wfactory.getLabel("", Color.WHITE, songFont);
            labels[i].setHorizontalAlignment(PLabel.LEFT);
            labels[i].addListener(this);
            labelIcon[i] = wfactory.getIcon(otherSong, ResizePolicy.NO_RESIZE);
            labelIcon[i].addListener(this);
            labelsPanel.add(labelIcon[i].getComponent(), "al center center,w 32!");
            labelsPanel.add(labels[i].getComponent(), "grow");
        }
        coverPanel.setOpaque(false);
        coverPanel.add(cover.getComponent(), "grow");
        infoPanel.setOpaque(false);
        infoPanel.add(infoSong.getComponent(), "grow");
        infoPanel.add(infoAlbum.getComponent(), "grow");
        infoPanel.add(infoArtist.getComponent(), "grow");
        infoPanel.add(infoLength.getComponent(), "grow");
        controlsPanel.setOpaque(false);
        controlsPanel.add(play.getComponent(), "h 32!");
        controlsPanel.add(pause.getComponent());
        controlsPanel.add(new JLabel(""), "grow");
        controlsPanel.add(skipback.getComponent());
        controlsPanel.add(skip.getComponent());
        controlsPanel.add(new JLabel(""), "grow");
        controlsPanel.add(stop.getComponent());
        prev.addAction(prevAction);
        next.addAction(nextAction);
        play.addAction(playAction);
        pause.addAction(pauseAction);
        skipback.addAction(skipBack);
        skip.addAction(skipAction);
        stop.addAction(stopAction);
        refreshList();
        synchUI(playlist.getSelectedIndex());
    }

    public JComponent getComponent() {
        return this;
    }

    public void viewAttached() {
        menu.addAction(playAction);
        menu.addAction(pauseAction);
        menu.addAction(skipBack);
        menu.addAction(skipAction);
        menu.addAction(stopAction);
        menu.addAction(clearAction);
    }

    public void viewDetached() {
        menu.clearActions();
    }

    public void labelClicked(InputEvent event) {
        PWidget label = event.getWidget();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == label) {
                synchUI(page * pageSize + i);
            }
        }
    }

    public void iconClicked(InputEvent event) {
        PWidget icon = event.getWidget();
        for (int i = 0; i < labels.length; i++) {
            if (labelIcon[i] == icon) {
                playlist.setSelectedIndex(page * pageSize + i);
            }
        }
    }

    public void listSelectedIndexChanged(Playlist list) {
        synchUI(playlist.getSelectedIndex());
    }

    public void listSongsChanged(Playlist list) {
        refreshList();
    }

    public void songStarted(File songFile) {
        synchUI(playlist.getSelectedIndex());
    }

    public void songFinished(File song) {
    }

    public void songPaused(File song) {
    }

    public void songResumed(File song) {
    }

    private void synchUI(int selIndex) {
        if (selIndex < 0 || selIndex >= songs.size()) {
            infoSong.setText("");
            infoAlbum.setText("");
            infoArtist.setText("");
            infoLength.setText("");
            cover.setImage(jukebox);
        } else {
            Song song = songs.get(selIndex);
            infoSong.setText(song.getTitle());
            infoAlbum.setText(song.getAlbum().getName());
            infoArtist.setText(song.getAuthor().getName());
            infoLength.setText(song.getFormattedDuration() + " - " + song.getChannels() + "ch - " + song.getBitrate() + "kbps");
            Image img = imgGfx.loadImage(song.getAlbum().getCover());
            if (img == null) {
                cover.setImage(jukebox);
            } else {
                cover.setImage(img);
            }
        }
        for (int i = 0; i < labels.length; i++) {
            int songIndex = page * pageSize + i;
            if (songIndex == playlist.getSelectedIndex()) {
                labelIcon[i].setImage(currentSong);
            } else if (songIndex == selIndex) {
                labelIcon[i].setImage(selSong);
            } else {
                labelIcon[i].setImage(otherSong);
            }
        }
    }

    private void refreshList() {
        songs = playlist.getSongs();
        gotoPage(0);
    }

    private void gotoPage(int page) {
        int pages = (songs.size() + pageSize - 1) / pageSize;
        if (pages <= 0) {
            pages = 1;
        }
        if (page < 0) {
            page = pages - 1;
        } else if (page >= pages) {
            page = 0;
        }
        this.page = page;
        title.setText("Lista de reproducción (" + (page + 1) + "/" + pages + ")");
        int offset = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int index = offset + i;
            if (index < songs.size()) {
                labels[i].setText(songs.get(index).getTitle());
                labels[i].getComponent().setVisible(true);
                labelIcon[i].getComponent().setVisible(true);
            } else {
                labels[i].setText("");
                labels[i].getComponent().setVisible(false);
                labelIcon[i].getComponent().setVisible(false);
            }
        }
        synchUI(playlist.getSelectedIndex());
        repaint();
    }

    @Override
    public String getTitle() {
        return title.getText();
    }

    @Override
    public void setTitle(String title) {
        this.title.setText(title);
    }
}
