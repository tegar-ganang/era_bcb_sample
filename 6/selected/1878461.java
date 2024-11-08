package totalpos;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import java.awt.Window;
import java.io.File;
import java.net.NoRouteToHostException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.jfree.ui.ExtensionFileFilter;

/**
 *
 * @author Saúl Hidalgo
 */
public class UpdateStock implements Doer {

    public Working workingFrame;

    private String mode;

    UpdateStock(String mode) {
        this.mode = mode;
    }

    public void updateStock() {
        workingFrame = new Working((Window) Shared.getMyMainWindows());
        WaitSplash ws = new WaitSplash(this);
        Shared.centerFrame(workingFrame);
        workingFrame.setVisible(true);
        ws.execute();
    }

    @Override
    public void doIt() {
        try {
            Shared.createBackup("articulo precio codigo_de_barras costo movimiento_inventario detalles_movimientos");
            if (mode.equals("FTP")) {
                FTPClient client = new FTPClient();
                client.connect(Constants.ftpHost);
                client.login(Constants.ftpUser, Constants.ftpPass);
                client.changeDirectory(Constants.ftpDir);
                File ff = new File(Constants.tmpDir + Constants.tmpFtpFileName);
                client.download("GT99_A09.rar", ff);
                client.disconnect(false);
                Shared.prepareMovements(ff);
            } else if (mode.equals("File")) {
                JFileChooser jfc = new JFileChooser();
                FileFilter f = new ExtensionFileFilter("Traslados de Total Pos", "rar");
                jfc.setFileFilter(f);
                int selection = jfc.showOpenDialog(Shared.getMyMainWindows());
                if (selection == JFileChooser.APPROVE_OPTION) {
                    Shared.prepareMovements(jfc.getSelectedFile());
                } else {
                    return;
                }
            }
            Shared.updateMovements();
            if (Shared.isHadMovements()) {
                MessageBox msg = new MessageBox(MessageBox.SGN_SUCCESS, "Fue cargado el nuevo inventario satisfactoriamente!");
                msg.show(Shared.getMyMainWindows());
            } else {
                MessageBox msg = new MessageBox(MessageBox.SGN_WARNING, "La tienda no tuvo ningun movimiento asociado.");
                msg.show(Shared.getMyMainWindows());
            }
        } catch (Exception ex) {
            MessageBox msg = new MessageBox(MessageBox.SGN_DANGER, "Ha ocurrido un error.", ex);
            msg.show(Shared.getMyMainWindows());
        }
    }

    @Override
    public void close() {
        workingFrame.setVisible(false);
    }
}
