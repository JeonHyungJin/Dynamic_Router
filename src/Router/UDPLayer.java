package Router;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class UDPLayer extends BaseLayer {
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

    public byte[] makeChecksum(byte[] data) {
        //지금은 데이터 관련해서 덧셈을 하여 체크섬을 만들었는데.
        //피디에프 확인 결과 가상 헤더에 관해서 IPAddress의 관련 바이트를 또한 더해야한다.
        //하지만 정확한 이해가 되지않아 주석만 달아 올립니다.

        //((IPLayer)this.getUnderLayer()).ip_sourceIP;
        //((IPLayer)this.getUnderLayer()).ip_destinationIP;
        //zero = (byte)0x00;
        //protocol = (byte)0x11;
        //udp_length
        byte[] checksumFirst = new byte[1]; //굳이 배열안써도 될듯.
        byte[] checksumSecond = new byte[1];
        byte[] checksum = new byte[2];

        //Pseudo IPAddress
        checksumFirst[0] += ((IPLayer)this.getUnderLayer()).ip_sourceIP[0];
        checksumFirst[0] += ((IPLayer)this.getUnderLayer()).ip_sourceIP[2];
        checksumSecond[0] += ((IPLayer)this.getUnderLayer()).ip_sourceIP[1];
        checksumSecond[0] += ((IPLayer)this.getUnderLayer()).ip_sourceIP[3];

        //Pseudo DestinationAddress
        checksumFirst[0] += ((IPLayer)this.getUnderLayer()).ip_destinationIP[0];
        checksumFirst[0] += ((IPLayer)this.getUnderLayer()).ip_destinationIP[2];
        checksumSecond[0] += ((IPLayer)this.getUnderLayer()).ip_destinationIP[1];
        checksumSecond[0] += ((IPLayer)this.getUnderLayer()).ip_destinationIP[3];

        //Pseudo Protocol & udp_length
        checksumFirst[0] += (byte)0x00;
        checksumFirst[0] += udp_length[0];
        checksumSecond[0] += (byte)0x11; //Protocol
        checksumSecond[0] += udp_length[1];


        //UDP sourcePort & destinationPort
        checksumFirst[0] += udp_sourcePort[0];
        checksumFirst[0] += udp_destinationPort[0];
        checksumSecond[0] += udp_sourcePort[1];
        checksumSecond[0] += udp_destinationPort[1];

        //UDP Length
        checksumFirst[0] += udp_length[0];
        checksumSecond[0] += udp_length[1];

        //UDP Data
        for(int i = 0; i< data.length; i = i+2){
            checksumFirst[0] += data[i]; //홀수 인덱스끼리 더한다.
            checksumSecond[0] += data[i+1]; //짝수 인덱스끼리 더한다.
        }

        //보수를 취한다.
        checksum[0] = (byte)(~checksumFirst[0]);
        checksum[1] = (byte)(~checksumSecond[0]);

        /*
            그냥 보수취하는게 이상하긴 하지만 일단 pdf에 나와있는대로 했음.
        */

        return checksum; //체크섬 반환


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

    void setChecksum(byte[] checksum) { //checksum 헤더에 넣어요
        udp_checksum[0] = checksum[0];
        udp_checksum[1] = checksum[1];
    }

    // length & checksum 나중쓰
    void setLength(byte[] data) {
        if ((data.length + 8) < 255) { //이거 ㄹㅇ 개 애매 ㅇㅈ?
            udp_length[0] = (byte) 0x00;
            udp_length[1] = (byte) ((data.length + 8) & 0xFF);
            //udp_length 출력해보면 10진수로 출력되는데 이게 과연 맞는 것 인가?
        } else {    //여기도 애매 ㅇㅈ?
            udp_length[0] = (byte) (((data.length + 8)) / 0xFF);
            udp_length[1] = (byte) (((data.length + 8)) % 0xFF);
        }

    }

    boolean receiveUDP(byte[] data, byte[] gateway) {
        System.out.println("여길 온다고?!");
        if (checkChecksum(data)) {
            byte[] dst_port = new byte[2];
            // byte-order 고민한번쯤은~
            dst_port[0] = data[2];
            dst_port[1] = data[3];

            if (dst_port[0] == 0x02 && dst_port[1] == 0x08) {
                // rip 프로토콜 인거~
                byte[] dataRIP = new byte[data.length - UDP_HEAD_SIZE];
                System.arraycopy(data, UDP_HEAD_SIZE, dataRIP, 0, dataRIP.length);

                ((RIPLayer) this.getUpperLayer()).receiveRIP(dataRIP, gateway);
            }
        } else {
            // checksum 오류 맨~

            return false; //오류면 버린다(?)
        }
        return true;
    }

    boolean checkChecksum(byte[] data) {
        // 수신 시 !
        byte[] noheaderData = new byte[data.length - UDP_HEAD_SIZE];
        System.arraycopy(data, 8, noheaderData, 0, noheaderData.length); //짤라서
        byte[] checkingChecksum = new byte[2];
        checkingChecksum[0] = makeChecksum(noheaderData)[0];
        checkingChecksum[1] = makeChecksum(noheaderData)[1];

        byte[] dst_checksum = new byte[2]; //오리지널과
        dst_checksum[0] = data[6];
        dst_checksum[1] = data[7];
        //받은 패킷에 대한 체크썸.
        // now check the checksum;
        if (checkingChecksum[0] == dst_checksum[0] && checkingChecksum[1] == dst_checksum[1]) { //비교한다
            return true;
        } else {
            return false;
        }
    }

    boolean sendRIP(byte[] data, byte[] udp_destinationAddress) {
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
        setChecksum(makeChecksum(data));
        udp_data[6] = udp_checksum[0];
        udp_data[7] = udp_checksum[1];


        //데이터 설정
        for (int i = 0; i < length; i++)
            udp_data[i + UDP_HEAD_SIZE] = data[i];


        if (((IPLayer) this.getUnderLayer()).sendUDP(udp_data, udp_destinationAddress)) {
            return true;
        } else
            return false;
    }

}
