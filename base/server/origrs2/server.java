// To make use of this server you have to patch the client (only #317 will work!)
// so that it disables the RSA encryption. You can either do so by commenting out
// the modPow instruction or by setting the keypair to 1 (the smaller of the two keys)
// and something high enough (the larger one): 1000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
// I will not provide the client, please do NOT ask me!

// I know this code is still messed up, but maybe some people want to play with it as I don't know
// when they're going to update. If that happens we no longer can make use of sniffed packet data
// unless someone wants to update it for a new client. But be aware the protocol will change 
// significantly after such an event.

//  This file (server.java) is free software; you can redistribute it and/or modify it under
//  the terms of the GNU General Public License version 2, 1991 as published by
//  the Free Software Foundation.

//  This program is distributed in the hope that it will be useful, but WITHOUT
//  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
//  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
//  details.

//  A copy of the GNU General Public License can be found at:
//    http://www.gnu.org/licenses/gpl.html

// Note that the protocol is copyright by jagex.

public class server {
	// some misc functions
	public static void print_debug(String str)
	{
		System.out.print(str);				// comment this line out if you want to get rid of debug messages
	}
	public static void println_debug(String str)
	{
		System.out.println(str);
	}
	public static void print(String str)
	{
		System.out.print(str);
	}
	public static void println(String str)
	{
		System.out.println(str);
	}
	public static String Hex(byte data[], int offset, int len)
	{
		String temp = "";
		for(int cntr = 0; cntr < len; cntr++) {
			int num = data[offset+cntr] & 0xFF;
			String myStr;
			if(num < 16) myStr = "0";
			else myStr = "";
			temp += myStr + Integer.toHexString(num) + " ";
		}
		return temp.toUpperCase().trim();
	}
	public static int random(int range) {
		return (int)(java.lang.Math.random() * (range+1));
	}

	public static boolean shutdownServer = false;
	public static void main(java.lang.String args[])
	{
		clientHandler = new ClientHandler(null);
		(new Thread(clientHandler)).start();			// launch server listener

		while(!shutdownServer) {
			// could do game updating stuff in here...
			// maybe do all the major stuff here in a big loop and just do the packet
			// sending/receiving in the client subthreads. The actual packet forming code
			// will reside within here and all created packets are then relayed by the subthreads.
			// This way we avoid all the sync'in issues
			// The rough outline could look like:
			// doPlayers()		// updates all player related stuff
			// doNpcs()		// all npc related stuff
			// doObjects()
			// doWhatever()
			try { Thread.sleep(2000); } catch(java.lang.Exception _ex) { }
		}

		// shut down the server
		clientHandler.kill();
		clientHandler = null;
	}

	private static ClientHandler clientHandler = null; 	// handles all the clients
	private static java.net.ServerSocket clientListener = null;
	private static boolean shutdownClientHandler;			// signals ClientHandler to shut down
	private static int serverlistenerPort = 43594;

	public static class ClientHandler implements Runnable {
		private java.net.Socket mySock;
	    private java.io.InputStream in;
	    private java.io.OutputStream out;
		public String myName = null;			// name of the connecting client
		public stream inStream = null, outStream = null;
		public Cryption inStreamDecryption = null, outStreamDecryption = null;
		public boolean clientConnected = false;
		public ClientHandler(java.net.Socket s) {
			mySock = s;
			myName = "";
			if(s != null) {
				try {
			        in = s.getInputStream();
			        out = s.getOutputStream();
				} catch(java.io.IOException ioe) {
					ioe.printStackTrace();
				}
			}
			else {
				in = null;
				out = null;
			}
		}
		public void flushOutStream() throws java.io.IOException
		{
			out.write(outStream.buffer, 0, outStream.currentOffset);
			outStream.currentOffset = 0;		// reset
		}
		// forces to read forceRead bytes from the client - block until we have received those
		public void fillInStream(int forceRead) throws java.io.IOException
		{
			inStream.currentOffset = 0;
			in.read(inStream.buffer, 0, forceRead);
		}

