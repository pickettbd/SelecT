import java.io.*;

class DatabaseBuilder {
    private static InputFileReader inputReader;
    private static String FILENOTFOUND_ERROR = "Error: File not found.";

    public static DataAccessObject createDatabase(InputTask task){
        DataAccessObject dataAccess = new DataAccessObject();
        inputReader = new InputFileReader(dataAccess);
        try {
            if (task == null) {
              System.err.println("task was null");
                return null;}
            if(!inputReader.readFile(task.getFilename(), task.getPopulationID(), task.getDescription()))
                fileNotFound();
            else {
              dataAccess.commit();
      		    System.out.println("File successfully imported!\n");
            }
        } catch (IOException e) {
        }
        return dataAccess;
    }

    public static void fileNotFound() {
        System.out.println(FILENOTFOUND_ERROR);
    }
}
