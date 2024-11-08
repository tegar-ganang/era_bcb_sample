package org.jbudget.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jbudget.Core.Account;
import org.jbudget.Core.Budget;
import org.jbudget.Core.Month;
import org.jbudget.Core.SharedPreferences;
import org.jbudget.Core.Transaction;
import org.jbudget.Core.User;
import org.jbudget.gui.user.UserPreferences;
import org.jbudget.util.DateFormater;

/**
 *
 * @author petrov
 */
public class LocalStore implements StorageInterface {

    /** Root directory for storing all the data */
    private final File rootDirectory;

    /** Index of available data */
    private DataIndex dataIndex;

    /** A flag that is set to true when a new DataIndex is created. This
   *  means that an empty data directory is initialized. */
    private boolean newDataIndexFlag = false;

    /** A map of JDOM Elements for Month objects that were loaded from 
   *  the storage. Used for caching to prevent excessive reading and
   *  saving of XML data.
   */
    private final Map<Integer, Element> monthElementsMap = new HashMap<Integer, Element>();

    /** A map of JDOM Elements for Budget objects that were loaded from 
   *  the storage. Used for caching to prevent excessive reading and
   *  saving/reading of XML data.
   */
    private final Map<Integer, Element> budgetElementsMap = new HashMap<Integer, Element>();

    /** A map of JDOM Elements for Account objects that were loaded from 
   *  the storage. Used for caching to prevent excessive reading and
   *  saving/reading of XML data.
   */
    private final Map<Integer, Element> accountElementsMap = new HashMap<Integer, Element>();

    /** A map of JDOM Elements for User objects that were loaded from 
   *  the storage. Used for caching to prevent excessive reading and
   *  saving/reading of XML data.
   */
    private final Map<Integer, Element> userElementsMap = new HashMap<Integer, Element>();

    /** A map of JDOM Elements for UserPreferences objects that were loaded from 
   *  the storage. Used for caching to prevent excessive reading and
   *  saving/reading of XML data.
   */
    private final Map<Integer, Element> userPreferencesElementsMap = new HashMap<Integer, Element>();

    /** Global SharedPreference object used by the program. */
    private SharedPreferences sharedPreferences = null;

    /** Creates a new instance of LocalStore */
    public LocalStore(File rootDirectory) throws IOException, JDOMException {
        this.rootDirectory = rootDirectory;
        initialize();
    }

    /** Creates a new instance of LocalStore */
    public LocalStore(String rootDirectoryStr) throws IOException, JDOMException {
        this.rootDirectory = new File(rootDirectoryStr);
        initialize();
    }

    /** Makes sure that the root directory for storing all the files 
   *  exists.
   */
    private void initialize() throws IOException, JDOMException {
        if (!rootDirectory.isDirectory()) throw new IOException("Path has to point to a directory: " + rootDirectory);
        if (!rootDirectory.exists()) rootDirectory.mkdirs();
        if ((!rootDirectory.canRead()) || (!rootDirectory.canWrite())) throw new IOException("Can not read or write to directory: " + rootDirectory);
        initializeIndex();
    }

    /** Initializes an index of available data. Reads it from a file or creates
   *  a new one.
   */
    private synchronized void initializeIndex() throws JDOMException, IOException {
        File dataIndexFile = new File(rootDirectory, "data_index.xml");
        if (!dataIndexFile.canRead()) {
            dataIndex = new DataIndex(false);
            newDataIndexFlag = true;
            return;
        }
        InputStream iStream = new BufferedInputStream(new FileInputStream(dataIndexFile));
        SAXBuilder builder = new SAXBuilder();
        Document indexDoc = builder.build(iStream);
        Element indexElement = indexDoc.getRootElement();
        indexDoc.setRootElement(new Element("Empty"));
        dataIndex = DataIndex.getInstance(indexElement);
    }

