# JavaCommons-servlet Annotation Routing & Interceptor Guide

This guide explains the routing and interceptor mechanisms in `JavaCommons-servlet`. It clarifies the behaviors and technical differences between the library's core routing annotations (`@ApiPath`, `@RequestPath`), method constraints (`@RequestType`), and lifecycle hooks (`@RequestBefore`, `@RequestAfter`).

---

## At a Glance: Master Annotation Cheat-Sheet

| Annotation | Primary Use-Case | Target Scope | HTTP Method Verification | Multi-Match Execution | Exception Handling |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`@ApiPath`** | Programmatic JSON APIs consumed by client web apps. Generates client JS. | Methods only | **Enforced** (filtered via `@RequestType`) | **No** (first exact match wins) | **Automated JSON wrap** (returns structured JSON maps) |
| **`@RequestPath`** | Standard page-level & hierarchical sub-page routing. | Methods & Fields | **Enforced** (filtered via `@RequestType`) | **No** (first exact match wins) | **Raw propagation** (escapes to servlet container) |
| **`@RequestType`** | HTTP method constraint (GET, POST, etc.) for a route. | Methods only | **N/A** (defines the allowed verbs) | **N/A** (declares allowed verbs list) | **N/A** |
| **`@RequestBefore`** | Pre-request hook (runs before target endpoint). | Methods only | **Ignored** (accepts all HTTP methods) | **Yes** (runs **all** matches in specificity order) | Inherits behavior of matching parent route |
| **`@RequestAfter`** | Post-request hook (runs after target endpoint). | Methods only | **Ignored** (accepts all HTTP methods) | **Yes** (runs **all** matches in specificity order) | Inherits behavior of matching parent route |

---

## 1. Routing Annotations

### `@ApiPath` (Client-Facing API Layer)
Designed for backend programmatic JSON endpoints.
* **Frontend Integration**: Automatically scanned by `AxiosApiBuilder` to generate client-side JS wrapper functions (`axiosApi.js`), allowing frontend devs to call APIs natively (e.g. `api.user.getProfile()`).
* **Enforced Verb Filtering**: Honors `@RequestType` filtering. If an incoming request's HTTP verb does not match the `@RequestType` constraints of the endpoint, it returns a `404 Error` instead of executing.
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
1. **Fully Enforced on Route Handlers**: `@RequestType` is fully enforced when paired with either `@RequestPath` or `@ApiPath`. 
2. **Server-Side API Enforcement**: If an incoming request to an `@ApiPath` endpoint does not match its `@RequestType` constraints (e.g., executing a POST-only endpoint using a GET request), the routing engine rejects the route and returns a `404 Error`.
3. **Ignored on Interceptors**: `@RequestBefore` and `@RequestAfter` do not respect `@RequestType`. They execute on any matching request path regardless of the HTTP method used.
4. **Default Behavior (Omitting `@RequestType`)**: If `@RequestType` is omitted from an endpoint, it is **open to all HTTP methods** (GET, POST, PUT, DELETE, etc.).
   * *Unit Test Verification*: Both `@RequestPath` and `@ApiPath` verb verification behaviors (including single, multiple, and omitted constraints) are validated in the library test suite under `BasePage_requestType_test.java`.

### Multi-Verb Same-Endpoint Routing

You can map multiple distinct controller methods to the exact same route path by restricting each method with different HTTP verbs via `@RequestType`. This enables clean RESTful routing (e.g., mapping a `GET` request on `/users` to a list method, and a `POST` request on `/users` to a creation method).

```java
@RequestPath("users")
@RequestType("GET")
public void listUsers() { ... }

@RequestPath("users")
@RequestType("POST")
public void createUser() { ... }
```

#### Key Rules & Constraints:
1. **Duplicate Verbs Forbidden**: You cannot map overlapping HTTP verbs on the same route path (e.g. registering two `GET` handlers on `/users`, or a `GET` handler and a handler without any `@RequestType` constraints). Doing so will cause the scanner to fail with an `IllegalStateException: Duplicate endpoint registration` during class map initialization.
2. **Interceptors Apply Universally**: Interceptors (`@RequestBefore` and `@RequestAfter`) registered on the endpoint path (e.g. `/users`) are mapped using the `::all` wildcard suffix. They are executed on all requests matching that route path, completely ignoring the HTTP verb used.

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
