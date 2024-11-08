package org.fudaa.dodico.crue.io;

import com.memoire.bu.BuFileFilter;
import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import javax.swing.filechooser.FileFilter;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.ctulu.fileformat.FileReadOperationAbstract;
import org.fudaa.ctulu.fileformat.FileWriteOperationAbstract;
import org.fudaa.dodico.crue.config.CrueConfigMetier;
import org.fudaa.dodico.crue.io.common.CrueData;
import org.fudaa.dodico.crue.io.common.CrueFileType;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.io.dao.CrueXmlReaderWriter;

/**
 * File format customisé pour Crue. Indique si le fichier est de type crue 9 ou 10. Contient un validator xsd pour le
 * fichier donné. Contient une méthode statique très pratique qui permet de retourner automatiquement le bon FileFormat
 * en fonction du type renvoyé. T correspond à la structure métier associée au format du fichier
 * 
 * @param <M> Represente le modele Metier
 * @author Adrien Hadoux
 */
public class Crue10FileFormat<M> extends CustomFileFormatUnique<CrueIOResu<M>> {

    private final CrueXmlReaderWriter<M> readerWriter;

    /**
   * @param type le type en Upper case comme par exemple DFRT.
   */
    protected Crue10FileFormat(final CrueXmlReaderWriter<M> readerWriter) {
        this(readerWriter.getFileType(), readerWriter);
    }

    @Override
    public BuFileFilter createFileFilter() {
        return new CustomFileFilterExtension(extensions, getDescription());
    }

    public static FileFilter createFileFilter(CrueFileType fileType) {
        return new CustomFileFilterExtension(new String[] { fileType.getExtension() }, fileType.getExtension());
    }

    public static FileFilter createFileFilter(CrueFileType fileType, String description) {
        return new CustomFileFilterExtension(new String[] { fileType.getExtension() }, description);
    }

    /**
   * @param type le type en Upper case comme par exemple DFRT.
   */
    protected Crue10FileFormat(final CrueFileType type, final CrueXmlReaderWriter<M> readerWriter) {
        super(1);
        this.readerWriter = readerWriter;
        nom = type.toString().toLowerCase();
        id = nom;
        extensions = new String[] { type.getExtension() };
        description = nom + ".file";
    }

    @Override
    public String getVersionName() {
        return readerWriter.getVersion();
    }

    /**
   * @return type du fichier supporte.
   */
    public CrueFileType getFileType() {
        return readerWriter.getFileType();
    }

    /**
   * @deprecated
   * @return null
   */
    @Deprecated
    public FileReadOperationAbstract createReader() {
        return null;
    }

    /**
   * @deprecated
   * @return null
   */
    @Deprecated
    public FileWriteOperationAbstract createWriter() {
        return null;
    }

    /**
   * retourne les extensions acceptables par le fichier
   */
    @Override
    public final String[] getExtensions() {
        return extensions;
    }

    public final String getExtension() {
        return extensions[0];
    }

    /**
   * @return le validator xsd.
   */
    public final String getXsdValidator() {
        return readerWriter.getXsdValidator();
    }

    /**
   * Valide la grammaire du fichier XML passé en paramètre
   * 
   * @param xml
   * @param res
   * @return true si le fichier est valide
   */
    public boolean isValide(final File xml, final CtuluLog res) {
        return readerWriter.isValide(xml, res);
    }

    /**
   * Valide la grammaire du fichier XML représenté par un chemin relatif du fichier (contenant son nom) passé en
   * paramètre
   * 
   * @param xml
   * @param res
   * @return true si le fichier est valide
   */
    public boolean isValide(final String xml, final CtuluLog res) {
        return readerWriter.isValide(xml, res);
    }

    /**
   * Valide la grammaire du fichier XML représenté par son URL passée en paramètre
   * 
   * @param xml
   * @param res
   * @return true si le fichier est valide
   */
    public boolean isValide(final URL xml, final CtuluLog res) {
        return readerWriter.isValide(xml, res);
    }

    /**
   * Lit les données dans le fichier f avec les données liées.
   * 
   * @param url
   * @param analyzer
   * @param dataLinked
   * @return M l'objet métier
   */
    public final CrueIOResu<M> read(final URL url, final CtuluLog analyzer, final CrueData dataLinked) {
        return readerWriter.read(url, analyzer, dataLinked);
    }

    /**
   * Lit les données dans le fichier f avec les données liées.
   * 
   * @param f
   * @param analyzer
   * @param dataLinked
   * @return M l'objet métier
   */
    @Override
    public final CrueIOResu<M> read(final File f, final CtuluLog analyzer, final CrueData dataLinked) {
        analyzer.setDesc(f.getName());
        final CrueIOResu<M> result = readerWriter.readXML(f, analyzer, dataLinked);
        decoreResult(f, result);
        return result;
    }

    protected void decoreResult(final File f, CrueIOResu<M> res) {
    }

    /**
   * Lit les données dans le fichier f avec les données liées.
   * 
   * @param pathToResource
   * @param analyzer
   * @param dataLinked
   * @return M l'objet métier
   */
    public final CrueIOResu<M> read(final String pathToResource, final CtuluLog analyzer, final CrueData dataLinked) {
        return readerWriter.readXML(pathToResource, analyzer, dataLinked);
    }

    /**
   * Méthode qui permet d'écrire les datas dans le fichier f spécifié.
   * 
   * @param metier
   * @param f
   * @param analyzer
   * @return true si l'écriture s'est bien passée
   */
    public final boolean write(final CrueIOResu<CrueData> metier, final File f, final CtuluLog analyzer) {
        return readerWriter.writeXML(metier, f, analyzer);
    }

    public final boolean write(final CrueData metier, final File f, final CtuluLog analyzer) {
        return write(new CrueIOResu<CrueData>(metier), f, analyzer);
    }

    public boolean write(final CrueIOResu<CrueData> metier, final OutputStream out, final CtuluLog analyser) {
        return readerWriter.writeXML(metier, out, analyser);
    }

    public final boolean writeMetierDirect(final M metier, final File f, final CtuluLog analyzer, CrueConfigMetier props) {
        return readerWriter.writeXMLMetier(new CrueIOResu<M>(metier), f, analyzer, props);
    }
}
