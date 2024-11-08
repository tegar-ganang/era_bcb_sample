package org.jmetis.reflection.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.jmetis.kernel.assertion.Assertions;
import org.jmetis.kernel.metadata.IClassDescription;
import org.jmetis.kernel.metadata.IMetaDataIntrospector;
import org.jmetis.kernel.metadata.IPropertyAccessor;
import org.jmetis.kernel.metadata.IPropertyDescription;
import org.jmetis.reflection.Primitives;
import org.jmetis.reflection.property.MethodAccessor;

/**
 * {@code PropertyDescriptor}
 * 
 * @author aerlach
 */
public class PropertyDescriptor implements IPropertyDescription {

    protected static final Class<?>[] EMPTY_CLASS_ARRAY = {};

    private Annotation[] annotations;

    private Annotation[] declaredAnnotations;

    private ClassDescriptor classDescriptor;

    private String propertyName;

    private Class<?> propertyType;

    private Field declaredField;

    private Method readMethod;

    private Method writeMethod;

    private IPropertyAccessor propertyAccessor;

    /**
	 * Constructs a new {@code PropertyDescriptor} instance.
	 * 
	 * @param classDescriptor
	 * @param propertyName
	 */
    protected PropertyDescriptor(ClassDescriptor classDescriptor, String propertyName, Method readMethod, Method writeMethod) {
        super();
        this.classDescriptor = this.mustNotBeNull("classDescriptor", classDescriptor);
        this.propertyName = mustNotBeNullOrEmpty("propertyName", propertyName);
        this.readMethod = this.mustNotBeNull("readMethod", readMethod);
        this.writeMethod = writeMethod;
    }

    protected <T> T mustNotBeNull(String name, T value) {
        return Assertions.mustNotBeNull(name, value);
    }

    protected String mustNotBeNullOrEmpty(String name, String value) {
        return Assertions.mustNotBeNullOrEmpty(name, value);
    }

    protected IMetaDataIntrospector metaDataIntrospector() {
        return classDescriptor.metaDataIntrospector();
    }

    protected Class<?> declaringClass() {
        return classDescriptor.getDescribedClass();
    }

    protected Field declaredField() {
        if (declaredField == null) {
            try {
                declaredField = metaDataIntrospector().declaredFieldNamed(declaringClass(), propertyName);
            } catch (Exception ex) {
            }
        }
        return declaredField;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        if (!readMethod.isAnnotationPresent(annotationClass)) {
            Field declaredField = declaredField();
            if (declaredField != null && declaredField.isAnnotationPresent(annotationClass)) {
                return true;
            }
            if (writeMethod != null) {
                return writeMethod.isAnnotationPresent(annotationClass);
            }
            return false;
        }
        return true;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        T annotation = readMethod.getAnnotation(annotationClass);
        if (annotation == null) {
            Field declaredField = declaredField();
            if (declaredField != null) {
                annotation = declaredField.getAnnotation(annotationClass);
            }
            if (annotation == null && writeMethod != null) {
                annotation = writeMethod.getAnnotation(annotationClass);
            }
        }
        return annotation;
    }

    private Collection<Annotation> collectDeclaredAnnotationsInto(AnnotatedElement annotatedElement, Collection<Annotation> collectedDeclaredAnnotations) {
        if (annotatedElement != null) {
            Annotation[] declaredAnnotations = annotatedElement.getDeclaredAnnotations();
            if (declaredAnnotations.length > 0) {
                if (collectedDeclaredAnnotations == null) {
                    collectedDeclaredAnnotations = new ArrayList<Annotation>();
                }
                for (Annotation annotation : declaredAnnotations) {
                    collectedDeclaredAnnotations.add(annotation);
                }
            }
        }
        return collectedDeclaredAnnotations;
    }

    public Annotation[] getDeclaredAnnotations() {
        if (declaredAnnotations == null) {
            Collection<Annotation> collectedDeclaredAnnotations = collectDeclaredAnnotationsInto(readMethod, null);
            collectedDeclaredAnnotations = collectDeclaredAnnotationsInto(declaredField(), collectedDeclaredAnnotations);
            collectedDeclaredAnnotations = collectDeclaredAnnotationsInto(writeMethod, collectedDeclaredAnnotations);
            if (collectedDeclaredAnnotations != null) {
                declaredAnnotations = collectedDeclaredAnnotations.toArray(new Annotation[collectedDeclaredAnnotations.size()]);
            } else {
                declaredAnnotations = IClassDescription.EMPTY_ANNOTATION_ARRAY;
            }
        }
        return declaredAnnotations;
    }

