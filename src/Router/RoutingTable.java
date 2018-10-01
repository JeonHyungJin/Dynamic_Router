package Router;

/**
 * Routing table
 * 
 * 라우터가 갖는 Routing table
 * 
 * */
public class RoutingTable {
	/*Routing table에 사용 되는 상수 설정*/
   private final static int RT_DES_SIZE = 4;		//Destination IP 4바이트
   private final static int RT_NETMASK_SIZE = 4;	//NetMask 4바이트
   private final static int RT_GATEWAY_SIZE = 4;	//GateWay 4바이트

   private int RT_Index;
   private byte[] RT_des_IP;
   private byte[] RT_netmask_IP;
   private byte[] RT_gateway_IP;
   private Flag RT_flag;
   private int RT_interface;
   private int RT_metric;
   private int RT_class;

   public RoutingTable() {
      RT_des_IP = new byte[RT_DES_SIZE];
      RT_netmask_IP = new byte[RT_NETMASK_SIZE];
      RT_gateway_IP = new byte[RT_GATEWAY_SIZE];
      RT_flag = Flag.NONE;
      RT_interface = 0;
      RT_metric = 0;
      RT_Index = 0;
   }

   public void setRoutingTable(byte[] desIP, byte[] netmaskIP, byte[] gatewayIP, Flag flag, int interfaceNumber,
         int index) {
      int netmaskCheck = 0;
      System.arraycopy(desIP, 0, RT_des_IP, 0, 4);
      System.arraycopy(netmaskIP, 0, RT_netmask_IP, 0, 4);
      System.arraycopy(gatewayIP, 0, RT_gateway_IP, 0, 4);
      RT_flag = flag;
      RT_interface = interfaceNumber;
      RT_Index = index;

      netmaskCheck = this.netmaskCheckClass(netmaskIP);
      
      if ((netmaskIP[3] & (byte)0xff) != 0) {		//255.255.255.255
         RT_class = 3;
      } else if ((netmaskIP[2] & (byte)0xff) != 0) {	//255.255.255.0
         RT_class = 2;
      } else if ((netmaskIP[1] & (byte)0xff) != 0) {	//255.255.0.0		//기존 코드 오타
         RT_class = 1;
      } else {		//255.0.0.0
         RT_class = 0;
      }

      if (desIP[netmaskCheck] == gatewayIP[netmaskCheck] || netmaskCheck == 0) {
         RT_metric = 0x01;
      } else {
         RT_metric = 0x02;
      }
   }

   public byte[] getDestination() {
      return this.RT_des_IP;
   }

   public byte[] getNetMask() {
      return this.RT_netmask_IP;
   }

   public byte[] getGateway() {
      return this.RT_gateway_IP;
   }

   public Flag getFlag() {
      return this.RT_flag;
   }

   public int getInterface() {
      return this.RT_interface;
   }

   public int getMetric() {
      return this.RT_metric;
   }

   public int getIndex() {
      return this.RT_Index;
   }
   
   public int getClassNumber() {
      return this.RT_class;
   }

   /**
    * NetMask가 255.0.0.0 이면 A클래스
    * NetMask가 255.255.0.0 이면 B클래스
    * NetMask가 255.255.255.0 이면 C클래스
    * */
   private int netmaskCheckClass(byte[] netmaskIP) {
      int count = 0;

      for (int i = 0; i < RT_NETMASK_SIZE; i++)
         if (netmaskIP[i] == (byte)0xff)	//255와 비교		//기존코드에서 byte로 캐스팅 필요
            count++;

      return count;		//매개변수로 들어온 netmaskIP중에서 255의 갯수를 반환
   }
}