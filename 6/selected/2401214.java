package com.sun.sgs.test.impl.service.channel;

import com.sun.sgs.service.ClientSessionStatusListener;
import com.sun.sgs.service.SimpleCompletionHandler;
import com.sun.sgs.test.util.SgsTestNode;
import com.sun.sgs.tools.test.FilteredNameRunner;
import com.sun.sgs.tools.test.IntegrationTest;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FilteredNameRunner.class)
public class TestChannelServiceImplRelocatingSessions extends AbstractChannelServiceTest {

    private static final String REX = "rex";

    private final String[] oneUser = new String[] { REX };

    /** Constructs a test instance. */
    public TestChannelServiceImplRelocatingSessions() throws Exception {
    }

    protected void setUp(boolean clean) throws Exception {
        super.setUp(clean);
    }

    @Test
    @IntegrationTest
    public void testChannelJoinAndRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        ClientGroup group = new ClientGroup(someUsers);
        SgsTestNode node1 = addNode();
        SgsTestNode node2 = addNode();
        try {
            joinUsers(channelName, someUsers);
            checkUsersJoined(channelName, someUsers);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            moveClient(group.getClient(MOE), serverNode, node1);
            moveClient(group.getClient(LARRY), serverNode, node2);
            checkUsersJoined(channelName, someUsers);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            group.disconnect(true);
            Thread.sleep(WAIT_TIME);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelJoinAndRelocateMultipleTimes() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        ClientGroup group = new ClientGroup(someUsers);
        Set<SgsTestNode> nodes = addNodes(3);
        try {
            joinUsers(channelName, someUsers);
            checkUsersJoined(channelName, someUsers);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            DummyClient relocatingClient = group.getClient(MOE);
            SgsTestNode oldNode = serverNode;
            for (SgsTestNode newNode : nodes) {
                moveClient(relocatingClient, oldNode, newNode);
                checkUsersJoined(channelName, someUsers);
                sendMessagesToChannel(channelName, 2);
                checkChannelMessagesReceived(group, channelName, 2);
                oldNode = newNode;
            }
            group.disconnect(true);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelJoinAndRelocateWithOldNodeFailure() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        ClientGroup group = new ClientGroup(someUsers);
        SgsTestNode node1 = addNode();
        SgsTestNode node2 = addNode();
        try {
            joinUsers(channelName, someUsers);
            checkUsersJoined(channelName, someUsers);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            DummyClient relocatingClient = group.getClient(MOE);
            moveClient(relocatingClient, serverNode, node1);
            moveIdentityAndWaitForRelocationNotification(relocatingClient, node1, node2);
            node1.shutdown(false);
            Thread.sleep(WAIT_TIME * 3);
            group.removeSessionsFromGroup(node1.getAppPort());
            checkUsersJoined(channelName, LARRY, CURLY);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            group.disconnect(true);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelJoinToOldNodeDuringRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        ClientGroup group = new ClientGroup(oldNode.getAppPort(), oneUser);
        try {
            DummyClient relocatingClient = group.getClient(REX);
            holdChannelServerMethodToNode(oldNode, "join");
            joinUsers(channelName, oneUser);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveIdentity(relocatingClient, oldNode, newNode);
            Thread.sleep(200);
            releaseChannelServerMethodHeld(oldNode);
            relocatingClient.relocate(0, true, true);
            checkUsersJoined(channelName, oneUser);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            group.disconnect(true);
            Thread.sleep(WAIT_TIME);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelJoinToOldNodeAfterRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        ClientGroup group = new ClientGroup(oldNode.getAppPort(), oneUser);
        try {
            DummyClient relocatingClient = group.getClient(REX);
            holdChannelServerMethodToNode(oldNode, "join");
            joinUsers(channelName, oneUser);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveClient(relocatingClient, oldNode, newNode);
            releaseChannelServerMethodHeld(oldNode);
            checkUsersJoined(channelName, oneUser);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            group.disconnect(true);
            Thread.sleep(WAIT_TIME);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelJoinToOldNodeAfterRelocateTwice() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode1 = addNode();
        SgsTestNode newNode2 = addNode();
        ClientGroup group = new ClientGroup(oldNode.getAppPort(), oneUser);
        try {
            DummyClient relocatingClient = group.getClient(REX);
            holdChannelServerMethodToNode(oldNode, "join");
            joinUsers(channelName, oneUser);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveClient(relocatingClient, oldNode, newNode1);
            moveClient(relocatingClient, newNode1, newNode2);
            releaseChannelServerMethodHeld(oldNode);
            checkUsersJoined(channelName, oneUser);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            group.disconnect(true);
            Thread.sleep(WAIT_TIME);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelJoinDuringRelocatePreparation() throws Exception {
        String channelName1 = "foo";
        String channelName2 = "bar";
        createChannel(channelName1);
        createChannel(channelName2);
        ClientGroup group = new ClientGroup(someUsers);
        SgsTestNode newNode = addNode();
        MySessionStatusListener mySessionStatusListener = new MySessionStatusListener();
        serverNode.getClientSessionService().addSessionStatusListener(mySessionStatusListener);
        try {
            joinUsers(channelName1, someUsers);
            checkUsersJoined(channelName1, someUsers);
            DummyClient relocatingClient = group.getClient(MOE);
            moveIdentity(relocatingClient, serverNode, newNode);
            SimpleCompletionHandler handler = mySessionStatusListener.waitForPrepare();
            joinUsers(channelName2, someUsers);
            Thread.sleep(200);
            handler.completed();
            relocatingClient.relocate(0, true, true);
            checkUsersJoined(channelName2, someUsers);
            sendMessagesToChannel(channelName2, 2);
            checkChannelMessagesReceived(group, channelName2, 2);
            sendMessagesToChannel(channelName1, 2);
            checkChannelMessagesReceived(group, channelName1, 2);
            group.disconnect(true);
            Thread.sleep(WAIT_TIME);
            checkUsersJoined(channelName1, noUsers);
            checkUsersJoined(channelName2, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelJoinDuringRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        ClientGroup group = new ClientGroup(someUsers);
        SgsTestNode newNode = addNode();
        try {
            DummyClient relocatingClient = group.getClient(MOE);
            moveIdentityAndWaitForRelocationNotification(relocatingClient, serverNode, newNode);
            joinUsers(channelName, someUsers);
            relocatingClient.relocate(0, true, true);
            checkUsersJoined(channelName, someUsers);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(group, channelName, 2);
            group.disconnect(true);
            Thread.sleep(WAIT_TIME);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelLeaveAfterRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        DummyClient client = new DummyClient(REX);
        client.connect(port).login();
        SgsTestNode node1 = addNode();
        try {
            joinUsers(channelName, oneUser);
            checkUsersJoined(channelName, oneUser);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(client, channelName, 2);
            moveClient(client, serverNode, node1);
            checkUsersJoined(channelName, oneUser);
            leaveUsers(channelName, oneUser);
            client.assertLeftChannel(channelName);
            checkUsersJoined(channelName, noUsers);
        } finally {
            client.disconnect();
        }
    }

    @Test
    @IntegrationTest
    public void testChannelLeaveToOldNodeDuringRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        ClientGroup group = new ClientGroup(oldNode.getAppPort(), oneUser);
        try {
            DummyClient relocatingClient = group.getClient(REX);
            holdChannelServerMethodToNode(oldNode, "leave");
            joinUsers(channelName, oneUser);
            relocatingClient.assertJoinedChannel(channelName);
            checkUsersJoined(channelName, oneUser);
            leaveUsers(channelName, oneUser);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveIdentity(relocatingClient, oldNode, newNode);
            Thread.sleep(200);
            releaseChannelServerMethodHeld(oldNode);
            relocatingClient.relocate(0, true, true);
            relocatingClient.assertLeftChannel(channelName);
            Thread.sleep(2000);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelLeaveToOldNodeAfterRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        ClientGroup group = new ClientGroup(oldNode.getAppPort(), oneUser);
        try {
            DummyClient relocatingClient = group.getClient(REX);
            holdChannelServerMethodToNode(oldNode, "leave");
            joinUsers(channelName, oneUser);
            relocatingClient.assertJoinedChannel(channelName);
            leaveUsers(channelName, oneUser);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveClient(relocatingClient, oldNode, newNode);
            relocatingClient.assertJoinedChannel(channelName);
            releaseChannelServerMethodHeld(oldNode);
            relocatingClient.assertLeftChannel(channelName);
            checkUsersJoined(channelName, noUsers);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelLeaveAllAfterRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        ClientGroup group = new ClientGroup(oldNode.getAppPort(), oneUser);
        try {
            DummyClient relocatingClient = group.getClient(REX);
            joinUsers(channelName, oneUser);
            relocatingClient.assertJoinedChannel(channelName);
            holdChannelServerMethodToNode(oldNode, "close");
            leaveAll(channelName);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveClient(relocatingClient, oldNode, newNode);
            Thread.sleep(200);
            releaseChannelServerMethodHeld(oldNode);
            checkUsersJoined(channelName, noUsers);
            relocatingClient.assertLeftChannel(channelName);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelSendToOldNodeDuringRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        ClientGroup group = new ClientGroup(oldNode.getAppPort(), oneUser);
        try {
            DummyClient relocatingClient = group.getClient(REX);
            joinUsers(channelName, oneUser);
            relocatingClient.assertJoinedChannel(channelName);
            holdChannelServerMethodToNode(oldNode, "send");
            sendMessagesToChannel(channelName, 3);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveIdentityAndWaitForRelocationNotification(relocatingClient, oldNode, newNode);
            releaseChannelServerMethodHeld(oldNode);
            relocatingClient.relocate(0, true, true);
            checkUsersJoined(channelName, oneUser);
            checkChannelMessagesReceived(group, channelName, 3);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelSendToOldNodeAfterRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        ClientGroup group = new ClientGroup(oldNode.getAppPort(), oneUser);
        try {
            DummyClient relocatingClient = group.getClient(REX);
            joinUsers(channelName, oneUser);
            relocatingClient.assertJoinedChannel(channelName);
            holdChannelServerMethodToNode(oldNode, "send");
            sendMessagesToChannel(channelName, 3);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveClient(relocatingClient, oldNode, newNode);
            releaseChannelServerMethodHeld(oldNode);
            checkUsersJoined(channelName, oneUser);
            checkChannelMessagesReceived(group, channelName, 3);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelSendToOldNodeDuringRelocateWithMemberOnNewNode() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        String[] users = new String[] { "relocatingClient", "otherClient" };
        DummyClient relocatingClient = createDummyClient(users[0], oldNode);
        DummyClient otherClient = createDummyClient(users[1], newNode);
        try {
            joinUsers(channelName, users);
            relocatingClient.assertJoinedChannel(channelName);
            otherClient.assertJoinedChannel(channelName);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(relocatingClient, channelName, 2);
            checkChannelMessagesReceived(otherClient, channelName, 2);
            holdChannelServerMethodToNode(oldNode, "send");
            sendMessagesToChannel(channelName, 3);
            waitForHeldChannelServerMethodToNode(oldNode);
            moveIdentityAndWaitForRelocationNotification(relocatingClient, oldNode, newNode);
            releaseChannelServerMethodHeld(oldNode);
            relocatingClient.relocate(0, true, true);
            checkChannelMessagesReceived(relocatingClient, channelName, 3);
            checkChannelMessagesReceived(otherClient, channelName, 3);
        } finally {
            relocatingClient.disconnect();
            otherClient.disconnect();
        }
    }

    @Test
    @IntegrationTest
    public void testChannelSendDuringRelocatePreparation() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        ClientGroup group = new ClientGroup(someUsers);
        SgsTestNode newNode = addNode();
        MySessionStatusListener mySessionStatusListener = new MySessionStatusListener();
        serverNode.getClientSessionService().addSessionStatusListener(mySessionStatusListener);
        try {
            joinUsers(channelName, someUsers);
            checkUsersJoined(channelName, someUsers);
            DummyClient relocatingClient = group.getClient(MOE);
            moveIdentity(relocatingClient, serverNode, newNode);
            SimpleCompletionHandler handler = mySessionStatusListener.waitForPrepare();
            sendMessagesToChannel(channelName, 20);
            handler.completed();
            relocatingClient.relocate(0, true, true);
            checkUsersJoined(channelName, someUsers);
            checkChannelMessagesReceived(group, channelName, 20);
        } finally {
            group.disconnect(false);
        }
    }

    @Test
    @IntegrationTest
    public void testChannelSendDuringRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        String[] users = new String[] { "relocatingClient" };
        DummyClient relocatingClient = createDummyClient(users[0], oldNode);
        try {
            joinUsers(channelName, users);
            relocatingClient.assertJoinedChannel(channelName);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(relocatingClient, channelName, 2);
            moveIdentityAndWaitForRelocationNotification(relocatingClient, oldNode, newNode);
            sendMessagesToChannel(channelName, 3);
            relocatingClient.relocate(0, true, true);
            checkChannelMessagesReceived(relocatingClient, channelName, 3);
        } finally {
            relocatingClient.disconnect();
        }
    }

    @Test
    @IntegrationTest
    public void testChannelSendDuringRelocateWithMemberOnNewNode() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        String[] users = new String[] { "relocatingClient", "otherClient" };
        DummyClient relocatingClient = createDummyClient(users[0], oldNode);
        DummyClient otherClient = createDummyClient(users[1], newNode);
        try {
            joinUsers(channelName, users);
            relocatingClient.assertJoinedChannel(channelName);
            otherClient.assertJoinedChannel(channelName);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(relocatingClient, channelName, 2);
            checkChannelMessagesReceived(otherClient, channelName, 2);
            moveIdentityAndWaitForRelocationNotification(relocatingClient, oldNode, newNode);
            sendMessagesToChannel(channelName, 3);
            checkChannelMessagesReceived(otherClient, channelName, 3);
            relocatingClient.relocate(0, true, true);
            checkChannelMessagesReceived(relocatingClient, channelName, 3);
        } finally {
            relocatingClient.disconnect();
            otherClient.disconnect();
        }
    }

    @Test
    @IntegrationTest
    public void testSessionCleanupIfSessionFailsToPrepare() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        MySessionStatusListener oldNodeListener = new MySessionStatusListener(true);
        oldNode.getClientSessionService().addSessionStatusListener(oldNodeListener);
        MySessionStatusListener newNodeListener = new MySessionStatusListener(false);
        newNode.getClientSessionService().addSessionStatusListener(newNodeListener);
        String[] users = new String[] { "relocatingClient" };
        DummyClient relocatingClient = createDummyClient(users[0], oldNode);
        try {
            joinUsers(channelName, users);
            relocatingClient.assertJoinedChannel(channelName);
            assertTrue(newNodeListener.disconnectedSessions.isEmpty());
            assertTrue(oldNodeListener.disconnectedSessions.isEmpty());
            moveIdentity(relocatingClient, oldNode, newNode);
            Thread.sleep(5000);
            checkUsersJoined(channelName, noUsers);
            assertNull(relocatingClient.getSession());
            assertFalse(newNodeListener.disconnectedSessions.isEmpty());
            assertFalse(oldNodeListener.disconnectedSessions.isEmpty());
        } finally {
            relocatingClient.disconnect();
        }
    }

    @Test
    @IntegrationTest
    public void testSessionCleanupIfSessionFailsToRelocate() throws Exception {
        String channelName = "foo";
        createChannel(channelName);
        SgsTestNode oldNode = addNode();
        SgsTestNode newNode = addNode();
        MySessionStatusListener newNodeListener = new MySessionStatusListener(false);
        newNode.getClientSessionService().addSessionStatusListener(newNodeListener);
        String[] users = new String[] { "relocatingClient" };
        DummyClient relocatingClient = createDummyClient(users[0], oldNode);
        try {
            joinUsers(channelName, users);
            relocatingClient.assertJoinedChannel(channelName);
            sendMessagesToChannel(channelName, 2);
            checkChannelMessagesReceived(relocatingClient, channelName, 2);
            assertTrue(newNodeListener.disconnectedSessions.isEmpty());
            moveIdentityAndWaitForRelocationNotification(relocatingClient, oldNode, newNode);
            Thread.sleep(5000);
            checkUsersJoined(channelName, noUsers);
            assertNull(relocatingClient.getSession());
            assertFalse(newNodeListener.disconnectedSessions.isEmpty());
        } finally {
            relocatingClient.disconnect();
        }
    }

    /**
     * Reassigns the client's identity from {@code oldNode} to {@code newNode}.
     */
    private void moveIdentity(DummyClient client, SgsTestNode oldNode, SgsTestNode newNode) throws Exception {
        identityAssigner.moveIdentity(client.name, oldNode.getNodeId(), newNode.getNodeId());
    }

    /**
     * Reassigns the client's identity from {@code oldNode} to {@code newNode},
     * and waits for the client to receive the relocation notification.  The
     * client does not relocated unless instructed to do so via a {@code
     * relocate} invocation.
     */
    private void moveIdentityAndWaitForRelocationNotification(DummyClient client, SgsTestNode oldNode, SgsTestNode newNode) throws Exception {
        System.err.println("reassigning identity:" + client.name + " from node: " + oldNode.getNodeId() + " to node: " + newNode.getNodeId());
        moveIdentity(client, oldNode, newNode);
        client.waitForRelocationNotification(0);
    }

    /**
     * Moves the client from the server node to a new node.
     */
    private void moveClient(DummyClient client, SgsTestNode oldNode, SgsTestNode newNode) throws Exception {
        moveIdentityAndWaitForRelocationNotification(client, oldNode, newNode);
        client.relocate(0, true, true);
    }

    private class MySessionStatusListener implements ClientSessionStatusListener {

        private SimpleCompletionHandler handler = null;

        private final boolean controlledPreparation;

        private Set<BigInteger> disconnectedSessions = Collections.synchronizedSet(new HashSet<BigInteger>());

        private Set<BigInteger> relocatedSessions = Collections.synchronizedSet(new HashSet<BigInteger>());

        MySessionStatusListener() {
            this(true);
        }

        MySessionStatusListener(boolean controlledPreparation) {
            this.controlledPreparation = controlledPreparation;
        }

        public void disconnected(BigInteger sessionRefId, boolean isRelocating) {
            if (!isRelocating) {
                disconnectedSessions.add(sessionRefId);
            }
        }

        public void prepareToRelocate(BigInteger sessionRefId, long newNodeId, SimpleCompletionHandler handler) {
            synchronized (this) {
                this.handler = handler;
                notifyAll();
            }
            if (!controlledPreparation) {
                setPrepared();
            }
        }

        public void relocated(BigInteger sessionRefId) {
            relocatedSessions.add(sessionRefId);
        }

        SimpleCompletionHandler waitForPrepare() {
            synchronized (this) {
                if (handler == null) {
                    try {
                        wait(WAIT_TIME);
                    } catch (InterruptedException e) {
                    }
                }
            }
            return handler;
        }

        void setPrepared() {
            handler.completed();
        }
    }
}
