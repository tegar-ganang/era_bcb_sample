package ise.plugin.svn.command;

import java.io.*;
import java.util.*;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import ise.plugin.svn.data.CheckoutData;
import ise.plugin.svn.gui.DirTreeNode;
import ise.plugin.svn.library.FileUtilities;

public class BrowseRepository {

    public List<DirTreeNode> getRepository(CheckoutData cd) throws CommandInitializationException, SVNException {
        return getRepository(null, cd);
    }

    public List<DirTreeNode> getRepository(DirTreeNode node, CheckoutData cd) throws CommandInitializationException, SVNException {
        if (node == null && cd.getURL() == null) {
            return null;
        }
        String url = node == null ? cd.getURL() : node.getRepositoryLocation();
        if (cd.getOut() == null) {
            throw new CommandInitializationException("Invalid output stream.");
        }
        if (cd.getErr() == null) {
            cd.setErr(cd.getOut());
        }
        PrintStream out = cd.getOut();
        SVNRepository repository = null;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
        } catch (SVNException svne) {
            cd.getOut().printError("Error while creating an SVNRepository for location '" + url + "': " + svne.getMessage());
            return null;
        }
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(cd.getUsername(), cd.getPassword());
        repository.setAuthenticationManager(authManager);
        DirTreeNode root;
        if (node == null) {
            root = new DirTreeNode(url, false);
        } else {
            root = node;
        }
        List<DirTreeNode> children = null;
        try {
            SVNNodeKind nodeKind = repository.checkPath("", -1);
            if (nodeKind == SVNNodeKind.NONE) {
                cd.getOut().printError("There is no entry at '" + url + "'.");
                return null;
            } else if (nodeKind == SVNNodeKind.FILE) {
                cd.getOut().printError("The entry at '" + url + "' is a file while a directory was expected.");
                return null;
            }
            out.println("Repository Root: " + repository.getRepositoryRoot(true));
            out.println("Repository UUID: " + repository.getRepositoryUUID(true));
            out.println("");
            boolean isExternal = false;
            if (node != null) {
                isExternal = node.isExternal();
            }
            children = listEntries(repository, isExternal, "", out);
        } catch (SVNException svne) {
            cd.getOut().printError("error while listing entries: " + svne.getMessage());
            return null;
        }
        long latestRevision = -1;
        try {
            latestRevision = repository.getLatestRevision();
        } catch (SVNException svne) {
            cd.getOut().printError("error while fetching the latest repository revision: " + svne.getMessage());
            return null;
        }
        out.println("");
        out.println("---------------------------------------------");
        out.println("Repository latest revision: " + latestRevision);
        out.flush();
        out.close();
        return children;
    }

    public List<DirTreeNode> listEntries(SVNRepository repository, boolean isExternal, String path, PrintStream out) throws SVNException {
        List<DirTreeNode> list = new ArrayList<DirTreeNode>();
        Map dir_props = new HashMap();
        Collection entries = repository.getDir(path, -1, dir_props, (Collection) null);
        Iterator iterator = entries.iterator();
        while (iterator.hasNext()) {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            out.println("/" + (path.equals("") ? "" : path + "/") + entry.getName() + " (author: '" + entry.getAuthor() + "'; revision: " + entry.getRevision() + "; date: " + entry.getDate() + ")");
            DirTreeNode node = new DirTreeNode(entry.getName(), !(entry.getKind() == SVNNodeKind.DIR));
            if (isExternal) {
                node.setExternal(true);
                node.setRepositoryLocation(repository.getLocation().toString() + "/" + entry.getName());
            }
            list.add(node);
        }
        if (dir_props.size() > 0) {
            String value = (String) dir_props.get(SVNProperty.EXTERNALS);
            try {
                if (value != null) {
                    BufferedReader br = new BufferedReader(new StringReader(value));
                    String line = br.readLine();
                    while (line != null) {
                        String dir = line.substring(0, line.indexOf(" "));
                        String rep = line.substring(line.indexOf(" ") + 1);
                        DirTreeNode node = new DirTreeNode(dir, false);
                        node.setExternal(true);
                        node.setRepositoryLocation(rep);
                        list.add(node);
                        line = br.readLine();
                    }
                    br.close();
                }
            } catch (Exception e) {
            }
        }
        Collections.sort(list);
        List<DirTreeNode> newList = new ArrayList<DirTreeNode>();
        for (DirTreeNode node : list) {
            newList.add((DirTreeNode) node);
        }
        return newList;
    }

    public File getFile(String url, String filepath, long revision, String username, String password) {
        setupLibrary();
        SVNRepository repository = null;
        File outfile = null;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
            repository.setAuthenticationManager(authManager);
            SVNNodeKind nodeKind = repository.checkPath(filepath, revision);
            if (nodeKind == SVNNodeKind.NONE || nodeKind == SVNNodeKind.DIR) {
                return null;
            }
            Map fileproperties = new HashMap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            repository.getFile(filepath, revision, fileproperties, baos);
            String mimeType = (String) fileproperties.get(SVNProperty.MIME_TYPE);
            boolean isTextType = SVNProperty.isTextMimeType(mimeType);
            if (isTextType) {
                int index = filepath.lastIndexOf(".");
                index = index < 0 ? 0 : index;
                if (index == 0) {
                    int slash_index = filepath.lastIndexOf("/");
                    if (slash_index > 0 && slash_index < filepath.length()) {
                        index = slash_index + 1;
                    }
                }
                String filename = filepath.substring(0, index) + "-" + (revision < 0L ? "HEAD" : String.valueOf(revision)) + filepath.substring(index);
                filename = System.getProperty("java.io.tmpdir") + "/" + filename;
                outfile = new File(filename);
                if (outfile.exists()) {
                    outfile.delete();
                }
                outfile.deleteOnExit();
                outfile.getParentFile().mkdirs();
                StringReader reader = new StringReader(baos.toString());
                BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
                FileUtilities.copy(reader, writer);
                writer.close();
                return outfile;
            }
        } catch (Exception e) {
            e.printStackTrace();
            outfile = null;
        }
        return outfile;
    }

    public static void setupLibrary() {
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }
}
