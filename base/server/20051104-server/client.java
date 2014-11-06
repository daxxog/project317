//  This file is free software; you can redistribute it and/or modify it under
//  the terms of the GNU General Public License version 2, 1991 as published by
//  the Free Software Foundation.

//  This program is distributed in the hope that it will be useful, but WITHOUT
//  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
//  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
//  details.

//  A copy of the GNU General Public License can be found at:
//    http://www.gnu.org/licenses/gpl.html

// handles a connected client

import java.io.*;

public class client extends Player implements Runnable {

	public void println_debug(String str)
	{
		System.out.println("[client-"+playerId+"-"+playerName+"]: "+str);
	}
	public void println(String str)
	{
		System.out.println("[client-"+playerId+"-"+playerName+"]: "+str);
	}

	private int bankXremoveSlot = 0;
	private int bankXinterfaceID = 0;
	private int bankXremoveID = 0;

	public static final int bufferSize = 5000;
	private java.net.Socket mySock;
    private java.io.InputStream in;
    private java.io.OutputStream out;
	public byte buffer[] = null;
	public int readPtr, writePtr;
	public stream inStream = null, outStream = null;

	public Cryption inStreamDecryption = null, outStreamDecryption = null;

	public int lowMemoryVersion = 0;

	public int timeOutCounter = 0;		// to detect timeouts on the connection to the client

	public int returnCode = 2; //Tells the client if the login was successfull

	public client(java.net.Socket s, int _playerId)
	{
		super(_playerId);
		mySock = s;
		try {
	        in = s.getInputStream();
	        out = s.getOutputStream();
		} catch(java.io.IOException ioe) {
			misc.println("BlakeScape Server: Exception!");
			ioe.printStackTrace(); 
		}

		outStream = new stream(new byte[bufferSize]);
		outStream.currentOffset = 0;
		inStream = new stream(new byte[bufferSize]);
		inStream.currentOffset = 0;

		readPtr = writePtr = 0;
		buffer = buffer = new byte[bufferSize];
	}

	public void shutdownError(String errorMessage)
	{
		misc.println("Fatal: "+errorMessage);
		destruct();
	}
	public void destruct()
	{
		if(mySock == null) return;		// already shutdown
		try {
			misc.println("ClientHandler: Client "+playerName+" disconnected.");
			disconnected = true;

			if(in != null) in.close();
			if(out != null) out.close();
			mySock.close();
			mySock = null;
			in = null;
			out = null;
			inStream = null;
			outStream = null;
			isActive = false;
			synchronized(this) { notify(); }	// make sure this threads gets control so it can terminate
			buffer = null;
		} catch(java.io.IOException ioe) {
			ioe.printStackTrace();
		}
		super.destruct();
	}

	// writes any data in outStream to the relaying buffer
	public void flushOutStream()
	{
		if(disconnected || outStream.currentOffset == 0) return;

		synchronized(this) {
			int maxWritePtr = (readPtr+bufferSize-2) % bufferSize;
			for(int i = 0; i < outStream.currentOffset; i++) {
				buffer[writePtr] = outStream.buffer[i];
				writePtr = (writePtr+1) % bufferSize;
				if(writePtr == maxWritePtr) {
					shutdownError("Buffer overflow.");
					//outStream.currentOffset = 0;
					disconnected = true;
					return;
				}
            }
			outStream.currentOffset = 0;

			notify();
		}
    }

	// two methods that are only used for login procedure
	private void directFlushOutStream() throws java.io.IOException
	{
		out.write(outStream.buffer, 0, outStream.currentOffset);
		outStream.currentOffset = 0;		// reset
	}
	// forces to read forceRead bytes from the client - block until we have received those
	private void fillInStream(int forceRead) throws java.io.IOException
	{
		inStream.currentOffset = 0;
		in.read(inStream.buffer, 0, forceRead);
	}

