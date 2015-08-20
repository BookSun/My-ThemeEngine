
package com.lewa.support.laml.data;

import java.util.HashSet;

public class RootExpression extends Expression {

    public static final String LOG_TAG = "RootExression";

    private boolean mAlwaysEvaluate;

    private double mDoubleValue;

    private Expression mExp;

    private boolean mIsNumInit;

    private boolean mIsStrInit;

    private String mStringValue;

    private VarVersionVisitor mVarVersionVisitor;

    private HashSet<VarVersion> mVersionSet = new HashSet<VarVersion>();

    private VarVersion mVersions[];

    public RootExpression(Expression exp) {
        mExp = exp;
    }

    public void accept(ExpressionVisitor visitor) {
        mExp.accept(visitor);
    }

    public void addVarVersion(VarVersion version) {
        mVersionSet.add(version);
    }

    public double evaluate(Variables var) {
        if (!mIsNumInit) {
            mDoubleValue = mExp.evaluate(var);
            if (mVarVersionVisitor == null) {
                mVarVersionVisitor = new VarVersionVisitor(this, var);
                mExp.accept(mVarVersionVisitor);
                int size = mVersionSet.size();
                if (size > 0) {
                    mVersions = new VarVersion[size];
                    mVersionSet.toArray(mVersions);
                }
            }
            mIsNumInit = true;
        } else {
            boolean isChange = false;
            if (mVersions != null) {
                for (VarVersion version : mVersions) {
                    if (version != null) {
                        int newVersion = version.mType == VarVersion.TYPE_NUM ? var
                                .getNumVer(version.mIndex) : var.getStrVer(version.mIndex);
                        if (version.mVersion != newVersion) {
                            isChange = true;
                            version.mVersion = newVersion;
                        }
                    }
                }
            }
            if (isChange || mAlwaysEvaluate)
                mDoubleValue = mExp.evaluate(var);
        }
        return mDoubleValue;
    }

    public String evaluateStr(Variables var) {
        if (!mIsStrInit) {
            mStringValue = mExp.evaluateStr(var);
            if (mVarVersionVisitor == null) {
                mVarVersionVisitor = new VarVersionVisitor(this, var);
                mExp.accept(mVarVersionVisitor);
                int size = mVersionSet.size();
                if (size > 0) {
                    mVersions = new VarVersion[size];
                    mVersionSet.toArray(mVersions);
                }
            }
            mIsStrInit = true;
        } else {
            boolean isChange = false;
            if (mVersions != null) {
                for (VarVersion version : mVersions) {
                    if (version != null) {
                        int newVersion = version.mType == VarVersion.TYPE_NUM ? var
                                .getNumVer(version.mIndex) : var.getStrVer(version.mIndex);
                        if (version.mVersion != newVersion) {
                            isChange = true;
                            version.mVersion = newVersion;
                        }
                    }
                }
            }
            if (isChange || mAlwaysEvaluate)
                mStringValue = mExp.evaluateStr(var);
        }
        return mStringValue;
    }

    public boolean isNull(Variables var) {
        return mExp.isNull(var);
    }

    public static class VarVersion {
        public static final int TYPE_NUM = 1;

        public static final int TYPE_STR = 2;

        int mIndex;

        int mType;

        int mVersion;

        public boolean equals(Object version) {
            if (version instanceof VarVersion) {
                VarVersion tempVersion = (VarVersion) version;
                if (tempVersion.mIndex == mIndex && tempVersion.mType == mType) {
                    return true;
                }
            }
            return false;
        }

        public int hashCode() {
            return mType == TYPE_NUM ? mIndex : -1 - mIndex;
        }

        public VarVersion(int index, int version, int type) {
            mIndex = index;
            mVersion = version;
            mType = type;
        }
    }

    private static class VarVersionVisitor extends ExpressionVisitor {

        private RootExpression mRoot;

        private Variables mVar;

        public VarVersionVisitor(RootExpression root, Variables var) {
            mRoot = root;
            mVar = var;
        }

        public void visit(NumberVariableExpression exp) {
            exp.evaluate(mVar);
            mRoot.addVarVersion(new VarVersion(exp.getIndex(), exp.getVersion(),
                    VarVersion.TYPE_NUM));
        }

        public void visit(StringVariableExpression exp) {
            exp.evaluateStr(mVar);
            mRoot.addVarVersion(new VarVersion(exp.getIndex(), exp.getVersion(),
                    VarVersion.TYPE_STR));
        }

        public void visit(FunctionExpression exp) {
            if ("rand".equals(exp.getFunName()))
                mRoot.mAlwaysEvaluate = true;
        }
    }
}
