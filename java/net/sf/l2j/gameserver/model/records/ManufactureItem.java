package net.sf.l2j.gameserver.model.records;

import net.sf.l2j.gameserver.data.xml.RecipeData;

public record ManufactureItem(int recipeId, int cost, boolean isDwarven)
{
	public ManufactureItem(int recipeId, int cost)
	{
		this(recipeId, cost, RecipeData.getInstance().getRecipeList(recipeId).isDwarven());
	}
}