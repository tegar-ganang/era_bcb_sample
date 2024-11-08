package org.digitall.projects.gdigitall.lib.misc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.digitall.lib.sql.LibSQL;
import org.digitall.projects.gdigitall.lib.classes.PieValue;
import org.digitall.projects.gdigitall.lib.components.PopupWindow;

public class OP_Proced {

    static String SQLDriver = "org.postgresql.Driver";

    static String DataBase = "jdbc:postgresql://172.16.4.253/master";

    static String SQLUser = "consulta";

    static String SQLPass = "consulta";

    static String idrendicion, idarea, nombarea, idpersona, idcomercio = "", idorgan, ObjDestino = "", anio = "", sedeactual = "";

    static String ruta = "", rutaGrafica = "", rutaIcono = "/basedato/opsalta/Programas/SistemaOPSalta/iconos", rutaInforme = "", navegador = "", separador = "";

    static String esquema = "";

    static Set teclasInt = new HashSet();

    static Set teclasFun = new HashSet();

    static String[] vmes = { "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic" };

    static String[] vcombust = { "Nor", "Sup", "Go" };

    static String[] vnum = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };

    static String color0 = "#204EAA";

    static String colorAzul = "#14326C";

    static String colorCelesteOscuro = "#3A6EA5";

    static String colorVerde = "#72AE32";

    static String colorCeleste = "#0099CC";

    static boolean validado = true;

    static Cursor wait = new Cursor(Cursor.WAIT_CURSOR);

    static Cursor def = new Cursor(Cursor.DEFAULT_CURSOR);

    static String[] VArchivo = new String[65535];

    static int j = 0;

    static int cantPopups = 0;

    static Connection pgCon = null;

    static int tries = 0;

    static String lastpath = "";

    public OP_Proced() {
    }

    static void LeerConfiguracion() {
    }

    public static Connection CreateConnection() throws SQLException {
        try {
            Class.forName(SQLDriver);
        } catch (ClassNotFoundException x) {
        }
        return DriverManager.getConnection(DataBase, SQLUser, SQLPass);
    }

    public static JTree CreaArbol(String tabla, String campoid, String campodescrip, String padre) throws Exception {
        ResultSet Resul;
        Resul = exConsulta("Select * from " + tabla + " where " + campoid + "=" + padre + " and estado<>'*' order by oid");
        Resul.next();
        DefaultMutableTreeNode arbol = new DefaultMutableTreeNode(Resul.getString(campoid) + " - " + Resul.getString(campodescrip));
        Resul = exConsulta("Select * from " + tabla + " where padre=" + padre + " and estado<>'*' order by oid");
        CreaHijo(tabla, campoid, campodescrip, arbol, Resul);
        return new JTree(arbol);
    }

    public static void CreaHijo(String tabla, String campoid, String campodescrip, DefaultMutableTreeNode padre, ResultSet rs) throws SQLException {
        while (rs.next()) {
            DefaultMutableTreeNode hijos = new DefaultMutableTreeNode(rs.getString(campoid) + " - " + rs.getString(campodescrip));
            padre.add(hijos);
            ResultSet Resul = exConsulta("Select * from " + tabla + " where estado<>'*' and padre=" + rs.getString(campoid) + " order by oid");
            CreaHijo(tabla, campoid, campodescrip, hijos, Resul);
        }
    }

    public static String getCampo(String SQLQuery) {
        try {
            String campo = "";
            ResultSet Resul1 = exConsulta(SQLQuery);
            if (Resul1.next()) {
                campo = Resul1.getString(1);
            }
            if (String.valueOf(campo).equals("null")) {
                campo = "0";
            }
            return campo;
        } catch (SQLException x) {
            return "";
        }
    }

    public static String getCampo(String _SQLQuery, String _columna) {
        String campo = "";
        try {
            ResultSet Resul1 = exConsulta(_SQLQuery);
            if (Resul1.next()) {
                campo = Resul1.getString(_columna);
            }
            if (String.valueOf(campo).equals("null")) {
                campo = "0";
            }
        } catch (SQLException x) {
            OP_Proced.Mensaje(x.getMessage(), "Error");
        }
        return campo;
    }

    public static String Max(String Tabla, String Campo, String FiltroConWhere) {
        String maximo = "0";
        try {
            ResultSet Resulx = OP_Proced.exConsulta("Select max(" + Campo + ") + 1 from " + Tabla + FiltroConWhere);
            if (Resulx.next()) {
                maximo = Resulx.getString(1);
                try {
                    if (maximo.equals(null)) {
                    }
                } catch (NullPointerException x) {
                    maximo = "1";
                }
            }
        } catch (SQLException x) {
            Mensaje(x.getMessage(), "ERROR 001: ");
        }
        return maximo;
    }

    public static Vector getNextRow(ResultSet rs, ResultSetMetaData rsmd, boolean _checks, boolean boton) throws SQLException {
        Vector FilaActual = new Vector();
        try {
            for (int i = 1; i <= rsmd.getColumnCount(); ++i) switch(rsmd.getColumnType(i)) {
                case Types.VARCHAR:
                    FilaActual.addElement(rs.getString(i));
                    break;
                case Types.CHAR:
                    FilaActual.addElement(rs.getString(i));
                    break;
                case -7:
                    if (rs.getBoolean(i)) FilaActual.addElement("SI"); else FilaActual.addElement("--");
                    break;
                case Types.DATE:
                    FilaActual.addElement(Fecha2(TransformaNull_Texto(rs.getString(i)), true));
                    break;
                case Types.TIME:
                    FilaActual.addElement(Hora(TransformaNull_Texto(rs.getString(i)), true, true));
                    break;
                case Types.LONGVARCHAR:
                    FilaActual.addElement(rs.getString(i));
                    break;
                case Types.INTEGER:
                    FilaActual.addElement(new Long(rs.getLong(i)));
                    break;
                case Types.BIGINT:
                    FilaActual.addElement(new Long(rs.getLong(i)));
                    break;
                case Types.DOUBLE:
                    FilaActual.addElement(OP_Proced.DobleDec(String.valueOf(rs.getDouble(i))));
                    break;
                case Types.NUMERIC:
                    FilaActual.addElement(new Double(rs.getLong(i)));
                    break;
                case Types.DECIMAL:
                    FilaActual.addElement(new Double(rs.getDouble(i)));
                    break;
                default:
            }
        } catch (Exception x) {
            OP_Proced.Mensaje(x.getMessage(), "ERROR 002: ");
        }
        if (_checks) FilaActual.addElement(new Boolean(false));
        if (boton) FilaActual.addElement(new String("Acci�n"));
        return FilaActual;
    }

    /**
   * 
   * @throws java.sql.SQLException
   * @param Panel
   * @param jTabla
   * @param jTableTmp
   * @param Liminf
   * @param CantReg
   * @param ConsultaTabla
   * @param Tabla
   * @param Esquema
   * @deprecated
   */
    public static void RefreshTabla(String Esquema, String Tabla, String ConsultaTabla, int CantReg, int Liminf, DefaultTableModel jTableTmp, JTable jTabla, JScrollPane Panel) throws SQLException {
        try {
            ResultSet Resul;
            ResultSetMetaData ResulMD;
            if (jTableTmp.getColumnCount() == 0) {
                String localSQLQuery = "Select columna from " + Esquema + "columnas where tabla='" + Tabla + "' Order by idcolumna";
                Resul = exConsulta(localSQLQuery);
                ResulMD = Resul.getMetaData();
                jTableTmp.setColumnCount(0);
                while (Resul.next()) {
                    jTableTmp.addColumn(getNextRow(Resul, ResulMD, false, false));
                }
            }
            jTableTmp.setRowCount(0);
            Resul = exConsulta(ConsultaTabla + " LIMIT " + CantReg + " OFFSET " + Liminf);
            ResulMD = Resul.getMetaData();
            while (Resul.next()) {
                jTableTmp.addRow(getNextRow(Resul, ResulMD, false, false));
            }
            jTabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTabla.getTableHeader().setReorderingAllowed(false);
            jTabla.getTableHeader().setResizingAllowed(true);
            Panel.getViewport().remove(jTabla);
            Panel.getViewport().add(jTabla);
        } catch (ArrayIndexOutOfBoundsException x) {
            OP_Proced.Mensaje(x.getMessage(), "Error");
        }
    }

    /**
   * @throws java.sql.SQLException
   * @param checks
   * @param Panel
   * @param jTabla
   * @param jTableTmp
   * @param Liminf
   * @param CantReg
   * @param ConsultaTabla
   * @param Tabla
   * @param Esquema
   * @new
   */
    public static void RefreshTabla(String Esquema, String Tabla, String ConsultaTabla, int CantReg, int Liminf, DefaultTableModel jTableTmp, JTable jTabla, JScrollPane Panel, boolean checks, boolean boton) throws SQLException {
        try {
            ResultSet Resul;
            ResultSetMetaData ResulMD;
            if (jTableTmp.getColumnCount() == 0) {
                String localSQLQuery = "Select columna from " + Esquema + "columnas where tabla='" + Tabla + "' Order by idcolumna";
                Resul = exConsulta(localSQLQuery);
                ResulMD = Resul.getMetaData();
                jTableTmp.setColumnCount(0);
                while (Resul.next()) {
                    jTableTmp.addColumn(getNextRow(Resul, ResulMD, false, false));
                }
                if (checks) jTableTmp.addColumn("Sel.");
                if (boton) jTableTmp.addColumn("Acci�n");
            }
            jTableTmp.setRowCount(0);
            Resul = exConsulta(ConsultaTabla + " LIMIT " + CantReg + " OFFSET " + Liminf);
            ResulMD = Resul.getMetaData();
            while (Resul.next()) {
                jTableTmp.addRow(getNextRow(Resul, ResulMD, checks, boton));
            }
            jTabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTabla.getTableHeader().setReorderingAllowed(false);
            jTabla.getTableHeader().setResizingAllowed(true);
            Panel.getViewport().remove(jTabla);
            Panel.getViewport().add(jTabla);
        } catch (ArrayIndexOutOfBoundsException x) {
            OP_Proced.Mensaje(x.getMessage(), "Error");
        }
    }

    public static void CargaGrilla(Vector cabecera, String ConsultaTabla, int CantReg, int Liminf, DefaultTableModel jTableTmp, JTable jTabla, JScrollPane Panel, boolean _checks, boolean boton) throws SQLException {
        ResultSet Resul;
        ResultSetMetaData ResulMD;
        if (jTableTmp.getColumnCount() == 0) {
            for (int i = 0; i < cabecera.size(); i++) {
                try {
                    jTableTmp.addColumn(cabecera.elementAt(i));
                } catch (ArrayIndexOutOfBoundsException x) {
                    OP_Proced.Mensaje(x.getMessage(), "Error");
                }
            }
            if (_checks) {
                jTableTmp.addColumn("Sel.");
            }
            if (boton) jTableTmp.addColumn("Acci�n");
        }
        jTableTmp.setRowCount(0);
        Resul = exConsulta(ConsultaTabla + " LIMIT " + CantReg + " OFFSET " + Liminf);
        ResulMD = Resul.getMetaData();
        while (Resul.next()) {
            jTableTmp.addRow(getNextRow(Resul, ResulMD, _checks, boton));
        }
        jTabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTabla.getTableHeader().setReorderingAllowed(false);
        jTabla.getTableHeader().setResizingAllowed(true);
        Panel.getViewport().remove(jTabla);
        Panel.getViewport().add(jTabla);
    }

    public static void TamanioColumna(JTable tabla, int[] vecindextamcol) {
        try {
            tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < vecindextamcol.length; ++i) {
                TableColumn col = tabla.getColumnModel().getColumn(i);
                col.setPreferredWidth(vecindextamcol[i]);
            }
        } catch (Exception x) {
            System.out.println("ERROR TamCol");
        }
    }

    public static void RemueveCol(JTable jtabla, int[] vecindexcol) {
        try {
            for (int i = 0; i < vecindexcol.length; ++i) {
                jtabla.removeColumn(jtabla.getColumnModel().getColumn(vecindexcol[i]));
            }
        } catch (Exception x) {
            System.out.println("ERROR RemueveCol");
        }
    }

    public static void CargaCombo(JComboBox combo, String ConsultaCombo, String filtro) {
        combo.removeAllItems();
        ResultSet Result1 = exConsulta(ConsultaCombo);
        int tamaniocombo = 0;
        try {
            while (Result1.next()) {
                combo.addItem(Result1.getString(1));
                if (filtro.equals(Result1.getString(1))) combo.setSelectedIndex(tamaniocombo);
                tamaniocombo = tamaniocombo + 1;
            }
        } catch (SQLException x) {
            x.printStackTrace();
        }
    }

    /**
   * @deprecated
   * @throws java.lang.Exception
   * @return 
   * @param HoraCompleta
   * @param fecha
   */
    public static String FechaHora(boolean fecha, boolean HoraCompleta) throws Exception {
        String SQLQuery;
        if (fecha) {
            return Fecha(getCampo("Select current_date as string"), true);
        } else {
            return Hora(getCampo("Select current_time as string"), true, HoraCompleta);
        }
    }

    public static String FechaHora2(boolean fecha, boolean HoraCompleta) {
        try {
            String SQLQuery;
            if (fecha) {
                return Fecha2(getCampo("Select current_date as string"), true);
            } else {
                return Hora(getCampo("Select current_time as string"), true, HoraCompleta);
            }
        } catch (Exception x) {
            Mensaje("Error java.lang.Exception", "Error");
            x.printStackTrace();
            return "";
        }
    }

    public static String SumResFechaHora(String Parametro, boolean fecha, String operacion, String cantidad, String valor, boolean HoraCompleta) throws Exception {
        String SQLQuery;
        if (fecha) {
            System.out.println("Select date '" + Parametro + "' " + operacion + " Interval '" + cantidad + " " + valor + "' as string");
            return Fecha(getCampo("Select date '" + Parametro + "' " + operacion + " Interval '" + cantidad + " " + valor + "' as string"), true);
        } else {
            return Hora(getCampo("Select time '" + Parametro + "' " + operacion + " Interval '" + cantidad + " " + valor + "' as string"), false, HoraCompleta);
        }
    }

    /**
   * @deprecated 
   * @throws java.lang.Exception
   * @return 
   * @param mostrar
   * @param fecha
   */
    public static String Fecha(String fecha, boolean mostrar) throws Exception {
        Date date;
        SimpleDateFormat formatter;
        if (fecha.length() > 0) {
            if (mostrar) {
                formatter = new SimpleDateFormat("yy-MM-dd");
                date = (Date) formatter.parse(fecha);
                String fecharet = formatter.format(date);
                formatter.applyPattern("dd/MM/yy");
            } else {
                formatter = new SimpleDateFormat("dd/MM/yy");
                date = (Date) formatter.parse(fecha);
                String fecharet = formatter.format(date);
                formatter.applyPattern("yyyy-MM-dd");
            }
            return formatter.format(date).toString();
        } else {
            return "";
        }
    }

    public static String Fecha2(String fecha, boolean mostrar) {
        try {
            Date date;
            SimpleDateFormat formatter;
            if (fecha.length() > 0) {
                if (mostrar) {
                    formatter = new SimpleDateFormat("yyyy-MM-dd");
                    date = (Date) formatter.parse(fecha);
                    String fecharet = formatter.format(date);
                    formatter.applyPattern("dd/MM/yyyy");
                } else {
                    formatter = new SimpleDateFormat("dd/MM/yyyy");
                    date = (Date) formatter.parse(fecha);
                    String fecharet = formatter.format(date);
                    formatter.applyPattern("yyyy-MM-dd");
                }
                return formatter.format(date).toString();
            } else {
                return "";
            }
        } catch (Exception x) {
            OP_Proced.Mensaje("Error al convertir la fecha", "Error");
            return "";
        }
    }

    public static String Hora(String hora, boolean mostrar, boolean HoraCompleta) throws Exception {
        Date date;
        SimpleDateFormat formatter;
        if (hora.length() > 0) {
            if (mostrar) {
                formatter = new SimpleDateFormat("HH:mm:ss");
                date = (Date) formatter.parse(hora);
                String fecharet = formatter.format(date);
                if (HoraCompleta) {
                    formatter.applyPattern("HH:mm:ss");
                } else {
                    formatter.applyPattern("HH:mm");
                }
            } else {
                formatter = (hora.length() == 8 ? new SimpleDateFormat("HH:mm:ss") : new SimpleDateFormat("HH:mm"));
                date = (Date) formatter.parse(hora);
                String fecharet = formatter.format(date);
                formatter.applyPattern("HH:mm:ss");
            }
            return formatter.format(date).toString();
        } else {
            return "";
        }
    }

    public static void Mensaje(String mensaje, String titulo) {
        JOptionPane.showMessageDialog((Component) null, mensaje, titulo, JOptionPane.OK_OPTION);
    }

    public static void DeleteFile(String Archivo) {
        try {
            boolean success = (new File(Archivo)).delete();
            if (!success) {
            }
        } catch (Exception x) {
            Mensaje(x.getMessage(), "Error 006");
        }
    }

    public static String DobleDec(String numero) {
        double n = 0;
        try {
            n = Double.parseDouble(numero);
        } catch (Exception x) {
            n = 0;
        }
        NumberFormat formatter = new DecimalFormat("#0.00");
        return formatter.format(n).replace(',', '.');
    }

    public static void CentraVentana(JDialog ventana) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = ventana.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        ventana.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        ventana.setResizable(false);
    }

    public static void CentraVentana(JFrame ventana) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = ventana.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        ventana.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        ventana.setResizable(false);
    }

    public static void ActivaCheckButoon(JCheckBox jchek, String op) {
        if (op.equals("s")) {
            jchek.setSelected(true);
        } else {
            jchek.setSelected(false);
        }
    }

    public static void ActivaCheckButoon(JCheckBox jchek, boolean op) {
        jchek.setSelected(op);
    }

    public static String CharCheckBox(JCheckBox jchek) {
        if (jchek.isSelected()) {
            return "s";
        } else {
            return "n";
        }
    }

    public static void IniciaTeclas() {
        int[] teclasfun = new int[] { 127, 27, 32, 16, 17, 18, 65406, 33, 34, 35, 36, 37, 38, 39, 40, 224, 225, 226, 227 };
        for (int i = 0; i < teclasfun.length; i++) {
            teclasFun.add("" + teclasfun[i]);
        }
        int[] teclasint = new int[] { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105 };
        for (int i = 0; i < teclasint.length; i++) {
            teclasInt.add("" + teclasint[i]);
        }
    }

    public static void ExpandeSplit(JSplitPane jSP, boolean ocultar, boolean derecho, int tamanio) {
        if (derecho) {
            jSP.getRightComponent().setVisible(ocultar);
        } else {
            jSP.getLeftComponent().setVisible(ocultar);
        }
        jSP.setDividerLocation(tamanio);
        jSP.setDividerSize(0);
    }

    public static String TextoApostrofo(String texto) {
        return texto.replaceAll("\'", "\\\\'");
    }

    public static BufferedImage drawPie(Rectangle area, PieValue[] slices, double[] valores) {
        BufferedImage bufferedImage = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        double total = 0.0D;
        double curValue = 0.0D;
        int startAngle = 0;
        double val_total = 0;
        for (int i = 0; i < slices.length; i++) {
            total += slices[i].getValue();
            val_total += valores[i];
        }
        g2d.setColor(Color.white);
        g2d.fillRect(area.x, area.y, area.width, area.height);
        int Angulo = 0;
        double AnguloReal = 0;
        for (int i = 0; i < slices.length; i++) {
            startAngle = (int) (curValue * 360 / total);
            int arcAngle = (int) (slices[i].getValue() * 360 / total);
            if (i == slices.length - 1) {
                arcAngle = 360 - startAngle;
            }
            curValue += slices[i].getValue();
            if (arcAngle != 0) {
                g2d.setColor(slices[i].getColor());
                g2d.fillArc(area.x, area.y, area.width, area.height, startAngle, arcAngle);
                int style = Font.ROMAN_BASELINE;
                int size = 10;
                Font font = new Font("Serif", style, size);
                g2d.setFont(font);
                g2d.setColor(Color.black);
                AnguloReal = arcAngle / 2 + Angulo;
                Angulo += arcAngle;
                double yy = area.width / 4 * Math.sin(Math.toRadians(AnguloReal));
                double xx = area.width / 4 * Math.cos(Math.toRadians(AnguloReal));
                g2d.drawString(DobleDec(String.valueOf(valores[i] * 100 / val_total)) + " %", area.width / 2 + Math.round(xx) - 20, area.height / 2 - Math.round(yy));
            }
        }
        g2d.dispose();
        return bufferedImage;
    }

    public static void DiagramaBarras(Rectangle areaimg, Color[] vcolor, double valturas[], int ancho_barra, double maximo, String ruta, boolean iscolor, boolean diag_d_linea, String etiqueta_y, String etiqueta_x, String[] vetiqueta, Color colorfijo, int AjusteAlturaBarra) {
        BufferedImage bufferedImage = new BufferedImage(areaimg.width, areaimg.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        double[] vec_ord = OP_Proced.OrdenaVector(valturas);
        double Max = vec_ord[vec_ord.length - 1];
        double minimo = vec_ord[0];
        double medio = vec_ord[vec_ord.length / 2];
        if (maximo == 0) {
            maximo = Max;
        }
        g2d.setColor(Color.white);
        g2d.fillRect(areaimg.x, areaimg.y, areaimg.width, areaimg.height);
        long altura;
        g2d.setColor(Color.blue);
        int style = Font.ROMAN_BASELINE;
        int size = 10;
        Font font = new Font("Arial", style, size);
        g2d.setFont(font);
        g2d.fillRect(59, 0, 1, areaimg.height - 10);
        g2d.drawString(etiqueta_y, 61, 10);
        int y_max = Math.round(Math.round(areaimg.height - Max * (areaimg.height - 20 - AjusteAlturaBarra) / maximo) - 10);
        int y_med = Math.round(Math.round(areaimg.height - medio * (areaimg.height - 20 - AjusteAlturaBarra) / maximo) - 10);
        int y_min = Math.round(Math.round(areaimg.height - minimo * (areaimg.height - 20 - AjusteAlturaBarra) / maximo) - 10);
        g2d.drawString(String.valueOf(Max), 0, y_max);
        g2d.fillRect(49, y_max - 5, 10, 1);
        g2d.drawString(String.valueOf(medio), 0, y_med);
        g2d.fillRect(49, y_med - 5, 10, 1);
        g2d.drawString(String.valueOf(minimo), 0, y_min);
        g2d.fillRect(49, y_min - 5, 10, 1);
        for (int i = 0; i < valturas.length; i++) {
            if (iscolor) {
                g2d.setColor(vcolor[i]);
            } else {
                g2d.setColor(colorfijo);
            }
            altura = Math.round(valturas[i] * (areaimg.height - 20 - AjusteAlturaBarra) / maximo);
            if (diag_d_linea) {
                g2d.fill3DRect(i * 3 * ancho_barra, Math.round(areaimg.height - altura) - 15, ancho_barra, Math.round(altura - 10), true);
            } else {
                g2d.fill3DRect(i * ancho_barra + 60, Math.round(areaimg.height - altura) - 15, ancho_barra, Math.round(altura), true);
            }
            g2d.setColor(Color.blue);
            g2d.drawString(vetiqueta[i], (i * ancho_barra) + (ancho_barra / 2) + 55, areaimg.height - 3);
        }
        g2d.drawString(etiqueta_x, (valturas.length * ancho_barra) + (ancho_barra / 2) + 55, areaimg.height);
        g2d.fillRect(50, areaimg.height - 15, valturas.length * ancho_barra + 20, 1);
        g2d.dispose();
        OP_Proced.GuardaImagen(bufferedImage, ruta);
    }

    public static void GuardaImagen(BufferedImage bufferedImage, String ruta) {
        RenderedImage rendImage = bufferedImage;
        try {
            File file = new File(ruta);
            ImageIO.write(rendImage, "jpg", file);
        } catch (IOException e) {
            Mensaje(e.getMessage(), "ERROR 003: ");
        }
    }

    public static void Torta(double[] valores, Color[] vcolor, Rectangle area, String ruta) {
        PieValue[] slices = new PieValue[valores.length];
        for (int i = 0; i < valores.length; i++) {
            slices[i] = new PieValue(valores[i], vcolor[i]);
        }
        GuardaImagen(drawPie(area, slices, valores), ruta);
    }

    public static String HexaColor(Color color) {
        String rojo = Integer.toHexString(color.getRed());
        String verde = Integer.toHexString(color.getGreen());
        String azul = Integer.toHexString(color.getBlue());
        if (rojo.length() < 2) {
            rojo += "0";
        }
        if (verde.length() < 2) {
            verde += "0";
        }
        if (azul.length() < 2) {
            azul += "0";
        }
        return rojo + verde + azul;
    }

    public static double[] OrdenaVector(double[] vector) {
        double[] vec = new double[vector.length];
        double aux = 0;
        for (int i = 0; i < vector.length; i++) {
            vec[i] = vector[i];
        }
        for (int i = 0; i < vec.length - 1; i++) {
            for (int j = i + 1; j < vec.length; j++) {
                if (vec[i] > vec[j]) {
                    aux = vec[i];
                    vec[i] = vec[j];
                    vec[j] = aux;
                }
            }
        }
        return vec;
    }

    public static boolean ComparaFecha(String Fecha1, String Fecha2) {
        boolean rta = false;
        try {
            if (OP_Proced.getCampo("Select 'ok' where '" + Fecha(Fecha1, false) + "'<='" + Fecha(Fecha2, false) + "'").equals("ok")) {
                rta = true;
            }
        } catch (Exception x) {
            Mensaje(x.getMessage(), "ERROR 004: ");
        }
        return rta;
    }

    public static boolean ValidarFecha(String Fecha_Ref, String Fecha_Validar, String operacion, String parametro, String cantidad) {
        boolean rta = false;
        try {
            String fecha = getCampo("Select date('" + getCampo("Select (Select timestamp '" + Fecha_Ref + "') " + operacion + " interval '" + cantidad + " " + parametro + "' as string") + "')");
            if (getCampo("Select 'ok' where '" + Fecha_Validar + "' between '" + fecha + "' and '" + Fecha_Ref + "'").equals("ok")) {
                rta = true;
            } else {
                rta = false;
            }
        } catch (Exception x) {
            Mensaje(x.getMessage(), "ERROR 005: ");
        }
        return rta;
    }

    public static String TransformaCaracter(String Texto) {
        if (Texto.equals("s")) {
            return "si";
        } else {
            return "no";
        }
    }

    public static String TransformaNull_Texto(String Texto) {
        String text = "", Q = "";
        try {
            if (Texto.length() > 4) {
                text = getCampo("Select 0 where '" + Texto + "' is null");
            }
        } catch (Exception x) {
            return "";
        }
        if (text.equals("0")) {
            return "";
        } else {
            return Texto;
        }
    }

    public static String TransformaTexto_Null(String Texto) {
        if (Texto.length() > 0) {
            return "'" + Texto + "'";
        } else {
            return "null";
        }
    }

    public static String ObtieneHost() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            return hostname;
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        }
        return "Error al obtener el nombre del host";
    }

    public static String ObtieneUsuario() {
        String usuario = System.getProperty("user.name");
        if (usuario.equals("unknown")) {
            OP_Proced.Mensaje("No se detecto un 'Usuario'.\nDebe iniciar sesion como usuario valido de la red.\nEl programa se cerrar", "Usuario no autorizado");
            return "";
        }
        return usuario;
    }

    public static String ObtieneDiaSemana(String Fecha) {
        try {
            if (Fecha.length() > 0) {
                return getCampo("Select dia from dias where id=" + getCampo("SELECT EXTRACT(DOW FROM TIMESTAMP '" + Fecha(Fecha, false) + "')"));
            } else {
                return "";
            }
        } catch (Exception x) {
            x.printStackTrace();
            return "";
        }
    }

    public static String ObtieneMes(String Fecha) {
        try {
            if (Fecha.length() > 0) {
                return getCampo("Select nombre from mes where id=" + getCampo("SELECT EXTRACT(MONTH FROM TIMESTAMP '" + Fecha(Fecha, false) + "')"));
            } else {
                return "";
            }
        } catch (Exception x) {
            x.printStackTrace();
            return "";
        }
    }

    public static void Errores(int Codigo) {
        switch(Codigo) {
            case 0:
                Mensaje("Estos datos estan siendo utilizados por otro usuario, \nintente nuevamente en unos minutos...", "Registro en Uso");
                break;
            case 1:
                Mensaje("El USUARIO ingresado no existe", "Usuario inexistente");
                break;
            case 2:
                Mensaje("Debe seleccionar un registro", "Registro no seleccionado");
                break;
            case 3:
                Mensaje("No existen datos", "Registro vacio");
                break;
            case 4:
                Mensaje("No existe el numero de catastro ingresado", "Numero de Catastro invalido");
                break;
            case 5:
                Mensaje("No posee AUTORIZACI�N para realizar esta acci�n", "Acceso no Autorizado");
                break;
        }
    }

    public static String ObtieneCantDia(String FechaDesde, String FechaHasta) throws SQLException {
        return getCampo("Select date '" + FechaHasta + "' - date '" + FechaDesde + "'");
    }

    public static String Reloj(String hora) {
        int hr = Integer.parseInt(hora.substring(0, 2));
        int min = Integer.parseInt(hora.substring(3, 5));
        int seg = Integer.parseInt(hora.substring(6, 8));
        String horas = String.valueOf(hr), minutos = String.valueOf(min), segundos = String.valueOf(seg);
        seg += 1;
        if (seg > 59) {
            seg = 0;
            segundos = "00";
            min += 1;
            if (min > 59) {
                min = 0;
                minutos = "00";
                hr += 1;
                if (hr > 23) {
                    hr = 0;
                    horas = "00";
                } else {
                    horas = String.valueOf(hr);
                }
            } else {
                minutos = String.valueOf(min);
            }
        } else {
            segundos = String.valueOf(seg);
        }
        if (segundos.length() == 1) {
            segundos = "0" + segundos;
        }
        if (minutos.length() == 1) {
            minutos = "0" + minutos;
        }
        if (horas.length() == 1) {
            horas = "0" + horas;
        }
        return horas + ":" + minutos + ":" + segundos;
    }

    public static String valida(String tipodatos, String datos) {
        if (tipodatos.equals("int")) {
            try {
                if (datos.equals("")) datos = "0";
                int numero = Integer.parseInt(datos);
                return datos;
            } catch (NumberFormatException x) {
                System.out.println("Error, no puedo convertir a entero");
                return "*";
            }
        } else if (tipodatos.equals("dec")) {
            try {
                if (datos.equals("")) datos = "0.0";
                if (datos.substring(0, 1).equals(".")) datos = "0" + datos;
                double numero = Double.parseDouble(datos);
                return datos;
            } catch (NumberFormatException x) {
                System.out.println("Error, no puedo convertir a decimal");
                return "*";
            }
        } else if (tipodatos.equals("date")) {
            if (datos.equals("")) datos = "99/99/9999";
            try {
                return Fecha(datos, false);
            } catch (java.text.ParseException x) {
                System.out.println("Error, no puedo convertir a fecha, porque es nula");
                return "*";
            } catch (Exception x) {
                System.out.println("Error, no puedo convertir a fecha, porque es nula");
                return "*";
            }
        } else if (tipodatos.equals("time")) {
            if (datos.equals("")) datos = "99:99:99";
            try {
                return Hora(datos, false, false);
            } catch (java.text.ParseException x) {
                System.out.println("Error, no puedo convertir a hora, porque es nula");
                return "*";
            } catch (Exception x) {
                System.out.println("Error, no puedo convertir a hora, porque es nula");
                return "*";
            }
        } else {
            return datos;
        }
    }

    public static File[] ObtieneDirectorios(String ruta, boolean Directorio) {
        File dir = new File(ruta);
        File[] files = dir.listFiles();
        FileFilter fileFilter = new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        files = dir.listFiles(fileFilter);
        return files;
    }

    public static String[] visitAllDirsAndFiles(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                visitAllDirsAndFiles(new File(dir, children[i]));
            }
        } else {
            VArchivo[j] = dir.toString();
            j += 1;
        }
        return VArchivo;
    }

    public static void DialogoAbrir() {
        JFileChooser ab = new JFileChooser();
        int returnVal = ab.showOpenDialog(ab);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = ab.getSelectedFile();
        } else {
        }
    }

    public static boolean ValidaRangoHora(String HoraFuente, String HoraIni, String HoraFin) throws SQLException {
        boolean op = false;
        if (getCampo("Select 'OK' where '" + HoraFuente + "' between '" + HoraIni + "' and '" + HoraFin + "'").toUpperCase().equals("OK")) {
            op = true;
        }
        return op;
    }

    public static String HoraFin(String Fecha) throws SQLException {
        if (Fecha.length() > 0) {
            return getCampo("Select horafin from dias where id=" + getCampo("SELECT EXTRACT(DOW FROM TIMESTAMP '" + Fecha + "')"));
        } else {
            return "";
        }
    }

    public static int IndexComponente(Container Contenedor, String NombreComp) {
        int index = 0;
        for (int i = 1; i <= Contenedor.getComponentCount() - 1; i++) {
            if (OP_Proced.TransformaNull_Texto(Contenedor.getComponent(i).getName()).equals(NombreComp)) {
                index = i;
            }
        }
        return index;
    }

    public static String[] VectorArea() throws SQLException {
        int cant = Integer.parseInt(OP_Proced.getCampo("Select count(*) FROM areas_color"));
        String[] VAreas = new String[cant];
        for (int i = 0; i < cant; i++) {
            VAreas[i] = String.valueOf(i + 1);
        }
        return VAreas;
    }

    public static void AbreInforme(String Archivo) {
        try {
            String command = "";
            if (System.getProperty("os.name").equals("Linux")) {
                command = "konqueror " + Archivo;
            } else {
                command = "c:" + OP_Proced.separador + "archivos de programa" + OP_Proced.separador + "internet explorer" + OP_Proced.separador + "iexplore " + Archivo;
            }
            Process child = Runtime.getRuntime().exec(command);
        } catch (EmptyStackException x) {
            System.out.println("ErrorSTack02");
        } catch (IOException x) {
            OP_Proced.Mensaje("Error al intentar utilizar el visor externo", "true");
        }
    }

    public static void ExtraeArchivoJAR(String Archivo, String DirJAR, String DirDestino) {
        FileInputStream entrada = null;
        FileOutputStream salida = null;
        try {
            File f = new File(DirDestino + separador + Archivo);
            try {
                f.createNewFile();
            } catch (Exception sad) {
                sad.printStackTrace();
            }
            InputStream source = OP_Proced.class.getResourceAsStream(DirJAR + "/" + Archivo);
            BufferedInputStream in = new BufferedInputStream(source);
            FileOutputStream out = new FileOutputStream(f);
            int ch;
            while ((ch = in.read()) != -1) out.write(ch);
            in.close();
            out.close();
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (entrada != null) {
                try {
                    entrada.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (salida != null) {
                try {
                    salida.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /** Esta funcion devuelve la cantidad de dias no laborales, segun el calendario, en un rango de fecha indicado
   * 
   * @param FechaFin: Fecha inicial
   * @param FechaIni: Fecha final
   */
    public static String getDiasNoLaborales(String FechaIni, String FechaFin) throws SQLException {
        return getCampo("SELECT count(*) FROM calendario WHERE fecha BETWEEN '" + FechaIni + "' AND '" + FechaFin + "'");
    }

    /** Esta funcion devuelve la Fecha correspondiente a la cantidad de Dias segun el calendario laboral
   * 
   * @param CantDias: Cantidad de dias para sumar a la fecha inicial
   * @param Fecha: Fecha inicial
   */
    public static String getFechaLaboral(String Fecha, String CantDias) {
        try {
            String fechafin = Fecha(SumResFechaHora(Fecha, true, "+", CantDias, "days", false), false);
            String dias = getDiasNoLaborales(Fecha, fechafin);
            while (!dias.equals("0")) {
                Fecha = fechafin;
                fechafin = Fecha(SumResFechaHora(Fecha, true, "+", dias, "days", false), false);
                dias = getDiasNoLaborales(Fecha, fechafin);
            }
            return fechafin;
        } catch (Exception x) {
            return "";
        }
    }

    /** agregado */
    public static BufferedImage getImagen(String Ruta, String Archivo) {
        File file = new File(Ruta + Archivo);
        try {
            FileOutputStream fis = new FileOutputStream(file);
            ResultSet rs = OP_Proced.exConsulta("SELECT img FROM images WHERE imgname='" + Archivo + "'");
            AffineTransform transform = new AffineTransform();
            AffineTransformOp scaleimg = new AffineTransformOp(transform, null);
            BufferedOutputStream output = new BufferedOutputStream(fis);
            if (rs != null) {
                while (rs.next()) {
                    byte[] imgBytes = rs.getBytes(1);
                    output.write(imgBytes);
                }
                output.close();
                rs.close();
            }
            BufferedImage imagen = ImageIO.read(file);
            return imagen;
        } catch (Exception x) {
            return null;
        }
    }

    public static JTree CreaArbolDir(String Raiz) {
        DefaultMutableTreeNode arbol = new DefaultMutableTreeNode(Raiz);
        File[] directorios = OP_Proced.ObtieneDirectorios(Raiz, true);
        CreaHijoDir(arbol, directorios);
        return new JTree(arbol);
    }

    public static void CreaHijoDir(DefaultMutableTreeNode padre, File[] Directorios) {
        for (int i = 0; i < Directorios.length; i++) {
            DefaultMutableTreeNode hijos = new DefaultMutableTreeNode(Directorios[i].getName());
            padre.add(hijos);
            File[] dirHijos = OP_Proced.ObtieneDirectorios(Directorios[i].getPath(), true);
            CreaHijoDir(hijos, dirHijos);
        }
    }

    static int flag;

    static int numero;

    static String num;

    static String num_letra;

    static String num_letras;

    static String num_letram;

    static String num_letradm;

    static String num_letracm;

    static String num_letramm;

    static String num_letradmm;

    public static String unidad(int numero) {
        switch(numero) {
            case 9:
                num = "nueve";
                break;
            case 8:
                num = "ocho";
                break;
            case 7:
                num = "siete";
                break;
            case 6:
                num = "seis";
                break;
            case 5:
                num = "cinco";
                break;
            case 4:
                num = "cuatro";
                break;
            case 3:
                num = "tres";
                break;
            case 2:
                num = "dos";
                break;
            case 1:
                if (flag == 0) num = "un"; else num = "un";
                break;
            case 0:
                num = "";
                break;
        }
        return num;
    }

    public static String decena(int numero) {
        if (numero >= 90 && numero <= 99) {
            num_letra = "noventa ";
            if (numero > 90) num_letra = num_letra.concat("y ").concat(unidad(numero - 90));
        } else if (numero >= 80 && numero <= 89) {
            num_letra = "ochenta ";
            if (numero > 80) num_letra = num_letra.concat("y ").concat(unidad(numero - 80));
        } else if (numero >= 70 && numero <= 79) {
            num_letra = "setenta ";
            if (numero > 70) num_letra = num_letra.concat("y ").concat(unidad(numero - 70));
        } else if (numero >= 60 && numero <= 69) {
            num_letra = "sesenta ";
            if (numero > 60) num_letra = num_letra.concat("y ").concat(unidad(numero - 60));
        } else if (numero >= 50 && numero <= 59) {
            num_letra = "cincuenta ";
            if (numero > 50) num_letra = num_letra.concat("y ").concat(unidad(numero - 50));
        } else if (numero >= 40 && numero <= 49) {
            num_letra = "cuarenta ";
            if (numero > 40) num_letra = num_letra.concat("y ").concat(unidad(numero - 40));
        } else if (numero >= 30 && numero <= 39) {
            num_letra = "treinta ";
            if (numero > 30) num_letra = num_letra.concat("y ").concat(unidad(numero - 30));
        } else if (numero >= 20 && numero <= 29) {
            if (numero == 20) num_letra = "veinte "; else num_letra = "veinti".concat(unidad(numero - 20));
        } else if (numero >= 10 && numero <= 19) {
            switch(numero) {
                case 10:
                    num_letra = "diez ";
                    break;
                case 11:
                    num_letra = "once ";
                    break;
                case 12:
                    num_letra = "doce ";
                    break;
                case 13:
                    num_letra = "trece ";
                    break;
                case 14:
                    num_letra = "catorce ";
                    break;
                case 15:
                    num_letra = "quince ";
                    break;
                case 16:
                    num_letra = "dieciseis ";
                    break;
                case 17:
                    num_letra = "diecisiete ";
                    break;
                case 18:
                    num_letra = "dieciocho ";
                    break;
                case 19:
                    num_letra = "diecinueve ";
                    break;
            }
        } else num_letra = unidad(numero);
        return num_letra;
    }

    public static String centena(int numero) {
        if (numero >= 100) {
            if (numero >= 900 && numero <= 999) {
                num_letra = "novecientos ";
                if (numero > 900) num_letra = num_letra.concat(decena(numero - 900));
            } else if (numero >= 800 && numero <= 899) {
                num_letra = "ochocientos ";
                if (numero > 800) num_letra = num_letra.concat(decena(numero - 800));
            } else if (numero >= 700 && numero <= 799) {
                num_letra = "setecientos ";
                if (numero > 700) num_letra = num_letra.concat(decena(numero - 700));
            } else if (numero >= 600 && numero <= 699) {
                num_letra = "seiscientos ";
                if (numero > 600) num_letra = num_letra.concat(decena(numero - 600));
            } else if (numero >= 500 && numero <= 599) {
                num_letra = "quinientos ";
                if (numero > 500) num_letra = num_letra.concat(decena(numero - 500));
            } else if (numero >= 400 && numero <= 499) {
                num_letra = "cuatrocientos ";
                if (numero > 400) num_letra = num_letra.concat(decena(numero - 400));
            } else if (numero >= 300 && numero <= 399) {
                num_letra = "trescientos ";
                if (numero > 300) num_letra = num_letra.concat(decena(numero - 300));
            } else if (numero >= 200 && numero <= 299) {
                num_letra = "doscientos ";
                if (numero > 200) num_letra = num_letra.concat(decena(numero - 200));
            } else if (numero >= 100 && numero <= 199) {
                if (numero == 100) num_letra = "cien "; else num_letra = "ciento ".concat(decena(numero - 100));
            }
        } else num_letra = decena(numero);
        return num_letra;
    }

    public static String miles(int numero) {
        if (numero >= 1000 && numero < 2000) {
            num_letram = (" mil ").concat(centena(numero % 1000));
        }
        if (numero >= 2000 && numero < 10000) {
            flag = 1;
            num_letram = unidad(numero / 1000).concat(" mil ").concat(centena(numero % 1000));
        }
        if (numero < 1000) num_letram = centena(numero);
        return num_letram;
    }

    public static String decmiles(int numero) {
        if (numero == 10000) num_letradm = "diez mil";
        if (numero > 10000 && numero < 20000) {
            flag = 1;
            num_letradm = decena(numero / 1000).concat(" mil ").concat(centena(numero % 1000));
        }
        if (numero >= 20000 && numero < 100000) {
            flag = 1;
            num_letradm = decena(numero / 1000).concat(" mil ").concat(miles(numero % 1000));
        }
        if (numero < 10000) num_letradm = miles(numero);
        return num_letradm;
    }

    public static String cienmiles(int numero) {
        if (numero == 100000) num_letracm = "cien mil";
        if (numero >= 100000 && numero < 1000000) {
            flag = 1;
            num_letracm = centena(numero / 1000).concat(" mil ").concat(centena(numero % 1000));
        }
        if (numero < 100000) num_letracm = decmiles(numero);
        return num_letracm;
    }

    public static String millon(int numero) {
        if (numero >= 1000000 && numero < 2000000) {
            flag = 1;
            num_letramm = ("Un millon ").concat(cienmiles(numero % 1000000));
        }
        if (numero >= 2000000 && numero < 10000000) {
            flag = 1;
            num_letramm = unidad(numero / 1000000).concat(" millones ").concat(cienmiles(numero % 1000000));
        }
        if (numero < 1000000) num_letramm = cienmiles(numero);
        return num_letramm;
    }

    public static String decmillon(int numero) {
        if (numero == 10000000) num_letradmm = "diez millones";
        if (numero > 10000000 && numero < 20000000) {
            flag = 1;
            num_letradmm = decena(numero / 1000000).concat("millones ").concat(cienmiles(numero % 1000000));
        }
        if (numero >= 20000000 && numero < 100000000) {
            flag = 1;
            num_letradmm = decena(numero / 1000000).concat(" milllones ").concat(millon(numero % 1000000));
        }
        if (numero < 10000000) num_letradmm = millon(numero);
        return num_letradmm;
    }

    public static String convertirLetras(int numero) {
        num_letras = decmillon(numero);
        return num_letras;
    }

    public static JTree crearArbolDependencias(String tabla, String campoid, String campodescrip, String padre) throws Exception {
        ResultSet rs = exConsulta("Select * from " + tabla + " where " + campoid + "=" + padre + " and estado<>'*' order by oid");
        rs.next();
        int nivel = rs.getInt("niveljerarquico");
        DefaultMutableTreeNode arbol = new DefaultMutableTreeNode(rs.getString(campoid) + " - " + rs.getString(campodescrip));
        rs = exConsulta("Select * from " + tabla + " where padre=" + padre + " and estado<>'*' order by " + campoid);
        crearSubDependencias(tabla, campoid, campodescrip, arbol, rs, nivel + 2);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(new ImageIcon("/tmp/1.gif"));
        renderer.setClosedIcon(new ImageIcon("/tmp/1.gif"));
        renderer.setLeafIcon(new ImageIcon("/tmp/2.gif"));
        JTree tree = new JTree(arbol);
        tree.setCellRenderer(renderer);
        return tree;
    }

    public static void crearSubDependencias(String tabla, String campoid, String campodescrip, DefaultMutableTreeNode padre, ResultSet rs, int _nivel) throws SQLException {
        while (rs.next()) {
            int nivel = rs.getInt("niveljerarquico");
            while (_nivel + 1 < nivel) {
                DefaultMutableTreeNode directa = new DefaultMutableTreeNode("0 - Depende de (" + rs.getInt("padre") + ")");
                padre.add(directa);
                padre = directa;
                _nivel++;
            }
            DefaultMutableTreeNode hijos = new DefaultMutableTreeNode(rs.getString(campoid) + " - " + rs.getString(campodescrip));
            padre.add(hijos);
            ResultSet Resul = exConsulta("Select * from " + tabla + " where estado<>'*' and padre=" + rs.getString(campoid) + " order by niveljerarquico, " + campoid);
            crearSubDependencias(tabla, campoid, campodescrip, hijos, Resul, nivel);
        }
    }

    public static DefaultMutableTreeNode crearSubDependenciasVacias(DefaultMutableTreeNode padre, int cantidad, int nivel) throws SQLException {
        if (cantidad > 0) {
            DefaultMutableTreeNode hijos = new DefaultMutableTreeNode(" (" + nivel++ + ") ");
            padre.add(hijos);
            cantidad--;
            hijos = crearSubDependenciasVacias(hijos, cantidad, nivel++);
            return hijos;
        } else {
            return padre;
        }
    }

    public static String getNroEditado(String _monto) {
        String numeroEditado = "";
        if (!_monto.equals("0")) {
            int indice = _monto.indexOf(".");
            if (indice > 0) {
                String decimal = _monto.substring(_monto.indexOf(".") + 1, _monto.length());
                if (decimal.length() > 2) {
                    decimal = decimal.substring(0, 2);
                }
                String entero = _monto.substring(0, _monto.indexOf("."));
                if (entero.length() <= 3) {
                    String centena = entero.substring(entero.length() - 3, entero.length());
                    numeroEditado = centena + "," + decimal;
                } else if (entero.length() <= 6) {
                    String mil = entero.substring(0, entero.length() - 3);
                    String centena = entero.substring(entero.length() - 3, entero.length());
                    numeroEditado = mil + "." + centena + "," + decimal;
                } else if (entero.length() <= 9) {
                    String millon = entero.substring(0, entero.length() - 6);
                    String mil = entero.substring(entero.length() - 6, entero.length() - 3);
                    String centena = entero.substring(entero.length() - 3, entero.length());
                    numeroEditado = millon + "." + mil + "." + centena + "," + decimal;
                } else if (entero.length() <= 12) {
                    String cienMillones = entero.substring(0, entero.length() - 9);
                    String millon = entero.substring(entero.length() - 9, entero.length() - 6);
                    String mil = entero.substring(entero.length() - 6, entero.length() - 3);
                    String centena = entero.substring(entero.length() - 3, entero.length());
                    numeroEditado = cienMillones + "." + millon + "." + mil + "." + centena + "," + decimal;
                } else if (entero.length() <= 15) {
                    String milMillones = entero.substring(0, entero.length() - 12);
                    String cienMillones = entero.substring(entero.length() - 12, entero.length() - 9);
                    String millon = entero.substring(entero.length() - 9, entero.length() - 6);
                    String mil = entero.substring(entero.length() - 6, entero.length() - 3);
                    String centena = entero.substring(entero.length() - 3, entero.length());
                    numeroEditado = milMillones + "." + cienMillones + "." + millon + "." + mil + "." + centena + "," + decimal;
                } else if (entero.length() <= 18) {
                    String billones = entero.substring(0, entero.length() - 15);
                    String milMillones = entero.substring(entero.length() - 15, entero.length() - 12);
                    String cienMillones = entero.substring(entero.length() - 12, entero.length() - 9);
                    String millon = entero.substring(entero.length() - 9, entero.length() - 6);
                    String mil = entero.substring(entero.length() - 6, entero.length() - 3);
                    String centena = entero.substring(entero.length() - 3, entero.length());
                    numeroEditado = billones + "." + cienMillones + "." + millon + "." + mil + "." + centena + "," + decimal;
                }
            } else {
                if (_monto.length() <= 3) {
                    String centena = _monto.substring(0, _monto.length());
                    numeroEditado = centena + ",00";
                } else if (_monto.length() <= 6) {
                    String mil = _monto.substring(0, _monto.length() - 3);
                    String centena = _monto.substring(_monto.length() - 3, _monto.length());
                    numeroEditado = mil + "." + centena + ",00";
                } else if (_monto.length() <= 9) {
                    String millon = _monto.substring(0, _monto.length() - 6);
                    String mil = _monto.substring(_monto.length() - 6, _monto.length() - 3);
                    String centena = _monto.substring(_monto.length() - 3, _monto.length());
                    numeroEditado = millon + "." + mil + "." + centena + ",00";
                } else if (_monto.length() <= 12) {
                    String cienMillones = _monto.substring(0, _monto.length() - 9);
                    String millon = _monto.substring(_monto.length() - 9, _monto.length() - 6);
                    String mil = _monto.substring(_monto.length() - 6, _monto.length() - 3);
                    String centena = _monto.substring(_monto.length() - 3, _monto.length());
                    numeroEditado = cienMillones + "." + millon + "." + mil + "." + centena + ",00";
                } else if (_monto.length() <= 15) {
                    String milMillones = _monto.substring(0, _monto.length() - 12);
                    String cienMillones = _monto.substring(_monto.length() - 12, _monto.length() - 9);
                    String millon = _monto.substring(_monto.length() - 9, _monto.length() - 6);
                    String mil = _monto.substring(_monto.length() - 6, _monto.length() - 3);
                    String centena = _monto.substring(_monto.length() - 3, _monto.length());
                    numeroEditado = milMillones + "." + cienMillones + "." + millon + "." + mil + "." + centena + ",00";
                } else if (_monto.length() <= 18) {
                    String billones = _monto.substring(0, _monto.length() - 15);
                    String milMillones = _monto.substring(_monto.length() - 15, _monto.length() - 12);
                    String cienMillones = _monto.substring(_monto.length() - 12, _monto.length() - 9);
                    String millon = _monto.substring(_monto.length() - 9, _monto.length() - 6);
                    String mil = _monto.substring(_monto.length() - 6, _monto.length() - 3);
                    String centena = _monto.substring(_monto.length() - 3, _monto.length());
                    numeroEditado = billones + "." + cienMillones + "." + millon + "." + mil + "." + centena + ",00";
                }
            }
        }
        return numeroEditado;
    }

    public static Border BordePanel(String _titulo) {
        TitledBorder borde = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.blue, 1), _titulo, 0, 0, new Font("Dialog", Font.BOLD, 12));
        borde.setTitleColor(Color.BLUE);
        return borde;
    }

    public static void setEsquema(String _esquema) {
        esquema = _esquema;
    }

    public static String getSQLUser() {
        return SQLUser;
    }

    public static void setSQLUser(String _user) {
        SQLUser = _user;
    }

    public static String getSQLPass() {
        return SQLPass;
    }

    public static void setSQLPass(String _pass) {
        SQLPass = _pass;
    }

    public static void setSedeActual(String _sede) {
        sedeactual = _sede;
    }

    public static String getRuta() {
        return ruta;
    }

    public static void setRuta(String _ruta) {
        ruta = _ruta;
    }

    public static String getSeparador() {
        return separador;
    }

    public static void setSeparador(String _separador) {
        separador = _separador;
    }

    public static String getRutaIcono() {
        return rutaIcono;
    }

    public static String getRutaGrafica() {
        return rutaGrafica;
    }

    public static String getRutaInforme() {
        return rutaInforme;
    }

    public static String getObjDestino() {
        return ObjDestino;
    }

    public static void setObjDestino(String _destino) {
        ObjDestino = _destino;
    }

    public static String getIDComercio() {
        return idcomercio;
    }

    public static String getIDPersona() {
        return idpersona;
    }

    public static void setRutaIcono(String _ruta) {
        rutaIcono = _ruta;
    }

    public static void setRutaGrafica(String _ruta) {
        rutaGrafica = _ruta;
    }

    public static void setRutaInforme(String _ruta) {
        rutaInforme = _ruta;
    }

    public static void setCantPopups(int _cant) {
        cantPopups = _cant;
    }

    public static int getCantPopups() {
        return cantPopups;
    }

    public static boolean getValidado() {
        return validado;
    }

    public static void setValidado(boolean _validado) {
        validado = _validado;
    }

    public static Set getTeclasInt() {
        return teclasInt;
    }

    public static Set getTeclasFun() {
        return teclasFun;
    }

    public static String getColorAzul() {
        return colorAzul;
    }

    public static String getColorCelesteOscuro() {
        return colorCelesteOscuro;
    }

    public static String getColorCeleste() {
        return colorCeleste;
    }

    public static String getColorVerde() {
        return colorVerde;
    }

    public static String getColor0() {
        return color0;
    }

    public static String[] getVNum() {
        return vnum;
    }

    public static int getJota() {
        return j;
    }

    public static void setJota(int _j) {
        j = _j;
    }

    public static String[] getVMes() {
        return vmes;
    }

    public static String[] getVCombust() {
        return vcombust;
    }

    public static void setAnio(String _anio) {
        anio = _anio;
    }

    public static String getAnio() {
        return anio;
    }

    public static Cursor getWait() {
        return wait;
    }

    public static Cursor getDef() {
        return def;
    }

    public static String getDataBase() {
        return DataBase;
    }

    public static boolean tryToConnect(String _SQLUser, String _SQLPass) {
        try {
            try {
                Class.forName(SQLDriver);
            } catch (ClassNotFoundException x) {
                Mensaje("El controlador de la base de datos no est� instalado correctamente", "Error de conexi�n");
            }
            Connection tmp = DriverManager.getConnection(DataBase, _SQLUser, _SQLPass);
            tmp.close();
            return true;
        } catch (SQLException x) {
            return false;
        }
    }

    public static void MensajePopupWindow(String _mensaje) {
        Timer timerPopupHide = new Timer();
        final PopupWindow popupWindow = new PopupWindow(_mensaje);
        popupWindow.activarPopupWindow();
        popupWindow.showMensaje(_mensaje);
        timerPopupHide.schedule(new TimerTask() {

            public void run() {
                popupWindow.hideWindow();
            }
        }, 10000);
    }

    public static void MensajePopupWindow(String _mensaje, String _archivo) {
        Timer timerPopupHide = new Timer();
        final PopupWindow popupWindow = new PopupWindow(_mensaje, _archivo);
        popupWindow.activarPopupWindow();
        popupWindow.showMensaje(_mensaje);
        timerPopupHide.schedule(new TimerTask() {

            public void run() {
                popupWindow.hideWindow();
            }
        }, 10000);
    }

    public static void MensajePopupWindowConVentana(String _mensaje, JDialog _ventana) {
        Timer timerPopupHide = new Timer();
        final PopupWindow popupWindow = new PopupWindow(_mensaje, _ventana);
        popupWindow.activarPopupWindow();
        popupWindow.showMensaje(_mensaje);
        timerPopupHide.schedule(new TimerTask() {

            public void run() {
                popupWindow.hideWindow();
            }
        }, 10000);
    }

    public static boolean validaUsuarioEnGrupo(String _grupo) {
        try {
            String Query = "SELECT '" + getSQLUser() + "' IN (SELECT usename FROM pg_user WHERE (SELECT usesysid = any(grolist) FROM pg_group WHERE groname = '" + _grupo + "'))";
            ResultSet Reg = OP_Proced.exConsulta(Query);
            if (!Reg.next()) {
                return false;
            } else if (Reg.getString(1).equalsIgnoreCase("f")) {
                return false;
            } else {
                return true;
            }
        } catch (SQLException x) {
            OP_Proced.Mensaje(x.getMessage(), "Error");
            return false;
        }
    }

    public static boolean Connected() {
        pgCon = LibSQL.getConnection();
        return LibSQL.isConnected();
    }

    public static boolean exActualizar(char ch, String Query) {
        boolean bol = true;
        String accion = "";
        if (Connected()) {
            try {
                Statement Stat = pgCon.createStatement();
                if (ch == 'a') {
                    accion = " insertar ";
                    Stat.executeUpdate(Query);
                } else if (ch == 'b') {
                    int result = JOptionPane.showConfirmDialog((Component) null, "�Est� seguro que desea eliminar el registro?", "Eliminaci�n", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        accion = " eliminar ";
                        Stat.executeUpdate(Query);
                    } else bol = false;
                } else if (ch == 'm') {
                    int result = JOptionPane.showConfirmDialog((Component) null, "�Est� seguro que desea guardar los cambios?", "Actualizaci�n", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        accion = " actualizar ";
                        Stat.executeUpdate(Query);
                    } else bol = false;
                }
                return bol;
            } catch (SQLException x) {
                Mensaje(x.getMessage(), "Error" + x.getErrorCode());
                return false;
            }
        } else {
            Mensaje("Error, el servidor de Bases de Datos est� desconectado\nIntente nuevamente m�s tarde", "Error de Conexi�n");
            return false;
        }
    }

    public static ResultSet exConsulta(String Query) {
        if (Connected()) {
            try {
                Statement Stat = pgCon.createStatement();
                ResultSet Resul1 = Stat.executeQuery(Query);
                return Resul1;
            } catch (SQLException x) {
                Mensaje(x.getMessage(), "Error");
                x.printStackTrace();
                return null;
            }
        } else {
            Mensaje("Error, el servidor de Bases de Datos est� desconectado\nIntente nuevamente m�s tarde", "Error de Conexi�n");
            return null;
        }
    }

    public static void prueba() {
        if (Connected()) {
            try {
                ResultSet x = pgCon.getMetaData().getSchemas();
                while (x.next()) {
                    System.out.print(x.getString(1));
                }
            } catch (SQLException x) {
                x.printStackTrace();
            }
        }
    }

    public static String getDependencia(int iddep) {
        return getCampo("Select nombre from dependencias where iddep = " + iddep);
    }

    /**
   * * ESTE METODO SE REPITE EN EL FORMULARIO 'PERSONAS.JAVA'
   * @param ImgDestino
   * @param ImgOrigen
   */
    public static void escalaImg(BufferedImage ImgOrigen, JLabel ImgDestino) {
        if (ImgOrigen != null) {
            Image img1 = null;
            if (ImgOrigen.getWidth() > ImgDestino.getWidth()) {
                img1 = ImgOrigen.getScaledInstance(ImgDestino.getWidth(), ImgDestino.getWidth() * ImgOrigen.getHeight() / ImgOrigen.getWidth(), Image.SCALE_SMOOTH);
            } else {
                img1 = ImgOrigen;
            }
            if (img1 != null) {
                ImgDestino.setIcon(new ImageIcon(img1));
            } else ImgDestino.setIcon(null);
        }
    }

    /**
   * ESTE METODO SE REPITE EN EL FORMULARIO 'PERSONAS.JAVA'
   * @return 
   * @param nombreImg
   * @param Img
   */
    public static BufferedImage getImagen(byte[] Img, String nombreImg) {
        File file = new File(getRutaIcono() + getSeparador() + nombreImg + ".jpg");
        try {
            FileOutputStream fis = new FileOutputStream(file);
            AffineTransform transform = new AffineTransform();
            AffineTransformOp scaleimg = new AffineTransformOp(transform, null);
            BufferedOutputStream output = new BufferedOutputStream(fis);
            byte[] imgBytes = Img;
            output.write(imgBytes);
            output.close();
            BufferedImage imagen = ImageIO.read(file);
            return imagen;
        } catch (Exception x) {
            return null;
        }
    }

    public static String getLastPath() {
        return lastpath;
    }

    public static void setLastPath(String _lastpath) {
        lastpath = _lastpath;
    }
}
