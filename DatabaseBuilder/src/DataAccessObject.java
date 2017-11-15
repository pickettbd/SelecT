import java.sql.*;
import java.lang.StringBuilder;

public class DataAccessObject {
    public final static String ALLELES = "alleles";
    public final static String SNPS = "snps";
    public final static String INDIVIDUALS = "individuals";
    public final static String POPULATIONS = "populations";

    private Connection connection;

    public DataAccessObject() {
        try {
           Connect();
           CreateTables();
        } catch (Exception e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }

    public void Connect() throws Exception {
        connection = null;
        //Register driver:
        Class.forName("org.sqlite.JDBC");
        //Connect to database
        String currentDirectory = System.getProperty("user.dir");
        connection = DriverManager.getConnection("jdbc:sqlite:" + currentDirectory + "/DatabaseBuilder/src/db/SELECT.db");
        connection.setAutoCommit(false);
    }

    public void CreateTables() {
        try {
            if(connection == null)
                Connect();

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(makeAlleleTableCreateStatement());
            stmt.executeUpdate(makeSNPTableCreateStatement());
            stmt.executeUpdate(makeIndividualTableCreateStatement());
            stmt.executeUpdate(makePopulationTableCreateStatement());
            stmt.close();
        } catch (Exception e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }

    public void insert(String... args) {
        String insert = "";
        try {
            if(connection == null)
                Connect();

            insert = buildInsertStatement(args);
            Statement statement = connection.createStatement();
            statement.executeUpdate(insert);
            statement.close();
        } catch (Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.out.println(insert);
        }
    }

    public void commit() {
        try {
            connection.commit();
        } catch (Exception e) {
        }
    }

    private String buildInsertStatement(String... args) {
        StringBuilder insert = new StringBuilder("INSERT INTO ");
        insert.append(args[0]);
        /*switch(args[0]){
            case ALLELES: insert.append(alleleColumns()); break;
            case POPULATIONS: insert.append(populationColumns()); break;
            default: break;
        }*/
        insert.append(" VALUES('");
        insert.append(args[1]);
        for(int i = 2; i < args.length; i++)
            insert.append("', '" + args[i]);
        insert.append("');");
        return insert.toString();
    }

    private String makeAlleleTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + ALLELES +
                " (rsid TEXT NOT NULL," +
                " individualid TEXT NOT NULL," +
                " allele TEXT NOT NULL," +
                " FOREIGN KEY(rsid) REFERENCES " + SNPS + "(rsid));";
    }

    private String makeSNPTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + SNPS +
                " (rsid TEXT PRIMARY KEY NOT NULL," +
                " chr INT NOT NULL," +
                " pos INT NOT NULL," +
                " freq REAL NOT NULL)";
    }

    private String makeIndividualTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + INDIVIDUALS +
                " (individualid TEXT PRIMARY KEY NOT NULL," +
                " populationid TEXT NOT NULL," +
                " FOREIGN KEY(populationid) REFERENCES " + POPULATIONS + "(populationid))";
    }

    private String makePopulationTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + POPULATIONS +
                " (populationid TEXT PRIMARY KEY NOT NULL," +
                " description TEXT)";
    }
}
