import java.sql.*;
import java.lang.StringBuilder;
import java.io.*;
import java.util.*;

public class DataAccessObject {
    public final static String ALLELES = "alleles";
    public final static String TARGETSNPS = "targetsnps";
    public final static String CROSSSNPS = "crosssnps";
    public final static String HUMANSNPS = "humansnps";
    public final static String INDIVIDUALS = "individuals";
    public final static String POPULATIONS = "populations";
    public final static String STATS = "stats";  //for right now, this table only holds Delta DAF and FST

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

            e.printStackTrace(System.out);
        }
    }

    public void Connect() throws Exception {
        connection = null;
        //Register driver:
        Class.forName("org.sqlite.JDBC");
        //Connect to database
        String currentDirectory = System.getProperty("user.dir");
        //Determines location of database in file structure
        //System.out.println(currentDirectory);

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

    public void update(String... args) {

    }

    //Maybe we can combine this function and the ones above and below since they are similar?
    //add some keyword for which function to call for the value of insert
    public List<AlleleFrequency> getAlleleFrequency(String... args){
      String insert = "";
      List<AlleleFrequency> list = new ArrayList<AlleleFrequency>();
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          insert = buildAlleleFrequencyInsertStatement(args);
          //System.out.println(insert);
          ResultSet rs = statement.executeQuery(insert);
          while (rs.next()) {
            AlleleFrequency af = new AlleleFrequency(rs.getDouble("targetfreq"), rs.getDouble("crossfreq"), rs.getDouble("targetn"), rs.getDouble("crossn"));

            list.add(af);
          }
          statement.close();
          return list;
      } catch (Exception e){
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          System.out.println(insert);
          return null;
      }
    }

    public void commit() {
        try {
            connection.commit();
        } catch (Exception e) {
        }
    }

    public void close(){
      try {
        connection.close();
      } catch (Exception e){

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
                " allele1 TEXT NOT NULL," +
                " allele2 TEXT NOT NULL," +
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
                " targetfreq REAL NOT NULL," +
                " targetn REAL NOT NULL)"; //number of alleles sampled
    }

    private String makeCrossSNPTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + CROSSSNPS +
                " (rsid TEXT PRIMARY KEY NOT NULL," +
                " chr INT NOT NULL," +
                " pos INT NOT NULL," +
                " crossfreq REAL NOT NULL," +
                " crossn REAL NOT NULL)"; //number of alleles sampled
    }

    private String makeHumanSNPTableCreateStatement() {
        return "CREATE TABLE IF NOT EXISTS " + HUMANSNPS +
                " (rsid TEXT PRIMARY KEY NOT NULL," +
                " chr INT NOT NULL," +
                " pos INT NOT NULL," +
                " freq REAL NOT NULL," + //total frequency of all individuals sampled
                " targetfreq REAL NOT NULL," + //freq of target subpopulation
                " crossfreq REAL NOT NULL," + //freq of cross subpopulation
                " targetn REAL NOT NULL," + //number of target alleles sampled
                " crossn REAL NOT NULL)"; //number of cross alleles sampled
                //This table only handles one human file; do we need to inclue the ID
                //so we can potentially run multiple human files?
                //As the program is now, it can only run one/one set of files at a time...
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

    private String buildAlleleFrequencyInsertStatement(String... args){
      String SQL = new String("SELECT " + args[0]);
      for (int i = 1; i < 4; i++) {
        SQL = SQL + ", " + args[i];
      }
      SQL = SQL + " FROM " + args[4];
      if (args.length == 6) {
        SQL = SQL + ", " + args[5];
        SQL = SQL + " WHERE targetsnps.rsid = crosssnps.rsid";
      }
      //System.out.println("insert = " + SQL);
      return SQL;
    }

}
