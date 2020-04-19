package com.pressure.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;

public class Cache {

    public static final ObjectMapper JSON_UTIL                = new ObjectMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

}
