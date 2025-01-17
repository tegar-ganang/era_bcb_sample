package test.openmobster.core.agent.provisioning;

import junit.framework.TestCase;
import org.openmobster.cloud.api.ExecutionContext;
import org.openmobster.cloud.api.rpc.MobileServiceBean;
import org.openmobster.cloud.api.rpc.Request;
import org.openmobster.cloud.api.rpc.Response;
import org.openmobster.cloud.api.rpc.ServiceInfo;
import org.openmobster.core.agent.provisioning.IPhonePushCallback;
import org.openmobster.core.common.ServiceManager;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.openmobster.core.common.errors.ErrorHandler;
import org.openmobster.core.common.errors.SystemException;
import org.openmobster.core.security.device.Device;
import org.openmobster.core.security.device.DeviceController;
import org.openmobster.core.security.device.DeviceAttribute;
import org.openmobster.core.security.device.PushApp;
import org.openmobster.core.security.device.PushAppController;
import org.openmobster.core.security.identity.Identity;
import org.openmobster.core.security.identity.IdentityController;

/**
 * 
 * @author openmobster@gmail.com
 */
public class TestIPhonePushCallback extends TestCase {

    private IPhonePushCallback service;

    public void setUp() throws Exception {
        ServiceManager.bootstrap();
        this.service = (IPhonePushCallback) ServiceManager.locate("iphone_push_callback");
        IdentityController identityController = (IdentityController) ServiceManager.locate("security://IdentityController");
        if (identityController.read("blah@gmail.com") == null) {
            identityController.create(new Identity("blah@gmail.com", ""));
        }
        DeviceController deviceController = DeviceController.getInstance();
        String deviceId = "IMEI:8675309";
        if (deviceController.read(deviceId) == null) {
            Device device = new Device(deviceId, identityController.read("blah@gmail.com"));
            device.addAttribute(new DeviceAttribute("nonce", "blahblah"));
            deviceController.create(device);
        }
    }

    public void tearDown() throws Exception {
        ServiceManager.shutdown();
    }

    /**
	 * Device submits all the information, and a PushApp is created from scratch
	 * 
	 * @throws Exception
	 */
    public void testSimpleCaseWithChannels() throws Exception {
        List<String> channels = new ArrayList<String>();
        channels.add("push-1");
        channels.add("push-2");
        Device device = DeviceController.getInstance().read("IMEI:8675309");
        ExecutionContext.getInstance().setDevice(device);
        Request request = new Request("iphone_push_callback");
        request.setAttribute("os", "iphone");
        request.setAttribute("deviceToken", "device-token");
        request.setAttribute("appId", "blah");
        request.setListAttribute("channels", channels);
        this.service.invoke(request);
        device = DeviceController.getInstance().read("IMEI:8675309");
        String deviceToken = device.getDeviceToken();
        assertNotNull(deviceToken);
        assertEquals(deviceToken, "device-token");
        PushApp pushApp = PushAppController.getInstance().readPushApp("blah");
        assertNotNull(pushApp);
        Set<String> storedChannels = pushApp.getChannels();
        assertTrue(storedChannels != null && !storedChannels.isEmpty() && storedChannels.size() == 2);
        Set<String> devices = pushApp.getDevices();
        assertTrue(devices != null && !devices.isEmpty());
        String deviceId = devices.iterator().next();
        assertEquals(deviceId, device.getIdentifier());
    }

    /**
	 * Device submits all the information, and a PushApp is created from scratch,
	 * no channels submitted
	 * 
	 * @throws Exception
	 */
    public void testSimpleCaseNoChannels() throws Exception {
        List<String> channels = new ArrayList<String>();
        Device device = DeviceController.getInstance().read("IMEI:8675309");
        ExecutionContext.getInstance().setDevice(device);
        Request request = new Request("iphone_push_callback");
        request.setAttribute("os", "iphone");
        request.setAttribute("deviceToken", "device-token");
        request.setAttribute("appId", "blah");
        request.setListAttribute("channels", channels);
        this.service.invoke(request);
        device = DeviceController.getInstance().read("IMEI:8675309");
        String deviceToken = device.getDeviceToken();
        assertNotNull(deviceToken);
        assertEquals(deviceToken, "device-token");
        PushApp pushApp = PushAppController.getInstance().readPushApp("blah");
        assertNotNull(pushApp);
        Set<String> storedChannels = pushApp.getChannels();
        assertTrue(storedChannels == null || storedChannels.isEmpty());
        Set<String> devices = pushApp.getDevices();
        assertTrue(devices != null && !devices.isEmpty());
        String deviceId = devices.iterator().next();
        assertEquals(deviceId, device.getIdentifier());
    }

