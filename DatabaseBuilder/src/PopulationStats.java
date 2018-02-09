/**
 * Created by Hannah Wadham on 13 Dec 2017
 */

public class PopulationStats {
    private DataAccessObject dao;

    public PopulationStats(){
      dao = new DataAccessObject();
    }

    public void calculateStats(int numberOfFiles){
      calculateDeltaDAF(numberOfFiles);
      calculateFst(numberOfFiles);
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

    private void calculateFst(int numberOfFiles) {
      if (numberOfFiles == 1) {
        dao.getFst("target", dao.HUMANSNPS, "cross", dao.HUMANSNPS);
      }
      else {
        dao.getFst("freq", dao.TARGETSNPS, "freq", dao.CROSSSNPS);
      }
    }


}
