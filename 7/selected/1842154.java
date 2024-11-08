package net.sourceforge.hlm.util.storage;

public class RAMCollection extends StoredCollectionImpl {

    public RAMObject createRootObject(short typeID, short subTypeID) {
        if (this.rootObject == null) {
            this.rootObject = new RAMObject(null, typeID, subTypeID);
        } else {
            checkType(this.rootObject, typeID, subTypeID);
        }
        return this.rootObject;
    }

    @Override
    protected RAMObject getRootObject() {
        return this.rootObject;
    }

    static RAMObject checkType(RAMObject object, short typeID) {
        if (object != null && object.typeID != typeID) {
            throw new IllegalArgumentException();
        }
        return object;
    }

    static RAMObject checkType(RAMObject object, short typeID, short subTypeID) {
        if (object != null && (object.typeID != typeID || object.subTypeID != subTypeID)) {
            throw new IllegalArgumentException();
        }
        return object;
    }

    static void adjustReferenceCount(RAMObject source, RAMObject target, int change, RAMObject adjustRoot) {
        if (target == null || (adjustRoot != null && !adjustRoot.isAncestorOf(target))) {
            return;
        }
        while (target != adjustRoot) {
            while (source.level > target.level) {
                source = source.parent;
            }
            if (source == target) {
                return;
            }
            target.foreignReferenceCount += change;
            target = target.parent;
        }
    }

    static RAMObject getCommonAncestor(RAMObject object1, RAMObject object2) {
        while (object1 != object2) {
            if (object1.level > object2.level) {
                object1 = object1.parent;
            } else {
                object2 = object2.parent;
            }
        }
        return object1;
    }

    public long getMemoryUsage() {
        if (this.rootObject == null) {
            return 0;
        } else {
            return this.rootObject.getMemoryUsage();
        }
    }

    RAMObject rootObject;

    class RAMObject extends StoredObjectImpl {

        RAMObject(RAMObject parent, short typeID, short subTypeID) {
            this.parent = parent;
            if (this.parent != null) {
                this.level = parent.level + 1;
            }
            this.typeID = typeID;
            this.subTypeID = subTypeID;
        }

        public RAMObject getParent() {
            return this.parent;
        }

        public int getIndex() {
            return this.indexIn(this.parent.children);
        }

        public boolean isAncestorOf(StoredObject object) {
            RAMObject ramObject = (RAMObject) object;
            while (ramObject.level > this.level) {
                ramObject = ramObject.parent;
            }
            return (ramObject == this);
        }

        public short getTypeID() {
            return this.typeID;
        }

        public short getSubTypeID() {
            return this.subTypeID;
        }

        public void setChildCount(int count) {
            if (this.realChildCount == count) {
                return;
            }
            this.setChildCountInternal(count);
            this.changed(ChangeInfo.CHILD);
        }

        private void setChildCountInternal(int count) {
            int oldCount = this.realChildCount;
            if (count <= oldCount) {
                while (oldCount > count) {
                    oldCount--;
                    this.destroyChildInternal(oldCount);
                }
                if (count == 0) {
                    this.children = null;
                }
            } else if (this.children == null || count > this.children.length) {
                int length = this.children == null ? CHILDREN_START_LENGTH : this.children.length;
                while (length < count) {
                    length <<= 1;
                }
                RAMObject[] newChildren = new RAMObject[length];
                for (int i = 0; i < count && i < this.realChildCount; i++) {
                    newChildren[i] = this.children[i];
                }
                this.children = newChildren;
            }
            this.realChildCount = count;
        }

        public int getChildCount() {
            return this.realChildCount;
        }

        public RAMObject createChild(int index, short typeID, short subTypeID, boolean broadcast) {
            if (index >= this.realChildCount) {
                this.setChildCountInternal(index + 1);
            }
            RAMObject child = this.children[index];
            if (child != null) {
                return checkType(child, typeID, subTypeID);
            }
            child = new RAMObject(this, typeID, subTypeID);
            this.children[index] = child;
            this.childCreated(child, broadcast);
            return child;
        }

        public void destroyChild(int index) {
            if (!this.destroyChildInternal(index)) {
                return;
            }
            this.changed(ChangeInfo.CHILD);
        }

        private boolean destroyChildInternal(int index) {
            if (index >= this.realChildCount) {
                return false;
            }
            RAMObject child = this.children[index];
            if (child == null) {
                return false;
            }
            child.destroy();
            this.children[index] = null;
            return true;
        }

