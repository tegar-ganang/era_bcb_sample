package org.jcompany.commons.helper;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import javax.persistence.Embeddable;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;
import org.jcompany.commons.PlcBaseContextVO;
import org.jcompany.commons.PlcBaseEntity;
import org.jcompany.commons.PlcConstantsCommons;
import org.jcompany.commons.PlcException;
import org.jcompany.commons.PlcConstantsCommons.ENTITY;
import org.jcompany.commons.annotation.PlcPrimaryKey;
import org.jcompany.commons.comparator.PlcCompareId;
import org.jcompany.commons.comparator.PlcCompareName;
import org.jcompany.config.PlcConfigHelper;
import org.jcompany.config.commons.PlcConfigSuffixClass;

/**
 * jCompany 2.5.3. Singleton. Classe utilit�ria para datas
 */
public class PlcEntityHelper {

    private static final String ERROR_ORIGINAL = ". Original Error: ";

    private static final WeakHashMap<Class, Set<String>> PROPS_ALL_PLC_MAP = new WeakHashMap<Class, Set<String>>();

    private static PlcEntityHelper INSTANCE = new PlcEntityHelper();

    private PlcEntityHelper() {
    }

    /**
     * @since jCompany 2.5.3
     *      
     */
    public static PlcEntityHelper getInstance() {
        return INSTANCE;
    }

    protected static final Logger log = Logger.getLogger(PlcEntityHelper.class);

