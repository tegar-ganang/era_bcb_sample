package org.atlantal.impl.cms.field;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.atlantal.api.app.db.Query;
import org.atlantal.api.app.db.QueryException;
import org.atlantal.api.app.db.QueryResult;
import org.atlantal.api.cms.data.MapContentData;
import org.atlantal.api.cms.field.Field;
import org.atlantal.api.cms.field.FieldType;
import org.atlantal.api.cms.util.ContentAccessMode;
import org.atlantal.api.cms.util.FileInfo;
import org.atlantal.api.cms.util.ImageInfo;
import org.atlantal.utils.AtlantalImage;
import org.atlantal.utils.ImageInformation;
import org.atlantal.utils.Utils;

/**
 * <p>Titre : Atlantal Framework</p>
 * <p>Description : </p>
 * <p>Copyright : Copyright (c) 2001-2002</p>
 * <p>Soci�t� : Mably Multim�dia</p>
 * @author Fran�ois MASUREL
 * @version 1.0
 */
public class FieldImage extends FieldFile {

    private static final String CONVERT_PATH = System.getenv("IM_HOME");

    private static final String CONVERT_EXEC = "convert";

    private static final Logger LOGGER = Logger.getLogger(FieldImage.class);

    static {
        LOGGER.setLevel(Level.DEBUG);
    }

    private static FieldType singleton = new FieldImage();

    /**
     * Constructor
     */
    protected FieldImage() {
    }

    /**
     * @return itemtype
     */
    public static FieldType getInstance() {
        return singleton;
    }

    /**
     * {@inheritDoc}
     */
    protected FileInfo newFileInfo() {
        return new ImageInfo();
    }

    /**
     * {@inheritDoc}
     */
    public void xml(Field field, StringBuilder xml, MapContentData values) {
        if (values != null) {
            String valalias = field.getValueAlias();
            FileInfo info = (FileInfo) values.get(valalias + "_info");
            if (info != null) {
                String type = info.getExtension();
                String vpath = info.getVirtualPath();
                xml.append("<file type=\"" + type + "\" vpath=");
                xml.append("\"" + vpath + "\" format=\"normal\"");
                xml.append(" alternative=\"\"/>");
            }
        }
    }

    /** (non-Javadoc)
     * {@inheritDoc}
     */
    public void querySelect(Field field, Query query, String source, String fieldname, String fieldalias, ContentAccessMode cam) {
        super.querySelect(field, query, source, fieldname, fieldalias, cam);
        String tmpfieldsource = fieldname + "_width";
        String tmpfieldalias = fieldalias + "_width";
        query.addSelect(source, tmpfieldsource, tmpfieldalias, true);
        tmpfieldsource = fieldname + "_height";
        tmpfieldalias = fieldalias + "_height";
        query.addSelect(source, tmpfieldsource, tmpfieldalias, true);
    }

    /**
     * {@inheritDoc}
     */
    public void queryValues(Field field, QueryResult result, MapContentData values, String fieldalias) throws QueryException {
        super.queryValues(field, result, values, fieldalias);
        String valalias = field.getValueAlias();
        ImageInfo info = (ImageInfo) values.get(valalias + "_info");
        if (info != null) {
            info.setWidth(result.getInt(fieldalias + "_width"));
            info.setHeight(result.getInt(fieldalias + "_height"));
        }
    }

    /** (non-Javadoc)
     * {@inheritDoc}
     */
    public String sqlInsertFields(Field field, String fieldname) {
        StringBuilder sql = new StringBuilder();
        sql.append(super.sqlInsertFields(field, fieldname));
        sql.append(", ").append(fieldname).append("_width");
        sql.append(", ").append(fieldname).append("_height");
        return sql.toString();
    }

    /** (non-Javadoc)
     * {@inheritDoc}
     */
    public String sqlInsertValues(Field field, MapContentData values) {
        StringBuilder sql = new StringBuilder();
        sql.append(super.sqlInsertValues(field, values));
        String valalias = field.getValueAlias();
        Object filedelete = values.get(valalias + "_delete");
        if (filedelete == null) {
            ImageInfo info = (ImageInfo) values.get(valalias + "_info");
            int width = info.getWidth();
            int height = info.getHeight();
            sql.append(", ").append(width).append(", ").append(height);
        } else {
            sql.append(", NULL, NULL");
        }
        return sql.toString();
    }

    /** (non-Javadoc)
     * {@inheritDoc}
     */
    public String sqlUpdate(Field field, String fieldname, MapContentData values) {
        String valalias = field.getValueAlias();
        Object filedelete = values.get(valalias + "_delete");
        StringBuilder sql = new StringBuilder();
        if (filedelete == null) {
            ImageInfo info = (ImageInfo) values.get(valalias + "_info");
            int width = info.getWidth();
            int height = info.getHeight();
            sql.append(super.sqlUpdate(field, fieldname, values));
            sql.append(", ");
            sql.append(fieldname).append("_width = ").append(width);
            sql.append(", ");
            sql.append(fieldname).append("_height = ").append(height);
        } else {
            sql.append(super.sqlUpdate(field, fieldname, values));
            sql.append(", ").append(fieldname).append("_width = NULL");
            sql.append(", ").append(fieldname).append("_height = NULL");
        }
        return sql.toString();
    }

