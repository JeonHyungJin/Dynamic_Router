package Router;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

public class PacketDriverLayer extends BaseLayer {
	static {
		try {
			System.load(new File("jnetpcap.dll").getAbsolutePath());
			System.out.println(new File("jnetpcap.dll").getAbsolutePath());
		} catch (UnsatisfiedLinkError e) {
			System.out.println("Native code library failed to load.\n" + e);
			System.exit(1);
		}
	}

	int iNumberAdapter; // adapter에 인덱스를 부여하기 위한 변수
	public Pcap adapterObject; // 네트워크 어뎁터 객체 하나
	public PcapIf device; // 네트워크 인터페이스 객체
	public ArrayList<PcapIf> adapterList; // 네트워크 인터페이스 목록
	StringBuilder errorBuffer = new StringBuilder(); // 에러 메세지 생성
	long start; // 시작 시간

	/* 생성자 초기화용 */
	public PacketDriverLayer(String layerName) {
		super(layerName);

		adapterList = new ArrayList<PcapIf>();
		iNumberAdapter = 0;
		setAdapterList();

	}

	public void packetStartDriver() {
		int snaplength = 64 * 1024; // 패킷 캡쳐 길이
		int flags = Pcap.MODE_PROMISCUOUS; // 패킷 캡처 플래그
		int timeout = 1 * 1000; // 캡처 시간

		adapterObject = Pcap.openLive(adapterList.get(iNumberAdapter).getName(), snaplength, flags, timeout,
				errorBuffer);

	}

	/* adapter에 번호 부여 */
	public void setAdapterNumber(int iNumber) {
		iNumberAdapter = iNumber;
		packetStartDriver(); // 패킷 드라이버 시작
		receive(); // 패킷 수신
	}

	/* 네트워크 어뎁터 목록 생성 */
	public void setAdapterList() {
		int r = Pcap.findAllDevs(adapterList, errorBuffer);

		// adapter가 존재하지 않을 경우
		if (r == Pcap.NOT_OK || adapterList.isEmpty())
			System.out.println("[Error] 네트워크 어댑터를 읽지 못하였습니다. Error : " + errorBuffer.toString());
	}

	/* 연결된 adapter들을 저장해줄 리스트 */
	public ArrayList<PcapIf> getAdapterList() {
		return adapterList;
	}

	/* 상위에서 받아온 데이터 처리 */
	boolean send(byte[] data, int length) {
		ByteBuffer buffer = ByteBuffer.wrap(data); // data로 바이트 버퍼 생성
		start = System.currentTimeMillis(); // 현재 시간 저장 (시작시간)

		/* 데이터 전송, 어뎁터가 패킷 전송에 실패 했을 경우 */
		if (adapterObject.sendPacket(buffer) != Pcap.OK) {
			System.err.println(adapterObject.getErr());
			return false;
		}
		return true;
	}

	/* 데이터 수신 (쓰레드 시작) */
	synchronized boolean receive() {
		Receive_Thread thread = new Receive_Thread(adapterObject, (EthernetLayer) this.getUpperLayer()); // Receive_Thread에
																											// 있음
		Thread object = new Thread(thread);
		object.start(); // 쓰레드 시작
		try {
			object.join(1); // 현재 쓰레드가 동작 중이면 동작이 끝날 때 까지 기다림
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	String[] getNICDescription() {
		String[] descriptionArray = new String[adapterList.size()];

		for (int i = 0; i < adapterList.size(); i++)
			descriptionArray[i] = adapterList.get(i).getDescription(); // adapter 하나씩 description(설명?)을 저장

		return descriptionArray;
	}
}

class Receive_Thread implements Runnable {
	byte[] data;
	Pcap adapterObejct;
	EthernetLayer upperLayer;

	/* 상위 레이어, 네트워크 어뎁터 객체 초기화 */
	public Receive_Thread(Pcap adapterObject, EthernetLayer upperLayer) {
		this.adapterObejct = adapterObject;
		this.upperLayer = upperLayer;
	}

	/* 패킷 수신을 위한 라이브러리 함수 */
	@Override
	public void run() {
		while (true) {
			PcapPacketHandler<String> packetHandler = new PcapPacketHandler<String>() {
				public void nextPacket(PcapPacket packet, String user) {
					data = packet.getByteArray(0, packet.size()); // 패킷 크기를 알아냄

					// 아래 조건 만족 시 upperLayer에 데이터 전송 / (0x0800 0x0806) ARP방식 IP방식 둘 중에서만
					if ((data[12] == 8 && data[13] == 0) || (data[12] == 8 && data[13] == 6))
						upperLayer.receive(data);
				}
			};
			adapterObejct.loop(1000, packetHandler, "");
		}
	}
}