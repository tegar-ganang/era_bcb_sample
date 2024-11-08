package com.emcaster.topics;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;
import com.emcaster.topics.Message;
import com.emcaster.topics.UdpPublisher;
import com.emcaster.topics.UdpSubscriber;

public class Main {

    public static void main(String[] args) throws Exception {
        String address = "224.0.0.23";
        int port = 8001;
        receive(address, port);
    }

    private static void send(String address, int port) throws InterruptedException {
        UdpPublisher pub = new UdpPublisher(address, port);
        pub.connect();
        BatchWriter writer = new BatchWriter(1024 * 25, pub, pub.getAddress(), pub.getPort());
        writer.setAfterFlushSleep(0);
        Thread thread = new Thread(writer);
        thread.start();
        int count = 0;
        long startTime = System.currentTimeMillis();
        byte[] bytes = new byte[0];
        while (true) {
            count++;
            writer.publish("test", bytes, 0, bytes.length);
            if ((count % 500000) == 0) {
                long totalTime = System.currentTimeMillis() - startTime;
                System.out.println("count: " + count);
                double avg = count / (totalTime / 1000.00);
                System.out.println("avg/sec: " + avg);
                count = 0;
                startTime = System.currentTimeMillis();
            }
        }
    }

    private static void receive(String address, int port) throws IOException {
        UdpSubscriber sub = new UdpSubscriber(address, port, 64 * 1024);
        sub.connect();
        MessageCounter counter = new MessageCounter();
        while (true) {
            Iterator<Message> iter = sub.readNext();
            while (iter.hasNext()) {
                counter.onMessage(iter.next());
            }
        }
    }

    private static void receivePattern(String address, int port) throws IOException {
        UdpSubscriber sub = new UdpSubscriber(address, port, 64 * 1024);
        sub.connect();
        Pattern pattern = Pattern.compile(".*");
        MessageListener receiver = new MessageCounter();
        PatternListener listener = new PatternListener(pattern, receiver);
        SubscriberRunnable runnable = new SubscriberRunnable(sub);
        runnable.add(listener);
        runnable.dispatchMessages();
    }

    private static class MessageCounter implements MessageListener {

        long count = 0;

        long startTime = System.currentTimeMillis();

        public void onMessage(Message msg) {
            count++;
            if ((count % 500000) == 0) {
                long totalTime = System.currentTimeMillis() - startTime;
                System.out.println("count: " + count);
                double avg = count / (totalTime / 1000.00);
                System.out.println("avg/sec: " + avg);
                count = 0;
                startTime = System.currentTimeMillis();
            }
        }
    }
}
