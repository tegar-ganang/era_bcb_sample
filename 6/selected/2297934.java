package games.midhedava.client;

import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import marauroa.client.ariannexpTimeoutException;
import marauroa.client.net.DefaultPerceptionListener;
import marauroa.client.net.PerceptionHandler;
import marauroa.common.game.RPAction;
import marauroa.common.game.RPObject;
import marauroa.common.net.MessageS2CPerception;
import marauroa.common.net.TransferContent;

public class textClient extends Thread {

    private String host;

    private String username;

    private String password;

    private String character;

    private String port;

    private static boolean ShowWorld = false;

    private Map<RPObject.ID, RPObject> world_objects;

    private marauroa.client.ariannexp clientManager;

    private PerceptionHandler handler;

    public textClient(String h, String u, String p, String c, String P, boolean t) throws SocketException {
        host = h;
        username = u;
        password = p;
        character = c;
        port = P;
        world_objects = new HashMap<RPObject.ID, RPObject>();
        handler = new PerceptionHandler(new DefaultPerceptionListener() {

            @Override
            public int onException(Exception e, marauroa.common.net.MessageS2CPerception perception) {
                e.printStackTrace();
                System.out.println(perception);
                return 0;
            }
        });
        clientManager = new marauroa.client.ariannexp("games/midhedava/log4j.properties") {

            @Override
            protected String getGameName() {
                return "midhedava";
            }

            @Override
            protected String getVersionNumber() {
                return midhedava.VERSION;
            }

            @Override
            protected void onPerception(MessageS2CPerception message) {
                try {
                    System.out.println("Received perception " + message.getPerceptionTimestamp());
                    handler.apply(message, world_objects);
                    int i = message.getPerceptionTimestamp();
                    RPAction action = new RPAction();
                    if (i % 50 == 0) {
                        action.put("type", "move");
                        action.put("dy", "-1");
                        clientManager.send(action);
                    } else if (i % 50 == 20) {
                        action.put("type", "move");
                        action.put("dy", "1");
                        clientManager.send(action);
                    }
                    if (ShowWorld) {
                        System.out.println("<World contents ------------------------------------->");
                        int j = 0;
                        for (RPObject object : world_objects.values()) {
                            j++;
                            System.out.println(j + ". " + object);
                        }
                        System.out.println("</World contents ------------------------------------->");
                    }
                } catch (Exception e) {
                    onError(3, "Exception while applying perception");
                }
            }

            @Override
            protected List<TransferContent> onTransferREQ(List<TransferContent> items) {
                for (TransferContent item : items) {
                    item.ack = true;
                }
                return items;
            }

            @Override
            protected void onTransfer(List<TransferContent> items) {
                System.out.println("Transfering ----");
                for (TransferContent item : items) {
                    System.out.println(item);
                }
            }

            @Override
            protected void onAvailableCharacters(String[] characters) {
                System.out.println("Characters available");
                for (String characterAvail : characters) {
                    System.out.println(characterAvail);
                }
                try {
                    chooseCharacter(character);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void onServerInfo(String[] info) {
                System.out.println("Server info");
                for (String info_string : info) {
                    System.out.println(info_string);
                }
            }

            @Override
            protected void onError(int code, String reason) {
                System.out.println(reason);
            }
        };
    }

    @Override
    public void run() {
        try {
            clientManager.connect(host, Integer.parseInt(port), true);
            clientManager.login(username, password);
        } catch (SocketException e) {
            return;
        } catch (ariannexpTimeoutException e) {
            System.out.println("textClient can't connect to Midhedava server. Server is down?");
            return;
        }
        boolean cond = true;
        while (cond) {
            clientManager.loop(0);
            try {
                sleep(100);
            } catch (InterruptedException e) {
            }
            ;
        }
        while (clientManager.logout() == false) {
            ;
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                int i = 0;
                String username = null;
                String password = null;
                String character = null;
                String host = null;
                String port = null;
                boolean tcp = false;
                while (i != args.length) {
                    if (args[i].equals("-u")) {
                        username = args[i + 1];
                    } else if (args[i].equals("-p")) {
                        password = args[i + 1];
                    } else if (args[i].equals("-c")) {
                        character = args[i + 1];
                    } else if (args[i].equals("-h")) {
                        host = args[i + 1];
                    } else if (args[i].equals("-P")) {
                        port = args[i + 1];
                    } else if (args[i].equals("-W")) {
                        if ("1".equals(args[i + 1])) {
                            ShowWorld = true;
                        }
                    } else if (args[i].equals("-t")) {
                        tcp = true;
                    }
                    i++;
                }
                if ((username != null) && (password != null) && (character != null) && (host != null) && (port != null)) {
                    System.out.println("Parameter operation");
                    new textClient(host, username, password, character, port, tcp).start();
                    return;
                }
            }
            System.out.println("Midhedava textClient");
            System.out.println();
            System.out.println("  games.midehdava.textClient -u username -p pass -h host -P port -c character");
            System.out.println();
            System.out.println("Required parameters");
            System.out.println("* -h\tHost that is running Marauroa server");
            System.out.println("* -P\tPort on which Marauroa server is running");
            System.out.println("* -u\tUsername to log into Marauroa server");
            System.out.println("* -p\tPassword to log into Marauroa server");
            System.out.println("* -c\tCharacter used to log into Marauroa server");
            System.out.println("Optional parameters");
            System.out.println("* -W\tShow world content? 0 or 1");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
