package fireteam.orb.server.processors;

import fireteam.orb.server.processors.types.ObjAttr;
import fireteam.orb.server.processors.types.ObjAttrs;
import fireteam.orb.server.stub.StandardException;
import fireteam.orb.util.ObjUtil;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleResultSet;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hssf.util.Region;
import org.omg.CORBA.Any;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Tolik1
 */
public class ExcelReports {

    /**
	 * Поиск параметров в строке запроса, параметры должны быть типа :A и т.п. не используются знаки ?
	 * @param sSql	- строка запроса
	 * @return
	 */
    private static String[] findParameters(String sSql) {
        String sPrepared = sSql.replaceAll("('([^'])*')|(\"([^\"])*\")", "");
        ArrayList<String> arStr = new ArrayList<String>();
        Pattern pat = Pattern.compile(":(\\w*)");
        Matcher match = pat.matcher(sPrepared);
        while (match.find()) {
            for (int i = 1; i <= match.groupCount(); i++) {
                match.start(i);
                arStr.add(match.group(i));
                match.end(i);
            }
        }
        return arStr.toArray(new String[arStr.size()]);
    }

    private static void writeCell(HSSFRow row, ResultSetMetaData rData, ResultSet rSet, ReportSAXParser.Column c, HSSFCellStyle style) throws SQLException {
        String sResult = c.sFormat;
        HSSFCell cell = row.createCell(c.iNum);
        for (String sField : c.arsFields) {
            int iColIndex = -1;
            for (int i = 1; i <= rData.getColumnCount(); i++) {
                if (rData.getColumnName(i).equals(sField)) {
                    iColIndex = i;
                    break;
                }
            }
            if (iColIndex == -1) continue;
            switch(rData.getColumnType(iColIndex)) {
                case Types.DATE:
                    if (c.bSimpleType) {
                        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("m/d/yy"));
                        cell.setCellValue(rSet.getDate(iColIndex));
                        cell.setCellStyle(style);
                        return;
                    } else {
                        SimpleDateFormat fmt = new SimpleDateFormat(ResourceBundle.getBundle(ExcelReports.class.getName()).getString("DateFormat"));
                        sResult = sResult.replaceAll("\\{" + sField + "\\}", fmt.format(rSet.getDate(iColIndex)));
                    }
                    break;
                case Types.TIME:
                    if (c.bSimpleType) {
                        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("h:mm"));
                        cell.setCellValue(rSet.getTime(iColIndex));
                        cell.setCellStyle(style);
                        return;
                    } else {
                        SimpleDateFormat fmt = new SimpleDateFormat(ResourceBundle.getBundle(ExcelReports.class.getName()).getString("DateFormat"));
                        sResult = sResult.replaceAll("\\{" + sField + "\\}", fmt.format(rSet.getTime(iColIndex)));
                    }
                    break;
                case Types.TIMESTAMP:
                    if (c.bSimpleType) {
                        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("m/d/yy h:mm"));
                        cell.setCellValue(rSet.getTimestamp(iColIndex));
                        cell.setCellStyle(style);
                        return;
                    } else {
                        SimpleDateFormat fmt = new SimpleDateFormat(ResourceBundle.getBundle(ExcelReports.class.getName()).getString("DateFormat"));
                        sResult = sResult.replaceAll("\\{" + sField + "\\}", fmt.format(rSet.getTimestamp(iColIndex)));
                    }
                    break;
                case Types.VARCHAR:
                case Types.CHAR:
                    if (c.bSimpleType) {
                        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
                        cell.setCellValue(new HSSFRichTextString(rSet.getString(iColIndex)));
                        cell.setCellStyle(style);
                        return;
                    }
                    sResult = sResult.replaceAll("\\{" + sField + "\\}", rSet.getString(iColIndex));
                    break;
                case Types.INTEGER:
                    if (c.bSimpleType) {
                        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0"));
                        cell.setCellValue(rSet.getInt(iColIndex));
                        cell.setCellStyle(style);
                        return;
                    } else {
                        String sFieldValue = rSet.getString(iColIndex);
                        sResult = sResult.replaceAll("\\{" + sField + "\\}", sFieldValue);
                    }
                    break;
                case Types.DOUBLE:
                case Types.FLOAT:
                case Types.DECIMAL:
                case Types.NUMERIC:
                    if (c.bSimpleType) {
                        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0.00"));
                        cell.setCellValue(rSet.getDouble(iColIndex));
                        cell.setCellStyle(style);
                        return;
                    } else {
                        String sFieldValue = rSet.getString(iColIndex);
                        sResult = sResult.replaceAll("\\{" + sField + "\\}", sFieldValue);
                    }
                    break;
                default:
                    if (c.bSimpleType) {
                        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
                        cell.setCellType(HSSFCell.CELL_TYPE_STRING);
                        cell.setCellValue(new HSSFRichTextString(rSet.getString(iColIndex)));
                        cell.setCellStyle(style);
                        return;
                    } else {
                        sResult = sResult.replaceAll("\\{" + sField + "\\}", rSet.getString(iColIndex));
                    }
                    break;
            }
        }
        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
        cell.setCellValue(new HSSFRichTextString(sResult));
        cell.setCellStyle(style);
    }

    private static class ReportSAXParser extends DefaultHandler {

        class Picture {

            short row1, row2, col1, col2;

            String sSrc;

            Picture(Attributes attr) {
                row1 = attr.getValue("row1") != null ? Short.valueOf(attr.getValue("row1")) : 0;
                row2 = attr.getValue("row2") != null ? Short.valueOf(attr.getValue("row2")) : 0;
                col1 = attr.getValue("col1") != null ? Short.valueOf(attr.getValue("col1")) : 0;
                col2 = attr.getValue("col2") != null ? Short.valueOf(attr.getValue("col2")) : 0;
                sSrc = attr.getValue("src");
            }
        }

        class Column {

            String sFormat;

            short iNum;

            short iColspan;

            short iBorder;

            short iFontSize;

            short iWrap;

            String sFont;

            String arsFields[];

            boolean bSimpleType;

            short iWidth;

            String sLabel;

            byte rgbColor[];

            String[] findFields(String sFormat) {
                ArrayList<String> arStr = new ArrayList<String>();
                Pattern pat = Pattern.compile("\\{(\\w*)\\}");
                Matcher match = pat.matcher(sFormat);
                while (match.find()) {
                    for (int i = 1; i <= match.groupCount(); i++) {
                        match.start(i);
                        arStr.add(match.group(i));
                        match.end(i);
                    }
                }
                return arStr.toArray(new String[arStr.size()]);
            }

            Column(Attributes attr) {
                bSimpleType = false;
                sLabel = attr.getValue("label");
                sFormat = attr.getValue("format");
                sFont = attr.getValue("font");
                iNum = attr.getValue("num") != null ? Short.valueOf(attr.getValue("num")) : 0;
                iColspan = attr.getValue("colspan") != null ? Short.valueOf(attr.getValue("colspan")) : 0;
                iBorder = attr.getValue("border") != null ? Short.valueOf(attr.getValue("border")) : 0;
                iFontSize = attr.getValue("fontsize") != null ? Short.valueOf(attr.getValue("fontsize")) : 0;
                arsFields = findFields(sFormat);
                iWrap = attr.getValue("wrap") != null ? Short.valueOf(attr.getValue("wrap")) : 0;
                iWidth = attr.getValue("width") != null ? Short.valueOf(attr.getValue("width")) : 0;
                if (arsFields.length == 1 && sFormat.equalsIgnoreCase("{" + arsFields[0] + "}")) bSimpleType = true;
                String sVal = attr.getValue("color");
                if (sVal != null) {
                    String rgb[] = sVal.split("\\.");
                    rgbColor = new byte[] { (byte) (short) Short.valueOf(rgb[0]), (byte) (short) Short.valueOf(rgb[1]), (byte) (short) Short.valueOf(rgb[2]) };
                }
            }
        }

        ;

        class Row {

            short iNum;

            ArrayList<Column> arColumns = new ArrayList<Column>();

            Row(short iNum) {
                this.iNum = iNum;
            }
        }

        ;

        class Header {

            String sQuery;

            Picture picture;

            String sSheet;

            ArrayList<Row> arRows = new ArrayList<Row>();
        }

        ;

        class Rows {

            String sQuery;

            byte crEvenBack[] = { (byte) 255, (byte) 255, (byte) 255 };

            byte crOggBack[] = { (byte) 200, (byte) 200, (byte) 200 };

            byte crHeaderBack[] = { (byte) 120, (byte) 120, (byte) 120 };

            String sSheet;

            String sHeaderFont;

            short iHeaderFontSize;

            short iStartRow = 0;

            ArrayList<Column> arColumns = new ArrayList<Column>();

            Rows(Attributes attr) {
                String sVal = attr.getValue("even");
                if (sVal != null) {
                    String rgb[] = sVal.split("\\.");
                    crEvenBack = new byte[] { (byte) (short) Short.valueOf(rgb[0]), (byte) (short) Short.valueOf(rgb[1]), (byte) (short) Short.valueOf(rgb[2]) };
                }
                sVal = attr.getValue("ogg");
                if (sVal != null) {
                    String rgb[] = sVal.split("\\.");
                    crOggBack = new byte[] { (byte) (short) Short.valueOf(rgb[0]), (byte) (short) Short.valueOf(rgb[1]), (byte) (short) Short.valueOf(rgb[2]) };
                }
                sVal = attr.getValue("header");
                if (sVal != null) {
                    String rgb[] = sVal.split("\\.");
                    crHeaderBack = new byte[] { (byte) (short) Short.valueOf(rgb[0]), (byte) (short) Short.valueOf(rgb[1]), (byte) (short) Short.valueOf(rgb[2]) };
                }
                sSheet = attr.getValue("sheet");
                iStartRow = attr.getValue("start") != null ? Short.valueOf(attr.getValue("start")) : 0;
                sHeaderFont = attr.getValue("hfont");
                iHeaderFontSize = attr.getValue("hfontsize") != null ? Short.valueOf(attr.getValue("hfontsize")) : 0;
            }
        }

        private Header m_Header;

        private Header m_Footer;

        private Rows m_Rows[];

        private int m_iCurrent = -1;

        private boolean m_bQuery = false;

        private static final int HEADER = 0, FOOTER = 1, ROWS = 2;

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("Header")) {
                m_iCurrent = -1;
            }
            if (qName.equals("Footer")) {
                m_iCurrent = -1;
            }
            if (qName.equals("Rows")) {
                m_iCurrent = -1;
            }
            if (qName.equals("Query")) {
                m_bQuery = false;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("Header")) {
                m_Header = new Header();
                m_Header.sSheet = attributes.getValue("sheet");
                m_iCurrent = HEADER;
                return;
            }
            if (qName.equals("Footer")) {
                m_Footer = new Header();
                m_iCurrent = FOOTER;
                return;
            }
            if (qName.equals("Rows")) {
                if (m_Rows == null) {
                    m_Rows = new Rows[1];
                } else {
                    Rows[] oldRows = m_Rows;
                    m_Rows = new Rows[m_Rows.length + 1];
                    System.arraycopy(oldRows, 0, m_Rows, 0, oldRows.length);
                }
                m_Rows[m_Rows.length - 1] = new Rows(attributes);
                m_iCurrent = ROWS;
                return;
            }
            if (qName.equals("Row")) {
                short iRowNum = 0;
                String sValue = attributes.getValue("num");
                if (sValue != null) iRowNum = Short.valueOf(sValue);
                switch(m_iCurrent) {
                    case HEADER:
                        m_Header.arRows.add(new Row(iRowNum));
                        break;
                    case FOOTER:
                        m_Footer.arRows.add(new Row(iRowNum));
                        break;
                    case ROWS:
                        break;
                }
                return;
            }
            if (qName.equals("Column")) {
                switch(m_iCurrent) {
                    case HEADER:
                        m_Header.arRows.get(m_Header.arRows.size() - 1).arColumns.add(new Column(attributes));
                        break;
                    case FOOTER:
                        m_Footer.arRows.get(m_Footer.arRows.size() - 1).arColumns.add(new Column(attributes));
                        break;
                    case ROWS:
                        m_Rows[m_Rows.length - 1].arColumns.add(new Column(attributes));
                        break;
                }
                return;
            }
            if (qName.equals("Query")) {
                m_bQuery = true;
                return;
            }
            if (qName.equals("Picture")) {
                m_Header.picture = new Picture(attributes);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String val = new String(ch, start, length);
            if (val == null || val.length() == 0 || !m_bQuery) return;
            switch(m_iCurrent) {
                case HEADER:
                    if (m_Header.sQuery != null) m_Header.sQuery = m_Header.sQuery + val; else m_Header.sQuery = val;
                    break;
                case FOOTER:
                    if (m_Footer.sQuery != null) m_Footer.sQuery = m_Footer.sQuery + val; else m_Footer.sQuery = val;
                    break;
                case ROWS:
                    if (m_Rows[m_Rows.length - 1].sQuery != null) m_Rows[m_Rows.length - 1].sQuery = m_Rows[m_Rows.length - 1].sQuery + val; else m_Rows[m_Rows.length - 1].sQuery = val;
                    break;
            }
        }

        public boolean isValidXML(InputStream is) {
            try {
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                String location = ResourceBundle.getBundle(ExcelReports.class.getName()).getString("XMLFiles");
                String sSchemaName = ResourceBundle.getBundle(ExcelReports.class.getName()).getString("XMLSchema");
                Schema schema = sf.newSchema(ExcelReports.class.getResource(location + "/" + sSchemaName));
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(is));
                return true;
            } catch (Exception ex) {
                Logger.getLogger("fireteam").log(Level.SEVERE, null, ex);
            }
            return false;
        }

        public void parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String location = ResourceBundle.getBundle(ExcelReports.class.getName()).getString("XMLFiles");
            String sSchemaName = ResourceBundle.getBundle(ExcelReports.class.getName()).getString("XMLSchema");
            Schema schema = sf.newSchema(ExcelReports.class.getResource(location + "/" + sSchemaName));
            SAXParserFactory sax = SAXParserFactory.newInstance();
            sax.setSchema(schema);
            SAXParser parser = sax.newSAXParser();
            parser.parse(is, this);
        }

        private class Style {

            short iBorder;

            short iFontSize;

            short iWrap;

            String sFont;

            short color;

            short dataFormat;

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final ReportSAXParser.Style other = (ReportSAXParser.Style) obj;
                if (this.iBorder != other.iBorder) {
                    return false;
                }
                if (this.iFontSize != other.iFontSize) {
                    return false;
                }
                if (this.iWrap != other.iWrap) {
                    return false;
                }
                if (this.sFont == null || !this.sFont.equals(other.sFont)) {
                    return false;
                }
                if (this.color != other.color) {
                    return false;
                }
                if (this.dataFormat != other.dataFormat) {
                    return false;
                }
                return true;
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 83 * hash + this.iBorder;
                hash = 83 * hash + this.iFontSize;
                hash = 83 * hash + this.iWrap;
                hash = 83 * hash + (this.sFont != null ? this.sFont.hashCode() : 0);
                hash = 83 * hash + this.color;
                hash = 83 * hash + this.dataFormat;
                return hash;
            }

            public Style(short iBorder, short iFontSize, short iWrap, String sFont, short color, short dataFormat) {
                this.iBorder = iBorder;
                this.iFontSize = iFontSize;
                this.iWrap = iWrap;
                this.sFont = sFont;
                this.color = color;
                this.dataFormat = dataFormat;
            }
        }

        ;

        private void writeCell(HSSFRow row, ResultSetMetaData rData, ResultSet rSet, ReportSAXParser.Column c, HSSFWorkbook wb, short colIndex) throws SQLException {
            String sResult = c.sFormat;
            HSSFCell cell = row.createCell(c.iNum);
            HSSFCellStyle style = null;
            if (rData != null || rSet != null) {
                for (String sField : c.arsFields) {
                    int iColIndex = -1;
                    for (int i = 1; i <= rData.getColumnCount(); i++) {
                        if (rData.getColumnName(i).equals(sField)) {
                            iColIndex = i;
                            break;
                        }
                    }
                    if (iColIndex == -1) continue;
                    switch(rData.getColumnType(iColIndex)) {
                        case Types.DATE:
                            Date dt = rSet.getDate(iColIndex);
                            if (c.bSimpleType) {
                                style = createStyle(c, wb, colIndex, HSSFDataFormat.getBuiltinFormat("m/d/yy"));
                                if (dt != null) cell.setCellValue(dt);
                                cell.setCellStyle(style);
                                return;
                            } else {
                                if (dt != null) {
                                    SimpleDateFormat fmt = new SimpleDateFormat(ResourceBundle.getBundle(ExcelReports.class.getName()).getString("DateFormat"));
                                    sResult = sResult.replaceAll("\\{" + sField + "\\}", fmt.format(dt));
                                } else sResult = sResult.replaceAll("\\{" + sField + "\\}", "");
                            }
                            break;
                        case Types.TIME:
                            Time tm = rSet.getTime(iColIndex);
                            if (c.bSimpleType) {
                                style = createStyle(c, wb, colIndex, HSSFDataFormat.getBuiltinFormat("h:mm"));
                                if (tm != null) cell.setCellValue(tm);
                                cell.setCellStyle(style);
                                return;
                            } else {
                                if (tm != null) {
                                    SimpleDateFormat fmt = new SimpleDateFormat(ResourceBundle.getBundle(ExcelReports.class.getName()).getString("DateFormat"));
                                    sResult = sResult.replaceAll("\\{" + sField + "\\}", fmt.format(tm));
                                } else sResult = sResult.replaceAll("\\{" + sField + "\\}", "");
                            }
                            break;
                        case Types.TIMESTAMP:
                            Timestamp tms = rSet.getTimestamp(iColIndex);
                            if (c.bSimpleType) {
                                style = createStyle(c, wb, colIndex, HSSFDataFormat.getBuiltinFormat("m/d/yy h:mm"));
                                if (tms != null) cell.setCellValue(tms);
                                cell.setCellStyle(style);
                                return;
                            } else {
                                if (tms != null) {
                                    SimpleDateFormat fmt = new SimpleDateFormat(ResourceBundle.getBundle(ExcelReports.class.getName()).getString("DateFormat"));
                                    sResult = sResult.replaceAll("\\{" + sField + "\\}", fmt.format(rSet.getTimestamp(iColIndex)));
                                } else sResult = sResult.replaceAll("\\{" + sField + "\\}", "");
                            }
                            break;
                        case Types.VARCHAR:
                        case Types.CHAR:
                            String ss = rSet.getString(iColIndex);
                            if (c.bSimpleType) {
                                style = createStyle(c, wb, colIndex, HSSFDataFormat.getBuiltinFormat("0"));
                                if (ss != null) cell.setCellValue(new HSSFRichTextString(ss));
                                cell.setCellStyle(style);
                                return;
                            }
                            if (ss != null) sResult = sResult.replaceAll("\\{" + sField + "\\}", ss); else sResult = sResult.replaceAll("\\{" + sField + "\\}", "");
                            break;
                        case Types.INTEGER:
                            if (c.bSimpleType) {
                                style = createStyle(c, wb, colIndex, HSSFDataFormat.getBuiltinFormat("0"));
                                cell.setCellValue(rSet.getInt(iColIndex));
                                cell.setCellStyle(style);
                                return;
                            } else {
                                String sFieldValue = rSet.getString(iColIndex);
                                sResult = sResult.replaceAll("\\{" + sField + "\\}", sFieldValue);
                            }
                            break;
                        case Types.DOUBLE:
                        case Types.FLOAT:
                        case Types.DECIMAL:
                        case Types.NUMERIC:
                            if (c.bSimpleType) {
                                style = createStyle(c, wb, colIndex, HSSFDataFormat.getBuiltinFormat("#,##0.00"));
                                cell.setCellValue(rSet.getDouble(iColIndex));
                                cell.setCellStyle(style);
                                return;
                            } else {
                                String sFieldValue = rSet.getString(iColIndex);
                                sResult = sResult.replaceAll("\\{" + sField + "\\}", sFieldValue);
                            }
                            break;
                        default:
                            ss = rSet.getString(iColIndex);
                            if (c.bSimpleType) {
                                style = createStyle(c, wb, colIndex, HSSFDataFormat.getBuiltinFormat("text"));
                                if (ss != null) cell.setCellValue(new HSSFRichTextString(ss));
                                cell.setCellStyle(style);
                                return;
                            } else {
                                if (ss != null) sResult = sResult.replaceAll("\\{" + sField + "\\}", ss); else sResult = sResult.replaceAll("\\{" + sField + "\\}", "");
                            }
                            break;
                    }
                }
            } else sResult = "";
            style = createStyle(c, wb, colIndex, HSSFDataFormat.getBuiltinFormat("text"));
            cell.setCellValue(new HSSFRichTextString(sResult));
            cell.setCellStyle(style);
        }

        HashMap<Style, HSSFCellStyle> m_styles = new HashMap<Style, HSSFCellStyle>();

        private HSSFCellStyle createStyle(ReportSAXParser.Column c, HSSFWorkbook wb, short colIndex, short dataFormat) {
            Style st = new Style(c.iBorder, c.iFontSize, c.iWrap, c.sFont, colIndex, dataFormat);
            HSSFCellStyle newStyle = m_styles.get(st);
            if (newStyle != null) return newStyle;
            newStyle = wb.createCellStyle();
            m_styles.put(st, newStyle);
            newStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
            newStyle.setDataFormat(dataFormat);
            switch(c.iBorder) {
                case 1:
                    newStyle.setBorderBottom(HSSFCellStyle.BORDER_NONE);
                    newStyle.setBorderTop(HSSFCellStyle.BORDER_NONE);
                    newStyle.setBorderLeft(HSSFCellStyle.BORDER_NONE);
                    newStyle.setBorderRight(HSSFCellStyle.BORDER_NONE);
                    break;
                case 2:
                    newStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
                    newStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
                    newStyle.setBorderLeft(HSSFCellStyle.BORDER_NONE);
                    newStyle.setBorderRight(HSSFCellStyle.BORDER_NONE);
                    break;
                case 3:
                    newStyle.setBorderBottom(HSSFCellStyle.BORDER_NONE);
                    newStyle.setBorderTop(HSSFCellStyle.BORDER_NONE);
                    newStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
                    newStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
                    break;
                case 4:
                    newStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
                    newStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
                    newStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
                    newStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
                    break;
            }
            if (colIndex == -1) {
                newStyle.setFillPattern(HSSFCellStyle.NO_FILL);
            } else {
                newStyle.setFillForegroundColor(colIndex);
                newStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
            }
            HSSFFont fn = wb.findFont((short) 400, (short) 32767, (short) (c.iFontSize * 20), c.sFont, false, false, (short) 0, (byte) 0);
            if (fn == null) {
                fn = wb.createFont();
                fn.setFontName(c.sFont);
                fn.setFontHeight((short) (c.iFontSize * 20));
            }
            newStyle.setFont(fn);
            if (c.iWrap > 0) newStyle.setWrapText(true); else newStyle.setWrapText(false);
            return newStyle;
        }
    }

    private static void setParameter(PreparedStatement stmt, int iPar, Object o) throws SQLException {
        if (o instanceof String) {
            stmt.setString(iPar, (String) o);
            return;
        }
        if (o == null) {
            stmt.setNull(iPar, Types.VARCHAR);
            return;
        }
        if (o instanceof Integer) {
            stmt.setInt(iPar, (Integer) o);
            return;
        }
        if (o instanceof Long) {
            stmt.setLong(iPar, (Long) o);
            return;
        }
        if (o instanceof Double) {
            stmt.setDouble(iPar, (Double) o);
            return;
        }
        if (o instanceof Date) {
            stmt.setDate(iPar, (Date) o);
            return;
        }
        if (o instanceof Timestamp) {
            stmt.setTimestamp(iPar, (Timestamp) o);
            return;
        }
    }

    private static int loadPicture(InputStream fis, HSSFWorkbook wb) throws IOException {
        int pictureIndex;
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream();
            int c;
            while ((c = fis.read()) != -1) bos.write(c);
            pictureIndex = wb.addPicture(bos.toByteArray(), HSSFWorkbook.PICTURE_TYPE_PNG);
        } finally {
            if (fis != null) fis.close();
            if (bos != null) bos.close();
        }
        return pictureIndex;
    }

    /**
	 * Функция создает файл в формате MS Excel и возвращает его 
	 * @param con			- Соединение с БД
	 * @param values		- Параметры в формате HashMap
	 *							FORM	- файл с описание отчета (String)
	 *							Остальное параметры, 
	 *							которые будут использованы при отработке запросов
	 * 
	 * @param retValue файл Excel сжатый GZIP в виде byte[]
	 * @return
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws org.xml.sax.SAXException
	 */
    public static Any createExcelFile(Connection con, Any values, Any retValue) throws SQLException, IOException, ParserConfigurationException, SAXException, StandardException {
        HashMap val = (HashMap) values.extract_Value();
        String sForm = (String) val.get("FORM");
        final int MAX_ROWS = Integer.valueOf(ResourceBundle.getBundle(ExcelReports.class.getName()).getString("excelMaxRows"));
        String sXMLFiles = ResourceBundle.getBundle(ExcelReports.class.getName()).getString("XMLFiles");
        InputStream is = ExcelReports.class.getResourceAsStream(sXMLFiles + "/" + sForm);
        ReportSAXParser parser = new ReportSAXParser();
        if (!parser.isValidXML(is)) {
            throw (new StandardException("XML File is not valid"));
        }
        is.close();
        is = ExcelReports.class.getResourceAsStream(sXMLFiles + "/" + sForm);
        parser.parse(is);
        HSSFWorkbook wb = new HSSFWorkbook();
        {
            HSSFSheet sheet = null;
            String[] parameters = null;
            PreparedStatement stmt = null;
            int iRow = 0;
            if (parser.m_Header != null && parser.m_Header.sSheet != null) sheet = wb.createSheet(parser.m_Header.sSheet); else sheet = wb.createSheet("Report_Sheet0");
            if (parser.m_Header != null) try {
                if (parser.m_Header.picture != null) {
                    HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
                    HSSFClientAnchor anchor = new HSSFClientAnchor(0, 0, 0, 0, parser.m_Header.picture.col1, parser.m_Header.picture.row1, parser.m_Header.picture.col2, parser.m_Header.picture.row2);
                    anchor.setAnchorType(2);
                    patriarch.createPicture(anchor, loadPicture(new FileInputStream(parser.m_Header.picture.sSrc), wb));
                }
                parameters = findParameters(parser.m_Header.sQuery);
                stmt = con.prepareStatement(parser.m_Header.sQuery);
                int iPar = 0;
                for (String parName : parameters) {
                    Object o = val.get(parName);
                    iPar++;
                    setParameter(stmt, iPar, o);
                }
                ResultSet rSet = stmt.executeQuery();
                ResultSetMetaData data = rSet.getMetaData();
                if (rSet.next()) {
                    for (ReportSAXParser.Row r : parser.m_Header.arRows) {
                        HSSFRow row = sheet.getRow(r.iNum);
                        if (row == null) row = sheet.createRow(r.iNum);
                        iRow = iRow < r.iNum ? r.iNum : iRow;
                        for (ReportSAXParser.Column c : r.arColumns) {
                            if (c.iWidth > 0) sheet.setColumnWidth(c.iNum, (short) (c.iWidth * 256));
                            if (c.iColspan > 1) sheet.addMergedRegion(new Region(r.iNum, c.iNum, r.iNum, (short) (c.iNum + c.iColspan - 1)));
                            parser.writeCell(row, data, rSet, c, wb, (short) -1);
                        }
                    }
                }
                rSet.close();
            } finally {
                if (stmt != null) stmt.close();
            }
            int iSheet = 1;
            iRow++;
            for (ReportSAXParser.Rows pRow : parser.m_Rows) {
                try {
                    stmt = con.prepareStatement(pRow.sQuery);
                    parameters = findParameters(pRow.sQuery);
                    int iPar = 0;
                    for (String parName : parameters) {
                        Object o = val.get(parName);
                        iPar++;
                        setParameter(stmt, iPar, o);
                    }
                    HSSFPalette palette = wb.getCustomPalette();
                    HSSFColor colHeader = palette.findSimilarColor(pRow.crHeaderBack[0], pRow.crHeaderBack[1], pRow.crHeaderBack[2]);
                    palette.setColorAtIndex(colHeader.getIndex(), pRow.crHeaderBack[0], pRow.crHeaderBack[1], pRow.crHeaderBack[2]);
                    colHeader = palette.getColor(colHeader.getIndex());
                    HSSFColor colEven = palette.findSimilarColor(pRow.crEvenBack[0], pRow.crEvenBack[1], pRow.crEvenBack[2]);
                    palette.setColorAtIndex(colEven.getIndex(), pRow.crEvenBack[0], pRow.crEvenBack[1], pRow.crEvenBack[2]);
                    colEven = palette.getColor(colEven.getIndex());
                    HSSFColor colOgg = palette.findSimilarColor(pRow.crOggBack[0], pRow.crOggBack[1], pRow.crOggBack[2]);
                    palette.setColorAtIndex(colOgg.getIndex(), pRow.crOggBack[0], pRow.crOggBack[1], pRow.crOggBack[2]);
                    colOgg = palette.getColor(colOgg.getIndex());
                    iRow = pRow.iStartRow > iRow ? pRow.iStartRow : iRow;
                    if (pRow.sSheet != null) {
                        sheet = wb.getSheet(pRow.sSheet);
                        if (sheet == null) {
                            sheet = wb.createSheet(pRow.sSheet);
                            iRow = pRow.iStartRow;
                        }
                    }
                    HSSFRow row = sheet.getRow(iRow);
                    if (row == null) row = sheet.createRow(iRow);
                    for (ReportSAXParser.Column c : pRow.arColumns) {
                        String sOldFont = c.sFont;
                        short iOldFont = c.iFontSize;
                        if (pRow.sHeaderFont != null) {
                            c.sFont = pRow.sHeaderFont;
                            c.iFontSize = pRow.iHeaderFontSize;
                        }
                        HSSFCellStyle newStyle = parser.createStyle(c, wb, colHeader.getIndex(), HSSFDataFormat.getBuiltinFormat("text"));
                        newStyle.setWrapText(true);
                        if (c.iWidth > 0) sheet.setColumnWidth(c.iNum, (short) (c.iWidth * 256));
                        if (c.iColspan > 1) sheet.addMergedRegion(new Region(iRow, c.iNum, iRow, (short) (c.iNum + c.iColspan - 1)));
                        HSSFCell cell = row.createCell(c.iNum);
                        cell.setCellValue(new HSSFRichTextString(c.sLabel));
                        cell.setCellStyle(newStyle);
                        if (pRow.sHeaderFont != null) {
                            c.sFont = sOldFont;
                            c.iFontSize = iOldFont;
                        }
                    }
                    iRow++;
                    ResultSet rSet = stmt.executeQuery();
                    ResultSetMetaData data = rSet.getMetaData();
                    boolean bHasRows = false;
                    while (rSet.next()) {
                        bHasRows = true;
                        iRow = pRow.iStartRow > iRow ? pRow.iStartRow : iRow;
                        row = sheet.getRow(iRow);
                        if (row == null) row = sheet.createRow(iRow);
                        for (ReportSAXParser.Column c : pRow.arColumns) {
                            short colIndex = -1;
                            if (iRow % 2 == 0) {
                                colIndex = colEven.getIndex();
                            } else {
                                colIndex = colOgg.getIndex();
                            }
                            if (c.iWidth > 0) sheet.setColumnWidth(c.iNum, (short) (c.iWidth * 256));
                            if (c.iColspan > 1) sheet.addMergedRegion(new Region(iRow, c.iNum, iRow, (short) (c.iNum + c.iColspan - 1)));
                            if (c.sFormat != null && c.sFormat.length() > 0) parser.writeCell(row, data, rSet, c, wb, colIndex);
                        }
                        iRow++;
                        if (iRow == MAX_ROWS) {
                            sheet = wb.createSheet("Report_Sheet" + iSheet);
                            iRow = 0;
                            iSheet++;
                            continue;
                        }
                    }
                    rSet.close();
                    if (!bHasRows) {
                        iRow = pRow.iStartRow > iRow ? pRow.iStartRow : iRow;
                        row = sheet.getRow(iRow);
                        if (row == null) row = sheet.createRow(iRow);
                        for (ReportSAXParser.Column c : pRow.arColumns) {
                            short colIndex = -1;
                            if (iRow % 2 == 0) {
                                colIndex = colEven.getIndex();
                            } else {
                                colIndex = colOgg.getIndex();
                            }
                            if (c.iWidth > 0) sheet.setColumnWidth(c.iNum, (short) (c.iWidth * 256));
                            if (c.iColspan > 1) sheet.addMergedRegion(new Region(iRow, c.iNum, iRow, (short) (c.iNum + c.iColspan - 1)));
                            if (c.sFormat != null && c.sFormat.length() > 0) {
                                parser.writeCell(row, null, null, c, wb, colIndex);
                            }
                        }
                        iRow++;
                    }
                } finally {
                    if (stmt != null) stmt.close();
                }
            }
            if (parser.m_Footer != null) try {
                stmt = con.prepareStatement(parser.m_Footer.sQuery);
                int iPar = 0;
                for (String parName : parameters) {
                    Object o = val.get(parName);
                    iPar++;
                    setParameter(stmt, iPar, o);
                }
                ResultSet rSet = stmt.executeQuery();
                ResultSetMetaData data = rSet.getMetaData();
                if (rSet.next()) {
                    for (ReportSAXParser.Row r : parser.m_Footer.arRows) {
                        HSSFRow row = sheet.getRow(iRow + r.iNum);
                        if (row == null) row = sheet.createRow(iRow + r.iNum);
                        for (ReportSAXParser.Column c : r.arColumns) {
                            if (c.iWidth > 0) sheet.setColumnWidth(c.iNum, (short) (c.iWidth * 256));
                            if (c.iColspan > 1) sheet.addMergedRegion(new Region(iRow + r.iNum, c.iNum, iRow + r.iNum, (short) (c.iNum + c.iColspan - 1)));
                            parser.writeCell(row, data, rSet, c, wb, (short) -1);
                        }
                    }
                }
                rSet.close();
            } finally {
                if (stmt != null) stmt.close();
            }
        }
        long heapSize = Runtime.getRuntime().totalMemory();
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        long heapFreeSize = Runtime.getRuntime().freeMemory();
        System.out.println("Freing memory FreeSize: " + heapFreeSize + " ....");
        System.gc();
        heapFreeSize = Runtime.getRuntime().freeMemory();
        System.out.println("OK FreeSize: " + heapFreeSize + " ....");
        ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
        GZIPOutputStream gs = new GZIPOutputStream(fileOut);
        wb.write(gs);
        gs.finish();
        fileOut.close();
        System.gc();
        retValue.insert_Value(fileOut.toByteArray());
        return retValue;
    }

    /**
	 * Функция возвращает список отчетов
	 * @param con			- Соединение с БД
	 * @param values		- Параметры тип HAshMap
	 *							PID		- Идентифкатор родительского элемента
	 * @param retValue возвращаемое значение тип hashMap
	 *							IOBJID		- Идентификатор
	 *							COBJNAME	- Название
	 *							DCREATE		- Дата создания
	 *							COBJNUM		- Форма отчета
	 * @return
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws org.xml.sax.SAXException
	 */
    public static Any getReportList(Connection con, Any values, Any retValue) throws SQLException, IOException, ParserConfigurationException, SAXException {
        PreparedStatement ps = null;
        try {
            HashMap map = (HashMap) values.extract_Value();
            String sParentID = (String) map.get("PID");
            String sSql = ResourceBundle.getBundle(ExcelReports.class.getName()).getString("getReportList");
            ps = con.prepareStatement(sSql);
            ps.setString(1, sParentID);
            ResultSet rSet = ps.executeQuery();
            HashMap[] ret = ObjUtil.getResultFromResultSet(rSet, "IOBJID", "COBJNAME", "DCREATE", "COBJNUM");
            retValue.insert_Value(ret);
        } finally {
            if (ps != null) ps.close();
        }
        System.gc();
        return retValue;
    }

    /**
	 * Фзвращает список атрибутов для отчета
	 * @param con			- Соединение с БД
	 * @param values		- Параметры тип HashMap
	 *							ID		- Идентификатор отчета
	 * @param retValue Возврщаемое значение тип HashMap[]
	 *											 NAME	- Название
	 *										     DESC	- Описание
	 * @return
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws org.xml.sax.SAXException
	 */
    public static Any getReportAttrs(Connection con, Any values, Any retValue) throws SQLException, IOException, ParserConfigurationException, SAXException {
        OracleCallableStatement ps = null;
        try {
            HashMap map = (HashMap) values.extract_Value();
            String sID = (String) map.get("ID");
            String sSql = ResourceBundle.getBundle(ExcelReports.class.getName()).getString("getReportAttrs");
            ps = (OracleCallableStatement) con.prepareCall(sSql);
            ps.setString(1, sID);
            OracleResultSet rSet = (OracleResultSet) ps.executeQuery();
            if (rSet.next()) {
                ObjAttrs arAttrs = (ObjAttrs) rSet.getORAData("ATTRS", new ObjAttrs());
                ObjAttr attrs[] = arAttrs.getArray();
                ArrayList<HashMap> arHash = new ArrayList<HashMap>();
                for (ObjAttr atr : attrs) {
                    HashMap val = new HashMap();
                    val.put("NAME", atr.getCname());
                    val.put("DESC", atr.getCdesc());
                    val.put("MASK", atr.getCtype());
                    arHash.add(val);
                }
                retValue.insert_Value(arHash.toArray(new HashMap[arHash.size()]));
            }
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    /**
	 * Функция добавляет отчет в систему
	 * @param con			- Соединение с БД
	 * @param values		- Параметры тип HashMap
	 *							PID	- Идентификатор родителя
	 *							ID	- Идентификатор отета, если есть
	 *							NAME - Нвазвание
	 *							FORM - Форма
	 *							ATTR - Атрибуты (параметры) отчета тип Hashmap[]
	 *									NAME	- Название 
	 *									DESC	- Описание
	 *									MASK	- Маска ввода
	 * @param retValue ничего не возвращает
	 * @return
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws org.xml.sax.SAXException
	 */
    public static Any addReport(Connection con, Any values, Any retValue) throws SQLException, IOException, ParserConfigurationException, SAXException {
        OracleCallableStatement ps = null;
        try {
            HashMap map = (HashMap) values.extract_Value();
            String sParentID = (String) map.get("PID");
            String sID = (String) map.get("ID");
            String sName = (String) map.get("NAME");
            String sForm = (String) map.get("FORM");
            HashMap[] attr = (HashMap[]) map.get("ATTR");
            ArrayList<ObjAttr> arAttr = new ArrayList<ObjAttr>();
            for (HashMap a : attr) {
                arAttr.add(new ObjAttr((String) a.get("NAME"), (String) a.get("DESC"), (String) a.get("MASK")));
            }
            String sSql = ResourceBundle.getBundle(ExcelReports.class.getName()).getString("addReport");
            ps = (OracleCallableStatement) con.prepareCall(sSql);
            ObjAttrs arAttrs = new ObjAttrs(arAttr.toArray(new ObjAttr[arAttr.size()]));
            ps.setString(1, sParentID);
            ps.setString(2, sName);
            ps.setString(3, sForm);
            ps.setORAData(4, arAttrs);
            if (sID != null) ps.setString(5, sID); else ps.setNull(5, Types.VARCHAR);
            ps.executeUpdate();
            con.commit();
        } finally {
            if (ps != null) ps.close();
        }
        return retValue;
    }

    private ExcelReports() {
    }
}
