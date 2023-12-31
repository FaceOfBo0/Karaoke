package Interpreter;

import Interpreter.ObjSystem.*;
import Parser.AST.Expressions.*;
import Parser.AST.Statements.*;
import Parser.AST.*;

import java.util.*;

public abstract class Interpreter {

    public static NullObj NULL = new NullObj();
    static BooleanObj TRUE = new BooleanObj(true);
    static BooleanObj FALSE = new BooleanObj(false);
    static Map<String, Entity> builtins = new HashMap<>(0);

    public static void init() {
        initBuiltIns();
    }

    public static Entity eval(Node pNode, Environment env) {

        // Whole program
        if (pNode.getClass() == Program.class)
            return evalProgram(((Program) pNode).getStatements(), env);

        // Expression statements
        else if (pNode.getClass() == ExpressionStatement.class)
            return eval(((ExpressionStatement) pNode).value(), env);

        // Block statements
        else if (pNode.getClass() == BlockStatement.class)
            return evalBlockStatement((BlockStatement) pNode, env);

        // Return statements
        else if (pNode.getClass() == ReturnStatement.class) {
            Entity val = eval(((ReturnStatement) pNode).value(), env);
            if (isError(val))
                return val;
            return new ReturnValue(val);
        }

        // Integer Literals
        else if (pNode.getClass() == IntegerLiteral.class)
            return new IntegerObj(((IntegerLiteral) pNode).value());

        // Boolean Literals
        else if (pNode.getClass() == BooleanLiteral.class)
            return getBoolObject(((BooleanLiteral) pNode).value());

        // String Literals
        else if (pNode.getClass() == StringLiteral.class)
            return new StringObj(((StringLiteral) pNode).value());

        // Array Literals
        else if (pNode.getClass() == ArrayLiteral.class) {
            List<Entity> elements = evalExpressionsList(((ArrayLiteral) pNode).elements(), env);
            if (elements.size() == 1 && isError(elements.get(0)))
                return elements.get(0);
            return new ArrayObj(elements);
        }

        // Map Literals
        else if (pNode.getClass() == MapLiteral.class) {
            Map<Entity, Entity> elements = evalMapPairs(((MapLiteral) pNode).pairs(), env);
            if (elements.size() == 1 && isError(elements.get(NULL)))
                return elements.get(NULL);
            return new MapObj(elements);
        }

        // Index Expressions
        else if (pNode.getClass() == IndexExpression.class) {
            Entity left = eval(((IndexExpression) pNode).left(), env);
            if (isError(left))
                return left;
            if (left.Type() != EntityType.ARRAY_OBJ && left.Type() != EntityType.MAP_OBJ)
                return newError("[] can't be used on that type - expected: ARRAY or MAP, got: %s", left.Type());
            Entity index = eval(((IndexExpression) pNode).index(), env);
            if (isError(index))
                return index;
            if(index.Type() != EntityType.INT_OBJ && left.Type() == EntityType.ARRAY_OBJ)
                return newError("Type mismatch on index value - expected: INT, got %s", index.Type());
            return evalIndexExpression(left, index);
        }

        // Prefix Expressions
        else if (pNode.getClass() == PrefixExpression.class) {
            Entity right = eval(((PrefixExpression) pNode).right(), env);
            if (isError(right))
                return right;
            return evalPrefixExpression(((PrefixExpression) pNode).op(), right);
        }
        // Infix Expressions
        else if (pNode.getClass() == InfixExpression.class) {
            Entity left = eval(((InfixExpression) pNode).left(), env);
            if (isError(left))
                return left;
            Entity right = eval(((InfixExpression) pNode).right(), env);
            if (isError(right))
                return right;
            assert left != null;
            return evalInfixExpression(((InfixExpression) pNode).op(), left, right);
        }
        // If Expressions
        else if (pNode.getClass() == IfExpression.class) {
            Entity condition = eval((((IfExpression) pNode).condition()), env);
            if (isError(condition))
                return condition;
            return evalIfExpression(condition, ((IfExpression) pNode).consequence(), ((IfExpression) pNode).alternative(), env);
        }
        // Let Statements
        else if (pNode.getClass() == LetStatement.class) {
            Entity val = eval(((LetStatement) pNode).value(), env);
            if (isError(val))
                return val;
            env.set(((LetStatement) pNode).name().value(), val);
        }
        // Identifiers
        else if (pNode.getClass() == Identifier.class)
            return evalIdentifier(pNode, env);

        // Function Literals
        else if (pNode.getClass() == FunctionLiteral.class){
            List<Identifier> params = ((FunctionLiteral) pNode).parameters();
            BlockStatement body = ((FunctionLiteral) pNode).body();
            return new FunctionObj(params, body, env);
        }
        // Call Expressions
        else if (pNode.getClass() == CallExpression.class) {
            Entity func = eval(((CallExpression) pNode).function(), env);
            if (isError(func))
                return func;
            List<Entity> args = evalExpressionsList(((CallExpression) pNode).params(), env);
            if (args.size() == 1 && isError(args.get(0)))
                return args.get(0);
            return evalFunction(func, args);
        }
        // default
        return NULL;
    }

