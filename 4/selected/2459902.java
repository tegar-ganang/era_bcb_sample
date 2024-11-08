package net.sf.gridarta.model.collectable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import net.sf.gridarta.model.face.ArchFaceProvider;
import net.sf.gridarta.model.face.FaceObject;
import net.sf.gridarta.model.face.FaceObjects;
import net.sf.gridarta.utils.ActionBuilderUtils;
import net.sf.japi.swing.action.ActionBuilder;
import net.sf.japi.swing.action.ActionBuilderFactory;
import net.sf.japi.swing.misc.Progress;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link Collectable} that creates the atrinik.0/crossfire.0/daimonin.0 files
 * which contains all defined faces. It also creates index or editor helper
 * files such as bmaps or bmaps.paths.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 * @author Andreas Kirschbaum
 */
public class FaceObjectsCollectable implements Collectable {

    /**
     * The {@link ActionBuilder} instance.
     */
    private static final ActionBuilder ACTION_BUILDER = ActionBuilderFactory.getInstance().getActionBuilder("net.sf.gridarta");

    /**
     * The {@link FaceObjects} being collected.
     */
    @NotNull
    private final FaceObjects faceObjects;

    /**
     * The {@link ArchFaceProvider} to use for collection.
     */
    @NotNull
    private final ArchFaceProvider archFaceProvider;

    /**
     * Creates a new instance.
     * @param faceObjects the face objects to collect
     * @param archFaceProvider the arch face provider to use
     */
    public FaceObjectsCollectable(@NotNull final FaceObjects faceObjects, @NotNull final ArchFaceProvider archFaceProvider) {
        this.faceObjects = faceObjects;
        this.archFaceProvider = archFaceProvider;
    }

    /**
     * {@inheritDoc} Collects the faces. The graphics information is written to
     * "crossfire.0" resp. "daimonin.0". The meta information (offsets etc.) is
     * written to "bmaps". The tree information for the editor is written to
     * "bmaps.paths" resp. "facetree". <p/> Theoretically it would also be
     * possible to recode the images. But the Java image encoder isn't as good
     * as that of gimp in many cases (yet much better as the old visualtek's).
     */
    @Override
    public void collect(@NotNull final Progress progress, @NotNull final File collectedDirectory) throws IOException {
        collectTreeFile(progress, collectedDirectory);
        collectBmapsFile(progress, collectedDirectory);
        collectImageFile(progress, collectedDirectory);
    }

    /**
     * Creates the image file.
     * @param progress the progress to report progress to
     * @param collectedDirectory the destination directory to collect data to
     * @throws IOException in case of I/O problems during collection
     */
    private void collectImageFile(@NotNull final Progress progress, @NotNull final File collectedDirectory) throws IOException {
        final File file = new File(collectedDirectory, ActionBuilderUtils.getString(ACTION_BUILDER, "configSource.image.name"));
        final FileOutputStream fos = new FileOutputStream(file);
        try {
            final FileChannel outChannel = fos.getChannel();
            try {
                final int numOfFaceObjects = faceObjects.size();
                progress.setLabel(ActionBuilderUtils.getString(ACTION_BUILDER, "archCollectImages"), numOfFaceObjects);
                final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                final Charset charset = Charset.forName("ISO-8859-1");
                int i = 0;
                for (final FaceObject faceObject : faceObjects) {
                    final String face = faceObject.getFaceName();
                    final String path = archFaceProvider.getFilename(face);
                    try {
                        final FileInputStream fin = new FileInputStream(path);
                        try {
                            final FileChannel inChannel = fin.getChannel();
                            final long imageSize = inChannel.size();
                            byteBuffer.clear();
                            byteBuffer.put(("IMAGE " + (faceObjects.isIncludeFaceNumbers() ? i + " " : "") + imageSize + " " + face + "\n").getBytes(charset));
                            byteBuffer.flip();
                            outChannel.write(byteBuffer);
                            inChannel.transferTo(0L, imageSize, outChannel);
                        } finally {
                            fin.close();
                        }
                    } catch (final FileNotFoundException ignored) {
                        ACTION_BUILDER.showMessageDialog(progress.getParentComponent(), "archCollectErrorFileNotFound", path);
                        return;
                    } catch (final IOException e) {
                        ACTION_BUILDER.showMessageDialog(progress.getParentComponent(), "archCollectErrorIOException", path, e);
                        return;
                    }
                    if (i++ % 100 == 0) {
                        progress.setValue(i);
                    }
                }
                progress.setValue(faceObjects.size());
            } finally {
                outChannel.close();
            }
        } finally {
            fos.close();
        }
    }

    /**
     * Creates the tree file.
     * @param progress the progress to report progress to
     * @param collectedDirectory the destination directory to collect data to
     * @throws IOException in case of I/O problems during collection
     */
    private void collectTreeFile(@NotNull final Progress progress, @NotNull final File collectedDirectory) throws IOException {
        collectFile(progress, new File(collectedDirectory, ActionBuilderUtils.getString(ACTION_BUILDER, "configSource.facetree.name")), ActionBuilderUtils.getString(ACTION_BUILDER, "archCollectTree"), ActionBuilderUtils.getString(ACTION_BUILDER, "configSource.facetree.output"));
    }

    /**
     * Creates the bmaps file.
     * @param progress the progress to report progress to
     * @param collectedDirectory the destination directory to collect data to
     * @throws IOException in case of I/O problems during collection
     */
    private void collectBmapsFile(@NotNull final Progress progress, @NotNull final File collectedDirectory) throws IOException {
        collectFile(progress, new File(collectedDirectory, ActionBuilderUtils.getString(ACTION_BUILDER, "configSource.face.name")), ActionBuilderUtils.getString(ACTION_BUILDER, "archCollectBmaps"), ActionBuilderUtils.getString(ACTION_BUILDER, "configSource.face.output"));
    }

    /**
     * Creates an output file containing all faces.
     * @param progress the process to report progress to
     * @param file the output file to write
     * @param label the progress label
     * @param format the format string for writing the output file
     * @throws IOException if an I/O error occurs
     */
    private void collectFile(@NotNull final Progress progress, @NotNull final File file, @NotNull final String label, @NotNull final String format) throws IOException {
        final FileOutputStream fos = new FileOutputStream(file);
        try {
            final OutputStreamWriter osw = new OutputStreamWriter(fos);
            try {
                final BufferedWriter bw = new BufferedWriter(osw);
                try {
                    final int numOfFaceObjects = faceObjects.size();
                    progress.setLabel(label, numOfFaceObjects);
                    int i = 0;
                    for (final FaceObject faceObject : faceObjects) {
                        final String path = faceObject.getPath();
                        final String face = faceObject.getFaceName();
                        bw.append(String.format(format, i, path, face)).append('\n');
                        if (i++ % 100 == 0) {
                            progress.setValue(i);
                        }
                    }
                    progress.setValue(numOfFaceObjects);
                } finally {
                    bw.close();
                }
            } finally {
                osw.close();
            }
        } finally {
            fos.close();
        }
    }
}
