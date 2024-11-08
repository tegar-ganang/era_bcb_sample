package cvreport;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import org.jfree.chart.*;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.time.*;
import au.com.bytecode.opencsv.*;
import com.lowagie.text.DocumentException;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

public class cvreport {

    static Logger lgr = Logger.getLogger("cvreport");

    public cvreport() {
    }

    public static void main(String args[]) throws IOException {
        BasicConfigurator.configure();
        lgr.debug("This is a logging message!");
        System.out.println(new Date() + " Starting.");
        System.out.println(new Date() + " Reading.");
        File td = new File("tmp0");
        td.mkdir();
        File children[] = td.listFiles();
        for (int i = 0; i < children.length; i++) {
            children[i].delete();
        }
        td.delete();
        td.mkdir();
        File td2 = new File("output");
        td2.mkdir();
        createHtmlHeader("output/cvreport2.html");
        CSVReader reader = new CSVReader(new FileReader("cvreport.data"));
        int numberLines = 0;
        String nextLine[];
        while ((nextLine = reader.readNext()) != null) {
            if (nextLine[0].equals("*")) {
                System.out.println(new Date() + " COMMENT: " + nextLine[1]);
            } else {
                numberLines++;
                createCash(nextLine[0], nextLine[2], nextLine[6]);
                if (nextLine[6].equals("")) {
                    System.out.println(nextLine[0]);
                }
                if (nextLine[1].equals("OPENACCT")) {
                    actionOpenacct(nextLine[2], nextLine[3], nextLine[6], nextLine[0]);
                }
                if (nextLine[1].equals("EXP")) {
                    actionExp(nextLine[0], nextLine[2], nextLine[3], nextLine[4], nextLine[6]);
                }
                if (nextLine[1].equals("BUY")) {
                    actionBuy(nextLine[0], nextLine[2], nextLine[3], nextLine[4], nextLine[5]);
                    actionBuy(nextLine[0], "allsecs3", nextLine[3], nextLine[4], nextLine[5]);
                }
                if (nextLine[1].equals("SELL")) {
                    actionSell(nextLine[0], nextLine[2], nextLine[3], nextLine[4], nextLine[5]);
                    actionSell(nextLine[0], "allsecs3", nextLine[3], nextLine[4], nextLine[5]);
                }
                if (nextLine[1].equals("DIV")) {
                    actionDiv(nextLine[0], nextLine[3], nextLine[6]);
                }
                if (nextLine[1].equals("INC")) {
                    actionInc(nextLine[0], nextLine[3], nextLine[6]);
                }
                if (nextLine[1].equals("UPD")) {
                    actionUpd(nextLine[0], nextLine[2], nextLine[3], nextLine[5]);
                    actionUpd(nextLine[0], "allsecs3", nextLine[3], nextLine[5]);
                }
                if (nextLine[1].equals("XFR")) {
                    actionXfr(nextLine[0], nextLine[3], nextLine[6]);
                }
            }
        }
        System.out.println(new Date() + " Reading finished.");
        System.out.println(new Date() + " Data points: " + numberLines);
        System.out.println(new Date() + " Creating Networth Graph");
        totalCash();
        stockReportINV();
        stockReportRESP();
        stockReportRRSP();
        stockReportALL();
        uniqExp();
        uniqInc();
        System.out.println(new Date() + " Total Dividends.");
        uniqDiv();
        totalDiv();
        monthlyHeader();
        monthlyDisplayValues("INVESTMENTS");
        monthlyDisplayValues("RESP");
        monthlyDisplayValues("RRSP");
        monthlyInc();
        monthlyDiv();
        monthlyExp();
        monthlyFooter();
        uniqSec();
        piePort("allsecs2");
        System.out.println(new Date() + " Finished: " + numberLines);
        createHtmlFooter("output/cvreport2.html");
        html_pdf();
    }

    static void createHtmlHeader(String filename) throws IOException {
        File f = new File(filename);
        FileOutputStream out = new FileOutputStream(f);
        PrintStream os = new PrintStream(out);
        os.println("<html><head><title>cvreport</title>");
        os.println("<style type=\"text/css\">");
        os.println("@page { size: letter; @bottom-right { content: \"Page \" counter(page) \" of \" counter(pages); } }");
        os.println("</style>");
        os.println("</head><body>");
        os.close();
    }

    static void createHtmlFooter(String filename) throws IOException {
        File f = new File(filename);
        FileOutputStream out = new FileOutputStream(f, true);
        PrintStream os = new PrintStream(out);
        os.println("</body></html>");
        os.close();
    }

    static void actionOpenacct(String B, String C, String D, String date) throws IOException {
        File f = new File("tmp0/" + C + ".accounttype");
        FileOutputStream out = new FileOutputStream(f, true);
        PrintStream os = new PrintStream(out);
        os.println(B);
        os.close();
        File f2 = new File("tmp0/" + B + ".cash2");
        FileOutputStream out2 = new FileOutputStream(f2);
        PrintStream os2 = new PrintStream(out2);
        os2.println(date + " " + D);
        os2.close();
        updateNetworth(date, D);
    }

