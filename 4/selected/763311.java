package edu.simplemqom.test;

import java.util.concurrent.TimeUnit;
import edu.simplemqom.Message;
import edu.simplemqom.MessageBrokerController;
import edu.simplemqom.objects.SMQOMChannel;
import edu.simplemqom.objects.SMQOMQueue;

/**
 * @author Pawe�
 *
 */
public class TestReceiver implements Runnable {

    private MessageBrokerController controller;

    public TestReceiver(MessageBrokerController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            SMQOMChannel channel = controller.getChannel("TESTCHANNEL");
            SMQOMQueue test1In = channel.getQueue("TEST1.IN");
            SMQOMQueue test2In = channel.getQueue("TEST2.IN");
            int test1InCounter = 0;
            int test2InCounter = 0;
            Message m11, m22;
            do {
                m11 = test1In.poll(10000L, TimeUnit.MILLISECONDS);
                if (m11 != null) {
                    test1InCounter++;
                } else {
                    System.out.println("Failed to get message from TEST1.IN");
                }
                m22 = test2In.poll(10000L, TimeUnit.MILLISECONDS);
                if (m22 != null) {
                    test2InCounter++;
                } else {
                    System.out.println("Failed to get message from TEST2.IN");
                }
            } while (m11 != null || m22 != null);
            System.out.println("\n\tTEST1.IN: " + test1InCounter + "\n\tTEST2.IN: " + test2InCounter);
            SMQOMQueue error = channel.getQueue(channel.getName() + ".ERR");
            System.out.println("Error Queue length: " + error.size());
        } catch (InterruptedException e) {
        }
    }
}
