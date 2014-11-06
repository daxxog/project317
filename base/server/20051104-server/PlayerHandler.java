//  This file is free software; you can redistribute it and/or modify it under
//  the terms of the GNU General Public License version 2, 1991 as published by
//  the Free Software Foundation.

//  This program is distributed in the hope that it will be useful, but WITHOUT
//  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
//  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
//  details.

//  A copy of the GNU General Public License can be found at:
//    http://www.gnu.org/licenses/gpl.html

import java.io.*;

public class PlayerHandler{

	// Remark: the player structures are just a temporary solution for now
	// Later we will avoid looping through all the players for each player
	// by making use of a hash table maybe based on map regions (8x8 granularity should be ok)
	public static final int maxPlayers = 512;
	public Player players[] = new Player[maxPlayers];
	public int playerSlotSearchStart = 1;			// where we start searching at when adding a new player
	public static String kickNick = "";
	public static boolean kickAllPlayers=false;
	public static String messageToAll = "";
	public static int playerCount=0;
	public static String playersCurrentlyOn[] = new String[maxPlayers];

	PlayerHandler()
	{
		for(int i = 0; i < maxPlayers; i++) {
			players[i] = null;
		}
	}

	// a new client connected
	public void newPlayerClient(java.net.Socket s, String connectedFrom)
	{
		// first, search for a free slot
		//int slot = -1, i = playerSlotSearchStart;
		int slot = -1, i = 1;
		do {
			if(players[i] == null) {
				slot = i;
				break;
			}
			i++;
			if(i >= maxPlayers) i = 0;		// wrap around
//		} while(i != playerSlotSearchStart);
		} while(i <= maxPlayers);

		client newClient = new client(s, slot);
		newClient.handler = this;
		(new Thread(newClient)).start();
		if(slot == -1) return;		// no free slot found - world is full
		players[slot] = newClient;
		players[slot].connectedFrom=connectedFrom;

		// start at next slot when issuing the next search to speed it up
		playerSlotSearchStart = slot + 1;
		if(playerSlotSearchStart > maxPlayers) playerSlotSearchStart = 1;
		// Note that we don't use slot 0 because playerId zero does not function
		// properly with the client.
	}

	public void destruct()
	{
		for(int i = 0; i < maxPlayers; i++) {
			if(players[i] == null) continue;
			players[i].destruct();
			players[i] = null;
		}
	}

	public static int getPlayerCount()
	{
		return playerCount;
	}

	public void updatePlayerNames(){
		playerCount=0;
		for(int i = 0; i < maxPlayers; i++) {
			if(players[i] != null)
			{
				playersCurrentlyOn[i] = players[i].playerName;
				playerCount++;
			}
			else
				playersCurrentlyOn[i] = "";
		}
	}

	public static boolean isPlayerOn(String playerName)
	{
		for(int i = 0; i < maxPlayers; i++) {
			if(playersCurrentlyOn[i] != null){
				if(playersCurrentlyOn[i].equalsIgnoreCase(playerName)) return true;
			}
		}
		return false;
	}

