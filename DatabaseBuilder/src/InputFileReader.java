import java.io.*;
import java.util.*;

class InputFileReader {

    private DataAccessObject dao;
    private BufferedReader in;

    public InputFileReader(DataAccessObject dao){
        this.dao = dao;
    }

    public boolean readFile(InputTask task) throws IOException {
        //Prepare reader
        FileReader fileReader;
	      try {
	          String workingDirectory = System.getProperty("user.dir") + "/DatabaseBuilder/test/"; //consider taking input so they can put their file wherever
            fileReader = new FileReader(workingDirectory + task.getFilename());
	      } catch (FileNotFoundException e) { //Consider throwing this to DatabaseBuilder?
            return false;
        }

        in = new BufferedReader(fileReader);

        String line;
        List<String> individualIDs = new ArrayList<>();


        String targetPopulation = "";
        String crossPopulation = "";

        //Read in lines of .vcf files.
        int lineNumber = 0;
        while ((line = in.readLine()) != null){
            if(line.charAt(0) == '#') {
                //Ignore ## header lines. Single # line contains column headings
                if(line.charAt(1) != '#'){
                    //Break up header to get individual IDs (from position 9 until the end of line)
                    String[] parts = line.split("\t");
                    for(int i = 9; i < parts.length; i++){
                        //dao.insert(dao.INDIVIDUALS, parts[i], popid);
                        individualIDs.add(parts[i]);
                    }
                    dao.createInputTables(individualIDs, task.getMakeAlleleTable());
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
                if(infoPart.startsWith(task.getColumnToRead() + "=")) {
                    freq = infoPart.substring(3);
                    //System.out.println("freq = " + freq);
                }
                else if (infoPart.startsWith("AN")) {
                  n = infoPart.substring(3);
                  break;
                }
            }

            dao.insert(dao.TARGETSNPS, String.valueOf(lineNumber), id, chrom, pos, freq, n);

            //dao.insertOneValue(dao.STATS, "rsid", id);!!!!!!!!!!!  We need to find a workaround for this!!!!!!!

            //Iterate through individuals and store each of their alleles into allele table
            if (task.getMakeAlleleTable()){
              String[] finalAlleles = new String[2*individualIDs.size() + 3];
              int j = 0;
              finalAlleles[j++] = dao.TARGETALLELES;
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
            }


            lineNumber++;
        }
        return true;
    }
}
