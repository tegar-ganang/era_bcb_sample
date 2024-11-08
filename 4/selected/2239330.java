package org.fudaa.ctulu;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import com.memoire.fu.FuLog;
import org.fudaa.ctulu.CtuluCacheFile;

/**
 * Un container d'image. Utilise des caches d'images (snapshots) pour des 
 * repr�sentations zoom�es.
 * 
 * @author fred deniger
 * @version $Id: CtuluImageContainer.java,v 1.8 2007-04-20 16:20:19 deniger Exp $
 */
public class CtuluImageContainer {

    public static class Snapshot {

        SoftReference<BufferedImage> bigSnapshot_;

        final int defaultBigSnapshotSize_ = 512;

        final int defaultSnapshotSize_ = 256;

        boolean fileFinish_;

        long lastModified_;

        private float ratioBigSnapshot_;

        private float ratioSnapshot_;

        BufferedImage snapshot_;

        final CtuluImageContainer src_;

        /** Si true, utilisation d'un big cache */
        final boolean useBigSnapshot_;

        File tmpBigSnapshot_;

        public Snapshot(final CtuluImageContainer _src, boolean _useBigSnapshot) {
            super();
            src_ = _src;
            useBigSnapshot_ = _useBigSnapshot;
        }

        synchronized void createBigSnapshot() {
            if (!useBigSnapshot_) return;
            new Thread() {

                public void run() {
                    actBigSnap();
                }
            }.start();
        }

        private synchronized BufferedImage createSmallSnapshot() throws IOException {
            updateFileTime();
            final ImageReadParam params = src_.reader_.getDefaultReadParam();
            ratioSnapshot_ = CtuluLibImage.getRatio(src_.w_, src_.h_, defaultSnapshotSize_);
            if (ratioSnapshot_ > 0 && ratioSnapshot_ < 1) {
                final int toSkip = (int) Math.ceil(1 / ratioSnapshot_);
                params.setSourceSubsampling(toSkip, toSkip, 0, 0);
            }
            final BufferedImage im = src_.reader_.read(0, params);
            ratioSnapshot_ = CtuluLibImage.getRatio(im.getWidth(), src_.w_);
            return im;
        }

        private boolean isFileModified() {
            return (src_.file_ != null && src_.file_.lastModified() > lastModified_);
        }

        private void updateFileTime() {
            if (src_.file_ != null) {
                lastModified_ = src_.file_.lastModified();
            }
        }

        protected synchronized void actBigSnap() {
            final float ratio = CtuluLibImage.getRatio(src_.w_, src_.h_, defaultBigSnapshotSize_);
            final BufferedImage im = src_.getImage(new Rectangle(0, 0, src_.w_, src_.h_), ratio, ratio, false);
            if (isFileModified() && snapshot_ != null) {
                snapshot_ = null;
            }
            updateFileTime();
            setBigSnapshot(new SoftReference<BufferedImage>(im), CtuluLibImage.getRatio(im.getWidth(), src_.w_));
            ImageWriter writer = null;
            writer = ImageIO.getImageWriter(src_.reader_);
            setFileFinish(false);
            File f = tmpBigSnapshot_;
            if (f == null) {
                try {
                    f = CtuluCacheFile.createTempFile("img512", src_.file_.getName());
                } catch (final IOException _evt) {
                    FuLog.error(_evt);
                }
            }
            if (f == null) {
                tmpBigSnapshot_ = null;
            } else {
                boolean ok = true;
                if (writer == null) {
                    if (tmpBigSnapshot_ != null) {
                        tmpBigSnapshot_.delete();
                    }
                    f = new File(f.getAbsolutePath() + ".png");
                    writer = CtuluLibImage.getImageWriter(f);
                } else {
                    try {
                        writer.setOutput(ImageIO.createImageOutputStream(f));
                    } catch (final IOException _evt) {
                        ok = false;
                        FuLog.error(_evt);
                    }
                }
                if (ok) {
                    try {
                        writer.write(im);
                    } catch (final IOException _evt) {
                        FuLog.error(_evt);
                        ok = false;
                    }
                }
                if (ok) {
                    tmpBigSnapshot_ = f;
                } else {
                    if (tmpBigSnapshot_ != null) {
                        tmpBigSnapshot_.delete();
                    }
                    tmpBigSnapshot_ = null;
                }
            }
            setFileFinish(true);
        }

