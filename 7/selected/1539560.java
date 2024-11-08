package com.chessclub.bettingbot.services;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Properties;
import java.util.Vector;
import org.chessworks.common.javatools.io.QuietFileHelper;
import com.chessclub.bettingbot.BettingBot;
import com.chessclub.bettingbot.model.User;

public class UserService {

    private static final File CHAMPS_DB = new File("./champs.db");

    private static final File CHIPDATA_DB = new File("./chipdata.db");

    private static final File CHIPMONTH_DB = new File("./chipmonth.db");

    private static final File CHIPWEEK_DB = new File("./chipweek.db");

    private static final File FUTURES_DB = new File("./futures.db");

    private static final File PLAYERDATA_DB = new File("./playerdata.db");

    private static final File PLAYERVARS_DB = new File("./playervars.db");

    private static final File PLAYERS_TXT = new File("./Players.txt");

    /** The limit to the total number of registered users. */
    public static final int maxuser = 9999;

    public static final int initialEndowment = 5000;

    public static final int initialEndowmentFreeTrial = 1500;

    public static final String MARKET_MAKER = "Kiebitz";

    /** The total number of registered users at this time. */
    public int maxID;

    /** The BettingBot users. */
    public User[] users = new User[maxuser];

    /** The users, sorted by number of chips. Matches chips in chipslist. */
    public User[] bestChipsList = new User[maxuser];

    /** The users, sorted by chips won/lost this month. Matches amounts in monthList. */
    public User[] bestMonthList = new User[maxuser];

    /** The user names, sorted by chips won/lost this week. Matches amounts in weekList. */
    public User[] bestWeekList = new User[maxuser];

    public String richest;

    public String weekwinner;

    public String monthwinner;

    public int newregcount;

    public int newregcount2;

    /** The number of BGM and BM. */
    public int masterCounter;

    /** The names of the BGM and BM. Sorted by master points. length=masterCounter. */
    public String[] masterHandleList = new String[maxmaster];

    /** The master points for the names in the masterHandleList. Sorted by master points. lenght=masterCounter. */
    public int[] masterPointsList = new int[maxmaster];

    /** The maximum size of the masterHandleList and masterPoints list. It's the maximum number of betting masters supported. */
    public static final int maxmaster = 1500;

    private final BettingBot bot;

    public UserService(BettingBot bot) {
        this.bot = bot;
    }

    public boolean assign(String handle, int assignedAmount) {
        User user = findUser(handle);
        if (user == null) return false;
        return assign(user, assignedAmount);
    }

    public boolean assign(User user, int assignedAmount) {
        if ((user.actualChips + assignedAmount < 0) && !user.handle.equalsIgnoreCase(MARKET_MAKER)) return false;
        user.actualChips += assignedAmount;
        chipsIO.writeData();
        return true;
    }

    private int countDeletedUsers() {
        int count = 0;
        for (int i = 0; i < maxID; i++) {
            if (users[i].deleted) count++;
        }
        return count;
    }

    public User findUser(String handle) {
        for (int i = 0; i < maxID; i++) {
            if (users[i].handle.equalsIgnoreCase(handle)) {
                return users[i];
            }
        }
        return null;
    }

    public int getUserCount() {
        return maxID;
    }

    public void load() {
        newregcount2 = 0;
        playerListIO.readData();
        chipsIO.readData();
        futuresIO.readData();
        playerVarsIO.readData();
        champsIO.readData();
        monthlyChipsIO.readData();
        weeklyChipsIO.readData();
        sortMasters();
        sortWealth();
        sortWeeklyWinnings();
        sortMonthlyWinnings();
    }

    public void purgeDeletedPlayers() {
        int count = maxID - countDeletedUsers();
        playerListIO.writeData();
        bot.tellProgrammers("handle done");
        chipsIO.writeData();
        bot.tellProgrammers("chips done");
        futuresIO.writeData();
        bot.tellProgrammers("futures done");
        futuresIO.writeData();
        bot.tellProgrammers("vars done");
        monthlyChipsIO.writeData();
        bot.tellProgrammers("month done");
        weeklyChipsIO.writeData();
        bot.tellProgrammers("week done");
    }