    private Collection<Annotation> collectAnnotationsInto(AnnotatedElement annotatedElement, Collection<Annotation> collectedAnnotations) {
        if (annotatedElement != null) {
            Annotation[] annotations = annotatedElement.getAnnotations();
            if (annotations.length > 0) {
                if (collectedAnnotations == null) {
                    collectedAnnotations = new ArrayList<Annotation>();
                }
                for (Annotation annotation : annotations) {
                    collectedAnnotations.add(annotation);
                }
            }
        }
        return collectedAnnotations;
    }

    public Annotation[] getAnnotations() {
        if (annotations == null) {
            Collection<Annotation> collectedAnnotations = collectAnnotationsInto(readMethod, null);
            collectedAnnotations = collectAnnotationsInto(declaredField(), collectedAnnotations);
            collectedAnnotations = collectAnnotationsInto(writeMethod, collectedAnnotations);
            if (collectedAnnotations != null) {
                annotations = collectedAnnotations.toArray(new Annotation[collectedAnnotations.size()]);
            } else {
                annotations = IClassDescription.EMPTY_ANNOTATION_ARRAY;
            }
        }
        return annotations;
    }

    public String getPropertyName() {
        return propertyName;
    }

    /**
	 * @return the classDescriptor
	 */
    public IClassDescription getClassDescriptor() {
        return classDescriptor;
    }

    protected Map<TypeVariable<?>, Type> typeVariableMap() {
        return classDescriptor.typeVariableMap();
    }

    /**
	 * Returns the type that will be returned by the read method, or accepted as
	 * parameter type by the write method.
	 * 
	 * @return the type info for the property
	 * @see org.jmetis.kernel.metadata.IPropertyDescription#getPropertyType()
	 */
    public Class<?> getPropertyType() {
        if (propertyType == null) {
            propertyType = readMethod.getReturnType();
            if (propertyType == Object.class) {
                Type genericReturnType = readMethod.getGenericReturnType();
                if (genericReturnType instanceof TypeVariable<?>) {
                    if (typeVariableMap().containsKey(genericReturnType)) {
                        propertyType = (Class<?>) typeVariableMap().get(genericReturnType);
                    }
                    if (propertyType == Object.class) {
                        Method bridgedMethod = Primitives.findBridgedMethod(readMethod);
                        if (bridgedMethod != null) {
                            genericReturnType = bridgedMethod.getGenericReturnType();
                            if (genericReturnType instanceof TypeVariable<?>) {
                                if (typeVariableMap().containsKey(genericReturnType)) {
                                    propertyType = (Class<?>) typeVariableMap().get(genericReturnType);
                                }
                            } else {
                                propertyType = (Class<?>) genericReturnType;
                            }
                        }
                    }
                }
            }
        }
        return propertyType;
    }

    public IClassDescription getPropertyTypeDescriptor() {
        return classDescriptor.metaDataIntrospector().classDescriptorOf(getPropertyType());
    }

    public Class<?>[] getElementTypes() {
        Type genericType = readMethod.getGenericReturnType();
        Type[] typeArguments;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            typeArguments = parameterizedType.getActualTypeArguments();
        } else {
            return PropertyDescriptor.EMPTY_CLASS_ARRAY;
        }
        Class<?>[] elementTypes = new Class<?>[typeArguments.length];
        for (int i = 0, n = typeArguments.length; i < n; i++) {
            Type elementType = typeArguments[i];
            if (elementType instanceof TypeVariable<?>) {
                elementType = typeVariableMap().get(elementType);
            }
            if (elementType instanceof Class) {
                elementTypes[i] = (Class<?>) elementType;
            } else {
                elementTypes[i] = Object.class;
            }
        }
        return elementTypes;
    }

    protected IPropertyAccessor createPropertyAccessor() {
        return new MethodAccessor(this, readMethod, writeMethod);
    }

    public IPropertyAccessor getPropertyAccessor() {
        if (propertyAccessor == null) {
            propertyAccessor = createPropertyAccessor();
        }
        return propertyAccessor;
    }
}
