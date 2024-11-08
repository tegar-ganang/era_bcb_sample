package de.psychomatic.mp3db.gui.modules;

import org.apache.log4j.Logger;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.psychomatic.mp3db.gui.modules.dnd.CoverTarget;
import de.psychomatic.mp3db.utils.GuiStrings;
import de.psychomatic.mp3db.utils.ImageKeeper;

/**
 * @author Kykal
 */
public class CoverPanel extends JPanel implements ActionListener {

    /**
     * Logger for this class
     */
    private static Logger _log = Logger.getLogger(CoverPanel.class);

    public CoverPanel(String header) {
        _changed = false;
        _data = null;
        setLayout(new FormLayout("3dlu, fill:min, fill:min, fill:min, 3dlu", "3dlu, fill:min, 2dlu, fill:min, 5dlu, fill:min, 3dlu:grow"));
        JLabel title = new JLabel(header);
        Dimension d = new Dimension(120, 120);
        _image = new JLabel(GuiStrings.getString("nopic"));
        _image.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        _image.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        _image.setSize(d);
        _image.setMinimumSize(d);
        _image.setPreferredSize(d);
        _image.setMaximumSize(d);
        DropTarget dt = new DropTarget(_image, new CoverTarget(this));
        _image.setDropTarget(dt);
        dt.setActive(true);
        JButton load = new JButton(new ImageIcon(ImageKeeper.loadImage(CoverPanel.class, "images/open.png")));
        load.setActionCommand("LOAD");
        load.addActionListener(this);
        JButton save = new JButton(new ImageIcon(ImageKeeper.loadImage(CoverPanel.class, "images/save.png")));
        save.setActionCommand("SAVE");
        save.addActionListener(this);
        JButton del = new JButton(new ImageIcon(ImageKeeper.loadImage(CoverPanel.class, "images/remove.png")));
        del.setActionCommand("DELETE");
        del.addActionListener(this);
        CellConstraints cc = new CellConstraints();
        add(title, cc.xyw(2, 2, 3));
        add(_image, cc.xyw(2, 4, 3, CellConstraints.CENTER, CellConstraints.CENTER));
        add(load, cc.xy(2, 6));
        add(save, cc.xy(3, 6));
        add(del, cc.xy(4, 6));
    }

    public void setImage(byte[] data) {
        _data = data;
        ImageIcon pre = new ImageIcon(data);
        double w = pre.getIconWidth() / ((double) pre.getIconWidth() / (double) _image.getWidth());
        double h = pre.getIconHeight() / ((double) pre.getIconWidth() / (double) _image.getWidth());
        if (h > _image.getHeight()) {
            w = pre.getIconWidth() / ((double) pre.getIconHeight() / (double) _image.getHeight());
            h = pre.getIconHeight() / ((double) pre.getIconHeight() / (double) _image.getHeight());
        }
        ImageIcon icon = new ImageIcon(pre.getImage().getScaledInstance((int) w, (int) h, Image.SCALE_SMOOTH));
        _image.setText("");
        _image.setIcon(icon);
    }

    public byte[] getImageData() {
        return _data;
    }

    public void setLoadedImage(byte[] data) {
        _changed = true;
        setImage(data);
    }

    public void removeImage() {
        _image.setIcon(null);
        _image.setText(GuiStrings.getString("nopic"));
        _data = null;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("LOAD")) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new JPEGFilter());
            chooser.setMultiSelectionEnabled(false);
            if (chooser.showOpenDialog(getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = chooser.getSelectedFile();
                    BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
                    int read = is.read();
                    while (read != -1) {
                        bos.write(read);
                        read = is.read();
                    }
                    is.close();
                    _changed = true;
                    setImage(bos.toByteArray());
                } catch (Exception e1) {
                    _log.error("actionPerformed(ActionEvent)", e1);
                }
            }
        } else if (e.getActionCommand().equals("SAVE")) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new JPEGFilter());
            chooser.setMultiSelectionEnabled(false);
            if (_data != null && chooser.showSaveDialog(getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = chooser.getSelectedFile();
                    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                    os.write(_data);
                    os.flush();
                    os.close();
                } catch (Exception e1) {
                    _log.error("actionPerformed(ActionEvent)", e1);
                }
            }
        } else if (e.getActionCommand().equals("DELETE")) {
            if (_data != null) {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(), GuiStrings.getString("message.removeimg"), GuiStrings.getString("title.confirm"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    removeImage();
                    _changed = true;
                }
            }
        }
    }

    public boolean isChanged() {
        return _changed;
    }

    private JLabel _image;

    private byte[] _data;

    private boolean _changed;
}
