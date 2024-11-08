package com.jpianobar.main.gui;

import com.jPianoBar.PandoraRater;
import com.jPianoBar.PandoraSong;
import com.jPianoBar.PlaylistDownloader;
import com.jpianobar.main.Pandora;
import com.jpianobar.main.PandoraPlayer;
import org.apache.commons.io.FileUtils;
import org.xmlrpc.android.XMLRPCException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: vincent
 * Date: 7/17/11
 * Time: 10:37 AM
 */
public class JPianoBarMainWindow {

    private JComboBox stationListBox;

    private JTable upcomingTracksTable;

    private JEditorPane trackDetailsField;

    private JPanel mainPanel;

    private JPanel albumArtPanel;

    private JLabel statusField;

    private JPanel mainControlsPanel;

    private JProgressBar musicProgressBar;

    private JSlider volumeSlider;

    public static JLabel statusLabel;

    private static Logger LOG = Logger.getLogger(JPianoBarMainWindow.class.getName());

    public JPianoBarMainWindow() {
        addStationListBoxListener();
        addVolumeChangeListener();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void createUIComponents() {
        statusField = new JLabel();
        statusLabel = statusField;
        musicProgressBar = new JProgressBar();
        volumeSlider = new JSlider();
        mainControlsPanel = new JPanel();
        trackDetailsField = new SongDetailsPane();
        upcomingTracksTable = new UpcomingTracksTable();
        albumArtPanel = new ImagePanel(130, 130);
        Pandora.tracksTable = (UpcomingTracksTable) upcomingTracksTable;
        Box horizontalBox = Box.createHorizontalBox();
        try {
            JButton playPauseButton = buildPlayPauseButton();
            JButton skipButton = buildSkipButton(playPauseButton);
            JButton likeButton = buildLikeButton();
            JButton dislikeButton = buildDislikeButton();
            JButton addStationButton = buildAddStationButton();
            JButton downloadButton = buildDownloadButton();
            JButton songInfoButton = buildSongInfoButton();
            JButton settingsButton = buildSettingsButton();
            JButton helpButton = buildHelpButton();
            addComponentsToBox(horizontalBox, Arrays.<JComponent>asList(playPauseButton, skipButton, likeButton, dislikeButton, addStationButton, downloadButton, songInfoButton, settingsButton, helpButton));
            mainControlsPanel.add(horizontalBox);
        } catch (Exception e) {
            ErrorDialogBuilder.showErrorDialog("Error", e);
        }
        populateStationList();
        try {
            initializePandoraPlayer();
            beginPlayingFirstSong();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage());
        }
    }

