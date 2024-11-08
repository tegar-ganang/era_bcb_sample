package net.sourceforge.javautil.gui.io;

import javax.el.ValueExpression;
import net.sourceforge.javautil.common.ReflectionUtil;
import net.sourceforge.javautil.common.reflection.cache.ClassCache;
import net.sourceforge.javautil.common.reflection.cache.ClassDescriptor;
import net.sourceforge.javautil.common.reflection.cache.ClassProperty;
import net.sourceforge.javautil.gui.GUIContext;

/**
 * A base implementation for most cases.
 * 
 * @author elponderador
 * @author $Author$
 * @version $Id$
 */
public class InputComponentHandlerBase<CTX extends GUIContext> implements InputComponentHandler<CTX> {

    protected final ClassDescriptor descriptor;

    protected final InputComponent component;

    protected final ClassProperty readProperty;

    protected final ClassProperty writeProperty;

    protected Converter<CTX, Object, Object> converter;

    protected Validator<CTX> validator;

    public InputComponentHandlerBase(InputComponent component, String readProperty, String writeProperty) {
        this.component = component;
        this.descriptor = ClassCache.getFor(component.getClass());
        this.readProperty = descriptor.getProperty(readProperty);
        this.writeProperty = descriptor.getProperty(writeProperty);
    }

    public InputComponentHandlerBase setConverter(Converter converter) {
        this.converter = converter;
        return this;
    }

    public InputComponentHandlerBase setValidator(Validator validator) {
        this.validator = validator;
        return this;
    }

    public Object getConvertedValue(CTX ctx) {
        return converter == null ? getValue(ctx) : converter.convertFromInput(ctx, component, getValue(ctx));
    }

    public Object getValue(CTX ctx) {
        return readProperty.getValue(component);
    }

    public boolean validate(CTX ctx) {
        return validator == null ? true : validator.validate(ctx, component, getValue(ctx));
    }

    public InputComponentHandlerBase apply(CTX ctx) {
        if (component.getValue() != null && !component.getValue().isReadOnly(ctx.getELContext())) {
            component.getValue().setValue(ctx.getELContext(), ReflectionUtil.coerce(component.getValue().getExpectedType(), this.getConvertedValue(ctx)));
        }
        return this;
    }

    public InputComponentHandlerBase update(CTX ctx) {
        if (component.getValue() != null) {
            Object value = component.getValue().getValue(ctx.getELContext());
            if (this.converter != null) value = converter.convertToInput(ctx, component, value);
            writeProperty.setValue(component, ReflectionUtil.coerce(writeProperty.getType(), value));
        }
        return this;
    }
}
