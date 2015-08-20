/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lewa.themechooser.server.intf.jsonimpl;

import android.util.Log;

import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themechooser.server.intf.NetBaseParam;
import com.lewa.themechooser.server.intf.NetHelper;
import com.lewa.themechooser.server.intf.PageResolver;
import com.lewa.themechooser.server.intf.UrlParam;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenliang
 */
public class PageDemander extends PageResolver {
    private final static String TAG = "PageDemander";
    private final static Boolean DBG = true;

    List whole = new ArrayList(0);
    List curr = new ArrayList();

    public PageDemander(int pageSize) {
        super(pageSize);
    }

    @Override
    public List currentPage() {
        if (pageSize == -1) {
            curr = getWhole(clientResolver.url);
            return curr;
        }
        return LewaServerJsonParser.parseListThemeBase(NetHelper.getNetString(
                clientResolver.url.toString()), NetBaseParam.isPackgeResource(
                clientResolver.url.mType));
    }

    @Override
    public List nextPage() {
        pageNo++;
        curr = currentPage();
        whole.addAll(curr);
        return curr;
    }

    @Override
    public void init() {

    }

    @Override
    public void requestList() {

    }

    @Override
    public boolean count(Object... ifneeded) {
        if (DBG)
            Log.d(TAG, "========= string " + NetHelper.getNetString(((UrlParam) ifneeded[0]).toCountUrl()));
        this.totalCount = LewaServerJsonParser.parseCount(NetHelper.getNetString(((UrlParam) ifneeded[0]).toCountUrl()));
        resetTotalCount(this.totalCount);
        if (DBG) Log.d(TAG, "--- getPageCount " + this.getPageCount());
        return true;
    }

    @Override
    public List getRequestedEntities() {
        return curr;
    }

    public ThemeBase addThumbNPreviews(ThemeBase themebase, String cntStr) {

        return themebase;
    }

    @Override
    public List pretPages(int n) {
        this.clear();
        for (int i = 1; i <= n; i++) {
            this.setPageNo(i);
            this.whole.addAll(this.currentPage());
        }
        return this.whole;
    }

    @Override
    public void setPageNo(int pageNo) {
        super.setPageNo(pageNo);
        ((UrlParam) clientResolver.url).pageNo = pageNo;
    }

    private List getWhole(String url) {
        if (DBG) Log.d(TAG, "=========" + url);
        return LewaServerJsonParser.parseListThemeBase(NetHelper.getNetString(url)
                , NetBaseParam.isPackgeResource(clientResolver.url.actualtype)
                , clientResolver.url);
    }

    private List getWhole(NetBaseParam url) {
        return getWhole(url.toString());
    }

    public void clear() {
        whole.clear();
        curr.clear();
    }

    @Override
    public List getNextPageContent(int n) {
        return LewaServerJsonParser.parseListThemeBase(NetHelper.getNetString(clientResolver.url.changeString(n))
                , NetBaseParam.isPackgeResource(clientResolver.url.actualtype)
                , clientResolver.url);
    }
}
