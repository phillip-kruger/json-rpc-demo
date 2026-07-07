import { LitElement, html } from 'lit';
import { GreetingResource } from '@quarkiverse/json-rpc-api';

class GreetingApp extends LitElement {
    static properties = {
        _result: { state: true }
    };

    constructor() {
        super();
        this._result = '(click the button)';
    }

    async _greet() {
        this._result = await GreetingResource.hello({ name: 'Demo' });
    }

    async _createPerson() {
        const person = await GreetingResource.createPerson({ name: 'Alice', age: 30 });
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
