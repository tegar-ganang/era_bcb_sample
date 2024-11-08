package com.netx.ebs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.Writer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import com.netx.generics.util.Text;
import com.netx.generics.basic.Checker;
import com.netx.generics.basic.ErrorList;

public class Template {

    private static final String _PROP_BEGIN = "#:";

    private static final String _LIST_BEGIN = "#LIST:";

    private static final String _LIST_END = "#END-LIST";

    private static final String _UNDEFINED = "UNDEFINED";

    private final List _lines;

    private final boolean _writeSpaces;

    private int _currentLine;

    public Template(Reader reader, boolean writeSpaces) throws IOException {
        Checker.checkNull(reader, "reader");
        _lines = new Text(reader).getLines();
        _writeSpaces = writeSpaces;
        _currentLine = 0;
    }

    public Template(InputStream in, boolean writeSpaces) throws IOException {
        Checker.checkNull(in, "in");
        _lines = new Text(new InputStreamReader(in)).getLines();
        _writeSpaces = writeSpaces;
        _currentLine = 0;
    }

    public Template(InputStream in) throws IOException {
        this(in, true);
    }

    public Template(File file, boolean writeSpaces) throws IOException {
        Checker.checkNull(file, "file");
        _lines = new Text(new FileReader(file)).getLines();
        _writeSpaces = writeSpaces;
        _currentLine = 0;
    }

    public Template(File file) throws IOException {
        this(new FileReader(file), true);
    }

    public void write(Writer out, TemplateValues values) throws TemplateException, IOException {
        if (!(out instanceof BufferedWriter)) {
            out = new BufferedWriter(out);
        }
        final List<String> errors = new ArrayList<String>();
        _parseLines(out, values, errors);
        out.close();
        if (!errors.isEmpty()) {
            throw new TemplateException(new ErrorList("errors parsing template", errors));
        }
    }

    private void _parseLines(Writer out, TemplateValues tv, List<String> errors) throws IOException {
        while (_currentLine < _lines.size()) {
            String line = _lines.get(_currentLine).toString();
            if (line.indexOf(_LIST_BEGIN) != -1) {
                int index = line.indexOf(_LIST_BEGIN);
                String name = _getPropertyName(line, index, _LIST_BEGIN);
                List tvList = tv.getList(name);
                if (tvList == null) {
                    _addError(errors, "could not find list \"" + name + "\"");
                    _currentLine++;
                    while (line.indexOf(_LIST_END) == -1) {
                        line = _lines.get(_currentLine++).toString();
                    }
                } else if (tvList.isEmpty()) {
                    _currentLine++;
                    while (line.indexOf(_LIST_END) == -1) {
                        line = _lines.get(_currentLine++).toString();
                    }
                } else {
                    _currentLine++;
                    Iterator it = tvList.iterator();
                    int lastCurrent = _currentLine;
                    while (it.hasNext()) {
                        _currentLine = lastCurrent;
                        _parseLines(out, (TemplateValues) it.next(), errors);
                    }
                    _currentLine++;
                }
            } else if (line.indexOf(_LIST_END) != -1) {
                return;
            } else {
                String result = _treatLine(line, tv, _currentLine, errors);
                if (_writeSpaces) {
                    out.write(result);
                    out.write("\r\n");
                } else {
                    out.write(result.trim());
                }
                _currentLine++;
            }
        }
    }

    private String _treatLine(String line, TemplateValues tv, int lineNumber, List<String> errors) {
        StringBuilder sb = new StringBuilder(line);
        int index = 0;
        while (true) {
            index = sb.indexOf(_PROP_BEGIN);
            if (index == -1) {
                return sb.toString();
            } else {
                String name = _getPropertyName(sb.toString(), index, _PROP_BEGIN);
                String value = tv.getValue(name);
                if (value == null) {
                    value = _UNDEFINED;
                    _addError(errors, "could not find property \"" + name + "\"");
                }
                sb.replace(index, index + _PROP_BEGIN.length() + name.length(), value);
            }
        }
    }

    private String _getPropertyName(String line, int index, String token) {
        index += token.length();
        StringBuilder name = new StringBuilder();
        while (index < line.length() && (Character.isJavaIdentifierStart(line.charAt(index)) || Character.isDigit(line.charAt(index)))) {
            name.append(line.charAt(index++));
        }
        return name.toString();
    }

    private void _addError(List<String> errors, String message) {
        errors.add("line " + (_currentLine + 1) + ": " + message);
    }
}
