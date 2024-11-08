package trivialbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

public class MyTriviaBot extends PircBot {

    private String channel;

    private String adminName;

    private boolean gameInProgress = false;

    private ArrayList<Question> questions = null;

    private HashMap<String, Integer> scores = new HashMap<String, Integer>();

    private Random rand = new Random(System.currentTimeMillis());

    private String expectedAnswer = "";

    private final int warningStep;

    private final int questionTimeout;

    private long questionAskedAt;

    private int allowedConsecutiveTimeouts;

    private long currentConsecutiveTimeouts = 0;

    private Timer timer = new Timer();

    public MyTriviaBot(String hostname, String channelName, String channelPassword, String name, String adminName, ArrayList<Question> questions, int questionTimeout, int warningStep, int allowedConsecutiveTimeouts) throws IOException, IrcException {
        setAutoNickChange(true);
        setName(name);
        this.adminName = adminName;
        this.questions = questions;
        this.questionTimeout = questionTimeout;
        this.warningStep = warningStep;
        this.channel = channelName;
        this.allowedConsecutiveTimeouts = allowedConsecutiveTimeouts;
        connect(hostname);
        if (channelPassword == null) {
            joinChannel(channelName);
        } else {
            joinChannel(channelName, channelPassword);
        }
        System.out.println("Connected to: " + getServer());
    }

    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        super.onJoin(channel, sender, login, hostname);
        if (sender.equals(getNick())) {
            System.out.println("Joined channel: " + getChannels()[0]);
            sendMessage(channel, Colors.BOLD + "Hello World !");
            sendMessage(channel, Colors.BOLD + "My name is TrivialBot, i am apparently a trivial bot !");
            sendMessage(channel, Colors.BOLD + "Type !help for a list of available commands");
            final String f = channel;
        }
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        super.onMessage(channel, sender, login, hostname, message);
        if (!gameInProgress) {
            if (!sender.equals(adminName)) {
                if (message.equals("!start") || message.equals("!stop") || message.equals("!skip") || message.equals("!stat") || message.equals("!disconnect") || message.equals("!help")) {
                    sendMessage(channel, Colors.BOLD + "Sorry " + sender + ", only admin can issue commands");
                }
                return;
            }
            if (message.equals("!stop")) {
                currentConsecutiveTimeouts = 0;
                sendMessage(channel, Colors.BOLD + "huh? Stop what?! start a game first !");
                return;
            }
            if (message.equals("!skip")) {
                currentConsecutiveTimeouts = 0;
                sendMessage(channel, Colors.BOLD + "huh? Skip what?! start a game first !");
                return;
            }
            if (message.equals("!start")) {
                currentConsecutiveTimeouts = 0;
                sendMessage(channel, Colors.BOLD + "Game started !");
                sendMessage(channel, Colors.BOLD + "--> Play fair, don't cheat ! <--");
                gameInProgress = true;
                User[] users = getUsers(channel);
                for (int i = 0; i < users.length; i++) {
                    if (!users[i].getNick().equals(getNick())) {
                        scores.put(users[i].getNick(), 0);
                    }
                }
                sendRandomQuestion();
                return;
            }
        } else {
            if (message.equals("!start")) {
                if (!sender.equals(adminName)) {
                    sendMessage(channel, Colors.BOLD + "Sorry " + sender + ", only admin can issue commands");
                    return;
                }
                currentConsecutiveTimeouts = 0;
                sendMessage(channel, Colors.BOLD + "A game is already in progress");
                gameInProgress = false;
                return;
            }
            if (message.equals("!stop")) {
                if (!sender.equals(adminName)) {
                    sendMessage(channel, Colors.BOLD + "Sorry " + sender + ", only admin can issue commands");
                    return;
                }
                currentConsecutiveTimeouts = 0;
                sendMessage(channel, Colors.BOLD + "Game ended !");
                gameInProgress = false;
                timer.cancel();
                return;
            }
            if (message.equals("!skip")) {
                if (!sender.equals(adminName)) {
                    sendMessage(channel, Colors.BOLD + "Sorry " + sender + ", only admin can issue commands");
                    return;
                }
                currentConsecutiveTimeouts = 0;
                sendMessage(channel, Colors.BOLD + "Skipping this question");
                sendRandomQuestion();
                return;
            }
            if (!message.startsWith("!")) {
                if (message.equalsIgnoreCase(expectedAnswer)) {
                    Integer score = scores.get(sender);
                    if (score != null) {
                        currentConsecutiveTimeouts = 0;
                        expectedAnswer = null;
                        sendMessage(channel, Colors.BOLD + sender + ", Correct answer ! Your current score: " + (score.intValue() + 1));
                        scores.put(sender, score.intValue() + 1);
                        sendRandomQuestion();
                    }
                }
                return;
            }
        }
        if (!sender.equals(adminName)) {
            sendMessage(channel, Colors.BOLD + "Sorry " + sender + ", only admin can issue commands");
            return;
        } else {
            if (message.equals("!help")) {
                currentConsecutiveTimeouts = 0;
                sendMessage(channel, Colors.BOLD + "~~~~ List of my trivial commands ~~~~");
                sendMessage(channel, Colors.BOLD + "!help: shows this list of commands ... obviously");
                sendMessage(channel, Colors.BOLD + "!start: starts a trivia game with current users in channel");
                sendMessage(channel, Colors.BOLD + "!stop: stops the current game");
                sendMessage(channel, Colors.BOLD + "!skip: skips the current question");
                sendMessage(channel, Colors.BOLD + "!stat: shows the scoreboard");
                sendMessage(channel, Colors.BOLD + "!disconnect: kills me !");
                sendMessage(channel, Colors.BOLD + "~~~~ List of my trivial commands ~~~~");
                return;
            }
            if (message.equals("!disconnect")) {
                sendMessage(channel, Colors.BOLD + "gtg .. see you around, bye !");
                timer.cancel();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MyTriviaBot.class.getName()).log(Level.SEVERE, null, ex);
                }
                disconnect();
                System.out.println("Disconnected");
                System.out.println("Quitting .. bye !");
                System.exit(0);
            }
            if (message.equals("!stat")) {
                currentConsecutiveTimeouts = 0;
                sendMessage(channel, Colors.BOLD + "~~~~ Scoreboard ~~~~");
                Set<Entry<String, Integer>> scoresSet = scores.entrySet();
                if (scores.isEmpty()) {
                    sendMessage(channel, Colors.BOLD + "No scores ..");
                } else {
                    for (Entry<String, Integer> score : scoresSet) {
                        if (score.getValue() != 0) {
                            sendMessage(channel, Colors.BOLD + score.getKey() + ": " + score.getValue());
                        }
                    }
                }
                sendMessage(channel, Colors.BOLD + "~~~~ Scoreboard ~~~~");
                return;
            }
        }
    }

    @Override
    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
        scores.put(newNick, scores.get(oldNick));
        scores.remove(oldNick);
    }

    private void sendRandomQuestion() {
        Question question = questions.get(rand.nextInt(questions.size()));
        expectedAnswer = question.getAnswer();
        sendMessage(channel, Colors.BOLD + question.getQuestion());
        questionAskedAt = System.currentTimeMillis();
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                sendMessage(channel, Colors.BOLD + "Timeout! The correct answer was: " + expectedAnswer);
                currentConsecutiveTimeouts++;
                if (currentConsecutiveTimeouts >= allowedConsecutiveTimeouts) {
                    sendMessage(channel, Colors.BOLD + "Too many timeouts, TrivialBot will disconnect");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MyTriviaBot.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    disconnect();
                    System.out.println("Reached maximum consecutive timeouts, TrivialBot will exit");
                    System.exit(0);
                }
                sendRandomQuestion();
            }
        }, questionTimeout * 1000, questionTimeout * 1000);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                sendMessage(channel, Colors.BOLD + (questionTimeout - ((System.currentTimeMillis() - questionAskedAt)) / 1000) + " seconds remaining");
            }
        }, warningStep * 1000, warningStep * 1000);
    }
}
