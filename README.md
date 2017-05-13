# ASE17-SLP
This program calculates the average entropy of an entered post using a language model constructed from a specified Github project.

The first argument when starting the program is a path to the training set, the second is the name of a project to test on. The possible projects are listed below:
```
jigish_slate
SwiftyJSON_SwiftyJSON
chjj_marked
facebook_relay
scrapy_scrapy
kennethreitz_requests
alvarotrigo_fullPage.js
loverajoel_jstips
zxing_zxing
junegunn_fzf
node-inspector_node-inspector
teamcapybara_capybara
dkhamsing_open-source-ios-apps
t4t5_sweetalert
tobiasahlin_SpinKit
AngularClass_angular2-webpack-starter
hashicorp_consul
prakhar1989_awesome-courses
Thibaut_devdocs
sindresorhus_awesome
sovereign_sovereign
svg_svgo
jquery_jquery-mobile
futurice_android-best-practices
segmentio_nightmare
facebook_infer
blueimp_jQuery-File-Upload
karpathy_convnetjs
ptmt_react-native-macos
nodejitsu_node-http-proxy
facebook_react-native
reactjs_react-redux
jwagner_smartcrop.js
mperham_sidekiq
sass_sass
ReactiveX_RxAndroid
facebook_AsyncDisplayKit
DefinitelyTyped_DefinitelyTyped
sitaramc_gitolite
h5bp_Front-end-Developer-Interview-Questions
minimaxir_big-list-of-naughty-strings
nolimits4web_Framework7
mozilla_pdf.js
Flipboard_FLEX
herrbischoff_awesome-osx-command-line
slackhq_SlackTextViewController
chenglou_react-motion
realm_realm-cocoa
```
A training set has been included in the repo. It is located at src/data/issue_comments_REPLACECODE_TOKENIZEDBODY_FLAT.tar.gz and must be unpacked.

To run on this training set enter:
```
java -jar ASE.jar src/data/issue_comments_REPLACECODE_TOKENIZEDBODY_FLAT.csv name_of_project
```

After building the model the program continuosly reads in a path to a file containing text of a post to be tested. The output is the average entropy of the post.
