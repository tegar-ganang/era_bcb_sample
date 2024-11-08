package org.midhedava.plrmgr;

import java.net.SocketException;
import java.util.List;
import java.util.ResourceBundle;
import marauroa.client.ariannexpTimeoutException;
import marauroa.common.game.RPAction;
import marauroa.common.net.MessageS2CPerception;
import marauroa.common.net.TransferContent;

public class MarauroaPlayerManager {

    private marauroa.client.ariannexp clientManager;

    private String character;

    private static final String version;

    private static final String game;

    private boolean transferred;

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("midhedava");
        version = bundle.getString("version");
        game = bundle.getString("game");
    }

    public MarauroaPlayerManager() throws SocketException {
        clientManager = new marauroa.client.ariannexp("log4j.properties") {

            @Override
            protected String getGameName() {
                return game;
            }

            @Override
            protected String getVersionNumber() {
                return version;
            }

            @Override
            protected void onPerception(MessageS2CPerception message) {
            }

            @Override
            protected List<TransferContent> onTransferREQ(List<TransferContent> items) {
                for (TransferContent item : items) {
                    item.ack = true;
                }
                transferred = true;
                return items;
            }

            @Override
            protected void onTransfer(List<TransferContent> items) {
            }

            @Override
            protected void onAvailableCharacters(String[] characters) {
                try {
                    chooseCharacter(character);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void onServerInfo(String[] info) {
            }

            @Override
            protected void onError(int code, String reason) {
                System.out.println("Error: " + reason);
            }
        };
    }

    /**
	 * This method created an account based on the information in PlayerData
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
    public boolean createAccount(PlayerData data) throws Exception {
        try {
            character = data.getCharacterName();
            clientManager.connect(data.getServer(), data.getPort(), true);
            if (clientManager.createAccount(data.getPlayerName(), data.getPassword(), data.getEmail()) == false) {
                String result = clientManager.getEvent();
                if (result == null) {
                    result = "The server is not responding. Please check that it is online, and that you " + "supplied the correct details.";
                }
                throw new Exception(result);
            }
        } catch (ariannexpTimeoutException ex) {
            String result = "Unable to connect to server to create your account. " + "The server may be down or, if you are using a custom server, you may have entered its " + "name and port number incorrectly.";
            throw new Exception(result);
        }
        try {
            if (clientManager.login(data.getPlayerName(), data.getPassword()) == false) {
                String result = clientManager.getEvent();
                if (result == null) {
                    result = "Unable to connect to server. The server may be down or, if you are using a " + "custom server, you may have entered its name and port number incorrectly.";
                    throw new Exception(result);
                }
            } else {
                int i = 0;
                while (!transferred) {
                    clientManager.loop(0);
                }
                transferred = false;
                setInitialAtributes(data.getRace(), data.getSex());
                while (i++ < 30) {
                    clientManager.loop(0);
                }
                while (clientManager.logout() == false) {
                    ;
                }
            }
        } catch (ariannexpTimeoutException ex) {
            String result = "Unable to connect to the server. The server may be down or, if you are using a " + "custom server, you may have entered its name and port number incorrectly.";
            throw new Exception(result);
        }
        return true;
    }

    private void setInitialAtributes(String race, String sex) {
        RPAction rpaction = new RPAction();
        rpaction.put("type", "InitialAtributes");
        rpaction.put("race", race);
        rpaction.put("sex", sex);
        clientManager.send(rpaction);
    }
}
