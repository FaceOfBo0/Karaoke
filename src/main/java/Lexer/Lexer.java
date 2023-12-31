package Lexer;

import Lexer.Token.Token;
import Lexer.Token.TokenType;

import java.util.*;

public class Lexer {
    private final Map<String, TokenType> keywords = new HashMap<>(Map.of("var", TokenType.VAR, "fn", TokenType.FUNC, "true", TokenType.TRUE,
            "false", TokenType.FALSE, "return", TokenType.RETURN, "if", TokenType.IF, "else", TokenType.ELSE));
    public String source;
    public char curChar;
    public int readPos = 0;
    public int curPos = 0;
    public  List<Token> tokens = new ArrayList<>(0);

    public Lexer (String source) {
        this.source = source;
        this.readChar();
    }

    private void skipWhitespaces() {
        while (this.curChar == '\n' || this.curChar == '\t' || this.curChar == '\r' || this.curChar == ' ') {
            this.readChar();
        }
    }

    private void readChar() {
        if (this.readPos >= this.source.length())
            this.curChar = 0;
        else
            this.curChar = this.source.charAt(this.readPos);
        this.curPos = this.readPos;
        this.readPos++;
    }

    private char peekChar() {
        if (this.readPos >= this.source.length())
            return 0;
        else return this.source.charAt(this.readPos);
    }

    private String readIdent() {
        int pos = this.curPos;
        while (this.isAlpha(this.curChar) || (this.isAlphaNumeric(this.curChar) && this.curPos > pos)) {
            this.readChar();
        }
        return this.source.substring(pos, this.curPos);
    }

    private String readInt() {
        int pos = this.curPos;
        while (this.isDigit(this.curChar)) {
            this.readChar();
        }
        return this.source.substring(pos, this.curPos);
    }

    private TokenType lookupIdent (String tag) {
        return keywords.getOrDefault(tag, TokenType.IDENT);
    }

    private boolean isDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }

    private boolean isAlpha(char ch) {
        return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == '_';
    }

    private boolean isAlphaNumeric(char ch) { return isAlpha(ch) || isDigit(ch); }

    public Token nextToken () {
        Token tok;
        this.skipWhitespaces();
        switch (this.curChar) {
            case '=' -> {
                if (this.peekChar() == '=') {
                    this.readChar();
                    tok = new Token(TokenType.EQ,"==");
                }
                else tok = new Token(TokenType.ASSIGN, String.valueOf(this.curChar));
            }
            case '!' -> {
                if (this.peekChar() == '=') {
                    this.readChar();
                    tok = new Token(TokenType.BANG_EQ, "!=");
                }
                else tok = new Token(TokenType.BANG, String.valueOf(this.curChar));
            }
            case '<' -> {
                if (this.peekChar() == '=') {
                    this.readChar();
                    tok = new Token(TokenType.LESS_EQ, "<=");
                }
                else tok = new Token(TokenType.LESS, String.valueOf(this.curChar));
            }
            case '>' -> {
                if (this.peekChar() == '=') {
                    this.readChar();
                    tok = new Token(TokenType.GREATER_EQ, ">=");
                }
                else tok = new Token(TokenType.GREATER, String.valueOf(this.curChar));
            }
            case '"' -> {
                StringBuilder result = new StringBuilder();
                while (this.peekChar() != '"' && this.peekChar() != 0) {
                    this.readChar();
                    result.append(this.curChar);
                }
                this.readChar();
                tok = new Token(TokenType.STRING, result.toString());
            }
            case ';' -> tok = new Token(TokenType.SEMICOL, String.valueOf(this.curChar));
            case ':' -> tok = new Token(TokenType.COLON, String.valueOf(this.curChar));
            case ',' -> tok = new Token(TokenType.COMMA, String.valueOf(this.curChar));
            case '(' -> tok = new Token(TokenType.LPAREN, String.valueOf(this.curChar));
            case ')' -> tok = new Token(TokenType.RPAREN, String.valueOf(this.curChar));
            case '{' -> tok = new Token(TokenType.LBRACE, String.valueOf(this.curChar));
            case '}' -> tok = new Token(TokenType.RBRACE, String.valueOf(this.curChar));
            case '[' -> tok = new Token(TokenType.LBRACKET, String.valueOf(this.curChar));
            case ']' -> tok = new Token(TokenType.RBRACKET, String.valueOf(this.curChar));
            case '+' -> tok = new Token(TokenType.PLUS, String.valueOf(this.curChar));
            case '-' -> tok = new Token(TokenType.MINUS, String.valueOf(this.curChar));
            case '*' -> tok = new Token(TokenType.ASTERISK, String.valueOf(this.curChar));
            case '/' -> tok = new Token(TokenType.SLASH, String.valueOf(this.curChar));
            case '~' -> tok = new Token(TokenType.TILDE, String.valueOf(this.curChar));

            case 0 -> tok = new Token(TokenType.EOF, "null");
            default -> {
                String ident;
                String num;
                if (isAlpha(this.curChar)) {
                    ident = this.readIdent();
                    TokenType lookup = this.lookupIdent(ident);
                    if (lookup == TokenType.IDENT)
                        return new Token(lookup, ident);
                    else
                        return new Token(lookup, lookup.toString().toLowerCase());
                }
                else if (isDigit(this.curChar)) {
                    num = this.readInt();
                    return new Token(TokenType.INT, num);
                }
                else tok = new Token(TokenType.ILLEGAL, String.valueOf(this.curChar));
            }
        }
        this.readChar();
        return tok;
    }
    public List<Token> tokenize() {
        Token tok = new Token(TokenType.ILLEGAL);
        while (!(tok.type() == TokenType.EOF)) {
            tok = this.nextToken();
            tokens.add(tok);
        }
        return this.tokens;
    }
}


