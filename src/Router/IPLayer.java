package Router;

import java.util.Arrays;

public class IPLayer extends BaseLayer {
	final static int IP_HEAD_SIZE = 20;

	byte[] ip_head = new byte[IP_HEAD_SIZE];
	byte[] ip_sourceIP = new byte[4];
	byte[] ip_destinationIP = new byte[4];
	byte[] ip_data;

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
		// destination Address 있어야하나 ?
		// UDP에서 내려오는 RIP response request 등
		int length = data.length;

		ip_data = new byte[data.length + IP_HEAD_SIZE];
		// ip checksum도 필요
		// encapsulation
		// version & IHL
		ip_data[0] = 0x45;
		// TOS
		ip_data[1] = 0x00;
		// Total Packet Length ( 16 bit )
		ip_data[2] = (byte) ip_data.length;
		// identification

		// flags

		// 13 bit fragment offset

		// TTL

		// protocol
		ip_data[9] = 0x11;

		// Source
		ip_data[12] = ip_sourceIP[0];
		ip_data[13] = ip_sourceIP[1];
		ip_data[14] = ip_sourceIP[2];
		ip_data[15] = ip_sourceIP[3];

		// header checksum
		// 헤더 체크섬 만들기


		// Destination 주소 설정은 RIPLayer에서 완료
		// 어느 Layer로 가야할지 택.

		// 내가 있는 네트워크의 라우터로 보내면 된다.
		// destination
		int check = 0;
		for (int i = routingIndex -1 ; i >= 0; i--) {
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
					System.out.println("여기 나오면 이상한거");
					return false;
				}
				// Destination
				ip_data[16] = routingTable[i].getGateway()[0];
				ip_data[17] = routingTable[i].getGateway()[1];
				ip_data[18] = routingTable[i].getGateway()[2];
				ip_data[19] = routingTable[i].getGateway()[3];


				// 전송 할 data를 Ethernet frame으로 복사
				for (int k = 0; k < length; k++)
					ip_data[k+ IP_HEAD_SIZE] = data[k];

				// 바로 센딩
				return ((ARPLayer) this.getUnderLayer()).send(ip_data, routingTable[i].getGateway() );
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

		if( data[9] == 0x11 ){
			// UDP 레이어
			byte[] frame_src_ip = new byte[4];

			frame_src_ip[0] = data[12];
			frame_src_ip[1] = data[13];
			frame_src_ip[2] = data[14];
			frame_src_ip[3] = data[15];

			byte[] udpData = Arrays.copyOfRange(data, IP_HEAD_SIZE, data.length);

			((UDPLayer)this.getUpperLayer()).receiveUDP(udpData, frame_src_ip);
		}else{
			// 데이터
			System.arraycopy(data, 0, ip_data, 0, data.length);

			int check = 0;
			// routing table 확인하여 알맞은 인터페이스에 연결
			for (int i = routingIndex -1 ; i >= 0; i--) {
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


					/*if( routingTable[i].getFlag() == Flag.UG) { // 갈곳이 Gateway, 즉 router를 한번더 거쳐야한다면, gateway 주소로
						if (interfaceNumber == routingTable[i].getInterface()) {
							((ARPLayer) this.getUnderLayer()).send(ip_data, routingTable[i].getGateway());

						} else {
							((ARPLayer) otherIPLayer.getUnderLayer()).send(ip_data, routingTable[i].getGateway());
						}
					}
					else if( routingTable[i].getFlag() == Flag.U || routingTable[i].getFlag() == Flag.UH){ // 아니면, 직접 연결된 상태니까, 바로 목적지로 전송
						if (interfaceNumber == routingTable[i].getInterface()) {
							((ARPLayer) this.getUnderLayer()).send(ip_data, frame_dst_ip);

						} else {
							((ARPLayer) otherIPLayer.getUnderLayer()).send(ip_data, frame_dst_ip);
						}
					}*/
					// 체크가 1일때 인터페이스 번호가 라우팅 테이블에 있는 인터페이스라면 현재 거의 하위레이어에 보내고
					// 아니면 다른 아이피레이어의 하위 레이어로 보낸다. //라우터의 다른부분.
					return true;
				}else{
					System.out.println("테이블에 저장되지 않은 목적지");
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
			System.out.println("reply 왔어용");
			return true;
		}
		check = 0;
		// routing table 확인하여, 다른 네트워크인 경우 나를 알리고, 해당 네트워크에 새로운 reply 전송
		for (int i = routingIndex -1 ; i >= 0; i--) {
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
}