import java.io.*;

public class ObjectHandler {
	public static int MaxObjects = 100000;
							//process() is called evry 500 ms
	public static int MaxOpenDelay = 120; //120 * 500 = 60000 / 1000 = 60s

	public static int[] ObjectOriID = new int[MaxObjects];
	public static int[] ObjectID = new int[MaxObjects];
	public static int[] ObjectX = new int[MaxObjects];
	public static int[] ObjectY = new int[MaxObjects];
	public static int[] ObjectH = new int[MaxObjects];
	public static int[] ObjectDelay = new int[MaxObjects];
	public static int[] ObjectOriType = new int[MaxObjects];
	public static int[] ObjectType = new int[MaxObjects];
	public static int[] ObjectOriFace = new int[MaxObjects];
	public static int[] ObjectFace = new int[MaxObjects];
	public static boolean[] ObjectOriOpen = new boolean[MaxObjects];
	public static boolean[] ObjectOpen = new boolean[MaxObjects];
/*FIREMAKING*/
	public static int FireDelay = 80; //80 * 500 = 40000 / 1000 = 40s
	public static int FireGianDelay = 10; //10 * 500 = 5000 / 1000 = 5s
	public static int[] ObjectFireID = new int[MaxObjects];
	public static int[] ObjectFireX = new int[MaxObjects];
	public static int[] ObjectFireY = new int[MaxObjects];
	public static int[] ObjectFireH = new int[MaxObjects];
	public static int[] ObjectFireDelay = new int[MaxObjects];
	public static int[] ObjectFireMaxDelay = new int[MaxObjects];
	public static int[] ObjectFireDeletecount = new int[MaxObjects];

	ObjectHandler() {
		for(int i = 0; i < MaxObjects; i++) {
			ObjectID[i] = -1;
			ObjectX[i] = -1;
			ObjectY[i] = -1;
			ObjectH[i] = -1;
			ObjectDelay[i] = 0;
			ObjectOriType[i] = 1;
			ObjectType[i] = 1;
			ObjectOriFace[i] = 0;
			ObjectFace[i] = 0;
			ObjectOriOpen[i] = false;
			ObjectOpen[i] = false;
			ResetFire(i);
		}
		loadObjects("config\\objects.cfg");
	}

	public void process() {
		for(int i = 0; i < MaxObjects; i++) {
			if (ObjectID[i] > -1) {
				if (ObjectDelay[i] > 0) {
					ObjectDelay[i]--;
				}
				if (ObjectDelay[i] == 0) {
					if (ObjectOpen[i] != ObjectOriOpen[i]) {
						for (int j = 0; j < server.playerHandler.maxPlayers; j++) {
							if (server.playerHandler.players[j] != null) {
								server.playerHandler.players[j].ChangeDoor[i] = true;
							}
						}
						ObjectOpen[i] = ObjectOriOpen[i];
					}
				}
			}
		}
	}

	public boolean loadObjects(String FileName) {
		String line = "";
		String token = "";
		String token2 = "";
		String token2_2 = "";
		String[] token3 = new String[10];
		boolean EndOfFile = false;
		int ReadMode = 0;
		BufferedReader characterfile = null;
		try {
			characterfile = new BufferedReader(new FileReader("./"+FileName));
		} catch(FileNotFoundException fileex) {
			misc.println(FileName+": file not found.");
			return false;
		}
		try {
			line = characterfile.readLine();
		} catch(IOException ioexception) {
			misc.println(FileName+": error loading file.");
			return false;
		}
		while(EndOfFile == false && line != null) {
			line = line.trim();
			int spot = line.indexOf("=");
			if (spot > -1) {
				token = line.substring(0, spot);
				token = token.trim();
				token2 = line.substring(spot + 1);
				token2 = token2.trim();
				token2_2 = token2.replaceAll("\t\t", "\t");
				token2_2 = token2_2.replaceAll("\t\t", "\t");
				token2_2 = token2_2.replaceAll("\t\t", "\t");
				token2_2 = token2_2.replaceAll("\t\t", "\t");
				token2_2 = token2_2.replaceAll("\t\t", "\t");
				token3 = token2_2.split("\t");
				if (token.equals("object")) {
					for (int i = 0; i < MaxObjects; i++) {
						if (ObjectID[i] == -1) {
							ObjectOriID[i] = Integer.parseInt(token3[0]);
							ObjectID[i] = Integer.parseInt(token3[0]);
							ObjectX[i] = Integer.parseInt(token3[1]);
							ObjectY[i] = Integer.parseInt(token3[2]);
							ObjectH[i] = Integer.parseInt(token3[3]);
							ObjectOriFace[i] = Integer.parseInt(token3[4]);
							ObjectFace[i] = Integer.parseInt(token3[4]);
							ObjectOriType[i] = Integer.parseInt(token3[5]);
							ObjectType[i] = Integer.parseInt(token3[5]);
							if (token3[6].equals("true")) {
								ObjectOriOpen[i] = true;
								ObjectOpen[i] = true;
							}
							break;
						}
					}
				}
			} else {
				if (line.equals("[ENDOFOBJECTLIST]")) {
					try { characterfile.close(); } catch(IOException ioexception) { }
					return true;
				}
			}
			try {
				line = characterfile.readLine();
			} catch(IOException ioexception1) { EndOfFile = true; }
		}
		try { characterfile.close(); } catch(IOException ioexception) { }
		return false;
	}
/*FIREMAKING*/
	public void firemaking_process() {
		for(int i = 0; i < MaxObjects; i++) {
			if (ObjectFireID[i] > -1) {
				if (ObjectFireDelay[i] < ObjectFireMaxDelay[i]) {
					ObjectFireDelay[i]++;
				} else {
					for (int j = 1; j < server.playerHandler.maxPlayers; j++) {
						if (server.playerHandler.players[j] != null) {
							server.playerHandler.players[j].FireDelete[i] = true;
						}
					}
				}
			}
		}
	}
	public void ResetFire(int ArrayID) {
			ObjectFireID[ArrayID] = -1;
			ObjectFireX[ArrayID] = -1;
			ObjectFireY[ArrayID] = -1;
			ObjectFireH[ArrayID] = -1;
			ObjectFireDelay[ArrayID] = 0;
			ObjectFireMaxDelay[ArrayID] = 0;
	}
}
