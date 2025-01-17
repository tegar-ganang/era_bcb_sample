package lcjeminertool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author Hisayani
 */
public class MineralChart extends javax.swing.JFrame implements Runnable {

    private MiningTool tool;

    private MiningView view;

    /** Creates new form MineralChart
     * @param view
     * @param tool
     */
    public MineralChart(MiningView view, MiningTool tool) {
        this.tool = tool;
        this.view = view;
        initComponents();
        this.jList1.setSelectedIndex(0);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jSeparator1 = new javax.swing.JSeparator();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        priceField = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Mineral Chart");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }

            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });
        jScrollPane1.setName("jScrollPane1");
        jList1.setModel(new javax.swing.AbstractListModel() {

            List<Mineral> list = tool.getMinerals();

            public int getSize() {
                return list.size();
            }

            public Object getElementAt(int i) {
                return list.get(i).getName();
            }
        });
        jList1.setDoubleBuffered(true);
        jList1.setName("jList1");
        jList1.addListSelectionListener(new javax.swing.event.ListSelectionListener() {

            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList1ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);
        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator1.setDoubleBuffered(true);
        jSeparator1.setName("jSeparator1");
        jButton1.setText("Retrieve from eve-central");
        jButton1.setName("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jLabel1.setText("Average Price :");
        jLabel1.setName("jLabel1");
        priceField.setName("priceField");
        jButton2.setText("Apply");
        jButton2.setName("jButton2");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup().addComponent(priceField, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)).addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap(258, Short.MAX_VALUE).addComponent(jButton1).addContainerGap()).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addGap(39, 39, 39).addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(priceField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jButton2))).addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)).addContainerGap()));
        pack();
    }

    private void jList1ValueChanged(javax.swing.event.ListSelectionEvent evt) {
        if (this.jList1.getSelectedIndex() >= 0) {
            this.priceField.setText(String.valueOf(this.tool.getMinerals().get(this.jList1.getSelectedIndex()).getRegionPrice()));
        }
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        if (!this.priceField.getText().equals("")) {
            try {
                this.tool.getMinerals().get(this.jList1.getSelectedIndex()).setRegionPrice(Double.parseDouble(this.priceField.getText()));
                this.view.listChanged();
            } catch (NumberFormatException ex) {
            }
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            File tmp = File.createTempFile("marketinf", "xml");
            PrintStream out = new PrintStream(tmp);
            out.print(getMarketInfo());
            out.close();
            parser.parse(tmp, new MarketInfoHandler(this.tool.getMinerals()));
            updateList();
            this.jList1.setSelectedIndex(0);
        } catch (IOException ex) {
            Logger.getLogger(MineralChart.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(MineralChart.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(MineralChart.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void formWindowDeactivated(java.awt.event.WindowEvent evt) {
        view.updateData();
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        view.updateData();
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JList jList1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JTextField priceField;

    public String getMarketInfo() {
        try {
            URL url = new URL("http://api.eve-central.com/api/evemon");
            BufferedReader s = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            String xml = "";
            while ((line = s.readLine()) != null) {
                xml += line;
            }
            return xml;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void run() {
        this.setVisible(true);
    }

    private void updateList() {
        this.jList1.setModel(new javax.swing.AbstractListModel() {

            List<Mineral> list = tool.getMinerals();

            public int getSize() {
                return list.size();
            }

            public Object getElementAt(int i) {
                return list.get(i).getName();
            }
        });
        this.jList1.updateUI();
    }
}
