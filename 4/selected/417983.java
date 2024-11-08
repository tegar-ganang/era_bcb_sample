package br.nic.connector.dao;

import java.util.Date;
import java.util.List;
import org.hibernate.Session;
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
 * DAO responsible for obtaining and writing data related to the hosts.
 * @author Pedro Hadek
 */
public class HostsDAO extends GenericDAO {

    /**
	 * Initializes the DAO accordingly to the GenericDAO's initialize.
	 */
    public HostsDAO(Session session, boolean encrypt) {
        initializer(session, encrypt);
    }

    /**
	 * Removes all accessibility data from the Hosts database. Must be used before their
	 * re-consolidation.
	 */
    public void clearAcessibilityData() {
        session.createSQLQuery("UPDATE " + Sitios.NAME_table + " SET " + Sitios.NAME_ases + " = NULL").executeUpdate();
    }

    /**
	 * Returns the complete list of hosts on the database.
	 */
    @SuppressWarnings("unchecked")
    public List<Sitios> getCompleteHostsList() {
        return session.createCriteria(Sitios.class).list();
    }

    /**
	 * Special "Tested" getter. Will obtain all domains yet untested concerning the download
	 * of their homepages.
	 */
    @SuppressWarnings("unchecked")
    public List<String> getDownHomeTestedHosts() {
        return session.createCriteria(Paginas.class).add(Restrictions.isNotNull(Paginas.NAME_data_w3c)).add(Restrictions.isNotNull(Paginas.NAME_data_ases)).setProjection(Projections.property(Paginas.NAME_sitio)).list();
    }

    /**
	 * Obtains a host from it's wireID. Is is expected to be unique. Otherwise, an Exception
	 * will occur.
	 */
    public Sitios getHostFromHostWireId(Long hostWireId) {
        return (Sitios) session.createCriteria(Sitios.class).add(Restrictions.eq(Sitios.NAME_wireId, hostWireId)).setCacheable(true).uniqueResult();
    }

    /**
	 * Obtains a host from it's URL.
	 */
    public Sitios getHostFromURL(String host) {
        if (encrypt) host = Utils.getHash(host);
        return (Sitios) session.createCriteria(Sitios.class).add(Restrictions.eq(Sitios.NAME_sitio, host)).setCacheable(true).uniqueResult();
    }

    /**
	 * Obtains the name of a host from it's wireID. Is is expected to be unique.
	 * Otherwise, an Exception will occur.
	 */
    public String getHostNameFromHostId(Long hostId) {
        return (String) session.createCriteria(Sitios.class).add(Restrictions.eq(Sitios.NAME_id, hostId)).setProjection(Projections.property(Sitios.NAME_sitio)).uniqueResult();
    }

    /**
	 * Method used to search in the database a list with all the hosts, and their respective IP
	 * addresses.
	 * @return
	 * A List<Object[]>, in which for each Object[] the first element is a string corresponing
	 * to the Site's name, and the second the IPv4 of the Site's server.
	 */
    @SuppressWarnings("unchecked")
    public List<Object[]> getHostsNamesIPsList() {
        return session.createCriteria(Sitios.class).setProjection(Projections.projectionList().add(Projections.property(Sitios.NAME_sitio)).add(Projections.property(Sitios.NAME_ipv4))).list();
    }

    /**
	 * Returns a list with all the hostNames on the database.
	 */
    @SuppressWarnings("unchecked")
    public List<String> getHostsNamesList() {
        List<String> list = (List<String>) session.createCriteria(Sitios.class).setProjection(Projections.property(Sitios.NAME_sitio)).list();
        if (list == null) System.out.println("a query ao banco está retornando uma lista vazia"); else System.out.println("a lista completa possui: " + list.size() + " elementos");
        return list;
    }

    /**
	 * Obtains all the names of the hosts already tested for a given testParameter.
	 * @param testParameter
	 * Parameter for which the host must have been tested for. Will vary according to each test,
	 * and thus must be set individually.
	 * @return
	 * List of tested Hosts.
	 */
    @SuppressWarnings("unchecked")
    public List<String> getTestedHosts(String testParameter) {
        return session.createCriteria(Sitios.class).add(Restrictions.isNotNull(testParameter)).setProjection(Projections.property(Sitios.NAME_sitio)).list();
    }