	public void process()
	{
		updatePlayerNames();
		if (messageToAll.length() > 0)
		{
			int msgTo=1;
			do {
				if(players[msgTo] != null) {
					players[msgTo].globalMessage=messageToAll;
				}
				msgTo++;
			} while(msgTo < maxPlayers);
			messageToAll="";
		}
		if (kickAllPlayers)
		{
			int kickID = 1;
			do {
				if(players[kickID] != null) {
					players[kickID].isKicked = true;
				}
				kickID++;
			} while(kickID < maxPlayers);
			kickAllPlayers = false;
		}

		// at first, parse all the incoming data
		// this has to be seperated from the outgoing part because we have to keep all the player data
		// static, so each client gets exactly the same and not the one clients are ahead in time
		// than the other ones.
		for(int i = 0; i < maxPlayers; i++) {
			if(players[i] == null || !players[i].isActive) continue;

			players[i].actionAmount--;

			players[i].preProcessing();
			while(players[i].process());
			players[i].postProcessing();

			players[i].getNextPlayerMovement();

			if(players[i].playerName.equalsIgnoreCase(kickNick))
			{
				players[i].kick();
				kickNick="";
			}
			if(players[i].disconnected) {
				if(saveGame(players[i])){ System.out.println("Game saved for player "+players[i].playerName); } else { System.out.println("Could not save for "+players[i].playerName); };
				removePlayer(players[i]);
				players[i] = null;
			}
		}

		// loop through all players and do the updating stuff
		for(int i = 0; i < maxPlayers; i++) {
			if(players[i] == null || !players[i].isActive) continue;

			if(players[i].disconnected) {
				if(saveGame(players[i])){ System.out.println("Game saved for player "+players[i].playerName); } else { System.out.println("Could not save for "+players[i].playerName); };
				removePlayer(players[i]);
				players[i] = null;
			}
			else {
				if(!players[i].initialized) {
					players[i].initialize();
					players[i].initialized = true;
				}
				else players[i].update();
			}
		}

		// post processing
		for(int i = 0; i < maxPlayers; i++) {
			if(players[i] == null || !players[i].isActive) continue;

			players[i].clearUpdateFlags();
		}
	}

	private stream updateBlock = new stream(new byte[10000]);
	// should actually be moved to client.java because it's very client specific
	public void updatePlayer(Player plr, stream str)
	{
		updateBlock.currentOffset = 0;

		// update thisPlayer
		plr.updateThisPlayerMovement(str);		// handles walking/running and teleporting
		// do NOT send chat text back to thisPlayer!
		boolean saveChatTextUpdate = plr.chatTextUpdateRequired;
		plr.chatTextUpdateRequired = false;
		plr.appendPlayerUpdateBlock(updateBlock);
		plr.chatTextUpdateRequired = saveChatTextUpdate;

		// update/remove players that are already in the playerList
		str.writeBits(8, plr.playerListSize);
		int size = plr.playerListSize;
		plr.playerListSize = 0;		// we're going to rebuild the list right away
		for(int i = 0; i < size; i++) {
			// this update packet does not support teleporting of other players directly
			// instead we're going to remove this player here and readd it right away below
			if(!plr.playerList[i].didTeleport && plr.withinDistance(plr.playerList[i])) {
				plr.playerList[i].updatePlayerMovement(str);
				plr.playerList[i].appendPlayerUpdateBlock(updateBlock);
				plr.playerList[plr.playerListSize++] = plr.playerList[i];
			}
			else {
				int id = plr.playerList[i].playerId;
				plr.playerInListBitmap[id>>3] &= ~(1 << (id&7));		// clear the flag
				str.writeBits(1, 1);
				str.writeBits(2, 3);		// tells client to remove this char from list
			}
		}

		// iterate through all players to check whether there's new players to add
		for(int i = 0; i < maxPlayers; i++) {
			if(players[i] == null || !players[i].isActive || players[i] == plr) continue;
			int id = players[i].playerId;
			if((plr.playerInListBitmap[id>>3]&(1 << (id&7))) != 0) continue;	// player already in playerList
			if(!plr.withinDistance(players[i])) continue;		// out of sight

			plr.addNewPlayer(players[i], str, updateBlock);
		}

		if(updateBlock.currentOffset > 0) {
			str.writeBits(11, 2047);	// magic EOF - needed only when player updateblock follows
			str.finishBitAccess();

			// append update block
			str.writeBytes(updateBlock.buffer, updateBlock.currentOffset, 0);
		}
		else str.finishBitAccess();

		str.endFrameVarSizeWord();
	}

	private void removePlayer(Player plr)
	{
		// anything can be done here like unlinking this player structure from any of the other existing structures
		plr.destruct();
	}

	public boolean saveGame(Player plr)
	{
		PlayerSave tempSave = new PlayerSave(plr);
		try
		{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("./savedGames/"+tempSave.playerName+".dat"));
			out.writeObject((PlayerSave)tempSave);
			out.close();
		}
		catch(Exception e){
			return false;
		}
		return true;
	}
}
