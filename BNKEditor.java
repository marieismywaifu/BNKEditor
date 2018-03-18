package bnkeditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A tool for changing the sounds inside an Audiokinetic Wwise SoundBank, witout modifying any of the additional information inside the SoundBank.
 * Just about all I know about this format, I know from reading <a href="http://wiki.xentax.com/index.php/Wwise_SoundBank_(*.bnk)">wiki.xentax.com/index.php/Wwise_SoundBank_(*.bnk)</a>.
 * @author marieismywaifu
 * @version 1.0
 */
public class BNKEditor {
	private final CustomInputStream input;
	private final byte[] bkhd;
	private final int numWEMs;
	private final byte[][] bufferedWEMs;
	private final int[] ids, offsets, originalLengths, replacedLengths;
	private final File[] replacements;
	private final int offsetAbsolute, dataLength;
	
	private byte[] rest;
	
	/**
	 * Constructs a new <code>BNKEditor</code>.
	 * Initializes all the stuff that needs to be initialized, and gets ready for modifying.
	 * @param bnk the file to modify
	 * @param littleEndian the endianness of the file to modify
	 * @throws FileNotFoundException if the file cannot be found
	 * @throws IllegalArgumentException if the file has an unexpected layout (for example when it's not a SoundBank at all)
	 * @throws IOException if something else goes wrong
	 */
	public BNKEditor(File bnk, boolean littleEndian) throws IOException {
		if (bnk.length() > Integer.MAX_VALUE) throw new IllegalArgumentException("The file is too big!");
		input = new CustomInputStream(bnk, littleEndian);
		
		// read and verify BKHD section
		if (!"BKHD".equals(input.readMagic())) throw new IllegalArgumentException("The file doesn't have a BKHD section!");
		int bkhdLength = input.readInt();
		bkhd = input.read(bkhdLength);
		
		// read and verify DIDX section header
		if (!"DIDX".equals(input.readMagic())) throw new IllegalArgumentException("The file doesn't have a DIDX section!");
		int didxLength = input.readInt();
		if (didxLength % 12 != 0) throw new IllegalArgumentException("The file has a corrupted DIDX section! (its length is " + didxLength + ", which is not divisible by 12)");
		
		// read DIDX section data
		numWEMs = didxLength / 12;
		bufferedWEMs = new byte[numWEMs][];
		ids = new int[numWEMs];
		offsets = new int[numWEMs];
		originalLengths = new int[numWEMs];
		replacedLengths = new int[numWEMs];
		replacements = new File[numWEMs];
		for (int i = 0; i < numWEMs; i++) {
			int id = input.readInt(), offset = input.readInt(), length_ = input.readInt();
			if (i > 0 && offset < offsets[i - 1]) throw new IllegalArgumentException("The file has a corrupted DIDX section! (WEM number " + (i + 1) + " is located at offset " + offset + ", while WEM number " + i + " is located at offset " + offsets[i - 1] + ")");
			ids[i] = id;
			offsets[i] = offset;
			originalLengths[i] = length_;
			replacedLengths[i] = length_;
		}
		
		// read and verify DATA section header
		if (!"DATA".equals(input.readMagic())) throw new IllegalArgumentException("The file doesn't have a DATA section!");
		dataLength = input.readInt();
		int calc = 0;
		for (int wem = 0; wem < numWEMs; wem++) {
			calc += originalLengths[wem];
		}
		if (dataLength < calc) throw new IllegalArgumentException("The file has a corrupted DATA section! (calculated length: " + calc + ", actual length: " + dataLength + ")");
		offsetAbsolute = (int) input.getCurrentPosition();
	}
	
	/**
	 * Returns an array containing the IDs of all the WEMs inside this SoundBank.
	 * Note that IDs seem to be completely random, so neighboring IDs might be scattered across multiple SoundBanks.
	 * Some (if not most) of the possible IDs may not exist at all.
	 * @return an array containing the IDs of all the WEMs inside this SoundBank
	 */
	public int[] getIDs() {
		return ids;
	}
	
	/**
	 * Writes the specified WEM into a seperate file.
	 * @param index purpose explained below
	 * @param isID <code>false</code>: <code><b>index</b></code> is the position of the WEM inside the SoundBank
	 * <br><code>true</code>: <code><b>index</b></code> is the ID of the WEM to write
	 * @param wem the file to write the WEM to
	 * @throws ArrayIndexOutOfBoundsException if <code><b>isID</b></code> is true and the ID <code><b>index</b></code> does not exist in this SoundBank
	 * @throws IOException if something else goes wrong
	 */
	public void writeWEM(int index, boolean isID, File wem) throws IOException {
		// find out the position in the arrays
		int position = index;
		if (isID) {
			for (int i = 0; i < numWEMs; i++) {
				if (ids[i] == index) {
					position = i;
					break;
				}
			}
		}
		
		// read all WEMs before the one you have to write
		for (int i = 0; i <= position; i++) {
			if (input.getCurrentPosition() <= offsets[i] + offsetAbsolute) {
				input.skipUntil(offsets[i] + offsetAbsolute);
				bufferedWEMs[i] = input.read(originalLengths[i]);
			}
		}
		
		// actually write
		wem.createNewFile();
		CustomOutputStream output = new CustomOutputStream(wem, true);
		output.write(bufferedWEMs[position]);
		output.flushAndClose();
	}
	
