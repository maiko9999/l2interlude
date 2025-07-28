package net.sf.l2j.gameserver.model.records.custom;

public record Item(int itemId, int min, int max, int enchantLevel, int chance)
{
}