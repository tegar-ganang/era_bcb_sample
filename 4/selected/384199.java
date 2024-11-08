package org.inigma.utopia.sync;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import javax.sql.DataSource;
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
import org.inigma.utopia.utils.CalendarUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class DataSyncTemplate extends SimpleJdbcTemplate {

    private static final String EXISTS_KINGDOM = "SELECT id FROM kingdom WHERE id=? ";

    private static final String EXISTS_PROVINCE = "SELECT id FROM province WHERE id=? ";

    private static final String EXISTS_SCIENCE = "SELECT id FROM science WHERE id=? ";

    private static final String EXISTS_SURVEY = "SELECT id FROM survey WHERE id=? ";

    private static final String EXISTS_MILITARY = "SELECT id FROM military WHERE id=? ";

    private static final String SELECT_SYNC_KINGDOM = "SELECT id, last_update, kingdom, island, name, relation, stance, " + "war_count, war_win, war_nwdiff " + "FROM kingdom " + "WHERE last_update > ? ";

    private static final String SELECT_SYNC_PROVINCE = "SELECT id, kingdom_id, last_update, name, acres, gender, leader, " + "networth, race, rank, personality, peasants, gold, food, runes, trade, thieves, wizards, " + "soldiers, offspecs, defspecs, elites, horses, prisoners, offense, defense " + "FROM province " + "WHERE last_update > ? " + "OR id IN (SELECT province_id FROM science WHERE last_update > ? " + "  UNION ALL " + "  SELECT province_id FROM survey WHERE last_update > ? " + "  UNION ALL " + "  SELECT province_id FROM military WHERE last_update > ? )";

    private static final String SELECT_KINGDOM = "SELECT id, last_update, kingdom, island, name, relation, stance, " + "war_count, war_win, war_nwdiff " + "FROM kingdom " + "WHERE kingdom = ? " + "AND island = ? ";

    private static final String SELECT_KINGDOM_BY_ID = "SELECT id, last_update, kingdom, island, name, relation, stance, " + "war_count, war_win, war_nwdiff " + "FROM kingdom " + "WHERE id = ? ";

    private static final String INSERT_KINGDOM = "INSERT INTO kingdom(id, last_update, kingdom, island, name, relation, " + "stance, war_count, war_win, war_nwdiff) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String UPDATE_KINGDOM = "UPDATE kingdom SET last_update=?, name=?, relation=?, stance=?, " + "war_count=?, war_win=?, war_nwdiff=? " + "WHERE id=? " + "AND last_update < ?";

    private static final String SELECT_PROVINCE_BY_NAME = "SELECT id, kingdom_id, last_update, name, acres, gender, leader, " + "networth, race, rank, personality, peasants, gold, food, runes, trade, thieves, wizards, " + "soldiers, offspecs, defspecs, elites, horses, prisoners, offense, defense " + "FROM province " + "WHERE name=? ";

    private static final String INSERT_PROVINCE = "INSERT INTO province(id, kingdom_id, last_update, name, acres, " + "gender, leader, networth, race, rank, personality, peasants, gold, food, runes, trade, thieves, wizards, " + "soldiers, offspecs, defspecs, elites, horses, prisoners, offense, defense) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_PROVINCE = "UPDATE province " + "SET kingdom_id=?, last_update=?, name=?, acres=?, " + "gender=?, leader=?, networth=?, race=?, rank=?, personality=?, peasants=?, gold=?, food=?, runes=?, " + "trade=?, thieves=?, wizards=?, soldiers=?, offspecs=?, defspecs=?, elites=?, horses=?, prisoners=?, " + "offense=?, defense=? " + "WHERE id=? " + "AND last_update < ?";

    private static final String SELECT_SCIENCE = "SELECT id, province_id, last_update, alchemy, tools, housing, food, " + " military, crime, channeling " + "FROM science " + "WHERE province_id=? ";

    private static final String INSERT_SCIENCE = "INSERT INTO science(id, province_id, last_update, alchemy, tools, " + " housing, food, military, crime, channeling) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String UPDATE_SCIENCE = "UPDATE science SET last_update=?, alchemy=?, tools=?, housing=?, " + "food=?, military=?, crime=?, channeling=? " + "WHERE id=? " + "AND last_update < ? ";

    private static final String SELECT_SURVEY = "SELECT id, province_id, last_update, efficiency, barren, homes, farms, " + "mills, banks, training_grounds, barracks, armories, forts, guard_stations, hospitals, guilds, towers, " + "thief_dens, watchtowers, libraries, schools, stables, dungeons " + "FROM survey " + "WHERE province_id=?";

    private static final String INSERT_SURVEY = "INSERT INTO survey(id, province_id, last_update, efficiency, barren, " + "homes, farms, mills, banks, training_grounds, barracks, armories, forts, guard_stations, hospitals, guilds, " + "towers, thief_dens, watchtowers, libraries, schools, stables, dungeons) " + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_SURVEY = "UPDATE survey SET last_update=?, efficiency=?, barren=?, homes=?, " + "farms=?, mills=?, banks=?, training_grounds=?, barracks=?, armories=?, forts=?, guard_stations=?, " + "hospitals=?, guilds=?, towers=?, thief_dens=?, watchtowers=?, libraries=?, schools=?, stables=?, dungeons=? " + "WHERE id=? " + "AND last_update < ? ";

    private static final String SELECT_MILITARY = "SELECT id, province_id, last_update, offense, defense, raw_ind " + "FROM military " + "WHERE province_id=? ";

    private static final String INSERT_MILITARY = "INSERT INTO military(id, province_id, last_update, offense, defense, " + "raw_ind) VALUES(?, ?, ?, ?, ?, ?) ";

    private static final String UPDATE_MILITARY = "UPDATE military SET last_update=?, offense=?, defense=?, raw_ind=? " + "WHERE id=? " + "AND last_update < ? ";

    private static final String SELECT_ARMY = "SELECT id, military_id, generals, soldiers, offspecs, defspecs, elites, " + "horses, spoils, eta " + "FROM army " + "WHERE military_id=? ";

    private static final String INSERT_ARMY = "INSERT INTO army(id, military_id, generals, soldiers, offspecs, " + "defspecs, elites, horses, spoils, eta) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String DELETE_ARMIES = "DELETE FROM army WHERE military_id=? ";

    @Autowired
    public DataSyncTemplate(DataSource ds) {
        super(ds);
    }

    public Kingdom getKingdom(int kingdom, int island) {
        try {
            return queryForObject(SELECT_KINGDOM, kingdomMapper, kingdom, island);
        } catch (EmptyResultDataAccessException e) {
            Kingdom k = new Kingdom(new Coordinate(kingdom, island));
            Calendar update = CalendarUtils.getCalendar();
            update.setTimeInMillis(0);
            k.setLastUpdate(update);
            return k;
        }
    }

    public Province getProvinceByName(String name) {
        try {
            return queryForObject(SELECT_PROVINCE_BY_NAME, provinceMapper, name);
        } catch (EmptyResultDataAccessException e) {
            Province province = new Province();
            province.setName(name);
            province.getLastUpdate().setTimeInMillis(0);
            return province;
        }
    }

    public Kingdom getKingdomById(String id) {
        return queryForObject(SELECT_KINGDOM_BY_ID, kingdomMapper, id);
    }

    public List<Kingdom> getKingdomSyncList(Calendar lastUpdate) {
        return query(SELECT_SYNC_KINGDOM, kingdomMapper, lastUpdate);
    }

    public List<Province> getProvinceSyncList(Calendar lastUpdate) {
        return query(SELECT_SYNC_PROVINCE, provinceMapper, lastUpdate, lastUpdate, lastUpdate, lastUpdate);
    }

    public boolean updateKingdom(Kingdom kingdom) {
        int updated = 0;
        try {
            queryForObject(EXISTS_KINGDOM, String.class, kingdom.getId());
            updated = update(UPDATE_KINGDOM, kingdom.getLastUpdate(), kingdom.getName(), kingdom.getRelation().toString(), kingdom.getStance().toString(), kingdom.getWarCount(), kingdom.getWarWins(), kingdom.getWarNetworthDiff(), kingdom.getId(), kingdom.getLastUpdate());
        } catch (EmptyResultDataAccessException e) {
            updated = update(INSERT_KINGDOM, kingdom.getId(), kingdom.getLastUpdate(), kingdom.getLocation().getKingdom(), kingdom.getLocation().getIsland(), kingdom.getName(), kingdom.getRelation().toString(), kingdom.getStance().toString(), kingdom.getWarCount(), kingdom.getWarWins(), kingdom.getWarNetworthDiff());
        }
        return updated > 0;
    }

    public boolean updateProvince(Province province) {
        int updated = 0;
        Coordinate coord = province.getCoordinate();
        Calendar update = province.getLastUpdate();
        Kingdom kingdom = getKingdom(coord.getKingdom(), coord.getIsland());
        if (kingdom.getLastUpdate().getTimeInMillis() == 0) {
            kingdom.setLastUpdate(province.getLastUpdate());
            updateKingdom(kingdom);
        }
        try {
            queryForObject(EXISTS_PROVINCE, String.class, province.getId());
            updated = update(UPDATE_PROVINCE, kingdom.getId(), update, province.getName(), province.getAcres(), province.isGender(), province.getLeader(), province.getNetworth(), province.getRace().toString(), province.getRank().toString(), province.getPersonality().toString(), province.getPeasants(), province.getGold(), province.getFood(), province.getRunes(), province.getTradeBalance(), province.getThieves(), province.getWizards(), province.getSoldiers(), province.getOffspecs(), province.getDefspecs(), province.getElites(), province.getHorses(), province.getPrisoners(), province.getOffense(), province.getDefense(), province.getId(), update);
        } catch (EmptyResultDataAccessException e) {
            updated = update(INSERT_PROVINCE, province.getId(), kingdom.getId(), update, province.getName(), province.getAcres(), province.isGender(), province.getLeader(), province.getNetworth(), province.getRace().toString(), province.getRank().toString(), province.getPersonality().toString(), province.getPeasants(), province.getGold(), province.getFood(), province.getRunes(), province.getTradeBalance(), province.getThieves(), province.getWizards(), province.getSoldiers(), province.getOffspecs(), province.getDefspecs(), province.getElites(), province.getHorses(), province.getPrisoners(), province.getOffense(), province.getDefense());
        }
        return updated > 0;
    }

    public boolean updateMilitary(Military military) {
        int updated = 0;
        String provinceId = military.getProvince().getId();
        Calendar update = military.getLastUpdate();
        try {
            queryForObject(EXISTS_MILITARY, String.class, military.getId());
            updated = update(UPDATE_MILITARY, update, military.getOffense(), military.getDefense(), military.isRaw(), military.getId(), update);
        } catch (EmptyResultDataAccessException e) {
            updated = update(INSERT_MILITARY, military.getId(), provinceId, update, military.getOffense(), military.getDefense(), military.isRaw());
        }
        update(DELETE_ARMIES, military.getId());
        for (Army army : military.getArmies()) {
            update(INSERT_ARMY, army.getId(), military.getId(), army.getGenerals(), army.getSoldiers(), army.getOffspecs(), army.getDefspecs(), army.getElites(), army.getHorses(), army.getSpoils(), army.getReturnTime());
        }
        return updated > 0;
    }

    public boolean updateScience(Science science) {
        int updated = 0;
        String provinceId = science.getProvince().getId();
        Calendar update = science.getLastUpdate();
        try {
            queryForObject(EXISTS_SCIENCE, String.class, science.getId());
            updated = update(UPDATE_SCIENCE, update, science.getAlchemy(), science.getTools(), science.getHousing(), science.getFood(), science.getMilitary(), science.getCrime(), science.getChanneling(), science.getId(), update);
        } catch (EmptyResultDataAccessException e) {
            updated = update(INSERT_SCIENCE, science.getId(), provinceId, update, science.getAlchemy(), science.getTools(), science.getHousing(), science.getFood(), science.getMilitary(), science.getCrime(), science.getChanneling());
        }
        return updated > 0;
    }

    public boolean updateSurvey(Survey survey) {
        int updated = 0;
        String provinceId = survey.getProvince().getId();
        Calendar update = survey.getLastUpdate();
        try {
            queryForObject(EXISTS_SURVEY, String.class, survey.getId());
            updated = update(UPDATE_SURVEY, update, survey.getEfficiency(), survey.getBarren(), survey.getHomes(), survey.getFarms(), survey.getMills(), survey.getBanks(), survey.getTrainingGrounds(), survey.getBarracks(), survey.getArmories(), survey.getForts(), survey.getGuardStations(), survey.getHospitals(), survey.getGuilds(), survey.getTowers(), survey.getThievesDens(), survey.getWatchtowers(), survey.getLibraries(), survey.getSchools(), survey.getStables(), survey.getDungeons(), survey.getId(), update);
        } catch (EmptyResultDataAccessException e) {
            updated = update(INSERT_SURVEY, survey.getId(), provinceId, update, survey.getEfficiency(), survey.getBarren(), survey.getHomes(), survey.getFarms(), survey.getMills(), survey.getBanks(), survey.getTrainingGrounds(), survey.getBarracks(), survey.getArmories(), survey.getForts(), survey.getGuardStations(), survey.getHospitals(), survey.getGuilds(), survey.getTowers(), survey.getThievesDens(), survey.getWatchtowers(), survey.getLibraries(), survey.getSchools(), survey.getStables(), survey.getDungeons());
        }
        return updated > 0;
    }

    private Calendar getCalendar(Timestamp ts) {
        Calendar time = CalendarUtils.getCalendar();
        time.setTimeInMillis(ts.getTime());
        return time;
    }

    private ParameterizedRowMapper<Kingdom> kingdomMapper = new ParameterizedRowMapper<Kingdom>() {

        public Kingdom mapRow(ResultSet rs, int rowNum) throws SQLException {
            Coordinate coord = new Coordinate();
            coord.setKingdom(rs.getInt("kingdom"));
            coord.setIsland(rs.getInt("island"));
            Kingdom kingdom = new Kingdom(coord);
            kingdom.setId(rs.getString("id"));
            kingdom.setLastUpdate(getCalendar(rs.getTimestamp("last_update")));
            kingdom.setName(rs.getString("name"));
            kingdom.setRelation(Relation.valueOf(rs.getString("relation")));
            kingdom.setStance(Stance.valueOf(rs.getString("stance")));
            kingdom.setWarCount(rs.getInt("war_count"));
            kingdom.setWarWins(rs.getInt("war_win"));
            kingdom.setWarNetworthDiff(rs.getInt("war_nwdiff"));
            return kingdom;
        }
    };

    private ParameterizedRowMapper<Province> provinceMapper = new ParameterizedRowMapper<Province>() {

        public Province mapRow(ResultSet rs, int rowNum) throws SQLException {
            Kingdom kingdom = getKingdomById(rs.getString("kingdom_id"));
            Province province = new Province(rs.getString("name"), kingdom.getLocation());
            province.setId(rs.getString("id"));
            province.setKingdomId(rs.getString("kingdom_id"));
            Timestamp timestamp = rs.getTimestamp("last_update");
            province.getLastUpdate().setTimeInMillis(timestamp.getTime());
            province.setAcres(rs.getInt("acres"));
            province.setGender(rs.getBoolean("gender"));
            province.setName(rs.getString("name"));
            province.setLeader(rs.getString("leader"));
            province.setNetworth(rs.getInt("networth"));
            province.setRace(Race.valueOf(rs.getString("race")));
            province.setRank(Rank.valueOf(rs.getString("rank")));
            province.setPersonality(Personality.valueOf(rs.getString("personality")));
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
            try {
                Science science = queryForObject(SELECT_SCIENCE, scienceMapper, province.getId());
                science.setProvince(province);
                province.setScience(science);
            } catch (EmptyResultDataAccessException ignore) {
                province.getScience().getLastUpdate().setTimeInMillis(0);
            }
            try {
                Survey survey = queryForObject(SELECT_SURVEY, surveyMapper, province.getId());
                survey.setProvince(province);
                province.setSurvey(survey);
            } catch (EmptyResultDataAccessException ignore) {
                province.getSurvey().getLastUpdate().setTimeInMillis(0);
            }
            try {
                Military military = queryForObject(SELECT_MILITARY, militaryMapper, province.getId());
                military.setProvince(province);
                province.setMilitary(military);
            } catch (EmptyResultDataAccessException ignore) {
                province.getMilitary().getLastUpdate().setTimeInMillis(0);
            }
            return province;
        }
    };

    private ParameterizedRowMapper<Army> armyMapper = new ParameterizedRowMapper<Army>() {

        public Army mapRow(ResultSet rs, int rowNum) throws SQLException {
            Army army = new Army();
            army.setId(rs.getString("id"));
            army.setGenerals(rs.getInt("generals"));
            army.getReturnTime().setTimeInMillis(rs.getTimestamp("eta").getTime());
            army.setSoldiers(rs.getInt("soldiers"));
            army.setOffspecs(rs.getInt("offspecs"));
            army.setDefspecs(rs.getInt("defspecs"));
            army.setElites(rs.getInt("elites"));
            army.setHorses(rs.getInt("horses"));
            army.setSpoils(rs.getInt("spoils"));
            return army;
        }
    };

    private ParameterizedRowMapper<Military> militaryMapper = new ParameterizedRowMapper<Military>() {

        public Military mapRow(ResultSet rs, int rowNum) throws SQLException {
            Military military = new Military();
            military.setId(rs.getString("id"));
            military.getLastUpdate().setTimeInMillis(rs.getTimestamp("last_update").getTime());
            military.setOffense(rs.getInt("offense"));
            military.setDefense(rs.getInt("defense"));
            military.setRaw(rs.getBoolean("raw_ind"));
            military.setArmies(query(SELECT_ARMY, armyMapper, military.getId()));
            for (Army army : military.getArmies()) {
                army.setMilitary(military);
            }
            return military;
        }
    };

    private ParameterizedRowMapper<Science> scienceMapper = new ParameterizedRowMapper<Science>() {

        public Science mapRow(ResultSet rs, int rowNum) throws SQLException {
            Science science = new Science();
            science.setId(rs.getString("id"));
            science.getLastUpdate().setTimeInMillis(rs.getTimestamp("last_update").getTime());
            science.setAlchemy(rs.getInt("alchemy"));
            science.setTools(rs.getInt("tools"));
            science.setHousing(rs.getInt("housing"));
            science.setFood(rs.getInt("food"));
            science.setMilitary(rs.getInt("military"));
            science.setCrime(rs.getInt("crime"));
            science.setChanneling(rs.getInt("channeling"));
            return science;
        }
    };

    private ParameterizedRowMapper<Survey> surveyMapper = new ParameterizedRowMapper<Survey>() {

        public Survey mapRow(ResultSet rs, int rowNum) throws SQLException {
            Survey survey = new Survey();
            survey.setId(rs.getString("id"));
            survey.getLastUpdate().setTimeInMillis(rs.getTimestamp("last_update").getTime());
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
    };
}
