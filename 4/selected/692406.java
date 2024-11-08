package lpg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lpg.util.DispatchWriter;
import lpg.util.Utility;

class Option {

    Option(String[] args) {
        this.args = args;
        String mainInputFile = args[args.length - 1];
        String lpgTemplate = System.getenv("LPG_TEMPLATE");
        String lpgInclude = System.getenv("LPG_INCLUDE");
        File inputFile = new File(mainInputFile);
        homeDirectory = inputFile.getParent();
        processPath(templateSearchDirectory, lpgTemplate, homeDirectory);
        processPath(includeSearchDirectory, lpgInclude, homeDirectory);
        String fileName = inputFile.getName();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            grmFile = mainInputFile + ".g";
            lisFile = mainInputFile + ".l";
            tabFile = mainInputFile + ".t";
        } else {
            grmFile = mainInputFile;
            String temp = mainInputFile.substring(0, mainInputFile.length() - fileName.length() + dot);
            lisFile = temp + ".l";
            tabFile = temp + ".t";
        }
        try {
            syslisFos = new FileOutputStream(lisFile);
            syslis = new BufferedWriter(new OutputStreamWriter(syslisFos, Utility.getCharset()));
            errorFlag = false;
        } catch (IOException e) {
            throw new LpgException("Listing file \"" + lisFile + "\" cannot be openned.");
        } finally {
            if (errorFlag) Utility.close(syslisFos);
        }
        _syslis = new DispatchWriter();
        _syslis.write("//--_" + lisFile + "\n");
        if (_syslis.checkError()) throw new LpgException("Listing file \"" + lisFile + "\" cannot be openned.");
    }

    void close() {
        try {
            syslis.close();
            syslisFos = null;
        } catch (IOException e) {
        }
        _syslis.close();
    }

    void release() {
        Utility.close(syslisFos);
        Utility.close(_syslis);
    }

    void setLexStream(LexStream lexStream) {
        this.lexStream = lexStream;
    }

    void flushReport() {
        flushReport(true);
    }

    void flushReport(boolean writeSyslis) {
        String content = new String(report);
        if (writeSyslis) {
            try {
                syslis.write(content);
            } catch (IOException e) {
            }
            _syslis.write(content);
        }
        System.out.print(content);
        System.out.flush();
        report = new StringBuilder();
    }

    Token getTokenLocation(int location, int length) {
        if (inputFileSymbol != null) {
            Token errorToken = lexStream.getErrorToken(inputFileSymbol, location);
            errorToken.setEndLocation(location + length - 1);
            errorToken.setKind(0);
            return errorToken;
        }
        return null;
    }

    private void emitHeader(Token startToken, Token endToken, CharSequence header) {
        if (startToken == null) startToken = lexStream.getTokenReference(0);
        if (endToken == null) endToken = lexStream.getTokenReference(0);
        report.append(startToken.fileName());
        report.append(':');
        report.append(startToken.line());
        report.append(':');
        report.append(startToken.column());
        report.append(':');
        report.append(endToken.endLine());
        report.append(':');
        report.append(endToken.endColumn());
        report.append(':');
        report.append(startToken.startLocation());
        report.append(':');
        report.append(endToken.endLocation());
        report.append(": ");
        report.append(header);
    }

    private void emit(Token startToken, Token endToken, CharSequence header, CharSequence msg) {
        emitHeader(startToken, endToken, header);
        report.append(msg);
        report.append(Util.LINE_SEPARATOR);
        flushReport();
    }

    private void emit(Token token, CharSequence header, CharSequence msg) {
        emit(token, token, header, msg);
    }

    void emitError(int index, CharSequence msg) {
        emit(lexStream.getTokenReference(index), "Error: ", msg);
    }

    void emitWarning(int index, CharSequence msg) {
        emit(lexStream.getTokenReference(index), "Warning: ", msg);
    }

    void emitInformative(int index, CharSequence msg) {
        emit(lexStream.getTokenReference(index), "Informative: ", msg);
    }

    void emitError(Token token, CharSequence msg) {
        emit(token, "Error: ", msg);
        errorFlag = true;
    }

    void emitWarning(Token token, CharSequence msg) {
        emit(token, "Warning: ", msg);
    }

    void emitInformative(Token token, CharSequence msg) {
        emit(token, "Informative: ", msg);
    }

    void emitError(Token startToken, Token endToken, CharSequence msg) {
        emit(startToken, endToken, "Error: ", msg);
        errorFlag = true;
    }

    void emitWarning(Token startToken, Token endToken, CharSequence msg) {
        emit(startToken, endToken, "Warning: ", msg);
    }

    void emitInformative(Token startToken, Token endToken, CharSequence msg) {
        emit(startToken, endToken, "Informative: ", msg);
    }

    void invalidValueError(char[] param, int start, String value, int length) {
        emitError(getTokenLocation(start, length), "\"" + value + "\" is an invalid value for option \"" + new String(param, start, length) + "\"");
    }

    void invalidTripletValueError(char[] param, int start, int length, String type, String format) {
        emitError(getTokenLocation(start, length), "Illegal " + type + " option specified: " + new String(param, start, length) + ". A value of the form \"" + format + "\" was expected.");
    }

    private String allocateString(OptionEnum op, char c) {
        return op.canonicalName + "='" + c + "'";
    }

    private String allocateString(OptionEnum op, String str) {
        StringBuilder buf = new StringBuilder(op.canonicalName);
        buf.append('=');
        if (str != null) {
            boolean needQuote = !str.startsWith("(");
            if (needQuote) buf.append('"');
            buf.append(str);
            if (needQuote) buf.append('"');
        } else buf.append("\"\"");
        return new String(buf);
    }

    private String allocateString(OptionEnum op, int i) {
        return op.canonicalName + '=' + i;
    }

    private String allocateString(OptionEnum op, boolean flag) {
        return flag ? op.canonicalName : "NO" + op.canonicalName;
    }

    private String allocateString(OptionEnum op, Enum enumeration) {
        return op.canonicalName + '=' + enumeration.name();
    }

    static int advancePastOption(char[] param, int start, int end) {
        if (start == end) return end;
        char c;
        while (!isDelimiter(c = param[start]) && c != '=') if (++start == end) return end;
        while (Character.isWhitespace(param[start])) if (++start == end) return end;
        if (param[start] == '=') {
            do if (++start == end) return end; while (Character.isWhitespace(param[start]));
            while (!isDelimiter(param[start])) if (++start == end) return end;
        }
        return start;
    }

    int classifyBadOption(char[] param, int start, int end, boolean flag) {
        int tail = advancePastOption(param, start, end);
        if (!flag) start -= 2;
        int length = tail - start;
        emitError(getTokenLocation(start, length), "\"" + new String(param, start, length) + "\" is an invalid option");
        return tail;
    }

    int reportAmbiguousOption(char[] param, int start, int end, boolean flag, String choiceMsg) {
        int tail = advancePastOption(param, start, end);
        if (!flag) start -= 2;
        int length = tail - start;
        emitError(getTokenLocation(start, length), "The option \"" + new String(param, start, length) + "\" is ambiguous: " + choiceMsg);
        return tail;
    }

    int reportMissingValue(char[] param, int start, int end) {
        int tail = advancePastOption(param, start, end);
        int length = tail - start;
        emitError(getTokenLocation(start, length), "A value is required for this option: \"" + new String(param, start, length) + "\"");
        return tail;
    }

    int reportValueNotRequired(char[] param, int start, int end) {
        int tail = advancePastOption(param, start, end);
        int length = tail - start;
        emitError(getTokenLocation(start, length), "An illegal value was specified for this option: \"" + new String(param, start, length) + "\"");
        return tail;
    }

    static boolean isDelimiter(char c) {
        return c == ',' || Character.isWhitespace(c);
    }

    static int cleanUp(char[] param, int start, int end) {
        while (start < end && isDelimiter(param[start])) start++;
        return start;
    }

    static int valuedOption(char[] param, int p, int end) {
        if (p == end) return -1;
        while (Character.isWhitespace(param[p])) if (++p == end) return -1;
        if (param[p] == '=') {
            while (++p < end && Character.isWhitespace(param[p])) ;
            return p;
        }
        return -1;
    }

    String getValue(char[] param, int start, int end) {
        tail = start;
        if (start == end) return "";
        while (tail < end && !isDelimiter(param[tail])) tail++;
        return new String(param, start, tail - start);
    }

    String getStringValue(char[] param, int start, int end) {
        tail = start;
        if (start == end) return "";
        char quote = param[start];
        if (quote == '\'' || quote == '\"') {
            while (++tail < end && param[tail] != quote) ;
            if (tail < end) {
                return new String(param, ++start, tail++ - start);
            }
            int length = tail - start;
            String str = new String(param, start, length);
            emitError(getTokenLocation(start, length), "The string " + str + "\" was not properly terminated");
            return str;
        }
        while (tail < end && !isDelimiter(param[tail])) tail++;
        return new String(param, start, tail - start);
    }

    private void processBlock(BlockInfo blockInfo) {
        String filename = expandFilename(blockInfo.filename);
        ActionFileSymbol filenameSymbol = actionBlocks.findFilename(filename);
        if (filenameSymbol != null) {
            emitError(blockInfo.location, "The action filename \"" + filename + "\" was previously associated with the begin block marker \"" + filenameSymbol.block().name() + "\" and cannot be reassociated with the block marker \"" + blockInfo.blockBegin + "\"");
        } else if (actionBlocks.findBlockname(blockInfo.blockBegin) != null) {
            emitError(blockInfo.location, "The action block begin string \"" + blockInfo.blockBegin + "\" was used in a previous block definition");
        } else {
            actionBlocks.insertBlock(blockInfo.location, BlockSymbol.Kind.MAIN_BLOCK, actionBlocks.findOrInsertFilename(filename), blockInfo.blockBegin, blockInfo.blockEnd);
        }
    }

    private void processHeaderOrTrailer(BlockInfo blockInfo, BlockSymbol.Kind blockKind) {
        String filename = expandFilename(blockInfo.filename);
        ActionFileSymbol filenameSymbol = actionBlocks.findFilename(filename);
        if (filenameSymbol != null) {
            if (actionBlocks.findBlockname(blockInfo.blockBegin) != null) {
                emitError(blockInfo.location, "The action block begin string \"" + blockInfo.blockBegin + "\" was used in a previous definition");
            } else {
                actionBlocks.insertBlock(blockInfo.location, blockKind, filenameSymbol, blockInfo.blockBegin, blockInfo.blockEnd);
            }
        } else {
            emitError(blockInfo.location, "The action filename \"" + filename + "\" must be associated with an action block before being used here");
        }
    }

    private void checkBlockMarker(Token markerLocation, String blockMarker) {
        if (blockMarker.length() == 1) {
            char markerChar = blockMarker.charAt(0);
            if (markerChar == escape) emitError(markerLocation, "The ESCAPE symbol, \"" + blockMarker + "\", cannot be used as a block marker."); else if (markerChar == orMarker) emitError(markerLocation, "The OR_MARKER symbol, \"" + blockMarker + "\", cannot be used as a block marker.");
        } else if (blockMarker.equals("::=")) emitError(markerLocation, "\"::=\" cannot be used as a block marker"); else if (blockMarker.equals("->")) emitError(markerLocation, "\"->\" cannot be used as a block marker");
    }

    private void checkGlobalOptionsConsistency() {
        if (orMarkerLocation == null) orMarkerLocation = lexStream.getTokenReference(0);
        if (escapeLocation == null) escapeLocation = lexStream.getTokenReference(0);
        if (orMarker == escape) emitError(orMarkerLocation, "The ESCAPE symbol, \"" + escape + "\", cannot be used as the OR_MARKER"); else if (orMarker == ':') emitError(orMarkerLocation, "\":\" cannot be used as the OR_MARKER"); else if (orMarker == '<') emitError(orMarkerLocation, "\"<\" cannot be used as the OR_MARKER"); else if (orMarker == '-') emitError(orMarkerLocation, "\"-\" cannot be used as the OR_MARKER"); else if (orMarker == '\'') emitError(orMarkerLocation, "\"'\" cannot be used as the OR_MARKER"); else if (orMarker == '\"') emitError(orMarkerLocation, "\" cannot be used as the OR_MARKER");
        if (escape == orMarker) emitError(escapeLocation, "The OR_MARKER symbol, \"" + orMarker + "\", cannot be used as the EACAPE"); else if (escape == ':') emitError(escapeLocation, "\":\" cannot be used as the ESCAPE"); else if (escape == '<') emitError(escapeLocation, "\"<\" cannot be used as the ESCAPE"); else if (escape == '-') emitError(escapeLocation, "\"-\" cannot be used as the ESCAPE"); else if (escape == '\'') emitError(escapeLocation, "\"'\" cannot be used as the ESCAPE"); else if (escape == '\"') emitError(escapeLocation, "\" cannot be used as ESCAPE");
        for (int i = 0, n = actionBlocks.numActionBlocks(); i < n; i++) {
            BlockSymbol block = actionBlocks.get(i);
            checkBlockMarker(block.location(), block.blockBegin());
            checkBlockMarker(block.location(), block.blockEnd());
        }
        for (List<BlockSymbol> blocks : actionBlocks.actionBlocks()) {
            for (int i = 0, n = blocks.size(); i < n; i++) {
                BlockSymbol block1 = blocks.get(i);
                for (int k = i + 1; k < n; k++) {
                    BlockSymbol block2 = blocks.get(k);
                    if (block1.blockBegin().length() < block2.blockBegin().length()) {
                        if (block2.blockBegin().startsWith(block1.blockBegin())) {
                            emitError(block1.location(), "The block marker \"" + block1.blockBegin() + "\" is a substring of the block marker \"" + block2.blockBegin() + "\"");
                        }
                    } else if (block1.blockBegin().startsWith(block2.blockBegin())) {
                        emitError(block2.location(), "The block marker \"" + block2.blockBegin() + "\" is a substring of the block marker \"" + block1.blockBegin() + "\"");
                    }
                }
            }
        }
    }

    private void checkAutomaticAst() {
        if (astDirectory == null) {
            astPackage = packageName;
            if (packageName.length() == 0) astDirectoryPrefix = "."; else {
                StringBuilder buf = new StringBuilder();
                int numDots = Utility.checkQualifiedIdentifier(packageName);
                for (int i = 0; i < numDots + 1; i++) buf.append("..").append(File.separatorChar);
                buf.append(packageName.replace('.', File.separatorChar));
                astDirectoryPrefix = new String(buf);
            }
        } else {
            if (astDirectoryLocation == null) astDirectoryLocation = lexStream.getTokenReference(0);
            astDirectoryPrefix = astDirectory;
            checkDirectory(astDirectoryLocation, astDirectory);
            File file = new File(astDirectory);
            List<String> pathNames = new ArrayList<String>();
            do {
                pathNames.add(file.getName());
                file = file.getParentFile();
            } while (file != null);
            int size = pathNames.size();
            int startAstPackage = size - 1;
            for (; startAstPackage >= 0; startAstPackage--) {
                String path = pathNames.get(startAstPackage);
                if (path.length() > 0 && !path.equals(".") && !path.equals("..")) break;
            }
            int temp = size - 1;
            for (; temp >= 0; temp--) {
                String path = pathNames.get(temp);
                if (path.length() > 0 && !path.equals(".")) break;
            }
            if (startAstPackage == temp) {
                if (packageName.length() == 0) {
                    astPackage = "";
                    emitError(astDirectoryLocation, "The ast package cannot be a subpackage of the unnamed package." + " Please specify a package name using the package option");
                } else astPackage = appendPackage(packageName, pathNames, startAstPackage);
            } else astPackage = appendPackage("", pathNames, startAstPackage);
        }
    }

    private String appendPackage(String name, List<String> pathNames, int startAstPackage) {
        StringBuilder buf = new StringBuilder(name);
        boolean first = (name.length() == 0);
        for (; startAstPackage >= 0; startAstPackage--) {
            if (first) first = false; else buf.append('.');
            buf.append(pathNames.get(startAstPackage));
        }
        return new String(buf);
    }

    private void processOptions(char[] param, int start, int end) {
        OptionParser optionParser = new OptionParser(this, param, end);
        while ((start = cleanUp(param, start, end)) < end) {
            char c = param[start];
            boolean flag = !((c == 'n' || c == 'N') && ((c = param[start + 1]) == 'o' || c == 'O'));
            if (!flag) start += 2;
            start = optionParser.classifyOption(start, flag);
        }
    }

    void processUserOptions(InputFileSymbol inputFileSymbol, char[] line, int length, int start) {
        this.inputFileSymbol = inputFileSymbol;
        processOptions(line, length, start);
    }

    void processCommandOptions() {
        this.inputFileSymbol = null;
        StringBuilder buf = new StringBuilder();
        for (int m = 0, n = args.length - 1; m < n; m++) {
            String arg = args[m];
            if (arg.startsWith("-")) {
                buf.append(arg.substring(1));
            } else {
                emitError(0, "Option \"" + arg + "\" is missing preceding '-'");
                buf.append(arg);
            }
            buf.append(',');
        }
        buf.append('\n');
        int length = buf.length();
        char[] param = new char[length];
        buf.getChars(0, length, param, 0);
        processOptions(param, 0, length);
    }

    static void processPath(List<String> list, String paths) {
        if (paths != null) {
            for (String path : paths.split(";")) {
                list.add(path.trim());
            }
        }
    }

    static void processPath(List<String> list, String paths, String startDirectory) {
        list.add(startDirectory);
        processPath(list, paths);
    }

    private String getFile(String directory, String fileSuffix, String fileType) {
        return new File(directory, filePrefix + fileSuffix + fileType).getPath();
    }

    private String getType(String filespec) {
        String filename = new File(filespec).getName();
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? filename : filename.substring(0, dot);
    }

    private String expandFilename(String filename) {
        return (filename.startsWith("*") ? getFile(outDirectory, filename.substring(1), "") : filename);
    }

    private void checkDirectory(Token directoryLocation, String directory) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            if (dir.exists()) emitError(directoryLocation, "The file \"" + directory + "\" is not a directory"); else if (!dir.mkdirs()) emitError(directoryLocation, "Unable to create directory: \"" + directory + "\"");
        }
    }

    void completeOptionProcessing() {
        if (escape == ' ') {
            escape = (programmingLanguage == ProgrammingLanguage.JAVA || programmingLanguage == ProgrammingLanguage.C || programmingLanguage == ProgrammingLanguage.CPP ? '$' : '%');
        }
        if (packageName == null) packageName = "";
        if (prefix == null) prefix = "";
        if (suffix == null) suffix = "";
        if (templateName == null) templateName = "";
        assert (filePrefix != null);
        if (outDirectory == null) outDirectory = homeDirectory; else checkDirectory(outDirectoryLocation, outDirectory);
        for (int i = 0, n = actionOptions.size(); i < n; i++) processBlock(actionOptions.get(i));
        for (int i = 0, n = headerOptions.size(); i < n; i++) processHeaderOrTrailer(headerOptions.get(i), BlockSymbol.Kind.HEADER_BLOCK);
        for (int i = 0, n = trailerOptions.size(); i < n; i++) processHeaderOrTrailer(trailerOptions.get(i), BlockSymbol.Kind.TRAILER_BLOCK);
        defaultBlock = actionBlocks.findBlockname(DEFAULT_BLOCK_BEGIN);
        if (defaultBlock == null) {
            assert (actionBlocks.findFilename("") == null);
            BlockInfo defaultInfo = new BlockInfo(lexStream.getTokenReference(0), "", DEFAULT_BLOCK_BEGIN, DEFAULT_BLOCK_END);
            processBlock(defaultInfo);
            defaultBlock = actionBlocks.findBlockname(DEFAULT_BLOCK_BEGIN);
        }
        defaultActionFile = defaultBlock.actionFileSymbol();
        defaultActionPrefix = new File(defaultActionFile.name()).getParent();
        actionType = getType(defaultActionFile.name());
        checkGlobalOptionsConsistency();
        if (astDirectory != null) {
            astDirectory = astDirectory.trim();
            if (astDirectory.length() == 0) astDirectory = null;
        }
        if (automaticAst != AutomaticAst.NONE) {
            if (variables == Variables.NONE) variables = Variables.BOTH;
            if (automaticAst == AutomaticAst.TOPLEVEL) checkAutomaticAst();
        }
        if (astPackage == null) astPackage = "";
        if (astType == null) astType = "Ast";
        if (visitorType == null) visitorType = "Visitor";
        if (prsFile == null) prsFile = getFile(outDirectory, "prs.", programmingLanguage.headerSuffix);
        prsType = getType(prsFile);
        if (symFile == null) symFile = getFile(outDirectory, "sym.", programmingLanguage.headerSuffix);
        symType = getType(symFile);
        if (datDirectory == null) datDirectory = homeDirectory; else checkDirectory(datDirectoryLocation, datDirectory);
        if (datFile == null) datFile = getFile(datDirectory, "dcl.", "data");
        if (dclFile == null) {
            dclFile = getFile(outDirectory, programmingLanguage == ProgrammingLanguage.C || programmingLanguage == ProgrammingLanguage.CPP ? "prs." : "dcl.", programmingLanguage == ProgrammingLanguage.PLXASM ? "assemble" : programmingLanguage.suffix);
        }
        dclType = getType(dclFile);
        if (defFile == null) defFile = getFile(outDirectory, "def.", programmingLanguage.suffix);
        defType = getType(defFile);
        if (directoryPrefix != null) {
            directoryPrefix = directoryPrefix.trim();
            if (directoryPrefix.length() == 0) directoryPrefix = null;
        }
        if (expFile == null) expFile = getFile(outDirectory, "exp.", programmingLanguage.headerSuffix); else expFile = expandFilename(expFile);
        expType = getType(expFile);
        if (expPrefix == null) expPrefix = "";
        if (expSuffix == null) expSuffix = "";
        if (factory == null) factory = "new ";
        if (impFile == null) impFile = getFile(outDirectory, "imp.", programmingLanguage.headerSuffix);
        impType = getType(impFile);
        if (softKeywords) {
            lalrLevel = 1;
            singleProductions = false;
            backtrack = true;
        }
        if (glr) {
            lalrLevel = 1;
            singleProductions = false;
            backtrack = true;
        }
        if (verbose) {
            first = true;
            follow = true;
            list = true;
            states = true;
            xref = true;
            warnings = true;
        }
    }

    void printOptionsInEffect() {
        if (quiet) return;
        assert (grmFile != null);
        DispatchWriter optWriter = new DispatchWriter();
        optWriter.setOutputBuffer(report);
        optWriter.write("\nOptions in effect for ");
        optWriter.write(grmFile);
        optWriter.write(":\n\n");
        for (int i = 0, n = actionBlocks.numActionBlocks(); i < n; i++) {
            BlockSymbol block = actionBlocks.get(i);
            optWriter.write("    ");
            optWriter.write(block.kind().option.canonicalName);
            optWriter.write("=(\"");
            optWriter.write(block.actionFileSymbol().name());
            optWriter.write("\",\"");
            optWriter.write(block.blockBegin());
            optWriter.write("\",\"");
            optWriter.write(block.blockEnd());
            optWriter.write("\")");
            if (actionBlocks.isIgnoredBlock(block.blockBegin())) optWriter.write(" : IGNORED");
            optWriter.write('\n');
        }
        optWriter.write('\n');
        optWriter.beginAutoWrap();
        optWriter.setIndentSize(4);
        optWriter.setSeparatorSize(2);
        optWriter.write(allocateString(OptionEnum.AST_DIRECTORY, astDirectory));
        optWriter.write(allocateString(OptionEnum.AST_TYPE, astType));
        optWriter.write(allocateString(OptionEnum.ATTRIBUTES, attributes));
        optWriter.write(automaticAst == AutomaticAst.NONE ? allocateString(OptionEnum.AUTOMATIC_AST, false) : allocateString(OptionEnum.AUTOMATIC_AST, automaticAst));
        optWriter.write(allocateString(OptionEnum.BACKTRACK, backtrack));
        if (byteFlag) optWriter.write(allocateString(OptionEnum.BYTE, true));
        optWriter.write(allocateString(OptionEnum.CONFLICTS, conflicts));
        optWriter.write(allocateString(OptionEnum.DAT_DIRECTORY, datDirectory));
        optWriter.write(allocateString(OptionEnum.DAT_FILE, datFile));
        optWriter.write(allocateString(OptionEnum.DCL_FILE, dclFile));
        optWriter.write(allocateString(OptionEnum.DEBUG, debug));
        optWriter.write(allocateString(OptionEnum.DEF_FILE, defFile));
        optWriter.write(allocateString(OptionEnum.DIRECTORY_PREFIX, directoryPrefix));
        optWriter.write(allocateString(OptionEnum.EDIT, edit));
        optWriter.write(allocateString(OptionEnum.ERROR_MAPS, errorMaps));
        optWriter.write(allocateString(OptionEnum.ESCAPE, escape));
        optWriter.write(allocateString(OptionEnum.EXPORT_TERMINALS, "(\"" + expFile + "\",\"" + expPrefix + "\",\"" + expSuffix + "\")"));
        if (extendsParsetable == null) optWriter.write(allocateString(OptionEnum.EXTENDS_PARSETABLE, false)); else if (extendsParsetable.length() == 0) optWriter.write(allocateString(OptionEnum.EXTENDS_PARSETABLE, true)); else optWriter.write(allocateString(OptionEnum.EXTENDS_PARSETABLE, extendsParsetable));
        optWriter.write(allocateString(OptionEnum.FACTORY, factory));
        optWriter.write(allocateString(OptionEnum.FILE_PREFIX, filePrefix));
        if (filter != null) optWriter.write(allocateString(OptionEnum.FILTER, filter));
        optWriter.write(allocateString(OptionEnum.FIRST, first));
        optWriter.write(allocateString(OptionEnum.FOLLOW, follow));
        optWriter.write(allocateString(OptionEnum.GLR, glr));
        optWriter.write(allocateString(OptionEnum.GOTO_DEFAULT, gotoDefault));
        optWriter.write("GRM-FILE=\"" + grmFile + "\"");
        if (impFile != null) optWriter.write(allocateString(OptionEnum.IMP_FILE, impFile));
        if (importTerminals != null) optWriter.write(allocateString(OptionEnum.IMPORT_TERMINALS, importTerminals));
        optWriter.write(allocateString(OptionEnum.INCLUDE_DIRECTORY, includeDirectory));
        if (!slr) optWriter.write(allocateString(OptionEnum.LALR, lalrLevel));
        optWriter.write(allocateString(OptionEnum.LEGACY, legacy));
        optWriter.write(allocateString(OptionEnum.LIST, list));
        optWriter.write(allocateString(OptionEnum.MARGIN, margin));
        optWriter.write(allocateString(OptionEnum.MAX_CASES, maxCases));
        optWriter.write(allocateString(OptionEnum.NAMES, names));
        optWriter.write(allocateString(OptionEnum.NT_CHECK, ntCheck));
        optWriter.write(allocateString(OptionEnum.OR_MARKER, orMarker));
        optWriter.write(allocateString(OptionEnum.OUT_DIRECTORY, outDirectory));
        optWriter.write(allocateString(OptionEnum.PACKAGE, packageName));
        optWriter.write(allocateString(OptionEnum.PARENT_SAVED, parentSaved));
        if (parsetableInterfaces == null) optWriter.write(allocateString(OptionEnum.PARSETABLE_INTERFACES, false)); else if (parsetableInterfaces.length() == 0) optWriter.write(allocateString(OptionEnum.PARSETABLE_INTERFACES, true)); else optWriter.write(allocateString(OptionEnum.PARSETABLE_INTERFACES, parsetableInterfaces));
        optWriter.write(allocateString(OptionEnum.PREFIX, prefix));
        optWriter.write(allocateString(OptionEnum.PRIORITY, priority));
        optWriter.write(allocateString(OptionEnum.PROGRAMMING_LANGUAGE, programmingLanguage));
        optWriter.write(allocateString(OptionEnum.PROSTHESES, prostheses));
        optWriter.write(allocateString(OptionEnum.PRS_FILE, prsFile));
        optWriter.write(allocateString(OptionEnum.QUIET, quiet));
        optWriter.write(allocateString(OptionEnum.READ_REDUCE, readReduce));
        optWriter.write(allocateString(OptionEnum.REMAP_TERMINALS, remapTerminals));
        optWriter.write(allocateString(OptionEnum.SCOPES, scopes));
        optWriter.write(allocateString(OptionEnum.SERIALIZE, serialize));
        optWriter.write(allocateString(OptionEnum.SHIFT_DEFAULT, shiftDefault));
        if (!byteFlag) optWriter.write("SHORT");
        optWriter.write(allocateString(OptionEnum.SINGLE_PRODUCTIONS, singleProductions));
        if (slr) optWriter.write(allocateString(OptionEnum.SLR, true));
        optWriter.write(allocateString(OptionEnum.SOFT_KEYWORDS, softKeywords));
        optWriter.write(allocateString(OptionEnum.STATES, states));
        optWriter.write(allocateString(OptionEnum.SUFFIX, suffix));
        optWriter.write(allocateString(OptionEnum.SYM_FILE, symFile));
        optWriter.write(allocateString(OptionEnum.TAB_FILE, tabFile));
        optWriter.write(allocateString(OptionEnum.TABLE, table));
        optWriter.write(allocateString(OptionEnum.TEMPLATE, templateName));
        optWriter.write(trace == Trace.NONE ? allocateString(OptionEnum.TRACE, false) : allocateString(OptionEnum.TRACE, trace));
        optWriter.write(variables == Variables.NONE ? allocateString(OptionEnum.VARIABLES, false) : variables == Variables.BOTH ? allocateString(OptionEnum.VARIABLES, true) : allocateString(OptionEnum.VARIABLES, variables));
        optWriter.write(allocateString(OptionEnum.VERBOSE, verbose));
        optWriter.write(visitor == Visitor.NONE ? allocateString(OptionEnum.VISITOR, false) : allocateString(OptionEnum.VISITOR, visitor));
        optWriter.write(allocateString(OptionEnum.VISITOR_TYPE, visitorType));
        optWriter.write(allocateString(OptionEnum.WARNINGS, warnings));
        optWriter.write(allocateString(OptionEnum.XREFERENCE, xref));
        optWriter.endAutoWrap();
        optWriter.close();
    }

    static void printOptionsList() {
        System.out.println();
        System.out.println(Control.HEADER_INFO);
        System.out.println("Usage: lpg.Main [options] [filename[.extension]]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("========");
        System.out.println();
        for (OptionEnum option : EnumSet.allOf(OptionEnum.class)) {
            System.out.println(option.getDescription());
        }
        System.out.println();
        System.out.println("Options must be separated by a space.  Any non-ambiguous initial prefix of a");
        System.out.println("valid option may be used as an abbreviation for that option.  When an option is");
        System.out.println("composed of two separate words, an abbreviation may be formed by concatenating");
        System.out.println("the first character of each word.  Options that are switches may be negated by");
        System.out.println("prefixing them with the string \"no\".  Default input file extension is \".g\"");
        System.out.println();
        System.out.println("Version " + Control.VERSION);
    }

    Blocks actionBlocks() {
        return actionBlocks;
    }

    BlockSymbol defaultBlock() {
        return defaultBlock;
    }

    ActionFileSymbol defaultActionFile() {
        return defaultActionFile;
    }

    String defaultActionPrefix() {
        return defaultActionPrefix;
    }

    private final String[] args;

    final DispatchWriter _syslis;

    final BufferedWriter syslis;

    private FileOutputStream syslisFos = null;

    boolean errorFlag = true;

    final String homeDirectory;

    final List<String> includeSearchDirectory = new ArrayList<String>();

    final List<String> templateSearchDirectory = new ArrayList<String>();

    final List<String> filterFile = new ArrayList<String>();

    final List<String> importFile = new ArrayList<String>();

    private String templateDirectory;

    private String astDirectoryPrefix;

    boolean attributes = false;

    boolean backtrack = false;

    boolean legacy = true;

    boolean list = false;

    boolean glr = false;

    boolean slr = false;

    boolean verbose = false;

    boolean first = false;

    boolean follow = false;

    boolean priority = true;

    boolean edit = false;

    boolean states = false;

    boolean xref = false;

    boolean ntCheck = false;

    boolean conflicts = true;

    boolean readReduce = true;

    boolean remapTerminals = true;

    boolean gotoDefault = false;

    boolean shiftDefault = false;

    boolean byteFlag = true;

    boolean warnings = true;

    boolean singleProductions = false;

    boolean errorMaps = false;

    boolean debug = false;

    boolean parentSaved = false;

    boolean scopes = false;

    boolean serialize = false;

    boolean softKeywords = false;

    boolean table = false;

    int lalrLevel = 1;

    int margin = 0;

    int maxCases = 1024;

    Names names = Names.OPTIMIZED;

    Trace trace = Trace.CONFLICTS;

    ProgrammingLanguage programmingLanguage = ProgrammingLanguage.XML;

    Prostheses prostheses = Prostheses.OPTIMIZED;

    AutomaticAst automaticAst = AutomaticAst.NONE;

    Variables variables = Variables.NONE;

    Visitor visitor = Visitor.NONE;

    char escape = ' ';

    char orMarker = '|';

    String factory = null;

    String filePrefix = null;

    String grmFile = null;

    String lisFile = null;

    String tabFile = null;

    String datDirectory = null;

    String datFile = null;

    String dclFile = null;

    String defFile = null;

    String directoryPrefix = null;

    String prsFile = null;

    String symFile = null;

    String impFile = null;

    String expFile = null;

    String expPrefix = null;

    String expSuffix = null;

    String outDirectory = null;

    String astDirectory = null;

    String astPackage = null;

    String astType = null;

    String expType = null;

    String prsType = null;

    String symType = null;

    String dclType = null;

    String impType = null;

    String defType = null;

    String actionType = null;

    String visitorType = null;

    String filter = null;

    String importTerminals = null;

    String includeDirectory = null;

    String templateName = null;

    String extendsParsetable = null;

    String parsetableInterfaces = null;

    String packageName = null;

    String prefix = null;

    String suffix = null;

    boolean quiet = false;

    StringBuilder report = new StringBuilder();

    private LexStream lexStream = null;

    List<BlockInfo> actionOptions = new ArrayList<BlockInfo>();

    List<BlockInfo> headerOptions = new ArrayList<BlockInfo>();

    List<BlockInfo> trailerOptions = new ArrayList<BlockInfo>();

    Token datDirectoryLocation = null;

    Token outDirectoryLocation = null;

    Token astDirectoryLocation = null;

    Token escapeLocation = null;

    Token orMarkerLocation = null;

    Blocks actionBlocks = new Blocks();

    static final String DEFAULT_BLOCK_BEGIN = "/.";

    static final String DEFAULT_BLOCK_END = "./";

    private InputFileSymbol inputFileSymbol;

    private BlockSymbol defaultBlock = null;

    private ActionFileSymbol defaultActionFile = null;

    private String defaultActionPrefix = null;

    int tail = 0;

    static class BlockInfo {

        BlockInfo(Token location, String filename, String blockBegin, String blockEnd) {
            this.location = location;
            this.filename = filename;
            this.blockBegin = blockBegin;
            this.blockEnd = blockEnd;
        }

        final Token location;

        final String filename;

        final String blockBegin;

        final String blockEnd;
    }

    static enum Names {

        OPTIMIZED, MINIMUM, MAXIMUM
    }

    static enum ProgrammingLanguage {

        XML("xml"), C("h", "c"), CPP("h", "cpp"), JAVA("java"), PLX("copy"), PLXASM("copy"), ML("ml");

        ProgrammingLanguage(String suffix) {
            this(suffix, suffix);
        }

        ProgrammingLanguage(String headerSuffix, String suffix) {
            this.headerSuffix = headerSuffix;
            this.suffix = suffix;
        }

        private final String headerSuffix;

        private final String suffix;
    }

    static enum Prostheses {

        OPTIMIZED, MINIMUM, MAXIMUM
    }

    static enum Trace {

        NONE, CONFLICTS, FULL
    }

    static enum AutomaticAst {

        NONE, NESTED, TOPLEVEL
    }

    static enum Variables {

        NONE, BOTH, NON_TERMINALS, TERMINALS
    }

    static enum Visitor {

        NONE, DEFAULT, PREORDER
    }
}
