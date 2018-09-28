import java.io.*;

class DatabaseBuilder {
    private static InputFileReader inputReader;
    private static String FILENOTFOUND_ERROR = "Error: File not found.";

    public static DataAccessObject createDatabase(InputTask task){
        DataAccessObject dao = new DataAccessObject();
        inputReader = new InputFileReader(dao);
        try {
            if (task == null) {
              System.err.println("task was null");
                return null;}
            if(!inputReader.readFile(task))
                fileNotFound();
            else {
              dao.commit();
              dao.close();
      		    System.out.println("File successfully imported!\n");
            }
        } catch (IOException e) {
          System.out.println("error: " + e.getMessage());
        }
        return dao;
    }

    public static void fileNotFound() {
        System.out.println(FILENOTFOUND_ERROR);
    }
}
