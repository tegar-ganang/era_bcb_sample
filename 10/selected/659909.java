package bl.zk.code;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

/**
 * class DB Prepares the information for db-queries and asks them. No errors are
 * handled here - they're passed back to the caller.
 * 
 * @author bihrm
 */
class DB {

    private Connection conn;

    /**
	 * Initialise driver and connection
	 * 
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
    public void init(String driver, String url, String user, String password) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException, FileNotFoundException, IOException {
        Class.forName(driver).newInstance();
        conn = DriverManager.getConnection(url, user, password);
    }

    /**
	 * Close the Connection
	 */
    public void clean() throws SQLException {
        conn.close();
    }

    /**
	 * retrieve a person
	 * 
	 * @param code
	 *            The 0/1-Stream (line 2-4): totally 18 bit
	 * @return The person as an PEntry object
	 */
    public PEntry getPerson(String code) throws SQLException {
        Statement stmt = conn.createStatement();
        PEntry pei = new PEntry();
        ResultSet rset = stmt.executeQuery("SELECT * FROM personal WHERE personalbs = '" + code + "';");
        if (!rset.next()) return null;
        pei.setName(rset.getString("preName") + " " + rset.getString("name"));
        pei.setLang(rset.getString("lang"));
        pei.setPersonalId(rset.getInt("personalnr"));
        pei.setState(rset.getInt("stamppersonalid"));
        if (pei.getState() != 0) {
            rset = stmt.executeQuery("SELECT * FROM stampzk, zeitk, posten, best, sachb, kunde, funcs, images WHERE stampzk.outtime='0' AND " + "stampzk.personalid = '" + pei.getPersonalId() + "' AND " + "stampzk.zeitkid=zeitk.zeitkid AND zeitk.postennr=posten.postennr AND " + "posten.ordernr=best.ordernr AND best.sachbnr=sachb.sachbnr AND " + "sachb.clientnr=kunde.clientnr AND stampzk.funcsid=funcs.funcsid AND " + "posten.zgnr=images.imagesnr AND stampzk.sf='0' ORDER BY stampzk.intime ASC;");
            while (rset.next()) {
                Workitem wi = new Workitem();
                wi.setStampZkId(rset.getInt("stampzkid"));
                wi.setInfoString(rset.getString("firm") + ": " + rset.getString("description") + ": " + rset.getString(pei.getLang()));
                int firstId = rset.getInt("firstid");
                if (firstId == 0) wi.setIntime(rset.getLong("intime")); else {
                    ResultSet zres = conn.createStatement().executeQuery("SELECT intime FROM stampzk WHERE stampzkid='" + firstId + "';");
                    zres.next();
                    wi.setIntime(zres.getLong("intime"));
                }
                pei.addItem(wi);
            }
            rset = stmt.executeQuery("SELECT * FROM stampzk, specialfuncs WHERE stampzk.outtime='0' AND stampzk.personalid = '" + pei.getPersonalId() + "' AND stampzk.zeitkid=specialfuncs.specialfuncsid AND stampzk.sf='1';");
            while (rset.next()) {
                Workitem spec = new Workitem();
                spec.setStampZkId(rset.getInt("stampzkid"));
                spec.setInfoString(rset.getString(pei.getLang()));
                int firstId = rset.getInt("firstid");
                if (firstId == 0) spec.setIntime(rset.getLong("intime")); else {
                    ResultSet zres = conn.createStatement().executeQuery("SELECT intime FROM stampzk WHERE stampzkid='" + firstId + "';");
                    zres.next();
                    spec.setIntime(zres.getLong("intime"));
                }
                spec.setIntime(rset.getLong("intime"));
                pei.addSpecialItem(spec);
            }
        }
        return pei;
    }

