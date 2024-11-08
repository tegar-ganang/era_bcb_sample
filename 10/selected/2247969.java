package recipe;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import beans.Category;
import beans.Ingredient;
import beans.IngredientContainer;
import beans.Recipe;
import beans.RecipeContainer;
import beans.Schedule;

/**
 * @author ville
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DBHandler {

    private Connection conn;

    private String userName = "recipe";

    private String password = "recipe";

    private String databaseDriver = "com.mysql.jdbc.Driver";

    private String url = "jdbc:mysql://localhost";

    private String databaseName = "recipe";

    /**
	 * Constructor
	 * @param
	 * Checks whether the program can find the database
	 * - if not, opens the "Open File"-dialog 
	 * */
    public DBHandler() {
        conn = null;
        System.out.println("K�ytet��n MySQL kantaa");
    }

    /**
	 * @return Connection
	 * @throws Exception 
	 */
    private synchronized Connection getConnection() throws Exception {
        conn = null;
        Class.forName(databaseDriver);
        if (conn == null || conn.isClosed()) conn = DriverManager.getConnection(url + "/" + databaseName, userName, password);
        try {
            conn.setAutoCommit(false);
        } catch (Exception e) {
        }
        return conn;
    }

    /**
	 * Gets the ingredients for a specified recipe
	 * @param int recipeId
	 * @return IngredientContainer
	 */
    private IngredientContainer getIngredientsById(int recipeId) {
        String sql = "SELECT name, amount, measure_id, shop_flag FROM ingredients WHERE recipe_id = ?";
        IngredientContainer ic = null;
        PreparedStatement stat = null;
        ResultSet rs = null;
        try {
            ic = new IngredientContainer();
            conn = this.getConnection();
            stat = conn.prepareStatement(sql);
            stat.setInt(1, recipeId);
            rs = stat.executeQuery();
            while (rs != null && rs.next()) {
                System.out.println("ingredients -- id: " + recipeId + " name: " + rs.getString(1) + " amount: " + Integer.toString(rs.getInt(2)) + " type: " + rs.getInt(3) + " shopFlag: " + rs.getInt(4));
                Ingredient ib = new Ingredient(recipeId, rs.getString(1), rs.getDouble(2), rs.getInt(3), rs.getInt(4));
                ic.addToContainer(ib);
            }
        } catch (Exception e) {
            System.out.println("Database error, when executing \"getIngredientsById\" statement.");
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (stat != null) stat.close();
            } catch (SQLException sqle) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return ic;
    }

    /**
	 * Fetches all the recipes from the database
	 * @return RecipeContainer
	 */
    public RecipeContainer getRecipes() {
        String sql = "SELECT recipe_id, name, instructions, category_id FROM recipes ORDER BY name";
        RecipeContainer rc = null;
        PreparedStatement stat = null;
        ResultSet rs = null;
        try {
            rc = new RecipeContainer();
            conn = getConnection();
            stat = conn.prepareStatement(sql);
            rs = stat.executeQuery(sql);
            Recipe ib = null;
            int id = 0;
            while (rs != null && rs.next()) {
                System.out.println("recipes -- id: " + rs.getInt(1) + " name: " + rs.getString(2) + " instructions: " + rs.getString(3));
                id = rs.getInt(1);
                ib = new Recipe(id, rs.getString(2), getIngredientsById(id), rs.getString(3), rs.getInt(4));
                rc.getRecipes().add(ib);
            }
        } catch (Exception e) {
            System.out.println("Can't get recipes, error message was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (stat != null) stat.close();
            } catch (SQLException sqle) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return rc;
    }

    /**
	 * Adds a new recipe to the database
	 * @param Recipe recipe
	 * @return int recipeId
	 * @throws Exception
	 */
    public int addRecipe(Recipe recipe) throws Exception {
        PreparedStatement pst1 = null;
        PreparedStatement pst2 = null;
        ResultSet rs = null;
        int retVal = -1;
        try {
            conn = getConnection();
            pst1 = conn.prepareStatement("INSERT INTO recipes (name, instructions, category_id) VALUES (?, ?, ?)");
            pst1.setString(1, recipe.getName());
            pst1.setString(2, recipe.getInstructions());
            pst1.setInt(3, recipe.getCategoryId());
            if (pst1.executeUpdate() > 0) {
                pst2 = conn.prepareStatement("SELECT recipe_id FROM recipes WHERE name = ? AND instructions = ? AND category_id = ?");
                pst2.setString(1, recipe.getName());
                pst2.setString(2, recipe.getInstructions());
                pst2.setInt(3, recipe.getCategoryId());
                rs = pst2.executeQuery();
                conn.commit();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    addIngredients(recipe, id);
                    MainFrame.recipePanel.update();
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
            MainFrame.appendStatusText("Can't add recipe, the exception was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (pst1 != null) pst1.close();
                pst1 = null;
                if (pst2 != null) pst2.close();
                pst2 = null;
            } catch (SQLException sqle) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return retVal;
    }

    /**
	 * Adds ingredients for the specific recipe.
	 * Used by addRecipe-function
	 * @param Recipe recipe is bean for the recipe.
	 * @param int id is id of the recipe
	 * @throws SQLException
	 */
    private void addIngredients(Recipe recipe, int id) throws Exception {
        PreparedStatement pst = null;
        try {
            conn = getConnection();
            pst = conn.prepareStatement("INSERT INTO ingredients (recipe_id, name, amount, measure_id, shop_flag) VALUES (?,?,?,?,?)");
            IngredientContainer ings = recipe.getIngredients();
            Ingredient ingBean = null;
            Iterator it;
            for (it = ings.getIngredients().iterator(); it.hasNext(); ) {
                ingBean = (Ingredient) it.next();
                pst.setInt(1, id);
                pst.setString(2, ingBean.getName());
                pst.setDouble(3, ingBean.getAmount());
                pst.setInt(4, ingBean.getType());
                pst.setInt(5, ingBean.getShopFlag());
                pst.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            MainFrame.appendStatusText("Can't add ingredient, the exception was " + e.getMessage());
        } finally {
            try {
                if (pst != null) pst.close();
                pst = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
    }

    /**
	 * Modifies a recipe in the database
	 * @param int oldRecipeId
     * @param Recipe newRecipe
	 * @return int recipe_id
	 * @throws Exception
	 */
    public int editRecipe(int oldRecipeId, Recipe newRecipe) throws Exception {
        PreparedStatement pst1 = null;
        PreparedStatement pst2 = null;
        ResultSet rs = null;
        int retVal = -1;
        try {
            conn = getConnection();
            pst1 = conn.prepareStatement("UPDATE recipes SET name = ?, instructions = ?, category_id =? WHERE recipe_id = ?");
            pst1.setString(1, newRecipe.getName());
            pst1.setString(2, newRecipe.getInstructions());
            pst1.setInt(3, newRecipe.getCategoryId());
            pst1.setInt(4, oldRecipeId);
            int rsVal = pst1.executeUpdate();
            conn.commit();
            if (rsVal > 0) {
                updateIngredients(newRecipe, oldRecipeId);
                MainFrame.recipePanel.update();
                retVal = oldRecipeId;
            } else {
                retVal = -1;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw new Exception("Can't edit recipe, the exception was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (pst1 != null) pst1.close();
                pst1 = null;
                if (pst2 != null) pst2.close();
                pst2 = null;
            } catch (SQLException sqle) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return retVal;
    }

    /**
	 * Updates ingredients for the specific recipe.
	 * Used by editRecipe-function
	 * @param Recipe recipe is bean for the recipe.
	 * @param int id is id of the recipe
	 * @throws SQLException
	 */
    private void updateIngredients(Recipe recipe, int id) throws Exception {
        PreparedStatement pst1 = null;
        PreparedStatement pst2 = null;
        try {
            conn = getConnection();
            pst1 = conn.prepareStatement("DELETE FROM ingredients WHERE recipe_id = ?");
            pst1.setInt(1, id);
            if (pst1.executeUpdate() >= 0) {
                pst2 = conn.prepareStatement("INSERT INTO ingredients (recipe_id, name, amount, measure_id, shop_flag) VALUES (?,?,?,?,?)");
                IngredientContainer ings = recipe.getIngredients();
                Ingredient ingBean = null;
                Iterator it;
                for (it = ings.getIngredients().iterator(); it.hasNext(); ) {
                    ingBean = (Ingredient) it.next();
                    pst2.setInt(1, id);
                    pst2.setString(2, ingBean.getName());
                    pst2.setDouble(3, ingBean.getAmount());
                    pst2.setInt(4, ingBean.getType());
                    pst2.setInt(5, ingBean.getShopFlag());
                    pst2.executeUpdate();
                }
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            MainFrame.appendStatusText("Can't add ingredient, the exception was " + e.getMessage());
        } finally {
            try {
                if (pst1 != null) pst1.close();
                pst1 = null;
                if (pst2 != null) pst2.close();
                pst2 = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
    }

    /**
	 * Removes a recipe and the corresponding ingredients from the database
	 * @param Recipe recipe is a recipe bean ... perhaps
	 * @throws Exception if can't execute sql statements ... maybe
	 */
    public void removeRecipe(Recipe recipe) throws Exception {
        PreparedStatement pst1 = null;
        PreparedStatement pst2 = null;
        PreparedStatement pst3 = null;
        ResultSet rs = null;
        try {
            int id = -1;
            conn = getConnection();
            pst1 = conn.prepareStatement("SELECT recipe_id FROM recipes WHERE name = ? AND instructions = ? ");
            pst1.setString(1, recipe.getName());
            pst1.setString(2, recipe.getInstructions());
            rs = pst1.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1);
            }
            pst2 = conn.prepareStatement("DELETE FROM ingredients WHERE recipe_id = ? ");
            pst3 = conn.prepareStatement("DELETE FROM recipes WHERE recipe_id = ? ");
            pst2.setInt(1, id);
            pst3.setInt(1, id);
            pst2.executeUpdate();
            if (pst3.executeUpdate() > 0) {
                MainFrame.appendStatusText("Resepti poistettu kannasta");
            } else {
                MainFrame.appendStatusText("Resepti� poistettaessa tietokannasta tapahtui virhe");
            }
            conn.commit();
            MainFrame.recipePanel.update();
            MainFrame.recipePanel.update();
        } catch (Exception e) {
            conn.rollback();
            MainFrame.appendStatusText("Can't remove recipe, the exception was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (pst1 != null) pst1.close();
                pst1 = null;
                if (pst2 != null) pst2.close();
                pst2 = null;
                if (pst3 != null) pst3.close();
                pst3 = null;
            } catch (SQLException sqle) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
    }

    /**
	 * Fetches all the measures from the database
     * @return Vector 
	 */
    public Vector getMeasures() {
        PreparedStatement stat = null;
        ResultSet rs = null;
        Vector v = new Vector();
        try {
            conn = getConnection();
            stat = conn.prepareStatement("SELECT name FROM measure");
            rs = stat.executeQuery();
            while (rs.next()) {
                System.out.println("getMeasures -- Mittayksikk�: " + rs.getString(1));
                v.add(rs.getString(1));
            }
        } catch (Exception e) {
            MainFrame.appendStatusText("Can't get measures, the exception was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (stat != null) stat.close();
                stat = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return v;
    }

    /**
	 * Fetches discription to type from the database
	 * @param int type is a index ox measure
	 * @return String measure
	 */
    public String getMeasure(int type) {
        PreparedStatement stat = null;
        ResultSet rs = null;
        String retVal = "";
        try {
            conn = getConnection();
            stat = conn.prepareStatement("SELECT name FROM measure WHERE measure_id = ?");
            stat.setInt(1, type);
            rs = stat.executeQuery();
            while (rs.next()) {
                retVal = rs.getString(1);
            }
        } catch (Exception e) {
            MainFrame.appendStatusText("Can't get measure, the exception was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (stat != null) stat.close();
                stat = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return retVal;
    }

    /**
     * 
     * @param int recipeId
     * @param Timestamp time
     * @param int meal
     */
    public boolean setRecipeToTimetable(int recipeId, Timestamp time, int meal) {
        System.out.println("setRecipeToTimetable");
        PreparedStatement statement = null;
        StringBuffer query = new StringBuffer("insert into timetable (recipe_id, time, meal) values (?,?,?)");
        try {
            conn = getConnection();
            statement = conn.prepareStatement(query.toString());
            statement.setInt(1, recipeId);
            statement.setTimestamp(2, time);
            statement.setInt(3, meal);
            statement.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ex) {
            }
            MainFrame.appendStatusText("Error when trying to execute sql: " + e.getMessage());
        } finally {
            try {
                if (statement != null) statement.close();
                statement = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return true;
    }

    /**
     * 
     * @param String recipeName
     * @return int recipeId
     */
    public int getRecipeId(String recipeName) {
        System.out.println("getRecipeId");
        PreparedStatement statement = null;
        ResultSet rs = null;
        int returnValue = -1;
        StringBuffer query = new StringBuffer("select recipe_id from recipes where name = ?");
        try {
            conn = getConnection();
            statement = conn.prepareStatement(query.toString());
            statement.setString(1, recipeName);
            rs = statement.executeQuery();
            if (rs.next()) returnValue = rs.getInt(1);
        } catch (Exception e) {
            MainFrame.appendStatusText("Error when trying to execute sql: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (statement != null) statement.close();
                statement = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return returnValue;
    }

    /**
	 * Fetches categories from the database
	 * @return Vector include catogory beans
	 */
    public Vector getCategories() {
        PreparedStatement stat = null;
        ResultSet rs = null;
        Vector categoryVector = new Vector();
        try {
            conn = getConnection();
            stat = conn.prepareStatement("SELECT * FROM categories");
            rs = stat.executeQuery();
            while (rs.next()) {
                Category catBean = new Category();
                catBean.setCategoryId(rs.getInt("category_id"));
                catBean.setName(rs.getString("name"));
                if (rs.getString("description") != null) catBean.setDescription(rs.getString("description"));
                categoryVector.add(catBean);
            }
        } catch (Exception e) {
            MainFrame.appendStatusText("Can't get categories, the exception was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (stat != null) stat.close();
                stat = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return categoryVector;
    }

    /**
	 * 
     * @param String categoryName
	 * @return Vector include catogory beans
	 */
    public Vector getRecipesByCategory(String categoryName) {
        PreparedStatement stat1 = null;
        PreparedStatement stat2 = null;
        ResultSet rs = null;
        Vector recipeVector = new Vector();
        try {
            conn = getConnection();
            stat1 = conn.prepareStatement("SELECT category_id FROM categories WHERE name = ?");
            stat1.setString(1, categoryName);
            ResultSet rs_id = stat1.executeQuery();
            if (categoryName.equalsIgnoreCase("kaikki")) {
                stat2 = conn.prepareStatement("SELECT * FROM recipes");
            } else {
                stat2 = conn.prepareStatement("SELECT * FROM recipes WHERE category_id = ?");
                if (rs_id.next()) stat2.setInt(1, rs_id.getInt("category_id"));
            }
            rs = stat2.executeQuery();
            Recipe recipeBean;
            while (rs.next()) {
                recipeBean = new Recipe(rs.getInt("recipe_id"), rs.getString("name"), getIngredientsById(rs.getInt("recipe_id")), rs.getString("instructions"), rs.getInt("category_id"));
                recipeVector.add(recipeBean);
            }
        } catch (Exception e) {
            MainFrame.appendStatusText("Can't get recipes for category \"" + categoryName + "\", the exception was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (stat1 != null) stat1.close();
                stat1 = null;
                if (stat2 != null) stat2.close();
                stat2 = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return recipeVector;
    }

    /**
	 * 
     * @param String categoryName
	 * @return Vector include catogory beans
	 */
    public Recipe getRecipeById(int recipeId) {
        PreparedStatement stat1 = null;
        ResultSet rs = null;
        Recipe recipeBean = null;
        try {
            conn = getConnection();
            stat1 = conn.prepareStatement("SELECT * FROM recipes WHERE recipe_id = ?");
            stat1.setInt(1, recipeId);
            rs = stat1.executeQuery();
            if (rs.next()) {
                recipeBean = new Recipe(recipeId, rs.getString("name"), getIngredientsById(recipeId), rs.getString("instructions"), rs.getInt("category_id"));
            }
        } catch (Exception e) {
            MainFrame.appendStatusText("Can't get recipe for id \"" + recipeId + "\", the exception was " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (stat1 != null) stat1.close();
                stat1 = null;
            } catch (Exception ex) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return recipeBean;
    }

    /**
	 * Lis�� uuden schedulen timetable-tauluun. Poistaa
	 * kyseisen p�iv�n vanhat tiedot.
	 * @param Schedule s
	 * @return boolean
	 */
    public boolean setSchedule(Schedule s) {
        PreparedStatement pst1 = null;
        PreparedStatement pst2 = null;
        PreparedStatement pst3 = null;
        ResultSet rs2 = null;
        boolean retVal = true;
        try {
            conn = getConnection();
            pst1 = conn.prepareStatement("INSERT INTO timetable (recipe_id, time, meal) VALUES (?, ?, ?);");
            pst2 = conn.prepareStatement("SELECT * FROM timetable WHERE time BETWEEN ? AND ?");
            pst3 = conn.prepareStatement("DELETE FROM timetable WHERE time = ? AND meal = ? AND recipe_id = ?");
            long dateInMillis = s.getDate().getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");
            Date beginDate = null, endDate = null;
            try {
                String temp = sdf.format(new java.util.Date(dateInMillis));
                sdf.applyPattern("yyyy-MM-dd");
                java.util.Date temppidate = sdf.parse(temp);
                beginDate = new Date(temppidate.getTime());
                endDate = new Date(temppidate.getTime() + (24 * 3600 * 1000));
            } catch (Exception e) {
                System.out.println("Ollos virhe saapunut, siks ohjelmamme kaatunut! --Vanha kalevalalainen sananlasku--");
                e.printStackTrace();
            }
            pst2.setDate(1, beginDate);
            pst2.setDate(2, endDate);
            rs2 = pst2.executeQuery();
            MainFrame.appendStatusText("Poistetaan p�iv�n \"" + s.getDate() + "\" vanhat reseptit kannasta");
            while (rs2.next()) {
                pst3.clearParameters();
                pst3.setTimestamp(1, rs2.getTimestamp("time"));
                pst3.setInt(2, rs2.getInt("meal"));
                pst3.setInt(3, rs2.getInt("recipe_id"));
                pst3.executeUpdate();
            }
            if (s.getBreakfast() != null) {
                MainFrame.appendStatusText("Lis�t��n aamupala \"" + s.getBreakfast().getName() + "\"");
                pst1.clearParameters();
                pst1.setInt(1, s.getBreakfast().getId());
                pst1.setTimestamp(2, new Timestamp(s.getDate().getTime()));
                pst1.setInt(3, 1);
                pst1.executeUpdate();
            }
            if (s.getLunch() != null) {
                MainFrame.appendStatusText("Lis�t��n lounas \"" + s.getLunch().getName() + "\"");
                pst1.clearParameters();
                pst1.setInt(1, s.getLunch().getId());
                pst1.setTimestamp(2, new Timestamp(s.getDate().getTime()));
                pst1.setInt(3, 2);
                pst1.executeUpdate();
            }
            if (s.getSnack() != null) {
                MainFrame.appendStatusText("Lis�t��n v�lipala \"" + s.getSnack().getName() + "\"");
                pst1.clearParameters();
                pst1.setInt(1, s.getSnack().getId());
                pst1.setTimestamp(2, new Timestamp(s.getDate().getTime()));
                pst1.setInt(3, 3);
                pst1.executeUpdate();
            }
            if (s.getDinner() != null) {
                MainFrame.appendStatusText("Lis�t��n p�iv�llinen \"" + s.getDinner().getName() + "\"");
                pst1.clearParameters();
                pst1.setInt(1, s.getDinner().getId());
                pst1.setTimestamp(2, new Timestamp(s.getDate().getTime()));
                pst1.setInt(3, 4);
                pst1.executeUpdate();
            }
            if (s.getSupper() != null) {
                MainFrame.appendStatusText("Lis�t��n illallinen \"" + s.getSupper().getName() + "\"");
                pst1.clearParameters();
                pst1.setInt(1, s.getSupper().getId());
                pst1.setTimestamp(2, new Timestamp(s.getDate().getTime()));
                pst1.setInt(3, 5);
                pst1.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                MainFrame.appendStatusText("Aterioiden lis�ys ep�onnistui");
                e1.printStackTrace();
            }
            MainFrame.appendStatusText("Can't add schedule, the exception was " + e.getMessage());
        } finally {
            try {
                if (rs2 != null) rs2.close();
                rs2 = null;
                if (pst1 != null) pst1.close();
                pst1 = null;
                if (pst2 != null) pst2.close();
                pst2 = null;
            } catch (SQLException sqle) {
                MainFrame.appendStatusText("Can't close database connection.");
            }
        }
        return retVal;
    }

    /**
	 * Hakee kannan timetable-taulusta annetun p�iv�n tiedot ja 
	 * palauttaa ne Schedule:ssa 
	 * @param java.sql.Date date
	 * @return Schedule s
	 */
    public Schedule getSchedule(Date date) {
        Schedule s = new Schedule();
        s.setDate(date);
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = conn.prepareStatement("SELECT * FROM timetable WHERE time BETWEEN ? AND ?");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");
            Date beginDate = null, endDate = null;
            try {
                String temp = sdf.format(new java.util.Date(date.getTime()));
                sdf.applyPattern("yyyy-MM-dd");
                java.util.Date temppidate = sdf.parse(temp);
                beginDate = new Date(temppidate.getTime());
                endDate = new Date(temppidate.getTime() + (24 * 3600 * 1000));
            } catch (Exception e) {
                System.out.println("Ollos virhe saapunut, siks ohjelmamme kaatunut! --Vanha kalevalalainen sananlasku--");
                e.printStackTrace();
            }
            pst.setDate(1, beginDate);
            pst.setDate(2, endDate);
            rs = pst.executeQuery();
            while (rs.next()) {
                if (rs.getInt("meal") == 1) {
                    s.setBreakfast(this.getRecipeById(rs.getInt("recipe_id")));
                } else if (rs.getInt("meal") == 2) {
                    s.setLunch(this.getRecipeById(rs.getInt("recipe_id")));
                } else if (rs.getInt("meal") == 3) {
                    s.setSnack(this.getRecipeById(rs.getInt("recipe_id")));
                } else if (rs.getInt("meal") == 4) {
                    s.setDinner(this.getRecipeById(rs.getInt("recipe_id")));
                } else if (rs.getInt("meal") == 5) {
                    s.setSupper(this.getRecipeById(rs.getInt("recipe_id")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                rs = null;
                if (pst != null) pst.close();
                pst = null;
            } catch (SQLException sqle) {
                MainFrame.appendStatusText("Can't close prepared statements.");
            }
        }
        return s;
    }
}
