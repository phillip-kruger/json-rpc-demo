# Demo Playbook: quarkus-json-rpc

## Pre-demo Setup

Have a basic Quarkus app ready (just `quarkus-arc` + optionally a REST endpoint). Make sure `quarkus-json-rpc` is **not** added yet — you'll add it live.

Add this to `application.properties` ahead of time so the Dev UI JSON-RPC log is visible:
```properties
quarkus.dev-ui.show-json-rpc-log=true
```

Start the app with `mvn quarkus:dev`.

---

## Act 1: Dev UI uses JSON-RPC under the hood

**Goal:** Show the audience that JSON-RPC is already happening in their Quarkus app — they just don't see it.

1. Open the Dev UI at `http://localhost:8080/q/dev-ui`
2. Click around — Extensions, Configuration, etc.
3. Open the **Dev UI Log** tab in the footer bar (enabled by the config above)
4. Point out the raw JSON-RPC messages flowing — every click in Dev UI generates JSON-RPC requests/responses over WebSocket
5. **Talking point:** "Every Quarkus app in dev mode already speaks JSON-RPC. You've been using it without knowing."

---

## Act 2: Add quarkus-json-rpc and write your first endpoint

**Goal:** Show how trivially simple it is to expose your own JSON-RPC API.

1. Add the dependency (live reload picks it up):
   ```xml
   <dependency>
       <groupId>io.quarkiverse.json-rpc</groupId>
       <artifactId>quarkus-json-rpc</artifactId>
       <version>1.1.2</version>
   </dependency>
   ```

2. Create a simple service class:
   ```java
   package com.example;

   import io.quarkiverse.jsonrpc.api.JsonRPCApi;

   @JsonRPCApi
   public class GreetingService {

       public String hello(String name) {
           return "Hello " + name;
       }
   }
   ```

3. Wait for live reload, then show in Dev UI — the extension now appears with an interactive method browser/tester. Click **GreetingService#hello**, enter a name, and invoke it.

4. **Talking point:** "One annotation, zero boilerplate. The method is discovered at build time via Jandex — this works in native image too."

---

## Act 3: Add streaming with Multi

**Goal:** Show the subscription protocol — the most interesting JSON-RPC pattern.

1. Add a streaming method to the same class:
   ```java
   import io.smallrye.mutiny.Multi;
   import java.time.Duration;

   public Multi<String> countdown(String name) {
       return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
               .select().first(5)
               .onItem().transform(n -> "(" + (5 - n) + ") Hello " + name);
   }
   ```

2. Walk through what happens:
   - **ACK response** with subscription ID
   - **Notification stream** — items arrive as `method: "subscription"` notifications
   - **Completion notification** when the Multi completes

3. **Optional:** Show unsubscribe by starting a non-terminating Multi, then sending:
   ```json
   {"jsonrpc":"2.0","id":3,"method":"unsubscribe","params":{"subscription":"<id>"}}
   ```

4. **Talking point:** "This is stateful behavior layered on stateless JSON-RPC — same pattern MCP uses for its sessions."

---

## Act 4: POJO parameters and return types

**Goal:** Show that JSON-RPC isn't limited to strings — complex objects are serialized automatically via Jackson.

1. Create a record:
   ```java
   package com.example;

   public record Person(String name, int age) {}
   ```

2. Add methods that accept and return the record:
   ```java
   public Person createPerson(String name, int age) {
       return new Person(name, age);
   }

   public String greetPerson(Person person) {
       return "Hello " + person.getName() + ", age " + person.getAge();
   }
   ```

3. Invoke `createPerson` from the tester with params `name=Alice` and `age=30` — show the JSON object response:
   ```json
   {"jsonrpc":"2.0","id":3,"result":{"name":"Alice","age":30}}
   ```

4. Invoke `greetPerson` — show sending a POJO as a named parameter:
   ```json
   {"jsonrpc":"2.0","id":4,"method":"GreetingService#greetPerson","params":{"person":{"name":"Bob","age":25}}}
   ```
   Response:
   ```json
   {"jsonrpc":"2.0","id":4,"result":"Hello Bob, age 25"}
   ```

5. Browse to `http://localhost:8080/json-rpc/openrpc.json` — point out that the `Person` schema is generated automatically with JSON Schema types for `name` (string) and `age` (integer).

6. **Talking point:** "Records, POJOs, nested objects, collections, Java time types, Optional — anything Jackson can handle. The OpenRPC document generates the full JSON Schema so clients can be generated automatically."

---

## Act 5: Generate the JavaScript client

**Goal:** Show that the extension generates a typed JS proxy for free.

1. Add to `application.properties`:
   ```properties
   quarkus.json-rpc.js-client.enabled=true
   ```

2. Add `quarkus-web-dependency-locator` and `lit` dependencies:
   ```xml
   <dependency>
       <groupId>io.quarkus</groupId>
       <artifactId>quarkus-web-dependency-locator</artifactId>
   </dependency>
   <dependency>
       <groupId>org.mvnpm</groupId>
       <artifactId>lit</artifactId>
       <version>3.3.3</version>
   </dependency>
   ```

3. Show the generated files after live reload (browse to them or `curl`):
   - `http://localhost:8080/_static/quarkus-json-rpc/jsonrpc-client.js` — the client library
   - `http://localhost:8080/_static/quarkus-json-rpc-api/jsonrpc-api.js` — the typed proxy with your `GreetingService` exports

4. Create a minimal `src/main/resources/web/index.html`:
   ```html
   <!DOCTYPE html>
   <html>
   <head>
       <title>JSON-RPC Demo</title>
       {#bundle /}
   </head>
   <body>
       <greeting-app></greeting-app>
   </body>
   </html>
   ```

5. Create `src/main/resources/web/app/greeting-app.js`:
   ```javascript
   import { LitElement, html } from 'lit';
   import { GreetingService } from '@quarkiverse/json-rpc-api';

   class GreetingApp extends LitElement {
       static properties = {
           _result: { state: true }
       };

       constructor() {
           super();
           this._result = '(click the button)';
       }

       async _greet() {
           this._result = await GreetingService.hello({ name: 'Demo' });
       }

       async _createPerson() {
           const person = await GreetingService.createPerson({ name: 'Alice', age: 30 });
           this._result = JSON.stringify(person, null, 2);
       }

       render() {
           return html`
               <button @click=${this._greet}>Call hello</button>
               <button @click=${this._createPerson}>Create Person</button>
               <pre>${this._result}</pre>
           `;
       }
   }
   customElements.define('greeting-app', GreetingApp);
   ```

6. Open `http://localhost:8080` — click the button, show the result.

7. **Talking point:** "The generated client gives you `await GreetingService.hello(...)` — typed, auto-connected, with reconnect built in. Streaming methods return subscription objects with `onItem`/`onComplete`/`cancel`."

---

## Timing Guide

| Act | Duration | What to show |
|-----|----------|--------------|
| 1. Dev UI log | 2 min | JSON-RPC is already there |
| 2. First endpoint | 3 min | `@JsonRPCApi`, Dev UI tester, footer log |
| 3. Streaming | 3 min | Multi subscription protocol |
| 4. POJOs | 3 min | Object params/returns, OpenRPC schema |
| 5. JS client | 4 min | Generated proxy, Lit component |
| **Total** | **~15 min** | |

---

## Things to have ready

- Code snippets pre-typed in a scratch file so you can copy-paste rather than type live
- Browser with Dev UI already open in one tab
- The `application.properties` config line for the Dev UI log already set before starting
