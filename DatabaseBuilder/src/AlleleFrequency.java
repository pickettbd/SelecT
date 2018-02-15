

public class AlleleFrequency{
  private double targetfreq;
  private double crossfreq;
  private double targetn;
  private double crossn;

  public AlleleFrequency(double targetfreq, double crossfreq, double targetn, double crossn){
    this.targetfreq = targetfreq;
    this.crossfreq = crossfreq;
    this.targetn = targetn;
    this.crossn = crossn;
  }

  public double getTargetFreq(){
    return targetfreq;
  }

  public double getCrossFreq(){
    return crossfreq;
  }

  public double getTargetN(){
    return targetn;
  }

  public double getCrossN(){
    return crossn;
  }
}
