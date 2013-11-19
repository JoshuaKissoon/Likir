package unito.likir.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import unito.likir.EnvironmentImpl;
import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.io.FileManager;
import unito.likir.routing.Contact;
import unito.likir.storage.StorageEntry;

/**
 * A simple Shell to play with small Likir networks
 *
 * @author Luca Maria Aiello
 * @version 0.1
 */
public class Shell
{
    //startup parameters

    NodeId idProva;
    public final int MAXNODES = 10000;
    public int initialPort;
    EnvironmentImpl env;
    int port;
    String defaultId;
    int maxIdIndex;
    Node currentNode;
    FileManager fileManager;
    public int nodeNumber = 2;
    File contentArchiveDir;
    File nodeCounter;
    public String contentDirectoryPath = "." + File.separator + "Contents";
    public String nodeCounterPath = "." + File.separator + "nodeCounter";

    //input reader
    public InputStreamReader inReader;
    public BufferedReader bufReader;

    public Shell()
    {
        env = new EnvironmentImpl();
        defaultId = "User";
        inReader = new InputStreamReader(System.in);
        bufReader = new BufferedReader(inReader);
        fileManager = new FileManager();

        contentArchiveDir = new File(contentDirectoryPath);
        if (!(contentArchiveDir.exists() && contentArchiveDir.isDirectory()))
        {
            contentArchiveDir.mkdir();
        }

        try
        {
            nodeCounter = new File(nodeCounterPath);
            if (!nodeCounter.exists())
            {
                nodeCounter.createNewFile();
            }
        }
        catch (IOException ioe)
        {
            System.out.println("Error while loading shell settings ");
            System.exit(-1);
        }
    }

    public static void main(String[] args)
    {

        int choice = -1;
        Shell shell = new Shell();
        shell.printBanner();
        shell.networkStartup();
        while (true)
        {
            shell.printEnvironmentMenu();
            choice = shell.choose(" >> ", 0, 9);
            switch (choice)
            {
                case 1:
                    shell.currentNode = shell.selectNode();
                    while (choice != 0 && shell.currentNode != null)
                    {
                        shell.printNodeMenu(shell.currentNode);
                        choice = shell.choose(" >> ", 0, 8);
                        switch (choice)
                        {
                            case 1:
                                shell.lookup();
                                break;
                            case 2:
                                shell.get(false);
                                break;
                            case 3:
                                shell.get(true);
                                break;
                            case 4:
                                shell.put();
                                break;
                            case 5:
                                shell.unsignedPut();
                                break;
                            case 6:
                                shell.forceStorageManteinance();
                                break;
                            case 7:
                                shell.printRouteTable();
                                break;
                            case 8:
                                shell.printStorage();
                                break;
                            case 0:
                                shell.currentNode = null;
                                break;
                            default:
                                shell.currentNode = null;
                                choice = 0;
                                break;
                        }
                    }
                    break;
                case 2:
                    shell.registerNode();
                    break;
                case 3:
                    shell.killNode();
                    break;
                case 4:
                    shell.forceManteinanceAll();
                    break;
                case 5:
                    shell.printRouteTableAll();
                    break;
                case 6:
                    shell.printStorageAll();
                    break;
                case 7:
                    shell.printEnvironment();
                    break;
                case 0:
                    shell.exit();
                    break;
                default:
                    shell.exit();
                    break;
            }
        }
    }

    private void lookup()
    {
        try
        {
            System.out.print(" Lookup Key : ");
            String input = bufReader.readLine();
            NodeId key = fileManager.hash(input);
            Collection<Contact> result = currentNode.lookup(key).get();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LOOKUP result: \n");
            for (Contact c : result)
            {
                stringBuilder.append(c + "\n");
            }
            System.out.println(stringBuilder.toString());
        }
        catch (Exception e)
        {
            System.err.println(" Error! \n" + e);
        }
    }

