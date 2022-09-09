package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.JsonAdapter;
import moe.knox.factorio.core.parser.api.data.desirealizer.OperatorJsonDeserializer;

@JsonAdapter(OperatorJsonDeserializer.class)
public class Operator implements Arrangeable {
    public String name;
    public Method method;
    public Attribute attribute;

    public void arrangeElements() {
        if (method != null) {
            method.arrangeElements();
        }

        if (attribute != null) {
            attribute.arrangeElements();
        }
    }

    public Type getType()
    {
        return Type.fromNativeName(name);
    }

    public enum Type {
        LENGTH("length"),
        INDEX("index"),
        CALL("call");

        private final String nativeName;

        Type(String nativeName) {
            this.nativeName = nativeName;
        }

        public static Type fromNativeName(String nativeName) {
            for (Type b : Type.values()) {
                if (b.nativeName.equals(nativeName)) {
                    return b;
                }
            }

            return null;
        }
    }
}
