import java.io.*;

class DatabaseBuilder {
    private static InputFileReader inputReader;
    private static DataAccessObject dataAccess;
    private static UserInterface ui;

    public static void main(String[] args){
        DataAccessObject dataAccess = new DataAccessObject();
        inputReader = new InputFileReader(dataAccess);
        ui = new UserInterface();
        InputTask task;
        while (true) {
            try {
                task = ui.taskPrompt();
                if (task == null)
                    break;
                if(!inputReader.readFile(task.getFilename(), task.getPopulationID(), task.getDescription()))
                    ui.fileNotFound();
		dataAccess.commit();
            } catch (IOException e) {
                break;
            }
        } 
    }
}
