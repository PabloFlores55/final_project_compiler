import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int pos;
    private ValidationResult result;

    public SelectStatement parse(List<Token> tokens, ValidationResult result) {
        this.tokens = tokens;
        this.pos = 0;
        this.result = result;
        SelectStatement statement = new SelectStatement();
        expect(TokenType.SELECT, "SYNTACTIC_EXPECTED_SELECT");
        parseColumns(statement);
        expect(TokenType.FROM, "SYNTACTIC_EXPECTED_FROM");
        Token table = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_TABLE");
        if (table != null) statement.table = table.lexeme;

        if (match(TokenType.WHERE)) {
            statement.where = parseWhereChain();
        }

        if (check(TokenType.SEMICOLON)) advance();
        if (!check(TokenType.EOF)) {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_UNEXPECTED_TOKEN", "Token inesperado: " + current().lexeme, current().span));
        }
        return statement;
    }

    private ConditionChain parseWhereChain() {
        ConditionChain chain = new ConditionChain();
        WhereCondition first = parseWhereCondition();
        if (first != null) chain.conditions.add(first);
        while (check(TokenType.AND) || check(TokenType.OR)) {
            String connector = advance().lexeme.toUpperCase();
            chain.connectors.add(connector);
            WhereCondition next = parseWhereCondition();
            if (next != null) chain.conditions.add(next);
        }
        return chain;
    }

    private WhereCondition parseWhereCondition() {
        if (!check(TokenType.IDENTIFIER)) {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_EXPECTED_WHERE_OPERAND",
                "Se esperaba un identificador en WHERE", current().span));
            return null;
        }
        Token col = advance();
        Token op = expectOperator();
        if (op == null) return null;
        Token lit = parseLiteral();
        if (lit == null) {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_EXPECTED_WHERE_OPERAND",
                "Se esperaba un literal en WHERE", current().span));
            return null;
        }
        LiteralType litType = getLiteralType(lit);
        return new WhereCondition(col.lexeme, op.lexeme, lit.lexeme, litType,
            col.span, op.span, lit.span);
    }

    private Token expectOperator() {
        if (check(TokenType.EQUAL) || check(TokenType.GREATER) || check(TokenType.LESS) ||
            check(TokenType.GREATER_EQUAL) || check(TokenType.LESS_EQUAL) || check(TokenType.NOT_EQUAL)) {
            return advance();
        }
        result.diagnostics.add(new Diagnostic("SYNTACTIC_EXPECTED_OPERATOR",
            "Se esperaba un operador de comparación", current().span));
        return null;
    }

    private Token parseLiteral() {
        if (check(TokenType.NUMBER) || check(TokenType.STRING) ||
            check(TokenType.TRUE) || check(TokenType.FALSE)) {
            return advance();
        }
        return null;
    }

    private LiteralType getLiteralType(Token lit) {
        if (lit.type == TokenType.NUMBER) return LiteralType.NUMBER;
        if (lit.type == TokenType.STRING) return LiteralType.STRING;
        if (lit.type == TokenType.TRUE || lit.type == TokenType.FALSE) return LiteralType.BOOLEAN;
        return LiteralType.UNKNOWN;
    }

    private void parseColumns(SelectStatement statement) {
        if (match(TokenType.STAR)) {
            statement.columns.add("*");
            return;
        }
        Token first = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
        if (first != null) statement.columns.add(first.lexeme);
        while (match(TokenType.COMMA)) {
            Token next = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
            if (next != null) statement.columns.add(next.lexeme);
        }
    }

    private Token expect(TokenType type, String code) {
        if (check(type)) return advance();
        result.diagnostics.add(new Diagnostic(code, "Se esperaba " + type + " y se encontró " + current().type, current().span));
        return null;
    }

    private boolean match(TokenType type) { if (check(type)) { advance(); return true; } return false; }
    private boolean check(TokenType type) { return current().type == type; }
    private Token current() { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }
}
