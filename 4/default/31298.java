import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;

/**
 * @author levigo
 */
public class TemplateTest extends TestCase {

    public void testFillTemplate() throws Exception {
        String file = streamAsString(getClass().getResourceAsStream("/template.txt"));
        Map<String, String> client = new HashMap<String, String>();
        client.put("BootOptions.KernelName", "yada-kernel-name");
        client.put("BootOptions.InitrdName", "yada-initrd-name");
        client.put("BootOptions.NFSRootserver", "yada-rootserver");
        client.put("BootOptions.NFSRootPath", "yada-rootpath");
        System.out.println("before: " + file);
        Pattern p = Pattern.compile("\\$\\{([^\\}]+)\\}", Pattern.MULTILINE);
        StringBuffer result = new StringBuffer();
        Matcher m = p.matcher(file);
        while (m.find()) {
            String group = m.group(1);
            System.out.println("matches. group: " + group);
            String value = client.get(group);
            if (null == value) System.out.println("Pattern refers to undefined variable " + m.group(1));
            m.appendReplacement(result, null != value ? value : "");
        }
        m.appendTail(result);
        String processed = result.toString().replaceAll("\\r", "");
        processed = processed.replaceAll("\\\\[\\t ]*\\n", "");
        processed = processed.replaceAll("[\\t ]+", " ");
        System.out.println("result: " + processed);
    }

    public void testReplace() throws Exception {
        System.out.println("bla ${foo} bla".replaceAll("\\$", "\\\\\\$"));
    }

    private String streamAsString(InputStream is) throws IOException {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        byte b[] = new byte[1024];
        int read;
        while ((read = is.read(b)) >= 0) s.write(b, 0, read);
        is.close();
        return s.toString("ASCII");
    }
}
