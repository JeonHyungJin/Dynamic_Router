package Router;

public class RIPLayer extends BaseLayer {
    // RIP header
    // Version은 2만 사용한다고 가정
    byte[] rip_message;
    byte[] ip_sourceIP = new byte[4];
    RoutingTable[] routingTable;
    private int routingIndex;

    int interfaceNumber;
    RIPLayer otherRIPLayer;

    void setRoutingTable(RoutingTable[] routingTable) {
        this.routingTable = routingTable;
    }


    public RIPLayer(String layerName) {
        super(layerName);
        routingIndex=0;
    }

    public void setOtherRIPLayer(RIPLayer otherRIPLayer) {
        this.otherRIPLayer = otherRIPLayer;
    }

    public void receiveRIP(byte[] dataRIP, byte[] gateway) {
        // TODO Auto-generated method stub
        // table 업데이트

        if (dataRIP[0] == 0x01) {
            // request
            // 요청한 라우터에게 entire routing table을 보낸다.
            // 1. request에 대한 response - 들어온 곳

            rip_message = new byte[4 + 20 * routingIndex];

            this.rip_message[0] = 0x02;
            this.rip_message[1] = 0x02;

            // request에는 상대방이 원하는 엔트리에 대한 정보가 온다.
            // routing table 내용을 entry로 하나씩 추가
            if( dataRIP.length == 4 ) {
                // initial request
                for (int i = 0; i < routingIndex; i++) {
                    // 모든걸 다 넣어야 하는가 ?
                    rip_message[4 + 20 * i + 0] = 0x0002;
                    rip_message[4 + 20 * i + 2] = 0x0001;
                    System.arraycopy(routingTable[i].getDestination(), 0, rip_message, 4 + 20 * i + 4, 4);
                    System.arraycopy(routingTable[i].getNetMask(), 0, rip_message, 4 + 20 * i + 8, 4);
                    // next hop
                    if (routingTable[i].getFlag() == Flag.UG) {
                        System.arraycopy(routingTable[i].getGateway(), 0, rip_message, 4 + 20 * i + 12, 4);
                    } else {
                        System.arraycopy(ip_sourceIP, 0, rip_message, 4 + 20 * i + 12, 4);
                    }
                    System.arraycopy(routingTable[i].getMetric(), 0, rip_message, 4 + 20 * i + 16, 4);
                }
            }else{
                // except initial
                // 나중에
               /* int entry_count = (dataRIP.length - 4) / 20;

                byte[] networkAddress = new byte[4];
                byte[] netMask = new byte[4];
                byte[] nexthop = new byte[4];
                byte[] metric_byte = new byte[4];
                int metric = 0;

                for ( int i = 0; i < entry_count; i ++){

                }*/
            }
            ((UDPLayer) this.getUnderLayer()).sendRIP(rip_message, gateway);
        } else if (dataRIP[0] == 0x02) {
            // response
            // 2. response에 대한 response - 반대

            // 패킷 처리 알고리즘 ~, 패킷내용보면서 엔트리 추가시 게이트웨이는 gateway로
            int length = dataRIP.length;
            int entry_count = (length - 4) / 20;

            byte[] networkAddress = new byte[4];
            byte[] netMask = new byte[4];
            byte[] nexthop = new byte[4];
            byte[] metric_byte = new byte[4];
            int metric = 0;

            for (int i = 0; i < entry_count; i++) {
                // 들어온 엔트리 알고리즘에 맞춰 테이블 업데이트.
                System.arraycopy(dataRIP, 4 + 20 * i + 0, networkAddress, 0, networkAddress.length);
                System.arraycopy(dataRIP, 4 + 20 * i + 4, netMask, 0, netMask.length);
                System.arraycopy(dataRIP, 4 + 20 * i + 8, nexthop, 0, nexthop.length);
                System.arraycopy(dataRIP, 4 + 20 * i + 12, metric_byte, 0, metric_byte.length);
                metric = byteArrayToInt( metric_byte) ;

                // 테이블 업데이트 중 ...

                int index = findRoutingEntry(networkAddress);
                if ( index == -1 ) {
                    // 추가
                    routingTable[routingIndex].setRoutingTable(networkAddress,netMask, gateway, Flag.UG, interfaceNumber, routingIndex, metric + 1 );
                    routingIndex++;
                }else{
                    int check_nextHop = 1;
                    // next_hop is the same
                    for( int j = 0; j < 4; j++){
                        if (nexthop[j] != routingTable[routingIndex].getGateway()[j]) {
                            check_nextHop = 0;
                            break;
                        }
                    }
                    if( check_nextHop != 0 ){
                        // Replace entry in the table
                        routingTable[index].setRoutingTable(networkAddress, netMask, gateway, Flag.UG, interfaceNumber, index, metric + 1);
                    }else{
                        if( routingTable[index].getMetric() > metric ){
                            // interfaceNumber 어떻게 구하지
                            routingTable[index].setRoutingTable(networkAddress, netMask, gateway, Flag.UG, interfaceNumber, index, metric + 1);

                        }else{
                            // do nothing
                        }
                    }
                }
            }
            // entry 업데이트 한 후 상대 인터페이스로 전달
            rip_message = new byte[4 + 20 * routingIndex];
            byte[] other_rip_message = new byte[4 + 20 * routingIndex];
            byte[] other_gateway = {(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff};
            int find_other = 0;
            this.rip_message[0] = 0x02;
            this.rip_message[1] = 0x02;
            this.rip_message[2] = 0x00;
            this.rip_message[3] = 0x00;

            other_rip_message[0] = 0x02;
            other_rip_message[1] = 0x02;
            other_rip_message[2] = 0x00;
            other_rip_message[3] = 0x00;

            // initial request
            byte[] max_hop = {(byte)0x00, 0x00,0x00, 0x10};

            for (int i = 0; i < routingIndex; i++) {
                // 모든걸 다 넣어야 하는가 ?
                // 현재 인터페이스
                // 상대 인터페이스의 게이트웨이 찾기
                // how ?

                rip_message[4 + 20 * i + 0] = 0x0002;
                rip_message[4 + 20 * i + 2] = 0x0001;
                System.arraycopy(routingTable[i].getDestination(), 0, rip_message, 4 + 20 * i + 4, 4);
                System.arraycopy(routingTable[i].getNetMask(), 0, rip_message, 4 + 20 * i + 8, 4);
                // next hop
                if (routingTable[i].getFlag() == Flag.UG) {
                    System.arraycopy(routingTable[i].getGateway(), 0, rip_message, 4 + 20 * i + 12, 4);
                } else {
                    System.arraycopy(ip_sourceIP, 0, rip_message, 4 + 20 * i + 12, 4);
                }
                // 상대 인터페이스
                other_rip_message[4 + 20 * i + 0] = 0x0002;
                other_rip_message[4 + 20 * i + 2] = 0x0001;
                System.arraycopy(routingTable[i].getDestination(), 0, other_rip_message, 4 + 20 * i + 4, 4);
                System.arraycopy(routingTable[i].getNetMask(), 0, other_rip_message, 4 + 20 * i + 8, 4);
                // next hop
                if (routingTable[i].getFlag() == Flag.UG) {
                    System.arraycopy(routingTable[i].getGateway(), 0, other_rip_message, 4 + 20 * i + 12, 4);
                } else {
                    System.arraycopy(ip_sourceIP, 0, other_rip_message, 4 + 20 * i + 12, 4);
                }

                if ( routingTable[i].getInterface() == interfaceNumber){
                    System.arraycopy(max_hop, 0, rip_message, 4 + 20 * i + 16, 4);
                    System.arraycopy(routingTable[i].getMetric(), 0, other_rip_message, 4 + 20 * i + 16, 4);
                }else{
                    System.arraycopy(routingTable[i].getMetric(), 0, rip_message, 4 + 20 * i + 16, 4);
                    System.arraycopy(max_hop, 0, other_rip_message, 4 + 20 * i + 16, 4);
                }
            }
            ((UDPLayer)this.getUnderLayer()).sendRIP( rip_message, gateway);
            ((UDPLayer) otherRIPLayer.getUnderLayer()).sendRIP(other_rip_message, other_gateway );
        }
    }

    public void sendRIP(){
        //router 들에게만
        // hum...

        // timer 별로 송신

    }

    public void initialization() {
        // 좀더 생각 해보기~
        // request -> response

        // 내가 처음 연결 됬을 때 ?
        // initialization
        // broadcast로 송신
        // header 설정
        // 처음 연결 됐을 때는 그냥 브로드캐스트로 송신만 하면 되는건지 아니면 보내고
        // 그에대한 응답을 또 기다려야 하는지?
        this.rip_message = new byte[4];
        this.rip_message[0] = 0x01;
        this.rip_message[1] = 0x02;
        this.rip_message[2] = 0x00;
        this.rip_message[3] = 0x00;

        byte[] broadcast = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
/*
        this.rip_message[4] = 0x0002;
        this.rip_message[6] = 0x0001;

        byte[] netmask ={(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00};
        System.arraycopy( ip_sourceIP , 0, rip_message, 8, 4);
        System.arraycopy( netmask, 0, rip_message, 12, 4);
        System.arraycopy(broadcast, 0, rip_message, 16, 4);
        System.arraycopy(0, 0, rip_message, 20, 4);
*/

        // request 전송
        ((UDPLayer) this.getUnderLayer()).sendRIP(rip_message, broadcast);
    }

    int findRoutingEntry(byte[] address) {
        // 테이블 내에 있는가 찾느것
        int check = 0;
        for (int i = routingIndex -1 ; i >= 0; i--) {
            byte[] destination = routingTable[i].getDestination();
            for (int j = 0; j < 4; j++) {
                if (destination[j] != address[j]) {
                    check = 0;
                    break;
                } else
                    check = 1;
            }
            if(check == 1){
                return i;
            }
        }
        return -1;

    }

    public int byteArrayToInt(byte bytes[]) {
        return ((((int) bytes[0] & 0xff) << 24) |
                (((int) bytes[1] & 0xff) << 16) |
                (((int) bytes[2] & 0xff) << 8) |
                (((int) bytes[3] & 0xff)));
    }

}