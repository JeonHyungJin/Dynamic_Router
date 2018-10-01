package Router;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.awt.Color;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.JButton;
import java.awt.Label;
import javax.swing.JComboBox;
import java.awt.List;
import java.awt.Font;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

/**
 * GUI 환경 구성
 *  - 테이블 내용 추가, 버튼 리스너 코딩
 *  201402420 전형진
 *  201302477 전철민
 *  201402343 남진우
 *  201402450 현도훈
 */

public class ApplicationLayer extends JFrame {
	/* GUI 구성 
	 * ip, mac 주소 입력
	 * table, button 등
	 * */
   private JPanel contentPane;
   private JTextField MyIPaddress;
   private JTextField MyIPaddress2;
   JComboBox NIC_ComboBox;
   JComboBox NIC_ComboBox2;
   private JTextField Mac_address;
   private JTextField Mac_address2;

   JButton myAddressSet_btn;
   JButton myAddressSet_btn2;
   JButton ProxyARPAdd_btn;
   JButton ARPCasheDel_btn;
   JButton StaticRoutingAdd_btn;
   JButton StaticRoutingDelete_btn;
   JButton ProxyARPDelete_btn;
   
   static List ARPCachelist;
   static List Proxylist;
   static List StaticRoutingList;
   
   private JTextField ProxyARPDevice;
   private JTextField ProxyARPIP;
   private JTextField ProxyARPMac;
   private JTextField Destination;
   private JTextField Netmask;
   private JTextField Gateway;

   private JCheckBox CheckBoxUp;
   private JCheckBox CheckBoxGateway;
   private JCheckBox CheckBoxHost;

   /* Interface별 레이어
    * 2개의 인터페이스가 있는 거로 가정 
    * routing table & arp cache table 
    * */
   static PacketDriverLayer m_PacketDriverLayer_1;
   static EthernetLayer m_EthernetLayer_1;
   static ARPLayer m_ARPLayer_1;
   static IPLayer m_IPLayer_1;
   static ApplicationLayer m_ApplicationLayer;

   static PacketDriverLayer m_PacketDriverLayer_2;
   static EthernetLayer m_EthernetLayer_2;
   static ARPLayer m_ARPLayer_2;
   static IPLayer m_IPLayer_2;
   static RoutingTable[] routingTable;
   static int routingIndex;
   
   int adapterNumber = 0;
   int adapterNumber2 = 0;
   
   private JTextField interface_box;
   private JButton btnAClass;
   private JButton btnBClass;
   private JButton btnCClass;
   
   static byte[][] ARPCacheTable = new byte[255][11];


