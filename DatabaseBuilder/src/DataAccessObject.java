import java.sql.*;
import java.lang.StringBuilder;

public class DataAccessObject {
    public final static String ALLELES = "alleles";
    public final static String TARGETSNPS = "targetsnps";
    public final static String CROSSSNPS = "crosssnps";
    public final static String HUMANSNPS = "humansnps";
    public final static String INDIVIDUALS = "individuals";
    public final static String POPULATIONS = "populations";
    public final static String STATS = "stats";  //for right now, this table only holds Delta DAF

    private Connection connection;
    //Somewhere in the code we need to figure out where to commit the changes that are made in each function.
    //Also need to figure out where (and what...) to close stuff.
    public DataAccessObject() {
        try {
           Connect();
           CreateTables();
        } catch (Exception e) {
            System.err.println("error from DAO");
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
        System.out.println(currentDirectory);

        connection = DriverManager.getConnection("jdbc:sqlite:" + currentDirectory + "/DatabaseBuilder/src/db/SELECT.db");
        connection.setAutoCommit(false);
    }

    public void CreateTables() {
        try {
            if(connection == null)
                Connect();

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(makeAlleleTableCreateStatement());
            stmt.executeUpdate(makeCrossSNPTableCreateStatement());
            stmt.executeUpdate(makeHumanSNPTableCreateStatement());
            stmt.executeUpdate(makeTargetSNPTableCreateStatement());
            stmt.executeUpdate(makeIndividualTableCreateStatement());
            stmt.executeUpdate(makePopulationTableCreateStatement());
            stmt.executeUpdate(makeStatsTableCreateStatement());
            stmt.close();
        } catch (Exception e) {
            System.err.println("this happened in CreateTables");
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            //System.err.println( e.printStackTrace());
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

    //Maybe we can combine this function and the ones above and below since they are similar?
    //add some keyword for which function to call for the value of insert
    public void getDeltaDAF(String... args){
      String insert = "";
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          //Calculate Delta DAF and insert into the stats table
          insert = buildDAFInsertStatement(args);
          statement.executeUpdate(insert);
          statement.close();
      } catch (Exception e){
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          System.out.println(insert);
      }
    }

    public void getFst(String... args){
      String insert = "";
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          //Calculate Delta DAF and insert into the stats table
          insert = buildFSTInsertStatement(args);
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
                " FOREIGN KEY(rsid) REFERENCES " + HUMANSNPS + "(rsid));";
                //HUMANSNPS was used so that the program will compile, but we need to look
                //at which table should actually be referenced here.  We may
                //need two allele tables depending if we are working with humans or not
    }

    //We need to make sure that data storage types will not introduce roundoff error!
    private String makeTargetSNPTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + TARGETSNPS +
                " (rsid TEXT PRIMARY KEY NOT NULL," +
                " chr INT NOT NULL," +
                " pos INT NOT NULL," +
                " freq REAL NOT NULL," +
                " n REAL NOT NULL)";
    }

    private String makeCrossSNPTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + CROSSSNPS +
                " (rsid TEXT PRIMARY KEY NOT NULL," +
                " chr INT NOT NULL," +
                " pos INT NOT NULL," +
                " freq REAL NOT NULL," +
                " n REAL NOT NULL)";
    }

    private String makeHumanSNPTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + HUMANSNPS +
                " (rsid TEXT PRIMARY KEY NOT NULL," +
                " chr INT NOT NULL," +
                " pos INT NOT NULL," +
                " freq REAL NOT NULL," + //total frequency of all individuals sampled
                " target REAL NOT NULL," + //freq of target subpopulation
                " cross REAL NOT NULL," + //freq of cross subpopulation
                " n REAL NOT NULL)";
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

    //perhaps add another table or extend this one to hold intermediate calculations for FST, etc.
    //N so that we can reuse N to calculate D.  Also maybe q.
    private String makeStatsTableCreateStatement(){
      return "CREATE TABLE IF NOT EXISTS " + STATS +
              " (daf REAL NOT NULL," +
              " fst REAL)";
    }

    //This method and the one below need to be tested to insure it is calculating the correct values
    private String buildDAFInsertStatement(String... args){
      return "INSERT INTO " + STATS + " ((SELECT " + args[0] + " FROM " + args[1] +
              ") - ( SELECT " + args[2] + " FROM " + args[3] + "))";
    }

    //Is this (and above) a valid way to update all rows at once?
    //Would it be more optimized to get a result set, do the calculations in java,
    //and then update?  Rather than having a crazy long formula....
    private String buildFSTInsertStatement(String... args){
      return "UPDATE " + STATS + " SET fst = ((SELECT SQUARE(daf) FROM " +
       STATS + ") - (SELECT " + args[1] + " * (1 - " + args[1] +
        " ) * n / (n - 1) FROM " + args[2]  + ") - (SELECT " + args[3] +
        " * (1 - " + args[3] + " ) * n / (n - 1) FROM " + args[4]  + ")";
      //so far this is only implementing N
    }

}