    static void actionBuy(String date, String account, String sec, String units, String price) throws IOException {
        File file = new File("tmp0/" + account + ".secvalue");
        Vector vecStocks = new Vector();
        float units4 = 0.0F;
        units4 = Float.valueOf(units.trim()).floatValue();
        float price3 = Float.valueOf(price.trim()).floatValue();
        float price2 = price3 / units4;
        float book2 = price2;
        if (file.exists()) {
            CSVReader rdsecv = new CSVReader(new FileReader("tmp0/" + account + ".secvalue"));
            String nextL2[];
            while ((nextL2 = rdsecv.readNext()) != null) {
                float units3 = Float.valueOf(nextL2[1].trim()).floatValue();
                float book3 = Float.valueOf(nextL2[2].trim()).floatValue();
                float market3 = Float.valueOf(nextL2[3].trim()).floatValue();
                Stocks obj = new Stocks(nextL2[0], units3, book3, market3, market3 * units3);
                vecStocks.add(obj);
            }
            ListIterator iter6 = vecStocks.listIterator();
            for (int element = 0; iter6.hasNext(); element++) {
                Stocks temp = new Stocks();
                temp = (Stocks) iter6.next();
                float units5 = temp.getunits();
                float units3 = Float.valueOf(units.trim()).floatValue();
                String sec2 = temp.getname();
                if (!sec2.equals(sec)) {
                    continue;
                }
                units4 = units3 + units5;
                book2 = (temp.getbook() * temp.getunits() + units3 * price2) / units4;
                vecStocks.remove(element);
                break;
            }
        }
        Stocks obj = new Stocks(sec, units4, book2, price2, price2 * units4);
        vecStocks.add(obj);
        ListIterator iterw = vecStocks.listIterator();
        File fileout = new File("tmp0/" + account + ".secvalue");
        FileOutputStream out = new FileOutputStream(fileout);
        PrintStream os = new PrintStream(out);
        File fileout3 = new File("tmp0/" + account + ".porttotal2");
        FileOutputStream out3 = new FileOutputStream(fileout3, true);
        PrintStream os3 = new PrintStream(out3);
        float booktotal = 0.0F;
        float markettotal;
        Stocks temp;
        for (markettotal = 0.0F; iterw.hasNext(); markettotal += temp.getmarket() * temp.getunits()) {
            temp = new Stocks();
            temp = (Stocks) iterw.next();
            os.println(temp.getname() + "," + temp.getunits() + "," + temp.getbook() + "," + temp.getmarket());
            booktotal += temp.getbook() * temp.getunits();
        }
        os.close();
        os3.println(date + "," + booktotal + "," + markettotal);
        os3.close();
        monthlyAccountValues(date, account, markettotal);
    }

    static void actionSell(String date, String account, String sec, String units, String price) throws IOException {
        File file = new File("tmp0/" + account + ".secvalue");
        Vector vecStocks = new Vector();
        float units4 = 0.0F;
        units4 = Float.valueOf(units.trim()).floatValue();
        float price3 = Float.valueOf(price.trim()).floatValue();
        float price2 = price3 / units4;
        float book2 = price2;
        if (file.exists()) {
            CSVReader rdsecv = new CSVReader(new FileReader("tmp0/" + account + ".secvalue"));
            String nextL2[];
            while ((nextL2 = rdsecv.readNext()) != null) {
                float units3 = Float.valueOf(nextL2[1].trim()).floatValue();
                float book3 = Float.valueOf(nextL2[2].trim()).floatValue();
                float market3 = Float.valueOf(nextL2[3].trim()).floatValue();
                Stocks obj = new Stocks(nextL2[0], units3, book3, market3, market3 * units3);
                vecStocks.add(obj);
            }
            ListIterator iter6 = vecStocks.listIterator();
            for (int element = 0; iter6.hasNext(); element++) {
                Stocks temp = new Stocks();
                temp = (Stocks) iter6.next();
                float units5 = temp.getunits();
                float units3 = Float.valueOf(units.trim()).floatValue();
                String sec2 = temp.getname();
                if (!sec2.equals(sec)) {
                    continue;
                }
                units4 = units5 - units3;
                book2 = (temp.getbook() * temp.getunits() - units3 * price2) / units4;
                vecStocks.remove(element);
                break;
            }
        }
        if (units4 > 0.0F) {
            Stocks obj = new Stocks(sec, units4, book2, price2, price2 * units4);
            vecStocks.add(obj);
        }
        ListIterator iterw = vecStocks.listIterator();
        File fileout = new File("tmp0/" + account + ".secvalue");
        FileOutputStream out = new FileOutputStream(fileout);
        PrintStream os = new PrintStream(out);
        File fileout3 = new File("tmp0/" + account + ".porttotal2");
        FileOutputStream out3 = new FileOutputStream(fileout3, true);
        PrintStream os3 = new PrintStream(out3);
        float booktotal = 0.0F;
        float markettotal;
        Stocks temp;
        for (markettotal = 0.0F; iterw.hasNext(); markettotal += temp.getmarket() * temp.getunits()) {
            temp = new Stocks();
            temp = (Stocks) iterw.next();
            os.println(temp.getname() + "," + temp.getunits() + "," + temp.getbook() + "," + temp.getmarket());
            booktotal += temp.getbook() * temp.getunits();
        }
        os.close();
        os3.println(date + "," + booktotal + "," + markettotal);
        os3.close();
        monthlyAccountValues(date, account, markettotal);
    }

