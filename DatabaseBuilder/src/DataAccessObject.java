import java.sql.*;
import java.lang.StringBuilder;
import java.io.*;
import java.util.*;

//clean up functions so less repetition!!!!!
public class DataAccessObject {
    public static String TARGETALLELES;
    public static String CROSSALLELES;
    public static String TARGETSNPS;
    public static String CROSSSNPS;
    public static String STATS;
    public static String STANDARDIZATIONSTATS;
    private static String databaseFileName;

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

        connection = DriverManager.getConnection("jdbc:sqlite:" + currentDirectory + "/DatabaseBuilder/src/db/" + databaseFileName + ".db");
        connection.setAutoCommit(false);
    }

    public static void setDatabaseFileName(String name){
      databaseFileName = name;
    }

    public static void setStatsTableName(String name){
      STATS = name;
      STANDARDIZATIONSTATS = name + "_standardization_stats";
    }

    public static void setTargetPopName(String name){
      TARGETSNPS = name;
      //TARGETALLELES = name + "_alleles";
      //WE NEED TO MODIFY SOME STRUCTURE TO HANDLE TWO ALLELES TABLES
    }

    public static void setCrossPopName(String name){
      CROSSSNPS = name;
      //CROSSALLELES = name + "_alleles";
      //WE NEED TO MODIFY SOME STRUCTURE TO HANDLE TWO ALLELES TABLES
    }

    public static void setCrossAllelesName(String name){
      CROSSALLELES = name;
    }

    public static void setTargetAllelesName(String name){
      TARGETALLELES = name;
    }


    public void createInputTables(List<String> individualIDs, boolean makeAlleleTable) {
      //System.out.println("createTables called");
        try {
            if(connection == null)
                Connect();

            Statement stmt = connection.createStatement();
            if (makeAlleleTable) {
              stmt.executeUpdate(makeAlleleTableCreateStatement(individualIDs));
            }
            stmt.executeUpdate(makeTargetSNPTableCreateStatement());
            stmt.close();
        } catch (Exception e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            e.printStackTrace();
        }
    }

    public void createStatTables(){
      try {
          if(connection == null)
              Connect();

          Statement stmt = connection.createStatement();
          stmt.executeUpdate(makeStatsTableCreateStatement());
          insertStatsRsids();
          stmt.executeUpdate(makeStandardizationStatsTableCreateStatement());
          stmt.close();
      } catch (Exception e) {
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
        "' WHERE rsid = (SELECT rsid from " + TARGETSNPS + " WHERE row = '" + String.valueOf(row) + "')";
        executeUpdate(insert);
    }

    private void insertStatsRsids(){
      String insert = "INSERT INTO " + STATS + " (rsid) SELECT rsid FROM " + TARGETSNPS;
      executeUpdate(insert);
    }

    public boolean statsWereCaluclated(String stat){
      String insert = "SELECT COUNT(" + stat + ") AS rowcount FROM " + STATS + " WHERE " + stat + " != null";
      return getCount(insert);
    }

    public boolean freqHasStats(String frequency){
      String insert = "SELECT COUNT(freq) AS rowcount FROM " + STANDARDIZATIONSTATS + " WHERE freq = '" + frequency + "';";
      return getCount(insert);
    }

    private boolean getCount(String insert){
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          //System.out.println(insert);
          ResultSet rs = statement.executeQuery(insert);
          int result = rs.getInt("rowcount");
          statement.close();
          if (result == 0) {
            return false;
          }
          else
            return true;
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
      String insert = "SELECT COUNT(rsid) AS rowcount FROM " + TARGETSNPS;
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
    public List<AlleleFrequency> getAlleleFrequency(){
      String insert = "SELECT " + TARGETSNPS + ".freq AS targetfreq, " + TARGETSNPS + ".n AS targetn, " + CROSSSNPS + ".freq AS crossfreq, " + CROSSSNPS + ".n AS crossn " + "FROM " + TARGETSNPS + ", " + CROSSSNPS + " WHERE " + TARGETSNPS + ".rsid = " + CROSSSNPS +  ".rsid";
      List<AlleleFrequency> list = new ArrayList<AlleleFrequency>();
      try {
          if(connection == null)
              Connect();

          Statement statement = connection.createStatement();
          //insert = buildAlleleFrequencyInsertStatement(args);
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
      String insert = "SELECT unstandardizedIHS FROM " + STATS + " WHERE " + STATS + ".rsid IN (SELECT rsid FROM " + TARGETSNPS + " WHERE freq = '" + freq + "');";
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
      String insert = "SELECT freq FROM " + TARGETSNPS + " WHERE row = '" + String.valueOf(i) + "';";
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
      String insert = "SELECT unstandardizedIHS FROM " + STATS + " WHERE rsid = (SELECT rsid FROM " + TARGETSNPS + " WHERE row = '" + String.valueOf(i) + "');";
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
      String request = "SELECT rsid FROM " + TARGETSNPS;
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

    public List<Integer> getAlleleRow(int i, String alleleTableName){
      //System.out.println(individualIDs.toString());
      String request = "SELECT * FROM " + alleleTableName + " WHERE row = '" + String.valueOf(i) + "';";
      //System.out.println(request);
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

    public List<Integer> getAlleleColumn(String individualId, String whereClause, String alleleTableName){
      String request = "SELECT " + individualId + " FROM " + alleleTableName + " " + whereClause + ";";
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
      String query = "SELECT pos FROM " + TARGETSNPS + " h INNER JOIN " + TARGETALLELES + " a ON h.rsid = a.rsid WHERE a.row = " + String.valueOf(i) + ";";
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
        sb.append("CREATE TABLE IF NOT EXISTS " + TARGETALLELES +
                " (row INTEGER PRIMARY KEY," +
                " rsid TEXT NOT NULL" );
                for (String id : individualIDs) {
                  sb.append(", " + id + "allele1");
                  sb.append(", " + id + "allele2");
                  this.individualIDs.add(id + "allele1");
                  this.individualIDs.add(id + "allele2");
                }
                sb.append(");");
                return sb.toString();
    }

    public List<String> getIndividualIDs(){
      return individualIDs;
    }

    //We need to make sure that data storage types will not introduce roundoff error!
    private String makeTargetSNPTableCreateStatement() {
        return  "CREATE TABLE IF NOT EXISTS " + TARGETSNPS +
                " (row INTEGER PRIMARY KEY," +
                " rsid TEXT NOT NULL," +
                " chr INT NOT NULL," +
                " pos INT NOT NULL," +
                " freq REAL NOT NULL," +
                " n REAL NOT NULL)";
    }


    //perhaps add another table or extend this one to hold intermediate calculations for FST, etc.
    //N so that we can reuse N to calculate D.  Also maybe q.
    private String makeStatsTableCreateStatement(){
      return "CREATE TABLE IF NOT EXISTS " + STATS +
              " (row INTEGER PRIMARY KEY," +
              " rsid TEXT NOT NULL," +  //I need to find when I update the table to insert this
              " daf REAL," +
              " fst REAL," +
              " ehh0downstream REAL," +
              " ehh0upstream REAL," +
              " ehh1downstream REAL," +
              " ehh1upstream REAL," +
              " ihh0 REAL," +
              " ihh1 REAL," +
              " unstandardizedIHS REAL," +
              " ihs REAL," +
              " xpehh REAL)";
    }

    private String makeStandardizationStatsTableCreateStatement(){
      return "CREATE TABLE IF NOT EXISTS " + STANDARDIZATIONSTATS +
              " (freq REAL," +
              " mean REAL," +
              " standardDev REAL)";
    }

/*
    private String buildAlleleFrequencyInsertStatement(){
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
    */

}
