package br.nic.connector.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import br.nic.connector.database.HibernateFactory;
import br.nic.connector.database.Paginas;
import br.nic.connector.database.Sitios;
import br.nic.connector.general.Constants;
import br.nic.connector.general.SimpleLog;
import br.nic.connector.general.Utils;

/**
 * DAO responsible for getting and writing data related to Paginas.
 * @author Pedro Hadek
 */
public class PagesDAO extends GenericDAO {

    /**
	 * Initializes the DAO accordingly to the GenericDAO's initialize.
	 */
    public PagesDAO(Session session, boolean encrypt) {
        this.encrypt = encrypt;
        this.session = session;
        this.sessRestart = 0;
    }

    /**
	 * Returns the highest amount of accessibility errors of each level for some host. Used in order
	 * to calculate the accessibility level of a site. The objects in the list are to be found on the
	 * following format and order:<p>
	 * 
	 * 		0 - Max Level 1 Errors - Integer<br>
	 * 		1 - Max Level 2 Errors - Integer<br>
	 * 		2 - Max Level 3 Errors - Integer<br>
	 * 		3 - Host for which the data was obtained - String
	 */
    @SuppressWarnings("unchecked")
    public List<Object[]> getAcessibilidadePaginas() {
        return (List<Object[]>) session.createCriteria(Paginas.class).setProjection(Projections.projectionList().add(Projections.max(Paginas.NAME_erros_ases_1)).add(Projections.max(Paginas.NAME_erros_ases_2)).add(Projections.max(Paginas.NAME_erros_ases_3)).add(Projections.groupProperty(Paginas.NAME_sitio))).list();
    }

    /**
	 * Returns a Paginas from the database based on it's name.
	 * @param name
	 * Name identifying the Paginas to be found..
	 * @return
	 * If no page with that name exists, returns null;<br>
	 * If more than one page exists, returns just the first result, but this should be impossible,
	 * since name is an Unique field.
	 */
    public Paginas getPageFromName(String name) {
        Paginas page = (Paginas) session.createCriteria(Paginas.class).add(Restrictions.eq(Paginas.NAME_paginas, name)).uniqueResult();
        return page;
    }

    /**
	 * Returns a Paginas from the database based on it's wireId.
	 * @param wireId
	 * Wire ID identifying the Paginas to be found..
	 * @return
	 * If no page with that wireId exists, returns null;<br>
	 * If more than one page exists, returns just the first result. This may happen in case different
	 * Wire Runs have been done, but should be prevented.
	 */
    public Paginas getPageFromWireId(Long wireId) {
        Paginas page = (Paginas) session.createCriteria(Paginas.class).add(Restrictions.eq(Paginas.NAME_wireId, wireId)).uniqueResult();
        return page;
    }

    /**
	 * Returns the name of a Page from the database based on it's wireId.
	 * @param wireId
	 * Wire ID identifying the Paginas to be found..
	 * @return
	 * If no page with that wireId exists, returns an empty String;<br>
	 * If more than one page exists, returns just the first result. This may happen in case different
	 * Wire Runs have been done, but should be prevented.
	 */
    @SuppressWarnings("unchecked")
    public synchronized String getPageNameFromWireId(Long wireId) {
        try {
            List<String> pageName = session.createCriteria(Paginas.class).add(Restrictions.eq(Paginas.NAME_wireId, wireId)).setProjection(Projections.property(Paginas.NAME_paginas)).list();
            if (pageName.size() > 0) return pageName.get(0);
            return "";
        } catch (NullPointerException e) {
            SimpleLog.getInstance().writeLog(3, "Erro!!! wire id " + wireId + "não foi encontrado");
            SimpleLog.getInstance().writeException(e, 3);
            return "";
        } catch (Exception e) {
            treatException(e, "erro na obtenção da página");
            return "";
        }
    }

