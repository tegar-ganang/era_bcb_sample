package net.sf.filePiper.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import net.sf.filePiper.gui.SizeAndUnitEditor;
import net.sf.filePiper.model.ExecutionPhase;
import net.sf.filePiper.model.FileProcessor;
import net.sf.filePiper.model.FileProcessorEnvironment;
import net.sf.filePiper.model.InputFileInfo;
import net.sf.filePiper.model.StatusHolder;
import net.sf.sfac.gui.editor.ObjectEditor;
import net.sf.sfac.setting.Settings;

/**
 * Processor chunking an input file in a set of output files of fixed size (calculated in lines or bytes).
 * 
 * @author BEROL
 * 
 */
public class ChunkProcessor implements FileProcessor, SizeAndUnit {

    private static final String CHUNK_SIZE = "chunk.size";

    private static final String CHUNK_UNITS = "chunk.units";

    private Settings setts;

    private StatusHolder holder = new StatusHolder() {

        @Override
        protected String getRunningMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append("Chunking ");
            appendCount(getInputFileCount(), "file", sb);
            sb.append(" in ");
            sb.append(getOutputFileCount());
            sb.append("...");
            return sb.toString();
        }

        @Override
        protected String getDoneMessage() {
            StringBuilder sb = new StringBuilder();
            appendCount(getInputFileCount(), "file", sb);
            sb.append(" chunked in ");
            sb.append(getOutputFileCount());
            if (getInputFileCount() > 1) {
                sb.append(" (");
                appendCount(getOutputFileCount() / getInputFileCount(), "chunk", sb);
                sb.append(" per file)");
            }
            sb.append(".");
            return sb.toString();
        }
    };

    public String getProcessorName() {
        return "Chunk";
    }

    public int getSize() {
        return setts.getIntProperty(CHUNK_SIZE, 100);
    }

    public void setSize(int newSize) {
        setts.setIntProperty(CHUNK_SIZE, newSize);
    }

    public int getUnits() {
        return setts.getIntProperty(CHUNK_UNITS, UNIT_LINE);
    }

    public void setUnits(int newUnits) {
        setts.setIntProperty(CHUNK_UNITS, newUnits);
    }

    public ObjectEditor getEditor() {
        return new SizeAndUnitEditor("Chunk the input file into fixed-size files", "Chunk size");
    }

    public int getOutputCardinality(int inputCardinality) {
        return MANY;
    }

    public void init(Settings settngs) {
        setts = settngs;
    }

    public void process(InputStream is, InputFileInfo info, FileProcessorEnvironment env) throws IOException {
        if (getUnits() == UNIT_LINE) chunkInLines(is, info, env); else chunkInBytes(is, info, env);
    }

    public void chunkInLines(InputStream is, InputFileInfo info, FileProcessorEnvironment env) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        holder.inputFileStarted();
        int chunkCount = 0;
        int chunkSize = getSize();
        int linesInChunk = 0;
        String initialName = info.getProposedName();
        BufferedWriter bw = null;
        String line;
        while (((line = br.readLine()) != null) && env.shouldContinue()) {
            if (bw == null) {
                info.setProposedName(getChunkName(initialName, chunkCount));
                OutputStream os = env.getOutputStream(info);
                bw = new BufferedWriter(new OutputStreamWriter(os));
                holder.outputFileStarted();
                chunkCount++;
                linesInChunk = 0;
            }
            bw.write(line);
            bw.newLine();
            linesInChunk++;
            if (linesInChunk >= chunkSize) {
                bw.close();
                bw = null;
            }
        }
        if (bw != null) bw.close();
    }

    private String getChunkName(String initialName, int index) {
        StringBuffer sb = new StringBuffer(initialName);
        sb.append('-');
        if (index < 10) sb.append('0');
        if (index < 100) sb.append('0');
        sb.append(index);
        return sb.toString();
    }

    public void chunkInBytes(InputStream is, InputFileInfo info, FileProcessorEnvironment env) throws IOException {
        holder.inputFileStarted();
        int chunkCount = 0;
        int chunkSize = getSize();
        int bufferSize = (chunkSize > 1024) ? 1024 : chunkSize;
        int bytesInChunk = 0;
        String initialName = info.getProposedName();
        int readByteCount;
        byte[] buffer = new byte[bufferSize];
        OutputStream os = getOutputStream(info, env, chunkCount, initialName);
        holder.outputFileStarted();
        while (((readByteCount = is.read(buffer)) >= 0) && env.shouldContinue()) {
            if (bytesInChunk + readByteCount > chunkSize) {
                int i = chunkSize - bytesInChunk;
                os.write(buffer, 0, i);
                os.close();
                chunkCount++;
                os = getOutputStream(info, env, chunkCount, initialName);
                holder.outputFileStarted();
                bytesInChunk = readByteCount - i;
                os.write(buffer, i, bytesInChunk);
            } else {
                os.write(buffer, 0, readByteCount);
                bytesInChunk += readByteCount;
            }
        }
        if (os != null) os.close();
    }

    private OutputStream getOutputStream(InputFileInfo info, FileProcessorEnvironment env, int chunkCount, String initialName) throws IOException {
        OutputStream os;
        info.setProposedName(getChunkName(initialName, chunkCount));
        os = env.getOutputStream(info);
        return os;
    }

    public void startBatch(FileProcessorEnvironment env) {
        holder.reset(ExecutionPhase.STARTING);
    }

    public void endBatch(FileProcessorEnvironment env) {
        holder.setCurrentPhase(env.getCurrentPhase());
    }

    public String getStatusMessage() {
        return holder.getStatusMessage();
    }
}
