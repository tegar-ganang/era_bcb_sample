package net.bervini.rasael.mathexplorer.apps;

import net.bervini.rasael.mathexplorer.math.FMath;
import net.bervini.rasael.mathexplorer.math.Frazione;
import net.bervini.rasael.mathexplorer.math.MathEngine;
import net.bervini.rasael.mathexplorer.Tree.MathNode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import java.util.Vector;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import net.bervini.rasael.mathexplorer.Tree.NumberNode;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultComboBoxRenderer;
import org.pushingpixels.substance.api.skin.GraphiteAquaSkin;
import org.pushingpixels.substance.api.skin.SkinInfo;

/**
 *
 * @author Rasael
 */
public class MainApp extends javax.swing.JFrame {

    private static final char[] validOperators = { '/', '*', '+', '-' };

    DefaultMutableTreeNode root;

    StringBuffer scratchBookBuffer = new StringBuffer();

    float windowProportionX;

    float windowProportionY;

    BufferedImage graphicImage = null;

    class FourierParam {

        private String a0;

        private String an;

        private String bn;

        private String t0;

        public static final int SIMPLE = 0;

        public static final int COMPACT = 1;

        public static final int COMPLEX = 2;

        private int type;

        private String name;

        public FourierParam(String name, int type, String a0, String an, String bn, String t0) {
            this.name = name;
            this.type = type;
            this.a0 = a0;
            this.an = an;
            this.bn = bn;
            this.t0 = t0;
        }

        public String toString() {
            return getName();
        }

        /**
         * @return the a0
         */
        public String getA0() {
            return a0;
        }

        /**
         * @param a0 the a0 to set
         */
        public void setA0(String a0) {
            this.a0 = a0;
        }

        /**
         * @return the an
         */
        public String getAn() {
            return an;
        }

        /**
         * @param an the an to set
         */
        public void setAn(String an) {
            this.an = an;
        }

        /**
         * @return the bn
         */
        public String getBn() {
            return bn;
        }

        /**
         * @param bn the bn to set
         */
        public void setBn(String bn) {
            this.bn = bn;
        }

        /**
         * @return the type
         */
        public int getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(int type) {
            this.type = type;
        }

        /**
         * @return the t0
         */
        public String getT0() {
            return t0;
        }

        /**
         * @param t0 the t0 to set
         */
        public void setT0(String t0) {
            this.t0 = t0;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }
    }

    private Frazione calcolaEspressione(String espressione) {
        return valoreFunzione(espressione, null);
    }

    private Frazione valoreFunzione(String formula, Map<String, Frazione> x) {
        MathNode albero = MathEngine.generaAlbero(formula);
        if (albero == null) {
            JOptionPane.showMessageDialog(this, "C'è stato un errore nell'interpretazione della formula!");
            return new Frazione(Double.NaN);
        }
        txtBinaryTreeExpression.setText(formula);
        DefaultTreeModel model = (DefaultTreeModel) treeExpression.getModel();
        model.setRoot(generaJTree(albero));
        return MathEngine.evaluateResult(albero, x);
    }

