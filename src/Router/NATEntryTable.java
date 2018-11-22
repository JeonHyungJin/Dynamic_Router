package Router;

public class NATEntryTable {
    private final static int ET_SRC_SIZE = 4;
    private final static int ET_PORT_SIZE = 2;
    private final static int ET_DES_SIZE = 4;

    public byte[] getET_src_IP() {
        return ET_src_IP;
    }

    public void setET_src_IP(byte[] ET_src_IP) {
        this.ET_src_IP = ET_src_IP;
    }

    public byte[] getET_src_port() {
        return ET_src_port;
    }

    public void setET_src_port(byte[] ET_src_port) {
        this.ET_src_port = ET_src_port;
    }

    public byte[] getET_des_IP() {
        return ET_des_IP;
    }

    public void setET_des_IP(byte[] ET_des_IP) {
        this.ET_des_IP = ET_des_IP;
    }

    public byte[] getET_des_port() {
        return ET_des_port;
    }

    public void setET_des_port(byte[] ET_des_port) {
        this.ET_des_port = ET_des_port;
    }

    private byte[] ET_src_IP; //source IP
    private byte[] ET_src_port; //source Port
    private byte[] ET_des_IP; //Destination Ip
    private byte[] ET_des_port; //destination port

    public NATEntryTable(byte[] srcIP,byte[] srcPort,byte[] desIP,byte[] desPort){
        ET_src_IP = new byte[ET_SRC_SIZE];
        ET_des_IP = new byte[ET_DES_SIZE];
        ET_src_port = new byte[ET_PORT_SIZE];
        ET_des_port = new byte[ET_PORT_SIZE];

        System.arraycopy(srcIP, 0, ET_src_IP, 0, 4);
        System.arraycopy(desIP, 0, ET_des_IP, 0, 4);
        System.arraycopy(srcPort, 0, ET_src_port, 0, 4);
        System.arraycopy(desPort, 0, ET_des_port, 0, 4);
    }
}
