package beans;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Book {

    private PassengerBean booking_details;

    private DBconn dbc;

    private Connection conn;

    private int number_of_tickets_rem;

    private List<String> coach;

    private List<String> seatno;

    private boolean[] finalize;

    private String sourcenws;

    private String destnnws;

    private static final String pref[] = { "UB", "MB", "LB", "LB", "MB", "UB", "SL", "SU" };

    private boolean useragent;

    private boolean firsttime;

    private boolean availfin = false;

    public Book(PassengerBean details, boolean agent) {
        booking_details = details;
        number_of_tickets_rem = 0;
        coach = new ArrayList<String>();
        seatno = new ArrayList<String>();
        for (int i = 1; i <= booking_details.getNoOfPersons(); i++) {
            coach.add("WL");
            booking_details.coachlist.add("WL");
            seatno.add("WL");
            booking_details.seatlist.add("WL");
        }
        sourcenws = booking_details.getSource().replaceAll(" ", "");
        destnnws = booking_details.getDestination().replaceAll(" ", "");
        useragent = agent;
    }

    private void InitConnection() {
        dbc = new DBconn();
        conn = dbc.getConnection();
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void EndConnection() {
        try {
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean Start() {
        try {
            InitConnection();
            FindAvail();
            if (availfin) {
                System.out.println("find avail completed");
                Reserve();
                EndConnection();
            } else conn.setAutoCommit(true);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void FindAvail() throws ParserConfigurationException, SQLException {
        Savepoint sp1;
        String availsql = "select xmlquery('$c/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]' ";
        availsql += "passing hp_administrator.availability.AVAIL as \"c\") ";
        availsql += " from hp_administrator.availability ";
        availsql += " where date = '" + booking_details.getDate() + "' and train_no like '" + booking_details.getTrain_no() + "'";
        System.out.println(availsql);
        String availxml = "";
        String seatxml = "";
        String navailstr = "";
        String nspavailstr = "";
        String currentcoachstr = "";
        String srctillstr = "", srcavailstr = "", srcmaxstr = "";
        Integer srctill, srcavail, srcmax;
        Integer navailcoach;
        Integer nspavailcoach, seatstart, seatcnt, alloccnt;
        String routesrcstr = "", routedeststr = "";
        PreparedStatement pstseat;
        Statement stavail, stavailupd, stseatupd, stseat;
        ResultSet rsavail, rsseat;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document docavail, docseattmp, docseatfin, docseat;
        Element rootavail, rootseat;
        Node n;
        try {
            stavail = conn.createStatement();
            sp1 = conn.setSavepoint();
            rsavail = stavail.executeQuery(availsql);
            if (rsavail.next()) availxml = rsavail.getString(1);
            System.out.println(availxml);
            StringBuffer StringBuffer1 = new StringBuffer(availxml);
            ByteArrayInputStream Bis1 = new ByteArrayInputStream(StringBuffer1.toString().getBytes("UTF-16"));
            docavail = db.parse(Bis1);
            StringWriter sw;
            OutputFormat formatter;
            formatter = new OutputFormat();
            formatter.setPreserveSpace(true);
            formatter.setEncoding("UTF-8");
            formatter.setOmitXMLDeclaration(true);
            XMLSerializer serializer;
            rootavail = docavail.getDocumentElement();
            NodeList coachlist = rootavail.getElementsByTagName("coach");
            Element currentcoach, minseat;
            Element routesrc, routedest, nextstn, dest, user, agent;
            NodeList nl, nl1;
            number_of_tickets_rem = booking_details.getNoOfPersons();
            int tickpos = 0;
            firsttime = true;
            boolean enterloop;
            for (int i = 0; i < coachlist.getLength(); i++) {
                currentcoach = (Element) coachlist.item(i);
                currentcoachstr = currentcoach.getAttribute("number");
                String coachmaxstr = currentcoach.getAttribute("coachmax");
                Integer coachmax = Integer.parseInt(coachmaxstr.trim());
                routesrc = (Element) currentcoach.getFirstChild();
                routedest = (Element) currentcoach.getLastChild();
                routedest = (Element) routedest.getPreviousSibling().getPreviousSibling().getPreviousSibling();
                routesrcstr = routesrc.getNodeName();
                routedeststr = routedest.getNodeName();
                String seatsql = "select xmlquery('$c/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]' ";
                seatsql += " passing hp_administrator.book_tickets.SEAT as \"c\") from hp_administrator.book_tickets ";
                seatsql += " where  date = '" + booking_details.getDate() + "' and train_no like '" + booking_details.getTrain_no() + "' ";
                System.out.println("route  :" + sourcenws);
                System.out.println("route  :" + destnnws);
                System.out.println("route src :" + routesrcstr);
                System.out.println("route dest :" + routedeststr);
                System.out.println(seatsql);
                stseat = conn.createStatement();
                rsseat = stseat.executeQuery(seatsql);
                if (rsseat.next()) seatxml = rsseat.getString(1);
                StringBuffer StringBuffer2 = new StringBuffer(seatxml);
                ByteArrayInputStream Bis2 = new ByteArrayInputStream(StringBuffer2.toString().getBytes("UTF-16"));
                docseat = db.parse(Bis2);
                rootseat = docseat.getDocumentElement();
                enterloop = false;
                if (routesrcstr.equals(sourcenws) && routedeststr.equals(destnnws)) {
                    System.out.println("case 1");
                    navailstr = routesrc.getTextContent();
                    navailcoach = Integer.parseInt(navailstr.trim());
                    if (useragent) nspavailstr = routesrc.getAttribute("agent"); else nspavailstr = routesrc.getAttribute("user");
                    nspavailcoach = Integer.parseInt(nspavailstr.trim());
                    srctillstr = routesrc.getAttribute(sourcenws + "TILL");
                    srctill = Integer.parseInt(srctillstr.trim());
                    srcmaxstr = routesrc.getAttribute(sourcenws);
                    srcmax = Integer.parseInt(srcmaxstr.trim());
                    srcavailstr = routesrc.getTextContent();
                    srcavail = Integer.parseInt(srcavailstr.trim());
                    seatstart = coachmax - srctill + 1;
                    seatcnt = srcmax;
                    alloccnt = srcmax - srcavail;
                    seatstart += alloccnt;
                    seatcnt -= alloccnt;
                    Element seat, stn;
                    NodeList nl3 = rootseat.getElementsByTagName("seat");
                    seat = (Element) nl3.item(0);
                    if (booking_details.getNoOfPersons() <= navailcoach && booking_details.getNoOfPersons() <= nspavailcoach) {
                        coach.clear();
                        booking_details.coachlist.clear();
                        booking_details.seatlist.clear();
                        seatno.clear();
                        for (tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                            coach.add(currentcoachstr);
                            booking_details.coachlist.add(currentcoachstr);
                            while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                                seat = (Element) seat.getNextSibling();
                            }
                            seatstart++;
                            System.out.println(seat.getFirstChild().getTextContent().trim());
                            seatno.add(seat.getFirstChild().getTextContent().trim());
                            booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                            nl3 = seat.getElementsByTagName(sourcenws);
                            stn = (Element) nl3.item(0);
                            while (!stn.getNodeName().equals(destnnws)) {
                                stn.setTextContent("0");
                                stn = (Element) stn.getNextSibling();
                            }
                        }
                        number_of_tickets_rem = 0;
                        navailcoach -= booking_details.getNoOfPersons();
                        nspavailcoach -= booking_details.getNoOfPersons();
                        if (!firsttime) conn.rollback(sp1);
                        String availupdstr = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupdstr += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + navailcoach + "\"";
                        availupdstr += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        int updvar = stavailupd.executeUpdate(availupdstr);
                        if (updvar > 0) System.out.println("upda avail success");
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        System.out.println(seatupdstr);
                        stseatupd = conn.createStatement();
                        updvar = stseatupd.executeUpdate(seatupdstr);
                        if (updvar > 0) System.out.println("upda seat success");
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupdstr = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupdstr += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + nspavailcoach + "\"";
                        availupdstr += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupdstr);
                        stavailupd = conn.createStatement();
                        updvar = stavailupd.executeUpdate(availupdstr);
                        if (updvar > 0) System.out.println("upda" + sp + " success");
                        break;
                    }
                    while (navailcoach > 0 && nspavailcoach > 0 && number_of_tickets_rem > 0) {
                        if (firsttime) {
                            sp1 = conn.setSavepoint();
                            firsttime = false;
                        }
                        enterloop = true;
                        coach.add(currentcoachstr);
                        booking_details.coachlist.add(currentcoachstr);
                        tickpos++;
                        number_of_tickets_rem--;
                        navailcoach--;
                        nspavailcoach--;
                        while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                            seat = (Element) seat.getNextSibling();
                        }
                        seatstart++;
                        System.out.println(seat.getFirstChild().getTextContent().trim());
                        seatno.add(seat.getFirstChild().getTextContent().trim());
                        booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                        nl3 = seat.getElementsByTagName(sourcenws);
                        stn = (Element) nl3.item(0);
                        while (!stn.getNodeName().equals(destnnws)) {
                            stn.setTextContent("0");
                            stn = (Element) stn.getNextSibling();
                        }
                        String availupdstr = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupdstr += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + navailcoach + "\"";
                        availupdstr += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupdstr);
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupdstr = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupdstr += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + sp + "\"]/" + sourcenws + "/@" + sp + " with \"" + nspavailcoach + "\"";
                        availupdstr += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupdstr);
                    }
                    if (enterloop) {
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        stseatupd = conn.createStatement();
                        stseatupd.executeUpdate(seatupdstr);
                    }
                } else if (routesrcstr.equals(sourcenws) && !routedeststr.equals(destnnws)) {
                    System.out.println("case 2");
                    String excesssrcstr = routesrc.getTextContent();
                    System.out.println(excesssrcstr);
                    Integer excesssrc = Integer.parseInt(excesssrcstr.trim());
                    NodeList nl2 = currentcoach.getElementsByTagName(destnnws);
                    Element e2 = (Element) nl2.item(0);
                    String desttillstr = e2.getAttribute(destnnws + "TILL");
                    System.out.println(desttillstr);
                    Integer desttillcnt = Integer.parseInt(desttillstr.trim());
                    srcmaxstr = routesrc.getAttribute(sourcenws);
                    System.out.println(srcmaxstr);
                    srcmax = Integer.parseInt(srcmaxstr.trim());
                    String spexcesssrcstr = "", spdesttillstr = "";
                    if (useragent) {
                        spexcesssrcstr = routesrc.getAttribute("agent");
                        spdesttillstr = e2.getAttribute("agenttill");
                    } else {
                        spexcesssrcstr = routesrc.getAttribute("user");
                        spdesttillstr = e2.getAttribute("usertill");
                    }
                    System.out.println(spdesttillstr);
                    System.out.println(spexcesssrcstr);
                    Integer spdesttillcnt = Integer.parseInt(spdesttillstr.trim());
                    Integer spexcesssrc = Integer.parseInt(spexcesssrcstr.trim());
                    Element seat, stn;
                    if (booking_details.getNoOfPersons() <= desttillcnt && booking_details.getNoOfPersons() <= spdesttillcnt) {
                        seatstart = coachmax - desttillcnt + 1;
                        seatcnt = desttillcnt;
                        NodeList nl3 = rootseat.getElementsByTagName("seat");
                        seat = (Element) nl3.item(0);
                        coach.clear();
                        seatno.clear();
                        booking_details.coachlist.clear();
                        booking_details.seatlist.clear();
                        for (tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                            coach.add(currentcoachstr);
                            booking_details.coachlist.add(currentcoachstr);
                            while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                                seat = (Element) seat.getNextSibling();
                            }
                            seatstart++;
                            System.out.println(seat.getFirstChild().getTextContent().trim());
                            seatno.add(seat.getFirstChild().getTextContent().trim());
                            booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                            nl3 = seat.getElementsByTagName(sourcenws);
                            stn = (Element) nl3.item(0);
                            while (!stn.getNodeName().equals(destnnws)) {
                                stn.setTextContent("0");
                                stn = (Element) stn.getNextSibling();
                            }
                        }
                        number_of_tickets_rem = 0;
                        desttillcnt -= booking_details.getNoOfPersons();
                        spdesttillcnt -= booking_details.getNoOfPersons();
                        if (!firsttime) conn.rollback(sp1);
                        String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                        availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + destnnws + "TILL" + " with \"" + desttillcnt + "\" ";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupd);
                        stavailupd = conn.createStatement();
                        int upst = stavailupd.executeUpdate(availupd);
                        if (upst > 0) System.out.println("update avail success");
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        System.out.println(seatupdstr);
                        stseatupd = conn.createStatement();
                        upst = stseatupd.executeUpdate(seatupdstr);
                        if (upst > 0) System.out.println("update seat success");
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                        availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + sp + "till" + " with \"" + spdesttillcnt + "\" ";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupd);
                        stavailupd = conn.createStatement();
                        upst = stavailupd.executeUpdate(availupd);
                        if (upst > 0) System.out.println("update " + sp + " success");
                        break;
                    } else if (booking_details.getNoOfPersons() > spdesttillcnt && spdesttillcnt > 0 && booking_details.getNoOfPersons() <= spdesttillcnt + spexcesssrc) {
                        int diff = 0;
                        if (booking_details.getNoOfPersons() > spdesttillcnt) diff = booking_details.getNoOfPersons() - spdesttillcnt;
                        tickpos = 0;
                        boolean initflg = true;
                        seatstart = coachmax - desttillcnt + 1;
                        seatcnt = desttillcnt;
                        NodeList nl3 = rootseat.getElementsByTagName("seat");
                        seat = (Element) nl3.item(0);
                        coach.clear();
                        seatno.clear();
                        booking_details.coachlist.clear();
                        booking_details.seatlist.clear();
                        for (tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                            coach.add(currentcoachstr);
                            booking_details.coachlist.add(currentcoachstr);
                            while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                                seat = (Element) seat.getNextSibling();
                            }
                            seatstart++;
                            System.out.println(seat.getFirstChild().getTextContent().trim());
                            seatno.add(seat.getFirstChild().getTextContent().trim());
                            booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                            nl3 = seat.getElementsByTagName(sourcenws);
                            stn = (Element) nl3.item(0);
                            while (!stn.getNodeName().equals(destnnws)) {
                                stn.setTextContent("0");
                                stn = (Element) stn.getNextSibling();
                            }
                            if (spdesttillcnt != 0) {
                                desttillcnt--;
                                spdesttillcnt--;
                            }
                            System.out.println("desttillcnt=" + desttillcnt + " spdes = " + desttillcnt + "initflg =" + initflg);
                            if (spdesttillcnt == 0 && initflg == true) {
                                alloccnt = srcmax - excesssrc;
                                seatstart = 1 + alloccnt;
                                initflg = false;
                                seat = (Element) seat.getParentNode().getFirstChild();
                            }
                        }
                        excesssrc -= diff;
                        spexcesssrc -= diff;
                        number_of_tickets_rem = 0;
                        if (!firsttime) conn.rollback(sp1);
                        String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                        availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + destnnws + "TILL" + " with \"" + desttillcnt + "\" ";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupd);
                        stavailupd = conn.createStatement();
                        int upst = stavailupd.executeUpdate(availupd);
                        if (upst > 0) System.out.println("update avail success");
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + excesssrc + "\"";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupd);
                        stavailupd = conn.createStatement();
                        upst = stavailupd.executeUpdate(availupd);
                        if (upst > 0) System.out.println("update avail success");
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        System.out.println(seatupdstr);
                        stseatupd = conn.createStatement();
                        upst = stseatupd.executeUpdate(seatupdstr);
                        if (upst > 0) System.out.println("update seat success");
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                        availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + sp + "till" + " with \"" + spdesttillcnt + "\" ";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupd);
                        stavailupd = conn.createStatement();
                        upst = stavailupd.executeUpdate(availupd);
                        if (upst > 0) System.out.println("update " + sp + " success");
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + spexcesssrc + "\"";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupd);
                        stavailupd = conn.createStatement();
                        upst = stavailupd.executeUpdate(availupd);
                        if (upst > 0) System.out.println("update " + sp + " success");
                        break;
                    } else if (booking_details.getNoOfPersons() > spdesttillcnt && spdesttillcnt == 0 && booking_details.getNoOfPersons() <= spexcesssrc) {
                        alloccnt = srcmax - excesssrc;
                        seatstart = 1 + alloccnt;
                        tickpos = 0;
                        boolean initflg = true;
                        NodeList nl3 = rootseat.getElementsByTagName("seat");
                        seat = (Element) nl3.item(0);
                        coach.clear();
                        seatno.clear();
                        booking_details.coachlist.clear();
                        booking_details.seatlist.clear();
                        for (tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                            coach.add(currentcoachstr);
                            booking_details.coachlist.add(currentcoachstr);
                            while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                                seat = (Element) seat.getNextSibling();
                            }
                            seatstart++;
                            System.out.println(seat.getFirstChild().getTextContent().trim());
                            seatno.add(seat.getFirstChild().getTextContent().trim());
                            booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                            nl3 = seat.getElementsByTagName(sourcenws);
                            stn = (Element) nl3.item(0);
                            while (!stn.getNodeName().equals(destnnws)) {
                                stn.setTextContent("0");
                                stn = (Element) stn.getNextSibling();
                            }
                            System.out.println("desttillcnt=" + desttillcnt + " spdes = " + desttillcnt + "initflg =" + initflg);
                        }
                        excesssrc -= booking_details.getNoOfPersons();
                        spexcesssrc -= booking_details.getNoOfPersons();
                        ;
                        number_of_tickets_rem = 0;
                        if (!firsttime) conn.rollback(sp1);
                        String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + excesssrc + "\"";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupd);
                        stavailupd = conn.createStatement();
                        int upst = stavailupd.executeUpdate(availupd);
                        if (upst > 0) System.out.println("update avail success");
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        System.out.println(seatupdstr);
                        stseatupd = conn.createStatement();
                        upst = stseatupd.executeUpdate(seatupdstr);
                        if (upst > 0) System.out.println("update seat success");
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + spexcesssrc + "\"";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupd);
                        stavailupd = conn.createStatement();
                        upst = stavailupd.executeUpdate(availupd);
                        if (upst > 0) System.out.println("update " + sp + " success");
                        break;
                    }
                    NodeList nl3 = rootseat.getElementsByTagName("seat");
                    seat = (Element) nl3.item(0);
                    seatstart = 1;
                    String sp = "";
                    if (useragent) sp = "agent"; else sp = "user";
                    while (spexcesssrc + spdesttillcnt > 0 && number_of_tickets_rem > 0) {
                        if (firsttime) {
                            sp1 = conn.setSavepoint();
                            firsttime = false;
                        }
                        enterloop = true;
                        if (spdesttillcnt > 0) {
                            seatstart = coachmax - desttillcnt + 1;
                            desttillcnt--;
                            spdesttillcnt--;
                            String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                            availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + destnnws + "TILL" + " with \"" + desttillcnt + "\" ";
                            availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                            stavailupd = conn.createStatement();
                            stavailupd.executeUpdate(availupd);
                            availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                            availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + sp + "till" + " with \"" + spdesttillcnt + "\" ";
                            availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                            stavailupd = conn.createStatement();
                            stavailupd.executeUpdate(availupd);
                        } else if (spdesttillcnt == 0) {
                            alloccnt = srcmax - excesssrc;
                            seatstart = 1 + alloccnt;
                            excesssrc--;
                            spexcesssrc--;
                            String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                            availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + excesssrc + "\"";
                            availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                            stavailupd = conn.createStatement();
                            stavailupd.executeUpdate(availupd);
                            availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                            availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + spexcesssrc + "\"";
                            availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                            stavailupd = conn.createStatement();
                            stavailupd.executeUpdate(availupd);
                        }
                        while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                            seat = (Element) seat.getNextSibling();
                        }
                        nl3 = seat.getElementsByTagName(sourcenws);
                        stn = (Element) nl3.item(0);
                        while (!stn.getNodeName().equals(destnnws)) {
                            stn.setTextContent("0");
                            stn = (Element) stn.getNextSibling();
                        }
                        coach.add(currentcoachstr);
                        booking_details.coachlist.add(currentcoachstr);
                        tickpos++;
                        number_of_tickets_rem--;
                    }
                    if (enterloop) {
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        stseatupd = conn.createStatement();
                        stseatupd.executeUpdate(seatupdstr);
                    }
                } else if (!routesrcstr.equals(sourcenws) && routedeststr.equals(destnnws)) {
                    System.out.println("case 3");
                    NodeList nl2 = currentcoach.getElementsByTagName(sourcenws);
                    Element e2 = (Element) nl2.item(0);
                    navailstr = e2.getTextContent();
                    System.out.println(navailstr);
                    navailcoach = Integer.parseInt(navailstr.trim());
                    if (useragent) nspavailstr = e2.getAttribute("agent"); else nspavailstr = e2.getAttribute("user");
                    System.out.println(nspavailstr);
                    nspavailcoach = Integer.parseInt(nspavailstr.trim());
                    srctillstr = e2.getAttribute(sourcenws + "TILL");
                    System.out.println(srctillstr);
                    srctill = Integer.parseInt(srctillstr.trim());
                    srcmaxstr = e2.getAttribute(sourcenws);
                    System.out.println(srcmaxstr);
                    srcmax = Integer.parseInt(srcmaxstr.trim());
                    seatstart = coachmax - srctill + 1;
                    seatcnt = srcmax;
                    alloccnt = srcmax - navailcoach;
                    seatstart += alloccnt;
                    seatcnt -= alloccnt;
                    Element seat, stn;
                    NodeList nl3 = rootseat.getElementsByTagName("seat");
                    seat = (Element) nl3.item(0);
                    if (booking_details.getNoOfPersons() <= navailcoach && booking_details.getNoOfPersons() <= nspavailcoach) {
                        coach.clear();
                        seatno.clear();
                        booking_details.coachlist.clear();
                        booking_details.seatlist.clear();
                        for (tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                            coach.add(currentcoachstr);
                            booking_details.coachlist.add(currentcoachstr);
                            while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                                seat = (Element) seat.getNextSibling();
                            }
                            seatstart++;
                            System.out.println(seat.getFirstChild().getTextContent().trim());
                            seatno.add(seat.getFirstChild().getTextContent().trim());
                            booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                            nl3 = seat.getElementsByTagName(sourcenws);
                            stn = (Element) nl3.item(0);
                            while (!stn.getNodeName().equals(destnnws)) {
                                stn.setTextContent("0");
                                stn = (Element) stn.getNextSibling();
                            }
                        }
                        number_of_tickets_rem = 0;
                        navailcoach -= booking_details.getNoOfPersons();
                        nspavailcoach -= booking_details.getNoOfPersons();
                        String availupdstr = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupdstr += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + navailcoach + "\"";
                        availupdstr += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupdstr);
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        stseatupd = conn.createStatement();
                        stseatupd.executeUpdate(seatupdstr);
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupdstr = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupdstr += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + nspavailcoach + "\"";
                        availupdstr += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupdstr);
                        break;
                    }
                    while (navailcoach > 0 && nspavailcoach > 0 && number_of_tickets_rem > 0) {
                        if (firsttime) {
                            sp1 = conn.setSavepoint();
                            firsttime = false;
                        }
                        enterloop = true;
                        coach.add(currentcoachstr);
                        booking_details.coachlist.add(currentcoachstr);
                        tickpos++;
                        number_of_tickets_rem--;
                        navailcoach--;
                        nspavailcoach--;
                        while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                            seat = (Element) seat.getNextSibling();
                        }
                        seatstart++;
                        System.out.println(seat.getFirstChild().getTextContent().trim());
                        seatno.add(seat.getFirstChild().getTextContent().trim());
                        booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                        nl3 = seat.getElementsByTagName(sourcenws);
                        stn = (Element) nl3.item(0);
                        while (!stn.getNodeName().equals(destnnws)) {
                            stn.setTextContent("0");
                            stn = (Element) stn.getNextSibling();
                        }
                        String availupdstr = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupdstr += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + navailcoach + "\"";
                        availupdstr += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupdstr);
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupdstr);
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupdstr = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupdstr += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + nspavailcoach + "\"";
                        availupdstr += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        System.out.println(availupdstr);
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupdstr);
                    }
                    if (enterloop) {
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        System.out.println("!@#------->" + seatupdstr);
                        stseatupd = conn.createStatement();
                    }
                } else if (!routesrcstr.equals(sourcenws) && !routedeststr.equals(destnnws)) {
                    System.out.println("case 4");
                    srcmaxstr = routesrc.getAttribute(sourcenws);
                    System.out.println(srcmaxstr);
                    srcmax = Integer.parseInt(srcmaxstr.trim());
                    Element seat, stn;
                    NodeList nl2 = currentcoach.getElementsByTagName(sourcenws);
                    Element e2 = (Element) nl2.item(0);
                    navailstr = e2.getTextContent();
                    Integer excesssrc = Integer.parseInt(navailstr.trim());
                    nl2 = currentcoach.getElementsByTagName(destnnws);
                    e2 = (Element) nl2.item(0);
                    navailstr = e2.getAttribute(destnnws + "TILL");
                    Integer desttillcnt = Integer.parseInt(navailstr.trim());
                    String spexcesssrcstr = "", spdesttillstr = "";
                    if (useragent) {
                        spexcesssrcstr = routesrc.getAttribute("agent");
                        spdesttillstr = e2.getAttribute("agenttill");
                    } else {
                        spexcesssrcstr = routesrc.getAttribute("user");
                        spdesttillstr = e2.getAttribute("usertill");
                    }
                    Integer spdesttillcnt = Integer.parseInt(spdesttillstr.trim());
                    Integer spexcesssrc = Integer.parseInt(spexcesssrcstr.trim());
                    NodeList nl3 = rootseat.getElementsByTagName("seat");
                    seat = (Element) nl3.item(0);
                    boolean initflg = true;
                    if (booking_details.getNoOfPersons() <= spdesttillcnt) {
                        seatstart = coachmax - desttillcnt + 1;
                        seatcnt = desttillcnt;
                        coach.clear();
                        seatno.clear();
                        booking_details.coachlist.clear();
                        booking_details.seatlist.clear();
                        for (tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                            coach.add(currentcoachstr);
                            booking_details.coachlist.add(currentcoachstr);
                            while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                                seat = (Element) seat.getNextSibling();
                            }
                            seatstart++;
                            System.out.println(seat.getFirstChild().getTextContent().trim());
                            seatno.add(seat.getFirstChild().getTextContent().trim());
                            booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                            nl3 = seat.getElementsByTagName(sourcenws);
                            stn = (Element) nl3.item(0);
                            while (!stn.getNodeName().equals(destnnws)) {
                                stn.setTextContent("0");
                                stn = (Element) stn.getNextSibling();
                            }
                        }
                        number_of_tickets_rem = 0;
                        desttillcnt -= booking_details.getNoOfPersons();
                        spdesttillcnt -= booking_details.getNoOfPersons();
                        if (!firsttime) conn.rollback(sp1);
                        String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                        availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + destnnws + "TILL" + " with \"" + desttillcnt + "\" ";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupd);
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        stseatupd = conn.createStatement();
                        stseatupd.executeUpdate(seatupdstr);
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                        availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + sp + "till" + " with \"" + spdesttillcnt + "\" ";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupd);
                        break;
                    } else if (booking_details.getNoOfPersons() > spdesttillcnt && spdesttillcnt > 0 && booking_details.getNoOfPersons() <= spdesttillcnt + spexcesssrc) {
                        int diff = 0;
                        if (booking_details.getNoOfPersons() > spdesttillcnt) diff = booking_details.getNoOfPersons() - spdesttillcnt;
                        seatstart = coachmax - desttillcnt + 1;
                        seatcnt = desttillcnt;
                        coach.clear();
                        seatno.clear();
                        booking_details.coachlist.clear();
                        booking_details.seatlist.clear();
                        for (tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                            coach.add(currentcoachstr);
                            booking_details.coachlist.add(currentcoachstr);
                            while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                                seat = (Element) seat.getNextSibling();
                            }
                            seatstart++;
                            System.out.println(seat.getFirstChild().getTextContent().trim());
                            seatno.add(seat.getFirstChild().getTextContent().trim());
                            booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                            nl3 = seat.getElementsByTagName(sourcenws);
                            stn = (Element) nl3.item(0);
                            while (!stn.getNodeName().equals(destnnws)) {
                                stn.setTextContent("0");
                                stn = (Element) stn.getNextSibling();
                            }
                            if (spdesttillcnt != 0) {
                                desttillcnt--;
                                spdesttillcnt--;
                            }
                            if (spdesttillcnt == 0 && initflg == true) {
                                alloccnt = srcmax - excesssrc;
                                seatstart = 1 + alloccnt;
                                initflg = false;
                            }
                        }
                        excesssrc -= diff;
                        spexcesssrc -= diff;
                        number_of_tickets_rem = 0;
                        if (!firsttime) conn.rollback(sp1);
                        String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                        availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + destnnws + "TILL" + " with \"" + desttillcnt + "\" ";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupd);
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + excesssrc + "\"";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupd);
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        stseatupd = conn.createStatement();
                        stseatupd.executeUpdate(seatupdstr);
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                        availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + sp + "till" + " with \"" + spdesttillcnt + "\" ";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupd);
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + spexcesssrc + "\"";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupd);
                        break;
                    } else if (spdesttillcnt == 0 && booking_details.getNoOfPersons() <= spexcesssrc) {
                        alloccnt = srcmax - excesssrc;
                        seatstart = 1 + alloccnt;
                        coach.clear();
                        seatno.clear();
                        booking_details.coachlist.clear();
                        booking_details.seatlist.clear();
                        for (tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                            coach.add(currentcoachstr);
                            booking_details.coachlist.add(currentcoachstr);
                            while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                                seat = (Element) seat.getNextSibling();
                            }
                            seatstart++;
                            System.out.println(seat.getFirstChild().getTextContent().trim());
                            seatno.add(seat.getFirstChild().getTextContent().trim());
                            booking_details.seatlist.add(seat.getFirstChild().getTextContent().trim());
                            nl3 = seat.getElementsByTagName(sourcenws);
                            stn = (Element) nl3.item(0);
                            while (!stn.getNodeName().equals(destnnws)) {
                                stn.setTextContent("0");
                                stn = (Element) stn.getNextSibling();
                            }
                        }
                        excesssrc -= booking_details.getNoOfPersons();
                        spexcesssrc -= booking_details.getNoOfPersons();
                        number_of_tickets_rem = 0;
                        if (!firsttime) conn.rollback(sp1);
                        String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + excesssrc + "\"";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupd);
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        stseatupd = conn.createStatement();
                        stseatupd.executeUpdate(seatupdstr);
                        String sp = "";
                        if (useragent) sp = "agent"; else sp = "user";
                        availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                        availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + spexcesssrc + "\"";
                        availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                        stavailupd = conn.createStatement();
                        stavailupd.executeUpdate(availupd);
                        break;
                    }
                    seatstart = 1;
                    String sp = "";
                    if (useragent) sp = "agent"; else sp = "user";
                    while (spexcesssrc + spdesttillcnt > 0 && number_of_tickets_rem > 0) {
                        if (firsttime) {
                            sp1 = conn.setSavepoint();
                            firsttime = false;
                        }
                        enterloop = true;
                        if (spdesttillcnt > 0) {
                            seatstart = coachmax - desttillcnt + 1;
                            desttillcnt--;
                            spdesttillcnt--;
                            String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                            availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + destnnws + "TILL" + " with \"" + desttillcnt + "\" ";
                            availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                            stavailupd = conn.createStatement();
                            stavailupd.executeUpdate(availupd);
                            availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL modify do replace value of ";
                            availupd += "$new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + destnnws + "/@" + sp + "till" + " with \"" + spdesttillcnt + "\" ";
                            availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                            stavailupd = conn.createStatement();
                            stavailupd.executeUpdate(availupd);
                        } else if (spdesttillcnt == 0) {
                            alloccnt = srcmax - excesssrc;
                            seatstart = 1 + alloccnt;
                            excesssrc--;
                            spexcesssrc--;
                            String availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                            availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + "/@" + sp + " with \"" + excesssrc + "\"";
                            availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                            stavailupd = conn.createStatement();
                            stavailupd.executeUpdate(availupd);
                            availupd = "update hp_administrator.availability set AVAIL = xmlquery( 'transform copy $new := $AVAIL  modify do ";
                            availupd += " replace value of $new/coach_status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"]/" + sourcenws + " with \"" + spexcesssrc + "\"";
                            availupd += " return  $new')  where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                            stavailupd = conn.createStatement();
                            stavailupd.executeUpdate(availupd);
                        }
                        while (Integer.parseInt(seat.getFirstChild().getTextContent().trim()) < seatstart) {
                            seat = (Element) seat.getNextSibling();
                        }
                        nl3 = seat.getElementsByTagName(sourcenws);
                        stn = (Element) nl3.item(0);
                        while (!stn.getNodeName().equals(destnnws)) {
                            stn.setTextContent("0");
                            stn = (Element) stn.getNextSibling();
                        }
                        coach.add(currentcoachstr);
                        booking_details.coachlist.add(currentcoachstr);
                        tickpos++;
                        number_of_tickets_rem--;
                    }
                    if (enterloop) {
                        sw = new StringWriter();
                        serializer = new XMLSerializer(sw, formatter);
                        serializer.serialize(docseat);
                        String seatupdstr = "update hp_administrator.book_tickets set SEAT = xmlquery( 'transform copy $new := $SEAT ";
                        seatupdstr += " modify do replace $new/status/class[@type=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoachstr + "\"] with " + sw.toString();
                        seatupdstr += " return $new') where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "'  ";
                        stseatupd = conn.createStatement();
                        stseatupd.executeUpdate(seatupdstr);
                    }
                }
            }
            availfin = true;
        } catch (SQLException e) {
            conn.rollback();
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            conn.rollback();
            e.printStackTrace();
        } catch (SAXException e) {
            conn.rollback();
            e.printStackTrace();
        } catch (IOException e) {
            conn.rollback();
            e.printStackTrace();
        }
    }

    private void FixSeat() {
    }

    private void Reserve() throws SQLException {
        Statement stbookings, stchartwl;
        String sp = "";
        if (useragent) sp = "agent"; else sp = "user";
        String userbooksql = "";
        String agentbooksql = "";
        String bookingid = String.valueOf(System.currentTimeMillis());
        String currentcoach;
        String currentseat;
        try {
            if (useragent) {
                agentbooksql = "update hp_administrator.agent_bookings set BOOKINGS = xmlquery('copy $new := $BOOKINGS modify do insert ";
                agentbooksql += " <detail booking_id=\"" + booking_details.getTicketno() + "\" status=\"open\" train_no=\"" + booking_details.getTrain_no() + "\" source=\"" + booking_details.getSource() + "\" dest=\"" + booking_details.getDestination() + "\" dep_date=\"" + booking_details.getDate() + "\" > ";
            } else if (!useragent) {
                userbooksql = "update hp_administrator.user_bookings set BOOKINGS = xmlquery('copy $new := $BOOKINGS modify do insert ";
                userbooksql += " <detail booking_id=\"" + booking_details.getTicketno() + "\" status=\"open\" train_no=\"" + booking_details.getTrain_no() + "\" source=\"" + booking_details.getSource() + "\" dest=\"" + booking_details.getDestination() + "\" dep_date=\"" + booking_details.getDate() + "\" > ";
            }
            for (int tickpos = 0; tickpos < booking_details.getNoOfPersons(); tickpos++) {
                currentcoach = coach.get(tickpos);
                currentseat = seatno.get(tickpos);
                if (!currentcoach.equals("WL")) {
                    String chartavailupdsql = "update hp_administrator.chart_wl_order set AVAILABLE_BOOKED = xmlquery('copy $new := $AVAILABLE_BOOKED   modify do insert ";
                    chartavailupdsql += "<seat number=\"" + currentseat + "\"><details user_id=\"" + booking_details.getUserId() + "\" usertype=\"" + sp + "\" ticket_no=\"" + booking_details.getTicketno() + "\" name=\"" + booking_details.getNameAt(tickpos) + "\" age=\"" + booking_details.getAgeAt(tickpos) + "\" sex=\"" + booking_details.getSexAt(tickpos) + "\" type=\"primary\"  /></seat>";
                    chartavailupdsql += " into $new/status/class[@name=\"" + booking_details.getTclass() + "\"]/coach[@number=\"" + currentcoach + "\"] ";
                    chartavailupdsql += " return  $new' ) where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                    System.out.println(chartavailupdsql);
                    stchartwl = conn.createStatement();
                    int updstat = stchartwl.executeUpdate(chartavailupdsql);
                    if (updstat > 0) System.out.println("chart_wl  availability  updated");
                } else if (currentcoach.equals("WL")) {
                    String chartwlupdsql = "update hp_administrator.chart_wl_order set WAITLISTING = xmlquery('copy $new := $WAITLISTING modify do insert ";
                    chartwlupdsql += "<details user_id=\"" + booking_details.getUserId() + "\" usertype=\"" + sp + "\" ticket_no=\"" + booking_details.getTicketno() + "\" name=\"" + booking_details.getNameAt(tickpos) + "\" age=\"" + booking_details.getAgeAt(tickpos) + "\" sex=\"" + booking_details.getSexAt(tickpos) + "\" type=\"primary\" /></seat>";
                    chartwlupdsql += " into $new/status/class[@name=\"" + booking_details.getTclass() + "\"] ";
                    chartwlupdsql += " return  $new' ) where train_no like '" + booking_details.getTrain_no() + "' and date = '" + booking_details.getDate() + "' ";
                    System.out.println(chartwlupdsql);
                    stchartwl = conn.createStatement();
                    int updstat = stchartwl.executeUpdate(chartwlupdsql);
                    if (updstat > 0) System.out.println("chart_wl  waitlisting  updated");
                }
                if (useragent) agentbooksql += "<person><coach>" + currentcoach + "</coach><seat>" + currentseat + "</seat></person>"; else userbooksql += "<person><coach>" + currentcoach + "</coach><seat>" + currentseat + "</seat></person>";
            }
            if (useragent) {
                agentbooksql += "</detail>   as first into $new/book return  $new' ) where agent_id like '" + booking_details.getUserId() + "'";
                System.out.println(agentbooksql);
                stbookings = conn.createStatement();
                int updstat = stbookings.executeUpdate(agentbooksql);
                if (updstat > 0) System.out.println("agent bookings updated");
            } else {
                userbooksql += "</detail>   as first into $new/book return  $new' ) where user_id like '" + booking_details.getUserId() + "'";
                System.out.println(userbooksql);
                stbookings = conn.createStatement();
                int updstat = stbookings.executeUpdate(userbooksql);
                if (updstat > 0) System.out.println("user bookings  updated");
            }
        } catch (SQLException e) {
            conn.rollback();
            e.printStackTrace();
        }
    }
}
