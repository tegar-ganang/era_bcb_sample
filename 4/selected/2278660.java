package org.verus.ngl.client.technicalprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import org.jdom.Element;
import org.verus.ngl.client.components.NGLUtilities;
import org.verus.ngl.client.components.ServletConnector;
import org.verus.ngl.client.guicomponents.OutputSuggestionsPanel;
import org.verus.ngl.utilities.NGLUtility;
import org.verus.ngl.utilities.NGLXMLUtility;
import org.verus.ngl.utilities.transactionLog.Log;
import org.verus.ngl.utilities.transactionLog.Record;

/**
 *
 * @author  root
 */
public class NGLDigitalAttachmentsPanel extends javax.swing.JPanel {

    /** Creates new form NGLDigitalAttachmentsPanel */
    public NGLDigitalAttachmentsPanel(OutputSuggestionsPanel panelOutSugg, String catalogueId) {
        this.panelOutSugg = panelOutSugg;
        this.catalogueId = catalogueId;
        initComponents();
        initSettings();
    }

    private void initSettings() {
        utility = NGLUtility.getInstance();
        initTableSettings();
        refresh();
    }

    private void refresh() {
        String libraryId = utility.getTestedStringWithTrim(NGLUtilities.getInstance().getLibraryId());
        String response = getAttachments(libraryId, catalogueId);
        setAttachments(response);
    }

    private void setAttachments(String xmlAttachments) {
        dtmAttachments.setRowCount(0);
        Element root = NGLXMLUtility.getInstance().getRootElementFromXML(xmlAttachments);
        if (root != null) {
            String status = utility.getTestedStringWithTrim(root.getChildText("Status"));
            if (status.equalsIgnoreCase("SUCCESS")) {
                List listAttachments = root.getChildren("Attachment");
                if (listAttachments != null && listAttachments.size() > 0) {
                    htAttachments = new Hashtable();
                    for (int i = 0; i < listAttachments.size(); i++) {
                        Element eleAttachment = (Element) listAttachments.get(i);
                        if (eleAttachment != null) {
                            String id = utility.getTestedStringWithTrim(eleAttachment.getChildText("Id"));
                            String libraryId = utility.getTestedStringWithTrim(eleAttachment.getChildText("LibraryId"));
                            String catalogueId = utility.getTestedStringWithTrim(eleAttachment.getChildText("CatalogueId"));
                            String fileName = utility.getTestedStringWithTrim(eleAttachment.getChildText("FileName"));
                            String displayName = utility.getTestedStringWithTrim(eleAttachment.getChildText("DisplayName"));
                            String description = utility.getTestedStringWithTrim(eleAttachment.getChildText("Description"));
                            String extension = utility.getTestedStringWithTrim(eleAttachment.getChildText("Extension"));
                            String categoryId = utility.getTestedStringWithTrim(eleAttachment.getChildText("CategoryId"));
                            String fileLocation = utility.getTestedStringWithTrim(eleAttachment.getChildText("FileLocation"));
                            String transactionLog = utility.getTestedStringWithTrim(eleAttachment.getChildText("TransactionLog"));
                            Hashtable htAttachment = new Hashtable();
                            htAttachment.put("Id", id);
                            htAttachment.put("LibraryId", utility.getTestedStringWithTrim(NGLUtilities.getInstance().getLibraryId()));
                            htAttachment.put("CatalogueId", catalogueId);
                            htAttachment.put("FileName", fileName);
                            htAttachment.put("FilePath", "{server}/" + fileName);
                            htAttachment.put("Extension", extension);
                            htAttachment.put("DisplayName", displayName);
                            htAttachment.put("Description", description);
                            htAttachment.put("CategoryId", categoryId);
                            htAttachment.put("FileLocation", fileLocation);
                            htAttachment.put("TransactionLog", transactionLog);
                            htAttachment.put("Status", "S");
                            htAttachments.put(id, htAttachment);
                            dtmAttachments.addRow(getVectorRow(false, id, fileName, "{server}/" + fileName, displayName, description));
                        }
                    }
                    if (listAttachments.size() == 1) {
                        panelOutSugg.setMessage(0, 0, "one attachment is found");
                    } else {
                        panelOutSugg.setMessage(0, 0, listAttachments.size() + " Attachments are found");
                    }
                } else {
                    panelOutSugg.setMessage(0, 0, "Attachments are not found");
                }
            } else {
                panelOutSugg.setMessage(0, 1, "Failed to retrieve the attachment(s)");
            }
        } else {
            panelOutSugg.setMessage(0, 1, "Failed to retrieve the attachment(s)");
        }
    }

