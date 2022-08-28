package moe.knox.factorio.core.parser.api.data.desirealizer;

import com.google.gson.*;
import moe.knox.factorio.core.CoreException;
import moe.knox.factorio.core.parser.api.ParsingHelper;
import moe.knox.factorio.core.parser.api.data.ValueType;

public class ValueTypeJsonDeserializer implements JsonDeserializer<ValueType>
{
    @Override
    public ValueType deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement.isJsonPrimitive()) {
            return new ValueType.Simple(jsonElement.getAsString());
        }

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("complex_type")) {
            return deserializeComplexType(jsonElement);
        }

        return null;
    }

    private ValueType deserializeComplexType(JsonElement jsonElement)
    {
        var jsonObject = jsonElement.getAsJsonObject();
        var complexTypeNativeName = jsonObject.get("complex_type").getAsString();
        var clazz = getTypeClass(complexTypeNativeName);
        var builder = (new GsonBuilder());

        ParsingHelper.addDeserializers(builder);

        return (builder.create()).fromJson(jsonElement, clazz);
    }

    private Class<? extends ValueType> getTypeClass(String complexTypeNativeName) {
        var valueType = ValueType.TypeName.fromNativeName(complexTypeNativeName);

        return switch (valueType) {
            case FUNCTION -> ValueType.Function.class;
            case UNION -> ValueType.Union.class;
            case ARRAY -> ValueType.Array.class;
            case DICTIONARY -> ValueType.Dictionary.class;
            case TABLE -> ValueType.Table.class;
            case LITERAL -> ValueType.Literal.class;
            case STRUCT -> ValueType.Struct.class;
            case TUPLE -> ValueType.Tuple.class;
            case TYPE -> ValueType.Type.class;
            case LUA_LAZY_LOADED_VALUE -> ValueType.LuaLazyLoadedValue.class;
            case LUA_CUSTOM_TABLE -> ValueType.LuaCustomTable.class;
            default -> throw new CoreException("Unknown complex type");
        };
    }
}
