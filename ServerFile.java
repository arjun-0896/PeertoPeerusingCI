import java.io.IOException;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;

class peer_datastructure{
	public String host;
	public String port;
	public peer_datastructure(String host, String port) {
		this.host = host;
		this.port = port;
	}
}

class rfc_datastructure{
	public String host;
	public String port;
	public String title;
	public String rfc;
	public int cport;
	
	public rfc_datastructure(String rfc, String host, String port, String title, int cport){
		this.rfc = rfc;
		this.host = host;
		this.port = port;
		this.title = title;
		this.cport = cport;
	}
}

public class ServerFile implements Runnable{
	private static final String ci_version = "P2P-CI/1.0";
	public ServerSocket accept_socket; // socket to accept connection
	/* Synchronized lists are used to enable thread safe list as multithreading is used */
	public static List<Object> list_rfcs = Collections.synchronizedList(new ArrayList<Object>()); // list to store rfcs
	public static List<Object> list_peers = Collections.synchronizedList(new ArrayList<Object>()); // list to store active peers
	
	public ServerFile()throws IOException {
		//System.out.println("Hi A new accept socket is created");
	}
	
	public static void main(String args[]) throws IOException {
		
		if(args.length == 1){
			int port = Integer.parseInt(args[0]);
			new ServerFile().create_accept_socket(port); // A server socket with the port number will be created
		} 
		
		else
		{
			System.out.println("Invalid port");
		}
	}
	
	private void create_accept_socket(int port) {
		// TODO Auto-generated method stub
		try {
			accept_socket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      	try {
			System.out.println("A centralized server is started at "+ InetAddress.getLocalHost().getHostAddress()+ " on port " + accept_socket.getLocalPort());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Opearting System: " + System.getProperty("os.name"));
		System.out.println("CI-Version: " + ci_version);
		new Thread(this).start();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		ObjectInputStream  input_stream = null; 
	    ObjectOutputStream output_stream = null;
		Socket connection_socket = null;
		String host_name = null;
	    int client_port = 0;
		try {
		    connection_socket = accept_socket.accept(); // client connection established in connection_socket
	        new Thread(this).start();// A new thread for the connection
	        client_port = connection_socket.getPort(); // the port at the client side
			System.out.println("Client connection is estblished at port: "+ client_port);
			output_stream = new ObjectOutputStream(connection_socket.getOutputStream());
			input_stream = new ObjectInputStream (connection_socket.getInputStream());
			/* get the host name of the client and add the entry to the peer list */
		    host_name = (input_stream.readObject()).toString();
		    //System.out.println("I am here");
		    peer_datastructure peerObj = new peer_datastructure(host_name, Integer.toString(client_port));
			list_peers.add(peerObj);
			/* Now the peer's host name and the corresponding upload port is added to the peer list */
			System.out.println("The new peer is added successfully");
		    
		} catch(Exception e) {
			System.out.println("There is a connection failure " + e); 
			// remove the peer from the peer list cuz the connection failed
	        if (connection_socket.isConnected())
	        {
	        	delete_peer_Entry(client_port); // false indicates no transfer took place
	        		try 
	        		{
	        			connection_socket.close(); // close the connection to the client on port 'client_port'
	        		}catch(IOException ioexception)
	        		{
	        			System.out.println("Input output exception");
	        		} 
            	}
	        return;
		}
		 
	   try { 
		   System.out.println("Now start accepting requests from the client");
		   while (true)
	       {  
			   	String request = (String) input_stream.readObject();
			 	System.out.println(request);
			 	String[] result = request.trim().split("\\s"); // the step extracts the request as the first element of the String array
			 	implement_client_Request(result[0], input_stream, output_stream);
	        }       
		   
	  	} catch (Exception e) {
	  		delete_peer_rfcs(client_port);
	  		delete_peer_Entry(client_port);
	  		System.out.println("The Connection closes with client " + host_name +" @ " + client_port );
	  	}
	}
	
	private void delete_peer_Entry(int clientPort) {
		try {
			ListIterator<Object> piterator = list_peers.listIterator();
			peer_datastructure temp_peer = null;
			while((piterator.hasNext()))                                    
			{
				temp_peer = (peer_datastructure) piterator.next();
				if(Integer.toString(clientPort).equals(temp_peer.port)){
					list_peers.remove(temp_peer);
				}
			}
		} catch (ConcurrentModificationException e) 
		{
			/* to ensure thread safety */
		}
	}

	private void delete_peer_rfcs(int clientPort) {
		
			rfc_datastructure temp_list = null;
			int i = 0;
			
			/* deleting rfc entries that the client had it before */
			while(i < list_rfcs.size()) {
				temp_list = (rfc_datastructure) list_rfcs.get(i);
			    if(clientPort == temp_list.cport) {
			    	list_rfcs.remove(i);
			        i = 0;
			        continue;
			    }
			    i += 1;
			}
	}
	
	
	 private void implement_client_Request(String str, ObjectInputStream input_stream, ObjectOutputStream output_stream) throws ClassNotFoundException, IOException {
				str.trim();
				if (str.equals("ADD")){
					int clientPort = 0;
					try {
							String rfc = (String) input_stream.readObject();
							String hostName = (String) input_stream.readObject();
							String port = (String) input_stream.readObject();
							String title = (String) input_stream.readObject();
							clientPort = Integer.parseInt((String) input_stream.readObject());
							rfc_datastructure rfcObj = new rfc_datastructure(rfc, hostName, port, title, clientPort);
							list_rfcs.add(rfcObj);
							System.out.println("A new rfc is added");
							output_stream.writeObject("The received rfc is added successfully");
						} catch (NumberFormatException e) {
							delete_peer_Entry(clientPort);   
							System.out.println(e);
						}
					
				}
				else if (str.equals("LOOKUP"))
				{
					try {
						String rfcName = (String) input_stream.readObject();
						String rfcTitle = (String) input_stream.readObject();
						ListIterator<Object> iterator = list_rfcs.listIterator();
						rfc_datastructure temp_list = null;
						boolean exists = false;



						while((iterator.hasNext()))                                    
					    {
							temp_list = (rfc_datastructure) iterator.next();
					    	if(rfcName.equals(temp_list.rfc))
					    	{
					    		if(!exists)
					    		{
					    			output_stream.writeObject(ci_version + " 200 OK\n");
					    			exists = true;
					    		}
					    		
					    		output_stream.writeObject(rfcName + " " + rfcTitle + " " + temp_list.host + " " + temp_list.port + "\n");
					    	}
					    }
					    output_stream.writeObject("\n");
						if(!exists){
							output_stream.writeObject("404 Not Found");
						}
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
				}
				else if (str.equals("LIST"))
				{
					try{
						output_stream.writeObject(ci_version + " 200 OK\n");
						ListIterator<Object> iterator = list_rfcs.listIterator();
						rfc_datastructure temp_list = null;

					    while((iterator.hasNext()))                                    
					    {
					    	temp_list = (rfc_datastructure) iterator.next();
					    	output_stream.writeObject(temp_list.rfc + " " + temp_list.title + " " + temp_list.host + " " + temp_list.port + "\n");
					    }
					    //output_stream.writeObject("The entries are listed");
					    output_stream.writeObject("finish");
					}catch(Exception e){
						e.printStackTrace();	
					}
				}
				
		}
	
	
}


