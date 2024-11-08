package jamm;

import jamm.utils.JammEnum;
import jamm.utils.WriteLog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.DefaultListModel;

public class Options extends Observable {

    private Properties prop;

    /**
   * Current selected Month INDEX
   */
    private Integer current_month_index;

    /**
   * Current selected Year INDEX
   */
    private Integer current_year_index;

    /**
   * Current selected sector INDEX
   */
    private Integer current_sector_index;

    private String current_locale;

    private Boolean file_logs;

    private Boolean console_logs;

    private JammEnum file_log_type;

    private JammEnum console_log_type;

    private Integer windowWidth_FinanceGui;

    private Integer windowHeight_FinanceGui;

    private Integer windowWidth_OverviewGui;

    private Integer windowHeight_OverviewGui;

    private Integer dividerLoc_Splitpane;

    private File jamm_folder;

    private File log_file_folder;

    private File log_file;

    private File options_folder;

    private File options_file;

    private File db_folder;

    private File db_file;

    private InputStream db_backupFile;

    private File accounts_file;

    private File years_file;

    private File colSizes_file;

    /**
   * String Array with all Month
   */
    private String[] list_month;

    /**
   * ListModel to store all sectors
   */
    private DefaultListModel list_sectors;

    /**
   * ListModel to store all accounts
   */
    private DefaultListModel list_accounts;

    /**
   * ArrayList to store all years
   */
    private ArrayList<Integer> list_years;

    private ArrayList<Integer> list_columnSizes;

    private WriteLog logger;

    public Options() {
        prop = new Properties();
        logger = new WriteLog(this);
        list_month = getTranslatedMonths();
        list_sectors = new DefaultListModel();
        list_accounts = new DefaultListModel();
        list_years = new ArrayList<Integer>();
        list_columnSizes = new ArrayList<Integer>();
        current_month_index = new Integer(0);
        current_year_index = new Integer(0);
        current_sector_index = new Integer(0);
        current_locale = "EN";
        file_logs = new Boolean(true);
        console_logs = new Boolean(true);
        file_log_type = JammEnum.WRITE_LOG_ALL;
        console_log_type = JammEnum.WRITE_LOG_ALL;
        windowWidth_FinanceGui = new Integer(0);
        windowHeight_FinanceGui = new Integer(0);
        windowWidth_OverviewGui = new Integer(0);
        windowHeight_OverviewGui = new Integer(0);
        dividerLoc_Splitpane = new Integer(0);
        jamm_folder = new File(System.getProperty("user.home") + "/.jamm");
        log_file_folder = new File(jamm_folder + "/LogFiles");
        log_file = new File(log_file_folder + "/logfile.log");
        options_folder = new File(jamm_folder + "/Options");
        options_file = new File(options_folder + "/jamm.properties");
        db_folder = new File(jamm_folder + "/DB");
        db_file = new File(db_folder + "/jamm.db3");
        db_backupFile = (Toolkit.getDefaultToolkit().getClass().getResourceAsStream("/res/DB/jamm.db3"));
        accounts_file = new File(db_folder + "/accounts");
        years_file = new File(db_folder + "/years");
        colSizes_file = new File(db_folder + "/colSizes");
    }

    public void notifyTheObservers() {
        super.setChanged();
        super.notifyObservers();
    }

    public Integer getCurrent_month_index() {
        return current_month_index;
    }

    public void setCurrent_month_index(Integer current_month_index) {
        this.current_month_index = current_month_index;
    }

    public Integer getCurrent_year_index() {
        return current_year_index;
    }

    public void setCurrent_year_index(Integer current_year_index) {
        this.current_year_index = current_year_index;
        notifyTheObservers();
    }

    public Integer getCurrent_sector_index() {
        return current_sector_index;
    }

    public void setCurrent_sector_index(Integer current_sector_index) {
        this.current_sector_index = current_sector_index;
    }

    public void setCurrent_locale(String newLocale) {
        this.current_locale = newLocale;
        notifyTheObservers();
    }

    public String getCurrent_locale() {
        return this.current_locale;
    }

    public Boolean getConsole_logs() {
        return console_logs;
    }

    public void setConsole_logs(Boolean console_logs) {
        this.console_logs = console_logs;
    }

    public JammEnum getFile_log_type() {
        return file_log_type;
    }

    public void setFile_log_type(JammEnum file_log_type) {
        this.file_log_type = file_log_type;
    }

    public JammEnum getConsole_log_type() {
        return console_log_type;
    }

    public void setConsole_log_type(JammEnum console_log_type) {
        this.console_log_type = console_log_type;
    }

    public File getDb_folder() {
        return db_folder;
    }

    public void setDb_folder(File db_folder) {
        this.db_folder = db_folder;
    }

    public DefaultListModel getList_sectors() {
        return list_sectors;
    }