		public void run() {
			if(clientListener == null) {
				// setup the listener
				try {
					shutdownClientHandler = false;
					clientListener = new java.net.ServerSocket(serverlistenerPort, 1, null);
					System.out.println("Starting BlakeScape Server on " + clientListener.getInetAddress().getHostAddress() + ":" + clientListener.getLocalPort());
					while(true) {
						java.net.Socket s = clientListener.accept();
						s.setTcpNoDelay(true);
						String connectingHost = s.getInetAddress().getHostName();
						if(/*connectingHost.startsWith("localhost") || connectingHost.equals("127.0.0.1")*/true) {
							System.out.println("ClientHandler: Accepted from "+connectingHost+":"+s.getPort());
							(new Thread(new ClientHandler(s))).start();
						}
						else {
							System.out.println("ClientHandler: Rejected "+connectingHost+":"+s.getPort());
							s.close();
						}
					}
				} catch(java.io.IOException ioe) {
					if(!shutdownClientHandler)
						System.out.println("Error: Unable to startup listener on "+serverlistenerPort+" - port already in use?");
					else System.out.println("ClientHandler was shut down.");
				}
				return; 	// we're done in here
			}


			if(mySock == null) return;		// no socket there - closed

			// we accepted a new connection
			outStream = new stream(new byte[100000]);
			outStream.currentOffset = 0;
			inStream = new stream(new byte[10000]);
			inStream.currentOffset = 0;

			// handle the login stuff
			long serverSessionKey = 0, clientSessionKey = 0;
			// randomize server part of the session key
			serverSessionKey = ((long)(java.lang.Math.random() * 99999999D) << 32) + (long)(java.lang.Math.random() * 99999999D);

			try {
				fillInStream(2);
				if(inStream.readUnsignedByte() != 14) {
					shutdownError("Expected login Id 14 from client.");
					return;
				}
				// this is part of the usename. Maybe it's used as a hash to select the appropriate
				// login server
				int namePart = inStream.readUnsignedByte();
				for(int i = 0; i < 8; i++) out.write(0);		// is being ignored by the client

				// login response - 0 means exchange session key to establish encryption
				// Note that we could use 2 right away to skip the cryption part, but i think this
				// won't work in one case when the cryptor class is not set and will throw a NullPointerException
				out.write(0);

				// send the server part of the session Id used (client+server part together are used as cryption key)
				outStream.writeQWord(serverSessionKey);
				flushOutStream();
				fillInStream(2);
				int loginType = inStream.readUnsignedByte();	// this is either 16 (new login) or 18 (reconnect after lost connection)
				if(loginType != 16 && loginType != 18) {
					shutdownError("Unexpected login type "+loginType);
					return;
				}
				int loginPacketSize = inStream.readUnsignedByte();
				int loginEncryptPacketSize = loginPacketSize-(36+1+1+2);	// the size of the RSA encrypted part (containing password)
				println_debug("LoginPacket size: "+loginPacketSize+", RSA packet size: "+loginEncryptPacketSize);
				if(loginEncryptPacketSize <= 0) {
					shutdownError("Zero RSA packet size!");
					return;
				}
				fillInStream(loginPacketSize);
				if(inStream.readUnsignedByte() != 255 || inStream.readUnsignedWord() != 317) {
					shutdownError("Wrong login packet magic ID (expected 255, 317)");
					return;
				}
				int lowMemoryVersion = inStream.readUnsignedByte();
				println_debug("Client type: "+((lowMemoryVersion==1) ? "low" : "high")+" memory version");
				for(int i = 0; i < 9; i++) {
					println_debug("dataFileVersion["+i+"]: 0x"+Integer.toHexString(inStream.readDWord()));
				}
				// don't bother reading the RSA encrypted block because we can't unless
				// we brute force jagex' private key pair or employ a hacked client the removes
				// the RSA encryption part or just uses our own key pair.
				// Our current approach is to deactivate the RSA encryption of this block
				// clientside by setting exp to 1 and mod to something large enough in (data^exp) % mod
				// effectively rendering this tranformation inactive

				loginEncryptPacketSize--;		// don't count length byte
				int tmp = inStream.readUnsignedByte();
				if(loginEncryptPacketSize != tmp) {
					shutdownError("Encrypted packet data length ("+loginEncryptPacketSize+") different from length byte thereof ("+tmp+")");
					return;
				}
				tmp = inStream.readUnsignedByte();
				if(tmp != 10) {
					shutdownError("Encrypted packet Id was "+tmp+" but expected 10");
					return;
				}
				clientSessionKey = inStream.readQWord();
				serverSessionKey = inStream.readQWord();
				println("UserId: "+inStream.readDWord());
				myName = inStream.readString();
				String password = inStream.readString();
				println("Indent: "+myName+":"+password);

				int sessionKey[] = new int[4];
				sessionKey[0] = (int)(clientSessionKey >> 32);
				sessionKey[1] = (int)clientSessionKey;
				sessionKey[2] = (int)(serverSessionKey >> 32);
				sessionKey[3] = (int)serverSessionKey;

				for(int i = 0; i < 4; i++)
					println_debug("inStreamSessionKey["+i+"]: 0x"+Integer.toHexString(sessionKey[i]));

				inStreamDecryption = new Cryption(sessionKey);
				for(int i = 0; i < 4; i++) sessionKey[i] += 50;

				for(int i = 0; i < 4; i++)
					println_debug("outStreamSessionKey["+i+"]: 0x"+Integer.toHexString(sessionKey[i]));

				outStreamDecryption = new Cryption(sessionKey);
				outStream.packetEncryption = outStreamDecryption;

				out.write(2);		// login response (1: wait 2seconds, 2=login successfull, 4=ban :-)
				out.write(0);		// mod level: 0=normal player, 1=player mod, 2=real mod
				out.write(0);		// no log

			} catch(java.lang.Exception __ex) {
				System.out.println("BlakeScape Server: Exception!");
				__ex.printStackTrace(); 
				shutdown();
				return;
			}
			// End of login procedure


			// those packets were sniffed from a real session and split into parts
			int mapRegionX = 0x182, mapRegionY = 0x195; 	// change those to get a different starting point
			//int mapRegionX = 270, mapRegionY = 350; 	// change those to get a different starting point

			// initiate loading of new map area
			outStream.createFrame(73);
			outStream.writeWordA(mapRegionX);
			outStream.writeWord(mapRegionY);

			// players initialization
			outStream.createFrame(81);
			outStream.writeWord(0); 		// placeholder for size of this packet.
			int ofs = outStream.currentOffset;
			outStream.initBitAccess();

			// update this player
			outStream.writeBits(1, 1);		// set to true if updating thisPlayer
			outStream.writeBits(2, 3);		// updateType - 3=jump to pos
			// the following applies to type 3 only
			outStream.writeBits(2, 0);		// height level (0-3)
			outStream.writeBits(1, 1);		// set to true, if discarding walking queue (after teleport e.g.)
			outStream.writeBits(1, 1);		// set to true, if this player is not in local list yet???
			outStream.writeBits(7, 0x20);	// y-position
			outStream.writeBits(7, 0x20);	// x-position

			// update other players...?!
			outStream.writeBits(8, 0);		// number of players to add

			// add new players???
			outStream.writeBits(11, 2047);	// magic EOF
			outStream.finishBitAccess();

			outStream.writeByte(0);		// ???? needed that to stop client from crashing

			outStream.writeFrameSizeWord(outStream.currentOffset - ofs);

			CreateNoobyItems();


			// the major part was done, now we're just looping reading incoming packets
			// and sending out some testing shit...
			packetType = -1;
			packetSize = 0;
			clientConnected = true;
			int walkingSteps = 0;
			int currentCoord = 0x20;
			while(clientConnected) {
				while(interpreteIncommingPackets());
				if(!clientConnected) break;

				// here we place the code that handles the outgoing stuff

				// some n00by walking packet
				outStream.createFrame(81);
				outStream.writeWord(0); 		// placeholder for size of this packet.
				ofs = outStream.currentOffset;

				// update thisPlayer
				outStream.initBitAccess();
				outStream.writeBits(1, 1);		// set to true if updating thisPlayer

				// walk code
/*				outStream.writeBits(2, 1);		// updateType - 1=walk in direction
				outStream.writeBits(3, 1);		// direction
				outStream.writeBits(1, 0);		// set to true, if this player is not in local list yet???
*/

				// run code
				outStream.writeBits(2, 2);		// updateType - 2=run in direction
				outStream.writeBits(3, 1);		// direction step 1
				outStream.writeBits(3, 1);		// direction step 2
				outStream.writeBits(1, 0);		// set to true, if this player is not in local list yet???


				outStream.writeBits(8, 0);		// no other players...
				outStream.finishBitAccess();

				outStream.writeFrameSizeWord(outStream.currentOffset - ofs);	// write size

				currentCoord+=2;		// +=1 for walking
				if(currentCoord > 90) {
					if(mapRegionY > 500) {
						//outStream.createFrame(109); 	// log out
						//println("Forcing log out of client!");
						println("Next row...");
						mapRegionX += 8;
						mapRegionY = 350;
						outStream.createFrame(73);
						outStream.writeWordA(mapRegionX);
						outStream.writeWord(mapRegionY);
						try {
							flushOutStream();
							Thread.sleep(2000); // wait some time until client loaded new area
						} catch(Exception x) {}





					// teleport packet
					outStream.createFrame(81);
					outStream.writeWord(0); 		// placeholder for size of this packet.
					ofs = outStream.currentOffset;

					// update thisPlayer
					outStream.initBitAccess();
					outStream.writeBits(1, 1);		// set to true if updating thisPlayer

					outStream.writeBits(2, 3);		// updateType - 3=jump to pos
					// the following applies to type 3 only
					outStream.writeBits(2, 0);		// height level (0-3)
					outStream.writeBits(1, 1);		// set to true, if discarding walking queue (after teleport e.g.)
					outStream.writeBits(1, 0);		// set to true, if this player is not in local list yet???
					outStream.writeBits(7, 0x20);	// y-position
					outStream.writeBits(7, 0x20);	// x-position

					// update other players...?!
					outStream.writeBits(8, 0);		// number of players that follow

					outStream.finishBitAccess();

					outStream.writeFrameSizeWord(outStream.currentOffset - ofs);
					
					currentCoord = 0x20;





					}
					else {
						outStream.createFrame(73);
						outStream.writeWordA(mapRegionX);
						mapRegionY += 8;
						currentCoord -= 8*8;		// granularity of a map region is 8
						outStream.writeWord(mapRegionY);
						println("Entering map region "+mapRegionX+","+mapRegionY);
						CreateNoobyItems();
						try {
							flushOutStream();
							Thread.sleep(2000); // wait some time until client loaded new area
						} catch(Exception x) {}
					}
				}


				if(!clientConnected) break;

				timeOutCounter++;
				if(timeOutCounter++ > 200) {
					println("Client lost connection: timeout");
					break;
				}

				// send all cummulated packets to the client
				try {
					flushOutStream();
					Thread.sleep(500);		// TODO: think about a way to sync all this stuff and about timing
				} catch(java.lang.Exception __ex) {
					System.out.println("BlakeScape Server: Exception!");
					__ex.printStackTrace(); 
					clientConnected = false;
				}
			}

			// we're done
			shutdown();
		}

