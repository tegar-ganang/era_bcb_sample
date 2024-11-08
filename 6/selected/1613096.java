package com.clanwts.bncs.client.test;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.clanwts.bncs.client.BattleNetChatClient;
import com.clanwts.bncs.client.BattleNetChatClientFactory;
import com.clanwts.bncs.client.SimpleBattleNetChatClientListener;
import com.clanwts.bncs.codec.standard.messages.Platform;
import com.clanwts.bncs.codec.standard.messages.Product;
import com.clanwts.bncs.codec.standard.messages.ProductType;

public class Test {

    private static int RECONNECT_DELAY_SEC = 10;

    private static Executor exec;

    private static BattleNetChatClient bncs;

    public static void main(String[] args) throws Exception {
        exec = Executors.newSingleThreadExecutor();
        BattleNetChatClientFactory fact = new BattleNetChatClientFactory();
        fact.setPlatform(Platform.X86);
        fact.setProduct(Product.W3TFT_1_23A);
        fact.setKeys("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        fact.setKeyOwner("Richard Milhous Nixon");
        bncs = fact.createClient();
        bncs.addListener(new Listener());
        doConnect();
    }

    private static void doConnect() throws Exception {
        System.out.println("[BNET] Beginning connection...");
        bncs.connect(new InetSocketAddress("localhost", 6112)).get();
        System.out.println("[BNET] Connection complete.");
        System.out.println("[BNET] Beginning login...");
        bncs.login("WTSBot", "password", true).get();
        System.out.println("[BNET] Login complete.");
        System.out.println("[BNET] Joining channel...");
        bncs.join("W3").get();
        System.out.println("[BNET] Join channel complete.");
    }

    private static class AsyncReconnectRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    System.out.println("[BNET] Waiting " + RECONNECT_DELAY_SEC + " seconds until reconnect.");
                    Thread.sleep(RECONNECT_DELAY_SEC * 1000);
                    System.out.println("[BNET] Reconnecting...");
                    doConnect();
                    System.out.println("[BNET] Reconnect SUCCESS!");
                    break;
                } catch (Exception e) {
                    System.out.println("[BNET] Reconnect FAILED!");
                }
            }
        }
    }

    private static class Listener extends SimpleBattleNetChatClientListener {

        @Override
        public void forcedDisconnect() {
            System.out.println("[BNET] Forced disconnect!");
            exec.execute(new AsyncReconnectRunnable());
        }

        @Override
        public void channelChatReceived(String user, String message) {
            System.out.println("[CHANNEL] " + user + ": " + message);
        }

        @Override
        public void forcedJoin(String channel) {
            System.out.println("[CHANNEL] You have been forcefully joined to channel " + channel + ".");
        }

        @Override
        public void forcedPart() {
            System.out.println("[CHANNEL] You have been forcefully parted from the channel.");
        }

        @Override
        public void privateChatReceived(String user, String message) {
            System.out.println("[WHISPER] " + user + ": " + message);
        }

        @Override
        public void errorMessageReceived(String msg) {
            System.out.println("[ERROR] " + msg);
        }

        @Override
        public void infoMessageReceived(String msg) {
            System.out.println("[INFO] " + msg);
        }

        @Override
        public void otherJoined(String user, ProductType type) {
            System.out.println("[CHANNEL] " + user + " (" + type + ") has joined the channel.");
        }

        @Override
        public void otherParted(String user) {
            System.out.println("[CHANNEL] " + user + " has parted the channel.");
        }

        @Override
        public void otherShown(String user, ProductType type) {
            System.out.println("[CHANNEL] " + user + " (" + type + ") is currently in the channel.");
        }
    }
}
