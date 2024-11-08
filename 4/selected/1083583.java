package net.simpleframework.ado.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.simpleframework.core.ado.DataObjectException;
import net.simpleframework.core.ado.db.Column;
import net.simpleframework.core.ado.db.Table;
import net.simpleframework.core.id.ID;
import net.simpleframework.util.BeanUtils;
import net.simpleframework.util.BeanUtils.BeanInvoke;
import net.simpleframework.util.BeansOpeException;
import net.simpleframework.util.ConvertUtils;
import net.simpleframework.util.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class SQLBuilder {

    private static StringBuilder buildSelectSQL(final StringBuilder sb, final Table table, final Object[] columns) {
        sb.append("select ");
        if (columns == null || columns.length == 0) {
            sb.append("*");
        } else {
            int i = 0;
            for (final Object column : columns) {
                if (column == null) {
                    continue;
                }
                if (i++ > 0) {
                    sb.append(",");
                }
                if (column instanceof Column) {
                    final Column col = (Column) column;
                    sb.append(col.getColumnSqlName());
                    final String text = col.getColumnText();
                    if (StringUtils.hasText(text)) {
                        sb.append(" as ").append(text);
                    }
                } else {
                    sb.append(ConvertUtils.toString(column));
                }
            }
        }
        sb.append(" from ").append(table.getName());
        return sb;
    }

    private static StringBuilder buildDeleteSQL(final StringBuilder sb, final Table table) {
        return sb.append("delete from ").append(table.getName());
    }

    static StringBuilder buildUniqueColumns(final StringBuilder sb, final Table table) {
        final Object[] columns = table.getUniqueColumns();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(" and ");
            }
            sb.append(columns[i]).append("=?");
        }
        return sb;
    }

    private static String trimExpression(String expression) {
        if (expression != null) {
            expression = expression.trim();
            if (expression.toLowerCase().startsWith("where")) {
                expression = expression.substring(5).trim();
            }
        }
        return expression;
    }

    public static String getSelectUniqueSQL(final Table table, final Object[] columns) {
        final StringBuilder sb = new StringBuilder();
        buildSelectSQL(sb, table, columns);
        sb.append(" where ");
        buildUniqueColumns(sb, table);
        return sb.toString();
    }

    public static String getSelectExpressionSQL(final Table table, final Object[] columns, String expression) {
        final StringBuilder sb = new StringBuilder();
        buildSelectSQL(sb, table, columns);
        expression = trimExpression(expression);
        if (StringUtils.hasText(expression)) {
            sb.append(" where ").append(expression);
        }
        return sb.toString();
    }

    public static String getDeleteUniqueSQL(final Table table) {
        final StringBuilder sb = new StringBuilder();
        buildDeleteSQL(sb, table);
        sb.append(" where ");
        buildUniqueColumns(sb, table);
        return sb.toString();
    }

    public static String getDeleteExpressionSQL(final Table table, String expression) {
        final StringBuilder sb = new StringBuilder();
        buildDeleteSQL(sb, table);
        expression = trimExpression(expression);
        if (StringUtils.hasText(expression)) {
            sb.append(" where ").append(expression);
        }
        return sb.toString();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map toMapData(final Table table, final Object object) {
        Map data = null;
        if (object instanceof Map) {
            data = (Map) object;
        } else {
            boolean bEntity = false;
            if (object instanceof IEntityBeanAware) {
                final IEntityBeanAware entityBeanAware = (IEntityBeanAware) object;
                final Map<String, Column> columns = entityBeanAware.getTableColumnDefinition();
                if (columns != null && columns.size() > 0) {
                    bEntity = true;
                    data = new LinkedHashMap(columns.size());
                    for (final Map.Entry<String, Column> column : columns.entrySet()) {
                        final String propertyName = column.getKey();
                        Object vObject = BeanUtils.getProperty(object, propertyName);
                        if (vObject == null && ID.class.isAssignableFrom(BeanUtils.getPropertyType(object, propertyName))) {
                            vObject = table.getNullId();
                        }
                        data.put(column.getValue().getColumnName(), vObject);
                    }
                }
            }
            if (!bEntity) {
                data = BeanUtils.toMap(object, true, new BeanInvoke() {

                    @Override
                    public Object invoke(final Object bean, final Method readMethod, final Method writeMethod) {
                        try {
                            final Object vObject = readMethod.invoke(bean);
                            if (ID.class.isAssignableFrom(readMethod.getReturnType()) && vObject == null) {
                                return table.getNullId();
                            } else {
                                return vObject;
                            }
                        } catch (final Exception e) {
                            throw BeansOpeException.wrapException(e);
                        }
                    }
                });
            }
        }
        return data;
    }

    public static SQLValue getInsertSQLValue(final Table table, final Object object) {
        final StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(table.getName()).append("(");
        final Map<?, ?> data = toMapData(table, object);
        final int size = data.size();
        if (data == null || size == 0) {
            return null;
        }
        sb.append(StringUtils.join(data.keySet(), ","));
        final Object[] values = data.values().toArray();
        sb.append(") values(");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        return new SQLValue(sb.toString(), values);
    }

    public static SQLValue getUpdateSQLValue(final Table table, final Object[] columns, final Object object) {
        final Object[] uniqueColumns = table.getUniqueColumns();
        if (uniqueColumns == null || uniqueColumns.length == 0) {
            return null;
        }
        final List<Object> vl = new ArrayList<Object>();
        final StringBuilder sb = new StringBuilder();
        sb.append("update ").append(table.getName()).append(" set ");
        final Map<?, ?> data = toMapData(table, object);
        if (data == null || data.size() == 0) {
            return null;
        }
        final Collection<?> coll = (columns != null && columns.length > 0) ? Arrays.asList(columns) : data.keySet();
        int i = 0;
        for (final Object key : coll) {
            if (i++ > 0) {
                sb.append(",");
            }
            sb.append(key).append("=?");
            vl.add(data.get(key));
        }
        sb.append(" where ");
        buildUniqueColumns(sb, table);
        for (final Object column : uniqueColumns) {
            final Object value = data.get(column);
            if (value == null) {
                throw DataObjectException.wrapException("");
            }
            vl.add(value);
        }
        return new SQLValue(sb.toString(), vl.toArray());
    }
}
