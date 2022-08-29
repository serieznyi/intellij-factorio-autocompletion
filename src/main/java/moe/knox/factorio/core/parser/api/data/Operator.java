package moe.knox.factorio.core.parser.api.data;

public class Operator implements Arrangeable {
    /**
     * "index", "length": Attributes
     * "call": Method
     */
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

    public boolean isCall()
    {
        return name.equals("call");
    }

    public boolean isLength()
    {
        return name.equals("length");
    }

    public boolean isIndex()
    {
        return name.equals("index");
    }
}