    private static void initBuiltIns() {

        // len() for Strings and Arrays
        BuiltInFunction lenBuiltInFn = (Entity... args) -> {
            if (args.length == 1) {
                if (args[0].Type() == EntityType.STRING_OBJ)
                    return new IntegerObj(((StringObj) args[0]).value().length());
                else if (args[0].Type() == EntityType.ARRAY_OBJ)
                    return new IntegerObj(((ArrayObj) args[0]).value().size());
                else return newError("wrong type of argument for 'len'; expected: STRING, got: %s", args[0].Type());
            }
            return newError("wrong number of arguments; got: %d, want: 1",args.length);
        };
        builtins.put("len", new BuiltIn(lenBuiltInFn));

        // head() for Arrays
        BuiltInFunction headBuiltInFn = (Entity... args) -> {
            if (args.length == 1) {
                if (args[0].Type() == EntityType.ARRAY_OBJ) {
                    if (!((ArrayObj) args[0]).value().isEmpty())
                        return ((ArrayObj) args[0]).value().get(0);
                    return new ArrayObj();
                }
                else return newError("wrong type of argument for 'head'; expected: ARRAY, got: %s", args[0].Type());
            }
            return newError("wrong number of arguments - want: 1, got: %d",args.length);
        };
        builtins.put("head", new BuiltIn(headBuiltInFn));

        // tail() for Arrays
        BuiltInFunction tailBuiltInFn = (Entity... args) -> {
            if (args.length == 1) {
                if (args[0].Type() == EntityType.ARRAY_OBJ) {
                    if (!((ArrayObj) args[0]).value().isEmpty()) {
                        List<Entity> shorterArr = new ArrayList<>(((ArrayObj) args[0]).value());
                        shorterArr.remove(0);
                        return new ArrayObj(shorterArr);
                    }
                    return new ArrayObj();
                }
                else return newError("wrong type of argument for 'tail'; expected: ARRAY, got: %s", args[0].Type());
            }
            return newError("wrong number of arguments - want: 1, got: %d",args.length);
        };
        builtins.put("tail", new BuiltIn(tailBuiltInFn));

        // last() for Arrays
        BuiltInFunction lastBuiltInFn = (Entity... args) -> {
            if (args.length == 1) {
                if (args[0].Type() == EntityType.ARRAY_OBJ) {
                    if (!((ArrayObj) args[0]).value().isEmpty()) {
                        var indexLast = ((ArrayObj) args[0]).value().size() - 1;
                        return ((ArrayObj) args[0]).value().get(indexLast);
                    }
                    return new ArrayObj();
                }
                else return newError("wrong type of argument for 'last'; expected: ARRAY, got: %s", args[0].Type());
            }
            return newError("wrong number of arguments - want: 1, got: %d",args.length);
        };
        builtins.put("last", new BuiltIn(lastBuiltInFn));

        // push() for Arrays
        BuiltInFunction pushBuiltInFn = (Entity... args) -> {
            if (args.length == 2) {
                if (args[0].Type() == EntityType.ARRAY_OBJ) {
                    var maxIndex = ((ArrayObj) args[0]).value().size();
                    ((ArrayObj) args[0]).value().add(maxIndex, args[1]);
                    return args[0];
                }
                else return newError("wrong type of argument for 'push'; expected: ARRAY, got: %s", args[0].Type());
            }
            return newError("wrong number of arguments - expected: 2, got: %d",args.length);
        };
        builtins.put("push", new BuiltIn(pushBuiltInFn));
    }

