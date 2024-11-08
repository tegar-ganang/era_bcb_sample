package org.jcompany.commons;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.jcompany.commons.annotation.PlcPrimaryKey;
import org.jcompany.commons.annotation.PlcEntityMetadata;
import org.jcompany.config.PlcConfigHelper;
import org.jcompany.config.commons.PlcConfigSuffixClass;

/**
 * jCompany 2.5.3. Value Object ancestral para todos os VOs de aplica��es com
 * persist�ncia, inclusive VOs de Chave Natural. Traz servi�os de equals,
 * toString e hashCode gen�ricos, bem como manipuladores de identifica��o
 * gen�ricos.
 */
public class PlcBaseEntity implements Serializable, Cloneable {

    private static final String ERROR_ORIGINAL = ". Original Error: ";

    protected static final Logger log = Logger.getLogger(PlcBaseEntity.class);

    private static final long serialVersionUID = -6328544514859752259L;

    protected static String[] PROPS_NATURAL_KEY_PLC = new String[] { "id" };

    /**
	 * Mapa de propriedades das subclasses de PlcBaseEntity. � preenchida conforme a necessidade, e pode ser coletado pelo GC (usa weak reference).
	 * @since jCompany 3.0.2
	 */
    private static final WeakHashMap<Class, Set<String>> PROPS_ALL_PLC_MAP = new WeakHashMap<Class, Set<String>>();

    /**
     * Devolve a declara��o est�tica de propriedades chave. Deve ser
     * especializado para permitir polimorfismo sobre declara��o est�tica e montagem
     * de hiperlinks gen�ricos para recupera��o
     *
     * @return String est�tico com rela��o de propriedades chave.
     * @since jCompany 3.0
	 */
    public String[] getPropsNaturalKeyPlc() {
        PlcPrimaryKey pk = this.getClass().getAnnotation(PlcPrimaryKey.class);
        if (pk == null || PlcBaseEntity.class.equals(pk.classe())) return PROPS_NATURAL_KEY_PLC;
        return pk.properties();
    }

