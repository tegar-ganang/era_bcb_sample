package org.fudaa.dodico.crue.io.neuf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ResourceBundle;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.ctulu.fileformat.FortranInterface;
import org.fudaa.ctulu.fileformat.FortranLib;
import org.fudaa.dodico.crue.io.common.CrueData;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.fichiers.NativeNIOHelperCorrected;
import org.fudaa.dodico.fortran.FileCharSimpleReaderAbstract;

/**
 * Un reader pour les fichiers binaires. Le champs {@link #helper} est initialisé dans cette classe. Les sous-classes
 * doivent implémentées la méthode {@link #internalReadResu()}. La fermeture des flux est prise en charge par cette
 * classe.
 * 
 * @author deniger
 * @param <T> le resultat attendu
 */
public abstract class AbstractCrueBinaryReader<T> extends FileCharSimpleReaderAbstract<CrueIOResu<T>> {

    protected NativeNIOHelperCorrected helper;

    protected FileInputStream in;

    protected File file;

    private CrueData dataLinked;

    @Override
    protected FortranInterface getFortranInterface() {
        return FortranLib.getFortranInterface(in);
    }

    @Override
    public final void setFile(final File _f) {
        file = _f;
        if (analyze_ == null) {
            analyze_ = new CtuluLog();
        }
        analyze_.setDesc(super.getOperationDescription(_f));
        try {
            in = new FileInputStream(file);
            helper = new NativeNIOHelperCorrected(in.getChannel());
            helper.inverseOrder();
        } catch (final FileNotFoundException _e) {
            analyze_.manageException(_e);
            if (in != null) {
                CtuluLibFile.close(in);
            }
        }
    }

    /**
   * @param res le resourceBundle attacée aux messages.
   */
    public void setResourceBundle(final ResourceBundle res) {
        if (analyze_ == null) {
            analyze_ = new CtuluLog(res);
        } else {
            analyze_.setDefaultResourceBundle(res);
        }
    }

    protected abstract T internalReadResu() throws IOException;

    @Override
    protected CrueIOResu<T> internalRead() {
        T res = null;
        try {
            res = internalReadResu();
        } catch (final IOException e) {
            analyze_.manageException(e);
        }
        final CrueIOResu<T> ioResu = new CrueIOResu<T>();
        ioResu.setAnalyse(analyze_);
        ioResu.setMetier(res);
        return ioResu;
    }

    /**
   * @return the dataLinked
   */
    public CrueData getDataLinked() {
        return dataLinked;
    }

    /**
   * @param dataLinked the dataLinked to set
   */
    public void setDataLinked(final CrueData dataLinked) {
        this.dataLinked = dataLinked;
    }
}
