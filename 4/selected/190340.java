package org.jcvi.trace.frg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.jcvi.Distance;
import org.jcvi.common.util.Range;
import org.jcvi.common.util.Range.CoordinateSystem;
import org.jcvi.glyph.encoder.TigrQualitiesEncodedGyphCodec;
import org.jcvi.glyph.nuc.NucleotideGlyph;
import org.jcvi.io.IOUtil;
import org.jcvi.io.fileServer.DirectoryFileServer;
import org.jcvi.io.fileServer.DirectoryFileServer.ReadWriteDirectoryFileServer;
import org.jcvi.sequence.Library;
import org.jcvi.sequence.Mated;

public class Frg2Writer {

    private static final TigrQualitiesEncodedGyphCodec QUALITY_CODEC = TigrQualitiesEncodedGyphCodec.getINSTANCE();

    private static final String FRG_VERSION_2_TAG = "{VER\nver:2\n}\n";

    private static final String LIBRARY_FORMAT = "{LIB\nact:A\nacc:%s\nori:%s\nmea:%.2f\nstd:%.2f\nsrc:\n.\nnft:0\nfea:\n.\n}\n";

    private static final String FRG_2_FORMAT = "{FRG\nact:A\nacc:%s\nrnd:1\nsta:G\nlib:%s\npla:0\nloc:0\nsrc:\n%s\n.\nseq:\n%s\n.\nqlt:\n%s\n.\nhps:\n.\nclv:%d,%d\nclr:%d,%d\n}\n";

    private static final String LKG_MESSAGE = "{LKG\nact:A\nfrg:%s\nfrg:%s\n}\n";

    public void writeFrg2(final Iterable<Fragment> unmatedFrgs, OutputStream out) throws IOException, InterruptedException, ExecutionException {
        writeFrg2(Collections.<Mated<Fragment>>emptyList(), unmatedFrgs, out);
    }

    /**
     * Create a mated FRG 2 file. 
     * @param matedFrags list of mated fragments to write
     * @param out outputStream to write data to.
     * @throws IOException
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public void writeFrg2(final List<Mated<Fragment>> matedFrags, final Iterable<Fragment> unmatedFrgs, OutputStream out) throws IOException, InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        writeVersion(out);
        final ReadWriteDirectoryFileServer tempDir = DirectoryFileServer.createTemporaryDirectoryFileServer();
        List<Callable<Void>> writers = new ArrayList<Callable<Void>>();
        writers.add(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                OutputStream temp = null;
                try {
                    temp = new FileOutputStream(tempDir.createNewFile("tmp.lib"));
                    writeLibraries(matedFrags, unmatedFrgs, temp);
                    return null;
                } finally {
                    IOUtil.closeAndIgnoreErrors(temp);
                }
            }
        });
        writers.add(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                OutputStream temp = null;
                try {
                    temp = new FileOutputStream(tempDir.createNewFile("tmp.frag"));
                    writeFragments(matedFrags, unmatedFrgs, temp);
                    return null;
                } finally {
                    IOUtil.closeAndIgnoreErrors(temp);
                }
            }
        });
        writers.add(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                OutputStream temp = null;
                try {
                    temp = new FileOutputStream(tempDir.createNewFile("tmp.link"));
                    writeLinkages(matedFrags, temp);
                    return null;
                } finally {
                    IOUtil.closeAndIgnoreErrors(temp);
                }
            }
        });
        for (Future<Void> f : executor.invokeAll(writers)) {
            f.get();
        }
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        appendTempFile(tempDir.getFile("tmp.lib"), out);
        appendTempFile(tempDir.getFile("tmp.frag"), out);
        appendTempFile(tempDir.getFile("tmp.link"), out);
        IOUtil.closeAndIgnoreErrors(out);
        IOUtil.closeAndIgnoreErrors(tempDir);
    }

    private void appendTempFile(File temp, OutputStream out) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(temp);
            IOUtils.copy(in, out);
        } finally {
            IOUtil.closeAndIgnoreErrors(in);
        }
    }

    private void writeLinkages(List<Mated<Fragment>> matedFragsLists, OutputStream out) throws IOException {
        for (Mated<Fragment> matedFrgs : matedFragsLists) {
            writeLinkageMessage(matedFrgs.getMates(), out);
        }
    }

    private void writeLinkageMessage(List<Fragment> mates, OutputStream out) throws IOException {
        out.write(String.format(LKG_MESSAGE, mates.get(0).getId(), mates.get(1).getId()).getBytes());
    }

    private void writeFragments(Iterable<Mated<Fragment>> matedFragsList, Iterable<Fragment> unmatedFrgs, OutputStream out) throws IOException {
        for (Mated<Fragment> matedFrags : matedFragsList) {
            for (Fragment frag : matedFrags.getMates()) {
                writeFrag(frag, out);
            }
        }
        for (Fragment unmatedFrag : unmatedFrgs) {
            writeFrag(unmatedFrag, out);
        }
    }

    private void writeFrag(Fragment frag, OutputStream out) throws IOException {
        Range clearRange = frag.getValidRange().convertRange(CoordinateSystem.SPACE_BASED);
        Range vectorClearRange = frag.getVectorClearRange().convertRange(CoordinateSystem.SPACE_BASED);
        Library library = frag.getLibrary();
        out.write(String.format(FRG_2_FORMAT, frag.getId(), library.getId(), writeSourceComment(frag), NucleotideGlyph.convertToString(frag.getBasecalls().decode()), new String(QUALITY_CODEC.encode(frag.getQualities().decode())), vectorClearRange.getLocalStart(), vectorClearRange.getLocalEnd(), clearRange.getLocalStart(), clearRange.getLocalEnd()).getBytes());
    }

    private String writeSourceComment(Fragment frag) {
        return frag.getComment();
    }

    private void writeVersion(OutputStream out) throws IOException {
        out.write(FRG_VERSION_2_TAG.getBytes());
    }

    private void writeLibraries(Iterable<Mated<Fragment>> matedFrags, Iterable<Fragment> unmatedFrgs, OutputStream out) throws IOException {
        Set<String> seen = new HashSet<String>();
        for (Fragment frag : unmatedFrgs) {
            writeLibraryIfNotYetSeen(out, seen, frag);
        }
        for (Mated<Fragment> mated : matedFrags) {
            for (Fragment frag : mated.getMates()) {
                writeLibraryIfNotYetSeen(out, seen, frag);
            }
        }
    }

    private void writeLibraryIfNotYetSeen(OutputStream out, Set<String> seen, Fragment frag) throws IOException {
        if (!seen.contains(frag.getLibraryId())) {
            writeLibrary(frag.getLibrary(), out);
            seen.add(frag.getLibraryId());
        }
    }

    private void writeLibrary(Library library, OutputStream out) throws IOException {
        final Distance distance = library.getDistance();
        out.write(String.format(LIBRARY_FORMAT, library.getId(), library.getMateOrientation().getCharacter(), distance.getMean(), distance.getStdDev()).getBytes());
    }
}
