package userDB;

import java.io.*;
import java.sql.*;
import java.util.*;

public class UserDBImplementation implements UserDBInterface {

    private static UserDBImplementation _instance = null;

    public static UserDBImplementation getUserDB() throws UserDBException {
        if (null == _instance) {
            _instance = new UserDBImplementation();
        }
        return _instance;
    }

    public Vector getInputProviderNames() throws UserDBException {
        try {
            rs = queryInputProviderNamesPS.executeQuery();
            Vector v = new Vector();
            while (rs.next()) v.add((String) rs.getString("name"));
            return (v);
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    public Vector getUserNames() throws UserDBException {
        try {
            rs = queryUserNamesPS.executeQuery();
            Vector v = new Vector();
            while (rs.next()) v.add((String) rs.getString("name"));
            return (v);
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    public Vector getUserNamesByRep() throws UserDBException {
        try {
            rs = queryUserNamesByRepPS.executeQuery();
            Vector v = new Vector();
            while (rs.next()) v.add((String) rs.getString("name"));
            return (v);
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    public void register(String name) throws UserDBException, SQLException {
        con.setAutoCommit(false);
        try {
            queryRegUserPS.setString(1, name);
            rs = queryRegUserPS.executeQuery();
            _found = rs.next();
            if (!_found) {
                insertNewRegUserPS.setString(1, name);
                if (insertNewRegUserPS.executeUpdate() != 1) {
                    throw new UserDBException("insertRegNewUser had no effect");
                }
            } else {
                throw new UserDBException("no such user found");
            }
        } catch (SQLException sqle) {
            con.rollback();
            con.setAutoCommit(true);
            throw new UserDBException(sqle.getMessage());
        }
        con.commit();
        con.setAutoCommit(true);
    }

    public boolean getNospam(String name) throws UserDBException {
        try {
            queryRegUserPS.setString(1, name);
            rs = queryRegUserPS.executeQuery();
            return ((boolean) rs.next());
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    public void unregister(String name) throws NoSuchUserException, UserDBException, SQLException {
        try {
            deleteRegUserPS.setString(1, name);
            if (deleteRegUserPS.executeUpdate() != 1) {
                throw new NoSuchUserException("deleteUser had no effect");
            }
        } catch (SQLException sqle) {
            throw new NoSuchUserException(sqle.getMessage());
        }
    }

    protected UserDBImplementation() throws UserDBException {
        try {
            Class.forName("org.gjt.mm.mysql.Driver");
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserDBException(e.getMessage());
        }
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost/newreps?user=RepBot&password=wrong-");
            deleteUserPS = con.prepareStatement("DELETE FROM users WHERE name = ?");
            deleteRegUserPS = con.prepareStatement("DELETE FROM registered WHERE name = ?");
            deleteInputProviderPS = con.prepareStatement("DELETE FROM inputproviders WHERE name = ?");
            dumpInputProvidersPS = con.prepareStatement("SELECT * FROM inputproviders");
            dumpUsersPS = con.prepareStatement("SELECT * FROM users");
            dumpRegisteredUsersPS = con.prepareStatement("SELECT * FROM registered");
            insertNewInputProviderPS = con.prepareStatement("INSERT INTO inputproviders " + "SET name = ?, " + "experience = ?, " + "ncomplaints = 0, " + "nvouches = 0 ");
            insertNewUserPS = con.prepareStatement("INSERT INTO users " + "SET name = ?, " + "complainers = ',,,,,,,,,,,', " + "vouchers= ',,,,,,,,,,,', " + "reputation = 0 ");
            insertNewRegUserPS = con.prepareStatement("INSERT INTO registered " + "SET name = ?, " + "nospam= 'Y' ");
            queryRegUserPS = con.prepareStatement("SELECT * FROM registered " + "WHERE name = ?");
            queryExperiencePS = con.prepareStatement("SELECT experience FROM inputproviders " + "WHERE name = ?");
            queryComplainersPS = con.prepareStatement("SELECT complainers FROM users WHERE name = ?");
            queryVouchersPS = con.prepareStatement("SELECT vouchers FROM users WHERE name = ?");
            queryComplaintsPS = con.prepareStatement("SELECT name FROM users WHERE complainers LIKE ?");
            queryVouchesPS = con.prepareStatement("SELECT name FROM users WHERE vouchers LIKE ?");
            queryNComplaintsPS = con.prepareStatement("SELECT ncomplaints FROM inputproviders WHERE name = ?");
            queryNVouchesPS = con.prepareStatement("SELECT nvouches FROM inputproviders WHERE name = ?");
            queryReputationPS = con.prepareStatement("SELECT reputation FROM users WHERE name = ?");
            queryInputProviderNamesPS = con.prepareStatement("SELECT name FROM inputproviders");
            queryUserNamesPS = con.prepareStatement("SELECT name FROM users ORDER BY name");
            queryUserNamesByRepPS = con.prepareStatement("SELECT name FROM users ORDER BY reputation");
            queryUserPS = con.prepareStatement("SELECT name, complainers, vouchers, reputation FROM users WHERE name = ?");
            queryInputProviderPS = con.prepareStatement("SELECT name, experience, ncomplaints, nvouches FROM " + "inputproviders WHERE name = ?");
            updateReputationPS = con.prepareStatement("UPDATE users SET reputation = ? WHERE name = ?");
            updateExperiencePS = con.prepareStatement("UPDATE inputproviders SET experience = ? WHERE name = ?");
            updateComplainersPS = con.prepareStatement("UPDATE users SET complainers = ? WHERE name = ?");
            updateVouchersPS = con.prepareStatement("UPDATE users SET vouchers = ? WHERE name = ?");
            updateNComplaintsPS = con.prepareStatement("UPDATE inputproviders SET ncomplaints = ? WHERE name = ?");
            updateNVouchesPS = con.prepareStatement("UPDATE inputproviders SET nvouches = ? WHERE name = ?");
            currentUser = new UserRecord(LISTSIZE, this);
            currentInputProvider = new InputProviderRecord();
        } catch (Exception e) {
            throw new UserDBException(e.getMessage());
        }
    }

    public static void main(String args[]) {
        try {
            UserDBImplementation userDB = new UserDBImplementation();
            String inputLine;
            BufferedReader bin = new BufferedReader(new InputStreamReader(System.in));
            String cmd;
            String user;
            while ((inputLine = bin.readLine()) != null) {
                System.out.println(inputLine);
                if ('#' != inputLine.charAt(0)) {
                    StringTokenizer tokens = new StringTokenizer(inputLine);
                    try {
                        cmd = tokens.nextToken();
                        if (cmd.compareTo("complain") == 0) {
                            userDB.complain(tokens.nextToken(), Integer.parseInt(tokens.nextToken()), tokens.nextToken());
                        } else if (cmd.compareTo("list") == 0) {
                            String _tempUser = tokens.nextToken();
                            try {
                                System.out.println("complainers:" + userDB.getComplainers(_tempUser));
                                System.out.println("vouchers:" + userDB.getVouchers(_tempUser));
                            } catch (NoSuchUserException nsue) {
                                System.out.println(_tempUser + "not in USER table");
                            }
                            System.out.println("complaints:" + userDB.getComplaints(_tempUser));
                            System.out.println("vouches:" + userDB.getVouches(_tempUser));
                        } else if (cmd.compareTo("vouch") == 0) {
                            userDB.vouch(tokens.nextToken(), Integer.parseInt(tokens.nextToken()), tokens.nextToken());
                        } else if (cmd.compareTo("register") == 0) {
                            userDB.register(tokens.nextToken());
                        } else if (cmd.compareTo("unregister") == 0) {
                            userDB.unregister(tokens.nextToken());
                        } else if (cmd.compareTo("withdraw") == 0) {
                            userDB.withdraw(tokens.nextToken(), Integer.parseInt(tokens.nextToken()), tokens.nextToken());
                        } else if (cmd.compareTo("getReputation") == 0) {
                            user = tokens.nextToken();
                            System.out.println("Reputation for user " + user + " = " + userDB.getReputation(user));
                        } else if (cmd.compareTo("dump") == 0) {
                            System.out.println(userDB.dump());
                        }
                    } catch (NoSuchElementException tokene) {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            userDB.shutdown();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public String getComplainers(String user) throws NoSuchUserException, UserDBException {
        try {
            queryComplainersPS.setString(1, user);
            rs = queryComplainersPS.executeQuery();
            if (rs.next()) {
                return rs.getString("complainers");
            } else {
                throw new NoSuchUserException(user);
            }
        } catch (Exception e) {
            throw new UserDBException(e.getMessage());
        }
    }

    public String getVouchers(String user) throws NoSuchUserException, UserDBException {
        try {
            queryVouchersPS.setString(1, user);
            rs = queryVouchersPS.executeQuery();
            if (rs.next()) {
                return rs.getString("vouchers");
            } else {
                throw new NoSuchUserException(user);
            }
        } catch (Exception e) {
            throw new UserDBException(e.getMessage());
        }
    }

    public String getComplaints(String inputProvider) throws UserDBException {
        try {
            StringBuffer sb = new StringBuffer();
            queryComplaintsPS.setString(1, "%," + inputProvider + ",%");
            rs = queryComplaintsPS.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("name") + ",");
            }
            return (sb.toString());
        } catch (Exception e) {
            throw new UserDBException(e.getMessage());
        }
    }

    public String getVouches(String inputProvider) throws UserDBException {
        try {
            StringBuffer sb = new StringBuffer();
            queryVouchesPS.setString(1, "%," + inputProvider + ",%");
            rs = queryVouchesPS.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("name") + ",");
            }
            return (sb.toString());
        } catch (Exception e) {
            throw new UserDBException(e.getMessage());
        }
    }

    public synchronized String dump() throws UserDBException, SQLException {
        StringBuffer ret = new StringBuffer();
        ret.append("USERS:\tNAME\tCOMPLAINERS\tVOUCHERS\tREPUTATION\n");
        rs = dumpUsersPS.executeQuery();
        while (rs.next()) {
            ret.append("\t" + rs.getString("name") + "\t\t\t\t\t");
            ret.append(rs.getInt("reputation") + "\n");
            ret.append("\t\t" + rs.getString("complainers") + "\n");
            ret.append("\t\t\t\t" + rs.getString("vouchers") + "\n");
        }
        ret.append("IPs:\tNAME\tEXPERIENCE\tnComplaints\tnVouches\n");
        rs = dumpInputProvidersPS.executeQuery();
        while (rs.next()) {
            ret.append("\t" + rs.getString("name") + "\t" + rs.getInt("experience") + "\t" + "\t" + rs.getInt("ncomplaints") + "\t" + "\t" + rs.getInt("nvouches") + "\n");
        }
        ret.append("REGs:\tNAME\tNOSPAM\n");
        rs = dumpRegisteredUsersPS.executeQuery();
        while (rs.next()) {
            ret.append("\t" + rs.getString("name") + "\t" + rs.getString("nospam") + "\n");
        }
        return ret.toString();
    }

    public synchronized void backup() throws UserDBException {
    }

    public synchronized int getReputation(String username) throws NoSuchUserException {
        try {
            queryReputationPS.setString(1, username);
            rs = queryReputationPS.executeQuery();
            if (rs.next()) {
                return rs.getInt("reputation");
            } else {
                throw new NoSuchUserException();
            }
        } catch (SQLException sqle) {
            throw new NoSuchUserException();
        }
    }

    public synchronized void complain(String fromWho, int fromExperience, String aboutWho) throws MaxComplaintsException, ExperienceTooLowException, AlreadyComplainedException, UserDBException, SQLException {
        if (fromWho.equals(aboutWho)) throw new UserDBException("can not complain about yourself");
        con.setAutoCommit(false);
        try {
            updateUser(aboutWho);
            updateInputProvider(fromWho, fromExperience);
        } catch (Exception e) {
            con.rollback();
            con.setAutoCommit(true);
            throw new UserDBException(e.getMessage());
        }
        if (currentInputProvider.getNComplaints() >= maxComplaints) {
            throw new MaxComplaintsException(fromWho + " over max complaints");
        }
        if (currentInputProvider.isComplainer(currentUser)) {
            con.rollback();
            con.setAutoCommit(true);
            updateExperiencePS.setInt(1, fromExperience);
            updateExperiencePS.setString(2, fromWho);
            if (updateExperiencePS.executeUpdate() != 1) {
                throw new UserDBException("updateExperience had no effect");
            }
            con.setAutoCommit(false);
            throw new AlreadyComplainedException(fromWho + " already complained about " + aboutWho);
        }
        if (currentInputProvider.isVoucher(currentUser)) {
            this.removeVoucher(fromWho);
        }
        if (null == this.addComplainer(fromWho)) {
            throw new ExperienceTooLowException(fromWho + " does not have more experience than current complainers");
        } else {
            this.updateReputation();
        }
        con.commit();
        con.setAutoCommit(true);
    }

    public synchronized void shutdown() throws UserDBException {
        try {
            deleteUserPS.close();
            deleteInputProviderPS.close();
            insertNewUserPS.close();
            insertNewInputProviderPS.close();
            queryComplainersPS.close();
            queryVouchersPS.close();
            queryExperiencePS.close();
            queryInputProviderPS.close();
            queryNComplaintsPS.close();
            queryNVouchesPS.close();
            queryReputationPS.close();
            queryUserPS.close();
            queryInputProviderNamesPS.close();
            queryUserNamesPS.close();
            queryUserNamesByRepPS.close();
            updateReputationPS.close();
            updateComplainersPS.close();
            updateNComplaintsPS.close();
            updateNVouchesPS.close();
            updateVouchersPS.close();
            con.close();
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    public synchronized void vouch(String fromWho, int fromExperience, String aboutWho) throws MaxVouchesException, ExperienceTooLowException, AlreadyVouchedException, UserDBException, SQLException {
        if (fromWho.equals(aboutWho)) throw new UserDBException("can not vouch for yourself");
        con.setAutoCommit(false);
        try {
            updateUser(aboutWho);
            updateInputProvider(fromWho, fromExperience);
        } catch (Exception e) {
            con.rollback();
            con.setAutoCommit(true);
            throw new UserDBException(e.getMessage());
        }
        if (currentInputProvider.getNVouches() >= maxVouches) {
            throw new MaxVouchesException(fromWho + " over max vouches");
        }
        if (currentInputProvider.isVoucher(currentUser)) {
            con.rollback();
            con.setAutoCommit(true);
            updateExperiencePS.setInt(1, fromExperience);
            updateExperiencePS.setString(2, fromWho);
            if (updateExperiencePS.executeUpdate() != 1) {
                throw new UserDBException("updateExperience had no effect");
            }
            con.setAutoCommit(false);
            throw new AlreadyVouchedException(fromWho + " already vouched for " + aboutWho);
        }
        if (currentInputProvider.isComplainer(currentUser)) {
            this.removeComplainer(fromWho);
        }
        if (null == this.addVoucher(fromWho)) {
            throw new ExperienceTooLowException(fromWho + " does not have more experience than current vouchers");
        } else {
            this.updateReputation();
        }
        con.commit();
        con.setAutoCommit(true);
    }

    public synchronized void withdraw(String fromWho, int fromExperience, String aboutWho) throws UserDBException, SQLException {
        if (fromWho.equals(aboutWho)) throw new UserDBException("can not withdraw from yourself");
        con.setAutoCommit(false);
        try {
            updateUser(aboutWho);
            updateInputProvider(fromWho, fromExperience);
            if (currentInputProvider.isComplainer(currentUser)) {
                System.out.println("***BEFORE: " + currentUser.getComplainers());
                this.removeComplainer(fromWho);
                System.out.println("***AFTER: " + currentUser.getComplainers());
            }
            if (currentInputProvider.isVoucher(currentUser)) {
                System.out.println("trying to remove " + fromWho + " from vouchers list of " + currentUser);
                this.removeVoucher(fromWho);
                System.out.println("\tsuccess");
            }
            if (currentUser.canBeDestroyed()) {
                deleteUser(currentUser.getName());
            } else {
                this.updateReputation();
                System.out.println("\tsuccess on rep recalc");
            }
            System.out.println("getting _nComplaints and _nVouches for " + fromWho);
            queryNComplaintsPS.setString(1, fromWho);
            rs = queryNComplaintsPS.executeQuery();
            rs.next();
            int _nComplaints = rs.getInt("ncomplaints");
            queryNVouchesPS.setString(1, fromWho);
            rs = queryNVouchesPS.executeQuery();
            rs.next();
            int _nVouches = rs.getInt("nvouches");
            System.out.println(fromWho + "_nComplaints=" + _nComplaints + ", _nVouches=" + _nVouches);
            if ((_nComplaints == 0) && (_nVouches == 0)) {
                deleteInputProviderPS.setString(1, fromWho);
                if (deleteInputProviderPS.executeUpdate() != 1) {
                    throw new UserDBException("deleteInputProvider had no effect");
                }
            }
        } catch (Exception e) {
            con.rollback();
            con.setAutoCommit(true);
            throw new UserDBException(e.getMessage());
        }
        con.commit();
        con.setAutoCommit(true);
    }

    public int getExperience(String inputProvider) throws NoSuchUserException {
        try {
            queryExperiencePS.setString(1, inputProvider);
            rs = queryExperiencePS.executeQuery();
            rs.next();
            return rs.getInt("experience");
        } catch (SQLException sqle) {
            throw new NoSuchUserException("No such user " + inputProvider);
        }
    }

    private String addComplainer(String complainer) throws UserDBException {
        try {
            _replaced = currentUser.addComplainer(complainer);
            if (_replaced != null) {
                updateComplainersPS.setString(1, currentUser.getComplainers());
                updateComplainersPS.setString(2, currentUser.getName());
                if (updateComplainersPS.executeUpdate() != 1) {
                    throw new UserDBException("updateComplainers had no effect");
                }
                currentInputProvider.incrementNComplaints();
                updateNComplaintsPS.setInt(1, (currentInputProvider.getNComplaints()));
                updateNComplaintsPS.setString(2, complainer);
                if (updateNComplaintsPS.executeUpdate() != 1) {
                    throw new UserDBException("updateNComplaints had no effect");
                }
                if (!_replaced.equals(complainer)) {
                    queryNComplaintsPS.setString(1, _replaced);
                    rs = queryNComplaintsPS.executeQuery();
                    rs.next();
                    _nComplaints = rs.getInt("ncomplaints") - 1;
                    queryNVouchesPS.setString(1, _replaced);
                    rs = queryNVouchesPS.executeQuery();
                    rs.next();
                    if ((_nComplaints == 0) && (rs.getInt("nvouches") == 0)) {
                        deleteInputProviderPS.setString(1, _replaced);
                        if (deleteInputProviderPS.executeUpdate() != 1) {
                            throw new UserDBException("deleteInputProvider had no effect");
                        }
                    } else {
                        updateNComplaintsPS.setInt(1, _nComplaints);
                        updateNComplaintsPS.setString(2, _replaced);
                        if (updateNComplaintsPS.executeUpdate() != 1) {
                            throw new UserDBException("updateNComplaints had no effect");
                        }
                    }
                }
            }
            return _replaced;
        } catch (Exception e) {
            throw new UserDBException(e.getMessage());
        }
    }

    private String addVoucher(String voucher) throws UserDBException {
        try {
            _replaced = currentUser.addVoucher(voucher);
            if (_replaced != null) {
                updateVouchersPS.setString(1, currentUser.getVouchers());
                updateVouchersPS.setString(2, currentUser.getName());
                if (updateVouchersPS.executeUpdate() != 1) {
                    throw new UserDBException("updateVouchers had no effect");
                }
                currentInputProvider.incrementNVouches();
                updateNVouchesPS.setInt(1, (currentInputProvider.getNVouches()));
                updateNVouchesPS.setString(2, voucher);
                if (updateNVouchesPS.executeUpdate() != 1) {
                    throw new UserDBException("updateNVouches had no effect");
                }
                if (!_replaced.equals(voucher)) {
                    queryNVouchesPS.setString(1, _replaced);
                    rs = queryNVouchesPS.executeQuery();
                    rs.next();
                    _nVouches = rs.getInt("nvouches") - 1;
                    queryNComplaintsPS.setString(1, _replaced);
                    rs = queryNComplaintsPS.executeQuery();
                    rs.next();
                    if ((_nVouches == 0) && (rs.getInt("ncomplaints") == 0)) {
                        deleteInputProviderPS.setString(1, _replaced);
                        if (deleteInputProviderPS.executeUpdate() != 1) {
                            throw new UserDBException("deleteInputProvider had no effect");
                        }
                    } else {
                        updateNVouchesPS.setInt(1, _nVouches);
                        updateNVouchesPS.setString(2, _replaced);
                        if (updateNVouchesPS.executeUpdate() != 1) {
                            throw new UserDBException("updateNVouches had no effect");
                        }
                    }
                }
            }
            return _replaced;
        } catch (Exception e) {
            throw new UserDBException(e.getMessage());
        }
    }

    private int calculateReputation() throws UserDBException {
        try {
            _repAccum = 0;
            String complainers = currentUser.getComplainers();
            if (complainers != null) {
                _listTokenizer = new StringTokenizer(currentUser.getComplainers(), ",");
                _listCount = _listTokenizer.countTokens();
                for (_i = 0; _i < _listCount; _i++) {
                    queryExperiencePS.setString(1, _listTokenizer.nextToken());
                    rs = queryExperiencePS.executeQuery();
                    rs.next();
                    _repAccum -= Math.min(rs.getInt("experience"), 10000);
                }
            }
            String vouchers = currentUser.getVouchers();
            if (vouchers != null) {
                _listTokenizer = new StringTokenizer(currentUser.getVouchers(), ",");
                _listCount = _listTokenizer.countTokens();
                for (_i = 0; _i < _listCount; _i++) {
                    queryExperiencePS.setString(1, _listTokenizer.nextToken());
                    rs = queryExperiencePS.executeQuery();
                    rs.next();
                    _repAccum += Math.min(rs.getInt("experience"), 10000);
                }
            }
            return _repAccum;
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    private void removeComplainer(String complainer) throws UserDBException {
        try {
            currentUser.removeComplainer(complainer);
            updateComplainersPS.setString(1, currentUser.getComplainers());
            updateComplainersPS.setString(2, currentUser.getName());
            if (updateComplainersPS.executeUpdate() != 1) {
                throw new UserDBException("updateComplainers had no effect");
            }
            updateNComplaintsPS.setInt(1, currentInputProvider.getNComplaints() - 1);
            updateNComplaintsPS.setString(2, complainer);
            if (updateNComplaintsPS.executeUpdate() != 1) {
                throw new UserDBException("updateNComplaints had no effect");
            }
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    private void deleteUser(String user) throws UserDBException {
        try {
            deleteUserPS.setString(1, user);
            if (deleteUserPS.executeUpdate() != 1) {
                throw new UserDBException("deleteUser had no effect");
            }
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    private void removeVoucher(String voucher) throws UserDBException {
        try {
            currentUser.removeVoucher(voucher);
            updateVouchersPS.setString(1, currentUser.getVouchers());
            updateVouchersPS.setString(2, currentUser.getName());
            if (updateVouchersPS.executeUpdate() != 1) {
                throw new UserDBException("updateVouchers had no effect");
            }
            updateNVouchesPS.setInt(1, currentInputProvider.getNVouches() - 1);
            updateNVouchesPS.setString(2, voucher);
            if (updateNVouchesPS.executeUpdate() != 1) {
                throw new UserDBException("updateNVouches had no effect");
            }
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    private void updateInputProvider(String name, int experience) throws UserDBException {
        try {
            queryInputProviderPS.setString(1, name);
            rs = queryInputProviderPS.executeQuery();
            _found = rs.next();
            if (!_found) {
                insertNewInputProviderPS.setString(1, name);
                insertNewInputProviderPS.setInt(2, experience);
                if (insertNewInputProviderPS.executeUpdate() != 1) {
                    throw new UserDBException("insertNewInputProvider had no effect");
                }
                currentInputProvider.setName(name);
                currentInputProvider.setExperience(experience);
                currentInputProvider.setNComplaints(0);
                currentInputProvider.setNVouches(0);
            } else {
                updateExperiencePS.setInt(1, experience);
                updateExperiencePS.setString(2, name);
                if (updateExperiencePS.executeUpdate() != 1) {
                    throw new UserDBException("updateExperience had no effect");
                }
                currentInputProvider.setName(rs.getString("name"));
                currentInputProvider.setExperience(rs.getInt("experience"));
                currentInputProvider.setNComplaints(rs.getInt("ncomplaints"));
                currentInputProvider.setNVouches(rs.getInt("nvouches"));
            }
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    private void updateReputation() throws UserDBException {
        try {
            currentUser.setReputation(this.calculateReputation());
            updateReputationPS.setInt(1, currentUser.getReputation());
            updateReputationPS.setString(2, currentUser.getName());
            if (updateReputationPS.executeUpdate() != 1) {
                throw new UserDBException("updateReputation had no effect");
            }
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    private void updateUser(String name) throws UserDBException {
        try {
            queryUserPS.setString(1, name);
            rs = queryUserPS.executeQuery();
            _found = rs.next();
            if (!_found) {
                insertNewUserPS.setString(1, name);
                if (insertNewUserPS.executeUpdate() != 1) {
                    throw new UserDBException("insertNewUser had no effect");
                }
                currentUser.setName(name);
                currentUser.setComplainers(",,,,,,,,,,,");
                currentUser.setVouchers(",,,,,,,,,,,");
                currentUser.setReputation(0);
            } else {
                currentUser.setName(name);
                currentUser.setComplainers(rs.getString("complainers"));
                currentUser.setVouchers(rs.getString("vouchers"));
                currentUser.setReputation(rs.getInt("reputation"));
            }
        } catch (SQLException sqle) {
            throw new UserDBException(sqle.getMessage());
        }
    }

    public static final int LISTSIZE = 10;

    private static final int maxComplaints = 100;

    private static final int maxVouches = 100;

    private Connection con = null;

    private ResultSet rs = null;

    private UserRecord currentUser;

    private InputProviderRecord currentInputProvider;

    private PreparedStatement deleteUserPS;

    private PreparedStatement deleteRegUserPS;

    private PreparedStatement deleteInputProviderPS;

    private PreparedStatement dumpUsersPS;

    private PreparedStatement dumpInputProvidersPS;

    private PreparedStatement dumpRegisteredUsersPS;

    private PreparedStatement insertNewUserPS;

    private PreparedStatement insertNewRegUserPS;

    private PreparedStatement insertNewInputProviderPS;

    private PreparedStatement queryComplaintsPS;

    private PreparedStatement queryVouchesPS;

    private PreparedStatement queryComplainersPS;

    private PreparedStatement queryVouchersPS;

    private PreparedStatement queryExperiencePS;

    private PreparedStatement queryInputProviderPS;

    private PreparedStatement queryNComplaintsPS;

    private PreparedStatement queryNVouchesPS;

    private PreparedStatement queryReputationPS;

    private PreparedStatement queryUserPS;

    private PreparedStatement queryRegUserPS;

    private PreparedStatement queryUserNamesPS;

    private PreparedStatement queryUserNamesByRepPS;

    private PreparedStatement queryInputProviderNamesPS;

    private PreparedStatement updateExperiencePS;

    private PreparedStatement updateReputationPS;

    private PreparedStatement updateComplainersPS;

    private PreparedStatement updateNComplaintsPS;

    private PreparedStatement updateNVouchesPS;

    private PreparedStatement updateVouchersPS;

    private boolean _found;

    private String _replaced;

    private int _nComplaints;

    private int _nVouches;

    private int _i;

    private StringTokenizer _listTokenizer;

    private int _listCount;

    private int _repAccum;

    private String _tempUser;
}