        protected synchronized void setBigSnapshot(final SoftReference<BufferedImage> _im, final float _ratio) {
            bigSnapshot_ = _im;
            ratioBigSnapshot_ = _ratio;
        }

        public synchronized BufferedImage getBigSnapshot() {
            BufferedImage res = null;
            if (bigSnapshot_ != null) {
                if (isFileModified()) {
                    snapshot_ = null;
                    createBigSnapshot();
                    return null;
                }
                final Object o = bigSnapshot_.get();
                if (o != null) {
                    res = (BufferedImage) o;
                } else if (tmpBigSnapshot_ != null && tmpBigSnapshot_.exists() && isFileFinish()) {
                    try {
                        res = ImageIO.read(tmpBigSnapshot_);
                        bigSnapshot_ = new SoftReference<BufferedImage>(res);
                    } catch (final IOException _evt) {
                        FuLog.error(_evt);
                    }
                }
            } else {
                createBigSnapshot();
            }
            return res;
        }

        public float getRatioSnapshot() {
            return ratioSnapshot_;
        }

        public BufferedImage getSmallSnapshot() {
            if (snapshot_ == null || isFileModified()) {
                try {
                    snapshot_ = createSmallSnapshot();
                } catch (final IOException _evt) {
                    FuLog.error(_evt);
                }
            }
            if (isFileModified() && bigSnapshot_ != null) {
                bigSnapshot_ = null;
                createBigSnapshot();
            }
            return snapshot_;
        }

        public synchronized boolean isFileFinish() {
            return fileFinish_;
        }

        public void resetCache() {
            snapshot_ = null;
            setBigSnapshot(null, 0);
            if (tmpBigSnapshot_ != null) {
                tmpBigSnapshot_.delete();
                tmpBigSnapshot_ = null;
            }
        }

        public synchronized void setFileFinish(final boolean _fileFinish) {
            fileFinish_ = _fileFinish;
        }

        public float getRatioBigSnapshot() {
            return ratioBigSnapshot_;
        }
    }

    final File file_;

    final int h_;

    final int imageIdx_ = 0;

    final ImageReader reader_;

    final Snapshot snapshot_;

    final int tileSize_ = 512;

    final int w_;

    public CtuluImageContainer(final File _file) {
        this(CtuluLibImage.getImageReader(_file, null), _file);
    }

    public CtuluImageContainer(final ImageReader _reader, final File _f) {
        this(_reader, _f, true);
    }

    /**
   * Cree le container avec ou sans bigSnapshot. Le bigSnapshot �tant cr�e dans un thread
   * s�par�, cela peut poser probl�me si on accede � l'image en m�me temps que le thread 
   * de cr�ation.
   * 
   * @param _reader Le reader associ� � l'image.
   * @param _f Le fichier image.
   * @param _useBigSnapshot True : Creation d'un big snapshot (d�faut)
   */
    public CtuluImageContainer(final ImageReader _reader, final File _f, boolean _useBigSnapshot) {
        super();
        file_ = _f;
        reader_ = _reader;
        if (reader_ == null) {
            w_ = 0;
            h_ = 0;
            snapshot_ = null;
            return;
        }
        int hRead = 0;
        int wRead = 0;
        try {
            hRead = reader_.getHeight(imageIdx_);
            wRead = reader_.getWidth(imageIdx_);
        } catch (final IOException _evt) {
            FuLog.error(_evt);
        }
        w_ = wRead;
        h_ = hRead;
        snapshot_ = new Snapshot(this, _useBigSnapshot);
        snapshot_.createBigSnapshot();
    }

    private boolean canUseBigSnapshot(final float _ratioX, final float _ratioY) {
        return canUseSnapshot(snapshot_.getBigSnapshot(), snapshot_.getRatioBigSnapshot(), _ratioX, _ratioY);
    }

