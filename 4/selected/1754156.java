package org.digitall.common.digitalk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Vector;
import org.digitall.lib.common.ConfigFile;
import org.digitall.lib.sql.LibSQLMini;

public class ChWYServerThread extends Thread {

    private Socket clientSocket = null;

    private DataInputStream inputStream = null;

    private DataOutputStream outputStream = null;

    private String userName = "";

    private Vector userList;

    private boolean connected = false;

    private static String ServerIP = "";

    private static String DBName = "";

    private static String DBString = "jdbc:postgresql://" + ServerIP + "/" + DBName;

    private static String nombreUsuario = "";

    private static String valor = "";

    private ConfigFile configFile;

    private boolean isSuperUsuario = false;

    private String nombreOrganizacion = "";

    private LibSQLMini libSQL = new LibSQLMini();

    public ChWYServerThread(Socket _clientSocket, Vector _userList) {
        userList = _userList;
        clientSocket = _clientSocket;
    }

    public ChWYServerThread(Socket _clientSocket, Vector _userList, ConfigFile _configFile) {
        configFile = _configFile;
        userList = _userList;
        clientSocket = _clientSocket;
        cargarConfiguracion();
    }

    public ChWYServerThread(Socket _clientSocket, HashMap _listadoSistemas) {
        clientSocket = _clientSocket;
    }

    private void cargarConfiguracion() {
        ServerIP = configFile.getProperty("serverlocal");
        DBName = configFile.getProperty("bdlocal");
        DBString = "jdbc:postgresql://" + ServerIP + "/" + DBName;
        nombreUsuario = configFile.getProperty("usuario");
        valor = configFile.getProperty("valor");
    }