    /** Creates new form MainApp */
    public MainApp() {
        initComponents();
        root = new DefaultMutableTreeNode("");
        DefaultTreeModel model = (DefaultTreeModel) treeExpression.getModel();
        model.setRoot(root);
        DefaultComboBoxModel comboModel = new DefaultComboBoxModel();
        comboModel.addElement(new FourierParam("Sawtooth", FourierParam.SIMPLE, "PI", "0", "-1/n", "2*PI"));
        comboModel.addElement(new FourierParam("t^2", FourierParam.SIMPLE, "8/3", "4/(n^2*PI^2)", "0", "2"));
        comboAnalisiWaveforms.setModel(comboModel);
        int state = getExtendedState();
        state |= MAXIMIZED_BOTH;
        setExtendedState(state);
        Vector<SkinInfo> skinInfos = new Vector<SkinInfo>(SubstanceLookAndFeel.getAllSkins().values());
        SkinInfo[] skinInfosStrings = new SkinInfo[skinInfos.size()];
        for (int i = 0; i < skinInfos.size(); i++) {
            skinInfosStrings[i] = skinInfos.get(i);
        }
        DefaultTableModel tmodel = new DefaultTableModel();
        tableValuesTable.setModel(tmodel);
        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel(skinInfosStrings);
        comboAvaiableLookAndFeels.setModel(comboBoxModel);
        comboAvaiableLookAndFeels.setRenderer(new SubstanceDefaultComboBoxRenderer(comboAvaiableLookAndFeels) {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                SkinInfo si = (SkinInfo) value;
                return super.getListCellRendererComponent(list, si.getDisplayName(), index, isSelected, cellHasFocus);
            }
        });
        int delay = 1000;
        int period = 10000;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                System.gc();
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, delay, period);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jTabbedPane1 = new javax.swing.JTabbedPane();
        tabScratchbook = new javax.swing.JPanel();
        scrollScratchText = new javax.swing.JScrollPane();
        txtScratchbook = new javax.swing.JEditorPane();
        btnEVALScratch = new javax.swing.JButton();
        comboUserInput = new javax.swing.JComboBox();
        chkSimplify = new javax.swing.JCheckBox();
        tabMain = new javax.swing.JPanel();
        lblFY = new javax.swing.JLabel();
        btnEval = new javax.swing.JButton();
        btnDisplay = new javax.swing.JButton();
        panDisplay = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        rasaelImagePanel1 = new net.bervini.rasael.mathexplorer.swing.RasaelImagePanel();
        comboY = new javax.swing.JComboBox();
        lblX = new javax.swing.JLabel();
        txtX = new javax.swing.JTextField();
        txtFx = new javax.swing.JTextField();
        lblFx = new javax.swing.JLabel();
        panDisplayParam = new javax.swing.JPanel();
        lblDisplayFrom = new javax.swing.JLabel();
        txtDisplayFrom = new javax.swing.JTextField();
        lblDisplayTo = new javax.swing.JLabel();
        txtDisplayTo = new javax.swing.JTextField();
        lblDisplayStep = new javax.swing.JLabel();
        txtDisplayStep = new javax.swing.JTextField();
        chkShowValues = new javax.swing.JCheckBox();
        chkShowLines = new javax.swing.JCheckBox();
        lblMouseXLabel = new javax.swing.JLabel();
        lbllMouseX = new javax.swing.JLabel();
        lblMouseYLabel = new javax.swing.JLabel();
        lblMouseY = new javax.swing.JLabel();
        chkShowGrid = new javax.swing.JCheckBox();
        chkShowSeparators = new javax.swing.JCheckBox();
        chkInvertiXY = new javax.swing.JCheckBox();
        chkInvertiValori = new javax.swing.JCheckBox();
        btnFunctionHelp = new javax.swing.JButton();
        btnSCSimpleFourier = new javax.swing.JButton();
        btnSCCompactFourier = new javax.swing.JButton();
        btnSCComplexFourier = new javax.swing.JButton();
        lblXOffset = new javax.swing.JLabel();
        txtXOffset = new javax.swing.JTextField();
        lblYOffset = new javax.swing.JLabel();
        txtYOffset = new javax.swing.JTextField();
        btnShowFunctionsList = new javax.swing.JButton();
        chkTabellamentoRisultati = new javax.swing.JToggleButton();
        btnGetValuesFromTable = new javax.swing.JButton();
        tabTabella = new javax.swing.JPanel();
        scrollValuesTable = new javax.swing.JScrollPane();
        tableValuesTable = new javax.swing.JTable();
        btnTableIMport = new javax.swing.JButton();
        btnTableExport = new javax.swing.JButton();
        btnTableRemove = new javax.swing.JButton();
        btnTableRemoveAll = new javax.swing.JButton();
        tabMaterie = new javax.swing.JTabbedPane();
        tabNumerica = new javax.swing.JPanel();
        panIntegration = new javax.swing.JPanel();
        lblAlgorithm = new javax.swing.JLabel();
        comboAlgorithm = new javax.swing.JComboBox();
        lblIntegrationFrom = new javax.swing.JLabel();
        txtIntegrationFrom = new javax.swing.JTextField();
        lblIntegrationTo = new javax.swing.JLabel();
        txtIntegrationTo = new javax.swing.JTextField();
        btnIntegrationCompute = new javax.swing.JButton();
        lblIntegrationN = new javax.swing.JLabel();
        txtIntegrationN = new javax.swing.JTextField();
        panInterpolation = new javax.swing.JPanel();
        tabAnalisi = new javax.swing.JPanel();
        btnAnalisiLoadWaveform = new javax.swing.JButton();
        comboAnalisiWaveforms = new javax.swing.JComboBox();
        panFourierSeries = new javax.swing.JTabbedPane();
        tabFourierGeneral = new javax.swing.JPanel();
        txtFourierMaxN = new javax.swing.JTextField();
        lblFourierMaxN = new javax.swing.JLabel();
        txtFourierStep = new javax.swing.JTextField();
        lblFourierStep = new javax.swing.JLabel();
        txtFourierPeriods = new javax.swing.JTextField();
        lblFourierPeriods = new javax.swing.JLabel();
        lblFourierT0 = new javax.swing.JLabel();
        txtT0 = new javax.swing.JTextField();
        lblPeriods = new javax.swing.JLabel();
        tabFourierReal = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        txtSimpleFourierA0 = new javax.swing.JTextField();
        txtSimpleFourierAN = new javax.swing.JTextField();
        txtSimpleFourierBN = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        btnEvalSimpleFourier = new javax.swing.JButton();
        jButton24 = new javax.swing.JButton();
        txtOmega = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        txtCompactFourierC0 = new javax.swing.JTextField();
        txtCompactFourierCN = new javax.swing.JTextField();
        txtCompactFourierWN = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        btnEvalCompactFourier = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        tabFourierComplex = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        txtComplexFourierD0 = new javax.swing.JTextField();
        txtComplexFourierDN = new javax.swing.JTextField();
        txtComplexFourierWN = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        btnEvalCompactFourier1 = new javax.swing.JButton();
        jButton25 = new javax.swing.JButton();
        tabCostanti = new javax.swing.JPanel();
        scrollConstantsTable = new javax.swing.JScrollPane();
        tableConstants = new javax.swing.JTable();
        btnConstantsDefine = new javax.swing.JButton();
        btnConstantsRemove = new javax.swing.JButton();
        btnConstantsImport = new javax.swing.JButton();
        btnConstantsExport = new javax.swing.JButton();
        tabAlbero = new javax.swing.JPanel();
        scrollExpressionTree = new javax.swing.JScrollPane();
        treeExpression = new javax.swing.JTree();
        lblBinaryTreeExpression = new javax.swing.JLabel();
        txtBinaryTreeExpression = new javax.swing.JTextField();
        tabOptions = new javax.swing.JPanel();
        panTraslazioni = new javax.swing.JPanel();
        lblTranslationHorizontal = new javax.swing.JLabel();
        txtTraslazioneOrizzontale = new javax.swing.JTextField();
        lblTraslazioneVerticale = new javax.swing.JLabel();
        lblCompressionHorizontal = new javax.swing.JLabel();
        lblCompressionVertical = new javax.swing.JLabel();
        txtTraslazioneVerticale = new javax.swing.JTextField();
        txtCompressioneOrizzontale = new javax.swing.JTextField();
        txtCompressioneVerticale = new javax.swing.JTextField();
        panLookAndFeel = new javax.swing.JPanel();
        comboAvaiableLookAndFeels = new javax.swing.JComboBox();
        lblRoundToDigits = new javax.swing.JLabel();
        txtOptionsRoundToDigits = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        mniMiniDifferentiator = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Rasael's MathExplorer");
        setResizable(false);
        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        txtScratchbook.setBackground(new java.awt.Color(0, 0, 0));
        txtScratchbook.setContentType("text/html");
        txtScratchbook.setEditable(false);
        txtScratchbook.setFont(new java.awt.Font("Courier New", 0, 14));
        txtScratchbook.setForeground(new java.awt.Color(255, 255, 255));
        txtScratchbook.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n\t<font color=\"white\">\n    <p style=\"margin-top: 0\">\r\n      \rBenvenuti in <b>Rasael MathExplorer</b>, un riassunto di quanto imparato a mate!<br>\n    </p>\n</font>\r\n  </body>\r\n</html>\r\n");
        scrollScratchText.setViewportView(txtScratchbook);
        btnEVALScratch.setText("EVAL");
        btnEVALScratch.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEVALScratchActionPerformed(evt);
            }
        });
        comboUserInput.setEditable(true);
        comboUserInput.setFont(new java.awt.Font("Tahoma", 0, 14));
        comboUserInput.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "(x+5/3)*x", "(x+6/3)*x" }));
        comboUserInput.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboUserInputActionPerformed(evt);
            }
        });
        chkSimplify.setSelected(true);
        chkSimplify.setText("Semplifica espressioni");
        javax.swing.GroupLayout tabScratchbookLayout = new javax.swing.GroupLayout(tabScratchbook);
        tabScratchbook.setLayout(tabScratchbookLayout);
        tabScratchbookLayout.setHorizontalGroup(tabScratchbookLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabScratchbookLayout.createSequentialGroup().addContainerGap().addGroup(tabScratchbookLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(scrollScratchText, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 886, Short.MAX_VALUE).addGroup(tabScratchbookLayout.createSequentialGroup().addComponent(comboUserInput, 0, 823, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnEVALScratch)).addComponent(chkSimplify, javax.swing.GroupLayout.Alignment.LEADING)).addContainerGap()));
        tabScratchbookLayout.setVerticalGroup(tabScratchbookLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabScratchbookLayout.createSequentialGroup().addComponent(chkSimplify).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(scrollScratchText, javax.swing.GroupLayout.DEFAULT_SIZE, 656, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabScratchbookLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(btnEVALScratch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(comboUserInput, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)).addContainerGap()));
        jTabbedPane1.addTab("Scratchbook", tabScratchbook);
        lblFY.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblFY.setText("F(X): Y=");
        btnEval.setForeground(new java.awt.Color(0, 255, 0));
        btnEval.setText("Computa");
        btnEval.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEvalActionPerformed(evt);
            }
        });
        btnDisplay.setText("PLOT");
        btnDisplay.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDisplayActionPerformed(evt);
            }
        });
        panDisplay.setBorder(javax.swing.BorderFactory.createTitledBorder("Display"));
        panDisplay.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseMoved(java.awt.event.MouseEvent evt) {
                panDisplayMouseMoved(evt);
            }
        });
        javax.swing.GroupLayout rasaelImagePanel1Layout = new javax.swing.GroupLayout(rasaelImagePanel1);
        rasaelImagePanel1.setLayout(rasaelImagePanel1Layout);
        rasaelImagePanel1Layout.setHorizontalGroup(rasaelImagePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 666, Short.MAX_VALUE));
        rasaelImagePanel1Layout.setVerticalGroup(rasaelImagePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 563, Short.MAX_VALUE));
        jScrollPane1.setViewportView(rasaelImagePanel1);
        javax.swing.GroupLayout panDisplayLayout = new javax.swing.GroupLayout(panDisplay);
        panDisplay.setLayout(panDisplayLayout);
        panDisplayLayout.setHorizontalGroup(panDisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDisplayLayout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE).addContainerGap()));
        panDisplayLayout.setVerticalGroup(panDisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDisplayLayout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE).addContainerGap()));
        comboY.setEditable(true);
        comboY.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1/X", "X^3", "A*X^2+B*X+C", "SQRT(R^2-X^2)", "A*X^3+B*X^2+C*X+D", "SIN(X)+Q", "x^2-10.5", "X^(2*X)+SIN(X)", "X+4", "(X+4)^2", "SIN(X-2*X)", "X^2-2*X+1" }));
        comboY.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboYActionPerformed(evt);
            }
        });
        lblX.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblX.setText("X=");
        txtX.setText("1.0");
        txtX.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtXActionPerformed(evt);
            }
        });
        txtFx.setEditable(false);
        lblFx.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblFx.setText("F(X)=");
        panDisplayParam.setBorder(javax.swing.BorderFactory.createTitledBorder("Parametri di visualizzazione"));
        lblDisplayFrom.setText("Da:");
        txtDisplayFrom.setText("-10.0");
        lblDisplayTo.setText("A:");
        txtDisplayTo.setText("10.0");
        lblDisplayStep.setText("Passo:");
        txtDisplayStep.setText("0.1");
        chkShowValues.setText("Mostra valori");
        chkShowLines.setSelected(true);
        chkShowLines.setText("Congiungi punti");
        lblMouseXLabel.setText("Mouse X:");
        lbllMouseX.setText("0.0");
        lblMouseYLabel.setText("Mouse Y:");
        lblMouseY.setText("0.0");
        chkShowGrid.setText("Mostra griglia");
        chkShowSeparators.setSelected(true);
        chkShowSeparators.setText("Mostra separatori");
        chkInvertiXY.setText("Inverti X/Y");
        chkInvertiValori.setText("Inverti valori");
        javax.swing.GroupLayout panDisplayParamLayout = new javax.swing.GroupLayout(panDisplayParam);
        panDisplayParam.setLayout(panDisplayParamLayout);
        panDisplayParamLayout.setHorizontalGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panDisplayParamLayout.createSequentialGroup().addContainerGap(20, Short.MAX_VALUE).addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDisplayParamLayout.createSequentialGroup().addGap(21, 21, 21).addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(panDisplayParamLayout.createSequentialGroup().addComponent(lblMouseYLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lblMouseY)).addGroup(panDisplayParamLayout.createSequentialGroup().addComponent(lblMouseXLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbllMouseX)))).addComponent(chkInvertiValori).addComponent(chkInvertiXY).addComponent(chkShowGrid).addComponent(chkShowLines).addComponent(lblDisplayStep).addComponent(chkShowValues).addGroup(panDisplayParamLayout.createSequentialGroup().addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblDisplayFrom).addComponent(lblDisplayTo)).addGap(33, 33, 33).addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(panDisplayParamLayout.createSequentialGroup().addGap(11, 11, 11).addComponent(txtDisplayStep, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE)).addGroup(panDisplayParamLayout.createSequentialGroup().addGap(10, 10, 10).addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtDisplayFrom, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE).addComponent(txtDisplayTo))))).addComponent(chkShowSeparators))));
        panDisplayParamLayout.setVerticalGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDisplayParamLayout.createSequentialGroup().addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDisplayFrom).addComponent(txtDisplayFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDisplayTo).addComponent(txtDisplayTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDisplayStep).addComponent(txtDisplayStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(chkShowValues).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(chkShowLines).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(chkShowGrid).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(chkShowSeparators).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(chkInvertiXY).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(chkInvertiValori).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblMouseXLabel).addComponent(lbllMouseX)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panDisplayParamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblMouseYLabel).addComponent(lblMouseY))));
        btnFunctionHelp.setText("?");
        btnFunctionHelp.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFunctionHelpActionPerformed(evt);
            }
        });
        btnSCSimpleFourier.setForeground(new java.awt.Color(0, 0, 255));
        btnSCSimpleFourier.setText("Fourier Semplice");
        btnSCSimpleFourier.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSCSimpleFourierActionPerformed(evt);
            }
        });
        btnSCCompactFourier.setForeground(new java.awt.Color(0, 204, 0));
        btnSCCompactFourier.setText("Fourier Compatto");
        btnSCCompactFourier.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSCCompactFourierActionPerformed(evt);
            }
        });
        btnSCComplexFourier.setForeground(new java.awt.Color(204, 0, 204));
        btnSCComplexFourier.setText("Fourier Complesso");
        lblXOffset.setText("xOffset:");
        txtXOffset.setText("0");
        lblYOffset.setText("yOffset:");
        txtYOffset.setText("0");
        btnShowFunctionsList.setText("...");
        chkTabellamentoRisultati.setSelected(true);
        chkTabellamentoRisultati.setText("Tabella");
        btnGetValuesFromTable.setText("Da tabella");
        btnGetValuesFromTable.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGetValuesFromTableActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout tabMainLayout = new javax.swing.GroupLayout(tabMain);
        tabMain.setLayout(tabMainLayout);
        tabMainLayout.setHorizontalGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabMainLayout.createSequentialGroup().addContainerGap().addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabMainLayout.createSequentialGroup().addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(panDisplayParam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGroup(tabMainLayout.createSequentialGroup().addComponent(lblXOffset).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(txtXOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(tabMainLayout.createSequentialGroup().addComponent(lblYOffset).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(txtYOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(panDisplay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addGroup(tabMainLayout.createSequentialGroup().addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblFY, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblX, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblFx, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtX, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 769, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabMainLayout.createSequentialGroup().addComponent(comboY, 0, 671, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(btnShowFunctionsList).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnFunctionHelp)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabMainLayout.createSequentialGroup().addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtFx, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE).addGroup(tabMainLayout.createSequentialGroup().addGap(250, 250, 250).addComponent(btnGetValuesFromTable))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(chkTabellamentoRisultati, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE).addComponent(btnEval, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnDisplay)))).addGroup(tabMainLayout.createSequentialGroup().addComponent(btnSCSimpleFourier).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnSCCompactFourier).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnSCComplexFourier))).addContainerGap()));
        tabMainLayout.setVerticalGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabMainLayout.createSequentialGroup().addContainerGap().addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblFY).addComponent(comboY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btnFunctionHelp).addComponent(btnShowFunctionsList)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblX).addComponent(txtX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnEval).addComponent(btnDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(txtFx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblFx))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnSCCompactFourier).addComponent(btnSCComplexFourier).addComponent(btnGetValuesFromTable)).addComponent(btnSCSimpleFourier).addComponent(chkTabellamentoRisultati)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabMainLayout.createSequentialGroup().addComponent(panDisplayParam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(txtXOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblXOffset)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblYOffset).addComponent(txtYOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addComponent(panDisplay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        jTabbedPane1.addTab("Basics", tabMain);
        tableValuesTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        scrollValuesTable.setViewportView(tableValuesTable);
        btnTableIMport.setText("Importa");
        btnTableExport.setText("Esporta");
        btnTableRemove.setText("Elimina");
        btnTableRemove.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTableRemoveActionPerformed(evt);
            }
        });
        btnTableRemoveAll.setText("Cancella tutto");
        btnTableRemoveAll.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTableRemoveAllActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout tabTabellaLayout = new javax.swing.GroupLayout(tabTabella);
        tabTabella.setLayout(tabTabellaLayout);
        tabTabellaLayout.setHorizontalGroup(tabTabellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabTabellaLayout.createSequentialGroup().addContainerGap().addGroup(tabTabellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(scrollValuesTable, javax.swing.GroupLayout.DEFAULT_SIZE, 886, Short.MAX_VALUE).addGroup(tabTabellaLayout.createSequentialGroup().addComponent(btnTableIMport).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnTableExport).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnTableRemove).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnTableRemoveAll))).addContainerGap()));
        tabTabellaLayout.setVerticalGroup(tabTabellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabTabellaLayout.createSequentialGroup().addContainerGap().addGroup(tabTabellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnTableIMport).addComponent(btnTableExport).addComponent(btnTableRemove).addComponent(btnTableRemoveAll)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(scrollValuesTable, javax.swing.GroupLayout.DEFAULT_SIZE, 698, Short.MAX_VALUE).addContainerGap()));
        jTabbedPane1.addTab("Tabella", tabTabella);
        panIntegration.setBorder(javax.swing.BorderFactory.createTitledBorder("Integrazione"));
        lblAlgorithm.setText("Algoritmo:");
        comboAlgorithm.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Cavalieri-Simpson", "Punto-medio", "Romberg", "Trapezi", "Trapezi-B" }));
        lblIntegrationFrom.setText("da:");
        txtIntegrationFrom.setText("1.0");
        lblIntegrationTo.setText("a:");
        txtIntegrationTo.setText("2.0");
        btnIntegrationCompute.setText("Computa");
        btnIntegrationCompute.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIntegrationComputeActionPerformed(evt);
            }
        });
        lblIntegrationN.setText("n:");
        txtIntegrationN.setText("16");
        javax.swing.GroupLayout panIntegrationLayout = new javax.swing.GroupLayout(panIntegration);
        panIntegration.setLayout(panIntegrationLayout);
        panIntegrationLayout.setHorizontalGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panIntegrationLayout.createSequentialGroup().addContainerGap().addGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblAlgorithm).addComponent(lblIntegrationFrom).addComponent(lblIntegrationTo)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(comboAlgorithm, 0, 796, Short.MAX_VALUE).addGroup(panIntegrationLayout.createSequentialGroup().addGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(txtIntegrationTo, javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtIntegrationFrom, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)).addGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panIntegrationLayout.createSequentialGroup().addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 605, Short.MAX_VALUE).addComponent(btnIntegrationCompute)).addGroup(panIntegrationLayout.createSequentialGroup().addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(lblIntegrationN).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(txtIntegrationN, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))))).addContainerGap()));
        panIntegrationLayout.setVerticalGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panIntegrationLayout.createSequentialGroup().addGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblAlgorithm).addComponent(comboAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblIntegrationFrom).addComponent(txtIntegrationFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblIntegrationN).addComponent(txtIntegrationN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panIntegrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblIntegrationTo).addComponent(txtIntegrationTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btnIntegrationCompute)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        panInterpolation.setBorder(javax.swing.BorderFactory.createTitledBorder("Interpolazione"));
        javax.swing.GroupLayout panInterpolationLayout = new javax.swing.GroupLayout(panInterpolation);
        panInterpolation.setLayout(panInterpolationLayout);
        panInterpolationLayout.setHorizontalGroup(panInterpolationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 869, Short.MAX_VALUE));
        panInterpolationLayout.setVerticalGroup(panInterpolationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 100, Short.MAX_VALUE));
        javax.swing.GroupLayout tabNumericaLayout = new javax.swing.GroupLayout(tabNumerica);
        tabNumerica.setLayout(tabNumericaLayout);
        tabNumericaLayout.setHorizontalGroup(tabNumericaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabNumericaLayout.createSequentialGroup().addContainerGap().addGroup(tabNumericaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(panInterpolation, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(panIntegration, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        tabNumericaLayout.setVerticalGroup(tabNumericaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabNumericaLayout.createSequentialGroup().addContainerGap().addComponent(panIntegration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(panInterpolation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(464, Short.MAX_VALUE)));
        tabMaterie.addTab("Numerica", tabNumerica);
        btnAnalisiLoadWaveform.setText("Carica");
        btnAnalisiLoadWaveform.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAnalisiLoadWaveformActionPerformed(evt);
            }
        });
        comboAnalisiWaveforms.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Sawtooth", "t^2" }));
        tabFourierGeneral.setBorder(javax.swing.BorderFactory.createTitledBorder("Impostazioni base"));
        txtFourierMaxN.setText("7");
        lblFourierMaxN.setText("Limite sommatoria:");
        txtFourierStep.setText("PI/100");
        lblFourierStep.setText("Passo:");
        txtFourierPeriods.setText("1");
        lblFourierPeriods.setText("Estendi per");
        lblFourierT0.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblFourierT0.setText("Periodo T0:");
        txtT0.setText("2*PI");
        lblPeriods.setText("periodi");
        javax.swing.GroupLayout tabFourierGeneralLayout = new javax.swing.GroupLayout(tabFourierGeneral);
        tabFourierGeneral.setLayout(tabFourierGeneralLayout);
        tabFourierGeneralLayout.setHorizontalGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabFourierGeneralLayout.createSequentialGroup().addContainerGap().addGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblFourierStep, javax.swing.GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE).addComponent(lblFourierMaxN, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(lblFourierPeriods, javax.swing.GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE).addComponent(lblFourierT0)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtFourierMaxN, javax.swing.GroupLayout.PREFERRED_SIZE, 715, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtFourierStep, javax.swing.GroupLayout.PREFERRED_SIZE, 715, javax.swing.GroupLayout.PREFERRED_SIZE).addGroup(tabFourierGeneralLayout.createSequentialGroup().addGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(txtFourierPeriods, javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtT0, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lblPeriods, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))).addContainerGap()));
        tabFourierGeneralLayout.setVerticalGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabFourierGeneralLayout.createSequentialGroup().addGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(txtT0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblFourierT0)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(txtFourierPeriods, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblPeriods).addComponent(lblFourierPeriods)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(txtFourierMaxN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblFourierMaxN)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabFourierGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(txtFourierStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblFourierStep)).addContainerGap(517, Short.MAX_VALUE)));
        panFourierSeries.addTab("Generale", tabFourierGeneral);
        tabFourierReal.setLayout(new javax.swing.BoxLayout(tabFourierReal, javax.swing.BoxLayout.PAGE_AXIS));
        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Serie di fourier semplice", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 11), new java.awt.Color(0, 0, 255)));
        jLabel3.setText("A0:");
        jLabel8.setText("An:");
        jLabel9.setText("Bn:");
        txtSimpleFourierA0.setText("PI");
        txtSimpleFourierAN.setText("0");
        txtSimpleFourierBN.setText("-1/n");
        jLabel10.setForeground(new java.awt.Color(0, 0, 255));
        jLabel10.setText("s(t) = A0/T0 + SUM(An * cos(n*w*t),n) + SUM(Bn * sin(n*w*t),n)");
        btnEvalSimpleFourier.setText("Calcola");
        btnEvalSimpleFourier.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEvalSimpleFourierActionPerformed(evt);
            }
        });
        jButton24.setText("Ricava da forma compatta");
        jButton24.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton24ActionPerformed(evt);
            }
        });
        txtOmega.setEditable(false);
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("w:");
        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup().addContainerGap().addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE).addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE).addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtOmega, javax.swing.GroupLayout.PREFERRED_SIZE, 787, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtSimpleFourierBN, javax.swing.GroupLayout.PREFERRED_SIZE, 787, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtSimpleFourierAN, javax.swing.GroupLayout.PREFERRED_SIZE, 787, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtSimpleFourierA0, javax.swing.GroupLayout.PREFERRED_SIZE, 787, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(jPanel4Layout.createSequentialGroup().addGap(10, 10, 10).addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, 844, Short.MAX_VALUE)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup().addGap(598, 598, 598).addComponent(jButton24, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnEvalSimpleFourier, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE))).addContainerGap()));
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addComponent(jLabel10).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel3).addComponent(txtSimpleFourierA0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel8).addComponent(txtSimpleFourierAN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel9).addComponent(txtSimpleFourierBN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel12).addComponent(txtOmega, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnEvalSimpleFourier).addComponent(jButton24))));
        tabFourierReal.add(jPanel4);
        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Serie di fourier compatta", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 11), new java.awt.Color(0, 204, 0)));
        jLabel20.setText("C0:");
        jLabel21.setText("Cn:");
        jLabel22.setText("Wn:");
        txtCompactFourierC0.setText("PI");
        txtCompactFourierCN.setText("0");
        txtCompactFourierWN.setText("-1/n");
        jLabel23.setForeground(new java.awt.Color(0, 204, 0));
        jLabel23.setText("s(t) = C0 + SUM(C(n) * cos(n*t + w(n))");
        btnEvalCompactFourier.setText("Calcola");
        btnEvalCompactFourier.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEvalCompactFourierActionPerformed(evt);
            }
        });
        jButton23.setText("Ricava dai coefficienti semplici");
        jButton23.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton23ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel6Layout.createSequentialGroup().addContainerGap().addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel20).addComponent(jLabel21).addComponent(jLabel22)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtCompactFourierWN, javax.swing.GroupLayout.DEFAULT_SIZE, 820, Short.MAX_VALUE).addComponent(txtCompactFourierCN, javax.swing.GroupLayout.DEFAULT_SIZE, 820, Short.MAX_VALUE).addComponent(txtCompactFourierC0, javax.swing.GroupLayout.DEFAULT_SIZE, 820, Short.MAX_VALUE)).addContainerGap()).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup().addContainerGap(606, Short.MAX_VALUE).addComponent(jButton23).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnEvalCompactFourier).addGap(10, 10, 10)).addGroup(jPanel6Layout.createSequentialGroup().addGap(10, 10, 10).addComponent(jLabel23).addContainerGap(662, Short.MAX_VALUE)));
        jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel6Layout.createSequentialGroup().addComponent(jLabel23).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel20).addComponent(txtCompactFourierC0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel21).addComponent(txtCompactFourierCN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel22).addComponent(txtCompactFourierWN, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnEvalCompactFourier).addComponent(jButton23)).addContainerGap(320, Short.MAX_VALUE)));
        tabFourierReal.add(jPanel6);
        panFourierSeries.addTab("Serie di fourier Reale", tabFourierReal);
        tabFourierComplex.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Serie di fourier complessa", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 11), new java.awt.Color(204, 0, 204)));
        jLabel24.setText("D0:");
        jLabel25.setText("Dn:");
        jLabel26.setText("Wn:");
        txtComplexFourierD0.setText("PI");
        txtComplexFourierDN.setText("0");
        txtComplexFourierWN.setText("-1/n");
        jLabel27.setForeground(new java.awt.Color(204, 0, 204));
        jLabel27.setText("s(t) = SUM(D(n) * exp(i*n*w*t),-oo,+oo)");
        btnEvalCompactFourier1.setText("Calcola");
        btnEvalCompactFourier1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEvalCompactFourier1ActionPerformed(evt);
            }
        });
        jButton25.setText("Ricava");
        jButton25.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton25ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout tabFourierComplexLayout = new javax.swing.GroupLayout(tabFourierComplex);
        tabFourierComplex.setLayout(tabFourierComplexLayout);
        tabFourierComplexLayout.setHorizontalGroup(tabFourierComplexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabFourierComplexLayout.createSequentialGroup().addContainerGap().addGroup(tabFourierComplexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel24).addComponent(jLabel25).addComponent(jLabel26)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabFourierComplexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtComplexFourierWN, javax.swing.GroupLayout.DEFAULT_SIZE, 820, Short.MAX_VALUE).addComponent(txtComplexFourierDN, javax.swing.GroupLayout.DEFAULT_SIZE, 820, Short.MAX_VALUE).addComponent(txtComplexFourierD0, javax.swing.GroupLayout.DEFAULT_SIZE, 820, Short.MAX_VALUE)).addContainerGap()).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabFourierComplexLayout.createSequentialGroup().addContainerGap(716, Short.MAX_VALUE).addComponent(jButton25).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnEvalCompactFourier1).addGap(10, 10, 10)).addGroup(tabFourierComplexLayout.createSequentialGroup().addGap(10, 10, 10).addComponent(jLabel27).addContainerGap(653, Short.MAX_VALUE)));
        tabFourierComplexLayout.setVerticalGroup(tabFourierComplexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabFourierComplexLayout.createSequentialGroup().addComponent(jLabel27).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabFourierComplexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel24).addComponent(txtComplexFourierD0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabFourierComplexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel25).addComponent(txtComplexFourierDN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabFourierComplexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel26).addComponent(txtComplexFourierWN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 500, Short.MAX_VALUE).addGroup(tabFourierComplexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnEvalCompactFourier1).addComponent(jButton25))));
        panFourierSeries.addTab("Serie di fourier complessa", tabFourierComplex);
        javax.swing.GroupLayout tabAnalisiLayout = new javax.swing.GroupLayout(tabAnalisi);
        tabAnalisi.setLayout(tabAnalisiLayout);
        tabAnalisiLayout.setHorizontalGroup(tabAnalisiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabAnalisiLayout.createSequentialGroup().addContainerGap().addGroup(tabAnalisiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(panFourierSeries, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 881, Short.MAX_VALUE).addGroup(tabAnalisiLayout.createSequentialGroup().addComponent(comboAnalisiWaveforms, 0, 812, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnAnalisiLoadWaveform))).addContainerGap()));
        tabAnalisiLayout.setVerticalGroup(tabAnalisiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabAnalisiLayout.createSequentialGroup().addContainerGap().addGroup(tabAnalisiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnAnalisiLoadWaveform).addComponent(comboAnalisiWaveforms, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(panFourierSeries, javax.swing.GroupLayout.DEFAULT_SIZE, 670, Short.MAX_VALUE).addContainerGap()));
        tabMaterie.addTab("Analisi dei segnali", tabAnalisi);
        jTabbedPane1.addTab("Materie", tabMaterie);
        tableConstants.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        scrollConstantsTable.setViewportView(tableConstants);
        btnConstantsDefine.setText("Definisci");
        btnConstantsRemove.setText("Elimina");
        btnConstantsImport.setText("Importa");
        btnConstantsExport.setText("Esporta");
        javax.swing.GroupLayout tabCostantiLayout = new javax.swing.GroupLayout(tabCostanti);
        tabCostanti.setLayout(tabCostantiLayout);
        tabCostantiLayout.setHorizontalGroup(tabCostantiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabCostantiLayout.createSequentialGroup().addContainerGap().addGroup(tabCostantiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(scrollConstantsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 886, Short.MAX_VALUE).addGroup(tabCostantiLayout.createSequentialGroup().addComponent(btnConstantsDefine).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnConstantsRemove).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnConstantsImport).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnConstantsExport))).addContainerGap()));
        tabCostantiLayout.setVerticalGroup(tabCostantiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabCostantiLayout.createSequentialGroup().addContainerGap().addGroup(tabCostantiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnConstantsDefine).addComponent(btnConstantsRemove).addComponent(btnConstantsImport).addComponent(btnConstantsExport)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(scrollConstantsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 698, Short.MAX_VALUE).addContainerGap()));
        jTabbedPane1.addTab("Costanti", tabCostanti);
        scrollExpressionTree.setViewportView(treeExpression);
        lblBinaryTreeExpression.setText("Albero binario per l'espressione:");
        txtBinaryTreeExpression.setEditable(false);
        javax.swing.GroupLayout tabAlberoLayout = new javax.swing.GroupLayout(tabAlbero);
        tabAlbero.setLayout(tabAlberoLayout);
        tabAlberoLayout.setHorizontalGroup(tabAlberoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabAlberoLayout.createSequentialGroup().addContainerGap().addGroup(tabAlberoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(scrollExpressionTree, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 886, Short.MAX_VALUE).addGroup(tabAlberoLayout.createSequentialGroup().addComponent(lblBinaryTreeExpression).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(txtBinaryTreeExpression, javax.swing.GroupLayout.DEFAULT_SIZE, 729, Short.MAX_VALUE))).addContainerGap()));
        tabAlberoLayout.setVerticalGroup(tabAlberoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabAlberoLayout.createSequentialGroup().addContainerGap().addGroup(tabAlberoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblBinaryTreeExpression).addComponent(txtBinaryTreeExpression, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(scrollExpressionTree, javax.swing.GroupLayout.DEFAULT_SIZE, 701, Short.MAX_VALUE).addContainerGap()));
        jTabbedPane1.addTab("Albero dell'espressione", tabAlbero);
        panTraslazioni.setBorder(javax.swing.BorderFactory.createTitledBorder("Traslazioni:"));
        lblTranslationHorizontal.setText("Traslazione orizzontale:");
        txtTraslazioneOrizzontale.setText("0.0");
        lblTraslazioneVerticale.setText("Traslazione verticale:");
        lblCompressionHorizontal.setText("Compressione orizzontale:");
        lblCompressionVertical.setText("Compressione verticale:");
        txtTraslazioneVerticale.setText("1.0");
        txtCompressioneOrizzontale.setText("1.0");
        txtCompressioneVerticale.setText("1.0");
        javax.swing.GroupLayout panTraslazioniLayout = new javax.swing.GroupLayout(panTraslazioni);
        panTraslazioni.setLayout(panTraslazioniLayout);
        panTraslazioniLayout.setHorizontalGroup(panTraslazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panTraslazioniLayout.createSequentialGroup().addContainerGap().addGroup(panTraslazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblTranslationHorizontal).addComponent(lblTraslazioneVerticale).addComponent(lblCompressionHorizontal).addComponent(lblCompressionVertical)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panTraslazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(txtTraslazioneVerticale, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE).addComponent(txtCompressioneOrizzontale).addComponent(txtTraslazioneOrizzontale, javax.swing.GroupLayout.DEFAULT_SIZE, 502, Short.MAX_VALUE).addComponent(txtCompressioneVerticale)).addContainerGap()));
        panTraslazioniLayout.setVerticalGroup(panTraslazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panTraslazioniLayout.createSequentialGroup().addGroup(panTraslazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblTranslationHorizontal).addComponent(txtTraslazioneOrizzontale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panTraslazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblTraslazioneVerticale).addComponent(txtTraslazioneVerticale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panTraslazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblCompressionHorizontal).addComponent(txtCompressioneOrizzontale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panTraslazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblCompressionVertical).addComponent(txtCompressioneVerticale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))));
        panLookAndFeel.setBorder(javax.swing.BorderFactory.createTitledBorder("Look and feel"));
        comboAvaiableLookAndFeels.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboAvaiableLookAndFeels.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboAvaiableLookAndFeelsActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout panLookAndFeelLayout = new javax.swing.GroupLayout(panLookAndFeel);
        panLookAndFeel.setLayout(panLookAndFeelLayout);
        panLookAndFeelLayout.setHorizontalGroup(panLookAndFeelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panLookAndFeelLayout.createSequentialGroup().addContainerGap().addComponent(comboAvaiableLookAndFeels, 0, 854, Short.MAX_VALUE).addContainerGap()));
        panLookAndFeelLayout.setVerticalGroup(panLookAndFeelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panLookAndFeelLayout.createSequentialGroup().addComponent(comboAvaiableLookAndFeels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        lblRoundToDigits.setText("Arrontada a cifre:");
        txtOptionsRoundToDigits.setText("6");
        javax.swing.GroupLayout tabOptionsLayout = new javax.swing.GroupLayout(tabOptions);
        tabOptions.setLayout(tabOptionsLayout);
        tabOptionsLayout.setHorizontalGroup(tabOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabOptionsLayout.createSequentialGroup().addContainerGap().addGroup(tabOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabOptionsLayout.createSequentialGroup().addGap(10, 10, 10).addComponent(lblRoundToDigits).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(txtOptionsRoundToDigits, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()).addGroup(tabOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabOptionsLayout.createSequentialGroup().addComponent(panLookAndFeel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()).addGroup(tabOptionsLayout.createSequentialGroup().addComponent(panTraslazioni, javax.swing.GroupLayout.DEFAULT_SIZE, 710, Short.MAX_VALUE).addGap(186, 186, 186))))));
        tabOptionsLayout.setVerticalGroup(tabOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabOptionsLayout.createSequentialGroup().addContainerGap().addComponent(panTraslazioni, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(panLookAndFeel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(tabOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblRoundToDigits).addComponent(txtOptionsRoundToDigits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(523, Short.MAX_VALUE)));
        jTabbedPane1.addTab("Opzioni", tabOptions);
        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);
        jMenu2.setText("Edit");
        mniMiniDifferentiator.setText("jMenuItem1");
        jMenu2.add(mniMiniDifferentiator);
        jMenuBar1.add(jMenu2);
        setJMenuBar(jMenuBar1);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jTabbedPane1).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 754, Short.MAX_VALUE).addContainerGap()));
        pack();
    }

    private void btnEvalActionPerformed(java.awt.event.ActionEvent evt) {
        String[] listaVariabili = MathEngine.getVariables(comboY.getSelectedItem().toString());
        Frazione x = calcolaEspressione(txtX.getText());
        String formula = comboY.getSelectedItem().toString();
        if (listaVariabili.length == 1) {
            try {
                Frazione y = valoreFunzione(formula, toHash(x));
                txtFx.setText("" + y);
                appendToBuffer("f(x)=<a href=\"#copyFormula:" + formula + "\">" + formula + "</a>");
                appendToBuffer("\t=> f(<a href=\"#copyValue:" + x + "\">" + x + "</a>)=<a href=\"#copyValue:" + y + "\">" + y + "</a>");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Formula non riconosciuta!");
            }
        } else {
            HashMap<String, Frazione> mappa = new HashMap<String, Frazione>();
            for (int i = 0; i < listaVariabili.length; i++) {
                String variabile = listaVariabili[i];
                if (!variabile.equalsIgnoreCase("x")) {
                    String val = JOptionPane.showInputDialog(variabile + "=");
                    if (val.equals("") || val == null) {
                        return;
                    }
                    Frazione d = calcolaEspressione(val);
                    mappa.put(variabile, d);
                }
            }
            try {
                mappa.put("x", x);
                final Frazione y = valoreFunzione(formula, mappa);
                appendToBuffer("f(x)=<a href=\"#copyFormula:" + formula + "\">" + formula + "</a>");
                final Object[] keySet = mappa.keySet().toArray();
                String str = "";
                for (int i = 0; i < mappa.size(); i++) {
                    final String name = keySet[i].toString();
                    final Frazione val = mappa.get(keySet[i]);
                    if (i > 0) {
                        str += ", ";
                    }
                    str += name + "=" + val;
                }
                appendToBuffer("\twith " + str);
                appendToBuffer("\t=> f(" + x + ")=" + y);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Formula non riconosciuta!");
            }
        }
    }

    void appendToBuffer(String text) {
        text = text.replaceAll("\t", "    ");
        text = text.replaceAll("\n", "<br>");
        text = "<font color=\"white\">" + text + "</font>";
        scratchBookBuffer.append("<br>" + text);
        txtScratchbook.setText(scratchBookBuffer.toString());
    }

    private HashMap<String, Frazione> toHash(Frazione x) {
        HashMap<String, Frazione> xx = new HashMap<String, Frazione>();
        xx.put("x", x);
        return xx;
    }

    private void btnDisplayActionPerformed(java.awt.event.ActionEvent evt) {
        HashMap<String, Frazione> mappa = new HashMap<String, Frazione>();
        String formula = comboY.getSelectedItem().toString();
        formula = formula.replaceAll("x", "x+" + txtTraslazioneOrizzontale.getText());
        String[] listaVariabili = MathEngine.getVariables(formula);
        String str = "";
        appendToBuffer("f(x)=<a href=\"#copyFormula:" + formula + "\">" + formula + "</a>");
        if (listaVariabili.length != 1) {
            for (int i = 0; i < listaVariabili.length; i++) {
                String variabile = listaVariabili[i];
                if (!variabile.equalsIgnoreCase("x")) {
                    String val = JOptionPane.showInputDialog(variabile + "=");
                    if (val == null || val.equals("")) {
                        return;
                    }
                    Frazione d = calcolaEspressione(val);
                    mappa.put(variabile, d);
                    if (i > 0) {
                        str = str + ", ";
                    }
                    str = str + variabile + "=" + d;
                }
            }
            appendToBuffer("PLOT(f(x)) with " + str);
        } else {
            appendToBuffer("PLOT(f(x))");
        }
        jTabbedPane1.setSelectedIndex(0);
        disegnaGrafico(mappa);
    }

    private void disegnaGrafico(HashMap<String, Frazione> mappa) {
        Frazione from = calcolaEspressione(txtDisplayFrom.getText());
        Frazione to = calcolaEspressione(txtDisplayTo.getText());
        Frazione step = calcolaEspressione(txtDisplayStep.getText());
        appendToBuffer("Plotting x in [" + from + "," + to + "] with a step of " + step);
        int n = (int) (FMath.abs(from.subtract(to)).divideBy(step)).doubleValue();
        Frazione[] x = new Frazione[n];
        Frazione[] y = new Frazione[n];
        for (int i = 0; i < n; i++) {
            x[i] = from.add(step.multiplyBy(i));
            mappa.put("x", x[i]);
            y[i] = valoreFunzione(comboY.getSelectedItem().toString(), mappa);
        }
        disegnaGrafico(x, y);
    }

    public void disegnaGrafico(Frazione[] x, Frazione[] y) {
        if (chkTabellamentoRisultati.isSelected()) {
            String[] headers = { "X", "Y" };
            Frazione[][] data = new Frazione[x.length][2];
            int arrotonda = Integer.parseInt(txtOptionsRoundToDigits.getText());
            for (int i = 0; i < x.length; i++) {
                data[i][0] = FMath.round(x[i].multiplyBy(Math.pow(10, arrotonda))).divideBy(Math.pow(10, arrotonda));
                data[i][1] = FMath.round(y[i].multiplyBy(Math.pow(10, arrotonda))).divideBy(Math.pow(10, arrotonda));
            }
            DefaultTableModel model = new DefaultTableModel(data, headers);
            tableValuesTable.setModel(model);
        }
        if (chkInvertiXY.isSelected()) {
            _disegnaGrafico(y, x);
        } else {
            _disegnaGrafico(x, y);
        }
    }

    public void _disegnaGrafico(Frazione[] x, Frazione[] y) {
        if (chkInvertiValori.isSelected()) {
            for (int i = 0; i < x.length; i++) {
                x[i] = x[i].opposto();
                y[i] = x[i].opposto();
            }
        }
        __disegnaGrafico(x, y);
    }

    public void __disegnaGrafico(Frazione[] x, Frazione[] y) {
        double maxX = 0.0, minX = Double.MAX_VALUE, maxY = 0.0, minY = Double.MAX_VALUE;
        for (int i = 0; i < x.length; i++) {
            maxX = Math.max(maxX, x[i].doubleValue());
            minX = Math.min(minX, x[i].doubleValue());
            maxY = Math.max(maxY, y[i].doubleValue());
            minY = Math.min(minY, y[i].doubleValue());
        }
        int h = panDisplay.getHeight();
        int w = panDisplay.getWidth();
        graphicImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = graphicImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.black);
        int xOffset = w / 2 + Integer.parseInt(txtXOffset.getText());
        int yOffset = h / 2 + Integer.parseInt(txtYOffset.getText());
        windowProportionX = ((float) Math.abs(maxX) + (float) Math.abs(minX)) / ((float) w - 10);
        windowProportionY = ((float) Math.abs(maxY) + (float) Math.abs(minY)) / ((float) h - 10);
        final int separatori = 10;
        double hSep = Math.abs(minY - maxY) / separatori;
        double wSep = Math.abs(minX - maxX) / separatori;
        hSep = Math.round(hSep * 100.0) / 100.0;
        wSep = Math.round(wSep * 100.0) / 100.0;
        for (int i = 0; i < separatori; i++) {
            double yReale = -hSep * i;
            int posY = -(int) (maxY - yReale / windowProportionY);
            if (chkShowGrid.isSelected()) {
                g.setColor(Color.lightGray);
                g.drawLine(0, posY + yOffset, w, posY + yOffset);
            }
            if (chkShowSeparators.isSelected()) {
                g.setColor(Color.black);
                g.drawLine(xOffset - 1, posY + yOffset, xOffset + 1, posY + yOffset);
                g.drawString("" + Math.abs(Math.round(yReale * 10.0) / 10.0), xOffset + 10, posY + yOffset);
            }
        }
        for (int i = 1; i < separatori; i++) {
            double yReale = hSep * i;
            int posY = -(int) (maxY - yReale / windowProportionY);
            if (chkShowGrid.isSelected()) {
                g.setColor(Color.lightGray);
                g.drawLine(0, posY + yOffset, w, posY + yOffset);
            }
            if (chkShowSeparators.isSelected()) {
                g.setColor(Color.black);
                g.drawLine(xOffset - 1, posY + yOffset, xOffset + 1, posY + yOffset);
                g.drawString("" + Math.round(yReale * 10.0) / -10.0, xOffset + 10, posY + yOffset);
            }
        }
        for (int i = 1; i < separatori; i++) {
            double xReale = wSep * i;
            int posX = -(int) (maxX - xReale / windowProportionX);
            if (chkShowGrid.isSelected()) {
                g.setColor(Color.lightGray);
                g.drawLine(posX + xOffset, 0, posX + xOffset, h);
            }
            if (chkShowSeparators.isSelected()) {
                g.setColor(Color.black);
                g.drawLine(posX + xOffset, yOffset - 1, posX + xOffset, yOffset - 1);
                g.drawString("" + Math.abs(Math.round(xReale * 10.0) / 10.0), xOffset + posX, yOffset + 10);
            }
        }
        for (int i = 1; i < separatori; i++) {
            double xReale = -wSep * i;
            int posX = -(int) (maxX - xReale / windowProportionX);
            if (chkShowGrid.isSelected()) {
                g.setColor(Color.lightGray);
                g.drawLine(posX + xOffset, 0, posX + xOffset, h);
            }
            if (chkShowSeparators.isSelected()) {
                g.setColor(Color.black);
                g.drawLine(posX + xOffset, yOffset - 1, posX + xOffset, yOffset - 1);
                g.drawString("" + Math.round(xReale * 10.0) / 10.0, xOffset + posX, yOffset + 10);
            }
        }
        g.setColor(Color.black);
        g.drawLine(0, 0 + yOffset, w, 0 + yOffset);
        g.drawLine(0 + xOffset, 0, 0 + xOffset, h);
        g.setColor(Color.blue);
        for (int i = 0; i < x.length - 1; i++) {
            int posX = (int) (x[i].doubleValue() / windowProportionX);
            int posY = -(int) (y[i].doubleValue() / windowProportionY);
            g.setColor(Color.blue);
            if (chkShowLines.isSelected()) {
                int posNextX = (int) (x[i + 1].doubleValue() / windowProportionX);
                int posNextY = -(int) (y[i + 1].doubleValue() / windowProportionY);
                g.drawLine(posX + xOffset, posY + yOffset, posNextX + xOffset, posNextY + yOffset);
            } else {
                g.drawLine(posX + xOffset, posY + yOffset, posX + xOffset, posY + yOffset);
            }
            if (chkShowValues.isSelected()) {
                g.setColor(Color.black);
                String strX = "" + (Math.round(x[i].doubleValue() * 10.0) / 10.0);
                String strY = "" + (Math.round(y[i].doubleValue() * 10.0) / 10.0);
                g.fillRect(posX + xOffset - 1, posY + yOffset - 1, 2, 2);
                g.drawString("(" + strX + "," + strY + ")", posX + xOffset, posY + yOffset);
            }
        }
        g.dispose();
        updateGraphicImage();
    }

    void updateGraphicImage() {
        if (graphicImage != null) {
            rasaelImagePanel1.setImage(graphicImage);
        }
    }

    private void comboYActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void txtXActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void btnFunctionHelpActionPerformed(java.awt.event.ActionEvent evt) {
        String s = "";
        s += "Funzioni supportate:";
        s += "\n\t- log()";
        s += "\n\t- exp()";
        s += "\n\t- sin(),cos(),tan()";
        s += "\n\t- abs()";
        s += "\n\t- sinh(),cosh(),tanh()";
        s += "\n\t- sqrt()";
        s += "\n\t- asin(),acos(),atan()";
        s += "\n\t- asinh(),acosh(),atanh()";
        JOptionPane.showMessageDialog(this, s);
    }

    private void panDisplayMouseMoved(java.awt.event.MouseEvent evt) {
        double val = (evt.getX() - (panDisplay.getWidth() / 2.0)) * windowProportionX;
        String str = "" + Math.round(val * 100.0) / 100.0;
        if (str.length() > 6) {
            str = str.substring(0, 6);
        }
        lbllMouseX.setText(str);
        val = (evt.getY() - (panDisplay.getHeight() / 2.0)) * windowProportionY;
        str = "" + Math.round(val * 100.0) / -100.0;
        if (str.length() > 6) {
            str = str.substring(0, 6);
        }
        lblMouseY.setText(str);
    }

    private void btnEvalSimpleFourierActionPerformed(java.awt.event.ActionEvent evt) {
        Frazione T0 = calcolaEspressione(txtT0.getText());
        Frazione w = (new Frazione(2 * Math.PI)).divideBy(T0);
        txtOmega.setText("" + w);
        int maxN = Integer.parseInt(txtFourierMaxN.getText());
        boolean validFormula = true;
        validFormula = validFormula && MathEngine.isValidFormula(txtSimpleFourierA0.getText());
        validFormula = validFormula && MathEngine.isValidFormula(txtSimpleFourierAN.getText());
        validFormula = validFormula && MathEngine.isValidFormula(txtSimpleFourierBN.getText());
        if (!validFormula) {
            JOptionPane.showMessageDialog(this, "Errore: Le funzioni a0, an, bn non sono valide");
            return;
        }
        final int periods = Integer.parseInt(txtFourierPeriods.getText());
        Frazione from = T0.opposto().divideBy(2).multiplyBy(periods);
        Frazione to = T0.divideBy(2).multiplyBy(periods);
        Frazione step = calcolaEspressione(txtFourierStep.getText());
        int numeroDiPunti = (int) FMath.abs(from.subtract(to)).divideBy(step).doubleValue();
        Frazione t[] = new Frazione[numeroDiPunti];
        Frazione y[] = new Frazione[numeroDiPunti];
        for (int i = 0; i < numeroDiPunti; i++) {
            t[i] = from.add(step.multiplyBy(i));
            Frazione scos = new Frazione(0);
            Frazione ssin = new Frazione(0);
            Frazione tt = t[i];
            while (FMath.smallerThan(tt, from)) {
                tt = tt.add(T0);
            }
            while (FMath.greaterThan(tt, to)) {
                tt = tt.subtract(T0);
            }
            for (int n = 1; n < maxN; n++) {
                HashMap<String, Frazione> mappa = new HashMap<String, Frazione>();
                mappa.put("n", new Frazione(n));
                mappa.put("t", tt);
                mappa.put("w", w);
                String formula;
                formula = "(" + txtSimpleFourierAN.getText() + ")*cos(n*w*t)";
                scos = scos.add(valoreFunzione(formula, mappa));
                formula = "(" + txtSimpleFourierBN.getText() + ")*sin(n*w*t)";
                ssin = ssin.add(valoreFunzione(formula, mappa));
            }
            MathNode a0 = MathEngine.generaAlbero(txtSimpleFourierA0.getText());
            Frazione a0val = MathEngine.evaluateResult(a0, mappa("t", t[i]));
            y[i] = a0val.divideBy(2).add(scos).add(ssin);
        }
        disegnaGrafico(t, y);
    }

    private void btnSCSimpleFourierActionPerformed(java.awt.event.ActionEvent evt) {
        btnEvalSimpleFourierActionPerformed(null);
    }

    private void btnEvalCompactFourierActionPerformed(java.awt.event.ActionEvent evt) {
        Frazione T0 = calcolaEspressione(txtT0.getText());
        Frazione w = new Frazione(2 * Math.PI).divideBy(T0);
        txtOmega.setText("" + w);
        int maxN = Integer.parseInt(txtFourierMaxN.getText());
        boolean isValidFormula = true;
        isValidFormula &= MathEngine.isValidFormula(txtCompactFourierC0.getText());
        isValidFormula &= MathEngine.isValidFormula(txtCompactFourierCN.getText());
        isValidFormula &= MathEngine.isValidFormula(txtCompactFourierWN.getText());
        if (!isValidFormula) {
            JOptionPane.showMessageDialog(this, "Errore: Le funzioni C0, Cn, Wn non sono valide");
            return;
        }
        Frazione from = T0.opposto().divideBy(2.0);
        Frazione to = T0.divideBy(2.0);
        Frazione step = calcolaEspressione(txtFourierStep.getText());
        int numeroDiPunti = (int) FMath.abs(from.subtract(to)).divideBy(step).doubleValue();
        Frazione t[] = new Frazione[numeroDiPunti];
        Frazione y[] = new Frazione[numeroDiPunti];
        for (int i = 0; i < numeroDiPunti; i++) {
            t[i] = from.add(step.multiplyBy(i));
            Frazione scos = new Frazione(0);
            for (int n = 1; n < maxN; n++) {
                final HashMap<String, Frazione> mappa = new HashMap<String, Frazione>();
                mappa.put("n", new Frazione(n));
                mappa.put("t", t[i]);
                mappa.put("w", w);
                final String formula = "(" + txtCompactFourierCN.getText() + ")*cos(n*t+(" + txtCompactFourierWN.getText() + "))";
                scos = scos.add(valoreFunzione(formula, mappa));
            }
            final MathNode c0 = MathEngine.generaAlbero(txtCompactFourierC0.getText());
            final Frazione c0val = MathEngine.evaluateResult(c0, mappa("t", t[i]));
            y[i] = c0val.add(scos);
        }
        disegnaGrafico(t, y);
    }

    private void jButton23ActionPerformed(java.awt.event.ActionEvent evt) {
        txtCompactFourierC0.setText("(" + txtSimpleFourierA0.getText() + ")/2");
        if (calcolaEspressione(txtSimpleFourierAN.getText()).doubleValue() == 0.0) {
            txtCompactFourierCN.setText("ABS(" + txtSimpleFourierBN.getText() + ")");
        } else if (calcolaEspressione(txtSimpleFourierBN.getText()).doubleValue() == 0.0) {
            txtCompactFourierCN.setText("ABS(" + txtSimpleFourierAN.getText() + ")");
        } else {
            txtCompactFourierCN.setText("ABS(SQRT(" + "(" + txtSimpleFourierAN.getText() + ")^2+(" + txtSimpleFourierBN.getText() + ")^2))");
        }
        if (txtCompactFourierCN.getText().equals("0.0")) {
            txtCompactFourierWN.setText("0.0");
        } else {
            if (calcolaEspressione(txtSimpleFourierAN.getText()).doubleValue() == 0.0) {
                txtCompactFourierWN.setText("PI/2");
            } else {
                txtCompactFourierWN.setText("ATAN((-1*(" + txtSimpleFourierBN.getText() + "))/(" + txtSimpleFourierAN.getText() + "))");
            }
        }
    }

    private void jButton24ActionPerformed(java.awt.event.ActionEvent evt) {
        txtSimpleFourierA0.setText("2*(" + txtCompactFourierC0.getText() + ")");
        txtSimpleFourierAN.setText("(" + txtCompactFourierCN.getText() + ")*COS(" + txtCompactFourierWN.getText() + ")");
        txtSimpleFourierBN.setText("-1*(" + txtCompactFourierCN.getText() + ")*SIN(" + txtCompactFourierWN.getText() + ")");
    }

    private void btnEvalCompactFourier1ActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jButton25ActionPerformed(java.awt.event.ActionEvent evt) {
        txtSimpleFourierA0.setText("2*(" + txtCompactFourierC0.getText() + ")");
        txtSimpleFourierAN.setText("(" + txtCompactFourierCN.getText() + ")*COS(" + txtCompactFourierWN.getText() + ")");
        txtSimpleFourierBN.setText("-1*(" + txtCompactFourierCN.getText() + ")*SIN(" + txtCompactFourierWN.getText() + ")");
    }

    private void btnSCCompactFourierActionPerformed(java.awt.event.ActionEvent evt) {
        btnEvalCompactFourierActionPerformed(null);
    }

    private void btnAnalisiLoadWaveformActionPerformed(java.awt.event.ActionEvent evt) {
        FourierParam param = (FourierParam) comboAnalisiWaveforms.getSelectedItem();
        if (param.getType() == FourierParam.SIMPLE) {
            txtSimpleFourierA0.setText(param.a0);
            txtSimpleFourierAN.setText(param.an);
            txtSimpleFourierBN.setText(param.bn);
            txtT0.setText(param.t0);
        }
    }

    private void comboAvaiableLookAndFeelsActionPerformed(java.awt.event.ActionEvent evt) {
        SubstanceLookAndFeel.setSkin(((SkinInfo) comboAvaiableLookAndFeels.getSelectedItem()).getClassName());
    }

    private void btnGetValuesFromTableActionPerformed(java.awt.event.ActionEvent evt) {
        final int rowCount = tableValuesTable.getRowCount();
        if (rowCount > 0) {
            Frazione x[] = new Frazione[rowCount];
            Frazione y[] = new Frazione[rowCount];
            for (int i = 0; i < rowCount; i++) {
                x[i] = Frazione.parseFrazione(tableValuesTable.getModel().getValueAt(i, 0).toString());
                y[i] = Frazione.parseFrazione(tableValuesTable.getModel().getValueAt(i, 1).toString());
            }
            disegnaGrafico(x, y);
        } else {
            JOptionPane.showMessageDialog(this, "Non ci sono dati a sufficienza nella tabella");
        }
    }

    public Frazione f(Frazione a) {
        return valoreFunzione(f(), mappa("x", a));
    }

    public String f() {
        return comboY.getSelectedItem().toString();
    }

    private void btnIntegrationComputeActionPerformed(java.awt.event.ActionEvent evt) {
        Frazione a = calcolaEspressione(txtIntegrationFrom.getText());
        txtIntegrationFrom.setText("" + a);
        Frazione b = calcolaEspressione(txtIntegrationTo.getText());
        txtIntegrationTo.setText("" + b);
        Frazione result = new Frazione(0);
        long nanoTime = 0l;
        double eps = Math.pow(10.0, -12.0);
        switch(comboAlgorithm.getSelectedIndex()) {
            case 0:
                metodoSimpson: {
                    appendToBuffer("Integrating (Metodo Cavalieri-Simpson) <a href=\"#copyFormula:" + f() + "\">" + f() + "</a>");
                    nanoTime = System.nanoTime();
                    Frazione h = b.subtract(a).divideBy(2);
                    Frazione s1 = f(a).add(f(b));
                    Frazione s2 = new Frazione(0.0);
                    Frazione s4 = f(a.add(h));
                    int n = 2;
                    Frazione sNuovo = h.multiplyBy(s1.multiplyBy(s4.multiplyBy(4))).divideBy(3);
                    Frazione sVecchio = sNuovo;
                    do {
                        sVecchio = sNuovo;
                        n = 2 * n;
                        h = h.divideBy(2);
                        s2 = s2.add(s4);
                        s4 = new Frazione(0);
                        int j = 1;
                        do {
                            s4 = s4.add(f(a.add(h.multiplyBy(j))));
                            j = j + 2;
                        } while (j <= n);
                        sNuovo = h.multiplyBy(s1.add(s2.multiplyBy(2).add(s4.multiplyBy(4)))).divideBy(3);
                    } while (FMath.abs(sNuovo.subtract(sVecchio)).doubleValue() > eps);
                    nanoTime = System.nanoTime() - nanoTime;
                    result = sNuovo;
                }
                break;
            case 1:
                appendToBuffer("Integrating (Regola punto medio) <a href=\"#copyFormula:" + f() + "\">" + f() + "</a>");
                break;
            case 2:
                appendToBuffer("Integrating (Metodo romberg) <a href=\"#copyFormula:" + f() + "\">" + f() + "</a>");
                nanoTime = System.nanoTime();
                metodoRomberg: {
                    int precisione = Integer.parseInt(txtIntegrationN.getText());
                    Frazione[] t = new Frazione[precisione];
                    Frazione h = b.subtract(a);
                    Frazione s = f(a).add(f(b)).divideBy(2);
                    t[0] = s.multiplyBy(h);
                    int n = 1;
                    int i = 1;
                    Frazione vecchioValore = new Frazione(0);
                    Frazione vhj = new Frazione(0.0);
                    do {
                        n = n * 2;
                        h = h.divideBy(2);
                        int j = 1;
                        do {
                            s = s.add(f(a.add(h.multiplyBy(j))));
                            j = j + 2;
                        } while (j <= n);
                        t[i] = s.multiplyBy(h);
                        vhj = new Frazione(1);
                        for (int k = i - 1; k >= 0; k--) {
                            vhj = vhj.multiplyBy(4);
                            vecchioValore = t[k];
                            t[k] = t[k + 1].add(t[k + 1].subtract(vecchioValore).divideBy(vhj.subtract(1)));
                        }
                        i = i + 1;
                    } while (FMath.abs(vecchioValore.subtract(t[0])).doubleValue() > eps && i <= precisione - 1);
                    nanoTime = System.nanoTime() - nanoTime;
                    if (i > 15) {
                        JOptionPane.showMessageDialog(this, "Non è stata ottenuta la precisione voluta");
                    }
                    result = t[0];
                }
                break;
            case 3:
                {
                    metodoTrapezi: {
                        appendToBuffer("Integrating (Metodo trapezi) <a href=\"#copyFormula:" + f() + "\">" + f() + "</a>");
                        nanoTime = System.nanoTime();
                        int n = 1;
                        Frazione h = b.subtract(a);
                        Frazione T = h.multiplyBy(f(a).add(f(b))).divideBy(2);
                        Frazione M = new Frazione(0);
                        do {
                            M = new Frazione(0);
                            for (int i = 0; i < n - 1; i++) {
                                M = M.add(f(a.add(h.multiplyBy(i + 0.5d))));
                            }
                            M = M.multiplyBy(h);
                            T = T.add(M).divideBy(2);
                            h = h.divideBy(2);
                            n = 2 * n;
                        } while (FMath.abs(T.subtract(M)).doubleValue() > eps);
                        nanoTime = System.nanoTime() - nanoTime;
                        result = T;
                    }
                }
                break;
            case 4:
                {
                    appendToBuffer("Integrating (Metodo trapezi-b) <a href=\"#copyFormula:" + f() + "\">" + f() + "</a>");
                    nanoTime = System.nanoTime();
                    int n = 1;
                    Frazione h = b.subtract(a).divideBy(n);
                    Frazione somma = f(a);
                    for (int i = 1; i < n - 1; i++) {
                        somma = somma.add(f(a.add(h.multiplyBy(i))).multiplyBy(2));
                    }
                    somma = somma.add(f(b));
                    somma = somma.multiplyBy(h.divideBy(2));
                    nanoTime = System.nanoTime() - nanoTime;
                    result = somma;
                }
                break;
        }
        appendToBuffer("from a=" + a + " to b=" + b);
        int i = 0;
        final String[] unit = { "nanoseconds", "microseconds", "milliseconds", "seconds" };
        double dNanoTime = (double) nanoTime;
        while (dNanoTime > 1000 && i < 3) {
            dNanoTime /= 1000;
            i++;
        }
        appendToBuffer("=> <a href=\"#copyValue:" + result + "\">" + result + "</a> in ~" + round(dNanoTime, 2) + " " + unit[i]);
        txtFx.setText("" + result);
    }

    public static double round(double a, int cifre) {
        cifre = Math.max(cifre, 0);
        return Math.round(a * Math.pow(10, cifre)) / Math.pow(10, cifre);
    }

    long lastAction = 0;

    private void btnEVALScratchActionPerformed(java.awt.event.ActionEvent evt) {
        antiRebounce: {
            if (evt.getWhen() - lastAction < 300) {
                return;
            }
            lastAction = evt.getWhen();
        }
        String formula = comboUserInput.getSelectedItem().toString();
        String codedFormula = MathEngine.formulaToString(formula);
        String simplifiedFormula = MathEngine.formulaToString(MathEngine.evaluate(formula), false);
        if (chkSimplify.isSelected()) {
            simplifiedFormula = simplifiedFormula.replaceAll(" ", "");
            formula = simplifiedFormula;
        }
        appendToBuffer("f(x)=<font color=\"red\"><a href=\"#copyFormula:" + codedFormula + "\">" + formula + "</a></font>  [ " + simplifiedFormula + "]");
        try {
            calcolaEspressione(formula);
        } catch (NullPointerException e) {
            JOptionPane.showMessageDialog(this, "Espressione non riconocsciuta");
            return;
        }
        MathNode preliminaryResult = MathEngine.evaluate(formula);
        DefaultComboBoxModel model = (DefaultComboBoxModel) comboUserInput.getModel();
        model.addElement(formula);
        if (preliminaryResult instanceof NumberNode) {
            final NumberNode nNode = (NumberNode) preliminaryResult;
            appendToBuffer("\t=>" + nNode.getValue());
            return;
        } else {
            String[] listaVariabili = MathEngine.getVariables(formula);
            if (listaVariabili != null && listaVariabili.length > 0) {
                if (listaVariabili.length > 1) {
                    final HashMap<String, Frazione> mappa = new HashMap<String, Frazione>();
                    for (int i = 0; i < listaVariabili.length; i++) {
                        final String variabile = listaVariabili[i];
                        final String val = JOptionPane.showInputDialog(variabile + "=");
                        if (val == null || val.equals("")) {
                            return;
                        }
                        final Frazione d = calcolaEspressione(val);
                        mappa.put(variabile, d);
                    }
                    try {
                        final Frazione y = valoreFunzione(formula, mappa);
                        final Object[] keySet = mappa.keySet().toArray();
                        String str = "";
                        for (int i = 0; i < mappa.size(); i++) {
                            final String name = keySet[i].toString();
                            final Frazione val = mappa.get(keySet[i]);
                            if (i > 0) {
                                str += ", ";
                            }
                            str += name + "=" + val;
                        }
                        appendToBuffer("\twith " + str);
                        appendToBuffer("\t=> <a href=\"#copyValue:" + y + "\">" + y + "</a>");
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Formula non riconosciuta!");
                        return;
                    }
                } else {
                    appendToBuffer("SIMPLIFY()");
                    appendToBuffer("\t=>" + simplifiedFormula);
                }
                comboUserInput.setSelectedItem(simplifiedFormula);
            } else {
                formula = formula.toLowerCase();
                if (!(formula.startsWith("dec2bin") || formula.startsWith("dec2oct") || formula.startsWith("dec2hex") || formula.startsWith("dec2char") || formula.startsWith("bin2dec"))) {
                    appendToBuffer("Espressione non riconosciuta: '" + formula + "'");
                }
            }
        }
    }

    private void comboUserInputActionPerformed(java.awt.event.ActionEvent evt) {
        btnEVALScratchActionPerformed(evt);
    }

    private void btnTableRemoveAllActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel tmodel = new DefaultTableModel();
        tableValuesTable.setModel(tmodel);
    }

    private void btnTableRemoveActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private HashMap<String, Frazione> mappa(String variabile, Frazione valore) {
        HashMap<String, Frazione> mappa = new HashMap<String, Frazione>();
        mappa.put(variabile, valore);
        return mappa;
    }

    private DefaultMutableTreeNode generaJTree(MathNode i) {
        if (i != null) {
            DefaultMutableTreeNode pos = new DefaultMutableTreeNode(i);
            if (i.getSX() != null) {
                pos.add(generaJTree(i.getSX()));
            }
            if (i.getDX() != null) {
                pos.add(generaJTree(i.getDX()));
            }
            return pos;
        }
        return null;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                SubstanceLookAndFeel.setSkin(new GraphiteAquaSkin());
                new MainApp().setVisible(true);
            }
        });
    }

    private javax.swing.JButton btnAnalisiLoadWaveform;

    private javax.swing.JButton btnConstantsDefine;

    private javax.swing.JButton btnConstantsExport;

    private javax.swing.JButton btnConstantsImport;

    private javax.swing.JButton btnConstantsRemove;

    private javax.swing.JButton btnDisplay;

    private javax.swing.JButton btnEVALScratch;

    private javax.swing.JButton btnEval;

    private javax.swing.JButton btnEvalCompactFourier;

    private javax.swing.JButton btnEvalCompactFourier1;

    private javax.swing.JButton btnEvalSimpleFourier;

    private javax.swing.JButton btnFunctionHelp;

    private javax.swing.JButton btnGetValuesFromTable;

    private javax.swing.JButton btnIntegrationCompute;

    private javax.swing.JButton btnSCCompactFourier;

    private javax.swing.JButton btnSCComplexFourier;

    private javax.swing.JButton btnSCSimpleFourier;

    private javax.swing.JButton btnShowFunctionsList;

    private javax.swing.JButton btnTableExport;

    private javax.swing.JButton btnTableIMport;

    private javax.swing.JButton btnTableRemove;

    private javax.swing.JButton btnTableRemoveAll;

    private javax.swing.JCheckBox chkInvertiValori;

    private javax.swing.JCheckBox chkInvertiXY;

    private javax.swing.JCheckBox chkShowGrid;

    private javax.swing.JCheckBox chkShowLines;

    private javax.swing.JCheckBox chkShowSeparators;

    private javax.swing.JCheckBox chkShowValues;

    private javax.swing.JCheckBox chkSimplify;

    private javax.swing.JToggleButton chkTabellamentoRisultati;

    private javax.swing.JComboBox comboAlgorithm;

    private javax.swing.JComboBox comboAnalisiWaveforms;

    private javax.swing.JComboBox comboAvaiableLookAndFeels;

    private javax.swing.JComboBox comboUserInput;

    private javax.swing.JComboBox comboY;

    private javax.swing.JButton jButton23;

    private javax.swing.JButton jButton24;

    private javax.swing.JButton jButton25;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel12;

    private javax.swing.JLabel jLabel20;

    private javax.swing.JLabel jLabel21;

    private javax.swing.JLabel jLabel22;

    private javax.swing.JLabel jLabel23;

    private javax.swing.JLabel jLabel24;

    private javax.swing.JLabel jLabel25;

    private javax.swing.JLabel jLabel26;

    private javax.swing.JLabel jLabel27;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JLabel jLabel9;

    private javax.swing.JMenu jMenu1;

    private javax.swing.JMenu jMenu2;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel6;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JLabel lblAlgorithm;

    private javax.swing.JLabel lblBinaryTreeExpression;

    private javax.swing.JLabel lblCompressionHorizontal;

    private javax.swing.JLabel lblCompressionVertical;

    private javax.swing.JLabel lblDisplayFrom;

    private javax.swing.JLabel lblDisplayStep;

    private javax.swing.JLabel lblDisplayTo;

    private javax.swing.JLabel lblFY;

    private javax.swing.JLabel lblFourierMaxN;

    private javax.swing.JLabel lblFourierPeriods;

    private javax.swing.JLabel lblFourierStep;

    private javax.swing.JLabel lblFourierT0;

    private javax.swing.JLabel lblFx;

    private javax.swing.JLabel lblIntegrationFrom;

    private javax.swing.JLabel lblIntegrationN;

    private javax.swing.JLabel lblIntegrationTo;

    private javax.swing.JLabel lblMouseXLabel;

    private javax.swing.JLabel lblMouseY;

    private javax.swing.JLabel lblMouseYLabel;

    private javax.swing.JLabel lblPeriods;

    private javax.swing.JLabel lblRoundToDigits;

    private javax.swing.JLabel lblTranslationHorizontal;

    private javax.swing.JLabel lblTraslazioneVerticale;

    private javax.swing.JLabel lblX;

    private javax.swing.JLabel lblXOffset;

    private javax.swing.JLabel lblYOffset;

    private javax.swing.JLabel lbllMouseX;

    private javax.swing.JMenuItem mniMiniDifferentiator;

    private javax.swing.JPanel panDisplay;

    private javax.swing.JPanel panDisplayParam;

    private javax.swing.JTabbedPane panFourierSeries;

    private javax.swing.JPanel panIntegration;

    private javax.swing.JPanel panInterpolation;

    private javax.swing.JPanel panLookAndFeel;

    private javax.swing.JPanel panTraslazioni;

    private net.bervini.rasael.mathexplorer.swing.RasaelImagePanel rasaelImagePanel1;

    private javax.swing.JScrollPane scrollConstantsTable;

    private javax.swing.JScrollPane scrollExpressionTree;

    private javax.swing.JScrollPane scrollScratchText;

    private javax.swing.JScrollPane scrollValuesTable;

    private javax.swing.JPanel tabAlbero;

    private javax.swing.JPanel tabAnalisi;

    private javax.swing.JPanel tabCostanti;

    private javax.swing.JPanel tabFourierComplex;

    private javax.swing.JPanel tabFourierGeneral;

    private javax.swing.JPanel tabFourierReal;

    private javax.swing.JPanel tabMain;

    private javax.swing.JTabbedPane tabMaterie;

    private javax.swing.JPanel tabNumerica;

    private javax.swing.JPanel tabOptions;

    private javax.swing.JPanel tabScratchbook;

    private javax.swing.JPanel tabTabella;

    private javax.swing.JTable tableConstants;

    private javax.swing.JTable tableValuesTable;

    private javax.swing.JTree treeExpression;

    private javax.swing.JTextField txtBinaryTreeExpression;

    private javax.swing.JTextField txtCompactFourierC0;

    private javax.swing.JTextField txtCompactFourierCN;

    private javax.swing.JTextField txtCompactFourierWN;

    private javax.swing.JTextField txtComplexFourierD0;

    private javax.swing.JTextField txtComplexFourierDN;

    private javax.swing.JTextField txtComplexFourierWN;

    private javax.swing.JTextField txtCompressioneOrizzontale;

    private javax.swing.JTextField txtCompressioneVerticale;

    private javax.swing.JTextField txtDisplayFrom;

    private javax.swing.JTextField txtDisplayStep;

    private javax.swing.JTextField txtDisplayTo;

    private javax.swing.JTextField txtFourierMaxN;

    private javax.swing.JTextField txtFourierPeriods;

    private javax.swing.JTextField txtFourierStep;

    private javax.swing.JTextField txtFx;

    private javax.swing.JTextField txtIntegrationFrom;

    private javax.swing.JTextField txtIntegrationN;

    private javax.swing.JTextField txtIntegrationTo;

    private javax.swing.JTextField txtOmega;

    private javax.swing.JTextField txtOptionsRoundToDigits;

    private javax.swing.JEditorPane txtScratchbook;

    private javax.swing.JTextField txtSimpleFourierA0;

    private javax.swing.JTextField txtSimpleFourierAN;

    private javax.swing.JTextField txtSimpleFourierBN;

    private javax.swing.JTextField txtT0;

    private javax.swing.JTextField txtTraslazioneOrizzontale;

    private javax.swing.JTextField txtTraslazioneVerticale;

    private javax.swing.JTextField txtX;

    private javax.swing.JTextField txtXOffset;

    private javax.swing.JTextField txtYOffset;
}
