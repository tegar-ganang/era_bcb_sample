package com.gite.application.chat.smileys;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import com.gite.core.Utils;

public class SmileysButton extends JButton implements ActionListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 3564105031646990203L;

    private JTextPane jTextPane;

    private LabelPopupMenu lpm;

    SmileysButton(JTextPane tp) {
        addActionListener(this);
        setText("Smileys");
        jTextPane = tp;
        lpm = new LabelPopupMenu();
    }

    public void actionPerformed(ActionEvent e) {
        SmileysChooser sc = new SmileysChooser("Send heart");
        jTextPane.requestFocus();
        centerOnScreen(sc);
        int result = sc.showDialog();
        if (result == SmileysChooser.INDEX_OPTION) {
            ImageIcon ii = sc.getImageList().get(SmileysChooser.INDEX);
            JLabel lb = new JLabel(ii);
            lb.addMouseListener(new LabelMouseListener(lb, ii));
            StyledDocument doc = jTextPane.getStyledDocument();
            Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
            Style s = doc.addStyle(ii.toString(), def);
            StyleConstants.setComponent(s, lb);
            try {
                doc.insertString(doc.getLength(), ii.toString(), s);
                ii.setImageObserver(null);
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
    }

    private static void centerOnScreen(Window window) {
        window.setLocationRelativeTo(null);
    }

    class LabelMouseListener implements MouseListener {

        private ImageIcon ii;

        private JLabel lb;

        LabelMouseListener(JLabel lb, ImageIcon ii) {
            this.ii = ii;
            this.lb = lb;
        }

        public void mouseClicked(MouseEvent e) {
            lpm.setImage(ii);
            lpm.show(e.getComponent(), e.getX(), e.getY());
        }

        public void mouseEntered(MouseEvent e) {
            Cursor hourglassCursor = new Cursor(Cursor.HAND_CURSOR);
            lb.setCursor(hourglassCursor);
        }

        public void mouseExited(MouseEvent e) {
            lb.setCursor(null);
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
    }

    class LabelPopupMenu extends JPopupMenu {

        /**
		 * 
		 */
        private static final long serialVersionUID = 5557834762925093210L;

        private ImageIcon ii;

        LabelPopupMenu() {
            JMenuItem menuItem = new JMenuItem("Add to collection (requires permission");
            this.add(menuItem);
            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                }
            });
            menuItem = new JMenuItem("Save to disk as JPG");
            this.add(menuItem);
            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = Utils.initFileChooser();
                    int returnVal = fc.showDialog(SmileysButton.this, "Save");
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        boolean write = false;
                        if (fc.getSelectedFile().isFile()) {
                            if (JOptionPane.showConfirmDialog(null, "The file you selected already exists!\n" + "Do you want to overwrite?", "Overwrite", JOptionPane.YES_NO_OPTION) == 0) {
                                write = true;
                            }
                        } else {
                            write = true;
                        }
                        if (write) {
                            int w = ii.getIconWidth();
                            int h = ii.getIconHeight();
                            BufferedImage save = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                            Graphics2D g2 = save.createGraphics();
                            ii.paintIcon(null, g2, 0, 0);
                            g2.dispose();
                            try {
                                ImageIO.write(save, "jpg", new File(fc.getSelectedFile().getCanonicalPath()));
                                JOptionPane.showMessageDialog(null, "Saved...");
                            } catch (IOException ioe) {
                                System.err.println("write: " + ioe.getMessage());
                                JOptionPane.showMessageDialog(null, "RUN TO THE WOODS!!!", "Saving failed", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            });
        }

        public void setImage(ImageIcon ii) {
            this.ii = ii;
        }
    }
}
