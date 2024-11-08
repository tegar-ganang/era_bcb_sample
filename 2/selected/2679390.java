package newgen.presentation.administration;

import newgen.presentation.administration.*;
import java.util.*;
import org.jdom.*;
import org.jdom.output.*;
import org.jdom.input.*;

/**
 *
 * @author  Administrator
 */
public class NewCorporateNameAFInternalFrame extends javax.swing.JInternalFrame {

    /** Creates new form NewCorporateNameAFInternalFrame */
    private NewCorporateNameAFInternalFrame() {
        initComponents();
        javax.help.HelpBroker helpbroker = newgen.presentation.NewGenMain.getAppletInstance().getHelpbroker();
        javax.help.HelpSet helpset = newgen.presentation.NewGenMain.getAppletInstance().getHelpset();
        helpbroker.enableHelp(this, "Authorityfileshelp", helpset);
        java.awt.event.ActionListener bhelpal = new javax.help.CSH.DisplayHelpFromSource(helpbroker);
        this.bhelp.addActionListener(bhelpal);
        this.corporatenamepanel = new newgen.presentation.administration.CorporateNameAF();
        this.addpanel.add(this.corporatenamepanel);
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(this);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        bok = new javax.swing.JButton();
        bhelp = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        bcancel = new javax.swing.JButton();
        bexit = new javax.swing.JButton();
        addpanel = new javax.swing.JPanel();
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("NewCorporateName"));
        jPanel1.setBorder(new javax.swing.border.EtchedBorder());
        bok.setMnemonic('o');
        bok.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Ok"));
        bok.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bokActionPerformed(evt);
            }
        });
        jPanel1.add(bok);
        bhelp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/help.gif")));
        bhelp.setMnemonic('h');
        jPanel1.add(bhelp);
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/helpcsh.gif")));
        jPanel1.add(jButton1);
        bcancel.setMnemonic('c');
        bcancel.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Cancel"));
        jPanel1.add(bcancel);
        bexit.setMnemonic('e');
        bexit.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bexit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bexitActionPerformed(evt);
            }
        });
        jPanel1.add(bexit);
        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);
        addpanel.setLayout(new java.awt.BorderLayout());
        getContentPane().add(addpanel, java.awt.BorderLayout.CENTER);
        pack();
    }

    private void bexitActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void bokActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.corporatenamepanel.getEnteredValues().get(0).toString().trim().equals("")) {
            this.showWarningMessage("Enter Corporate Name");
        } else {
            String[] patlib = newgen.presentation.NewGenMain.getAppletInstance().getPatronLibraryIds();
            String xmlreq = newgen.presentation.administration.AdministrationXMLGenerator.getInstance().saveCorporateName("2", corporatenamepanel.getEnteredValues(), patlib);
            try {
                java.net.URL url = new java.net.URL(ResourceBundle.getBundle("Administration").getString("ServerURL") + ResourceBundle.getBundle("Administration").getString("ServletSubPath") + "CorporateNameServlet");
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
        }
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

    public static NewCorporateNameAFInternalFrame getInstance() {
        if (thisScreen == null) {
            thisScreen = new NewCorporateNameAFInternalFrame();
            thisScreen.setSize(570, 250);
            thisScreen.show();
        } else {
            thisScreen.setSize(570, 250);
            thisScreen.show();
        }
        return thisScreen;
    }

    private javax.swing.JPanel addpanel;

    private javax.swing.JButton bcancel;

    private javax.swing.JButton bexit;

    private javax.swing.JButton bhelp;

    private javax.swing.JButton bok;

    private javax.swing.JButton jButton1;

    private javax.swing.JPanel jPanel1;

    private newgen.presentation.administration.CorporateNameAF corporatenamepanel;

    private static NewCorporateNameAFInternalFrame thisScreen;
}