    /**
	 * Obtains all the names of the hosts yet untested for a given testParameter.
	 * @param testParameter
	 * Parameter for which the host must not have been tested for. Will vary according to each test,
	 * and thus must be set individually.
	 * @return 
	 * List of untested Hosts.
	 */
    @SuppressWarnings("unchecked")
    public List<String> getUntestedHosts(String testParameter) {
        return session.createCriteria(Sitios.class).add(Restrictions.isNull(testParameter)).setProjection(Projections.property(Sitios.NAME_sitio)).list();
    }

    /**
	 * Obtains all the names and IPv4 addresses of the hosts already tested for a given testParameter.
	 * @param testParameter
	 * Parameter for which the host must have been tested for. Will vary according to each test,
	 * and thus must be set individually.
	 * @return
	 * A List<Object[]> of the tested hosts, in which for each Object[] the first element is a
	 * string corresponding to the Site's name, and the second the IPv4 of the Site's server.
	 */
    @SuppressWarnings("unchecked")
    public List<Object[]> getTestedHostsNamesIPsList(String testParameter) {
        return session.createCriteria(Sitios.class).add(Restrictions.isNotNull(testParameter)).setProjection(Projections.projectionList().add(Projections.property(Sitios.NAME_sitio)).add(Projections.property(Sitios.NAME_ipv4))).list();
    }

    /**
	 * Obtains all the names and IPv4 addresses of the hosts yet untested for a given testParameter.
	 * @param testParameter
	 * Parameter for which the host must not have been tested for. Will vary according to each test,
	 * and thus must be set individually.
	 * @return
	 * A List<Object[]> of the untested hosts, in which for each Object[] the first element is a
	 * string corresponding to the Site's name, and the second the IPv4 of the Site's server.
	 */
    @SuppressWarnings("unchecked")
    public List<Object[]> getUntestedHostsNamesIPsList(String testParameter) {
        return session.createCriteria(Sitios.class).add(Restrictions.isNull(testParameter)).setProjection(Projections.projectionList().add(Projections.property(Sitios.NAME_sitio)).add(Projections.property(Sitios.NAME_ipv4))).list();
    }

    /**
	 * Indicates if the Database contains data about a given host. True if it contains any.
	 */
    public boolean hasHost(String host) {
        return getHostFromURL(host) != null;
    }

    /**
	 * Verifies if this table already contains a wire_id. True if it contains any.
	 */
    public boolean hasWireIDs() {
        return !session.createCriteria(Sitios.class).add(Restrictions.isNotNull(Sitios.NAME_wireId)).list().isEmpty();
    }

    /**
	 * Verifies if the dataBase is empty. True fi empty.
	 */
    public boolean isEmpty() {
        return session.createCriteria(Sitios.class).setMaxResults(1).list().isEmpty();
    }

    /**
	 * Generates appropriate error messages for each exception, and deals with possible errors that may
	 * cascade from the original exception.
	 */
    private void treatException(Exception e, String currTestElement) {
        try {
            if (session == null || !session.isOpen() || !session.isConnected()) {
                if (session != null) session.close();
                session = new HibernateFactory().getSession();
            }
            SimpleLog.getInstance().writeLog(3, "Exceção ocorrida em HostsDAO;\n" + "Ocorrida durante análise de " + currTestElement);
            if (e instanceof ConstraintViolationException) {
                SimpleLog.getInstance().writeLog(3, "Constraint Exception devido a " + ((ConstraintViolationException) e).getConstraintName());
            }
            SimpleLog.getInstance().writeException(e, 3);
        } catch (Exception ex) {
            System.err.println("\nExceção no tratamento de exceção!\n");
            ex.printStackTrace();
        }
    }

