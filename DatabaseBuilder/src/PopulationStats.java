/**
 * Created by Hannah Wadham on 13 Dec 2017
 */

import java.sql.*;
import java.lang.*;
import java.util.*;

public class PopulationStats {
    private DataAccessObject dao;

    public PopulationStats(){
      dao = new DataAccessObject();
    }

    public void calculateStats(int numberOfFiles){
      //System.out.println("calculate stats called" + numberOfFiles);
      List<AlleleFrequency> list = getAlleleFrequency(numberOfFiles);
      //rs.first();
      for(int i = 0; i < list.size(); i++){
        //System.out.println("while loop entered");
        double DAF;
        double FST;
        DAF = calculateDeltaDAF(list.get(i).getTargetFreq(), list.get(i).getCrossFreq());
        FST = calculateFst(list.get(i).getTargetFreq(), list.get(i).getCrossFreq(), list.get(i).getTargetN(), list.get(i).getCrossN());
        //System.out.println("DAF: " + DAF + " FST: " + FST);
        //System.out.println(String.valueOf(DAF) + " " + String.valueOf(FST));
        //add iHH, etc
        dao.insert(dao.STATS, String.valueOf(DAF), String.valueOf(FST));
      }
      dao.commit(); //is there anything we need to check for before committing?
      dao.close();
    }

    private List<AlleleFrequency> getAlleleFrequency(int numberOfFiles){
      if (numberOfFiles == 1) {
        return dao.getAlleleFrequency("targetfreq", "crossfreq", "targetn", "crossn", dao.HUMANSNPS);
      }
      else {
        return dao.getAlleleFrequency("targetfreq", "targetn", "crossfreq", "crossn", dao.TARGETSNPS, dao.CROSSSNPS);
        //I need to modify this to not have copycat code
      }
    }

    //Should this be in absolute value?
    private double calculateDeltaDAF(double target, double cross){
      //System.out.println(target);
      //System.out.println(cross);
      return target - cross;
    }


//check if a double really is the best way of maintaining decimal precision!
    private double calculateFst(double target, double cross, double targetN, double crossN) {
      double numerator;
      double denominator;
      numerator = Math.pow((target - cross), 2) - target * (1 - target)*targetN/(targetN - 1) - cross * (1 - cross) * crossN/(crossN - 1);
      denominator = numerator + 2 * target * (1 - target) + 2 * cross * (1 - cross);
      return numerator/denominator;
    }

}
