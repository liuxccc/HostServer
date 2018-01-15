/*
 1. Refer to John Reagan's HostServer.java code. I typed it in, played with it and come out with these comments. More details goes to:
 http://condor.depaul.edu/elliott/435/hw/programs/hostserver/HostServer-Reagan.java.txt
 2. Name/Date:
 Xiaochang Liu
 03/01/2017
 3. Files needed to run this program:
 HostServer.java
 HostServer.class
 agentHolder.class
 AgentListener.class
 AgentWorker.class
 4. Instructions to run this program:
 >javac HostServer.java
 >java HostServer
 When program is running correctly in command shell, open the browser and get connect to local host:
 http://localhost:1565
 Then enter the text. What's more, you can try to refresh, new connection, migrate.
 5. Notes
 */



/**********
*Program overview:
*Here are four classes in this program. Here is the descriptions of their functions:
*agentWorker:
 *a. Update the state number
 *b. get a new gost (when client choose to migrate)
 *c. send HTML form message back to client
 *d. kill the former agentListener.
*agentHolder: hold the state information
*agentListner:
 *a. begin with the initial state
 *b. get next availiable sate number
 *c. send the port number back to client
*HostServer: Listening at port 1565. Wait for requests.
 
**********/



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

class AgentWorker extends Thread {//class definition
	
	Socket sock; //define the socket which is connect to client
	agentHolder parentAgentHolder; //hold the sockt and state number for agentstate
	int localPort; //which port is being used
	
	
	AgentWorker (Socket s, int prt, agentHolder ah) {//define members of class
		sock = s;
		localPort = prt;
		parentAgentHolder = ah;
	}
	public void run() {
		
		//Get I/O streams in/out from the socket
        //initialize the variebles
		PrintStream out = null;
		BufferedReader in = null;
		String NewHost = "localhost";
		//the main work will run on port 1565
		int NewHostMainPort = 1565;		
		String buf = "";//initialize the variable
		int newPort;
		Socket clientSock;
		BufferedReader fromHostServer;
		PrintStream toHostServer;
		
		try {
			out = new PrintStream(sock.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			//read line from the client input
			String inLine = in.readLine();
			//allocates the maximum space for object
			StringBuilder htmlString = new StringBuilder();
			
			//client request
			System.out.println();
			System.out.println("Request line: " + inLine);
			
			if(inLine.indexOf("migrate") > -1) {
				//if the input is migrate, switch the client to a new port
				//creat a new port
                //wait at port 1565
				clientSock = new Socket(NewHost, NewHostMainPort);
				fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
				//send a request to port 1565
                //get the next open port
				toHostServer = new PrintStream(clientSock.getOutputStream());
				toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]");
				toHostServer.flush();
				
				//wait for the response
                //read a response until get the next avaliable port
				for(;;) {
					//read line
                    //check if it could be the next port
					buf = fromHostServer.readLine();
					if(buf.indexOf("[Port=") > -1) {
						break;
					}
				}
				
				//check the format of the response
                //get the new port and save it in tempbuf
				String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) );
				newPort = Integer.parseInt(tempbuf);//parse the response by integer
				System.out.println("newPort is: " + newPort);//use it as new port
				
				htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine));
				//show this message on console to inform the clients that the migration
                //has been completed
				htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n");
				htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");
				//finish
				
                htmlString.append(AgentListener.sendHTMLsubmit());
                //kill the server waiting at the port
				System.out.println("Killing parent listening loop.");
                //store the used port in the parentAgentHolder
				ServerSocket ss = parentAgentHolder.sock;
				//close the socket
				ss.close();
				
				
			} else if(inLine.indexOf("person") > -1) {
				//count the how much time does client touch the trigger button
                //store it as state number
				parentAgentHolder.agentState++;
				//display the state number on console
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
				htmlString.append(AgentListener.sendHTMLsubmit());

			} else {
				//display the the message that request is invalid
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("You have not entered a valid request!\n");
				htmlString.append(AgentListener.sendHTMLsubmit());		
				
		
			}
			//here is the output
			AgentListener.sendHTMLtoStream(htmlString.toString(), out);
			
			//close the socket
            //close this connection, but not the server
			sock.close();
			
			
		} catch (IOException ioe) {//catch the errors
			System.out.println(ioe);
		}
	}
	
}
/*****
 ** The agentHolder class is for holding the state information.
 ** Keep records of the state number when we migrating to another port.
 *****/
