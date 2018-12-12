package Router;

public class ICMPLayer extends BaseLayer {


    byte[] localIP = new byte[4];
    byte[] localIdentifier = {0x10, 0x01};

    ICMPTable[] ICMPTable;
    private int icmpIndex=0;

    public ICMPLayer(String layerName) {
        super(layerName);
    }

    public void setLocalIP(byte[] localIP){
        for (int i = 0; i < 4; i++)
            this.localIP[i] = localIP[i];
    }

    public void setIcmpIndex(int icmpIndex){ this.icmpIndex = icmpIndex;}
    public void setICMPTable(ICMPTable[] icmpTable) {
        this.ICMPTable = icmpTable;
    }


    void checksum(byte[] buf, int length) {
        int i = 0;
        long sum = 0;
        while (length > 0) {
            sum += (buf[i++]&0xff) << 8;
            if ((--length)==0) break;
            sum += (buf[i++]&0xff);
            --length;
        }
        sum = (~((sum & 0xFFFF)+(sum >> 16)))&0xFFFF;

        buf[2] = (byte)((sum>>8)&0xFF);
        buf[3] = (byte)(sum&0xff);

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

        ICMPTable[icmpIndex] = new ICMPTable(srcIP,srcIdentifier,frame_dst_ip,localIdentifier);
        icmpIndex++;
        ApplicationLayer.icmpTableChaged(0, icmpIndex);

        frame_src_ip[0] = localIP[0];
        frame_src_ip[1] = localIP[1];
        frame_src_ip[2] = localIP[2];
        frame_src_ip[3] = localIP[3];

        data[4] = localIdentifier[0];
        data[5] = localIdentifier[1];

        // checksum
        data[2] = 0x00;
        data[3] = 0x00;

        checksum(data, data.length);
    }
    public void convertToOriginal(byte[] data,byte[] socketSrcIP,byte[] socketDstIP ){
        byte[] socketSrcIdentifier = {data[4], data[5]};
        for(int i = 0; i< icmpIndex; i++){
            if(compareAddress(ICMPTable[i].getNew_IP(),socketSrcIP) && comparePort(ICMPTable[i].getNew_identifier(),socketSrcIdentifier)){
                socketDstIP[0] = ICMPTable[i].getSrc_IP()[0];
                socketDstIP[1] = ICMPTable[i].getSrc_IP()[1];
                socketDstIP[2] = ICMPTable[i].getSrc_IP()[2];
                socketDstIP[3] = ICMPTable[i].getSrc_IP()[3];
                data[4] = ICMPTable[i].getSrc_identifier()[0];
                data[5] = ICMPTable[i].getSrc_identifier()[1];
                icmpIndex--;
                ApplicationLayer.icmpTableChaged(1, icmpIndex);

                // checksum
                data[2] = 0x00;
                data[3] = 0x00;

                checksum(data, data.length);

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