    private void beginPlayingFirstSong() {
        Thread songPlayThread = new Thread(new Runnable() {

            public void run() {
                try {
                    Pandora.pandoraPlayer.playSong(Pandora.songsFromStation.get(0));
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        songPlayThread.start();
    }

    private void initializePandoraPlayer() throws XMLRPCException {
        Pandora.selectedStation = Pandora.pandoraAccount.getPandoraStations().get(0);
        Pandora.songsFromStation = new PlaylistDownloader().getPlaylistForStation(Pandora.selectedStation, Pandora.pandoraAccount, Pandora.pandoraSettings.getDefaultAudio());
        Pandora.pandoraPlayer = new PandoraPlayer((ImagePanel) albumArtPanel, (SongDetailsPane) trackDetailsField, musicProgressBar);
    }

    private void populateStationList() {
        stationListBox = new JComboBox(Pandora.pandoraAccount.getPandoraStations().toArray());
    }

    private void addComponentsToBox(Box horizontalBox, List<JComponent> components) {
        for (JComponent component : components) {
            horizontalBox.add(component);
        }
    }

    private JButton buildHelpButton() throws IOException {
        JButton helpButton = buildActionItemButton("Help", "help.png");
        helpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            }
        });
        return helpButton;
    }

    private JButton buildSongInfoButton() throws IOException {
        JButton songInfoButton = buildActionItemButton("More Information About Currently Playing Song", "info.png");
        songInfoButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFrame frame = new JFrame("jPianoBar - Song Info");
                frame.setContentPane(new SongInfoWindow(Pandora.currentlyPlayingSong).getMainPanel());
                frame.pack();
                frame.setVisible(true);
            }
        });
        return songInfoButton;
    }

    private JButton buildLikeButton() throws IOException {
        JButton likeButton = buildActionItemButton("Like Currently Playing Song", "like.png");
        likeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    new PandoraRater().rate(Pandora.pandoraAccount, Pandora.selectedStation, Pandora.currentlyPlayingSong, true);
                    DialogBuilder.showMessage("Success!", "You've liked this song!");
                } catch (XMLRPCException e) {
                    ErrorDialogBuilder.showErrorDialog("Error", e);
                }
            }
        });
        return likeButton;
    }

    private JButton buildDislikeButton() throws IOException {
        JButton dislikeButton = buildActionItemButton("Dislikes Currently Playing Song", "dislike.png");
        dislikeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    new PandoraRater().rate(Pandora.pandoraAccount, Pandora.selectedStation, Pandora.currentlyPlayingSong, false);
                    DialogBuilder.showMessage("Success!", "You've disliked this song!");
                } catch (XMLRPCException e) {
                    ErrorDialogBuilder.showErrorDialog("Error", e);
                }
            }
        });
        return dislikeButton;
    }

    private JButton buildSettingsButton() throws IOException {
        JButton settingsButton = buildActionItemButton("Settings", "settings.png");
        settingsButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFrame frame = new JFrame("jPianoBar - Main");
                frame.setContentPane(new SettingsWindow().getMainPanel());
                frame.pack();
                frame.setVisible(true);
            }
        });
        return settingsButton;
    }

    private JButton buildAddStationButton() throws IOException {
        JButton addStationButton = buildActionItemButton("Add Station", "addStation.png");
        addStationButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFrame frame = new JFrame("jPianoBar - Add Station");
                frame.setContentPane(new AddStationWindow().getMainPanel());
                frame.pack();
                frame.setVisible(true);
            }
        });
        return addStationButton;
    }

    private JButton buildDownloadButton() throws IOException {
        JButton downloadButton = buildActionItemButton("Download Current Song", "download.png");
        downloadButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser downloadFileChooser = new JFileChooser(new File("/"));
                if (downloadFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File saveToFile = downloadFileChooser.getSelectedFile();
                    try {
                        FileUtils.copyFile(new File("tempSongFile.aac"), saveToFile);
                    } catch (IOException e) {
                        ErrorDialogBuilder.showErrorDialog("Failed To Download File!", e);
                    }
                }
            }
        });
        return downloadButton;
    }

    private JButton buildSkipButton(final JButton playPauseButton) throws IOException {
        final JButton skipButton = buildActionItemButton("Skip Current Song", "skip.png");
        skipButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                Thread changeSongThread = new Thread(new Runnable() {

                    public void run() {
                        try {
                            playPauseButton.setIcon(new ImageIcon(ImageIO.read(ClassLoader.getSystemResourceAsStream("pause.png")).getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
                            Pandora.pandoraPlayer.playSong(Pandora.songsFromStation.get(0));
                        } catch (Exception e) {
                            ErrorDialogBuilder.showErrorDialog("Error", e);
                        }
                    }
                });
                changeSongThread.start();
            }
        });
        return skipButton;
    }

    private JButton buildPlayPauseButton() throws IOException {
        final JButton playPauseButton = buildActionItemButton("Pause", "pause.png");
        playPauseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                if (playPauseButton.getToolTipText().equalsIgnoreCase("Play")) {
                    try {
                        Pandora.pandoraPlayer.playSong(Pandora.currentlyPlayingSong);
                        playPauseButton.setToolTipText("Pause");
                        playPauseButton.setIcon(new ImageIcon(ImageIO.read(ClassLoader.getSystemResourceAsStream("pause.png")).getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Pandora.pandoraPlayer.pauseSong();
                        playPauseButton.setToolTipText("Play");
                        playPauseButton.setIcon(new ImageIcon(ImageIO.read(ClassLoader.getSystemResourceAsStream("play.png")).getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
        return playPauseButton;
    }

    public JButton buildActionItemButton(String tooltipText, String resource) throws IOException {
        JButton actionItemButton = new JButton();
        actionItemButton.setToolTipText(tooltipText);
        actionItemButton.setOpaque(true);
        actionItemButton.setBackground(mainControlsPanel.getBackground());
        actionItemButton.setBorder(BorderFactory.createEmptyBorder());
        actionItemButton.setContentAreaFilled(false);
        actionItemButton.setIcon(new ImageIcon(ImageIO.read(ClassLoader.getSystemResourceAsStream(resource)).getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
        return actionItemButton;
    }

    public static void setStatus(String status) {
        LOG.log(Level.WARNING, status);
        statusLabel.setText(status);
    }

    private void addVolumeChangeListener() {
        volumeSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                Pandora.volume = volumeSlider.getValue();
            }
        });
    }

    private void addStationListBoxListener() {
        stationListBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                Thread changeSongThread = new Thread(new Runnable() {

                    public void run() {
                        try {
                            Pandora.selectedStation = Pandora.pandoraAccount.getPandoraStations().get(stationListBox.getSelectedIndex());
                            Pandora.songsFromStation = new PlaylistDownloader().getPlaylistForStation(Pandora.selectedStation, Pandora.pandoraAccount, Pandora.pandoraSettings.getDefaultAudio());
                            Pandora.pandoraPlayer.playSong(Pandora.songsFromStation.get(0));
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE, e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                changeSongThread.start();
            }
        });
    }
}
