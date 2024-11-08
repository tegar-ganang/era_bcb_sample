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
public class PlcEntity implements Serializable, Cloneable {

    private static final String ERRO_ORIGINAL = ". Erro original: ";

    protected static final Logger log = Logger.getLogger(PlcEntity.class);

    private static final long serialVersionUID = -6328544514859752259L;

    protected static String[] PROPS_CHAVE_NATURAL_PLC = new String[] { "id" };

    /**
	 * Mapa de propriedades das subclasses de PlcEntity. � preenchida conforme a necessidade, e pode ser coletado pelo GC (usa weak reference).
	 * @since jCompany 3.0.2
	 */
    private static final WeakHashMap<Class, Set<String>> PROPS_TODAS_PLC_MAP = new WeakHashMap<Class, Set<String>>();

    /**
     * Devolve a declara��o est�tica de propriedades chave. Deve ser
     * especializado para permitir polimorfismo sobre declara��o est�tica e montagem
     * de hiperlinks gen�ricos para recupera��o
     *
     * @return String est�tico com rela��o de propriedades chave.
     * @since jCompany 3.0
	 */
    public String[] getPropsChaveNaturalPlc() {
        PlcPrimaryKey chavePrimaria = this.getClass().getAnnotation(PlcPrimaryKey.class);
        if (chavePrimaria == null || PlcEntity.class.equals(chavePrimaria.classe())) return PROPS_CHAVE_NATURAL_PLC;
        return chavePrimaria.propriedades();
    }

