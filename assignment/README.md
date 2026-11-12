# AEM Product Listing Component - Technical Evaluation & Interview Submission

> **Tapasa Technical Assessment Submission**  
> **Component:** Product Listing 3x3 Grid Component (`wknd/components/productlisting`)  
> **Platform:** Adobe Experience Manager (AEM as a Cloud Service / AEM 6.5+)  
> **Stack:** Java 11/21, OSGi R7, Apache Sling Models, HTL (Sightly), Coral UI 3 Dialogs, CSS Grid (BEM), Vanilla ES6 JS, JUnit 5 + AEM Mocks.

---

## 1. Executive Summary & Deliverables

This solution delivers an enterprise-grade AEM component designed to render a **3x3 product catalog grid** consumed asynchronously from a backend endpoint.

### Key Deliverables:
1. **Backend OSGi Service (`ProductListingService` & `ProductListingServiceImpl`):**
   - Configurable via OSGi Metatype (`ProductListingServiceConfig`).
   - Fetches product data from external REST endpoints with timeout management (`HttpURLConnection`).
   - Resilient fallback to the supplied 10-product seed catalog if external systems are unreachable.
   - Slices results to the requested limit (e.g., 9 items for the 3x3 grid).
2. **Backend Sling Servlet (`ProductListingServlet`):**
   - Dual-registered via **Resource Type** (`wknd/components/productlisting`, selectors `products`, extension `json`) and **Direct Path** (`/bin/wknd/products`).
   - Handles `limit` query parameters with validation and bounds checking.
   - Outputs standards-compliant `application/json;charset=UTF-8` with HTTP cache headers.
   - Graceful JSON error handling with appropriate HTTP status codes (200, 400, 500).
3. **Sling Model (`ProductListingModel`):**
   - Adaptable from both `Resource` and `SlingHttpServletRequest`.
   - Injects authored dialog values (Title, Endpoint, Limit, Columns) with default fallbacks.
   - Exposes computed CSS grid layout styles and optional server-side initial data for progressive enhancement and SEO.
4. **Component Dialog (`_cq_dialog/.content.xml`):**
   - Granite / Coral UI 3 dialog allowing content authors to configure the title, endpoint, item limit, and grid column count.
5. **HTL / Sightly Template (`productlisting.html`):**
   - Clean, secure HTML5 markup following Core Component BEM conventions.
   - Context-aware XSS escaping (`context='uri'`, `context='styleToken'`).
   - Accessible ARIA attributes (`aria-live="polite"`, `role="region"`, `aria-busy="true"`).
   - Built-in loading skeleton placeholders (3x3 shimmer cards) and retry-enabled error states.
6. **Frontend ClientLib (CSS & JS):**
   - **CSS (`productlisting.css`):** Exact match to the specification screenshot: dark teal rectangular cards (`#135d79`), centered white typography, responsive 3x3 grid collapsing gracefully to 2 columns on tablets and 1 column on mobile.
   - **JS (`productlisting.js`):** Modular, vanilla ES6 class with IIFE isolation. Safe DOM generation using `document.createElement` (100% XSS immune), request timeout abort controllers, and error retry handlers.
7. **Comprehensive Unit Testing Suite:**
   - 16 tests across Service, Servlet, and Sling Model using **JUnit 5**, **Mockito**, and **AEM Mocks** (`io.wcm.testing.aem-mock.junit5`).
   - **100% passing test execution**.
8. **Interviewer Evaluation Package & Interactive Demo:**
   - Standalone demo (`export/demo/index.html`) ready to inspect in any browser without needing a running AEM instance.
   - Ready-to-deploy AEM package zip (`wknd.ui.apps-1.0.0-SNAPSHOT.zip`).

---

## 2. Architecture & Data Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Browser
    participant HTL as HTL (productlisting.html)
    participant JS as ClientLib JS (productlisting.js)
    participant Servlet as ProductListingServlet (/bin/wknd/products)
    participant Service as ProductListingService (OSGi)
    participant Remote as External REST API / Local Seed

    User->>HTL: Requests Page containing Component
    HTL-->>User: Renders Title + 3x3 Shimmer Skeleton + data-endpoint
    Note over User,JS: DOM Ready: productlisting.js initializes
    JS->>Servlet: HTTP GET /bin/wknd/products?limit=9
    Servlet->>Service: getProductsAsJson(limit=9)
    alt External API configured
        Service->>Remote: HTTP GET (with timeouts)
        Remote-->>Service: JSON Payload
    else Blank or Network Failure
        Service->>Service: Fallback to local 10-product seed catalog
    end
    Service->>Service: Slice to top 9 products & serialize via Gson
    Service-->>Servlet: JSON Array string
    Servlet-->>JS: HTTP 200 (application/json)
    JS->>JS: Build DOM cards safely (document.createElement)
    JS-->>User: Renders 3x3 Teal Product Cards in DOM
