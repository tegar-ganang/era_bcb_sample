package gnu.chu.anjelica.despiece;

import gnu.chu.controles.*;
import gnu.chu.utilidades.*;
import gnu.chu.sql.*;
import java.sql.*;
import gnu.chu.Menu.*;
import java.awt.*;
import java.util.*;
import javax.swing.BorderFactory;
import gnu.chu.camposdb.*;
import gnu.chu.interfaces.*;
import javax.swing.SwingConstants;
import java.awt.event.ActionEvent;
import javax.swing.event.*;
import java.awt.event.*;

/**
 *
 * <p>Titulo: valdespi2</p>
 * <p>Descripcion: Valoracion de Despieces Version 2.0</p>
 * <p>Copyright: Copyright (c) 2005</p>
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
* <p>Empresa: MISL</p>
* @author chuchiP
* @version 1.0
*/
public class valdespi2 extends ventanaPad implements PAD {

    boolean swVerCab = false;

    boolean DEBUG = false;

    int numDesAnt = 0;

    double precAcu = 0;

    double cantiAcu = 0;

    int tirCodi;

    boolean swMsgBox = false;

    String s;

    CTextField pro_codi1E = new CTextField();

    CTextField pro_nombE = new CTextField(Types.CHAR, "X", 50);

    CTextField prp_anoE = new CTextField(Types.DECIMAL, "###9");

    CTextField prp_serieE = new CTextField(Types.CHAR, "X", 1);

    CTextField def_partE = new CTextField(Types.DECIMAL, "###9");

    CTextField def_indiE = new CTextField(Types.DECIMAL, "##9");

    CTextField def_pesoE = new CTextField(Types.DECIMAL, "---,--9.99");

    CTextField def_prcostE = new CTextField(Types.DECIMAL, "---,--9.99");

    CPanel cPanel1 = new CPanel();

    CPanel Pcabe = new CPanel();

    CLabel cLabel1 = new CLabel();

    CTextField feulinE = new CTextField(Types.DATE, "dd-MM-yyyy");

    CLabel deo_fechaL = new CLabel();

    CTextField deo_fechaE = new CTextField(Types.DATE, "dd-MM-yyyy");

    CLabel cLabel2 = new CLabel();

    CTextField deo_codiE = new CTextField(Types.DECIMAL, "-----9");

    CPanel cPanel2 = new CPanel();

    CLabel cLabel3 = new CLabel();

    CTextField tid_codiE = new CTextField(Types.DECIMAL, "##9");

    CLabel cLabel4 = new CLabel();

    proPanel pro_codiE = new proPanel();

    CLabel cLabel6 = new CLabel();

    CTextField pro_numindE = new CTextField(Types.DECIMAL, "##9");

    CLabel cLabel8 = new CLabel();

    CTextField pro_loteE = new CTextField(Types.DECIMAL, "####9");

    CTextField deo_serlotE = new CTextField(Types.CHAR, "X");

    CLabel cLabel9 = new CLabel();

    CTextField deo_emplotE = new CTextField(Types.DECIMAL, "#9");

    CLabel cLabel7 = new CLabel();

    CTextField deo_ejelotE = new CTextField(Types.DECIMAL, "###9");

    CLabel cLabel5 = new CLabel();

    CLabel cLabel10 = new CLabel();

    CTextField usu_nombE = new CTextField();

    CTextField deo_ejlogeE = new CTextField(Types.DECIMAL, "###9");

    CTextField deo_nulogeE = new CTextField(Types.DECIMAL, "####9");

    CLabel cLabel11 = new CLabel();

    CTextField deo_selogeE = new CTextField(Types.CHAR, "X");

    CLabel cLabel13 = new CLabel();

    CLabel cLabel14 = new CLabel();

    CTextField deo_emlogeE = new CTextField(Types.DECIMAL, "#9");

    CLabel cLabel15 = new CLabel();

    CLabel cLabel16 = new CLabel();

    CLabel cLabel17 = new CLabel();

    CTextField deo_kilosE = new CTextField(Types.DECIMAL, "---,--9.99");

    CGridEditable jtLin = new CGridEditable(8) {

        public void afterCambiaLinea() {
            recalcCostoLin();
            jtLin.salirFoco();
        }
    };

    CLabel cLabel18 = new CLabel();

    CTextField deo_prcostE = new CTextField(Types.DECIMAL, "---,--9.99");

