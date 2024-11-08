package uk.org.sgj.OHCApparatus.Essay;

import uk.org.sgj.OHCApparatus.*;
import uk.org.sgj.OHCApparatus.RTFDocument.RTFPreviewTextPane;
import uk.org.sgj.SGJNifty.Files.*;
import uk.org.sgj.OHCApparatus.Records.*;
import uk.org.sgj.OHCApparatus.RTFDocument.RTFDocument;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import uk.org.sgj.UnicodeInsertion.InsertMenu;

public class ApparatusPreviewPane extends JPanel {

    private RTFPreviewTextPane textPane;

    private RTFDocument doc;

    private JScrollPane scroll;

    private JButton writeToDisk;

    private WriteToDiskListener wtdl;

    private OHCEssay currentEssay;

    private boolean preview;

    private JFrame frame;

    private boolean biblio;

    class WriteToDiskListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String actionText = e.getActionCommand();
            if (actionText.equals("write apparatus")) {
                attemptToWriteToFile();
            }
        }
    }

    protected void attemptToWriteToFile() {
        File selectedFile = null;
        try {
            File lastSaved;
            if (biblio) {
                lastSaved = currentEssay.getLastSavedBibFile();
            } else {
                lastSaved = currentEssay.getLastSavedAppFile();
            }
            File lastDir;
            if (lastSaved != null) {
                lastDir = lastSaved;
            } else {
                lastDir = OHCEssay.getLastDirectoryForAnyEssay();
            }
            selectedFile = FileUtils.selectFileToSave("Rich Text Format", "rtf", lastDir, lastSaved);
            if (null != selectedFile) {
                writeFile(selectedFile);
            }
        } catch (FileNotFoundException fnf) {
            JOptionPane.showMessageDialog(null, "Trying to write to the following file failed.\n" + selectedFile + "\n" + "The file might be read-only or it might be open in another program.\n" + "The essay was not exported.\n" + "You should export the essay to a different file.\n\n" + "Exception text follows:" + fnf.toString(), "Couldn't write to file!", JOptionPane.ERROR_MESSAGE);
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void writeFile(File fname) throws FileNotFoundException, BadLocationException, IOException {
        FileOutputStream fos = new FileOutputStream(fname);
        doc.writeToFile(fos);
        fos.close();
        if (biblio) {
            currentEssay.setLastSavedBibFile(fname);
        } else {
            currentEssay.setLastSavedAppFile(fname);
        }
        frame.setVisible(false);
        frame.dispose();
    }

    private void setUpApparatusFrame(String essayName) {
        String str = (biblio ? "Bibliography for " : "Footnotes for ");
        frame = new JFrame(str + essayName);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(this);
        frame.pack();
        if (!biblio) {
            frame.setLocation(50, 50);
        }
        frame.setVisible(true);
    }

    ApparatusPreviewPane(OHCEssay essay, boolean pre, boolean biblio) {
        currentEssay = essay;
        preview = pre;
        this.biblio = biblio;
        setLayout(new BorderLayout());
        textPane = new RTFPreviewTextPane();
        InsertMenu.registerUnicodeField(textPane);
        doc = new RTFDocument(textPane);
        scroll = new JScrollPane(textPane);
        add(scroll);
        if (preview) {
            JPanel panel = new JPanel();
            String writeBibOrApp = biblio ? "Write bibliography to disk" : "Write footnotes to disk";
            writeToDisk = new JButton(writeBibOrApp);
            writeToDisk.setMnemonic(KeyEvent.VK_W);
            writeToDisk.setActionCommand("write apparatus");
            writeToDisk.setToolTipText("Click this button to write this bibliography to disk as an RTF file.");
            writeToDisk.setEnabled(true);
            wtdl = new WriteToDiskListener();
            writeToDisk.addActionListener(wtdl);
            panel.add(writeToDisk);
            add(panel, BorderLayout.PAGE_START);
        }
        textPane.setEditable(false);
        textPane.setMargin(new Insets(25, 25, 25, 25));
        scroll.setPreferredSize(new Dimension(800, 600));
        if (biblio) {
            doc.addTextPair(new TextPair("Bibliography", "heading"));
            doc.addTextPair(new TextPair("\n", "heading"));
            doc.addTextPair(new TextPair("\n", "heading"));
        } else {
            doc.addTextPair(new TextPair("Footnotes will be displayed for each record in the following format:", "heading"));
            doc.addTextPair(new TextPair("\n", "heading"));
            doc.addTextPair(new TextPair("Initial citation", "heading"));
            doc.addTextPair(new TextPair("\n", "heading"));
            doc.addTextPair(new TextPair("Any subsequent citations", "heading"));
            doc.addTextPair(new TextPair("\n", "heading"));
            doc.addTextPair(new TextPair("---------------", "heading"));
            doc.addTextPair(new TextPair("\n", "heading"));
        }
        setUpApparatusFrame(essay.toString());
    }

    protected void putDocumentInPane() {
        textPane.setCaretPosition(0);
        if (!preview) {
            attemptToWriteToFile();
        }
    }

    protected void writeRecordToFn(OHCBasicRecord currentRecord) {
        Iterator<TextPair> firstRef = currentRecord.getTextFirstReference();
        while (firstRef.hasNext()) {
            TextPair tp = (TextPair) firstRef.next();
            doc.addTextPair(tp);
        }
        doc.addTextPair(new TextPair("\n", "footnote"));
        Iterator<TextPair> secondRef = currentRecord.getTextSecondReference();
        while (secondRef.hasNext()) {
            TextPair tp = (TextPair) secondRef.next();
            doc.addTextPair(tp);
        }
        doc.addTextPair(new TextPair("\n", "footnote"));
        doc.addTextPair(new TextPair("---------------", "footnote"));
        doc.addTextPair(new TextPair("\n", "footnote"));
    }

    protected void writeRecordToDocument(OHCBasicRecord currentRecord, OHCBasicRecord previousRecord) {
        if (biblio) {
            writeRecordToBiblio(currentRecord, previousRecord);
        } else {
            writeRecordToFn(currentRecord);
        }
    }

    protected void writeRecordToBiblio(OHCBasicRecord currentRecord, OHCBasicRecord previousRecord) {
        if (previousRecord == null) {
            writeRecordToBiblio(currentRecord);
        } else {
            String previousName = previousRecord.getPrimaryName();
            String currentName = currentRecord.getPrimaryName();
            if (previousName.equals(currentName)) {
                Iterator<TextPair> bibliography = currentRecord.getTextBibliography();
                if (bibliography.hasNext()) {
                    TextPair tp = bibliography.next();
                    String replaced;
                    if (currentName.endsWith(".")) {
                        replaced = tp.text.replaceFirst(currentName, FUN.RepeatedAuthor + ".");
                    } else {
                        replaced = tp.text.replaceFirst(currentName, FUN.RepeatedAuthor);
                    }
                    TextPair newTp = new TextPair(replaced, tp.style);
                    doc.addTextPair(newTp);
                }
                while (bibliography.hasNext()) {
                    doc.addTextPair(bibliography.next());
                }
                doc.addTextPair(new TextPair("\n", "biblio"));
            } else {
                writeRecordToBiblio(currentRecord);
            }
        }
    }

    protected void writeRecordToBiblio(OHCBasicRecord currentRecord) {
        Iterator<TextPair> bibliography = currentRecord.getTextBibliography();
        while (bibliography.hasNext()) {
            TextPair tp = (TextPair) bibliography.next();
            doc.addTextPair(tp);
        }
        doc.addTextPair(new TextPair("\n", "biblio"));
    }

    public static Iterator<TextPair> getRecordAsAltCit(OHCBasicRecord currentRecord) {
        Vector<TextPair> altCit = new Vector<TextPair>();
        Iterator<TextPair> firstRef = currentRecord.getTextFirstReference();
        addIteratorToNB(firstRef, altCit, false);
        Iterator<TextPair> secondRef = currentRecord.getTextSecondReference();
        addIteratorToNB(secondRef, altCit, false);
        Iterator<TextPair> bibliography = currentRecord.getTextBibliography();
        addIteratorToNB(bibliography, altCit, true);
        return (altCit.iterator());
    }

    private static void addIteratorToNB(Iterator<TextPair> pairs, Vector<TextPair> notaBeneAltCit, boolean bibliography) {
        String text, style;
        TextPair tt, clone;
        while (pairs.hasNext()) {
            tt = pairs.next();
            text = tt.text.replace(FUN.OHCAPages, FUN.NotaBenePages);
            if (bibliography) {
                style = new String(tt.style);
            } else {
                if (tt.style.equals("footnote")) {
                    style = "biblio";
                } else {
                    style = "biblio italic";
                }
            }
            clone = new TextPair(text, style);
            notaBeneAltCit.add(clone);
        }
        if (!bibliography) {
            notaBeneAltCit.add(new TextPair("|", "biblio"));
        }
    }
}
