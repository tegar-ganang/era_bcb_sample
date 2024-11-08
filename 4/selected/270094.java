package net.sf.jerkbot.bot.impl;

import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.Session;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.MessageEvent;
import jerklib.listeners.DefaultIRCEventListener;
import jerklib.listeners.TaskCompletionListener;
import jerklib.util.NickServAuthPlugin;
import net.sf.jerkbot.JerkBotConstants;
import net.sf.jerkbot.bot.BotService;
import net.sf.jerkbot.commands.Command;
import net.sf.jerkbot.commands.CommandService;
import net.sf.jerkbot.commands.MessageContext;
import net.sf.jerkbot.commands.MessageContextService;
import net.sf.jerkbot.commands.UnknownCommand;
import net.sf.jerkbot.configuration.JerkBotConfiguration;
import net.sf.jerkbot.configuration.ProfileConfiguration;
import net.sf.jerkbot.configuration.ThreadPoolConfiguration;
import net.sf.jerkbot.exceptions.AccessDeniedException;
import net.sf.jerkbot.exceptions.CommandSyntaxException;
import net.sf.jerkbot.exceptions.ConfigurationException;
import net.sf.jerkbot.exceptions.JerkBotException;
import net.sf.jerkbot.interceptors.MessageInterceptorService;
import net.sf.jerkbot.resolvers.CommandResolverService;
import net.sf.jerkbot.session.UserSessionService;
import net.sf.jerkbot.util.FloodControl;
import net.sf.jerkbot.util.MessageUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.sf.jerkbot.session.UserKey;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 *         The irc bot itself that listens to incoming IRC messages
 * @version 0.0.1
 */
@Component(name = "JerkBot", immediate = true)
@Service(value = BotService.class)
public class BotServiceImpl extends DefaultIRCEventListener implements Runnable, BotService {

    private static final List<Character> triggers = new ArrayList<Character>();

    private static final String NO_MONOLOGUE = "Why would I talk to myself? Please explain.";

    private static final Logger Log = LoggerFactory.getLogger(BotServiceImpl.class.getName());

    private final FloodControl floodControl = new FloodControl(4L, TimeUnit.SECONDS);

