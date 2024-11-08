package com.ggvaidya.TaxonDNA.Modules;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;
import com.ggvaidya.TaxonDNA.DNA.formats.*;
import com.ggvaidya.TaxonDNA.UI.*;

public class Randomizer extends Panel implements UIExtension, ActionListener, ItemListener, Runnable {

    private TaxonDNA taxonDNA;

    private TextField tf_dataset1 = new TextField();

    private FileInputPanel finp_dataset2 = null;

    private Button btn_Go = new Button("Go!");

    private TextField tf_noOfReplicates = new TextField("100");

    private DirectoryInputPanel dinp_output = null;

    private Button btn_Randomize = new Button("Randomize");

    private Choice choice_format = new Choice();

    private TextArea text_results = new TextArea();

    private SequenceList sl1 = null;

    private SequenceList sl2 = null;

    private Hashtable hash_seq_counts = null;

    /**
	 * No, no commands to add, thank you very much.
	 */
    public boolean addCommandsToMenu(Menu menu) {
        return false;
    }

    public Randomizer(TaxonDNA view) {
        super();
        taxonDNA = view;
        RightLayout rl = null;
        Panel top = new Panel();
        rl = new RightLayout(top);
        top.setLayout(rl);
        tf_dataset1.setEditable(false);
        rl.add(new Label("Please select dataset A: "), RightLayout.LEFT);
        rl.add(tf_dataset1, RightLayout.BESIDE | RightLayout.STRETCH_X);
        finp_dataset2 = new FileInputPanel("Please select dataset B: ", FileInputPanel.MODE_FILE_READ, taxonDNA.getFrame());
        rl.add(finp_dataset2, RightLayout.FILL_2 | RightLayout.NEXTLINE | RightLayout.STRETCH_X);
        btn_Go.addActionListener(this);
        rl.add(btn_Go, RightLayout.NEXTLINE | RightLayout.FILL_2 | RightLayout.STRETCH_X);
        Panel output = new Panel();
        output.setLayout(new BorderLayout());
        text_results.setEditable(false);
        text_results.setFont(new Font("Monospaced", Font.PLAIN, 12));
        output.add(text_results);
        Panel results = new Panel();
        rl = new RightLayout(results);
        results.setLayout(rl);
        rl.add(new Label("Number of replicates:"), RightLayout.LEFT);
        rl.add(tf_noOfReplicates, RightLayout.BESIDE | RightLayout.STRETCH_X);
        rl.add(new Label("Choose directory:"), RightLayout.NEXTLINE);
        dinp_output = new DirectoryInputPanel(null, DirectoryInputPanel.MODE_DIR_MODIFY_FILES, taxonDNA.getFrame());
        rl.add(dinp_output, RightLayout.BESIDE | RightLayout.FILL_2 | RightLayout.STRETCH_X);
        rl.add(new Label("In this format:"), RightLayout.NEXTLINE | RightLayout.LEFT);
        Iterator i = SequenceList.getFormatHandlers().iterator();
        while (i.hasNext()) {
            FormatHandler fh = (FormatHandler) i.next();
            choice_format.add(fh.getShortName());
        }
        rl.add(choice_format, RightLayout.BESIDE | RightLayout.STRETCH_X);
        btn_Randomize.addActionListener(this);
        rl.add(btn_Randomize, RightLayout.NEXTLINE | RightLayout.LEFT | RightLayout.STRETCH_X | RightLayout.FILL_2);
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(output);
        add(results, BorderLayout.SOUTH);
    }

    public void dataChanged() {
        sl1 = null;
        sl2 = null;
        hash_seq_counts = null;
        text_results.setText("No results to display");
        finp_dataset2.setFile(null);
        String fileName = "";
        SequenceList sl = taxonDNA.lockSequenceList();
        if (sl == null) {
            fileName = "None specified";
        } else if (sl.getFile() != null) fileName = sl.getFile().getAbsolutePath(); else fileName = "None specified";
        tf_dataset1.setText(fileName);
        taxonDNA.unlockSequenceList();
    }

