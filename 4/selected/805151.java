package jelb;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import jelb.commands.Command;
import jelb.commands.account.GetCommand;
import jelb.commands.account.MoveCommand;
import jelb.commands.account.PutCommand;
import jelb.commands.account.ShowCommand;
import jelb.commands.bot.BotAddGuildMember;
import jelb.commands.bot.BotGetCommand;
import jelb.commands.bot.BotMoveCommand;
import jelb.commands.bot.BotOrderBuyCommand;
import jelb.commands.bot.BotOrderCancelCommand;
import jelb.commands.bot.BotOrderSellCommand;
import jelb.commands.bot.BotOrdersListCommand;
import jelb.commands.bot.BotShowCommand;
import jelb.commands.chat.JoinChannelCommand;
import jelb.commands.chat.LeaveChannelCommand;
import jelb.commands.chat.SayCommand;
import jelb.commands.common.AliasCommand;
import jelb.commands.common.AllInventoryCommand;
import jelb.commands.common.CommandCommand;
import jelb.commands.common.DebugCommand;
import jelb.commands.common.GiveCommand;
import jelb.commands.common.HelpCommand;
import jelb.commands.common.InfoCommand;
import jelb.commands.common.LoadCommand;
import jelb.commands.common.LocateCommand;
import jelb.commands.common.RebootCommand;
import jelb.commands.common.SaveCommand;
import jelb.commands.common.StatsCommand;
import jelb.commands.common.TellCommand;
import jelb.commands.common.WearCommand;
import jelb.commands.guild.AcceptMeCommand;
import jelb.commands.guild.GuildCommand;
import jelb.commands.guild.GuildMembersAdminCommand;
import jelb.commands.guild.GuildMembersCommand;
import jelb.commands.guild.RemoveMeCommand;
import jelb.commands.privilege.BanCommand;
import jelb.commands.privilege.BanListCommand;
import jelb.commands.privilege.PrivilegeCommand;
import jelb.commands.privilege.PrivilegeCommandCommand;
import jelb.commands.privilege.PrivilegeGroupCommand;
import jelb.commands.privilege.PrivilegePlayerCommand;
import jelb.commands.privilege.UnbanCommand;
import jelb.commands.trade.BuyCommand;
import jelb.commands.trade.DonateCommand;
import jelb.commands.trade.InventoryCommand;
import jelb.commands.trade.OrderBuyCommand;
import jelb.commands.trade.OrderCancelCommand;
import jelb.commands.trade.OrderSellCommand;
import jelb.commands.trade.OrdersListCommand;
import jelb.commands.trade.PriceCommand;
import jelb.commands.trade.PriceListCommand;
import jelb.commands.trade.WantedCommand;
import jelb.common.Actor;
import jelb.common.ActorList;
import jelb.common.IBotProto;
import jelb.common.IChatter;
import jelb.common.ICommandable;
import jelb.common.IEventHandler;
import jelb.common.IGuildKeeper;
import jelb.common.IInfoFileProvider;
import jelb.common.IItemHandler;
import jelb.common.IItemsKeeper;
import jelb.common.ILocateable;
import jelb.common.IMobsObserver;
import jelb.common.IPrivilegeable;
import jelb.common.IStoreable;
import jelb.common.Inventory;
import jelb.common.Item;
import jelb.common.MapLocation;
import jelb.common.Mob;
import jelb.common.Parser;
import jelb.common.Player;
import jelb.common.PlayerList;
import jelb.features.Chatting;
import jelb.features.ItemsKeeping;
import jelb.features.MobsObserving;
import jelb.jobs.InfoFileGenerator;
import jelb.jobs.PhpBbScaner;
import jelb.jobs.Ticker;
import jelb.jobs.Updater;
import jelb.messaging.GetTradeAccept;
import jelb.messaging.GetTradeExit;
import jelb.messaging.GetTradeObject;
import jelb.messaging.GetTradeReject;
import jelb.messaging.HereYourInventory;
import jelb.messaging.HereYourStats;
import jelb.messaging.IMessageListener;
import jelb.messaging.LogInOk;
import jelb.messaging.NewMinute;
import jelb.messaging.RawText;
import jelb.messaging.RemoveTradeObject;
import jelb.netio.Message;
import jelb.netio.Protocol;
import jelb.netio.Uint8;
import jelb.netio.Protocol.Channel;
import jelb.netio.Protocol.Emote;
import jelb.struct.Accounts;
import jelb.struct.ChatableUser;
import jelb.struct.DataManager;
import jelb.struct.GuildMember;
import jelb.struct.GuildMembers;
import jelb.struct.OrdersList;
import jelb.struct.OrdersListItem;
import jelb.struct.Privilege;
import jelb.struct.ChatableUser.AnserwGateway;
import jelb.trade.BuyEvent;
import jelb.trade.Event;
import jelb.trade.TradeContainer;
import jelb.www.MicroWWWServer;
import jelb.xmpp.Client;
import jelb.xmpp.Client.IncomingMessage;

public class TradeBot extends BotProto implements IEventHandler<Client, IncomingMessage>, ICommandable, IPrivilegeable, IStoreable, ILocateable, IItemHandler, IChatter, IInfoFileProvider, IGuildKeeper, IItemsKeeper, IMobsObserver {

    private ItemsKeeping itemsKeeping;

    private MobsObserving mobsObserving;

    private Chatting chatting;

