package Parser.AST.Expressions;

import Lexer.Token.Token;
import Parser.AST.Expression;

import java.util.List;

public record ArrayLiteral(Token tok, List<Expression> elements) implements Expression {

    @Override
    public void expressionNode() {
    }

    @Override
    public String tokenLiteral() {
        return this.tok.literal();
    }

    @Override
    public int length() {
        return this.toString().length();
    }

    @Override
    public char charAt(int index) {
        return this.toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return this.toString().subSequence(start, end);
    }

    @Override
    public String toString() {
        return "[" + String.join(", ", this.elements) + ']';
    }
}
