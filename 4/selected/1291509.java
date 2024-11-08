package com.ashs.jump.plugin;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.io.*;
import com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.datasource.*;
import javax.swing.JFileChooser;

/**
 * Adds to the JUMP Workbench the UIs for opening and saving files with the
 * CGDEF format.
 */
public class InstallASHSDataSourceQueryChooserPlugIn extends AbstractPlugIn {

    private void addFileDataSourceQueryChoosers(JUMPReader reader, JUMPWriter writer, final String description, WorkbenchContext context, Class readerWriterDataSourceClass) {
        DataSourceQueryChooserManager.get(context.getBlackboard()).addSaveDataSourceQueryChooser(new SaveFileDataSourceQueryChooser(readerWriterDataSourceClass, description, extensions(readerWriterDataSourceClass), context));
    }

    public static String[] extensions(Class readerWriterDataSourceClass) {
        try {
            return ((StandardReaderWriterFileDataSource) readerWriterDataSourceClass.newInstance()).getExtensions();
        } catch (Exception e) {
            Assert.shouldNeverReachHere(e.toString());
            return null;
        }
    }

    public void initialize(final PlugInContext context) throws Exception {
        addFileDataSourceQueryChoosers(new WKTReader(), new CGDEFWriter(), "CGDEF", context.getWorkbenchContext(), CGDEFWriter.CGDEF.class);
    }

    public static void addCompressedFileFilter(final String description, JFileChooser chooser) {
        chooser.addChoosableFileFilter(GUIUtil.createFileFilter("Compressed " + description, new String[] { "zip", "gz" }));
    }
}
