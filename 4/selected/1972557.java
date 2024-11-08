package at.fhj.utils.graphics.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;
import at.fhj.utils.misc.FileUtils;

/**
 * This class provides means to export an image to a file.
 * 
 * @author Ilya Boyandin
 *
 * $Revision: 1.1 $
 */
public class ImageExporter {

    private static final FileFilter pngFilter = new ImageFilter("png", "PNG Image");

    private static final FileFilter jpgFilter = new ImageFilter("jpg", "JPEG Image");

    private static final Font exportImgTitleFont = new Font("Dialog", Font.BOLD, 12);

    public ImageExporter() {
    }

    /**
   * First asks the painter to paint the image, then writes
   * it to the dest file. The output format is determined
   * from the dest file extension.
   *  
   * @param title
   * @param painter
   * @param dest
   * @throws IOException
   */
    public void exportImageToFile(String title, ImagePainter painter, File dest) throws IOException {
        final int headerSize = 25;
        Dimension ps = painter.getSize();
        BufferedImage bi = new BufferedImage(ps.width, ps.height + headerSize, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, ps.width, headerSize);
        g.setColor(Color.white);
        g.fillRect(0, headerSize, ps.width, ps.height);
        if (title != null) {
            g.setColor(Color.black);
            g.setFont(exportImgTitleFont);
            g.drawString(title, 10, exportImgTitleFont.getSize() + 7);
        }
        painter.paintImage(g, 0, headerSize);
        ImageIO.write(bi, FileUtils.getExtension(dest.getName()), dest);
    }

    /**
   * Shows a "Save Image As" filechooser dialog to get from the
   * user the output file name.
   * 
   * @param parent
   * @param suggestedPath
   * @param suggestedName
   * @return
   */
    public File showFileDialog(final Component parent, final String suggestedPath, String suggestedName) {
        final JFileChooser fc = new JFileChooser() {

            public void setFileFilter(FileFilter filter) {
                final File sel = getSelectedFile();
                if (filter instanceof ImageFilter && (sel == null || !filter.accept(sel))) {
                    String name = null;
                    String path = null;
                    FileChooserUI ui = getUI();
                    if (ui instanceof BasicFileChooserUI) {
                        BasicFileChooserUI bui = (BasicFileChooserUI) getUI();
                        name = bui.getFileName();
                        path = getCurrentDirectory().getAbsolutePath();
                    } else if (sel != null) {
                        name = sel.getName();
                        path = sel.getParent();
                    }
                    if (name != null) {
                        name = FileUtils.cutOffExtension(name);
                        if (name.length() > 0) {
                            ImageFilter imf = (ImageFilter) filter;
                            final String newName = path + File.separator + name + imf.getExtension();
                            EventQueue.invokeLater(new Runnable() {

                                public void run() {
                                    setSelectedFile(new File(newName));
                                }
                            });
                        }
                    }
                }
                super.setFileFilter(filter);
            }

            public void approveSelection() {
                final File selFile = getSelectedFile();
                if (selFile.exists()) {
                    int overwrite = JOptionPane.showConfirmDialog(parent, "File '" + selFile.getName() + "' already exists.\n" + "Would you like to overwrite it?", "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (overwrite != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                super.approveSelection();
            }
        };
        fc.setMultiSelectionEnabled(false);
        if (suggestedPath != null) {
            fc.setCurrentDirectory(new File(suggestedPath));
        }
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(jpgFilter);
        fc.addChoosableFileFilter(pngFilter);
        if (suggestedName == null) {
            suggestedName = "exported";
        }
        String ext = ((ImageFilter) fc.getFileFilter()).getExtension();
        fc.setSelectedFile(new File(suggestedPath + File.separator + suggestedName + ext));
        fc.setApproveButtonText("Save Image");
        int confirm = fc.showDialog(parent, "Save Image");
        if (confirm == JFileChooser.APPROVE_OPTION) {
            File selFile = fc.getSelectedFile();
            if (selFile != null) {
                final String selName = selFile.getName();
                String selExt = FileUtils.getExtension(selName);
                if ("".equals(selExt)) {
                    final String selPath = selFile.getParent();
                    selExt = ((ImageFilter) fc.getFileFilter()).getExtension();
                    selFile = new File(selPath + File.separator + selName + selExt);
                }
            }
            return selFile;
        } else {
            return null;
        }
    }

    private static class ImageFilter extends FileFilter {

        private String ext;

        private String descr;

        public ImageFilter(String type, String descr) {
            this.ext = "." + type;
            this.descr = "*" + ext + " (" + descr + ")";
        }

        public String getExtension() {
            return ext;
        }

        public boolean accept(File f) {
            return (f.isDirectory() || f.getName().endsWith(ext));
        }

        public String getDescription() {
            return descr;
        }
    }
}
