# Contribution Guide

Community contributions are very welcome in vane!
If you'd like to contribute, this document will guide you through the most important things that you need to know.

## What you can do

There are a lot of things that you can do to help the project.
And contributions don't necessarily have to involve coding.
Below, I've written a short checklist of things that you can check:

- Help to keep translations up to date or add a new language. [Guide to creating translations](https://github.com/oddlama/vane/wiki/Creating-a-Translation).
- Submit your ideas or discuss existing ideas on the [Issue Tracker](https://github.com/oddlama/vane/issues)
- Join our [Discord](https://discord.gg/RueJ6A59x2) and try helping people in the support channel.
- Check if you'd like to implement a feature from the [Issue Tracker](https://github.com/oddlama/vane/issues).
- Help us to build documentation for the internal framework (vane-core)
- Or discuss your own ideas with us on our [Discord](https://discord.gg/RueJ6A59x2).
- ...

## Building vane and submitting a PR

1. Get a recent version of the Java Development Kit (at least version 17). If your system doesn't provide the required JDK, you can get it from [here (adoptium.net)](https://adoptium.net/).
2. Fork the repository on GitHub.
3. Clone your fork and switch to the `develop` branch.
4. Begin working on whatever you want to do.

See [Building from source](https://github.com/oddlama/vane#building-from-source) for instruction on how to build vane.
You can open a draft PR while you are working on your changes to allow
us to provide early feedback on your implementation.

#### Before submitting your final PR

1. Make sure everything compiles
2. Make sure that your commits have meaningful messages and follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) style. Look into the commit history to see examples of good messages.
3. Rebase your changes on the newest development branch, if necessary. Usually, GitHub can do that for you.

If you need help with anything, don't hesitate to ask.

## Some things to keep in mind

- Vane is opinionated. If you want to make a contribution, please communicate with us before you start implementing,
  so we can help you design the feature in vane's spirit. We also want to avoid it that two people unknowingly work on the same thing.
- By submitting a PR, you accept that your contributions will be published under the same [LICENSE](./LICENSE) as the rest of vane.

But most importantly: Have fun! You don't need to work on anything just for the sake of contributing.
