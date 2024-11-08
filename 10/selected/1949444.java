package gnu.chu.anjelica.ventas;

import gnu.chu.controles.*;
import gnu.chu.utilidades.*;
import gnu.chu.sql.*;
import java.sql.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * <p>T�tulo: sepAlbVen </p>
 * <p>Descripci�n: Separar Albaranes de Ventas. Este clase es invocada desde
 * el mantenimiento de albaranes de ventas y se encarga de convertir un albaran en varios.
 * Utilizado sobre todo para albaranes a particulares cuyo importe no puede ser superior a un limite
 * que marca la ley</p>
 * <p>Copyright: Copyright (c) 2005
 *  Este programa es software libre. Puede redistribuirlo y/o modificarlo bajo
 *  los t�rminos de la Licencia P�blica General de GNU seg�n es publicada por
 *  la Free Software Foundation, bien de la versi�n 2 de dicha Licencia
 *  o bien (seg�n su elecci�n) de cualquier versi�n posterior.
 *  Este programa se distribuye con la esperanza de que sea �til,
 *  pero SIN NINGUNA GARANT�A, incluso sin la garant�a MERCANTIL impl�cita
 *  o sin garantizar la CONVENIENCIA PARA UN PROP�SITO PARTICULAR.
 *  V�ase la Licencia P�blica General de GNU para m�s detalles.
 *  Deber�a haber recibido una copia de la Licencia P�blica General junto con este programa.
 *  Si no ha sido as�, escriba a la Free Software Foundation, Inc.,
 *  en 675 Mass Ave, Cambridge, MA 02139, EEUU.
 * </p>
 * @author chuchiP
 *
 *
 */
public class sepAlbVen extends ventana {

    double impLinea = 0;

    int numDec = 2;

    int nLiAlb;

    int nLiDes;

    int nLiJT;

    int numAlb = 0;

    boolean swRotoAlb;

    boolean swRotoLin;

    double impAcum = 0;

    double kLiAlb;

    int uLiAlb;

    actCabAlbFra datCab;

    CPanel Pprinc = new CPanel();

    CPanel PdatAlb = new CPanel();

    CTextField avc_seriE = new CTextField();

    CTextField avc_numeE = new CTextField(Types.DECIMAL, "#####9");

    CLabel cLabel4 = new CLabel();

    CTextField avc_anoE = new CTextField(Types.DECIMAL, "###9");

    CLabel cLabel1 = new CLabel();

    CLabel Clabel1 = new CLabel();

    CTextField emp_codiE = new CTextField(Types.DECIMAL, "#9");

    CLabel cLabel2 = new CLabel();

    CTextField avc_fecalbE = new CTextField(Types.DATE, "dd-MM-yyyy");

    CLabel cLabel10 = new CLabel();

    CTextField impLinE = new CTextField(Types.DECIMAL, "----,--9.99");

    CTextField kilosE = new CTextField(Types.DECIMAL, "----,--9.99");

    CLabel cLabel9 = new CLabel();

    CPanel Pcond = new CPanel();

    CLabel cLabel3 = new CLabel();

    CTextField impMaxE = new CTextField(Types.DECIMAL, "----,--9.99");

    CLabel cLabel5 = new CLabel();

    CTextField intFechasE = new CTextField(Types.DECIMAL, "9.9");

    CLabel cLabel6 = new CLabel();

    CTextField avc_feciniE = new CTextField(Types.DATE, "dd-MM-yyyy");

    CButton Bcalcular = new CButton();

    CGridEditable jt = new CGridEditable(3) {

        public void afterCambiaLinea() {
            albImpE.setEnabled(getSelectedRow() + 1 != getRowCount());
        }

        public int cambiaLinea(int row, int col) {
            return checkLinea(row);
        }
    };

    CTextField albNumE = new CTextField(Types.DECIMAL, "#####9");

    CTextField albFecE = new CTextField(Types.DATE, "dd-MM-yyyy");

    CTextField albImpE = new CTextField(Types.DECIMAL, "----,--9.99");

    CButton Baceptar = new CButton("Aceptar", Iconos.getImageIcon("check"));

    DatosTabla dtAdd = new DatosTabla();

    GridBagLayout gridBagLayout1 = new GridBagLayout();

    public sepAlbVen(EntornoUsuario eu) {
        EU = eu;
        try {
            setTitulo("Desglosar Albaranes Ventas");
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
            setErrorInit(true);
        }
    }

