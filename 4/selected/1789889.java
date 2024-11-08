package utils.FPacTranscoder;

import jlib.engine.NotificationEngine;
import jlib.misc.FileSystem;
import jlib.xml.Tag;
import generate.CJavaFPacEntityFactory;
import generate.fpacjava.CFPacJavaClass;
import generate.java.CJavaExporter;
import parser.CParser;
import parser.FPac.CFPacParser;
import parser.FPac.elements.CFPacScript;
import semantic.CBaseEntityFactory;
import utils.Transcoder;
import utils.TranscoderEngine;
import utils.CGlobalEntityCounter;
import utils.CObjectCatalog;
import utils.CTransApplicationGroup;
import lexer.CBaseLexer;
import lexer.CTokenList;
import lexer.FPac.CFPacLexer;

public class FPacTranscoderEngine extends TranscoderEngine<CFPacScript, CFPacJavaClass> {

    @Override
    protected CBaseLexer getLexer() {
        return new CFPacLexer();
    }

    @Override
    protected CParser<CFPacScript> doParsing(CTokenList lst) {
        CParser<CFPacScript> parser = new CFPacParser();
        if (parser.StartParsing(lst)) {
            CGlobalEntityCounter.GetInstance().CountCobolFile();
            return parser;
        } else {
            Transcoder.logError("FILEPAC parsing failed");
            return null;
        }
    }

    @Override
    protected CFPacJavaClass doSemanticAnalysis(CParser<CFPacScript> parser, String fileName, CObjectCatalog cat, CTransApplicationGroup grp, boolean bResources) {
        cat.RegisterNotifHandler(new DefaultFileManager());
        CJavaExporter out = new CJavaExporter(cat.m_Listing, fileName, parser.m_CommentContainer, bResources);
        cat.setExporter(out);
        CJavaFPacEntityFactory factory = new CJavaFPacEntityFactory(cat, out);
        InitCustomCICSEntriesFromRules(factory);
        CFPacScript prg = parser.GetRootElement();
        prg.setName(FileSystem.getNameWithoutExtension(fileName));
        CFPacJavaClass eSem = prg.DoSemanticAnalysis(factory);
        parser.m_CommentContainer.DoSemanticAnalysis(factory);
        return eSem;
    }

    protected void InitCustomCICSEntriesFromRules(CBaseEntityFactory factory) {
        int nb = m_RulesManager.getNbRules("environmentVariableFPac");
        for (int i = 0; i < nb; i++) {
            Tag e = m_RulesManager.getRule("environmentVariableFPac", i);
            String name = e.getVal("name");
            String read = e.getVal("methodeRead");
            String write = e.getVal("methodeWrite");
            boolean bNumeric = e.getValAsBoolean("Numeric");
            factory.NewEntityEnvironmentVariable(name, read, write, bNumeric);
        }
        nb = m_RulesManager.getNbRules("routineEmulation");
        for (int i = 0; i < nb; i++) {
            Tag e = m_RulesManager.getRule("routineEmulation", i);
            String name = e.getVal("routine");
            String method = e.getVal("method");
            String csRequiredToolsLib = e.getVal("requiredToolsLib", null);
            factory.m_ProgramCatalog.RegisterRoutineEmulation(name, method, csRequiredToolsLib);
        }
        nb = m_RulesManager.getNbRules("routineEmulationExternal");
        for (int i = 0; i < nb; i++) {
            Tag e = m_RulesManager.getRule("routineEmulation", i);
            String name = e.getVal("routine");
            String method = e.getVal("method");
            factory.m_ProgramCatalog.RegisterRoutineEmulation(name, method, true);
        }
    }

    @Override
    protected void doLogs(String csInput, String csOutput) {
        Transcoder.logInfo("Start transcoding file to " + csOutput);
    }

    @Override
    protected void doPopulateSpecialActionHandlers(NotificationEngine engine) {
    }

    /**
	 * @see utils.BaseEngine#CustomInit(jlib.xml.Tag)
	 */
    @Override
    public boolean CustomInit(Tag tagTrans) {
        return true;
    }

    /**
	 * @see utils.TranscoderEngine#generateOutputFileName(java.lang.String)
	 */
    @Override
    protected String generateOutputFileName(String filename) {
        return ReplaceExtensionFileName(filename, "java");
    }

    /**
	 * @see utils.TranscoderEngine#generateInputFileName(java.lang.String)
	 */
    @Override
    public String generateInputFileName(String filename) {
        return filename;
    }
}
