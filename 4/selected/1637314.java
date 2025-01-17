package javazoom.jlgui.player.amp.tag.ui;

import java.text.DecimalFormat;
import javax.swing.JFrame;
import javazoom.jlgui.player.amp.tag.OggVorbisInfo;

/**
 * OggVorbisDialog class implements a DialogBox to diplay OggVorbis info.
 */
public class OggVorbisDialog extends TagInfoDialog {

    private OggVorbisInfo _vorbisinfo = null;

    /**
     * Creates new form MpegDialog
     */
    public OggVorbisDialog(JFrame parent, String title, OggVorbisInfo mi) {
        super(parent, title);
        initComponents();
        _vorbisinfo = mi;
        int size = _vorbisinfo.getLocation().length();
        locationLabel.setText(size > 50 ? ("..." + _vorbisinfo.getLocation().substring(size - 50)) : _vorbisinfo.getLocation());
        if ((_vorbisinfo.getTitle() != null) && ((!_vorbisinfo.getTitle().equals("")))) textField.append("Title=" + _vorbisinfo.getTitle() + "\n");
        if ((_vorbisinfo.getArtist() != null) && ((!_vorbisinfo.getArtist().equals("")))) textField.append("Artist=" + _vorbisinfo.getArtist() + "\n");
        if ((_vorbisinfo.getAlbum() != null) && ((!_vorbisinfo.getAlbum().equals("")))) textField.append("Album=" + _vorbisinfo.getAlbum() + "\n");
        if (_vorbisinfo.getTrack() > 0) textField.append("Track=" + _vorbisinfo.getTrack() + "\n");
        if ((_vorbisinfo.getYear() != null) && ((!_vorbisinfo.getYear().equals("")))) textField.append("Year=" + _vorbisinfo.getYear() + "\n");
        if ((_vorbisinfo.getGenre() != null) && ((!_vorbisinfo.getGenre().equals("")))) textField.append("Genre=" + _vorbisinfo.getGenre() + "\n");
        java.util.List comments = _vorbisinfo.getComment();
        for (int i = 0; i < comments.size(); i++) textField.append(comments.get(i) + "\n");
        int secondsAmount = Math.round(_vorbisinfo.getPlayTime());
        if (secondsAmount < 0) secondsAmount = 0;
        int minutes = secondsAmount / 60;
        int seconds = secondsAmount - (minutes * 60);
        lengthLabel.setText("Length : " + minutes + ":" + seconds);
        bitrateLabel.setText("Average bitrate : " + _vorbisinfo.getAverageBitrate() / 1000 + " kbps");
        DecimalFormat df = new DecimalFormat("#,###,###");
        sizeLabel.setText("File size : " + df.format(_vorbisinfo.getSize()) + " bytes");
        nominalbitrateLabel.setText("Nominal bitrate : " + (_vorbisinfo.getBitRate() / 1000) + " kbps");
        maxbitrateLabel.setText("Max bitrate : " + _vorbisinfo.getMaxBitrate() / 1000 + " kbps");
        minbitrateLabel.setText("Min bitrate : " + _vorbisinfo.getMinBitrate() / 1000 + " kbps");
        channelsLabel.setText("Channel : " + _vorbisinfo.getChannels());
        samplerateLabel.setText("Sampling rate : " + _vorbisinfo.getSamplingRate() + " Hz");
        serialnumberLabel.setText("Serial number : " + _vorbisinfo.getSerial());
        versionLabel.setText("Version : " + _vorbisinfo.getVersion());
        vendorLabel.setText("Vendor : " + _vorbisinfo.getVendor());
        buttonsPanel.add(_close);
        pack();
    }

    /**
     * Returns VorbisInfo.
     */
    public OggVorbisInfo getOggVorbisInfo() {
        return _vorbisinfo;
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jPanel3 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        locationLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textField = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        lengthLabel = new javax.swing.JLabel();
        bitrateLabel = new javax.swing.JLabel();
        sizeLabel = new javax.swing.JLabel();
        nominalbitrateLabel = new javax.swing.JLabel();
        maxbitrateLabel = new javax.swing.JLabel();
        minbitrateLabel = new javax.swing.JLabel();
        channelsLabel = new javax.swing.JLabel();
        samplerateLabel = new javax.swing.JLabel();
        serialnumberLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        vendorLabel = new javax.swing.JLabel();
        buttonsPanel = new javax.swing.JPanel();
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        jPanel3.setLayout(new java.awt.GridBagLayout());
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        jLabel1.setText("File/URL :");
        jPanel1.add(jLabel1);
        jPanel1.add(locationLabel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jPanel1, gridBagConstraints);
        jLabel2.setText("Standard Tags");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jLabel2, gridBagConstraints);
        jLabel3.setText("File/Stream info");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jLabel3, gridBagConstraints);
        textField.setColumns(20);
        textField.setRows(10);
        jScrollPane1.setViewportView(textField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jScrollPane1, gridBagConstraints);
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));
        jPanel2.add(lengthLabel);
        jPanel2.add(bitrateLabel);
        jPanel2.add(sizeLabel);
        jPanel2.add(nominalbitrateLabel);
        jPanel2.add(maxbitrateLabel);
        jPanel2.add(minbitrateLabel);
        jPanel2.add(channelsLabel);
        jPanel2.add(samplerateLabel);
        jPanel2.add(serialnumberLabel);
        jPanel2.add(versionLabel);
        jPanel2.add(vendorLabel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jPanel2, gridBagConstraints);
        getContentPane().add(jPanel3);
        getContentPane().add(buttonsPanel);
    }

    private javax.swing.JLabel bitrateLabel;

    private javax.swing.JPanel buttonsPanel;

    private javax.swing.JLabel channelsLabel;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JLabel lengthLabel;

    private javax.swing.JLabel locationLabel;

    private javax.swing.JLabel maxbitrateLabel;

    private javax.swing.JLabel minbitrateLabel;

    private javax.swing.JLabel nominalbitrateLabel;

    private javax.swing.JLabel samplerateLabel;

    private javax.swing.JLabel serialnumberLabel;

    private javax.swing.JLabel sizeLabel;

    private javax.swing.JTextArea textField;

    private javax.swing.JLabel vendorLabel;

    private javax.swing.JLabel versionLabel;
}
