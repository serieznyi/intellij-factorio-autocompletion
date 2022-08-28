package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import moe.knox.factorio.core.parser.api.data.desirealizer.ValueTypeJsonDeserializer;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

@JsonAdapter(ValueTypeJsonDeserializer.class)
public interface ValueType extends Arrangeable {
    enum TypeName {
        /**
         * PRIMITIVE TYPES
         */
        SIMPLE("simple"),
        /**
         * COMPLEX TYPES
         */
        FUNCTION("function"),
        UNION("union"),
        ARRAY("array"),
        DICTIONARY("dictionary"),
        TABLE("table"),
        LITERAL("literal"),
        STRUCT("struct"),
        TUPLE("tuple"),
        TYPE("type"),
        LUA_LAZY_LOADED_VALUE("LuaLazyLoadedValue"),
        LUA_CUSTOM_TABLE("LuaCustomTable"),
        ;

        private final String nativeName;

        TypeName(String nativeName) {
            this.nativeName = nativeName;
        }

        public String getNativeName() {
            return nativeName;
        }

        public static TypeName fromNativeName(String nativeName) {
            for (TypeName b : TypeName.values()) {
                if (b.nativeName.equals(nativeName)) {
                    return b;
                }
            }

            return null;
        }
    }

    default String getDescription() { return ""; };

    @Override
    default void arrangeElements() {}

    TypeName getName();

    record Simple(String value) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.SIMPLE;
        }
    }

    record Array(ValueType value) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.ARRAY;
        }
    }

    record Literal(String value, String description) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.LITERAL;
        }

        public String getDescription() { return description; }
    }

    record Type(String value, String description) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.TYPE;
        }

        public String getDescription() { return description; }
    }

    record Function(List<ValueType> parameters) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.FUNCTION;
        }
    }

    record Tuple(List<TypeTupleParameter> parameters) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.TUPLE;
        }

        public record TypeTupleParameter(String name, int order, String description, ValueType type, boolean optional) {
        }
    }

    record Struct(List<StructAttribute> attributes) implements ValueType {
        public record StructAttribute(
                String name,
                int order,
                String description,
                ValueType type,
                boolean optional,
                boolean read,
                boolean write
        ) {
        }

        @Override
        public TypeName getName() {
            return TypeName.STRUCT;
        }

        @Override
        public void arrangeElements() {
            attributes.sort(Comparator.comparingDouble(attribute -> attribute.order));
        }
    }

    record Union(List<ValueType> options, @SerializedName("full_format") boolean fullFormat) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.UNION;
        }
    }

    record Dictionary(ValueType key, ValueType value) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.DICTIONARY;
        }
    }

    record LuaCustomTable(ValueType key, ValueType value) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.LUA_CUSTOM_TABLE;
        }
    }

    record Table(
            List<Parameter> parameters,

            @Nullable
            @SerializedName("variant_parameter_groups")
            List<ParameterGroup> variantParameterGroups,

            @SerializedName("variant_parameter_description")
            @Nullable
            String variantParameterDescription
    ) implements ValueType {

        @Override
        public TypeName getName() {
            return TypeName.TABLE;
        }

        public String getDescription() { return variantParameterDescription; }

        @Override
        public void arrangeElements() {
            if (variantParameterGroups != null && !variantParameterGroups.isEmpty()) {
                variantParameterGroups.sort(Comparator.comparingDouble(v -> v.order));
                variantParameterGroups.forEach(ParameterGroup::arrangeElements);
            }

            parameters.sort(Comparator.comparingDouble(v -> v.order));
        }
    }

    record LuaLazyLoadedValue(ValueType value) implements ValueType {
        @Override
        public TypeName getName() {
            return TypeName.LUA_LAZY_LOADED_VALUE;
        }
    }
}
