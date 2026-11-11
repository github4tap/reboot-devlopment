/**
 * WKND Product Listing Component
 * Asynchronously fetches and renders product blocks into a 3x3 responsive grid.
 */
(function () {
    "use strict";

    class ProductListing {
        constructor(element) {
            this.container = element;
            this.endpoint = element.dataset.endpoint || "/bin/wknd/products";
            this.limit = parseInt(element.dataset.limit, 10) || 9;
            this.grid = element.querySelector(".cmp-productlisting__grid--active");
            this.loading = element.querySelector(".cmp-productlisting__loading");
            this.errorContainer = element.querySelector(".cmp-productlisting__status--error");
            this.errorMessage = element.querySelector(".cmp-productlisting__status-message");
            this.emptyContainer = element.querySelector(".cmp-productlisting__status--empty");
            this.retryBtn = element.querySelector(".cmp-productlisting__retry-btn");

            this.init();
        }

        init() {
            if (this.retryBtn) {
                this.retryBtn.addEventListener("click", () => this.fetchProducts());
            }

            this.fetchProducts();
        }

        /**
         * Fetch product data from the backend servlet/service.
         */
        async fetchProducts() {
            this.showLoading();

            const url = new URL(this.endpoint, window.location.origin);
            url.searchParams.set("limit", this.limit);

            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 6000);

            try {
                const response = await fetch(url.toString(), {
                    method: "GET",
                    headers: {
                        "Accept": "application/json"
                    },
                    signal: controller.signal
                });

                clearTimeout(timeoutId);

                if (!response.ok) {
                    throw new Error(`HTTP error ${response.status}: ${response.statusText}`);
                }

                const data = await response.json();

                if (!Array.isArray(data)) {
                    throw new Error("Invalid response format: Expected JSON array of products.");
                }

                this.renderProducts(data);
            } catch (error) {
                clearTimeout(timeoutId);
                console.error("[ProductListing] Fetch error:", error);
                this.showError(error.name === "AbortError"
                    ? "Request timed out while loading products. Please try again."
                    : "Unable to load products. Please check the backend connection.");
            }
        }

        /**
         * Safely render product cards to prevent XSS.
         */
        renderProducts(products) {
            this.hideStatus();

            if (!this.grid) {
                return;
            }

            // Slice to exact limit (e.g. 9 for 3x3 blocks)
            const displayProducts = products.slice(0, this.limit);

            if (displayProducts.length === 0) {
                this.showEmpty();
                return;
            }

            // Clear previous cards
            this.grid.innerHTML = "";

            const fragment = document.createDocumentFragment();

            displayProducts.forEach((item) => {
                const card = document.createElement("article");
                card.className = "cmp-productlisting__card";
                card.setAttribute("tabindex", "0");
                card.setAttribute("role", "article");
                card.setAttribute("aria-label", item.name || "Product item");

                // Line 1: Product ID
                const idLine = document.createElement("p");
                idLine.className = "cmp-productlisting__card-line cmp-productlisting__card-id";
                idLine.textContent = `Product ID: ${item.id !== undefined ? item.id : ""}`;

                // Line 2: Product Name
                const nameLine = document.createElement("p");
                nameLine.className = "cmp-productlisting__card-line cmp-productlisting__card-name";
                nameLine.textContent = `Product Name :${item.name || ""}`;

                // Line 3: Price
                const priceLine = document.createElement("p");
                priceLine.className = "cmp-productlisting__card-line cmp-productlisting__card-price";
                const priceVal = typeof item.price === "number" ? item.price.toFixed(2) : (item.price || "");
                const currencyVal = item.currency || "";
                priceLine.textContent = `Price : ${priceVal}${currencyVal}`;

                card.appendChild(idLine);
                card.appendChild(nameLine);
                card.appendChild(priceLine);

                fragment.appendChild(card);
            });

            this.grid.appendChild(fragment);
            this.grid.style.display = "grid";
        }

        showLoading() {
            if (this.loading) this.loading.style.display = "block";
            if (this.grid) this.grid.style.display = "none";
            if (this.errorContainer) this.errorContainer.style.display = "none";
            if (this.emptyContainer) this.emptyContainer.style.display = "none";
        }

        hideStatus() {
            if (this.loading) this.loading.style.display = "none";
            if (this.errorContainer) this.errorContainer.style.display = "none";
            if (this.emptyContainer) this.emptyContainer.style.display = "none";
        }

        showError(message) {
            if (this.loading) this.loading.style.display = "none";
            if (this.grid) this.grid.style.display = "none";
            if (this.emptyContainer) this.emptyContainer.style.display = "none";

            if (this.errorContainer) {
                if (this.errorMessage) {
                    this.errorMessage.textContent = message;
                }
                this.errorContainer.style.display = "block";
            }
        }

        showEmpty() {
            if (this.loading) this.loading.style.display = "none";
            if (this.grid) this.grid.style.display = "none";
            if (this.errorContainer) this.errorContainer.style.display = "none";
            if (this.emptyContainer) this.emptyContainer.style.display = "block";
        }
    }

    /**
     * Auto-initialize all component instances on DOM ready.
     */
    function initAll() {
        const elements = document.querySelectorAll('[data-cmp-is="productlisting"]');
        elements.forEach((el) => {
            if (!el.__productListingInitialized) {
                el.__productListingInitialized = new ProductListing(el);
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initAll);
    } else {
        initAll();
    }
})();
