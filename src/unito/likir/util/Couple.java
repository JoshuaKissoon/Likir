package unito.likir.util;

public class Couple<T,E>
{
	private T first;
	private E second;
	
	public Couple(T first, E second)
	{
		this.first = first;
		this.second = second;
	}
	
	public T first()
	{
		return first;
	}
	
	public E second()
	{
		return second;
	}
}