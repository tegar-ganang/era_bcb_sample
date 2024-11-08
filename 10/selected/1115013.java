package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseCreator {

    private static Connection conn;

    private String userName = "recipe";

    private String password = "recipe";

    private String databaseDriver = "com.mysql.jdbc.Driver";

    private String url = "jdbc:mysql://localhost";

    private String databaseName = "recipe";

    private static final String[] categories = { "Alkuruoat", "Kasvisruoat", "Keitot", "Kastikkeet", "Kalaruoat", "Linnut", "Liharuoat", "Leivonnaiset", "J�lkiruoat" };

    private static final String[] measures = { "ml", "cl", "dl", "l", "g", "kg", "kpl", "tl", "rkl", "tlk", "ruukku", "nippu", "hyppysellinen" };

    private static final String[] resepti1 = { "", "", "" };

    public DatabaseCreator() {
        try {
            Class.forName(databaseDriver);
            Properties props = new Properties();
            props.setProperty("character-set", "latin1");
            props.setProperty("username", userName);
            props.setProperty("password", password);
            conn = DriverManager.getConnection(url + "/" + databaseName, userName, password);
            conn.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            System.out.println("Luokkaa ei l�ytynyt, " + e.getMessage());
        } catch (SQLException se) {
            System.out.println("Virhe yhteytt� luotaessa, " + se.getMessage());
        }
    }

    public static void createTables() throws Exception {
        PreparedStatement dropRecipesTable = conn.prepareStatement("DROP TABLE recipes");
        PreparedStatement dropIngsTable = conn.prepareStatement("DROP TABLE ingredients");
        PreparedStatement dropMeasureTable = conn.prepareStatement("DROP TABLE measure");
        PreparedStatement dropCategTable = conn.prepareStatement("DROP TABLE categories");
        PreparedStatement dropTimeTable = conn.prepareStatement("DROP TABLE timetable");
        PreparedStatement createRecipesTable = conn.prepareStatement("CREATE TABLE recipes (recipe_id INTEGER NOT NULL AUTO_INCREMENT,name VARCHAR(50) NOT NULL,instructions VARCHAR(300),category_id INTEGER,PRIMARY KEY (recipe_id))");
        PreparedStatement createIngsTable = conn.prepareStatement("CREATE TABLE ingredients (recipe_id INTEGER NOT NULL,name VARCHAR(50),amount decimal(4,2),measure_id INTEGER,shop_flag INTEGER NOT NULL)");
        PreparedStatement createMeasureTable = conn.prepareStatement("CREATE TABLE measure (measure_id INTEGER NOT NULL AUTO_INCREMENT,name VARCHAR(30) NOT NULL,PRIMARY KEY (measure_id))");
        PreparedStatement createCategTable = conn.prepareStatement("CREATE TABLE categories (category_id INTEGER NOT NULL AUTO_INCREMENT,name VARCHAR(30) NOT NULL,description VARCHAR(300),PRIMARY KEY (category_id))");
        PreparedStatement createTimeTable = conn.prepareStatement("CREATE TABLE timetable (recipe_id INTEGER NOT NULL,time timestamp NOT NULL,meal integer NOT NULL)");
        if (conn != null) {
            int success = -1;
            try {
                success = dropRecipesTable.executeUpdate();
            } catch (SQLException se) {
            }
            success = createRecipesTable.executeUpdate();
            if (success != -1) System.out.println("\trecipes-taulu luotu"); else throw new Exception("recipes-taulun luonti ep�onnistui, success:" + success);
            try {
                success = dropIngsTable.executeUpdate();
            } catch (SQLException se) {
            }
            success = createIngsTable.executeUpdate();
            if (success != -1) System.out.println("\tingredients-taulu luotu"); else throw new Exception("ingredients-taulun luonti ep�onnistui, success:" + success);
            try {
                success = dropMeasureTable.executeUpdate();
            } catch (SQLException se) {
            }
            success = createMeasureTable.executeUpdate();
            if (success != -1) System.out.println("\tmeasure-taulu luotu"); else throw new Exception("measure-taulun luonti ep�onnistui, success:" + success);
            try {
                success = dropCategTable.executeUpdate();
            } catch (SQLException se) {
            }
            success = createCategTable.executeUpdate();
            if (success != -1) System.out.println("\tcategories-taulu luotu"); else throw new Exception("categories-taulun luonti ep�onnistui, success:" + success);
            try {
                success = dropTimeTable.executeUpdate();
            } catch (SQLException se) {
            }
            success = createTimeTable.executeUpdate();
            if (success != -1) System.out.println("\ttimetable-taulu luotu"); else throw new Exception("timetable-taulun luonti ep�onnistui, success:" + success);
        }
    }

    public static void insertData() throws Exception {
        PreparedStatement insertMeasures = conn.prepareStatement("INSERT INTO measure (name) VALUES (?)");
        PreparedStatement insertCategories = conn.prepareStatement("INSERT INTO categories (name) VALUES (?)");
        if (conn != null) {
            int success = -1;
            for (int i = 0; i < categories.length; ++i) {
                insertCategories.setString(1, categories[i]);
                success = insertCategories.executeUpdate();
                if (success == -1) throw new Exception("Kategorioiden lis�ys ep�onnistui");
            }
            System.out.println("\tKategoriat lis�tty kantaan");
            for (int i = 0; i < measures.length; ++i) {
                insertMeasures.setString(1, measures[i]);
                success = insertMeasures.executeUpdate();
                if (success == -1) throw new Exception("Mittayksik�iden lis�ys ep�onnistui");
            }
            System.out.println("\tMittayksik�t lis�tty kantaan");
            System.out.println("Lis�t��n resepti");
            String[][] ainesosat = { { "lihalienta", "1", "4", "0" }, { "koskenlaskija sulatejuustoa", "250", "5", "1" }, { "sinihomejuustoa", "50", "5", "1" }, { "yrttimaustesekoitusta", "2", "8", "1" }, { "jauhettua muskottipahkinaa", "0,5", "8", "1" }, { "valkosipulin kyntta murskattuna", "2-3", "7", "1" }, { "puolikarkeita vehnajauhoja", "2-3", "9", "1" }, { "kahvikermaa", "2", "3", "1" }, { "basilikaa", "1", "11", "1" } };
            addRecipe("Juustokakku", "1. Mittaa lihaliemi ja juustot kattilaan. Kuumenna kiehuvaksi ja keit� kunnes juustot ovat sulaneet liemeen.\n" + "2. Lis�� yrttimauste, muskottip�hkin�, murskatut valkosipulin kynnet ja kermaan sekoitetut vehn�jauhot. Keit� 5 minuuttia.\n" + "3. Silppua tuore basilika keiton joukkoon. Tarjoa paahtoleiv�n tai patongin kanssa.", 9, ainesosat);
            System.out.println("Lis�t��n toinen resepti");
            String[][] ainesosat2 = { { "puuta", "1", "4", "0" }, { "heinaa", "250", "5", "1" }, { "ja muutama vesiper�", "250", "5", "1" } };
            addRecipe("Puppua", "Kukkuupulu", 2, ainesosat2);
            System.out.println("Lis�t��n toinen resepti");
            String[][] ainesosat3 = { { "Rautaa", "10", "6", "0" } };
            addRecipe("Tosi-sissin alkupala", "Sy� rautaa ja paskanna kettinki�", 1, ainesosat3);
        }
    }

    public static void addRecipe(String name, String instructions, int categoryId, String[][] ainekset) throws Exception {
        PreparedStatement pst1 = null;
        PreparedStatement pst2 = null;
        ResultSet rs = null;
        int retVal = -1;
        try {
            pst1 = conn.prepareStatement("INSERT INTO recipes (name, instructions, category_id) VALUES (?, ?, ?)");
            pst1.setString(1, name);
            pst1.setString(2, instructions);
            pst1.setInt(3, categoryId);
            if (pst1.executeUpdate() > 0) {
                pst2 = conn.prepareStatement("SELECT recipe_id FROM recipes WHERE name = ? AND instructions = ? AND category_id = ?");
                pst2.setString(1, name);
                pst2.setString(2, instructions);
                pst2.setInt(3, categoryId);
                rs = pst2.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("Lis�t��n ainesosat");
                    String[] aines;
                    for (int i = 0; i < ainekset.length; ++i) {
                        aines = ainekset[i];
                        addIngredient(id, aines[0], aines[1], Integer.parseInt(aines[2]), Integer.parseInt(aines[3]));
                    }
                    retVal = id;
                } else {
                    retVal = -1;
                }
            } else {
                retVal = -1;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw new Exception("Reseptin lis�ys ep�onnistui. Poikkeus: " + e.getMessage());
        }
    }

    private static void addIngredient(int recipeId, String name, String amount, int measureId, int shopFlag) throws Exception {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO ingredients (recipe_id, name, amount, measure_id, shop_flag) VALUES (?,?,?,?,?)");
            pst.setInt(1, recipeId);
            pst.setString(2, name);
            pst.setString(3, amount);
            pst.setInt(4, measureId);
            pst.setInt(5, shopFlag);
            pst.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw new Exception("Ainesosan lis�ys ep�onnistui. Poikkeus: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        DatabaseCreator databaseCreator = new DatabaseCreator();
        if (conn == null) {
            System.out.println("Kantayhteyden luonti ep�onnistui");
            System.exit(1);
        } else {
            System.out.println("Kantayhteyden luonti onnistui");
            try {
                System.out.println("\nYritet��n luoda tauluja");
                createTables();
                System.out.println("\nLaitetaan datat kantaan");
                insertData();
                System.out.println("\nHomma bueno, paetaan paikalta...");
            } catch (Exception e) {
                System.out.println(e.toString());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