    /**
     * @since 3.1
     * @param obj Objeto a ser investigado
     * @return Conjunto de propriedades para compara��o completa
     */
    private static Set<String> getPropsTodasPlc(PlcEntity obj) {
        Set<String> propSet = PROPS_TODAS_PLC_MAP.get(obj.getClass());
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
                PROPS_TODAS_PLC_MAP.put(obj.getClass(), propSet);
            } catch (Exception e) {
                log.fatal("jCompany. Erro fatal ao tentar recuperar propriedades do vo " + obj + ERRO_ORIGINAL + e);
                e.printStackTrace();
            }
        }
        return propSet;
    }

    /**
     * Determina de uma propriedade � uma propriedade bean com leitura e escrita.
     * @param desc A descri��o da propriedade
     * @return true se a propriedade tem m�todo Read e Write
     */
    private static boolean isBeanProperty(final PropertyDescriptor desc) {
        Method readMethod = desc.getReadMethod();
        if ((readMethod == null) && (desc instanceof IndexedPropertyDescriptor)) {
            readMethod = ((IndexedPropertyDescriptor) desc).getIndexedReadMethod();
        }
        Method writeMethod = desc.getWriteMethod();
        if ((writeMethod == null) && (desc instanceof IndexedPropertyDescriptor)) {
            writeMethod = ((IndexedPropertyDescriptor) desc).getIndexedWriteMethod();
        }
        return readMethod != null && writeMethod != null;
    }

    /** Chave Object Id Gen�rica * */
    protected Long id = null;

    protected String idAux = "";

    /** Chave Natural Gen�rica */
    protected PlcEntity idNatural = null;

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

    public void setId(java.lang.Long novoId) {
        id = novoId;
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
    public void setIdAux(String novoIdAux) {
        idAux = novoIdAux;
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
        String sufixoPadraoEntidade = "ENTITY";
        try {
            sufixoPadraoEntidade = PlcConfigHelper.getInstance().get(PlcConfigSuffixClass.class).entidade();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        String auxProp = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".") + 1);
        if (this.getClass().getName().endsWith(sufixoPadraoEntidade)) return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1, auxProp.indexOf(sufixoPadraoEntidade)); else return auxProp.substring(0, 1).toLowerCase() + auxProp.substring(1);
    }

    /**
     * Chama dinamicamente porque XDoclet exige o m�todo getIdNatural no
     * descenente
     * @since jCompany 3.0
     */
    public PlcEntity getDynamicNaturalId() {
        PlcEntity idNatural = null;
        if (PropertyUtils.isReadable(this, "idNatural")) {
            try {
                idNatural = (PlcEntity) PropertyUtils.getSimpleProperty(this, "idNatural");
            } catch (Exception e) {
                log.fatal("Erro ao tentar chamar metodo getIdNatural em " + this.getClass().getName());
            }
        }
        return idNatural;
    }

    /**
     * @since jCompany 3.0 
     */
    public void setIdNatural(PlcEntity idNatural) {
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
    public void setIndExcPlc(String novoindExcPlc) {
        indExcPlc = novoindExcPlc;
    }

    /**
     * @deprecated Utilizar equalsPlc est�tico, passando (this,(MinhaClasse)outro,new String[]{"prop1","prop2"});
     */
    protected boolean equalsPlc(Object outro, Object[] props) {
        String[] sAux = null;
        if (props != null) {
            sAux = new String[props.length];
            for (int i = 0; i < props.length; i++) {
                sAux[i] = props[i].toString();
            }
            ;
        }
        return equalsPlc(this, (PlcEntity) outro, sAux);
    }

    /**
     * Processamento gen�rico para c�digo "equals" em VOs. <br>
     * Testa nulidades do outro ENTITY e de valores, retornando false caso somente
     * um dos "lados" esteja nulo. Se ambos estiverem nulos, retorna true. Testa
     * genericamente para os tipo String, Long, Integer, Double, BigDecimal e
     * java.util.Date Todas as propriedades passadas devem estar iguais para que
     * o m�todo retorne true.
     *
     * @param outro
     *            ENTITY a ser comparado com o atual
     * @param props
     *            Object[] com nomes das propriedades.
     * @return true se todas estiverem iguais ou false em caso contr�rio.
     */
    protected static boolean equalsPlc(PlcEntity este, PlcEntity outro, String... props) {
        if (outro == null || !(este.getClass().isAssignableFrom(outro.getClass()) || outro.getClass().isAssignableFrom(este.getClass()))) return false;
        if (este == outro) {
            return true;
        }
        try {
            Collection<String> propSet = props == null || props.length == 0 ? (Collection<String>) getPropsTodasPlc(este) : Arrays.asList(props);
            for (String umaProp : propSet) {
                try {
                    if (!propriedadeIgual(este, outro, umaProp)) return false;
                } catch (Exception e) {
                    log.fatal("jCompany. Erro fatal ao tentar comparar propriedades do vo " + este + " contra " + outro + " para propriedade " + umaProp + ERRO_ORIGINAL + e);
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.fatal("jCompany. Erro fatal ao tentar comparar propriedades do vo " + este + " contra " + outro + ERRO_ORIGINAL + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica se uma propriedade � igual em dois vos diferentes.
     * Considerando que os dois s�o instancias do mesmo objeto
     * @since jCompany 3.0
     */
    protected static boolean propriedadeIgual(PlcEntity este, PlcEntity outro, String prop) throws Exception {
        try {
            if (!PropertyUtils.isReadable(este, prop)) {
                return true;
            }
            if (prop.equals("versao")) return true;
            Object valorDeste = PropertyUtils.getProperty(este, prop);
            Object valorOutro = PropertyUtils.getProperty(outro, prop);
            if (prop.equals("id")) {
                if (este.getClass().getAnnotation(Embeddable.class) != null) return true;
                valorOutro = ((PlcEntity) outro).getId();
            }
            if ((valorDeste == null && valorOutro == null) && prop.equals("id")) {
                return false;
            }
            if (valorDeste == valorOutro) {
                return true;
            }
            if ((valorDeste == null || valorOutro == null) && !Collection.class.isAssignableFrom(PropertyUtils.getPropertyType(este, prop))) {
                return false;
            } else if (Collection.class.isAssignableFrom(PropertyUtils.getPropertyType(este, prop))) {
                return true;
            } else if (valorDeste.getClass().isArray()) {
                if (!valorOutro.getClass().isArray()) {
                    return false;
                }
                Class<?> componentType = valorDeste.getClass().getComponentType();
                if (componentType.isPrimitive()) {
                    if (!valorDeste.getClass().getComponentType().equals(valorOutro.getClass().getComponentType())) {
                        return false;
                    }
                    if (componentType.equals(boolean.class)) {
                        return Arrays.equals((boolean[]) valorDeste, (boolean[]) valorOutro);
                    }
                    if (componentType.equals(byte.class)) {
                        return Arrays.equals((byte[]) valorDeste, (byte[]) valorOutro);
                    }
                    if (componentType.equals(char.class)) {
                        return Arrays.equals((char[]) valorDeste, (char[]) valorOutro);
                    }
                    if (componentType.equals(double.class)) {
                        return Arrays.equals((double[]) valorDeste, (double[]) valorOutro);
                    }
                    if (componentType.equals(float.class)) {
                        return Arrays.equals((float[]) valorDeste, (float[]) valorOutro);
                    }
                    if (componentType.equals(int.class)) {
                        return Arrays.equals((int[]) valorDeste, (int[]) valorOutro);
                    }
                    if (componentType.equals(long.class)) {
                        return Arrays.equals((long[]) valorDeste, (long[]) valorOutro);
                    }
                    if (componentType.equals(short.class)) {
                        return Arrays.equals((short[]) valorDeste, (short[]) valorOutro);
                    }
                }
                return Arrays.equals((Object[]) valorDeste, (Object[]) valorOutro);
            }
            if (PlcEntity.class.isAssignableFrom(valorDeste.getClass()) && PlcEntity.class.isAssignableFrom(valorOutro.getClass())) {
                if (((PlcEntity) valorDeste).getId() != null) {
                    return ((PlcEntity) valorDeste).getIdAux().equals(((PlcEntity) valorOutro).getIdAux());
                } else if (((PlcEntity) valorDeste).getDynamicNaturalId() != null) {
                    return ((PlcEntity) valorDeste).getDynamicNaturalId().equals(((PlcEntity) valorOutro).getDynamicNaturalId());
                } else {
                    return valorDeste.equals(valorOutro);
                }
            }
            return valorDeste.equals(valorOutro);
        } catch (Exception e) {
            log.fatal("jCompany. Erro fatal ao tentar comparar propriedades do vo " + este + " contra " + outro + " para prop=" + prop + ERRO_ORIGINAL + e.toString());
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
     * @param outro Outro ENTITY a comparar
     * @return true se todas as propriedades forem iguais
     */
    public boolean apiTotEquals(Object outro) {
        return equalsPlc(this, (PlcEntity) outro);
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
                            Object valor = PropertyUtils.getSimpleProperty(this, pd.getName());
                            if (valor != null && !(valor instanceof PlcEntity) && !(valor instanceof java.util.List) && !(valor instanceof java.util.ArrayList) && !(valor instanceof java.util.Set) && !(valor instanceof java.util.Map)) {
                                cont++;
                                if (cont > 1) sb.append(",");
                                sb.append(pd.getName() + "=" + valor);
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
                        Object valor = PropertyUtils.getSimpleProperty(this, prop);
                        if (valor != null) {
                            cont++;
                            if (cont > 1) sb.append(",");
                            sb.append(prop + "=" + valor);
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
    public int hashCodeChavePlc() {
        String[] props = getPropsChaveNaturalPlc();
        int result = 17;
        if (props != null) {
            for (int i = 0; i < props.length; i++) {
                try {
                    if (PropertyUtils.isReadable(this, props[i]) && (!Collection.class.isAssignableFrom(PropertyUtils.getPropertyType(this, props[i])))) {
                        Object valor = PropertyUtils.getNestedProperty(this, props[i]);
                        int valorAux = valor == null ? 0 : valor.hashCode();
                        result = result * 37 + valorAux;
                    }
                } catch (Exception e) {
                    log.fatal("Erro ao tentar pegar hashCode de propriedade " + props[i] + " Erro:" + e);
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
            Object valor = null;
            try {
                Set<String> propsTodas = getPropsTodasPlc(this);
                for (String prop : propsTodas) {
                    valor = PropertyUtils.getProperty(this, prop);
                    int valorAux = 0;
                    if (valor != null) {
                        if (valor.getClass().isArray()) {
                            Class<?> componentType = valor.getClass().getComponentType();
                            if (componentType.isPrimitive()) {
                                if (componentType.equals(boolean.class)) {
                                    valorAux = Arrays.hashCode((boolean[]) valor);
                                } else if (componentType.equals(byte.class)) {
                                    valorAux = Arrays.hashCode((byte[]) valor);
                                } else if (componentType.equals(char.class)) {
                                    valorAux = Arrays.hashCode((char[]) valor);
                                } else if (componentType.equals(double.class)) {
                                    valorAux = Arrays.hashCode((double[]) valor);
                                } else if (componentType.equals(float.class)) {
                                    valorAux = Arrays.hashCode((float[]) valor);
                                } else if (componentType.equals(int.class)) {
                                    valorAux = Arrays.hashCode((int[]) valor);
                                } else if (componentType.equals(long.class)) {
                                    valorAux = Arrays.hashCode((long[]) valor);
                                } else if (componentType.equals(short.class)) {
                                    valorAux = Arrays.hashCode((short[]) valor);
                                }
                            } else {
                                valorAux = Arrays.hashCode((Object[]) valor);
                            }
                        } else if (valor instanceof PlcEntity) {
                            valorAux = ((PlcEntity) valor).hashCodeChavePlc();
                        } else if (valor instanceof Collection) {
                        } else {
                            valorAux = valor.hashCode();
                        }
                    }
                    result = result * 37 + valorAux;
                }
            } catch (Exception e) {
                log.fatal("Erro ao tentar pegar hashCode de propriedade " + valor + " Erro:" + e);
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
    public boolean equalsChaveNatural(PlcEntity outro) {
        PlcPrimaryKey chavePrimaria = (PlcPrimaryKey) this.getClass().getAnnotation(PlcPrimaryKey.class);
        if (chavePrimaria != null) {
            PlcEntity thisIdNatural = this.getDynamicNaturalId();
            PlcEntity outroIdNatural = outro.getDynamicNaturalId();
            return equalsPlc(thisIdNatural, outroIdNatural, getPropsChaveNaturalPlc());
        } else return equalsPlc(this, outro, getPropsChaveNaturalPlc());
    }

    /**
     * @since jCompany 3.0
     * C�digo padronizado para equals, baseado em todos os valores do ENTITY. 
     * Recomenda-se especializar fazendo teste espec�fico ou passando propriedades suficientes
     * como array de String
     */
    public boolean equals(Object outro) {
        boolean igual = outro instanceof PlcEntity && equalsPlc(this, (PlcEntity) outro);
        return igual;
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
        return toStringPlc(getPropsChaveNaturalPlc());
    }

    /**
     * @since jCompany 2.5.3.
     * Link de edi��o gen�rico
     */
    public String getLinkEdicaoPlc() {
        StringBuilder link = new StringBuilder();
        if (getId() != null) {
            link.append("&chPlc=").append(getId());
        } else if (getIdAux() != null && !getIdAux().trim().equals("")) {
            try {
                link.append("&chPlc=").append(URLEncoder.encode(getIdAux(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                link.append("&chPlc=").append(getIdAux());
                log.fatal("jCompany. Erro fatal ao tentar montar link de edicao do ENTITY " + this + " para propriedade " + " idAux" + ERRO_ORIGINAL + e.toString());
                e.printStackTrace();
            }
        } else {
            PlcEntity idNatural = this.getDynamicNaturalId();
            if (idNatural != null) {
                link.append("&evento=y");
                for (String propriedade : getPropsChaveNaturalPlc()) {
                    try {
                        link.append("&").append(propriedade).append("=");
                        Class<?> propertyType = PropertyUtils.getPropertyDescriptor(idNatural, propriedade).getPropertyType();
                        if (propertyType != null && propertyType.equals(java.util.Date.class)) {
                            Timestamp valorField = (Timestamp) PropertyUtils.getProperty(idNatural, propriedade);
                            if (valorField != null) {
                                link.append(valorField.getTime());
                            }
                        } else if (propertyType != null && PlcEntity.class.isAssignableFrom(propertyType)) {
                            PlcEntity b = (PlcEntity) PropertyUtils.getProperty(idNatural, propriedade);
                            link.append(b.getIdAux());
                        } else link.append(URLEncoder.encode(PropertyUtils.getProperty(idNatural, propriedade).toString(), "ISO-8859-1"));
                    } catch (Exception e) {
                        log.fatal("jCompany. Erro fatal ao tentar montar link de edicao do ENTITY " + this + " para propriedade " + propriedade + ERRO_ORIGINAL + e.toString());
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
    public String getLinkEdicaoAvancadoPlc() {
        StringBuffer link = new StringBuffer("");
        if (this.getClass().getAnnotation(PlcEntityMetadata.class) != null) {
            String props = ((PlcEntityMetadata) this.getClass().getAnnotation(PlcEntityMetadata.class)).navegacao();
            StringTokenizer st = new StringTokenizer(props, ",");
            while (st.hasMoreElements()) {
                String prop = (String) st.nextElement();
                try {
                    if (PropertyUtils.getPropertyDescriptor(this, prop).getPropertyType().equals(java.util.Date.class)) link.append("&" + prop + "=" + PropertyUtils.getProperty(this, prop + "Aux")); else if (PlcEntity.class.isAssignableFrom(PropertyUtils.getPropertyDescriptor(this, prop).getPropertyType())) {
                        PlcEntity b = (PlcEntity) PropertyUtils.getProperty(this, prop);
                        link.append("&" + prop + "=" + b.getIdAux());
                    } else if (prop.equals("id")) link.append("&" + prop + "=" + getIdAux() + "&chPlc=" + getIdAux()); else link.append("&" + prop + "=" + PropertyUtils.getProperty(this, prop));
                } catch (Exception e) {
                    log.fatal("jCompany. Erro fatal ao tentar montar link de edicao avancado (com navegacao) do ENTITY " + this + " para propriedade " + prop + ERRO_ORIGINAL + e.toString());
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
    public boolean isIdentificado() {
        boolean eIdentificado = true;
        Object[] props = getPropsChaveNaturalPlc();
        if (props == null) {
            if (getId() == null) eIdentificado = false;
        } else {
            for (int i = 0; i < props.length; i++) {
                String prop = (String) props[i];
                try {
                    Object valor = null;
                    if (prop.equals("id")) valor = getId(); else valor = PropertyUtils.getSimpleProperty(PropertyUtils.getSimpleProperty(this, "idNatural"), prop);
                    if (valor == null) {
                        eIdentificado = false;
                    }
                } catch (Exception e) {
                    log.fatal("Erro ao tentar pegar valor de propriedade " + prop + " Erro:" + e);
                    e.printStackTrace();
                }
            }
        }
        return eIdentificado;
    }

    public void setIndExcPlc(Boolean value) {
        if (value != null && value.equals(true)) this.setIndExcPlc("S"); else this.setIndExcPlc("N");
    }

    @Transient
    private transient PlcFileEntity arquivoAnexado;

    public PlcFileEntity getAttachedFile() {
        return arquivoAnexado;
    }

    public void setArquivoAnexado(PlcFileEntity arquivoAnexado) {
        this.arquivoAnexado = arquivoAnexado;
    }
}