		public void shutdownError(String errorMessage)
		{
			println("Fatal: "+errorMessage);
			shutdown();
		}
		public void shutdown()
		{
			try {
				System.out.println("ClientHandler: Client "+myName+" disconnected.");
				if(in != null) in.close();
				if(out != null) out.close();
				mySock.close();
				mySock = null;
				in = null;
				out = null;
			} catch(java.io.IOException ioe) {
				ioe.printStackTrace();
			}
		}

		public void kill()
		{
			try {
				shutdownClientHandler = true;
				if(clientListener != null) clientListener.close();
				clientListener = null;
			} catch(java.lang.Exception __ex) {
				__ex.printStackTrace();
			}
		}

	// just testing...
	public void CreateNoobyItems()
	{
		// this performs very slow for huge amount of items
/*		for(int x = 1; x < 100; x++) {
			for(int y = 1; y < 100; y++) {
				outStream.createFrame(85);
				outStream.writeByteC(y); 	// baseY
				outStream.writeByteC(x); 	// baseX

				outStream.createFrame(44);
				outStream.writeWordBigEndianA(random(1,1000)); 	// objectType
				outStream.writeWord(1);						// amount
				outStream.writeByte(0);						// x(4 MSB) y(LSB) coords
			}
		} */
		// send all items combined to larger clusters
		for(int x = 0; x < 11; x++) {
			for(int y = 0; y < 11; y++) {
				outStream.createFrame(60);
				outStream.writeWord(0); 		// placeholder for size of this packet.
				int ofs = outStream.currentOffset;

				outStream.writeByte(y*8); 	// baseY
				outStream.writeByteC(x*8); 	// baseX
				// here come the actual packets
				for(int kx = 0; kx < 8; kx++) {
					for(int ky = 0; ky < 8; ky++) {
						outStream.writeByte(44);		// formerly createFrame, but its just a plain byte in this encapsulated packet
						outStream.writeWordBigEndianA(random(1000)); 	// objectType
						outStream.writeWord(1);						// amount
						outStream.writeByte(kx*16+ky);						// x(4 MSB) y(LSB) coords
					}
				}

				outStream.writeFrameSizeWord(outStream.currentOffset - ofs);
			}
		}
	}

	public int packetSize = 0, packetType = 0;
	public int timeOutCounter = 0;
	public boolean interpreteIncommingPackets()
	{
		try {
			int avail = in.available();
			if(avail == 0) return false;

			// Important note: doesn't work yet because we don't know the lengths of any possible
			// incoming packets so we can't parse them and might miss the one or other ending up
			// with screwing the encryption
			if(packetType == -1) {
				packetType = in.read() & 0xff;
				if(inStreamDecryption != null)
					packetType = packetType - inStreamDecryption.getNextKey() & 0xff;
				avail--;
                packetSize = avail;
            }

			fillInStream(packetSize);
            timeOutCounter = 0;

			packetType = -1;
		} catch(java.lang.Exception __ex) {
			System.out.println("BlakeScape Server: Exception!");
			__ex.printStackTrace(); 
			clientConnected = false;
		}
		return true;
	}

	}		// class ClientHandler end

}