    private boolean canUseSmallSnapshot(final float _ratioX, final float _ratioY) {
        if (snapshot_ == null) return false;
        return canUseSnapshot(snapshot_.getSmallSnapshot(), snapshot_.getRatioSnapshot(), _ratioX, _ratioY);
    }

    public boolean isImageLoaded() {
        return reader_ != null && snapshot_ != null;
    }

    private boolean canUseSnapshot(final BufferedImage _im, final float _ratioSnap, final float _ratioX, final float _ratioY) {
        return _im != null && _ratioSnap > 0 && _ratioX <= _ratioSnap && _ratioY <= _ratioSnap;
    }

    private BufferedImage createFromBigSnapshot(final float _ratioX, final float _ratioY, final Rectangle _source) {
        return getImageFromSnapshot(_ratioX, _ratioY, _source, snapshot_.getRatioBigSnapshot(), snapshot_.getBigSnapshot());
    }

    private BufferedImage createFromSmallSnapshot(final float _ratioX, final float _ratioY, final Rectangle _source) {
        return getImageFromSnapshot(_ratioX, _ratioY, _source, snapshot_.getRatioSnapshot(), snapshot_.getSmallSnapshot());
    }

    private BufferedImage getImageFromSnapshot(final float _ratioX, final float _ratioY, final Rectangle _source, final float _ratioSnap, final BufferedImage _imSnap) {
        BufferedImage res;
        res = new BufferedImage((int) Math.ceil(_source.width * _ratioX), (int) Math.ceil(_source.height * _ratioY), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = res.createGraphics();
        g2d.getRenderingHints().put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        final int xInSnapshot = (int) Math.floor(_source.x * _ratioSnap);
        final int yInSnapshot = (int) Math.floor(_source.y * _ratioSnap);
        int wInSnap = (int) Math.ceil(_source.width * _ratioSnap);
        wInSnap = Math.min(wInSnap, _imSnap.getWidth() - xInSnapshot);
        int hInSnap = (int) Math.ceil(_source.height * _ratioSnap);
        hInSnap = Math.min(hInSnap, _imSnap.getHeight() - yInSnapshot);
        final BufferedImage subimage = _imSnap.getSubimage(xInSnapshot, yInSnapshot, wInSnap, hInSnap);
        g2d.drawImage(subimage, 0, 0, res.getWidth(), res.getHeight(), null);
        subimage.flush();
        g2d.dispose();
        return res;
    }

    private BufferedImage readInFile(final float _ratioX, final float _ratioY, final Rectangle _source) {
        BufferedImage res;
        final ImageReadParam params = reader_.getDefaultReadParam();
        final int nbXTiled = (int) Math.ceil(CtuluLibImage.getRatio(_source.width, tileSize_));
        final int nbYTiled = (int) Math.ceil(CtuluLibImage.getRatio(_source.height, tileSize_));
        final Rectangle r = new Rectangle();
        res = new BufferedImage((int) Math.ceil(_source.width * _ratioX), (int) Math.ceil(_source.height * _ratioY), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = res.createGraphics();
        if (_ratioX > 0.8 || _ratioY > 0.8) {
            g2d.getRenderingHints().put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.getRenderingHints().put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        } else {
            g2d.getRenderingHints().put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        for (int i = 0; i < nbXTiled; i++) {
            r.width = Math.min(tileSize_, _source.width - i * tileSize_);
            r.x = _source.x + i * tileSize_;
            for (int j = 0; j < nbYTiled; j++) {
                r.height = Math.min(tileSize_, _source.height - j * tileSize_);
                r.y = _source.y + j * tileSize_;
                params.setSourceRegion(r);
                try {
                    final BufferedImage tmp = reader_.read(imageIdx_, params);
                    g2d.drawImage(tmp, (int) Math.floor(i * tileSize_ * _ratioX), (int) Math.floor(j * tileSize_ * _ratioY), (int) Math.ceil(r.width * _ratioX), (int) Math.ceil(r.height * _ratioY), null);
                    tmp.flush();
                } catch (final IOException _evt) {
                    FuLog.error(_evt);
                }
            }
        }
        g2d.dispose();
        return res;
    }

    private BufferedImage readInFileSampling(final float _ratioX, final float _ratioY, final Rectangle _source) {
        BufferedImage sampling = null;
        final ImageReadParam params = reader_.getDefaultReadParam();
        params.setSourceRegion(new Rectangle(_source.x, _source.y, _source.width, _source.height));
        if (_ratioX < 1 || _ratioY < 1) {
            final int toSkipX = _ratioX < 1 ? (int) Math.ceil(1 / _ratioX) : 1;
            final int toSkipY = _ratioY < 1 ? (int) Math.ceil(1 / _ratioY) : 1;
            params.setSourceSubsampling(toSkipX, toSkipY, 0, 0);
        }
        try {
            sampling = reader_.read(0, params);
        } catch (final IOException _evt) {
            FuLog.error(_evt);
        }
        if (sampling != null && (_ratioX > 1 || _ratioY > 1)) {
            sampling = CtuluLibImage.resize(sampling, (int) (_source.width * _ratioX), (int) (_source.height * _ratioY));
        }
        return sampling;
    }

    public BufferedImage getAvailableSnapshot() {
        final BufferedImage res = snapshot_.getBigSnapshot();
        return res == null ? getSnapshot() : res;
    }

    public BufferedImage getBigSnapshot() {
        return snapshot_.getBigSnapshot();
    }

    public int getDefaultSnapshotSize() {
        return snapshot_.defaultSnapshotSize_;
    }

    public File getFile() {
        return file_;
    }

    public BufferedImage getImage(final Rectangle _source, final float _ratioX, final float _ratioY) {
        return getImage(_source, _ratioX, _ratioY, true);
    }

    public BufferedImage getImage(final Rectangle _source, final float _ratioX, final float _ratioY, final boolean _useSnapshot) {
        final Rectangle source = new Rectangle(_source);
        source.width = Math.min(source.width, w_ - source.x);
        source.height = Math.min(source.height, h_ - source.y);
        BufferedImage res = null;
        if (_useSnapshot && canUseSmallSnapshot(_ratioX, _ratioY)) {
            res = createFromSmallSnapshot(_ratioX, _ratioY, source);
        } else if (_useSnapshot && canUseBigSnapshot(_ratioX, _ratioY)) {
            res = createFromBigSnapshot(_ratioX, _ratioY, source);
        } else if ((_ratioX > 0.6 || _ratioY > 0.6)) {
            res = readInFile(_ratioX, _ratioY, source);
        } else {
            res = readInFileSampling(_ratioX, _ratioY, source);
        }
        _source.setBounds(source);
        return res;
    }

    public int getImageHeight() {
        return h_;
    }

    public int getImageWidth() {
        return w_;
    }

    public BufferedImage getSnapshot() {
        return (snapshot_ == null) ? null : snapshot_.getSmallSnapshot();
    }

    public boolean isFileFound() {
        return file_ != null && file_.exists();
    }

    /**
   * Dessine l'image de pr�visualisation dans un carr� de _w, _h en centrant l'image.
   * 
   * @param _dest le graphics a utilse
   * @param _w la largeur disponible
   * @param _h la hauteur disponible
   */
    public void paintSnapshot(final Graphics2D _dest, final int _w, final int _h) {
        final BufferedImage im = getSnapshot();
        if (im == null) {
            return;
        }
        final int width = im.getWidth();
        final int height = im.getHeight();
        final double ratio = CtuluLibImage.getRatio(width, height, Math.min(_w, _h));
        final int wFinal = (int) (ratio * width);
        final int hFinal = (int) (ratio * height);
        final int x = (_w - wFinal) / 2;
        final int y = (_h - hFinal) / 2;
        _dest.drawImage(im, x, y, wFinal, hFinal, null);
    }

    public void resetCache() {
        if (snapshot_ != null) {
            snapshot_.resetCache();
        }
    }
}