    private Thread botThread;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "bindCommandService", unbind = "unbindCommandService", referenceInterface = CommandService.class)
    private CommandService commandService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "bindMessageInterceptorService", unbind = "unbindMessageInterceptorService", referenceInterface = MessageInterceptorService.class)
    private MessageInterceptorService messageInterceptorService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "bindCommandHintsService", unbind = "unbindCommandHintsService", referenceInterface = MessageContextService.class)
    private MessageContextService messageContextService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "bindUserSessionService", unbind = "unbindUserSessionService", referenceInterface = UserSessionService.class)
    private UserSessionService userSessionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "bindCommandResolverService", unbind = "unbindCommandResolverService", referenceInterface = CommandResolverService.class)
    private CommandResolverService commandResolverService;

    private final ThreadPoolExecutor executor = ThreadPoolConfiguration.createExecutor();

    private Session session;

    private Properties env = new Properties();

    protected void bindCommandHintsService(MessageContextService messageContextService) {
        this.messageContextService = messageContextService;
    }

    protected void unbindCommandHintsService(MessageContextService messageContextService) {
        this.messageContextService = null;
    }

    protected void bindUserSessionService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    protected void unbindUserSessionService(UserSessionService userSessionService) {
        this.userSessionService = null;
    }

    protected void bindMessageInterceptorService(MessageInterceptorService messageInterceptorService) {
        this.messageInterceptorService = messageInterceptorService;
    }

    protected void unbindMessageInterceptorService(MessageInterceptorService messageInterceptorService) {
        this.messageInterceptorService = null;
    }

    protected void bindCommandService(CommandService commandService) {
        this.commandService = commandService;
    }

    protected void unbindCommandService(CommandService commandService) {
        this.commandService = null;
    }

    protected void bindCommandResolverService(CommandResolverService commandResolverService) {
        this.commandResolverService = commandResolverService;
    }

    protected void unbindCommandResolverService(CommandResolverService commandResolverService) {
        this.commandResolverService = null;
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        Log.debug("Starting JerkBot");
        botThread = new Thread(this);
        botThread.start();
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        if (botThread != null) {
            if (botThread.isAlive()) {
                executor.shutdown();
                botThread.interrupt();
            }
        }
    }

    /**
     * Checks if is trigger.
     *
     * @param message the message
     * @return true, if is trigger
     */
    public String isTrigger(String message) {
        if (StringUtils.isEmpty(message)) {
            return null;
        }
        char firstChar = message.charAt(0);
        if (triggers.contains(firstChar)) {
            String doubleTrigger = "" + firstChar + firstChar;
            if (message.startsWith(doubleTrigger)) {
                return doubleTrigger;
            }
            return String.valueOf(firstChar);
        }
        return null;
    }

    public void run() {
        ProfileConfiguration profileConfiguration = new ProfileConfiguration();
        Profile profile = profileConfiguration.getProfile();
        ConnectionManager manager = new ConnectionManager(profile);
        JerkBotConfiguration configuration = new JerkBotConfiguration();
        env.putAll(configuration.getConfig());
        if (!StringUtils.isEmpty(env.getProperty(JerkBotConstants.IRC_TRIGGERS))) {
            String[] configuredTriggers = env.getProperty(JerkBotConstants.IRC_TRIGGERS).split(",");
            for (String configuredTrigger : configuredTriggers) {
                triggers.add(configuredTrigger.charAt(0));
            }
        } else {
            triggers.add('~');
        }
        session = manager.requestConnection(env.getProperty(JerkBotConstants.IRC_SERVER, "niven.freenode.net"));
        String channelList = env.getProperty(JerkBotConstants.IRC_CHANNELS, JerkBotConstants.DEFAULT_CHANNEL);
        final String[] channels = channelList.split(",");
        if (env.getProperty(JerkBotConstants.IRC_PASSWORD) != null) {
            Log.info("Authenticating...");
            NickServAuthPlugin auth = new NickServAuthPlugin(env.getProperty(JerkBotConstants.IRC_PASSWORD), 'e', session, Arrays.asList(channels));
            auth.addTaskListener(new TaskCompletionListener() {

                public void taskComplete(Object result) {
                    if (result.equals(Boolean.TRUE)) {
                        Log.info("Authenticated successfully!");
                    } else {
                        Log.warn("NickServ authentication failed!");
                    }
                    joinChannels(channels);
                }
            });
        } else {
            joinChannels(channels);
        }
    }

    @Override
    protected void handleJoinCompleteEvent(JoinCompleteEvent evt) {
        String greeting = env.getProperty(JerkBotConstants.IRC_GREETING, "");
        if (!StringUtils.isBlank(greeting)) {
            evt.getChannel().say(greeting);
        }
    }

    protected void joinChannels(String[] channels) {
        session.addIRCEventListener(BotServiceImpl.this);
        for (String channel : channels) {
            session.join(channel);
        }
    }

    public Session getSession() {
        return session;
    }

    @Override
    protected void handleChannelMessage(final MessageEvent event) {
        UserKey key = new UserKey(event.getNick(), event.getHostName());
        if (floodControl.shouldIgnoreUser(key)) {
            return;
        }
        executor.execute(new Runnable() {

            public void run() {
                onMessageEvent(event);
            }
        });
    }

    private void onMessageEvent(MessageEvent evt) {
        Log.debug(String.format("User:'%s', Hostname:'%s', Message:'%s'", evt.getNick(), evt.getHostName(), evt.getMessage()));
        String message = evt.getMessage();
        String trigger = isTrigger(message);
        if ((trigger == null) && !evt.isPrivate()) {
            return;
        }
        try {
            messageInterceptorService.beforeParsing(evt);
        } catch (AccessDeniedException ex) {
            session.sayPrivate(evt.getNick(), ex.getMessage());
            return;
        }
        MessageContext context = messageContextService.createContext(trigger, evt);
        final String target = context.getUserTarget();
        if (!StringUtils.isEmpty(target)) {
            if (target.equalsIgnoreCase(session.getNick())) {
                session.sayPrivate(context.getSender(), NO_MONOLOGUE);
                return;
            }
        }
        try {
            messageInterceptorService.afterParsing(context);
        } catch (AccessDeniedException ex) {
            session.sayPrivate(context.getSender(), ex.getMessage());
            return;
        }
        final String operationName = context.getCommandName();
        if (!StringUtils.isEmpty(operationName)) {
            Command command = commandService.createCommand(operationName);
            try {
                if (command.getClass().isAssignableFrom(UnknownCommand.class)) {
                    commandResolverService.attemptResolve(context);
                } else {
                    command.execute(context);
                }
            } catch (CommandSyntaxException e) {
                MessageUtil.sayFormatted(context, "Invalid syntax for the command '%s' : '%s'", operationName, e.getMessage());
            } catch (ConfigurationException e) {
                MessageUtil.sayFormatted(context, "A configuration error occured: %s", e.getMessage());
                Log.error(e.getMessage(), e);
            } catch (JerkBotException e) {
                MessageUtil.say(context, e.getMessage());
                Log.error(e.getMessage(), e);
            } catch (Exception e) {
                Log.error(e.getMessage(), e);
            }
        }
    }
}
