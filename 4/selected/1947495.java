package com.homeautomate.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Fonctions {

    public static String getDateFormat(Date date, String format_Ex_YYYY_MM_DD) {
        if (format_Ex_YYYY_MM_DD == null) {
            format_Ex_YYYY_MM_DD = "yyyyMMddHHmmss";
        }
        DateFormat dateFormat = new SimpleDateFormat(format_Ex_YYYY_MM_DD);
        return dateFormat.format(date);
    }

    public static String getFieldFromString(String chaine, String delimiteur, Integer Field) {
        String[] liste = chaine.split(delimiteur);
        String retour = "";
        try {
            retour = liste[Field];
        } catch (Exception e) {
        }
        return retour;
    }

    public static Date getDateFormat(String date, String format_Ex_YYYY_MM_DD) {
        Date dt = null;
        try {
            SimpleDateFormat df = new SimpleDateFormat(format_Ex_YYYY_MM_DD);
            dt = df.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dt;
    }

    public static void copieFichier(File fichier1, File fichier2) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(fichier1).getChannel();
            out = new FileOutputStream(fichier2).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
