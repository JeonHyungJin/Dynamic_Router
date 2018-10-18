package Router;

import java.util.Arrays;

public class IPLayer extends BaseLayer {
    final static int IP_HEAD_SIZE = 20;

    byte[] ip_head = new byte[IP_HEAD_SIZE];
    byte[] ip_sourceIP = new byte[4];
    byte[] ip_destinationIP = new byte[4];
    byte[] ip_data;
    byte[] ip_checksum = new byte[2];

    int interfaceNumber;

    IPLayer otherIPLayer;

    RoutingTable[] routingTable;
    private int routingIndex;

    public IPLayer(String layerName) {
        super(layerName);
    }

    void setOtherIPLayer(IPLayer other) {
        otherIPLayer = other;
    }

    void setInterfaceNumber(int number) {
        interfaceNumber = number;
    }

    void setRoutingTable(RoutingTable[] routingTable) {
        this.routingTable = routingTable;
    }

    void setSourceIpAddress(byte[] sourceAddress) {
        for (int i = 0; i < 4; i++)
            ip_sourceIP[i] = sourceAddress[i];
    }

    void setDestinationIPAddress(byte[] destinationAddress) {
        for (int i = 0; i < 4; i++)
            ip_destinationIP[i] = destinationAddress[i];
    } //여기까지는 set해주는 부분들.

    boolean sendUDP(byte[] data) {
        int length = data.length;
        ip_data = new byte[data.length + IP_HEAD_SIZE];
        // encapsulation
        // version & IHL
        ip_data[0] = 0x45;
        // TOS
        ip_data[1] = 0x00;
        // Total Packet Length ( 16 bit )
        ip_data[2] = (byte) (ip_data.length / 0xFF);
        ip_data[3] = (byte) (ip_data.length % 0xFF);
        // identification
        ip_data[4] = 0x00;
        ip_data[5] = 0x01;
        // 3 bit flags & 13 bit fragment offset
        ip_data[6] = 0x00;
        ip_data[7] = 0x00;
        // TTL
        ip_data[8] = 0x04;
        // protocol
        ip_data[9] = 0x11; //UDP protocol = 17
        //checksum
        for (int i = 0; i < 16; i = i + 2) {
            ip_data[10] += ip_data[i];
            ip_data[11] += ip_data[i + 1];
        }
        //final checksum ( 보수 취하기 )
        ip_data[10] = makeChecksum(data)[0];
        ip_data[11] = makeChecksum(data)[1];
        ip_data[10] = (byte) (~ip_data[10]);
        ip_data[11] = (byte) (~ip_data[11]);

        // Source
        ip_data[12] = ip_sourceIP[0];
        ip_data[13] = ip_sourceIP[1];
        ip_data[14] = ip_sourceIP[2];
        ip_data[15] = ip_sourceIP[3];
        // destination
        int check = 0;
        for (int i = routingIndex - 1; i >= 0; i--) {
            byte[] destination = routingTable[i].getDestination();
            for (int j = 0; j < 4; j++) {
                byte[] netMask = routingTable[i].getNetMask();
                if (destination[j] != (netMask[j] & ip_sourceIP[j])) {
                    check = 0;
                    break;
                } else
                    check = 1;
            }
            if (check == 1) {
                if (routingTable[i].getFlag() != Flag.UG) {
                    return false;
                }
                // Destination
                ip_data[16] = routingTable[i].getGateway()[0];
                ip_data[17] = routingTable[i].getGateway()[1];
                ip_data[18] = routingTable[i].getGateway()[2];
                ip_data[19] = routingTable[i].getGateway()[3];
                // 전송 할 data를 Ethernet frame으로 복사
                for (int k = 0; k < length; k++)
                    ip_data[k + IP_HEAD_SIZE] = data[k];
                // 바로 센딩
                return ((ARPLayer) this.getUnderLayer()).send(ip_data, routingTable[i].getGateway());
            }
        }
        return false;
    }