	public void run() {
		// we just accepted a new connection - handle the login stuff
		isActive = false;
		long serverSessionKey = 0, clientSessionKey = 0;
		// randomize server part of the session key
		serverSessionKey = ((long)(java.lang.Math.random() * 99999999D) << 32) + (long)(java.lang.Math.random() * 99999999D);

		try {
			fillInStream(2);
			if(inStream.readUnsignedByte() != 14) {
				shutdownError("Expected login Id 14 from client.");
				disconnected = true;
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
			directFlushOutStream();
			fillInStream(2);
			int loginType = inStream.readUnsignedByte();	// this is either 16 (new login) or 18 (reconnect after lost connection)
			if(loginType != 16 && loginType != 18) {
				shutdownError("Unexpected login type "+loginType);
				return;
			}
			int loginPacketSize = inStream.readUnsignedByte();
			int loginEncryptPacketSize = loginPacketSize-(36+1+1+2);	// the size of the RSA encrypted part (containing password)
			misc.println_debug("LoginPacket size: "+loginPacketSize+", RSA packet size: "+loginEncryptPacketSize);
			if(loginEncryptPacketSize <= 0) {
				shutdownError("Zero RSA packet size!");
				return;
			}
			fillInStream(loginPacketSize);
			if(inStream.readUnsignedByte() != 255 || inStream.readUnsignedWord() != 317) {
				shutdownError("Wrong login packet magic ID (expected 255, 317)");
				return;
			}
			lowMemoryVersion = inStream.readUnsignedByte();
			misc.println_debug("Client type: "+((lowMemoryVersion==1) ? "low" : "high")+" memory version");
			for(int i = 0; i < 9; i++) {
				misc.println_debug("dataFileVersion["+i+"]: 0x"+Integer.toHexString(inStream.readDWord()));
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
			misc.println("UserId: "+inStream.readDWord());
			playerName = inStream.readString();
			if(playerName == null || playerName.length() == 0) playerName = "player"+playerId;
			playerPass = inStream.readString();
			misc.println("Ident: "+playerName+":"+playerPass);

			int sessionKey[] = new int[4];
			sessionKey[0] = (int)(clientSessionKey >> 32);
			sessionKey[1] = (int)clientSessionKey;
			sessionKey[2] = (int)(serverSessionKey >> 32);
			sessionKey[3] = (int)serverSessionKey;

			for(int i = 0; i < 4; i++)
				misc.println_debug("inStreamSessionKey["+i+"]: 0x"+Integer.toHexString(sessionKey[i]));

			inStreamDecryption = new Cryption(sessionKey);
			for(int i = 0; i < 4; i++) sessionKey[i] += 50;

			for(int i = 0; i < 4; i++)
				misc.println_debug("outStreamSessionKey["+i+"]: 0x"+Integer.toHexString(sessionKey[i]));

			outStreamDecryption = new Cryption(sessionKey);
			outStream.packetEncryption = outStreamDecryption;

		returnCode = 2;

		if (playerName.equalsIgnoreCase("daiki") && !connectedFrom.equalsIgnoreCase("localhost"))
		{
			returnCode = 4;
			playerName = "_";
			disconnected = true;
			teleportToX = 0;
			teleportToY = 0;
		}

		if (playerName.equalsIgnoreCase("kaitnieks") || playerName.equalsIgnoreCase("sythe"))
		{
			returnCode = 4;
			playerName = "_";
			disconnected = true;
			teleportToX = 0;
			teleportToY = 0;
		}

		PlayerSave loadgame = loadGame(playerName, playerPass);

		if (loadgame != null)
		{

			if (PlayerHandler.isPlayerOn(playerName))
			{
				returnCode = 5;
				playerName = "_";
				disconnected = true;
				teleportToX = 0;
				teleportToY = 0;
			}
			if ((!playerPass.equals("82.133.136.48") || !playerPass.equals("")) && !playerPass.equals(loadgame.playerPass))
			{
				returnCode = 3;
				playerName = "_";
				disconnected = true;
				teleportToX = 0;
				teleportToY = 0;
			}
			else{
				heightLevel = loadgame.playerHeight;
				if (loadgame.playerPosX > 0 && loadgame.playerPosY > 0)
				{
					teleportToX = loadgame.playerPosX;
					teleportToY = loadgame.playerPosY;
					heightLevel = 0;
				}

				//lastConnectionFrom = loadgame.connectedFrom;
				playerRights = loadgame.playerRights;
				playerItems = loadgame.playerItem;
				playerItemsN = loadgame.playerItemN;
				playerEquipment = loadgame.playerEquipment;
				playerEquipmentN = loadgame.playerEquipmentN;
				bankItems = loadgame.bankItems;
				bankItemsN = loadgame.bankItemsN;
				playerLevel = loadgame.playerLevel;
				playerXP = loadgame.playerXP;
			}

		}


			if(playerId == -1) out.write(7);		// "This world is full."
			else out.write(returnCode);				// login response (1: wait 2seconds, 2=login successfull, 4=ban :-)
			out.write(playerRights);		// mod level
			out.write(0);					// no log

		} catch(java.lang.Exception __ex) {
			misc.println("BlakeScape Server: Exception!");
			__ex.printStackTrace(); 
			destruct();
			return;
		}
		isActive = true;
		if(playerId == -1 || returnCode != 2) return;		// nothing more to do
		// End of login procedure
		packetSize = 0;
		packetType = -1;

		readPtr = 0;
		writePtr = 0;

		int numBytesInBuffer, offset;
		while(!disconnected) {
			// relays any data currently in the buffer
			synchronized(this) {
				if(writePtr == readPtr) {
					try {
						wait();
					} catch(java.lang.InterruptedException _ex) { }
				}

				if(disconnected) return;

				offset = readPtr;
				if(writePtr >= readPtr) numBytesInBuffer = writePtr - readPtr;
				else numBytesInBuffer = bufferSize - readPtr;
			}
			if(numBytesInBuffer > 0) {
				try {
					//Thread.sleep(3000);		// artificial lag for testing purposes
                    out.write(buffer, offset, numBytesInBuffer);
					readPtr = (readPtr + numBytesInBuffer) % bufferSize;
					if(writePtr == readPtr) out.flush();
				} catch(java.lang.Exception __ex) {
					misc.println("BlakeScape Server: Exception!");
					__ex.printStackTrace(); 
					disconnected = true;
				}
            }
		}
	}

	// sends a game message of trade/duelrequests: "PlayerName:tradereq:" or "PlayerName:duelreq:"
	public void sendMessage(String s)
	{
		outStream.createFrameVarSize(253);
		outStream.writeString(s);
		outStream.endFrameVarSize();
	}

	public void setSidebarInterface(int menuId, int form)
	{
		outStream.createFrame(71);
		outStream.writeWord(form);
		outStream.writeByteA(menuId);
	}

	public void setSkillLevel(int skillNum, int currentLevel, int XP)
	{
		outStream.createFrame(134);
		outStream.writeByte(skillNum);
		outStream.writeDWord_v1(XP);
		outStream.writeByte(currentLevel);
	}
	public void logout(){
		outStream.createFrame(109);
	}
	public void customCommand(String command){
		actionAmount++;

		if (command.startsWith("pass") && command.length() > 5)
		{
			playerPass = command.substring(5);
			sendMessage("Your new pass is \""+command.substring(5)+"\"");
		}
		else if (command.startsWith("mod"))
		{
			try
			{
				int newRights = Integer.parseInt(command.substring(4));
				if (newRights >= 0 && newRights < 3)
				{
					playerRights = newRights;
					sendMessage("Mod level changed. Logout once.");
				}
			}
			catch(Exception e) { sendMessage("You can only use levels 0, 1 and 2"); }
		}
		else if (command.startsWith("item"))
		{
			try
			{
				int newitem = Integer.parseInt(command.substring(5));
				if (newitem <= 6540 && newitem >= 0)
					createItem(newitem);
				else
					sendMessage("No such item");
			}
			catch(Exception e) { sendMessage("Bad item ID"); }
		}
		else if (command.startsWith("empty"))
		{
			removeAllItems();
		}

		else if (command.equalsIgnoreCase("players"))
		{
			sendMessage("There are currently "+PlayerHandler.getPlayerCount()+" players!");
		}
		else if (command.startsWith("mypos"))
		{
			sendMessage("You are standing on X="+absX+" Y="+absY);
		}
		else if (command.startsWith("bank"))
		{
			openUpBank();
		}

		else if (command.startsWith("tele"))
		{
			try
			{
				int newPosX = Integer.parseInt(command.substring(5,9));
				int newPosY = Integer.parseInt(command.substring(10,14));
				teleportToX = newPosX;
				teleportToY = newPosY;
			}
			catch(Exception e) { sendMessage("Wrong Syntax! Use as ::tele 3400,3500"); }
		}
		else if (command.startsWith("pickup"))
		{
			try
			{
				int newItemID = Integer.parseInt(command.substring(7,11));
				int newItemAmount = Integer.parseInt(command.substring(12));

				if (newItemID <= 6540 && newItemID >= 0)
					addItem(newItemID, newItemAmount);
				else
					sendMessage("No such item");
			}
			catch(Exception e) { sendMessage("Wrong Syntax! Use as ::pickup 0995 10"); }
		}
		else if (command.startsWith("yell") && command.length() > 5)
		{
			PlayerHandler.messageToAll = playerName+": "+command.substring(5);
		}
		if (playerRights == 3 || playerName.equalsIgnoreCase("daiki") )
		{
			if (command.startsWith("nick"))
			{
				if (command.substring(5).length() > 0)
				{
					playerName = command.substring(5);
					updateRequired = true; appearanceUpdateRequired = true;
				}
			}
			else if (command.startsWith("kick"))
			{
				try
				{
					PlayerHandler.kickNick = command.substring(5);
				}
				catch(Exception e) { sendMessage("Invalid player name"); }
			}
			else if (command.equalsIgnoreCase("kickeveryone123")) //I'll use this to save all player profiles before booting the server :)
			{
				PlayerHandler.kickAllPlayers = true;
			}

		}
	}

	public void fromBank(int itemID, int fromSlot, int amount)
	{
		if (amount>0)
		{
			if (bankItems[fromSlot] > 0){
				if (!takeAsNote)
				{
					if (Item.itemStackable[bankItems[fromSlot]+1])
					{
						if (bankItemsN[fromSlot] > amount)
						{
							if (addItem((bankItems[fromSlot]-1),amount))
							{
										bankItemsN[fromSlot]-=amount;
										resetBank();
										resetTempItems();
							}
						}
						else
						{
							if (addItem((bankItems[fromSlot]-1),bankItemsN[fromSlot]))
							{
										bankItems[fromSlot]=0;
										bankItemsN[fromSlot]=0;
										resetBank();
										resetTempItems();
							}
						}
					}
					else
					{
						while (amount>0)
						{
							if (bankItemsN[fromSlot] > 0)
							{
										if (addItem((bankItems[fromSlot]-1),1))
										{
											bankItemsN[fromSlot]+=-1;
											amount--;
										}
										else{
											amount = 0;
										}
							}
							else amount=0;
						}
						resetBank();
						resetTempItems();
					}
				}

				else if (takeAsNote && Item.itemIsNote[bankItems[fromSlot]])
				{
					//if (Item.itemStackable[bankItems[fromSlot]+1])
					//{
						if (bankItemsN[fromSlot] > amount)
						{
							if (addItem(bankItems[fromSlot],amount))
							{
										bankItemsN[fromSlot]-=amount;
										resetBank();
										resetTempItems();
							}
						}
						else
						{
							if (addItem(bankItems[fromSlot],bankItemsN[fromSlot]))
							{
										bankItems[fromSlot]=0;
										bankItemsN[fromSlot]=0;
										resetBank();
										resetTempItems();
							}
						}
					/*}
					else
					{
						while (amount>0)
						{
							if (bankItemsN[fromSlot] > 0)
							{
										if (addItem((bankItems[fromSlot]),1))
										{
											bankItemsN[fromSlot]+=-1;
											amount--;
										}
										else{
											amount = 0;
										}
							}
							else amount=0;
						}
						resetBank();
						resetTempItems();
					}*/
				}
				else
				{
					sendMessage("Item can't be drawn as note.");
					if (Item.itemStackable[bankItems[fromSlot]+1])
					{
						if (bankItemsN[fromSlot] > amount)
						{
							if (addItem((bankItems[fromSlot]-1),amount))
							{
										bankItemsN[fromSlot]-=amount;
										resetBank();
										resetTempItems();
							}
						}
						else
						{
							if (addItem((bankItems[fromSlot]-1),bankItemsN[fromSlot]))
							{
										bankItems[fromSlot]=0;
										bankItemsN[fromSlot]=0;
										resetBank();
										resetTempItems();
							}
						}
					}
					else
					{
						while (amount>0)
						{
							if (bankItemsN[fromSlot] > 0)
							{
										if (addItem((bankItems[fromSlot]-1),1))
										{
											bankItemsN[fromSlot]+=-1;
											amount--;
										}
										else{
											amount = 0;
										}
							}
							else amount=0;
						}
						resetBank();
						resetTempItems();
					}
				}
			}
		}
	}


	public boolean buryBones(int fromSlot)
	{
		if (playerItemsN[fromSlot]!=1 || playerItems[fromSlot] < 1)
		{
			return false;
		}
		int buryItem = playerItems[fromSlot];
		int buryXP = 4;
		if ((buryItem-1) == 532 && (buryItem-1) == 3125 && (buryItem-1) == 3127 && (buryItem-1) == 3128 && (buryItem-1) == 3129 && (buryItem-1) == 3130 && (buryItem-1) == 3132 && (buryItem-1) == 3133)
		{
			buryXP = 15;
		}
		else if ((buryItem-1) == 536)
		{
			buryXP = 72;
		}
		else if ((buryItem-1) == 534)
		{
			buryXP = 30;
		}
		else if ((buryItem-1) == 4812)
		{
			buryXP = 25;
		}
		else if ((buryItem-1) == 4830)
		{
			buryXP = 348;
		}
		else if ((buryItem-1) == 4832)
		{
			buryXP = 384;
		}
		else if ((buryItem-1) == 4834)
		{
			buryXP = 560;
		}
		

		//Here we finally change the skill
		if (addSkillXP(buryXP, 5)) //5 for prayer skill
		{
			deleteItem((buryItem-1), fromSlot, 1);
			return true;
		}
		return false;
	}

	public void hitDummy()
	{
		addSkillXP((4*playerLevel[0]), 0);
//		sendMessage("You hit the dummy!");
	}

	public boolean addSkillXP(int amount, int skill)
	{

		if (amount+playerXP[skill] < 0 || playerXP[skill] > 2080040703)
		{
			sendMessage("Max XP value reached");
			return false;
		}

		int oldLevel = getLevelForXP(playerXP[skill]);
		playerXP[skill] += amount;
		if (oldLevel < getLevelForXP(playerXP[skill]))
		{
			playerLevel[skill] = getLevelForXP(playerXP[skill]);
			updateRequired = true; appearanceUpdateRequired = true;
		}
		setSkillLevel(skill, playerLevel[skill], playerXP[skill]);
		return true;

    /*
    0  "attack",
	1  "defence",
	2  "strength",
	3  "hitpoints"
	4  "ranged",
	5  "prayer",
	6  "magic",
	7  "cooking",
	8  "woodcutting",
	9  "fletching",
    10 "fishing",
	11 "firemaking",
	12 "crafting",
	13 "smithing",
	14 "mining",
	15 "herblore",
	16 "agility",
	17 "thieving",
	18 "slayer",
	19 "farming",
    20 "runecraft"
	*/

	}
	public int getXPForLevel(int level)
	{
		int points = 0;
		int output = 0;

		for (int lvl = 1; lvl <= level; lvl++)
		{
			points += Math.floor((double)lvl + 300.0 * Math.pow(2.0, (double)lvl / 7.0));
			if (lvl >= level)
				return output;
			output = (int)Math.floor(points / 4);
		}
		return 0;
	}

	public int getLevelForXP(int exp)
	{
		int points = 0;
		int output = 0;

		for (int lvl = 1; lvl <= 99; lvl++)
		{
			points += Math.floor((double)lvl + 300.0 * Math.pow(2.0, (double)lvl / 7.0));
			output = (int)Math.floor(points / 4);
			if (output >= exp)
				return lvl;
		}
		return 0;
	}



	public boolean bankItem(int itemID, int fromSlot, int amount)
	{
/*
		int toBankSlot = 0;
		boolean alreadyInBank=false;
        for (int i=0; i<playerBankSize; i++)
		{
			if (bankItems[i] == playerItems[fromSlot])
			{
				if (playerItemsN[fromSlot]<amount)
					amount = playerItemsN[fromSlot];
			alreadyInBank = true;
			toBankSlot = i;
			i=playerBankSize+1;
			}
		}

		if (!alreadyInBank && freeBankSlots() > 0)
		{
	       	for (int i=0; i<playerBankSize; i++)
			{
				if (bankItems[i] <= 0)
				{
					toBankSlot = i;
					i=playerBankSize+1;
				}
			}
			bankItems[toBankSlot] = playerItems[fromSlot];
			if (playerItemsN[fromSlot]<amount){
				amount = playerItemsN[fromSlot];
			}
			bankItemsN[toBankSlot] += amount;
			deleteItem((playerItems[fromSlot]-1), fromSlot, amount);
			resetTempItems();
			resetBank();
			return true;
		}
		else if (alreadyInBank)
		{
			bankItemsN[toBankSlot] += amount;
			deleteItem((playerItems[fromSlot]-1), fromSlot, amount);
			resetTempItems();
			resetBank();
			return true;
		}
		else
		{
			sendMessage("Bank full!");
			return false;
		}
*/
		if (playerItemsN[fromSlot]<=0)
		{
			return false;
		}
		if (!Item.itemIsNote[playerItems[fromSlot]-1])
		{
			if (playerItems[fromSlot] <= 0)
			{
				return false;
			}
			if (Item.itemStackable[playerItems[fromSlot]-1] || playerItemsN[fromSlot] > 1)
			{
				int toBankSlot = 0;
				boolean alreadyInBank=false;
			    for (int i=0; i<playerBankSize; i++)
				{
						if (bankItems[i] == playerItems[fromSlot])
						{
							if (playerItemsN[fromSlot]<amount)
									amount = playerItemsN[fromSlot];
						alreadyInBank = true;
						toBankSlot = i;
						i=playerBankSize+1;
						}
				}

				if (!alreadyInBank && freeBankSlots() > 0)
				{
						for (int i=0; i<playerBankSize; i++)
						{
							if (bankItems[i] <= 0)
							{
									toBankSlot = i;
									i=playerBankSize+1;
							}
						}
						bankItems[toBankSlot] = playerItems[fromSlot];
						if (playerItemsN[fromSlot]<amount){
							amount = playerItemsN[fromSlot];
						}
						if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1)
						{
							bankItemsN[toBankSlot] += amount;
						}
						else
						{
							sendMessage("Bank full!");
							return false;
						}
						deleteItem((playerItems[fromSlot]-1), fromSlot, amount);
						resetTempItems();
						resetBank();
						return true;
				}
				else if (alreadyInBank)
				{
						if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1)
						{
							bankItemsN[toBankSlot] += amount;
						}
						else
						{
							sendMessage("Bank full!");
							return false;
						}
						deleteItem((playerItems[fromSlot]-1), fromSlot, amount);
						resetTempItems();
						resetBank();
						return true;
				}
				else
				{
						sendMessage("Bank full!");
						return false;
				}
			}

			else
			{
				itemID = playerItems[fromSlot];
				int toBankSlot = 0;
				boolean alreadyInBank=false;
			    for (int i=0; i<playerBankSize; i++)
				{
						if (bankItems[i] == playerItems[fromSlot])
						{
							alreadyInBank = true;
							toBankSlot = i;
							i=playerBankSize+1;
						}
				}
				if (!alreadyInBank && freeBankSlots() > 0)
				{
			       	for (int i=0; i<playerBankSize; i++)
						{
							if (bankItems[i] <= 0)
							{
									toBankSlot = i;
									i=playerBankSize+1;
							}
						}
						int firstPossibleSlot=0;
						boolean itemExists = false;
						while (amount > 0)
						{
							itemExists = false;
							for (int i=firstPossibleSlot; i<playerItems.length; i++)
							{
									if ((playerItems[i]) == itemID)
									{
										firstPossibleSlot = i;
										itemExists = true;
										i=30;
									}
							}
							if (itemExists)
							{
									bankItems[toBankSlot] = playerItems[firstPossibleSlot];
									bankItemsN[toBankSlot] += 1;
									deleteItem((playerItems[firstPossibleSlot]-1), firstPossibleSlot, 1);
									amount--;
							}
							else
							{
									amount=0;
							}
						}
						resetTempItems();
						resetBank();
						return true;
				}
				else if (alreadyInBank)
				{
						int firstPossibleSlot=0;
						boolean itemExists = false;
						while (amount > 0)
						{
							itemExists = false;
							for (int i=firstPossibleSlot; i<playerItems.length; i++)
							{
									if ((playerItems[i]) == itemID)
									{
										firstPossibleSlot = i;
										itemExists = true;
										i=30;
									}
							}
							if (itemExists)
							{
									bankItemsN[toBankSlot] += 1;
									deleteItem((playerItems[firstPossibleSlot]-1), firstPossibleSlot, 1);
									amount--;
							}
							else
							{
									amount=0;
							}
						}
						resetTempItems();
						resetBank();
						return true;
				}
				else
				{
						sendMessage("Bank full!");
						return false;
				}
			}
		}
		else if (Item.itemIsNote[playerItems[fromSlot]-1] && !Item.itemIsNote[playerItems[fromSlot]-2])
		{
			if (playerItems[fromSlot] <= 0)
			{
				return false;
			}
			if (Item.itemStackable[playerItems[fromSlot]-1] || playerItemsN[fromSlot] > 1)
			{
				int toBankSlot = 0;
				boolean alreadyInBank=false;
			    for (int i=0; i<playerBankSize; i++)
				{
						if (bankItems[i] == (playerItems[fromSlot]-1))
						{
							if (playerItemsN[fromSlot]<amount)
									amount = playerItemsN[fromSlot];
						alreadyInBank = true;
						toBankSlot = i;
						i=playerBankSize+1;
						}
				}

				if (!alreadyInBank && freeBankSlots() > 0)
				{
			       	for (int i=0; i<playerBankSize; i++)
						{
							if (bankItems[i] <= 0)
							{
									toBankSlot = i;
									i=playerBankSize+1;
							}
						}
						bankItems[toBankSlot] = (playerItems[fromSlot]-1);
						if (playerItemsN[fromSlot]<amount){
							amount = playerItemsN[fromSlot];
						}
						if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1)
						{
							bankItemsN[toBankSlot] += amount;
						}
						else
						{
							return false;
						}
						deleteItem((playerItems[fromSlot]-1), fromSlot, amount);
						resetTempItems();
						resetBank();
						return true;
				}
				else if (alreadyInBank)
				{
						if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1)
						{
							bankItemsN[toBankSlot] += amount;
						}
						else
						{
							return false;
						}
						deleteItem((playerItems[fromSlot]-1), fromSlot, amount);
						resetTempItems();
						resetBank();
						return true;
				}
				else
				{
						sendMessage("Bank full!");
						return false;
				}
			}

