package net.sourceforge.solexatools.dao.hibernate;

import net.sourceforge.solexatools.*;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import net.sourceforge.solexatools.dao.LaneDAO;
import net.sourceforge.solexatools.model.Lane;
import net.sourceforge.solexatools.model.Run;
import net.sourceforge.solexatools.model.Experiment;
import net.sourceforge.solexatools.model.ExperimentType;
import net.sourceforge.solexatools.model.Organism;
import net.sourceforge.solexatools.model.Registration;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SQLQuery;
import org.hibernate.Query;
import java.sql.SQLException;

public class LaneDAOHibernate extends HibernateDaoSupport implements LaneDAO {

    public LaneDAOHibernate() {
        super();
    }

    /**
	 * Inserts an instance of Lane into the database.
	 */
    public void insert(Experiment exp, Lane lane) {
        this.getHibernateTemplate().save(lane);
    }

    /**
	 * Updates an instance of Lane in the database.
	 */
    public void update(Lane lane) {
        this.getHibernateTemplate().update(lane);
    }

    public void refresh(Lane lane) {
        this.getHibernateTemplate().refresh(lane);
    }

    public void merge(Lane nu) {
        Integer id = nu.getLaneId();
        Lane old = null;
        if (id != null) {
            old = findByID(id);
            if (old == null) {
                nu.setLaneId(null);
            }
        }
        if (old == null) {
            Session ssn = this.getSession(false);
            Query sql = null;
            Transaction tx = ssn.getTransaction();
            if (tx == null || !tx.isActive()) tx = ssn.beginTransaction();
            {
                sql = ssn.createSQLQuery("insert into lane (" + "	name, organism, description, tags, regions, skip," + "	sample_name, sample_code, sample_type," + "	real_experiment_id, experiment_id" + ") values (" + "	:name," + "	:organism," + "	:description," + "	:tags," + "	:regions," + "	:skip," + "	:sample_name," + "	:sample_code," + "	:sample_type," + "	:real_experiment_id," + "	:experiment_id " + ")");
                sql.setParameter("name", nu.getName());
                sql.setParameter("organism", nu.getOrganism());
                sql.setParameter("description", nu.getDescription());
                sql.setParameter("tags", nu.getTags());
                sql.setParameter("regions", nu.getRegions());
                sql.setParameter("skip", nu.getSkip());
                sql.setParameter("sample_name", nu.getSampleName());
                sql.setParameter("sample_code", nu.getSampleCode());
                sql.setParameter("sample_type", nu.getSampleType());
                Integer runId = null;
                Run run = nu.getRun();
                if (run != null) runId = run.getRunId();
                Integer expId = null;
                Experiment experiment = nu.getExperiment();
                if (experiment != null) expId = experiment.getExperimentId();
                sql.setParameter("experiment_id", runId, Hibernate.INTEGER);
                sql.setParameter("real_experiment_id", expId, Hibernate.INTEGER);
            }
            if (sql.executeUpdate() != 1) {
                tx.rollback();
            } else {
                tx.commit();
            }
            if (!tx.isActive()) tx.begin();
        } else {
            Session ssn = this.getSession(false);
            Query sql = null;
            Transaction tx = ssn.getTransaction();
            if (tx == null || !tx.isActive()) tx = ssn.beginTransaction();
            {
                sql = ssn.createSQLQuery("update	lane" + "	set	name			= :name," + "		organism		= :organism," + "		description		= :description," + "		tags			= :tags," + "		regions			= :regions," + "		skip			= :skip," + "		sample_name		= :sample_name," + "		sample_code		= :sample_code," + "		sample_type		= :sample_type," + "		experiment_id	= :experiment_id " + "where lane_id			= :lane_id");
                sql.setParameter("name", nu.getName());
                sql.setParameter("organism", nu.getOrganism());
                sql.setParameter("description", nu.getDescription());
                sql.setParameter("tags", nu.getTags());
                sql.setParameter("regions", nu.getRegions());
                sql.setParameter("skip", nu.getSkip());
                sql.setParameter("sample_name", nu.getSampleName());
                sql.setParameter("sample_code", nu.getSampleCode());
                sql.setParameter("sample_type", nu.getSampleType());
                Integer runId = null;
                Run run = nu.getRun();
                if (run != null) runId = run.getRunId();
                sql.setParameter("experiment_id", runId, Hibernate.INTEGER);
                sql.setParameter("lane_id", nu.getLaneId());
            }
            if (sql.executeUpdate() != 1) {
                tx.rollback();
            } else {
                tx.commit();
            }
            if (!tx.isActive()) tx.begin();
        }
    }

    public void merge(List<Lane> lanes) {
    }

    /**
	 * Finds an instance of Lane in the database by the Lane
	 * emailAddress.
	 *
	 * @return Lane or null if not found
	 */
    public Lane findByName(String name) {
        String query = "from lane as lane where lane.name = ?";
        Lane lane = null;
        Object[] parameters = { name };
        List list = this.getHibernateTemplate().find(query, parameters);
        if (list.size() > 0) {
            lane = (Lane) list.get(0);
        }
        return lane;
    }

    public Lane findByID(Integer id) {
        String query = "from Lane as lane where lane.laneId = ?";
        Lane lane = null;
        Object[] parameters = { id };
        List list = this.getHibernateTemplate().find(query, parameters);
        if (list.size() > 0) {
            lane = (Lane) list.get(0);
        }
        return lane;
    }

    public List<Lane> listUnassigned(Registration registration) {
        if (registration == null || (!registration.isTechnician() && !registration.isLIMSAdmin())) return new ArrayList<Lane>();
        String query = "from Lane as lane where run is null order by createTimestamp desc";
        List list = this.getHibernateTemplate().find(query);
        return list;
    }

    public List<Organism> listOrganisms() {
        return this.getSession().createCriteria(Organism.class).list();
    }

    public List<ExperimentType> listExperimentTypes() {
        return this.getSession().createCriteria(ExperimentType.class).list();
    }
}
