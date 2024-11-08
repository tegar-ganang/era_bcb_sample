package org.gems.designer.metamodel.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.gems.designer.model.Atom;
import org.gems.designer.model.Model;
import org.gems.designer.model.ModelObject;

public class IconGenerator {

    public IconGenerator() {
        super();
    }

    public void generateIcon(File target, ModelObjectInfo iconfor) {
        generateIcon(target, (ModelObject) iconfor);
    }

    public void generateIcon(File target, ModelObject iconfor) {
        try {
            if (iconfor instanceof AtomInfo) {
                InputStream str = Atom.class.getResourceAsStream("icons/Atom.gif");
                copy(str, new File(target.getAbsolutePath() + File.separator + iconfor.getName() + ".gif"));
                str.close();
                str = Atom.class.getResourceAsStream("icons/Atom_s.gif");
                copy(str, new File(target.getAbsolutePath() + File.separator + iconfor.getName() + "_s.gif"));
                str.close();
            } else if (iconfor instanceof ModelInfo) {
                InputStream str = Model.class.getResourceAsStream("icons/Model.gif");
                copy(str, new File(target.getAbsolutePath() + File.separator + iconfor.getName() + ".gif"));
                str.close();
                str = Model.class.getResourceAsStream("icons/Model_s.gif");
                copy(str, new File(target.getAbsolutePath() + File.separator + iconfor.getName() + "_s.gif"));
                str.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copy(InputStream str, File dest) throws IOException {
        byte[] buff = new byte[8192];
        if (dest.exists()) return;
        FileOutputStream fout = new FileOutputStream(dest);
        int read = 0;
        while ((read = str.read(buff)) != -1) {
            fout.write(buff, 0, read);
        }
        fout.flush();
        fout.close();
    }
}
