package com.triplea.rolap.plugins.filter.PluginFilterExcelRE;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import junit.framework.JUnit4TestAdapter;
import static org.junit.Assert.*;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.triplea.rolap.plugins.IServer;
import com.triplea.rolap.plugins.filter.ExcelRE.*;

public class ExcelRETest {

    private static final org.apache.log4j.Logger _logger = org.apache.log4j.Logger.getLogger(ExcelRETest.class.getName());

    private final String _configFile = "plugins/PluginFilterExcelRE.xml";

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ExcelRETest.class);
    }

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        PropertyConfigurator.configure("log4j.properties");
    }

    @Before
    public void setUp() throws Exception {
        String configFileContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<plugin>" + "	<rules>" + "		<rule id=\"1\" name=\"rule1\">" + "			<description>desc of first rule</description>" + "			<file path=\"excel1.xls\"/>" + "			<parameters>" + "				<parameter>" + "					<connection_string value=\"localhost/demo\"/>" + "					<dimension name=\"dim1\"/>" + "					<default value=\"dim1_val\"/>" + "				</parameter>" + "				<parameter>" + "					<connection_string value=\"localhost/demo\"/>" + "					<dimension name=\"dim3\"/>" + "					<default value=\"dim3_val\"/>" + "				</parameter>" + "			</parameters>" + "			<auto_execution onread=\"0\" onwrite=\"1\">" + "				<database name=\"db1\"/>" + "				<cube name=\"cube1\"/>" + "				<dimensions>" + "					<dimension name=\"dim4\"/>" + "					<dimension name=\"dim5\"/>" + "				</dimensions>" + "			</auto_execution>" + "		</rule>" + "		<rule id=\"2\" name=\"rule2\">" + "			<description>desc of second rule</description>" + "			<file path=\"excel2.xls\"/>" + "			<parameters>" + "				<parameter>" + "					<connection_string value=\"localhost/demo\"/>" + "					<dimension name=\"dim2_1\"/>" + "					<default value=\"dim2_1_val\"/>" + "				</parameter>" + "				<parameter>" + "					<connection_string value=\"localhost/demo\"/>" + "					<dimension name=\"dim2_3\"/>" + "					<default value=\"dim2_3_val\"/>" + "				</parameter>" + "				<dimension name=\"dim2_3\"/>" + "			</parameters>" + "			<auto_execution onread=\"1\" onwrite=\"0\">" + "				<database name=\"db1\"/>" + "				<cube name=\"cube2\"/>" + "				<dimensions>" + "					<dimension name=\"dim2_4\"/>" + "					<dimension name=\"dim2_5\"/>" + "				</dimensions>" + "			</auto_execution>" + "		</rule>" + "	</rules>" + "	<packages>" + "		<package name=\"package1\">" + "			<contains>" + "				<rule id=\"1\"/>" + "				<rule id=\"2\"/>" + "				<rule id=\"unknown_id\"/>" + "			</contains>" + "		</package>" + "		<package name=\"package2\">" + "			<contains>" + "				<rule id=\"2\"/>" + "			</contains>" + "		</package>" + "	</packages>" + "</plugin>";
        File configFile = new File(this._configFile);
        configFile.delete();
        configFile.createNewFile();
        FileWriter writer = new FileWriter(configFile);
        writer.write(configFileContent);
        writer.flush();
        writer.close();
    }

    @After
    public void tearDown() throws Exception {
        this.removeConfigFile();
    }

    private void removeConfigFile() {
        File configFile = new File(this._configFile);
        configFile.delete();
    }

    @Test
    public final void testLoadParameters() {
        ExcelRE plugin = new ExcelRE();
        ArrayList<Rule> rules = plugin.getRules();
        assertEquals("Incorrect count of loaded rules", 2, rules.size());
        ArrayList<RulesPackage> packages = plugin.getRulesPackages();
        assertEquals("Incorrect count of loaded packages", 2, packages.size());
        Rule rule = rules.get(0);
        assertEquals("Incorrect ID", "1", rule.getId());
        assertEquals("Incorrect Name", "rule1", rule.getName());
        assertEquals("Incorrect Desc", "desc of first rule", rule.getDescription());
        assertEquals("Incorrect Database", "db1", rule.getDatabase());
        assertEquals("Incorrect CubeName", "cube1", rule.getCubeName());
        assertEquals("Incorrect FilePath", "excel1.xls", rule.getFilePath());
        ArrayList<RuleParameter> parameters = rule.getParameters();
        assertEquals("Incorrect count of Parameters", 2, parameters.size());
        RuleParameter parameter = parameters.get(0);
        assertEquals("Incorrect parameter's dimension", "dim1", parameter.getDimension());
        assertEquals("Incorrect parameter's connection", "localhost/demo", parameter.getConnectionString());
        assertEquals("Incorrect parameter's default value", "dim1_val", parameter.getDefaultValue());
        assertEquals("Incorrect autoExecute on read", false, rule.isExecuteOnRead());
        assertEquals("Incorrect autoExecute on write", true, rule.isExecuteOnWrite());
        ArrayList<String> dimensions = rule.getDimensionsForExecution();
        assertEquals("Incorrect count of dimensions", 2, dimensions.size());
        assertEquals("Incorrect dimension", "dim4", dimensions.get(0));
        assertEquals("Incorrect dimension", "dim5", dimensions.get(1));
        RulesPackage rulesPackage = packages.get(0);
        assertEquals("Incorrect package name", "package1", rulesPackage.getName());
        ArrayList<Rule> packageRules = rulesPackage.getRules();
        assertEquals("Incorrect count of rules in the package1", 2, packageRules.size());
    }

    @Test
    public final void testSaveParameters() {
        ExcelRE pluginBefore = new ExcelRE();
        this.removeConfigFile();
        ArrayList<Rule> rules = pluginBefore.getRules();
        rules.get(0).setId("new_id");
        rules.get(0).setName("new_name");
        rules.get(0).setDescription("new_desc");
        rules.get(0).setDatabase("db2");
        rules.get(0).setCubeName("new_cubename");
        rules.get(0).setFilePath("new_filepath");
        ArrayList<RuleParameter> parameters = new ArrayList<RuleParameter>();
        RuleParameter param = new RuleParameter();
        param.setDimension("new_paramdim_1");
        param.setConnectionString("connect1");
        param.setDefaultValue("default1");
        parameters.add(param);
        param = new RuleParameter();
        param.setDimension("new_paramdim_2");
        param.setConnectionString("connect2");
        param.setDefaultValue("default2");
        parameters.add(param);
        param = new RuleParameter();
        param.setDimension("new_paramdim_3");
        param.setConnectionString("connect3");
        param.setDefaultValue("default3");
        parameters.add(param);
        rules.get(0).setParameters(parameters);
        rules.get(0).setExecuteOnRead(!rules.get(0).isExecuteOnRead());
        rules.get(0).setExecuteOnWrite(!rules.get(0).isExecuteOnWrite());
        ArrayList<String> dimensions = new ArrayList<String>();
        dimensions.add("newdim_1");
        dimensions.add("newdim_2");
        dimensions.add("newdim_3");
        dimensions.add("newdim_4");
        rules.get(0).setDimensionsForExecution(dimensions);
        ArrayList<RulesPackage> rulePackages = pluginBefore.getRulesPackages();
        rulePackages.get(0).setName("new_package1");
        pluginBefore.saveParameters();
        ExcelRE pluginAfter = new ExcelRE();
        assertEquals("Amount of saved rules differ!", pluginBefore.getRules().size(), pluginAfter.getRules().size());
        for (int i = 0; i < pluginAfter.getRules().size(); i++) {
            assertEquals("Incorrect ID", pluginBefore.getRules().get(i).getId(), pluginAfter.getRules().get(i).getId());
            assertEquals("Incorrect names", pluginBefore.getRules().get(i).getName(), pluginAfter.getRules().get(i).getName());
            assertEquals("Incorrect desc", pluginBefore.getRules().get(i).getDescription(), pluginAfter.getRules().get(i).getDescription());
            assertEquals("Incorrect cube", pluginBefore.getRules().get(i).getCubeName(), pluginAfter.getRules().get(i).getCubeName());
            assertEquals("Incorrect database", pluginBefore.getRules().get(i).getDatabase(), pluginAfter.getRules().get(i).getDatabase());
            assertEquals("Incorrect names", pluginBefore.getRules().get(i).getFilePath(), pluginAfter.getRules().get(i).getFilePath());
            for (int j = 0; j < pluginAfter.getRules().get(i).getParameters().size(); j++) {
                assertEquals("Incorrect parameter connectString", pluginBefore.getRules().get(i).getParameters().get(j).getConnectionString(), pluginAfter.getRules().get(i).getParameters().get(j).getConnectionString());
                assertEquals("Incorrect parameter dimension", pluginBefore.getRules().get(i).getParameters().get(j).getDimension(), pluginAfter.getRules().get(i).getParameters().get(j).getDimension());
                assertEquals("Incorrect parameter default", pluginBefore.getRules().get(i).getParameters().get(j).getDefaultValue(), pluginAfter.getRules().get(i).getParameters().get(j).getDefaultValue());
            }
            assertEquals("Incorrect onRead", pluginBefore.getRules().get(i).isExecuteOnRead(), pluginAfter.getRules().get(i).isExecuteOnRead());
            assertEquals("Incorrect onWrite", pluginBefore.getRules().get(i).isExecuteOnWrite(), pluginAfter.getRules().get(i).isExecuteOnWrite());
        }
        assertEquals("Amount of saved packages differ!", pluginBefore.getRulesPackages().size(), pluginAfter.getRulesPackages().size());
        for (int i = 0; i < pluginAfter.getRulesPackages().size(); i++) {
            assertEquals("Incorrect names", pluginBefore.getRulesPackages().get(i).getName(), pluginAfter.getRulesPackages().get(i).getName());
            for (int ruleIndex = 0; ruleIndex < pluginAfter.getRulesPackages().get(ruleIndex).getRules().size(); ruleIndex++) {
                assertEquals("Incorrect rule ID", pluginBefore.getRulesPackages().get(ruleIndex).getRules().get(ruleIndex).getId(), pluginAfter.getRulesPackages().get(ruleIndex).getRules().get(ruleIndex).getId());
            }
        }
    }

    @Test
    public final void getPackageByUnknownName() {
        ExcelRE plugin = new ExcelRE();
        String name = "unknown_package";
        RulesPackage rulesPackage = plugin.getPackageByName(name);
        assertNull("Package doesn't have to be found", rulesPackage);
    }

    @Test
    public final void getPackageByName() {
        ExcelRE plugin = new ExcelRE();
        String name = "package1";
        RulesPackage rulesPackage = plugin.getPackageByName(name);
        assertNotNull("Package not found", rulesPackage);
    }

    @Test
    public final void getServerContext() {
        ExcelRE plugin = new ExcelRE("../");
        IServer serverContext = plugin.getServerContext();
        assertNotNull("No server context", serverContext);
    }
}
