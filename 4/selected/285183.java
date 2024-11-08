package client;

import client.entidades.Channel;
import client.entidades.Client;
import client.entidades.User;
import comum.InterpretadorMSG;
import java.awt.Component;
import java.util.Observable;
import java.util.Observer;
import comum.Message;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author Anderson
 */
public class JanelaPrincipal extends javax.swing.JFrame implements Observer {

    private static JanelaPrincipal instancia = null;

    private PainelBase painelSelecionado;

    private ArrayList<PainelBase> abas = new ArrayList<PainelBase>();

    public static JanelaPrincipal getInstancia() {
        if (instancia == null) {
            instancia = new JanelaPrincipal();
        }
        return instancia;
    }

    public void trocaStatus(String txt) {
        status.setText(txt);
        trocaIcone();
    }

    private void trocaIcone() {
        if (status.getText().equals("Conectado")) {
            btConnect.setEnabled(false);
            btDesconnect.setEnabled(true);
            status.setForeground(Color.BLUE);
        } else {
            btConnect.setEnabled(true);
            btDesconnect.setEnabled(false);
            status.setForeground(Color.RED);
        }
    }

    private void criaTabServidor() {
        addPainel(new PainelUsuario(new User("Servidor")));
    }

    public PainelBase getPainelSelecionado() {
        return painelSelecionado;
    }

    public void setPainelSelecionado(PainelBase painelSelecionado) {
        this.painelSelecionado = painelSelecionado;
    }

    private JanelaPrincipal() {
        initComponents();
        init2();
        trocaStatus("Desconectado");
        criaTabServidor();
        Client.getInstance().addObserver(this);
    }

    public PainelBase getPainelPeloNome(String nomePainel) {
        for (PainelBase p : abas) {
            if (p.toString().equals(nomePainel)) {
                return p;
            }
        }
        return null;
    }

    public void daFocoPainel(PainelBase p) {
        painelCentral.setSelectedComponent(p);
    }

    public void addPainel(PainelBase p) {
        painelCentral.add(p.getPainelName(), p);
        painelCentral.setSelectedComponent(p);
        setPainelSelecionado(p);
        abas.add(p);
    }

    public void removePainel(Channel c) {
        Component[] components = painelCentral.getComponents();
        for (Component comp : components) {
            if (comp instanceof PainelCanal) {
                if (((PainelCanal) comp).getCanal() == c) {
                    painelCentral.remove(comp);
                    abas.remove(comp);
                    Client.getInstance().getChannels().removerCanal(c);
                }
            }
        }
    }

    public void removePainel(User u) {
        Component[] components = painelCentral.getComponents();
        for (Component comp : components) {
            if (comp instanceof PainelUsuario) {
                if (((PainelUsuario) comp).getUsuario() == u) {
                    painelCentral.remove(comp);
                    abas.remove(comp);
                    Client.getInstance().getUsers().removerUsuario(u);
                }
            }
        }
    }

    private void PainelTabMouseClicked(java.awt.event.MouseEvent evt) {
        setPainelSelecionado((PainelBase) (JPanel) painelCentral.getSelectedComponent());
        if (!getPainelSelecionado().toString().equals("Servidor")) {
            if (evt.getButton() == MouseEvent.BUTTON3) {
                if (JOptionPane.showConfirmDialog(this, "Deseja fechar esta aba?") == 0) {
                    Object painel = painelCentral.getSelectedComponent();
                    if (painel instanceof PainelBase) {
                        PainelBase painelBase = (PainelBase) painel;
                        if (painelBase.isEhUser()) {
                            removePainel((User) painelBase.getParentIMsgSorce());
                        }
                        if (painelBase.isEhChannel()) {
                            removePainel((Channel) painelBase.getParentIMsgSorce());
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        barraFerramentas = new javax.swing.JToolBar();
        btConnect = new javax.swing.JButton();
        btDesconnect = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jLabel1 = new javax.swing.JLabel();
        status = new javax.swing.JLabel();
        painelCentral = new javax.swing.JTabbedPane();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("iMirc");
        barraFerramentas.setFloatable(false);
        barraFerramentas.setRollover(true);
        barraFerramentas.setEnabled(false);
        btConnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/client/icons/connectIcon.png")));
        btConnect.setToolTipText("Conectar");
        btConnect.setFocusPainted(false);
        btConnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btConnectActionPerformed(evt);
            }
        });
        barraFerramentas.add(btConnect);
        btDesconnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/client/icons/disconnectIcon.png")));
        btDesconnect.setToolTipText("Desconectar");
        btDesconnect.setFocusable(false);
        btDesconnect.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btDesconnect.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btDesconnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btDesconnectActionPerformed(evt);
            }
        });
        barraFerramentas.add(btDesconnect);
        barraFerramentas.add(jSeparator1);
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/client/novo_canal.png")));
        jButton1.setToolTipText("Novo canal");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        barraFerramentas.add(jButton1);
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/client/config.png")));
        jButton2.setToolTipText("Configurações");
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        barraFerramentas.add(jButton2);
        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/client/conexao.png")));
        jButton3.setToolTipText("Configuração conexão");
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        barraFerramentas.add(jButton3);
        barraFerramentas.add(jSeparator2);
        jLabel1.setText("Status:  ");
        barraFerramentas.add(jLabel1);
        status.setFont(new java.awt.Font("sansserif", 0, 10));
        status.setForeground(new java.awt.Color(0, 204, 0));
        status.setText("Status");
        barraFerramentas.add(status);
        painelCentral.setBorder(null);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(barraFerramentas, javax.swing.GroupLayout.DEFAULT_SIZE, 831, Short.MAX_VALUE).addComponent(painelCentral, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 831, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(barraFerramentas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(painelCentral, javax.swing.GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)));
        pack();
    }

    private void btConnectActionPerformed(java.awt.event.ActionEvent evt) {
        Client.getInstance().conectar();
    }

    private void btDesconnectActionPerformed(java.awt.event.ActionEvent evt) {
        Client.getInstance().disconnect();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        JanelaNovoCanal janelaNovoCanal = new JanelaNovoCanal();
        janelaNovoCanal.setLocationRelativeTo(null);
        janelaNovoCanal.setVisible(true);
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        JanelaConfiguracaoGeral janelaCOnfiguracaoGeral = new JanelaConfiguracaoGeral();
        janelaCOnfiguracaoGeral.setLocationRelativeTo(this);
        janelaCOnfiguracaoGeral.setVisible(true);
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        JanelaConfiguracaoConexao janelaConfiguracaoConexao = new JanelaConfiguracaoConexao();
        janelaConfiguracaoConexao.setLocationRelativeTo(this);
        janelaConfiguracaoConexao.setVisible(true);
    }

    private javax.swing.JToolBar barraFerramentas;

    private javax.swing.JButton btConnect;

    private javax.swing.JButton btDesconnect;

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButton3;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JToolBar.Separator jSeparator1;

    private javax.swing.JToolBar.Separator jSeparator2;

    private javax.swing.JTabbedPane painelCentral;

    private javax.swing.JLabel status;

    public void update(Observable o, Object arg) {
        if (arg instanceof String) {
            String leitura = (String) arg;
            InterpretadorMSG.getInstance().interpretaMsg(new Message(leitura));
        }
    }

    private void init2() {
        painelCentral.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                PainelTabMouseClicked(evt);
            }
        });
    }
}
