package prmss_server;

import helper.*;
import helper.group;
import helper.status;
import helper.user;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import org.h2.tools.RunScript;

/**
 *
 * @author Chan Wei Ann Kevin,Tay Wei Ze Gerald, Nie Zhongyuan
 */
public class CodeManager {

    public static CodeManager getCurrentCM() {
        return currentCM;
    }

    public static void setCurrentCM(CodeManager aCurrentCM) {
        currentCM = aCurrentCM;
    }

    private Connection con;

    private String connDriver = "";

    private String connPath = "";

    private String connUsername = "";

    private String connPassword = "";

    private ServerFrame serverFrame;

    private static CodeManager currentCM;

    private Main mainInstance;

    /** Creates a new instance of CodeManager */
    public CodeManager() {
        mainInstance = Main.getCurrentMain();
        serverFrame = ServerFrame.getCurrentFrame();
        currentCM = this;
        serverFrame.throwMsg("DB>: Starting Database instance...");
        serverFrame.throwMsg("Config>: Starting to parse configuration...");
        ConfigParser confParser = new ConfigParser();
        Config config = confParser.parse();
        this.connDriver = config.getConnDriver();
        this.connPath = config.getConnPath();
        this.connUsername = config.getConnUsername();
        this.connPassword = config.getConnPassword();
        serverFrame.throwMsg("Config>: Configuration parsed DONE...");
        this.startDB();
    }

    public void initCheckDB() {
        if (this.isDefaultTableExisting()) {
            serverFrame.throwMsg("Default table(s) exist...");
        } else {
            serverFrame.throwMsg("Default table(s) NOT exist ! \nSetting up default table(s) begin...");
            this.createDefaultTables("SQL/prmss-table-setup.sql");
            boolean ok = this.isDefaultTableExisting();
            if (ok == true) {
                serverFrame.throwMsg("Default table(s) have been SUCCESSFULLY setup !");
            } else {
                serverFrame.throwMsg("Default table(s) have FAILED setup !");
            }
        }
    }

    /**
     * Database system related
     * runs H2 scripts (H2)
     * @param sqlUrl
     */
    public void runscript(String sqlUrl) {
        try {
            RunScript run = new RunScript();
            run.execute(connPath, connUsername, connPassword, sqlUrl, null, true);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Database system related
     * check if tables are existing (H2)
     * @return
     */
    public boolean isDefaultTableExisting() {
        boolean ok = false;
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC'");
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                if (rs.last() == true) {
                    ok = true;
                }
            }
        } catch (SQLException ex) {
            serverFrame.throwMsg("Tables not existing...");
        }
        return ok;
    }

    /**
     * Database system related
     * Create all table from default script (H2)
     * @param filepath
     */
    public void createDefaultTables(String filepath) {
        File file = new File(filepath);
        if (file.exists()) {
            runscript(file.getAbsolutePath());
        } else {
            serverFrame.throwMsg("Table setup script: " + file.toString() + " ,file is not found !\nFAILED to setup all tables.");
        }
    }

    /**
     * Database system related
     * Drops all tables (H2)
     * @param filepath
     */
    public void dropDefaultTables(String filepath) {
        File file = new File(filepath);
        if (file.exists()) {
            runscript(file.getAbsolutePath());
        } else {
            serverFrame.throwMsg("Table delete script: " + file.toString() + " ,file is not found !\nFAILED to delete all tables.");
        }
    }

    /**
     * Database system related 
     * Deletes the entire DB including users , schema ...etc. (H2)
     * @param filepath
     */
    public void killAll(String filepath) {
        File file = new File(filepath);
        if (file.exists()) {
            runscript(file.getAbsolutePath());
        } else {
            serverFrame.throwMsg("KILL ALL script: " + file.toString() + " ,file is not found !\nFAILED to KILL ALL.");
        }
    }

