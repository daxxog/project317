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
public abstract class Player {
	public void println_debug(String str)
	{
		System.out.println("[player-"+playerId+"]: "+str);
	}
	public void println(String str)
	{
		System.out.println("[player-"+playerId+"]: "+str);
	}

	// some remarks: one map region is 8x8
	// a 7-bit (i.e. 128) value thus ranges over 16 such regions
	// the active area of 104x104 is comprised of 13x13 such regions, i.e. from
	// the center region that is 6 regions in each direction (notice the magical 6
	// appearing also in map region arithmetics...)

	public Player(int _playerId) {
		playerId = _playerId;
		//playerName = "player"+playerId;
		playerRights = 2; //pelaajantila

		for (int i=0; i<playerItems.length; i++) //Setting player items
		{
			playerItems[i] = 0;
		}
		for (int i=0; i<playerItemsN.length; i++) //Setting Item amounts
		{
			playerItemsN[i] = 0;
		}

		for (int i=0; i<playerLevel.length; i++) //Setting Levels
		{
			if (i == 3)
			{
				playerLevel[i] = 10;
			}
			else
			{
				playerLevel[i] = 1;
			}
		}

		for (int i=0; i<playerXP.length; i++) //Setting XP for Levels. Player levels are shown as "Level/LevelForXP(XP)"
		{
			if (i == 3)
			{
				playerXP[i] = 1154;
			}
			else
			{
				playerXP[i] = 0;
			}
		}

		for (int i=0; i<playerBankSize; i++) //Setting bank items
		{
			bankItems[i] = 0;
		}

		for (int i=0; i<playerBankSize; i++) //Setting bank item amounts
		{
			bankItemsN[i] = 0;
		}

		//Giving the player an unique look


		playerEquipment[playerHat]=Item.randomHat();
		playerEquipment[playerCape]=Item.randomCape();
		playerEquipment[playerAmulet]=Item.randomAmulet();
		playerEquipment[playerChest]=Item.randomBody();
		playerEquipment[playerShield]=Item.randomShield();
		playerEquipment[playerLegs]=Item.randomLegs();
		playerEquipment[playerHands]=Item.randomGloves();
		playerEquipment[playerFeet]=Item.randomBoots();
		playerEquipment[playerRing]=Item.randomRing();
		playerEquipment[playerArrows]=Item.randomArrows();
		playerEquipment[playerWeapon]=1275;

/*
0-9: male head
10-17: male beard
18-25: male torso
26-32: male arms
33-35: male hands
36-41: male legs
42-44: male feet

45-55: fem head
56-60: fem torso
61-66: fem arms
67-69: fem hands
70-78: fem legs
79-81: fem feet
*/

		pHead=3;
		pTorso=19;
		pArms=29;
		pHands=35;
		pLegs=39;
		pFeet=44;


		// initial x and y coordinates of the player
		heightLevel = 0;
		// the first call to updateThisPlayerMovement() will craft the proper initialization packet
		teleportToX = 3254;//3072;
		teleportToY = 3420;//3312;

		// client initially doesn't know those values yet
		absX = absY = -1;
		mapRegionX = mapRegionY = -1;
		currentX = currentY = 0;
		resetWalkingQueue();
	}

	void destruct() {
		playerListSize = 0;
		for(int i = 0; i < maxPlayerListSize; i++) playerList[i] = null;

		absX = absY = -1;
		mapRegionX = mapRegionY = -1;
		currentX = currentY = 0;
		resetWalkingQueue();
	}

	public boolean initialized = false, disconnected = false;
	public boolean isActive = false;
	public boolean isKicked = false;

	public int actionTimer = 0;
	public int actionAmount = 0;
	public String actionName = "";

	public String connectedFrom="";
	public String lastConnectionFrom="";
	public String globalMessage="";

	public int tradeWith = 0;
	public int tradeFromID = 0;
	public String tradeFrom = "";
	public int tradeWaitingTime=0;
	public boolean tradeAccepted=false;

	public boolean takeAsNote = false;
	
	public abstract void initialize();

	public abstract void update();

	public int playerId = -1;		// -1 denotes world is full, otherwise this is the playerId
									// corresponds to the index in Player players[]