    /** Saves the index of available data to a file */
    private synchronized void storeIndex() throws IOException {
        File dataIndexFile = new File(rootDirectory, "data_index.xml");
        OutputStream oStream = new BufferedOutputStream(new FileOutputStream(dataIndexFile));
        Element indexElement = dataIndex.getXmlElement();
        Document indexDocument = new Document();
        indexDocument.setRootElement(indexElement);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(indexDocument, oStream);
        oStream.close();
        indexDocument.setRootElement(new Element("Empty"));
    }

    /** Interface implementation */
    public synchronized DataIndex getDataIndex() {
        return dataIndex;
    }

    public boolean isNewDataInidex() {
        return newDataIndexFlag;
    }

    /** Reads a JDOM Element for a given Month from a corresponding file.
   *  Adds the element to the monthsElementsMap.
   *  @throws IOException if there is a problem reading the file.
   *  @throws JDOMExceptoin if there is a problem parsing the contents of 
   *          the file.
   */
    private Element readMonthElement(int cMonth, int cYear) throws IOException, JDOMException {
        File monthFile = getMonthFile(cMonth, cYear);
        InputStream iStream = new BufferedInputStream(new FileInputStream(monthFile));
        SAXBuilder builder = new SAXBuilder();
        Document monthDoc = builder.build(iStream);
        Element monthElement = monthDoc.getRootElement();
        monthDoc.setRootElement(new Element("Empty"));
        Integer monthIndex = new Integer(cYear * 100 + cMonth);
        monthElementsMap.put(monthIndex, monthElement);
        return monthElement;
    }

    /** Stores a JDOM Element corresponding to a Month object to a file.
   * @throws IOException if there are IO errors.
   */
    private void saveMonthElement(Element element, int cMonth, int cYear) throws IOException, JDOMException {
        File monthFile = getMonthFile(cMonth, cYear);
        File dir = monthFile.getParentFile();
        dir.mkdirs();
        backupFile(monthFile);
        OutputStream oStream = new BufferedOutputStream(new FileOutputStream(monthFile));
        Document monthDocument = new Document();
        monthDocument.setRootElement(element);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(monthDocument, oStream);
        oStream.close();
        monthDocument.setRootElement(new Element("Empty"));
    }

    /** Returns a JDOM element that corresponds to the given month.
   *  Returns a cached version if it is available. Otherwise reads the 
   *  object from permanent storage.
   *  @throws IllegalArgumentException if month is not available.
   */
    private Element getMonthElement(int month, int year) throws IOException, JDOMException {
        if (!dataIndex.isValidMonth(month, year)) throw new IllegalArgumentException("Trying to get a Month element for a " + "month that does not exist.\n year: " + year + ", month: " + (new DateFormater()).formatMonth(month));
        Integer monthIndex = new Integer(year * 100 + month);
        Element element = monthElementsMap.get(monthIndex);
        if (element == null) element = readMonthElement(month, year);
        return element;
    }

