package dbaccess.dmsp;

import java.awt.*;
import dbaccess.util.*;

/*** This class is used to retrieve and print/display channel online and qc* descriptions.  Use the <i>Channel</i> constructor to create the channel* object for a specific channel online and availability code set.  It* retrieves the code descriptions when instantiating.  Use the <i>print()</i>* and <i>display()</i> methods to print channel infor to stdout or to* display it to an AWT text area, respectively.* <p>* Channels are assigned as follows:* <ol>* <li>OIS VIS* <li>OIS IR* <li>MI 85V* <li>MI 85H* <li>MI 37V* <li>MI 37H* <li>MI 22V* <li>MI 19V* <li>MI 19H* <li>T1* <li>T2* <li>J4* </ol>* Possible values for the online code are:* <ul>*     <b>0</b> = Not on-line* <br><b>1</b> = On-line browsable* </ul>* Possible values for the QC/Availability code are:* <ul>*     <b>0</b> = Not available* <br><b>1</b> = Available/Unknown QC* <br><b>2</b> = Available/Bad QC* <br><b>3</b> = Available/Suspect QC* <br><b>4</b> = Available/Good QC* </ul>*/
public class Channel {

    String name;

    char online;

    String onlineDesc;

    char qc;

    String qcDesc;

    /**    * Creates a channel from its channel number and get descriptions for the    * channel Online and QC/Availability codes    * @param c Channel number (1-12).    * @param o Online code (0-1).    * @param q QC code (0-4).    */
    public Channel(int c, char o, char q) {
        name = getChannelDesc(c);
        online = o;
        onlineDesc = getOnlineDesc(o);
        qc = q;
        qcDesc = getQcDesc(q);
    }

    private static String getChannelDesc(int ch) {
        if (ch == 1) return "OIS VIS";
        if (ch == 2) return "OIS OR ";
        if (ch == 3) return "MI 85V ";
        if (ch == 4) return "MI 85H ";
        if (ch == 5) return "MI 37V ";
        if (ch == 6) return "MI 37H ";
        if (ch == 7) return "MI 22V ";
        if (ch == 8) return "MI 19V ";
        if (ch == 9) return "MI 19H ";
        if (ch == 10) return "T1    ";
        if (ch == 11) return "T2    ";
        if (ch == 12) return "J4    ";
        return "";
    }

    private static String getOnlineDesc(char code) {
        if (code == '0') return "Not on-line      ";
        if (code == '1') return "On-line browsable";
        return "";
    }

    private static String getQcDesc(char code) {
        if (code == '0') return "Not available       ";
        if (code == '1') return "Available/Unknown QC";
        if (code == '2') return "Available/Bad QC    ";
        if (code == '3') return "Available/Suspect QC";
        if (code == '4') return "Available/Good QC   ";
        return "";
    }

    public void print() {
        System.out.println("  " + name + "   " + online + "-" + onlineDesc + "  " + qc + "-" + qcDesc);
    }

    public void display(TextArea t) {
        t.append("  " + name + "   " + online + "-" + onlineDesc + "  " + qc + "-" + qcDesc + "\n");
    }
}
