package com.ggvaidya.TaxonDNA.GenBankExplorer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;
import com.ggvaidya.TaxonDNA.DNA.formats.*;
import com.ggvaidya.TaxonDNA.UI.*;

public class Exporter {

    private GenBankExplorer explorer = null;

    /**
	 * Constructor. Stores the GenBankExplorer object for future use.
	 */
    public Exporter(GenBankExplorer exp) {
        explorer = exp;
    }

    /**
	 * Checks that a list of SequenceContainers is 'okay'. Eventually,
	 * this means we'll check to make sure you're not exporting 
	 * identical sequences, overlapping sequences, etc. For now, not
	 * much.
	 */
    public boolean verifyExport(java.util.List containers) {
        return true;
    }

    /**
	 * Runs the FileDialog to get a File.
	 */
    public File getSaveFile(String title) {
        FileDialog fd = new FileDialog(explorer.getFrame(), title, FileDialog.SAVE);
        fd.setVisible(true);
        if (fd.getFile() != null) {
            if (fd.getDirectory() != null) {
                return new File(fd.getDirectory() + fd.getFile());
            } else return new File(fd.getFile());
        }
        return null;
    }

    public void reportIOException(IOException e, File f) {
        reportException("Error: Could not access/write to file.", "I could not access/write to the file '" + f.getAbsolutePath() + "'! Are you sure you have permissions to read from or write to this file?\n\nThe technical description of the error I got is: " + e.getMessage());
    }

    public void reportException(String title, String message) {
        MessageBox mb = new MessageBox(explorer.getFrame(), title, message);
        mb.go();
    }

    /**
	 * Export to multiple Fasta files. Note that this means we
	 * don't HAVE a container set. So, instead:
	 * 1.	We need to ask the user where he wants the file to go.
	 * 2.	We need to get our hands on the current FeatureBin.
	 * 3.	Iterate through all the categories, then the subcategories.
	 * 4.	Come up with valid names ($name_$type.txt) in the directory specified.
	 * 5.	And, err ... that's it.
	 *
	 * @param min_species the minimum number of species to export. Files with less than min_species species will not be exported at all.
	 */
    public void exportMultipleFasta(int min_species) {
        if (explorer.getViewManager().getGenBankFile() == null) {
            new MessageBox(explorer.getFrame(), "No file loaded!", "There's no file to export (or a bug in the program). Either way, you need to open a file.").go();
        }
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Please choose a directory to export to ...");
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.showSaveDialog(explorer.getFrame());
        File f_base = jfc.getSelectedFile();
        if (f_base == null) return;
        MessageBox mb = new MessageBox(explorer.getFrame(), "Are you sure you want to export multiple files to this directory?", "Are you sure you want to export all files to " + f_base + "? I will refuse to overwrite any files in this directory, but if the export aborts, you might be left with an incomplete set of files.", MessageBox.MB_TITLE_IS_UNIQUE | MessageBox.MB_YESNOTOALL);
        if (mb.showMessageBox() == MessageBox.MB_YES) {
            FeatureBin fb = new FeatureBin(explorer.getViewManager().getGenBankFile());
            ProgressDialog pd = new ProgressDialog(explorer.getFrame(), "Please wait, processing features ...", "I'm processing features; I'll start writing them in a bit. Sorry for the wait!");
            java.util.List l = null;
            try {
                l = fb.getGenes(pd);
            } catch (DelayAbortedException e) {
                return;
            }
            Iterator i = l.iterator();
            while (i.hasNext()) {
                FeatureBin.FeatureList list = (FeatureBin.FeatureList) i.next();
                File f = new File(f_base, file_system_sanitize(list.getName() + ".txt"));
                int number = 1;
                while (f.exists()) f = new File(f_base, file_system_sanitize(list.getName() + "_" + number + ".txt"));
                try {
                    _export(list, new FastaFile(), f, new ProgressDialog(explorer.getFrame(), "Exporting '" + f + "' ...", "Currently exporting sequences to '" + f + "'"), true, min_species);
                } catch (SequenceException e) {
                    reportException("Error exporting sequences!", "The following error occured while combining the sequences: " + e.getMessage() + ". This is probably an error in the program itself.");
                } catch (IOException e) {
                    reportIOException(e, f);
                    return;
                } catch (DelayAbortedException e) {
                    return;
                }
            }
        }
    }

    private String file_system_sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9_()\\.]", "_");
    }

    /**
	 * Export the java.util.List of SequenceContainers as a FASTA file. 
	 * As you can see, we throw no exceptions: any errors are reported
	 * to the user before we return.
	 */
    public void exportAsFasta(java.util.List containers) {
        export(containers, new FastaFile());
    }

    public void export(java.util.List containers, FormatHandler fh) {
        if (!verifyExport(containers)) return;
        File f = getSaveFile("Select " + fh.getShortName() + " file to export to ...");
        if (f == null) return;
        int count = 0;
        try {
            count = _export(containers, fh, f, new ProgressDialog(explorer.getFrame(), "Please wait, assembling sequences for export ...", "I am assembling all selected sequences in preparation for export. Please give me a second!"), false, 0);
        } catch (SequenceException e) {
            reportException("Error exporting sequences!", "The following error occured while combining the sequences: " + e.getMessage() + ". This is probably an error in the program itself.");
            return;
        } catch (IOException e) {
            reportIOException(e, f);
            return;
        } catch (DelayAbortedException e) {
            return;
        }
        MessageBox mb = new MessageBox(explorer.getFrame(), "Export successful!", count + " features were successfully exported to '" + f.getAbsolutePath() + "' in the " + fh.getShortName() + " format.");
        mb.go();
    }

    public int _export(java.util.List containers, FormatHandler fh, File f, DelayCallback delay, boolean no_overwrite, int min_species) throws IOException, DelayAbortedException, SequenceException {
        if (f.exists() && no_overwrite) throw new IOException("The file '" + f + "' exists! I will not overwrite it.");
        SequenceList sl = combineContainers(containers, delay);
        int count = sl.count();
        if (min_species > 0) {
            if (sl.getSpeciesDetails(null).getSpeciesCount() < min_species) return 0;
        }
        ProgressDialog pd = new ProgressDialog(explorer.getFrame(), "Please wait, exporting " + count + " features ...", "I am exporting " + count + " features to '" + f.getAbsolutePath() + "' in the " + fh.getShortName() + " format. Sorry for the wait!");
        fh.writeFile(f, sl, pd);
        return count;
    }

    public SequenceList combineContainers(java.util.List list, DelayCallback delay) throws SequenceException, DelayAbortedException {
        SequenceList sl = new SequenceList();
        Iterator i = list.iterator();
        if (delay != null) delay.begin();
        int x = 0;
        while (i.hasNext()) {
            if (delay != null) delay.delay(x, list.size());
            x++;
            SequenceContainer container = (SequenceContainer) i.next();
            sl.addAll(container.getAsSequenceList());
            if (container.alsoContains().size() > 0) sl.addAll(combineContainers(container.alsoContains(), null));
        }
        if (delay != null) delay.end();
        return sl;
    }
}
