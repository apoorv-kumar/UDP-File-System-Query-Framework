import java.io.*;
import java.nio.*;
import java.net.*;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.util.*;

public class Server {
	/* main server class */
	
	public class Connection {
		/* class Stores connection data for each connection */
		public InetAddress client_addr;
		public List<String> pending_requests = new ArrayList<String>(); // the pending requests queue
		boolean theEnd = false; // marks whether the thread is supposed to be closed (after getting DONE_ACK) - used to come out of wait() loop.
	}
	
	public String get_last_modif_date(String file_addr)
	{/*func returns the  last modified information for a file */
	// Create an instance of file object.
	try {
		File file = new File(file_addr);
		// Get the last modification information.
		Long lastModified = file.lastModified();
		
		// Create a new date object and pass last modified information
		// to the date object.
		Date date = new Date(lastModified);
		
		// We know when the last time the file was modified.
		System.out.println("found modification date of : " + file_addr + " - " + date.toString());
		return date.toString();
	} catch (Exception e) {
		return "file not found";
	}
	
	
	}
	
	//send datagram of size "len" from "outbuf" to given "address:port"
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
	
	//parses network order packet back into a NetPacket class
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
		short name_len = b.getShort();
		//char buff
		char[] file_name = new char[name_len];
		for (short i = 0; i < name_len ; i++) {
			file_name[i] = b.getChar();
		}
		
		
		
		//print stuff
		NetPacket np = new NetPacket();
		np.msg = new String(file_name);
		np.con_id = con_id;
		np.type = type;
		np.status = status;
		
		return np;
		
	}
	
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
	
	
	public static Connection[] central_con_list = new Connection[1000];
	public static short current_max_id = 0;
	public static int STD_SERVER_PORT = 4434;
	public static int STD_CLIENT_PORT = 4435;
	
	//producer consumer problem
	
	class ConsumeAndServe implements Runnable {
		
		short con_id;
		
		public ConsumeAndServe(short c_id) {
			con_id = c_id;
		}			
		
		
		public void run () {
			
			while (!central_con_list[con_id].theEnd) {
				synchronized (central_con_list[con_id].pending_requests) {
					while (central_con_list[con_id].pending_requests.isEmpty() && (!central_con_list[con_id].theEnd)) {
						try {
							central_con_list[con_id].pending_requests.wait();
						} catch (InterruptedException e) {
							Thread.interrupted();
						}
					}
					//process the request
					//this hack is efficient if only one thread is trying to access the resource. which is true in this case
					//otherwise the processing should be done out of loop.
					if (!central_con_list[con_id].pending_requests.isEmpty()) {
						System.out.println("Processing request from " + con_id);
						String file_to_process = central_con_list[con_id].pending_requests.remove(0);
						
						//get file detail from server...
						String last_mod_date = get_last_modif_date(file_to_process);
						
						//net packet to be sent
						NetPacket np = new NetPacket();
						
						if (last_mod_date.compareTo("Thu Jan 01 05:30:00 IST 1970") == 0 ) {
							//send back the error report to client
							
							np.con_id = con_id;
							np.type  = 2; // DONE_PACKET TYPE
							np.status = 1; // send error signal
							np.msg = "";
						}
						else{
							//send back the info to client
							
							np.con_id = con_id;
							np.type  = 2; // DONE_PACKET TYPE
							np.status = 0;// successful completion of query
							np.msg = last_mod_date;
							
						}
						
						//convert the date into network str
						byte[] buffer = generate_packet(np);
						
						//send the packet
						senddatagram(central_con_list[con_id].client_addr, STD_CLIENT_PORT, buffer, buffer.length);
						
						
					}
				}
				
				
				
			}
			
		}
	}
	
	
	//produce -- after receiving req
	// don't need a runnable class as it is used only by one thread; thus no sync
	public void produce(String file_name , short con_id) {
		
		System.out.println("Producing " + file_name + " for: " + con_id);
		synchronized (central_con_list[con_id].pending_requests) {
			central_con_list[con_id].pending_requests.add(file_name);
			central_con_list[con_id].pending_requests.notifyAll();
		}
	}
	
	//consume -- and serve
	
	public void start() throws InterruptedException
	{
		try {
			
			//clear the connection list and init
			for (int i = 0; i < central_con_list.length; i++) {
				central_con_list[i] = null;
			}
			byte[] buffer = new byte[530]; // actually 520 is the max -- some extra
			
			//receive a new packet
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			DatagramSocket socket = new DatagramSocket(STD_SERVER_PORT);
			System.out.println("waiting for packets ... ");
			
			while(true){
				

				socket.receive(packet);
				
				//decide what to do with packet
				InetAddress client_addr = packet.getAddress();
				buffer = packet.getData();
				NetPacket np = parse_packet(buffer);
				
				//if the packet is a REQ
				if (np.type == 0) // REQUEST_CODE
			{
				if(np.con_id == -1)// needs new thread
				{
					short new_con_id = current_max_id;
					current_max_id += 1;
					
					//create new connection
					Connection new_con = new Connection();
					new_con.client_addr = client_addr;
					new_con.pending_requests.clear();
					
					//add to the central list
					System.out.println("creating new connection for the client - " + client_addr.toString() + " - id: " + new_con_id);
					central_con_list[new_con_id] = new_con;
					
					//insert the data in packet list for connection
					produce(np.msg, new_con_id);
					//send an ack to client informing about accepted request
					np.con_id = new_con_id;
					np.status = 0;
					np.type = 1 ; // REQ_ACK type
					
					//get the network format of data
					buffer = generate_packet(np);
					
					// send data
					senddatagram(client_addr, STD_CLIENT_PORT, buffer, buffer.length);
					System.out.println("ACK datagram sent");
					//fork a thread to serve the connection
					Thread n_thread = new Thread(new ConsumeAndServe(new_con_id), Short.toString(new_con_id));
					n_thread.start();
				}
				else // previously present id ... just add to queue
				{
					//insert the data in packet list for connection
					if(central_con_list[np.con_id] == null)
					{
						//report error
						System.out.println("invalid request from the client - " + client_addr.toString());
					}
					else
					{
						System.out.println("queued from the client - " + client_addr.toString() + " - filename: " + np.msg);
						produce(np.msg, np.con_id);
					}
				}
				
			}
			else if(np.type == 3)//DONE_ACK type
			{
				//request to end the connection
				//ensure that this is not a packet spoofing attack - ip addr should be consistent
				if(client_addr == central_con_list[np.con_id].client_addr)
				{
					//delete connection
					System.out.println("closing connection from client - " + client_addr.toString());
					central_con_list[np.con_id].theEnd = true;
				}
			}
			else // invalid request
			{
				//report error
				System.out.println("invalid request from the client - " + client_addr.toString());
			}
			
			
			}
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	
	public static void main(String args[])
	{
		Server my_server = new Server();
		try {
			my_server.start(); // start running the server
		} catch (InterruptedException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
		
	}
	
}