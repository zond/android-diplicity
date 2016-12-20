package se.oort.diplicity.apigen;

import java.util.*;
import com.google.gson.*;
import java.lang.reflect.Type;
	
public class TickerUnserializer implements JsonDeserializer<Ticker> {
  public Ticker deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return new Ticker(new Date(), json.getAsLong());
  }
}