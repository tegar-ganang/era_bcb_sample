package research.ui.handlers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import research.dao.hibernate.HibernateUtil;
import research.domain.Application;
import research.domain.Production;
import research.domain.Research;
import research.domain.Sample;
import research.domain.Strength;
import research.domain.StrengthValue;
import research.domain.Test;
import research.domain.TestParameter;
import research.domain.TestParameterValue;
import research.domain.TestResult;
import research.domain.Viscosity;
import research.domain.ViscosityValue;
import research.entity.Entity;
import research.entity.EntityType;
import research.model.ResearchHierarchy;
import research.model.ResearchProductionHierarchy;
import research.model.SampleStrengthHierarchy;
import research.model.SampleTestHierarchy;
import research.model.SampleViscosityHierarchy;
import research.model.StrengthHierarchy;
import research.model.TestHierarchy;
import research.model.TestParameterHierarchy;
import research.model.ViscosityHierarchy;
import research.persistence.PersistenceManager;
import com.jasperassistant.designer.viewer.ViewerApp;

public class ReportEntityHandler extends AbstractHandler {

    private Map<String, Object> getParametersMap(Entity obj) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        if (obj instanceof Application) {
            parameters.put("id", ((Application) obj).getId());
        } else if (obj instanceof Research) {
            parameters.put("id", ((Research) obj).getId());
        } else if (obj instanceof Production) {
            parameters.put("id", ((Production) obj).getId());
        } else if (obj instanceof Viscosity) {
            parameters.put("id", ((Viscosity) obj).getId());
        } else if (obj instanceof Strength) {
            parameters.put("id", ((Strength) obj).getId());
        } else if (obj instanceof Test) {
            parameters.put("id", ((Test) obj).getId());
        }
        return parameters;
    }

    private JasperPrint getEntityReport(Entity entity, String report) {
        Map<String, Object> parameters = entity != null ? getParametersMap(entity) : null;
        JasperPrint print = null;
        if (report != null && !report.isEmpty()) {
            try {
                print = JasperFillManager.fillReport((JasperReport) JRLoader.loadObjectFromLocation(report), parameters, getConnection());
            } catch (JRException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return print;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
        if (selection != null & selection instanceof IStructuredSelection) {
            IStructuredSelection strucSelection = (IStructuredSelection) selection;
            for (Iterator<Object> iterator = strucSelection.iterator(); iterator.hasNext(); ) {
                Object element = (Object) iterator.next();
                if (element instanceof Entity) {
                    Entity entity = (Entity) element;
                    if (entity instanceof Research) {
                        processResarchReport((Research) entity, event);
                    } else {
                        JasperPrint print = getEntityReport(entity, entity.getType().getReport());
                        if (print != null) {
                            ViewerApp app = new ViewerApp();
                            app.getReportViewer().setDocument(print);
                            app.open();
                        }
                    }
                }
            }
        }
        return null;
    }

    private void processResarchReport(Research res, ExecutionEvent event) {
        SafeSaveDialog dlg = new SafeSaveDialog(HandlerUtil.getActiveShell(event));
        dlg.setFilterExtensions(new String[] { "*.pdf" });
        String file = dlg.open();
        if (file == null) return;
        List<JasperPrint> jasperPrintList = new ArrayList<JasperPrint>();
        jasperPrintList.add(getEntityReport(res, res.getType().getReport()));
        Application app = res.getApplication();
        if (app != null) jasperPrintList.add(getEntityReport(app, app.getType().getReport()));
        jasperPrintList.add(getEntityReport(res, "formula.jasper"));
        List<Entity> prod = PersistenceManager.getInstance().getDataSource().getChildren(res, new ResearchProductionHierarchy());
        List<Entity> viscosities = new ArrayList<Entity>();
        List<Entity> strengths = new ArrayList<Entity>();
        List<Entity> tests = new ArrayList<Entity>();
        for (Entity entity : prod) {
            jasperPrintList.add(getEntityReport(entity, entity.getType().getReport()));
            jasperPrintList.add(getEntityReport(entity, "parameter.jasper"));
            for (Entity ent : PersistenceManager.getInstance().getDataSource().getChildren(entity, new ResearchHierarchy())) {
                for (Entity vis : PersistenceManager.getInstance().getDataSource().getChildren(ent, new SampleViscosityHierarchy())) {
                    viscosities.add(vis);
                }
                for (Entity vis : PersistenceManager.getInstance().getDataSource().getChildren(ent, new SampleStrengthHierarchy())) {
                    strengths.add(vis);
                }
                for (Entity test : PersistenceManager.getInstance().getDataSource().getChildren(ent, new SampleTestHierarchy())) {
                    tests.add(test);
                }
            }
        }
        for (Entity vis : viscosities) {
            jasperPrintList.add(getEntityReport(vis, vis.getType().getReport()));
        }
        for (Entity str : strengths) {
            jasperPrintList.add(getEntityReport(str, str.getType().getReport()));
        }
        for (Entity test : tests) {
            jasperPrintList.add(getEntityReport(test, test.getType().getReport()));
        }
        prepareResults(res);
        jasperPrintList.add(getEntityReport(null, EntityType.TestResult.getReport()));
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setParameter(JRExporterParameter.JASPER_PRINT_LIST, jasperPrintList);
        exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, file);
        try {
            exporter.exportReport();
        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    private void clearResults() {
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            s.beginTransaction();
            s.getNamedQuery("TestResultTruncate").executeUpdate();
            s.getTransaction().commit();
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            s.getTransaction().rollback();
        }
    }

    private void prepareResults(Research research) {
        clearResults();
        List<Entity> prod = PersistenceManager.getInstance().getDataSource().getChildren(research, new ResearchProductionHierarchy());
        List<TestResult> results = new ArrayList<TestResult>();
        for (Entity production : prod) {
            for (Entity sample : PersistenceManager.getInstance().getDataSource().getChildren(production, new ResearchHierarchy())) {
                for (Entity vis : PersistenceManager.getInstance().getDataSource().getChildren(sample, new SampleViscosityHierarchy())) {
                    for (Entity vis_val : PersistenceManager.getInstance().getDataSource().getChildren(vis, new ViscosityHierarchy())) {
                        TestResult res = TestResult.getNew();
                        res.setSample(((Sample) sample).getName());
                        res.setParameter("Ударная вязкость: En, кДж/м^2");
                        res.setValue(((ViscosityValue) vis_val).getEn());
                        results.add(res);
                    }
                }
                for (Entity str : PersistenceManager.getInstance().getDataSource().getChildren(sample, new SampleStrengthHierarchy())) {
                    for (Entity str_val : PersistenceManager.getInstance().getDataSource().getChildren(str, new StrengthHierarchy())) {
                        TestResult res = TestResult.getNew();
                        res.setSample(((Sample) sample).getName());
                        res.setParameter("Прочность при растяжении, МПа");
                        res.setValue(((StrengthValue) str_val).getStrengthValue());
                        results.add(res);
                    }
                }
                for (Entity test : PersistenceManager.getInstance().getDataSource().getChildren(sample, new SampleTestHierarchy())) {
                    for (Entity test_val : PersistenceManager.getInstance().getDataSource().getChildren(test, new TestHierarchy())) {
                        TestParameter par = (TestParameter) PersistenceManager.getInstance().getDataSource().getParent(test_val, new TestParameterHierarchy());
                        if (!par.isReported()) continue;
                        TestResult res = TestResult.getNew();
                        res.setSample(((Sample) sample).getName());
                        res.setParameter(par.getName() + ", " + par.getUnits());
                        res.setValue(((TestParameterValue) test_val).getValue());
                        results.add(res);
                    }
                }
            }
        }
        for (TestResult testResult : results) {
            PersistenceManager.getInstance().persist(testResult);
        }
    }

    private static Connection getConnection() throws ClassNotFoundException, SQLException {
        return HibernateUtil.getConnection();
    }
}
