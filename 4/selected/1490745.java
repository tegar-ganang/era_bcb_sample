package org.inigma.waragent.crud;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.inigma.iniglet.utils.QueryHandler;
import org.inigma.utopia.Account;
import org.inigma.utopia.Army;
import org.inigma.utopia.Coordinate;
import org.inigma.utopia.Kingdom;
import org.inigma.utopia.Military;
import org.inigma.utopia.Personality;
import org.inigma.utopia.Province;
import org.inigma.utopia.Race;
import org.inigma.utopia.Rank;
import org.inigma.utopia.Relation;
import org.inigma.utopia.Science;
import org.inigma.utopia.Stance;
import org.inigma.utopia.Survey;

public class AccountCrud extends AbstractCrud {

    protected static final Properties SQL = new Properties();

    protected static QueryHandler<Account> rshAccount = null;

    static {
        try {
            SQL.load(AbstractCrud.class.getResourceAsStream("/org/inigma/waragent/account-sql.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load sql queries");
        }
        try {
            rshAccount = new QueryHandler<Account>("handleAccount", new AccountResultSetHandler());
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initiate account handler", e);
        }
    }

    public static Collection<Account> getAccountList() throws SQLException {
        return rshAccount.selectList(connection, SQL.getProperty("select.account.list"));
    }

    public static Account getActiveAccount() throws SQLException {
        String id = configuration.getString("active.account");
        Account account = rshAccount.select(connection, SQL.getProperty("select.account"), id);
        if (account == null) {
            id = UUID.randomUUID().toString();
            String provinceId = UUID.randomUUID().toString();
            account = new Account(id, provinceId);
            configuration.put("active.account", id);
            AccountCrud crud = new AccountCrud();
            crud.saveAccount();
            AbstractCrud.commit();
        }
        return account;
    }

    public static void setActiveAccount(Account account) {
        configuration.put("active.account", account.getId());
    }

    protected Account account;

    protected QueryHandler<Kingdom> rshKingdom;

    protected QueryHandler<Long> rshLong;

    protected QueryHandler<Military> rshMilitary;

    protected QueryHandler<Army> rshArmy;

    protected QueryHandler<Province> rshProvince;

    protected QueryHandler<Science> rshScience;

    protected QueryHandler<Survey> rshSurvey;

    public AccountCrud() throws SQLException {
        this(getActiveAccount());
    }

    public AccountCrud(Account account) {
        try {
            this.rshKingdom = new QueryHandler<Kingdom>("handleKingdom", this);
            this.rshLong = new QueryHandler<Long>(QueryHandler.HANDLE_LONG);
            this.rshMilitary = new QueryHandler<Military>("handleMilitary", this);
            this.rshArmy = new QueryHandler<Army>("handleArmy", this);
            this.rshProvince = new QueryHandler<Province>("handleProvince", this);
            this.rshScience = new QueryHandler<Science>("handleScience", this);
            this.rshSurvey = new QueryHandler<Survey>("handleSurvey", this);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initialize data handlers");
        }
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    public List<Kingdom> getKingdomSyncList() throws SQLException {
        return rshKingdom.selectList(connection, SQL.getProperty("sync.select.kingdom.list"), account.getId(), account.getLastSync());
    }

    public List<Province> getProvinceSyncList() throws SQLException {
        return rshProvince.selectList(connection, SQL.getProperty("sync.select.province.list"), account.getId(), account.getLastSync(), account.getLastSync(), account.getLastSync(), account.getLastSync());
    }

    public Kingdom getKingdom(Coordinate cord) throws SQLException {
        Kingdom kingdom = rshKingdom.select(connection, SQL.getProperty("select.kingdom"), account.getId(), cord.getKingdom(), cord.getIsland());
        if (kingdom == null) {
            kingdom = new Kingdom(cord);
            saveKingdom(kingdom);
        }
        return kingdom;
    }

    public Kingdom getKingdom(String id) throws SQLException {
        return rshKingdom.select(connection, SQL.getProperty("select.kingdom.by.id"), account.getId(), id);
    }

    public List<Kingdom> getKingdomList() throws SQLException {
        return rshKingdom.selectList(connection, SQL.getProperty("select.kingdom.list"), account.getId());
    }

    public Province getProvince() throws SQLException {
        return getProvince(account.getProvinceId());
    }

    public Province getProvince(String provinceId) throws SQLException {
        return rshProvince.select(connection, SQL.getProperty("select.province.by.id"), provinceId);
    }

    public Province getProvinceByName(String provinceName) throws SQLException {
        return rshProvince.select(connection, SQL.getProperty("select.province.by.name"), provinceName, account.getId());
    }

    public List<Province> getProvinces(String kingdomId) throws SQLException {
        return rshProvince.selectList(connection, SQL.getProperty("select.provinces.by.kingdom.id"), kingdomId);
    }

    public boolean saveAccount() throws SQLException {
        Timestamp lastSync = QueryHandler.toTimestamp(account.getLastSync());
        int rows = QueryHandler.update(connection, SQL.getProperty("update.account"), account.getProvinceId(), lastSync, account.getSyncUrl(), account.getSyncLogin(), account.getSyncPassword(), account.getId());
        if (rows == 0) {
            rows = QueryHandler.insert(connection, SQL.getProperty("insert.account"), account.getId(), account.getProvinceId(), lastSync, account.getSyncUrl(), account.getSyncLogin(), account.getSyncPassword());
        }
        return rows > 0;
    }

    public boolean saveKingdom(Kingdom kingdom) throws SQLException {
        Timestamp lastUpdate = QueryHandler.toTimestamp(kingdom.getLastUpdate());
        int rows = QueryHandler.update(connection, SQL.getProperty("update.kingdom"), lastUpdate, kingdom.getName(), kingdom.getRelation().ordinal(), kingdom.getStance().ordinal(), kingdom.getWarCount(), kingdom.getWarWins(), kingdom.getWarNetworthDiff(), account.getId(), kingdom.getLocation().getKingdom(), kingdom.getLocation().getIsland());
        if (rows == 0) {
            rows = QueryHandler.insert(connection, SQL.getProperty("insert.kingdom"), kingdom.getId(), account.getId(), kingdom.getLocation().getKingdom(), kingdom.getLocation().getIsland(), lastUpdate, kingdom.getName(), kingdom.getRelation().ordinal(), kingdom.getStance().ordinal(), kingdom.getWarCount(), kingdom.getWarWins(), kingdom.getWarNetworthDiff());
        }
        commit();
        return rows > 0;
    }

    public boolean saveProvince(Province province) throws SQLException {
        String gender = "M";
        if (province.isGender()) {
            gender = "F";
        }
        if (province.getKingdomId() == null) {
            Kingdom kingdom = getKingdom(province.getCoordinate());
            province.setKingdomId(kingdom.getId());
        }
        boolean updateMode = true;
        QueryHandler<String> qh = new QueryHandler<String>("handleString");
        String string = qh.select(connection, SQL.getProperty("find.province.by.id"), province.getId());
        if (string == null) {
            updateMode = false;
        }
        if (updateMode) {
            QueryHandler.update(connection, SQL.getProperty("update.province"), province.getKingdomId(), province.getName(), province.getLastUpdate(), province.getAcres(), gender, province.getLeader(), province.getNetworth(), province.getRace().ordinal(), province.getRank().ordinal(), province.getPersonality().ordinal(), province.getPeasants(), province.getGold(), province.getFood(), province.getRunes(), province.getTradeBalance(), province.getThieves(), province.getWizards(), province.getSoldiers(), province.getOffspecs(), province.getDefspecs(), province.getElites(), province.getHorses(), province.getPrisoners(), province.getOffense(), province.getDefense(), province.getId());
        } else {
            QueryHandler.insert(connection, SQL.getProperty("insert.province"), province.getId(), province.getKingdomId(), province.getLastUpdate(), province.getName(), province.getAcres(), gender, province.getLeader(), province.getNetworth(), province.getRace().ordinal(), province.getRank().ordinal(), province.getPersonality().ordinal(), province.getPeasants(), province.getGold(), province.getFood(), province.getRunes(), province.getTradeBalance(), province.getThieves(), province.getWizards(), province.getSoldiers(), province.getOffspecs(), province.getDefspecs(), province.getElites(), province.getHorses(), province.getPrisoners(), province.getOffense(), province.getDefense());
        }
        saveProvinceDetails(province, updateMode);
        Messaging.fireNotification(province);
        commit();
        return true;
    }

    public boolean saveProvince(Province province, boolean minimode) throws SQLException {
        if (minimode) {
            return saveProvinceMini(province);
        }
        return saveProvince(province);
    }

    protected Army handleArmy(ResultSet rs, Connection conn) throws SQLException {
        Army army = new Army();
        army.setId(rs.getString("id"));
        army.setGenerals(rs.getInt("generals"));
        army.setReturnTime(QueryHandler.toCalendar(rs.getTimestamp("eta")));
        army.setSoldiers(rs.getInt("soldiers"));
        army.setOffspecs(rs.getInt("offspecs"));
        army.setDefspecs(rs.getInt("defspecs"));
        army.setElites(rs.getInt("elites"));
        army.setHorses(rs.getInt("horses"));
        army.setSpoils(rs.getInt("spoils"));
        return army;
    }

    protected Kingdom handleKingdom(ResultSet rs, Connection conn) throws SQLException {
        Coordinate coord = new Coordinate(rs.getInt("kingdom"), rs.getInt("island"));
        Kingdom kingdom = new Kingdom(coord);
        kingdom.setId(rs.getString("id"));
        kingdom.setLastUpdate(QueryHandler.toCalendar(rs.getTimestamp("last_update")));
        kingdom.setName(rs.getString("name"));
        kingdom.setRelation(Relation.values()[rs.getInt("relation")]);
        kingdom.setStance(Stance.values()[rs.getInt("stance")]);
        kingdom.setWarCount(rs.getInt("war_count"));
        kingdom.setWarWins(rs.getInt("war_win"));
        kingdom.setWarNetworthDiff(rs.getInt("war_nwdiff"));
        kingdom.setTotalAcres(rshLong.select(conn, SQL.getProperty("select.kingdom.acres"), kingdom.getId()));
        kingdom.setTotalNetworth(rshLong.select(conn, SQL.getProperty("select.kingdom.networth"), kingdom.getId()));
        return kingdom;
    }

    protected Military handleMilitary(ResultSet rs, Connection conn) throws SQLException {
        Military military = new Military();
        military.setId(rs.getString("id"));
        military.setLastUpdate(QueryHandler.toCalendar(rs.getTimestamp("last_update")));
        military.setOffense(rs.getInt("offense"));
        military.setDefense(rs.getInt("defense"));
        military.setRaw("Y".equals(rs.getString("raw_ind")));
        return military;
    }

    protected Province handleProvince(ResultSet rs, Connection conn) throws SQLException {
        Province province = new Province();
        province.setId(rs.getString("id"));
        province.setKingdomId(rs.getString("kingdom_id"));
        if (province.getKingdomId() == null) {
            province.setCoordinate(Coordinate.UNKNOWN);
        } else {
            province.setCoordinate(getKingdom(province.getKingdomId()).getLocation());
        }
        province.setLastUpdate(QueryHandler.toCalendar(rs.getTimestamp("last_update")));
        province.setAcres(rs.getInt("acres"));
        province.setGender("F".equals(rs.getString("gender")));
        province.setName(rs.getString("name"));
        province.setLeader(rs.getString("leader"));
        province.setNetworth(rs.getInt("networth"));
        province.setRace(Race.values()[rs.getInt("race")]);
        province.setRank(Rank.values()[rs.getInt("rank")]);
        province.setPersonality(Personality.values()[rs.getInt("personality")]);
        province.setPeasants(rs.getInt("peasants"));
        province.setGold(rs.getInt("gold"));
        province.setFood(rs.getInt("food"));
        province.setRunes(rs.getInt("runes"));
        province.setTradeBalance(rs.getInt("trade"));
        province.setThieves(rs.getInt("thieves"));
        province.setWizards(rs.getInt("wizards"));
        province.setSoldiers(rs.getInt("soldiers"));
        province.setOffspecs(rs.getInt("offspecs"));
        province.setDefspecs(rs.getInt("defspecs"));
        province.setElites(rs.getInt("elites"));
        province.setHorses(rs.getInt("horses"));
        province.setPrisoners(rs.getInt("prisoners"));
        province.setOffense(rs.getInt("offense"));
        province.setDefense(rs.getInt("defense"));
        Science science = rshScience.select(connection, SQL.getProperty("select.science"), province.getId());
        if (science != null) {
            science.setProvince(province);
            province.setScience(science);
        }
        Survey survey = rshSurvey.select(conn, SQL.getProperty("select.survey"), province.getId());
        if (survey != null) {
            survey.setProvince(province);
            province.setSurvey(survey);
        }
        Military military = rshMilitary.select(connection, SQL.getProperty("select.military"), province.getId());
        if (military != null) {
            military.setProvince(province);
            province.setMilitary(military);
            List<Army> armyList = rshArmy.selectList(connection, SQL.getProperty("select.army"), military.getId());
            for (Army army : armyList) {
                army.setMilitary(military);
            }
            military.setArmies(armyList);
        }
        return province;
    }

    protected Science handleScience(ResultSet rs, Connection conn) throws SQLException {
        Science science = new Science();
        science.setId(rs.getString("id"));
        science.setLastUpdate(QueryHandler.toCalendar(rs.getTimestamp("last_update")));
        science.setAlchemy(rs.getInt("alchemy"));
        science.setTools(rs.getInt("tools"));
        science.setHousing(rs.getInt("housing"));
        science.setFood(rs.getInt("food"));
        science.setMilitary(rs.getInt("military"));
        science.setCrime(rs.getInt("crime"));
        science.setChanneling(rs.getInt("channeling"));
        return science;
    }

    protected Survey handleSurvey(ResultSet rs, Connection conn) throws SQLException {
        Survey survey = new Survey();
        survey.setId(rs.getString("id"));
        survey.setLastUpdate(QueryHandler.toCalendar(rs.getTimestamp("last_update")));
        survey.setEfficiency(rs.getFloat("efficiency"));
        survey.setBarren(rs.getInt("barren"));
        survey.setHomes(rs.getInt("homes"));
        survey.setFarms(rs.getInt("farms"));
        survey.setMills(rs.getInt("mills"));
        survey.setBanks(rs.getInt("banks"));
        survey.setTrainingGrounds(rs.getInt("training_grounds"));
        survey.setBarracks(rs.getInt("barracks"));
        survey.setArmories(rs.getInt("armories"));
        survey.setForts(rs.getInt("forts"));
        survey.setGuardStations(rs.getInt("guard_stations"));
        survey.setHospitals(rs.getInt("hospitals"));
        survey.setGuilds(rs.getInt("guilds"));
        survey.setTowers(rs.getInt("towers"));
        survey.setThievesDens(rs.getInt("thief_dens"));
        survey.setWatchtowers(rs.getInt("watchtowers"));
        survey.setLibraries(rs.getInt("libraries"));
        survey.setSchools(rs.getInt("schools"));
        survey.setStables(rs.getInt("stables"));
        survey.setDungeons(rs.getInt("dungeons"));
        return survey;
    }

    private boolean saveProvinceMini(Province province) throws SQLException {
        String gender = "M";
        if (province.isGender()) {
            gender = "F";
        }
        boolean updateMode = true;
        int rows = QueryHandler.update(connection, SQL.getProperty("update.province.mini"), province.getAcres(), gender, province.getNetworth(), province.getRace().ordinal(), province.getRank().ordinal(), province.getKingdomId(), province.getName());
        if (rows == 0) {
            updateMode = false;
            rows = QueryHandler.insert(connection, SQL.getProperty("insert.province.mini"), province.getId(), province.getKingdomId(), province.getName(), QueryHandler.toTimestamp(province.getLastUpdate()), province.getAcres(), gender, province.getNetworth(), province.getRace().ordinal(), province.getRank().ordinal(), province.getPersonality().ordinal());
        }
        saveProvinceDetails(province, updateMode);
        commit();
        return rows > 0;
    }

    private void saveProvinceDetails(Province province, boolean updateMode) throws SQLException {
        Military military = province.getMilitary();
        Science science = province.getScience();
        Survey survey = province.getSurvey();
        String militaryRaw = "N";
        if (military.isRaw()) {
            militaryRaw = "Y";
        }
        if (updateMode) {
            QueryHandler.update(connection, SQL.getProperty("update.science"), science.getLastUpdate(), science.getAlchemy(), science.getTools(), science.getHousing(), science.getFood(), science.getMilitary(), science.getCrime(), science.getChanneling(), science.getProvince().getId());
            QueryHandler.update(connection, SQL.getProperty("update.military"), military.getLastUpdate(), military.getOffense(), military.getDefense(), militaryRaw, military.getProvince().getId());
            QueryHandler.update(connection, SQL.getProperty("update.survey"), survey.getLastUpdate(), survey.getEfficiency(), survey.getBarren(), survey.getHomes(), survey.getFarms(), survey.getMills(), survey.getBanks(), survey.getTrainingGrounds(), survey.getBarracks(), survey.getArmories(), survey.getForts(), survey.getGuardStations(), survey.getHospitals(), survey.getGuilds(), survey.getTowers(), survey.getThievesDens(), survey.getWatchtowers(), survey.getLibraries(), survey.getSchools(), survey.getStables(), survey.getDungeons(), survey.getProvince().getId());
        } else {
            QueryHandler.insert(connection, SQL.getProperty("insert.science"), science.getId(), science.getProvince().getId(), science.getLastUpdate(), science.getAlchemy(), science.getTools(), science.getHousing(), science.getFood(), science.getMilitary(), science.getCrime(), science.getChanneling());
            QueryHandler.insert(connection, SQL.getProperty("insert.military"), military.getId(), military.getProvince().getId(), military.getLastUpdate(), military.getOffense(), military.getDefense(), militaryRaw);
            QueryHandler.insert(connection, SQL.getProperty("insert.survey"), survey.getId(), survey.getProvince().getId(), survey.getLastUpdate(), survey.getEfficiency(), survey.getBarren(), survey.getHomes(), survey.getFarms(), survey.getMills(), survey.getBanks(), survey.getTrainingGrounds(), survey.getBarracks(), survey.getArmories(), survey.getForts(), survey.getGuardStations(), survey.getHospitals(), survey.getGuilds(), survey.getTowers(), survey.getThievesDens(), survey.getWatchtowers(), survey.getLibraries(), survey.getSchools(), survey.getStables(), survey.getDungeons());
        }
        QueryHandler.delete(connection, SQL.getProperty("delete.army.by.militaryId"), military.getId());
        for (Army army : military.getArmies()) {
            QueryHandler.insert(connection, SQL.getProperty("insert.army"), army.getId(), army.getMilitary().getId(), army.getGenerals(), army.getSoldiers(), army.getOffspecs(), army.getDefspecs(), army.getElites(), army.getHorses(), army.getSpoils(), army.getReturnTime());
        }
    }
}
