package org.tn5250j.tools.filters;

import java.io.*;
import java.util.ArrayList;
import org.tn5250j.tools.*;

public class KSpreadOutputFilter implements OutputFilterInterface {

    private int row = 0;

    StringBuffer sb = new StringBuffer();

    PrintStream fout = null;

    public void createFileInstance(String fileName) throws FileNotFoundException {
        fout = new PrintStream(new FileOutputStream(fileName));
    }

    /**
    * Write the html header of the output file
    */
    public void parseFields(byte[] cByte, ArrayList ffd, StringBuffer rb) {
        FileFieldDef f;
        row++;
        int column = 1;
        for (int x = 0; x < ffd.size(); x++) {
            f = (FileFieldDef) ffd.get(x);
            if (f.isWriteField()) {
                rb.append("    <cell row=" + "\"" + row + "\"");
                rb.append(" column=" + "\"" + column++ + "\" > \n");
                switch(f.getFieldType()) {
                    case 'P':
                    case 'S':
                        rb.append("     <format precision=\"" + f.getPrecision() + "\" > \n     </format>\n");
                        break;
                    default:
                        rb.append("     <format/>\n");
                        break;
                }
                rb.append("    <text>");
                rb.append(tr2xml(f.parseData(cByte)));
                rb.append("</text> </cell>\n");
            }
        }
        fout.println(rb);
        fout.flush();
    }

    private String tr2xml(String s) {
        sb.setLength(0);
        for (int x = 0; x < s.length(); x++) {
            switch(s.charAt(x)) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                default:
                    sb.append(s.charAt(x));
            }
        }
        return sb.toString();
    }

    /**
    * Write the html header of the output file
    */
    public void writeHeader(String fileName, String host, ArrayList ffd, char decChar) {
        final String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE spreadsheet >" + " <spreadsheet mime=\"application/x-kspread\" editor=\"KSpread\" >\n" + " <paper format=\"A4\" orientation=\"Portrait\" >\n" + "  <borders right=\"20\" left=\"20\" bottom=\"20\" top=\"20\" />\n" + "  <head/>\n" + "  <foot/>\n" + " </paper>\n" + " <locale positivePrefixCurrencySymbol=\"True\"" + " negativeMonetarySignPosition=\"4\"" + " negativePrefixCurrencySymbol=\"True\"" + " fracDigits=\"2\"" + " thousandsSeparator=\".\"" + " dateFormat=\"%A %d %B %Y\"" + " timeFormat=\"%H:%M:%S\"" + " monetaryDecimalSymbol=\"" + decChar + "\"" + " weekStartsMonday=\"True\"" + " negativeSign=\"-\"" + " positiveSign=\"+\"" + " positiveMonetarySignPosition=\"4\"" + " decimalSymbol=\"" + decChar + "\"" + " dateFormatShort=\"%d.%m.%Y\" />\n" + " <map markerColumn=\"1\" activeTable=\"Table1\" markerRow=\"1\" >\n" + "  <table columnnumber=\"0\" borders=\"0\" hide=\"0\" grid=\"1\" formular=\"0\" lcmode=\"0\" name=\"Table1\" >";
        try {
            fout.write(head.getBytes());
            fout.write('\n');
            FileFieldDef f;
            row++;
            int column = 1;
            for (int x = 0; x < ffd.size(); x++) {
                f = (FileFieldDef) ffd.get(x);
                if (f.isWriteField()) {
                    fout.print("   <cell row=" + "\"" + row + "\"");
                    fout.print(" column=" + "\"" + column++ + "\" >\n");
                    fout.print("    <format>\n");
                    fout.print("     <font size=\"11\" style=\"\" bold=\"yes\" weight=\"75\" />\n");
                    fout.print("    </format>\n    <text>");
                    fout.print(f.getFieldName());
                    fout.print("</text>\n   </cell>\n");
                }
            }
            fout.flush();
        } catch (IOException ioex) {
            System.out.println(ioex.getMessage());
        }
    }

    /**
    * write the footer of the xml output
    */
    public void writeFooter(ArrayList ffd) {
        try {
            fout.write("  </table>\n".getBytes());
            fout.write(" </map>\n".getBytes());
            fout.write("</spreadsheet>\n".getBytes());
            fout.flush();
            fout.close();
        } catch (IOException ioex) {
            System.out.println(ioex.getMessage());
        }
    }
}
