package Parser.AST.Statements;

import Lexer.Token.Token;
import Parser.AST.*;

public record ExpressionStatement(Token tok, Expression value) implements Statement {

    @Override
    public String tokenLiteral() {
        return this.tok.literal();
    }

    @Override
    public void statementNode() {}

    @Override
    public String toString() {
        if (this.value != null)
            return this.value.toString();
        else return "";
    }
}