    /**
	 * Device submits all the information, and a PushApp exists,
	 * Channels same
	 * 
	 * @throws Exception
	 */
    public void testComplexCaseWithChannels() throws Exception {
        List<String> channels = new ArrayList<String>();
        channels.add("push-1");
        channels.add("push-2");
        Set<String> stored = new HashSet<String>();
        stored.add("push-1");
        stored.add("push-2");
        PushApp pushApp = new PushApp();
        pushApp.setAppId("blah");
        pushApp.setChannels(stored);
        PushAppController.getInstance().create(pushApp);
        Device device = DeviceController.getInstance().read("IMEI:8675309");
        ExecutionContext.getInstance().setDevice(device);
        Request request = new Request("iphone_push_callback");
        request.setAttribute("os", "iphone");
        request.setAttribute("deviceToken", "device-token");
        request.setAttribute("appId", "blah");
        request.setListAttribute("channels", channels);
        this.service.invoke(request);
        device = DeviceController.getInstance().read("IMEI:8675309");
        String deviceToken = device.getDeviceToken();
        assertNotNull(deviceToken);
        assertEquals(deviceToken, "device-token");
        pushApp = PushAppController.getInstance().readPushApp("blah");
        assertNotNull(pushApp);
        Set<String> storedChannels = pushApp.getChannels();
        assertTrue(storedChannels != null && !storedChannels.isEmpty() && storedChannels.size() == 2);
        Set<String> devices = pushApp.getDevices();
        assertTrue(devices != null && !devices.isEmpty());
        String deviceId = devices.iterator().next();
        assertEquals(deviceId, device.getIdentifier());
    }

    /**
	 * Device submits all the information, and a PushApp exists,
	 * No channels submitted
	 * 
	 * @throws Exception
	 */
    public void testComplexCaseNoChannels() throws Exception {
        List<String> channels = new ArrayList<String>();
        Set<String> stored = new HashSet<String>();
        stored.add("push-1");
        stored.add("push-2");
        PushApp pushApp = new PushApp();
        pushApp.setAppId("blah");
        pushApp.setChannels(stored);
        PushAppController.getInstance().create(pushApp);
        Device device = DeviceController.getInstance().read("IMEI:8675309");
        ExecutionContext.getInstance().setDevice(device);
        Request request = new Request("iphone_push_callback");
        request.setAttribute("os", "iphone");
        request.setAttribute("deviceToken", "device-token");
        request.setAttribute("appId", "blah");
        request.setListAttribute("channels", channels);
        this.service.invoke(request);
        device = DeviceController.getInstance().read("IMEI:8675309");
        String deviceToken = device.getDeviceToken();
        assertNotNull(deviceToken);
        assertEquals(deviceToken, "device-token");
        pushApp = PushAppController.getInstance().readPushApp("blah");
        assertNotNull(pushApp);
        Set<String> storedChannels = pushApp.getChannels();
        assertTrue(storedChannels == null || storedChannels.isEmpty());
        Set<String> devices = pushApp.getDevices();
        assertTrue(devices != null && !devices.isEmpty());
        String deviceId = devices.iterator().next();
        assertEquals(deviceId, device.getIdentifier());
    }

    /**
	 * Device submits all the information, and a PushApp exists,
	 * more channels are added
	 * 
	 * @throws Exception
	 */
    public void testComplexCaseMoreChannels() throws Exception {
        List<String> channels = new ArrayList<String>();
        channels.add("push-1");
        channels.add("push-2");
        channels.add("push-3");
        channels.add("push-4");
        Set<String> stored = new HashSet<String>();
        stored.add("push-1");
        stored.add("push-2");
        PushApp pushApp = new PushApp();
        pushApp.setAppId("blah");
        pushApp.setChannels(stored);
        PushAppController.getInstance().create(pushApp);
        Device device = DeviceController.getInstance().read("IMEI:8675309");
        ExecutionContext.getInstance().setDevice(device);
        Request request = new Request("iphone_push_callback");
        request.setAttribute("os", "iphone");
        request.setAttribute("deviceToken", "device-token");
        request.setAttribute("appId", "blah");
        request.setListAttribute("channels", channels);
        this.service.invoke(request);
        device = DeviceController.getInstance().read("IMEI:8675309");
        String deviceToken = device.getDeviceToken();
        assertNotNull(deviceToken);
        assertEquals(deviceToken, "device-token");
        pushApp = PushAppController.getInstance().readPushApp("blah");
        assertNotNull(pushApp);
        Set<String> storedChannels = pushApp.getChannels();
        assertTrue(storedChannels != null && !storedChannels.isEmpty() && storedChannels.size() == 4);
        Set<String> devices = pushApp.getDevices();
        assertTrue(devices != null && !devices.isEmpty());
        String deviceId = devices.iterator().next();
        assertEquals(deviceId, device.getIdentifier());
    }
}