    /**
	 * Obtains the amount of pages already tested for a given parameter. Used in case repeating
	 * tests where disabled.
	 * @param criticalTestType
	 * Name of the critical parameter that, if null, indicates that some test has not been done.
	 */
    public long getQuantity(String criticalTestType) {
        if (criticalTestType == null) return 0l; else {
            return (Long) session.createCriteria(Paginas.class).add(Restrictions.isNotNull(criticalTestType)).setProjection(Projections.count(Paginas.NAME_ID)).uniqueResult();
        }
    }

    /**
	 * Returns true if a page with that name already exists, false otherwise.
	 * @param pagePath
	 * Name of the page whose existence should be verified.
	 * @param criticalTestType
	 * Name of the critical parameter that, if null, indicates that some test has not been done.
	 */
    public boolean hasTestedPage(String pagePath, String criticalTestType) {
        if (pagePath.endsWith("index.root")) {
            pagePath = pagePath.substring(0, pagePath.indexOf("index.root"));
        }
        if (encrypt) {
            pagePath = Utils.getHash(pagePath);
        }
        return (session.createCriteria(Paginas.class).add(Restrictions.eq(Paginas.NAME_paginas, pagePath)).add(Restrictions.isNotNull(criticalTestType)).setProjection(Projections.property(Paginas.NAME_ID)).uniqueResult() != null);
    }

    /**
	 * Should indicate if this table contains any wire_ids or not.
	 */
    public boolean hasWireIDs() {
        return !session.createCriteria(Paginas.class).add(Restrictions.isNotNull(Paginas.NAME_wireId)).setMaxResults(1).list().isEmpty();
    }

    /**
	 * Lists all pages whose name ends with "index.r00t", a suffix used whenever WIRE downloads the 
	 * default page of a domain. IE: If WIRE downloads the page from www.example.com, it will name the
	 * page downloaded from "http://www.example.com/" as index.r00t.  
	 */
    @SuppressWarnings("unchecked")
    public List<Paginas> indexRootList() {
        Query l = session.createQuery("from Paginas where " + Paginas.NAME_paginas + " like '%index.r00t' or " + Paginas.NAME_paginas + " like '%[renamed]%'");
        return l.list();
    }

    /**
	 * Indicates if the Paginas database is empty or not. 
	 */
    public boolean isEmpty() {
        return session.createCriteria(Paginas.class).setMaxResults(1).list().isEmpty();
    }

    /**
	 * Map that should relate every Page Wire ID to it's corresponding host's Wire ID.
	 */
    @SuppressWarnings("unchecked")
    public Map<Long, Long> mapPageWireIDtoHost() {
        long time2 = System.currentTimeMillis();
        List<Object[]> list = session.createQuery("SELECT page." + Paginas.NAME_wireId + ", hosts." + Sitios.NAME_id + " FROM " + Sitios.NAME_table + " hosts, " + Paginas.NAME_Table + " page" + " WHERE page." + Paginas.NAME_sitio + " = hosts." + Sitios.NAME_sitio + " AND " + "page." + Paginas.NAME_wireId + " IS NOT NULL" + " AND hosts." + Sitios.NAME_id + " IS NOT NULL").list();
        long time3 = System.currentTimeMillis();
        Map<Long, Long> map = new HashMap<Long, Long>();
        Iterator<Object[]> it = list.iterator();
        while (it.hasNext()) {
            Object[] register = it.next();
            if (register[1] != null && register[0] != null) map.put((Long) register[0], (Long) register[1]);
        }
        System.out.println("Tempo de query: " + (time3 - time2) / 1000.0 + "s");
        return map;
    }

    /**
	 * Generates log messages, and deal with unfortunate situations that may arise, from
	 * whatever exception may occur.  
	 */
    private void treatException(Exception e, String currTestElement) {
        try {
            if (e instanceof ConstraintViolationException) {
                SimpleLog.getInstance().writeLog(3, "Constraint Exception devido a " + ((ConstraintViolationException) e).getConstraintName());
                session.clear();
                session.close();
            }
            if (session == null || !session.isOpen() || !session.isConnected()) {
                if (session != null) session.close();
            }
            HibernateFactory factory = new HibernateFactory();
            factory.rebuildFactory();
            session = factory.getSession();
            SimpleLog.getInstance().writeLog(3, "Exceção ocorrida em PagesDAO;\n" + "Ocorrida durante análise de " + currTestElement);
            SimpleLog.getInstance().writeException(e, 3);
        } catch (Exception ex) {
            SimpleLog.getInstance().writeLog(3, "\nExceção no tratamento de exceção!\n");
            SimpleLog.getInstance().writeException(ex, 3);
        }
    }