   public static void main(String[] args)
   {
	   /* interface별 레이어 객체 생성  */
      m_PacketDriverLayer_1 = new PacketDriverLayer("CPacketDriverLayer_1");
      m_EthernetLayer_1 = new EthernetLayer("CEthernetLayer_1");
      m_ARPLayer_1 = new ARPLayer("ARPLayer_1");
      m_IPLayer_1 = new IPLayer("CIPLayer_1");
      m_ApplicationLayer = new ApplicationLayer();

      m_PacketDriverLayer_2 = new PacketDriverLayer("CPacketDriverLayer_2");
      m_EthernetLayer_2 = new EthernetLayer("CEthernetLayer_2");
      m_ARPLayer_2 = new ARPLayer("ARPLayer_2");
      m_IPLayer_2 = new IPLayer("CIPLayer_2");
      
      /* 레이어 연결 
       * 밑에서부터 : PacketDriverLayer <-> EthernetLayer <-> IPLayer -> ApplicationLayer
       * 									  ^- ARPLayer <-^
       *  */
      m_PacketDriverLayer_1.setUpperLayer(m_EthernetLayer_1);
      m_EthernetLayer_1.setUnderLayer(m_PacketDriverLayer_1);
      m_EthernetLayer_1.setUpperLayer(m_IPLayer_1);
      m_ARPLayer_1.setUnderLayer(m_EthernetLayer_1);
      m_ARPLayer_1.setUpperLayer(m_IPLayer_1);
      m_IPLayer_1.setUnderLayer(m_ARPLayer_1);
      m_IPLayer_1.setUpperLayer(m_ApplicationLayer);

      m_PacketDriverLayer_2.setUpperLayer(m_EthernetLayer_2);
      m_EthernetLayer_2.setUnderLayer(m_PacketDriverLayer_2);
      m_EthernetLayer_2.setUpperLayer(m_IPLayer_2);
      m_ARPLayer_2.setUnderLayer(m_EthernetLayer_2);
      m_ARPLayer_2.setUpperLayer(m_IPLayer_2);
      m_IPLayer_2.setUnderLayer(m_ARPLayer_2);
      m_IPLayer_2.setUpperLayer(m_ApplicationLayer);

      /* routing table 공간 할당 */
	  routingTable = new RoutingTable[20];
      routingIndex = 0;
      
      /* 각 Interface별 routing 정보 저장을 위해 routing table 연결 */
      m_IPLayer_1.setRoutingTable(routingTable);
      m_IPLayer_2.setRoutingTable(routingTable);
      
      /* Routing 후 알맞은 interface에게 보내기 위해 서로 다른 IPLayer를 각각 연결 */
      m_IPLayer_1.setOtherIPLayer(m_IPLayer_2);
      m_IPLayer_2.setOtherIPLayer(m_IPLayer_1);

      /* 각 Interface별 arp 정보 저장을 위해 arp cache table 연결 */
      m_ARPLayer_1.set_ARPTable(ARPCacheTable);
      m_ARPLayer_2.set_ARPTable(ARPCacheTable);
      
      /* 각 Interface별 번호 설정 */
      m_IPLayer_1.setInterfaceNumber(0);
      m_IPLayer_2.setInterfaceNumber(1);
      
      /* 프로그램 GUI 띄우기 */
      EventQueue.invokeLater(new Runnable() {
         public void run() {
            try {
               ApplicationLayer frame = new ApplicationLayer();
               frame.setVisible(true);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      
      /* Thread를 이용해 프로그램 구동 */
      Chat_Send_Thread thread = new Chat_Send_Thread();

      Thread object = new Thread(thread);
      object.start();

   }

   static class Chat_Send_Thread implements Runnable {

      public Chat_Send_Thread() {

      }
      
      @Override
      public void run() {
    	  /* Thread start 시 시작되는 함수 */
    	  
         byte[] ARP_Table_Data;
         byte[] Proxy_Table_Data;
         byte[] ARP_IP = null;
         byte[] ARP_ETHERNET = null;
         byte[] ARP_STATE = null;
         byte[] ARP_NAME = null;
         while (true) {
        	 // 주로 테이블 업데이트 담당
            try {
               Thread.sleep(500);
            } catch (InterruptedException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
            // ARP Cache List 업데이트
            // 일정 시간이 지나면 상태 변화 및 cache 삭제가 이루어져야 하며, 추가되는 경우도 있기에
            ARPCachelist.removeAll();
            for (int i = 0; i < m_ARPLayer_1.ARPCacheTableCount; i++) {
               ARP_Table_Data = m_ARPLayer_1.ARPCacheTable[i];

               ARP_IP = new byte[4];
               ARP_ETHERNET = new byte[6];
               ARP_STATE = new byte[1];

               for (int j = 0; j < 4; j++) {
                  ARP_IP[j] = ARP_Table_Data[j];
               }
               for (int j = 0; j < 6; j++) {
                  ARP_ETHERNET[j] = ARP_Table_Data[j + 4];
               }
               ARP_STATE[0] = ARP_Table_Data[10];

               int ARP_stat = ARP_STATE[0] & 0xFF;
               String ARP_Check;
               if (ARP_stat == 1) {
                  ARP_Check = "complete";
               } else {
                  ARP_Check = "imcomplete";
               }
               String Mac_byte = byteArrayToHex(ARP_ETHERNET);

               if (Mac_byte.equals("ff:ff:ff:ff:ff:ff")) {
                  Mac_byte = "??:??:??:??:??:??";
               }

               String temp_ip = byte2IP(ARP_IP);
               if (!temp_ip.equals("0.0.0.0")) {

                  ARPCachelist.addItem(byte2IP(ARP_IP) + "  " + Mac_byte + "  " + ARP_Check);
               }

            }
            
            /* Proxylist 업데이트 */
            Proxylist.removeAll();
            for (int i = 0; i < m_ARPLayer_1.ARPProxyTable_count; i++) {
               Proxy_Table_Data = m_ARPLayer_1.ARPProxyTable[i];

               ARP_IP = new byte[4];
               ARP_ETHERNET = new byte[6];
               ARP_STATE = new byte[1];
               ARP_NAME = new byte[10];

               for (int j = 0; j < 4; j++) {
                  ARP_IP[j] = Proxy_Table_Data[j];
               }
               for (int j = 0; j < 6; j++) {
                  ARP_ETHERNET[j] = Proxy_Table_Data[j + 4];
               }
               for (int j = 0; j < 10; j++) {
                  ARP_NAME[j] = Proxy_Table_Data[j + 10];
               }
               String String_ARP_NAME = new String(ARP_NAME);

               Proxylist.addItem(String_ARP_NAME + "  " + byte2IP(ARP_IP) + "  " + byteArrayToHex(ARP_ETHERNET));

            }

            for (int i = 0; i < m_ARPLayer_2.ARPProxyTable_count; i++) {
               Proxy_Table_Data = m_ARPLayer_2.ARPProxyTable[i];

               ARP_IP = new byte[4];
               ARP_ETHERNET = new byte[6];
               ARP_STATE = new byte[1];
               ARP_NAME = new byte[10];

               for (int j = 0; j < 4; j++) {
                  ARP_IP[j] = Proxy_Table_Data[j];
               }
               for (int j = 0; j < 6; j++) {
                  ARP_ETHERNET[j] = Proxy_Table_Data[j + 4];
               }
               for (int j = 0; j < 10; j++) {
                  ARP_NAME[j] = Proxy_Table_Data[j + 10];
               }
               String String_ARP_NAME = new String(ARP_NAME);

               Proxylist.addItem(String_ARP_NAME + "  " + byte2IP(ARP_IP) + "  " + byteArrayToHex(ARP_ETHERNET));

            }

         }
      }
   }

   public static int byte2Int(byte[] src) {
      int s1 = src[0] & 0xFF;
      int s2 = src[1] & 0xFF;
      int s3 = src[2] & 0xFF;
      int s4 = src[3] & 0xFF;

      return ((s1 << 24) + (s2 << 16) + (s3 << 8) + (s4 << 0));
   }

   public static String byte2IP(byte[] src) {
      int s1 = src[0] & 0xFF;
      int s2 = src[1] & 0xFF;
      int s3 = src[2] & 0xFF;
      int s4 = src[3] & 0xFF;

      return s1 + "." + s2 + "." + s3 + "." + s4;
   }

   static String byteArrayToHex(byte[] a) {
      StringBuilder sb = new StringBuilder();
      for (final byte b : a)
         sb.append(String.format("%02x:", b & 0xff));
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
   }


   @SuppressWarnings("serial")
   public class JEditorPaneLimit extends PlainDocument {
      private int limit;

      public JEditorPaneLimit(int limit) {
         super();
         this.limit = limit;
      }

      public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
         if (str == null)
            return;
         if (getLength() + str.length() <= limit)
            super.insertString(offset, str, attr);
      }
   }
   
   /* 버튼이나 텍스트등의 뷰의 리스터  */
   class setAddressListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {

         byte[] tempSourceAddress = new byte[6];
         byte[] tempSourceAddress2 = new byte[6];
         byte[] tempIPAddress1 = new byte[4];
         byte[] tempIPAddress2 = new byte[4];

         if (e.getSource() == myAddressSet_btn) { // 1번 Interface 준비 버튼 클릭시
        	if(Mac_address.getText().length() == 0){ // MAC 주소 입력 확인
         		JOptionPane.showMessageDialog(null, "Address 1의 Mac 주소를 선택해주세요.", "WARNING_MESSAGE",
 						JOptionPane.WARNING_MESSAGE);
         	} else if (MyIPaddress.getText().length() < 12){ // IP 주소 입력 확인
         		JOptionPane.showMessageDialog(null, "Address 1의 IP 주소를 12자리 입력해주세요.", "WARNING_MESSAGE",
 						JOptionPane.WARNING_MESSAGE);
         	} else {
         		// 입력된 주소로 1번 Interface의 Layer별 정보 설정
	            System.out.println("Address 1 : [" + adapterNumber + "] 준비 완료!");
	            for(int i = 0; i < 4; i++){ 
	                tempIPAddress1[i] = ((byte) Integer.parseInt(MyIPaddress.getText().substring(i*3, (i+1)*3)));
	            }
	            for (int i = 0, j = 0; i < 12; i += 2, j++) {
	                tempSourceAddress[j] = Integer.valueOf(Mac_address.getText().substring(i, i + 2), 16).byteValue();
	            }
	            m_IPLayer_1.setSourceIpAddress(tempIPAddress1);
	            m_ARPLayer_1.setSrcIPAddress(MyIPaddress.getText());
	            m_ARPLayer_1.setSrcEthAddress(tempSourceAddress);
	            m_EthernetLayer_1.setSourceAddress(tempSourceAddress);
	            m_PacketDriverLayer_1.setAdapterNumber(adapterNumber);
	            if(myAddressSet_btn.getText() == "Set"){
	            	myAddressSet_btn.setText("Reset");
	            	NIC_ComboBox.setEnabled(false);
	            	MyIPaddress.setEnabled(false);
	            	if(myAddressSet_btn2.getText() == "Reset"){
		            	Destination.setEnabled(true);
		            	Netmask.setEnabled(true);
		            	Gateway.setEnabled(true);
		            	CheckBoxUp.setEnabled(true);
		            	CheckBoxGateway.setEnabled(true);
		            	CheckBoxHost.setEnabled(true);
		            	interface_box.setEnabled(true);
		            	StaticRoutingAdd_btn.setEnabled(true);
		            	StaticRoutingDelete_btn.setEnabled(true);
		            	btnAClass.setEnabled(true);
		            	btnBClass.setEnabled(true);
		            	btnCClass.setEnabled(true);
		            	ARPCasheDel_btn.setEnabled(true);
		            	ProxyARPDevice.setEnabled(true);
		            	ProxyARPIP.setEnabled(true);
		            	ProxyARPMac.setEnabled(true);
		            	ProxyARPAdd_btn.setEnabled(true);
		            	ProxyARPDelete_btn.setEnabled(true);
	            	}
	            } else {
	            	myAddressSet_btn.setText("Set");
	            	NIC_ComboBox.setEnabled(true);
	            	MyIPaddress.setEnabled(true);
	            	Destination.setEnabled(false);
	            	Netmask.setEnabled(false);
	            	Gateway.setEnabled(false);
	            	CheckBoxUp.setEnabled(false);
	            	CheckBoxGateway.setEnabled(false);
	            	CheckBoxHost.setEnabled(false);
	            	interface_box.setEnabled(false);
	            	StaticRoutingAdd_btn.setEnabled(false);
	            	StaticRoutingDelete_btn.setEnabled(false);
	            	btnAClass.setEnabled(false);
	            	btnBClass.setEnabled(false);
	            	btnCClass.setEnabled(false);
	            	ARPCasheDel_btn.setEnabled(false);
	            	ProxyARPDevice.setEnabled(false);
	            	ProxyARPIP.setEnabled(false);
	            	ProxyARPMac.setEnabled(false);
	            	ProxyARPAdd_btn.setEnabled(false);
	            	ProxyARPDelete_btn.setEnabled(false);
	            }
         	}
         } else if (e.getSource() == myAddressSet_btn2) { // 2번 Interface 준비 버튼 클릭시
        	if(Mac_address2.getText().length() == 0){
        		JOptionPane.showMessageDialog(null, "Address 2의 Mac 주소를 선택해주세요.", "WARNING_MESSAGE",
						JOptionPane.WARNING_MESSAGE);
        	} else if (MyIPaddress2.getText().length() < 12){
        		JOptionPane.showMessageDialog(null, "Address 2의 IP 주소를 12자리 입력해주세요.", "WARNING_MESSAGE",
						JOptionPane.WARNING_MESSAGE);
        	} else {
	            System.out.println("Address 2 : [" + adapterNumber2 + "] 준비 완료!");
		    	for(int i = 0; i < 4; i++){
		    		 tempIPAddress2[i] = ((byte) Integer.parseInt(MyIPaddress2.getText().substring(i*3, (i+1)*3)));
		        }
		    	for (int i = 0, j = 0; i < 12; i += 2, j++) {
		            tempSourceAddress2[j] = Integer.valueOf(Mac_address2.getText().substring(i, i + 2), 16).byteValue();
		        }
	            m_IPLayer_2.setSourceIpAddress(tempIPAddress2);
	            m_ARPLayer_2.setSrcIPAddress(MyIPaddress2.getText());
	            m_ARPLayer_2.setSrcEthAddress(tempSourceAddress2);
	            m_EthernetLayer_2.setSourceAddress(tempSourceAddress2);
	            m_PacketDriverLayer_2.setAdapterNumber(adapterNumber2);
	            if(myAddressSet_btn2.getText() == "Set"){
	            	myAddressSet_btn2.setText("Reset");
	            	NIC_ComboBox2.setEnabled(false);
	            	MyIPaddress2.setEnabled(false);
	            	if(myAddressSet_btn.getText() == "Reset"){
		            	Destination.setEnabled(true);
		            	Netmask.setEnabled(true);
		            	Gateway.setEnabled(true);
		            	CheckBoxUp.setEnabled(true);
		            	CheckBoxGateway.setEnabled(true);
		            	CheckBoxHost.setEnabled(true);
		            	interface_box.setEnabled(true);
		            	StaticRoutingAdd_btn.setEnabled(true);
		            	StaticRoutingDelete_btn.setEnabled(true);
		            	btnAClass.setEnabled(true);
		            	btnBClass.setEnabled(true);
		            	btnCClass.setEnabled(true);
		            	ARPCasheDel_btn.setEnabled(true);
		            	ProxyARPDevice.setEnabled(true);
		            	ProxyARPIP.setEnabled(true);
		            	ProxyARPMac.setEnabled(true);
		            	ProxyARPAdd_btn.setEnabled(true);
		            	ProxyARPDelete_btn.setEnabled(true);
	            	}
	            } else {
	            	myAddressSet_btn2.setText("Set");
	            	NIC_ComboBox2.setEnabled(true);
	            	MyIPaddress2.setEnabled(true);
	            	Destination.setEnabled(false);
	            	Netmask.setEnabled(false);
	            	Gateway.setEnabled(false);
	            	CheckBoxUp.setEnabled(false);
	            	CheckBoxGateway.setEnabled(false);
	            	CheckBoxHost.setEnabled(false);
	            	interface_box.setEnabled(false);
	            	StaticRoutingAdd_btn.setEnabled(false);
	            	StaticRoutingDelete_btn.setEnabled(false);
	            	btnAClass.setEnabled(false);
	            	btnBClass.setEnabled(false);
	            	btnCClass.setEnabled(false);
	            	ARPCasheDel_btn.setEnabled(false);
	            	ProxyARPDevice.setEnabled(false);
	            	ProxyARPIP.setEnabled(false);
	            	ProxyARPMac.setEnabled(false);
	            	ProxyARPAdd_btn.setEnabled(false);
	            	ProxyARPDelete_btn.setEnabled(false);
	            }
        	}
         } else if (e.getSource() == StaticRoutingAdd_btn) { // static routing table 추가 버튼 클릭시
        	if(Destination.getText().length() < 12) {
				JOptionPane.showMessageDialog(null, "12자리 Destination IP 주소를 입력해주세요.", "WARNING_MESSAGE",
							JOptionPane.WARNING_MESSAGE);
			} else if(Netmask.getText().length() < 12) {
				JOptionPane.showMessageDialog(null, "12자리 Netmask IP 주소를 입력해주세요.", "WARNING_MESSAGE",
							JOptionPane.WARNING_MESSAGE);
			} else if(Gateway.getText().length() < 12) {
				JOptionPane.showMessageDialog(null, "12자리 Gateway IP 주소를 입력해주세요.", "WARNING_MESSAGE",
							JOptionPane.WARNING_MESSAGE);
			} else if(!CheckBoxUp.isSelected() && !CheckBoxGateway.isSelected() && !CheckBoxHost.isSelected()){
				JOptionPane.showMessageDialog(null, "Flag를 선택해주세요.", "WARNING_MESSAGE",
						JOptionPane.WARNING_MESSAGE);
			} else if(CheckBoxGateway.isSelected() && CheckBoxHost.isSelected()){
				JOptionPane.showMessageDialog(null, "Flag에서 Gateway와 Host를 동시에 선택할 수 없습니다.", "WARNING_MESSAGE",
						JOptionPane.WARNING_MESSAGE);
			} else if(interface_box.getText().length() < 1){
				JOptionPane.showMessageDialog(null, "Interface 이름을 입력해주세요.", "WARNING_MESSAGE",
						JOptionPane.WARNING_MESSAGE);
			} else {
				// static routing table 추가
	       	  	routingTable[routingIndex] = new RoutingTable();
	            Flag flag;
	            // flag, dest, netmask, gateway 정보 설정
	            if(CheckBoxUp.isSelected() && CheckBoxGateway.isSelected())
	               flag = Flag.UG;
	            else if(CheckBoxUp.isSelected() && CheckBoxHost.isSelected())
	               flag = Flag.UH;
	            else if(CheckBoxUp.isSelected())
	               flag = Flag.U;
	            else if(CheckBoxGateway.isSelected())
	               flag = Flag.G;
	            else if(CheckBoxHost.isSelected())
	               flag = Flag.H;
	            else
	               flag = Flag.NONE;
	            byte[] tempDestination = new byte[4];
	            for(int i = 0; i < 4; i ++){
	            	tempDestination[i] = ((byte) Integer.parseInt(Destination.getText().substring(i*3, (i+1)*3)));
	            }
	             
	            byte[] tempNetmask = new byte[4];
	            for(int i = 0; i < 4; i ++){
	            	tempNetmask[i] = ((byte) Integer.parseInt(Netmask.getText().substring(i*3, (i+1)*3)));
	            }
	            
	            byte[] tempGateway = new byte[4];
	            for(int i = 0; i < 4; i ++){
	            	tempGateway[i] = ((byte) Integer.parseInt(Gateway.getText().substring(i*3, (i+1)*3)));
	            }
	             
	            routingTable[routingIndex].setRoutingTable(tempDestination, tempNetmask, tempGateway, flag, 
	                   Integer.parseInt(interface_box.getText()), routingIndex);
	            for (int j = 0; j < routingIndex; j++) {
	                  if (routingTable[j].getClassNumber() < routingTable[routingIndex].getClassNumber()) {
	                     RoutingTable temp = routingTable[routingIndex];
	                     for (int k = routingIndex; k < j; k--) {
	                        routingTable[k] = routingTable[k - 1];
	                     }
	                     routingTable[j] = temp;
	                  }
	               }
	            StaticRoutingList.addItem(byte2IP(tempDestination) + "  " 
	             + byte2IP(tempNetmask) + "  " + byte2IP(tempGateway)
	             + "  " + flag + "  " + interface_box.getText() + "  " + routingTable[routingIndex].getMetric());
	            routingIndex++;
			}
         } else if (e.getSource() == StaticRoutingDelete_btn) { // static router table 삭제 버튼 클릭시
        	if(routingIndex > 0){
	        	StaticRoutingList.remove(routingIndex - 1);
	        	routingTable[routingIndex] = null;
	        	routingIndex--;
        	}
         } else if (e.getSource() == ProxyARPAdd_btn) { // proxy table 추가 버튼 클릭시
				if (ProxyARPDevice.getText().length() < 1 && ProxyARPIP.getText().length() < 12
						&& ProxyARPMac.getText().length() < 12) {
					JOptionPane.showMessageDialog(null, "Proxy ARP Entry의 정보를 입력해주세요.", "WARNING_MESSAGE",
							JOptionPane.WARNING_MESSAGE);
				} else if (ProxyARPDevice.getText().length() < 1) {
					JOptionPane.showMessageDialog(null, "Proxy ARP Entry의 Device Name를 입력해주세요.", "WARNING_MESSAGE",
							JOptionPane.WARNING_MESSAGE);
				} else if (ProxyARPIP.getText().length() < 12) {
					JOptionPane.showMessageDialog(null, "Proxy ARP Entry의 IP 주소를 12자리 입력해주세요.", "WARNING_MESSAGE",
							JOptionPane.WARNING_MESSAGE);
				} else if (ProxyARPMac.getText().length() < 12) {
					JOptionPane.showMessageDialog(null, "Proxy ARP Entry의 MAC 주소를 12자리 입력해주세요.", "WARNING_MESSAGE",
							JOptionPane.WARNING_MESSAGE);
				} else {
					m_ARPLayer_1.setARPProxyTable(ProxyARPDevice.getText(), ProxyARPIP.getText(),
							ProxyARPMac.getText());
					m_ARPLayer_2.setARPProxyTable(ProxyARPDevice.getText(), ProxyARPIP.getText(),
							ProxyARPMac.getText());
				}
         } else if (e.getSource() == ProxyARPDelete_btn) { // proxy table 삭제 버튼 클릭시
        	 m_ARPLayer_1.ProxyTable_delete();
        	 m_ARPLayer_2.ProxyTable_delete();
         } else if (e.getSource() == ARPCasheDel_btn) { // arp cache 삭제 버튼 클릭시
        	 m_ARPLayer_1.ARPTable_reset();
        	 m_ARPLayer_2.ARPTable_reset();
         } else if (e.getSource() == btnAClass)	{
        	 Netmask.setText("255000000000");
         } else if (e.getSource() == btnBClass) {
        	 Netmask.setText("255255000000");
         } else if (e.getSource() == btnCClass) {
        	 Netmask.setText("255255255000");
         }
      }
   }

   public String get_MacAddress(byte[] byte_MacAddress) {

      String MacAddress = "";
      try {    	  
         for (int i = 0; i < m_PacketDriverLayer_1.getAdapterList().get(adapterNumber)
               .getHardwareAddress().length; i++) {
            MacAddress += String.format("%02X%s",
                  m_PacketDriverLayer_1.getAdapterList().get(adapterNumber).getHardwareAddress()[i],
                  (i < MacAddress.length() - 1) ? "" : "");
         }
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      System.out.println("1번 현재 선택된 주소" + MacAddress);

      System.out.println(m_PacketDriverLayer_1.getAdapterList().get(adapterNumber).getAddresses());

      return MacAddress;
   }

   public String get_MacAddress2(byte[] byte_MacAddress) {

      String MacAddress = "";
      try {
         for (int i = 0; i < m_PacketDriverLayer_2.getAdapterList().get(adapterNumber2)
               .getHardwareAddress().length; i++) {
            MacAddress += String.format("%02X%s",
                  m_PacketDriverLayer_2.getAdapterList().get(adapterNumber2).getHardwareAddress()[i],
                  (i < MacAddress.length() - 1) ? "" : "");
         }
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      System.out.println("2번 현재 선택된 주소" + MacAddress);

      System.out.println(m_PacketDriverLayer_2.getAdapterList().get(adapterNumber2).getAddresses());

      return MacAddress;
   }

   public ApplicationLayer() {
      setTitle(
            "Static Router");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setBounds(100, 100, 735, 605);
      contentPane = new JPanel();
      contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
      setContentPane(contentPane);
      contentPane.setLayout(null);

      JPanel ARPcachePanel = new JPanel();
      ARPcachePanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "ARP Cache Table",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      ARPcachePanel.setBounds(430, 10, 280, 160);
      contentPane.add(ARPcachePanel);
      ARPcachePanel.setLayout(null);

      JPanel ARPcacheEditorPanel = new JPanel();
      ARPcacheEditorPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      ARPcacheEditorPanel.setBounds(10, 15, 260, 100);
      ARPcachePanel.add(ARPcacheEditorPanel);
      ARPcacheEditorPanel.setLayout(null);

      ARPCachelist = new List();
      ARPCachelist.setBounds(0, 0, 260, 100);
      ARPcacheEditorPanel.add(ARPCachelist);
      ARPCasheDel_btn = new JButton("Item Delete");
      ARPCasheDel_btn.setEnabled(false);
      ARPCasheDel_btn.setBounds(90, 120, 100, 30);
      ARPCasheDel_btn.addActionListener(new setAddressListener());
      ARPcachePanel.add(ARPCasheDel_btn);

      JPanel ProxyARPpanel = new JPanel();
      ProxyARPpanel.setBounds(430, 170, 280, 280);
      contentPane.add(ProxyARPpanel);
      ProxyARPpanel.setLayout(null);
      ProxyARPpanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Proxy ARP Table",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

      JPanel ProxyARPeditorPanel = new JPanel();
      ProxyARPeditorPanel.setLayout(null);
      ProxyARPeditorPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      ProxyARPeditorPanel.setBounds(12, 25, 260, 100);
      ProxyARPpanel.add(ProxyARPeditorPanel);

      Proxylist = new List();
      Proxylist.setBounds(0, 0, 260, 100);
      ProxyARPeditorPanel.add(Proxylist);

      ProxyARPAdd_btn = new JButton("Add");
      ProxyARPAdd_btn.setEnabled(false);
      ProxyARPAdd_btn.setBounds(25, 235, 100, 30);
      ProxyARPAdd_btn.addActionListener(new setAddressListener());
      ProxyARPpanel.add(ProxyARPAdd_btn);

      ProxyARPDelete_btn = new JButton("Delete");
      ProxyARPDelete_btn.setEnabled(false);
      ProxyARPDelete_btn.setBounds(150, 235, 100, 30);
      ProxyARPDelete_btn.addActionListener(new setAddressListener());
      ProxyARPpanel.add(ProxyARPDelete_btn);

      ProxyARPDevice = new JTextField();
      ProxyARPDevice.setEnabled(false);
      ProxyARPDevice.setHorizontalAlignment(SwingConstants.CENTER);
      ProxyARPDevice.setColumns(10);
      ProxyARPDevice.setBounds(90, 140, 160, 20);
      ProxyARPDevice.setDocument(new JEditorPaneLimit(10));
      ProxyARPpanel.add(ProxyARPDevice);

      ProxyARPIP = new JTextField();
      ProxyARPIP.setEnabled(false);
      ProxyARPIP.setHorizontalAlignment(SwingConstants.CENTER);
      ProxyARPIP.setColumns(10);
      ProxyARPIP.setBounds(90, 170, 160, 20);
      ProxyARPIP.setDocument(new JEditorPaneLimit(12));
      ProxyARPpanel.add(ProxyARPIP);

      ProxyARPMac = new JTextField();
      ProxyARPMac.setEnabled(false);
      ProxyARPMac.setHorizontalAlignment(SwingConstants.CENTER);
      ProxyARPMac.setColumns(10);
      ProxyARPMac.setBounds(90, 200, 160, 20);
      ProxyARPMac.setDocument(new JEditorPaneLimit(12));
      ProxyARPpanel.add(ProxyARPMac);

      Label label_1 = new Label("IP_Addr");
      label_1.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_1.setAlignment(Label.CENTER);
      label_1.setBounds(10, 170, 70, 20);
      ProxyARPpanel.add(label_1);

      Label label_5 = new Label("MAC_주소");
      label_5.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_5.setAlignment(Label.CENTER);
      label_5.setBounds(10, 200, 70, 20);
      ProxyARPpanel.add(label_5);

      Label label_6 = new Label("Device");
      label_6.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_6.setAlignment(Label.CENTER);
      label_6.setBounds(10, 140, 70, 20);
      ProxyARPpanel.add(label_6);

      JPanel AddressPanel = new JPanel();
      AddressPanel.setLayout(null);
      AddressPanel.setBorder(new TitledBorder(null, "Address 1", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      AddressPanel.setBounds(10, 450, 350, 110);
      contentPane.add(AddressPanel);

      Label label = new Label("MAC");
      label.setAlignment(Label.CENTER);
      label.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label.setBounds(10, 25, 50, 20);
      AddressPanel.add(label);

      Label label_2 = new Label("IP");
      label_2.setAlignment(Label.CENTER);
      label_2.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_2.setBounds(10, 65, 50, 20);
      AddressPanel.add(label_2);

      myAddressSet_btn = new JButton("Set");
      myAddressSet_btn.setBounds(220, 60, 100, 30);
      AddressPanel.add(myAddressSet_btn);
      myAddressSet_btn.addActionListener(new setAddressListener());

      MyIPaddress = new JTextField();
      MyIPaddress.setHorizontalAlignment(SwingConstants.CENTER);
      MyIPaddress.setBounds(60, 65, 130, 20);
      MyIPaddress.setDocument(new JEditorPaneLimit(12));
      MyIPaddress.setColumns(10);
      AddressPanel.add(MyIPaddress);

      NIC_ComboBox = new JComboBox();
      NIC_ComboBox.setBounds(60, 25, 130, 20);
      AddressPanel.add(NIC_ComboBox);

      Mac_address = new JTextField();
      Mac_address.setHorizontalAlignment(SwingConstants.CENTER);
      Mac_address.setBounds(205, 25, 130, 20);
      Mac_address.setColumns(10);
      AddressPanel.add(Mac_address);

      JPanel AddressPanel2 = new JPanel();
      AddressPanel2.setLayout(null);
      AddressPanel2.setBorder(new TitledBorder(null, "Address 2", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      AddressPanel2.setBounds(360, 450, 350, 110);
      contentPane.add(AddressPanel2);

      Label label_3 = new Label("MAC");
      label_3.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_3.setAlignment(Label.CENTER);
      label_3.setBounds(10, 25, 50, 20);
      AddressPanel2.add(label_3);

      Label label_4 = new Label("IP");
      label_4.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_4.setAlignment(Label.CENTER);
      label_4.setBounds(10, 65, 50, 20);
      AddressPanel2.add(label_4);

      myAddressSet_btn2 = new JButton("Set");
      myAddressSet_btn2.setBounds(220, 60, 100, 30);
      AddressPanel2.add(myAddressSet_btn2);
      myAddressSet_btn2.addActionListener(new setAddressListener());

      MyIPaddress2 = new JTextField();
      MyIPaddress2.setHorizontalAlignment(SwingConstants.CENTER);
      MyIPaddress2.setColumns(10);
      MyIPaddress2.setBounds(60, 65, 130, 20);
      MyIPaddress2.setDocument(new JEditorPaneLimit(12));
      AddressPanel2.add(MyIPaddress2);

      NIC_ComboBox2 = new JComboBox();
      NIC_ComboBox2.setBounds(60, 25, 130, 20);
      AddressPanel2.add(NIC_ComboBox2);

      // 선택한 Interface 정보 설정
      for (int i = 0; m_PacketDriverLayer_1.getAdapterList().size() > i; i++) {
         NIC_ComboBox.addItem(m_PacketDriverLayer_1.getAdapterList().get(i).getDescription());
         NIC_ComboBox2.addItem(m_PacketDriverLayer_1.getAdapterList().get(i).getDescription());
      }

      
      NIC_ComboBox.addItemListener(new ItemListener() {

         public void itemStateChanged(ItemEvent e) {
            adapterNumber = NIC_ComboBox.getSelectedIndex();
                                   
            try {

               Mac_address.setText(get_MacAddress(
                     m_PacketDriverLayer_1.getAdapterList().get(adapterNumber).getHardwareAddress()));

               String temp_Address = m_PacketDriverLayer_1.getAdapterList().get(adapterNumber).getAddresses()
                     .get(0).getAddr().toString();

            } catch (IOException e1) {
               // TODO Auto-generated catch block
               e1.printStackTrace();
            }
         }
      });

      NIC_ComboBox2.addItemListener(new ItemListener() {

         public void itemStateChanged(ItemEvent e) {
            adapterNumber2 = NIC_ComboBox2.getSelectedIndex();
            try {

               Mac_address2.setText(get_MacAddress2(
                     m_PacketDriverLayer_2.getAdapterList().get(adapterNumber2).getHardwareAddress()));

               String temp_Address = m_PacketDriverLayer_2.getAdapterList().get(adapterNumber2).getAddresses()
                     .get(0).getAddr().toString();

            } catch (IOException e1) {
               // TODO Auto-generated catch block
               e1.printStackTrace();
            }
         }
      });

      Mac_address2 = new JTextField();
      Mac_address2.setHorizontalAlignment(SwingConstants.CENTER);
      Mac_address2.setColumns(10);
      Mac_address2.setBounds(205, 25, 130, 20);
      AddressPanel2.add(Mac_address2);
      JPanel StaticRoutingPanel = new JPanel();
      StaticRoutingPanel.setLayout(null);
      StaticRoutingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"),
            "Static Routing Table", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      StaticRoutingPanel.setBounds(10, 10, 420, 440);
      contentPane.add(StaticRoutingPanel);

      JPanel StaticRoutingeditorPanel = new JPanel();
      StaticRoutingeditorPanel.setLayout(null);
      StaticRoutingeditorPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      StaticRoutingeditorPanel.setBounds(10, 15, 400, 150);
      StaticRoutingPanel.add(StaticRoutingeditorPanel);

      StaticRoutingList = new List();
      StaticRoutingList.setBounds(0, 0, 400, 150);
      StaticRoutingeditorPanel.add(StaticRoutingList);

      StaticRoutingAdd_btn = new JButton("Add");
      StaticRoutingAdd_btn.setEnabled(false);
      StaticRoutingAdd_btn.addActionListener(new setAddressListener());
      StaticRoutingAdd_btn.setBounds(80, 370, 100, 30);
      StaticRoutingPanel.add(StaticRoutingAdd_btn);

      StaticRoutingDelete_btn = new JButton("Delete");
      StaticRoutingDelete_btn.setEnabled(false);
      StaticRoutingDelete_btn.addActionListener(new setAddressListener());
      StaticRoutingDelete_btn.setBounds(240, 370, 100, 30);
      StaticRoutingPanel.add(StaticRoutingDelete_btn);

      Label label_7 = new Label("Destination");
      label_7.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_7.setAlignment(Label.CENTER);
      label_7.setBounds(25, 200, 70, 20);
      StaticRoutingPanel.add(label_7);

      Label label_8 = new Label("Netmask");
      label_8.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_8.setAlignment(Label.CENTER);
      label_8.setBounds(25, 230, 70, 20);
      StaticRoutingPanel.add(label_8);

      Label label_9 = new Label("Gateway");
      label_9.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_9.setAlignment(Label.CENTER);
      label_9.setBounds(25, 260, 70, 20);
      StaticRoutingPanel.add(label_9);

      Destination = new JTextField();
      Destination.setEnabled(false);
      Destination.setHorizontalAlignment(SwingConstants.CENTER);
      Destination.setColumns(10);
      Destination.setBounds(110, 200, 160, 20);
      Destination.setDocument(new JEditorPaneLimit(12));
      StaticRoutingPanel.add(Destination);

      Netmask = new JTextField();
      Netmask.setEnabled(false);
      Netmask.setHorizontalAlignment(SwingConstants.CENTER);
      Netmask.setColumns(10);
      Netmask.setBounds(110, 230, 160, 20);
      Netmask.setDocument(new JEditorPaneLimit(12));
      StaticRoutingPanel.add(Netmask);

      Gateway = new JTextField();
      Gateway.setEnabled(false);
      Gateway.setHorizontalAlignment(SwingConstants.CENTER);
      Gateway.setColumns(10);
      Gateway.setBounds(110, 260, 160, 20);
      Gateway.setDocument(new JEditorPaneLimit(12));
      StaticRoutingPanel.add(Gateway);

      Label label_10 = new Label("Flag");
      label_10.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_10.setAlignment(Label.CENTER);
      label_10.setBounds(25, 290, 70, 20);
      StaticRoutingPanel.add(label_10);

      CheckBoxUp = new JCheckBox("UP");
      CheckBoxUp.setEnabled(false);
      CheckBoxUp.setHorizontalAlignment(SwingConstants.CENTER);
      CheckBoxUp.setBounds(95, 290, 50, 20);
      StaticRoutingPanel.add(CheckBoxUp);

      CheckBoxGateway = new JCheckBox("GateWay");
      CheckBoxGateway.setEnabled(false);
      CheckBoxGateway.setHorizontalAlignment(SwingConstants.CENTER);
      CheckBoxGateway.setBounds(145, 290, 80, 20);
      StaticRoutingPanel.add(CheckBoxGateway);

      CheckBoxHost = new JCheckBox("Host");
      CheckBoxHost.setEnabled(false);
      CheckBoxHost.setHorizontalAlignment(SwingConstants.CENTER);
      CheckBoxHost.setBounds(225, 290, 60, 20);
      StaticRoutingPanel.add(CheckBoxHost);

      Label label_11 = new Label("Interface");
      label_11.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
      label_11.setAlignment(Label.CENTER);
      label_11.setBounds(25, 320, 70, 20);
      StaticRoutingPanel.add(label_11);
      
      interface_box = new JTextField();
      interface_box.setEnabled(false);
      interface_box.setHorizontalAlignment(SwingConstants.CENTER);
      interface_box.setColumns(10);
      interface_box.setBounds(110, 320, 160, 20);
      interface_box.setDocument(new JEditorPaneLimit(10));
      StaticRoutingPanel.add(interface_box);
      
      btnAClass = new JButton("A Class");
      btnAClass.setEnabled(false);
      btnAClass.setBounds(305, 230, 80, 20);
      btnAClass.addActionListener(new setAddressListener());
      StaticRoutingPanel.add(btnAClass);
      
      btnBClass = new JButton("B Class");
      btnBClass.setEnabled(false);
      btnBClass.setBounds(305, 260, 80, 20);
      btnBClass.addActionListener(new setAddressListener());
      StaticRoutingPanel.add(btnBClass);
      
      btnCClass = new JButton("C Class");
      btnCClass.setEnabled(false);
      btnCClass.setBounds(305, 290, 80, 20);
      btnCClass.addActionListener(new setAddressListener());
      StaticRoutingPanel.add(btnCClass);
      
      Label label_12 = new Label("Macro");
      label_12.setAlignment(Label.CENTER);
      label_12.setBounds(310, 200, 70, 20);
      StaticRoutingPanel.add(label_12);

   }
}