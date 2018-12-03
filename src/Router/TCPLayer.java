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
     * @param sourcePort : NAT 입장에선 바뀐 PORT로 설정해준다.
     */
    void setSourcePort(byte[] sourcePort) {
        for (int i = 0; i < 2; i++)
            tcp_sourcePort[i] = sourcePort[i];
    }

    void setDestinationPort(byte[] destinationPort) {
        for (int i = 0; i < 2; i++)
            tcp_destinationPort[i] = destinationPort[i];
    }

    /**
     * port가 바뀌므로 체크썸도 그에 맞춰 변경 시켜줘야한다.
     */
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
        checksumFirst[0] += (byte) 0x00;
        checksumFirst[0] += (byte) (((data.length + 8)) / 0xFF);
        ;
        checksumSecond[0] += (byte) 0x11; //Protocol
        checksumSecond[0] += (byte) (((data.length + 8)) % 0xFF);
        ;


        //UDP sourcePort & destinationPort
        checksumFirst[0] += tcp_sourcePort[0];
        checksumFirst[0] += tcp_destinationPort[0];
        checksumSecond[0] += tcp_sourcePort[1];
        checksumSecond[0] += tcp_destinationPort[1];

        //UDP Length
        checksumFirst[0] += (byte) (((data.length + 8)) / 0xFF);
        ;
        checksumSecond[0] += (byte) (((data.length + 8)) % 0xFF);
        ;

        //UDP Data
        for (int i = 0; i < data.length; i = i + 2) {
            if (i == data.length - 1) {
                checksumFirst[0] += data[i];
            } else {
                checksumFirst[0] += data[i]; //홀수 인덱스끼리 더한다.
                checksumSecond[0] += data[i + 1]; //짝수 인덱스끼리 더한다.
            }
        }

        //보수를 취한다. 되네!^^
        checksum[0] = (byte) (~checksumFirst[0]);
        checksum[1] = (byte) (~checksumSecond[0]);


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
        tcp_checksum[0] = checksum[0];
        tcp_checksum[1] = checksum[1];
    }

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
            ((RIPLayer) this.getUpperLayer()).convertToOriginal(destinationIP, dst_port);
        else{
            ((RIPLayer) this.getUpperLayer()).receiveNAT(sourceIP, src_port, destinationIP, dst_port);
        }

        ((IPLayer)this.underLayer).setSourceIpAddress(sourceIP);
        ((IPLayer)this.underLayer).setDestinationIPAddress(destinationIP);

        setSourcePort(src_port);
        setDestinationPort(dst_port);
        makeChecksum(data, sourceIP, destinationIP);

        // 이상 없는 데이터
        // 여기 까지 올라온 패킷은 NAT의 기능을 누리려는 친구들
        // 고로 이 상위인 RoutingModule ( 아직은 RIPLayer) 로 넘겨서 NAT 된 체로 넘어간다.

        // return RoutingModule.translation(data)

        return true;
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
        byte[] noheaderData = new byte[data.length - TCP_HEAD_SIZE];
        System.arraycopy(data, 8, noheaderData, 0, noheaderData.length); //짤라서

        byte[] checkingChecksum = new byte[2];
        checkingChecksum[0] = makeChecksum(noheaderData, sourceIP, destinationIP)[0];
        checkingChecksum[1] = makeChecksum(noheaderData, sourceIP, destinationIP)[1];

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

    boolean sendTCP(byte[] data) {
        int length = data.length;
        byte[] destinationPort = {(byte) 0x02, 0x08};
        byte[] sourcePort = {(byte) 0x02, 0x08}; //임의로 넣어 놓은 것.
        tcp_data = new byte[data.length + TCP_HEAD_SIZE];

        // encapsulation

        //UDP header 설정

        //udp source port 설정, 더 고민해봐야함.
        setSourcePort(sourcePort);
        tcp_data[0] = tcp_sourcePort[0];
        tcp_data[1] = tcp_sourcePort[1];

        //udp destination port 설정
        setDestinationPort(destinationPort);
        tcp_data[2] = tcp_destinationPort[0];
        tcp_data[3] = tcp_destinationPort[1];

        //length를 설정 해야할까요..? ㅎㅎ

        /*checksum을 udp_data header에 추가한다*/
        byte[] tempSource = ((IPLayer) this.getUnderLayer()).ip_sourceIP;
        byte[] tempDestination = ((IPLayer) this.getUnderLayer()).getConnectedRouter(((IPLayer) this.getUnderLayer()).ip_sourceIP);
        byte[] broadcast = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        if (tempDestination == null) {
            setChecksum(makeChecksum(data, tempSource, broadcast));

        } else {
            setChecksum(makeChecksum(data, tempSource, tempDestination));

        }
        tcp_data[6] = tcp_checksum[0];
        tcp_data[7] = tcp_checksum[1];


        //데이터 설정
        for (int i = 0; i < length; i++)
            tcp_data[i + TCP_HEAD_SIZE] = data[i];

        if (((IPLayer) this.getUnderLayer()).sendUDP(tcp_data)) {
            return true;
        } else
            return false;
    }

}