    /**
	 * Generic "Paginas" results writer. Should be called whenever a new Addition to the data in the
	 * Paginas table must be made, unless some pretty specific requirements arise. It's usage is
	 * encouraged since it already do Exception verification and a lot of other things that
	 * shouldn't be duplicated.
	 * @param newPage
	 * Page where the new date is. Will replace any of the values on the database with those, unless
	 * they are null.
	 */
    public void writeResults(Paginas newPage) {
        if (newPage.getPagina().length() >= 1000) newPage.setPagina(newPage.getPagina().substring(0, 1000));
        if (newPage != null && newPage.getPagina() != null) {
            String currPagina = newPage.getPagina();
            int tries = 0;
            boolean done = false;
            while (!done && tries < Constants.DEFAULT_MAXWRITETRYS) {
                tries++;
                try {
                    sessRestarter();
                    if (encrypt) {
                        currPagina = Utils.getHash(currPagina);
                        newPage.setPagina(currPagina);
                        if (newPage.getSitio() != null) {
                            newPage.setSitio(Utils.getHash(newPage.getSitio()));
                        }
                    }
                    Paginas oldPage = (Paginas) session.createCriteria(Paginas.class).add(Restrictions.eq(Paginas.NAME_paginas, currPagina)).uniqueResult();
                    if (oldPage == null) {
                        oldPage = new Paginas();
                        oldPage.setPagina(currPagina);
                        oldPage.copyPaginas(newPage);
                        session.save(oldPage);
                    } else {
                        oldPage.copyPaginas(newPage);
                        session.flush();
                    }
                    done = true;
                } catch (Exception e) {
                    treatException(e, currPagina);
                } catch (ThreadDeath err) {
                    try {
                        session.clear();
                        session.close();
                    } catch (Throwable t) {
                        SimpleLog.getInstance().writeLog(3, "Erro para limpar sessão durante" + " ThreadDeath.");
                    }
                    session = null;
                    session = new HibernateFactory().getSession();
                    System.out.println("Passou aqui!");
                    throw err;
                }
            }
        }
    }

    /**
	 * Writes the results of the Home Page Download test, according to the generic writeResults.
	 * @param pageName
	 * Name of the page that was analyzed.
	 * @param pageLength
	 * Length of the page being saved.
	 * @param numErrosASES
	 * How many ASES errors and warnings appeared. 0-2: Errors; 3-5: Warnings
	 * @param numErrosW3C
	 * How many W3C Markup Validation Errors appeared.
	 * @param docType
	 * DocType, if any, identified for the page.
	 */
    public void writeDownloadHomeResult(String pageName, long pageLength, Integer[] numErrosASES, Integer numErrosW3C, String docType) {
        if (pageName != null) {
            Paginas page = new Paginas();
            page.setPagina(pageName);
            page.setSitio(pageName);
            page.setTamanho(pageLength);
            if (numErrosASES != null && numErrosASES.length >= 6) {
                page.setErros_ases_p1(numErrosASES[0]);
                page.setErros_ases_p2(numErrosASES[1]);
                page.setErros_ases_p3(numErrosASES[2]);
                page.setAvisos_ases_p1(numErrosASES[3]);
                page.setAvisos_ases_p2(numErrosASES[4]);
                page.setAvisos_ases_p3(numErrosASES[5]);
            }
            page.setData_ases(new Date(System.currentTimeMillis()));
            page.setErros_w3c(numErrosW3C);
            page.setData_w3c(new Date(System.currentTimeMillis()));
            page.setDoc_type(docType);
            writeResults(page);
        }
    }