    /**
	 * retrieve a Zeitkarte
	 * 
	 * @param key The key the user pressed before scanning
	 * @param code The 0/1-Stream (line 2-4): totally 18 bit
	 * @return The Zeitkarte (incl. worker information)
	 */
    public ZKEntry getZeitkarte(String key, String code) throws SQLException {
        ZKEntry zkei = new ZKEntry();
        PEntry pei = new PEntry();
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT * FROM personal, personalkeymapping WHERE personalkeymapping.keymap='" + key + "' AND personalkeymapping.personalid=personal.personalnr AND personal.stamppersonalid!='0';");
        if (!rset.next()) {
            return null;
        }
        pei.setName(rset.getString("preName") + " " + rset.getString("name"));
        pei.setLang(rset.getString("lang"));
        pei.setPersonalId(rset.getInt("personalnr"));
        zkei.setWorker(pei);
        int decoded = Integer.parseInt(code, 2);
        rset = stmt.executeQuery("SELECT firm, intnr, zeitkid, description, images.zgnr as imageszgnr FROM zeitk, posten, best, sachb, kunde, images WHERE " + "zeitk.zeitkid='" + decoded + "' AND zeitk.postennr=posten.postennr AND " + "posten.ordernr=best.ordernr AND best.sachbnr=sachb.sachbnr AND " + "sachb.clientnr=kunde.clientnr AND posten.zgnr=images.imagesnr;");
        if (!rset.next()) {
            zkei.setCustomer(null);
            return zkei;
        }
        zkei.setCustomer(rset.getString("firm"));
        zkei.setDescription(rset.getString("imageszgnr") + "  (" + rset.getString("description") + ")");
        zkei.setIntern(rset.getInt("intnr"));
        zkei.setZeitKId(rset.getInt("zeitkid"));
        rset = stmt.executeQuery("SELECT * FROM stampzk, funcs WHERE stampzk.outtime='0' AND " + "stampzk.personalid = '" + pei.getPersonalId() + "' AND stampzk.zeitkid='" + decoded + "' AND stampzk.funcsid=funcs.funcsid AND stampzk.sf='0' ORDER BY number DESC ;");
        int i = 0;
        while (rset.next()) {
            Workitem wi = new Workitem();
            wi.setStampZkId(rset.getInt("stampzkid"));
            wi.setWorkType(rset.getInt("number"));
            wi.setInfoString(rset.getString(pei.getLang()));
            int firstId = rset.getInt("firstid");
            if (firstId == 0) wi.setIntime(rset.getLong("intime")); else {
                ResultSet zres = conn.createStatement().executeQuery("SELECT intime FROM stampzk WHERE stampzkid='" + firstId + "';");
                zres.next();
                wi.setIntime(zres.getLong("intime"));
            }
            zkei.addItem(i, wi);
            i++;
        }
        return zkei;
    }

    public SpecialEntry getSpecial(String key, String code) throws SQLException {
        SpecialEntry spec = new SpecialEntry();
        PEntry pei = new PEntry();
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT * FROM personal, personalkeymapping WHERE personalkeymapping.keymap='" + key + "' AND personalkeymapping.personalid=personal.personalnr AND personal.stamppersonalid!='0';");
        if (!rset.next()) {
            spec.setWorker(null);
            return spec;
        }
        pei.setName(rset.getString("preName") + " " + rset.getString("name"));
        pei.setLang(rset.getString("lang"));
        pei.setPersonalId(rset.getInt("personalnr"));
        spec.setWorker(pei);
        int decoded = Integer.parseInt(code, 2);
        rset = stmt.executeQuery("SELECT * FROM specialfuncs WHERE specialfuncsid='" + decoded + "';");
        if (!rset.next()) {
            spec.setDescription(null);
            return spec;
        }
        spec.setDescription(rset.getString(pei.getLang()));
        spec.setSpecialId(rset.getInt("specialid"));
        rset = stmt.executeQuery("SELECT * FROM stampzk, specialfuncs WHERE stampzk.outtime='0' AND " + "stampzk.personalid='" + pei.getPersonalId() + "' AND stampzk.zeitkid'" + decoded + "' AND stampzk.zeitkid=specialfuncs.specialfuncsid AND stampzk.sf='1';");
        if (!rset.next()) return null;
        spec.setStampZkId(rset.getInt("stampzkid"));
        int firstId = rset.getInt("firstid");
        if (firstId == 0) spec.setIntime(rset.getLong("intime")); else {
            ResultSet zres = conn.createStatement().executeQuery("SELECT intime FROM stampzk WHERE stampzkid='" + firstId + "';");
            zres.next();
            spec.setIntime(zres.getLong("intime"));
        }
        return spec;
    }

