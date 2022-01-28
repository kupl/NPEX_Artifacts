package org.apache.felix.framework;

import java.util.Enumeration;
import java.util.List;

import org.apache.felix.framework.cache.Content;

public class Main {
	public static void main(String[] args) {
		BundleRevisionImpl bri = new BundleRevisionImpl(null, null) {
            @Override
            synchronized List<Content> getContentPath()
            {
                return null;
            }
        };
        Enumeration<?> en = bri.getResourcesLocal("foo");
        en.hasMoreElements();
	}
}
