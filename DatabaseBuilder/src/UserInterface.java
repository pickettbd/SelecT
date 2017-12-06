import java.io.*;

class UserInterface {
    private static String FILENAME_PROMPT = "Enter path to .vcf input file (leave blank if none): ";
    private static String POPULATIONID_PROMPT = "Enter population ID for data stored in the file: ";
    private static String POPULATIONDESC_PROMPT = "Enter a description of this population: ";

    public static void main(String[] args) {
        System.out.println("Welcome to SelecT!");

        while(true) {
            System.out.println("Please select from among the following options:");
            System.out.println("[1] : Import population file");
            System.out.println("[2] : Calculate EFF");
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
        System.out.print(POPULATIONID_PROMPT);
        populationID = input.readLine();
        System.out.print(POPULATIONDESC_PROMPT);
        description = input.readLine();
        InputTask task = new InputTask(filename, populationID, description);
        DatabaseBuilder.createDatabase(task);
    }

    private static void runStats() {
        return;
    }
}
