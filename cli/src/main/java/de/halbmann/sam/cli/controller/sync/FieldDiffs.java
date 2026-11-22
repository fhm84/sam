package de.halbmann.sam.cli.controller.sync;

import de.halbmann.sam.api.entity.persons.Person;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compares the fields of an import file's record with the target record and copies differing
 * values onto the target (ADR-0019).
 *
 * <p>A field counts only when the file has a value for it: {@code null}, a blank string or an
 * empty collection in the file means "unknown", never "clear". Strings are compared stripped; a
 * {@link Person} (composer, arranger) by name, ignoring case, since the server resolves persons
 * by name. File and target may be different classes (e.g. {@code CreateInstrument} and {@code
 * Instrument}); fields are matched by bean property name.
 */
public final class FieldDiffs {

    private FieldDiffs() {}

    /** The fields among {@code fields} whose file value differs from the target's. */
    public static List<FieldDiff> compare(final Object file, final Object target, final Collection<String> fields) {
        Map<String, PropertyDescriptor> fileProperties = properties(file.getClass());
        Map<String, PropertyDescriptor> targetProperties = properties(target.getClass());
        List<FieldDiff> diffs = new ArrayList<>();
        for (String field : fields) {
            Object fileValue = read(property(fileProperties, file, field), file);
            if (isUnknown(fileValue)) {
                continue;
            }
            Object targetValue = read(property(targetProperties, target, field), target);
            if (!Objects.equals(normalize(fileValue), normalize(targetValue))) {
                diffs.add(new FieldDiff(field, fileValue, targetValue));
            }
        }
        return diffs;
    }

    /** Writes each diff's file value onto the target; other fields keep the target's value. */
    public static void apply(final List<FieldDiff> diffs, final Object target) {
        Map<String, PropertyDescriptor> targetProperties = properties(target.getClass());
        for (FieldDiff diff : diffs) {
            PropertyDescriptor property = property(targetProperties, target, diff.field());
            if (property.getWriteMethod() == null) {
                throw new IllegalStateException(
                        target.getClass().getSimpleName() + "." + diff.field() + " cannot be written");
            }
            Object value = diff.fileValue();
            if (value instanceof Person person) {
                // a fresh reference by name: the server resolves or creates the person
                Person byName = new Person();
                byName.setName(person.getName().strip());
                value = byName;
            } else if (value instanceof String s) {
                value = s.strip();
            }
            invoke(property, target, value);
        }
    }

    static String display(final Object value) {
        if (value == null) {
            return "(empty)";
        }
        if (value instanceof Person person) {
            return "\"" + person.getName() + "\"";
        }
        // one report line per field: show line breaks inside a value as \n
        return value instanceof String str ? "\"" + str.replace("\n", "\\n") + "\"" : value.toString();
    }

    private static boolean isUnknown(final Object value) {
        return value == null
                || (value instanceof String s && s.isBlank())
                || (value instanceof Collection<?> c && c.isEmpty())
                || (value instanceof Person p
                        && (p.getName() == null || p.getName().isBlank()));
    }

    private static Object normalize(final Object value) {
        if (value instanceof String s) {
            return s.strip();
        }
        if (value instanceof Person p) {
            return p.getName() == null ? null : p.getName().strip().toLowerCase(Locale.ROOT);
        }
        return value;
    }

    private static Map<String, PropertyDescriptor> properties(final Class<?> type) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                    .filter(p -> p.getReadMethod() != null)
                    .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity()));
        } catch (IntrospectionException e) {
            throw new IllegalStateException("Cannot inspect " + type.getName(), e);
        }
    }

    private static PropertyDescriptor property(
            final Map<String, PropertyDescriptor> properties, final Object bean, final String field) {
        PropertyDescriptor property = properties.get(field);
        if (property == null) {
            throw new IllegalArgumentException(bean.getClass().getSimpleName() + " has no field " + field);
        }
        return property;
    }

    private static Object read(final PropertyDescriptor property, final Object bean) {
        try {
            return property.getReadMethod().invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot read " + property.getName(), e);
        }
    }

    private static void invoke(final PropertyDescriptor property, final Object bean, final Object value) {
        try {
            property.getWriteMethod().invoke(bean, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot write " + property.getName(), e);
        }
    }
}
