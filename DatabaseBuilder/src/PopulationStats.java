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
        calculateEHH();
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

    private void calculateEHH() {
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

         class Allele {
            public Allele(String i, int a) {
                this.individualID = i;
                this.alleleNumber = a;

            public String individualID;
            public int alleleNumber;
         }

         List<String> rsids = database.getRSIDs();
         List<double> downstreamEHHs = new ArrayList<>();
         List<double> upstreamEHHs = new ArrayList<>();
         for(String rsid : rsids) {
            List<Object> alleles = database.get(table=alleles, column=*, WHERE rsid = <rsid>);
            List<Allele> list0 = new List<>();
            List<Allele> list1 = new List<>();

            for(Object allele : alleles) {
                //not sure how the data will be returned by the database - maybe as a json that we should convert to a Java object?
                if(allele.getAllele1() == 0) {
                    list0.push(new Allele(allele.getIndividualID(), 1));
                } else if(allele.getAllele1() == 1) {
                    list1.push(new allele(allele.getIndividualID(), 1));
                }
                if(allele.getAllele2() == 0) {
                    list0.push(new Allele(allele.getIndividualID(), 2));
                } else if(allele.getAllele2() == 1) {
                    list1.push(new allele(allele.getIndividualID(), 2));
                }
            }

            Map<String, int> downstreamHaplotypes = new Map<>();
            Map<String, int> upstreamHaplotypes = new Map<>();

            for(Allele allele : list0) {
                List<Object> downstreamSNPs = database.get(table=alleles, column=<allele.individualID>, WHERE <rsid is after the current one>);
                List<Object> upstreamSNPs = database.get(table=alleles, column=<allele.individualID>, WHERE <rsid is before the current one> ORDER BY rsid DESC);
                    // note: in the final implementation, this will need to be limited to the next and previous million SNPs, respectively.
                    // see supporting online material for "A Composite of Multiple Signals" which indicates 1MB before and after the SNP of interest.
                StringBuilder downstreamHaplotype = new StringBuilder();
                StringBuilder upstreamHaplotype = new StringBuilder();

                if(allele.alleleNumber == 1) {
                    for(Object snp : downstreamSNPs) {
                        downstreamHaplotype.append(snp.getAllele1);
                    }
                    for(Object snp : upstreamSNPs) {
                        upstreamHaplotype.append(snp.getAllele1);
                } else {
                    for(Object snp : downstreamSNPs) {
                        downstreamHaplotype.append(snp.getAllele2);
                    }
                    for(Object snp : upstreamSNPs) {
                        upstreamHaplotype.append(snp.getAllele2);
                    }
                }

                if(downstreamHaplotypes.keyExists(downstreamHaplotype.toString) {
                    downstreamHaplotypes.set(downstreamHaplotype.toString, downstreamHaplotypes.get(downstreamHaplotype.toString) + 1);
                } else {
                    downstreamHaplotypes.set(downstreamHaplotype.toString, 1);
                }

                if(upstreamHaplotypes.keyExists(upstreamHaplotype.toString) {
                    upstreamHaplotypes.set(upstreamHaplotype.toString, upstreamHaplotypes.get(upstreamHaplotype.toString) + 1);
                } else {
                    upstreamHaplotypes.set(upstreamHaplotype.toString, 1);
                }
            }

            double EHH = 0;
            for (Map.Entry<String, int> entry : downstreamHaplotypes.entrySet()) {
                EHH += Math.pow(entry.getValue / list0.size(), 2);
            }
            downstreamEHHs.push(EHH);
            EHH = 0;
            for (Map.Entry<String, int> entry : upstreamHaplotypes.entrySet()) {
                EHH += Math.pow(entry.getValue / list0.size(), 2);
            }
            upstreamEHHs.push(EHH);
         }
         database.update(rsids, downstreamEHHs);
         database.update(rsids, upstreamEHHs);
        */
    }
}
