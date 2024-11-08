package com.scholardesk.abstracts.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.scholardesk.abstracts.AbstractsTask;
import com.scholardesk.abstracts.constants.AbstractStatus;
import com.scholardesk.abstracts.mapper.AbstractMapper;
import com.scholardesk.abstracts.model.Abstract;
import com.scholardesk.abstracts.utilities.PDFConverter;
import com.scholardesk.model.UploadFile;
import com.scholardesk.utilities.StringUtil;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.BadPdfFormatException;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfCopy;

public class PdfExportTask extends AbstractsTask {

    @Override
    public void process() {
        Integer _status_id = null;
        if (StringUtil.exists(http_request.getParameter("status_id"))) _status_id = Integer.valueOf(http_request.getParameter("status_id"));
        List<Abstract> _abstracts = null;
        if (_status_id != null) _abstracts = new AbstractMapper().findAllByProgramAndStatus(program.getId(), _status_id); else _abstracts = new AbstractMapper().findAllByProgram(program.getId());
        if (_abstracts == null) return;
        Document _document = new Document(PageSize.LETTER);
        PdfCopy _writer = null;
        try {
            _writer = new PdfCopy(_document, http_response.getOutputStream());
            _document.open();
        } catch (DocumentException _e) {
            throw new RuntimeException(_e);
        } catch (IOException _e) {
            throw new RuntimeException(_e);
        }
        try {
            for (Abstract _abstract : _abstracts) {
                PdfReader _reader = null;
                if (_abstract.hasAbstractFile()) _reader = new PdfReader(getFileInputStream(_abstract.getAbstractFile())); else {
                    ByteArrayOutputStream _baos = new ByteArrayOutputStream();
                    PDFConverter.abstract2PDF(_abstract, _baos);
                    _reader = new PdfReader(new ByteArrayInputStream(_baos.toByteArray()));
                }
                PdfImportedPage _page = null;
                for (int _i = 1; _i <= _reader.getNumberOfPages(); _i++) {
                    _page = _writer.getImportedPage(_reader, _i);
                    _writer.addPage(_page);
                }
            }
        } catch (IOException _e) {
            throw new RuntimeException(_e);
        } catch (BadPdfFormatException _e) {
            throw new RuntimeException(_e);
        }
        _document.close();
    }

    private FileInputStream getFileInputStream(UploadFile _file) throws FileNotFoundException {
        if (_file == null) throw new NullPointerException("UploadFile must exist for this abstract!");
        String _filename = config.getString("upload.storage_path") + "/" + program.getId() + "/" + _file.getId();
        return new FileInputStream(_filename);
    }

    @Override
    public HttpServletResponse setResponse() {
        String _status_label = "All";
        if (StringUtil.exists(http_request.getParameter("status_id"))) {
            Integer _status_id = Integer.valueOf(http_request.getParameter("status_id"));
            _status_label = AbstractStatus.lookup(_status_id).getLabel();
        }
        http_response.setContentType("application/pdf");
        http_response.setHeader("Content-Disposition", "inline; filename=abstracts-" + _status_label + "-" + program.getId() + ".pdf");
        return http_response;
    }

    @Override
    public HttpServletRequest setRequest() {
        return http_request;
    }
}
