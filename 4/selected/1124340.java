package jacky.lanlan.song.extension.struts.interceptor;

import static org.fest.reflect.core.Reflection.field;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import jacky.lanlan.song.extension.struts.InfrastructureKeys;
import jacky.lanlan.song.extension.struts.action.POJOTestAction;
import jacky.lanlan.song.extension.struts.annotation.Forward;
import jacky.lanlan.song.extension.struts.scope.FlashScopeContainer;
import jacky.lanlan.song.junitx.v4.atunit.AtUnit;
import jacky.lanlan.song.junitx.v4.atunit.Mock;
import jacky.lanlan.song.junitx.v4.atunit.InjectMock;
import jacky.lanlan.song.junitx.v4.atunit.Unit;
import jacky.lanlan.song.reflection.ReflectionUtils;
import jacky.lanlan.song.resource.ResourceUtils;
import jacky.lanlan.song.test.common.UnitUtils;
import jacky.lanlan.song.web.Scope;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.struts.Globals;
import org.apache.struts.upload.FormFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jacky.Song
 *
 * 测试拦截器。
 */
@RunWith(AtUnit.class)
@InjectMock
public class InterceptorTest {

    @Unit
    private ActionInterceptor intercepter;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    private FlashScopeSetter flash;

    private CancellInterceptor cancell;

    private CleanInterceptor clean;

    private JSONEncodeInterceptor remote;

    private DownloadInterceptor download;

    private UploadInterceptor upload;

    private POJOTestAction action;

    private UnitUtils utils;

    @Before
    public void init() {
        utils = new UnitUtils();
        action = new POJOTestAction();
        upload = new UploadInterceptor();
        download = new DownloadInterceptor();
        cancell = new CancellInterceptor();
        clean = new CleanInterceptor();
        flash = new FlashScopeSetter();
        remote = new JSONEncodeInterceptor();
    }

    @Test
    public void testUploadInterceptor() throws Exception {
        Method method = ReflectionUtils.findMethod(POJOTestAction.class, "upload", InfrastructureKeys.METHOD_PARAM);
        stub(mockRequest.getAttribute(InfrastructureKeys.EXECUTION)).toReturn(method);
        List<FormFile> list = new ArrayList<FormFile>();
        list.add(mock(FormFile.class));
        list.add(mock(FormFile.class));
        stub(mockRequest.getAttribute(InfrastructureKeys.UPLOAD_DATA)).toReturn(list);
        assertNull("应该成功上传文件，并触发下一个拦截器", upload.preHandle(mockRequest, mockResponse, action));
    }

    @Test
    public void testJSONEncoderInterceptorEncodeCollection() throws Exception {
        String realResp = this.runJSONEncoderInterceptorTest("getJavaAnimalsList");
        assertEquals("[\"Kestrel\",\"Merlin\",\"Tiger\",\"Mustang\",\"Dolphin\"]", realResp.toString());
    }

    @Test
    public void testJSONEncoderInterceptorEncodeArray() throws Exception {
        String realResp = this.runJSONEncoderInterceptorTest("getJavaAnimalsArray");
        assertEquals("[\"Dolphin\",\"Tiger\",\"Mustang\"]", realResp.toString());
    }

    @Test
    public void testJSONEncoderInterceptorEncodeMap() throws Exception {
        String realResp = this.runJSONEncoderInterceptorTest("getJavaVersionMap");
        assertEquals("{\"Java 6.0\":\"Mustang\",\"Java 7.0\":\"Dolphin\",\"Java 1.4.2\":\"Mantis\",\"Java 5.0\":\"Tiger\"}", realResp.toString());
    }

    @Test
    public void testJSONEncoderInterceptorEncodeObject() throws Exception {
        String realResp = this.runJSONEncoderInterceptorTest("getTestDomain");
        assertEquals("{\"addr\":\"Street 1\",\"carNo\":\"12345\"," + "\"emitTime\":{\"date\":1,\"day\":4,\"hours\":8,\"minutes\":0,\"month\":0,\"seconds\":2,\"time\":2008,\"timezoneOffset\":-480,\"year\":70}," + "\"installNum\":7,\"justGetter\":false}", realResp.toString());
    }

