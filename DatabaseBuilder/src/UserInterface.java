import java.io.*;

//Note to self: run from SelecT with java -cp ./DatabaseBuilder/src:./DatabaseBuilder/lib/ UserInterface
//So how do I get a connection with the database?
//Right now this program only works to run one set of stats- you have to delete the database and start over if you're going to do new files.
//So how can we make this a little more useful- give the user the option to find multiple statistics from the pre-loaded database?
class UserInterface {
    private static String FILENAME_PROMPT = "Enter file name (It should be in the 'test' folder) (leave blank if none): ";
    private static String POPULATIONID_PROMPT = "Enter UNIQUE population ID for data stored in the file: ";
    private static String POPULATIONDESC_PROMPT = "Enter a description of this population (human, target, or cross): ";
    private static String TARGETPOPULATION_PROMPT = "Enter the name of the target population (e.g. AFR_AF): ";
    private static String CROSSPOPULATION_PROMPT = "Enter the name of the cross population (e.g. AMR_AF): ";
    private static int numberOfFiles;
    private static DataAccessObject dao;

    public static void main(String[] args) {
        System.out.println("Welcome to SelecT!");
        System.out.println("Please select from among the following options:");
        System.out.println("[1]: run on human genome data- only one vcf file");
        System.out.println("[2]: run on another species' genome data- two vcf files");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            switch (in.readLine()) {
                case "1":
                    numberOfFiles = 1;
                    break;
                case "2":
                    numberOfFiles = 2;
                    break;
                default:
                    System.out.println("Invalid selection. Please quit and try again.");
           }
        } catch (IOException e) {
            System.out.println("Input error: " + e.getMessage());
        }

        while(true) {
            System.out.println("Please select from among the following options:");
            System.out.println("[1] : Import population file");
            System.out.println("[2] : Calculate Statistics");
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            try {
                switch (input.readLine()) {
                    case "1":
                        importFile();
                        break;
                    case "2":
                        runStats();
                        break;
                    default:
                        System.out.println("Invalid selection. Please try again.");
                }
            } catch (IOException e) {
                System.out.println("Input error: " + e.getMessage());
            }
        }
    }

    private static void importFile() throws IOException {
        String filename = null, populationID = null, description = null;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        System.out.print(FILENAME_PROMPT);
        filename = input.readLine();
        if (filename.equals("")) {
            System.out.println("Invalid file name");
            return;
        }
        //this creates a unique key for one of the tables... We need to verify that it is a valid key
        //or remove it altogether.
        System.out.print(POPULATIONID_PROMPT);
        populationID = input.readLine();
        System.out.print(POPULATIONDESC_PROMPT);  //!!!!Check for valid input: human, target, or cross
        description = input.readLine();
        InputTask task = new InputTask(filename, populationID, description);
        dao = DatabaseBuilder.createDatabase(task);
    }

    private static void runStats() {
      //check that there are the appropriate number of files loaded....
      PopulationStats stats = new PopulationStats();
      stats.calculateStats(numberOfFiles);
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
