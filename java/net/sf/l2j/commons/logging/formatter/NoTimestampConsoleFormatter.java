package net.sf.l2j.commons.logging.formatter;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class NoTimestampConsoleFormatter extends Formatter
{
    @Override
    public String format(LogRecord record)
    {
        return record.getMessage() + System.lineSeparator();
    }
}