import java.sql.*;
import java.lang.StringBuilder;
import java.io.*;
import java.util.*;

//clean up functions so less repetition!!!!!
public class DataAccessObject {
    public final static String ALLELES = "alleles";
    public final static String TARGETSNPS = "targetsnps";
    public final static String CROSSSNPS = "crosssnps";
    public final static String HUMANSNPS = "humansnps";
    public final static String STATS = "stats";
    public final static String STANDARDIZATIONSTATS = "standardization_stats";

    private static List<String> individualIDs;
    private Connection connection;
    //Somewhere in the code we need to figure out where to commit the changes that are made in each function.
    //Also need to figure out where (and what...) to close stuff.
    public DataAccessObject() {
        try {
           Connect();
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

    public void CreateTables(List<String> individualIDs) {
      //System.out.println("createTables called");
        try {
            if(connection == null)
                Connect();

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(makeAlleleTableCreateStatement(individualIDs));
            stmt.executeUpdate(makeCrossSNPTableCreateStatement());
            stmt.executeUpdate(makeHumanSNPTableCreateStatement());
            stmt.executeUpdate(makeTargetSNPTableCreateStatement());
            stmt.executeUpdate(makeStatsTableCreateStatement());
            stmt.executeUpdate(makeStandardizationStatsTableCreateStatement());
            stmt.close();
        } catch (Exception e) {
            System.err.println("this happened in CreateTables");
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            e.printStackTrace();
        }
    }

    public void insert(String... args) {
        String insert = "";
        insert = buildInsertStatement(args);
        executeUpdate(insert);
    }

    public void updateDB(String table, String column, Double value, int row) {
        String insert = "UPDATE " + table +
        " SET " + column + " = '" + String.valueOf(value) +
        "' WHERE rsid = (SELECT rsid FROM alleles WHERE row = '" + String.valueOf(row) + "')";
        executeUpdate(insert);
    }

    public boolean freqHasStats(String frequency){
      String insert = "SELECT COUNT(freq) AS rowcount FROM " + STANDARDIZATIONSTATS + " WHERE freq = '" + frequency + "';";
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          //System.out.println(insert);
          ResultSet rs = statement.executeQuery(insert);
          int result = rs.getInt("rowcount");
          statement.close();
          if (result == 1) {
            return true;
          }
          else
            return false;
        } catch (Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.out.println(insert);
            return false; //Is this a valid solution?
        }
    }

    public void insertOneValue(String table, String column, String value){
      String insert = "INSERT INTO " + table +
      " ( " + column + " ) VALUES ( '" + value + "'); ";
      executeUpdate(insert);
    }

    private void executeUpdate(String insert){
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          statement.executeUpdate(insert);
          statement.close();
        } catch (Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.out.println(insert);
        }
    }

    public int getDBLength(){
      String insert = "SELECT COUNT(row) AS rowcount FROM " + ALLELES;
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          //System.out.println(insert);
          ResultSet rs = statement.executeQuery(insert);
          int length = rs.getInt("rowcount");
          statement.close();
          return length;
      } catch (Exception e){
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          System.out.println(insert);
          return 0;
      }
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