        public RAMObject getChild(int index) {
            if (index >= this.realChildCount) {
                return null;
            }
            return this.children[index];
        }

        public RAMObject getChild(int index, short typeID) {
            if (index >= this.realChildCount) {
                return null;
            }
            return checkType(this.children[index], typeID);
        }

        public RAMObject getChild(int index, short typeID, short subTypeID) {
            if (index >= this.realChildCount) {
                return null;
            }
            return checkType(this.children[index], typeID, subTypeID);
        }

        public RAMObject insertChild(int index, short typeID, short subTypeID) {
            this.insertChildInternal(index);
            RAMObject child = new RAMObject(this, typeID, subTypeID);
            this.children[index] = child;
            this.childCreated(child, true);
            return child;
        }

        public void insertChild(int index) {
            this.insertChildInternal(index);
            this.changed(ChangeInfo.CHILD);
        }

        private void insertChildInternal(int index) {
            int oldCount = this.realChildCount;
            int newCount = (index > oldCount ? index : oldCount) + 1;
            if (this.children == null || newCount > this.children.length) {
                int length = this.children == null ? CHILDREN_START_LENGTH : this.children.length;
                while (length < newCount) {
                    length <<= 1;
                }
                RAMObject[] newChildren = new RAMObject[length];
                int i;
                for (i = 0; i < index && i < oldCount; i++) {
                    newChildren[i] = this.children[i];
                }
                for (; i < oldCount; i++) {
                    newChildren[i + 1] = this.children[i];
                }
                this.children = newChildren;
            } else {
                for (int i = oldCount; i > index; i--) {
                    this.children[i] = this.children[i - 1];
                }
                this.children[index] = null;
            }
            this.realChildCount = newCount;
        }

        public void removeChild(int index) {
            if (index >= this.realChildCount) {
                return;
            }
            RAMObject child = this.children[index];
            if (child != null) {
                child.destroy();
            }
            this.removeChildInternal(index);
            this.changed(ChangeInfo.CHILD);
        }

        private void removeChildInternal(int index) {
            int count = this.realChildCount - 1;
            for (; index < count; index++) {
                this.children[index] = this.children[index + 1];
            }
            this.children[index] = null;
            this.realChildCount = count;
        }

        public void moveChild(int oldIndex, StoredObject newParent, int newIndex) {
            if (newParent == null || newParent == this) {
                if (newIndex == oldIndex) {
                    return;
                }
                if (newIndex < 0 || newIndex >= this.realChildCount) {
                    throw new IllegalArgumentException();
                }
                this.moveItem(this.children, oldIndex, newIndex);
                this.changed(ChangeInfo.CHILD);
            } else {
                RAMObject child = this.children[oldIndex];
                RAMObject ramParent = (RAMObject) newParent;
                if (newIndex < 0 || newIndex > ramParent.realChildCount || (child != null && child.isAncestorOf(ramParent))) {
                    throw new IllegalArgumentException();
                }
                this.removeChildInternal(oldIndex);
                ramParent.insertChildInternal(newIndex);
                if (child != null) {
                    child.setParent(ramParent);
                    ramParent.children[newIndex] = child;
                    getCommonAncestor(this, ramParent).updateReferenceCounts();
                }
                this.changedSilently(ChangeInfo.CHILD);
                ramParent.changed(ChangeInfo.CHILD);
            }
        }

        public void swapChildren(int index1, int index2) {
            if (index1 == index2) {
                return;
            }
            RAMObject child1 = this.children[index1];
            RAMObject child2 = this.children[index2];
            this.children[index1] = child2;
            this.children[index2] = child1;
            this.changed(ChangeInfo.CHILD);
        }

        public RAMObject findChild(int stringIndex, String value, short typeID) {
            if (this.children != null) {
                for (RAMObject child : this.children) {
                    if (child != null && value.equals(child.getString(stringIndex))) {
                        return checkType(child, typeID);
                    }
                }
            }
            return null;
        }

        public void setReferenceCount(int count) {
            int currentCount = this.getReferenceCount();
            if (currentCount == count) {
                return;
            }
            this.setReferenceCountInternal(count, currentCount);
            this.changed(ChangeInfo.REFERENCE);
        }

