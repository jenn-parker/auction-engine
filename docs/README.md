# Static API documentation

`index.html` is a static Swagger UI entry point backed by the checked-in
`openapi.yaml` contract. It does not require the Spring Boot application,
Postgres, LocalStack, or Cognito to be running.

For local preview, serve this directory so the browser can load the YAML file:

```bash
python3 -m http.server 8000 --directory docs
```

Then open <http://localhost:8000>.

For a public portfolio preview, enable GitHub Pages for the repository and
select the `main` branch with `/docs` as the publishing folder. The resulting
site will open the Swagger UI automatically.
