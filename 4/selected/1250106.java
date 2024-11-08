package checkers3d.storage;

import java.io.*;
import java.util.*;
import javax.swing.*;
import checkers3d.logic.*;

/**
 *
 * @author Sean Keel
 */
public class DataManagerAccounts {

    /**
     * Saves a player's name and password to an accounts file.
     *
     * @param name The player's username.
     * @param password The player's password.
     */
    public static void addPlayer(Player playerAccount) throws IOException {
        FileWriter fw = new FileWriter("Accounts.txt", true);
        BufferedWriter out = new BufferedWriter(fw);
        if (!doesPlayerExist(playerAccount.getName())) {
            DataManagerStats.createIndividualStatistic(playerAccount);
            String playerInfo = new String();
            playerInfo = (playerAccount.getName() + "\t" + playerAccount.getPassword());
            out.write(playerInfo);
            out.newLine();
            JOptionPane.showMessageDialog(null, "The account was successfully created.");
        } else JOptionPane.showMessageDialog(null, "The account could not be created" + " because\nan account with the name " + playerAccount.getName() + " already exists.");
        out.close();
    }

    /**
     * Removes a player and their password from the accounts file if they exist.
     *
     * @param name The player's username.
     */
    public static void removePlayer(String name) throws FileNotFoundException, IOException {
        if (doesPlayerExist(name)) {
            createBackupAccountsFile();
            deleteAllAccounts();
            String deletedAccountName = new String();
            String deletedAccountPassword = new String();
            FileReader fr = new FileReader("BackupAccounts.txt");
            Scanner scan = new Scanner(fr);
            FileWriter fw = new FileWriter("Accounts.txt", true);
            BufferedWriter out = new BufferedWriter(fw);
            while (scan.hasNext()) {
                StringTokenizer st = new StringTokenizer(scan.nextLine(), "\t");
                String tempToken = st.nextToken();
                if (tempToken.equals(name)) {
                    deletedAccountName = tempToken;
                    deletedAccountPassword = st.nextToken();
                } else {
                    String tempPlayerPassword = st.nextToken();
                    out.write(tempToken + "\t" + tempPlayerPassword);
                    out.newLine();
                }
            }
            out.close();
            scan.close();
        } else JOptionPane.showMessageDialog(null, "The account for " + name + " cannot be removed\nbecause it does not exist.");
    }

    /**
     * Returns all of the player names and passwords from the accounts file.
     *
     * @return String The combined names of all the players.
     */
    public static String findAllPlayers() throws FileNotFoundException {
        FileReader fr = new FileReader("Accounts.txt");
        Scanner scan = new Scanner(fr);
        String players = new String();
        while (scan.hasNext()) {
            StringTokenizer st = new StringTokenizer(scan.nextLine(), " ");
            players += (showPlayerName(st.nextToken()) + " ");
        }
        scan.close();
        return players;
    }

    /**
     * This is called from another function to return the player's name.
     *
     * @param token A player's username.
     * @return String Returns a player's username.
     */
    public static String showPlayerName(String name) {
        return name;
    }

    /**
     * Returns a string of information about this class containing
     * the class name.
     *
     * @param username A player's username.
     * @return boolean Returns true if the player is found in the accounts file
     * and false otherwise.
     */
    public static boolean doesPlayerExist(String name) throws FileNotFoundException {
        FileReader fr = new FileReader("Accounts.txt");
        Scanner scan = new Scanner(fr);
        boolean flag = false;
        while (scan.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(scan.nextLine(), "\t");
            if (st.nextToken().equals(name)) {
                flag = true;
                break;
            }
        }
        scan.close();
        return flag;
    }

    /**
     * Loads a player object based on a username. Returns null if the player
     * does not exist.
     *
     * @param username A player's username.
     * @return Loaded player object.
     */
    public static Player loadPlayer(String username) {
        Player player = null;
        try {
            if (doesPlayerExist(username)) player = new Player(username, getPlayerPassword(username));
        } catch (Exception err) {
        }
        return player;
    }

    /**
     * Returns a given player's password.
     *
     * @param username A player's username.
     * @return String Returns the player's password.
     */
    public static String getPlayerPassword(String username) throws FileNotFoundException {
        FileReader fr = new FileReader("Accounts.txt");
        Scanner scan = new Scanner(fr);
        boolean flag = false;
        String password = new String();
        while (scan.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(scan.nextLine(), "\t");
            if (st.nextToken().equals(username)) {
                flag = true;
                password = st.nextToken();
                break;
            }
        }
        scan.close();
        return password;
    }

    /**
     * Deletes the entire accounts file.
     *
     * @param password A player's unencrypted password.
     */
    public static void createPasswordHash(String password) {
        int i;
        long hash = 0;
        String newPassword = new String();
        for (i = 0; i < password.length(); i++) {
            hash = (password.charAt(i) << 2);
            newPassword += hash;
        }
    }

    /**
     * Deletes the entire accounts file.
     */
    public static void deleteAllAccounts() throws IOException {
        FileWriter fw = new FileWriter("Accounts.txt", false);
        fw.close();
    }

    /**
     * Deletes the entire backup accounts file.
     */
    public static void deleteBackupAccounts() throws IOException {
        FileWriter fw = new FileWriter("BackupAccounts.txt", false);
        fw.close();
    }

    /**
     * Creates a backup of the accounts file.
     */
    public static void createBackupAccountsFile() throws IOException {
        deleteBackupAccounts();
        FileReader fr = new FileReader("Accounts.txt");
        Scanner scan = new Scanner(fr);
        FileWriter fw = new FileWriter("BackupAccounts.txt", true);
        BufferedWriter out = new BufferedWriter(fw);
        while (scan.hasNext()) {
            out.write(scan.nextLine());
            out.newLine();
        }
        scan.close();
        out.close();
    }
}
