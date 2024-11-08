package oxil.action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import oxil.ApplicationContext;
import oxil.entity.ConfiguracaoROM;
import oxil.entity.InformacaoScript;
import oxil.entity.LinhaTabela;
import oxil.entity.Tabela;
import oxil.entity.exception.ReadConfigurationException;
import oxil.gui.OxilFrame;
import oxil.util.HexaUtil;

public class AbrirROMAction implements ActionListener {

    private static final String SUFIXO_CFG_FILE = "oxil";

    private static final String SUFIXO_TBL_FILE = "tbl";

    private static final FileNameExtensionFilter CFG_FILE_FILTER = new FileNameExtensionFilter("0xiL Configure File", SUFIXO_CFG_FILE);

    private static final FileNameExtensionFilter TBL_FILE_FILTER = new FileNameExtensionFilter("Table File", SUFIXO_TBL_FILE);

    private static final String[] searchList = new String[] { "\\n", "\\t" };

    private static final String[] replacementList = new String[] { "\n", "\t" };

    public void actionPerformed(ActionEvent event) {
        OxilFrame oxilFrame = OxilFrame.getInstance();
        JTabbedPane tabbedPane = oxilFrame.getTabbedPane();
        try {
            JFileChooser fc = new JFileChooser(ApplicationContext.basePath);
            if (fc.showOpenDialog(oxilFrame) == JFileChooser.APPROVE_OPTION) {
                tabbedPane.removeAll();
                ApplicationContext.arquivoOriginal = null;
                ApplicationContext.configuracaoROM = null;
                ApplicationContext.tabela = null;
                File file = fc.getSelectedFile();
                if (file.exists()) {
                    String baseFilePath = file.getAbsolutePath();
                    baseFilePath = baseFilePath.substring(0, baseFilePath.lastIndexOf("."));
                    ApplicationContext.basePath = file.getParentFile().getAbsolutePath();
                    File cfgFile = new File(baseFilePath + "." + SUFIXO_CFG_FILE);
                    if (!cfgFile.exists()) {
                        JFileChooser cfgFileChooser = new JFileChooser(file.getParentFile());
                        cfgFileChooser.setFileFilter(CFG_FILE_FILTER);
                        if (cfgFileChooser.showOpenDialog(oxilFrame) == JFileChooser.APPROVE_OPTION) {
                            cfgFile = cfgFileChooser.getSelectedFile();
                        }
                        if (!cfgFile.exists()) {
                            throw new ReadConfigurationException("N�o foi poss�vel carregar um arquivo de configura��es v�lido.");
                        }
                    }
                    File tblFile = new File(baseFilePath + "." + SUFIXO_TBL_FILE);
                    if (!tblFile.exists()) {
                        JFileChooser tblFileChooser = new JFileChooser(cfgFile.getParentFile());
                        tblFileChooser.setFileFilter(TBL_FILE_FILTER);
                        if (tblFileChooser.showOpenDialog(oxilFrame) == JFileChooser.APPROVE_OPTION) {
                            tblFile = tblFileChooser.getSelectedFile();
                        }
                        if (!tblFile.exists()) {
                            throw new ReadConfigurationException("N�o foi poss�vel carregar uma tabela v�lida.");
                        }
                    }
                    ConfiguracaoROM configuracaoROM = leArquivoConfiguracoes(cfgFile);
                    ApplicationContext.configuracaoROM = configuracaoROM;
                    Tabela tabela = leTabela(tblFile);
                    ApplicationContext.tabela = tabela;
                    ApplicationContext.arquivoOriginal = file;
                    oxilFrame.habilitarCampos();
                    List<InformacaoScript> informacoesScript = configuracaoROM.getInformacoesScript();
                    FileInputStream fis = new FileInputStream(file);
                    FileChannel fileChannel = fis.getChannel();
                    for (int i = 0; i < informacoesScript.size(); i++) {
                        InformacaoScript informacaoScript = informacoesScript.get(i);
                        int offsetInicioTexto = informacaoScript.getOffsetInicioTexto();
                        int offsetFimTexto = informacaoScript.getOffsetFimTexto();
                        int offsetInicioTextoExtra = informacaoScript.getOffsetInicioTextoExtra();
                        int offsetFimTextoExtra = informacaoScript.getOffsetFimTextoExtra();
                        int tamanhoTexto = offsetFimTexto - offsetInicioTexto;
                        int tamanhoTextoExtra = offsetFimTextoExtra - offsetInicioTextoExtra;
                        byte[] fileContent = new byte[tamanhoTexto + 1];
                        fileChannel.position(0);
                        fis.skip(offsetInicioTexto);
                        fis.read(fileContent);
                        StringBuffer tabContent = new StringBuffer((tamanhoTexto) + (tamanhoTextoExtra));
                        for (int j = 0; j < fileContent.length; j++) {
                            tabContent.append(HexaUtil.hexaValue(fileContent[j]));
                        }
                        if (offsetInicioTextoExtra != -1 && offsetFimTextoExtra != -1) {
                            fileContent = new byte[tamanhoTextoExtra + 1];
                            fileChannel.position(0);
                            fis.skip(offsetInicioTextoExtra);
                            fis.read(fileContent);
                            for (int j = 0; j < fileContent.length; j++) {
                                tabContent.append(HexaUtil.hexaValue(fileContent[j]));
                            }
                        }
                        final JTextArea jTextArea = criaTextArea();
                        jTextArea.setText(exibeTexto(tabContent.toString(), tabela, configuracaoROM.getValorHexaNuloString()));
                        jTextArea.requestFocusInWindow();
                        jTextArea.setFont(AlterarFonteAction.getFonteSelecionada());
                        jTextArea.setCaretPosition(0);
                        JScrollPane jScrollPane = new JScrollPane();
                        jScrollPane.setViewportView(jTextArea);
                        tabbedPane.addTab("Script " + (i + 1), jScrollPane);
                    }
                    fis.close();
                    if (tabbedPane != null) {
                        Component[] tabs = tabbedPane.getComponents();
                        if (tabs != null && tabs.length > 0) {
                            JScrollPane jScrollPane = (JScrollPane) tabs[0];
                            JTextArea textArea = (JTextArea) jScrollPane.getViewport().getView();
                            textArea.requestFocusInWindow();
                        }
                    }
                } else {
                    oxilFrame.desabilitarCampos();
                    JOptionPane.showMessageDialog(oxilFrame, "O arquivo selecionado � inv�lido.", "Ops.", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (ReadConfigurationException e) {
            e.printStackTrace();
            oxilFrame.desabilitarCampos();
            JOptionPane.showMessageDialog(oxilFrame, e.getMessage(), "Ops.", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable t) {
            t.printStackTrace();
            oxilFrame.desabilitarCampos();
            JOptionPane.showMessageDialog(oxilFrame, "Ocorreu um erro ao processar a sua requisi��o.", "Ops.", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("serial")
    private JTextArea criaTextArea() {
        final OxilFrame oxilFrame = OxilFrame.getInstance();
        final Highlighter hilit = new DefaultHighlighter();
        JTextArea textcomp = new JTextArea();
        final UndoManager undo = new UndoManager();
        Document doc = textcomp.getDocument();
        textcomp.setHighlighter(hilit);
        doc.addUndoableEditListener(new UndoableEditListener() {

            public void undoableEditHappened(UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
            }
        });
        textcomp.getActionMap().put("Undo", new AbstractAction("Undo") {

            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException e) {
                }
            }
        });
        textcomp.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        textcomp.getActionMap().put("Redo", new AbstractAction("Redo") {

            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException e) {
                }
            }
        });
        textcomp.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        textcomp.getActionMap().put("Find", new AbstractAction("Find") {

            public void actionPerformed(ActionEvent evt) {
                oxilFrame.getPesquisaTextField().requestFocusInWindow();
            }
        });
        textcomp.getInputMap().put(KeyStroke.getKeyStroke("control F"), "Find");
        textcomp.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent caretEvent) {
                if (!AbstractPesquisaTexto.searchCarret) {
                    if (caretEvent.getDot() == caretEvent.getMark()) {
                        hilit.removeAllHighlights();
                    }
                }
                AbstractPesquisaTexto.searchCarret = false;
                oxilFrame.repaint();
            }
        });
        return textcomp;
    }

    @SuppressWarnings("unchecked")
    private Tabela leTabela(File tblFile) throws ReadConfigurationException {
        Tabela tabela = new Tabela();
        try {
            List<LinhaTabela> caracteresEspeciais = ApplicationContext.configuracaoROM.getCaracteresEspeciais();
            List<LinhaTabela> tbl = new ArrayList<LinhaTabela>();
            List<LinhaTabela> tblCharsEspeciais = new ArrayList<LinhaTabela>();
            List<String> lines = FileUtils.readLines(tblFile);
            if (lines != null && !lines.isEmpty()) {
                for (String line : lines) {
                    line = StringUtils.stripStart(line, null);
                    if (line != null && !line.trim().equals("") && !line.startsWith("--")) {
                        String[] lineArray = line.split("=", 2);
                        String key = lineArray[0];
                        String value = lineArray[1];
                        LinhaTabela auxLinhaTabela = new LinhaTabela(key, null);
                        if (caracteresEspeciais.contains(auxLinhaTabela)) {
                            int position = caracteresEspeciais.indexOf(auxLinhaTabela);
                            String keyCharEspecial = caracteresEspeciais.get(position).getValue();
                            String linhaTabelaValue = StringUtils.replaceEach(keyCharEspecial, searchList, replacementList);
                            tblCharsEspeciais.add(new LinhaTabela(HexaUtil.hexaValue(Integer.parseInt(value, 16)), linhaTabelaValue));
                        } else {
                            key = key.length() % 2 == 1 ? "0" + key.toUpperCase() : key.toUpperCase();
                            tbl.add(new LinhaTabela(key, value));
                        }
                    }
                }
            }
            Collections.sort(tbl);
            Collections.sort(tblCharsEspeciais);
            tabela.setTbl(tbl);
            tabela.setTblCharsEspeciais(tblCharsEspeciais);
        } catch (Throwable t) {
            throw new ReadConfigurationException("Ocorreu um erro ao ler a tabela.", t);
        }
        return tabela;
    }

    private String exibeTexto(String texto, Tabela tabela, String valorHexaNulo) {
        List<LinhaTabela> tblValorNulo = new ArrayList<LinhaTabela>();
        tblValorNulo.add(new LinhaTabela(valorHexaNulo, ""));
        String[] orderedText = new String[texto.length() / 2];
        boolean[] checked = new boolean[texto.length()];
        List<LinhaTabela> tbl = tabela.getTbl();
        List<LinhaTabela> tblCharsEspeciais = tabela.getTblCharsEspeciais();
        processaTexto(tbl, texto, orderedText, checked);
        processaTexto(tblCharsEspeciais, texto, orderedText, checked);
        processaTexto(tblValorNulo, texto, orderedText, checked);
        for (int position = 0; position < texto.length(); position++) {
            if (!checked[position]) {
                orderedText[position / 2] = "{" + texto.substring(position, (!checked[position + 1] ? (position + 2) : position++)) + "}";
                if (!checked[position + 1]) {
                    position++;
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        if (orderedText != null && orderedText.length > 0) {
            for (int i = 0; i < orderedText.length; i++) {
                if (orderedText[i] != null) {
                    sb.append(orderedText[i]);
                }
            }
        }
        return sb.toString();
    }

    private void processaTexto(List<LinhaTabela> tbl, String texto, String[] orderedText, boolean[] checked) {
        for (LinhaTabela linhaTabela : tbl) {
            String key = linhaTabela.getKey();
            String value = linhaTabela.getValue();
            int position = texto.indexOf(key);
            while (position != -1) {
                if (position % 2 == 0 && !checked[position]) {
                    orderedText[position / 2] = value;
                    for (int i = 0; i < key.length(); i++) {
                        checked[position + i] = true;
                    }
                    position = texto.indexOf(key, position + key.length());
                } else {
                    position = texto.indexOf(key, position + 1);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ConfiguracaoROM leArquivoConfiguracoes(File cfgFile) throws ReadConfigurationException {
        ConfiguracaoROM configuracaoROM = new ConfiguracaoROM();
        try {
            List<String> lines = FileUtils.readLines(cfgFile);
            List<LinhaTabela> caracteresEspeciais = new ArrayList<LinhaTabela>();
            if (lines != null && !lines.isEmpty()) {
                List<InformacaoScript> informacoesScript = new ArrayList<InformacaoScript>();
                boolean leuConfiguracoesGerais = false;
                boolean specialsChars = false;
                for (String line : lines) {
                    line = StringUtils.stripStart(line, null);
                    if (line != null && !line.trim().equals("") && !line.startsWith("#")) {
                        if (!leuConfiguracoesGerais) {
                            String[] aux = line.trim().split(",");
                            if (aux.length >= 2 && aux.length <= 4) {
                                if (!aux[0].trim().equals("")) {
                                    configuracaoROM.setValorHexaNulo(Integer.parseInt(aux[0], 16));
                                }
                                if (!aux[1].trim().equals("")) {
                                    configuracaoROM.setRecalcularPonteiros("s".equalsIgnoreCase(aux[1]) ? Boolean.TRUE : Boolean.FALSE);
                                    if (configuracaoROM.getRecalcularPonteiros()) {
                                        if (aux.length == 4 && !aux[2].trim().equals("") && !aux[3].trim().equals("")) {
                                            configuracaoROM.setInverterBytesPonteiros("s".equalsIgnoreCase(aux[2]) ? Boolean.TRUE : Boolean.FALSE);
                                            configuracaoROM.setValorHexaPonteiro(Integer.parseInt(aux[3], 16));
                                        } else {
                                            throw new ReadConfigurationException("� necess�rio informar os valores para o rec�lculo dos ponteiros.");
                                        }
                                    }
                                } else {
                                    configuracaoROM.setRecalcularPonteiros(Boolean.FALSE);
                                }
                                if (!configuracaoROM.getRecalcularPonteiros()) {
                                    configuracaoROM.setInverterBytesPonteiros(null);
                                    configuracaoROM.setValorHexaPonteiro(null);
                                }
                                leuConfiguracoesGerais = true;
                            } else {
                                throw new ReadConfigurationException("Ocorreu um erro ao ler o arquivo de configura��es.");
                            }
                        } else {
                            String trimLine = line.trim();
                            if (trimLine.trim().equals("SOSC")) {
                                specialsChars = true;
                                continue;
                            } else if (trimLine.equals("EOSC")) {
                                specialsChars = false;
                                continue;
                            }
                            if (specialsChars) {
                                String[] lineArray = line.split("=", 2);
                                String key = lineArray[0];
                                String value = lineArray[1];
                                caracteresEspeciais.add(new LinhaTabela(key, value));
                            } else {
                                String[] infoArray = line.split(",");
                                InformacaoScript informacaoScript = new InformacaoScript();
                                informacaoScript.setOffsetInicioPonteiros(Integer.parseInt(infoArray[0], 16));
                                informacaoScript.setOffsetFimPonteiros(Integer.parseInt(infoArray[1], 16));
                                informacaoScript.setOffsetInicioTexto(Integer.parseInt(infoArray[2], 16));
                                informacaoScript.setOffsetFimTexto(Integer.parseInt(infoArray[3], 16));
                                if (infoArray.length >= 5) {
                                    String aux = infoArray[4];
                                    if (aux != null && !aux.trim().equals("")) {
                                        informacaoScript.setOffsetInicioTextoExtra(Integer.parseInt(aux, 16));
                                    } else {
                                        informacaoScript.setOffsetInicioTextoExtra(-1);
                                    }
                                }
                                if (infoArray.length >= 6) {
                                    String aux = infoArray[5];
                                    if (aux != null && !aux.trim().equals("")) {
                                        informacaoScript.setOffsetFimTextoExtra(Integer.parseInt(aux, 16));
                                    } else {
                                        informacaoScript.setOffsetFimTextoExtra(-1);
                                    }
                                }
                                if (infoArray.length >= 7) {
                                    String aux = infoArray[6];
                                    if (aux != null && !aux.trim().equals("")) {
                                        informacaoScript.setHeader(Integer.parseInt(aux, 16));
                                    }
                                }
                                informacoesScript.add(informacaoScript);
                            }
                        }
                    }
                }
                Collections.sort(caracteresEspeciais);
                configuracaoROM.setCaracteresEspeciais(caracteresEspeciais);
                configuracaoROM.setInformacoesScript(informacoesScript);
            }
        } catch (Throwable t) {
            throw new ReadConfigurationException("Ocorreu um erro ao ler o arquivo de configura��es.", t);
        }
        return configuracaoROM;
    }
}
