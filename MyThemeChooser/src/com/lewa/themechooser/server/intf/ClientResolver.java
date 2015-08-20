/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lewa.themechooser.server.intf;

import com.lewa.themechooser.server.intf.jsonimpl.PageDemander;
import com.lewa.themechooser.server.intf.xlsimpl.PageLocalParser;

/**
 * @author chenliang
 */
public class ClientResolver {

    public static final int XLS_IMPL = 0;
    public static final int JSON_IMPL = 1;
    public static final int DEFAULT_PAGE_SIZE = 18;
    public NetBaseParam url;
    public PageResolver pageResolver;
    int implType = -1;
    int pageSize;

    public ClientResolver(NetBaseParam url, int implType, int pageSize) {
        this.url = url;
        this.implType = implType;
        this.pageSize = pageSize;
        switch (implType) {
            case XLS_IMPL:
                pageResolver = new PageLocalParser(pageSize);
                break;
            case JSON_IMPL:
                pageResolver = new PageDemander(pageSize);
                break;
        }
        pageResolver.setClientResolver(this);
    }

    public boolean count(Object... objs) {
        return pageResolver.count(objs);
    }

    public String getUrl() {
        return url.toString();
    }

    public PageResolver getPageResolver() {
        return pageResolver;
    }
}
