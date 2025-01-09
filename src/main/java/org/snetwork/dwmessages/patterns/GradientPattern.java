package org.snetwork.dwmessages.patterns;

import java.awt.Color;
import java.util.regex.Matcher;
import org.snetwork.dwmessages.IridiumColorAPI;

public class GradientPattern implements Pattern {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<GRADIENT:([0-9A-Fa-f]{6})>(.*?)</GRADIENT:([0-9A-Fa-f]{6})>");

    public String process(String string) {
        String start;
        String end;
        String content;
        for(Matcher matcher = this.pattern.matcher(string); matcher.find(); string = string.replace(matcher.group(), IridiumColorAPI.color(content, new Color(Integer.parseInt(start, 16)), new Color(Integer.parseInt(end, 16))))) {
            start = matcher.group(1);
            end = matcher.group(3);
            content = matcher.group(2);
        }

        return string;
    }
}