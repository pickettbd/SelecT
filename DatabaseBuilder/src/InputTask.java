class InputTask {
    String filename;
    String populationID;
    String description;

    public InputTask (String filename, String populationID, String description) {
        this.filename = filename;
        this.populationID = populationID;
        this.description = description;
    };

    public String getFilename() {
        return filename;
    }

    public String getPopulationID() {
        return populationID;
    }

    public String getDescription() {
        return description;
    }
}
