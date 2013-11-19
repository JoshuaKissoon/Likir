package unito.likir.io;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple ObservableFuture implementation
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class ObservableFutureTask<T> implements ObservableFuture<T>
{
	private T result;
	private Set<FutureObserver<T>> observers;
	private boolean isCancelled;
	private boolean isDone;
	private Lock resultLock;
	private Condition resultCondition; 
	
	/**
	 * Creates a new ObservableFutureTask
	 */
	public ObservableFutureTask()
	{
		this.result = null;
		this.observers = Collections.synchronizedSet(new HashSet<FutureObserver<T>>());
		this.isCancelled = false;
		this.isDone = false;
		this.resultLock = new ReentrantLock();
		this.resultCondition = resultLock.newCondition();
	}
	
	public Collection<FutureObserver<T>> getObservers()
	{
		return observers;
	}
	
	public void addObserver(FutureObserver<T> observer)
	{
		observers.add(observer);
		if (isDone)
			observer.update(this, result);
	}
	
	public void notify(T result)
	{
		for (FutureObserver<T> observer : observers)
			observer.update(this, result);
	}
	
	public void notifyFailure(Exception e)
	{
		for (FutureObserver<T> observer : observers)
			observer.updateFailure(this, e);
	}
	
	public boolean cancel(boolean mayInterruptIfRunning) 
	{
		synchronized(this)
		{
			isCancelled = true;
		}
		return true;
	}
	
	public T get() throws InterruptedException
	{
		resultLock.lock();

			if (isCancelled)
				throw new CancellationException();
		
			if (!isDone)
			{
				try
				{	
					resultCondition.await();//this.wait();
				}
				catch (InterruptedException e)
				{
					throw new InterruptedException();
				}
			}
			
		resultLock.unlock();
		return result;
	}
	
	public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
	{
		resultLock.lock();
			if (isCancelled)
				throw new CancellationException();
			
			if (!isDone)
			{
				try
				{
					resultCondition.await(timeout,unit);
				}
				catch (InterruptedException e)
				{
					throw new InterruptedException();
				}
			}
		resultLock.unlock();
			
			if (!isDone)
				throw new TimeoutException();
			else
				return result;
	}
	
	public boolean isCancelled()
	{
		return isCancelled;
	}
	
	public boolean isDone()
	{
		return isDone;
	}
	
	public boolean set(T result)
	{
		if (!isDone)
		{
			resultLock.lock();
				this.result = result;
				isDone = true;
				resultCondition.signalAll();
			resultLock.unlock();
			notify(result);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Override this method
	 */
	public void run()
	{
	}
}