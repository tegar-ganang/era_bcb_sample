package ch.unifr.nio.framework.mockups;

import ch.unifr.nio.framework.AbstractChannelHandler;
import ch.unifr.nio.framework.ChannelHandler;
import ch.unifr.nio.framework.HandlerAdapter;
import ch.unifr.nio.framework.transform.AbstractForwarder;
import ch.unifr.nio.framework.transform.ChannelWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * a ChannelHandler for tests
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class TestChannelHandler extends AbstractChannelHandler {

    /**
     * determines what the TestChannelHandler does with the input
     */
    public enum InputHandling {

        /**
         * the input is echoed
         */
        ECHO, /**
         * the input is forwarded
         */
        FORWARD
    }

    private InputHandling inputHandling;

    private Field interestOpsField;

    private final Lock lock = new ReentrantLock();

    private final Condition enqueued = lock.newCondition();

    private final Condition forwarded = lock.newCondition();

    private final Condition inputClosedCondition = lock.newCondition();

    private boolean echoDone;

    private boolean forwardDone;

    private boolean closeDone;

    private int closeCounter;

    private ChannelHandler peer;

    /**
     * creates a new TestChannelHandler
     */
    public TestChannelHandler() {
        inputHandling = InputHandling.ECHO;
        channelReader.setNextForwarder(new TestTransformer());
    }

    @Override
    public void channelRegistered(HandlerAdapter handlerAdapter) {
        super.channelRegistered(handlerAdapter);
        try {
            interestOpsField = HandlerAdapter.class.getDeclaredField("cachedInterestOps");
            interestOpsField.setAccessible(true);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void inputClosed() {
        lock.lock();
        try {
            closeDone = true;
            closeCounter++;
            inputClosedCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * waits until the input is closed
     * @throws java.lang.InterruptedException
     */
    public void waitForInputClosed() throws InterruptedException {
        lock.lock();
        try {
            while (!closeDone) {
                inputClosedCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void channelException(Exception exception) {
    }

    /**
     * waits until the TestChannelHandler blocks
     * @throws java.lang.InterruptedException
     */
    public void waitForBlock() throws InterruptedException {
        lock.lock();
        try {
            while (!echoDone) {
                enqueued.await();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * wait until the data has been forwarded
     * @return true, if the data could be forwarded sucessfully, false otherwise
     * @throws java.lang.InterruptedException
     */
    public boolean hasForwarded() throws InterruptedException {
        lock.lock();
        try {
            if (forwardDone) {
                return true;
            } else {
                return forwarded.await(3, TimeUnit.SECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * returns the cached interest ops
     * @return the cached interest ops
     * @throws java.lang.IllegalAccessException if the interest ops could not be
     * read
     */
    public int getCachedInterestOps() throws IllegalAccessException {
        return interestOpsField.getInt(handlerAdapter);
    }

    /**
     * returns the selectionKey of the TestChannelHandler
     * @return the selectionKey of the TestChannelHandler
     * @throws java.lang.NoSuchFieldException if there is no field "key" in
     * TestChannelHandler
     * @throws java.lang.IllegalAccessException if the field "key" of
     * TestChannelHandler is not accessible
     */
    public SelectionKey getSelectionKey() throws NoSuchFieldException, IllegalAccessException {
        Field selectionKeyField = HandlerAdapter.class.getDeclaredField("selectionKey");
        selectionKeyField.setAccessible(true);
        return (SelectionKey) selectionKeyField.get(handlerAdapter);
    }

    /**
     * sets the peer of this channel handler
     * @param peer the peer of this channel handler
     */
    public void setPeer(ChannelHandler peer) {
        this.peer = peer;
    }

    /**
     * sets the input handling method
     * @param inputHandling the input handling method
     */
    public void setInputHandling(InputHandling inputHandling) {
        this.inputHandling = inputHandling;
    }

    /**
     * returns the HandlerAdapter of the TestChannelHandler
     * @return the HandlerAdapter of the TestChannelHandler
     */
    public HandlerAdapter getHandlerAdapter() {
        return handlerAdapter;
    }

    /**
     * returns the number of close() calls
     * @return the number of close() calls
     */
    public int getCloseCounter() {
        return closeCounter;
    }

    private class TestTransformer extends AbstractForwarder<ByteBuffer, Void> {

        @Override
        public void forward(ByteBuffer input) throws IOException {
            switch(inputHandling) {
                case FORWARD:
                    try {
                        ChannelWriter peerWriter = peer.getChannelWriter();
                        peerWriter.forward(input);
                        forwardDone = true;
                        lock.lock();
                        try {
                            forwarded.signal();
                        } finally {
                            lock.unlock();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case ECHO:
                default:
                    lock.lock();
                    try {
                        channelWriter.forward(input);
                        echoDone = true;
                        enqueued.signal();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
            }
            try {
                Thread.sleep(1000100);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
