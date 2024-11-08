package gr.demokritos.iit.jinsect.gui;

import gr.demokritos.iit.conceptualIndex.LocalWordNetMeaningExtractor;
import gr.demokritos.iit.conceptualIndex.structs.Concatenation;
import gr.demokritos.iit.conceptualIndex.documentModel.DistributionDocument;
import gr.demokritos.iit.conceptualIndex.documentModel.SemanticIndex;
import gr.demokritos.iit.conceptualIndex.documentModel.SymbolicGraph;
import gr.demokritos.iit.conceptualIndex.structs.Union;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.IMatching;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.algorithms.estimators.DistanceEstimator;
import gr.demokritos.iit.jinsect.algorithms.estimators.NGramSizeEstimator;
import gr.demokritos.iit.jinsect.algorithms.statistics.statisticalCalculation;
import gr.demokritos.iit.jinsect.console.StatusConsole;
import gr.demokritos.iit.jinsect.console.StreamOutputConsole;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.structs.IntegerPair;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;
import gr.demokritos.iit.jinsect.threading.ThreadList;
import gr.demokritos.iit.jinsect.utils;
import gr.demokritos.iit.summarization.analysis.EntropyChunker;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;

/**
 *
 * @author  ggianna
 */
public class NGramCorrelationForm extends javax.swing.JFrame implements IMatching {

    DistributionDocument[] cdDoc;

    SymbolicGraph sgOverallGraph;

    SemanticIndex siIndex;

    private Date dLastUpdate = new Date();

    ArrayList lActiveActions = new ArrayList();

    TreeMap Delims = new TreeMap();

    EntropyChunker Chunker = new EntropyChunker();

    boolean RightToLeftText = false;

    public static final int MinLevel = 0;