    /**
     * @since jCompany 3.0
     * Recupera todos os nomes das propriedades do ENTITY.<br>
     * As propriedades s�o retornados em forma de string separados por ','.<br>
     * <b>Obs.:</b>N�o ser�o adicionados as propriedades que estiverem emn contextParam.getPropNaoGerar().<br>   
     * <br>
     * @param entity ENTITY que ter� seus atributos retornados.
     * @param contextParam
     * @return string com os atributos do vo separados por ','. null caso n�o seja poss�vel recuperar as propriedades.
     */
    public String getPropertiesToString(Object entity, PlcBaseContextVO contextParam) throws PlcException {
        log.debug("############### Entered in getPropertiesToString");
        try {
            StringBuffer attributs = new StringBuffer();
            String attributsNotRetrieve = "";
            BeanInfo bi = null;
            bi = Introspector.getBeanInfo(entity.getClass());
            PropertyDescriptor[] pd = bi.getPropertyDescriptors();
            if (pd != null) {
                for (int i = 0; i < pd.length; i++) {
                    if (pd[i].getReadMethod() != null && attributsNotRetrieve.indexOf(pd[i].getName()) == -1) attributs.append(pd[i].getName()).append(",");
                }
            }
            if (attributs.length() > 0) return attributs.toString().substring(0, attributs.length() - 1);
            return null;
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "getPropertiesToString", e }, e, log);
        }
    }

    /**
     * @since jCompany 3.0
     * Devolve a posi��o em uma cole��o em que se encontra um ENTITY descendente de
     * Object com um Id determinado
     *
     * @param l
     *            Cole��o de VOs
     * @param id
     *            Cole��o de identificadores
     * @return posi��o em que se encontra o ENTITY ou -1 se n�o encontrou
     */
    public int positionEntity(List l, Long id) throws PlcException {
        Iterator i = l.iterator();
        int cont = 0;
        while (i.hasNext()) {
            Object entity = i.next();
            try {
                if (((Long) PropertyUtils.getProperty(entity, "id")).intValue() == id.intValue()) return cont;
            } catch (Exception e) {
                throw new PlcException("Nao achou 'id' na Entidade " + entity.getClass().getName());
            }
            cont++;
        }
        return -1;
    }

    /**
     * @since jCompany 1.5
     * Recebe um Set contendo VOs do type PlcBaseEntity e clona tudo, limpando o OID apos a clonagem
     *
     * @param s Set
     * @param parent (Opcional) Se informado, coloca em "paiPlc" apos clonar
     * @return novo Set com VOs clonados
     */
    public Set cloneCollection(Set s, Object parent) throws PlcException {
        log.debug("######## Entered in getPropertiesToString");
        if (s == null) return null;
        Set newSet = new HashSet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            Object entity = i.next();
            Object newEntity;
            try {
                newEntity = BeanUtils.cloneBean(entity);
                if (parent != null) PropertyUtils.setProperty(newEntity, ENTITY.PARENT_PLC, parent);
                PropertyUtils.setProperty(newEntity, "id", null);
                newSet.add(newEntity);
            } catch (Exception e) {
                throw new PlcException("jcompany.error.generic", new Object[] { "cloneCollection", e }, e, log);
            }
        }
        return newSet;
    }

    /**
     * @since jCompany 2.5.3
     * Compara dois objetos com "equals" considerando que podem
     * ser, cada um, nulos
     *
     * @return true se forem iguais ou ambos nulos ou false se forem diferentes
     *         ou somente um deles nulo.
     */
    public boolean equalsNull(Object obj1, Object obj2) {
        if ((obj1 == null && obj2 != null) || (obj2 == null && obj1 != null)) return false;
        if (obj1 == null && obj2 == null) return true;
        if (obj1.equals(obj2)) return true; else return false;
    }

    /**
     * @since jCompany 3.0
     * jCompany. Compara duas cole��es de PlcBaseEntity e retorna true ou
     * false conforme sejam iguais (tenha o mesmo size e contenha os mesmos
     * objetos, com o mesmo OID). A compara��o � feita comparando-se os OIDs (id
     * Long) de todos os objetos que comp�em as cole��es. A ordem n�o �
     * importante. Objetos com id nulo s�o desprezados.
     *
     * @param list1
     * @param list2
     * @return true ou false conforme seja igual ou diferente
     */
    public boolean collectionEquals(List list1, List list2) throws PlcException {
        log.debug("######## Entered in collectionEquals");
        if (list1.size() != list2.size()) return false;
        Iterator i = list1.iterator();
        while (i.hasNext()) {
            Object entity1 = i.next();
            Iterator j = list2.iterator();
            boolean found = false;
            while (j.hasNext() && !found) {
                Object entity2 = j.next();
                try {
                    if ((PropertyUtils.getProperty(entity2, "id") != null && PropertyUtils.getProperty(entity1, "id") != null) && (((Long) PropertyUtils.getProperty(entity2, "id")).longValue() == ((Long) PropertyUtils.getProperty(entity1, "id")).longValue())) found = true;
                } catch (Exception e) {
                    throw new PlcException("jcompany.error.generic", new Object[] { "collectionEquals", e }, e, log);
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /**
     * @since jCompany 3.0
     * Verifica se uma cole��o de PlcBaseEntity tem um ENTITY especifico,
     * baseado na sua chave OID
     *
     * @param l
     *            cole��o
     * @param entity
     *            vo
     * @return true ou false
     */
    public boolean containEntity(List l, Object entity) throws PlcException {
        if (l == null || entity == null) return false;
        Iterator i = l.iterator();
        while (i.hasNext()) {
            Object ent = i.next();
            try {
                if (((Long) PropertyUtils.getProperty(ent, "id")).longValue() == ((Long) PropertyUtils.getProperty(entity, "id")).longValue()) return true;
            } catch (Exception e) {
                throw new PlcException("jcompany.erro.generico", new Object[] { "containEntity", e }, e, log);
            }
        }
        return false;
    }

    /**
     * 
     * @since jCompany 2.7.1
     * @param l Cole��o de VOs
     * @param oid
     * @return true se contiver ou false se n�o contiver. Cole��o nula ou vo nulo devolvem false
     */
    public boolean containEntityById(List l, Long oid) throws PlcException {
        if (l == null || oid == null) return false;
        Iterator i = l.iterator();
        while (i.hasNext()) {
            Object ent = i.next();
            try {
                if (((Long) PropertyUtils.getProperty(ent, "id")).longValue() == oid.longValue()) return true;
            } catch (Exception e) {
                throw new PlcException("jcompany.error.generic", new Object[] { "containEntityById", e }, e, log);
            }
        }
        return false;
    }

    /**
     * @since jCompany 3.0
     * Ordena Cole��o de VOs descendentes de PlcBaseEntity por Nome
     *
     * @param l
     *            Colea��o a ordenar
     * @return Cole��o ordenada
     */
    public List orderEntityByName(List l) {
        PlcCompareName comp = new PlcCompareName();
        Collections.sort(l, comp);
        return l;
    }

    /**
     * @since jCompany 3.0
     * Ordena Cole��o de VOs descendentes de PlcBaseEntity por Id
     *
     * @param l
     *            Colea��o a ordenar
     * @return Cole��o ordenada
     */
    public List orderEntityById(List l) {
        PlcCompareId comp = new PlcCompareId();
        Collections.sort(l, comp);
        return l;
    }

    /**
     * @since jCompany 3.0
     * Recebe um Vo e uma contendo uma lista de VOs anteriormente existentes e,
     * baseados em seu OID, verifica se � opera��o de I-nclusao, A-lteracao ou
     * E-xclusa.
     *
     * @param entity
     *            Vo a ser verificado
     * @param listPrevious
     *            Lista de VOs anteriores ou null se for inclus�o
     * @return A opera��o I,A ou E e o vo Anterior correspondente se encontrado
     *         na cole��o.
     */
    public Object[] verifyOperationById(Object entity) throws PlcException {
        log.debug("######## Entered in verifyOperationById");
        String oper = "";
        if (entity == null) throw new PlcException("#Internal Error. Entity is null when trying to save");
        try {
            if (("S".equals(PropertyUtils.getProperty(entity, "indExcPlc").toString()) || "true".equals(PropertyUtils.getProperty(entity, "indExcPlc").toString()))) {
                log.debug("Decided to exclude");
                oper = "E";
            } else {
                if (PropertyUtils.isReadable(entity, "id")) {
                    if (PropertyUtils.getProperty(entity, "id") != null) oper = "A"; else oper = "I";
                } else {
                }
            }
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "verifyOperationById", e }, e, log);
        }
        return new Object[] { oper, entity };
    }

    /**
     * @since jCompany 2.7.
     * Procura por ENTITY igual dentro de cole��o
     * @param l Cole��o a inspecionar
     * @param entity ENTITY para compara��o
     * @return vo encontrado na cole��o igual ao vo do argumento (todas as
     *         propriedades via apiTotEquals) ou null, se n�o encontrou
     */
    public Object returnEntityEntirelyEqual(List l, Object entity) {
        if (l == null || entity == null) return null;
        Iterator i = l.iterator();
        while (i.hasNext()) {
            Object ent = i.next();
            if (equalsPlc(ent, entity)) return ent;
        }
        return null;
    }

    /**
     * @since jCompany 2.7.1.
     * Recebe o nome de uma classe, com ou sem pacote, e devolve o nome da
     * propriedade agregada padr�o, que segue o padr�o do nome da classe com inicial min�scula.
     *
     * Ex: recebendo com.empresa.app.vo.MinhaClasseVO, devolve minhaClasseVO
     *
     * @param primaryKeyClass Nome da classe com pacote
     * @return nome da propriedade ou null se nome for null ou string vazio
     */
    public String returnPropertyNameToClass(String primaryKeyClass) {
        log.debug("######## Entered in primaryKeyClass");
        if (primaryKeyClass == null || "".equals(primaryKeyClass.trim())) return null;
        String nameProp = null;
        if (primaryKeyClass.indexOf(".") > -1) {
            int pos = primaryKeyClass.lastIndexOf(".");
            nameProp = primaryKeyClass.substring(pos + 1, pos + 2).toLowerCase() + primaryKeyClass.substring(primaryKeyClass.lastIndexOf(".") + 2);
        } else nameProp = primaryKeyClass.substring(0, 1).toLowerCase() + primaryKeyClass.substring(1);
        return nameProp;
    }

    /**
     * @since jcompany 2.7.
     * Remove VOs marcados para exclus�o de uma cole��o.
     * @param listOrigin lista sem VOs marcados
     */
    public void excludeMarked(List listOrigin) throws PlcException {
        log.debug("######## Entered in marked");
        Iterator i = listOrigin.iterator();
        while (i.hasNext()) {
            Object entity = i.next();
            try {
                if ("S".equals(PropertyUtils.getProperty(entity, "indExcPlc").toString())) i.remove();
            } catch (Exception e) {
                throw new PlcException("jcompany.erro.generico", new Object[] { "excludeMarked", e }, e, log);
            }
        }
    }

    /**
     * @since jCompany 2.7.3
     * Recebe uma rela��o de valores de propriedades e uma instancia de JavaBean
     * (getters and setters declarados) e devolve esta inst�ncia com valores preenchidos.
     * @param bean JavaBean a ser preenchido
     * @param propsNames Rela��o de propriedades
     * @param propsValues Rela��o de valores respectivos, das propriedades
     */
    public Object fillEntityWithObjectArray(Object bean, String[] propsNames, Object[] propsValues) throws PlcException {
        log.debug("######## Entered in fillEntityWithObjectArray");
        try {
            for (int i = 0; i < propsNames.length; i++) {
                PropertyUtils.setProperty(bean, propsNames[i], propsValues[i]);
            }
            return bean;
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "fillEntityWithObjectArray", e }, e, log);
        }
    }

    /**
     * @since jCompany 3.0
     * Devolve a rela��o de propriedades de um type definido no vo informado. Procura
     * e aceita tamb�m propriedades sem sufixo ENTITY, para atender a tipos abstratos
     * @param entity Bean a ser investigado
     * @param type Tipo (Classe com package, tipicamente) para ver propriedades
     * @return O nome das propriedades para o type informado, em ordem aleat�ria
     */
    public List getAggregatePropertyForType(Object entity, String type) throws PlcException {
        log.debug("######## Entered in getAggregatePropertyForType");
        try {
            String entityDefaultSuffix = PlcConfigHelper.getInstance().get(PlcConfigSuffixClass.class).entitySuffix();
            PropertyDescriptor[] pd = PropertyUtils.getPropertyDescriptors(entity);
            List l = new ArrayList();
            String alternativeType = type;
            if (type.endsWith(entityDefaultSuffix)) alternativeType = type.substring(0, type.indexOf(entityDefaultSuffix)); else alternativeType = type + entityDefaultSuffix;
            for (int k = 0; k < pd.length; k++) {
                if (log.isDebugEnabled()) log.debug("type property =" + pd[k].getPropertyType().getName());
                if (pd[k].getPropertyType().getName().equals(type) || pd[k].getPropertyType().getName().equals(alternativeType)) {
                    log.debug("Entered!");
                    l.add(pd[k].getName());
                }
            }
            return l;
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "getAggregatePropertyForType", e }, e, log);
        }
    }

    public List<Collection> getCollectionsToClass(Object entity, Class<?> classDestiny) throws PlcException {
        log.debug("######## Entered in getCollectionsToClass");
        try {
            PropertyDescriptor[] pd = PropertyUtils.getPropertyDescriptors(entity);
            List<Collection> l = new ArrayList<Collection>();
            for (int k = 0; k < pd.length; k++) {
                if (log.isDebugEnabled()) log.debug("type property =" + pd[k].getPropertyType().getName());
                if (Collection.class.isAssignableFrom(pd[k].getPropertyType())) {
                    log.debug("Entered!");
                    Collection collection = (Collection) pd[k].getReadMethod().invoke(entity, (Object[]) null);
                    if (collection != null && collection.size() > 0) {
                        Iterator iter = collection.iterator();
                        Object first = null;
                        while (first == null && iter.hasNext()) {
                            first = iter.next();
                        }
                        if (first != null && (classDestiny.isAssignableFrom(first.getClass()) || first.getClass().isAssignableFrom(classDestiny))) {
                            l.add(collection);
                        }
                    }
                }
            }
            return l;
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "getCollectionsToClass", e }, e, log);
        }
    }

    /**
     * @since jCompany 3.0
	 *  jCompany. M�todo para clonar um ArrayList de VOs do type PlcBaseEntity (ex: detalhes)
	 * @param 	list ArrayList a ser clonado.
	 * @return 	clonedList, resultado da clonagem.
	 * @throws 	Exception
	 */
    public List clone(List list) throws Exception {
        log.debug("########## Entered in clone");
        ArrayList clonedList = clonedList = new ArrayList();
        Iterator i = list.iterator();
        while (i.hasNext()) {
            Object next = i.next();
            Object cloneBean = BeanUtils.cloneBean(next);
            clonedList.add(cloneBean);
        }
        if (log.isDebugEnabled()) log.debug("Number of Cloned elements: " + clonedList.size());
        return clonedList;
    }

    /**
     * @since jCompany 3.0
	 *  jCompany. M�todo para clonar um Set de VOs do type PlcBaseEntity (ex: detalhes)
	 * @param 	list ArrayList a ser clonado.
	 * @return 	clonedList, resultado da clonagem.
	 * @throws 	Exception
	 */
    public Set cloneSet(Set s) throws Exception {
        HashSet clonedSet = clonedSet = new HashSet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            clonedSet.add(BeanUtils.cloneBean(i.next()));
        }
        return clonedSet;
    }

    /**
	 * @since jCompany 3.0
	 * JCompany. Recebe o nome com pacote de uma classe e devolve somente o nome da Classe.
	 */
    public String getClassNameWithoutPackage(String className) throws PlcException {
        if (className.endsWith(".class")) className = className.substring(0, className.lastIndexOf(".class"));
        String nomeVO = null;
        try {
            nomeVO = className.substring(className.lastIndexOf(".") + 1);
        } catch (Exception ex) {
            throw new PlcException("jcompany.errors.get.entity", null, ex, log);
        }
        return nomeVO;
    }

    /**
	 * @since jCompany 3.0
  	 * N�o precisa fazer nada nesta vers�o, j� que o transfereBeans j� coloca um novo ENTITY na
  	 * propriedade agregada.
     */
    public void retrieveAggregatedClassLazy(Object entity) throws PlcException {
        log.debug("######## Entered in retrieveAggregatedClassLazy");
        try {
            Class[] lazyAggregatedClasses = (Class[]) PropertyUtils.getSimpleProperty(entity, PlcConstantsCommons.ATTRIBUT_AGGREGATE_LAZY);
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "retrieveAggregatedClassLazy", e }, e, log);
        }
    }

    /**
 	 * @since jCompany 1.5
 	 * Anula objetos agregados, para facilitar exclus�o pela Hibernate
 	 * @param ENTITY Value Object a ser inspecionado e que ter� suas classes agregadas anuladas.
 	 * @throws PlcException Trata exce��es e transforma em PlcException, para tratamento gen�rico e exibi��o para usu�rio.
 	 * @deprecated Este m�todo � mantido por compatibilidade com jcompany beta. N�o � mais necess�rio e ser� removido de vers�es futuras.
 	 */
    public void annulObjects(Object entity) throws PlcException {
        log.debug("######## Entered in annulObjects");
        try {
            Method method = null;
            Object[] objValue = null;
            Class classEntity = entity.getClass();
            BeanInfo info = Introspector.getBeanInfo(classEntity);
            PropertyDescriptor[] pd = info.getPropertyDescriptors();
            if (pd != null) {
                for (int i = 0; i < pd.length; i++) {
                    if (log.isDebugEnabled()) log.debug("annulObjects: attribut name = " + pd[i].getName());
                    if (log.isDebugEnabled()) log.debug("annulObjects: attribut type = " + pd[i].getPropertyType().getName());
                    if (PlcConstantsCommons.PLC_CLASS_ENTITY.isAssignableFrom(pd[i].getPropertyType()) && !(pd[i].getPropertyType().getName().indexOf("org.jcompany") >= 0)) {
                        method = pd[i].getWriteMethod();
                        objValue = new Object[1];
                        objValue[0] = null;
                        method.invoke(entity, objValue);
                        log.debug("anulaObjetos: atributo valor =  null");
                    }
                }
            }
        } catch (Exception ex) {
            throw new PlcException("jcompany.errors.persistence.annul.objects", new Object[] { ex }, ex, log);
        }
    }

    /**
      * @since jCompany 3.0
      * @param detailSet lista com os detalhes
      * @param id identificador a ser procurado
      * @return PlcBaseEntity quando � encontrado, null caso n�o
      */
    public Object containDetail(Set detailSet, Long id) throws PlcException {
        log.debug("###### Entered in containDetail");
        if (id == null) {
            log.debug("id equals to null.");
            return null;
        }
        for (Iterator iter = detailSet.iterator(); iter.hasNext(); ) {
            Object entity = iter.next();
            try {
                if (id.longValue() == ((Long) PropertyUtils.getProperty(entity, "id")).longValue()) {
                    log.debug("it contains detail with id : " + id);
                    return entity;
                }
            } catch (Exception e) {
                throw new PlcException("jcompany.erro.generico", new Object[] { "containDetail", e }, e, log);
            }
        }
        log.debug("it doens't contain detail with id: " + id);
        return null;
    }

    /**
      * @since jCompany 3.0
 	 * JCompany. Recebe o nome completo do type do Value Object e
 	 * devolve somente o nome da Classe.
 	 */
    public String getEntityName(String entityType) throws PlcException {
        log.debug("############# Entered in getEntityName");
        String entityName = null;
        try {
            entityName = entityType.substring(entityType.lastIndexOf(".") + 1);
            if (log.isDebugEnabled()) log.debug("ENTITY name=" + entityName);
        } catch (Exception ex) {
            throw new PlcException("jcompany.erros.get.entity", null, ex, log);
        }
        return entityName;
    }

    /**
	 * @since jCompany 3.0
	 * Clona o vo principal e todos os VOs em cole��es descendentes.
	 * @param entity ENTITY Mestre
	 * @param cleanId Se deve ou n�o limpar os ids ap�s a clonagem (recomendado para OIDs)
	 * @return voi Mestre e seus detalhes clonados.
	 */
    public Object cloneEntityWithDetails(List detailNames, Object entity, boolean cleanId) throws PlcException {
        try {
            Object versionEntity = PlcBeanCloneHelper.getInstance().cloneBean(entity);
            if (cleanId) PropertyUtils.setProperty(versionEntity, "id", null);
            cloneDetails(detailNames, versionEntity, entity, cleanId);
            return versionEntity;
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "cloneEntityWithDetails", e }, e, log);
        }
    }

    /**
     * @since jCompany 3.0
     * Clonas todos os detalhes padr�es do jCompany. <br>
     * <br>
     * 
     * @param versionEntity
     *            vo que receber� os detalhes clonados.
     * @param originEntity
     *            vo com os detalhes que ser�o clonados.
     * @param cleanId true para atribuir nulo no id dos detalhes.
     */
    private void cloneDetails(List detailNames, Object versionEntity, Object originEntity, boolean cleanId) throws Exception {
        if (detailNames == null) return;
        for (Iterator iter = detailNames.iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            if (name.indexOf("_Det") >= 0) {
                name = name.substring(0, name.indexOf("_"));
            }
            Collection versionDetail = cloneDetail(name, originEntity, versionEntity, cleanId);
            PropertyUtils.setProperty(versionEntity, name, versionDetail);
        }
    }

    /**
     * @since jCompany 3.0
     * Clona o set de detalhe do vo.<br>
     * <br>
     * @param detailName nome do detalhe que ser� clonado
     * @param originDate vo que contem o set de detalhe que ser� clonado.
     * @param versionEntity vo mestre dos detalhes
     * @param cleanId true para atribuir nulo no id dos detalhes.
     * @return Set com os detalhes clonados. null se o set for nulo.
     * @throws PlcException
     */
    private Collection cloneDetail(String detailName, Object originDate, Object versionEntity, boolean cleanId) throws PlcException {
        log.debug("############### Entered in cloneDetail");
        try {
            Collection details = (Collection) PropertyUtils.getProperty(originDate, detailName);
            if (details != null && details.size() > 0) {
                Collection clonedDetails = null;
                if (Set.class.isAssignableFrom(details.getClass())) clonedDetails = new HashSet(); else clonedDetails = new ArrayList();
                for (Iterator iter = details.iterator(); iter.hasNext(); ) {
                    Object detail = iter.next();
                    Object clonedDetail = BeanUtilsBean.getInstance().cloneBean(detail);
                    String propertyNamePlc = (String) PropertyUtils.getProperty(versionEntity, "propertyNamePlc");
                    PropertyUtils.setProperty(clonedDetail, propertyNamePlc, versionEntity);
                    if (cleanId) PropertyUtils.setProperty(clonedDetail, "id", null);
                    clonedDetails.add(clonedDetail);
                }
                return clonedDetails;
            } else return new HashSet();
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "cloneDetail", e }, e, log);
        }
    }

    /**
     * @since jCompany 3.0
    */
    public String[] mountStringFromObjectList(List argTypes) throws PlcException {
        log.debug("############### Entered in mountStringFromObjectList");
        String[] lTipoS = new String[argTypes.size()];
        int cont = 0;
        for (Iterator iter = argTypes.iterator(); iter.hasNext(); ) {
            String tipo = (String) iter.next();
            lTipoS[cont] = tipo;
            cont++;
        }
        return lTipoS;
    }

    /**
	 * @since jCompany 3.0
	 * Torna todos os VOs (Mestre, Detalhe e SubDetalhes) de um grafo, na situa��o
	 * passada. Se alguns deles n�o contiverem a propriedade STATUS_HISTORIC_PLC, despreza
	 * @param context Contexto contendo defini��es do grafo
	 * @param entity ENTITY principal
	 * @param statusHistoricPlc Situa��o para atualiza��o
	 */
    public void updateStatusAll(PlcBaseContextVO context, Object entity, String statusHistoricPlc) throws PlcException {
        log.debug("############### Entered in updateStatusAll");
        try {
            PropertyUtils.setProperty(entity, ENTITY.STATUS_HISTORIC_PLC, statusHistoricPlc);
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "updateStatusAll", e }, e, log);
        }
    }

    /**
	 * @since jCompany 3.0
	 * Devolve nome padrao para propriedade agregada para classe informada.
	 * Ex: para classe gov.empresa.rh.vo.CandidatoVO devolve 'candidato'
	 * @param clazz nome da classe com pacote
	 * @return nome da propriedade padrao sugerido
	 */
    public String getDefaultPropName(String clazz) throws PlcException {
        String auxProp = clazz.substring(clazz.lastIndexOf(".") + 1);
        String entityDefaultSuffix = PlcConfigHelper.getInstance().get(PlcConfigSuffixClass.class).entitySuffix();
        if (clazz.endsWith(entityDefaultSuffix)) return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1, auxProp.indexOf(entityDefaultSuffix)); else return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1);
    }

    /**
	 * @since jCompany 3.0
	 * Devolve nome padrao para propriedade agregada para classe informada.
	 * Ex: para classe gov.empresa.rh.vo.CandidatoVO devolve 'candidato'
	 * @param clazz nome da classe com pacote
	 * @return nome da propriedade padrao sugerido
	 */
    public String getSimpleClassNameWithoutEntitySuffix(Class clazz) throws PlcException {
        String entityDefaultSuffix = PlcConfigHelper.getInstance().get(PlcConfigSuffixClass.class).entitySuffix();
        if (clazz.getSimpleName().endsWith(entityDefaultSuffix)) return clazz.getSimpleName().substring(0, clazz.getSimpleName().indexOf(entityDefaultSuffix)); else return clazz.getSimpleName();
    }

    /**
	 * @since jCompany 3.0
	 * Devolve nome padrao para propriedade agregada para classe informada.
	 * Ex: para classe gov.empresa.rh.vo.CandidatoVO devolve 'candidatoVO'
	 * @param clazz nome da classe com pacote
	 * @return nome da propriedade padrao sugerido
	 */
    public String getDefaultNamePropWithSuffix(String clazz) throws PlcException {
        String auxProp = clazz.substring(clazz.lastIndexOf(".") + 1);
        return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1);
    }

    /**
	 * @since jCompany 3.0
	 * Recebe um ENTITY e rela�ao de propriedades para averiguar se est� com valor nulo. 
	 * @param entity Value Object a ser inspecionado.
	 * @param propsNames Nome das propriedades a serem inspecionadas. Ex: {id,nome,salario}
	 * @return nome das propriedades que est�o nulas no vo passado. Ex: {salario} (supondo que somente este esteja.
	 */
    public String[] retrievePropsWithNullValue(Object entity, String[] propsNames) throws PlcException {
        if (log.isDebugEnabled()) log.debug("############### Entered in retrievePropsWithNullValue - entity " + entity);
        try {
            String[] nullPropsNames = new String[propsNames.length];
            int counter = 0;
            for (int i = 0; i < propsNames.length; i++) {
                String prop = propsNames[i];
                if (prop != null && PropertyUtils.getProperty(entity, prop) == null) {
                    nullPropsNames[counter] = prop;
                    counter++;
                }
            }
            return nullPropsNames;
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "retrievePropsWithNullValue", e }, e, log);
        }
    }

    /**
	 * @since jCompany 3.0
	 * Coloca valor na propriedade correspondente do vo, convertendo de String para o Tipo especifico.
	 * Considera Long, Integer, Double, Date (DD/mm/YYYY) e String
	 * @param entityArg ENTITY
	 * @param property
	 * @param value
	 */
    public void setProperty(Object entityArg, String property, String value) throws PlcException {
        log.debug("############### Entered in setProperty");
        try {
            Class propertyClass = PropertyUtils.getPropertyDescriptor(entityArg, property).getClass();
            if (Long.class.isAssignableFrom(PropertyUtils.getPropertyDescriptor(entityArg, property).getPropertyType())) {
                PropertyUtils.setProperty(entityArg, property, new Long(value));
            } else if (Integer.class.isAssignableFrom(PropertyUtils.getPropertyDescriptor(entityArg, property).getPropertyType())) {
                PropertyUtils.setProperty(entityArg, property, Integer.valueOf(value));
            } else if (Double.class.isAssignableFrom(PropertyUtils.getPropertyDescriptor(entityArg, property).getPropertyType())) {
                PropertyUtils.setProperty(entityArg, property, new Double(value));
            } else if (Date.class.isAssignableFrom(PropertyUtils.getPropertyDescriptor(entityArg, property).getPropertyType())) {
                SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy");
                Date dataValue = sf.parse(value);
                PropertyUtils.setProperty(entityArg, property, dataValue);
            } else if (String.class.isAssignableFrom(PropertyUtils.getPropertyDescriptor(entityArg, property).getPropertyType())) {
                PropertyUtils.setProperty(entityArg, property, value);
            } else throw new PlcException("jcompany.erro.setproperty", new Object[] { entityArg.getClass().getName(), property, value, propertyClass.getName() });
        } catch (PlcException plcE) {
            throw plcE;
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "setProperty", e }, e, log);
        }
    }

    /**
	 * Instancia um objeto para a classe informada
	 * @param clazz Classe a ser instanciada
	 * @return Objeto instanciado
	 */
    public Object createInstance(String clazz) throws PlcException {
        try {
            return Class.forName(clazz).newInstance();
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "createInstance", e }, e, log);
        }
    }

    /**
	 * Remove o sufixo padrao do nome da Entidade
	 * @param simpleName
	 * @return simpleName sem sufixo.
	 */
    public String removeSuffixEntity(String simpleName) throws PlcException {
        String entityDefaultSuffix = PlcConfigHelper.getInstance().get(PlcConfigSuffixClass.class).entitySuffix();
        return simpleName.substring(0, simpleName.indexOf(entityDefaultSuffix));
    }

    /**
     * @since jCompany 3.0
     * C�digo padronizado para equals, baseado em valores declarados como chave de neg�cio do ENTITY. 
     */
    public boolean equalsPrimaryKey(Object main, Object other) throws PlcException {
        PlcPrimaryKey pk = (PlcPrimaryKey) this.getClass().getAnnotation(PlcPrimaryKey.class);
        try {
            if (pk != null) {
                Object thisIdNatural = PropertyUtils.getProperty(main, "dynamicNaturalId");
                Object otherIdNatural = PropertyUtils.getProperty(other, "dynamicNaturalId");
                return equalsPlc(thisIdNatural, otherIdNatural, (String[]) PropertyUtils.getProperty(main, "propsNaturalKeyPlc"));
            } else return equalsPlc(this, other, new String[] { "id" });
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "equalsPrimaryKey", e }, e, log);
        }
    }

    protected static boolean equalsPlc(Object main, Object other, String... props) {
        if (other == null || !(main.getClass().isAssignableFrom(other.getClass()) || other.getClass().isAssignableFrom(main.getClass()))) return false;
        if (main == other) {
            return true;
        }
        try {
            Collection<String> propSet = props == null || props.length == 0 ? (Collection<String>) getPropsAllPlc(main) : Arrays.asList(props);
            for (String oneProp : propSet) {
                try {
                    if (!propertyEqual(main, other, oneProp)) return false;
                } catch (Exception e) {
                    log.fatal("jCompany. Fatal Error trying to compare entity  " + main + " to " + other + " for property " + oneProp + ERROR_ORIGINAL + e);
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.fatal("jCompany. Fatal Error trying to compare entity " + main + " to " + other + ERROR_ORIGINAL + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    protected static boolean propertyEqual(Object one, Object other, String prop) throws Exception {
        try {
            if (!PropertyUtils.isReadable(one, prop)) {
                return true;
            }
            if (prop.equals("versao")) return true;
            Object valueOne = PropertyUtils.getProperty(one, prop);
            Object valueOther = PropertyUtils.getProperty(other, prop);
            if (prop.equals("id")) {
                if (one.getClass().getAnnotation(Embeddable.class) != null) return true;
                valueOther = PropertyUtils.getProperty(other, "id");
            }
            if ((valueOne == null && valueOther == null) && prop.equals("id")) {
                return false;
            }
            if (valueOne == valueOther) {
                return true;
            }
            if ((valueOne == null || valueOther == null) && !Collection.class.isAssignableFrom(PropertyUtils.getPropertyType(one, prop))) {
                return false;
            } else if (Collection.class.isAssignableFrom(PropertyUtils.getPropertyType(one, prop))) {
                return true;
            } else if (valueOne.getClass().isArray()) {
                if (!valueOther.getClass().isArray()) {
                    return false;
                }
                Class<?> componentType = valueOne.getClass().getComponentType();
                if (componentType.isPrimitive()) {
                    if (!valueOne.getClass().getComponentType().equals(valueOther.getClass().getComponentType())) {
                        return false;
                    }
                    if (componentType.equals(boolean.class)) {
                        return Arrays.equals((boolean[]) valueOne, (boolean[]) valueOther);
                    }
                    if (componentType.equals(byte.class)) {
                        return Arrays.equals((byte[]) valueOne, (byte[]) valueOther);
                    }
                    if (componentType.equals(char.class)) {
                        return Arrays.equals((char[]) valueOne, (char[]) valueOther);
                    }
                    if (componentType.equals(double.class)) {
                        return Arrays.equals((double[]) valueOne, (double[]) valueOther);
                    }
                    if (componentType.equals(float.class)) {
                        return Arrays.equals((float[]) valueOne, (float[]) valueOther);
                    }
                    if (componentType.equals(int.class)) {
                        return Arrays.equals((int[]) valueOne, (int[]) valueOther);
                    }
                    if (componentType.equals(long.class)) {
                        return Arrays.equals((long[]) valueOne, (long[]) valueOther);
                    }
                    if (componentType.equals(short.class)) {
                        return Arrays.equals((short[]) valueOne, (short[]) valueOther);
                    }
                }
                return Arrays.equals((Object[]) valueOne, (Object[]) valueOther);
            }
            if ((PropertyUtils.isReadable(valueOne, "id") && PropertyUtils.isReadable(valueOther, "id")) || (PropertyUtils.isReadable(valueOne, "dynamicNaturalId") && PropertyUtils.isReadable(valueOther, "dynamicNaturalId"))) {
                if (PropertyUtils.getProperty(valueOne, "id") != null) {
                    return ((Long) PropertyUtils.getProperty(valueOne, "id")).longValue() == ((Long) PropertyUtils.getProperty(valueOther, "id")).longValue();
                } else if (PropertyUtils.getProperty(valueOne, "dynamicNaturalId") != null) {
                    return PropertyUtils.getProperty(valueOne, "dynamicNaturalId").equals(PropertyUtils.getProperty(valueOther, "dynamicNaturalId"));
                } else {
                    return valueOne.equals(valueOther);
                }
            }
            return valueOne.equals(valueOther);
        } catch (Exception e) {
            log.fatal("jCompany. Fatal Error trying to compare entity " + one + " to " + other + " for property =" + prop + ERROR_ORIGINAL + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    /**
   * Novo! Verifica se entidade � identificada (via Object-id ou Chave Natural
   */
    public boolean isIdentified(Object entity) throws PlcException {
        try {
            boolean isIdentified = true;
            if (PropertyUtils.isReadable(entity, "id")) {
                if (PropertyUtils.getProperty(entity, "id") == null) isIdentified = false;
            } else {
                if (!PropertyUtils.isReadable(entity, "propsNaturalKeyPlc")) throw new PlcException("The object isn't a Entity");
                Object[] props = (Object[]) PropertyUtils.getProperty(entity, "propsNaturalKeyPlc");
                for (int i = 0; i < props.length; i++) {
                    String prop = (String) props[i];
                    try {
                        Object value = null;
                        if (prop.equals("id")) value = PropertyUtils.getProperty(entity, "id"); else value = PropertyUtils.getSimpleProperty(PropertyUtils.getSimpleProperty(this, "idNatural"), prop);
                        if (value == null) {
                            isIdentified = false;
                        }
                    } catch (Exception e) {
                        log.fatal("Error trying to get property value " + prop + " Error:" + e);
                        e.printStackTrace();
                    }
                }
            }
            return isIdentified;
        } catch (Exception e) {
            throw new PlcException("jcompany.error.generic", new Object[] { "isIdentified", e }, e, log);
        }
    }

    /**
   * @since 3.1
   * @param obj Objeto a ser investigado
   * @return Conjunto de propriedades para compara��o completa
   */
    private static Set<String> getPropsAllPlc(Object obj) {
        Set<String> propSet = PROPS_ALL_PLC_MAP.get(obj.getClass());
        if (propSet == null) {
            try {
                PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(obj.getClass());
                propSet = new HashSet<String>();
                for (PropertyDescriptor desc : propertyDescriptors) {
                    String name = desc.getName();
                    if (!name.endsWith("Aux") && !name.endsWith("Str") && !name.endsWith("Fon") && !name.endsWith("usuarioUltAlteracao") && !name.endsWith("dataUltAlteracao") && !name.endsWith("versao") && isBeanProperty(desc)) {
                        propSet.add(name);
                    }
                }
                PROPS_ALL_PLC_MAP.put(obj.getClass(), propSet);
            } catch (Exception e) {
                log.fatal("jCompany. Fatal error trying to retrieve entity properties " + obj + ERROR_ORIGINAL + e);
                e.printStackTrace();
            }
        }
        return propSet;
    }

    /**
   * Determina de uma propriedade � uma propriedade bean com leitura e escrita.
   * @param property A descri��o da propriedade
   * @return true se a propriedade tem m�todo Read e Write
   */
    private static boolean isBeanProperty(final PropertyDescriptor property) {
        Method readMethod = property.getReadMethod();
        if ((readMethod == null) && (property instanceof IndexedPropertyDescriptor)) {
            readMethod = ((IndexedPropertyDescriptor) property).getIndexedReadMethod();
        }
        Method writeMethod = property.getWriteMethod();
        if ((writeMethod == null) && (property instanceof IndexedPropertyDescriptor)) {
            writeMethod = ((IndexedPropertyDescriptor) property).getIndexedWriteMethod();
        }
        return readMethod != null && writeMethod != null;
    }

    /**
     * Devolve o nome da propriedade padr�o para este ENTITY. O padr�o � seu nome final com inicial min�scula e
     * sem o ENTITY (nome abstrato). Pode ser sobreposto nos descendentes, se desejado
     * @since jCompany 3.0
     */
    public String getPropertyNamePlc(Object entity) {
        String entityDefaultSuffix = "ENTITY";
        try {
            entityDefaultSuffix = PlcConfigHelper.getInstance().get(PlcConfigSuffixClass.class).entitySuffix();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        String auxProp = entity.getClass().getName().substring(entity.getClass().getName().lastIndexOf(".") + 1);
        if (this.getClass().getName().endsWith(entityDefaultSuffix)) return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1, auxProp.indexOf(entityDefaultSuffix)); else return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1);
    }

    /**
     * the object is an Entity?
     */
    public boolean isEntity(Object object) {
        if (object.getClass().equals(Class.class)) {
            log.error("This method should receive an instance");
            return false;
        } else {
            return PropertyUtils.isReadable(object, "id") || PropertyUtils.isReadable(object, "dynamicNaturalId");
        }
    }

    /**
     * the object is an Entity?
     */
    public boolean isEntity(Class classe) {
        Class[] c = null;
        try {
            classe.getMethod("getId", c);
            return true;
        } catch (Exception e) {
        }
        try {
            classe.getMethod("getDynamicNaturalId", c);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public String getEditionLinkPlc(Object entity) {
        StringBuilder link = new StringBuilder();
        try {
            if (PropertyUtils.isReadable(entity, "id")) {
                link.append("&chPlc=").append(PropertyUtils.getProperty(entity, "id").toString());
            } else if (PropertyUtils.isReadable(entity, "idAux")) {
                try {
                    link.append("&chPlc=").append(URLEncoder.encode(PropertyUtils.getProperty(entity, "idAux").toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    link.append("&chPlc=").append(PropertyUtils.getProperty(entity, "idAux").toString());
                    log.fatal("jCompany. Fatal Error trying to mount edition link ENTITY " + this + " for property " + " idAux" + ERROR_ORIGINAL + e.toString());
                    e.printStackTrace();
                }
            } else {
                Object idNatural = PropertyUtils.getProperty(entity, "dynamicNaturalId");
                if (idNatural != null) {
                    link.append("&evento=y");
                    for (String property : (String[]) PropertyUtils.getProperty(entity, "propsNaturalKeyPlc")) {
                        try {
                            link.append("&").append(property).append("=");
                            Class<?> propertyType = PropertyUtils.getPropertyDescriptor(idNatural, property).getPropertyType();
                            if (propertyType != null && propertyType.equals(java.util.Date.class)) {
                                Timestamp fieldValue = (Timestamp) PropertyUtils.getProperty(idNatural, property);
                                if (fieldValue != null) {
                                    link.append(fieldValue.getTime());
                                }
                            } else if (propertyType != null && PlcBaseEntity.class.isAssignableFrom(propertyType)) {
                                PlcBaseEntity b = (PlcBaseEntity) PropertyUtils.getProperty(idNatural, property);
                                link.append(b.getIdAux());
                            } else link.append(URLEncoder.encode(PropertyUtils.getProperty(idNatural, property).toString(), "ISO-8859-1"));
                        } catch (Exception e) {
                            log.fatal("jCompany. Fatal Error trying to mount edition link  ENTITY " + this + " for property " + property + ERROR_ORIGINAL + e.toString());
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Must use 'id' or 'idAux' or 'dynamicNaturalId' to identify Entities", e);
        }
        return link.toString();
    }
}
