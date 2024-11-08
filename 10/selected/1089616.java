package gnu.chu.anjelica.angulo;

import gnu.chu.anjelica.*;
import gnu.chu.controles.*;
import gnu.chu.utilidades.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import gnu.chu.sql.*;
import java.util.*;
import javax.swing.*;
import gnu.chu.isql.*;
import gnu.chu.Menu.*;

/**
 *
 * <p>T�tulo: consVentas</p>
 * <p>Descripci�n: Consulta Ventas de un Representante</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Empresa: </p>
 * @deprecated Este programa es un engendro 'malo' para introducir los datos a traves
 * de una hoja excel y luego consultarlos. NO UTILIZAR.
 * @author gnu.chup
 * @version 1.0
 */
public class consVentas extends ventana {

    String tabla;

    String fecfor = "dd-MM-yyyy";

    boolean INLINUX = false;

    DatosTabla dt;

    DatosTabla dtBloq;

    int nLin;

    utilSql utSq = new utilSql() {

        protected void procesaLinea(Vector v) {
            procesLin(v);
        }
    };

    Statement st1;

    ResultSet rs1;

    Statement st;

    ResultSet rs;

    CPanel Pprinc = new CPanel();

    CLabel cLabel1 = new CLabel();

    CTextField feciniE = new CTextField(Types.DATE, "dd-MM-yyyy");

    CTextField fecfinE = new CTextField(Types.DATE, "dd-MM-yyyy");

    CLabel cLabel2 = new CLabel();

    CButton Baceptar = new CButton();

    Cgrid jt = new Cgrid(9) {
    };

    CButton Bsintar = new CButton();

    CButton BactTar = new CButton();

    CPanel PintrDatos = new CPanel();

    CPanel cPanel1 = new CPanel();

    CLabel cLabel3 = new CLabel();

    CTabbedPane TPentra = new CTabbedPane();

    CPanel Pconsulta = new CPanel();

    CTextField feciniE1 = new CTextField(Types.DATE, "dd-MM-yyyy");

    CLabel cLabel4 = new CLabel();

    CLabel cLabel5 = new CLabel();

    CTextField fecfinE1 = new CTextField(Types.DATE, "dd-MM-yyyy");

    CButton Bacecon = new CButton();

    CPanel cPanel2 = new CPanel();

    CLabel cLabel6 = new CLabel();

    CTextField nombficE = new CTextField();

    CButton Bcarfic = new CButton();

    CButton Bbusfic = new CButton(Iconos.getImageIcon("openFile.png"));

    JFileChooser ficeleE;

    CPanel cPanel3 = new CPanel();

    CLabel cLabel7 = new CLabel();

    CButton Bcarfic1 = new CButton();

    CTextField nombficE1 = new CTextField();

    CButton Bbusfic1 = new CButton(Iconos.getImageIcon("openFile.png"));

    public consVentas(EntornoUsuario eu, Principal p) {
        EU = eu;
        vl = p.panel1;
        jf = p;
        eje = true;
        setTitulo("Consulta Ventas Raul Angulo (V 1.0)");
        try {
            if (jf.gestor.apuntar(this)) jbInit(); else setErrorInit(true);
        } catch (Exception e) {
            e.printStackTrace();
            setErrorInit(true);
        }
    }

