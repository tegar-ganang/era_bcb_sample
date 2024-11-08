package resourcemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class MenuActions {

    public static void createTablesDB() {
        try {
            Statement stmt = G.conn.createStatement();
            stmt.execute("DROP TABLE IF EXISTS main;");
            String query = "CREATE TABLE main (word VARCHAR(50), idL INTEGER, idT INTEGER, name VARCHAR(50), nameNN VARCHAR(50))";
            stmt.execute(query);
            stmt.execute("DROP TABLE IF EXISTS language;");
            query = "CREATE TABLE IF NOT EXISTS language(" + "id INTEGER PRIMARY KEY," + "name VARCHAR(45) NOT NULL)";
            stmt.execute(query);
            stmt.execute("DROP TABLE IF EXISTS type;");
            query = "CREATE TABLE IF NOT EXISTS type (" + "id INTEGER PRIMARY KEY," + "name VARCHAR(45) NOT NULL)";
            stmt.execute(query);
            stmt.execute("CREATE UNIQUE INDEX main_index ON main (word, idL, idT, name, nameNN)");
            stmt.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.toString());
        }
    }

    private static boolean exists(String list[], String name) {
        boolean exists = false;
        int i = 0;
        while ((i < list.length) && !exists) {
            exists = name.equals(list[i]);
            i++;
        }
        return exists;
    }

    public static void importDB(String input, String output) {
        try {
            Class.forName("org.sqlite.JDBC");
            String fileName = output + File.separator + G.databaseName;
            File dataBase = new File(fileName);
            if (!dataBase.exists()) {
                G.conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
                createTablesDB();
            } else G.conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
            long tiempoInicio = System.currentTimeMillis();
            String directoryPath = input + File.separator;
            File myDirectory = new File(directoryPath);
            String[] list = myDirectory.list();
            File fileXML = new File(input + File.separator + G.imagesName);
            if (!fileXML.exists()) {
                JOptionPane.showMessageDialog(null, "No se encuentra el fichero XML", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                SAXBuilder builder = new SAXBuilder(false);
                Document docXML = builder.build(fileXML);
                Element root = docXML.getRootElement();
                List images = root.getChildren("image");
                Iterator j = images.iterator();
                List<Element> globalLanguages = root.getChild("languages").getChildren("language");
                Iterator<Element> langsI = globalLanguages.iterator();
                HashMap<String, Integer> languageIDs = new HashMap<String, Integer>();
                HashMap<String, Integer> typeIDs = new HashMap<String, Integer>();
                Element e;
                int i = 0;
                int contTypes = 0;
                int contImages = 0;
                while (langsI.hasNext()) {
                    e = langsI.next();
                    languageIDs.put(e.getText(), i);
                    PreparedStatement stmt = G.conn.prepareStatement("INSERT OR IGNORE INTO language (id,name) VALUES (?,?)");
                    stmt.setInt(1, i);
                    stmt.setString(2, e.getText());
                    stmt.executeUpdate();
                    stmt.close();
                    i++;
                }
                G.conn.setAutoCommit(false);
                while (j.hasNext()) {
                    Element image = (Element) j.next();
                    String id = image.getAttributeValue("id");
                    List languages = image.getChildren("language");
                    Iterator k = languages.iterator();
                    if (exists(list, id)) {
                        String pathSrc = directoryPath.concat(id);
                        String pathDst = output + File.separator + id.substring(0, 1).toUpperCase() + File.separator;
                        String folder = output + File.separator + id.substring(0, 1).toUpperCase();
                        String pathDstTmp = pathDst.concat(id);
                        String idTmp = id;
                        File testFile = new File(pathDstTmp);
                        int cont = 1;
                        while (testFile.exists()) {
                            idTmp = id.substring(0, id.lastIndexOf('.')) + '_' + cont + id.substring(id.lastIndexOf('.'), id.length());
                            pathDstTmp = pathDst + idTmp;
                            testFile = new File(pathDstTmp);
                            cont++;
                        }
                        pathDst = pathDstTmp;
                        id = idTmp;
                        File newDirectoryFolder = new File(folder);
                        if (!newDirectoryFolder.exists()) {
                            newDirectoryFolder.mkdirs();
                        }
                        try {
                            FileChannel srcChannel = new FileInputStream(pathSrc).getChannel();
                            FileChannel dstChannel = new FileOutputStream(pathDst).getChannel();
                            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                            srcChannel.close();
                            dstChannel.close();
                        } catch (IOException exc) {
                            System.out.println(exc.toString());
                        }
                        while (k.hasNext()) {
                            Element languageElement = (Element) k.next();
                            String language = languageElement.getAttributeValue("id");
                            List words = languageElement.getChildren("word");
                            Iterator l = words.iterator();
                            while (l.hasNext()) {
                                Element wordElement = (Element) l.next();
                                String type = wordElement.getAttributeValue("type");
                                if (!typeIDs.containsKey(type)) {
                                    typeIDs.put(type, contTypes);
                                    PreparedStatement stmt = G.conn.prepareStatement("INSERT OR IGNORE INTO type (id,name) VALUES (?,?)");
                                    stmt.setInt(1, contTypes);
                                    stmt.setString(2, type);
                                    stmt.executeUpdate();
                                    stmt.close();
                                    contTypes++;
                                }
                                PreparedStatement stmt = G.conn.prepareStatement("INSERT OR IGNORE INTO main (word, idL, idT, name, nameNN) VALUES (?,?,?,?,?)");
                                stmt.setString(1, wordElement.getText().toLowerCase());
                                stmt.setInt(2, languageIDs.get(language));
                                stmt.setInt(3, typeIDs.get(type));
                                stmt.setString(4, id);
                                stmt.setString(5, id);
                                stmt.executeUpdate();
                                stmt.close();
                                if (contImages == 5000) {
                                    G.conn.commit();
                                    contImages = 0;
                                } else contImages++;
                            }
                        }
                    } else {
                    }
                }
                G.conn.setAutoCommit(true);
                G.conn.close();
                long totalTiempo = System.currentTimeMillis() - tiempoInicio;
                System.out.println("El tiempo total es :" + totalTiempo / 1000 + " segundos");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void exportDB(String input, String output) {
        try {
            Class.forName("org.sqlite.JDBC");
            String fileName = input + File.separator + G.databaseName;
            File dataBase = new File(fileName);
            if (!dataBase.exists()) {
                JOptionPane.showMessageDialog(null, "No se encuentra el fichero DB", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                G.conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
                HashMap<Integer, String> languageIDs = new HashMap<Integer, String>();
                HashMap<Integer, String> typeIDs = new HashMap<Integer, String>();
                long tiempoInicio = System.currentTimeMillis();
                Element dataBaseXML = new Element("database");
                Element languages = new Element("languages");
                Statement stat = G.conn.createStatement();
                ResultSet rs = stat.executeQuery("select * from language order by id");
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    languageIDs.put(id, name);
                    Element language = new Element("language");
                    language.setText(name);
                    languages.addContent(language);
                }
                dataBaseXML.addContent(languages);
                rs = stat.executeQuery("select * from type order by id");
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    typeIDs.put(id, name);
                }
                rs = stat.executeQuery("select distinct name from main order by name");
                while (rs.next()) {
                    String name = rs.getString("name");
                    Element image = new Element("image");
                    image.setAttribute("id", name);
                    Statement stat2 = G.conn.createStatement();
                    ResultSet rs2 = stat2.executeQuery("select distinct idL from main where name = \"" + name + "\" order by idL");
                    while (rs2.next()) {
                        int idL = rs2.getInt("idL");
                        Element language = new Element("language");
                        language.setAttribute("id", languageIDs.get(idL));
                        Statement stat3 = G.conn.createStatement();
                        ResultSet rs3 = stat3.executeQuery("select * from main where name = \"" + name + "\" and idL = " + idL + " order by idT");
                        while (rs3.next()) {
                            int idT = rs3.getInt("idT");
                            String word = rs3.getString("word");
                            Element wordE = new Element("word");
                            wordE.setAttribute("type", typeIDs.get(idT));
                            wordE.setText(word);
                            language.addContent(wordE);
                            String pathSrc = input + File.separator + name.substring(0, 1).toUpperCase() + File.separator + name;
                            String pathDst = output + File.separator + name;
                            try {
                                FileChannel srcChannel = new FileInputStream(pathSrc).getChannel();
                                FileChannel dstChannel = new FileOutputStream(pathDst).getChannel();
                                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                                srcChannel.close();
                                dstChannel.close();
                            } catch (IOException exc) {
                                System.out.println(exc.getMessage());
                                System.out.println(exc.toString());
                            }
                        }
                        rs3.close();
                        stat3.close();
                        image.addContent(language);
                    }
                    rs2.close();
                    stat2.close();
                    dataBaseXML.addContent(image);
                }
                rs.close();
                stat.close();
                XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
                FileOutputStream f = new FileOutputStream(output + File.separator + G.imagesName);
                out.output(dataBaseXML, f);
                f.flush();
                f.close();
                long totalTiempo = System.currentTimeMillis() - tiempoInicio;
                System.out.println("El tiempo total es :" + totalTiempo / 1000 + " segundos");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addImageDB(String pictogramsPath, String pictogramToAddPath, String language, String type, String word) {
        try {
            Class.forName("org.sqlite.JDBC");
            String fileName = pictogramsPath + File.separator + G.databaseName;
            File dataBase = new File(fileName);
            if (!dataBase.exists()) {
                JOptionPane.showMessageDialog(null, "No se encuentra el fichero DB", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                int idL = 0, idT = 0;
                G.conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
                Statement stat = G.conn.createStatement();
                ResultSet rs = stat.executeQuery("select id from language where name=\"" + language + "\"");
                while (rs.next()) {
                    idL = rs.getInt("id");
                }
                rs.close();
                stat.close();
                stat = G.conn.createStatement();
                rs = stat.executeQuery("select id from type where name=\"" + type + "\"");
                while (rs.next()) {
                    idT = rs.getInt("id");
                }
                rs.close();
                stat.close();
                String id = pictogramToAddPath.substring(pictogramToAddPath.lastIndexOf(File.separator) + 1, pictogramToAddPath.length());
                String idOrig = id;
                String pathSrc = pictogramToAddPath;
                String pathDst = pictogramsPath + File.separator + id.substring(0, 1).toUpperCase() + File.separator;
                String folder = pictogramsPath + File.separator + id.substring(0, 1).toUpperCase();
                String pathDstTmp = pathDst.concat(id);
                String idTmp = id;
                File testFile = new File(pathDstTmp);
                int cont = 1;
                while (testFile.exists()) {
                    idTmp = id.substring(0, id.lastIndexOf('.')) + '_' + cont + id.substring(id.lastIndexOf('.'), id.length());
                    pathDstTmp = pathDst + idTmp;
                    testFile = new File(pathDstTmp);
                    cont++;
                }
                pathDst = pathDstTmp;
                id = idTmp;
                File newDirectoryFolder = new File(folder);
                if (!newDirectoryFolder.exists()) {
                    newDirectoryFolder.mkdirs();
                }
                try {
                    FileChannel srcChannel = new FileInputStream(pathSrc).getChannel();
                    FileChannel dstChannel = new FileOutputStream(pathDst).getChannel();
                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    srcChannel.close();
                    dstChannel.close();
                } catch (IOException exc) {
                    System.out.println(exc.toString());
                }
                PreparedStatement stmt = G.conn.prepareStatement("INSERT OR IGNORE INTO main (word, idL, idT, name, nameNN) VALUES (?,?,?,?,?)");
                stmt.setString(1, word.toLowerCase());
                stmt.setInt(2, idL);
                stmt.setInt(3, idT);
                stmt.setString(4, id);
                stmt.setString(5, idOrig);
                stmt.executeUpdate();
                stmt.close();
                G.conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteImageDB() {
    }
}
