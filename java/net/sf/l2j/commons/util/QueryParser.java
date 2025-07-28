package net.sf.l2j.commons.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;

public class QueryParser
{
	private String _input;
	
	private int _pos, _start, _end;
	
	private String image()
	{
		return _input.substring(_start, _end);
	}
	
	private int peek()
	{
		return _input.charAt(_pos);
	}
	
	private boolean eof()
	{
		return _input.length() <= _pos;
	}
	
	private void consume()
	{
		_pos++;
	}
	
	public Map<String, String> scan(String input)
	{
		_pos = _start = _end = 0;
		_input = input;
		
		final Map<String, String> props = new HashMap<>();
		props.put("$name", scanID().trim());
		if (match(QST))
		{
			while (!eof())
			{
				final String key = scanID().trim();
				if (match(EQS))
				{
					props.put(key, scanID().trim());
					if (match(AMP))
						continue;
					
					if (eof())
						break;
				}
				
				if (match(AMP) || eof())
				{
					props.put(key, "");
					continue;
				}
				break;
			}
		}
		return props;
	}
	
	private String scanID()
	{
		_start = _pos;
		while (match(CH))
		{
		}
		_end = _pos;
		return image();
	}
	
	public boolean is(IntPredicate predicate)
	{
		return !eof() && predicate.test(peek());
	}
	
	public boolean match(IntPredicate predicate)
	{
		if (is(predicate))
		{
			consume();
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public static final IntPredicate QST = ch -> ch == '?';
	public static final IntPredicate EQS = ch -> ch == '=';
	public static final IntPredicate AMP = ch -> ch == '&';
	public static final IntPredicate CH = QST.or(EQS).or(AMP).negate();
	
	public static Map<String, String> parse(String input)
	{
		return new QueryParser().scan(input);
	}
}