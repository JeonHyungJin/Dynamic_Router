package Router;

import java.util.Arrays;

import static Router.EthernetLayer.ETHERNET_HEAD_SIZE;

public class IPLayer extends BaseLayer {
	final static int IP_HEAD_SIZE = 20;

	byte[] ip_head = new byte[IP_HEAD_SIZE];
	byte[] ip_sourceIP = new byte[4];
	byte[] ip_destinationIP = new byte[4];
	byte[] ip_data;

	int interfaceNumber;

	IPLayer otherIPLayer;

	RoutingTable[] routingTable;

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
		// UDP에서 내려오는 RIP response request 등
		int length = data.length;
		byte[] broadcast = {(byte) 0xff,(byte) 0xff,(byte)  0xff,(byte) 0xff};

		ip_data = new byte[data.length + IP_HEAD_SIZE];

		// Destination 주소 설정은 RIPLayer에서 완료
		// 어느 Layer로 가야할지 택.
		if( data[0] == 0x01 ) // request면 직접 연결된 router에게 broadcast로 전송
		{
			// 고로 routing 테이블를 통해 직접 연결된 router가 어느 인터페이스인지를 파악해야함
			for (int i = 0; i < routingTable.length; i++) {
				Flag flag = routingTable[i].getFlag();

				if (flag == Flag.U) {
					// flag가 U 인 곳에만 보내면 된다.
					if (interfaceNumber == routingTable[i].getInterface()) {
						((ARPLayer) this.getUnderLayer()).send(ip_data, routingTable[i].getGateway());
					} else {
						((ARPLayer) otherIPLayer.getUnderLayer()).send(ip_data, routingTable[i].getGateway());
					}
				}
			}
		}else{	//response의 경우
			// response면 destination의 라우터에게만 전송

			((ARPLayer) this.getUnderLayer()).send(ip_data, this.ip_destinationIP);

		}
		return true;
	}
	
	boolean receiveIP(byte[] data) {
		// RIP 패킷인지 확인을 위해, IP header의 protocol 값이 17인가 확인이 필요

		ip_data = new byte[data.length];
		byte[] frame_dst_ip = new byte[4];
		frame_dst_ip[0] = data[16];
		frame_dst_ip[1] = data[17];
		frame_dst_ip[2] = data[18];
		frame_dst_ip[3] = data[19];

		if( data[10] == 0x00 && data[11] == 0x11 ){
			// UDP 레이어
			byte[] udpData = Arrays.copyOfRange(data, IP_HEAD_SIZE, data.length);

			((UDPLayer)this.getUpperLayer()).receiveUDP(udpData, frame_dst_ip);
		}else{
			// 데이터
			System.arraycopy(data, 0, ip_data, 0, data.length);

			int check = 0;
			// routing table 확인하여 알맞은 인터페이스에 연결
			for (int i = 0; i < ((ApplicationLayer) this.getUpperLayer()).routingIndex; i++) {
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
					// 체크가 1일때 인터페이스 번호가 라우팅 테이블에 있는 인터페이스라면 현재 거의 하위레이어에 보내고
					// 아니면 다른 아이피레이어의 하위 레이어로 보낸다. //라우터의 다른부분.
					return true;
				}
			}
		}


		return false;
	}
	
	public static int byte2Int(byte[] src)
    {
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
			return true;
		}
		check = 0;
		// routing table 확인하여, 다른 네트워크인 경우 나를 알리고, 해당 네트워크에 새로운 reply 전송
		for (int i = 0; i < ((ApplicationLayer) this.getUpperLayer()).routingIndex; i++) {
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
}