    private static Entity evalIndexExpression(Entity left, Entity index) {
        if (left.Type() == EntityType.ARRAY_OBJ) {
            var indexInt = ((IntegerObj) index).value();
            var maxIndex = ((ArrayObj) left).value().size() - 1;
            if (indexInt > maxIndex)
                return newError("IndexOutOfBounds - max index: %s, got: %s", maxIndex, indexInt);
            return ((ArrayObj) left).value().get(indexInt);
        }
        else {
            var value = ((MapObj) left).value().getOrDefault(index, NULL);
            if (value == NULL)
                return newError("Map - no value found for key: %s", index.Inspect());
            return value;
        }
    }

    private static Entity evalFunction(Entity func, List<Entity> args) {
        if (func.getClass() == BuiltIn.class)
            return ((BuiltIn) func).fn().parse(args.toArray(new Entity[0]));
        else if (func.getClass() == FunctionObj.class) {
            Environment extendedEnv = extendedFunctionEnv(func, args);
            Entity evalBody = eval(((FunctionObj) func).body(), extendedEnv);
            return unwrapReturnVal(evalBody);
        }
        else return newError("not a function: %s", func.Type());
    }

    private static Entity unwrapReturnVal(Entity obj) {
        if (obj.getClass() == ReturnValue.class)
            return ((ReturnValue) obj).value();
        return obj;
    }

    private static Environment extendedFunctionEnv(Entity func, List<Entity> args) {
        assert func.getClass() == FunctionObj.class;
        EnclosedEnvironment newEnv = new EnclosedEnvironment(((FunctionObj) func).env());
        int i = 0;
        for (Identifier name: ((FunctionObj) func).parameters()) {
            // TODO: OutOfBounds error handling for arguments mismatch!
            newEnv.set(name.value(), args.get(i));
            i++;
        }
        return newEnv;
    }

    private static Map<Entity, Entity> evalMapPairs(Map<Expression, Expression> pairs, Environment env) {
        Map<Entity, Entity> elements = new HashMap<>(0);
         for (Map.Entry<Expression, Expression> entry : pairs.entrySet()) {
            Entity key = eval(entry.getKey(), env);
            if (!(key.Type() == EntityType.STRING_OBJ || key.Type() == EntityType.BOOLEAN_OBJ || key.Type() == EntityType.INT_OBJ))
                return new HashMap<>(Map.of(NULL, newError("Map: Key type mismatch - expected: STRING, BOOL or INT, got %s", key.Type())));
            Entity value = eval(entry.getValue(), env);
            if (!(value.Type() == EntityType.STRING_OBJ || value.Type() == EntityType.BOOLEAN_OBJ || value.Type() == EntityType.INT_OBJ))
                return new HashMap<>(Map.of(NULL, newError("Map: Value type mismatch - expected: STRING, BOOL or INT, got %s", value.Type())));
            elements.put(key, value);
        }
        return elements;
    }

    private static List<Entity> evalExpressionsList(List<Expression> params, Environment env) {
        List<Entity> expressions = new ArrayList<>(0);
        for (Expression arg: params) {
            Entity result = eval(arg, env);
            if (isError(result)){
                return new ArrayList<>(List.of(result));
            }
            expressions.add(result);
        }
        return expressions;
    }

    private static Entity evalIdentifier(Node pNode, Environment env) {
        Entity result = env.get(((Identifier) pNode).value());
        if (result != NULL)
            return result;
        result = builtins.getOrDefault(((Identifier) pNode).value(), NULL);
        if (result != NULL)
            return result;
        return newError("Identifier not found: %s", ((Identifier) pNode).value());
    }

    private static Entity evalBlockStatement(BlockStatement pBlock, Environment env) {
        Entity result = null;
        for (Statement stmt: pBlock.statements()) {
            result = eval(stmt, env);
            if (result != null) {
                if (result.Type() == EntityType.RETURN_VALUE_OBJ || result.Type() == EntityType.ERROR_OBJ)
                    return result;
            }
        }
        return result;
    }

