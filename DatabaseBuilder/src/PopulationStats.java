/**
 * Created by Hannah Wadham on 13 Dec 2017
 */

public class PopulationStats {
    private DataAccessObject dao;

    public PopulationStats(DataAccessObject dao){
      this.dao = dao;
      //Do I create a new dao or do I need to pass it into my constructor to access the premade tables?
      //If I create a new one, then we need to make DatabaseBuilder.createDatabase void
    }

    public void calculateStats(int numberOfFiles){
      calculateDeltaDAF(numberOfFiles);
      calculateFst();
      //add iHH, etc
    }

    //This is still pseudocode
    //find an easy way to pass filenames and column names with consistency between classes!!!
    private void calculateDeltaDAF(int numberOfFiles){
      if (numberOfFiles == 1) {
        dao.getDeltaDAF(col1 tablename col2 tablename);
      }
      else {
        dao.getDeltaDAF(col tablename1 col tablename2);
      }
    }

    private void calculateFst() {
        return;
    }


}
