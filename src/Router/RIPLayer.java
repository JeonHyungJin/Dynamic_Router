package Router;

import java.util.Timer;
import java.util.TimerTask;

public class RIPLayer extends BaseLayer {
    // RIP header
    // Version은 2만 사용한다고 가정
    byte[] rip_message;
    byte[] ip_sourceIP = new byte[4];
    RoutingTable[] routingTable;

    private int routingIndex;

    int interfaceNumber = 0;
    RIPLayer otherRIPLayer;

    byte[] max_hop = {(byte)0x00, 0x00,0x00, 0x10};

    public void setRoutingTable(RoutingTable[] routingTable) {
        this.routingTable = routingTable;
    }

    public void setInterfaceNumber(int interfaceNumber) {
        this.interfaceNumber = interfaceNumber;
    }

    public RIPLayer(String layerName) {
        super(layerName);
        routingIndex=0;
    }

    public byte[] getIp_sourceIP() {
        return ip_sourceIP;
    }

    public void setIp_sourceIP(byte[] ip_sourceIP) {
        this.ip_sourceIP = ip_sourceIP;
    }

    public void setRoutingIndex(int routingIndex) {
        this.routingIndex = routingIndex;
    }

    public void setOtherRIPLayer(RIPLayer otherRIPLayer) {
        this.otherRIPLayer = otherRIPLayer;
    }

