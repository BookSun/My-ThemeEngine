
package com.lewa.support.laml.data;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.IndexedStringVariable;
import com.lewa.support.laml.util.Utils;
import com.lewa.support.laml.util.Variable;

public abstract class Expression {
    private static final boolean DEBUG = false;

    private static final boolean USE_ROOT_CACHE = false;

    private static final String LOG_TAG = "Expression";

    private static String[] mOperatorsPriority = {
            "+-", "*/%"
    };

    public String mExp;

    public static Expression build(String exp) {
        if(!USE_ROOT_CACHE) {
            return buildInner(exp);
        }
        Expression ex = buildInner(exp);
        if (ex == null) {
            return null;
        } else {
            if (DEBUG)
                ex.mExp = exp;
            return new RootExpression(ex);
        }
    }

    private static Expression buildInner(String exp) {
        if (TextUtils.isEmpty(exp.trim()))
            return null;
        Tokenizer tk = new Tokenizer(exp);
        Tokenizer.Token token = null;
        Tokenizer.Token preToken = null;
        Stack<Tokenizer.Token> opeStack = new Stack<Tokenizer.Token>();
        Stack<Expression> expStack = new Stack<Expression>();
        boolean minus = false;
        Expression newExp;
        while ((token = tk.getToken()) != null) {
            newExp = null;
            switch (token.type) {
                case VAR:
                case VARSTR:
                case NUM:
                case STR:
                case OPE:
                    switch (token.type) {
                        case VAR:
                            newExp = new NumberVariableExpression(token.token);
                            break;
                        case VARSTR:
                            newExp = new StringVariableExpression(token.token);
                            break;
                        case NUM:
                            if (minus) {
                                newExp = new NumberExpression("-" + token.token);
                                minus = false;
                            } else {
                                newExp = new NumberExpression(token.token);
                            }
                            break;
                        case STR:
                            newExp = new StringExpression(token.token);
                            break;
                        case OPE:
                            if (!token.token.equals("-")
                                    || (preToken != null && preToken.type != Tokenizer.TokenType.OPE)) {
                                while (opeStack.size() > 0
                                        && cmpOpePri(token.token, opeStack.peek().token) <= 0) {
                                    if (expStack.size() >= 2) {
                                        Expression exp2 = expStack.pop();
                                        Expression exp1 = expStack.pop();
                                        expStack.push(new BinaryExpression(exp1, exp2, opeStack
                                                .pop().token));
                                    } else {
                                        Log.e(LOG_TAG, "fail to build: invalid operation position:"
                                                + exp);
                                        return null;
                                    }
                                }
                                opeStack.push(token);
                                minus = false;
                            } else {
                                minus = true;
                            }
                            break;
                        default:
                            break;
                    }
                    if (token.type != Tokenizer.TokenType.OPE) {
                        if (minus)
                            newExp = new UnaryExpression(newExp, "-");
                        expStack.push(newExp);
                    }
                    break;
                case FUN:
                    opeStack.push(token);
                    break;
                case BRACKET:
                    newExp = buildBracket(token, opeStack);
                    if (newExp == null)
                        return null;
                    if (minus) {
                        newExp = new UnaryExpression(newExp, "-");
                    }
                    expStack.push(newExp);
                    break;
                default:
                    break;
            }
            preToken = token;
        }
        if (expStack.size() != (opeStack.size() + 1)) {
            Log.e(LOG_TAG, "failed to build: invalid expression:" + exp);
            return null;
        }
        while (opeStack.size() > 0) {
            Expression exp2 = expStack.pop();
            Expression exp1 = expStack.pop();
            newExp = new BinaryExpression(exp1, exp2, opeStack.pop().token);
            expStack.push(newExp);
        }
        return expStack.pop();
    }

    private static Expression buildBracket(Tokenizer.Token token, Stack<Tokenizer.Token> opeStack) {
        Expression[] newExps = buildMultiple(token.token);
        if (!checkParams(newExps)) {
            Log.e(LOG_TAG, "invalid expressions: " + token.token);
            return null;
        }

        try {
            if (!opeStack.isEmpty() && opeStack.peek().type == Tokenizer.TokenType.FUN)
                return new FunctionExpression(newExps, opeStack.pop().token);

            if (newExps.length == 1)
                return newExps[0];
        } catch (ScreenElementLoadException e) {
            Log.e(LOG_TAG,
                    "fail to buid: multiple expressions in brackets, but seems no function presents:"
                            + token.token);
        }
        return null;
    }

