package reports.circulation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;
import reports.utility.datamodel.technicalprocessing.SEARCHABLE_CATALOGUERECORD;
import reports.utility.datamodel.technicalprocessing.SEARCHABLE_CATALOGUERECORD_KEY;
import reports.utility.datamodel.technicalprocessing.SEARCHABLE_CATALOGUERECORD_MANAGER;

/**
 *
 * @author yogesh
 */
public class Semester_Dept_Report extends javax.swing.JInternalFrame {

    private javax.swing.table.DefaultTableModel tableModel;

    private String FileName;

    private int totalCount;

    private int CurrentStatusCount = 1;

    private int renderingReportStatus = 0;

    Timer timer = null;

    File createFile = null;

    int valforcount = 1;

    int minValue = 1;

    /** Creates new form Semester_Dept_Report */
    public Semester_Dept_Report() {
        initComponents();
        createSemester();
        jProgressBar1.setStringPainted(true);
    }

    public void createSemester() {
        java.sql.Connection conn = null;
        java.sql.Statement st = null;
        java.sql.ResultSet rs = null;
        try {
            tableModel = new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Select", "Semester", "" }) {

                Class[] types = new Class[] { java.lang.Boolean.class, java.lang.Object.class };

                @Override
                public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }
            };
            jTable1.setModel(tableModel);
            jTable1.getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(0);
            jTable1.getColumnModel().getColumn(2).setMaxWidth(0);
            jTable1.getColumnModel().getColumn(2).setMinWidth(0);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(0);
            conn = reports.utility.database.ConnectionPoolFactory.getInstance().getConnectionPool().getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("select s.id,s.name from semester_config s order by s.name asc");
            jProgressBar1.setMaximum(getTotalCount());
            while (rs.next()) {
                Object[] obj = new Object[3];
                obj[0] = Boolean.class;
                obj[1] = rs.getString("name");
                obj[2] = rs.getString("id");
                tableModel.addRow(new Object[] { null, obj[1], obj[2] });
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (st != null) {
                    st.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        jTable1.setModel(tableModel);
        jScrollPane1.setViewportView(jTable1);
        pack();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        Browse = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jPanel2 = new javax.swing.JPanel();
        GenereteReport = new javax.swing.JButton();
        Ok = new javax.swing.JButton();
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));
        jScrollPane1.setMaximumSize(new java.awt.Dimension(2147483647, 72));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null }, { null, null }, { null, null }, { null, null } }, new String[] { "Title 1", "Title 2" }));
        jScrollPane1.setViewportView(jTable1);
        getContentPane().add(jScrollPane1);
        jPanel1.setLayout(new java.awt.GridBagLayout());
        jLabel1.setText("Select the file");
        jPanel1.add(jLabel1, new java.awt.GridBagConstraints());
        jTextField1.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        jPanel1.add(jTextField1, gridBagConstraints);
        Browse.setText("...");
        Browse.setAlignmentX(0.5F);
        Browse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BrowseActionPerformed(evt);
            }
        });
        jPanel1.add(Browse, new java.awt.GridBagConstraints());
        getContentPane().add(jPanel1);
        jProgressBar1.setPreferredSize(new java.awt.Dimension(351, 27));
        jPanel3.add(jProgressBar1);
        getContentPane().add(jPanel3);
        GenereteReport.setText("GenerateReport");
        GenereteReport.setAlignmentX(0.5F);
        GenereteReport.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        GenereteReport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        GenereteReport.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GenereteReportActionPerformed(evt);
            }
        });
        jPanel2.add(GenereteReport);
        Ok.setText("Ok");
        Ok.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OkActionPerformed(evt);
            }
        });
        jPanel2.add(Ok);
        getContentPane().add(jPanel2);
        pack();
    }

    private void BrowseActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File file) {
                boolean status = false;
                try {
                    String fileName = file.getName().toLowerCase();
                    status = fileName.endsWith(".csv");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return status;
            }

            @Override
            public String getDescription() {
                return ".csv";
            }
        });
        int i = chooser.showSaveDialog(this);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (i == JFileChooser.APPROVE_OPTION) {
            String file = chooser.getSelectedFile().toString();
            StringTokenizer str = new StringTokenizer(file, ".");
            if (str.countTokens() <= 2) {
                if (str.countTokens() == 1) {
                    createFile = new File(chooser.getSelectedFile().toString() + ".csv");
                    if (createFile.exists()) {
                        int cnt = JOptionPane.showConfirmDialog(this, "This file already exists ! Are you sure \n you want to over write it.", "check", JOptionPane.OK_CANCEL_OPTION);
                        if (cnt == 0) {
                            jTextField1.setText(createFile.toString());
                            System.out.println("override");
                        } else {
                            createFile = null;
                            jTextField1.setText("");
                        }
                    } else {
                        jTextField1.setText(createFile.toString());
                    }
                } else {
                    str.nextToken();
                    String s1 = str.nextToken(".");
                    if (s1.equalsIgnoreCase("csv")) {
                        createFile = new File(chooser.getSelectedFile().toString());
                        if (createFile.exists()) {
                            int cnt = JOptionPane.showConfirmDialog(this, "This file already exists ! Are you sure \n you want to over write it.", "check", JOptionPane.OK_CANCEL_OPTION);
                            if (cnt == 0) {
                                jTextField1.setText(createFile.toString());
                                System.out.println("override");
                            } else {
                                createFile = null;
                                jTextField1.setText("");
                            }
                        } else {
                            jTextField1.setText(createFile.toString());
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "The given file is not in .csv format \n Please create .csv extension.", "check", JOptionPane.CANCEL_OPTION);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "The given file name is not correct \n Please create a new file.", "check", JOptionPane.YES_OPTION);
            }
        } else {
            jTextField1.setText("");
        }
    }

    private void GenereteReportActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            jProgressBar1.setValue(0);
            final int count = tableModel.getRowCount();
            renderingReportStatus = 0;
            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;
            setTotalCount(count);
            if (renderingReportStatus < 3) {
                if (createFile != null) {
                    int finalCount = 0;
                    if (count > 0) {
                        for (int i = 0; i < count; i++) {
                            Boolean b = (Boolean) tableModel.getValueAt(i, 0);
                            if (b != null) {
                                if (b == true) {
                                    int id = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
                                    try {
                                        conn = reports.utility.database.ConnectionPoolFactory.getInstance().getConnectionPool().getConnection();
                                        st = conn.createStatement();
                                        rs = st.executeQuery("select count(*) from dept d,cataloguerecord_semester c where c.semesterid='" + id + "' and c.dept_id = d.dept_id");
                                        while (rs.next()) {
                                            int val = rs.getInt(1);
                                            finalCount += val;
                                        }
                                        rs.close();
                                        st.close();
                                        setCurrentStatusCount(jProgressBar1.getMaximum());
                                        renderingReportStatus = 1;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        java.io.BufferedWriter writer = null;
                        try {
                            File file = new File(jTextField1.getText());
                            writer = new java.io.BufferedWriter(new java.io.FileWriter(file));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        int semId = 0;
                        int deptId = 0;
                        for (int i = 0; i < count; i++) {
                            Boolean b = (Boolean) tableModel.getValueAt(i, 0);
                            if (b != null) {
                                if (b == true) {
                                    int id = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
                                    try {
                                        conn = reports.utility.database.ConnectionPoolFactory.getInstance().getConnectionPool().getConnection();
                                        st = conn.createStatement();
                                        rs = st.executeQuery("select c.cataloguerecordid,c.ownerlibraryid,c.semesterid,c.dept_id,d.dept_name from dept d,cataloguerecord_semester c where c.semesterid='" + id + "' and c.dept_id = d.dept_id");
                                        while (rs.next()) {
                                            int semID = rs.getInt("semesterid");
                                            int deptID = rs.getInt("dept_id");
                                            if (semId != semID || deptId != deptID) {
                                                semId = semID;
                                                deptId = deptID;
                                                writer.newLine();
                                                writer.newLine();
                                                writer.write("Semester :" + tableModel.getValueAt(i, 1).toString() + "," + "Department :" + rs.getString("dept_name"));
                                                writer.newLine();
                                                writer.write("Author" + "," + "Title" + "," + "Edition" + "," + "Publisher" + "," + "Isbn");
                                                writer.newLine();
                                                writer.newLine();
                                            }
                                            org.hibernate.Session session = tools.HibernateUtil.getSessionFactory().openSession();
                                            try {
                                                SEARCHABLE_CATALOGUERECORD_KEY catkey = new SEARCHABLE_CATALOGUERECORD_KEY();
                                                catkey.setCataloguerecordid(new Integer(rs.getString("cataloguerecordid")));
                                                catkey.setOwner_library_id(new Integer(rs.getString("ownerlibraryid")));
                                                SEARCHABLE_CATALOGUERECORD catrec = (new SEARCHABLE_CATALOGUERECORD_MANAGER()).load(session, catkey);
                                                String text = catrec.getWholecataloguerecord();
                                                newgenlib.marccomponent.conversion.Converter conv = new newgenlib.marccomponent.conversion.Converter();
                                                Hashtable h = conv.getDetails(text);
                                                String author = h.get("AUTHOR").toString().replaceAll(",", "");
                                                String title = h.get("TITLE").toString().replaceAll(",", "");
                                                String isbn = h.get("ISBN").toString().replaceAll(",", "");
                                                String edition = h.get("EDITION").toString().replaceAll(",", "");
                                                String publisher = h.get("PUBLISHER").toString().replaceAll(",", "");
                                                writer.write(author + "," + title + "," + edition + "," + publisher + "," + isbn);
                                                writer.newLine();
                                                writer.flush();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            } finally {
                                                session.close();
                                            }
                                        }
                                    } catch (Exception ex) {
                                        Logger.getLogger(Semester_Dept_Report.class.getName()).log(Level.SEVERE, null, ex);
                                    } finally {
                                        try {
                                            if (conn != null) {
                                                conn.close();
                                            }
                                            if (st != null) {
                                                st.close();
                                            }
                                            if (rs != null) {
                                                rs.close();
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "There is no files to Check.", "nothing", JOptionPane.OK_OPTION);
                    }
                    setFileName(createFile.toString());
                    jProgressBar1.setMaximum(finalCount);
                    jProgressBar1.setMinimum(minValue);
                    timer = new Timer(1000, new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            while (valforcount <= count) {
                                setCurrentStatusCount(valforcount);
                                setTotalCount(count);
                                valforcount++;
                                jProgressBar1.setValue(getCurrentStatusCount());
                                if (getTotalCount() == getCurrentStatusCount()) {
                                    if (timer != null) {
                                        timer.setRepeats(false);
                                        timer.stop();
                                        System.out.println("Timer is stoped");
                                        JOptionPane.showMessageDialog(null, "Report generated", "check", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                }
                            }
                            refreshData();
                        }
                    });
                    timer.setRepeats(true);
                    timer.start();
                } else {
                    JOptionPane.showMessageDialog(this, "Please select the file to generate the date.", "check", JOptionPane.YES_OPTION);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void OkActionPerformed(java.awt.event.ActionEvent evt) {
        if (timer != null) {
            timer.stop();
        }
        this.dispose();
    }

    private javax.swing.JButton Browse;

    private javax.swing.JButton GenereteReport;

    private javax.swing.JButton Ok;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JProgressBar jProgressBar1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTable jTable1;

    private javax.swing.JTextField jTextField1;

    /**
     * @return the fileName
     */
    public String getFileName() {
        return FileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String FileName) {
        this.FileName = FileName;
    }

    /**
     * @return the TotalCount
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * @param TotalCount the TotalCount to set
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * @return the CurrentStatusCount
     */
    public int getCurrentStatusCount() {
        return CurrentStatusCount;
    }

    /**
     * @param CurrentStatusCount the CurrentStatusCount to set
     */
    public void setCurrentStatusCount(int CurrentStatusCount) {
        this.CurrentStatusCount = CurrentStatusCount;
    }

    /**
     * @return the renderingReportStatus
     */
    public int getRenderingReportStatus() {
        return renderingReportStatus;
    }

    /**
     * @param renderingReportStatus the renderingReportStatus to set
     */
    public void setRenderingReportStatus(int renderingReportStatus) {
        this.renderingReportStatus = renderingReportStatus;
    }

    private void refreshData() {
        setTotalCount(0);
        setCurrentStatusCount(0);
        timer.stop();
        valforcount = 0;
        jProgressBar1.setValue(0);
        setRenderingReportStatus(0);
    }
}