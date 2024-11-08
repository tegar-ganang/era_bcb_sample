package savenews.backend.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import savenews.backend.exceptions.AppInfrastructureException;
import savenews.backend.exceptions.FileAlreadyExistsException;
import savenews.backend.exceptions.TemplateLoadingException;
import savenews.backend.to.Article;
import savenews.backend.util.ConfigurationResources;
import savenews.backend.util.DeploymentConstants;

/**
 * Perform XSLT transformation to generate article files
 * @author Eduardo Ferreira
 */
public class ArticleDAO {

    private SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getInstance();

    /** Singleton instance */
    private static ArticleDAO instance = new ArticleDAO();

    /** Logger */
    private Logger logger = Logger.getAnonymousLogger();

    private TransformerFactory factory;

    private Templates defaultTemplate;

    private Map<String, Templates> templates;

    /**
	 * Private constructor to avoid instantiation
	 */
    private ArticleDAO() {
        templates = new HashMap<String, Templates>();
        sdf.applyLocalizedPattern("dd/MM/yyyy");
        factory = TransformerFactory.newInstance();
        try {
            defaultTemplate = factory.newTemplates(new StreamSource(this.getClass().getResourceAsStream(DeploymentConstants.TEMPLATES_RES_DIR + "/default.xsl")));
        } catch (TransformerConfigurationException e) {
            throw new AppInfrastructureException(e);
        }
    }

    /**
	 * @return Singleton instance
	 */
    public static ArticleDAO getInstance() {
        return instance;
    }

    public void export(Article article, boolean overwriteFile) throws FileAlreadyExistsException {
        File outputFile = null;
        FileOutputStream outputStream = null;
        try {
            Transformer transformer = getTransformerForArticle(article);
            outputFile = getResultDestination(article, overwriteFile);
            outputStream = new FileOutputStream(outputFile);
            Document articleDocument = getDocumentFromArticle(article);
            transformer.transform(new DOMSource(articleDocument), new StreamResult(outputStream));
        } catch (TransformerException e) {
            throw new AppInfrastructureException(e);
        } catch (FileNotFoundException e) {
            throw new AppInfrastructureException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "DAO error closing file:", e);
                }
            }
        }
    }

    private Document getDocumentFromArticle(Article article) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("article");
            Element title = document.createElement("title");
            root.appendChild(title);
            title.setTextContent(article.getTitle());
            if (article.getDate() != null) {
                Element date = document.createElement("date");
                root.appendChild(date);
                date.setTextContent(sdf.format(article.getDate()));
            }
            String originDescription = null;
            if (article.getOrigin() != null) {
                originDescription = article.getOrigin().getRenderName();
            } else if (article.getOriginDescription() != null && !article.getOriginDescription().trim().equals("")) {
                originDescription = article.getOriginDescription();
            }
            if (originDescription != null) {
                Element origin = document.createElement("origin");
                root.appendChild(origin);
                origin.setTextContent(originDescription);
            }
            for (String paragraph : article.getParagraphs()) {
                Element paragraphElement = document.createElement("paragraph");
                root.appendChild(paragraphElement);
                paragraphElement.setTextContent(paragraph);
            }
            document.appendChild(root);
            return document;
        } catch (ParserConfigurationException e) {
            throw new AppInfrastructureException(e);
        }
    }

    private Transformer getTransformerForArticle(Article article) {
        try {
            Templates template = defaultTemplate;
            if (article.getOrigin() != null) {
                if (article.getOrigin().getStyleName() != null && !article.getOrigin().getStyleName().trim().equals("")) {
                    template = loadTemplate(article.getOrigin().getStyleName());
                }
            }
            Transformer transformer = template.newTransformer();
            ;
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            return transformer;
        } catch (TransformerConfigurationException e) {
            throw new AppInfrastructureException(e);
        }
    }

    private synchronized Templates loadTemplate(String styleName) {
        Templates template = templates.get(styleName);
        if (template == null) {
            try {
                FileInputStream templateIS = new FileInputStream(DeploymentConstants.DATA_DIR + "/" + DeploymentConstants.TEMPLATES_DATA_DIR + "/" + styleName);
                template = factory.newTemplates(new StreamSource(templateIS));
            } catch (TransformerConfigurationException e) {
                throw new TemplateLoadingException(e);
            } catch (FileNotFoundException e) {
                throw new TemplateLoadingException(e);
            }
        }
        return template;
    }

    private File getResultDestination(Article article, boolean overwriteFile) throws FileAlreadyExistsException {
        File baseDirectory = new File(ConfigurationResources.getInstance().getOutputDirectory());
        if (!baseDirectory.isDirectory()) {
            throw new AppInfrastructureException("Configuration error: output directory not found");
        }
        File outputFile = new File(baseDirectory, article.getDestinationFileName());
        if (outputFile.isFile()) {
            if (!overwriteFile) {
                throw new FileAlreadyExistsException(outputFile.getName());
            }
        }
        return outputFile;
    }
}
