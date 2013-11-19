package unito.likir.io;

/**
 * An interface for observers of ObservableFuture objects
 * @author Aiello Luca Maria
 * @version 0.1
 */

public interface FutureObserver<T>
{
	/**
	 * Called by the observed object when a result is available
	 * @param o the observed object
	 * @param arg the result
	 */
	public void update(ObservableFuture<T> o, T arg);
	
	/**
	 * Called by the observer object when a failure occurs
	 * @param o the observed object
	 * @param e the exception occurred
	 */
	public void updateFailure(ObservableFuture<T> o, Exception e);
}