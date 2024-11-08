package checkers3d.storage;

import java.io.*;
import java.util.*;
import checkers3d.logic.*;

/**
 *
 * @author Sean
 */
public class DataManagerStats {

    public static void createIndividualStatistic(Player player) throws IOException {
        FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
        BufferedWriter out = new BufferedWriter(fw);
        int wins = 0;
        int losses = 0;
        int ties = 0;
        out.write(player.getName() + "\t" + wins + "\t" + losses + "\t" + ties);
        out.newLine();
        out.close();
    }

    public static void updateInidivualStatistic(Player playerAccount, boolean wins, boolean losses, boolean ties) throws IOException {
        createBackupStatisticsFile();
        deleteStatistics();
        String tempName = new String();
        int winsToUpdate = 0;
        int lossesToUpdate = 0;
        int tiesToUpdate = 0;
        FileReader fr = new FileReader("BackupStatistics.txt");
        Scanner scan = new Scanner(fr);
        FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
        BufferedWriter out = new BufferedWriter(fw);
        while (scan.hasNext()) {
            tempName = scan.next();
            if (tempName.equals(playerAccount.getName())) {
                if (wins == true) winsToUpdate = scan.nextInt() + 1; else if (wins == false) winsToUpdate = scan.nextInt();
                if (losses == true) lossesToUpdate = scan.nextInt() + 1; else if (losses == false) lossesToUpdate = scan.nextInt();
                if (ties == true) tiesToUpdate = scan.nextInt() + 1; else if (ties == false) tiesToUpdate = scan.nextInt();
                String buildStatistic = new String(tempName + "\t" + winsToUpdate + "\t" + lossesToUpdate + "\t" + tiesToUpdate);
                out.write(buildStatistic);
                out.newLine();
            } else {
                String buildStatistic = new String(tempName + "\t" + scan.nextInt() + "\t" + scan.nextInt() + "\t" + scan.nextInt());
                out.write(buildStatistic);
                out.newLine();
            }
        }
        out.newLine();
        out.close();
    }

