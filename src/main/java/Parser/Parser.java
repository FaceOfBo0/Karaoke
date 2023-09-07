package Parser;
import Parser.AST.*;
import Parser.AST.Statements.*;
import Parser.AST.Expressions.*;
import Token.*;
import Lexer.*;

import java.util.*;

enum Precedence {
    LOWEST,
    EQUALS,
    LESSGREATER,
    SUM,
    PRODUCT,
    PREFIX,
    CALL
}

interface PrefixParseFn {
    Expression parse();
}

interface InfixParseFn {
    Expression parse(Expression leftOp);
}

public class Parser {
    Lexer lex;
    Token curToken = null;
    Token peekToken = null;
    List<Token> tokens = new ArrayList<>(0);
    List<String> errors = new ArrayList<>(0);
    Map<TokenType, PrefixParseFn> prefixParseMap = new HashMap<>(0);
    Map<TokenType, InfixParseFn> infixParseMap = new HashMap<>(0);
    Map<TokenType, Precedence> precedences = new HashMap<>(Map.of(TokenType.EQ, Precedence.EQUALS,
            TokenType.BANG_EQ, Precedence.EQUALS, TokenType.GREATER, Precedence.LESSGREATER,
            TokenType.GREATER_EQ, Precedence.LESSGREATER, TokenType.LESS, Precedence.LESSGREATER,
            TokenType.LESS_EQ, Precedence.LESSGREATER, TokenType.PLUS, Precedence.SUM,
            TokenType.MINUS, Precedence.SUM, TokenType.ASTERISK, Precedence.PRODUCT,
            TokenType.SLASH, Precedence.PRODUCT));

    public Parser(Lexer pLex) {
        this.lex = pLex;
        this.precedences.put(TokenType.LPAREN, Precedence.CALL);
        this.nextToken();
        this.nextToken();
        this.initParseFns();
    }

    private void initParseFns(){
        // Identifiers & Integer Literals
        this.setPrefix(TokenType.IDENT, () -> new Identifier(this.curToken));
        this.setPrefix(TokenType.INT, () -> new IntegerLiteral(this.curToken, Long.parseLong(this.curToken.literal)));
        // Boolean Literals
        this.setPrefix(TokenType.FALSE, () -> new BooleanLiteral(this.curToken, false));
        this.setPrefix(TokenType.TRUE, () -> new BooleanLiteral(this.curToken, true));
        // Grouped Expressions
        this.setPrefix(TokenType.LPAREN, () -> {
            this.nextToken();
            Expression exp = this.parseExpression(Precedence.LOWEST);
            if (this.expectedPeekNot(TokenType.RPAREN))
                return null;
            return exp;
        });
        // Call Expression
        this.setInfix(TokenType.LPAREN, (Expression function) -> {
            CallExpression callExp = new CallExpression(this.curToken);
            callExp.setFnIdent(function);
            this.nextToken();
            callExp.setParams(this.parseCallParameters());
            return callExp;
        });
        // If Expression
        this.setPrefix(TokenType.IF, () -> {
            IfExpression ifExp = new IfExpression(this.curToken);
            if (this.expectedPeekNot(TokenType.LPAREN))
                return null;
            this.nextToken();
            ifExp.condition = this.parseExpression(Precedence.LOWEST);
            if (this.expectedPeekNot(TokenType.RPAREN))
                return null;
            if (this.expectedPeekNot(TokenType.LBRACE))
                return null;
            ifExp.consequence = this.parseBlockStatement();
            if (this.peekTokenIs(TokenType.ELSE)) {
                this.nextToken();
                if (this.expectedPeekNot(TokenType.LBRACE))
                    return null;
                ifExp.alternative = this.parseBlockStatement();
            }
            return ifExp;
        });
        // Function Expression
        this.setPrefix(TokenType.FUNC, () -> {
            FunctionLiteral fnExp = new FunctionLiteral(this.curToken);
            if (this.expectedPeekNot(TokenType.LPAREN))
                return null;
            this.nextToken();
            fnExp.setParameters(this.parseFunctionParameters());
            if (this.expectedPeekNot(TokenType.LBRACE))
                return null;
            fnExp.setBody(this.parseBlockStatement());
            return fnExp;
        });
        // Prefix Expressions
        PrefixParseFn parsePrefixExpr = () -> {
            String op = this.curToken.literal;
            this.nextToken();
            Expression right =this.parseExpression(Precedence.PREFIX);
            return new PrefixExpression(this.curToken, op, right);
        };
        this.setPrefix(TokenType.MINUS, parsePrefixExpr);
        this.setPrefix(TokenType.BANG, parsePrefixExpr);
        // Infix Expressions
        InfixParseFn parseInfixExpr = (Expression left) -> {
            String op = this.curToken.literal;
            Precedence precedence = this.curPrecedence();
            this.nextToken();
            Expression right = this.parseExpression(precedence);
            return new InfixExpression(this.curToken,left,op,right);
        };
        this.setInfix(TokenType.MINUS, parseInfixExpr);
        this.setInfix(TokenType.PLUS, parseInfixExpr);
        this.setInfix(TokenType.LESS, parseInfixExpr);
        this.setInfix(TokenType.GREATER, parseInfixExpr);
        this.setInfix(TokenType.ASTERISK, parseInfixExpr);
        this.setInfix(TokenType.SLASH, parseInfixExpr);
        this.setInfix(TokenType.EQ, parseInfixExpr);
        this.setInfix(TokenType.BANG_EQ, parseInfixExpr);
    }

