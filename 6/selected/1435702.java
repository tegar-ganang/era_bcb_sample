package br.com.mendsoft.gtalk.bot.fs.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import br.com.mendsoft.gtalk.bot.fs.commands.ACommand;
import br.com.mendsoft.gtalk.bot.fs.config.Configuration;

/**
 * @author Daniel
 *
 */
public class GTalkBotFS extends Thread {

    private String fileCommandConfig;

    private String fileUserConfig;

    private Map<String, ACommand> commands;

    private Configuration configuration;

    private Map<String, String> pathCurrentSender = new HashMap<String, String>();

    private XMPPConnection connection;

    /**
	 * @param args 
	 */
    public static void main(String[] args) {
        GTalkBotFS thread = new GTalkBotFS();
        String fileUserConfig = null;
        String fileCommandConfig = null;
        for (String arg : args) {
            if (arg.startsWith("-u=")) {
                fileUserConfig = arg.substring(3);
            } else if (arg.startsWith("-c=")) {
                fileCommandConfig = arg.substring(3);
            }
        }
        thread.setFileUserConfig(fileUserConfig);
        thread.setFileCommandConfig(fileCommandConfig);
        thread.start();
    }

    public void run() {
        super.run();
        try {
            loadUserConfiguration();
            loadCommandsConfiguration();
            openConnection();
            listenConversations();
            while (!interrupted()) {
                sleep(1);
            }
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (connection != null) {
            connection.disconnect();
        }
        System.exit(0);
    }

    /**
	 * 
	 * @throws Exception
	 */
    private void loadUserConfiguration() throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(getFileUserConfig()));
        String str = "";
        String username = "";
        String password = "";
        String usersSenders = "";
        String chave = "";
        String valor = "";
        String[] listUsers = null;
        int index = 0;
        while (in.ready()) {
            str = in.readLine();
            index = str.indexOf("=");
            if (index > 0) {
                chave = str.substring(0, index);
                valor = str.substring(index + 1);
                if (chave.equals("username")) {
                    username = valor;
                } else if (chave.equals("password")) {
                    password = valor;
                } else if (chave.equals("userSender")) {
                    usersSenders = valor;
                }
            }
        }
        listUsers = usersSenders.split(",");
        configuration = new Configuration(username, password, listUsers);
        in.close();
    }

    /**
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
    private void loadCommandsConfiguration() throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(getFileCommandConfig()));
        String str = "";
        commands = new HashMap<String, ACommand>();
        int index = 0;
        while (in.ready()) {
            str = in.readLine();
            index = str.indexOf("=");
            Class clasz = Class.forName(str.substring(index + 1));
            commands.put(str.substring(0, index), (ACommand) clasz.newInstance());
        }
        in.close();
    }

    /**
	 * @throws XMPPException
	 */
    private void openConnection() throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        config.setSASLAuthenticationEnabled(true);
        config.setReconnectionAllowed(true);
        connection = new XMPPConnection(config);
        connection.connect();
        String userWithoutArroba = configuration.getUsername().substring(0, configuration.getUsername().lastIndexOf("@"));
        connection.login(userWithoutArroba, configuration.getPassword());
    }

    /**
	 * 
	 */
    private void listenConversations() {
        ChatManager chatManager = connection.getChatManager();
        chatManager.addChatListener(getChatMessageListener());
    }

    /**
	 * @return
	 */
    private ChatManagerListener getChatMessageListener() {
        ChatManagerListener charMessageListener = new ChatManagerListener() {

            public void chatCreated(Chat chat, boolean arg1) {
                if (checkSenders(chat)) {
                    chat.addMessageListener(new MessageListener() {

                        public void processMessage(Chat chat, Message message) {
                            try {
                                listenMessage(chat, message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        };
        return charMessageListener;
    }

    /**
	 * @param chat
	 * @param message
	 * @throws Exception
	 */
    private void listenMessage(Chat chat, Message message) throws Exception {
        String text = null;
        int index = message.getBody().indexOf(" ");
        String commandName = "";
        String argCommand = "";
        String pathCurrent = "";
        if (index < 0) {
            commandName = message.getBody();
            argCommand = "";
        } else {
            commandName = message.getBody().substring(0, index);
            argCommand = message.getBody().substring(index + 1);
        }
        if (commandName.equals("exit")) {
            System.out.println("Fechando GTalkSendFile.");
            interrupt();
        } else {
            Iterator<Entry<String, ACommand>> iteCommand = null;
            iteCommand = commands.entrySet().iterator();
            while (iteCommand.hasNext()) {
                Entry<String, ACommand> entry = (Entry<String, ACommand>) iteCommand.next();
                if (entry.getKey().equals(commandName)) {
                    pathCurrent = pathCurrentSender.get(getUserSender(chat));
                    if (pathCurrent == null) {
                        pathCurrent = "";
                    }
                    entry.getValue().setPathCurrent(pathCurrent);
                    entry.getValue().setConfiguration(configuration);
                    entry.getValue().setUserSender(getUserSender(chat));
                    text = entry.getValue().execute(argCommand);
                    pathCurrentSender.put(getUserSender(chat), entry.getValue().getPathCurrent());
                }
            }
            if (text != null) {
                chat.sendMessage(text);
            } else {
                System.out.println("Mensagem n�o entendida. [" + message.getBody() + "]");
            }
        }
    }

    /**
	 * @param chat
	 * @return
	 */
    private boolean checkSenders(Chat chat) {
        for (String userSender : configuration.getUsersSenders()) {
            if (chat != null && chat.getParticipant().startsWith(userSender)) {
                return true;
            }
        }
        return false;
    }

    /**
	 * @param chat
	 * @return
	 */
    private String getUserSender(Chat chat) {
        for (String userSender : configuration.getUsersSenders()) {
            if (chat != null && chat.getParticipant().startsWith(userSender)) {
                return userSender;
            }
        }
        return null;
    }

    /**
	 * @return the fileCommandConfig
	 */
    public String getFileCommandConfig() {
        if (fileCommandConfig == null) {
            fileCommandConfig = System.getProperty("user.dir") + "\\..\\conf\\command.conf";
        }
        return fileCommandConfig;
    }

    /**
	 * @param fileCommandConfig the fileCommandConfig to set
	 */
    public void setFileCommandConfig(String fileCommandConfig) {
        this.fileCommandConfig = fileCommandConfig;
    }

    /**
	 * @return the fileUserConfig
	 */
    public String getFileUserConfig() {
        if (fileUserConfig == null) {
            fileUserConfig = System.getProperty("user.dir") + "\\..\\conf\\user.conf";
        }
        return fileUserConfig;
    }

    /**
	 * @param fileUserConfig the fileUserConfig to set
	 */
    public void setFileUserConfig(String fileUserConfig) {
        this.fileUserConfig = fileUserConfig;
    }
}
