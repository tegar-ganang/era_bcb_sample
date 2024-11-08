package newgen.presentation.administration;

/**
 *
 * @author  Naveen
 */
public class FormLetterFrame extends javax.swing.JInternalFrame implements newgen.presentation.component.NewGenLibScreen {

    private newgen.presentation.component.Utility utility = null;

    private newgen.presentation.component.NewGenXMLGenerator newGenXMLGenerator = null;

    private static final FormLetterFrame SINGLETON = new FormLetterFrame();

    public static FormLetterFrame getInstance() {
        SINGLETON.reloadLocales();
        SINGLETON.setVisible(true);
        SINGLETON.tfTitle.grabFocus();
        return SINGLETON;
    }

    /** Creates new form FormLetterFrame */
    private FormLetterFrame() {
        initComponents();
        javax.help.HelpBroker helpbroker = newgen.presentation.NewGenMain.getAppletInstance().getHelpbroker();
        javax.help.HelpSet helpset = newgen.presentation.NewGenMain.getAppletInstance().getHelpset();
        helpbroker.enableHelp(this, "Formletters", helpset);
        java.awt.event.ActionListener bhelpal = new javax.help.CSH.DisplayHelpFromSource(helpbroker);
        this.bHelp.addActionListener(bhelpal);
        newGenXMLGenerator = newgen.presentation.component.NewGenXMLGenerator.getInstance();
        utility = newgen.presentation.component.Utility.getInstance();
        lsd = new newgen.presentation.component.FormLetterStyledDocument(5000);
        tpTemplate.setStyledDocument(lsd);
        String[] col = { newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("FormatId"), newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("FormLetterTitle") };
        dtm1 = new javax.swing.table.DefaultTableModel(col, 0) {

            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tableformat.setModel(dtm1);
        tableformat.getTableHeader().setReorderingAllowed(false);
        tableformat.getColumnModel().getColumn(0).setMinWidth(0);
        tableformat.getColumnModel().getColumn(0).setPreferredWidth(0);
        tableformat.getColumnModel().getColumn(0).setMaxWidth(0);
        tableformat.getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        String[] cols = { newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("ParameterNumber"), newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("ParameterDefinition") };
        dtm2 = new javax.swing.table.DefaultTableModel(cols, 0) {

            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tableparas.setModel(dtm2);
        tableparas.getTableHeader().setReorderingAllowed(false);
        tableparas.getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableparas.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableparas.getColumnModel().getColumn(1).setPreferredWidth(400);
        java.util.prefs.Preferences pref = java.util.prefs.Preferences.systemRoot();
        String oopath = pref.get("openofficeexecutablepath", "");
        tfOO.setText(oopath);
        String htmlpath = pref.get("htmlexecutablepath", "");
        tfHtml.setText(htmlpath);
        FormLetterDetails();
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(this);
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(dialog);
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(dialog1);
    }

    public void reloadLocales() {
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(this);
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(dialog);
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(dialog1);
        jLabel2.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Prefix"));
        dialog.setTitle(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Template"));
        bnCLose.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnCLose.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        lbExcel.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("OpenOfficeExecutablePath"));
        lbHtml.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("HTMLExecutablePath"));
        bnClose1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnClose1.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        setTitle(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("EditViewFormLetters"));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("SelectAFormLetterTitle")));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("FormLetterDetailsForAboveSelection")));
        jLabel1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Title"));
        jLabel1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Title"));
        jLabel3.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("InstantMessage"));
        jLabel4.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Email"));
        jLabel5.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Print"));
        rbMandatoryIM.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Mandatory"));
        rbOptionalIM.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Optional"));
        rbNotRequiredIM.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("NotRequired"));
        rbMandatoryEm.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Mandatory"));
        rbOptionalEm.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Optional"));
        rbNotRequiredEm.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("NotRequired"));
        rbMandatoryPr.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Mandatory"));
        rbOptionalPr.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Optional"));
        rbNotRequiredPr.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("NotRequired"));
        jLabel6.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("GenerateOpenOfficeDocument"));
        rbGenOOYes.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Yes"));
        rbGenOONo.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("No"));
        jLabel7.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("GenerateHTMLDocument"));
        rbGenHTMLYes.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Yes"));
        rbGenHTMLNo.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("No"));
        jLabel8.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("GenerateTextDocument"));
        rbGenTextYes.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Yes"));
        rbGenTextNo.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("No"));
        bnDT.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineText"));
        bnDT.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineText"));
        bnDOOD.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineOpenOfficeDocument"));
        bnDOOD.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineOpenOfficeDocument"));
        bnDH.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineHTML"));
        bnDH.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineHTML"));
        bnOk.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Ok"));
        bnOk.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Ok"));
        bHelp.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Help"));
        bnCancel.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Cancel"));
        bnCancel.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Cancel"));
        bnClose.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnClose.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        String[] col = { newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("FormatId"), newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("FormLetterTitle") };
        dtm1.setColumnIdentifiers(col);
        tableformat.getColumnModel().getColumn(0).setMinWidth(0);
        tableformat.getColumnModel().getColumn(0).setPreferredWidth(0);
        tableformat.getColumnModel().getColumn(0).setMaxWidth(0);
        String[] cols = { newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("ParameterNumber"), newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("ParameterDefinition") };
        dtm2.setColumnIdentifiers(cols);
        tableparas.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableparas.getColumnModel().getColumn(1).setPreferredWidth(400);
    }

    public void FormLetterDetails() {
        String xmlStr;
        org.jdom.Element root = new org.jdom.Element("OperationId");
        root.setAttribute("no", "3");
        utility.addLoginDetailsToTheRootElement(root);
        org.jdom.Document doc = new org.jdom.Document(root);
        xmlStr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        xmlStr = newgen.presentation.component.ServletConnector.getInstance().sendRequest("FormLetterServlet", xmlStr);
        org.jdom.Element root1 = newGenXMLGenerator.getRootElement(xmlStr);
        if (root1 != null) {
            for (int i = dtm1.getRowCount(); i > 0; i--) {
                dtm1.removeRow(i - 1);
            }
            dtm1.setRowCount(0);
            Object[] object = new Object[0];
            try {
                object = root1.getChildren("Form").toArray();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            java.util.ArrayList arrayList = new java.util.ArrayList();
            if (object.length > 0) {
                for (int i = 0; i < object.length; i++) {
                    org.jdom.Element element = (org.jdom.Element) object[i];
                    Object[] row = new Object[2];
                    row[0] = utility.getTestedString(element.getChildText("FORMAT_ID"));
                    row[1] = utility.getTestedString(element.getChildText("TITLE"));
                    arrayList.add(row);
                }
                java.util.Collections.sort(arrayList, new newgen.presentation.component.NewGenLibComparator(1));
                for (int k = 0; k < arrayList.size(); k++) {
                    dtm1.addRow(((Object[]) arrayList.get(k)));
                }
            } else {
                newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("NoFormLetterDetails"));
            }
        } else {
            newgen.presentation.NewGenMain.getAppletInstance().showErrorMessage(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Problemoccuredwhilefetchingdata"));
        }
    }

    public void dispDetails(String fid) {
        String xmlStr;
        org.jdom.Element root = new org.jdom.Element("OperationId");
        root.setAttribute("no", "4");
        utility.addLoginDetailsToTheRootElement(root);
        org.jdom.Element formatid = new org.jdom.Element("FORMAT_ID");
        formatid.setText(fid);
        root.addContent(formatid);
        org.jdom.Document doc = new org.jdom.Document(root);
        xmlStr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        xmlStr = newgen.presentation.component.ServletConnector.getInstance().sendRequest("FormLetterServlet", xmlStr);
        org.jdom.Element root1 = newGenXMLGenerator.getRootElement(xmlStr);
        if (root1 != null) {
            Object[] object = new Object[1];
            try {
                object = root1.getChildren("Form").toArray();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (object.length > 0) {
                int i = 0;
                org.jdom.Element element = (org.jdom.Element) object[i];
                tfTitle.setText(utility.getTestedString(element.getChildText("TITLE")));
                tfPrefix.setText(utility.getTestedString(element.getChildText("PREFIX")));
                String instmsg = utility.getTestedString(element.getChildText("INSTANT_MESSAGE_STATUS"));
                if (instmsg.equals("A")) {
                    rbMandatoryIM.setSelected(true);
                    rbOptionalIM.setSelected(false);
                    rbNotRequiredIM.setSelected(false);
                } else if (instmsg.equals("B")) {
                    rbMandatoryIM.setSelected(false);
                    rbOptionalIM.setSelected(true);
                    rbNotRequiredIM.setSelected(false);
                } else if (instmsg.equals("C")) {
                    rbMandatoryIM.setSelected(false);
                    rbOptionalIM.setSelected(false);
                    rbNotRequiredIM.setSelected(true);
                }
                String emailstat = utility.getTestedString(element.getChildText("EMAIL_STATUS"));
                if (emailstat.equals("A")) {
                    rbMandatoryEm.setSelected(true);
                    rbOptionalEm.setSelected(false);
                    rbNotRequiredEm.setSelected(false);
                } else if (emailstat.equals("B")) {
                    rbMandatoryEm.setSelected(false);
                    rbOptionalEm.setSelected(true);
                    rbNotRequiredEm.setSelected(false);
                } else if (emailstat.equals("C")) {
                    rbMandatoryEm.setSelected(false);
                    rbOptionalEm.setSelected(false);
                    rbNotRequiredEm.setSelected(true);
                }
                String printstat = utility.getTestedString(element.getChildText("PRINT_STATUS"));
                if (printstat.equals("A")) {
                    rbMandatoryPr.setSelected(true);
                    rbOptionalPr.setSelected(false);
                    rbNotRequiredPr.setSelected(false);
                } else if (printstat.equals("B")) {
                    rbMandatoryPr.setSelected(false);
                    rbOptionalPr.setSelected(true);
                    rbNotRequiredPr.setSelected(false);
                } else if (printstat.equals("C")) {
                    rbMandatoryPr.setSelected(false);
                    rbOptionalPr.setSelected(false);
                    rbNotRequiredPr.setSelected(true);
                }
                String genoo = utility.getTestedString(element.getChildText("GEN_OO"));
                if (genoo.equals("A")) {
                    rbGenOOYes.setSelected(true);
                    rbGenOONo.setSelected(false);
                } else if (genoo.equals("B")) {
                    rbGenOOYes.setSelected(false);
                    rbGenOONo.setSelected(true);
                }
                String genhtml = utility.getTestedString(element.getChildText("GEN_HTML"));
                if (genhtml.equals("A")) {
                    rbGenHTMLYes.setSelected(true);
                    rbGenHTMLNo.setSelected(false);
                } else if (genhtml.equals("B")) {
                    rbGenHTMLYes.setSelected(false);
                    rbGenHTMLNo.setSelected(true);
                }
                String gentext = utility.getTestedString(element.getChildText("GEN_TEXT"));
                if (gentext.equals("A")) {
                    rbGenTextYes.setSelected(true);
                    rbGenTextNo.setSelected(false);
                } else if (gentext.equals("B")) {
                    rbGenTextYes.setSelected(false);
                    rbGenTextNo.setSelected(true);
                }
                format = utility.getTestedString(element.getChildText("FORMAT"));
            }
            for (int i = dtm2.getRowCount(); i > 0; i--) {
                dtm2.removeRow(i - 1);
            }
            dtm2.setRowCount(0);
            java.util.Hashtable ht1 = new java.util.Hashtable();
            ht1 = newgen.presentation.component.Utility.getInstance().getFromLetterParameeters(fid);
            java.util.Enumeration enum1 = ht1.keys();
            int htsize = ht1.size();
            for (int i = 0; i < htsize; i++) {
                String key = String.valueOf(i);
                String val = ht1.get(key).toString();
                java.util.Vector temp = new java.util.Vector();
                temp.addElement("{" + key + "}");
                temp.addElement(val);
                dtm2.addRow(temp);
            }
        }
    }

    public void getOODocument(String fid) {
        String xmlStr;
        org.jdom.Element root = new org.jdom.Element("OperationId");
        root.setAttribute("no", "5");
        utility.addLoginDetailsToTheRootElement(root);
        org.jdom.Element formatid = new org.jdom.Element("FORMAT_ID");
        formatid.setText(fid);
        root.addContent(formatid);
        org.jdom.Document doc = new org.jdom.Document(root);
        xmlStr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        xmlStr = newgen.presentation.component.ServletConnector.getInstance().sendRequestObjectResponse("FormLetterServlet", xmlStr).toString();
        System.out.println("openofficepath" + xmlStr);
        ooFile = xmlStr;
    }

    public void postDocument() {
        String formatid = dtm1.getValueAt(tableformat.getSelectedRow(), 0).toString();
        String libId = newgen.presentation.NewGenMain.getAppletInstance().getLibraryID();
        java.util.Vector vec = new java.util.Vector();
        vec.add(libId);
        vec.add(formatid);
        Object[] obj = new Object[3];
        obj[0] = "UPLOAD";
        obj[1] = "FORMLETTER";
        try {
            java.io.File fOO = new java.io.File(ooFile);
            java.io.File fHTML = new java.io.File(htmlFile);
            java.nio.channels.FileChannel fcOO = (new java.io.FileInputStream(fOO)).getChannel();
            java.nio.channels.FileChannel fcHTML = (new java.io.FileInputStream(fHTML)).getChannel();
            int fileLengthOO = (int) fcOO.size();
            int fileLengthHTML = (int) fcHTML.size();
            java.nio.MappedByteBuffer bbOO = fcOO.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fileLengthOO);
            java.nio.MappedByteBuffer bbHTML = fcHTML.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fileLengthHTML);
            byte[] byx1 = new byte[bbOO.capacity()];
            byte[] byx2 = new byte[bbHTML.capacity()];
            bbOO.get(byx1);
            vec.add(byx1);
            bbHTML.get(byx2);
            vec.add(byx2);
            obj[2] = vec;
            String xmlStr = "";
            xmlStr = newgen.presentation.component.ServletConnector.getInstance().sendObjectRequest("FileUploadDownloadServlet", obj).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void getHTMLDocument(String fid) {
        String xmlStr;
        org.jdom.Element root = new org.jdom.Element("OperationId");
        root.setAttribute("no", "6");
        utility.addLoginDetailsToTheRootElement(root);
        org.jdom.Element formatid = new org.jdom.Element("FORMAT_ID");
        formatid.setText(fid);
        root.addContent(formatid);
        org.jdom.Document doc = new org.jdom.Document(root);
        xmlStr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        xmlStr = newgen.presentation.component.ServletConnector.getInstance().sendRequestObjectResponse("FormLetterServlet", xmlStr).toString();
        htmlFile = xmlStr;
    }

    private void refresh() {
        for (int i = dtm2.getRowCount(); i > 0; i--) {
            dtm2.removeRow(i - 1);
        }
        dtm2.setRowCount(0);
        tfTitle.setText("");
        tfPrefix.setText("");
        rbMandatoryIM.setSelected(true);
        rbOptionalIM.setSelected(false);
        rbNotRequiredIM.setSelected(false);
        rbMandatoryEm.setSelected(true);
        rbOptionalEm.setSelected(false);
        rbNotRequiredEm.setSelected(false);
        rbMandatoryPr.setSelected(true);
        rbOptionalPr.setSelected(false);
        rbNotRequiredPr.setSelected(false);
        rbGenOOYes.setSelected(true);
        rbGenOONo.setSelected(false);
        rbGenHTMLYes.setSelected(true);
        rbGenHTMLNo.setSelected(false);
        rbGenTextYes.setSelected(true);
        rbGenTextNo.setSelected(false);
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        b1 = new javax.swing.ButtonGroup();
        b2 = new javax.swing.ButtonGroup();
        b3 = new javax.swing.ButtonGroup();
        b4 = new javax.swing.ButtonGroup();
        b5 = new javax.swing.ButtonGroup();
        b6 = new javax.swing.ButtonGroup();
        dialog = new javax.swing.JDialog();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tpTemplate = new javax.swing.JTextPane();
        jPanel9 = new javax.swing.JPanel();
        bnCLose = new javax.swing.JButton();
        dialog1 = new javax.swing.JDialog();
        jPanel10 = new javax.swing.JPanel();
        lbExcel = new javax.swing.JLabel();
        tfOO = new newgen.presentation.UnicodeTextField();
        bnOO = new javax.swing.JButton();
        lbHtml = new javax.swing.JLabel();
        tfHtml = new newgen.presentation.UnicodeTextField();
        bnHtml = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        bnClose1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableformat = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        tfTitle = new newgen.presentation.UnicodeTextField();
        tfPrefix = new newgen.presentation.UnicodeTextField();
        jPanel5 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        rbMandatoryIM = new javax.swing.JRadioButton();
        rbOptionalIM = new javax.swing.JRadioButton();
        rbNotRequiredIM = new javax.swing.JRadioButton();
        rbMandatoryEm = new javax.swing.JRadioButton();
        rbOptionalEm = new javax.swing.JRadioButton();
        rbNotRequiredEm = new javax.swing.JRadioButton();
        rbMandatoryPr = new javax.swing.JRadioButton();
        rbOptionalPr = new javax.swing.JRadioButton();
        rbNotRequiredPr = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        rbGenOOYes = new javax.swing.JRadioButton();
        rbGenOONo = new javax.swing.JRadioButton();
        jLabel7 = new javax.swing.JLabel();
        rbGenHTMLYes = new javax.swing.JRadioButton();
        rbGenHTMLNo = new javax.swing.JRadioButton();
        jLabel8 = new javax.swing.JLabel();
        rbGenTextYes = new javax.swing.JRadioButton();
        rbGenTextNo = new javax.swing.JRadioButton();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableparas = new javax.swing.JTable();
        jPanel7 = new javax.swing.JPanel();
        bnDT = new javax.swing.JButton();
        bnDOOD = new javax.swing.JButton();
        bnDH = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        bnOk = new javax.swing.JButton();
        bHelp = new javax.swing.JButton();
        bnCancel = new javax.swing.JButton();
        bnClose = new javax.swing.JButton();
        dialog.setTitle(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Template"));
        jPanel8.setLayout(new java.awt.BorderLayout());
        jScrollPane3.setViewportView(tpTemplate);
        jPanel8.add(jScrollPane3, java.awt.BorderLayout.CENTER);
        dialog.getContentPane().add(jPanel8, java.awt.BorderLayout.CENTER);
        jPanel9.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        bnCLose.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnCLose.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnCLose.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnCLoseActionPerformed(evt);
            }
        });
        jPanel9.add(bnCLose);
        dialog.getContentPane().add(jPanel9, java.awt.BorderLayout.SOUTH);
        jPanel10.setLayout(new java.awt.GridBagLayout());
        jPanel10.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        lbExcel.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("OpenOfficeExecutablePath"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel10.add(lbExcel, gridBagConstraints);
        tfOO.setColumns(30);
        tfOO.setMinimumSize(new java.awt.Dimension(336, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel10.add(tfOO, gridBagConstraints);
        bnOO.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/opencomp.gif")));
        bnOO.setPreferredSize(new java.awt.Dimension(47, 20));
        bnOO.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnOOActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel10.add(bnOO, gridBagConstraints);
        lbHtml.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("HTMLExecutablePath"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel10.add(lbHtml, gridBagConstraints);
        tfHtml.setColumns(30);
        tfHtml.setMinimumSize(new java.awt.Dimension(336, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel10.add(tfHtml, gridBagConstraints);
        bnHtml.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/opencomp.gif")));
        bnHtml.setPreferredSize(new java.awt.Dimension(47, 20));
        bnHtml.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnHtmlActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel10.add(bnHtml, gridBagConstraints);
        dialog1.getContentPane().add(jPanel10, java.awt.BorderLayout.CENTER);
        jPanel11.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        bnClose1.setMnemonic('e');
        bnClose1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnClose1.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnClose1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnClose1ActionPerformed(evt);
            }
        });
        jPanel11.add(bnClose1);
        dialog1.getContentPane().add(jPanel11, java.awt.BorderLayout.SOUTH);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("EditViewFormLetters"));
        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("SelectAFormLetterTitle")));
        tableformat.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { {}, {}, {}, {} }, new String[] {}));
        tableformat.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableformatMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tableformat);
        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);
        getContentPane().add(jPanel1);
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("FormLetterDetailsForAboveSelection")));
        jPanel4.setLayout(new java.awt.GridBagLayout());
        jLabel1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Title"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel4.add(jLabel1, gridBagConstraints);
        jLabel2.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Prefix"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel4.add(jLabel2, gridBagConstraints);
        tfTitle.setColumns(30);
        jPanel4.add(tfTitle, new java.awt.GridBagConstraints());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel4.add(tfPrefix, gridBagConstraints);
        jPanel2.add(jPanel4);
        jPanel5.setLayout(new java.awt.GridBagLayout());
        jLabel3.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("InstantMessage"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel5.add(jLabel3, gridBagConstraints);
        jLabel4.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Email"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel5.add(jLabel4, gridBagConstraints);
        jLabel5.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Print"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel5.add(jLabel5, gridBagConstraints);
        b1.add(rbMandatoryIM);
        rbMandatoryIM.setSelected(true);
        rbMandatoryIM.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Mandatory"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel5.add(rbMandatoryIM, gridBagConstraints);
        b1.add(rbOptionalIM);
        rbOptionalIM.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Optional"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel5.add(rbOptionalIM, gridBagConstraints);
        b1.add(rbNotRequiredIM);
        rbNotRequiredIM.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("NotRequired"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        jPanel5.add(rbNotRequiredIM, gridBagConstraints);
        b2.add(rbMandatoryEm);
        rbMandatoryEm.setSelected(true);
        rbMandatoryEm.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Mandatory"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        jPanel5.add(rbMandatoryEm, gridBagConstraints);
        b2.add(rbOptionalEm);
        rbOptionalEm.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Optional"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        jPanel5.add(rbOptionalEm, gridBagConstraints);
        b2.add(rbNotRequiredEm);
        rbNotRequiredEm.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("NotRequired"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        jPanel5.add(rbNotRequiredEm, gridBagConstraints);
        b3.add(rbMandatoryPr);
        rbMandatoryPr.setSelected(true);
        rbMandatoryPr.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Mandatory"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        jPanel5.add(rbMandatoryPr, gridBagConstraints);
        b3.add(rbOptionalPr);
        rbOptionalPr.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Optional"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        jPanel5.add(rbOptionalPr, gridBagConstraints);
        b3.add(rbNotRequiredPr);
        rbNotRequiredPr.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("NotRequired"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        jPanel5.add(rbNotRequiredPr, gridBagConstraints);
        jLabel6.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("GenerateOpenOfficeDocument"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel5.add(jLabel6, gridBagConstraints);
        b4.add(rbGenOOYes);
        rbGenOOYes.setSelected(true);
        rbGenOOYes.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Yes"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel5.add(rbGenOOYes, gridBagConstraints);
        b4.add(rbGenOONo);
        rbGenOONo.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("No"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel5.add(rbGenOONo, gridBagConstraints);
        jLabel7.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("GenerateHTMLDocument"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel5.add(jLabel7, gridBagConstraints);
        b5.add(rbGenHTMLYes);
        rbGenHTMLYes.setSelected(true);
        rbGenHTMLYes.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Yes"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel5.add(rbGenHTMLYes, gridBagConstraints);
        b5.add(rbGenHTMLNo);
        rbGenHTMLNo.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("No"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel5.add(rbGenHTMLNo, gridBagConstraints);
        jLabel8.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("GenerateTextDocument"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel5.add(jLabel8, gridBagConstraints);
        b6.add(rbGenTextYes);
        rbGenTextYes.setSelected(true);
        rbGenTextYes.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Yes"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel5.add(rbGenTextYes, gridBagConstraints);
        b6.add(rbGenTextNo);
        rbGenTextNo.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("No"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel5.add(rbGenTextNo, gridBagConstraints);
        jPanel2.add(jPanel5);
        jPanel6.setLayout(new java.awt.BorderLayout());
        tableparas.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { {}, {}, {}, {} }, new String[] {}));
        jScrollPane2.setViewportView(tableparas);
        jPanel6.add(jScrollPane2, java.awt.BorderLayout.CENTER);
        jPanel2.add(jPanel6);
        jPanel7.setLayout(new java.awt.GridBagLayout());
        bnDT.setMnemonic('t');
        bnDT.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineText"));
        bnDT.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineText"));
        bnDT.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnDTActionPerformed(evt);
            }
        });
        bnDT.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                bnDTMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        jPanel7.add(bnDT, gridBagConstraints);
        bnDOOD.setMnemonic('f');
        bnDOOD.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineOpenOfficeDocument"));
        bnDOOD.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineOpenOfficeDocument"));
        bnDOOD.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnDOODActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel7.add(bnDOOD, gridBagConstraints);
        bnDH.setMnemonic('d');
        bnDH.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineHTML"));
        bnDH.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("DefineHTML"));
        bnDH.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnDHActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel7.add(bnDH, gridBagConstraints);
        jPanel2.add(jPanel7);
        getContentPane().add(jPanel2);
        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        bnOk.setMnemonic('o');
        bnOk.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Ok"));
        bnOk.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Ok"));
        bnOk.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnOkActionPerformed(evt);
            }
        });
        jPanel3.add(bnOk);
        bHelp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/help.gif")));
        bHelp.setMnemonic('h');
        bHelp.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Help"));
        jPanel3.add(bHelp);
        bnCancel.setMnemonic('c');
        bnCancel.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Cancel"));
        bnCancel.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Cancel"));
        bnCancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnCancelActionPerformed(evt);
            }
        });
        jPanel3.add(bnCancel);
        bnClose.setMnemonic('e');
        bnClose.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnClose.setToolTipText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Close"));
        bnClose.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnCloseActionPerformed(evt);
            }
        });
        jPanel3.add(bnClose);
        getContentPane().add(jPanel3);
        pack();
    }

    private void bnDHActionPerformed(java.awt.event.ActionEvent evt) {
        if (tableformat.getSelectedRow() != -1 && !format.equals("") && !ooFile.equals("") && !htmlFile.equals("")) {
            if (tfHtml.getText().trim().length() > 0) {
                try {
                    java.io.File fHTML = new java.io.File(htmlFile);
                    Runtime rt = Runtime.getRuntime();
                    java.util.prefs.Preferences pref = java.util.prefs.Preferences.systemRoot();
                    rt.exec(pref.get("htmlexecutablepath", "") + " " + fHTML);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("ConfigureHtmlExecutablepath"));
                dialog1.setSize(650, 250);
                dialog1.setLocation(newgen.presentation.NewGenMain.getAppletInstance().getLocation(350, 450));
                dialog1.show();
            }
        } else {
            newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("PLEASE DOUBLE CLICK ON THE ROW");
        }
    }

    private void bnClose1ActionPerformed(java.awt.event.ActionEvent evt) {
        java.util.prefs.Preferences pref = java.util.prefs.Preferences.systemRoot();
        pref.put("openofficeexecutablepath", tfOO.getText());
        dialog1.dispose();
    }

    private void bnHtmlActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.showOpenDialog(null);
        java.io.File htmfile = fc.getSelectedFile();
        tfHtml.setText(htmfile.getAbsolutePath());
        java.util.prefs.Preferences pref = java.util.prefs.Preferences.systemRoot();
        pref.put("htmlexecutablepath", tfHtml.getText());
    }

    private void bnOOActionPerformed(java.awt.event.ActionEvent evt) {
        if (newgen.presentation.component.NewGenLibRoot.isOperationgSystemWindows()) {
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
            fc.showOpenDialog(null);
            java.io.File oofile = fc.getSelectedFile();
            tfOO.setText(oofile.getAbsolutePath());
            java.util.prefs.Preferences pref = java.util.prefs.Preferences.systemRoot();
            pref.put("openofficeexecutablepath", tfOO.getText());
        } else {
            newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("<html>you are working on linux machine.<br>so please enter appropriate linux command.<br> eg:openoffice.org-2.0 -writer </html>");
        }
    }

    private void bnDOODActionPerformed(java.awt.event.ActionEvent evt) {
        if (tableformat.getSelectedRow() != -1 && !format.equals("") && !ooFile.equals("") && !htmlFile.equals("")) {
            if (tfOO.getText().trim().length() > 0) {
                try {
                    java.io.File fOO = new java.io.File(ooFile);
                    java.util.prefs.Preferences pref = java.util.prefs.Preferences.systemRoot();
                    Runtime rt = Runtime.getRuntime();
                    rt.exec(pref.get("openofficeexecutablepath", "") + " " + fOO);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("ConfigureOpenOfficeExecutablepath"));
                dialog1.setSize(650, 250);
                dialog1.setLocation(newgen.presentation.NewGenMain.getAppletInstance().getLocation(350, 450));
                dialog1.show();
            }
        } else {
            newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("PLEASE DOUBLE CLICK ON THE ROW");
        }
    }

    private void bnCancelActionPerformed(java.awt.event.ActionEvent evt) {
        tableformat.clearSelection();
        refresh();
    }

    private void bnCloseActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void bnOkActionPerformed(java.awt.event.ActionEvent evt) {
        if (tableformat.getSelectedRow() != -1 && !format.equals("") && !ooFile.equals("") && !htmlFile.equals("")) {
            if (tfTitle.getText().trim().length() > 0) {
                if (tfPrefix.getText().trim().length() > 0) {
                    String formatid = dtm1.getValueAt(tableformat.getSelectedRow(), 0).toString();
                    String newFormat = utility.getTestedString(format);
                    String title = utility.getTestedString(tfTitle.getText());
                    String prefix = utility.getTestedString(tfPrefix.getText());
                    String imStatus = "";
                    String emailStatus = "";
                    String printStatus = "";
                    String genOO = "";
                    String genHTML = "";
                    String genText = "";
                    if (rbMandatoryIM.isSelected()) {
                        imStatus = "A";
                    } else if (rbOptionalIM.isSelected()) {
                        imStatus = "B";
                    } else {
                        imStatus = "C";
                    }
                    if (rbMandatoryEm.isSelected()) {
                        emailStatus = "A";
                    } else if (rbOptionalEm.isSelected()) {
                        emailStatus = "B";
                    } else {
                        emailStatus = "C";
                    }
                    if (rbMandatoryPr.isSelected()) {
                        printStatus = "A";
                    } else if (rbOptionalPr.isSelected()) {
                        printStatus = "B";
                    } else {
                        printStatus = "C";
                    }
                    if (rbGenOOYes.isSelected()) {
                        genOO = "A";
                    } else {
                        genOO = "B";
                    }
                    if (rbGenHTMLYes.isSelected()) {
                        genHTML = "A";
                    } else {
                        genHTML = "B";
                    }
                    if (rbGenTextYes.isSelected()) {
                        genText = "A";
                    } else {
                        genText = "B";
                    }
                    String xmlStr;
                    org.jdom.Element root = new org.jdom.Element("OperationId");
                    root.setAttribute("no", "7");
                    utility.addLoginDetailsToTheRootElement(root);
                    org.jdom.Element formatId = new org.jdom.Element("FORMAT_ID");
                    formatId.setText(formatid);
                    root.addContent(formatId);
                    org.jdom.Element format = new org.jdom.Element("FORMAT");
                    format.setText(newFormat);
                    root.addContent(format);
                    org.jdom.Element formatTitle = new org.jdom.Element("TITLE");
                    formatTitle.setText(title);
                    root.addContent(formatTitle);
                    org.jdom.Element formatPrifix = new org.jdom.Element("PREFIX");
                    formatPrifix.setText(prefix);
                    root.addContent(formatPrifix);
                    org.jdom.Element instMesStatus = new org.jdom.Element("INSTANT_MESSAGE_STATUS");
                    instMesStatus.setText(imStatus);
                    root.addContent(instMesStatus);
                    org.jdom.Element emStatus = new org.jdom.Element("EMAIL_STATUS");
                    emStatus.setText(emailStatus);
                    root.addContent(emStatus);
                    org.jdom.Element prStatus = new org.jdom.Element("PRINT_STATUS");
                    prStatus.setText(printStatus);
                    root.addContent(prStatus);
                    org.jdom.Element genOODoc = new org.jdom.Element("GEN_OO");
                    genOODoc.setText(genOO);
                    root.addContent(genOODoc);
                    org.jdom.Element genHTMLDoc = new org.jdom.Element("GEN_HTML");
                    genHTMLDoc.setText(genHTML);
                    root.addContent(genHTMLDoc);
                    org.jdom.Element genTextDoc = new org.jdom.Element("GEN_TEXT");
                    genTextDoc.setText(genText);
                    root.addContent(genTextDoc);
                    org.jdom.Document doc = new org.jdom.Document(root);
                    xmlStr = (new org.jdom.output.XMLOutputter()).outputString(doc);
                    xmlStr = newgen.presentation.component.ServletConnector.getInstance().sendRequest("FormLetterServlet", xmlStr);
                    postDocument();
                    org.jdom.Element root1 = newGenXMLGenerator.getRootElement(xmlStr);
                    String update = root1.getChildText("Update");
                    if (update.equals("Y")) {
                        refresh();
                        newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("TaskSuccessful"));
                    } else {
                        refresh();
                        newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("Not Posted");
                    }
                } else {
                    newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("Please Enter the Prefix");
                }
            } else {
                newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("Please Enter the Title");
            }
        } else {
            newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("PLEASE DOUBLE CLICK ON THE ROW");
        }
    }

    private void bnCLoseActionPerformed(java.awt.event.ActionEvent evt) {
        format = utility.getTestedString(tpTemplate.getText());
        dialog.dispose();
    }

    private void bnDTActionPerformed(java.awt.event.ActionEvent evt) {
        if (tableformat.getSelectedRow() != -1 && !format.equals("")) {
            dialog.setSize(600, 300);
            tpTemplate.setText(format);
            tpTemplate.setCaretPosition(0);
            dialog.setLocation(200, 200);
            dialog.show();
        } else if (tableformat.getSelectedRow() != -1 && format.equals("")) {
            newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("PLEASE DOUBLE CLICK ON THE ROW");
        } else {
            newgen.presentation.NewGenMain.getAppletInstance().showInformationMessage("PLEASE DOUBLE CLICK ON THE ROW");
        }
    }

    private void bnDTMouseClicked(java.awt.event.MouseEvent evt) {
    }

    private void tableformatMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() == 1) {
            refresh();
            format = "";
        } else if (evt.getClickCount() == 2) {
            if (tableformat.getSelectedRow() != -1) {
                String fid = dtm1.getValueAt(tableformat.getSelectedRow(), 0).toString();
                dispDetails(fid);
                getOODocument(fid);
                getHTMLDocument(fid);
            }
        }
    }

    private javax.swing.ButtonGroup b1;

    private javax.swing.ButtonGroup b2;

    private javax.swing.ButtonGroup b3;

    private javax.swing.ButtonGroup b4;

    private javax.swing.ButtonGroup b5;

    private javax.swing.ButtonGroup b6;

    private javax.swing.JButton bHelp;

    private javax.swing.JButton bnCLose;

    private javax.swing.JButton bnCancel;

    private javax.swing.JButton bnClose;

    private javax.swing.JButton bnClose1;

    private javax.swing.JButton bnDH;

    private javax.swing.JButton bnDOOD;

    private javax.swing.JButton bnDT;

    private javax.swing.JButton bnHtml;

    private javax.swing.JButton bnOO;

    private javax.swing.JButton bnOk;

    private javax.swing.JDialog dialog;

    private javax.swing.JDialog dialog1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel10;

    private javax.swing.JPanel jPanel11;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel6;

    private javax.swing.JPanel jPanel7;

    private javax.swing.JPanel jPanel8;

    private javax.swing.JPanel jPanel9;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JLabel lbExcel;

    private javax.swing.JLabel lbHtml;

    private javax.swing.JRadioButton rbGenHTMLNo;

    private javax.swing.JRadioButton rbGenHTMLYes;

    private javax.swing.JRadioButton rbGenOONo;

    private javax.swing.JRadioButton rbGenOOYes;

    private javax.swing.JRadioButton rbGenTextNo;

    private javax.swing.JRadioButton rbGenTextYes;

    private javax.swing.JRadioButton rbMandatoryEm;

    private javax.swing.JRadioButton rbMandatoryIM;

    private javax.swing.JRadioButton rbMandatoryPr;

    private javax.swing.JRadioButton rbNotRequiredEm;

    private javax.swing.JRadioButton rbNotRequiredIM;

    private javax.swing.JRadioButton rbNotRequiredPr;

    private javax.swing.JRadioButton rbOptionalEm;

    private javax.swing.JRadioButton rbOptionalIM;

    private javax.swing.JRadioButton rbOptionalPr;

    private javax.swing.JTable tableformat;

    private javax.swing.JTable tableparas;

    private newgen.presentation.UnicodeTextField tfHtml;

    private newgen.presentation.UnicodeTextField tfOO;

    private newgen.presentation.UnicodeTextField tfPrefix;

    private newgen.presentation.UnicodeTextField tfTitle;

    private javax.swing.JTextPane tpTemplate;

    private javax.swing.table.DefaultTableModel dtm1 = null;

    private javax.swing.table.DefaultTableModel dtm2 = null;

    private String format = "";

    private newgen.presentation.component.FormLetterStyledDocument lsd;

    public static int temp = 0;

    private String ooFile = "";

    private String htmlFile = "";
}
