package jSimMacs.display.gromacs;

import jSimMacs.display.dialog.ssh.SSHFileChooser;
import jSimMacs.display.gromacs.event.ParamEditorFinishedEvent;
import jSimMacs.display.gromacs.event.ParamEditorFinishedEventListener;
import jSimMacs.logic.JSimLogic;
import jSimMacs.logic.handler.SSHDataHandler;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.EventListenerList;

/**
 * @author sr
 * Adapted from Guimacs paramEditor Class
 */
public class ParameterEditor extends JFrame implements ActionListener {

    Font f = new Font("Serif", Font.BOLD, 20);

    JLabel title = new JLabel("MD Parameter Editor");

    JLabel desc = new JLabel("<html>This program allows the user to create Molecular Dynamics Parameter (MDP) files from scratch.<br><br>The user is to choose the required parameters from the ones listed below and enter appropriate<br>values in the text boxes given next to each of them. This program does not check for consistency<br>of values entered in any of the fields below. It is the user's responsibility to enter values<br>relevant to parameters used.</html>");

    String titles[] = new String[] { "Preprocessing", "Run Control", "Langevin Dynamics", "Energy Minimization", "Shell Molecular Dynamics", "Output Control", "Neighbor Searching", "Electrostatics", "VdW", "Tables", "Ewald", "Temperature Coupling", "Pressure Coupling", "Simulated Annealing", "Velocity Generation", "Bonds", "Energy Group Exclusions", "NMR Refinement", "Free Energy Perturbation", "Non-Equilibrium MD", "Electric Fields", "Mixed Quantum/Classical Dynamics" };

    String ppLblTxt[], rcLblTxt[], rcTxtBox[], ldLblTxt[], emLblTxt[], smdLblTxt[], ocLblTxt[], nsLblTxt[], esLblTxt[], vdwLblTxt[], tLblTxt[], ewLblTxt[], tcLblTxt[], pcLblTxt[], saLblTxt[], vgLblTxt[], bLblTxt[], egeLblTxt[], nrLblTxt[], fepLblTxt[], nemLblTxt[], efLblTxt[], qcLblTxt[];

    String IntOptTxt[], CmodeTxt[], NsTypeTxt[], PbcTxt[], CTypeTxt[], VdwTypeTxt[], DispCorrTxt[], OptfftTxt[], TcouplTxt[], PcouplTxt[], PcouplTypeTxt[], AnnealingTxt[], GenVelTxt[], ConstraintsTxt[], ConstAlgoTxt[], UnconstStartTxt[], MorseTxt[], DisreTxt[], DisreWtTxt[], DisreMixedTxt[], OrireTxt[], FreeEnrTxt[], QMTxt[], QMSchemeTxt[], SHTxt[];

    GridBagLayout[] gridbags, leftbags;

    GridBagConstraints[] gridconsts, leftconsts;

    JLabel[] ppLabels, rcLabels, ldLabels, emLabels, smdLabels, ocLabels, nsLabels, esLabels, vdwLabels, tLabels, ewLabels, tcLabels, pcLabels, saLabels, vgLabels, bLabels, egeLabels, nrLabels, fepLabels, nemLabels, efLabels, qcLabels;

    JTextField[] ppTextBoxes, rcTextBoxes, ldTextBoxes, emTextBoxes, smdTextBoxes, ocTextBoxes, nsTextBoxes, esTextBoxes, vdwTextBoxes, tTextBoxes, ewTextBoxes, tcTextBoxes, pcTextBoxes, saTextBoxes, vgTextBoxes, bTextBoxes, egeTextBoxes, nrTextBoxes, fepTextBoxes, nemTextBoxes, efTextBoxes, qcTextBoxes;

    JComboBox IntOpt, CmodeOpt, NsTypeOpt, PbcOpt, CtypeOpt, VdwTypeOpt, DispCorrOpt, OptfftOpt, TcouplOpt, PcouplOpt, PcouplTypeOpt, AnnealingOpt, GenVelOpt, ConstraintsOpt, ConstAlgoOpt, UnconstStartOpt, MorseOpt, DisreOpt, DisreWtOpt, DisreMixedOpt, OrireOpt, FreeEnrOpt, QMOpt, QMSchemeOpt, SHOpt;

    JPanel[] in_panels, left_panels;

    JScrollPane[] scrpanes;

    JButton save;

    JFileChooser savefile, openfile;

    private String sshOutfile;

    int i = 0, j = 0, returnVal, n;

    File infile, outfile;

    String mdp, value, line;

    private Object fileChooser;

    protected EventListenerList listenerList = new EventListenerList();

    private JTabbedPane tabbedPane;

