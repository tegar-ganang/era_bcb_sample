package org.monet.setup.core.compiler.stages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.monet.kernel.model.BusinessModel;
import org.monet.kernel.model.BusinessUnit;
import org.monet.kernel.model.Dictionary;
import org.monet.kernel.model.definition.Definition;
import org.monet.kernel.model.definition.Model.LogoType;
import org.monet.kernel.utils.StreamHelper;
import org.monet.setup.core.compiler.GlobalData;
import org.monet.setup.core.compiler.Stage;

public class GenerateMobileModel extends Stage {

    @Override
    public void execute() {
        File currentModelDirectory = new File(this.globalData.getData(String.class, GlobalData.MODEL_INSTALL_DIRECTORY));
        File mobileDirectory = new File(currentModelDirectory, "mobile");
        mobileDirectory.mkdir();
        File mobileModelFile = new File(mobileDirectory, "mobile_model.zip");
        FileOutputStream outputStream = null;
        ZipOutputStream zipStream = null;
        BusinessModel businessModel = BusinessUnit.getInstance().getBusinessModel();
        try {
            mobileModelFile.createNewFile();
            outputStream = new FileOutputStream(mobileModelFile);
            zipStream = new ZipOutputStream(outputStream);
            Dictionary dictionary = businessModel.getDictionary();
            for (Definition definition : dictionary.getAllDefinitions()) {
                ZipEntry entry = new ZipEntry(definition.getRelativeFileName());
                zipStream.putNextEntry(entry);
                writeBinary(definition.getAbsoluteFileName(), zipStream);
            }
            final String modelFilename = "model.xml";
            ZipEntry entry = new ZipEntry(modelFilename);
            zipStream.putNextEntry(entry);
            writeBinary(businessModel.getAbsoluteFilename(modelFilename), zipStream);
            final String logoFilename = dictionary.getModel().getImageSource(LogoType.mobile);
            entry = new ZipEntry(logoFilename);
            zipStream.putNextEntry(entry);
            writeBinary(businessModel.getAbsoluteFilename(logoFilename), zipStream);
        } catch (IOException e) {
            agentLogger.error(e);
        } finally {
            StreamHelper.close(zipStream);
            StreamHelper.close(outputStream);
        }
    }

    private void writeBinary(String filename, OutputStream output) throws IOException {
        FileInputStream inputStream = new FileInputStream(filename);
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = inputStream.read(buffer)) > -1) output.write(buffer, 0, bytesRead);
        inputStream.close();
    }
}
