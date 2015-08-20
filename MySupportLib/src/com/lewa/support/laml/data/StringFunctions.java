package com.lewa.support.laml.data;

import com.lewa.support.laml.util.Utils;


public class StringFunctions extends Expression.FunctionImpl {

    private static final String LOG_TAG = "Expression";
    private final Fun           mFun;

    private StringFunctions(Fun paramFun, int paramInt){
        super(paramInt);
        this.mFun = paramFun;
    }

    public static void load() {
        Expression.FunctionExpression.registerFunction("strToLowerCase", new StringFunctions(Fun.STR_TOLOWER, 1));
        Expression.FunctionExpression.registerFunction("strToUpperCase", new StringFunctions(Fun.STR_TOUPPER, 1));
        Expression.FunctionExpression.registerFunction("strTrim", new StringFunctions(Fun.STR_TRIM, 1));
        Expression.FunctionExpression.registerFunction("strReplace", new StringFunctions(Fun.STR_REPLACE, 3));
        Expression.FunctionExpression.registerFunction("strReplaceAll", new StringFunctions(Fun.STR_REPLACEALL, 3));
        Expression.FunctionExpression.registerFunction("strReplaceFirst", new StringFunctions(Fun.STR_REPLACEFIRST, 3));
        Expression.FunctionExpression.registerFunction("strContains", new StringFunctions(Fun.STR_CONTAINS, 2));
        Expression.FunctionExpression.registerFunction("strStartsWith", new StringFunctions(Fun.STR_STARTWITH, 2));
        Expression.FunctionExpression.registerFunction("strEndsWith", new StringFunctions(Fun.STR_ENDSWITH, 2));
        Expression.FunctionExpression.registerFunction("strIsEmpty", new StringFunctions(Fun.STR_ISEMPTY, 1));
        Expression.FunctionExpression.registerFunction("strMatches", new StringFunctions(Fun.STR_MATCHES, 2));
        Expression.FunctionExpression.registerFunction("strIndexOf", new StringFunctions(Fun.STR_INDEXOF, 2));
        Expression.FunctionExpression.registerFunction("strLastIndexOf", new StringFunctions(Fun.STR_LASTINDEXOF, 2));
    }

    public double evaluate(Expression[] mParaExps, Variables var) {
        String str1 = mParaExps[0].evaluateStr(var);
        if (str1 == null ){
            return Utils.stringToDouble(evaluateStr(mParaExps, var), 0);
        }
        String str2 = mParaExps[1].evaluateStr(var);
        switch (mFun) {
            case STR_TOLOWER:
                break;
            case STR_TOUPPER:
                break;
            case STR_TRIM:
                break;
            case STR_REPLACE:
                break;
            case STR_REPLACEALL:
                break;
            case STR_REPLACEFIRST:
                break;
            case STR_CONTAINS:
                if (str1 != null && str2 != null && str1.contains(str2))
                    return 1;
                break;
            case STR_STARTWITH:
                if (str1 != null && str2 != null && str1.startsWith(str2))
                    return 1;
            case STR_ENDSWITH:
                if (str1 != null && str2 != null && str1.endsWith(str2))
                    return 1;
                break;
            case STR_ISEMPTY:
                if(str1 != null && !str1.isEmpty())
                    return 1;
                break;
            case STR_MATCHES:
                if (str1 != null && str2 != null && str1.matches(str2) )
                  return 1;
                break;
            case STR_INDEXOF:
                if (str1 != null && str2 != null)
                    return str1.indexOf(str2);
                break;
            case STR_LASTINDEXOF:
                if (str1 != null && str2 != null)
                    return str1.lastIndexOf(str2);
                break;
        }
         return Utils.stringToDouble(evaluateStr(mParaExps, var), 0);
    }

    public String evaluateStr(Expression[] mParaExps, Variables var) {
        String str1 = mParaExps[0].evaluateStr(var);
        if(str1 == null){
            return Utils.doubleToString(evaluate(mParaExps, var));
        }
        String str2 = mParaExps[1].evaluateStr(var);
        String str3 = mParaExps[2].evaluateStr(var);
        switch (mFun) {
            case STR_TOLOWER:
                return str1.toLowerCase();
            case STR_TOUPPER:
                return str1.toUpperCase();
            case STR_TRIM:
                return str1.trim();
            case STR_REPLACE:
                if(str2 != null && str3 != null) {
                    return str1.replace(str2, str3);
                }
                break;
            case STR_REPLACEALL:
                if(str2 != null && str3 != null){
                    return str1.replaceAll(str2, str3);
                }
                break;
            case STR_REPLACEFIRST:
                if(str2 != null && str3 != null){
                    return str1.replaceFirst(str2, str3);
                }
                break;
            case STR_CONTAINS:
                break;
            case STR_STARTWITH:
                break;
            case STR_ENDSWITH:
                break;
            case STR_ISEMPTY:
                break;
            case STR_MATCHES:
                break;
            case STR_INDEXOF:
                break;
            case STR_LASTINDEXOF:
                break;
        }
        return  Utils.doubleToString(evaluate(mParaExps, var));
    }

    private static enum Fun {
        INVALID, STR_TOLOWER, STR_TOUPPER, STR_TRIM, STR_REPLACE, STR_REPLACEALL, STR_REPLACEFIRST, STR_CONTAINS,
        STR_STARTWITH, STR_ENDSWITH, STR_ISEMPTY, STR_MATCHES, STR_INDEXOF, STR_LASTINDEXOF

    }
}
