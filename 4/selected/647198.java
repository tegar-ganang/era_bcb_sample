package br.com.gonow.gtt.rpc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import br.com.gonow.gtt.client.util.GenericValidator;
import br.com.gonow.gtt.exception.TranslationAlreadyExistException;
import br.com.gonow.gtt.model.Entry;
import br.com.gonow.gtt.model.File;
import br.com.gonow.gtt.model.Project;
import br.com.gonow.gtt.model.Role;
import br.com.gonow.gtt.model.Translation;
import br.com.gonow.gtt.persistence.LanguagePersistence;
import br.com.gonow.gtt.persistence.LocalUserPersistence;
import br.com.gonow.gtt.service.ProjectDao;
import br.com.gonow.gtt.service.TranslationToolService;
import br.com.gonow.gtt.service.impl.ProjectDaoImpl;
import br.com.gonow.gtt.service.impl.TranslationToolServiceImpl;
import br.com.gonow.gtt.util.PersistenceManagerFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

public class UploadServlet extends HttpServlet {

    private static final long serialVersionUID = 8305367618713715640L;

    private static final Logger log = Logger.getLogger(UploadServlet.class.getName());

    private TranslationToolService service = null;

    private ProjectDao projectDao = null;

    @Override
    public void init() throws ServletException {
        log.info("loading UploadServlet");
        service = new TranslationToolServiceImpl();
        projectDao = new ProjectDaoImpl();
        super.init();
        log.info("loaded UploadServlet");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletInputStream sis = request.getInputStream();
        User user = UserServiceFactory.getUserService().getCurrentUser();
        LocalUserPersistence pLocalUser = projectDao.getLocalUserbyUserId(user.getUserId());
        LanguagePersistence pLanguage = null;
        byte[] buf = new byte[8192];
        int len = 0;
        String limiter = null;
        String line = null;
        String disposition = null;
        boolean dataReading = false;
        String name = null;
        String filename = null;
        String description = null;
        String meaning = null;
        String parameters = null;
        File file = new File();
        String sProject = null;
        String locale = null;
        PersistenceManager pm = PersistenceManagerFactory.get().getPersistenceManager();
        try {
            Date start = new Date();
            Date end = null;
            while ((len = sis.readLine(buf, 0, 8096)) >= 0) {
                line = getLine(buf, len);
                if (limiter == null) {
                    limiter = line;
                    continue;
                }
                if (line.startsWith("Content-Disposition: ")) {
                    disposition = line.substring(21);
                    StringTokenizer st = new StringTokenizer(disposition, ";");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken().trim();
                        if (token.indexOf("=") >= 0) {
                            String key = token.substring(0, token.indexOf("="));
                            String value = token.substring(token.indexOf("=") + 2, token.length() - 1);
                            if ("name".equals(key)) {
                                name = value;
                            } else if ("filename".equals(key)) {
                                filename = value;
                            }
                        }
                    }
                    continue;
                }
                if (line.startsWith("Content-Type: ")) {
                    continue;
                }
                if (!dataReading && line.equals("")) {
                    dataReading = true;
                    if ("uploader".equals(name)) {
                        file.setFilename(filename);
                        if (GenericValidator.isBlankOrNull(locale)) {
                            file = service.addFileInProject(file, sProject);
                        }
                    }
                    continue;
                }
                if (line.startsWith(limiter)) {
                    dataReading = false;
                    continue;
                }
                if ("project".equals(name)) {
                    sProject = line;
                    boolean participate = service.doesUserParticipateInProject(user, sProject, new Role[] { Role.ADMINISTRATOR, Role.DEVELOPER });
                    if (!participate) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    }
                    continue;
                }
                if ("locale".equals(name)) {
                    locale = line;
                    pLanguage = projectDao.getLanguageByLocale(locale);
                    continue;
                }
                if ("uploader".equals(name)) {
                    line = line.trim();
                    if ((line.startsWith("#")) || (line.startsWith("//"))) {
                        line = line.substring(2);
                        if (line.startsWith("Description")) {
                            description = line.substring(13);
                        } else if (line.startsWith("Meaning")) {
                            meaning = line.substring(9);
                        } else if (line.startsWith("0=")) {
                            parameters = line;
                        }
                    } else {
                        int pos = line.indexOf("=");
                        if (pos > 0) {
                            String key = line.substring(0, pos);
                            String value = line.substring(pos + 1);
                            Project project = new Project();
                            project.setId(sProject);
                            Entry entry = new Entry();
                            entry.setKey(key);
                            entry.setContent(value);
                            entry.setProject(project);
                            entry.setMeaning(meaning);
                            entry.setParameters(parameters);
                            entry.setDescription(description);
                            if (GenericValidator.isBlankOrNull(locale)) {
                                service.addEntryInFile(entry, file, pm);
                            } else {
                                Translation translation = new Translation();
                                translation.setEntry(entry);
                                translation.setLocale(locale);
                                translation.setTranslation(value);
                                service.addTranslationByKey(translation, pLocalUser, pLanguage, pm);
                            }
                            description = null;
                            meaning = null;
                            parameters = null;
                        }
                    }
                }
                end = new Date();
                log.info("Processing: " + line + " [" + (end.getTime() - start.getTime()) + "ms]");
                start = new Date();
            }
            response.getOutputStream().write("Upload successful.".getBytes());
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        } catch (TranslationAlreadyExistException e) {
            response.getOutputStream().write("Attempt to translate a term already translated.".getBytes());
            log.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            pm.close();
        }
    }

    private String getLine(byte[] buf, int len) throws UnsupportedEncodingException {
        StringBuilder line;
        line = new StringBuilder(new String(buf, 0, len, "UTF-8"));
        while ((line.length() > 0) && ((line.charAt(line.length() - 1) == '\n') || (line.charAt(line.length() - 1) == '\r'))) {
            line.deleteCharAt(line.length() - 1);
        }
        return line.toString();
    }
}
