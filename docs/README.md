# Vane documentation

```sh
nix build .#website
# Generated site is in result/; docs/build.sh is a compatibility wrapper.
```

- Feature text: `content/`, ordered by `content.toml`.
- HTML and CSS: `templates/`; Tailwind configuration: `tailwind.config.js`.
- Recipe/loot examples and icon lookup: `data/` (see its README for provenance).
- Static assets retain their original paths: `assets/`, `images/`, `wiki/`, `css/`,
  `favicon.ico` and `vane.png`. The generator copies only these entries, not its sources.
- Published, compatibility-sensitive pack archives: `resourcepacks/`.

`resourcepacks/v<version>.zip` files are versioned build inputs, **not disposable
website output**. Do not delete or overwrite published versions. Core hashes the
matching pack; Geyser converts it at build time.

The Python generator uses Markdown and Python 3.11+'s TOML parser; Nix supplies
these and the pinned standalone Tailwind compiler. It validates every local HTML
link/icon and the complete content catalog. No npm install, client JAR or server
configuration is required. Rendered HTML/CSS are not checked in. Pages builds and
deploys the Nix output, including historical resource packs at their existing URLs.

Run `nix fmt` after edits and `nix flake check` before submitting. Website generator
unit tests and the complete site build are independent flake checks. External CDN
fonts/icons remain runtime browser dependencies; the build itself uses no network.
