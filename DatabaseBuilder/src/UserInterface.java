import java.io.*;

class UserInterface {
    private static String FILENAME_PROMPT = "Enter path to .vcf input file (leave blank if none): ";
    private static String POPULATIONID_PROMPT = "Enter population ID for data stored in the file: ";
    private static String POPULATIONDESC_PROMPT = "Enter a description of this population: ";
    private static String FILENOTFOUND_ERROR = "Error: File not found.";

    public InputTask taskPrompt() throws IOException {
        String filename = null, populationID = null, description = null;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        System.out.print(FILENAME_PROMPT);
        filename = input.readLine();
        if (filename.equals(""))
            return null;
        System.out.print(POPULATIONID_PROMPT);
        populationID = input.readLine();
        System.out.print(POPULATIONDESC_PROMPT);
        description = input.readLine();
       return new InputTask(filename, populationID, description);
    }

    public void fileNotFound() {
        System.out.println(FILENOTFOUND_ERROR);
    }
}
