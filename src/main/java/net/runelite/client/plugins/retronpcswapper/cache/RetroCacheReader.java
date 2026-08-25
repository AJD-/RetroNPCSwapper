/*
 * Copyright (c) 2026, AJD
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.retronpcswapper.cache;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader for RS2 2005 cache format (main_file_cache.dat & idx files).
 */
public class RetroCacheReader
{
	private static final Logger log = LoggerFactory.getLogger(RetroCacheReader.class);

	private final File cacheDir;
	private RandomAccessFile datFile;
	private final RandomAccessFile[] idxFiles = new RandomAccessFile[5];

	public RetroCacheReader(File cacheDir)
	{
		this.cacheDir = cacheDir;
	}

	public boolean init()
	{
		try
		{
			File mainDat = new File(cacheDir, "main_file_cache.dat");
			if (!mainDat.exists())
			{
				log.warn("2005 cache main_file_cache.dat not found in: {}", cacheDir.getAbsolutePath());
				return false;
			}
			datFile = new RandomAccessFile(mainDat, "r");

			for (int i = 0; i < 5; i++)
			{
				File idx = new File(cacheDir, "main_file_cache.idx" + i);
				if (idx.exists())
				{
					idxFiles[i] = new RandomAccessFile(idx, "r");
				}
			}
			log.info("Initialized 2005 RetroCacheReader successfully from: {}", cacheDir.getAbsolutePath());
			return true;
		}
		catch (Exception e)
		{
			log.error("Failed to initialize 2005 RetroCacheReader", e);
			return false;
		}
	}

	public byte[] readFile(int indexId, int fileId)
	{
		if (indexId < 0 || indexId >= idxFiles.length || idxFiles[indexId] == null || datFile == null)
		{
			return null;
		}

		try
		{
			RandomAccessFile idx = idxFiles[indexId];
			long idxOffset = (long) fileId * 6;
			if (idxOffset + 6 > idx.length())
			{
				return null;
			}

			idx.seek(idxOffset);
			int fileSize = ((idx.readUnsignedByte() & 0xFF) << 16)
				| ((idx.readUnsignedByte() & 0xFF) << 8)
				| (idx.readUnsignedByte() & 0xFF);

			int firstSector = ((idx.readUnsignedByte() & 0xFF) << 16)
				| ((idx.readUnsignedByte() & 0xFF) << 8)
				| (idx.readUnsignedByte() & 0xFF);

			if (fileSize <= 0 || firstSector <= 0)
			{
				return null;
			}

			byte[] data = new byte[fileSize];
			int currentSector = firstSector;
			int bytesRead = 0;
			int part = 0;

			while (bytesRead < fileSize)
			{
				if (currentSector == 0)
				{
					break;
				}

				long sectorOffset = (long) currentSector * 520;
				if (sectorOffset + 520 > datFile.length())
				{
					break;
				}

				datFile.seek(sectorOffset);
				int nextFileId = datFile.readUnsignedShort();
				int currentPart = datFile.readUnsignedShort();
				int nextSector = ((datFile.readUnsignedByte() & 0xFF) << 16)
					| ((datFile.readUnsignedByte() & 0xFF) << 8)
					| (datFile.readUnsignedByte() & 0xFF);
				int nextIndexId = datFile.readUnsignedByte() & 0xFF;

				if (nextFileId != fileId || currentPart != part || nextIndexId != (indexId + 1))
				{
					log.warn("Corrupted sector header: expected file {}, part {}, index {}, got file {}, part {}, index {}",
						fileId, part, indexId + 1, nextFileId, currentPart, nextIndexId);
					return null;
				}

				int chunkLen = Math.min(fileSize - bytesRead, 512);
				datFile.readFully(data, bytesRead, chunkLen);
				bytesRead += chunkLen;
				currentSector = nextSector;
				part++;
			}

			return data;
		}
		catch (Exception e)
		{
			log.error("Failed to read file {} from index {}", fileId, indexId, e);
			return null;
		}
	}

