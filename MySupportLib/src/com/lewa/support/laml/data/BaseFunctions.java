
package com.lewa.support.laml.data;

import com.lewa.support.laml.data.Expression.FunctionExpression;
import com.lewa.support.laml.data.Expression.FunctionImpl;
import com.lewa.support.laml.util.Utils;

import android.text.TextUtils;
import android.util.Log;

public class BaseFunctions extends FunctionImpl {

    private static final String LOG_TAG = "BaseFunctions";

    private final Fun mFun;

    private BaseFunctions(Fun fun, int i) {
        super(i);
        mFun = fun;
    }

    private int digit(int number, int n) {
        if (n > 0) {
            if (number == 0 && n == 1)
                return 0;

            for (int i = 0; number > 0 && i < n - 1; i++)
                number /= 10;

            if (number > 0)
                return number % 10;
        }
        return -1;
    }

    public static void load() {
        FunctionExpression.registerFunction("rand", new BaseFunctions(Fun.RAND, 1));
        FunctionExpression.registerFunction("sin", new BaseFunctions(Fun.SIN, 1));
        FunctionExpression.registerFunction("cos", new BaseFunctions(Fun.COS, 1));
        FunctionExpression.registerFunction("tan", new BaseFunctions(Fun.TAN, 1));
        FunctionExpression.registerFunction("asin", new BaseFunctions(Fun.ASIN, 1));
        FunctionExpression.registerFunction("acos", new BaseFunctions(Fun.ACOS, 1));
        FunctionExpression.registerFunction("atan", new BaseFunctions(Fun.ATAN, 1));
        FunctionExpression.registerFunction("sinh", new BaseFunctions(Fun.SINH, 1));
        FunctionExpression.registerFunction("cosh", new BaseFunctions(Fun.COSH, 1));
        FunctionExpression.registerFunction("sqrt", new BaseFunctions(Fun.SQRT, 1));
        FunctionExpression.registerFunction("abs", new BaseFunctions(Fun.ABS, 1));
        FunctionExpression.registerFunction("len", new BaseFunctions(Fun.LEN, 1));
        FunctionExpression.registerFunction("round", new BaseFunctions(Fun.ROUND, 1));
        FunctionExpression.registerFunction("int", new BaseFunctions(Fun.INT, 1));
        FunctionExpression.registerFunction("isnull", new BaseFunctions(Fun.ISNULL, 1));
        FunctionExpression.registerFunction("not", new BaseFunctions(Fun.NOT, 1));
        
        FunctionExpression.registerFunction("min", new BaseFunctions(Fun.MIN, 2));
        FunctionExpression.registerFunction("max", new BaseFunctions(Fun.MAX, 2));
        FunctionExpression.registerFunction("digit", new BaseFunctions(Fun.DIGIT, 2));
        FunctionExpression.registerFunction("eq", new BaseFunctions(Fun.EQ, 2));
        FunctionExpression.registerFunction("ne", new BaseFunctions(Fun.NE, 2));
        FunctionExpression.registerFunction("ge", new BaseFunctions(Fun.GE, 2));
        FunctionExpression.registerFunction("gt", new BaseFunctions(Fun.GT, 2));
        FunctionExpression.registerFunction("le", new BaseFunctions(Fun.LE, 2));
        FunctionExpression.registerFunction("lt", new BaseFunctions(Fun.LT, 2));
        FunctionExpression.registerFunction("ifelse", new BaseFunctions(Fun.IFELSE, 3));
        FunctionExpression.registerFunction("eqs", new BaseFunctions(Fun.EQS, 2));
        FunctionExpression.registerFunction("substr", new BaseFunctions(Fun.SUBSTR, 2));
        FunctionExpression.registerFunction("replace", new BaseFunctions(Fun.REPLACE, 3));
    }