    private String getAttachments(String libraryId, String catalogueId) {
        String xmlRequest = this.getRequestXML(libraryId, catalogueId);
        return "";
    }

    private String getRequestXML(String libraryId, String catalogueId) {
        return this.getRequestXML(libraryId, catalogueId, "", "", "", "", "", "", "");
    }

    private void initTableSettings() {
        Object[] columns = new Object[] { org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("Select"), org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("Id"), org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("FileName"), org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("FilePath"), org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("DisplayName"), org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("Description") };
        dtmAttachments = new DefaultTableModel(columns, 0) {

            public boolean isCellEditable(int row, int col) {
                if (col == 0) {
                    return true;
                }
                return false;
            }

            public Class getColumnClass(int col) {
                return getValueAt(0, col).getClass();
            }
        };
        tableAttachments.setModel(dtmAttachments);
        tableAttachments.getColumnModel().getColumn(0).setMaxWidth(100);
        tableAttachments.getColumnModel().getColumn(0).setMinWidth(30);
        tableAttachments.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableAttachments.getColumnModel().getColumn(1).setMaxWidth(0);
        tableAttachments.getColumnModel().getColumn(1).setMinWidth(0);
        tableAttachments.getColumnModel().getColumn(1).setPreferredWidth(0);
        tableAttachments.getColumnModel().getColumn(2).setMaxWidth(150);
        tableAttachments.getColumnModel().getColumn(2).setMinWidth(50);
        tableAttachments.getColumnModel().getColumn(2).setPreferredWidth(100);
        tableAttachments.getColumnModel().getColumn(3).setMaxWidth(200);
        tableAttachments.getColumnModel().getColumn(3).setMinWidth(100);
        tableAttachments.getColumnModel().getColumn(3).setPreferredWidth(150);
        tableAttachments.getColumnModel().getColumn(4).setMaxWidth(150);
        tableAttachments.getColumnModel().getColumn(4).setMinWidth(50);
        tableAttachments.getColumnModel().getColumn(4).setPreferredWidth(100);
        tableAttachments.getColumnModel().getColumn(5).sizeWidthToFit();
        tableAttachments.setSelectionMode(tableAttachments.getSelectionModel().SINGLE_SELECTION);
        tableAttachments.getTableHeader().setReorderingAllowed(false);
    }

    public void setParentDialog(JDialog parent) {
        parentDialog = parent;
    }

