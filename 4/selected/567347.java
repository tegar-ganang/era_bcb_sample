package vm;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Match {

    private Date time;

    private Team team1;

    private Team team2;

    private int goalsTeamOne = -1;

    private int goalsTeamTwo;

    private String channel;

    public void setGoalsTeamOne(int goalsTeamOne) {
        this.goalsTeamOne = goalsTeamOne;
    }

    public int getGoalsTeamOne() {
        return goalsTeamOne;
    }

    public int getGoalsTeamTwo() {
        return goalsTeamTwo;
    }

    public void setGoalsTeamTwo(int goalsTeamTwo) {
        this.goalsTeamTwo = goalsTeamTwo;
    }

    public Date getTime() {
        return time;
    }

    public String getNiceTime() {
        SimpleDateFormat sdftmp = new SimpleDateFormat("m");
        String tmp = sdftmp.format(time);
        SimpleDateFormat sdf = new SimpleDateFormat("d 'juni klokken' k:m");
        if (tmp.length() == 1) {
            sdf = new SimpleDateFormat("d 'juni klokken' k:00");
        }
        return sdf.format(time);
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Team getTeam1() {
        return team1;
    }

    public void setTeam1(Team team1) {
        this.team1 = team1;
    }

    public Team getTeam2() {
        return team2;
    }

    public void setTeam2(Team team2) {
        this.team2 = team2;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String toString() {
        if (goalsTeamOne == -1) {
            return team1.getName() + " - " + team2.getName() + ", " + this.getNiceTime() + ", " + getChannel() + ". ";
        } else {
            return team1.getName() + " - " + team2.getName() + " (" + goalsTeamOne + "-" + goalsTeamTwo + "), " + this.getNiceTime() + ", " + getChannel() + ". ";
        }
    }
}
