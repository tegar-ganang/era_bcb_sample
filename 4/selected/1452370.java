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
public class GraphVizCompiler implements DocumentCompiler {

    private static final String MEDIA_RESOURCES = "classpath:/org/springframework/beandoc/output/media/*";

    private static final String APPLET_RESOURCES = "classpath:/org/springframework/beandoc/output/lib/*";

    private static final String SVG_TYPE = "svg";

    private String dotFileExtension = ".dot";

    protected final Log logger = LogFactory.getLog(getClass());

    private String dotExe;

    private boolean removeDotFiles = true;

    private File outputDir;

    /**
	 * Generates actual images and HTML image maps (as required) from the dot
	 * files created by DotFileTransformer. Subsequently plugs the image maps
	 * into placeholders in the graph html files.
	 * 
	 * @see org.springframework.beandoc.output.DocumentCompiler#compile(Document[],
	 * File)
	 */
    public void compile(Document[] contextDocuments) {
        if (outputDir == null) throw new IllegalStateException("Please supply a value for the root output directory (setOutputDir())");
        if (contextDocuments == null || contextDocuments.length < 1) throw new IllegalArgumentException("Please supply valid context documents");
        copyMediaResources(outputDir);
        String consolidatedImage = contextDocuments[0].getRootElement().getAttributeValue(GraphVizDecorator.ATTRIBUTE_GRAPH_CONSOLIDATED);
        String graphOutputType = StringUtils.unqualify(consolidatedImage);
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
            if (SVG_TYPE.equalsIgnoreCase(graphOutputType)) runDot(dotFile, "png", "png");
            if (removeDotFiles) {
                dotFile.delete();
            }
        } else logger.warn("GraphViz 'dot' executable not set or couldn't be found.  No graphs generated.");
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
            Process dot = Runtime.getRuntime().exec(cmd, null, outputFile.getParentFile());
            new StreamPipe(dot.getInputStream(), System.out).start();
            new StreamPipe(dot.getErrorStream(), System.err).start();
            dot.waitFor();
            logger.debug("Process exited with value [" + dot.exitValue() + "]");
        } catch (IOException ioe) {
            logger.warn("Problem attempting to create [" + outputFile + "] from dot file [" + dotFileName + "]; " + ioe.getMessage());
        } catch (InterruptedException e) {
        }
        return outputFile;
    }

    /**
	 * Writes the media files to the output location.
	 */
    private void copyMediaResources(File outputDir) {
        logger.debug("Copying media resources to output location");
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(new DefaultResourceLoader());
            Resource[] media = resolver.getResources(MEDIA_RESOURCES);
            extractResourcesFromJar(outputDir, media);
            Resource[] libs = resolver.getResources(APPLET_RESOURCES);
            File libDir = new File(outputDir, "lib");
            libDir.mkdirs();
            extractResourcesFromJar(libDir, libs);
        } catch (Exception e) {
            logger.error("Failed to move media resources to output directory", e);
        }
    }

    private void extractResourcesFromJar(File outputDir, Resource[] resources) throws FileNotFoundException, IOException {
        for (int i = 0; i < resources.length; i++) {
            File target = new File(outputDir, resources[i].getFilename());
            logger.debug("copying media resource [" + target.getAbsolutePath() + "]");
            FileOutputStream fos = new FileOutputStream(target);
            InputStream is = resources[i].getInputStream();
            byte[] buff = new byte[1];
            while (is.read(buff) != -1) fos.write(buff);
            fos.flush();
            fos.close();
            is.close();
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
	 * sets the file extension of Graphviz 'dot' files. Defaults to '.dot'
	 * 
	 * @param dotFileExtension the extension to use
	 */
    public void setDotFileExtension(String dotFileExtension) {
        this.dotFileExtension = dotFileExtension;
    }

    /**
	 * set a top level directory that this compiler will use to generate 
	 * file based output to
	 * 
	 * @param outputDir
	 */
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    /**
	 * helper class to retrieve and print {@link java.io.InputStream}s, e.g. from {@link Process#getErrorStream()}
	 */
    private class StreamPipe extends Thread {

        java.io.InputStream __is = null;

        java.io.PrintStream __ps = null;

        public StreamPipe(java.io.InputStream is, PrintStream ps) {
            __is = is;
            __ps = ps;
        }

        public void run() {
            try {
                while (true) {
                    int _ch = __is.read();
                    if (_ch != -1) __ps.write((char) _ch); else break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
