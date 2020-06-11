package tp;

import java.io.*;
import java.net.*;
import java.util.*;
 
public class FtpConnection extends Object
{
    /**
     * Si le flag est a true l'on affiche les messages FTP entrant/sortant
     */
    private static boolean PRINT_DEBUG_INFO = false;
 
    private boolean DEBUG = true;

    private BufferedWriter writer, writerData;

    private BufferedInputStream reader;
    /**
     * Le flag d�fini le type de connection pour les transfers de donn�e.
     * 0: Passif (PASV)
     * 1: Actif (PORT)
     */
    private static int CONNECTION_MODE = 0;
 
    /**
     * Le socket avec lequel l'on se connecte
     */
    private Socket connectionSocket = null;
 
    /**
     * Socket sp�cifique au mode passif.
     */
	private Socket pasvSocket = null;
 
    /**
     * Flux g�n�ral de sortie
     */
    private PrintStream outputStream = null;
 
    /**
     * Flux g�n�ral d'entr�e
     */
    private BufferedReader inputStream = null;
 
    /**
     * Point de retour pour les resumes de transfer
     */
    private long restartPoint = 0L;
 
    /**
     * Status de la connection
     */
    private boolean loggedIn = false;
 
    /**
     * Signe de terminaison pour les r�ponses multi-lignes
     */
    public String lineTerm = "\n";
 
    /**
     * Taille du buffer pour les transfers
     */
    private static int BLOCK_SIZE = 4096;
 
 
    /**
     * Apr�s avoir cr�e un Objet FtpConnection vous devez utiliser les m�thodes connect()
     * puis login(). N'oubliez pas � la fin de vous logout() puis disconnect(),
     * question de politesse ;)
     */
    public FtpConnection ()
    {
    	//Constructeur par defaut
    }
 
 
    /**
     * Surcharge pour sp�cifier le status du mode debug. (true = debug)
     * Ainsi que le mode de connection (1-Passif et 2-Actif)
     * Apr�s avoir cr�e un Objet FtpConnection vous devez utiliser les m�thodes connect()
     * puis login(). N'oubliez pas � la fin de vous logout() puis disconnect(),
     * question de politesse ;)
     */
    public FtpConnection (boolean debugOut,int mode)
    {
    	PRINT_DEBUG_INFO = debugOut;
    	CONNECTION_MODE = mode;
    	DEBUG = debugOut;
    }
 
 
    /**
     * Affiche les informations de debugging si le flag PRINT_DEBUG_INFO est on.
     */
    private void debugPrint(String message) 
    {
        if (PRINT_DEBUG_INFO) System.err.println(message);
    }
 
 
    /**
     * Se connect au FTP donn� sur le port donn�
     */
    public void connect(String host)throws UnknownHostException, IOException
    {
    	host = "127.0.0.1";
    }
 
 
    /**
     * Se connect au FTP donn� sur le port donn�
     */
    public void connect(String host, int port)throws UnknownHostException, IOException
    {
        connectionSocket = new Socket(host, port);
        outputStream = new PrintStream(connectionSocket.getOutputStream());

        inputStream = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

        reader = new BufferedInputStream(connectionSocket.getInputStream());

        writer = new BufferedWriter(new OutputStreamWriter(connectionSocket.getOutputStream()));
        if (!isPositiveCompleteResponse(getServerReply())){
            disconnect();
        }
 
        System.out.println(	"220 Service pr�t pour un nouvel utilisateur");
    }
 
 
    /**
     * Se d�connecte de l'host sur lequel on est actuellement connect�
     */
    public void disconnect()
    {
        if (outputStream != null) {
            try {
        		if (loggedIn) { logout(); };
                outputStream.close();
                inputStream.close();
                connectionSocket.close();
            } catch (IOException e) {}
 
            outputStream = null;
            inputStream = null;
            connectionSocket = null;
        }
    }
 
 
    /**
     * On se log sur le ftp avec un login et un password.
     */
    public void login(String username, String password)throws IOException
    {
        int response = executeCommand("user " + username);
        if (response==331)
        {
        	System.out.println(response+" Utilisateur reconnu. En attente du mot de passe");
        }else { //response==500
        	System.out.println(response+" Erreur de syntaxe. Utilisateur inconnu ");
        }
         response = executeCommand("pass " + password);
        if (response==230)
        {
        	System.out.println(response+" Authentification r�ussie");
        }else { //response==530
        	System.out.println(response+" Connexion non �tablie ");
        }
    }
 
 
    // QUIT :Permet de d�connecter de la session en cours 
    
