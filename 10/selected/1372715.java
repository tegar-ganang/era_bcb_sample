package courseScheduler;

import java.sql.SQLException;

/**
 *
 * @author david
 */
public class Faculty extends Person {

    /**
     * Constructor that uses Person super class constructor
     * @param newFName value of the first name
     * @param newLName value of the last name
     * @param newDOB value of the DOB
     */
    Faculty(String newFName, String newLName, date newDOB, java.sql.Connection newConn) throws java.sql.SQLException {
        super(newFName, newLName, newDOB);
        conn = newConn;
    }

    /**
     * Constructor that takes an existing person to create a Faculty.
     * @param newPerson existing person.
     */
    Faculty(Person newPerson, java.sql.Connection newConn) throws java.sql.SQLException {
        super(newPerson);
        conn = newConn;
    }

    Faculty(java.sql.Connection newConn, int newID) throws java.sql.SQLException {
        super();
        conn = newConn;
        conn.setAutoCommit(false);
        prepareGetStatement();
        getStmnt.setInt(1, newID);
        java.sql.ResultSet rs = getStmnt.executeQuery();
        if (rs.first()) {
            int count = 0;
            do {
                count++;
            } while (rs.next());
            rs.first();
            if (count == 1) {
                setLName(rs.getString(2));
                setFName(rs.getString(3));
                setMInitial((rs.getString(4)).charAt(0));
                setAddressOne(rs.getString(5));
                setAddressTwo(rs.getString(6));
                setCity(rs.getString(7));
                setState(rs.getString(8));
                setZip(rs.getString(9));
                setPhone(rs.getString(10));
                setEmail(rs.getString(11));
                setOfficeBuilding(rs.getString(12));
                setOfficeNumber(rs.getInt(13));
                setID(newID);
            } else {
                javax.swing.JOptionPane.showMessageDialog(null, "Can not prcess; two entries found");
            }
        }
    }

    public void setFaculty() throws java.sql.SQLException {
        prepareSetStatement();
        getStmnt.setInt(1, idNumber);
        int entriesChanged = getStmnt.executeUpdate();
        if (entriesChanged > 1) {
            javax.swing.JOptionPane.showMessageDialog(null, "Duplicate primary keys");
            conn.rollback();
        } else {
            conn.commit();
        }
    }

    public void insertFaculty() throws java.sql.SQLException {
        prepareInsertStatement();
        int entriesChanged = insertStmnt.executeUpdate();
        if (entriesChanged != 1) {
            javax.swing.JOptionPane.showMessageDialog(null, "Insert failed");
            conn.rollback();
        } else {
            conn.commit();
        }
    }

    int idNumber;

    int officeNumber;

    String officeBuilding;

    String officePhone;

    private java.sql.Connection conn;

    private java.sql.PreparedStatement getStmnt;

    private java.sql.PreparedStatement setStmnt;

    private java.sql.PreparedStatement insertStmnt;

    private void prepareGetStatement() throws SQLException {
        getStmnt = conn.prepareStatement("SELECT id,l_name,f_name,m_initial,address_one,address_two," + "city,state_,zipcode,phone,e_mail,office_building,office_number " + "FROM faculty WHERE id = ?;");
    }

    private void prepareSetStatement() throws SQLException {
        setStmnt = conn.prepareStatement("Update faculty (l_name = \"" + getLName() + "\",f_name = \"" + getFName() + "\",m_initial = '" + getMInitial() + "\",address_one = \"" + getAddressOne() + "\",address_two = \"" + getAddressTwo() + "\"," + "city = \"" + getCity() + "\",state_ = \"" + getState() + "\",zipcode = \"" + getZip() + "\",phone = \"" + getPhone() + "\",e_mail = \"" + getEmail() + "\",office_building = \"" + getOfficeBuilding() + "\",office_number = \"" + getOfficeNumber() + "\"" + " WHERE id = ?;");
    }

