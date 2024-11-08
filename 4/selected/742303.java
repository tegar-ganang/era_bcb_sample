package com.whitebearsolutions.caldav.store;

import java.io.*;
import java.util.*;
import java.security.Principal;
import com.whitebearsolutions.caldav.*;
import com.whitebearsolutions.caldav.security.acl.CalDAVResourceACL;
import com.whitebearsolutions.caldav.security.acl.FileSystemResourceACL;
import com.whitebearsolutions.caldav.session.CalDAVITransaction;
import com.whitebearsolutions.caldav.session.CalDAVTransaction;

public class FileSystemStore implements CalDAVStore {

    private static int BUF_SIZE = 65536;

    private File _root = null;

    public FileSystemStore(File root) {
        this._root = root;
    }

    public CalDAVTransaction begin(Principal principal) throws CalDAVException {
        if (!this._root.exists()) {
            if (!this._root.mkdirs()) {
                throw new CalDAVException("root path: " + this._root.getAbsolutePath() + " does not exist and could not be created");
            }
        }
        CalDAVTransaction transaction = new CalDAVITransaction(principal);
        if (!new File(this._root.getAbsolutePath() + File.separator + ".acl.xml").exists()) {
            new FileSystemResourceACL(this, transaction, File.separator);
        }
        return transaction;
    }

    public CalDAVResourceACL getResourceACL(CalDAVTransaction transaction, String uri) throws CalDAVException {
        return new FileSystemResourceACL(this, transaction, uri);
    }

    public String getRootPath() {
        return this._root.getAbsolutePath();
    }

    public void checkAuthentication(CalDAVTransaction transaction) throws SecurityException {
    }

    public void commit(CalDAVTransaction transaction) throws CalDAVException {
    }

    public void rollback(CalDAVTransaction transaction) throws CalDAVException {
    }

    public void createFolder(CalDAVTransaction transaction, String uri) throws CalDAVException {
        File file = new File(this._root, uri);
        if (!file.mkdir()) {
            throw new CalDAVException("cannot create folder: " + uri);
        }
    }

    public void createResource(CalDAVTransaction transaction, String uri) throws CalDAVException {
        File file = new File(this._root, uri);
        try {
            if (!file.createNewFile()) throw new CalDAVException("cannot create file: " + uri);
        } catch (IOException _ex) {
            throw new CalDAVException(_ex);
        }
    }

    public long setResourceContent(CalDAVTransaction transaction, String uri, InputStream is, String contentType, String characterEncoding) throws CalDAVException {
        File file = new File(this._root, uri);
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(file), BUF_SIZE);
            try {
                int read;
                byte[] copyBuffer = new byte[BUF_SIZE];
                while ((read = is.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    os.write(copyBuffer, 0, read);
                }
            } finally {
                try {
                    is.close();
                } finally {
                    os.close();
                }
            }
        } catch (IOException _ex) {
            throw new CalDAVException(_ex);
        }
        long length = -1;
        try {
            length = file.length();
        } catch (SecurityException _ex) {
        }
        return length;
    }

    public String[] getChildrenNames(CalDAVTransaction transaction, String uri) throws CalDAVException {
        File file = new File(this._root, uri);
        String[] childrenNames = new String[] {};
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            List<String> childList = new ArrayList<String>();
            String name = null;
            for (int i = 0; i < children.length; i++) {
                name = children[i].getName();
                if (name.startsWith(".")) {
                    continue;
                }
                childList.add(name);
            }
            childrenNames = new String[childList.size()];
            childrenNames = (String[]) childList.toArray(childrenNames);
            return childrenNames;
        } else {
            return childrenNames;
        }
    }

    public String[] getAllChildrenNames(CalDAVTransaction transaction, String uri) throws CalDAVException {
        File file = new File(this._root, uri);
        String[] childrenNames = new String[] {};
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            List<String> childList = new ArrayList<String>();
            String name = null;
            for (int i = 0; i < children.length; i++) {
                name = children[i].getName();
                if (name.startsWith(".")) {
                    continue;
                }
                childList.add(name);
            }
            childrenNames = new String[childList.size()];
            childrenNames = (String[]) childList.toArray(childrenNames);
            return childrenNames;
        } else {
            return childrenNames;
        }
    }

    public void removeObject(CalDAVTransaction transaction, String uri) throws CalDAVException {
        File file = new File(this._root, uri);
        if (file.isDirectory()) {
            File _acl = new File(file.getAbsolutePath() + "/.acl.xml");
            if (_acl.exists()) {
                if (!_acl.delete()) {
                    throw new CalDAVException("cannot delete object: " + uri);
                }
            }
        }
        if (!file.delete()) {
            throw new CalDAVException("cannot delete object: " + uri);
        }
    }

    public boolean resourceExists(CalDAVTransaction transaction, String uri) throws CalDAVException {
        File file = new File(this._root, uri);
        return file.exists();
    }

    public InputStream getResourceContent(CalDAVTransaction transaction, String uri) throws CalDAVException {
        File file = new File(this._root, uri);
        InputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
        } catch (IOException _ex) {
            throw new CalDAVException(_ex);
        }
        return in;
    }

    public long getResourceLength(CalDAVTransaction transaction, String uri) throws CalDAVException {
        File file = new File(this._root, uri);
        return file.length();
    }

    public StoredObject getStoredObject(CalDAVTransaction transaction, String uri) {
        StoredObject so = null;
        StringTokenizer _st = new StringTokenizer(uri, "/");
        while (_st.hasMoreTokens()) {
            String name = _st.nextToken();
            if (name != null && name.startsWith(".")) {
                return so;
            }
        }
        File file = new File(this._root, uri);
        if (file.exists()) {
            so = new StoredObject();
            so.setFolder(file.isDirectory());
            so.setLastModified(new Date(file.lastModified()));
            so.setCreationDate(new Date(file.lastModified()));
            so.setResourceLength(file.length());
        }
        return so;
    }
}
