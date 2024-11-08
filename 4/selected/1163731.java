package org.ala.spatial.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.legend.Legend;
import org.ala.layers.legend.LegendEqualArea;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.layers.OccurrenceDensity;
import org.ala.spatial.analysis.layers.Records;
import org.ala.spatial.analysis.layers.SitesBySpecies;
import org.ala.spatial.analysis.layers.SpeciesDensity;

/**
 *
 * @author Adam
 */
public class AnalysisJobSitesBySpecies extends AnalysisJob {

    long[] stageTimes;

    String currentPath;

    String speciesq;

    String qname;

    double gridsize;

    boolean sitesbyspecies, occurrencedensity, speciesdensity;

    int movingAverageSize;

    LayerFilter[] envelope;

    SimpleRegion region;

    String biocacheserviceurl;

    String areasqkm;

    public AnalysisJobSitesBySpecies(String pid, String currentPath_, String qname, String speciesq, double gridsize, SimpleRegion region_, LayerFilter[] filter_, boolean sitesbyspecies, boolean occurrencedensity, boolean speciesdensity, int movingAverageSize, String biocacheserviceurl, String areasqkm) {
        super(pid);
        currentPath = currentPath_ + File.separator + pid + File.separator;
        new File(currentPath).mkdirs();
        this.qname = qname;
        this.speciesq = speciesq;
        region = region_;
        envelope = filter_;
        this.gridsize = gridsize;
        this.movingAverageSize = movingAverageSize;
        stageTimes = new long[2];
        this.sitesbyspecies = sitesbyspecies;
        this.occurrencedensity = occurrencedensity;
        this.speciesdensity = speciesdensity;
        this.biocacheserviceurl = biocacheserviceurl;
        this.areasqkm = areasqkm;
    }

