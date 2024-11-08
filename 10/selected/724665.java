package org.freedom.modulos.crm.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.Vector;
import org.freedom.infra.dao.AbstractDAO;
import org.freedom.infra.model.jdbc.DbConnection;
import org.freedom.library.functions.Funcoes;
import org.freedom.library.swing.frame.Aplicativo;
import org.freedom.modulos.crm.business.component.EstagioCheck;
import org.freedom.modulos.crm.business.object.Atendimento;
import org.freedom.modulos.crm.business.object.Atendimento.EColAtend;
import org.freedom.modulos.crm.business.object.Atendimento.EColExped;
import org.freedom.modulos.crm.business.object.Atendimento.INIFINTURNO;
import org.freedom.modulos.crm.business.object.Atendimento.PARAM_PRIM_LANCA;
import org.freedom.modulos.crm.business.object.Atendimento.PREFS;
import org.freedom.modulos.crm.business.object.Atendimento.PROC_IU;
import org.freedom.modulos.gpe.business.object.Batida;

public class DAOAtendimento extends AbstractDAO {

    private Object prefs[] = null;

    private enum COLBAT {

        INITURNO, INIINTTURNO, FININTTURNO, FINTURNO
    }

    ;

    private enum COLBATLANCTO {

        BATIDA, LANCTO, DIF, POS
    }

    ;

    public DAOAtendimento(DbConnection cn) {
        super(cn);
    }

    private Integer getSequencia(Integer codemp, Integer codfilial, String tab) throws SQLException {
        Integer result = null;
        StringBuilder sql = new StringBuilder("select iseq from spgeranum( ?, ?, ? )");
        PreparedStatement ps = getConn().prepareStatement(sql.toString());
        ps.setInt(1, codemp);
        ps.setInt(2, codfilial);
        ps.setString(3, tab);
        ResultSet rs = ps.executeQuery();
        return result;
    }

