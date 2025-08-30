package org.projectcontrol.cli.utils;

import com.google.common.base.Splitter;
import picocli.CommandLine;

import java.util.List;

public class PicocliListConverter implements CommandLine.ITypeConverter<List<String>> {

    private static final Splitter SPLITTER = Splitter.on(',')
            .omitEmptyStrings()
            .trimResults();

    @Override
    public List<String> convert(String s) throws Exception {
        return SPLITTER.splitToList(s);
    }
}