    public static Expression[] buildMultiple(String exp) {
        if(!USE_ROOT_CACHE) {
            return buildMultipleInner(exp);
        }
        Expression exps[] = buildMultipleInner(exp);
        RootExpression roots[] = new RootExpression[exps.length];
        for (int i = 0, N = exps.length; i < N; i++) {
            roots[i] = exps[i] == null ? null : new RootExpression(exps[i]);
        }
        return roots;
    }

    private static Expression[] buildMultipleInner(String exp) {
        int bracketCount = 0;
        boolean inApostrophe = false;
        int start = 0;
        ArrayList<Expression> exps = new ArrayList<Expression>();
        for (int i = 0, N = exp.length(); i < N; i++) {
            char c = exp.charAt(i);
            if (!inApostrophe) {
                if (c == ',' && bracketCount == 0) {
                    exps.add(buildInner(exp.substring(start, i)));
                    start = i + 1;
                } else if (c == '(') {
                    bracketCount++;
                } else if (c == ')') {
                    bracketCount--;
                }
            }
            if (c == '\'')
                inApostrophe = !inApostrophe;
        }

        if (start < exp.length())
            exps.add(buildInner(exp.substring(start)));
        return exps.toArray(new Expression[exps.size()]);
    }

    private static boolean checkParams(Expression[] params) {
        for (int i = 0; i < params.length; i++)
            if (params[i] == null)
                return false;

        return true;
    }

    private static int cmpOpePri(String op1, String op2) {
        return getPriority(op1) - getPriority(op2);
    }

    private static int getPriority(String op) {
        for (int i = 0, N = mOperatorsPriority.length; i < N; i++) {
            if (mOperatorsPriority[i].indexOf(op) >= 0)
                return i;
        }
        return -1;
    }

    private static boolean isDigitChar(char c) {
        return c >= '0' && c <= '9' || c == '.';
    }

    private static boolean isFunctionChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static boolean isVariableChar(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_'
                || c == '.';
    }

    public abstract double evaluate(Variables var);

    public abstract void accept(ExpressionVisitor v);

    public String evaluateStr(Variables var) {
        return null;
    }

    public boolean isNull(Variables var) {
        return false;
    }

    static class BinaryExpression extends Expression {
        private static final String TAG = "BinaryExpression";

        private Expression mExp1;

        private Expression mExp2;

        private Ope mOpe = Ope.INVALID;

        public BinaryExpression(Expression exp1, Expression exp2, String op) {
            if (DEBUG)
                Log.d(TAG, "BinaryExpression()");
            mExp1 = exp1;
            mExp2 = exp2;
            mOpe = parseOperator(op);
            if (mOpe == Ope.INVALID)
                Log.e(LOG_TAG, "BinaryExpression: invalid operator:" + op);
        }

        public static Ope parseOperator(String op) {
            if (DEBUG)
                Log.d(TAG, "parseOperator(" + op + ")");

            if (op.equals("+"))
                return Ope.ADD;
            else if (op.equals("-"))
                return Ope.MIN;
            else if (op.equals("*"))
                return Ope.MUL;
            else if (op.equals("/"))
                return Ope.DIV;
            else if (op.equals("%"))
                return Ope.MOD;
            else
                return Ope.INVALID;
        }

        public static Ope parseOperator(char op) {
            if (DEBUG)
                Log.d(TAG, "parseOperator(" + op + ")");
            switch (op) {
                case '+':
                    return Ope.ADD;
                case '-':
                    return Ope.MIN;
                case '*':
                    return Ope.MUL;
                case '/':
                    return Ope.DIV;
                case '%':
                    return Ope.MOD;
                default:
                    return Ope.INVALID;
            }
        }

        public void accept(ExpressionVisitor v) {
            v.visit(this);
            mExp1.accept(v);
            mExp2.accept(v);
        }

