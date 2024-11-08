package org.loon.framework.javase.game.extend.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.WeakHashMap;
import org.loon.framework.javase.game.core.resource.LRAFile;

/**
 * Copyright 2008
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loonframework
 * @author chenpeng
 * @email��ceponline@yahoo.com.cn
 * @version 0.1.2
 */
public class MDBEngine {

    private static final Map chacheOnlyDBMap = new WeakHashMap(20);

    public static final MDB getMDB(final String fileName) {
        Object object = chacheOnlyDBMap.get(fileName);
        if (object == null) {
            chacheOnlyDBMap.put(fileName, (object = new MDBImpl(fileName)));
        }
        return (MDB) object;
    }

    public static final Access getAccess(final LRAFile file) throws FileNotFoundException {
        return new AccessImpl(file);
    }

    public static final Access getAccess(final File file, final boolean read, final boolean write) throws FileNotFoundException {
        return new AccessImpl(file, read, write);
    }
}