        private void setReferenceCountInternal(int count, int currentCount) {
            while (currentCount > count) {
                this.setReference(--currentCount, null);
            }
            RAMObject[] newReferences = count == 0 ? null : new RAMObject[count];
            if (this.references != null) {
                for (int i = 0; i < count && i < this.references.length; i++) {
                    newReferences[i] = this.references[i];
                }
            }
            this.references = newReferences;
        }

        public int getReferenceCount() {
            return (this.references == null ? 0 : this.references.length);
        }

        @Override
        protected void setReference(int index, StoredObjectImpl target) {
            if (!this.setReferenceInternal(index, (RAMObject) target)) {
                return;
            }
            this.changed(ChangeInfo.REFERENCE);
        }

        private boolean setReferenceInternal(int index, RAMObject target) {
            int currentCount = this.getReferenceCount();
            if (index >= currentCount) {
                if (target == null) {
                    return false;
                }
                this.setReferenceCountInternal(index + 1, currentCount);
            }
            RAMObject current = this.references[index];
            if (target == current) {
                return false;
            }
            adjustReferenceCount(this, current, -1, null);
            adjustReferenceCount(this, target, 1, null);
            this.references[index] = target;
            return true;
        }

        public RAMObject getReference(int index) {
            if (index >= this.getReferenceCount()) {
                return null;
            }
            return this.references[index];
        }

        public RAMObject getReference(int index, short typeID) {
            if (index >= this.getReferenceCount()) {
                return null;
            }
            return checkType(this.references[index], typeID);
        }

        public RAMObject getReference(int index, short typeID, short subTypeID) {
            if (index >= this.getReferenceCount()) {
                return null;
            }
            return checkType(this.references[index], typeID, subTypeID);
        }

        public void insertReference(int index, StoredObject target) {
            int count = this.getReferenceCount();
            RAMObject[] newReferences = new RAMObject[(index > count ? index : count) + 1];
            this.insertItem(this.references, newReferences, index);
            RAMObject ramTarget = (RAMObject) target;
            adjustReferenceCount(this, ramTarget, 1, null);
            newReferences[index] = ramTarget;
            this.references = newReferences;
            this.changed(ChangeInfo.REFERENCE);
        }

        public void removeReference(int index) {
            int count = this.getReferenceCount();
            if (index >= count) {
                return;
            }
            adjustReferenceCount(this, this.references[index], -1, null);
            if (count > 1) {
                RAMObject[] newReferences = new RAMObject[count - 1];
                this.removeItem(this.references, newReferences, index);
                this.references = newReferences;
            } else {
                this.references = null;
            }
            this.changed(ChangeInfo.REFERENCE);
        }

        public void moveReference(int oldIndex, int newIndex) {
            if (newIndex == oldIndex) {
                return;
            }
            if (newIndex < 0 || newIndex >= this.getReferenceCount()) {
                throw new IllegalArgumentException();
            }
            this.moveItem(this.references, oldIndex, newIndex);
            this.changed(ChangeInfo.REFERENCE);
        }

        public void swapReferences(int index1, int index2) {
            if (index1 == index2) {
                return;
            }
            RAMObject reference1 = this.references[index1];
            RAMObject reference2 = this.references[index2];
            this.references[index1] = reference2;
            this.references[index2] = reference1;
            this.changed(ChangeInfo.REFERENCE);
        }

        public void setIntCount(int count) {
            if (this.getIntCount() == count) {
                return;
            }
            this.setIntCountInternal(count);
            this.changed(ChangeInfo.INT);
        }

        private void setIntCountInternal(int count) {
            if (count == 0) {
                this.ints = null;
                return;
            }
            int[] newAttributes = new int[count];
            if (this.ints != null) {
                for (int i = 0; i < count && i < this.ints.length; i++) {
                    newAttributes[i] = this.ints[i];
                }
            }
            this.ints = newAttributes;
        }

        public int getIntCount() {
            return this.ints == null ? 0 : this.ints.length;
        }

        public void setInt(int index, int value) {
            if (index >= this.getIntCount()) {
                if (value == 0) {
                    return;
                }
                this.setIntCountInternal(index + 1);
            }
            this.ints[index] = value;
            this.changed(ChangeInfo.INT);
        }

        public int getInt(int index) {
            if (index >= this.getIntCount()) {
                return 0;
            }
            return this.ints[index];
        }