    private void get(boolean countersOnly)
    {
        try
        {
            boolean recent = false;
            String owner = null;
            String type = null;

            System.out.print(" Lookup Key      : ");
            String keyWord = bufReader.readLine();
            System.out.print(" Type            : ");
            type = bufReader.readLine();
            System.out.print(" Owner           : ");
            owner = bufReader.readLine();
            System.out.print(" Recent (y/n)    : ");
            String recentChoice = bufReader.readLine();
            int depth = choose(" Lookup depth    : ", 1, 100);

            NodeId key = fileManager.hash(keyWord);
            if (owner.equals(""))
            {
                owner = null;
            }
            if (recentChoice.equals("y"))
            {
                recent = true;
            }

            if (countersOnly)
            {
                LinkedList<HashMap<String, Integer>> result = currentNode.getCounters(keyWord, type, owner, recent, depth).get();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("LOOKUP results : \n");
                stringBuilder.append("Counters--------------------- \n");
                for (HashMap<String, Integer> map : result)
                {
                    for (String k : map.keySet())
                    {
                        stringBuilder.append("[" + k + ":" + map.get(k) + "] - ");
                    }
                    stringBuilder.append("\n");
                }
                System.out.println(stringBuilder.toString());
            }
            else
            {
                Collection<StorageEntry> result = currentNode.get(key, type, owner, recent, depth).get();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("LOOKUP results : \n");
                stringBuilder.append("Contents--------------------- \n");
                for (StorageEntry e : result)
                {
                    stringBuilder.append(e + "\n");
                }
                System.out.println(stringBuilder.toString());
            }
        }
        catch (Exception e)
        {
            System.err.println(" Errore! \n" + e);
        }
    }

    /* OLD SINGLE PUT
     private void put()
     {
     try
     {
     String type = null;
			
     System.out.print(" File name   : ");
     String filename =  bufReader.readLine();
     System.out.print(" File type   : ");
     type =  bufReader.readLine();
     String path = contentDirectoryPath + File.separator + filename;
     File file = new File(path);
     long ttl = choose("Time to live : (millisec)", 1, 1000000000);
     int result = currentNode.put(file, type, ttl).get();
     System.out.println("Content stored in "+result+" nodes");
     }
     catch(Exception e)
     {
     System.err.println(" Error! \n" + e);
     }
		
     }*/
    private void put()
    {
        try
        {
            System.out.print(" How many contents?   : ");
            int n = Integer.parseInt(bufReader.readLine());

            System.out.print(" Store key  : ");
            String key = bufReader.readLine();
            byte[][] contents = new byte[n][];
            String[] types = new String[n];
            long[] ttls = new long[n];

            for (int i = 0; i < n; i++)
            {
                System.out.print(" Content " + i + "  : ");
                contents[i] = bufReader.readLine().getBytes();
                System.out.print(" Type " + i + "  : ");
                types[i] = bufReader.readLine();
                ttls[i] = choose("Time to live " + i + " : (millisec)", 1, 1000000000);
            }

            int result = currentNode.put(key, contents, types, ttls).get();
            System.out.println("Content stored in " + result + " nodes");
        }
        catch (Exception e)
        {
            System.err.println(" Error! \n" + e);
        }

    }

    private void unsignedPut()
    {
        try
        {
            System.out.print(" How many contents?   : ");
            int n = Integer.parseInt(bufReader.readLine());

            System.out.print(" Store key  : ");
            String key = bufReader.readLine();
            byte[][] contents = new byte[n][];
            String[] types = new String[n];
            long[] ttls = new long[n];

            for (int i = 0; i < n; i++)
            {
                System.out.print(" Content " + i + "  : ");
                contents[i] = bufReader.readLine().getBytes();
                System.out.print(" Type " + i + "  : ");
                types[i] = bufReader.readLine();
                ttls[i] = choose("Time to live " + i + " : (millisec)", 1, 1000000000);
            }

            int result = currentNode.unsignedPut(key, contents, types, ttls).get();
            System.out.println("Content stored in " + result + " nodes");
        }
        catch (Exception e)
        {
            System.err.println(" Error! \n" + e);
        }

    }

