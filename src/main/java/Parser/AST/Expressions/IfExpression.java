package Parser.AST.Expressions;

import Parser.AST.Expression;
import Parser.AST.Statements.BlockStatement;
import Token.*;

public class IfExpression implements Expression {
    Token tok;
    public Expression condition;
    public BlockStatement consequence;
    public BlockStatement alternative;

    public IfExpression(Token tok) {
        this.tok = tok;
    }

    @Override
    public void expressionNode() { }

    @Override
    public String tokenLiteral() {
        return this.tok.literal;
    }

    @Override
    public String toString() {
        String out= "If " + this.condition + " " + this.consequence;
        if (this.alternative != null)
            out += " else " + this.alternative;

        return out;
    }
}