    static void actionUpd(String date, String account, String sec, String price) throws IOException {
        File file = new File("tmp0/" + account + ".secvalue");
        Vector vecStocks = new Vector();
        float units2 = 0.0F;
        float price2 = 0.0F;
        float book2 = 0.0F;
        if (file.exists()) {
            CSVReader rdsecv = new CSVReader(new FileReader("tmp0/" + account + ".secvalue"));
            String nextL2[];
            while ((nextL2 = rdsecv.readNext()) != null) {
                units2 = Float.valueOf(nextL2[1].trim()).floatValue();
                book2 = Float.valueOf(nextL2[2].trim()).floatValue();
                price2 = Float.valueOf(nextL2[3].trim()).floatValue();
                Stocks obj = new Stocks(nextL2[0], units2, book2, price2, price2 * units2);
                vecStocks.add(obj);
            }
            ListIterator iter6 = vecStocks.listIterator();
            for (int element = 0; iter6.hasNext(); element++) {
                Stocks temp = new Stocks();
                temp = (Stocks) iter6.next();
                String sec2 = temp.getname();
                if (sec2.equals(sec)) {
                    units2 = temp.getunits();
                    book2 = temp.getbook();
                    price2 = Float.valueOf(price.trim()).floatValue();
                    Stocks obj = new Stocks(sec, units2, book2, price2, price2 * units2);
                    vecStocks.setElementAt(obj, element);
                }
            }
        }
        ListIterator iterw = vecStocks.listIterator();
        File fileout = new File("tmp0/" + account + ".secvalue");
        FileOutputStream out = new FileOutputStream(fileout);
        PrintStream os = new PrintStream(out);
        File fileout2 = new File("tmp0/" + account + ".porttotal2");
        FileOutputStream out2 = new FileOutputStream(fileout2, true);
        PrintStream os2 = new PrintStream(out2);
        float booktotal = 0.0F;
        float markettotal;
        Stocks temp;
        for (markettotal = 0.0F; iterw.hasNext(); markettotal += temp.getmarket() * temp.getunits()) {
            temp = new Stocks();
            temp = (Stocks) iterw.next();
            os.println(temp.getname() + "," + temp.getunits() + "," + temp.getbook() + "," + temp.getmarket());
            booktotal += temp.getbook() * temp.getunits();
        }
        os.close();
        os2.println(date + "," + booktotal + "," + markettotal);
        os2.close();
        monthlyAccountValues(date, account, markettotal);
    }

    static void actionExp(String date, String account, String name, String exp, String price) throws IOException {
        StringTokenizer st = new StringTokenizer(date, "/", false);
        int year = Integer.parseInt(st.nextToken());
        int month = Integer.parseInt(st.nextToken());
        File f3 = new File("tmp0/" + year + "." + month + ".exp");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        os3.println(date + "," + account + "," + name + "," + exp + "," + price);
        os3.close();
        File f4 = new File("tmp0/exp.types");
        FileOutputStream out4 = new FileOutputStream(f4, true);
        PrintStream os4 = new PrintStream(out4);
        os4.println(exp);
        os4.close();
        updateNetworth(date, price);
    }

    static void actionDiv(String date, String sec, String value) throws IOException {
        StringTokenizer st = new StringTokenizer(date, "/", false);
        int year = Integer.parseInt(st.nextToken());
        int month = Integer.parseInt(st.nextToken());
        File f3 = new File("tmp0/" + year + "." + month + ".div");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        os3.println(sec + "," + value);
        os3.close();
        File f4 = new File("tmp0/div.types");
        FileOutputStream out4 = new FileOutputStream(f4, true);
        PrintStream os4 = new PrintStream(out4);
        os4.println(sec);
        os4.close();
        updateNetworth(date, value);
    }

    static void actionInc(String date, String payee, String value) throws IOException {
        StringTokenizer st = new StringTokenizer(date, "/", false);
        int year = Integer.parseInt(st.nextToken());
        int month = Integer.parseInt(st.nextToken());
        File f3 = new File("tmp0/" + year + "." + month + ".inc");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        os3.println(payee + "," + value);
        os3.close();
        File f4 = new File("tmp0/inc.types");
        FileOutputStream out4 = new FileOutputStream(f4, true);
        PrintStream os4 = new PrintStream(out4);
        os4.println(payee);
        os4.close();
        updateNetworth(date, value);
    }

    static void actionXfr(String date, String account, String amount) throws IOException {
        File f3 = new File("tmp0/" + account + ".cash");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        float amount2 = Float.valueOf(amount.trim()).floatValue();
        amount2 *= -1F;
        os3.println(date + "," + amount2);
        os3.close();
    }

    static void createCash(String date, String account, String amount) throws IOException {
        File f3 = new File("tmp0/" + account + ".cash");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        os3.println(date + "," + amount);
        os3.close();
    }

