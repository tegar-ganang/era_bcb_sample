package GUI;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import Dominio.Utilidades;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
@SuppressWarnings("serial")
public class calendarioFecha extends JFrame implements ActionListener {

    JFrame padre = null;

    JTextField jTextFieldPadre = null;

    private JLabel etiqueta, horaL, lugarL, actividadL;

    private JScrollPane jScrollPane_IL;

    private JButton jButton1;

    private JTextField mes, fecha, horaT, lugarT;

    private JButton anterior, siguiente, ir, recordatorio, guardar, cancelar;

    private DefaultTableModel tabla;

    private JTable table;

    private JFrame marco;

    private JTextArea actividadT;

    String dias[] = { "L", "M", "X", "J", "V", "S", "D" };

    String di[] = { "Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom" };

    String meses[] = { "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE" };

    String meses2[] = { "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Set", "Oct", "Nov", "Dic" };

    String datoSeleccionado = "";

    int months[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    int anoActual = 0;

    int mesActual = 0;

    int diaActual = 0;

    int anoTemporal = 0;

    int mesTemporal = 0;

    int columnaSeleccionada = 0;

    int filaSeleccionada = 0;

    int cantidadArchivos = 1;

    int buffer = 2048;

    public static void main(String args[]) {
        calendarioFecha cal = new calendarioFecha(null, null);
        cal.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    public calendarioFecha(JFrame p, JTextField f) {
        super("Calendario");
        padre = p;
        jTextFieldPadre = f;
        Container c = getContentPane();
        c.setLayout(new FlowLayout());
        tabla = new DefaultTableModel() {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ;
        for (int i = 0; i < dias.length; i++) tabla.addColumn(dias[i]);
        for (int i = 0; i < 6; i++) {
            String rows[] = new String[7];
            tabla.addRow(rows);
        }
        table = new JTable(tabla);
        table.setPreferredScrollableViewportSize(new Dimension(500, 96));
        table.setSelectionMode(0);
        table.setCellSelectionEnabled(true);
        JPanel p5 = new JPanel(new BorderLayout(3, 3));
        JPanel p7 = new JPanel(new BorderLayout(5, 5));
        p7.add(p5, BorderLayout.SOUTH);
        p5.setLayout(null);
        p5.setPreferredSize(new java.awt.Dimension(216, 35));
        {
            jButton1 = new JButton();
            p5.add(jButton1);
            jButton1.setText("seleccionar");
            jButton1.setBounds(63, 0, 85, 25);
            jButton1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    jButton1ActionPerformed(evt);
                }
            });
        }
        {
            JPanel p6 = new JPanel(new BorderLayout(10, 10));
            p7.add(p6, BorderLayout.NORTH);
            {
                JPanel p3 = new JPanel(new BorderLayout());
                p6.add(p3, BorderLayout.NORTH);
                {
                    etiqueta = new JLabel(establecerHora());
                    p3.add(etiqueta);
                    etiqueta.setBounds(121, -5, 90, 18);
                }
                {
                    fecha = new JTextField(10);
                    p3.add(fecha);
                    fecha.setEditable(false);
                    fecha.setBounds(0, -5, 96, 17);
                }
                {
                    anterior = new JButton("< Anterior");
                    p3.add(anterior);
                    anterior.addActionListener(this);
                    anterior.setMnemonic('A');
                    anterior.setToolTipText("Mostrar el mes anterior");
                    anterior.setBounds(2, 20, 58, 20);
                    anterior.setText("<< ");
                }
                {
                    mes = new JTextField(10);
                    p3.add(mes);
                    mes.setEditable(false);
                    mes.setBounds(66, 20, 82, 20);
                    mes.setBackground(new java.awt.Color(255, 255, 0));
                }
                {
                    siguiente = new JButton("Siguiente >>");
                    p3.add(siguiente);
                    siguiente.addActionListener(this);
                    siguiente.setMnemonic('S');
                    siguiente.setToolTipText("Mostrar el mes siguiente");
                    siguiente.setBounds(154, 20, 58, 20);
                    siguiente.setText(">>");
                }
                p3.setLayout(null);
                p3.setPreferredSize(new java.awt.Dimension(216, 59));
            }
            {
                JPanel p4 = new JPanel(new GridLayout(1, 4, 3, 3));
                p6.add(p4, BorderLayout.SOUTH);
                {
                    recordatorio = new JButton("Recordatorio");
                    p4.add(recordatorio);
                    recordatorio.addActionListener(this);
                    recordatorio.setMnemonic('R');
                    recordatorio.setToolTipText("Crear o ver un recordatorio");
                    recordatorio.setPreferredSize(new java.awt.Dimension(52, 23));
                }
                {
                    ir = new JButton("Ir a...");
                    p4.add(ir);
                    ir.addActionListener(this);
                    ir.setMnemonic('I');
                    ir.setToolTipText("Escoger un mes o ver el mes actual");
                    ir.setBounds(1, -1, 58, 23);
                }
            }
        }
        JPanel p8 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        {
            jScrollPane_IL = new JScrollPane(table);
            p8.add(jScrollPane_IL);
            jScrollPane_IL.setBounds(0, 54, 226, 122);
        }
        p8.add(p7);
        p7.setBounds(5, 5, 216, 213);
        c.add(p8);
        p8.setLayout(null);
        p8.setPreferredSize(new java.awt.Dimension(226, 218));
        establecerFechaActual();
        mostrarEnTabla(anoActual, mesActual);
        Utilidades.CentrarJFrame(this);
        this.setSize(240, 250);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setResizable(false);
        show();
    }

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == anterior) mesAnterior();
        if (e.getSource() == siguiente) mesSiguiente();
        if (e.getSource() == ir) irA();
        if (e.getSource() == recordatorio) opcionRecordatorio();
        if (e.getSource() == cancelar) {
            marco.hide();
            marco.dispose();
        }
        if (e.getSource() == guardar) guardarRecordatorio();
    }

    public String establecerHora() {
        Date horas = new Date();
        Date minutos = new Date();
        Date segundos = new Date();
        String h = String.valueOf(horas.getHours());
        String m = String.valueOf(minutos.getMinutes());
        String s = String.valueOf(segundos.getSeconds());
        String hora = laHora(h) + ":" + m + ":" + s + " " + meridiano(h);
        return (hora);
    }

    public String laHora(String ho) {
        int a = Integer.parseInt(ho);
        String horas[] = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11" };
        String retorno = "";
        if (a == 0) retorno = "12"; else if (a >= 13 && a <= 23) retorno = horas[a - 13]; else retorno = ho;
        return (retorno);
    }

    public String meridiano(String ho) {
        int b = Integer.parseInt(ho);
        String retorno = "";
        if (b >= 12 && b <= 23) retorno = "pm"; else retorno = "am";
        return (retorno);
    }

    public void establecerFechaActual() {
        String fechaTotal = String.valueOf(new Date());
        mes.setText(meses[month(fechaTotal.substring(4, 7))] + " " + fechaTotal.substring(fechaTotal.length() - 4, fechaTotal.length()));
        fecha.setText(di[day(fechaTotal.substring(0, 3))] + ", " + fechaTotal.substring(8, 10) + " " + meses2[month(fechaTotal.substring(4, 7))] + " " + fechaTotal.substring(fechaTotal.length() - 4, fechaTotal.length()));
        anoActual = Integer.parseInt(fechaTotal.substring(fechaTotal.length() - 4, fechaTotal.length()));
        mesActual = month(fechaTotal.substring(4, 7));
        diaActual = Integer.parseInt(fechaTotal.substring(8, 10));
        anoTemporal = anoActual;
        mesTemporal = mesActual;
    }

    public int month(String m) {
        int mo = 0;
        if (m.equals("Ene") || m.equals("Jan")) mo = 0; else if (m.equals("Feb")) mo = 1; else if (m.equals("Mar")) mo = 2; else if (m.equals("Abr") || m.equals("Apr")) mo = 3; else if (m.equals("May")) mo = 4; else if (m.equals("Jun")) mo = 5; else if (m.equals("Jul")) mo = 6; else if (m.equals("Ago") || m.equals("Aug")) mo = 7; else if (m.equals("Sep") || m.equals("Set")) mo = 8; else if (m.equals("Oct")) mo = 9; else if (m.equals("Nov")) mo = 10; else mo = 11;
        return (mo);
    }

    public int day(String m) {
        int mo = 0;
        if (m.equals("Lun") || m.equals("Mon")) mo = 0; else if (m.equals("Mar") || m.equals("Tue")) mo = 1; else if (m.equals("Mie") || m.equals("Wed")) mo = 2; else if (m.equals("Jue") || m.equals("Thu")) mo = 3; else if (m.equals("Vie") || m.equals("Fri")) mo = 4; else if (m.equals("Sab") || m.equals("Sat")) mo = 5; else if (m.equals("Dom") || m.equals("Sun")) mo = 6;
        return (mo);
    }

    public boolean bisiesto(int a) {
        boolean retorno = false;
        if (a % 4 == 0 || a % 100 == 0 || a % 400 == 0) retorno = true;
        return (retorno);
    }

    public void mostrarEnTabla(int ac, int ma) {
        int columna = primerDia(ac, ma);
        int fila = 0;
        mes.setText(meses[ma] + " " + ac);
        if (bisiesto(ac)) months[1] = 29; else months[1] = 28;
        for (int i = 1; i <= months[ma]; i++) {
            if (chequearMarca(ac, ma, i)) {
                if (ac == anoActual && ma == mesActual && i == diaActual) table.setValueAt("<" + marca(ac, ma, i) + ">", fila, columna); else table.setValueAt(marca(ac, ma, i), fila, columna);
            } else {
                if (ac == anoActual && ma == mesActual && i == diaActual) table.setValueAt("<" + String.valueOf(i) + ">", fila, columna); else table.setValueAt(String.valueOf(i), fila, columna);
            }
            if (columna == 6) {
                columna = 0;
                fila++;
            } else columna++;
        }
    }

    public int primerDia(int a, int m) {
        String d = String.valueOf((new GregorianCalendar(a, m, 1)).getTime());
        return (day(d.substring(0, 3)));
    }

    public void mesAnterior() {
        limpiarTabla();
        if (mesTemporal == 0) {
            mesTemporal = 11;
            anoTemporal--;
        } else mesTemporal--;
        mostrarEnTabla(anoTemporal, mesTemporal);
    }

    public void limpiarTabla() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                table.setValueAt("", i, j);
            }
        }
    }

    public void mesSiguiente() {
        limpiarTabla();
        if (mesTemporal == 11) {
            mesTemporal = 0;
            anoTemporal++;
        } else mesTemporal++;
        mostrarEnTabla(anoTemporal, mesTemporal);
    }

    public void irA() {
        JLabel seleccion = new JLabel("Seleccione una opci�n: ");
        JRadioButton escoger = new JRadioButton("Escoger un mes", true);
        JRadioButton actual = new JRadioButton("Volver a la fecha actual");
        ButtonGroup mx = new ButtonGroup();
        mx.add(escoger);
        mx.add(actual);
        JPanel i1 = new JPanel(new GridLayout(3, 1, 3, 3));
        i1.add(seleccion);
        i1.add(escoger);
        i1.add(actual);
        JOptionPane.showMessageDialog(null, i1, "Ir a...", JOptionPane.QUESTION_MESSAGE);
        if (escoger.isSelected()) escogerMes(); else fechaActual();
    }

    public void fechaActual() {
        anoTemporal = anoActual;
        mesTemporal = mesActual;
        limpiarTabla();
        mostrarEnTabla(anoActual, mesActual);
    }

    public void escogerMes() {
        try {
            JLabel titulo1 = new JLabel("Digite el a�o y escoja el mes");
            JLabel titulo2 = new JLabel("al cual desea ir");
            JTextField aa = new JTextField(5);
            JComboBox me = new JComboBox();
            me.setMaximumRowCount(5);
            for (int i = 0; i < meses.length; i++) me.addItem(meses[i]);
            JPanel e1 = new JPanel(new GridLayout(2, 1));
            e1.add(titulo1);
            e1.add(titulo2);
            JPanel e2 = new JPanel(new GridLayout(2, 1, 3, 3));
            e2.add(new JLabel("A�o: "));
            e2.add(aa);
            e2.add(new JLabel("Mes: "));
            e2.add(me);
            JPanel e3 = new JPanel(new BorderLayout(3, 3));
            e3.add(e1, BorderLayout.NORTH);
            e3.add(e2, BorderLayout.SOUTH);
            JOptionPane.showMessageDialog(null, e3, "Selecci�n", JOptionPane.QUESTION_MESSAGE);
            int a = Integer.parseInt(aa.getText());
            int m = me.getSelectedIndex();
            if (a > 0) {
                anoTemporal = a;
                mesTemporal = m;
                limpiarTabla();
                mostrarEnTabla(a, m);
            } else JOptionPane.showMessageDialog(null, "Debe digitar numeros enteros positivos en el espacio para el a�o", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Debe digitar numeros enteros positivos en el espacio para el a�o", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String obtenerDato(int fila, int columna) {
        String dato = "";
        dato = String.valueOf(table.getValueAt(fila, columna));
        return (dato);
    }

    public void opcionRecordatorio() {
        columnaSeleccionada = table.getSelectedColumn();
        filaSeleccionada = table.getSelectedRow();
        if (columnaSeleccionada != -1 && filaSeleccionada != -1) {
            datoSeleccionado = obtenerDato(filaSeleccionada, columnaSeleccionada);
            if (datoSeleccionado.equals("null") || datoSeleccionado.equals("")) JOptionPane.showMessageDialog(null, "Debe seleccionar una celda que no est� vac�a", "Error", JOptionPane.WARNING_MESSAGE); else {
                JLabel pregunta = new JLabel("�Qu� desea hacer?");
                JRadioButton crear = new JRadioButton("Crear un Recordatorio", true);
                JRadioButton ver = new JRadioButton("Ver un Recordatorio");
                ButtonGroup cv = new ButtonGroup();
                cv.add(crear);
                cv.add(ver);
                JPanel o1 = new JPanel(new GridLayout(3, 1, 3, 3));
                o1.add(pregunta);
                o1.add(crear);
                o1.add(ver);
                JOptionPane.showMessageDialog(null, o1, "Selecci�n", JOptionPane.QUESTION_MESSAGE);
                if (crear.isSelected()) crearRecordatorioInterfaz(); else verRecordatorio();
            }
        } else JOptionPane.showMessageDialog(null, "Primero debe seleccionar una celda de la cuadricula", "Error", JOptionPane.WARNING_MESSAGE);
    }

    public boolean fechaValida(int a1, int a2, int m1, int m2, int d1, int d2) {
        boolean retorno = false;
        if (a2 > a1) retorno = true; else if (a2 == a1) {
            if (m2 > m1) retorno = true; else if (m2 == m1) {
                if (d2 >= d1) retorno = true;
            }
        }
        return (retorno);
    }

    public void crearRecordatorioInterfaz() {
        int diaTemporal = Integer.parseInt(identificarDato(datoSeleccionado));
        if (fechaValida(anoActual, anoTemporal, mesActual, mesTemporal, diaActual, diaTemporal)) {
            marco = new JFrame("Crear un Recordatorio");
            marco.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    marco.hide();
                    marco.dispose();
                }
            });
            horaL = new JLabel("Hora:");
            lugarL = new JLabel("Lugar:");
            actividadL = new JLabel("Actividad:");
            horaT = new JTextField(5);
            lugarT = new JTextField(10);
            actividadT = new JTextArea(10, 35);
            guardar = new JButton("Guardar");
            guardar.addActionListener(this);
            guardar.setMnemonic('G');
            guardar.setToolTipText("Guardar el recordatorio");
            cancelar = new JButton("Cancelar");
            cancelar.addActionListener(this);
            cancelar.setMnemonic('C');
            cancelar.setToolTipText("Cierra la ventana");
            JPanel c1 = new JPanel(new GridLayout(2, 1));
            c1.add(horaL);
            c1.add(horaT);
            JPanel c2 = new JPanel(new GridLayout(2, 1));
            c2.add(lugarL);
            c2.add(lugarT);
            JPanel c3 = new JPanel(new BorderLayout(1, 1));
            c3.add(actividadL, BorderLayout.NORTH);
            c3.add(new JScrollPane(actividadT), BorderLayout.SOUTH);
            JPanel c4 = new JPanel(new BorderLayout(5, 5));
            c4.add(c1, BorderLayout.NORTH);
            c4.add(c2, BorderLayout.CENTER);
            c4.add(c3, BorderLayout.SOUTH);
            JPanel c5 = new JPanel(new GridLayout(1, 2, 3, 3));
            c5.add(guardar);
            c5.add(cancelar);
            JPanel c6 = new JPanel(new BorderLayout(5, 5));
            c6.add(c4, BorderLayout.NORTH);
            c6.add(c5, BorderLayout.SOUTH);
            JPanel c7 = new JPanel(new FlowLayout(FlowLayout.CENTER));
            c7.add(c6);
            marco.getContentPane().add(c7);
            marco.setSize(300, 375);
            marco.show();
        } else JOptionPane.showMessageDialog(null, "Solo se permite crear un recordatorio\n" + "con una fecha igual o superior a la fecha actual", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public boolean espaciosLlenos() {
        boolean retorno = false;
        if (!horaT.getText().equals("") && !lugarT.getText().equals("") && !actividadT.getText().equals("")) retorno = true;
        return (retorno);
    }

    public String identificarDato(String d) {
        char dt[] = d.toCharArray();
        String retorno = "";
        boolean hay = true;
        for (int i = 0; i < dt.length; i++) {
            if (String.valueOf(dt[i]).equals("<") || String.valueOf(dt[i]).equals(">") || String.valueOf(dt[i]).equals("*") || String.valueOf(dt[i]).equals("#") || String.valueOf(dt[i]).equals("&") || String.valueOf(dt[i]).equals(" ")) hay = false; else hay = true;
            if (hay) retorno += String.valueOf(dt[i]);
        }
        return (retorno);
    }

    public void guardarRecordatorio() {
        try {
            if (espaciosLlenos()) {
                guardarCantidad();
                String dat = "";
                String filenametxt = String.valueOf("recordatorio" + cantidadArchivos + ".txt");
                String filenamezip = String.valueOf("recordatorio" + cantidadArchivos + ".zip");
                cantidadArchivos++;
                dat += identificarDato(datoSeleccionado) + "\n";
                dat += String.valueOf(mesTemporal) + "\n";
                dat += String.valueOf(anoTemporal) + "\n";
                dat += horaT.getText() + "\n";
                dat += lugarT.getText() + "\n";
                dat += actividadT.getText() + "\n";
                File archivo = new File(filenametxt);
                FileWriter fw = new FileWriter(archivo);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter salida = new PrintWriter(bw);
                salida.print(dat);
                salida.close();
                BufferedInputStream origin = null;
                FileOutputStream dest = new FileOutputStream(filenamezip);
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
                byte data[] = new byte[buffer];
                File f = new File(filenametxt);
                FileInputStream fi = new FileInputStream(f);
                origin = new BufferedInputStream(fi, buffer);
                ZipEntry entry = new ZipEntry(filenametxt);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, buffer)) != -1) out.write(data, 0, count);
                out.close();
                JOptionPane.showMessageDialog(null, "El recordatorio ha sido guardado con exito", "Recordatorio Guardado", JOptionPane.INFORMATION_MESSAGE);
                marco.hide();
                marco.dispose();
                establecerMarca();
                table.clearSelection();
            } else JOptionPane.showMessageDialog(null, "Debe llenar los espacios de Hora, Lugar y Actividad", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error en: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void establecerMarca() {
        mostrarEnTabla(anoTemporal, mesTemporal);
    }

    public void guardarCantidad() {
        try {
            String can = String.valueOf(cantidadArchivos);
            File archivo = new File("cantidadArchivos.txt");
            FileWriter fw = new FileWriter(archivo);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter salida = new PrintWriter(bw);
            salida.print(can);
            salida.close();
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream("cantidadArchivos.zip");
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[buffer];
            File f = new File("cantidadArchivos.txt");
            FileInputStream fi = new FileInputStream(f);
            origin = new BufferedInputStream(fi, buffer);
            ZipEntry entry = new ZipEntry("cantidadArchivos.txt");
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, buffer)) != -1) out.write(data, 0, count);
            out.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error en: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void verRecordatorio() {
        try {
            cantidadArchivos = obtenerCantidad() + 1;
            boolean existe = false;
            String filenametxt = "";
            String filenamezip = "";
            String hora = "";
            String lugar = "";
            String actividad = "";
            String linea = "";
            int dia = 0;
            int mes = 0;
            int ano = 0;
            for (int i = 1; i < cantidadArchivos; i++) {
                filenamezip = "recordatorio" + i + ".zip";
                filenametxt = "recordatorio" + i + ".txt";
                BufferedOutputStream dest = null;
                BufferedInputStream is = null;
                ZipEntry entry;
                ZipFile zipfile = new ZipFile(filenamezip);
                Enumeration e = zipfile.entries();
                while (e.hasMoreElements()) {
                    entry = (ZipEntry) e.nextElement();
                    is = new BufferedInputStream(zipfile.getInputStream(entry));
                    int count;
                    byte data[] = new byte[buffer];
                    FileOutputStream fos = new FileOutputStream(entry.getName());
                    dest = new BufferedOutputStream(fos, buffer);
                    while ((count = is.read(data, 0, buffer)) != -1) dest.write(data, 0, count);
                    dest.flush();
                    dest.close();
                    is.close();
                }
                DataInputStream input = new DataInputStream(new FileInputStream(filenametxt));
                dia = Integer.parseInt(input.readLine());
                mes = Integer.parseInt(input.readLine());
                ano = Integer.parseInt(input.readLine());
                if (dia == Integer.parseInt(identificarDato(datoSeleccionado))) {
                    existe = true;
                    hora = input.readLine();
                    lugar = input.readLine();
                    while ((linea = input.readLine()) != null) actividad += linea + "\n";
                    verRecordatorioInterfaz(hora, lugar, actividad);
                    hora = "";
                    lugar = "";
                    actividad = "";
                }
                input.close();
            }
            if (!existe) JOptionPane.showMessageDialog(null, "No existe un recordatorio guardado\n" + "para el " + identificarDato(datoSeleccionado) + " de " + meses[mesTemporal].toLowerCase() + " del a�o " + anoTemporal, "No existe", JOptionPane.INFORMATION_MESSAGE);
            table.clearSelection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error en: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void verRecordatorioInterfaz(String h, String l, String a) {
        marco = new JFrame("Ver un Recordatorio");
        marco.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                marco.hide();
                marco.dispose();
            }
        });
        horaL = new JLabel("Hora:");
        lugarL = new JLabel("Lugar:");
        actividadL = new JLabel("Actividad:");
        horaT = new JTextField(5);
        horaT.setEditable(false);
        lugarT = new JTextField(10);
        lugarT.setEditable(false);
        actividadT = new JTextArea(10, 35);
        actividadT.setEditable(false);
        JPanel c1 = new JPanel(new GridLayout(2, 1));
        c1.add(horaL);
        c1.add(horaT);
        JPanel c2 = new JPanel(new GridLayout(2, 1));
        c2.add(lugarL);
        c2.add(lugarT);
        JPanel c3 = new JPanel(new BorderLayout(1, 1));
        c3.add(actividadL, BorderLayout.NORTH);
        c3.add(new JScrollPane(actividadT), BorderLayout.SOUTH);
        JPanel c4 = new JPanel(new BorderLayout(5, 5));
        c4.add(c1, BorderLayout.NORTH);
        c4.add(c2, BorderLayout.CENTER);
        c4.add(c3, BorderLayout.SOUTH);
        JPanel c5 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        c5.add(c4);
        marco.getContentPane().add(c5);
        horaT.setText(h);
        lugarT.setText(l);
        actividadT.setText(a);
        marco.setSize(300, 350);
        marco.show();
    }

    public int obtenerCantidad() {
        try {
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;
            ZipEntry entry;
            ZipFile zipfile = new ZipFile("cantidadArchivos.zip");
            Enumeration e = zipfile.entries();
            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
                is = new BufferedInputStream(zipfile.getInputStream(entry));
                int count;
                byte data[] = new byte[buffer];
                FileOutputStream fos = new FileOutputStream(entry.getName());
                dest = new BufferedOutputStream(fos, buffer);
                while ((count = is.read(data, 0, buffer)) != -1) dest.write(data, 0, count);
                dest.flush();
                dest.close();
                is.close();
            }
            DataInputStream input = new DataInputStream(new FileInputStream("cantidadArchivos.txt"));
            int a = Integer.parseInt(input.readLine());
            input.close();
            return (a);
        } catch (Exception e) {
            return (0);
        }
    }

    public boolean chequearMarca(int a, int m, int d) {
        boolean existe = false;
        try {
            cantidadArchivos = obtenerCantidad() + 1;
            String filenametxt = "";
            String filenamezip = "";
            int dia = 0;
            int mes = 0;
            int ano = 0;
            for (int i = 1; i < cantidadArchivos; i++) {
                filenamezip = "recordatorio" + i + ".zip";
                filenametxt = "recordatorio" + i + ".txt";
                BufferedOutputStream dest = null;
                BufferedInputStream is = null;
                ZipEntry entry;
                ZipFile zipfile = new ZipFile(filenamezip);
                Enumeration e = zipfile.entries();
                while (e.hasMoreElements()) {
                    entry = (ZipEntry) e.nextElement();
                    is = new BufferedInputStream(zipfile.getInputStream(entry));
                    int count;
                    byte data[] = new byte[buffer];
                    FileOutputStream fos = new FileOutputStream(entry.getName());
                    dest = new BufferedOutputStream(fos, buffer);
                    while ((count = is.read(data, 0, buffer)) != -1) dest.write(data, 0, count);
                    dest.flush();
                    dest.close();
                    is.close();
                }
                DataInputStream input = new DataInputStream(new FileInputStream(filenametxt));
                dia = Integer.parseInt(input.readLine());
                mes = Integer.parseInt(input.readLine());
                ano = Integer.parseInt(input.readLine());
                if (ano == a && mes == m && dia == d) existe = true;
                input.close();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error en: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return (existe);
    }

    public String marca(int a, int m, int d) {
        String retorno = "";
        if (a < anoActual) retorno = String.valueOf(d) + " &"; else if (a > anoActual) retorno = String.valueOf(d) + " #"; else {
            if (m < mesActual) retorno = String.valueOf(d) + " &"; else if (m > mesActual) retorno = String.valueOf(d) + " #"; else {
                if (d < diaActual) retorno = String.valueOf(d) + " &"; else if (d > diaActual) retorno = String.valueOf(d) + " #"; else retorno = String.valueOf(d) + " *";
            }
        }
        return (retorno);
    }

    private String mesLetraNumero(String mesLetra) {
        String mesNumero = "";
        if (mesLetra.equals("ENERO")) mesNumero = "1"; else if (mesLetra.equals("FEBRERO")) mesNumero = "2"; else if (mesLetra.equals("MARZO")) mesNumero = "3"; else if (mesLetra.equals("ABRIL")) mesNumero = "4"; else if (mesLetra.equals("MAYO")) mesNumero = "5"; else if (mesLetra.equals("JUNIO")) mesNumero = "6"; else if (mesLetra.equals("JULIO")) mesNumero = "7"; else if (mesLetra.equals("AGOSTO")) mesNumero = "8"; else if (mesLetra.equals("SEPTIEMBRE")) mesNumero = "9"; else if (mesLetra.equals("OCTUBRE")) mesNumero = "10"; else if (mesLetra.equals("NOVIEMBRE")) mesNumero = "11"; else if (mesLetra.equals("DICIEMBRE")) mesNumero = "12";
        return mesNumero;
    }

    private void jButton1ActionPerformed(ActionEvent evt) {
        System.out.println("jButton1.actionPerformed, event=" + evt);
        int fila = table.getSelectedRow();
        int columna = table.getSelectedColumn();
        String dd = table.getValueAt(fila, columna).toString();
        String[] f = mes.getText().split(" ");
        String mm = mesLetraNumero(f[0]);
        String aaa = f[1];
        int dia = -1;
        try {
            dia = Integer.parseInt(dd);
        } catch (Exception e) {
            Calendar calendario = new GregorianCalendar();
            dia = calendario.get(Calendar.DAY_OF_MONTH);
        }
        String fecha = Utilidades.ceros(String.valueOf(dia)) + "/" + Utilidades.ceros(String.valueOf(mm)) + "/" + aaa;
        System.out.println("Fecha Calendario: " + fecha);
        padre.setEnabled(true);
        jTextFieldPadre.setText(fecha);
        this.dispose();
    }
}
