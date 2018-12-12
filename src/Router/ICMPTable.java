package Router;

public class ICMPTable {
    private final static int SRC_SIZE = 4;
    private final static int IDENTIFIER_SIZE = 2;
    private final static int DES_SIZE = 4;

    public byte[] getSrc_IP() {
        return src_IP;
    }

    public void setSrc_IP(byte[] src_IP) {
        this.src_IP = src_IP;
    }

    public byte[] getSrc_identifier() {
        return src_identifier;
    }

    public void setSrc_identifier(byte[] src_identifier) {
        this.src_identifier = src_identifier;
    }

    public byte[] getNew_IP() {
        return new_IP;
    }

    public void setNew_IP(byte[] new_IP) {
        this.new_IP = new_IP;
    }

    public byte[] getNew_identifier() {
        return new_identifier;
    }

    public void setNew_identifier(byte[] new_identifier) {
        this.new_identifier = new_identifier;
    }

    private byte[] src_IP; //source IP
    private byte[] src_identifier; //source Identifier
    private byte[] new_IP; //Destination Ip
    private byte[] new_identifier; //destination Identifier


    public ICMPTable(byte[] srcIP, byte[] srcPort, byte[] desIP, byte[] desPort){
        src_IP = new byte[SRC_SIZE];
        new_IP = new byte[DES_SIZE];
        src_identifier = new byte[IDENTIFIER_SIZE];
        new_identifier = new byte[IDENTIFIER_SIZE];

        System.arraycopy(srcIP, 0, src_IP, 0, 4);
        System.arraycopy(desIP, 0, new_IP, 0, 4);
        System.arraycopy(srcPort, 0, src_identifier, 0, 2);
        System.arraycopy(desPort, 0, new_identifier, 0, 2);
    }
}
