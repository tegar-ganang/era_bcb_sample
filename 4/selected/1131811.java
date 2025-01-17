package javazoom.jlgui.player.amp.tag.ui;

import java.text.DecimalFormat;
import javax.swing.JFrame;
import javazoom.jlgui.player.amp.tag.MpegInfo;

/**
 * OggVorbisDialog class implements a DialogBox to diplay OggVorbis info.
 */
public class MpegDialog extends TagInfoDialog {

    private MpegInfo _mpeginfo = null;

    /**
     * Creates new form MpegDialog
     */
    public MpegDialog(JFrame parent, String title, MpegInfo mi) {
        super(parent, title);
        initComponents();
        _mpeginfo = mi;
        int size = _mpeginfo.getLocation().length();
        locationLabel.setText(size > 50 ? ("..." + _mpeginfo.getLocation().substring(size - 50)) : _mpeginfo.getLocation());
        if ((_mpeginfo.getTitle() != null) && ((!_mpeginfo.getTitle().equals("")))) textField.append("Title=" + _mpeginfo.getTitle() + "\n");
        if ((_mpeginfo.getArtist() != null) && ((!_mpeginfo.getArtist().equals("")))) textField.append("Artist=" + _mpeginfo.getArtist() + "\n");
        if ((_mpeginfo.getAlbum() != null) && ((!_mpeginfo.getAlbum().equals("")))) textField.append("Album=" + _mpeginfo.getAlbum() + "\n");
        if (_mpeginfo.getTrack() > 0) textField.append("Track=" + _mpeginfo.getTrack() + "\n");
        if ((_mpeginfo.getYear() != null) && ((!_mpeginfo.getYear().equals("")))) textField.append("Year=" + _mpeginfo.getYear() + "\n");
        if ((_mpeginfo.getGenre() != null) && ((!_mpeginfo.getGenre().equals("")))) textField.append("Genre=" + _mpeginfo.getGenre() + "\n");
        java.util.List comments = _mpeginfo.getComment();
        if (comments != null) {
            for (int i = 0; i < comments.size(); i++) textField.append(comments.get(i) + "\n");
        }
        int secondsAmount = Math.round(_mpeginfo.getPlayTime());
        if (secondsAmount < 0) secondsAmount = 0;
        int minutes = secondsAmount / 60;
        int seconds = secondsAmount - (minutes * 60);
        lengthLabel.setText("Length : " + minutes + ":" + seconds);
        DecimalFormat df = new DecimalFormat("#,###,###");
        sizeLabel.setText("Size : " + df.format(_mpeginfo.getSize()) + " bytes");
        versionLabel.setText(_mpeginfo.getVersion() + " " + _mpeginfo.getLayer());
        bitrateLabel.setText((_mpeginfo.getBitRate() / 1000) + " kbps");
        samplerateLabel.setText(_mpeginfo.getSamplingRate() + " Hz " + _mpeginfo.getChannelsMode());
        vbrLabel.setText("VBR : " + _mpeginfo.getVBR());
        crcLabel.setText("CRCs : " + _mpeginfo.getCRC());
        copyrightLabel.setText("Copyrighted : " + _mpeginfo.getCopyright());
        originalLabel.setText("Original : " + _mpeginfo.getOriginal());
        emphasisLabel.setText("Emphasis : " + _mpeginfo.getEmphasis());
        buttonsPanel.add(_close);
        pack();
    }

    /**
     * Returns VorbisInfo.
     */
    public MpegInfo getOggVorbisInfo() {
        return _mpeginfo;
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
        sizeLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        bitrateLabel = new javax.swing.JLabel();
        samplerateLabel = new javax.swing.JLabel();
        vbrLabel = new javax.swing.JLabel();
        crcLabel = new javax.swing.JLabel();
        copyrightLabel = new javax.swing.JLabel();
        originalLabel = new javax.swing.JLabel();
        emphasisLabel = new javax.swing.JLabel();
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
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jScrollPane1, gridBagConstraints);
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));
        jPanel2.add(lengthLabel);
        jPanel2.add(sizeLabel);
        jPanel2.add(versionLabel);
        jPanel2.add(bitrateLabel);
        jPanel2.add(samplerateLabel);
        jPanel2.add(vbrLabel);
        jPanel2.add(crcLabel);
        jPanel2.add(copyrightLabel);
        jPanel2.add(originalLabel);
        jPanel2.add(emphasisLabel);
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

    private javax.swing.JLabel copyrightLabel;

    private javax.swing.JLabel crcLabel;

    private javax.swing.JLabel emphasisLabel;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JLabel lengthLabel;

    private javax.swing.JLabel locationLabel;

    private javax.swing.JLabel originalLabel;

    private javax.swing.JLabel samplerateLabel;

    private javax.swing.JLabel sizeLabel;

    private javax.swing.JTextArea textField;

    private javax.swing.JLabel vbrLabel;

    private javax.swing.JLabel versionLabel;
}
