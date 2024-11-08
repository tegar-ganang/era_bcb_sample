package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import javax.swing.JTextPane;

/**
 * Dialog that contains audio/video file information.
 * 
 * @author Petri Tuononen
 *
 */
public class CodecInformation extends JDialog {

    private static final long serialVersionUID = -5882743133690066249L;

    private final JPanel contentPanel = new JPanel();

    /**
	 * Create the dialog.
	 */
    public CodecInformation(JFrame mainFrame, String filePath) {
        String codecInfo = getFormattedCodecInformation(filePath);
        if (codecInfo == null) {
            dispose();
        }
        setTitle("Codec information");
        setBounds(100, 100, 679, 271);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));
        {
            JPanel panel = new JPanel();
            contentPanel.add(panel, BorderLayout.CENTER);
            panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            {
                JTextPane txtCodecInfo = new JTextPane();
                txtCodecInfo.setOpaque(false);
                txtCodecInfo.setEditable(false);
                txtCodecInfo.setText(codecInfo);
                panel.add(txtCodecInfo);
            }
        }
        {
            JPanel buttonPane = new JPanel();
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            {
                JButton okButton = new JButton("OK");
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
                okButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        dispose();
                    }
                });
            }
        }
    }

    /**
	 * Get codec information as a formatted String.
	 * 
	 * @return String Codec information. Returns null if file selection is cancelled.
	 */
    private String getFormattedCodecInformation(String filePath) {
        if (filePath == null) {
            return null;
        } else {
            IContainer container = IContainer.make();
            int result = container.open(filePath, IContainer.Type.READ, null);
            if (result < 0) throw new RuntimeException("Failed to open media file");
            int numStreams = container.getNumStreams();
            long duration = container.getDuration();
            double fileSize = container.getFileSize();
            fileSize = fileSize / 1048576;
            fileSize = (double) Math.round(fileSize * 100) / 100;
            long bitRate = container.getBitRate();
            StringBuilder sB = new StringBuilder();
            sB.append("Number of streams: " + numStreams + "\n");
            sB.append("Duration (s): " + duration / 1000000 + "\n");
            sB.append("File Size (MB): " + fileSize + "\n");
            sB.append("Bit Rate: " + bitRate + "\n");
            sB.append("\n");
            for (int i = 0; i < numStreams; i++) {
                IStream stream = container.getStream(i);
                IStreamCoder coder = stream.getStreamCoder();
                sB.append("stream " + i + " ");
                sB.append("type: " + coder.getCodecType() + "; ");
                sB.append("duration: " + stream.getDuration() + "; ");
                sB.append("start time: " + container.getStartTime() + "; ");
                sB.append("timebase: " + stream.getTimeBase().getNumerator() + "," + " " + stream.getTimeBase().getDenominator() + "; ");
                sB.append("coder tb: " + coder.getTimeBase().getNumerator() + "," + " " + coder.getTimeBase().getDenominator() + "; ");
                sB.append("\n\t");
                if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                    sB.append("sample rate: " + coder.getSampleRate() + "; ");
                    sB.append("channels: " + coder.getChannels() + "; ");
                    sB.append("format: " + coder.getSampleFormat() + "; ");
                } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                    sB.append("width: " + coder.getWidth() + "; ");
                    sB.append("height: " + coder.getHeight() + "; ");
                    sB.append("format: " + coder.getPixelType() + "; ");
                    sB.append("frame-rate: " + coder.getFrameRate().getDouble() + "; ");
                }
                sB.append("\n");
            }
            return sB.toString();
        }
    }
}
