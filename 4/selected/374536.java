package mudownmanager;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mudownmanager.backend.LinkBuckReslutionException;
import mudownmanager.backend.MultiClient;
import org.apache.commons.io.IOUtils;
import org.jdesktop.application.Action;

public class UrlListDialog extends javax.swing.JDialog implements ClipboardOwner {

    private static final long serialVersionUID = -4622522825184615281L;

    /** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;

    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;

    private Callback<String, Object> callback;

    private MultiClient client;

    private static final Pattern pattern = Pattern.compile("href=\"(http://.*)\".*");

    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    /** Creates new form UrlListDialog */
    public UrlListDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    /** Creates new form UrlListDialog */
    public UrlListDialog(java.awt.Frame parent, boolean modal, Callback<String, Object> callback) {
        super(parent, modal);
        this.callback = callback;
        initComponents();
    }

    /** @return the return status of this dialog - one of RET_OK or RET_CANCEL */
    public int getReturnStatus() {
        return returnStatus;
    }

    private void initComponents() {
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        setMinimumSize(new java.awt.Dimension(0, 500));
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        setName("Form");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mudownmanager.MuDownManagerApp.class).getContext().getResourceMap(UrlListDialog.class);
        okButton.setText(resourceMap.getString("okButton.text"));
        okButton.setName("okButton");
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        cancelButton.setText(resourceMap.getString("cancelButton.text"));
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        jLabel1.setText(resourceMap.getString("jLabel1.text"));
        jLabel1.setName("jLabel1");
        jScrollPane1.setName("jScrollPane1");
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1");
        jScrollPane1.setViewportView(jTextArea1);
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(mudownmanager.MuDownManagerApp.class).getContext().getActionMap(UrlListDialog.class, this);
        jButton1.setAction(actionMap.get("paste"));
        jButton1.setText(resourceMap.getString("jButton1.text"));
        jButton1.setName("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(jButton1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 69, Short.MAX_VALUE).addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton)).addComponent(jLabel1).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)).addContainerGap()));
        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { cancelButton, okButton });
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(cancelButton).addComponent(okButton).addComponent(jButton1)).addContainerGap()));
        pack();
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doClose(RET_OK);
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doClose(RET_CANCEL);
    }

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {
        doClose(RET_CANCEL);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
    }

    @Action
    public void paste() throws LinkBuckReslutionException {
        try {
            try {
                DataFlavor dataFlavor = new DataFlavor("text/html;class=java.lang.String");
                byte[] bytes = ((String) clipboard.getData(dataFlavor)).getBytes();
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] > 0) arrayOutputStream.write(bytes[i]);
                }
                StringBuffer buffer = new StringBuffer();
                String separator = "";
                String[] tokens = arrayOutputStream.toString("utf8").split(" ");
                for (int i = 0; i < tokens.length; i++) {
                    Matcher matcher = pattern.matcher(tokens[i].trim());
                    if (matcher.matches()) {
                        String url = matcher.group(1);
                        buffer.append(separator).append(url);
                        separator = "\n";
                    }
                }
                String toAdd = "";
                if (!jTextArea1.getText().isEmpty()) {
                    toAdd = jTextArea1.getText() + "\n";
                }
                jTextArea1.setText(toAdd + buffer.toString());
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(UrlListDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (UnsupportedFlavorException ex) {
            Logger.getLogger(UrlListDialog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UrlListDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void doClose(int retStatus) {
        returnStatus = retStatus;
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("returnStatus", retStatus + "");
        try {
            Writer writer = new StringWriter();
            jTextArea1.write(writer);
            properties.put("lines", IOUtils.readLines(IOUtils.toInputStream(writer.toString())));
        } catch (Exception e) {
        }
        if (callback != null) callback.call(properties);
        setVisible(false);
        dispose();
    }

    public void emptyTextArera() {
        jTextArea1.setText("");
    }

    /**
	 * @param args
	 *            the command line arguments
	 */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                UrlListDialog dialog = new UrlListDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    private javax.swing.JButton cancelButton;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTextArea jTextArea1;

    private javax.swing.JButton okButton;

    private int returnStatus = RET_CANCEL;

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClient(MultiClient client) {
        this.client = client;
    }
}
