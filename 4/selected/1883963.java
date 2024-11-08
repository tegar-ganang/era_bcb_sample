package gui.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import org.dom4j.Element;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import data.DataSourceSingleton;

/**
 * @author Administrator
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SaveAsAction extends Action {

    ApplicationWindow window;

    private FileDialog saveAsDialog;

    String[] saveAsTypes = { "*.xml", "*.txt" };

    public SaveAsAction(ApplicationWindow w) {
        window = w;
        setText("&SaveAs...@Ctrl+S");
        setToolTipText("Save as a type of file");
        try {
            setImageDescriptor(ImageDescriptor.createFromURL(new URL("file:data/icons/save_as.gif")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        if (saveAsDialog == null) {
            saveAsDialog = new FileDialog(window.getShell(), SWT.SAVE);
            saveAsDialog.setFilterExtensions(saveAsTypes);
        }
        String outputFile = saveAsDialog.open();
        if (outputFile != null) {
            Object inputFile = DataSourceSingleton.getInstance().getContainer().getWrapped();
            InputStream in;
            try {
                if (inputFile instanceof URL) in = ((URL) inputFile).openStream(); else in = new FileInputStream((File) inputFile);
                OutputStream out = new FileOutputStream(outputFile);
                if (outputFile.endsWith("xml")) {
                    int c;
                    while ((c = in.read()) != -1) out.write(c);
                } else {
                    PrintWriter pw = new PrintWriter(out);
                    Element data = DataSourceSingleton.getInstance().getRawData();
                    writeTextFile(data, pw, -1);
                    pw.close();
                }
                in.close();
                out.close();
            } catch (MalformedURLException e1) {
            } catch (IOException e) {
            }
        }
    }

    private void writeTextFile(Element data, PrintWriter pw, int n) {
        n++;
        Iterator ite = data.elementIterator();
        for (int i = 0; i < n; i++) pw.print("    ");
        pw.println(data.getName() + "  " + data.getTextTrim());
        while (ite.hasNext()) {
            Element el = (Element) ite.next();
            writeTextFile(el, pw, n);
        }
    }
}
