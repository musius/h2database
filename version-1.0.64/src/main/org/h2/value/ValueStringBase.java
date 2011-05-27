/*
 * Copyright 2004-2007 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.h2.util.MathUtils;
import org.h2.util.StringUtils;

/**
 * The base class for all ValueString* classes.
 */
abstract class ValueStringBase extends Value {
    protected final String value;

    protected ValueStringBase(String value) {
        this.value = value;
    }

    public String getSQL() {
        return StringUtils.quoteStringSQL(value);
    }

    public String getString() {
        return value;
    }

    public long getPrecision() {
        return value.length();
    }

    public Object getObject() {
        return value;
    }

    public void set(PreparedStatement prep, int parameterIndex) throws SQLException {
        prep.setString(parameterIndex, value);
    }

    public Value convertPrecision(long precision) {
        if (precision == 0 || value.length() <= precision) {
            return this;
        }
        int p = MathUtils.convertLongToInt(precision);
        return ValueString.get(value.substring(0, p));
    }

    public int getDisplaySize() {
        return value.length();
    }

    protected boolean isEqual(Value v) {
        return v instanceof ValueString && value.equals(((ValueString) v).value);
    }

}