    private void forceStorageManteinance()
    {
        currentNode.forceStorageManteinace();
        System.out.println("Storage manteinance done!");
    }

    private void printStorage()
    {
        System.out.println(currentNode.getStorage());
    }

    private void printRouteTable()
    {
        System.out.println(currentNode.getRouteTable());
    }

    private Node selectNode()
    {
        try
        {
            System.out.print(" NodeId : ");
            String input = bufReader.readLine();
            Node n = env.selectNode(input);
            if (n == null)
            {
                System.out.println("Node " + input + "does not exists");
            }
            return n;
        }
        catch (IOException ioe)
        {
            System.err.println(" Error! ");
            return null;
        }
    }

    private void registerNode()
    {
        /*Node n = null;
         String name;
         do
         {
         maxIdIndex++;
         System.out.println("MAXINDEX = " + maxIdIndex);
         name = defaultId+maxIdIndex;
         if (env.selectNode(name) == null)
         n = env.registerNode(defaultId+maxIdIndex, initialPort + maxIdIndex);
         }while(n == null);*/

        maxIdIndex++;
        String name = defaultId + maxIdIndex;
        Node n = env.registerNode(defaultId + maxIdIndex, initialPort + maxIdIndex);

        try
        {
            n.startup();
            n.bootstrap();
        }
        catch (Exception e)
        {
            System.out.println("Join failed! " + e);
            env.unregisterNode(name);
            System.out.println("Node not inserted!");
        }
        System.out.println("New node:");
        System.out.println(n);
    }

    private void killNode()
    {
        try
        {
            System.out.print(" UserId : ");
            String input = bufReader.readLine();
            Node n = env.selectNode(input);
            if (n != null)
            {
                n.exit(false);
                env.unregisterNode(input);
                nodeNumber--;
                System.out.println("Node removed!");
            }
            else
            {
                System.out.println("Node " + input + "does not exists");
            }
        }
        catch (IOException ioe)
        {
            System.err.println(" Error! ");
        }

    }

    private void forceManteinanceAll()
    {
        for (Node n : env.getAllNodes())
        {
            n.forceStorageManteinace();
        }
        System.out.println("Storage manteinance done!");
    }

