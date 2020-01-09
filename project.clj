(defproject lilactown/helix "0.0.2"
  :description "ClojureScript optimized for modern React development."
  :url "https://github.com/Lokeh/helix"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :source-paths ["src"]
  :dependencies [[ilk "1.0.0"]
                 [cljs-bean "1.5.0"]]
  :deploy-repositories [["releases"  {:sign-releases false :url "https://clojars.org"}]
                        ["snapshots" {:sign-releases false :url "https://clojars.org"}]])