        @Override
        public void setIntBit(int index, int bit, boolean value) {
            if (value) {
                if (index >= this.getIntCount()) {
                    this.setIntCountInternal(index + 1);
                } else if ((this.ints[index] & (1 << bit)) != 0) {
                    return;
                }
                this.ints[index] |= (1 << bit);
            } else {
                if (index >= this.getIntCount() || (this.ints[index] & (1 << bit)) == 0) {
                    return;
                }
                this.ints[index] &= ~(1 << bit);
            }
            this.changed(ChangeInfo.INT);
        }

        public void insertInt(int index, int value) {
            int count = this.getIntCount();
            int[] newInts = new int[(index > count ? index : count) + 1];
            int i;
            for (i = 0; i < index && i < count; i++) {
                newInts[i] = this.ints[i];
            }
            newInts[index] = value;
            for (; i < count; i++) {
                newInts[i + 1] = this.ints[i];
            }
            this.ints = newInts;
            this.changed(ChangeInfo.INT);
        }

        public void removeInt(int index) {
            if (index >= this.getIntCount()) {
                return;
            }
            int count = this.ints.length - 1;
            int[] newInts = count == 0 ? null : new int[count];
            int i;
            for (i = 0; i < index; i++) {
                newInts[i] = this.ints[i];
            }
            for (; i < count; i++) {
                newInts[i] = this.ints[i + 1];
            }
            this.ints = newInts;
            this.changed(ChangeInfo.INT);
        }

        public void moveInt(int oldIndex, int newIndex) {
            if (newIndex == oldIndex) {
                return;
            }
            if (newIndex < 0 || newIndex >= this.getIntCount()) {
                throw new IllegalArgumentException();
            }
            int value = this.ints[oldIndex];
            if (newIndex > oldIndex) {
                for (int i = oldIndex; i < newIndex; i++) {
                    this.ints[i] = this.ints[i + 1];
                }
            } else {
                for (int i = oldIndex; i > newIndex; i--) {
                    this.ints[i] = this.ints[i - 1];
                }
            }
            this.ints[newIndex] = value;
            this.changed(ChangeInfo.INT);
        }

        public void swapInts(int index1, int index2) {
            if (index1 == index2) {
                return;
            }
            int int1 = this.ints[index1];
            int int2 = this.ints[index2];
            this.ints[index1] = int2;
            this.ints[index2] = int1;
            this.changed(ChangeInfo.INT);
        }

        public void setStringCount(int count) {
            if (this.getStringCount() == count) {
                return;
            }
            this.setStringCountInternal(count);
            this.changed(ChangeInfo.STRING);
        }

        private void setStringCountInternal(int count) {
            if (count == 0) {
                this.strings = null;
                return;
            }
            String[] newAttributes = new String[count];
            if (this.strings != null) {
                for (int i = 0; i < count && i < this.strings.length; i++) {
                    newAttributes[i] = this.strings[i];
                }
            }
            this.strings = newAttributes;
        }

        public int getStringCount() {
            return this.strings == null ? 0 : this.strings.length;
        }

        public void setString(int index, String value) {
            if (index >= this.getStringCount()) {
                if (value == null) {
                    return;
                }
                this.setStringCountInternal(index + 1);
            }
            if (value == null && this.strings[index] == null) {
                return;
            }
            this.strings[index] = value;
            this.changed(ChangeInfo.STRING);
        }

        public String getString(int index) {
            if (index >= this.getStringCount()) {
                return null;
            }
            return this.strings[index];
        }

        public void insertString(int index, String value) {
            int count = this.getStringCount();
            String[] newStrings = new String[(index > count ? index : count) + 1];
            this.insertItem(this.strings, newStrings, index);
            newStrings[index] = value;
            this.strings = newStrings;
            this.changed(ChangeInfo.STRING);
        }

        public void removeString(int index) {
            int count = this.getStringCount();
            if (index >= count) {
                return;
            }
            if (count > 1) {
                String[] newStrings = new String[count - 1];
                this.removeItem(this.strings, newStrings, index);
                this.strings = newStrings;
            } else {
                this.strings = null;
            }
            this.changed(ChangeInfo.STRING);
        }

        public void moveString(int oldIndex, int newIndex) {
            if (newIndex == oldIndex) {
                return;
            }
            if (newIndex < 0 || newIndex >= this.getStringCount()) {
                throw new IllegalArgumentException();
            }
            this.moveItem(this.strings, oldIndex, newIndex);
            this.changed(ChangeInfo.STRING);
        }

