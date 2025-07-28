package net.sf.l2j.gameserver.model.actor.ai;

/**
 * A datatype used as a simple "wish" for Npc.<br>
 * <br>
 * The weight is used to order the priority of the related {@link Intention}.
 */
public class Desire extends Intention
{
	private double _weight;
	
	public Desire(double weight)
	{
		super();
		
		_weight = weight;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return super.equals(obj);
	}
	
	@Override
	public int compareTo(Intention other)
	{
		if (other instanceof Desire otherDesire)
		{
			// Compare by weight
			double weightCompare = Double.compare(otherDesire.getWeight(), getWeight());
			if (weightCompare != 0.0)
				return weightCompare > 0 ? 1 : weightCompare < 0 ? -1 : 0;
		}
		
		return super.compareTo(other);
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
	
	@Override
	public String toString()
	{
		return "Desire [type=" + _type.toString() + " weight=" + _weight + "]";
	}
	
	public double getWeight()
	{
		return _weight;
	}
	
	public void setWeight(double weight)
	{
		_weight = weight;
	}
	
	public void addWeight(double value)
	{
		_weight = Math.min(_weight + value, Double.MAX_VALUE);
	}
	
	public void reduceWeight(double value)
	{
		_weight -= value;
	}
	
	public void autoDecreaseWeight()
	{
		switch (_type)
		{
			case ATTACK:
				_weight -= 6.6;
				break;
			
			case CAST:
				_weight -= 66000;
				break;
			
			case NOTHING:
				_weight -= 0.5;
				break;
		}
	}
//	
//	public boolean isInvalid(Npc actor)
//	{
//		if (_weight <= 0)
//			return true;
//		
//		if (_target != null && !actor.knows(getTarget()))
//			return true;
//		
//		if (_finalTarget != null)
//		{
//			if (!actor.knows(_finalTarget) || _finalTarget.isAlikeDead())
//				return true;
//			
//			switch (_type)
//			{
//				case ATTACK:
//					final AggroInfo aggro = actor.getAI().getAggroList().get(_finalTarget);
//					if (aggro == null || aggro.getHate() == 0)
//						return true;
//					
//					if (_finalTarget.distance3D(actor) > 1500)
//						return true;
//					break;
//				
//				case CAST:
//					final Double hate = actor.getAI().getHateList().get(_finalTarget);
//					if (hate == null || hate == 0)
//						return true;
//					
//					if (!actor.getCast().meetsHpMpDisabledConditions(_finalTarget, _skill))
//						return true;
//					break;
//			}
//		}
//		
//		return false;
//	}
}