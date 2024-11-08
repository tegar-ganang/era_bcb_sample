package sisi.imp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.activation.MimetypesFileTypeMap;
import org.zkoss.zul.Label;
import sisi.aliquote.Aliquote;
import sisi.aliquote.AliquoteController;
import sisi.articoli.Articoli;
import sisi.articoli.ArticoliController;
import sisi.artimg.Artimg;
import sisi.artimg.ArtimgController;
import sisi.categ.Categ;
import sisi.categ.CategController;
import sisi.listini.Listini;
import sisi.listini.ListiniController;
import sisi.nominativi.Nominativi;
import sisi.nominativi.NominativiController;
import sisi.tipilistini.Tipilistini;
import sisi.tipilistini.TipilistiniController;

public class importaDaWP {

    public importaDaWP(String percorso, boolean lClienti, boolean lFornitori, Label lblTesto, boolean lListini) {
        int iArt = 0, iAli = 0, iCli = 0, iFor = 0, iCat = 0, iLis = 0;
        boolean debug = true, lCategWeb = false;
        String anno = "10";
        String databasePath = "c:\\winpro\\arcgfd\\";
        if (percorso == null || percorso.isEmpty()) {
            return;
        }
        databasePath = percorso;
        lblTesto.setValue("Creazione files dbf di appoggio...");
        String fileName = "art.d" + anno;
        File f = new File(fileName);
        f.delete();
        try {
            copyFile(databasePath + fileName, databasePath + "art.dbf");
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        fileName = "clifor.d" + anno;
        f = new File(fileName);
        f.delete();
        try {
            copyFile(databasePath + fileName, databasePath + "clifor.dbf");
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        fileName = "aliquote.d" + anno;
        f = new File(fileName);
        f.delete();
        try {
            copyFile(databasePath + fileName, databasePath + "aliquote.dbf");
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        fileName = "categ.d" + anno;
        f = new File(fileName);
        f.delete();
        try {
            copyFile(databasePath + fileName, databasePath + "categ.dbf");
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        fileName = "listini.d" + anno;
        f = new File(fileName);
        f.delete();
        try {
            copyFile(databasePath + fileName, databasePath + "listini.dbf");
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        fileName = "list.dat";
        f = new File(fileName);
        f.delete();
        try {
            copyFile(databasePath + fileName, databasePath + "list.dbf");
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        String url = "jdbc:odbc:DRIVER={Microsoft dBase Driver (*.dbf)};DBQ=" + databasePath;
        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        try {
            Connection connection = DriverManager.getConnection(url);
            Statement stmt = connection.createStatement();
            ResultSet rsArt, rsAli, rsCli, rsLis, rsCat;
            lblTesto.setValue("Importazione aliquote...");
            rsAli = stmt.executeQuery("SELECT * FROM aliquote");
            while (rsAli.next()) {
                String codali = rsAli.getString("ALI_COD");
                String desali = rsAli.getString("ALI_DES");
                String aliper = rsAli.getString("ALI_PERC");
                String alitip = rsAli.getString("ALI_TIP");
                String alirep = rsAli.getString("ALI_REPART");
                if (codali != null && !codali.isEmpty()) {
                    Aliquote ali = new AliquoteController().getAliquoteXCodice(codali, false);
                    if (ali.getCodali().isEmpty()) {
                        iAli++;
                        ali = new Aliquote();
                        ali.setAlides(desali);
                        ali.setCodali(codali);
                        ali.setPerc(new BigDecimal(aliper));
                        ali.setTipo((alitip == "3" ? "E" : "I"));
                        ali.setReparto(alirep);
                        new AliquoteController().addAliquote(ali);
                        if (debug) {
                            System.out.println(codali + " -> " + desali);
                        }
                    }
                }
            }
            lblTesto.setValue("Importazione listini...");
            rsLis = stmt.executeQuery("SELECT * FROM list");
            String[] lIvati = new String[9];
            rsLis.next();
            for (int i = 0; i < lIvati.length; i++) {
                String valore = (rsLis.getString(i + 1).equals("0") ? "N" : "S");
                lIvati[i] = valore;
            }
            if (lListini) {
                rsLis = stmt.executeQuery("SELECT * FROM listini");
                while (rsLis.next()) {
                    String codart = rsLis.getString("COD_ART");
                    String codtag = rsLis.getString("COD_TAG");
                    String codlis = rsLis.getString("COD_LIS");
                    String prezzo = rsLis.getString("PREZZO");
                    if (codart != null && !codart.isEmpty() && prezzo != null && !prezzo.isEmpty()) {
                        Listini lis = new ListiniController().getListiniXCodice(codart, codtag, codlis);
                        if (lis.getCodlis() == null || lis.getCodlis().isEmpty()) {
                            iLis++;
                            lis = new Listini();
                            lis.setCodart(codart);
                            lis.setCodlis(codlis);
                            lis.setCodtag(codtag);
                            lis.setLiprezzo(new BigDecimal(prezzo));
                            Tipilistini tl = new TipilistiniController().getListinoXCodice(codlis, false);
                            if (tl.getCodlis().isEmpty()) {
                                tl = new Tipilistini();
                                tl.setCodlis(codlis);
                                tl.setTldes("Listino " + codlis);
                                tl.setTlconiva(lIvati[Integer.parseInt(codlis) - 1]);
                                new TipilistiniController().addTipilistini(tl);
                            }
                            new ListiniController().addListini(lis);
                            if (debug) {
                                System.out.println(codart + " -> " + codlis + " ==> " + prezzo);
                            }
                        }
                    }
                }
            }
            if (!lCategWeb) {
                lblTesto.setValue("Importazione categorie...");
                rsCat = stmt.executeQuery("SELECT * FROM categ");
                while (rsCat.next()) {
                    String codcat = rsCat.getString("CAT_COD");
                    String descat = rsCat.getString("CAT_DES");
                    if (codcat != null && !codcat.isEmpty()) {
                        codcat = "0" + codcat;
                        Categ cat = new CategController().getCategXCodice(codcat, false);
                        if (cat.getCodcat().isEmpty()) {
                            iCat++;
                            cat = new Categ();
                            cat.setCodcat(codcat);
                            cat.setCades(descat);
                            new CategController().addCateg(cat);
                            if (debug) {
                                System.out.println(codcat + " -> " + descat);
                            }
                        }
                    }
                }
            } else {
                lblTesto.setValue("Importazione categorie...");
                rsCat = stmt.executeQuery("SELECT * FROM cat_web");
                while (rsCat.next()) {
                    String codcat = rsCat.getString("ID_CATEG");
                    String codpar = rsCat.getString("ID_PARENT");
                    String descat = rsCat.getString("DESC");
                    if (codcat != null && !codcat.isEmpty()) {
                        codcat = "0" + codcat;
                        Categ cat = new CategController().getCategXCodice(codcat, false);
                        if (cat.getCodcat().isEmpty()) {
                            iCat++;
                            cat = new Categ();
                            cat.setCodcat(codcat);
                            if (codpar != null && !codpar.isEmpty()) {
                                cat.setIdparent("0" + codpar);
                            }
                            cat.setCades(descat);
                            cat.setCaweb("S");
                            new CategController().addCateg(cat);
                            if (debug) {
                                System.out.println(codcat + " -> " + descat);
                            }
                        }
                    }
                }
            }
            lblTesto.setValue("Importazione articoli...");
            rsArt = stmt.executeQuery("SELECT * FROM art");
            while (rsArt.next()) {
                String cod = rsArt.getString("COD");
                String des = rsArt.getString("DES");
                des = (des == null ? "" : des);
                String cat;
                if (!lCategWeb) {
                    cat = rsArt.getString("CAT");
                } else {
                    cat = rsArt.getString("CAT_WEB");
                }
                cat = (cat == null ? "" : cat);
                cat = (cat.isEmpty() ? "" : "0" + cat);
                String mar_cod = rsArt.getString("MAR_COD");
                mar_cod = (mar_cod == null ? "" : mar_cod);
                String cod_ali_iva = rsArt.getString("CODALIIVA");
                cod_ali_iva = (cod_ali_iva == null ? "" : cod_ali_iva);
                String cosu = rsArt.getString("COSU");
                cosu = (cosu == null ? "" : cosu);
                String codba = rsArt.getString("CODBA");
                codba = (codba == null ? "" : codba);
                String img = rsArt.getString("IMMAGINE");
                img = (img == null ? "" : img);
                img = img.replace("img\\", "");
                String posizione = rsArt.getString("POSIZ");
                posizione = (posizione == null ? "" : posizione);
                FileReader fr;
                String testoNota = "";
                try {
                    String cNomeFile = databasePath + "A" + cod.trim() + ".txt";
                    if (new File(cNomeFile).exists()) {
                        fr = new FileReader(cNomeFile);
                        BufferedReader in = new BufferedReader(fr);
                        String line;
                        while ((line = in.readLine()) != null) {
                            testoNota += line;
                        }
                        fr.close();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ;
                Articoli art = new ArticoliController().getArticoliXCodice(cod, false);
                if (art.getArid() == 0) {
                    art = new Articoli();
                    art.setArdes(des);
                    art.setCodart(cod);
                    BigDecimal bdCosto = new BigDecimal(cosu);
                    art.setArcostoult(bdCosto);
                    art.setCodali(cod_ali_iva);
                    art.setCodcat(cat);
                    art.setArpos(posizione);
                    if (!testoNota.isEmpty()) {
                        art.setIpertesto("<p>" + testoNota + "</p>");
                    }
                    if (debug) {
                        System.out.println(cod + " - " + des + " - " + cat + " - " + mar_cod + " - " + cod_ali_iva + " - " + cosu + " - " + codba);
                    }
                    art = new ArticoliController().addArticolo(art);
                    iArt++;
                }
                if (!img.isEmpty()) {
                    Artimg[] artimg = new ArtimgController().getArtimg(art.getCodart());
                    byte[] bytes = null;
                    File fileImage = new File(databasePath + "//img//" + img);
                    String contenttype = "";
                    if (fileImage.exists()) {
                        if (debug) {
                            System.out.println("immagine: " + img);
                        }
                        contenttype = new MimetypesFileTypeMap().getContentType(fileImage);
                        try {
                            int size = (int) fileImage.length();
                            bytes = new byte[size];
                            DataInputStream dis = new DataInputStream(new FileInputStream(fileImage));
                            int read = 0;
                            int numRead = 0;
                            while (read < bytes.length && (numRead = dis.read(bytes, read, bytes.length - read)) >= 0) {
                                read = read + numRead;
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (artimg != null && artimg.length >= 1) {
                        artimg[0].setPercorso(img);
                        artimg[0].setImmagine(bytes);
                        artimg[0].setContenttype(contenttype);
                        new ArtimgController().updateArtimg(artimg[0]);
                    } else {
                        Artimg immagine = new Artimg();
                        immagine.setCodart(cod);
                        immagine.setPercorso(img);
                        immagine.setImmagine(bytes);
                        immagine.setContenttype(contenttype);
                        new ArtimgController().addArtimg(immagine);
                    }
                }
            }
            for (int i = 0; i < 2; i++) {
                boolean lOk = false;
                if (i == 0 && lClienti) {
                    lOk = true;
                    lblTesto.setValue("Importazione Clienti...");
                }
                if (i == 1 && lFornitori) {
                    lOk = true;
                    lblTesto.setValue("Importazione Fornitori...");
                }
                if (lOk) {
                    String clifo = (i == 0 ? "C" : "F");
                    rsCli = stmt.executeQuery("SELECT * FROM clifor WHERE clifo='" + clifo + "'");
                    while (rsCli.next()) {
                        String nom = rsCli.getString("NOM");
                        nom = (nom == null ? "" : nom);
                        String con = rsCli.getString("TIT");
                        con = (con == null ? "" : con);
                        String ind = rsCli.getString("IND");
                        ind = (ind == null ? "" : ind);
                        String cap = rsCli.getString("CAP");
                        cap = (cap == null ? "" : cap);
                        String cit = rsCli.getString("CIT");
                        cit = (cit == null ? "" : cit);
                        String pv = rsCli.getString("PV");
                        pv = (pv == null ? "" : pv);
                        String tel = rsCli.getString("TEL1");
                        tel = (tel == null ? "" : tel);
                        String fax = rsCli.getString("FAX");
                        fax = (fax == null ? "" : fax);
                        String cel = rsCli.getString("CEL");
                        cel = (cel == null ? "" : cel);
                        String email = rsCli.getString("EMAIL");
                        email = (email == null ? "" : email);
                        String sito = rsCli.getString("SITOWEB");
                        sito = (sito == null ? "" : sito);
                        String pi = rsCli.getString("PI");
                        if (pi != null) {
                            pi = pi.replace("IT", "");
                            pi = pi.replace("it", "");
                            pi = pi.substring(0);
                        } else {
                            pi = "";
                        }
                        String cf = rsCli.getString("CFI");
                        cf = (cf == null ? "" : cf);
                        String iban = rsCli.getString("IBAN");
                        iban = (iban == null ? "" : iban);
                        Nominativi[] nominativi = new NominativiController().getNominativi(nom.trim(), (i == 0));
                        if (nominativi.length == 0) {
                            Nominativi newnom = new Nominativi();
                            newnom.setIban(iban);
                            newnom.setNomcap(cap);
                            newnom.setNomcel(cel);
                            if (i == 0) {
                                newnom.setNomcli("S");
                                newnom.setNomfor("N");
                                iCli++;
                            } else {
                                newnom.setNomcli("N");
                                newnom.setNomfor("S");
                                iFor++;
                            }
                            newnom.setNomcf(cf);
                            newnom.setNomcitta(cit);
                            newnom.setNomemail(email);
                            newnom.setNomfax(fax);
                            newnom.setNomind(ind);
                            newnom.setNomnom(nom);
                            newnom.setNompi(pi);
                            newnom.setNompv(pv);
                            newnom.setNomsitoweb(sito);
                            newnom.setNomtel(tel);
                            newnom.setNomcon(con);
                            newnom.setCodnom(new NominativiController().lastCodicePlus1());
                            new NominativiController().addNominativi(newnom);
                            if (debug) {
                                System.out.println(nom);
                            }
                        }
                    }
                }
            }
            connection.close();
            lblTesto.setValue("Fine Procedura...");
            new sisi.General().MsgBox("Inseriti: " + iArt + " articoli - " + iAli + " Aliquote Iva - " + iCat + " Categorie - " + iLis + " Listini - " + iCli + " Clienti - " + iFor + " Fornitori.", "Importazione dati");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void copyFile(String sInput, String sOutput) throws IOException {
        File inputFile = new File(sInput);
        File outputFile = new File(sOutput);
        FileReader in = new FileReader(inputFile);
        FileWriter out = new FileWriter(outputFile);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }
}
