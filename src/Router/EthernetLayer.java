package Router;

import java.util.Arrays;

/** 
 * 2계층에 해당 하는 Ethernet 
 * 동작 과정 
 *  1.IP 송신
 *   -> 상위 Layer로 부터 sendIP 호출 
 *   -> encapsulation ( setSourceAddress, setDestinationAddress, setFrameType ) 
 *   -> PacketDriver layer의 send 호출
 *  2.ARP 송신
 *   -> 상위 Layer로 부터 sendARP 호출 
 *   -> encapsulation ( setSourceAddress, setDestinationAddress, setFrameType )
 *     * ARP request or reply에 알맞은 destination address 설정
 *   -> PacketDriver layer의 send 호출
 *  3.Frame 수신
 *   -> 하위 Layer로 부터 receive 호출
 *   -> Frame의 source & destination address 확인하여 수신 진행
 *    * 자신이 보낸 것 X, Broadcast O, 타인이 나에게 보낸 것 O
 *   -> 수신 한다면, decapsulation 후 Ethernet type을 확인하여 알맞은 상위 Lyaer로 전달
 *    * 0x0800 : IP Layer, 0x0806 : ARP Layer
 */

public class EthernetLayer extends BaseLayer {
	/* 2계층에서 보내는 Frame의 최대 크기 및 헤더 사이즈 등 필요한 상수 설정 */
   final static int ETHERNET_MAX_SIZE = 1514;
   final static int ETHERNET_HEAD_SIZE = 14;
   final static int ETHERNET_MAX_DATA = ETHERNET_MAX_SIZE - ETHERNET_HEAD_SIZE;

   /* Frame의 내용을 담는 공간 */
   byte[] Ethernet_type;
   byte[] Ethernet_sourceAddress;
   byte[] Ethernet_data;

   /* 본 Layer에서는 사용안함 */
   int check = 0;

   public EthernetLayer(String layerName) {
      super(layerName);
      resetHeader();
   }

   /**
    * resetHeader : Frame을 담는 class 변수들 초기화
    * @param  
    */
   void resetHeader() {
	  Ethernet_type = new byte[2];
      Ethernet_sourceAddress = new byte[6];
      Ethernet_data = new byte[ETHERNET_MAX_SIZE];
   }

   /**
    * setSourceAddress : 매개변수의 값을 class 변수인 Ethernet_sourceAddress와 Ethernet_data의 알맞은 위치로 복사
    * @param sourceAddress : 송신 하는 device의 물리적 주소
    */
   void setSourceAddress(byte[] sourceAddress) {
      for (int i = 0; i < 6; i++) {
         Ethernet_sourceAddress[i] = sourceAddress[i];
         Ethernet_data[i + 6] = sourceAddress[i];
      }
   }

   /**
    * setDestinationAddress : 매개변수의 값을 class 변수인 Ethernet_data의 알맞은 위치로 복사
    * @param destinationAddress : 수신 하는 device의 물리적 주소
    */
   void setDestinationAddress(byte[] destinationAddress) {
      for (int i = 0; i < 6; i++)
         Ethernet_data[i] = destinationAddress[i];
   }
   
   /**
    * setFrameType : 매개변수의 값을 class 변수인 Ethernet_data의 알맞은 위치로 복사
    * @param frameType : Ethernet frame의 type, ip : 0x0800, arp : 0x0806
    */
   void setFrameType(byte[] frameType) {
      for (int i = 0; i < 2; i++)
         Ethernet_data[i + 12] = frameType[i];
   }

