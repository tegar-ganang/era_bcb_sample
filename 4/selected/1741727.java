package com.ggvaidya.TaxonDNA.GenBankExplorer;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;
import com.ggvaidya.TaxonDNA.DNA.formats.*;
import com.ggvaidya.TaxonDNA.UI.*;

public class ClustalMapping implements ActionListener {

    private GenBankExplorer explorer = null;

    private SequenceList list_initial = null;

    private SequenceList list_final = null;

    private SequenceList list_missing = null;

    private File file_initial = null;

    private JDialog dialog = null;

    private JPanel p_exportClustalMapped = new JPanel();

    private JPanel p_exportFinal = new JPanel();

    private JPanel p_bottom = new JPanel();

    private FileInputPanel finp_clustalMapped = null;

    private FileInputPanel finp_finalOutput = null;

    private JButton btn_exportClustalMapped = null;

    private JButton btn_exportFinal = null;

    private JButton btn_close = null;

    public ClustalMapping(GenBankExplorer exp) {
        explorer = exp;
        createUI();
    }

    public void createUI() {
        dialog = new JDialog(explorer.getFrame(), "Clustal Mapping exports ...", true);
        dialog.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                closeDialog();
            }
        });
        finp_clustalMapped = new FileInputPanel("Clustal-mapped Fasta file: ", FileInputPanel.MODE_FILE_WRITE, dialog);
        btn_exportClustalMapped = new JButton("Export to Clustal-mapped Fasta file");
        finp_finalOutput = new FileInputPanel("Final Fasta file: ", FileInputPanel.MODE_FILE_WRITE, dialog);
        btn_exportFinal = new JButton("Export to final Fasta file");
        btn_exportFinal.addActionListener(this);
        RightLayout rl = new RightLayout(p_exportClustalMapped);
        p_exportClustalMapped.setLayout(rl);
        rl.add(new Label("1. Please select a new file to export these sequences to:"), RightLayout.NONE);
        rl.add(finp_clustalMapped, RightLayout.NEXTLINE | RightLayout.STRETCH_X);
        rl.add(btn_exportClustalMapped, RightLayout.NEXTLINE);
        btn_exportClustalMapped.addActionListener(this);
        rl = new RightLayout(p_exportFinal);
        p_exportFinal.setLayout(rl);
        rl.add(new Label("2. Please select a file to export the clustal-mapped sequences to:"), RightLayout.NEXTLINE);
        rl.add(finp_finalOutput, RightLayout.NEXTLINE | RightLayout.STRETCH_X);
        rl.add(new Label("Any sequences not present in the Clustal-mapped Fasta file will be written to 'missing.txt' in the same directory as the final export file specified above."), RightLayout.NEXTLINE | RightLayout.STRETCH_X);
        rl.add(btn_exportFinal, RightLayout.NEXTLINE);
        p_bottom = new JPanel();
        p_bottom.setLayout(new BorderLayout());
        btn_close = new JButton("Close this dialog");
        btn_close.addActionListener(this);
        p_bottom.add(btn_close);
    }

    private Frame getFrame() {
        return explorer.getFrame();
    }

    private File getExportedFile() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btn_exportClustalMapped)) {
            File f = finp_clustalMapped.getFile();
            if (exportSequenceSet(list_initial, f)) {
                showDialog(true);
                file_initial = f;
            }
        } else if (e.getSource().equals(btn_exportFinal)) {
            File f_final = finp_finalOutput.getFile();
            importSequenceSet(list_initial, file_initial);
            File file_being_processed = null;
            try {
                String desc_final = list_final.size() + " final sequences to '" + f_final + "'";
                file_being_processed = f_final;
                new FastaFile().writeFile(f_final, list_final, new ProgressDialog(explorer.getFrame(), "Please wait, exporting final Fasta file ...", "The final Fasta file is being exported. Sorry for the wait!"));
                String desc_missing = "";
                if (list_missing != null && list_missing.size() > 0) {
                    File f_missing = new File(f_final.getParent(), "missing.txt");
                    if (f_missing.exists()) {
                        MessageBox mb = new MessageBox(explorer.getFrame(), "Overwrite file?", "The file '" + f_missing + "' already exists! Would you like to overwrite it?\n\nIf you say 'Yes' here, the file '" + f_missing + "' will be deleted.\nIf you say 'No' here, the file '" + f_missing + "' will not be altered. No missing file will be saved.", MessageBox.MB_YESNO);
                        if (mb.showMessageBox() == MessageBox.MB_NO) f_missing = null;
                    }
                    if (f_missing != null) {
                        desc_missing = " and " + list_missing.size() + " missing sequences to '" + f_missing + "'";
                        file_being_processed = f_missing;
                        new FastaFile().writeFile(f_missing, list_missing, new ProgressDialog(explorer.getFrame(), "Please wait, exporting missing sequences to a separate Fasta file ...", "The Fasta file containing missing seuqences is being written out now. Please wait a moment!"));
                    }
                }
                new MessageBox(explorer.getFrame(), "All done!", "Successfully exported " + desc_final + desc_missing + ".").go();
            } catch (IOException exp) {
                new MessageBox(explorer.getFrame(), "Could not write file " + file_being_processed + "!", "There was an error writing to '" + file_being_processed + "'. Try again, and ensure you have adequate permissions to the files you are trying to write to. The error which occured is: " + exp.getMessage());
            } catch (DelayAbortedException exp) {
                return;
            }
        } else if (e.getSource().equals(btn_close)) {
            closeDialog();
        }
    }

    public void closeDialog() {
        list_initial = null;
        dialog.setVisible(false);
    }

    private void loadSequenceList() {
        try {
            list_initial = explorer.getSelectedSequenceList(new ProgressDialog(explorer.getFrame(), "Please wait, assembling sequences ...", "I am assembling all selected sequences preparatory to writing them into a file. Sorry for the delay!"));
        } catch (DelayAbortedException e) {
            list_initial = null;
            return;
        } catch (SequenceException e) {
            new MessageBox(explorer.getFrame(), "Could not load sequence!", "The following sequence could not be created:\n" + e.getMessage()).go();
        }
    }

    public void showDialog(boolean showPartDeux) {
        dialog.getContentPane().setLayout(new BorderLayout());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(p_exportClustalMapped, BorderLayout.NORTH);
        if (showPartDeux) p.add(p_exportFinal, BorderLayout.SOUTH);
        dialog.getContentPane().add(p);
        dialog.getContentPane().add(p_bottom, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setVisible(true);
    }

    public void go() {
        loadSequenceList();
        showDialog(false);
    }

    /**
	 * Imports all the data in inputFile (combined with the present file, which acts as
	 * a mapfile) into a NEW sequenceSet, which is spawned off into it's own SpeciesIdentifier.
	 * (it's the only way i can think of of doing this without confusing the user, or
	 * obliterating his dataset without any complaints). Missing sequences will have
	 * the warning flag set, so they "fall" to the bottom of the list.
	 *
	 * @return null; the final sequences are stored in list_final, and the missing sequences in list_missing.
	 */
    public void importSequenceSet(SequenceList set_initial, File inputFile) {
        SequenceList set_final = null;
        SequenceList set_map = new SequenceList(set_initial);
        if (set_map == null || set_map.count() == 0) set_map = new SequenceList();
        FastaFile ff = new com.ggvaidya.TaxonDNA.DNA.formats.FastaFile();
        String error_occured_in = "";
        error_occured_in = inputFile.toString();
        try {
            set_final = new SequenceList(inputFile, ff, new ProgressDialog(getFrame(), "Loading '" + inputFile + "' ...", "Loading the sequences from '" + inputFile + "', please wait."));
        } catch (SequenceListException e) {
            MessageBox mb = new MessageBox(getFrame(), "There is an error in '" + error_occured_in + "'!", "The following error occured while trying to read '" + error_occured_in + "'. Please make sure that it has been formatted correctly.");
            mb.go();
            return;
        } catch (DelayAbortedException e) {
            return;
        }
        if (set_final == null) return;
        Pattern pSequenceDDD = Pattern.compile("^seq(.*)$");
        Iterator i = set_final.iterator();
        while (i.hasNext()) {
            Sequence seq = (Sequence) i.next();
            String name = seq.getFullName();
            Matcher m = pSequenceDDD.matcher(name);
            if (m.matches() && m.groupCount() == 1) {
                String no = m.group(1);
                if (!no.equals("")) {
                    Iterator iMap = set_map.iterator();
                    while (iMap.hasNext()) {
                        Sequence seq2 = (Sequence) iMap.next();
                        String compareTo = "gi|" + no + "|";
                        if (seq2.getFullName().indexOf(compareTo) != -1) {
                            seq.changeName(seq2.getFullName());
                            iMap.remove();
                            break;
                        }
                        compareTo = "gi|" + no + ":";
                        if (seq2.getFullName().indexOf(compareTo) != -1) {
                            seq.changeName(seq2.getFullName());
                            iMap.remove();
                            break;
                        }
                        if (no.length() > 0 && no.charAt(0) == 'U') {
                            compareTo = "[uniqueid:" + no.substring(1) + "]";
                            if (seq2.getFullName().indexOf(compareTo) != -1) {
                                seq.changeName(seq2.getFullName());
                                iMap.remove();
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (set_map.count() > 0) {
            String path = "";
            File f = getExportedFile();
            if (f != null) path = f.getParent() + File.separator;
            set_map.setFile(new File(path + "missing_sequences.txt"));
            list_missing = set_map;
        } else list_missing = null;
        list_final = set_final;
    }

    /**
	 * This method checks to see if every sequence in the specified 
	 * SequenceList has a (hopefully unique, and we DO test this)
	 * identifier. We assume that GIs are completely unique. We also
	 * create an arbitrary value called the '[uniqueid:([\d\-]+)]', which
	 * uniquely identifies the sequence. Since we're the only one
	 * who uses the 'uniqueid', we'll work it entirely out of here.
	 * Nobody else needs unique id's at this point, but in case
	 * they do ... err ... code's going to move.
	 *
	 * @return true, iff unique IDs were generated. If we return false, we do NOT guarantee every Sequence will return a valid GI or uniqueid. We have already informed the user of this.
	 */
    public boolean createUniqueIds(SequenceList list) {
        boolean warned = false;
        MessageBox mb = null;
        Vector vec_nonIdentical = new Vector();
        Hashtable unique = new Hashtable();
        Iterator i = list.iterator();
        while (i.hasNext()) {
            Sequence seq = (Sequence) i.next();
            String id = seq.getGI();
            if (id == null || id.equals("")) {
                Pattern p = Pattern.compile("\\[uniqueid:(.*)\\]");
                Matcher m = p.matcher(seq.getFullName());
                if (m.find()) id = m.group(1);
            }
            if (id == null || id.equals("")) {
                seq.changeName(seq.getFullName() + " [uniqueid:" + seq.getId().toString() + "]");
                unique.put(seq.getId(), new Boolean(true));
            } else if (unique.get(id) != null) {
                vec_nonIdentical.add(seq.getFullName());
            } else {
                unique.put(id, new Boolean(true));
            }
        }
        if (vec_nonIdentical.size() > 0) {
            new MessageBox(explorer.getFrame(), "ERROR: Some sequences were not exported!", "The following sequences could not be exported, since they share GI numbers with other sequences in your dataset. They will be exported to a Fasta file entitled 'duplicates.txt' in the same directory as the Clustal-mapped Fasta file.\n" + Pearls.repeat(" - ", vec_nonIdentical, "\n")).go();
        }
        return true;
    }

    /**
	 * Exports all the data in 'set' into an output file. This means we check to
	 * see if the present dataset is "mappable", and change it if it isn't. The 
	 * output file will contain a list of all the sequences, in appropriate FASTA 
	 * format (good for passing into AlignmentHelper). See docs for this class on 
	 * how this works.
	 */
    public boolean exportSequenceSet(SequenceList set, File outputFile) {
        PrintWriter output = null;
        Hashtable uniques = new Hashtable();
        SequenceList sl_duplicateSequences = new SequenceList();
        if (set == null) return false;
        try {
            createUniqueIds(set);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().equals("Duplicate:notified")) return false;
            throw e;
        }
        try {
            output = new PrintWriter(new FileWriter(outputFile));
            Iterator i = set.iterator();
            int no = 0;
            while (i.hasNext()) {
                Sequence seq = (Sequence) i.next();
                String id = seq.getGI();
                if (id == null || id.equals("")) {
                    Pattern p = Pattern.compile("\\[uniqueid:(.*)\\]");
                    Matcher m = p.matcher(seq.getFullName());
                    if (m.find()) id = "U" + m.group(1); else {
                        throw new RuntimeException("Something wrong with this program in Clustal:exportSequenceSet()");
                    }
                }
                if (uniques.get(id) != null) {
                    sl_duplicateSequences.add(seq);
                    continue;
                }
                uniques.put(id, new Boolean(true));
                output.println(">seq" + id);
                output.println(seq.getSequenceWrapped(80));
                no++;
            }
            if (sl_duplicateSequences.count() > 0) {
                try {
                    File f = new File(outputFile.getParent(), "duplicates.txt");
                    new FastaFile().writeFile(f, sl_duplicateSequences, new ProgressDialog(explorer.getFrame(), "Please wait, exporting duplicate sequences ...", "All duplicate sequences are now being exported to '" + f + "'. Please be patient!"));
                } catch (IOException e) {
                } catch (DelayAbortedException e) {
                }
            }
            MessageBox mb = new MessageBox(getFrame(), "Success!", no + " sequences were exported successfully. You may now run Clustal on the sequences you specified. Once that is done, please follow step 2 to retrieve the original sequences.");
            mb.go();
            return true;
        } catch (IOException e) {
            MessageBox mb = new MessageBox(getFrame(), "Error while writing to file", "There was an error writing to '" + outputFile + "'. Are you sure you have write permissions to both the Clustal input and the map file? The technical description of this error is: " + e);
            mb.go();
            return false;
        } finally {
            if (output != null) output.close();
        }
    }
}
