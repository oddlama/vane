{
  inputs = {
    devshell = {
      url = "github:numtide/devshell";
      inputs.nixpkgs.follows = "nixpkgs";
    };

    flake-utils.url = "github:numtide/flake-utils";
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

    pre-commit-hooks = {
      url = "github:cachix/pre-commit-hooks.nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = {
    self,
    devshell,
    flake-utils,
    nixpkgs,
    pre-commit-hooks,
  }:
    flake-utils.lib.eachDefaultSystem (
      localSystem: let
        pkgs = import nixpkgs {
          inherit localSystem;
          overlays = [devshell.overlays.default];
        };
      in {
        checks.pre-commit = pre-commit-hooks.lib.${localSystem}.run {
          src = ./.;
          hooks = {
            alejandra.enable = true;
            statix.enable = true;
          };
        };

        # `nix develop`
        devShells.default = pkgs.devshell.mkShell {
          name = "vane";

          commands = [
            {
              package = pkgs.alejandra;
              help = "Format nix code";
            }
            {
              package = pkgs.statix;
              help = "Lint nix code";
            }
            {
              package = pkgs.deadnix;
              help = "Find unused expressions in nix code";
            }
          ];

          devshell.startup.pre-commit.text = self.checks.${localSystem}.pre-commit.shellHook;
          packages = [
            pkgs.temurin-bin
            (pkgs.gradle.override {java = pkgs.temurin-bin;})
            (pkgs.python3.withPackages (ps: [
              ps.markdown
              ps.toml
              ps.pyyaml
            ]))
            pkgs.nodejs
          ];
        };

        formatter = pkgs.alejandra; # `nix fmt`
      }
    );
}
