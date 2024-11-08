package org.monet.modelling.ide.builders.stages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.monet.modelling.ide.agents.AgentFilesystem;
import org.monet.modelling.ide.builders.BuilderResource;
import org.monet.modelling.ide.builders.IGlobalData;
import org.monet.modelling.ide.builders.Module;
import org.monet.modelling.ide.builders.Stage;
import org.monet.modelling.ide.builders.StageState;
import org.w3c.dom.Document;

public class ModelDump extends Stage {

    private ArrayList<Module> modules = new ArrayList<Module>();

    private BuilderResource builderResource;

    @Override
    public Object getOutData() {
        return this.builderResource;
    }

    @Override
    public void setInData(Object data) {
        if (data != null) {
            this.builderResource = (BuilderResource) data;
            this.modules = (ArrayList<Module>) this.builderResource.getAllModules();
        }
    }

    @Override
    public void execute() {
        this.state = StageState.COMPLETE;
        boolean executingStage = (Boolean) this.globalData.getData(IGlobalData.DEFINITIONS_CHANDED);
        if (!executingStage) return;
        System.out.println("ModelDump");
        try {
            int i = 0;
            for (Module module : this.modules) {
                if (module.getDocument() != null) writeXmlFile(module.getDocument(), String.format(this.globalData.getData(IGlobalData.PROJECT_OUTPUT_PATH) + "/%s", module.getResource().getProjectRelativePath())); else writeBinaryFile(module.getResource().getLocation().toString(), String.format(this.globalData.getData(IGlobalData.PROJECT_OUTPUT_PATH) + "/%s", module.getResource().getProjectRelativePath()));
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.state = StageState.COMPLETE_WITH_ERRORS;
        }
    }

    private static void writeXmlFile(Document doc, String filename) throws Exception {
        Source source = new DOMSource(doc);
        File file = new File(filename);
        AgentFilesystem.forceDir(file.getParent());
        file.createNewFile();
        Result result = new StreamResult(file);
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(source, result);
        xformer.reset();
    }

    private static void writeBinaryFile(String filename, String target) throws IOException {
        File outputFile = new File(target);
        AgentFilesystem.forceDir(outputFile.getParent());
        FileOutputStream output = new FileOutputStream(new File(target));
        FileInputStream inputStream = new FileInputStream(filename);
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = inputStream.read(buffer)) > -1) output.write(buffer, 0, bytesRead);
        inputStream.close();
        output.close();
    }
}
