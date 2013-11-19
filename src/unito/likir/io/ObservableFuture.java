package unito.likir.io;

import java.util.Collection;
import java.util.concurrent.RunnableFuture;

/**
 * An ObservableFuture object represent a result of an asynchronous computation, like the Future class,
 * provided with the standard Java library. ObservableFuture provides the RunnableFuture functionalities
 * improved with the observer pattern. All the observers registered to this ObservableFuture will be
 * notified when the computation ends or if a failure occurs
 * @author Luca Maria Aiello
 * @see java.util.concurrent.Future
 * @see java.util.concurrent.RunnableFuture
 * @version 0.1
 */

public interface ObservableFuture<T> extends RunnableFuture<T>
{
	/**
	 * Add an observer to this ObservableFuture
	 * @param observer the observer
	 */
	public void addObserver(FutureObserver<T> observer);
	
	/**
	 * Return the set of observers registered to this ObservableFuture
	 * @return the collection of observers
	 */
	public Collection<FutureObserver<T>> getObservers();
	
	/**
	 * Notify the result of the computation to the observers 
	 * @param result the result of the computation
	 */
	public void notify(T result);
	
	/**
	 * Notify a failure to the observers
	 * @param e the exception occurred
	 */
	public void notifyFailure(Exception e);
}