    /**
	 * Close the items of the list
	 * 
	 * @param zke
	 *            Info about the Zeitkarte
	 * @param toRemove
	 *            List with the Workitems to remove
	 * @param toOpen
	 * 			  List with the Workitems to open
	 */
    public void openAndClose(ZKEntry zke, LinkedList toOpen, LinkedList toRemove) throws SQLException {
        conn.setAutoCommit(false);
        try {
            Statement stm = conn.createStatement();
            ResultSet rset = stm.executeQuery("SELECT now();");
            rset.next();
            Timestamp now = rset.getTimestamp("now()");
            for (int i = 0; i < toRemove.size(); i++) {
                Workitem wi = (Workitem) toRemove.get(i);
                rset = stm.executeQuery("SELECT intime, part FROM stampzk WHERE stampzkid = '" + wi.getStampZkId() + "';");
                rset.next();
                long diff = now.getTime() - rset.getLong("intime");
                float diffp = diff * rset.getFloat("part");
                stm.executeUpdate("UPDATE stampzk SET outtime='" + now.getTime() + "', diff='" + diff + "', diffp='" + diffp + "' WHERE stampzkid='" + wi.getStampZkId() + "';");
            }
            rset = stm.executeQuery("SELECT COUNT(*) FROM stampzk WHERE personalid='" + zke.getWorker().getPersonalId() + "' AND outtime='0';");
            rset.next();
            int count = rset.getInt("COUNT(*)") + toOpen.size();
            rset = stm.executeQuery("SELECT * FROM stampzk WHERE personalid='" + zke.getWorker().getPersonalId() + "' AND outtime='0';");
            while (rset.next()) {
                long diff = now.getTime() - rset.getLong("intime");
                float diffp = diff * rset.getFloat("part");
                int firstId = rset.getInt("firstid");
                if (firstId == 0) firstId = rset.getInt("stampzkid");
                Statement ust = conn.createStatement();
                ust.executeUpdate("UPDATE stampzk SET outtime='" + now.getTime() + "', diff='" + diff + "', diffp='" + diffp + "' WHERE stampzkid='" + rset.getInt("stampzkid") + "';");
                ust.executeUpdate("INSERT INTO stampzk SET zeitkid='" + rset.getInt("zeitkid") + "', personalid='" + zke.getWorker().getPersonalId() + "', funcsid='" + rset.getInt("funcsid") + "', part='" + (float) 1f / count + "', intime='" + now.getTime() + "', firstid='" + firstId + "';");
            }
            for (int i = 0; i < toOpen.size(); i++) {
                stm.executeUpdate("INSERT INTO stampzk SET zeitkid='" + zke.getZeitKId() + "', personalid='" + zke.getWorker().getPersonalId() + "', intime='" + now.getTime() + "', funcsid='" + ((Workitem) toOpen.get(i)).getWorkType() + "', part='" + (float) 1f / count + "';");
            }
        } catch (SQLException sqle) {
            conn.rollback();
            conn.setAutoCommit(true);
            throw sqle;
        }
        conn.commit();
        conn.setAutoCommit(true);
    }

