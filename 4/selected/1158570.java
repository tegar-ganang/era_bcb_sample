package org.inigma.utopia.sync;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.inigma.utopia.Army;
import org.inigma.utopia.Kingdom;
import org.inigma.utopia.Military;
import org.inigma.utopia.Province;
import org.inigma.utopia.Science;
import org.inigma.utopia.Survey;
import org.springframework.beans.factory.annotation.Autowired;

public class DataSyncService {

    private static Log logger = LogFactory.getLog(DataSyncService.class);

    @Autowired
    private DataSyncTemplate template;

    public String synchronize(String inputXml) {
        String error = "";
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(inputXml.getBytes());
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            DataSyncSaxHandler handler = new DataSyncSaxHandler(template);
            parser.parse(stream, handler);
            Calendar lastSync = handler.getLastDataSync();
            StringBuilder sb = new StringBuilder("<data>\n");
            for (Kingdom kingdom : template.getKingdomSyncList(lastSync)) {
                sb.append(toXml(kingdom));
            }
            for (Province province : template.getProvinceSyncList(lastSync)) {
                sb.append(toXml(province, lastSync.getTimeInMillis()));
            }
            sb.append("</data>");
            return sb.toString();
        } catch (Exception e) {
            error = e.getMessage();
            logger.error("TODO", e);
        }
        return "<data error='" + StringEscapeUtils.escapeXml(error) + "' />";
    }

    private String toXml(Kingdom kingdom) {
        StringBuilder sb = new StringBuilder("<kingdom ");
        sb.append("lastUpdate='").append(kingdom.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("kingdom='").append(kingdom.getLocation().getKingdom()).append("' ");
        sb.append("island='").append(kingdom.getLocation().getIsland()).append("' ");
        sb.append("name='").append(StringEscapeUtils.escapeXml(kingdom.getName())).append("' ");
        sb.append("relation='").append(kingdom.getRelation()).append("' ");
        sb.append("stance='").append(kingdom.getStance()).append("' ");
        sb.append("warCount='").append(kingdom.getWarCount()).append("' ");
        sb.append("warWins='").append(kingdom.getWarWins()).append("' ");
        sb.append("warNetworthDiff='").append(kingdom.getWarNetworthDiff()).append("' ");
        sb.append(" />\n");
        return sb.toString();
    }

    private String toXml(Military military) {
        StringBuilder sb = new StringBuilder("<military ");
        sb.append("lastUpdate='").append(military.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("raw='").append(military.isRaw()).append("' ");
        sb.append("offense='").append(military.getOffense()).append("' ");
        sb.append("defense='").append(military.getDefense()).append("' ");
        sb.append(">\n");
        for (Army army : military.getArmies()) {
            sb.append("<army ");
            sb.append("general='").append(army.getGenerals()).append("' ");
            sb.append("soldiers='").append(army.getSoldiers()).append("' ");
            sb.append("offspecs='").append(army.getOffspecs()).append("' ");
            sb.append("defspecs='").append(army.getDefspecs()).append("' ");
            sb.append("elites='").append(army.getElites()).append("' ");
            sb.append("horses='").append(army.getHorses()).append("' ");
            sb.append("spoils='").append(army.getSpoils()).append("' ");
            sb.append("eta='").append(army.getReturnTime().getTimeInMillis()).append("' ");
            sb.append("/>\n");
        }
        sb.append("</military>\n");
        return sb.toString();
    }

    private String toXml(Province province, long lastSync) {
        Kingdom kingdom = template.getKingdomById(province.getKingdomId());
        StringBuilder sb = new StringBuilder("<province ");
        sb.append("lastUpdate='").append(province.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("kingdom='").append(kingdom.getLocation().getKingdom()).append("' ");
        sb.append("island='").append(kingdom.getLocation().getIsland()).append("' ");
        sb.append("name='").append(StringEscapeUtils.escapeXml(province.getName())).append("' ");
        sb.append("acres='").append(province.getAcres()).append("' ");
        sb.append("networth='").append(province.getNetworth()).append("' ");
        sb.append("race='").append(province.getRace()).append("' ");
        sb.append("rank='").append(province.getRank()).append("' ");
        if (province.getLeader() != null) {
            sb.append("gender='").append(province.isGender()).append("' ");
            sb.append("leader='").append(StringEscapeUtils.escapeXml(province.getLeader())).append("' ");
            sb.append("personality='").append(province.getPersonality()).append("' ");
            sb.append("peasants='").append(province.getPeasants()).append("' ");
            sb.append("gold='").append(province.getGold()).append("' ");
            sb.append("food='").append(province.getFood()).append("' ");
            sb.append("runes='").append(province.getRunes()).append("' ");
            sb.append("trade='").append(province.getTradeBalance()).append("' ");
            sb.append("thieves='").append(province.getThieves()).append("' ");
            sb.append("wizards='").append(province.getWizards()).append("' ");
            sb.append("soldiers='").append(province.getSoldiers()).append("' ");
            sb.append("offspecs='").append(province.getOffspecs()).append("' ");
            sb.append("defspecs='").append(province.getDefspecs()).append("' ");
            sb.append("elites='").append(province.getElites()).append("' ");
            sb.append("horses='").append(province.getHorses()).append("' ");
            sb.append("prisoners='").append(province.getPrisoners()).append("' ");
            sb.append("offense='").append(province.getOffense()).append("' ");
            sb.append("defense='").append(province.getDefense()).append("' ");
        }
        sb.append(">\n");
        long lastUpdate = province.getScience().getLastUpdate().getTimeInMillis();
        if (lastUpdate != 0 && lastUpdate > lastSync) {
            sb.append(toXml(province.getScience()));
        }
        lastUpdate = province.getSurvey().getLastUpdate().getTimeInMillis();
        if (lastUpdate != 0 && lastUpdate > lastSync) {
            sb.append(toXml(province.getSurvey()));
        }
        lastUpdate = province.getMilitary().getLastUpdate().getTimeInMillis();
        if (lastUpdate != 0 && lastUpdate > lastSync) {
            sb.append(toXml(province.getMilitary()));
        }
        sb.append("</province>\n");
        return sb.toString();
    }

    private String toXml(Science science) {
        StringBuilder sb = new StringBuilder("<science ");
        sb.append("lastUpdate='").append(science.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("alchemy='").append(science.getAlchemy()).append("' ");
        sb.append("tools='").append(science.getTools()).append("' ");
        sb.append("housing='").append(science.getHousing()).append("' ");
        sb.append("food='").append(science.getFood()).append("' ");
        sb.append("military='").append(science.getMilitary()).append("' ");
        sb.append("crime='").append(science.getCrime()).append("' ");
        sb.append("channeling='").append(science.getChanneling()).append("' ");
        sb.append(" />\n");
        return sb.toString();
    }

    private String toXml(Survey survey) {
        StringBuilder sb = new StringBuilder("<survey ");
        sb.append("lastUpdate='").append(survey.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("barrens='").append(survey.getBarren()).append("' ");
        sb.append("homes='").append(survey.getHomes()).append("' ");
        sb.append("farms='").append(survey.getFarms()).append("' ");
        sb.append("mills='").append(survey.getMills()).append("' ");
        sb.append("banks='").append(survey.getBanks()).append("' ");
        sb.append("trainingGrounds='").append(survey.getTrainingGrounds()).append("' ");
        sb.append("barracks='").append(survey.getBarracks()).append("' ");
        sb.append("armories='").append(survey.getArmories()).append("' ");
        sb.append("forts='").append(survey.getForts()).append("' ");
        sb.append("guardStations='").append(survey.getGuardStations()).append("' ");
        sb.append("hospitals='").append(survey.getHospitals()).append("' ");
        sb.append("guilds='").append(survey.getGuilds()).append("' ");
        sb.append("towers='").append(survey.getTowers()).append("' ");
        sb.append("thiefDens='").append(survey.getThievesDens()).append("' ");
        sb.append("watchtower='").append(survey.getWatchtowers()).append("' ");
        sb.append("libraries='").append(survey.getLibraries()).append("' ");
        sb.append("schools='").append(survey.getSchools()).append("' ");
        sb.append("stables='").append(survey.getStables()).append("' ");
        sb.append("dungeons='").append(survey.getDungeons()).append("' ");
        sb.append(" />\n");
        return sb.toString();
    }
}
