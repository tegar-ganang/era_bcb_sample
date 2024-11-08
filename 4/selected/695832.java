package server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;

/**
 *
 * @author Steven
 */
public class NetworkManager {

    private GUI gui;

    private Socket connectionSocket;

    /**
     * Constructor voor de NetworkManager van de server.
     * 
     * @param gui
     */
    public NetworkManager(GUI gui) {
        this.gui = gui;
    }

    /**
     * Deze functie stuurt een lijstje met filenames van alle bestaande backups op
     * naar de client.
     * 
     * @return
     */
    public ArrayList<String> restoreBackup() {
        ArrayList<String> backupFiles = new ArrayList<String>();
        File dir = new File(System.getProperty("user.dir"));
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith((".bak"))) {
                backupFiles.add(file.getName());
            }
        }
        return backupFiles;
    }

    /**
     * Functie voor het herstellen van de database via een backup bestand
     * (gebruikersgedefinieerde locatie).
     * 
     * @param filename
     * @return
     */
    public Boolean restoreBackup(String filename) {
        gui.writeLine("Restoring backup: " + filename);
        int r = executeQuery("USE [master] RESTORE database [Demo Database NAV (5-0)] FROM DISK = '" + System.getProperty("user.dir") + "\\" + filename + "' WITH REPLACE,RECOVERY");
        return (r > -1) ? true : false;
    }

    /**
     * Functie voor het maken van een backup van de database.
     */
    public void createBackup() {
        executeQuery("BACKUP database [Demo Database NAV (5-0)] to DISK = '" + System.getProperty("user.dir") + "\\" + Calendar.getInstance().get(Calendar.DATE) + "-" + Calendar.getInstance().get(Calendar.MONTH) + "-" + Calendar.getInstance().get(Calendar.YEAR) + " " + Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + Calendar.getInstance().get(Calendar.MINUTE) + ".bak'", false);
        gui.writeLine("Creating backup file...");
    }

    /**
     * Functie voor het uitvoeren van een query. De returnwaarde geeft aan of de
     * uitvoering gelukt is (0 of hoger) of niet (-1). Als de bewerking gelukt
     * is, zal de returnwaarde aangeven hoeveel rows er aangepast waren.
     * 
     * @param query
     * @return
     */
    public int executeQuery(String query) {
        String con = "jdbc:sqlserver://localhost:1433;databaseName=Demo Database NAV (5-0);username=sa;password=1234";
        try {
            Connection connection = DriverManager.getConnection(con);
            Statement statement = connection.createStatement();
            int result = statement.executeUpdate(query);
            connection.close();
            return result;
        } catch (Exception ex) {
            System.out.println("Er is een probleem opgetreden");
            System.out.println(ex);
            return -1;
        }
    }

    /**
     * Functie voor het uitvoeren van een query. Deze return betreft een ingepakte
     * reflectie van de result van de query. Als firstRowTableNames true is, wordt
     * de eerste row gevuld met de namen van de columns.
     * 
     * @param query
     * @param firstRowTableNames
     * @return
     */
    public ArrayList executeQuery(String query, Boolean firstRowTableNames) {
        query = query.replaceAll("~", ":");
        ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
        String con = "jdbc:sqlserver://localhost:1433;databaseName=Demo Database NAV (5-0);username=sa;password=1234";
        try {
            Connection connection = DriverManager.getConnection(con);
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query);
            ResultSetMetaData rsmd = result.getMetaData();
            if (firstRowTableNames) {
                rows.add(new ArrayList<String>());
                for (int i = 1; i < (rsmd.getColumnCount() + 1); i++) {
                    rows.get(0).add(rsmd.getColumnLabel(i));
                }
            }
            for (int r = 0; result.next(); r++) {
                rows.add(new ArrayList<String>());
                for (int i = 1; i < (rsmd.getColumnCount() + 1); i++) {
                    rows.get((firstRowTableNames) ? (r + 1) : r).add(result.getString(i));
                }
            }
            if (firstRowTableNames && query.toLowerCase().indexOf("group by") > -1) {
                ArrayList<ArrayList<String>> tempArray = new ArrayList<ArrayList<String>>();
                tempArray.add(new ArrayList<String>());
                for (int i = 1; i < rows.size(); i++) {
                    tempArray.get(0).add(rows.get(i).get(0));
                    System.out.println(rows.get(i).get(0));
                }
                for (int i = 1; i < rows.get(0).size(); i++) {
                    tempArray.add(new ArrayList<String>());
                    for (int r = 1; r < rows.size(); r++) {
                        tempArray.get(i).add(rows.get(r).get(i));
                        System.out.println(rows.get(r).get(i));
                    }
                }
                rows = tempArray;
            }
            connection.close();
            return rows;
        } catch (Exception ex) {
            System.out.println("Er is een probleem opgetreden");
            System.out.println(ex);
            return null;
        }
    }

    /**
     * Functie voor het wachten op invoer van clients. Inkomende commando's worden
     * uitgevoerd en gelogd in het logbestand.
     */
    public void listen() {
        try {
            ServerSocket welcomeSocket = new ServerSocket(6789);
            while (true) {
                String clientData;
                String clientDataArray[];
                String clientCommand;
                String clientCommandParameters[];
                connectionSocket = welcomeSocket.accept();
                InputStreamReader inFromClientStream = new InputStreamReader(connectionSocket.getInputStream());
                BufferedReader inFromClient = new BufferedReader(inFromClientStream);
                clientData = inFromClient.readLine();
                clientDataArray = clientData.split(":");
                clientCommand = clientDataArray[0];
                clientCommandParameters = new String[clientDataArray.length - 1];
                System.arraycopy(clientDataArray, 1, clientCommandParameters, 0, clientDataArray.length - 1);
                if (clientCommand != null) {
                    String reportClientData = ">> Received command \"" + clientCommand + "\" from IP " + connectionSocket.getInetAddress();
                    if (clientCommandParameters.length != 0) {
                        reportClientData += " with parameters ";
                        for (int i = 0; i < clientCommandParameters.length; i++) {
                            if (i != 0) {
                                reportClientData += ", ";
                            }
                            reportClientData += "'" + clientCommandParameters[i] + "'";
                        }
                    }
                    gui.writeLine(reportClientData);
                }
                System.out.println(clientCommand);
                if (clientCommand.equals("hello") || clientCommand.equals("handshake")) {
                    commandHandshake();
                }
                if (clientCommand.equals("login")) {
                    commandLogin(clientCommandParameters);
                }
                if (clientCommand.equals("executeQuerySelect") && clientCommandParameters.length > 0) {
                    commandExecuteQuerySelect(clientCommandParameters[0], Boolean.parseBoolean(clientCommandParameters[1]));
                }
                if (clientCommand.equals("executeQuery") && clientCommandParameters.length > 0) {
                    commandExecuteQuery(clientCommandParameters[0]);
                }
                if (clientCommand.equals("createBackup")) {
                    createBackup();
                }
                if (clientCommand.equals("restoreBackup") && clientCommandParameters.length == 0) {
                    commandRestoreBackup();
                }
                if (clientCommand.equals("restoreBackup") && clientCommandParameters.length == 1) {
                    commandRestoreBackup(clientCommandParameters[0]);
                }
                if (clientCommand.equals("sendLog")) {
                    commandSendLog();
                }
                inFromClient.close();
                connectionSocket.close();
            }
        } catch (BindException ex) {
            gui.writeLine("ERROR: Failed to initialize connection socket (address already in use).");
            ex.printStackTrace();
        } catch (Exception ex) {
            gui.writeLine("ERROR: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Client commando voor het herstellen van de database via een backup bestand
     * (standard locatie).
     * 
     * @param filename
     */
    public void commandRestoreBackup(String filename) {
        try {
            ObjectOutputStream objectOutToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
            objectOutToClient.writeObject(restoreBackup(filename));
            objectOutToClient.close();
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex);
        }
    }

    /**
     * Client commando voor het herstellen van de database via een backup bestand.
     */
    public void commandRestoreBackup() {
        gui.writeLine("Listing backupfiles");
        try {
            ObjectOutputStream objectOutToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
            objectOutToClient.writeObject(restoreBackup());
            objectOutToClient.close();
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex);
        }
    }

    /**
     * Client commando voor het uitvoeren van een query op de database. Als 
     * firstRowTableNames true is, wordt de eerste row gevuld met de namen van
     * de columns.
     * 
     * @param query
     * @param firstRowTableNames
     */
    public void commandExecuteQuerySelect(String query, Boolean firstRowTableNames) {
        gui.writeLine("Executing query... Getting information");
        try {
            ObjectOutputStream objectOutToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
            objectOutToClient.writeObject(executeQuery(query, firstRowTableNames));
            objectOutToClient.close();
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex);
        }
    }

    /**
     * Client commando voor het uivoeren van een query op de database. De 
     * returnwaarde geeft aan of de uitvoering gelukt is (0 of hoger) of niet
     * (-1). Als de bewerking gelukt is, zal de returnwaarde aangeven hoeveel
     * rows er aangepast waren.
     * 
     * @param query
     */
    public void commandExecuteQuery(String query) {
        gui.writeLine("Executing query... Modifying information");
        try {
            DataOutputStream dataOutToClient = new DataOutputStream(connectionSocket.getOutputStream());
            dataOutToClient.writeInt(executeQuery(query));
            dataOutToClient.close();
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex);
        }
    }

    /**
     * Deze functie stuurt een handshake terug naar de client.
     */
    public void commandHandshake() {
        try {
            DataOutputStream outToClientStream = new DataOutputStream(connectionSocket.getOutputStream());
            PrintWriter outToClient = new PrintWriter(outToClientStream, true);
            String IP = connectionSocket.getInetAddress().toString();
            outToClient.println("hello");
            gui.writeLine("Greeted " + IP);
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex);
        }
    }

    /**
     * Deze functie loopt na of de ingevoerde logininformatie correct is. Een
     * boolean wordt teruggestuurd naar client om aan te geven of het inloggen
     * gelukt is of niet. Als er een specifieke fout wordt de betreffende fout
     * achter de boolean gezet, zodat de client weet wat er precies fout is
     * gegaan tijdens het inloggen.
     * 
     * @param loginData
     */
    public void commandLogin(String[] loginData) {
        String IP = connectionSocket.getInetAddress().toString();
        try {
            DataOutputStream outToClientStream = new DataOutputStream(connectionSocket.getOutputStream());
            PrintWriter outToClient = new PrintWriter(outToClientStream, true);
            if (loginData.length > 0) {
                ArrayList<ArrayList<String>> result = executeQuery("SELECT * FROM [User] WHERE [Name] = '" + loginData[0] + "' AND [Password] = '" + loginData[1] + "'", false);
                if (result.size() > 0) {
                    gui.writeLine("Login correct for IP " + IP);
                    outToClient.println("true");
                } else {
                    gui.writeLine("Login incorrect for IP " + IP + " (wrong username/password)");
                    outToClient.println("false");
                }
            } else {
                outToClient.println("Login failed: wrong parameters!");
                gui.writeLine("Incorrect login for IP " + IP + " (wrong parameters).");
            }
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex);
        }
    }

    /**
     * Deze functie stuurt het logbestand naar de client.
     */
    public void commandSendLog() {
        String IP = connectionSocket.getInetAddress().toString();
        try {
            DataOutputStream outToClientStream = new DataOutputStream(connectionSocket.getOutputStream());
            PrintWriter outToClient = new PrintWriter(outToClientStream, true);
            FileReader reader = new FileReader("log.txt");
            BufferedReader in = new BufferedReader(reader);
            String text = "";
            String s = in.readLine();
            while (s != null) {
                text += s + "\n";
                s = in.readLine();
            }
            in.close();
            reader.close();
            outToClient.println(text.replace("\n", "\\n"));
            System.out.println("Logfile sent to IP " + IP);
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex);
        }
    }
}