    public Command<?>[] Commands = { new HelpCommand("help", "help [command] - displays help"), new LocateCommand("loc", "loc  - displays my location"), new DebugCommand("dbg", "dbg  - some debug info"), new GiveCommand("give", "give - <quantity> <item> - gives you an item"), new AllInventoryCommand("ainv", "ainv - [item name] - displays all items in inventory"), new InfoCommand("info", "info - displays some information about me"), new BotAddGuildMember("acceptgm", "acceptgm add|del <name> - adds or remove guild members from the guild"), new RebootCommand("reboot", "reboot <poweroff> - makes an reboot"), new LoadCommand("load", "load - load all configuration files"), new SaveCommand("save", "save - saves configuration"), new TellCommand("tell", "tell <nick> <message> - sends a message to nick"), new StatsCommand("stats", "stats - accounts and inventory stats"), new PrivilegeCommand("prv", "prv  - (add|del) <command> <nick> - add|remove privilege"), new PrivilegePlayerCommand("pprv", "pprv - <group> (add|del) <nick> - add|del nick to group"), new PrivilegeGroupCommand("gprv", "gprv - (add|del|list) <name> - add|del|list group|s"), new PrivilegeCommandCommand("cprv", "cprv - <command name> (add|del) <group name>|<list> - grant|revoke group to command"), new BanCommand("ban", "ban  - <nick> add ban to player"), new UnbanCommand("unban", "unban- <nick> removes ban from player"), new BanListCommand("banl", "banl - list banned players"), new InventoryCommand("inv", "inv  - [item name] - display what i want to sell"), new BuyCommand("buy", "buy  - <quantity> <item> - buy an item"), new PriceCommand("price", "price- (<add> <emu> <stackable>)|(del) <item name>)"), new WantedCommand("wanted", "wanted- [item name] - displays what i want to buy"), new OrderBuyCommand("bo", "bo   - <price> [quantity] <item name> - gives me an order to buy an item"), new OrderSellCommand("so", "so   - <price> [quantity] <item name> - gives me an order to sell yours items"), new OrdersListCommand("lo", "lo   - [item name] - send you yours orders state"), new OrderCancelCommand("co", "co   - <order number> - cancel order of given number (use lo to display numbers)"), new CommandCommand("cmd", "cmd  - <command text> do command"), new PriceListCommand("plst", "plst - [item name] - list price|s"), new AliasCommand("alias", "alias- (add [alias]/|del|show) [base name]"), new GuildCommand("guild", "guild-<info>|(<treasure>(<put>|<get>)) displays info about guild"), new GuildMembersCommand("m", "m    - displays guild members curently online"), new GuildMembersAdminCommand("am", "am   - show|(add|del|suspend|activate) <name> - show|add|remove|suspend|activate guild members"), new DonateCommand("donate", "donate - puts the content of the tradewindow into the guilds own account"), new PutCommand("put", "put  - adds items to your account"), new GetCommand("get", "get  - <quantity> <item name> - give you item from your account"), new ShowCommand("acc", "acc  - lists your account"), new MoveCommand("move", "move - <account> [quantity] <item name> - moves an item from your account to another"), new BotOrderBuyCommand("botbuyorder", "botbuyorder   - <price> [quantity] <item name> - gives me an order to buy an item from my money"), new BotOrderSellCommand("botsellorder", "botsellorder   - <price> [quantity] <item name> - gives me an order to sell my items"), new BotOrdersListCommand("botlistorder", "botlistorder   - [item name] - send you my account orders state"), new BotOrderCancelCommand("botcancelorder", "botcancelorder   - <order number> - cancel my order of given number (use lo to display numbers)"), new BotShowCommand("botacc", "botacc  - lists the content of my account"), new BotMoveCommand("botmove", "botmove - <account> [quantity] <item name> - moves an item from my account to another"), new BotGetCommand("botget", "botget  - <quantity> <item name> - give you item from my account"), new WearCommand("wear", "wear - on|off <item> - dress or undress an item"), new RemoveMeCommand("removeme", "removeme - removes you from guild"), new AcceptMeCommand("acceptme", "acceptme - accepts you into guild") };

    public TradeBotData data;

    private PlayerList players;

    private Inventory inventory;

    private InfoFileGenerator infoFileGenerator;

    private Ticker tickerTradeCancel;

    private Ticker tickerGuildMoney;

    private Ticker tickerBoardcast;

    private Ticker tickerPlayerList;

    private Ticker tickerUpdate;

    private Ticker tickerForumScaner;

    private Ticker tickerMessanger;

    private Ticker tickerGenerateInfoFile;

    private MicroWWWServer wwwServer;

    private Client client;

    private String locator;

    private MapLocation lastLocationOnMap;

    private Event lastSuccessfullyTrade;

    private String somebodyWaitToTrade;

    private boolean reactionOnBalancedTradeWasSent;

    private Actor tradePartner;

    private Event tradeEvent;

    private String tradeLock;

    public final jelb.common.Event<TradeBot, Item> ItemInitialized = new jelb.common.Event<TradeBot, Item>();

    public long load;

    public String day;

    public String dayDesc;

    public int gameTimeHour;

    public int gameTimeMinute;

    class ItemInitalizeRethrow implements IEventHandler<IItemsKeeper, Item> {

        @Override
        public boolean handle(IItemsKeeper sender, Item args) {
            ItemInitialized.invoke((TradeBot) sender, args);
            return false;
        }
    }

