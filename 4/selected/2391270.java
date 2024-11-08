package corner.orm.tapestry.component.textfield;

import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.form.TranslatedField;
import corner.orm.tapestry.translator.NumTranslator;
import corner.util.StringUtils;

/**
 * 复写Tapestry的TextField,提供指定TextField默认值的能力
 * 
 * @author Ghost
 * @version $Revision: 4205 $
 * @since 2.2.1
 */
public abstract class TextField extends org.apache.tapestry.form.TextField {

    /**
	 * @see org.apache.tapestry.form.TextField#renderFormComponent(org.apache.tapestry.IMarkupWriter,
	 *      org.apache.tapestry.IRequestCycle)
	 */
    @Override
    protected void renderFormComponent(IMarkupWriter writer, IRequestCycle cycle) {
        Object defaultValue = this.getDefaultValue();
        boolean isReadOnly = this.getOnlyRead() != null ? this.getOnlyRead().booleanValue() : false;
        if (isReadOnly) {
            String value = null;
            if (defaultValue == null || StringUtils.blank(defaultValue.toString())) {
                value = getTranslatedFieldSupport().format(this, getValue());
            } else {
                if (StringUtils.isNumber(getValue())) {
                    if (checkNumberUseDefValue(this)) {
                        value = getTranslatedFieldSupport().format(this, this.getDefaultValue());
                    } else {
                        value = getTranslatedFieldSupport().format(this, getValue());
                    }
                } else {
                    if (getValue() != null && StringUtils.notBlank(getValue().toString())) {
                        value = getTranslatedFieldSupport().format(this, getValue());
                    } else {
                        value = getTranslatedFieldSupport().format(this, this.getDefaultValue());
                    }
                }
            }
            renderDelegatePrefix(writer, cycle);
            writer.beginEmpty("input");
            writer.attribute("type", isHidden() ? "password" : "text");
            writer.attribute("name", getName());
            if (isDisabled()) writer.attribute("disabled", "disabled");
            if (value != null) writer.attribute("value", value);
            writer.attribute("readonly", "true");
            renderIdAttribute(writer, cycle);
            renderDelegateAttributes(writer, cycle);
            getTranslatedFieldSupport().renderContributions(this, writer, cycle);
            getValidatableFieldSupport().renderContributions(this, writer, cycle);
            renderInformalParameters(writer, cycle);
            writer.closeTag();
            renderDelegateSuffix(writer, cycle);
        } else {
            if (defaultValue == null || StringUtils.blank(defaultValue.toString())) {
                super.renderFormComponent(writer, cycle);
            } else {
                String value = getTranslatedFieldSupport().format(this, getValue());
                if (StringUtils.isNumber(getValue()) || (this.getTranslator() instanceof NumTranslator)) {
                    if (checkNumberUseDefValue(this)) {
                        this.setValue(this.getDefaultValue());
                        super.renderFormComponent(writer, cycle);
                    } else {
                        super.renderFormComponent(writer, cycle);
                    }
                } else {
                    if (value != null && StringUtils.notBlank(value)) {
                        super.renderFormComponent(writer, cycle);
                    } else {
                        this.setValue(this.getDefaultValue());
                        super.renderFormComponent(writer, cycle);
                    }
                }
            }
        }
    }

    /**
	 * 判断数字类型时是否使用defaultValue
	 * @return boolean值
	 * 1. 如果refValue类型为Number(long,double等等),而defValue也是Number类型
	 *     true:1.refValue=0 同时 defValue!=0 此时属于新增状态，使用defValue
	 *     false:2.refValue>0 此时属于编辑状态，此时使用refValue
	 * 2. 如果refValue或者defValue其中任意一个不是Number类型，返回false
	 */
    protected boolean checkNumberUseDefValue(TranslatedField field) {
        Object defValue = getDefaultValue();
        Object refValue = getValue();
        if (defValue != null) {
            if (StringUtils.isNumber(defValue)) {
                double defV = Double.valueOf(defValue.toString());
                double refV = refValue != null ? Double.valueOf(refValue.toString()) : 0;
                if (refV == 0 && defV != 0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
	 * 取得默认值
	 * 
	 * @return
	 */
    @Parameter
    public abstract Object getDefaultValue();

    /**
	 * 取得该TextField的属性 true:该TextField为readonly;false:该TextField不是readonly
	 * 
	 * @return
	 */
    @Parameter(defaultValue = "ognl:false")
    public abstract Boolean getOnlyRead();
}