    boolean receiveIP(byte[] data) {
        // RIP 패킷인지 확인을 위해, IP header의 protocol 값이 17인가 확인이 필요

        ip_data = new byte[data.length];
        byte[] frame_dst_ip = new byte[4];
        frame_dst_ip[0] = data[16];
        frame_dst_ip[1] = data[17];
        frame_dst_ip[2] = data[18];
        frame_dst_ip[3] = data[19];

        if (data[9] == 0x11) {
            // UDP 레이어
            byte[] frame_src_ip = new byte[4];

            frame_src_ip[0] = data[12];
            frame_src_ip[1] = data[13];
            frame_src_ip[2] = data[14];
            frame_src_ip[3] = data[15];

            byte[] udpData = Arrays.copyOfRange(data, IP_HEAD_SIZE, data.length);
            if (!((UDPLayer) this.getUpperLayer()).receiveUDP(udpData, frame_src_ip, frame_dst_ip)) {
                int check = 0;
                for (int i = routingIndex - 1; i >= 0; i--) {
                    byte[] destination = routingTable[i].getDestination();
                    for (int j = 0; j < 4; j++) {
                        byte[] netMask = routingTable[i].getNetMask();
                        if (destination[j] != (netMask[j] & frame_dst_ip[j])) {
                            check = 0;
                            break;
                        } else
                            check = 1;
                    }
                    if (check == 1) {
                        if (interfaceNumber == routingTable[i].getInterface()) {
                            ((ARPLayer) this.getUnderLayer()).send(data, routingTable[i].getGateway());
                        } else {
                            ((ARPLayer) otherIPLayer.getUnderLayer()).send(data, routingTable[i].getGateway());
                        }
                        return true;
                    }
                }
            }
        } else {
            // 데이터
            System.arraycopy(data, 0, ip_data, 0, data.length);

            int check = 0;
            // routing table 확인하여 알맞은 인터페이스에 연결
            for (int j = 0; j < 4; j++) {
                if (ip_sourceIP[j] != frame_dst_ip[j]) {
                    check = 0;
                    break;
                } else
                    check = 1;
            }
            if (check == 1)
                return true;
            for (int i = routingIndex - 1; i >= 0; i--) {
                byte[] destination = routingTable[i].getDestination();
                for (int j = 0; j < 4; j++) {
                    byte[] netMask = routingTable[i].getNetMask();
                    if (destination[j] != (netMask[j] & frame_dst_ip[j])) {
                        check = 0;
                        break;
                    } else
                        check = 1;
                } //서브넷 마스크와 아이피를 앤드 연산을 하고 그것이 데스티네이션과 같지않다면 체크가 0, 있으면 1 로 한다.
                if (check == 1) {
                    if (interfaceNumber == routingTable[i].getInterface()) {
                        ((ARPLayer) this.getUnderLayer()).send(ip_data, routingTable[i].getGateway());
                    } else {
                        ((ARPLayer) otherIPLayer.getUnderLayer()).send(ip_data, routingTable[i].getGateway());
                    }
                    return true;
                } else {
                    System.out.println("테이블에 저장되지 않은 목적지");
                }
            }
        }

        return false;
    }

    public static int byte2Int(byte[] src) {
        int s1 = src[0] & 0xFF;
        int s2 = src[1] & 0xFF;

        return ((s1 << 8) + (s2 << 0));
    }