    @Override
    public void run() {
        try {
            if (movingAverageSize % 2 == 0 || movingAverageSize <= 0 || movingAverageSize >= 16) {
                String msg = "Moving average size " + movingAverageSize + " is not valid.  Must be odd and between 1 and 15.";
                setProgress(1, "failed: " + msg);
                setCurrentState(FAILED);
                System.out.println("failed: " + msg);
                setMessage(msg);
                return;
            }
            setCurrentState(RUNNING);
            setStage(0);
            double[] bbox = new double[4];
            bbox[0] = region.getBoundingBox()[0][0];
            bbox[1] = region.getBoundingBox()[0][1];
            bbox[2] = region.getBoundingBox()[1][0];
            bbox[3] = region.getBoundingBox()[1][1];
            setProgress(0, "getting species data");
            Records records = new Records(biocacheserviceurl, speciesq, bbox, null);
            int occurrenceCount = records.getRecordsSize();
            int boundingboxcellcount = (int) ((bbox[2] - bbox[0]) * (bbox[3] - bbox[1]) / (gridsize * gridsize));
            System.out.println("SitesBySpecies for " + occurrenceCount + " occurrences in up to " + boundingboxcellcount + " grid cells.");
            String error = null;
            if (boundingboxcellcount > AlaspatialProperties.getAnalysisLimitGridCells()) {
                error = "Too many potential output grid cells.  Decrease area or increase resolution.";
            } else if (occurrenceCount > AlaspatialProperties.getAnalysisLimitOccurrences()) {
                error = "Too many occurrences for the selected species.  " + occurrenceCount + " occurrences found, must be less than " + AlaspatialProperties.getAnalysisLimitOccurrences();
            } else if (occurrenceCount == 0) {
                error = "No occurrences found";
            }
            if (error != null) {
                setProgress(1, "failed: " + error);
                setCurrentState(FAILED);
                System.out.println("SitesBySpecies ERROR: " + error);
                setMessage(error);
                return;
            }
            setStage(1);
            setProgress(0, "building sites by species matrix for " + records.getSpeciesSize() + " species in " + records.getRecordsSize() + " occurrences");
            String envelopeFile = AlaspatialProperties.getAnalysisWorkingDir() + "envelope_" + getName();
            Grid envelopeGrid = null;
            if (envelope != null) {
                GridCutter.makeEnvelope(envelopeFile, AlaspatialProperties.getLayerResolutionDefault(), envelope);
                envelopeGrid = new Grid(envelopeFile);
            }
            if (sitesbyspecies) {
                SitesBySpecies sbs = new SitesBySpecies(0, gridsize, bbox);
                int[] counts = sbs.write(records, currentPath + File.separator, region, envelopeGrid);
                writeMetadata(currentPath + File.separator + "sxs_metadata.html", "Sites by Species", records, bbox, false, false, counts, areasqkm);
            }
            Legend occurrencesLegend = null;
            if (occurrencedensity) {
                setProgress(0.3, "building occurrence density layer");
                OccurrenceDensity od = new OccurrenceDensity(movingAverageSize, gridsize, bbox);
                od.write(records, currentPath + File.separator, "occurrence_density", AlaspatialProperties.getAnalysisThreadCount(), true, true);
                writeProjectionFile(currentPath + File.separator + "occurrence_density.prj");
                String url = AlaspatialProperties.getGeoserverUrl() + "/rest/workspaces/ALA/coveragestores/odensity_" + getName() + "/file.arcgrid?coverageName=odensity_" + getName();
                String extra = "";
                String username = AlaspatialProperties.getGeoserverUsername();
                String password = AlaspatialProperties.getGeoserverPassword();
                String[] infiles = { currentPath + "occurrence_density.asc", currentPath + "occurrence_density.prj" };
                String ascZipFile = currentPath + "occurrence_density.zip";
                Zipper.zipFiles(infiles, ascZipFile);
                System.out.println("Uploading file: " + ascZipFile + " to \n" + url);
                UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);
                occurrencesLegend = produceSld(currentPath + File.separator + "occurrence_density");
                url = AlaspatialProperties.getGeoserverUrl() + "/rest/styles/";
                UploadSpatialResource.loadCreateStyle(url, extra, username, password, "odensity_" + getName());
                url = AlaspatialProperties.getGeoserverUrl() + "/rest/styles/odensity_" + getName();
                UploadSpatialResource.loadSld(url, extra, username, password, currentPath + File.separator + "occurrence_density.sld");
                String data = "<layer><enabled>true</enabled><defaultStyle><name>odensity_" + getName() + "</name></defaultStyle></layer>";
                url = AlaspatialProperties.getGeoserverUrl() + "/rest/layers/ALA:odensity_" + getName();
                UploadSpatialResource.assignSld(url, extra, username, password, data);
                occurrencesLegend.generateLegend(currentPath + File.separator + "occurrence_density_legend.png");
                writeMetadata(currentPath + File.separator + "odensity_metadata.html", "Occurrence Density", records, bbox, occurrencedensity, false, null, null);
                new File(ascZipFile).delete();
            }
            Legend speciesLegend = null;
            if (speciesdensity) {
                setProgress(0.6, "building species richness layer");
                SpeciesDensity sd = new SpeciesDensity(movingAverageSize, gridsize, bbox);
                sd.write(records, currentPath + File.separator, "species_richness", AlaspatialProperties.getAnalysisThreadCount(), true, true);
                writeProjectionFile(currentPath + File.separator + "species_richness.prj");
                String url = AlaspatialProperties.getGeoserverUrl() + "/rest/workspaces/ALA/coveragestores/srichness_" + getName() + "/file.arcgrid?coverageName=srichness_" + getName();
                String extra = "";
                String username = AlaspatialProperties.getGeoserverUsername();
                String password = AlaspatialProperties.getGeoserverPassword();
                String[] infiles = { currentPath + "species_richness.asc", currentPath + "species_richness.prj" };
                String ascZipFile = currentPath + "species_richness.zip";
                Zipper.zipFiles(infiles, ascZipFile);
                System.out.println("Uploading file: " + ascZipFile + " to \n" + url);
                UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);
                speciesLegend = produceSld(currentPath + File.separator + "species_richness");
                url = AlaspatialProperties.getGeoserverUrl() + "/rest/styles/";
                UploadSpatialResource.loadCreateStyle(url, extra, username, password, "srichness_" + getName());
                url = AlaspatialProperties.getGeoserverUrl() + "/rest/styles/srichness_" + getName();
                UploadSpatialResource.loadSld(url, extra, username, password, currentPath + File.separator + "species_richness.sld");
                String data = "<layer><enabled>true</enabled><defaultStyle><name>srichness_" + getName() + "</name></defaultStyle></layer>";
                url = AlaspatialProperties.getGeoserverUrl() + "/rest/layers/ALA:srichness_" + getName();
                UploadSpatialResource.assignSld(url, extra, username, password, data);
                speciesLegend.generateLegend(currentPath + File.separator + "species_richness_legend.png");
                writeMetadata(currentPath + File.separator + "srichness_metadata.html", "Species Richness", records, bbox, false, speciesdensity, null, null);
                new File(ascZipFile).delete();
            }
            setProgress(1, "finished");
            CitationService.generateSitesBySpeciesReadme(currentPath + File.separator, sitesbyspecies, occurrencedensity, speciesdensity);
            setCurrentState(SUCCESSFUL);
            System.out.println("finished building sites by species matrix");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed with exception: " + e.getMessage());
            setProgress(1, "failed: " + e.getMessage());
            setCurrentState(FAILED);
            setMessage("Error processing your Sites By Species request. Please try again or if problem persists, contact the Administrator\nPlease quote the Prediction ID: " + getName());
        }
    }

    @Override
    public long getEstimate() {
        if (getProgress() == 0) {
            return 0;
        }
        long progTime;
        synchronized (progress) {
            progTime = progressTime;
        }
        long timeRemaining = 0;
        long t1 = 0, t2 = 0, t3 = 0;
        if (stage <= 0) {
            t1 = t1 + progTime - stageTimes[0];
        }
        if (stage <= 1) {
            if (stage == 1) {
                t2 = t2 + progTime - stageTimes[1];
            }
        }
        timeRemaining = t1 + t2 + t3;
        return timeRemaining;
    }

    @Override
    public void setProgress(double d) {
        if (stage == 0) {
            progress = d / 5.0;
        } else if (stage == 1) {
            progress = 0.2 + 10 * d / 7.0;
        } else {
            progress = 0.9 + d / 10.0;
        }
        super.setProgress(progress);
    }

    @Override
    public double getProgress() {
        long currentTime = System.currentTimeMillis();
        long progTime;
        synchronized (progress) {
            progTime = progressTime;
        }
        long t1 = 0, t2 = 0;
        double d1, d2;
        if (stage <= 0) {
            t1 += 0;
            d1 = (currentTime - stageTimes[0]) / (double) t1;
            if (d1 > 0.9) {
                d1 = 0.9;
            }
            d1 *= 0.2;
        } else {
            d1 = 0.2;
        }
        if (stage <= 1) {
            t2 += 0;
            if (stage == 1) {
                d2 = (currentTime - stageTimes[1]) / (double) t2;
            } else {
                d2 = 0;
            }
            if (d2 > 0.9) {
                d2 = 0.9;
            }
            d2 *= 0.7;
        } else {
            d2 = 0.7;
        }
        return d1 + d2;
    }

    @Override
    public String getStatus() {
        if (getProgress() < 1) {
            String msg;
            if (stage == 0) {
                msg = "Data preparation, ";
            } else if (stage == 1) {
                msg = "Running, ";
            } else {
                msg = "Exporting results, ";
            }
            return msg + "est remaining: " + getEstimateInMinutes() + " min";
        } else {
            if (stage == -1) {
                return "not started, est: " + getEstimateInMinutes() + " min";
            } else {
                return "finished, total run time=" + Math.round(getRunTime() / 1000) + "s";
            }
        }
    }

    @Override
    public void setStage(int i) {
        super.setStage(i);
        if (i < 4) {
            stageTimes[i] = System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getName());
        sb.append("; SitesBySpecies");
        sb.append("; state=").append(getCurrentState());
        sb.append("; status=").append(getStatus());
        sb.append("; resolution=").append(gridsize);
        sb.append("; speciesq=").append(speciesq);
        return sb.toString();
    }

    @Override
    public String getImage() {
        return "";
    }

    @Override
    AnalysisJob copy() {
        return new AnalysisJobSitesBySpecies(String.valueOf(System.currentTimeMillis()), currentPath, qname, speciesq, gridsize, region, envelope, sitesbyspecies, occurrencedensity, speciesdensity, movingAverageSize, biocacheserviceurl, areasqkm);
    }

    private void writeProjectionFile(String outputpath_prj) {
        try {
            FileWriter fw = new FileWriter(outputpath_prj);
            fw.append("GEOGCS[\"WGS 84\", ").append("\n");
            fw.append("    DATUM[\"WGS_1984\", ").append("\n");
            fw.append("        SPHEROID[\"WGS 84\",6378137,298.257223563, ").append("\n");
            fw.append("            AUTHORITY[\"EPSG\",\"7030\"]], ").append("\n");
            fw.append("        AUTHORITY[\"EPSG\",\"6326\"]], ").append("\n");
            fw.append("    PRIMEM[\"Greenwich\",0, ").append("\n");
            fw.append("        AUTHORITY[\"EPSG\",\"8901\"]], ").append("\n");
            fw.append("    UNIT[\"degree\",0.01745329251994328, ").append("\n");
            fw.append("        AUTHORITY[\"EPSG\",\"9122\"]], ").append("\n");
            fw.append("    AUTHORITY[\"EPSG\",\"4326\"]] ").append("\n");
            fw.close();
        } catch (IOException ex) {
            System.out.println("error writing prj file:");
            ex.printStackTrace(System.out);
        }
    }

    Legend produceSld(String gridfilename) {
        Grid g = new Grid(gridfilename);
        float[] d = g.getGrid();
        java.util.Arrays.sort(d);
        Legend legend = new LegendEqualArea();
        legend.generate(d);
        legend.determineGroupSizes(d);
        legend.evaluateStdDevArea(d);
        d = null;
        g = null;
        System.gc();
        g = new Grid(gridfilename);
        d = g.getGrid();
        legend.exportImage(d, g.ncols, gridfilename + ".png", 1, true);
        legend.exportSLD(g, gridfilename + ".sld", "", false, true);
        return legend;
    }

    void writeMetadata(String filename, String title, Records records, double[] bbox, boolean odensity, boolean sdensity, int[] counts, String addAreaSqKm) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        FileWriter fw = new FileWriter(filename);
        fw.append("<html><h1>").append(title).append("</h1>");
        fw.append("<table>");
        fw.append("<tr><td>Date/time " + sdf.format(new Date()) + "</td></tr>");
        fw.append("<tr><td>Model reference number: " + getName() + "</td></tr>");
        fw.append("<tr><td>Species selection " + qname + "</td></tr>");
        if (!odensity && !sdensity) {
            fw.append("<tr><td>Grid: " + 1 + "x" + 1 + " moving average, resolution " + gridsize + " degrees</td></tr>");
        } else {
            fw.append("<tr><td>Grid: " + movingAverageSize + "x" + movingAverageSize + " moving average, resolution " + gridsize + " degrees</td></tr>");
        }
        fw.append("<tr><td>" + records.getSpeciesSize() + " species</td></tr>");
        fw.append("<tr><td>" + records.getRecordsSize() + " occurrences</td></tr>");
        if (counts != null) {
            fw.append("<tr><td>" + counts[0] + " grid cells with an occurrence</td></tr>");
            fw.append("<tr><td>" + counts[1] + " grid cells in the area (both marine and terrestrial)</td></tr>");
        }
        if (addAreaSqKm != null) {
            fw.append("<tr><td>Selected area " + addAreaSqKm + " sqkm</td></tr>");
        }
        fw.append("<tr><td>bounding box of the selected area " + bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3] + "</td></tr>");
        if (odensity) {
            fw.append("<tr><td><br>Occurrence Density</td></tr>");
            fw.append("<tr><td><img src='occurrence_density.png' width='300px' height='300px'><img src='occurrence_density_legend.png'></td></tr>");
        }
        if (sdensity) {
            fw.append("<tr><td><br>Species Richness</td></tr>");
            fw.append("<tr><td><img src='species_richness.png' width='300px' height='300px'><img src='species_richness_legend.png'></td></tr>");
        }
        fw.append("</table>");
        fw.append("</html>");
        fw.close();
    }
}
