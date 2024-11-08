import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBHelper {

    public static String db = "dbclubs";

    public static String username = "srm2997";

    public static String passwd = "dbclass";

    public static String host = "SQL09.FREEMYSQL.NET";

    private Connection cn;

    /**
	 * Constructor, initializes Connection object for doing queries later
	 * 
	 * @throws SQLException
	 */
    public DBHelper() throws SQLException {
        cn = DriverManager.getConnection("jdbc:mysql://" + host + "/" + db, username, passwd);
        try {
            Populate(new boolean[] { false, false, false });
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void Formation() throws SQLException {
        Statement sm = cn.createStatement();
        boolean populate[] = new boolean[] { false, false, false };
        ResultSet rs = sm.executeQuery("SHOW TABLES LIKE \"Person\"");
        if (!rs.first()) {
            sm.executeUpdate("CREATE TABLE `Person` ( " + "`uid` INT NOT NULL AUTO_INCREMENT PRIMARY KEY , " + "`first` VARCHAR( 50 ) NOT NULL , " + "`last` VARCHAR( 50 ) NOT NULL , " + "`email` VARCHAR( 50 ) NOT NULL )");
            System.out.println("Person table added");
            populate[0] = true;
        }
        rs = sm.executeQuery("SHOW TABLES LIKE \"Club\"");
        if (!rs.first()) {
            sm.executeUpdate("CREATE TABLE `Club` ( " + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY , " + "`rank` INT NOT NULL , " + "`type` VARCHAR( 50 ) NOT NULL , " + "`name` VARCHAR( 50 ) NOT NULL )");
            System.out.println("Club table added");
            populate[1] = true;
        }
        rs = sm.executeQuery("SHOW TABLES LIKE \"Member_Of\"");
        if (!rs.first()) {
            sm.executeUpdate("CREATE TABLE `dbclubs`.`Member_Of` ( " + "`uid` INT NOT NULL , " + "`id` INT NOT NULL , " + "`position` VARCHAR( 50 ) NOT NULL , " + "`date` DATE NOT NULL , " + "PRIMARY KEY ( `uid` , `id` ) )");
            System.out.println("Member_Of table added");
            populate[2] = true;
        }
        Populate(populate);
    }

    private void Form2() throws SQLException {
        String[] queries = { "drop table if exists Holds_Event;", "drop table if exists Receives_Discipline;", "drop table if exists Member_Of;", "drop table if exists MemberOf;", "drop table if exists Discipline;", "drop table if exists Budget;", "drop table if exists Quarter;", "drop table if exists Event_Timings;", "drop table if exists Event;", "drop table if exists Finances;", "drop table if exists Club;", "drop table if exists Person;", "create table Person(" + "uid int NOT NULL," + "first varchar(255) NOT NULL," + "last varchar(255) NOT NULL," + "email varchar(255) NOT NULL," + "PRIMARY KEY (uid) );", "create table Club(" + "club_id int NOT NULL," + "rank varchar(255) NOT NULL," + "type varchar(255)," + "name varchar(255) NOT NULL," + "PRIMARY KEY (club_id) );", "create table Finances(" + "f_id int NOT NULL," + "club_id int NOT NULL," + "transaction_date date NOT NULL," + "description varchar(500)," + "location varchar(255)," + "amount int NOT NULL," + "PRIMARY KEY (f_id)," + "CONSTRAINT fk_club2 FOREIGN KEY (club_id)" + "REFERENCES CLUB (club_id) );", "create table Event( " + "name varchar(255) NOT NULL," + "location varchar(255)," + "PRIMARY KEY (name) );", "create table Event_Timings(" + "id int NOT NULL," + "event_name varchar(255) NOT NULL," + "start_time timestamp NOT NULL," + "end_time timestamp NOT NULL," + "PRIMARY KEY (id)," + "CONSTRAINT fk_event FOREIGN KEY (event_name)" + "REFERENCES EVENT (name) );", "create table Quarter(" + "quarter_id int NOT NULL," + "start_date date NOT NULL," + "end_date date NOT NULL," + "PRIMARY KEY (quarter_id) );", "create table Budget(" + "id int NOT NULL," + "club_id int NOT NULL," + "quarter_id int NOT NULL," + "used int NOT NULL," + "available int NOT NULL," + "PRIMARY KEY (id)," + "CONSTRAINT fk_club3 FOREIGN KEY (club_id)" + "REFERENCES CLUB (club_id)," + "CONSTRAINT fk_quarter FOREIGN KEY (quarter_id)" + "REFERENCES QUARTER (quarter_id) );", "create table Discipline(" + "code int NOT NULL," + "description varchar(500)," + "punishment varchar(255)," + "PRIMARY KEY (code) );", "create table Member_Of(" + "id int NOT NULL," + "uid int NOT NULL," + "club_id int NOT NULL," + "position varchar(255)," + "join_date date," + "PRIMARY KEY (id)," + "CONSTRAINT fk_s FOREIGN KEY (uid)" + "REFERENCES PERSON (uid)," + "CONSTRAINT fk_club FOREIGN KEY (club_id)" + "REFERENCES CLUB (club_id) );", "create table Receives_Discipline(" + "id int NOT NULL," + "club_id int NOT NULL," + "code int NOT NULL," + "reason varchar(255)," + "expiration date," + "PRIMARY KEY (id)," + "CONSTRAINT fk_club4 FOREIGN KEY (club_id)" + "REFERENCES CLUB (club_id)," + "CONSTRAINT fk_discipline FOREIGN KEY (code)" + "REFERENCES DISCIPLINE (code) );", "create table Holds_Event(" + "id int NOT NULL," + "club_id int NOT NULL," + "event_name varchar(255) NOT NULL," + "cost int NOT NULL," + "PRIMARY KEY (id)," + "CONSTRAINT fk_club5 FOREIGN KEY (club_id)" + "REFERENCES CLUB (club_id)," + "CONSTRAINT fk_event2 FOREIGN KEY (event_name)" + "REFERENCES EVENT (name) );" };
        Statement sm = cn.createStatement();
        for (int i = 0; i < queries.length; i++) {
            System.out.println(queries[i]);
            sm.execute(queries[i]);
        }
    }

    /**
	 * Will check the database for tables, and if they don't exist
	 * it creates them and populates them with dummy data.
	 * @throws SQLException 
	 */
    private void Populate(boolean pop[]) throws SQLException {
        Statement sm = cn.createStatement();
        if (pop[0]) {
            String inserts = "Dummy data: ";
            int inserted = sm.executeUpdate("INSERT INTO Person (uid, first, last, email) VALUES (224001174, 'Sam', 'Milton', 'srm2997@rit.edu')");
            inserts += "Sam Milton: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO Person (uid, first, last, email) VALUES (1, 'Sticky', 'Glazer', 'sfg6126@rit.edu')");
            inserts += ", Sticky Glazer: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO Person (uid, first, last, email) VALUES (2, 'Kyle', 'Marcotte', 'kam9144@rit.edu')");
            inserts += ", Kyle Marcotte: " + inserted;
            System.out.println(inserts);
        }
        if (pop[1]) {
            String inserts = "Dummy data: ";
            int inserted = sm.executeUpdate("INSERT INTO Club (club_id, rank, type, name) VALUES (1, -1, 'Sports', 'Women''s Lacrosse')");
            inserts += "Women's Lacrosse: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO Club (club_id, rank, type, name) VALUES (2, -1, 'Hobbies', 'Electronic Gaming Society')");
            inserts += ", EGS: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO Club (club_id, rank, type, name) VALUES (3, -1, 'Hobbies', 'Juggling')");
            inserts += ", Juggling: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO Club (club_id, rank, type, name) VALUES (4, -1, 'Fine Arts', 'Acting')");
            inserts += ", Acting: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO Club (club_id, rank, type, name) VALUES (5, -1, 'Sports', 'Rock Climbing')");
            inserts += ", Rock Climbing: " + inserted;
            System.out.println(inserts);
        }
        if (pop[2]) {
            String inserts = "Dummy data: ";
            int inserted = sm.executeUpdate("INSERT INTO `Member_Of` (`id`, `uid`, `club_id`, `position`, `join_date`) VALUES ('1', '224001174', '5', 'Crash Mat', '2010-02-15');");
            inserts += "Rock Climbing + Sam Milton: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO `Member_Of` (`id`, `uid`, `club_id`, `position`, `join_date`) VALUES ('2', '1', '2', 'Goomba', '2008-01-01');");
            inserts += ", EGS + Sticky Glazer: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO `Member_Of` (`id`, `uid`, `club_id`, `position`, `join_date`) VALUES ('3', '2', '1', 'Cheer Leader', '2010-09-03');");
            inserts += ", Women's Lacrosse + Kyle Marcotte: " + inserted;
            inserted = sm.executeUpdate("INSERT INTO `Member_Of` (`id`, `uid`, `club_id`, `position`, `join_date`) VALUES ('4', '224001174', '1', 'Spectator', '2010-09-03');");
            inserts += ", Women's Lacrosse + Sam Milton: " + inserted;
            System.out.println(inserts);
        }
    }

    /**
	 * Gets the list of students in the system
	 * 
	 * @return Array of students
	 */
    public StudentInfo[] getStudents() {
        return getStudents(true);
    }

    /**
	 * Adds a student to the system
	 * 
	 * @param uid   Univ. ID of the student
	 * @param first First name of the student
	 * @param last  Last name of the student
	 * @param email Email of the student
	 * @return      Returns true if insert succeeded, false on failure
	 */
    public boolean addStudent(int uid, String first, String last, String email) {
        String query = "INSERT INTO Person (`uid`, `first`, `last`, `email`) VALUES ('" + uid + "', '" + first + "', '" + last + "', '" + email + "')";
        try {
            Statement sm = cn.createStatement();
            sm.executeUpdate(query);
            return true;
        } catch (SQLException e) {
            System.err.println(query + "\n" + e.getMessage());
        }
        return false;
    }

    /**
	 * Removes a student from the system
	 * 
	 * @param uid Univ. ID of the student
	 * @return    Returns true on success, false on failure
	 */
    public boolean removeStudent(int uid) {
        try {
            Statement sm = cn.createStatement();
            sm.executeUpdate("DELETE FROM Member_Of WHERE `uid`=" + uid);
            sm.executeUpdate("DELETE FROM Person WHERE `uid`=" + uid);
            return true;
        } catch (SQLException e) {
        }
        return false;
    }

    /**
	 * Gets the list of students in the system
	 * 
	 * @param first_last
	 *            True to order by first name, false to order by last name
	 * @return Array of student names
	 */
    public StudentInfo[] getStudents(boolean first_last) {
        StudentInfo users[];
        try {
            Statement sm = cn.createStatement();
            ResultSet rs = sm.executeQuery("SELECT uid, first, last, email FROM Person ORDER BY " + (first_last ? "first, last" : "last, first"));
            rs.last();
            users = new StudentInfo[rs.getRow()];
            rs.first();
            do {
                StudentInfo temp = new StudentInfo();
                temp.email = rs.getString("email");
                temp.first = rs.getString("first");
                temp.last = rs.getString("last");
                temp.uid = Integer.parseInt(rs.getString("uid"));
                users[rs.getRow() - 1] = temp;
            } while (rs.next());
        } catch (SQLException e) {
            users = new StudentInfo[0];
            e.printStackTrace();
        }
        return users;
    }

    /**
	 * Gets in depth information for the student with matching uid
	 * 
	 * @param uid
	 *            Student's UID
	 * @return StudentInfo object with specified students id number, or null if
	 *         they didn't exist
	 * @throws SQLException
	 */
    public StudentInfo getStudentInfo(int uid) throws SQLException {
        Statement sm = cn.createStatement();
        ResultSet rs = sm.executeQuery("SELECT * FROM Person WHERE uid=" + uid);
        StudentInfo inf = null;
        if (rs.first()) {
            inf = new StudentInfo();
            inf.first = rs.getString("first");
            inf.last = rs.getString("last");
            inf.email = rs.getString("email");
            inf.uid = Integer.parseInt(rs.getString("uid"));
        }
        return inf;
    }

    /**
	 * Gets a list of clubs
	 * 
	 * @return a 2-D array of clubs. Each club has [0]=id and [1]=name
	 * @throws SQLException
	 */
    public Club[] getClubs() {
        Club clubs[];
        try {
            Statement sm = cn.createStatement();
            ResultSet rs = sm.executeQuery("SELECT club_id, name, type, rank FROM Club ORDER BY name");
            rs.last();
            clubs = new Club[rs.getRow()];
            rs.first();
            do {
                Club temp = new Club();
                temp.name = rs.getString("name");
                temp.id = Integer.parseInt(rs.getString("club_id"));
                temp.rank = rs.getString("rank");
                temp.type = rs.getString("type");
                clubs[rs.getRow() - 1] = temp;
            } while (rs.next());
        } catch (SQLException e) {
            clubs = new Club[0];
            e.printStackTrace();
        }
        return clubs;
    }

    public ClubMember[] getClubMembers(int cid) throws SQLException {
        Statement sm = cn.createStatement();
        ClubMember[] students;
        ResultSet rs = sm.executeQuery("SELECT * FROM Person NATURAL JOIN Member_Of WHERE Member_Of.club_id=" + cid);
        if (rs.last()) {
            students = new ClubMember[rs.getRow()];
            rs.first();
            do {
                ClubMember tmp = new ClubMember();
                tmp.member.first = rs.getString("first");
                tmp.member.last = rs.getString("last");
                tmp.member.email = rs.getString("email");
                tmp.member.uid = Integer.parseInt(rs.getString("uid"));
                tmp.position = rs.getString("position");
                tmp.joined = rs.getString("join_date");
                students[rs.getRow() - 1] = tmp;
            } while (rs.next());
        } else {
            students = new ClubMember[0];
        }
        return students;
    }

    /**
	 * Adds a member to a club
	 * @param uid Univ. ID of the student
	 * @param cid Club id
	 * @return    True on success, false on failure
	 */
    public boolean addMember(int uid, int cid) {
        try {
            Statement sm = cn.createStatement();
            sm.executeUpdate("INSERT INTO Member_Of (`uid`, `club_id`, `join_date`) VALUES ('" + uid + "', '" + cid + "', NOW())");
            return true;
        } catch (SQLException e) {
        }
        return false;
    }

    /**
	 * Adds a member to a club with an optional position
	 * @param uid Univ. ID of the student
	 * @param cid Club id
	 * @throws SQLException if a failure occurs
	 */
    public void addMember(int uid, int cid, String position) throws SQLException {
        Statement sm = cn.createStatement();
        sm.executeUpdate("INSERT INTO Member_Of (`uid`, `club_id`, `join_date`, `position`) VALUES ('" + uid + "', '" + cid + "', NOW(), '" + position + "');");
    }

    /**
	 * Removes a member from a club
	 * 
	 * @param uid    Univ. ID of the student
	 * @param clubId Club's ID
	 * @return       True on success, false on failure
	 */
    public boolean removeMember(int uid, int clubId) {
        try {
            Statement sm = cn.createStatement();
            sm.executeUpdate("DELETE FROM Member_Of WHERE `uid`=" + uid + " AND `club_id`=" + clubId);
            return true;
        } catch (SQLException e) {
        }
        return false;
    }

    /**
	 * Get finances
	 * @param clubid - clubs id
	 * @throws SQLException if the query fails for any reason
	 */
    public Finance[] getFinances(int clubid) throws SQLException {
        Statement sm = cn.createStatement();
        Finance[] finances;
        ResultSet rs = sm.executeQuery("SELECT transaction_date, description, location, amount FROM Finance WHERE club_id=" + clubid + ";");
        if (rs.last()) {
            finances = new Finance[rs.getRow()];
            rs.first();
            do {
                Finance f = new Finance();
                f.date = rs.getDate("transaction_date");
                f.description = rs.getString("description");
                f.location = rs.getString("location");
                f.amount = rs.getBigDecimal("amount");
                finances[rs.getRow() - 1] = f;
            } while (rs.next());
        } else {
            finances = new Finance[0];
        }
        return finances;
    }

    /**
	 * Get Budget
	 * @param clubid - clubs id
	 * @param quarterid - quarter id
	 * @throws SQLException if the query fails for any reason
	 */
    public Budget getBudget(int clubid, int quarterid) throws SQLException {
        Statement sm = cn.createStatement();
        ResultSet rs = sm.executeQuery("SELECT used, available FROM Budget WHERE club_id=" + clubid + " and quarter_id=" + quarterid + ";");
        if (rs.first()) {
            Budget budget = new Budget();
            budget.available = rs.getBigDecimal("available");
            budget.used = rs.getBigDecimal("used");
            return budget;
        } else {
            return null;
        }
    }

    /**
	 * Adds a finance
	 * @throws FinanceException if their isn't room in the budget for the expense
	 * @throws SQLException if the query fails for any reason
	 */
    public void addFinance(int clubid, int quarterid, String date, String desc, String loc, BigDecimal amount) throws FinanceException, SQLException {
        String budgetQuery = "SELECT used, available FROM Budget WHERE club_id=" + clubid + " and quarter_id=" + quarterid + ";";
        String financeUpdate = "INSERT INTO Finance (`club_id`, `transaction_date`, `description`, `location`, `amount`) VALUES ('" + clubid + "', '" + date + "', '" + desc + "', '" + "', '" + loc + "', '" + amount + "');";
        Budget b = new Budget();
        try {
            cn.setAutoCommit(false);
            Statement sm = cn.createStatement();
            ResultSet rs = sm.executeQuery(budgetQuery);
            if (rs.first()) {
                b.used = rs.getBigDecimal(1);
                b.available = rs.getBigDecimal(2);
            } else {
                throw new FinanceException("No budget exists for this club!!");
            }
            if (b.available.compareTo(amount.negate()) >= 0) {
                if (amount.equals(new BigDecimal(0))) ;
                {
                    b.used = b.used.subtract(amount);
                }
                b.available = b.available.add(amount);
                sm = cn.createStatement();
                sm.executeUpdate(financeUpdate);
                sm = cn.createStatement();
                sm.executeUpdate("Update Budget SET used = " + b.used + ", amount = " + b.available + " WHERE club_id=" + clubid + " and quarter_id=" + quarterid + ";");
                cn.commit();
            } else {
                throw new FinanceException("The proposed expenditure is not within the club's budget.");
            }
        } catch (SQLException e) {
            cn.rollback();
            throw e;
        } finally {
            cn.setAutoCommit(true);
        }
    }

    /**
	 * Main method, for testing only
	 * 
	 * @param args
	 *            Command line args, not used
	 */
    public static void main(String args[]) {
        System.out.println("Go!\n");
        DBHelper db;
        try {
            db = new DBHelper();
            System.out.println("getStudentInfo(224001174)");
            StudentInfo stu = db.getStudentInfo(224001174);
            System.out.println(stu.first + " " + stu.last + " (" + stu.uid + "): " + stu.email + "\n");
            System.out.println("getStudents()");
            StudentInfo[] names = db.getStudents();
            for (int i = 0; i < names.length; ++i) {
                System.out.println(names[i].first + "\t" + names[i].last + "\t" + names[i].uid);
            }
            System.out.println("\ngetClubs() -> getClubMembers()");
            Club[] clubs = db.getClubs();
            for (int i = 0; i < clubs.length; ++i) {
                System.out.println(clubs[i].id + "  " + clubs[i].name);
                ClubMember[] members = db.getClubMembers(clubs[i].id);
                for (int k = 0; k < members.length; ++k) {
                    System.out.println("\t" + members[k].member.first + " " + members[k].member.last + ":\t" + members[k].position + "\t on " + members[k].joined);
                }
            }
            System.out.println("Added Sticky to Acting: " + db.addMember(2, 4));
            System.out.println("Removing Sticky from Acting: " + db.removeMember(2, 4));
            System.out.println("Adding Student 'John Doe': " + db.addStudent(-1, "John", "Doe", "none@anywhere.net"));
            System.out.println("Removing 'John Doe': " + db.removeStudent(-1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
