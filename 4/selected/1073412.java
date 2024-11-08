package paperscope;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.io.File;

public class SaveGraphDialog extends javax.swing.JFrame {

    /** Creates new form SaveGraphLayoutMessage */
    public SaveGraphDialog(RadialGraph graph, String currentDirectory) {
        initComponents();
        this.graph = graph;
        this.currentDirectory = currentDirectory;
    }

    public void saveGraphFile() {
        if (this.currentDirectory.equals("")) {
            this.fc = new JFileChooser();
        } else {
            this.fc = new JFileChooser(this.currentDirectory);
        }
        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            final String fileName = file.getAbsolutePath();
            if (file.exists()) {
                final JFrame overwriteMessage = new JFrame();
                overwriteMessage.setTitle("Overwrite file?");
                overwriteMessage.setLocationRelativeTo(fc);
                JLabel title = new JLabel(" " + fileName);
                title.setFont(new Font("Tahoma", 0, 12));
                JLabel title2 = new JLabel(" File already exists, overwrite it?");
                title.setFont(new Font("Tahoma", 0, 12));
                JButton saveButton = new JButton();
                saveButton.setText("Yes");
                saveButton.setToolTipText("Overwrite file?");
                saveButton.addMouseListener(new java.awt.event.MouseAdapter() {

                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        ResultXML xml = new ResultXML();
                        xml.writeFile(graph.getDataGraph(), fileName);
                        overwriteMessage.setVisible(false);
                    }
                });
                JButton noButton = new JButton();
                noButton.setText("No");
                noButton.setToolTipText("Overwrite file");
                noButton.addMouseListener(new java.awt.event.MouseAdapter() {

                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        overwriteMessage.setVisible(false);
                    }
                });
                JPanel buttons = new JPanel();
                GridLayout buttonLayout = new GridLayout(1, 2);
                buttonLayout.addLayoutComponent("savebutton", saveButton);
                buttonLayout.addLayoutComponent("nobutton", noButton);
                buttons.add(saveButton);
                buttons.add(noButton);
                buttons.setLayout(buttonLayout);
                JPanel complete = new JPanel();
                GridLayout layout = new GridLayout(3, 1);
                layout.addLayoutComponent("title", title);
                layout.addLayoutComponent("title2", title2);
                layout.addLayoutComponent("buttons", buttons);
                complete.add(title);
                complete.add(title2);
                complete.add(buttons);
                complete.setLayout(layout);
                Dimension d = new Dimension(450, 100);
                overwriteMessage.setSize(d);
                overwriteMessage.setContentPane(complete);
                overwriteMessage.setVisible(true);
            } else {
                String xmlExtension = ".xml";
                String fileName2 = fileName;
                if (!fileName.contains(xmlExtension)) fileName2 = file.getAbsolutePath() + xmlExtension;
                ResultXML xml = new ResultXML();
                xml.writeFile(this.graph.getDataGraph(), fileName2);
            }
            this.currentDirectory = file.getPath();
        }
    }

    public String getCurrentDirectory() {
        return this.currentDirectory;
    }

    private void initComponents() {
        messageText = new javax.swing.JLabel();
        yesButton = new javax.swing.JButton();
        noButton = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Save Graph Layout?");
        messageText.setText("Save Graph Layout?");
        yesButton.setText("Yes");
        yesButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                yesButtonMouseClicked(evt);
            }
        });
        noButton.setText("No");
        noButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                noButtonMouseClicked(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(messageText, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addComponent(yesButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(noButton))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(messageText).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(yesButton).addComponent(noButton)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pack();
    }

    private void noButtonMouseClicked(java.awt.event.MouseEvent evt) {
        this.setVisible(false);
        saveGraphFile();
    }

    private void yesButtonMouseClicked(java.awt.event.MouseEvent evt) {
        this.graph.saveGraphLayout();
        this.setVisible(false);
        saveGraphFile();
    }

    private RadialGraph graph;

    private String currentDirectory;

    private JFileChooser fc;

    private javax.swing.JLabel messageText;

    private javax.swing.JButton noButton;

    private javax.swing.JButton yesButton;
}