	public String playerName = null;			// name of the connecting client
	public String playerPass = null;			// name of the connecting client


	public int playerRights;		// 0=normal player, 1=player mod, 2=real mod, 3=admin?

	public PlayerHandler handler = null;

	public int maxItemAmount = 2000000000;

	public int[] playerItems = new int[28];
	public int[] playerItemsN = new int[28];

	public int playerBankSize = 120;
	public int[] bankItems = new int[800];
	public int[] bankItemsN = new int[800];
	public boolean bankNotes = false;


	//Default appearance
	public int pHead;
	public int pTorso;
	public int pArms;
	public int pHands;
	public int pLegs;
	public int pFeet;

	public int[] playerEquipment = new int[14];
	public int[] playerEquipmentN = new int[14];
	
	public int playerHat=0;
	public int playerCape=1;
	public int playerAmulet=2;
	public int playerWeapon=3;
	public int playerChest=4;
	public int playerShield=5;
	public int playerLegs=7;
	public int playerHands=9;
	public int playerFeet=10;
	public int playerRing=12;
	public int playerArrows=13;

/*
 *	public int playerHat=1048;	// blue party hat
 *	public int playerCape=1052;	// <- legends cape
 *	public int playerAmulet=1712;	// Amulet of glory (4)
 *	public int playerWeapon=4726;	// <-- guthan spear(Abyssal whip = 4151)
 *	public int playerChest=3140;	// Dragon chainbody = 3140
 *	public int playerShield=3842;	// Unholy book
 *	public int playerLegs=4585;	// Dragon plateskirt
 *	public int playerHands=775;	// Cooking gauntlets
 *	public int playerFeet=1837;	// desert boots
 *	public int playerRing=2570;	// ring of life
 *	public int playerArrows=892;	// rune arrow
 */

	public int[] playerLevel = new int[25];
	public int[] playerXP = new int[25];

	// the list of players currently seen by thisPlayer
	// this has to be remembered because the client will build up exactly the same list
	// and will be used on subsequent player movement update packets for efficiency
	public final static int maxPlayerListSize = PlayerHandler.maxPlayers;
	public Player playerList[] = new Player[maxPlayerListSize];
	public int playerListSize = 0;
	// bit at position playerId is set to 1 incase player is currently in playerList
	public byte playerInListBitmap[] = new byte[(PlayerHandler.maxPlayers+7) >> 3];


	// supported within the packet adding new players are coordinates relative to thisPlayer
	// that are >= -16 and <= 15 (i.e. a signed 5-bit number)
	public boolean withinDistance(Player otherPlr)
	{
		if(heightLevel != otherPlr.heightLevel) return false;
		int deltaX = otherPlr.absX-absX, deltaY = otherPlr.absY-absY;
		return deltaX <= 15 && deltaX >= -16 && deltaY <= 15 && deltaY >= -16;
	}


	public int mapRegionX, mapRegionY;		// the map region the player is currently in
	public int absX, absY;					// absolute x/y coordinates
	public int currentX, currentY;			// relative x/y coordinates (to map region)
	// Note that mapRegionX*8+currentX yields absX
	public int heightLevel;		// 0-3 supported by the client

	public boolean updateRequired = true;		// set to true if, in general, updating for this player is required
												// i.e. this should be set to true whenever any of the other 
												// XXXUpdateRequired flags are set to true
												// Important: this does NOT include chatTextUpdateRequired!

	// walking related stuff - walking queue etc...
	public static final int walkingQueueSize = 50;
    public int walkingQueueX[] = new int[walkingQueueSize], walkingQueueY[] = new int[walkingQueueSize];
	public int wQueueReadPtr = 0;		// points to slot for reading from queue
	public int wQueueWritePtr = 0;		// points to (first free) slot for writing to the queue
	public boolean isRunning = false;
	public int teleportToX = -1, teleportToY = -1;	// contain absolute x/y coordinates of destination we want to teleport to

	public void resetWalkingQueue()
	{
		wQueueReadPtr = wQueueWritePtr = 0;
		// properly initialize this to make the "travel back" algorithm work
		for(int i = 0; i < walkingQueueSize; i++) {
			walkingQueueX[i] = currentX;
			walkingQueueY[i] = currentY;
		}
	}