	public Map<String, byte[]> readArchive(byte[] archiveData)
	{
		Map<String, byte[]> files = new HashMap<>();
		if (archiveData == null || archiveData.length < 6)
		{
			return files;
		}

		try
		{
			Buffer header = new Buffer(archiveData);
			int uncompressedSize = header.read24BitInt();
			int compressedSize = header.read24BitInt();

			byte[] decompressed;
			boolean archiveExtracted;

			if (compressedSize != uncompressedSize)
			{
				byte[] compData = new byte[archiveData.length - 6];
				System.arraycopy(archiveData, 6, compData, 0, compData.length);
				decompressed = BZip2Decompressor.decompress(compData, uncompressedSize);
				archiveExtracted = true;
			}
			else
			{
				decompressed = archiveData;
				archiveExtracted = false;
			}

			Buffer archiveStream = new Buffer(decompressed);
			if (archiveExtracted)
			{
				archiveStream.setOffset(0);
			}
			else
			{
				archiveStream.setOffset(6);
			}

			int totalFiles = archiveStream.readUnsignedShort();

			int[] nameHashes = new int[totalFiles];
			int[] uncompressedLens = new int[totalFiles];
			int[] compressedLens = new int[totalFiles];
			int[] fileOffsets = new int[totalFiles];

			int dataStartOffset = archiveStream.getOffset() + totalFiles * 10;
			for (int i = 0; i < totalFiles; i++)
			{
				nameHashes[i] = archiveStream.readInt();
				uncompressedLens[i] = archiveStream.read24BitInt();
				compressedLens[i] = archiveStream.read24BitInt();
				fileOffsets[i] = dataStartOffset;
				dataStartOffset += compressedLens[i];
			}

			for (int i = 0; i < totalFiles; i++)
			{
				try
				{
					byte[] fileBuffer;
					if (compressedLens[i] != uncompressedLens[i])
					{
						byte[] comp = new byte[compressedLens[i]];
						System.arraycopy(decompressed, fileOffsets[i], comp, 0, compressedLens[i]);
						if (nameHashes[i] == 1489108188 || nameHashes[i] == 1489126980)
						{
							System.out.println("Sub-file hash " + nameHashes[i] + " comp len=" + compressedLens[i] + ", uncomp len=" + uncompressedLens[i] + ", first 6 bytes: " + (comp[0]&0xFF) + " " + (comp[1]&0xFF) + " " + (comp[2]&0xFF) + " " + (comp[3]&0xFF) + " " + (comp[4]&0xFF) + " " + (comp[5]&0xFF));
						}
						fileBuffer = BZip2Decompressor.decompress(comp, uncompressedLens[i]);
					}
					else
					{
						fileBuffer = new byte[uncompressedLens[i]];
						System.arraycopy(decompressed, fileOffsets[i], fileBuffer, 0, uncompressedLens[i]);
					}
					files.put(String.valueOf(nameHashes[i]), fileBuffer);
				}
				catch (Exception ex)
				{
					log.error("Sub-file extraction failed for hash {}", nameHashes[i], ex);
				}
			}

			return files;
		}
		catch (Exception e)
		{
			log.error("Failed to unpack Jag archive", e);
			return files;
		}
	}

	public byte[] getRawSubFile(byte[] archiveData, int targetHash)
	{
		if (archiveData == null || archiveData.length < 6) return null;
		try
		{
			Buffer header = new Buffer(archiveData);
			int uncompressedSize = header.read24BitInt();
			int compressedSize = header.read24BitInt();

			byte[] decompressed;
			boolean archiveExtracted;

			if (compressedSize != uncompressedSize)
			{
				byte[] compData = new byte[archiveData.length - 6];
				System.arraycopy(archiveData, 6, compData, 0, compData.length);
				decompressed = BZip2Decompressor.decompress(compData, uncompressedSize);
				archiveExtracted = true;
			}
			else
			{
				decompressed = archiveData;
				archiveExtracted = false;
			}

			Buffer archiveStream = new Buffer(decompressed);
			if (archiveExtracted)
			{
				archiveStream.setOffset(0);
			}
			else
			{
				archiveStream.setOffset(6);
			}

			int totalFiles = archiveStream.readUnsignedShort();

			int[] nameHashes = new int[totalFiles];
			int[] uncompressedLens = new int[totalFiles];
			int[] compressedLens = new int[totalFiles];
			int[] fileOffsets = new int[totalFiles];

			int dataStartOffset = archiveStream.getOffset() + totalFiles * 10;
			for (int i = 0; i < totalFiles; i++)
			{
				nameHashes[i] = archiveStream.readInt();
				uncompressedLens[i] = archiveStream.read24BitInt();
				compressedLens[i] = archiveStream.read24BitInt();
				fileOffsets[i] = dataStartOffset;
				dataStartOffset += compressedLens[i];
			}

			for (int i = 0; i < totalFiles; i++)
			{
				if (nameHashes[i] == targetHash)
				{
					byte[] comp = new byte[compressedLens[i]];
					System.arraycopy(decompressed, fileOffsets[i], comp, 0, compressedLens[i]);
					return comp;
				}
			}
		}
		catch (Exception ignored) {}
		return null;
	}

	public static int hashFileName(String name)
	{
		int hash = 0;
		name = name.toUpperCase();
		for (int i = 0; i < name.length(); i++)
		{
			hash = (hash * 61 + name.charAt(i)) - 32;
		}
		return hash;
	}

	public void close()
	{
		try
		{
			if (datFile != null) datFile.close();
			for (RandomAccessFile f : idxFiles)
			{
				if (f != null) f.close();
			}
		}
		catch (Exception ignored)
		{
		}
	}
}
