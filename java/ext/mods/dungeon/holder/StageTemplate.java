package ext.mods.dungeon.holder;

public class StageTemplate
{
	public final int order;
	public final int x, y, z;
	public final boolean teleport;
	public final int timeLimit;

	public StageTemplate(int order, int x, int y, int z, boolean teleport, int timeLimit)
	{
		this.order = order;
		this.x = x;
		this.y = y;
		this.z = z;
		this.teleport = teleport;
		this.timeLimit = timeLimit;
	}
}