	public void addToWalkingQueue(int x, int y)
	{
		int next = (wQueueWritePtr+1) % walkingQueueSize;
		if(next == wQueueWritePtr) return;		// walking queue full, silently discard the data
		walkingQueueX[wQueueWritePtr] = x;
		walkingQueueY[wQueueWritePtr] = y;
		wQueueWritePtr = next; 
	}

	// returns 0-7 for next walking direction or -1, if we're not moving
	public int getNextWalkingDirection()
	{
		if(wQueueReadPtr == wQueueWritePtr) return -1;		// walking queue empty
		int dir;
		do {
			dir = misc.direction(currentX, currentY, walkingQueueX[wQueueReadPtr], walkingQueueY[wQueueReadPtr]);
			if(dir == -1) wQueueReadPtr = (wQueueReadPtr+1) % walkingQueueSize;
			else if((dir&1) != 0) {
				println_debug("Invalid waypoint in walking queue!");
				resetWalkingQueue();
				return -1;
			}
		} while(dir == -1 && wQueueReadPtr != wQueueWritePtr);
		if(dir == -1) return -1;
		dir >>= 1;
		currentX += misc.directionDeltaX[dir];
		currentY += misc.directionDeltaY[dir];
		absX += misc.directionDeltaX[dir];
		absY += misc.directionDeltaY[dir];
		return dir;
	}

	// calculates directions of player movement, or the new coordinates when teleporting
	public boolean didTeleport = false;		// set to true if char did teleport in this cycle
	public boolean mapRegionDidChange = false;
	public int dir1 = -1, dir2 = -1;		// direction char is going in this cycle
        public boolean createItems = false;
        public int poimiX = 0, poimiY = 0;
	public void getNextPlayerMovement()
	{
		mapRegionDidChange = false;
		didTeleport = false;
		dir1 = dir2 = -1;

		if(teleportToX != -1 && teleportToY != -1) {
			mapRegionDidChange = true;
			if(mapRegionX != -1 && mapRegionY != -1) {
				// check, whether destination is within current map region
				int relX = teleportToX-mapRegionX*8, relY = teleportToY-mapRegionY*8;
				if(relX >= 2*8 && relX < 11*8 && relY >= 2*8 && relY < 11*8)
					mapRegionDidChange = false;
			}
			if(mapRegionDidChange) {
				// after map region change the relative coordinates range between 48 - 55
				mapRegionX = (teleportToX>>3)-6;
				mapRegionY = (teleportToY>>3)-6;

				playerListSize = 0;		// completely rebuild playerList after teleport AND map region change
			}

			currentX = teleportToX - 8*mapRegionX;
			currentY = teleportToY - 8*mapRegionY;
			absX = teleportToX;
			absY = teleportToY;
			resetWalkingQueue();

			teleportToX = teleportToY = -1;
			didTeleport = true;
		}
		else {
			dir1 = getNextWalkingDirection();
			if(dir1 == -1) return;		// standing

			if(isRunning) dir2 = getNextWalkingDirection();

			// check, if we're required to change the map region
			int deltaX = 0, deltaY = 0;
			if(currentX < 2*8) {
				deltaX = 4*8;
				mapRegionX -= 4;
				mapRegionDidChange = true;
			}
			else if(currentX >= 11*8) {
				deltaX = -4*8;
				mapRegionX += 4;
				mapRegionDidChange = true;
			}
			if(currentY < 2*8) {
				deltaY = 4*8;
				mapRegionY -= 4;
				mapRegionDidChange = true;
			}
			else if(currentY >= 11*8) {
				deltaY = -4*8;
				mapRegionY += 4;
				mapRegionDidChange = true;
			}

			if(mapRegionDidChange) {
				// have to adjust all relative coordinates
				currentX += deltaX;
				currentY += deltaY;
				for(int i = 0; i < walkingQueueSize; i++) {
					walkingQueueX[i] += deltaX;
					walkingQueueY[i] += deltaY;
				}
			}

		}
	}