    public boolean logout()throws IOException
    {
        int response = executeCommand("quit");
        loggedIn = !isPositiveCompleteResponse(response);
        return !loggedIn;
    }
 
    
     //CWD: Permet de changer de r�pertoire (Change Working Directory)
    
    public boolean changeDirectory(String directory)throws IOException
    {
        int response = executeCommand("cwd " + directory);
        return isPositiveCompleteResponse(response);
    }
 
    //Suppression d'un fichier
    public boolean deleteFile(String fileName)throws IOException
    {
        int response = executeCommand("dele " + fileName);
        return isPositiveCompleteResponse(response);
    }
 
 
    
      //R�cup�re le nom du dossier en cours.
    
    public String getCurrentDirectory()throws IOException
    {
        String response = getExecutionResponse("pwd");
        StringTokenizer strtok = new StringTokenizer(response);
 
        // Get rid of the first token, which is the return code
        if (strtok.countTokens() < 2) return null;
        strtok.nextToken();
        String directoryName = strtok.nextToken();
 
        // Most servers surround the directory name with quotation marks
        int strlen = directoryName.length();
        if (strlen == 0) return null;
        if (directoryName.charAt(0) == '\"') {
            directoryName = directoryName.substring(1);
            strlen--;
        }
        if (directoryName.charAt(strlen - 1) == '\"')
            return directoryName.substring(0, strlen - 1);
        return directoryName;
    }
 
    
      //Retourne la taille d'un fichier (en octet)
    
    public long getFileSize(String fileName) throws IOException
    {
        String response = excludeCode(getExecutionResponse("size " + fileName));
        try {
            return Long.parseLong(response);
        } catch (Exception e) {
            return -1L;
        }
    }
 
    
      //T�l�charger un fichier.
    
    public boolean downloadFile(String fileName) throws IOException
    {
        return readDataToFile("retr " + fileName, fileName);
    }
 

     //Uploader un fichier.
     
    public boolean uploadFile(String fileName)throws IOException
    {
        return writeDataFromFile("stor " + fileName, fileName);
    }
 
 
 
 
 
