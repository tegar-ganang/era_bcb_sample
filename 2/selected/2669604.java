package newgen.presentation.administration;

import java.util.*;
import java.util.*;
import org.jdom.*;
import org.jdom.output.*;
import org.jdom.input.*;

/**
 *
 * @author  Administrator
 */
public class NewGeographicalSubDivisionAFDialog extends javax.swing.JDialog {

    /** Creates new form NewGeographicalSubDivisionAFDialog */
    public NewGeographicalSubDivisionAFDialog() {
        super();
        initComponents();
        this.setModal(true);
        this.setSize(536, 132);
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(this);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        tfGeographicalSubDivision = new newgen.presentation.UnicodeTextField();
        jPanel3 = new javax.swing.JPanel();
        bok = new javax.swing.JButton();
        bhelp = new javax.swing.JButton();
        bHelpCsh = new javax.swing.JButton();
        bcancel = new javax.swing.JButton();
        setTitle(java.util.ResourceBundle.getBundle("Administration").getString("NewGeographicSubDivision"));
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        jPanel2.setLayout(new java.awt.GridBagLayout());
        jPanel2.setBorder(new javax.swing.border.EtchedBorder());
        jLabel1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("GeographicalSubDivision"));
        jPanel2.add(jLabel1, new java.awt.GridBagConstraints());
        tfGeographicalSubDivision.setColumns(35);
        jPanel2.add(tfGeographicalSubDivision, new java.awt.GridBagConstraints());
        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);
        jPanel3.setBorder(new javax.swing.border.EtchedBorder());
        bok.setMnemonic('o');
        bok.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Ok"));
        bok.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bokActionPerformed(evt);
            }
        });
        jPanel3.add(bok);
        bhelp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/help.gif")));
        bhelp.setMnemonic('h');
        jPanel3.add(bhelp);
        bHelpCsh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/helpcsh.gif")));
        jPanel3.add(bHelpCsh);
        bcancel.setMnemonic('c');
        bcancel.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Cancel"));
        jPanel3.add(bcancel);
        getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);
        pack();
    }

    private void bokActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.tfGeographicalSubDivision.getText().trim().equals("")) {
            this.showWarningMessage("Enter geographic sub division");
        } else {
            if (this.fromsearch) {
                String[] patlib = newgen.presentation.NewGenMain.getAppletInstance().getPatronLibraryIds();
                String xmlreq = newgen.presentation.administration.AdministrationXMLGenerator.getInstance().saveGeographicalSubDivision("7", this.tfGeographicalSubDivision.getText(), patlib);
                System.out.println(xmlreq);
                try {
                    java.net.URL url = new java.net.URL(ResourceBundle.getBundle("Administration").getString("ServerURL") + ResourceBundle.getBundle("Administration").getString("ServletSubPath") + "SubDivisionServlet");
                    java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
                    urlconn.setDoOutput(true);
                    java.io.OutputStream dos = urlconn.getOutputStream();
                    dos.write(xmlreq.getBytes());
                    java.io.InputStream ios = urlconn.getInputStream();
                    SAXBuilder saxb = new SAXBuilder();
                    Document retdoc = saxb.build(ios);
                    Element rootelement = retdoc.getRootElement();
                    if (rootelement.getChild("Error") == null) {
                        this.showInformationMessage(ResourceBundle.getBundle("Administration").getString("DataSavedInDatabase"));
                    } else {
                        this.showErrorMessage(ResourceBundle.getBundle("Administration").getString("ErrorPleaseContactTheVendor"));
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            } else {
                rettext = this.tfGeographicalSubDivision.getText();
            }
            this.dispose();
        }
    }

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {
        setVisible(false);
        dispose();
    }

    public String getEnteredValue() {
        return rettext;
    }

    void showErrorMessage(String message) {
        newgen.presentation.NewGenMain app = newgen.presentation.NewGenMain.getAppletInstance();
        app.showErrorMessage(message);
    }

    void showInformationMessage(String message) {
        newgen.presentation.NewGenMain app = newgen.presentation.NewGenMain.getAppletInstance();
        app.showInformationMessage(message);
    }

    void showWarningMessage(String message) {
        newgen.presentation.NewGenMain app = newgen.presentation.NewGenMain.getAppletInstance();
        app.showWarningMessage(message);
    }

    void showQuestionMessage(String message) {
        newgen.presentation.NewGenMain app = newgen.presentation.NewGenMain.getAppletInstance();
        app.showQuestionMessage(message);
    }

    public void setFromSearch(boolean val) {
        fromsearch = val;
    }

    private javax.swing.JButton bHelpCsh;

    private javax.swing.JButton bcancel;

    private javax.swing.JButton bhelp;

    private javax.swing.JButton bok;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private newgen.presentation.UnicodeTextField tfGeographicalSubDivision;

    private boolean fromsearch;

    private String rettext;
}