    public void receiveRIP(byte[] dataRIP, byte[] gateway) {
        // TODO Auto-generated method stub
        // table 업데이트

            if (dataRIP[0] == 0x01) {
                System.out.println("request");
                // request
                // 요청한 라우터에게 entire routing table을 보낸다.
                // 1. request에 대한 response - 들어온 곳

                // initial request
                if (dataRIP.length == 4) {
                    rip_message = new byte[4 + 20 * routingIndex];

                    this.rip_message[0] = 0x02;
                    this.rip_message[1] = 0x02;

                    for (int i = 0; i < routingIndex; i++) {
                        // 모든걸 다 // initial 이니
                        rip_message[4 + 20 * i + 1] = 0x0002;
                        rip_message[4 + 20 * i + 3] = 0x0001;
                        System.arraycopy(routingTable[i].getDestination(), 0, rip_message, 4 + 20 * i + 4, 4);
                        System.arraycopy(routingTable[i].getNetMask(), 0, rip_message, 4 + 20 * i + 8, 4);
                        // next h
                        System.arraycopy(ip_sourceIP, 0, rip_message, 4 + 20 * i + 12, 4);
                        System.arraycopy(intToByteArray(routingTable[i].getMetric()), 0, rip_message, 4 + 20 * i + 16, 4);
                    }
                } else {
                    // request에는 상대방이 원하는 엔트리에 대한 정보가 온다.
                    // routing table 내용을 entry로 하나씩 추가
                    int entry_count = (dataRIP.length - 4) / 20;
                    rip_message = new byte[4 + 20 * entry_count];
                    this.rip_message[0] = 0x02;
                    this.rip_message[1] = 0x02;
                    this.rip_message[2] = 0x00;
                    this.rip_message[3] = 0x00;

                    byte[] networkAddress = new byte[4];
                    for (int i = 0; i < entry_count; i++) {
                        System.arraycopy(dataRIP, 4 + 20 * i + 0, networkAddress, 0, networkAddress.length);
                        int index = findRoutingEntry(networkAddress);

                        if (index == -1) {
                            // 없어진 경우만 있다 가정
                            rip_message[4 + 20 * i + 1] = 0x0002;
                            rip_message[4 + 20 * i + 3] = 0x0001;
                            System.arraycopy(networkAddress, 0, rip_message, 4 + 20 * i + 4, 4);
                            System.arraycopy(routingTable[i].getNetMask(), 0, rip_message, 4 + 20 * i + 8, 4);
                            // next h
                            System.arraycopy(ip_sourceIP, 0, rip_message, 4 + 20 * i + 12, 4);
                            System.arraycopy(max_hop, 0, rip_message, 4 + 20 * i + 16, 4);
                        } else {
                            // expire timer 갱신
                            byte[] metric = intToByteArray(routingTable[index].getMetric());
                            routingTable[index].restartExpireTimer(interfaceNumber);
                            System.arraycopy(networkAddress, 0, rip_message, 4 + 20 * i + 4, 4);
                            System.arraycopy(routingTable[index].getNetMask(), 0, rip_message, 4 + 20 * i + 8, 4);
                            // next h
                            System.arraycopy(ip_sourceIP, 0, rip_message, 4 + 20 * i + 12, 4);
                            System.arraycopy(metric, 0, rip_message, 4 + 20 * i + 16, 4);
                        }
                    }
                }

                ((UDPLayer) this.getUnderLayer()).sendRIP(rip_message);
            } else if (dataRIP[0] == 0x02) {
                // 패킷 처리 알고리즘 ~, 패킷내용보면서 엔트리 추가시 게이트웨이는 gateway로
                int length = dataRIP.length;
                int entry_count = (length - 4) / 20;

                byte[] networkAddress = new byte[4];
                byte[] netMask = new byte[4];
                byte[] nexthop = new byte[4];
                byte[] metric_byte = new byte[4];
                int metric;

                int change_count = 0;
                byte[] temp_message = new byte[entry_count * 20];
                byte[] temp_other_rip_message = new byte[entry_count * 20];

                for (int i = 0; i < entry_count; i++) {
                    // 들어온 엔트리 알고리즘에 맞춰 테이블 업데이트.
                    System.arraycopy(dataRIP, 4 + 20 * i + 4, networkAddress, 0, networkAddress.length);
                    System.arraycopy(dataRIP, 4 + 20 * i + 8, netMask, 0, netMask.length);
                    System.arraycopy(dataRIP, 4 + 20 * i + 12, nexthop, 0, nexthop.length);
                    System.arraycopy(dataRIP, 4 + 20 * i + 16, metric_byte, 0, metric_byte.length);
                    metric = byteArrayToInt(metric_byte);

                    // 테이블 업데이트 중 ...
                    // metric 16인 경우 - 통신 불가, route poisoning
                    // 16인 경우 - poison reverse
                    // 변화가 생겼을 시, 즉각 전송
                    // 30초마다는 전체 전송,
                    // 전송된게 디폴트면 추가 안함.
                    byte[] default_entry = {0x00,0x00,0x00,0x00};
                    int default_way = 1;
                    for (int j = 0; j < 4; j++) {
                        if (default_entry[j] != networkAddress[j]) {
                            default_way = 0;
                            break;
                        }
                    }
                    if( default_way != 0 )
                        continue;

                    int index = findRoutingEntry(networkAddress);
                    if (index == -1) {
                        if (metric != 16) {
                            // 추가
                            routingTable[routingIndex] = new RoutingTable(networkAddress, netMask, nexthop, Flag.UG,
                                    interfaceNumber, routingIndex, metric + 1);
                            routingIndex++;
                            ApplicationLayer.ifTableChaged(0, routingIndex, interfaceNumber);

                            // 들온 곳에 보내는 경우
                            temp_message[20 * change_count + 1] = 0x0002;
                            temp_message[20 * change_count + 3] = 0x0001;
                            System.arraycopy(networkAddress, 0, temp_message, 20 * change_count + 4, 4);
                            System.arraycopy(netMask, 0, temp_message, 20 * change_count + 8, 4);
                            // next h
                            System.arraycopy(nexthop, 0, temp_message, 20 * change_count + 12, 4);
                            System.arraycopy(max_hop, 0, temp_message, 20 * change_count + 16, 4);


                            // 그 반대에 보내는 경우
                            temp_other_rip_message[20 * change_count + 1] = 0x0002;
                            temp_other_rip_message[20 * change_count + 3] = 0x0001;
                            System.arraycopy(networkAddress, 0, temp_other_rip_message, 20 * change_count + 4, 4);
                            System.arraycopy(netMask, 0, temp_other_rip_message, 20 * change_count + 8, 4);
                            // next h
                            System.arraycopy(ip_sourceIP, 0, temp_other_rip_message, 20 * change_count + 12, 4);
                            System.arraycopy(intToByteArray(metric + 1), 0, temp_other_rip_message, 20 * change_count + 16, 4);

                            change_count++;
                        }else{
                            System.out.printf("%d.%d.%d.%d \n",networkAddress[0],networkAddress[1],networkAddress[2],networkAddress[3]);
                        }
                    } else {
                        // expire timer 갱신
                        routingTable[index].restartExpireTimer(interfaceNumber);
                        int check_nextHop = 1;
                        // next_hop is the same
                        for (int j = 0; j < 4; j++) {
                            if (nexthop[j] != routingTable[index].getGateway()[j]) {
                                check_nextHop = 0;
                                break;
                            }
                        }
                        if (check_nextHop != 0) {
                            // 기존 테이블 엔트리 게이트 웨이와 비교하여 보니 바로 옆에 친구가 보내온 작은 테이블 엔트리다.
                            if (metric != 16) {
                                routingTable[index].setRoutingTable(networkAddress, netMask, nexthop, Flag.UG, interfaceNumber, index, metric + 1);
                                // 변화 되었으니 업데이트
                                ApplicationLayer.ifTableChaged(1, index, interfaceNumber);
                                System.out.printf("network: %d.%d.%d.%d\n", networkAddress[0],networkAddress[1],networkAddress[2],networkAddress[3]);
                                System.out.printf("network: %d.%d.%d.%d\n", netMask[0],netMask[1],netMask[2],netMask[3]);
                                System.out.printf("network: %d.%d.%d.%d\n", nexthop[0],nexthop[1],nexthop[2],nexthop[3]);
                                // 들온 곳에 보내는 경우
                                temp_message[20 * change_count + 1] = 0x0002;
                                temp_message[20 * change_count + 3] = 0x0001;
                                System.arraycopy(networkAddress, 0, temp_message, 20 * change_count + 4, 4);
                                System.arraycopy(netMask, 0, temp_message, 20 * change_count + 8, 4);
                                // next h
                                System.arraycopy(nexthop, 0, temp_message, 20 * change_count + 12, 4);
                                System.arraycopy(max_hop, 0, temp_message, 20 * change_count + 16, 4);
                                // 그 반대에 보내는 경우
                                temp_other_rip_message[20 * change_count + 1] = 0x0002;
                                temp_other_rip_message[20 * change_count + 3] = 0x0001;
                                System.arraycopy(networkAddress, 0, temp_other_rip_message, 20 * change_count + 4, 4);
                                System.arraycopy(netMask, 0, temp_other_rip_message, 20 * change_count + 8, 4);
                                // next h
                                System.arraycopy(ip_sourceIP, 0, temp_other_rip_message, 20 * change_count + 12, 4);
                                System.arraycopy(intToByteArray(metric + 1), 0, temp_other_rip_message, 20 * change_count + 16, 4);

                                change_count++;
                            } else {
                                // unreachable 된 경우
                                // 테이블 삭제와 주변(반대)에 전달.
                                for (int j = index; j < routingIndex - 1; j++) {
                                    routingTable[j] = routingTable[j + 1];
                                    routingTable[j].setRT_Index(j);
                                }
                                routingTable[routingIndex - 1] = null;
                                routingIndex--;

                                ApplicationLayer.ifTableChaged(2, i, interfaceNumber);

                                // 들온 곳에 보내는 경우
                                temp_message[20 * change_count + 1] = 0x0002;
                                temp_message[20 * change_count + 3] = 0x0001;
                                System.arraycopy(networkAddress, 0, temp_message, 20 * change_count + 4, 4);
                                System.arraycopy(netMask, 0, temp_message, 20 * change_count + 8, 4);
                                // next h
                                System.arraycopy(nexthop, 0, temp_message, 20 * change_count + 12, 4);
                                System.arraycopy(max_hop, 0, temp_message, 20 * change_count + 16, 4);

                                temp_other_rip_message[20 * change_count + 1] = 0x0002;
                                temp_other_rip_message[20 * change_count + 3] = 0x0001;
                                System.arraycopy(networkAddress, 0, temp_other_rip_message, 20 * change_count + 4, 4);
                                System.arraycopy(netMask, 0, temp_other_rip_message, 20 * change_count + 8, 4);
                                // next h
                                System.arraycopy(ip_sourceIP, 0, temp_other_rip_message, 20 * change_count + 12, 4);
                                System.arraycopy(max_hop, 0, temp_other_rip_message, 20 * change_count + 16, 4);

                                change_count++;
                            }
                        } else {
                            if (metric != 16) {
                                if (routingTable[index].getMetric() > metric) {
                                    routingTable[index].setRoutingTable(networkAddress, netMask, nexthop, Flag.UG, interfaceNumber, index, metric + 1);
                                    // 변경
                                    ApplicationLayer.ifTableChaged(1, index, interfaceNumber);

                                    // 들온 곳에 보내는 경우
                                    temp_message[20 * change_count + 1] = 0x0002;
                                    temp_message[20 * change_count + 3] = 0x0001;
                                    System.arraycopy(networkAddress, 0, temp_message, 20 * change_count + 4, 4);
                                    System.arraycopy(netMask, 0, temp_message, 20 * change_count + 8, 4);
                                    // next h
                                    System.arraycopy(nexthop, 0, temp_message, 20 * change_count + 12, 4);
                                    System.arraycopy(max_hop, 0, temp_message, 20 * change_count + 16, 4);

                                    temp_other_rip_message[20 * change_count + 1] = 0x0002;
                                    temp_other_rip_message[20 * change_count + 3] = 0x0001;
                                    System.arraycopy(networkAddress, 0, temp_other_rip_message, 20 * change_count + 4, 4);
                                    System.arraycopy(netMask, 0, temp_other_rip_message, 20 * change_count + 8, 4);
                                    // next h
                                    System.arraycopy(ip_sourceIP, 0, temp_other_rip_message, 20 * change_count + 12, 4);
                                    System.arraycopy(intToByteArray(metric + 1), 0, temp_other_rip_message, 20 * change_count + 16, 4);

                                    change_count++;
                                } else {
                                    // do nothing
                                }
                            } else {
                                // poison reverse 에 해당하는 경우
                                // do nothing
                                System.out.printf("poison network: %d.%d.%d.%d\n", networkAddress[0],networkAddress[1],networkAddress[2],networkAddress[3]);

                            }
                        }

                }


            }
                System.out.println("바뀐 엔트리 개수 : " + change_count);

                if (change_count != 0) {
                    // 변화가 생긴 경우, 즉시 전송
                    rip_message = new byte[4 + 20 * change_count];
                    byte[] other_rip_message = new byte[4 + 20 * change_count];

                    this.rip_message[0] = 0x02;
                    this.rip_message[1] = 0x02;
                    this.rip_message[2] = 0x00;
                    this.rip_message[3] = 0x00;

                    other_rip_message[0] = 0x02;
                    other_rip_message[1] = 0x02;
                    other_rip_message[2] = 0x00;
                    other_rip_message[3] = 0x00;

                    System.arraycopy(temp_message, 0, rip_message, 4, 20 * change_count);
                    System.arraycopy(temp_other_rip_message, 0, other_rip_message, 4, 20 * change_count);

                    ((UDPLayer) this.getUnderLayer()).sendRIP(rip_message);
                    ((UDPLayer) otherRIPLayer.getUnderLayer()).sendRIP(other_rip_message);
                }
        }
    }

