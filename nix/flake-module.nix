{
  inputs,
  self,
  ...
}:
{
  imports = [
    inputs.devshell.flakeModule
    inputs.treefmt-nix.flakeModule
  ];

  perSystem =
    {
      pkgs,
      config,
      system,
      ...
    }:
    let
      toolchain = import ./toolchain.nix { inherit pkgs; };
      vane = pkgs.callPackage ./plugins.nix { inherit (toolchain) gradle jdk; };
      bridge = pkgs.callPackage ./bridge.nix { };
      website = pkgs.callPackage ./website.nix { };
      paper = pkgs.callPackage ./paper.nix { inherit (toolchain) jdk; };
      apps = import ./apps.nix {
        inherit pkgs paper;
        inherit (toolchain) jdk;
        plugins = vane;
      };
      release = pkgs.callPackage ./release.nix {
        plugins = vane;
        bridge = bridge.bridge;
        revision = self.rev or null;
      };
      publish =
        destination:
        pkgs.writeShellApplication {
          name = "publish-${destination}";
          runtimeInputs = [
            pkgs.gh
            (pkgs.python3.withPackages (ps: [ ps.requests ]))
          ];
          text = ''exec python ${./scripts/publish.py} ${destination} --assets ${release} "$@"'';
        };
      commitlintConfig = pkgs.writeText "commitlint.cjs" ''
        module.exports = {extends: ["${pkgs.commitlint}/lib/node_modules/@commitlint/root/@commitlint/config-conventional/lib/index.js"], rules: {"body-max-line-length": [0]}};
      '';
      lintCommits = pkgs.writeShellApplication {
        name = "lint-commits";
        runtimeInputs = [
          pkgs.git
          pkgs.commitlint
        ];
        text = ''
          if [[ $# -ne 2 ]]; then echo 'Usage: lint-commits BASE HEAD' >&2; exit 2; fi
          revisions=$(git rev-list --no-merges --reverse "$1..$2")
          while IFS= read -r revision; do
            [[ -n "$revision" ]] || continue
            git show -s --format=%B "$revision" | commitlint --config ${commitlintConfig}
          done <<< "$revisions"
        '';
      };
    in
    {
      _module.args.pkgs = import inputs.nixpkgs {
        inherit system;
        config.allowUnfreePredicate = pkg: inputs.nixpkgs.lib.getName pkg == "vane-paper-runtime";
      };
      packages.default = vane;
      packages.plugins = vane;
      packages.bridge = bridge.bridge;
      packages.website = website;
      packages.paper = paper;
      packages.release = release;
      apps.publish-github = {
        type = "app";
        program = "${publish "github"}/bin/publish-github";
        meta.description = "Verify release assets and preview/create a GitHub draft";
      };
      apps.publish-modrinth = {
        type = "app";
        program = "${publish "modrinth"}/bin/publish-modrinth";
        meta.description = "Verify release assets and preview/publish Modrinth variants";
      };
      apps.lint-commits = {
        type = "app";
        program = "${lintCommits}/bin/lint-commits";
        meta.description = "Lint a commit range with pinned Conventional Commits rules";
      };
      apps.testserver = {
        type = "app";
        program = "${apps.testserver}/bin/testserver";
        meta.description = "Launch a disposable Paper server with explicit EULA opt-in";
      };
      checks =
        (import ./checks.nix {
          inherit pkgs website;
          inherit (pkgs) lib;
          inherit (toolchain) jdk;
          plugins = vane;
          bridgePython = bridge.testPython;
        })
        // {
          inherit release;
        };
      apps.update-gradle-deps = {
        type = "app";
        program = "${vane.mitmCache.updateScript}";
        meta.description = "Capture and hash Gradle dependencies (network-enabled maintenance)";
      };
      devshells.default = {
        name = "vane";
        packages = [
          toolchain.gradle.unwrapped
          toolchain.jdk
          config.treefmt.build.wrapper
          bridge.testPython
        ];
        env = [
          {
            name = "JAVA_HOME";
            value = "${toolchain.jdk}";
          }
          {
            name = "TREEFMT_WALK";
            # Colocated checkouts otherwise select Git and walk nested jj workspaces.
            # If jj is unavailable, fail rather than silently falling back to Git.
            eval = ''$(if [[ -e "$PRJ_ROOT/.jj" ]]; then echo jujutsu; else echo auto; fi)'';
          }
        ];
        commands = [
          {
            name = "bridge-test";
            help = "Test the Python bridge working tree";
            command = ''PYTHONPATH="$PRJ_ROOT/vane-busybar/bridge" python -m pytest "$PRJ_ROOT/vane-busybar/bridge/tests" "$@"'';
          }
          {
            name = "build";
            help = "Incrementally build plugins in this checkout";
            command = "gradle build \"$@\"";
          }
          {
            name = "check";
            help = "Run all sandboxed flake checks";
            command = "nix flake check -L \"$@\"";
          }
          {
            name = "testserver";
            help = "Build dirty sources and start a disposable loopback server";
            command = ''cd "$PRJ_ROOT"; exec ${apps.runner}/bin/vane-testserver --plugins "$PRJ_ROOT/target" --build "$@"'';
          }
        ];
      };
      treefmt = {
        projectRootFile = "flake.nix";
        programs.nixfmt.enable = true;
        programs.ktfmt.enable = true;
        programs.ruff-format.enable = true;
        programs.shfmt = {
          enable = true;
          indent_size = 2;
        };
        programs.prettier = {
          enable = true;
          settings = builtins.fromJSON (builtins.readFile ../.prettierrc.json);
        };
        settings.formatter.ktfmt.options = [ "--kotlinlang-style" ];
        # The config path participates in treefmt's cache key when formatting policy changes.
        settings.formatter.ruff-format.options = [
          "--config"
          "${../ruff.toml}"
        ];
        settings.global.excludes = [
          ".pi/**"
          ".jj/**"
          ".direnv/**"
          ".gradle/**"
          ".kotlin/**"
          ".vane-testserver/**"
          "**/build/**"
          "**/.venv/**"
          "**/__pycache__/**"
          "**/.pytest_cache/**"
          "target/**"
          "run/**"
          "result*"
          "gradlew"
          "gradlew.bat"
          "nix/gradle-deps.json"
          "docs/resourcepacks/**"
          "docs/assets/**"
          "docs/images/**"
          "docs/wiki/**"
          "docs/css/**"
          "docs/favicon.ico"
          "docs/vane.png"
          "vane-*/src/main/resources/**"
        ];
      };
    };
}
