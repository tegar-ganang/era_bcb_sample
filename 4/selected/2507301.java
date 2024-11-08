package org.swemas.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import org.swemas.rendering.RenderingException;
import org.swemas.rendering.composing.xhtml.SwXhtmlComposer;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.kernel.KernelException;
import org.swemas.core.kernel.SwKernel;
import org.swemas.data.xml.IXmlChannel;
import org.swemas.data.xml.XmlException;
import org.w3c.dom.Document;

/**
 * @author Alexey Chernov
 * 
 */
public class Rendering {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        T2 t2 = new T2();
        IT1 it1 = t2;
        boolean b = false;
        b = it1 instanceof IT2;
        int i = 0;
        ++i;
    }

    private static void test_render() {
        try {
            SwKernel kernel = new SwKernel("/srv/www/htdocs/swemas", "org.swemas.data.xml.SwXml");
            SwXhtmlComposer cmp = new SwXhtmlComposer(kernel);
            Document doc = (Document) cmp.render(null, null).get(0);
            IXmlChannel ixml = (IXmlChannel) kernel.getChannel(IXmlChannel.class);
            File f = new File("/srv/www/htdocs/swemas/test_out.xml");
            FileOutputStream fs = new FileOutputStream(f);
            ixml.save(doc, fs);
        } catch (KernelException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ModuleNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlException e) {
            e.printStackTrace();
        } catch (RenderingException e) {
            e.printStackTrace();
        }
    }
}
