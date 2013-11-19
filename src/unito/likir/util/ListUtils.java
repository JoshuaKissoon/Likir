package unito.likir.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

import unito.likir.security.MTRandom;

/**
 * List utilities 
 * @author Luca Maria Aiello
 */
public class ListUtils
{

	/**
	 * Durstenfeld's algorithm of Fisher-Yates shuffle
	 * @param list the list to be shuffled
	 */
	@SuppressWarnings("unchecked")
	public static <E> void shuffle(Collection<E> list)
	{
		MTRandom rand = new MTRandom(111);
		
		Object[] arr = list.toArray();
		
		int n = arr.length;     
		while (n > 1) 
        {
            int k = rand.getUniform(0, n-1);  // 0 <= k < n.
            n--;                     // n is now the last pertinent index;
            Object temp = arr[n];     // swap array[n] with array[k] (does nothing if k == n).
            arr[n] = arr[k];
            arr[k] = temp;
        }
		list.clear();
		for (Object obj : arr)
			list.add((E)obj);
	}
	
	public static <T> LinkedList<T> pickRandom(LinkedList<T> list, int n)
	{
		if (n<=0)
			throw new IllegalArgumentException("Parameter n must be > 0");
		if (n>list.size())
			return list;
		int l = list.size();
		LinkedList<T> temp = list;
		LinkedList<T> res = new LinkedList<T>();
		Random r = new Random();
		int index;
		int remaining=l;
		for (int i=0; i<n; i++)
		{
			index = r.nextInt(remaining);
			remaining--;
			res.add(temp.get(index));
			temp.remove(index);
		}
		return res;
	}
	
	/*public static void main(String... args)
	{
		int[] arr = {1,2,3,4,5,6,7,8,9};
		LinkedList<Integer> list = new LinkedList<Integer>();
		for (int i : arr)
			list.add(i);
		ListUtils.shuffle(list);
		for (int i : list)
			System.out.println(" - " + i);
	}*/
}
