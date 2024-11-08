package net.sf.rcpforms.experimenting.ui_tests.data;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import net.sf.rcpforms.experimenting.model.bean.util.BeanUtil;
import net.sf.rcpforms.experimenting.model.bean.util.BeanUtil.BeanClassInfo;

public class BeanCopyTest {

    public class Machine implements Serializable {

        private Long id;

        private String hostname;

        private String standort;

        private String description;

        public Machine() {
        }

        public Machine(final String hostname) {
            this.hostname = hostname;
        }

        @Override
        public String toString() {
            return this.hostname;
        }

        public Long getId() {
            return id;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(final String hostname) {
            this.hostname = hostname;
        }

        public String getStandort() {
            return standort;
        }

        public void setStandort(final String standort) {
            this.standort = standort;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }
    }

    public static void main(final String[] args) {
        final BeanClassInfo inputInfo = BeanUtil.getBeanClassInfo(Machine.class);
        for (final PropertyDescriptor descriptor : inputInfo.propertyDescriptors) {
            final String name = descriptor.getName();
            System.out.println(" '" + name + "' : " + descriptor.getPropertyType() + "  (read: " + descriptor.getReadMethod() + ", write: " + descriptor.getWriteMethod() + ")");
        }
    }
}
