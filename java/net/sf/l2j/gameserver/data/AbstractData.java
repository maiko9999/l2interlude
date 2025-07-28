package net.sf.l2j.gameserver.data;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import net.sf.l2j.Config;

public abstract class AbstractData
{
	public abstract void load();
	
	protected Path resolve(String file)
	{
		return BASE_DATA_PATH.resolve(file);
	}
	
	protected Path resolve(Path file)
	{
		return BASE_DATA_PATH.resolve(file);
	}
	
	protected byte[] readAllBytes(Path path) throws DataException
	{
		try
		{
			return Files.readAllBytes(path);
		}
		catch (IOException e)
		{
			throw new DataException(e);
		}
	}
	
	protected String readString(Path path) throws DataException
	{
		return readString(path, Config.CHARSET);
	}
	
	protected String readString(Path path, Charset charset) throws DataException
	{
		return new String(readAllBytes(path), charset);
	}
	
	public final static Path BASE_DATA_PATH;
	
	static
	{
		BASE_DATA_PATH = Config.DATA_PATH;
	}
}