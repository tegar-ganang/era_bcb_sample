package net.jetrix.winlist;

import java.util.*;
import net.jetrix.*;

/**
 * The result of a game.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 816 $, $Date: 2010-01-19 12:05:31 -0500 (Tue, 19 Jan 2010) $
 */
public class GameResult {

    private Date startTime;

    private Date endTime;

    private List<GamePlayer> gamePlayers;

    private Channel channel;

    /**
     * Return the date of the beginning of the game.
     */
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Return the date of the end of the game.
     */
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    /**
     * Return the channel associated to this result. It may be used to send a
     * message in the channel reporting the new scores of the players.
     */
    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Return the list of players involved in the game. The collection contains
     * instances of GamePlayer.
     */
    public Collection<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    private void addGamePlayer(GamePlayer gamePlayer) {
        if (gamePlayers == null) {
            gamePlayers = new ArrayList<GamePlayer>();
        }
        gamePlayers.add(gamePlayer);
    }

    /**
     * Update the result of the game by indicating if the specified user won or not.
     */
    public void update(User user, boolean isWinner) {
        GamePlayer player = new GamePlayer();
        player.setName(user.getName());
        player.setTeamName(user.getTeam());
        player.setWinner(isWinner);
        player.setEndTime(new Date());
        addGamePlayer(player);
    }

    /**
     * Return the players that finished the game at the specified rank.
     */
    public Collection<GamePlayer> getPlayersAtRank(int rank) {
        Collection<GamePlayer> players = new ArrayList<GamePlayer>();
        if (rank == 1) {
            for (GamePlayer player : gamePlayers) {
                if (player.isWinner()) {
                    players.add(player);
                }
            }
        } else {
            Collections.sort(gamePlayers, new Comparator<GamePlayer>() {

                public int compare(GamePlayer player1, GamePlayer player2) {
                    return player2.getEndTime().compareTo(player1.getEndTime());
                }
            });
            int i = 1;
            Iterator<GamePlayer> it = gamePlayers.iterator();
            while (it.hasNext() && i < rank) {
                GamePlayer player = it.next();
                if (!player.isWinner()) {
                    i++;
                    if (i == rank) {
                        players.add(player);
                    }
                }
            }
        }
        return players;
    }

    /**
     * Return the number of teams in this game.
     */
    public int getTeamCount() {
        Map<String, String> teams = new HashMap<String, String>();
        int teamCount = 0;
        for (GamePlayer player : gamePlayers) {
            String team = player.getTeamName();
            if (team == null) {
                teamCount++;
            } else {
                teams.put(team, team);
            }
        }
        return teamCount + teams.size();
    }
}