        public double evaluate(Variables var) {
            if (DEBUG)
                Log.d(TAG, "evaluate()");
            switch (mOpe) {
                case ADD:
                    return mExp1.evaluate(var) + mExp2.evaluate(var);
                case MIN:
                    return mExp1.evaluate(var) - mExp2.evaluate(var);
                case MUL:
                    return mExp1.evaluate(var) * mExp2.evaluate(var);
                case DIV:
                    return mExp1.evaluate(var) / mExp2.evaluate(var);
                case MOD:
                    return mExp1.evaluate(var) % mExp2.evaluate(var);
                default:
                    Log.e(LOG_TAG, "fail to evaluate BinaryExpression, invalid operator");
            }
            return 0;
        }

        public String evaluateStr(Variables var) {
            if (DEBUG)
                Log.d(TAG, "evaluateStr()");
            String str1 = mExp1.evaluateStr(var);
            String str2 = mExp2.evaluateStr(var);
            switch (mOpe) {
                case ADD:
                    if (str1 == null && str2 == null)
                        break;
                    if (str1 == null)
                        break;
                    if (str2 == null)
                        return str1;
                    return str1 + str2;
                default:
                    Log.e(LOG_TAG, "fail to evaluate string BinaryExpression, invalid operator");
                    break;
            }
            return null;
        }

        public boolean isNull(Variables var) {
            if (DEBUG)
                Log.d(TAG, "isNull()");
            switch (mOpe) {
                case ADD:
                case MIN:
                    if (!mExp1.isNull(var) || !mExp2.isNull(var))
                        return false;
                case MUL:
                case DIV:
                case MOD:
                    if (!mExp1.isNull(var) && !mExp2.isNull(var))
                        return false;
                default:
                    break;
            }
            return true;
        }

        public static enum Ope {
            INVALID, ADD, MIN, MUL, DIV, MOD
        };
    }

    public static class FunctionExpression extends Expression {
        protected static HashMap<String, FunctionImpl> sFunMap = new HashMap<String, FunctionImpl>();

        private FunctionImpl mFun;

        private String mFunName;

        private Expression[] mParaExps;

        static {
            FunctionsLoader.load();
        }

        public FunctionExpression(Expression[] params, String fun)
                throws ScreenElementLoadException {
            mParaExps = params;
            mFunName = fun;
            parseFunction(fun);
        }

        private void parseFunction(String fun) throws ScreenElementLoadException {
            FunctionImpl fd = sFunMap.get(fun);
            Utils.asserts(fd != null, "invalid function:" + fun);
            mFun = fd;
            Utils.asserts(mParaExps.length >= fd.params,
                    "parameters count not matching for function: " + fun);
        }

        public static void registerFunction(String f, FunctionImpl d) {
            FunctionImpl fun = sFunMap.put(f, d);
            if (fun != null)
                Log.w(LOG_TAG, "duplicated function name registation: " + f);
        }

        public static void removeFunction(String f, FunctionImpl d) {
            sFunMap.remove(f);
        }

        public void accept(ExpressionVisitor v) {
            v.visit(this);
            for (int i = 0, N = mParaExps.length; i < N; i++)
                mParaExps[i].accept(v);
        }

        public double evaluate(Variables var) {
            return mFun.evaluate(mParaExps, var);
        }

        public String evaluateStr(Variables var) {
            return mFun.evaluateStr(mParaExps, var);
        }

        public String getFunName() {
            return mFunName;
        }
    }

    static class NumberVariableExpression extends VariableExpression {
        private static final String LOG_TAG = "NumberVariableExpression";

        private IndexedNumberVariable mIndexedVar;

        public NumberVariableExpression(String exp) {
            super(exp);
            if (DEBUG)
                Log.d(LOG_TAG, "NumberVariableExpression(" + exp + ")");
        }

        public void accept(ExpressionVisitor v) {
            v.visit(this);
        }

        public int getIndex() {
            return mIndexedVar.getIndex();
        }

        public int getVersion() {
            return mIndexedVar.getVersion();
        }

        private void ensureVar(Variables var) {
            if (DEBUG)
                Log.d(LOG_TAG, "ensureVar()");
            if (mIndexedVar == null)
                mIndexedVar = new IndexedNumberVariable(mVar.getObjName(), mVar.getPropertyName(),
                        var);
        }

        public double evaluate(Variables var) {
            if (DEBUG)
                Log.d(LOG_TAG, "evaluate()");
            ensureVar(var);
            Double value = mIndexedVar.get();
            return value == null ? 0 : value;
        }

