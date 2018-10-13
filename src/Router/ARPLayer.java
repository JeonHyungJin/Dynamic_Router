package Router;

import java.util.Arrays;

public class ARPLayer extends BaseLayer {
	final static int ARP_MAX_SIZE = 28;
	final static int ARP_IP_SIZE = 4;
	final static int ARP_ETH_SIZE = 6;
	final static int ARP_STATE_SIZE = 1;
	final static int ARP_TABLE_SIZE = ARP_IP_SIZE + ARP_ETH_SIZE + ARP_STATE_SIZE;
	final static int ARP_DEVICE_NAME = 10;
	final static int ARP_PROXY_TOTALSIZE = ARP_IP_SIZE + ARP_ETH_SIZE + ARP_DEVICE_NAME;
	final static int ARP_PROXY_SIZE = ARP_IP_SIZE + ARP_ETH_SIZE + ARP_STATE_SIZE;

	byte[] ARP_hardtype;
	byte[] ARP_prototype;
	byte[] ARP_hardsize;
	byte[] ARP_protosize;
	byte[] ARP_OP;
	byte[] ARP_senderEthAddr;
	byte[] ARP_senderIPAddr;
	byte[] ARP_targetEthAddr;
	byte[] ARP_targetIPAddr;

	byte[][] ARPCacheTable;
	byte[][] ARPProxyTable = new byte[255][ARP_PROXY_TOTALSIZE];

	static int ARPCacheTableCount = 0;
	static int ARPProxyTable_count = 0;

	public ARPLayer(String layerName) {
		super(layerName);
		resetARP();
	}

	public void set_ARPTable(byte[][] ARPCacheTable) {
		this.ARPCacheTable = ARPCacheTable;
	}

	public void resetARP() {
		ARP_hardtype = new byte[2];
		ARP_hardtype[0] = 0x00;
		ARP_hardtype[1] = 0x01;
		ARP_prototype = new byte[2];
		
		ARP_prototype[0] = 0x08;
		ARP_prototype[1] = 0x00;
		ARP_OP = new byte[2];
		ARP_OP[0] = 0x00;
		ARP_OP[1] = 0x01;
		ARP_hardsize = new byte[1];
		ARP_hardsize[0] = 0x06;
		ARP_protosize = new byte[1];
		ARP_protosize[0] = 0x04;
		ARP_senderEthAddr = new byte[6];
		ARP_senderIPAddr = new byte[4];
		ARP_targetEthAddr = new byte[6];
		ARP_targetIPAddr = new byte[4];
	}

	void setARPCacheTable(byte[] IP_Address, byte[] Ether_Address, byte state) {
		int index = findARPCacheTable(IP_Address);
		if (Arrays.equals(IP_Address, ARP_senderIPAddr)) {
			//test
			System.arraycopy(IP_Address, 0, ARPCacheTable[ARPCacheTableCount], 0, 4);
			System.arraycopy(Ether_Address, 0, ARPCacheTable[ARPCacheTableCount], 4, 6);
			ARPCacheTable[ARPCacheTableCount][10] = state;
			ARPCacheTableCount++;
			return;
		}
		if (index == -1) {
			System.arraycopy(IP_Address, 0, ARPCacheTable[ARPCacheTableCount], 0, 4);
			System.arraycopy(Ether_Address, 0, ARPCacheTable[ARPCacheTableCount], 4, 6);
			ARPCacheTable[ARPCacheTableCount][10] = state;
			ARPCacheTableCount++;
		} else {
			System.arraycopy(Ether_Address, 0, ARPCacheTable[index], 4, 6);
			ARPCacheTable[index][10] = state;
		}	
	}

	byte[] getARPCacheTable(byte[] IP_Address) {
		int index = findARPCacheTable(IP_Address);
		if (index != -1)
			return ARPCacheTable[index];
		return null;
	}

	private int findARPCacheTable(byte[] IP_Address) {
		byte[] temp = new byte[4];
		for (int i = 0; i < ARPCacheTableCount; i++) {
			System.arraycopy(ARPCacheTable[i], 0, temp, 0, 4);
			if (java.util.Arrays.equals(IP_Address, temp)) {
				return i;
			}
		}
		return -1;
	}

