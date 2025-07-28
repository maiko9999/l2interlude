package net.sf.l2j.gameserver.communitybbs.custom.model;

public class Function
{
	private String _itemType;
	private String _itemGrade;
	private String _currency;
	private String _search;
	
	private int _lastPage;
	private int _viewId;
	private int _editId;
	private int _itemId;
	
	public Function()
	{
		_itemType = "ALL";
		_itemGrade = "ALL";
		_currency = "ALL";
		_search = "";
	}
	
	public final String getItemType()
	{
		return _itemType.toUpperCase();
	}
	
	public final boolean isItemType(String type)
	{
		return _itemType.toUpperCase().equals(type.toUpperCase());
	}
	
	public void setItemType(String val)
	{
		_itemType = val;
	}
	
	public final String getItemGrade()
	{
		return _itemGrade.toUpperCase();
	}
	
	public final boolean isItemGrade(String grade)
	{
		return _itemGrade.toUpperCase().equals(grade.toUpperCase());
	}
	
	public void setItemGrade(String val)
	{
		_itemGrade = val;
	}
	
	public final String getCurrency()
	{
		return _currency;
	}
	
	public void setCurrency(String currency)
	{
		_currency = currency;
	}
	
	public final String getSearch()
	{
		return _search;
	}
	
	public void setSearch(String val)
	{
		_search = val;
	}
	
	public final int getLastPage()
	{
		return Math.clamp(_lastPage, 1, Integer.MAX_VALUE);
	}
	
	public void setLastPage(int lastPage)
	{
		_lastPage = lastPage;
	}
	
	public final int getViewId()
	{
		return _viewId;
	}
	
	public void setViewId(int viewId)
	{
		_viewId = viewId;
	}
	
	public final int getEditId()
	{
		return _editId;
	}
	
	public void setEditId(int editId)
	{
		_editId = editId;
	}
	
	public final int getItemId()
	{
		return _itemId;
	}
	
	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}
}