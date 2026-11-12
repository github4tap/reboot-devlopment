/*
 *  Copyright 2024 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.adobe.aem.guides.wknd.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.aem.guides.wknd.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class TableModelTest {

    private final AemContext context = AppAemContext.newAemContext();

    private TableModel model;
    private Resource resource;

    @BeforeEach
    void setUp() {
        resource = context.create().resource("/content/wknd/table-test",
            "sling:resourceType", "wknd/components/table",
            "tableTitle", "Team table",
            "tableCaption", "Project staffing",
            "firstRowIsHeader", true,
            "firstColIsHeader", true,
            "striped", true,
            "bordered", true,
            "hoverHighlight", true);

        context.create().resource(resource, "rows/item0/cells/item0", "cellValue", "Name");
        context.create().resource(resource, "rows/item0/cells/item1", "cellValue", "Role");
        context.create().resource(resource, "rows/item1/cells/item0", "cellValue", "Alice");
        context.create().resource(resource, "rows/item1/cells/item1", "cellValue", "Developer");
        context.create().resource(resource, "rows/item2/cells/item0", "cellValue", "Bob");
        context.create().resource(resource, "rows/item2/cells/item1", "cellValue", "QA");

        model = resource.adaptTo(TableModel.class);
    }

    @Test
    void testTableModelParsesRowsAndMetadata() {
        assertNotNull(model);
        assertEquals("Team table", model.getTableTitle());
        assertTrue(model.hasTableTitle());
        assertTrue(model.isReady());
        assertTrue(model.isFirstRowIsHeader());
        assertTrue(model.isFirstColIsHeader());
        assertTrue(model.isStriped());
        assertTrue(model.isBordered());
        assertTrue(model.isHoverHighlight());
        assertEquals(3, model.getTableRows().size());
        assertEquals(2, model.getBodyRows().size());
        assertNotNull(model.getHeaderRow());
        assertEquals("cmp-table cmp-table--bordered cmp-table--striped cmp-table--hover", model.getTableCssClass());
    }
}
