package org.edits.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import org.edits.Edits;
import org.edits.definition.ModuleDefinition;
import org.edits.definition.ObjectFactory;
import org.edits.distance.algorithms.CosineSimilarity;
import org.edits.distance.algorithms.JaroWinkler;
import org.edits.distance.algorithms.OverlapDistance;
import org.edits.distance.algorithms.RougeS;
import org.edits.distance.algorithms.RougeW;
import org.edits.distance.algorithms.TokenEditDistance;

public class AlgorithmsPanel extends JPanel implements MouseListener, ActionListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private Map<String, String> algorithmFullNames;

    private AlgorithmPanel apanel;

    private ObjectFactory factory;

    private JTextField field;

    private JFrame frame;

    private JRadioButton linear;

    private File path;

    private JRadioButton svm;

    private JPanel tpane;

    private JRadioButton weka;

    public AlgorithmsPanel(JFrame frame_) {
        factory = new ObjectFactory();
        path = new File(Edits.path());
        algorithmFullNames = new HashMap<String, String>();
        algorithmFullNames.put(OverlapDistance.NAME, "Word Overlap");
        algorithmFullNames.put(CosineSimilarity.NAME, "Cosine Similarity");
        algorithmFullNames.put(TokenEditDistance.NAME, "Token Edit Distance");
        algorithmFullNames.put(JaroWinkler.NAME, "Jaro Winkler");
        algorithmFullNames.put(RougeS.NAME, "Rouge S");
        algorithmFullNames.put(RougeW.NAME, "Rouge W");
        setLayout(new BorderLayout());
        frame = frame_;
        setBorder(new TitledBorder("Algorithm"));
        field = new JTextField("edits -r +overlap");
        field.setEditable(false);
        field.addMouseListener(this);
        add(BorderLayout.CENTER, field);
        tpane = new JPanel(new BorderLayout());
        apanel = new AlgorithmPanel("overlap");
        tpane.add(BorderLayout.CENTER, apanel);
        JPanel algs = new JPanel(new GridLayout(algorithmFullNames.keySet().size(), 1));
        tpane.add(BorderLayout.WEST, algs);
        for (String a : algorithmFullNames.keySet()) {
            JCheckBox bo = new JCheckBox(algorithmFullNames.get(a));
            bo.setSelected(a.equals("overlap"));
            bo.addActionListener(this);
            bo.setActionCommand(a);
            algs.add(bo);
        }
        JPanel comb = new JPanel();
        comb.setBorder(new TitledBorder("Combination"));
        ButtonGroup g = new ButtonGroup();
        linear = new JRadioButton("Linear");
        g.add(linear);
        linear.setSelected(true);
        linear.setEnabled(false);
        comb.add(linear);
        weka = new JRadioButton("Weka");
        g.add(weka);
        weka.setSelected(false);
        weka.setEnabled(false);
        comb.add(weka);
        svm = new JRadioButton("SVM");
        g.add(svm);
        svm.setSelected(false);
        svm.setEnabled(false);
        comb.add(svm);
        tpane.add(BorderLayout.NORTH, comb);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (algorithmFullNames.containsKey(e.getActionCommand())) {
            JCheckBox v = (JCheckBox) e.getSource();
            List<String> xx = apanel.getNames();
            if (v.isSelected()) {
                xx.add(v.getActionCommand());
            } else {
                if (xx.size() == 1) {
                    v.setSelected(true);
                    return;
                }
                xx.remove(e.getActionCommand());
            }
            linear.setEnabled(xx.size() > 1);
            weka.setEnabled(xx.size() > 1);
            svm.setEnabled(xx.size() > 1);
            apanel.setWeightButtons();
            return;
        }
        if (e.getActionCommand().equals("View")) {
            try {
                JDialog w = new JDialog(frame, "Configuratin", true);
                JScrollPane p = new JScrollPane(new JTextArea(factory.marshal(factory.createModule(definition()))));
                w.setContentPane(p);
                w.setPreferredSize(new Dimension(500, 300));
                w.pack();
                w.setVisible(true);
                return;
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(frame, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        JFileChooser f = new JFileChooser(path);
        f.setMultiSelectionEnabled(false);
        f.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = f.showSaveDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        try {
            if (f.getSelectedFile().exists()) {
                if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(frame, "File alread exists. Do you want me to overwrite it")) return;
            }
            factory.marshal(f.getSelectedFile().getAbsolutePath(), factory.createModule(definition()), true);
            JOptionPane.showMessageDialog(frame, "Configuration Saved", "Done", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(frame, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        path = f.getSelectedFile().getParentFile();
    }

    public ModuleDefinition definition() {
        ModuleDefinition def = apanel.definition();
        if (apanel.getNames().size() > 1) {
            if (linear.isSelected()) def.setName("linear"); else def.setName(weka.isSelected() ? "weka" : "svmlight");
        }
        return def;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu m = new JPopupMenu("Configuration");
            JMenuItem item = new JMenuItem("Save");
            item.addActionListener(this);
            m.add(item);
            item = new JMenuItem("View");
            item.addActionListener(this);
            m.add(item);
            m.show(this, e.getX(), e.getY());
            return;
        }
        int i = JOptionPane.showConfirmDialog(frame, tpane, "Algorithm", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
        if (i == JOptionPane.OK_OPTION) {
            System.out.println("Here");
            String line = apanel.getLine();
            if (apanel.getNames().size() > 1) {
                String co = null;
                if (linear.isSelected()) co = "linear"; else co = weka.isSelected() ? "weka" : "svmlight";
                line = "-" + co + " " + line;
            }
            field.setText("edits -r " + line);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
}
