# Vicinity24 Client

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 17.3.17.

## Development server

Run `npm start` for a dev server. Navigate to `http://localhost:4200/`.

Notes:
- The app uses relative API paths (`/api/v1`, `/ws`) in dev.
- The dev server proxies `/api/*` + `/ws/*` to `http://localhost:8081` via `proxy.conf.json`.
- If the backend is running in multi-tenant mode, direct API tools or custom clients should send `X-Tenant-ID` unless the backend is configured with `SETTING_USE_DEFAULT_DATABASE=true`.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