    private String runJSONEncoderInterceptorTest(String testMethodName) throws Exception {
        Method method = ReflectionUtils.findMethod(POJOTestAction.class, testMethodName, InfrastructureKeys.METHOD_PARAM);
        stub(mockRequest.getAttribute(InfrastructureKeys.EXECUTION)).toReturn(method);
        final StringBuilder realResp = new StringBuilder();
        stub(mockResponse.getOutputStream()).toReturn(new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                realResp.append((char) b);
            }
        });
        String direction = remote.preHandle(mockRequest, mockResponse, action);
        verify(mockResponse).getOutputStream();
        assertEquals(Forward.INPUT, direction);
        return realResp.toString();
    }

    @Test
    public void testDownloadInterceptor() throws Exception {
        File log4j = ResourceUtils.getFile("classpath:log4j.properties");
        FileChannel fc = new FileInputStream(log4j).getChannel();
        ByteBuffer bb = ByteBuffer.allocate(1024 * 10);
        fc.read(bb);
        bb.flip();
        byte[] realContent = new byte[bb.limit()];
        bb.get(realContent);
        Method method = ReflectionUtils.findMethod(POJOTestAction.class, "download", InfrastructureKeys.METHOD_PARAM);
        stub(mockRequest.getAttribute(InfrastructureKeys.EXECUTION)).toReturn(method);
        final byte[] actualContent = new byte[realContent.length];
        stub(mockResponse.getOutputStream()).toReturn(new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                actualContent[utils.getCounter().get()] = (byte) b;
                utils.increaseCounter();
            }
        });
        String direction = download.preHandle(mockRequest, mockResponse, action);
        verify(mockResponse).setContentType("application/properties");
        assertEquals(Forward.INPUT, direction);
        assertTrue("应该得到log4j.properties文件的字节流", Arrays.equals(realContent, actualContent));
    }

    @Test
    public void testCancellInterceptor() throws Exception {
        Method method = ReflectionUtils.findMethod(POJOTestAction.class, "businessMethod", InfrastructureKeys.METHOD_PARAM);
        stub(mockRequest.getAttribute(Globals.CANCEL_KEY)).toReturn("true");
        stub(mockRequest.getAttribute(InfrastructureKeys.EXECUTION)).toReturn(method);
        stub(mockRequest.getAttribute(InfrastructureKeys.REQ_DATA)).toReturn(Collections.EMPTY_MAP);
        String direction = cancell.preHandle(mockRequest, mockResponse, action);
        assertEquals(Forward.INPUT, direction);
    }

    @Test
    public void testCleanInterceptor() throws Exception {
        clean.afterCompletion(mockRequest, mockResponse, null, null);
        verify(mockRequest).removeAttribute(InfrastructureKeys.VSAC);
        verify(mockRequest).removeAttribute(InfrastructureKeys.EXECUTION);
        verify(mockRequest).removeAttribute(InfrastructureKeys.REQ_DATA);
    }

    @Test
    public void testFlashScopeSetter() throws Exception {
        HttpSession mockSession = mock(HttpSession.class);
        stub(mockRequest.getSession(false)).toReturn(mockSession);
        FlashScopeContainer flashValue = new FlashScopeContainer(mockRequest, "foo", 111);
        verify(mockSession).setAttribute("foo", 111);
        final String flashValueName = Scope.Flash + "_foo";
        stub(mockSession.getAttribute(flashValueName)).toReturn(flashValue);
        Enhancer en = new Enhancer();
        en.setSuperclass(Enumeration.class);
        en.setCallback(new MethodInterceptor() {

            Enumeration e = mock(Enumeration.class);

            boolean flag;

            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                String mn = method.getName();
                if (mn.equals("hasMoreElements")) {
                    flag = !flag;
                    return flag;
                } else if (mn.equals("nextElement")) {
                    return flashValueName;
                }
                return proxy.invoke(e, args);
            }
        });
        stub(mockSession.getAttributeNames()).toReturn((Enumeration) en.create());
        HttpServletRequest newReq = incommingRequest(mockSession, flashValue);
        flash.preHandle(newReq, mockResponse, null);
        verify(newReq).setAttribute("foo", 111);
        assertEquals("FlashScopeValue 计数器应该为1", (Integer) 1, field("count").ofType(int.class).in(flashValue).get());
        newReq = incommingRequest(mockSession, flashValue);
        flash.preHandle(newReq, mockResponse, null);
        verify(newReq).setAttribute("foo", 111);
        assertEquals("FlashScopeValue 计数器应该为0", (Integer) 0, field("count").ofType(int.class).in(flashValue).get());
        newReq = incommingRequest(mockSession, flashValue);
        flash.preHandle(newReq, mockResponse, null);
        verify(newReq, never()).setAttribute("foo", 111);
        verify(mockSession).removeAttribute(flashValueName);
    }

    private HttpServletRequest incommingRequest(HttpSession session, FlashScopeContainer flashValue) {
        HttpServletRequest newReq = mock(HttpServletRequest.class);
        stub(newReq.getSession(false)).toReturn(session);
        return newReq;
    }

    @After
    public void onTearDown() {
        utils.resetAll(this);
    }
}
