package org.isi.monet.modelling.compiler.stages;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.isi.monet.modelling.compiler.IGlobalData;
import org.isi.monet.modelling.compiler.Module;
import org.isi.monet.modelling.compiler.Stage;
import org.isi.monet.modelling.compiler.errors.InternalError;
import org.w3c.dom.Document;

public class Packaging extends Stage {

    private ArrayList<Module> modules = new ArrayList<Module>();

    private String packagePath;

    @Override
    public Object getOutData() {
        return packagePath;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setInData(Object data) {
        this.modules.addAll((Collection<Module>) data);
    }

    @Override
    public void execute() {
        String projectName = (String) this.globalData.getData(IGlobalData.PROJECT_NAME);
        compress(projectName);
    }

    public void compress(String packageName) {
        FileOutputStream oDestination;
        ZipOutputStream oOutput;
        try {
            File oDestFile = new File(String.format(this.globalData.getData(IGlobalData.PROJECT_OUTPUT_PATH) + "%s.mnm", packageName));
            if (!oDestFile.exists()) oDestFile.createNewFile();
            oDestination = new FileOutputStream(oDestFile);
            oOutput = new ZipOutputStream(new BufferedOutputStream(oDestination));
            oOutput.setLevel(9);
            for (Module module : modules) {
                String sRelativePath = module.getResource().getProjectRelativePath().toString();
                ZipEntry oEntry = new ZipEntry(sRelativePath);
                oOutput.putNextEntry(oEntry);
                if (module.getDocument() == null) {
                    writeBinary(module.getResource().getLocation().toString(), oOutput);
                } else {
                    writeXml(module.getDocument(), oOutput);
                }
            }
            oOutput.close();
        } catch (Exception e) {
            this.problems.add(new InternalError(null, e.getMessage()));
        }
    }

    private static void writeBinary(String filename, OutputStream output) throws IOException {
        FileInputStream inputStream = new FileInputStream(filename);
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = inputStream.read(buffer)) > -1) output.write(buffer, 0, bytesRead);
        inputStream.close();
    }

    private static void writeXml(Document doc, OutputStream output) {
        try {
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(new DOMSource(doc), new StreamResult(output));
        } catch (TransformerConfigurationException e) {
        } catch (TransformerException e) {
        }
    }
}
