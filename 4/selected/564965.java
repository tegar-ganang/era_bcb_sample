package net.emotivecloud.accounting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.emotivecloud.scheduler.simple.Scheduler;
import net.emotivecloud.utils.ovf.EmotiveOVF;
import net.emotivecloud.utils.ovf.OVFWrapper;
import org.hsqldb.cmdline.SqlFile;

public class Accounting extends TimerTask {

    private static DbServerThread serverThread;

    private Connection connection = null;

    private Scheduler scheduler;

    private Timer updateTimer;

    private Properties dbconfig;

    /**
     * If DB server is not running, instantiates it.
     * 
     * @param dbConfig Configuration files
     * @throws SQLException 
     */
    public Accounting(Properties dbConfig, Scheduler scheduler) throws SQLException {
        this.dbconfig = dbConfig;
        this.scheduler = scheduler;
        serverThread = new DbServerThread(dbConfig);
    }

    /**
     * If DB server is not running, runs it.
     * If Database tables are not created, it creates them.
     * Also prepares all the SQL statements.
     */
    public void start() {
        serverThread.start();
        while (!serverThread.isOnline()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Accounting.class.getName()).log(Level.WARNING, "Error when Thread.sleep(): " + ex.getMessage());
            }
        }
        try {
            Class.forName(dbconfig.getProperty("driver"));
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
        try {
            connection = DriverManager.getConnection(dbconfig.getProperty("url"), dbconfig.getProperty("username"), dbconfig.getProperty("password"));
            createDB();
        } catch (SQLException ex) {
            Logger.getLogger(Accounting.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (scheduler != null) {
            updateTimer = new Timer();
            updateTimer.scheduleAtFixedRate(this, UPDATE_PERIOD_MS, UPDATE_PERIOD_MS);
        }
    }

    /**
     * Drops all the tables and creates them newly. WARNING: that will erase all the data in the databas.
     */
    protected void removeAndReinstall() {
        Logger.getLogger(Accounting.class.getName()).log(Level.INFO, "Reinstalling Database");
        dropDB();
        createDB();
    }

    private void dropDB() {
        if (!serverThread.isStarted) {
            start();
        }
        try {
            InputStream sqlStream = getClass().getResourceAsStream("/removeDB.sql");
            File f = File.createTempFile("sql" + System.currentTimeMillis(), ".sql");
            FileOutputStream fos = new FileOutputStream(f);
            byte[] data = new byte[1024];
            int read = -1;
            do {
                read = sqlStream.read(data);
                if (read > 0) {
                    fos.write(data, 0, read);
                }
            } while (read >= 0);
            SqlFile sf = new SqlFile(f);
            sf.setConnection(connection);
            Logger.getLogger(Accounting.class.getName()).log(Level.INFO, "Dropping all tables");
            sf.execute();
            f.delete();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void createDB() {
        if (!serverThread.isStarted) {
            start();
        }
        try {
            InputStream sqlStream = getClass().getResourceAsStream("/createDB.sql");
            File f = File.createTempFile("sql" + System.currentTimeMillis(), ".sql");
            FileOutputStream fos = new FileOutputStream(f);
            byte[] data = new byte[1024];
            int read = -1;
            do {
                read = sqlStream.read(data);
                if (read > 0) {
                    fos.write(data, 0, read);
                }
            } while (read >= 0);
            SqlFile sf = new SqlFile(f);
            sf.setConnection(connection);
            sf.execute();
            f.delete();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Closes all the connections and stops the database server.
     */
    public void close() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Accounting.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    public List<URI> getNodesURI() {
        List<URI> nodes = new ArrayList<URI>();
        try {
            PreparedStatement nodesGet = connection.prepareStatement("SELECT uri FROM Nodes ORDER BY uri;");
            ResultSet rs = nodesGet.executeQuery();
            while (rs.next()) {
                nodes.add(new URI(rs.getString(1)));
            }
            return nodes;
        } catch (Exception ex) {
            Logger.getLogger(Accounting.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void deleteNode(URI uri) {
        try {
            PreparedStatement nodesDelete = connection.prepareStatement("DELETE FROM Nodes WHERE uri=?;");
            nodesDelete.setString(1, uri.toString());
            nodesDelete.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void addNode(URI uri) {
        try {
            PreparedStatement nodesAdd = connection.prepareStatement("INSERT INTO Nodes VALUES (?);");
            nodesAdd.setString(1, uri.toString());
            nodesAdd.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void addVirtualMachine(URI node, EmotiveOVF ovf) {
        try {
            PreparedStatement vmAdd = connection.prepareStatement("INSERT INTO VirtualMachines(vmId, vmName, nodeUri, userName, state) VALUES (?,?,?,?,?);");
            vmAdd.setString(1, ovf.getId());
            vmAdd.setString(2, ovf.getProductProperty(EmotiveOVF.PROPERTYNAME_VM_NAME));
            vmAdd.setString(3, node.toString());
            vmAdd.setString(4, ovf.getCredentials() != null ? ovf.getCredentials().getUserDN() : null);
            vmAdd.setString(5, ovf.getState().toString());
            vmAdd.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
        addVMRecord(ovf);
    }

    public URI getVMNode(String vmId, String userDN) {
        URI node = null;
        try {
            String sql = "SELECT nodeUri FROM VirtualMachines WHERE vmId=?";
            if (userDN != null) {
                sql += " AND userName=?;";
            } else {
                sql += ";";
            }
            PreparedStatement vmGetNodeLocation = connection.prepareStatement(sql);
            vmGetNodeLocation.setString(1, vmId);
            if (userDN != null) {
                vmGetNodeLocation.setString(2, userDN);
            }
            ResultSet rs = vmGetNodeLocation.executeQuery();
            if (rs.next()) {
                node = new URI(rs.getString(1));
            }
            if (rs.next()) {
                Logger.getLogger(Accounting.class.getName()).log(Level.WARNING, "Database inconsistency! Vm " + vmId + " is found in several tables. Returning the first occurence");
            }
        } catch (Exception ex) {
            Logger.getLogger(Accounting.class.getName()).log(Level.WARNING, "Can't get node for VM " + vmId + ": " + ex.getMessage());
        }
        return node;
    }

    public void removeVM(String vmId, String userDN) {
        try {
            String sql = "DELETE FROM VirtualMachines WHERE vmId=?";
            if (userDN != null) {
                sql += " AND userName=?;";
            } else {
                sql += ";";
            }
            PreparedStatement vmRemove = connection.prepareStatement(sql);
            vmRemove.setString(1, vmId);
            if (userDN != null) {
                vmRemove.setString(2, userDN);
            }
            vmRemove.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
        updateVMRecord(vmId);
    }

    public void migrateVM(String vmId, URI newNode, String userDN) {
        try {
            String sql = "UPDATE VirtualMachines SET nodeUri=? WHERE vmId=?";
            if (userDN != null) {
                sql += " AND userName=?;";
            } else {
                sql += ";";
            }
            PreparedStatement vmMigrate = connection.prepareStatement(sql);
            vmMigrate.setString(1, newNode.toString());
            vmMigrate.setString(2, vmId);
            if (userDN != null) {
                vmMigrate.setString(3, userDN);
            }
            vmMigrate.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void addTask(String taskId, String vmId, String status, String jsdl) {
        try {
            Logger.getLogger(Accounting.class.getName()).log(Level.INFO, "Adding : " + taskId + ", " + vmId + ", " + status);
            PreparedStatement taskAdd = connection.prepareStatement("INSERT INTO Tasks(taskId,vmId,state,jsdl) VALUES (?,?,?,?);");
            taskAdd.setString(1, taskId);
            taskAdd.setString(2, vmId);
            taskAdd.setString(3, status);
            taskAdd.setString(4, jsdl);
            taskAdd.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Set<String> getTaskIds(String userDN) {
        Set<String> tasks = new TreeSet<String>();
        try {
            String sql = "SELECT taskId FROM Tasks";
            if (userDN != null) {
                sql += " WHERE userName=?;";
            } else {
                sql += ";";
            }
            PreparedStatement taskGetIds = connection.prepareStatement(sql);
            if (userDN != null) {
                taskGetIds.setString(1, userDN);
            }
            ResultSet rs = taskGetIds.executeQuery();
            while (rs.next()) {
                tasks.add(rs.getString(1));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
        return tasks;
    }

    public String getVmId(String taskId) {
        try {
            PreparedStatement taskGetVmId = connection.prepareStatement("SELECT vmId FROM Tasks WHERE taskId=?;");
            taskGetVmId.setString(1, taskId);
            ResultSet rs = taskGetVmId.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void setTaskState(String taskId, String state) {
        try {
            PreparedStatement taskSetStatus = connection.prepareStatement("UPDATE Tasks SET state=? WHERE taskId=?;");
            taskSetStatus.executeUpdate();
            connection.commit();
            taskSetStatus.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public String getTaskState(String taskId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT state FROM Tasks WHERE taskId=?");
            ResultSet rs = stmt.executeQuery();
            stmt.close();
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return "Unknown";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public String getTaskJSDL(String taskId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT jsdl FROM Tasks WHERE taskId=?");
            ResultSet rs = stmt.executeQuery();
            stmt.close();
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void addVMRecord(OVFWrapper ow) {
        try {
            EmotiveOVF ovf;
            if (ow instanceof EmotiveOVF) {
                ovf = (EmotiveOVF) ow;
            } else {
                ovf = new EmotiveOVF(ow);
            }
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO VMRecords(userName,vmId,cpus,startTime,lastPoll) VALUES (?,?,?,?,?);");
            stmt.setString(1, ovf.getCredentials().getUserName());
            stmt.setString(2, ovf.getId());
            stmt.setFloat(3, ovf.getCPUsNumber());
            Timestamp now = new Timestamp(System.currentTimeMillis());
            stmt.setTimestamp(4, now);
            stmt.setTimestamp(5, now);
            stmt.executeUpdate();
            connection.commit();
            stmt.close();
        } catch (Exception ex) {
            Logger.getLogger(Accounting.class.getName()).log(Level.WARNING, ex.getMessage());
        }
    }

    public void updateVMRecord(String vmId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE VMRecords SET lastPoll=? WHERE vmId=?;");
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, vmId);
            stmt.executeUpdate();
            connection.commit();
            stmt.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Exception e) {
            Logger.getLogger(Accounting.class.getName()).log(Level.WARNING, "Exception finalizing Accounting: " + e.getMessage());
        }
        super.finalize();
    }

    @Override
    public void run() {
        List<OVFWrapper> domains = scheduler.getAllDomains();
        for (OVFWrapper d : domains) {
            updateVMRecord(d.getId());
        }
    }

    private static final long UPDATE_PERIOD_MS = 60 * 1000;
}
