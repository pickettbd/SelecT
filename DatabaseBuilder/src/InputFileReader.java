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
            fileReader = new FileReader(filename);
	} catch (FileNotFoundException e) {
            return false;
        }
        BufferedReader buffReader = new BufferedReader(fileReader);

        String line = null;
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

            //[0] = chrom, [1] = pos, [2] = rsid, [5] = allele freq, [9- ] = individual alleles
            //Insert SNP information into SNP table 
            dao.insert(dao.SNPS, parts[2], parts[0], parts[1], parts[5]);
            //Iterate through individuals and store each of their alleles into allele table
            for(int i = 9; i < parts.length; i++){
                String[] alleles = parts[i].split("|");
                dao.insert(dao.ALLELES, parts[2], individualIDs.get(i-9)+"-0", alleles[0]);
                dao.insert(dao.ALLELES, parts[2], individualIDs.get(i-9)+"-1", alleles[2]);
            }
        }
        return true;
    }
}
