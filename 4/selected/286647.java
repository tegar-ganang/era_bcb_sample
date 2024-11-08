package org.plog4u.wiki.builder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.plog4u.wiki.actions.mediawiki.exceptions.ConfigurationException;
import org.plog4u.wiki.editor.WikiEditorPlugin;
import org.plog4u.wiki.preferences.Util;
import org.plog4u.wiki.renderer.IContentRenderer;
import org.plog4u.wiki.renderer.RendererFactory;
import org.plog4u.wiki.renderer.StringUtil;
import org.plog4u.wiki.renderer.WikiOfflineModel;

/**
 * Create a static HTML page
 */
public class CreatePageAction implements IObjectActionDelegate {

    /**
	 * Constant for an empty char array
	 */
    public static final char[] NO_CHAR = new char[0];

    private static final int DEFAULT_READING_SIZE = 8192;

    private IWorkbenchPart workbenchPart;

    /**
	 * 
	 */
    public CreatePageAction() {
        super();
    }

    /**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        workbenchPart = targetPart;
    }

    public void run(IAction action) {
        ISelectionProvider selectionProvider = null;
        selectionProvider = workbenchPart.getSite().getSelectionProvider();
        StructuredSelection selection = null;
        selection = (StructuredSelection) selectionProvider.getSelection();
        Iterator iterator = null;
        iterator = selection.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof IResource) {
                IResource resource = (IResource) obj;
                switch(resource.getType()) {
                    case IResource.FILE:
                        createPage((IFile) resource);
                }
            }
        }
    }

    /**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    public static boolean createPDFFragmentPage(IFile file, StringBuffer htmlBuffer) {
        BufferedInputStream stream = null;
        boolean noContent = true;
        try {
            String srcBasePath = Util.getWPSourcePath(file);
            String binBasePath = Util.getProjectsWikiOutputPath(file.getProject(), WikiEditorPlugin.HTML_OUTPUT_PATH);
            IContentRenderer renderer = RendererFactory.createContentRenderer(file.getProject());
            stream = new BufferedInputStream(file.getContents());
            String fileName = Util.getHTMLFileName(file, binBasePath, srcBasePath);
            String content = new String(getInputStreamAsCharArray(stream, -1, "utf-8"));
            noContent = StringUtil.checkNoContent(content);
            String filePath = file.getLocation().toString();
            if (filePath.startsWith(srcBasePath)) {
                filePath = filePath.substring(srcBasePath.length() + 1);
            }
            int index = 0;
            int level = 0;
            while (index >= 0) {
                index = fileName.indexOf('/', index);
                if (index >= 0) {
                    level++;
                    index++;
                }
            }
            IProject project = file.getProject();
            String baseFilename = Util.getProjectsWikiOutputPath(project, WikiEditorPlugin.HTML_OUTPUT_PATH);
            String imageFilename = "file:///" + baseFilename + "/${image}";
            WikiOfflineModel wikiModel = new WikiOfflineModel(imageFilename, "${title}.html", project, null, file.getLocation().toPortableString());
            renderer.renderPDF(wikiModel, content, htmlBuffer, level);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
        return noContent;
    }

    public static void createPage(IFile file) {
        String templateFileName = Util.getLocalTemplate(file);
        String srcBasePath = Util.getWPSourcePath(file);
        String binBasePath = Util.getProjectsWikiOutputPath(file.getProject(), WikiEditorPlugin.HTML_OUTPUT_PATH);
        createPage(templateFileName, file, binBasePath, srcBasePath);
    }

    public static void createPage(String templateFileName, IFile file, String binBasePath, String srcBasePath) {
        if ("wp".equalsIgnoreCase(file.getFileExtension())) {
            try {
                IContentRenderer renderer = RendererFactory.createContentRenderer(file.getProject());
                convertWikiFile(templateFileName, file, binBasePath, srcBasePath, renderer);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (CoreException e1) {
                e1.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            String fname = file.getName().toLowerCase();
            if ((fname.charAt(0) == '.') || "project.index".equals(fname) || "cvs".equals(fname) || "entries".equals(fname) || "repository".equals(fname) || "root".equals(fname)) {
                return;
            }
            FileOutputStream output = null;
            InputStream contentStream = null;
            try {
                String filename = Util.getHTMLFileName(file, binBasePath, srcBasePath);
                if (filename != null) {
                    int index = filename.lastIndexOf('/');
                    if (index >= 0) {
                        File ioFile = new File(filename.substring(0, index));
                        if (!ioFile.isDirectory()) {
                            ioFile.mkdirs();
                        }
                    }
                    output = new FileOutputStream(filename);
                    contentStream = file.getContents(false);
                    int chunkSize = contentStream.available();
                    byte[] readBuffer = new byte[chunkSize];
                    int n = contentStream.read(readBuffer);
                    while (n > 0) {
                        output.write(readBuffer);
                        n = contentStream.read(readBuffer);
                    }
                }
            } catch (Exception e) {
            } finally {
                try {
                    if (output != null) output.close();
                    if (contentStream != null) contentStream.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    public static void convertWikiFile(String templateFileName, IFile file, String binBasePath, String srcBasePath, IContentRenderer renderer) throws CoreException, ConfigurationException {
        StringBuffer htmlBuffer = new StringBuffer();
        convertWikiBuffer(templateFileName, htmlBuffer, file, renderer, true);
        String htmlName = Util.getHTMLFileName(file, binBasePath, srcBasePath);
        if (htmlName != null) {
            writeHTMLFile(htmlBuffer, htmlName);
        }
    }

    public static void getWikiBuffer(StringBuffer htmlBuffer, IFile file) throws CoreException {
        BufferedInputStream stream = new BufferedInputStream(file.getContents());
        try {
            htmlBuffer.append(getInputStreamAsCharArray(stream, -1, null));
            return;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
        return;
    }

    public static void convertWikiBuffer(String templateFileName, StringBuffer htmlBuffer, IFile file, IContentRenderer renderer, boolean completeHTML) throws CoreException {
        BufferedInputStream stream = new BufferedInputStream(file.getContents());
        try {
            String content = new String(getInputStreamAsCharArray(stream, -1, null));
            String srcPath = Util.getWPSourcePath(file);
            String filePath = file.getLocation().toString();
            if (filePath.startsWith(srcPath)) {
                filePath = filePath.substring(srcPath.length() + 1);
            }
            createWikiBuffer(file.getProject(), templateFileName, htmlBuffer, filePath, content, renderer, completeHTML);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public static void createWikiBuffer(IProject project, String templateFileName, StringBuffer htmlBuffer, String fileName, String content, IContentRenderer renderer, boolean completeHTML) {
        int index = 0;
        int level = 0;
        while (index >= 0) {
            index = fileName.indexOf('/', index);
            if (index >= 0) {
                level++;
                index++;
            }
        }
        WikiOfflineModel wikiModel = Util.getWikiModel(project, null, fileName);
        wikiModel.setTitle(Util.getWikiTitle(fileName.replaceAll("_", " ")));
        renderer.render(wikiModel, templateFileName, content, htmlBuffer, level, completeHTML);
    }

    public static void writeHTMLFile(StringBuffer buffer, String filename) {
        int index = filename.lastIndexOf('/');
        if (index >= 0) {
            File file = new File(filename.substring(0, index));
            if (!file.isDirectory()) {
                file.mkdirs();
            }
        }
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(filename);
            fileWriter.write(buffer.toString());
            fileWriter.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Returns the given input stream's contents as a character array. If a
	 * length is specified (i.e. if length != -1), only length chars are
	 * returned. Otherwise all chars in the stream are returned. Note this
	 * doesn't close the stream.
	 * 
	 * @throws IOException
	 *             if a problem occurred reading the stream.
	 */
    public static char[] getInputStreamAsCharArray(InputStream stream, int length, String encoding) throws IOException {
        InputStreamReader reader = null;
        reader = encoding == null ? new InputStreamReader(stream) : new InputStreamReader(stream, encoding);
        char[] contents;
        if (length == -1) {
            contents = NO_CHAR;
            int contentsLength = 0;
            int amountRead = -1;
            do {
                int amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE);
                if (contentsLength + amountRequested > contents.length) {
                    System.arraycopy(contents, 0, contents = new char[contentsLength + amountRequested], 0, contentsLength);
                }
                amountRead = reader.read(contents, contentsLength, amountRequested);
                if (amountRead > 0) {
                    contentsLength += amountRead;
                }
            } while (amountRead != -1);
            if (contentsLength < contents.length) {
                System.arraycopy(contents, 0, contents = new char[contentsLength], 0, contentsLength);
            }
        } else {
            contents = new char[length];
            int len = 0;
            int readSize = 0;
            while ((readSize != -1) && (len != length)) {
                len += readSize;
                readSize = reader.read(contents, len, length - len);
            }
            if (len != length) System.arraycopy(contents, 0, (contents = new char[len]), 0, len);
        }
        return contents;
    }
}
