# JavaCommons-servlet

`JavaCommons-servlet` is the web and routing layer of the `JavaCommons` library stack. It manages the server's menu, routing incoming web requests dynamically to controller classes via custom annotations, providing structured response helpers, and generating client-side JavaScript APIs.

---

## Core Features

* **Annotation-based Routing**: Route incoming HTTP requests directly to controller methods using annotations instead of mapping files.
* **Hierarchical Nested Routing**: Fields can be annotated to mount recursive sub-page routers dynamically to keep controller code modular and highly structured.
* **Dynamic Axios client-side generation**: Scans your backend endpoints to compile frontend-consumable JavaScript API client wrappers automatically.
* **Type-Safe Input/Output Mapping**: Supports automatic binding of common parameters (e.g. `PrintWriter`, `ServletRequestMap`, `ApiResponseMap`) and serializes returned Map results directly to JSON.

---

## Documentation

To help developers get up to speed with the routing system and understand the behaviors of standard servlet annotations, see the dedicated guides:

* [Annotation Routing Guide (ApiPath vs RequestPath)](./docs/servlet_annotations.md) — A comprehensive guide explaining the difference between `@ApiPath` and `@RequestPath` annotations, their targets, execution pipelines, and error handling.
