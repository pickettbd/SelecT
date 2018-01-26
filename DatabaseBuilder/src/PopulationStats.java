/**
 * Created by Hannah Wadham on 13 Dec 2017
 */

public class PopulationStats {
    private DataAccessObject dao;

    public PopulationStats(DataAccessObject dao){
      this.dao = dao;
      //Do I actually need to pass the same DataAccessObject?
      //If we load in multiple files, DatabaseBuilder recreates a new DataAccessObject
      //each time, does that mean we won't have access to all of our tables?
    }

    public void calculateStats(int numberOfFiles){
      calculateDeltaDAF(numberOfFiles);
      calculateFst();
      //add iHH, etc
    }


    private void calculateDeltaDAF(int numberOfFiles){
      if (numberOfFiles == 1) {
        dao.getDeltaDAF("target", dao.HUMANSNPS, "cross", dao.HUMANSNPS);
      }
      else {
        dao.getDeltaDAF("freq", dao.TARGETSNPS, "freq", dao.CROSSSNPS);
      }
    }

    private void calculateFst() {
        return;
    }


}