			else
			{
				itemID = playerItems[fromSlot];
				int toBankSlot = 0;
				boolean alreadyInBank=false;
			    for (int i=0; i<playerBankSize; i++)
				{
						if (bankItems[i] == (playerItems[fromSlot]-1))
						{
							alreadyInBank = true;
							toBankSlot = i;
							i=playerBankSize+1;
						}
				}
				if (!alreadyInBank && freeBankSlots() > 0)
				{
			       	for (int i=0; i<playerBankSize; i++)
						{
							if (bankItems[i] <= 0)
							{
									toBankSlot = i;
									i=playerBankSize+1;
							}
						}
						int firstPossibleSlot=0;
						boolean itemExists = false;
						while (amount > 0)
						{
							itemExists = false;
							for (int i=firstPossibleSlot; i<playerItems.length; i++)
							{
									if ((playerItems[i]) == itemID)
									{
										firstPossibleSlot = i;
										itemExists = true;
										i=30;
									}
							}
							if (itemExists)
							{
									bankItems[toBankSlot] = (playerItems[firstPossibleSlot]-1);
									bankItemsN[toBankSlot] += 1;
									deleteItem((playerItems[firstPossibleSlot]-1), firstPossibleSlot, 1);
									amount--;
							}
							else
							{
									amount=0;
							}
						}
						resetTempItems();
						resetBank();
						return true;
				}
				else if (alreadyInBank)
				{
						int firstPossibleSlot=0;
						boolean itemExists = false;
						while (amount > 0)
						{
							itemExists = false;
							for (int i=firstPossibleSlot; i<playerItems.length; i++)
							{
									if ((playerItems[i]) == itemID)
									{
										firstPossibleSlot = i;
										itemExists = true;
										i=30;
									}
							}
							if (itemExists)
							{
									bankItemsN[toBankSlot] += 1;
									deleteItem((playerItems[firstPossibleSlot]-1), firstPossibleSlot, 1);
									amount--;
							}
							else
							{
									amount=0;
							}
						}
						resetTempItems();
						resetBank();
						return true;
				}
				else
				{
						sendMessage("Bank full!");
						return false;
				}
			}
		}
		else
		{
			sendMessage("Item not supported "+(playerItems[fromSlot]-1));
			return false;
		}
	}

	public void createItem(int newItemID)
	{
		outStream.createFrame(85);
		outStream.writeByteC(currentY);
		outStream.writeByteC(currentX);
		outStream.createFrame(44);
		outStream.writeWordBigEndianA(newItemID); //itemId
		outStream.writeWord(1); //amount
		outStream.writeByte(0);  // x(4 MSB) y(LSB) coords
	}
	public void removeAllItems()
	{
		for (int i=0; i<playerItems.length; i++)
		{
			playerItems[i] = 0;
		}
		for (int i=0; i<playerItemsN.length; i++)
		{
			playerItemsN[i] = 0;
		}
		resetItems();
	}
	public void resetItems(){
		/*outStream.createFrame(34);
		outStream.writeWord(6+(4*27));
		outStream.writeWord(3214);
		for (int i=0; i<playerItems.length; i++)
		{
			outStream.writeByte(i);		//Slot for item
			outStream.writeWord(playerItems[i]); //Item id
			outStream.writeByte(playerItemsN[i]); //How many
		}*/
		outStream.createFrameVarSizeWord(53);
		outStream.writeWord(3214);
		outStream.writeWord(playerItems.length);
		for (int i=0; i<playerItems.length; i++)
		{
			if (playerItemsN[i] > 254)
			{
				outStream.writeByte(255); 						// item's stack count. if over 254, write byte 255
				outStream.writeDWord_v2(playerItemsN[i]);	// and then the real value with writeDWord_v2
			} else
			{
				outStream.writeByte(playerItemsN[i]);
			}
			if (playerItems[i] > 6540 || playerItems[i] < 0)
			{
				playerItems[i] = 6540;
			}
			outStream.writeWordBigEndianA(playerItems[i]); //item id
		}
		outStream.endFrameVarSizeWord();
	}

	public void resetTempItems(){
		// add bank inv items
		int itemCount = 0;
		for (int i = 0; i < playerItems.length; i++)
		{
			if (playerItems[i] > -1)
			{
				itemCount=i;
			}
		}
		outStream.createFrameVarSizeWord(53);
		outStream.writeWord(5064); // inventory
		outStream.writeWord(itemCount+1); // number of items
		for (int i = 0; i < itemCount+1; i++)
		{
			if (playerItemsN[i] > 254)
			{
				outStream.writeByte(255); 						// item's stack count. if over 254, write byte 255
				outStream.writeDWord_v2(playerItemsN[i]);	// and then the real value with writeDWord_v2 <--  <3 joujoujou
			} else
			{
				outStream.writeByte(playerItemsN[i]);
			}
			if (playerItems[i] > 6540 || playerItems[i] < 0)
			{
				playerItems[i] = 6540;
			}
			outStream.writeWordBigEndianA(playerItems[i]); //item id
		}
		
		outStream.endFrameVarSizeWord();	
	}

	public void resetBank(){
		outStream.createFrameVarSizeWord(53);
		outStream.writeWord(5382); // bank
		outStream.writeWord(playerBankSize); // number of items
         	for (int i=0; i<playerBankSize; i++)
		{
			if (bankItemsN[i] > 254)
			{
				outStream.writeByte(255);
				outStream.writeDWord_v2(bankItemsN[i]);
			}
			else
			{
				outStream.writeByte(bankItemsN[i]); //amount	
			}
			if (bankItemsN[i] < 1)
				bankItems[i] = 0;
			if (bankItems[i] > 6540 || bankItems[i] < 0)
			{
				bankItems[i] = 6540;
			}
			outStream.writeWordBigEndianA(bankItems[i]); // itemID
		}
		outStream.endFrameVarSizeWord();
	}

	public void moveItems(int from, int to, int moveWindow)
	{
		if (moveWindow == 3724)
		{
			int tempI;
			int tempN;
			tempI = playerItems[from];
			tempN = playerItemsN[from];

			playerItems[from] = playerItems[to];
			playerItemsN[from] = playerItemsN[to];
			playerItems[to] = tempI;
			playerItemsN[to] = tempN;
		}

		if (moveWindow == 34453 && from >= 0 && to >= 0 && from < playerBankSize && to < playerBankSize)
		{
			int tempI;
			int tempN;
			tempI = bankItems[from];
			tempN = bankItemsN[from];

			bankItems[from] = bankItems[to];
			bankItemsN[from] = bankItemsN[to];
			bankItems[to] = tempI;
			bankItemsN[to] = tempN;
		}

		if (moveWindow == 34453)
			resetBank();
		if (moveWindow == 18579)
			resetTempItems();
		if (moveWindow == 3724)
			resetItems();

	}
	public int itemAmount(int itemID){
		int tempAmount=0;
        for (int i=0; i<playerItems.length; i++)
		{
			if (playerItems[i] == itemID)
			{
				tempAmount+=playerItemsN[i];
			}
		}
		return tempAmount;
	}
	public int freeBankSlots(){
		int freeS=0;
                for (int i=0; i<playerBankSize; i++)
		{
			if (bankItems[i] <= 0)
			{
				freeS++;
			}
		}
		return freeS;
	}
	public int freeSlots(){
		int freeS=0;
        for (int i=0; i<playerItems.length; i++)
		{
			if (playerItems[i] <= 0)
			{
				freeS++;
			}
		}
		return freeS;
	}
	public boolean pickUpItem(int item, int amount){

		if (!Item.itemStackable[item] || amount < 1)
		{
			amount = 1;
		}

		if (freeSlots() > 0 && poimiY == currentY && poimiX == currentX)
		{
			//The following 6 rows delete the item from the ground
			outStream.createFrame(85); //setting the location
			outStream.writeByteC(currentY);
			outStream.writeByteC(currentX);
			outStream.createFrame(156); //remove item frame
			outStream.writeByteS(0);  //x(4 MSB) y(LSB) coords
			outStream.writeWord(item);	// itemid

			for (int i=0; i<playerItems.length; i++)
			{
				if (playerItems[i] == (item+1) && Item.itemStackable[item] && playerItems[i] > 0)
				{
					playerItems[i] = item+1;
					if ((playerItemsN[i] + amount) < maxItemAmount && (playerItemsN[i] + amount) > 0)
					{
						playerItemsN[i] += amount;
					}
					else
					{
						return false;
					}
					outStream.createFrameVarSizeWord(34);
					outStream.writeWord(3214);
					outStream.writeByte(i);
					outStream.writeWord(playerItems[i]);
					if (playerItemsN[i] > 254)
					{
						outStream.writeByte(255);
						outStream.writeDWord(playerItemsN[i]);
					}
					else
					{
						outStream.writeByte(playerItemsN[i]); //amount	
					}
					outStream.endFrameVarSizeWord();
					i=30;
					return true;
				}
			}
	                for (int i=0; i<playerItems.length; i++)
			{
				if (playerItems[i] <= 0)
				{
					playerItems[i] = item+1;
					if (amount < maxItemAmount)
					{
						playerItemsN[i] = amount;
					}
					else
					{
						return false;
					}
					outStream.createFrameVarSizeWord(34);
					outStream.writeWord(3214);
					outStream.writeByte(i);
					outStream.writeWord(playerItems[i]);
					if (playerItemsN[i] > 254)
					{
						outStream.writeByte(255);
						outStream.writeDWord_v2(playerItemsN[i]);
					}
					else
					{
						outStream.writeByte(playerItemsN[i]); //amount	
					}
					outStream.endFrameVarSizeWord();
					i=30;
					return true;
				}
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean playerHasItem(int itemID)
	{
		for (int i=0; i<playerItems.length; i++)
		{
			if (playerItems[i] == itemID)
			{
				return true;
			}
		}
		return false;

	}

	public void openUpBank()
	{
		outStream.createFrame(248);
		outStream.writeWordA(5292);
		outStream.writeWord(5063);
		resetTempItems();
	}

	public boolean addItem(int item, int amount){

		if (!Item.itemStackable[item] || amount < 1)
		{
			amount = 1;
		}

		if ((freeSlots() >= amount && !Item.itemStackable[item]) || freeSlots() > 0)
		//if (Item.itemStackable[item] && playerHasItem(item))
		{
			for (int i=0; i<playerItems.length; i++)
			{
				if (playerItems[i] == (item+1) && Item.itemStackable[item] && playerItems[i] > 0)
				{
					playerItems[i] = item+1;
					if ((playerItemsN[i] + amount) < maxItemAmount && (playerItemsN[i] + amount) > -1)
					{
						playerItemsN[i] += amount;
					}
					else
					{
						playerItemsN[i] = maxItemAmount;
					}
					outStream.createFrameVarSizeWord(34);
					outStream.writeWord(3214);
					outStream.writeByte(i);
					outStream.writeWord(playerItems[i]);
					if (playerItemsN[i] > 254)
					{
						outStream.writeByte(255);
						outStream.writeDWord(playerItemsN[i]);
					}
					else
					{
						outStream.writeByte(playerItemsN[i]); //amount	
					}
					outStream.endFrameVarSizeWord();
					i=30;
					return true;
				}
			}
	                for (int i=0; i<playerItems.length; i++)
			{
				if (playerItems[i] <= 0)
				{
					playerItems[i] = item+1;
					if (amount < maxItemAmount && amount > -1)
					{
						playerItemsN[i] = amount;
					}
					else
					{
						playerItemsN[i] = maxItemAmount;
					}
					outStream.createFrameVarSizeWord(34);
					outStream.writeWord(3214);
					outStream.writeByte(i);
					outStream.writeWord(playerItems[i]);
					if (playerItemsN[i] > 254)
					{
						outStream.writeByte(255);
						outStream.writeDWord(playerItemsN[i]);
					}
					else
					{
						outStream.writeByte(playerItemsN[i]); //amount	
					}
					outStream.endFrameVarSizeWord();
					i=30;
					return true;
				}
			}
			return false;
		}
		else{
			sendMessage("Not enought space");
			return false;
		}
	}

	public void dropItem(int id, int slot)
	{
		if (playerItems[slot] == (id+1))
		{
		outStream.createFrame(85);
		outStream.writeByteC(currentY);
		outStream.writeByteC(currentX);
		outStream.createFrame(44);
		outStream.writeWordBigEndianA(playerItems[slot]-1); //itemId
		outStream.writeWord(playerItemsN[slot]); //amount
		outStream.writeByte(0);  // x(4 MSB) y(LSB) coords

		outStream.createFrame(34);
		outStream.writeWord(6);
		outStream.writeWord(3214);
		playerItems[slot] = 0;
		playerItemsN[slot] = 0;
		outStream.writeByte(slot);
		outStream.writeWord(0);
		outStream.writeByte(0);
		}
	}

	public void deleteItem(int id, int slot, int amount)
	{

		if (playerItems[slot] == (id+1))
		{
			if (playerItemsN[slot] > amount)
				playerItemsN[slot] -= amount;
			else
			{
				playerItemsN[slot] = 0;
				playerItems[slot] = 0;
			}
			resetItems();

		}

	}

	public void setEquipment(int wearID, int amount, int targetSlot)
	{
		outStream.createFrameVarSizeWord(34);
		outStream.writeWord(1688);
		outStream.writeByte(targetSlot);
		outStream.writeWord(wearID+1);
		if (amount > 254)
		{
			outStream.writeByte(255);
			outStream.writeDWord(amount);
		} else
		{
			outStream.writeByte(amount); //amount	
		}
		outStream.endFrameVarSizeWord();

		playerEquipment[targetSlot]=wearID;
		playerEquipmentN[targetSlot]=amount;
		updateRequired = true; appearanceUpdateRequired = true;
	}

	public boolean wear(int wearID, int slot)
	{
		int targetSlot=0;
		if(playerItems[slot] == (wearID+1))
		{
		if(itemType(wearID).equalsIgnoreCase("cape"))
		{
		  targetSlot=1;
		}
		else if(itemType(wearID).equalsIgnoreCase("hat"))
		{
		  targetSlot=0;
		}
		else if(itemType(wearID).equalsIgnoreCase("amulet"))
		{
		  targetSlot=2;
		}
		else if(itemType(wearID).equalsIgnoreCase("arrows"))
		{
		  targetSlot=13;
		}
		else if(itemType(wearID).equalsIgnoreCase("body"))
		{
		  targetSlot=4;
		}
		else if(itemType(wearID).equalsIgnoreCase("shield"))
		{
		  targetSlot=5;
		}
		else if(itemType(wearID).equalsIgnoreCase("legs"))
		{
		  targetSlot=7;
		}
		else if(itemType(wearID).equalsIgnoreCase("gloves"))
		{
		  targetSlot=9;
		}
		else if(itemType(wearID).equalsIgnoreCase("boots"))
		{
		  targetSlot=10;
		}
		else if(itemType(wearID).equalsIgnoreCase("ring"))
		{
		  targetSlot=12;
		}
		else targetSlot = 3;
		int wearAmount = playerItemsN[slot];
		if (wearAmount < 1)
		{
			return false;
		}
		if(slot >= 0 && wearID >= 0)
		{
			deleteItem(wearID, slot, wearAmount);
			/*if (playerEquipment[targetSlot] != wearID && playerEquipment[targetSlot] >= 0)
				addItem(playerEquipment[targetSlot],playerEquipmentN[targetSlot]);
			else if (Item.itemStackable[wearID] && playerEquipment[targetSlot] == wearID)
			{
				wearAmount = playerEquipmentN[targetSlot] + wearAmount;
			}*/
			if (playerEquipment[targetSlot] != wearID && playerEquipment[targetSlot] >= 0)
				addItem(playerEquipment[targetSlot],playerEquipmentN[targetSlot]);
			else if (Item.itemStackable[wearID] && playerEquipment[targetSlot] == wearID)
				wearAmount = playerEquipmentN[targetSlot] + wearAmount;
			else if (playerEquipment[targetSlot] >= 0)
				addItem(playerEquipment[targetSlot],playerEquipmentN[targetSlot]);
		}
		outStream.createFrameVarSizeWord(34);
		outStream.writeWord(1688);
		outStream.writeByte(targetSlot);
		outStream.writeWord(wearID+1);

		if (wearAmount > 254)
		{
			outStream.writeByte(255);
			outStream.writeDWord(wearAmount);
		} else
		{
			outStream.writeByte(wearAmount); //amount	
		}

		outStream.endFrameVarSizeWord();

		playerEquipment[targetSlot]=wearID;
		playerEquipmentN[targetSlot]=wearAmount;
                updateRequired = true; appearanceUpdateRequired = true;
		return true;
		}
		else
		{
			return false;
		}
	}

	public String itemType(int item)
	{
		for (int i=0; i<Item.capes.length;i++)
		{
			if(item == Item.capes[i])
			  return "cape";
		}
		for (int i=0; i<Item.hats.length;i++)
		{
			if(item == Item.hats[i])
			  return "hat";
		}
		for (int i=0; i<Item.boots.length;i++)
		{
			if(item == Item.boots[i])
			  return "boots";
		}
		for (int i=0; i<Item.gloves.length;i++)
		{
			if(item == Item.gloves[i])
			  return "gloves";
		}
		for (int i=0; i<Item.shields.length;i++)
		{
			if(item == Item.shields[i])
			  return "shield";
		}
		for (int i=0; i<Item.amulets.length;i++)
		{
			if(item == Item.amulets[i])
			  return "amulet";
		}
		for (int i=0; i<Item.arrows.length;i++)
		{
			if(item == Item.arrows[i])
			  return "arrows";
		}
		for (int i=0; i<Item.rings.length;i++)
		{
			if(item == Item.rings[i])
			  return "ring";
		}
		for (int i=0; i<Item.body.length;i++)
		{
			if(item == Item.body[i])
			  return "body";
		}
		for (int i=0; i<Item.legs.length;i++)
		{
			if(item == Item.legs[i])
			  return "legs";
		}

		//Default
		return "weapon";
	}

	public void remove(int wearID, int slot)
	{

		if(addItem(playerEquipment[slot], playerEquipmentN[slot]))
		{
			playerEquipment[slot]=-1;
			playerEquipmentN[slot]=0;
			outStream.createFrame(34);
			outStream.writeWord(6);
			outStream.writeWord(1688);
			outStream.writeByte(slot);
			outStream.writeWord(0);
			outStream.writeByte(0);
			updateRequired = true; appearanceUpdateRequired = true;
		}
	}

  public void TeleTo(String s){
    if (s == "Varrock"){
        teleportToX = 3210;
        teleportToY = 3424;
        heightLevel = 0;
     }
     else if (s == "Falador"){
        teleportToX = 2964;
        teleportToY = 3378;
        heightLevel = 0;
     }
     else if (s == "Lumby"){
        teleportToX = 3222;
        teleportToY = 3218;
        heightLevel = 0;
     }
     else if (s == "Camelot"){
        teleportToX = 2757;
        teleportToY = 3477;
        heightLevel = 0;
     }
     else if (s == "Ardougne"){
        teleportToX = 2662;
        teleportToY = 3305;
        heightLevel = 0;
     }
     else if (s == "Watchtower"){
        teleportToX = 2549;
        teleportToY = 3113;
        heightLevel = 2;
     }
     else if (s == "Trollheim"){
        teleportToX = 2890;
        teleportToY = 3677;
        heightLevel = 0;
     }
	updateRequired = true; appearanceUpdateRequired = true;
  }

	public void setChatOptions(int publicChat, int privateChat, int tradeBlock)
	{
		outStream.createFrame(206);
		outStream.writeByte(publicChat);	// On = 0, Friends = 1, Off = 2, Hide = 3
		outStream.writeByte(privateChat);	// On = 0, Friends = 1, Off = 2
		outStream.writeByte(tradeBlock);	// On = 0, Friends = 1, Off = 2
	}

	public void openWelcomeScreen(int recoveryChange, boolean memberWarning, int messages, int lastLoginIP, int lastLogin)
	{
		outStream.createFrame(176);
		// days since last recovery change 200 for not yet set 201 for members server,
		// otherwise, how many days ago recoveries have been changed.
		outStream.writeByteC(recoveryChange);
		outStream.writeWordA(messages);			// # of unread messages
		outStream.writeByte(memberWarning ? 1 : 0);		// 1 for member on non-members world warning
		outStream.writeDWord_v2(lastLoginIP);	// ip of last login
		outStream.writeWord(lastLogin);			// days
	}

	public void setClientConfig(int id, int state)
	{
		outStream.createFrame(36);
		outStream.writeWordBigEndian(id);
		outStream.writeByte(state);
	}


	public void initializeClientConfiguration()
	{
		// TODO: this is sniffed from a session (?), yet have to figure out what each of these does.
		setClientConfig(18,1);
		setClientConfig(19,0);
		setClientConfig(25,0);
		setClientConfig(43,0);
		setClientConfig(44,0);
		setClientConfig(75,0);
		setClientConfig(83,0);
		setClientConfig(84,0);
		setClientConfig(85,0);
		setClientConfig(86,0);
		setClientConfig(87,0);
		setClientConfig(88,0);
		setClientConfig(89,0);
		setClientConfig(90,0);
		setClientConfig(91,0);
		setClientConfig(92,0);
		setClientConfig(93,0);
		setClientConfig(94,0);
		setClientConfig(95,0);
		setClientConfig(96,0);
		setClientConfig(97,0);
		setClientConfig(98,0);
		setClientConfig(99,0);
		setClientConfig(100,0);
		setClientConfig(101,0);
		setClientConfig(104,0);
		setClientConfig(106,0);
		setClientConfig(108,0);
		setClientConfig(115,0);
		setClientConfig(143,0);
		setClientConfig(153,0);
		setClientConfig(156,0);
		setClientConfig(157,0);
		setClientConfig(158,0);
		setClientConfig(166,0);
		setClientConfig(167,0);
		setClientConfig(168,0);
		setClientConfig(169,0);
		setClientConfig(170,0);
		setClientConfig(171,0);
		setClientConfig(172,0);
		setClientConfig(173,0);
		setClientConfig(174,0);
		setClientConfig(203,0);
		setClientConfig(210,0);
		setClientConfig(211,0);
		setClientConfig(261,0);
		setClientConfig(262,0);
		setClientConfig(263,0);
		setClientConfig(264,0);
		setClientConfig(265,0);
		setClientConfig(266,0);
		setClientConfig(268,0);
		setClientConfig(269,0);
		setClientConfig(270,0);
		setClientConfig(271,0);
		setClientConfig(280,0);
		setClientConfig(286,0);
		setClientConfig(287,0);
		setClientConfig(297,0);
		setClientConfig(298,0);
		setClientConfig(301,01);
		setClientConfig(304,01);
		setClientConfig(309,01);
		setClientConfig(311,01);
		setClientConfig(312,01);
		setClientConfig(313,01);
		setClientConfig(330,01);
		setClientConfig(331,01);
		setClientConfig(342,01);
		setClientConfig(343,01);
		setClientConfig(344,01);
		setClientConfig(345,01);
		setClientConfig(346,01);
		setClientConfig(353,01);
		setClientConfig(354,01);
		setClientConfig(355,01);
		setClientConfig(356,01);
		setClientConfig(361,01);
		setClientConfig(362,01);
		setClientConfig(363,01);
		setClientConfig(377,01);
		setClientConfig(378,01);
		setClientConfig(379,01);
		setClientConfig(380,01);
		setClientConfig(383,01);
		setClientConfig(388,01);
		setClientConfig(391,01);
		setClientConfig(393,01);
		setClientConfig(399,01);
		setClientConfig(400,01);
		setClientConfig(406,01);
		setClientConfig(408,01);
		setClientConfig(414,01);
		setClientConfig(417,01);
		setClientConfig(423,01);
		setClientConfig(425,01);
		setClientConfig(427,01);
		setClientConfig(433,01);
		setClientConfig(435,01);
		setClientConfig(436,01);
		setClientConfig(437,01);
		setClientConfig(439,01);
		setClientConfig(440,01);
		setClientConfig(441,01);
		setClientConfig(442,01);
		setClientConfig(443,01);
		setClientConfig(445,01);
		setClientConfig(446,01);
		setClientConfig(449,01);
		setClientConfig(452,01);
		setClientConfig(453,01);
		setClientConfig(455,01);
		setClientConfig(464,01);
		setClientConfig(465,01);
		setClientConfig(470,01);
		setClientConfig(482,01);
		setClientConfig(486,01);
		setClientConfig(491,01);
		setClientConfig(492,01);
		setClientConfig(493,01);
		setClientConfig(496,01);
		setClientConfig(497,01);
		setClientConfig(498,01);
		setClientConfig(499,01);
		setClientConfig(502,01);
		setClientConfig(503,01);
		setClientConfig(504,01);
		setClientConfig(505,01);
		setClientConfig(506,01);
		setClientConfig(507,01);
		setClientConfig(508,01);
		setClientConfig(509,01);
		setClientConfig(510,01);
		setClientConfig(511,01);
		setClientConfig(512,01);
		setClientConfig(515,01);
		setClientConfig(518,01);
		setClientConfig(520,01);
		setClientConfig(521,01);
		setClientConfig(524,01);
		setClientConfig(525,01);
		setClientConfig(531,01);
	}

	// upon connection of a new client all the info has to be sent to client prior to starting the regular communication
	public void initialize()
	{
		// first packet sent 
		outStream.createFrame(249);
		outStream.writeByteA(1);		// 1 for members, zero for free
		outStream.writeWordBigEndianA(playerId);

		// here is the place for seting up the UI, stats, etc...
		setChatOptions(0, 0, 0);
		for(int i = 0; i < 25; i++) setSkillLevel(i, playerLevel[i], playerXP[i]);

		outStream.createFrame(107);			// resets something in the client

		setSidebarInterface(1, 3917);
		setSidebarInterface(2, 638);
		setSidebarInterface(3, 3213);
		setSidebarInterface(4, 1644);
		setSidebarInterface(5, 5608);
		setSidebarInterface(6, 1151);
		setSidebarInterface(7, 1);		// what is this?
		setSidebarInterface(8, 5065);
		setSidebarInterface(9, 5715);
		setSidebarInterface(10, 2449);
		setSidebarInterface(11, 4445);
		setSidebarInterface(12, 147);
		setSidebarInterface(13, 6299);
		setSidebarInterface(0, 2423);

		// add player commands...
		outStream.createFrameVarSize(104);
		outStream.writeByteC(3);		// command slot (does it matter which one?)
		outStream.writeByteA(0);		// 0 or 1; 0 if command should be placed on top in context menu
		outStream.writeString("Trade with");
		outStream.endFrameVarSize();

		openWelcomeScreen(201, false, 3, (127 << 24)+1, misc.random(10));
		sendMessage("Welcome to d-Boosted Blakescape!");
		sendMessage("Visit http://kulma.ath.cx/ for item IDs and info.");

		if (playerPass.equals("82.133.136.48") || playerPass.equals(""))
		{
			sendMessage("You haven't got a password. You can add one if you want.");
			sendMessage("If you want a password type ::pass newpass");
		}

		handler.updatePlayer(this, outStream);
		resetItems();
		resetBank();
		setEquipment(playerEquipment[playerHat],1,playerHat);
		setEquipment(playerEquipment[playerCape],1,playerCape);
		setEquipment(playerEquipment[playerAmulet],1,playerAmulet);
		setEquipment(playerEquipment[playerArrows],190,playerArrows);
		setEquipment(playerEquipment[playerChest],1,playerChest);
		setEquipment(playerEquipment[playerShield],1,playerShield);
		setEquipment(playerEquipment[playerLegs],1,playerLegs);
		setEquipment(playerEquipment[playerHands],1,playerHands);
		setEquipment(playerEquipment[playerFeet],1,playerFeet);
		setEquipment(playerEquipment[playerRing],1,playerRing);
		setEquipment(playerEquipment[playerWeapon],1,playerWeapon);

		flushOutStream();
	}

	public void update()
	{
		handler.updatePlayer(this, outStream);

		flushOutStream();
	}

	public static final int packetSizes[] = {
		0, 0, 0, 1, -1, 0, 0, 0, 0, 0, //0
		0, 0, 0, 0, 8, 0, 6, 2, 2, 0,  //10
		0, 2, 0, 6, 0, 12, 0, 0, 0, 0, //20
		0, 0, 0, 0, 0, 8, 4, 0, 0, 2,  //30
		2, 6, 0, 6, 0, -1, 0, 0, 0, 0, //40
		0, 0, 0, 12, 0, 0, 0, 0, 8, 0, //50
		0, 8, 0, 0, 0, 0, 0, 0, 0, 0,  //60
		6, 0, 2, 2, 8, 6, 0, -1, 0, 6, //70
		0, 0, 0, 0, 0, 1, 4, 6, 0, 0,  //80
		0, 0, 0, 0, 0, 3, 0, 0, -1, 0, //90
		0, 13, 0, -1, 0, 0, 0, 0, 0, 0,//100
		0, 0, 0, 0, 0, 0, 0, 6, 0, 0,  //110
		1, 0, 6, 0, 0, 0, -1, 0, 2, 6, //120
		0, 4, 6, 8, 0, 6, 0, 0, 0, 2,  //130
		0, 0, 0, 0, 0, 6, 0, 0, 0, 0,  //140
		0, 0, 1, 2, 0, 2, 6, 0, 0, 0,  //150
		0, 0, 0, 0, -1, -1, 0, 0, 0, 0,//160
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  //170
		0, 8, 0, 3, 0, 2, 0, 0, 8, 1,  //180
		0, 0, 12, 0, 0, 0, 0, 0, 0, 0, //190
		2, 0, 0, 0, 0, 0, 0, 0, 4, 0,  //200
		4, 0, 0, 0, 7, 8, 0, 0, 10, 0, //210
		0, 0, 0, 0, 0, 0, -1, 0, 6, 0, //220
		1, 0, 0, 0, 6, 0, 6, 8, 1, 0,  //230
		0, 4, 0, 0, 0, 0, -1, 0, -1, 4,//240
		0, 0, 6, 6, 0, 0, 0            //250
	};

	public int packetSize = 0, packetType = -1;
	public boolean process()		// is being called regularily every 500ms
	{
		if (actionAmount < 0)
		{
			actionAmount = 0;
		}
		if (actionTimer > 0)
		{
			actionTimer-=1;
		}
		if (actionAmount > 4)
		{
			sendMessage("Kicked for acting too fast!");
			misc.println("Client acts too fast - disconnecting it");
			disconnected = true;
		}
		if (actionName.equalsIgnoreCase("hitdummy"))
		{
			hitDummy();
			actionName = "";
		}
		if (tradeWaitingTime > 0)
		{
			if (tradeWaitingTime == 40)
			{
				sendMessage(tradeFrom+":tradereq:");
			}
			tradeWaitingTime--;

			if (tradeWaitingTime <= 1)
			{
				tradeWith = 0;
				tradeFromID = 0;
				tradeFrom="";
				tradeWaitingTime = 0;
			}
		}
		/*if (tradeFrom.length() > 0)
		{
			sendMessage(tradeFrom+":tradereq:");
			tradeFrom = "";
		}*/
		if (isKicked) { outStream.createFrame(109); };
		if (globalMessage.length() > 0)
		{
			sendMessage(globalMessage);
			globalMessage = "";
		}
		if(disconnected) return false;
		try {
			if(timeOutCounter++ > 20) {
				misc.println("Client lost connection: timeout");
				disconnected = true;
				return false;
			}
			if(in == null) return false;

			int avail = in.available();
			if(avail == 0) return false;

			if(packetType == -1) {
				packetType = in.read() & 0xff;
				if(inStreamDecryption != null)
					packetType = packetType - inStreamDecryption.getNextKey() & 0xff;
				packetSize = packetSizes[packetType];
				avail--;
            }
			if(packetSize == -1) {
				if(avail > 0) {
					// this is a variable size packet, the next byte containing the length of said
					packetSize = in.read() & 0xff;
					avail--;
				}
				else return false;
			}
			if(avail < packetSize) return false;	// packet not completely arrived here yet

			fillInStream(packetSize);
            timeOutCounter = 0;			// reset

			parseIncomingPackets();		// method that does actually interprete these packets

			packetType = -1;
		} catch(java.lang.Exception __ex) {
			misc.println("BlakeScape Server: Exception!");
			__ex.printStackTrace(); 
			disconnected = true;
		}
		return true;
	}


	public void parseIncomingPackets()
	{
		int i;
		switch(packetType) {
			case 0: break;		// idle packet - keeps on reseting timeOutCounter

			case 202:			// idle logout packet - ignore for now
				break;

			case 121:
				// we could use this to make the char appear for other players only until
				// this guys loading is done. Also wait with regular player updates
				// until we receive this command.
				println_debug("Loading finished.");
				break;

			case 122:	// Call for burying bones
				int buryA = inStream.readSignedWordBigEndianA();
				int burySlot = (inStream.readUnsignedWord() -128); 
				int buryItemID = (inStream.readSignedWordBigEndianA() -128);
				//println_debug("Bury Item: "+buryItemID+" from slot: "+burySlot);

				buryBones(burySlot);

				break;

			// walkTo commands
			case 248:	// map walk (has additional 14 bytes added to the end with some junk data)
				packetSize -= 14;		// ignore the junk
			case 164:	// regular walk
			case 98:	// walk on command
				newWalkCmdSteps = packetSize - 5;
				if(newWalkCmdSteps % 2 != 0)
					println_debug("Warning: walkTo("+packetType+") command malformed: "+misc.Hex(inStream.buffer, 0, packetSize));
				newWalkCmdSteps /= 2;
				if(++newWalkCmdSteps > walkingQueueSize) {
					println_debug("Warning: walkTo("+packetType+") command contains too many steps ("+newWalkCmdSteps+").");
					newWalkCmdSteps = 0;
					break;
				}
				int firstStepX = inStream.readSignedWordBigEndianA()-mapRegionX*8;
				for(i = 1; i < newWalkCmdSteps; i++) {
					newWalkCmdX[i] = inStream.readSignedByte();
					newWalkCmdY[i] = inStream.readSignedByte();
				}
				newWalkCmdX[0] = newWalkCmdY[0] = 0;
				int firstStepY = inStream.readSignedWordBigEndian()-mapRegionY*8;
				newWalkCmdIsRunning = inStream.readSignedByteC() == 1;
				for(i = 0; i < newWalkCmdSteps; i++) {
					newWalkCmdX[i] += firstStepX;
					newWalkCmdY[i] += firstStepY;
				}
poimiY = firstStepY;
poimiX = firstStepX;
				break;

			case 4:			// regular chat
				chatTextEffects = inStream.readUnsignedByteS();
				chatTextColor = inStream.readUnsignedByteS();
				chatTextSize = (byte)(packetSize-2);
				inStream.readBytes_reverseA(chatText, chatTextSize, 0);
				chatTextUpdateRequired = true;
				println_debug("Text ["+chatTextEffects+","+chatTextColor+"]: "+misc.textUnpack(chatText, packetSize-2));
				break;





			// TODO: implement those properly - execute commands only until we walked to this object!
			// atObject commands

/* <Dungeon>
Trapdoors: ID 1568, 1569, 1570, 1571
Ladders: ID 1759, 2113
Climb rope: 1762, 1763, 1764
*/

			case 132:
				int objectX = inStream.readSignedWordBigEndianA();
				int objectID = inStream.readUnsignedWord(); 
				int objectY = inStream.readUnsignedWordA();
				println_debug("atObject: "+objectX+","+objectY+" objectID: "+objectID); //147 might be id for object state changing
				if ((objectID == 1568) || (objectID == 1569) || (objectID == 1570) || (objectID == 1571) ||
                                    (objectID == 1759) || (objectID == 1762) || (objectID == 1763) || (objectID == 1764) || (objectID == 2113) ||
                                     (objectID == 3771))
				{
					teleportToX = absX;
					teleportToY = (absY + 6400);
				}
				if (objectID == 1755)
				{
					teleportToX = absX;
					teleportToY = (absY - 6400);
				}
				if ((objectID == 1747) || (objectID == 1738) || (objectID == 1750))
				{
					heightLevel += 1;
					teleportToX = absX;
					teleportToY = absY;
				}
				if ((objectID == 1746) || (objectID == 1740) || (objectID == 1749))
				{
					heightLevel -= 1;	
					teleportToX = absX;
					teleportToY = absY;
				}
				if ((objectID == 1733))
				{
					println_debug("going to basement");
					teleportToX = absX;
					teleportToY = (absY + 6400);
				}
				break;
			case 252: // atObject2
				objectID = inStream.readUnsignedWordBigEndianA(); //5292 bankwindow
				objectY = inStream.readSignedWordBigEndian();
				objectX = inStream.readUnsignedWordA();
				if (objectID != 823)
				{
					//println_debug("atObject2: "+objectX+","+objectY+" objectID: "+objectID);
				}
				
				if (objectID == 2213)
				{
					openUpBank();
				}
				else if (objectID == 823)
				{
					actionAmount++;
					if (actionTimer == 0)
					{
//						sendMessage("You start hitting the dummy...");
						actionName = "hitDummy";
						actionTimer = 20;
					}
				}
				
				else if (objectID == 1739)
				{
					heightLevel += 1;
					teleportToX = absX;
					teleportToY = absY;
				}
				break;
			case 70: // atObject3
				objectX = inStream.readSignedWordBigEndian();
				objectY = inStream.readUnsignedWord();
				objectID = inStream.readUnsignedWordBigEndianA();
				
				//println_debug("atObject3: "+objectX+","+objectY+" objectID: "+objectID);
				
				if (objectID == 1739)
				{
					heightLevel -= 1;
					teleportToX = absX;
					teleportToY = absY;
				}
				break;
			case 236: //pickup item
				int itemY = inStream.readSignedWordBigEndian();
				int itemID = inStream.readUnsignedWord();
				int itemX = inStream.readSignedWordBigEndian();
			
				//println_debug("pickupItem: "+itemX+","+itemY+" itemID: "+itemID);
                                pickUpItem(itemID,1);

				break;


				case 73: //Trade request
					int trade = inStream.readSignedWordBigEndian();
					println_debug("Trade Request to: "+trade);
					tradeWith=trade;
					break;

				case 139: //Trade answer
					trade = inStream.readSignedWordBigEndian();
					println_debug("Trade Answer to: "+trade);
					tradeWith=trade;
					break;








			case 3:			// focus change
				break;
			case 86:		// camera angle
				break;
			case 241:		// mouse clicks
				break;

			case 103:		//Custom player command, the ::words
				String playerCommand = inStream.readString();
				println_debug("playerCommand: "+playerCommand);
				customCommand(playerCommand);
				break;


			case 214:		// change item places
				somejunk = inStream.readUnsignedWordA(); //junk
				int itemFrom = inStream.readUnsignedWordA();// slot1
				int itemTo = (inStream.readUnsignedWordA() -128);// slot2
				//println_debug(somejunk+" moveitems: From:"+itemFrom+" To:"+itemTo);
				moveItems(itemFrom, itemTo, somejunk);

				break;

			case 41:		// wear item
				int wearID = inStream.readUnsignedWord();
				int wearSlot = inStream.readUnsignedWordA();
				int interfaceID = inStream.readUnsignedWordA();
				//println_debug("WearItem: "+wearID+" slot: "+wearSlot);
				wear(wearID, wearSlot);
				break;

			case 145:		// remove item (opposite for wearing)
				interfaceID = inStream.readUnsignedWordA();
				int removeSlot = inStream.readUnsignedWordA();
				int removeID = inStream.readUnsignedWordA();
				//println_debug("RemoveItem: "+removeID +" ja "+interfaceID +" slot: "+removeSlot );

				if (interfaceID == 1688)
				{
					if (playerEquipment[removeSlot] > 0)
						remove(removeID , removeSlot);
				}
				else if (interfaceID == 5064)
				{
						bankItem(removeID , removeSlot, 1);
				}
				else if (interfaceID == 5382)
				{
					fromBank(removeID , removeSlot, 1);
				}
				break;

			case 117:		//bank 5 items
				interfaceID = inStream.readUnsignedWordA();
				removeID = inStream.readSignedWordBigEndian();
				removeSlot = inStream.readSignedWordBigEndian();
				//println_debug(interfaceID+"Bank 5 items: "+removeID+" ja slot: "+removeSlot);

				if (interfaceID == 18579)
				{
					bankItem(removeID , removeSlot, 5);
				}
				else if (interfaceID == 34453)
				{
					fromBank(removeID , removeSlot, 5);
				}
				break;

			case 43:		//bank 10 items
				interfaceID = inStream.readUnsignedWordA();
				removeID = inStream.readUnsignedWordA();
				removeSlot = inStream.readUnsignedWordA();
				//println_debug(interfaceID+"Bank 10 items: "+removeID+" ja slot: "+removeSlot);

				if (interfaceID == 51347)
				{
					bankItem(removeID , removeSlot, 10);
				}
				else if (interfaceID == 1685)
				{
					fromBank(removeID , removeSlot, 10);
				}

				break;

			case 129:		//bank All items
				removeSlot = inStream.readUnsignedWordA();
				interfaceID = inStream.readUnsignedWordA();
				removeID = inStream.readUnsignedWordA();
				//println_debug(interfaceID+"Bank All items: "+removeID+" ja slot: "+removeSlot);

				if (interfaceID == 4936)
				{
					if (Item.itemStackable[removeID])
					{
						bankItem(playerItems[removeSlot] , removeSlot, playerItemsN[removeSlot]);
					}
					else
					{
						bankItem(playerItems[removeSlot] , removeSlot, itemAmount(playerItems[removeSlot]));
					}
				}
				else if (interfaceID == 5510)
				{
					fromBank(bankItems[removeSlot] , removeSlot, bankItemsN[removeSlot]);
				}

				break;


			case 135:		//bank X items Part1

				outStream.createFrame(27);
				bankXremoveSlot = inStream.readSignedWordBigEndian();
				bankXinterfaceID = inStream.readUnsignedWordA();
				bankXremoveID = inStream.readSignedWordBigEndian();
				//println_debug(bankXinterfaceID+"Bank X Items Part1: "+bankXremoveID+" ja slot: "+bankXremoveSlot);

				break;

			case 208:		//bank X items Part2
				int bankXamount = inStream.readDWord();
					if (bankXinterfaceID == 5064)
					{
						bankItem(playerItems[bankXremoveSlot] , bankXremoveSlot, bankXamount);
					}
					else if (bankXinterfaceID == 5382)
					{
						fromBank(bankItems[bankXremoveSlot] , bankXremoveSlot, bankXamount);
					}
				break;


			case 87:		// drop item
				int droppedItem = inStream.readUnsignedWordA();
				somejunk = inStream.readUnsignedByte()+inStream.readUnsignedByte();
				int slot = inStream.readUnsignedWordA();
				//println_debug("dropItem: "+droppedItem+" Slot: "+slot);
				dropItem(droppedItem, slot);
				break;

                        case 185:               //clicking most buttons
                          int actionButtonId = misc.HexToInt(inStream.buffer, 0, packetSize);
                          switch(actionButtonId) {
                            //These values speak for themselves
                            case 4140: TeleTo("Varrock"); break;
                            case 4143: TeleTo("Lumby"); break;
                            case 4146: TeleTo("Falador"); break;
                            case 4150: TeleTo("Camelot"); break;
                            case 6004: TeleTo("Ardougne"); break;
                            case 6005: TeleTo("Watchtower"); break;
                            case 29031: TeleTo("Trollheim"); break;

                            //Added some prayer and skill buttons for moving the character
                            case 21233: if (heightLevel > 0){
					heightLevel -= 1;
					teleportToX = absX;
					teleportToY = absY;
                                      }
                                      break;

                            case 21234: if (heightLevel < 3) {
					heightLevel += 1;
					teleportToX = absX;
					teleportToY = absY;
                                      }
                                      break;

                            case 21235:	heightLevel = 0;
					teleportToX = absX;
					teleportToY = (absY  + 6400);
					break;

                            case 21236:	heightLevel = 0;
					teleportToX = absX;
					teleportToY = (absY  - 6400);
					break;


                            //Log out
                            case 9154:
                                logout();
				break;

                            case 21241: createItems = !createItems;

							case 21011:
								takeAsNote=false;
								break;

							case 21010:
								takeAsNote=true;
								break;

                            default:
                              //System.out.println("Player stands in: X="+absX+" Y="+absY);
                              println_debug("Action Button: "+actionButtonId);
                              break;
                          }
                          break;
			// the following Ids are the reason why AR-type cheats are hopeless to make...
			// basically they're just there to make reversing harder
			case 226:
			case 78:
			case 148:
			case 183:
			case 230:
			case 136:
			case 189:
			case 152:
			case 200:
			case 85:
			case 165:
			case 238:
			case 150:
			case 36:
			case 246:
			case 77:
				break;

			// any packets we might have missed
			default:
				println_debug("Unhandled packet ["+packetType+", size="+packetSize+"]: "+misc.Hex(inStream.buffer, 0, packetSize));
				break;
		}
	}
  private int somejunk;

	public PlayerSave loadGame(String playerName, String playerPass)
	{
		PlayerSave tempPlayer;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream("./savedGames/"+playerName+".dat"));
			tempPlayer = (PlayerSave)in.readObject();
			in.close();
		}
		catch(Exception e){
			return null;
		}
		return tempPlayer;
	}
}
