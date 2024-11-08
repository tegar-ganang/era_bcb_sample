package main;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.plugin.filter.AVI_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import java.awt.Dimension;
import java.awt.JobAttributes;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import tools.IJTools;
import net.miginfocom.swing.MigLayout;

/**
 * Ez az osztály a képekből Avi-t készít. A felületen megjelenik egy lista, melyből ki lehet választani, hogy mely képekből készítsen AVI-t.
 * @author jojo
 *
 */
public class CreateAvi implements PlugInFilter {

    public CreateAvi() {
        buildGui();
    }

    private void buildGui() {
        m_dlgAvi = new JDialog(m_owner, "Create Avi");
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        m_dlgAvi.setLocation((int) (dim.getWidth() / 2.0), (int) (dim.getHeight() / 2.0));
        m_btCopyTo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ImagePlus ip = null;
                JFileChooser fs = new JFileChooser(OpenDialog.getLastDirectory());
                fs.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fs.showSaveDialog(m_dlgAvi);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    if (m_nFirstIdx != -1 && m_nLastIdx != -1) ip = createNewImagePlus(m_imagePlus, m_nFirstIdx, m_nLastIdx); else ip = m_imagePlus;
                    IJTools.saveImageStack(ip, fs.getSelectedFile().getAbsolutePath());
                }
            }
        });
        m_btCreateAvi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    ImagePlus ip = null;
                    JFileChooser fs = new JFileChooser(OpenDialog.getLastDirectory());
                    fs.setFileFilter(new FileFilter() {

                        @Override
                        public boolean accept(File f) {
                            return (f.getName().toLowerCase().endsWith(".avi") || f.isDirectory());
                        }

                        @Override
                        public String getDescription() {
                            return "AVI files";
                        }
                    });
                    int returnVal = fs.showSaveDialog(m_dlgAvi);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File f = ensureCorrectFileExtension(fs.getSelectedFile());
                        if (m_nFirstIdx != -1 && m_nLastIdx != -1) ip = createNewImagePlus(m_imagePlus, m_nFirstIdx, m_nLastIdx); else ip = m_imagePlus;
                        if (generateInfoFile(m_imagePlus, f, m_nFirstIdx, m_nLastIdx)) {
                            m_aviWriter.writeImage(ip, f.getAbsolutePath(), AVI_Writer.NO_COMPRESSION, 0);
                            JOptionPane.showMessageDialog(m_dlgAvi, "AVI creation succesful", "Success", JOptionPane.PLAIN_MESSAGE);
                            m_dlgAvi.dispose();
                        }
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(m_dlgAvi, "An error occurred during the AVI creation", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        m_tfFirstImageName.setEditable(false);
        m_btFrom.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                m_nFirstIdx = m_imagePlus.getCurrentSlice();
                m_tfFirstImageName.setText(generateNameText(m_imagePlus, m_nFirstIdx));
                m_btGoToFirst.setEnabled(true);
            }
        });
        m_tfLastImageName.setEditable(false);
        m_btTo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                m_nLastIdx = m_imagePlus.getCurrentSlice();
                m_tfLastImageName.setText(generateNameText(m_imagePlus, m_nLastIdx));
                m_btGoToLast.setEnabled(true);
            }
        });
        m_btGoToFirst.setEnabled(false);
        m_btGoToFirst.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (m_nFirstIdx != -1) m_imagePlus.setSlice(m_nFirstIdx);
            }
        });
        m_btGoToLast.setEnabled(false);
        m_btGoToLast.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (m_nFirstIdx != -1) m_imagePlus.setSlice(m_nLastIdx);
            }
        });
        JPanel pnButtons = new JPanel();
        pnButtons.add(m_btCopyTo);
        pnButtons.add(m_btCreateAvi);
        JPanel pnBase = new JPanel(new MigLayout("", "[grow, fill]", "[]5[]10[]"));
        pnBase.add(new JLabel("First image: "));
        pnBase.add(m_tfFirstImageName);
        pnBase.add(m_btFrom, "");
        pnBase.add(m_btGoToFirst, "wrap");
        pnBase.add(new JLabel("Last image: "));
        pnBase.add(m_tfLastImageName);
        pnBase.add(m_btTo, "");
        pnBase.add(m_btGoToLast, "wrap");
        pnBase.add(pnButtons, "span, alignx center, grow 0");
        m_dlgAvi.getContentPane().add(pnBase);
        m_dlgAvi.pack();
        m_dlgAvi.setVisible(true);
    }

    @Override
    public void run(ImageProcessor ip) {
        throw new UnsupportedOperationException("Nem használható a run metódus. Paraméter: \"" + ip + "\"");
    }

    @Override
    public int setup(String arg, ImagePlus imp) {
        if (imp != null) {
            m_imagePlus = imp;
            m_ijStack = imp.getStack();
        } else m_dlgAvi.dispose();
        return DONE;
    }

    /**
	 * A meglévő ImagePlus objektum ImageStackjéből kimásolja a startIdx és endIdx közötti
	 * képeket, és létrehoz egy új ImagePlust, majd visszatér az új objektummal
	 * @param ip
	 * @param startIdx
	 * @param endIdx
	 * @return
	 */
    private ImagePlus createNewImagePlus(ImagePlus ip, int startIdx, int endIdx) {
        ImageStack isOld = ip.getStack();
        ImageStack is = new ImageStack(isOld.getWidth(), isOld.getHeight(), isOld.getColorModel());
        for (int i = startIdx; i <= endIdx; i++) is.addSlice(isOld.getSliceLabel(i), isOld.getProcessor(i));
        return new ImagePlus("AVI", is);
    }

    /**
	 * Leellenőrzi, hogy a megadott file kiterjesztése .avi-e.
	 * Ha igen visszaadja az eredeti file-t, ha nem, akkor hozzáad egy .avi
	 * kiterjesztést
	 * @param f
	 * @return
	 */
    private File ensureCorrectFileExtension(File f) {
        if (f.getName().toLowerCase().endsWith(".avi")) return f; else {
            File newFile = new File(f.getAbsolutePath() + ".avi");
            return newFile;
        }
    }

    /**
	 * Legenerál az AVI file mellé egy információs fájlt
	 * @param ip
	 * @param aviFile
	 * @return
	 */
    private boolean generateInfoFile(ImagePlus ip, File aviFile, int nFirstIdx, int nLastIdx) {
        if (nFirstIdx > nLastIdx || nLastIdx > ip.getStackSize()) throw new IllegalArgumentException("Start idx: " + nFirstIdx + ", End idx: " + nLastIdx + ", Stack size: " + ip.getStackSize());
        String parentPath = aviFile.getParentFile().getAbsolutePath();
        String infoFileName = aviFile.getName().replace(".avi", ".info");
        File infoFile = new File(parentPath + File.separator + infoFileName);
        try {
            if (infoFile.exists()) {
                int ans = JOptionPane.showConfirmDialog(m_dlgAvi, "The file already exists, do you want to overwrite?", "", JOptionPane.YES_NO_OPTION);
                if (ans == JOptionPane.NO_OPTION) return false;
                infoFile.delete();
            }
            infoFile.createNewFile();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(m_dlgAvi, "Couldn't create the info file: " + infoFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(infoFile));
            String nl = System.getProperty("line.separator");
            out.write((nLastIdx - nFirstIdx + 1) + nl);
            out.write(nl);
            ImageStack stack = ip.getStack();
            for (int i = nFirstIdx; i <= nLastIdx; i++) {
                out.write(stack.getSliceLabel(i) + nl);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(m_dlgAvi, "Couldn't write the info file: " + infoFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * Index alapján megkeresi az imageStackben a megfelelő képet, és visszaadja a
	 * nevét, valamint a számát a stackben
	 * @param ip
	 * @param idx
	 * @return
	 */
    private String generateNameText(ImagePlus ip, int idx) {
        String strText = null;
        if (ip != null) strText = idx + "/" + ip.getStackSize() + " - " + ip.getStack().getSliceLabel(idx);
        return strText;
    }

    private String m_strDefaultSaveLocation;

    private AVI_Writer m_aviWriter = new AVI_Writer();

    private ImageStack m_ijStack;

    private ImagePlus m_imagePlus;

    private JDialog m_dlgAvi;

    private JButton m_btCopyTo = new JButton("Copy frames to...");

    private JButton m_btCreateAvi = new JButton("Create AVI from frames");

    private JButton m_btFrom = new JButton("Set AS first image");

    private JButton m_btTo = new JButton("Set AS last image");

    private JButton m_btGoToFirst = new JButton("Go to first");

    private JButton m_btGoToLast = new JButton("Go to last");

    private int m_nFirstIdx = -1;

    private int m_nLastIdx = -1;

    private JTextField m_tfFirstImageName = new JTextField(15);

    private JTextField m_tfLastImageName = new JTextField(15);

    private JFrame m_owner;
}
