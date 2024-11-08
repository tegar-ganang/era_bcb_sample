package com.indragunawan.restobiz.app;

import com.indragunawan.restobiz.app.model.TransaksiViewTM;
import com.indragunawan.restobiz.app.model.TransaksiViewRD;
import com.jidesoft.swing.AutoCompletion;
import com.jidesoft.swing.ComboBoxSearchable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.jdesktop.application.Action;

/**
 *
 * @author  igoens
 */
public class TransaksiView extends javax.swing.JDialog {

    private static final long serialVersionUID = 5058900804090281755L;

    private TransaksiCustomerView transaksiCustomerView;

    private TransaksiOpenView transaksiOpenView;

    private PembayaranView pembayaranView;

    private EntityManager transaksiEntity;

    private String transaksiCustomer;

    private List menuList;

    private int i;

    private String Customer;

    private Query transaksiQuery;

    private GeneralConfig cfg = new GeneralConfig();

    private SimpleDateFormat dateDisplay = new SimpleDateFormat("dd/MM/yyyy");

    private SimpleDateFormat dateSQL = new SimpleDateFormat("yyyy-MM-dd");

    private NumberFormat invoiceDisplay = new DecimalFormat("0000");

    private NumberFormat floatDisplay = new DecimalFormat("#,##0.00");

    /** Creates new form TransaksiView */
    public TransaksiView(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        formatTransaksiTable();
        transaksiEntity = Persistence.createEntityManagerFactory("RestobizPU", cfg.getPersistanceDbProperties()).createEntityManager();
        lockTransaksiForm();
        setShiftValue(0);
        resetInputForm();
        loggedUserField.setText("Kasir: " + cfg.getLoggedUserName());
    }

    private void formatTransaksiTable() {
        for (i = 0; i < transaksiTable.getColumnCount(); i++) {
            transaksiTable.getColumnModel().getColumn(i).setCellRenderer(new TransaksiViewRD());
        }
    }

