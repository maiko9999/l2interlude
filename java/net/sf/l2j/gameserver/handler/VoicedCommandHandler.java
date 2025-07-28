package net.sf.l2j.gameserver.handler;

public class VoicedCommandHandler extends AbstractHandler<Integer, IVoicedCommandHandler>
{
	protected VoicedCommandHandler()
	{
		super(IVoicedCommandHandler.class, "voicedcommandhandlers");
	}
	
	@Override
	public void registerHandler(IVoicedCommandHandler handler)
	{
		for (String id : handler.getVoicedCommandList())
			_entries.put(id.hashCode(), handler);
	}
	
	public IVoicedCommandHandler getHandler(String voicedCommand)
	{
		String command = voicedCommand.split(" ")[0];
		return _entries.get(command.hashCode());
	}
	
	@Override
	public int size()
	{
		return _entries.size();
	}
	
	public static VoicedCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VoicedCommandHandler INSTANCE = new VoicedCommandHandler();
	}
}