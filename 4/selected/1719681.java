package prefwork.datasource;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import prefwork.Attribute;
import prefwork.CommonUtils;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class CSVDataSource implements BasicDataSource {

    protected String name;

    protected int targetAttribute = 2;

    protected int randomizeIndex;

    protected int userIndex = 0;

    protected int objectIndex = 1;

    protected Attribute[] attributes;

    protected Integer userID;

    /**  Set of users*/
    protected List<Integer> users;

    /**  Set of records with ratings*/
    protected List<List<Object>> records;

    /**  Set of records with ratings by users. Records should be ordered by randomize column*/
    protected HashMap<Integer, List<List<Object>>> usersRecords;

    /** Records of current user (null if userID is null) */
    protected List<List<Object>> userRecords;

    /** Current record. */
    protected List<Object> record;

    protected double[] classes = null;

    protected String path;

    Double from, to;

    boolean recordsFromRange;

    boolean loadOnlySpecifiedUsers = false;

    protected char separator = ',';

    protected void loadData() {
        records = new LinkedList<List<Object>>();
        usersRecords = new HashMap<Integer, List<List<Object>>>();
        if (users == null) users = new LinkedList<Integer>();
        try {
            CSVReader reader = new CSVReader(new FileReader(path), separator, '\'');
            CSVWriter writer = new CSVWriter(new FileWriter(path + "out"), separator, '\'');
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                List<Object> record = new ArrayList<Object>();
                int userId = CommonUtils.objectToInteger(nextLine[userIndex]);
                if (loadOnlySpecifiedUsers && !users.contains(userId)) continue;
                if (!usersRecords.containsKey(userId)) {
                    usersRecords.put(userId, new LinkedList<List<Object>>());
                }
                for (int i = 0; i < nextLine.length; i++) {
                    String s = nextLine[i];
                    if (i == userIndex || i == objectIndex) record.add(CommonUtils.objectToInteger(s)); else if (attributes[i].getType() == Attribute.NOMINAL) record.add(s); else if (attributes[i].getType() == Attribute.NUMERICAL) record.add(CommonUtils.objectToDouble(s)); else if (attributes[i].getType() == Attribute.LIST) {
                        String[] values = s.split(",");
                        record.add(Arrays.asList(values));
                    }
                }
                records.add(record);
                writer.writeNext(nextLine);
                usersRecords.get(userId).add(record);
            }
            reader.close();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sortUsersRatings();
    }

    /**
	 * Sorts ratings of users by randomize column.
	 */
    protected void sortUsersRatings() {
        Comparator<List<Object>> comp = new RandomComparator(randomizeIndex);
        for (List<List<Object>> records : usersRecords.values()) {
            Collections.sort(records, comp);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configDataSource(XMLConfiguration config, String section) {
        Configuration dsConf = config.configurationAt(section);
        attributes = CommonUtils.loadAttributes(config, section);
        targetAttribute = dsConf.getInt("targetAttribute");
        if (dsConf.containsKey("classes")) {
            List classesTemp = dsConf.getList("classes");
            classes = new double[classesTemp.size()];
            for (int i = 0; i < classesTemp.size(); i++) {
                classes[i] = Double.parseDouble(classesTemp.get(i).toString());
            }
        }
        this.objectIndex = dsConf.getInt("objectIndex");
        this.randomizeIndex = dsConf.getInt("randomizeIndex");
        this.userIndex = dsConf.getInt("userIndex");
        loadOnlySpecifiedUsers = dsConf.getBoolean("loadOnlySpecifiedUsers");
        if (dsConf.containsKey("usersList")) {
            List usersTemp = dsConf.getList("usersList");
            users = new LinkedList<Integer>();
            for (Object user : usersTemp) users.add(CommonUtils.objectToInteger(user));
        }
        if (dsConf.containsKey("path")) {
            path = dsConf.getString("path");
        }
    }

    @Override
    public void configDriver(XMLConfiguration config, String section) {
        Configuration dbConf = config.configurationAt(section);
        if (dbConf.containsKey("path")) {
            path = dbConf.getString("path");
        }
    }

    @Override
    public void fillRandomValues() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(path), separator, '\"');
            String[] outLine = new String[records.get(0).size()];
            for (List<Object> record : records) {
                for (Object o : record) outLine[record.indexOf(o)] = o.toString();
                outLine[randomizeIndex] = Double.toString(Math.random());
                writer.writeNext(outLine);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Attribute[] getAttributes() {
        return attributes;
    }

    @Override
    public String[] getAttributesNames() {
        String[] names = new String[attributes.length];
        for (int i = 0; i < names.length; i++) names[i] = attributes[i].getName();
        return names;
    }

    @Override
    public double[] getClasses() {
        if (classes != null) return classes;
        List<Double> tempClasses = new ArrayList<Double>();
        try {
            for (List<Object> record : records) {
                if (!tempClasses.contains(record.get(targetAttribute))) tempClasses.add(CommonUtils.objectToDouble(record.get(targetAttribute)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        classes = new double[tempClasses.size()];
        for (int i = 0; i < tempClasses.size(); i++) classes[i] = tempClasses.get(i);
        return classes;
    }

    /**
	 * Returns the current record and advances recordID.
	 * @return The current record.
	 */
    public List<Object> getRecord() {
        List<Object> recordTemp = record;
        advanceRecords();
        return recordTemp;
    }

    /**
	 * Advances to the next record.
	 * @throws Exception 
	 */
    protected void advanceRecords() {
        if (hasNextRecord()) record = userRecords.get(userRecords.indexOf(record) + 1); else record = null;
    }

    @Override
    public Integer getUserId() {
        if (users.size() == 0) return null; else if (userID == null) userID = users.get(0); else if (users.indexOf(userID) + 1 < users.size()) userID = users.get(users.indexOf(userID) + 1); else return null;
        return userID;
    }

    /**
	 * Checks if given random number fits into interval between from and to, or outside this interval, if recordsFromRange is false.
	 * @param d
	 * @return
	 */
    protected boolean satisfiesCondition(Double d) {
        if (recordsFromRange && d < to && d >= from) return true;
        if (!recordsFromRange && (d >= to || d < from)) return true;
        return false;
    }

    @Override
    public boolean hasNextRecord() {
        if (userID != null) {
            while (true) {
                if (record == null) return false;
                if (userRecords.indexOf(record) + 1 == userRecords.size()) return false;
                double randomize = CommonUtils.objectToDouble(record.get(randomizeIndex));
                if (satisfiesCondition(randomize)) return true; else {
                    if (userRecords.indexOf(record) + 1 == userRecords.size()) return false;
                    record = userRecords.get(userRecords.indexOf(record) + 1);
                }
            }
        }
        return false;
    }

    @Override
    public void restart() {
        if (records == null) {
            loadData();
        }
        record = userRecords.get(0);
    }

    @Override
    public void restartUserId() {
        if (records == null) {
            loadData();
        }
        userID = null;
        userRecords = null;
    }

    @Override
    public void setFixedUserId(Integer value) {
        this.userID = value;
        userRecords = usersRecords.get(value);
    }

    @Override
    public void setLimit(Double fromPct, Double toPct, boolean recordsFromRange) {
        this.from = fromPct;
        this.to = toPct;
        this.recordsFromRange = recordsFromRange;
    }

    @Override
    public int getTargetAttribute() {
        return targetAttribute;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setAttributes(Attribute[] attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getRandomColumn() {
        return Integer.toString(randomizeIndex);
    }

    @Override
    public void setRandomColumn(String randomColumn) {
    }
}

class RandomComparator implements Comparator<List<Object>> {

    public RandomComparator(int randomizeIndex) {
        this.randomizeIndex = randomizeIndex;
    }

    protected int randomizeIndex;

    public int compare(List<Object> o1, List<Object> o2) {
        if (o1 == null || o1.get(randomizeIndex) == null) return -1;
        if (o2 == null || o2.get(randomizeIndex) == null) return 1;
        if (o1.get(randomizeIndex) == null) return 1;
        if (o2.get(randomizeIndex) == null) return -1;
        return -Double.compare(CommonUtils.objectToDouble(o1.get(randomizeIndex)), CommonUtils.objectToDouble(o2.get(randomizeIndex)));
    }
}
