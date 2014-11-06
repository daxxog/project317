//A small class for changing player passwords and skill XP values.
//You'll have to figure out how it works. I haven't documented it.
//-daiki

import java.io.*;

public class SaveGameHandler implements Serializable {


	public static PlayerSave loadGame(String playerName)
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


	public static boolean saveGame(PlayerSave plr)
	{
		try
		{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("./savedGames/"+plr.playerName+".dat"));
			out.writeObject((PlayerSave)plr);
			out.close();
		}
		catch(Exception e){
			return false;
		}
		return true;
	}

	public static void main(String[] args)
	{
		if (args.length == 1)
		{
			String profilePass = loadGame(args[0]).playerPass;
			if (profilePass != null)
			{
				System.out.println(args[0]+":\""+profilePass+"\"");
			}
		}
		else if (args.length == 2)
		{

			PlayerSave loadgame = loadGame(args[0]);

			if (loadgame != null)
			{
				loadgame.playerPass = args[1];
				saveGame(loadgame);
				System.out.println(args[0]+"'s new pass is: \""+loadgame.playerPass+"\"");
			}
			else System.out.println("Profile not found!");
		}
		else if (args.length == 4)
		{
			try
			{
				PlayerSave loadgame = loadGame(args[0]);
				String word = args[1];
				int num1 = Integer.parseInt(args[2]);
				int num2 = Integer.parseInt(args[3]);				

				if (loadgame != null && word.equalsIgnoreCase("setExp"))
				{
					loadgame.playerXP[num1] = num2;
					saveGame(loadgame);
					System.out.println(num1+"'s new xp is: "+loadgame.playerXP[num1]);
				}
				else if (loadgame != null && word.equalsIgnoreCase("showExp"))
				{
					System.out.println("Skill("+num1+") : "+loadgame.playerXP[num1]+" xp");
				}
				else System.out.println("Profile not found!");
			}
			catch (Exception e)
			{
				System.out.println("wrong values");
			}
		}
	}
}