    private List<Expression> parseCallParameters() {
        List<Expression> exprs = new ArrayList<>(0);
        if (this.curTokenIs(TokenType.RPAREN))
            return exprs;
        exprs.add(this.parseExpression(Precedence.LOWEST));
        while (this.peekTokenIs(TokenType.COMMA)) {
            this.nextToken();
            this.nextToken();
            exprs.add(this.parseExpression(Precedence.LOWEST));
        }
        if (this.expectedPeekNot(TokenType.RPAREN))
            return null;
        return exprs;
    }

    private List<Identifier> parseFunctionParameters() {
        List<Identifier> idents = new ArrayList<>(0);
        if (this.curTokenIs(TokenType.RPAREN))
            return idents;
        idents.add(new Identifier(this.curToken));
        while (this.peekTokenIs(TokenType.COMMA)) {
            this.nextToken();
            this.nextToken();
            idents.add(new Identifier(this.curToken));
        }
        if (this.expectedPeekNot(TokenType.RPAREN))
            return null;
        return idents;
    }

    private void setPrefix(TokenType pType, PrefixParseFn fn) {
        this.prefixParseMap.put(pType, fn);
    }

    private void setInfix(TokenType pType, InfixParseFn fn) {
        this.infixParseMap.put(pType, fn);
    }

    public Program parseProgram() {
        Program program = new Program();
        Statement statement;
        while (!this.curTokenIs(TokenType.EOF)) {
            if (this.curTokenIs(TokenType.SEMICOL)) {
                this.nextToken();
                continue;
            }
            statement = parseStatement();
            if (statement != null)
                program.getStatements().add(statement);
            this.nextToken();
        }
        return program;
    }

    private Statement parseStatement(){
        switch (this.curToken.type){
            case LET -> { return this.parseLetStatement(); }
            case RETURN -> { return this.parseReturnStatement(); }
            default -> { return this.parseExpressionStatement(); }
        }
    }

    private BlockStatement parseBlockStatement() {
        BlockStatement block = new BlockStatement(this.curToken);
        this.nextToken();
        while (!(this.curTokenIs(TokenType.RBRACE) || this.curTokenIs(TokenType.EOF))) {
            Statement stmt = this.parseStatement();
            if (stmt != null)
                block.statements.add(stmt);
            this.nextToken();
        }
        return block;
    }

    private Statement parseLetStatement() {
        LetStatement stmt = new LetStatement(this.curToken);

        if (this.expectedPeekNot(TokenType.IDENT)) {
           // System.out.println("Parser Error: " + this.errors.get(this.errors.size()-1));
            return null;
        }

        stmt.setName(new Identifier(this.curToken));

        if (this.expectedPeekNot(TokenType.ASSIGN)) {
           // System.out.println("Parser Error: " + this.errors.get(this.errors.size()-1));
            return null;
        }
        this.nextToken();

        stmt.setValue(this.parseExpression(Precedence.LOWEST));

        if(this.peekTokenIs(TokenType.SEMICOL))
            this.nextToken();

        return stmt;
    }

    private Statement parseReturnStatement() {
        ReturnStatement stmt = new ReturnStatement(curToken);
        this.nextToken();

        stmt.setValue(this.parseExpression(Precedence.LOWEST));

        if(this.peekTokenIs(TokenType.SEMICOL))
            this.nextToken();

        return stmt;
    }

    private Expression parseExpression(Precedence precedence) {
        PrefixParseFn prefix = this.prefixParseMap.get(this.curToken.type);
        if (prefix == null) {
            this.noPrefixParseFnError(this.curToken.type);
            return null;
        }
        Expression leftExp = prefix.parse();

        while(!this.peekTokenIs(TokenType.SEMICOL) && precedence.ordinal() < this.peekPrecedence().ordinal()) {
            InfixParseFn infix = this.infixParseMap.get(this.peekToken.type);
            if (infix == null)
                return null;
            this.nextToken();
            leftExp = infix.parse(leftExp);
        }
        return leftExp;
    }

    private ExpressionStatement parseExpressionStatement() {
        ExpressionStatement stmt = new ExpressionStatement(this.curToken, this.parseExpression(Precedence.LOWEST));

        if (this.peekTokenIs(TokenType.SEMICOL))
            this.nextToken();

        return stmt;
    }

    public void nextToken() {
        this.curToken = this.peekToken;
        this.peekToken = this.lex.nextToken();
        this.tokens.add(this.peekToken);
    }

    private boolean expectedPeekNot(TokenType pType) {
        if (this.peekToken.type == pType) {
            this.nextToken();
            return false;
        }
        else {
            this.peekError(pType);
            return true;
        }
    }

    private Precedence peekPrecedence() {
        return this.precedences.getOrDefault(this.peekToken.type, Precedence.LOWEST);
    }

    private Precedence curPrecedence() {
        return this.precedences.getOrDefault(this.curToken.type, Precedence.LOWEST);
    }

    private boolean peekTokenIs(TokenType pType) {
        return this.peekToken.type == pType;
    }

    private boolean curTokenIs(TokenType pType) { return this.curToken.type == pType; }

    private void noPrefixParseFnError(TokenType pTokenType) {
        this.errors.add("no prefix parse function for "+ pTokenType + "found");
    }

    private void peekError(TokenType pType) {
        this.errors.add("Parse Error: Expected next Token to be "+ pType +", got "+ this.peekToken.type +" instead!");
    }

    public List<String> getErrors(){ return this.errors; }
}
