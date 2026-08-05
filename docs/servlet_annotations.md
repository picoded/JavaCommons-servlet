# JavaCommons-servlet Annotation Routing & Interceptor Guide

This guide explains the routing and interceptor mechanisms in `JavaCommons-servlet`. It clarifies the behaviors and technical differences between the library's core routing annotations (`@ApiPath`, `@RequestPath`), method constraints (`@RequestType`), and lifecycle hooks (`@RequestBefore`, `@RequestAfter`).

---

## At a Glance: Master Annotation Cheat-Sheet

| Annotation | Primary Use-Case | Target Scope | HTTP Method Verification | Multi-Match Execution | Exception Handling |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`@ApiPath`** | Programmatic JSON APIs consumed by client web apps. Generates client JS. | Methods only | **Ignored** (accepts all HTTP methods) | **No** (first exact match wins) | **Automated JSON wrap** (returns structured JSON maps) |
| **`@RequestPath`** | Standard page-level & hierarchical sub-page routing. | Methods & Fields | **Enforced** (filtered via `@RequestType`) | **No** (first exact match wins) | **Raw propagation** (escapes to servlet container) |
| **`@RequestType`** | HTTP method constraint (GET, POST, etc.) for a route. | Methods only | **N/A** (defines the allowed verbs) | **N/A** (declares allowed verbs list) | **N/A** |
| **`@RequestBefore`** | Pre-request hook (runs before target endpoint). | Methods only | **Ignored** (accepts all HTTP methods) | **Yes** (runs **all** matches in specificity order) | Inherits behavior of matching parent route |
| **`@RequestAfter`** | Post-request hook (runs after target endpoint). | Methods only | **Ignored** (accepts all HTTP methods) | **Yes** (runs **all** matches in specificity order) | Inherits behavior of matching parent route |

---

## 1. Routing Annotations

### `@ApiPath` (Client-Facing API Layer)
Designed for backend programmatic JSON endpoints.
* **Frontend Integration**: Automatically scanned by `AxiosApiBuilder` to generate client-side JS wrapper functions (`axiosApi.js`), allowing frontend devs to call APIs natively (e.g. `api.user.getProfile()`).
* **HTTP Method Blind**: Ignores HTTP verbs (e.g. `@RequestType` constraints are ignored during `@ApiPath` routing).
* **Automated Exception Handling**: Any exception thrown is caught internally, wrapped into an `ApiException`, and returned as a standard JSON error:
  ```json
  {"ERROR": {"message": "Error details..."}}
  ```

### `@RequestPath` (Traditional Page & Router Layer)
Acts as a traditional HTTP route handler, similar to standard servlet mapping.
* **Sub-Routers (Field Injection)**: Can target fields to build modular, hierarchical path structures:
  ```java
  @RequestPath("admin/*")
  private AdminPage adminRouter; // Mounts AdminPage routes under /admin/...
  ```
* **Enforced Verb Filtering**: Honors `@RequestType` filtering.
* **Raw Exceptions**: Standard runtime exceptions propagate directly to the servlet container (typically displaying 500 error pages or raw stack traces).

---

## 2. HTTP Method Filtering: `@RequestType`

The `@RequestType` annotation restricts an endpoint to specific HTTP methods (e.g. `GET`, `POST`, `PUT`, `DELETE`).

```java
@RequestPath("profile")
@RequestType("POST")
public void saveProfile(PrintWriter writer) { ... }
```

### Critical Behaviors & Gotchas:
1. **Requires `@RequestPath`**: `@RequestType` is **only enforced** when paired with `@RequestPath`. 
2. **Ignored on `@ApiPath`**: The `@ApiPath` lookup pipeline executes without HTTP method context, meaning any `@RequestType` constraint on an `@ApiPath` endpoint is **ignored** on the server side (though it is used by the frontend Axios generator).
3. **Ignored on Interceptors**: `@RequestBefore` and `@RequestAfter` do not respect `@RequestType`. They execute on any matching request path regardless of the HTTP method used.
4. **Default Behavior (Omitting `@RequestType`)**: If `@RequestType` is omitted from a `@RequestPath` method, it is **open to all HTTP methods** (GET, POST, PUT, DELETE, etc.).
   * *Unit Test Verification*: This default behavior and its strict enforcement when `@RequestType` is provided are explicitly validated in the servlet library test suite under `BasePage_requestType_test.java`.

---

## 3. Lifecycle Interceptors: `@RequestBefore` and `@RequestAfter`

Interceptors provide a powerful hook system to execute cross-cutting concerns (e.g., authentication, request logging, session management, or adding custom response headers).

```java
@RequestBefore("admin/*")
public void checkAdminAuth(ApiResponseMap response) {
    if (!isAdmin()) {
        halt(); // Halts further execution instantly
    }
}
```

### Key Execution Rules:

#### A. Execution Pipeline
For any incoming request, the execution sequence is:
$$\text{Matched } @RequestBefore \text{ hooks} \longrightarrow \text{Target Route (API/Request)} \longrightarrow \text{Matched } @RequestAfter \text{ hooks}$$

#### B. Multiple Interceptor Matching & Wildcards
Unlike route handlers (where only the first match is chosen), the servlet engine finds **all** matching `@RequestBefore` / `@RequestAfter` hooks and executes **every single one**.
* Wildcards (e.g. `admin/*`) and path templates (e.g. `user/:id/*`) are fully supported.

#### C. Deterministic Execution Order (Specificity Sorting)
When multiple interceptors match a request, they are sorted using the library's `sortEndpointList` algorithm:
1. **Exact match segment (Weight 0)** is prioritized over a **path variable segment (e.g. `:id`, Weight 1)**, which is prioritized over a **wildcard segment (e.g. `*`, Weight 2)**.
2. **Longer matched paths** (more segments) are prioritized over shorter ones.

> [!IMPORTANT]
> **Interceptors execute in order of specificity (most specific first, least specific last).**
> For a request to `/admin/user/profile`, the hook matching `/admin/user/profile` runs **before** `/admin/*`.

#### D. Parameter Injection
Interceptors support the same automatic parameter injection as routing methods, meaning you can request parameters such as `PrintWriter`, `ServletRequestMap`, `ApiResponseMap`, or `HttpServletRequest` directly in the method signature.

#### E. Halting Execution
Calling `halt()` on the page instance inside a `@RequestBefore` hook throws a `HaltException`. This immediately interrupts the execution pipeline, preventing the target route handler and any subsequent hooks from running.