    public User register(String name, boolean freeTrial) {
        int startAmount = (freeTrial ? initialEndowmentFreeTrial : initialEndowment);
        User newUser = new User(bot, maxID, name);
        newUser.chipsLastWeek = startAmount;
        newUser.chipsLastMonth = startAmount;
        newUser.actualChips = startAmount;
        newUser.unpaidFT = freeTrial;
        newUser.amountvar = 1;
        newUser.botvar = 1;
        newUser.jin = false;
        newUser.simOpen = false;
        newUser.initMasterPoints(0);
        newUser.monthlyFuturesTurnover = 0;
        newUser.recordChips = startAmount;
        newUser.selfBetType = 0;
        newUser.selfBetAmount = 0;
        newUser.futures = 0;
        newUser.doubleintmultiplier = 1;
        newregcount++;
        newregcount2++;
        users[newUser.id] = newUser;
        bestWeekList[newUser.id] = newUser;
        bestMonthList[newUser.id] = newUser;
        bestChipsList[newUser.id] = newUser;
        maxID = maxID + 1;
        playerListIO.writeData();
        playerVarsIO.writeData();
        return newUser;
    }

    public void newSave() {
        universalIO.writeText();
    }

    public void newLoad() {
        universalIO.readText();
    }

    public void save() {
        playerListIO.writeData();
        chipsIO.writeData();
        futuresIO.writeData();
        playerVarsIO.writeData();
        champsIO.writeData();
        monthlyChipsIO.writeData();
        weeklyChipsIO.writeData();
    }

    public void savePlayerData() {
        chipsIO.writeData();
        futuresIO.writeData();
        playerVarsIO.writeData();
    }

    public void saveWeeklyChips() {
        weeklyChipsIO.writeData();
    }

    public void saveChips() {
        chipsIO.writeData();
    }

    public void saveMonthlyChips() {
        monthlyChipsIO.writeData();
    }

    public void saveChamps() {
        champsIO.writeData();
    }