	void setARPProxyTable(String Device_name, String ip, String Mac) {
		byte[] byte_name = Device_name.getBytes();
		byte[] byte_ip = new byte[4];
		byte[] byte_mac = new byte[6];

		byte_ip[0] = ((byte) Integer.parseInt(ip.substring(0, 3)));
		byte_ip[1] = ((byte) Integer.parseInt(ip.substring(3, 6)));
		byte_ip[2] = ((byte) Integer.parseInt(ip.substring(6, 9)));
		byte_ip[3] = ((byte) Integer.parseInt(ip.substring(9, 12)));

		for (int i = 0, j = 0; i < 12; i += 2, j++) {
			byte_mac[j] = Integer.valueOf(Mac.substring(i, i + 2), 16).byteValue();
		}
		if (find_ARPProxyTable(byte_ip) == -1) {
			System.arraycopy(byte_ip, 0, ARPProxyTable[ARPProxyTable_count], 0, 4);
			System.arraycopy(byte_mac, 0, ARPProxyTable[ARPProxyTable_count], 4, 6);
			System.arraycopy(byte_name, 0, ARPProxyTable[ARPProxyTable_count], 10,
					byte_name.length > 10 ? 10 : byte_name.length);
			ARPProxyTable_count++;
		}
	}

	int find_ARPProxyTable(byte[] IP_address) {

		byte[] temp = new byte[4];

		for (int i = 0; i < ARPProxyTable_count; i++) {
			System.arraycopy(ARPProxyTable[i], 0, temp, 0, 4);
			if (java.util.Arrays.equals(IP_address, temp)) {
				return i;
			}
		}
		return -1;
	}

	byte[] get_ARPProxyTable(int index) {
		byte[] Mac_Address = new byte[6];
		for (int i = 0; i < 6; i++) {
			Mac_Address[i] = ARPProxyTable[index][i + 4];
		}
		return Mac_Address;
	}

