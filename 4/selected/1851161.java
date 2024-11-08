package org.web3d.x3d.actions.conversions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.web3d.x3d.X3DDataObject;
import org.web3d.x3d.X3DEditorSupport;

@ActionID(id = "org.web3d.x3d.actions.conversions.GzipX3dAction", category = "Tools")
@ActionRegistration(displayName = "#CTL_GzipX3dAction")
@ActionReference(path = "Menu/X3D/Conversions", position = 1000)
public class GzipX3dAction extends BaseConversionsAction {

    @Override
    public String transformSingleFile(X3DEditorSupport.X3dEditor xed) {
        Node[] node = xed.getActivatedNodes();
        X3DDataObject dob = (X3DDataObject) xed.getX3dEditorSupport().getDataObject();
        FileObject mySrc = dob.getPrimaryFile();
        File mySrcF = FileUtil.toFile(mySrc);
        File myOutF = new File(mySrcF.getParentFile(), mySrc.getName() + ".x3d.gz");
        TransformListener co = TransformListener.getInstance();
        co.message(NbBundle.getMessage(getClass(), "Gzip_compression_starting"));
        co.message(NbBundle.getMessage(getClass(), "Saving_as_") + myOutF.getAbsolutePath());
        co.moveToFront();
        co.setNode(node[0]);
        try {
            FileInputStream fis = new FileInputStream(mySrcF);
            GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(myOutF));
            byte[] buf = new byte[4096];
            int ret;
            while ((ret = fis.read(buf)) > 0) gzos.write(buf, 0, ret);
            gzos.close();
        } catch (Exception ex) {
            co.message(NbBundle.getMessage(getClass(), "Exception:__") + ex.getLocalizedMessage());
            return null;
        }
        co.message(NbBundle.getMessage(getClass(), "Gzip_compression_complete"));
        return myOutF.getAbsolutePath();
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(getClass(), "CTL_GzipX3dAction");
    }

    @Override
    protected void initialize() {
        super.initialize();
        putValue("noIconInMenu", Boolean.TRUE);
    }

    /**
   * Do this because this call in the super creates a new one every time, loosing any 
   * previous tt.
   * @return what goes into the menu
   */
    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem mi = super.getMenuPresenter();
        mi.setToolTipText(NbBundle.getMessage(getClass(), "CTL_GzipX3dAction_tt"));
        return mi;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
