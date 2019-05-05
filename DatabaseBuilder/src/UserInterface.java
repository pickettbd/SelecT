import java.io.*;
import java.util.*;

//Note to self: run from SelecT with java -cp ./DatabaseBuilder/src:./DatabaseBuilder/lib/ UserInterface
//compile from src like normal
//So how can we make this a little more useful- give the user the option to find multiple statistics from the pre-loaded database?
class UserInterface {
  private static String USAGE = "USAGE: -import [filename] [DBfilename] [tablename] {-col} [columnToImport] [Y|N] (associate alleles) OR -calculate [DBfilename] [stat] (ddaf, fst, ehh, ihh, ihs, xpehh) [targetTableName] {[crossTableName]} [statTableName]";

  //TODO: adjust to read in multiple rows
  public static void main(String[] args) {
    try{

      int i = 0;

      if (args[i++].equals("-import") & (args.length == 5 || args.length == 7)){
        InputTask task = new InputTask();
        task.setFilename(args[i++]);
        DataAccessObject.setDatabaseFileName(args[i++]); //fix to handle a .db extension
        DataAccessObject.setTargetPopName(args[i++]);
        String popName = "";
        if (args[i++].equals("-col")){
          popName = args[i++];
        }
        else {
          popName = "AF";
          i--;
        }
        task.setColumnToRead(popName);
        if (args[i++].equals("Y")){
          task.setMakeAlleleTable(true);
          DataAccessObject.setTargetAllelesName(popName + "_alleles");
        }
        else if (args[i].equals("N")){  //TODO: else throw exception //add toLower(), toUpper() etc
          task.setMakeAlleleTable(false);
        }
        DatabaseBuilder.createDatabase(task);
      }


      else if (args[0].equals("-calculate") & (args.length == 5 || args.length == 7)){
        DataAccessObject.setDatabaseFileName(args[i++]);
        String statToCalculate = args[i++];
        DataAccessObject.setTargetPopName(args[i++]);
        DataAccessObject.setTargetAllelesName(args[i] + "_alleles");
        if (statToCalculate.equals("ddaf") || statToCalculate.equals("fst") || statToCalculate.equals("XPEHH")){
          DataAccessObject.setCrossPopName(args[i++]);
          DataAccessObject.setCrossAllelesName(args[i] + "_alleles");
        }
        DataAccessObject.setStatsTableName(args[i++]);

        DataAccessObject dao = new DataAccessObject();
        dao.createStatTables();
        dao.commit();
        dao.close();

        PopulationStats stats = new PopulationStats();
        stats.calculateStats(statToCalculate);
      }
    }
    catch (Exception e){
      System.out.println(e.getMessage());
      System.out.println(USAGE);
    }
  }
}
