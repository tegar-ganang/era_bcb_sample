package strutter.helper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import strutter.config.tags.ConfigWSInterface;

public class WSActionHelper {

    private static Log log = LogFactory.getLog(WSActionHelper.class);

    static HashMap methods = new HashMap();

    public static String TYPE_XML = "text/xml";

    public static String TYPE_HTML = "text/html";

    public static String TYPE_GIF = "image/gif";

    public static String TYPE_JPEG = "image/jpeg";

    public static String TYPE_PNG = "image/png";

    public static String TYPE_PDF = "application/pdf";

    public static String TYPE_XLS = "application/vnd.ms-excel";

    public static String TYPE_DOC = "application/vnd.ms-word";

    public static String TYPE_OCTET = "application/octet-stream";

    public static void setContentType(String type) {
        ActionHelper.getResponse().setContentType(type);
    }

    public static void dispatchMethod(Object bean, String name) throws Exception {
        if (name == null) {
            if (bean instanceof ConfigWSInterface) ((ConfigWSInterface) bean).doGet(); else dispatchMethod(bean, "doGet");
            return;
        }
        String mname = bean.getClass().getName() + "#" + name;
        Method method = (Method) methods.get(mname);
        if (method == null) {
            try {
                method = bean.getClass().getMethod(name, null);
            } catch (Exception e) {
                log.error("missing action: [" + name + "]");
                throw new Exception("missing action: [" + name + "]");
            }
        }
        if (method != null) {
            methods.put(mname, method);
            try {
                method.invoke(bean, null);
            } catch (ClassCastException e) {
                throw e;
            } catch (IllegalAccessException e) {
                throw e;
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof Exception) {
                    throw ((Exception) t);
                } else {
                    throw new ServletException(t);
                }
            }
        }
    }

    public static void writeBody(String val) throws Exception {
        writeBody(val.getBytes("UTF8"));
    }

    public static void writeBody(byte[] val) throws Exception {
        ServletOutputStream writer = ActionHelper.getResponse().getOutputStream();
        writer.write(val);
        writer.close();
    }

    public static String readBody() throws Exception {
        BufferedReader reader = ActionHelper.getRequest().getReader();
        StringBuffer buf = new StringBuffer();
        String str;
        try {
            while ((str = reader.readLine()) != null) buf.append(str);
        } finally {
            reader.close();
        }
        return buf.toString();
    }

    public static byte[] readBodyBytes() throws Exception {
        BufferedInputStream is = new BufferedInputStream(ActionHelper.getRequest().getInputStream());
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(boas);
        int c;
        while ((c = is.read()) != -1) out.write(c);
        out.close();
        return boas.toByteArray();
    }
}
