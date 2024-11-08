package GUI;

import IXM.Document;
import IXM.DocumentCollection;
import IXM.Index;
import IXM.Term;
import java.awt.event.WindowEvent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 *
 * @author haris
 */
public class DocumentInsertionView extends javax.swing.JFrame {

    DocumentCollection activeCollection;

    /** Creates new form DocumentInsertionView */
    public DocumentInsertionView(DocumentCollection activeCollection) {
        this.activeCollection = activeCollection;
        initComponents();
        setAlwaysOnTop(true);
        setLocationRelativeTo(getParent());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        documentFileChooser = new javax.swing.JFileChooser();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(GUI.InfraRedApp.class).getContext().getResourceMap(DocumentInsertionView.class);
        documentFileChooser.setApproveButtonText(resourceMap.getString("documentFileChooser.approveButtonText"));
        documentFileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        documentFileChooser.setName("documentFileChooser");
        documentFileChooser.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                documentFileChooserActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(documentFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(documentFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pack();
    }

    private void documentFileChooserActionPerformed(java.awt.event.ActionEvent evt) {
        if (evt.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
            File selectedFile = documentFileChooser.getSelectedFile();
            File collectionCopyFile;
            String newDocumentName = selectedFile.getName();
            Document newDocument = new Document(newDocumentName);
            if (activeCollection.containsDocument(newDocument)) {
                int matchingFilenameDistinguisher = 1;
                StringBuilder distinguisherReplacer = new StringBuilder();
                newDocumentName = newDocumentName.concat("(" + matchingFilenameDistinguisher + ")");
                newDocument.setDocumentName(newDocumentName);
                while (activeCollection.containsDocument(newDocument)) {
                    matchingFilenameDistinguisher++;
                    newDocumentName = distinguisherReplacer.replace(newDocumentName.length() - 2, newDocumentName.length() - 1, new Integer(matchingFilenameDistinguisher).toString()).toString();
                    newDocument.setDocumentName(newDocumentName);
                }
            }
            Scanner tokenizer = null;
            FileChannel fileSource = null;
            FileChannel collectionDestination = null;
            HashMap<String, Integer> termHashMap = new HashMap<String, Integer>();
            Index collectionIndex = activeCollection.getIndex();
            int documentTermMaxFrequency = 0;
            int currentTermFrequency;
            try {
                tokenizer = new Scanner(new BufferedReader(new FileReader(selectedFile)));
                tokenizer.useDelimiter(Pattern.compile("\\p{Space}|\\p{Punct}|\\p{Cntrl}"));
                String nextToken;
                while (tokenizer.hasNext()) {
                    nextToken = tokenizer.next().toLowerCase();
                    if (!nextToken.isEmpty()) if (termHashMap.containsKey(nextToken)) termHashMap.put(nextToken, termHashMap.get(nextToken) + 1); else termHashMap.put(nextToken, 1);
                }
                Term newTerm;
                for (String term : termHashMap.keySet()) {
                    newTerm = new Term(term);
                    if (!collectionIndex.termExists(newTerm)) collectionIndex.addTerm(newTerm);
                    currentTermFrequency = termHashMap.get(term);
                    if (currentTermFrequency > documentTermMaxFrequency) documentTermMaxFrequency = currentTermFrequency;
                    collectionIndex.addOccurence(newTerm, newDocument, currentTermFrequency);
                }
                activeCollection.addDocument(newDocument);
                String userHome = System.getProperty("user.home");
                String fileSeparator = System.getProperty("file.separator");
                collectionCopyFile = new File(userHome + fileSeparator + "Infrared" + fileSeparator + activeCollection.getDocumentCollectionName() + fileSeparator + newDocumentName);
                collectionCopyFile.createNewFile();
                fileSource = new FileInputStream(selectedFile).getChannel();
                collectionDestination = new FileOutputStream(collectionCopyFile).getChannel();
                collectionDestination.transferFrom(fileSource, 0, fileSource.size());
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage() + " This error should never occur! The file was just selected!");
                return;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "An I/O error occured during file transfer!", "File transfer I/O error", JOptionPane.WARNING_MESSAGE);
                return;
            } finally {
                try {
                    if (tokenizer != null) tokenizer.close();
                    if (fileSource != null) fileSource.close();
                    if (collectionDestination != null) collectionDestination.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
            processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (evt.getActionCommand().equalsIgnoreCase(JFileChooser.CANCEL_SELECTION)) processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private javax.swing.JFileChooser documentFileChooser;
}
