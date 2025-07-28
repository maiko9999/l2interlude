package net.sf.l2j.gameserver.model.records.custom;

import java.util.List;

public record EventsInfo(String eventName, List<EventItem> items)
{
}