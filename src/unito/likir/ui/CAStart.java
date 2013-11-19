package unito.likir.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import unito.likir.ca.CA;

/**
 * Starts the Certification Authority service
 * @author Luca Maria Aiello
 * @version 0.1
 */
public class CAStart
{
	public static void main(String... args)
    {
    	try
        {
            CA ca = new CA();
            ca.startup();
            int choice;
            while (true)
    		{
            	printCAMenu();
    			choice = choose(" >> ", 0, 1);
    			switch (choice)
    			{
    				case 1 :
    					System.out.println(ca);
    					break;
    				case 0 :
    					InputStreamReader inReader = new InputStreamReader(System.in);
    		    		BufferedReader bufReader = new BufferedReader(inReader);
    		            System.out.println("Save state? (y/n)");
    		            String save = bufReader.readLine();
    		            if (save.toLowerCase().equals("yes") || save.toLowerCase().equals("y"))
    		            	ca.shutdown(true);
    		            else
    		            	ca.shutdown(false);
    		            System.exit(0);
    		            break;
    		        default:
    		        	System.out.println(ca);
    			}
    		}
        }
        catch(Exception e)
        {
            System.out.println("ERROR in CA!");
            e.printStackTrace();
        }
    }
 
	private static int choose(String requestString, int min, int max)
	{
		InputStreamReader inReader = new InputStreamReader(System.in);
		BufferedReader bufReader = new BufferedReader(inReader);
		String input;
		int inputNumber = -1;
		try
		{
			System.out.print(requestString);
			input = bufReader.readLine();
			inputNumber = Integer.parseInt(input);
			return inputNumber;
		}
		catch(Exception e)
		{
			inputNumber = -1;
		}
		return 0;
	}
    
	private static void printCAMenu()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("\n--------------------------------------\n");
		stringBuilder.append("|       CERTIFICATION AUTHORITY      |\n");
		stringBuilder.append("--------------------------------------\n");
		stringBuilder.append("| 1 - Print status                   |\n");
		stringBuilder.append("| 0 - Exit                           |\n");
		stringBuilder.append("--------------------------------------");
		System.out.println(stringBuilder.toString());
	}
}
