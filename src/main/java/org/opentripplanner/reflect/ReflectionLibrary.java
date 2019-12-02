package org.opentripplanner.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionLibrary {

    /** Concatenate all fields and values of a Java object. */
    public static String dumpFields (Object object) {
        StringBuilder sb = new StringBuilder();
        Class<?> clazz = object.getClass();
        sb.append("Summarizing ");
        sb.append(clazz.getSimpleName()).append(" {");
        sb.append('\n');
        addFields("  ", clazz, object, sb);
        sb.append("}");
        return sb.toString();
    }

    private static void addFields(String indent, Class<?> clazz, Object object, StringBuilder sb) {
        // Exit if the recursion is to deep; more than 8 levels down
        if(indent.length() > 16) { return; }
        // Add 2 spaces to the margin for each nested call to this method
        indent = indent + "  ";

        for (Field field : clazz.getFields()) {
            int modifiers = field.getModifiers();
            if(Modifier.isStatic(modifiers)) { continue; }

            try {
                Class<?> fieldClass = field.getType();
                Object fieldValue = field.get(object);

                sb.append(indent);
                sb.append(field.getName());

                if(fieldValue != null && isOtpClass(fieldClass)) {
                    sb.append(" {\n");
                    addFields(indent, fieldClass, fieldValue, sb);
                    sb.append(indent).append("}");
                }
                else {
                    String value = fieldValue == null ? "null" : fieldValue.toString();
                    sb.append(" = ");
                    sb.append(value);
                }
                sb.append('\n');
            }
            catch (IllegalAccessException ex) {
                sb.append(" = (non-public)\n");
            }
        }
    }

    private static boolean isOtpClass(Class<?> fieldClass) {
        return fieldClass.getName().startsWith("org.opentripplanner.");
    }
}
