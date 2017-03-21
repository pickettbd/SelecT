/**
 * Created by Spencer on 2/24/2017.
 */
public class SNP {
    private String rsid;
    private int chr;
    private int pos;
    private float DAF;
    private float IHH;
    private float unstdIHS;

    public String getRsid() {
        return rsid;
    }

    public void setRsid(String rsid) {
        this.rsid = rsid;
    }

    public int getChr() {
        return chr;
    }

    public void setChr(int chr) {
        this.chr = chr;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public float getDAF() {
        return DAF;
    }

    public void setDAF(float DAF) {
        this.DAF = DAF;
    }

    public float getIHH() {
        return IHH;
    }

    public void setIHH(float IHH) {
        this.IHH = IHH;
    }

    public float getUnstdIHS() {
        return unstdIHS;
    }

    public void setUnstdIHS(float unstdIHS) {
        this.unstdIHS = unstdIHS;
    }
}