        public String evaluateStr(Variables var) {
            if (DEBUG)
                Log.d(LOG_TAG, "evaluateStr()");
            return Utils.doubleToString(evaluate(var));
        }

        public boolean isNull(Variables var) {
            if (DEBUG)
                Log.d(LOG_TAG, "isNull()");
            ensureVar(var);
            return mIndexedVar.get() == null;
        }
    }

    public static class StringExpression extends Expression {
        private static final String LOG_TAG = "StringExpression";

        private String mValue;

        public StringExpression(String exp) {
            if (DEBUG)
                Log.d(LOG_TAG, "StringExpression(" + exp + ")");
            mValue = exp;
        }

        public void accept(ExpressionVisitor v) {
            v.visit(this);
        }

        public double evaluate(Variables var) {
            if (DEBUG)
                Log.d(LOG_TAG, "evaluate()");
            double value;
            try {
                value = Double.valueOf(Double.parseDouble(mValue)).doubleValue();
            } catch (NumberFormatException localNumberFormatException) {
                value = 0;
            }
            return value;
        }

        public String evaluateStr(Variables var) {
            if (DEBUG)
                Log.d(LOG_TAG, "evaluateStr()");
            return mValue;
        }
    }

    public static class NumberExpression extends Expression {
        private static final String TAG = "NumberExpression";

        private String mString;

        private double mValue;

        public NumberExpression(String exp) {
            if (DEBUG)
                Log.d(TAG, "NumberExpression(" + exp + ")");
            try {
                mValue = Double.parseDouble(exp);
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, "invalid NumberExpression:" + exp, e);
            }
        }

        public void accept(ExpressionVisitor v) {
            v.visit(this);
        }

        public double evaluate(Variables var) {
            if (DEBUG)
                Log.d(TAG, "evaluate()");
            return mValue;
        }

        public String evaluateStr(Variables var) {
            if (DEBUG)
                Log.d(TAG, "evaluateStr()");
            if (mString == null)
                mString = Utils.doubleToString(mValue);
            return mString;
        }