    public void runTimers(){
        Timer timer = new Timer();
        TimerTask periodic_timer = new TimerTask() {
            @Override
            public void run() {
                //주기적으로 실행되는 부분
                byte[] timer_rip_message = new byte[ 4 + 20 * routingIndex ];
                timer_rip_message[0] = 0x02;
                timer_rip_message[1] = 0x02;
                timer_rip_message[2] = 0x00;
                timer_rip_message[3] = 0x00;
                    for (int i = 0; i < routingIndex; i++) {
                        timer_rip_message[4 + 20 * i + 1] = 0x0002;
                        timer_rip_message[4 + 20 * i + 3] = 0x0001;
                        System.arraycopy(routingTable[i].getDestination(), 0, timer_rip_message, 4 + 20 * i + 4, 4);
                        System.arraycopy(routingTable[i].getNetMask(), 0, timer_rip_message, 4 + 20 * i + 8, 4);
                        byte[] metric = intToByteArray(routingTable[i].getMetric());
                        if (routingTable[i].getInterface() == interfaceNumber) {
                            // 나랑 연결된 곳으로 가야함.
                            System.arraycopy(routingTable[i].getGateway(), 0, timer_rip_message, 4 + 20 * i + 12, 4);
                            System.arraycopy(max_hop, 0, timer_rip_message, 4 + 20 * i + 16, 4);
                        } else {
                            // 그 반대 방향
                            System.arraycopy(ip_sourceIP, 0, timer_rip_message, 4 + 20 * i + 12, 4);
                            // metric 배열로 고치기
                            System.arraycopy(metric, 0, timer_rip_message, 4 + 20 * i + 16, 4);
                        }
                        // next h
                    }
                // 전송~
                ((UDPLayer)getUnderLayer()).sendRIP( timer_rip_message );
            }
        };
        timer.schedule(periodic_timer,30000,30000);  //sendRIP함수가 호출되면 0초후부터 run()함수 부분이 30초 마다 실행된다.
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

        // request 전송
        ((UDPLayer) this.getUnderLayer()).sendRIP(rip_message);
        runTimers();
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

    public byte[] intToByteArray(int i){
        byte[] temp = new byte[4];
        temp[0] = (byte) ( i >> 24 );
        temp[1] = (byte) ( i >> 16 );
        temp[2] = (byte) ( i >> 8 );
        temp[3] = (byte) ( i  );
        return temp;
    }

    public void sendUnreachable(int index) {
        byte[] unreachable = new byte[4+20];

        unreachable[0] = 0x02;
        unreachable[1] = 0x02;
        unreachable[2] = 0x00;
        unreachable[3] = 0x00;
        unreachable[4 + 1] = 0x0002;
        unreachable[4 + 3] = 0x0001;
            System.arraycopy(routingTable[index].getDestination(), 0, unreachable, 4 + 4, 4);
            System.arraycopy(routingTable[index].getNetMask(), 0, unreachable, 4 + 8, 4);
        // next h
        System.arraycopy(ip_sourceIP, 0, unreachable, 4 + 12, 4);
        System.arraycopy(max_hop, 0, unreachable, 4 + 16, 4);


        ((UDPLayer) this.getUnderLayer()).sendRIP(unreachable);
    }
}