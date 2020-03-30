package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.ViewDataBinding;
import android.support.annotation.IdRes;
import android.view.View;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BindingViewFinder {

    final ViewDataBinding binding;
    private final Resources resources;
    private final Map<String, Field> fieldMap = new HashMap<>();

    public BindingViewFinder(ViewDataBinding binding, Context context) {
        this.binding = binding;
        this.resources = context.getResources();

        for (Field field : binding.getClass().getDeclaredFields()) {
            if (!View.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            if (field.isAccessible()) {
                String idName = separateCamelCase(field.getName(), "_").toLowerCase();
                fieldMap.put(idName, field);
            }
        }
    }

    public View findViewById(@IdRes int id) {
        String fieldName = resources.getResourceEntryName(id);

        if (fieldMap.containsKey(fieldName)) {
            try {
                return (View) fieldMap.get(fieldName).get(binding);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    static String separateCamelCase(String name, String separator) {
        StringBuilder translation = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }
            translation.append(character);
        }
        return translation.toString();
    }
}