    /**
     * Database access related
     * Method for checking whether the user is valid
     * @param name
     * @param password
     * @return
     */
    public boolean checkUser(String name, String password) {
        boolean valid = false;
        user myUser = new user();
        try {
            PreparedStatement stmt = getCon().prepareStatement("Select * from user where uname = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    myUser.setUname(rs.getString("uname"));
                    myUser.setUpass(rs.getString("upass"));
                }
                if (name.equals(myUser.getUname()) && password.equals(myUser.getUpass())) {
                    valid = true;
                }
                return valid;
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return valid;
    }

    /**
     * Database access related
     * Method for check whether the group name is valid
     * @param gname
     * @return
     */
    public boolean checkGroup(String gname) {
        boolean valid = false;
        group myGroup = new group();
        try {
            PreparedStatement stmt = getCon().prepareStatement("Select * from grp where gname = ?");
            stmt.setString(1, gname);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    myGroup.setGname(rs.getString("gname"));
                }
                if (gname.equals(myGroup.getGname())) {
                    valid = true;
                }
                return valid;
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return valid;
    }

    /**
     * Database access related
     * Method for check whether the group is inside the status table according to the member data
     * @param uname
     * @param password
     * @param gname
     * @return
     */
    public boolean checkStatusGroup(String uname, String gname) {
        boolean valid = false;
        status myStatus = new status();
        try {
            PreparedStatement stmt = con.prepareStatement("select * from status where uname = ? and gname = ?");
            stmt.setString(1, uname);
            stmt.setString(2, gname);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                valid = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return valid;
    }

    /**
     * Database access related
     * Method for Login
     * @param name
     * @param password
     * @param groupname
     * @return
     */
    public boolean login(String name, String password, String groupname) {
        boolean isUserExist = false;
        boolean isStatusExist = false;
        boolean isLoginOk = false;
        String realName = "";
        String realPass = "";
        String realGrp = "";
        isUserExist = this.checkUser(name, password);
        isStatusExist = this.checkStatusGroup(name, groupname);
        if (isUserExist == true && isStatusExist == true) {
            isLoginOk = true;
        }
        return isLoginOk;
    }

    /**
     * Database access related
     * Method for creating a group
     * @param gname
     * @return
     */
    public boolean createGroup(String gname) {
        boolean groupCreated = false;
        group myGroup = new group();
        group checkgroup = null;
        try {
            System.out.println("createGroup() checking if group exist...");
            PreparedStatement stmt = getCon().prepareStatement("Select gname from grp where gname = ?");
            stmt.setString(1, gname);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                System.out.println("creatGroup() Group not exits... Creating Group...");
                PreparedStatement pstmt = getCon().prepareStatement("insert into grp (gname) values (?)");
                pstmt.setString(1, gname);
                int num = pstmt.executeUpdate();
                if (num == 1) {
                    groupCreated = true;
                    System.out.println("createGroup() Group created...");
                }
            } else {
                System.out.println("createGroup() Group exist - not going to create group...");
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groupCreated;
    }

    /**
     * Database access related
     * Method for joining an existing group
     * @param name
     * @param password
     * @param groupname
     * @param status
     * @return
     */
    public boolean joinGroup(String name, String groupname, String status) {
        boolean isJoinOk = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("Select uname,gname,position from status where uname = ? and gname =? and position = ?");
            stmt.setString(1, name);
            stmt.setString(2, groupname);
            stmt.setString(3, status);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                System.out.println("joinGroup() rs.next()");
                System.out.println("joinGroup() inserting join group...");
                PreparedStatement pstmt = getCon().prepareStatement("insert into status(uname,gname,position) values (?,?,?)");
                pstmt.setString(1, name);
                pstmt.setString(2, groupname);
                pstmt.setString(3, status);
                int num = pstmt.executeUpdate();
                if (num == 1) {
                    System.out.println("joinGroup() OK...");
                    isJoinOk = true;
                }
            } else {
                System.out.println("joinGroup() User already existed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isJoinOk;
    }

    /**
     * Database access related
     * Method for registration of user
     * @param name
     * @param password
     * @param email
     * @param addr
     * @param contactNo
     * @return
     */
    public int register(String name, String password, String email, String addr, String contactNo) {
        int uid = 0;
        int result = -1;
        try {
            getCon().setAutoCommit(false);
            if (!checkUser(name, password)) {
                PreparedStatement pstmt = getCon().prepareStatement("insert into user(uname, upass, uemail, uaddr, ucontact)" + " values (?,?,?,?,?)");
                pstmt.setString(1, name);
                pstmt.setString(2, password);
                pstmt.setString(3, email);
                pstmt.setString(4, addr);
                pstmt.setString(5, contactNo);
                int num = pstmt.executeUpdate();
                if (num == 1) {
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        result = rs.getInt(1);
                    }
                }
            } else {
                result = -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result = -1;
            try {
                System.out.println("Transaction roll back due to errors");
                getCon().rollback();
            } catch (Exception ex) {
            }
        }
        return result;
    }

    /**
     * Database access related
     * Method for viewing the entries in address book
     * @param name
     * @return
     */
    public user getAddress(String name) {
        user myuser = new user();
        try {
            PreparedStatement stmt = con.prepareStatement("Select * from user where uname= ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    myuser.setUid(rs.getInt("uid"));
                    myuser.setUname(rs.getString("uname"));
                    myuser.setUemail(rs.getString("uemail"));
                    myuser.setUaddr(rs.getString("uaddr"));
                    myuser.setUcontact(rs.getString("ucontact"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return myuser;
    }

    /**
     * Database access related
     * Gets a user by uid
     * @param uid
     * @return
     */
    public user getUserById(int uid) {
        user myuser = new user();
        try {
            PreparedStatement stmt = con.prepareStatement("Select * from user where uid= ?");
            stmt.setInt(1, uid);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    myuser.setUid(rs.getInt("uid"));
                    myuser.setUname(rs.getString("uname"));
                    myuser.setUemail(rs.getString("uemail"));
                    myuser.setUaddr(rs.getString("uaddr"));
                    myuser.setUcontact(rs.getString("ucontact"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return myuser;
    }

    /**
     * Database access related
     * Get user list of your group members except your name for your addressbook
     * @param uname
     * @param gname
     * @return
     */
    public ArrayList getUserList(String uname, String gname) {
        ArrayList aList = new ArrayList();
        String username;
        try {
            PreparedStatement stmt = con.prepareStatement("Select uname from status where uname != ? and gname = ? ");
            stmt.setString(1, uname);
            stmt.setString(2, gname);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    username = rs.getString("uname");
                    aList.add(username);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return aList;
    }

    /**
     * Database access related
     * Get information of a particular user
     * @param uName
     * @param groupName
     * @return
     */
    public user getUser(String uName, String groupName) {
        user myuser = new user();
        try {
            PreparedStatement stmt = getCon().prepareStatement("Select * from user where uname = ? ");
            stmt.setString(1, uName);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    myuser.setUid(rs.getInt("uid"));
                    myuser.setUname(rs.getString("uname"));
                    myuser.setUpass(rs.getString("upass"));
                    myuser.setUemail(rs.getString("uemail"));
                    myuser.setUaddr(rs.getString("uaddr"));
                    myuser.setUcontact(rs.getString("ucontact"));
                }
            }
            myuser.setUstat(getUserStat(uName, groupName));
            System.out.println("Get user: " + myuser.getUname() + "," + myuser.getUid() + "," + myuser.getUpass() + "," + myuser.getUstat());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return myuser;
    }

    /**
     * Database access related 
     * Get a user's status
     * @param uname
     * @param grp
     * @return
     */
    public String getUserStat(String uname, String grp) {
        String result = "";
        PreparedStatement stmt1;
        try {
            stmt1 = getCon().prepareStatement("select * from status where uname = ? and gname = ?");
            stmt1.setString(1, uname);
            stmt1.setString(2, grp);
            ResultSet rs1 = stmt1.executeQuery();
            if (rs1 != null) {
                while (rs1.next()) {
                    result = rs1.getString("position");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Get the list of available group
     * @return
     */
    public ArrayList getGroupList() {
        ArrayList result = new ArrayList();
        PreparedStatement stmt;
        try {
            stmt = getCon().prepareStatement("select gname from grp");
            ResultSet rs1 = stmt.executeQuery();
            if (rs1 != null) {
                while (rs1.next()) {
                    result.add(rs1.getString("gname"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Updates a user's information
     * @param user
     * @return
     */
    public boolean updateUser(user user) {
        boolean result = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("update user set uname=?, upass=?,uemail=?,uaddr=?,ucontact=? where uname=?");
            stmt.setString(1, user.getUname());
            stmt.setString(2, user.getUpass());
            stmt.setString(3, user.getUemail());
            stmt.setString(4, user.getUaddr());
            stmt.setString(5, user.getUcontact());
            stmt.setString(6, user.getUname());
            System.out.println("Update user: " + user.getUname() + "," + user.getUpass() + "," + user.getUemail() + "," + user.getUaddr() + "," + user.getUcontact());
            int num = stmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Sends personal messages
     * @param mail
     * @return
     */
    public boolean sendMessage(mail mail) {
        boolean result = false;
        PreparedStatement pstmt;
        try {
            pstmt = getCon().prepareStatement("insert into mail(toid,fromid,pmdatetime,title,content,stat,gname) values (?,?,?,?,?,?,?)");
            pstmt.setString(1, mail.getToid());
            pstmt.setString(2, mail.getFromid());
            pstmt.setString(3, mail.getPmdatetime());
            pstmt.setString(4, mail.getTitle());
            pstmt.setString(5, mail.getContent());
            pstmt.setString(6, mail.getStat());
            pstmt.setString(7, mail.getGname());
            int num = pstmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Gets all personal messages
     * @param name
     * @param group
     * @return
     */
    public ArrayList getAllMsg(String name, String group) {
        System.out.println("Getting mail for user: " + name + ",Group: " + group);
        ArrayList aList = new ArrayList();
        mail mail = null;
        PreparedStatement pstmt;
        try {
            pstmt = getCon().prepareStatement("select * from mail where toid = ? and gname = ?");
            pstmt.setString(1, name);
            pstmt.setString(2, group);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    mail = new mail(rs.getInt("pmid"), rs.getString("title"), rs.getString("fromid"), rs.getString("stat"), rs.getString("pmdatetime"));
                    aList.add(mail);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return aList;
    }

    /**
     * Database access related
     * Deletes personal message
     * @param mid
     * @return
     */
    public boolean deleteMsg(int mid) {
        boolean result = false;
        try {
            PreparedStatement pstmt = getCon().prepareStatement("delete from mail where pmid = ?");
            pstmt.setInt(1, mid);
            int num = pstmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Add new event
     * @param e
     * @return
     */
    public boolean addEvent(event e) {
        boolean result = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("insert into event (event_name,event_date,event_time,status,event_descrition,group_name)VALUES(?,?,?,?,?,?)");
            stmt.setString(1, e.getEname());
            stmt.setString(2, e.getEdate());
            stmt.setString(3, e.getEtime());
            stmt.setString(4, e.getStat());
            stmt.setString(5, e.getDesc());
            stmt.setString(6, e.getGname());
            int num = stmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Delete existing event
     * @param event_id
     * @return
     */
    public boolean deleteEvent(int event_id) {
        boolean result = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("delete from event where event_id=?");
            stmt.setInt(1, event_id);
            int num = stmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Update existing event
     * @param e
     * @return
     */
    public boolean editEvent(event e) {
        boolean result = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("update event set event_name=?,event_date=?, event_time=?,status=?,event_description=?,group_name=? where event_id=?");
            stmt.setString(1, e.getEname());
            stmt.setString(2, e.getEdate());
            stmt.setString(3, e.getEtime());
            stmt.setString(4, e.getStat());
            stmt.setString(5, e.getDesc());
            stmt.setString(6, e.getGname());
            stmt.setInt(7, e.getEid());
            int num = stmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Retrieve existing event by event_id
     * @param event_id
     * @return
     */
    public event retrieveByEvent_id(int event_id) {
        event e = null;
        try {
            PreparedStatement stmt = getCon().prepareStatement("Select * from event where event_id = ?");
            stmt.setInt(1, event_id);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    e = new event(rs.getString("event_name"), rs.getString("event_date"), rs.getString("event_time"), rs.getString("status"), rs.getString("event_descritpion"), rs.getString("group_name"));
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return e;
    }

    /**
     * Database access related
     * retrieve existing event by event_name
     * @param event_name
     * @return
     */
    public event retrieveByEvent_name(String event_name) {
        event e = null;
        try {
            PreparedStatement stmt = getCon().prepareStatement("Select * from event where event_name = ?");
            stmt.setString(1, event_name);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    e = new event(rs.getString("event_name"), rs.getString("event_date"), rs.getString("event_time"), rs.getString("status"), rs.getString("event_description"), rs.getString("group_name"));
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return e;
    }

    /**
     * Database access related
     * Retrieve all events available by group name
     * @param gname
     * @return
     */
    public ArrayList retrieveAllEventsByGroup(String gname) {
        ArrayList eList = new ArrayList();
        event tempEvent;
        try {
            PreparedStatement stmt = getCon().prepareStatement("select * from event where group_name = ?");
            stmt.setString(1, gname);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    tempEvent = new event(rs.getString("event_name"), rs.getString("event_date"), rs.getString("event_time"), rs.getString("status"), rs.getString("event_description"), rs.getString("group_name"));
                    tempEvent.setEid(rs.getInt("event_id"));
                    eList.add(tempEvent);
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return eList;
    }

    /**
     * Database access related
     * retrieve all existing events
     * @return
     */
    public ArrayList retrieveAllEvents() {
        ArrayList eList = new ArrayList();
        event tempEvent;
        try {
            PreparedStatement stmt = getCon().prepareStatement("select * from event");
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    tempEvent = new event(rs.getString("event_name"), rs.getString("event_date"), rs.getString("event_time"), rs.getString("status"), rs.getString("event_description"), rs.getString("group_name"));
                    tempEvent.setEid(rs.getInt("event_id"));
                    eList.add(tempEvent);
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return eList;
    }

    /**
     * Database access related
     * create new Task
     * @param t
     * @return
     */
    public boolean addTask(task t) {
        boolean result = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("insert into task(task_name,task_endDate,task_endTime,task_status,task_description,group_name)VALUES(?,?,?,?,?,?)");
            stmt.setString(1, t.getTname());
            stmt.setString(2, t.getTenddate());
            stmt.setString(3, t.getTendtime());
            stmt.setString(4, t.getStat());
            stmt.setString(5, t.getDesc());
            stmt.setString(6, t.getGname());
            int num = stmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            result = false;
        }
        return result;
    }

    /**
     * Database access related
     * Delete existing task
     * @param task_id
     * @return
     */
    public boolean deleteTask(int task_id) {
        boolean result = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("delete from task where task_id=?");
            stmt.setInt(1, task_id);
            int num = stmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            result = false;
        }
        return result;
    }

    /**
     * Database access related
     * Update the task in the check list
     * @param t
     * @return
     */
    public boolean editTaskCheckList(task t) {
        System.out.println("Editing Task for Check List");
        boolean result = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("update task set task_name=?,task_status=? where task_id=?");
            stmt.setString(1, t.getTname());
            stmt.setString(2, t.getStat());
            stmt.setInt(3, t.getTid());
            int num = stmt.executeUpdate();
            if (num == 1) {
                result = true;
                System.out.println("Edit Task succeed");
            } else {
                System.out.println("Edit Task failed");
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            result = false;
        }
        return result;
    }

    /**
     * Database access related
     * Update existing task
     * @param t
     * @return
     */
    public boolean editTask(task t) {
        boolean result = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("update task set task_name=?,task_endDate=?, task_endTime=?,task_status=?,task_description=?,group_name=? where task_id=?");
            stmt.setString(1, t.getTname());
            stmt.setString(2, t.getTenddate());
            stmt.setString(3, t.getTendtime());
            stmt.setString(4, t.getStat());
            stmt.setString(5, t.getDesc());
            stmt.setString(6, t.getGname());
            stmt.setInt(7, t.getTid());
            int num = stmt.executeUpdate();
            if (num >= 1) {
                result = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            result = false;
        }
        return result;
    }

    /**
     * Database access related
     * Retrieve existing task by task_id
     * @param task_id
     * @return
     */
    public task retrieveByTask_id(int task_id) {
        task t = null;
        try {
            String query = "Select * from task where task_id = ?";
            PreparedStatement stmt = getCon().prepareStatement(query);
            stmt.setInt(1, task_id);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    t = new task(rs.getInt("task_id"), rs.getString("task_name"), rs.getString("task_endTime"), rs.getString("task_endDate"), rs.getString("task_status"), rs.getString("task_description"), rs.getString("group_name"));
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return t;
    }

    /**
     * Database access related
     * Retrieve existing task by task_name
     * @param task_name
     * @return
     */
    public task retrieveByTask_name(String task_name) {
        task t = null;
        try {
            String query = "Select * from task where task_name= ?";
            PreparedStatement stmt = getCon().prepareStatement(query);
            stmt.setString(1, task_name);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    t = new task(rs.getInt("task_id"), rs.getString("task_name"), rs.getString("task_endTime"), rs.getString("task_endDate"), rs.getString("task_status"), rs.getString("task_description"), rs.getString("group_name"));
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return t;
    }

    /**
     * Database access related
     * Retrieve all available task by group name
     * @param gname
     * @return
     */
    public ArrayList retrieveAllTaskByGroup(String gname) {
        ArrayList tList = new ArrayList();
        task t = null;
        try {
            String query = "Select * from task where group_name= ?";
            PreparedStatement stmt = getCon().prepareStatement(query);
            stmt.setString(1, gname);
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    t = new task(rs.getInt("task_id"), rs.getString("task_name"), rs.getString("task_endTime"), rs.getString("task_endDate"), rs.getString("task_status"), rs.getString("task_description"), rs.getString("group_name"));
                    tList.add(t);
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return tList;
    }

    /**
     * Database access related
     * Retrieve all existing tasks
     * @return
     */
    public ArrayList retrieveAllTasks() {
        ArrayList tList = new ArrayList();
        task tempTask;
        int size = 0;
        try {
            PreparedStatement stmt = getCon().prepareStatement("select * from task");
            ResultSet rs = stmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    tempTask = new task(rs.getInt("task_id"), rs.getString("task_name"), rs.getString("task_endTime"), rs.getString("task_endDate"), rs.getString("task_status"), rs.getString("task_description"), rs.getString("group_name"));
                    tList.add(tempTask);
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return tList;
    }

    /**
     * Database access related
     * Get Personal Messgage 
     * @param pmid
     * @return
     */
    public mail getMsg(int pmid) {
        System.out.println("Getting mail pmid: " + pmid);
        mail mail = new mail();
        PreparedStatement pstmt;
        try {
            pstmt = getCon().prepareStatement("select * from mail where pmid = ?");
            pstmt.setInt(1, pmid);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    mail.setTitle(rs.getString("title"));
                    mail.setFromid(rs.getString("fromid"));
                    mail.setToid(rs.getString("toid"));
                    mail.setContent(rs.getString("content"));
                    mail.setPmid(rs.getInt("pmid"));
                    System.out.println("Found mail of pmid: " + pmid);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return mail;
    }

    /**
     * Database access related
     * Set personal message status to read
     * @param pmid
     * @return
     */
    public boolean mailRead(int pmid) {
        boolean result = false;
        try {
            PreparedStatement pstmt = getCon().prepareStatement("update mail set stat=? where pmid = ? ");
            pstmt.setString(1, "read");
            pstmt.setInt(2, pmid);
            int num = pstmt.executeUpdate();
            if (num == 1) {
                result = true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Database access related
     * Gets an individual announcement
     * @param aid
     * @return
     */
    public announcement getAnnouncement(int aid) {
        announcement ann = null;
        try {
            PreparedStatement pstmt = getCon().prepareStatement("select * from announcement where aid = ?");
            pstmt.setInt(1, aid);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    ann = new announcement();
                    ann.setAid(rs.getInt("aid"));
                    ann.setUid(rs.getInt("uid"));
                    ann.setPdatetime(rs.getTimestamp("pdatetime").toString());
                    ann.setTitle(rs.getString("title"));
                    ann.setContent(rs.getString("content"));
                    ann.setStat(rs.getString("stat"));
                    ann.setGname(rs.getString("gname"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ann;
    }

    /**
     * Database access related
     * Gets all announcement for the Group
     * @param gname
     * @return
     */
    public ArrayList getAllAnnouncement(String gname) {
        ArrayList aList = new ArrayList();
        announcement ann;
        try {
            PreparedStatement pstmt = getCon().prepareStatement("select * from announcement where gname = ?");
            pstmt.setString(1, gname);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    ann = new announcement();
                    ann.setAid(rs.getInt("aid"));
                    ann.setUid(rs.getInt("uid"));
                    ann.setPdatetime(rs.getTimestamp("pdatetime").toString());
                    ann.setTitle(rs.getString("title"));
                    ann.setContent(rs.getString("content"));
                    ann.setStat(rs.getString("stat"));
                    ann.setGname(rs.getString("gname"));
                    aList.add(ann);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return aList;
    }

    /**
     * Database access related
     * Creates a new announcement
     * @param uid
     * @param pdatetime
     * @param title
     * @param content
     * @param stat
     * @param gname
     * @return
     */
    public boolean makeAnnocunement(int uid, Timestamp pdatetime, String title, String content, String stat, String gname) {
        boolean ok = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("insert into announcement(uid,pdatetime,title,content,stat,gname)VALUES(?,?,?,?,?,?)");
            stmt.setInt(1, uid);
            stmt.setTimestamp(2, pdatetime);
            stmt.setString(3, title);
            stmt.setString(4, content);
            stmt.setString(5, stat);
            stmt.setString(6, gname);
            int num = stmt.executeUpdate();
            if (num == 1) {
                ok = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ok;
    }

    /**
     * Database access related
     * Updates an existing announcement
     * @param pdatetime
     * @param title
     * @param content
     * @param gname
     * @return
     */
    public boolean updateAnnouncment(Timestamp pdatetime, String title, String content, String gname, int aid) {
        boolean ok = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("update announcment set pdatetime=?,title=?, content=?,gname=? where aid=?");
            stmt.setTimestamp(1, pdatetime);
            stmt.setString(2, title);
            stmt.setString(3, content);
            stmt.setString(4, gname);
            stmt.setInt(5, aid);
            int num = stmt.executeUpdate();
            if (num == 1) {
                ok = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ok;
    }

    /**
     * Deletes an announcement
     * @param aid
     * @return
     */
    public boolean deleteAnnouncement(int aid) {
        boolean ok = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("delete from announcment where aid=?");
            stmt.setInt(1, aid);
            int num = stmt.executeUpdate();
            if (num == 1) {
                ok = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ok;
    }

    /**
     * Database access related
     * sets Status of Announcment
     * @param aid
     * @return
     */
    public boolean setAnnStat(int aid) {
        boolean ok = false;
        return ok;
    }

    /**
     * Database access related
     * Gets a resource
     * @param rid
     * @return
     */
    public resource getResource(int rid) {
        resource rc = null;
        try {
            PreparedStatement pstmt = getCon().prepareStatement("select * from resource where aid = ?");
            pstmt.setInt(1, rid);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    rc = new resource();
                    rc.setRid(rs.getInt("rid"));
                    rc.setRname(rs.getString("rname"));
                    rc.setDesc(rs.getString("desc"));
                    rc.setStat(rs.getString("stat"));
                    rc.setGname(rs.getString("gname"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rc;
    }

    /**
     * Database access related
     * Gets all resource for the Group
     * @param gname
     * @return
     */
    public ArrayList getAllResource(String gname) {
        ArrayList aList = new ArrayList();
        resource rc;
        try {
            PreparedStatement pstmt = getCon().prepareStatement("select * from resource where aid = ?");
            pstmt.setString(1, gname);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    rc = new resource();
                    rc.setRid(rs.getInt("rid"));
                    rc.setRname(rs.getString("rname"));
                    rc.setDesc(rs.getString("desc"));
                    rc.setStat(rs.getString("stat"));
                    rc.setGname(rs.getString("gname"));
                    aList.add(rc);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return aList;
    }

    /**
     * Database access related
     * Adds a resource
     * @param rname
     * @param desc
     * @param stat
     * @param gname
     * @return
     */
    public boolean addResource(String rname, String desc, String stat, String gname) {
        boolean ok = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("insert into resource(rname,desc,stat,gname)VALUES(?,?,?,?)");
            stmt.setString(1, rname);
            stmt.setString(2, desc);
            stmt.setString(3, stat);
            stmt.setString(4, gname);
            int num = stmt.executeUpdate();
            if (num == 1) {
                ok = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ok;
    }

    /**
     * Database access related
     * Updates existing Resource
     * @param rname
     * @param desc
     * @param stat
     * @param gname
     * @param rid
     * @return
     */
    public boolean updateResource(String rname, String desc, String stat, String gname, int rid) {
        boolean ok = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("update resource set rname=?,desc=?, stat=?,gname=? where aid=?");
            stmt.setString(1, rname);
            stmt.setString(2, desc);
            stmt.setString(3, stat);
            stmt.setString(4, gname);
            stmt.setInt(5, rid);
            int num = stmt.executeUpdate();
            if (num == 1) {
                ok = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ok;
    }

    /**
     * Database access related
     * Deletes an existing resource
     * @param rid
     * @return
     */
    public boolean deleteResource(int rid) {
        boolean ok = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("delete from resource where aid=?");
            stmt.setInt(1, rid);
            int num = stmt.executeUpdate();
            if (num == 1) {
                ok = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ok;
    }

    /**
     * Database access related
     * Get allocation of task by UID
     * @param uid
     * @return
     */
    public ArrayList getAllocateByUID(int uid) {
        ArrayList aList = new ArrayList();
        allocate al = null;
        try {
            PreparedStatement pstmt = getCon().prepareStatement("select * from allocate where uid = ?");
            pstmt.setInt(1, uid);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    al = new allocate();
                    al.setUid(rs.getInt("uid"));
                    al.setTid(rs.getInt("tid"));
                    aList.add(al);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return aList;
    }

    /**
     * Database access related
     * Get allocation by TID
     * @param tid
     * @return
     */
    public ArrayList getAllocateByTID(int tid) {
        ArrayList aList = new ArrayList();
        allocate al = null;
        try {
            PreparedStatement pstmt = getCon().prepareStatement("select * from allocate where tid = ?");
            pstmt.setInt(1, tid);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    al = new allocate();
                    al.setUid(rs.getInt("uid"));
                    al.setTid(rs.getInt("tid"));
                    aList.add(al);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return aList;
    }

    /**
     * Database access related
     * Updates Allocate by UID
     * @param updateUid
     * @param updateTid
     * @param originalUid
     * @return
     */
    public boolean updateAllocateByUID(int updateUid, int updateTid, int originalUid) {
        boolean ok = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("update allocate set tid=?,uid=? where tid=?");
            stmt.setInt(1, updateUid);
            stmt.setInt(2, updateTid);
            stmt.setInt(3, originalUid);
            int num = stmt.executeUpdate();
            if (num == 1) {
                ok = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ok;
    }

    /**
     * Database access related
     * Updates Allocate by TID
     * @param updateUid
     * @param updateTid
     * @param originalTid
     * @return
     */
    public boolean updateAllocateByTID(int updateUid, int updateTid, int originalTid) {
        boolean ok = false;
        try {
            PreparedStatement stmt = getCon().prepareStatement("update allocate set tid=?,uid=? where tid=?");
            stmt.setInt(1, updateUid);
            stmt.setInt(2, updateTid);
            stmt.setInt(3, originalTid);
            int num = stmt.executeUpdate();
            if (num == 1) {
                ok = true;
            }
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ok;
    }

    /**
     * Getters and Setters
     */
    public Connection getCon() {
        return con;
    }

    public void setCon(Connection con) {
        this.con = con;
    }

    public String getConnDriver() {
        return connDriver;
    }

    public void setConnDriver(String connDriver) {
        this.connDriver = connDriver;
    }

    public String getConnPath() {
        return connPath;
    }

    public void setConnPath(String connPath) {
        this.connPath = connPath;
    }

    public String getConnUsername() {
        return connUsername;
    }

    public void setConnUsername(String connUsername) {
        this.connUsername = connUsername;
    }

    public String getConnPassword() {
        return connPassword;
    }

    public void setConnPassword(String connPassword) {
        this.connPassword = connPassword;
    }

    public ServerFrame getServerFrame() {
        return serverFrame;
    }

    public void setServerFrame(ServerFrame serverFrame) {
        this.serverFrame = serverFrame;
    }

    /**
     * Database system related
     * Starts a database
     */
    public void startDB() {
        try {
            Class.forName(connDriver);
            con = DriverManager.getConnection(connPath, connUsername, connPassword);
            serverFrame.throwMsg("DB>: JDBC Database Connection has been fetched...");
        } catch (ClassNotFoundException e) {
            serverFrame.throwMsg("DB>: Cannot load JDBC Database Driver");
            e.printStackTrace();
        } catch (SQLException e) {
            serverFrame.throwMsg("DB>: SQLException caught");
            e.printStackTrace();
        }
        if (con == null) {
            serverFrame.throwMsg("DB>: Connection NOT EXIST");
        } else {
            serverFrame.throwMsg("DB>: Connection EXIST");
            initCheckDB();
        }
    }

    /**
     * Database system related
     * Closes database connection
     */
    public void closeDB() {
        try {
            serverFrame.throwMsg("DB>: Database connection closing...");
            con.commit();
            con.close();
            if (con.isClosed()) {
                serverFrame.throwMsg("DB>: Database connection closed SUCCESSFULLY !");
            } else {
                serverFrame.throwMsg("DB>: Database FAILED to close !");
            }
        } catch (SQLException ex) {
            serverFrame.throwMsg("DB>: Error closing database connection !");
            ex.printStackTrace();
        }
    }

    /**
     * Database system related
     * Kills database
     */
    public void killDB() {
        try {
            serverFrame.throwMsg("DB>: Killing database...");
            con.close();
            con = null;
            serverFrame.throwMsg("DB>: Databased KILLED !");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