    public boolean bnOkMethod() {
        System.out.println("hashtable ====================== " + htAttachments);
        if (htAttachments != null) {
            Enumeration enumeration = htAttachments.keys();
            int noOfFiles = 0;
            Hashtable htAttachmentData = new Hashtable();
            String oldLocation = this.getFileLocation(htAttachments);
            String fileLocation = oldLocation;
            Vector vFailedAttachments = new Vector();
            while (enumeration.hasMoreElements()) {
                String id = utility.getTestedString(enumeration.nextElement());
                Hashtable htAttachment = (Hashtable) htAttachments.get(id);
                if (htAttachment != null) {
                    id = utility.getTestedString(htAttachment.get("Id"));
                    String libraryId = utility.getTestedString(htAttachment.get("LibraryId"));
                    String catalogueId = utility.getTestedString(htAttachment.get("CatalogueId"));
                    String fileName = utility.getTestedString(htAttachment.get("FileName"));
                    String filePath = utility.getTestedString(htAttachment.get("FilePath"));
                    String extension = utility.getTestedString(htAttachment.get("Extension"));
                    String displayName = utility.getTestedString(htAttachment.get("DisplayName"));
                    String description = utility.getTestedString(htAttachment.get("Description"));
                    String categoryId = utility.getTestedString(htAttachment.get("CategoryId"));
                    String transactionLog = utility.getTestedString(htAttachment.get("TransactionLog"));
                    String status = utility.getTestedString(htAttachment.get("Status"));
                    if (status.equalsIgnoreCase("L")) {
                        String xmlRequest = this.getRequestXML(libraryId, catalogueId, fileName, displayName, description, extension, categoryId, fileLocation, transactionLog);
                        String response = "";
                        if (status.equalsIgnoreCase("SUCCESS")) {
                            id = this.getId(response);
                            if (!id.equalsIgnoreCase("")) {
                                htAttachmentData.put("Id", id);
                                htAttachmentData.put("CatalogueId", catalogueId);
                                htAttachmentData.put("Process", "U");
                                htAttachmentData.put("FileName", fileName);
                                oldLocation = fileLocation;
                                htAttachmentData.put("OldLocation", oldLocation);
                                File file = new File(filePath);
                                try {
                                    FileChannel readChannel = new FileInputStream(file).getChannel();
                                    long size = readChannel.size();
                                    if (size < Integer.MAX_VALUE) {
                                        int offset = 0;
                                        boolean flagStatus = true;
                                        while (offset < size && flagStatus) {
                                            byte[] bytes = null;
                                            if (offset < size - (1024 * 100)) {
                                                bytes = new byte[1024 * 100];
                                                readChannel.map(FileChannel.MapMode.READ_ONLY, offset, 1024 * 100).get(bytes);
                                            } else {
                                                bytes = new byte[(int) size - offset];
                                                readChannel.map(FileChannel.MapMode.READ_ONLY, offset, size - offset).get(bytes);
                                            }
                                            htAttachmentData.put("FileLocation", fileLocation);
                                            htAttachmentData.put("Bytes", bytes);
                                            Hashtable htResponse = (Hashtable) ServletConnector.getInstance().sendObjectRequest("NGLDigitalAttachmentsHandler", htAttachmentData);
                                            if (htResponse != null) {
                                                status = utility.getTestedStringWithTrim(htResponse.get("Status"));
                                                System.out.println("status ========================= " + status);
                                                if (status.equalsIgnoreCase("SUCCESS")) {
                                                    String newLocation = utility.getTestedString(htResponse.get("FileLocation"));
                                                    System.out.println("newLocation ===================== " + newLocation + ", fileLocation ================= " + fileLocation);
                                                    if (!newLocation.equalsIgnoreCase(fileLocation)) {
                                                        xmlRequest = getRequestXML(libraryId, catalogueId, newLocation);
                                                        if (this.getStatus(response).equalsIgnoreCase("SUCCESS")) {
                                                            fileLocation = newLocation;
                                                        } else {
                                                            htAttachmentData.put("Process", "DR");
                                                            htAttachmentData.put("SourceLocation", newLocation);
                                                            htAttachmentData.put("RestoreLocation", fileLocation);
                                                            htResponse = (Hashtable) ServletConnector.getInstance().sendObjectRequest("NGLDigitalAttachmentsHandler", htAttachmentData);
                                                            flagStatus = false;
                                                        }
                                                    }
                                                } else {
                                                    xmlRequest = getRequestXML(id, libraryId, catalogueId, oldLocation);
                                                    fileLocation = oldLocation;
                                                    flagStatus = false;
                                                }
                                            } else {
                                                xmlRequest = getRequestXML(id, libraryId, catalogueId, oldLocation);
                                                fileLocation = oldLocation;
                                                flagStatus = false;
                                            }
                                            offset += 1024 * 100;
                                        }
                                        if (!flagStatus) {
                                            noOfFiles++;
                                            vFailedAttachments.addElement(htAttachment);
                                        } else {
                                            System.out.println("Updating status..............");
                                            System.out.println("id======" + id + ", libraryId ======= " + libraryId);
                                            xmlRequest = getRequestXML(id, libraryId, catalogueId, "", "S");
                                            System.out.println("xmlRequest ================== " + xmlRequest);
                                            System.out.println("response ============================ " + response);
                                            if (!getStatus(response).equalsIgnoreCase("SUCCESS")) {
                                                noOfFiles++;
                                                vFailedAttachments.addElement(htAttachment);
                                            } else {
                                                System.out.println("Failed to update status...........");
                                            }
                                        }
                                    } else {
                                        panelOutSugg.setMessage(0, 1, "File is too large");
                                    }
                                } catch (Exception ex) {
                                    System.out.println("Exception ============== " + ex);
                                }
                            }
                        }
                    } else if (status.equalsIgnoreCase("M")) {
                    } else if (status.equalsIgnoreCase("D")) {
                    }
                }
            }
            if (noOfFiles > 0) {
                panelOutSugg.setMessage(0, 1, "Failed to upload " + noOfFiles + " file(s)");
            } else {
                return true;
            }
        }
        return false;
    }

    private String getRequestXML(String id, String libraryId, String catalogueId, String fileLocation, String status) {
        String xml = this.getRequestXML(id, libraryId, catalogueId, fileLocation);
        System.out.println("xml ==================== " + xml);
        Element root = NGLXMLUtility.getInstance().getRootElementFromXML(xml);
        Element eleStatus = root.getChild("Status");
        eleStatus.setText(status);
        Element copyEle = (Element) root.clone();
        String xmlRequest = NGLXMLUtility.getInstance().generateXML(copyEle);
        System.out.println("xmlRequest ============= " + xmlRequest);
        return xmlRequest;
    }

