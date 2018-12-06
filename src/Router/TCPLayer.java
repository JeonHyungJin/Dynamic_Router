package Router;

public class TCPLayer extends BaseLayer { //추가구현 : 실제 CISCO에서 사용하는 암호화를 이용하여 체크섬을 완료하였다.
    final static int TCP_HEAD_SIZE = 20;

    // TCP_HEADERB
    byte[] tcp_head = new byte[TCP_HEAD_SIZE];
    byte[] tcp_sourcePort = new byte[2];
    byte[] tcp_destinationPort = new byte[2];
    byte[] tcp_checksum = new byte[2];    //checksum에서 이거 사용 해야될듯
    byte[] tcp_data;

    // MakeChecksum 재검토 필요


    public TCPLayer(String layerName) {
        super(layerName);
    }

    /**
     * port가 바뀌므로 체크썸도 그에 맞춰 변경 시켜줘야한다.
     */
    boolean receiveTCP(byte[] data, byte[] sourceIP, byte[] destinationIP) {

        if (!checkChecksum(data, sourceIP, destinationIP)) {
            // checksum 오류 맨~
            return false; //오류면 버린다(?)
        }

        byte[] dst_port = new byte[2];
        // byte-order 한번 고민쯤은~
        dst_port[0] = data[2];
        dst_port[1] = data[3];
        byte[] src_port = new byte[2];
        // byte-order 한번 고민쯤은~
        src_port[0] = data[0];
        src_port[1] = data[1];

        if(isToIntra(destinationIP))
            ((RIPLayer) this.getUpperLayer()).convertToOriginal(sourceIP,destinationIP, dst_port);
        else{
            ((RIPLayer) this.getUpperLayer()).receiveNAT(sourceIP, src_port, destinationIP, dst_port);
        }

        // port 변경
        data[2] = dst_port[0];
        data[3] = dst_port[1];

        data[0] = src_port[0];
        data[1] = src_port[1];

        // 수도 헤더 만들기
        byte[] buf = new byte[data.length + IPLayer.IP_HEAD_SIZE];
        System.arraycopy(data, 0, buf, IPLayer.IP_HEAD_SIZE, data.length);
        System.arraycopy(sourceIP, 0, buf, 0, sourceIP.length);
        System.arraycopy(destinationIP, 0, buf, 4, destinationIP.length);
        buf[8]=0x00;
        buf[9]=0x06;
        buf[10]=(byte)(data.length&0xff00);
        buf[11]=(byte)(data.length&0xff);
        buf[IPLayer.IP_HEAD_SIZE+16] = 0x00;
        buf[IPLayer.IP_HEAD_SIZE+17] = 0x00;

        long cksum = checksum(buf, IPLayer.IP_HEAD_SIZE+data.length);

        data[16] = (byte)(cksum&0xff00);
        data[17] = (byte)(cksum&0xff);
        // checksum 다시 만들기

        // 이상 없는 데이터
        // 여기 까지 올라온 패킷은 NAT의 기능을 누리려는 친구들
        // 고로 이 상위인 RoutingModule ( 아직은 RIPLayer) 로 넘겨서 NAT 된 체로 넘어간다.

        // return RoutingModule.translation(data)

        return true;
    }


    private long checksum(byte[] buf, int length) {
        int i = 0;
        long sum = 0;
        while (length > 0) {
            sum += (buf[i++]&0xff) << 8;
            if ((--length)==0) break;
            sum += (buf[i++]&0xff);
            --length;
        }
        sum = (~((sum & 0xFFFF)+(sum >> 16)))&0xFFFF;

        return sum;

    }

    private boolean isToIntra(byte[] destinationIP) {
        for(int i=0;i<4;i++){
            if(((RIPLayer) this.getUpperLayer()).localIP[i] != destinationIP[i])
                return false;
        }
        return true;
    }


    boolean checkChecksum(byte[] data, byte[] sourceIP, byte[] destinationIP) {
        // 수신 시 !


        // 수도 헤더 만들기
        byte[] buf = new byte[data.length + IPLayer.IP_HEAD_SIZE];
        System.arraycopy(data, 0, buf, IPLayer.IP_HEAD_SIZE, data.length);
        System.arraycopy(sourceIP, 0, buf, 0, sourceIP.length);
        System.arraycopy(destinationIP, 0, buf, 4, destinationIP.length);
        buf[8]=0x00;
        buf[9]=0x06;
        buf[10]=(byte)(data.length&0xff00);
        buf[11]=(byte)(data.length&0xff);
        buf[IPLayer.IP_HEAD_SIZE+16] = 0x00;
        buf[IPLayer.IP_HEAD_SIZE+17] = 0x00;

        long cksum = checksum(buf, IPLayer.IP_HEAD_SIZE+data.length);

        byte[] checksum = {(byte)(cksum&0xff00),(byte)(cksum&0xff)};

        byte[] dst_checksum = new byte[2]; //오리지널과
        dst_checksum[0] = data[16];
        dst_checksum[1] = data[17];
        //받은 패킷에 대한 체크썸.
        // now check the checksum;


        System.out.println("Input packet's checksum");
        System.out.printf("%04x %04x\n",dst_checksum[0], dst_checksum[1]);

        System.out.println("Caculated packet's checksum");
        System.out.printf("%04x %04x\n",checksum[0], checksum[1]);

        if (checksum[0] == dst_checksum[0] && checksum[1] == dst_checksum[1]) { //비교한다
            return true;
        } else {
            return false;
        }
    }

}