    /** (non-Javadoc)
     * {@inheritDoc}
     */
    public String sqlCreateFields(Field field, String fieldname) {
        StringBuilder sql = new StringBuilder();
        sql.append(super.sqlCreateFields(field, fieldname));
        sql.append(", ");
        if (fieldname == null) {
            sql.append("`").append(field.getName()).append("_width`");
        } else {
            sql.append("`").append(fieldname).append("_width`");
        }
        sql.append(" int(10) unsigned default NULL");
        sql.append(", ");
        if (fieldname == null) {
            sql.append("`").append(field.getName()).append("_height`");
        } else {
            sql.append("`").append(fieldname).append("_height`");
        }
        sql.append(" int(10) unsigned default NULL");
        return sql.toString();
    }

    /** (non-Javadoc)
     * {@inheritDoc}
     */
    public String sqlCreateIndexes(Field field, String fieldname) {
        return super.sqlCreateIndexes(field, fieldname);
    }

    /**
     * {@inheritDoc}
     */
    protected void prepareFile(Field field, MapContentData values, String valalias) {
        ImageInfo fi = (ImageInfo) values.get(valalias + "_info");
        if (fi != null) {
            String ext = fi.getExtension();
            File file = fi.getFile();
            if (file.exists()) {
                try {
                    File workFile = null;
                    AtlantalImage imgTmp;
                    AtlantalImage imgWrk = null;
                    String maxsizestr = field.getParameter("maxsize");
                    if (maxsizestr != null) {
                        int maxsize = Integer.valueOf(maxsizestr).intValue();
                        try {
                            workFile = File.createTempFile("img", "." + ext);
                            scale(file, workFile, maxsize);
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }
                    }
                    String copyright = field.getParameter("copyright");
                    if (copyright != null) {
                        String cptext = (String) values.get(valalias + "_copyright");
                        if (cptext == null) {
                            cptext = field.getParameter("copyrightdefault");
                        }
                        if (cptext != null) {
                            imgWrk = getAtlantalImage(imgWrk, workFile, file);
                            try {
                                imgTmp = imgWrk;
                                imgWrk = imgTmp.drawCopyright(cptext, true);
                                imgTmp.close();
                            } catch (Exception e) {
                                e.printStackTrace(System.out);
                            }
                        }
                    }
                    String watermark = field.getParameter("watermark");
                    if (watermark != null) {
                        imgWrk = getAtlantalImage(imgWrk, workFile, file);
                        try {
                            imgTmp = imgWrk;
                            imgWrk = imgTmp.drawWatermark(watermark);
                            imgTmp.close();
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }
                    }
                    File f;
                    if (imgWrk != null) {
                        String type = fi.getType();
                        if ("image/x-png".equals(type)) {
                            f = File.createTempFile("img", ".png");
                            imgWrk.saveAsPNG(f);
                        } else if ("image/gif".equals(type)) {
                            f = File.createTempFile("img", ".gif");
                            imgWrk.saveAsGIF(f);
                        } else {
                            f = File.createTempFile("img", ".jpg");
                            imgWrk.saveAsJPEG(f);
                        }
                        if ((workFile != null) && workFile.exists()) {
                            workFile.delete();
                        }
                    } else {
                        f = File.createTempFile("img", "." + ext);
                        if (workFile != null) {
                            f.delete();
                            workFile.renameTo(f);
                        } else {
                            FileUtils.copyFile(file, f);
                        }
                    }
                    fi.setFile(f);
                    fi.setSize(f.length());
                    if (imgWrk != null) {
                        fi.setWidth(imgWrk.getWidth());
                        fi.setHeight(imgWrk.getHeight());
                        imgWrk.close();
                    } else {
                        ImageInformation ii = new ImageInformation();
                        RandomAccessFile in = new RandomAccessFile(f, "r");
                        ii.setInput(in);
                        if (ii.check()) {
                            fi.setWidth(ii.getWidth());
                            fi.setHeight(ii.getHeight());
                        }
                        in.close();
                    }
                    LOGGER.debug("CREATE >> " + f.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }

    private static AtlantalImage getAtlantalImage(AtlantalImage pImgWrk, File workFile, File tempFile) {
        AtlantalImage imgWrk = pImgWrk;
        if (imgWrk == null) {
            if (workFile == null) {
                imgWrk = new AtlantalImage(tempFile);
            } else {
                imgWrk = new AtlantalImage(workFile);
            }
        }
        return imgWrk;
    }

    private static void scale(File in, File out, int max) {
        convertIM(in, out, max, max, 90);
    }

    /**
     * Uses a Runtime.exec()to use imagemagick to perform the given conversion
     * operation. Returns true on success, false on failure. Does not check if
     * either file exists.
     *
     * @param in Description of the Parameter
     * @param out Description of the Parameter
     * @param newSize Description of the Parameter
     * @param quality Description of the Parameter
     * @return Description of the Return Value
     */
    private static boolean convertIM(File in, File out, int width, int height, int pQuality) {
        int quality = pQuality;
        if (quality < 0 || quality > 100) {
            quality = 75;
        }
        ArrayList command = new ArrayList(10);
        StringBuilder cmd = new StringBuilder();
        synchronized (CONVERT_PATH) {
            if (CONVERT_PATH != null) {
                cmd.append(CONVERT_PATH).append(File.separator);
            }
            cmd.append(CONVERT_EXEC);
        }
        command.add(cmd.toString());
        command.add("-size");
        command.add((width * 2) + "x" + (height * 2));
        command.add("-filter");
        command.add("Lanczos");
        command.add("-support");
        command.add("0.8");
        command.add("-resize");
        command.add("\"" + width + "x" + height + ">\"");
        command.add("-strip");
        command.add("-quality");
        command.add("" + quality);
        command.add(in.getAbsolutePath());
        command.add(out.getAbsolutePath());
        LOGGER.debug(command);
        return Utils.exec((String[]) command.toArray(new String[1]));
    }
}
