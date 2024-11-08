package jather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExecutiveTest {

    List<Executive> execs;

    List<ProcessId> requestList;

    Map<ProcessId, ProcessContext> resultMap;

    GroupMember member;

    RpcDispatcher dispatcher;

    Map<ProcessId, Callable<?>> callableMap;

    Callable<?> callable;

    CountDownLatch resultLatch;

    HiddenClassLoader hiddenClassLoader;

    static int counter;

    @Before
    public void init() throws Exception {
        hiddenClassLoader = new HiddenClassLoader();
        callable = new MockCallable();
        callableMap = new HashMap<ProcessId, Callable<?>>();
        member = new GroupMember();
        counter++;
        member.setClusterName("ExecutiveTest" + counter);
        requestList = new ArrayList<ProcessId>();
        resultMap = new HashMap<ProcessId, ProcessContext>();
        resultLatch = new CountDownLatch(Integer.MAX_VALUE);
        dispatcher = new RpcDispatcher(member.getConnectedChannel(), null, null, new JatherHandlerAdapter() {

            @Override
            public ProcessContext processRequest(RequestContext requestContext) {
                requestList.add(requestContext.getProcessId());
                synchronized (callableMap) {
                    try {
                        return new ProcessContext(requestContext.getProcessId(), callableMap.remove(requestContext.getProcessId()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void processResult(ProcessContext context) {
                resultMap.put(context.getProcessId(), context);
                resultLatch.countDown();
            }

            @Override
            public byte[] getResource(String name) {
                try {
                    return Util.toBytes(hiddenClassLoader.getResourceAsStream(name));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        execs = new ArrayList<Executive>();
        for (int i = 0; i < 5; i++) {
            Executive e = new Executive();
            e.setClusterName(member.getClusterName());
            execs.add(e);
        }
    }

    @After
    public void clean() {
        member.closeChannel();
        for (Executive e : execs) {
            e.close();
        }
    }

    @Test
    public void testAttributes() throws Exception {
        Executive exec = execs.iterator().next();
        assertEquals("Make sure the default slot count is set", 10, exec.getSlotCount());
        assertNotNull("Make sure the default queue is set", exec.getWorkQueue());
        assertNotNull("Make sure the thread pool is created", exec.getThreadPoolExecutor());
        assertEquals("Make sure the default cluster name is set", "ExecutiveTest" + counter, exec.getClusterName());
        assertNotNull("Make sure the channel factory is created", exec.getChannelFactory());
        URL url = new URL("http://myurl");
        exec.setChannelUrlProps(url);
        assertEquals("Make sure the property url is set", url, exec.getChannelUrlProps());
        String props = "String props";
        exec.setChannelStringProps(props);
        assertEquals("Make sure the property string is set", props, exec.getChannelStringProps());
        assertNotNull("Make sure the channel is created", exec.getChannel());
    }

    @Test
    public void testConnect() throws Exception {
        Executive exec = execs.iterator().next();
        assertTrue("Make sure the channel is connected", exec.getConnectedChannel().isConnected());
        exec.closeChannel();
        assertTrue("Make sure the channel is dis-connected", exec.getConnectedChannel().isConnected());
    }

    @Test
    public void testStart() throws Throwable {
        Executive exec = execs.iterator().next();
        exec.start();
        assertNotNull("Make sure the dispatcher is available", exec.getRpcDispatcher());
    }

    @Test(timeout = 60 * 1000)
    public void testSendRequests() throws Throwable {
        final List<ProcessDefinition> ids = new ArrayList<ProcessDefinition>();
        for (Executive e : execs) {
            e.setExecutiveHandler(new ExecutiveHandler(null) {

                @Override
                public void receiveRequest(ProcessDefinition processDefinition) {
                    ids.add(processDefinition);
                }
            });
            e.start();
        }
        ProcessId pid = new ProcessId(dispatcher.getChannel().getLocalAddress(), "id1");
        ProcessDefinition pd = new ProcessDefinition(pid);
        MethodCall method = new MethodCall(JatherHandlerAdapter.RECEIVE_REQUEST, new Object[] { pd });
        RspList list = dispatcher.callRemoteMethods(null, method, GroupRequest.GET_ALL, 0);
        assertEquals("Make sure all execs were called", execs.size(), ids.size());
        for (Rsp r : list.values()) {
            assertTrue("Make sure all calls were received", r.wasReceived());
        }
    }

    @Test(timeout = 60 * 1000)
    public void testHandleRequests() throws Throwable {
        for (Executive e : execs) {
            e.start();
        }
        resultLatch = new CountDownLatch(10);
        ProcessDefinition[] pds = new ProcessDefinition[(int) resultLatch.getCount()];
        for (int i = 0; i < pds.length; i++) {
            pds[i] = new ProcessDefinition(new ProcessId(dispatcher.getChannel().getLocalAddress(), "id" + i));
            callableMap.put(pds[i].getProcessId(), new MockPidCallable(pds[i].getProcessId()));
        }
        RspList[] lists = new RspList[pds.length];
        for (int i = 0; i < pds.length; i++) {
            MethodCall method = new MethodCall(JatherHandlerAdapter.RECEIVE_REQUEST, new Object[] { pds[i] });
            lists[i] = dispatcher.callRemoteMethods(null, method, GroupRequest.GET_ALL, 0);
        }
        for (int i = 0; i < lists.length; i++) {
            assertEquals("Make sure all execs were called", execs.size() + 1, lists[i].values().size());
            for (Rsp r : lists[i].values()) {
                assertTrue("Make sure all calls were received", r.wasReceived());
            }
        }
        resultLatch.await();
        for (ProcessDefinition pd : pds) {
            assertNotNull("Make sure the process result is collected:" + pd + ":" + resultMap, resultMap.get(pd.getProcessId()));
            assertEquals("Make sure the process result is present", pd.getProcessId(), resultMap.get(pd.getProcessId()).getResult());
        }
    }

    @Test(timeout = 60 * 1000)
    public void testHandleBadRequests() throws Throwable {
        for (Executive e : execs) {
            e.start();
        }
        resultLatch = new CountDownLatch(10);
        ProcessDefinition[] pds = new ProcessDefinition[(int) resultLatch.getCount()];
        for (int i = 0; i < pds.length; i++) {
            pds[i] = new ProcessDefinition(new ProcessId(dispatcher.getChannel().getLocalAddress(), "id" + i));
            callableMap.put(pds[i].getProcessId(), new MockBadCallable());
        }
        RspList[] lists = new RspList[pds.length];
        for (int i = 0; i < pds.length; i++) {
            MethodCall method = new MethodCall(JatherHandlerAdapter.RECEIVE_REQUEST, new Object[] { pds[i] });
            lists[i] = dispatcher.callRemoteMethods(null, method, GroupRequest.GET_ALL, 0);
        }
        for (int i = 0; i < lists.length; i++) {
            assertEquals("Make sure all execs were called", execs.size() + 1, lists[i].values().size());
            for (Rsp r : lists[i].values()) {
                assertTrue("Make sure all calls were received", r.wasReceived());
            }
        }
        resultLatch.await();
        for (ProcessDefinition pd : pds) {
            assertNotNull("Make sure the process result is collected:" + pd + ":" + resultMap, resultMap.get(pd.getProcessId()));
            assertNotNull("Make sure the process exception is present", resultMap.get(pd.getProcessId()).getCallableException());
        }
    }

    @Test(timeout = 10 * 1000)
    public void testHiddenClassLoader() throws Exception {
        Object obj = hiddenClassLoader.loadClass("hidden.HiddenString").newInstance();
        assertNotNull("Make sure the hidden string is loaded", obj);
        for (Executive e : execs) {
            e.start();
        }
        resultLatch = new CountDownLatch(5);
        ProcessDefinition[] pds = new ProcessDefinition[(int) resultLatch.getCount()];
        for (int i = 0; i < pds.length; i++) {
            pds[i] = new ProcessDefinition(new ProcessId(dispatcher.getChannel().getLocalAddress(), "id" + i));
            callableMap.put(pds[i].getProcessId(), new MockValueCallable(obj));
        }
        RspList[] lists = new RspList[pds.length];
        for (int i = 0; i < pds.length; i++) {
            MethodCall method = new MethodCall(JatherHandlerAdapter.RECEIVE_REQUEST, new Object[] { pds[i] });
            lists[i] = dispatcher.callRemoteMethods(null, method, GroupRequest.GET_ALL, 0);
        }
        for (int i = 0; i < lists.length; i++) {
            assertEquals("Make sure all execs were called", execs.size() + 1, lists[i].values().size());
            for (Rsp r : lists[i].values()) {
                assertTrue("Make sure all calls were received", r.wasReceived());
            }
        }
        resultLatch.await();
        for (ProcessDefinition pd : pds) {
            assertNotNull("Make sure the process result is collected:" + pd + ":" + resultMap, resultMap.get(pd.getProcessId()));
            assertNotNull("Make sure the process result is present", resultMap.get(pd.getProcessId()).getResult());
        }
    }
}
