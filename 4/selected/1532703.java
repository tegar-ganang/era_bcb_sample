package net.firstpartners.drools;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import net.firstpartners.RedConstants;
import net.firstpartners.drools.data.RuleSource;
import net.firstpartners.security.RedSecurityManager;
import org.apache.commons.codec.binary.Base64;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.common.DroolsObjectInputStream;
import org.drools.compiler.DroolsParserException;
import org.drools.definition.KnowledgePackage;
import org.drools.io.ResourceFactory;

public abstract class AbstractRuleLoader implements IRuleLoader {

    private static final Logger log = Logger.getLogger(AbstractRuleLoader.class.getName());

    /**
	 * Load multiple rules, with optional dsl and ruleflow file
	 * 
	 * @param rulesUrl
	 * @param dslFileUrl
	 * @param ruleFlowUrl
	 * @return
	 * @throws IOException
	 * @throws DroolsParserException
	 * @throws ClassNotFoundException
	 * @throws Exception
	 */
    public KnowledgeBase loadRules(RuleSource ruleSource) throws DroolsParserException, IOException, ClassNotFoundException {
        if (ruleSource.getKnowledgeBaseLocation() != null) {
            return loadKnowledgeBase(ruleSource);
        }
        KnowledgeBuilder localBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        for (String ruleFile : ruleSource.getRulesLocation()) {
            log.info("Loading file: " + ruleFile);
            if (ruleFile.endsWith(RedConstants.XLS_FILE_EXTENSION)) {
                log.info("Loading Excel file: " + ruleFile);
                loadExcelRules(ruleFile, localBuilder);
            } else {
                log.info("Loading Drl file: " + ruleFile);
                loadDrlRules(ruleFile, ruleSource.getDslFileLocation(), ruleSource.getRuleFlowFileUrl(), localBuilder);
            }
        }
        if (localBuilder.hasErrors()) {
            log.severe("Drools Errors");
            KnowledgeBuilderErrors errors = localBuilder.getErrors();
            Iterator<KnowledgeBuilderError> itErrors = errors.iterator();
            int[] errorLines;
            StringBuffer errorLineMessage;
            while (itErrors.hasNext()) {
                KnowledgeBuilderError thisError = itErrors.next();
                log.severe(thisError.getMessage());
                log.severe(thisError.toString());
                errorLines = thisError.getErrorLines();
                errorLineMessage = new StringBuffer();
                if (errorLines != null) {
                    for (int errorLine : errorLines) {
                        errorLineMessage.append(errorLine);
                        errorLineMessage.append(",");
                    }
                    log.severe("Error Lines:" + errorLineMessage.toString());
                }
            }
            log.severe("****/nDrools Errors:" + localBuilder.getErrors().toString());
            log.severe("****/nEnd Drools Errors");
            throw new RuntimeException("Error in Rules File:" + localBuilder.getErrors().toString());
        } else {
            log.info("No Drools Errors");
        }
        logKnowledgePackages(localBuilder);
        log.info("Creating new knowledgebase");
        KnowledgeBase localBase = KnowledgeBaseFactory.newKnowledgeBase();
        log.info("Adding packages to localBase");
        localBase.addKnowledgePackages(localBuilder.getKnowledgePackages());
        return localBase;
    }

    /**
	 * Print out what we know of the built packages
	 * 
	 * @param localBuilder
	 */
    void logKnowledgePackages(KnowledgeBuilder localBuilder) {
        Collection<KnowledgePackage> kpCollection = localBuilder.getKnowledgePackages();
        log.info("Number of packages" + kpCollection.size());
        Iterator<KnowledgePackage> kpIterator = kpCollection.iterator();
        while (kpIterator.hasNext()) {
            log.info(kpIterator.next().toString());
        }
    }

    /**
	 * Load the rule (i.e. non Excel) file
	 * 
	 * @param ruleUrl
	 * @param dslFileUrl
	 * @param ruleFlowFileUrl
	 * @param addRulesToThisBuilder
	 * @throws DroolsParserException
	 * @throws IOException
	 */
    private void loadDrlRules(String ruleUrl, String dslFileUrl, String ruleFlowFileUrl, KnowledgeBuilder addRulesToThisBuilder) throws DroolsParserException, IOException {
        log.info("Loading Main rule file");
        Reader ruleFileAsReader = getReader(ruleUrl);
        addRulesToThisBuilder.add(ResourceFactory.newReaderResource(ruleFileAsReader), ResourceType.DRL);
        ruleFileAsReader.close();
        if (dslFileUrl != null) {
            log.info("Loading DSL file");
            Reader dslFileAsReader = getReader(dslFileUrl);
            addRulesToThisBuilder.add(ResourceFactory.newReaderResource(dslFileAsReader), ResourceType.DSL);
            dslFileAsReader.close();
        }
        if (ruleFlowFileUrl != null) {
            log.info("Loading RuleFlow file");
            Reader ruleFlowAsReader = getReader(ruleFlowFileUrl);
            addRulesToThisBuilder.add(ResourceFactory.newReaderResource(ruleFlowAsReader), ResourceType.DSLR);
        }
        log.info("Finished Loading rule files");
    }

    /**
	 * Load Excel Rules
	 * 
	 * @param excelRuleFileUrl
	 * @param addRulesToThisBuilder
	 * @throws DroolsParserException
	 * @throws IOException
	 */
    private void loadExcelRules(String excelRuleFileUrl, KnowledgeBuilder addRulesToThisBuilder) throws DroolsParserException, IOException {
        byte[] excelAsBytes = getFile(excelRuleFileUrl);
        addRulesToThisBuilder.add(ResourceFactory.newByteArrayResource(excelAsBytes), ResourceType.DTABLE);
    }

    /**
	 * Abstract methods, implented in sub classes
	 * 
	 * @param excelRuleFileUrl
	 * @return
	 * @throws IOException
	 */
    abstract byte[] getFile(String excelRuleFileResource) throws IOException;

    /**
	 * Get a reader for a given resource
	 * 
	 * @param ruleFlowFileUrl
	 * @return
	 * @throws IOException
	 */
    abstract Reader getReader(String resource) throws IOException;

    /**
	 * Get an InputStream for a given resource
	 * 
	 * @param resource
	 *            to find
	 * @return
	 * @throws IOException
	 */
    abstract InputStream getInputStream(String resource) throws IOException;

    /**
	 * Load a previously cached resource (that has been saved using Base64)
	 * 
	 * 
	 * @param cacheResourceUnderName
	 * @return - the first serialized Knowledgebase in the file
	 * @throws DroolsParserException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    KnowledgeBase loadKnowledgeBase(RuleSource ruleSource) throws IOException, ClassNotFoundException, SecurityException {
        RedSecurityManager.checkUrl(ruleSource);
        BufferedInputStream inStream = new BufferedInputStream(getInputStream(ruleSource.getKnowledgeBaseLocation()));
        StringBuffer inString = new StringBuffer();
        ByteArrayOutputStream holdStream = new ByteArrayOutputStream();
        while (inStream.available() != 0) {
            holdStream.write(inStream.read());
        }
        byte[] base64Bytes = holdStream.toByteArray();
        byte[] binaryBytes = Base64.decodeBase64(base64Bytes);
        DroolsObjectInputStream in = new DroolsObjectInputStream(new ByteArrayInputStream(binaryBytes));
        Object inObject = in.readObject();
        log.info("inObject:" + inObject.getClass());
        return (KnowledgeBase) inObject;
    }
}