    public ParameterEditor() {
        super("Parameter Editor");
        fileChooser = new JFileChooser();
        title.setFont(f);
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gc = new GridBagConstraints();
        Container content = this.getContentPane();
        JPanel pane = new JPanel(new BorderLayout());
        content.add(pane, BorderLayout.CENTER);
        content.add(new JScrollPane(pane));
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        gridbags = new GridBagLayout[titles.length];
        gridconsts = new GridBagConstraints[titles.length];
        leftbags = new GridBagLayout[titles.length];
        leftconsts = new GridBagConstraints[titles.length];
        in_panels = new JPanel[titles.length];
        left_panels = new JPanel[titles.length];
        scrpanes = new JScrollPane[titles.length];
        JPanel titlePane = new JPanel(gb);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0.4;
        gc.weighty = 0.4;
        gc.insets = new Insets(10, 10, 10, 10);
        gb.setConstraints(title, gc);
        titlePane.add(title);
        gc.gridx = 0;
        gc.gridy = 1;
        gc.insets = new Insets(10, 10, 10, 10);
        gb.setConstraints(desc, gc);
        titlePane.add(desc);
        pane.add(titlePane, BorderLayout.NORTH);
        for (i = 0; i < titles.length; i++) {
            gridbags[i] = new GridBagLayout();
            gridconsts[i] = new GridBagConstraints();
            leftbags[i] = new GridBagLayout();
            leftconsts[i] = new GridBagConstraints();
            left_panels[i] = new JPanel(leftbags[i]);
            in_panels[i] = new JPanel();
            in_panels[i].setLayout(gridbags[i]);
            tabbedPane.add(titles[i], in_panels[i]);
        }
        pane.add(tabbedPane, BorderLayout.CENTER);
        JPanel btPane = new JPanel(new FlowLayout());
        save = new JButton("Save to File");
        btPane.add(save);
        save.addActionListener(this);
        save.setActionCommand("save");
        gc.gridx = 0;
        gc.gridy = titles.length + 2;
        gb.setConstraints(btPane, gc);
        pane.add(btPane, BorderLayout.SOUTH);
        initPreProcessor();
        initRunControl();
        initLangevinDynamics();
        initEnergyMinimization();
        initShellDynamics();
        initOutputControl();
        initNeighborSearching();
        initElectrostatics();
        initVdW();
        initTable();
        initEwald();
        initTemperatureCoup();
        initPressureCoup();
        initSimulatedAnnealing();
        initVelocityGeneration();
        initBond();
        initEnergyGroup();
        initNMRRefinement();
        initFreeEnergyPerturbation();
        initNoneqMD();
        initElectricField();
        initQuantumMD();
    }

    public ParameterEditor(Object fc) {
        this();
        this.fileChooser = fc;
    }

    public void addParamEditorFinishedEventListener(ParamEditorFinishedEventListener listener) {
        listenerList.add(ParamEditorFinishedEventListener.class, listener);
    }

    public void removeParamEditorFinishedEventListener(ParamEditorFinishedEventListener listener) {
        listenerList.remove(ParamEditorFinishedEventListener.class, listener);
    }