    public Panel getPanel() {
        return this;
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource().equals(btn_Go)) {
            new Thread(this, "Randomizer_load").start();
        } else if (evt.getSource().equals(btn_Randomize)) {
            new Thread(this, "Randomizer_write").start();
        }
    }

    public void run() {
        if (Thread.currentThread().getName().equals("Randomizer_load")) {
            loadAndSetupSpeciesDetails();
        } else {
            randomizeLists();
        }
    }

    public void randomizeLists() {
        if (sl2 == null || hash_seq_counts == null) {
            return;
        }
        int replicates = 0;
        replicates = Integer.parseInt(tf_noOfReplicates.getText());
        if (replicates == 0) {
            return;
        }
        tf_noOfReplicates.setText(String.valueOf(replicates));
        int format = choice_format.getSelectedIndex();
        FormatHandler fh = (FormatHandler) SequenceList.getFormatHandlers().get(format);
        if (fh == null) {
            fh = new com.ggvaidya.TaxonDNA.DNA.formats.FastaFile();
        }
        File output_dir = null;
        File dir = dinp_output.getDirectory();
        int n = 1000;
        do {
            output_dir = new File(dir, String.valueOf(new Date().getTime()));
            n--;
        } while (n > 0 && output_dir.exists());
        if (!output_dir.mkdir()) {
            MessageBox mb = new MessageBox(taxonDNA.getFrame(), "Couldn't create a directory for the randomizations!", "I tried to create '" + output_dir + "' to store the results of the randomizations, but an error occured. Please ensure that you have permissions to create this directory, and try again, or try another directory.");
            mb.go();
            return;
        }
        sl1 = taxonDNA.lockSequenceList();
        String name_a_full = "";
        if (sl1.getFile() == null) name_a_full = "rando"; else name_a_full = sl1.getFile().getName();
        String name_b_full = "";
        if (sl2.getFile() == null) name_b_full = "rando"; else name_b_full = sl2.getFile().getName();
        int current_size = 3;
        int current_index = 1;
        String name_a = name_a_full.substring(0, current_size);
        String name_b = name_b_full.substring(0, current_size);
        while (name_a.equals(name_b)) {
            current_size++;
            if (current_size > name_a_full.length()) {
                name_a = name_a + current_index;
                current_index++;
            } else {
                name_a = name_a_full.substring(0, current_size);
            }
            if (current_size > name_b_full.length()) {
                name_b = name_b + current_index;
                current_index++;
            } else {
                name_b = name_b_full.substring(0, current_size);
            }
            current_index++;
            if (current_size >= 255) {
                name_a = "rando_1";
                name_b = "rando_2";
            }
        }
        Iterator i;
        ProgressDialog pd = new ProgressDialog(taxonDNA.getFrame(), "Please wait, randomizing sequence lists ...", "I'm randomizing down where required, and writing " + replicates + " into " + output_dir + " using the " + fh.getShortName() + " format. Please wait!", 0);
        pd.begin();
        for (int x = 1; x <= replicates; x++) {
            try {
                pd.delay(x, replicates);
            } catch (DelayAbortedException e) {
                pd.end();
                taxonDNA.unlockSequenceList();
                return;
            }
            File file_a = new File(output_dir, name_a + "_" + x + ".txt");
            File file_b = new File(output_dir, name_b + "_" + x + ".txt");
            SequenceList sl_a = null;
            SequenceList sl_b = null;
            try {
                sl_a = randomizeDown(sl1);
                sl_b = randomizeDown(sl2);
            } catch (Exception e) {
                pd.end();
                MessageBox mb = new MessageBox(taxonDNA.getFrame(), "Error during randomizations", "An error occured during randomization. This is an internal error, so it's more likely to be a programming problem than anything else.\n\nThe technical description is as follows: " + e);
                mb.go();
                taxonDNA.unlockSequenceList();
                return;
            }
            try {
                fh.writeFile(file_a, sl_a, null);
                fh.writeFile(file_b, sl_b, null);
            } catch (IOException e) {
                pd.end();
                MessageBox mb = new MessageBox(taxonDNA.getFrame(), "Couldn't write file!", "I couldn't write a file (either '" + file_a + "' or '" + file_b + "'. Make sure you have permissions to create and write those files, and there's enough space on that drive.\n\nThe exact error which came up was: " + e);
                mb.go();
                return;
            } catch (DelayAbortedException e) {
                pd.end();
                taxonDNA.unlockSequenceList();
                return;
            }
        }
        pd.end();
        taxonDNA.unlockSequenceList();
    }

    private SequenceList randomizeDown(SequenceList sl) throws Exception {
        SequenceList results = new SequenceList();
        sl.resort(SequenceList.SORT_RANDOM_WITHIN_SPECIES);
        Iterator i = sl.iterator();
        Sequence seq = (Sequence) i.next();
        while (i.hasNext()) {
            if (hash_seq_counts.get(seq.getSpeciesName()) == null) {
                throw new Exception("Species name '" + seq.getSpeciesName() + "' does not exist in records!");
            }
            int count_original = ((Integer) hash_seq_counts.get(seq.getSpeciesName())).intValue();
            int count = count_original;
            String sp_name = seq.getSpeciesName();
            do {
                if (seq.getSpeciesName().equals(sp_name)) {
                    if (count > 0) {
                        results.add(seq);
                        count--;
                    }
                } else {
                    if (count > 0) {
                        throw new Exception("I should add '" + count_original + "' sequences of species '" + sp_name + "', but there are only '" + (count_original - count) + "' sequences of this species name, and I've reached the first " + seq.getSpeciesName() + "!");
                    } else {
                        break;
                    }
                }
            } while (i.hasNext() && (seq = (Sequence) i.next()) != null);
        }
        return results;
    }

    public void loadAndSetupSpeciesDetails() {
        ProgressDialog pd;
        sl1 = taxonDNA.lockSequenceList();
        sl2 = null;
        text_results.setText("No results to display");
        SpeciesDetails sd1 = null;
        SpeciesDetails sd2 = null;
        {
            File file = finp_dataset2.getFile();
            if (file == null) {
                taxonDNA.unlockSequenceList();
                return;
            }
            pd = new ProgressDialog(taxonDNA.getFrame(), "Please wait, loading file ...", "I'm loading the other file (" + file + ") into memory now. Sorry for the wait!");
            try {
                sl2 = SequenceList.readFile(file, pd);
            } catch (SequenceListException e) {
                new MessageBox(taxonDNA.getFrame(), "Problem reading file", e.toString()).go();
                taxonDNA.unlockSequenceList();
                return;
            } catch (DelayAbortedException e) {
                taxonDNA.unlockSequenceList();
                return;
            }
        }
        try {
            pd = new ProgressDialog(taxonDNA.getFrame(), "Please wait, calculating species summaries ...", "The species summaries for both sequence lists are being calculated. Sorry for the wait!");
            sd1 = sl1.getSpeciesDetails(pd);
            sd2 = sl2.getSpeciesDetails(pd);
        } catch (DelayAbortedException e) {
            taxonDNA.unlockSequenceList();
            return;
        }
        hash_seq_counts = new Hashtable();
        {
            StringBuffer text = new StringBuffer();
            Hashtable names = new Hashtable();
            Hashtable h1 = new Hashtable();
            Hashtable h2 = new Hashtable();
            Iterator i1 = sd1.getSpeciesNamesIterator();
            while (i1.hasNext()) {
                String name = (String) i1.next();
                h1.put(name, new Integer(sd1.getSpeciesDetailsByName(name).getSequencesCount()));
                names.put(name, new Object());
            }
            Iterator i2 = sd2.getSpeciesNamesIterator();
            while (i2.hasNext()) {
                String name = (String) i2.next();
                h2.put(name, new Integer(sd2.getSpeciesDetailsByName(name).getSequencesCount()));
                names.put(name, new Object());
            }
            Iterator i = names.keySet().iterator();
            while (i.hasNext()) {
                String name = (String) i.next();
                Integer integ1 = (Integer) h1.get(name);
                Integer integ2 = (Integer) h2.get(name);
                int int1 = 0;
                int int2 = 0;
                if (integ1 != null) int1 = integ1.intValue();
                if (integ2 != null) int2 = integ2.intValue();
                int fincount = 0;
                if (int2 > int1) fincount = int1; else fincount = int2;
                hash_seq_counts.put(name, new Integer(fincount));
                text.append(cutTextTo(name, 40) + cutTextTo("Dataset A: " + int1, 20) + cutTextTo("Dataset B: " + int2, 20) + cutTextTo("Finally: " + fincount, 20) + "\n");
            }
            text_results.setText(text.toString());
        }
        taxonDNA.unlockSequenceList();
    }

    private String cutTextTo(String text, int len) {
        if (text.length() < len) {
            StringBuffer padding = new StringBuffer();
            for (int x = 0; x < len - text.length(); x++) padding.append(' ');
            return text + padding.toString();
        } else if (text.length() == len) {
            return text;
        } else if (text.length() > len) {
            return text.substring(0, len - 3) + "...";
        } else {
            return "";
        }
    }

    public void itemStateChanged(ItemEvent e) {
    }

    public String getShortName() {
        return "Randomizer";
    }

    public String getDescription() {
        return "Randomizes two datasets together to remove the effect of differential species distributions to affect results";
    }

    public Frame getFrame() {
        return null;
    }
}
