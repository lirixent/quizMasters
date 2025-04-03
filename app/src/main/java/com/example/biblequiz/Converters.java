package com.example.biblequiz;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;

public class Converters {
    @TypeConverter
    public static String fromMap(Map<String, String> options) {
        return options == null ? null : new Gson().toJson(options);
    }

    @TypeConverter
    public static Map<String, String> toMap(String optionsJson) {
        return optionsJson == null ? null : new Gson().fromJson(optionsJson, new TypeToken<Map<String, String>>() {}.getType());
    }
}

