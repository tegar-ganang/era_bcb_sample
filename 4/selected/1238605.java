package fr.fg.server.core;

import fr.fg.server.data.DataAccess;
import fr.fg.server.data.Effect;
import fr.fg.server.data.Player;
import fr.fg.server.util.JSONStringer;
import fr.fg.server.util.LoggingSystem;
import fr.fg.server.util.Utilities;

public class Update {

    private static final int TYPE_NEW_MESSAGE = 1, TYPE_AREA = 2, TYPE_PLAYER_FLEETS = 4, TYPE_PLAYER_SYSTEMS = 5, TYPE_XP = 6, TYPE_CHAT = 10, TYPE_NEW_NEWS = 12, TYPE_CONTRACTS_STATE = 14, TYPE_PLAYER_CONTRACTS = 15, TYPE_NEW_EVENT = 16, TYPE_CHAT_CHANNELS = 17, TYPE_ALLY = 18, TYPE_SERVER_SHUTDOWN = 19, TYPE_INFORMATION = 20, TYPE_ADVANCEMENTS = 21, TYPE_EFFECT = 22, TYPE_PLAYER_GENERATORS = 23, TYPE_PRODUCTS = 24, TYPE_PLAYER_FLEET = 25;

    private static final Update UPDATE_AREA = new Update(TYPE_AREA), UPDATE_PLAYER_FLEETS = new Update(TYPE_PLAYER_FLEETS), UPDATE_PLAYER_SYSTEMS = new Update(TYPE_PLAYER_SYSTEMS), UPDATE_XP = new Update(TYPE_XP), UPDATE_NEW_NEWS = new Update(TYPE_NEW_NEWS), UPDATE_NEW_MESSAGE = new Update(TYPE_NEW_MESSAGE), UPDATE_NEW_EVENT = new Update(TYPE_NEW_EVENT), UPDATE_ADVANCEMENTS = new Update(TYPE_ADVANCEMENTS), UPDATE_CHAT_CHANNELS = new Update(TYPE_CHAT_CHANNELS), UPDATE_CONTRACTS_STATE = new Update(TYPE_CONTRACTS_STATE), UPDATE_PLAYER_CONTRACTS = new Update(TYPE_PLAYER_CONTRACTS), UPDATE_PLAYER_GENERATORS = new Update(TYPE_PLAYER_GENERATORS), UPDATE_PRODUCTS = new Update(TYPE_PRODUCTS);

    private int type;

    private Object[] args;

    private long date;

    private Update(int type, Object... args) {
        this.type = type;
        this.args = args;
        this.date = Utilities.now();
    }

    public int getType() {
        return type;
    }

    public String getData(Player player) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{\"type\":");
        buffer.append(type);
        buffer.append(",\"date\":");
        buffer.append(date);
        buffer.append(",\"data\":");
        buffer.append(buildUpdate(player));
        buffer.append("}");
        return buffer.toString();
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Update)) return false;
        Update update = (Update) object;
        if (update.getType() != getType()) return false;
        if (args.length != update.args.length) return false;
        for (int i = 0; i < args.length; i++) if (!args[i].equals(update.args[i])) return false;
        return true;
    }

    public static Update getAreaUpdate() {
        return UPDATE_AREA;
    }

    public static Update getPlayerFleetsUpdate() {
        return UPDATE_PLAYER_FLEETS;
    }

    public static Update getPlayerSystemsUpdate() {
        return UPDATE_PLAYER_SYSTEMS;
    }

    public static Update getChatUpdate(String data) {
        return new Update(TYPE_CHAT, data);
    }

    public static Update getXpUpdate() {
        return UPDATE_XP;
    }

    public static Update getNewEventUpdate() {
        return UPDATE_NEW_EVENT;
    }

    public static Update getNewNewsUpdate() {
        return UPDATE_NEW_NEWS;
    }

    public static Update getNewMessageUpdate() {
        return UPDATE_NEW_MESSAGE;
    }

    public static Update getChatChannelsUpdate() {
        return UPDATE_CHAT_CHANNELS;
    }

    public static Update getAllyUpdate(long lastUpdate) {
        return new Update(TYPE_ALLY, lastUpdate);
    }

    public static Update getServerShutdownUpdate(int time) {
        return new Update(TYPE_SERVER_SHUTDOWN, time);
    }

    public static Update getInformationUpdate(String information) {
        return new Update(TYPE_INFORMATION, information);
    }

    public static Update getAdvancementsUpdate() {
        return UPDATE_ADVANCEMENTS;
    }

    public static Update getEffectUpdate(Effect effect) {
        return new Update(TYPE_EFFECT, effect);
    }

    public static Update getContractStateUpdate() {
        return UPDATE_CONTRACTS_STATE;
    }

    public static Update getPlayerContractsUpdate() {
        return UPDATE_PLAYER_CONTRACTS;
    }

    public static Update getPlayerGeneratorsUpdate() {
        return UPDATE_PLAYER_GENERATORS;
    }

    public static Update getProductsUpdate() {
        return UPDATE_PRODUCTS;
    }

    public static Update getPlayerFleetUpdate(int idFleet) {
        return new Update(TYPE_PLAYER_FLEET, idFleet);
    }

    private String buildUpdate(Player player) {
        switch(type) {
            case TYPE_ADVANCEMENTS:
                return AdvancementTools.getPlayerAdvancements(null, player).toString();
            case TYPE_CONTRACTS_STATE:
                return ContractTools.getContractsState(null, player).toString();
            case TYPE_PLAYER_CONTRACTS:
                return ContractTools.getPlayerContracts(null, player).toString();
            case TYPE_INFORMATION:
                return new JSONStringer().value((String) args[0]).toString();
            case TYPE_EFFECT:
                return EffectTools.getEffect(null, (Effect) args[0]).toString();
            case TYPE_PLAYER_FLEETS:
                return FleetTools.getPlayerFleets(null, player).toString();
            case TYPE_PLAYER_FLEET:
                return FleetTools.getPlayerFleet(null, (Integer) args[0]).toString();
            case TYPE_PLAYER_SYSTEMS:
                return SystemTools.getPlayerSystems(null, player).toString();
            case TYPE_PLAYER_GENERATORS:
                return StructureTools.getPlayerGenerators(null, player).toString();
            case TYPE_XP:
                return XpTools.getPlayerXp(null, player).toString();
            case TYPE_SERVER_SHUTDOWN:
                return String.valueOf(args[0]);
            case TYPE_CHAT_CHANNELS:
                return ChatTools.getChannels(null, player).toString();
            case TYPE_NEW_EVENT:
            case TYPE_NEW_NEWS:
            case TYPE_NEW_MESSAGE:
                return "\"\"";
            case TYPE_CHAT:
                return (String) args[0];
            case TYPE_PRODUCTS:
                return ProductTools.getPlayerProducts(null, player).toString();
            case TYPE_AREA:
                try {
                    if (player.getIdCurrentArea() != 0) return AreaTools.getArea(null, DataAccess.getAreaById(player.getIdCurrentArea()), player).toString();
                } catch (Exception e) {
                    LoggingSystem.getServerLogger().warn("Could not build area update.", e);
                }
                return null;
            case TYPE_ALLY:
                try {
                    return AllyTools.getAlly(null, player, (Long) args[0]).toString();
                } catch (Exception e) {
                    LoggingSystem.getServerLogger().warn("Could not build ally update.", e);
                }
                return null;
            default:
                throw new IllegalStateException("No implementation defined for update: '" + type + "'.");
        }
    }
}
