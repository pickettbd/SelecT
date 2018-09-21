/**
 * Created by Hannah Wadham on 13 Dec 2017
 */

import java.sql.*;
import java.lang.*;
import java.util.*;

public class PopulationStats {
    private DataAccessObject dao;
    private int DBLength;

    public PopulationStats(){
      dao = new DataAccessObject();
      DBLength = dao.getDBLength();
    }

    //clean it up- put the database calls either entirely in this function or not at all
    public void calculateStats(int numberOfFiles){
      //System.out.println("calculate stats called" + numberOfFiles);
      calculateDAFandFST(numberOfFiles);
      dao.commit();  //is there anything we need to check for before committing?
      calculateEHH();
      dao.commit();
      calculateIHH();
      dao.commit();
      calculateUnstandardizedIHS();
      dao.commit();
      calculateIHS();
      dao.commit();
      dao.close();

      System.out.println("statistics calculated! Please view them in the database.");
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


//check if a double really is the best way of maintaining decimal precision!
//rename calculateFreqBasedStats?
//Should DAF be in absolute value?
    private void calculateDAFandFST(int numberOfFiles) {
      List<AlleleFrequency> AF = getAlleleFrequency(numberOfFiles); //Combine this with DAF above for less space & database calls
      for (int i = 0; i < DBLength; i++) {
        double target = AF.get(i).getTargetFreq();
        double cross = AF.get(i).getCrossFreq();
        double targetN = AF.get(i).getTargetN();
        double crossN = AF.get(i).getCrossN();

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
        dao.updateDB(dao.STATS, "daf", target - cross, i);
        dao.updateDB(dao.STATS, "fst", fst, i);
      }
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
    private void calculateEHH() {

         List<String> individualIDs = dao.getIndividualIDs();
         //System.out.println(individualIDs.toString());
         for(int row = 0; row < DBLength; row++) {
            List<Integer> alleles = dao.getAlleleRow(row);
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
            calculateDownstreamAndUpstreamEHH(individualIDs, list0, row, 0);
            calculateDownstreamAndUpstreamEHH(individualIDs, list1, row, 1);
         }
    }

//break down bottom function into these three?
/*
    private Double calculateDownstreamEHH(List<String> individualIDs, List<Integer> alleleLocations, int row){

    }
    private Double calculateUpstreamEHH(List<String> individualIDs, List<Integer> alleleLocations, int row){

    }

    private Double findAlleleFrequency(List<String> individualIDs, List<Integer> alleleLocations, int row){

    }
    */

    private void calculateDownstreamAndUpstreamEHH(List<String> individualIDs, List<Integer> alleleLocations, int row, int id){
      Map<String, Integer> downstreamHaplotypes = new HashMap<>();
      Map<String, Integer> upstreamHaplotypes = new HashMap<>();

      //System.out.println("Allele locations: " + alleleLocations.toString());
      //System.out.println("rsid index: " + row);

      for(int individual : alleleLocations) {
        //Add the option for the user to include a window size?
          List<Integer> downstreamSNPs = dao.getAlleleColumn(individualIDs.get(individual), "WHERE row > " + String.valueOf(row));
          //System.out.println("downstream SNPS: " + downstreamSNPs.toString());
          List<Integer> upstreamSNPs = dao.getAlleleColumn(individualIDs.get(individual), "WHERE row < " + String.valueOf(row)); //ORDER BY rsid DESC
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

      dao.updateDB(dao.STATS, "ehh" + String.valueOf(id) + "downstream", downstreamEHH, row);
      dao.updateDB(dao.STATS, "ehh" + String.valueOf(id) + "upstream", upstreamEHH, row);
    }

//modify ehh and ihh to handle doing cross-population if that is an option
//Loop through to do ehh0 AND ehh1 - right now this is only doing one or the other based on what is returned from the database
//Find a way to make it stop at tails of chromosomes
    private void calculateIHH(){
      List<Double> EHH0downstream = dao.getStat("ehh0downstream");
      List<Double> EHH0upstream = dao.getStat("ehh0upstream");
      List<Double> EHH1downstream = dao.getStat("ehh1downstream");
      List<Double> EHH1upstream = dao.getStat("ehh1upstream");
      calculateIHH(EHH0downstream, EHH0upstream, 0);
      calculateIHH(EHH1downstream, EHH1upstream, 1);
    }


    private void calculateIHH(List<Double> downstreamEHH, List<Double> upstreamEHH, int id){

      for (int currentBasePair = 0; currentBasePair < upstreamEHH.size(); currentBasePair++){
        //find a way to do this loop once and not repeat code
        Double forwardIHH;
        forwardIHH = 0.0;
        int index = currentBasePair;
        //update this to an actual function
        //List<Double> EHH = db.getSomethin(); //SELECT from either human or target WHERE rsid >= rsids.get(row)
        while (index < upstreamEHH.size()){
          if (upstreamEHH.get(index) < 0.05){
            break;  //I don't need to include the <.05 value because it's trapazoidal quadrature- averaging of areas IN BETWEEN points and continuing would put me past the .05 value.
          }
          else if ( index == (upstreamEHH.size() - 1) /*hit tail of chromosome*/){
            forwardIHH = null;
            break;
            //find a way to automatically truncate the backward process from here
          }
          //account for throwing an error from database?
          forwardIHH += (upstreamEHH.get(index) + upstreamEHH.get(index + 1)) * getGeneticDistance(index, index + 1)/2;
          index++;
        }

        Double backwardIHH;
        backwardIHH = 0.0;
        //List<Double> otherEHH = db.getOtherSomethin();
        index = currentBasePair;
        while (index != -1 && forwardIHH != null){
          //'Truncated once ehh < 0.05- but do I need to include the first <.05 value?'
          if (downstreamEHH.get(index) < 0.05){
            break;
          }
          else if (index == 0/*hit tail of chromosome*/){
            backwardIHH = null;
            break;
          }
          backwardIHH += (downstreamEHH.get(index) + downstreamEHH.get(index - 1)) * getGeneticDistance(index, index - 1) /2;
          index--;
        }
        Double totalIHH;
        if (forwardIHH != null && backwardIHH != null) {
          totalIHH = forwardIHH + backwardIHH;
        }
        else {
          totalIHH = null;
        }
        dao.updateDB(dao.STATS, "ihh" + String.valueOf(id), totalIHH, currentBasePair);
      }

    }


    private void calculateUnstandardizedIHS(){
      List<Double> IHH0 = dao.getStat("ihh0");
      List<Double> IHH1 = dao.getStat("ihh1");
      for (int currentBasePair = 0; currentBasePair < IHH0.size(); currentBasePair++){
        if(IHH0.get(currentBasePair) != null && IHH1.get(currentBasePair) != null){
          double IHS = Math.log(IHH0.get(currentBasePair)/IHH1.get(currentBasePair));
          dao.updateDB(dao.STATS, "unstandardizedIHS", IHS, currentBasePair);
        }
        else{
          dao.updateDB(dao.STATS, "unstandardizedIHS", null, currentBasePair);
        }
      }
    }

    private void calculateIHS(){
      //IF unstandardized is valid / snp is valid or whatever....
      for (int i = 0; i < DBLength; i++){
        String freq = dao.getFreq(i);
        if (dao.freqHasStats(freq) == false) {
          calculateStandardizationStatistics(freq);
        }
        Double mean = dao.getMean(freq);
        Double standardDeviation = dao.getStandardDeviation(freq);
        Double unstandardizedIHS = dao.getUnstandardizedIHS(i);
        Double IHS = (unstandardizedIHS - mean)/standardDeviation;
        dao.updateDB(dao.STATS, "ihs", IHS, i);
      }
      //dao.insert(dao.STANDARDIZATIONSTATS, freq /*!!!!!!*/, String.valueOf(IHS));
    }

    private void calculateStandardizationStatistics(String freq){
      List<Double> unstandardizedIHSs = dao.getUnstandardizedIHSvaluesFromSNPFrequency(freq);
      Double mean = calculateMean(unstandardizedIHSs);

      double squarePower = 2;
      List<Double> IHSsSquared = new ArrayList<Double>();
      for (int i = 0; i < unstandardizedIHSs.size() - 1; i++){ //need to take out the -1 once below is fixed
        //We're not going to be able to handle such a long vector?  FIX!
        IHSsSquared.add(Math.pow(unstandardizedIHSs.get(i), squarePower)); //Needs to handle a null value
      }
      Double meanOfSquares = calculateMean(IHSsSquared);
      Double standardDeviation = Math.sqrt(meanOfSquares - Math.pow(mean, squarePower));
      dao.insert(dao.STANDARDIZATIONSTATS,
        String.valueOf(freq),
        String.valueOf(mean),
        String.valueOf(standardDeviation));
    }

    //In other papers, this is referenced as 'expected value'
    private Double calculateMean(List<Double> unstandardizedIHSs){
        Double sum = 0.0;
        for (int i = 0; i < unstandardizedIHSs.size() - 1; i++){
          Double temp = unstandardizedIHSs.get(i);
          //NOT handling null, throws exception -need to fix!!!!
          if (Double.isFinite(temp) && (temp != null)){
            sum += temp;
          }
        }
        Double mean = sum / unstandardizedIHSs.size();
        return mean;
    }

    //Adjusts for too much space between selected SNPS
    private float getGeneticDistance(int i, int j){
      int distance = dao.getBasePairPosition(i) - dao.getBasePairPosition(j);
      if (Math.abs(distance) > 20){ //Why did I stick 20 in here? From Selscan paper- explain
        return ((float) 20)/ distance;
      }
      return 1;
    }
}
