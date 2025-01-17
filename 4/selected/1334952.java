package javazoom.jlgui.player.amp.tag.ui;

import java.text.DecimalFormat;
import javax.swing.JFrame;
import javazoom.jlgui.player.amp.tag.APEInfo;

/**
 * APEDialog class implements a DialogBox to diplay APE info.
 */
public class APEDialog extends TagInfoDialog {

    private APEInfo _apeinfo = null;

    /**
     * Creates new form ApeDialog
     */
    public APEDialog(JFrame parent, String title, APEInfo mi) {
        super(parent, title);
        initComponents();
        _apeinfo = mi;
        int size = _apeinfo.getLocation().length();
        locationLabel.setText(size > 50 ? ("..." + _apeinfo.getLocation().substring(size - 50)) : _apeinfo.getLocation());
        if ((_apeinfo.getTitle() != null) && (!_apeinfo.getTitle().equals(""))) textField.append("Title=" + _apeinfo.getTitle() + "\n");
        if ((_apeinfo.getArtist() != null) && (!_apeinfo.getArtist().equals(""))) textField.append("Artist=" + _apeinfo.getArtist() + "\n");
        if ((_apeinfo.getAlbum() != null) && (!_apeinfo.getAlbum().equals(""))) textField.append("Album=" + _apeinfo.getAlbum() + "\n");
        if (_apeinfo.getTrack() > 0) textField.append("Track=" + _apeinfo.getTrack() + "\n");
        if ((_apeinfo.getYear() != null) && (!_apeinfo.getYear().equals(""))) textField.append("Year=" + _apeinfo.getYear() + "\n");
        if ((_apeinfo.getGenre() != null) && (!_apeinfo.getGenre().equals(""))) textField.append("Genre=" + _apeinfo.getGenre() + "\n");
        java.util.List comments = _apeinfo.getComment();
        if (comments != null) {
            for (int i = 0; i < comments.size(); i++) textField.append(comments.get(i) + "\n");
        }
        int secondsAmount = Math.round(_apeinfo.getPlayTime());
        if (secondsAmount < 0) secondsAmount = 0;
        int minutes = secondsAmount / 60;
        int seconds = secondsAmount - (minutes * 60);
        lengthLabel.setText("Length : " + minutes + ":" + seconds);
        DecimalFormat df = new DecimalFormat("#,###,###");
        sizeLabel.setText("Size : " + df.format(_apeinfo.getSize()) + " bytes");
        versionLabel.setText("Version: " + df.format(_apeinfo.getVersion()));
        compressionLabel.setText("Compression: " + _apeinfo.getCompressionlevel());
        channelsLabel.setText("Channels: " + _apeinfo.getChannels());
        bitspersampleLabel.setText("Bits Per Sample: " + _apeinfo.getBitsPerSample());
        bitrateLabel.setText("Average Bitrate: " + (_apeinfo.getBitRate() / 1000) + " kbps");
        samplerateLabel.setText("Sample Rate: " + _apeinfo.getSamplingRate() + " Hz");
        peaklevelLabel.setText("Peak Level: " + (_apeinfo.getPeaklevel() > 0 ? String.valueOf(_apeinfo.getPeaklevel()) : ""));
        copyrightLabel.setText("Copyrighted: " + (_apeinfo.getCopyright() != null ? _apeinfo.getCopyright() : ""));
        buttonsPanel.add(_close);
        pack();
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
        compressionLabel = new javax.swing.JLabel();
        channelsLabel = new javax.swing.JLabel();
        bitspersampleLabel = new javax.swing.JLabel();
        bitrateLabel = new javax.swing.JLabel();
        samplerateLabel = new javax.swing.JLabel();
        peaklevelLabel = new javax.swing.JLabel();
        copyrightLabel = new javax.swing.JLabel();
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
        jPanel2.add(sizeLabel);
        jPanel2.add(versionLabel);
        jPanel2.add(compressionLabel);
        jPanel2.add(channelsLabel);
        jPanel2.add(bitspersampleLabel);
        jPanel2.add(bitrateLabel);
        jPanel2.add(samplerateLabel);
        jPanel2.add(peaklevelLabel);
        jPanel2.add(copyrightLabel);
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

    private javax.swing.JLabel bitspersampleLabel;

    private javax.swing.JPanel buttonsPanel;

    private javax.swing.JLabel channelsLabel;

    private javax.swing.JLabel compressionLabel;

    private javax.swing.JLabel copyrightLabel;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JLabel lengthLabel;

    private javax.swing.JLabel locationLabel;

    private javax.swing.JLabel peaklevelLabel;

    private javax.swing.JLabel samplerateLabel;

    private javax.swing.JLabel sizeLabel;

    private javax.swing.JTextArea textField;

    private javax.swing.JLabel versionLabel;
}
