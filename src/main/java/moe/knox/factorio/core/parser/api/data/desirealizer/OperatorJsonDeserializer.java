package moe.knox.factorio.core.parser.api.data.desirealizer;

import com.google.gson.*;
import moe.knox.factorio.core.parser.api.data.Attribute;
import moe.knox.factorio.core.parser.api.data.Method;
import moe.knox.factorio.core.parser.api.data.Operator;

public class OperatorJsonDeserializer  implements JsonDeserializer<Operator>
{
    @Override
    public Operator deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        var jsonObject = jsonElement.getAsJsonObject();
        var operator = new Operator();
        operator.name = jsonObject.get("name").getAsString();
        Gson gson = (new GsonBuilder()).create();

        if (operator.name.equals("call")) {
            operator.method = gson.fromJson(jsonElement, Method.class);
        } else {
            operator.attribute = gson.fromJson(jsonElement, Attribute.class);
        }

        return operator;
    }
}
