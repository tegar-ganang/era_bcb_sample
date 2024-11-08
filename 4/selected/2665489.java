package tico.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import tico.components.resources.SoundFilter;
import tico.components.resources.TFileUtils;
import tico.components.resources.TResourceManager;
import tico.configuration.TLanguage;
import tico.editor.TFileHandler;
import tico.editor.dialogs.TRecordSound;

/**
 * Components to a sound file.
 * 
 * @author Pablo Muñoz
 * @version 1.0 Nov 20, 2006
 */
public class TAnotherSoundChooser extends JPanel {

    private static final String DEFAULT_TITLE = TLanguage.getString("TAnotherSoundChosser.TITLE");

    private String soundFilePath;

    private boolean stopPlayback;

    private SourceDataLine sourceDataLine;

    private AudioFormat audioFormat;

    private AudioInputStream audioInputStream;

    private AudioPlayThread audioPlayThread;

    private JPanel soundNamePane;

    private TTextField soundNameTextField;

    private TButton playSoundButton;

    private TButton stopSoundButton;

    private JPanel buttonPanel;

    private TButton clearSoundButton;

    private TButton selectSoundButton;

    private TButton recordSoundButton;

    private static File defaultDirectory = null;

    /**
	 * Creates a new <code>TSoundChooser</code> with <i>NO_OPTIONS_TYPE</i>
	 * <code>type</code>.
	 */
    public TAnotherSoundChooser() {
        this(DEFAULT_TITLE);
    }

