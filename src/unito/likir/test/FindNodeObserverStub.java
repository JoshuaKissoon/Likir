package unito.likir.test;

import java.util.Collection;

import unito.likir.io.FutureObserver;
import unito.likir.io.ObservableFuture;
import unito.likir.storage.StorageEntry;

public class FindNodeObserverStub implements FutureObserver<Collection<StorageEntry>>
{
	public FindNodeObserverStub()
	{}
	
	public void update(ObservableFuture<Collection<StorageEntry>> o, Collection<StorageEntry> arg)
	{
		System.out.println("THE OBSERVER HAS RECEIVED A RESPONSE!");
		System.out.println(arg);
	}

	public void updateFailure(ObservableFuture<Collection<StorageEntry>> o, Exception e)
	{
		System.out.println("THE RPC HAS FAILED!");
		System.out.println(e);
	}
}