    private void updateHoraatendo(Integer codemp, Integer codfilial, Integer codatendo, String horaatendo, String horaatendofin) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("update atatendimento set horaatendo=?, horaatendofin=? ");
        sql.append("where codemp=? and codfilial=? and codatendo=?");
        PreparedStatement ps = getConn().prepareStatement(sql.toString());
        ps.setTime(1, Funcoes.strTimeToSqlTime(horaatendo, false));
        ps.setTime(2, Funcoes.strTimeToSqlTime(horaatendofin, false));
        ps.setInt(3, codemp);
        ps.setInt(4, codfilial);
        ps.setInt(5, codatendo);
        ps.executeUpdate();
        ps.close();
        try {
            getConn().commit();
        } catch (SQLException e) {
            getConn().rollback();
        }
    }

    public Atendimento loadAtendo(Integer codemp, Integer codfilial, Integer codatendo) throws SQLException {
        Atendimento result = null;
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("atd.codempto, atd.codfilialto, atd.codtpatendo, ");
        sql.append("atd.codempsa, atd.codfilialsa, atd.codsetat, ");
        sql.append("atd.obsatendo, atd.obsinterno, atd.statusatendo, ");
        sql.append("atd.codempcl, atd.codfilialcl, atd.codcli, atd.codempct, ");
        sql.append("atd.codfilialct, atd.codcontr, atd.coditcontr, ");
        sql.append("atd.codempca, atd.codfilialca, atd.codclasatendo,");
        sql.append("atd.codempch, atd.codfilialch, atd.codchamado, ");
        sql.append("atd.codempea, atd.codfilialea, atd.codespec, ");
        sql.append("atd.codempta, atd.codfilialta, atd.codtarefa ");
        sql.append("from atatendimento atd ");
        sql.append("where ");
        sql.append("atd.codemp=? and atd.codfilial=? and atd.codatendo=? ");
        if (codatendo != null) {
            PreparedStatement ps = getConn().prepareStatement(sql.toString());
            ps.setInt(1, codemp);
            ps.setInt(2, codfilial);
            ps.setInt(3, codatendo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = new Atendimento();
                result.setCodemp(codemp);
                result.setCodfilial(codfilial);
                result.setCodempto(rs.getInt("codempto"));
                result.setCodfilialto(rs.getInt("codfilialto"));
                result.setCodtpatendo(rs.getInt("codtpatendo"));
                result.setCodempsa(rs.getInt("codempsa"));
                result.setCodfilialsa(rs.getInt("codfilialsa"));
                result.setCodsetat(rs.getInt("codsetat"));
                result.setObsatendo(rs.getString("obsatendo"));
                result.setObsinterno(rs.getString("obsinterno"));
                result.setStatusatendo(rs.getString("statusatendo"));
                result.setCodempcl(rs.getInt("codempcl"));
                result.setCodfilialcl(rs.getInt("codfilialcl"));
                result.setCodcli(rs.getInt("codcli"));
                if (rs.getString("coditcontr") != null) {
                    result.setCodempct(rs.getInt("codempct"));
                    result.setCodfilialct(rs.getInt("codfilialct"));
                    result.setCodcontr(rs.getInt("codcontr"));
                    result.setCoditcontr(rs.getInt("coditcontr"));
                }
                result.setCodempca(rs.getInt("codempca"));
                result.setCodfilialca(rs.getInt("codfilialca"));
                result.setCodclasatendo(rs.getInt("codclasatendo"));
                if (rs.getString("codchamado") != null) {
                    result.setCodempch(rs.getInt("codempch"));
                    result.setCodfilialch(rs.getInt("codfilialch"));
                    result.setCodchamado(rs.getInt("codchamado"));
                }
                result.setCodempea(rs.getInt("codempea"));
                result.setCodfilialea(rs.getInt("codfilialea"));
                result.setCodespec(rs.getInt("codespec"));
                if (rs.getString("codtarefa") != null) {
                    result.setCodempta(rs.getInt("codempta"));
                    result.setCodfilialta(rs.getInt("codfilialta"));
                    result.setCodtarefa(rs.getInt("codtarefa"));
                }
                result.setDocatendo("0");
                result.setConcluichamado("N");
            }
        }
        return result;
    }

    public Atendimento loadModelAtend(Integer codemp, Integer codfilial, Integer codempmo, Integer codfilialmo, Integer codmodel) throws SQLException {
        Atendimento result = null;
        Integer codatendo = null;
        StringBuilder sql = new StringBuilder("select ");
        sql.append("mod.codempto, mod.codfilialto, mod.codtpatendo, ");
        sql.append("mod.codempsa, mod.codfilialsa, mod.codsetat, ");
        sql.append("mod.obsatendo, mod.obsinterno, mod.statusatendo, ");
        sql.append("mod.codempcl, mod.codfilialcl, mod.codcli, mod.codempct, ");
        sql.append("mod.codfilialct, mod.codcontr, mod.coditcontr, ");
        sql.append("mod.codempca, mod.codfilialca, mod.codclasatendo,");
        sql.append("mod.codempch, mod.codfilialch, mod.codchamado, ");
        sql.append("mod.codempea, mod.codfilialea, mod.codespec ");
        sql.append("from atmodatendo mod ");
        sql.append("where ");
        sql.append("mod.codemp=? and mod.codfilial=? and mod.codmodel=? ");
        if (codmodel != null) {
            codatendo = getSequencia(codemp, codfilial, "AT");
            PreparedStatement ps = getConn().prepareStatement(sql.toString());
            ps.setInt(1, codempmo);
            ps.setInt(2, codfilialmo);
            ps.setInt(3, codmodel);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = new Atendimento();
                result.setCodemp(codemp);
                result.setCodfilial(codfilial);
                result.setCodempto(rs.getInt("codempto"));
                result.setCodfilialto(rs.getInt("codfilialto"));
                result.setCodtpatendo(rs.getInt("codtpatendo"));
                result.setCodempsa(rs.getInt("codempsa"));
                result.setCodfilialsa(rs.getInt("codfilialsa"));
                result.setCodsetat(rs.getInt("codsetat"));
                result.setObsatendo(rs.getString("obsatendo"));
                result.setObsinterno(rs.getString("obsinterno"));
                result.setStatusatendo(rs.getString("statusatendo"));
                result.setCodempcl(rs.getInt("codempcl"));
                result.setCodfilialcl(rs.getInt("codfilialcl"));
                result.setCodcli(rs.getInt("codcli"));
                if (rs.getString("coditcontr") != null) {
                    result.setCodempct(rs.getInt("codempct"));
                    result.setCodfilialct(rs.getInt("codfilialct"));
                    result.setCodcontr(rs.getInt("codcontr"));
                    result.setCoditcontr(rs.getInt("coditcontr"));
                }
                result.setCodempca(rs.getInt("codempca"));
                result.setCodfilialca(rs.getInt("codfilialca"));
                result.setCodclasatendo(rs.getInt("codclasatendo"));
                if (rs.getString("codchamado") != null) {
                    result.setCodempch(rs.getInt("codempch"));
                    result.setCodfilialch(rs.getInt("codfilialch"));
                    result.setCodchamado(rs.getInt("codchamado"));
                }
                result.setCodempea(rs.getInt("codempea"));
                result.setCodfilialea(rs.getInt("codfilialea"));
                result.setCodespec(rs.getInt("codespec"));
                result.setDocatendo("0");
                result.setConcluichamado("N");
            }
        }
        return result;
    }

    public int getTotInconsistencia(Vector<Vector<Object>> vexped, Vector<Vector<Object>> vatend) {
        int result = 0;
        String sit = null;
        for (Vector<Object> row : vexped) {
            sit = (String) row.elementAt(EColExped.SITREVEXPED.ordinal());
            if ((sit != null) && (sit.charAt(1) == 'I')) {
                result++;
            }
        }
        for (Vector<Object> row : vatend) {
            sit = (String) row.elementAt(EColAtend.SITREVATENDO.ordinal());
            if ((sit != null) && (sit.charAt(1) == 'I')) {
                result++;
            }
        }
        return result;
    }

    public void gerarEstagio345(Vector<Vector<Object>> vatend, Integer codemp, Integer codfilial, Integer codempmo, Integer codfilialmo, Integer codempae, Integer codfilialae, Integer codatend, Integer codempus, Integer codfilialus, String idusu) throws SQLException {
        Atendimento atendimento = null;
        Object codmodel = null;
        Integer codatendo = null;
        String strcodmodel = null;
        Date dataatendo = null;
        Date dataatendofin = null;
        String horaini = null;
        String horafin = null;
        String sitrev = null;
        for (Vector<Object> row : vatend) {
            sitrev = (String) row.elementAt(EColAtend.SITREVATENDO.ordinal());
            if ((sitrev.equals(EstagioCheck.E3I.getValueTab())) || (sitrev.equals(EstagioCheck.E4I.getValueTab())) || (sitrev.equals(EstagioCheck.E5I.getValueTab()))) {
                codmodel = row.elementAt(EColAtend.CODMODEL.ordinal());
                if ("".equals(codmodel)) {
                    codmodel = null;
                }
                horaini = getTimeString(row.elementAt(EColAtend.HORAINI.ordinal()));
                horafin = getTimeString(row.elementAt(EColAtend.HORAFIN.ordinal()));
                if ((codmodel == null) && (sitrev.equals(EstagioCheck.E3I.getValueTab()))) {
                    codatendo = (Integer) row.elementAt(EColAtend.CODATENDO.ordinal());
                    updateHoraatendo(codemp, codfilial, codatendo, horaini, horafin);
                } else {
                    dataatendo = Funcoes.strDateToDate((String) row.elementAt(EColAtend.DATAATENDO.ordinal()));
                    dataatendofin = Funcoes.strDateToDate((String) row.elementAt(EColAtend.DATAATENDO.ordinal()));
                    atendimento = loadModelAtend(codemp, codfilial, codempmo, codfilialmo, (Integer) codmodel);
                    atendimento.setDataatendo(dataatendo);
                    atendimento.setDataatendofin(dataatendofin);
                    atendimento.setHoraatendo(horaini);
                    atendimento.setHoraatendofin(horafin);
                    atendimento.setCodempae(codempae);
                    atendimento.setCodfilialae(codfilialae);
                    atendimento.setCodatend(codatend);
                    atendimento.setCodempus(codempus);
                    atendimento.setCodfilialus(codfilialus);
                    atendimento.setIdusu(idusu);
                    insert(atendimento);
                }
            }
        }
    }

    public void insertIntervaloAtend(Integer codemp, Integer codfilial, Date dataatendo, Date dataatendofin, String horaini, String horafim, Integer codempae, Integer codfilialae, Integer codatend, Integer codempus, Integer codfilialus, String idusu) throws SQLException {
        Atendimento intervalo = loadModelAtend(codemp, codfilial, (Integer) prefs[PREFS.CODEMPMI.ordinal()], (Integer) prefs[PREFS.CODFILIALMI.ordinal()], (Integer) prefs[PREFS.CODMODELMI.ordinal()]);
        intervalo.setDataatendo(dataatendo);
        intervalo.setDataatendofin(dataatendofin);
        intervalo.setHoraatendo(horaini);
        intervalo.setHoraatendofin(horafim);
        intervalo.setCodempae(codempae);
        intervalo.setCodfilialae(codfilialae);
        intervalo.setCodatend(codatend);
        intervalo.setCodempus(codempus);
        intervalo.setCodfilialus(codfilialus);
        intervalo.setIdusu(idusu);
        insert(intervalo);
    }

    public void insertIntervaloChegada(Integer codemp, Integer codfilial, Date dataatendo, Date dataatendofin, String horaini, String horafim, Integer codempae, Integer codfilialae, Integer codatend, Integer codempus, Integer codfilialus, String idusu) throws SQLException {
        Atendimento intervalo = loadModelAtend(codemp, codfilial, (Integer) prefs[PREFS.CODEMPME.ordinal()], (Integer) prefs[PREFS.CODFILIALME.ordinal()], (Integer) prefs[PREFS.CODMODELME.ordinal()]);
        intervalo.setDataatendo(dataatendo);
        intervalo.setDataatendofin(dataatendofin);
        intervalo.setHoraatendo(horaini);
        intervalo.setHoraatendofin(horafim);
        intervalo.setCodempae(codempae);
        intervalo.setCodfilialae(codfilialae);
        intervalo.setCodatend(codatend);
        intervalo.setCodempus(codempus);
        intervalo.setCodfilialus(codfilialus);
        intervalo.setIdusu(idusu);
        insert(intervalo);
    }

    public void insertFaltaJustificada(Integer codemp, Integer codfilial, Date dataatendo, Date dataatendofin, String horaini, String horafim, Integer codempae, Integer codfilialae, Integer codatend, Integer codempus, Integer codfilialus, String idusu) throws SQLException {
        Atendimento falta = loadModelAtend(codemp, codfilial, (Integer) prefs[PREFS.CODEMPFJ.ordinal()], (Integer) prefs[PREFS.CODFILIALFJ.ordinal()], (Integer) prefs[PREFS.CODMODELFJ.ordinal()]);
        falta.setCodemp(codemp);
        falta.setCodfilial(codfilial);
        falta.setDataatendo(dataatendo);
        falta.setDataatendofin(dataatendofin);
        falta.setHoraatendo(horaini);
        falta.setHoraatendofin(horafim);
        falta.setCodempae(codempae);
        falta.setCodfilialae(codfilialae);
        falta.setCodatend(codatend);
        falta.setCodempus(codempus);
        falta.setCodfilialus(codfilialus);
        falta.setIdusu(idusu);
        insert(falta);
    }

    public void insertFaltaInjustificada(Integer codemp, Integer codfilial, Date dataatendo, Date dataatendofin, String horaini, String horafim, Integer codempae, Integer codfilialae, Integer codatend, Integer codempus, Integer codfilialus, String idusu) throws SQLException {
        Atendimento intervalo = loadModelAtend(codemp, codfilial, (Integer) prefs[PREFS.CODEMPFI.ordinal()], (Integer) prefs[PREFS.CODFILIALFI.ordinal()], (Integer) prefs[PREFS.CODMODELFI.ordinal()]);
        intervalo.setCodemp(codemp);
        intervalo.setCodfilial(codfilial);
        intervalo.setDataatendo(dataatendo);
        intervalo.setDataatendofin(dataatendofin);
        intervalo.setHoraatendo(horaini);
        intervalo.setHoraatendofin(horafim);
        intervalo.setCodempae(codempae);
        intervalo.setCodfilialae(codfilialae);
        intervalo.setCodatend(codatend);
        intervalo.setCodempus(codempus);
        intervalo.setCodfilialus(codfilialus);
        intervalo.setIdusu(idusu);
        insert(intervalo);
    }

    public int getAtendente(Integer Matempr) {
        StringBuffer sql = new StringBuffer();
        StringBuffer where = new StringBuffer();
        int iRet = 0;
        try {
            sql.append("select codatend from ATATENDENTE  ");
            sql.append(" where  matempr = " + Matempr);
            PreparedStatement ps = getConn().prepareStatement(sql.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                iRet = rs.getInt("Codatend");
                return iRet;
            }
            rs.close();
            ps.close();
            getConn().commit();
        } catch (SQLException err) {
            err.printStackTrace();
        }
        return iRet;
    }

    public void setPrefs(Integer codemp, Integer codfilial) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder sql = null;
        prefs = new Object[Atendimento.PREFS.values().length];
        try {
            sql = new StringBuilder("select codempmi, codfilialmi, codmodelmi,  ");
            sql.append("codempme, mi.descmodel descmodelmi, ");
            sql.append("codfilialme, codmodelme, me.descmodel descmodelme, tempomaxint, coalesce(tolregponto,20) tolregponto, ");
            sql.append("mi.codempea codempia, mi.codfilialea codfilialia, ea.codespec codespecia, ea.descespec descespecia , ");
            sql.append("codempfi, codfilialfi, codmodelfi, fi.descmodel descmodelfi, ");
            sql.append("codempfj, codfilialfj, codmodelfj, fj.descmodel descmodelfj, ");
            sql.append("codempap, codfilialap, codmodelap, ap.descmodel descmodelap ");
            sql.append("from sgprefere3 p ");
            sql.append("left outer join atmodatendo mi ");
            sql.append("on mi.codemp=p.codempmi and mi.codfilial=p.codfilialmi and mi.codmodel=p.codmodelmi ");
            sql.append("left outer join atmodatendo me ");
            sql.append("on me.codemp=p.codempme and me.codfilial=p.codfilialme and me.codmodel=p.codmodelme ");
            sql.append("left outer join atespecatend ea ");
            sql.append("on ea.codemp=mi.codempea and ea.codfilial=mi.codfilialea and ea.codespec=mi.codespec ");
            sql.append("left outer join atmodatendo fi ");
            sql.append("on fi.codemp=p.codempfi and fi.codfilial=p.codfilialfi and fi.codmodel=p.codmodelfi ");
            sql.append("left outer join atmodatendo fj ");
            sql.append("on fj.codemp=p.codempfj and fj.codfilial=p.codfilialfj and fj.codmodel=p.codmodelfj ");
            sql.append("left outer join atmodatendo ap ");
            sql.append("on ap.codemp=p.codempap and ap.codfilial=p.codfilialap and ap.codmodel=p.codmodelap ");
            sql.append("where  p.codemp=? and p.codfilial=?");
            ps = getConn().prepareStatement(sql.toString());
            ps.setInt(1, codemp);
            ps.setInt(2, codfilial);
            rs = ps.executeQuery();
            if (rs.next()) {
                prefs[PREFS.CODEMPMI.ordinal()] = new Integer(rs.getInt(PREFS.CODEMPMI.toString()));
                prefs[PREFS.CODFILIALMI.ordinal()] = new Integer(rs.getInt(PREFS.CODFILIALMI.toString()));
                prefs[PREFS.CODMODELMI.ordinal()] = new Integer(rs.getInt(PREFS.CODMODELMI.toString()));
                prefs[PREFS.DESCMODELMI.ordinal()] = rs.getString(PREFS.DESCMODELMI.toString());
                prefs[PREFS.CODEMPME.ordinal()] = new Integer(rs.getInt(PREFS.CODEMPME.toString()));
                prefs[PREFS.CODFILIALME.ordinal()] = new Integer(rs.getInt(PREFS.CODFILIALME.toString()));
                prefs[PREFS.CODMODELME.ordinal()] = new Integer(rs.getInt(PREFS.CODMODELME.toString()));
                prefs[PREFS.DESCMODELME.ordinal()] = rs.getString(PREFS.DESCMODELME.toString());
                prefs[PREFS.TEMPOMAXINT.ordinal()] = new Integer(rs.getInt(PREFS.TEMPOMAXINT.toString()));
                prefs[PREFS.TOLREGPONTO.ordinal()] = new Integer(rs.getInt(PREFS.TOLREGPONTO.toString()));
                prefs[PREFS.CODEMPIA.ordinal()] = new Integer(rs.getInt(PREFS.CODEMPIA.toString()));
                prefs[PREFS.CODFILIALIA.ordinal()] = new Integer(rs.getInt(PREFS.CODFILIALIA.toString()));
                prefs[PREFS.CODESPECIA.ordinal()] = new Integer(rs.getInt(PREFS.CODESPECIA.toString()));
                prefs[PREFS.DESCESPECIA.ordinal()] = rs.getString(PREFS.DESCESPECIA.toString());
                if (rs.getString(PREFS.CODMODELFI.toString()) != null) {
                    prefs[PREFS.CODEMPFI.ordinal()] = new Integer(rs.getInt(PREFS.CODEMPFI.toString()));
                    prefs[PREFS.CODFILIALFI.ordinal()] = new Integer(rs.getInt(PREFS.CODFILIALFI.toString()));
                    prefs[PREFS.CODMODELFI.ordinal()] = new Integer(rs.getInt(PREFS.CODMODELFI.toString()));
                    prefs[PREFS.DESCMODELFI.ordinal()] = rs.getString(PREFS.DESCMODELFI.toString());
                }
                if (rs.getString(PREFS.CODMODELFJ.toString()) != null) {
                    prefs[PREFS.CODEMPFJ.ordinal()] = new Integer(rs.getInt(PREFS.CODEMPFJ.toString()));
                    prefs[PREFS.CODFILIALFJ.ordinal()] = new Integer(rs.getInt(PREFS.CODFILIALFJ.toString()));
                    prefs[PREFS.CODMODELFJ.ordinal()] = new Integer(rs.getInt(PREFS.CODMODELFJ.toString()));
                    prefs[PREFS.DESCMODELFJ.ordinal()] = rs.getString(PREFS.DESCMODELFJ.toString());
                }
                prefs[PREFS.CODEMPAP.ordinal()] = new Integer(rs.getInt(PREFS.CODEMPAP.toString()));
                prefs[PREFS.CODFILIALAP.ordinal()] = new Integer(rs.getInt(PREFS.CODFILIALAP.toString()));
                prefs[PREFS.CODMODELAP.ordinal()] = new Integer(rs.getInt(PREFS.CODMODELAP.toString()));
                prefs[PREFS.DESCMODELAP.ordinal()] = rs.getString(PREFS.DESCMODELAP.toString());
            }
            rs.close();
            ps.close();
            getConn().commit();
        } finally {
            ps = null;
            rs = null;
            sql = null;
        }
    }

    public Object[] getPrefs() {
        return this.prefs;
    }

    public String getHoraPrimUltLanca(Integer codemp, Integer codfilial, Date dataatendo, String horaini, String horafim, Integer codempae, Integer codfilialae, Integer codatend, String aftela) throws SQLException {
        String result = null;
        StringBuilder sql = new StringBuilder();
        sql.append("select first 1 a.horaatendo, a.horaatendofin from atatendimento a ");
        sql.append("where a.codemp=? and a.codfilial=? and a.dataatendo=? and ");
        sql.append("a.codempae=? and a.codfilialae=? and a.codatend=? and ");
        if ("A".equals(aftela)) {
            sql.append("a.horaatendo>=? ");
            sql.append("order by a.dataatendo, a.horaatendo");
        } else {
            sql.append("a.horaatendofin<=? ");
            sql.append("order by a.dataatendo desc, a.horaatendofin desc");
        }
        PreparedStatement ps = getConn().prepareStatement(sql.toString());
        ps.setInt(PARAM_PRIM_LANCA.CODEMP.ordinal(), codemp);
        ps.setInt(PARAM_PRIM_LANCA.CODFILIAL.ordinal(), codfilial);
        ps.setDate(PARAM_PRIM_LANCA.DATAATENDO.ordinal(), Funcoes.dateToSQLDate(dataatendo));
        ps.setInt(PARAM_PRIM_LANCA.CODEMPAE.ordinal(), codempae);
        ps.setInt(PARAM_PRIM_LANCA.CODFILIALAE.ordinal(), codfilialae);
        ps.setInt(PARAM_PRIM_LANCA.CODATEND.ordinal(), codatend);
        if ("A".equals(aftela)) {
            ps.setTime(PARAM_PRIM_LANCA.HORAATENDO.ordinal(), Funcoes.strTimeToSqlTime(horaini, false));
        } else {
            ps.setTime(PARAM_PRIM_LANCA.HORAATENDO.ordinal(), Funcoes.strTimeToSqlTime(horafim, false));
        }
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            if ("A".equals(aftela)) {
                result = rs.getString(PARAM_PRIM_LANCA.HORAATENDO.toString());
            } else {
                result = rs.getString(PARAM_PRIM_LANCA.HORAATENDOFIN.toString());
            }
        }
        rs.close();
        ps.close();
        getConn().commit();
        return result;
    }

    public void insert(Atendimento atd) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("EXECUTE PROCEDURE ATATENDIMENTOIUSP(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        PreparedStatement ps = getConn().prepareStatement(sql.toString());
        ps.setString(PROC_IU.IU.ordinal(), "I");
        ps.setInt(PROC_IU.CODEMP.ordinal(), atd.getCodemp());
        ps.setInt(PROC_IU.CODFILIAL.ordinal(), atd.getCodfilial());
        if (atd.getCodatendo() == null) {
            ps.setInt(PROC_IU.CODATENDO.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODATENDO.ordinal(), atd.getCodatendo());
        }
        if (atd.getCodtpatendo() == null) {
            ps.setNull(PROC_IU.CODEMPTO.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALTO.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODTPATENDO.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPTO.ordinal(), atd.getCodempto());
            ps.setInt(PROC_IU.CODFILIALTO.ordinal(), atd.getCodfilialto());
            ps.setInt(PROC_IU.CODTPATENDO.ordinal(), atd.getCodtpatendo());
        }
        if (atd.getCodatend() == null) {
            ps.setNull(PROC_IU.CODEMPAE.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALAE.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODATEND.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPAE.ordinal(), atd.getCodempae());
            ps.setInt(PROC_IU.CODFILIALAE.ordinal(), atd.getCodfilialae());
            ps.setInt(PROC_IU.CODATEND.ordinal(), atd.getCodatend());
        }
        if (atd.getCodatend() == null) {
            ps.setNull(PROC_IU.CODEMPSA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALSA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODSETOR.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPSA.ordinal(), atd.getCodempae());
            ps.setInt(PROC_IU.CODFILIALSA.ordinal(), atd.getCodfilialae());
            ps.setInt(PROC_IU.CODSETOR.ordinal(), atd.getCodsetat());
        }
        if (atd.getIdusu() == null) {
            ps.setInt(PROC_IU.CODEMPUS.ordinal(), Types.INTEGER);
            ps.setInt(PROC_IU.CODFILIALUS.ordinal(), Types.INTEGER);
            ps.setInt(PROC_IU.IDUSU.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPUS.ordinal(), atd.getCodempus());
            ps.setInt(PROC_IU.CODFILIALUS.ordinal(), atd.getCodfilialus());
            ps.setString(PROC_IU.IDUSU.ordinal(), atd.getIdusu());
        }
        ps.setString(PROC_IU.DOCATENDO.ordinal(), atd.getDocatendo());
        ps.setDate(PROC_IU.DATAATENDO.ordinal(), Funcoes.dateToSQLDate(atd.getDataatendo()));
        ps.setDate(PROC_IU.DATAATENDOFIN.ordinal(), Funcoes.dateToSQLDate(atd.getDataatendofin()));
        ps.setTime(PROC_IU.HORAATENDO.ordinal(), Funcoes.strTimeToSqlTime(atd.getHoraatendo(), false));
        ps.setTime(PROC_IU.HORAATENDOFIN.ordinal(), Funcoes.strTimeToSqlTime(atd.getHoraatendofin(), false));
        ps.setString(PROC_IU.OBSATENDO.ordinal(), atd.getObsatendo());
        if (atd.getObsinterno() == null) {
            ps.setNull(PROC_IU.OBSINTERNO.ordinal(), Types.CHAR);
        } else {
            ps.setString(PROC_IU.OBSINTERNO.ordinal(), atd.getObsinterno());
        }
        ps.setString(PROC_IU.CONCLUICHAMADO.ordinal(), atd.getConcluichamado());
        ps.setString(PROC_IU.STATUSATENDO.ordinal(), atd.getStatusatendo());
        if (atd.getCodcli() == null) {
            ps.setNull(PROC_IU.CODEMPCL.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALCL.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODCLI.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPCL.ordinal(), atd.getCodempcl());
            ps.setInt(PROC_IU.CODFILIALCL.ordinal(), atd.getCodfilialcl());
            ps.setInt(PROC_IU.CODCLI.ordinal(), atd.getCodcli());
        }
        if (atd.getCodcontr() == null) {
            ps.setNull(PROC_IU.CODEMPCT.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALCT.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODCONTR.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODITCONTR.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPCT.ordinal(), Aplicativo.iCodEmp);
            ps.setInt(PROC_IU.CODFILIALCT.ordinal(), Aplicativo.iCodFilialPad);
            ps.setInt(PROC_IU.CODCONTR.ordinal(), atd.getCodcontr());
            ps.setInt(PROC_IU.CODITCONTR.ordinal(), atd.getCoditcontr());
        }
        if (atd.getCodrec() == null) {
            ps.setNull(PROC_IU.CODEMPIR.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALIR.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODREC.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.NPARCITREC.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPIR.ordinal(), atd.getCodempir());
            ps.setInt(PROC_IU.CODFILIALIR.ordinal(), atd.getCodfilialir());
            ps.setInt(PROC_IU.CODREC.ordinal(), atd.getCodrec());
            ps.setInt(PROC_IU.NPARCITREC.ordinal(), atd.getNparcitrec());
        }
        if (atd.getCodchamado() == null) {
            ps.setNull(PROC_IU.CODEMPCH.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALCH.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODCHAMADO.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPCH.ordinal(), atd.getCodempch());
            ps.setInt(PROC_IU.CODFILIALCH.ordinal(), atd.getCodfilialch());
            ps.setInt(PROC_IU.CODCHAMADO.ordinal(), atd.getCodchamado());
        }
        if (atd.getCodespec() == null) {
            ps.setNull(PROC_IU.CODEMPEA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALEA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODESPEC.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPEA.ordinal(), atd.getCodempae());
            ps.setInt(PROC_IU.CODFILIALEA.ordinal(), atd.getCodfilialae());
            ps.setInt(PROC_IU.CODESPEC.ordinal(), atd.getCodespec());
        }
        if (atd.getCodtarefa() == null) {
            ps.setNull(PROC_IU.CODEMPTA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALTA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODTAREFA.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPTA.ordinal(), atd.getCodempta());
            ps.setInt(PROC_IU.CODFILIALTA.ordinal(), atd.getCodfilialta());
            ps.setInt(PROC_IU.CODTAREFA.ordinal(), atd.getCodtarefa());
        }
        ps.execute();
        ps.close();
        getConn().commit();
    }

    public void update(Atendimento atd) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("EXECUTE PROCEDURE ATATENDIMENTOIUSP ");
        sql.append("( ");
        for (int i = 1; i < PROC_IU.values().length; i++) {
            if (i > 1) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(" )");
        PreparedStatement ps = getConn().prepareStatement(sql.toString());
        ps.setString(PROC_IU.IU.ordinal(), "U");
        ps.setInt(PROC_IU.CODEMP.ordinal(), atd.getCodemp());
        ps.setInt(PROC_IU.CODFILIAL.ordinal(), atd.getCodfilial());
        if (atd.getCodatendo() == null) {
            ps.setInt(PROC_IU.CODATENDO.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODATENDO.ordinal(), atd.getCodatendo());
        }
        if (atd.getCodtpatendo() == null) {
            ps.setNull(PROC_IU.CODEMPTO.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALTO.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODTPATENDO.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPTO.ordinal(), atd.getCodempto());
            ps.setInt(PROC_IU.CODFILIALTO.ordinal(), atd.getCodfilialto());
            ps.setInt(PROC_IU.CODTPATENDO.ordinal(), atd.getCodtpatendo());
        }
        if (atd.getCodatend() == null) {
            ps.setNull(PROC_IU.CODEMPAE.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALAE.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODATEND.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPAE.ordinal(), atd.getCodempae());
            ps.setInt(PROC_IU.CODFILIALAE.ordinal(), atd.getCodfilialae());
            ps.setInt(PROC_IU.CODATEND.ordinal(), atd.getCodatend());
        }
        ps.setString(PROC_IU.DOCATENDO.ordinal(), atd.getDocatendo());
        ps.setDate(PROC_IU.DATAATENDO.ordinal(), Funcoes.dateToSQLDate(atd.getDataatendo()));
        ps.setDate(PROC_IU.DATAATENDOFIN.ordinal(), Funcoes.dateToSQLDate(atd.getDataatendofin()));
        ps.setTime(PROC_IU.HORAATENDO.ordinal(), Funcoes.strTimeToSqlTime(atd.getHoraatendo(), false));
        ps.setTime(PROC_IU.HORAATENDOFIN.ordinal(), Funcoes.strTimeToSqlTime(atd.getHoraatendofin(), false));
        ps.setString(PROC_IU.OBSATENDO.ordinal(), atd.getObsatendo());
        if (atd.getCodcli() == null) {
            ps.setNull(PROC_IU.CODEMPCL.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALCL.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODCLI.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPCL.ordinal(), atd.getCodempcl());
            ps.setInt(PROC_IU.CODFILIALCL.ordinal(), atd.getCodfilialcl());
            ps.setInt(PROC_IU.CODCLI.ordinal(), atd.getCodcli());
        }
        if (atd.getCodsetat() == null) {
            ps.setNull(PROC_IU.CODEMPSA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALSA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODSETOR.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPSA.ordinal(), atd.getCodempsa());
            ps.setInt(PROC_IU.CODFILIALSA.ordinal(), atd.getCodfilialsa());
            ps.setInt(PROC_IU.CODSETOR.ordinal(), atd.getCodsetat());
        }
        if (atd.getIdusu() == null) {
            ps.setInt(PROC_IU.CODEMPUS.ordinal(), Types.INTEGER);
            ps.setInt(PROC_IU.CODFILIALUS.ordinal(), Types.INTEGER);
            ps.setInt(PROC_IU.IDUSU.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPUS.ordinal(), atd.getCodempus());
            ps.setInt(PROC_IU.CODFILIALUS.ordinal(), atd.getCodfilialus());
            ps.setString(PROC_IU.IDUSU.ordinal(), atd.getIdusu());
        }
        if (atd.getCodrec() == null) {
            ps.setNull(PROC_IU.CODEMPIR.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALIR.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODREC.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.NPARCITREC.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPIR.ordinal(), atd.getCodempir());
            ps.setInt(PROC_IU.CODFILIALIR.ordinal(), atd.getCodfilialir());
            ps.setInt(PROC_IU.CODREC.ordinal(), atd.getCodrec());
            ps.setInt(PROC_IU.NPARCITREC.ordinal(), atd.getNparcitrec());
        }
        if (atd.getCodchamado() == null) {
            ps.setNull(PROC_IU.CODEMPCH.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALCH.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODCHAMADO.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPCH.ordinal(), atd.getCodempch());
            ps.setInt(PROC_IU.CODFILIALCH.ordinal(), atd.getCodfilialch());
            ps.setInt(PROC_IU.CODCHAMADO.ordinal(), atd.getCodchamado());
        }
        if (atd.getCodcontr() == null) {
            ps.setNull(PROC_IU.CODEMPCT.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALCT.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODCONTR.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODITCONTR.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPCT.ordinal(), atd.getCodempct());
            ps.setInt(PROC_IU.CODFILIALCT.ordinal(), atd.getCodfilialct());
            ps.setInt(PROC_IU.CODCONTR.ordinal(), atd.getCodcontr());
            ps.setInt(PROC_IU.CODITCONTR.ordinal(), atd.getCoditcontr());
        }
        ps.setString(PROC_IU.STATUSATENDO.ordinal(), atd.getStatusatendo());
        if (atd.getObsinterno() == null) {
            ps.setNull(PROC_IU.OBSINTERNO.ordinal(), Types.CHAR);
        } else {
            ps.setString(PROC_IU.OBSINTERNO.ordinal(), atd.getObsinterno());
        }
        ps.setString(PROC_IU.CONCLUICHAMADO.ordinal(), atd.getConcluichamado());
        if (atd.getCodespec() == null) {
            ps.setNull(PROC_IU.CODEMPEA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALEA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODESPEC.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPEA.ordinal(), atd.getCodempea());
            ps.setInt(PROC_IU.CODFILIALEA.ordinal(), atd.getCodfilialea());
            ps.setInt(PROC_IU.CODESPEC.ordinal(), atd.getCodespec());
        }
        if (atd.getCodtarefa() == null) {
            ps.setNull(PROC_IU.CODEMPTA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODFILIALTA.ordinal(), Types.INTEGER);
            ps.setNull(PROC_IU.CODTAREFA.ordinal(), Types.INTEGER);
        } else {
            ps.setInt(PROC_IU.CODEMPTA.ordinal(), atd.getCodempta());
            ps.setInt(PROC_IU.CODFILIALTA.ordinal(), atd.getCodfilialta());
            ps.setInt(PROC_IU.CODTAREFA.ordinal(), atd.getCodtarefa());
        }
        ps.executeUpdate();
        ps.close();
        getConn().commit();
    }

    public String checarSitrevEstagio1234(final Vector<Vector<Object>> vexped, Vector<Vector<Object>> vatend) {
        String result = (String) EstagioCheck.EPE.getValue();
        for (Vector<Object> row : vexped) {
            result = (String) row.elementAt(EColExped.SITREVEXPED.ordinal());
            if (EstagioCheck.EPE.getValueTab().equals(result)) {
                result = (String) EstagioCheck.EPE.getValue();
            } else if (EstagioCheck.E1O.getValueTab().equals(result)) {
                result = (String) EstagioCheck.E1O.getValue();
            } else if (EstagioCheck.E2O.getValueTab().equals(result)) {
                result = (String) EstagioCheck.E2O.getValue();
            } else if (EstagioCheck.E1I.getValueTab().equals(result)) {
                result = (String) EstagioCheck.E1I.getValue();
                break;
            } else if (EstagioCheck.E2I.getValueTab().equals(result)) {
                result = (String) EstagioCheck.E2I.getValue();
                break;
            }
        }
        if (result.substring(2).equals("O")) {
            for (Vector<Object> row : vatend) {
                result = (String) row.elementAt(EColAtend.SITREVATENDO.ordinal());
                if (EstagioCheck.EPE.getValueTab().equals(result)) {
                    result = (String) EstagioCheck.EPE.getValue();
                } else if (EstagioCheck.E3O.getValueTab().equals(result)) {
                    result = (String) EstagioCheck.E3O.getValue();
                } else if (EstagioCheck.E3I.getValueTab().equals(result)) {
                    result = (String) EstagioCheck.E3I.getValue();
                    break;
                } else if (EstagioCheck.E4O.getValueTab().equals(result)) {
                    result = (String) EstagioCheck.E4O.getValue();
                } else if (EstagioCheck.E4I.getValueTab().equals(result)) {
                    result = (String) EstagioCheck.E4I.getValue();
                    break;
                } else if (EstagioCheck.E5I.getValueTab().equals(result)) {
                    result = (String) EstagioCheck.E5I.getValue();
                    break;
                }
            }
        }
        return result;
    }

    public String checarSitrevEstagio2(final Vector<Vector<Object>> vexped) {
        String result = (String) EstagioCheck.EPE.getValue();
        String temp = null;
        for (Vector<Object> row : vexped) {
            temp = (String) row.elementAt(EColExped.SITREVEXPED.ordinal());
            if (EstagioCheck.E1I.getValueTab().equals(temp)) {
                result = (String) EstagioCheck.E1I.getValue();
                break;
            }
        }
        return result;
    }

    public Vector<Batida> getRegistroBatidas(Vector<Vector<Object>> vexped, int nbatidas) {
        Vector<Batida> result = new Vector<Batida>();
        Batida batida = null;
        int colini = EColExped.HFIMTURNO.ordinal() + nbatidas + 1;
        Date dtbat = null;
        String hbat = null;
        for (Vector<Object> row : vexped) {
            dtbat = Funcoes.strDateToDate((String) row.elementAt(EColExped.DTEXPED.ordinal()));
            for (int i = colini; i < row.size(); i++) {
                hbat = (String) row.elementAt(i);
                if (!((hbat == null) || (hbat.trim().equals("")))) {
                    batida = new Batida();
                    batida.setDataponto(dtbat);
                    batida.setHoraponto(hbat);
                    result.addElement(batida);
                }
            }
        }
        return result;
    }

    public String checarSitrevAtend(final Vector<Vector<Object>> vatend) {
        String sitrev = null;
        String sitrevant = null;
        for (Vector<Object> row : vatend) {
            sitrev = "E" + row.elementAt(EColAtend.SITREVATENDO.ordinal());
            if (EstagioCheck.EPE.getValue().equals(sitrev)) {
                break;
            } else if (sitrevant == null) {
                sitrevant = sitrev;
            } else if (sitrevant.compareTo(sitrev) > 0) {
                sitrevant = sitrev;
            } else if (sitrev.compareTo(sitrevant) > 0) {
                sitrev = sitrevant;
            }
        }
        return sitrev;
    }

    public String checarSitrevExped(final Vector<Vector<Object>> vexped) {
        String sitrev = null;
        String sitrevant = null;
        for (Vector<Object> row : vexped) {
            sitrev = "E" + row.elementAt(EColExped.SITREVEXPED.ordinal());
            if (EstagioCheck.EPE.getValue().equals(sitrev)) {
                break;
            } else if (sitrevant == null) {
                sitrevant = sitrev;
            } else if (sitrevant.compareTo(sitrev) > 0) {
                sitrevant = sitrev;
            } else if (sitrev.compareTo(sitrevant) > 0) {
                sitrev = sitrevant;
            }
        }
        return sitrev;
    }

    public boolean checar(final Vector<Vector<Object>> vexped, final Vector<Vector<Object>> vatend, final int nbatidas) {
        boolean result = false;
        String sitrev = checarSitrevExped(vexped);
        if (EstagioCheck.EPE.getValue().equals(sitrev)) {
            result = checarEstagio1(vexped, vatend, nbatidas);
            if (!result) {
                result = checarEstagio2(vexped, nbatidas);
                if (!result) {
                    result = checarEstagio3(vexped, vatend, nbatidas);
                    if (!result) {
                        result = checarEstagio4(vexped, vatend, nbatidas);
                        if (!result) {
                            result = checarEstagio5(vexped, vatend, nbatidas);
                        }
                    }
                }
            }
        }
        return result;
    }

    public boolean checarEstagio3(final Vector<Vector<Object>> vexped, final Vector<Vector<Object>> vatend, final int nbatidas) {
        boolean result = false;
        int posini = EColExped.HFIMTURNO.ordinal() + 1;
        int numcols = posini + nbatidas;
        Vector<Object> atend = null;
        Vector<String> batidas = null;
        Vector<String> lanctos = null;
        Vector<Object[]> lanctosBatidas = null;
        String dtatend = null;
        for (Vector<Object> row : vexped) {
            batidas = getBatidas(row, posini, numcols);
            if ((batidas != null) && (batidas.size() >= 4)) {
                dtatend = (String) row.elementAt(EColExped.DTEXPED.ordinal());
                lanctos = getHorariosLanctos(dtatend, vatend);
                if (lanctos.size() > 0) {
                    lanctosBatidas = getHorariosLanctosBatidas(batidas, lanctos);
                    result = applyLanctoBatidas(vatend, nbatidas, lanctosBatidas, dtatend);
                }
            }
        }
        for (Vector<Object> row : vatend) {
            if (row.elementAt(EColAtend.SITREVATENDO.ordinal()).equals(EstagioCheck.EPE.getValueTab())) {
                row.setElementAt(EstagioCheck.E3O.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                row.setElementAt(EstagioCheck.E3O.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
            }
        }
        return result;
    }

    private boolean applyLanctoBatidas(final Vector<Vector<Object>> vatend, final int nbatidas, Vector<Object[]> lanctosBatidas, String dtatend) {
        Object[] lanctobatida = null;
        String inifinturno = null;
        String inifinturnoant = null;
        String horatemp1 = null;
        String horatemp2 = null;
        String dtatendant = null;
        long intervalo = 0;
        int intervalomin = 0;
        int intervaloant = 0;
        int posatend = -1;
        int posatendant = -1;
        boolean result = false;
        int tolintervalo = (Integer) prefs[PREFS.TOLREGPONTO.ordinal()] / 2;
        if (lanctosBatidas.size() > 0) {
            for (int lb = 0; lb < lanctosBatidas.size(); lb++) {
                lanctobatida = lanctosBatidas.elementAt(lb);
                if (lb % 2 > 0) {
                    posatend = locateAtend(vatend, dtatend, (String) lanctobatida[COLBATLANCTO.LANCTO.ordinal()], true);
                    inifinturno = INIFINTURNO.F.toString();
                } else {
                    posatend = locateAtend(vatend, dtatend, (String) lanctobatida[COLBATLANCTO.LANCTO.ordinal()], false);
                    inifinturno = INIFINTURNO.I.toString();
                }
                if (posatend > -1) {
                    inifinturnoant = (String) vatend.elementAt(posatend).elementAt(EColAtend.INIFINTURNO.ordinal());
                }
                if ((posatend > -1) && ("".equals(inifinturnoant))) {
                    horatemp1 = (String) lanctobatida[COLBATLANCTO.BATIDA.ordinal()];
                    vatend.elementAt(posatend).setElementAt(horatemp1, EColAtend.HORABATIDA.ordinal());
                    vatend.elementAt(posatend).setElementAt(inifinturno, EColAtend.INIFINTURNO.ordinal());
                    horatemp2 = (String) lanctobatida[COLBATLANCTO.LANCTO.ordinal()];
                    if (inifinturno.equals(INIFINTURNO.I.toString())) {
                        intervalo = Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(horatemp1, false), Funcoes.strTimeToSqlTime(horatemp2, false));
                    } else {
                        intervalo = Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(horatemp2, false), Funcoes.strTimeToSqlTime(horatemp1, false));
                    }
                    intervalomin = (int) intervalo / 1000 / 60;
                    intervaloant = (Integer) vatend.elementAt(posatend).elementAt(EColAtend.INTERVATENDO.ordinal());
                    vatend.elementAt(posatend).setElementAt(new Integer(intervalomin), EColAtend.INTERVATENDO.ordinal());
                    if (intervalomin == 0) {
                        if ((inifinturno.equals(INIFINTURNO.F.toString())) && (intervaloant > 0)) {
                            vatend.elementAt(posatend).setElementAt(new Integer(intervaloant), EColAtend.INTERVATENDO.ordinal());
                        }
                    } else {
                        result = true;
                        vatend.elementAt(posatend).setElementAt(EstagioCheck.E3I.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                        vatend.elementAt(posatend).setElementAt(EstagioCheck.E3I.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
                        if (intervalomin < 0) {
                            posatendant = posatend - 1;
                            if (posatendant > -1) {
                                dtatendant = (String) vatend.elementAt(posatendant).elementAt(EColAtend.DATAATENDO.ordinal());
                                if (dtatend.equals(dtatendant)) {
                                    inifinturnoant = (String) vatend.elementAt(posatendant).elementAt(EColAtend.INIFINTURNO.ordinal());
                                    if (((inifinturno.equals(INIFINTURNO.F.toString())) && (INIFINTURNO.I.toString().equals(inifinturnoant))) || ((inifinturno.equals(INIFINTURNO.I.toString()))) && ((!INIFINTURNO.F.toString().equals(inifinturnoant)))) {
                                        intervalomin = intervalomin * -1;
                                    }
                                }
                            }
                        }
                        if (intervalomin > 0) {
                            if (intervalomin > tolintervalo) {
                                intervalomin = tolintervalo;
                                vatend.elementAt(posatend).setElementAt(new Integer(intervalomin), EColAtend.INTERVATENDO.ordinal());
                                if (inifinturno.equals(INIFINTURNO.I.toString())) {
                                    horatemp2 = Funcoes.longTostrTime(Funcoes.somaTime(Funcoes.strTimeToSqlTime(horatemp1, false), Funcoes.strTimeToSqlTime(Funcoes.longTostrTime((long) intervalomin * 1000 * 60), false)));
                                } else {
                                    horatemp2 = Funcoes.longTostrTime(Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(Funcoes.longTostrTime((long) intervalomin * 1000 * 60), false), Funcoes.strTimeToSqlTime(horatemp1, false)));
                                }
                            }
                            vatend.elementAt(posatend).setElementAt(prefs[PREFS.CODMODELME.ordinal()], EColAtend.CODMODEL.ordinal());
                            vatend.elementAt(posatend).setElementAt(prefs[PREFS.DESCMODELME.ordinal()], EColAtend.DESCMODEL.ordinal());
                            horatemp1 = Funcoes.copy(horatemp1, 5);
                            horatemp2 = Funcoes.copy(horatemp2, 5);
                            if (inifinturno.equals(INIFINTURNO.I.toString())) {
                                vatend.elementAt(posatend).setElementAt(horatemp1, EColAtend.HORAINI.ordinal());
                                vatend.elementAt(posatend).setElementAt(horatemp2, EColAtend.HORAFIN.ordinal());
                            } else {
                                vatend.elementAt(posatend).setElementAt(horatemp2, EColAtend.HORAINI.ordinal());
                                vatend.elementAt(posatend).setElementAt(horatemp1, EColAtend.HORAFIN.ordinal());
                            }
                        } else {
                            horatemp1 = (String) vatend.elementAt(posatend).elementAt(EColAtend.HORAATENDO.ordinal());
                            horatemp2 = (String) vatend.elementAt(posatend).elementAt(EColAtend.HORAATENDOFIN.ordinal());
                            if (inifinturno.equals(INIFINTURNO.I.toString())) {
                                horatemp1 = (String) vatend.elementAt(posatend).elementAt(EColAtend.HORABATIDA.ordinal());
                            } else {
                                horatemp2 = (String) vatend.elementAt(posatend).elementAt(EColAtend.HORABATIDA.ordinal());
                            }
                            vatend.elementAt(posatend).setElementAt(horatemp1, EColAtend.HORAINI.ordinal());
                            vatend.elementAt(posatend).setElementAt(horatemp2, EColAtend.HORAFIN.ordinal());
                        }
                    }
                }
            }
        }
        return result;
    }

    private int locateAtend(Vector<Vector<Object>> vatend, String dataatendo, String horaatendo, boolean atendfin) {
        int result = -1;
        String hora = null;
        String data = null;
        for (int i = 0; i < vatend.size(); i++) {
            data = (String) vatend.elementAt(i).elementAt(EColAtend.DATAATENDO.ordinal());
            if (atendfin) {
                hora = (String) vatend.elementAt(i).elementAt(EColAtend.HORAATENDOFIN.ordinal());
            } else {
                hora = (String) vatend.elementAt(i).elementAt(EColAtend.HORAATENDO.ordinal());
            }
            if ((horaatendo.equals(hora)) && (dataatendo.equals(data))) {
                result = i;
                break;
            }
        }
        return result;
    }

    public boolean checarEstagio2(final Vector<Vector<Object>> vexped, final int nbatidas) {
        boolean result = false;
        int posini = EColExped.HFIMTURNO.ordinal() + 1;
        int numcols = posini + nbatidas;
        Vector<String> batidas = null;
        long intervalo1 = 0;
        long intervalo2 = 0;
        float intervhoras1 = 0;
        float intervhoras2 = 0;
        String tempo = "";
        float TEMPOMAXTURNO = 6f;
        for (Vector<Object> row : vexped) {
            batidas = getBatidas(row, posini, numcols);
            if ((batidas != null) && (batidas.size() >= 4)) {
                intervalo1 = Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(getTimeString(batidas.elementAt(COLBAT.INITURNO.ordinal())), false), Funcoes.strTimeToSqlTime(getTimeString(batidas.elementAt(COLBAT.INIINTTURNO.ordinal())), false));
                intervhoras1 = intervalo1 / 1000f / 60f / 60f;
                intervalo2 = Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(getTimeString((batidas.elementAt(COLBAT.FININTTURNO.ordinal()))), false), Funcoes.strTimeToSqlTime(getTimeString((batidas.elementAt(COLBAT.FINTURNO.ordinal()))), false));
                intervhoras2 = intervalo2 / 1000f / 60f / 60f;
                if ((intervhoras1 > TEMPOMAXTURNO) || (intervhoras2 > TEMPOMAXTURNO)) {
                    if (intervhoras1 > TEMPOMAXTURNO) {
                        tempo = Funcoes.longTostrTime(intervalo1);
                        row.setElementAt(tempo, numcols);
                    }
                    if (intervhoras2 > TEMPOMAXTURNO) {
                        tempo = Funcoes.longTostrTime(intervalo2);
                        row.setElementAt(tempo, numcols + 1);
                    }
                    row.setElementAt(EstagioCheck.E2I.getImg(), EColExped.SITREVEXPEDIMG.ordinal());
                    row.setElementAt(EstagioCheck.E2I.getValueTab(), EColExped.SITREVEXPED.ordinal());
                    result = true;
                }
            }
            if (!result) {
                row.setElementAt(EstagioCheck.E2O.getImg(), EColExped.SITREVEXPEDIMG.ordinal());
                row.setElementAt(EstagioCheck.E2O.getValueTab(), EColExped.SITREVEXPED.ordinal());
            }
        }
        return result;
    }

    public boolean checarEstagio1(final Vector<Vector<Object>> vexped, final Vector<Vector<Object>> vatend, int nbatidas) {
        boolean result = false;
        Vector<Object> row = null;
        int numcols = 0;
        int qtdant = 0;
        String dtexped = null;
        Vector<String> batidas = null;
        Vector<String> turno = null;
        Vector<String> turnosembatida = null;
        Vector<String> hlanctos = null;
        Vector<String> hlanctosturno = null;
        qtdant = EColExped.HFIMTURNO.ordinal() + 1;
        numcols = qtdant + nbatidas;
        for (int r = 0; r < vexped.size(); r++) {
            row = vexped.elementAt(r);
            dtexped = (String) row.elementAt(EColExped.DTEXPED.ordinal());
            turno = getTurno(getTimeString(row.elementAt(EColExped.HINITURNO.ordinal())), getTimeString(row.elementAt(EColExped.HINIINTTURNO.ordinal())), getTimeString(row.elementAt(EColExped.HFIMINTTURNO.ordinal())), getTimeString(row.elementAt(EColExped.HFIMTURNO.ordinal())));
            batidas = getBatidas(row, qtdant, numcols);
            if (batidas.size() < turno.size()) {
                row.setElementAt(EstagioCheck.E1I.getImg(), EColExped.SITREVEXPEDIMG.ordinal());
                row.setElementAt(EstagioCheck.E1I.getValueTab(), EColExped.SITREVEXPED.ordinal());
                turnosembatida = getTurnosembatida(batidas, turno);
                hlanctos = getHorariosLanctos(dtexped, vatend);
                hlanctosturno = getHorariosLanctosTurno(turnosembatida, hlanctos);
                result = setHorariosLanctos(row, numcols, hlanctosturno);
            } else {
                row.setElementAt(EstagioCheck.E1O.getImg(), EColExped.SITREVEXPEDIMG.ordinal());
                row.setElementAt(EstagioCheck.E1O.getValueTab(), EColExped.SITREVEXPED.ordinal());
            }
        }
        return result;
    }

    public static String getTimeString(Object hora) {
        return Funcoes.copy((String) hora, 5);
    }

    private boolean setHorariosLanctos(final Vector<Object> row, int numcols, Vector<String> hlanctos) {
        boolean result = false;
        int tot = numcols + hlanctos.size();
        int pos = 0;
        for (int i = numcols; i < tot; i++) {
            row.setElementAt(hlanctos.elementAt(pos), i);
            result = true;
            pos++;
        }
        return result;
    }

    private Vector<Object[]> getHorariosLanctosBatidas(Vector<String> batidas, Vector<String> lanctos) {
        Vector<Object[]> result = new Vector<Object[]>();
        Vector<Object[]> lanctosbatidas = new Vector<Object[]>();
        Vector<Object[]> temp = new Vector<Object[]>();
        Vector<Object[]> resulttmp = new Vector<Object[]>();
        Object[] batlancto = null;
        Object[] batlanctotmp = null;
        String hlancto = null;
        String hbatida = null;
        int posdif = -1;
        int posmin = -1;
        long dif = 0;
        long difant = 0;
        int iniloop = 0;
        for (int i = 0; i < batidas.size(); i++) {
            hbatida = batidas.elementAt(i);
            for (int t = 0; t < lanctos.size(); t++) {
                hlancto = lanctos.elementAt(t);
                dif = Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(hlancto, false), Funcoes.strTimeToSqlTime(hbatida, false));
                dif = dif / 1000 / 60;
                if (dif < 0) {
                    dif = dif * -1;
                }
                batlancto = new Object[COLBATLANCTO.values().length];
                batlancto[COLBATLANCTO.BATIDA.ordinal()] = hbatida;
                batlancto[COLBATLANCTO.LANCTO.ordinal()] = hlancto;
                batlancto[COLBATLANCTO.DIF.ordinal()] = new Long(dif);
                batlancto[COLBATLANCTO.POS.ordinal()] = new Integer(i);
                lanctosbatidas.addElement(batlancto);
            }
        }
        for (int i = 0; i < lanctosbatidas.size(); i++) {
            temp.addElement(lanctosbatidas.elementAt(i));
        }
        while (temp.size() > 0) {
            posdif = -1;
            difant = 0;
            for (int i = 0; i < temp.size(); i++) {
                batlancto = temp.elementAt(i);
                hbatida = (String) batlancto[COLBATLANCTO.BATIDA.ordinal()];
                hlancto = (String) batlancto[COLBATLANCTO.LANCTO.ordinal()];
                dif = (Long) batlancto[COLBATLANCTO.DIF.ordinal()];
                if (posdif == -1) {
                    posdif = i;
                } else {
                    difant = (Long) temp.elementAt(posdif)[COLBATLANCTO.DIF.ordinal()];
                    if (dif < difant) {
                        posdif = i;
                    }
                }
            }
            if (posdif > -1) {
                batlancto = temp.elementAt(posdif);
                resulttmp.add(batlancto);
                hbatida = (String) batlancto[COLBATLANCTO.BATIDA.ordinal()];
                hlancto = (String) batlancto[COLBATLANCTO.LANCTO.ordinal()];
                temp.removeElement(batlancto);
                iniloop = temp.size() - 1;
                for (int i = iniloop; ((i >= 0) && (temp.size() > 0)); i--) {
                    batlanctotmp = temp.elementAt(i);
                    if ((hbatida.equals(batlanctotmp[COLBATLANCTO.BATIDA.ordinal()])) || (hlancto.equals(batlanctotmp[COLBATLANCTO.LANCTO.ordinal()]))) {
                        temp.removeElement(batlanctotmp);
                    }
                }
            }
        }
        iniloop = resulttmp.size();
        while (result.size() != iniloop) {
            posmin = -1;
            for (int i = 0; i < resulttmp.size(); i++) {
                batlancto = resulttmp.elementAt(i);
                hbatida = (String) batlancto[COLBATLANCTO.BATIDA.ordinal()];
                hlancto = (String) batlancto[COLBATLANCTO.LANCTO.ordinal()];
                if (posmin == -1) {
                    posmin = i;
                } else {
                    batlanctotmp = resulttmp.elementAt(posmin);
                    if (hbatida.compareTo((String) batlanctotmp[COLBATLANCTO.BATIDA.ordinal()]) < 0) {
                        posmin = i;
                    }
                }
            }
            if (posmin != -1) {
                batlanctotmp = resulttmp.elementAt(posmin);
                result.addElement(batlanctotmp);
                resulttmp.removeElementAt(posmin);
            }
        }
        return result;
    }

    private Vector<String> getHorariosLanctosTurno(Vector<String> turnosembatida, Vector<String> hlanctos) {
        Vector<String> result = new Vector<String>();
        Vector<String> temp = new Vector<String>();
        String hbat = null;
        String hturno = null;
        int posdif = -1;
        long dif = 0;
        long difant = 0;
        for (int i = 0; i < hlanctos.size(); i++) {
            temp.add(hlanctos.elementAt(i));
        }
        for (int i = 0; i < turnosembatida.size(); i++) {
            hturno = turnosembatida.elementAt(i);
            posdif = -1;
            for (int t = 0; t < temp.size(); t++) {
                hbat = temp.elementAt(t);
                dif = Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(hbat, false), Funcoes.strTimeToSqlTime(hturno, false));
                if (dif < 0) {
                    dif = dif * -1;
                }
                if (posdif == -1) {
                    difant = dif;
                    posdif = t;
                } else if (dif < difant) {
                    difant = dif;
                    posdif = t;
                }
            }
            if (posdif > -1) {
                result.addElement(temp.elementAt(posdif));
                temp.remove(posdif);
            }
        }
        return result;
    }

    private Vector<String> getHorariosLanctos(String dtatend, Vector<Vector<Object>> vatend) {
        Vector<String> result = new Vector<String>();
        Vector<Object> row = null;
        int pos = primeiroAtend(vatend, dtatend);
        String dataatendo = null;
        String horaatendo = null;
        String horaatendofin = null;
        String horaatendofinant = null;
        int intervalo = 0;
        if (pos > -1) {
            row = vatend.elementAt(pos);
            dataatendo = (String) row.elementAt(EColAtend.DATAATENDO.ordinal());
            horaatendo = (String) row.elementAt(EColAtend.HORAATENDO.ordinal());
            horaatendofin = (String) row.elementAt(EColAtend.HORAATENDOFIN.ordinal());
            result.add(horaatendo);
            pos++;
            while ((pos < vatend.size()) && (dtatend.equals(dataatendo))) {
                horaatendofinant = (String) row.elementAt(EColAtend.HORAATENDOFIN.ordinal());
                row = vatend.elementAt(pos);
                dataatendo = (String) row.elementAt(EColAtend.DATAATENDO.ordinal());
                horaatendo = (String) row.elementAt(EColAtend.HORAATENDO.ordinal());
                horaatendofin = (String) row.elementAt(EColAtend.HORAATENDOFIN.ordinal());
                intervalo = (Integer) row.elementAt(EColAtend.INTERVATENDO.ordinal());
                if (!dtatend.equals(dataatendo)) {
                    result.add(horaatendofinant);
                } else if (intervalo > 0) {
                    result.add(horaatendofinant);
                    result.add(horaatendo);
                }
                pos++;
            }
            result.add(horaatendofin);
        }
        return result;
    }

    private Vector<String> getTurno(String hiniturno, String hiniintturno, String hfimintturno, String hfimturno) {
        Vector<String> result = new Vector<String>();
        if (hiniintturno.equals(hfimintturno)) {
            result.add(hiniturno);
            result.add(hfimturno);
        } else {
            result.add(hiniturno);
            result.add(hiniintturno);
            result.add(hfimintturno);
            result.add(hfimturno);
        }
        return result;
    }

    private Vector<String> getTurnosembatida(Vector<String> batidas, Vector<String> turnos) {
        Vector<String> result = new Vector<String>();
        String hbat = null;
        String hturno = null;
        int posdif = -1;
        long dif = 0;
        long difant = 0;
        for (int i = 0; i < turnos.size(); i++) {
            result.add(turnos.elementAt(i));
        }
        for (int i = 0; i < batidas.size(); i++) {
            hbat = batidas.elementAt(i);
            posdif = -1;
            for (int t = 0; t < result.size(); t++) {
                hturno = result.elementAt(t);
                dif = Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(hturno, false), Funcoes.strTimeToSqlTime(hbat, false));
                if (dif < 0) {
                    dif = dif * -1;
                }
                if (posdif == -1) {
                    difant = dif;
                    posdif = t;
                } else if (dif < difant) {
                    difant = dif;
                    posdif = t;
                }
            }
            if (posdif > -1) {
                result.remove(posdif);
            }
        }
        return result;
    }

    private Vector<String> getBatidas(Vector<Object> row, int posini, int numcols) {
        Vector<String> result = new Vector<String>();
        String hbat = null;
        if (row != null) {
            for (int i = posini; i < numcols; i++) {
                hbat = getTimeString((String) row.elementAt(i));
                if ((hbat != null) && (!"".equals(hbat.trim()))) {
                    result.add(hbat);
                }
            }
        }
        return result;
    }

    public boolean checarEstagio5(final Vector<Vector<Object>> vexped, final Vector<Vector<Object>> vatend, final int nbatidas) {
        boolean result = false;
        Integer codmodel = (Integer) prefs[PREFS.CODMODELAP.ordinal()];
        String descmodel = (String) prefs[PREFS.DESCMODELAP.ordinal()];
        String dtatendo = null;
        String horaini = null;
        String horafin = null;
        String horaintervalo = null;
        String horaatendo = null;
        int totalmin = 0;
        int intervalo = 0;
        for (Vector<Object> row : vatend) {
            dtatendo = (String) row.elementAt(EColAtend.DATAATENDO.ordinal());
            horaatendo = (String) row.elementAt(EColAtend.HORAATENDO.ordinal());
            intervalo = (Integer) row.elementAt(EColAtend.INTERVATENDO.ordinal());
            if (!verificHorarioTurno(vexped, dtatendo, horaatendo, nbatidas)) {
                intervalo = 0;
            }
            if (intervalo > 0) {
                horaintervalo = Funcoes.longTostrTime((long) intervalo * 1000 * 60);
                horafin = Funcoes.copy((String) row.elementAt(EColAtend.HORAATENDO.ordinal()), 5);
                horaini = Funcoes.copy(Funcoes.longTostrTime(Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(horaintervalo, false), Funcoes.strTimeToSqlTime(horafin, false))), 5);
                row.setElementAt(horaini, EColAtend.HORAINI.ordinal());
                row.setElementAt(horafin, EColAtend.HORAFIN.ordinal());
                row.setElementAt(EstagioCheck.E5I.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                row.setElementAt(EstagioCheck.E5I.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
                row.setElementAt(codmodel, EColAtend.CODMODEL.ordinal());
                row.setElementAt(descmodel, EColAtend.DESCMODEL.ordinal());
                result = true;
            } else {
                row.setElementAt(EstagioCheck.E5O.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                row.setElementAt(EstagioCheck.E5O.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
            }
        }
        return result;
    }

    public boolean checarEstagio6(final Vector<Vector<Object>> vexped, final Vector<Vector<Object>> vatend) {
        boolean result = false;
        Integer codmodel = (Integer) prefs[PREFS.CODMODELAP.ordinal()];
        String descmodel = (String) prefs[PREFS.DESCMODELAP.ordinal()];
        String dataatendo = null;
        String dataatendoant = null;
        String horaini = null;
        String horafin = null;
        String horaintervalo = null;
        String horaatendo = null;
        String inifinatendo = null;
        String inifinatendoant = null;
        Vector<Object> rowant = null;
        for (Vector<Object> row : vatend) {
            if (rowant == null) {
                rowant = row;
            } else {
                dataatendo = (String) row.elementAt(EColAtend.DATAATENDO.ordinal());
                inifinatendo = (String) row.elementAt(EColAtend.INIFINTURNO.ordinal());
                dataatendoant = (String) rowant.elementAt(EColAtend.DATAATENDO.ordinal());
                inifinatendoant = (String) rowant.elementAt(EColAtend.INIFINTURNO.ordinal());
                if ((inifinatendoant.equals(INIFINTURNO.F) && (inifinatendo.equals(INIFINTURNO.I))) || (inifinatendoant.equals("") && (inifinatendo.equals(INIFINTURNO.F)))) {
                    row.setElementAt(EstagioCheck.E6O.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                    row.setElementAt(EstagioCheck.E6O.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
                } else {
                    row.setElementAt(EstagioCheck.E6I.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                    row.setElementAt(EstagioCheck.E6I.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
                }
            }
        }
        return result;
    }

    private boolean verificHorarioTurno(Vector<Vector<Object>> vexped, String data, String hora, int nbatidas) {
        boolean result = true;
        String hini = null;
        String hfin = null;
        int posini = EColExped.HFIMTURNO.ordinal() + 1;
        int numcols = posini + nbatidas;
        Vector<String> batidas = null;
        Vector<Object> rowbat = null;
        for (Vector<Object> row : vexped) {
            if (row.elementAt(EColExped.DTEXPED.ordinal()).equals(data)) {
                rowbat = row;
                break;
            }
        }
        batidas = getBatidas(rowbat, posini, numcols);
        if (batidas.size() > 0) {
            hini = batidas.elementAt(0);
            if (batidas.size() >= 4) {
                hfin = batidas.elementAt(3);
            } else if (batidas.size() >= 2) {
                hfin = batidas.elementAt(1);
            }
            if ((hora.compareTo(hini) < 0) || (hora.compareTo(hfin) > 0)) {
                result = false;
            }
        }
        return result;
    }

    public boolean checarEstagio4(final Vector<Vector<Object>> vexped, final Vector<Vector<Object>> vatend, int nbatidas) {
        boolean result = false;
        Integer codmodel = (Integer) prefs[PREFS.CODMODELMI.ordinal()];
        String descmodel = (String) prefs[PREFS.DESCMODELMI.ordinal()];
        int tempomaxint = (Integer) prefs[PREFS.TEMPOMAXINT.ordinal()];
        Integer codespecia = (Integer) prefs[PREFS.CODESPECIA.ordinal()];
        String descespecia = (String) prefs[PREFS.DESCESPECIA.ordinal()];
        String dtatendopos = null;
        String dtatendo = null;
        String horaini = null;
        String horafin = null;
        String horaatendo = null;
        String horaintervalo = null;
        Vector<Object> row = null;
        Vector<Object> rowPos = null;
        int totalmin = 0;
        int intervalo = 0;
        int intervaloinserir = 0;
        int totintervalo = 0;
        int pos = 0;
        for (int i = 0; i < vatend.size(); i++) {
            row = vatend.elementAt(i);
            dtatendo = (String) row.elementAt(EColAtend.DATAATENDO.ordinal());
            horaatendo = (String) row.elementAt(EColAtend.HORAATENDO.ordinal());
            intervalo = (Integer) row.elementAt(EColAtend.INTERVATENDO.ordinal());
            if (!verificHorarioTurno(vexped, dtatendo, horaatendo, nbatidas)) {
                intervalo = 0;
            }
            if (intervalo > 0) {
                pos = primeiroAtend(vatend, dtatendo);
                dtatendopos = dtatendo;
                totalmin = 0;
                while ((pos < vatend.size()) && (dtatendo.equals(dtatendopos))) {
                    rowPos = vatend.elementAt(pos);
                    dtatendopos = (String) rowPos.elementAt(EColAtend.DATAATENDO.ordinal());
                    if (codespecia.equals((Integer) rowPos.elementAt(EColAtend.CODESPEC.ordinal()))) {
                        totalmin += ((Integer) rowPos.elementAt(EColAtend.TOTALMIN.ordinal()));
                    }
                    pos++;
                }
                intervaloinserir = intervalo;
                if (intervaloinserir > tempomaxint) {
                    intervaloinserir = tempomaxint;
                }
                intervaloinserir = intervaloinserir - totalmin;
                if (row != null) {
                    if ((row != null) && (intervaloinserir > 0)) {
                        horaintervalo = Funcoes.longTostrTime((long) intervaloinserir * 1000 * 60);
                        horafin = getTimeString(row.elementAt(EColAtend.HORAATENDO.ordinal()));
                        horaini = getTimeString(Funcoes.longTostrTime(Funcoes.subtraiTime(Funcoes.strTimeToSqlTime(horaintervalo, false), Funcoes.strTimeToSqlTime(horafin, false))));
                        row.setElementAt(horaini, EColAtend.HORAINI.ordinal());
                        row.setElementAt(horafin, EColAtend.HORAFIN.ordinal());
                        row.setElementAt(EstagioCheck.E4I.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                        row.setElementAt(EstagioCheck.E4I.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
                        row.setElementAt(codmodel, EColAtend.CODMODEL.ordinal());
                        row.setElementAt(descmodel, EColAtend.DESCMODEL.ordinal());
                        result = true;
                    } else {
                        row.setElementAt(EstagioCheck.E4O.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                        row.setElementAt(EstagioCheck.E4O.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
                    }
                }
            } else if (row != null) {
                row.setElementAt(EstagioCheck.E4O.getValueTab(), EColAtend.SITREVATENDO.ordinal());
                row.setElementAt(EstagioCheck.E4O.getImg(), EColAtend.SITREVATENDOIMG.ordinal());
            }
        }
        return result;
    }

    public int primeiroAtend(Vector<Vector<Object>> vatend, String dtatend) {
        int result = -1;
        Vector<Object> row = null;
        for (int i = 0; i < vatend.size(); i++) {
            row = vatend.elementAt(i);
            if (dtatend.equals(row.elementAt(EColAtend.DATAATENDO.ordinal()))) {
                result = i;
                break;
            }
        }
        return result;
    }

    public Integer locateSetor(int codemp, int codfilial, int codempae, int codfilialae, int codatend) {
        Integer result = null;
        ResultSet rs = null;
        int param = 1;
        PreparedStatement ps = null;
        StringBuilder sql = new StringBuilder();
        try {
            sql.append("select first 1 s.codsetat from atsetor s, atsetoratendente sa ");
            sql.append("where sa.codemp=s.codemp and sa.codfilial=s.codfilial and sa.codsetat=s.codsetat and ");
            sql.append("s.codemp=? and s.codfilial=? and ");
            sql.append("sa.codempae=? and sa.codfilialae=? and sa.codatend=? ");
            ps = getConn().prepareStatement(sql.toString());
            ps.setInt(param++, codemp);
            ps.setInt(param++, codfilial);
            ps.setInt(param++, codempae);
            ps.setInt(param++, codfilial);
            ps.setInt(param++, codatend);
            rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getInt("CODSETAT");
            }
            rs.close();
            ps.close();
            getConn().commit();
        } catch (SQLException e) {
            try {
                e.printStackTrace();
                getConn().rollback();
            } catch (Exception err) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