    /** 
     * R�cup�re le code de r�ponse du server FTP. En effet les serveurs FTP r�pondent
     * par des phrases de ce type "xxx message". L'on r�cup�re donc ce code �
     * 3 chiffres pour identifier la nature de la r�ponse (erreur, envoi, etc...).
     */
    private int getServerReply() throws IOException
    {
        return Integer.parseInt(getFullServerReply().substring(0, 3));
    }
 
 
    /** 
     * On retourne la d�rni�re ligne de r�ponse du serveur.
     */
    private String getFullServerReply() throws IOException
    {
        String reply;
 
        do {
            reply = inputStream.readLine();
            debugPrint(reply);
        } while(!(Character.isDigit(reply.charAt(0)) && 
                  Character.isDigit(reply.charAt(1)) &&
                  Character.isDigit(reply.charAt(2)) &&
                  reply.charAt(3) == ' '));
 
        return reply;
    }
 
 
    /** 
     * On retourne la d�rni�re ligne de r�ponse du serveur.
     * On stock l'int�gralit� de la r�ponde dans un buffer.
     */
    private String getFullServerReply(StringBuffer fullReply)throws IOException
    {
        String reply;
        fullReply.setLength(0);
 
        do {
            reply = inputStream.readLine();
            debugPrint(reply);
            fullReply.append(reply + lineTerm);
        } while(!(Character.isDigit(reply.charAt(0)) && 
                  Character.isDigit(reply.charAt(1)) &&
                  Character.isDigit(reply.charAt(2)) &&
                  reply.charAt(3) == ' '));
 
		if (fullReply.length() > 0)  
		{  
			fullReply.setLength(fullReply.length() - lineTerm.length());
		}
 
        return reply;
    }
 
 
    /** 
     * On r�cup�re la liste des fichiers pr�sents dans le dossier courant.
     */
	public String listFiles()
		throws IOException
	{
		return listFiles("");
	}
 
 
    /** 
     * On r�cup�re la liste des fichiers selon le param�tre pass� en argument.
     * Ce param�tre peut �tre le chemin complet, un mask ou les deux.
     * Exemple: "/pub/files/*.txt" va retourner la list de tous les fichiers
     * texte du dossier "files".
     */
	public String listFiles(String params) throws IOException
	{
		StringBuffer files = new StringBuffer();
		StringBuffer dirs = new StringBuffer();
		if (!getAndParseDirList(params, files, dirs))
		{
			debugPrint("Error getting file list");
		}
 
		return files.toString();
	}
 
 
    /** 
     * On r�cup�re la liste des dossiers pr�sents dans le dossier courant.
     */
	public String listSubdirectories()
		throws IOException
	{
		return listSubdirectories("");
	}
 
 
    /** 
     * On r�cup�re la liste des sous dossiers selon le param�tre pass� en argument.
     * Ce param�tre peut �tre le chemin complet, un mask ou les deux.
     * Exemple: "/pub/files/Sub*"
     */
	public String listSubdirectories(String params)throws IOException
	{
		StringBuffer files = new StringBuffer();
		StringBuffer dirs = new StringBuffer();
		if (!getAndParseDirList(params, files, dirs))
		{
			debugPrint("Error getting dir list");
		}
 
		return dirs.toString()+"\n"+files.toString();
	}
 
 
    /** 
     * Envoi et r�cup�re le r�sultat d'une commande de listage
     * tel que LIST ou NLIST.
     */
    private String processFileListCommand(String command)throws IOException
    {
        StringBuffer reply = new StringBuffer();
        String replyString;
 
        //Il est a not� que le listing des fichiers et dossier n�c�ssite,
        //comme le transfer de fichiers, d'une ouverture de port, cot� client
        //ou cot� serveur en fonction du mode de connection (passif ou actif).
 
		boolean success = executeDataCommand(command, reply);
 
		if (!success)
		{
			return "";
		}
 
        replyString = reply.toString();
 
        if(reply.length() > 0)
        {
        	return replyString.substring(0, reply.length() - 1);
        } 
        else 
        {
        	return replyString;
        }
    }
 
 
	/**
         * On r�cup�re toutes les infos d'un dossier et on parse le r�sultat pour
         * r�cup�rer la list des fichiers et des sous dossiers.
         */
	private boolean getAndParseDirList(String params, StringBuffer files, StringBuffer dirs)throws IOException
	{
		// On initialise � 0 les variables de retour
		files.setLength(0);
		dirs.setLength(0);
 
		// On fait les demmandes de listage avec les commandes NLST et LIST
		String shortList = processFileListCommand("NLST " + params);
		String longList = processFileListCommand("LIST " + params);
 
		// On tokenize les lignes r�cup�r�es
		StringTokenizer sList = new StringTokenizer(shortList, "\n");
		StringTokenizer lList = new StringTokenizer(longList, "\n");
 
		// Variables donc on va avoir besoin...
		String sString;
		String lString;
 
		// A not� que les deux lists ont le m�me nombre de ligne.
		while ((sList.hasMoreTokens()) && (lList.hasMoreTokens())) {
			sString = sList.nextToken();
			lString = lList.nextToken();
 
			if (lString.length() > 0)
			{
				if (lString.startsWith("d"))
				{
					dirs.append(sString.trim() + lineTerm);
					debugPrint("Dir: " + sString);
				} 
				else if (lString.startsWith("-")) 
				{
					files.append(sString.trim() + lineTerm);
					debugPrint("File: " + sString);
				} 
				else 
				{
					// Les liens symboliques commencent avec un "l"
					// (lowercase L) mais ne sont pas g�r� ici.
					debugPrint("Unknown: " + lString);
				}
			}
		}
 
		if (files.length() > 0)  {  files.setLength(files.length() - lineTerm.length());  }
		if (dirs.length() > 0)  {  dirs.setLength(dirs.length() - lineTerm.length());  }
 
		return true;
	}
 
 
    /**
     * Execute une commande simple sur le FTP et retourne juste
     * le code r�ponse du message de retour.
     */
    public int executeCommand(String command)throws IOException
    {
    	if(PRINT_DEBUG_INFO) System.out.println(command);
        outputStream.println(command);
        return getServerReply();
    }
 
 
    /**
     * Execute une command FTP et retourne la derni�re ligne de r�ponse du serveur.
     */
    public String getExecutionResponse(String command)throws IOException
    {
		if(PRINT_DEBUG_INFO) System.out.println(command);
        outputStream.println(command);
        return getFullServerReply();
    }
 
 
    /**
     * Execute une commande et stock le r�sultat dans un fichier.
     * Cette fonction � l'avantage de retourner un boulean pour �tre
     * tenu au courant du bon d�roulemet de l'op�ration.
     */
    public boolean readDataToFile(String command, String fileName)throws IOException
    {
        // On ouvre le fichier en local
        RandomAccessFile outfile = new RandomAccessFile(fileName, "rw");
 
        // On lance un restart si d�sir�
        if (restartPoint != 0) {
            debugPrint("Seeking to " + restartPoint);
            outfile.seek(restartPoint);
        }
 
        // Converti le RandomAccessFile en un OutputStream
        FileOutputStream fileStream = new FileOutputStream(outfile.getFD());
        boolean success = executeDataCommand(command, fileStream);
 
        outfile.close();
 
        return success;
    }
 
 
    /**
     * Execute une commande depuis le contenu d'un fichier.
     * Cette fonction retourne un boulean pour �tre
     * tenu au courant du bon d�roulemet de l'op�ration.
     */
    public boolean writeDataFromFile(String command, String fileName)throws IOException
    {
		// On ouvre le fichier en local
        RandomAccessFile infile = new RandomAccessFile(fileName, "r");
 
		// On lance un restart si d�sir�
        if (restartPoint != 0) {
            debugPrint("Seeking to " + restartPoint);
            infile.seek(restartPoint);
        }
 
        // Converti le RandomAccessFile en un InputStream
        FileInputStream fileStream = new FileInputStream(infile.getFD());
        boolean success = executeDataCommand(command, fileStream);
 
        infile.close();
 
        return success;
    }
 
 
    /**
     * Ex�cute une commande sur le serveur FTP et retourne le r�sultat
     * sur le flux de sortie sp�cifi� en argument.
     * Retourne true si l'op�ration c'est effectu�e sans probl�me, sinon false. 
     */
    public boolean executeDataCommand(String command, OutputStream out) throws IOException
    {
    	//Mode passif
		if(CONNECTION_MODE == 0)
		{
			if (!setupDataPasv(command)) return false;
			InputStream in = pasvSocket.getInputStream();
			transferData(in,out);
			in.close();
			pasvSocket.close();
		}
		//Mode actif
		else if(CONNECTION_MODE == 1)
		{
			// On ouvre un socket de donn� en local
			ServerSocket serverSocket = new ServerSocket(0);
			if (!setupDataPort(command, serverSocket)) return false;
			Socket clientSocket = serverSocket.accept();
 
			// On transfer les donn�s
			InputStream in = clientSocket.getInputStream();
			transferData(in, out);
 
			in.close();
			clientSocket.close();
			serverSocket.close();
		}
 
        return isPositiveCompleteResponse(getServerReply());    
    }
 
 
    /**
     * Ex�cute une commande sur le serveur FTP depuis le flux d'entr�e
     * sp�cifi� en argument.
     * Retourne true si l'op�ration c'est effectu�e sans probl�me, sinon false. 
     */
    public boolean executeDataCommand(String command, InputStream in)throws IOException
    {
    	//Mode passif
		if(CONNECTION_MODE == 0)
		{
			if (!setupDataPasv(command)) return false;
			OutputStream out = pasvSocket.getOutputStream();
			transferData(in,out);
			out.close();
			pasvSocket.close();
		}
		//Mode actif
		else if(CONNECTION_MODE == 1)
		{
			// On ouvre un socket de donn� en local
			ServerSocket serverSocket = new ServerSocket(0);
			if (!setupDataPort(command, serverSocket)) return false;
			Socket clientSocket = serverSocket.accept();
 
			// On transfer les donn�s
			OutputStream out = clientSocket.getOutputStream();
			transferData(in, out);
 
			out.close();
			clientSocket.close();
			serverSocket.close();
		}
        return isPositiveCompleteResponse(getServerReply());    
    }
 
