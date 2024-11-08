package com.shimari.games;

import com.shimari.bot.*;
import com.shimari.framework.*;
import java.util.*;

/**
 * Plays Rock-Paper-Scissors
 */
public class RockPaperScissorHandler implements Handler {

    private Bot bot = null;

    private final String botName;

    private Map scores = new HashMap();

    private Map challenges = new HashMap();

    public RockPaperScissorHandler(Config config) throws ConfigException {
        botName = config.getString("botName");
    }

    public void init(Registry r) throws ConfigException {
        bot = (Bot) r.getComponent(botName);
    }

    /** Handle message */
    public boolean handle(Message m) {
        String request = m.getMessage().trim();
        String nick = m.getFromNick();
        String channel = m.getChannel();
        String action;
        String target;
        int idx = request.indexOf(" ");
        if (idx > 0) {
            action = request.substring(0, idx).trim();
            target = request.substring(idx).trim();
        } else {
            action = request.trim();
            target = null;
        }
        if (action.equalsIgnoreCase("r") || action.equalsIgnoreCase("rock")) return handlePlay(m, nick, target, "rock"); else if (action.equalsIgnoreCase("p") || action.equalsIgnoreCase("paper")) return handlePlay(m, nick, target, "paper"); else if (action.equalsIgnoreCase("s") || action.equalsIgnoreCase("scissors")) return handlePlay(m, nick, target, "scissors"); else if (action.equalsIgnoreCase("score")) {
            return handleScore(m, target);
        } else {
            return false;
        }
    }

    public synchronized boolean handlePlay(Message m, String nick, String target, String move) {
        Challenge c = (Challenge) challenges.get(nick);
        if (c != null) {
            if (target == null || c.challenger.equals(target)) {
                String reason;
                String result;
                String winner;
                String loser;
                challenges.remove(nick);
                if (c.move.equals(move)) {
                    result = "draw";
                    winner = "nobody";
                    loser = null;
                } else if (c.move.equals("rock")) {
                    if (move.equals("scissors")) {
                        result = "rock dulls scissors";
                        winner = c.challenger;
                        loser = c.responder;
                    } else {
                        result = "paper covers rock";
                        winner = c.responder;
                        loser = c.challenger;
                    }
                } else if (c.move.equals("paper")) {
                    if (move.equals("rock")) {
                        result = "paper covers rock";
                        winner = c.challenger;
                        loser = c.responder;
                    } else {
                        result = "scissors cut paper";
                        winner = c.responder;
                        loser = c.challenger;
                    }
                } else if (c.move.equals("scissors")) {
                    if (move.equals("paper")) {
                        result = "scissors cut paper";
                        winner = c.challenger;
                        loser = c.responder;
                    } else {
                        result = "rock dulls scissors";
                        winner = c.responder;
                        loser = c.challenger;
                    }
                } else {
                    m.sendReply("Unable to process.");
                    return true;
                }
                m.getConnection().broadcastNotice(c.challenger + " picks " + c.move + ", " + c.responder + " picks " + move + ": " + result + "; " + winner + " wins!");
                if (loser != null) {
                    Score win = getScore(winner);
                    win.wins++;
                    Score lose = getScore(loser);
                    lose.losses++;
                    saveScores();
                }
                return true;
            }
        }
        if (target == null) {
            m.sendReply("Nobody has challenged you.");
        } else if (m.isPublic()) {
            m.sendReply("Challenges must be created by private message.");
        } else {
            if (nick.equals(target)) {
                m.sendReply("You cannot challenge yourself.");
            } else {
                c = new Challenge(nick, move, target);
                challenges.put(target, c);
                m.getConnection().broadcastNotice(nick + " challenges " + target + " to a game of rock/paper/scissors!");
            }
        }
        return true;
    }

    public Score getScore(String nick) {
        Score s = (Score) scores.get(nick);
        if (s == null) {
            s = new Score(nick);
            scores.put(nick, s);
        }
        return s;
    }

    public boolean handleScore(Message m, String target) {
        Score s = getScore(target);
        if (s != null) {
            m.sendReply(target + "'s rps score is " + s.toString());
        } else {
            m.sendReply(target + " has not played rock/paper/scissors");
        }
        return true;
    }

    public void saveScores() {
    }

    static class Challenge {

        String challenger;

        String responder;

        String move;

        public Challenge(String challenger, String move, String responder) {
            this.challenger = challenger;
            this.responder = responder;
            this.move = move;
        }
    }

    static class Score {

        final String nick;

        int wins;

        int losses;

        public Score(String nick) {
            this.nick = nick;
        }

        public String toString() {
            return nick + " " + wins + ":" + losses + " = " + getPercent() + "%";
        }

        public int getPercent() {
            return (int) (100 * ((double) wins / (wins + losses)));
        }
    }
}