    /**
	 * Creates a new <code>TSoundChooser</code> with the specified
	 * <code>title</code>.
	 * 
	 * @param title The specified <code>title</code>
	 */
    public TAnotherSoundChooser(String title) {
        super();
        setBorder(new TitledBorder(BorderFactory.createEtchedBorder(Color.WHITE, new Color(165, 163, 151)), title));
        createSoundNamePane();
        createButtonPanel();
        updateComponents();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(new GridBagLayout());
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 10, 0, 10);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        add(soundNamePane, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 10, 10, 10);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 1;
        add(buttonPanel, c);
    }

    private void createSoundNamePane() {
        soundNamePane = new JPanel();
        soundNamePane.setLayout(new FlowLayout());
        JLabel textNameLabel = new JLabel(TLanguage.getString("TSoundChooser.NAME"));
        soundNameTextField = new TTextField();
        soundNameTextField.setColumns(20);
        soundNameTextField.setEditable(false);
        playSoundButton = new TButton(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                playSoundFile();
            }
        });
        playSoundButton.setIcon(TResourceManager.getImageIcon("media-start-16.png"));
        playSoundButton.setMargin(new Insets(2, 2, 2, 2));
        playSoundButton.setToolTipText(TLanguage.getString("TSoundChooser.PLAY_TOOLTIP"));
        stopSoundButton = new TButton(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                stopSoundFile();
            }
        });
        stopSoundButton.setIcon(TResourceManager.getImageIcon("media-stop-16.png"));
        stopSoundButton.setMargin(new Insets(2, 2, 2, 2));
        stopSoundButton.setEnabled(false);
        stopSoundButton.setToolTipText(TLanguage.getString("TSoundChooser.STOP_TOOLTIP"));
        soundNamePane.add(textNameLabel);
        soundNamePane.add(soundNameTextField);
        soundNamePane.add(playSoundButton);
        soundNamePane.add(stopSoundButton);
    }

    private void createButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        selectSoundButton = new TButton(TLanguage.getString("TSoundChooser.BUTTON_SELECT"));
        selectSoundButton.addActionListener(new ChooseSoundButtonListener());
        recordSoundButton = new TButton(TLanguage.getString("TSoundChooser.RECORD"));
        recordSoundButton.addActionListener(new RecordSoundButtonListener());
        clearSoundButton = new TButton(new AbstractAction(TLanguage.getString("TSoundChooser.BUTTON_CLEAR")) {

            public void actionPerformed(ActionEvent e) {
                setSoundFilePath(null);
            }
        });
        buttonPanel.add(selectSoundButton);
        buttonPanel.add(recordSoundButton);
        buttonPanel.add(clearSoundButton);
    }

    /**
	 * Update all the components. Enables or disables the buttons.
	 */
    public void updateComponents() {
        if (soundFilePath != null) {
            playSoundButton.setEnabled(true);
            clearSoundButton.setEnabled(true);
            soundNameTextField.setText(TFileUtils.getFilename(soundFilePath));
            soundNameTextField.setCaretPosition(0);
        } else {
            playSoundButton.setEnabled(false);
            clearSoundButton.setEnabled(false);
            soundNameTextField.setText("");
        }
    }

    private void playSoundFile() {
        try {
            int sampleSizeInBits = 16;
            int internalBufferSize = AudioSystem.NOT_SPECIFIED;
            File soundFile = new File(soundFilePath);
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            audioFormat = audioInputStream.getFormat();
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, internalBufferSize);
            boolean isSuportedDirectly = AudioSystem.isLineSupported(dataLineInfo);
            if (!isSuportedDirectly) {
                AudioFormat sourceFormat = audioFormat;
                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), sampleSizeInBits, sourceFormat.getChannels(), sourceFormat.getChannels() * (sampleSizeInBits / 8), sourceFormat.getSampleRate(), false);
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                audioFormat = audioInputStream.getFormat();
            }
            dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, internalBufferSize);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(audioFormat, internalBufferSize);
            stopPlayback = false;
            stopSoundButton.setEnabled(true);
            playSoundButton.setEnabled(false);
            clearSoundButton.setEnabled(false);
            selectSoundButton.setEnabled(false);
            audioPlayThread = new AudioPlayThread();
            audioPlayThread.start();
        } catch (UnsupportedAudioFileException e) {
            JOptionPane.showMessageDialog(null, TLanguage.getString("TSoundChooser.INVALID_FORMAT_ERROR"), TLanguage.getString("ERROR") + "!", JOptionPane.ERROR_MESSAGE);
        } catch (LineUnavailableException e) {
            JOptionPane.showMessageDialog(null, TLanguage.getString("TSoundChooser.PLAY_FAILURE_ERROR"), TLanguage.getString("ERROR") + "!", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, TLanguage.getString("TSoundChooser.OPEN_FILE_ERROR"), TLanguage.getString("ERROR") + "!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopSoundFile() {
        stopPlayback = true;
        stopSoundButton.setEnabled(false);
        playSoundButton.setEnabled(true);
        clearSoundButton.setEnabled(true);
        selectSoundButton.setEnabled(true);
    }

    /**
	 * Returns the selected <code>soundFilePath</code>.
	 * 
	 * @return The selected <code>soundFilePath</code>
	 */
    public String getSoundFilePath() {
        return soundFilePath;
    }

    /**
	 * Set the <code>soundFilePath</code>.
	 * 
	 * @param soundFilePath The <code>soundFilePath</code> to set
	 */
    public void setSoundFilePath(String soundFilePath) {
        this.soundFilePath = soundFilePath;
        updateComponents();
    }

    private class RecordSoundButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            TRecordSound capturePlayback = new TRecordSound();
            TRecordSound.text = null;
            capturePlayback.open();
            TDialog f = new TDialog(null, TLanguage.getString("TSoundChooser.RECORD"), true);
            f.setEnabled(true);
            f.getContentPane().add("Center", capturePlayback);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int w = 720;
            int h = 340;
            f.setLocation(screenSize.width / 2 - w / 2, screenSize.height / 2 - h / 2);
            f.setSize(w, h);
            f.setVisible(true);
            if (TRecordSound.text != null) {
                File selectedFile = TRecordSound.text;
                defaultDirectory = selectedFile.getParentFile();
                try {
                    selectedFile = TFileHandler.importFile(selectedFile);
                    setSoundFilePath(selectedFile.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, TLanguage.getString("TSoundChooser.OPEN_FILE_ERROR"), TLanguage.getString("ERROR") + "!", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private class ChooseSoundButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(TLanguage.getString("TSoundChooser.CHOOSE_SOUND"));
            fileChooser.setCurrentDirectory(defaultDirectory);
            fileChooser.addChoosableFileFilter(new SoundFilter());
            fileChooser.setAcceptAllFileFilterUsed(false);
            int returnValue = fileChooser.showOpenDialog((Component) null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                defaultDirectory = selectedFile.getParentFile();
                try {
                    selectedFile = TFileHandler.importFile(selectedFile);
                    setSoundFilePath(selectedFile.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, TLanguage.getString("TSoundChooser.OPEN_FILE_ERROR"), TLanguage.getString("ERROR") + "!", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private class AudioPlayThread extends Thread {

        private static final int EXTERNAL_BUFFER_SIZE = 4096;

        public void run() {
            byte tempBuffer[] = new byte[EXTERNAL_BUFFER_SIZE];
            try {
                sourceDataLine.start();
                int readBytes = 0;
                while ((readBytes != -1) && !stopPlayback) {
                    readBytes = audioInputStream.read(tempBuffer, 0, tempBuffer.length);
                    if (readBytes > 0) sourceDataLine.write(tempBuffer, 0, readBytes);
                }
                sourceDataLine.drain();
                sourceDataLine.close();
                stopSoundFile();
            } catch (Exception e) {
                stopSoundFile();
                JOptionPane.showMessageDialog(null, TLanguage.getString("TSoundChooser.OPEN_FILE_ERROR"), TLanguage.getString("ERROR") + "!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
