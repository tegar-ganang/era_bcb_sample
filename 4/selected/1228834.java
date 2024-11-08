package strudle.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class SoundFrame extends JFrame2 {

    private JPanel jPanel = null;

    private JPanel jInfoPanel = null;

    private SoundViewerComponent soundViewerComponent = null;

    private JLabel jLabel = null;

    private JTextField jSampleRate = null;

    private JTextField jChannels = null;

    private JLabel jLabel1 = null;

    private JLabel jLabel2 = null;

    private JTextField jFilename = null;

    private JTextField jDuration = null;

    private JLabel jLabel3 = null;

    public SoundFrame() {
        super();
        initialize();
        setLocation(CenterType.ON_SCREEN);
        try {
            soundViewerComponent.openAudioFile("test/8bit/test2ch.wav");
            jFilename.setText(soundViewerComponent.getFilename());
            jDuration.setText(Double.toString(soundViewerComponent.getDuration()));
            jChannels.setText(Integer.toString(soundViewerComponent.getChannels()));
            jSampleRate.setText(Double.toString(soundViewerComponent.getSampleRate()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initialize() {
        this.setSize(new Dimension(811, 373));
        this.setMinimumSize(new Dimension(400, 300));
        this.setContentPane(getJPanel());
        this.setName("SoundTest");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Sound Test");
    }

    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.setLayout(new BorderLayout());
            jPanel.setPreferredSize(new Dimension(100, 80));
            jPanel.add(getJInfoPanel(), BorderLayout.SOUTH);
            jPanel.add(getSoundViewerComponent(), BorderLayout.CENTER);
        }
        return jPanel;
    }

    private JPanel getJInfoPanel() {
        if (jInfoPanel == null) {
            jLabel3 = new JLabel();
            jLabel3.setBounds(new Rectangle(14, 54, 91, 21));
            jLabel3.setText("Duration [sec]:");
            jLabel2 = new JLabel();
            jLabel2.setBounds(new Rectangle(384, 54, 100, 21));
            jLabel2.setText("Sample Rate [Hz]:");
            jLabel1 = new JLabel();
            jLabel1.setBounds(new Rectangle(385, 25, 61, 21));
            jLabel1.setText("Channels:");
            jLabel = new JLabel();
            jLabel.setBounds(new Rectangle(14, 24, 71, 21));
            jLabel.setText("File Name:");
            TitledBorder titledBorder = BorderFactory.createTitledBorder(BorderFactory.createCompoundBorder(null, null), "Properties", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51));
            titledBorder.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SystemColor.controlShadow, 1), BorderFactory.createLineBorder(Color.white, 1)));
            jInfoPanel = new JPanel();
            jInfoPanel.setLayout(null);
            jInfoPanel.setPreferredSize(new Dimension(100, 100));
            jInfoPanel.setBorder(titledBorder);
            jInfoPanel.add(jLabel, null);
            jInfoPanel.add(getJSampleRate(), null);
            jInfoPanel.add(getJChannels(), null);
            jInfoPanel.add(jLabel1, null);
            jInfoPanel.add(jLabel2, null);
            jInfoPanel.add(getJFilename(), null);
            jInfoPanel.add(getJDuration(), null);
            jInfoPanel.add(jLabel3, null);
        }
        return jInfoPanel;
    }

    private SoundViewerComponent getSoundViewerComponent() {
        if (soundViewerComponent == null) {
            soundViewerComponent = new SoundViewerComponent();
            soundViewerComponent.setShowScrollbar(true);
            soundViewerComponent.setShowRuler(true);
            soundViewerComponent.setShowScrollbar(true);
        }
        return soundViewerComponent;
    }

    private JTextField getJSampleRate() {
        if (jSampleRate == null) {
            jSampleRate = new JTextField();
            jSampleRate.setBounds(new Rectangle(484, 54, 183, 21));
        }
        return jSampleRate;
    }

    private JTextField getJChannels() {
        if (jChannels == null) {
            jChannels = new JTextField();
            jChannels.setBounds(new Rectangle(484, 24, 101, 21));
        }
        return jChannels;
    }

    /**
	 * This method initializes jFilename
	 * 
	 * @return javax.swing.JTextField
	 */
    private JTextField getJFilename() {
        if (jFilename == null) {
            jFilename = new JTextField();
            jFilename.setBounds(new Rectangle(104, 24, 251, 22));
        }
        return jFilename;
    }

    /**
	 * This method initializes jDuration
	 * 
	 * @return javax.swing.JTextField
	 */
    private JTextField getJDuration() {
        if (jDuration == null) {
            jDuration = new JTextField();
            jDuration.setBounds(new Rectangle(104, 54, 191, 21));
        }
        return jDuration;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
        }
        new SoundFrame().setVisible(true);
    }
}