    private void hapusTabelItem() {
        Date tanggal;
        Integer nomor;
        Integer menu;
        if (transaksiTable.getValueAt(transaksiTable.getSelectedRow(), 0) != "") {
            try {
                tanggal = dateDisplay.parse(tanggalField.getText());
                nomor = Integer.valueOf(invoiceField.getText());
                menu = Integer.valueOf(transaksiTable.getValueAt(transaksiTable.getSelectedRow(), 0).toString());
                hapusItem(nomor, tanggal, menu);
                openTransaksiForm(nomor, tanggal);
                formatTransaksiTable();
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Boolean hapusItem(Integer Nomor, Date Tanggal, Integer Menu) {
        Boolean r = false;
        try {
            transaksiEntity.getTransaction().begin();
            transaksiQuery = transaksiEntity.createNativeQuery("DELETE FROM detiltransaksi WHERE nomor = #nomor AND tanggal = #tanggal AND menu = #menu").setParameter("nomor", invoiceDisplay.format(Nomor)).setParameter("tanggal", dateSQL.format(Tanggal)).setParameter("menu", invoiceDisplay.format(Menu));
            transaksiQuery.executeUpdate();
            transaksiEntity.getTransaction().commit();
        } catch (Exception ex) {
            transaksiEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    private Boolean execPostingInvoice(Integer Nomor, Date Tanggal) {
        Boolean r = false;
        try {
            transaksiEntity.getTransaction().begin();
            transaksiQuery = transaksiEntity.createNativeQuery("UPDATE transaksi SET posted = true WHERE nomor = #nomor AND tanggal = #tanggal").setParameter("nomor", invoiceDisplay.format(Nomor)).setParameter("tanggal", dateSQL.format(Tanggal));
            transaksiQuery.executeUpdate();
            transaksiEntity.getTransaction().commit();
            openTransaksiForm(0, Tanggal);
            lockTransaksiForm();
        } catch (Exception ex) {
            transaksiEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    private void gantiBeratItem() {
        if (!transaksiTable.getValueAt(transaksiTable.getSelectedRow(), 0).toString().isEmpty()) {
            try {
                Double beratlama = null;
                Double beratbaru = null;
                Integer nomor;
                Integer menu;
                Date tanggal;
                String input;
                tanggal = dateDisplay.parse(tanggalField.getText());
                nomor = Integer.valueOf(invoiceField.getText());
                menu = Integer.valueOf(transaksiTable.getValueAt(transaksiTable.getSelectedRow(), 0).toString());
                beratlama = cfg.getBeratItem(tanggal, nomor, menu);
                input = JOptionPane.showInputDialog(null, "Total berat item", floatDisplay.format(beratlama));
                if ((input != null) && !input.isEmpty()) {
                    beratbaru = Double.valueOf(input);
                }
                if ((beratlama != beratbaru) && (beratbaru != null)) {
                    updateBerat(nomor, tanggal, menu, beratbaru);
                    openTransaksiForm(nomor, tanggal);
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
                Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Harap masukan berat item!");
            }
        }
    }

    private void gantiJumlahItem() {
        if (!transaksiTable.getValueAt(transaksiTable.getSelectedRow(), 0).toString().isEmpty()) {
            try {
                Double jumlahlama = null;
                Double jumlahbaru = null;
                Integer nomor;
                Integer menu;
                Date tanggal;
                String input;
                tanggal = dateDisplay.parse(tanggalField.getText());
                nomor = Integer.valueOf(invoiceField.getText());
                menu = Integer.valueOf(transaksiTable.getValueAt(transaksiTable.getSelectedRow(), 0).toString());
                jumlahlama = cfg.getJumlahItem(tanggal, nomor, menu);
                input = JOptionPane.showInputDialog(null, "Jumlah pesanan", floatDisplay.format(jumlahlama));
                if ((input != null) && !input.isEmpty()) {
                    jumlahbaru = Double.valueOf(input);
                }
                if ((jumlahlama != jumlahbaru) && (jumlahbaru != null)) {
                    updateJumlah(nomor, tanggal, menu, jumlahbaru);
                    openTransaksiForm(nomor, tanggal);
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
                Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Harap masukan jumlah pesanan!");
            }
        }
    }

    private void lockTransaksiForm() {
        resetTransaksiForm();
        namaField.setEnabled(false);
        menuField.setEnabled(false);
        jumlahField.setEnabled(false);
        beratField.setEnabled(false);
        hargaField.setEnabled(false);
        discountField.setEnabled(false);
        cetakInvoiceButton.setEnabled(false);
        cetakChecklistButton.setEnabled(false);
        postingInvoiceButton.setEnabled(false);
        pembayaranButton.setEnabled(false);
        promoCheckBox.setSelected(false);
        promoCheckBox.setEnabled(false);
    }

    private void resetInputForm() {
        menuField.setText("...");
        hargaField.setText(floatDisplay.format(0));
        jumlahField.setText(String.valueOf(Integer.valueOf(1)));
        beratField.setText(floatDisplay.format(0));
        discountField.setText(floatDisplay.format(0));
        namaField.setSelectedIndex(-1);
        tambahButton.setEnabled(false);
        namaField.requestFocus();
        promoCheckBox.setSelected(false);
    }

    private void setShiftValue(int shift) {
        if (shift == 0) {
            shiftField.setText(String.valueOf(cfg.getCurrentShift()));
        } else {
            shiftField.setText(String.valueOf(shift));
        }
    }

    private void setTransaksiSummary() {
        int Nomor = 0;
        Date Tanggal = null;
        Nomor = Integer.valueOf(invoiceField.getText());
        try {
            Tanggal = dateDisplay.parse(tanggalField.getText());
        } catch (ParseException ex) {
            ex.printStackTrace();
            Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
        }
        subTotalField.setText(floatDisplay.format(cfg.getSubTotal(Nomor, Tanggal)));
        totalDiscountField.setText(floatDisplay.format(cfg.getTotalDiscount(Nomor, Tanggal)));
        pajakField.setText(floatDisplay.format(cfg.getTotalPajak(Nomor, Tanggal)));
        totalTransaksiField.setText(floatDisplay.format(cfg.getTotalTransaksi(Nomor, Tanggal)));
        totalBayarField.setText(floatDisplay.format(cfg.getTotalBayar(Nomor, Tanggal)));
        sisaField.setText(floatDisplay.format(cfg.getTotalSisa(Nomor, Tanggal)));
        if (cfg.getTotalTransaksi(Nomor, Tanggal).equals(cfg.getTotalBayar(Nomor, Tanggal)) && (!cfg.getTotalBayar(Nomor, Tanggal).equals(0.0))) {
            postingInvoiceButton.setEnabled(true);
        } else {
            postingInvoiceButton.setEnabled(false);
        }
    }

    public void newTransaksi(int Nomor, Date Tanggal, int Shift, String Customer, String NamaUser) {
        try {
            transaksiEntity.getTransaction().begin();
            transaksiQuery = transaksiEntity.createNativeQuery("INSERT INTO transaksi VALUES (#nomor, #tanggal, #customer, #namauser, #shift, #posted)").setParameter("nomor", Nomor).setParameter("tanggal", dateSQL.format(Tanggal)).setParameter("customer", Customer).setParameter("namauser", NamaUser).setParameter("shift", Shift).setParameter("posted", false);
            transaksiQuery.executeUpdate();
            transaksiEntity.getTransaction().commit();
        } catch (Exception ex) {
            transaksiEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateAllDiscount(int Nomor, Date Tanggal, Double Discount) {
        try {
            Discount = Discount / 100;
            transaksiEntity.getTransaction().begin();
            transaksiQuery = transaksiEntity.createNativeQuery("UPDATE detiltransaksi dt SET dt.diskon = (dt.harga * #diskon), dt.pajak = (dt.harga - (dt.harga * #diskon)) * #pajak WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal").setParameter("diskon", Discount).setParameter("nomor", Nomor).setParameter("tanggal", Tanggal).setParameter("pajak", cfg.getVatValue() / 100);
            transaksiQuery.executeUpdate();
            transaksiQuery = transaksiEntity.createNativeQuery("UPDATE detiltransaksi dt SET dt.pajak = 0 WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal AND dt.menu IN (SELECT m.menu FROM menu m WHERE m.pajak = 0)").setParameter("nomor", Nomor).setParameter("tanggal", Tanggal);
            transaksiQuery.executeUpdate();
            transaksiEntity.getTransaction().commit();
        } catch (Exception ex) {
            transaksiEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void newDetilTransaksi(int Nomor, Date Tanggal, int Menu, Double Jumlah, Double Harga, Double Diskon, Double Pajak, String NamaUser, Double Berat) {
        try {
            transaksiEntity.getTransaction().begin();
            transaksiQuery = transaksiEntity.createNativeQuery("SELECT dt.* FROM detiltransaksi dt WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal AND dt.menu = #menu").setParameter("nomor", Nomor).setParameter("tanggal", Tanggal).setParameter("menu", Menu);
            if (transaksiQuery.getResultList().isEmpty()) {
                transaksiQuery = transaksiEntity.createNativeQuery("INSERT INTO detiltransaksi VALUES (#nomor, #tanggal, #menu, #jumlah, #harga, #diskon, #pajak, #namauser, #berat)").setParameter("nomor", Nomor).setParameter("tanggal", Tanggal).setParameter("menu", Menu).setParameter("jumlah", Jumlah).setParameter("harga", Harga).setParameter("diskon", Diskon).setParameter("pajak", Pajak).setParameter("namauser", NamaUser).setParameter("berat", Berat);
                transaksiQuery.executeUpdate();
            } else {
                transaksiQuery = transaksiEntity.createNativeQuery("UPDATE detiltransaksi dt SET dt.jumlah = (dt.jumlah + #jumlah), dt.berat = (dt.berat + #berat) WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal AND dt.menu = #menu").setParameter("nomor", invoiceDisplay.format(Nomor)).setParameter("tanggal", dateSQL.format(Tanggal)).setParameter("menu", invoiceDisplay.format(Menu)).setParameter("jumlah", Jumlah).setParameter("berat", Berat);
                transaksiQuery.executeUpdate();
            }
            transaksiEntity.getTransaction().commit();
        } catch (Exception ex) {
            transaksiEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateJumlah(int Nomor, Date Tanggal, int Menu, Double Jumlah) {
        try {
            transaksiEntity.getTransaction().begin();
            transaksiQuery = transaksiEntity.createNativeQuery("UPDATE detiltransaksi dt SET dt.jumlah = #jumlah WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal AND dt.menu = #menu").setParameter("menu", Menu).setParameter("jumlah", Jumlah).setParameter("nomor", Nomor).setParameter("tanggal", dateSQL.format(Tanggal));
            transaksiQuery.executeUpdate();
            transaksiEntity.getTransaction().commit();
        } catch (Exception ex) {
            transaksiEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateBerat(int Nomor, Date Tanggal, int Menu, Double Berat) {
        try {
            transaksiEntity.getTransaction().begin();
            transaksiQuery = transaksiEntity.createNativeQuery("UPDATE detiltransaksi dt SET dt.berat = #berat WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal AND dt.menu = #menu").setParameter("menu", Menu).setParameter("berat", Berat).setParameter("nomor", Nomor).setParameter("tanggal", dateSQL.format(Tanggal));
            transaksiQuery.executeUpdate();
            transaksiEntity.getTransaction().commit();
        } catch (Exception ex) {
            transaksiEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateCustomer(String Customer, Integer Nomor, Date Tanggal) {
        try {
            transaksiEntity.getTransaction().begin();
            transaksiQuery = transaksiEntity.createNativeQuery("UPDATE transaksi SET customer = #customer WHERE nomor = #nomor AND tanggal = #tanggal").setParameter("customer", Customer).setParameter("nomor", Nomor).setParameter("tanggal", dateSQL.format(Tanggal));
            transaksiQuery.executeUpdate();
            transaksiEntity.getTransaction().commit();
        } catch (Exception ex) {
            transaksiEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void unlockTransaksiForm() {
        namaField.setEnabled(true);
        menuField.setEnabled(true);
        jumlahField.setEnabled(true);
        beratField.setEnabled(true);
        hargaField.setEnabled(true);
        discountField.setEnabled(true);
        cetakInvoiceButton.setEnabled(true);
        cetakChecklistButton.setEnabled(true);
        pembayaranButton.setEnabled(true);
        promoCheckBox.setEnabled(true);
        promoCheckBox.setSelected(false);
        fillMenuField();
        resetInputForm();
    }

    public String getTransaksiCustomer() {
        return this.transaksiCustomer;
    }

    public void setTransaksiCustomer(String Customer) {
        this.transaksiCustomer = Customer;
    }

    private void fillMenuField() {
        try {
            menuList = cfg.getMenuNameList();
            namaField.removeAllItems();
            for (i = 0; i <= menuList.size() - 1; i++) {
                namaField.addItem(menuList.get(i));
            }
            AutoCompletion ac = new AutoCompletion(namaField, new ComboBoxSearchable(this.namaField) {

                @Override
                protected String convertElementToString(Object object) {
                    return String.valueOf(object);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void fillMenuData() {
        try {
            Integer menu;
            Double Discount;
            Double Harga;
            Double itemDiscount;
            menu = Integer.valueOf(String.valueOf(cfg.getMenuByNama(namaField.getEditor().getItem().toString())));
            menuField.setText(invoiceDisplay.format(menu));
            Harga = Double.valueOf(String.valueOf(cfg.getHargaByNama(String.valueOf(namaField.getSelectedItem()))));
            hargaField.setText(floatDisplay.format(Harga));
            Discount = Double.valueOf(cfg.getGlobalDiscount());
            itemDiscount = Harga * Discount / 100;
            discountField.setText(floatDisplay.format(itemDiscount));
            tambahButton.setEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void openTransaksiForm(int Nomor, Date Tanggal) {
        unlockTransaksiForm();
        transaksiTable.setModel(new TransaksiViewTM(openDetilTransaksi(Nomor, Tanggal)));
        invoiceField.setText(invoiceDisplay.format(Nomor));
        customerField.setText(Customer);
        tanggalField.setText(dateDisplay.format(Tanggal));
        formatTransaksiTable();
        setTransaksiSummary();
    }

    private void resetTransaksiForm() {
        Calendar cal = Calendar.getInstance();
        tanggalField.setText(dateDisplay.format(cal.getTime()));
        invoiceField.setText("...");
        customerField.setText("...");
        promoCheckBox.setSelected(false);
        resetInputForm();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();
        transaksiTableMenu = new javax.swing.JPopupMenu();
        editJumlahMenuItem = new javax.swing.JMenuItem();
        editBeratMenuItem = new javax.swing.JMenuItem();
        editBeratSeparator = new javax.swing.JSeparator();
        deleteItemMenuItem = new javax.swing.JMenuItem();
        headerPanel = new javax.swing.JPanel();
        headerLabel = new javax.swing.JLabel();
        invoiceLabel = new javax.swing.JLabel();
        customerLabel = new javax.swing.JLabel();
        invoiceField = new javax.swing.JLabel();
        customerField = new javax.swing.JLabel();
        tanggalLabel = new javax.swing.JLabel();
        tanggalField = new javax.swing.JLabel();
        shiftLabel = new javax.swing.JLabel();
        shiftField = new javax.swing.JLabel();
        gantiShiftButton = new javax.swing.JButton();
        loggedUserField = new javax.swing.JLabel();
        contentPanel = new javax.swing.JPanel();
        transaksiScrollPane = new javax.swing.JScrollPane();
        transaksiTable = new javax.swing.JTable();
        inputMenuPanel = new javax.swing.JPanel();
        menuLabel = new javax.swing.JLabel();
        menuField = new javax.swing.JTextField();
        jumlahLabel = new javax.swing.JLabel();
        jumlahField = new javax.swing.JTextField();
        namaLabel = new javax.swing.JLabel();
        hargaLabel = new javax.swing.JLabel();
        namaField = new javax.swing.JComboBox();
        hargaField = new javax.swing.JTextField();
        discountLabel = new javax.swing.JLabel();
        discountField = new javax.swing.JTextField();
        tambahButton = new javax.swing.JButton();
        beratLabel = new javax.swing.JLabel();
        beratField = new javax.swing.JTextField();
        commandPanel = new javax.swing.JPanel();
        transaksiBaruButton = new javax.swing.JButton();
        bukaTransaksiButton = new javax.swing.JButton();
        pembayaranButton = new javax.swing.JButton();
        cetakChecklistButton = new javax.swing.JButton();
        summaryPanel = new javax.swing.JPanel();
        subTotalLabel = new javax.swing.JLabel();
        subTotalField = new javax.swing.JTextField();
        totalDiscountField = new javax.swing.JTextField();
        totalDiscountLabel = new javax.swing.JLabel();
        pajakLabel = new javax.swing.JLabel();
        pajakField = new javax.swing.JTextField();
        totalTransaksiLabel = new javax.swing.JLabel();
        totalTransaksiField = new javax.swing.JTextField();
        totalBayarLabel = new javax.swing.JLabel();
        totalBayarField = new javax.swing.JTextField();
        sisaLabel = new javax.swing.JLabel();
        sisaField = new javax.swing.JTextField();
        footerPanel = new javax.swing.JPanel();
        cetakInvoiceButton = new javax.swing.JButton();
        postingInvoiceButton = new javax.swing.JButton();
        promoCheckBox = new javax.swing.JCheckBox();
        promoDiscountTextField = new javax.swing.JTextField();
        promoDiscountButton = new javax.swing.JButton();
        transaksiTableMenu.setName("transaksiTableMenu");
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.indragunawan.restobiz.app.MainApp.class).getContext().getResourceMap(TransaksiView.class);
        editJumlahMenuItem.setText(resourceMap.getString("editJumlahMenuItem.text"));
        editJumlahMenuItem.setName("editJumlahMenuItem");
        editJumlahMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editJumlahMenuItemActionPerformed(evt);
            }
        });
        transaksiTableMenu.add(editJumlahMenuItem);
        editBeratMenuItem.setText(resourceMap.getString("editBeratMenuItem.text"));
        editBeratMenuItem.setName("editBeratMenuItem");
        editBeratMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editBeratMenuItemActionPerformed(evt);
            }
        });
        transaksiTableMenu.add(editBeratMenuItem);
        editBeratSeparator.setName("editBeratSeparator");
        transaksiTableMenu.add(editBeratSeparator);
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.indragunawan.restobiz.app.MainApp.class).getContext().getActionMap(TransaksiView.class, this);
        deleteItemMenuItem.setAction(actionMap.get("hapusMenuTransaksi"));
        deleteItemMenuItem.setText(resourceMap.getString("deleteItemMenuItem.text"));
        deleteItemMenuItem.setName("deleteItemMenuItem");
        transaksiTableMenu.add(deleteItemMenuItem);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("inputTransaksi.title"));
        setName("inputTransaksi");
        headerPanel.setBackground(resourceMap.getColor("headerPanel.background"));
        headerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        headerPanel.setName("headerPanel");
        headerLabel.setFont(resourceMap.getFont("headerLabel.font"));
        headerLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        headerLabel.setText(resourceMap.getString("headerLabel.text"));
        headerLabel.setName("headerLabel");
        invoiceLabel.setFont(resourceMap.getFont("invoiceLabel.font"));
        invoiceLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        invoiceLabel.setText(resourceMap.getString("invoiceLabel.text"));
        invoiceLabel.setName("invoiceLabel");
        customerLabel.setFont(resourceMap.getFont("customerLabel.font"));
        customerLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        customerLabel.setText(resourceMap.getString("customerLabel.text"));
        customerLabel.setName("customerLabel");
        invoiceField.setText(resourceMap.getString("invoiceField.text"));
        invoiceField.setName("invoiceField");
        customerField.setText(resourceMap.getString("customerField.text"));
        customerField.setName("customerField");
        customerField.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                customerFieldMouseClicked(evt);
            }
        });
        tanggalLabel.setFont(resourceMap.getFont("tanggalLabel.font"));
        tanggalLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        tanggalLabel.setText(resourceMap.getString("tanggalLabel.text"));
        tanggalLabel.setName("tanggalLabel");
        tanggalField.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        tanggalField.setText(resourceMap.getString("tanggalField.text"));
        tanggalField.setName("tanggalField");
        shiftLabel.setFont(resourceMap.getFont("shiftLabel.font"));
        shiftLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        shiftLabel.setText(resourceMap.getString("shiftLabel.text"));
        shiftLabel.setName("shiftLabel");
        shiftField.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        shiftField.setText(resourceMap.getString("shiftField.text"));
        shiftField.setName("shiftField");
        gantiShiftButton.setAction(actionMap.get("changeShift"));
        gantiShiftButton.setMnemonic('S');
        gantiShiftButton.setText(resourceMap.getString("gantiShiftButton.text"));
        gantiShiftButton.setName("gantiShiftButton");
        loggedUserField.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        loggedUserField.setText(resourceMap.getString("loggedUserField.text"));
        loggedUserField.setName("loggedUserField");
        javax.swing.GroupLayout headerPanelLayout = new javax.swing.GroupLayout(headerPanel);
        headerPanel.setLayout(headerPanelLayout);
        headerPanelLayout.setHorizontalGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(headerPanelLayout.createSequentialGroup().addContainerGap().addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(customerLabel).addComponent(invoiceLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(invoiceField, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(customerField, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(loggedUserField, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE).addComponent(headerLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(tanggalLabel).addComponent(shiftLabel)).addGap(6, 6, 6).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addGroup(headerPanelLayout.createSequentialGroup().addComponent(shiftField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGap(3, 3, 3).addComponent(gantiShiftButton, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(tanggalField, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        headerPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { customerLabel, invoiceLabel, shiftLabel, tanggalLabel });
        headerPanelLayout.setVerticalGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(headerPanelLayout.createSequentialGroup().addContainerGap().addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(invoiceField).addComponent(invoiceLabel).addComponent(headerLabel)).addGroup(headerPanelLayout.createSequentialGroup().addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(tanggalLabel).addComponent(tanggalField)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(gantiShiftButton, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(shiftField).addComponent(shiftLabel))).addGroup(headerPanelLayout.createSequentialGroup().addGap(26, 26, 26).addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(customerLabel).addComponent(customerField).addComponent(loggedUserField)))).addContainerGap(20, Short.MAX_VALUE)));
        contentPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        contentPanel.setName("contentPanel");
        transaksiScrollPane.setName("transaksiScrollPane");
        transaksiTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Menu", "Nama", "Jumlah", "Harga" }) {

            Class[] types = new Class[] { java.lang.Short.class, java.lang.String.class, java.lang.Float.class, java.lang.Float.class };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        transaksiTable.setColumnSelectionAllowed(true);
        transaksiTable.setName("transaksiTable");
        transaksiTable.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                transaksiTableMouseClicked(evt);
            }
        });
        transaksiScrollPane.setViewportView(transaksiTable);
        transaksiTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        transaksiTable.getColumnModel().getColumn(0).setMaxWidth(50);
        transaksiTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("transaksiTable.columnModel.title0"));
        transaksiTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("transaksiTable.columnModel.title1"));
        transaksiTable.getColumnModel().getColumn(2).setMaxWidth(50);
        transaksiTable.getColumnModel().getColumn(2).setHeaderValue(resourceMap.getString("transaksiTable.columnModel.title2"));
        transaksiTable.getColumnModel().getColumn(3).setMaxWidth(100);
        transaksiTable.getColumnModel().getColumn(3).setHeaderValue(resourceMap.getString("transaksiTable.columnModel.title3"));
        inputMenuPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        inputMenuPanel.setName("inputMenuPanel");
        menuLabel.setDisplayedMnemonic('K');
        menuLabel.setLabelFor(menuField);
        menuLabel.setText(resourceMap.getString("menuLabel.text"));
        menuLabel.setName("menuLabel");
        menuField.setEditable(false);
        menuField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        menuField.setText(resourceMap.getString("menuField.text"));
        menuField.setFocusable(false);
        menuField.setName("menuField");
        jumlahLabel.setDisplayedMnemonic('J');
        jumlahLabel.setLabelFor(jumlahField);
        jumlahLabel.setText(resourceMap.getString("jumlahLabel.text"));
        jumlahLabel.setName("jumlahLabel");
        jumlahField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jumlahField.setText(resourceMap.getString("jumlahField.text"));
        jumlahField.setName("jumlahField");
        jumlahField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                jumlahFieldKeyPressed(evt);
            }
        });
        namaLabel.setDisplayedMnemonic('M');
        namaLabel.setLabelFor(namaField);
        namaLabel.setText(resourceMap.getString("namaLabel.text"));
        namaLabel.setName("namaLabel");
        hargaLabel.setDisplayedMnemonic('H');
        hargaLabel.setLabelFor(hargaField);
        hargaLabel.setText(resourceMap.getString("hargaLabel.text"));
        hargaLabel.setName("hargaLabel");
        namaField.setEditable(true);
        namaField.setName("namaField");
        namaField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                namaFieldActionPerformed(evt);
            }
        });
        namaField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                namaFieldKeyPressed(evt);
            }
        });
        hargaField.setEditable(false);
        hargaField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        hargaField.setText(resourceMap.getString("hargaField.text"));
        hargaField.setFocusable(false);
        hargaField.setName("hargaField");
        discountLabel.setDisplayedMnemonic('D');
        discountLabel.setLabelFor(discountField);
        discountLabel.setText(resourceMap.getString("discountLabel.text"));
        discountLabel.setName("discountLabel");
        discountField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        discountField.setText(resourceMap.getString("discountField.text"));
        discountField.setName("discountField");
        discountField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                discountFieldKeyPressed(evt);
            }
        });
        tambahButton.setAction(actionMap.get("tambahItem"));
        tambahButton.setMnemonic('a');
        tambahButton.setText(resourceMap.getString("tambahButton.text"));
        tambahButton.setName("tambahButton");
        tambahButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                tambahButtonKeyPressed(evt);
            }
        });
        beratLabel.setDisplayedMnemonic('B');
        beratLabel.setLabelFor(beratField);
        beratLabel.setText(resourceMap.getString("beratLabel.text"));
        beratLabel.setName("beratLabel");
        beratField.setBackground(resourceMap.getColor("beratField.background"));
        beratField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        beratField.setText(resourceMap.getString("beratField.text"));
        beratField.setName("beratField");
        beratField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                beratFieldKeyPressed(evt);
            }
        });
        javax.swing.GroupLayout inputMenuPanelLayout = new javax.swing.GroupLayout(inputMenuPanel);
        inputMenuPanel.setLayout(inputMenuPanelLayout);
        inputMenuPanelLayout.setHorizontalGroup(inputMenuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(inputMenuPanelLayout.createSequentialGroup().addContainerGap().addGroup(inputMenuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(menuLabel).addComponent(namaLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(inputMenuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(inputMenuPanelLayout.createSequentialGroup().addComponent(menuField, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jumlahLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jumlahField, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(beratLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(beratField, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(hargaLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(hargaField, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(discountLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(discountField, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tambahButton)).addComponent(namaField, 0, 776, Short.MAX_VALUE)).addContainerGap()));
        inputMenuPanelLayout.setVerticalGroup(inputMenuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inputMenuPanelLayout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(inputMenuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(namaLabel).addComponent(namaField, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(inputMenuPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jumlahLabel).addComponent(jumlahField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(menuLabel).addComponent(tambahButton).addComponent(discountLabel).addComponent(discountField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(hargaField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(hargaLabel).addComponent(beratLabel).addComponent(beratField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(menuField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        commandPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        commandPanel.setName("commandPanel");
        transaksiBaruButton.setAction(actionMap.get("baruTransaksi"));
        transaksiBaruButton.setMnemonic('n');
        transaksiBaruButton.setText(resourceMap.getString("transaksiBaruButton.text"));
        transaksiBaruButton.setName("transaksiBaruButton");
        transaksiBaruButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                transaksiBaruButtonKeyPressed(evt);
            }
        });
        bukaTransaksiButton.setAction(actionMap.get("bukaTransaksi"));
        bukaTransaksiButton.setMnemonic('B');
        bukaTransaksiButton.setName("bukaTransaksiButton");
        bukaTransaksiButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                bukaTransaksiButtonKeyPressed(evt);
            }
        });
        pembayaranButton.setAction(actionMap.get("bayarTransaksi"));
        pembayaranButton.setMnemonic('P');
        pembayaranButton.setName("pembayaranButton");
        pembayaranButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                pembayaranButtonKeyPressed(evt);
            }
        });
        cetakChecklistButton.setAction(actionMap.get("printChecklist"));
        cetakChecklistButton.setMnemonic('C');
        cetakChecklistButton.setText(resourceMap.getString("cetakChecklistButton.text"));
        cetakChecklistButton.setName("cetakChecklistButton");
        cetakChecklistButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                cetakChecklistButtonKeyPressed(evt);
            }
        });
        javax.swing.GroupLayout commandPanelLayout = new javax.swing.GroupLayout(commandPanel);
        commandPanel.setLayout(commandPanelLayout);
        commandPanelLayout.setHorizontalGroup(commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(commandPanelLayout.createSequentialGroup().addContainerGap().addGroup(commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(pembayaranButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, commandPanelLayout.createSequentialGroup().addComponent(transaksiBaruButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(bukaTransaksiButton)).addComponent(cetakChecklistButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        commandPanelLayout.setVerticalGroup(commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(commandPanelLayout.createSequentialGroup().addContainerGap().addGroup(commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(transaksiBaruButton).addComponent(bukaTransaksiButton)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pembayaranButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(cetakChecklistButton).addContainerGap(29, Short.MAX_VALUE)));
        summaryPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        summaryPanel.setName("summaryPanel");
        subTotalLabel.setLabelFor(subTotalField);
        subTotalLabel.setText(resourceMap.getString("subTotalLabel.text"));
        subTotalLabel.setName("subTotalLabel");
        subTotalField.setBackground(resourceMap.getColor("totalTransaksiField.background"));
        subTotalField.setEditable(false);
        subTotalField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        subTotalField.setText(resourceMap.getString("subTotalField.text"));
        subTotalField.setName("subTotalField");
        totalDiscountField.setBackground(resourceMap.getColor("totalTransaksiField.background"));
        totalDiscountField.setEditable(false);
        totalDiscountField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        totalDiscountField.setText(resourceMap.getString("totalDiscountField.text"));
        totalDiscountField.setName("totalDiscountField");
        totalDiscountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        totalDiscountLabel.setLabelFor(discountField);
        totalDiscountLabel.setText(resourceMap.getString("totalDiscountLabel.text"));
        totalDiscountLabel.setName("totalDiscountLabel");
        pajakLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        pajakLabel.setLabelFor(pajakLabel);
        pajakLabel.setText(resourceMap.getString("pajakLabel.text"));
        pajakLabel.setName("pajakLabel");
        pajakField.setBackground(resourceMap.getColor("totalTransaksiField.background"));
        pajakField.setEditable(false);
        pajakField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        pajakField.setText(resourceMap.getString("pajakField.text"));
        pajakField.setName("pajakField");
        totalTransaksiLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        totalTransaksiLabel.setLabelFor(totalTransaksiField);
        totalTransaksiLabel.setText(resourceMap.getString("totalTransaksiLabel.text"));
        totalTransaksiLabel.setName("totalTransaksiLabel");
        totalTransaksiField.setBackground(resourceMap.getColor("totalTransaksiField.background"));
        totalTransaksiField.setEditable(false);
        totalTransaksiField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        totalTransaksiField.setText(resourceMap.getString("totalTransaksiField.text"));
        totalTransaksiField.setName("totalTransaksiField");
        totalBayarLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        totalBayarLabel.setLabelFor(totalBayarField);
        totalBayarLabel.setText(resourceMap.getString("totalBayarLabel.text"));
        totalBayarLabel.setName("totalBayarLabel");
        totalBayarField.setBackground(resourceMap.getColor("totalTransaksiField.background"));
        totalBayarField.setEditable(false);
        totalBayarField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        totalBayarField.setText(resourceMap.getString("totalBayarField.text"));
        totalBayarField.setName("totalBayarField");
        sisaLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        sisaLabel.setLabelFor(sisaField);
        sisaLabel.setText(resourceMap.getString("sisaLabel.text"));
        sisaLabel.setName("sisaLabel");
        sisaField.setBackground(resourceMap.getColor("totalTransaksiField.background"));
        sisaField.setEditable(false);
        sisaField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        sisaField.setText(resourceMap.getString("sisaField.text"));
        sisaField.setName("sisaField");
        javax.swing.GroupLayout summaryPanelLayout = new javax.swing.GroupLayout(summaryPanel);
        summaryPanel.setLayout(summaryPanelLayout);
        summaryPanelLayout.setHorizontalGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, summaryPanelLayout.createSequentialGroup().addGap(190, 190, 190).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pajakLabel).addComponent(totalDiscountLabel).addComponent(subTotalLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(subTotalField).addComponent(pajakField, javax.swing.GroupLayout.Alignment.TRAILING).addComponent(totalDiscountField, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(totalTransaksiLabel).addComponent(totalBayarLabel).addComponent(sisaLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(totalTransaksiField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE).addComponent(totalBayarField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE).addComponent(sisaField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)).addContainerGap()));
        summaryPanelLayout.setVerticalGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(summaryPanelLayout.createSequentialGroup().addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(summaryPanelLayout.createSequentialGroup().addGap(77, 77, 77).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(sisaLabel).addComponent(pajakLabel))).addGroup(summaryPanelLayout.createSequentialGroup().addGap(17, 17, 17).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(totalTransaksiLabel).addComponent(subTotalLabel))).addGroup(summaryPanelLayout.createSequentialGroup().addContainerGap().addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(subTotalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(totalTransaksiField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(totalBayarField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(totalDiscountField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(sisaField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(pajakField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(summaryPanelLayout.createSequentialGroup().addGap(47, 47, 47).addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(totalBayarLabel).addComponent(totalDiscountLabel)))).addContainerGap(23, Short.MAX_VALUE)));
        javax.swing.GroupLayout contentPanelLayout = new javax.swing.GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, contentPanelLayout.createSequentialGroup().addContainerGap().addGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(transaksiScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 846, Short.MAX_VALUE).addComponent(inputMenuPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, contentPanelLayout.createSequentialGroup().addComponent(commandPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(summaryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))).addContainerGap()));
        contentPanelLayout.setVerticalGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(contentPanelLayout.createSequentialGroup().addContainerGap().addComponent(inputMenuPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(transaksiScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(commandPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(summaryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        footerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        footerPanel.setName("footerPanel");
        cetakInvoiceButton.setAction(actionMap.get("printInvoice"));
        cetakInvoiceButton.setMnemonic('I');
        cetakInvoiceButton.setText(resourceMap.getString("cetakInvoiceButton.text"));
        cetakInvoiceButton.setName("cetakInvoiceButton");
        cetakInvoiceButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                cetakInvoiceButtonKeyPressed(evt);
            }
        });
        postingInvoiceButton.setAction(actionMap.get("postingInvoice"));
        postingInvoiceButton.setMnemonic('o');
        postingInvoiceButton.setText(resourceMap.getString("postingInvoiceButton.text"));
        postingInvoiceButton.setName("postingInvoiceButton");
        postingInvoiceButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                postingInvoiceButtonKeyPressed(evt);
            }
        });
        promoCheckBox.setText(resourceMap.getString("promoCheckBox.text"));
        promoCheckBox.setName("promoCheckBox");
        promoDiscountTextField.setText(resourceMap.getString("promoDiscountTextField.text"));
        promoDiscountTextField.setName("promoDiscountTextField");
        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, promoCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), promoDiscountTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);
        promoDiscountButton.setAction(actionMap.get("updateDiscount"));
        promoDiscountButton.setText(resourceMap.getString("promoDiscountButton.text"));
        promoDiscountButton.setName("promoDiscountButton");
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, promoCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), promoDiscountButton, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);
        javax.swing.GroupLayout footerPanelLayout = new javax.swing.GroupLayout(footerPanel);
        footerPanel.setLayout(footerPanelLayout);
        footerPanelLayout.setHorizontalGroup(footerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, footerPanelLayout.createSequentialGroup().addContainerGap().addComponent(promoCheckBox).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(promoDiscountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(promoDiscountButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 379, Short.MAX_VALUE).addComponent(cetakInvoiceButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(postingInvoiceButton).addContainerGap()));
        footerPanelLayout.setVerticalGroup(footerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(footerPanelLayout.createSequentialGroup().addContainerGap().addGroup(footerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(postingInvoiceButton).addComponent(cetakInvoiceButton).addComponent(promoCheckBox).addComponent(promoDiscountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(promoDiscountButton)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(contentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(footerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(headerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(headerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(contentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(footerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
        bindingGroup.bind();
        pack();
    }

    private void transaksiTableMouseClicked(java.awt.event.MouseEvent evt) {
        if ((evt.getClickCount() == 2) && (evt.getButton() == MouseEvent.BUTTON1)) {
            gantiJumlahItem();
        }
        if (evt.getButton() == MouseEvent.BUTTON3) {
            transaksiTableMenu.show(transaksiTable, evt.getX(), evt.getY());
        }
    }

    private void editJumlahMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        gantiJumlahItem();
    }

    private void customerFieldMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            int Nomor = 0;
            Date Tanggal = null;
            Nomor = Integer.valueOf(invoiceField.getText());
            try {
                Tanggal = dateDisplay.parse(tanggalField.getText());
            } catch (ParseException ex) {
                ex.printStackTrace();
                Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (transaksiCustomerView == null) {
                JFrame mainFrame = MainApp.getApplication().getMainFrame();
                transaksiCustomerView = new TransaksiCustomerView(mainFrame, true);
                transaksiCustomerView.setLocationRelativeTo(mainFrame);
            }
            MainApp.getApplication().show(transaksiCustomerView);
            if (transaksiCustomerView.getConfirmNew()) {
                Customer = transaksiCustomerView.getCustomerValue();
                updateCustomer(Customer, Nomor, Tanggal);
                customerField.setText(Customer);
            }
        }
    }

    private void tambahButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            tambahItem();
        }
    }

    private void editBeratMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        gantiBeratItem();
    }

    private void transaksiBaruButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            baruTransaksi();
        }
    }

    private void bukaTransaksiButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            bukaTransaksi();
        }
    }

    private void pembayaranButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            bayarTransaksi();
        }
    }

    private void cetakChecklistButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            printChecklist();
        }
    }

    private void cetakInvoiceButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            printInvoice();
        }
    }

    private void postingInvoiceButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            postingInvoice();
        }
    }

    private void namaFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String nama;
        nama = namaField.getEditor().getItem().toString();
        if ((namaField.getSelectedIndex() != -1) && (cfg.isMenuNameExists(nama))) {
            fillMenuData();
        }
    }

    private void namaFieldKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            evt.getComponent().transferFocus();
        }
    }

    private void jumlahFieldKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            evt.getComponent().transferFocus();
        }
    }

    private void beratFieldKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            evt.getComponent().transferFocus();
        }
    }

    private void discountFieldKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            evt.getComponent().transferFocus();
        }
    }

    @Action
    public void baruTransaksi() {
        if (JOptionPane.showConfirmDialog(null, "Buat transaksi baru?", "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (transaksiCustomerView == null) {
                JFrame mainFrame = MainApp.getApplication().getMainFrame();
                transaksiCustomerView = new TransaksiCustomerView(mainFrame, true);
                transaksiCustomerView.setLocationRelativeTo(mainFrame);
            }
            MainApp.getApplication().show(transaksiCustomerView);
            if (transaksiCustomerView.getConfirmNew()) {
                try {
                    Calendar cal = Calendar.getInstance();
                    int Nomor = cfg.getNomorTransaksi();
                    Customer = transaksiCustomerView.getCustomerValue();
                    if (cfg.confirmReopenTransaction(dateDisplay.parse(tanggalField.getText()), Customer)) {
                        newTransaksi(Nomor, cal.getTime(), Integer.valueOf(shiftField.getText()), Customer, cfg.getLoggedUser());
                        cfg.incNomorTransaksi();
                        openTransaksiForm(Nomor, cal.getTime());
                        formatTransaksiTable();
                    }
                } catch (ParseException ex) {
                    ex.printStackTrace();
                    Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                JOptionPane.showMessageDialog(null, "Transaksi baru dibatalkan!");
            }
        }
    }

    @Action
    public void tutupTransaksiView() {
        dispose();
    }

    private List openDetilTransaksi(int Nomor, Date Tanggal) {
        List transaksiList = null;
        try {
            transaksiList = transaksiEntity.createNativeQuery("SELECT dt.menu, CASE WHEN (dt.berat>0) THEN CONCAT(m.nama,' ( x',CAST(dt.jumlah AS CHAR),')') ELSE (m.nama) END AS nama, CASE WHEN (dt.berat>0) THEN dt.berat ELSE dt.jumlah END AS jumlah, dt.harga FROM detiltransaksi dt INNER JOIN menu m ON dt.menu = m.menu WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal").setParameter("nomor", Nomor).setParameter("tanggal", Tanggal).getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return transaksiList;
    }

    @Action
    public void tambahItem() {
        try {
            Date Tanggal = dateDisplay.parse(tanggalField.getText());
            Double Pajak;
            Double Diskon;
            Double Harga;
            Double Jumlah;
            Double Berat;
            Harga = Double.valueOf(String.valueOf(floatDisplay.parse(hargaField.getText())));
            Diskon = Double.valueOf(String.valueOf(floatDisplay.parse(discountField.getText())));
            Jumlah = Double.valueOf(String.valueOf(jumlahField.getText()));
            Berat = Double.valueOf(String.valueOf(floatDisplay.parse(beratField.getText())));
            if (cfg.isVATItem(Integer.valueOf(menuField.getText()))) {
                Pajak = cfg.getVatValue() * (Harga - Diskon) / 100;
            } else {
                Pajak = 0.0;
            }
            newDetilTransaksi(Integer.valueOf(invoiceField.getText()), Tanggal, Integer.valueOf(menuField.getText()), Jumlah, Harga, Diskon, Pajak, cfg.getLoggedUser(), Berat);
            transaksiTable.setModel(new TransaksiViewTM(openDetilTransaksi(Integer.valueOf(invoiceField.getText()), Tanggal)));
            formatTransaksiTable();
            setTransaksiSummary();
            resetInputForm();
        } catch (ParseException ex) {
            ex.printStackTrace();
            Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action
    public void bukaTransaksi() {
        if ((transaksiOpenView != null) && (!transaksiOpenView.isVisible())) {
            transaksiOpenView = null;
        }
        if (transaksiOpenView == null) {
            JFrame mainFrame = MainApp.getApplication().getMainFrame();
            transaksiOpenView = new TransaksiOpenView(mainFrame, true);
            transaksiOpenView.setLocationRelativeTo(mainFrame);
        }
        MainApp.getApplication().show(transaksiOpenView);
        if (transaksiOpenView.getConfirmOpen()) {
            try {
                Customer = transaksiOpenView.getOpenedCustomer();
                openTransaksiForm(transaksiOpenView.getNomorTransaksi(), dateSQL.parse(transaksiOpenView.getTanggalTransaksi()));
                formatTransaksiTable();
            } catch (ParseException ex) {
                ex.printStackTrace();
                Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Action
    @SuppressWarnings("unchecked")
    public void printInvoice() {
        Integer Nomor;
        Date Tanggal;
        Nomor = Integer.valueOf(invoiceField.getText());
        try {
            Tanggal = dateDisplay.parse(tanggalField.getText());
            if (cfg.getTotalTransaksi(Nomor, Tanggal).equals(0.0)) {
                JOptionPane.showMessageDialog(this, "Belum ada transaksi untuk dicetak");
            } else {
                Map parameters = new HashMap();
                parameters = cfg.getCompanyReportHeader();
                parameters.put("NOMOR_INVOICE", invoiceField.getText());
                parameters.put("TANGGAL_INVOICE", dateSQL.format(Tanggal));
                parameters.put("RESTOBIZ_MESSAGE", cfg.getPesanPromosi());
                cfg.previewReport(this, "invoice.jasper", parameters, "Preview Invoice", true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action
    public void bayarTransaksi() {
        try {
            Integer Nomor;
            Date Tanggal;
            Nomor = Integer.valueOf(invoiceField.getText());
            Tanggal = dateDisplay.parse(tanggalField.getText());
            if (cfg.getTotalTransaksi(Nomor, Tanggal).equals(0.0)) {
                JOptionPane.showMessageDialog(this, "Belum ada transaksi untuk dibayar");
            } else {
                if ((pembayaranView != null) && (!pembayaranView.isVisible())) {
                    pembayaranView = null;
                }
                if (pembayaranView == null) {
                    JFrame mainFrame = MainApp.getApplication().getMainFrame();
                    pembayaranView = new PembayaranView(mainFrame, true, Nomor, Tanggal);
                    pembayaranView.setLocationRelativeTo(mainFrame);
                }
                MainApp.getApplication().show(pembayaranView);
            }
            setTransaksiSummary();
        } catch (ParseException ex) {
            ex.printStackTrace();
            Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action
    public void changeShift() {
        if (JOptionPane.showConfirmDialog(null, "Ganti shift?", "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            int n;
            cfg.incCurrentShift();
            n = cfg.getCurrentShift();
            setShiftValue(n);
        }
    }

    @Action
    public void hapusMenuTransaksi() {
        if (transaksiTable.getValueAt(transaksiTable.getSelectedRow(), 0) != "") {
            if (JOptionPane.showConfirmDialog(null, "Hapus item " + transaksiTable.getValueAt(transaksiTable.getSelectedRow(), 1) + " dari transaksi?", "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                hapusTabelItem();
            }
        }
    }

    @Action
    public void postingInvoice() {
        if (JOptionPane.showConfirmDialog(this, "Anda yakin akan memposting invoice ini?\nSetelah posting, invoice tidak dapat diubah.", "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                Date Tanggal;
                Tanggal = dateDisplay.parse(tanggalField.getText());
                execPostingInvoice(Integer.valueOf(invoiceField.getText()), Tanggal);
            } catch (ParseException ex) {
                ex.printStackTrace();
                Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Action
    @SuppressWarnings("unchecked")
    public void printChecklist() {
        Integer Nomor;
        Date Tanggal;
        Nomor = Integer.valueOf(invoiceField.getText());
        try {
            Tanggal = dateDisplay.parse(tanggalField.getText());
            if (cfg.getTotalTransaksi(Nomor, Tanggal).equals(0.0)) {
                JOptionPane.showMessageDialog(this, "Belum ada transaksi untuk dicetak");
            } else {
                Map parameters = new HashMap();
                parameters = cfg.getCompanyReportHeader();
                parameters.put("NOMOR_INVOICE", invoiceField.getText());
                parameters.put("TANGGAL_INVOICE", dateSQL.format(Tanggal));
                cfg.previewReport(this, "checklist.jasper", parameters, "Preview Checklist", true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action
    public void updateDiscount() {
        try {
            Integer Nomor;
            Date Tanggal;
            Double Discount;
            Nomor = Integer.valueOf(invoiceField.getText());
            Tanggal = dateDisplay.parse(tanggalField.getText());
            Discount = Double.valueOf(promoDiscountTextField.getText());
            updateAllDiscount(Nomor, Tanggal, Discount);
            setTransaksiSummary();
            resetInputForm();
        } catch (ParseException ex) {
            Logger.getLogger(TransaksiView.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    private javax.swing.JTextField beratField;

    private javax.swing.JLabel beratLabel;

    private javax.swing.JButton bukaTransaksiButton;

    private javax.swing.JButton cetakChecklistButton;

    private javax.swing.JButton cetakInvoiceButton;

    private javax.swing.JPanel commandPanel;

    private javax.swing.JPanel contentPanel;

    private javax.swing.JLabel customerField;

    private javax.swing.JLabel customerLabel;

    private javax.swing.JMenuItem deleteItemMenuItem;

    private javax.swing.JTextField discountField;

    private javax.swing.JLabel discountLabel;

    private javax.swing.JMenuItem editBeratMenuItem;

    private javax.swing.JSeparator editBeratSeparator;

    private javax.swing.JMenuItem editJumlahMenuItem;

    private javax.swing.JPanel footerPanel;

    private javax.swing.JButton gantiShiftButton;

    private javax.swing.JTextField hargaField;

    private javax.swing.JLabel hargaLabel;

    private javax.swing.JLabel headerLabel;

    private javax.swing.JPanel headerPanel;

    private javax.swing.JPanel inputMenuPanel;

    private javax.swing.JLabel invoiceField;

    private javax.swing.JLabel invoiceLabel;

    private javax.swing.JTextField jumlahField;

    private javax.swing.JLabel jumlahLabel;

    private javax.swing.JLabel loggedUserField;

    private javax.swing.JTextField menuField;

    private javax.swing.JLabel menuLabel;

    private javax.swing.JComboBox namaField;

    private javax.swing.JLabel namaLabel;

    private javax.swing.JTextField pajakField;

    private javax.swing.JLabel pajakLabel;

    private javax.swing.JButton pembayaranButton;

    private javax.swing.JButton postingInvoiceButton;

    private javax.swing.JCheckBox promoCheckBox;

    private javax.swing.JButton promoDiscountButton;

    private javax.swing.JTextField promoDiscountTextField;

    private javax.swing.JLabel shiftField;

    private javax.swing.JLabel shiftLabel;

    private javax.swing.JTextField sisaField;

    private javax.swing.JLabel sisaLabel;

    private javax.swing.JTextField subTotalField;

    private javax.swing.JLabel subTotalLabel;

    private javax.swing.JPanel summaryPanel;

    private javax.swing.JButton tambahButton;

    private javax.swing.JLabel tanggalField;

    private javax.swing.JLabel tanggalLabel;

    private javax.swing.JTextField totalBayarField;

    private javax.swing.JLabel totalBayarLabel;

    private javax.swing.JTextField totalDiscountField;

    private javax.swing.JLabel totalDiscountLabel;

    private javax.swing.JTextField totalTransaksiField;

    private javax.swing.JLabel totalTransaksiLabel;

    private javax.swing.JButton transaksiBaruButton;

    private javax.swing.JScrollPane transaksiScrollPane;

    private javax.swing.JTable transaksiTable;

    private javax.swing.JPopupMenu transaksiTableMenu;

    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
}
