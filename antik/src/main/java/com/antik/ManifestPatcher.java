package com.antik;

import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ManifestPatcher {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void patch(ApkModule m) throws Exception {

        AndroidManifestBlock mf = m.getAndroidManifest();

        if (mf == null)
        {
        return;
        }
        Boolean e = mf.isExtractNativeLibs();
        if (e != null && e.booleanValue() == false) {
        mf.setExtractNativeLibs(true);
        }

        ResXmlElement m_e = mf.getManifestElement();

        if (m_e != null)
        {
            m_e.removeAttributesWithName("requiredSplitTypes");
            m_e.removeAttributesWithName("splitTypes");
        }

        List md_r = Arrays.asList(new String[]{"com.android.stamp.source", "com.android.stamp.type", "com.android.vending.splits", "com.android.vending.derived.apk.id", "com.android.dynamic.apk.fused.modules", "com.android.vending.splits.required"});

        List act_r = Arrays.asList(new String[]{"com.pairip.licensecheck.LicenseActivity"});

        List pv_r = Arrays.asList(new String[]{"com.pairip.licensecheck.LicenseContentProvider"});

        Iterator i = mf.getApplicationElementsByTag("meta-data");

        List rm_ls = new ArrayList();

        while (i.hasNext()) {

            ResXmlElement el = (ResXmlElement) i.next();

            String n = AndroidManifestBlock.getAndroidNameValue(el);

            if (md_r.contains(n)) {
                rm_ls.add(el);
            }
        }

        int x = 0;
        for (; x < rm_ls.size(); x = x + 1) {
            ResXmlElement el = (ResXmlElement) rm_ls.get(x);
            el.removeSelf();
        }

        rm_ls.clear();

        i = mf.getApplicationElementsByTag("activity");

        while (i.hasNext()) {

            ResXmlElement el = (ResXmlElement) i.next();

            String n = AndroidManifestBlock.getAndroidNameValue(el);

            if (act_r.contains(n)) {
                rm_ls.add(el);
            }
        }

        for (x = 0; x < rm_ls.size(); x = x + 1) {
            ResXmlElement el = (ResXmlElement) rm_ls.get(x);
            el.removeSelf();
        }

        rm_ls.clear();

        i = mf.getApplicationElementsByTag("provider");

        while (i.hasNext()) {

            ResXmlElement el = (ResXmlElement) i.next();

            String n = AndroidManifestBlock.getAndroidNameValue(el);

            if (pv_r.contains(n)) {
                rm_ls.add(el);
            }
        }

        for (x = 0; x < rm_ls.size(); x++) {

            ResXmlElement el = (ResXmlElement) rm_ls.get(x);
            el.removeSelf();

        }

        m.setManifest(mf);
    }
}