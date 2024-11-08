import org.alfresco.ceoss.NativeTreeStore;
import org.alfresco.repo.content.filestore.ContentReader;
import org.alfresco.repo.content.filestore.ContentStore;
import org.alfresco.repo.content.filestore.ContentWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AlfrescoWebScriptTest2 {

    protected static ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "classpath:native-service-context.xml" });

    public static void main(String[] args) {
        short i = 10;
        System.out.println(Integer.MAX_VALUE);
        NativeTreeStore store = (NativeTreeStore) ctx.getBean("nativeTreeStore");
        ContentStore contentStore = (ContentStore) ctx.getBean("nativeContentStore");
        ContentWriter writer = contentStore.getContentWriter();
        writer.putContent("YAHOO");
        System.out.println(writer.getUri());
        System.out.println(writer.getUri().length());
        ContentReader reader = contentStore.getContentReader(writer.getUri());
        System.out.println(reader.getContentString());
    }
}
