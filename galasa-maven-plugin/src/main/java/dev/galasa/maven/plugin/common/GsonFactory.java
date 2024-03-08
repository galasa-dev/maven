package dev.galasa.maven.plugin.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonFactory {
    public Gson getGson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson ;
    }
}
