import java.sql.*;
import java.lang.StringBuilder;
import java.util.ArrayList;

public class DataAccessObject {
    public final static String SNPS = "snps";
    public final static String[] SNPS_COLUMNS = {"rsid", "chr", "pos", "freq"};
    public final static String ALLELES = "alleles";
    public final static String[] ALLELES_COLUMNS = {SNPS_COLUMNS[0], "individualid", "allele_number", "allele"};
    public final static String POPULATIONS = "populations";
    public static final String[] POPULATIONS_COLUMNS = {"populationid", "description"};
    public final static String INDIVIDUALS = "individuals";
    public final static String[] INDIVIDUALS_COLUMNS = {"individualid", POPULATIONS_COLUMNS[0]};

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
        //Determines location of database in file structure
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

    // Modify this so that it only gets results within a given window and returns something more useful than a ResultSet
    public ResultSet getSurroundingPositives(String rsid) {
        String getStatement = String.format("SELECT * FROM %1$s " +
                "WHERE %2$s = 1;",
                ALLELES,
                ALLELES_COLUMNS[2]);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(getStatement);
            return rs;
        } catch (SQLException e) {

        }
        return null;
    }

    public ArrayList<SNP> getSNPs() {
        ArrayList<SNP> snps = new ArrayList<>();

        String statement = String.format("SELECT * FROM %1$s;",
                SNPS);

        return snps;
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
        return "CREATE TABLE IF NOT EXISTS " + ALLELES + " (" +
                ALLELES_COLUMNS[0] + " TEXT NOT NULL, " +
                ALLELES_COLUMNS[1] + " TEXT NOT NULL, " +
                ALLELES_COLUMNS[2] + " INTEGER NOT NULL, " +
                ALLELES_COLUMNS[3] + " INTEGER NOT NULL, " +
                " FOREIGN KEY(" + ALLELES_COLUMNS[0] + ") REFERENCES " +
                SNPS + "(" + SNPS_COLUMNS[0] + "));";
    }

    private String makeSNPTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + SNPS + " (" +
                SNPS_COLUMNS[0] + " TEXT PRIMARY KEY NOT NULL, " +
                SNPS_COLUMNS[1] + " INT NOT NULL, " +
                SNPS_COLUMNS[2] + " INT NOT NULL, " +
                SNPS_COLUMNS[3] + " REAL NOT NULL);";
    }

    private String makeIndividualTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + INDIVIDUALS + " (" +
                INDIVIDUALS_COLUMNS[0] + " TEXT PRIMARY KEY NOT NULL, " +
                INDIVIDUALS_COLUMNS[1] + " TEXT NOT NULL, " +
                " FOREIGN KEY(" + INDIVIDUALS_COLUMNS[1] + ") REFERENCES " +
                POPULATIONS + "(" + POPULATIONS_COLUMNS[0] + "));";
    }

    private String makePopulationTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + POPULATIONS + " (" +
                POPULATIONS_COLUMNS[0] + " TEXT PRIMARY KEY NOT NULL, " +
                POPULATIONS_COLUMNS[1] + " TEXT);";
    }
}
