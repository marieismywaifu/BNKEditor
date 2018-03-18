package bnkeditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper for an <code>InputStream</code>.
 * Supports advanced stuff like arbitrary endianness, reading primitive types and <code>String</code>s, and reading until specified offsets.
 * Additionally, automatically closes the <code>InputStream</code> once its end has been reached.
 * This is not buffered, so if you read something once you can never read it again.
 * Uses a <code>FileInputStream</code> internally.
 * @author marieismywaifu
 */
public class CustomInputStream {
	private final InputStream a;
	private final boolean b;
	private final long c;
	
	private long d;
	
	
	
	/**
	 * Constructs a new <code>CustomInputStream</code>.
	 * @param file the file to read from
	 * @param littleEndian the endianness of the file (you cannot change endianness on the fly)
	 * @throws FileNotFoundException if the specified file cannot be found
	 */
	public CustomInputStream(File file, boolean littleEndian) throws FileNotFoundException {
		a = new FileInputStream(file);
		b = littleEndian;
		c = file.length();
		d = 0;
	}
	
	
	
	/**
	 * Reads from the current position until the end of the file.
	 * @return all read bytes
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 * @throws UnsupportedOperationException if the rest of the file is longer than <code>Integer.MAX_VALUE</code> bytes
	 */
	public byte[] readRest() throws IOException {
		if (c - d > Integer.MAX_VALUE) throw new UnsupportedOperationException("Can't store that many bytes at once!");
		return readUntil(c);
	}
	
	/**
	 * Reads from the current position until the specified position.
	 * @param position until where to read (exclusive)
	 * @return all read bytes
	 * @throws IllegalArgumentException if the specicied position has already been passed, if it is past the end of the file, or if the amount of bytes to read is greater than <code>Integer.MAX_VALUE</code>
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 */
	public byte[] readUntil(long position) throws IOException {
		if (position < d) throw new IllegalArgumentException("That point has already been passed!");
		else if (position - d > Integer.MAX_VALUE) throw new IllegalArgumentException("Can't store that many bytes at once!");
		return read((int) (position - d));
	}
	
	/**
	 * Reads from the file.
	 * @param length the amount of bytes to read
	 * @return all read bytes
	 * @throws IllegalArgumentException if there are less than <code><b>length</b></code> bytes remaining
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 */
	public byte[] read(int length) throws IOException {
		byte[] e = new byte[length];
		read(e);
		return e;
	}
	
	/**
	 * Reads from the file into the specified array.
	 * The entire array is filled, from start to finish.
	 * @param bytes an array to read into
	 * @throws IllegalArgumentException if there are less than <code><b>bytes</b>.length</code> bytes remaining
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 */
	public void read(byte[] bytes) throws IOException {
		if (c < d + bytes.length) throw new IllegalArgumentException("The file isn't that long!");
		long e = d;
		while (d < e + bytes.length) d += a.read(bytes, (int) (d - e), (int) (bytes.length - (d - e)));
		if (d == c) close();
	}
	
	
	
	/**
	 * Reads a four-letter <code>String</code>.
	 * @return the read <code>String</code>
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 * @throws UnsupportedOperationException if there are less than 4 bytes remaining
	 */
	public String readMagic() throws IOException {
		if (c < d + 4) throw new UnsupportedOperationException("The file isn't that long!");
		return readString(4);
	}
	
	/**
	 * Reads an ASCII-encoded <code>String</code>.
	 * @param length the length of the <code>String</code>
	 * @return the read <code>String</code>
	 * @throws IllegalArgumentException if there are less than <code><b>length</b></code> bytes remaining
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 */
	public String readString(int length) throws IOException {
		if (c < d + length) throw new IllegalArgumentException("The file isn't that long!");
		StringBuilder e = new StringBuilder();
		for (int f = 0; f < length; f++) e.append((char) read());
		return e.toString();
	}
	
	/**
	 * Reads a 64-bit integer.
	 * @return the read <code>long</code>
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 * @throws UnsupportedOperationException if there are less than 8 bytes remaining
	 */
	public long readLong() throws IOException {
		if (c < d + 8) throw new UnsupportedOperationException("The file isn't that long!");
		if (b) return read() + 0x100 * read() + 0x10000 * read() + 0x1000000 * read() + 0x100000000l * read() + 0x10000000000l * read() + 0x1000000000000l * read() + 0x100000000000000l * read();
		return 0x100000000000000l * read() + 0x1000000000000l * read() + 0x10000000000l * read() + 0x100000000l * read() + 0x1000000 * read() + 0x10000 * read() + 0x100 * read() + read();
	}
	
	/**
	 * Reads a 32-bit integer.
	 * @return the read <code>int</code>
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 * @throws UnsupportedOperationException if there are less than 4 bytes remaining
	 */
	public int readInt() throws IOException {
		if (c < d + 4) throw new UnsupportedOperationException("The file isn't that long!");
		if (b) return read() + 0x100 * read() + 0x10000 * read() + 0x1000000 * read();
		return 0x1000000 * read() + 0x10000 * read() + 0x100 * read() + read();
	}
	
	/**
	 * Reads a 16-bit integer.
	 * @return the read <code>short</code>
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 * @throws UnsupportedOperationException if there are less than 2 bytes remaining
	 */
	public short readShort() throws IOException {
		if (c < d + 2) throw new UnsupportedOperationException("The file isn't that long!");
		if (b) return (short) (read() + 0x100 * read());
		return (short) (0x100 * read() + read());
	}
	
	/**
	 * Reads a singleton byte.
	 * @return the unsigned integer equivalent to the read byte
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 */
	public int read() throws IOException {
		int e = a.read();
		d++;
		if (d == c) close();
		return e;
	}
	
	
	
	/**
	 * Skips until the specified position.
	 * @param position until where to skip (exclusive)
	 * @throws IllegalArgumentException if the specified position has already been reached or if it is past the end of the file
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 */
	public void skipUntil(long position) throws IOException {
		if (position < d) throw new IllegalArgumentException("That point has already been passed!");
		skip(position - d);
	}
	
	/**
	 * Skips over the specified amount of bytes.
	 * @param amount how many bytes to skip
	 * @throws IllegalArgumentException if there are less than <code><b>amount</b></code> bytes remaining
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 */
	public void skip(long amount) throws IOException {
		if (amount > c + d) throw new IllegalArgumentException("The file isn't that long!");
		long e = d;
		while (d < e +amount) d += a.skip(amount - (d - e));
		if (d == c) close();
		
	}
	
	
	
	/**
	 * Closes the underlying <code>InputStream</code>.
	 * This is called internally automatically once the end of the file has been reached.
	 * @throws IOException passed from the underlying <code>InputStream</code>
	 */
	public void close() throws IOException {
		a.close();
	}
	
	
	
	/**
	 * Returns the current position in the file.
	 * @return the current position in the file (the next byte to read)
	 */
	public long getCurrentPosition() {
		return d;
	}
	
	/**
	 * Returns the total length of the file.
	 * @return the total length of the file in bytes
	 */
	public long getLength() {
		return c;
	}
	
	/**
	 * Returns the amount of bytes remaining.
	 * @return the amount of bytes remaining
	 */
	public long getRemaining() {
		return c - d;
	}
}