    CGridEditable jtCab = new CGridEditable(8) {

        public boolean deleteLinea(int row, int col) {
            swVerCab = false;
            try {
                ponerGrupo(jtCab.getValInt(jtCab.tableView.getSelectedRow(), 1), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 0), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 2), 1);
            } catch (Exception k) {
                Error("Error al Quitar Linea del Grupo", k);
            }
            return true;
        }

        public void afterDeleteLinea() {
            swVerCab = true;
            try {
                verDaLiCab(jtCab.getValInt(jtCab.tableView.getSelectedRow(), 1), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 0), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 2), 0);
            } catch (Exception k) {
                Error("Error al ver Datos Cabecera", k);
            }
        }

        public void afterCambiaLinea() {
            try {
                calcCostoCab();
            } catch (Exception k) {
                Error("Error al Calcular Costos de Cabecera", k);
            }
        }
    };

    CLabel tid_nombL = new CLabel();

    CPanel Plotgen = new CPanel();

    CLabel cLabel12 = new CLabel();

    CLabel nlinL = new CLabel();

    CLabel cLabel19 = new CLabel();

    CTextField deo_kilosE1 = new CTextField(Types.DECIMAL, "---,--9.99");

    CPanel Ptotal1 = new CPanel();

    CLabel cLabel110 = new CLabel();

    CLabel cLabel111 = new CLabel();

    CLabel nlinL1 = new CLabel();

    CTextField deo_prcostE1 = new CTextField(Types.DECIMAL, "---,--9.99");

    CTextField c0 = new CTextField(Types.DECIMAL, "####9");

    CTextField c1 = new CTextField(Types.CHAR, "X", 50);

    CTextField c2 = new CTextField(Types.DECIMAL, "##9");

    CTextField c3 = new CTextField(Types.DECIMAL, "---,--9.99");

    CTextField c4 = new CTextField(Types.DECIMAL, "---,--9.99");

    CTextField c5 = new CTextField(Types.DECIMAL, "---,--9.99");

    CTextField c6 = new CTextField(Types.DECIMAL, "--99.99");

    CCheckBox c7 = new CCheckBox("S", "N");

    CLabel cLabel112 = new CLabel();

    CTextField deo_prcabE = new CTextField(Types.DECIMAL, "---,--9.99");

    CLabel cLabel20 = new CLabel();

    CTextField grd_numeE2 = new CTextField(Types.DECIMAL, "####9");

    CButton grdMaxB = new CButton(Iconos.getImageIcon("fill"));

    CTextField emp_codiE = new CTextField(Types.DECIMAL, "#9");

    CTextField eje_numeE = new CTextField(Types.DECIMAL, "###9");

    CLabel cLabel113 = new CLabel();

    CTextField deo_kilosE2 = new CTextField(Types.DECIMAL, "---,--9.99");

    CLabel cLabel114 = new CLabel();

    CTextField deo_prcostE2 = new CTextField(Types.DECIMAL, "---,--9.99");

    CTextField grd_numeE = new CTextField(Types.DECIMAL, "####9");

    CLabel cLabel21 = new CLabel();

    CButton Bactgrid = new CButton("F2", Iconos.getImageIcon("reload"));

    CCheckBox grd_incvalE = new CCheckBox("S", "N");

    CCheckBox grd_incvalE1 = new CCheckBox("S", "N");

    CButton BponGru = new CButton(Iconos.getImageIcon("pon"));

    CTextField tfCab0 = new CTextField();

    CTextField tfCab1 = new CTextField();

    CTextField tfCab2 = new CTextField();

    CTextField tfCab3 = new CTextField();

    CTextField tfCab4 = new CTextField();

    CTextField tfCab5 = new CTextField();

    CTextField deo_prcogrE = new CTextField(Types.DECIMAL, "---,--9.99");

    CTextField tfCab7 = new CTextField();

    GridBagLayout gridBagLayout1 = new GridBagLayout();

    CCheckBox opAgrupa = new CCheckBox();

    CCheckBox grd_valorE = new CCheckBox("S", "N");

    CButton Bcalc = new CButton(Iconos.getImageIcon("calc"));

    CCheckBox opAgrLin = new CCheckBox();

    CLabel valoraL = new CLabel();

    CComboBox valoraC = new CComboBox();

    CCheckBox grd_blockE = new CCheckBox("S", "N");

    CCheckBox opValDes = new CCheckBox();

    public valdespi2(EntornoUsuario eu, Principal p) {
        EU = eu;
        vl = p.panel1;
        jf = p;
        eje = true;
        setTitulo("Valoracion de Despieces");
        try {
            if (jf.gestor.apuntar(this)) jbInit(); else setErrorInit(true);
        } catch (Exception e) {
            e.printStackTrace();
            setErrorInit(true);
        }
    }

    public valdespi2(gnu.chu.anjelica.menu p, EntornoUsuario eu) {
        EU = eu;
        vl = p.getLayeredPane();
        setTitulo("Valoracion  de Despieces");
        eje = false;
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
            setErrorInit(true);
        }
    }

    private void jbInit() throws Exception {
        iniciarFrame();
        this.setSize(new Dimension(696, 506));
        this.setVersion("2006-01-12");
        statusBar = new StatusBar(this);
        strSql = getStrSql("") + getOrderQuery();
        nav = new navegador(this, dtCons, false, navegador.CURYCON);
        nav.insBedit();
        conecta();
        opAgrupa.setSelected(true);
        cPanel1.setLayout(gridBagLayout1);
        grd_valorE.setEnabled(false);
        grdMaxB.setFocusable(false);
        Pcabe.setMaximumSize(new Dimension(605, 72));
        Pcabe.setMinimumSize(new Dimension(605, 72));
        Pcabe.setOpaque(true);
        Pcabe.setPreferredSize(new Dimension(615, 72));
        Pcabe.setDefButton(Baceptar);
        Pcabe.setButton(KeyEvent.VK_F4, Baceptar);
        Plotgen.setDefButton(Baceptar);
        Plotgen.setButton(KeyEvent.VK_F4, Baceptar);
        valoraC.setVisible(false);
        valoraL.setVisible(false);
        Pcabe.setEscButton(Bcancelar);
        Plotgen.setEscButton(Bcancelar);
        grd_blockE.setEnabled(false);
        tid_nombL.setBackground(Color.orange);
        tid_nombL.setForeground(Color.white);
        tid_nombL.setOpaque(true);
        tid_nombL.setBounds(new Rectangle(301, 3, 217, 14));
        Plotgen.setBorder(BorderFactory.createRaisedBevelBorder());
        Plotgen.setMaximumSize(new Dimension(458, 20));
        Plotgen.setMinimumSize(new Dimension(458, 20));
        Plotgen.setPreferredSize(new Dimension(458, 20));
        Plotgen.setLayout(null);
        cLabel12.setText("NLin");
        cLabel12.setBounds(new Rectangle(457, 53, 26, 15));
        nlinL.setBackground(Color.orange);
        nlinL.setForeground(Color.white);
        nlinL.setOpaque(true);
        nlinL.setHorizontalAlignment(SwingConstants.RIGHT);
        nlinL.setBounds(new Rectangle(486, 53, 33, 14));
        cLabel19.setBounds(new Rectangle(97, 2, 52, 17));
        cLabel19.setText("C. Linea");
        deo_kilosE1.setEnabled(false);
        deo_kilosE1.setBounds(new Rectangle(37, 2, 57, 17));
        Ptotal1.setLayout(null);
        Ptotal1.setBorder(BorderFactory.createLoweredBevelBorder());
        Ptotal1.setMaximumSize(new Dimension(522, 20));
        Ptotal1.setMinimumSize(new Dimension(552, 20));
        Ptotal1.setPreferredSize(new Dimension(552, 20));
        cLabel110.setText("Kilos");
        cLabel110.setBounds(new Rectangle(5, 2, 33, 17));
        cLabel111.setText("NLin");
        cLabel111.setBounds(new Rectangle(327, 2, 26, 17));
        nlinL1.setBackground(Color.orange);
        nlinL1.setForeground(Color.white);
        nlinL1.setOpaque(true);
        nlinL1.setHorizontalAlignment(SwingConstants.RIGHT);
        nlinL1.setBounds(new Rectangle(356, 2, 28, 17));
        deo_prcostE1.setEnabled(false);
        deo_prcostE1.setBounds(new Rectangle(151, 2, 65, 17));
        cLabel112.setText("C. Cab");
        cLabel112.setBounds(new Rectangle(220, 2, 46, 17));
        deo_prcabE.setBounds(new Rectangle(258, 2, 65, 17));
        deo_prcabE.setEnabled(false);
        cPanel2.setMaximumSize(new Dimension(233, 20));
        cPanel2.setMinimumSize(new Dimension(233, 20));
        cPanel2.setPreferredSize(new Dimension(233, 20));
        jtCab.setMaximumSize(new Dimension(524, 125));
        jtCab.setMinimumSize(new Dimension(524, 125));
        jtCab.setPreferredSize(new Dimension(524, 125));
        jtLin.setMaximumSize(new Dimension(525, 194));
        jtLin.setMinimumSize(new Dimension(525, 194));
        jtLin.setPreferredSize(new Dimension(525, 194));
        Baceptar.setMaximumSize(new Dimension(152, 22));
        Baceptar.setMinimumSize(new Dimension(152, 22));
        Baceptar.setPreferredSize(new Dimension(152, 22));
        Baceptar.setMargin(new Insets(0, 0, 0, 0));
        Bcancelar.setMaximumSize(new Dimension(152, 22));
        Bcancelar.setMinimumSize(new Dimension(152, 22));
        Bcancelar.setPreferredSize(new Dimension(152, 22));
        Bcancelar.setMargin(new Insets(0, 0, 0, 0));
        cLabel20.setText("Grupo");
        cLabel20.setBounds(new Rectangle(150, 2, 39, 16));
        grd_numeE2.setEnabled(false);
        grd_numeE2.setBounds(new Rectangle(192, 2, 48, 16));
        grdMaxB.setBounds(new Rectangle(437, 2, 22, 16));
        grdMaxB.setMinimumSize(new Dimension(34, 10));
        grdMaxB.setToolTipText("Pone Ult. Grupo Despiece");
        grdMaxB.setMargin(new Insets(0, 0, 0, 0));
        emp_codiE.setEnabled(false);
        emp_codiE.setBounds(new Rectangle(75, 3, 23, 14));
        eje_numeE.setError(false);
        eje_numeE.setText("");
        eje_numeE.setEnabled(false);
        eje_numeE.setBounds(new Rectangle(101, 3, 40, 14));
        cLabel113.setBounds(new Rectangle(404, 2, 42, 16));
        cLabel113.setText("Precio");
        deo_kilosE2.setEnabled(false);
        deo_kilosE2.setBounds(new Rectangle(320, 1, 58, 16));
        cLabel114.setBounds(new Rectangle(281, 1, 33, 16));
        cLabel114.setText("Kilos");
        deo_prcostE2.setEnabled(false);
        deo_prcostE2.setBounds(new Rectangle(454, 2, 52, 16));
        grd_numeE.setBounds(new Rectangle(396, 2, 41, 16));
        cLabel21.setBounds(new Rectangle(360, 2, 39, 16));
        cLabel21.setText("Grupo");
        cLabel21.setAlignmentY((float) 0.5);
        Bactgrid.setBounds(new Rectangle(454, 4, 47, 15));
        Bactgrid.setMargin(new Insets(0, 0, 0, 0));
        cLabel15.setToolTipText("");
        grd_incvalE.setMargin(new Insets(0, 0, 0, 0));
        grd_incvalE.setText("Despiece");
        grd_incvalE.setBounds(new Rectangle(462, 2, 88, 16));
        grd_incvalE1.setBounds(new Rectangle(254, 2, 75, 16));
        grd_incvalE1.setText("Despiece");
        grd_incvalE1.setEnabled(false);
        grd_incvalE1.setMargin(new Insets(0, 0, 0, 0));
        BponGru.setToolTipText("Poner en el Grupo");
        BponGru.setVerifyInputWhenFocusTarget(true);
        BponGru.setMargin(new Insets(0, 0, 0, 0));
        BponGru.setText("");
        BponGru.setBounds(new Rectangle(555, 1, 38, 16));
        opAgrupa.setText("Agrupar");
        opAgrupa.setBounds(new Rectangle(377, 53, 76, 16));
        grd_valorE.setText("Valorado");
        grd_valorE.setBounds(new Rectangle(525, 2, 95, 16));
        Bcalc.setBounds(new Rectangle(503, 4, 47, 15));
        Bcalc.setMargin(new Insets(0, 0, 0, 0));
        Bcalc.setToolTipText("Calcular Costo de Linea");
        Bcalc.setText("F5");
        opAgrLin.setMaximumSize(new Dimension(70, 22));
        opAgrLin.setMargin(new Insets(0, 0, 0, 0));
        opAgrLin.setSelected(true);
        opAgrLin.setText("Agrupar");
        opAgrLin.setBounds(new Rectangle(385, 2, 68, 14));
        valoraL.setText("Valorado");
        valoraL.setBounds(new Rectangle(525, 2, 49, 14));
        valoraC.setBounds(new Rectangle(579, 2, 45, 16));
        grd_blockE.setToolTipText("");
        grd_blockE.setHorizontalTextPosition(SwingConstants.LEFT);
        grd_blockE.setText("Bloq.");
        grd_blockE.setBorderPaintedFlat(false);
        grd_blockE.setBounds(new Rectangle(541, 54, 58, 14));
        opValDes.setHorizontalAlignment(SwingConstants.LEADING);
        opValDes.setHorizontalTextPosition(SwingConstants.LEFT);
        opValDes.setMargin(new Insets(0, 0, 0, 0));
        opValDes.setSelected(true);
        opValDes.setText("Val Desp.");
        opValDes.setBounds(new Rectangle(600, 3, 77, 14));
        nav.add(nav.btnEdit, null);
        Pcabe.setBorder(BorderFactory.createEtchedBorder());
        Pcabe.setLayout(null);
        cLabel1.setText("Fec.Ult.Inv.");
        cLabel1.setBounds(new Rectangle(4, 2, 71, 16));
        feulinE.setBounds(new Rectangle(72, 2, 73, 16));
        deo_fechaL.setText("Fecha despiece");
        deo_fechaL.setBounds(new Rectangle(213, 54, 85, 14));
        deo_fechaE.setBounds(new Rectangle(301, 54, 73, 14));
        cLabel2.setText("N. Despiece");
        cLabel2.setBounds(new Rectangle(6, 3, 66, 14));
        deo_codiE.setSonidoAutoNext(false);
        deo_codiE.setBounds(new Rectangle(144, 3, 49, 14));
        cPanel2.setBorder(BorderFactory.createRaisedBevelBorder());
        cPanel2.setLayout(null);
        cLabel3.setText("Tipo Despice");
        cLabel3.setBounds(new Rectangle(196, 3, 73, 14));
        tid_codiE.setBounds(new Rectangle(268, 3, 32, 14));
        cLabel4.setText("Producto");
        cLabel4.setBounds(new Rectangle(8, 19, 53, 17));
        pro_codiE.setBounds(new Rectangle(64, 19, 454, 18));
        pro_codiE.setAncTexto(50);
        cLabel6.setBounds(new Rectangle(142, 37, 33, 15));
        cLabel6.setText("Serie");
        pro_numindE.setBounds(new Rectangle(306, 37, 37, 15));
        pro_numindE.setAutoNext(true);
        cLabel8.setBounds(new Rectangle(284, 37, 26, 15));
        cLabel8.setText("Ind.");
        pro_loteE.setBounds(new Rectangle(231, 37, 46, 15));
        pro_loteE.setAutoNext(true);
        deo_serlotE.setBounds(new Rectangle(172, 37, 17, 15));
        deo_serlotE.setText("A");
        deo_serlotE.setMayusc(true);
        deo_serlotE.setAutoNext(true);
        cLabel9.setBounds(new Rectangle(0, 37, 47, 15));
        cLabel9.setText("Ejercicio");
        deo_emplotE.setBounds(new Rectangle(114, 37, 21, 15));
        deo_emplotE.setAutoNext(true);
        cLabel7.setBounds(new Rectangle(193, 37, 41, 15));
        cLabel7.setText("N. Lote");
        deo_ejelotE.setBounds(new Rectangle(48, 37, 33, 15));
        deo_ejelotE.setAutoNext(true);
        cLabel5.setBounds(new Rectangle(84, 37, 32, 15));
        cLabel5.setText("Emp.");
        cLabel10.setText("Usuario");
        cLabel10.setBounds(new Rectangle(346, 1, 51, 15));
        usu_nombE.setBounds(new Rectangle(398, 37, 121, 15));
        deo_ejlogeE.setAutoNext(true);
        deo_ejlogeE.setBounds(new Rectangle(126, 2, 33, 16));
        deo_nulogeE.setAutoNext(true);
        deo_nulogeE.setBounds(new Rectangle(311, 2, 46, 16));
        cLabel11.setText("Serie");
        cLabel11.setBounds(new Rectangle(218, 2, 33, 16));
        deo_selogeE.setAutoNext(true);
        deo_selogeE.setMayusc(true);
        deo_selogeE.setText("A");
        deo_selogeE.setBounds(new Rectangle(246, 2, 17, 16));
        cLabel13.setText("No Lote");
        cLabel13.setBounds(new Rectangle(265, 2, 46, 16));
        cLabel14.setText("A�o");
        cLabel14.setBounds(new Rectangle(102, 2, 25, 16));
        deo_emlogeE.setAutoNext(true);
        deo_emlogeE.setBounds(new Rectangle(194, 2, 21, 16));
        cLabel15.setText("Emp.");
        cLabel15.setBounds(new Rectangle(164, 2, 31, 16));
        cLabel16.setBackground(Color.red);
        cLabel16.setForeground(Color.white);
        cLabel16.setOpaque(true);
        cLabel16.setHorizontalAlignment(SwingConstants.CENTER);
        cLabel16.setText("Lote Generado");
        cLabel16.setBounds(new Rectangle(5, 2, 92, 16));
        cLabel17.setText("Kilos");
        cLabel17.setBounds(new Rectangle(5, 54, 33, 14));
        deo_kilosE.setBounds(new Rectangle(37, 54, 67, 14));
        {
            Vector v = new Vector();
            v.addElement("Eje");
            v.addElement("Emp");
            v.addElement("N.Des");
            v.addElement("Producto");
            v.addElement("Descripcion");
            v.addElement("Kilos");
            v.addElement("Costo");
            v.addElement("N. Reg");
            jtCab.setCabecera(v);
            jtCab.setAnchoColumna(new int[] { 30, 30, 38, 47, 130, 56, 56, 40 });
            jtCab.setAlinearColumna(new int[] { 2, 2, 2, 0, 0, 2, 2, 2 });
            jtCab.setFormatoColumna(5, "###,##9.99");
            jtCab.setFormatoColumna(6, "###,##9.99");
            jtCab.setFormatoColumna(7, "###9");
            Vector v1 = new Vector();
            tfCab0.setEnabled(false);
            tfCab1.setEnabled(false);
            tfCab2.setEnabled(false);
            tfCab3.setEnabled(false);
            tfCab4.setEnabled(false);
            tfCab5.setEnabled(false);
            tfCab7.setEnabled(false);
            v1.addElement(tfCab0);
            v1.addElement(tfCab1);
            v1.addElement(tfCab2);
            v1.addElement(tfCab3);
            v1.addElement(tfCab4);
            v1.addElement(tfCab5);
            v1.addElement(deo_prcogrE);
            v1.addElement(tfCab7);
            jtCab.setCampos(v1);
            jtCab.setCanInsertLinea(false);
        }
        Vector v = new Vector();
        v.addElement("Producto");
        v.addElement("Descripcion");
        v.addElement("Ind.");
        v.addElement("Cantidad");
        v.addElement("Costo Cal");
        v.addElement("Costo Fin");
        v.addElement("% Costo");
        v.addElement("Bloq");
        jtLin.setCabecera(v);
        jtLin.setAnchoColumna(new int[] { 54, 172, 36, 56, 60, 60, 50, 50 });
        jtLin.setAlinearColumna(new int[] { 2, 0, 2, 2, 2, 2, 2, 1 });
        jtLin.setFormatoColumna(3, "---,--9.99");
        jtLin.setFormatoColumna(4, "---,--9.99");
        jtLin.setFormatoColumna(5, "---,--9.99");
        jtLin.setFormatoColumna(6, "---9.99");
        jtLin.setFormatoColumna(7, "BSN");
        jtLin.resetRenderer(7);
        jtLin.setAjustarGrid(true);
        c0.setEnabled(false);
        c1.setEnabled(false);
        c2.setEnabled(false);
        c3.setEnabled(false);
        c5.setEnabled(false);
        c6.setEnabled(false);
        Vector v1 = new Vector();
        v1.addElement(c0);
        v1.addElement(c1);
        v1.addElement(c2);
        v1.addElement(c3);
        v1.addElement(c4);
        v1.addElement(c5);
        v1.addElement(c6);
        v1.addElement(c7);
        jtLin.setCampos(v1);
        jtLin.setCanInsertLinea(false);
        jtLin.setCanDeleteLinea(false);
        cLabel18.setText("Precio");
        cLabel18.setBounds(new Rectangle(109, 54, 42, 14));
        deo_prcostE.setBounds(new Rectangle(145, 54, 61, 14));
        if (!dtCons.getNOREG()) dtCons.last();
        Iniciar(this);
        this.getContentPane().add(cPanel1, BorderLayout.CENTER);
        cPanel1.add(cPanel2, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        cPanel2.add(cLabel1, null);
        cPanel2.add(feulinE, null);
        cPanel2.add(deo_prcostE2, null);
        cPanel2.add(valoraC, null);
        cPanel2.add(grd_valorE, null);
        cPanel2.add(valoraL, null);
        cPanel2.add(grd_numeE2, null);
        cPanel2.add(cLabel20, null);
        cPanel2.add(deo_kilosE2, null);
        cPanel2.add(cLabel114, null);
        cPanel2.add(cLabel113, null);
        Pcabe.add(tid_nombL, null);
        Pcabe.add(deo_codiE, null);
        Pcabe.add(eje_numeE, null);
        Pcabe.add(emp_codiE, null);
        Pcabe.add(cLabel2, null);
        Pcabe.add(cLabel3, null);
        Pcabe.add(tid_codiE, null);
        Pcabe.add(pro_codiE, null);
        Pcabe.add(cLabel4, null);
        Pcabe.add(usu_nombE, null);
        Pcabe.add(cLabel9, null);
        Pcabe.add(deo_ejelotE, null);
        Pcabe.add(cLabel5, null);
        Pcabe.add(deo_emplotE, null);
        Pcabe.add(cLabel6, null);
        Pcabe.add(deo_serlotE, null);
        Pcabe.add(cLabel7, null);
        Pcabe.add(pro_loteE, null);
        Pcabe.add(cLabel8, null);
        Pcabe.add(pro_numindE, null);
        Pcabe.add(cLabel10, null);
        Pcabe.add(nlinL, null);
        Pcabe.add(cLabel17, null);
        Pcabe.add(deo_kilosE, null);
        Pcabe.add(cLabel18, null);
        Pcabe.add(deo_prcostE, null);
        Pcabe.add(deo_fechaL, null);
        Pcabe.add(deo_fechaE, null);
        Pcabe.add(opAgrupa, null);
        Pcabe.add(cLabel12, null);
        Pcabe.add(grd_blockE, null);
        Ptotal1.add(deo_kilosE1, null);
        jtLin.setAjustarGrid(true);
        jtCab.setAjustarGrid(true);
        jtLin.ajustar(false);
        Ptotal1.add(Bactgrid, null);
        Ptotal1.add(Bcalc, null);
        Ptotal1.add(deo_prcostE1, null);
        Ptotal1.add(cLabel19, null);
        Ptotal1.add(cLabel112, null);
        Ptotal1.add(deo_prcabE, null);
        Ptotal1.add(nlinL1, null);
        Ptotal1.add(cLabel111, null);
        Ptotal1.add(opAgrLin, null);
        Ptotal1.add(cLabel110, null);
        Baceptar.setText("Aceptar (F4)");
        cPanel1.add(Bcancelar, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 30), 14, 0));
        cPanel1.add(Baceptar, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 30, 0, 0), 0, 0));
        cPanel1.add(Ptotal1, new GridBagConstraints(0, 6, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 3, 0), 0, 0));
        cPanel1.add(Plotgen, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        cPanel1.add(jtLin, new GridBagConstraints(0, 5, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 2, 0, 2), 0, 0));
        cPanel1.add(jtCab, new GridBagConstraints(0, 4, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 2, 0, 2), 0, 0));
        Plotgen.add(cLabel16, null);
        Plotgen.add(cLabel14, null);
        Plotgen.add(deo_ejlogeE, null);
        Plotgen.add(cLabel15, null);
        Plotgen.add(deo_emlogeE, null);
        Plotgen.add(cLabel11, null);
        cPanel1.add(Pcabe, new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        Plotgen.add(grd_incvalE, null);
        Plotgen.add(cLabel21, null);
        Plotgen.add(grdMaxB, null);
        Plotgen.add(deo_selogeE, null);
        Plotgen.add(deo_nulogeE, null);
        Plotgen.add(cLabel13, null);
        Plotgen.add(grd_numeE, null);
        Plotgen.add(BponGru, null);
        Plotgen.add(opValDes, null);
        mensajeErr("Espere, por favor .. Iniciando programa");
    }

    public void iniciarVentana() throws Exception {
        String feulin;
        gnu.chu.Menu.LoginDB.iniciarLKEmpresa(EU, dtStat);
        s = "select MAX(rgs_fecha) as cci_feccon from v_regstock as r,v_motregu  as m " + " where r.emp_codi = " + EU.em_cod + " and r.tir_codi = m.tir_codi " + " and M.tir_afestk='=' ";
        dtStat.select(s);
        if (dtStat.getDatos("cci_feccon") == null) feulin = "01-01-" + EU.ejercicio; else feulin = dtStat.getFecha("cci_feccon", "dd-MM-yyyy");
        s = "select * from v_motregu WHERE tir_afestk='='";
        if (!dtStat.select(s)) throw new Exception("No encontrado Motivo tipo Inventario");
        tirCodi = dtStat.getInt("tir_codi");
        valoraC.addItem("--", "-");
        valoraC.addItem("SI", "S");
        valoraC.addItem("NO", "N");
        jtCab.setAltButton(Bactgrid);
        jtLin.setAltButton(Bactgrid);
        jtCab.setDefButton(Baceptar);
        jtLin.setDefButton(Baceptar);
        jtCab.setButton(KeyEvent.VK_F4, Baceptar);
        jtLin.setButton(KeyEvent.VK_F4, Baceptar);
        jtCab.setButton(KeyEvent.VK_F5, Bcalc);
        jtLin.setButton(KeyEvent.VK_F5, Bcalc);
        eje_numeE.setColumnaAlias("eje_nume");
        deo_fechaE.setColumnaAlias("deo_fecha");
        deo_codiE.setColumnaAlias("deo_codi");
        tid_codiE.setColumnaAlias("tid_codi");
        pro_codiE.setColumnaAlias("pro_codi");
        deo_ejelotE.setColumnaAlias("deo_ejelot");
        deo_emplotE.setColumnaAlias("deo_emplot");
        deo_serlotE.setColumnaAlias("deo_serlot");
        pro_loteE.setColumnaAlias("pro_lote");
        pro_numindE.setColumnaAlias("pro_numind");
        deo_kilosE.setColumnaAlias("deo_kilos");
        deo_prcostE.setColumnaAlias("deo_prcost");
        deo_ejlogeE.setColumnaAlias("deo_ejloge");
        deo_emlogeE.setColumnaAlias("deo_emloge");
        deo_selogeE.setColumnaAlias("deo_seloge");
        deo_nulogeE.setColumnaAlias("deo_nuloge");
        grd_numeE.setColumnaAlias("deo_numdes");
        feulinE.setText(feulin);
        activarEventos();
        verDatos(dtCons);
        this.setEnabled(true);
        mensajeErr("");
        activar(false);
    }

    void activarEventos() {
        grd_incvalE.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (nav.pulsado != navegador.QUERY && nav.pulsado != navegador.EDIT) incValora();
            }
        });
        BponGru.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PADChose();
            }
        });
        opAgrLin.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!jtLin.isVacio()) verDatos(dtCons);
            }
        });
        opAgrupa.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (deo_kilosE.getQuery()) return;
                verDatos(dtCons);
            }
        });
        Bcalc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                calcCosto();
            }
        });
        jtCab.tableView.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                try {
                    if (nav.pulsado == navegador.EDIT) return;
                    if (!swVerCab || opAgrupa.isSelected() == true) return;
                    verDaLiCab(jtCab.getValInt(jtCab.tableView.getSelectedRow(), 1), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 0), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 2), 0);
                    if (!opAgrupa.isSelected() && nav.pulsado != navegador.EDIT) verDatLin(jtCab.getValInt(jtCab.tableView.getSelectedRow(), 1), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 0), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 2), 0, false);
                } catch (Exception k) {
                    Error("Error al ver lineas de Despiece", k);
                }
            }
        });
        jtCab.tableView.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (!jtCab.isEnabled() && Bactgrid.isEnabled()) Bactgrid.doClick();
            }
        });
        jtLin.tableView.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (!jtLin.isEnabled() && Bactgrid.isEnabled()) Bactgrid.doClick();
            }
        });
        grdMaxB.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    grd_numeE.setValorDec(utildesp.buscaMaxGrp(dtCon1, eje_numeE.getValorInt(), emp_codiE.getValorInt(), 0));
                } catch (Exception k) {
                    Error("Error al Buscar Max. Grupo", k);
                }
            }
        });
        Bactgrid.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    if (jtLin.isEnabled()) {
                        jtLin.setEnabled(false);
                        jtLin.procesaAllFoco();
                        calcCostoCab();
                        jtCab.setEnabled(true);
                        jtCab.requestFocusInicio();
                        return;
                    }
                    if (jtCab.isEnabled()) {
                        jtCab.setEnabled(false);
                        jtCab.procesaAllFoco();
                        calcCostoCab();
                        jtLin.setEnabled(true);
                        recalcCostoLin();
                        jtLin.requestFocusInicio();
                        return;
                    }
                } catch (Exception k) {
                    Error("Error al Recalcular Grids", k);
                    return;
                }
            }
        });
    }

    /**
      * Busca Stock sobre un producto. Comprueba si el producto tiene control
      * individual, para buscar por numero de individuos o no.
      *
      * @param fecStock Fecha de Stock, fecha a la que queremos el valor
      *                 . SI es nulo es a fecha actual.
      * @param fecInic Fecha Inicio desde la que buscar Mvtos. Tecnicamente la fecha
      * del ultimo stock fisico. Si es NULO 1-1-(A�o curso)
      * @param proCodi Codigo de Producto .. No puede ser nulo.
      * @param ejeNume Ejercicio de producto .. si es 0 se pondra el actual.
      * @param empCodi Empresa ... si es 0 se buscara en la activa.
      * @param serie   Serie .. si es null se buscara en todas
      * @param numLote si es 0 .. se buscara en todas.
      * @throws Exception en caso de cualquier error
      *
      * @return vector con los datos.
      */
    boolean buscaStock(java.util.Date fecStock, java.util.Date fecInic, int proCodi, int ejeNume, int empCodi, String serie, int numLote) throws Exception {
        if (DEBUG) {
            cantiAcu = 2;
            precAcu = 3;
            return true;
        }
        boolean acepNeg = opValDes.isSelected();
        String s;
        boolean swErr = false;
        boolean swConInd;
        String feulst;
        String fecStockStr;
        int nReg;
        double precio;
        String tipMov;
        double canti;
        double canMvto;
        if (fecStock == null) fecStock = new java.util.Date(System.currentTimeMillis());
        if (ejeNume == 0) ejeNume = EU.ejercicio;
        if (empCodi == 0) empCodi = EU.em_cod;
        fecStockStr = Formatear.getFechaVer(fecStock);
        feulst = Formatear.getFechaVer(fecInic);
        s = "";
        s = "SELECT 'C' as sel,'+' as tipmov,c.acc_fecrec as fecmov, c.acc_serie as serie," + " c.acc_nume as  lote," + " l.acl_canti as canti,l.acl_prcom as precio,l.pro_codi as pro_codori " + " FROM v_albacoc c,v_albacol l" + " where c.emp_codi = l.emp_codi " + " AND c.acc_serie = l.acc_serie " + " AND c.acc_nume = l.acc_nume " + " and c.acc_ano = l.acc_ano " + " AND c.emp_codi = " + empCodi + (serie != null ? " AND c.acc_serie = '" + serie + "'" : "") + (numLote == 0 ? "" : " AND c.acc_nume = " + numLote) + " AND l.pro_codi = " + proCodi + " AND c.acc_fecrec >= TO_DATE('" + feulst + "','dd-MM-yyyy') " + " and c.acc_fecrec <= TO_DATE('" + fecStockStr + "','dd-MM-yyyy') ";
        s += " UNION all";
        s += " select 'V' as sel,'-' as tipmov,c.avc_fecalb as fecmov," + "  c.avc_serie as serie,c.avc_nume as  lote," + " l.avl_canti as canti,0 as precio,l.pro_codi as pro_codori " + "  from v_albavel l, v_albavec c" + " where c.emp_codi = l.emp_codi " + " AND c.avc_serie = l.avc_serie " + " AND c.avc_nume = l.avc_nume " + " and c.avc_ano = l.avc_ano " + " AND c.avc_serie >= 'A' AND c.avc_serie <= 'C'" + " AND c.emp_codi = " + empCodi + " AND l.pro_codi = " + proCodi + " AND c.avc_fecalb >= TO_DATE('" + feulst + "','dd-MM-yyyy') " + " and c.avc_fecalb <= TO_DATE('" + fecStockStr + "','dd-MM-yyyy') ";
        s += " UNION all " + " select 'D' as sel,'" + (acepNeg ? "+" : "-") + "' as tipmov,deo_fecha as fecmov," + "  deo_serlot as serie,pro_lote as  lote," + " deo_kilos as canti,0 as precio,pro_codi as pro_codori " + " from  v_desporig where " + "  emp_codi = " + empCodi + (serie != null ? " AND deo_Serlot = '" + serie + "'" : "") + (numLote == 0 ? "" : " AND pro_lote = " + numLote) + " AND pro_codi = " + proCodi + " AND deo_fecha >= TO_DATE('" + feulst + "','dd-MM-yyyy') " + " and deo_fecha <= TO_DATE('" + fecStockStr + "','dd-MM-yyyy') ";
        s += " UNION all " + " select 'd' as sel, '+' as tipmov,c.deo_fecha as fecmov," + "  l.def_serlot as serie,l.pro_lote as  lote," + " l.def_kilos as canti,l.def_prcost as precio,c.pro_codi as pro_codori " + " from  v_desporig c,v_despfin l where " + " C.EMP_codi = l.emp_codi " + " and c.eje_nume = l.eje_nume " + " and c.deo_codi = l.deo_codi " + " and l.emp_codi = " + empCodi + (serie != null ? " AND l.def_Serlot = '" + serie + "'" : "") + (numLote == 0 ? "" : " AND l.pro_lote = " + numLote) + " AND l.pro_codi = " + proCodi + " AND c.deo_fecha >= TO_DATE('" + feulst + "','dd-MM-yyyy') " + " and c.deo_fecha <= TO_DATE('" + fecStockStr + "','dd-MM-yyyy') ";
        s += " UNION all " + " select 'R' as sel,tir_afestk as tipmov,r.rgs_fecha as fecmov," + "  r.pro_serie as serie,r.pro_nupar as  lote," + " r.rgs_kilos as canti,r.rgs_prregu as precio,r.pro_codi as pro_codori " + " FROM v_regstock r, v_motregu m WHERE " + " m.tir_codi = r.tir_codi " + " and r.emp_codi = " + empCodi + (serie != null ? " AND r.pro_serie = '" + serie + "'" : "") + (numLote == 0 ? "" : " AND r.pro_nupar = " + numLote) + " AND r.pro_codi = " + proCodi + " AND r.rgs_fecha >= TO_DATE('" + feulst + "','dd-MM-yyyy') " + " and r.rgs_fecha <= TO_DATE('" + fecStockStr + "','dd-MM-yyyy') ";
        s += " ORDER BY 3,2 desc";
        precAcu = 0;
        cantiAcu = 0;
        if (dtCon1.select(s)) {
            feulst = "";
            do {
                canti = 0;
                precio = precAcu;
                tipMov = dtCon1.getString("tipmov");
                if (tipMov.equals("=")) {
                    if (serie == null && numLote == 0) {
                        if (!feulst.equals(dtCon1.getFecha("fecmov"))) {
                            feulst = dtCon1.getFecha("fecmov");
                            precio = dtCon1.getDouble("precio");
                            cantiAcu = 0;
                            canti = dtCon1.getDouble("canti");
                        } else tipMov = "+";
                    }
                }
                if (tipMov.equals("+")) {
                    canMvto = dtCon1.getString("sel").equals("D") ? dtCon1.getDouble("canti", true) * -1 : dtCon1.getDouble("canti", true);
                    canti = cantiAcu + canMvto;
                    if (dtCon1.getString("sel").equals("d")) {
                        if ((dtCon1.getInt("pro_codori") == proCodi && acepNeg == false) || dtCon1.getDouble("precio", true) == 0) {
                            cantiAcu = canti;
                            continue;
                        }
                    }
                    if ((cantiAcu < 0 || canti < 0) && acepNeg == false) {
                        if (swErr == false) {
                            swErr = true;
                            if (swMsgBox) msgBox("Atencion Stock en Negativo: " + cantiAcu + " EN Fecha: " + dtCon1.getFecha("fecmov")); else mensajeErr("Atencion Stock en Negativo: " + cantiAcu + " EN Fecha: " + dtCon1.getFecha("fecmov"));
                        }
                        precio = dtCon1.getDouble("precio", true);
                    } else {
                        precio = (precAcu * cantiAcu) + (canMvto * dtCon1.getDouble("precio", true));
                        if ((precio >= 0.01 || precio <= -0.01) && (canti >= 0.01 || canti <= -0.01)) precio = precio / canti; else precio = precAcu;
                    }
                }
                if (tipMov.equals("-")) canti = cantiAcu - dtCon1.getDouble("canti", true);
                precAcu = precio;
                cantiAcu = canti;
            } while (dtCon1.next());
        } else {
            return false;
        }
        return true;
    }

    public void activar(boolean enab) {
        jtCab.setEnabled(enab);
        jtLin.setEnabled(enab);
        Bactgrid.setEnabled(enab);
        Bcalc.setEnabled(enab);
        valoraC.setVisible(false);
        valoraL.setVisible(false);
        grd_valorE.setVisible(true);
        eje_numeE.setEnabled(false);
        deo_emlogeE.setEnabled(enab);
        deo_ejlogeE.setEnabled(enab);
        deo_selogeE.setEnabled(enab);
        deo_nulogeE.setEnabled(enab);
        deo_kilosE.setEnabled(enab);
        deo_prcostE.setEnabled(enab);
        deo_fechaE.setEnabled(enab);
        Pcabe.setEnabled(enab);
        opAgrupa.setEnabled(true);
        opAgrupa.setEnabledParent(true);
        opAgrLin.setEnabled(true);
        BponGru.setEnabled(!enab);
        feulinE.setEnabled(enab);
        Baceptar.setEnabled(enab);
        Bcancelar.setEnabled(enab);
    }

    public void ej_query() {
        ej_query1();
    }

    public void ej_query1() {
        if (Pcabe.getErrorConf() != null) {
            mensajeErr("Error en condiciones de busqueda");
            Pcabe.getErrorConf().requestFocus();
            return;
        }
        Vector v = new Vector();
        v.add(eje_numeE.getStrQuery());
        v.add(deo_fechaE.getStrQuery());
        v.add(deo_codiE.getStrQuery());
        v.add(tid_codiE.getStrQuery());
        v.add(pro_codiE.getStrQuery());
        v.add(deo_ejelotE.getStrQuery());
        v.add(deo_emplotE.getStrQuery());
        v.add(deo_serlotE.getStrQuery());
        v.add(pro_loteE.getStrQuery());
        v.add(pro_numindE.getStrQuery());
        v.add(deo_kilosE.getStrQuery());
        v.add(deo_prcostE.getStrQuery());
        v.add(deo_ejlogeE.getStrQuery());
        v.add(deo_emlogeE.getStrQuery());
        v.add(deo_selogeE.getStrQuery());
        v.add(deo_emlogeE.getStrQuery());
        v.add(deo_nulogeE.getStrQuery());
        v.add(usu_nombE.getStrQuery());
        v.add(grd_numeE.getStrQuery());
        s = creaWhere("", v, false);
        if (!valoraC.getValor().equals("-")) {
            if (valoraC.getValor().equals("S")) {
                s += " and exists (select * from grupdesp " + " WHERE eje_nume = v_desporig.eje_nume" + " AND  emp_codi = v_desporig.emp_codi " + " AND grd_nume = v_desporig.deo_numdes " + " and (grd_valor = 'S' or grd_nume=99))";
            } else {
                s += " and (not exists (select * from grupdesp " + " WHERE eje_nume = v_desporig.eje_nume" + " AND  emp_codi = v_desporig.emp_codi " + " AND grd_nume = v_desporig.deo_numdes " + " and (grd_valor = 'S' or grd_nume=99)) or v_desporig.deo_numdes < 99)";
            }
        }
        s = getStrSql(s) + getOrderQuery();
        activaTodo();
        Pcabe.setQuery(false);
        Plotgen.setQuery(false);
        grd_numeE.setQuery(false);
        try {
            if (!dtCons.select(s)) {
                mensaje("");
                mensajeErr("No encontrados Despieces para estos criterios");
                rgSelect();
                return;
            }
            mensaje("");
            strSql = s;
            rgSelect();
            mensajeErr("Nuevos regisgtros selecionados");
        } catch (Exception ex) {
            fatalError("Error al buscar Inventarios: ", ex);
        }
    }

    public void canc_query() {
        Pcabe.setQuery(false);
        Plotgen.setQuery(false);
        grd_numeE.setQuery(false);
        mensaje("");
        mensajeErr("Consulta ... CANCELADA");
        activaTodo();
        verDatos(dtCons);
        nav.pulsado = navegador.NINGUNO;
    }

    public void PADEdit() {
        if (jtLin.isVacio()) {
            mensajeErr("No existen lineas de despiece");
            activaTodo();
            return;
        }
        if (deo_fechaE.getError()) {
            mensajeErr("Fecha de Despieze ... Incorrecta");
            activaTodo();
            return;
        }
        if (feulinE.isNull()) {
            mensajeErr("Introduzca Fecha Ult. Inventario");
            activaTodo();
            return;
        }
        if (grd_blockE.isSelected()) {
            mensajeErr("GRUPO ESTA BLOQUEADO");
            activaTodo();
            return;
        }
        nav.pulsado = navegador.EDIT;
        try {
            verDatGru(eje_numeE.getValorInt(), emp_codiE.getValorInt(), deo_codiE.getValorInt(), grd_numeE2.getValorInt(), true);
            if (grd_numeE2.getValorInt() < 99 || (grd_numeE2.getValorInt() > 99 && !grd_valorE.isSelected())) {
                int nRow = jtCab.getRowCount();
                int nusem = 0;
                GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(Formatear.getDate(deo_fechaE.getText(), "dd-MM-yyyy"));
                nusem = gc.get(GregorianCalendar.WEEK_OF_YEAR);
                for (int n = 0; n < jtCab.getRowCount(); n++) {
                    s = "SELECT dpv_preci FROM desproval WHERE emp_codi = " + EU.em_cod + " AND pro_codi =" + jtCab.getValInt(n, 3) + " and dpv_nusem = " + nusem + " and eje_nume = " + deo_fechaE.getFecha("yyyy");
                    if (dtCon1.select(s)) {
                        jtCab.setValor(dtCon1.getString("dpv_preci"), n, 6);
                    } else {
                        boolean res = buscaStock(deo_fechaE.getDate(), feulinE.getDate(), jtCab.getValInt(n, 3), eje_numeE.getValorInt(), emp_codiE.getValorInt(), null, 0);
                        if (res) jtCab.setValor("" + precAcu, n, 6); else {
                            msgBox("Valor de Costo NO encontrado para Producto: " + jtCab.getValInt(n, 3));
                            jtCab.setValor("" + 0, n, 6);
                        }
                    }
                }
                calcCostoCab();
            }
            deo_kilosE.resetCambio();
            deo_prcostE.resetCambio();
            feulinE.setEnabled(true);
            Baceptar.setEnabled(true);
            Bcancelar.setEnabled(true);
            jtCab.setEnabled(true);
            jtLin.setEnabled(false);
            Bactgrid.setEnabled(true);
            Bcalc.setEnabled(true);
            opAgrupa.setEnabled(false);
            opAgrLin.setEnabled(false);
            calcCostoLin();
            if (grd_numeE2.getValorInt() > 100) {
                grd_numeE.setValorDec(grd_numeE2.getValorDec());
                grd_incvalE.setSelected(grd_incvalE1.isSelected());
            } else grd_numeE.setValorDec(0);
            jtCab.requestFocusInicio();
        } catch (Exception k) {
            Error("Error al buscar Precio Medio ", k);
            return;
        }
    }

    void calcCostoCab() throws Exception {
        int nRow = jtCab.getRowCount();
        double totCosto = 0;
        double kilos = 0;
        for (int n = 0; n < nRow; n++) {
            kilos += jtCab.getValorDec(n, 5);
            totCosto += jtCab.getValorDec(n, 6) * jtCab.getValorDec(n, 5);
        }
        deo_kilosE.setValorDec(kilos);
        deo_prcostE.setValorDec(totCosto / kilos);
    }

    void calcCostoLin() throws Exception {
        Vector v;
        int nRow = jtLin.getRowCount();
        double totCosto = 0;
        for (int n = 0; n < nRow; n++) {
            boolean res = buscaStock(deo_fechaE.getDate(), feulinE.getDate(), jtLin.getValInt(n, 0), deo_ejelotE.getValorInt(), deo_emplotE.getValorInt(), null, 0);
            if (res) jtLin.setValor("" + precAcu, n, 4); else {
                msgBox("Valor de Costo NO encontrado para Linea: " + n);
                jtLin.setValor("0", n, 4);
            }
        }
        recalcCostoLin();
    }

    public void rgSelect() throws SQLException {
        super.rgSelect();
        if (!dtCons.getNOREG()) {
            dtCons.last();
            nav.setEnabled(nav.ULTIMO, false);
            nav.setEnabled(nav.SIGUIENTE, false);
        }
        verDatos(dtCons);
    }

    void recalcCostoLin() {
        int nRow = jtLin.getRowCount();
        double totCosto = deo_prcostE.getValorDec() * deo_kilosE.getValorDec();
        double totFin = 0;
        deo_prcabE.setValorDec(totCosto);
        totCosto = 0;
        boolean swDesBloq = false;
        for (int n = 0; n < nRow; n++) {
            if (!jtLin.getValBoolean(n, 7) && jtLin.getValorDec(n, 4) > 0) swDesBloq = true;
            totCosto += (jtLin.getValorDec(n, 4) * jtLin.getValorDec(n, 3));
        }
        if (!swDesBloq) {
            for (int n = 0; n < nRow; n++) {
                jtLin.setValor("" + jtLin.getValorDec(n, 4), n, 5);
                totFin += jtLin.getValorDec(n, 4) * jtLin.getValorDec(n, 3);
            }
            deo_prcostE1.setValorDec(totFin);
            return;
        }
        if (totCosto == 0) totCosto = deo_prcostE.getValorDec() * deo_kilosE.getValorDec();
        double costo = 0;
        if (nRow == 1) {
            jtLin.setValor("100", 0, 6);
        } else {
            for (int n = 0; n < nRow; n++) {
                costo = jtLin.getValorDec(n, 3) * jtLin.getValorDec(n, 4);
                if (totCosto == 0) costo = 0; else costo = costo / totCosto * 100;
                jtLin.setValor("" + costo, n, 6);
            }
        }
        totCosto = deo_prcabE.getValorDec();
        if (totCosto == 0) {
            deo_prcostE1.setValorDec(0);
            return;
        }
        double totLin = 0;
        totFin = 0;
        double totFi1 = 0;
        boolean swAjus = false;
        for (int n = 0; n < nRow; n++) {
            costo = totCosto * jtLin.getValorDec(n, 6) / 100;
            costo = costo / jtLin.getValorDec(n, 3);
            if (jtLin.getValBoolean(n, 7)) {
                swAjus = true;
                jtLin.setValor("" + jtLin.getValorDec(n, 4), n, 5);
                totFin += jtLin.getValorDec(n, 4) * jtLin.getValorDec(n, 3);
                continue;
            }
            if (costo >= 0) {
                totFi1 += costo * jtLin.getValorDec(n, 3);
                totFin += costo * jtLin.getValorDec(n, 3);
                jtLin.setValor("" + costo, n, 5);
                totLin += costo * jtLin.getValorDec(n, 3);
            } else jtLin.setValor("0", n, 5);
        }
        if (swAjus) {
            double porc1;
            double dif = totCosto - totFin;
            for (int n = 0; n < nRow; n++) {
                if (!jtLin.getValBoolean(n, 7)) {
                    totLin = jtLin.getValorDec(n, 3) * jtLin.getValorDec(n, 5);
                    porc1 = totLin / totFi1;
                    jtLin.setValor("" + (totLin + (porc1 * dif)) / jtLin.getValorDec(n, 3), n, 5);
                }
            }
            totLin = 0;
            for (int n = 0; n < nRow; n++) {
                totLin += jtLin.getValorDec(n, 3) * jtLin.getValorDec(n, 5);
            }
        }
        deo_prcostE1.setValorDec(totLin);
    }

    String getStrSql(String condWhere) {
        return "select  emp_codi,eje_nume, 0 as deo_codi, deo_numdes" + " from v_desporig WHERE deo_numdes > 99 " + condWhere + " GROUP BY emp_codi,eje_nume,deo_numdes " + " UNION ALL " + " SELECT emp_codi, eje_nume,deo_codi, deo_numdes " + " from v_desporig WHERE deo_numdes <= 99 " + condWhere;
    }

    String getOrderQuery() {
        return " order by 2,1,4,3";
    }

    public void ej_edit1() {
        recalcCostoLin();
        jtLin.salirFoco();
        int nRow = jtLin.getRowCount();
        try {
            for (int n = 0; n < nRow; n++) {
                s = "UPDATE v_despfin SET def_prcost = " + jtLin.getValorDec(n, 5) + ", def_numdes = " + (grd_numeE.getValorInt() > 100 ? grd_numeE.getValorInt() : 99) + " WHERE eje_nume = " + dtCons.getInt("eje_nume") + " AND  emp_codi = " + dtCons.getInt("emp_codi") + (grd_numeE2.getValorInt() > 100 ? " and def_numdes = " + grd_numeE2.getValorInt() : " AND deo_codi = " + deo_codiE.getValorInt()) + " AND pro_codi = " + jtLin.getValInt(n, 0);
                stUp.executeUpdate(s);
            }
            nRow = jtCab.getRowCount();
            for (int n = 0; n < nRow; n++) {
                s = "UPDATE v_desporig set deo_numdes = " + (grd_numeE.getValorInt() > 100 ? grd_numeE.getValorInt() : 99) + ", deo_prcost = " + jtCab.getValorDec(n, 6) + " WHERE  eje_nume = " + eje_numeE.getText() + " AND emp_codi = " + emp_codiE.getValorInt() + (grd_numeE2.getValorInt() > 100 ? " and deo_numdes = " + grd_numeE2.getValorInt() : " AND deo_codi = " + deo_codiE.getValorInt()) + " and pro_codi = " + jtCab.getValInt(n, 3);
                stUp.executeUpdate(s);
            }
            if (grd_numeE.getValorInt() >= 100) {
                s = "SELECT * FROM grupdesp WHERE eje_nume = " + dtCons.getInt("eje_nume") + " AND  emp_codi = " + dtCons.getInt("emp_codi") + " AND grd_nume = " + grd_numeE.getValorInt();
                if (!dtAdd.select(s, true)) {
                    dtAdd.addNew("grupdesp");
                    dtAdd.setDato("emp_codi", dtCons.getInt("emp_codi"));
                    dtAdd.setDato("eje_nume", dtCons.getInt("eje_nume"));
                    dtAdd.setDato("grd_nume", grd_numeE.getValorInt());
                    dtAdd.setDato("prv_codi", EU.lkEmpresa.getDatoInt("emp_prvdes"));
                    dtAdd.setDato("grd_serie", "V");
                    dtAdd.setDato("grd_block", "N");
                } else dtAdd.edit(dtAdd.getCondWhere());
                dtAdd.setDato("grd_kilo", deo_kilosE.getValorDec());
                dtAdd.setDato("grd_unid", 1);
                dtAdd.setDato("grd_prmeco", deo_prcostE.getValorDec());
                dtAdd.setDato("grd_incval", grd_incvalE.getSelecion());
                dtAdd.setDato("grd_valor", "S");
                dtAdd.update(stUp);
            }
            ctUp.commit();
        } catch (Exception ex) {
            try {
                ctUp.rollback();
            } catch (SQLException ex1) {
            }
            Error("Error al Actualizar Datos", ex);
        }
        activaTodo();
        verDatos(dtCons);
        nav.pulsado = navegador.NINGUNO;
        mensajeErr("Despieze ... Valorado");
        nav.pulsado = navegador.NINGUNO;
    }

    public void canc_edit() {
        activaTodo();
        verDatos(dtCons);
        mensaje("");
        mensajeErr("Edicion ... CANCELADA");
        nav.pulsado = navegador.NINGUNO;
    }

    public void PADAddNew() {
    }

    public void ej_addnew1() {
    }

    public void canc_addnew() {
    }

    public void PADDelete() {
    }

    public void ej_delete1() {
    }

    public void canc_delete() {
    }

    public void PADPrimero() {
        verDatos(dtCons);
    }

    public void PADAnterior() {
        verDatos(dtCons);
    }

    public void PADSiguiente() {
        verDatos(dtCons);
    }

    public void PADUltimo() {
        verDatos(dtCons);
    }

    public void PADQuery() {
        Pcabe.setEnabled(true);
        deo_emlogeE.setEnabled(true);
        deo_ejlogeE.setEnabled(true);
        deo_selogeE.setEnabled(true);
        deo_nulogeE.setEnabled(true);
        opAgrupa.setSelected(true);
        deo_kilosE.setEnabled(true);
        deo_prcostE.setEnabled(true);
        deo_fechaE.setEnabled(true);
        valoraC.setVisible(true);
        valoraL.setVisible(true);
        grd_valorE.setVisible(false);
        eje_numeE.setEnabled(true);
        Pcabe.setQuery(true);
        Plotgen.setQuery(true);
        Baceptar.setEnabled(true);
        Bcancelar.setEnabled(true);
        grd_numeE.setQuery(true);
        Pcabe.resetTexto();
        Plotgen.resetTexto();
        grd_numeE.resetTexto();
        opAgrupa.setEnabled(true);
        opAgrupa.setSelected(true);
        eje_numeE.setText("" + EU.ejercicio);
        deo_codiE.requestFocus();
    }

    void verDatos(DatosTabla dt) {
        try {
            swVerCab = false;
            jtCab.removeAllDatos();
            if (dt.getNOREG()) return;
            emp_codiE.setText(dt.getString("emp_codi"));
            eje_numeE.setText(dt.getString("eje_nume"));
            int deoCodi = dt.getInt("deo_codi");
            int numGru = dt.getInt("deo_numdes");
            if (deoCodi != 0) {
                s = "SELECT deo_numdes FROM v_desporig WHERE  eje_nume = " + eje_numeE.getValorInt() + " AND emp_codi = " + emp_codiE.getValorInt() + "  AND deo_codi = " + deoCodi;
                if (dtCon1.select(s)) {
                    if (dtCon1.getInt("deo_numdes") >= 99) numGru = dtCon1.getInt("deo_numdes");
                    if (numGru > 100) deoCodi = 0;
                }
            }
            grd_numeE2.setValorDec(numGru);
            verDatGru(eje_numeE.getValorInt(), emp_codiE.getValorInt(), deoCodi, numGru, opAgrupa.isSelected());
        } catch (Exception k) {
            Error("Error al ver datos", k);
            return;
        }
    }

    void verDaLiCab(int ejeNume, int empCodi, int deoCodi, int numDes) throws Exception {
        s = "SELECT o.*,t.tid_nomb FROM v_desporig o left join tipodesp t " + " on o.tid_codi = t.tid_codi  " + " WHERE o.eje_nume = " + ejeNume + " AND o.emp_codi = " + empCodi + (deoCodi == 0 ? " and deo_numdes= " + numDes : " AND o.deo_codi = " + deoCodi);
        if (!dtCon1.select(s)) {
            msgBox("Registro NO encontrado ... Probablemente se ha borrado");
            return;
        }
        deo_ejlogeE.setText(dtCon1.getString("deo_ejloge"));
        deo_emlogeE.setText(dtCon1.getString("deo_emloge"));
        deo_selogeE.setText(dtCon1.getString("deo_seloge"));
        deo_nulogeE.setText(dtCon1.getString("deo_nuloge"));
        pro_codiE.setText(dtCon1.getString("pro_codi"));
        deo_fechaE.setText(dtCon1.getFecha("deo_fecha", "dd-MM-yyyy"));
        deo_codiE.setText(dtCon1.getString("deo_codi"));
        tid_codiE.setText(dtCon1.getString("tid_codi"));
        deo_ejelotE.setText(dtCon1.getString("deo_ejelot"));
        deo_emplotE.setText(dtCon1.getString("deo_emplot"));
        deo_serlotE.setText(dtCon1.getString("deo_serlot"));
        pro_loteE.setText(dtCon1.getString("pro_lote"));
        pro_numindE.setText(dtCon1.getString("pro_numind"));
        usu_nombE.setText(dtCon1.getString("usu_nomb"));
        tid_nombL.setText(dtCon1.getString("tid_nomb"));
    }

    void verDatGru(int ejeNume, int empCodi, int deoCodi, int numdes, boolean agrupa) throws Exception {
        numDesAnt = numdes;
        swVerCab = false;
        boolean swEnCab = jtCab.isEnabled();
        jtCab.setEnabled(false);
        jtCab.removeAllDatos();
        if (agrupa) {
            s = "SELECT 0 as emp_codi,0 as eje_nume,0 as deo_codi," + " pro_codi,sum(deo_kilos) as kilos,sum(deo_kilos*deo_prcost) as costo," + " count(*) as nreg,max(deo_fecha) as deo_fecha " + " FROM v_desporig o" + " WHERE o.eje_nume = " + ejeNume + " AND o.emp_codi = " + empCodi + (deoCodi == 0 ? " AND o.deo_numdes = " + numdes : " and o.deo_codi = " + deoCodi) + " group by pro_codi ";
        } else {
            s = "SELECT emp_codi, eje_nume,deo_codi," + " pro_codi,deo_kilos as kilos,deo_kilos*deo_prcost as costo," + " 1 as nreg,deo_fecha " + " FROM v_desporig o" + " WHERE o.eje_nume = " + ejeNume + " AND o.emp_codi = " + empCodi + (deoCodi == 0 ? " AND o.deo_numdes = " + numdes : " and o.deo_codi = " + deoCodi);
        }
        deo_kilosE2.resetTexto();
        deo_prcostE2.resetTexto();
        grd_incvalE1.resetTexto();
        deo_ejlogeE.resetTexto();
        deo_emlogeE.resetTexto();
        deo_selogeE.resetTexto();
        deo_nulogeE.resetTexto();
        pro_codiE.resetTexto();
        deo_fechaE.resetTexto();
        deo_codiE.resetTexto();
        tid_codiE.resetTexto();
        deo_ejelotE.resetTexto();
        deo_emplotE.resetTexto();
        deo_serlotE.resetTexto();
        pro_loteE.resetTexto();
        pro_numindE.resetTexto();
        usu_nombE.resetTexto();
        tid_nombL.setText("");
        if (dtCon1.select(s)) {
            deo_fechaE.setText(dtCon1.getFecha("deo_fecha", "dd-MM-yyyy"));
            double kilos = 0;
            double costo = 0;
            double nReg = 0;
            do {
                Vector v = new Vector();
                v.addElement("" + dtCon1.getInt("emp_Codi"));
                v.addElement("" + dtCon1.getInt("eje_nume"));
                v.addElement("" + dtCon1.getInt("deo_codi"));
                v.addElement(dtCon1.getString("pro_Codi"));
                v.addElement(pro_codiE.getNombArt(dtCon1.getString("pro_Codi")));
                v.addElement(dtCon1.getString("kilos"));
                v.addElement("" + (dtCon1.getDouble("costo") / dtCon1.getDouble("kilos")));
                v.addElement(dtCon1.getString("nreg"));
                jtCab.addLinea(v);
                kilos += dtCon1.getDouble("kilos");
                costo += dtCon1.getDouble("costo");
                nReg += dtCon1.getInt("nreg");
            } while (dtCon1.next());
            deo_prcabE.setValorDec(costo);
            jtCab.requestFocus(0, 0);
            if (!agrupa) verDaLiCab(jtCab.getValInt(0, 1), jtCab.getValInt(0, 0), jtCab.getValInt(0, 2), numdes); else {
                if (nReg == 1) verDaLiCab(ejeNume, empCodi, deoCodi, numdes);
            }
            deo_kilosE.setValorDec(kilos);
            deo_prcostE.setValorDec(costo / kilos);
            nlinL.setText("" + nReg);
        }
        if (!agrupa && nav.pulsado != navegador.EDIT) verDatLin(jtCab.getValInt(jtCab.tableView.getSelectedRow(), 1), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 0), jtCab.getValInt(jtCab.tableView.getSelectedRow(), 2), 0, false); else verDatLin(ejeNume, empCodi, deoCodi, numdes, true);
        grd_valorE.setSelected(false);
        grd_blockE.setSelected(false);
        if (numdes > 100) {
            s = "SELECT * FROM grupdesp WHERE eje_nume = " + ejeNume + " AND  emp_codi = " + empCodi + " AND grd_nume = " + numdes;
            if (dtCon1.select(s)) {
                deo_kilosE2.setValorDec(dtCon1.getDouble("grd_kilo"));
                deo_prcostE2.setValorDec(dtCon1.getDouble("grd_prmeco"));
                grd_valorE.setSelecion(dtCon1.getString("grd_valor"));
                grd_incvalE1.setSelecion(dtCon1.getString("grd_incval"));
                grd_incvalE.setSelected(grd_incvalE1.isSelected());
                grd_blockE.setSelecion(dtCon1.getString("grd_block"));
            }
        }
        swVerCab = true;
        jtCab.setEnabled(swEnCab);
        jtCab.setCanDeleteLinea(!opAgrupa.isSelected());
        jtCab.panelBusqueda.setEnabled(!opAgrupa.isSelected());
        jtCab.Bborra.setEnabled(!opAgrupa.isSelected());
    }

    void verDatLin(int ejeNume, int empCodi, int numDesp, int numGru, boolean agrupa) throws SQLException, java.text.ParseException {
        if (agrupa) {
            if (opAgrLin.isSelected() || nav.pulsado == navegador.EDIT) {
                s = " select f.pro_codi as pro_codi,sum(f.def_numpie) as def_numpie, " + " sum(f.def_kilos) as def_kilos ,sum(f.def_prcost*f.def_kilos) as def_prcost " + " from v_despfin f where " + " f.def_kilos <> 0 " + " and eje_nume = " + ejeNume + " AND emp_codi = " + empCodi + (numGru > 100 ? " and def_numdes = " + numGru : " AND deo_codi = " + numDesp) + " group by pro_codi " + " order by pro_codi ";
            } else {
                s = " select f.pro_codi as pro_codi,f.def_numpie as def_numpie, " + " f.def_kilos as def_kilos ,f.def_prcost*f.def_kilos as def_prcost " + " from v_despfin f where " + " f.def_kilos <> 0 " + " and eje_nume = " + ejeNume + " AND emp_codi = " + empCodi + (numGru > 100 ? " and def_numdes = " + numGru : " AND deo_codi = " + numDesp) + " order by pro_codi ";
            }
        } else {
            if (opAgrLin.isSelected() || nav.pulsado == navegador.EDIT) {
                s = " select f.pro_codi as pro_codi,sum(f.def_numpie) as def_numpie, " + " sum(f.def_kilos) as def_kilos ,sum(f.def_prcost*f.def_kilos) as def_prcost " + " from v_despfin f where " + " f.def_kilos <> 0 " + " and eje_nume = " + ejeNume + " AND emp_codi = " + empCodi + " AND deo_codi = " + numDesp + " group by pro_codi " + " order by pro_codi ";
            } else {
                s = " select f.pro_codi as pro_codi,f.def_numpie, " + "  def_kilos ,f.def_prcost*f.def_kilos as def_prcost " + " from v_despfin f where " + " f.def_kilos <> 0 " + " and eje_nume = " + ejeNume + " AND emp_codi = " + empCodi + " AND deo_codi = " + numDesp + " order by pro_codi ";
            }
        }
        verDatLi1(s);
    }

    void verDatLi1(String sql) throws SQLException, java.text.ParseException {
        double kilos = 0, costo = 0;
        int nLin = 0;
        int codArt;
        boolean jtEnab = jtLin.isEnabled();
        jtLin.setEnabled(false);
        jtLin.removeAllDatos();
        if (!dtAdd.select(sql)) {
            deo_kilosE1.setValorDec(0);
            deo_prcostE1.setValorDec(0);
            nlinL1.setText("0");
            jtLin.setEnabled(jtEnab);
            return;
        }
        do {
            Vector v = new Vector();
            codArt = dtAdd.getInt("pro_codi", true);
            v.addElement("" + codArt);
            v.addElement(pro_codiE.getNombArt("" + codArt));
            v.addElement(dtAdd.getString("def_numpie"));
            v.addElement(dtAdd.getString("def_kilos"));
            v.addElement("" + (dtAdd.getDouble("def_prcost", true) / dtAdd.getDouble("def_kilos", true)));
            v.addElement("");
            v.addElement("");
            v.addElement(new Boolean(false));
            jtLin.addLinea(v);
            kilos += dtAdd.getDouble("def_kilos", true);
            costo += dtAdd.getDouble("def_prcost", true);
            nLin++;
        } while (dtAdd.next());
        deo_kilosE1.setValorDec(kilos);
        deo_prcostE1.setValorDec(costo);
        nlinL1.setText("" + nLin);
        jtLin.setEnabled(jtEnab);
    }

    public void PADChose() {
        if (grd_numeE.getValorInt() <= 98 || deo_codiE.getValorInt() == 0) {
            mensajeErr("Grupo deber ser superior a 98 Y Num. Despiece diferente de 0");
            return;
        }
        if (dtCons.getNOREG()) return;
        try {
            ponerGrupo(eje_numeE.getValorInt(), emp_codiE.getValorInt(), deo_codiE.getValorInt(), grd_numeE.getValorInt());
            verDatos(dtCons);
        } catch (Exception k) {
            Error("Error al Actualizar Numero Grupo ", k);
            return;
        }
        mensajeErr("Numero Grupo ... ACTUALIZADO");
        activaTodo();
    }

    void incValora() {
        if (dtCons.getNOREG()) {
            mensajeErr("No hay registros Activos");
            return;
        }
        int grdNume = grd_numeE2.getValorInt();
        if (grdNume < 100) {
            mensajeErr("Grupo debe ser Superior a 99");
            return;
        }
        try {
            s = "SELECT * FROM grupdesp WHERE eje_nume = " + dtCons.getInt("eje_nume") + " AND  emp_codi = " + dtCons.getInt("emp_codi") + " AND grd_nume = " + grdNume;
            if (!dtAdd.select(s, true)) {
                msgBox("Grupo NO ENCONTRADO en Despieces");
                return;
            }
            dtAdd.edit(dtAdd.getCondWhere());
            dtAdd.setDato("grd_incval", grd_incvalE.getSelecion());
            dtAdd.update(stUp);
            ctUp.commit();
            if (grd_incvalE.isSelected()) mensajeErr("Grupo marcado para Valorar"); else mensajeErr("Grupo marcado para NO Valorar");
            grd_incvalE1.setSelecion(grd_incvalE.getSelecion());
        } catch (Exception k) {
            Error("Error al Marcar Para Valorar", k);
        }
    }

    void ponerGrupo(int ejeNume, int empCodi, int deoCodi, int grpNume) throws Exception {
        s = "UPDATE v_desporig set deo_numdes = " + grpNume + " WHERE  eje_nume = " + ejeNume + " AND emp_codi = " + empCodi + " AND deo_codi = " + deoCodi;
        stUp.executeUpdate(s);
        s = "UPDATE  v_despfin SET  def_numdes = " + grpNume + " WHERE eje_nume = " + ejeNume + " AND  emp_codi = " + empCodi + " AND deo_codi = " + deoCodi;
        stUp.executeUpdate(s);
        s = "SELECT * FROM grupdesp WHERE eje_nume = " + ejeNume + " AND  emp_codi = " + empCodi + " AND grd_nume = " + grpNume;
        if (!dtAdd.select(s, true)) {
            dtAdd.addNew("grupdesp");
            dtAdd.setDato("emp_codi", empCodi);
            dtAdd.setDato("eje_nume", ejeNume);
            dtAdd.setDato("grd_nume", grpNume);
            dtAdd.setDato("grd_serie", "V");
            dtAdd.setDato("grd_kilo", 0);
            dtAdd.setDato("grd_unid", 1);
            dtAdd.setDato("grd_prmeco", 0);
            dtAdd.setDato("prv_codi", EU.lkEmpresa.getDatoInt("emp_prvdes"));
            dtAdd.setDato("grd_incval", grd_incvalE.getSelecion());
            dtAdd.setDato("grd_valor", "N");
            dtAdd.update(stUp);
        } else {
            dtAdd.edit(dtAdd.getCondWhere());
            dtAdd.setDato("grd_incval", grd_incvalE.getSelecion());
            dtAdd.update(stUp);
        }
        ctUp.commit();
    }

    void calcCosto() {
        try {
            if (jtCab.isEnabled()) {
                int nRow = jtCab.getRowCount();
                int nusem = 0;
                GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(Formatear.getDate(deo_fechaE.getText(), "dd-MM-yyyy"));
                nusem = gc.get(GregorianCalendar.WEEK_OF_YEAR);
                int n = jtCab.getSelectedRow();
                s = "SELECT dpv_preci FROM desproval WHERE emp_codi = " + EU.em_cod + " AND pro_codi =" + jtCab.getValInt(n, 3) + " and dpv_nusem = " + nusem + " and eje_nume = " + deo_fechaE.getFecha("yyyy");
                if (dtCon1.select(s)) {
                    jtCab.setValor("" + dtCon1.getDouble("dpv_preci"), n, 6);
                    deo_prcogrE.setValorDec(dtCon1.getDouble("dpv_preci"));
                    calcCostoCab();
                    recalcCostoLin();
                } else {
                    if (buscaStock(deo_fechaE.getDate(), feulinE.getDate(), jtCab.getValInt(n, 3), eje_numeE.getValorInt(), emp_codiE.getValorInt(), null, 0)) {
                        jtCab.setValor("" + precAcu, n, 6);
                        deo_prcogrE.setValorDec(precAcu);
                        calcCostoCab();
                        recalcCostoLin();
                    } else msgBox("Valor de Costo NO encontrado para Producto: " + jtCab.getValInt(n, 3));
                }
                jtCab.requestFocusSelected();
            } else {
                int n = jtLin.getSelectedRow();
                if (buscaStock(deo_fechaE.getDate(), feulinE.getDate(), jtLin.getValInt(n, 0), deo_ejelotE.getValorInt(), deo_emplotE.getValorInt(), null, 0)) {
                    c4.setValorDec(precAcu);
                    jtLin.setValor("" + precAcu, n, 4);
                    recalcCostoLin();
                } else msgBox("Valor de Costo NO encontrado para Linea: " + n);
                jtLin.requestFocusSelected();
            }
        } catch (Exception k) {
            Error("Error al Calcular Costo", k);
        }
    }

    public void afterConecta() throws SQLException, java.text.ParseException {
        pro_codiE.iniciar(dtStat, this, vl, EU);
    }
}
