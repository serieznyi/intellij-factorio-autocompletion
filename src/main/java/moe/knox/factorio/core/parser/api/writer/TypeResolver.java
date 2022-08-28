package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.core.parser.api.data.Parameter;
import moe.knox.factorio.core.parser.api.data.ValueType;

import java.util.List;

final class TypeResolver
{
    /**
     * @return String in format {@code "{["huhu"]:number, ["baum"]:string}"}
     */
    static String presentTableParams(List<Parameter> parameters) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        boolean first = true;
        for (Parameter parameter : parameters) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(",");
            }
            stringBuilder.append("[\"").append(parameter.name).append("\"]:").append(getType(parameter.type));
            if (parameter.optional) {
                stringBuilder.append("|nil");
            }
        }
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    static String getType(ValueType type) {
        if (type.getName().equals(ValueType.TypeName.SIMPLE)) {
            var simpleType = (ValueType.Simple) type;

            return simpleType.value();
        }

        return switch (type.getName()) {
            case UNION -> presentUnion((ValueType.Union) type);
            case ARRAY -> presentArray((ValueType.Array) type);
            case LUA_CUSTOM_TABLE -> luaCustomTableType((ValueType.LuaCustomTable) type);
            case DICTIONARY -> presentDictionary((ValueType.Dictionary) type);
            case FUNCTION -> presentFunction((ValueType.Function) type);
            case TABLE -> presentTable((ValueType.Table) type);
            case LUA_LAZY_LOADED_VALUE -> type.getName().getNativeName(); // TODO override `LuaLazyLoadedValue` class with generic
            default -> throw new IllegalStateException("Unexpected value: " + type.getName().getNativeName());
        };
    }

    /**
     * @return String in format {@code "table<A, B>"}
     */
    private static String luaCustomTableType(ValueType.LuaCustomTable type) {
        return "table<" + getType(type.key()) + ", " + getType(type.value()) + ">";
    }

    /**
     * @return String in format {@code "table<A, B>"}
     */
    private static String presentDictionary(ValueType.Dictionary type) {
        return "table<" + getType(type.key()) + ", " + getType(type.value()) + ">";
    }

    /**
     * @return String in format {@code "fun(param:A, param2:B):RETURN_TYPE"}
     */
    private static String presentFunction(ValueType.Function type) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fun(");
        int i = 0;
        for (ValueType parameter : type.parameters()) {
            if (i > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append("param").append(i).append(':').append(getType(parameter));
            ++i;
        }
        stringBuilder.append(")");

        return stringBuilder.toString();
    }

    /**
     * @return String in format {@code "TYPE1|TYPE2"}
     */
    private static String presentUnion(ValueType.Union type) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (ValueType option : type.options()) {
            if (!first) {
                stringBuilder.append('|');
            }
            first = false;
            stringBuilder.append(getType(option));
        }

        return stringBuilder.toString();
    }

    /**
     * @return String in format {@code "TYPE[]"}
     */
    private static String presentArray(ValueType.Array type) {
        StringBuilder stringBuilder = new StringBuilder();
        // A[]
        try {
            stringBuilder.append(getType(type.value())).append("[]");
        } catch (NullPointerException e) {
            e.printStackTrace(); // todo check it
        }

        return stringBuilder.toString();
    }

    /**
     * @return String in format {@code "{["huhu"]:number, ["baum"]:string}"}
     */
    private static String presentTable(ValueType.Table type) {
        return presentTableParams(type.parameters());
    }
}
