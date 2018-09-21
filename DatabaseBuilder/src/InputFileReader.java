import java.io.*;
import java.util.*;

class InputFileReader {

    private DataAccessObject dao;

    public InputFileReader(DataAccessObject dao){
        this.dao = dao;
    }

    public boolean readFile(String filename, String popid, String popDescription) throws IOException {
        //Prepare reader
        FileReader fileReader;
	      try {
	          String workingDirectory = System.getProperty("user.dir") + "/DatabaseBuilder/test/";
            fileReader = new FileReader(workingDirectory + filename);
	      } catch (FileNotFoundException e) {
            return false;
        }
        BufferedReader buffReader = new BufferedReader(fileReader);

        String line;
        List<String> individualIDs = new ArrayList<>();


        String targetPopulation = "";
        String crossPopulation = "";

        if (popDescription.equals("human")) {
          targetPopulation = UserInterface.getTargetPopulation();
          crossPopulation = UserInterface.getCrossPopulation();
        }

        //Read in lines of .vcf files.
        int lineNumber = 0;
        while ((line = buffReader.readLine()) != null){
            if(line.charAt(0) == '#') {
                //Ignore ## header lines. Single # line contains column headings
                if(line.charAt(1) != '#'){
                    //Break up header to get individual IDs (from position 9 until the end of line)
                    String[] parts = line.split("\t");
                    for(int i = 9; i < parts.length; i++){
                        //dao.insert(dao.INDIVIDUALS, parts[i], popid);
                        individualIDs.add(parts[i]);
                    }
                    dao.CreateTables(individualIDs);
                }
                continue;
            }


            //Insert the population ID and description to its table

            String[] parts = line.split("\t");


            /*
             [0] = CHROM chromosome number
             [1] = POS reference position, with first base at position 1
             [2] = ID normally rsid (and referred to herein as such)
             [3] = REF reference base(s)
             [4] = ALT alternate base(s) (mutated, non-standard base)
             [5] = QUAL quality score for assertion in ALT
             [6] = FILTER gives a PASS if the position passed all filters
             [7] = INFO additional information, coded in <key>=<data>[,data] format.
             [8] = FORMAT if present, indicates genotype data
             [9- ] = individual alleles

             source: http://www.internationalgenome.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-40/
            */
            // Insert SNP information into SNP table
            String chrom = parts[0];
            String pos = parts[1];
            String id = parts[2];
            String[] info = parts[7].split(";");

            // Get frequency of mutated gene and total alleles (n) sequenced from INFO column
            String freq = "";
            String n = "";
            for (String infoPart : info) {
                if(infoPart.startsWith("AF=")) {
                    freq = infoPart.substring(3);
                    //System.out.println("freq = " + freq);
                }
                else if (infoPart.startsWith("AN")) {
                  n = infoPart.substring(3);
                  break;
                }
            }

            //This handles insertion for a typical species' vcf file (the end goal for the program)
            //We need to have two different tables, one for each file, not just SNPS
            if (popDescription.equals("target")) {
              dao.insert(dao.TARGETSNPS, id, chrom, pos, freq, n);
              //System.out.println("target entered");

            }

            else if (popDescription.equals("cross")) {
              dao.insert(dao.CROSSSNPS, id, chrom, pos, freq, n);
              //System.out.println("cross entered");

            }

            //in the case of a single human file where we need two additional columns
            else {
              //Get frequency of target and cross populations from INFO column
              //System.out.println("popDescription is " + popDescription);
              String target = "";
              for (String infoPart : info) {
                  if(infoPart.startsWith(targetPopulation)) {
                      target = infoPart.substring(8);
                      //System.out.println("freq = " + freq);
                      break;
                  }
              }
              String cross = "";
              for (String infoPart : info) {
                  if(infoPart.startsWith(crossPopulation)) {
                      cross = infoPart.substring(8);
                      //System.out.println("freq = " + freq);
                      break;
                  }
              }

              //We need to create a different table for humans that has two more rows
              dao.insert(dao.HUMANSNPS, id, chrom, pos, freq, target, cross, n, n);
              dao.insertOneValue(dao.STATS, "rsid", id);
            }

            //Iterate through individuals and store each of their alleles into allele table
            String[] finalAlleles = new String[2*individualIDs.size() + 3];
            int j = 0;
            finalAlleles[j++] = dao.ALLELES;
            finalAlleles[j++] = String.valueOf(lineNumber);
            finalAlleles[j++] = id;
            for(int i = 9; i < parts.length; i++){
                String[] alleles = parts[i].split("|");
                finalAlleles[j++] = alleles[0];
                finalAlleles[j++] = alleles[2];
                //dao.insert(dao.ALLELES, id, individualIDs.get(i-9), "0", alleles[0]);
                //dao.insert(dao.ALLELES, id, individualIDs.get(i-9), "1", alleles[2]);
            }
            dao.insert(finalAlleles);
            lineNumber++;
        }
        return true;
    }
}