    public void setList_sectors(DefaultListModel list_sectors) {
        this.list_sectors = list_sectors;
    }

    public DefaultListModel getList_accounts() {
        return list_accounts;
    }

    public void setList_accounts(DefaultListModel list_accounts) {
        this.list_accounts = list_accounts;
    }

    public ArrayList<Integer> getList_years() {
        return list_years;
    }

    public void setList_years(ArrayList<Integer> list_years) {
        this.list_years = list_years;
    }

    public ArrayList<Integer> getList_colSizes() {
        return this.list_columnSizes;
    }

    public void setList_colSizes(ArrayList<Integer> list_col) {
        this.list_columnSizes = list_col;
    }

    public void setFile_logs(Boolean file_logs) {
        this.file_logs = file_logs;
    }

    public Boolean getFile_logs() {
        return file_logs;
    }

    public File getJamm_folder() {
        return jamm_folder;
    }

    public File getLog_file_folder() {
        return log_file_folder;
    }

    public File getLog_file() {
        return log_file;
    }

    public File getOptions_folder() {
        return options_folder;
    }

    public File getOptions_file() {
        return options_file;
    }

    public File getDb_file() {
        return db_file;
    }

    public InputStream getDb_backupFile() {
        return db_backupFile;
    }

    public File getAccounts_file() {
        return accounts_file;
    }

    public File getYears_file() {
        return years_file;
    }

    public File getColumns_file() {
        return colSizes_file;
    }

    public String[] getList_month() {
        return list_month;
    }

    public void updateList_month() {
        this.list_month = getTranslatedMonths();
    }

    public Properties getProp() {
        return prop;
    }

    public Dimension getDimFinanceGui() {
        return new Dimension(windowWidth_FinanceGui, windowHeight_FinanceGui);
    }

    public void setDimFinanceGui(Dimension dim) {
        windowWidth_FinanceGui = dim.width;
        windowHeight_FinanceGui = dim.height;
    }

    public Dimension getDimOverviewGui() {
        return new Dimension(windowWidth_OverviewGui, windowHeight_OverviewGui);
    }

    public void setDimOverviewGui(Dimension dim) {
        windowWidth_OverviewGui = dim.width;
        windowHeight_OverviewGui = dim.height;
    }

    public Integer getDividerLocSplitpane() {
        return dividerLoc_Splitpane;
    }

    public void setDividerLocSplitpane(int divLoc) {
        dividerLoc_Splitpane = divLoc;
    }

    public void readOptions() {
        try {
            prop.load(new FileInputStream(options_file));
            current_month_index = Integer.parseInt(prop.getProperty("current_month_index"));
            current_year_index = Integer.parseInt(prop.getProperty("current_year_index"));
            current_sector_index = Integer.parseInt(prop.getProperty("current_section_index"));
            current_locale = prop.getProperty("current_locale");
            windowWidth_FinanceGui = Integer.parseInt(prop.getProperty("windowWidth_FinanceGui"));
            windowHeight_FinanceGui = Integer.parseInt(prop.getProperty("WindowHeight_FinanceGui"));
            windowWidth_OverviewGui = Integer.parseInt(prop.getProperty("windowWidth_OverviewGui"));
            windowHeight_OverviewGui = Integer.parseInt(prop.getProperty("windowHeight_OverviewGui"));
            dividerLoc_Splitpane = Integer.parseInt(prop.getProperty("dividerLoc_Splitpane"));
            if (getColumns_file().exists()) {
                readArray(JammEnum.READ_COLUMNSIZES);
            }
            if (list_columnSizes.size() != 6) list_columnSizes = getDefaultColumnSizes();
            file_logs = Boolean.parseBoolean(prop.getProperty("write_logs"));
            console_logs = Boolean.parseBoolean(prop.getProperty("console_logs"));
            file_log_type = JammEnum.valueOf(prop.getProperty("write_log_type"));
            console_log_type = JammEnum.valueOf(prop.getProperty("console_log_type"));
            db_folder = new File(prop.getProperty("db_folder"));
            logger.writeLog(JammEnum.INFO, "Finished Options reading");
        } catch (IOException ex) {
            logger.writeLog(JammEnum.ERROR, "IOException during Options read");
            ex.printStackTrace();
        }
    }

