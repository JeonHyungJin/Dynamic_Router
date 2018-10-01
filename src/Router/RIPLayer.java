package Router;

public class RIPLayer {
   // RIP header
   // Version은 2만 사용한다고 가정
   byte[] command;
   byte[] version;
   byte[] data;
   
   public void receiveRIP(byte[] dataRIP) {
      // TODO Auto-generated method stub
      // table 업데이트
      
      if( dataRIP[0] == 0x01 ) {
         // request
         // 패킷 처리 알고리즘 ~
         
      }else if( dataRIP[0] == 0x02) {
         // response
         // 패킷 처리 알고리즘 ~
         
      }
   }
   
   public void sendRIP() {
      // 좀더 생각 해보기~
      // request -> response
      
      // timer 별로 송신
      
      // 내가 처음 연결 됬을 때 ?
      
   }

}