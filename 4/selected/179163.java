package dbaccess.dmsp;

import java.awt.*;
import dbaccess.util.*;

/**
public class Channel {

    String name;

    char online;

    String onlineDesc;

    char qc;

    String qcDesc;

    /**
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