    @Override
    public double evaluate(Expression[] mParaExps, Variables var) {
        switch (mFun) {
            case RAND:
                return Math.random();
            case SIN:
                return Math.sin(mParaExps[0].evaluate(var));
            case COS:
                return Math.cos(mParaExps[0].evaluate(var));
            case TAN:
                return Math.tan(mParaExps[0].evaluate(var));
            case ASIN:
                return Math.asin(mParaExps[0].evaluate(var));
            case ACOS:
                return Math.acos(mParaExps[0].evaluate(var));
            case ATAN:
                return Math.atan(mParaExps[0].evaluate(var));
            case SINH:
                return Math.sinh(mParaExps[0].evaluate(var));
            case COSH:
                return Math.cosh(mParaExps[0].evaluate(var));
            case SQRT:
                return Math.sqrt(mParaExps[0].evaluate(var));
            case ABS:
                return Math.abs(mParaExps[0].evaluate(var));
            case LEN:
                String temp = mParaExps[0].evaluateStr(var);
                if (temp != null) {
                    return temp.length();
                } else {
                    Log.e(LOG_TAG, "ERROR! BaseFunctions.evaluate() function LEN :" + mParaExps[0]);
                }
                return 0;
            case ROUND:
                return Math.round(mParaExps[0].evaluate(var));
            case INT:
                return (int) mParaExps[0].evaluate(var);
            case MIN:
                return Math.min(mParaExps[0].evaluate(var), mParaExps[1].evaluate(var));
            case MAX:
                return Math.max(mParaExps[0].evaluate(var), mParaExps[1].evaluate(var));
            case DIGIT:
                return digit((int) mParaExps[0].evaluate(var), (int) mParaExps[1].evaluate(var));
            case EQ:
                return mParaExps[0].evaluate(var) == mParaExps[1].evaluate(var) ? 1 : 0;
            case NE:
                return mParaExps[0].evaluate(var) != mParaExps[1].evaluate(var) ? 1 : 0;
            case GE:
                return mParaExps[0].evaluate(var) >= mParaExps[1].evaluate(var) ? 1 : 0;
            case GT:
                return mParaExps[0].evaluate(var) > mParaExps[1].evaluate(var) ? 1 : 0;
            case LE:
                return mParaExps[0].evaluate(var) <= mParaExps[1].evaluate(var) ? 1 : 0;
            case LT:
                return mParaExps[0].evaluate(var) < mParaExps[1].evaluate(var) ? 1 : 0;
            case ISNULL:
                return mParaExps[0].isNull(var) ? 1 : 0;
            case NOT:
                return mParaExps[0].evaluate(var) <= 0 ? 1 : 0;
            case IFELSE:
                int len = mParaExps.length;
                if (len % 2 != 1) {
                    Log.e(LOG_TAG, "function parameter number should be 2*n+1: " + mFun.toString());
                } else {
                    for (int i = 0; i < (len - 1) / 2; i++)
                        if (mParaExps[i * 2].evaluate(var) > 0)
                            return mParaExps[i * 2 + 1].evaluate(var);
                    return mParaExps[len - 1].evaluate(var);
                }
                break;
            case EQS:
                return TextUtils.equals(mParaExps[0].evaluateStr(var), mParaExps[1].evaluateStr(var)) ? 1 : 0;
            case SUBSTR:
                return Utils.stringToDouble(evaluateStr(mParaExps, var), 0);
            case REPLACE:
                return Utils.stringToDouble(evaluateStr(mParaExps, var), 0);
            default:
                Log.e(LOG_TAG, "fail to evalute FunctionExpression, invalid function: ");
                break;
        }
        return 0;
    }

    @Override
    public String evaluateStr(Expression[] mParaExps, Variables var) {
        String str = null;
        int length = mParaExps.length;
        switch (mFun) {
            case IFELSE:
                if (length % 2 != 1) {
                    Log.e(LOG_TAG, "function parameter number should be 2*n+1: " + mFun.toString());
                } else {
                    for(int i = 0; i < (length - 1) / 2; i++)
                        if(mParaExps[i * 2].evaluate(var) > 0)
                            return mParaExps[1 + i * 2].evaluateStr(var);
                    return mParaExps[length - 1].evaluateStr(var);
                }
                break;
            case SUBSTR:
                String substr = mParaExps[0].evaluateStr(var);
                if (substr != null) {
                    try {
                        int index = (int) mParaExps[1].evaluate(var);
                        if (mParaExps.length >= 3) {
                            str = substr.substring(index, index + (int) mParaExps[2].evaluate(var));
                        } else {
                            str = substr.substring(index);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }
                break;
            case REPLACE:
                str = mParaExps[0].evaluateStr(var);
                if (str != null && mParaExps.length == 3) {
                    try {
                        String oldChar = mParaExps[1].evaluateStr(var);
                        String newChar = mParaExps[2].evaluateStr(var);
                        if(oldChar != null) {
                            if(newChar == null)
                                newChar = "";
                            str = str.replace(oldChar, newChar);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }
                break;
            default:
                str = Utils.doubleToString(evaluate(mParaExps, var));
                break;
        }

        return str;
    }
    
    @Override
    public String toString() {
        return mFun == null ? null : mFun.name();
    }

    static enum Fun {
        INVALID, RAND, SIN, COS, TAN, ASIN, ACOS, ATAN, SINH, COSH, SQRT, ABS, LEN, ROUND, INT, MIN, MAX, DIGIT, EQ, NE, GE, GT, LE, LT, ISNULL, NOT, IFELSE, EQS, SUBSTR, REPLACE
    }
}
