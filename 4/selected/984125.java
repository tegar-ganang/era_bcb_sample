package com.whitebearsolutions.caldav.method;

import java.io.*;
import javax.servlet.http.*;
import com.whitebearsolutions.caldav.locking.*;
import com.whitebearsolutions.caldav.AccessDeniedException;
import com.whitebearsolutions.caldav.CalDAVMimeType;
import com.whitebearsolutions.caldav.CalDAVResponse;
import com.whitebearsolutions.caldav.session.CalDAVTransaction;
import com.whitebearsolutions.caldav.store.*;
import com.whitebearsolutions.util.icalendar.VCalendar;

public class GET extends HEAD {

    public GET(CalDAVStore store, String draft_index_file, String insteadOf404, ResourceLocksMap resourceLocks, CalDAVMimeType mimeType, int contentLengthHeader) {
        super(store, draft_index_file, insteadOf404, resourceLocks, mimeType, contentLengthHeader);
    }

    protected void doBody(CalDAVTransaction transaction, HttpServletResponse resp, String path) {
        try {
            StoredObject so = this._store.getStoredObject(transaction, path);
            if (so.isNullResource()) {
                String methodsAllowed = CalDAVMethods.determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(CalDAVResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            OutputStream out = resp.getOutputStream();
            InputStream in = this._store.getResourceContent(transaction, path);
            try {
                int read = -1;
                byte[] copyBuffer = new byte[BUF_SIZE];
                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    out.write(copyBuffer, 0, read);
                }
            } finally {
                try {
                    in.close();
                } catch (Exception _ex) {
                }
                try {
                    out.flush();
                    out.close();
                } catch (Exception _ex) {
                }
            }
        } catch (AccessDeniedException _ex) {
            try {
                resp.sendError(CalDAVResponse.SC_FORBIDDEN);
            } catch (Exception _ex2) {
            }
        } catch (Exception _ex) {
        }
    }

    protected void folderBody(CalDAVTransaction transaction, String path, HttpServletResponse resp, HttpServletRequest req) throws IOException {
        StoredObject so = this._store.getStoredObject(transaction, path);
        if (so == null) {
            String parentPath = getParentPath(path);
            while (parentPath.endsWith(".ics")) {
                parentPath = parentPath.substring(0, parentPath.lastIndexOf("/"));
            }
            path = parentPath + path.substring(path.lastIndexOf("/"));
            if (path.endsWith(".ics")) {
                String calendarPath = parentPath.concat("/calendar.ics");
                String uid = path.substring(path.lastIndexOf("/") + 1);
                uid = uid.substring(0, uid.length() - 4);
                OutputStream _out = resp.getOutputStream();
                try {
                    if (this._store.resourceExists(transaction, calendarPath)) {
                        File _f = new File(this._store.getRootPath() + calendarPath);
                        VCalendar _vc = VCalendarCache.getVCalendar(_f);
                        if (_vc.hasVevent(uid) || _vc.hasVtodo(uid)) {
                            VCalendar _res_vc = new VCalendar();
                            if (_vc.hasVevent(uid)) {
                                _res_vc.addVevent(_vc.getVevent(uid));
                            } else if (_vc.hasVtodo(uid)) {
                                _res_vc.addVtodo(_vc.getVtodo(uid));
                            }
                            _out.write(_res_vc.toString().getBytes());
                        } else {
                            _out.write(_vc.toString().getBytes());
                        }
                    } else {
                        resp.sendError(CalDAVResponse.SC_NOT_FOUND);
                    }
                } catch (Exception _ex) {
                    resp.sendError(CalDAVResponse.SC_INTERNAL_SERVER_ERROR, _ex.getMessage());
                    return;
                } finally {
                    _out.flush();
                    _out.close();
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
            }
        } else {
            if (so.isNullResource()) {
                String methodsAllowed = CalDAVMethods.determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(CalDAVResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            if (so.isFolder()) {
                OutputStream _out = resp.getOutputStream();
                String[] children = this._store.getChildrenNames(transaction, path);
                StringBuilder childrenTemp = new StringBuilder();
                childrenTemp.append("Contents of this Folder:\n");
                for (String child : children) {
                    childrenTemp.append(child);
                    childrenTemp.append("\n");
                }
                _out.write(childrenTemp.toString().getBytes());
                _out.flush();
                _out.close();
            }
        }
    }
}
