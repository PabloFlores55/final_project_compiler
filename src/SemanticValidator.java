import java.util.HashMap;
import java.util.Map;

public class SemanticValidator {
    private final Map<String, Map<String, LiteralType>> schema = new HashMap<String, Map<String, LiteralType>>();

    public SemanticValidator() {
        addTable("usuarios", new String[][]{{"nombre", "STRING"}, {"edad", "NUMBER"}, {"activo", "BOOLEAN"}});
        addTable("productos", new String[][]{{"nombre", "STRING"}, {"precio", "NUMBER"}});
    }

    private void addTable(String table, String[][] columns) {
        Map<String, LiteralType> cols = new HashMap<String, LiteralType>();
        for (int i = 0; i < columns.length; i++) cols.put(columns[i][0], LiteralType.valueOf(columns[i][1]));
        schema.put(table, cols);
    }

    public void validate(SelectStatement ast, ValidationResult result) {
        if (ast == null || ast.table == null) return;
        Map<String, LiteralType> table = schema.get(ast.table.toLowerCase());
        if (table == null) {
            result.diagnostics.add(new Diagnostic("SEMANTIC_UNKNOWN_TABLE", "Tabla no existe: " + ast.table, new SourceSpan(1, 1)));
            return;
        }
        for (int i = 0; i < ast.columns.size(); i++) {
            String col = ast.columns.get(i);
            if (!col.equals("*") && !table.containsKey(col.toLowerCase())) {
                result.diagnostics.add(new Diagnostic("SEMANTIC_UNKNOWN_COLUMN", "Columna no existe: " + col, new SourceSpan(1, 1)));
            }
        }
        if (ast.where != null) {
            for (int i = 0; i < ast.where.conditions.size(); i++) {
                WhereCondition cond = ast.where.conditions.get(i);
                result.traces.add("TRACE|WHERE_TYPE_CHECK|" + cond.columnSpan.format() + "|" + cond.column + "|" + cond.operator + "|" + cond.literalType);
                if (!table.containsKey(cond.column.toLowerCase())) {
                    result.diagnostics.add(new Diagnostic("SEMANTIC_UNKNOWN_WHERE_COLUMN",
                        "Columna WHERE no existe: " + cond.column, cond.columnSpan));
                } else if (table.get(cond.column.toLowerCase()) != cond.literalType) {
                    result.diagnostics.add(new Diagnostic("SEMANTIC_TYPE_MISMATCH",
                        "Tipo incompatible: " + cond.column + " es " + table.get(cond.column.toLowerCase()) + " pero se recibió " + cond.literalType, cond.columnSpan));
                }
            }
        }
    }
}