    private static Entity evalIfExpression(Entity condition, BlockStatement consequence, BlockStatement alternative, Environment env) {
        if (isTruthy(condition))
            return eval(consequence, env);
        else if (alternative != null)
            return eval(alternative, env);
        else return NULL;
    }

    private static boolean isTruthy(Entity obj) {
        if (obj == TRUE)
            return true;
        return obj != FALSE && obj != NULL;
    }

    private static Entity evalInfixExpression(String op, Entity left, Entity right) {
        if (left.Type() == EntityType.INT_OBJ && right.Type() == EntityType.INT_OBJ)
            return evalIntegerInfixExpression(op, left, right);
        if (left.Type() == EntityType.STRING_OBJ && right.Type() == EntityType.STRING_OBJ)
            return evalStringInfixExpression(op, left, right);
        else if (Objects.equals(op, "!="))
            return getBoolObject(left != right);
        else if (Objects.equals(op, "=="))
            return getBoolObject(left == right);
        else if (left.Type() != right.Type())
            return newError("type mismatch: %s %s %s", left.Type(), op, right.Type());
        else
            return newError("unknown operator: %s %s %s", left.Type(), op, right.Type());
    }

    private static Entity evalStringInfixExpression(String op, Entity left, Entity right) {
        String leftVal = ((StringObj) left).value();
        String rightVal = ((StringObj) right).value();
        if (op.equals("+")) {
            return new StringObj(leftVal + rightVal);
        }
        return newError("unknown operator: %s %s %s", left.Type(), op, right.Type());
    }

    private static Entity evalIntegerInfixExpression(String op, Entity left, Entity right) {
        int leftVal = ((IntegerObj) left).value();
        int rightVal = ((IntegerObj) right).value();
        switch (op) {
            case "+" -> { return new IntegerObj(leftVal + rightVal); }
            case "-" -> { return new IntegerObj(leftVal - rightVal); }
            case "*" -> { return new IntegerObj(leftVal * rightVal); }
            case "/" -> { return new IntegerObj(leftVal / rightVal); }
            case "<" -> { return getBoolObject(leftVal < rightVal); }
            case "<=" -> { return getBoolObject(leftVal <= rightVal); }
            case ">" -> { return getBoolObject(leftVal > rightVal); }
            case ">=" -> { return getBoolObject(leftVal >= rightVal); }
            case "!=" -> { return getBoolObject(leftVal != rightVal); }
            case "==" -> { return getBoolObject(leftVal == rightVal); }
            default -> { return newError("unknown operator: %s %s %s", left.Type(), op, right.Type()); }
        }
    }

    private static Entity evalPrefixExpression(String op, Entity right) {
        switch (op) {
            case "-" -> { return evalMinusPrefixExpression(right); }
            case "!" -> { return evalBangExpression(right); }
            default -> { return newError("unknown operator: %s %s", op, right.Type()); }
        }
    }

    private static Entity evalMinusPrefixExpression(Entity right) {
        if (right.Type() != EntityType.INT_OBJ)
            return newError("unknown operator: -%s", right.Type());
        int val;
        if (right.Type() == EntityType.INT_OBJ) {
             val = ((IntegerObj) right).value();
             return new IntegerObj(-1 * val);
        }
        else return NULL;
    }

    private static Entity evalBangExpression(Entity right) {
        if (right == TRUE)
            return FALSE;
        else if (right == FALSE)
            return TRUE;
        else if (right == NULL)
            return TRUE;
        else return FALSE;
    }

    private static BooleanObj getBoolObject(boolean pValue) {
        if (pValue)
            return TRUE;
        else return FALSE;
    }

    private static Entity evalProgram(List<Statement> pStatements, Environment env) {
        Entity result = null;
        for (Statement stmt: pStatements) {
            result = eval(stmt, env);
            if (result != null) {
                if (result.getClass() == ReturnValue.class)
                    return ((ReturnValue) result).value();
                else if (result.getClass() == ErrorMsg.class)
                    return result;
            }
        }
        return result;
    }

    private static boolean isError(Entity obj) {
        if(obj != null)
            return obj.Type() == EntityType.ERROR_OBJ;
        return false;
    }

    private static ErrorMsg newError(String format, Object... entities) {
        return new ErrorMsg(String.format(format, entities));
    }
}
