package unito.likir.exceptions;

/**
 * A TaskException models an error occurring during the execution of Node managers
 * @author Aiello Luca Maria
 * @version 0.1
 */
public class TaskException extends RuntimeException
{
	private static final long serialVersionUID = -225352630987654321L;
	
	public TaskException(String errorMsg)
	{
		super(errorMsg);
	}
}
