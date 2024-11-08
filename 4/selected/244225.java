package org.rascalli.mbe.mmg;

import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.rascalli.framework.config.ConfigService;
import org.rascalli.framework.core.CommunicationChannel;
import org.rascalli.mbe.AbstractTool;
import org.rascalli.mbe.DialogueHistory;
import org.rascalli.mbe.MBEAgent;
import org.rascalli.mbe.RdfData;
import org.rascalli.mbe.ToolID;
import org.rascalli.mbe.mmg.MMGInputRepresentation.ContentType;
import org.rascalli.webui.ws.RascalliWSImpl;
import org.rascalli.webui.ws.RascalloResponse;

public class MMGTool extends AbstractTool {

    private final String WEBUI_URL = "http://intralife.researchstudio.at/rascalli/";

    private RascalliWSImpl rascalliWs;

    private final HashMap<String, String> timing;

    private final String ontologyPath;

    private DataStore dataStore;

    private String templateFile;

    private final String maleVoice = "jmk-arctic";

    private final String femaleVoice = "slt-arctic";

    private int sessionIdNumber;

    private QuestionAnalysis qa;

    private OntologyStructure os1;

    private final Random random;

    private final Log log = LogFactory.getLog(getClass());

    private MBEAgent agent;

    private boolean post;

    private final int maxLength = 28;

    private final String jabberTemplateFile = "templates/jabber.vm";

    private final String webTemplateFile = "templates/webinterface.vm";

    private Repository rascalliRepository;

    private String repositoryUrl = "http://rascalli.sirma.bg/openrdf-sesame";

    private String repositoryName = "rascalli";

    private String confDir;

    private boolean inputTooLong;

    private boolean firstLogin;