    private void printRouteTableAll()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (Node n : env.getAllNodes())
        {
            stringBuilder.append("------- " + n.getUserId() + " - NodeId = " + n.getNodeId() + " ------------------------\n");
            stringBuilder.append(n.getRouteTable() + "\n");
            stringBuilder.append("-------------------------------------------------------------------\n\n");
        }
        System.out.println(stringBuilder.toString());
    }

    private void printStorageAll()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (Node n : env.getAllNodes())
        {
            stringBuilder.append("-------" + n.getUserId() + " - NodeId = " + n.getNodeId() + "------------------------\n");
            stringBuilder.append(n.getStorage() + "\n");
            stringBuilder.append("-------------------------------------------------------------------\n\n");
        }
        System.out.println(stringBuilder.toString());
    }

    private void printEnvironment()
    {
        System.out.println(env);
    }

    private void exit()
    {
        String save = "";
        while (!(save.equals("y") || save.equals("n")))
        {
            System.out.print(" Save network (y/n) : ");
            try
            {
                save = bufReader.readLine();
                save = save.toLowerCase();
            }
            catch (IOException ioe)
            {
                System.out.println("Input error");
            }
        }
        if (save.equals("y"))
        {
            saveSettings();
            shutdownEnvironment(true);
        }
        else
        {
            shutdownEnvironment(false);
        }
        System.exit(0);
    }

    private void saveSettings()
    {
        FileOutputStream fos = null;
        DataOutputStream dos = null;
        try
        {
            nodeCounter.createNewFile();
            fos = new FileOutputStream(nodeCounter);
            dos = new DataOutputStream(fos);
            System.out.println("WRITING " + maxIdIndex);
            dos.writeInt(maxIdIndex);
            dos.flush();
            fos.close();
            dos.close();
        }
        catch (IOException ioe)
        {
            System.out.println("Error while saving shell settings");
        }
    }

    private void shutdownEnvironment(boolean save)
    {
        System.out.println("----- SHUTDOWN -----");
        env.shutDownAll(save);
    }

    private int choose(String requestString, int min, int max)
    {
        String input;
        int inputNumber = -1;
        while (inputNumber < min || inputNumber > max)
        {
            try
            {
                System.out.print(requestString);
                input = bufReader.readLine();
                inputNumber = Integer.parseInt(input);
            }
            catch (Exception e)
            {
                inputNumber = -1;
            }
        }
        return inputNumber;
    }

    private void printBanner()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("   ********************************\n");
        stringBuilder.append("   * +--------------------------+ *\n");
        stringBuilder.append("   * |     UNITO LIKIR v.0.1    | *\n");
        stringBuilder.append("   * +--------------------------+ *\n");
        stringBuilder.append("   ********************************");
        System.out.println(stringBuilder.toString());
    }

    private void printEnvironmentMenu()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n+------------------------------------+\n");
        stringBuilder.append("|             ENVIRONMENT            |\n");
        stringBuilder.append("+------------------------------------+\n");
        stringBuilder.append("| 1 - Select node                    |\n");
        stringBuilder.append("| 2 - Register new node              |\n");
        stringBuilder.append("| 3 - Kill node                      |\n");
        stringBuilder.append("| 4 - Force mateinance all           |\n");
        stringBuilder.append("| 5 - Print Route Table all          |\n");
        stringBuilder.append("| 6 - Print Storage all              |\n");
        stringBuilder.append("| 7 - Print nodes                    |\n");
        stringBuilder.append("| 0 - Exit                           |\n");
        stringBuilder.append("+------------------------------------+");
        System.out.println(stringBuilder.toString());
    }

    private void printNodeMenu(Node n)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n+------------------------------------+\n");
        stringBuilder.append("             NODE: " + n.getUserId() + "       \n");
        stringBuilder.append("+------------------------------------+\n");
        stringBuilder.append("|  1 - Lookup                        |\n");
        stringBuilder.append("|  2 - Get                           |\n");
        stringBuilder.append("|  3 - Get counters                  |\n");
        stringBuilder.append("|  4 - Put                           |\n");
        stringBuilder.append("|  5 - Unsigned Put                  |\n");
        stringBuilder.append("|  6 - Force Storage Manteinance     |\n");
        stringBuilder.append("|  7 - Print Route Table             |\n");
        stringBuilder.append("|  8 - Print Storage                 |\n");
        stringBuilder.append("|  0 - Back to Environment menu      |\n");
        stringBuilder.append("+------------------------------------+");
        System.out.println(stringBuilder.toString());
    }

    private void networkStartup()
    {
        String load = "n";
        while (!(load.equals("y") || load.equals("n")))
        {
            System.out.print(" Load network (y/n) : ");
            try
            {
                load = bufReader.readLine();
                load = load.toLowerCase();
            }
            catch (IOException ioe)
            {
                System.out.println("Input error");
            }
        }

        if (load.equals("y"))
        {
            try
            {
                FileInputStream fis = null;
                DataInputStream dis = null;
                fis = new FileInputStream(nodeCounter);
                dis = new DataInputStream(fis);
                maxIdIndex = dis.readInt();
                fis.close();
                dis.close();
            }
            catch (IOException ioe)
            {
                System.out.println("Error while loading shell settings " + ioe);
                System.exit(-1);
            }
            initialPort = choose(" Start UDP port (>1024) : ", 1024, 65000);
            env.loadNetwork();
        }
        else
        {
            nodeNumber = 15;//choose(" Number of nodes: ",1,MAXNODES);
            maxIdIndex = nodeNumber - 1;
            initialPort = 3000;//choose(" Start UDP port (>1024) : ", 1024,65000);

            env.registerNodes(nodeNumber, "User", initialPort);
        }

        env.startupAll();
        env.bootstrapAll();
    }
}