    public TradeBot(String homePath) {
        super(homePath);
        this.data = new TradeBotData(this, new DataManager(this, homePath));
        this.loadConfig();
        this.players = new PlayerList(this);
        this.inventory = new Inventory();
        String wwwRoot = homePath == null ? "www/" : homePath + "/www/";
        this.wwwServer = new MicroWWWServer(wwwRoot, this, this.data.getConfig().getWwwServerPort());
        this.wwwServer.start();
        this.infoFileGenerator = new InfoFileGenerator(this);
        this.itemsKeeping = new ItemsKeeping(this);
        this.itemsKeeping.ItemInitialized.addListener(new ItemInitalizeRethrow());
        this.mobsObserving = new MobsObserving(this);
        this.chatting = new Chatting(this);
        this.addListener(new IMessageListener<LogInOk>() {

            @Override
            public LogInOk getMessageInstance() {
                return new LogInOk();
            }

            @Override
            public boolean handle(LogInOk message) {
                send(new Message(Protocol.LOCATE_ME));
                return false;
            }
        });
        this.addListener(new IMessageListener<RawText>() {

            @Override
            public RawText getMessageInstance() {
                return new RawText();
            }

            @Override
            public boolean handle(RawText message) {
                log(LogLevel.Out, message.getLog());
                if (client != null) {
                    Integer marketChannelIndex = chatting.getOpenChannelIndex(Channel.Market);
                    Integer guildChannelIndex = chatting.getOpenChannelIndex(Channel.Guild);
                    int channelId = 0;
                    int marketChannelId = getChannelId("market");
                    int guildChannelId = getChannelId("guild");
                    int gmChannelId = getChannelId("#gm");
                    switch(message.getChannel()) {
                        case Protocol.CHAT_CHANNEL1:
                            if (marketChannelIndex != null && marketChannelIndex == 0) channelId = marketChannelId;
                            if (guildChannelIndex != null && guildChannelIndex == 0) channelId = guildChannelId;
                            break;
                        case Protocol.CHAT_CHANNEL2:
                            if (marketChannelIndex != null && marketChannelIndex == 1) channelId = marketChannelId;
                            if (guildChannelIndex != null && guildChannelIndex == 1) channelId = guildChannelId;
                            break;
                        case Protocol.CHAT_CHANNEL3:
                            if (marketChannelIndex != null && marketChannelIndex == 2) channelId = marketChannelId;
                            if (guildChannelIndex != null && guildChannelIndex == 2) channelId = guildChannelId;
                            break;
                        case Protocol.CHAT_GM:
                            channelId = gmChannelId;
                            break;
                    }
                    for (GuildMember m : data.getGuild().members) {
                        if (m.isListenToChannel(channelId)) client.sendMessage(m.jid, message.getContent());
                    }
                }
                String s = null;
                if (message.isPresonalChat()) {
                    s = Parser.getPlayerNameFromPrivateMessage(message.getMessage());
                    if (s != null) {
                        String cnt = Parser.getContentOfPrivateMessage(message.getMessage());
                        respond(s, cnt);
                    }
                    return false;
                }
                if (message.isServerChat()) {
                    s = Parser.getPlayerNameFromTradeMessage(message.getMessage());
                    if (s != null) {
                        if (data.getPrivileges().isBanned(s)) {
                            appendAnserw(s, jelb.Locale.MSG_BAN_INFO);
                            send(new Message(Protocol.REJECT_TRADE));
                            sendExitTrade();
                        } else {
                            log(LogLevel.Debug, "Trade request from:" + s);
                            if (data.getConfig().autoSit) send(new Message(Protocol.SIT_DOWN, new Uint8(0)));
                            somebodyWaitToTrade = s;
                        }
                        return false;
                    }
                    if (message.getContent().startsWith("You are in", 0)) {
                        lastLocationOnMap = new MapLocation(message.getContent());
                        if (locator != null) {
                            StringBuffer sb = new StringBuffer();
                            sb.append("Im located in ");
                            sb.append(lastLocationOnMap.getMapName());
                            sb.append(" at ");
                            sb.append(lastLocationOnMap.getLocation());
                            sendResponseByPm(locator, sb);
                            locator = null;
                        }
                        return false;
                    }
                    if (message.getContent().startsWith("Just an ordinary day") || message.getContent().startsWith("Today is a special day:")) {
                        send(new Message(Protocol.SIT_DOWN, new Uint8(0)));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                        if (message.getContent().startsWith("Just an ordinary day")) {
                            sendEmote(Emote.Stretch);
                        } else {
                            sendEmote(Emote.Jump);
                        }
                        dayDesc = message.getContent();
                        if (client != null) {
                            client.sendPresence(dayDesc.length() > 50 ? dayDesc.substring(0, 47) + "..." : dayDesc, true);
                            for (GuildMember m : data.getGuild().members) if (m.isListenToChannel(getChannelId("#day"))) client.sendMessage(m.jid, dayDesc);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                        send(new Message(Protocol.SIT_DOWN, new Uint8(1)));
                    }
                    if (message.getContent().startsWith("Game Date:")) day = message.getContent();
                }
                return false;
            }
        });
        this.addListener(new IMessageListener<NewMinute>() {

            @Override
            public NewMinute getMessageInstance() {
                return new NewMinute();
            }

            @Override
            public boolean handle(NewMinute message) {
                gameTimeHour = message.getHour();
                gameTimeMinute = message.getMinute();
                if (getGameDate() == "Uninitialized") {
                    send(new Message(Protocol.GET_DATE));
                }
                if (gameTimeHour == 6 && gameTimeMinute == 0) {
                    dayDesc = null;
                    send(new Message(Protocol.GET_DATE));
                    send(new Message(Protocol.RAW_TEXT, "#day"));
                }
                if (client != null && gameTimeHour == 5 && (gameTimeMinute > 54 || gameTimeMinute == 45 || gameTimeMinute == 30)) {
                    for (GuildMember m : data.getGuild().members) {
                        if (m.isListenToChannel(getChannelId("#day"))) {
                            client.sendMessage(m.jid, "New day will start in " + (60 - gameTimeMinute) + " minutes");
                        }
                    }
                }
                if (gameTimeHour == 5 && gameTimeMinute == 59 && data.getConfig().boardcastDayEnd) {
                    send(new Message(Protocol.SIT_DOWN, new Uint8(0)));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                    sendGuildMessage("New day will start in 1 minute.");
                    sendEmote(Emote.CheerRight);
                }
                return false;
            }
        });
        this.addListener(new IMessageListener<HereYourStats>() {

            @Override
            public HereYourStats getMessageInstance() {
                return new HereYourStats();
            }

            @Override
            public boolean handle(HereYourStats message) {
                load = message.getLoad();
                return false;
            }
        });
        this.addListener(new IMessageListener<HereYourInventory>() {

            @Override
            public HereYourInventory getMessageInstance() {
                return new HereYourInventory();
            }

            @Override
            public boolean handle(HereYourInventory message) {
                if (lastSuccessfullyTrade != null) {
                    Message commitMsg = lastSuccessfullyTrade.commit();
                    if (commitMsg != null) send(commitMsg);
                    lastSuccessfullyTrade = null;
                }
                return false;
            }
        });
        this.addListener(new IMessageListener<GetTradeObject>(this) {

            @Override
            public GetTradeObject getMessageInstance() {
                return new GetTradeObject();
            }

            @Override
            public boolean handle(GetTradeObject message) {
                reactionOnBalancedTradeWasSent = false;
                scratchHeadOnBalancingTradeWasSent = false;
                shakeHeadOnBalancingTradeWasSent = false;
                if (message.getWho().toByte() == Protocol.YOU) {
                    if (tradeEvent == null && tradePartner != null) {
                        appendAnserw(tradePartner.getName(), "Hi lets do buisness");
                        tradeEvent = new BuyEvent((TradeBot) this.args, tradePartner);
                    }
                }
                if (tradeEvent == null) log(LogLevel.Debug, "Getting trade object with null trade event");
                return false;
            }
        });
        this.addListener(new IMessageListener<RemoveTradeObject>() {

            @Override
            public RemoveTradeObject getMessageInstance() {
                return new RemoveTradeObject();
            }

            @Override
            public boolean handle(RemoveTradeObject message) {
                reactionOnBalancedTradeWasSent = false;
                scratchHeadOnBalancingTradeWasSent = false;
                shakeHeadOnBalancingTradeWasSent = false;
                if (tradeEvent == null) log(LogLevel.Debug, "Getting remove trade object with null trade event");
                return false;
            }
        });
        this.addListener(new IMessageListener<GetTradeReject>() {

            @Override
            public GetTradeReject getMessageInstance() {
                return new GetTradeReject();
            }

            @Override
            public boolean handle(GetTradeReject message) {
                if (tradeEvent != null) {
                    if (message.getWho().toByte() == Protocol.YOU) {
                        tradeEvent.setYouAccept(0);
                    } else tradeEvent.setMeAccept(0);
                }
                return false;
            }
        });
        this.addListener(new IMessageListener<GetTradeExit>() {

            @Override
            public GetTradeExit getMessageInstance() {
                return new GetTradeExit();
            }

            @Override
            public boolean handle(GetTradeExit message) {
                log(LogLevel.Debug, "<trade exit");
                reactionOnBalancedTradeWasSent = false;
                if (tradeEvent != null) {
                    if (tradeEvent.isAcceptedAndConfirmed()) {
                        lastSuccessfullyTrade = tradeEvent;
                    } else {
                        sendEmote(Emote.Shrug);
                        tradeEvent = null;
                    }
                }
                if (data.getConfig().autoSit) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                    }
                    send(new Message(Protocol.SIT_DOWN, new Uint8(1)));
                }
                tradeEvent = null;
                tradePartner = null;
                tickerTradeCancel.setSuspended(true);
                return false;
            }
        });
        this.addListener(new IMessageListener<GetTradeAccept>() {

            @Override
            public GetTradeAccept getMessageInstance() {
                return new GetTradeAccept();
            }

            @Override
            public boolean handle(GetTradeAccept message) {
                log(LogLevel.Debug, "<trade acccept:");
                if (tradeEvent == null) log(LogLevel.Error, "Getting trade accept with null trade event"); else {
                    if (message.getWho().toByte() == Protocol.YOU) {
                        tradeEvent.setYouAccept(tradeEvent.getYoursAcceptactionsCount() + 1);
                        jelb.trade.State tradeState = tradeEvent.balanceTrade();
                        if (tradeState.getCurrentState() == jelb.trade.State.States.Balanced) {
                            sendTradeAccept();
                        }
                    } else tradeEvent.setMeAccept(tradeEvent.getMineAcceptactionsCount() + 1);
                }
                return false;
            }
        });
    }

    public TradeBot() {
        this(null);
    }

    public GuildMembers getGuildMembers() {
        return this.data.getGuild().getMembers();
    }

    public Accounts getAccounts() {
        return this.data.getAccounts();
    }

    public void deliverMessage(jelb.struct.Message message) {
        this.data.getMessages().syncAdd(message);
    }

    public TradeContainer getMineTradeContainer() {
        if (this.tradeEvent != null) return this.tradeEvent.getMineTradeContainer();
        return null;
    }

    public TradeContainer getYoursTradeContainer() {
        if (this.tradeEvent != null) return this.tradeEvent.getYoursTradeContainer();
        return null;
    }

    public long getFreeLoad() {
        long emu = this.inventory.getEmu();
        return this.load - emu;
    }

    public long getMaxLoad() {
        return this.load;
    }

    @Override
    public String getHost() {
        return this.data.getConfig().getHost();
    }

    @Override
    public String getLogin() {
        return this.data.getConfig().getPlayerName();
    }

    @Override
    public String getPassword() {
        return this.data.getConfig().getPlayerPassword();
    }

    @Override
    public int getPort() {
        return this.data.getConfig().getPort();
    }

    public int getFreeSlots() {
        return 36 - this.inventory.getFullSlots();
    }

    public int getWwwRequests() {
        return this.wwwServer.getRequests();
    }

    public String getGameTime() {
        String hour = new Integer(this.gameTimeHour).toString();
        String minute = new Integer(this.gameTimeMinute).toString();
        if (this.gameTimeMinute < 10) minute = "0" + minute;
        return hour + ":" + minute;
    }

    public void syncListeners() {
        if (this.tickerMessanger != null) this.tickerMessanger.set(this.data.getMessages());
        if (this.tickerBoardcast != null) this.tickerBoardcast.set(this.data.getConfig());
        if (this.tickerGuildMoney != null) this.tickerGuildMoney.set(this.data.getGuild());
    }

    public String getGameDate() {
        if (this.day == null || this.day.length() < 32) return "Uninitialized";
        int day = Integer.parseInt(this.day.substring(22, 24));
        int month = Integer.parseInt(this.day.substring(25, 27));
        String year = this.day.substring(28, 32);
        return Locale.dateString.replace("{day}", Locale.days[day - 1]).replace("{month}", Locale.months[month - 1]).replace("{year}", year);
    }

    public String getGameDayDesc() {
        if (this.dayDesc == null) return "Just an ordinary day.<br />";
        return this.dayDesc.replace("\n", "<br />");
    }

    public String getUpdateUrl() {
        return this.data.getConfig().getUpdateUrl();
    }

    private void initTrade(Actor a) {
        this.send(new Message(Protocol.TRADE_WITH, a.getId()));
        somebodyWaitToTrade = null;
        this.tradePartner = a;
        this.tickerTradeCancel.setSuspended(false);
    }

    private List<Command<? extends IBotProto>> tmpCmds;

    public java.util.Collection<Command<? extends IBotProto>> getCommands() {
        if (this.tmpCmds == null) {
            this.tmpCmds = new Vector<Command<? extends IBotProto>>();
            this.tmpCmds.addAll(java.util.Arrays.asList((Command<? extends IBotProto>[]) this.Commands));
        }
        return this.tmpCmds;
    }

    private List<Command<? extends IBotProto>> tmpChatCmds;

    public java.util.Collection<Command<? extends IBotProto>> getChatCommands() {
        if (this.tmpChatCmds == null) {
            this.tmpChatCmds = new Vector<Command<? extends IBotProto>>();
            this.tmpChatCmds.add(new JoinChannelCommand("jc", "#jc - <channel>"));
            this.tmpChatCmds.add(new LeaveChannelCommand("lc", "#lc - <channel>"));
            this.tmpChatCmds.add(new SayCommand("@", "@ <message>"));
            this.tmpChatCmds.add(new jelb.commands.chat.InfoCommand("info", "info"));
        }
        return this.tmpChatCmds;
    }

    public void sendTradeAccept() {
        byte[] att = new byte[1 + this.getYoursTradeContainer().size()];
        att[0] = Protocol.ME;
        for (int i = 0; i < this.getYoursTradeContainer().size(); i++) {
            if (this.getYoursTradeContainer().get(i) != null) {
                att[i + 1] = this.getYoursTradeContainer().get(i).getFlags().toByte();
            } else att[i + 1] = (byte) 0;
        }
        if (this.tradeEvent.getMineAcceptactionsCount() < 2) {
            this.send(new Message(Protocol.ACCEPT_TRADE, new String(att)));
        }
    }

    public void sendExitTrade(String msg) {
        if (msg != null) {
            String name = (this.tradePartner == null) ? this.tradeLock : this.tradePartner.getName();
            if (name != null) this.appendAnserw(name, msg);
        }
        this.send(new Message(Protocol.EXIT_TRADE));
        this.tradePartner = null;
        this.tradeEvent = null;
        this.tradeLock = null;
        this.tickerTradeCancel.setSuspended(true);
    }

    public void sendExitTrade() {
        this.sendExitTrade(null);
    }

    public void sendLocationTo(String playerName) {
        this.locator = playerName;
        this.send(new Message(Protocol.LOCATE_ME));
    }

    public void sendLocalChat(String content) {
        this.send(new Message(Protocol.RAW_TEXT, content));
    }

    public void sendGuildMessage(String content) {
        StringBuilder sb = new StringBuilder("#gm ");
        sb.append(content);
        this.send(new Message(Protocol.RAW_TEXT, sb.toString()));
    }

    public void sendChatText(Channel channel, String message) {
        this.chatting.sayOnChannel(channel, message);
        if (channel == Channel.Market) this.chatting.leaveChannel(Channel.Market);
    }

    public void sendRawText(String content) {
        this.send(new RawText(content));
    }

    public Message preaparePm(String playerName, String content) {
        StringBuilder msg = new StringBuilder();
        msg.append(playerName);
        msg.append(" ");
        msg.append(content);
        return new Message(Protocol.SEND_PM, msg.toString());
    }

    public Actor getTardePartner() {
        return this.tradePartner;
    }

    public boolean isValidTradePartner(String playerName) {
        if (this.tradeLock != null) {
            this.appendAnserw(playerName, jelb.Locale.MSG_TRADE_LOCK_INFO);
            return false;
        }
        if (this.tradePartner == null) {
            this.appendAnserw(playerName, jelb.Locale.MSG_TRADE_WITH_ME_INFO);
            return false;
        }
        if (this.tradePartner.getName().equalsIgnoreCase(playerName)) return true; else {
            this.appendAnserw(playerName, jelb.Locale.MSG_BUSY_INFO);
            return false;
        }
    }

    public boolean lockTradePersonal(String playerName) {
        if ((this.tradePartner == null && this.tradeEvent == null) || this.tradePartner.getName().equalsIgnoreCase(playerName) && this.tradeLock == null) {
            this.tradeLock = playerName;
            return true;
        }
        this.appendAnserw(playerName, jelb.Locale.MSG_BUSY_INFO);
        return false;
    }

    public boolean lockTradeGlobal(String playerName) {
        if ((this.tradePartner == null && this.tradeEvent == null) && this.tradeLock == null) {
            this.tradeLock = playerName;
            return true;
        }
        this.appendAnserw(playerName, jelb.Locale.MSG_BUSY_INFO);
        return false;
    }

    public void unLockTrade() {
        this.tradeLock = null;
    }

    public String getVersion() {
        Package p = Package.getPackage("jelb");
        return p.getImplementationVersion();
    }

    class Responder implements IEventHandler<TradeBot, Item> {

        private String playerToRespond;

        private String message;

        public Responder(String player, String message) {
            this.playerToRespond = player;
            this.message = message;
        }

        public boolean handle(TradeBot sender, Item args) {
            if (inventory.isInitialized()) {
                appendAnserw(this.playerToRespond, Locale.MSG_READY);
                respond(playerToRespond, message);
                return true;
            }
            return false;
        }
    }

    private void respond(String playerName, String message) {
        if (this.players.isBot(playerName) && !playerName.equalsIgnoreCase("rraisa")) {
            return;
        }
        if (this.data.getPrivileges().isBanned(playerName)) {
            this.appendAnserw(playerName, jelb.Locale.MSG_BAN_INFO);
            this.sendEmote(Emote.ShakeHead);
            return;
        }
        if (!this.inventory.isInitialized()) {
            this.appendAnserw(playerName, Locale.MSG_GIVE_ME_SECOND);
            this.sendEmote(Emote.ScratchHead);
            this.ItemInitialized.addListener(new Responder(playerName, message));
            return;
        }
        Command<? extends IBotProto> cmd = Parser.parseCommand(this.getCommands(), message);
        if (cmd == null) {
            this.appendAnserw(playerName, String.format(Locale.MSG_DONT_UNDERSTAND, message, this.data.getConfig().getPlayerName()));
            this.sendEmote(Emote.ShakeHead);
        } else {
            String params[] = Parser.parseParams(message);
            if (!this.data.getPrivileges().isAllowed(playerName, cmd.getName())) {
                this.appendAnserw(playerName, String.format(Locale.MSG_NOT_PRIVILEGED, cmd.getName()));
            } else {
                boolean ok = true;
                if (cmd.isGlobalTradeLock()) ok = this.lockTradeGlobal(playerName);
                if (cmd.isPersonalTradeLock()) ok = this.lockTradePersonal(playerName);
                if (ok) {
                    cmd.tryInvoke(this, playerName, params);
                    if (this.HasNotSentEmoteInLastSecond()) this.sendEmote(Emote.NodHead);
                    if (cmd.isGlobalTradeLock() || cmd.isPersonalTradeLock()) this.unLockTrade();
                } else {
                    this.sendEmote(Emote.ShakeHead);
                }
            }
        }
    }

    public Event getTradeEvent() {
        return this.tradeEvent;
    }

    public void setTradeEvent(Event e) {
        this.tradeEvent = e;
        this.lastTradeState = jelb.trade.State.States.Balancing;
        this.emoteOnBalancingTradeLastSent = new Date();
        this.scratchHeadOnBalancingTradeWasSent = false;
        this.emoteOnBalancedTradeWasSent = false;
        this.shakeHeadOnBalancingTradeWasSent = false;
    }

    private boolean atLeastSecondPassed(Date from) {
        return new Date().getTime() - from.getTime() > 2500;
    }

    private jelb.trade.State.States lastTradeState;

    private Date emoteOnBalancingTradeLastSent = new Date();

    private Emote emoteOnBalancedTrade;

    private boolean emoteOnBalancedTradeWasSent;

    private boolean shakeHeadOnBalancingTradeWasSent;

    private boolean scratchHeadOnBalancingTradeWasSent;

    private void handle(jelb.trade.State state) {
        if (state.getCurrentState() != this.lastTradeState) {
            this.emoteOnBalancedTradeWasSent = false;
            this.shakeHeadOnBalancingTradeWasSent = false;
            this.scratchHeadOnBalancingTradeWasSent = false;
            this.emoteOnBalancingTradeLastSent = new Date();
            this.lastTradeState = state.getCurrentState();
        }
        switch(state.getCurrentState()) {
            case Balanced:
                if (!reactionOnBalancedTradeWasSent) {
                    if (state.getMessage() != null) this.appendAnserw(this.tradePartner.getName(), state.getMessage());
                    if (this.tradeEvent.getYoursAcceptactionsCount() == 0 && this.tradeEvent.getMineAcceptactionsCount() == 0) {
                        this.sendTradeAccept();
                    }
                    this.emoteOnBalancedTrade = new Random().nextBoolean() ? Emote.NodHead : Emote.ThumbsUp;
                    this.reactionOnBalancedTradeWasSent = true;
                }
                if (this.tradeEvent == null) this.emoteOnBalancedTrade = Emote.Shrug;
                if (!this.emoteOnBalancedTradeWasSent && this.atLeastSecondPassed(this.emoteOnBalancingTradeLastSent)) {
                    this.sendEmote(this.emoteOnBalancedTrade);
                    this.emoteOnBalancedTradeWasSent = true;
                }
                return;
            case Balancing:
                this.reactionOnBalancedTradeWasSent = false;
                if (this.atLeastSecondPassed(this.emoteOnBalancingTradeLastSent)) {
                    if (!this.scratchHeadOnBalancingTradeWasSent) {
                        sendEmote(Emote.ScratchHead);
                        this.scratchHeadOnBalancingTradeWasSent = true;
                        this.emoteOnBalancingTradeLastSent = new Date();
                    }
                    if (!this.tradeEvent.getYoursTradeContainer().isEmpty() && !this.shakeHeadOnBalancingTradeWasSent && this.atLeastSecondPassed(this.emoteOnBalancingTradeLastSent)) {
                        sendEmote(Emote.ShakeHead);
                        this.shakeHeadOnBalancingTradeWasSent = true;
                        this.emoteOnBalancingTradeLastSent = new Date();
                    }
                }
                return;
            case Exception:
                if (state.getMessage() != null) this.appendAnserw(this.tradePartner.getName(), state.getMessage());
                this.reactionOnBalancedTradeWasSent = false;
                sendEmote(Emote.Shrug);
                return;
            case CriticalException:
                this.sendExitTrade(state.getMessage());
                this.reactionOnBalancedTradeWasSent = false;
                sendEmote(Emote.FacePalm);
                return;
        }
    }

    protected void onInitalizing() {
        this.players.setUrl(this.data.getConfig().getPlayerListUrl(), this.data.getConfig().getBotListUrl());
        this.tickerGenerateInfoFile = new Ticker("generate info file", 60000 * 1, this.infoFileGenerator, this);
        this.tickerTradeCancel = new Ticker("timer trade cancel", 60000 * 2, this, this);
        this.tickerBoardcast = new Ticker("timer boardcast", 60000 * this.data.getConfig().getMessageDelay(), this.data.getConfig(), this);
        this.tickerGuildMoney = new Ticker("timer guild", 60000 * 60, this.data.getGuild(), this);
        this.tickerPlayerList = new Ticker("timer player list", 60000 * 1, this.players, this);
        this.tickerPlayerList.add(this);
        Updater u = new Updater(this.data.getConfig().getUpdateUrl(), this);
        this.tickerUpdate = new Ticker("timer update", 60000 * 6, u, this);
        this.tickerForumScaner = new Ticker("timer forum scaner", 60000 * 6, new PhpBbScaner(this, this.data.getConfig().phpBbHost, this.data.getConfig().phpBbUser, this.data.getConfig().phpBbPass), this);
        this.tickerMessanger = new Ticker("timer messanger", 5000, this.data.getMessages(), this);
        this.tickerGenerateInfoFile.setSuspended(false);
        this.tickerBoardcast.setSuspended(false);
        this.tickerGuildMoney.setSuspended(false);
        this.tickerPlayerList.setSuspended(false);
        this.tickerUpdate.setSuspended(!this.data.getConfig().autoUpdate);
        this.tickerForumScaner.setSuspended(false);
        this.tickerMessanger.setSuspended(false);
        if (this.data.getConfig().isJabberConfigured()) {
            this.client = new Client(this.data.getConfig().jabberHost, this.data.getConfig().jabberPort, this.data.getConfig().jabberUser, this.data.getConfig().jabberPassword, this.data.getConfig().jabberServer);
            this.client.NewMessage.addListener(this);
            this.client.setLogger(this);
            this.client.keepAlive();
        }
    }

    protected void onRebooting() {
        if (this.client != null) {
            this.client.sendEndStream();
        }
        this.tickerBoardcast.kill();
        this.tickerGuildMoney.kill();
        this.tickerPlayerList.kill();
        this.tickerUpdate.kill();
        this.tickerForumScaner.kill();
        this.tickerMessanger.kill();
        this.tickerGenerateInfoFile.kill();
    }

    protected void onDie() {
        this.log(LogLevel.Out, "saving config");
        this.data.save();
        this.log(LogLevel.Out, "done");
        this.wwwServer.kill();
    }

    protected void onDying() {
        this.sendGuildMessage(jelb.Locale.MSG_KILLED);
    }

    protected void onPulse() {
        if (somebodyWaitToTrade != null) {
            Actor a = this.mobsObserving.getVisibleMobs().getActor(somebodyWaitToTrade);
            if (a != null) {
                this.initTrade(a);
            } else {
                this.log(LogLevel.Error, "Actor " + somebodyWaitToTrade + " not found!!!");
                this.sendExitTrade();
            }
        }
        if (this.tradeEvent != null) {
            this.handle(this.tradeEvent.balanceTrade());
        }
    }

    protected void onDisconnecting() {
        this.mobsObserving.clear();
        this.locator = null;
        this.tradeEvent = null;
        if (this.tickerTradeCancel != null) this.tickerTradeCancel.setSuspended(true);
        if (this.inventory != null) this.inventory.clear();
        this.itemsKeeping.clear();
        this.reactionOnBalancedTradeWasSent = false;
    }

    @Override
    public void onTick(Ticker sender) {
        if (this.data.getConfig().greetGuildMembers) {
            if (sender == this.tickerPlayerList) {
                for (Player p : this.players.getLastTimeLogIns()) {
                    if (data.getGuild().members.getGuildMemeber(p.getNick()) != null) {
                        this.sendGuildMessage(String.format("Uik %s :)", p.getNick()));
                    }
                }
                for (Player p : this.players.getLastTimeLogOuts()) {
                    if (data.getGuild().members.getGuildMemeber(p.getNick()) != null) {
                        this.sendGuildMessage(String.format("Uikaj %s", p.getNick()));
                    }
                }
            }
        }
        super.onTick(sender);
    }

    @Override
    public void loadConfig() {
        this.data.load();
    }

    @Override
    public void saveConfig() {
        this.data.save();
    }

    @Override
    public Privilege getPrivileges() {
        return this.data.getPrivileges();
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    protected synchronized void sendResponse(String playerName, StringBuffer response) {
        ChatableUser user = this.getChatableUser(playerName);
        if (user == null) {
            this.sendResponseByPm(playerName, response);
        } else {
            switch(user.getGateway()) {
                case PM:
                    this.sendResponseByPm(playerName, response);
                    return;
                case XMPP:
                    this.client.sendMessage(user.getJid(), response.toString());
                    return;
            }
        }
    }

    private static final String[] availableChannels = new String[] { "market", "guild", "#gm", "#day" };

    public String[] getAvailablechannels() {
        return availableChannels;
    }

    public boolean handle(Client sender, IncomingMessage args) {
        if (!args.getType().equals("chat")) {
            this.log(LogLevel.Error, String.format("Error report on message Body:(%s) From:(%s) To:(%s) Type(%s)", args.getBody(), args.getFrom(), args.getTo(), args.getType()));
            return false;
        }
        GuildMember m = null;
        for (GuildMember gm : this.data.getGuild().members) {
            if (gm.hasJid() && args.getFrom().startsWith(gm.jid)) {
                m = gm;
                break;
            }
        }
        if (m != null) {
            m.setGateway(AnserwGateway.XMPP);
            String message = args.getBody();
            if (message.startsWith("@")) message = message.replaceFirst("@", "@ ");
            Command<? extends IBotProto> cmd = Parser.parseCommand(this.getChatCommands(), message);
            if (cmd != null) cmd.tryInvoke(this, m.getName(), Parser.parseParams(message)); else this.respond(m.getName(), args.getBody());
            this.flushAnserws();
            m.setGateway(AnserwGateway.PM);
        }
        return false;
    }

    public void sendXmppAddContact(String jid, String name) {
        if (this.client != null) {
            this.client.sendAddContact(jid, name);
            this.client.sendMessage(jid, String.format(Locale.MSG_WELCOME, this.data.getConfig().player));
        }
    }

    public String getInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append("Game time:");
        sb.append(this.getGameTime());
        sb.append("\n\n");
        sb.append(this.getGameDate());
        sb.append("\n");
        sb.append(this.getGameDayDesc().replace("<br />", "\n"));
        sb.append("\n");
        sb.append("Guild members online:");
        for (GuildMember gm : this.data.getGuild().members) {
            if (this.players.isOnline(gm.getName())) {
                sb.append(gm.getName());
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public ChatableUser getChatableUser(String playerName) {
        for (GuildMember member : this.data.getGuild().members) {
            if (member.name.equalsIgnoreCase(playerName)) {
                return member;
            }
        }
        return null;
    }

    public int getChannelId(String channel) {
        for (int i = 0; i < TradeBot.availableChannels.length; i++) {
            if (channel.equalsIgnoreCase(TradeBot.availableChannels[i])) {
                return (int) Math.pow(2, i);
            }
        }
        return -1;
    }

    @Override
    public String getContent() {
        String newLine = System.getProperty("line.separator");
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("botname,%s%s", this.data.getConfig().getPlayerName(), newLine));
        buffer.append(String.format("owner,%s%s", this.data.getConfig().getOwner(), newLine));
        buffer.append(String.format("location,%s,%d,%d%s", this.lastLocationOnMap.getMapName(), this.lastLocationOnMap.getLocation().getX(), this.lastLocationOnMap.getLocation().getY(), newLine));
        for (OrdersListItem buyOrderListItem : this.data.getOrders().getOffert(false)) {
            String price = Double.toString(buyOrderListItem.price).replace(",", ".");
            buffer.append(String.format("buying,%s,%d,%s%s", buyOrderListItem.name, buyOrderListItem.quantity, price, newLine));
        }
        for (OrdersListItem sellOrderListItem : this.data.getOrders().getOffert(true)) {
            String price = Double.toString(sellOrderListItem.getPrice()).replace(",", ".");
            buffer.append(String.format("selling,%s,%d,%s%s", sellOrderListItem.name, sellOrderListItem.quantity, price, newLine));
        }
        return buffer.toString();
    }

    @Override
    public String getOutputFilePath() {
        return new java.io.File(this.wwwServer.GetWwwRootPath(), this.data.getConfig().player + ".csv").getPath();
    }

    public PlayerList getPlayersOnline() {
        return this.players;
    }

    public ActorList getVisibleActors() {
        return this.mobsObserving.getVisibleMobs();
    }

    public OrdersList getOrders() {
        return this.data.getOrders();
    }

    @Override
    public void onMobAppear(Mob mob) {
        if (mob instanceof Actor) {
            sendEmote(Emote.WaveRight);
        }
    }

    @Override
    public void onMobDisapear(Mob mob) {
    }
}