    private void prepareInsertStatement() throws SQLException {
        insertStmnt = conn.prepareStatement("ISERT INTO faculty (l_name,f_name,m_initial,address_one," + "address_two,city,state_,zipcode,phone,e_mail,office_building" + "office_number) VALUES (\"" + getLName() + "\",\"" + getFName() + "\",'" + getMInitial() + "',\"" + getAddressOne() + "\",\"" + getAddressTwo() + "\",\"" + getCity() + "\",\"" + getState() + "\",\"" + getZip() + "\",\"" + getOfficeBuilding() + "\"," + getOfficeNumber() + ");");
    }

    /**
     * Class method for setting the value of the ID number.
     * @param newID new value for ID number.
     */
    public final void setID(int newID) {
        idNumber = newID;
    }

    /**
     * Class method for accessing the value of the ID number.
     * @return ID number.
     */
    public int getID() {
        return idNumber;
    }

    /**
     * Class method for setting the office number.
     * @param newOffNum new value for the office number.
     */
    public final void setOfficeNumber(int newOffNum) {
        officeNumber = newOffNum;
    }

    /**
     * Class method for accessing the office number.
     * @return Office number.
     */
    public int getOfficeNumber() {
        return officeNumber;
    }

    /**
     * Class method for setting the name of the office building.
     * @param newBuilding new value for the name of the office building.
     */
    public final void setOfficeBuilding(String newBuilding) {
        officeBuilding = newBuilding;
    }

    /**
     * Class method for accessing the name of the office building.
     * @return Office building.
     */
    public String getOfficeBuilding() {
        return officeBuilding;
    }

    /**
     * Class method that sets the value of office phone
     * @param newPhone new value for office phone
     */
    public final void setOfficePhone(String newPhone) {
        officePhone = newPhone;
    }

    /**
     * Class method used to access the value of the office phone.
     * @return Office phone.
     */
    public String getOfficePhone() {
        return officePhone;
    }

    public final void setConn(java.sql.Connection newConn) {
        conn = newConn;
    }

    public static int getID(java.sql.Connection sConn, String sLName, String sFName) throws java.sql.SQLException {
        java.sql.Statement stmnt;
        java.sql.ResultSet rs;
        stmnt = sConn.createStatement();
        rs = stmnt.executeQuery("SELECT id FROM faculty WHERE " + "l_name LIKE \"" + sLName + "\" AND f_name LIKE " + "\"" + sFName + "\";");
        rs.first();
        return rs.getInt(1);
    }

    public static int getID(java.sql.Connection sConn, String sCoursePrefix, int sCourseNumber, int sSectionNumber) throws java.sql.SQLException {
        java.sql.Statement stmnt;
        java.sql.ResultSet rs;
        stmnt = sConn.createStatement();
        rs = stmnt.executeQuery("SELECT section.professor_id FROM " + "course JOIN section ON section.course_id = " + "course.id WHERE course.number = " + sCourseNumber + " AND section.section_id = " + sSectionNumber + " AND course.type_ LIKE \"" + sCoursePrefix + "\";");
        rs.first();
        return rs.getInt(1);
    }

    public static int getID(java.sql.Connection sConn, String sBuildingName, int sRoomNumber, int sDay, int sTime) throws java.sql.SQLException {
        java.sql.Statement stmnt;
        java.sql.ResultSet rs;
        stmnt = sConn.createStatement();
        rs = stmnt.executeQuery("SELECT section.professor_id FROM " + "(((room JOIN block ON block.room_id = room.id)" + "JOIN date_time ON block.date_time_id = date_time.id)" + "JOIN block_section ON block.id = block_section.block_id)" + "JOIN section ON block_section.section_id = section.id" + "WHERE room.building LIKE \"" + sBuildingName + "\" " + "AND room.number LIKE \"" + sRoomNumber + "\" " + "AND date_time.day = " + sDay + " " + "AND date_time.time = " + sTime + ";");
        rs.first();
        return rs.getInt(1);
    }
}
