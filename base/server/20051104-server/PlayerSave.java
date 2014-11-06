import java.io.*;

public class PlayerSave implements Serializable
{
	public String playerPass="";
	public String playerName="";
	//public String connectedFrom=""; //Don't enable this yet, or the old save-files get corrupted
	public int playerPosX;
	public int playerPosY;
	public int playerHeight;
	public int playerRights;
	public int playerStatus;
	public int playerHeadIcon;
	public int[] playerItem;
	public int[] playerItemN;
	public int[] playerEquipment;
	public int[] playerEquipmentN;
	public int[] bankItems;
	public int[] bankItemsN;
	public int[] playerLevel;
	public int[] playerXP;
	public int[] playerQuest;


	public PlayerSave(Player plr)
	{
		playerPass = plr.playerPass;
		playerName = plr.playerName;
		playerPosX = plr.absX;
		playerPosY = plr.absY;
		playerHeight = plr.heightLevel;
		playerRights = plr.playerRights;
		playerItem = plr.playerItems;
		playerItemN = plr.playerItemsN;
		bankItems = plr.bankItems;
		bankItemsN = plr.bankItemsN;
		playerEquipment = plr.playerEquipment;
		playerEquipmentN = plr.playerEquipmentN;
		playerLevel = plr.playerLevel;
		playerXP = plr.playerXP;
		//connectedFrom = plr.connectedFrom;

	}

	/*public static PlayerSave loadGame(String playerName, String playerPass)
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

	private boolean saveGame(Player plr)
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
	}*/

}