    public consVentas(gnu.chu.anjelica.menu p, EntornoUsuario eu) {
        EU = eu;
        vl = p.getLayeredPane();
        setTitulo("Consulta Ventas Raul Angulo (V 1.0)");
        eje = false;
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
            setErrorInit(true);
        }
    }

    private void jbInit() throws Exception {
        iniciar(740, 540);
        conecta();
        dt = new DatosTabla(ct);
        dtBloq = new DatosTabla(ct);
        st = ct.createStatement();
        st1 = ct.createStatement();
        Pprinc.setDefButton(Baceptar);
        statusBar = new StatusBar(this);
        Pprinc.setLayout(null);
        cLabel1.setText("De Fecha");
        cLabel1.setBounds(new Rectangle(11, 22, 55, 18));
        feciniE.setText("01-04-2003");
        feciniE.setBounds(new Rectangle(70, 22, 70, 18));
        fecfinE.setText("05-04-2003");
        fecfinE.setBounds(new Rectangle(213, 22, 77, 18));
        cLabel2.setText("A");
        cLabel2.setBounds(new Rectangle(190, 23, 19, 17));
        Baceptar.setBounds(new Rectangle(118, 43, 108, 24));
        Baceptar.setText("Aceptar");
        Vector v = new Vector();
        Bsintar.setBounds(new Rectangle(137, 314, 168, 27));
        Bsintar.setText("Mostar Ventas S/Tarifa");
        BactTar.setBounds(new Rectangle(354, 314, 97, 27));
        BactTar.setText("Act.Tarifa");
        PintrDatos.setBorder(BorderFactory.createRaisedBevelBorder());
        PintrDatos.setBounds(new Rectangle(156, 0, 311, 75));
        cPanel1.setBounds(new Rectangle(99, 3, 321, 74));
        PintrDatos.setLayout(null);
        cPanel1.setBorder(BorderFactory.createRaisedBevelBorder());
        cPanel1.setBounds(new Rectangle(6, 78, 721, 369));
        cPanel1.setLayout(null);
        cLabel3.setBackground(Color.orange);
        cLabel3.setForeground(Color.white);
        cLabel3.setOpaque(true);
        cLabel3.setPreferredSize(new Dimension(42, 15));
        cLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        cLabel3.setText("Insertar Datos Venta en \'ven_angulo_iber\"");
        cLabel3.setBounds(new Rectangle(17, 4, 282, 14));
        Pconsulta.setLayout(null);
        feciniE1.setBounds(new Rectangle(178, 5, 79, 18));
        feciniE1.setText("27-04-2003");
        cLabel4.setBounds(new Rectangle(119, 5, 55, 18));
        cLabel4.setText("De Fecha");
        cLabel5.setBounds(new Rectangle(265, 6, 19, 17));
        cLabel5.setText("A");
        fecfinE1.setBounds(new Rectangle(282, 5, 83, 20));
        fecfinE1.setText("31-05-2003");
        Bacecon.setBounds(new Rectangle(179, 43, 105, 26));
        Bacecon.setText("Aceptar");
        Pconsulta.setDefButton(Bacecon);
        Pconsulta.setDefButtonDisable(false);
        cPanel2.setBorder(BorderFactory.createLoweredBevelBorder());
        cPanel2.setText("cPanel2");
        cPanel2.setBounds(new Rectangle(15, 89, 434, 125));
        cPanel2.setLayout(null);
        cLabel6.setText("Datos Raul Angulo");
        cLabel6.setBounds(new Rectangle(8, 11, 105, 16));
        nombficE.setText("/home/cpuente/documentos/angulo/junio/ventas.csv");
        nombficE.setBounds(new Rectangle(9, 31, 391, 18));
        Bcarfic.setBounds(new Rectangle(159, 73, 132, 27));
        Bcarfic.setText("Cargar Fichero");
        Bbusfic.setBounds(new Rectangle(405, 28, 22, 23));
        Bbusfic.setMargin(new Insets(0, 0, 0, 0));
        Bbusfic.setText("");
        cPanel3.setBorder(BorderFactory.createEtchedBorder());
        cPanel3.setBounds(new Rectangle(16, 221, 440, 83));
        cPanel3.setLayout(null);
        cLabel7.setBounds(new Rectangle(11, 5, 86, 17));
        cLabel7.setText("Tarifa a cargar");
        Bcarfic1.setText("Cargar Fichero");
        Bcarfic1.setBounds(new Rectangle(156, 48, 132, 27));
        nombficE1.setBounds(new Rectangle(9, 27, 391, 18));
        nombficE1.setText("/home/cpuente/documentos/angulo/junio/tarifa.txt");
        Bbusfic1.setText("");
        Bbusfic1.setMargin(new Insets(0, 0, 0, 0));
        Bbusfic1.setBounds(new Rectangle(406, 23, 22, 23));
        v.add("Num.Alb");
        v.add("Fec.Albaran");
        v.add("Cliente");
        v.add("Nombre Cliente");
        v.add("Producto");
        v.add("Nombre Producto");
        v.add("K.Venta");
        v.add("Pr.Venta");
        v.add("Pr.Tarifa");
        jt.setCabecera(v);
        jt.setAnchoColumna(new int[] { 54, 78, 52, 185, 50, 167, 45, 42, 62 });
        jt.setCellEditable(true, 8);
        jt.setBounds(new Rectangle(6, 5, 701, 290));
        this.getContentPane().add(statusBar, BorderLayout.SOUTH);
        this.getContentPane().add(TPentra, BorderLayout.CENTER);
        TPentra.add("Actualizar", Pprinc);
        TPentra.add(Pconsulta, "Consulta");
        Pconsulta.add(fecfinE1, null);
        Pconsulta.add(cLabel4, null);
        Pconsulta.add(feciniE1, null);
        Pconsulta.add(cLabel5, null);
        Pconsulta.add(Bacecon, null);
        Pconsulta.add(cPanel2, null);
        cPanel2.add(cLabel6, null);
        cPanel2.add(nombficE, null);
        cPanel2.add(Bbusfic, null);
        cPanel2.add(Bcarfic, null);
        Pconsulta.add(cPanel3, null);
        cPanel3.add(cLabel7, null);
        cPanel3.add(nombficE1, null);
        cPanel3.add(Bbusfic1, null);
        cPanel3.add(Bcarfic1, null);
        Pprinc.add(cPanel1, null);
        PintrDatos.add(Baceptar, null);
        PintrDatos.add(feciniE, null);
        PintrDatos.add(fecfinE, null);
        PintrDatos.add(cLabel2, null);
        PintrDatos.add(cLabel1, null);
        PintrDatos.add(cLabel3, null);
        Pprinc.add(cPanel1, null);
        cPanel1.add(BactTar, null);
        cPanel1.add(Bsintar, null);
        cPanel1.add(jt, null);
        Pprinc.add(PintrDatos, null);
    }

    public void iniciarVentana() throws Exception {
        BactTar.setEnabled(false);
        activarEventos();
        statusBar.setEnabled(true);
        this.setEnabled(true);
    }

    void activarEventos() {
        Baceptar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Baceptar_actionPerformed(e);
            }
        });
        Bsintar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Bsintar_actionPerformed(e);
            }
        });
        BactTar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                BactTar_actionPerformed(e);
            }
        });
        Bacecon.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Bacecon_actionPerformed(e);
            }
        });
        Bbusfic.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Bbusfic_actionPerformed(e);
            }
        });
        Bbusfic1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Bbusfic1_actionPerformed(e);
            }
        });
        Bcarfic.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Bcarfic_actionPerformed(e);
            }
        });
        Bcarfic1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Bcarfic1_actionPerformed(e);
            }
        });
    }

    void Bacecon_actionPerformed(ActionEvent ee) {
        try {
            new licomven(EU, ct, feciniE1.getText(), fecfinE1.getText());
        } catch (Exception k) {
            k.printStackTrace();
        }
    }

    void Baceptar_actionPerformed(ActionEvent e) {
        String s;
        try {
            s = "SELECT * FROM ven_angulo_iber where ran_fecalb >= '" + feciniE.getFechaDB() + "' and ran_fecalb <= '" + fecfinE.getFechaDB() + "'";
            rs = st1.executeQuery(s);
            if (rs.next()) {
                if (mensajes.mensajeYesNo("Hay registros en estas fechas\n nBorrarlos?") != mensajes.YES) {
                    mensajeErr("Ventas Angulo IberRioja ... NO creadas");
                    return;
                }
            }
            s = "DELETE FROM ven_angulo_iber where ran_fecalb >= '" + feciniE.getFechaDB() + "' and ran_fecalb <= '" + fecfinE.getFechaDB() + "'";
            st1.executeUpdate(s);
            s = "SELECT ac.emp_codi,ac.avc_ano,ac.emp_codi,ac.cli_codi, cl.cli_nomb,cl.cli_direc,cl.cli_pobl,ac.avc_serie,ac.avc_nume," + "al.avl_numlin,ac.avc_fecalb,al.pro_codi,al.avl_canti,al.avl_prbase,al.avl_prven," + "ac.avc_dtopp,cl.cli_zonrep from v_albavec ac,v_albavel al,v_cliente cl where " + " avc_fecalb >= '" + feciniE.getFechaDB() + "' and avc_fecalb <= '" + fecfinE.getFechaDB() + "'" + " and cli_zonrep like 'A%' AND cl.cli_activ ='S' and cl.cli_codi=ac.cli_codi " + " and ac.avc_serie = al.avc_serie" + " and ac.avc_nume = al.avc_nume " + " and ac.avc_ano = al.avc_ano " + " and ac.emp_codi = al.emp_codi " + " ORDER BY ac.avc_serie,ac.avc_nume,al.avl_numlin,al.avl_prven";
            rs = st.executeQuery(s);
            if (!rs.next()) {
                mensajes.mensajeAviso("NO encontrados Albaranes");
                return;
            }
            String alcSerie = rs.getString("avc_serie");
            int alcNumalb = rs.getInt("avc_nume");
            java.util.Date alcFecalb = rs.getDate("avc_fecalb");
            int cliCodi = rs.getInt("cli_codi");
            int proCodi = rs.getInt("pro_codi");
            double prVen = rs.getDouble("avl_prven");
            int numLin = rs.getInt("avl_numlin");
            int ranAno = rs.getInt("avc_ano");
            int empCodi = rs.getInt("emp_codi");
            String zonRep = rs.getString("cli_zonrep");
            double kgVen = 0;
            do {
                if (rs.getInt("pro_codi") == proCodi && prVen == rs.getDouble("avl_prven")) kgVen = kgVen + rs.getDouble("avl_canti"); else {
                    insRegistro(alcSerie, alcNumalb, alcFecalb, cliCodi, proCodi, prVen, kgVen, numLin, ranAno, empCodi, zonRep);
                    alcSerie = rs.getString("avc_serie");
                    alcNumalb = rs.getInt("avc_nume");
                    alcFecalb = rs.getDate("avc_fecalb");
                    cliCodi = rs.getInt("cli_codi");
                    proCodi = rs.getInt("pro_codi");
                    prVen = rs.getDouble("avl_prven");
                    kgVen = rs.getDouble("avl_canti");
                    numLin = rs.getInt("avl_numlin");
                    ranAno = rs.getInt("avc_ano");
                    empCodi = rs.getInt("emp_codi");
                    zonRep = rs.getString("cli_zonrep");
                }
            } while (rs.next());
            insRegistro(alcSerie, alcNumalb, alcFecalb, cliCodi, proCodi, prVen, kgVen, numLin, ranAno, empCodi, zonRep);
            ct.commit();
        } catch (Exception k) {
            fatalError("Error al Buscar Datos de Venta ", k);
            try {
                ct.rollback();
            } catch (Exception k1) {
            }
            return;
        }
        mensajeErr("Datos procesados ...");
    }

    void insRegistro(String alcSerie, int alcNumalb, java.util.Date alcFecalb, int cliCodi, int proCodi, double prVen, double kgVen, int numlin, int ranAno, int empCodi, String zonrep) throws Exception {
        String s;
        boolean swSinTar;
        double tarPreci;
        String proNomb;
        String fecha = "";
        fecha = Formatear.fechaDB(alcFecalb);
        s = " SELECT pro_nomb, tar_preci " + " FROM c_Tarifa where pro_codi = " + proCodi + " AND tar_fecini <=  '" + fecha + "' AND tar_fecfin>=  '" + fecha + "'";
        rs1 = st1.executeQuery(s);
        if (!rs1.next()) {
            swSinTar = true;
            tarPreci = 0;
            s = "select  pro_nomb from v_articulo where pro_codi = " + proCodi;
            rs1 = st1.executeQuery(s);
            if (!rs1.next()) {
                mensajes.mensajeUrgente("No encontrado articulo " + proCodi + " en tabla articulos ");
                return;
            }
            proNomb = rs1.getString("pro_nomb");
        } else {
            swSinTar = false;
            tarPreci = rs1.getDouble("tar_preci");
            proNomb = rs1.getString("pro_nomb");
            if (!zonrep.equals("A6")) tarPreci += 0.15;
        }
        s = "insert into ven_angulo_iber values(" + empCodi + "," + ranAno + "," + "'" + alcSerie + "'," + alcNumalb + "," + numlin + "," + "'" + fecha + "','" + cliCodi + "','" + proCodi + "','" + proNomb + "','" + kgVen + "','" + prVen + "'," + (swSinTar ? null : "" + tarPreci) + "," + "'N')";
        st1.executeUpdate(s);
    }

    public void matar(boolean mat) {
        try {
            if (mat) {
                ct.close();
            }
        } catch (Exception k) {
            k.printStackTrace();
        }
        super.matar(false);
    }

    void Bsintar_actionPerformed(ActionEvent e) {
        String s = "SELECT cli_nomb,v.* FROM VEN_ANGULO_IBER v,v_cliente c where v.cli_codi =c.cli_codi " + " and ran_prcom IS null" + " order by pro_codi,ran_fecalb";
        try {
            jt.removeAllDatos();
            rs = st.executeQuery(s);
            if (!rs.next()) {
                mensajeErr("Todas las ventas tienen precio de Tarifa");
                return;
            }
            jt.setEnabled(false);
            do {
                Vector v = new Vector();
                v.addElement(rs.getString("ran_numalb"));
                v.addElement(Formatear.getFechaVer(rs.getDate("ran_fecalb")));
                v.addElement(rs.getString("cli_codi"));
                v.addElement(rs.getString("cli_nomb"));
                v.addElement(rs.getString("pro_codi"));
                v.addElement(rs.getString("pro_nomb"));
                v.addElement(rs.getString("ran_kilven"));
                v.addElement(rs.getString("ran_prven"));
                v.add("");
                jt.addLinea(v);
            } while (rs.next());
            jt.setEnabled(true);
            jt.requestFocusInicio();
            BactTar.setEnabled(true);
        } catch (Exception k) {
            fatalError("Error al cargar el grid" + k);
        }
    }

    void BactTar_actionPerformed(ActionEvent e) {
        int nUpd;
        double prTar = 0;
        try {
            for (int n = 0; n < jt.getRowCount(); n++) {
                if (jt.getValString(n, 8, true).equals("")) continue;
                try {
                    prTar = Double.parseDouble(jt.getValString(n, 8));
                } catch (Exception k1) {
                    continue;
                }
                String s = "update  ven_angulo_iber set ran_prcom = " + prTar + " where" + " ran_numalb = " + jt.getValorInt(n, "Num.Alb") + " and ran_fecalb = '" + Formatear.getFechaDB(jt.getValString(n, "Fec.Albaran")) + "'" + " and cli_codi = " + jt.getValorInt(n, "Cliente") + " and pro_codi = " + jt.getValorInt(n, "Producto") + " and ran_kilven = " + jt.getValString(n, "K.Venta") + " and ran_prven = " + jt.getValString(n, "Pr.Venta") + " and ran_prcom IS null ";
                nUpd = st.executeUpdate(s);
                if (nUpd != 1) throw new Exception("NO encontrado Registro: (" + nUpd + ")+\n" + s);
            }
            ct.commit();
            mensajeErr("Datos Modificados...");
            BactTar.setEnabled(false);
        } catch (Exception ex) {
            fatalError("Error al  Actualizar tarifa" + ex, ex);
        }
    }

    void Bbusfic_actionPerformed(ActionEvent e) {
        try {
            configurarFile();
            int returnVal = ficeleE.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                nombficE.setText(ficeleE.getSelectedFile().getAbsolutePath());
            }
        } catch (Exception k) {
            fatalError("error al elegir el fichero", k);
        }
    }

    void Bbusfic1_actionPerformed(ActionEvent e) {
        try {
            configurarFile();
            int returnVal = ficeleE.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                nombficE1.setText(ficeleE.getSelectedFile().getAbsolutePath());
            }
        } catch (Exception k) {
            fatalError("error al elegir el fichero", k);
        }
    }

    void Bcarfic_actionPerformed(ActionEvent e) {
        nLin = 0;
        new miThread("xxd") {

            public void run() {
                try {
                    fecfor = "dd/MM/yyyy";
                    tabla = "ven_angulo_ang";
                    utSq.load(nombficE.getText(), tabla, dtBloq, st1, "dd-MM-yyyy");
                    msgBox("Insertadas " + nLin + " Lineas");
                    ct.commit();
                } catch (Throwable ex) {
                    fatalError("Error al Cargar Fichero", ex);
                }
            }
        };
    }

    void Bcarfic1_actionPerformed(ActionEvent e) {
        nLin = 0;
        new miThread("xxd") {

            public void run() {
                try {
                    fecfor = "dd/MM/yyyy";
                    this.setPriority(Thread.MAX_PRIORITY);
                    tabla = "c_tarifa";
                    utSq.load(nombficE1.getText(), tabla, dtBloq, st1, "dd-MM-yyyy");
                    msgBox("Insertadas " + nLin + " Lineas");
                    ct.commit();
                } catch (Throwable ex) {
                    fatalError("Error al Cargar Fichero", ex);
                }
            }
        };
    }

    void configurarFile() throws Exception {
        if (ficeleE != null) return;
        ficeleE = new JFileChooser();
        ficeleE.setName("Abrir Fichero");
        ficeleE.setCurrentDirectory(new java.io.File("d:/"));
    }

    void procesLin(Vector v) {
        if (muerto) return;
        String s;
        s = "SELECT * FROM " + tabla;
        String valor;
        try {
            dt.select(s);
            s = "INSERT INTO " + tabla + " VALUES (";
            for (int n = 0; n < v.size(); n++) {
                valor = v.elementAt(n).toString();
                if (utilSql.getTipo(dt.getTipCampo(n)) == Types.CHAR) s += "'" + valor + "',"; else if (utilSql.getTipo(dt.getTipCampo(n)) == Types.DECIMAL) {
                    valor = valor.replace(',', '.');
                    try {
                        Double.parseDouble(valor);
                    } catch (NumberFormatException k) {
                        valor = "null";
                    }
                    s += valor + ",";
                } else {
                    if (valor.trim().equals("")) s += "null,"; else {
                        if (valor.length() > 10) valor = valor.substring(0, 10);
                        s += "TO_DATE('" + valor + "','" + fecfor + "'),";
                    }
                }
            }
            s = s.substring(0, s.length() - 1) + ")";
            dtBloq.executeUpdate(s);
            nLin++;
        } catch (Exception k) {
            fatalError("error al insertar datos\n" + s, k);
            muerto = true;
            return;
        }
    }

    void procesLin1(Vector v) {
        String s = "SELECT * FROM ven_angulo_iber";
        String valor;
        try {
            rs = st.executeQuery(s);
            rs.next();
            s = "INSERT INTO ven_angulo_iber VALUES(";
            for (int n = 0; n < v.size(); n++) {
                valor = v.elementAt(n).toString();
                if (utilSql.getTipo(rs.getMetaData().getColumnType(n + 1)) == Types.CHAR) s += "'" + valor + "',"; else if (utilSql.getTipo(rs.getMetaData().getColumnType(n + 1)) == Types.DECIMAL) s += valor + ","; else {
                    s += "'" + valor + "',";
                }
            }
            s = s.substring(0, s.length() - 1) + ")";
            ct.createStatement().executeUpdate(s);
            nLin++;
        } catch (Exception k) {
            fatalError("error al insertar datos\n" + s, k);
            return;
        }
    }

    void procesLin2(Vector v) {
        if (muerto) return;
        String s = "SELECT * FROM albaran_lin";
        String valor;
        try {
            dt.select(s);
            dt.addNew();
            for (int n = 0; n < v.size(); n++) {
                if (nLin % 20 == 0) mensaje("Insertadas " + nLin + " Lineas", false);
                valor = v.elementAt(n).toString();
                if (dt.getTipCampo(n) == Types.DATE) {
                    dt.setDato(n, valor, "dd-MM-yyyy");
                } else {
                    if (valor.equals("")) {
                        dt.setDato(n, null);
                    } else {
                        dt.setDato(n, valor);
                    }
                }
            }
            dt.update(st);
            nLin++;
        } catch (Exception k) {
            fatalError("error al insertar datos", k);
            return;
        }
    }
}