    static void totalCash() throws IOException {
        ExtensionFilter filter = new ExtensionFilter(".cash");
        File dir = new File("tmp0/");
        String list[] = dir.list(filter);
        for (int i = 0; i < list.length; i++) {
            totalCash2(list[i]);
        }
    }

    static void totalCash2(String file2) throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/" + file2));
        float total = 0.0F;
        TimeSeries pop = new TimeSeries("Test", org.jfree.data.time.Day.class);
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            float amount = Float.valueOf(nextL[1].trim()).floatValue();
            total += amount;
            File f3 = new File("tmp0/" + file2 + ".total");
            FileOutputStream out3 = new FileOutputStream(f3, true);
            PrintStream os3 = new PrintStream(out3);
            os3.println(nextL[0] + " " + total);
            os3.close();
            StringTokenizer st = new StringTokenizer(nextL[0], "/", false);
            int year = Integer.parseInt(st.nextToken());
            int month = Integer.parseInt(st.nextToken());
            int day = Integer.parseInt(st.nextToken());
            pop.addOrUpdate(new Day(day, month, year), total);
        }
        rde.close();
        String file3 = file2.substring(0, file2.length() - 5);
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(pop);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(file3, "Date", "$", dataset, true, true, false);
        try {
            ChartUtilities.saveChartAsPNG(new File("output/" + file3 + ".png"), chart, 670, 256);
        } catch (IOException e) {
            System.err.println("Problem occurred creating chart.");
        }
        File f2 = new File("output/cvreport2.html");
        FileOutputStream out2 = new FileOutputStream(f2, true);
        PrintStream os2 = new PrintStream(out2);
        os2.println("<img src=\"" + file3 + ".png\" width=\"330\" height=\"200\"></img>");
        os2.close();
    }

    static void stockReport(String account) throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/" + account + ".secvalue"));
        float booktotaltotal = 0.0F;
        float markettotaltotal = 0.0F;
        float perctotal = 0.0F;
        String dollarfmt = "0.00";
        DecimalFormat df3 = new DecimalFormat(dollarfmt);
        File f2 = new File("output/cvreport2.html");
        FileOutputStream out2 = new FileOutputStream(f2, true);
        PrintStream os2 = new PrintStream(out2);
        os2.println("<div style=\"page-break-before: always;\"></div>");
        os2.println("<table cellSpacing=\"0\" cellPadding=\"5\" border=\"1\" id=\"table4\" width=\"670\"> ");
        os2.println(" <tr vAlign=\"bottom\">");
        os2.println("  <td>");
        os2.println("  <font face=\"Arial\" size=\"1\" color=\"#081042\"><b>" + account + "</b> </font></td>");
        os2.println(" </tr>");
        os2.println(" <tr>");
        os2.println(" <img src=\"" + account + ".port.png\" width=\"330\" height=\"200\"></img>");
        os2.println(" <img src=\"" + account + ".pie.png\" width=\"330\" height=\"200\"></img>");
        os2.println(" </tr>");
        os2.println("<tr>");
        os2.println("<td vAlign=\"top\" width=\"82%\">");
        os2.println("<table style=\"-fs-table-paginate: paginate;\" cellSpacing=\"0\" cellPadding=\"5\" border=\"1\" id=\"table31\" width=\"650\">");
        os2.println("<thead>");
        os2.println("<tr vAlign=\"center\">");
        os2.println("<th>Units</th>");
        os2.println("<th>Description</th>");
        os2.println("<th>Price</th>");
        os2.println("<th>Book</th>");
        os2.println("<th>Market</th>");
        os2.println("<th>%</th>");
        os2.println("</tr>");
        os2.println("</thead>");
        os2.println("<tbody>");
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            float units = Float.valueOf(nextL[1].trim()).floatValue();
            float book = Float.valueOf(nextL[2].trim()).floatValue();
            float market = Float.valueOf(nextL[3].trim()).floatValue();
            float booktotal = book * units;
            float markettotal = market * units;
            float perc = ((markettotal - booktotal) / booktotal) * 100F;
            String unitsfmt = "0.0000";
            DecimalFormat df2 = new DecimalFormat(unitsfmt);
            String Strunits = df2.format(units);
            DecimalFormat df = new DecimalFormat(dollarfmt);
            String Strbooktotal = df.format(booktotal);
            String Strmarkettotal = df.format(markettotal);
            String Strperc = df.format(perc);
            String Strmarket = df.format(market);
            addAllSecs(nextL[0], markettotal);
            os2.println("<tr><td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">" + Strunits + "</font></td>");
            os2.println("<td><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"font-size:" + " 6pt\">" + nextL[0].replace('&', '_') + "</font></td>");
            os2.println("<td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=" + "\"font-size: 6pt\">" + Strmarket + "</font></td>");
            os2.println("<td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\"" + " style=\"font-size: 6pt\">" + Strbooktotal + "</font></td>");
            os2.println("<td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=" + "\"font-size: 6pt\">" + Strmarkettotal + "</font></td>");
            os2.println("<td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\"" + " style=\"font-size: 6pt\">" + Strperc + "</font></td>");
            os2.println("</tr>");
            booktotaltotal += booktotal;
            markettotaltotal += markettotal;
            perctotal = ((markettotaltotal - booktotaltotal) / booktotaltotal) * 100F;
        }
        String Strbooktotaltotal = df3.format(booktotaltotal);
        String Strmarkettotaltotal = df3.format(markettotaltotal);
        String Strperctotal = df3.format(perctotal);
        os2.println("<tr><td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">\" \"" + "</font>");
        os2.println("</td><td><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"font-size:" + " 8pt\">" + "TOTAL </font>");
        os2.println("</td><td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=" + "\"font-size: 6pt\">" + "\" \"" + "</font>");
        os2.println("</td><td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\"" + " style=\"font-size: 6pt\">" + Strbooktotaltotal + "</font>");
        os2.println("</td><td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=" + "\"font-size: 6pt\">" + Strmarkettotaltotal + "</font>");
        os2.println("</td><td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\"" + " style=\"font-size: 6pt\">" + Strperctotal + "</font></td></tr>");
        os2.println("</tbody>");
        os2.println("</table></td></tr>");
        os2.println("</table>");
        os2.close();
    }

    static void stockReportRRSP() throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/RRSP.accounttype"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            stockReport(nextL[0]);
            gnuplotPort(nextL[0]);
            piePort(nextL[0]);
        }
        (new File("tmp0/RRSP.accountype")).delete();
    }

    static void stockReportALL() throws IOException {
        gnuplotPort("allsecs3");
        piePort("allsecs3");
    }

    static void stockReportRESP() throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/RESP.accounttype"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            stockReport(nextL[0]);
            gnuplotPort(nextL[0]);
            piePort(nextL[0]);
        }
    }

    static void stockReportINV() throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/INVESTMENTS.accounttype"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            stockReport(nextL[0]);
            gnuplotPort(nextL[0]);
            piePort(nextL[0]);
        }
    }

    static void gnuplotPort(String account) throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/" + account + ".porttotal2"));
        TimeSeries pop = new TimeSeries("Book", org.jfree.data.time.Day.class);
        TimeSeries pop2 = new TimeSeries("Market", org.jfree.data.time.Day.class);
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            StringTokenizer st = new StringTokenizer(nextL[0], "/", false);
            int year = Integer.parseInt(st.nextToken());
            int month = Integer.parseInt(st.nextToken());
            int day = Integer.parseInt(st.nextToken());
            float amount = Float.valueOf(nextL[1].trim()).floatValue();
            float amount2 = Float.valueOf(nextL[2].trim()).floatValue();
            pop.addOrUpdate(new Day(day, month, year), amount);
            pop2.addOrUpdate(new Day(day, month, year), amount2);
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(pop);
        dataset.addSeries(pop2);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(account, "Date", "$", dataset, true, true, false);
        try {
            ChartUtilities.saveChartAsPNG(new File("output/" + account + ".port.png"), chart, 670, 400);
        } catch (IOException e) {
            System.err.println("Problem occurred creating chart.");
        }
    }

    static void uniqExp() throws IOException {
        int elements = 0;
        ArrayList Exp = new ArrayList();
        CSVReader rde = new CSVReader(new FileReader("tmp0/exp.types"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            if (!Exp.contains(nextL[0])) {
                Exp.add(nextL[0]);
                elements++;
            }
        }
        Collections.sort(Exp);
        File f3 = new File("tmp0/exp2.types");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        for (int i = 0; i < elements; i++) {
            String exp2 = (String) Exp.get(i);
            os3.println(exp2);
        }
        os3.close();
    }

    static void uniqInc() throws IOException {
        int elements = 0;
        ArrayList Exp = new ArrayList();
        CSVReader rde = new CSVReader(new FileReader("tmp0/inc.types"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            if (!Exp.contains(nextL[0])) {
                Exp.add(nextL[0]);
                elements++;
            }
        }
        Collections.sort(Exp);
        File f3 = new File("tmp0/inc2.types");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        for (int i = 0; i < elements; i++) {
            String exp2 = (String) Exp.get(i);
            os3.println(exp2);
        }
        os3.close();
    }

    static void uniqDiv() throws IOException {
        int elements = 0;
        ArrayList Exp = new ArrayList();
        CSVReader rde = new CSVReader(new FileReader("tmp0/div.types"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            if (!Exp.contains(nextL[0])) {
                Exp.add(nextL[0]);
                elements++;
            }
        }
        Collections.sort(Exp);
        File f3 = new File("tmp0/div2.types");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        for (int i = 0; i < elements; i++) {
            String exp2 = (String) Exp.get(i);
            os3.println(exp2);
        }
        os3.close();
    }

    static void totalDiv() throws IOException {
        ExtensionFilter filter = new ExtensionFilter(".div");
        File dir = new File("tmp0/");
        String list[] = dir.list(filter);
        for (int i = 0; i < list.length; i++) {
            totalDiv2(list[i]);
        }
    }

    static void totalDiv2(String file2) throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/" + file2));
        float total = 0.0F;
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            float amount = Float.valueOf(nextL[1].trim()).floatValue();
            total += amount;
            File f3 = new File("tmp0/" + file2 + ".divtotal");
            FileOutputStream out3 = new FileOutputStream(f3);
            PrintStream os3 = new PrintStream(out3);
            os3.println(total);
            os3.close();
        }
        rde.close();
    }

    static void monthlyHeader() throws IOException {
        File f = new File("output/cvreport2.html");
        FileOutputStream out = new FileOutputStream(f, true);
        PrintStream os2 = new PrintStream(out);
        os2.println("<br style=\"page-break-before: always;\"></br>");
        os2.println("<table style=\"-fs-table-paginate: paginate;\" cellSpacing=\"0\" cellPadding=\"5\" border=\"1\" id=\"table31\" width=\"650\">");
        os2.println("<thead>");
        os2.println("<tr vAlign=\"center\">");
        os2.println("<th></th>");
        os2.println("<th>Jan</th>");
        os2.println("<th>Feb</th>");
        os2.println("<th>Mar</th>");
        os2.println("<th>Apr</th>");
        os2.println("<th>May</th>");
        os2.println("<th>Jun</th>");
        os2.println("<th>Jul</th>");
        os2.println("<th>Aug</th>");
        os2.println("<th>Sep</th>");
        os2.println("<th>Oct</th>");
        os2.println("<th>Nov</th>");
        os2.println("<th>Dec</th>");
        os2.println("<th>Total</th>");
        os2.println("</tr>");
        os2.println("</thead>");
        os2.println("<tbody>");
        os2.close();
    }

    static void monthlyFooter() throws IOException {
        File f = new File("output/cvreport2.html");
        FileOutputStream out = new FileOutputStream(f, true);
        PrintStream os = new PrintStream(out);
        os.println("</tbody>");
        os.println("</table>");
        os.close();
    }

    static void monthlyDiv() throws IOException {
        File f = new File("output/cvreport2.html");
        FileOutputStream out = new FileOutputStream(f, true);
        PrintStream os = new PrintStream(out);
        os.println("<tr>");
        os.println("<td align=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"font" + "-size: 6pt\"><B>Dividends</B></font></td>");
        os.println("<td>\" \"</td><td>\" \"</td><td>\" \"</td><td>\" \"</td></tr>");
        CSVReader rde = new CSVReader(new FileReader("tmp0/div2.types"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            os.println("<tr><td align=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"font" + "-size: 6pt\">\"  \"" + nextL[0].replace('&', '_') + "</font></td>");
            for (int i = 1; i < 13; i++) {
                String s = "\" \"";
                File f2 = new File("tmp0/2008." + i + ".div");
                if (f2.exists()) {
                    float total2 = 0.0F;
                    CSVReader rde2 = new CSVReader(new FileReader("tmp0/2008." + i + ".div"));
                    String nextL2[];
                    while ((nextL2 = rde2.readNext()) != null) {
                        if (nextL2[0].equals(nextL[0])) {
                            String unitsfmt = "0.00";
                            DecimalFormat df2 = new DecimalFormat(unitsfmt);
                            float value = Float.valueOf(nextL2[1].trim()).floatValue();
                            total2 += value;
                            s = df2.format(total2);
                        }
                    }
                }
                os.println("<td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"fon" + "t-size: 6pt\">" + s + "</font></td>");
            }
            os.println("</tr>");
        }
        os.close();
    }

    static void monthlyExp() throws IOException {
        File f = new File("output/cvreport2.html");
        FileOutputStream out = new FileOutputStream(f, true);
        PrintStream os = new PrintStream(out);
        os.println("<tr>");
        os.println("<td align=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"font" + "-size: 8pt\"><B>Expenses</B></font></td>");
        os.println("<td>\" \"</td><td>\" \"</td><td>\" \"</td><td>\" \"</td></tr>");
        CSVReader rde = new CSVReader(new FileReader("tmp0/exp2.types"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            os.println("<tr><td align=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">\"  \"" + nextL[0] + "</font></td>");
            float total3 = 0.0F;
            for (int i = 1; i < 13; i++) {
                String s = "\" \"";
                File f2 = new File("tmp0/2008." + i + ".exp");
                if (f2.exists()) {
                    float total2 = 0.0F;
                    CSVReader rde2 = new CSVReader(new FileReader("tmp0/2008." + i + ".exp"));
                    String nextL2[];
                    while ((nextL2 = rde2.readNext()) != null) {
                        if (nextL2[3].equals(nextL[0])) {
                            float value = Float.valueOf(nextL2[4].trim()).floatValue();
                            total2 += value;
                            total3 += value;
                            String unitsfmt = "0.00";
                            DecimalFormat df2 = new DecimalFormat(unitsfmt);
                            s = df2.format(total2);
                        }
                    }
                }
                os.println("<td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">" + s + "</font></td>");
            }
            String s2 = "\" \"";
            String unitsfmt2 = "0.00";
            DecimalFormat df3 = new DecimalFormat(unitsfmt2);
            s2 = df3.format(total3);
            os.println("<td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">" + s2 + "</font></td>");
            os.println("</tr>");
        }
        os.close();
    }

    static void monthlyInc() throws IOException {
        File f = new File("output/cvreport2.html");
        FileOutputStream out = new FileOutputStream(f, true);
        PrintStream os = new PrintStream(out);
        os.println("<tr>");
        os.println("<td align=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"font" + "-size: 6pt\"><B>Income</B></font></td>");
        os.println("<td>\" \"</td><td>\" \"</td><td>\" \"</td><td>\" \"</td></tr>");
        CSVReader rde = new CSVReader(new FileReader("tmp0/inc2.types"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            os.println("<tr><td align=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">\"  \"" + nextL[0] + "</font></td>");
            for (int i = 1; i < 13; i++) {
                String s = "\" \"";
                File f2 = new File("tmp0/2008." + i + ".inc");
                if (f2.exists()) {
                    float total2 = 0.0F;
                    CSVReader rde2 = new CSVReader(new FileReader("tmp0/2008." + i + ".inc"));
                    String nextL2[];
                    while ((nextL2 = rde2.readNext()) != null) {
                        if (nextL2[0].equals(nextL[0])) {
                            float value = Float.valueOf(nextL2[1].trim()).floatValue();
                            total2 += value;
                            String unitsfmt = "0.00";
                            DecimalFormat df2 = new DecimalFormat(unitsfmt);
                            s = df2.format(total2);
                        }
                    }
                }
                os.println("<td align=\"right\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">" + s + "</font></td>");
            }
            os.println("</tr>");
        }
        os.close();
    }

    static void piePort(String account) throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/" + account + ".secvalue"));
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            float units = Float.valueOf(nextL[1].trim()).floatValue();
            float market = Float.valueOf(nextL[3].trim()).floatValue();
            float total = units * market;
            pieDataset.setValue(nextL[0], total);
        }
        PiePlot plot = new PiePlot(pieDataset);
        plot.setDataset(DatasetUtilities.createConsolidatedPieDataset(plot.getDataset(), "Other", 0.04, 5));
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} = {2}"));
        JFreeChart chart = new JFreeChart(plot);
        plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator(""));
        ChartUtilities.saveChartAsPNG(new File("output/" + account + ".pie.png"), chart, 670, 400);
    }

    static void updateNetworth(String date, String value) throws IOException {
        float new_total, total = 0;
        File f = new File("tmp0/networth.money");
        if (f.exists()) {
            CSVReader rde = new CSVReader(new FileReader("tmp0/networth.money"));
            String nextL[];
            while ((nextL = rde.readNext()) != null) {
                total = Float.valueOf(nextL[1].trim()).floatValue();
            }
            rde.close();
        }
        float flvalue = Float.valueOf(value.trim()).floatValue();
        new_total = total + flvalue;
        File f2 = new File("tmp0/networth.money");
        FileOutputStream out2 = new FileOutputStream(f2, true);
        PrintStream os2 = new PrintStream(out2);
        os2.println(date + "," + new_total);
        os2.close();
    }

    static void networthGraph() throws IOException {
        CSVReader rde = new CSVReader(new FileReader("tmp0/networth.money"));
        float total = 0.0F;
        TimeSeries pop = new TimeSeries("$", org.jfree.data.time.Day.class);
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            float amount = Float.valueOf(nextL[1].trim()).floatValue();
            StringTokenizer st = new StringTokenizer(nextL[0], "/", false);
            int year = Integer.parseInt(st.nextToken());
            int month = Integer.parseInt(st.nextToken());
            int day = Integer.parseInt(st.nextToken());
            pop.addOrUpdate(new Day(day, month, year), amount);
        }
        rde.close();
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(pop);
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Networth", "Date", "$", dataset, true, true, false);
        try {
            ChartUtilities.saveChartAsPNG(new File("output/networth.png"), chart, 670, 400);
        } catch (IOException e) {
            System.err.println("Problem occurred creating chart.");
        }
        File f2 = new File("output/cvreport.html");
        FileOutputStream out2 = new FileOutputStream(f2, true);
        PrintStream os2 = new PrintStream(out2);
        os2.println("<br style=\"page-break-before:always;\"></br>");
        os2.println("<img src=\"networth.png\"></img><BR></BR>");
        os2.println("<table width=\"670\" border=\"1\" bordercolor=\"#CCCCCC\" cellpadding=\"4\" cellspacing=\"0\">");
        os2.println("<tr><td colspan=\"2\"><font face=\"arial\"><small><b>Assets</b></small></font></td></tr>");
        os2.println("<tr><td colspan=\"2\"><font face=\"arial\"><small><i>Chequing</i></small></font></td></tr>");
        CSVReader rde3 = new CSVReader(new FileReader("tmp0/CHEQUING.accounttype"));
        String nextL3[], nextL2[];
        String amount = "0";
        while ((nextL3 = rde3.readNext()) != null) {
            CSVReader rde2 = new CSVReader(new FileReader("tmp0/" + nextL3[0] + ".cash.total"));
            while ((nextL2 = rde2.readNext()) != null) {
                StringTokenizer st = new StringTokenizer(nextL2[0], " ", false);
                String date = (st.nextToken());
                amount = (st.nextToken());
            }
            os2.println("<tr><td><font face=\"arial\"><small>" + nextL3[0] + "</small></font></td>");
            os2.println("<td align=\"right\"><font face=\"arial\"><small>" + amount + "</small></font></td></tr>");
            rde2.close();
        }
        rde3.close();
        os2.println("</table>");
        os2.println("<br style=\"page-break-before:always;\"></br>");
        os2.close();
    }

    static void monthlyAccountValues(String date, String account, Float value) throws IOException {
        StringTokenizer st = new StringTokenizer(date, "/", false);
        int year = Integer.parseInt(st.nextToken());
        int month = Integer.parseInt(st.nextToken());
        File f3 = new File("tmp0/" + year + "." + month + "." + account + ".totalvalue");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        os3.println(date + "," + value);
        os3.close();
    }

    static void monthlyDisplayValues(String types) throws IOException {
        File f = new File("output/cvreport.html");
        FileOutputStream out = new FileOutputStream(f, true);
        PrintStream os = new PrintStream(out);
        os.println("<TR><TH HEIGHT=\"4\"></TH></TR>");
        os.println("<TR>");
        os.println("<TD ALIGN=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"font" + "-size: 6pt\"><B>" + types + "</B></font></TD>");
        os.println("<TD>\" \"</TD><TD>\" \"</TD><TD>\" \"</TD><TD>\" \"</TD></TR>");
        CSVReader rde = new CSVReader(new FileReader("tmp0/" + types + ".accounttype"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            os.println("<TR><TD ALIGN=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">\"  \"" + nextL[0] + "</font></TD>");
            for (int i = 1; i < 13; i++) {
                String s = "\" \"";
                File f2 = new File("tmp0/2008." + i + "." + nextL[0] + ".totalvalue");
                if (f2.exists()) {
                    float total2 = 0.0F;
                    CSVReader rde2 = new CSVReader(new FileReader("tmp0/2008." + i + "." + nextL[0] + ".totalvalue"));
                    String nextL2[];
                    while ((nextL2 = rde2.readNext()) != null) {
                        float value = Float.valueOf(nextL2[1].trim()).floatValue();
                        String unitsfmt = "0.00";
                        DecimalFormat df2 = new DecimalFormat(unitsfmt);
                        s = df2.format(value);
                    }
                }
                os.println("<TD ALIGN=\"left\"><font face=\"Verdana, Arial, Helvetica, sans-serif\" style=\"" + "font-size: 6pt\">" + s + "</font></TD>");
            }
            os.println("</TR>");
        }
        os.close();
    }

    static void addAllSecs(String sec, float marketvalue) throws IOException {
        File file = new File("tmp0/allsecs.secvalue");
        FileOutputStream out = new FileOutputStream(file, true);
        PrintStream os3 = new PrintStream(out);
        os3.println(sec + ",1,1," + marketvalue);
        os3.close();
        File file2 = new File("tmp0/sec.types");
        FileOutputStream out2 = new FileOutputStream(file2, true);
        PrintStream os2 = new PrintStream(out2);
        os2.println(sec);
        os2.close();
    }

    static void uniqSec() throws IOException {
        int elements = 0;
        ArrayList Exp = new ArrayList();
        CSVReader rde = new CSVReader(new FileReader("tmp0/sec.types"));
        String nextL[];
        while ((nextL = rde.readNext()) != null) {
            if (!Exp.contains(nextL[0])) {
                Exp.add(nextL[0]);
                elements++;
            }
        }
        Collections.sort(Exp);
        File f3 = new File("tmp0/sec2.types");
        FileOutputStream out3 = new FileOutputStream(f3, true);
        PrintStream os3 = new PrintStream(out3);
        for (int i = 0; i < elements; i++) {
            String exp2 = (String) Exp.get(i);
            os3.println(exp2);
        }
        os3.close();
        uniqSec2();
    }

    static void uniqSec2() throws IOException {
        CSVReader rde4 = new CSVReader(new FileReader("tmp0/sec2.types"));
        String nextL4[];
        String sec = "";
        while ((nextL4 = rde4.readNext()) != null) {
            float total2 = 0.0F;
            CSVReader rde2 = new CSVReader(new FileReader("tmp0/allsecs.secvalue"));
            String nextL2[];
            while ((nextL2 = rde2.readNext()) != null) {
                if (nextL2[0].equals(nextL4[0])) {
                    float value = Float.valueOf(nextL2[3].trim()).floatValue();
                    total2 += value;
                    sec = nextL2[0];
                }
            }
            File file6 = new File("tmp0/allsecs2.secvalue");
            FileOutputStream out6 = new FileOutputStream(file6, true);
            PrintStream os6 = new PrintStream(out6);
            os6.println(sec + ",1,1," + total2);
            os6.close();
        }
    }

    static void html_pdf() throws IOException {
        String inputFile = "output/cvreport2.html";
        String url = new File(inputFile).toURI().toURL().toString();
        String outputFile = "output/cvreport2.pdf";
        OutputStream os = new FileOutputStream(outputFile);
        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocument(url);
            renderer.layout();
            renderer.createPDF(os);
        } catch (DocumentException e) {
            System.err.println("Problem occurred.");
        }
        os.close();
    }
}
