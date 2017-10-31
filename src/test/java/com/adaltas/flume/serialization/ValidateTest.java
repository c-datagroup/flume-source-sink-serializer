package com.adaltas.flume.serialization;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidateTest {
    public static void main(String[] args) {

        Pattern pattern = Pattern.compile("^\\w+$");

        Matcher m = pattern.matcher("2aaaaaaaa_2d   2");
        if(m.find()){
            System.out.println(m.group());
        }

    }
}
