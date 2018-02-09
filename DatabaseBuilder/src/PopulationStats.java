/**
 * Created by Hannah Wadham on 13 Dec 2017
 */

import java.sql.*;

public class PopulationStats {
    private DataAccessObject dao;

    public PopulationStats(){
      dao = new DataAccessObject();
    }

    public void calculateStats(int numberOfFiles){
      private ResultSet rs = new ResultSet();
      if (numberOfFiles == 1) {
        rs = dao.getAlleleFrequency("target", dao.HUMANSNPS, "cross", dao.HUMANSNPS);
      }
      else {
        rs = dao.getAlleleFrequency("freq", dao.TARGETSNPS, "freq", dao.CROSSSNPS);
      }
      rs.first();
      while(!rs.isAfterLast()){
        float DAF;
        float FST;
        DAF = calculateDeltaDAF(rs.getFloat("target"), rs.getFloat("cross"));
        FST = calculateFst(rs.getFloat("target"), rs.getFloat("cross"));
        //add iHH, etc
        insert(DAF, FST);
        rs.next();
      }

    }


    private float calculateDeltaDAF(float target, float cross){
      //do calculations
    }

    private float calculateFst(int numberOfFiles) {
      //do calculations
    }

    private void insert(float DAF, float FST){
      //make some call on dao to insert calculations
    }

}
