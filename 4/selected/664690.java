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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import org.zkoss.zul.Label;
import sisi.MyEmf;
import sisi.aliquote.Aliquote;
import sisi.aliquote.AliquoteController;
import sisi.articoli.Articoli;
import sisi.articoli.ArticoliController;
import sisi.articoli.Correlati;
import sisi.articoli.CorrelatiController;
import sisi.artimg.Artimg;
import sisi.artimg.ArtimgController;
import sisi.categ.Categ;
import sisi.categ.CategController;
import sisi.listini.Listini;
import sisi.listini.ListiniController;
import sisi.nominativi.Nominativi;
import sisi.nominativi.NominativiController;
import sisi.taglie.Tagliacolore;
import sisi.tipilistini.Tipilistini;
import sisi.tipilistini.TipilistiniController;

public class ImportaYouCommerce {

    public ImportaYouCommerce() {
        System.out.println("INIZIO");
        Connection conn = null;
        String url = "jdbc:mysql://192.168.1.71:3306/";
        String dbName = "Sql262328_1";
        String driver = "com.mysql.jdbc.Driver";
        String userName = "prova";
        String password = "prova";
        boolean debug = true, impImg = true;
        String pathImg = "X://premiosicuro.it/";
        EntityManagerFactory emf = new MyEmf().getEmf();
        EntityManager em = emf.createEntityManager();
        EntityTransaction entr = em.getTransaction();
        entr.begin();
        Query query2 = em.createNativeQuery("DELETE FROM Tagliacolore");
        query2.executeUpdate();
        entr.commit();
        new sisi.General().controllaSequence();
        try {
            Class.forName(driver).newInstance();
            conn = DriverManager.getConnection(url + dbName, userName, password);
            try {
                Statement st = conn.createStatement();
                ResultSet res = st.executeQuery("SELECT * FROM  tarticoli");
                System.out.println("<-- TARTICOLI -->");
                while (res.next()) {
                    String s = res.getString("art_nome");
                    if (debug) {
                        System.out.println("art_nome: " + s);
                    }
                    int artcod = res.getInt("art_id");
                    String artdes = res.getString("art_nome");
                    artdes = (artdes == null ? "" : artdes);
                    artdes = (artdes.trim().length() > 60 ? artdes.substring(0, 59) : artdes);
                    String artipertesto = res.getString("art_predescrizione");
                    artipertesto = (artipertesto == null ? "" : artipertesto);
                    String pre_descrizione = res.getString("art_predescrizione");
                    String meta_descrizione = res.getString("art_description");
                    String img = res.getString("art_imgpreview");
                    img = (img == null ? "" : img);
                    String cod_ali_iva = "00001";
                    Articoli art = new ArticoliController().getArticoliXCodice("" + artcod, false);
                    if (art.getArid() == 0) {
                        art = new Articoli();
                    }
                    art.setArdes(artdes);
                    art.setCodart("" + artcod);
                    art.setCodali(cod_ali_iva);
                    art.setIpertesto("<p>" + artipertesto + "</p>");
                    art.setPre_descrizione(pre_descrizione);
                    art.setMeta_descrizione(meta_descrizione);
                    if (art.getArid() == 0) {
                        art = new ArticoliController().addArticolo(art);
                        if (debug) {
                            System.out.println("nuovo articolo: " + artcod + " " + artdes);
                        }
                    } else {
                        art = new ArticoliController().updateArticolo(art);
                        if (debug) {
                            System.out.println("update articolo: " + artcod + " " + artdes);
                        }
                    }
                    String art_correlati = res.getString("art_correlati");
                    art_correlati = (art_correlati == null ? "" : art_correlati.trim());
                    if (art_correlati.length() > 0 && !art_correlati.equalsIgnoreCase("0")) {
                        for (int i = 0; i < art_correlati.length(); i++) {
                            String correlato = art_correlati.substring(i, art_correlati.length());
                            String valore;
                            if (correlato.contains(",")) {
                                valore = correlato.substring(0, correlato.indexOf(","));
                                i = i + valore.length();
                            } else {
                                valore = correlato;
                                i = i + valore.length();
                            }
                            Correlati arCorrelato = new Correlati();
                            arCorrelato.setCodcorrelato(valore);
                            arCorrelato.setCodprincipale("" + artcod);
                            new CorrelatiController().addCorrelati(arCorrelato);
                        }
                    }
                    if (impImg) {
                        if (!img.isEmpty()) {
                            Artimg[] artimg = new ArtimgController().getArtimg(art.getCodart());
                            byte[] bytes = null;
                            File fileImage = new File(pathImg + img);
                            String contenttype = "";
                            if (fileImage.exists()) {
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
                            String nomeImmagine = img;
                            nomeImmagine = img.replace("/public//", "");
                            nomeImmagine = sisi.General.ConvertoNomeFile(nomeImmagine);
                            if (artimg != null && artimg.length >= 1) {
                                artimg[0].setPercorso(nomeImmagine);
                                artimg[0].setImmagine(bytes);
                                artimg[0].setContenttype(contenttype);
                                new ArtimgController().updateArtimg(artimg[0]);
                                if (debug) {
                                    System.out.println("update immagine per l'articolo: " + artcod);
                                }
                            } else {
                                Artimg immagine = new Artimg();
                                immagine.setCodart("" + artcod);
                                immagine.setPercorso(nomeImmagine);
                                immagine.setImmagine(bytes);
                                immagine.setContenttype(contenttype);
                                new ArtimgController().addArtimg(immagine);
                                if (debug) {
                                    System.out.println("nuova immagine per l'articolo: " + artcod);
                                }
                            }
                        }
                    }
                }
                System.out.println("CATEGORIE");
                st = conn.createStatement();
                ResultSet rsCat = st.executeQuery("SELECT * FROM  tcategorie");
                while (rsCat.next()) {
                    String codcat = rsCat.getString("CAT_ID");
                    String codpar = rsCat.getString("CAT_IDPADRE");
                    String descat = rsCat.getString("CAT_NOME");
                    codpar = (codpar == null || codpar.equals("0") ? null : codpar);
                    if (codcat != null && !codcat.isEmpty()) {
                        codcat = sisi.General.paddingString(codcat, 5, '0', true);
                        Categ cat = new CategController().getCategXCodice(codcat, false);
                        if (cat.getCodcat().isEmpty()) {
                            cat = new Categ();
                            cat.setCodcat(codcat);
                            if (codpar != null && !codpar.isEmpty()) {
                                codpar = sisi.General.paddingString(codpar, 5, '0', true);
                                cat.setIdparent(codpar);
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
                System.out.println("COLLEGAMENTO CATEGORIE --> ARTICOLI");
                st = conn.createStatement();
                ResultSet rsCat2 = st.executeQuery("SELECT * FROM tart_cat");
                while (rsCat2.next()) {
                    String codart = rsCat2.getString("IDART");
                    String codcat = rsCat2.getString("IDCAT");
                    String visible = rsCat2.getString("VISIBILE");
                    codcat = sisi.General.paddingString(codcat, 5, '0', true);
                    Articoli art = new ArticoliController().getArticoliXCodice(codart, false);
                    if (debug) {
                        System.out.println("COLLEGAMENTO CATEGORIE --> ARTICOLO: " + codart + " IDCAT: " + codcat);
                    }
                    if (art.getArid() != 0) {
                        art.setCodcat(codcat);
                        if (visible.equals("0")) {
                            art.setArweb("N");
                        } else {
                            art.setArweb("S");
                        }
                    }
                    art = new ArticoliController().updateArticolo(art);
                }
                System.out.println("VARIANTI (TAGLIA E COLORI)");
                Articoli[] tutti_gli_articoli = new ArticoliController().getArticoli();
                for (Articoli articolo : tutti_gli_articoli) {
                    String cQuery = "SELECT *, 0 AS tab FROM tvariante0 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 1 as tab FROM tvariante1 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 2 as tab FROM tvariante2 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 3 as tab FROM tvariante3 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 4 as tab FROM tvariante4 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 5 as tab FROM tvariante5 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 6 as tab FROM tvariante6 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 7 as tab FROM tvariante7 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 8 as tab FROM tvariante8 where var_idart=" + articolo.getCodart();
                    cQuery += " UNION ";
                    cQuery += " SELECT *, 9 as tab FROM tvariante9 where var_idart=" + articolo.getCodart();
                    st = conn.createStatement();
                    ResultSet rsVarianti = st.executeQuery(cQuery);
                    String tab1 = "";
                    String tab2 = "";
                    int i = 1;
                    while (rsVarianti.next()) {
                        if (i == 1) {
                            tab1 = rsVarianti.getString("tab");
                        } else {
                            if (!tab1.equalsIgnoreCase(rsVarianti.getString("tab"))) {
                                tab2 = rsVarianti.getString("tab");
                                break;
                            }
                        }
                        i++;
                    }
                    if (debug && articolo.getCodart().trim().equalsIgnoreCase("168")) {
                        System.out.println("COLLEGAMENTO CATEGORIE --> ARTICOLO: " + articolo.getCodart() + " tab1: " + tab1 + " tab2: " + tab2);
                    }
                    if (!tab1.isEmpty() && !tab2.isEmpty()) {
                        st = conn.createStatement();
                        String cQuery1 = "SELECT tvariante" + tab1 + ".var_idart, tvariante_opzione.nome_opzione, tvariante.nome_variante FROM tvariante" + tab1 + " LEFT JOIN tvariante_opzione ON tvariante" + tab1 + ".var_opzione_id=tvariante_opzione.id LEFT JOIN tvariante ON tvariante.id = tvariante_opzione.variante_id WHERE tvariante" + tab1 + ".var_idart=" + articolo.getCodart() + " ORDER by tvariante" + tab1 + ".var_id";
                        rsVarianti = st.executeQuery(cQuery1);
                        while (rsVarianti.next()) {
                            if (rsVarianti.getString("VAR_IDART") != null) {
                                String codart = rsVarianti.getString("VAR_IDART");
                                String variante = rsVarianti.getString("NOME_OPZIONE");
                                String nomeVariante = rsVarianti.getString("NOME_VARIANTE");
                                String descTaglia = rsVarianti.getString("NOME_VARIANTE");
                                Articoli art = new ArticoliController().getArticoliXCodice(codart, false);
                                if (debug) {
                                }
                                if (art.getArid() != 0) {
                                    Statement st2 = conn.createStatement();
                                    String cQuery2 = "SELECT tvariante" + tab2 + ".var_idart, tvariante_opzione.nome_opzione, tvariante.nome_variante FROM tvariante" + tab2 + " LEFT JOIN tvariante_opzione ON tvariante" + tab2 + ".var_opzione_id=tvariante_opzione.id LEFT JOIN tvariante ON tvariante.id = tvariante_opzione.variante_id WHERE tvariante" + tab2 + ".var_idart = " + codart + " order by tvariante" + tab2 + ".var_id";
                                    ResultSet rsVarianti1 = st2.executeQuery(cQuery2);
                                    while (rsVarianti1.next()) {
                                        if (rsVarianti1.getString("NOME_OPZIONE") != null) {
                                            nomeVariante = rsVarianti1.getString("NOME_OPZIONE");
                                            String descColore = rsVarianti1.getString("NOME_VARIANTE");
                                            String nomeVariante2 = (nomeVariante.length() > 50 ? nomeVariante.substring(0, 49) : nomeVariante);
                                            String variante2 = (variante.length() > 50 ? variante.substring(0, 49) : variante);
                                            Tagliacolore tagliaColore = new sisi.taglie.Tagliacolore();
                                            tagliaColore.setArticoliTagCol(art);
                                            tagliaColore.setCodart(art.getCodart());
                                            tagliaColore.setCodtag(nomeVariante2);
                                            tagliaColore.setColore(variante2);
                                            tagliaColore.setTag_descrizione(descTaglia);
                                            tagliaColore.setCol_descrizione(descColore);
                                            new sisi.taglie.TagliacoloreController().addTagliacolore(tagliaColore);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                System.out.println("LISTINI");
                st = conn.createStatement();
                for (int i = 0; i < 2; i++) {
                    String tabella = "tlistino" + i;
                    String codlis = "LI" + (i + 1);
                    ResultSet rsListini = st.executeQuery("SELECT " + tabella + ".list_idart, " + tabella + ".list_prezzobase FROM " + tabella);
                    while (rsListini.next()) {
                        String codart = rsListini.getString("LIST_IDART");
                        double listino = rsListini.getDouble("LIST_PREZZOBASE");
                        Articoli art = new ArticoliController().getArticoliXCodice(codart, false);
                        if (art.getCodart() != null && !art.getCodart().isEmpty()) {
                            Listini lisart = new sisi.listini.ListiniController().getListiniXCodice(codart, null, codlis);
                            if (debug) {
                                System.out.println("LISTINI: " + codart + " listino: " + listino);
                            }
                            if (lisart.getLiid() == null) {
                                lisart.setCodart(codart);
                                lisart.setLiprezzo(BigDecimal.valueOf(listino));
                                lisart.setCodlis(codlis);
                                lisart.setArticoliLis(art);
                                new sisi.listini.ListiniController().addListini(lisart);
                            } else {
                                lisart.setLiprezzo(BigDecimal.valueOf(listino));
                                new sisi.listini.ListiniController().updateListini(lisart);
                            }
                        }
                    }
                }
                conn.close();
            } catch (SQLException s) {
                s.printStackTrace();
                System.out.println("ERRORE QUERY SQL.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("*** ERRORE COLLEGAMENTO ***");
        }
        System.out.println("FINE");
    }

    void importaDati() {
        String percorso = "";
        boolean lClienti = false, lFornitori = false, lListini = false;
        Label lblTesto;
        int iArt = 0, iAli = 0, iCli = 0, iFor = 0, iCat = 0, iLis = 0;
        boolean debug = true, lCategWeb = false;
        String anno = "10";
        String databasePath = "c:\\winpro\\arcgfd\\";
        if (percorso == null || percorso.isEmpty()) {
            return;
        }
        databasePath = percorso;
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
                }
                if (i == 1 && lFornitori) {
                    lOk = true;
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