    public void run() {
        try {
            inputStream = new DataInputStream(clientSocket.getInputStream());
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            connected = true;
        } catch (IOException x) {
            x.printStackTrace();
            connected = false;
        }
        String command;
        int status = 0;
        while (connected) {
            try {
                System.out.println("Waiting for command:");
                command = inputStream.readUTF();
                System.out.print("Command received: " + command);
                if (command.startsWith(ChWYConfig.USERNAME)) {
                    iniciarSesion(command);
                } else if (command.startsWith(ChWYConfig.RTRVUSERLIST)) {
                } else if (command.startsWith(ChWYConfig.MESSAGE_ORIG)) {
                    enviarMensaje(command);
                }
            } catch (IOException e) {
                System.out.println("Connection closed");
                userList.removeElement(this);
                sendUserList();
                connected = false;
                break;
            }
        }
        userList.removeElement(this);
        try {
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean sendMessage(String _message, ChWYServerThread _destThread) {
        String answer = "";
        int status = 0;
        int tries = 0;
        boolean messageSent = false;
        try {
            System.out.print("Writing message to destination user");
            _destThread.outputStream.writeUTF(ChWYConfig.MESSAGE_ORIG + userName + ";" + ChWYConfig.MESSAGE_STRING + _message);
            System.out.print("Message sent");
        } catch (IOException e) {
            org.digitall.lib.components.Advisor.messageBox("Error al enviar mensaje", "Error");
            e.printStackTrace();
        }
        return messageSent;
    }

    private String tryToGetUserName() {
        int tries = 0;
        System.out.println("Trying to get the user name");
        while (userName.equals("") && tries < 10) {
            try {
                System.out.println("Waiting...");
                String command = inputStream.readUTF();
                setUserName(command);
                System.out.println("Command received: " + command);
                connected = true;
            } catch (IOException e) {
                setUserName("");
                connected = false;
                e.printStackTrace();
            }
            tries++;
        }
        if (userName.equals("") || tries >= 10) {
            org.digitall.lib.components.Advisor.messageBox("Error getting user name, so... you're disconnected", "Error");
            disconnect();
        }
        return userName;
    }

    private void disconnect() {
        connected = false;
    }

    public void setUserName(String _userName) {
        try {
            if (_userName.length() > 0) {
                userName = _userName;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            userName = "";
        }
    }

    public String getUserName() {
        return userName;
    }

    private void sendUserList() {
        for (int j = 0; j < userList.size(); j++) {
            String users = ChWYConfig.USERLIST;
            ChWYServerThread hilo = (ChWYServerThread) (userList.elementAt(j));
            if (hilo.nombreOrganizacion.equals(this.nombreOrganizacion)) {
                String _tempUserName = ((ChWYServerThread) (userList.elementAt(j))).getUserName();
                System.out.println("Sending user list to user: " + _tempUserName);
                DataOutputStream _tempOutputStream = ((ChWYServerThread) (userList.elementAt(j))).outputStream;
                for (int i = 0; i < userList.size(); i++) {
                    ChWYServerThread hilo2 = (ChWYServerThread) (userList.elementAt(i));
                    if (hilo.nombreOrganizacion.equals(this.nombreOrganizacion)) {
                        String sendUser = ((ChWYServerThread) (userList.elementAt(i))).getUserName();
                        if ((!sendUser.equals(_tempUserName)) && (hilo2.nombreOrganizacion.equals(this.nombreOrganizacion))) {
                            users += ChWYConfig.USERLISTITEM + sendUser;
                            System.out.println("Sending user " + sendUser + " to user: " + ((ChWYServerThread) (userList.elementAt(j))).getUserName());
                        }
                    }
                }
                try {
                    _tempOutputStream.writeUTF(users);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addUsuario(String _orgName) {
        userList.add(this);
    }

    /**2010-03-15(moraless)*/
    private void iniciarSesion(String _comandos) {
        String[] comandos = _comandos.split(";");
        userName = comandos[0].replaceFirst(ChWYConfig.USERNAME, "");
        System.out.println("Welcome user: " + userName);
        nombreOrganizacion = comandos[1];
        addUsuario(comandos[1].replaceFirst(ChWYConfig.ORGANIZATIONNAME, ""));
        if (mostrarListadoCompleto(userName)) {
            nombreOrganizacion = "orgName:ADMIN";
            sendUserList();
        } else {
            sendUserList();
        }
        String params = "'" + comandos[2].replaceFirst(SystemInformation.USERSYSTEM, "") + "','" + comandos[3].replaceFirst(SystemInformation.DATETIME, "") + "','" + comandos[4].replaceFirst(SystemInformation.NAMESYSTEM, "") + "','" + comandos[5].replaceFirst(SystemInformation.HOSTNAME, "") + "','" + comandos[6].replaceFirst(SystemInformation.DATABASE, "") + "','" + comandos[7].replaceFirst(SystemInformation.IPADDRESS, "") + "','" + comandos[8].replaceFirst(SystemInformation.MACADDRESS, "") + "','" + comandos[9].replaceFirst(SystemInformation.VERSIONJVM, "") + "','" + comandos[10].replaceFirst(SystemInformation.SO, "") + "','" + comandos[11].replaceFirst(SystemInformation.RAM, "") + "','" + comandos[12].replaceFirst(SystemInformation.PROCESSOR, "") + "','" + comandos[13].replaceFirst(SystemInformation.RESOLUTION, "") + "','','" + SystemInformation.getHostName() + "'";
        libSQL.closeConnection();
        libSQL.setDataBaseString(DBString);
        libSQL.tryToConnect(nombreUsuario, valor);
        if (libSQL.getInt("auditorias.addlogsystem", params) > -1) {
            System.out.println("Se guardaron los par�metros recibidos");
        } else {
            System.out.println("No se guardaron los par�metros recibidos");
        }
    }

    /**2010-03-15(moraless)*/
    private boolean mostrarListadoCompleto(String _userName) {
        boolean retornar = false;
        String usuarioSuper = _userName.substring(0, _userName.indexOf("@"));
        ResultSet result = libSQL.exQuery("SELECT usesysid=any(pg_group.grolist) as belongs FROM pg_user, pg_group  WHERE groname = 'chwy' AND pg_user.usename = '" + usuarioSuper + "'");
        try {
            if (result.next()) {
                retornar = result.getBoolean("belongs");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retornar;
    }

    private void enviarMensaje(String _comandos) {
        String origUser = "";
        String destUser = "";
        String message = "";
        String[] comandos = _comandos.split(";");
        String orgName = comandos[1];
        origUser = comandos[0].replaceFirst(ChWYConfig.MESSAGE_ORIG + "", "");
        destUser = comandos[2].replaceFirst(ChWYConfig.MESSAGE_DEST + "", "");
        message = comandos[3].replaceAll(ChWYConfig.MESSAGE_STRING, "");
        ChWYServerThread destThread = null;
        boolean found = false;
        for (int i = 0; i < userList.size(); i++) {
            ChWYServerThread hilo = (ChWYServerThread) userList.elementAt(i);
            String sendUser = ((ChWYServerThread) (userList.elementAt(i))).getUserName();
            if ((hilo.nombreOrganizacion.equals(orgName)) && (sendUser.equals(destUser))) {
                destThread = (ChWYServerThread) (userList.elementAt(i));
                found = true;
            }
        }
        if (found) {
            if (!sendMessage(message, destThread)) {
            }
        }
    }
}