    /**
	 * Writes the results of an accessibility validation test, according to writeResults.
	 * @param pagePath
	 * Fully qualified name of the page.
	 * @param pageLength
	 * Size of the page that was download.
	 * @param numErros
	 * Number of errors and warnings found on the accessibility validation test. On the Integer, 0-2
	 * contain the errors, and 3-5 contain the warnings.
	 */
    public void writeAccessibilityResult(String pagePath, long pageLength, Integer[] numErros) {
        String currPagePath = Utils.getPageName(pagePath, Constants.STATICS_PAGE_PATH);
        if (currPagePath != null && numErros != null) {
            Paginas page = new Paginas();
            page.setPagina(currPagePath);
            page.setSitio(Utils.getPageHost(pagePath, Constants.STATICS_PAGE_PATH));
            page.setTamanho(pageLength);
            page.setErros_ases_p1(numErros[0]);
            page.setErros_ases_p2(numErros[1]);
            page.setErros_ases_p3(numErros[2]);
            page.setAvisos_ases_p1(numErros[3]);
            page.setAvisos_ases_p2(numErros[4]);
            page.setAvisos_ases_p3(numErros[5]);
            page.setData_ases(new Date(System.currentTimeMillis()));
            writeResults(page);
        }
    }

    /**
	 * Escreve diversos dados relativos às páginas obtidos a partir da análise do Wire e
	 * exportados para um formato CVS.
	 * Somente escreve alguma coisa se o identificador pageName não for nulo.
	 * @param pageName
	 * Nome da página, usado como identificador.
	 * @param paginaDados
	 * Objeto do tipo Paginas contendo as informações que se deseja analisar.
	 */
    public void writeCSVResult(String pageName, Paginas paginaDados) {
        if (pageName != null && paginaDados != null) {
            Paginas page = new Paginas();
            page.setSitio(paginaDados.getSitio());
            page.setPagina(pageName);
            page.setMime_type(paginaDados.getMime_type());
            page.setHttp_status(paginaDados.getHttp_status());
            page.setIdade(paginaDados.getIdade());
            page.setContent_length(paginaDados.getContent_length());
            page.setDepth(paginaDados.getDepth());
            page.setIs_dynamic(paginaDados.is_dynamic());
            page.setIn_degree(paginaDados.getIn_degree());
            page.setOut_degree(paginaDados.getOut_degree());
            page.setPagerank(paginaDados.getPagerank());
            page.setWlrank(paginaDados.getWlrank());
            page.setHubrank(paginaDados.getHubrank());
            page.setAuthrank(paginaDados.getAuthrank());
            page.setWire_id(paginaDados.getWire_id());
            page.setData_CSV(new Date(System.currentTimeMillis()));
            page.setTamanho(paginaDados.getTamanho());
            page.setDuplicate_of(paginaDados.getDuplicate_of());
            page.setTamanho(paginaDados.getTamanho());
            writeResults(page);
        }
    }

    /**
	 * Escreve dados de idiomas relativos às páginas obtidos a partir da análise do Wire e
	 * exportados para um formato CVS.
	 * Somente escreve alguma coisa se o identificador pageName não for nulo.
	 * @param pageName
	 * Nome da página, usado como identificador.
	 * @param paginaDados
	 * Objeto do tipo Paginas contendo as informações que se deseja analisar.
	 * 
	 * OBS: Does not uses "writeResults" Since it has some pretty specific requirements.
	 */
    public void writeCSVResultLang(Long wireID, Paginas paginaDados) {
        if (wireID != null && paginaDados != null) {
            int trys = 0;
            boolean done = false;
            while (!done && trys < Constants.DEFAULT_MAXWRITETRYS) {
                try {
                    sessRestarter();
                    Paginas page = getPageFromWireId(wireID);
                    if (page != null) {
                        page.setLinguagem(paginaDados.getLinguagem());
                        page.setData_Lang(new Date(System.currentTimeMillis()));
                        session.flush();
                    }
                    done = true;
                } catch (Exception e) {
                    treatException(e, wireID.toString());
                }
                trys++;
            }
        }
    }

