package net.sf.l2j.gameserver.network.serverpackets;

import java.util.List;

import net.sf.l2j.gameserver.enums.skills.AcquireSkillType;
import net.sf.l2j.gameserver.model.holder.skillnode.ClanSkillNode;
import net.sf.l2j.gameserver.model.holder.skillnode.FishingSkillNode;
import net.sf.l2j.gameserver.model.holder.skillnode.GeneralSkillNode;
import net.sf.l2j.gameserver.model.holder.skillnode.SkillNode;

public final class AcquireSkillList extends L2GameServerPacket
{
	private final AcquireSkillType _type;
	private final List<? extends SkillNode> _skills;
	
	public AcquireSkillList(AcquireSkillType type, List<? extends SkillNode> skills)
	{
		_type = type;
		_skills = skills;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x8a);
		writeD(_type.ordinal());
		writeD(_skills.size());
		
		switch (_type)
		{
			case USUAL:
				_skills.stream().map(GeneralSkillNode.class::cast).forEach(gsn ->
				{
					writeD(gsn.getId());
					writeD(gsn.getValue());
					writeD(gsn.getValue());
					writeD(gsn.getCorrectedCost());
					writeD(0);
				});
				break;
			
			case FISHING:
				_skills.stream().map(FishingSkillNode.class::cast).forEach(gsn ->
				{
					writeD(gsn.getId());
					writeD(gsn.getValue());
					writeD(gsn.getValue());
					writeD(0);
					writeD(1);
				});
				break;
			
			case CLAN:
				_skills.stream().map(ClanSkillNode.class::cast).forEach(gsn ->
				{
					writeD(gsn.getId());
					writeD(gsn.getValue());
					writeD(gsn.getValue());
					writeD(gsn.getCost());
					writeD(0);
				});
				break;
		}
	}
}