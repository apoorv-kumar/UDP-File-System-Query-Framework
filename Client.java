import java.nio. *;
import java.net.UnknownHostException;
import java.nio.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client
{
	
	public static int STD_SERVER_PORT = 4434;
	public static int STD_CLIENT_PORT = 4435;
	
	
	public class NetPacket {
		
		public void set_params(short c_id, short typ , short stats , String ms) {
			con_id = c_id;
			type = typ;
			status = stats;
			msg = ms;
		}
		
		public NetPacket(short c_id) {
			con_id = c_id;
		}
		
		public NetPacket() {
		}
		
		public short con_id;
		public short type;
		public short status;
		public String msg; // size const in the true net packet
	}
	
	//send datagram
	public static void senddatagram(InetAddress target, int port, byte[] outbuf, int len) {
		
		try {
			DatagramPacket request = new DatagramPacket(outbuf, len, target, port);
			
			DatagramSocket socket = new DatagramSocket();
			socket.send(request);
		} catch (SocketException e) {
		} catch (IOException e) {
		}
		
	}
	
	// creates the packet in network order - ready to be sent
	public byte[] generate_packet(NetPacket np)
	{
		String file_name = np.msg;
		short con_id = np.con_id ;
		short type = np.type ;
		short status = np.status;
		//create a buffer of size 260*2
		ByteBuffer b = ByteBuffer.allocate(260*2); // see packet str
		byte[] final_buf = new byte[b.capacity()]; // this is to be sent finally
		// ------ header part -------
		//put the con id inside
		b.putShort(con_id);
		//put the type
		b.putShort(type);
		//put the status
		b.putShort(status);
		//put the name length
		b.putShort((short)file_name.length());
		//char buff
		short name_len = (short)file_name.length();
		for (short i = 0; i < name_len ; i++) {
			b.putChar(file_name.charAt((int)i));
		}
		//clear initial position to start copying in the byte array
		b.clear();
		//put all of it in the bytearray
		b.get(final_buf , 0 , final_buf.length);
		
		return final_buf;
		
	}
	
	public NetPacket parse_packet(byte[] packet)
	{
		//create a buffer of size 262
		ByteBuffer b = ByteBuffer.wrap(packet);
		
		
		//put the con id inside
		short con_id = b.getShort();
		//put the type
		short type =  b.getShort();
		//put the status
		short status =  b.getShort();
		//get name len
		short msg_len = b.getShort();
		//char buff
		char[] msg = new char[msg_len];
		for (short i = 0; i < msg_len ; i++) {
			msg[i] = b.getChar();
		}
		
		//print stuff
		NetPacket np = new NetPacket();
		np.msg = new String(msg);
		np.con_id = con_id;
		np.type = type;
		np.status = status;
		
		return np;
		
	}
	
	public void parse_response(NetPacket np)
	{
		if(np.type == 2){//DONE type
				if(np.status == 0)//success
				{
					System.out.println("------------------------------------------------------ ");
					System.out.println("the file"+" was located ... ");
					System.out.println("the last modification date of file is : " + np.msg);
					System.out.println("------------------------------------------------------ ");
				}
				else
				{
					System.out.println("-------------------------------------- ");
					System.out.println("the file"+" was not located ... ");
					System.out.println("-------------------------------------- ");
				}
		}


		if(np.type == 1){//REQ_ACK type


			System.out.println("a new connection id has been allocated: " + np.con_id);
			
			
		}
	}
	
	
	public void  run_req( String remote_host , String remote_file_name )
	{
		try {
			System.out.println("sending req to " + remote_host);
			Client my_client = new Client();
			NetPacket np = new NetPacket();
			np.set_params((short) -1, (short) 0, (short) 0, remote_file_name); //params for  REQ packet
			byte[] packet = my_client.generate_packet(np);
			
			InetAddress addr = InetAddress.getByName(remote_host);
			senddatagram(addr, STD_SERVER_PORT, packet, packet.length);
			//rcv packet
			System.out.println("request sent ...\n now waiting for reply ...");
			byte[] buffer = new byte[520];
			//init the socket
			DatagramPacket rcv_packet = new DatagramPacket(buffer, buffer.length);
			DatagramSocket socket;
			socket = new DatagramSocket(STD_CLIENT_PORT);
			//repeat procedure 2 times --- for ack and done
			//parse_response() prints the response of server on screen
			//iter1
			socket.receive(rcv_packet);
			buffer = rcv_packet.getData();
			np = parse_packet(buffer);
			parse_response(np);
			//iter2
			socket.receive(rcv_packet);
			buffer = rcv_packet.getData();
			np = parse_packet(buffer);
			parse_response(np);
			
			//get con_id
			short con_id = np.con_id;
			
			//if both packets received ... send DONE_ACK to ask the server to kill connection
			System.out.println("sending DONE_ACK to " + remote_host);
			np.set_params(con_id , (short)3 , (short)0 , ""); //params for  REQ packet
			packet = my_client.generate_packet(np);
			senddatagram(addr, STD_SERVER_PORT, packet, packet.length);
			
			
		} catch (IOException ex) {
			Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		} 
		
		
		
		
		
		
	}
	
	
	public static void main(String[] args)
	{
		System.out.println("starting the client ... ");
		
		
		String remote_file_name = args[1];
		String remote_host = args[0];
		
		
		Client my_client = new Client();
		my_client.run_req(remote_host, remote_file_name);
		
	}
}