    public List<Double> getUnstandardizedIHSvaluesFromSNPFrequency(String freq){
      String insert = "SELECT unstandardizedIHS FROM " + STATS + " WHERE stats.rsid IN (SELECT rsid FROM humansnps WHERE freq = '" + freq + "');";
      List<Double> list = new ArrayList<Double>();
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          //System.out.println(insert);
          ResultSet rs = statement.executeQuery(insert);
          while (rs.next()) {
            Double unstandardizedIHS = rs.getDouble("unstandardizedIHS");
            list.add(unstandardizedIHS);
          }
          statement.close();
          return list;
      } catch (Exception e){
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          System.out.println(insert);
          return null;
      }
    }

    public String getFreq(int i){
      String insert = "SELECT freq FROM " + HUMANSNPS + " WHERE rsid = (SELECT rsid FROM alleles WHERE row = '" + String.valueOf(i) + "');";
      return getAString(insert, "freq");
    }

    public Double getMean(String freq){ //Rewrite to return a string!!!!!!!
      String insert = "SELECT mean FROM " + STANDARDIZATIONSTATS + " WHERE freq = '" + freq + "';";
      return getADouble(insert, "mean");
    }

    public Double getStandardDeviation(String freq){
      String insert = "SELECT standardDev FROM " + STANDARDIZATIONSTATS + " WHERE freq = '" + freq + "';";
      return getADouble(insert, "standardDev");
    }

    public Double getUnstandardizedIHS(int i){
      String insert = "SELECT unstandardizedIHS FROM " + STATS + " WHERE rsid = (SELECT rsid FROM alleles WHERE row = '" + String.valueOf(i) + "');";
      return getADouble(insert, "unstandardizedIHS");
    }

    private Double getADouble(String insert, String key){
      try {
          Double freq = 0.0;
          if(connection == null)
              Connect();
          Statement statement = connection.createStatement();
          //System.out.println(insert);
          ResultSet rs = statement.executeQuery(insert);
          while (rs.next()) {
            freq = rs.getDouble(key);
          }
          statement.close();
          return freq;
      } catch (Exception e){
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          System.out.println(insert);
          return null;
      }
    }


    private String getAString(String insert, String key){
      try {
          String freq = "";
          if(connection == null)
              Connect();
          Statement statement = connection.createStatement();
          //System.out.println(insert);
          ResultSet rs = statement.executeQuery(insert);
          while (rs.next()) {
            freq = rs.getString(key);
          }
          statement.close();
          return freq;
      } catch (Exception e){
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          System.out.println(insert);
          return null;
      }
    }

    public List<String> getRSIDs(){
      String request = "SELECT rsid FROM alleles";
      //is this the most efficient list type?
      List<String> rsids = new ArrayList<String>();
      try{
        if (connection == null)
          Connect();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(request);

        while (rs.next()){
          rsids.add(rs.getString("rsid"));
        }
        statement.close();
      }
      catch (Exception e){
        e.printStackTrace();
      }
      return rsids;
    }

    public List<Double> getStat(String statType){
      String insert = "SELECT " + statType + " FROM " + STATS + ";";
      return getListOfDoubles(insert, statType);
    }

    private List<Double> getListOfDoubles(String insert, String key){
      List<Double> answers = new ArrayList<>();
      try{
        if (connection == null)
          Connect();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(insert);

        while (rs.next()){
          answers.add(rs.getDouble(key));
        }
        statement.close();
      }
      catch (Exception e){
        e.printStackTrace();
      }
      return answers;
    }

    public List<Integer> getAlleleRow(int i){
      //System.out.println(individualIDs.toString());
      String request = "SELECT * FROM " + ALLELES + " WHERE rsid = (SELECT rsid FROM alleles WHERE row = '" + String.valueOf(i) + "');";
      List<Integer> alleles = new ArrayList<Integer>();
      try{
        if (connection == null)
          Connect();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(request);

        for (int j = 0; j < individualIDs.size(); j++){
          alleles.add(rs.getInt(individualIDs.get(j)));
        }
        statement.close();
      }
      catch (Exception e){
        e.printStackTrace();
      }
      return alleles;
    }

    public List<Integer> getAlleleColumn(String individualId, String whereClause){
      String request = "SELECT " + individualId + " FROM " + ALLELES + " " + whereClause + ";";
      //System.out.println(request);
      List<Integer> alleles = new ArrayList<Integer>();
      try{
        if (connection == null)
          Connect();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(request);

        while (rs.next()){
          alleles.add(rs.getInt(individualId));
        }
        statement.close();
      }
      catch (Exception e){
        e.printStackTrace();
      }
      return alleles;
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

    public int getBasePairPosition(int i){
      String query = "SELECT pos FROM humansnps h INNER JOIN alleles a ON h.rsid = a.rsid WHERE row = " + String.valueOf(i) + ";";
      int position = 0;
      try{
        if (connection == null)
          Connect();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);

        position = rs.getInt("pos");
        statement.close();
      }
      catch (Exception e){
        e.printStackTrace();
      }
      return position;
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

    private String makeAlleleTableCreateStatement(List<String> individualIDs) {
        StringBuilder sb = new StringBuilder();
        this.individualIDs = new ArrayList<String>();
        sb.append("CREATE TABLE IF NOT EXISTS " + ALLELES +
                " (row INTEGER PRIMARY KEY," +
                " rsid TEXT NOT NULL" );
                for (String id : individualIDs) {
                  sb.append(", " + id + "allele1");
                  sb.append(", " + id + "allele2");
                  this.individualIDs.add(id + "allele1");
                  this.individualIDs.add(id + "allele2");
                }
                sb.append(");");
                //" FOREIGN KEY(rsid) REFERENCES " + HUMANSNPS + "(rsid));";
                //HUMANSNPS was used so that the program will compile, but we need to look
                //at which table should actually be referenced here.  We may
                //need two allele tables depending if we are working with humans or not
                //System.out.println("allele create table statement:");
                //System.out.println(sb.toString());
                //System.out.println(individualIDs);
                //System.out.println(this.individualIDs);
                return sb.toString();
    }

    public List<String> getIndividualIDs(){
      return individualIDs;
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

    //perhaps add another table or extend this one to hold intermediate calculations for FST, etc.
    //N so that we can reuse N to calculate D.  Also maybe q.
    private String makeStatsTableCreateStatement(){
      return "CREATE TABLE IF NOT EXISTS " + STATS +
              " (rsid TEXT PRIMARY KEY NOT NULL," +  //I need to find when I update the table to insert this
              " daf REAL," +
              " fst REAL," +
              " ehh0downstream REAL," +
              " ehh0upstream REAL," +
              " ehh1downstream REAL," +
              " ehh1upstream REAL," +
              " ihh0 REAL," +
              " ihh1 REAL," +
              " unstandardizedIHS REAL," +
              " ihs REAL)";
    }

    private String makeStandardizationStatsTableCreateStatement(){
      return "CREATE TABLE IF NOT EXISTS " + STANDARDIZATIONSTATS +
              " (freq REAL," +
              " mean REAL," +
              " standardDev REAL)";
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