	// handles anything related to character position, i.e. walking,running and teleportation
	// applies only to the char and the client which is playing it
	public void updateThisPlayerMovement(stream str)
	{
		if(mapRegionDidChange) {
			str.createFrame(73);
			str.writeWordA(mapRegionX+6);	// for some reason the client substracts 6 from those values
			str.writeWord(mapRegionY+6);
		}

		if(didTeleport) {
			str.createFrameVarSizeWord(81);
			str.initBitAccess();
			str.writeBits(1, 1);
			str.writeBits(2, 3);			// updateType
			str.writeBits(2, heightLevel);
			str.writeBits(1, 1);			// set to true, if discarding (clientside) walking queue
			str.writeBits(1, (updateRequired) ? 1 : 0);
			str.writeBits(7, currentY);
			str.writeBits(7, currentX);

			return ;
		}

		if(dir1 == -1) {
			// don't have to update the character position, because we're just standing
			str.createFrameVarSizeWord(81);
			str.initBitAccess();
			if(updateRequired) {
				// tell client there's an update block appended at the end
				str.writeBits(1, 1);
				str.writeBits(2, 0);
			}
			else str.writeBits(1, 0);
		}
		else {
                   if (createItems) {
                        //Creating new items here
			str.createFrame(85);
			str.writeByteC(currentY);
			str.writeByteC(currentX);
			str.createFrame(44);
                        int newItemID = (int)((Math.random() * 10) + 1038);
                        if (newItemID % 2 == 1)
                          newItemID++;
			str.writeWordBigEndianA(newItemID); //itemId
			str.writeWord(5); //amount
			str.writeByte(0);  // x(4 MSB) y(LSB) coords
                   }

			str.createFrameVarSizeWord(81);
			str.initBitAccess();
			str.writeBits(1, 1);

			if(dir2 == -1) {
				// send "walking packet"
				str.writeBits(2, 1);		// updateType
				str.writeBits(3, misc.xlateDirectionToClient[dir1]);
				if(updateRequired) str.writeBits(1, 1);		// tell client there's an update block appended at the end
				else str.writeBits(1, 0);
			}
			else {
				// send "running packet"
				str.writeBits(2, 2);		// updateType
				str.writeBits(3, misc.xlateDirectionToClient[dir1]);
				str.writeBits(3, misc.xlateDirectionToClient[dir2]);
				if(updateRequired) str.writeBits(1, 1);		// tell client there's an update block appended at the end
				else str.writeBits(1, 0);
			}
		}

	}

	// handles anything related to character position basically walking, running and standing
	// applies to only to "non-thisPlayer" characters
	public void updatePlayerMovement(stream str)
	{
		if(dir1 == -1) {
			// don't have to update the character position, because the char is just standing
			if(updateRequired || chatTextUpdateRequired) {
				// tell client there's an update block appended at the end
				str.writeBits(1, 1);
				str.writeBits(2, 0);
			}
			else str.writeBits(1, 0);
		}
		else if(dir2 == -1) {
			// send "walking packet"
			str.writeBits(1, 1);
			str.writeBits(2, 1);
			str.writeBits(3, misc.xlateDirectionToClient[dir1]);
			str.writeBits(1, (updateRequired || chatTextUpdateRequired) ? 1: 0);
		}
		else {
			// send "running packet"
			str.writeBits(1, 1);
			str.writeBits(2, 2);
			str.writeBits(3, misc.xlateDirectionToClient[dir1]);
			str.writeBits(3, misc.xlateDirectionToClient[dir2]);
			str.writeBits(1, (updateRequired || chatTextUpdateRequired) ? 1: 0);
		}
	}

	// a bitmap of players that we want to keep track of whether char appearance has changed so
	// we know if we have to transmit those or can make use of the cached char appearances in the client
	public byte cachedPropertiesBitmap[] = new byte[(PlayerHandler.maxPlayers+7) >> 3];

