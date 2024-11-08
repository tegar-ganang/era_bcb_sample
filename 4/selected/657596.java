package br.org.dbt.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.GroupLayout.Alignment;
import br.org.dbt.connection.ConnectionFactory;
import br.org.dbt.control.Comparar;
import br.org.dbt.load.LoadWriteProperties;
import br.org.dbt.model.ColumnBean;
import br.org.dbt.model.ConnectionBean;
import br.org.dbt.model.ConnectionParm;
import br.org.dbt.model.ConstraintBean;
import br.org.dbt.model.TableBean;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private JDesktopPane desktop = new JDesktopPane();

    private EstruturaWindow estruturawindow = new EstruturaWindow();

    private Console console = new Console();

    private ConnectionBean conn_source, conn_target;

    private MainWindow() {
        super("Database Tools");
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height - inset * 2);
        desktop = new JDesktopPane();
        desktop.add(console);
        desktop.add(estruturawindow);
        setContentPane(desktop);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        chamaConsole();
        chamaEstrutura();
    }

    private void chamaEstrutura() {
        estruturawindow.setVisible(true);
    }

    private void chamaConsole() {
        console.setVisible(true);
    }

    public static void main(String[] args) {
        MainWindow frame = new MainWindow();
        frame.setVisible(true);
    }

    private class EstruturaWindow extends JInternalFrame {

        private static final long serialVersionUID = 1L;

        private JLabel lbSource;

        private JLabel lbTarget;

        private JButton btSource;

        private JButton btTarget;

        private JLabel lbObject;

        private JLabel lbSeparator;

        private JLabel lbColorSource;

        private JLabel lbColorTarget;

        private JCheckBox ckTable;

        private JCheckBox ckColumn;

        private JCheckBox ckPrimaryKey;

        private JCheckBox ckForeignKey;

        private JCheckBox ckCheckConstraint;

        private JButton btComparar;

        private JButton btGerar;

        private Color corR = new Color(10027059);

        private Color corG = new Color(3381555);

        private EstruturaWindow() {
            super("Estruturas", false, false, false, false);
            this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            lbSource = new JLabel("Conexao Origem:");
            lbTarget = new JLabel("Conexão Destino:");
            btSource = new JButton("Conexão");
            btTarget = new JButton("Conexão");
            lbSeparator = new JLabel("       ");
            lbColorSource = new JLabel("# ");
            lbColorTarget = new JLabel("# ");
            lbObject = new JLabel("Selecionar Componentes:");
            ckTable = new JCheckBox("Tabelas");
            ckColumn = new JCheckBox("Colunas");
            ckPrimaryKey = new JCheckBox("Primary Keys");
            ckForeignKey = new JCheckBox("Foreign Keys");
            ckCheckConstraint = new JCheckBox("Check Constraints");
            btComparar = new JButton("Comparar");
            btGerar = new JButton("Gravar");
            lbColorSource.setForeground(corR);
            lbColorTarget.setForeground(corR);
            GroupLayout layout = new GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
            hGroup.addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(lbSource).addComponent(lbTarget).addComponent(lbSeparator).addComponent(lbObject).addComponent(ckTable).addComponent(ckColumn).addComponent(ckPrimaryKey).addComponent(ckForeignKey).addComponent(ckCheckConstraint));
            hGroup.addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(btSource).addComponent(btTarget).addComponent(btComparar).addComponent(btGerar));
            hGroup.addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(lbColorSource).addComponent(lbColorTarget));
            layout.setHorizontalGroup(hGroup);
            layout.linkSize(SwingConstants.HORIZONTAL, btTarget, btComparar, btSource, btGerar);
            GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(lbSource).addComponent(btSource).addComponent(lbColorSource));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(lbTarget).addComponent(btTarget).addComponent(lbColorTarget));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(lbSeparator));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(lbObject));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(ckTable));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(ckColumn));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(ckPrimaryKey));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(ckForeignKey));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(ckCheckConstraint));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(btComparar));
            vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(btGerar));
            layout.setVerticalGroup(vGroup);
            pack();
            btSource.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    sourceButtonActionPerformed(evt);
                }
            });
            btTarget.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    targetButtonActionPerformed(evt);
                }
            });
            btComparar.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    compararButtonActionPerformed(evt);
                }
            });
        }

        private void sourceButtonActionPerformed(java.awt.event.ActionEvent evt) {
            ConnectionParm con = new ConnectionParm();
            LoadWriteProperties prop = new LoadWriteProperties();
            try {
                prop.loadSourceProperties();
                con.setUsername(prop.conn.getUsername());
                con.setPassword(prop.conn.getPassword());
                con.setHost(prop.conn.getHost());
                con.setPort(prop.conn.getPort());
                con.setSID(prop.conn.getSID());
            } catch (IOException e) {
                e.printStackTrace();
            }
            ConnectionParmWindow window = new ConnectionParmWindow(con);
            window.setVisible(true);
            ConnectionParm conready = new ConnectionParm();
            if (window.getResult() == 1) {
                conready = window.getParms();
                try {
                    ConnectionParm parm_s = new ConnectionParm(conready.getUsername(), conready.getPassword(), conready.getHost(), conready.getPort(), conready.getSID());
                    Connection conn_s = ConnectionFactory.getConnectionSource(parm_s.getUsername(), parm_s.getPassword(), parm_s.getHost(), parm_s.getPort(), parm_s.getSID());
                    conn_source = new ConnectionBean(conn_s, parm_s);
                    System.out.println("Conexao origem estabelecida com servidor: " + conready.getHost() + " Usuario: " + conready.getUsername().toUpperCase() + " Banco: " + conready.getSID());
                    try {
                        prop.writeSourceProperties(conready);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    lbColorSource.setForeground(corG);
                } catch (SQLException e) {
                    lbColorSource.setForeground(corR);
                    System.out.println(e);
                }
            } else {
            }
        }

        private void targetButtonActionPerformed(java.awt.event.ActionEvent evt) {
            ConnectionParm con = new ConnectionParm();
            LoadWriteProperties prop = new LoadWriteProperties();
            try {
                prop.loadTargetProperties();
                con.setUsername(prop.conn.getUsername());
                con.setPassword(prop.conn.getPassword());
                con.setHost(prop.conn.getHost());
                con.setPort(prop.conn.getPort());
                con.setSID(prop.conn.getSID());
            } catch (IOException e) {
                e.printStackTrace();
            }
            ConnectionParmWindow window = new ConnectionParmWindow(con);
            window.setVisible(true);
            if (window.getResult() == 1) {
                try {
                    ConnectionParm conready = new ConnectionParm();
                    conready = window.getParms();
                    ConnectionParm parm_t = new ConnectionParm(conready.getUsername(), conready.getPassword(), conready.getHost(), conready.getPort(), conready.getSID());
                    Connection conn_t = ConnectionFactory.getConnectionTarget(parm_t.getUsername(), parm_t.getPassword(), parm_t.getHost(), parm_t.getPort(), parm_t.getSID());
                    conn_target = new ConnectionBean(conn_t, parm_t);
                    prop.writeTargetProperties(conready);
                    System.out.println("Conexao destino estabelecida com servidor: " + conready.getHost() + " Usuario: " + conready.getUsername().toUpperCase() + " Banco: " + conready.getSID());
                    lbColorTarget.setForeground(corG);
                } catch (Exception e) {
                    lbColorTarget.setForeground(corR);
                    System.out.println(e);
                }
            } else {
            }
        }

        private void compararButtonActionPerformed(java.awt.event.ActionEvent evt) {
            if (ckTable.isSelected()) {
                try {
                    List<TableBean> comparatabela = Comparar.compareTables(conn_source, conn_target);
                    System.out.println("Tabelas nao encontradas:");
                    for (TableBean table : comparatabela) {
                        System.out.println("" + table.getOwner() + "." + table.getTableName() + "");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
            }
            if (ckColumn.isSelected()) {
                try {
                    List<ColumnBean> comparatabelaecoluna = Comparar.compareTablesColumns(conn_source, conn_target);
                    System.out.println("Colunas nao encontradas:");
                    for (ColumnBean column : comparatabelaecoluna) {
                        System.out.println(column.getName() + " da tabela: " + column.getTable().getTableName());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
            }
            if (ckForeignKey.isSelected() && ckPrimaryKey.isSelected()) {
                try {
                    List<ConstraintBean> comparaTabelaConstraint = Comparar.compareTablesConstraints(conn_source, conn_target);
                    System.out.println("Constraints nao encontradas:");
                    for (ConstraintBean constraint : comparaTabelaConstraint) {
                        System.out.println("" + constraint.getType() + " " + constraint.getName());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
            }
            if (ckCheckConstraint.isSelected()) {
                try {
                    List<ConstraintBean> comparaTabelaCheckConstraint = Comparar.compareTablesCheckConstraints(conn_source, conn_target);
                    System.out.println("Check Constraints nao encontradas:");
                    for (ConstraintBean constraint : comparaTabelaCheckConstraint) {
                        System.out.println("" + constraint.getType() + " " + constraint.getName());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("---------------------------------");
            }
        }
    }
}
