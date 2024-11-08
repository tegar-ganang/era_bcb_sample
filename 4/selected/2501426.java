package fr.soleil.sgad.mysql.templates;

public class SNAP_Template {

    public static void objects_gen() {
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "# \t\t\t OBJETS " + "(Schema " + fr.soleil.sgad.mysql.Constants.schema[2] + ")" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine);
        tables_gen();
    }

    private static void tables_gen() {
        String db = fr.soleil.sgad.mysql.Constants.schema[2];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "# \t\t\t Tables " + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("#" + fr.soleil.sgad.Constants.newLine + "# Database name: " + db + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("DROP DATABASE IF EXISTS " + db + ";" + fr.soleil.sgad.Constants.newLine + "CREATE DATABASE " + db + ";" + fr.soleil.sgad.Constants.newLine + "USE " + db + ";" + fr.soleil.sgad.Constants.newLine);
        tableAST_gen();
        tableContext_gen();
        tableList_gen();
        tableSnapshots_gen();
        table_im_1val();
        table_im_2val();
        table_sp_1val();
        table_sp_2val();
        table_sc_num_1val();
        table_sc_num_2val();
        table_sc_str_1val();
        table_sc_str_2val();
    }

    private static void tableContext_gen() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[1];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_context` smallint(6) NOT NULL auto_increment," + fr.soleil.sgad.Constants.newLine + "\t" + "`time` date NOT NULL default '0000-00-00'," + fr.soleil.sgad.Constants.newLine + "\t" + "`name` varchar(128) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`author` varchar(64) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`reason` longtext NOT NULL," + fr.soleil.sgad.Constants.newLine + "\t" + "`description` longtext NOT NULL," + fr.soleil.sgad.Constants.newLine + "PRIMARY KEY  (`id_context`)," + fr.soleil.sgad.Constants.newLine + "UNIQUE KEY `id_context_2` (`id_context`)," + fr.soleil.sgad.Constants.newLine + "KEY `id_context` (`id_context`)" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " AUTO_INCREMENT=1 ;" + fr.soleil.sgad.Constants.newLine);
    }

    private static void tableList_gen() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[2];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_context` smallint(6) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` smallint(6) NOT NULL default '0'" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + ";" + fr.soleil.sgad.Constants.newLine);
    }

    private static void tableSnapshots_gen() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[3];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` smallint(6) NOT NULL auto_increment," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_context` mediumint(9) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`time` timestamp(14) NOT NULL DEFAULT '2005-07-01 0:00:00'," + fr.soleil.sgad.Constants.newLine + "\t" + "`snap_comment` varchar(255) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "PRIMARY KEY  (`id_snap`)," + fr.soleil.sgad.Constants.newLine + "KEY `id_snap` (`id_snap`)" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " AUTO_INCREMENT=1 ;" + fr.soleil.sgad.Constants.newLine);
    }

    private static void tableAST_gen() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[0];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`ID` smallint(5) NOT NULL auto_increment," + fr.soleil.sgad.Constants.newLine + "\t" + "`time` datetime default NULL," + fr.soleil.sgad.Constants.newLine + "\t" + "`full_name` varchar(200) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`device` varchar(150) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`domain` varchar(35) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`family` varchar(35) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`member` varchar(35) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`att_name` varchar(50) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`data_type` tinyint(1) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`data_format` tinyint(1) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`writable` tinyint(1) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`max_dim_x` smallint(6) unsigned NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`max_dim_y` smallint(6) unsigned NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`levelg` tinyint(1) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`facility` varchar(45) NOT NULL default ''," + fr.soleil.sgad.Constants.newLine + "\t" + "`archivable` tinyint(1) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`substitute` smallint(9) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "PRIMARY KEY  (`ID`)," + fr.soleil.sgad.Constants.newLine + "KEY `ID` (`ID`)," + fr.soleil.sgad.Constants.newLine + "KEY `ID_2` (`ID`)" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Attribute Snap Table' AUTO_INCREMENT=1 ;" + fr.soleil.sgad.Constants.newLine);
    }

    private static void table_im_1val() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[4];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`dim_x` SMALLINT( 6 ) NOT NULL," + fr.soleil.sgad.Constants.newLine + "\t" + "`dim_y` SMALLINT( 6 ) NOT NULL," + fr.soleil.sgad.Constants.newLine + "\t" + "`value` longblob" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Table for Image, Read only attributes';" + fr.soleil.sgad.Constants.newLine);
    }

    private static void table_im_2val() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[5];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`dim_x` SMALLINT( 6 ) NOT NULL," + fr.soleil.sgad.Constants.newLine + "\t" + "`dim_y` SMALLINT( 6 ) NOT NULL," + fr.soleil.sgad.Constants.newLine + "\t" + "`read_value` longblob," + fr.soleil.sgad.Constants.newLine + "\t" + "`write_value` longblob" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Table for Image, Write only attributes';" + fr.soleil.sgad.Constants.newLine);
    }

    private static void table_sc_num_1val() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[8];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`value` double NOT NULL default '0'" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Table for Scalar, Read only attributes';" + fr.soleil.sgad.Constants.newLine);
    }

    private static void table_sc_num_2val() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[9];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`read_value` double NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`write_value` double NOT NULL default '0'" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Table for Scalar, Read/Write + Read with Write attributes';" + fr.soleil.sgad.Constants.newLine);
    }

    private static void table_sc_str_1val() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[10];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`value` varchar(255)" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Table for String, Read only attributes';" + fr.soleil.sgad.Constants.newLine);
    }

    private static void table_sc_str_2val() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[11];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`read_value` varchar(255)," + fr.soleil.sgad.Constants.newLine + "\t" + "`write_value` varchar(255)" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Table for String, Read/Write + Read with Write attributes';" + fr.soleil.sgad.Constants.newLine);
    }

    private static void table_sp_1val() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[6];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`dim_x` SMALLINT( 6 ) NOT NULL," + fr.soleil.sgad.Constants.newLine + "\t" + "`value` blob" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Table for Spectrum, Read only attributes';" + fr.soleil.sgad.Constants.newLine);
    }

    private static void table_sp_2val() {
        String table = fr.soleil.sgad.mysql.Constants.snapObjects[7];
        fr.soleil.sgad.mysql.Generator.add_object(fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.mysql.Constants.separator + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + "# Table structure :  `" + table + "`" + fr.soleil.sgad.Constants.newLine + "#" + fr.soleil.sgad.Constants.newLine + fr.soleil.sgad.Constants.newLine);
        fr.soleil.sgad.mysql.Generator.add_object("CREATE TABLE `" + table + "` (" + fr.soleil.sgad.Constants.newLine + "\t" + "`id_snap` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`id_att` int(11) NOT NULL default '0'," + fr.soleil.sgad.Constants.newLine + "\t" + "`dim_x` SMALLINT( 6 ) NOT NULL," + fr.soleil.sgad.Constants.newLine + "\t" + "`read_value` blob," + fr.soleil.sgad.Constants.newLine + "\t" + "`write_value` blob" + fr.soleil.sgad.Constants.newLine + ") TYPE=" + fr.soleil.sgad.mysql.Constants.storage_engine + " COMMENT='Table for Spectrum, Read/Write attributes';" + fr.soleil.sgad.Constants.newLine);
    }
}
