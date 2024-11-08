package corina.util;

import java.io.File;
import javax.swing.JOptionPane;
import corina.core.App;
import corina.gui.UserCancelledException;

/**
   A helper function for asking "do you really want to overwrite that
   file?".

   <p>To use, simply add a call to overwrite().  If the user cancels,
   a UserCancelledException is thrown; otherwise, it does nothing.</p>

<pre>
    try {
       String filename = FileDialog.showDialog("save");
       Overwrite.overwrite(filename);
       save(filename);
    } catch (UserCancelledException uce) {
       // do nothing
    } catch (IOException ioe) {
       // ...
    }
</pre>

   @author Ken Harris &lt;kbh7 <i style="color: gray">at</i> cornell <i style="color: gray">dot</i> edu&gt;
   @version $Id: Overwrite.java,v 1.3 2005/01/24 03:09:34 aaron Exp $
*/
public class Overwrite {

    private Overwrite() {
    }

    /**
       Ask if the user really wants to overwrite a file.

       <p>If the file doesn't exists, does nothing.  If it does
       already exist, it shows a dialog which informs the user that
       this file exists, and asks if Corina should overwrite it.</p>

       <p>If the file already exists, and the user chooses "Cancel",
       this method throws UserCancelledException; otherwise, it does
       nothing.</p>

       @param filename the name of the file
       @exception UserCancelledException if the file exists, and the
       user doesn't want to overwrite it
    */
    public static void overwrite(String filename) throws UserCancelledException {
        boolean exists = new File(filename).exists();
        if (!exists) return;
        String question = "A file called \"" + filename + "\"\n" + "already exists; overwrite it with this data?";
        String title = (App.platform.isMac() ? "" : "Already Exists");
        Object choices[] = new Object[] { "Overwrite", "Cancel" };
        int x = JOptionPane.showOptionDialog(null, question, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, null);
        boolean overwrite = (x == 0);
        if (!overwrite) throw new UserCancelledException();
    }
}
