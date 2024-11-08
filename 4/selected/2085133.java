package org.digitall.apps.corporation.report.xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import javax.swing.JFileChooser;
import org.digitall.lib.common.ConfigFile;
import org.digitall.lib.components.Advisor;
import org.digitall.lib.data.Base64Coder;
import org.digitall.lib.sql.LibSQL;

/** FORMATOS DE TEXTO EN XML
TAMA�O DE LETRA (48 en XML es 24 en Word, 32 en XML es 16 en Word): 
  <w:r><w:rPr><w:sz w:val=\"48\"/><w:sz-cs w:val=\"48\"/></w:rPr><w:t> TEXTO </w:t></w:r>
SUBRAYADO SIMPLE:
  <w:r><w:rPr><w:u w:val=\"single\"/></w:rPr><w:t> TEXTO </w:t></w:r>
NEGRITA:
  <w:r><w:rPr><w:b/></w:rPr><w:t> TEXTO </w:t></w:r>
PARRAFO CENTRADO:
  <w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr></w:p>
*/
public class XMLWorkBook {

    private FileWriter xmlFile;

    private BufferedWriter log;

    private String xmlPath = "";

    private int nroHojas = 0;

    private String nombreLibro = "Libro sin nombre";

    private String[] tituloHoja;

    private String[] nombreHoja;

    private Vector[] vectorCabecera;

    private Vector[] vectorTipoDatos;

    private Vector[] vectorQuerys;

    private Vector[][] vectorColores;

    private int[] cantidadColumnas;

    private ConfigFile cfg = new ConfigFile("ddesktop.conf");

    public XMLWorkBook() {
    }

    public void createWorkBook() {
        xmlPath = cfg.getProperty("ReportsPath") + File.separator;
        if (AbreArchivo(xmlPath)) {
            writeWorkBook();
            if (CierraArchivo()) {
                Advisor.messagePopupWindow("<html><p align=center>Reporte generado con �xito<br><a href=>Click aqu� para verlo</a></p></html>", xmlPath);
            }
        }
    }