    void fireParamEditorFinished(ParamEditorFinishedEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ParamEditorFinishedEventListener.class) {
                ((ParamEditorFinishedEventListener) listeners[i + 1]).paramEditorFinished(evt);
            }
        }
    }

    private void initQuantumMD() {
        qcLblTxt = new String[] { "QMMM", "QMMM-grps", "QMMMscheme", "QMmethod", "QMbasis", "QMcharge", "QMmult", "CASorbitals", "CASelectrons", "SH" };
        QMTxt = new String[] { "Choose One", "no", "yes" };
        QMSchemeTxt = new String[] { "Choose One", "normal", "ONIOM", "QMMM-grps" };
        SHTxt = new String[] { "Choose One", "no", "yes" };
        qcLabels = new JLabel[qcLblTxt.length];
        qcTextBoxes = new JTextField[qcLblTxt.length - 3];
        QMOpt = new JComboBox(QMTxt);
        QMSchemeOpt = new JComboBox(QMSchemeTxt);
        SHOpt = new JComboBox(SHTxt);
        for (int i = 0; i < qcLblTxt.length; i++) {
            qcLabels[i] = new JLabel(qcLblTxt[i]);
            qcLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < qcTextBoxes.length; i++) {
            qcTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < qcLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(qcLabels[i], leftconsts[j]);
            left_panels[j].add(qcLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(QMOpt, leftconsts[j]);
        left_panels[j].add(QMOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 1;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(qcTextBoxes[0], leftconsts[j]);
        left_panels[j].add(qcTextBoxes[0]);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 2;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(QMSchemeOpt, leftconsts[j]);
        left_panels[j].add(QMSchemeOpt);
        for (int i = 1; i < qcTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 2;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(qcTextBoxes[i], leftconsts[j]);
            left_panels[j].add(qcTextBoxes[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 9;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(SHOpt, leftconsts[j]);
        left_panels[j].add(SHOpt);
    }

    private void initElectricField() {
        efLblTxt = new String[] { "E_x", "E_y", "E_z" };
        efLabels = new JLabel[efLblTxt.length];
        efTextBoxes = new JTextField[efLblTxt.length];
        for (int i = 0; i < efLblTxt.length; i++) {
            efLabels[i] = new JLabel(efLblTxt[i]);
            efLabels[i].setPreferredSize(new Dimension(150, 15));
            efTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < efLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(efLabels[i], leftconsts[j]);
            left_panels[j].add(efLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(efTextBoxes[i], leftconsts[j]);
            left_panels[j].add(efTextBoxes[i]);
        }
        j++;
    }

    private void initNoneqMD() {
        nemLblTxt = new String[] { "acc_grps", "accelerate", "freezegrps", "freezedim", "cos_acceleration", "deform" };
        nemLabels = new JLabel[nemLblTxt.length];
        nemTextBoxes = new JTextField[nemLblTxt.length];
        for (int i = 0; i < nemLblTxt.length; i++) {
            nemLabels[i] = new JLabel(nemLblTxt[i]);
            nemLabels[i].setPreferredSize(new Dimension(150, 15));
            nemTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < nemLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(nemLabels[i], leftconsts[j]);
            left_panels[j].add(nemLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(nemTextBoxes[i], leftconsts[j]);
            left_panels[j].add(nemTextBoxes[i]);
        }
        j++;
    }

    private void initFreeEnergyPerturbation() {
        fepLblTxt = new String[] { "free_energy", "init_lambda", "delta_lambda", "sc_alpha", "sc_power", "sc_sigma" };
        FreeEnrTxt = new String[] { "Choose One", "no", "yes" };
        fepLabels = new JLabel[fepLblTxt.length];
        fepTextBoxes = new JTextField[fepLblTxt.length - 1];
        FreeEnrOpt = new JComboBox(FreeEnrTxt);
        for (int i = 0; i < fepLblTxt.length; i++) {
            fepLabels[i] = new JLabel(fepLblTxt[i]);
            fepLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < fepTextBoxes.length; i++) {
            fepTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < fepLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(fepLabels[i], leftconsts[j]);
            left_panels[j].add(fepLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(FreeEnrOpt, leftconsts[j]);
        left_panels[j].add(FreeEnrOpt);
        for (int i = 0; i < fepTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 1;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(fepTextBoxes[i], leftconsts[j]);
            left_panels[j].add(fepTextBoxes[i]);
        }
        j++;
    }

    private void initNMRRefinement() {
        nrLblTxt = new String[] { "disre", "disre_weighting", "disre_mixed", "disre_fc", "disre_tau", "nstdisreout", "orire", "orire_fc", "orire_tau", "orire_fitgrp", "nstorireout" };
        DisreTxt = new String[] { "Choose One", "no", "simple", "ensemble" };
        DisreWtTxt = new String[] { "Choose One", "conservative", "equal" };
        DisreMixedTxt = new String[] { "Choose One", "no", "yes" };
        OrireTxt = new String[] { "Choose One", "no", "yes" };
        nrLabels = new JLabel[nrLblTxt.length];
        nrTextBoxes = new JTextField[nrLblTxt.length - 4];
        DisreOpt = new JComboBox(DisreTxt);
        DisreWtOpt = new JComboBox(DisreWtTxt);
        DisreMixedOpt = new JComboBox(DisreMixedTxt);
        OrireOpt = new JComboBox(OrireTxt);
        for (int i = 0; i < nrLblTxt.length; i++) {
            nrLabels[i] = new JLabel(nrLblTxt[i]);
            nrLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < nrTextBoxes.length; i++) {
            nrTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < nrLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(nrLabels[i], leftconsts[j]);
            left_panels[j].add(nrLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(DisreOpt, leftconsts[j]);
        left_panels[j].add(DisreOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 1;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(DisreWtOpt, leftconsts[j]);
        left_panels[j].add(DisreWtOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 2;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(DisreMixedOpt, leftconsts[j]);
        left_panels[j].add(DisreMixedOpt);
        for (int i = 0; i < 3; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 3;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(nrTextBoxes[i], leftconsts[j]);
            left_panels[j].add(nrTextBoxes[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 6;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(OrireOpt, leftconsts[j]);
        left_panels[j].add(OrireOpt);
        for (int i = 3; i < nrTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 4;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(nrTextBoxes[i], leftconsts[j]);
            left_panels[j].add(nrTextBoxes[i]);
        }
        j++;
    }

    private void initEnergyGroup() {
        egeLblTxt = new String[] { "energygrp_excl" };
        egeLabels = new JLabel[egeLblTxt.length];
        egeTextBoxes = new JTextField[egeLblTxt.length];
        for (int i = 0; i < egeLblTxt.length; i++) {
            egeLabels[i] = new JLabel(egeLblTxt[i]);
            egeLabels[i].setPreferredSize(new Dimension(150, 15));
            egeTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < egeLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(egeLabels[i], leftconsts[j]);
            left_panels[j].add(egeLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(egeTextBoxes[i], leftconsts[j]);
            left_panels[j].add(egeTextBoxes[i]);
        }
        j++;
    }

    private void initBond() {
        bLblTxt = new String[] { "constraints", "constraint_algorithm", "unconstrained_start", "shake_tol", "lincs_order", "lincs_iter", "lincs_warnangle", "morse" };
        ConstraintsTxt = new String[] { "Choose One", "none", "hbonds", "all-bonds", "h-angles", "all-angles" };
        ConstAlgoTxt = new String[] { "Choose One", "lincs", "shake" };
        UnconstStartTxt = new String[] { "Choose One", "no", "yes" };
        MorseTxt = new String[] { "Choose One", "no", "yes" };
        bLabels = new JLabel[bLblTxt.length];
        bTextBoxes = new JTextField[bLblTxt.length - 4];
        ConstraintsOpt = new JComboBox(ConstraintsTxt);
        ConstAlgoOpt = new JComboBox(ConstAlgoTxt);
        UnconstStartOpt = new JComboBox(UnconstStartTxt);
        MorseOpt = new JComboBox(MorseTxt);
        for (int i = 0; i < bLblTxt.length; i++) {
            bLabels[i] = new JLabel(bLblTxt[i]);
            bLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < bTextBoxes.length; i++) {
            bTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < bLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(bLabels[i], leftconsts[j]);
            left_panels[j].add(bLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(ConstraintsOpt, leftconsts[j]);
        left_panels[j].add(ConstraintsOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 1;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(ConstAlgoOpt, leftconsts[j]);
        left_panels[j].add(ConstAlgoOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 2;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(UnconstStartOpt, leftconsts[j]);
        left_panels[j].add(UnconstStartOpt);
        for (int i = 0; i < bTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 3;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(bTextBoxes[i], leftconsts[j]);
            left_panels[j].add(bTextBoxes[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 7;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(MorseOpt, leftconsts[j]);
        left_panels[j].add(MorseOpt);
        j++;
    }

    private void initVelocityGeneration() {
        vgLblTxt = new String[] { "gen_vel", "gen_temp", "gen_seed" };
        GenVelTxt = new String[] { "Choose One", "no", "yes" };
        vgLabels = new JLabel[vgLblTxt.length];
        vgTextBoxes = new JTextField[vgLblTxt.length - 1];
        GenVelOpt = new JComboBox(GenVelTxt);
        for (int i = 0; i < vgLblTxt.length; i++) {
            vgLabels[i] = new JLabel(vgLblTxt[i]);
            vgLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < vgTextBoxes.length; i++) {
            vgTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < vgLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(vgLabels[i], leftconsts[j]);
            left_panels[j].add(vgLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(GenVelOpt, leftconsts[j]);
        left_panels[j].add(GenVelOpt);
        for (int i = 0; i < vgTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 1;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(vgTextBoxes[i], leftconsts[j]);
            left_panels[j].add(vgTextBoxes[i]);
        }
        j++;
    }

    private void initSimulatedAnnealing() {
        saLblTxt = new String[] { "annealing", "annealing_npoints", "annealing_time", "annealing_temp" };
        AnnealingTxt = new String[] { "Choose One", "no", "single", "periodic" };
        saLabels = new JLabel[saLblTxt.length];
        saTextBoxes = new JTextField[saLblTxt.length - 1];
        AnnealingOpt = new JComboBox(AnnealingTxt);
        for (int i = 0; i < saLblTxt.length; i++) {
            saLabels[i] = new JLabel(saLblTxt[i]);
            saLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < saTextBoxes.length; i++) {
            saTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < saLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(saLabels[i], leftconsts[j]);
            left_panels[j].add(saLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(AnnealingOpt, leftconsts[j]);
        left_panels[j].add(AnnealingOpt);
        for (int i = 0; i < saTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 1;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(saTextBoxes[i], leftconsts[j]);
            left_panels[j].add(saTextBoxes[i]);
        }
        j++;
    }

    private void initPressureCoup() {
        pcLblTxt = new String[] { "pcoupl", "pcoupltype", "tau_p", "compressibility", "ref_p" };
        PcouplTxt = new String[] { "Choose One", "no", "berendsen", "Parrinello-Rahman" };
        PcouplTypeTxt = new String[] { "Choose One", "isotropic", "semiisotropic", "anisotropic", "surface-tension" };
        pcLabels = new JLabel[pcLblTxt.length];
        pcTextBoxes = new JTextField[pcLblTxt.length - 2];
        PcouplOpt = new JComboBox(PcouplTxt);
        PcouplTypeOpt = new JComboBox(PcouplTypeTxt);
        for (int i = 0; i < pcLblTxt.length; i++) {
            pcLabels[i] = new JLabel(pcLblTxt[i]);
            pcLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < pcTextBoxes.length; i++) {
            pcTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < pcLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(pcLabels[i], leftconsts[j]);
            left_panels[j].add(pcLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(PcouplOpt, leftconsts[j]);
        left_panels[j].add(PcouplOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 1;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(PcouplTypeOpt, leftconsts[j]);
        left_panels[j].add(PcouplTypeOpt);
        for (int i = 0; i < tcTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 2;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(pcTextBoxes[i], leftconsts[j]);
            left_panels[j].add(pcTextBoxes[i]);
        }
        j++;
    }

    private void initTemperatureCoup() {
        tcLblTxt = new String[] { "tcoupl", "tc_grps", "tau_t", "ref_t" };
        TcouplTxt = new String[] { "Choose One", "no", "berendsen", "nose-hoover" };
        tcLabels = new JLabel[tcLblTxt.length];
        tcTextBoxes = new JTextField[tcLblTxt.length - 1];
        TcouplOpt = new JComboBox(TcouplTxt);
        for (int i = 0; i < tcLblTxt.length; i++) {
            tcLabels[i] = new JLabel(tcLblTxt[i]);
            tcLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < tcTextBoxes.length; i++) {
            tcTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < tcLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(tcLabels[i], leftconsts[j]);
            left_panels[j].add(tcLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(TcouplOpt, leftconsts[j]);
        left_panels[j].add(TcouplOpt);
        for (int i = 0; i < tcTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 1;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(tcTextBoxes[i], leftconsts[j]);
            left_panels[j].add(tcTextBoxes[i]);
        }
        j++;
    }

    private void initEwald() {
        ewLblTxt = new String[] { "fourierspacing", "fourier_nx", "fourier_ny", "fourier_nz", "pme_order", "ewald_rtol", "ewald_geometry", "epsilon_surface", "optimize_fft" };
        OptfftTxt = new String[] { "Choose One", "no", "yes" };
        ewLabels = new JLabel[ewLblTxt.length];
        ewTextBoxes = new JTextField[ewLblTxt.length - 1];
        OptfftOpt = new JComboBox(OptfftTxt);
        for (int i = 0; i < ewLblTxt.length; i++) {
            ewLabels[i] = new JLabel(ewLblTxt[i]);
            ewLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < ewTextBoxes.length; i++) {
            ewTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < ewLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(ewLabels[i], leftconsts[j]);
            left_panels[j].add(ewLabels[i]);
        }
        for (int i = 0; i < ewTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(ewTextBoxes[i], leftconsts[j]);
            left_panels[j].add(ewTextBoxes[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 8;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(OptfftOpt, leftconsts[j]);
        left_panels[j].add(OptfftOpt);
        j++;
    }

    private void initTable() {
        tLblTxt = new String[] { "table-extension", "energygrp_table" };
        tLabels = new JLabel[tLblTxt.length];
        tTextBoxes = new JTextField[tLblTxt.length];
        for (int i = 0; i < tLblTxt.length; i++) {
            tLabels[i] = new JLabel(tLblTxt[i]);
            tLabels[i].setPreferredSize(new Dimension(150, 15));
            tTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < tLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(tLabels[i], leftconsts[j]);
            left_panels[j].add(tLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(tTextBoxes[i], leftconsts[j]);
            left_panels[j].add(tTextBoxes[i]);
        }
        j++;
    }

    private void initVdW() {
        vdwLblTxt = new String[] { "vdwtype", "rvdw_switch", "rvdw", "DispCorr" };
        VdwTypeTxt = new String[] { "Choose One", "Cut-off", "Shift", "Switch", "Encad-Shift", "User" };
        DispCorrTxt = new String[] { "Choose One", "no", "EnerPres", "Ener" };
        vdwLabels = new JLabel[vdwLblTxt.length];
        vdwTextBoxes = new JTextField[vdwLblTxt.length - 2];
        for (int i = 0; i < vdwLblTxt.length; i++) {
            vdwLabels[i] = new JLabel(vdwLblTxt[i]);
            vdwLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < vdwTextBoxes.length; i++) {
            vdwTextBoxes[i] = new JTextField(10);
        }
        VdwTypeOpt = new JComboBox(VdwTypeTxt);
        DispCorrOpt = new JComboBox(DispCorrTxt);
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < vdwLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(vdwLabels[i], leftconsts[j]);
            left_panels[j].add(vdwLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(VdwTypeOpt, leftconsts[j]);
        left_panels[j].add(VdwTypeOpt);
        for (int i = 0; i < vdwTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 1;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(vdwTextBoxes[i], leftconsts[j]);
            left_panels[j].add(vdwTextBoxes[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 3;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(DispCorrOpt, leftconsts[j]);
        left_panels[j].add(DispCorrOpt);
        j++;
    }

    private void initElectrostatics() {
        esLblTxt = new String[] { "coulomb_type", "rcoulomb_switch", "rcoulomb", "epsilon_r", "epsilon_rf" };
        CTypeTxt = new String[] { "Choose One", "Cut-off", "Ewald", "PME", "PPPM", "Reaction-Field", "Generalized-Reaction-Field", "Reaction-Field-nec", "Shift", "Encad-Shift", "Switch", "User", "PME-User" };
        esLabels = new JLabel[esLblTxt.length];
        esTextBoxes = new JTextField[esLblTxt.length - 1];
        for (int i = 0; i < esLblTxt.length; i++) {
            esLabels[i] = new JLabel(esLblTxt[i]);
            esLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < esTextBoxes.length; i++) {
            esTextBoxes[i] = new JTextField(10);
        }
        CtypeOpt = new JComboBox(CTypeTxt);
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < esLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(esLabels[i], leftconsts[j]);
            left_panels[j].add(esLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(CtypeOpt, leftconsts[j]);
        left_panels[j].add(CtypeOpt);
        for (int i = 0; i < esTextBoxes.length; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 1;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(esTextBoxes[i], leftconsts[j]);
            left_panels[j].add(esTextBoxes[i]);
        }
        j++;
    }

    private void initNeighborSearching() {
        nsLblTxt = new String[] { "nstlist", "ns_type", "pbc", "rlist" };
        nsLabels = new JLabel[nsLblTxt.length];
        nsTextBoxes = new JTextField[2];
        for (int i = 0; i < nsLblTxt.length; i++) {
            nsLabels[i] = new JLabel(nsLblTxt[i]);
            nsLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < nsTextBoxes.length; i++) {
            nsTextBoxes[i] = new JTextField(10);
        }
        NsTypeTxt = new String[] { "Choose One", "grid", "simple" };
        PbcTxt = new String[] { "Choose One", "xyz", "no" };
        NsTypeOpt = new JComboBox(NsTypeTxt);
        PbcOpt = new JComboBox(PbcTxt);
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < nsLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(nsLabels[i], leftconsts[j]);
            left_panels[j].add(nsLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(nsTextBoxes[0], leftconsts[j]);
        left_panels[j].add(nsTextBoxes[0]);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 1;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(NsTypeOpt, leftconsts[j]);
        left_panels[j].add(NsTypeOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 2;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(PbcOpt, leftconsts[j]);
        left_panels[j].add(PbcOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 3;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(nsTextBoxes[1], leftconsts[j]);
        left_panels[j].add(nsTextBoxes[1]);
        j++;
    }

    private void initOutputControl() {
        ocLblTxt = new String[] { "nstxout", "nstvout", "nstfout", "nstlog", "nstenergy", "nstxtcout", "xtc_precision", "xtc_grps", "energygrps" };
        ocLabels = new JLabel[ocLblTxt.length];
        ocTextBoxes = new JTextField[ocLblTxt.length];
        for (int i = 0; i < ocLblTxt.length; i++) {
            ocLabels[i] = new JLabel(ocLblTxt[i]);
            ocLabels[i].setPreferredSize(new Dimension(150, 15));
            ocTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < ocLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(ocLabels[i], leftconsts[j]);
            left_panels[j].add(ocLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(ocTextBoxes[i], leftconsts[j]);
            left_panels[j].add(ocTextBoxes[i]);
        }
        j++;
    }

    private void initShellDynamics() {
        smdLblTxt = new String[] { "emtol", "niter", "fcstep" };
        smdLabels = new JLabel[smdLblTxt.length];
        smdTextBoxes = new JTextField[smdLblTxt.length];
        for (int i = 0; i < smdLblTxt.length; i++) {
            smdLabels[i] = new JLabel(smdLblTxt[i]);
            smdLabels[i].setPreferredSize(new Dimension(150, 15));
            smdTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < smdLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(smdLabels[i], leftconsts[j]);
            left_panels[j].add(smdLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(smdTextBoxes[i], leftconsts[j]);
            left_panels[j].add(smdTextBoxes[i]);
        }
        j++;
    }

    private void initEnergyMinimization() {
        emLblTxt = new String[] { "emtol", "emstep", "nstcgsteep", "nbfgscorr" };
        emLabels = new JLabel[emLblTxt.length];
        emTextBoxes = new JTextField[emLblTxt.length];
        for (int i = 0; i < emLblTxt.length; i++) {
            emLabels[i] = new JLabel(emLblTxt[i]);
            emLabels[i].setPreferredSize(new Dimension(150, 15));
            emTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < emLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(emLabels[i], leftconsts[j]);
            left_panels[j].add(emLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(emTextBoxes[i], leftconsts[j]);
            left_panels[j].add(emTextBoxes[i]);
        }
        j++;
    }

    private void initLangevinDynamics() {
        ldLblTxt = new String[] { "bd_fric", "ld_seed" };
        ldLabels = new JLabel[ldLblTxt.length];
        ldTextBoxes = new JTextField[ldLblTxt.length];
        for (int i = 0; i < ldLblTxt.length; i++) {
            ldLabels[i] = new JLabel(ldLblTxt[i]);
            ldLabels[i].setPreferredSize(new Dimension(150, 15));
            ldTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < ldLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(ldLabels[i], leftconsts[j]);
            left_panels[j].add(ldLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(ldTextBoxes[i], leftconsts[j]);
            left_panels[j].add(ldTextBoxes[i]);
        }
        j++;
    }

    public void initPreProcessor() {
        ppLblTxt = new String[] { "title", "cpp", "include", "define" };
        ppLabels = new JLabel[ppLblTxt.length];
        ppTextBoxes = new JTextField[ppLblTxt.length];
        for (int i = 0; i < ppLblTxt.length; i++) {
            ppLabels[i] = new JLabel(ppLblTxt[i]);
            ppLabels[i].setPreferredSize(new Dimension(150, 15));
            ppTextBoxes[i] = new JTextField(10);
        }
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < ppLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(ppLabels[i], leftconsts[j]);
            left_panels[j].add(ppLabels[i]);
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(ppTextBoxes[i], leftconsts[j]);
            left_panels[j].add(ppTextBoxes[i]);
        }
        j++;
    }

    public void initRunControl() {
        rcLblTxt = new String[] { "integrator", "tinit", "dt", "nsteps", "init_step", "comm_mode", "nstcomm" };
        rcTxtBox = new String[] { "0", "0.001", "0", "0", "1" };
        rcLabels = new JLabel[rcLblTxt.length];
        rcTextBoxes = new JTextField[rcLblTxt.length - 2];
        for (int i = 0; i < rcLabels.length; i++) {
            rcLabels[i] = new JLabel(rcLblTxt[i]);
            rcLabels[i].setPreferredSize(new Dimension(150, 15));
        }
        for (int i = 0; i < rcTextBoxes.length; i++) {
            rcTextBoxes[i] = new JTextField(10);
            rcTextBoxes[i].setText(rcTxtBox[i]);
        }
        IntOptTxt = new String[] { "Choose One", "md", "sd", "bd", "steep", "cg", "l-bfgs", "nm", "tpi" };
        CmodeTxt = new String[] { "Choose One", "Linear", "Angular", "No" };
        IntOpt = new JComboBox(IntOptTxt);
        CmodeOpt = new JComboBox(CmodeTxt);
        gridconsts[j].gridx = 0;
        gridconsts[j].gridy = 0;
        gridconsts[j].weightx = 0.4;
        gridconsts[j].weighty = 0.4;
        gridconsts[j].ipadx = 10;
        gridconsts[j].anchor = GridBagConstraints.LINE_START;
        gridbags[j].setConstraints(left_panels[j], gridconsts[j]);
        in_panels[j].add(left_panels[j]);
        for (int i = 0; i < rcLabels.length; i++) {
            leftconsts[j].gridx = 0;
            leftconsts[j].gridy = i;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(rcLabels[i], leftconsts[j]);
            left_panels[j].add(rcLabels[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 0;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(IntOpt, leftconsts[j]);
        left_panels[j].add(IntOpt);
        for (int i = 0; i < rcTxtBox.length - 1; i++) {
            leftconsts[j].gridx = 1;
            leftconsts[j].gridy = i + 1;
            leftconsts[j].anchor = GridBagConstraints.LINE_START;
            leftconsts[j].insets = new Insets(3, 0, 0, 0);
            leftbags[j].setConstraints(rcTextBoxes[i], leftconsts[j]);
            left_panels[j].add(rcTextBoxes[i]);
        }
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 5;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(CmodeOpt, leftconsts[j]);
        left_panels[j].add(CmodeOpt);
        leftconsts[j].gridx = 1;
        leftconsts[j].gridy = 6;
        leftconsts[j].anchor = GridBagConstraints.LINE_START;
        leftconsts[j].insets = new Insets(3, 0, 0, 0);
        leftbags[j].setConstraints(rcTextBoxes[4], leftconsts[j]);
        left_panels[j].add(rcTextBoxes[4]);
        j++;
    }

    private void openFileChooser() {
        if (fileChooser instanceof JFileChooser) {
            JFileChooser fc = (JFileChooser) fileChooser;
            int returnVal = fc.showSaveDialog(this);
            outfile = null;
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                outfile = fc.getSelectedFile();
            }
        } else if (fileChooser instanceof SSHFileChooser) {
            SSHFileChooser sshFc = (SSHFileChooser) fileChooser;
            sshFc.showDialog();
            sshOutfile = null;
            if (sshFc.isApproval()) {
                sshOutfile = sshFc.getSelectedFile();
            }
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals("save")) {
            openFileChooser();
            if (outfile != null) {
                if (outfile.exists()) {
                    n = JOptionPane.showConfirmDialog(null, "The file " + outfile.toString() + " already exists.\nDo you want to overwrite this file?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                } else {
                    outfile = new File(outfile.toString());
                }
                if (n == JOptionPane.YES_OPTION) {
                    outfile = new File(outfile.toString());
                    JSimLogic.getInstance().writeParameterFile(this, outfile);
                    fireParamEditorFinished(new ParamEditorFinishedEvent(this));
                }
            }
            if (sshOutfile != null) {
                SSHFileChooser sshFc = (SSHFileChooser) fileChooser;
                SSHDataHandler handler = sshFc.getHandler();
                String localFile = handler.createTempPath(sshOutfile);
                localFile += sshOutfile.substring(sshOutfile.lastIndexOf("/") + 1, sshOutfile.length());
                outfile = new File(localFile);
                JSimLogic.getInstance().writeParameterFile(this, outfile);
                try {
                    handler.copyToServer(outfile);
                    fireParamEditorFinished(new ParamEditorFinishedEvent(this));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * @return the outfile
	 */
    public String getFileText() {
        if (outfile != null) return outfile.getAbsolutePath();
        if (sshOutfile != null) return sshOutfile;
        return null;
    }

    /**
	 * @return the ppLabels
	 */
    public JLabel[] getPpLabels() {
        return ppLabels;
    }

    /**
	 * @return the ppTextBoxes
	 */
    public JTextField[] getPpTextBoxes() {
        return ppTextBoxes;
    }

    /**
	 * @return the rcLabels
	 */
    public JLabel[] getRcLabels() {
        return rcLabels;
    }

    /**
	 * @return the rcTextBoxes
	 */
    public JTextField[] getRcTextBoxes() {
        return rcTextBoxes;
    }

    /**
	 * @return the intOpt
	 */
    public JComboBox getIntOpt() {
        return IntOpt;
    }

    /**
	 * @return the cmodeOpt
	 */
    public JComboBox getCmodeOpt() {
        return CmodeOpt;
    }

    /**
	 * @return the ldLabels
	 */
    public JLabel[] getLdLabels() {
        return ldLabels;
    }

    /**
	 * @return the ldTextBoxes
	 */
    public JTextField[] getLdTextBoxes() {
        return ldTextBoxes;
    }

    /**
	 * @return the emLabels
	 */
    public JLabel[] getEmLabels() {
        return emLabels;
    }

    /**
	 * @return the emTextBoxes
	 */
    public JTextField[] getEmTextBoxes() {
        return emTextBoxes;
    }

    /**
	 * @return the smdLabels
	 */
    public JLabel[] getSmdLabels() {
        return smdLabels;
    }

    /**
	 * @return the smdTextBoxes
	 */
    public JTextField[] getSmdTextBoxes() {
        return smdTextBoxes;
    }

    /**
	 * @return the ocLabels
	 */
    public JLabel[] getOcLabels() {
        return ocLabels;
    }

    /**
	 * @return the ocTextBoxes
	 */
    public JTextField[] getOcTextBoxes() {
        return ocTextBoxes;
    }

    /**
	 * @return the nsLabels
	 */
    public JLabel[] getNsLabels() {
        return nsLabels;
    }

    /**
	 * @return the nsTextBoxes
	 */
    public JTextField[] getNsTextBoxes() {
        return nsTextBoxes;
    }

    /**
	 * @return the nsTypeOpt
	 */
    public JComboBox getNsTypeOpt() {
        return NsTypeOpt;
    }

    /**
	 * @return the pbcOpt
	 */
    public JComboBox getPbcOpt() {
        return PbcOpt;
    }

    /**
	 * @return the esLabels
	 */
    public JLabel[] getEsLabels() {
        return esLabels;
    }

    /**
	 * @return the esTextBoxes
	 */
    public JTextField[] getEsTextBoxes() {
        return esTextBoxes;
    }

    /**
	 * @return the ctypeOpt
	 */
    public JComboBox getCtypeOpt() {
        return CtypeOpt;
    }

    /**
	 * @return the vdwLabels
	 */
    public JLabel[] getVdwLabels() {
        return vdwLabels;
    }

    /**
	 * @return the vdwTextBoxes
	 */
    public JTextField[] getVdwTextBoxes() {
        return vdwTextBoxes;
    }

    /**
	 * @return the vdwTypeOpt
	 */
    public JComboBox getVdwTypeOpt() {
        return VdwTypeOpt;
    }

    /**
	 * @return the dispCorrOpt
	 */
    public JComboBox getDispCorrOpt() {
        return DispCorrOpt;
    }

    /**
	 * @return the tLabels
	 */
    public JLabel[] getTLabels() {
        return tLabels;
    }

    /**
	 * @return the tTextBoxes
	 */
    public JTextField[] getTTextBoxes() {
        return tTextBoxes;
    }

    /**
	 * @return the ewLabels
	 */
    public JLabel[] getEwLabels() {
        return ewLabels;
    }

    /**
	 * @return the ewTextBoxes
	 */
    public JTextField[] getEwTextBoxes() {
        return ewTextBoxes;
    }

    /**
	 * @return the optfftOpt
	 */
    public JComboBox getOptfftOpt() {
        return OptfftOpt;
    }

    /**
	 * @return the tcLabels
	 */
    public JLabel[] getTcLabels() {
        return tcLabels;
    }

    /**
	 * @return the tcTextBoxes
	 */
    public JTextField[] getTcTextBoxes() {
        return tcTextBoxes;
    }

    /**
	 * @return the tcouplOpt
	 */
    public JComboBox getTcouplOpt() {
        return TcouplOpt;
    }

    /**
	 * @return the pcLabels
	 */
    public JLabel[] getPcLabels() {
        return pcLabels;
    }

    /**
	 * @return the pcTextBoxes
	 */
    public JTextField[] getPcTextBoxes() {
        return pcTextBoxes;
    }

    /**
	 * @return the pcouplOpt
	 */
    public JComboBox getPcouplOpt() {
        return PcouplOpt;
    }

    /**
	 * @return the pcouplTypeOpt
	 */
    public JComboBox getPcouplTypeOpt() {
        return PcouplTypeOpt;
    }

    /**
	 * @return the saLabels
	 */
    public JLabel[] getSaLabels() {
        return saLabels;
    }

    /**
	 * @return the saTextBoxes
	 */
    public JTextField[] getSaTextBoxes() {
        return saTextBoxes;
    }

    /**
	 * @return the annealingOpt
	 */
    public JComboBox getAnnealingOpt() {
        return AnnealingOpt;
    }

    /**
	 * @return the vgLabels
	 */
    public JLabel[] getVgLabels() {
        return vgLabels;
    }

    /**
	 * @return the vgTextBoxes
	 */
    public JTextField[] getVgTextBoxes() {
        return vgTextBoxes;
    }

    /**
	 * @return the genVelOpt
	 */
    public JComboBox getGenVelOpt() {
        return GenVelOpt;
    }

    /**
	 * @return the bLabels
	 */
    public JLabel[] getBLabels() {
        return bLabels;
    }

    /**
	 * @return the bTextBoxes
	 */
    public JTextField[] getBTextBoxes() {
        return bTextBoxes;
    }

    /**
	 * @return the constraintsOpt
	 */
    public JComboBox getConstraintsOpt() {
        return ConstraintsOpt;
    }

    /**
	 * @return the constAlgoOpt
	 */
    public JComboBox getConstAlgoOpt() {
        return ConstAlgoOpt;
    }

    /**
	 * @return the unconstStartOpt
	 */
    public JComboBox getUnconstStartOpt() {
        return UnconstStartOpt;
    }

    /**
	 * @return the morseOpt
	 */
    public JComboBox getMorseOpt() {
        return MorseOpt;
    }

    /**
	 * @return the egeLabels
	 */
    public JLabel[] getEgeLabels() {
        return egeLabels;
    }

    /**
	 * @return the egeTextBoxes
	 */
    public JTextField[] getEgeTextBoxes() {
        return egeTextBoxes;
    }

    /**
	 * @return the nrLabels
	 */
    public JLabel[] getNrLabels() {
        return nrLabels;
    }

    /**
	 * @return the nrTextBoxes
	 */
    public JTextField[] getNrTextBoxes() {
        return nrTextBoxes;
    }

    /**
	 * @return the disreOpt
	 */
    public JComboBox getDisreOpt() {
        return DisreOpt;
    }

    /**
	 * @return the disreWtOpt
	 */
    public JComboBox getDisreWtOpt() {
        return DisreWtOpt;
    }

    /**
	 * @return the disreMixedOpt
	 */
    public JComboBox getDisreMixedOpt() {
        return DisreMixedOpt;
    }

    /**
	 * @return the orireOpt
	 */
    public JComboBox getOrireOpt() {
        return OrireOpt;
    }

    /**
	 * @return the fepLabels
	 */
    public JLabel[] getFepLabels() {
        return fepLabels;
    }

    /**
	 * @return the fepTextBoxes
	 */
    public JTextField[] getFepTextBoxes() {
        return fepTextBoxes;
    }

    /**
	 * @return the freeEnrOpt
	 */
    public JComboBox getFreeEnrOpt() {
        return FreeEnrOpt;
    }

    /**
	 * @return the nemLabels
	 */
    public JLabel[] getNemLabels() {
        return nemLabels;
    }

    /**
	 * @return the nemTextBoxes
	 */
    public JTextField[] getNemTextBoxes() {
        return nemTextBoxes;
    }

    /**
	 * @return the efLabels
	 */
    public JLabel[] getEfLabels() {
        return efLabels;
    }

    /**
	 * @return the efTextBoxes
	 */
    public JTextField[] getEfTextBoxes() {
        return efTextBoxes;
    }

    /**
	 * @return the qcLabels
	 */
    public JLabel[] getQcLabels() {
        return qcLabels;
    }

    /**
	 * @return the qcTextBoxes
	 */
    public JTextField[] getQcTextBoxes() {
        return qcTextBoxes;
    }

    /**
	 * @return the qMOpt
	 */
    public JComboBox getQMOpt() {
        return QMOpt;
    }

    /**
	 * @return the qMSchemeOpt
	 */
    public JComboBox getQMSchemeOpt() {
        return QMSchemeOpt;
    }

    /**
	 * @return the sHOpt
	 */
    public JComboBox getSHOpt() {
        return SHOpt;
    }
}