    /**
     * Ex�cute une commande sur le serveur FTP et retourne le r�sultat
     * dans un buffer sp�cifi� en argument.
     * Retourne true si l'op�ration s'est effectu�e sans probl�me, sinon false. 
     */
    public boolean executeDataCommand(String command, StringBuffer sb)throws IOException
    {
    	//Mode passif
		if(CONNECTION_MODE == 0)
		{
			if (!setupDataPasv(command)) return false;
			InputStream in = pasvSocket.getInputStream();
			transferData(in,sb);
			in.close();
			pasvSocket.close();
		}
		//Mode actif
		else if(CONNECTION_MODE == 1)
		{
			// On ouvre un socket de donn� en local
			ServerSocket serverSocket = new ServerSocket(0);
			if (!setupDataPort(command, serverSocket)) return false;
			Socket clientSocket = serverSocket.accept();
 
			// On transfer les donn�s
			InputStream in = clientSocket.getInputStream();			
			transferData(in, sb);
 
			in.close();
			clientSocket.close();
			serverSocket.close();
		}
 
        return isPositiveCompleteResponse(getServerReply());    
    }
 
 
    /**
     * Transfer des donn�es depuis un flux d'entr�e vers un flux de sortie.
     */
    private void transferData(InputStream in, OutputStream out) throws IOException
    {
        byte b[] = new byte[BLOCK_SIZE];
        int amount;
 
		// Stock les donn�s dans un fichier
        while ((amount = in.read(b)) > 0)
        {
            out.write(b, 0, amount);
        }
    }
 
