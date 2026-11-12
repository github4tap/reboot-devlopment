/*
 * Copyright 2024 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.aem.guides.wknd.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model for the custom WKND Table component.
 *
 * <p>Reads the dialog-authored properties and child nodes (rows/cells multifield)
 * and exposes them to the HTL rendering script.</p>
 *
 * <h2>JCR content structure produced by the dialog</h2>
 * <pre>
 * table [cq:Component]
 *   @tableTitle       String
 *   @tableCaption     String
 *   @numberOfColumns  Long
 *   @firstRowIsHeader Boolean
 *   @firstColIsHeader Boolean
 *   @striped          Boolean
 *   @bordered         Boolean
 *   @hoverHighlight   Boolean
 *   columnHeaders [nt:unstructured]  (multifield)
 *     item0 [nt:unstructured]
 *       @headerText String
 *     item1 ...
 *   rows [nt:unstructured]  (multifield)
 *     item0 [nt:unstructured]
 *       cells [nt:unstructured] (nested multifield)
 *         item0 [nt:unstructured]
 *           @cellValue String
 *         item1 ...
 *     item1 ...
 * </pre>
 */
@Model(adaptables = Resource.class, adapters = TableModel.class, resourceType = "wknd/components/table")
public class TableModel {

    private static final Logger LOG = LoggerFactory.getLogger(TableModel.class);

    // ── Component properties ─────────────────────────────────────────────────

    @ValueMapValue(name = "tableTitle")
    @Default(values = "")
    private String tableTitle;

    @ValueMapValue(name = "tableCaption")
    @Default(values = "")
    private String tableCaption;

    @ValueMapValue(name = "numberOfColumns")
    @Default(longValues = 3L)
    private long numberOfColumns;

    @ValueMapValue(name = "firstRowIsHeader")
    @Default(booleanValues = false)
    private boolean firstRowIsHeader;

    @ValueMapValue(name = "firstColIsHeader")
    @Default(booleanValues = false)
    private boolean firstColIsHeader;

    @ValueMapValue(name = "striped")
    @Default(booleanValues = false)
    private boolean striped;

    @ValueMapValue(name = "bordered")
    @Default(booleanValues = true)
    private boolean bordered;

    @ValueMapValue(name = "hoverHighlight")
    @Default(booleanValues = true)
    private boolean hoverHighlight;

    // ── Injected resource for child-node traversal ────────────────────────────

    @SlingObject
    private Resource resource;

    // ── Computed fields ───────────────────────────────────────────────────────

    /** Parsed column headers from the "columnHeaders" multifield. */
    private List<String> columnHeaders = new ArrayList<>();

    /** Parsed rows from the "rows" multifield. Each row is a list of cell strings. */
    private List<List<String>> rows = new ArrayList<>();

    // ── Inner classes ─────────────────────────────────────────────────────────

    /**
     * Represents a single table row with its cells and whether it should be
     * rendered as a header row (&lt;thead&gt; / &lt;th&gt; elements).
     */
    public static class TableRow {
        private final List<String> cells;
        private final boolean headerRow;
        private final boolean firstColIsHeader;
        private final int index;

        public TableRow(List<String> cells, boolean headerRow, boolean firstColIsHeader, int index) {
            this.cells = Collections.unmodifiableList(cells);
            this.headerRow = headerRow;
            this.firstColIsHeader = firstColIsHeader;
            this.index = index;
        }

        public List<String> getCells() {
            return cells;
        }

        public boolean isHeaderRow() {
            return headerRow;
        }

        public boolean isFirstColIsHeader() {
            return firstColIsHeader;
        }

        public int getIndex() {
            return index;
        }

        /** Returns true when this row sits at an even index (0, 2, 4 …). */
        public boolean isEven() {
            return index % 2 == 0;
        }

