package gui;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import converter.Start;

/**
 * the ESwingWorker in which the BachroundOperations are coordinated
 * @author Sebastian Geib
 * @author Alexander Sch&auml;ffer
 */
class ESwingWorker extends SwingWorker<String, Object> implements FileFilter {

    /**
	 * the gif image
	 */
    private File source;

    /**
	 * the png image
	 */
    private File destination;

    /**
	 * is called after a ESwingWorker-Obj. called execute();
	 * this method is runs automatically in another Thread than the Gui
	 *  (see JavaDoc SwingWorker Workflow for more details)<br>
	 *  
	 *  checks if the source file or directory exists and if its
	 *  a directory or a file, depending on this it calls the prepareConvert
	 *  method once or if its a directory as often as gif files in the directory
	 */
    protected String doInBackground() {
        source = new File(EJFrame.getEJFrame().mPathPanel.getSourcePath());
        EJFrame.getEJFrame().mConvertPanel.updateoverallcur(0);
        EJFrame.getEJFrame().mConvertPanel.updateoverallmax(0);
        EJFrame.getEJFrame().mConvertPanel.updatecur(0);
        EJFrame.getEJFrame().mConvertPanel.updatemax(0);
        EJFrame.getEJFrame().mConvertPanel.mFileNameLabel.setText("current File/Dir.: " + source.getName());
        if (!source.exists()) {
            return "Source file or directory doesn't exists.";
        }
        if (!source.isDirectory()) {
            EJFrame.getEJFrame().mConvertPanel.updateoverallmax(1);
            destination = new File(EJFrame.getEJFrame().mPathPanel.getDestinationPath());
            EJFrame.getEJFrame().mConvertPanel.mFileNameLabel.setText("current File: " + source.getName());
            String ret = this.prepareConvert(source, destination);
            if (ret == null) {
                EJFrame.getEJFrame().mConvertPanel.updateoverallcur(1);
                return null;
            } else {
                return ret;
            }
        } else {
            File[] f = source.listFiles(this);
            EJFrame.getEJFrame().mConvertPanel.updateoverallmax(f.length);
            File destDirectory = new File(EJFrame.getEJFrame().mPathPanel.getDestinationPath());
            if (!destDirectory.isDirectory()) {
                return "Destination isn't a directory or the directory doesn't exist.";
            }
            for (int i = 0; i < f.length; i++) {
                if (this.isCancelled()) {
                    return null;
                }
                EJFrame.getEJFrame().mConvertPanel.updateoverallcur(i);
                String destinationPath = destDirectory.getAbsolutePath() + File.separator + f[i].getName().substring(0, f[i].getName().length() - 4) + ".apng";
                destination = new File(destinationPath);
                EJFrame.getEJFrame().mConvertPanel.mFileNameLabel.setText("current File: " + f[i].getName());
                String ret = this.prepareConvert(f[i], destination);
                if (i == f.length - 1) {
                    EJFrame.getEJFrame().mConvertPanel.updateoverallcur(i + 1);
                }
                if (ret != null) {
                    EJFrame.getEJFrame().mConvertPanel.updateoverallcur(i + 1);
                    return ret;
                }
            }
        }
        return null;
    }

    /**
	 * is called after doINBackround() finsihed;
	 * runs in another Thread than doInBackround()
	 * (see JavaDoc SwingWorker Workflow for more details);<br>
	 * 
	 * gets with get() the excptions or return-values came
	 * from doInBackround()<br>
	 * deletes the current picture that was in creation when an error occurred<br>
	 * if the return value from doInBackround() isnt null it shows an MessageDialog
	 * with the error in it
	 */
    protected void done() {
        EJFrame.getEJFrame().mConvertPanel.mStopButton.setEnabled(false);
        EJFrame.getEJFrame().mConvertPanel.mConvertButton.setEnabled(true);
        String err = "";
        try {
            err = get();
            if (err != null) {
                if (err.equals(EJFrame.CANCEL_STRING)) {
                    throw new CancellationException();
                } else {
                    JOptionPane.showMessageDialog(EJFrame.getEJFrame(), err, err, JOptionPane.ERROR_MESSAGE);
                    EJFrame.getEJFrame().mConvertPanel.mFileNameLabel.setText("<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>" + "<font color=\"#d00000\">error</font></b> at \"" + EJFrame.getEJFrame().mConvertPanel.mFileNameLabel.getText() + "\"</html>");
                }
            } else {
                EJFrame.getEJFrame().mConvertPanel.mFileNameLabel.setText("<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b><font color=#008f00>finished</font></b></html>");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (CancellationException e) {
            EJFrame.getEJFrame().mConvertPanel.updatecur(0);
            EJFrame.getEJFrame().mConvertPanel.updatemax(0);
            EJFrame.getEJFrame().mConvertPanel.updateoverallcur(0);
            EJFrame.getEJFrame().mConvertPanel.updateoverallmax(0);
            EJFrame.getEJFrame().mConvertPanel.mFileNameLabel.setText("<html>&nbsp;&nbsp;&nbsp;&nbsp;<b><font color=#ff8f00>Cancelled</font></b></html>");
        }
    }

    @Override
    public boolean accept(File pathname) {
        if (!pathname.isDirectory() && pathname.getName().length() >= 4) {
            StringBuffer sb = new StringBuffer(pathname.getName());
            return sb.substring(sb.length() - 3).toLowerCase().equals("gif");
        } else {
            return false;
        }
    }

    /**
	 * Prepares the convert, means check for correct file-endings (.gif/.GIF .png/.PNG)
	 * and create the destination File, when in the preparing no error occured the 
	 * convert will be started<br>
	 * @param source the source(.gif) File
	 * @param destination the destination(.png) File
	 * @return null if no error occured otherwise an errormessage
	 */
    public String prepareConvert(File source, File destination) {
        if (!PathJPanel.isGif(source)) {
            return "Source File not a .gif";
        }
        try {
            if (destination.exists()) {
                int i = JOptionPane.showConfirmDialog(EJFrame.getEJFrame(), "\"" + destination.getName() + "\"" + " already exists, overwrite this file?", "File already exists", JOptionPane.YES_NO_CANCEL_OPTION);
                if (i == JOptionPane.OK_OPTION) {
                    destination.delete();
                    destination.createNewFile();
                } else if (i == JOptionPane.NO_OPTION) {
                    return null;
                } else if (i == JOptionPane.CANCEL_OPTION || i < 0) {
                    return EJFrame.CANCEL_STRING;
                }
            }
            Start.convert(source, destination, EJFrame.getEJFrame());
        } catch (Exception e) {
            if (e instanceof CancellationException) {
                return EJFrame.CANCEL_STRING;
            }
            String errmsg = e.getMessage();
            String errtype = "";
            if (errmsg.contains("reading")) {
                errtype = " while reading";
            } else if (errmsg.contains("writing")) {
                errtype = " while writing";
            }
            if (e.getCause() != null && errtype.length() != 0) {
                errmsg = e.getCause().getMessage();
            }
            System.err.println(errmsg);
            return "Error" + errtype + ": " + errmsg;
        }
        return null;
    }
}
