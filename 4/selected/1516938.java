package org.jcompany.control.jsf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jcompany.commons.PlcBaseContextVO;
import org.jcompany.commons.PlcException;
import org.jcompany.config.PlcConfigControlHelper;
import org.jcompany.config.PlcConfigHelper;
import org.jcompany.config.control.geral.PlcConfigGroupControlApplication;
import org.jcompany.config.domain.PlcConfigGroupAggregation;
import org.jcompany.control.PlcConstants;
import org.jcompany.control.PlcControlLocator;
import org.jcompany.control.jsf.helper.PlcContextHelper;
import org.jcompany.control.jsf.helper.PlcContextMountHelper;
import org.jcompany.control.service.PlcClassLookupService;
import org.jcompany.control.struts.service.PlcI18nService;

/**
 * Classe de servi�o respons�vel pelas listas de domains utilizados em Combos na tela.
 * � um componente Seam em escopo de Evento (request), para que a cada requisi��o do usu�rio
 * busque a lista mais atualizada poss�vel.
 * @author Bruno Grossi
 * @author Mois�s Paula
 * @since jCompany 5.0
 */
@Name(PlcConstants.PlcJsfConstants.PLC_DOMAINS)
@Scope(ScopeType.EVENT)
public class PlcDomainsLookupService {

    protected static final Logger log = Logger.getLogger(PlcDomainsLookupService.class);

    /**
     * Instancia da classe auxiliar.
     */
    protected static final PlcContextMountHelper mountContextHelper = PlcContextMountHelper.getInstance();

    /**
	 * Utilit�rio para ler e gravar atributos no request e na sess�o
	 */
    protected static final PlcContextHelper contextHelper = PlcContextHelper.getInstance();

    /**
	 * Armazena as classes que n�o variam na aplica��o (para otimiza��o), ou sejam, que v�m de Enum.
	 */
    private static Map<String, List<SelectItem>> staticsDomainsMap = null;

    /**
	 * Mapa de dom�nios dispon�veis.
	 */
    private Map<String, List> domains = new HashMap<String, List>();

    /**
	 * Transfere os dados do cache da aplica��o para o mapa de domains.
	 * � executado pelo Seam na cria��o do componente.
	 * @throws PlcException 
	 */
    @Create
    public void transferFromCacheToMap() throws PlcException {
        transferClassesLookup();
        transferClassesDiscreetDomains();
    }

    /**
	 * Metodo que faz a transferencia das classes lookup do cache para o mapa local.
	 * @throws PlcException
	 */
    protected void transferClassesLookup() throws PlcException {
        PlcConfigGroupAggregation configDomain = PlcConfigControlHelper.getInstance().getConfigCurrentDomain(PlcContextHelper.getInstance().getRequest());
        Class[] classesLookup = configDomain.classesLookup();
        if (classesLookup != null && classesLookup.length > 0) {
            for (Class lookupClass : classesLookup) {
                List entityList = getServiceClassLookup().retrieveObjectsFromCache(lookupClass);
                addDomain(lookupClass.getSimpleName(), entityList);
            }
        }
    }

    /**
	 * M�todo que faz a transferencia das classes de dominio discreto para o mapa de dominio.
	 * Faz um cache para otimiza��o.
	 */
    protected void transferClassesDiscreetDomains() throws PlcException {
        this.domains.putAll(getStaticDomainsMap());
    }

    /**
	 * Cria o mapa de dominos est�ticos caso ainda n�o tenha sido criado.
	 * A implementa��o default utiliza um cache, para n�o criar v�rias vezes.
	 */
    protected Map<String, List<SelectItem>> getStaticDomainsMap() throws PlcException {
        if (staticsDomainsMap == null) {
            staticsDomainsMap = new HashMap<String, List<SelectItem>>();
            PlcConfigGroupControlApplication configControlApp = PlcConfigHelper.getInstance().get(PlcConfigGroupControlApplication.class);
            if (configControlApp != null) {
                Class<? extends Enum>[] discreetDomain = configControlApp.classesDiscreetDomain();
                if (discreetDomain != null && discreetDomain.length > 0) {
                    for (int i = 0; i < discreetDomain.length; i++) {
                        String domainName = discreetDomain[i].getSimpleName();
                        if (staticsDomainsMap.containsKey(domainName)) {
                            staticsDomainsMap.remove(domainName);
                        }
                        PlcI18nService serviceI18n = getServiceI18n();
                        HttpServletRequest request = PlcContextHelper.getInstance().getRequest();
                        List<SelectItem> uiList = new ArrayList<SelectItem>();
                        Enum[] obj = discreetDomain[i].getEnumConstants();
                        if (obj != null) {
                            for (Enum e : obj) {
                                SelectItem item = new SelectItem();
                                String name = PlcContextHelper.getInstance().adjustNameFirstTiny(e.getClass().getSimpleName());
                                item.setLabel(serviceI18n.mountLocalizedMessageAnyBundle(request, name + '.' + e.name(), null));
                                item.setValue(e);
                                uiList.add(item);
                            }
                        }
                        staticsDomainsMap.put(domainName, uiList);
                    }
                }
            }
        }
        return staticsDomainsMap;
    }

