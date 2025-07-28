package net.sf.l2j.commons.cached;

public class CachedDataValueObject<T> extends CachedDataValue
{
	private final Converter<T> converter;
	private T value;
	
	CachedDataValueObject(String valueName, String defaultValue, int charId, Converter<T> converter)
	{
		super(valueName, defaultValue, charId);
		this.converter = converter;
	}
	
	@Override
	void load()
	{
		super.load();
		value = converter.fromString(getValue());
	}
	
	public T get()
	{
		return value;
	}
	
	public void set(T value)
	{
		this.value = value;
		setValue(converter.toString(value));
	}
	
	public interface Converter<T>
	{
		T fromString(String value);
		
		String toString(T value);
	}
}