    /**
	 * Change the persons state in DB (checked-in/out)
	 * 
	 * @param pe The Person
	 */
    public float stampPerson(PEntry pe) throws SQLException {
        conn.setAutoCommit(false);
        float result;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rset = stmt.executeQuery("SELECT now();");
            rset.next();
            Timestamp now = rset.getTimestamp("now()");
            Calendar cal = new GregorianCalendar();
            cal.setTime(now);
            if (pe.getState() != 0) {
                for (int i = 0; i < pe.getOpenItems().size(); i++) {
                    Workitem wi = (Workitem) pe.getOpenItems().get(i);
                    long diff = now.getTime() - wi.getIntime();
                    float diffp = diff * (float) 1f / pe.getOpenItems().size();
                    stmt.executeUpdate("UPDATE stampzk SET outtime='" + now.getTime() + "', diff='" + diff + "', diffp='" + diffp + "' WHERE stampzkid='" + wi.getStampZkId() + "';");
                }
                rset = stmt.executeQuery("SELECT intime FROM stamppersonal WHERE stamppersonalid='" + pe.getState() + "';");
                rset.next();
                long inDate = rset.getLong("intime");
                long diff = (now.getTime() - inDate);
                stmt.executeUpdate("UPDATE stamppersonal SET outtime='" + now.getTime() + "', diff='" + diff + "' WHERE stamppersonalid='" + pe.getState() + "';");
                stmt.executeUpdate("UPDATE personal SET stamppersonalid='0' WHERE personalnr='" + pe.getPersonalId() + "';");
                stmt.executeUpdate("UPDATE personalyearworktime SET worktime=worktime+" + (float) diff / 3600000f + " WHERE year=" + cal.get(Calendar.YEAR) + " AND personalid='" + pe.getPersonalId() + "';");
                rset = stmt.executeQuery("SELECT SUM(diff) AS twt FROM stamppersonal WHERE personalid='" + pe.getPersonalId() + "' AND datum='" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "';");
                rset.next();
                result = (float) rset.getInt("twt") / 3600000f;
            } else {
                stmt.executeUpdate("INSERT INTO stamppersonal SET personalid='" + pe.getPersonalId() + "', intime='" + now.getTime() + "', datum='" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "';");
                rset = stmt.executeQuery("SELECT stamppersonalid FROM stamppersonal WHERE personalid='" + pe.getPersonalId() + "' AND outtime='0' ORDER BY stamppersonalid DESC LIMIT 1;");
                rset.next();
                int sppid = rset.getInt("stamppersonalid");
                stmt.executeUpdate("UPDATE personal SET stamppersonalid='" + sppid + "' WHERE personalnr='" + pe.getPersonalId() + "';");
                Calendar yest = new GregorianCalendar();
                yest.setTime(now);
                yest.add(Calendar.DAY_OF_YEAR, -1);
                rset = stmt.executeQuery("SELECT SUM(diff) AS twt FROM stamppersonal WHERE personalid='" + pe.getPersonalId() + "' AND datum='" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "';");
                rset.next();
                float today = (float) rset.getInt("twt") / 3600000f;
                rset = stmt.executeQuery("SELECT worktime FROM personalyearworktime WHERE personalid='" + pe.getPersonalId() + "' AND year='" + cal.get(Calendar.YEAR) + "';");
                rset.next();
                float ist = rset.getFloat("worktime") - today;
                rset = stmt.executeQuery("SELECT duetime FROM dueworktime WHERE datum='" + yest.get(Calendar.YEAR) + "-" + (yest.get(Calendar.MONTH) + 1) + "-" + yest.get(Calendar.DAY_OF_MONTH) + "' AND personalid='" + pe.getPersonalId() + "';");
                rset.next();
                result = ist - rset.getFloat("duetime");
            }
        } catch (SQLException sqle) {
            conn.rollback();
            conn.setAutoCommit(true);
            throw sqle;
        }
        conn.commit();
        conn.setAutoCommit(true);
        return result;
    }

    public void openOrCloseSpecial(SpecialEntry spec, boolean open) throws SQLException {
        conn.setAutoCommit(false);
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT NOW();");
        rset.next();
        Timestamp now = rset.getTimestamp("NOW()");
        Calendar cal = new GregorianCalendar();
        cal.setTime(now);
        if (!open) {
            rset = stmt.executeQuery("SELECT intime, part FROM stampzk WHERE stampzkid = '" + spec.getStampZkId() + "';");
            rset.next();
            long diff = now.getTime() - rset.getLong("intime");
            float diffp = diff * rset.getFloat("part");
            stmt.executeUpdate("UPDATE stampzk SET outtime='" + now.getTime() + "', diff='" + diff + "', diffp='" + diffp + "' WHERE stampzkid='" + spec.getStampZkId() + "';");
        }
        rset = stmt.executeQuery("SELECT COUNT(*) FROM stampzk WHERE personalid='" + spec.getWorker().getPersonalId() + "' AND outtime='0';");
        rset.next();
        int count = rset.getInt("COUNT(*)") + (open ? 1 : 0);
        rset = stmt.executeQuery("SELECT * FROM stampzk WHERE personalid='" + spec.getWorker().getPersonalId() + "' AND outtime='0';");
        while (rset.next()) {
            long diff = now.getTime() - rset.getLong("intime");
            float diffp = diff * rset.getFloat("part");
            int firstId = rset.getInt("firstid");
            if (firstId == 0) firstId = rset.getInt("stampzkid");
            Statement ust = conn.createStatement();
            ust.executeUpdate("UPDATE stampzk SET outtime='" + now.getTime() + "', diff='" + diff + "', diffp='" + diffp + "' WHERE stampzkid='" + rset.getInt("stampzkid") + "';");
            ust.executeUpdate("INSERT INTO stampzk SET zeitkid='" + rset.getInt("zeitkid") + "', personalid='" + spec.getWorker().getPersonalId() + "', funcsid='" + rset.getInt("funcsid") + "', part='" + (float) 1f / count + "', intime='" + now.getTime() + "', firstid='" + firstId + "';");
        }
        if (open) {
            stmt.executeUpdate("INSERT INTO stampzk SET zeitkid='" + spec.getSpecialId() + "', personalid='" + spec.getWorker().getPersonalId() + "', intime='" + now.getTime() + "' AND part='" + (float) 1f / count + "';");
        }
        conn.commit();
        conn.setAutoCommit(true);
    }

    public String getInfoString(PEntry pei, String workType) throws SQLException {
        ResultSet rset = conn.createStatement().executeQuery("SELECT " + pei.getLang() + " FROM funcs WHERE number = '" + workType + "';");
        rset.next();
        return rset.getString(pei.getLang());
    }

    public String[][] getWorktypes(String lang) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT COUNT(*) FROM funcs;");
        rset.next();
        int nOfF = rset.getInt("COUNT(*)");
        String[][] result = new String[nOfF][2];
        rset = stmt.executeQuery("SELECT number, " + lang + " FROM funcs;");
        int i = 0;
        while (rset.next()) {
            int number = rset.getInt("number");
            result[i][0] = (number < 10 ? " " : "") + number;
            result[i][1] = rset.getString(lang);
            i++;
        }
        return result;
    }

    public boolean wtExists(String wt) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT funcsid FROM funcs WHERE number='" + wt + "';");
        return rset.next();
    }

    public boolean alive() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rset = stmt.executeQuery("SELECT NOW()");
            return rset.next();
        } catch (SQLException sqle) {
            return false;
        }
    }
}
