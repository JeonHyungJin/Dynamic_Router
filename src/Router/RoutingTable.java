package Router;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Routing table
 * 
 * 라우터가 갖는 Routing table
 * Metric 변경 되는 작업 필요
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
   private int RT_state;
   private Timer expiration_timer;
   private TimerTask expiration_task;
   private Timer garbage_timer;
   private TimerTask garbage_task;


   public RoutingTable() {
      RT_des_IP = new byte[RT_DES_SIZE];
      RT_netmask_IP = new byte[RT_NETMASK_SIZE];
      RT_gateway_IP = new byte[RT_GATEWAY_SIZE];
      RT_flag = Flag.NONE;
      RT_interface = 0;
      RT_metric = 0;
      RT_Index = 0;
      RT_state = 180;

   }

   public RoutingTable(byte[] desIP, byte[] netmaskIP, byte[] gatewayIP, Flag flag, int interfaceNumber, int index, int metric) {
      RT_des_IP = new byte[RT_DES_SIZE];
      RT_netmask_IP = new byte[RT_NETMASK_SIZE];
      RT_gateway_IP = new byte[RT_GATEWAY_SIZE];
      RT_flag = Flag.NONE;

      System.arraycopy(desIP, 0, RT_des_IP, 0, 4);
      System.arraycopy(netmaskIP, 0, RT_netmask_IP, 0, 4);
      System.arraycopy(gatewayIP, 0, RT_gateway_IP, 0, 4);
      RT_flag = flag;
      RT_interface = interfaceNumber;
      RT_Index = index;

      if ((netmaskIP[3] & (byte)0xff) != 0) {		//255.255.255.255
         RT_class = 3;
      } else if ((netmaskIP[2] & (byte)0xff) != 0) {	//255.255.255.0
         RT_class = 2;
      } else if ((netmaskIP[1] & (byte)0xff) != 0) {	//255.255.0.0		//기존 코드 오타
         RT_class = 1;
      } else {		//255.0.0.0
         RT_class = 0;
      }
      this.RT_metric = metric;

      RT_state = 180; // ?
      if( RT_flag == Flag.UG ){
         // 게이트 웨이를 타고 가야만 만날수 있는 경우
         // 180초 타이머 돌리기
         expiration_timer = new Timer();
         expiration_task = new TimerTask() {
            @Override
            public void run() {
                  RT_metric = 16;
                  // 나 바뀌었어용~
                  // sending
                  // 반대쪽 으로 보내기인데
               // 정보 변경
               RT_state = -1;
                  ApplicationLayer.ifTableChaged(4, RT_Index, RT_interface);

               garbage_timer = new Timer();
               garbage_task = new TimerTask() {
                  @Override
                  public void run() {
                     // this.index의 테이블을 지운다.
                     // 굳이 보낼 필요는 없다.
                     ApplicationLayer.ifTableChaged(3, RT_Index, RT_interface);
                     garbage_timer.cancel();
                  }
               };
               garbage_timer.schedule(garbage_task,120000, 120000);
               expiration_timer.cancel();
            }
         };

         expiration_timer.schedule(expiration_task,180000, 180000);
      }else{
         this.expiration_timer = null;
         this.expiration_task = null;
         this.garbage_task= null;
         this.garbage_timer = null;
      }
   }

   public int getRT_state() {
      return RT_state;
   }

   public void setRT_state(int RT_state) {
      this.RT_state = RT_state;
   }

   public void setRT_Index(int RT_Index) {
      this.RT_Index = RT_Index;
   }

   public void setRT_des_IP(byte[] RT_des_IP) {
      this.RT_des_IP = RT_des_IP;
   }

   public void setRT_netmask_IP(byte[] RT_netmask_IP) {
      this.RT_netmask_IP = RT_netmask_IP;
   }

   public void setRT_gateway_IP(byte[] RT_gateway_IP) {
      this.RT_gateway_IP = RT_gateway_IP;
   }

   public void setRT_flag(Flag RT_flag) {
      this.RT_flag = RT_flag;
   }

   public void setRT_interface(int RT_interface) {
      this.RT_interface = RT_interface;
   }

   public void setRT_metric(int RT_metric) {
      this.RT_metric = RT_metric;
   }

   public void setRT_class(int RT_class) {
      this.RT_class = RT_class;
   }

   public void setRoutingTable(byte[] desIP, byte[] netmaskIP, byte[] gatewayIP, Flag flag, int interfaceNumber,
                               int index, int metric) {
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

      /*if (desIP[netmaskCheck] == gatewayIP[netmaskCheck] || netmaskCheck == 0) {
         RT_metric = 0x01;
      } else {
         RT_metric = 0x02;
      }*/
      this.RT_metric = metric;
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

   public void restartExpireTimer() {
      if( RT_state == -1 ){
         // 지울려 하는데 패킷 도착한 경우
         RT_state = 0;
         this.garbage_timer.cancel();
      }else{
         this.expiration_timer.cancel();
      }
      expiration_timer = new Timer();
      expiration_task = new TimerTask() {
         @Override
         public void run() {
            RT_metric = 16;
            // 나 바뀌었어용~
            // sending
            // 반대쪽 으로 보내기인데
            // 정보 변경
            RT_state = -1;
            ApplicationLayer.ifTableChaged(4, RT_Index, RT_interface);

            garbage_timer = new Timer();
            garbage_task = new TimerTask() {
               @Override
               public void run() {
                  // this.index의 테이블을 지운다.
                  // 굳이 보낼 필요는 없다.
                  ApplicationLayer.ifTableChaged(3, RT_Index, RT_interface);
                  garbage_timer.cancel();
               }
            };
            garbage_timer.schedule(garbage_task,120000, 120000);
            expiration_timer.cancel();
         }
      };
      expiration_timer.schedule(expiration_task,180000, 180000);
   }
}