    /** Implementing the interface. Gets a JDOM Element for a month converts
   *  it to a Month object and returns it.
   * @throws IOException if there are IO errors.
   * @throws JDOMException if there are parsing errors.
   */
    public synchronized Month getMonth(int cMonth, int cYear) throws IOException {
        try {
            Element monthElement = getMonthElement(cMonth, cYear);
            Month month = Month.getInstance(monthElement);
            return month;
        } catch (JDOMException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /** Implementing the interface. Converts a give month object to a JDOM Element
   *  and stores it in the cache. Also adds a record in the data index. Does
   *  not actually save the Element to disk, this is done when the cache is 
   *  synchronized to disk.
   */
    public synchronized boolean storeMonth(Month month) {
        int cMonth = month.getMonth();
        int cYear = month.getYear();
        Element monthElement = month.getXmlElement();
        Integer monthIndex = new Integer(cYear * 100 + cMonth);
        monthElementsMap.put(monthIndex, monthElement);
        dataIndex.registerMonth(cMonth, cYear);
        return true;
    }

    /** Implementing the interfaces. Removes a month. 
    * @throws IllegalArgumentException if there is no such month
    */
    public synchronized boolean removeMonth(int cMonth, int cYear) {
        if (!dataIndex.isValidMonth(cMonth, cYear)) throw new IllegalArgumentException("Trying to remove a Montn " + "that does not exist.\n year: " + cYear + ", month: " + (new DateFormater()).formatMonth(cMonth));
        dataIndex.unregisterMonth(cMonth, cYear);
        Integer monthIndex = new Integer(cYear * 100 + cMonth);
        monthElementsMap.remove(monthIndex);
        File monthFile = getMonthFile(cMonth, cYear);
        if (monthFile.exists()) monthFile.delete();
        return true;
    }

    /** Implementing the interfaces. Removes a month. 
    * @throws IllegalArgumentException if there is no such month
    */
    public synchronized boolean removeMonth(Month month) {
        return removeMonth(month.getMonth(), month.getYear());
    }

    /** Reads a JDOM Element containing the budget with given budget id.
   * @throws IOException
   * @throws IllegalArgumentException if there is no such budget.
   */
    private Element readBudgetElement(int id) throws IOException, JDOMException {
        if (!dataIndex.isValidBudget(id)) throw new IllegalArgumentException("No budget with id: " + id);
        File budgetFile = getBudgetFile(id);
        InputStream iStream = new BufferedInputStream(new FileInputStream(budgetFile));
        SAXBuilder builder = new SAXBuilder();
        Document budgetDoc = builder.build(iStream);
        Element budgetElement = budgetDoc.getRootElement();
        budgetDoc.setRootElement(new Element("Empty"));
        Integer budgetIndex = new Integer(id);
        budgetElementsMap.put(budgetIndex, budgetElement);
        return budgetElement;
    }

    /** Saves a JDOM Element containing the budget with given budget id. */
    private void saveBudgetElement(Element element, int id) throws IOException, JDOMException {
        File budgetFile = getBudgetFile(id);
        File dir = budgetFile.getParentFile();
        dir.mkdirs();
        backupFile(budgetFile);
        OutputStream oStream = new BufferedOutputStream(new FileOutputStream(budgetFile));
        Document budgetDocument = new Document();
        budgetDocument.setRootElement(element);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(budgetDocument, oStream);
        oStream.close();
        budgetDocument.setRootElement(new Element("Empty"));
    }

    /** Returns a JDOM Element containing the budget with given id. 
   *  Uses a cached version if it is avalilable. Otherwise the Element
   *  is read from storage.
   * @throws IllegalArgumentException if there is no budget with the given id
   */
    private Element getBudgetElement(int id) throws IOException, JDOMException {
        if (!dataIndex.isValidBudget(id)) throw new IllegalArgumentException("No budget with id: " + id);
        Integer budgetIndex = new Integer(id);
        Element budgetElement = budgetElementsMap.get(budgetIndex);
        if (budgetElement == null) budgetElement = readBudgetElement(id);
        return budgetElement;
    }

    /** Implementing the interface. Returns a Budget object with a given
   *  id. 
   * @throws IllegalArgumentException if there is no budget with the given id
   * @throws IOException if there are IO errors.
   */
    public synchronized Budget getBudget(int id) throws IOException {
        try {
            Element budgetElement = getBudgetElement(id);
            Budget budget = Budget.getInstance(budgetElement);
            return budget;
        } catch (JDOMException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /** Stores a given budget.
   * @return true when the operation has completed successfully.
   */
    public synchronized boolean storeBudget(Budget budget) {
        int id = budget.getID();
        String name = budget.getName();
        Element budgetElement = budget.getXmlElement();
        Integer budgetIndex = new Integer(id);
        budgetElementsMap.put(budgetIndex, budgetElement);
        dataIndex.registerBudget(id, name);
        return true;
    }

    /** removes a budget.
   * @return true when the operation has completed successfully.
   * @throws IllegalArgumentException if no such budget exists.
   */
    public synchronized boolean removeBudget(int id) {
        if (!dataIndex.isValidBudget(id)) throw new IllegalArgumentException("No budget with id: " + id);
        dataIndex.unregisterBudget(id);
        Integer budgetIndex = new Integer(id);
        budgetElementsMap.remove(budgetIndex);
        File budgetFile = getBudgetFile(id);
        if (budgetFile.exists()) budgetFile.delete();
        return true;
    }

    /** removes a budget.
   * @return true when the operation has completed successfully.
   * @throws IllegalArgumentException if no such budget exists.
   */
    public synchronized boolean removeBudget(Budget budget) {
        return removeBudget(budget.getID());
    }

    /** Reads a JDOM Element containing the account with given account id.
   * @throws IOException
   * @throws IllegalArgumentException if there is no such account.
   */
    private Element readAccountElement(int id) throws IOException, JDOMException {
        if (!dataIndex.isValidAccount(id)) throw new IllegalArgumentException("No account with id: " + id);
        File accountFile = getAccountFile(id);
        InputStream iStream = new BufferedInputStream(new FileInputStream(accountFile));
        SAXBuilder builder = new SAXBuilder();
        Document accountDoc = builder.build(iStream);
        Element accountElement = accountDoc.getRootElement();
        accountDoc.setRootElement(new Element("Empty"));
        Integer accountIndex = new Integer(id);
        accountElementsMap.put(accountIndex, accountElement);
        return accountElement;
    }

    /** Saves a JDOM Element containing the account with given account id. */
    private void saveAccountElement(Element element, int id) throws IOException, JDOMException {
        File accountFile = getAccountFile(id);
        File dir = accountFile.getParentFile();
        dir.mkdirs();
        backupFile(accountFile);
        OutputStream oStream = new BufferedOutputStream(new FileOutputStream(accountFile));
        Document accountDocument = new Document();
        accountDocument.setRootElement(element);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(accountDocument, oStream);
        oStream.close();
        accountDocument.setRootElement(new Element("Empty"));
    }

    /** Returns a JDOM Element containing the account with given id. 
   *  Uses a cached version if it is avalilable. Otherwise the Element
   *  is read from storage.
   * @throws IllegalArgumentException if there is no account with the given id
   */
    private Element getAccountElement(int id) throws IOException, JDOMException {
        if (!dataIndex.isValidAccount(id)) throw new IllegalArgumentException("No account with id: " + id);
        Integer accountIndex = new Integer(id);
        Element accountElement = accountElementsMap.get(accountIndex);
        if (accountElement == null) accountElement = readAccountElement(id);
        return accountElement;
    }

    /** Implementing the interface. Returns a Account object with a given
   *  id. 
   * @throws IllegalArgumentException if there is no account with the given id
   * @throws IOException if there are IO errors.
   */
    public synchronized Account getAccount(int id) throws IOException {
        try {
            Element accountElement = getAccountElement(id);
            Account account = Account.getInstance(accountElement);
            return account;
        } catch (JDOMException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /** Stores a given account.
   * @return true when the operation has completed successfully.
   */
    public synchronized boolean storeAccount(Account account) {
        int id = account.getID();
        String name = account.getName();
        Element accountElement = account.getXmlElement();
        Integer accountIndex = new Integer(id);
        accountElementsMap.put(accountIndex, accountElement);
        dataIndex.registerAccount(id, name);
        return true;
    }

    /** removes a account.
   * @return true when the operation has completed successfully.
   * @throws IllegalArgumentException if no such account exists.
   */
    public synchronized boolean removeAccount(int id) {
        if (!dataIndex.isValidAccount(id)) throw new IllegalArgumentException("No account with id: " + id);
        dataIndex.unregisterAccount(id);
        Integer accountIndex = new Integer(id);
        accountElementsMap.remove(accountIndex);
        File accountFile = getAccountFile(id);
        if (accountFile.exists()) accountFile.delete();
        return true;
    }

    /** removes a account.
   * @return true when the operation has completed successfully.
   * @throws IllegalArgumentException if no such account exists.
   */
    public synchronized boolean removeAccount(Account account) {
        return removeAccount(account.getID());
    }

    /** Reads a JDOM Element containing the user with a given uid.
   * @throws IOException
   * @throws IllegalArgumentException if there is no such user.
   */
    private Element readUserElement(int uid) throws IOException, JDOMException {
        if (!dataIndex.isValidUser(uid)) throw new IllegalArgumentException("No user with uid: " + uid);
        File userFile = getUserFile(uid);
        InputStream iStream = new BufferedInputStream(new FileInputStream(userFile));
        SAXBuilder builder = new SAXBuilder();
        Document userDoc = builder.build(iStream);
        Element userElement = userDoc.getRootElement();
        userDoc.setRootElement(new Element("Empty"));
        Integer userIndex = new Integer(uid);
        userElementsMap.put(userIndex, userElement);
        return userElement;
    }

    /** Saves a JDOM Element containing the user with given uid. */
    private void saveUserElement(Element element, int uid) throws IOException, JDOMException {
        File userFile = getUserFile(uid);
        File dir = userFile.getParentFile();
        dir.mkdirs();
        backupFile(userFile);
        OutputStream oStream = new BufferedOutputStream(new FileOutputStream(userFile));
        Document userDocument = new Document();
        userDocument.setRootElement(element);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(userDocument, oStream);
        oStream.close();
        userDocument.setRootElement(new Element("Empty"));
    }

    /** Returns a JDOM Element containing the user with a given uid. 
   *  Uses a cached version if it is avalilable. Otherwise the Element
   *  is read from storage.
   * @throws IllegalArgumentException if there is no user with the given id
   */
    private Element getUserElement(int uid) throws IOException, JDOMException {
        if (!dataIndex.isValidUser(uid)) throw new IllegalArgumentException("No user with uid: " + uid);
        Integer userIndex = new Integer(uid);
        Element userElement = userElementsMap.get(userIndex);
        if (userElement == null) userElement = readUserElement(uid);
        return userElement;
    }

    /** Implementing the interface. Returns a User object with a given
   *  uid. 
   * @throws IllegalArgumentException if there is no user with the given uid
   * @throws IOException if there are IO errors.
   */
    public synchronized User getUser(int uid) throws IOException {
        try {
            Element userElement = getUserElement(uid);
            User user = User.getInstance(userElement);
            return user;
        } catch (JDOMException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /** Stores a given user.
   * @return true when the operation has completed successfully.
   */
    public synchronized boolean storeUser(User user) {
        int uid = user.getUID();
        String name = user.getName();
        Element userElement = user.getXmlElement();
        Integer userIndex = new Integer(uid);
        userElementsMap.put(userIndex, userElement);
        dataIndex.registerUser(uid, name);
        return true;
    }

    /** removes a user.
  * @return true when the operation has completed successfully.
  * @throws IllegalArgumentException if no such user exists.
  */
    public synchronized boolean removeUser(int uid) {
        if (!dataIndex.isValidUser(uid)) throw new IllegalArgumentException("No user with uid: " + uid);
        dataIndex.unregisterUser(uid);
        Integer userIndex = new Integer(uid);
        userElementsMap.remove(userIndex);
        File userFile = getUserFile(uid);
        if (userFile.exists()) userFile.delete();
        return true;
    }

    /** removes a user.
   * @return true when the operation has completed successfully.
   * @throws IllegalArgumentException if no such user exists.
   */
    public synchronized boolean removeUser(User user) {
        return removeUser(user.getUID());
    }

    /** Reads a JDOM Element containing the user preferences for a user with a
   * given uid.
   * @throws IOException
   * @throws IllegalArgumentException if there is no such user.
   */
    private Element readUserPreferencesElement(int uid) throws IOException, JDOMException {
        if (!dataIndex.isValidUser(uid) && uid != User.SYSTEM.getUID()) throw new IllegalArgumentException("No user with uid: " + uid);
        File userPreferencesFile = getUserPreferencesFile(uid);
        InputStream iStream = new BufferedInputStream(new FileInputStream(userPreferencesFile));
        SAXBuilder builder = new SAXBuilder();
        Document userDoc = builder.build(iStream);
        Element userPreferencesElement = userDoc.getRootElement();
        userDoc.setRootElement(new Element("Empty"));
        Integer userIndex = new Integer(uid);
        userPreferencesElementsMap.put(userIndex, userPreferencesElement);
        return userPreferencesElement;
    }

    /** Saves a JDOM Element containing the user preferences for a user with 
   * a given uid. */
    private void saveUserPreferencesElement(Element element, int uid) throws IOException, JDOMException {
        File userFile = getUserPreferencesFile(uid);
        File dir = userFile.getParentFile();
        dir.mkdirs();
        backupFile(userFile);
        OutputStream oStream = new BufferedOutputStream(new FileOutputStream(userFile));
        Document userDocument = new Document();
        userDocument.setRootElement(element);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(userDocument, oStream);
        oStream.close();
        userDocument.setRootElement(new Element("Empty"));
    }

    /** Returns a JDOM Element containing the user preferences for the user
   * with a given uid. Uses a cached version if it is avalilable. Otherwise the
   * Element is read from storage.
   * @throws IllegalArgumentException if there is no user with the given id
   */
    private Element getUserPreferencesElement(int uid) throws IOException, JDOMException {
        if (!dataIndex.isValidUser(uid) && uid != User.SYSTEM.getUID()) throw new IllegalArgumentException("No user with uid: " + uid);
        Integer userIndex = new Integer(uid);
        Element userPreferencesElement = userPreferencesElementsMap.get(userIndex);
        if (userPreferencesElement == null) userPreferencesElement = readUserPreferencesElement(uid);
        return userPreferencesElement;
    }

    /** Implementing the interface. Returns a UserPreferences object for the user
   * with a given uid. 
   * @throws IllegalArgumentException if there is no user with the given uid
   * @throws IOException if there are IO errors.
   */
    public synchronized UserPreferences getUserPreferences(int uid) throws IOException {
        try {
            Element userPreferencesElement = getUserPreferencesElement(uid);
            UserPreferences userPreferences = UserPreferences.getInstance(userPreferencesElement);
            return userPreferences;
        } catch (JDOMException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /** Stores the preferences for the user with a given uid.
   * @return true when the operation has completed successfully.
   */
    public synchronized boolean storeUserPreferences(int uid, UserPreferences preferences) {
        Element userPreferencesElement = preferences.getXmlElement();
        userPreferencesElementsMap.put(new Integer(uid), userPreferencesElement);
        return true;
    }

    /** Reads a JDOM Element containing the user preferences for a user with a
   * given uid.
   * @throws IOException
   * @throws IllegalArgumentException if there is no such user.
   */
    private Element readSharedPreferencesElement() throws IOException, JDOMException {
        File sharedPreferencesFile = getSharedPreferencesFile();
        if (!sharedPreferencesFile.exists()) return null;
        InputStream iStream = new BufferedInputStream(new FileInputStream(sharedPreferencesFile));
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(iStream);
        Element element = doc.getRootElement();
        doc.setRootElement(new Element("Empty"));
        return element;
    }

    /** Saves a JDOM Element containing the user preferences for a user with 
   * a given uid. */
    private void saveSharedPreferencesElement(Element element) throws IOException, JDOMException {
        File file = getSharedPreferencesFile();
        File dir = file.getParentFile();
        dir.mkdirs();
        backupFile(file);
        OutputStream oStream = new BufferedOutputStream(new FileOutputStream(file));
        Document userDocument = new Document();
        userDocument.setRootElement(element);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(userDocument, oStream);
        oStream.close();
        userDocument.setRootElement(new Element("Empty"));
    }

    /** Returns the SharedPrefences object used by the program. */
    public synchronized SharedPreferences getSharedPreferences() {
        if (sharedPreferences != null) return sharedPreferences;
        try {
            Element element = readSharedPreferencesElement();
            if (element == null) sharedPreferences = new SharedPreferences(); else sharedPreferences = SharedPreferences.getInstance(element);
        } catch (JDOMException ex) {
            sharedPreferences = new SharedPreferences();
        } catch (IOException ex) {
            sharedPreferences = new SharedPreferences();
        }
        return sharedPreferences;
    }

    private void saveSharedPreferences() throws IOException, JDOMException {
        if (sharedPreferences == null) return;
        Element element = sharedPreferences.getXmlElement();
        saveSharedPreferencesElement(element);
    }

    public synchronized boolean addTransaction(Transaction transaction, int cMonth, int cYear) throws IOException, JDOMException {
        Element monthElement = getMonthElement(cMonth, cYear);
        Month.addTransaction(monthElement, transaction);
        return true;
    }

    public synchronized boolean removeTransaction(long transactionID, int cMonth, int cYear) throws IOException, JDOMException {
        Element monthElement = getMonthElement(cMonth, cYear);
        Month.removeTransaction(monthElement, transactionID);
        return true;
    }

    public synchronized boolean addAllocation(Transaction allocation, int cMonth, int cYear) throws IOException, JDOMException {
        Element monthElement = getMonthElement(cMonth, cYear);
        Month.addAllocation(monthElement, allocation);
        return true;
    }

    public synchronized boolean removeAllocation(long allocationID, int cMonth, int cYear) throws IOException, JDOMException {
        Element monthElement = getMonthElement(cMonth, cYear);
        Month.removeAllocation(monthElement, allocationID);
        return true;
    }

    /** Writes all JDOME elements stored in the cache and the data index
   *  to the permanent storage.
   */
    public synchronized void commit() throws IOException, JDOMException {
        storeIndex();
        Set<Integer> dateSet = monthElementsMap.keySet();
        for (Integer date : dateSet) {
            int cYear = date.intValue() / 100;
            int cMonth = date.intValue() - cYear * 100;
            Element monthElement = monthElementsMap.get(date);
            saveMonthElement(monthElement, cMonth, cYear);
        }
        Set<Integer> idSet = budgetElementsMap.keySet();
        for (Integer id : idSet) {
            int iId = id.intValue();
            Element budgetElement = budgetElementsMap.get(iId);
            saveBudgetElement(budgetElement, iId);
        }
        Set<Integer> accountIdSet = accountElementsMap.keySet();
        for (Integer id : accountIdSet) {
            int iId = id.intValue();
            Element accountElement = accountElementsMap.get(iId);
            saveAccountElement(accountElement, iId);
        }
        Set<Integer> uidSet = userElementsMap.keySet();
        for (Integer uid : uidSet) {
            int iuid = uid.intValue();
            Element userElement = userElementsMap.get(iuid);
            saveUserElement(userElement, iuid);
        }
        uidSet = userPreferencesElementsMap.keySet();
        for (Integer uid : uidSet) {
            int iuid = uid.intValue();
            Element userElement = userPreferencesElementsMap.get(iuid);
            saveUserPreferencesElement(userElement, iuid);
        }
        saveSharedPreferences();
    }

    /** Returns string ID of this instance of the storage interface. */
    public String getIdString() {
        return "Local storage:" + rootDirectory.getAbsolutePath();
    }

    /** Returns a File object that is initialized to point to the xml
   * file corresponding to the given month
   */
    private File getMonthFile(int cMonth, int cYear) {
        String separator = File.separator;
        String yearStr = Integer.toString(cYear);
        String monthStr = (new DateFormater()).formatMonth(cMonth);
        StringBuffer buffer = new StringBuffer();
        buffer.append("months");
        buffer.append(separator);
        buffer.append(yearStr);
        buffer.append(separator);
        buffer.append(monthStr);
        buffer.append(".xml");
        String path = buffer.toString();
        return new File(rootDirectory, path);
    }

    /** Returns a File object that is initialized to point to the xml
   * file corresponing to the Budget with a given id.
   */
    private File getBudgetFile(int id) {
        String separator = File.separator;
        String idStr = Integer.toString(id);
        StringBuffer buffer = new StringBuffer();
        buffer.append("budgets");
        buffer.append(separator);
        buffer.append(idStr);
        buffer.append(".xml");
        String path = buffer.toString();
        return new File(rootDirectory, path);
    }

    /** Returns a File object that is initialized to point to the xml
   * file corresponing to the Account with a given id.
   */
    private File getAccountFile(int id) {
        String separator = File.separator;
        String idStr = Integer.toString(id);
        StringBuffer buffer = new StringBuffer();
        buffer.append("accounts");
        buffer.append(separator);
        buffer.append(idStr);
        buffer.append(".xml");
        String path = buffer.toString();
        return new File(rootDirectory, path);
    }

    /** Returns a File object that is initialized to point to the xml
   * file corresponing to the User with a given uid.
   */
    private File getUserFile(int uid) {
        String separator = File.separator;
        String idStr = Integer.toString(uid);
        StringBuffer buffer = new StringBuffer();
        buffer.append("users");
        buffer.append(separator);
        buffer.append(idStr);
        buffer.append(".xml");
        String path = buffer.toString();
        return new File(rootDirectory, path);
    }

    /** Returns a File object that is initialized to point to the xml
   * file corresponing to the UserPreferences for the user with a given uid.
   */
    private File getUserPreferencesFile(int uid) {
        String separator = File.separator;
        String idStr = Integer.toString(uid);
        StringBuffer buffer = new StringBuffer();
        buffer.append("users");
        buffer.append(separator);
        buffer.append("preferences");
        buffer.append(separator);
        buffer.append(idStr);
        buffer.append(".xml");
        String path = buffer.toString();
        return new File(rootDirectory, path);
    }

    private File getSharedPreferencesFile() {
        return new File(rootDirectory, "shared_preferences.xml");
    }

    /** Creates a new numeric budget id and increases the counter. */
    public int generateBudgetID() {
        return dataIndex.generateBudgetID();
    }

    /** Creates a new numeric account id and increases the counter. */
    public int generateAccountID() {
        return dataIndex.generateAccountID();
    }

    /** Creates a new numeric id for an expense category 
   * and increases the counter. */
    public int generateExpenseCategoryID() {
        return dataIndex.generateExpenseCategoryID();
    }

    /** Creates a new numeric id for an income source 
   * and increases the counter. */
    public int generateIncomeSourceID() {
        return dataIndex.generateIncomeSourceID();
    }

    /** Creates a new uid for a new user 
   * and increases the counter. */
    public int generateUID() {
        return dataIndex.generateUID();
    }

    /** Creates a new numeric transaction id for 
   * and increases the counter. */
    public long generateTransactionID() {
        return dataIndex.generateTransactionID();
    }

    /** Creates a backup of a given file. */
    private void backupFile(File file) throws IOException {
        if (!file.exists()) return;
        if (!file.canRead()) throw new IOException("Cannot read file " + file.getAbsolutePath());
        String backup_file_path = file.getAbsolutePath() + ".bkp";
        File backup_file = new File(backup_file_path);
        if (backup_file.exists()) backup_file.delete();
        backup_file.createNewFile();
        BufferedReader inputStream = null;
        PrintWriter outputStream = null;
        try {
            inputStream = new BufferedReader(new FileReader(file));
            outputStream = new PrintWriter(new FileWriter(backup_file));
            String l;
            while ((l = inputStream.readLine()) != null) outputStream.println(l);
        } finally {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
        }
    }
}