    /**
	 * Adiciona um novo dom�nio ao mapa.
	 * @param domainName
	 * @param entityList
	 */
    public void addDomain(String domainName, List entityList) {
        if (domains.containsKey(domainName)) {
            domains.remove(domainName);
        }
        List uiList = new ArrayList();
        if (entityList != null) {
            for (Object entity : entityList) {
                if (PropertyUtils.isReadable(entity, "indExcPlc")) {
                    try {
                        Object indExcPlcValue = PropertyUtils.getProperty(entity, "indExcPlc");
                        if (indExcPlcValue != null && ("S".equalsIgnoreCase(indExcPlcValue.toString()) || "true".equalsIgnoreCase(indExcPlcValue.toString()))) {
                            continue;
                        }
                    } catch (Exception e) {
                        log.info("Exception treated:", e);
                    }
                }
                uiList.add(entity);
            }
        }
        domains.put(domainName, uiList);
    }

    /**
	 * Mapa de dom�nios.
	 * @return
	 */
    public Map<String, List> getDomains() {
        return domains;
    }

    /**
	 * Adiciona uma lista de ENTITY's no scopo de conversa��o.
	 * Estas listas s�o normalmente utilizadas em combos que sofrem altera��o da sua lista. Ex: combo aninhados
	 * 
	 * @param nomeDominio, nome do Dominio que ser� adicionado.  [ nomeEntidade_propriedade ] 
	 * @param vos, lista de ENTITY's para o dominio
	 */
    public void addDomainConversation(String domainName, List entityList) throws PlcException {
        try {
            Contexts.getConversationContext().set(domainName, entityList);
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "addConversationDomain", e }, e, log);
        }
    }

    /**
	 * Recupera uma lista de ENTITY's no scopo de conversa��o, conforme dominio.
	 * Estas listas s�o normalmente utilizadas em combos que tem altera��o da sua lista. Ex: combos aninhados
	 * 
	 * @param nomeDominio, nome do Dominio que ser� adicionado. 
	 */
    public List getDomainConversation(String domainName) throws PlcException {
        try {
            if (Contexts.isConversationContextActive()) return (List<SelectItem>) Contexts.getConversationContext().get(domainName); else return new ArrayList();
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "getDomainConversation", e }, e, log);
        }
    }

    /**
	 * Monta um nome padr�o para uma lista de ENTITY's que ser� adicionada no scopo de conversa��o.
	 */
    public String mountDomainForConversation(Class entityClass, String propertyName) throws PlcException {
        return entityClass.getSimpleName() + "Lookup" + propertyName;
    }

    /**
	 * Recupera o Dominio para um Combo Aninhado.
	 */
    public List getDomainNestedCombo(Class classPrincipalEntity, String nestedComboProperty) throws PlcException {
        try {
            String domain = mountDomainForConversation(classPrincipalEntity, nestedComboProperty);
            Map mapItemsStatus = (Map) PlcContextHelper.getInstance().getRequestAttribute(PlcConstants.PlcJsfConstants.PLC_ITEMS_STATUS);
            return getDomainConversation(domain);
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "getDomainNestedCombo", e }, e, log);
        }
    }

    /**
	 * Busca a lista referendo a um dom�nio.
	 * @param domainName
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public List getDomain(String domainName) {
        return domains.get(domainName);
    }

    /**
	 * @since jCompany 5.0
	 * Recupera todas as classes lookup configuradas da aplica��o para o cache.
	 * Apenas aponta para o {@link PlcClassLookupService}.
	 * @throws PlcException
	 */
    public void retrieveAllClassesLookupFromPersistenceToCache() throws PlcException {
        getServiceClassLookup().retrieveAllClassesLookupFromPersistenceToCache();
        transferClassesLookup();
    }

    /**
     * jCompany 3.0 DP Composite. Devolve o singleton que encapsula l�gica de  manipula��es de classes de lookup<p>
     * Ao se pegar o servi�o de registro visual a partir deste m�todo e n�o instanci�-lo diretamente, cria-se um desacoplamento
     * que permite que se altere este servi�o por outros espec�ficos, com m�nimo de esfor�o.
     * @return Servi�o de manipula��o de classes de lookup.
     */
    protected static PlcClassLookupService getServiceClassLookup() throws PlcException {
        return (PlcClassLookupService) PlcControlLocator.getInstance().get(PlcClassLookupService.class);
    }

    /**
	 * Retorna Servi�o de I18n.
	 */
    protected static PlcI18nService getServiceI18n() {
        try {
            return (PlcI18nService) PlcControlLocator.getInstance().get(PlcI18nService.class);
        } catch (Exception e) {
            return new PlcI18nService();
        }
    }

    /**
	 * jCompany 3.0 DP Context POJO. Informa��es gerais para envia � camada
	 * modelo. S�o montadas inicialmente no execute e disponibilizadas para
	 * complemento neste m�todo
	 */
    protected PlcBaseContextVO getContext(HttpServletRequest request) {
        return (PlcBaseContextVO) request.getAttribute(PlcConstants.CONTEXT);
    }
}
