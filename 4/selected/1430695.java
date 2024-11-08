package com.m4f.web.controller;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.StringTokenizer;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.Category;
import com.m4f.business.domain.Inbox;
import com.m4f.business.domain.MediationService;
import com.m4f.business.domain.PhraseSearch;
import com.m4f.business.domain.ResultSearchEmail;
import com.m4f.business.domain.extended.Province;
import com.m4f.business.domain.extended.Region;
import com.m4f.business.service.exception.ContextNotActiveException;
import com.m4f.business.service.exception.ServiceNotFoundException;
import com.m4f.utils.PageManager;
import com.m4f.utils.StackTraceUtil;
import com.m4f.utils.search.ifc.ISearchResults;
import com.m4f.utils.search.ifc.ISearchParams.PARAM;
import com.m4f.utils.search.impl.SearchParamsImpl;
import com.m4f.web.bind.form.InboxForm;
import com.m4f.web.bind.form.SearchForm;

@Controller
@RequestMapping("/search")
public class SearchController extends BaseController {

    private static final Logger LOGGER = Logger.getLogger(SearchController.class.getName());

    private static final String gsa_client = "hirubila";

    @RequestMapping(method = RequestMethod.GET)
    public String getSearchForm(@RequestParam(defaultValue = "", required = false) String q, @RequestParam(defaultValue = "hirubila-nonreglated-", required = false) String collection, @RequestParam(defaultValue = "1", required = false) Integer page, Model model, Locale locale) {
        SearchForm searchForm = new SearchForm();
        searchForm.setCollection(collection);
        try {
            if (q != null && !("").equals(q)) {
                StringTokenizer terms = new StringTokenizer(q.trim());
                SearchParamsImpl params = new SearchParamsImpl();
                params.addParam(PARAM.CLIENT, gsa_client);
                params.addParam(PARAM.SEARCH_URI, this.serviceLocator.getAppConfigurationService().getGlobalConfiguration().getSearchUri());
                params.addParam(PARAM.BASE_COLLECTION_NAME, collection);
                params.addParam(PARAM.LANG, locale.getLanguage());
                PageManager<ISearchResults> paginator = new PageManager<ISearchResults>();
                paginator.setStart((page - 1) * paginator.getOffset());
                params.addParam(PARAM.START, String.valueOf(paginator.getStart()));
                this.serviceLocator.getSearchEngine().search(terms, params);
                ISearchResults results = this.serviceLocator.getSearchEngine().getSearchResults();
                paginator.setCollection((Collection) results);
                try {
                    paginator.setSize(Long.parseLong(results.getTotalResults()));
                    model.addAttribute("paginator", paginator);
                } catch (Exception e) {
                    paginator.setSize(0);
                }
            }
        } catch (Exception e) {
            LOGGER.severe(StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        if (q != null && !("").equals(q)) {
            searchForm.setQuery(q);
            model.addAttribute("search", searchForm);
            return "search.results";
        } else {
            model.addAttribute("search", searchForm);
            return "search.form";
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    public String search(@ModelAttribute("form") SearchForm searchForm, @RequestParam(defaultValue = "", required = false) String q, @RequestParam(defaultValue = "1", required = false) Integer page, Model model, Locale locale) {
        try {
            if (searchForm.getQuery() == null || "".equals(searchForm.getQuery())) {
                return "redirect:/" + locale.getLanguage() + "/search";
            }
            StringTokenizer terms = new StringTokenizer(searchForm.getQuery().trim());
            SearchParamsImpl params = new SearchParamsImpl();
            params.addParam(PARAM.CLIENT, gsa_client);
            params.addParam(PARAM.SEARCH_URI, this.serviceLocator.getAppConfigurationService().getGlobalConfiguration().getSearchUri());
            params.addParam(PARAM.BASE_COLLECTION_NAME, searchForm.getCollection());
            params.addParam(PARAM.LANG, locale.getLanguage());
            PageManager<ISearchResults> paginator = new PageManager<ISearchResults>();
            paginator.setStart((page - 1) * paginator.getOffset());
            params.addParam(PARAM.START, String.valueOf(paginator.getStart()));
            this.serviceLocator.getSearchEngine().search(terms, params);
            ISearchResults results = this.serviceLocator.getSearchEngine().getSearchResults();
            paginator.setCollection((Collection) results);
            try {
                paginator.setSize(Long.parseLong(results.getTotalResults()));
            } catch (Exception e) {
                paginator.setSize(0);
            }
            model.addAttribute("paginator", paginator);
        } catch (Exception e) {
            LOGGER.severe(StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        if (q != null && !("").equals(q)) searchForm.setQuery(q);
        model.addAttribute("search", searchForm);
        return "search.results";
    }

    @RequestMapping(value = "/results", method = RequestMethod.GET)
    public String results(@ModelAttribute("form") SearchForm searchForm, @RequestParam(defaultValue = "", required = false) String q, @RequestParam(defaultValue = "1", required = false) Integer page, Model model, Locale locale) {
        this.searchAdvanced(searchForm, q, page, model, locale);
        return "search.results";
    }

    @RequestMapping(value = "/dispatch", method = RequestMethod.GET)
    public String dispatchResult(@RequestParam(required = true) String resultUrl) {
        return "redirect:" + resultUrl;
    }

    @RequestMapping(value = "/results/send", method = RequestMethod.GET)
    public String getResultSearchEmail(@RequestParam(required = true, defaultValue = "") String link, Model model) {
        try {
            ResultSearchEmail email = this.serviceLocator.getInboxService().createResultSearchEmail();
            email.setResultLink(link);
            model.addAttribute("email", email);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        model.addAttribute("sent", false);
        return "search.results.send";
    }

    @RequestMapping(value = "/results/send", method = RequestMethod.POST)
    public String sendResultSearchEmail(@ModelAttribute("email") @Valid ResultSearchEmail form, BindingResult result, HttpServletRequest request, HttpSession session, Locale locale, Model model) {
        if (result.hasErrors() || session.getAttribute("qaptcha") == null) {
            model.addAttribute("sent", false);
            return "search.results.send";
        }
        session.setAttribute("qaptcha", null);
        try {
            String host = request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
            form.setRemoteHost(host);
            this.serviceLocator.getInboxService().save(form, locale);
            if (form.getId() != null) {
                Map<String, String> params = new HashMap<String, String>();
                params.put("resultSearchEmailId", form.getId().toString());
                this.serviceLocator.getWorkerFactory().createWorker().addWork(this.serviceLocator.getAppConfigurationService().getGlobalConfiguration().MAIL_QUEUE, "/task/sendSearchResult", params);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        model.addAttribute("sent", true);
        return "search.results.send";
    }

    @RequestMapping(value = "/results/send/disableqaptcha", method = RequestMethod.POST)
    @ResponseBody
    public String sendResultSearchEmail(HttpSession session, Locale locale) {
        session.setAttribute("qaptcha", "ok");
        return "ok";
    }

    @RequestMapping(value = "/results/error", method = RequestMethod.GET)
    public String getSendErrorPage(Model model) {
        InboxForm inbox = new InboxForm();
        model.addAttribute("inbox", inbox);
        model.addAttribute("sent", false);
        return "search.results.error";
    }

    @RequestMapping(value = "/results/error", method = RequestMethod.POST)
    public String sendErrorPage(@ModelAttribute("inbox") @Valid InboxForm form, BindingResult result, HttpServletRequest request, Locale locale, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("sent", false);
            return "search.results.error";
        }
        try {
            String host = request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
            Inbox inbox = this.serviceLocator.getInboxService().createInbox();
            inbox.setType(Inbox.TYPE.PROBLEM);
            inbox.setRemoteHost(host);
            inbox.setUser(Inbox.USER.EXTERNAL);
            inbox.setContent(form.getContent());
            inbox.setFrom(form.getFrom());
            inbox.setName(form.getName());
            inbox.setCreated(new Date());
            this.serviceLocator.getInboxService().save(inbox, locale);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        model.addAttribute("sent", true);
        return "search.results.error";
    }

    @RequestMapping(value = "/advanced", method = RequestMethod.GET)
    public String getAdvancedSearchForm(Model model, Locale locale) {
        try {
            List<Province> provinces = this.serviceLocator.getTerritorialService().getAllProvinces(locale);
            HashMap<Long, List<Region>> regions = new HashMap<Long, List<Region>>();
            for (Province province : provinces) {
                regions.put(province.getId(), this.serviceLocator.getTerritorialService().getRegionsByProvince(province.getId(), locale));
            }
            HashMap<String, Category> mapa = new HashMap<String, Category>();
            List<Category> tags = this.serviceLocator.getCourseService().getCoursesTags(locale);
            model.addAttribute("provinces", provinces);
            model.addAttribute("regions", regions);
            model.addAttribute("tags", tags);
            model.addAttribute("search", new SearchForm());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        return "search.advanced";
    }

    @RequestMapping(value = "/advanced", method = RequestMethod.POST)
    private String searchAdvanced(@ModelAttribute("form") SearchForm searchForm, @RequestParam(defaultValue = "", required = false) String q, @RequestParam(defaultValue = "1", required = false) Integer page, Model model, Locale locale) {
        try {
            StringTokenizer terms = new StringTokenizer("");
            if (searchForm.getQuery() != null && !("").equals(searchForm.getQuery())) {
                terms = new StringTokenizer(searchForm.getQuery().trim());
            }
            SearchParamsImpl params = new SearchParamsImpl();
            params.addParam(PARAM.CLIENT, gsa_client);
            params.addParam(PARAM.SEARCH_URI, this.serviceLocator.getAppConfigurationService().getGlobalConfiguration().getSearchUri());
            params.addParam(PARAM.BASE_COLLECTION_NAME, this.serviceLocator.getAppConfigurationService().getGlobalConfiguration().getSearchBaseCollectionName());
            params.addParam(PARAM.LANG, locale.getLanguage());
            if (searchForm.getInMeta() != null && !"".equals(searchForm.getInMeta())) {
                String urlEndodedField = URLEncoder.encode(searchForm.getInMeta(), "UTF-8");
                params.addParam(PARAM.INMETA, urlEndodedField);
            }
            PageManager<ISearchResults> paginator = new PageManager<ISearchResults>();
            paginator.setStart((page - 1) * paginator.getOffset());
            params.addParam(PARAM.START, String.valueOf(paginator.getStart()));
            this.serviceLocator.getSearchEngine().search(terms, params);
            ISearchResults results = this.serviceLocator.getSearchEngine().getSearchResults();
            paginator.setCollection((Collection) results);
            try {
                paginator.setSize(Long.parseLong(results.getTotalResults()));
            } catch (Exception e) {
                paginator.setSize(0);
            }
            model.addAttribute("searc", searchForm);
            model.addAttribute("paginator", paginator);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        return "search.results";
    }

    @RequestMapping(value = "/mediators", method = RequestMethod.GET)
    public String getMediatorsNetIndex(Model model, Locale locale) {
        try {
            HashMap<Long, Collection<MediationService>> mediatorsNet = new HashMap<Long, Collection<MediationService>>();
            List<Province> provinces = this.serviceLocator.getTerritorialService().getAllProvinces(locale);
            for (Province province : provinces) {
                Collection<MediationService> mediation = this.serviceLocator.getMediatorService().getMediationServicesByProvince(province.getId(), locale);
                mediatorsNet.put(province.getId(), mediation);
            }
            model.addAttribute("provinces", provinces);
            model.addAttribute("mediators", mediatorsNet);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        return "search.mediator.net.index";
    }

    @RequestMapping(value = "/mediators/{mediationId}", method = RequestMethod.GET)
    public String getMediatorDetail(@PathVariable Long mediationId, Model model, Locale locale) {
        try {
            HashMap<Long, Collection<MediationService>> mediatorsNet = new HashMap<Long, Collection<MediationService>>();
            List<Province> provinces = this.serviceLocator.getTerritorialService().getAllProvinces(locale);
            for (Province province : provinces) {
                Collection<MediationService> mediation = this.serviceLocator.getMediatorService().getMediationServicesByProvince(province.getId(), locale);
                mediatorsNet.put(province.getId(), mediation);
            }
            model.addAttribute("provinces", provinces);
            model.addAttribute("mediators", mediatorsNet);
            model.addAttribute("mediatonService", this.serviceLocator.getMediatorService().getMediationService(mediationId, locale));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
            return "common.error";
        }
        return "search.mediator.net.detail";
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String dispatchResult(Model model) {
        try {
            model.addAttribute("phrases", this.serviceLocator.getPhraseSearchService().getAllPhrases());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
        }
        return "search.list";
    }

    @RequestMapping(value = "/list/json", method = RequestMethod.GET)
    @ResponseBody
    public List<PhraseSearch> listJson(@RequestParam(required = false, defaultValue = "") String query, Model model, Locale locale) {
        try {
            return this.serviceLocator.getPhraseSearchService().findPhrasesByPhrase(query, locale);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, StackTraceUtil.getStackTrace(e));
            return new ArrayList<PhraseSearch>();
        }
    }

    public String generateChannel() {
        Random generator = new Random(Calendar.getInstance().getTimeInMillis());
        String userId = "clientId-" + generator.nextInt();
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        String token = channelService.createChannel(userId);
        this.connectedClients.add(userId);
        return token;
    }

    public long totalCourses(Locale locale) {
        long total = 0;
        try {
            total = this.serviceLocator.getCatalogService().countCourseCatalog(locale);
        } catch (ServiceNotFoundException e) {
            StackTraceUtil.getStackTrace(e);
        } catch (ContextNotActiveException e) {
            StackTraceUtil.getStackTrace(e);
        } catch (Exception e) {
            StackTraceUtil.getStackTrace(e);
        }
        return total;
    }
}