class agentHolder {
	//define and active the socket
	ServerSocket sock;
	//initiate the variable
	int agentState;
	
	agentHolder(ServerSocket s) { sock = s;}
}
/*****
 ** The agentListener class is used for monitoring every different
 ** port and send back the response to the requests.
 ******/
class AgentListener extends Thread {
	//instance vars
	Socket sock;
	int localPort;
	
	AgentListener(Socket As, int prt) {
		sock = As;
		localPort = prt;
	}
	//initiate the state
    //set it to 0 at the begining
	int agentState = 0;
	
	//called from start() when a request is made on the listening port
	public void run() {
		BufferedReader in = null;
		PrintStream out = null;
		String NewHost = "localhost";
		System.out.println("In AgentListener Thread");		
		try {
			String buf;
			out = new PrintStream(sock.getOutputStream());
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			buf = in.readLine();//read the first line
			
			//parse the request
			if(buf != null && buf.indexOf("[State=") > -1) {
				//check if there is a state, store it in tempbuf
				String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State=")));
				//parse the state
				agentState = Integer.parseInt(tempbuf);
				//display the agentState on console
				System.out.println("agentState is: " + agentState);
					
			}
			
			System.out.println(buf);
			//hold the response in htmlResponse
			StringBuilder htmlResponse = new StringBuilder();
			//display the first response on cosole
			//diaplay the message and which port is being used.
			htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));
			htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
			htmlResponse.append("[Port="+localPort+"]<br/>\n");
			htmlResponse.append(sendHTMLsubmit());
			
			sendHTMLtoStream(htmlResponse.toString(), out);
			
			//begin the connection
			ServerSocket servsock = new ServerSocket(localPort,2);
			//store the socket and state number in agentHold
			agentHolder agenthold = new agentHolder(servsock);
			agenthold.agentState = agentState;
			
			//wait
			while(true) {
				sock = servsock.accept();
				//receiving the connection.
                //display the message
				System.out.println("Got a connection to agent at port " + localPort);
				//connection received.
                //startc the new agentWorker
				new AgentWorker(sock, localPort, agenthold).start();
			}
		
		} catch(IOException ioe) {
			//catch the error during switch
			System.out.println("Either connection failed, or just killed listener loop for agent at port " + localPort);
			System.out.println(ioe);
		}
	}


    //This function is used to send messages
	static String sendHTMLheader(int localPort, String NewHost, String inLine) {
		
		StringBuilder htmlString = new StringBuilder();
        //get html and form. record the port.
        //or go to the new port which we are listening.
		htmlString.append("<html><head> </head><body>\n");//display the html header
		htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");
		htmlString.append("<h3>You sent: "+ inLine + "</h3>");
		htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");
		htmlString.append("Enter text or <i>migrate</i>:");
		htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");
		
		return htmlString.toString();
	}
	//submit the message we send by sendHTMLheader
    //finish the process
	static String sendHTMLsubmit() {
		return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
	}
	//diaplay the response headers
    //diaplay the content length(good for using other browsers)
    //better not use it with ie browser
	static void sendHTMLtoStream(String html, PrintStream out) {
		
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + html.length());
		out.println("Content-Type: text/html");
		out.println("");		
		out.println(html);
	}
	
}
/*******
 * 
 ** This main class HostServer is listening at port 1565.
 ** Receive the request, increment the port number.
 ** Move to next port every time receive the request and listen to the new port.
******/
public class HostServer {
	//start at port 3001
	public static int NextPort = 3000;
	
	public static void main(String[] a) throws IOException {
		int q_len = 6;//
		int port = 1565;
		Socket sock;
		
		ServerSocket servsock = new ServerSocket(port, q_len);
		System.out.println("John Reagan's DIA Master receiver started at port 1565.");
		System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:1565\"\n");
		
		while(true) {//listening at port 1565
			NextPort = NextPort + 1;//increment the port number
			sock = servsock.accept();//open connection. wait for the client
			System.out.println("Starting AgentListener at port " + NextPort);//diaplay the port number
			new AgentListener(sock, NextPort).start();//call the worker. create and going to listen at new port
		}
		
	}
}
