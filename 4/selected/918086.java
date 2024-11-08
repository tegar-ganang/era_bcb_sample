package server.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import player.Player;
import player.PlayerImp;
import player.PlayerManager;
import adventure.vocation.Vocation;
import adventure.vocation.VocationEdit;

/**
 *
 * @author Michael Hanns
 *
 */
public class LoginManagerImp implements LoginManager {

    private PlayerManager players;

    private Player activePlayer;

    private boolean loggedOn;

    private BufferedReader in;

    private PrintWriter out;

    private int serverCapacity;

    public LoginManagerImp(BufferedReader read, PrintWriter write, PlayerManager p, int capacity) {
        this.in = read;
        this.out = write;
        this.players = p;
        this.serverCapacity = capacity;
        this.loggedOn = false;
    }

    @Override
    public boolean loginPrompt() {
        try {
            out.println("What is thy name, adventurer?");
            String input = FixTelnetInput.fix(in.readLine());
            if (input == null) {
                out.println("Error reading input! Login cancelled.");
                return false;
            }
            if (input.length() < 3 || input.length() > 12) {
                out.println("Invalid name! Please enter a name between 3 and 12 characters long.");
                return true;
            } else if (input.contains(" ")) {
                out.println("Invalid name! You may not include spaces in your name.");
                return true;
            }
            if (maximumPlayersReached()) {
                return true;
            } else if (input.equalsIgnoreCase("DISCONNECT") || input.equalsIgnoreCase("QUIT")) {
                return false;
            } else {
                activePlayer = players.getOfflinePlayer(input);
                if (activePlayer.getID() > 0) {
                    out.println("Player " + activePlayer.getAliveName() + " found! Please enter password.");
                    String password = FixTelnetInput.fix(in.readLine());
                    if (maximumPlayersReached()) {
                        return true;
                    } else if (password.equalsIgnoreCase("DISCONNECT") || password.equalsIgnoreCase("QUIT")) {
                        out.println("Login cancelled.");
                    } else if (players.logOn(activePlayer.getAliveName(), password)) {
                        out.println("Success! Entering world...");
                        loggedOn = true;
                    } else {
                        out.println("Password does not match!\r\n");
                        loggedOn = false;
                    }
                } else {
                    if (players.getOnlinePlayer(input).getID() > 0) {
                        out.println("A player by that name is already playing!\r\n");
                        loggedOn = false;
                    } else {
                        char[] name = input.toCharArray();
                        name[0] = Character.toUpperCase(name[0]);
                        input = String.copyValueOf(name);
                        createCharacterPrompt(input);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean maximumPlayersReached() {
        if (players.onlinePlayers() >= serverCapacity) {
            out.println("The server has reached maximum capacity. Please try again later.");
            return true;
        } else {
            return false;
        }
    }

    private void createCharacterPrompt(String name) throws IOException {
        out.println(name + " is not known here. Create new character? [Y/N]");
        String input = FixTelnetInput.fix(in.readLine());
        if (input != null) {
            while (input != null && !(input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("N"))) {
                out.println("Please enter Y or N");
                input = FixTelnetInput.fix(in.readLine());
            }
            if (input != null && input.equalsIgnoreCase("Y")) {
                createCharacter(name);
            }
        } else {
            out.println("Error reading input! Character creation cancelled.");
        }
    }

    private void createCharacter(String name) throws IOException {
        out.println("Please enter a password for " + name + ":");
        String pass = FixTelnetInput.fix(in.readLine());
        if (pass.length() < 3 || pass.length() > 12) {
            out.println("Invalid password! Please enter a password between 3 and 12 characters long.");
            createCharacter(name);
            return;
        } else if (pass.contains(" ")) {
            out.println("Invalid password! You may not include spaces in your password.");
            createCharacter(name);
            return;
        }
        out.println("Please confirm password for " + name + ":");
        String passConfirm = FixTelnetInput.fix(in.readLine());
        if (pass.length() < 3 || pass.length() > 12) {
            out.println("Invalid password! Please enter a password between 3 and 12 characters long.");
            createCharacter(name);
            return;
        } else if (pass.contains(" ")) {
            out.println("Invalid password! You may not include spaces in your password.");
            createCharacter(name);
            return;
        }
        if (passConfirm.equals(pass)) {
            Vocation vocation = null;
            while (vocation == null) {
                vocation = selectVocation();
            }
            if (players.createPlayer(name, pass, vocation)) {
                out.println("Player " + name + " created!\r\n");
            } else {
                out.println("Error creating player: unable to save file. Please contact your server administrator.");
            }
        } else {
            out.println("Passwords do not match!");
            createCharacter(name);
        }
    }

    private Vocation selectVocation() throws IOException {
        out.println("\r\nWhich vocation would you like to choose?");
        out.println("Type the vocation's name to read more.");
        String[] vocations = players.getVocationNames();
        String vocationsOut = "";
        for (int x = 0; x < vocations.length; x++) {
            vocationsOut += vocations[x] + " ";
        }
        out.println(vocationsOut);
        String vocSelect = FixTelnetInput.fix(in.readLine());
        for (int x = 0; x < vocations.length; x++) {
            if (vocations[x].equalsIgnoreCase(vocSelect)) {
                if (confirmVocation(x + 1)) {
                    return ((VocationEdit) players.getVocation(x + 1)).cloneThis();
                } else {
                    return selectVocation();
                }
            }
        }
        out.println("That is not a valid vocation!");
        return selectVocation();
    }

    private boolean confirmVocation(int id) throws IOException {
        Vocation voc = players.getVocation(id);
        out.println(voc.getName());
        out.println(voc.getDescription() + "\r\n");
        out.println(" +------------------+-----+-----+-----+-----+-----+");
        out.println(" |                  | ATK | DEF | INT | SPD | LCK |");
        out.println(" +------------------+-----+-----+-----+-----+-----+");
        out.println(" | Base:            | " + getWhiteSpace(voc.getBaseAttack()) + "  | " + getWhiteSpace(voc.getBaseDefence()) + "  | " + getWhiteSpace(voc.getBaseIntel()) + "  | " + getWhiteSpace(voc.getBaseSpeed()) + "  | " + getWhiteSpace(voc.getBaseLuck()) + "  |");
        out.println(" | Increase Chance: | " + getWhiteSpace((int) (voc.getChanceAttack() * 100)) + "% | " + getWhiteSpace((int) (voc.getChanceDefence() * 100)) + "% | " + getWhiteSpace((int) (voc.getChanceIntel() * 100)) + "% | " + getWhiteSpace((int) (voc.getChanceSpeed() * 100)) + "% | " + getWhiteSpace((int) (voc.getChanceLuck() * 100)) + "% | ");
        out.println(" +------------------+-----+-----+-----+-----+-----+");
        out.println("\r\nKeep the above stats? [Y/N]");
        String confirm = FixTelnetInput.fix(in.readLine());
        if (confirm == null) {
            out.println("Please enter Y or N.");
        } else {
            if (confirm.equalsIgnoreCase("Y")) {
                return true;
            } else if (confirm.equalsIgnoreCase("N")) {
                out.println("\r\nRetrying...");
                return false;
            } else {
                out.println("Please enter Y or N.");
            }
        }
        return false;
    }

    private String getWhiteSpace(int stat) {
        if (stat < 10) {
            return stat + " ";
        } else return stat + "";
    }

    @Override
    public boolean loggedOn() {
        return loggedOn;
    }

    @Override
    public Player getCharacter() {
        if (loggedOn) {
            return activePlayer;
        } else {
            return new PlayerImp();
        }
    }
}