        public void setValue(double value) {
            mValue = value;
        }
    }

    private static class Tokenizer {
        private int mPos;

        private String mString;

        public static enum TokenType {
            INVALID, VAR, VARSTR, NUM, STR, OPE, FUN, BRACKET
        }

        public Tokenizer(String exp) {
            mString = exp;
            reset();
        }

        public static class Token {
            public String token;

            public TokenType type = TokenType.INVALID;

            public Token(TokenType t, String s) {
                type = t;
                token = s;
            }
        }

        public Token getToken() {
            int bracketCount = 0;
            int bracketStart = -1;

            for (int i = mPos, N = mString.length(); i < N; i++) {
                int j = 0;
                char c = mString.charAt(i);
                if (bracketCount == 0) {
                    if (c == '#' || c == '@') {
                        for (j = i + 1; j < mString.length(); j++) {
                            if (!isVariableChar(mString.charAt(j)))
                                break;
                        }
                        if (j == (i + 1)) {
                            Log.e(LOG_TAG, "invalid variable name:" + mString);
                            return null;
                        }
                        mPos = j;
                        return new Token(c == '#' ? TokenType.VAR : TokenType.VARSTR,
                                mString.substring(i + 1, j));
                    }
                    if (isDigitChar(c)) {
                        for (j = i + 1; j < mString.length(); j++) {
                            if (!isDigitChar(mString.charAt(j)))
                                break;
                        }
                        mPos = j;
                        return new Token(TokenType.NUM, mString.substring(i, j));
                    }
                    if (isFunctionChar(c)) {
                        for (j = i + 1, N = mString.length(); j < N; j++) {
                            if (!isFunctionChar(mString.charAt(j)))
                                break;
                        }
                        mPos = j;
                        return new Token(TokenType.FUN, mString.substring(i, j));
                    }
                    if (BinaryExpression.parseOperator(c) != BinaryExpression.Ope.INVALID) {
                        mPos = i + 1;
                        return new Token(TokenType.OPE, String.valueOf(c));
                    }
                    if (c == '\'') {
                        boolean slash = false;
                        for (j = i + 1, N = mString.length(); j < N; j++) {
                            char cc = mString.charAt(j);
                            if (slash || cc != '\'') {
                                if (cc == '\\')
                                    slash = true;
                                else
                                    slash = false;
                            } else {
                                mPos = j + 1;
                                return new Token(TokenType.STR, mString.substring(i + 1, j)
                                        .replace("\\\'", "\'"));
                            }
                        }
                    }
                }
                if (c == '(') {
                    if (bracketCount == 0)
                        bracketStart = i + 1;
                    bracketCount++;
                } else if (c == ')' && --bracketCount == 0) {
                    mPos = i + 1;
                    return new Token(TokenType.BRACKET, mString.substring(bracketStart, i));
                }
            }
            if (bracketCount != 0)
                Log.e(LOG_TAG, "mismatched bracket:" + mString);
            return null;
        }

        public void reset() {
            mPos = 0;
        }

    }

    static class UnaryExpression extends Expression {
        private static final String TAG = "UnaryExpression";

        private Expression mExp;

        private Ope mOpe = Ope.INVALID;

        public UnaryExpression(Expression exp, String op) {
            if (DEBUG)
                Log.d(TAG, "UnaryExpression()");
            mExp = exp;
            mOpe = parseOperator(op);
            if (mOpe == Ope.INVALID)
                Log.e(LOG_TAG, "BinaryExpression: invalid operator:" + op);
        }

        public static Ope parseOperator(String op) {
            if (DEBUG)
                Log.d(TAG, "parseOperator(" + op + ")");
            if (op.equals("-"))
                return Ope.MIN;
            return Ope.INVALID;
        }

        public void accept(ExpressionVisitor v) {
            v.visit(this);
            mExp.accept(v);
        }

        public double evaluate(Variables var) {
            if (DEBUG)
                Log.d(TAG, "evaluate()");
            switch (mOpe) {
                case MIN:
                    return (0 - mExp.evaluate(var));
                default:
                    Log.e(LOG_TAG, "fail to evalute UnaryExpression, invalid operator");
                    break;
            }
            return mExp.evaluate(var);
        }

        public String evaluateStr(Variables var) {
            if (DEBUG)
                Log.d(TAG, "evaluateStr()");
            return Utils.doubleToString(evaluate(var));
        }

        public boolean isNull(Variables var) {
            if (DEBUG)
                Log.d(TAG, "isNull()");
            return mExp.isNull(var);
        }

        public static enum Ope {
            INVALID, MIN
        }
    }

    static class StringVariableExpression extends VariableExpression {
        private static final String TAG = "StringVariableExpression";

        private IndexedStringVariable mIndexedVar;

        public StringVariableExpression(String exp) {
            super(exp);
            if (DEBUG)
                Log.d(TAG, "StringVariableExpression(" + exp + ")");
        }

        public void accept(ExpressionVisitor v) {
            v.visit(this);
        }

        private void ensureVar(Variables var) {
            if (DEBUG)
                Log.d(TAG, "ensureVar()");
            if (mIndexedVar == null)
                mIndexedVar = new IndexedStringVariable(mVar.getObjName(), mVar.getPropertyName(),
                        var);
        }

        public double evaluate(Variables var) {
            if (DEBUG)
                Log.d(TAG, "evaluate()");
            String str = evaluateStr(var);
            if (str == null)
                return 0;
            if (str != null)
                try {
                    return Double.valueOf(Double.parseDouble(str)).doubleValue();
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.toString());
                }
            return 0;
        }

        public String evaluateStr(Variables var) {
            if (DEBUG)
                Log.d(TAG, "evaluateStr()");
            ensureVar(var);
            return mIndexedVar.get();
        }

        public int getIndex() {
            return mIndexedVar.getIndex();
        }

        public int getVersion() {
            return mIndexedVar.getVersion();
        }

        public boolean isNull(Variables var) {
            if (DEBUG)
                Log.d(TAG, "isNull()");
            ensureVar(var);
            return mIndexedVar.get() == null;
        }
    }

    private static abstract class VariableExpression extends Expression {
        protected Variable mVar;

        public VariableExpression(String exp) {
            mVar = new Variable(exp);
        }
    }

    public static abstract class FunctionImpl {
        public int params;

        public FunctionImpl(int p) {
            params = p;
        }

        public abstract double evaluate(Expression[] params, Variables var);

        public abstract String evaluateStr(Expression[] params, Variables var);
    }
}
