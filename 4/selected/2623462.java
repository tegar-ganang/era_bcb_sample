package javazoom.jlgui.player.amp.tag.ui;

import java.text.DecimalFormat;
import javax.swing.JFrame;
import javazoom.jlgui.player.amp.tag.FlacInfo;

/**
 * FlacDialog class implements a DialogBox to diplay Flac info.
 */
public class FlacDialog extends TagInfoDialog {

    private FlacInfo _flacinfo = null;

    /**
     * Creates new form FlacDialog
     */
    public FlacDialog(JFrame parent, String title, FlacInfo mi) {
        super(parent, title);
        initComponents();
        _flacinfo = mi;
        int size = _flacinfo.getLocation().length();
        locationLabel.setText(size > 50 ? ("..." + _flacinfo.getLocation().substring(size - 50)) : _flacinfo.getLocation());
        if ((_flacinfo.getTitle() != null) && (!_flacinfo.getTitle().equals(""))) textField.append("Title=" + _flacinfo.getTitle() + "\n");
        if ((_flacinfo.getArtist() != null) && (!_flacinfo.getArtist().equals(""))) textField.append("Artist=" + _flacinfo.getArtist() + "\n");
        if ((_flacinfo.getAlbum() != null) && (!_flacinfo.getAlbum().equals(""))) textField.append("Album=" + _flacinfo.getAlbum() + "\n");
        if (_flacinfo.getTrack() > 0) textField.append("Track=" + _flacinfo.getTrack() + "\n");
        if ((_flacinfo.getYear() != null) && (!_flacinfo.getYear().equals(""))) textField.append("Year=" + _flacinfo.getYear() + "\n");
        if ((_flacinfo.getGenre() != null) && (!_flacinfo.getGenre().equals(""))) textField.append("Genre=" + _flacinfo.getGenre() + "\n");
        java.util.List comments = _flacinfo.getComment();
        if (comments != null) {
            for (int i = 0; i < comments.size(); i++) textField.append(comments.get(i) + "\n");
        }
        DecimalFormat df = new DecimalFormat("#,###,###");
        sizeLabel.setText("Size : " + df.format(_flacinfo.getSize()) + " bytes");
        channelsLabel.setText("Channels: " + _flacinfo.getChannels());
        bitspersampleLabel.setText("Bits Per Sample: " + _flacinfo.getBitsPerSample());
        samplerateLabel.setText("Sample Rate: " + _flacinfo.getSamplingRate() + " Hz");
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
        channelsLabel = new javax.swing.JLabel();
        bitspersampleLabel = new javax.swing.JLabel();
        bitrateLabel = new javax.swing.JLabel();
        samplerateLabel = new javax.swing.JLabel();
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
        jPanel2.add(channelsLabel);
        jPanel2.add(bitspersampleLabel);
        jPanel2.add(bitrateLabel);
        jPanel2.add(samplerateLabel);
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

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JLabel lengthLabel;

    private javax.swing.JLabel locationLabel;

    private javax.swing.JLabel samplerateLabel;

    private javax.swing.JLabel sizeLabel;

    private javax.swing.JTextArea textField;
}
