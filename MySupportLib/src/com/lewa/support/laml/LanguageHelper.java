
package com.lewa.support.laml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.os.MemoryFile;
import android.util.Log;

import java.io.InputStream;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.util.IndexedStringVariable;
import com.lewa.support.laml.util.Utils;

public class LanguageHelper {

    private static final String DEFAULT_STRING_FILE_PATH = "strings/strings.xml";

    private static final String LOG_TAG = "LanguageHelper";

    private static final String STRING_FILE_PATH = DEFAULT_STRING_FILE_PATH;

    private static final String STRING_ROOT_TAG = "strings";

    private static final String RESOURCE_ROOT_TAG = "resources";

    private static final String STRING_TAG = "string";

    public static boolean load(Locale locale, ResourceManager resourceManager, Variables variables) {
        MemoryFile memoryfile = null;
        if (locale != null) {
            memoryfile = resourceManager.getFile(Utils.addFileNameSuffix(STRING_FILE_PATH,
                    locale.toString()));
            if (memoryfile == null)
                memoryfile = resourceManager.getFile(Utils.addFileNameSuffix(STRING_FILE_PATH,
                        locale.getLanguage()));
        }
        if (memoryfile == null) {
            memoryfile = resourceManager.getFile(STRING_FILE_PATH);
            if (memoryfile == null) {
                Log.w(LOG_TAG, "no available string resources to load.");
                return false;
            }
        }
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream is = memoryfile.getInputStream();
            Document document = builder.parse(is);
            return setVariables(document, variables);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            if(memoryfile != null)
                memoryfile.close();
        }
        return false;
    }

    private static boolean setVariables(Document doc, Variables variables) {
        NodeList rootsList = doc.getElementsByTagName(STRING_ROOT_TAG);
        boolean isResources = false ;
        if (rootsList.getLength() <= 0){
            rootsList = doc.getElementsByTagName(RESOURCE_ROOT_TAG);
            isResources = true ;
        }
        if (rootsList.getLength() <= 0){
            return false;
        }
        Element root = (Element) (rootsList.item(0));
        NodeList stringList = root.getElementsByTagName(STRING_TAG);
        for (int i = 0; i < stringList.getLength(); i++) {
            Element element = (Element) stringList.item(i);
            IndexedStringVariable stringVar = new IndexedStringVariable(element.getAttribute("name"), variables);
            String value  = null ;
            if(isResources){
                value = element.getFirstChild().getNodeValue();
            } else {
                value = element.getAttribute("value") ;
            }
            stringVar.set(value);
        }
        return true;
    }
}
