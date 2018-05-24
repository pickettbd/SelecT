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

    //clean it up- put the database calls either entirely in this function or not at all
    public void calculateStats(int numberOfFiles){
      //System.out.println("calculate stats called" + numberOfFiles);
      List<AlleleFrequency> list = getAlleleFrequency(numberOfFiles);
      //rs.first();
      List<String> rsids = dao.getRSIDs();
      List<List<Double>> EHH = calculateEHH(rsids);
      List<Double> EHH0downstream = EHH.get(0);
      List<Double> EHH0upstream = EHH.get(1);
      List<Double> EHH1downstream = EHH.get(2);
      List<Double> EHH1upstream = EHH.get(3);
      //ADD for EHH1 up & down stream
      //Write daf and fst to iterate through list on their own just like ehh does?
      for(int i = 0; i < list.size(); i++){
        //System.out.println("while loop entered");
        double DAF;
        double FST;
        DAF = calculateDeltaDAF(list.get(i).getTargetFreq(), list.get(i).getCrossFreq());
        FST = calculateFST(list.get(i).getTargetFreq(), list.get(i).getCrossFreq(), list.get(i).getTargetN(), list.get(i).getCrossN());
         // Pass in rsids instead of making extra internal database call
        //System.out.println("DAF: " + DAF + " FST: " + FST);
        //System.out.println(String.valueOf(DAF) + " " + String.valueOf(FST));
        //add iHH, etc
        dao.insert(dao.STATS, rsids.get(i),
          String.valueOf(DAF),
          String.valueOf(FST),
          String.valueOf(EHH0downstream.get(i)),
          String.valueOf(EHH0upstream.get(i)),
          String.valueOf(EHH1downstream.get(i)),
          String.valueOf(EHH1upstream.get(i)));
      }
      System.out.println("statistics calculated! Please view them in the database (src/db/SELECT.db)");
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
    private Double calculateFST(double target, double cross, double targetN, double crossN) {
      double h1;
      h1 = (target * (1 - target) * targetN/(targetN - 1));
      double h2;
      h2 = (cross * (1 - cross) * crossN/(crossN - 1));
      double numerator;
      double denominator;
      numerator = Math.pow((target - cross), 2) - h1/targetN - h2/crossN;
      denominator = numerator + h1 + h2;
      double fst;
      if (numerator == 0 && denominator == 0) {
        fst = 0;
      }
      else if (numerator != 0 && denominator == 0) {
        fst = 1;
      }
      else {
        fst = numerator/denominator;
      }
      return fst;
    }


    /*

    **CALCULATING DOWNSTREAM EHH**
     from the database, get all the RSIDs (or chrom and pos)
     from the database, get the first row from the alleles table (ex. chrom 21, pos 9411239, rsid rs559462325 for the first one)
     find each individual/allele combination that has a zero and add it to a list. (ex. HG0179-the first individual-has a 0 in both alleles)
        maybe make a class to hold this. The class would have attributes individualid and allele (1 or 2).
     do the same thing with each individual/allele combination that has a 1. You should have two lists now -
        one with all 0s and the other with all 1s. These are your core haplotypes.

     create a map with a binary number key and an integer value.
        the binary number key will be the haplotype and the integer value will be the number of occurances of that haplotype.

     from the database, retrieve the column from the alleles table with an individual ID matching the first individual in the 0 list.
        in our test database, that should be HG01879
        note which chromosome the 0 occured on (allele 1 or 2).
     create a StringBuilder called "haplotype"
     iterate through each SNP following the core haplotype at the same allele as the core haplotype, appending its value to "haplotype"
        if the core haplotype was on allele 1, run this on allele 1 for the rest of the SNPs.
     convert "haplotype" to a binary integer and check to see if its value has been inserted into the map yet
        if it has, increment the value found there
        if it has not, create the entry with value 1 (ex. Set.add(0100010111010100010100110100000100, 1))
     Repeat with each individual in the 0 list.

     now that we have found all the haplotypes following each occurance of 0 in the SNP we're looking at, we can calculate EHH0.
     create a double called "EHH"
     iterate through the members of your map
        for each member, divide the value by the size of the 0 list and square the result
        add this to "EHH"
     you now have EHH0. Store this in the database.

     to calculate EHH1, repeat this process with the 1 list

     repeat this for each locus

     (note: we will also need to calculate upstream EHH to calculate iHH)


     **Pseudocode:**
    */
    private List<List<Double>> calculateEHH(List<String> rsids) {


         //List<String> rsids = database.getRSIDs();
         //integer tableSize = database.getAlleleTableSize(); // Hannah added but don't think we need
         int rsidIndex = 0; //This will allow us to query the database appropriately according to rsid which may not be in numerical order.
         List<Double> downstreamEHH0s = new ArrayList<>();
         List<Double> upstreamEHH0s = new ArrayList<>();
         List<Double> downstreamEHH1s = new ArrayList<>();
         List<Double> upstreamEHH1s = new ArrayList<>();
         List<String> individualIDs = dao.getIndividualIDs();
         System.out.println(individualIDs.toString());
         for(String rsid : rsids) {
           //for Dr. Ridge- why are rsids not in numerical order? is the order in a VCF file random??
            List<Integer> alleles = dao.getAlleleRow(rsid);
            List<Integer> list0 = new ArrayList<>();
            List<Integer> list1 = new ArrayList<>();

            int individualIDindex = 0;
            for(Integer allele : alleles) {
                //how can I get the individual's id?
                if(allele == 0) {
                    list0.add(individualIDindex);
                } else if(allele == 1) {
                    list1.add(individualIDindex);
                }
                individualIDindex++;
            }

            //insert parameters!!!  Break up into two seperate function calls.
            //Calculate statistics for the current rsid
            List<Double> EHHs = calculateDownstreamAndUpstreamEHH(individualIDs, list0, rsidIndex);
            downstreamEHH0s.add(EHHs.get(0));
            upstreamEHH0s.add(EHHs.get(1));
            EHHs = calculateDownstreamAndUpstreamEHH(individualIDs, list1, rsidIndex);
            downstreamEHH1s.add(EHHs.get(0));
            upstreamEHH1s.add(EHHs.get(1));
            rsidIndex++;
         }

         //return all statistics
         List<List<Double>> returnValues = new ArrayList<List<Double>>();
         returnValues.add(downstreamEHH0s);
         returnValues.add(upstreamEHH0s);
         returnValues.add(downstreamEHH1s);
         returnValues.add(upstreamEHH1s);
         return returnValues;
    }

//break down bottom function into these three?
/*
    private Double calculateDownstreamEHH(List<String> individualIDs, List<Integer> alleleLocations, int rsidIndex){

    }
    private Double calculateUpstreamEHH(List<String> individualIDs, List<Integer> alleleLocations, int rsidIndex){

    }

    private Double findAlleleFrequency(List<String> individualIDs, List<Integer> alleleLocations, int rsidIndex){

    }
    */

    private List<Double> calculateDownstreamAndUpstreamEHH(List<String> individualIDs, List<Integer> alleleLocations, int rsidIndex){
      Map<String, Integer> downstreamHaplotypes = new HashMap<>();
      Map<String, Integer> upstreamHaplotypes = new HashMap<>();

      //System.out.println("Allele locations: " + alleleLocations.toString());
      //System.out.println("rsid index: " + rsidIndex);

      for(int individual : alleleLocations) {
          List<Integer> downstreamSNPs = dao.getAlleleColumn(individualIDs.get(individual), "WHERE row > " + String.valueOf(rsidIndex));
          //System.out.println("downstream SNPS: " + downstreamSNPs.toString());
          List<Integer> upstreamSNPs = dao.getAlleleColumn(individualIDs.get(individual), "WHERE row < " + String.valueOf(rsidIndex)); //ORDER BY rsid DESC
              // note: in the final implementation, this will need to be limited to the next and previous million SNPs, respectively.
              // see supporting online material for "A Composite of Multiple Signals" which indicates 1MB before and after the SNP of interest.
          StringBuilder downstreamHaplotype = new StringBuilder();
          StringBuilder upstreamHaplotype = new StringBuilder();

          for(Object snp : downstreamSNPs) {
              downstreamHaplotype.append(snp);
          }
          for(Object snp : upstreamSNPs) {
              upstreamHaplotype.append(snp);
          }

          if(downstreamHaplotypes.containsKey(downstreamHaplotype.toString())) {
              downstreamHaplotypes.put(downstreamHaplotype.toString(), downstreamHaplotypes.get(downstreamHaplotype.toString()) + 1);
          } else {
              downstreamHaplotypes.put(downstreamHaplotype.toString(), 1);
          }

          if(upstreamHaplotypes.containsKey(upstreamHaplotype.toString())) {
              upstreamHaplotypes.put(upstreamHaplotype.toString(), upstreamHaplotypes.get(upstreamHaplotype.toString()) + 1);
          } else {
              upstreamHaplotypes.put(upstreamHaplotype.toString(), 1);
          }
      }

      double downstreamEHH = 0;
      //System.out.println(downstreamHaplotypes.toString());
      //System.out.println(String.valueOf(alleleLocations.size()));
      for (Map.Entry<String, Integer> entry : downstreamHaplotypes.entrySet()) {
        //we're assuming alleleLocations.size != 0
          downstreamEHH += Math.pow( ((double) entry.getValue()) / alleleLocations.size(), 2);
          //System.out.println(downstreamEHH);
          //System.out.println(entry.getValue() + " " + alleleLocations.size());
      }
      double upstreamEHH = 0;
      for (Map.Entry<String, Integer> entry : upstreamHaplotypes.entrySet()) {
          upstreamEHH += Math.pow( ((double) entry.getValue()) / alleleLocations.size(), 2);
      }

      List<Double> returnValues = new ArrayList<Double>();
      returnValues.add(downstreamEHH);
      returnValues.add(upstreamEHH);
      return returnValues;
    }
}