    private void sortMasters() {
        masterCounter = 0;
        for (int i = 0; i < maxID; i++) {
            if (users[i].getMasterPoints() > 0) {
                masterHandleList[masterCounter] = users[i].getHandle();
                masterPointsList[masterCounter] = users[i].getMasterPoints();
                masterCounter = masterCounter + 1;
            }
        }
        for (int i = masterCounter; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (masterPointsList[j] > masterPointsList[j + 1]) {
                    int tempp = masterPointsList[j];
                    String temppstring = masterHandleList[j];
                    masterPointsList[j] = masterPointsList[j + 1];
                    masterHandleList[j] = masterHandleList[j + 1];
                    masterPointsList[j + 1] = tempp;
                    masterHandleList[j + 1] = temppstring;
                }
            }
        }
    }

    private void sortMonthlyWinnings() {
        masterCounter = 0;
        for (int i = 0; i < maxID; i++) {
            bestMonthList[i] = users[i];
        }
        for (int i = maxID; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (bestMonthList[j].getProfitForMonth() > bestMonthList[j + 1].getProfitForMonth()) {
                    User u = bestMonthList[j];
                    bestMonthList[j] = bestMonthList[j + 1];
                    bestMonthList[j + 1] = u;
                }
            }
        }
    }

    private void sortWealth() {
        masterCounter = 0;
        for (int i = 0; i < maxID; i++) {
            bestChipsList[i] = users[i];
        }
        for (int i = maxID; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (bestChipsList[j].getTotalValue() > bestChipsList[j + 1].getTotalValue()) {
                    User u = bestChipsList[j];
                    bestChipsList[j] = bestChipsList[j + 1];
                    bestChipsList[j + 1] = u;
                }
            }
        }
        richest = bestChipsList[maxID - 1].getHandle();
    }

    private void sortWeeklyWinnings() {
        masterCounter = 0;
        for (int i = 0; i < maxID; i++) {
            bestWeekList[i] = users[i];
        }
        for (int i = maxID; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (bestWeekList[j].getProfitForWeek() > bestWeekList[j + 1].getProfitForWeek()) {
                    User u = bestWeekList[j];
                    bestWeekList[j] = bestWeekList[j + 1];
                    bestWeekList[j + 1] = u;
                }
            }
        }
    }

    private BotIO champsIO = new BotIO(CHAMPS_DB) {

        @Override
        public void doRead(DataInputStream in) throws Exception {
            weekwinner = in.readLine();
            monthwinner = in.readLine();
        }

        @Override
        public void doWrite(DataOutputStream out) throws Exception {
            out.writeBytes(weekwinner);
            out.writeByte('\n');
            out.writeBytes(monthwinner);
            out.writeByte('\n');
            out.writeBytes("EndOfFileBBrec");
        }
    };

    private BotIO chipsIO = new BotIO(CHIPDATA_DB) {

        @Override
        public void doRead(DataInputStream in) throws Exception {
            for (int i = 0; i < maxID; i++) {
                users[i].actualChips = in.readFloat();
            }
        }

        @Override
        public void doWrite(DataOutputStream out) throws Exception {
            for (int j = 0; j < maxID; j++) {
                out.writeFloat(users[j].actualChips);
            }
        }
    };

    private BotIO futuresIO = new BotIO(FUTURES_DB) {

        @Override
        public void doRead(DataInputStream in) throws Exception {
            for (int i = 0; i < maxID; i++) {
                users[i].futures = in.readInt();
            }
        }

        @Override
        public void doWrite(DataOutputStream out) throws Exception {
            for (int j = 0; j < maxID; j++) {
                out.writeInt(users[j].futures);
            }
        }
    };

    private BotIO monthlyChipsIO = new BotIO(CHIPMONTH_DB) {

        @Override
        public void doRead(DataInputStream in) throws Exception {
            bot.monthmonth = in.readInt();
            bot.monthday = in.readInt();
            bot.monthhour = in.readInt();
            bot.monthminute = in.readInt();
            int monthnumber = in.readInt();
            for (int i = 0; i < monthnumber; i++) {
                users[i].chipsLastMonth = in.readFloat();
            }
            for (int i = monthnumber; i < maxID; i++) {
                if (users[i].unpaidFT) {
                    users[i].chipsLastMonth = initialEndowmentFreeTrial;
                } else {
                    users[i].chipsLastMonth = initialEndowment;
                }
            }
        }

        @Override
        public void doWrite(DataOutputStream out) throws Exception {
            out.writeInt(bot.monthmonth);
            out.writeInt(bot.monthday);
            out.writeInt(bot.monthhour);
            out.writeInt(bot.monthminute);
            out.writeInt(maxID);
            for (int i = 0; i < maxID; i++) {
                out.writeFloat(users[i].chipsLastMonth);
            }
        }
    };

    private BotIO playerListIO = new BotIO(PLAYERDATA_DB) {

        @Override
        public void doRead(DataInputStream in) throws Exception {
            for (int i = 0; i < UserService.maxuser; i++) {
                String handle = in.readLine();
                User user = new User(bot, i, handle);
                users[i] = user;
                if (handle.equalsIgnoreCase("ffinalplaya")) {
                    maxID = i;
                    break;
                }
            }
        }

        @Override
        public void doWrite(DataOutputStream out) throws Exception {
            for (int j = 0; j < maxID; j++) {
                out.writeBytes(users[j].handle);
                out.writeByte('\n');
            }
            out.writeBytes("ffinalplaya");
            out.writeByte('\n');
        }
    };

    private BotIO playerVarsIO = new BotIO(PLAYERVARS_DB) {

        @Override
        public void doRead(DataInputStream in) throws Exception {
            for (int i = 0; i < maxID; i++) {
                users[i].numbertourneysbet = in.readInt();
                users[i].totalturnover = in.readFloat();
                users[i].messagevar = in.readBoolean();
                users[i].nonoisevar = in.readBoolean();
                users[i].eligible = in.readBoolean();
                users[i].jin = in.readBoolean();
                users[i].simOpen = in.readBoolean();
                users[i].unpaidFT = in.readBoolean();
                users[i].amountvar = in.readInt();
                users[i].botvar = in.readInt();
                users[i].initMasterPoints(in.readInt());
                users[i].recordChips = in.readFloat();
                users[i].selfBetType = in.readInt();
                users[i].selfBetAmount = in.readInt();
                users[i].slotProfits = in.readInt();
                users[i].slotTurnover = in.readInt();
                users[i].weeklySlotProfits = in.readInt();
                users[i].weeklySlotTurnover = in.readInt();
                users[i].monthlyFuturesTurnover = in.readInt();
                users[i].doubleintmultiplier = in.readFloat();
                int commentLength = in.readInt();
                for (int j = 0; j < commentLength; j++) {
                    users[i].comments.addElement(new String(in.readLine()));
                }
            }
        }

        @Override
        public void doWrite(DataOutputStream out) throws Exception {
            for (int i = 0; i < maxID; i++) {
                out.writeInt(users[i].numbertourneysbet);
                out.writeFloat(users[i].totalturnover);
                out.writeBoolean(users[i].messagevar);
                out.writeBoolean(users[i].nonoisevar);
                out.writeBoolean(users[i].eligible);
                out.writeBoolean(users[i].jin);
                out.writeBoolean(users[i].simOpen);
                out.writeBoolean(users[i].unpaidFT);
                out.writeInt(users[i].amountvar);
                out.writeInt(users[i].botvar);
                out.writeInt(users[i].getMasterPoints());
                out.writeFloat(users[i].recordChips);
                out.writeInt(users[i].selfBetType);
                out.writeInt(users[i].selfBetAmount);
                out.writeInt(users[i].slotProfits);
                out.writeInt(users[i].slotTurnover);
                out.writeInt(users[i].weeklySlotProfits);
                out.writeInt(users[i].weeklySlotTurnover);
                out.writeInt(users[i].monthlyFuturesTurnover);
                out.writeFloat(users[i].doubleintmultiplier);
                int commentLength = users[i].comments.size();
                out.writeInt(commentLength);
                for (String comment : users[i].comments) {
                    out.writeBytes(comment);
                    out.writeByte('\n');
                }
            }
        }
    };

    private BotIO weeklyChipsIO = new BotIO(CHIPWEEK_DB) {

        @Override
        public void doRead(DataInputStream in) throws Exception {
            bot.weekday3 = in.readInt();
            bot.weekday2 = in.readInt();
            bot.weekhour = in.readInt();
            bot.weekminute = in.readInt();
            int weeknumber = in.readInt();
            for (int i = 0; i < weeknumber; i++) {
                users[i].chipsLastWeek = in.readFloat();
            }
            for (int i = weeknumber; i < maxID; i++) {
                if (users[i].unpaidFT) {
                    users[i].chipsLastWeek = initialEndowmentFreeTrial;
                } else {
                    users[i].chipsLastWeek = initialEndowment;
                }
            }
        }

        @Override
        public void doWrite(DataOutputStream out) throws Exception {
            out.writeInt(bot.weekday3);
            out.writeInt(bot.weekday2);
            out.writeInt(bot.weekhour);
            out.writeInt(bot.weekminute);
            out.writeInt(maxID);
            for (int i = 0; i < maxID; i++) {
                out.writeFloat(users[i].chipsLastWeek);
            }
        }
    };

    private abstract class BotIO extends QuietFileHelper {

        public BotIO(File file) {
            super(file);
        }

        @Override
        protected void onException(Throwable e) {
            bot.logException(e);
        }
    }

    private BotIO universalIO = new BotIO(PLAYERS_TXT) {

        @Override
        public void doRead(BufferedReader in) throws Exception {
            DataProperties data = new DataProperties(in);
            weekwinner = data.getString("champ.weekly");
            monthwinner = data.getString("champ.month");
            bot.monthmonth = data.getInteger("monthly.month");
            bot.monthday = data.getInteger("monthly.month");
            bot.monthhour = data.getInteger("monthly.month");
            bot.monthminute = data.getInteger("monthly.month");
            bot.weekday3 = data.getInteger("weekly.weekday3");
            bot.weekday2 = data.getInteger("weekly.weekday2");
            bot.weekhour = data.getInteger("weekly.hour");
            bot.weekminute = data.getInteger("weekly.minute");
            for (maxID = 0; maxID < maxuser; maxID++) {
                String handle = data.getString("player.{1}.handle", maxID);
                if (handle == null) break;
                User user = new User(bot, maxID, handle);
                user.actualChips = data.getFloat("player.{1}.actualChips", maxID);
                user.futures = data.getInteger("player.{1}.futures", maxID);
                user.chipsLastMonth = data.getFloat("player.{1}.chipsLastMonth", maxID);
                user.chipsLastWeek = data.getFloat("player.{1}.chipsLastWeek", maxID);
                user.initMasterPoints(data.getInteger("player.{1}.masterPoints", maxID));
                user.numbertourneysbet = data.getInteger("player.{1}.numbertourneysbet", maxID);
                user.totalturnover = data.getFloat("player.{1}.totalturnover", maxID);
                user.messagevar = data.getBoolean("player.{1}.messagevar", maxID);
                user.nonoisevar = data.getBoolean("player.{1}.nonoisevar", maxID);
                user.eligible = data.getBoolean("player.{1}.eligible", maxID);
                user.jin = data.getBoolean("player.{1}.jin", maxID);
                user.simOpen = data.getBoolean("player.{1}.simOpen", maxID);
                user.unpaidFT = data.getBoolean("player.{1}.unpaidFT", maxID);
                user.amountvar = data.getInteger("player.{1}.amountvar", maxID);
                user.botvar = data.getInteger("player.{1}.botvar", maxID);
                user.recordChips = data.getFloat("player.{1}.recordChips", maxID);
                user.selfBetType = data.getInteger("player.{1}.selfBetType", maxID);
                user.selfBetAmount = data.getInteger("player.{1}.selfBetAmount", maxID);
                user.slotProfits = data.getInteger("player.{1}.slotProfits", maxID);
                user.slotTurnover = data.getInteger("player.{1}.slotTurnover", maxID);
                user.weeklySlotProfits = data.getInteger("player.{1}.weeklySlotProfits", maxID);
                user.weeklySlotTurnover = data.getInteger("player.{1}.weeklySlotTurnover", maxID);
                user.monthlyFuturesTurnover = data.getInteger("player.{1}.monthlyFuturesTurnover", maxID);
                user.doubleintmultiplier = data.getFloat("player.{1}.doubleintmultiplier", maxID);
                user.comments = data.getStringList("player.{1}.comment", maxID);
                users[maxID] = user;
            }
        }

        @Override
        public void doWrite(PrintWriter out) throws Exception {
            out.format("champ.weekly=%s%n", weekwinner);
            out.format("champ.month=%s%n", monthwinner);
            out.format("monthly.month=%d%n", bot.monthmonth);
            out.format("monthly.month=%d%n", bot.monthday);
            out.format("monthly.month=%d%n", bot.monthhour);
            out.format("monthly.month=%d%n", bot.monthminute);
            out.format("weekly.weekday3=%d%n", bot.weekday3);
            out.format("weekly.weekday2=%d%n", bot.weekday2);
            out.format("weekly.hour=%d%n", bot.weekhour);
            out.format("weekly.minute=%d%n", bot.weekminute);
            for (int id = 0; id < maxID; id++) {
                User user = users[id];
                out.println();
                out.format("player.%d.handle=%s%n", id, user.handle);
                out.format("player.%d.actualChips=%f%n", id, user.actualChips);
                out.format("player.%d.futures=%d%n", id, user.futures);
                out.format("player.%d.chipsLastMonth=%f%n", id, user.chipsLastMonth);
                out.format("player.%d.chipsLastWeek=%f%n", id, user.chipsLastWeek);
                out.format("player.%d.masterPoints=%d%n", id, user.getMasterPoints());
                out.format("player.%d.numbertourneysbet=%d%n", id, user.numbertourneysbet);
                out.format("player.%d.totalturnover=%f%n", id, user.totalturnover);
                out.format("player.%d.messagevar=%b%n", id, user.messagevar);
                out.format("player.%d.nonoisevar=%b%n", id, user.nonoisevar);
                out.format("player.%d.eligible=%b%n", id, user.eligible);
                out.format("player.%d.jin=%b%n", id, user.jin);
                out.format("player.%d.simOpen=%b%n", id, user.simOpen);
                out.format("player.%d.unpaidFT=%b%n", id, user.unpaidFT);
                out.format("player.%d.amountvar=%d%n", id, user.amountvar);
                out.format("player.%d.botvar=%d%n", id, user.botvar);
                out.format("player.%d.recordChips=%f%n", id, user.recordChips);
                out.format("player.%d.selfBetType=%d%n", id, user.selfBetType);
                out.format("player.%d.selfBetAmount=%d%n", id, user.selfBetAmount);
                out.format("player.%d.slotProfits=%d%n", id, user.slotProfits);
                out.format("player.%d.slotTurnover=%d%n", id, user.slotTurnover);
                out.format("player.%d.weeklySlotProfits=%d%n", id, user.weeklySlotProfits);
                out.format("player.%d.weeklySlotTurnover=%d%n", id, user.weeklySlotTurnover);
                out.format("player.%d.monthlyFuturesTurnover=%d%n", id, user.monthlyFuturesTurnover);
                out.format("player.%d.doubleintmultiplier=%f%n", id, user.doubleintmultiplier);
                int commentNum = 0;
                for (String comment : user.comments) {
                    commentNum++;
                    out.format("player.%d.comment.%d=%s%n", id, commentNum, comment);
                }
            }
        }
    };

    private static final class DataProperties extends Properties {

        private static final long serialVersionUID = 3392258123214515298L;

        public DataProperties(Reader in) throws IOException {
            super();
            load(in);
        }

        private String format(String key, Object... args) {
            return key.replaceFirst("\\{1\\}", String.valueOf(args[0]));
        }

        public int getInteger(String key, Object... args) {
            if (args.length > 0) {
                key = format(key, args);
            }
            String value = getProperty(key);
            return Integer.valueOf(value);
        }

        public float getFloat(String key, Object... args) {
            if (args.length > 0) {
                key = format(key, args);
            }
            String value = getProperty(key);
            return Float.valueOf(value);
        }

        public boolean getBoolean(String key, Object... args) {
            if (args.length > 0) {
                key = format(key, args);
            }
            String value = getProperty(key);
            return Boolean.valueOf(value);
        }

        public String getString(String key, Object... args) {
            if (args.length > 0) {
                key = format(key, args);
            }
            String value = getProperty(key);
            return value;
        }

        public Vector<String> getStringList(String key, Object... args) {
            if (args.length > 0) {
                key = format(key, args);
            }
            Vector<String> list = new Vector<String>();
            for (int i = 1; true; i++) {
                String s = key + "." + i;
                String value = getProperty(s);
                if (value == null) break;
                list.add(value);
            }
            return list;
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Insufficient arguments");
            return;
        }
        boolean binary1 = "binary".equals(args[0]);
        boolean binary2 = "binary".equals(args[1]);
        BettingBot bot = new BettingBot(null);
        UserService service = new UserService(bot);
        if (binary1) {
            service.load();
        } else {
            service.newLoad();
        }
        if (binary2) {
            service.save();
        } else {
            service.newSave();
        }
    }
}
