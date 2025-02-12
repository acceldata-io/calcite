/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql.fun;

import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.type.SqlOperandTypeChecker;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;

/**
 * The <code>TIMESTAMPDIFF</code> function, which calculates the difference
 * between two timestamps.
 *
 * <p>The SQL syntax is
 *
 * <blockquote>
 * <code>TIMESTAMPDIFF(<i>timestamp interval</i>, <i>timestamp</i>,
 * <i>timestamp</i>)</code>
 * </blockquote>
 *
 * <p>The interval time unit can one of the following literals:<ul>
 * <li>NANOSECOND (and synonym SQL_TSI_FRAC_SECOND)
 * <li>MICROSECOND (and synonyms SQL_TSI_MICROSECOND, FRAC_SECOND)
 * <li>SECOND (and synonym SQL_TSI_SECOND)
 * <li>MINUTE (and synonym  SQL_TSI_MINUTE)
 * <li>HOUR (and synonym  SQL_TSI_HOUR)
 * <li>DAY (and synonym SQL_TSI_DAY)
 * <li>WEEK (and synonym  SQL_TSI_WEEK)
 * <li>MONTH (and synonym SQL_TSI_MONTH)
 * <li>QUARTER (and synonym SQL_TSI_QUARTER)
 * <li>YEAR (and synonym  SQL_TSI_YEAR)
 * </ul>
 *
 * <p>Returns difference between two timestamps in indicated timestamp
 * interval.
 */
class SqlTimestampDiffFunction extends SqlFunction {
  private static RelDataType inferReturnType2(SqlOperatorBinding opBinding) {
    final RelDataTypeFactory typeFactory = opBinding.getTypeFactory();
    TimeUnit timeUnit;
    RelDataType type1;
    RelDataType type2;
    if (opBinding.isOperandLiteral(0, true)) {
      type1 = opBinding.getOperandType(0);
      type2 = opBinding.getOperandType(1);
      timeUnit = opBinding.getOperandLiteralValue(2, TimeUnit.class);
    } else {
      timeUnit = opBinding.getOperandLiteralValue(0, TimeUnit.class);
      type1 = opBinding.getOperandType(1);
      type2 = opBinding.getOperandType(2);
    }
    SqlTypeName sqlTypeName =
        timeUnit == TimeUnit.NANOSECOND
            ? SqlTypeName.BIGINT
            : SqlTypeName.INTEGER;
    return typeFactory.createTypeWithNullability(
        typeFactory.createSqlType(sqlTypeName),
        type1.isNullable()
            || type2.isNullable());
  }

  /** Creates a SqlTimestampDiffFunction. */
  SqlTimestampDiffFunction(String name, SqlOperandTypeChecker operandTypeChecker) {
    super(name, SqlKind.TIMESTAMP_DIFF,
        SqlTimestampDiffFunction::inferReturnType2, null,
        operandTypeChecker,
        SqlFunctionCategory.TIMEDATE);
  }

  @Override public void validateCall(SqlCall call, SqlValidator validator,
      SqlValidatorScope scope, SqlValidatorScope operandScope) {
    super.validateCall(call, validator, scope, operandScope);

    // This is either a time unit or a time frame:
    //
    //  * In "TIMESTAMPDIFF(YEAR, timestamp1, timestamp2)" operand 0 is a SqlIntervalQualifier
    //    with startUnit = YEAR and timeFrameName = null. The same is true for BigQuery's
    //    TIMESTAMP_DIFF() however the SqlIntervalQualifier is operand 2 due to differing
    //    parameter orders.
    //
    //  * In "TIMESTAMP_ADD(MINUTE15, timestamp1, timestamp2) operand 0 is a SqlIntervalQualifier
    //    with startUnit = EPOCH and timeFrameName = 'MINUTE15'. As above, for BigQuery's
    //    TIMESTAMP_DIFF() the SqlIntervalQualifier is found in operand 2 instead.
    //
    // If the latter, check that timeFrameName is valid.
    if (call.operand(2) instanceof SqlIntervalQualifier) {
      validator.validateTimeFrame((SqlIntervalQualifier) call.operand(2));
    } else {
      validator.validateTimeFrame((SqlIntervalQualifier) call.operand(0));
    }
  }
}
