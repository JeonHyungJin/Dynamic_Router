package Router;

public class RIPLayer extends BaseLayer {
    // RIP header
    // Version은 2만 사용한다고 가정
    byte[] rip_message;

    RoutingTable[] routingTable;

    UDPLayer otherUDPLayer;

    void setRoutingTable(RoutingTable[] routingTable) {
        this.routingTable = routingTable;
    }


    public RIPLayer(String layerName) {
        super(layerName);
    }

    public void setOtherUDPLayer(UDPLayer otherUDPLayer) {
        this.otherUDPLayer = otherUDPLayer;
    }

    public void receiveRIP(byte[] dataRIP, byte[] gateway) {
        // TODO Auto-generated method stub
        // table 업데이트

        if (dataRIP[0] == 0x01) {
            // request
            // 요청한 라우터에게 entire routing table을 보낸다.
            // 1. request에 대한 response - 들어온 곳
            IPLayer ipLayer = ((IPLayer) ((UDPLayer) this.getUnderLayer()).getUnderLayer());
            ipLayer.setDestinationIPAddress(gateway);

            rip_message = new byte[4 + 20 * routingTable.length];

            this.rip_message[0] = 0x02;
            this.rip_message[1] = 0x02;
            // routing table 내용을 entry로 하나씩 추가
            for (int i = 0; i < routingTable.length; i++) {
                // 모든걸 다 넣어야 하는가 ?
                rip_message[4 + 20 * i + 0] = 0x0002;
                rip_message[4 + 20 * i + 2] = 0x0001;
                System.arraycopy(routingTable[i].getDestination(), 0, rip_message, 4 + 20 * i + 4, 4);
                System.arraycopy(routingTable[i].getNetMask(), 0, rip_message, 4 + 20 * i + 8, 4);
                // next hop
                System.arraycopy(routingTable[i].getMetric(), 0, rip_message, 4 + 20 * i + 16, 4);
            }
            ((UDPLayer) this.getUnderLayer()).sendRIP(rip_message);
        } else if (dataRIP[0] == 0x02) {
            // response
            // 2. response에 대한 response - 반대
            byte[] broadcast = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
            // 255.255.255.255로 설정
            IPLayer ipLayer = (IPLayer) otherUDPLayer.getUnderLayer();
            ipLayer.setDestinationIPAddress(broadcast);

            // 패킷 처리 알고리즘 ~, 패킷내용보면서 엔트리 추가시 게이트웨이는 gateway로
            int length = dataRIP.length;
            int entry_count = (length - 4) / 20;

            byte[] networkAddress = new byte[4];
            byte[] netMask = new byte[4];
            int metric = 0;

            for (int i = 0; i < entry_count; i++) {
                // 들어온 엔트리 알고리즘에 맞춰 테이블 업데이트.
                System.arraycopy(dataRIP, 4 + 20 * i + 0, networkAddress, 0, networkAddress.length);
                System.arraycopy(dataRIP, 4 + 20 * i + 4, netMask, 0, netMask.length);
                //


                if (findRoutingEntry(networkAddress)) {
                }

            }
            otherUDPLayer.sendRIP(rip_message);
        }
    }

    public void initialization() {
        // 좀더 생각 해보기~
        // request -> response

        // timer 별로 송신

        // 내가 처음 연결 됬을 때 ?
        // initialization
        // broadcast로 송신
        // header 설정
        this.rip_message = new byte[4];
        this.rip_message[0] = 0x01;
        this.rip_message[1] = 0x02;

        byte[] broadcast = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

        // 255.255.255.255로 설정
        IPLayer ipLayer = ((IPLayer) ((UDPLayer) this.getUnderLayer()).getUnderLayer());

        ipLayer.setDestinationIPAddress(broadcast);
        // request 전송
        ((UDPLayer) this.getUnderLayer()).sendRIP(rip_message);
    }

    boolean findRoutingEntry(byte[] address) {
        // 테이블 내에 있는가 찾느것
        return true;

    }

    public int byteArrayToInt(byte bytes[]) {
        return ((((int) bytes[0] & 0xff) << 24) |
                (((int) bytes[1] & 0xff) << 16) |
                (((int) bytes[2] & 0xff) << 8) |
                (((int) bytes[3] & 0xff)));
    }

}