    private boolean AbreArchivo(String _path) {
        JFileChooser chooser = new JFileChooser(_path);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int returnVal = chooser.showSaveDialog(chooser.getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".xls")) {
                    path += ".xls";
                }
                cfg.setProperty("ReportsPath", chooser.getSelectedFile().getParent());
                xmlFile = new FileWriter(path, false);
                log = new BufferedWriter(xmlFile);
                return true;
            } catch (IOException x) {
                org.digitall.lib.components.Advisor.messageBox("Error de E/S", "Error XMLWorkBook");
                x.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean CierraArchivo() {
        try {
            xmlFile.close();
            return true;
        } catch (IOException x) {
            Advisor.messageBox("Error de E/S", "Error XMLWorkBook");
            x.printStackTrace();
            return false;
        }
    }

    private void write(String _cadena) {
        try {
            xmlFile.write(_cadena);
        } catch (IOException x) {
            Advisor.messageBox("Error de E/S", "Error XMLWorkBook");
            x.printStackTrace();
        }
    }

    private void writeln(String _cadena) {
        try {
            xmlFile.write(_cadena + "\n");
        } catch (IOException x) {
            Advisor.messageBox("Error de E/S", "Error XMLWorkBook");
            x.printStackTrace();
        }
    }

    private void writeln() {
        try {
            xmlFile.write("<w:p/>\n");
        } catch (IOException x) {
            Advisor.messageBox("Error de E/S", "Error XMLWorkBook");
            x.printStackTrace();
        }
    }

    private String writeFoto(int _idfoto, double _width, double _height) {
        try {
            ResultSet rs = LibSQL.exQuery("SELECT foto, ancho, alto from fotos where idfoto = " + _idfoto);
            if (rs != null) {
                write("<w:pict><w:binData w:name=\"wordml://" + _idfoto + ".jpg\">");
                while (rs.next()) {
                    byte[] imgBytes = rs.getBytes(1);
                    write(new String(Base64Coder.encode(imgBytes)));
                }
                double anchoimagen = rs.getDouble("ancho");
                double altoimagen = rs.getDouble("alto");
                double factor1 = _width / anchoimagen;
                double factor2 = _height / altoimagen;
                double factor = 1;
                if (factor1 > factor2) factor = factor1; else factor = factor2;
                double width = anchoimagen * factor;
                double height = altoimagen * factor;
                write("</w:binData><v:shape id=\"_x0000_i1025\" type=\"#_x0000_t75\" style=\"width:" + width + "pt" + ";height:" + height + "pt\">" + "<v:imagedata src=\"wordml://" + _idfoto + ".jpg\" o:title=\"Foto\"/>" + "</v:shape></w:pict>");
                rs.close();
            }
        } catch (Exception x) {
            Advisor.messageBox("Error al obtener la Foto", "Error XMLWorkBook");
            x.printStackTrace();
        }
        return "";
    }

    private void writeEncabezadoPie() {
        writeln("<!-- Inicio del Encabezado y Pie -->");
        writeln("<!-- Fin del Encabezado y Pie -->");
    }

    private String writeFoto(int indice) {
        try {
            ResultSet rs = LibSQL.exFunction("SELECT foto, ancho, alto from fotos where idfoto = " + (indice + 1));
            if (rs != null) {
                write("<w:pict><w:binData w:name=\"wordml://" + indice + ".jpg\">");
                while (rs.next()) {
                    byte[] imgBytes = rs.getBytes(1);
                    write(new String(Base64Coder.encode(imgBytes)));
                }
                write("</w:binData><v:shape id=\"_x0000_i1025\" type=\"#_x0000_t75\" style=\"width:" + rs.getString(2) + "pt" + ";height:" + rs.getString(3) + "pt\">" + "<v:imagedata src=\"wordml://" + indice + ".jpg\" o:title=\"Foto\"/>" + "</v:shape></w:pict>");
                rs.close();
            }
        } catch (Exception x) {
            Advisor.messageBox("Error al obtener la Foto", "Error XMLWorkBook");
            x.printStackTrace();
        }
        return "";
    }

    public void setNroHojas(int _nroHojas) {
        nroHojas = _nroHojas;
        tituloHoja = new String[nroHojas];
        nombreHoja = new String[nroHojas];
        cantidadColumnas = new int[nroHojas];
        vectorCabecera = new Vector[nroHojas];
        vectorTipoDatos = new Vector[nroHojas];
        vectorQuerys = new Vector[nroHojas];
        vectorColores = new Vector[nroHojas][1000];
    }

    public void setNombreLibro(String _nombreLibro) {
        nombreLibro = _nombreLibro;
    }

    public void setTituloHoja(int _nroHoja, String _tituloHoja) {
        if (_nroHoja < nroHojas) {
            tituloHoja[_nroHoja] = _tituloHoja;
        } else {
            Advisor.messageBox("El �ndice especificado supera el n�mero de Hojas", "Error de XMLWorkBook");
        }
    }

    public void setNombreHoja(int _nroHoja, String _nombreHoja) {
        if (_nroHoja < nroHojas) {
            nombreHoja[_nroHoja] = _nombreHoja;
        } else {
            Advisor.messageBox("El �ndice especificado supera el n�mero de Hojas", "Error de XMLWorkBook");
        }
    }

    public void setCabecera(int _nroHoja, Vector _cabecera) {
        if (_nroHoja < nroHojas) {
            vectorCabecera[_nroHoja] = _cabecera;
            cantidadColumnas[_nroHoja] = _cabecera.size();
        } else {
            Advisor.messageBox("El �ndice especificado supera el n�mero de Hojas", "Error de XMLWorkBook");
        }
    }

    public void setTipoDatos(int _nroHoja, Vector _tipoDatos) {
        if (_nroHoja < nroHojas) {
            vectorTipoDatos[_nroHoja] = _tipoDatos;
        } else {
            Advisor.messageBox("El �ndice especificado supera el n�mero de Hojas", "Error de XMLWorkBook");
        }
    }

    public void setQuerys(int _nroHoja, Vector _querys) {
        if (_nroHoja < nroHojas) {
            vectorQuerys[_nroHoja] = _querys;
        } else {
            Advisor.messageBox("El �ndice especificado supera el n�mero de Hojas", "Error de XMLWorkBook");
        }
    }

    public void setColores(int _nroHoja, int _nroQuery, Vector _colores) {
        if (_nroHoja < nroHojas) {
            vectorColores[_nroHoja][_nroQuery] = _colores;
        } else {
            Advisor.messageBox("El �ndice especificado supera el n�mero de Hojas", "Error de XMLWorkBook");
        }
    }

    private void writeWorkBook() {
        write("<?xml version=\"1.0\" encoding=\"iso-8859-1\" standalone=\"yes\"?>\n" + "<?mso-application progid=\"Excel.Sheet\"?>\n" + "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n" + " xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n" + " xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n" + " xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n" + " xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");
        write(" <DocumentProperties xmlns=\"urn:schemas-microsoft-com:office:office\">\n" + "  <Author>usuario</Author>\n" + "  <LastAuthor>usuario</LastAuthor>\n" + "  <Created>2006-08-30T08:12:27Z</Created>\n" + "  <Company>Secretar�a de Obras y Servicios P�blicos</Company>\n" + "  <Version>11.6568</Version>\n" + " </DocumentProperties>\n" + " <ExcelWorkbook xmlns=\"urn:schemas-microsoft-com:office:excel\">\n" + "  <WindowHeight>9210</WindowHeight>\n" + "  <WindowWidth>11595</WindowWidth>\n" + "  <WindowTopX>360</WindowTopX>\n" + "  <WindowTopY>90</WindowTopY>\n" + "  <ActiveSheet>1</ActiveSheet>\n" + "  <ProtectStructure>False</ProtectStructure>\n" + "  <ProtectWindows>False</ProtectWindows>\n" + " </ExcelWorkbook>\n");
        write(" <Styles>\n" + "  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n" + "   <Alignment ss:Vertical=\"Bottom\"/>\n" + "   <Borders/>\n" + "   <Font/>\n" + "   <Interior/>\n" + "   <NumberFormat/>\n" + "   <Protection/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Normal8\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Size=\"8\" />\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "  </Style>\n" + "  <Style ss:ID=\"Normal8Number\">\n" + "   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Size=\"8\" />\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "  </Style>\n" + "  <Style ss:ID=\"Normal8Percent\">\n" + "   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Size=\"8\" />\n" + "  <NumberFormat ss:Format=\"Percent\"/>" + "  </Style>\n" + "  <Style ss:ID=\"Normal8DateTime\">\n" + "   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Size=\"8\" />\n" + "  <NumberFormat ss:Format=\"dd/mm/yyyy;@\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Negrita8\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Size=\"8\" ss:Bold=\"1\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "  </Style>\n" + "  <Style ss:ID=\"s26\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "  </Style>\n" + "  <Style ss:ID=\"s28\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "  </Style>\n" + "  <Style ss:ID=\"s00\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#FFFFFF\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"s01\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#FFFFF9\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Rojo\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Color=\"#FFFFFF\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#FF0000\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Naranja\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#FF9900\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Amarillo\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#FFFF00\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Verde\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#99CC00\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Azul\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Color=\"#FFFFFF\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#0000FF\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Violeta\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#CC99FF\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Celeste\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#00FFFF\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Fucsia\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#FF00FF\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"Gris\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#C0C0C0\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + "  <Style ss:ID=\"OtroColor\">\n" + "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n" + "   <Borders>\n" + "    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n" + "   </Borders>\n" + "   <Font ss:FontName=\"Times New Roman\" x:Family=\"Roman\" ss:Bold=\"1\" ss:Size=\"8\"/>\n" + "  <NumberFormat ss:Format=\"Standard\"/>" + "   <Interior ss:Color=\"#00F9F9\" ss:Pattern=\"Solid\"/>\n" + "  </Style>\n" + " </Styles>\n");
        for (int i = 0; i < nroHojas; i++) {
            writeSheet(i);
        }
        write("</Workbook>\n");
    }

    private void writeSheet(int _nroHoja) {
        write("<!-- Inicio del cuerpo -->\n" + " <Worksheet ss:Name=\"" + nombreHoja[_nroHoja] + "\">\n" + "   <Names>\n" + "     <NamedRange ss:Name=\"_FilterDatabase\" ss:RefersTo=\"='" + nombreHoja[_nroHoja] + "'!R1C1:R1C" + cantidadColumnas[_nroHoja] + "\" ss:Hidden=\"1\"/>\n" + "   </Names>\n");
        String iniciotabla = "<!-- Inicio de la tabla -->\n" + "  <Table ss:ExpandedColumnCount=\"" + cantidadColumnas[_nroHoja] + "\" x:FullColumns=\"1\"\n" + "   x:FullRows=\"1\" ss:DefaultColumnWidth=\"216\">\n";
        for (int i = 0; i < cantidadColumnas[_nroHoja]; i++) {
            iniciotabla += "       <Column ss:Width=\"" + 216 + "\"/>\n";
        }
        iniciotabla += "   <Row ss:Height=\"16.5\">\n";
        for (int i = 0; i < cantidadColumnas[_nroHoja]; i++) {
            iniciotabla += "    <Cell ss:StyleID=\"Negrita8\"><Data ss:Type=\"String\">" + vectorCabecera[_nroHoja].elementAt(i).toString() + "</Data><NamedCell ss:Name=\"_FilterDatabase\"/></Cell>\n";
        }
        iniciotabla += "   </Row>\n";
        write(iniciotabla);
        for (int i = 0; i < vectorQuerys[_nroHoja].size(); i++) {
            writeCuerpoTabla(_nroHoja, vectorQuerys[_nroHoja].elementAt(i).toString(), vectorColores[_nroHoja][i]);
        }
        write("  </Table>\n" + "<!-- Fin de la tabla -->\n");
        write("  <WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">\n" + "   <PageSetup>\n" + "    <Header x:Margin=\"0\"/>\n" + "    <Footer x:Margin=\"0\"/>\n" + "    <PageMargins x:Bottom=\"0.984251969\" x:Left=\"0.78740157499999996\"\n" + "     x:Right=\"0.78740157499999996\" x:Top=\"0.984251969\"/>\n" + "   </PageSetup>\n" + "   <Selected/>\n" + "   <Panes>\n" + "    <Pane>\n" + "     <Number>3</Number>\n" + "     <ActiveRow>1</ActiveRow>\n" + "     <ActiveCol>1</ActiveCol>\n" + "    </Pane>\n" + "   </Panes>\n" + "   <ProtectObjects>False</ProtectObjects>\n" + "   <ProtectScenarios>False</ProtectScenarios>\n" + "  </WorksheetOptions>\n" + "  <AutoFilter x:Range=\"R1C1:R1C" + cantidadColumnas[_nroHoja] + "\" xmlns=\"urn:schemas-microsoft-com:office:excel\">\n" + "  </AutoFilter>\n" + " </Worksheet>\n");
    }

    private void writeCuerpoTabla(int _nroHoja, String _query, Vector _colores) {
        String fila = "";
        try {
            ResultSet Resulx = LibSQL.exFunction(_query);
            int filas = 0;
            while (Resulx.next()) {
                fila = "   <Row ss:Height=\"16.5\">\n";
                for (int i = 0; i < cantidadColumnas[_nroHoja]; i++) {
                    fila += "    <Cell ss:StyleID=\"" + _colores.elementAt(i) + "\"><Data ss:Type=\"" + vectorTipoDatos[_nroHoja].elementAt(i) + "\">" + Resulx.getString(i + 1) + "</Data></Cell>\n";
                }
                fila += "   </Row>\n";
                write(fila);
                filas++;
            }
            if (filas == 0) {
                fila = "   <Row ss:Height=\"16.5\">\n";
                for (int i = 0; i < cantidadColumnas[_nroHoja]; i++) {
                    fila += "    <Cell ss:StyleID=\"" + _colores.elementAt(i) + "\"><Data ss:Type=\"String\">" + "Sin Resultados" + "</Data></Cell>\n";
                }
                fila += "   </Row>\n";
                write(fila);
            }
        } catch (SQLException x) {
            Advisor.messageBox(x.getMessage(), "Error XMLWorkBook");
            x.printStackTrace();
        }
    }
}