    public void writeHttpStatusRedownload(String pagina, int httpStatus) {
        if (pagina != null) {
            Paginas page = new Paginas();
            page.setPagina(pagina);
            page.setHttpStatusRedownload(httpStatus);
            writeResults(page);
        }
    }

    /**
	 * Remove um dado objeto do Banco de Dados.
	 * @param o
	 * Objeto a ser removido.
	 */
    public void delete(Object o) {
        session.delete(o);
    }

    /**
	 * Retorna uma lista com as páginas e seus respectivos hosts que ainda não passaram pelo
	 * teste de HTML, mas que já tem dados no Banco de Dados.
	 * Paginas vem primeiro, depois vem o nome do hosts.
	 */
    public ScrollableResults getUntestedHTMLPageList() {
        return session.createCriteria(Paginas.class).add(Restrictions.isNull(Paginas.NAME_data_w3c)).add(Restrictions.between(Paginas.NAME_http_status, 200, 299)).setProjection(Projections.projectionList().add(Projections.property(Paginas.NAME_paginas)).add(Projections.property(Paginas.NAME_sitio))).scroll();
    }

    public ScrollableResults getUntestedAcessibilityPageList() {
        return session.createCriteria(Paginas.class).add(Restrictions.isNull(Paginas.NAME_data_ases)).add(Restrictions.between(Paginas.NAME_http_status, 200, 299)).setProjection(Projections.projectionList().add(Projections.property(Paginas.NAME_paginas)).add(Projections.property(Paginas.NAME_sitio))).scroll();
    }

    public ScrollableResults getUndowloadedPages() {
        return session.createCriteria(Paginas.class).add(Restrictions.isNull(Paginas.NAME_data_ases)).add(Restrictions.between(Paginas.NAME_http_status, 200, 299)).setProjection(Projections.projectionList().add(Projections.property(Paginas.NAME_paginas)).add(Projections.property(Paginas.NAME_sitio)).add(Projections.property(Paginas.NAME_wireId)).add(Projections.property(Paginas.NAME_aleatorio))).addOrder(Order.asc(Paginas.NAME_aleatorio)).scroll();
    }

    public ScrollableResults getUnanalizedPages() {
        return session.createQuery("select pagina, host, wire_id, aleatorio from Paginas where wire_id is not null and http_status between 200 and 299 and (http_status_redownload between 200 and 299 or http_status_redownload is null) and (erros_ases_p1 = -1 or erros_ases_p2 = -1 or erros_ases_p3 = -1 or avisos_ases_p1 = -1 or avisos_ases_p2 = -1 or avisos_ases_p3 = -1)").scroll();
    }

    public ScrollableResults getUnanalizedPagesW3C() {
        return session.createQuery("select pagina, host, wire_id, aleatorio from Paginas where wire_id is not null and data_w3c is null and data_ases is not null order by aleatorio").scroll();
    }

    @SuppressWarnings("unchecked")
    public List<Paginas> getPagesWithDotSlash() {
        return session.createCriteria(Paginas.class).add(Restrictions.like(Paginas.NAME_paginas, "%/./%")).list();
    }

    @SuppressWarnings("unchecked")
    public List<Paginas> getPagesWithDotDotSlash() {
        return session.createCriteria(Paginas.class).add(Restrictions.like(Paginas.NAME_paginas, "%/../%")).list();
    }

    public int updateDuplicatedContentLangClassification() {
        String sql = "update Paginas as P, (select A.id, p2.linguagem from Paginas as p2," + " (select p1.id, p1.wire_id, duplicate_of from Paginas as p1 where not duplicate_of = 0)" + " as A where p2.wire_id = A.duplicate_of)" + " as B set P.linguagem = B.linguagem where P.id = B.id";
        return session.createSQLQuery(sql).executeUpdate();
    }
}
