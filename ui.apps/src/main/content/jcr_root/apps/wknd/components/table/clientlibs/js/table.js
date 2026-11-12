/**
 * WKND Table Component — table.js
 *
 * Progressive enhancement for the table component.
 * Adds optional column sorting (click a header to sort ascending/descending).
 */
(function () {
    "use strict";

    var SELECTOR = ".cmp-table[data-sortable]";
    var HEADER_CELL_SELECTOR = ".cmp-table__header-cell";
    var SORT_ASC_CLASS = "cmp-table__header-cell--sort-asc";
    var SORT_DESC_CLASS = "cmp-table__header-cell--sort-desc";

    /**
     * Initialise sorting behaviour on a single table element.
     * @param {HTMLTableElement} table
     */
    function initSort(table) {
        var headers = table.querySelectorAll(HEADER_CELL_SELECTOR);
        headers.forEach(function (th, colIndex) {
            th.style.cursor = "pointer";
            th.setAttribute("title", "Click to sort by this column");
            th.addEventListener("click", function () {
                sortTable(table, th, colIndex);
            });
        });
    }

    /**
     * Sort table rows by the given column.
     * Toggles between ascending and descending on subsequent clicks.
     *
     * @param {HTMLTableElement} table
     * @param {HTMLTableCellElement} clickedTh
     * @param {number} colIndex
     */
    function sortTable(table, clickedTh, colIndex) {
        var isAsc = clickedTh.classList.contains(SORT_ASC_CLASS);
        var tbody = table.querySelector("tbody");
        if (!tbody) return;

        // Reset all header indicators
        table.querySelectorAll(HEADER_CELL_SELECTOR).forEach(function (th) {
            th.classList.remove(SORT_ASC_CLASS, SORT_DESC_CLASS);
        });

        // Set indicator on clicked header
        clickedTh.classList.add(isAsc ? SORT_DESC_CLASS : SORT_ASC_CLASS);

        var rows = Array.prototype.slice.call(tbody.querySelectorAll("tr"));
        rows.sort(function (a, b) {
            var cellA = getCellText(a, colIndex);
            var cellB = getCellText(b, colIndex);

            // Numeric sort when both values look like numbers
            var numA = parseFloat(cellA);
            var numB = parseFloat(cellB);
            if (!isNaN(numA) && !isNaN(numB)) {
                return isAsc ? numB - numA : numA - numB;
            }
            // Lexicographic sort
            return isAsc
                ? cellB.localeCompare(cellA)
                : cellA.localeCompare(cellB);
        });

        rows.forEach(function (row) {
            tbody.appendChild(row);
        });
    }

    /**
     * Get the trimmed text content of the nth cell in a row.
     * @param {HTMLTableRowElement} row
     * @param {number} colIndex
     * @returns {string}
     */
    function getCellText(row, colIndex) {
        var cells = row.querySelectorAll("td, th");
        var cell = cells[colIndex];
        return cell ? cell.textContent.trim() : "";
    }

    /**
     * Bootstrap all sortable tables on the page.
     */
    function init() {
        var tables = document.querySelectorAll(SELECTOR);
        tables.forEach(initSort);
    }

    // Run after DOM is ready
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
}());
