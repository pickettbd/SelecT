import java.io.*;
import java.util.*;

//Note to self: run from SelecT with java -cp ./DatabaseBuilder/src:./DatabaseBuilder/lib/ UserInterface
//compile from src like normal
//Right now this program only works to run one set of stats- you have to delete the database and start over if you're going to do new files.
//So how can we make this a little more useful- give the user the option to find multiple statistics from the pre-loaded database?
class UserInterface {
    private static String SPACER = "***************************";
    private static String WELCOME_PROMPT = "Welcome to SelecT!";
    private static String PLEASESELECT_PROMPT = "Please select from among the following options:";
    private static String DBNAME_PROMT = "Enter database name (case sensitive) (Please do not include a .db extension): ";
    private static String STATSNAME_PROMPT = "Enter a unique name for the stats table that will be created: ";
    private static String WHICHSTATS_PROMPT =
      "Which statistics would you like to run? 'dDAF', 'FST', 'EHH', 'IHH', 'IHS', or 'XPEHH': " +
      "Note that EHH, IHH and IHS will use only one population, the rest will use two. ";
    private static String FILENAME_PROMPT = "Enter file name (It should be in the 'test' folder) (leave blank if none): ";
    private static String TARGETPOPULATION_PROMPT = "Enter the name of the target population (e.g. AFR_AF) (Also is the table name): ";
    private static String CROSSPOPULATION_PROMPT = "Enter the name of the cross population (e.g. AMR_AF) (Also is the table name): ";
    private static DataAccessObject dao; //Not used - delete?!
    private static final List<String> onePopStats = new ArrayList<>() {{
      add("EHH");
      add("IHH");
      add("IHS");
    }};

    //start up program and give option to pick between importing data and calculating stats
    public static void main(String[] args) {
        System.out.println(SPACER);
        System.out.println(WELCOME_PROMPT);
        printMainOptions();
        boolean quit = false;
        while(!quit) {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            try {
                switch (in.readLine()) {
                    case "1":
                        importFile();
                        printMainOptions();
                        break;
                    case "2":
                        runStats();
                        printMainOptions();
                        break;
                    case "3":
                        quit = true;
                        break;
                    default:
                        System.out.println("Invalid selection. Please try again.");
                }
            } catch (IOException e) {
                System.out.println("Input error: " + e.getMessage());
            }
        }
    }

    private static void printMainOptions(){
      System.out.println(PLEASESELECT_PROMPT);
      System.out.println("[1] : Import population file");
      System.out.println("[2] : Calculate Statistics (both populations must have been read into the same database)");
      System.out.println("[3] : Quit");
    }

    private static void importFile() throws IOException {
      InputTask task = new InputTask();
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      try{
        System.out.println(DBNAME_PROMT);
        DataAccessObject.setDatabaseFileName(in.readLine());

        System.out.println(PLEASESELECT_PROMPT);
        System.out.println("[1] : human data--multiple populations in one file");
        System.out.println("[2] : another species--only one population in the file");

        boolean quit = false;
        while(!quit){
          switch (in.readLine()) {
              case "1":
                  setHumanFilePreferences(task);
                  quit = true;
                  break;
              case "2":
                  getFileInformation(task);
                  quit = true;
                  break;
              default:
                  System.out.println("Invalid selection. Please try again.");
          }
        }
      }catch(IOException e){
        System.out.println("Input error: " + e.getMessage());
      }
        System.out.println(task.getMakeAlleleTable());
        DatabaseBuilder.createDatabase(task);
    }

    private static void setHumanFilePreferences(InputTask task){
      try{
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Which population would you like to read in?  (Enter the column name, e.g. AF, AMR_AF, etc)");
        String population = in.readLine();
        if (population.equals("")) {
            System.out.println("Invalid population name");
            return;
        }
        task.setColumnToRead(population);

        System.out.println("Do you want to read in the alleles in the file and associate them with this population (this is necessary for ehh, ihh, and ihs) Y/N");
        String alleles = in.readLine(); //Error check this!!!!!
        System.out.println(alleles);
        if (alleles.equals("N")){
          task.setMakeAlleleTable(false);
          System.out.println("set to false");
        }

        getFileInformation(task);

      }catch(IOException e){
        System.out.println("Input error: " + e.getMessage());
      }
    }



    private static void getFileInformation(InputTask task){
      try{
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        System.out.println(FILENAME_PROMPT);
        String filename = in.readLine();
        if (filename.equals("")) {
            System.out.println("Invalid file name");
            return;
        }
        task.setFilename(filename);
        System.out.println("Enter the population name (what the database table will be called): ");
        String popName = in.readLine();
        DataAccessObject.setTargetPopName(popName);
        if (task.getMakeAlleleTable()) DataAccessObject.setTargetAllelesName(popName + "_alleles");
      }catch(IOException e){
        System.out.println("Input error: " + e.getMessage());
      }
    }

    //Get DB name, should make this a method
    //then target and cross pop names,
    //then what they want the stats table to be called,
    //then which stats they want to run
    private static void runStats() {
      //check that there are the appropriate number of files loaded....
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      //Get DB name, should make this a method
      String statToCalculate = "";
      try{
        System.out.println(DBNAME_PROMT);
        DataAccessObject.setDatabaseFileName(in.readLine());

        //make this one while loop validate that name not in given DB
        System.out.println(STATSNAME_PROMPT);
        DataAccessObject.setStatsTableName(in.readLine());

        DataAccessObject dao = new DataAccessObject();
        dao.CreateStatTables();
        dao.commit();
        dao.close();
        //Also make this one error-checking loop.
        System.out.println(WHICHSTATS_PROMPT);
        statToCalculate = in.readLine();
      }
      catch(IOException e){
        System.out.println("Input error: " + e.getMessage());
      }

      //make these 2 while loops and validate input - that names are in given DB
      String target = getTargetPopulation();
      DataAccessObject.setTargetPopName(target);
      DataAccessObject.setTargetAllelesName(target + "_alleles");
      if (!onePopStats.contains(statToCalculate)){
        String cross = getCrossPopulation();
        DataAccessObject.setCrossPopName(cross);
        DataAccessObject.setCrossAllelesName(cross + "_alleles");
      }

      PopulationStats stats = new PopulationStats();
      stats.calculateStats(statToCalculate);
      System.out.println("statistics calculated! Please view them in the database.");
    }

    //get user information to know which column to load for human data:  Used by InputFileReader
    //modify to check for valid input!  from or convert to AFR_AF, AM_AF, etc in test file
    //maybe check in InputFileReader and have it throw the error if it couldn't find the user input
    //consider throwing the error and handling elsewhere in the program?
    protected static String getTargetPopulation(){
      while (true){
        try {
            System.out.println(TARGETPOPULATION_PROMPT);
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            return input.readLine();
        } catch (IOException e) {
            System.out.println("Input error: " + e.getMessage());
        }
      }
    }

    //get user information to know which column to load for human data.  Used by InputFileReader
    //modify to check for valid input! (same options as above, maybe make an error-checking function?)
    //consider throwing the error and handling elsewhere in the program?
    protected static String getCrossPopulation(){
      while (true){
        try {
            System.out.println(CROSSPOPULATION_PROMPT);
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            return input.readLine();
        } catch (IOException e) {
            System.out.println("Input error: " + e.getMessage());
        }
      }
    }


}