	public void addNewPlayer(Player plr, stream str, stream updateBlock)
	{
		int id = plr.playerId;
		playerInListBitmap[id >> 3] |= 1 << (id&7);	// set the flag
		playerList[playerListSize++] = plr;

		str.writeBits(11, id);	// client doesn't seem to like id=0

		// TODO: properly implement the character appearance handling
		// send this everytime for now and don't make use of the cached ones in client
		str.writeBits(1, 1);	// set to true, if player definitions follow below
		boolean savedFlag = plr.appearanceUpdateRequired;
		boolean savedUpdateRequired = plr.updateRequired;
		plr.appearanceUpdateRequired = true;
		plr.updateRequired = true;
		plr.appendPlayerUpdateBlock(updateBlock);
		plr.appearanceUpdateRequired = savedFlag;
		plr.updateRequired = savedUpdateRequired;


		str.writeBits(1, 1);	// set to true, if we want to discard the (clientside) walking queue
								// no idea what this might be useful for yet
		int z = plr.absY-absY;
		if(z < 0) z += 32;
		str.writeBits(5, z);	// y coordinate relative to thisPlayer
		z = plr.absX-absX;
		if(z < 0) z += 32;
		str.writeBits(5, z);	// x coordinate relative to thisPlayer
	}



	// player appearance related stuff
	protected boolean appearanceUpdateRequired = true;	// set to true if the player appearance wasn't synchronized
														// with the clients yet or changed recently

	protected static stream playerProps;
	static {
		playerProps = new stream(new byte[100]);
	}
	protected void appendPlayerAppearance(stream str)
	{
		playerProps.currentOffset = 0;

		// TODO: yet some things to figure out on this block + properly implement this
		playerProps.writeByte(0);		// player sex. 0=Male and 1=Female
		playerProps.writeByte(0);		// playerStatusMask - skull, prayers etc alkup 0

		// defining the character shape - 12 slots following - 0 denotes a null entry and just a byte is used
		// slot 0,8,11,1 is head part - missing additional things are beard and eyepatch like things either 11 or 1
		// cape, apron, amulet... the remaining things...

///////////////////////////////////////AAAAA///////////////////////////////////

		if (playerEquipment[playerHat] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerHat]);
		else
			playerProps.writeByte(0);

		if (playerEquipment[playerCape] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerCape]);
		else
			playerProps.writeByte(0);

		if (playerEquipment[playerAmulet] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerAmulet]);
		else
			playerProps.writeByte(0);


		if (playerEquipment[playerWeapon] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerWeapon]);
		else
			playerProps.writeByte(0);

		if (playerEquipment[playerChest] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerChest]);
		else
			playerProps.writeWord(0x100+pTorso);

		if (playerEquipment[playerShield] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerShield]);
		else
			playerProps.writeByte(0);

		if (!Item.isPlate(playerEquipment[playerChest]))
			playerProps.writeWord(0x100+pArms);
		else
			playerProps.writeByte(0);

		if (playerEquipment[playerLegs] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerLegs]);
		else
			playerProps.writeWord(0x100+pLegs);

		if (!Item.isFullHelm(playerEquipment[playerHat]) && !Item.isFullMask(playerEquipment[playerHat]))
			playerProps.writeWord(0x100 + pHead);		// head
		else
			playerProps.writeByte(0);

		if (playerEquipment[playerHands] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerHands]);
		else
			playerProps.writeWord(0x100+pHands);

		if (playerEquipment[playerFeet] > 1)
			playerProps.writeWord(0x200 + playerEquipment[playerFeet]);
		else playerProps.writeWord(0x100+pFeet);

		playerProps.writeByte(0);

/*
[3204]: name="Dragon halberd"
[4087]: name="Dragon platelegs"
[4585]: name="Dragon plateskirt"
[4587]: name="Dragon scimitar"
[1187]: name="Dragon sq shield"
[1664]: name="Dragon necklace"
[3140]: name="Dragon chainbody"
*/
/*
0-9: male head
10-17: male beard
18-25: male torso
26-32: male arms
33-35: male hands
36-41: male legs
42-44: male feet

45-55: fem head
56-60: fem torso
61-66: fem arms
67-69: fem hands
70-78: fem legs
79-81: fem feet
82-83: some weird wagon???
*/

		// npc as player
//		playerProps.writeWord(-1);
//		playerProps.writeWord(83);


		// array of 5 bytes defining the colors
		playerProps.writeByte(7);	// hair color
		playerProps.writeByte(8);	// torso color.
		playerProps.writeByte(9);	// leg color
		playerProps.writeByte(5);	// feet color
		playerProps.writeByte(0);	// skin color (0-6)

		// player animation indices
		playerProps.writeWord(0x328);		// standAnimIndex
		playerProps.writeWord(0x337);		// standTurnAnimIndex
		playerProps.writeWord(0x333);		// walkAnimIndex
		playerProps.writeWord(0x334);		// turn180AnimIndex
		playerProps.writeWord(0x335);		// turn90CWAnimIndex
		playerProps.writeWord(0x336);		// turn90CCWAnimIndex
		playerProps.writeWord(0x338);		// runAnimIndex


		// npc animations - just testing...
