package com.indragunawan.restobiz.app;

import com.indragunawan.restobiz.app.model.PembayaranViewRD;
import com.indragunawan.restobiz.app.model.PembayaranViewTM;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.swing.JOptionPane;
import org.jdesktop.application.Action;

/**
 *
 * @author  igoens
 */
public class PembayaranView extends javax.swing.JDialog {

    private static final long serialVersionUID = 3870468928543382534L;

    private EntityManager pembayaranEntity;

    private GeneralConfig cfg = new GeneralConfig();

    private NumberFormat invoiceDisplay = new DecimalFormat("0000");

    private NumberFormat floatDisplay = new DecimalFormat("#,##0.00");

    private SimpleDateFormat dateDisplay = new SimpleDateFormat("dd/MM/yyyy");

    private SimpleDateFormat dateSQL = new SimpleDateFormat("yyyy-MM-dd");

    private Query pembayaranQuery;

    private List jenisBayarList;

    private int i;

    /** Creates new form PembayaranView */
    public PembayaranView(java.awt.Frame parent, boolean modal, int Nomor, Date Tanggal) {
        super(parent, modal);
        initComponents();
        invoiceField.setText(invoiceDisplay.format(Nomor));
        tanggalField.setText(dateDisplay.format(Tanggal));
        pembayaranEntity = Persistence.createEntityManagerFactory("RestobizPU", cfg.getPersistanceDbProperties()).createEntityManager();
        fillJenisBayarField();
        refreshTabelPembayaran(Nomor, Tanggal);
        resetInputPembayaran();
    }

