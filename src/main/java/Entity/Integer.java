package Entity;

public class Integer implements Entity {
    long value;

    @Override
    public EntityType Type() {
        return EntityType.INT_OBJ;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String Inspect() {
        return String.valueOf(this.value);
    }
}