    private String getStatus(String strXml) {
        Element root = NGLXMLUtility.getInstance().getRootElementFromXML(strXml);
        if (root != null) {
            return utility.getTestedStringWithTrim(root.getChildText("Status"));
        }
        return "";
    }

    private String getId(String strXml) {
        Element root = NGLXMLUtility.getInstance().getRootElementFromXML(strXml);
        if (root != null) {
            return utility.getTestedStringWithTrim(root.getChildText("Id"));
        }
        return "";
    }

    private String getRequestXML(String libraryId, String catalogueId, String fileLocation) {
        return this.getRequestXML("", libraryId, catalogueId, fileLocation);
    }

    private String getRequestXML(String id, String libraryId, String catalogueId, String fileLocation) {
        return this.getRequestXML(id, libraryId, catalogueId, "", "", "", "", "", fileLocation, "");
    }

    private String getRequestXML(String id, String libraryId, String catalogueId, String fileName, String displayName, String description, String extension, String categoryId, String fileLocation, String transactionLog) {
        Element root = NGLXMLUtility.getInstance().getRequestDataRoot("NGLDigitalAttachmentsHandler", "1");
        Element eleId = new Element("Id");
        eleId.setText(id);
        root.addContent(eleId);
        Element eleLibraryId = new Element("LibraryId");
        eleLibraryId.setText(libraryId);
        root.addContent(eleLibraryId);
        Element eleCatalogueId = new Element("CatalogueId");
        eleCatalogueId.setText(catalogueId);
        root.addContent(eleCatalogueId);
        Element eleFileName = new Element("FileName");
        eleFileName.setText(fileName);
        root.addContent(eleFileName);
        Element eleDisplayName = new Element("DisplayName");
        eleDisplayName.setText(displayName);
        root.addContent(eleDisplayName);
        Element eleDescription = new Element("Description");
        eleDescription.setText(description);
        root.addContent(eleDescription);
        Element eleExtension = new Element("Extension");
        eleExtension.setText(extension);
        root.addContent(eleExtension);
        Element eleCategoryId = new Element("CategoryId");
        eleCategoryId.setText(categoryId);
        root.addContent(eleCategoryId);
        Element eleFileLocation = new Element("FileLocation");
        eleFileLocation.setText(fileLocation);
        root.addContent(eleFileLocation);
        Element eleTransactionLog = new Element("TransactionLog");
        eleTransactionLog.setText(transactionLog);
        root.addContent(eleTransactionLog);
        Element eleStatus = new Element("Status");
        eleStatus.setText("F");
        root.addContent(eleStatus);
        return NGLXMLUtility.getInstance().generateXML(root);
    }

    private String getRequestXML(String libraryId, String catalogueId, String fileName, String displayName, String description, String extension, String categoryId, String fileLocation, String transactionLog) {
        return this.getRequestXML("", libraryId, catalogueId, fileName, displayName, description, extension, categoryId, fileLocation, transactionLog);
    }

    private String getFileLocation(Hashtable htAttachments) {
        if (htAttachments != null) {
            Enumeration enumeration = htAttachments.keys();
            while (enumeration.hasMoreElements()) {
                Hashtable htAttachment = (Hashtable) htAttachments.get(enumeration.nextElement());
                if (htAttachment != null) {
                    String id = utility.getTestedStringWithTrim(htAttachment.get("Id"));
                    if (!id.equalsIgnoreCase("") && !id.startsWith("local")) {
                        return utility.getTestedStringWithTrim(htAttachment.get("FileLocation"));
                    }
                }
            }
        }
        return "";
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        spAttachments = new javax.swing.JScrollPane();
        tableAttachments = new javax.swing.JTable();
        panelButtons = new javax.swing.JPanel();
        bnAdd = new javax.swing.JButton();
        bnEdit = new javax.swing.JButton();
        bnDelete = new javax.swing.JButton();
        setLayout(new java.awt.BorderLayout());
        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        spAttachments.setBorder(javax.swing.BorderFactory.createTitledBorder(org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("NGLDigitalAttachments")));
        tableAttachments.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        spAttachments.setViewportView(tableAttachments);
        add(spAttachments, java.awt.BorderLayout.CENTER);
        panelButtons.setLayout(new java.awt.GridBagLayout());
        bnAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/verus/ngl/client/images/Add16.gif")));
        bnAdd.setMnemonic('a');
        bnAdd.setToolTipText(org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("Add"));
        bnAdd.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnAddActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        panelButtons.add(bnAdd, gridBagConstraints);
        bnEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/verus/ngl/client/images/Edit16.gif")));
        bnEdit.setMnemonic('m');
        bnEdit.setToolTipText(org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("Edit"));
        bnEdit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnEditActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        panelButtons.add(bnEdit, gridBagConstraints);
        bnDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/verus/ngl/client/images/Delete16.gif")));
        bnDelete.setMnemonic('d');
        bnDelete.setToolTipText(org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("Remove"));
        bnDelete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnDeleteActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        panelButtons.add(bnDelete, gridBagConstraints);
        add(panelButtons, java.awt.BorderLayout.EAST);
    }

