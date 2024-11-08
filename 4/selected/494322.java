package fr.michaelm.jump.drivers.dxf;

import javax.swing.JFileChooser;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.datasource.InstallStandardDataSourceQueryChoosersPlugIn;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.io.JUMPWriter;
import com.vividsolutions.jump.io.JUMPReader;
import com.vividsolutions.jump.workbench.datasource.LoadFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserManager;
import com.vividsolutions.jump.workbench.WorkbenchContext;

public class InstallDXFDataSourceQueryChooserPlugIn extends InstallStandardDataSourceQueryChoosersPlugIn {

    private void addFileDataSourceQueryChoosers(JUMPReader reader, JUMPWriter writer, final String description, WorkbenchContext workbenchContext, Class readerWriterDataSourceClass) {
        DataSourceQueryChooserManager.get(workbenchContext.getBlackboard()).addLoadDataSourceQueryChooser(new LoadFileDataSourceQueryChooser(readerWriterDataSourceClass, description, extensions(readerWriterDataSourceClass), workbenchContext) {

            protected void addFileFilters(JFileChooser chooser) {
                super.addFileFilters(chooser);
                InstallStandardDataSourceQueryChoosersPlugIn.addCompressedFileFilter(description, chooser);
            }
        }).addSaveDataSourceQueryChooser(new SaveDxfFileDataSourceQueryChooser(readerWriterDataSourceClass, description, extensions(readerWriterDataSourceClass), workbenchContext));
    }

    public void initialize(final PlugInContext context) throws Exception {
        Blackboard blackboard = context.getWorkbenchContext().getWorkbench().getBlackboard();
        addFileDataSourceQueryChoosers(new DxfReader(), new DxfWriter(), "DXF", context.getWorkbenchContext(), DXFFileReaderWriter.class);
    }
}
