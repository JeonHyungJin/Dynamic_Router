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

    public byte[] makeChecksum(byte[] data) { // SHA-512 암호화

        MessageDigest digest;
        byte[] checksum = null;
        try {
            digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(data);
            byte[] hiddenData = digest.digest();// 암호화 시킴
            checksum = Arrays.copyOfRange(hiddenData, 0, 2);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return checksum; //checksum 리턴
    }

    void setChecksum(byte[] checksum) { //checksum 헤더에 넣어요
        udp_checksum[0] = checksum[0];
        udp_checksum[1] = checksum[1];
    }

    // length & checksum 나중쓰
    void setLength(byte[] data) {
        if ((byte) (data.length + 8) < (byte) 0xFFFF) {
            udp_length[0] = (byte) 0x00;
            udp_length[1] = (byte) (data.length + 8);
        } else {
            udp_length[0] = (byte) (((byte) (data.length + 8)) / (byte) 0xFFFF);
            udp_length[1] = (byte) (((byte) (data.length + 8)) % (byte) 0xFFFF);
        }

    }

    boolean receiveUDP(byte[] data) {
        if (checkChecksum(data)) {
            byte[] dst_port = new byte[2];
            // byte-order 고민한번쯤은~
            dst_port[0] = data[2];
            dst_port[1] = data[3];

            if (dst_port[0] == 0x02 && dst_port[1] == 0x08) {
                // rip 프로토콜 인거~
                byte[] dataRIP = new byte[data.length - UDP_HEAD_SIZE];
                System.arraycopy(data, 8, dataRIP, 0, dataRIP.length);

                ((RIPLayer) this.getUpperLayer()).receiveRIP(dataRIP);
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
        checkingChecksum = makeChecksum(noheaderData); //암호화시키고

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

    boolean sendUDP(byte[] data) {
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


        if (((IPLayer) this.getUnderLayer()).sendUDP(udp_data)) {
            return true;
        } else
            return false;
    }

}
