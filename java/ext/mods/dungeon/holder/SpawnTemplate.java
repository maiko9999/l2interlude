package ext.mods.dungeon.holder;

public class SpawnTemplate
{
	public final int npcId;
	public final String title;
	public final int count;
	public final int range;
	public final int x, y, z;

	public final String drops;
	
	public SpawnTemplate(int npcId, String title, int count, int range, int x, int y, int z, String drops)
	{
		this.npcId = npcId;
		this.title = title;
		this.count = count;
		this.range = range;
		this.x = x;
		this.y = y;
		this.z = z;
		this.drops = drops;
	}
}