	boolean Gratuious_ARP_Send(byte[] byte_Mac) {
		byte[] send_arp_data = new byte[ARP_MAX_SIZE];
		System.arraycopy(ARP_hardtype, 0, send_arp_data, 0, 2);
		System.arraycopy(ARP_prototype, 0, send_arp_data, 2, 2);
		send_arp_data[4] = ARP_hardsize[0];
		send_arp_data[5] = ARP_protosize[0];
		send_arp_data[6] = 0;
		send_arp_data[7] = 1;

		for (int i = 0; i < 6; i++) {
			ARP_targetEthAddr[i] = byte_Mac[i];
		}

		System.arraycopy(ARP_targetEthAddr, 0, send_arp_data, 8, 6);
		System.arraycopy(ARP_senderIPAddr, 0, send_arp_data, 14, 4);
		System.arraycopy(ARP_targetEthAddr, 0, send_arp_data, 18, 6);

		for (int i = 0; i < 4; i++) {
			send_arp_data[24 + i] = 0;
		}

		byte[] frame_type = new byte[2];
		frame_type[0] = 0x08;
		frame_type[1] = 0x06;



		if (((EthernetLayer) this.getUnderLayer()).sendARP(send_arp_data)) {
			return true;
		} else {
			return false;
		}
	}
	// ip 패킷 전송 시 호출, dest_ip_address에는 목적지가 담겨 있음, gateway or host
	boolean send(byte[] send_ip_data, byte[] dest_ip_address) {
		byte[] send_arp_data = new byte[send_ip_data.length + ARP_MAX_SIZE];

		if ((findARPCacheTable(dest_ip_address) == -1)) {
			System.out.printf("%d.%d.%d.%d\n", dest_ip_address[0], dest_ip_address[1], dest_ip_address[2], dest_ip_address[3]);

			// 대상의 arp 정보 없는 경우 arp request 하고, reply 올 때 까지 패킷 전송 유보
			ARP_request_send(dest_ip_address);
			int count = 0 ;
			while (count < 20) {
				try {
					Thread.sleep(100);
					if (findARPCacheTable(dest_ip_address) != -1 && ARPCacheTable[findARPCacheTable(dest_ip_address)][10] != 0) {
						System.out.println("arp 찾기 끝");
						byte[] dest_mac_address = new byte[6];

						dest_mac_address[0] = getARPCacheTable(dest_ip_address)[4];
						dest_mac_address[1] = getARPCacheTable(dest_ip_address)[5];
						dest_mac_address[2] = getARPCacheTable(dest_ip_address)[6];
						dest_mac_address[3] = getARPCacheTable(dest_ip_address)[7];
						dest_mac_address[4] = getARPCacheTable(dest_ip_address)[8];
						dest_mac_address[5] = getARPCacheTable(dest_ip_address)[9];

						return ((EthernetLayer) this.getUnderLayer()).sendIP(send_ip_data, dest_mac_address);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 count++;
			}
		} else {
			// arp table에 정보 있는 경우 해당 정보로 전송
			byte[] dest_mac_address = new byte[6];

			dest_mac_address[0] = getARPCacheTable(dest_ip_address)[4];
			dest_mac_address[1] = getARPCacheTable(dest_ip_address)[5];
			dest_mac_address[2] = getARPCacheTable(dest_ip_address)[6];
			dest_mac_address[3] = getARPCacheTable(dest_ip_address)[7];
			dest_mac_address[4] = getARPCacheTable(dest_ip_address)[8];
			dest_mac_address[5] = getARPCacheTable(dest_ip_address)[9];

			return ((EthernetLayer) this.getUnderLayer()).sendIP(send_ip_data, dest_mac_address);
		}
		return true;

	}

	// IP 패킷 수신 한 경우 수신자 정보 없을 시 arp_request_send 호출
	boolean ARP_request_send(byte[] dest_ip_address) {
		byte[] send_arp_data = new byte[ARP_MAX_SIZE];
		byte[] temp = new byte[6];
		System.arraycopy(ARP_hardtype, 0, send_arp_data, 0, 2);
		System.arraycopy(ARP_prototype, 0, send_arp_data, 2, 2);
		send_arp_data[4] = ARP_hardsize[0];
		send_arp_data[5] = ARP_protosize[0];
		send_arp_data[6] = 0;
		send_arp_data[7] = 1;

		for (int i = 0; i < 6; i++) {
			ARP_targetEthAddr[i] = 0;
			temp[i] = (byte) 0xff;
		}

		System.arraycopy(ARP_senderEthAddr, 0, send_arp_data, 8, 6);
		System.arraycopy(ARP_senderIPAddr, 0, send_arp_data, 14, 4);
		System.arraycopy(ARP_targetEthAddr, 0, send_arp_data, 18, 6);
		System.arraycopy(dest_ip_address, 0, send_arp_data, 24, 4);

		if (findARPCacheTable(dest_ip_address) == -1)
			setARPCacheTable(dest_ip_address, temp, (byte) 0);
		
		return ((EthernetLayer) this.getUnderLayer()).sendARP(send_arp_data);
	}

	boolean ARP_reply_send(byte[] data) {
		// 자기한테 온거면 request 인지 reply인지 생각않고 여기로 떨어진다.

		byte[] receive_sender_Ethernet = new byte[6];
		byte[] receive_sender_IP = new byte[4];
		byte[] receive_target_IP = new byte[4];
		byte[] receive_target_Ethernet = new byte[6];

		System.arraycopy(data, 8, receive_sender_Ethernet, 0, 6);
		System.arraycopy(data, 14, receive_sender_IP, 0, 4);
		System.arraycopy(data, 18, receive_target_Ethernet, 0, 6);
		System.arraycopy(data, 24, receive_target_IP, 0, 4);

		setARPCacheTable(receive_sender_IP, receive_sender_Ethernet, (byte) 1);

		// 이것도 정확히 무얼 하는 건지 모르곘다.
		if (java.util.Arrays.equals(ARP_senderIPAddr, receive_sender_IP)) {
			return false;
		}
		
		if (data[6] == 0 && data[7] == 1) {	// arp request를 받은 경우
			byte[] conform = new byte[4];
			conform[0] = data[24];
			conform[1] = data[25];
			conform[2] = data[26];
			conform[3] = data[27];
			
			// GARP 인 경우
			if (java.util.Arrays.equals(receive_sender_Ethernet, receive_target_Ethernet)) {
				if (findARPCacheTable(receive_sender_IP) != -1) {
					setARPCacheTable(receive_sender_IP, receive_sender_Ethernet, (byte) 1);
					return true;
				}
			}

			System.arraycopy(receive_sender_Ethernet, 0, data, 18, 6);
			System.arraycopy(receive_sender_IP, 0, data, 24, 4);
			System.arraycopy(receive_target_IP, 0, data, 14, 4);
			System.arraycopy(ARP_senderEthAddr, 0, data, 8, 6);
			
			// arp request 패킷 생성 하여 전송
			data[6] = 0;
			data[7] = 2;
			byte[] frame_type = new byte[2];
			frame_type[0] = 0x08;
			frame_type[1] = 0x06;
			((EthernetLayer) this.getUnderLayer()).sendARP(data);
			return true;

		} else if (data[6] == 0 && data[7] == 2) { // arp reply를 받은 경우
			// table 업데이트
			System.arraycopy(data, 8, receive_sender_Ethernet, 0, 6);
			System.arraycopy(data, 14, receive_sender_IP, 0, 4);
			if (findARPCacheTable(receive_sender_IP) != -1) {
				setARPCacheTable(receive_sender_IP, receive_sender_Ethernet, (byte) 1);
				return true;
			}
		}
		return true;
	}

	public void setSrcEthAddress(byte[] src_EthAddress) {
		ARP_senderEthAddr = Arrays.copyOf(src_EthAddress, src_EthAddress.length);
	}

	public void setDstIPAddress(String dst_IPAddress) {
		ARP_targetIPAddr[0] = ((byte) Integer.parseInt(dst_IPAddress.substring(0, 3)));
		ARP_targetIPAddr[1] = ((byte) Integer.parseInt(dst_IPAddress.substring(3, 6)));
		ARP_targetIPAddr[2] = ((byte) Integer.parseInt(dst_IPAddress.substring(6, 9)));
		ARP_targetIPAddr[3] = ((byte) Integer.parseInt(dst_IPAddress.substring(9, 12)));
	}

	public void setSrcIPAddress(String src_IPAddress) {
		ARP_senderIPAddr[0] = ((byte) Integer.parseInt(src_IPAddress.substring(0, 3)));
		ARP_senderIPAddr[1] = ((byte) Integer.parseInt(src_IPAddress.substring(3, 6)));
		ARP_senderIPAddr[2] = ((byte) Integer.parseInt(src_IPAddress.substring(6, 9)));
		ARP_senderIPAddr[3] = ((byte) Integer.parseInt(src_IPAddress.substring(9, 12)));
	}

	public void ARPTable_reset() {
		for (int i = 0; i < ARPCacheTableCount; i++) {
			for (int j = 0; j < ARP_TABLE_SIZE; j++) {
				ARPCacheTable[i][j] = 0;
			}
		}
		ARPCacheTableCount = 0;
	}

	public void ARPTable_delete() {
		if (ARPCacheTableCount > 0) {
			ARPCacheTableCount--;
			for (int j = 0; j < ARP_TABLE_SIZE; j++) {
				ARPCacheTable[ARPCacheTableCount][j] = 0;
			}
		}
	}

	public void ProxyTable_delete() {
		if (ARPProxyTable_count > 0) {
			ARPProxyTable_count--;
			for (int j = 0; j < ARPProxyTable_count; j++) {
				ARPProxyTable[ARPProxyTable_count][j] = 0;
			}
		}
	}

	public void ARPTable_IP_delete(byte[] ip) {
		if (findARPCacheTable(ip) != -1) {
			for (int i = findARPCacheTable(ip); i < ARPCacheTableCount - 1; i++) {
				ARPCacheTable[i] = ARPCacheTable[i + 1];
			}
			ARPCacheTableCount--;
		}
	}
}