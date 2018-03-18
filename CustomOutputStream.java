package bnkeditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A wrapper for an <code>OutputStream</code>.
 * Supports advenced stuff like arbitrary endianness and writing primitive types and <code>String</code>s.
 * Uses a <code>FileOutputStream</code> internally.
 * @author marieismywaifu
 */
public class CustomOutputStream {
	private final OutputStream a;
	private final boolean b;
	
	
	
	/**
	 * Constructs a new <code>CustomOutputStream</code>.
	 * @param file the file to write to
	 * @param littleEndian the endianness of the file (you cannot change endianness on the fly)
	 * @throws FileNotFoundException if the specified file cannot be found
	 */
	public CustomOutputStream(File file, boolean littleEndian) throws FileNotFoundException {
		a = new FileOutputStream(file);
		b = littleEndian;
	}
	
	
	
	/**
	 * Writes an ASCII-encoded <code>String</code>.
	 * @param aString the <code>String</code> to write
	 * @throws IOException passed from the underlying <code>OutputStream</code>
	 */
	public void writeString(String aString) throws IOException {
		write(aString.getBytes(StandardCharsets.US_ASCII));
	}
	
	/**
	 * Writes to the file.
	 * @param bytes the bytes to write
	 * @throws IOException passed from the underlying <code>OutputStream</code>
	 */
	public void write(byte[] bytes) throws IOException {
		a.write(bytes);
	}
	
	
	
	/**
	 * Writes a 64-bit integer.
	 * @param aLong the <code>long</code> to write
	 * @throws IOException passed from the underlying <code>OutputStream</code>
	 */
	public void writeLong(long aLong) throws IOException {
		if (b) {
			writeInt((int) aLong);
			writeInt((int) (aLong >> 32));
		} else {
			writeInt((int) (aLong >> 32));
			writeInt((int) aLong);
		}
	}
	
	/**
	 * Writes a 32-bit integer.
	 * @param anInt the <code>int</code> to write
	 * @throws IOException passed from the underlying <code>OutputStream</code>
	 */
	public void writeInt(int anInt) throws IOException {
		if (b) {
			writeShort((short) anInt);
			writeShort((short) (anInt >> 16));
		} else {
			writeShort((short) (anInt >> 16));
			writeShort((short) anInt);
		}
	}
	
	/**
	 * Writes a 16-bit integer.
	 * @param aShort the <code>short</code> to write
	 * @throws IOException passed from the underlying <code>OutputStream</code>
	 */
	public void writeShort(short aShort) throws IOException {
		if (b) {
			write(aShort);
			write(aShort >> 8);
		} else {
			write(aShort >> 8);
			write(aShort);
		}
	}
	
	/**
	 * Writes a singleton byte.
	 * @param aByte the unsigned integer equivalent to the byte to write
	 * @throws IOException passed from the underlying <code>OutputStream</code>
	 */
	public void write(int aByte) throws IOException {
		a.write(aByte);
	}
	
	
	
	/**
	 * Flushes and closes the underlying <code>OutputStream</code>.
	 * @throws IOException passed from the underlying <code>OutputStream</code>
	 */
	public void flushAndClose() throws IOException {
		a.flush();
		a.close();
	}
}