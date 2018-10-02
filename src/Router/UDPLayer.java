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
	byte[] udp_checksum = new byte[2];	//checksum에서 이거 사용 해야될듯
	byte[] udp_data;

	public UDPLayer(String layerName) {
		super(layerName);
	}

	/**
	 *
	 * @param sourcePort
	 * */
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
			byte[] hiddenData = digest.digest();// amhohwa
			checksum = Arrays.copyOfRange(hiddenData, 0, 2);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return checksum;
	}

	void setChecksum(byte[] checksum) { //checksum 헤더에 넣어요
		//Line 14에 선언된 변수를 이용하도록 변경 요청
		udp_data[6] = checksum[0];
		udp_data[7] = checksum[1];
	}

	// length & checksum 나중쓰

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
			return false;
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
		// now check the checksum;
		if (checkingChecksum[0] == dst_checksum[0] && checkingChecksum[1] == dst_checksum[1]) { //비교한다
			return true;
		} else {
			return false;
		}
	}

	boolean sendUDP(byte[] data) {
		int length = data.length;
		byte[] destinationPort = { (byte) 0x02, 0x08 };
		udp_data = new byte[data.length + UDP_HEAD_SIZE];

		// encapsulation
		setDestinationPort(destinationPort);	//이렇게 하면 DestinationPort가 정해지고 실제 보내지는 data부분에 붙는건 아니지 않나?

		for (int i = 0; i < length; i++)
			udp_data[i + UDP_HEAD_SIZE] = data[i];

		// sourcePort 설정은 고민~
		setChecksum(makeChecksum(data));
		if (((IPLayer) this.getUnderLayer()).sendUDP(udp_data)) {
			return true;
		} else
			return false;
	}

}
