package Router;

public class ICMPLayer extends BaseLayer {

    private int entryTableIndex=0;
    byte[] localIP = ((ApplicationLayer)this.getUpperLayer()).tempIPAddress1;
    byte[] localIdentifier = {0x10, 0x01};

    ICMPTable[] ICMPTable;

    public ICMPLayer(String layerName) {
        super(layerName);
    }

    public void receiveICMP(byte[] data, byte[] frame_src_ip, byte[] frame_dst_ip){
        // 글로벌로 보내는 경우
        // src ip src port routerip routerport???
        byte[] srcIP = new byte[4];
        srcIP[0] = frame_src_ip[0];
        srcIP[1] = frame_src_ip[1];
        srcIP[2] = frame_src_ip[2];
        srcIP[3] = frame_src_ip[3];

        //this.getUnderLayer에서 포트넘버를 가져온다.
        byte[] srcIdentifier = new byte[2];
        srcIdentifier[0] = data[4];
        srcIdentifier[1] = data[5];

        ICMPTable[entryTableIndex] = new ICMPTable(srcIP,srcIdentifier,localIP,localIdentifier);
        entryTableIndex++;

        frame_src_ip[0] = localIP[0];
        frame_src_ip[1] = localIP[1];
        frame_src_ip[2] = localIP[2];
        frame_src_ip[3] = localIP[3];

        data[4] = localIdentifier[0];
        data[5] = localIdentifier[1];
    }
    public void convertToOriginal(byte[] socketDstIP, byte[] socketDstIdentifier){
        for(int i = 0; i< ICMPTable.length; i++){
            if(compareAddress(ICMPTable[i].getNew_IP(),socketDstIP) && comparePort(ICMPTable[i].getNew_identifier(),socketDstIdentifier)){
                socketDstIP[0] = ICMPTable[i].getSrc_IP()[0];
                socketDstIP[1] = ICMPTable[i].getSrc_IP()[1];
                socketDstIP[2] = ICMPTable[i].getSrc_IP()[2];
                socketDstIP[3] = ICMPTable[i].getSrc_IP()[3];
                socketDstIdentifier[0] = ICMPTable[i].getSrc_identifier()[0];
                socketDstIdentifier[1] = ICMPTable[i].getSrc_identifier()[1];
            }
        }
    }
    public boolean compareAddress(byte[] firstIP, byte[] secondIP){
        if(firstIP[0] == secondIP[0]){
            if(firstIP[1] == secondIP[1]){
                if(firstIP[2] == secondIP[2]){
                    if(firstIP[3] == secondIP[3]){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean comparePort(byte[] firstPort, byte[] secondPort){
        if(firstPort[0] == secondPort[0]){
            if(firstPort[1] == secondPort[1]){
                return true;
            }
        }
        return false;
    }
}
