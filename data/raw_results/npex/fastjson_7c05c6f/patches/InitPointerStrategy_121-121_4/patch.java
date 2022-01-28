package com.alibaba.fastjson.parser.deserializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.DefaultJSONParser.ResolveTask;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.ParseContext;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.FieldInfo;

public class DefaultFieldDeserializer extends FieldDeserializer {

    protected ObjectDeserializer fieldValueDeserilizer;
    protected boolean            customDeserilizer     = false;

    public DefaultFieldDeserializer(ParserConfig config, Class<?> clazz, FieldInfo fieldInfo){
        super(clazz, fieldInfo);
        JSONField annotation = fieldInfo.getAnnotation();
        if (annotation != null) {
            Class<?> deserializeUsing = annotation.deserializeUsing();
            customDeserilizer = deserializeUsing != null && deserializeUsing != Void.class;
        }
    }

    public ObjectDeserializer getFieldValueDeserilizer(ParserConfig config) {
        if (fieldValueDeserilizer == null) {
            JSONField annotation = fieldInfo.getAnnotation();
            if (annotation != null && annotation.deserializeUsing() != Void.class) {
                Class<?> deserializeUsing = annotation.deserializeUsing();
                try {
                    fieldValueDeserilizer = (ObjectDeserializer) deserializeUsing.newInstance();
                } catch (Exception ex) {
                    throw new JSONException("create deserializeUsing ObjectDeserializer error", ex);
                }
            } else {
                fieldValueDeserilizer = config.getDeserializer(fieldInfo.fieldClass, fieldInfo.fieldType);
            }
        }

        return fieldValueDeserilizer;
    }

    @Override
public void parseField(com.alibaba.fastjson.parser.DefaultJSONParser parser, java.lang.Object object, java.lang.reflect.Type objectType, java.util.Map<java.lang.String, java.lang.Object> fieldValues) {
    if (this.fieldValueDeserilizer == null) {
        getFieldValueDeserilizer(parser.getConfig());
    }
    com.alibaba.fastjson.parser.deserializer.ObjectDeserializer fieldValueDeserilizer = this.fieldValueDeserilizer;
    java.lang.reflect.Type fieldType = fieldInfo.fieldType;
    if (objectType instanceof java.lang.reflect.ParameterizedType) {
        com.alibaba.fastjson.parser.ParseContext objContext = parser.getContext();
        if (objContext != null) {
            objContext.type = objectType;
        }
        if (fieldType != objectType) {
            fieldType = com.alibaba.fastjson.util.FieldInfo.getFieldType(this.clazz, objectType, fieldType);
            fieldValueDeserilizer = parser.getConfig().getDeserializer(fieldType);
        }
    }
    // ContextObjectDeserializer
    java.lang.Object value;
    if ((fieldValueDeserilizer instanceof com.alibaba.fastjson.parser.deserializer.JavaBeanDeserializer) && (fieldInfo.parserFeatures != 0)) {
        com.alibaba.fastjson.parser.deserializer.JavaBeanDeserializer javaBeanDeser = ((com.alibaba.fastjson.parser.deserializer.JavaBeanDeserializer) (fieldValueDeserilizer));
        value = javaBeanDeser.deserialze(parser, fieldType, fieldInfo.name, fieldInfo.parserFeatures);
    } else if ((this.fieldInfo.format != null) && (fieldValueDeserilizer instanceof com.alibaba.fastjson.parser.deserializer.ContextObjectDeserializer)) {
        value = // 
        ((com.alibaba.fastjson.parser.deserializer.ContextObjectDeserializer) (fieldValueDeserilizer)).deserialze(parser, fieldType, fieldInfo.name, fieldInfo.format, fieldInfo.parserFeatures);
    } else {
        value = fieldValueDeserilizer.deserialze(parser, fieldType, fieldInfo.name);
    }
    if ((value instanceof byte[]) && ("gzip".equals(fieldInfo.format) || "gzip,base64".equals(fieldInfo.format))) {
        byte[] bytes = ((byte[]) (value));
        java.util.zip.GZIPInputStream gzipIn = null;
        try {
            gzipIn = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(bytes));
            java.io.ByteArrayOutputStream byteOut = new java.io.ByteArrayOutputStream();
            for (; ;) {
                byte[] buf = new byte[1024];
                int len = gzipIn.read(buf);
                if (len == (-1)) {
                    break;
                }
                if (len > 0) {
                    byteOut.write(buf, 0, len);
                }
            }
            value = byteOut.toByteArray();
        } catch (java.io.IOException ex) {
            throw new com.alibaba.fastjson.JSONException("unzip bytes error.", ex);
        }
    }
    if (parser.getResolveStatus() == com.alibaba.fastjson.parser.DefaultJSONParser.NeedToResolve) {
        com.alibaba.fastjson.parser.DefaultJSONParser.ResolveTask task = parser.getLastResolveTask();
        task.fieldDeserializer = this;
        task.ownerContext = parser.getContext();
        parser.setResolveStatus(com.alibaba.fastjson.parser.DefaultJSONParser.NONE);
    } else if (object == null) {
        /* NPEX_PATCH_BEGINS */
        if (fieldValues == null) {
            fieldValues = null;
        }
        fieldValues.put(fieldInfo.name, value);
    } else {
        setValue(object, value);
    }
}

    public int getFastMatchToken() {
        if (fieldValueDeserilizer != null) {
            return fieldValueDeserilizer.getFastMatchToken();
        }

        return JSONToken.LITERAL_INT;
    }

    public void parseFieldUnwrapped(DefaultJSONParser parser, Object object, Type objectType, Map<String, Object> fieldValues) {
        throw new JSONException("TODO");
    }
}