	/**
	 * Marks the specified WEM as replaced with the specified file.
	 * Note that the specified file is not accessed until {@link #writeBNK(File, boolean)} is called.
	 * @param index purpose explained below
	 * @param isID <code>false</code>: <code><b>index</b></code> is the position of the WEM inside the SoundBank
	 * <br><code>true</code>: <code><b>index</b></code> is the ID of the WEM to replace
	 * @param replacement the file to replace the specified WEM with
	 * @throws ArrayIndexOutOfBoundsException if <code><b>isID</b></code> is true and the ID <code><b>index</b></code> does not exist in this SoundBank
	 * @throws IllegalArgumentException if the specified file is longer than <code>Integer.MAX_VALUE</code> bytes
	 */
	public void replace(int index, boolean isID, File replacement) {
		if (replacement.length() > Integer.MAX_VALUE) throw new IllegalArgumentException("The WEM is too large!");
		
		// find out the position in the arrays
		int position = index;
		if (isID) {
			for (int i = 0; i < numWEMs; i++) {
				if (ids[i] == index) {
					position = i;
					break;
				}
			}
		}
		
		replacements[position] = replacement;
		replacedLengths[position] = (int) replacement.length();
	}
	
	/**
	 * Cancels the replacement of the specified WEM.
	 * Calling this method before marking the specified WEM as replaced causes no problems.
	 * @param index purpose explained below
	 * @param isID <code>false</code>: <code><b>index</b></code> is the position of the WEM inside the SoundBank
	 * <br><code>true</code>: <code><b>index</b></code> is the ID of the WEM to replace
	 * @throws ArrayIndexOutOfBoundsException if <code><b>isID</b></code> is true and the ID <code><b>index</b></code> does not exist in this SoundBank
	 */
	public void cancelReplacement(int index, boolean isID) {
		// find out the position in the arrays
		int position = index;
		if (isID) {
			for (int i = 0; i < numWEMs; i++) {
				if (ids[i] == index) {
					position = i;
					break;
				}
			}
		}
		
		replacements[position] = null;
		replacedLengths[position] = originalLengths[position];
	}
	
	/**
	 * Returns an array of all the files that will be accessed if {@link #writeBNK(File, boolean)} is called now.
	 * <code>null</code> means the WEM in this position in the SoundBank will not be replaced.
	 * Anything else means that file is accessed when <code>writeBNK</code> is called.
	 * @return an array of all the files that will be accessed if <code>writeBNK</code> is called now
	 */
	public File[] getReplacements() {
		return replacements;
	}
	
	/**
	 * Writes the modified SoundBank to the specified file.
	 * Note that all replacements are accessed upon calling this method.
	 * If they've been deleted between calling {@link #replace(int, boolean, File)} and now, you're gonna run into some problems.
	 * @param bnk the file to write the modified SoundBank to
	 * @param littleEndian the endianness of the SoundBank
	 * @throws IOException if something goes wrong
	 */
	public void writeBNK(File bnk, boolean littleEndian) throws IOException {
		bnk.createNewFile();
		CustomOutputStream output = new CustomOutputStream(bnk, littleEndian);
		
		// write BKHD section
		output.writeString("BKHD");
		output.writeInt(bkhd.length);
		output.write(bkhd);
		
		// write DIDX section header
		output.writeString("DIDX");
		output.writeInt(numWEMs * 12);
		
		// write DIDX section data
		output.writeInt(ids[0]);
		output.writeInt(0);
		output.writeInt(replacedLengths[0]);
		int currentAddress = replacedLengths[0];
		for (int i = 1; i < numWEMs; i++) {
			output.writeInt(ids[i]);
			output.writeInt(currentAddress);
			output.writeInt(replacedLengths[i]);
			currentAddress += replacedLengths[i];
		}
		
		// write DATA section header
		output.writeString("DATA");
		int calc = 0;
		for (int i = 0; i < numWEMs; i++) {
			calc += replacedLengths[i];
		}
		output.writeInt(calc);
		
		// write DATA section data
		for (int i = 0; i < numWEMs; i++) {
			if (replacements[i] != null) {
				CustomInputStream replacement = new CustomInputStream(replacements[i], littleEndian);
				output.write(replacement.readRest());
				continue;
			}
			for (int j = 0; j <= i; j++) {
				if (input.getCurrentPosition() <= offsets[j] + offsetAbsolute) {
					input.skipUntil(offsets[j] + offsetAbsolute);
					bufferedWEMs[j] = input.read(originalLengths[j]);
				}
			}
			output.write(bufferedWEMs[i]);
		}
		
		// write rest of file
		if (input.getRemaining() > 0) rest = input.readRest();
		output.write(rest);
		output.flushAndClose();
	}
}