    private void execTambahPembayaran(String Jenis, Double Jumlah) throws NumberFormatException {
        try {
            int Kuitansi;
            Integer Nomor = Integer.valueOf(invoiceField.getText());
            Date Tanggal = dateDisplay.parse(tanggalField.getText());
            if (persenCheckBox.isSelected()) {
                Jumlah = cfg.getTotalTransaksi(Nomor, Tanggal) * Jumlah / 100;
            }
            if (cfg.isNegativePay(Jenis)) {
                Jumlah = -Jumlah;
            }
            Kuitansi = cfg.getNomorKuitansi();
            pembayaranEntity.getTransaction().begin();
            pembayaranQuery = pembayaranEntity.createNativeQuery("INSERT INTO pembayaran VALUES (#nobukti, #nomor, #tanggal, #jenis, #jumlah, #namauser, #dibayar, #shift)").setParameter("nobukti", Kuitansi).setParameter("nomor", Nomor).setParameter("tanggal", dateSQL.format(Tanggal)).setParameter("jenis", Jenis).setParameter("jumlah", Jumlah).setParameter("namauser", cfg.getLoggedUser()).setParameter("dibayar", dateSQL.format(new Date())).setParameter("shift", cfg.getCurrentShift());
            pembayaranQuery.executeUpdate();
            cfg.incNomorKuitansi();
            pembayaranEntity.getTransaction().commit();
            resetInputPembayaran();
            refreshTabelPembayaran(Nomor, Tanggal);
        } catch (ParseException ex) {
            pembayaranEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(PembayaranView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List openDaftarPembayaran(int Nomor, Date Tanggal) {
        List pembayaranList = null;
        try {
            pembayaranList = pembayaranEntity.createQuery("SELECT p.pembayaranPK.nobukti, p.pembayaranPK.dibayar, j.jenis, p.jumlah FROM Pembayaran p JOIN p.jenis j WHERE p.pembayaranPK.nomor = :nomor AND p.pembayaranPK.tanggal = :tanggal").setParameter("nomor", Nomor).setParameter("tanggal", Tanggal).getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return pembayaranList;
    }

    private void refreshTabelPembayaran(int Nomor, Date Tanggal) {
        pembayaranTable.setModel(new PembayaranViewTM(openDaftarPembayaran(Nomor, Tanggal)));
        for (i = 0; i < pembayaranTable.getColumnCount(); i++) {
            pembayaranTable.getColumnModel().getColumn(i).setCellRenderer(new PembayaranViewRD());
        }
    }

    private void fillJenisBayarField() {
        try {
            jenisBayarList = cfg.getJenisBayar();
            jenisBayarField.removeAllItems();
            for (i = 0; i <= jenisBayarList.size() - 1; i++) {
                Vector result = (Vector) jenisBayarList.get(i);
                jenisBayarField.addItem(result.get(0));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Boolean execHapusPembayaran(Integer NoBukti, Date Dibayar) {
        Double Nilai;
        String Jenis;
        Boolean r = false;
        Jenis = String.valueOf(pembayaranTable.getValueAt(pembayaranTable.getSelectedRow(), 2));
        Nilai = Double.valueOf(String.valueOf(pembayaranTable.getValueAt(pembayaranTable.getSelectedRow(), 3)));
        if (JOptionPane.showConfirmDialog(this, "Hapus pembayaran " + Jenis + " sejumlah " + floatDisplay.format(Nilai) + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                pembayaranEntity.getTransaction().begin();
                pembayaranQuery = pembayaranEntity.createNativeQuery("DELETE FROM pembayaran WHERE nobukti = #nobukti AND dibayar = #dibayar").setParameter("nobukti", NoBukti).setParameter("dibayar", Dibayar);
                pembayaranQuery.executeUpdate();
                pembayaranEntity.getTransaction().commit();
                r = true;
            } catch (Exception ex) {
                pembayaranEntity.getTransaction().rollback();
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return r;
    }

    private void resetInputPembayaran() {
        jenisBayarField.setSelectedIndex(-1);
        jumlahField.setText("");
        jenisBayarField.requestFocus();
        persenCheckBox.setSelected(false);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        pembayaranPopup = new javax.swing.JPopupMenu();
        hapusMenuItem = new javax.swing.JMenuItem();
        headerPanel = new javax.swing.JPanel();
        pembayaranTitle = new javax.swing.JLabel();
        pembayaranSubTitle = new javax.swing.JLabel();
        invoiceLabel = new javax.swing.JLabel();
        invoiceField = new javax.swing.JLabel();
        tanggalLabel = new javax.swing.JLabel();
        tanggalField = new javax.swing.JLabel();
        contentPanel = new javax.swing.JPanel();
        pembayaranScrollPane = new javax.swing.JScrollPane();
        pembayaranTable = new javax.swing.JTable();
        jenisBayarLabel = new javax.swing.JLabel();
        jenisBayarField = new javax.swing.JComboBox();
        jumlahLabel = new javax.swing.JLabel();
        jumlahField = new javax.swing.JTextField();
        tambahButton = new javax.swing.JButton();
        persenCheckBox = new javax.swing.JCheckBox();
        footerPanel = new javax.swing.JPanel();
        hapusButton = new javax.swing.JButton();
        hitungOtomatisButton = new javax.swing.JButton();
        tutupButton = new javax.swing.JButton();
        pembayaranPopup.setName("pembayaranPopup");
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.indragunawan.restobiz.app.MainApp.class).getContext().getActionMap(PembayaranView.class, this);
        hapusMenuItem.setAction(actionMap.get("deletePembayaran"));
        hapusMenuItem.setName("hapusMenuItem");
        pembayaranPopup.add(hapusMenuItem);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.indragunawan.restobiz.app.MainApp.class).getContext().getResourceMap(PembayaranView.class);
        setTitle(resourceMap.getString("Form.title"));
        setName("pembayaran");
        setResizable(false);
        headerPanel.setBackground(resourceMap.getColor("headerPanel.background"));
        headerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        headerPanel.setName("headerPanel");
        pembayaranTitle.setFont(resourceMap.getFont("pembayaranTitle.font"));
        pembayaranTitle.setText(resourceMap.getString("pembayaranTitle.text"));
        pembayaranTitle.setName("pembayaranTitle");
        pembayaranSubTitle.setText(resourceMap.getString("pembayaranSubTitle.text"));
        pembayaranSubTitle.setName("pembayaranSubTitle");
        invoiceLabel.setFont(resourceMap.getFont("invoiceLabel.font"));
        invoiceLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        invoiceLabel.setText(resourceMap.getString("invoiceLabel.text"));
        invoiceLabel.setName("invoiceLabel");
        invoiceField.setText(resourceMap.getString("invoiceField.text"));
        invoiceField.setName("invoiceField");
        tanggalLabel.setFont(resourceMap.getFont("tanggalLabel.font"));
        tanggalLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        tanggalLabel.setText(resourceMap.getString("tanggalLabel.text"));
        tanggalLabel.setName("tanggalLabel");
        tanggalField.setText(resourceMap.getString("tanggalField.text"));
        tanggalField.setName("tanggalField");
        javax.swing.GroupLayout headerPanelLayout = new javax.swing.GroupLayout(headerPanel);
        headerPanel.setLayout(headerPanelLayout);
        headerPanelLayout.setHorizontalGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(headerPanelLayout.createSequentialGroup().addContainerGap().addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pembayaranTitle).addGroup(headerPanelLayout.createSequentialGroup().addGap(12, 12, 12).addComponent(pembayaranSubTitle))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 255, Short.MAX_VALUE).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tanggalLabel, javax.swing.GroupLayout.Alignment.TRAILING).addComponent(invoiceLabel, javax.swing.GroupLayout.Alignment.TRAILING)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(invoiceField).addComponent(tanggalField)).addContainerGap()));
        headerPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { invoiceField, tanggalField });
        headerPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { invoiceLabel, tanggalLabel });
        headerPanelLayout.setVerticalGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(headerPanelLayout.createSequentialGroup().addContainerGap().addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(headerPanelLayout.createSequentialGroup().addComponent(pembayaranTitle).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pembayaranSubTitle)).addGroup(headerPanelLayout.createSequentialGroup().addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(invoiceLabel).addComponent(invoiceField)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(tanggalLabel).addComponent(tanggalField)))).addContainerGap(19, Short.MAX_VALUE)));
        contentPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        contentPanel.setName("contentPanel");
        pembayaranScrollPane.setName("pembayaranScrollPane");
        pembayaranTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "No. Bukti", "Tgl. Bayar", "Jenis", "Jumlah" }) {

            Class[] types = new Class[] { java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.Float.class };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        pembayaranTable.setName("pembayaranTable");
        pembayaranTable.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pembayaranTableMouseClicked(evt);
            }
        });
        pembayaranScrollPane.setViewportView(pembayaranTable);
        pembayaranTable.getColumnModel().getColumn(0).setMaxWidth(50);
        pembayaranTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("pembayaranTable.columnModel.title0"));
        pembayaranTable.getColumnModel().getColumn(1).setMaxWidth(100);
        pembayaranTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("pembayaranTable.columnModel.title1"));
        pembayaranTable.getColumnModel().getColumn(2).setHeaderValue(resourceMap.getString("pembayaranTable.columnModel.title2"));
        pembayaranTable.getColumnModel().getColumn(3).setMaxWidth(100);
        pembayaranTable.getColumnModel().getColumn(3).setHeaderValue(resourceMap.getString("pembayaranTable.columnModel.title3"));
        jenisBayarLabel.setDisplayedMnemonic('J');
        jenisBayarLabel.setLabelFor(jenisBayarField);
        jenisBayarLabel.setText(resourceMap.getString("jenisBayarLabel.text"));
        jenisBayarLabel.setName("jenisBayarLabel");
        jenisBayarField.setBackground(resourceMap.getColor("jenisBayarField.background"));
        jenisBayarField.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TUNAI" }));
        jenisBayarField.setName("jenisBayarField");
        jumlahLabel.setDisplayedMnemonic('u');
        jumlahLabel.setLabelFor(jumlahField);
        jumlahLabel.setText(resourceMap.getString("jumlahLabel.text"));
        jumlahLabel.setName("jumlahLabel");
        jumlahField.setText(resourceMap.getString("jumlahField.text"));
        jumlahField.setName("jumlahField");
        tambahButton.setAction(actionMap.get("addPembayaran"));
        tambahButton.setMnemonic('a');
        tambahButton.setText(resourceMap.getString("tambahButton.text"));
        tambahButton.setName("tambahButton");
        tambahButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                tambahButtonKeyPressed(evt);
            }
        });
        persenCheckBox.setText(resourceMap.getString("persenCheckBox.text"));
        persenCheckBox.setName("persenCheckBox");
        javax.swing.GroupLayout contentPanelLayout = new javax.swing.GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(contentPanelLayout.createSequentialGroup().addContainerGap().addGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pembayaranScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 489, Short.MAX_VALUE).addGroup(contentPanelLayout.createSequentialGroup().addComponent(jenisBayarLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jenisBayarField, 0, 187, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jumlahLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jumlahField, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(persenCheckBox).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tambahButton))).addContainerGap()));
        contentPanelLayout.setVerticalGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(contentPanelLayout.createSequentialGroup().addContainerGap().addGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jenisBayarLabel).addComponent(tambahButton).addComponent(jumlahLabel).addComponent(jumlahField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jenisBayarField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(persenCheckBox)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(pembayaranScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        footerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        footerPanel.setName("footerPanel");
        hapusButton.setAction(actionMap.get("deletePembayaran"));
        hapusButton.setMnemonic('H');
        hapusButton.setText(resourceMap.getString("hapusButton.text"));
        hapusButton.setName("hapusButton");
        hapusButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                hapusButtonKeyPressed(evt);
            }
        });
        hitungOtomatisButton.setAction(actionMap.get("hitungOtomatis"));
        hitungOtomatisButton.setMnemonic('O');
        hitungOtomatisButton.setText(resourceMap.getString("hitungOtomatisButton.text"));
        hitungOtomatisButton.setName("hitungOtomatisButton");
        hitungOtomatisButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                hitungOtomatisButtonKeyPressed(evt);
            }
        });
        tutupButton.setAction(actionMap.get("tutupPembayaran"));
        tutupButton.setMnemonic('T');
        tutupButton.setText(resourceMap.getString("tutupButton.text"));
        tutupButton.setName("tutupButton");
        tutupButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                tutupButtonKeyPressed(evt);
            }
        });
        javax.swing.GroupLayout footerPanelLayout = new javax.swing.GroupLayout(footerPanel);
        footerPanel.setLayout(footerPanelLayout);
        footerPanelLayout.setHorizontalGroup(footerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(footerPanelLayout.createSequentialGroup().addContainerGap().addComponent(hapusButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 293, Short.MAX_VALUE).addComponent(hitungOtomatisButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tutupButton).addContainerGap()));
        footerPanelLayout.setVerticalGroup(footerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(footerPanelLayout.createSequentialGroup().addContainerGap().addGroup(footerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(hapusButton).addComponent(tutupButton).addComponent(hitungOtomatisButton)).addContainerGap(16, Short.MAX_VALUE)));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(headerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(footerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(contentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(headerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(contentPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(footerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
        pack();
    }

    @SuppressWarnings("empty-statement")
    private void pembayaranTableMouseClicked(java.awt.event.MouseEvent evt) {
        if (pembayaranTable.getSelectedRowCount() == 1) {
            if ((evt.getButton() == MouseEvent.BUTTON3) && (evt.getClickCount() == 1)) {
                pembayaranPopup.show(pembayaranTable, evt.getX(), evt.getY());
            }
            ;
        }
        ;
    }

    private void tambahButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            addPembayaran();
        }
    }

    private void hapusButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            deletePembayaran();
        }
    }

    private void hitungOtomatisButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            hitungOtomatis();
        }
    }

    private void tutupButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            tutupPembayaran();
        }
    }

    @Action
    public void addPembayaran() {
        if (!jumlahField.getText().isEmpty() && jenisBayarField.getSelectedIndex() != -1) {
            Double Jumlah = Double.valueOf(jumlahField.getText());
            String Jenis = jenisBayarField.getSelectedItem().toString();
            execTambahPembayaran(Jenis, Jumlah);
        } else {
            JOptionPane.showMessageDialog(this, "Harap isi data pembayaran yang diperlukan");
            jenisBayarField.requestFocus();
        }
    }

    @Action
    public void deletePembayaran() {
        if (pembayaranTable.getSelectedRowCount() == 1) {
            try {
                Integer NoBukti;
                Date Dibayar;
                NoBukti = Integer.valueOf(String.valueOf(pembayaranTable.getValueAt(pembayaranTable.getSelectedRow(), 0)));
                Dibayar = (Date) pembayaranTable.getValueAt(pembayaranTable.getSelectedRow(), 1);
                execHapusPembayaran(NoBukti, Dibayar);
                refreshTabelPembayaran(Integer.valueOf(invoiceField.getText()), dateDisplay.parse(tanggalField.getText()));
                resetInputPembayaran();
            } catch (ParseException ex) {
                ex.printStackTrace();
                Logger.getLogger(PembayaranView.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Pilih pembayaran yang akan dihapus");
        }
    }

    @Action
    public void tutupPembayaran() {
        this.dispose();
    }

    @Action
    public void hitungOtomatis() {
        try {
            Integer Nomor = Integer.valueOf(invoiceField.getText());
            Date Tanggal = dateDisplay.parse(tanggalField.getText());
            Double totalTransaksi = cfg.getTotalTransaksi(Nomor, Tanggal);
            Double totalBayar = cfg.getTotalBayar(Nomor, Tanggal);
            if (totalBayar == 0) {
                execTambahPembayaran("TUNAI", totalTransaksi);
            } else if (totalBayar.equals(totalTransaksi)) {
                JOptionPane.showMessageDialog(this, "Transaksi dan pembayaran sudah sesuai");
            } else if (totalBayar < totalTransaksi) {
                execTambahPembayaran("TUNAI", totalTransaksi - totalBayar);
            } else if (totalBayar > totalTransaksi) {
                execTambahPembayaran("KEMBALI", totalBayar - totalTransaksi);
            } else {
                JOptionPane.showMessageDialog(this, "Metode perhitungan otomatis tidak dapat dilakukan");
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
            Logger.getLogger(PembayaranView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private javax.swing.JPanel contentPanel;

    private javax.swing.JPanel footerPanel;

    private javax.swing.JButton hapusButton;

    private javax.swing.JMenuItem hapusMenuItem;

    private javax.swing.JPanel headerPanel;

    private javax.swing.JButton hitungOtomatisButton;

    private javax.swing.JLabel invoiceField;

    private javax.swing.JLabel invoiceLabel;

    private javax.swing.JComboBox jenisBayarField;

    private javax.swing.JLabel jenisBayarLabel;

    private javax.swing.JTextField jumlahField;

    private javax.swing.JLabel jumlahLabel;

    private javax.swing.JPopupMenu pembayaranPopup;

    private javax.swing.JScrollPane pembayaranScrollPane;

    private javax.swing.JLabel pembayaranSubTitle;

    private javax.swing.JTable pembayaranTable;

    private javax.swing.JLabel pembayaranTitle;

    private javax.swing.JCheckBox persenCheckBox;

    private javax.swing.JButton tambahButton;

    private javax.swing.JLabel tanggalField;

    private javax.swing.JLabel tanggalLabel;

    private javax.swing.JButton tutupButton;
}
