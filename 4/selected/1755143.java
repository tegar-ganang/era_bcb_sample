package org.exmaralda.partitureditor.partiture.fileActions;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.exmaralda.partitureditor.jexmaraldaswing.fileDialogs.ExportFileDialog;
import org.exmaralda.partitureditor.jexmaraldaswing.fileFilters.ParameterFileFilter;
import org.exmaralda.partitureditor.partiture.*;
import org.exmaralda.partitureditor.jexmaralda.*;
import org.exmaralda.partitureditor.jexmaralda.convert.*;
import java.io.*;
import org.exmaralda.folker.data.EventListTranscription;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.segment.CHATSegmentation;
import org.jdom.JDOMException;
import org.jdom.transform.XSLTransformException;
import org.xml.sax.SAXException;

/**
 *
 * exports the current transcription to some 3rd party format
 * Menu: File --> Export... 
 * @author  thomas
 */
public class ExportAction extends org.exmaralda.partitureditor.partiture.AbstractTableAction {

    /** Creates a new instance of ExportAGAction */
    public ExportAction(PartitureTableWithActions t, javax.swing.ImageIcon icon) {
        super("Export...", icon, t);
    }

    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        System.out.println("exportAction!");
        table.commitEdit(true);
        try {
            export();
        } catch (Exception ex) {
            String message = "File could not be exported:\n" + ex.getLocalizedMessage();
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(table, message);
        }
    }

    private void export() throws SAXException, IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException, JexmaraldaException, FSMException, JDOMException, XSLTransformException, Exception {
        ExportFileDialog dialog = new ExportFileDialog(table.homeDirectory);
        ActionUtilities.setFileFilter("last-export-filter", table.getTopLevelAncestor(), dialog);
        int retValue = dialog.showSaveDialog(table.parent);
        if (retValue != javax.swing.JFileChooser.APPROVE_OPTION) return;
        ParameterFileFilter selectedFileFilter = (ParameterFileFilter) (dialog.getFileFilter());
        File selectedFile = dialog.getSelectedFile();
        String filename = selectedFile.getAbsolutePath();
        if (!(selectedFile.getName().indexOf(".") >= 0)) {
            filename += "." + selectedFileFilter.getSuffix();
        }
        File exportFile = new File(filename);
        if (exportFile.exists()) {
            int confirm = javax.swing.JOptionPane.showConfirmDialog(table, exportFile.getAbsolutePath() + "\nalready exists. Overwrite?");
            if (confirm == javax.swing.JOptionPane.CANCEL_OPTION) return;
            if (confirm == javax.swing.JOptionPane.NO_OPTION) export();
        }
        BasicTranscription trans = table.getModel().getTranscription().makeCopy();
        if (selectedFileFilter == dialog.TASXFileFilter) {
            TASXConverter tc = new TASXConverter();
            tc.writeTASXToFile(trans, filename);
        } else if (selectedFileFilter == dialog.PraatFileFilter) {
            PraatConverter pc = new PraatConverter();
            pc.writePraatToFile(trans, filename);
        } else if (selectedFileFilter == dialog.AGFileFilter) {
            AIFConverter.writeAIFToFile(trans, filename);
        } else if (selectedFileFilter == dialog.EAFFileFilter) {
            ELANConverter ec = new ELANConverter();
            ec.writeELANToFile(trans, filename);
        } else if (selectedFileFilter == dialog.AudacityLabelFileFilter) {
            switch(dialog.audacityExportAccessoryPanel.getMethod()) {
                case AudacityConverter.ALL_TIERS:
                    AudacityConverter.writeAudacityToFile(trans, filename);
                    break;
                case AudacityConverter.SELECTED_TIERS:
                    AudacityConverter.writeAudacityToFile(trans, filename, table.selectionStartRow, table.selectionEndRow);
                    break;
                case AudacityConverter.TIMELINE:
                    AudacityConverter.writeTimelineToFile(trans, filename);
                    break;
            }
        } else if (selectedFileFilter == dialog.TEIFileFilter) {
            TEIConverter ec;
            switch(dialog.teiExportAccessoryPanel.getMethod()) {
                case TEIConverter.GENERIC_METHOD:
                    ec = new TEIConverter();
                    ec.writeGenericTEIToFile(trans, filename);
                    break;
                case TEIConverter.AZM_METHOD:
                    ec = new TEIConverter();
                    ec.writeTEIToFile(trans, filename);
                    break;
                case TEIConverter.MODENA_METHOD:
                    ec = new TEIConverter("/org/exmaralda/partitureditor/jexmaralda/xsl/EXMARaLDA2TEI_Modena.xsl");
                    ec.writeModenaTEIToFile(trans, filename);
                    break;
                case TEIConverter.HIAT_METHOD:
                    ec = new TEIConverter();
                    ec.writeHIATTEIToFile(trans, filename);
                    break;
                case TEIConverter.HIAT_NEW_METHOD:
                    ec = new TEIConverter();
                    ec.writeNewHIATTEIToFile(trans, filename);
                    break;
                case TEIConverter.CGAT_METHOD:
                    ec = new TEIConverter();
                    ec.writeFOLKERTEIToFile(trans, filename);
                    break;
            }
        } else if (selectedFileFilter == dialog.TEIModenaFileFilter) {
            TEIConverter ec = new TEIConverter("/org/exmaralda/partitureditor/jexmaralda/xsl/EXMARaLDA2TEI_Modena.xsl");
            ec.writeModenaTEIToFile(trans, filename);
        } else if (selectedFileFilter == dialog.CHATTranscriptFileFilter) {
            switch(dialog.chatExportAccessoryPanel.getMethod()) {
                case CHATConverter.CHAT_SEGMENTATION_METHOD:
                    exportCHATTranscript(trans, filename, "UTF-8");
                    break;
                case CHATConverter.HIAT_SEGMENTATION_METHOD:
                    CHATConverter.writeHIATSegmentedCHATFile(trans, exportFile);
                    break;
                case CHATConverter.EVENT_METHOD:
                    CHATConverter.writeEventSegmentedCHATFile(trans, exportFile);
                    break;
            }
        } else if (selectedFileFilter == dialog.ExmaraldaSegmentedTranscriptionFileFilter) {
            SegmentedTranscription st = trans.toSegmentedTranscription();
            st.setEXBSource(table.filename);
            st.writeXMLToFile(filename, "none");
        } else if (selectedFileFilter == dialog.FOLKERTranscriptionFileFilter) {
            EventListTranscription elt = org.exmaralda.folker.io.EventListTranscriptionConverter.importExmaraldaBasicTranscription(trans);
            org.exmaralda.folker.io.EventListTranscriptionXMLReaderWriter.writeXML(elt, exportFile, new org.exmaralda.folker.data.GATParser(), 0);
        } else if (selectedFileFilter == dialog.TreeTaggerFilter) {
            new TreeTaggerConverter().writeText(trans, exportFile);
        }
        ActionUtilities.memorizeFileFilter("last-export-filter", table.getTopLevelAncestor(), dialog);
        table.status("Transcription exported as " + filename);
    }

    void exportCHATTranscript(BasicTranscription bt, String filename, String encoding) throws JexmaraldaException, FSMException, SAXException, FileNotFoundException, IOException {
        CHATSegmentation segmenter = new org.exmaralda.partitureditor.jexmaralda.segment.CHATSegmentation(table.chatFSM);
        System.out.println("Segmenter initialized");
        ListTranscription lt = segmenter.BasicToUtteranceList(bt);
        System.out.println("List transformation completed");
        String text = CHATSegmentation.toText(lt);
        System.out.println("Text generated");
        System.out.println("started writing document...");
        FileOutputStream fos = new FileOutputStream(new File(filename));
        if (encoding.length() == 0) {
            fos.write(text.getBytes());
        } else {
            fos.write(text.getBytes(encoding));
        }
        fos.close();
        System.out.println("document written.");
    }
}