    private void bnDeleteMethod() {
        int[] selectedRows = this.getSelectedRows();
        if (selectedRows != null && selectedRows.length > 0) {
            int result = JOptionPane.NO_OPTION;
            if (parentDialog != null) {
                result = JOptionPane.showConfirmDialog(parentDialog, "Are you sure you want to delete the selected " + selectedRows.length + " record(s)?", "Confirm dialog", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            } else {
                result = JOptionPane.showConfirmDialog(NGLUtilities.getInstance().getNGLFrame(), "Are you sure you want to delete the selected " + selectedRows.length + " record(s)?", "Confirm dialog", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            }
            if (result == JOptionPane.YES_OPTION) {
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    int row = selectedRows[i];
                    if (row >= 0) {
                        String id = utility.getTestedString(tableAttachments.getValueAt(row, 1));
                        if (id.startsWith("local")) {
                            htAttachments.remove(id);
                        } else {
                            Hashtable htAttachment = (Hashtable) htAttachments.get(id);
                            if (htAttachment != null) {
                                htAttachment.put("Status", "D");
                            }
                        }
                        dtmAttachments.removeRow(row);
                    }
                }
                if (selectedRows.length == 1) {
                    panelOutSugg.setMessage(0, 0, "Selected record is deleted successfully");
                } else {
                    panelOutSugg.setMessage(0, 0, "Selected " + selectedRows.length + " records are deleted successfully");
                }
            }
        } else {
            panelOutSugg.setMessage(1, 0, "Please select at least one record");
        }
    }

    private int[] getSelectedRows() {
        try {
            if (tableAttachments.getRowCount() > 0) {
                Vector vSelectedRows = new Vector();
                for (int i = 0; i < tableAttachments.getRowCount(); i++) {
                    Boolean isSelected = (Boolean) tableAttachments.getValueAt(i, 0);
                    if (isSelected) {
                        vSelectedRows.addElement(i);
                    }
                }
                if (vSelectedRows.size() > 0) {
                    int[] selectedRows = new int[vSelectedRows.size()];
                    for (int i = 0; i < vSelectedRows.size(); i++) {
                        selectedRows[i] = Integer.parseInt(vSelectedRows.get(i).toString());
                    }
                    return selectedRows;
                } else if (tableAttachments.getSelectedRow() >= 0) {
                    return new int[] { tableAttachments.getSelectedRow() };
                }
            }
        } catch (Exception e) {
            System.out.println("Exception in getSelectedIndexes method : " + e);
        }
        return null;
    }

    private void bnDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        bnDeleteMethod();
    }