    /**
     * Transfer des donn�es depuis un flux d'entr�e vers un buffer.
     */
    private void transferData(InputStream in, StringBuffer sb) throws IOException
    {		
        byte b[] = new byte[BLOCK_SIZE];
        int amount;
 
        // Stock les donn�s dans un buffer
        while ((amount = in.read(b)) > 0)
        {
            sb.append(new String(b, 0, amount));
        }
    }
 
 
    /**
     * On execute la commande donn�e en sp�cifiant au pr�alable:
     * - le PORT sur lequel le serveur FTP va se connecter
     * - le type de transfer. "TYPE i" pour un transfer binaire.
     * - le restartpoint si il existe
     * Si l'opr�ration s'est effectu�e avec succ�s on retourne true, sinon false.
     */
    private boolean setupDataPort(String command, ServerSocket serverSocket) throws IOException
    {
 
        if (!openPort(serverSocket)) return false;
 
        // Lance le mode binaire pour la r�c�ption des donn�s
		if(PRINT_DEBUG_INFO) System.out.println("TYPE i");
		outputStream.println("TYPE i");
		if (!isPositiveCompleteResponse(getServerReply()))
		{
			debugPrint("Could not set transfer type");
			return false;
		}
 
        // Si l'on a un point de restart
        if (restartPoint != 0) {
			if(PRINT_DEBUG_INFO) System.out.println("rest " + restartPoint);
            outputStream.println("rest " + restartPoint);
            restartPoint = 0;
            // TODO: Interpret server response here
            getServerReply();
        }
 
        // Envoi de la commande
		if(PRINT_DEBUG_INFO) System.out.println(command);
        outputStream.println(command);
 
        return isPositivePreliminaryResponse(getServerReply());
    }
 
	/**
        * On execute la commande donn�e en sp�cifiant au pr�alable:
        * - le PORT sur lequel l'on va se connecter
        * - le type de transfer. "TYPE i" pour un transfer binaire.
        * - le restartpoint si il existe
        * Si l'opr�ration s'est effectu�e avec succ�s on retourne true, sinon false.
        */
	private boolean setupDataPasv(String command) throws IOException
	{
 
		if (!openPasv()) return false;
 
		// Lance le mode binaire pour la r�c�ption des donn�s
		if(PRINT_DEBUG_INFO) System.out.println("TYPE i");
		outputStream.println("TYPE i");
		if (!isPositiveCompleteResponse(getServerReply()))
		{
			debugPrint("Could not set transfer type");
			return false;
		}
 
		// Si l'on a un point de restart
		if (restartPoint != 0) {
			if(PRINT_DEBUG_INFO) System.out.println("rest " + restartPoint);
			outputStream.println("rest " + restartPoint);
			restartPoint = 0;
			// TODO: Interpret server response here
			getServerReply();
		}
 
		// Envoi de la command
		if(PRINT_DEBUG_INFO) System.out.println(command);
		outputStream.println(command);
 
		return isPositivePreliminaryResponse(getServerReply());
	}
 
