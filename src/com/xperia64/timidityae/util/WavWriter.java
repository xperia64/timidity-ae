/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.util;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class WavWriter {
	//private boolean writeToFile = false;
	private long filesize = 0;
	private String fileToWrite = "";
	public boolean finishedWriting = false;
	//public boolean dataWritten = false;
	private DataOutputStream outFile;

	private boolean mono;
	//private long sampleRate;

	private byte[] intToByteArray(int i) {
		byte[] b = new byte[4];
		b[0] = (byte) (i & 0xFF);
		b[1] = (byte) ((i >> 8) & 0xFF);
		b[2] = (byte) ((i >> 16) & 0xFF);
		b[3] = (byte) ((i >> 24) & 0xFF);
		return b;
	}

	private byte[] shortToByteArray(short data) {
		return new byte[]{(byte) (data & 0xff), (byte) ((data >>> 8) & 0xff)};
	}

	public void setupOutputFile(String filename, boolean mono, long mySampleRate) {
		//this.sampleRate = sampleRate;
		this.mono = mono;
		finishedWriting = false;
		fileToWrite = filename;
		filesize = 0;
		try {
			long mySubChunk1Size = 16;
			int myBitsPerSample = 16;
			int myFormat = 1;
			long myChannels = ((mono) ? 1 : 2);
			//long mySampleRate = sampleRate;
			long myByteRate = mySampleRate * myChannels * myBitsPerSample / 8;
			int myBlockAlign = (int) (myChannels * myBitsPerSample / 8);

			// byte[] clipData = getBytesFromFile(fileToConvert);

			// long myDataSize = clipData.length; // this changes
			// long myChunk2Size = myDataSize * myChannels * myBitsPerSample/8;
			// long myChunkSize = 36 + myChunk2Size;

			OutputStream os;
			os = new FileOutputStream(new File(fileToWrite));
			BufferedOutputStream bos = new BufferedOutputStream(os);
			outFile = new DataOutputStream(bos);

			outFile.writeBytes("RIFF"); // 00 - RIFF
			outFile.write(intToByteArray(0/* (int)myChunkSize */), 0, 4); // 04 - how big is the rest of this file?
			outFile.writeBytes("WAVE"); // 08 - WAVE
			outFile.writeBytes("fmt "); // 12 - fmt
			outFile.write(intToByteArray((int) mySubChunk1Size), 0, 4); // 16 - size of this chunk
			outFile.write(shortToByteArray((short) myFormat), 0, 2); // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
			outFile.write(shortToByteArray((short) myChannels), 0, 2); // 22 - mono or stereo? 1 or 2? (or 5 or ???)
			outFile.write(intToByteArray((int) mySampleRate), 0, 4); // 24 - samples per second (numbers per second)
			outFile.write(intToByteArray((int) myByteRate), 0, 4); // 28 - bytes per second
			outFile.write(shortToByteArray((short) myBlockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
			outFile.write(shortToByteArray((short) myBitsPerSample), 0, 2); // 34 - how many bits in a sample(number)? usually 16 or 24
			outFile.writeBytes("data"); // 36 - data
			outFile.write(intToByteArray(0/* (int)myChunkSize */), 0, 4); // 40 - how big is this data chunk
			// outFile.write(clipData); // 44 - the actual data itself - just a long string of numbers

			// outFile.flush();
			// outFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finishOutput() {
		try {
			outFile.flush();
			outFile.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		finishedWriting = true;
		long myDataSize = filesize; // this changes
		int myBitsPerSample = 16;
		long myChannels = ((mono) ? 1 : 2);
		long myChunk2Size = myDataSize * myChannels * myBitsPerSample / 8;
		long myChunkSize = 36 + myChunk2Size;

		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(fileToWrite, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		try {
			raf.seek(04);
			raf.write(intToByteArray((int) myChunkSize));
			raf.seek(40);
			raf.write(intToByteArray((int) myDataSize));
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void write(byte[] data, int offset, int length) throws IOException {
		filesize += length;
		outFile.write(data, offset, length);
	}
}
