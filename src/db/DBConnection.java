package db;

import model.ThroughputEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBConnection {

    private final String table;
    private static final String user = "CLOUD";
    private static final String password = "Dormant-2000";
    private static final String DB2url = "jdbc:db2://192.168.1.2:50000/NEWRELIC";


    /** Set the table name for applications */
    public  static DBConnection createApplication() {
        return new DBConnection("APPLICATIONDATA");
    }

    public DBConnection(String table) {
        this.table =  String.format("NRDATA.%s", table);
    }


    public Connection getConnection() throws IllegalAccessException,
            InstantiationException, ClassNotFoundException, SQLException {

        try {
            Class.forName("COM.ibm.db2os390.sqlj.jdbc.DB2SQLJDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Connecting to database...");
        Connection  connection = DriverManager.getConnection(DB2url, user, password);
        System.out.println( "From DAO, connection obtained " );
        return connection;
    }


    public List<ThroughputEntry> getThroughputEntriesInTimespan(String env, String app, Timestamp firstPeriod, Timestamp lastPeriod, Connection connection) throws Exception {
        List<ThroughputEntry> entries = new ArrayList<>();

        Statement statement = connection.createStatement();

        if (statement.execute("SELECT * FROM " + table + " WHERE " +
                "ENVIRONMENT = '" + env + "'" +
                " AND APPNAME = '" + app + "'"+
                " AND RETRIEVED >= '" + firstPeriod + "'" +
                " AND PERIOD_END <= '" + lastPeriod + "'")) {
            while (statement.getResultSet().next()) {
                entries.add(new ThroughputEntry(statement.getResultSet()));

            }
        } else {
            return null;
        }

        return entries;
    }
}
