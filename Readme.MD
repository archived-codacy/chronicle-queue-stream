# Chronicle Queue Stream

This repo contains utilities to use a [Chronicle Queue](https://github.com/OpenHFT/Chronicle-Queue) to back your Akka Source/Sink and Flow.

## Credits

This repo elaborate a little further over the awesome work done in [squbs-pattern](https://github.com/paypal/squbs/tree/master/squbs-pattern).
Because of very business related motivations (i.e. publishing this library with checked compatibility for Akka 2.4 and 2.5) we felt the need to fork instead of contributing upstream.

## Usage

Most of the usage is extensively described [here](https://github.com/paypal/squbs/blob/master/docs/persistent-buffer.md), where instead of `PersistentBuffer` you will use `com.codacy.stream.ChronicleQueue`.

We have to add that for inter-process communication we provide additional `Source` and `Sink`.

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

## Free for Open Source

Codacy is free for Open Source projects.

## License

chronicle-queue-stream is available under the Apache2 license. See the LICENSE file for more info.