    public MMGTool() {
        super(ToolID.MMG.toString());
        ontologyPath = "gossipontology.owl";
        post = true;
        try {
            os1 = new OntologyStructure("http://www.owl-ontologies.com/gossipontology.owl#");
            os1.setOntologyPath(ontologyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        random = new Random(System.currentTimeMillis());
        sessionIdNumber = 1;
        inputTooLong = false;
        timing = new HashMap<String, String>();
        timing.put("start", "500ms");
        timing.put("break", "250ms");
        firstLogin = true;
    }

    public void setConfigService(ConfigService configService) throws Exception {
        final URL templateDirUrl = configService.getConfigFileUrl("mmg");
        if (log.isDebugEnabled()) {
            log.debug("template dir url: " + templateDirUrl);
        }
        confDir = templateDirUrl.getPath();
        Properties p = new Properties();
        p.setProperty("resource.loader", "file");
        p.setProperty("file.resource.loader.path", templateDirUrl.getPath());
        p.setProperty("velocimacro.permissions.allow.inline.to.replace.global", "true");
        p.setProperty("velocimacro.library", "templates/macros.vm");
        Velocity.init(p);
        if (log.isDebugEnabled()) {
            log.debug("file.resource.loader.path: " + Velocity.getProperty("file.resource.loader.path"));
        }
    }

    public void setMBEAgent(MBEAgent agent) {
        this.agent = agent;
    }

    public MMGOutput generateOutput(RdfData data) throws Exception {
        MMGInput ir = new MMGInputRepresentation(data);
        return generateOutput(ir);
    }

    public MMGOutput generateOutput(MMGInput ir) throws Exception {
        inputTooLong = false;
        final Thread currentThread = Thread.currentThread();
        final ClassLoader oldCCL = currentThread.getContextClassLoader();
        if (qa == null) {
            if (log.isWarnEnabled()) {
                log.warn("T-MMG: QuestionAnalysis is not set.");
            }
        }
        try {
            currentThread.setContextClassLoader(getClass().getClassLoader());
            String dialogueAct = ir.getDialogueAct();
            dataStore = new DataStore();
            String inputString;
            if (ir.getContentType() == ContentType.CONCEPTS) {
                try {
                    if (rascalliRepository == null) {
                        rascalliRepository = new HTTPRepository(repositoryUrl, repositoryName);
                        rascalliRepository.initialize();
                    }
                    GraphToUtterance ga = new GraphToUtterance();
                    ga.setRascalliRepositoryConnection(rascalliRepository.getConnection());
                    Templates templates = TemplateReader.readTemplates(confDir + "templatelist.txt");
                    Utterance utterance = ga.analyze(ir.getRdfData().getRepository().getConnection());
                    utterance.setTemplates(templates);
                    utterance.buildSentenceTrees();
                    inputString = utterance.generateUtterance(templates);
                } catch (RepositoryException e) {
                    if (log.isErrorEnabled()) {
                        log.error("Rascalli Repository not available, " + e);
                    }
                    inputString = ir.getTextValue();
                }
            } else {
                inputString = ir.getTextValue();
            }
            if (log.isDebugEnabled()) {
                log.debug("Input String: " + inputString);
                log.debug("dialogue act: " + dialogueAct);
                log.debug("template name: " + ir.getTemplateName());
            }
            Boolean emptyInput = false;
            if (ir.getContentType() == ContentType.EMPTY) {
                emptyInput = true;
                inputString = "";
            } else {
                if (inputString.matches("[ \n]*")) {
                    emptyInput = true;
                }
            }
            VelocityContext context = new VelocityContext();
            boolean containsUrl = false;
            boolean onlyUrl = false;
            Vector<String> media = new Vector<String>();
            String changedInputString = inputString;
            if ((inputString != null) && inputString.matches("http://.*")) {
                containsUrl = true;
                onlyUrl = true;
                String regex = "(http://[^ ]+?)( |$)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(inputString);
                while (matcher.find()) {
                    media.add(addLoginToUrl(matcher.group(1)));
                }
            } else if ((inputString != null) && inputString.matches(".+http://.*")) {
                String regex = "http://[^ ]+?( |$)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(inputString);
                while (matcher.find()) {
                    media.add(addLoginToUrl(matcher.group()));
                }
                changedInputString = inputString.replaceAll(regex, " this link ");
                containsUrl = true;
            }
            if (!ir.getLinkUrlList().isEmpty()) {
                onlyUrl = true;
                for (String i : ir.getLinkUrlList()) {
                    media.add(addLoginToUrl(i));
                }
            }
            context.put("media", media);
            ArrayList<String> inputList = new ArrayList<String>();
            System.out.println("INPUT TYPE: " + ir.getContentType().toString());
            String answerString = "";
            context.put("inputType", ir.getContentType().toString());
            context.put("inputList", inputList);
            context.put("emptyInput", emptyInput);
            context.put("inputTooLong", inputTooLong);
            context.put("onlyUrl", onlyUrl);
            context.put("containsUrl", containsUrl);
            context.put("map", dataStore);
            context.put("random", random);
            context.put("inputString", changedInputString);
            context.put("cat", getLocalName(dialogueAct));
            context.put("questionAnalysis", qa);
            context.put("maleVoice", maleVoice);
            context.put("femaleVoice", femaleVoice);
            context.put("dialogueHistory", getDialogueHistory());
            context.put("timing", timing);
            context.put("firstLogin", firstLogin);
            context.put("maxLength", maxLength);
            context.put("answerString", answerString);
            context.put("os", os1);
            HashMap<String, String> lexicon = new HashMap<String, String>();
            context.put("lexicon", lexicon);
            context.put("templateName", ir.getTemplateName());
            context.put("sid", "session_" + sessionIdNumber);
            context.put("agent", agent);
            context.put("user", agent.getUser());
            context.put("keywordList", ir.getKeywords());
            if (ir.getEmotionType() != null) {
                context.put("emotionType", ir.getEmotionType());
            } else {
                context.put("emotionType", "");
            }
            for (int i = 1; i < 7; i++) {
                context.put("mark" + i, "<ssml:mark name=\"m" + i + "\" xmlns:ssml=\"http://www.w3.org/2001/10/synthesis\" />");
            }
            if (agent.getLastToolUsed().matches("(?i).*T-NALQI.*") && !emptyInput && (inputString.length() > maxLength)) {
                for (String i : inputString.split(",(\n)?")) {
                    inputList.add(i);
                }
                ArrayList<String> list = new ArrayList<String>();
                list.addAll(inputList);
                inputList.clear();
                while (!list.isEmpty()) {
                    Random r = new Random();
                    inputList.add(list.remove(r.nextInt(list.size())));
                }
                if (list.size() > 8) {
                    inputTooLong = true;
                    if (log.isDebugEnabled()) {
                        log.debug("writing output to web page since it is a list with more than 8 elements");
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("detected: last tool used was Nalqi");
                }
            }
            if (inputString.length() > maxLength) {
                inputTooLong = true;
                if (log.isDebugEnabled()) {
                    log.debug("writing output to web page since string is longer than " + maxLength);
                }
            }
            sessionIdNumber++;
            String question = agent.getLastQuestion();
            if (question == null) {
                question = "[question]";
            }
            String webPageContent = "";
            if (log.isDebugEnabled()) {
                if (ir.hasPassData()) {
                    log.debug("Tool has data to pass on without processing!");
                }
            }
            if (dialogueAct.matches(".*ToolOutput.*") || dialogueAct.matches(".*NewRss.*")) {
                StringWriter webdata = new StringWriter();
                if (ir.hasPassData()) {
                    webdata.append(ir.getTextValue());
                    if (!media.isEmpty()) {
                        webdata.append("<br/><a href=\"" + media.get(0) + "\">" + "Link" + "</a>");
                    }
                }
                if (ir.getContentType() == ContentType.CONCEPTS) {
                    webdata.append("<p>" + inputString + "</p>");
                } else {
                    Velocity.mergeTemplate(webTemplateFile, "iso-8859-1", context, webdata);
                }
                webPageContent = webdata.toString();
                int id = 0;
                if (log.isDebugEnabled()) {
                    log.debug("posting to web interface...");
                }
                id = postWebContentToWebInterface(question, webPageContent);
                if (log.isDebugEnabled()) {
                    log.debug("...done");
                }
                String baseUrl = System.getProperty("org.rascalli.webui.url", WEBUI_URL);
                String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "rascalloResponse.form?id=" + id;
                context.put("webPageUrl", url);
                if (inputTooLong && !ir.hasPassData()) {
                    if (log.isDebugEnabled()) {
                        log.debug("long input, adding link to web interface");
                    }
                    media.add(addLoginToUrl(url));
                }
            }
            StringWriter output = new StringWriter();
            CommunicationChannel communicationChannel;
            if (ir.getCommunicationChannel().equals("JABBER")) {
                if (ir.hasPassData()) {
                    if (log.isDebugEnabled()) {
                        log.debug("appending data to jabber output: " + ir.getTextValue());
                    }
                    output.append(ir.getTextValue());
                    for (String i : media) {
                        output.append("\n\n" + i);
                    }
                } else {
                    Velocity.mergeTemplate(jabberTemplateFile, "iso-8859-1", context, output);
                }
                communicationChannel = CommunicationChannel.JABBER;
            } else {
                if (ir.hasPassData()) {
                    String substitute = "&lt;content type=\"internal_window\" id=\"media_1\" action=\"open\"&gt;http://rascalli.dfki.de/live/blank-screen.html&lt;/content&gt;";
                    String passData = StringEscapeUtils.unescapeXml(ir.getPassData().replace(substitute, ""));
                    passData = passData.replace("<content type=\"internal_window\" id=\"media_1\" action=\"open\">", "<content type=\"url\" id=\"media_1\">");
                    passData = passData.replace("&", "&amp;");
                    if (log.isDebugEnabled()) {
                        log.debug("Input has BML data, passing the following to Nebula:\n" + passData);
                    }
                    output.append(passData);
                } else {
                    Velocity.mergeTemplate(templateFile, "iso-8859-1", context, output);
                }
                communicationChannel = CommunicationChannel.ECA;
            }
            if (firstLogin) {
                if (dialogueAct.contains("R-Greeting")) {
                    firstLogin = false;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(output.toString());
            }
            return new MMGOutput(output.toString(), communicationChannel, webPageContent);
        } finally {
            currentThread.setContextClassLoader(oldCCL);
        }
    }

    /**
	 * @return
	 */
    private DialogueHistory getDialogueHistory() {
        return agent.getDialogueHistory();
    }

    private int postWebContentToWebInterface(String question, String answer) {
        if (post) {
            int agentId = agent.getId();
            RascalloResponse rascalloResponse = new RascalloResponse();
            rascalloResponse.setQuestion(question);
            rascalloResponse.setAnswer(answer);
            rascalloResponse.setRascalloId(agentId);
            return rascalliWs.saveRascalloResponseObj(rascalloResponse);
        } else {
            return 1;
        }
    }

    protected String generateLinks(String in) {
        String regex = "(http://[^ ]+?)( |$)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(in);
        while (matcher.find()) {
            String url = matcher.group(1);
            url = url.replaceAll("\\?", "\\\\?");
            in = in.replaceFirst(url, "<a href=\"" + matcher.group(1) + "\">" + matcher.group(1) + "</a>");
        }
        return in;
    }

    private String addLoginToUrl(String in) {
        if (!in.contains("/rascalli") || in.contains("napster")) {
            return in;
        } else {
            String baseUrl = System.getProperty("org.rascalli.webui.url", WEBUI_URL).replace("/rascalli/", "/");
            if (agent.getUser().getPassword() == null) {
                return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "rascalli-ws-1.3/loginBox.form?&backLink=" + in;
            } else {
                return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "rascalli-ws-1.3/login.form?userName=" + agent.getUser().getName() + "&password=" + agent.getUser().getPassword() + "&backLink=" + in;
            }
        }
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    /**
	 * @param rascalliWs
	 *            the rascalliWs to set
	 */
    public void setRascalliWSImpl(RascalliWSImpl rascalliWs) {
        this.rascalliWs = rascalliWs;
    }

    public void setFstServiceUrl(String fstServiceUrl) {
        if (qa != null) qa.setFstServiceUrl(fstServiceUrl);
    }

    public void setDefaultParser(String defaultParser) {
        if (qa != null) qa.setDefaultParser(defaultParser);
    }

    @Override
    public RdfData executeAction(RdfData data) throws Exception {
        MMGOutput output = generateOutput(data);
        agent.sendMultimodalOutput(output.getContent(), output.getChannel());
        return null;
    }

    public void clearWebInterface() {
        rascalliWs.deleteRascalloResponseFromUser(agent.getUser().getId());
    }

    public void noPost() {
        post = false;
    }

    public void setQuestionAnalysis(QuestionAnalysis qa) {
        this.qa = qa;
    }

    public class MMGOutput {

        private final String content;

        private final CommunicationChannel channel;

        private final String webPageContent;

        public MMGOutput(String content, CommunicationChannel channel, String webPageContent) {
            this.content = content;
            this.channel = channel;
            this.webPageContent = webPageContent;
        }

        public CommunicationChannel getChannel() {
            return channel;
        }

        public String getContent() {
            return content;
        }

        public String getWebPageContent() {
            return webPageContent;
        }

        @Override
        public String toString() {
            return "COMMUNICATION CHANNEL: " + channel.toString() + "\nMMG OUTPUT>>\n" + content + "\n<<END\n" + "WEB PAGE CONTENT:>>\n" + webPageContent + "<<END\n";
        }
    }

    private String getLocalName(String s) {
        return s.substring(s.lastIndexOf("#") + 1);
    }

    @Override
    protected void finalize() throws RepositoryException {
        rascalliRepository.shutDown();
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public void setRascalliRepository(Repository repos) {
        rascalliRepository = repos;
    }
}