    private void jbInit() throws Exception {
        iniciarFrame();
        this.setSize(new Dimension(575, 492));
        setVersion("2006-03-17");
        statusBar = new StatusBar(this);
        this.getContentPane().add(statusBar, BorderLayout.SOUTH);
        this.getContentPane().add(Pprinc, BorderLayout.CENTER);
        Pprinc.setLayout(gridBagLayout1);
        Baceptar.setMaximumSize(new Dimension(115, 32));
        Baceptar.setMinimumSize(new Dimension(115, 32));
        Baceptar.setPreferredSize(new Dimension(115, 32));
        Baceptar.setMargin(new Insets(0, 0, 0, 0));
        jt.setMaximumSize(new Dimension(416, 284));
        jt.setMinimumSize(new Dimension(416, 284));
        jt.setPreferredSize(new Dimension(416, 284));
        Pcond.setMaximumSize(new Dimension(566, 30));
        Pcond.setMinimumSize(new Dimension(566, 30));
        Pcond.setPreferredSize(new Dimension(566, 30));
        PdatAlb.setMaximumSize(new Dimension(451, 42));
        PdatAlb.setMinimumSize(new Dimension(451, 42));
        PdatAlb.setPreferredSize(new Dimension(451, 42));
        Pprinc.add(jt, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 0, 0, 0), 0, 0));
        Pprinc.add(Baceptar, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        Pprinc.add(Pcond, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        Pprinc.add(PdatAlb, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        Bcalcular.setBounds(new Rectangle(472, 3, 91, 24));
        intFechasE.setBounds(new Rectangle(435, 4, 32, 17));
        cLabel5.setBounds(new Rectangle(346, 4, 97, 17));
        avc_feciniE.setBounds(new Rectangle(258, 4, 75, 17));
        cLabel6.setBounds(new Rectangle(192, 4, 71, 17));
        impMaxE.setBounds(new Rectangle(97, 4, 77, 17));
        cLabel3.setBounds(new Rectangle(4, 4, 97, 17));
        PdatAlb.setBorder(BorderFactory.createLoweredBevelBorder());
        PdatAlb.setLayout(null);
        Pcond.setLayout(null);
        cLabel4.setText("Fec.Alb");
        cLabel4.setBounds(new Rectangle(317, 4, 49, 16));
        cLabel1.setText("A�o");
        cLabel1.setBounds(new Rectangle(90, 4, 24, 16));
        Clabel1.setText("Empresa");
        Clabel1.setBounds(new Rectangle(4, 4, 51, 16));
        cLabel2.setText("Albaran");
        cLabel2.setBounds(new Rectangle(156, 4, 47, 16));
        cLabel10.setText("Imp. Lineas");
        cLabel10.setBounds(new Rectangle(295, 21, 65, 17));
        cLabel9.setText("Kilos");
        cLabel9.setBounds(new Rectangle(5, 21, 31, 17));
        impLinE.setBounds(new Rectangle(357, 21, 84, 17));
        kilosE.setBounds(new Rectangle(41, 21, 61, 17));
        avc_fecalbE.setBounds(new Rectangle(365, 4, 75, 16));
        avc_numeE.setBounds(new Rectangle(237, 4, 65, 16));
        avc_seriE.setBounds(new Rectangle(207, 4, 28, 16));
        avc_anoE.setBounds(new Rectangle(112, 4, 38, 16));
        emp_codiE.setBounds(new Rectangle(54, 4, 33, 16));
        Pcond.setBorder(BorderFactory.createRaisedBevelBorder());
        impMaxE.setValorDec(83);
        intFechasE.setValorInt(0);
        cLabel3.setText("Importe Maximo");
        cLabel5.setText("Intervalo Fechas");
        cLabel6.setRequestFocusEnabled(true);
        cLabel6.setText("Fecha Inicio");
        Bcalcular.setMargin(new Insets(0, 0, 0, 0));
        Bcalcular.setText("Calcular");
        PdatAlb.add(avc_fecalbE, null);
        PdatAlb.add(avc_seriE, null);
        PdatAlb.add(cLabel4, null);
        PdatAlb.add(avc_anoE, null);
        PdatAlb.add(cLabel1, null);
        PdatAlb.add(Clabel1, null);
        PdatAlb.add(emp_codiE, null);
        PdatAlb.add(cLabel2, null);
        PdatAlb.add(impLinE, null);
        PdatAlb.add(cLabel10, null);
        PdatAlb.add(kilosE, null);
        PdatAlb.add(cLabel9, null);
        PdatAlb.add(avc_numeE, null);
        Pcond.add(cLabel3, null);
        Pcond.add(impMaxE, null);
        Pcond.add(cLabel6, null);
        Pcond.add(avc_feciniE, null);
        Pcond.add(intFechasE, null);
        Pcond.add(Bcalcular, null);
        Pcond.add(cLabel5, null);
        PdatAlb.setEnabled(false);
        confGrid();
        jt.setCanDeleteLinea(false);
        jt.setCanInsertLinea(false);
        activarEventos();
    }

    private void activarEventos() {
        Bcalcular.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Bcalcular_actionPerformed();
            }
        });
        Baceptar.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Baceptar_actionPerformed();
            }
        });
    }

    public String iniciar(DatosTabla dtCon1, DatosTabla dtStat, DatosTabla dtAdd, int empCodi, int ejeNume, String avcSerie, int avcNume, double kilos, double importe) throws Exception {
        if (datCab == null) datCab = new actCabAlbFra(dtCon1, dtAdd);
        this.dtCon1 = dtCon1;
        this.dtAdd = dtAdd;
        this.dtStat = dtStat;
        emp_codiE.setValorDec(empCodi);
        avc_anoE.setValorInt(ejeNume);
        avc_seriE.setText(avcSerie);
        avc_numeE.setValorInt(avcNume);
        jt.setEnabled(false);
        Baceptar.setEnabled(true);
        Bcalcular.setEnabled(true);
        jt.removeAllDatos();
        String s;
        s = "SELECT PR.pro_codi FROM V_ALBAVEl as l,v_articulo as pr  " + " WHERE l.avc_ano = " + ejeNume + " and l.emp_codi = " + empCodi + " and l.avc_serie = '" + avcSerie + "'" + " and l.avc_nume = " + avcNume + " and pr.emp_codi = " + empCodi + " and pr.pro_codi = l.pro_codi " + " and pr.pro_tiplot != 'V'";
        if (dtStat.select(s)) return "SOLO SE PUEDEN DESGLOSAR ALBARANES SIN LINEAS DE COMENTARIO";
        s = "SELECT c.*,cl.cli_exeiva,cl.cli_recequ FROM V_ALBAVEC as c, v_cliente as cl " + " WHERE avc_ano = " + ejeNume + " and c.emp_codi = " + empCodi + " and c.avc_serie = '" + avcSerie + "'" + " and c.avc_nume = " + avcNume + " and cl.emp_codi = " + empCodi + " and cl.cli_codi = c.cli_codi ";
        if (!dtStat.select(s)) return "ALBARAN ... NO ENCONTRADO";
        if (!setBloqueo(dtAdd, "V_albavec", ejeNume + "|" + empCodi + "|" + avcSerie + "|" + avcNume)) return msgBloqueo;
        avc_fecalbE.setText(dtStat.getFecha("avc_fecalb", "dd-MM-yyyy"));
        kilosE.setValorDec(kilos);
        impLinE.setValorDec(importe);
        avc_feciniE.setText(avc_fecalbE.getText());
        return null;
    }

    private void confGrid() throws Exception {
        Vector v = new Vector();
        v.addElement("Num.Albaran");
        v.addElement("Fecha Alb.");
        v.addElement("Imp.Albar");
        jt.setCabecera(v);
        jt.setAnchoColumna(new int[] { 80, 100, 100 });
        jt.setAlinearColumna(new int[] { 2, 1, 2 });
        jt.setFormatoColumna(0, "#####9");
        jt.setFormatoColumna(2, "----,--9.99");
        albNumE.setEnabled(false);
        albFecE.setEnabled(false);
        Vector v1 = new Vector();
        v1.addElement(albNumE);
        v1.addElement(albFecE);
        v1.addElement(albImpE);
        jt.setCampos(v1);
    }

    private boolean checkCabe() throws Exception {
        if (impMaxE.getValorDec() == 0) {
            mensajeErr("Introduzca Una cantidad Maxima");
            impMaxE.requestFocus();
            return false;
        }
        if (avc_feciniE.isNull() || avc_feciniE.getError()) {
            mensajeErr("Fecha Inicio NO es valida");
            avc_feciniE.requestFocus();
            return false;
        }
        if (!checkEjercicio(avc_anoE.getValorInt(), avc_feciniE.getDate())) {
            avc_feciniE.requestFocus();
            return false;
        }
        return true;
    }

    private void Bcalcular_actionPerformed() {
        try {
            jt.setEnabled(false);
            jt.removeAllDatos();
            if (!checkCabe()) return;
            double impLin = 0;
            double impTot = impLinE.getValorDec();
            java.util.Date fecha = avc_feciniE.getDate();
            String fecStr;
            int nLin = 0;
            while (impTot > 0) {
                impLin = impMaxE.getValorDec();
                if (impTot - impLin < 0) {
                    impLin = impTot;
                    impTot = 0;
                } else {
                    impTot -= impMaxE.getValorDec();
                    impLin = impMaxE.getValorDec();
                }
                fecStr = Formatear.sumaDias(fecha, (int) (intFechasE.getValorDec() * nLin));
                Vector v = new Vector();
                v.addElement("");
                v.addElement(fecStr);
                v.addElement("" + impLin);
                jt.addLinea(v);
                nLin++;
            }
            jt.setEnabled(true);
            jt.requestFocusInicio();
        } catch (Exception k) {
            Error("Error al Calcular importes de Albaran", k);
        }
    }

    private void Baceptar_actionPerformed() {
        try {
            if (!checkCabe()) return;
            if (jt.isVacio()) {
                mensajeErr("Introduzca valores en grid");
                return;
            }
            String s = "SELECT l.*,a.* FROM v_albavel as l, V_albvenpar as a where " + " l.emp_codi = " + emp_codiE.getValorInt() + " and l.avc_ano = " + avc_anoE.getValorInt() + " and l.avc_nume = " + avc_numeE.getValorInt() + " and l.avc_serie = '" + avc_seriE.getText() + "'" + " and a.emp_codi = " + emp_codiE.getValorInt() + " and a.avc_ano = " + avc_anoE.getValorInt() + " and a.avc_serie = '" + avc_seriE.getText() + "'" + " and a.avc_nume = " + avc_numeE.getValorInt() + " and a.avl_numlin = l.avl_numlin" + " order by l.avl_numlin,a.avp_numlin ";
            if (!dtCon1.select(s)) {
                msgBox("Lineas de Albaran NO encontradas");
                mensajeErr("Lineas de Albaran NO encontradas");
                return;
            }
            swRotoAlb = true;
            swRotoLin = true;
            int numLin = dtCon1.getInt("avl_numlin");
            double kilos;
            double impAlb = 0;
            nLiAlb = 0;
            nLiDes = 0;
            impAcum = 0;
            nLiJT = 0;
            double impGrid = jt.getValorDec(nLiJT, 2);
            double acumAlb = 0;
            do {
                if (dtCon1.getInt("avl_numlin") != numLin) {
                    numLin = dtCon1.getInt("avl_numlin");
                    swRotoLin = true;
                }
                impLinea = Formatear.Redondea(dtCon1.getDouble("avl_prven") * dtCon1.getDouble("avp_canti"), numDec);
                while (impLinea > 0 && nLiJT + 1 <= jt.getRowCount()) {
                    if (impAcum + impLinea > impGrid) {
                        impAlb = jt.getValorDec(nLiJT, 2) - impAcum;
                        kilos = Formatear.Redondea(impAlb / dtCon1.getDouble("avl_prven"), numDec);
                        acumAlb += Formatear.Redondea((kilos * dtCon1.getDouble("avl_prven")), numDec);
                        impLinea -= Formatear.Redondea((kilos * dtCon1.getDouble("avl_prven")), numDec);
                        guardaLineaDes(kilos);
                        swRotoAlb = true;
                        swRotoLin = true;
                        nLiJT++;
                        impGrid = jt.getValorDec(nLiJT, 2);
                        impAcum = 0;
                    } else {
                        kilos = Formatear.Redondea(impLinea / dtCon1.getDouble("avl_prven"), numDec);
                        acumAlb += Formatear.Redondea((kilos * dtCon1.getDouble("avl_prven")), numDec);
                        impAcum += Formatear.Redondea((kilos * dtCon1.getDouble("avl_prven")), numDec);
                        impLinea = 0;
                        guardaLineaDes(kilos);
                    }
                }
            } while (dtCon1.next());
            for (int n = 0; n < jt.getRowCount(); n++) {
                jt.setValor("" + actAlbaran(emp_codiE.getValorInt(), avc_anoE.getValorInt(), avc_seriE.getText(), jt.getValorInt(n, 0)), n, 2);
            }
            s = "DELETE FROM v_albavec WHERE avc_ano = " + avc_anoE.getValorInt() + " and emp_codi = " + emp_codiE.getValorInt() + " and avc_serie = '" + avc_seriE.getText() + "'" + " and avc_nume = " + avc_numeE.getValorInt();
            dtAdd.executeUpdate(s);
            s = "DELETE FROM v_albavel WHERE avc_ano = " + avc_anoE.getValorInt() + " and emp_codi = " + emp_codiE.getValorInt() + " and avc_serie = '" + avc_seriE.getText() + "'" + " and avc_nume = " + avc_numeE.getValorInt();
            dtAdd.executeUpdate(s);
            s = "DELETE FROM v_albvenpar WHERE avc_ano = " + avc_anoE.getValorInt() + " and emp_codi = " + emp_codiE.getValorInt() + " and avc_serie = '" + avc_seriE.getText() + "'" + " and avc_nume = " + avc_numeE.getValorInt();
            dtAdd.executeUpdate(s);
            dtAdd.commit();
            mensajeErr("Albaranes .... desglosados");
            jt.setEnabled(false);
            Baceptar.setEnabled(false);
            Bcalcular.setEnabled(false);
        } catch (Exception k) {
            Error("Error al calcular Albaranes", k);
            try {
                dtAdd.rollback();
            } catch (SQLException k1) {
                k1.printStackTrace();
            }
        }
    }

    double actAlbaran(int empCodi, int avcAno, String avcSerie, int avcNume) throws Exception {
        String s = "update v_albavel set avl_canti = " + " (select sum(avp_canti) from v_albvenpar where v_albavel.emp_codi = v_albvenpar.emp_codi " + " and v_albavel.avc_ano= v_albvenpar.avc_ano " + " and v_albavel.avc_serie = v_albvenpar.avc_serie " + " and v_albavel.avc_nume= v_albvenpar.avc_nume " + " and v_albavel.avl_numlin = v_albvenpar.avl_numlin), " + " avl_unid = (select sum(avp_numuni)  " + " from v_albvenpar where v_albavel.emp_codi = v_albvenpar.emp_codi " + " and v_albavel.avc_ano= v_albvenpar.avc_ano " + " and v_albavel.avc_serie = v_albvenpar.avc_serie " + " and v_albavel.avc_nume= v_albvenpar.avc_nume " + " and v_albavel.avl_numlin = v_albvenpar.avl_numlin) " + " where emp_codi = " + empCodi + " and avc_ano= " + avcAno + " and avc_serie = '" + avcSerie + "'" + " and avc_nume= " + avcNume;
        dtAdd.executeUpdate(s);
        datCab.actDatosAlb(empCodi, avcAno, avcSerie, avcNume, dtStat.getInt("cli_exeiva") == 0 && empCodi < 90, dtStat.getDouble("avc_dtopp") + dtStat.getDouble("avc_dtocom"), dtStat.getInt("cli_recequ"));
        if (datCab.getCambioIva()) throw new Exception("ALBARAN " + empCodi + "-" + avcAno + "-" + avcSerie + "/" + avcNume + " ERRONEO ... TIENE TIPOS DE IVA DIFERENTES");
        s = "SELECT * FROM V_albavec WHERE avc_ano =" + avcAno + " and emp_codi = " + empCodi + " and avc_serie = '" + avcSerie + "'" + " and avc_nume = " + avcNume;
        if (!dtAdd.select(s, true)) throw new Exception("No encontrado Cabecera Albaran.\n Select: " + s);
        debug("Albaran: avc_ano =" + avcAno + " and emp_codi = " + empCodi + " and avc_serie = '" + avcSerie + "'" + " and avc_nume = " + avcNume + " Importe: " + datCab.getValDouble("avc_impalb"));
        dtAdd.edit(dtAdd.getCondWhere());
        dtAdd.setDato("avc_impalb", datCab.getValDouble("avc_impalb"));
        dtAdd.update();
        return datCab.getValDouble("avc_impbru");
    }

    void guardaLineaDes(double kilos) throws Exception {
        if (swRotoAlb) guardaCab();
        if (swRotoLin) guardaLin(kilos, uLiAlb);
        nLiDes++;
        debug("Guardando Desglose Lina Albaran: " + numAlb + " Linea: " + nLiAlb + " Linea Despiece: " + nLiDes + " Kilos: " + kilos + " Importe: " + kilos * dtCon1.getDouble("avl_prven") + " Imp. Pendiente: " + impLinea);
        dtAdd.addNew("v_albvenpar");
        dtAdd.setDato("avc_ano", avc_anoE.getValorInt());
        dtAdd.setDato("emp_codi", emp_codiE.getValorInt());
        dtAdd.setDato("avc_serie", avc_seriE.getText());
        dtAdd.setDato("avc_nume", numAlb);
        dtAdd.setDato("avl_numlin", nLiAlb);
        dtAdd.setDato("avp_numlin", nLiDes);
        dtAdd.setDato("pro_codi", dtCon1.getInt("pro_codi"));
        dtAdd.setDato("avp_tiplot", dtCon1.getString("avp_tiplot"));
        dtAdd.setDato("avp_ejelot", dtCon1.getInt("avp_ejelot"));
        dtAdd.setDato("avp_emplot", dtCon1.getInt("avp_emplot"));
        dtAdd.setDato("avp_serlot", dtCon1.getString("avp_serlot"));
        dtAdd.setDato("avp_numpar", dtCon1.getInt("avp_numpar"));
        dtAdd.setDato("avp_numind", dtCon1.getInt("avp_numind"));
        dtAdd.setDato("avp_numuni", 1);
        dtAdd.setDato("avp_canti", kilos);
        dtAdd.update();
        kLiAlb += kilos;
    }

    void guardaCab() throws Exception {
        nLiAlb = 0;
        nLiDes = 0;
        numAlb = pdalbara.getNumAlb(true, dtAdd, emp_codiE.getValorInt(), avc_anoE.getValorInt(), avc_seriE.getText());
        debug("... Nueva cabecera ...");
        debug("Guardando Cabecera Albaran: " + numAlb + " Linea Grid " + nLiJT);
        java.util.Date fecha = Formatear.getDate(jt.getValString(nLiJT, 1), "dd-MM-yyyy");
        dtAdd.addNew("v_albavec");
        dtAdd.setDato(dtStat);
        dtAdd.setDato("avc_ano", avc_anoE.getValorInt());
        dtAdd.setDato("emp_codi", emp_codiE.getValorInt());
        dtAdd.setDato("avc_serie", avc_seriE.getText());
        dtAdd.setDato("avc_nume", numAlb);
        dtAdd.setDato("avc_fecalb", fecha);
        dtAdd.update();
        jt.setValor("" + numAlb, nLiJT, 0);
        swRotoAlb = false;
        nLiAlb = 0;
    }

    void guardaLin(double kilos, int unid) throws Exception {
        nLiAlb++;
        debug("Guardando Linea Albaran " + nLiAlb + " DE Albaran: " + numAlb);
        dtAdd.addNew("v_albavel");
        dtAdd.setDato(dtCon1);
        dtAdd.setDato("avc_ano", avc_anoE.getValorInt());
        dtAdd.setDato("emp_codi", emp_codiE.getValorInt());
        dtAdd.setDato("avc_serie", avc_seriE.getText());
        dtAdd.setDato("avc_nume", numAlb);
        dtAdd.setDato("avl_numlin", nLiAlb);
        dtAdd.setDato("avl_unid", unid);
        dtAdd.setDato("avl_canbru", kilos);
        dtAdd.setDato("avl_canti", kilos);
        dtAdd.update();
        swRotoLin = false;
        kLiAlb = 0;
    }

    boolean checkEjercicio(int ejercicio, java.util.Date fecha) {
        return true;
    }

    public int checkLinea(int row) {
        if (albImpE.getValorDec() > impMaxE.getValorDec()) {
            mensajeErr("Importe Albaran NO Puede ser superior al Maximo");
            return 2;
        }
        if (albImpE.getValorDec() <= 5) {
            mensajeErr("Importe Albaran NO Puede ser inferior a 5 Euros");
            return 2;
        }
        double impAcum = 0;
        for (int n = 0; n < jt.getRowCount() - 1; n++) {
            if (n == row) impAcum += albImpE.getValorDec(); else impAcum += jt.getValorDec(n, 2);
        }
        jt.setValor("" + (impLinE.getValorDec() - impAcum), jt.getRowCount() - 1, 2);
        return -1;
    }
}
