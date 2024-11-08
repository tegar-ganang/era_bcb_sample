package org.springframework.beandoc.output;

import java.io.*;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.springframework.beandoc.util.BeanDocUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link DocumentCompiler} interface. This class
 * invokes the GraphViz binary (where suitably configured) to create the graphs
 * and image map HTML from the generated .dot files. Further, it copies media
 * resources from the BeanDoc jar file to the output location.
 * 
 * @author Darren Davison
 * @since 1.0
 */
public class DocumentCompilerImpl implements DocumentCompiler {

    private static final String MAP_MARKER = "## imagemap ##";

    private static final String MEDIA_RESOURCES = "classpath:/org/springframework/beandoc/output/media/*";

    private FilenameStrategy filenameStrategy;

    private String dotFileExtension = ".dot";

    private String dotFileMapFormat = "cmapx";

    protected final Log logger = LogFactory.getLog(getClass());

    private String graphOutputType = "png";

    private String dotExe;

    private boolean removeDotFiles = true;

    /**
	 * Generates actual images and HTML image maps (as required) from the dot
	 * files created by DotFileTransformer. Subsequently plugs the image maps
	 * into placeholders in the graph html files.
	 * 
	 * @see org.springframework.beandoc.output.DocumentCompiler#compile(Document[],
	 * File)
	 */
    public void compile(Document[] contextDocuments, File outputDir) {
        String consolidatedImage = contextDocuments[0].getRootElement().getAttributeValue(GraphVizDecorator.ATTRIBUTE_GRAPH_CONSOLIDATED);
        this.graphOutputType = StringUtils.unqualify(consolidatedImage);
        List dotFileList = BeanDocUtils.listFilesRecursively(outputDir, new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getName().endsWith(dotFileExtension);
            }
        });
        logger.info("Generating [" + graphOutputType + "] graphs in [" + outputDir.getAbsolutePath() + "]");
        if (dotExe != null && new File(dotExe).isFile()) for (int i = 0; i < dotFileList.size(); i++) {
            File dotFile = (File) dotFileList.get(i);
            logger.debug("Running dot on [" + dotFile.getAbsolutePath() + "]");
            runDot(dotFile, graphOutputType, graphOutputType);
            File mapFile = runDot(dotFile, dotFileMapFormat, "map");
            plugMap(mapFile, outputDir.getAbsolutePath().equals(dotFile.getParentFile().getAbsolutePath()));
            if (removeDotFiles) {
                dotFile.delete();
                mapFile.delete();
            }
        } else logger.warn("GraphViz 'dot' executable not set or couldn't be found.  No graphs generated.");
        copyMediaResources(outputDir);
    }

    /**
	 * @param dotFile the .dot format file to compile
	 * @param outputType the output parameter for the Dot binary to determine
	 * output format
	 * @param fileExt the filename extension of the output file that Dot should
	 * generate
	 * @return the output file created by Dot
	 */
    private File runDot(File dotFile, String outputType, String fileExt) {
        String dotFileName = dotFile.getAbsolutePath();
        File outputFile = new File(StringUtils.replace(dotFileName, dotFileExtension, "." + fileExt));
        String[] cmd = new String[4];
        cmd[0] = dotExe;
        cmd[1] = "-T" + outputType;
        cmd[2] = "-o" + outputFile.getAbsolutePath();
        cmd[3] = dotFile.getAbsolutePath();
        try {
            logger.debug("Generating [" + outputFile.getAbsolutePath() + "] from [" + dotFileName + "]");
            if (logger.isDebugEnabled()) {
                String dbug = "   ... using command line: [" + dotExe + " ";
                for (int i = 0; i < cmd.length; i++) dbug += cmd[i] + " ";
                logger.debug(dbug + "]");
            }
            Process dot = Runtime.getRuntime().exec(cmd);
            dot.waitFor();
            logger.debug("Process exited with value [" + dot.exitValue() + "]");
        } catch (IOException ioe) {
            logger.warn("Problem attempting to create [" + outputFile + "] from dot file [" + dotFileName + "]; " + ioe.getMessage());
        } catch (InterruptedException e) {
        }
        return outputFile;
    }

    /**
	 * @param mapFile
	 */
    private void plugMap(File mapFile, boolean isInRootDir) {
        logger.debug("Plugging map file [" + mapFile.getAbsolutePath() + "]");
        File graphFile = new File(StringUtils.replace(mapFile.getAbsolutePath(), ".map", filenameStrategy.getFileName("")));
        StringBuffer map = new StringBuffer(256);
        StringBuffer doc = new StringBuffer(512);
        FileWriter writer = null;
        try {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(mapFile));
            while ((line = reader.readLine()) != null) {
                String append = isInRootDir ? line : line.replaceAll("href=\"[^#]*(?<!#)(\\\\|/)(.*)", "href=\"$1");
                map.append(append).append("\n");
            }
            reader = new BufferedReader(new FileReader(graphFile));
            while ((line = reader.readLine()) != null) if (line.indexOf(MAP_MARKER) > -1) doc.append(map).append("\n"); else doc.append(line).append("\n");
            writer = new FileWriter(graphFile);
            writer.write(doc.toString());
            writer.flush();
        } catch (FileNotFoundException e) {
            logger.warn("Unable to find either [" + graphFile.getAbsolutePath() + "] or [" + mapFile.getAbsolutePath() + "] - graphing output probably not configured.");
        } catch (IOException e) {
            logger.warn("Unable to generate documentation from [" + graphFile.getAbsolutePath() + "] and [" + mapFile.getAbsolutePath() + "]", e);
        } finally {
            if (writer != null) try {
                writer.close();
            } catch (IOException e) {
                logger.error("FAILED TO CLOSE OUTPUT STREAM FOR [" + graphFile.getAbsolutePath() + "]", e);
            }
        }
    }

    /**
	 * Writes the media files to the output location.
	 */
    private void copyMediaResources(File outputDir) {
        logger.debug("Copying media resources to output location");
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(new DefaultResourceLoader());
            Resource[] media = resolver.getResources(MEDIA_RESOURCES);
            for (int i = 0; i < media.length; i++) {
                File target = new File(outputDir, media[i].getFilename());
                logger.debug("copying media resource [" + target.getAbsolutePath() + "]");
                FileOutputStream fos = new FileOutputStream(target);
                InputStream is = media[i].getInputStream();
                byte[] buff = new byte[1];
                while (is.read(buff) != -1) fos.write(buff);
                fos.flush();
                fos.close();
                is.close();
            }
        } catch (Exception e) {
            logger.error("Failed to move media resources to output directory", e);
        }
    }

    /**
	 * Set the location of the 'dot' executable file from the Graphviz
	 * installation. This file will be called with appropriate parameters if
	 * graphing output is required using a
	 * <code>Runtime.getRuntime().exec(...)</code> call. If this value is not
	 * set, graphing output will be disabled.
	 * 
	 * @param dotExe the platform dependent location of the binary, ie
	 * "/usr/local/bin/dot" or "C:/graphviz/dot.exe"
	 */
    public void setDotExe(String dotExe) {
        this.dotExe = dotExe;
    }

    /**
	 * A series of intermediate files (.dot files) are created which is what
	 * GraphViz uses to actually generate the graphs. Usually these will not be
	 * needed after the graphs are generated and so by default are discarded. If
	 * you need to keep them for any reason, set this value to
	 * <code>false</code>
	 * 
	 * @param removeDotFiles set to false to prevent intermediate .dot files
	 * being discarded. True by default.
	 */
    public void setRemoveDotFiles(boolean removeDotFiles) {
        this.removeDotFiles = removeDotFiles;
    }

    /**
	 * Location of the GraphViz 'dot' executable program on the local machine
	 * 
	 * @return the platform-dependent location of the GraphViz 'dot' executable
	 * file
	 */
    public String getDotExe() {
        return dotExe;
    }

    /**
	 * Should intermediate .dot files be removed?
	 * 
	 * @return true if intermediate .dot files will be removed after graphing
	 * output has completed, or false if they will be kept in the output
	 * directory. True by default.
	 */
    public boolean isRemoveDotFiles() {
        return removeDotFiles;
    }

    /**
	 * @return the format string to denote output type of the image map
	 */
    public String getDotFileMapFormat() {
        return dotFileMapFormat;
    }

    /**
	 * The image map format that Dot should use to generate an image map for the
	 * context graphs. Most likely to be "cmap" or "cmapx". See GraphViz
	 * documentation for more information.
	 * 
	 * @param dotFileMapFormat the format string to denote output type of the
	 * image map
	 */
    public void setDotFileMapFormat(String dotFileMapFormat) {
        this.dotFileMapFormat = dotFileMapFormat;
    }

    /**
	 * sets the file extension of Graphviz 'dot' files. Defaults to '.dot'
	 * 
	 * @param dotFileExtension the extension to use
	 */
    public void setDotFileExtension(String dotFileExtension) {
        this.dotFileExtension = dotFileExtension;
    }

    /**
	 * set a filename strategy for output files generated by this class
	 * 
	 * @param filenameStrategy the filename strategy to be used
	 */
    public void setFilenameStrategy(FilenameStrategy filenameStrategy) {
        this.filenameStrategy = filenameStrategy;
    }
}
