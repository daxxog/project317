//  This file is free software; you can redistribute it and/or modify it under
//  the terms of the GNU General Public License version 2, 1991 as published by
//  the Free Software Foundation.

//  This program is distributed in the hope that it will be useful, but WITHOUT
//  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
//  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
//  details.

//  A copy of the GNU General Public License can be found at:
//    http://www.gnu.org/licenses/gpl.html

public class server implements Runnable {

	public server()
	{
		// the current way of controlling the server at runtime and a great debugging/testing tool
		//jserv js = new jserv(this);
		//js.start();

	}

	// TODO: yet to figure out proper value for timing, but 500 seems good
	public static final int cycleTime = 500;

	public static void main(java.lang.String args[])
	{
		clientHandler = new server();
		(new Thread(clientHandler)).start();			// launch server listener

		playerHandler = new PlayerHandler();

		int waitFails = 0;
		long lastTicks = System.currentTimeMillis();
		long totalTimeSpentProcessing = 0;
		int cycle = 0;
		while(!shutdownServer) {
			// could do game updating stuff in here...
			// maybe do all the major stuff here in a big loop and just do the packet
			// sending/receiving in the client subthreads. The actual packet forming code
			// will reside within here and all created packets are then relayed by the subthreads.
			// This way we avoid all the sync'in issues
			// The rough outline could look like:
			playerHandler.process();			// updates all player related stuff
			// doNpcs()		// all npc related stuff
			// doObjects()
			// doWhatever()

			// taking into account the time spend in the processing code for more accurate timing
			long timeSpent = System.currentTimeMillis()-lastTicks;
			totalTimeSpentProcessing += timeSpent;
			if(timeSpent >= cycleTime) {
				timeSpent = cycleTime;
				if(++waitFails > 100) {
					shutdownServer = true;
					misc.println("[KERNEL]: machine is too slow to run this server!");
				}
			}

			try { Thread.sleep(cycleTime-timeSpent); } catch(java.lang.Exception _ex) { }
			lastTicks = System.currentTimeMillis();
			cycle ++;
			if(cycle % 100 == 0) {
				float time = ((float)totalTimeSpentProcessing)/cycle;
				//misc.println_debug("[KERNEL]: "+(time*100/cycleTime)+"% processing time");
			}
		}

		// shut down the server
		playerHandler.destruct();
		clientHandler.killServer();
		clientHandler = null;
	}

	public static server clientHandler = null;			// handles all the clients
	public static java.net.ServerSocket clientListener = null;
	public static boolean shutdownServer = false;		// set this to true in order to shut down and kill the server
	public static boolean shutdownClientHandler;			// signals ClientHandler to shut down
	public static int serverlistenerPort = 43594; //43594=default

	public static PlayerHandler playerHandler = null;


	public void run() {
		// setup the listener
		try {
			shutdownClientHandler = false;
			clientListener = new java.net.ServerSocket(serverlistenerPort, 1, null);
			misc.println("Starting BlakeScape Server on " + clientListener.getInetAddress().getHostAddress() + ":" + clientListener.getLocalPort());
			while(true) {
				java.net.Socket s = clientListener.accept();
				s.setTcpNoDelay(true);
				String connectingHost = s.getInetAddress().getHostName();
				if(/*connectingHost.startsWith("localhost") || connectingHost.equals("127.0.0.1")*/true) {
					misc.println("ClientHandler: Accepted from "+connectingHost+":"+s.getPort());
					playerHandler.newPlayerClient(s, connectingHost);
				}
				else {
					misc.println("ClientHandler: Rejected "+connectingHost+":"+s.getPort());
					s.close();
				}
			}
		} catch(java.io.IOException ioe) {
			if(!shutdownClientHandler)
				misc.println("Error: Unable to startup listener on "+serverlistenerPort+" - port already in use?");
			else misc.println("ClientHandler was shut down.");
		}
	}

	public void killServer()
	{
		try {
			shutdownClientHandler = true;
			if(clientListener != null) clientListener.close();
			clientListener = null;
		} catch(java.lang.Exception __ex) {
			__ex.printStackTrace();
		}
	}
}
