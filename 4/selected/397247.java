package com.isa.jump.plugin;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.java2xml.Java2XML;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.io.datasource.DataSource;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CreateArchivePlugIn extends AbstractPlugIn {

    private static JFileChooser fileChooser;

    private static WorkbenchContext workbenchContext = null;

    public void initialize(PlugInContext context) throws Exception {
        workbenchContext = context.getWorkbenchContext();
        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        fileChooser = GUIUtil.createJFileChooserWithOverwritePrompting();
        fileChooser.setDialogTitle("Save Archive");
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setMultiSelectionEnabled(false);
        GUIUtil.removeChoosableFileFilters(fileChooser);
        FileFilter fileFilter1 = GUIUtil.createFileFilter("ZIP Files", new String[] { "zip" });
        fileChooser.addChoosableFileFilter(fileFilter1);
        fileChooser.setFileFilter(fileFilter1);
        JPopupMenu layerNamePopupMenu = workbenchContext.getWorkbench().getFrame().getLayerNamePopupMenu();
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, this, "Archive Selected Datasets", false, ICON, CreateArchivePlugIn.createEnableCheck(workbenchContext));
    }

    public static final ImageIcon ICON = IconLoader.icon("compress.png");

    public boolean execute(PlugInContext context) throws Exception {
        workbenchContext = context.getWorkbenchContext();
        Collection layerCollection = (Collection) context.getWorkbenchContext().getLayerNamePanel().selectedNodes(Layer.class);
        boolean writeWarning = false;
        boolean askUser = false;
        String warningStr = "Cannot create the archive because the following layer(s) have not been saved:";
        for (Iterator l = layerCollection.iterator(); l.hasNext(); ) {
            Layer layer = (Layer) l.next();
            DataSourceQuery dsq = layer.getDataSourceQuery();
            if (dsq == null) {
                warningStr = warningStr + "\n" + layer.getName();
                writeWarning = true;
            } else {
                Object fnameObj = dsq.getDataSource().getProperties().get("File");
                String fname = "";
                if (fnameObj != null) fname = fnameObj.toString();
                if (fname == "") {
                    warningStr = warningStr + "\n" + layer.getName();
                    writeWarning = true;
                }
            }
            if (layer.isFeatureCollectionModified()) {
                askUser = true;
            }
        }
        if (writeWarning) {
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame().warnUser("Warning: archive not created - see output window");
            context.getWorkbenchFrame().getOutputFrame().addText(warningStr);
            return true;
        }
        if (askUser) {
            MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
            dialog.addLabel("Some modified datasets have not been saved.\n");
            dialog.addLabel("The archive will not contain these changes.\n");
            dialog.addLabel(" ");
            dialog.addLabel("Continue?");
            dialog.setVisible(true);
            if (!dialog.wasOKPressed()) return true;
        }
        File saveDir = FileUtil.GetSaveDirectory(workbenchContext);
        if (saveDir != null) fileChooser.setCurrentDirectory(saveDir);
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy_HHmm-ss");
        String dateStr = df.format(date);
        String suggestedFileName = context.getTask().getName() + "_" + dateStr + ".zip";
        fileChooser.setSelectedFile(new File(suggestedFileName));
        if (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(workbenchContext.getLayerViewPanel())) {
            FileUtil.PutSaveDirectory(workbenchContext, fileChooser.getCurrentDirectory());
            String zipFileName = fileChooser.getSelectedFile().getPath();
            if (!(zipFileName.toLowerCase().endsWith(".zip"))) zipFileName = zipFileName + ".zip";
            try {
                ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFileName));
                zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
                LayerManager layerManager = new LayerManager();
                for (Iterator l = layerCollection.iterator(); l.hasNext(); ) {
                    Layer layer = (Layer) l.next();
                    String layerName = layerManager.uniqueLayerName(layer.getName());
                    FeatureSchema featureSchema = new FeatureSchema();
                    featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
                    FeatureDataset featureCollection = new FeatureDataset(featureSchema);
                    layerManager.addLayer("Working", layerName, featureCollection);
                    String entryPrefix = layerName;
                    String taskFilePath = createLayerTask(layer, zipFileName, entryPrefix);
                    WriteZipEntry(taskFilePath, entryPrefix, zipOut);
                    if (layer.getDataSourceQuery().getDataSource().isWritable()) {
                        String layerFilePath = layer.getDataSourceQuery().getDataSource().getProperties().get("File").toString();
                        WriteZipEntry(layerFilePath, entryPrefix, zipOut);
                    }
                }
                zipOut.close();
            } catch (Exception e) {
                context.getWorkbenchFrame().getOutputFrame().createNewDocument();
                context.getWorkbenchFrame().warnUser("Error: see output window");
                context.getWorkbenchFrame().getOutputFrame().addText("CreateArchivePlugIn Exception:" + e.toString());
                return false;
            }
        }
        return true;
    }

    public static void WriteZipEntry(String fileName, String entryPrefix, ZipOutputStream zout) throws Exception {
        if ((fileName == "") || (entryPrefix == "") || (zout == null)) return;
        File layerFile = new File(fileName);
        if (layerFile.exists()) {
            int dotPosition = fileName.lastIndexOf('.');
            String ext = fileName.substring(dotPosition);
            zout.putNextEntry(new ZipEntry(entryPrefix + ext));
            FileInputStream fin = new FileInputStream(fileName);
            writeToZip(fin, zout);
            if (ext.toLowerCase().equalsIgnoreCase(".shp")) {
                fileName = layerFile.getParent() + File.separator + GUIUtil.nameWithoutExtension(layerFile);
                if (new File(fileName + ".shx").exists()) {
                    zout.putNextEntry(new ZipEntry(entryPrefix + ".shx"));
                    fin = new FileInputStream(fileName + ".shx");
                    writeToZip(fin, zout);
                }
                if (new File(fileName + ".dbf").exists()) {
                    zout.putNextEntry(new ZipEntry(entryPrefix + ".dbf"));
                    fin = new FileInputStream(fileName + ".dbf");
                    writeToZip(fin, zout);
                }
                if (new File(fileName + ".shp.xml").exists()) {
                    zout.putNextEntry(new ZipEntry(entryPrefix + ".shp.xml"));
                    fin = new FileInputStream(fileName + ".shp.xml");
                    writeToZip(fin, zout);
                }
                if (new File(fileName + ".prj").exists()) {
                    zout.putNextEntry(new ZipEntry(entryPrefix + ".prj"));
                    fin = new FileInputStream(fileName + ".prj");
                    writeToZip(fin, zout);
                }
            }
        }
    }

    public static String createLayerTask(Layer layer, String zipFileName, String entryPrefix) throws Exception {
        DataSourceQuery dsqOut = layer.getDataSourceQuery();
        if (dsqOut == null) return "";
        DataSource ds = dsqOut.getDataSource();
        Object fileObj = ds.getProperties().get("File");
        if (fileObj == null) return "";
        String sourcePath = fileObj.toString();
        String ext = FileUtil.getExtension(new File(sourcePath));
        String saveLayerFilePath = ds.getProperties().get("File").toString();
        ds.getProperties().put("File", "_local_to_task_." + ext);
        File layerTaskFile = File.createTempFile("ltf", ".jmp");
        String layerTaskPath = layerTaskFile.getAbsolutePath();
        layerTaskFile.deleteOnExit();
        Task task = new Task();
        LayerManager layerManager = task.getLayerManager();
        layerManager.addLayer("Working", layer);
        StringWriter stringWriter = new StringWriter();
        try {
            WorkbenchFrame frame = workbenchContext.getWorkbench().getFrame();
            JInternalFrame taskWindow = frame.getActiveInternalFrame();
            task.setMaximized(taskWindow.isMaximum());
            if (taskWindow.isMaximum()) {
                Rectangle normalBounds = taskWindow.getNormalBounds();
                task.setTaskWindowLocation(new Point(normalBounds.x, normalBounds.y));
                task.setTaskWindowSize(new Dimension(normalBounds.width, normalBounds.height));
            } else {
                task.setTaskWindowLocation(taskWindow.getLocation());
                task.setTaskWindowSize(taskWindow.getSize());
            }
            task.setSavedViewEnvelope(frame.getContext().getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates());
            new Java2XML().write(task, "project", stringWriter);
        } finally {
            stringWriter.flush();
        }
        FileUtil.setContents(layerTaskPath, stringWriter.toString());
        ds.getProperties().put("File", saveLayerFilePath);
        return layerTaskPath;
    }

    public static void writeToZip(FileInputStream in, ZipOutputStream out) {
        byte[] buffer = new byte[18024];
        int len;
        try {
            while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            out.closeEntry();
            in.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck()).add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
    }
}