    /*
     * EthernetLayer에서 수신한 ARP 패킷 처리
     * - 자신에 대한 ARP라면, ARP_reply_send 호출
     */
    boolean receiveARP(byte[] data) {
        int check = 1;
        for (int i = 0; i < 4; i++) {
            if (ip_sourceIP[i] != data[i + 24]) {
                check = 0;
                break;
            }
        }
        if (check == 1) {
            ((ARPLayer) this.getUnderLayer()).ARP_reply_send(data);
            System.out.println("reply 왔어용");
            return true;
        }
        check = 0;
        // routing table 확인하여, 다른 네트워크인 경우 나를 알리고, 해당 네트워크에 새로운 reply 전송
        for (int i = routingIndex - 1; i >= 0; i--) {
            byte[] destination = routingTable[i].getDestination();
            for (int j = 0; j < 4; j++) {
                byte[] netMask = routingTable[i].getNetMask();
                if (destination[j] != (netMask[j] & data[j + 24])) {
                    check = 0; //같지않으면 0으로 설정.
                    break;
                } else
                    check = 1; //같으면 1로 설정.
            }
            if (check == 1) { // 앤드연산을 하고 데스티네이션이랑 비교했을때 같으면 !
                if (interfaceNumber != routingTable[i].getInterface()) {
                    // routing table에 있는 정보라면, 자신이 arp_request에 응답을 하고, 다른 인터페이스에 arp 전송
                    ((ARPLayer) this.getUnderLayer()).ARP_reply_send(data);
                    ((ARPLayer) otherIPLayer.getUnderLayer()).ARP_request_send(routingTable[i].getGateway());
                } else {
                    // 나도 아니고, 그렇다고 다른 네트워크의 PC도 아닌것에 대한 arp reply를 응답하는 게 이해가 안간다.
                    ((ARPLayer) this.getUnderLayer()).ARP_reply_send(data);
                }
                return true;
            }
        }
        ((ARPLayer) this.getUnderLayer()).ARP_reply_send(data);
        return false;
    }

    public void setRoutingIndex(int routingIndex) {
        this.routingIndex = routingIndex;
    }

    public byte[] getConnectedRouter(byte[] sourceIP) {
        int check = 0;
        for (int i = routingIndex - 1; i >= 0 && check != 1; i--) {
            byte[] destination = routingTable[i].getDestination();
            for (int j = 0; j < 4; j++) {
                byte[] netMask = routingTable[i].getNetMask();
                if (destination[j] != (netMask[j] & sourceIP[j])) {
                    check = 0;
                    break;
                } else {
                    check = 1;
                }
            }
            if (check == 1)
                return routingTable[i].getGateway();
        }
        return null;
    }

    public byte[] makeChecksum(byte[] data) {
        ip_checksum[0] += 0x45; //version, header_length
        ip_checksum[1] += 0x00; // TOS
        // total_length
        ip_checksum[0] += (data.length + 8) / 256;
        ip_checksum[1] += (data.length + 8) % 256;
        //identification
        ip_checksum[0] += 0x00;
        ip_checksum[1] += 0x01;
        //flag & fragment offset
        ip_checksum[0] += 0x00;
        ip_checksum[1] += 0x00;
        // TTL & protocol
        ip_checksum[0] += 0x04; //??
        ip_checksum[1] += 0x11;
        //source ip address
        ip_checksum[0] += ip_sourceIP[0];
        ip_checksum[1] += ip_sourceIP[1];
        ip_checksum[0] += ip_sourceIP[2];
        ip_checksum[1] += ip_sourceIP[3];
        //destination ip address
        ip_checksum[0] += ip_destinationIP[0];
        ip_checksum[1] += ip_destinationIP[1];
        ip_checksum[0] += ip_destinationIP[2];
        ip_checksum[1] += ip_destinationIP[3];

        ip_checksum[0] = (byte) (~ip_checksum[0]);
        ip_checksum[1] = (byte) (~ip_checksum[1]);

        //헤더만 더해요 ㅎㅎㅎ
        return ip_checksum;
    }

    boolean checkCheckSum(byte[] data) {
        byte[] noheaderData = new byte[data.length - IP_HEAD_SIZE];
        System.arraycopy(data, 20, noheaderData, 0, noheaderData.length); //짤라서

        byte[] checkingChecksum = new byte[2];
        checkingChecksum[0] = makeChecksum(noheaderData)[0];
        checkingChecksum[1] = makeChecksum(noheaderData)[1];

        byte[] dst_checksum = new byte[2]; //오리지널과
        dst_checksum[0] = data[10];
        dst_checksum[1] = data[11];
        //받은 패킷에 대한 체크썸.
        // now check the checksum;

        if (checkingChecksum[0] == dst_checksum[0] && checkingChecksum[1] == dst_checksum[1]) { //비교한다
            return true;
        } else {
            return false;
        }
    }
}