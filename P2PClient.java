import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Random;

public class P2PClient implements Runnable {

	ServerSocket servsok;

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Socket peerSock = null;
		
		try
		{
			peerSock = servsok.accept();
			new Thread(this).start();
			ObjectOutputStream out = new ObjectOutputStream(peerSock.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(peerSock.getInputStream());
			String reply = (String) in.readObject();
			System.out.println(reply);
			if (reply.contains("GET"))
			{
				String rfc = in.readObject().toString();
				File folder = new File("RFC");
				File pathToFile = new File(folder.getCanonicalPath()+"\\"+"RFC"+rfc+".txt");
				
				if(pathToFile.exists())
				{
					out.writeObject("P2P-CI/1.0 200 OK \n"+
							"Date: "+ (new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss")).format(new Date(0)) + " GMT\n"+
							"OS: " + System.getProperty("os.name")+ "\n"+
							"Last Modified: " + (new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss")).format(new Date(pathToFile.lastModified())) +" GMT \n"+
							"Content-Length: " + pathToFile.length() + "\nContent-Type: Text \n");
					byte[] content = Files.readAllBytes(pathToFile.toPath());
					out.writeObject(content);
				}
				else 
				{
					out.writeObject("P2P-CI/1.0 404 Not Found \n");
					out.flush();
				}
				out.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		
		if(args.length==1)
		{
			String address = args[0];
			String hostName = InetAddress.getLocalHost().getHostAddress();
			int port = 7734;
			Random rand = new Random();
			int clservport = 5000 + rand.nextInt(5000);
			new P2PClient().createClServSock(clservport);
			try
			{
				//System.out.println("DEBUG");
				Socket clsock = new Socket(address,port);
				//System.out.println("DEBUG");
				ObjectInputStream input = new ObjectInputStream(clsock.getInputStream());
				ObjectOutputStream output = new ObjectOutputStream(clsock.getOutputStream());
				//System.out.println("DEBUG");
				
				//System.out.println("DEBUG");
				output.writeObject(hostName);
				int clport=clsock.getLocalPort();
				System.out.println(hostName+" "+clport);
				BufferedReader ip = new BufferedReader(new InputStreamReader(System.in));
				String cmd;
				while(true)
				{
					System.out.println("Enter Command:");
					cmd = ip.readLine().trim();
					if(cmd.equalsIgnoreCase("ADD"))
					{
						add(input,output,ip,hostName,clservport,clport);
					}
					else if(cmd.equalsIgnoreCase("GET"))
					{
						get(input,output,ip,hostName,clservport,clport);
					}
					else if(cmd.equalsIgnoreCase("LIST"))
					{
						list(input,output,ip,hostName,clservport,clport);
					}
					else if(cmd.equalsIgnoreCase("LOOKUP"))
					{
						lookup(input,output,ip,hostName,clservport,clport);
					}
					else if(cmd.equalsIgnoreCase("EXIT"))
					{
						clsock.close();
						System.out.println("Closing Connection....");
						System.exit(0);
					}
					else
					{
						System.out.println("Please enter a valid Command");
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
		}
		else
		{
			System.out.println("Please provide hostname of the server !!!");
		}
	}
	
	void createClServSock(int port) throws IOException {
		//System.out.println("DEBUG");
		this.servsok = new ServerSocket(port);
		System.out.println("Client is Listening");
		new Thread(this).start();
	}
	
	static void add(ObjectInputStream in, ObjectOutputStream out, BufferedReader br, String host,
			int port, int lport)
	{
		String rfc=null;
		String rfct = null;
		try
		{
			System.out.println("Enter RFC:");
			rfc = br.readLine();
			System.out.println("Enter RFC title:");
			rfct = br.readLine();
			File dir = new File("RFC");
			File fil=null;
			fil = new File(dir.getCanonicalPath()+"\\"+"RFC"+rfc+".txt");
			System.out.println("RFC"+rfc+".txt");
			if((fil.exists()))
			{
				System.out.println(" ADD RFC " + rfc + "P2P-CI/1.0\n HOST:"+ host + "\n PORT:" + port + "\n TITLE:" + rfct + "\n");
				out.writeObject(" ADD RFC " + rfc + "P2P-CI/1.0\n HOST:"+ host + "\n PORT:" + port + "\n TITLE:" + rfct + "\n");
				out.writeObject(rfc);
				out.writeObject(host);
				out.writeObject(new Integer(port).toString());
				out.writeObject(rfct);
				out.writeObject(new Integer(lport).toString());
				System.out.println(in.readObject());
			}
			else
			{
				System.out.println("ERROR: File does not exists !!!");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	static void list(ObjectInputStream in, ObjectOutputStream out, BufferedReader br, String host,
			int port, int lport)
	{
		try 
		{
			out.writeObject(" LIST ALL P2P-CI/1.0\n HOST: " + host + "\n PORT: " + port + "\n");
			String reply = (in.readObject()).toString().trim();
			System.out.println(reply);
			if (! reply.startsWith("P2P-CI/1.0")) 
			{
				System.out.println("Error: Version Different !!!");
				return;
			}
			if (!(reply.contains("200 OK"))) 
			{
				System.out.println("Error Occured at Server: No list received");
			}
			else
			{
				reply = in.readObject().toString();
				while (reply.equalsIgnoreCase("finish") == false) 
				{
					//System.out.println("DEBUG");
					System.out.print(reply);
					reply = (String) in.readObject();
				}
				return;
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	static void lookup(ObjectInputStream in, ObjectOutputStream out, BufferedReader br, String host,
			int port, int lport)
	{
		try 
		{
			System.out.println("Enter the RFC to lookup:");
			String rfc = br.readLine().trim();
			System.out.println("Enter the title of the RFC:");
			String rfct = br.readLine().trim();
			out.writeObject(" LOOKUP RFC " + rfc + " P2P-CI/1.0\n HOST: "
					+ host + "\n PORT: " + port + "\n TITLE: " + rfct
					+ "\n");
			out.writeObject(rfc);
			out.writeObject(rfct);
			String reply = (in.readObject()).toString().trim();
			System.out.println(reply);
			if ((reply.contains("200 OK"))) 
			{
				reply = (String) in.readObject();
				while (reply.equalsIgnoreCase("\n") == false)
				{
					System.out.print(reply);
					reply = (String) in.readObject();
				}
				return;
			} 
			else
			{
				System.out.println("RFC does not exist");
			}
		} 
		catch (Exception e)
		{
			System.out.println("Error Occured at server");
		}
	}
	
	static void get(ObjectInputStream in, ObjectOutputStream out, BufferedReader br, String host,
			int port, int lport)
	{
		Socket peersock = null;
		String rfc = null;
		String peerhost = null;
		int peerport = 0;
		try 
		{
			System.out.println("Enter the RFC:");
			rfc = br.readLine().trim();
			System.out.println("Enter the host:");
			peerhost = br.readLine().trim();
			System.out.println("Enter the port:");
			peerport = Integer.parseInt(br.readLine().trim());
			peersock = new Socket(peerhost, peerport);
			ObjectOutputStream outs = new ObjectOutputStream(peersock.getOutputStream());
			ObjectInputStream ins = new ObjectInputStream(peersock.getInputStream());
			outs.writeObject(" GET RFC " + rfc + "  P2P-CI/1.0\n HOST: "+ host + "\n OS: " + System.getProperty("os.name") + "\n");
			outs.writeObject(rfc);
			String reply = ins.readObject().toString().trim();
			System.out.println(reply);
			if (!reply.startsWith("P2P-CI/1.0"))
			{
				System.out.println("Version difference with peer... Aborting...");
				return;
			}
			if ((reply.contains("200 OK"))) 
			{
				File dir = new File("RFC");
				File file = new File(dir.getCanonicalPath() + "\\RFC" + rfc + ".txt");
				file.createNewFile();
				byte[] content = (byte[]) ins.readObject();
				Files.write(file.toPath(), content);
			} 
			else
			{
				System.out.println("Error Occured: RFC not copied !!!");
			}
		} 
		catch (Exception e) 
		{
			System.out.println("Peer Unreachable !!!");
		}	
	}

}