package com.pottery.studio.common;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;

/** 把请求对象里非 null 的字段拷到已有实体上，实现 PUT 局部更新 */
public final class PartialCopy {

    private PartialCopy() {
    }

    public static void apply(Object from, Object to, String... skipFields) {
        BeanWrapper src = new BeanWrapperImpl(from);
        BeanWrapper dst = new BeanWrapperImpl(to);
        for (PropertyDescriptor pd : src.getPropertyDescriptors()) {
            if (pd.getWriteMethod() == null || pd.getReadMethod() == null) {
                continue;
            }
            String name = pd.getName();
            if ("id".equals(name) || "class".equals(name)) {
                continue;
            }
            boolean skip = false;
            for (String s : skipFields) {
                if (s.equals(name)) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            Object value = src.getPropertyValue(name);
            if (value != null && dst.isWritableProperty(name)) {
                dst.setPropertyValue(name, value);
            }
        }
    }
}