/*		playerProps.writeWord(66);		// standAnimIndex
		playerProps.writeWord(66);		// standTurnAnimIndex
		playerProps.writeWord(63);		// walkAnimIndex
		playerProps.writeWord(66);		// turn180AnimIndex
		playerProps.writeWord(66);		// turn90CWAnimIndex
		playerProps.writeWord(66);		// turn90CCWAnimIndex
		playerProps.writeWord(63);		// runAnimIndex
*/

		playerProps.writeQWord(misc.playerNameToInt64(playerName));

		int combatLevel = (int)((double)playerLevel[0]*0.32707 + (double)playerLevel[1]*0.249 + (double)playerLevel[2]*0.324 + (double)playerLevel[3]*0.25 + (double)playerLevel[5]*0.124);

		playerProps.writeByte(combatLevel);		// combat level
		playerProps.writeWord(0);		// incase != 0, writes skill-%d

		str.writeByteC(playerProps.currentOffset);		// size of player appearance block
		str.writeBytes(playerProps.buffer, playerProps.currentOffset, 0);
 	}

	protected boolean chatTextUpdateRequired = false;
	protected byte chatText[] = new byte[4096], chatTextSize = 0;
	protected int chatTextEffects = 0, chatTextColor = 0;
	protected void appendPlayerChatText(stream str)
	{
		str.writeWordBigEndian(((chatTextColor&0xFF) << 8) + (chatTextEffects&0xFF));
		str.writeByte(playerRights);
		str.writeByteC(chatTextSize);		// no more than 256 bytes!!!
		str.writeBytes_reverse(chatText, chatTextSize, 0);
	}

	public void appendPlayerUpdateBlock(stream str)
	{
		if(!updateRequired && !chatTextUpdateRequired) return ;		// nothing required
		int updateMask = 0;
		if(chatTextUpdateRequired) updateMask |= 0x80;
		if(appearanceUpdateRequired) updateMask |= 0x10;

		if(updateMask >= 0x100) {
			// byte isn't sufficient
			updateMask |= 0x40;			// indication for the client that updateMask is stored in a word
			str.writeByte(updateMask & 0xFF);
			str.writeByte(updateMask >> 8);
		}
		else str.writeByte(updateMask);

		// now writing the various update blocks itself - note that their order crucial
		if(chatTextUpdateRequired) appendPlayerChatText(str);
		if(appearanceUpdateRequired) appendPlayerAppearance(str);

		// TODO: add the various other update blocks

	}

	public void clearUpdateFlags()
	{
		updateRequired = false;
		chatTextUpdateRequired = false;
		appearanceUpdateRequired = false;
	}



	protected static int newWalkCmdX[] = new int[walkingQueueSize];
	protected static int newWalkCmdY[] = new int[walkingQueueSize];
	protected static int newWalkCmdSteps = 0;
	protected static boolean newWalkCmdIsRunning = false;
	protected static int travelBackX[] = new int[walkingQueueSize];
	protected static int travelBackY[] = new int[walkingQueueSize];
	protected static int numTravelBackSteps = 0;

	public void preProcessing()
	{
		newWalkCmdSteps = 0;
	}

	// is being called regularily every 500ms - do any automatic player actions herein
	public abstract boolean process();

	public void postProcessing()
	{
		if(newWalkCmdSteps > 0) {
			// place this into walking queue
			// care must be taken and we can't just append this because usually the starting point (clientside) of
			// this packet and the current position (serverside) do not coincide. Therefore we might have to go
			// back in time in order to find a proper connecting vertex. This is also the origin of the character
			// walking back and forth when there's noticeable lag and we keep on seeding walk commands.
			int firstX = newWalkCmdX[0], firstY = newWalkCmdY[0];	// the point we need to connect to from our current position...

			// travel backwards to find a proper connection vertex
			int lastDir = 0;
			boolean found = false;
			numTravelBackSteps = 0;
			int ptr = wQueueReadPtr;
			int dir = misc.direction(currentX, currentY, firstX, firstY);
			if(dir != -1 && (dir&1) != 0) {
				// we can't connect first and current directly
				do {
					lastDir = dir;
					if(--ptr < 0) ptr = walkingQueueSize-1;

					travelBackX[numTravelBackSteps] = walkingQueueX[ptr];
					travelBackY[numTravelBackSteps++] = walkingQueueY[ptr];
					dir = misc.direction(walkingQueueX[ptr], walkingQueueY[ptr], firstX, firstY);
					if(lastDir != dir) {
						found = true;
						break;		// either of those two, or a vertex between those is a candidate
					}

				} while(ptr != wQueueWritePtr);
			}
			else found = true;	// we didn't need to go back in time because the current position
								// already can be connected to first

			if(!found) println_debug("Fatal: couldn't find connection vertex! Dropping packet.");
			else {
				wQueueWritePtr = wQueueReadPtr;		// discard any yet unprocessed waypoints from queue

				addToWalkingQueue(currentX, currentY);	// have to add this in order to keep consistency in the queue

				if(dir != -1 && (dir&1) != 0) {
					// need to place an additional waypoint which lies between walkingQueue[numTravelBackSteps-2] and
					// walkingQueue[numTravelBackSteps-1] but can be connected to firstX/firstY

					for(int i = 0; i < numTravelBackSteps-1; i++) {
						addToWalkingQueue(travelBackX[i], travelBackY[i]);
					}
					int wayPointX2 = travelBackX[numTravelBackSteps-1], wayPointY2 = travelBackY[numTravelBackSteps-1];
					int wayPointX1, wayPointY1;
					if(numTravelBackSteps == 1) {
						wayPointX1 = currentX;
						wayPointY1 = currentY;
					}
					else {
						wayPointX1 = travelBackX[numTravelBackSteps-2];
						wayPointY1 = travelBackY[numTravelBackSteps-2];
					}
					// we're coming from wayPoint1, want to go in direction wayPoint2 but only so far that
					// we get a connection to first

					// the easiest, but somewhat ugly way:
					// maybe there is a better way, but it involves shitload of different
					// cases so it seems like it isn't anymore
					dir = misc.direction(wayPointX1, wayPointY1, wayPointX2, wayPointY2);
					if(dir == -1 || (dir&1) != 0) {
						println_debug("Fatal: The walking queue is corrupt! wp1=("+wayPointX1+", "+wayPointY1+"), "+
							"wp2=("+wayPointX2+", "+wayPointY2+")");
					}
					else {
						dir >>= 1;
						found = false;
						int x = wayPointX1, y = wayPointY1;
						while(x != wayPointX2 || y != wayPointY2) {
							x += misc.directionDeltaX[dir];
							y += misc.directionDeltaY[dir];
							if((misc.direction(x, y, firstX, firstY)&1) == 0) {
								found = true;
								break;
							}
						}
						if(!found) {
							println_debug("Fatal: Internal error: unable to determine connection vertex!"+
								"  wp1=("+wayPointX1+", "+wayPointY1+"), wp2=("+wayPointX2+", "+wayPointY2+"), "+
								"first=("+firstX+", "+firstY+")");
						}
						else addToWalkingQueue(wayPointX1, wayPointY1);
					}
				}
				else {
					for(int i = 0; i < numTravelBackSteps; i++) {
						addToWalkingQueue(travelBackX[i], travelBackY[i]);
					}
				}

				// now we can finally add those waypoints because we made sure about the connection to first
				for(int i = 0; i < newWalkCmdSteps; i++) {
					addToWalkingQueue(newWalkCmdX[i], newWalkCmdY[i]);
				}

			}

			isRunning = newWalkCmdIsRunning;
		}
	}

	public void kick()
	{
		isKicked=true;
	}
	public void newTrade()
	{
		System.out.println(playerName+": Opens a new trade with "+tradeFrom+"!");
		tradeFrom="";
		//tradeWith = 0;
		tradeFromID = 0;
		tradeWaitingTime = 0;
	}
}