    /**
     * On r�cup�re notre adresse IP et notre numero de port gr�ce � notre
     * ServerSocket et on envoi le tout au serveru FTP via la commande PORT.
     * C'est l'inverse du mode passif avec la commande PASV.
     * Si l'opr�ration s'est effectu�e avec succ�s on retourne true, sinon false.
     */
    private boolean openPort(ServerSocket serverSocket) throws IOException
    {                        
        int localport = serverSocket.getLocalPort();
 
        // On r�cup�re l'adresse IP locale
        InetAddress inetaddress = serverSocket.getInetAddress();
        InetAddress localip;
        try 
        {
            localip = inetaddress.getLocalHost();
        }
        catch(UnknownHostException e)
        {
            debugPrint("Can't get local host");
            return false;
        }
 
        byte[] addrbytes = localip.getAddress();
        short addrshorts[] = new short[4];
 
        for(int i = 0; i <= 3; i++)
        {
            addrshorts[i] = addrbytes[i];
            if (addrshorts[i] < 0)
                addrshorts[i] += 256;
        }
 
        String port = "port " + addrshorts[0] + "," + addrshorts[1] +
		"," + addrshorts[2] + "," + addrshorts[3] + "," +
		((localport & 0xff00) >> 8) + "," +
		(localport & 0x00ff);
 
        if(PRINT_DEBUG_INFO) System.out.println(port);
        outputStream.println(port);
 
        return isPositiveCompleteResponse(getServerReply());
    }
 
    /**
     * On demande au serveur FTP sur quelle adresse IP et quel numero de PORT
     * l'on va �tablir la connection.
     * C'est l'inverse du mode Actif avec la commande PORT.
     * Si l'op�ration s'est effectu�e avec succ�s on retourne true, sinon false.
     */
	private boolean openPasv() throws IOException
	{
		//On passe en mode passif
		String tmp = getExecutionResponse("PASV");
		String pasv = excludeCode(tmp);
		//On r�cup�re l'IP et le PORT pour la connection
		pasv = pasv.substring(pasv.indexOf("(")+1,pasv.indexOf(")"));
		String[] splitedPasv = pasv.split(",");
		int port1 = Integer.parseInt(splitedPasv[4]);
		int port2 = Integer.parseInt(splitedPasv[5]);
		int port = (port1*256)+port2;
		String ip = splitedPasv[0]+"."+splitedPasv[1]+"."+splitedPasv[2]+"."+splitedPasv[3];
 
		pasvSocket = new Socket(ip,port);
 
		return isPositiveCompleteResponse(Integer.parseInt(tmp.substring(0,3)));
	}
 
    /**
     * Retourne true si le code r�ponse du serveur est compris entre 100 et 199.
     */
    private boolean isPositivePreliminaryResponse(int response)
    {
        return (response >= 100 && response < 200);
    }
 
 
    /**
     * Retourne true si le code r�ponse du serveur est compris entre 300 et 399.
     */
    private boolean isPositiveIntermediateResponse(int response)
    {
        return (response >= 300 && response < 400);
    }
 
    /**
     * Retourne true si le code r�ponse du serveur est compris entre 200 et 299.
     */
    private boolean isPositiveCompleteResponse(int response)
    {
        return (response >= 200 && response < 300);
    }
 
 
    /**
     * Retourne true si le code r�ponse du serveur est compris entre 400 et 499.
     */
    private boolean isTransientNegativeResponse(int response)
    {
        return (response >= 400 && response < 500);
    }
 
 
    /**
     * Retourne true si le code r�ponse du serveur est compris entre 500 et 599.
     */
    private boolean isPermanentNegativeResponse(int response)
    {
        return (response >= 500 && response < 600);
    }
 
 
    /**
     * Supprime le code r�ponse au d�but d'une string.
     */
    private String excludeCode(String response)
    {
        if (response.length() < 5) return response;
        return response.substring(4);
    }

}