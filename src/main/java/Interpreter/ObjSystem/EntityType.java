package Interpreter.ObjSystem;

public enum EntityType {
    INT_OBJ,
    BOOLEAN_OBJ,
    NULL_OBJ,
    RETURN_VALUE_OBJ,
    ERROR_OBJ,
    FUNCTION_OBJ,
    STRING_OBJ,
    BUILTIN_OBJ,
    ARRAY_OBJ,
    MAP_OBJ;

    @Override
    public String toString() {
        switch (this) {
            case ERROR_OBJ -> { return "ERROR"; }
            case INT_OBJ -> { return "INTEGER"; }
            case BOOLEAN_OBJ -> { return "BOOLEAN"; }
            case RETURN_VALUE_OBJ -> { return "RETURN_VALUE"; }
            case NULL_OBJ -> {return "NULL";}
            case FUNCTION_OBJ -> {return "FUNCTION"; }
            case STRING_OBJ -> {return "STRING"; }
            case BUILTIN_OBJ -> {return "BUILTIN"; }
            case ARRAY_OBJ -> {return "ARRAY"; }
            case MAP_OBJ -> {return "MAP"; }
        }
        return "";
    }
};
