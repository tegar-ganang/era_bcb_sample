package br.unb.unbiquitous.ubiquitos.uos.driverManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManagerControlCenter;
import br.unb.unbiquitous.ubiquitos.network.loopback.LoopbackDevice;
import br.unb.unbiquitous.ubiquitos.network.loopback.connection.LoopbackClientConnection;
import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;
import br.unb.unbiquitous.ubiquitos.network.model.connection.ClientConnection;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.connectivity.proxying.ProxyDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;

public class ReflectionServiceCallerTest {

    private ReflectionServiceCaller caller;

    @Before
    public void setUp() {
        caller = new ReflectionServiceCaller(null);
    }

    @Test(expected = DriverManagerException.class)
    public void shouldFailOnAnNullDriver() throws Exception {
        caller.callServiceOnDriver(null, null, null);
    }

    @Test(expected = DriverManagerException.class)
    public void shouldFailOnAnNonExistantServiceMethod() throws Exception {
        caller.callServiceOnDriver(new ServiceCall(null, "nonExistantService"), new DriverSpy(), null);
    }

    @Test(expected = DriverManagerException.class)
    public void shouldFailOnAnNonPublicServiceMethod() throws Exception {
        caller.callServiceOnDriver(new ServiceCall(null, "privateService"), new DriverSpy(), null);
    }

    @Test(expected = DriverManagerException.class)
    public void shouldFailForNoServiceInformed() throws Exception {
        caller.callServiceOnDriver(new ServiceCall(), new DriverSpy(), null);
    }

    @Test(expected = DriverManagerException.class)
    public void shouldFailOnAMethodWithoutACompliantInterface() throws Exception {
        caller.callServiceOnDriver(new ServiceCall(null, "wrongService"), new DriverSpy(), null);
    }

    @Test
    public void shouldCallASimpleCompliantServiceWhithTheRightParameters() throws Exception {
        ServiceCall call = new ServiceCall(null, "myService");
        UOSMessageContext msgCtx = new UOSMessageContext();
        DriverSpy driver = new DriverSpy();
        caller.callServiceOnDriver(call, driver, msgCtx);
        assertEquals(call, driver.capturedCall);
        assertEquals(msgCtx, driver.capturedContext);
        assertNotNull(driver.capturedResponse);
        assertNull(driver.capturedResponse.getResponseData());
    }

    @Test
    public void shouldCallASimpleCompliantServiceIgnoringCase() throws Exception {
        ServiceCall call = new ServiceCall(null, "MySeRvIcE");
        UOSMessageContext msgCtx = new UOSMessageContext();
        DriverSpy driver = new DriverSpy();
        caller.callServiceOnDriver(call, driver, msgCtx);
        assertEquals(call, driver.capturedCall);
        assertEquals(msgCtx, driver.capturedContext);
        assertNotNull(driver.capturedResponse);
        assertNull(driver.capturedResponse.getResponseData());
    }

    @Test(expected = DriverManagerException.class)
    public void shouldFailWhenServiceFails() throws Exception {
        caller.callServiceOnDriver(new ServiceCall(null, "failService"), new DriverSpy(), new UOSMessageContext());
    }

    @Test
    public void shouldForwardServiceOnAProxyDriverMaintainingTheParameters() throws Exception {
        ServiceCall call = new ServiceCall(null, "myService");
        UOSMessageContext msgCtx = new UOSMessageContext();
        ProxyDriverSpy driver = new ProxyDriverSpy();
        caller.callServiceOnDriver(call, driver, msgCtx);
        assertTrue(driver.forwardCalled);
        assertEquals(call, driver.capturedCall);
        assertEquals(call, driver.capturedCall);
        assertEquals(msgCtx, driver.capturedContext);
        assertNotNull(driver.capturedResponse);
        assertNull(driver.capturedResponse.getResponseData());
    }

    @Test
    public void shouldCreateTheAppropriateChannelsForAStreamService() throws Exception {
        final LoopbackDevice device = new LoopbackDevice(182);
        UOSMessageContext msgCtx = new UOSMessageContext() {

            public NetworkDevice getCallerDevice() {
                return device;
            }
        };
        ConnectionManagerControlCenter mockNet = mock(ConnectionManagerControlCenter.class);
        ClientConnection cc = mock(ClientConnection.class);
        when(cc.getDataInputStream()).thenReturn(new DataInputStream(null));
        when(cc.getDataOutputStream()).thenReturn(new DataOutputStream(null));
        when(mockNet.openActiveConnection(anyString(), anyString())).thenReturn(cc);
        when(mockNet.getHost(anyString())).thenReturn("myname");
        caller = new ReflectionServiceCaller(mockNet);
        ServiceCall call = new ServiceCall(null, "myService");
        call.setServiceType(ServiceType.STREAM);
        call.setChannelType(device.getNetworkDeviceType());
        call.setChannels(4);
        call.setChannelIDs(new String[] { "P1", "P2", "P3", "P4" });
        caller.callServiceOnDriver(call, new DriverSpy(), msgCtx);
        verify(mockNet).openActiveConnection("myname:P1", call.getChannelType());
        verify(mockNet).openActiveConnection("myname:P2", call.getChannelType());
        verify(mockNet).openActiveConnection("myname:P3", call.getChannelType());
        verify(mockNet).openActiveConnection("myname:P4", call.getChannelType());
        for (int i = 0; i < 4; i++) {
            assertNotNull("InputStream: " + i, msgCtx.getDataInputStream(i));
            assertNotNull("OutputStream: " + i, msgCtx.getDataOutputStream(i));
        }
        assertNull(msgCtx.getDataInputStream(4));
        assertNull(msgCtx.getDataOutputStream(4));
    }

    public static class DriverSpy {

        ServiceCall capturedCall;

        ServiceResponse capturedResponse;

        UOSMessageContext capturedContext;

        public void myService(ServiceCall sc, ServiceResponse r, UOSMessageContext ctx) {
            capturedCall = sc;
            capturedResponse = r;
            capturedContext = ctx;
        }

        public void failService(ServiceCall sc, ServiceResponse r, UOSMessageContext ctx) {
            throw new RuntimeException("Failed on purpose");
        }

        public void wrongService() {
        }

        @SuppressWarnings("unused")
        private void privateService() {
        }
    }

    public static class ProxyDriverSpy extends DriverSpy implements ProxyDriver {

        boolean forwardCalled = false;

        public UpDriver getDriver() {
            return null;
        }

        public void init(Gateway gateway, String instanceId) {
        }

        public void destroy() {
        }

        public void forwardServiceCall(ServiceCall serviceCall, ServiceResponse serviceResponse, UOSMessageContext messageContext) {
            forwardCalled = true;
            capturedCall = serviceCall;
            capturedResponse = serviceResponse;
            capturedContext = messageContext;
        }

        public UpDevice getProvider() {
            return null;
        }

        @Override
        public List<UpDriver> getParent() {
            return null;
        }
    }
}
