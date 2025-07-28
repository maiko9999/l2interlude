package net.sf.l2j.gameserver.handler;

public class BypassHandler extends AbstractHandler<Integer, IBypassHandler>
{
	protected BypassHandler()
	{
		super(IBypassHandler.class, "bypasshandlers");
	}
	
	@Override
	public void registerHandler(IBypassHandler handler)
	{
		for (String id : handler.getBypassList())
			_entries.put(id.hashCode(), handler);
	}
	
	@Override
	public IBypassHandler getHandler(Object key)
	{
		if (!(key instanceof String bypassCommand))
			return null;
		
		final int index = bypassCommand.indexOf(" ");
		final String command = (index == -1) ? bypassCommand : bypassCommand.substring(0, index);
		
		return super.getHandler(command.hashCode());
	}
	
	public static BypassHandler getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		private static final BypassHandler INSTANCE = new BypassHandler();
	}
}