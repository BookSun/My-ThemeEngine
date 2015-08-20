
package com.lewa.support.laml.util;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.IllegalFormatException;

import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Variables;

import org.w3c.dom.Element;

public class TextFormatter {

    private static final String LOG_TAG = "TextFormatter";

    private String mFormat;

    private Expression mFormatExpression;

    private Variable mFormatVar;

    private IndexedStringVariable mIndexedFormatVar;

    private IndexedStringVariable mIndexedTextVar;

    private FormatPara mParas[];

    private Object mParasValue[];

    private String mText;

    private Expression mTextExpression;

    private Variable mTextVar;

    public TextFormatter(String text) {
        this(text, "", "");
    }

    public TextFormatter(String text, Expression textExp) {
        this(text, "", "", textExp, null);
    }

    public TextFormatter(String format, String paras) {
        this("", format, paras);
    }

    public TextFormatter(String text, String format, String paras) {
        mText = text;
        if (mText.startsWith("@")) {
            mText = mText.substring(1);
            if (!mText.startsWith("@")) {
                mTextVar = new Variable(mText);
                mText = "";
            }
        }
        mFormat = format;
        if (mFormat.startsWith("@")) {
            mFormat = mFormat.substring(1);
            if (!mFormat.startsWith("@")) {
                mFormatVar = new Variable(mFormat);
                mFormat = "";
            }
        }
        if (!TextUtils.isEmpty(paras)) {
            mParas = FormatPara.buildArray(paras);
            if (mParas != null)
                mParasValue = new Object[mParas.length];
        }
    }

    public TextFormatter(String text, String format, String paras, Expression textExp,
            Expression formatExp) {
        this(text, format, paras);
        mTextExpression = textExp;
        mFormatExpression = formatExp;
    }

    public static TextFormatter fromElement(Element e) {
        return new TextFormatter(e.getAttribute("text"), e.getAttribute("format"),
                e.getAttribute("paras"), Expression.build(e.getAttribute("textExp")),
                Expression.build(e.getAttribute("formatExp")));
    }

    public static TextFormatter fromElement(Element e, String textAttr, String formatAttr,
            String parasAttr, String textExpAttr, String formatExpAttr) {
        return new TextFormatter(e.getAttribute(textAttr), e.getAttribute(formatAttr),
                e.getAttribute(parasAttr), Expression.build(e.getAttribute(textExpAttr)),
                Expression.build(e.getAttribute(formatExpAttr)));
    }

    public String getFormat(Variables v) {
        if (mFormatExpression != null)
            return mFormatExpression.evaluateStr(v);
        if (mFormatVar != null) {
            if (mIndexedFormatVar == null)
                mIndexedFormatVar = new IndexedStringVariable(mFormatVar.getObjName(),
                        mFormatVar.getPropertyName(), v);
            return mIndexedFormatVar.get();
        } else {
            return mFormat;
        }
    }

    public String getText(Variables v) {
        if (mTextExpression != null)
            return mTextExpression.evaluateStr(v);
        String format = getFormat(v);
        if (!TextUtils.isEmpty(format) && mParas != null) {
            for (int i = 0; i < mParas.length; i++)
                mParasValue[i] = mParas[i].evaluate(v);
            try {
                return String.format(format, mParasValue);
            } catch (IllegalFormatException e) {
                return "Format error: " + format;
            }
        }
        if (mTextVar != null) {
            if (mIndexedTextVar == null)
                mIndexedTextVar = new IndexedStringVariable(mTextVar.getObjName(),
                        mTextVar.getPropertyName(), v);
            return mIndexedTextVar.get();
        } else {
            return mText;
        }
    }

    public boolean hasFormat() {
        return !TextUtils.isEmpty(mFormat) || mFormatVar != null;
    }

    public void setText(String text) {
        mText = text;
        mFormat = "";
    }

    private static class ExpressioPara extends FormatPara {

        private Expression mExp;

        public Object evaluate(Variables var) {
            return (long) mExp.evaluate(var);
        }

        public ExpressioPara(Expression exp) {
            mExp = exp;
        }
    }

    private static abstract class FormatPara {

        public static FormatPara build(String para) {
            String exp = para.trim();
            if (exp.startsWith("@"))
                return new StringVarPara(new Variable(exp.substring(1)));
            Expression expression = Expression.build(exp);
            if (expression == null) {
                Log.e(LOG_TAG, "invalid parameter expression:" + para);
                return null;
            } else {
                return new ExpressioPara(expression);
            }
        }

        public static FormatPara[] buildArray(String exp) {
            int bracketCount = 0;
            int start = 0;
            ArrayList<FormatPara> exps = new ArrayList<FormatPara>();
            for (int i = 0, N = exp.length(); i < N; i++) {
                char c = exp.charAt(i);
                if (bracketCount == 0 && c == ',') {
                    FormatPara para = build(exp.substring(start, i));
                    if (para != null) {
                        exps.add(para);
                        start = i + 1;
                    }
                } else if (c == '(') {
                    bracketCount++;
                } else if (c == ')')
                    bracketCount--;
            }

            FormatPara para = build(exp.substring(start));
            if (para != null) {
                exps.add(para);
                FormatPara ret[] = new FormatPara[exps.size()];
                return exps.toArray(ret);
            }
            return null;
        }

        public abstract Object evaluate(Variables variables);

        private FormatPara() {
        }

    }

    private static class StringVarPara extends FormatPara {

        private IndexedStringVariable mVar;

        private Variable mVariable;

        public Object evaluate(Variables var) {
            if (mVar == null)
                mVar = new IndexedStringVariable(mVariable.getObjName(),
                        mVariable.getPropertyName(), var);
            String string = mVar.get();
            if (string == null)
                string = "";
            return string;
        }

        public StringVarPara(Variable v) {
            mVariable = v;
        }
    }
}