```

---

## 3. Best Practices & Design Decisions

| Category | Best Practice Followed | Rationale |
| :--- | :--- | :--- |
| **OSGi Service Design** | Interface-based service definition with separate implementation in `.impl`. | Follows Dependency Inversion Principle; enables easy mocking and future endpoint swaps. |
| **OSGi Configuration** | Metatype `@ObjectClassDefinition` via `@Designate`. | Enables zero-downtime runtime configuration through Felix Web Console and AEM Cloud Manager OSGi configs. |
| **Sling Servlet** | ResourceType registration as primary, Path registration as secondary. | Best practice in modern AEM to support dispatcher caching and resource-level access control. |
| **XSS Prevention** | DOM nodes created via `document.createElement` and `textContent`. HTL contexts strictly applied (`@ context='uri'`). | Eliminates vulnerabilities from unescaped product names or malicious strings injected via API. |
| **Progressive Enhancement** | Sling Model provides optional server-side fallback dataset alongside client-side AJAX. | Allows crawlers and search engines to index products while maintaining responsive client-side hydration. |
| **CSS Grid & BEM** | Pure CSS Grid `repeat(3, 1fr)` with BEM naming (`cmp-productlisting__*`). | Pixel-perfect alignment matching screenshot; mobile-friendly responsive breakpoints without layout shift. |
| **Resilience & Fault Tolerance** | Timeout abort controllers on client; connect/read timeouts + automatic fallback to seed data on backend. | Protects the user experience so the page never hangs or displays broken markup. |
| **Unit Testing** | AEM Mocks (`io.wcm.testing.aem-mock.junit5`) + Mockito. | Tests real OSGi service injection, request lifecycle, and Sling Model adaptation without starting an AEM server. |

---

## 4. Source Files Created / Modified

### Backend (Java / OSGi / Sling)
- `core/src/main/java/.../dto/Product.java`: Product DTO with `@SerializedName` annotations and formatted price helper.
- `core/src/main/java/.../services/ProductListingService.java`: OSGi Service interface.
- `core/src/main/java/.../services/config/ProductListingServiceConfig.java`: OSGi Metatype configuration definition.
- `core/src/main/java/.../services/impl/ProductListingServiceImpl.java`: Service implementation with REST caller, Gson serialization, and fallback seed catalog.
- `core/src/main/java/.../servlets/ProductListingServlet.java`: Sling Servlet mounted at `/bin/wknd/products` and `wknd/components/productlisting.products.json`.
- `core/src/main/java/.../models/ProductListingModel.java`: Sling Model injecting component dialog configuration and exposing grid styling.

### Frontend (HTL / ClientLibs / Dialog)
- `ui.apps/src/.../components/productlisting/.content.xml`: Component registration under `WKND Site - Content`.
- `ui.apps/src/.../components/productlisting/_cq_dialog/.content.xml`: Granite / Coral UI 3 authoring dialog.
- `ui.apps/src/.../components/productlisting/_cq_editConfig.xml`: Edit configuration.
- `ui.apps/src/.../components/productlisting/productlisting.html`: HTL rendering template.
- `ui.apps/src/.../components/productlisting/clientlibs/.content.xml`: ClientLib folder registration (`wknd.productlisting`, `wknd.site`).
- `ui.apps/src/.../components/productlisting/clientlibs/css/productlisting.css`: 3x3 Grid, teal block cards, loading skeleton shimmer, responsive breakpoints.
- `ui.apps/src/.../components/productlisting/clientlibs/js/productlisting.js`: Asynchronous fetch, abort controller, error handling, XSS-safe DOM builder.

### Unit Tests
- `core/src/test/java/.../services/ProductListingServiceTest.java`: Tests limit slicing (9 items for 3x3), JSON serialization, network failure fallbacks, DTO methods.
- `core/src/test/java/.../servlets/ProductListingServletTest.java`: Tests servlet GET responses, HTTP headers, query parameter parsing, and 400 error handling.
- `core/src/test/java/.../models/ProductListingModelTest.java`: Tests Sling Model property injection and defaults using `AemContext`.

---

## 5. Verification & Test Evidence

### Maven Test Execution Summary:
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.adobe.aem.guides.wknd.core.schedulers.SimpleScheduledTaskTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.adobe.aem.guides.wknd.core.filters.LoggingFilterTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.adobe.aem.guides.wknd.core.models.HelloWorldModelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.adobe.aem.guides.wknd.core.models.ProductListingModelTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.adobe.aem.guides.wknd.core.models.TableModelTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.adobe.aem.guides.wknd.core.services.ProductListingServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.adobe.aem.guides.wknd.core.listeners.SimpleResourceListenerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.adobe.aem.guides.wknd.core.servlets.SimpleServletTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.adobe.aem.guides.wknd.core.servlets.ProductListingServletTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

---

## 6. How the Interview Panel Can Evaluate This Work

### Option A: 1-Click Interactive Evaluation Demo (Zero setup required)
An interactive standalone preview is packaged in `export/demo/index.html`.
You can open it directly in Google Chrome, Edge, Safari, or Firefox:
```bash
# Open directly in your browser:
xdg-open export/demo/index.html   # On Linux
# or
open export/demo/index.html       # On macOS
# or start a quick local server:
cd export/demo && python3 -m http.server 8080
```
**Features in the Interactive Demo:**
- **Exact Visual Match:** 3x3 Grid with teal cards (`#135d79`), centered white text lines:
  - `Product ID: <id>`
  - `Product Name :<name>`
  - `Price : <price><currency>`
- **Live Interceptor:** Intercepts `/bin/wknd/products` and demonstrates the asynchronous loading skeleton before rendering.
- **Interviewer Toolbar:** 
  - Change grid slices (9 items for 3x3 default, 6 items, 3 items, or 10 items).
  - Simulate 500 backend failure with auto-retry.
  - Click any card to open a modal displaying detailed JSON payload and image attributes.

### Option B: Execute Maven Unit Tests
To run the automated test suite:
```bash
cd core
mvn test
```

### Option C: Build and Deploy to AEM
To build all OSGi bundles and content packages:
```bash
mvn clean install -PautoInstallSinglePackage
```
To deploy to a running local AEM author instance on `http://localhost:4502`:
```bash
mvn clean install -PautoInstallPackage
```
Then navigate to `/content/wknd/us/en.html` or inspect the component in `/editor.html/content/wknd/us/en.html`.