        /**
         * Returns the CSS class string for the &lt;tr&gt; element,
         * including even/odd modifier.
         */
        public String getRowCssClass() {
            return isEven()
                    ? "cmp-table__row cmp-table__row--even"
                    : "cmp-table__row cmp-table__row--odd";
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PostConstruct
    protected void init() {
        parseColumnHeaders();
        parseRows();
    }

    /**
     * Reads child nodes under the "columnHeaders" multifield resource and builds
     * the {@link #columnHeaders} list.
     */
    private void parseColumnHeaders() {
        Resource colHeadersResource = resource.getChild("columnHeaders");
        if (colHeadersResource == null) {
            return;
        }
        for (Resource item : colHeadersResource.getChildren()) {
            String headerText = item.getValueMap().get("headerText", String.class);
            columnHeaders.add(StringUtils.defaultString(headerText));
        }
    }

    /**
     * Reads child nodes under the "rows" multifield resource and builds the
     * {@link #rows} list (list of cell-value lists).
     */
    private void parseRows() {
        Resource rowsResource = resource.getChild("rows");
        if (rowsResource == null) {
            return;
        }
        for (Resource rowItem : rowsResource.getChildren()) {
            List<String> cells = new ArrayList<>();
            Resource cellsResource = rowItem.getChild("cells");
            if (cellsResource != null) {
                for (Resource cellItem : cellsResource.getChildren()) {
                    String cellValue = cellItem.getValueMap().get("cellValue", String.class);
                    cells.add(StringUtils.defaultString(cellValue));
                }
            }
            rows.add(cells);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getTableTitle() {
        return tableTitle;
    }

    public boolean hasTableTitle() {
        return StringUtils.isNotBlank(tableTitle);
    }

    public String getTableCaption() {
        return tableCaption;
    }

    public boolean hasTableCaption() {
        return StringUtils.isNotBlank(tableCaption);
    }

    public long getNumberOfColumns() {
        return numberOfColumns;
    }

    public boolean isFirstRowIsHeader() {
        return firstRowIsHeader;
    }

    public boolean isFirstColIsHeader() {
        return firstColIsHeader;
    }

    public boolean isStriped() {
        return striped;
    }

    public boolean isBordered() {
        return bordered;
    }

    public boolean isHoverHighlight() {
        return hoverHighlight;
    }

    /** Returns the list of column header labels (from the "Column Headers" multifield). */
    public List<String> getColumnHeaders() {
        return Collections.unmodifiableList(columnHeaders);
    }

    /** Returns true when at least one column header label has been defined. */
    public boolean hasColumnHeaders() {
        return columnHeaders.stream().anyMatch(StringUtils::isNotBlank);
    }

    /**
     * Returns all rows as {@link TableRow} objects.
     * The first row is flagged as a header row when {@link #firstRowIsHeader} is true.
     */
    public List<TableRow> getTableRows() {
        List<TableRow> tableRows = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            boolean isHeaderRow = (i == 0) && firstRowIsHeader;
            tableRows.add(new TableRow(rows.get(i), isHeaderRow, firstColIsHeader, i));
        }
        return Collections.unmodifiableList(tableRows);
    }

    /**
     * Convenience: returns only the body rows (all rows when firstRowIsHeader=false,
     * or all rows after the first when firstRowIsHeader=true).
     */
    public List<TableRow> getBodyRows() {
        List<TableRow> all = getTableRows();
        if (firstRowIsHeader && !all.isEmpty()) {
            return all.subList(1, all.size());
        }
        return all;
    }

    /**
     * Returns the first row as a header row, or null if firstRowIsHeader=false or no rows exist.
     */
    public TableRow getHeaderRow() {
        List<TableRow> all = getTableRows();
        if (firstRowIsHeader && !all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    /**
     * Returns true when the component has sufficient data to render a meaningful table.
     */
    public boolean isReady() {
        return !rows.isEmpty() || hasColumnHeaders();
    }

    /**
     * Builds the CSS class string for the &lt;table&gt; element based on dialog settings.
     */
    public String getTableCssClass() {
        StringBuilder sb = new StringBuilder("cmp-table");
        if (bordered) {
            sb.append(" cmp-table--bordered");
        }
        if (striped) {
            sb.append(" cmp-table--striped");
        }
        if (hoverHighlight) {
            sb.append(" cmp-table--hover");
        }
        return sb.toString();
    }
}
