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

        //Insert the population ID and description to its table
        dao.insert(dao.POPULATIONS, popid, popDescription);

        //Read in lines of .vcf files.
        while((line = buffReader.readLine()) != null){
            if(line.charAt(0) == '#') {
                //Ignore ## header lines. Single # line contains column headings
                if(line.charAt(1) != '#'){   
                    //Break up header to get individual IDs (from position 9 until the end of line)
                    String[] parts = line.split("\t"); 
                    for(int i = 9; i < parts.length; i++){
                        dao.insert(dao.INDIVIDUALS, parts[i], popid);
                        individualIDs.add(parts[i]);
                    }
                }
                continue;
            }
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

            // Get frequency of mutated gene from INFO column
            String freq = "";
            for (String infoPart : info) {
                if(infoPart.startsWith("AF")) {
                    freq = infoPart.substring(3);
                    //System.out.println("freq = " + freq);
                    break;
                }
            }

            dao.insert(dao.SNPS, id, chrom, pos, freq);
            //Iterate through individuals and store each of their alleles into allele table
            for(int i = 9; i < parts.length; i++){
                String[] alleles = parts[i].split("|");
                dao.insert(dao.ALLELES, id, individualIDs.get(i-9)+"-0", alleles[0]);
                dao.insert(dao.ALLELES, id, individualIDs.get(i-9)+"-1", alleles[2]);
            }
        }
        return true;
    }
}