    /**
	 * Write a new "Sitios" record to the database or, if a Sitios with the same unique identifier
	 * "sitio" is already there, alters the existing record with the data from the Sitios passed here
	 * as a parameter.
	 * @param newSitio
	 * Sitios containing the to be written.
	 */
    public void writeResults(Sitios newSitio) {
        if (newSitio != null && newSitio.getSitio() != null) {
            String currHost = newSitio.getSitio();
            int trys = 0;
            boolean done = false;
            while (!done && trys < Constants.DEFAULT_MAXWRITETRYS) {
                try {
                    sessRestarter();
                    if (encrypt) {
                        currHost = Utils.getHash(currHost);
                        newSitio.setSitio(currHost);
                    }
                    Sitios oldSitio = (Sitios) session.createCriteria(Sitios.class).add(Restrictions.eq(Sitios.NAME_sitio, currHost)).uniqueResult();
                    if (oldSitio == null) {
                        oldSitio = new Sitios();
                        oldSitio.setSitio(currHost);
                        oldSitio.copySitios(newSitio);
                        session.save(oldSitio);
                    } else {
                        oldSitio.copySitios(newSitio);
                        session.flush();
                    }
                    done = true;
                } catch (Exception e) {
                    treatException(e, currHost);
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
                trys++;
            }
        }
    }

    /**
	 * Using the generic writeResults, writes the results of the accesibility level consolidation
	 * of a host. Will only do something if currHost is not null.
	 * @param currHost
	 * Name of the host for which the test was done.
	 * @param asesLevel
	 * Acessibility level of the Host, according to eMAG 2.0
	 */
    public void writeAccessibilidadeResults(String currHost, String asesLevel) {
        if (currHost != null) {
            Sitios host = new Sitios();
            host.setSitio(currHost);
            host.setNivel_acessibilidade(asesLevel);
            writeResults(host);
        }
    }

    /**
	 * Using the generic writeResults, writes the data obtained from WIRE's analysis that were
	 * exported as a CSV. Will only do something if currHost is not null.
	 * @param currHost
	 * Name of the host for which the test was done.
	 * @param hostDados
	 * Object of the Sitios type containing the info to be analyzed.
	 */
    public void writeCSVResult(String currHost, Sitios hostDados) {
        if (currHost != null) {
            Sitios host = new Sitios();
            host.setSitio(currHost);
            host.setData_CSV(new Date(System.currentTimeMillis()));
            host.setAge_average_page(hostDados.getAge_average_page());
            host.setAge_oldest_page(hostDados.getAge_oldest_page());
            host.setAge_newest_page(hostDados.getAge_newest_page());
            host.setCount_doc(hostDados.getCount_doc());
            host.setCount_doc_ok(hostDados.getCount_doc_ok());
            host.setCount_doc_gathered(hostDados.getCount_doc_gathered());
            host.setCount_doc_new(hostDados.getCount_doc_new());
            host.setCount_doc_static(hostDados.getCount_doc_static());
            host.setCount_doc_dynamic(hostDados.getCount_doc_dynamic());
            host.setRaw_content_length(hostDados.getRaw_content_length());
            host.setHas_valid_robots_txt(hostDados.isHas_valid_robots_txt());
            host.setSiterank(hostDados.getSiterank());
            host.setIn_degree(hostDados.getIn_degree());
            host.setOut_degree(hostDados.getOut_degree());
            host.setSum_pagerank(hostDados.getSum_pagerank());
            host.setSum_hubrank(hostDados.getSum_hubrank());
            host.setSum_authrank(hostDados.getSum_authrank());
            host.setInternal_links(hostDados.getInternal_links());
            host.setMax_depth(hostDados.getMax_depth());
            host.setComponent(hostDados.getComponent());
            host.setWire_id(hostDados.getWire_id());
            writeResults(host);
        }
    }

    /**
	 * Using the generic writeResults, writes the test results data for a host domain's. Will
	 * only do something if currHost is not null.
	 * @param currHost
	 * Name of the host for which the test was done.
	 * @param subdominio
	 * Domain identified for the host (Example: example.com.br)
	 * @param dominio
	 * TLD + ccTLD identified for the host.
	 */
    public void writeDomainResult(String currHost, String subdominio, String dominio) {
        if (currHost != null) {
            Sitios host = new Sitios();
            host.setSitio(currHost);
            host.setDominio(dominio);
            host.setSubdominio(subdominio);
            writeResults(host);
        }
    }

    /**
	 * Using the generic writeResults, writes the data for the IPv6 test. Will
	 * only do something if currHost is not null.
	 * @param currHost
	 * Name of the host for which the test was done.
	 * @param hostIPv6
	 * Sitios entry with aceita, IPv6 and DataIPv6 set.
	 */
    public void writeIpv6Result(String currHost, Sitios hostIPv6) {
        if (currHost != null) {
            Sitios host = new Sitios();
            host.setSitio(currHost);
            host.setAceita(hostIPv6.getAceita());
            host.setIpv6(hostIPv6.getIpv6());
            host.setDataIPv6(new Date(System.currentTimeMillis()));
            writeResults(host);
        }
    }

    /**
	 * Using the generic writeResults, writes the data for the server response test. Will
	 * only do something if currHost is not null.
	 * @param currHost
	 * Name of the host for which the test was done.
	 * @param result
	 * Sitios entry with RespServer, RespDifTempo, RespDelay, IPv4 and DataResp set.
	 */
    public void writeResponseResult(String currHost, Sitios result) {
        if (currHost != null) {
            Sitios host = new Sitios();
            host.setSitio(currHost);
            host.setRespDifTempo(result.getRespDifTempo());
            host.setRespDelay(result.getRespDelay());
            String respServer = result.getRespServer();
            if (respServer.length() > Constants.RESULT_MAXSERVERLENGTH) {
                respServer = respServer.substring(0, Constants.RESULT_MAXSERVERLENGTH);
            }
            host.setRespServer(respServer);
            host.setDataResp(new Date(System.currentTimeMillis()));
            String ipv4 = result.getIpv4();
            if (ipv4.length() > Constants.RESULT_MAXIPV4LENGTH) {
                SimpleLog.getInstance().writeLog(3, "IPv4 maior que 25 caracters: " + ipv4);
                ipv4 = ipv4.substring(0, Constants.RESULT_MAXIPV4LENGTH);
            }
            host.setIpv4(ipv4);
            writeResults(host);
        }
    }

    /**
	 * Using the generic writeResults, writes the data for the server geolocalization test. Will
	 * only do something if currHost is not null.
	 * @param currHost
	 * Name of the host for which the test was done.
	 * @param geolocation
	 * Value referring to the local identified for the Host.
	 */
    public void writeServerGeolocationResult(String currHost, String geolocation) {
        if (currHost != null) {
            Sitios host = new Sitios();
            host.setSitio(currHost);
            host.setGeolocal(geolocation);
            host.setDataGeolocal(new Date(System.currentTimeMillis()));
            writeResults(host);
        }
    }

    /**
	 * Using the generic writeResults, writes the data for the server NTP response test. Will
	 * only do something if currHost is not null.
	 * @param currHost
	 * Name of the host for which the test was done.
	 * @param ntpValues
	 * Results of the NTP test, in order, NTP stratum, NTP offset and NTP delay.
	 */
    public void writeServerNtpResult(String currHost, Object[] ntpValues) {
        if (currHost != null) {
            if (ntpValues == null) ntpValues = new Object[3];
            if (ntpValues[0] == null) ntpValues[0] = 16;
            if (ntpValues[1] == null) ntpValues[1] = -1f; else if ((Float) ntpValues[1] < 0) ntpValues[1] = -(Float) ntpValues[1];
            if (ntpValues[2] == null) ntpValues[2] = -1f; else if ((Float) ntpValues[2] < 0) ntpValues[2] = -(Float) ntpValues[2];
            Sitios host = new Sitios();
            host.setSitio(currHost);
            host.setNtp_stratum((Integer) ntpValues[0]);
            host.setNtp_offset((Float) ntpValues[1]);
            host.setNtp_delay((Float) ntpValues[2]);
            host.setData_ntp(new Date(System.currentTimeMillis()));
            writeResults(host);
        }
    }
}