    public static void addOpponentStats(Player playerOne, Player playerTwo, int wins, int losses, int ties) throws IOException {
        String name1 = playerOne.getName();
        String name2 = playerTwo.getName();
        String playerStats1 = (name1 + "/vs./" + name2 + "\t" + wins + "\t" + losses + "\t" + ties);
        String playerStats2 = (name2 + "/vs./" + name1 + "\t" + losses + "\t" + wins + "\t" + ties);
        createBackupStatisticsFile();
        deleteStatistics();
        FileReader fr = new FileReader("BackupStatistics.txt");
        Scanner scan = new Scanner(fr);
        FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
        BufferedWriter out = new BufferedWriter(fw);
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            StringTokenizer st = new StringTokenizer(line, "\t");
            String token = st.nextToken();
            if (token.equals(name1)) {
                out.write(line);
                out.newLine();
                out.write(playerStats1);
                out.newLine();
            } else if (token.equals(name2)) {
                out.write(line);
                out.newLine();
                out.write(playerStats2);
                out.newLine();
            } else {
                out.write(line);
                out.newLine();
            }
        }
        out.close();
        scan.close();
    }

    public static boolean isOpponentStatsExist(Player player1, Player player2) throws FileNotFoundException {
        boolean flag = false;
        FileReader fr = new FileReader("PlayerStatistics.txt");
        Scanner scan = new Scanner(fr);
        String name1 = player1.getName();
        String name2 = player2.getName();
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            StringTokenizer st = new StringTokenizer(line, "\t");
            if (line.contains("vs.") && st.nextToken().equals(name1 + "/vs./" + name2)) {
                flag = true;
                break;
            }
        }
        scan.close();
        return flag;
    }

    public static void updateOponentStats(Player player1, Player player2, boolean wins, boolean losses, boolean ties) throws IOException {
        if (!isOpponentStatsExist(player1, player2)) addOpponentStats(player1, player2, 0, 0, 0);
        createBackupStatisticsFile();
        deleteStatistics();
        String tempName = new String();
        int winsToUpdate = 0;
        int lossesToUpdate = 0;
        int tiesToUpdate = 0;
        FileReader fr = new FileReader("BackupStatistics.txt");
        Scanner scan = new Scanner(fr);
        FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
        BufferedWriter out = new BufferedWriter(fw);
        while (scan.hasNext()) {
            tempName = scan.next();
            if (tempName.equals(player1.getName() + "/vs./" + player2.getName())) {
                if (wins == true) winsToUpdate = scan.nextInt() + 1; else if (wins == false) winsToUpdate = scan.nextInt();
                if (losses == true) lossesToUpdate = scan.nextInt() + 1; else if (losses == false) lossesToUpdate = scan.nextInt();
                if (ties == true) tiesToUpdate = scan.nextInt() + 1; else if (ties == false) tiesToUpdate = scan.nextInt();
                String buildStatistic = new String(tempName + "\t" + winsToUpdate + "\t" + lossesToUpdate + "\t" + tiesToUpdate);
                out.write(buildStatistic);
                out.newLine();
            } else {
                String buildStatistic = new String(tempName + "\t" + scan.nextInt() + "\t" + scan.nextInt() + "\t" + scan.nextInt());
                out.write(buildStatistic);
                out.newLine();
            }
        }
        out.newLine();
        out.close();
    }

    /**
     * Deletes the entire statistics file.
     */
    public static void deleteStatistics() throws IOException {
        FileWriter fw = new FileWriter("PlayerStatistics.txt", false);
        fw.close();
    }

    /**
     * Deletes the entire backup statistics file.
     */
    public static void deleteBackupStatistics() throws IOException {
        FileWriter fw = new FileWriter("BackupStatistics.txt", false);
        fw.close();
    }

    /**
     * Creates a backup of the statistics file.
     */
    public static void createBackupStatisticsFile() throws IOException {
        deleteBackupStatistics();
        FileReader fr = new FileReader("PlayerStatistics.txt");
        Scanner scan = new Scanner(fr);
        FileWriter fw = new FileWriter("BackupStatistics.txt", true);
        BufferedWriter out = new BufferedWriter(fw);
        while (scan.hasNext()) {
            out.write(scan.nextLine());
            out.newLine();
        }
        scan.close();
        out.close();
    }

    public static void removePlayerStats(Player player1) throws FileNotFoundException, IOException {
        if (DataManagerAccounts.doesPlayerExist(player1.getName())) {
            createBackupStatisticsFile();
            deleteStatistics();
            FileReader fr = new FileReader("BackupStatistics.txt");
            Scanner scan = new Scanner(fr);
            FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
            BufferedWriter out = new BufferedWriter(fw);
            while (scan.hasNext()) {
                String line = scan.nextLine();
                StringTokenizer st1 = new StringTokenizer(line, "\t");
                String stn1 = st1.nextToken();
                if (!stn1.equals(player1.getName())) {
                    out.write(line);
                    out.newLine();
                }
            }
            scan.close();
            out.close();
        }
    }

    public static String getWins(Player player) throws FileNotFoundException {
        if (DataManagerAccounts.doesPlayerExist(player.getName())) {
            FileReader fr = new FileReader("PlayerStatistics.txt");
            Scanner scan1 = new Scanner(fr);
            boolean flag = false;
            String win = new String();
            while (scan1.hasNextLine()) {
                StringTokenizer st = new StringTokenizer(scan1.nextLine(), "\t");
                if (st.nextToken().equals(player.getName())) {
                    flag = true;
                    win = st.nextToken();
                    break;
                }
            }
            return win;
        } else {
            return "error";
        }
    }

    public static void setWins(int wins, Player player) throws FileNotFoundException, IOException {
        if (DataManagerAccounts.doesPlayerExist(player.getName())) {
            String loss = getLosses(player);
            String tie = getTies(player);
            createBackupStatisticsFile();
            deleteStatistics();
            FileReader fr = new FileReader("BackupStatistics.txt");
            Scanner scan = new Scanner(fr);
            FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
            BufferedWriter out = new BufferedWriter(fw);
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                StringTokenizer st = new StringTokenizer(line, "\t");
                if (!(st.nextToken().equals(player.getName()))) {
                    out.write(line);
                    out.newLine();
                } else {
                    out.write(player.getName() + "\t" + wins + "\t" + loss + "\t" + tie);
                    out.newLine();
                }
            }
            scan.close();
            out.close();
        }
    }

    public static String getLosses(Player player) throws FileNotFoundException {
        if (DataManagerAccounts.doesPlayerExist(player.getName())) {
            FileReader fr = new FileReader("PlayerStatistics.txt");
            Scanner scan1 = new Scanner(fr);
            boolean flag = false;
            String loss = new String();
            while (scan1.hasNextLine()) {
                StringTokenizer st = new StringTokenizer(scan1.nextLine(), "\t");
                if (st.nextToken().equals(player.getName())) {
                    flag = true;
                    st.nextToken();
                    loss = st.nextToken();
                    break;
                }
            }
            return loss;
        } else {
            return "error";
        }
    }

    public static void setLosses(Player player, int losses) throws IOException {
        if (DataManagerAccounts.doesPlayerExist(player.getName())) {
            String tie = getTies(player);
            String win = getWins(player);
            createBackupStatisticsFile();
            deleteStatistics();
            FileReader fr = new FileReader("BackupStatistics.txt");
            Scanner scan = new Scanner(fr);
            FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
            BufferedWriter out = new BufferedWriter(fw);
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                StringTokenizer st = new StringTokenizer(line, "\t");
                if (!(st.nextToken().equals(player.getName()))) {
                    out.write(line);
                    out.newLine();
                } else {
                    out.write(player.getName() + "\t" + win + "\t" + losses + "\t" + tie);
                    out.newLine();
                }
            }
            scan.close();
            out.close();
        }
    }

    public static String getTies(Player player) throws FileNotFoundException {
        if (DataManagerAccounts.doesPlayerExist(player.getName())) {
            FileReader fr = new FileReader("PlayerStatistics.txt");
            Scanner scan1 = new Scanner(fr);
            boolean flag = false;
            String tie = new String();
            while (scan1.hasNextLine()) {
                StringTokenizer st = new StringTokenizer(scan1.nextLine(), "\t");
                if (st.nextToken().equals(player.getName())) {
                    flag = true;
                    st.nextToken();
                    st.nextToken();
                    tie = st.nextToken();
                    break;
                }
            }
            return tie;
        } else {
            return "error";
        }
    }

    public static void setTies(Player player, int ties) throws FileNotFoundException, IOException {
        if (DataManagerAccounts.doesPlayerExist(player.getName())) {
            String loss = getLosses(player);
            String win = getWins(player);
            createBackupStatisticsFile();
            deleteStatistics();
            FileReader fr = new FileReader("BackupStatistics.txt");
            Scanner scan = new Scanner(fr);
            FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
            BufferedWriter out = new BufferedWriter(fw);
            while (scan.hasNext()) {
                String line = scan.nextLine();
                StringTokenizer st = new StringTokenizer(line, "\t");
                if (!(st.nextToken().equals(player.getName()))) {
                    out.write(line);
                    out.newLine();
                } else {
                    out.write(player.getName() + "\t" + win + "\t" + loss + "\t" + ties);
                    out.newLine();
                }
            }
            scan.close();
            out.close();
        }
    }

    public static String getPlayerWins(String name, String name2) throws FileNotFoundException {
        FileReader fr = new FileReader("PlayerStatistics.txt");
        Scanner scan = new Scanner(fr);
        boolean flag = false;
        String win = new String();
        while (scan.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(scan.nextLine(), "\t");
            if (st.nextToken().equals(name + "/vs./" + name2)) {
                flag = true;
                win = st.nextToken();
                break;
            }
        }
        scan.close();
        return win;
    }

    public static String getPlayerLosses(String name, String name2) throws FileNotFoundException {
        FileReader fr = new FileReader("PlayerStatistics.txt");
        Scanner scan = new Scanner(fr);
        boolean flag = false;
        String loss = new String();
        while (scan.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(scan.nextLine(), "\t");
            if (st.nextToken().equals(name + "/vs./" + name2)) {
                flag = true;
                st.nextToken();
                loss = st.nextToken();
                break;
            }
        }
        scan.close();
        return loss;
    }

    public static String getPlayerTies(String name, String name2) throws FileNotFoundException {
        FileReader fr = new FileReader("PlayerStatistics.txt");
        Scanner scan = new Scanner(fr);
        boolean flag = false;
        String tie = new String();
        while (scan.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(scan.nextLine(), "\t");
            if (st.nextToken().equals(name + "/vs./" + name2)) {
                flag = true;
                st.nextToken();
                st.nextToken();
                tie = st.nextToken();
                break;
            }
        }
        scan.close();
        return tie;
    }

    public static void removeOponentStats(Player player1, Player player2) throws FileNotFoundException, IOException {
        createBackupStatisticsFile();
        deleteStatistics();
        FileReader fr = new FileReader("BackupStatistics.txt");
        Scanner scan = new Scanner(fr);
        FileWriter fw = new FileWriter("PlayerStatistics.txt", true);
        BufferedWriter out = new BufferedWriter(fw);
        while (scan.hasNext()) {
            String line = scan.nextLine();
            StringTokenizer st1 = new StringTokenizer(line, "\t");
            String stn1 = st1.nextToken();
            String name1 = player1.getName();
            String name2 = player2.getName();
            if (!(stn1.equals(name1 + "/vs./" + name2))) {
                if (!(stn1.equals(name2 + "/vs./" + name1))) {
                    out.write(line);
                    out.newLine();
                }
            }
        }
        scan.close();
        out.close();
    }

    public static void setOpponentStats(Player player1, Player player2, int wins, int losses, int ties) throws FileNotFoundException, IOException {
        if (isOpponentStatsExist(player1, player2)) removeOponentStats(player1, player2);
        addOpponentStats(player1, player2, wins, losses, ties);
    }
}