    private void bnEditMethod() {
        int row = tableAttachments.getSelectedRow();
        if (row >= 0) {
            String id = utility.getTestedString(tableAttachments.getValueAt(row, 1));
            Hashtable htAttachment = (Hashtable) htAttachments.get(id);
            if (htAttachment != null) {
                NGLDigitalAttachmentCreationDialog dialog = null;
                if (parentDialog != null) {
                    dialog = new NGLDigitalAttachmentCreationDialog(parentDialog, true);
                } else {
                    dialog = new NGLDigitalAttachmentCreationDialog(NGLUtilities.getInstance().getNGLFrame(), true);
                }
                String fileName = utility.getTestedString(htAttachment.get("FileName"));
                String filePath = utility.getTestedString(htAttachment.get("FilePath"));
                String displayName = utility.getTestedString(htAttachment.get("DisplayName"));
                String description = utility.getTestedString(htAttachment.get("Description"));
                String categoryId = utility.getTestedString(htAttachment.get("CategoryId"));
                dialog.setData(filePath, displayName, description, categoryId);
                dialog.setTitle(org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("EditAttachment"));
                dialog.setVisible(true);
                if (dialog.getClickedButton() == dialog.BUTTON_OK) {
                    if (!utility.getTestedStringWithTrim(htAttachment.get("Id")).startsWith("local")) {
                        htAttachment.put("Status", "M");
                    } else {
                        filePath = dialog.getFilePath();
                        fileName = dialog.getFileName(filePath);
                        String extension = dialog.getFileExtension(fileName);
                        htAttachment.put("FilePath", filePath);
                        htAttachment.put("FileName", fileName);
                        htAttachment.put("Extension", extension);
                    }
                    displayName = dialog.getDisplayName();
                    description = dialog.getDescription();
                    categoryId = dialog.getCategoryId();
                    htAttachment.put("DisplayName", displayName);
                    htAttachment.put("Description", description);
                    htAttachment.put("CategoryId", categoryId);
                    dtmAttachments.removeRow(row);
                    dtmAttachments.insertRow(row, getVectorRow(false, id, fileName, filePath, displayName, description));
                }
            }
        } else {
            panelOutSugg.setMessage(1, 0, "Please select any one record");
        }
    }

    private void bnEditActionPerformed(java.awt.event.ActionEvent evt) {
        bnEditMethod();
    }

    private void bnAddMethod() {
        NGLDigitalAttachmentCreationDialog dialog = null;
        if (parentDialog != null) {
            dialog = new NGLDigitalAttachmentCreationDialog(parentDialog, true);
        } else {
            dialog = new NGLDigitalAttachmentCreationDialog(NGLUtilities.getInstance().getNGLFrame(), true);
        }
        dialog.setTitle(org.verus.ngl.client.main.NGLResourceBundle.getInstance().getString("AddAttachment"));
        dialog.setVisible(true);
        if (dialog.getClickedButton() == dialog.BUTTON_OK) {
            String id = "local" + localId++;
            String filePath = dialog.getFilePath();
            String fileName = dialog.getFileName(filePath);
            String displayName = dialog.getDisplayName();
            String description = dialog.getDescription();
            String extension = dialog.getFileExtension(fileName);
            String categoryId = dialog.getCategoryId();
            String fileLocation = this.getFileLocation(htAttachments);
            Log transactionLog = new Log("");
            Record record = new Record(new Date().getTime(), NGLUtilities.getInstance().getUserId(), Record.TYPE_CREATE, "");
            String strLog = transactionLog.addRecord(record);
            if (htAttachments == null) {
                htAttachments = new Hashtable();
            }
            Hashtable htAttachment = new Hashtable();
            htAttachment.put("Id", id);
            htAttachment.put("LibraryId", utility.getTestedStringWithTrim(NGLUtilities.getInstance().getLibraryId()));
            htAttachment.put("CatalogueId", catalogueId);
            htAttachment.put("FileName", fileName);
            htAttachment.put("FilePath", filePath);
            htAttachment.put("Extension", extension);
            htAttachment.put("DisplayName", displayName);
            htAttachment.put("Description", description);
            htAttachment.put("CategoryId", categoryId);
            htAttachment.put("FileLocation", fileLocation);
            htAttachment.put("TransactionLog", strLog);
            htAttachment.put("Status", "L");
            htAttachments.put(id, htAttachment);
            dtmAttachments.addRow(getVectorRow(false, id, fileName, filePath, displayName, description));
        }
    }

    private Vector getVectorRow(Boolean isSelected, Object id, Object fileName, Object filePath, Object displayName, Object description) {
        Vector vRow = new Vector();
        vRow.addElement(isSelected);
        vRow.addElement(id);
        vRow.addElement(fileName);
        vRow.addElement(filePath);
        vRow.addElement(displayName);
        vRow.addElement(description);
        return vRow;
    }

    private void bnAddActionPerformed(java.awt.event.ActionEvent evt) {
        bnAddMethod();
    }

    private javax.swing.JButton bnAdd;

    private javax.swing.JButton bnDelete;

    private javax.swing.JButton bnEdit;

    private javax.swing.JPanel panelButtons;

    private javax.swing.JScrollPane spAttachments;

    private javax.swing.JTable tableAttachments;

    private OutputSuggestionsPanel panelOutSugg = null;

    private JDialog parentDialog = null;

    private NGLUtility utility = null;

    private Hashtable htAttachments = null;

    private DefaultTableModel dtmAttachments = null;

    private int localId = 0;

    private String catalogueId = "";
}
