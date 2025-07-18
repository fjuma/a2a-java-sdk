# Changelog

## 0.2.3 (2025-07-18)


### Features

* Add a2a-tck runs to CI for PRs (GH-152) ([#162](https://github.com/a2aproject/a2a-java/issues/162)) ([a31d73a](https://github.com/a2aproject/a2a-java/commit/a31d73a015ba6401156c8f15d4afcbc9a31a0b4f))
* docs: add community articles ([#156](https://github.com/a2aproject/a2a-java/issues/156)) ([f1b8801](https://github.com/a2aproject/a2a-java/commit/f1b88014d871a8f585c6dc78f42c20586eba3a63))
* Externalise sdk-jakarta, add list of integrations and make Quarkus reference implementation ([#178](https://github.com/a2aproject/a2a-java/issues/178)) ([8d31ddf](https://github.com/a2aproject/a2a-java/commit/8d31ddf2ae45457323d58c5861305d5e5215eb2a))
* Implement the spec updates for v0.2.4 and v0.2.5 ([#192](https://github.com/a2aproject/a2a-java/issues/192)) ([93d6bae](https://github.com/a2aproject/a2a-java/commit/93d6bae9e7b1565c5a0083597f53e650816984cb))
* Rework tests so they don't rely on running in container ([#185](https://github.com/a2aproject/a2a-java/issues/185)) ([f902f18](https://github.com/a2aproject/a2a-java/commit/f902f1800bc06c8681de0e447a3e08ce732f99b9))


### Bug Fixes

* (run-tck): update TCK repository reference ([#175](https://github.com/a2aproject/a2a-java/issues/175)) ([03ed17e](https://github.com/a2aproject/a2a-java/commit/03ed17edf28d535134b8013b6f1ca9e911be6344))
* add clarifying comment about done callback and consuming order ([#154](https://github.com/a2aproject/a2a-java/issues/154)) ([982138c](https://github.com/a2aproject/a2a-java/commit/982138c726afc8150b0ac2c5110e65ea457c7b40))
* client sse ([#177](https://github.com/a2aproject/a2a-java/issues/177)) ([c3e7f9e](https://github.com/a2aproject/a2a-java/commit/c3e7f9e49c91f0db611953c81de51172d7335de2))
* Fix dependency for client example ([#167](https://github.com/a2aproject/a2a-java/issues/167)) ([2c5af01](https://github.com/a2aproject/a2a-java/commit/2c5af01a10b41510e0db6e4a92d3990061b38484))
* Fix JSONRPCVoidResponseSerializer constructor ([#193](https://github.com/a2aproject/a2a-java/issues/193)) ([da6e49d](https://github.com/a2aproject/a2a-java/commit/da6e49d422b912dbd406b5580f56da7868713815))
* fix TCK failure ([#153](https://github.com/a2aproject/a2a-java/issues/153)) ([fd0cdb7](https://github.com/a2aproject/a2a-java/commit/fd0cdb7cfc8a28b12d13624bbf6ca1dcd77dafe7))
* Fixed the intermittent failure issue in `testOnMessageStreamNewMessageSendPushNotificationSuccess` ([#161](https://github.com/a2aproject/a2a-java/issues/161)) ([884dfdb](https://github.com/a2aproject/a2a-java/commit/884dfdb1cea506a665eb7d5e67d2a3e46c745732)), closes [#140](https://github.com/a2aproject/a2a-java/issues/140)
* fixing issues when running in a JakartaEE server. ([#144](https://github.com/a2aproject/a2a-java/issues/144)) ([3ab3ad2](https://github.com/a2aproject/a2a-java/commit/3ab3ad2ff1885d85f793f11bcd9f06ff42c21ebd))
* incorporate feedback from [#138](https://github.com/a2aproject/a2a-java/issues/138) ([#146](https://github.com/a2aproject/a2a-java/issues/146)) ([d8fa0e6](https://github.com/a2aproject/a2a-java/commit/d8fa0e6268f015f03c8b8c6f844324e7d370f588))
* Incorporate latest feedback from https://github.com/a2aproject/a2a-java/pull/138 ([#182](https://github.com/a2aproject/a2a-java/issues/182)) ([d121d5c](https://github.com/a2aproject/a2a-java/commit/d121d5c325239ed0dfe44d7a61a247d63151ae71))
* Moving the AsyncExecutorProducer from @Singleton to ([#150](https://github.com/a2aproject/a2a-java/issues/150)) ([33b8e77](https://github.com/a2aproject/a2a-java/commit/33b8e771e4db8031d7a11f0626f56a378d16b508))
* README has a broken link to examples ([#134](https://github.com/a2aproject/a2a-java/issues/134)) ([558e695](https://github.com/a2aproject/a2a-java/commit/558e695459a65124277a952629ca247fabe2ee25))
* Remove `TempLoggerWrapper` and use `logback` as the logging implementation. ([#158](https://github.com/a2aproject/a2a-java/issues/158)) ([9a8e9e5](https://github.com/a2aproject/a2a-java/commit/9a8e9e58d05af4336f769ae63ebebeda00317120))
* remove printlns and use logging ([#159](https://github.com/a2aproject/a2a-java/issues/159)) ([b6712e8](https://github.com/a2aproject/a2a-java/commit/b6712e8a174f592c54bd49d64132adbb06a50ad4))
* Remove unnecessary check in the tck AgentExecutor implementation ([#194](https://github.com/a2aproject/a2a-java/issues/194)) ([b420b52](https://github.com/a2aproject/a2a-java/commit/b420b5297e8de16682a564c70e16ef48a013a025))
* Split out server related code from a2a-java-sdk-core into a new a2a-java-sdk-server-common module ([#129](https://github.com/a2aproject/a2a-java/issues/129)) ([8508b8d](https://github.com/a2aproject/a2a-java/commit/8508b8d56f6e16d0e19426fa0f28c0eb0f7b01da))
* Update paths in example READMEs and move the READMEs to the client and server dirs ([#148](https://github.com/a2aproject/a2a-java/issues/148)) ([546d00d](https://github.com/a2aproject/a2a-java/commit/546d00d44f585483a1e3aa4b8048aaae8776614a))
* Update the README following the module re-organization ([#169](https://github.com/a2aproject/a2a-java/issues/169)) ([3e15279](https://github.com/a2aproject/a2a-java/commit/3e152791501113717eec517d554f3568d20d9861))


### Documentation

* fix link to use official repository ([#160](https://github.com/a2aproject/a2a-java/issues/160)) ([f2dae4e](https://github.com/a2aproject/a2a-java/commit/f2dae4e8bad182c8ee5d70b42d5348057809b2db))


### Miscellaneous Chores

* release 0.2.3.Alpha1 ([55a16fd](https://github.com/a2aproject/a2a-java/commit/55a16fde092d882a702921410a0958e67e13c914))
