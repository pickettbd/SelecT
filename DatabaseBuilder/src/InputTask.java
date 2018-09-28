class InputTask {
    String filename;
    String columnToRead;
    boolean makeAlleleTable;

    public InputTask(){
      makeAlleleTable = true;
      columnToRead = "AF";
    }

    public InputTask (String filename, boolean makeAlleleTable, String columnToRead) {
        this.filename = filename;
        this.columnToRead = columnToRead;
        this.makeAlleleTable = makeAlleleTable;
    }

    public void setColumnToRead(String columnToRead){
      this.columnToRead = columnToRead;
    }

    public void setFilename(String filename){
      this.filename = filename;
    }

    public void setMakeAlleleTable(boolean makeAlleleTable){
      this.makeAlleleTable = makeAlleleTable;
    }

    public String getFilename() {
        return filename;
    }

    public String getColumnToRead(){
      return columnToRead;
    }

    public boolean getMakeAlleleTable() {
        return makeAlleleTable;
    }
}
