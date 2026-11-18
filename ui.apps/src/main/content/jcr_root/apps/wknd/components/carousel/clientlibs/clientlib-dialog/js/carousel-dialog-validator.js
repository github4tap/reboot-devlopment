/**
 * Granite UI validator to enforce max slides restriction in Carousel dialog.
 */
(function ($, Granite) {
    "use strict";

    const MAX_ITEMS = 8;

    $(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
        selector: "[data-foundation-validation~='carousel-max-items']",
        validate: function (element) {
            const multifield = element.closest("coral-multifield");
            if (!multifield) {
                return null;
            }

            const itemsCount = multifield.items ? multifield.items.length : 0;
            if (itemsCount > MAX_ITEMS) {
                return Granite.I18n.get("Maximum {0} slides allowed for optimal web performance and user experience.", [MAX_ITEMS]);
            }
            return null;
        }
    });

    $(document).on("coral-multifield:add", function (e) {
        const multifield = e.target;
        if (multifield.getAttribute("data-foundation-validation") === "carousel-max-items" ||
            multifield.getAttribute("validation") === "carousel-max-items") {
            const count = multifield.items.length;
            if (count > MAX_ITEMS) {
                const ui = $(window).adaptTo("foundation-ui");
                if (ui) {
                    ui.alert("Performance Warning",
                        "Best practice: A carousel should contain at most " + MAX_ITEMS + " slides to avoid severe Core Web Vitals (LCP) performance hits and user drop-off.",
                        "warning");
                }
            }
        }
    });
})(Granite.$, Granite);

