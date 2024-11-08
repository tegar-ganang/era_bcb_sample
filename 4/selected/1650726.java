package uk.ac.ed.csbe.plasmo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import uk.ac.ed.plasmo.ws.client.pro.ProWebServicesClient;
import uk.ac.ed.plasmo.ws.client.pro.ProWebServicesStub.VsnSubmission;
import uk.ac.ed.plasmo.ws.client.pro.ProWebServicesStub.WSModelSubmission;
import uk.ac.ed.plasmo.ws.client.pub.WebServicesClient;

public class WSWrapper {

    File lib;

    public WSWrapper(WebServicesClient ws) {
        this.ws = ws;
    }

    public WSWrapper(String userName, String password) {
        createTempFilesForProWs();
        try {
            prows = new ProWebServicesClient(lib.getAbsolutePath(), userName, password);
        } catch (Exception e) {
            throw new RuntimeException("Problem with web service connection." + e.getMessage());
        }
    }

    private WebServicesClient ws;

    private ProWebServicesClient prows;

    public String getModelById(String id) throws Exception {
        return prows == null ? ws.getPublicModelById(id) : prows.getModelById(id);
    }

    private void createTempFilesForProWs() {
        try {
            lib = new File(System.getProperty("java.io.tmpdir"), "lib");
            if (!lib.exists()) lib.mkdirs();
            File modules = new File(lib.getAbsolutePath() + File.separator + "modules");
            if (!modules.exists()) modules.mkdirs();
            File rahas = new File(System.getProperty("java.io.tmpdir"), "lib" + File.separator + "modules" + File.separator + "rahas-1.5.mar");
            if (!rahas.exists()) {
                rahas.createNewFile();
                FileOutputStream fos = new FileOutputStream(rahas);
                InputStream is = PlasmoQuery.class.getResourceAsStream("/lib/modules/rahas-1.5.mar");
                byte[] bytes = new byte[4096];
                int read = 0;
                while ((read = is.read(bytes)) != -1) {
                    fos.write(bytes, 0, read);
                }
                is.close();
                fos.close();
            }
            File rampart = new File(System.getProperty("java.io.tmpdir"), "lib" + File.separator + "modules" + File.separator + "rampart-1.5.mar");
            if (!rampart.exists()) {
                rampart.createNewFile();
                FileOutputStream fos = new FileOutputStream(rampart);
                InputStream is = PlasmoQuery.class.getResourceAsStream("/lib/modules/rampart-1.5.mar");
                byte[] bytes = new byte[4096];
                int read = 0;
                while ((read = is.read(bytes)) != -1) {
                    fos.write(bytes, 0, read);
                }
                is.close();
                fos.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp files for loading of security modules. File system may be corrupt.");
        }
    }

    public List<PlasmoModelWrapper> getSummaryOfAllModels(int i, int j) throws Exception {
        List<PlasmoModelWrapper> results = new ArrayList<PlasmoModelWrapper>();
        if (ws != null) {
            uk.ac.ed.plasmo.ws.client.pub.PubWebServicesStub.ModelSummary[] models = ws.getSummaryOfAllPublicModels(i, j);
            if (models == null || models[0] == null) return null;
            for (int start = 0; start < models.length; start++) results.add(new PlasmoModelWrapper(models[start]));
        } else {
            uk.ac.ed.plasmo.ws.client.pro.ProWebServicesStub.ModelSummary[] models = prows.getSummaryOfAllAccessibleModels(i, j);
            if (models == null || models[0] == null) return null;
            for (int start = 0; start < models.length; start++) results.add(new PlasmoModelWrapper(models[start]));
        }
        return results;
    }

    public List<PlasmoModelWrapper> getSummaryOfAllPublicModelsSearch(int i, int j, String term) throws Exception {
        List<PlasmoModelWrapper> results = new ArrayList<PlasmoModelWrapper>();
        if (ws != null) {
            uk.ac.ed.plasmo.ws.client.pub.PubWebServicesStub.ModelSummary[] models = ws.getSummaryOfAllPublicModelsSearch(i, j, term);
            if (models == null || models[0] == null) return null;
            for (int start = 0; start < models.length; start++) results.add(new PlasmoModelWrapper(models[start]));
        } else {
            uk.ac.ed.plasmo.ws.client.pro.ProWebServicesStub.ModelSummary[] models = prows.getSummaryOfAllAccessibleModelsSearch(i, j, term);
            if (models == null || models[0] == null) return null;
            for (int start = 0; start < models.length; start++) results.add(new PlasmoModelWrapper(models[start]));
        }
        return results;
    }

    /**
	 * An attempt to hack together something out of the 'api' that plasmo webservice provides.... may be slow...
	 * The issue being that the public WS of plasmo cant be searched by ID to return ModelSummaries
	 * @param plasmoID
	 * @return
	 * @throws Exception
	 */
    public PlasmoModelWrapper getModelSummaryById(String plasmoID) throws Exception {
        String name = null;
        if (ws != null) {
            name = ws.getModelNameById(plasmoID);
            List<PlasmoModelWrapper> wrappers = getSummaryOfAllPublicModelsSearch(0, 1000000, plasmoID);
            for (PlasmoModelWrapper wrap : wrappers) {
                if (wrap.getId().equals(plasmoID)) return wrap;
            }
        } else {
            return new PlasmoModelWrapper(prows.getModelSummaryById(plasmoID));
        }
        return null;
    }

    /**
	 * @param selectedModel
	 * @param privacy true if model will be private 
	 * @throws Exception
	 * Adds a new model and gives it version=1
	 */
    public void addNewModel(String modelAsString, String modelName, String newModelFormat, boolean privacy) throws Exception {
        if (prows == null) throw new IllegalArgumentException("Only private web service can upload a model.");
        WSModelSubmission wm = new WSModelSubmission();
        wm.setModelAsString(modelAsString);
        wm.setName(modelName);
        wm.setFormat(newModelFormat);
        wm.set_private(privacy);
        prows.addNewModel(wm);
    }

    /**
	 * @param modelAsString
	 * @param newModelFormat
	 * @param existingVersion
	 * @throws Exception
	 * adds a model with its version being equals to the latest of an existing model with same plasmo ID plus ONE
	 */
    public void addNewVersionOfModel(String modelAsString, String newModelFormat, PlasmoModelWrapper existingVersion) throws Exception {
        if (prows == null) throw new IllegalArgumentException("Only private web service can upload a model.");
        VsnSubmission newVersion = new VsnSubmission();
        newVersion.setModelAsString(modelAsString);
        newVersion.setFormat(newModelFormat);
        newVersion.setVersion("" + (new Integer(existingVersion.getLatestVersion()) + 1));
        newVersion.setAccession(existingVersion.getId());
        prows.addNewModelVersion(newVersion);
    }

    /**
	 * @param modelAsString
	 * @param newModelFormat
	 * @param existingVersion
	 * @throws Exception
	 * replaces an existing model in the database - the one replaced is whichever has the highest version at the time of upload. Version does NOT change ina  replace
	 */
    public void replaceModel(String modelAsString, String newModelFormat, PlasmoModelWrapper existingVersion) throws Exception {
        if (prows == null) throw new IllegalArgumentException("Only private web service can upload a model.");
        VsnSubmission newVersion = new VsnSubmission();
        newVersion.setModelAsString(modelAsString);
        newVersion.setFormat(newModelFormat);
        newVersion.setVersion(existingVersion.getLatestVersion());
        newVersion.setAccession(existingVersion.getId());
        prows.replaceModel(newVersion);
    }
}
