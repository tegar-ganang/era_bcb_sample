package com.scholardesk.abstracts.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.scholardesk.model.UploadFile;
import com.scholardesk.abstracts.AbstractsTask;
import com.scholardesk.abstracts.constants.AbstractStatus;
import com.scholardesk.abstracts.mapper.AbstractMapper;
import com.scholardesk.abstracts.model.Abstract;
import com.scholardesk.abstracts.utilities.PDFConverter;
import com.scholardesk.utilities.StringUtil;

public class ZipExportTask extends AbstractsTask {

    List<Abstract> m_abstracts = null;

    @Override
    public void process() {
        Integer _status_id = null;
        if (StringUtil.exists(http_request.getParameter("status_id"))) _status_id = Integer.valueOf(http_request.getParameter("status_id"));
        if (_status_id != null) m_abstracts = new AbstractMapper().findAllByProgramAndStatus(program.getId(), _status_id); else m_abstracts = new AbstractMapper().findAllByProgram(program.getId());
        if (m_abstracts == null) return;
        try {
            ZipOutputStream _out = new ZipOutputStream(http_response.getOutputStream());
            String _file_type = (String) http_request.getParameter("file_type");
            for (Abstract _abstract : m_abstracts) {
                UploadFile _file = null;
                InputStream _in = null;
                if (_file_type != null && _file_type.equals("paper")) {
                    _file = _abstract.getPaperFile();
                    _in = getFileInputStream(_file);
                } else {
                    _file = _abstract.getAbstractFile();
                    if (_file != null) _in = getFileInputStream(_file); else {
                        ByteArrayOutputStream _baos = new ByteArrayOutputStream();
                        PDFConverter.abstract2PDF(_abstract, _baos);
                        _in = new ByteArrayInputStream(_baos.toByteArray());
                    }
                }
                if (_in == null) continue;
                String _filename = null;
                if (_file != null) _filename = _file.getId() + "_" + _file.getFilename(); else _filename = _abstract.getId() + ".pdf";
                addZipEntry(_filename, _in, _out);
            }
            _out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addZipEntry(String _filename, InputStream _in, ZipOutputStream _out) throws IOException {
        byte[] _buf = new byte[1024];
        _out.putNextEntry(new ZipEntry(_filename));
        int _len;
        while ((_len = _in.read(_buf)) > 0) _out.write(_buf, 0, _len);
        _out.closeEntry();
        _in.close();
    }

    private FileInputStream getFileInputStream(UploadFile _file) throws FileNotFoundException {
        if (_file == null) return null;
        String _filename = config.getString("upload.storage_path") + "/" + program.getId() + "/" + _file.getId();
        return new FileInputStream(_filename);
    }

    @Override
    public String getView() {
        if (m_abstracts == null) return "no_export_files.vm";
        return null;
    }

    @Override
    public HttpServletRequest setRequest() {
        return http_request;
    }

    @Override
    public HttpServletResponse setResponse() {
        String _file_prefix = "abstracts";
        if (http_request.getParameter("file_type") != null && http_request.getParameter("file_type").equals("paper")) _file_prefix = "papers";
        String _status_label = "All";
        if (StringUtil.exists(http_request.getParameter("status_id"))) {
            Integer _status_id = Integer.valueOf(http_request.getParameter("status_id"));
            _status_label = AbstractStatus.lookup(_status_id).getLabel();
        }
        http_response.setContentType("application/zip");
        http_response.setHeader("Content-Disposition", "attachment; filename=" + _file_prefix + "-" + _status_label + "-" + program.getId() + ".zip");
        return http_response;
    }
}