        public void swapStrings(int index1, int index2) {
            if (index1 == index2) {
                return;
            }
            String string1 = this.strings[index1];
            String string2 = this.strings[index2];
            this.strings[index1] = string2;
            this.strings[index2] = string1;
            this.changed(ChangeInfo.STRING);
        }

        public int getForeignReferenceCount() {
            return this.foreignReferenceCount;
        }

        private void destroy() {
            this.preDestroy();
            this.clearAllReferences();
            this.clearAllChildren();
        }

        private int indexIn(RAMObject[] list) {
            for (int index = list.length - 1; index >= 0; index--) {
                if (list[index] == this) {
                    return index;
                }
            }
            return -1;
        }

        private <A> void insertItem(A[] oldList, A[] newList, int index) {
            if (oldList == null) {
                return;
            }
            if (index > oldList.length) {
                index = oldList.length;
            }
            int i;
            for (i = 0; i < index; i++) {
                newList[i] = oldList[i];
            }
            for (; i < oldList.length; i++) {
                newList[i + 1] = oldList[i];
            }
        }

        private <A> void removeItem(A[] oldList, A[] newList, int index) {
            int i;
            for (i = 0; i < index; i++) {
                newList[i] = oldList[i];
            }
            for (; i < newList.length; i++) {
                newList[i] = oldList[i + 1];
            }
        }

        private <A> void moveItem(A[] list, int oldIndex, int newIndex) {
            A item = list[oldIndex];
            if (newIndex > oldIndex) {
                for (int i = oldIndex; i < newIndex; i++) {
                    list[i] = list[i + 1];
                }
            } else {
                for (int i = oldIndex; i > newIndex; i--) {
                    list[i] = list[i - 1];
                }
            }
            list[newIndex] = item;
        }

        private void clearAllReferences() {
            if (this.references != null) {
                for (int index = this.references.length - 1; index >= 0; index--) {
                    adjustReferenceCount(this, this.references[index], -1, null);
                }
            }
            if (this.children != null) {
                for (RAMObject child : this.children) {
                    if (child != null) {
                        child.clearAllReferences();
                    }
                }
            }
        }

        private void clearAllChildren() {
            if (this.children != null) {
                for (RAMObject child : this.children) {
                    if (child != null) {
                        child.preDestroy();
                        child.clearAllChildren();
                    }
                }
                this.realChildCount = 0;
                this.children = null;
            }
        }

        private void updateReferenceCounts() {
            this.clearReferenceCounts();
            RAMCollection.this.rootObject.rebuildReferenceCounts(this);
        }

        private void clearReferenceCounts() {
            if (this.children != null) {
                for (RAMObject child : this.children) {
                    if (child != null) {
                        child.foreignReferenceCount = 0;
                        child.clearReferenceCounts();
                    }
                }
            }
        }

        private void rebuildReferenceCounts(RAMObject adjustRoot) {
            if (this.children != null) {
                for (RAMObject child : this.children) {
                    if (child != null) {
                        child.rebuildReferenceCounts(adjustRoot);
                    }
                }
            }
            if (this.references != null) {
                for (RAMObject target : this.references) {
                    adjustReferenceCount(this, target, 1, adjustRoot);
                }
            }
        }

        private void setParent(RAMObject parent) {
            this.parent = parent;
            this.changeLevel(parent.level + 1);
        }

        private void changeLevel(int level) {
            if (this.level != level) {
                this.level = level;
                if (this.children != null) {
                    for (RAMObject child : this.children) {
                        if (child != null) {
                            child.changeLevel(level + 1);
                        }
                    }
                }
            }
        }

        long getMemoryUsage() {
            long result = 8 + 10 * 4;
            if (this.children != null) {
                result += 8 + this.children.length * 4;
                for (RAMObject child : this.children) {
                    if (child != null) {
                        result += child.getMemoryUsage();
                    }
                }
            }
            if (this.references != null) {
                result += 8 + this.references.length * 4;
            }
            if (this.ints != null) {
                result += 8 + this.ints.length * 4;
            }
            if (this.strings != null) {
                result += 8 + this.strings.length * 4;
                for (String string : this.strings) {
                    if (string != null) {
                        result += 8 + 4 + string.length() * 2;
                    }
                }
            }
            return result;
        }

        RAMObject parent;

        int level;

        short typeID;

        short subTypeID;

        RAMObject[] children;

        int realChildCount;

        RAMObject[] references;

        int[] ints;

        String[] strings;

        int foreignReferenceCount;

        private static final int CHILDREN_START_LENGTH = 4;
    }
}