    /**
     * @since 3.1
     * @param obj Objeto a ser investigado
     * @return Conjunto de propriedades para compara��o completa
     */
    private static Set<String> getPropsAllPlc(PlcBaseEntity obj) {
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

    /** Chave Object Id Gen�rica * */
    protected Long id = null;

    protected String idAux = "";

    /** Chave Natural Gen�rica */
    protected PlcBaseEntity idNatural = null;

    /** Auxiliar para gravar estados transientes * */
    protected String indExcPlc = "N";

    /** Guarda hashCode **/
    protected int hashCodePlc = 0;

    /**
     * Mapeamento padr�o para OID, para todas as classes
     * @since jCompany 3.0
     */
    public java.lang.Long getId() {
        return id;
    }

    public void setId(java.lang.Long newId) {
        id = newId;
    }

    /**
     * OID auxiliar em forma string para entradas de dados tabulares, que n�o
     * possam usar o form-bean da struts
     * @since jCompany 3.0
     */
    public String getIdAux() {
        if (getId() != null) return (getId().toString()); else return "";
    }

    /**
     * @since jCompany 3.0
     */
    public void setIdAux(String newIdAux) {
        idAux = newIdAux;
        if (idAux != null && !idAux.equals("")) {
            this.id = new Long(idAux);
            setId(this.id);
        } else {
            id = null;
            setId(null);
        }
    }

    /**
     * Devolve o nome da propriedade padr�o para este ENTITY. O padr�o � seu nome final com inicial min�scula e
     * sem o ENTITY (nome abstrato). Pode ser sobreposto nos descendentes, se desejado
     * @since jCompany 3.0
     */
    public String getPropertyNamePlc() {
        String entityDefaultSuffix = "ENTITY";
        try {
            entityDefaultSuffix = PlcConfigHelper.getInstance().get(PlcConfigSuffixClass.class).entitySuffix();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        String auxProp = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".") + 1);
        if (this.getClass().getName().endsWith(entityDefaultSuffix)) return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1, auxProp.indexOf(entityDefaultSuffix)); else return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1);
    }

    /**
     * Chama dinamicamente porque XDoclet exige o m�todo getIdNatural no
     * descenente
     * @since jCompany 3.0
     */
    public PlcBaseEntity getDynamicNaturalId() {
        PlcBaseEntity idNatural = null;
        if (PropertyUtils.isReadable(this, "idNatural")) {
            try {
                idNatural = (PlcBaseEntity) PropertyUtils.getSimpleProperty(this, "idNatural");
            } catch (Exception e) {
                log.fatal("Error trying to class getIdNatural method in " + this.getClass().getName());
            }
        }
        return idNatural;
    }

    /**
     * @since jCompany 3.0 
     */
    public void setIdNatural(PlcBaseEntity idNatural) {
        this.idNatural = idNatural;
    }

    /**
     * Auxiliar que indica para cada objeto que este dever� ser excluido.
     * Utilizado em padr�es Tabular ou Detalhes e preenchido em fun��o do
     * checkbox de inclus�o
     * @since jCompany 3.0
     */
    public String getIndExcPlc() {
        return indExcPlc;
    }

    /**
     * @since jCompany 3.0 
     */
    public void setIndExcPlc(String newIndExcPlc) {
        indExcPlc = newIndExcPlc;
    }

    /**
     * @deprecated Utilizar equalsPlc est�tico, passando (this,(MinhaClasse)outro,new String[]{"prop1","prop2"});
     */
    protected boolean equalsPlc(Object other, Object[] props) {
        String[] sAux = null;
        if (props != null) {
            sAux = new String[props.length];
            for (int i = 0; i < props.length; i++) {
                sAux[i] = props[i].toString();
            }
            ;
        }
        return equalsPlc(this, (PlcBaseEntity) other, sAux);
    }

    /**
     * Processamento gen�rico para c�digo "equals" em VOs. <br>
     * Testa nulidades do outro ENTITY e de valores, retornando false caso somente
     * um dos "lados" esteja nulo. Se ambos estiverem nulos, retorna true. Testa
     * genericamente para os type String, Long, Integer, Double, BigDecimal e
     * java.util.Date Todas as propriedades passadas devem estar iguais para que
     * o m�todo retorne true.
     *
     * @param other
     *            ENTITY a ser comparado com o atual
     * @param props
     *            Object[] com nomes das propriedades.
     * @return true se todas estiverem iguais ou false em caso contr�rio.
     */
    protected static boolean equalsPlc(PlcBaseEntity one, PlcBaseEntity other, String... props) {
        if (other == null || !(one.getClass().isAssignableFrom(other.getClass()) || other.getClass().isAssignableFrom(one.getClass()))) return false;
        if (one == other) {
            return true;
        }
        try {
            Collection<String> propSet = props == null || props.length == 0 ? (Collection<String>) getPropsAllPlc(one) : Arrays.asList(props);
            for (String oneProp : propSet) {
                try {
                    if (!propertyEqual(one, other, oneProp)) return false;
                } catch (Exception e) {
                    log.fatal("jCompany. Fatal Error trying to compare entity  " + one + " to " + other + " for property " + oneProp + ERROR_ORIGINAL + e);
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.fatal("jCompany. Fatal Error trying to compare entity " + one + " to " + other + ERROR_ORIGINAL + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica se uma propriedade � igual em dois vos diferentes.
     * Considerando que os dois s�o instancias do mesmo objeto
     * @since jCompany 3.0
     */
    protected static boolean propertyEqual(PlcBaseEntity one, PlcBaseEntity other, String prop) throws Exception {
        try {
            if (!PropertyUtils.isReadable(one, prop)) {
                return true;
            }
            if (prop.equals("versao")) return true;
            Object valueOne = PropertyUtils.getProperty(one, prop);
            Object valueOther = PropertyUtils.getProperty(other, prop);
            if (prop.equals("id")) {
                if (one.getClass().getAnnotation(Embeddable.class) != null) return true;
                valueOther = ((PlcBaseEntity) other).getId();
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
            if (PlcBaseEntity.class.isAssignableFrom(valueOne.getClass()) && PlcBaseEntity.class.isAssignableFrom(valueOther.getClass())) {
                if (((PlcBaseEntity) valueOne).getId() != null) {
                    return ((PlcBaseEntity) valueOne).getIdAux().equals(((PlcBaseEntity) valueOther).getIdAux());
                } else if (((PlcBaseEntity) valueOne).getDynamicNaturalId() != null) {
                    return ((PlcBaseEntity) valueOne).getDynamicNaturalId().equals(((PlcBaseEntity) valueOther).getDynamicNaturalId());
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
     * jCompany 3.0. Deve ser sobreposto no descendente para passar todas as propriedades do ENTITY,
     * de modo a possibilitar compara��o por todas elas, para casos em que se
     * fa�a necess�rio. Ex: registro de auditoria de tabular.<p>
     * Ex:  return super.equalsPlc(outro,new Object[]{"codigo,descricao,tipoCliente"});
     * O m�todo equals deve ser utilizado para compara��o de identificadores (ex: OID ou Chave Natural)
     * @deprecated Utilizar o "equals" normalmente.
     * @param other Outro ENTITY a comparar
     * @return true se todas as propriedades forem iguais
     */
    public boolean apiTotEquals(Object other) {
        return equalsPlc(this, (PlcBaseEntity) other);
    }

    /**
     * jCompany 2.5.1. Override no m�todo de java.lang.Object utilizando
     * propriedades n�o nulas na exibi��o. Pode-se sobrepor este m�todo nos
     * descendentes para especializar a visualiza��o.
     *
     * @return todas as propriedades n�o nulas do ENTITY
     * 
     * @since jCompany 3.0
     */
    public String toStringPlc(Object[] props) {
        StringBuffer sb = new StringBuffer("[");
        int cont = 0;
        if (props == null) {
            PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(this.getClass());
            if (pds != null) {
                for (int i = 0; i < pds.length; i++) {
                    PropertyDescriptor pd = (PropertyDescriptor) pds[i];
                    if (PropertyUtils.isReadable(this, pd.getName())) {
                        try {
                            Object value = PropertyUtils.getSimpleProperty(this, pd.getName());
                            if (value != null && !(value instanceof PlcBaseEntity) && !(value instanceof java.util.List) && !(value instanceof java.util.ArrayList) && !(value instanceof java.util.Set) && !(value instanceof java.util.Map)) {
                                cont++;
                                if (cont > 1) sb.append(",");
                                sb.append(pd.getName() + "=" + value);
                            }
                        } catch (Exception e) {
                            sb.append(pd.getName() + "=erro:" + e.toString());
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < props.length; i++) {
                String prop = (String) props[i];
                if (PropertyUtils.isReadable(this, prop)) {
                    try {
                        Object value = PropertyUtils.getSimpleProperty(this, prop);
                        if (value != null) {
                            cont++;
                            if (cont > 1) sb.append(",");
                            sb.append(prop + "=" + value);
                        }
                    } catch (Exception e) {
                        sb.append(prop + "=erro:" + e.toString());
                    }
                }
            }
        }
        sb.append("]");
        if (sb.toString().equals("[]")) return ""; else return sb.toString();
    }

    /**
     * @since jCompany 3.1
     * @deprecated Utilizar hashCodePlc sem argumentos. 
     */
    public int hashCodePlc(String[] props) {
        return hashCodePlc();
    }

    /**
     * Utilizada para montagem de hashCode recursiva, situa��o onde somente � montada a chave
     * @since jCompany 3.1
     */
    public int hashCodeKeyPlc() {
        String[] props = getPropsNaturalKeyPlc();
        int result = 17;
        if (props != null) {
            for (int i = 0; i < props.length; i++) {
                try {
                    if (PropertyUtils.isReadable(this, props[i]) && (!Collection.class.isAssignableFrom(PropertyUtils.getPropertyType(this, props[i])))) {
                        Object value = PropertyUtils.getNestedProperty(this, props[i]);
                        int valorAux = value == null ? 0 : value.hashCode();
                        result = result * 37 + valorAux;
                    }
                } catch (Exception e) {
                    log.fatal("Error trying to get hashCode from property " + props[i] + " Erro:" + e);
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Implementa��o de hashCode em conformidade com o padr�o Bloch.
     * Importante: O resultado do "hashCode" deve ser compat�vel com o resultado do "equals". Assim,
     * como o "equals" do jCompany percorre todas as propriedades, o mesmo deve se dar com o hashCode
     * @since jcompany 2.5.3
     */
    public int hashCodePlc() {
        if (this.hashCodePlc == 0) {
            int result = 17;
            Object value = null;
            try {
                Set<String> propsAll = getPropsAllPlc(this);
                for (String prop : propsAll) {
                    value = PropertyUtils.getProperty(this, prop);
                    int valueAux = 0;
                    if (value != null) {
                        if (value.getClass().isArray()) {
                            Class<?> componentType = value.getClass().getComponentType();
                            if (componentType.isPrimitive()) {
                                if (componentType.equals(boolean.class)) {
                                    valueAux = Arrays.hashCode((boolean[]) value);
                                } else if (componentType.equals(byte.class)) {
                                    valueAux = Arrays.hashCode((byte[]) value);
                                } else if (componentType.equals(char.class)) {
                                    valueAux = Arrays.hashCode((char[]) value);
                                } else if (componentType.equals(double.class)) {
                                    valueAux = Arrays.hashCode((double[]) value);
                                } else if (componentType.equals(float.class)) {
                                    valueAux = Arrays.hashCode((float[]) value);
                                } else if (componentType.equals(int.class)) {
                                    valueAux = Arrays.hashCode((int[]) value);
                                } else if (componentType.equals(long.class)) {
                                    valueAux = Arrays.hashCode((long[]) value);
                                } else if (componentType.equals(short.class)) {
                                    valueAux = Arrays.hashCode((short[]) value);
                                }
                            } else {
                                valueAux = Arrays.hashCode((Object[]) value);
                            }
                        } else if (value instanceof PlcBaseEntity) {
                            valueAux = ((PlcBaseEntity) value).hashCodeKeyPlc();
                        } else if (value instanceof Collection) {
                        } else {
                            valueAux = value.hashCode();
                        }
                    }
                    result = result * 37 + valueAux;
                }
            } catch (Exception e) {
                log.fatal("Error trying to get hashCode from property " + value + " Erro:" + e);
                e.printStackTrace();
            }
            this.hashCodePlc = result;
        }
        return this.hashCodePlc;
    }

    /**
     * @since jCompany 3.0
     * C�digo padronizado para equals, baseado em valores declarados como chave de neg�cio do ENTITY. 
     */
    public boolean equalsPrimaryKey(PlcBaseEntity other) {
        PlcPrimaryKey pk = (PlcPrimaryKey) this.getClass().getAnnotation(PlcPrimaryKey.class);
        if (pk != null) {
            PlcBaseEntity thisIdNatural = this.getDynamicNaturalId();
            PlcBaseEntity otherIdNatural = other.getDynamicNaturalId();
            return equalsPlc(thisIdNatural, otherIdNatural, getPropsNaturalKeyPlc());
        } else return equalsPlc(this, other, getPropsNaturalKeyPlc());
    }

    /**
     * @since jCompany 3.0
     * C�digo padronizado para equals, baseado em todos os valores do ENTITY. 
     * Recomenda-se especializar fazendo teste espec�fico ou passando propriedades suficientes
     * como array de String
     */
    public boolean equals(Object other) {
        boolean equal = other instanceof PlcBaseEntity && equalsPlc(this, (PlcBaseEntity) other);
        return equal;
    }

    /**
     * @since jCompany 3.0.
     * C�digo padronizado para hashCode
     */
    public int hashCode() {
        return hashCodePlc();
    }

    /**
     * @since jCompany 2.5.3.
     * C�digo gen�rico para toString
     */
    public String toString() {
        return toStringPlc(getPropsNaturalKeyPlc());
    }

    /**
     * @since jCompany 2.5.3.
     * Link de edi��o gen�rico
     */
    public String getEditionLinkPlc() {
        StringBuilder link = new StringBuilder();
        if (getId() != null) {
            link.append("&chPlc=").append(getId());
        } else if (getIdAux() != null && !getIdAux().trim().equals("")) {
            try {
                link.append("&chPlc=").append(URLEncoder.encode(getIdAux(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                link.append("&chPlc=").append(getIdAux());
                log.fatal("jCompany. Fatal Error trying to mount edition link ENTITY " + this + " for property " + " idAux" + ERROR_ORIGINAL + e.toString());
                e.printStackTrace();
            }
        } else {
            PlcBaseEntity idNatural = this.getDynamicNaturalId();
            if (idNatural != null) {
                link.append("&evento=y");
                for (String property : getPropsNaturalKeyPlc()) {
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
        return link.toString();
    }

    /**
     * @since jCompany 2.5.3.
     * Link de edi��o que inclui todos os argumentos informados com a anotacao PlcNavigation.
     */
    public String getEditionLinkAdvancedPlc() {
        StringBuffer link = new StringBuffer("");
        if (this.getClass().getAnnotation(PlcEntityMetadata.class) != null) {
            String props = ((PlcEntityMetadata) this.getClass().getAnnotation(PlcEntityMetadata.class)).navegacao();
            StringTokenizer st = new StringTokenizer(props, ",");
            while (st.hasMoreElements()) {
                String prop = (String) st.nextElement();
                try {
                    if (PropertyUtils.getPropertyDescriptor(this, prop).getPropertyType().equals(java.util.Date.class)) link.append("&" + prop + "=" + PropertyUtils.getProperty(this, prop + "Aux")); else if (PlcBaseEntity.class.isAssignableFrom(PropertyUtils.getPropertyDescriptor(this, prop).getPropertyType())) {
                        PlcBaseEntity b = (PlcBaseEntity) PropertyUtils.getProperty(this, prop);
                        link.append("&" + prop + "=" + b.getIdAux());
                    } else if (prop.equals("id")) link.append("&" + prop + "=" + getIdAux() + "&chPlc=" + getIdAux()); else link.append("&" + prop + "=" + PropertyUtils.getProperty(this, prop));
                } catch (Exception e) {
                    log.fatal("jCompany. Fatal Error trying to mount advanced edition link (with navigation) do ENTITY " + this + " for property " + prop + ERROR_ORIGINAL + e.toString());
                    e.printStackTrace();
                }
            }
        }
        return link.toString();
    }

    /**
     * @since jCompany 2.7
     * Auxiliar para manipula��o de VOs com chave natural Se o
     * objeto tem seus identificados informados, seja OID ou chave natural
     *
     * @return true se possui todas as propriedades de identifica��o informadas
     */
    public boolean isIdentified() {
        boolean isIdentified = true;
        Object[] props = getPropsNaturalKeyPlc();
        if (props == null) {
            if (getId() == null) isIdentified = false;
        } else {
            for (int i = 0; i < props.length; i++) {
                String prop = (String) props[i];
                try {
                    Object value = null;
                    if (prop.equals("id")) value = getId(); else value = PropertyUtils.getSimpleProperty(PropertyUtils.getSimpleProperty(this, "idNatural"), prop);
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
    }

    public void setIndExcPlc(Boolean value) {
        if (value != null && value.equals(true)) this.setIndExcPlc("S"); else this.setIndExcPlc("N");
    }

    @Transient
    private transient PlcFileEntity attachedFile;

    public PlcFileEntity getAttachedFile() {
        return attachedFile;
    }

    public void setAttachedFile(PlcFileEntity attachedFile) {
        this.attachedFile = attachedFile;
    }
}