    /** Creates new form NGramCorrelationForm */
    public NGramCorrelationForm() {
        initComponents();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        FilePathEdt = new javax.swing.JTextField();
        SelectInputFileBtn = new javax.swing.JButton();
        CreateNGramGraphBtn = new javax.swing.JButton();
        NGramSizeSldr = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        LogMemo = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        TermEdt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        LookUpTermBtn = new javax.swing.JButton();
        BreakDownBtn = new javax.swing.JButton();
        FindPathBtn = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        CorpusPercentSld = new javax.swing.JSlider();
        ClearGraphsBtn = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        SelectInputFileEdt = new javax.swing.JTextField();
        SelectTestFileBtn = new javax.swing.JButton();
        BreakFileDownBtn = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        SecondInputFileLbl = new javax.swing.JLabel();
        SelectSecondInputFileEdt = new javax.swing.JTextField();
        SelectSecondTestFileBtn = new javax.swing.JButton();
        CompareFilesBtn = new javax.swing.JButton();
        CancelAllBtn = new javax.swing.JButton();
        AnalyseCorpusBtn = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        DelimitersEdt = new javax.swing.JTextField();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("NGram Correlation Experiments");
        setName("NGramCorrelationFrame");
        getContentPane().setLayout(new java.awt.GridBagLayout());
        FilePathEdt.setText("/home/ggianna/Documents/JApplications/JInsect/conceptualCorpus");
        FilePathEdt.setName("FilePathEd");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(FilePathEdt, gridBagConstraints);
        SelectInputFileBtn.setText("Select input file");
        SelectInputFileBtn.setName("SelectInputFileBtn");
        SelectInputFileBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectInputFileBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(SelectInputFileBtn, gridBagConstraints);
        CreateNGramGraphBtn.setText("Create NGram Graphs");
        CreateNGramGraphBtn.setName("CreateGraphBtn");
        CreateNGramGraphBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreateNGramGraphBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 14;
        getContentPane().add(CreateNGramGraphBtn, gridBagConstraints);
        NGramSizeSldr.setMajorTickSpacing(5);
        NGramSizeSldr.setMaximum(22);
        NGramSizeSldr.setMinimum(2);
        NGramSizeSldr.setMinorTickSpacing(1);
        NGramSizeSldr.setPaintLabels(true);
        NGramSizeSldr.setPaintTicks(true);
        NGramSizeSldr.setValue(9);
        NGramSizeSldr.setName("NGramSizeSldr");
        NGramSizeSldr.setValueIsAdjusting(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(NGramSizeSldr, gridBagConstraints);
        jLabel1.setText("Max NGram Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        getContentPane().add(jLabel1, gridBagConstraints);
        jScrollPane1.setMaximumSize(null);
        jScrollPane1.setMinimumSize(null);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(300, 400));
        LogMemo.setColumns(20);
        LogMemo.setRows(5);
        LogMemo.setPreferredSize(null);
        jScrollPane1.setViewportView(LogMemo);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jScrollPane1, gridBagConstraints);
        jLabel2.setText("Graph training corpus");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        getContentPane().add(jLabel2, gridBagConstraints);
        TermEdt.setMinimumSize(new java.awt.Dimension(200, 19));
        TermEdt.setPreferredSize(new java.awt.Dimension(100, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        getContentPane().add(TermEdt, gridBagConstraints);
        jLabel3.setText("Select Term to Look up");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        getContentPane().add(jLabel3, gridBagConstraints);
        LookUpTermBtn.setText("Look up Term");
        LookUpTermBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LookUpTermBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(LookUpTermBtn, gridBagConstraints);
        BreakDownBtn.setText("Break term down");
        BreakDownBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BreakDownBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(BreakDownBtn, gridBagConstraints);
        FindPathBtn.setText("Test Search");
        FindPathBtn.setEnabled(false);
        FindPathBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FindPathBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(FindPathBtn, gridBagConstraints);
        jLabel4.setText("Select % of Corpus to Use");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        getContentPane().add(jLabel4, gridBagConstraints);
        CorpusPercentSld.setMajorTickSpacing(10);
        CorpusPercentSld.setMinorTickSpacing(5);
        CorpusPercentSld.setPaintLabels(true);
        CorpusPercentSld.setPaintTicks(true);
        CorpusPercentSld.setValue(100);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(CorpusPercentSld, gridBagConstraints);
        ClearGraphsBtn.setText("Clear Graphs");
        ClearGraphsBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearGraphsBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 14;
        getContentPane().add(ClearGraphsBtn, gridBagConstraints);
        jLabel5.setText("Select Input File");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 8;
        getContentPane().add(jLabel5, gridBagConstraints);
        SelectInputFileEdt.setText("/home/ggianna/Documents/JApplications/JInsect/conceptualCorpus");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(SelectInputFileEdt, gridBagConstraints);
        SelectTestFileBtn.setText("Select test file");
        SelectTestFileBtn.setName("SelectInputFileBtn");
        SelectTestFileBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectTestFileBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(SelectTestFileBtn, gridBagConstraints);
        BreakFileDownBtn.setText("Break file down");
        BreakFileDownBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BreakFileDownBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(BreakFileDownBtn, gridBagConstraints);
        jButton1.setText("Chunk String");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jButton1, gridBagConstraints);
        SecondInputFileLbl.setText("Select Second File");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 9;
        getContentPane().add(SecondInputFileLbl, gridBagConstraints);
        SelectSecondInputFileEdt.setText("/home/ggianna/Documents/JApplications/JInsect/conceptualCorpus");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(SelectSecondInputFileEdt, gridBagConstraints);
        SelectSecondTestFileBtn.setText("Select second file");
        SelectSecondTestFileBtn.setName("SelectInputFileBtn");
        SelectSecondTestFileBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectSecondTestFileBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(SelectSecondTestFileBtn, gridBagConstraints);
        CompareFilesBtn.setText("Compare Files");
        CompareFilesBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompareFilesBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(CompareFilesBtn, gridBagConstraints);
        CancelAllBtn.setText("Cancel All Processes");
        CancelAllBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CancelAllBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 15;
        getContentPane().add(CancelAllBtn, gridBagConstraints);
        AnalyseCorpusBtn.setText("Analyse Corpus");
        AnalyseCorpusBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AnalyseCorpusBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 14;
        getContentPane().add(AnalyseCorpusBtn, gridBagConstraints);
        jLabel6.setText("Delimiters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 13;
        getContentPane().add(jLabel6, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(DelimitersEdt, gridBagConstraints);
        pack();
    }

    private void AnalyseCorpusBtnActionPerformed(java.awt.event.ActionEvent evt) {
        Thread t = new Thread() {

            @Override
            public void run() {
                DoAnalyseCorpus();
                unregisterThread(this);
            }
        };
        registerThread(t);
        t.setPriority(Math.min(Thread.MAX_PRIORITY, t.getPriority() + 1));
        t.start();
    }

    private synchronized void registerThread(Thread t) {
        lActiveActions.add(t);
        System.out.println("Added " + t.getId() + ":" + t.getName());
    }

    private synchronized void unregisterThread(Thread t) {
        lActiveActions.remove(t);
        System.out.println("Removed " + t.getId() + ":" + t.getName());
    }

    private void CancelAllBtnActionPerformed(java.awt.event.ActionEvent evt) {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to stop running processes?", "Cancelling running processes", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Iterator iIter = lActiveActions.iterator();
            while (iIter.hasNext()) {
                Object oNext = iIter.next();
                if (oNext instanceof Thread) ((Thread) oNext).interrupt();
                iIter.remove();
            }
        }
    }

    private List KeepLeastSizeSets(List lSets) {
        return KeepLeastSizeSets(lSets, 0);
    }

    private List KeepLeastSizeSets(List lSets, int iMinContainedNGramSize) {
        ArrayList lRes = new ArrayList();
        int iMinSize = Integer.MAX_VALUE;
        Iterator iIter = lSets.iterator();
        while (iIter.hasNext()) {
            List lNext = (List) iIter.next();
            iMinSize = Math.min(iMinSize, lNext.size());
        }
        iIter = lSets.iterator();
        while (iIter.hasNext()) {
            List lNext = (List) iIter.next();
            if (lNext.size() == iMinSize) lRes.add(lNext);
        }
        if (iMinContainedNGramSize > 0) {
            iIter = lSets.iterator();
            while (iIter.hasNext()) {
                List lNext = (List) iIter.next();
                boolean bIsOK = true;
                Iterator iNGrams = lNext.iterator();
                while (iNGrams.hasNext()) if (((String) iNGrams.next()).length() < iMinContainedNGramSize) {
                    bIsOK = false;
                    break;
                }
                if (!bIsOK) iIter.remove();
            }
        }
        return lRes;
    }

    private void DoCompareFiles() {
        appendToLog("*** First file\n");
        ArrayList Meanings1 = new ArrayList();
        String sDataString = "";
        try {
            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            FileInputStream fiIn = new FileInputStream(SelectInputFileEdt.getText());
            int iData = 0;
            while ((iData = fiIn.read()) > -1) bsOut.write(iData);
            sDataString = bsOut.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        List<String> saSubtexts = Chunker.chunkString(sDataString);
        Iterator iStrIter = saSubtexts.iterator();
        int iCnt = 0, iMax = saSubtexts.size();
        final gr.demokritos.iit.jinsect.gui.StatusFrame fStatus = new gr.demokritos.iit.jinsect.gui.StatusFrame();
        fStatus.setVisible(true);
        while (iStrIter.hasNext()) {
            String sStr = (String) iStrIter.next();
            fStatus.setStatus("Checking: " + sStr, (double) iCnt++ / iMax);
            List lSubStrings = utils.getSubStrings(sStr, sStr.length(), this);
            if (lSubStrings.size() == 0) continue;
            appendToLog(utils.printList(lSubStrings));
            ArrayList lOptions = new ArrayList();
            lOptions.addAll(lSubStrings);
            Iterator iIter = lOptions.iterator();
            HashMap hSubstringSet = new HashMap();
            while (iIter.hasNext()) {
                Object oNext = iIter.next();
                List lNext;
                if (oNext instanceof List) {
                    lNext = (List) oNext;
                } else {
                    lNext = new ArrayList();
                    lNext.add(oNext);
                }
                if (hSubstringSet.containsKey(lNext.toString())) continue;
                appendToLog("Case " + utils.printList(lNext));
                hSubstringSet.put(lNext.toString(), 1);
                List lNodes = new ArrayList();
                Iterator iSubstrings = lNext.iterator();
                while (iSubstrings.hasNext()) {
                    lNodes.add(new VertexImpl(iSubstrings.next()));
                }
                Iterator iNodes = lNodes.iterator();
                String sUnionMeaning = "";
                while (iNodes.hasNext()) {
                    String sCur = ((Vertex) iNodes.next()).toString();
                    Object oTxt = siIndex.getMeaning(new VertexImpl(sCur));
                    if (oTxt != null) {
                        sUnionMeaning += "-" + SemanticIndex.meaningToString(oTxt) + "-";
                        Meanings1.add(oTxt);
                    } else appendToLog("No meaning found...");
                }
            }
        }
        fStatus.setVisible(false);
        appendToLog("*** Second file\n");
        ArrayList Meanings2 = new ArrayList();
        sDataString = "";
        try {
            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            FileInputStream fiIn = new FileInputStream(SelectSecondInputFileEdt.getText());
            int iData = 0;
            while ((iData = fiIn.read()) > -1) bsOut.write(iData);
            sDataString = bsOut.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        saSubtexts = Chunker.chunkString(sDataString);
        iStrIter = saSubtexts.iterator();
        while (iStrIter.hasNext()) {
            String sStr = (String) iStrIter.next();
            ArrayList lOptions = new ArrayList();
            List lSubStrings = utils.getSubStrings(sStr, sStr.length(), this, 2 * NGramSizeSldr.getValue());
            appendToLog(utils.printList(lSubStrings));
            lOptions.addAll(lSubStrings);
            Iterator iIter = lOptions.iterator();
            HashMap hSubstringSet = new HashMap();
            while (iIter.hasNext()) {
                Object oNext = iIter.next();
                List lNext;
                if (oNext instanceof List) {
                    lNext = (List) oNext;
                } else {
                    lNext = new ArrayList();
                    lNext.add(oNext);
                }
                if (hSubstringSet.containsKey(lNext.toString())) continue;
                appendToLog("Case " + utils.printList(lNext));
                hSubstringSet.put(lNext.toString(), 1);
                List lNodes = new ArrayList();
                Iterator iSubstrings = lNext.iterator();
                while (iSubstrings.hasNext()) {
                    lNodes.add(new VertexImpl(iSubstrings.next()));
                }
                Iterator iNodes = lNodes.iterator();
                String sUnionMeaning = "";
                while (iNodes.hasNext()) {
                    String sCur = ((Vertex) iNodes.next()).toString();
                    Object oTxt = siIndex.getMeaning(new VertexImpl(sCur));
                    if (oTxt != null) {
                        sUnionMeaning += "-" + siIndex.meaningToString(oTxt) + "-";
                        Meanings2.add(oTxt);
                    } else appendToLog("No meaning found...");
                }
            }
        }
        double dRes = 0.0;
        Iterator iIter1 = Meanings1.iterator();
        while (iIter1.hasNext()) {
            double dMaxSim = 0.0;
            Iterator iIter2 = Meanings2.iterator();
            WordDefinition d1 = (WordDefinition) iIter1.next();
            while (iIter2.hasNext()) {
                WordDefinition d2 = (WordDefinition) iIter2.next();
                dMaxSim = Math.max(dMaxSim, siIndex.compareWordDefinitions(d1, d2));
            }
            appendToLog("Concluded similarity of " + dMaxSim);
            dRes += dMaxSim;
        }
        dRes = 2 * dRes / (Meanings1.size() + Meanings2.size());
        appendToLog("Final Similarity : " + dRes);
    }

    private void CompareFilesBtnActionPerformed(java.awt.event.ActionEvent evt) {
        Thread t = new Thread() {

            @Override
            public void run() {
                DoCompareFiles();
                unregisterThread(this);
            }
        };
        registerThread(t);
        t.start();
    }

    private void SelectSecondTestFileBtnActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory((FilePathEdt.getText().length() == 0) ? new java.io.File(".") : new java.io.File(FilePathEdt.getText()));
        fc.setSelectedFile(fc.getCurrentDirectory());
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int iRet = fc.showOpenDialog(this);
        if (iRet == JFileChooser.APPROVE_OPTION) SelectSecondInputFileEdt.setText(fc.getSelectedFile().getAbsolutePath());
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        String sStr = TermEdt.getText();
        getSymbolsByEntropy(sStr, 3);
        appendToLog(gr.demokritos.iit.jinsect.utils.printList(Arrays.asList(chunkString(sStr))));
    }

    private void DoBreakFileDown() {
        String sDataString = "";
        try {
            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            FileInputStream fiIn = new FileInputStream(SelectInputFileEdt.getText());
            int iData = 0;
            while ((iData = fiIn.read()) > -1) bsOut.write(iData);
            sDataString = bsOut.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        List saSubtexts = Chunker.chunkString(sDataString);
        final gr.demokritos.iit.jinsect.gui.StatusFrame fStatus = new gr.demokritos.iit.jinsect.gui.StatusFrame();
        fStatus.setVisible(true);
        int iCnt = 0;
        try {
            Iterator iStrIter = saSubtexts.iterator();
            while (iStrIter.hasNext()) {
                String sStr = (String) iStrIter.next();
                fStatus.setStatus("Extracting meanings...", (double) (++iCnt) / saSubtexts.size());
                List lSubStrings = utils.getSubStrings(sStr, sStr.length(), this);
                if (lSubStrings.size() == 0) continue;
                appendToLog(utils.printList(lSubStrings));
                ArrayList lOptions = new ArrayList();
                lOptions.addAll(lSubStrings);
                Iterator iIter = lOptions.iterator();
                HashMap hSubstringSet = new HashMap();
                while (iIter.hasNext()) {
                    Object oNext = iIter.next();
                    List lNext;
                    if (oNext instanceof List) {
                        lNext = (List) oNext;
                    } else {
                        lNext = new ArrayList();
                        lNext.add(oNext);
                    }
                    if (hSubstringSet.containsKey(lNext.toString())) continue;
                    appendToLog("Case " + utils.printList(lNext));
                    hSubstringSet.put(lNext.toString(), 1);
                    List lNodes = new ArrayList();
                    Iterator iSubstrings = lNext.iterator();
                    while (iSubstrings.hasNext()) {
                        lNodes.add(new VertexImpl(iSubstrings.next()));
                    }
                    Iterator iNodes = lNodes.iterator();
                    String sUnionMeaning = "";
                    while (iNodes.hasNext()) {
                        String sCur = ((Vertex) iNodes.next()).toString();
                        Object oTxt = siIndex.getMeaning(new VertexImpl(sCur));
                        if (oTxt != null) sUnionMeaning += "-" + siIndex.meaningToString(oTxt) + "-"; else appendToLog("No meaning found...");
                    }
                }
            }
        } finally {
            fStatus.setVisible(false);
        }
    }

    private void BreakFileDownBtnActionPerformed(java.awt.event.ActionEvent evt) {
        Thread t = new Thread() {

            @Override
            public void run() {
                DoBreakFileDown();
                unregisterThread(this);
            }
        };
        registerThread(t);
        t.start();
    }

    private void SelectTestFileBtnActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory((FilePathEdt.getText().length() == 0) ? new java.io.File(".") : new java.io.File(FilePathEdt.getText()));
        fc.setSelectedFile(fc.getCurrentDirectory());
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int iRet = fc.showOpenDialog(this);
        if (iRet == JFileChooser.APPROVE_OPTION) SelectInputFileEdt.setText(fc.getSelectedFile().getAbsolutePath());
    }

    private void ClearGraphsBtnActionPerformed(java.awt.event.ActionEvent evt) {
        int Levels = Math.max(NGramSizeSldr.getValue(), 2);
        sgOverallGraph = new SymbolicGraph(1, Levels);
        Iterator iIter = Arrays.asList(cdDoc).iterator();
        while (iIter.hasNext()) {
            DistributionDocument d = (DistributionDocument) iIter.next();
            d.clearDocumentGraph();
        }
        siIndex = new SemanticIndex(sgOverallGraph);
        try {
            siIndex.MeaningExtractor = new LocalWordNetMeaningExtractor();
        } catch (IOException ioe) {
            siIndex.MeaningExtractor = null;
        }
    }

    private void FindPathBtnActionPerformed(java.awt.event.ActionEvent evt) {
        SymbolicGraph g = new SymbolicGraph(1, 9);
        g.setDataString("abcdefg");
        ArrayList alNodes = new ArrayList();
        alNodes.add(new VertexImpl("c"));
        alNodes.add(new VertexImpl("e"));
        alNodes.add(new VertexImpl("h"));
        Vertex vRes = g.getCommonSubnode(alNodes);
        boolean bFound;
        if (vRes != null) appendToLog(vRes.toString()); else appendToLog("No common child node found... Backtracking...");
        bFound = vRes != null;
        int iSize = alNodes.size();
        while (!bFound) {
            while (--iSize > 1) {
                Union uPossible = gr.demokritos.iit.jinsect.utils.getCombinationsBy(alNodes, iSize);
                Iterator iPossibleIter = uPossible.iterator();
                while (iPossibleIter.hasNext()) {
                    Concatenation cCur = (Concatenation) iPossibleIter.next();
                    vRes = g.getCommonSubnode(cCur);
                    if (vRes != null) {
                        appendToLog(vRes.toString());
                        bFound = true;
                    }
                }
                if (bFound) break;
            }
        }
        if (!bFound) appendToLog("No common child node found...");
    }

    public final boolean match(Object o) {
        String sStr = (String) o;
        if (sStr.length() < 2) return false;
        int iCnt = Math.min(cdDoc.length, sStr.length()) - 1;
        if (iCnt < sStr.length() - 1) return false;
        double dNorm = cdDoc[iCnt].normality(sStr);
        return dNorm >= 0.5;
    }

    private void BreakDownBtnActionPerformed(java.awt.event.ActionEvent evt) {
        String sStr = TermEdt.getText();
        List lSubStrings = utils.getSubStrings(sStr, sStr.length(), this);
        appendToLog(utils.printList(lSubStrings));
        ArrayList lOptions = new ArrayList();
        lOptions.addAll(lSubStrings);
        Iterator iIter = lOptions.iterator();
        HashMap hSubstringSet = new HashMap();
        while (iIter.hasNext()) {
            List lNext = (List) iIter.next();
            if (hSubstringSet.containsKey(lNext.toString())) continue;
            appendToLog("Case " + utils.printList(lNext));
            hSubstringSet.put(lNext.toString(), 1);
            List lNodes = new ArrayList();
            Iterator iSubstrings = lNext.iterator();
            while (iSubstrings.hasNext()) {
                lNodes.add(new VertexImpl(iSubstrings.next()));
            }
            int iAnswer = JOptionPane.showConfirmDialog(this, "Do you want to look up constituent nodes?");
            if (iAnswer == JOptionPane.NO_OPTION) continue;
            if (iAnswer == JOptionPane.CANCEL_OPTION) return;
            Vertex vRes = sgOverallGraph.getCommonSubnode(lNodes);
            boolean bFound;
            if (vRes != null) {
                appendToLog(vRes.toString());
                Object oTxt = siIndex.getMeaning(vRes);
                appendToLog(siIndex.meaningToString(oTxt));
            } else appendToLog("No common child node found... Backtracking...");
            bFound = vRes != null;
            int iSize = lNodes.size();
            while (!bFound) {
                while (--iSize > 1) {
                    Union uPossible = gr.demokritos.iit.jinsect.utils.getCombinationsBy(lNodes, iSize);
                    Iterator iPossibleIter = uPossible.iterator();
                    while (iPossibleIter.hasNext()) {
                        Concatenation cCur = (Concatenation) iPossibleIter.next();
                        vRes = sgOverallGraph.getCommonSubnode(cCur);
                        if (vRes != null) {
                            appendToLog(vRes.toString());
                            bFound = true;
                            Object oTxt = siIndex.getMeaning(vRes);
                            appendToLog(siIndex.meaningToString(oTxt));
                        }
                    }
                    if (bFound) break;
                }
                if (iSize <= 1) break;
            }
            if (!bFound) appendToLog("No common child node found...");
        }
    }

    private void LookUpTermBtnActionPerformed(java.awt.event.ActionEvent evt) {
        String sStr = TermEdt.getText();
        appendToLog("Looking up term:" + sStr);
        appendToLog("===============");
        for (int iCnt = 0; iCnt < Math.min(cdDoc.length, sStr.length()); iCnt++) appendToLog("Level " + String.valueOf(iCnt) + ":" + String.valueOf(cdDoc[iCnt].normality(sStr)));
        appendToLog("===DONE========\n");
    }

    private void appendToLog(final String s) {
        Runnable rQuick = new Runnable() {

            public void run() {
                LogMemo.append(s + "\n");
                System.err.println(s);
            }
        };
        dLastUpdate = new Date();
        try {
            SwingUtilities.invokeLater(rQuick);
        } catch (Exception e) {
        }
    }

    /** Extract language symbols from candidate text, based on entropy 
     * calculation.
     * 
     * @param sStr The string from which symbols are extracted.
     * @param iMaxSymbolSize The maximum size of extracted symbols.
     * @return A {@link List} indicating the extracted symbols.
     */
    private List getSymbolsByEntropy(String sStr, int iMaxSymbolSize) {
        ArrayList alRes = new ArrayList();
        String sSubStr;
        TreeMap tmRes = new TreeMap();
        for (int iNGramSize = 1; iNGramSize < iMaxSymbolSize; iNGramSize++) {
            for (int iCnt = 0; iCnt <= sStr.length() - iNGramSize; iCnt++) {
                if (iCnt + iNGramSize > sStr.length()) continue;
                sSubStr = sStr.substring(iCnt, iCnt + iNGramSize);
                Vertex vStrNode = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(sgOverallGraph, sSubStr);
                if (vStrNode == null) continue;
                List lEdges = gr.demokritos.iit.jinsect.utils.getOutgoingEdges(sgOverallGraph, vStrNode);
                Iterator iEdgeIter = lEdges.iterator();
                Distribution dDist = new Distribution();
                if (lEdges.size() > 0) {
                    while (iEdgeIter.hasNext()) {
                        WeightedEdge weCur = (WeightedEdge) iEdgeIter.next();
                        dDist.setValue(weCur.toString(), weCur.getWeight());
                    }
                    dDist.normalizeToSum();
                    double dEntropy = statisticalCalculation.entropy(dDist);
                    tmRes.put(dEntropy, sSubStr);
                }
            }
        }
        return alRes;
    }

    private String[] chunkString(String sStr) {
        return chunkString(sStr, 1);
    }

    private TreeMap identifyCandidateDelimiters(String sStr, int iNGramSize) {
        String sSubStr;
        Integer[] iRes;
        ArrayList alRes = new ArrayList();
        TreeMap tmRes = new TreeMap();
        for (int iCnt = 0; iCnt <= sStr.length() - iNGramSize; iCnt++) {
            if (iCnt + iNGramSize > sStr.length()) continue;
            sSubStr = sStr.substring(iCnt, iCnt + iNGramSize);
            if (tmRes.containsValue(sSubStr)) continue;
            Vertex vStrNode = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(sgOverallGraph, sSubStr);
            if (vStrNode == null) continue;
            double dEntropy = getEntropyOfNextChar(sSubStr, false);
            tmRes.put(dEntropy, sSubStr);
        }
        return tmRes;
    }

    private double getEntropyOfNextChar(String sStr) {
        return getEntropyOfNextChar(sStr, false);
    }

    private final double getEntropyOfNextChar(String sStr, boolean bNormalized) {
        double dRes = 0.0;
        Vertex vStrNode = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(sgOverallGraph, sStr);
        if (vStrNode == null) return dRes;
        List lEdges = gr.demokritos.iit.jinsect.utils.getOutgoingEdges(sgOverallGraph, vStrNode);
        Iterator iEdgeIter = lEdges.iterator();
        Distribution dDist = new Distribution();
        if (lEdges.size() > 0) {
            while (iEdgeIter.hasNext()) {
                WeightedEdge weCur = (WeightedEdge) iEdgeIter.next();
                if (Double.isNaN(weCur.getWeight())) System.err.println("WARNING: Not a number edge weight for edge:" + weCur.toString());
                dDist.setValue(weCur.toString(), weCur.getWeight());
            }
            dDist.normalizeToSum();
            if (bNormalized) {
                double dLogOccurences = (Math.log(dDist.calcTotalValues()) / Math.log(2));
                dRes = statisticalCalculation.entropy(dDist) / dLogOccurences;
            } else dRes = statisticalCalculation.entropy(dDist);
        }
        if (Double.isNaN(dRes)) System.err.println("WARNING: Not a number entropy for symbol:" + vStrNode);
        return dRes;
    }

    /** Returns the probability of generation of an exact given string, based on the alphabet of the 
     * Symbolic Graph, and given that there the generation process is random.
     *@param sString The string for which the probability of appearence is to be calculated.
     *@return The probability of appearence of the given string.
     */
    private final double getProbabilityOfStringInRandomText(String sPrefix, String sSuffix) {
        double dRes = 0.0;
        int iPrefixCount = 0;
        int iLastOccurence = -1;
        if (sPrefix.length() == 0) return 1.0;
        while ((iLastOccurence = sgOverallGraph.getDataString().indexOf(sPrefix, iLastOccurence + 1)) > -1) iPrefixCount++;
        int iFullStringCount = (int) Math.ceil((double) iPrefixCount * Math.pow(1.0 / sgOverallGraph.getAlphabet().size(), sSuffix.length()));
        double pPrefix = (double) iPrefixCount / (sgOverallGraph.getDataString().length() / sPrefix.length());
        String sFullString = sPrefix + sSuffix;
        double pJoined = (double) iFullStringCount / (sgOverallGraph.getDataString().length() / sFullString.length());
        dRes = (iPrefixCount == 0) ? 0.0 : pPrefix * pJoined;
        return dRes;
    }

    /** Returns the probability of occurence of a given suffix, given a prefix, within the data string
     *of the Symbolic Graph.
     *@param sPrefix The prefix required.
     *@param sSuffix The suffix for which the probability of occurence is to be calculated, given the
     * prefix.
     *@return The probability of occurence of the suffix, given the prefix.
     */
    private final double getProbabilityOfStringInText(String sPrefix, String sSuffix) {
        double dRes = 0.0;
        int iPrefixCount = 0;
        int iLastOccurence = -1;
        if (sPrefix.length() == 0) return 1.0;
        while ((iLastOccurence = sgOverallGraph.getDataString().indexOf(sPrefix, iLastOccurence + 1)) > -1) iPrefixCount++;
        String sFullString = sPrefix + sSuffix;
        int iFullStringCount = 0;
        iLastOccurence = -1;
        while ((iLastOccurence = sgOverallGraph.getDataString().indexOf(sFullString, iLastOccurence + 1)) > -1) iFullStringCount++;
        double pPrefix = (double) iPrefixCount / (sgOverallGraph.getDataString().length() / sPrefix.length());
        double pJoined = (double) iFullStringCount / (sgOverallGraph.getDataString().length() / sFullString.length());
        dRes = (iPrefixCount == 0) ? 0.0 : pPrefix * pJoined;
        return dRes;
    }

    private Integer[] splitPointsByDelimiterList(String sStr, char[] lDelimiters) {
        TreeMap tmDels = new TreeMap();
        for (int iCnt = 0; iCnt < lDelimiters.length; iCnt++) tmDels.put(iCnt, new String() + lDelimiters[iCnt]);
        return splitPointsByDelimiterList(sStr, tmDels);
    }

    private Integer[] splitPointsByDelimiterList(String sStr, SortedMap lDelimiters) {
        ArrayList alRes = new ArrayList();
        TreeMap lLocal = new TreeMap();
        lLocal.putAll(lDelimiters);
        while (lLocal.size() > 0) {
            Object oNext = lLocal.lastKey();
            int iNextSplit = 0;
            int iLastSplit = 0;
            while ((iNextSplit = sStr.indexOf((String) lDelimiters.get(oNext), iLastSplit)) > -1) {
                alRes.add(new Integer(iNextSplit + ((String) lDelimiters.get(oNext)).length()));
                iLastSplit = iNextSplit + 1;
            }
            lLocal.remove(oNext);
        }
        Integer[] iaRes = new Integer[alRes.size()];
        alRes.toArray(iaRes);
        gr.demokritos.iit.jinsect.utils.bubbleSortArray(iaRes);
        return iaRes;
    }

    private String[] chunkString(String sStr, int iNGramSize) {
        Integer[] iRes = splitPointsByDelimiterList(sStr, getDelimiters());
        String[] sRes = splitStringByDelimiterPoints(sStr, iRes);
        appendToLog("Text splitted into " + sRes.length + " chunks.");
        return sRes;
    }

    private double DetermineWMeanMutualInformationOf(String sStr, int iPos, int iMaxDistance, int iMinDistance, boolean bBefore) {
        double dMeanMutualInfo = 0.0;
        for (int iCurDist = iMinDistance; iCurDist < iMaxDistance; iCurDist++) {
            int iSubStrStart = Math.max(iPos - iCurDist, 0);
            int iSubStrEnd = Math.min(iPos + iCurDist, sStr.length());
            double dPrvEntropy, dCurEntropy;
            if (bBefore) {
                dPrvEntropy = getEntropyOfNextChar(sStr.substring(iSubStrStart, iPos));
                dCurEntropy = getEntropyOfNextChar(sStr.substring(iSubStrStart, iPos + 1));
            } else {
                dPrvEntropy = getEntropyOfNextChar(sStr.substring(iPos + 1, iSubStrEnd));
                dCurEntropy = getEntropyOfNextChar(sStr.substring(iPos, iSubStrEnd));
            }
            double dMutualInformationBefore = dCurEntropy - dPrvEntropy;
            dMeanMutualInfo += (iMaxDistance - iCurDist + 1) * dMutualInformationBefore;
        }
        int iSum = 0;
        for (int iCnt = iMinDistance; iCnt < iMaxDistance; iCnt++) iSum += iCnt;
        dMeanMutualInfo /= iSum;
        return dMeanMutualInfo;
    }

    private double DetermineMeanMutualInformationOf(String sStr, int iPos, int iMaxDistance, int iMinDistance, boolean bBefore) {
        double dMeanMutualInfo = 0.0;
        for (int iCurDist = iMinDistance; iCurDist < iMaxDistance; iCurDist++) {
            int iSubStrStart = Math.max(iPos - iCurDist, 0);
            int iSubStrEnd = Math.min(iPos + iCurDist, sStr.length());
            double dPrvEntropy, dCurEntropy;
            if (bBefore) {
                dPrvEntropy = getEntropyOfNextChar(sStr.substring(iSubStrStart, iPos));
                dCurEntropy = getEntropyOfNextChar(sStr.substring(iSubStrStart, iPos + 1));
            } else {
                dPrvEntropy = getEntropyOfNextChar(sStr.substring(iPos + 1, iSubStrEnd));
                dCurEntropy = getEntropyOfNextChar(sStr.substring(iPos, iSubStrEnd));
            }
            double dMutualInformationBefore = dCurEntropy - dPrvEntropy;
            dMeanMutualInfo += dMutualInformationBefore;
        }
        dMeanMutualInfo /= iMaxDistance - iMinDistance;
        return dMeanMutualInfo;
    }

    private Double[] DetermineMutualInformationDistributionOf(String sStr, int iPos, int iMaxDistance, int iMinDistance, boolean bBefore) {
        Distribution dRes = new Distribution();
        double dMeanMutualInfo = 0.0;
        for (int iCurDist = iMinDistance; iCurDist < iMaxDistance; iCurDist++) {
            int iSubStrStart = Math.max(iPos - iCurDist, 0);
            int iSubStrEnd = Math.min(iPos + iCurDist, sStr.length());
            double dPrvEntropy, dCurEntropy;
            if (bBefore) {
                dPrvEntropy = getEntropyOfNextChar(sStr.substring(iSubStrStart, iPos));
                dCurEntropy = getEntropyOfNextChar(sStr.substring(iSubStrStart, iPos + 1));
            } else {
                dPrvEntropy = getEntropyOfNextChar(sStr.substring(iPos + 1, iSubStrEnd));
                dCurEntropy = getEntropyOfNextChar(sStr.substring(iPos, iSubStrEnd));
            }
            double dMutualInformationBefore = dCurEntropy - dPrvEntropy;
            dRes.setValue(iCurDist, dMutualInformationBefore);
        }
        Double[] dblRes = new Double[dRes.asTreeMap().size()];
        dRes.asTreeMap().values().toArray(dblRes);
        return dblRes;
    }

    private Integer[] evalAndSelectActualDelimiterPoints(Integer[] iaDelimPoints, String sStr) {
        int iCurDelim = 0;
        ArrayList alList = new ArrayList();
        try {
            FileWriter fOut = new FileWriter("train.arff");
            HashMap hDelims = new HashMap();
            for (int iCnt = 0; iCnt < iaDelimPoints.length; iCnt++) {
                String sChar = sStr.substring(iaDelimPoints[iCnt], Math.min(iaDelimPoints[iCnt] + 1, sStr.length()));
                hDelims.put(String.valueOf(sChar.hashCode()), sChar);
            }
            fOut.write("@relation train" + System.getProperty("line.separator") + System.getProperty("line.separator"));
            fOut.write("@attribute delimChar {");
            String sLegend = "";
            Iterator iIter = hDelims.keySet().iterator();
            while (iIter.hasNext()) {
                String sNext = (String) iIter.next();
                fOut.write(sNext);
                if (Character.isISOControl(sNext.charAt(0)) || Character.isWhitespace(sNext.charAt(0))) sLegend += "% " + sNext + ": chr(" + sNext + ")" + System.getProperty("line.separator"); else sLegend += "% " + sNext + ": chr(" + hDelims.get(sNext) + ")" + System.getProperty("line.separator");
                if (iIter.hasNext()) fOut.write(",");
            }
            fOut.write("}" + System.getProperty("line.separator"));
            fOut.write("@attribute class {delim,nondelim}" + System.getProperty("line.separator"));
            for (int iCnt = 0; iCnt < NGramSizeSldr.getValue(); iCnt++) {
                fOut.write("@attribute entropyBefore" + iCnt + " real" + System.getProperty("line.separator"));
            }
            for (int iCnt = 0; iCnt < NGramSizeSldr.getValue(); iCnt++) {
                fOut.write("@attribute entropyAfter" + iCnt + " real" + System.getProperty("line.separator"));
            }
            fOut.write(System.getProperty("line.separator") + System.getProperty("line.separator"));
            fOut.write("@data" + System.getProperty("line.separator"));
            while (iCurDelim < iaDelimPoints.length) {
                double dMutualInformationBefore = DetermineMeanMutualInformationOf(sStr, iaDelimPoints[iCurDelim], Math.max(NGramSizeSldr.getValue(), 1), 1, true);
                double dMutualInformationAfter = DetermineMeanMutualInformationOf(sStr, iaDelimPoints[iCurDelim], Math.max(NGramSizeSldr.getValue(), 1), 1, false);
                int iSubStrStart = Math.max(iaDelimPoints[iCurDelim] - NGramSizeSldr.getValue(), 0);
                int iSubStrEnd = Math.min(iaDelimPoints[iCurDelim] + NGramSizeSldr.getValue(), sStr.length());
                fOut.write(sStr.substring(iaDelimPoints[iCurDelim], Math.min(iaDelimPoints[iCurDelim] + 1, sStr.length())).hashCode() + ",");
                if (" ()-\n".indexOf(sStr.substring(iaDelimPoints[iCurDelim], Math.min(iaDelimPoints[iCurDelim] + 1, sStr.length()))) > -1) fOut.write("delim,"); else fOut.write("nondelim,");
                Double[] dBefore = DetermineMutualInformationDistributionOf(sStr, iaDelimPoints[iCurDelim], Math.max(NGramSizeSldr.getValue(), 1), 1, true);
                Double[] dAfter = DetermineMutualInformationDistributionOf(sStr, iaDelimPoints[iCurDelim], Math.max(NGramSizeSldr.getValue(), 1), 1, false);
                String sMutualInformationBefore = "";
                if (dBefore.length < NGramSizeSldr.getValue()) for (int iCnt = dBefore.length; iCnt < NGramSizeSldr.getValue(); iCnt++) sMutualInformationBefore += "0.0,";
                for (int iCnt = 0; iCnt < dBefore.length; iCnt++) {
                    sMutualInformationBefore += dBefore[iCnt];
                    if (iCnt < dBefore.length - 1) sMutualInformationBefore += ",";
                }
                String sMutualInformationAfter = "";
                if (dAfter.length < NGramSizeSldr.getValue()) for (int iCnt = dAfter.length; iCnt < NGramSizeSldr.getValue(); iCnt++) sMutualInformationAfter += "0.0,";
                for (int iCnt = 0; iCnt < dAfter.length; iCnt++) {
                    sMutualInformationAfter += dAfter[iCnt];
                    if (iCnt < dAfter.length - 1) sMutualInformationAfter += ",";
                }
                fOut.write(sMutualInformationBefore + "," + sMutualInformationAfter + System.getProperty("line.separator"));
                fOut.flush();
                if ((dMutualInformationBefore > 1) && (dMutualInformationAfter < 1)) {
                    alList.add(iaDelimPoints[iCurDelim]);
                }
                ++iCurDelim;
            }
            fOut.close();
        } catch (IOException ioe) {
            System.out.println("Output failed.");
        }
        Integer[] iaRes = new Integer[alList.size()];
        iaRes = (Integer[]) alList.toArray(iaRes);
        return iaRes;
    }

    private int determineImportantDelimiters(SortedMap smMap) {
        Iterator iIter = smMap.keySet().iterator();
        Distribution dDist = new Distribution();
        Distribution dReverse = new Distribution();
        Double dPrv = Double.NEGATIVE_INFINITY;
        Double dTwoPrv = Double.NEGATIVE_INFINITY;
        while (iIter.hasNext()) {
            Double oNext = (Double) iIter.next();
            if ((dPrv != Double.NEGATIVE_INFINITY) && (dTwoPrv != Double.NEGATIVE_INFINITY)) {
                if (oNext.isNaN()) System.err.println("WARNING: Encountered NaN. Ignoring...");
                dDist.setValue(dPrv, dPrv * Math.abs(dPrv - dTwoPrv - oNext + dPrv));
                dReverse.setValue(dPrv * Math.abs(dPrv - dTwoPrv - oNext + dPrv), dPrv);
            }
            dTwoPrv = dPrv;
            dPrv = oNext;
        }
        double dVar = dDist.variance(true);
        double dMean = dDist.average(true);
        return getDelimiterIndexByThreshold(smMap, dReverse.getValue(dDist.maxValue()));
    }

    private int getDelimiterIndexByThreshold(SortedMap smMap, double dThreshold) {
        Iterator iIter = smMap.keySet().iterator();
        int iCnt = 0;
        while (iIter.hasNext()) {
            if ((Double) iIter.next() > dThreshold) break;
            iCnt++;
        }
        return smMap.size() - iCnt + 1;
    }

    private void DoAnalyseCorpus() {
        gr.demokritos.iit.jinsect.gui.StatusFrame fStatus = new gr.demokritos.iit.jinsect.gui.StatusFrame();
        analyseCorpus(fStatus);
        fStatus.dispose();
    }

    private double determineDistanceDeviation() {
        double dRes = 0.0;
        SortedMap smDelims = identifyCandidateDelimiters(sgOverallGraph.getDataString(), 1);
        int iImportant = determineImportantDelimiters(smDelims);
        Iterator iIter = smDelims.keySet().iterator();
        int iCnt = 0;
        while (iIter.hasNext() && (iCnt++ < smDelims.size() - iImportant)) iIter.next();
        smDelims = smDelims.tailMap(iIter.next());
        if (!smDelims.containsValue(StreamTokenizer.TT_EOF)) {
            smDelims.put((Double) smDelims.lastKey() + 0.1, new StringBuffer().append((char) StreamTokenizer.TT_EOF).toString());
        }
        String[] saChunks = splitStringByDelimiterPoints(sgOverallGraph.getDataString(), splitPointsByDelimiterList(sgOverallGraph.getDataString(), smDelims));
        Distribution dSizes = new Distribution();
        for (iCnt = 0; iCnt < saChunks.length; iCnt++) {
            dSizes.setValue((double) saChunks[iCnt].length(), dSizes.getValue((double) saChunks[iCnt].length()) + 1);
        }
        dRes = dSizes.average(false) + dSizes.standardDeviation(false);
        return dRes;
    }

    private void analyseCorpus(final IStatusDisplayer fStatus) {
        final String sDistrosFile = "Distros.tmp";
        final String sSymbolsFile = "Symbols.tmp";
        Chunker = new EntropyChunker();
        int Levels = 2;
        sgOverallGraph = new SymbolicGraph(1, Levels);
        siIndex = new SemanticIndex(sgOverallGraph);
        try {
            siIndex.MeaningExtractor = new LocalWordNetMeaningExtractor();
        } catch (IOException ioe) {
            siIndex.MeaningExtractor = null;
        }
        try {
            DocumentSet dsSet = new DocumentSet(FilePathEdt.getText(), 1.0);
            dsSet.createSets(true, (double) 100 / 100);
            int iCurCnt, iTotal;
            String sFile = "";
            Iterator iIter = dsSet.getTrainingSet().iterator();
            iTotal = dsSet.getTrainingSet().size();
            if (iTotal == 0) {
                appendToLog("No input documents.\n");
                appendToLog("======DONE=====\n");
                return;
            }
            appendToLog("Training chunker...");
            Chunker.train(dsSet.toFilenameSet(DocumentSet.FROM_WHOLE_SET));
            appendToLog("Setting delimiters...");
            setDelimiters(Chunker.getDelimiters());
            iCurCnt = 0;
            cdDoc = new DistributionDocument[Levels];
            for (int iCnt = 0; iCnt < Levels; iCnt++) cdDoc[iCnt] = new DistributionDocument(1, MinLevel + iCnt);
            fStatus.setVisible(true);
            ThreadList t = new ThreadList(Runtime.getRuntime().availableProcessors() + 1);
            appendToLog("(Pass 1/3) Loading files..." + sFile);
            TreeSet tsOverallSymbols = new TreeSet();
            while (iIter.hasNext()) {
                sFile = ((CategorizedFileEntry) iIter.next()).getFileName();
                fStatus.setStatus("(Pass 1/3) Loading file..." + sFile, (double) iCurCnt / iTotal);
                final DistributionDocument[] cdDocArg = cdDoc;
                final String sFileArg = sFile;
                for (int iCnt = 0; iCnt < cdDoc.length; iCnt++) {
                    final int iCntArg = iCnt;
                    while (!t.addThreadFor(new Runnable() {

                        public void run() {
                            if (!RightToLeftText) cdDocArg[iCntArg].loadDataStringFromFile(sFileArg, false); else {
                                cdDocArg[iCntArg].setDataString(utils.reverseString(utils.loadFileToString(sFileArg)), iCntArg, false);
                            }
                        }
                    })) Thread.yield();
                }
                try {
                    t.waitUntilCompletion();
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                    appendToLog("Interrupted...");
                    sgOverallGraph.removeNotificationListener();
                    return;
                }
                sgOverallGraph.setDataString(((new StringBuffer().append((char) StreamTokenizer.TT_EOF))).toString());
                sgOverallGraph.loadFromFile(sFile);
                fStatus.setStatus("Loaded file..." + sFile, (double) ++iCurCnt / iTotal);
                Thread.yield();
            }
            Set sSymbols = null;
            File fPreviousSymbols = new File(sSymbolsFile);
            boolean bSymbolsLoadedOK = false;
            if (fPreviousSymbols.exists()) {
                System.err.println("ATTENTION: Using previous symbols...");
                try {
                    FileInputStream fis = new FileInputStream(fPreviousSymbols);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    sSymbols = (Set) ois.readObject();
                    ois.close();
                    bSymbolsLoadedOK = true;
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace(System.err);
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }
            }
            if (!bSymbolsLoadedOK) sSymbols = getSymbolsByProbabilities(sgOverallGraph.getDataString(), fStatus);
            int iMinSymbolSize = Integer.MAX_VALUE;
            int iMaxSymbolSize = Integer.MIN_VALUE;
            Iterator iSymbol = sSymbols.iterator();
            while (iSymbol.hasNext()) {
                String sCurSymbol = (String) iSymbol.next();
                if (iMaxSymbolSize < sCurSymbol.length()) iMaxSymbolSize = sCurSymbol.length();
                if (iMinSymbolSize > sCurSymbol.length()) iMinSymbolSize = sCurSymbol.length();
            }
            try {
                FileOutputStream fos = new FileOutputStream(sSymbolsFile);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(sSymbols);
                oos.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace(System.err);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            appendToLog("(Pass 2/3) Determining symbol distros per n-gram size...");
            iIter = dsSet.getTrainingSet().iterator();
            iTotal = dsSet.getTrainingSet().size();
            if (iTotal == 0) {
                appendToLog("No input documents.\n");
                appendToLog("======DONE=====\n");
                return;
            }
            iCurCnt = 0;
            Distribution dSymbolsPerSize = new Distribution();
            Distribution dNonSymbolsPerSize = new Distribution();
            Distribution dSymbolSizes = new Distribution();
            File fPreviousRun = new File(sDistrosFile);
            boolean bDistrosLoadedOK = false;
            if (fPreviousRun.exists()) {
                System.err.println("ATTENTION: Using previous distros...");
                try {
                    FileInputStream fis = new FileInputStream(fPreviousRun);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    dSymbolsPerSize = (Distribution) ois.readObject();
                    dNonSymbolsPerSize = (Distribution) ois.readObject();
                    dSymbolSizes = (Distribution) ois.readObject();
                    ois.close();
                    bDistrosLoadedOK = true;
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace(System.err);
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                    dSymbolsPerSize = new Distribution();
                    dNonSymbolsPerSize = new Distribution();
                    dSymbolSizes = new Distribution();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                    dSymbolsPerSize = new Distribution();
                    dNonSymbolsPerSize = new Distribution();
                    dSymbolSizes = new Distribution();
                }
            }
            if (!bDistrosLoadedOK) while (iIter.hasNext()) {
                fStatus.setStatus("(Pass 2/3) Parsing file..." + sFile, (double) iCurCnt++ / iTotal);
                sFile = ((CategorizedFileEntry) iIter.next()).getFileName();
                String sDataString = "";
                try {
                    ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
                    FileInputStream fiIn = new FileInputStream(sFile);
                    int iData = 0;
                    while ((iData = fiIn.read()) > -1) bsOut.write(iData);
                    sDataString = bsOut.toString();
                } catch (IOException ioe) {
                    ioe.printStackTrace(System.err);
                }
                final Distribution dSymbolsPerSizeArg = dSymbolsPerSize;
                final Distribution dNonSymbolsPerSizeArg = dNonSymbolsPerSize;
                final Distribution dSymbolSizesArg = dSymbolSizes;
                final String sDataStringArg = sDataString;
                final Set sSymbolsArg = sSymbols;
                for (int iSymbolSize = iMinSymbolSize; iSymbolSize <= iMaxSymbolSize; iSymbolSize++) {
                    final int iSymbolSizeArg = iSymbolSize;
                    while (!t.addThreadFor(new Runnable() {

                        public void run() {
                            NGramDocument ndCur = new NGramDocument(iSymbolSizeArg, iSymbolSizeArg, 1, iSymbolSizeArg, iSymbolSizeArg);
                            ndCur.setDataString(sDataStringArg);
                            int iSymbolCnt = 0;
                            int iNonSymbolCnt = 0;
                            Iterator iExtracted = ndCur.getDocumentGraph().getGraphLevel(0).getVertexSet().iterator();
                            while (iExtracted.hasNext()) {
                                String sCur = ((Vertex) iExtracted.next()).toString();
                                if (sSymbolsArg.contains(sCur)) {
                                    iSymbolCnt++;
                                    synchronized (dSymbolSizesArg) {
                                        dSymbolSizesArg.setValue(sCur.length(), dSymbolSizesArg.getValue(sCur.length()) + 1.0);
                                    }
                                } else iNonSymbolCnt++;
                            }
                            synchronized (dSymbolsPerSizeArg) {
                                dSymbolsPerSizeArg.setValue(iSymbolSizeArg, dSymbolsPerSizeArg.getValue(iSymbolSizeArg) + iSymbolCnt);
                            }
                            synchronized (dNonSymbolsPerSizeArg) {
                                dNonSymbolsPerSizeArg.setValue(iSymbolSizeArg, dNonSymbolsPerSizeArg.getValue(iSymbolSizeArg) + iNonSymbolCnt);
                            }
                        }
                    })) Thread.yield();
                }
            }
            if (!bDistrosLoadedOK) try {
                t.waitUntilCompletion();
                try {
                    FileOutputStream fos = new FileOutputStream(sDistrosFile);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(dSymbolsPerSize);
                    oos.writeObject(dNonSymbolsPerSize);
                    oos.writeObject(dSymbolSizes);
                    oos.close();
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace(System.err);
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            } catch (InterruptedException ex) {
                appendToLog("Interrupted...");
                sgOverallGraph.removeNotificationListener();
                return;
            }
            appendToLog("\n(Pass 3/3) Determining optimal n-gram range...\n");
            NGramSizeEstimator nseEstimator = new NGramSizeEstimator(dSymbolsPerSize, dNonSymbolsPerSize);
            IntegerPair p = nseEstimator.getOptimalRange();
            appendToLog("\nProposed n-gram sizes:" + p.first() + "," + p.second());
            fStatus.setStatus("Determining optimal distance...", 0.0);
            DistanceEstimator de = new DistanceEstimator(dSymbolsPerSize, dNonSymbolsPerSize, nseEstimator);
            int iBestDist = de.getOptimalDistance(1, nseEstimator.getMaxRank() * 2, p.first(), p.second());
            fStatus.setStatus("Determining optimal distance...", 1.0);
            appendToLog("\nOptimal distance:" + iBestDist);
            appendToLog("======DONE=====\n");
        } finally {
            sgOverallGraph.removeNotificationListener();
        }
    }

    private void DoCreateNGramGraph() {
        final gr.demokritos.iit.jinsect.gui.StatusFrame fStatus = new gr.demokritos.iit.jinsect.gui.StatusFrame();
        int Levels = Math.max(NGramSizeSldr.getValue(), 2);
        sgOverallGraph = new SymbolicGraph(1, Levels);
        siIndex = new SemanticIndex(sgOverallGraph);
        try {
            siIndex.MeaningExtractor = new LocalWordNetMeaningExtractor();
        } catch (IOException ioe) {
            siIndex.MeaningExtractor = null;
        }
        try {
            DocumentSet dsSet = new DocumentSet(FilePathEdt.getText(), 1.0);
            dsSet.createSets(true, (double) CorpusPercentSld.getValue() / 100);
            int iCurCnt, iTotal;
            String sFile = "";
            Iterator iIter = dsSet.getTrainingSet().iterator();
            iTotal = dsSet.getTrainingSet().size();
            if (iTotal == 0) {
                appendToLog("No input documents.\n");
                appendToLog("======DONE=====\n");
                return;
            }
            iCurCnt = 0;
            cdDoc = new DistributionDocument[Levels];
            for (int iCnt = 0; iCnt < Levels; iCnt++) cdDoc[iCnt] = new DistributionDocument(1, MinLevel + iCnt);
            fStatus.setVisible(true);
            while (iIter.hasNext()) {
                sFile = ((CategorizedFileEntry) iIter.next()).getFileName();
                appendToLog("Loading file..." + sFile);
                fStatus.setStatus("Loading file..." + sFile, (double) iCurCnt / iTotal);
                for (int iCnt = 0; iCnt < cdDoc.length; iCnt++) {
                    cdDoc[iCnt].loadDataStringFromFile(sFile, false);
                }
                sgOverallGraph.loadFromFile(sFile);
                appendToLog("Loaded file..." + sFile);
                fStatus.setStatus("Loaded file..." + sFile, (double) ++iCurCnt / iTotal);
                Thread.yield();
            }
            appendToLog("======DONE=====\n");
        } finally {
            sgOverallGraph.removeNotificationListener();
            fStatus.dispose();
        }
    }

    private void CreateNGramGraphBtnActionPerformed(java.awt.event.ActionEvent evt) {
        Thread t = new Thread() {

            @Override
            public void run() {
                DoCreateNGramGraph();
                unregisterThread(this);
            }
        };
        registerThread(t);
        t.setPriority(Math.min(Thread.MAX_PRIORITY, t.getPriority() + 1));
        t.start();
    }

    private void SelectInputFileBtnActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory((FilePathEdt.getText().length() == 0) ? new java.io.File(".") : new java.io.File(FilePathEdt.getText()));
        fc.setSelectedFile(fc.getCurrentDirectory());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int iRet = fc.showOpenDialog(this);
        if (iRet == JFileChooser.APPROVE_OPTION) FilePathEdt.setText(fc.getSelectedFile().getAbsolutePath());
    }

    private static String[] splitStringByDelimiterPoints(String sStr, Integer[] iRes) {
        ArrayList alRes = new ArrayList();
        for (int iCnt = 0; iCnt < iRes.length; iCnt++) {
            if (iCnt == 0) alRes.add(sStr.substring(0, iRes[iCnt])); else alRes.add(sStr.substring(iRes[iCnt - 1], iRes[iCnt]));
        }
        if (iRes.length > 0) alRes.add(sStr.substring(iRes[iRes.length - 1])); else alRes.add(sStr);
        String[] sRes = new String[alRes.size()];
        alRes.toArray(sRes);
        return sRes;
    }

    private final synchronized void setDelimiters(SortedMap smDelims) {
        String sRes = "";
        Iterator iIter = smDelims.keySet().iterator();
        while (iIter.hasNext()) {
            Object oNext = smDelims.get(iIter.next());
            sRes = oNext.toString() + sRes;
        }
        try {
            DelimitersEdt.setText(sRes);
        } catch (Exception e) {
            System.err.println("Cannot update edit (probably due to strange encoding). Continuing...");
        }
        Delims.clear();
        Delims.putAll(smDelims);
    }

    public final synchronized SortedMap getDelimiters() {
        return Delims;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        if (args.length == 0) java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new NGramCorrelationForm().setVisible(true);
            }
        }); else {
            StatusConsole sc = new StatusConsole(80);
            Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
            int iOutputNGramMinSize = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "outputSymbolsOfMinSize", "-1")).intValue();
            int iOutputNGramMaxSize = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "outputSymbolsOfMaxSize", String.valueOf(iOutputNGramMinSize + 10))).intValue();
            String sSymbolFile = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "symbolFile", "Symbols.tmp");
            boolean bRightToLeftText = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "rightToLeft", String.valueOf(false))).booleanValue();
            if (iOutputNGramMinSize > 0) {
                int iCount = printOutSymbolsOfSize(iOutputNGramMinSize, iOutputNGramMaxSize, sSymbolFile, new StreamOutputConsole(System.out, false));
                System.out.println("Total: " + iCount);
            } else {
                if (bRightToLeftText) {
                    System.err.println("Performing right to left analysis...");
                }
                NGramCorrelationForm nfMain = new NGramCorrelationForm();
                nfMain.RightToLeftText = bRightToLeftText;
                String sInputDir = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "dir", "./DUC/reducedModels2005/");
                nfMain.FilePathEdt.setText(sInputDir);
                nfMain.analyseCorpus(sc);
                nfMain.dispose();
            }
        }
    }

    private Set getSymbolsByProbabilities(String sText, IStatusDisplayer fStatus) {
        StringBuffer sbSubStr = new StringBuffer();
        TreeSet tsRes = new TreeSet();
        Date dStartTime = new Date();
        for (int iCnt = 0; iCnt < sText.length(); iCnt++) {
            String sNextChar = sText.substring(iCnt, iCnt + 1);
            if ((sbSubStr.length() == 0) || (getProbabilityOfStringInText(sbSubStr.toString(), sNextChar) > getProbabilityOfStringInRandomText(sbSubStr.toString(), sNextChar))) sbSubStr.append(sText.charAt(iCnt)); else {
                tsRes.add(sbSubStr.toString());
                sbSubStr = new StringBuffer(sNextChar);
            }
            Date dCurTime = new Date();
            long lRemaining = (sText.length() - iCnt + 1) * (long) ((double) (dCurTime.getTime() - dStartTime.getTime()) / iCnt);
            String sRemaining = String.format(" - Remaining: %40s\r", gr.demokritos.iit.jinsect.utils.millisToMinSecString(lRemaining));
            fStatus.setStatus("Determining corpus symbols..." + sRemaining, (double) iCnt / sText.length());
        }
        if (sbSubStr.length() > 0) tsRes.add(sbSubStr.toString());
        return tsRes;
    }

    public static int printOutSymbolsOfSize(int iMinSize, int iMaxSize, String sSymbolsFile, IStatusDisplayer isStatus) {
        Set sSymbols = null;
        File fPreviousSymbols = new File(sSymbolsFile);
        boolean bSymbolsLoadedOK = false;
        int iCurCnt = 0;
        if (fPreviousSymbols.exists()) {
            try {
                FileInputStream fis = new FileInputStream(fPreviousSymbols);
                ObjectInputStream ois = new ObjectInputStream(fis);
                sSymbols = (Set) ois.readObject();
                ois.close();
                bSymbolsLoadedOK = true;
            } catch (FileNotFoundException ex) {
                ex.printStackTrace(System.err);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace(System.err);
            }
            Iterator iSymbolIter = sSymbols.iterator();
            int iTotalSize = sSymbols.size();
            while (iSymbolIter.hasNext()) {
                String sCur = (String) iSymbolIter.next();
                int iLen = sCur.length();
                if ((iLen >= iMinSize) && (iLen <= iMaxSize)) {
                    isStatus.setStatus(sCur, (double) iCurCnt / iTotalSize);
                    iCurCnt++;
                }
            }
        }
        return iCurCnt;
    }

    private javax.swing.JButton AnalyseCorpusBtn;

    private javax.swing.JButton BreakDownBtn;

    private javax.swing.JButton BreakFileDownBtn;

    private javax.swing.JButton CancelAllBtn;

    private javax.swing.JButton ClearGraphsBtn;

    private javax.swing.JButton CompareFilesBtn;

    private javax.swing.JSlider CorpusPercentSld;

    private javax.swing.JButton CreateNGramGraphBtn;

    private javax.swing.JTextField DelimitersEdt;

    private javax.swing.JTextField FilePathEdt;

    private javax.swing.JButton FindPathBtn;

    private javax.swing.JTextArea LogMemo;

    private javax.swing.JButton LookUpTermBtn;

    private javax.swing.JSlider NGramSizeSldr;

    private javax.swing.JLabel SecondInputFileLbl;

    private javax.swing.JButton SelectInputFileBtn;

    private javax.swing.JTextField SelectInputFileEdt;

    private javax.swing.JTextField SelectSecondInputFileEdt;

    private javax.swing.JButton SelectSecondTestFileBtn;

    private javax.swing.JButton SelectTestFileBtn;

    private javax.swing.JTextField TermEdt;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JScrollPane jScrollPane1;
}
