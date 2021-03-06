package Router;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class UDPLayer extends BaseLayer { //추가구현 : 실제 CISCO에서 사용하는 암호화를 이용하여 체크섬을 완료하였다.
    final static int UDP_HEAD_SIZE = 8;

    // UDP_HEADER BBAAM;
    byte[] udp_head = new byte[UDP_HEAD_SIZE];
    byte[] udp_sourcePort = new byte[2];
    byte[] udp_destinationPort = new byte[2];
    byte[] udp_length = new byte[2];
    byte[] udp_checksum = new byte[2];    //checksum에서 이거 사용 해야될듯
    byte[] udp_data;

    public UDPLayer(String layerName) {
        super(layerName);
    }

    /**
     * @param sourcePort
     */
    void setSourcePort(byte[] sourcePort) {
        for (int i = 0; i < 2; i++)
            udp_sourcePort[i] = sourcePort[i];
    }

    void setDestinationPort(byte[] destinationPort) {
        for (int i = 0; i < 2; i++)
            udp_destinationPort[i] = destinationPort[i];
    }

    public byte[] makeChecksum(byte[] data, byte[] sourceIP, byte[] destinationIP) {
        //지금은 데이터 관련해서 덧셈을 하여 체크섬을 만들었는데.
        //피디에프 확인 결과 가상 헤더에 관해서 IPAddress의 관련 바이트를 또한 더해야한다.
        //하지만 정확한 이해가 되지않아 주석만 달아 올립니다.

        //((IPLayer)this.getUnderLayer()).ip_sourceIP;
        //((IPLayer)this.getUnderLayer()).ip_destinationIP;
        //zero = (byte)0x00;
        //protocol = (byte)0x11;
        //udp_length]
        //////////////////////////////////////////////////////////////// W
        byte[] checksumFirst = new byte[1]; //굳이 배열안써도 될듯.
        byte[] checksumSecond = new byte[1];
        byte[] checksum = new byte[2];

        int length = data.length;

        //Pseudo IPAddress
        checksumFirst[0] += sourceIP[0];
        checksumFirst[0] += sourceIP[2];
        checksumSecond[0] += sourceIP[1];
        checksumSecond[0] += sourceIP[3];

        //Pseudo DestinationAddress
        checksumFirst[0] += destinationIP[0];
        checksumFirst[0] += destinationIP[2];
        checksumSecond[0] += destinationIP[1];
        checksumSecond[0] += destinationIP[3];

        //Pseudo Protocol & udp_length
        checksumFirst[0] += (byte)0x00;
        checksumFirst[0] += (byte) (((data.length + 8)) / 0xFF);;
        checksumSecond[0] += (byte)0x11; //Protocol
        checksumSecond[0] += (byte) (((data.length + 8)) % 0xFF);;


        //UDP sourcePort & destinationPort
        checksumFirst[0] += udp_sourcePort[0];
        checksumFirst[0] += udp_destinationPort[0];
        checksumSecond[0] += udp_sourcePort[1];
        checksumSecond[0] += udp_destinationPort[1];

        //UDP Length
        checksumFirst[0] += (byte) (((data.length + 8)) / 0xFF);;
        checksumSecond[0] += (byte) (((data.length + 8)) % 0xFF);;

        //UDP Data
        for(int i = 0; i< data.length; i = i+2){
            if( i == data.length - 1){
                checksumFirst[0] += data[i];
            }else{
                checksumFirst[0] += data[i]; //홀수 인덱스끼리 더한다.
                checksumSecond[0] += data[i+1]; //짝수 인덱스끼리 더한다.
            }
        }

        //보수를 취한다. 되네!^^
        checksum[0] = (byte)(~checksumFirst[0]);
        checksum[1] = (byte)(~checksumSecond[0]);


        //   그냥 보수취하는게 이상하긴 하지만 일단 pdf에 나와있는대로 했음.


        return checksum; //체크섬 반환
//////////////////////////////////////////////////////////////////////////// N

//        SHA-512암호화
//        MessageDigest digest;
//        byte[] checksum = null;
//        try {
//            digest = MessageDigest.getInstance("SHA-512");
//            digest.reset();
//            digest.update(data);
//            byte[] hiddenData = digest.digest();// 암호화 시킴
//            checksum = Arrays.copyOfRange(hiddenData, 0, 2);
//        } catch (NoSuchAlgorithmException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        return checksum; //checksum 리턴
    }



    void setChecksum(byte[] checksum) {
        //checksum 헤더에 넣어요
        udp_checksum[0] = checksum[0];
        udp_checksum[1] = checksum[1];
    }


    void setLength(byte[] data) {
        // 길이를 설정한다.
        // data.length + 8 을 255 기준으로 2바이트로 나누어 저장한다.
        if ((data.length + 8) < 256) { //확실쓰
            udp_length[0] = (byte) 0x00;
            udp_length[1] = (byte) ((data.length + 8) & 0xFF);
        } else {
            udp_length[0] = (byte) (((data.length + 8)) / 256);
            udp_length[1] = (byte) (((data.length + 8)) % 256);
        }

    }

    int receiveUDP(byte[] data, byte[] sourceIP, byte[] destinationIP) {

        if (checkChecksum(data, sourceIP, destinationIP)) {

            byte[] dst_port = new byte[2];
            // byte-order 한번 고민쯤은~
            dst_port[0] = data[2];
            dst_port[1] = data[3];
            byte[] src_port = new byte[2];
            // byte-order 한번 고민쯤은~
            src_port[0] = data[0];
            src_port[1] = data[1];
// 520

            if (dst_port[0] == 0x02 && dst_port[1] == 0x08) {
                // rip 프로토콜 인거~

                byte[] dataRIP = new byte[data.length];

                System.arraycopy(data, UDP_HEAD_SIZE, dataRIP, 0, dataRIP.length);
                ((RIPLayer) this.getUpperLayer()).receiveRIP(dataRIP, sourceIP);
                return 1;
            }else{
                // nat 이뤄져야함

                // 이상 없는 데이터
                // 여기 까지 올라온 패킷은 NAT의 기능을 누리려는 친구들
                // 고로 이 상위인 RoutingModule ( 아직은 RIPLayer) 로 넘겨서 NAT 된 체로 넘어간다.
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
                byte[] buf = new byte[data.length + TCPLayer.PSEUDO_HEAD_SIZE];
                System.arraycopy(data, 0, buf, TCPLayer.PSEUDO_HEAD_SIZE, data.length);
                System.arraycopy(sourceIP, 0, buf, 0, sourceIP.length);
                System.arraycopy(destinationIP, 0, buf, 4, destinationIP.length);
                buf[8]=0x00;
                buf[9]=0x11;
                buf[10]=(byte)((data.length>>8)&0xff);
                buf[11]=(byte)(data.length&0xff);
                buf[TCPLayer.PSEUDO_HEAD_SIZE+6] = 0x00;
                buf[TCPLayer.PSEUDO_HEAD_SIZE+7] = 0x00;

                long cksum = checksum(buf, TCPLayer.PSEUDO_HEAD_SIZE+data.length);

                data[6] = (byte)((cksum>>8)&0xff);
                data[7] = (byte)(cksum&0xff);
                // checksum 다시 만들기
               //checksum(data, sourceIP, destinationIP);

                return 0;
            }
        } else {
            // checksum 오류 맨~

            return 0; //오류면 버린다(?)
        }

    }

    private boolean isToIntra(byte[] destinationIP) {
        for(int i=0;i<4;i++){
            if(((RIPLayer) this.getUpperLayer()).localIP[i] != destinationIP[i])
                return false;
        }
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


    boolean checkChecksum(byte[] data, byte[] sourceIP, byte[] destinationIP) {
        // 수신 시 !

        byte[] buf = new byte[data.length + TCPLayer.PSEUDO_HEAD_SIZE];
        System.arraycopy(data, 0, buf, TCPLayer.PSEUDO_HEAD_SIZE, data.length);
        System.arraycopy(sourceIP, 0, buf, 0, sourceIP.length);
        System.arraycopy(destinationIP, 0, buf, 4, destinationIP.length);
        buf[8]=0x00;
        buf[9]=0x11;

        buf[10]=(byte)((data.length>>8)&0xff);
        buf[11]=(byte)(data.length&0xff);
        buf[TCPLayer.PSEUDO_HEAD_SIZE+6] = 0x00;
        buf[TCPLayer.PSEUDO_HEAD_SIZE+7] = 0x00;

        long cksum = checksum(buf, TCPLayer.PSEUDO_HEAD_SIZE+data.length);

        byte[] checksum = {(byte)((cksum>>8)&0xff),(byte)(cksum&0xff)};

        byte[] dst_checksum = new byte[2]; //오리지널과
        dst_checksum[0] = data[6];
        dst_checksum[1] = data[7];
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

    boolean sendRIP(byte[] data) {
        int length = data.length;
        byte[] destinationPort = {(byte) 0x02, 0x08};
        byte[] sourcePort = {(byte) 0x02, 0x08}; //임의로 넣어 놓은 것.
        udp_data = new byte[data.length + UDP_HEAD_SIZE];

        // encapsulation

        //UDP header 설정

        //udp source port 설정, 더 고민해봐야함.
        setSourcePort(sourcePort);
        udp_data[0] = udp_sourcePort[0];
        udp_data[1] = udp_sourcePort[1];

        //udp destination port 설정
        setDestinationPort(destinationPort);
        udp_data[2] = udp_destinationPort[0];
        udp_data[3] = udp_destinationPort[1];

        //length를 설정 해야할까요..? ㅎㅎ
        setLength(data);
        udp_data[4] = udp_length[0];
        udp_data[5] = udp_length[1];

        /*checksum을 udp_data header에 추가한다*/
        byte[] tempSource = ((IPLayer)this.getUnderLayer()).ip_sourceIP;
        byte[] tempDestination = ((IPLayer)this.getUnderLayer()).getConnectedRouter(((IPLayer)this.getUnderLayer()).ip_sourceIP);
        byte[] broadcast = {(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff};
        if( tempDestination == null ){
            setChecksum(makeChecksum(data, tempSource, broadcast));

        }else{
            setChecksum(makeChecksum(data, tempSource, tempDestination));

        }
        udp_data[6] = udp_checksum[0];
        udp_data[7] = udp_checksum[1];


        //데이터 설정
        for (int i = 0; i < length; i++)
            udp_data[i + UDP_HEAD_SIZE] = data[i];

        if (((IPLayer) this.getUnderLayer()).sendUDP(udp_data)) {
            return true;
        } else
            return false;
    }

}