    public void writeOptions() {
        try {
            prop.setProperty("current_month_index", current_month_index.toString());
            prop.setProperty("current_year_index", current_year_index.toString());
            prop.setProperty("current_section_index", current_sector_index.toString());
            prop.setProperty("current_locale", current_locale);
            prop.setProperty("windowWidth_FinanceGui", windowWidth_FinanceGui.toString());
            prop.setProperty("WindowHeight_FinanceGui", windowHeight_FinanceGui.toString());
            prop.setProperty("windowWidth_OverviewGui", windowWidth_OverviewGui.toString());
            prop.setProperty("windowHeight_OverviewGui", windowHeight_OverviewGui.toString());
            prop.setProperty("dividerLoc_Splitpane", dividerLoc_Splitpane.toString());
            prop.setProperty("write_logs", file_logs.toString());
            prop.setProperty("console_logs", console_logs.toString());
            prop.setProperty("write_log_type", file_log_type.toString());
            prop.setProperty("console_log_type", console_log_type.toString());
            prop.setProperty("db_folder", db_folder.getPath());
            prop.store(new FileOutputStream(options_file), null);
            logger.writeLog(JammEnum.INFO, "Finished Options writing");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
   * Write a given Array to HDD
   * 
   * @param objects
   *          Array of Objects, which will be written to the HDD
   * @param type
   *          Type of writing Data: WRITE_ACCOUNTS, WRITE_YEARS
   * @throws IllegalArgumentException
   *           If the given Enum does not match
   */
    public void writeArray(Object[] objects, JammEnum type) throws IllegalArgumentException {
        try {
            PrintWriter writer;
            String info_text;
            switch(type) {
                case WRITE_ACCOUNTS:
                    accounts_file.delete();
                    writer = new PrintWriter(new FileWriter(accounts_file, true));
                    for (Object o : objects) {
                        writer.println((String) o);
                    }
                    info_text = "Finished Accounts Array writing";
                    break;
                case WRITE_YEARS:
                    years_file.delete();
                    writer = new PrintWriter(new FileWriter(years_file, true));
                    for (Object o : objects) {
                        writer.println(o);
                    }
                    info_text = "Finished Years Array writing";
                    break;
                case WRITE_COLUMNSIZES:
                    colSizes_file.delete();
                    writer = new PrintWriter(new FileWriter(colSizes_file, true));
                    for (Object o : objects) {
                        writer.println(o);
                    }
                    info_text = "Finished ColumnSize Array writing";
                    break;
                default:
                    logger.writeLog(JammEnum.ERROR, "IOException during Array write");
                    throw new IllegalArgumentException();
            }
            logger.writeLog(JammEnum.INFO, info_text);
            writer.close();
        } catch (IOException e) {
            logger.writeLog(JammEnum.ERROR, "File not found on HDD");
            e.printStackTrace();
        }
    }

    /**
   * Read an Array of Objects from the HDD
   * 
   * @param type
   *          Type of Data to read: READ_ACCOUNTS, READ_YEARS
   * @throws IllegalArgumentException
   *           If the given Enum does not match
   */
    public void readArray(JammEnum type) throws IllegalArgumentException {
        try {
            FileReader fr;
            BufferedReader br;
            String line;
            switch(type) {
                case READ_ACCOUNTS:
                    fr = new FileReader(accounts_file);
                    br = new BufferedReader(fr);
                    while ((line = br.readLine()) != null) {
                        list_accounts.addElement(line);
                    }
                    logger.writeLog(JammEnum.INFO, "Finished Accounts Array reading");
                    break;
                case READ_COLUMNSIZES:
                    list_columnSizes.clear();
                    fr = new FileReader(colSizes_file);
                    br = new BufferedReader(fr);
                    while (((line = br.readLine()) != null)) {
                        list_columnSizes.add(Integer.parseInt(line));
                    }
                    break;
                default:
                    logger.writeLog(JammEnum.ERROR, "IOException during Array read");
                    throw new IllegalArgumentException();
            }
            fr.close();
        } catch (IOException e) {
            logger.writeLog(JammEnum.ERROR, "File not found on HDD");
            e.printStackTrace();
        }
    }

    public void updateFolderPath() {
        db_file = new File(db_folder + "/jamm.db3");
        accounts_file = new File(db_folder + "/accounts");
        years_file = new File(db_folder + "/years");
    }

    private String[] getTranslatedMonths() {
        String[] months = new String[12];
        months[0] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_January");
        months[1] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_February");
        months[2] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_March");
        months[3] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_April");
        months[4] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_May");
        months[5] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_June");
        months[6] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_July");
        months[7] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_August");
        months[8] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_September");
        months[9] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_October");
        months[10] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_November");
        months[11] = ResourceBundle.getBundle("res.localization.localizationMessages").getString("General_Months_December");
        return months;
    }

    /**
   * Returns a Hashtable containing the TableHeaders and their size
   * 
   * @return HashTable<String,Integer>
   */
    private ArrayList<Integer> getDefaultColumnSizes() {
        ArrayList<Integer> colSizes = new ArrayList();
        colSizes.add(0);
        colSizes.add(20);
        colSizes.add(15);
        colSizes.add(20);
        colSizes.add(15);
        colSizes.add(30);
        return colSizes;
    }
}