   /**
    * sendIP : IP Layer에서 온 데이터에 header로 encapsulation 하여 전송
    * @param data : 전송할 data, packet
    * @param destinationAddress : 수신 device 물리적 주소
    */
   boolean sendIP(byte[] data, byte[] destinationAddress) {
      int length = data.length;
      byte[] type = { (byte) 0x08, 0x00 };							// Ip packet 이므로 type이 0x0800
      Ethernet_data = new byte[data.length + ETHERNET_HEAD_SIZE];
      
      // header로 encapsulation, Ethernet type, source & destination device mac address
      setFrameType(type);											
      setSourceAddress(Ethernet_sourceAddress);
      setDestinationAddress(destinationAddress);

      // 전송 할 data를 Ethernet frame으로 복사
      for (int i = 0; i < length; i++)
         Ethernet_data[i + ETHERNET_HEAD_SIZE] = data[i];

      // 하위 Layer로 전달, 만들어진 Ethernet frame과 그 길이
      if (((PacketDriverLayer) this.getUnderLayer()).send(Ethernet_data, Ethernet_data.length))
         return true;
      else
         return false;
   }
   
   /**
    * sendARP : ARP Layer에서 온 데이터에 header로 encapsulation 하여 전송
    * @param data : 전송할 data, ARP Packet
    */
   boolean sendARP(byte[] data) {
      int length = data.length;
      byte[] destinationAddress = new byte[6];
      Ethernet_data = new byte[data.length + ETHERNET_HEAD_SIZE];
      byte[] type = { 0x08, 0x06 };										// Arp packet 이므로 type이 0x0806
      
      // header로 encapsulation, Ethernet type, source & destination device mac address
      setFrameType(type);
      setSourceAddress(Ethernet_sourceAddress);
      
      // encapsulation 과정 중, Arp 패킷의 destination address는 주의
      // ARP 패킷 중 Operation 확인하여 request와 reply를 구분하여 설정
      if (data[7] == 2) {			// ARP reply
    	 // ARP 패킷의 target mac address를 보고 설정
         for (int i = 0; i < 6; i++)
            destinationAddress[i] = data[i + 18];
         setDestinationAddress(destinationAddress);
      } else {						// ARP request
    	  // BroadCast로 설정
         for (int i = 0; i < 6; i++)
            destinationAddress[i] = (byte) 0xff;
         setDestinationAddress(destinationAddress);
      }

      // 전송 할 data를 Ethernet frame으로 복사
      for (int i = 0; i < length; i++)
         Ethernet_data[i + ETHERNET_HEAD_SIZE] = data[i];

      // 하위 Layer로 전달, 만들어진 Ethernet frame과 그 길이
      if (((PacketDriverLayer) this.getUnderLayer()).send(Ethernet_data, Ethernet_data.length))
         return true;
      else
         return false;
   }

   /**
    * receive : PacketDriver Layer에서 온 데이터에 header를 풀어 decapsulation 하여 전송
    * @param data : 수신한 frame
    * synchronized : 하위 레이어에서 thread 별 receive 호출  시 동기화를 위하여 설정
    */
   synchronized boolean receive(byte[] data) {
      byte[] destinationMAC = new byte[6];
      byte[] sourceMAC = new byte[6];
      byte[] broadcast = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
      
      // 수신한 frame으로 부터 주소 추출, source & destination
      System.arraycopy(data, 0, destinationMAC, 0, 6);					
      System.arraycopy(data, 6, sourceMAC, 0, 6);
      
      // 정상수신을 해야하는 frame인지 주소 비교
 	  // 자신이 보낸 frame인지 체크, 아니면 수신
      if (java.util.Arrays.equals(Ethernet_sourceAddress, sourceMAC))
         return false;
      // Broadcast 거나 자신에게 온 frame인지 체크, 맞다면 수신
      if (!(java.util.Arrays.equals(broadcast, destinationMAC) || java.util.Arrays.equals(Ethernet_sourceAddress, destinationMAC)))
         return false;
      
      // 수신할 frame을 decapsulation하여 상위 레이어로 전달
      byte[] dataFrame = Arrays.copyOfRange(data, ETHERNET_HEAD_SIZE, data.length);
      // Ethernet frame type을 확인하여, 알맞은 레이어로 전달
      if (data[12] == 8 && data[13] == 0)	// IP : 0x0800
         ((IPLayer) this.getUpperLayer()).receiveIP(dataFrame);
      if (data[12] == 8 && data[13] == 6)	// ARP : 0x0806
         ((IPLayer) this.getUpperLayer()).receiveARP(dataFrame);
      return true;
   }
}