package coopnetserver.utils;

import coopnetserver.ConnectionDropper;
import coopnetserver.Globals;
import coopnetserver.data.channel.Channel;
import coopnetserver.data.channel.ChannelData;
import coopnetserver.data.player.Player;
import coopnetserver.data.player.PlayerData;
import coopnetserver.protocol.out.Protocol;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public final class RuntimeConsoleCommands {

    private RuntimeConsoleCommands() {
    }

    public static void bindAndListen() {
        try {
            ServerSocket socket = new ServerSocket(6668, 5, InetAddress.getByName("localhost"));
            while (true) {
                try {
                    final Socket s = socket.accept();
                    new ErrThread() {

                        @Override
                        public void handledRun() throws Throwable {
                            startProcessing(s);
                        }
                    }.start();
                } catch (Exception e) {
                    Logger.log(e);
                }
            }
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public static void startProcessing(Socket socket) throws IOException {
        Scanner sc = new Scanner(socket.getInputStream());
        PrintWriter pw = new PrintWriter(socket.getOutputStream());
        while (sc.hasNext()) {
            String lastcommand = sc.nextLine();
            synchronized (RuntimeConsoleCommands.class) {
                try {
                    if (lastcommand.startsWith("startdropping")) {
                        if (!ConnectionDropper.isRunning()) {
                            new ConnectionDropper().start();
                        }
                        continue;
                    }
                    if (lastcommand.startsWith("stopdropping")) {
                        ConnectionDropper.setRunning(false);
                        continue;
                    }
                    if (lastcommand.startsWith("shutdown")) {
                        Protocol.sendServerShuttingDown();
                        System.exit(0);
                    }
                    if (lastcommand.startsWith("list online players")) {
                        pw.println("ONLINE PLAYERS:");
                        for (Player p : PlayerData.getOnlinePlayers()) {
                            pw.println(p.getLoginName());
                        }
                        pw.println("--");
                        pw.flush();
                        continue;
                    }
                    if (lastcommand.startsWith("start debug")) {
                        Globals.setDebug(true);
                        continue;
                    }
                    if (lastcommand.startsWith("stop debug")) {
                        Globals.setDebug(false);
                        continue;
                    }
                    if (lastcommand.startsWith("onlinecount")) {
                        pw.println("ONLINE PLAYERS:" + PlayerData.getOnlinePlayers().size());
                        pw.flush();
                        continue;
                    }
                    if (lastcommand.startsWith("list channels")) {
                        pw.println("OPEN CHANNELS:<usercount>");
                        for (Channel ch : ChannelData.getChannels().values()) {
                            pw.println(ch.ID + ": " + ch.getPlayersInChannel().length);
                        }
                        pw.println("--");
                        pw.flush();
                        continue;
                    }
                    if (lastcommand.startsWith("help")) {
                        pw.println("\rServer version: " + Globals.getServerVersion());
                        pw.println("\rAvaible commands:");
                        pw.println("\rshutdown - stops the server\n" + "\rlist online players - prints the online players\n" + "\ronlinecount - the number of conencted cliens\n" + "\rlist channels - lists the open channels\n" + "\rstartdropping - start the connectiondropper thread\n" + "\rstopdropping - stops the connectiondropper thread\n");
                        pw.flush();
                        continue;
                    }
                    pw.println("unknown command (type help for usage):" + lastcommand);
                    pw.flush();
                } catch (Exception e) {
                    pw.println("error while executing console command:");
                    pw.flush();
                    e.printStackTrace();
                }
            }
        }
    }
}
