package us.altio.socketserver;
import java.util.Enumeration;
import java.util.NoSuchElementException;

class ArrayEnumeration implements Enumeration {
	private Object[] array;
	private int index;

	public ArrayEnumeration(Object[] paramArrayOfObject) {
		this.array = paramArrayOfObject;
	}

	public boolean hasMoreElements() {
		return (this.index < this.array.length);
	}

	public Object nextElement() {
		if (this.index < this.array.length)
			return this.array[(this.index++)];
		throw new NoSuchElementException();
	}
}